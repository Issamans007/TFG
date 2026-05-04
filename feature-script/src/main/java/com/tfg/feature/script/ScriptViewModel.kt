package com.tfg.feature.script

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tfg.domain.model.BacktestResult
import com.tfg.domain.model.Candle
import com.tfg.domain.model.CustomTemplate
import com.tfg.domain.model.MonteCarloResult
import com.tfg.domain.model.Script
import com.tfg.domain.model.ScriptExecutor
import com.tfg.domain.model.SignalMarker
import com.tfg.domain.model.SignalType
import com.tfg.domain.model.StrategyTemplate
import com.tfg.domain.model.StrategyTemplates
import com.tfg.domain.model.extractParamsFromCode
import com.tfg.domain.model.extractParamMeta
import com.tfg.domain.model.validateParam
import com.tfg.domain.model.ParamMeta
import com.tfg.domain.model.injectParamIntoCode
import com.tfg.domain.model.toCsv
import com.tfg.domain.model.runMonteCarlo
import com.tfg.domain.repository.MarketRepository
import com.tfg.domain.repository.ScriptRepository
import com.tfg.domain.service.ConsoleBus
import com.tfg.domain.service.ConsoleSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancelAndJoin
import javax.inject.Inject

val BACKTEST_INTERVALS = listOf("1m", "5m", "15m", "30m", "1h", "4h", "1d")

data class OptimizationParam(
    val name: String,
    val min: Double,
    val max: Double,
    val step: Double
)

data class OptimizationResult(
    val params: Map<String, String>,
    val pnlPercent: Double,
    val winRate: Double,
    val sharpe: Double,
    val maxDrawdown: Double,
    val trades: Int,
    val profitFactor: Double
)

data class ScriptUiState(
    val scripts: List<Script> = emptyList(),
    val selectedScript: Script? = null,
    val activeStrategy: Script? = null,
    val templates: List<StrategyTemplate> = StrategyTemplates.getAll(),
    val customTemplates: List<CustomTemplate> = emptyList(),
    val code: String = DEFAULT_SCRIPT,
    val selectedPair: String = "BTCUSDT",
    val backtestInterval: String = "1h",
    val backtestResult: BacktestResult? = null,
    val isRunning: Boolean = false,
    val backtestProgress: Float = 0f,
    val isActivating: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val syntaxError: String? = null,
    val console: List<String> = emptyList(),
    val editableParams: Map<String, String> = emptyMap(),
    val paramMeta: Map<String, ParamMeta> = emptyMap(),
    val paramErrors: Map<String, String> = emptyMap(),
    val showSettings: Boolean = false,
    val currentTemplateName: String? = null,
    val candles: List<Candle> = emptyList(),
    val signalMarkers: List<SignalMarker> = emptyList(),
    val isLoadingChart: Boolean = false,
    // ── Replay Mode ──
    val replayMode: Boolean = false,
    val replayBarIndex: Int = 0,
    val replayPlaying: Boolean = false,
    val replaySpeedMs: Long = 500L,
    val replaySignals: List<SignalMarker> = emptyList(),
    // ── Buy & Hold Comparison ──
    val showBuyAndHoldComparison: Boolean = false,
    val buyAndHoldEquityCurve: List<Double> = emptyList(),
    val strategyEquityCurve: List<Double> = emptyList(),
    // ── Optimizer ──
    val isOptimizing: Boolean = false,
    val optimizationProgress: Float = 0f,
    val optimizationResults: List<OptimizationResult> = emptyList(),
    val optimizationSortBy: String = "pnlPercent",
    val showOptimizer: Boolean = false,
    val optimizationParams: List<OptimizationParam> = emptyList(),
    // ── Backtest Cost Config (B3) ──
    val backtestMakerFee: Double = 0.1,       // % (default 0.1%)
    val backtestTakerFee: Double = 0.1,       // %
    val backtestSlippagePct: Double = 0.0,     // %
    val backtestStartingCapital: Double = 10_000.0, // USD
    // ── Backtest Market Type / Leverage Override ──
    val backtestMarketType: com.tfg.domain.model.MarketType = com.tfg.domain.model.MarketType.SPOT,
    val backtestLeverage: Int = 1,
    // ── Backtest Date Range (C2) ──
    val backtestStartDateMs: Long? = null,     // null = use days
    val backtestEndDateMs: Long? = null,
    // ── Backtest Export (C5) ──
    val backtestCsvContent: String? = null,
    // ── Monte Carlo (C4) ──
    val monteCarloResult: MonteCarloResult? = null,
    // ── Multi-Symbol Backtest (C1) ──
    val backtestSymbols: List<String> = emptyList(),  // empty = single-symbol mode
    // ── Strategy plot overlays & dashboard (from ScriptExecutor) ──
    val strategyPlotJson: String? = null,
    val dashboardOverlayJson: String? = null
)

val POPULAR_PAIRS = listOf(
    "BTCUSDT", "ETHUSDT", "BNBUSDT", "SOLUSDT", "XRPUSDT",
    "ADAUSDT", "DOGEUSDT", "AVAXUSDT", "DOTUSDT", "MATICUSDT"
)

