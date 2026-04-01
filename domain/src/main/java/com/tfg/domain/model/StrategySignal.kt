package com.tfg.domain.model

/**
 * A single TP or SL level: [pct] is the distance in % from entry price,
 * [quantityPct] is what fraction of the *original* position to close (0-100).
 */
data class TpSlLevel(val pct: Double, val quantityPct: Double = 100.0)

sealed class StrategySignal {
    object HOLD : StrategySignal()
    object CLOSE_IF_LONG : StrategySignal()
    object CLOSE_IF_SHORT : StrategySignal()

    data class BUY(
        val sizePct: Double = 2.0,
        val stopLossPct: Double? = null,
        val takeProfitPct: Double? = null,
        val takeProfitLevels: List<TpSlLevel> = emptyList(),
        val stopLossLevels: List<TpSlLevel> = emptyList()
    ) : StrategySignal()

    data class SELL(
        val sizePct: Double = 100.0,
        val stopLossPct: Double? = null,
        val takeProfitPct: Double? = null,
        val takeProfitLevels: List<TpSlLevel> = emptyList(),
        val stopLossLevels: List<TpSlLevel> = emptyList()
    ) : StrategySignal()
}
