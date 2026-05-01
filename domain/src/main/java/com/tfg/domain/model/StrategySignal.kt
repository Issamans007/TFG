package com.tfg.domain.model

/**
 * A single TP or SL level: [pct] is the distance in % from entry price,
 * [quantityPct] is what fraction of the *original* position to close (0-100).
 */
data class TpSlLevel(val pct: Double, val quantityPct: Double = 100.0)

/**
 * Signal sub-type for chart display and audit logging.
 * Scripts return a `label` string (e.g. "PAT_BUY", "BREAKOUT_BUY") which
 * is normalised into one of these well-known types — or kept verbatim in [CUSTOM].
 */
enum class SignalLabel(val display: String) {
    EMA_CROSS("EMA ✕"),
    PATTERN("Pattern"),
    BREAKOUT("Breakout"),
    REVERSAL("Reversal"),
    SCALP("Scalp"),
    DCA("DCA"),
    CUSTOM("");                // display text is the raw label string

    companion object {
        fun from(raw: String?): SignalLabel =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: CUSTOM
    }
}

/**
 * Order execution type requested by the strategy.
 * Maps to [OrderType] at trade-execution time.
 */
enum class SignalOrderType {
    MARKET, LIMIT, STOP_LIMIT, TRAILING_STOP;

    companion object {
        fun from(raw: String?): SignalOrderType =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: MARKET
    }
}

/**
 * Chart overlay/panel data the strategy wants plotted on the chart.
 * Returned inside the JS strategy result object as `overlays` and `panels`.
 */
data class StrategyPlotData(
    val overlays: List<PlotSeries> = emptyList(),
    val panels: List<PanelSeries> = emptyList(),
    val hlines: List<HLine> = emptyList(),
    val fills: List<FillBetween> = emptyList(),
    val labels: List<ChartLabel> = emptyList(),
    val bgcolor: List<BgColor> = emptyList()
)

sealed class StrategySignal {
    object HOLD : StrategySignal()
    object CLOSE_IF_LONG : StrategySignal()
    object CLOSE_IF_SHORT : StrategySignal()

    data class BUY(
        val sizePct: Double = 2.0,
        val stopLossPct: Double? = null,
        val takeProfitPct: Double? = null,
        val takeProfitLevels: List<TpSlLevel> = emptyList(),
        val stopLossLevels: List<TpSlLevel> = emptyList(),
        val label: SignalLabel = SignalLabel.EMA_CROSS,
        val labelRaw: String = "",
        val orderType: SignalOrderType = SignalOrderType.MARKET,
        val limitPrice: Double? = null,
        val marketType: MarketType? = null,
        val leverage: Int? = null,
        val marginType: MarginType? = null
    ) : StrategySignal()

    data class SELL(
        val sizePct: Double = 100.0,
        val stopLossPct: Double? = null,
        val takeProfitPct: Double? = null,
        val takeProfitLevels: List<TpSlLevel> = emptyList(),
        val stopLossLevels: List<TpSlLevel> = emptyList(),
        val label: SignalLabel = SignalLabel.EMA_CROSS,
        val labelRaw: String = "",
        val orderType: SignalOrderType = SignalOrderType.MARKET,
        val limitPrice: Double? = null,
        val marketType: MarketType? = null,
        val leverage: Int? = null,
        val marginType: MarginType? = null
    ) : StrategySignal()
}
