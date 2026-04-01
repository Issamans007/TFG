package com.tfg.domain.model

/**
 * User-created custom indicator that can be plotted on a chart.
 * Unlike scripts (bot engine for trading signals), indicators are
 * purely visual — they compute and display data on the chart with
 * optional visual-only signal markers.
 */
data class Indicator(
    val id: String,
    val name: String,
    val code: String,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Output of evaluating an indicator against candles.
 * Contains all series data for chart rendering.
 */
data class IndicatorOutput(
    /** Lines drawn on the price chart (e.g., moving averages, bands) */
    val overlays: List<PlotSeries> = emptyList(),
    /** Separate sub-panes below the chart (e.g., RSI, MACD) */
    val panels: List<PanelSeries> = emptyList(),
    /** Visual-only signal markers on the chart (NOT trading signals) */
    val signals: List<IndicatorSignal> = emptyList(),
    /** Fill between two overlay lines (e.g., Bollinger bands, Ichimoku cloud) */
    val fills: List<FillBetween> = emptyList(),
    /** Horizontal reference lines on the main chart */
    val hlines: List<HLine> = emptyList(),
    /** Background color bars based on conditions */
    val bgcolor: List<BgColor> = emptyList(),
    /** Text labels at specific candle positions */
    val labels: List<ChartLabel> = emptyList()
)

/**
 * A named data series to be plotted as an overlay on the price chart.
 */
data class PlotSeries(
    val name: String,
    val values: List<Double?>,   // one per candle, null = no data
    val color: String = "#58A6FF",
    val lineWidth: Int = 2,
    val lineStyle: Int = 0       // 0=solid, 1=dotted, 2=dashed
)

/**
 * A sub-chart panel (e.g., RSI pane, MACD histogram) below the main chart.
 */
data class PanelSeries(
    val name: String,
    val values: List<Double?>,
    val color: String = "#9C27B0",
    val type: String = "line",   // "line" or "histogram"
    /** Horizontal reference lines (e.g., RSI 70/30) */
    val refLines: List<RefLine> = emptyList(),
    /** Additional lines rendered in the same sub-pane */
    val extraLines: List<PlotSeries> = emptyList()
)

data class RefLine(
    val value: Double,
    val color: String = "#8B949E"
)

/**
 * A visual-only signal marker on the chart.
 * These do NOT trigger trades — they are for charting only.
 */
data class IndicatorSignal(
    val type: String,  // "BUY" or "SELL"
    val index: Int,    // candle index
    val price: Double
)

/** Fill area between two overlay lines. */
data class FillBetween(
    val line1: String,
    val line2: String,
    val color: String = "#58A6FF33"
)

/** Horizontal reference line on the main chart. */
data class HLine(
    val price: Double,
    val color: String = "#58A6FF",
    val lineStyle: Int = 2,   // 0=solid, 1=dotted, 2=dashed
    val title: String = ""
)

/** Background color for a specific candle bar. */
data class BgColor(
    val index: Int,
    val color: String
)

/** Text label at a specific candle position. */
data class ChartLabel(
    val index: Int,
    val text: String,
    val position: String = "above",  // "above" or "below"
    val color: String = "#FFFFFF"
)

/**
 * Interface for evaluating indicator JS code. Implemented by IndicatorEngine.
 */
interface IndicatorExecutor {
    fun evaluate(code: String, candles: List<Candle>): IndicatorOutput
}
