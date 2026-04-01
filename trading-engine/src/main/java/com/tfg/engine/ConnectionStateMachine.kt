package com.tfg.engine

import com.tfg.data.remote.websocket.WebSocketManager
import com.tfg.domain.model.ConnectionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

data class EngineConnectionState(
    val signalWs: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val binanceWs: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val internet: Boolean = true
)

@Singleton
class ConnectionStateMachine @Inject constructor(
    private val webSocketManager: WebSocketManager
) {

    private val _state = MutableStateFlow(
        EngineConnectionState(
            signalWs = ConnectionStatus.DISCONNECTED,
            binanceWs = ConnectionStatus.DISCONNECTED,
            internet = true
        )
    )
    val state: StateFlow<EngineConnectionState> = _state

    private var reconnectRequested = false

    fun updateSignalWs(status: ConnectionStatus) {
        _state.value = _state.value.copy(signalWs = status)
        Timber.d("Signal WS: $status")
        evaluateOverallState()
    }

    fun updateBinanceWs(status: ConnectionStatus) {
        _state.value = _state.value.copy(binanceWs = status)
        Timber.d("Binance WS: $status")
        evaluateOverallState()
    }

    fun updateInternet(available: Boolean) {
        _state.value = _state.value.copy(internet = available)
        Timber.d("Internet: $available")
        evaluateOverallState()
    }

    private fun evaluateOverallState() {
        val s = _state.value
        if (!s.internet) {
            Timber.w("No internet - entering offline mode")
            reconnectRequested = false
        } else if (s.binanceWs == ConnectionStatus.DISCONNECTED && !reconnectRequested) {
            Timber.w("WS disconnected with internet - triggering reconnect")
            reconnectRequested = true
            webSocketManager.reconnectAll()
        } else if (s.binanceWs == ConnectionStatus.CONNECTED) {
            reconnectRequested = false
        }
    }

    val isFullyConnected: Boolean
        get() {
            val s = _state.value
            return s.internet &&
                   s.signalWs == ConnectionStatus.CONNECTED &&
                   s.binanceWs == ConnectionStatus.CONNECTED
        }

    val isOffline: Boolean
        get() = !_state.value.internet
}
