package com.tfg.domain.model

/**
 * A price alert that the user creates for a watchlisted trading pair.
 *
 * When the [condition] is met the system fires a notification.
 * If [isRepeating] the notification repeats every [repeatIntervalSec] seconds
 * until the user dismisses it.
 */
data class Alert(
    val id: String,
    val symbol: String,                       // e.g. "BTCUSDT"
    val name: String,                         // user-chosen label
    val type: AlertType,
    val condition: AlertCondition,
    val targetValue: Double,                  // price level or % threshold
    val secondaryValue: Double? = null,       // lower bound for BETWEEN / base price for PRICE_PERCENT
    val interval: String = "1h",              // kept for DB compat — not used for price alerts
    val isEnabled: Boolean = true,
    val isRepeating: Boolean = false,         // alarm-style looping notification
    val repeatIntervalSec: Int = 60,          // seconds between repeat notifications
    val lastTriggeredAt: Long? = null,
    val triggerCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class AlertType {
    PRICE,              // raw price crosses a level
    PRICE_PERCENT       // price changes by X% from the base price stored in secondaryValue
}

enum class AlertCondition {
    CROSSES_ABOVE,      // value crosses above threshold
    CROSSES_BELOW,      // value crosses below threshold
    BETWEEN,            // value enters range [targetValue, secondaryValue]
    OUTSIDE,            // value exits range
    EQUALS              // value equals threshold (with tolerance)
}
