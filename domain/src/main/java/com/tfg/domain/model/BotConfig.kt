package com.tfg.domain.model

data class BotConfig(
    val isEnabled: Boolean = false,
    val activePairs: List<String> = emptyList(),
    val mode: BotMode = BotMode.SIGNAL_FOLLOW,
    val autoAcceptSignals: Boolean = true,
    val autoAcceptMissedSignals: Boolean = false,
    val maxConcurrentOrders: Int = 3,
    val defaultOrderSizePercent: Double = 5.0,
    val donationEnabled: Boolean = true,
    val donationPercent: Double = 5.0,
    val paperTradingEnabled: Boolean = false,
    val telegramAlertsEnabled: Boolean = false,
    val telegramChatId: String = "",
    val riskConfig: RiskConfig = RiskConfig()
)

enum class BotMode { SIGNAL_FOLLOW, SCRIPT_AUTO, MANUAL_ONLY }

data class ConnectionState(
    val status: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val lastConnectedAt: Long? = null,
    val reconnectAttempts: Int = 0,
    val offlineSince: Long? = null,
    val pendingQueueSize: Int = 0
)

enum class ConnectionStatus {
    CONNECTED, RECONNECTING, AUTONOMOUS, EMERGENCY, PAUSED, DISCONNECTED
}

data class OfflineQueueItem(
    val id: String,
    val signal: Signal? = null,
    val order: Order? = null,
    val action: QueueAction,
    val priority: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val maxRetries: Int = 5,
    val lastError: String? = null
)

enum class QueueAction { EXECUTE_SIGNAL, PLACE_ORDER, CLOSE_POSITION, SYNC_STATE }
