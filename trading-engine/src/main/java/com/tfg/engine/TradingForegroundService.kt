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
import com.tfg.data.remote.websocket.WebSocketManager
import com.tfg.domain.model.*
import com.tfg.domain.repository.AuditRepository
import com.tfg.domain.repository.TradingRepository
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
        acquireWakeLock()

        // Start all engines (bot + alerts)
        engineManager.startAll()

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
            val intent = android.content.Intent("com.tfg.widget.REFRESH")
            intent.setPackage(packageName)
            sendBroadcast(intent)
        } catch (_: Exception) { }
    }

    private suspend fun drainOfflineQueue() {
        val items = offlineQueueDao.getRetryable()
        for (item in items) {
            try {
                tradeExecutor.executeQueueItem(item)
                offlineQueueDao.delete(item.id)
            } catch (e: Exception) {
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
        serviceScope.cancel()
        webSocketManager.disconnectAll()
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Timber.i("TradingForegroundService stopped")
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "TFG::TradingWakeLock"
        ).apply { acquire(10 * 60 * 1000L) } // 10-minute timeout, re-acquired each cycle
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
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
        Timber.w("Task removed - service continues in background")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        releaseWakeLock()
    }
}
