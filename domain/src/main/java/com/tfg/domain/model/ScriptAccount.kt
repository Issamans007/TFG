package com.tfg.domain.model

/**
 * Account / position context injected into JS scripts as `_account`.
 * Allows strategies to size positions based on equity, detect open positions, etc.
 */
data class ScriptAccount(
    val equity: Double = 0.0,
    val balance: Double = 0.0,
    val positionSide: String? = null,  // "LONG", "SHORT", or null
    val positionPnl: Double = 0.0,
    val positionSize: Double = 0.0,
    val positionEntry: Double = 0.0,
    /** "WIN", "LOSS", "EVEN", or null if no trades closed yet */
    val lastTradeResult: String? = null,
    /** Number of open orders currently pending on-exchange */
    val pendingOrderCount: Int = 0,
    /** Number of consecutive losing trades (resets on a win) */
    val consecutiveLosses: Int = 0
)

/**
 * A signal tagged with an optional target symbol.
 * Used by multi-signal strategies that emit signals for multiple pairs.
 */
data class TargetedSignal(val symbol: String? = null, val signal: StrategySignal)
