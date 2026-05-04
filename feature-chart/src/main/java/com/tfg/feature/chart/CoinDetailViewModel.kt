package com.tfg.feature.chart

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tfg.domain.model.*
import com.tfg.domain.model.extractParamsFromCode
import com.tfg.domain.model.injectParamIntoCode
import com.tfg.domain.repository.AlertRepository
import com.tfg.domain.repository.MarketRepository
import com.tfg.domain.repository.ScriptRepository
import com.tfg.domain.repository.IndicatorRepository
import com.tfg.data.local.dao.DrawingSnapshotDao
import com.tfg.data.local.entity.DrawingSnapshotEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

// ─── Indicator setting models ───────────────────────────────────────

/** Extracted input() call definition from user indicator code */
data class IndicatorInputDef(val name: String, val defaultValue: String)

/** Extract input("name", default) calls from JS code */
fun extractInputDefs(code: String): List<IndicatorInputDef> {
    // Match: input("name", defaultValue) or input('name', defaultValue)
    val regex = Regex("""input\(\s*["']([^"']+)["']\s*,\s*([^)]+)\)""")
    return regex.findAll(code).map { match ->
        IndicatorInputDef(
            name = match.groupValues[1],
            defaultValue = match.groupValues[2].trim()
        )
    }.toList()
}

/** Indicator template entry */
data class IndicatorTemplateItem(val name: String, val description: String, val code: String)

/** Built-in indicator templates */
val INDICATOR_TEMPLATES = listOf(
    IndicatorTemplateItem("SMA Crossover", "Fast/Slow SMA crossover with buy/sell signals", """
function indicator(candles) {
  var fast = smaSeries(candles, 10);
  var slow = smaSeries(candles, 30);
  var signals = [];
  for (var i = 1; i < candles.length; i++) {
    if (fast[i] !== null && slow[i] !== null && fast[i-1] !== null && slow[i-1] !== null) {
      if (fast[i-1] < slow[i-1] && fast[i] > slow[i])
        signals.push({ type: "BUY", index: i, price: candles[i].close });
      else if (fast[i-1] > slow[i-1] && fast[i] < slow[i])
        signals.push({ type: "SELL", index: i, price: candles[i].close });
    }
  }
  return {
    overlays: [
      { name: "Fast SMA(10)", values: fast, color: "#2196F3", lineWidth: 2 },
      { name: "Slow SMA(30)", values: slow, color: "#FF9800", lineWidth: 2 }
    ],
    panels: [],
    signals: signals
  };
}
""".trimIndent()),
    IndicatorTemplateItem("EMA Ribbon", "8 EMAs creating a visual ribbon", """
function indicator(candles) {
  var periods = [8, 13, 21, 34, 55, 89, 144, 200];
  var colors = ["#E91E63","#FF5722","#FF9800","#FFC107","#4CAF50","#009688","#2196F3","#3F51B5"];
  var overlays = [];
  for (var p = 0; p < periods.length; p++) {
    overlays.push({ name: "EMA("+periods[p]+")", values: emaSeries(candles, periods[p]), color: colors[p], lineWidth: 1 });
  }
  return { overlays: overlays, panels: [], signals: [] };
}
""".trimIndent()),
    IndicatorTemplateItem("RSI", "RSI with overbought/oversold zones and signals", """
function indicator(candles) {
  var rsi = rsiSeries(candles, 14);
  var signals = [];
  for (var i = 1; i < rsi.length; i++) {
    if (rsi[i] !== null && rsi[i-1] !== null) {
      if (rsi[i-1] < 30 && rsi[i] >= 30)
        signals.push({ type: "BUY", index: i, price: candles[i].close });
      else if (rsi[i-1] > 70 && rsi[i] <= 70)
        signals.push({ type: "SELL", index: i, price: candles[i].close });
    }
  }
  return {
    overlays: [],
    panels: [{
      name: "RSI(14)", values: rsi, color: "#E040FB", type: "line",
      lines: [{ value: 70, color: "#EF535080" }, { value: 30, color: "#26A69A80" }]
    }],
    signals: signals
  };
}
""".trimIndent()),
    IndicatorTemplateItem("MACD", "MACD line, signal line, and histogram", """
function indicator(candles) {
  var m = macdSeries(candles, 12, 26, 9);
  return {
    overlays: [],
    panels: [{
      name: "MACD", values: m.histogram, color: "#26A69A", type: "histogram",
      extraLines: [
        { name: "MACD Line", values: m.macd, color: "#2196F3" },
        { name: "Signal", values: m.signal, color: "#FF9800" }
      ]
    }],
    signals: []
  };
}
""".trimIndent()),
    IndicatorTemplateItem("Bollinger Bands", "Bollinger Bands overlay with squeeze detection", """
function indicator(candles) {
  var bb = bollingerSeries(candles, 20, 2);
  return {
    overlays: [
      { name: "BB Upper", values: bb.upper, color: "#FFD700", lineWidth: 1 },
      { name: "BB Middle", values: bb.middle, color: "#FFD70080", lineWidth: 1, lineStyle: 2 },
      { name: "BB Lower", values: bb.lower, color: "#FFD700", lineWidth: 1 }
    ],
    panels: [],
    signals: []
  };
}
""".trimIndent()),
    IndicatorTemplateItem("Stochastic", "Stochastic %K/%D oscillator", """
function indicator(candles) {
  var st = stochasticSeries(candles, 14, 3);
  return {
    overlays: [],
    panels: [{
      name: "%K", values: st.k, color: "#2196F3", type: "line",
      lines: [{ value: 80, color: "#EF535080" }, { value: 20, color: "#26A69A80" }],
      extraLines: [{ name: "%D", values: st.d, color: "#FF9800" }]
    }],
    signals: []
  };
}
""".trimIndent()),
    IndicatorTemplateItem("ATR", "Average True Range volatility indicator", """
function indicator(candles) {
  var atr = atrSeries(candles, 14);
  return {
    overlays: [],
    panels: [{ name: "ATR(14)", values: atr, color: "#607D8B", type: "line" }],
    signals: []
  };
}
""".trimIndent()),
    IndicatorTemplateItem("VWAP", "Volume Weighted Average Price", """
function indicator(candles) {
  var v = vwapSeries(candles);
  return {
    overlays: [{ name: "VWAP", values: v, color: "#00E5FF", lineWidth: 2 }],
    panels: [],
    signals: []
  };
}
""".trimIndent()),
    IndicatorTemplateItem("OBV", "On Balance Volume", """
function indicator(candles) {
  var o = obvSeries(candles);
  return {
    overlays: [],
    panels: [{ name: "OBV", values: o, color: "#4CAF50", type: "line" }],
    signals: []
  };
}
""".trimIndent()),
    IndicatorTemplateItem("RSI + MACD Combo", "RSI and MACD in separate panels with signals", """
function indicator(candles) {
  var rsi = rsiSeries(candles, 14);
  var m = macdSeries(candles, 12, 26, 9);
  var signals = [];
  for (var i = 1; i < rsi.length; i++) {
    if (rsi[i] !== null && rsi[i-1] !== null && m.macd[i] !== null && m.signal[i] !== null) {
      if (rsi[i-1] < 30 && rsi[i] >= 30 && m.macd[i] > m.signal[i])
        signals.push({ type: "BUY", index: i, price: candles[i].close });
      else if (rsi[i-1] > 70 && rsi[i] <= 70 && m.macd[i] < m.signal[i])
        signals.push({ type: "SELL", index: i, price: candles[i].close });
    }
  }
  return {
    overlays: [],
    panels: [
      { name: "RSI(14)", values: rsi, color: "#E040FB", type: "line",
        lines: [{ value: 70, color: "#EF535080" }, { value: 30, color: "#26A69A80" }] },
      { name: "MACD", values: m.histogram, color: "#26A69A", type: "histogram",
        extraLines: [
          { name: "MACD Line", values: m.macd, color: "#2196F3" },
          { name: "Signal", values: m.signal, color: "#FF9800" }
        ] }
    ],
    signals: signals
  };
}
""".trimIndent())
)

