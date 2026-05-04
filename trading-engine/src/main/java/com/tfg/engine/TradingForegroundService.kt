package com.tfg.engine

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.tfg.data.local.dao.*
import com.tfg.data.local.mapper.EntityMapper.toDomain
import com.tfg.data.local.mapper.EntityMapper.toEntity
import com.tfg.data.remote.api.UserDataStreamManager
import com.tfg.data.remote.websocket.WebSocketManager
import com.tfg.domain.model.*
import com.tfg.domain.repository.AuditRepository
import com.tfg.domain.repository.PortfolioRepository
import com.tfg.domain.repository.TradingRepository
import com.tfg.domain.service.ConsoleBus
import com.tfg.domain.service.ConsoleSource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class TradingForegroundService : Service() {

    @Inject lateinit var webSocketManager: WebSocketManager
    @Inject lateinit var riskEngine: RiskEngine
    @Inject lateinit var tradeExecutor: TradeExecutor
    @Inject lateinit var orderDao: OrderDao
    @Inject lateinit var offlineQueueDao: OfflineQueueDao
    @Inject lateinit var strategyRunner: StrategyRunner
    @Inject lateinit var engineManager: EngineManager
    @Inject lateinit var auditRepository: AuditRepository
    @Inject lateinit var tradingRepository: TradingRepository
    @Inject lateinit var portfolioRepository: PortfolioRepository
    @Inject lateinit var consoleBus: ConsoleBus
    @Inject lateinit var userDataStreamManager: UserDataStreamManager
    @Inject lateinit var alertDao: AlertDao
    @Inject lateinit var tradingPairDao: TradingPairDao

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var wakeLock: PowerManager.WakeLock? = null
    @Volatile
    private var isRunning = false

    companion object {
        const val CHANNEL_ID = "tfg_trading_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.tfg.engine.START"
        const val ACTION_STOP = "com.tfg.engine.STOP"
        const val ACTION_EMERGENCY_CLOSE = "com.tfg.engine.EMERGENCY_CLOSE"

        fun start(context: Context) {
            val intent = Intent(context, TradingForegroundService::class.java).apply {
                action = ACTION_START
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, TradingForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTrading()
            ACTION_STOP -> stopTrading()
            ACTION_EMERGENCY_CLOSE -> emergencyCloseAll()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startTrading() {
        if (isRunning) return
        isRunning = true

        startForeground(NOTIFICATION_ID, buildNotification("Trading Bot Active"))
        try {
            acquireWakeLock()
        } catch (e: Exception) {
            // Wake lock failure means the CPU may sleep mid-monitor and miss
            // SL/TP triggers. Surface this prominently rather than silently
            // continuing in a degraded state.
            Timber.e(e, "Failed to acquire WakeLock in startTrading() \u2014 monitoring may be unreliable")
            notifyWakeLockFailure(e)
        }

        // Start all engines (bot + alerts)
        engineManager.startAll()

        // Open Binance user-data stream (executionReport / ORDER_TRADE_UPDATE)
        // — enables real-time fill detection + listenKey keepalive every 30 min.
        userDataStreamManager.start()

        // Seed RiskEngine with real portfolio balance before the first bar fires.
        // Without this, risk checks run against a zero-balance default until the
        // first StrategyRunner bar tick (which may be 1s or more after startup).
        // Retry up to 3 times to tolerate transient network hiccups at cold start.
        serviceScope.launch {
            var seeded = false
            repeat(3) { attempt ->
                if (seeded) return@repeat
                try {
                    portfolioRepository.refreshPortfolio()
                    val portfolio = portfolioRepository.getPortfolio().first()
                    riskEngine.updatePortfolio(portfolio)
                    seeded = true
                    Timber.i("Startup portfolio seed OK: balance=%.2f".format(portfolio.totalBalance))
                } catch (e: Exception) {
                    Timber.w(e, "Startup portfolio seed attempt ${attempt + 1}/3 failed")
                    if (attempt == 2) {
                        // All retries exhausted — surface a prominent console warning.
                        consoleBus.warn(
                            ConsoleSource.SYSTEM,
                            "Portfolio unavailable at startup",
                            "Could not fetch balance after 3 attempts. " +
                            "All trades will be blocked until the portfolio refreshes. " +
                            "Check your internet connection.",
                        )
                    } else {
                        delay(2_000L * (attempt + 1)) // 2s, 4s back-off
                    }
                }
            }
        }

        // Reconcile with Binance BEFORE the monitoring loop starts so any
        // ghost orders / positions opened or closed off-app are detected and
        // logged. Failure is non-fatal — the loop still runs against local DB.
        serviceScope.launch {
            try {
                tradingRepository.reconcileWithBinance()
                    .onFailure { Timber.w(it, "Startup reconcile with Binance failed") }
                    .onSuccess { Timber.i("Startup reconcile with Binance complete") }
            } catch (e: Exception) {
                Timber.w(e, "Startup reconcile threw")
            }
        }

        // Local SL/TP monitoring loop - every 1 second
        serviceScope.launch {
            while (isActive) {
                try {
                    monitorPositions()
                } catch (e: Exception) {
                    Timber.e(e, "Error in monitoring loop")
                }
                delay(1000)
            }
        }

        // Connection state monitoring
        serviceScope.launch {
            webSocketManager.connectionState.collect { event ->
                val detail = when (event) {
                    WebSocketManager.ConnectionEvent.BINANCE_CONNECTED -> "Binance Connected - Trading Active"
                    WebSocketManager.ConnectionEvent.BINANCE_DISCONNECTED -> "Binance Disconnected"
                    WebSocketManager.ConnectionEvent.RECONNECTING -> "Reconnecting..."
                    WebSocketManager.ConnectionEvent.MAX_RETRIES_REACHED -> "Connection Lost - Emergency Mode"
                }
                detail?.let {
                    updateNotification(it)
                    auditRepository.log(AuditLog(
                        id = java.util.UUID.randomUUID().toString(),
                        action = AuditAction.CONFIG_CHANGED,
                        category = AuditCategory.SYSTEM,
                        details = "Connection: $it",
                        userId = "system"
                    ))
                }
            }
        }

        // Offline queue drainer
        serviceScope.launch {
            while (isActive) {
                try {
                    drainOfflineQueue()
                } catch (e: Exception) {
                    Timber.e(e, "Error draining offline queue")
                }
                delay(5000)
            }
        }

        // Heartbeat logger
        serviceScope.launch {
            while (isActive) {
                Timber.d("Heartbeat: Service running, positions monitored")
                // Refresh wake lock so the 24h cap is reset well before expiry.
                try { acquireWakeLock() } catch (e: Exception) {
                    Timber.e(e, "wakeLock refresh failed — surfacing to user")
                    notifyWakeLockFailure(e)
                }
                delay(60_000)
            }
        }

        Timber.i("TradingForegroundService started")
        serviceScope.launch {
            auditRepository.log(AuditLog(
                id = java.util.UUID.randomUUID().toString(),
                action = AuditAction.CONFIG_CHANGED,
                category = AuditCategory.SYSTEM,
                details = "Trading service started",
                userId = "system"
            ))
        }
    }

    private suspend fun monitorPositions() {
        val openOrders = orderDao.getOpenOrders().first()
        riskEngine.updateOpenTradeCount(openOrders.size)

        for (orderEntity in openOrders) {
            val order = orderEntity.toDomain()
            tradeExecutor.checkTakeProfits(order)
            tradeExecutor.checkStopLosses(order)
            tradeExecutor.checkTrailingStop(order)
            tradeExecutor.checkConditionalOrder(order)
            tradeExecutor.checkTimeBasedOrder(order)
        }

        // Check price-line alerts
        val priceAlerts = alertDao.getEnabled().first().filter { it.type == "PRICE" }
        for (alert in priceAlerts) {
            val pair = tradingPairDao.getBySymbol(alert.symbol).first() ?: continue
            val currentPrice = pair.lastPrice
            val triggered = when (alert.condition) {
                "CROSSES_ABOVE" -> currentPrice >= alert.targetValue
                "CROSSES_BELOW" -> currentPrice <= alert.targetValue
                else -> false
            }
            if (triggered) {
                alertDao.updateEnabled(alert.id, false)
                alertDao.recordTrigger(alert.id, System.currentTimeMillis())
                firePriceAlertNotification(alert.symbol, alert.targetValue, alert.condition, currentPrice)
            }
        }

        // Update home screen widget (throttled to ~30s)
        widgetUpdateCounter++
        if (widgetUpdateCounter >= 30) {
            widgetUpdateCounter = 0
            updateHomeWidget(openOrders.size)
        }
    }

    @Volatile private var widgetUpdateCounter = 0

    private fun updateHomeWidget(positionCount: Int) {
        try {
            val manager = android.appwidget.AppWidgetManager.getInstance(this)
            val ids = manager.getAppWidgetIds(
                android.content.ComponentName(this, "com.tfg.widget.TradingWidgetProvider")
            )
            if (ids.isNotEmpty()) {
                val intent = android.content.Intent(android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                intent.setPackage(packageName)
                intent.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                sendBroadcast(intent)
            }
        } catch (e: Exception) { Timber.w(e, "Widget update failed") }
    }

    private suspend fun drainOfflineQueue() {
        val items = offlineQueueDao.getRetryable()
        for (item in items) {
            // Atomically claim the row so a second concurrent drain triggered by
            // a rapid network reconnect event does not double-process the same item.
            val claimed = offlineQueueDao.claimForProcessing(item.id)
            if (claimed == 0) continue   // another drainer already owns this item
            try {
                tradeExecutor.executeQueueItem(item)
                offlineQueueDao.delete(item.id)
            } catch (e: Exception) {
                // markFailed also resets isProcessing so the item is retried next drain
                offlineQueueDao.markFailed(item.id, e.message ?: "Unknown error")
            }
        }
    }

    private fun emergencyCloseAll() {
        serviceScope.launch {
            Timber.w("EMERGENCY CLOSE ALL triggered")
            auditRepository.log(AuditLog(
                id = java.util.UUID.randomUUID().toString(),
                action = AuditAction.KILL_SWITCH_ACTIVATED,
                category = AuditCategory.TRADING,
                details = "Emergency close all positions triggered",
                userId = "system"
            ))
            riskEngine.activateKillSwitch()
            tradeExecutor.closeAllPositions()
        }
    }

    private fun stopTrading() {
        // Fire-and-forget audit log on IO — avoids runBlocking ANR
        CoroutineScope(Dispatchers.IO).launch {
            try {
                auditRepository.log(AuditLog(
                    id = java.util.UUID.randomUUID().toString(),
                    action = AuditAction.CONFIG_CHANGED,
                    category = AuditCategory.SYSTEM,
                    details = "Trading service stopped",
                    userId = "system"
                ))
            } catch (_: Exception) { }
        }
        isRunning = false
        engineManager.stopAll()
        userDataStreamManager.stop()
        serviceScope.cancel()
        webSocketManager.disconnectAll()
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Timber.i("TradingForegroundService stopped")
    }

    private fun firePriceAlertNotification(
        symbol: String, targetPrice: Double, condition: String, currentPrice: Double
    ) {
        try {
            val channelId = "tfg_price_alerts"
            val nm = getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(channelId) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(channelId, "Price Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                        description = "Notifications when price alert levels are reached"
                        enableLights(true)
                        enableVibration(true)
                    }
                )
            }
            val directionWord = if (condition == "CROSSES_ABOVE") "above" else "below"
            val notif = NotificationCompat.Builder(this, channelId)
                .setContentTitle("Price Alert: $symbol")
                .setContentText("$symbol reached $directionWord ${String.format("%.6f", targetPrice).trimEnd('0').trimEnd('.')} (now ${String.format("%.6f", currentPrice).trimEnd('0').trimEnd('.')})")
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            nm.notify(NOTIFICATION_ID + 100 + symbol.hashCode().and(0xFFFF), notif)
        } catch (_: Exception) {}
    }

    private fun acquireWakeLock() {
        // Re-acquire safely if a previous wake lock is still held.
        wakeLock?.let { if (it.isHeld) it.release() }
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "TFG::TradingWakeLock"
        ).apply {
            setReferenceCounted(false)
            // Hold without an aggressive 10-minute timeout (was causing the
            // CPU to sleep mid-monitoring loop). The wake lock is released in
            // stopTrading()/onDestroy() and re-acquired by the heartbeat below.
            acquire(24 * 60 * 60 * 1000L) // 24h max — heartbeat refreshes it
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    /**
     * Posts a high-importance user-visible notification when WakeLock acquisition
     * fails. CPU may sleep mid-monitor loop, missing SL/TP triggers — the user
     * needs to know the bot is operating in a degraded state.
     */
    private fun notifyWakeLockFailure(error: Throwable) {
        try {
            val channelId = "tfg_wakelock_failure"
            val nm = getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(channelId) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        channelId,
                        "Trading System Alerts",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Critical alerts about the trading engine state"
                    }
                )
            }
            val notif = NotificationCompat.Builder(this, channelId)
                .setContentTitle("Trading bot may be unreliable")
                .setContentText("WakeLock failed: ${error.message ?: error.javaClass.simpleName}. CPU may sleep and miss SL/TP triggers.")
                .setStyle(NotificationCompat.BigTextStyle().bigText(
                    "WakeLock acquisition failed: ${error.message ?: error.javaClass.simpleName}.\n" +
                    "The CPU may enter sleep state, causing the 1-second SL/TP monitoring loop " +
                    "to pause. Open positions may not close at configured levels. " +
                    "Consider stopping the bot until this is resolved."
                ))
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            nm.notify(NOTIFICATION_ID + 1, notif)
            // Also write an audit log entry so the failure is in the trail.
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    auditRepository.log(AuditLog(
                        id = java.util.UUID.randomUUID().toString(),
                        action = AuditAction.CONFIG_CHANGED,
                        category = AuditCategory.SYSTEM,
                        details = "WakeLock failure: ${error.message ?: error.javaClass.simpleName}",
                        userId = "system"
                    ))
                } catch (_: Exception) { }
            }
        } catch (_: Exception) {
            // Notification system itself broken — nothing more we can do.
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Trading Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "TradeForGood trading engine"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TradeForGood")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Some OEMs (Xiaomi/Huawei/etc.) kill foreground services tied to the
        // task on swipe-away. Schedule a restart via AlarmManager so the
        // 24/7 trading loop comes back automatically.
        if (isRunning) {
            try {
                val restartIntent = Intent(applicationContext, TradingForegroundService::class.java).apply {
                    action = ACTION_START
                    setPackage(packageName)
                }
                val pendingIntent = android.app.PendingIntent.getForegroundService(
                    applicationContext,
                    1,
                    restartIntent,
                    android.app.PendingIntent.FLAG_ONE_SHOT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
                val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                alarmManager.set(
                    android.app.AlarmManager.ELAPSED_REALTIME,
                    android.os.SystemClock.elapsedRealtime() + 1_000L,
                    pendingIntent
                )
                Timber.w("Task removed - scheduled service restart in 1s")
            } catch (e: Exception) {
                Timber.e(e, "Failed to schedule service restart on task removal")
            }
        } else {
            Timber.w("Task removed - service not running, no restart needed")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        engineManager.shutdown()
        serviceScope.cancel()
        releaseWakeLock()
    }
}
