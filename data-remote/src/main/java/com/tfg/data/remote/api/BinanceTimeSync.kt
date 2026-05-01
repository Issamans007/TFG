package com.tfg.data.remote.api

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks the offset between local clock and Binance server clock so that every
 * signed REST request carries a `timestamp` Binance will accept.
 *
 * Without this, an Android device with a few seconds of clock drift produces
 * `-1021 Timestamp for this request was 1000ms ahead of the server's time`
 * and orders are silently rejected. We sync on first use, every 60s, and on
 * any -1021 we re-sync and let the caller retry.
 *
 * recvWindow is fixed at 10_000 ms (Binance max is 60_000) — a balance
 * between tolerance for transient drift and the replay-attack window.
 */
@Singleton
class BinanceTimeSync @Inject constructor(
    private val spotApi: BinanceApi,
    private val futuresApi: BinanceFuturesApi
) {
    @Volatile private var offsetMs: Long = 0L
    @Volatile private var lastSyncMs: Long = 0L
    private val mutex = Mutex()

    companion object {
        const val RECV_WINDOW_MS = 10_000L
        private const val RESYNC_INTERVAL_MS = 60_000L
    }

    /** Returns server-aligned epoch ms. Triggers a resync if cache is stale. */
    suspend fun now(): Long {
        if (System.currentTimeMillis() - lastSyncMs > RESYNC_INTERVAL_MS) {
            ensureSynced(force = false)
        }
        return System.currentTimeMillis() + offsetMs
    }

    /** Force-resync regardless of cache freshness — call this on -1021. */
    suspend fun forceResync() = ensureSynced(force = true)

    private suspend fun ensureSynced(force: Boolean) = mutex.withLock {
        // Re-check inside the lock so concurrent callers don't all hit the API.
        if (!force && System.currentTimeMillis() - lastSyncMs <= RESYNC_INTERVAL_MS) return@withLock
        try {
            val before = System.currentTimeMillis()
            val server = spotApi.getServerTime().serverTime
            val after = System.currentTimeMillis()
            // Subtract half the round-trip so our offset isn't biased by
            // network latency in one direction.
            val mid = (before + after) / 2
            offsetMs = server - mid
            lastSyncMs = after
            Timber.d("Binance time synced: offset=${offsetMs}ms (rtt=${after - before}ms)")
        } catch (e: Exception) {
            Timber.w(e, "Binance time sync failed — keeping previous offset=${offsetMs}ms")
        }
    }
}

/**
 * Thrown by signed-request helpers so the caller can map -1021 / -1131 etc
 * to specific recovery actions (resync time, retry, surface to UI, ...).
 */
class BinanceApiException(val code: Int, message: String) : RuntimeException(message) {
    val isTimestampError: Boolean get() = code == -1021
}

/**
 * Best-effort parse of a Binance error response body. Binance returns
 * `{"code":-1021,"msg":"..."}` on signed-request errors.
 */
fun parseBinanceError(throwable: Throwable): BinanceApiException? {
    val httpEx = throwable as? retrofit2.HttpException ?: return null
    val body = try { httpEx.response()?.errorBody()?.string() } catch (_: Exception) { null } ?: return null
    val codeMatch = Regex("\"code\"\\s*:\\s*(-?\\d+)").find(body) ?: return null
    val msgMatch = Regex("\"msg\"\\s*:\\s*\"([^\"]*)\"").find(body)
    return BinanceApiException(
        code = codeMatch.groupValues[1].toIntOrNull() ?: 0,
        message = msgMatch?.groupValues?.get(1) ?: body.take(200)
    )
}