/** Default template code for new custom indicators */
const val INDICATOR_TEMPLATE = """
function indicator(candles) {
  var fast = smaSeries(candles, 10);
  var slow = smaSeries(candles, 50);

  return {
    overlays: [
      { name: "Fast SMA(10)", values: fast, color: "#2196F3", lineWidth: 2 },
      { name: "Slow SMA(50)", values: slow, color: "#FF9800", lineWidth: 2 }
    ],
    panels: [],
    signals: []
  };
}
"""

data class IndicatorSettings(
    // Moving Averages
    val smaPeriod: Int = 20,
    val emaPeriod: Int = 20,
    val wmaPeriod: Int = 20,
    val vwmaPeriod: Int = 20,
    val hullMaPeriod: Int = 14,
    val demaPeriod: Int = 20,
    val temaPeriod: Int = 20,
    // Bands
    val bollingerPeriod: Int = 20,
    val bollingerMult: Double = 2.0,
    // MACD
    val macdFast: Int = 12,
    val macdSlow: Int = 26,
    val macdSignal: Int = 9,
    // RSI
    val rsiPeriod: Int = 14,
    // Supertrend
    val supertrendPeriod: Int = 10,
    val supertrendMult: Double = 3.0,
    // Ichimoku
    val ichimokuTenkan: Int = 9,
    val ichimokuKijun: Int = 26,
    val ichimokuSenkouB: Int = 52,
    // Stochastic
    val stochK: Int = 14,
    val stochD: Int = 3,
    // Stochastic RSI
    val stochRsiPeriod: Int = 14,
    val stochRsiStoch: Int = 14,
    val stochRsiK: Int = 3,
    val stochRsiD: Int = 3,
    // Others
    val atrPeriod: Int = 14,
    val adxPeriod: Int = 14,
    val cciPeriod: Int = 20,
    val williamsRPeriod: Int = 14,
    val mfiPeriod: Int = 14,
    val cmfPeriod: Int = 20
)

/** A custom indicator line computed from a script — rendered as overlay on chart */
data class CustomIndicatorLine(
    val name: String,
    val values: List<Double?>,  // one value per candle index, null = no data
    val color: Long,            // ARGB color as Long (0xFFRRGGBB)
    val isOverlay: Boolean = true,  // true = overlay on price, false = separate pane
    val lineWidth: Float = 1.5f
)

/** Dashboard data for TFG ALGO's 10-factor confluence scoring. */
data class TfgDashboardData(
    val score: Int = 0,
    val cTrend: Boolean = false,
    val cVwap: Boolean = false,
    val cVolume: Boolean = false,
    val cRsi: Boolean = false,
    val cMacd: Boolean = false,
    val cCandle: Boolean = false,
    val cSupport: Boolean = false,
    val cDi: Boolean = false,
    val cAdx: Boolean = false,
    val cBreakout: Boolean = false,
    val rsiVal: Double = 0.0,
    val adxVal: Double = 0.0,
    val diPlus: Double = 0.0,
    val diMinus: Double = 0.0,
    val htfBull: Boolean = true,
    val inPosition: Boolean = false,
    val pnlPct: Double = 0.0,
    val supportLevel: Double? = null,
    val resistanceLevel: Double? = null,
    val bullEngulf: Boolean = false,
    val bullPinBar: Boolean = false,
    val strongBull: Boolean = false
)

