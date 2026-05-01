package com.tfg.domain.model

/**
 * Default thresholds for [RiskConfig]. Centralised here so:
 *   1. Tests can refer to the same numeric constants without redefining them.
 *   2. A future "reset to defaults" button maps to a single source of truth.
 *   3. The defaults are documented next to the rule they apply to.
 *
 * NOTE: these values are intentionally conservative. Crossing them blocks
 * trades — they are NOT advisory.
 */
object RiskDefaults {
    /** Maximum cumulative realised loss for the day, as % of total balance. */
    const val DAILY_LOSS_LIMIT_PERCENT = 5.0

    /** Maximum notional of a single order, as % of total balance. */
    const val MAX_POSITION_SIZE_PERCENT = 10.0

    /** Hard cap on simultaneously open positions. */
    const val MAX_OPEN_TRADES = 5

    /** Maximum exposure to one symbol across all open orders, as %. */
    const val SYMBOL_EXPOSURE_LIMIT_PERCENT = 20.0

    /** Block fill if observed slippage from quoted price exceeds this %. */
    const val MAX_SLIPPAGE_PERCENT = 0.5

    /** Block entry if bid/ask spread exceeds this % of mid price. */
    const val MAX_SPREAD_PERCENT = 0.3

    /** After N TP levels fire, raise SL to entry (breakeven). */
    const val BREAK_EVEN_AFTER_TP_COUNT = 1

    /** Pause trading when 14-bar ATR percentage exceeds this threshold. */
    const val VOLATILITY_PAUSE_THRESHOLD = 5.0

    /** Halt new entries after this many consecutive losing trades. */
    const val CONSECUTIVE_LOSS_LIMIT = 3

    /** Maximum acceptable risk on one trade (entry to stop), as % of balance. */
    const val MAX_LOSS_PER_TRADE_PERCENT = 2.0

    /** Pause trading once equity drawdown from peak reaches this %. */
    const val DRAWDOWN_PAUSE_PERCENT = 10.0

    /** Maximum number of correlated open positions. */
    const val CORRELATION_LIMIT = 2

    /** Pause if Fear-and-Greed index drops below this. */
    const val FEAR_GREED_PAUSE_BELOW = 20

    /** Pause if Fear-and-Greed index rises above this. */
    const val FEAR_GREED_PAUSE_ABOVE = 80

    /** Whether weekend trading is enabled by default. */
    const val WEEKEND_TRADING_ENABLED = true

    /** Master kill-switch state. */
    const val KILL_SWITCH_ENABLED = false

    /** Emergency-close-all flag. */
    const val EMERGENCY_CLOSE_ALL = false
}

data class RiskConfig(
    val dailyLossLimitPercent: Double = RiskDefaults.DAILY_LOSS_LIMIT_PERCENT,
    val maxPositionSizePercent: Double = RiskDefaults.MAX_POSITION_SIZE_PERCENT,
    val maxOpenTrades: Int = RiskDefaults.MAX_OPEN_TRADES,
    val symbolExposureLimitPercent: Double = RiskDefaults.SYMBOL_EXPOSURE_LIMIT_PERCENT,
    val maxSlippagePercent: Double = RiskDefaults.MAX_SLIPPAGE_PERCENT,
    val maxSpreadPercent: Double = RiskDefaults.MAX_SPREAD_PERCENT,
    val breakEvenAfterTpCount: Int = RiskDefaults.BREAK_EVEN_AFTER_TP_COUNT,
    val volatilityPauseThreshold: Double = RiskDefaults.VOLATILITY_PAUSE_THRESHOLD,
    val consecutiveLossLimit: Int = RiskDefaults.CONSECUTIVE_LOSS_LIMIT,
    val maxLossPerTradePercent: Double = RiskDefaults.MAX_LOSS_PER_TRADE_PERCENT,
    val drawdownPausePercent: Double = RiskDefaults.DRAWDOWN_PAUSE_PERCENT,
    val correlationLimit: Int = RiskDefaults.CORRELATION_LIMIT,
    val tradingWindowStart: String? = null,
    val tradingWindowEnd: String? = null,
    val fearGreedPauseBelow: Int = RiskDefaults.FEAR_GREED_PAUSE_BELOW,
    val fearGreedPauseAbove: Int = RiskDefaults.FEAR_GREED_PAUSE_ABOVE,
    val weekendTradingEnabled: Boolean = RiskDefaults.WEEKEND_TRADING_ENABLED,
    val killSwitchEnabled: Boolean = RiskDefaults.KILL_SWITCH_ENABLED,
    val emergencyCloseAll: Boolean = RiskDefaults.EMERGENCY_CLOSE_ALL
)

data class RiskCheckResult(
    val allowed: Boolean,
    val violations: List<RiskViolation> = emptyList()
)

data class RiskViolation(
    val rule: String,
    val message: String,
    val severity: RiskSeverity
)

enum class RiskSeverity { WARNING, BLOCK, EMERGENCY }
