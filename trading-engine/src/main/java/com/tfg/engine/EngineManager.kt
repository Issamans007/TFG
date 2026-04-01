package com.tfg.engine

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates multiple concurrent engines (bot, alerts, future modules).
 * A single foreground service hosts this manager — individual engines
 * can be started / stopped independently.
 */
@Singleton
class EngineManager @Inject constructor(
    val strategyRunner: StrategyRunner,
    val alertMonitor: AlertMonitor
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** True when at least one engine is active */
    val hasActiveEngine: StateFlow<Boolean> =
        combine(strategyRunner.isRunning, alertMonitor.isRunning) { bot, alerts ->
            bot || alerts
        }.stateIn(scope, SharingStarted.Eagerly, false)

    fun startBot() {
        Timber.i("EngineManager: starting bot")
        strategyRunner.start()
    }

    fun stopBot() {
        Timber.i("EngineManager: stopping bot")
        strategyRunner.stop()
    }

    fun startAlerts() {
        Timber.i("EngineManager: starting alert monitor")
        alertMonitor.start()
    }

    fun stopAlerts() {
        Timber.i("EngineManager: stopping alert monitor")
        alertMonitor.stop()
    }

    fun startAll() {
        startBot()
        startAlerts()
    }

    fun stopAll() {
        stopBot()
        stopAlerts()
    }
}