const val DEFAULT_SCRIPT = """// GainzAlgo V2 — Enhanced Strategy
// Engulfing + stability + EMA trend alignment + volume + RSI + ATR TP/SL
// Optional: MACD histogram, ADX strength (enable for fewer but better signals)

// ── Core ──
var CANDLE_STABILITY = 0.5;  // body/TR min ratio
var RSI_PERIOD = 14;
var RSI_BUY_MAX = 55.0;     // tightened from original 70
var RSI_SELL_MIN = 45.0;    // tightened from original 30
var DELTA_LENGTH = 4;

// ── Trend Filter (EMA alignment only — allows pullback entries) ──
var USE_TREND_FILTER = 1;
var EMA_FAST = 50;
var EMA_SLOW = 200;

// ── Volume Filter ──
var USE_VOLUME_FILTER = 1;
var VOL_LOOKBACK = 20;
var VOL_MULTIPLIER = 1.0;

// ── MACD (off by default — enable for higher quality) ──
var USE_MACD_FILTER = 0;
var MACD_FAST = 12;
var MACD_SLOW = 26;
var MACD_SIGNAL = 9;

// ── ADX (off by default) ──
var USE_ADX_FILTER = 0;
var ADX_PERIOD = 14;
var ADX_THRESHOLD = 20.0;

// ── Risk ──
var TP_SL_MULTIPLIER = 1.0;
var RISK_REWARD_RATIO = 2.0;
var POSITION_SIZE_PCT = 2.0;

function strategy(candles) {
    var minBars = USE_TREND_FILTER ? Math.max(EMA_SLOW + 5, 52) : Math.max(DELTA_LENGTH + 15, 30);
    if (candles.length < minBars) return {type:'HOLD'};

    var last = candles[candles.length - 1];
    var prev = candles[candles.length - 2];
    var deltaRef = candles[candles.length - DELTA_LENGTH - 1];

    // Candle stability
    var tr = Math.max(last.high - last.low,
                      Math.abs(last.high - prev.close),
                      Math.abs(last.low - prev.close));
    var body = Math.abs(last.close - last.open);
    if (!(tr > 0 && body / tr > CANDLE_STABILITY)) return {type:'HOLD'};

    // Engulfing
    var bullEngulf = prev.close < prev.open && last.close > last.open && last.close > prev.open;
    var bearEngulf = prev.close > prev.open && last.close < last.open && last.close < prev.open;
    if (!bullEngulf && !bearEngulf) return {type:'HOLD'};

    var rsiVal = rsi(candles, RSI_PERIOD);

    // Trend filter: EMA alignment only (NOT price > EMA)
    // Allows "buy the dip in an uptrend" — engulfing reversal within larger trend
    var trendBull = true, trendBear = true;
    if (USE_TREND_FILTER) {
        var emaF = ema(candles, EMA_FAST);
        var emaS = ema(candles, EMA_SLOW);
        trendBull = emaF > emaS;  // uptrend
        trendBear = emaF < emaS;  // downtrend
    }

    // Volume filter
    if (USE_VOLUME_FILTER && candles.length > VOL_LOOKBACK) {
        var volSum = 0;
        for (var i = candles.length - VOL_LOOKBACK - 1; i < candles.length - 1; i++)
            volSum += candles[i].volume;
        var avgVol = volSum / VOL_LOOKBACK;
        if (avgVol > 0 && last.volume < avgVol * VOL_MULTIPLIER)
            return {type:'HOLD'};
    }

    // MACD histogram (optional)
    var macdBull = true, macdBear = true;
    if (USE_MACD_FILTER) {
        var m = macd(candles, MACD_FAST, MACD_SLOW, MACD_SIGNAL);
        var mP = macd(candles.slice(0, -1), MACD_FAST, MACD_SLOW, MACD_SIGNAL);
        macdBull = m.histogram > mP.histogram;
        macdBear = m.histogram < mP.histogram;
    }

    // ADX strength (optional)
    if (USE_ADX_FILTER && adx(candles, ADX_PERIOD) < ADX_THRESHOLD)
        return {type:'HOLD'};

    // TP/SL
    var atrVal = atr(candles, 14);
    var slPct = atrVal * TP_SL_MULTIPLIER / last.close * 100;
    var tpPct = slPct * RISK_REWARD_RATIO;

    // BUY: engulf + RSI<55 + dip + EMA uptrend + volume
    if (bullEngulf && rsiVal < RSI_BUY_MAX && last.close < deltaRef.close
        && trendBull && macdBull) {
        return {type:'BUY', sizePct: POSITION_SIZE_PCT,
            stopLossPct: slPct, takeProfitPct: tpPct};
    }

    // SELL: engulf + RSI>45 + rise + EMA downtrend + volume
    if (bearEngulf && rsiVal > RSI_SELL_MIN && last.close > deltaRef.close
        && trendBear && macdBear) {
        return {type:'SELL', sizePct: 100.0,
            stopLossPct: slPct, takeProfitPct: tpPct};
    }

    return {type:'HOLD'};
}
"""

