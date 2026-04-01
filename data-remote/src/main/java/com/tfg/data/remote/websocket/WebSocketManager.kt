package com.tfg.data.remote.websocket

import com.google.gson.Gson
import com.tfg.core.util.Constants
import com.tfg.data.remote.dto.TickerWsDto
import com.tfg.data.remote.dto.KlineWsDto
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
    private val gson: Gson
) {
    private var binanceTickerSocket: WebSocket? = null
    private var binanceKlineSocket: WebSocket? = null
    private var binanceUserDataSocket: WebSocket? = null

    private var reconnectAttempts = 0
    private val maxReconnectAttempts = Constants.WS_MAX_RETRIES
    private var reconnectScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Store last connection params for reconnectAll()
    private var lastTickerSymbols: List<String>? = null
    private var lastListenKey: String? = null

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

    fun connectBinanceTicker(symbols: List<String>) {
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
        val url = "wss://stream.binance.com:9443/ws/${symbol.lowercase()}@kline_$interval"
        val request = Request.Builder().url(url).build()
        binanceKlineSocket?.close(1000, "Switching")
        binanceKlineSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val kline = gson.fromJson(text, KlineWsDto::class.java)
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

    fun connectBinanceUserData(listenKey: String) {
        lastListenKey = listenKey
        val url = "wss://stream.binance.com:9443/ws/$listenKey"
        val request = Request.Builder().url(url).build()
        binanceUserDataSocket?.close(1000, "Reconnecting")
        binanceUserDataSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
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
        if (reconnectAttempts >= maxReconnectAttempts) {
            _connectionState.tryEmit(ConnectionEvent.MAX_RETRIES_REACHED)
            Timber.e("Max reconnect attempts reached")
            return
        }
        reconnectAttempts++
        val delay = minOf(Constants.WS_RECONNECT_BASE_DELAY * (1L shl minOf(reconnectAttempts, 10)), Constants.WS_RECONNECT_MAX_DELAY)
        _connectionState.tryEmit(ConnectionEvent.RECONNECTING)
        Timber.d("Reconnecting in ${delay}ms (attempt $reconnectAttempts)")
        reconnectScope.launch {
            delay(delay)
            reconnect()
        }
    }

    fun reconnectAll() {
        reconnectAttempts = 0
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
        reconnectAttempts = 0
    }

    private data class StreamWrapper(val stream: String, val data: Any)

    enum class ConnectionEvent {
        BINANCE_CONNECTED, BINANCE_DISCONNECTED,
        RECONNECTING, MAX_RETRIES_REACHED
    }
}
