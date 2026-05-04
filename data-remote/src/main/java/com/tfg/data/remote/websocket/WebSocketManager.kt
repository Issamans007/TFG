package com.tfg.data.remote.websocket

import com.google.gson.Gson
import com.tfg.core.util.Constants
import com.tfg.data.remote.api.BinanceApi
import com.tfg.data.remote.dto.TickerWsDto
import com.tfg.data.remote.dto.KlineWsDto
import com.tfg.data.remote.dto.KlineDataDto
import com.tfg.data.remote.dto.UserDataWsDto
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import okhttp3.*
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketManager @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson,
    private val binanceApi: BinanceApi
) {
    private var binanceTickerSocket: WebSocket? = null
    private var binanceKlineSocket: WebSocket? = null
    private var binanceUserDataSocket: WebSocket? = null

    // OkHttp invokes onFailure on its own dispatcher threads; multiple
    // streams may race here so the counter must be thread-safe.
    private val reconnectAttempts = java.util.concurrent.atomic.AtomicInteger(0)
    private val maxReconnectAttempts = Constants.WS_MAX_RETRIES
    private var reconnectScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Store last connection params for reconnectAll()
    @Volatile private var lastTickerSymbols: List<String>? = null
    @Volatile private var lastListenKey: String? = null

    // Kline backfill state — a WS reconnect may take several seconds during
    // which final-close kline events are lost. We remember the last finalised
    // bar's closeTime (per symbol+interval) and on reconnect ask Binance REST
    // for any bars that closed in the gap, then re-emit them so downstream
    // signal/strategy code sees a continuous bar stream.
    @Volatile private var lastKlineSymbol: String? = null
    @Volatile private var lastKlineInterval: String? = null
    @Volatile private var lastKlineCloseTimeMs: Long = 0L

    // Stale-heartbeat watchdog: Binance pings every ~3min. If we see no
    // messages at all for STALE_THRESHOLD_MS, the socket is silently dead
    // (NAT timeout, OS-level half-open, etc.) — force a reconnect rather than
    // wait for the OS TCP keepalive (which can take 2h).
    @Volatile private var lastTickerMessageMs: Long = 0L
    @Volatile private var lastUserDataMessageMs: Long = 0L
    private var watchdogJob: Job? = null
    private val STALE_THRESHOLD_MS = 60_000L // 60s silence → reconnect
    private val WATCHDOG_INTERVAL_MS = 10_000L

    init {
        // One watchdog covers all sockets — cheap timer that just checks
        // timestamps and triggers a close() so the existing onFailure path
        // schedules the reconnect.
        watchdogJob = reconnectScope.launch {
            while (isActive) {
                delay(WATCHDOG_INTERVAL_MS)
                val now = System.currentTimeMillis()
                val tickerSocket = binanceTickerSocket
                if (tickerSocket != null && lastTickerMessageMs > 0L &&
                    now - lastTickerMessageMs > STALE_THRESHOLD_MS) {
                    Timber.w("Ticker WS silent for ${(now - lastTickerMessageMs)/1000}s — force reconnect")
                    lastTickerMessageMs = now // reset so we don't re-trigger every tick
                    tickerSocket.close(1001, "stale heartbeat")
                }
                val userSocket = binanceUserDataSocket
                if (userSocket != null && lastUserDataMessageMs > 0L &&
                    now - lastUserDataMessageMs > STALE_THRESHOLD_MS) {
                    Timber.w("User-data WS silent for ${(now - lastUserDataMessageMs)/1000}s — force reconnect")
                    lastUserDataMessageMs = now
                    userSocket.close(1001, "stale heartbeat")
                }
            }
        }
    }

    private val _tickerFlow = MutableSharedFlow<TickerWsDto>(
        replay = 0, extraBufferCapacity = 256, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val tickerFlow: SharedFlow<TickerWsDto> = _tickerFlow

    private val _klineFlow = MutableSharedFlow<KlineWsDto>(
        replay = 0, extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val klineFlow: SharedFlow<KlineWsDto> = _klineFlow

    private val _userDataFlow = MutableSharedFlow<UserDataWsDto>(
        replay = 0, extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val userDataFlow: SharedFlow<UserDataWsDto> = _userDataFlow

    private val _connectionState = MutableSharedFlow<ConnectionEvent>(
        replay = 1, extraBufferCapacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val connectionState: SharedFlow<ConnectionEvent> = _connectionState

    // ─── Input validators ──────────────────────────────────────────
    // Block malformed input early so we never enter an infinite reconnect
    // loop on a URL the exchange will keep rejecting.
    private val symbolRegex = Regex("^[A-Za-z0-9]{2,32}$")
    private val intervalRegex = Regex("^(1s|1m|3m|5m|15m|30m|1h|2h|4h|6h|8h|12h|1d|3d|1w|1M)$")
    private val listenKeyRegex = Regex("^[A-Za-z0-9]{16,128}$")

    private fun rejectInvalid(reason: String) {
        Timber.e("WebSocket connect rejected: $reason")
        _connectionState.tryEmit(ConnectionEvent.BINANCE_DISCONNECTED)
    }

    fun connectBinanceTicker(symbols: List<String>) {
        if (symbols.isEmpty() || symbols.any { !symbolRegex.matches(it) }) {
            rejectInvalid("invalid ticker symbols: $symbols")
            return
        }
        lastTickerSymbols = symbols
        val streams = symbols.joinToString("/") { "${it.lowercase()}@ticker" }
        val url = "wss://stream.binance.com:9443/stream?streams=$streams"
        val request = Request.Builder().url(url).build()
        binanceTickerSocket?.close(1000, "Reconnecting")
        binanceTickerSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _connectionState.tryEmit(ConnectionEvent.BINANCE_CONNECTED)
                Timber.d("Binance ticker WebSocket connected for ${symbols.size} symbols")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                lastTickerMessageMs = System.currentTimeMillis()
                try {
                    val wrapper = gson.fromJson(text, StreamWrapper::class.java)
                    val ticker = gson.fromJson(gson.toJson(wrapper.data), TickerWsDto::class.java)
                    _tickerFlow.tryEmit(ticker)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse ticker")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Timber.e(t, "Binance ticker WebSocket failed")
                _connectionState.tryEmit(ConnectionEvent.BINANCE_DISCONNECTED)
                scheduleReconnect { connectBinanceTicker(symbols) }
            }
        })
    }

    fun connectBinanceKline(symbol: String, interval: String) {
        if (!symbolRegex.matches(symbol) || !intervalRegex.matches(interval)) {
            rejectInvalid("invalid kline params: symbol=$symbol interval=$interval")
            return
        }
        lastKlineSymbol = symbol
        lastKlineInterval = interval
        val url = "wss://stream.binance.com:9443/ws/${symbol.lowercase()}@kline_$interval"
        val request = Request.Builder().url(url).build()
        binanceKlineSocket?.close(1000, "Switching")
        binanceKlineSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                // Backfill any final bars that closed while the socket was
                // disconnected so consumers don't see gaps in the bar series.
                val sinceMs = lastKlineCloseTimeMs
                if (sinceMs > 0L) {
                    reconnectScope.launch { backfillKlines(symbol, interval, sinceMs) }
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val kline = gson.fromJson(text, KlineWsDto::class.java)
                    if (kline.k.isFinal) {
                        // Only advance the cursor on closed bars so we don't
                        // accidentally skip a bar that was still forming when
                        // the socket dropped.
                        lastKlineCloseTimeMs = maxOf(lastKlineCloseTimeMs, kline.k.closeTime)
                    }
                    _klineFlow.tryEmit(kline)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse kline")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Timber.e(t, "Binance kline WebSocket failed")
                scheduleReconnect { connectBinanceKline(symbol, interval) }
            }
        })
    }

    /**
     * Pull any klines that closed in (sinceMs, now] from REST and re-emit them
     * as KlineWsDto events with isFinal=true. Bounded to one batch (limit=500)
     * because longer disconnects almost always mean the user opened a different
     * chart and the consumer no longer cares about the old stream.
     */
    private suspend fun backfillKlines(symbol: String, interval: String, sinceMs: Long) {
        try {
            val rows = binanceApi.getKlines(
                symbol = symbol,
                interval = interval,
                limit = 500,
                startTime = sinceMs + 1
            )
            if (rows.isEmpty()) return
            Timber.i("Kline backfill: ${rows.size} bars for $symbol@$interval since $sinceMs")
            for (row in rows) {
                if (row.size < 9) continue
                val openTime = (row[0] as? Number)?.toLong() ?: continue
                val closeTime = (row[6] as? Number)?.toLong() ?: continue
                // Skip the still-open last bar — the live socket will deliver it.
                if (closeTime > System.currentTimeMillis()) continue
                val data = KlineDataDto(
                    openTime = openTime,
                    open = row[1].toString(),
                    high = row[2].toString(),
                    low = row[3].toString(),
                    close = row[4].toString(),
                    volume = row[5].toString(),
                    closeTime = closeTime,
                    quoteVolume = row[7].toString(),
                    numberOfTrades = (row[8] as? Number)?.toInt() ?: 0,
                    isFinal = true
                )
                lastKlineCloseTimeMs = maxOf(lastKlineCloseTimeMs, closeTime)
                _klineFlow.tryEmit(KlineWsDto(symbol = symbol, k = data))
            }
        } catch (e: Exception) {
            Timber.w(e, "Kline backfill failed for $symbol@$interval")
        }
    }

    fun connectBinanceUserData(listenKey: String) {
        if (!listenKeyRegex.matches(listenKey)) {
            rejectInvalid("invalid listenKey format")
            return
        }
        lastListenKey = listenKey
        val url = "wss://stream.binance.com:9443/ws/$listenKey"
        val request = Request.Builder().url(url).build()
        binanceUserDataSocket?.close(1000, "Reconnecting")
        binanceUserDataSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                lastUserDataMessageMs = System.currentTimeMillis()
                Timber.d("User-data WebSocket connected")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                lastUserDataMessageMs = System.currentTimeMillis()
                try {
                    val event = gson.fromJson(text, UserDataWsDto::class.java)
                    _userDataFlow.tryEmit(event)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse user data event")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Timber.e(t, "User data WebSocket failed")
                scheduleReconnect { connectBinanceUserData(listenKey) }
            }
        })
    }

    private fun scheduleReconnect(reconnect: () -> Unit) {
        val attempt = reconnectAttempts.incrementAndGet()
        if (attempt > maxReconnectAttempts) {
            _connectionState.tryEmit(ConnectionEvent.MAX_RETRIES_REACHED)
            Timber.e("Max reconnect attempts reached")
            return
        }
        val delay = minOf(Constants.WS_RECONNECT_BASE_DELAY * (1L shl minOf(attempt, 10)), Constants.WS_RECONNECT_MAX_DELAY)
        _connectionState.tryEmit(ConnectionEvent.RECONNECTING)
        Timber.d("Reconnecting in ${delay}ms (attempt $attempt)")
        reconnectScope.launch {
            delay(delay)
            reconnect()
        }
    }

    fun reconnectAll() {
        reconnectAttempts.set(0)
        lastTickerSymbols?.let { connectBinanceTicker(it) }
        lastListenKey?.let { connectBinanceUserData(it) }
    }

    fun disconnectAll() {
        reconnectScope.cancel()
        reconnectScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        binanceTickerSocket?.close(1000, "App closing")
        binanceKlineSocket?.close(1000, "App closing")
        binanceUserDataSocket?.close(1000, "App closing")
        binanceTickerSocket = null
        binanceKlineSocket = null
        binanceUserDataSocket = null
        reconnectAttempts.set(0)
    }

    private data class StreamWrapper(val stream: String, val data: Any)

    enum class ConnectionEvent {
        BINANCE_CONNECTED, BINANCE_DISCONNECTED,
        RECONNECTING, MAX_RETRIES_REACHED
    }
}