@HiltViewModel
class ScriptViewModel @Inject constructor(
    private val scriptRepository: ScriptRepository,
    private val marketRepository: MarketRepository,
    private val scriptExecutor: ScriptExecutor,
    private val consoleBus: ConsoleBus
) : ViewModel() {

    private val _state = MutableStateFlow(ScriptUiState())
    val state: StateFlow<ScriptUiState> = _state
    private var candleJob: Job? = null
    private var validateJob: Job? = null

    init {
        loadScripts()
        loadActiveStrategy()
        loadCandles()
        loadCustomTemplates()
    }

    private fun loadScripts() {
        viewModelScope.launch {
            scriptRepository.getAll().collect { scripts ->
                _state.update { it.copy(scripts = scripts) }
            }
        }
    }

    private fun loadActiveStrategy() {
        viewModelScope.launch {
            scriptRepository.getActiveStrategy().collect { active ->
                _state.update { it.copy(activeStrategy = active) }
            }
        }
    }

    private fun loadCustomTemplates() {
        viewModelScope.launch {
            scriptRepository.getCustomTemplates().collect { customs ->
                _state.update { it.copy(customTemplates = customs) }
            }
        }
    }

    private fun loadCandles() {
        candleJob?.cancel()
        candleJob = viewModelScope.launch {
            _state.update { it.copy(isLoadingChart = true) }
            try {
                val interval = _state.value.backtestInterval
                marketRepository.getCandles(_state.value.selectedPair, interval, 200)
                    .collect { candles ->
                        _state.update { it.copy(candles = candles, isLoadingChart = false) }
                    }
            } catch (e: Exception) {
                _state.update { it.copy(isLoadingChart = false) }
            }
        }
    }

    private fun loadBacktestCandles() {
        candleJob?.cancel()
        candleJob = viewModelScope.launch {
            _state.update { it.copy(isLoadingChart = true) }
            try {
                val interval = _state.value.backtestInterval
                marketRepository.getCandlesFromDb(_state.value.selectedPair, interval)
                    .collect { candles ->
                        _state.update { it.copy(candles = candles, isLoadingChart = false) }
                    }
            } catch (e: Exception) {
                _state.update { it.copy(isLoadingChart = false) }
            }
        }
    }

    /** Run a final evaluate on current candles to capture plot() overlays and dashboard. */
    private fun refreshPlotData() {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val s = _state.value
                val candles = s.candles
                if (candles.size < 10) return@launch
                scriptExecutor.evaluate(s.code, candles, s.editableParams)
                val plotJson = scriptExecutor.getLastPlotData()
                val rawDashJson = scriptExecutor.getLastDashboard()
                val dashJson = rawDashJson?.let { buildDashboardOverlayJson(it) }
                _state.update { it.copy(strategyPlotJson = plotJson, dashboardOverlayJson = dashJson) }
            } catch (_: Exception) { /* ignore evaluation errors */ }
        }
    }

    /** Transform raw _state.dashboard JSON into the format chart.html setDashboard() expects. */
    private fun buildDashboardOverlayJson(rawJson: String): String? {
        return try {
            val obj = org.json.JSONObject(rawJson)
            // Detect TFG_ALGO_PINE-format dashboards (UT Bot + LinReg + STC), distinct
            // from the legacy 10-factor TFG_ALGO scoring layout.
            val isPineAlgo = obj.has("tier") || obj.has("stc") || obj.has("sigLine") || obj.has("xATS")
            if (isPineAlgo) return buildPineAlgoOverlay(obj)
            val rows = org.json.JSONArray()
            fun addRow(label: String, passKey: String, status: String) {
                val pass = obj.optInt(passKey, 0) == 1
                rows.put(org.json.JSONObject().put("label", label).put("pass", pass).put("status", status))
            }
            addRow("Trend", "c_trend", if (obj.optInt("c_trend") == 1) "Bullish" else "Bearish")
            addRow("VWAP", "c_vwap", if (obj.optInt("c_vwap") == 1) "Above" else "Below")
            addRow("Volume", "c_volume", if (obj.optInt("c_volume") == 1) "Strong" else "Weak")
            addRow("RSI", "c_rsi", String.format("%.1f", obj.optDouble("rsiVal", 0.0)))
            addRow("MACD", "c_macd", if (obj.optInt("c_macd") == 1) "Bullish" else "Bearish")
            addRow("Candle", "c_candle", when {
                obj.optInt("bullEngulf") == 1 -> "Engulfing"
                obj.optInt("bullPinBar") == 1 -> "Pin Bar"
                obj.optInt("strongBull") == 1 -> "Strong Bull"
                else -> "Neutral"
            })
            addRow("Support", "c_support", obj.optDouble("supportLevel", 0.0).let {
                if (it > 0) String.format("S:%.2f", it) else "—"
            })
            addRow("DI+/DI-", "c_di", String.format("%.1f/%.1f", obj.optDouble("diPlus", 0.0), obj.optDouble("diMinus", 0.0)))
            addRow("ADX", "c_adx", String.format("%.1f", obj.optDouble("adxVal", 0.0)))
            addRow("Breakout", "c_breakout", if (obj.optInt("c_breakout") == 1) "Yes" else "No")
            org.json.JSONObject().apply {
                put("title", "TFG ALGO")
                put("score", obj.optInt("score", 0))
                put("maxScore", 10)
                put("rows", rows)
                put("inPosition", obj.optInt("inPosition", 0) == 1)
                put("pnlPct", obj.optDouble("pnlPct", 0.0))
                put("htfBull", obj.optInt("htfBull", 1) == 1)
            }.toString()
        } catch (_: Exception) { null }
    }

    /** Dashboard overlay for TFG_ALGO_PINE template (UT Bot + LinReg + STC). */
    private fun buildPineAlgoOverlay(obj: org.json.JSONObject): String {
        val rows = org.json.JSONArray()
        fun row(label: String, pass: Boolean, status: String) {
            rows.put(org.json.JSONObject().put("label", label).put("pass", pass).put("status", status))
        }
        val action = obj.optString("action", "HOLD")
        val tier = obj.optInt("tier", 0)
        val strong = obj.optInt("strong", 0) == 1
        val stc = obj.optDouble("stc", 0.0)
        val sigLine = obj.optDouble("sigLine", 0.0)
        val bclose = obj.optDouble("bclose", 0.0)
        val xATS = obj.optDouble("xATS", 0.0)
        val market = obj.optString("market", "SPOT")
        val leverage = obj.optInt("leverage", 1)
        row("Signal", action != "HOLD", action)
        row("Tier", tier in 1..3, if (tier > 0) "T$tier${if (strong) "★" else ""}" else "—")
        row("STC", stc < 25 || stc > 75, String.format("%.1f", stc))
        row("LinReg vs Sig", bclose >= sigLine, if (bclose >= sigLine) "Above" else "Below")
        row("UT Trail", action == "BUY" && bclose > xATS || action == "SELL" && bclose < xATS,
            String.format("%.4f", xATS))
        row("Market", true, "$market${if (market == "FUTURES_USDM") " ${leverage}x" else ""}")
        return org.json.JSONObject().apply {
            put("title", "ISSA ALGO")
            put("score", tier)
            put("maxScore", 4)
            put("rows", rows)
            put("inPosition", false)
            put("pnlPct", 0.0)
            put("htfBull", true)
        }.toString()
    }

    fun updateCode(code: String) {
        val extracted = extractParamsFromCode(code)
        val meta = extractParamMeta(code)
        _state.update {
            val merged = if (extracted.isNotEmpty()) extracted else it.editableParams
            // Re-validate existing param values against new meta
            val errors = merged.entries.mapNotNull { (k, v) ->
                val m = meta[k] ?: return@mapNotNull null
                val err = validateParam(k, v, m) ?: return@mapNotNull null
                k to err
            }.toMap()
            it.copy(code = code, editableParams = merged, paramMeta = meta, paramErrors = errors)
        }
        scheduleValidation(code)
    }

    private fun scheduleValidation(code: String) {
        validateJob?.cancel()
        validateJob = viewModelScope.launch {
            delay(500L)
            val error = scriptRepository.validateSyntax(code)
            _state.update { it.copy(syntaxError = error) }
        }
    }

    fun validateCode() {
        viewModelScope.launch {
            val error = scriptRepository.validateSyntax(_state.value.code)
            _state.update { it.copy(syntaxError = error) }
        }
    }

    /**
     * Run the script ONCE on the current candles to surface runtime errors that
     * the backtest loop would otherwise swallow (e.g. ReferenceError thrown
     * inside strategy()). Result is dumped into the console so the user can
     * read the exact exception message + any console.log output.
     */
    fun checkRuntime() {
        viewModelScope.launch(Dispatchers.Default) {
            val s = _state.value
            // Need *some* candles. If the chart hasn't loaded any yet, fall back
            // to a synthetic warmup so the user can still get a quick syntax
            // + runtime sanity check without waiting for a Binance round-trip.
            val candles = if (s.candles.size >= 50) s.candles else buildSyntheticCandles(s.selectedPair, s.backtestInterval, 600)
            // Show a "running…" line immediately in the console panel
            _state.update { it.copy(console = listOf("[CHECK] Running on ${candles.size} bars…")) }

            val res = scriptExecutor.runtimeCheck(s.code, candles, s.editableParams)

            // Build console lines that will appear in the panel
            val lines = mutableListOf<String>()
            if (res.ok) {
                lines += "[CHECK] ✓ OK  signal=${res.signalType}  time=${res.elapsedMs} ms"
            } else {
                lines += "[ERROR] ✗ FAILED after ${res.elapsedMs} ms"
                lines += "[ERROR] ${res.errorMessage}"
            }
            // Append any console.log/warn/error lines the script emitted
            if (res.logs.isNotEmpty()) {
                lines += "[CHECK] --- console output (${res.logs.size} lines) ---"
                lines += res.logs.take(100)
                if (res.logs.size > 100) lines += "… (${res.logs.size - 100} more lines suppressed)"
            } else {
                lines += "[CHECK] (no console.log output)"
            }
            _state.update { it.copy(console = lines) }
        }
    }

    /** Generate fake but plausible OHLCV bars for runtime checks when no real candles are loaded yet. */
    private fun buildSyntheticCandles(symbol: String, interval: String, count: Int): List<Candle> {
        val intervalMs = when (interval) {
            "1m" -> 60_000L; "5m" -> 300_000L; "15m" -> 900_000L; "30m" -> 1_800_000L
            "1h" -> 3_600_000L; "4h" -> 14_400_000L; "1d" -> 86_400_000L; else -> 3_600_000L
        }
        val now = System.currentTimeMillis()
        var price = 50_000.0
        val rnd = java.util.Random(42L)
        return List(count) { idx ->
            val open = price
            val drift = (rnd.nextGaussian() * 0.005) * price
            val close = (open + drift).coerceAtLeast(1.0)
            val high = maxOf(open, close) + Math.abs(rnd.nextGaussian()) * 0.002 * price
            val low = minOf(open, close) - Math.abs(rnd.nextGaussian()) * 0.002 * price
            price = close
            val openTime = now - (count - idx).toLong() * intervalMs
            Candle(
                symbol = symbol, interval = interval, openTime = openTime,
                open = open, high = high, low = low, close = close,
                volume = 100.0 + Math.abs(rnd.nextGaussian()) * 50,
                closeTime = openTime + intervalMs - 1,
                quoteVolume = 0.0, numberOfTrades = 0
            )
        }
    }

    fun clearConsole() {
        _state.update { it.copy(console = emptyList()) }
    }

    fun selectPair(pair: String) {
        _state.update { it.copy(selectedPair = pair, signalMarkers = emptyList(), backtestResult = null) }
        loadCandles()
    }

    fun loadTemplate(template: StrategyTemplate) {
        _state.update {
            it.copy(
                code = template.code,
                selectedScript = null,
                currentTemplateName = template.name,
                editableParams = template.defaultParams,
                showSettings = template.defaultParams.isNotEmpty()
            )
        }
    }

    fun updateParam(key: String, value: String) {
        // Only update the editable map. We DO NOT touch the code on every
        // keystroke — partial / empty values used to silently corrupt the
        // source via regex (eating across newlines), which made the field
        // unable to recover once cleared. Code is synced on Apply / before run.
        _state.update {
            val meta = it.paramMeta[key]
            val error = if (meta != null) validateParam(key, value, meta) else null
            val newErrors = if (error != null) it.paramErrors + (key to error)
                           else it.paramErrors - key
            it.copy(
                editableParams = it.editableParams + (key to value),
                paramErrors = newErrors
            )
        }
    }

    // B3: Configurable backtest costs
    fun updateBacktestMakerFee(pct: Double) { _state.update { it.copy(backtestMakerFee = pct) } }
    fun updateBacktestTakerFee(pct: Double) { _state.update { it.copy(backtestTakerFee = pct) } }
    fun updateBacktestSlippage(pct: Double) { _state.update { it.copy(backtestSlippagePct = pct) } }
    fun updateBacktestStartingCapital(amount: Double) { _state.update { it.copy(backtestStartingCapital = amount.coerceAtLeast(1.0)) } }
    fun updateBacktestMarketType(mt: com.tfg.domain.model.MarketType) { _state.update { it.copy(backtestMarketType = mt) } }
    fun updateBacktestLeverage(lev: Int) { _state.update { it.copy(backtestLeverage = lev.coerceIn(1, 125)) } }

    /** Returns the trading plan for the current script (defaults if none saved). */
    fun currentPlan(): com.tfg.domain.model.TradingPlan =
        com.tfg.domain.model.TradingPlan.fromJson(_state.value.editableParams[com.tfg.domain.model.TradingPlan.PARAM_KEY])

    /** Persists the user's trading plan into editable params + DB. */
    fun savePlan(plan: com.tfg.domain.model.TradingPlan) {
        _state.update {
            val newParams = it.editableParams.toMutableMap().also { m ->
                m[com.tfg.domain.model.TradingPlan.PARAM_KEY] = plan.toJson()
            }
            it.copy(editableParams = newParams)
        }
        // Persist to DB if a script is loaded
        viewModelScope.launch {
            val s = _state.value.selectedScript
            if (s != null) {
                scriptRepository.save(
                    s.copy(
                        params = _state.value.editableParams,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }
    fun updateBacktestStartDate(ms: Long?) { _state.update { it.copy(backtestStartDateMs = ms) } }
    fun updateBacktestEndDate(ms: Long?) { _state.update { it.copy(backtestEndDateMs = ms) } }

    // C5: Export backtest report
    fun exportBacktestCsv() {
        val result = _state.value.backtestResult ?: return
        _state.update { it.copy(backtestCsvContent = result.toCsv()) }
    }
    fun clearBacktestCsv() { _state.update { it.copy(backtestCsvContent = null) } }

    // C4: Monte Carlo simulation
    fun runMonteCarloSimulation() {
        val result = _state.value.backtestResult ?: return
        viewModelScope.launch(Dispatchers.Default) {
            val mc = result.runMonteCarlo()
            _state.update { it.copy(monteCarloResult = mc) }
        }
    }

    fun applyParamsToCode() {
        // Sync the editable params into the source code (skipping blanks),
        // then persist. Called on the Apply button and implicitly before any
        // backtest so the JS engine sees the latest user inputs.
        _state.update { s ->
            var code = s.code
            s.editableParams.forEach { (k, v) ->
                if (v.isNotBlank()) code = injectParamIntoCode(code, k, v)
            }
            s.copy(code = code)
        }
        val script = _state.value.selectedScript ?: return
        viewModelScope.launch {
            val updated = script.copy(
                code = _state.value.code,
                params = _state.value.editableParams,
                updatedAt = System.currentTimeMillis()
            )
            scriptRepository.save(updated)
            _state.update { it.copy(selectedScript = updated) }
        }
    }

    fun toggleSettings() {
        _state.update { it.copy(showSettings = !it.showSettings) }
    }

    fun saveScript(name: String) {
        if (_state.value.isSaving) return
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                // Use existing ID if editing, otherwise generate a stable new ID
                val existingId = _state.value.selectedScript?.id
                    ?.takeIf { it.isNotBlank() }
                    ?: "script_${System.currentTimeMillis()}"

                // Detect which template: use existing script's templateId, or match by name, or match by code
                val templateId = _state.value.selectedScript?.strategyTemplateId
                    ?: _state.value.templates.firstOrNull { it.name == _state.value.currentTemplateName }?.id?.name
                    ?: _state.value.templates.firstOrNull { it.code.trim() == _state.value.code.trim() }?.id?.name

                val script = Script(
                    id = existingId,
                    name = name,
                    code = _state.value.code,
                    strategyTemplateId = templateId,
                    params = _state.value.editableParams,
                    relatedSymbols = _state.value.selectedScript?.relatedSymbols ?: emptyList(),
                    createdAt = _state.value.selectedScript?.createdAt ?: System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                scriptRepository.save(script)
                _state.update { it.copy(selectedScript = script) }
                // Auto-backtest so chart updates immediately
                runBacktest(_state.value.selectedPair, _state.value.backtestInterval, 30)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = "Save failed: ${e.message ?: "Unknown error"}") }
            } finally {
                _state.update { it.copy(isSaving = false) }
            }
        }
    }

    fun addRelatedSymbol(symbol: String) {
        val trimmed = symbol.trim().uppercase()
        if (trimmed.isBlank()) return
        val current = _state.value.selectedScript ?: return
        if (current.relatedSymbols.contains(trimmed)) return
        val updated = current.copy(relatedSymbols = current.relatedSymbols + trimmed, updatedAt = System.currentTimeMillis())
        _state.update { it.copy(selectedScript = updated) }
        viewModelScope.launch { scriptRepository.save(updated) }
    }

    fun removeRelatedSymbol(symbol: String) {
        val current = _state.value.selectedScript ?: return
        val updated = current.copy(relatedSymbols = current.relatedSymbols - symbol, updatedAt = System.currentTimeMillis())
        _state.update { it.copy(selectedScript = updated) }
        viewModelScope.launch { scriptRepository.save(updated) }
    }

    fun selectScript(script: Script) {
        // Load params from saved script, or fall back to template defaults
        val params = if (script.params.isNotEmpty()) {
            script.params
        } else {
            _state.value.templates.firstOrNull { it.id.name == script.strategyTemplateId }?.defaultParams ?: emptyMap()
        }
        val template = _state.value.templates.firstOrNull { it.id.name == script.strategyTemplateId }
        val templateName = template?.name
        // Recovery path: if a saved script has blank or syntactically broken
        // code (possible after older buggy param-injection corrupted it) and
        // we know which template it came from, restore the template's fresh
        // code. The user keeps their custom params; only the source is reset.
        val storedCode = script.code
        val effectiveCode = if (template != null && (storedCode.isBlank() ||
                scriptRepository.validateSyntax(storedCode) != null)) {
            template.code
        } else storedCode
        _state.update {
            it.copy(
                selectedScript = script,
                code = effectiveCode,
                currentTemplateName = templateName,
                editableParams = params,
                showSettings = params.isNotEmpty()
            )
        }
    }

    fun activateStrategy(scriptId: String) {
        val pair = _state.value.selectedPair
        viewModelScope.launch {
            _state.update { it.copy(isActivating = true, error = null) }
            try {
                scriptRepository.activateStrategy(scriptId, pair)
                _state.update { it.copy(isActivating = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isActivating = false, error = e.message) }
            }
        }
    }

    fun deactivateStrategy() {
        val active = _state.value.activeStrategy ?: return
        viewModelScope.launch {
            _state.update { it.copy(isActivating = true, error = null) }
            try {
                scriptRepository.deactivateStrategy(active.id)
                _state.update { it.copy(isActivating = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isActivating = false, error = e.message) }
            }
        }
    }

    private var backtestJob: Job? = null

    fun cancelBacktest() {
        backtestJob?.cancel()
        backtestJob = null
        _state.update { it.copy(isRunning = false, backtestProgress = 0f, error = null) }
    }

    fun runBacktest(symbol: String, interval: String, days: Int) {
        val resolvedTemplateId = _state.value.selectedScript?.strategyTemplateId
            ?: _state.value.templates.firstOrNull { it.name == _state.value.currentTemplateName }?.id?.name
            ?: _state.value.templates.firstOrNull { it.code.trim() == _state.value.code.trim() }?.id?.name
            ?: ""

        val previousJob = backtestJob
        backtestJob = viewModelScope.launch {
            // Wait for the previous run to fully cancel — without this, the JS
            // worker pool can still be in use when we try to reset it, causing
            // the second backtest to silently produce nothing.
            try { previousJob?.cancelAndJoin() } catch (_: CancellationException) {}
            consoleBus.info(
                ConsoleSource.BACKTEST,
                title = "Backtest started",
                message = "$symbol $interval • ${days}d window",
                symbol = symbol
            )
            _state.update { it.copy(
                isRunning = true, backtestProgress = 0f, error = null,
                console = listOf("[backtest] starting $symbol $interval ${days}d…"),
                signalMarkers = emptyList(), backtestResult = null,
                selectedPair = symbol, backtestInterval = interval
            ) }
            try {
                // Make sure any pending edits in the Strategy Settings panel
                // are baked into the source code BEFORE we save + run, so the
                // JS engine sees the user's latest inputs even if they didn't
                // press the Apply button.
                _state.update { s ->
                    var code = s.code
                    s.editableParams.forEach { (k, v) ->
                        if (v.isNotBlank()) code = injectParamIntoCode(code, k, v)
                    }
                    s.copy(code = code)
                }
                // Auto-save current code + params before running
                val scriptId = ensureScriptSaved(resolvedTemplateId)

                val result = scriptRepository.backtest(
                    scriptId, resolvedTemplateId, symbol, interval, days,
                    onProgress = { progress -> _state.update { it.copy(backtestProgress = progress) } },
                    makerFeePct = _state.value.backtestMakerFee / 100.0,
                    takerFeePct = _state.value.backtestTakerFee / 100.0,
                    slippagePct = _state.value.backtestSlippagePct,
                    startDateMs = _state.value.backtestStartDateMs,
                    endDateMs = _state.value.backtestEndDateMs,
                    marketTypeOverride = _state.value.backtestMarketType,
                    leverageOverride = _state.value.backtestLeverage,
                    startingCapital = _state.value.backtestStartingCapital
                )
                // Load actual signal markers from DB (preserves TP_HIT, SL_HIT, PAT_BUY types).
                // Backtest impl wipes prior markers for this symbol before inserting,
                // so everything returned here belongs to this run.
                val markers = scriptRepository.getSignalMarkers(symbol, interval).first()
                val logs = scriptRepository.getLastLogs()
                val summary = listOf(
                    "[backtest] done: ${result.totalTrades} trades, ${markers.size} signals, " +
                    "PnL=${"%.2f".format(result.totalPnl)} winRate=${"%.1f".format(result.winRate)}%"
                )
                _state.update { it.copy(
                    isRunning = false, backtestResult = result, signalMarkers = markers,
                    console = summary + logs
                ) }
                // Emit a structured "done" event so the global Console can pick it up.
                if (result.totalTrades == 0 && markers.isEmpty()) {
                    consoleBus.warn(
                        ConsoleSource.BACKTEST,
                        title = "Backtest produced no trades",
                        message = "$symbol $interval — strategy returned HOLD on every bar. " +
                            "Likely cause: parameters too strict, JS error in strategy() (check Logcat), " +
                            "or insufficient candles for the indicator window.",
                        symbol = symbol
                    )
                } else {
                    consoleBus.success(
                        ConsoleSource.BACKTEST,
                        title = "Backtest complete",
                        message = "${result.totalTrades} trades • ${markers.size} signals • " +
                            "PnL ${"%.2f".format(result.totalPnl)} • " +
                            "win ${"%.1f".format(result.winRate)}%",
                        symbol = symbol
                    )
                }
                logs.forEach { line ->
                    if (line.isNotBlank()) consoleBus.debug(ConsoleSource.STRATEGY, line, symbol = symbol)
                }
                // Reload candles from DB (backtest already stored them) — don't re-fetch from API
                loadBacktestCandles()
                // Evaluate strategy on final candles to capture plot overlays & dashboard
                refreshPlotData()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Surface the failure into the console panel so the user can see it.
                val msg = "Backtest failed: ${e.message ?: e::class.simpleName ?: "Unknown error"}"
                val stack = e.stackTraceToString().lineSequence().take(8).toList()
                _state.update { it.copy(
                    isRunning = false,
                    error = msg,
                    console = listOf("[ERROR] $msg") + stack
                ) }
                consoleBus.error(
                    ConsoleSource.BACKTEST,
                    title = "Backtest failed",
                    message = msg + "\n" + stack.joinToString("\n"),
                    symbol = symbol
                )
            }
        }
    }

    // C1: Multi-symbol backtest
    fun updateBacktestSymbols(symbols: List<String>) { _state.update { it.copy(backtestSymbols = symbols) } }

    fun runMultiBacktest(symbols: List<String>, interval: String, days: Int) {
        backtestJob?.cancel()
        backtestJob = viewModelScope.launch {
            _state.update { it.copy(isRunning = true, backtestProgress = 0f, error = null, console = emptyList(), backtestInterval = interval, monteCarloResult = null) }
            try {
                val scriptId = ensureScriptSaved("")
                val result = scriptRepository.backtestMultiSymbol(
                    scriptId, symbols, interval, days,
                    onProgress = { progress -> _state.update { it.copy(backtestProgress = progress) } },
                    makerFeePct = _state.value.backtestMakerFee / 100.0,
                    takerFeePct = _state.value.backtestTakerFee / 100.0,
                    slippagePct = _state.value.backtestSlippagePct,
                    startDateMs = _state.value.backtestStartDateMs,
                    endDateMs = _state.value.backtestEndDateMs
                )
                val logs = scriptRepository.getLastLogs()
                _state.update { it.copy(isRunning = false, backtestResult = result, console = logs) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isRunning = false, error = "Multi-symbol backtest failed: ${e.message ?: "Unknown error"}") }
            }
        }
    }

    fun deleteScript(id: String) {
        viewModelScope.launch {
            try {
                // Deactivate if deleting the active strategy
                if (_state.value.activeStrategy?.id == id) {
                    scriptRepository.deactivateStrategy(id)
                }
                scriptRepository.delete(id)
                if (_state.value.selectedScript?.id == id) {
                    _state.update { it.copy(selectedScript = null, code = DEFAULT_SCRIPT) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = "Delete failed: ${e.message ?: "Unknown error"}") }
            }
        }
    }

    fun saveAsCustomTemplate(name: String, description: String, customParams: Map<String, String>) {
        val currentCode = _state.value.code
        // Determine the base built-in template
        val baseTemplateId = _state.value.selectedScript?.strategyTemplateId
            ?: _state.value.templates.firstOrNull { it.name == _state.value.currentTemplateName }?.id?.name
            ?: _state.value.templates.firstOrNull { it.code.trim() == currentCode.trim() }?.id?.name

        if (baseTemplateId == null) {
            _state.update { it.copy(error = "Load a built-in template first, then save as custom template.") }
            return
        }

        // Inject any new params into code as val declarations
        var code = currentCode
        customParams.forEach { (key, value) ->
            if (!code.contains("val $key")) {
                // Insert after last UPPER_SNAKE_CASE val (settings area), not local vars
                val lines = code.lines().toMutableList()
                val lastParamIdx = lines.indexOfLast {
                    it.trimStart().matches(Regex("""val\s+[A-Z][A-Z0-9_]*\s*=.*"""))
                }
                if (lastParamIdx >= 0) {
                    lines.add(lastParamIdx + 1, "val $key = $value")
                } else {
                    // Insert after last comment header line at top
                    val insertIdx = lines.indexOfLast { it.trimStart().startsWith("//") && lines.indexOf(it) < 5 } + 1
                    lines.add(insertIdx.coerceAtLeast(0), "val $key = $value")
                }
                code = lines.joinToString("\n")
            } else {
                code = injectParamIntoCode(code, key, value)
            }
        }

        viewModelScope.launch {
            val template = CustomTemplate(
                id = "ct_${System.currentTimeMillis()}",
                name = name,
                description = description,
                baseTemplateId = baseTemplateId,
                code = code,
                defaultParams = customParams
            )
            scriptRepository.saveCustomTemplate(template)
        }
    }

    fun loadCustomTemplate(template: CustomTemplate) {
        _state.update {
            it.copy(
                code = template.code,
                selectedScript = null,
                currentTemplateName = template.name,
                editableParams = template.defaultParams,
                showSettings = template.defaultParams.isNotEmpty()
            )
        }
    }

    fun deleteCustomTemplate(id: String) {
        viewModelScope.launch {
            scriptRepository.deleteCustomTemplate(id)
        }
    }

    fun updateCustomTemplate(
        existingTemplate: CustomTemplate,
        name: String,
        description: String,
        updatedParams: Map<String, String>
    ) {
        // Start from existing code, sync all param changes
        var code = existingTemplate.code

        // Remove params that were deleted
        val removedKeys = existingTemplate.defaultParams.keys - updatedParams.keys
        removedKeys.forEach { key ->
            code = code.lines().filter { line ->
                !line.trimStart().startsWith("val $key ")
                    && !line.trimStart().startsWith("val $key=")
            }.joinToString("\n")
        }

        // Update existing params and add new ones
        updatedParams.forEach { (key, value) ->
            if (code.contains(Regex("""val\s+${Regex.escape(key)}\s*="""))) {
                code = injectParamIntoCode(code, key, value)
            } else {
                // Insert after last UPPER_SNAKE_CASE val (settings area)
                val lines = code.lines().toMutableList()
                val lastParamIdx = lines.indexOfLast {
                    it.trimStart().matches(Regex("""val\s+[A-Z][A-Z0-9_]*\s*=.*"""))
                }
                if (lastParamIdx >= 0) {
                    lines.add(lastParamIdx + 1, "val $key = $value")
                } else {
                    val insertIdx = lines.indexOfLast { it.trimStart().startsWith("//") && lines.indexOf(it) < 5 } + 1
                    lines.add(insertIdx.coerceAtLeast(0), "val $key = $value")
                }
                code = lines.joinToString("\n")
            }
        }

        val updated = existingTemplate.copy(
            name = name,
            description = description,
            code = code,
            defaultParams = updatedParams
        )
        viewModelScope.launch {
            scriptRepository.saveCustomTemplate(updated)
        }
    }

    /**
     * Ensure the current code + params are saved to DB before running.
     * Creates a new script if none is selected.
     */
    private suspend fun ensureScriptSaved(templateId: String): String {
        val existing = _state.value.selectedScript
        val script = if (existing != null) {
            existing.copy(
                code = _state.value.code,
                params = _state.value.editableParams,
                strategyTemplateId = templateId,
                updatedAt = System.currentTimeMillis()
            )
        } else {
            Script(
                id = "run_${System.currentTimeMillis()}",
                name = _state.value.currentTemplateName ?: "Script",
                code = _state.value.code,
                strategyTemplateId = templateId,
                params = _state.value.editableParams
            )
        }
        scriptRepository.save(script)
        _state.update { it.copy(selectedScript = script) }
        return script.id
    }

    // ═══════════════════════════════════════════════════════════════════
    //  REPLAY MODE — Uses backtest signals so replay exactly mirrors backtest
    // ═══════════════════════════════════════════════════════════════════

    private var replayJob: Job? = null

    fun enterReplayMode() {
        val s = _state.value
        val candles = s.candles
        // Require backtest to have been run first
        if (candles.size < 10 || s.backtestResult == null || s.signalMarkers.isEmpty()) return
        replayJob?.cancel()
        val startBar = 50.coerceAtMost(candles.size - 1)
        // Filter backtest signals up to the start bar
        val startTime = candles[startBar].openTime
        val visibleSignals = s.signalMarkers.filter { it.openTime <= startTime }
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
        if (!s.replayMode || s.replayBarIndex >= s.candles.size - 1) return
        val newIdx = s.replayBarIndex + 1
        val barTime = s.candles[newIdx].openTime
        // Show all backtest signals up to this bar — exact same signals as backtest
        val visibleSignals = s.signalMarkers.filter { it.openTime <= barTime }
        _state.update { it.copy(replayBarIndex = newIdx, replaySignals = visibleSignals) }
    }

    fun replayStepBack() {
        val s = _state.value
        if (!s.replayMode || s.replayBarIndex <= 1) return
        val newIdx = s.replayBarIndex - 1
        val barTime = s.candles[newIdx].openTime
        val visibleSignals = s.signalMarkers.filter { it.openTime <= barTime }
        _state.update { it.copy(replayBarIndex = newIdx, replaySignals = visibleSignals) }
    }

    fun replayPlay() {
        _state.update { it.copy(replayPlaying = true) }
        replayJob?.cancel()
        replayJob = viewModelScope.launch {
            while (_state.value.replayPlaying && _state.value.replayBarIndex < _state.value.candles.size - 1) {
                delay(_state.value.replaySpeedMs)
                replayStepForward()
            }
            _state.update { it.copy(replayPlaying = false) }
        }
    }

    fun replayPause() {
        replayJob?.cancel()
        _state.update { it.copy(replayPlaying = false) }
    }

    fun setReplaySpeed(ms: Long) {
        _state.update { it.copy(replaySpeedMs = ms) }
    }

    fun seekReplay(barIndex: Int) {
        val s = _state.value
        val idx = barIndex.coerceIn(1, s.candles.size - 1)
        val barTime = s.candles[idx].openTime
        // Instant seek — just filter pre-computed backtest signals, no re-evaluation needed
        val visibleSignals = s.signalMarkers.filter { it.openTime <= barTime }
        _state.update { it.copy(replayBarIndex = idx, replaySignals = visibleSignals) }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  BUY & HOLD COMPARISON
    // ═══════════════════════════════════════════════════════════════════

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

    // ═══════════════════════════════════════════════════════════════════
    //  PARAMETER OPTIMIZER
    // ═══════════════════════════════════════════════════════════════════

    private var optimizerJob: Job? = null

    fun toggleOptimizer() {
        val s = _state.value
        if (!s.showOptimizer) {
            // Auto-detect params from current code
            val params = s.editableParams.map { (k, v) ->
                val num = v.toDoubleOrNull() ?: 0.0
                OptimizationParam(
                    name = k,
                    min = (num * 0.5).coerceAtLeast(1.0),
                    max = num * 2.0,
                    step = if (num >= 10) (num * 0.1).coerceAtLeast(1.0) else 1.0
                )
            }
            _state.update { it.copy(showOptimizer = true, optimizationParams = params, optimizationResults = emptyList()) }
        } else {
            _state.update { it.copy(showOptimizer = false) }
        }
    }

    fun updateOptimizationParam(name: String, min: Double, max: Double, step: Double) {
        _state.update { s ->
            val updated = s.optimizationParams.map { p ->
                if (p.name == name) p.copy(min = min, max = max, step = step) else p
            }
            s.copy(optimizationParams = updated)
        }
    }

    fun sortOptimizationBy(field: String) {
        _state.update { s ->
            val sorted = when (field) {
                "pnlPercent" -> s.optimizationResults.sortedByDescending { it.pnlPercent }
                "winRate" -> s.optimizationResults.sortedByDescending { it.winRate }
                "sharpe" -> s.optimizationResults.sortedByDescending { it.sharpe }
                "maxDrawdown" -> s.optimizationResults.sortedBy { it.maxDrawdown }
                "trades" -> s.optimizationResults.sortedByDescending { it.trades }
                "profitFactor" -> s.optimizationResults.sortedByDescending { it.profitFactor }
                else -> s.optimizationResults
            }
            s.copy(optimizationResults = sorted, optimizationSortBy = field)
        }
    }

    fun cancelOptimization() {
        optimizerJob?.cancel()
        optimizerJob = null
        _state.update { it.copy(isOptimizing = false, optimizationProgress = 0f) }
    }

    fun applyOptimizationResult(result: OptimizationResult) {
        var code = _state.value.code
        result.params.forEach { (k, v) ->
            code = injectParamIntoCode(code, k, v)
        }
        _state.update { it.copy(code = code, editableParams = result.params, showOptimizer = false) }
    }

    fun runOptimization(symbol: String, interval: String, days: Int) {
        val params = _state.value.optimizationParams.filter { it.step > 0 && it.max > it.min }
        if (params.isEmpty()) {
            _state.update { it.copy(error = "No valid parameters to optimize") }
            return
        }

        optimizerJob?.cancel()
        optimizerJob = viewModelScope.launch {
            _state.update { it.copy(isOptimizing = true, optimizationProgress = 0f, optimizationResults = emptyList()) }
            try {
                // Generate all combinations
                val ranges = params.map { p ->
                    val values = mutableListOf<Double>()
                    var v = p.min
                    while (v <= p.max + p.step * 0.01) {
                        values.add(v)
                        v += p.step
                    }
                    p.name to values
                }

                // Calculate total combinations
                val totalCombinations = ranges.fold(1L) { acc, (_, vals) -> acc * vals.size }
                val maxCombinations = 500L // safety limit
                if (totalCombinations > maxCombinations) {
                    _state.update { it.copy(isOptimizing = false, error = "Too many combinations ($totalCombinations). Reduce ranges or increase step size. Max: $maxCombinations") }
                    return@launch
                }

                // Generate param grids
                fun generateCombinations(remaining: List<Pair<String, List<Double>>>, current: Map<String, String>): List<Map<String, String>> {
                    if (remaining.isEmpty()) return listOf(current)
                    val (name, values) = remaining.first()
                    val rest = remaining.drop(1)
                    return values.flatMap { v ->
                        val display = if (v == v.toLong().toDouble()) v.toLong().toString() else String.format("%.2f", v)
                        generateCombinations(rest, current + (name to display))
                    }
                }

                val allCombinations = generateCombinations(ranges, emptyMap())
                val results = mutableListOf<OptimizationResult>()
                val baseCode = _state.value.code

                for ((idx, paramSet) in allCombinations.withIndex()) {
                    // Inject params into code
                    var code = baseCode
                    paramSet.forEach { (k, v) -> code = injectParamIntoCode(code, k, v) }

                    // Run mini-backtest
                    try {
                        val templateId = _state.value.selectedScript?.strategyTemplateId ?: ""
                        // Save temp script
                        val tempScript = Script(
                            id = "opt_${System.currentTimeMillis()}_$idx",
                            name = "Optimizer Run",
                            code = code,
                            strategyTemplateId = templateId,
                            params = paramSet
                        )
                        scriptRepository.save(tempScript)
                        val btResult = scriptRepository.backtest(tempScript.id, templateId, symbol, interval, days, onProgress = {})
                        // Clean up temp script
                        scriptRepository.delete(tempScript.id)

                        results.add(
                            OptimizationResult(
                                params = paramSet,
                                pnlPercent = if (btResult.startingCapital > 0) btResult.totalPnl / btResult.startingCapital * 100.0 else 0.0,
                                winRate = btResult.winRate,
                                sharpe = btResult.sharpeRatio,
                                maxDrawdown = btResult.maxDrawdown,
                                trades = btResult.totalTrades,
                                profitFactor = btResult.profitFactor
                            )
                        )
                    } catch (_: Exception) { }

                    _state.update { it.copy(optimizationProgress = (idx + 1).toFloat() / allCombinations.size) }
                }

                val sorted = results.sortedByDescending { it.pnlPercent }
                _state.update { it.copy(isOptimizing = false, optimizationResults = sorted, optimizationProgress = 1f) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isOptimizing = false, error = "Optimization failed: ${e.message}") }
            }
        }
    }
}
