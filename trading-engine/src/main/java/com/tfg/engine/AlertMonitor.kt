package com.tfg.engine

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.tfg.data.remote.websocket.WebSocketManager
import com.tfg.domain.model.*
import com.tfg.domain.repository.AlertRepository
import com.tfg.domain.repository.AuditRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class AlertMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alertRepository: AlertRepository,
    private val webSocketManager: WebSocketManager,
    private val auditRepository: AuditRepository
) {

    private var monitorJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    /** Previous tick values keyed by alert-id — used for "crosses" detection */
    private val previousValues = mutableMapOf<String, Double>()

    /** Active alarm-loop jobs keyed by alert-id */
    private val activeAlarms = mutableMapOf<String, Job>()

    /** Latest prices from the ticker WebSocket, keyed by symbol */
    private val latestPrices = mutableMapOf<String, Double>()

    companion object {
        const val ALERT_CHANNEL_ID = "tfg_alert_channel"
        private const val CHECK_INTERVAL_MS = 5_000L
        private const val RELATIVE_TOLERANCE = 0.001 // 0.1% relative tolerance for EQUALS
    }

    // ─── Lifecycle ───────────────────────────────────────────────

    fun start() {
        if (monitorJob?.isActive == true) return
        _isRunning.value = true
        createAlertNotificationChannel()

        // Collect live ticker prices in background
        monitorJob = scope.launch {
            launch { collectTickerPrices() }
            // Main evaluation loop
            while (isActive) {
                try {
                    checkAlerts()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Alert monitor error")
                }
                delay(CHECK_INTERVAL_MS)
            }
        }
        Timber.i("AlertMonitor started")
    }

    fun stop() {
        monitorJob?.cancel()
        monitorJob = null
        activeAlarms.values.forEach { it.cancel() }
        activeAlarms.clear()
        previousValues.clear()
        _isRunning.value = false
        Timber.i("AlertMonitor stopped")
    }

    // ─── Ticker price collector ──────────────────────────────────

    private suspend fun collectTickerPrices() {
        try {
            webSocketManager.tickerFlow.collect { ticker ->
                try {
                    latestPrices[ticker.symbol.uppercase()] = ticker.lastPrice.toDouble()
                } catch (_: NumberFormatException) { }
            }
        } catch (e: Exception) {
            Timber.w(e, "WebSocket ticker flow interrupted, alerts may be delayed until reconnect")
        }
    }

    // ─── Main check loop ─────────────────────────────────────────

    private suspend fun checkAlerts() {
        val alerts = alertRepository.getEnabledAlerts().first()
        for (alert in alerts) {
            try {
                val currentValue = resolveCurrentValue(alert) ?: continue
                val prevValue = previousValues[alert.id]
                previousValues[alert.id] = currentValue

                if (prevValue != null && checkCondition(alert, currentValue, prevValue)) {
                    triggerAlert(alert)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "Failed evaluating alert ${alert.id} (${alert.name})")
            }
        }
    }

    // ─── Value resolution by AlertType ───────────────────────────

    private fun resolveCurrentValue(alert: Alert): Double? {
        return when (alert.type) {
            AlertType.PRICE -> {
                getLatestPrice(alert.symbol)
            }
            AlertType.PRICE_PERCENT -> {
                val price = getLatestPrice(alert.symbol) ?: return null
                val basePrice = alert.secondaryValue ?: return null
                if (basePrice == 0.0) return null
                ((price - basePrice) / basePrice) * 100.0
            }
        }
    }

    private fun getLatestPrice(symbol: String): Double? {
        return latestPrices[symbol.uppercase()]
    }

    // ─── Condition checking ──────────────────────────────────────

    private fun checkCondition(alert: Alert, current: Double, previous: Double): Boolean {
        return when (alert.condition) {
            AlertCondition.CROSSES_ABOVE -> {
                previous < alert.targetValue && current >= alert.targetValue
            }
            AlertCondition.CROSSES_BELOW -> {
                previous > alert.targetValue && current <= alert.targetValue
            }
            AlertCondition.BETWEEN -> {
                val low = minOf(alert.targetValue, alert.secondaryValue ?: alert.targetValue)
                val high = maxOf(alert.targetValue, alert.secondaryValue ?: alert.targetValue)
                val wasOutside = previous < low || previous > high
                val isInside = current in low..high
                wasOutside && isInside
            }
            AlertCondition.OUTSIDE -> {
                val low = minOf(alert.targetValue, alert.secondaryValue ?: alert.targetValue)
                val high = maxOf(alert.targetValue, alert.secondaryValue ?: alert.targetValue)
                val wasInside = previous in low..high
                val isOutside = current < low || current > high
                wasInside && isOutside
            }
            AlertCondition.EQUALS -> {
                val threshold = if (alert.targetValue != 0.0)
                    abs(alert.targetValue) * RELATIVE_TOLERANCE else RELATIVE_TOLERANCE
                val wasNotEqual = abs(previous - alert.targetValue) > threshold
                val isEqual = abs(current - alert.targetValue) <= threshold
                wasNotEqual && isEqual
            }
        }
    }

    // ─── Trigger notification ────────────────────────────────────

    private suspend fun triggerAlert(alert: Alert) {
        Timber.i("Alert triggered: ${alert.name} (${alert.type} ${alert.condition} on ${alert.symbol})")

        alertRepository.recordTrigger(alert.id, System.currentTimeMillis())

        auditRepository.log(
            AuditLog(
                id = UUID.randomUUID().toString(),
                action = AuditAction.CONFIG_CHANGED,
                category = AuditCategory.SIGNAL,
                details = "Alert '${alert.name}' triggered: ${alert.type} ${alert.condition} on ${alert.symbol}",
                symbol = alert.symbol,
                userId = "system"
            )
        )

        fireNotification(alert)

        if (alert.isRepeating) {
            startAlarmLoop(alert)
        }
    }

    private fun fireNotification(alert: Alert) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val title = "\uD83D\uDD14 ${alert.symbol} Alert"
        val body = buildNotificationBody(alert)
        val notifId = alert.id.hashCode()

        val notification = androidx.core.app.NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setAutoCancel(true)
            .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(notifId, notification)
    }

    private fun buildNotificationBody(alert: Alert): String {
        val price = latestPrices[alert.symbol.uppercase()]?.let { "%.6g".format(it) } ?: "N/A"
        return when (alert.type) {
            AlertType.PRICE -> "${alert.name}: Price ${alert.condition.label()} ${alert.targetValue} (now $price)"
            AlertType.PRICE_PERCENT -> "${alert.name}: Price moved ${alert.condition.label()} ${alert.targetValue}% (now $price)"
        }
    }

    private fun AlertCondition.label(): String = when (this) {
        AlertCondition.CROSSES_ABOVE -> "crossed above"
        AlertCondition.CROSSES_BELOW -> "crossed below"
        AlertCondition.BETWEEN -> "entered range"
        AlertCondition.OUTSIDE -> "exited range"
        AlertCondition.EQUALS -> "reached"
    }

    private fun startAlarmLoop(alert: Alert) {
        // Cancel existing alarm for this alert if any
        activeAlarms[alert.id]?.cancel()
        activeAlarms[alert.id] = scope.launch {
            val intervalMs = (alert.repeatIntervalSec * 1000L).coerceAtLeast(10_000L)
            while (isActive) {
                delay(intervalMs)
                fireNotification(alert)
            }
        }
    }

    /** Stop a specific alert's repeating alarm (called from notification action or UI) */
    fun dismissAlarm(alertId: String) {
        activeAlarms[alertId]?.cancel()
        activeAlarms.remove(alertId)
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(alertId.hashCode())
    }

    // ─── Notification channel ────────────────────────────────────

    private fun createAlertNotificationChannel() {
        val channel = NotificationChannel(
            ALERT_CHANNEL_ID, "Price Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts for price levels and percentage changes"
            enableVibration(true)
            setShowBadge(true)
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
