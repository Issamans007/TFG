package com.tfg.feature.script

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tfg.domain.model.BacktestResult
import com.tfg.domain.model.Candle
import com.tfg.domain.model.CustomTemplate
import com.tfg.domain.model.OrderSide
import com.tfg.domain.model.Script
import com.tfg.domain.model.SignalMarker
import com.tfg.domain.model.SignalType
import com.tfg.domain.model.StrategyTemplate
import com.tfg.domain.model.StrategyTemplates
import com.tfg.domain.model.extractParamsFromCode
import com.tfg.domain.model.injectParamIntoCode
import com.tfg.domain.repository.MarketRepository
import com.tfg.domain.repository.ScriptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

val BACKTEST_INTERVALS = listOf("1m", "5m", "15m", "30m", "1h", "4h", "1d")

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
    val console: List<String> = emptyList(),
    val editableParams: Map<String, String> = emptyMap(),
    val showSettings: Boolean = false,
    val currentTemplateName: String? = null,
    val candles: List<Candle> = emptyList(),
    val signalMarkers: List<SignalMarker> = emptyList(),
    val isLoadingChart: Boolean = false
)

val POPULAR_PAIRS = listOf(
    "BTCUSDT", "ETHUSDT", "BNBUSDT", "SOLUSDT", "XRPUSDT",
    "ADAUSDT", "DOGEUSDT", "AVAXUSDT", "DOTUSDT", "MATICUSDT"
)

const val DEFAULT_SCRIPT = """// GainzAlgo V2 Strategy
// Engulfing pattern + candle stability + RSI filter + ATR TP/SL
// Detects bullish/bearish engulfing reversals with confirmation

var CANDLE_STABILITY_INDEX = 0.5;
var RSI_PERIOD = 14;
var RSI_UPPER = 70.0;
var RSI_LOWER = 30.0;
var CANDLE_DELTA_LENGTH = 4;
var TP_SL_MULTIPLIER = 1.0;
var RISK_REWARD_RATIO = 2.0;   // 1:2 risk-reward
var POSITION_SIZE_PCT = 2.0;

function strategy(candles) {
    if (candles.length < 15) return {type:'HOLD'};

    var last = candles[candles.length - 1];
    var prev = candles[candles.length - 2];
    var deltaRef = candles[candles.length - CANDLE_DELTA_LENGTH - 1];

    // True range
    var tr = Math.max(last.high - last.low,
                      Math.abs(last.high - prev.close),
                      Math.abs(last.low - prev.close));

    // Candle stability: body / TR > threshold
    var candleBody = Math.abs(last.close - last.open);
    var stable = tr > 0.0 && candleBody / tr > CANDLE_STABILITY_INDEX;

    var rsiVal = rsi(candles, RSI_PERIOD);
    var atrVal = atr(candles, RSI_PERIOD);
    var dist = atrVal * TP_SL_MULTIPLIER;
    var slPct = dist / last.close * 100;
    var tpPct = slPct * RISK_REWARD_RATIO;

    // BUY: bullish engulfing + stable + RSI < 70 + price dip
    var bullishEngulfing = prev.close < prev.open
                        && last.close > last.open
                        && last.close > prev.open;
    if (bullishEngulfing && stable && rsiVal < RSI_UPPER
        && last.close < deltaRef.close) {
        return {type:'BUY', sizePct: POSITION_SIZE_PCT,
            stopLossPct: slPct, takeProfitPct: tpPct};
    }

    // SELL: bearish engulfing + stable + RSI > 30 + price rise
    var bearishEngulfing = prev.close > prev.open
                        && last.close < last.open
                        && last.close < prev.open;
    if (bearishEngulfing && stable && rsiVal > RSI_LOWER
        && last.close > deltaRef.close) {
        return {type:'SELL', sizePct: POSITION_SIZE_PCT,
            stopLossPct: slPct, takeProfitPct: tpPct};
    }

    return {type:'HOLD'};
}
"""

@HiltViewModel
class ScriptViewModel @Inject constructor(
    private val scriptRepository: ScriptRepository,
    private val marketRepository: MarketRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ScriptUiState())
    val state: StateFlow<ScriptUiState> = _state
    private var candleJob: Job? = null

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

    fun updateCode(code: String) {
        val extracted = extractParamsFromCode(code)
        _state.update {
            val merged = if (extracted.isNotEmpty()) extracted else it.editableParams
            it.copy(code = code, editableParams = merged)
        }
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
        _state.update {
            val newParams = it.editableParams + (key to value)
            val newCode = injectParamIntoCode(it.code, key, value)
            it.copy(editableParams = newParams, code = newCode)
        }
    }

    fun applyParamsToCode() {
        // Save current code + params to DB
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

    fun selectScript(script: Script) {
        // Load params from saved script, or fall back to template defaults
        val params = if (script.params.isNotEmpty()) {
            script.params
        } else {
            _state.value.templates.firstOrNull { it.id.name == script.strategyTemplateId }?.defaultParams ?: emptyMap()
        }
        val templateName = _state.value.templates.firstOrNull { it.id.name == script.strategyTemplateId }?.name
        _state.update {
            it.copy(
                selectedScript = script,
                code = script.code,
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

        backtestJob?.cancel()
        backtestJob = viewModelScope.launch {
            _state.update { it.copy(isRunning = true, backtestProgress = 0f, error = null, console = emptyList(), selectedPair = symbol, backtestInterval = interval) }
            try {
                // Auto-save current code + params before running
                val scriptId = ensureScriptSaved(resolvedTemplateId)

                val result = scriptRepository.backtest(scriptId, resolvedTemplateId, symbol, interval, days) { progress ->
                    _state.update { it.copy(backtestProgress = progress) }
                }
                // Generate signal markers from backtest trades for chart overlay
                val markers = result.trades.flatMap { trade ->
                    listOfNotNull(
                        SignalMarker(
                            id = "bt_entry_${trade.entryTime}",
                            scriptId = scriptId,
                            symbol = symbol,
                            interval = interval,
                            openTime = trade.entryTime,
                            signalType = if (trade.side == OrderSide.BUY) SignalType.BUY else SignalType.SELL,
                            price = trade.entryPrice
                        ),
                        if (trade.exitTime > 0) SignalMarker(
                            id = "bt_exit_${trade.exitTime}",
                            scriptId = scriptId,
                            symbol = symbol,
                            interval = interval,
                            openTime = trade.exitTime,
                            signalType = SignalType.CLOSE,
                            price = trade.exitPrice
                        ) else null
                    )
                }
                _state.update { it.copy(isRunning = false, backtestResult = result, signalMarkers = markers) }
                // Reload candles from DB (backtest already stored them) — don't re-fetch from API
                loadBacktestCandles()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isRunning = false, error = "Backtest failed: ${e.message ?: "Unknown error"}") }
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
}
