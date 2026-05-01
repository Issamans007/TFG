package com.tfg.domain.service

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Severity tier for a console event. Higher = noisier.
 */
enum class ConsoleSeverity { DEBUG, INFO, SUCCESS, WARNING, ERROR }

/**
 * High-level source/category of a console event. Used for filtering and
 * deciding which icon / colour to render in the main Console screen.
 */
enum class ConsoleSource {
    BACKTEST,
    STRATEGY,
    SIGNAL,
    TRADING,
    SYSTEM,
    NETWORK,
    SECURITY,
    USER
}

/**
 * One entry in the global console stream. Lightweight (in-memory only).
 * For long-term persistence of trading-relevant events keep using
 * [com.tfg.domain.repository.AuditRepository] — this bus is the *live*
 * pipe, not the audit log.
 */
data class ConsoleEvent(
    val id: Long,
    val timestamp: Long,
    val severity: ConsoleSeverity,
    val source: ConsoleSource,
    val title: String,
    val message: String,
    val symbol: String? = null,
    val tag: String? = null
)

/**
 * Per-source / per-severity preferences for the in-app Console.
 * All toggles are non-destructive — disabling a category just hides events
 * from the live feed; nothing about persistence or strategy execution
 * changes when these flip.
 */
data class ConsoleNotificationSettings(
    val showInLiveFeed: Boolean = true,
    val playSound: Boolean = false,
    val vibrate: Boolean = false,
    val toastOnError: Boolean = true,
    val minSeverity: ConsoleSeverity = ConsoleSeverity.DEBUG,
    val enabledSources: Set<ConsoleSource> = ConsoleSource.values().toSet()
) {
    fun accepts(event: ConsoleEvent): Boolean =
        showInLiveFeed &&
            event.severity.ordinal >= minSeverity.ordinal &&
            event.source in enabledSources
}

/**
 * App-wide event bus for console output. Inject this anywhere — repositories,
 * view-models, engine code — and call [emit] to surface a message into the
 * main Console screen.
 *
 * The bus replays the last 200 events to new subscribers so the user can
 * scroll back through recent activity even after navigating away.
 */
@Singleton
class ConsoleBus @Inject constructor() {

    private val _events = MutableSharedFlow<ConsoleEvent>(
        replay = REPLAY,
        extraBufferCapacity = 128,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<ConsoleEvent> = _events.asSharedFlow()

    private val _settings = MutableStateFlow(ConsoleNotificationSettings())
    val settings: StateFlow<ConsoleNotificationSettings> = _settings.asStateFlow()

    private var nextId = 1L

    @Synchronized
    fun emit(
        severity: ConsoleSeverity,
        source: ConsoleSource,
        title: String,
        message: String = "",
        symbol: String? = null,
        tag: String? = null
    ) {
        val ev = ConsoleEvent(
            id = nextId++,
            timestamp = System.currentTimeMillis(),
            severity = severity,
            source = source,
            title = title,
            message = message,
            symbol = symbol,
            tag = tag
        )
        _events.tryEmit(ev)
    }

    fun debug(source: ConsoleSource, title: String, message: String = "", symbol: String? = null) =
        emit(ConsoleSeverity.DEBUG, source, title, message, symbol)

    fun info(source: ConsoleSource, title: String, message: String = "", symbol: String? = null) =
        emit(ConsoleSeverity.INFO, source, title, message, symbol)

    fun success(source: ConsoleSource, title: String, message: String = "", symbol: String? = null) =
        emit(ConsoleSeverity.SUCCESS, source, title, message, symbol)

    fun warn(source: ConsoleSource, title: String, message: String = "", symbol: String? = null) =
        emit(ConsoleSeverity.WARNING, source, title, message, symbol)

    fun error(source: ConsoleSource, title: String, message: String = "", symbol: String? = null) =
        emit(ConsoleSeverity.ERROR, source, title, message, symbol)

    fun updateSettings(transform: (ConsoleNotificationSettings) -> ConsoleNotificationSettings) {
        _settings.value = transform(_settings.value)
    }

    /** Buffered snapshot of the last [REPLAY] events (newest last). */
    fun snapshot(): List<ConsoleEvent> = _events.replayCache

    companion object { const val REPLAY = 200 }
}
