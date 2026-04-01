package com.tfg.domain.model

data class AuditLog(
    val id: String,
    val action: AuditAction,
    val category: AuditCategory,
    val details: String,
    val oldValue: String? = null,
    val newValue: String? = null,
    val orderId: String? = null,
    val symbol: String? = null,
    val userId: String,
    val ipAddress: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

enum class AuditAction {
    LOGIN, LOGOUT, ORDER_PLACED, ORDER_FILLED, ORDER_CANCELLED,
    ORDER_EMERGENCY_CLOSED, SIGNAL_RECEIVED, SIGNAL_EXECUTED,
    SIGNAL_SKIPPED, API_KEY_SET, API_KEY_REVOKED,
    CONFIG_CHANGED, PIN_ENTERED, PIN_FAILED, BIOMETRIC_AUTH,
    KYC_SUBMITTED, KYC_VERIFIED, DONATION_SENT,
    RISK_VIOLATION, KILL_SWITCH_ACTIVATED, APP_CRASH,
    OFFLINE_SYNC, CONFLICT_RESOLVED, EXPORT_GENERATED,
    API_ERROR
}

enum class AuditCategory {
    AUTH, TRADING, SIGNAL, SECURITY, CONFIG, COMPLIANCE, SYSTEM
}

enum class NotificationType {
    ORDER_FILLED, ORDER_CANCELLED, STOP_LOSS_TRIGGERED, TAKE_PROFIT_TRIGGERED,
    SIGNAL_RECEIVED, SIGNAL_EXPIRED, RISK_ALERT, API_ERROR,
    SYSTEM, INFO
}

data class Donation(
    val id: String,
    val orderId: String,
    val amount: Double,
    val currency: String,
    val ngoName: String,
    val ngoId: String,
    val status: DonationStatus,
    val timestamp: Long = System.currentTimeMillis()
)

enum class DonationStatus { PENDING, SENT, CONFIRMED, FAILED }