data class CoinDetailUiState(
    val symbol: String = "",
    val candles: List<Candle> = emptyList(),
    val signalMarkers: List<SignalMarker> = emptyList(),
    val ticker: Ticker? = null,
    val interval: String = "1h",
    val isLoading: Boolean = false,
    val error: String? = null,
    // Overlay indicator toggles
    val showSma: Boolean = false,
    val showEma: Boolean = false,
    val showWma: Boolean = false,
    val showVwma: Boolean = false,
    val showHullMa: Boolean = false,
    val showDema: Boolean = false,
    val showTema: Boolean = false,
    val showBollinger: Boolean = false,
    val showVwap: Boolean = false,
    val showSupertrend: Boolean = false,
    val showIchimoku: Boolean = false,
    val showVolume: Boolean = true,
    // Sub-chart indicator toggles
    val showRsi: Boolean = false,
    val showMacd: Boolean = false,
    val showStochastic: Boolean = false,
    val showStochRsi: Boolean = false,
    val showAtr: Boolean = false,
    val showAdx: Boolean = false,
    val showCci: Boolean = false,
    val showWilliamsR: Boolean = false,
    val showObv: Boolean = false,
    val showMfi: Boolean = false,
    val showCmf: Boolean = false,
    // Editable settings
    val indicatorSettings: IndicatorSettings = IndicatorSettings(),
    val showSettingsDialog: Boolean = false,
    val editingIndicator: String? = null,
    // Drawing tools
    val showDrawingTools: Boolean = false,
    // Custom indicators from scripts
    val customIndicators: List<CustomIndicatorLine> = emptyList(),
    // Chart type
    val chartType: String = "candle", // candle, line, area, bar, heikinashi
    // Custom indicator system (user-written JS indicators)
    val userIndicators: List<Indicator> = emptyList(),
    val indicatorOutputs: List<IndicatorOutput> = emptyList(),
    val showIndicatorEditor: Boolean = false,
    val editingIndicatorId: String? = null,
    val indicatorEditorCode: String = INDICATOR_TEMPLATE,
    val indicatorEditorName: String = "",
    val indicatorError: String? = null,
    val showIndicatorHelp: Boolean = false,
    /** Extracted input() parameter definitions from user code */
    val indicatorInputs: List<IndicatorInputDef> = emptyList(),
    /** Current input values set by user in the settings UI */
    val indicatorInputValues: Map<String, String> = emptyMap(),
    /** Live preview auto-run toggle */
    val livePreviewEnabled: Boolean = true,
    // Script panel
    val showScriptPanel: Boolean = false,
    val scriptCode: String = "",
    val scriptTemplates: List<StrategyTemplate> = emptyList(),
    val selectedTemplateId: StrategyTemplateId? = null,
    val scriptParams: Map<String, String> = emptyMap(),
    val showScriptSettings: Boolean = false,
    val backtestResult: BacktestResult? = null,
    val isBacktesting: Boolean = false,
    val backtestProgress: Float = 0f,
    val scriptSaved: Boolean = false,
    val backtestDays: Int = 30,
    // Backtest vs Live comparison
    val liveEquityCurve: List<Double> = emptyList(),
    val showBacktestComparison: Boolean = false,
    // Buy & Hold vs Strategy comparison
    val showBuyAndHoldComparison: Boolean = false,
    val buyAndHoldEquityCurve: List<Double> = emptyList(),
    val strategyEquityCurve: List<Double> = emptyList(),
    // Backtest-derived signal markers for replay
    val backtestSignalMarkers: List<SignalMarker> = emptyList(),
    // Replay mode
    val replayMode: Boolean = false,
    val replayBarIndex: Int = 0,
    val replayPlaying: Boolean = false,
    val replaySpeedMs: Long = 500L,
    val replaySignals: List<SignalMarker> = emptyList(),
    // TFG ALGO dashboard
    val tfgDashboard: TfgDashboardData? = null,
    // Strategy overlay data for chart WebView
    val strategyPlotJson: String? = null,
    val dashboardOverlayJson: String? = null,
    // When true, loadSignalMarkers() skips DB updates to preserve backtest markers
    val isBacktestActive: Boolean = false,
    // Drawings loaded from Room DB to inject into chart after symbol load
    val savedDrawingsJson: String? = null
)

