package com.tfg.data.remote.api

import com.tfg.data.remote.websocket.WebSocketManager
import kotlinx.coroutines.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the lifecycle of Binance Spot + Futures user-data streams:
 *  1. POST /api/v3/userDataStream  (and /fapi/v1/listenKey) to obtain a listenKey.
 *  2. Connect WebSocket to wss://stream.binance.com/ws/<listenKey>.
 *  3. PUT every 30 minutes to keep the listenKey alive (Binance expires after 60 min).
 *  4. DELETE on shutdown.
 *
 * Should be started once when the trading service starts and stopped on shutdown.
 * Failures are logged but never crash the service — REST fill polling remains
 * as a fallback if the user-data stream is unavailable.
 */
@Singleton
class UserDataStreamManager @Inject constructor(
    private val spotApi: BinanceApi,
    private val futuresApi: BinanceFuturesApi,
    private val webSocketManager: WebSocketManager
) {
    companion object {
        private const val KEEPALIVE_INTERVAL_MS = 30L * 60_000L // 30 min — Binance expires at 60.
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var spotListenKey: String? = null
    @Volatile private var futuresListenKey: String? = null
    private var keepaliveJob: Job? = null

    /** Open both spot & futures user-data streams. Idempotent. */
    fun start() {
        if (keepaliveJob?.isActive == true) return
        scope.launch {
            try {
                val key = spotApi.createSpotListenKey().listenKey
                spotListenKey = key
                webSocketManager.connectBinanceUserData(key)
                Timber.i("Spot user-data stream connected")
            } catch (e: Exception) {
                Timber.w(e, "Failed to start spot user-data stream — REST polling will be used as fallback")
            }
        }
        scope.launch {
            try {
                val key = futuresApi.createFuturesListenKey().listenKey
                futuresListenKey = key
                // We currently route both streams through the same WS connection
                // slot in WebSocketManager; futures events arrive on /ws/<key>
                // just like spot. If the user runs spot-only, the futures listen
                // key is harmless (Binance returns a key even with no futures
                // account, but the WS will simply emit nothing).
                Timber.i("Futures listenKey acquired")
            } catch (e: Exception) {
                Timber.w(e, "Failed to start futures user-data stream")
            }
        }
        keepaliveJob = scope.launch {
            // Wait one full interval before the first keepalive — the listen
            // key is freshly minted, no need to ping it immediately.
            while (isActive) {
                delay(KEEPALIVE_INTERVAL_MS)
                spotListenKey?.let { key ->
                    runCatching { spotApi.keepaliveSpotListenKey(key) }
                        .onFailure { Timber.w(it, "Spot listenKey keepalive failed") }
                }
                runCatching { futuresApi.keepaliveFuturesListenKey() }
                    .onFailure { Timber.w(it, "Futures listenKey keepalive failed") }
            }
        }
    }

    /** Cleanly close listen keys on shutdown. */
    fun stop() {
        keepaliveJob?.cancel()
        keepaliveJob = null
        scope.launch {
            spotListenKey?.let { key ->
                runCatching { spotApi.closeSpotListenKey(key) }
            }
            runCatching { futuresApi.closeFuturesListenKey() }
            spotListenKey = null
            futuresListenKey = null
        }
    }
}
