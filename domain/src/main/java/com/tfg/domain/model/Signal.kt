package com.tfg.domain.model

data class Signal(
    val id: String,
    val symbol: String,
    val side: OrderSide,
    val entryPrice: Double,
    val takeProfits: List<TakeProfit>,
    val stopLosses: List<StopLoss>,
    val confidence: Double,
    val riskRewardRatio: Double,
    val expiresAt: Long,
    val receivedAt: Long = System.currentTimeMillis(),
    val status: SignalStatus = SignalStatus.PENDING,
    val hmacSignature: String = "",
    val isExpired: Boolean = false,
    val wasExecuted: Boolean = false,
    val missedWhileOffline: Boolean = false
) {
    fun isValid(): Boolean = !isExpired && System.currentTimeMillis() < expiresAt
}

enum class SignalStatus {
    PENDING, ACCEPTED, EXECUTED, SKIPPED, EXPIRED, REJECTED, QUEUED_OFFLINE
}