@HiltViewModel
class CoinDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val marketRepository: MarketRepository,
    private val scriptRepository: ScriptRepository,
    private val indicatorRepository: IndicatorRepository,
    private val indicatorExecutor: IndicatorExecutor,
    private val scriptExecutor: ScriptExecutor,
    private val drawingSnapshotDao: DrawingSnapshotDao,
    private val alertRepository: AlertRepository
) : ViewModel() {

    private val symbol: String = savedStateHandle["symbol"] ?: "BTCUSDT"

    private val _state = MutableStateFlow(CoinDetailUiState(symbol = symbol))
    val state: StateFlow<CoinDetailUiState> = _state

    private var livePreviewJob: Job? = null
    private var candleJob: Job? = null
    private var markerJob: Job? = null

    init {
        loadData()
        startTickerRefresh()
        loadScriptTemplates()
        loadUserIndicators()
        loadDrawings()
    }

    /** Load saved drawings from Room for the current symbol and inject into chart state. */
    private fun loadDrawings() {
        viewModelScope.launch(Dispatchers.IO) {
            val snapshot = drawingSnapshotDao.getForSymbol(symbol)
            _state.update { it.copy(savedDrawingsJson = snapshot?.drawingsJson) }
        }
    }

    /** Called from the JS bridge when drawings change (add/undo/clear). Saves to Room. */
    fun onDrawingsSync(json: String) {
        viewModelScope.launch(Dispatchers.IO) {
            drawingSnapshotDao.upsert(DrawingSnapshotEntity(symbol = symbol, drawingsJson = json))
        }
    }

    /**
     * Called from JS bridge when a drawing is added.
     * For alert-type drawings, also persists an [AlertEntity] so the trading
     * engine can fire a price-alert notification when the price is reached.
     */
    fun onDrawingAdded(type: String, dataJson: String) {
        if (type == "alert") {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val obj = org.json.JSONObject(dataJson)
                    val price = obj.getDouble("price")
                    val currentPrice = _state.value.ticker?.price ?: 0.0
                    val condition = if (currentPrice < price) AlertCondition.CROSSES_ABOVE else AlertCondition.CROSSES_BELOW
                    val alert = Alert(
                        id = UUID.randomUUID().toString(),
                        symbol = symbol,
                        name = "Price Alert @ ${String.format("%.6f", price).trimEnd('0').trimEnd('.')}",
                        type = AlertType.PRICE,
                        condition = condition,
                        targetValue = price,
                        isEnabled = true,
                        isRepeating = false,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    alertRepository.saveAlert(alert)
                } catch (_: Exception) {}
            }
        }
    }

    fun changeInterval(interval: String) {
        _state.update { it.copy(interval = interval, isBacktestActive = false, backtestResult = null) }
        loadCandles()
        loadSignalMarkers()
    }

    // ─── Indicator toggles (long-press = open settings) ─────────────

    fun toggleSma() { _state.update { it.copy(showSma = !it.showSma) } }
    fun toggleEma() { _state.update { it.copy(showEma = !it.showEma) } }
    fun toggleWma() { _state.update { it.copy(showWma = !it.showWma) } }
    fun toggleVwma() { _state.update { it.copy(showVwma = !it.showVwma) } }
    fun toggleHullMa() { _state.update { it.copy(showHullMa = !it.showHullMa) } }
    fun toggleDema() { _state.update { it.copy(showDema = !it.showDema) } }
    fun toggleTema() { _state.update { it.copy(showTema = !it.showTema) } }
    fun toggleBollinger() { _state.update { it.copy(showBollinger = !it.showBollinger) } }
    fun toggleVwap() { _state.update { it.copy(showVwap = !it.showVwap) } }
    fun toggleSupertrend() { _state.update { it.copy(showSupertrend = !it.showSupertrend) } }
    fun toggleIchimoku() { _state.update { it.copy(showIchimoku = !it.showIchimoku) } }
    fun toggleVolume() { _state.update { it.copy(showVolume = !it.showVolume) } }
    fun toggleRsi() { _state.update { it.copy(showRsi = !it.showRsi) } }
    fun toggleMacd() { _state.update { it.copy(showMacd = !it.showMacd) } }
    fun toggleStochastic() { _state.update { it.copy(showStochastic = !it.showStochastic) } }
    fun toggleStochRsi() { _state.update { it.copy(showStochRsi = !it.showStochRsi) } }
    fun toggleAtr() { _state.update { it.copy(showAtr = !it.showAtr) } }
    fun toggleAdx() { _state.update { it.copy(showAdx = !it.showAdx) } }
    fun toggleCci() { _state.update { it.copy(showCci = !it.showCci) } }
    fun toggleWilliamsR() { _state.update { it.copy(showWilliamsR = !it.showWilliamsR) } }
    fun toggleObv() { _state.update { it.copy(showObv = !it.showObv) } }
    fun toggleMfi() { _state.update { it.copy(showMfi = !it.showMfi) } }
    fun toggleCmf() { _state.update { it.copy(showCmf = !it.showCmf) } }
    fun toggleDrawingTools() { _state.update { it.copy(showDrawingTools = !it.showDrawingTools) } }

    fun toggleBacktestComparison() {
        val current = _state.value
        if (!current.showBacktestComparison && current.backtestResult != null) {
            // Generate simulated live equity curve based on actual candle data
            // Uses buy & hold as a proxy for "live" performance
            val candles = current.candles
            if (candles.size >= 2) {
                val startPrice = candles.first().close
                val startingCapital = current.backtestResult.startingCapital
                val liveEquity = candles.map { c ->
                    startingCapital * (c.close / startPrice)
                }
                _state.update { it.copy(showBacktestComparison = true, liveEquityCurve = liveEquity) }
            } else {
                _state.update { it.copy(showBacktestComparison = true) }
            }
        } else {
            _state.update { it.copy(showBacktestComparison = !it.showBacktestComparison) }
        }
    }

    fun openSettings(indicator: String) {
        _state.update { it.copy(showSettingsDialog = true, editingIndicator = indicator) }
    }

    // ─── Replay Mode — Uses backtest signals (mirrors backtest exactly) ───

    private var replayJob: Job? = null

    fun enterReplayMode() {
        val s = _state.value
        val candles = s.candles
        // Require a backtest to have been run first
        if (candles.size < 10 || s.backtestResult == null || s.backtestSignalMarkers.isEmpty()) return
        replayJob?.cancel()
        val startBar = 50.coerceAtMost(candles.size - 1)
        val startTime = candles[startBar].openTime
        val visibleSignals = s.backtestSignalMarkers.filter { it.openTime <= startTime }
        _state.update {
            it.copy(
                replayMode = true,
                replayBarIndex = startBar,
                replayPlaying = false,
                replaySignals = visibleSignals
            )
        }
    }

    fun exitReplayMode() {
        replayJob?.cancel()
        _state.update {
            it.copy(
                replayMode = false,
                replayPlaying = false,
                replayBarIndex = 0,
                replaySignals = emptyList()
            )
        }
    }

    fun replayStepForward() {
        val s = _state.value
        if (s.replayBarIndex >= s.candles.size - 1) return
        val newIdx = s.replayBarIndex + 1
        val barTime = s.candles[newIdx].openTime
        val visibleSignals = s.backtestSignalMarkers.filter { it.openTime <= barTime }
        _state.update { it.copy(replayBarIndex = newIdx, replaySignals = visibleSignals) }
    }

    fun replayStepBack() {
        val s = _state.value
        if (s.replayBarIndex <= 1) return
        val newIdx = s.replayBarIndex - 1
        val barTime = s.candles[newIdx].openTime
        val visibleSignals = s.backtestSignalMarkers.filter { it.openTime <= barTime }
        _state.update { it.copy(replayBarIndex = newIdx, replaySignals = visibleSignals) }
    }

    fun replayPlay() {
        _state.update { it.copy(replayPlaying = true) }
        replayJob?.cancel()
        replayJob = viewModelScope.launch {
            while (_state.value.replayPlaying) {
                val s = _state.value
                if (s.replayBarIndex >= s.candles.size - 1) {
                    _state.update { it.copy(replayPlaying = false) }
                    break
                }
                replayStepForward()
                delay(_state.value.replaySpeedMs)
            }
        }
    }

    fun replayPause() {
        replayJob?.cancel()
        _state.update { it.copy(replayPlaying = false) }
    }

    fun setReplaySpeed(speedMs: Long) {
        _state.update { it.copy(replaySpeedMs = speedMs) }
    }

    fun seekReplay(barIndex: Int) {
        val s = _state.value
        val clamped = barIndex.coerceIn(1, s.candles.size - 1)
        val barTime = s.candles[clamped].openTime
        val visibleSignals = s.backtestSignalMarkers.filter { it.openTime <= barTime }
        _state.update { it.copy(replayBarIndex = clamped, replaySignals = visibleSignals) }
    }

    // ─── Buy & Hold Comparison ───────────────────────────────────────

    fun toggleBuyAndHoldComparison() {
        val s = _state.value
        if (s.showBuyAndHoldComparison) {
            _state.update { it.copy(showBuyAndHoldComparison = false, buyAndHoldEquityCurve = emptyList(), strategyEquityCurve = emptyList()) }
            return
        }
        val result = s.backtestResult ?: return
        val candles = s.candles
        if (candles.isEmpty()) return
        val startPrice = candles.first().close
        val startCapital = result.entryAmount
        val bhCurve = candles.map { c -> startCapital * (c.close / startPrice) }
        _state.update {
            it.copy(
                showBuyAndHoldComparison = true,
                buyAndHoldEquityCurve = bhCurve,
                strategyEquityCurve = result.equityCurve
            )
        }
    }

    fun closeSettings() {
        _state.update { it.copy(showSettingsDialog = false, editingIndicator = null) }
    }

    fun updateSettings(new: IndicatorSettings) {
        _state.update { it.copy(indicatorSettings = new) }
        // Recompute custom indicators that depend on settings
        recomputeCustomIndicators()
    }

    // ─── Script panel ────────────────────────────────────────────────

    fun toggleScriptPanel() {
        _state.update { it.copy(showScriptPanel = !it.showScriptPanel) }
    }

    fun updateScriptCode(code: String) {
        val extracted = extractParamsFromCode(code)
        _state.update {
            val merged = if (extracted.isNotEmpty()) extracted else it.scriptParams
            it.copy(scriptCode = code, scriptSaved = false, scriptParams = merged)
        }
    }

    fun loadScriptTemplate(template: StrategyTemplate) {
        _state.update {
            it.copy(
                scriptCode = template.code,
                selectedTemplateId = template.id,
                scriptParams = template.defaultParams,
                showScriptSettings = template.defaultParams.isNotEmpty(),
                scriptSaved = false,
                backtestResult = null
            )
        }
    }

    fun updateScriptParam(key: String, value: String) {
        _state.update {
            val newParams = it.scriptParams + (key to value)
            val newCode = injectParamIntoCode(it.scriptCode, key, value)
            it.copy(scriptParams = newParams, scriptCode = newCode)
        }
    }

    fun toggleScriptSettings() {
        _state.update { it.copy(showScriptSettings = !it.showScriptSettings) }
    }

    fun updateBacktestDays(days: Int) {
        _state.update { it.copy(backtestDays = days) }
    }

    fun saveAndActivateScript() {
        val code = _state.value.scriptCode
        val templateId = _state.value.selectedTemplateId?.name
            ?: _state.value.scriptTemplates.firstOrNull { it.code == code }?.id?.name
        if (templateId == null) {
            _state.update { it.copy(error = "Select a strategy template first") }
            return
        }
        viewModelScope.launch {
            try {
                val script = Script(
                    id = UUID.randomUUID().toString(),
                    name = "Chart Strategy",
                    code = code,
                    strategyTemplateId = templateId,
                    params = _state.value.scriptParams,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                scriptRepository.save(script)
                scriptRepository.activateStrategy(script.id, symbol)
                _state.update { it.copy(scriptSaved = true, error = null) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    private var backtestJob: Job? = null

    fun cancelBacktest() {
        backtestJob?.cancel()
        backtestJob = null
        _state.update { it.copy(isBacktesting = false, backtestProgress = 0f, error = null) }
    }

    fun runChartBacktest() {
        val code = _state.value.scriptCode
        val templateId = _state.value.selectedTemplateId?.name
            ?: _state.value.scriptTemplates.firstOrNull { it.code == code }?.id?.name
        if (templateId == null) {
            _state.update { it.copy(error = "Select a strategy template first") }
            return
        }
        backtestJob?.cancel()
        backtestJob = viewModelScope.launch {
            _state.update { it.copy(isBacktesting = true, backtestProgress = 0f, error = null) }
            try {
                // Auto-save current code + params before running
                val tempId = "backtest_${System.currentTimeMillis()}"
                val script = Script(
                    id = tempId,
                    name = "Backtest",
                    code = code,
                    strategyTemplateId = templateId,
                    params = _state.value.scriptParams
                )
                scriptRepository.save(script)
                _state.update { it.copy(scriptSaved = true) }
                val result = scriptRepository.backtest(
                    tempId, templateId, symbol, _state.value.interval, _state.value.backtestDays,
                    onProgress = { progress ->
                        _state.update { it.copy(backtestProgress = progress) }
                    }
                )
                // Load actual signal markers from DB (backtest already persisted them with proper types: BUY, PAT_BUY, TP_HIT, SL_HIT, CLOSE)
                val markers = scriptRepository.getSignalMarkers(symbol, _state.value.interval).first()
                _state.update { it.copy(backtestResult = result, isBacktesting = false, backtestSignalMarkers = markers, signalMarkers = markers, isBacktestActive = true) }
                refreshTfgDashboard()
                // Reload candles from DB (backtest already stored them) — don't re-fetch from API
                loadBacktestCandles()
            } catch (e: Exception) {
                _state.update { it.copy(isBacktesting = false, error = e.message) }
            }
        }
    }

    private fun loadScriptTemplates() {
        val templates = StrategyTemplates.getAll()
        val first = templates.first()
        _state.update {
            it.copy(
                scriptTemplates = templates,
                scriptCode = first.code,
                selectedTemplateId = first.id,
                scriptParams = first.defaultParams
            )
        }
    }

    // ─── Data loading ────────────────────────────────────────────────

    private fun loadData() {
        loadCandles()
        loadSignalMarkers()
        loadCustomIndicators()
    }

    private fun loadCandles() {
        candleJob?.cancel()
        candleJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                marketRepository.getCandles(symbol, _state.value.interval, 500)
                    .collect { candles ->
                        _state.update { it.copy(candles = candles, isLoading = false) }
                        recomputeCustomIndicators()
                        recomputeIndicatorOutputs()
                        refreshTfgDashboard()
                    }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load candles for $symbol")
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun loadBacktestCandles() {
        candleJob?.cancel()
        candleJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                marketRepository.getCandlesFromDb(symbol, _state.value.interval)
                    .collect { candles ->
                        _state.update { it.copy(candles = candles, isLoading = false) }
                        recomputeCustomIndicators()
                        recomputeIndicatorOutputs()
                        refreshTfgDashboard()
                    }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load backtest candles for $symbol")
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun loadSignalMarkers() {
        markerJob?.cancel()
        markerJob = viewModelScope.launch {
            try {
                scriptRepository.getSignalMarkers(symbol, _state.value.interval)
                    .collect { markers ->
                        // Don't overwrite backtest-generated markers with DB markers
                        if (!_state.value.isBacktestActive) {
                            _state.update { it.copy(signalMarkers = markers) }
                        }
                    }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load signal markers")
            }
        }
    }

    /** Evaluate strategy on current candles to populate plot overlays and the dashboard score panel. */
    private fun refreshTfgDashboard() {
        val s = _state.value
        if (s.selectedTemplateId == null) {
            if (s.tfgDashboard != null || s.strategyPlotJson != null || s.dashboardOverlayJson != null) {
                _state.update { it.copy(tfgDashboard = null, strategyPlotJson = null, dashboardOverlayJson = null) }
            }
            return
        }
        val candles = s.candles
        if (candles.size < 60) return
        viewModelScope.launch {
            try {
                val code = s.scriptCode.ifBlank {
                    StrategyTemplates.getAll().firstOrNull { it.id == s.selectedTemplateId }?.code ?: return@launch
                }
                scriptExecutor.evaluate(code, candles, s.scriptParams)

                // Capture strategy plot data (overlays/panels plotted via plot() API)
                val plotJson = scriptExecutor.getLastPlotData()

                // Dashboard — only TFG ALGO has a structured dashboard
                if (s.selectedTemplateId == StrategyTemplateId.TFG_ALGO) {
                    val json = scriptExecutor.getLastDashboard()
                    val dash = if (json != null) parseTfgDashboard(json) else null
                    _state.update { it.copy(
                        tfgDashboard = dash,
                        strategyPlotJson = plotJson,
                        dashboardOverlayJson = if (dash != null) buildDashboardOverlayJson(dash) else null
                    )}
                } else {
                    _state.update { it.copy(
                        tfgDashboard = null,
                        strategyPlotJson = plotJson,
                        dashboardOverlayJson = null
                    )}
                }
            } catch (e: Exception) {
                Timber.w(e, "Failed to refresh strategy overlay")
            }
        }
    }

    private fun parseTfgDashboard(json: String): TfgDashboardData? {
        return try {
            val obj = org.json.JSONObject(json)
            TfgDashboardData(
                score = obj.optInt("score", 0),
                cTrend = obj.optInt("c_trend") == 1,
                cVwap = obj.optInt("c_vwap") == 1,
                cVolume = obj.optInt("c_volume") == 1,
                cRsi = obj.optInt("c_rsi") == 1,
                cMacd = obj.optInt("c_macd") == 1,
                cCandle = obj.optInt("c_candle") == 1,
                cSupport = obj.optInt("c_support") == 1,
                cDi = obj.optInt("c_di") == 1,
                cAdx = obj.optInt("c_adx") == 1,
                cBreakout = obj.optInt("c_breakout") == 1,
                rsiVal = obj.optDouble("rsiVal", 0.0),
                adxVal = obj.optDouble("adxVal", 0.0),
                diPlus = obj.optDouble("diPlus", 0.0),
                diMinus = obj.optDouble("diMinus", 0.0),
                htfBull = obj.optInt("htfBull", 1) == 1,
                inPosition = obj.optInt("inPosition") == 1,
                pnlPct = obj.optDouble("pnlPct", 0.0),
                supportLevel = if (obj.has("supportLevel") && !obj.isNull("supportLevel")) obj.optDouble("supportLevel") else null,
                resistanceLevel = if (obj.has("resistanceLevel") && !obj.isNull("resistanceLevel")) obj.optDouble("resistanceLevel") else null,
                bullEngulf = obj.optInt("bullEngulf") == 1,
                bullPinBar = obj.optInt("bullPinBar") == 1,
                strongBull = obj.optInt("strongBull") == 1
            )
        } catch (e: Exception) {
            Timber.w(e, "Failed to parse TFG dashboard JSON")
            null
        }
    }

    /** Convert TfgDashboardData into the JSON format the chart WebView overlay expects. */
    private fun buildDashboardOverlayJson(dash: TfgDashboardData): String {
        val rows = org.json.JSONArray()
        fun addRow(label: String, pass: Boolean, status: String) {
            rows.put(org.json.JSONObject().put("label", label).put("pass", pass).put("status", status))
        }
        addRow("Trend", dash.cTrend, if (dash.cTrend) "Bullish" else "Bearish")
        addRow("VWAP", dash.cVwap, if (dash.cVwap) "Above" else "Below")
        addRow("Volume", dash.cVolume, if (dash.cVolume) "Strong" else "Weak")
        addRow("RSI", dash.cRsi, String.format("%.1f", dash.rsiVal))
        addRow("MACD", dash.cMacd, if (dash.cMacd) "Bullish" else "Bearish")
        addRow("Candle", dash.cCandle, if (dash.cCandle) "Bullish" else "Neutral")
        addRow("Support", dash.cSupport, if (dash.cSupport) "Near" else "Away")
        addRow("DI+/DI-", dash.cDi, String.format("%.1f/%.1f", dash.diPlus, dash.diMinus))
        addRow("ADX", dash.cAdx, String.format("%.1f", dash.adxVal))
        addRow("Breakout", dash.cBreakout, if (dash.cBreakout) "Yes" else "No")
        return org.json.JSONObject().apply {
            put("title", "TFG ALGO")
            put("score", dash.score)
            put("maxScore", 10)
            put("rows", rows)
            put("inPosition", dash.inPosition)
            put("pnlPct", dash.pnlPct)
            put("htfBull", dash.htfBull)
        }.toString()
    }

    private fun loadCustomIndicators() {
        viewModelScope.launch {
            try {
                scriptRepository.getActiveStrategy().collect { script ->
                    if (script != null) {
                        recomputeCustomIndicators()
                    } else {
                        _state.update { it.copy(customIndicators = emptyList()) }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load custom indicators")
            }
        }
    }

    private fun recomputeCustomIndicators() {
        val candles = _state.value.candles
        if (candles.isEmpty()) return

        viewModelScope.launch {
            try {
                scriptRepository.getActiveStrategy().first()?.let { script ->
                    val indicators = computeScriptIndicators(script, candles)
                    _state.update { it.copy(customIndicators = indicators) }
                }
            } catch (_: Exception) { }
        }
    }

    /**
     * Parses the script code for `indicator()` calls and computes them.
     * Script format:
     *   indicator("My SMA", sma, 50, 0xFF00FF00)
     *   indicator("My EMA", ema, 21, 0xFFFF00FF)
     *   indicator("RSI Line", rsi, 7, 0xFFFFAA00, overlay=false)
     */
    private fun computeScriptIndicators(script: Script, candles: List<Candle>): List<CustomIndicatorLine> {
        val result = mutableListOf<CustomIndicatorLine>()
        val code = script.code
        val indicatorRegex = Regex(
            """indicator\s*\(\s*"([^"]+)"\s*,\s*(\w+)\s*,\s*(\d+)\s*(?:,\s*(0x[0-9a-fA-F]+))?\s*(?:,\s*overlay\s*=\s*(true|false))?\s*\)""",
            RegexOption.IGNORE_CASE
        )
        for (match in indicatorRegex.findAll(code)) {
            val name = match.groupValues[1]
            val type = match.groupValues[2].lowercase()
            val period = match.groupValues[3].toIntOrNull() ?: 20
            val colorHex = match.groupValues[4].ifBlank { null }
            val isOverlay = match.groupValues[5].ifBlank { "true" } != "false"

            val colorLong = colorHex?.let {
                try { java.lang.Long.decode(it) } catch (_: Exception) { 0xFFFFFFFF }
            } ?: 0xFF00BFFF  // default: light blue

            val values: List<Double?> = when (type) {
                "sma" -> StrategyEvaluator.smaSeries(candles, period)
                "ema" -> StrategyEvaluator.emaSeries(candles, period)
                "rsi" -> StrategyEvaluator.rsiSeries(candles, period).map { it }
                else -> continue
            }

            result.add(
                CustomIndicatorLine(
                    name = name,
                    values = values,
                    color = colorLong,
                    isOverlay = isOverlay
                )
            )
        }
        return result
    }

    private fun startTickerRefresh() {
        viewModelScope.launch {
            while (true) {
                try {
                    marketRepository.getTicker(symbol).collect { ticker ->
                        _state.update { it.copy(ticker = ticker) }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Ticker refresh failed")
                }
                delay(5_000)
            }
        }
    }

    // ─── Custom Indicator System ─────────────────────────────────────

    private fun loadUserIndicators() {
        viewModelScope.launch {
            indicatorRepository.getAllIndicators().collect { indicators ->
                _state.update { it.copy(userIndicators = indicators) }
                recomputeIndicatorOutputs()
            }
        }
    }

    private fun recomputeIndicatorOutputs() {
        val candles = _state.value.candles
        if (candles.isEmpty()) return
        val enabledIndicators = _state.value.userIndicators.filter { it.isEnabled }
        if (enabledIndicators.isEmpty()) {
            _state.update { it.copy(indicatorOutputs = emptyList()) }
            return
        }
        viewModelScope.launch {
            val outputs = enabledIndicators.mapNotNull { indicator ->
                try {
                    indicatorExecutor.evaluate(indicator.code, candles)
                } catch (e: Exception) {
                    Timber.w(e, "Failed to evaluate indicator: ${indicator.name}")
                    null
                }
            }
            _state.update { it.copy(indicatorOutputs = outputs) }
        }
    }

    fun toggleIndicatorEditor() {
        _state.update { it.copy(showIndicatorEditor = !it.showIndicatorEditor) }
    }

    fun openIndicatorEditor(indicatorId: String? = null) {
        if (indicatorId != null) {
            val existing = _state.value.userIndicators.find { it.id == indicatorId }
            if (existing != null) {
                val inputs = extractInputDefs(existing.code)
                val inputValues = inputs.associate { it.name to it.defaultValue }
                _state.update {
                    it.copy(
                        showIndicatorEditor = true,
                        editingIndicatorId = indicatorId,
                        indicatorEditorCode = existing.code,
                        indicatorEditorName = existing.name,
                        indicatorInputs = inputs,
                        indicatorInputValues = inputValues
                    )
                }
                return
            }
        }
        val defaultInputs = extractInputDefs(INDICATOR_TEMPLATE)
        _state.update {
            it.copy(
                showIndicatorEditor = true,
                editingIndicatorId = null,
                indicatorEditorCode = INDICATOR_TEMPLATE,
                indicatorEditorName = "",
                indicatorInputs = defaultInputs,
                indicatorInputValues = defaultInputs.associate { d -> d.name to d.defaultValue }
            )
        }
    }

    fun closeIndicatorEditor() {
        _state.update { it.copy(showIndicatorEditor = false, editingIndicatorId = null, indicatorError = null) }
    }

    fun updateIndicatorCode(code: String) {
        val inputs = extractInputDefs(code)
        // Preserve existing user values, fill new ones with defaults
        val currentValues = _state.value.indicatorInputValues
        val newValues = inputs.associate { def ->
            def.name to (currentValues[def.name] ?: def.defaultValue)
        }
        _state.update {
            it.copy(
                indicatorEditorCode = code,
                indicatorError = null,
                indicatorInputs = inputs,
                indicatorInputValues = newValues
            )
        }
        // Live preview debounce
        if (_state.value.livePreviewEnabled) {
            livePreviewJob?.cancel()
            livePreviewJob = viewModelScope.launch {
                delay(800)
                previewIndicator()
            }
        }
    }

    fun updateInputValue(name: String, value: String) {
        val updatedValues = _state.value.indicatorInputValues.toMutableMap()
        updatedValues[name] = value
        _state.update { it.copy(indicatorInputValues = updatedValues) }
        // Re-trigger live preview if enabled
        if (_state.value.livePreviewEnabled) {
            livePreviewJob?.cancel()
            livePreviewJob = viewModelScope.launch {
                delay(400)
                previewIndicator()
            }
        }
    }

    fun toggleLivePreview() {
        _state.update { it.copy(livePreviewEnabled = !it.livePreviewEnabled) }
    }

    fun exportIndicatorJson(): String {
        val name = _state.value.indicatorEditorName.ifBlank { "Custom Indicator" }
        val code = _state.value.indicatorEditorCode
        val inputValues = _state.value.indicatorInputValues
        val json = org.json.JSONObject()
        json.put("name", name)
        json.put("code", code)
        json.put("version", 1)
        if (inputValues.isNotEmpty()) {
            val inputsJson = org.json.JSONObject()
            inputValues.forEach { (k, v) -> inputsJson.put(k, v) }
            json.put("inputs", inputsJson)
        }
        return json.toString(2)
    }

    fun importIndicatorJson(jsonStr: String): Boolean {
        return try {
            val json = org.json.JSONObject(jsonStr)
            val name = json.optString("name", "Imported Indicator")
            val code = json.optString("code", "")
            if (code.isBlank()) return false
            val inputsObj = json.optJSONObject("inputs")
            val inputValues = mutableMapOf<String, String>()
            inputsObj?.keys()?.forEach { key ->
                inputValues[key] = inputsObj.optString(key, "")
            }
            _state.update {
                it.copy(
                    indicatorEditorName = name,
                    indicatorEditorCode = code,
                    indicatorInputValues = inputValues,
                    indicatorInputs = extractInputDefs(code),
                    indicatorError = null
                )
            }
            true
        } catch (e: Exception) {
            _state.update { it.copy(indicatorError = "Import failed: ${e.message}") }
            false
        }
    }

    fun loadIndicatorTemplate(template: IndicatorTemplateItem) {
        _state.update {
            it.copy(
                indicatorEditorCode = template.code,
                indicatorEditorName = template.name,
                indicatorError = null
            )
        }
    }

    fun toggleIndicatorHelp() {
        _state.update { it.copy(showIndicatorHelp = !it.showIndicatorHelp) }
    }

    fun setChartType(type: String) {
        _state.update { it.copy(chartType = type) }
    }

    fun updateIndicatorName(name: String) {
        _state.update { it.copy(indicatorEditorName = name) }
    }

    fun saveIndicator() {
        val name = _state.value.indicatorEditorName.ifBlank { "Custom Indicator" }
        val code = _state.value.indicatorEditorCode
        val existingId = _state.value.editingIndicatorId
        viewModelScope.launch {
            val indicator = Indicator(
                id = existingId ?: UUID.randomUUID().toString(),
                name = name,
                code = code,
                isEnabled = true,
                createdAt = if (existingId != null) {
                    _state.value.userIndicators.find { it.id == existingId }?.createdAt
                        ?: System.currentTimeMillis()
                } else System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            indicatorRepository.saveIndicator(indicator)
            _state.update { it.copy(showIndicatorEditor = false, editingIndicatorId = null) }
        }
    }

    fun deleteIndicator(id: String) {
        viewModelScope.launch { indicatorRepository.deleteIndicator(id) }
    }

    fun toggleIndicatorEnabled(id: String) {
        val current = _state.value.userIndicators.find { it.id == id } ?: return
        viewModelScope.launch { indicatorRepository.setEnabled(id, !current.isEnabled) }
    }

    fun previewIndicator() {
        val code = _state.value.indicatorEditorCode
        val candles = _state.value.candles
        if (candles.isEmpty()) return
        // Inject input values into the code by prepending _inputs object
        val inputValues = _state.value.indicatorInputValues
        val inputsJs = if (inputValues.isNotEmpty()) {
            "var _inputs = {" + inputValues.entries.joinToString(", ") { (k, v) ->
                "\"$k\": $v"
            } + "};\n"
        } else ""
        val fullCode = inputsJs + code
        viewModelScope.launch {
            try {
                val output = indicatorExecutor.evaluate(fullCode, candles)
                val existingOutputs = _state.value.indicatorOutputs.toMutableList()
                existingOutputs.removeAll { out -> out.overlays.any { it.name.startsWith("_preview_") } || out.panels.any { it.name.startsWith("_preview_") } }
                val previewOutput = IndicatorOutput(
                    overlays = output.overlays.map { it.copy(name = "_preview_${it.name}") },
                    panels = output.panels.map { it.copy(name = "_preview_${it.name}") },
                    signals = output.signals,
                    fills = output.fills,
                    hlines = output.hlines,
                    bgcolor = output.bgcolor,
                    labels = output.labels
                )
                existingOutputs.add(previewOutput)
                _state.update { it.copy(indicatorOutputs = existingOutputs, indicatorError = null) }
            } catch (e: Exception) {
                _state.update { it.copy(indicatorError = "Error: ${e.message}") }
            }
        }
    }
}
