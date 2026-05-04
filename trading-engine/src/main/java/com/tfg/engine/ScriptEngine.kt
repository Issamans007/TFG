package com.tfg.engine

import com.tfg.domain.model.*
import com.tfg.domain.service.ConsoleBus
import com.tfg.domain.service.ConsoleSource
import app.cash.quickjs.QuickJs
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import timber.log.Timber
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * JavaScript-based script execution engine using QuickJS.
 *
 * Users write pure JavaScript. The engine injects indicator helper
 * functions, candle data, and params, then calls the user's
 * strategy() function in a sandboxed QuickJS context.
 *
 * A single QuickJS instance is cached and reused across evaluations.
 * A watchdog thread enforces execution timeouts to guard against
 * infinite loops in user code.
 */
/** Thrown when no QuickJS worker is available within the acquisition timeout. */
class ScriptEngineBusyException(message: String) : RuntimeException(message)

@Singleton
class ScriptEngine @Inject constructor(
    private val consoleBus: ConsoleBus
) : ScriptExecutor {

    private val gson = Gson()

    // ─── Pooled QuickJS workers ─────────────────────────────────────
    // Each worker owns an independent QuickJS instance so the optimizer
    // can run backtests in parallel instead of serialising on a single lock.

    private class Worker {
        var js: QuickJs? = null
        var loadedCode: String? = null
        var loadedParams: Map<String, String>? = null
        var lastLogs: List<String> = emptyList()
    }

    private val poolSize = (Runtime.getRuntime().availableProcessors()).coerceIn(2, 8)
    private val workerPool = ArrayBlockingQueue<Worker>(poolSize).also { q ->
        repeat(poolSize) { q.put(Worker()) }
    }

    /** Maximum time to wait for a free worker before failing fast. */
    private val workerAcquireTimeoutMs = 2_000L

    /**
     * Maximum heap utilisation ratio (used / max) above which a script
     * evaluation will be refused. The QuickJS-Android JNI binding does not
     * expose `setMemoryLimit`, so this is a coarse JVM-side safeguard:
     * if the process is already near OOM, do not give a runaway script the
     * opportunity to push it over the edge.
     */
    private val maxHeapUtilisation = 0.85

    private fun memoryPressureGuard() {
        val rt = Runtime.getRuntime()
        val used = rt.totalMemory() - rt.freeMemory()
        val ratio = used.toDouble() / rt.maxMemory().toDouble()
        if (ratio > maxHeapUtilisation) {
            // Try to recover before refusing.
            System.gc()
            val used2 = rt.totalMemory() - rt.freeMemory()
            val ratio2 = used2.toDouble() / rt.maxMemory().toDouble()
            if (ratio2 > maxHeapUtilisation) {
                throw OutOfMemoryError(
                    "Heap utilisation %.0f%% exceeds %.0f%% \u2014 refusing script evaluation"
                        .format(ratio2 * 100, maxHeapUtilisation * 100)
                )
            }
        }
    }

    private fun <T> withWorker(block: (Worker) -> T): T {
        val worker = workerPool.poll(workerAcquireTimeoutMs, TimeUnit.MILLISECONDS)
            ?: throw ScriptEngineBusyException(
                "All $poolSize QuickJS workers busy after ${workerAcquireTimeoutMs}ms"
            )
        return try {
            block(worker)
        } finally {
            workerPool.put(worker)
        }
    }

    private val watchdog = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "quickjs-watchdog").also { it.isDaemon = true }
    }

    /** Logs captured from the most recent strategy() call (last-writer-wins). */
    @Volatile private var _lastLogs: List<String> = emptyList()

    /** Dashboard JSON captured from _state.dashboard after the last evaluate(). */
    @Volatile private var _lastDashboard: String? = null

    /** Plot-data JSON captured from _state.plot after the last evaluate(). */
    @Volatile private var _lastPlotData: String? = null

    /** Diagnostic JSON captured from _state.diag after the last evaluate(). */
    @Volatile private var _lastDiag: String? = null

    /** Counts evaluate()/executeMulti() calls that fell back to HOLD due to a
     *  thrown exception (including watchdog timeouts). Surfaced by the
     *  backtest engine so a 0-trade run isn't silently caused by per-bar
     *  timeouts. */
    private val _errorCount = java.util.concurrent.atomic.AtomicInteger(0)

    override fun evaluate(code: String, candles: List<Candle>, params: Map<String, String>): StrategySignal =
        execute(code, candles, params)

    override fun evaluate(code: String, candles: List<Candle>, params: Map<String, String>, account: ScriptAccount?): StrategySignal =
        execute(code, candles, params, account)

    override fun evaluate(code: String, candles: List<Candle>, params: Map<String, String>, account: ScriptAccount?, htfCandles: Map<String, List<Candle>>): StrategySignal =
        execute(code, candles, params, account, htfCandles)

    override fun evaluate(code: String, candles: List<Candle>, params: Map<String, String>, account: ScriptAccount?, htfCandles: Map<String, List<Candle>>, relatedCandles: Map<String, List<Candle>>): StrategySignal =
        execute(code, candles, params, account, htfCandles, relatedCandles)

    override fun evaluateMulti(code: String, candles: List<Candle>, params: Map<String, String>, account: ScriptAccount?): List<TargetedSignal> =
        executeMulti(code, candles, params, account)

    override fun getLastLogs(): List<String> = _lastLogs

    override fun getLastDashboard(): String? = _lastDashboard

    override fun getLastPlotData(): String? = _lastPlotData

    override fun getLastDiag(): String? = _lastDiag

    override fun getAndResetErrorCount(): Int = _errorCount.getAndSet(0)

    override fun resetState() {
        // Drain ALL workers, fully close their QuickJS contexts so the next
        // call rebuilds them from scratch (zero-state guarantee). This
        // prevents stale `_state`, cached indicator results, or half-finished
        // executions from one backtest leaking into the next.
        // Use poll-with-timeout to avoid blocking forever if a worker is
        // currently in use by a parallel evaluate (e.g. refreshPlotData).
        val drained = mutableListOf<Worker>()
        repeat(poolSize) {
            val w = workerPool.poll(50, TimeUnit.MILLISECONDS)
            if (w != null) drained.add(w)
        }
        for (w in drained) {
            try { w.js?.close() } catch (_: Exception) {}
            w.js = null
            w.loadedCode = null
            w.loadedParams = null
            w.lastLogs = emptyList()
        }
        // Clear engine-wide cached outputs from the previous run.
        _lastLogs = emptyList()
        _lastDashboard = null
        _lastPlotData = null
        _lastDiag = null
        lastEmittedJsErrorKey = null
        drained.forEach { workerPool.put(it) }
    }

    override fun validateSyntax(code: String): String? {
        return try {
            val tempJs = QuickJs.create()
            try {
                tempJs.evaluate(JS_INDICATOR_LIBRARY, "indicators")
                tempJs.evaluate(code, "strategy")
                null
            } finally {
                tempJs.close()
            }
        } catch (e: Exception) {
            e.message ?: "Unknown syntax error"
        }
    }

    /**
     * One-shot runtime check: build a fresh QuickJS context, run strategy()
     * once on the supplied candles, and report whether it threw — *with* the
     * full exception message, console logs, and elapsed ms. Unlike [execute]
     * this never silently returns HOLD on error: the caller gets the truth.
     *
     * Caps the candle slice to 500 bars to match the backtest sliding window
     * so the timing measured here reflects what the loop actually pays per bar.
     */
    override fun runtimeCheck(
        code: String,
        candles: List<Candle>,
        params: Map<String, String>
    ): RuntimeCheckResult {
        val startNs = System.nanoTime()
        val capped = if (candles.size > 500) candles.subList(candles.size - 500, candles.size) else candles
        val tempJs = QuickJs.create()
        // Hard time cap so a runaway strategy doesn't hang the UI thread.
        val timeoutFuture = watchdog.schedule({
            try { tempJs.close() } catch (_: Exception) {}
        }, TIMEOUT_MS, TimeUnit.MILLISECONDS)
        return try {
            tempJs.evaluate(JS_INDICATOR_LIBRARY, "indicators")
            // Param decls (same as production path)
            val paramDecls = buildSafeParamDecls(params)
            if (paramDecls.isNotBlank()) tempJs.evaluate(paramDecls, "paramDecls")
            tempJs.evaluate(code, "strategy")
            tempJs.evaluate("var _state = {};", "state")
            tempJs.evaluate("""
                var _log = [];
                var console = {
                    log: function() { var p=[]; for(var i=0;i<arguments.length;i++) p.push(String(arguments[i])); _log.push(p.join(' ')); },
                    warn: function() { var p=['[WARN]']; for(var i=0;i<arguments.length;i++) p.push(String(arguments[i])); _log.push(p.join(' ')); },
                    error: function() { var p=['[ERROR]']; for(var i=0;i<arguments.length;i++) p.push(String(arguments[i])); _log.push(p.join(' ')); },
                    info: function() { var p=['[INFO]']; for(var i=0;i<arguments.length;i++) p.push(String(arguments[i])); _log.push(p.join(' ')); }
                };
            """.trimIndent(), "console")
            tempJs.evaluate("var _account = null;", "account")
            tempJs.evaluate("var _htf = {};", "htf")
            tempJs.evaluate("var _candles = ${buildCandlesJson(capped)};", "data")
            tempJs.evaluate("var _params = ${buildParamsJson(params)};", "params")
            val result = tempJs.evaluate("JSON.stringify(strategy(_candles));", "call")
            val logs = captureLogs(tempJs)
            val sig = parseSignal(result?.toString() ?: "")
            val sigType = when (sig) {
                is StrategySignal.BUY -> "BUY"
                is StrategySignal.SELL -> "SELL"
                StrategySignal.CLOSE_IF_LONG -> "CLOSE_IF_LONG"
                StrategySignal.CLOSE_IF_SHORT -> "CLOSE_IF_SHORT"
                StrategySignal.HOLD -> "HOLD"
            }
            RuntimeCheckResult(
                ok = true,
                signalType = sigType,
                errorMessage = null,
                logs = logs,
                elapsedMs = (System.nanoTime() - startNs) / 1_000_000L
            )
        } catch (e: Exception) {
            // Try to grab whatever logs were captured before the throw.
            val logs = try { captureLogs(tempJs) } catch (_: Exception) { emptyList() }
            RuntimeCheckResult(
                ok = false,
                signalType = "HOLD",
                errorMessage = "${e::class.simpleName}: ${e.message ?: "unknown"}",
                logs = logs,
                elapsedMs = (System.nanoTime() - startNs) / 1_000_000L
            )
        } finally {
            timeoutFuture.cancel(false)
            try { tempJs.close() } catch (_: Exception) {}
        }
    }

    /**
     * Execute user code against the provided candles and params.
     * Returns the StrategySignal produced by the user's strategy() function.
     * Falls back to HOLD on any execution error (including timeout).
     * Thread-safe: borrows a worker from the pool, so parallel calls run concurrently.
     */
    fun execute(
        code: String,
        candles: List<Candle>,
        params: Map<String, String>,
        account: ScriptAccount? = null,
        htfCandles: Map<String, List<Candle>> = emptyMap(),
        relatedCandles: Map<String, List<Candle>> = emptyMap()
    ): StrategySignal = withWorker { worker ->
        memoryPressureGuard()
        val js = getOrCreateJs(worker, code, params)
        val timeoutFuture = watchdog.schedule({ closeJs(worker) }, TIMEOUT_MS, TimeUnit.MILLISECONDS)
        try {
            // Reset per-bar log
            js.evaluate("_log = [];", "resetLog")

            // 1. Inject candles
            val candlesJs = buildCandlesJson(candles)
            js.evaluate("var _candles = $candlesJs;", "data")

            // 2. Inject params
            val paramsJs = buildParamsJson(params)
            js.evaluate("var _params = $paramsJs;", "params")

            // 3. Inject account context
            js.evaluate(buildAccountJs(account), "account")

            // 4. Inject higher-timeframe candles (B1)
            if (htfCandles.isNotEmpty()) {
                val htfJs = buildHtfJson(htfCandles)
                js.evaluate("var _htf = $htfJs;", "htf")
            } else {
                js.evaluate("var _htf = {};", "htf")
            }

            // 5. Inject related-symbol candles (_related)
            if (relatedCandles.isNotEmpty()) {
                js.evaluate("var _related = ${buildRelatedJson(relatedCandles)};", "related")
            } else {
                js.evaluate("var _related = {};", "related")
            }

            // 5. Call strategy() and parse result
            val resultStr = js.evaluate("JSON.stringify(strategy(_candles));", "call")

            // 5. Capture console output
            worker.lastLogs = captureLogs(js)
            _lastLogs = worker.lastLogs

            // 6. Capture dashboard from _state.dashboard (if set by TFG ALGO etc.)
            _lastDashboard = try {
                val d = js.evaluate("typeof _state !== 'undefined' && _state.dashboard ? JSON.stringify(_state.dashboard) : null;", "dash")
                d?.toString()?.takeIf { it != "null" && it.isNotBlank() }
            } catch (_: Exception) { null }

            // 7. Capture strategy plot data from _state.plot (overlays, panels, hlines, etc.)
            _lastPlotData = try {
                val p = js.evaluate("typeof _state !== 'undefined' && _state.plot ? JSON.stringify(_state.plot) : null;", "plot")
                p?.toString()?.takeIf { it != "null" && it.isNotBlank() }
            } catch (_: Exception) { null }

            // 8. Capture diagnostic counters from _state.diag (e.g. tier breakdown for ISSA ALGO)
            _lastDiag = try {
                val d = js.evaluate("typeof _state !== 'undefined' && _state.diag ? JSON.stringify(_state.diag) : null;", "diag")
                d?.toString()?.takeIf { it != "null" && it.isNotBlank() }
            } catch (_: Exception) { null }

            parseSignal(resultStr?.toString() ?: "")
        } catch (e: Exception) {
            _errorCount.incrementAndGet()
            Timber.w(e, "ScriptEngine execution failed, falling back to HOLD")
            // Dedupe: a strategy that throws on every bar would otherwise
            // spam the console with hundreds of identical entries during a
            // single backtest run. Only surface a given (class, message)
            // pair once per execute() session.
            val key = (e::class.simpleName ?: "JsError") + "|" + (e.message ?: "")
            if (lastEmittedJsErrorKey != key) {
                lastEmittedJsErrorKey = key
                consoleBus.error(
                    ConsoleSource.STRATEGY,
                    title = "Strategy threw",
                    message = e.message ?: e::class.simpleName ?: "unknown JS error"
                )
            }
            closeJs(worker)
            StrategySignal.HOLD
        } finally {
            timeoutFuture.cancel(false)
        }
    }

    @Volatile private var lastEmittedJsErrorKey: String? = null

    fun executeMulti(
        code: String,
        candles: List<Candle>,
        params: Map<String, String>,
        account: ScriptAccount? = null
    ): List<TargetedSignal> = withWorker { worker ->
        memoryPressureGuard()
        val js = getOrCreateJs(worker, code, params)
        val timeoutFuture = watchdog.schedule({ closeJs(worker) }, TIMEOUT_MS, TimeUnit.MILLISECONDS)
        try {
            js.evaluate("_log = [];", "resetLog")
            js.evaluate("var _candles = ${buildCandlesJson(candles)};", "data")
            js.evaluate("var _params = ${buildParamsJson(params)};", "params")
            js.evaluate(buildAccountJs(account), "account")
            val resultStr = js.evaluate("JSON.stringify(strategy(_candles));", "call")
            worker.lastLogs = captureLogs(js)
            _lastLogs = worker.lastLogs
            parseSignalOrList(resultStr?.toString() ?: "")
        } catch (e: Exception) {
            _errorCount.incrementAndGet()
            Timber.w(e, "ScriptEngine multi-execution failed")
            closeJs(worker)
            listOf(TargetedSignal(null, StrategySignal.HOLD))
        } finally {
            timeoutFuture.cancel(false)
        }
    }

    // ─── Cached QuickJS lifecycle (per worker) ──────────────────────

    private fun getOrCreateJs(worker: Worker, code: String, params: Map<String, String>): QuickJs {
        val existing = worker.js
        if (existing != null && worker.loadedCode == code && worker.loadedParams == params) return existing

        // Code changed or first call — rebuild context
        existing?.close()
        val js = QuickJs.create()
        // Load indicator library once
        js.evaluate(JS_INDICATOR_LIBRARY, "indicators")
        // Inject param values as constants
        val paramDecls = buildSafeParamDecls(params)
        if (paramDecls.isNotBlank()) {
            js.evaluate(paramDecls, "paramDecls")
        }
        // Evaluate user code (defines strategy function)
        js.evaluate(code, "strategy")

        // Persistent state object — survives across evaluate() calls
        js.evaluate("var _state = {};", "state")

        // Console capture — reset each bar in execute(), accumulated into _log
        js.evaluate("""
            var _log = [];
            var console = {
                log: function() { var p=[]; for(var i=0;i<arguments.length;i++) p.push(String(arguments[i])); _log.push(p.join(' ')); },
                warn: function() { var p=['[WARN]']; for(var i=0;i<arguments.length;i++) p.push(String(arguments[i])); _log.push(p.join(' ')); },
                error: function() { var p=['[ERROR]']; for(var i=0;i<arguments.length;i++) p.push(String(arguments[i])); _log.push(p.join(' ')); },
                info: function() { var p=['[INFO]']; for(var i=0;i<arguments.length;i++) p.push(String(arguments[i])); _log.push(p.join(' ')); }
            };
        """.trimIndent(), "console")

        // Null account until caller injects real data
        js.evaluate("var _account = null;", "account")

        worker.js = js
        worker.loadedCode = code
        worker.loadedParams = params
        return js
    }

    private fun closeJs(worker: Worker) {
        worker.js?.close()
        worker.js = null
        worker.loadedCode = null
        worker.loadedParams = null
    }

    // ─── JSON builders ──────────────────────────────────────────────

    private fun buildCandlesJson(candles: List<Candle>): String {
        val sb = StringBuilder("[")
        candles.forEachIndexed { i, c ->
            if (i > 0) sb.append(",")
            sb.append("{\"openTime\":${c.openTime},\"open\":${c.open},\"high\":${c.high},")
            sb.append("\"low\":${c.low},\"close\":${c.close},\"volume\":${c.volume}}")
        }
        sb.append("]")
        return sb.toString()
    }

    private fun buildParamsJson(params: Map<String, String>): String {
        // Names that don't match the identifier regex are skipped entirely.
        // Values are JSON-encoded by Gson so any embedded quotes / backslashes /
        // newlines cannot break out of the JS string literal.
        val safe = params.entries.filter { (k, _) -> SAFE_PARAM_NAME.matches(k) }
        val entries = safe.joinToString(",") { (k, v) -> "${gson.toJson(k)}:${gson.toJson(v)}" }
        return "{$entries}"
    }

    /**
     * Build the per-param `var X = ...;` declarations safely.
     *
     * Param names that are not valid JS identifiers are dropped (NOT injected
     * verbatim) so a hostile preset can't smuggle code by naming a key
     * `}; alert('x'); var x`. Values are emitted as JS literals via Gson, so a
     * value containing quotes or a newline cannot terminate the surrounding
     * string and start a new statement.
     */
    private fun buildSafeParamDecls(params: Map<String, String>): String =
        params.entries
            .filter { (k, _) -> SAFE_PARAM_NAME.matches(k) }
            .joinToString("\n") { (k, v) ->
                val numVal = v.toDoubleOrNull()
                if (numVal != null) "var $k = $numVal;" else "var $k = ${gson.toJson(v)};"
            }

    /** Build JSON object mapping interval keys to candle arrays, e.g. {"4h":[...], "1d":[...]} */
    private fun buildHtfJson(htfCandles: Map<String, List<Candle>>): String {
        val sb = StringBuilder("{")
        htfCandles.entries.forEachIndexed { idx, (interval, candles) ->
            if (idx > 0) sb.append(",")
            sb.append("\"$interval\":")
            sb.append(buildCandlesJson(candles))
        }
        sb.append("}")
        return sb.toString()
    }

    /** Build JSON object mapping symbol names to candle arrays, e.g. {"ETHUSDT":[...], "BNBUSDT":[...]} */
    private fun buildRelatedJson(relatedCandles: Map<String, List<Candle>>): String {
        val sb = StringBuilder("{")
        relatedCandles.entries.forEachIndexed { idx, (symbol, candles) ->
            if (idx > 0) sb.append(",")
            sb.append("\"$symbol\":")
            sb.append(buildCandlesJson(candles))
        }
        sb.append("}")
        return sb.toString()
    }

    // ─── Signal parser (Gson) ───────────────────────────────────────

    /** Flat DTO mirroring the JS object returned by strategy(). */
    private data class SignalDto(
        val type: String? = null,
        val symbol: String? = null,
        val sizePct: Double? = null,
        val stopLossPct: Double? = null,
        val takeProfitPct: Double? = null,
        val takeProfitLevels: List<LevelDto>? = null,
        val stopLossLevels: List<LevelDto>? = null,
        val label: String? = null,
        val orderType: String? = null,
        val limitPrice: Double? = null,
        val marketType: String? = null,
        val leverage: Int? = null,
        val marginType: String? = null
    )
    private data class LevelDto(val pct: Double? = null, val quantityPct: Double? = null)

    private fun dtoToSignal(dto: SignalDto): StrategySignal {
        val type = dto.type?.uppercase() ?: return StrategySignal.HOLD
        val sizePct = dto.sizePct ?: 2.0
        val tpLevels = dto.takeProfitLevels?.mapNotNull { lv ->
            val p = lv.pct ?: return@mapNotNull null; TpSlLevel(p, lv.quantityPct ?: 100.0)
        } ?: emptyList()
        val slLevels = dto.stopLossLevels?.mapNotNull { lv ->
            val p = lv.pct ?: return@mapNotNull null; TpSlLevel(p, lv.quantityPct ?: 100.0)
        } ?: emptyList()
        val signalLabel = SignalLabel.from(dto.label)
        val labelRaw = dto.label ?: ""
        val signalOrdType = SignalOrderType.from(dto.orderType)
        val mkt = runCatching { dto.marketType?.let { MarketType.valueOf(it.uppercase()) } }.getOrNull()
        val mgn = runCatching { dto.marginType?.let { MarginType.valueOf(it.uppercase()) } }.getOrNull()
        val lev = dto.leverage?.coerceIn(1, 125)
        return when (type) {
            "BUY" -> StrategySignal.BUY(sizePct = sizePct, stopLossPct = dto.stopLossPct, takeProfitPct = dto.takeProfitPct,
                takeProfitLevels = tpLevels, stopLossLevels = slLevels,
                label = signalLabel, labelRaw = labelRaw, orderType = signalOrdType, limitPrice = dto.limitPrice,
                marketType = mkt, leverage = lev, marginType = mgn)
            "SELL" -> StrategySignal.SELL(sizePct = sizePct, stopLossPct = dto.stopLossPct, takeProfitPct = dto.takeProfitPct,
                takeProfitLevels = tpLevels, stopLossLevels = slLevels,
                label = signalLabel, labelRaw = labelRaw, orderType = signalOrdType, limitPrice = dto.limitPrice,
                marketType = mkt, leverage = lev, marginType = mgn)
            "CLOSE_IF_LONG" -> StrategySignal.CLOSE_IF_LONG
            "CLOSE_IF_SHORT" -> StrategySignal.CLOSE_IF_SHORT
            else -> StrategySignal.HOLD
        }
    }

    private fun parseSignal(json: String): StrategySignal {
        val clean = json.trim().removeSurrounding("\"").replace("\\\"", "\"")
        if (clean.isEmpty() || clean == "null" || clean == "undefined") return StrategySignal.HOLD
        val dto = try {
            gson.fromJson(clean, SignalDto::class.java) ?: return StrategySignal.HOLD
        } catch (_: Exception) {
            return StrategySignal.HOLD
        }
        return dtoToSignal(dto)
    }

    /** Parse single signal or array of targeted signals (for multi-pair strategies). */
    private fun parseSignalOrList(json: String): List<TargetedSignal> {
        val clean = json.trim().removeSurrounding("\"").replace("\\\"", "\"")
        if (clean.isEmpty() || clean == "null" || clean == "undefined")
            return listOf(TargetedSignal(null, StrategySignal.HOLD))

        // Array format: [{type:'BUY', symbol:'BTCUSDT'}, ...]
        if (clean.trimStart().startsWith("[")) {
            return try {
                val list: List<SignalDto> = gson.fromJson(clean, object : TypeToken<List<SignalDto>>() {}.type)
                    ?: return listOf(TargetedSignal(null, StrategySignal.HOLD))
                list.map { dto -> TargetedSignal(dto.symbol, dtoToSignal(dto)) }
            } catch (_: Exception) {
                listOf(TargetedSignal(null, StrategySignal.HOLD))
            }
        }

        // Single signal
        return listOf(TargetedSignal(null, parseSignal(json)))
    }

    private fun buildAccountJs(account: ScriptAccount?): String {
        if (account == null) return "_account = null;"
        val side = account.positionSide?.let { "\"$it\"" } ?: "null"
        val lastResult = account.lastTradeResult?.let { "\"$it\"" } ?: "null"
        return """_account = {equity:${account.equity},balance:${account.balance},positionSide:$side,positionPnl:${account.positionPnl},positionSize:${account.positionSize},positionEntry:${account.positionEntry},lastTradeResult:$lastResult,pendingOrderCount:${account.pendingOrderCount},consecutiveLosses:${account.consecutiveLosses}};"""
    }

    private fun captureLogs(js: QuickJs): List<String> {
        return try {
            val raw = js.evaluate("JSON.stringify(_log);", "getLogs")?.toString() ?: return emptyList()
            gson.fromJson<List<String>>(raw, object : TypeToken<List<String>>() {}.type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ─── JavaScript indicator library (injected into QuickJS context) ─

    companion object {
        /** Maximum wall-clock time a single strategy() call may run. */
        private const val TIMEOUT_MS = 5_000L

        /**
         * Whitelist of legal JS identifiers we will inject as `var <name> = ...;`.
         * Everything else (including reserved words is acceptable for our use \u2014
         * QuickJS will surface a syntax error) is filtered to prevent code
         * injection via param names from saved templates or shared presets.
         */
        private val SAFE_PARAM_NAME = Regex("^[A-Za-z_][A-Za-z0-9_]{0,63}$")

        val JS_INDICATOR_LIBRARY = """
// ─── Indicator functions available to user scripts ─────────────

function sma(candles, period) {
    if (candles.length < period) return candles[candles.length - 1].close;
    var slice = candles.slice(-period);
    return slice.reduce(function(s, c) { return s + c.close; }, 0) / period;
}

function ema(candles, period) {
    if (candles.length < period) return candles[candles.length - 1].close;
    var mult = 2.0 / (period + 1);
    var val_ = candles.slice(0, period).reduce(function(s, c) { return s + c.close; }, 0) / period;
    for (var i = period; i < candles.length; i++) {
        val_ = (candles[i].close - val_) * mult + val_;
    }
    return val_;
}

function rsi(candles, period) {
    if (candles.length < period + 1) return 50;
    var gains = 0, losses = 0;
    for (var i = 1; i <= period; i++) {
        var change = candles[i].close - candles[i - 1].close;
        if (change > 0) gains += change; else losses -= change;
    }
    var avgGain = gains / period, avgLoss = losses / period;
    for (var i = period + 1; i < candles.length; i++) {
        var change = candles[i].close - candles[i - 1].close;
        avgGain = (avgGain * (period - 1) + (change > 0 ? change : 0)) / period;
        avgLoss = (avgLoss * (period - 1) + (change < 0 ? -change : 0)) / period;
    }
    if (avgLoss === 0) return 100;
    var rs = avgGain / avgLoss;
    return 100 - (100 / (1 + rs));
}

function atr(candles, period) {
    if (candles.length < period + 1) return 0;
    var atrVal = 0;
    for (var i = 1; i <= period; i++) {
        atrVal += Math.max(candles[i].high - candles[i].low,
                           Math.abs(candles[i].high - candles[i-1].close),
                           Math.abs(candles[i].low - candles[i-1].close));
    }
    atrVal /= period;
    for (var i = period + 1; i < candles.length; i++) {
        var tr = Math.max(candles[i].high - candles[i].low,
                          Math.abs(candles[i].high - candles[i-1].close),
                          Math.abs(candles[i].low - candles[i-1].close));
        atrVal = (atrVal * (period - 1) + tr) / period;
    }
    return atrVal;
}

function bollinger(candles, period, numStd) {
    var mid = sma(candles, period);
    var slice = candles.slice(-period);
    var closes = slice.map(function(c) { return c.close; });
    var mean = closes.reduce(function(a,b) { return a+b; }, 0) / closes.length;
    var variance = closes.reduce(function(s, v) { return s + (v - mean) * (v - mean); }, 0) / closes.length;
    var sd = Math.sqrt(variance);
    return { upper: mid + numStd * sd, middle: mid, lower: mid - numStd * sd };
}

function macd(candles, fast, slow, sig) {
    if (candles.length < slow) return { macd: 0, signal: 0, histogram: 0 };
    var mult_f = 2.0 / (fast + 1);
    var mult_s = 2.0 / (slow + 1);
    var emaF = candles.slice(0, fast).reduce(function(s,c){return s+c.close;},0) / fast;
    var emaS = candles.slice(0, slow).reduce(function(s,c){return s+c.close;},0) / slow;
    // Advance fast EMA through bars fast..slow-1 so it doesn't skip any
    for (var i = fast; i < slow; i++) {
        emaF = (candles[i].close - emaF) * mult_f + emaF;
    }
    var macdHist = [];
    for (var i = slow; i < candles.length; i++) {
        emaF = (candles[i].close - emaF) * mult_f + emaF;
        emaS = (candles[i].close - emaS) * mult_s + emaS;
        macdHist.push(emaF - emaS);
    }
    var macdLine = macdHist[macdHist.length - 1];
    // Signal line = EMA of MACD history
    var mult_sig = 2.0 / (sig + 1);
    var sigLen = Math.min(sig, macdHist.length);
    var sigVal = macdHist.slice(0, sigLen).reduce(function(s,v){return s+v;},0) / sigLen;
    for (var j = sigLen; j < macdHist.length; j++) {
        sigVal = (macdHist[j] - sigVal) * mult_sig + sigVal;
    }
    return { macd: macdLine, signal: sigVal, histogram: macdLine - sigVal };
}

function obv(candles) {
    var val_ = 0;
    for (var i = 1; i < candles.length; i++) {
        if (candles[i].close > candles[i-1].close) val_ += candles[i].volume;
        else if (candles[i].close < candles[i-1].close) val_ -= candles[i].volume;
    }
    return val_;
}

function vwap(candles) {
    var cumVol = 0, cumTpVol = 0;
    for (var i = 0; i < candles.length; i++) {
        var tp = (candles[i].high + candles[i].low + candles[i].close) / 3;
        cumTpVol += tp * candles[i].volume;
        cumVol += candles[i].volume;
    }
    return cumVol > 0 ? cumTpVol / cumVol : candles[candles.length-1].close;
}

function supertrend(candles, period, multiplier) {
    if (candles.length < period + 2) return [candles[candles.length-1].close, true];
    var atrVals = [];
    for (var i = 1; i < candles.length; i++) {
        var tr = Math.max(candles[i].high - candles[i].low,
                          Math.abs(candles[i].high - candles[i-1].close),
                          Math.abs(candles[i].low - candles[i-1].close));
        atrVals.push(tr);
    }
    if (atrVals.length < period) return [candles[candles.length-1].close, true];
    var smoothAtr = [atrVals.slice(0, period).reduce(function(a,b){return a+b;},0)/period];
    for (var i = period; i < atrVals.length; i++) {
        smoothAtr.push((smoothAtr[smoothAtr.length-1] * (period-1) + atrVals[i]) / period);
    }
    var st = candles[period].close, isUp = true;
    for (var i = 0; i < smoothAtr.length; i++) {
        var ci = i + 1;
        if (ci >= candles.length) break;
        var hl2 = (candles[ci].high + candles[ci].low) / 2;
        var upper = hl2 + multiplier * smoothAtr[i];
        var lower = hl2 - multiplier * smoothAtr[i];
        if (candles[ci].close > st) { st = lower; isUp = true; }
        else { st = upper; isUp = false; }
    }
    return [st, isUp];
}

function stochastic(candles, kPeriod, dPeriod) {
    if (candles.length < kPeriod) return [50, 50];
    var kVals = [];
    for (var i = kPeriod - 1; i < candles.length; i++) {
        var slice = candles.slice(i - kPeriod + 1, i + 1);
        var high = Math.max.apply(null, slice.map(function(c){return c.high;}));
        var low = Math.min.apply(null, slice.map(function(c){return c.low;}));
        var k = (high - low) > 0 ? (candles[i].close - low) / (high - low) * 100 : 50;
        kVals.push(k);
    }
    var k = kVals[kVals.length - 1];
    var d = kVals.length >= dPeriod ? kVals.slice(-dPeriod).reduce(function(a,b){return a+b;},0)/dPeriod : k;
    return [k, d];
}

function ichimoku(candles, tenkan, kijun, senkouB) {
    function donchianMid(data, p) {
        if (data.length < p) return data[data.length-1].close;
        var s = data.slice(-p);
        return (Math.max.apply(null,s.map(function(c){return c.high;})) + Math.min.apply(null,s.map(function(c){return c.low;}))) / 2;
    }
    var t = donchianMid(candles, tenkan), kj = donchianMid(candles, kijun);
    return {tenkan:t, kijun:kj, senkouA:(t+kj)/2, senkouB:donchianMid(candles,senkouB), chikou:candles[candles.length-1].close};
}

function stochasticRsi(candles, rsiPeriod, stochPeriod, kSmooth, dSmooth) {
    if (candles.length < rsiPeriod + stochPeriod) return [50, 50];
    var rsiVals = [];
    for (var i = rsiPeriod; i < candles.length; i++) {
        var gains = 0, losses = 0;
        for (var j = i - rsiPeriod + 1; j <= i; j++) {
            var change = candles[j].close - candles[j-1].close;
            if (change > 0) gains += change; else losses -= change;
        }
        var ag = gains / rsiPeriod, al = losses / rsiPeriod;
        rsiVals.push(al === 0 ? 100 : 100 - (100 / (1 + ag / al)));
    }
    if (rsiVals.length < stochPeriod) return [50, 50];
    var kVals = [];
    for (var i = stochPeriod - 1; i < rsiVals.length; i++) {
        var w = rsiVals.slice(i - stochPeriod + 1, i + 1);
        var hi = Math.max.apply(null, w), lo = Math.min.apply(null, w);
        kVals.push((hi - lo) > 0 ? (rsiVals[i] - lo) / (hi - lo) * 100 : 50);
    }
    var smoothK = kVals;
    if (kSmooth > 1 && kVals.length >= kSmooth) {
        smoothK = [];
        for (var i = kSmooth - 1; i < kVals.length; i++) {
            smoothK.push(kVals.slice(i - kSmooth + 1, i + 1).reduce(function(a,b){return a+b;},0) / kSmooth);
        }
    }
    var k = smoothK[smoothK.length - 1];
    var d = k;
    if (dSmooth > 1 && smoothK.length >= dSmooth) {
        d = smoothK.slice(-dSmooth).reduce(function(a,b){return a+b;},0) / dSmooth;
    }
    return [k, d];
}

function adx(candles, period) {
    if (candles.length < period * 2 + 1) return 25;
    var pDM = [], nDM = [], trVals = [];
    for (var i = 1; i < candles.length; i++) {
        var upMove = candles[i].high - candles[i-1].high;
        var downMove = candles[i-1].low - candles[i].low;
        pDM.push(upMove > downMove && upMove > 0 ? upMove : 0);
        nDM.push(downMove > upMove && downMove > 0 ? downMove : 0);
        trVals.push(Math.max(candles[i].high - candles[i].low,
                             Math.abs(candles[i].high - candles[i-1].close),
                             Math.abs(candles[i].low - candles[i-1].close)));
    }
    var smoothTR = trVals.slice(0, period).reduce(function(a,b){return a+b;},0);
    var smoothPDM = pDM.slice(0, period).reduce(function(a,b){return a+b;},0);
    var smoothNDM = nDM.slice(0, period).reduce(function(a,b){return a+b;},0);
    var dxVals = [];
    for (var i = period; i < trVals.length; i++) {
        if (i > period) {
            smoothTR = smoothTR - smoothTR / period + trVals[i];
            smoothPDM = smoothPDM - smoothPDM / period + pDM[i];
            smoothNDM = smoothNDM - smoothNDM / period + nDM[i];
        }
        var pDI = smoothTR > 0 ? 100 * smoothPDM / smoothTR : 0;
        var nDI = smoothTR > 0 ? 100 * smoothNDM / smoothTR : 0;
        var diSum = pDI + nDI;
        dxVals.push(diSum > 0 ? 100 * Math.abs(pDI - nDI) / diSum : 0);
    }
    if (dxVals.length < period) return dxVals.length > 0 ? dxVals[dxVals.length - 1] : 25;
    var adxVal = dxVals.slice(0, period).reduce(function(a,b){return a+b;},0) / period;
    for (var i = period; i < dxVals.length; i++) {
        adxVal = (adxVal * (period - 1) + dxVals[i]) / period;
    }
    return adxVal;
}

function dmi(candles, period) {
    if (!period) period = 14;
    if (candles.length < period * 2 + 1) return {diPlus:0, diMinus:0, adx:25};
    var pDM = [], nDM = [], trVals = [];
    for (var i = 1; i < candles.length; i++) {
        var upMove = candles[i].high - candles[i-1].high;
        var downMove = candles[i-1].low - candles[i].low;
        pDM.push(upMove > downMove && upMove > 0 ? upMove : 0);
        nDM.push(downMove > upMove && downMove > 0 ? downMove : 0);
        trVals.push(Math.max(candles[i].high - candles[i].low,
                             Math.abs(candles[i].high - candles[i-1].close),
                             Math.abs(candles[i].low - candles[i-1].close)));
    }
    var smoothTR = trVals.slice(0, period).reduce(function(a,b){return a+b;},0);
    var smoothPDM = pDM.slice(0, period).reduce(function(a,b){return a+b;},0);
    var smoothNDM = nDM.slice(0, period).reduce(function(a,b){return a+b;},0);
    var dxVals = [];
    var lastPDI = 0, lastNDI = 0;
    for (var i = period; i < trVals.length; i++) {
        if (i > period) {
            smoothTR = smoothTR - smoothTR / period + trVals[i];
            smoothPDM = smoothPDM - smoothPDM / period + pDM[i];
            smoothNDM = smoothNDM - smoothNDM / period + nDM[i];
        }
        lastPDI = smoothTR > 0 ? 100 * smoothPDM / smoothTR : 0;
        lastNDI = smoothTR > 0 ? 100 * smoothNDM / smoothTR : 0;
        var diSum = lastPDI + lastNDI;
        dxVals.push(diSum > 0 ? 100 * Math.abs(lastPDI - lastNDI) / diSum : 0);
    }
    if (dxVals.length < period) {
        var lastDx = dxVals.length > 0 ? dxVals[dxVals.length - 1] : 25;
        return {diPlus: lastPDI, diMinus: lastNDI, adx: lastDx};
    }
    var adxV = dxVals.slice(0, period).reduce(function(a,b){return a+b;},0) / period;
    for (var i = period; i < dxVals.length; i++) {
        adxV = (adxV * (period - 1) + dxVals[i]) / period;
    }
    return {diPlus: lastPDI, diMinus: lastNDI, adx: adxV};
}

function cci(candles, period) {
    if (candles.length < period) return 0;
    var slice = candles.slice(-period);
    var tps = slice.map(function(c){return (c.high+c.low+c.close)/3;});
    var mean = tps.reduce(function(a,b){return a+b;},0)/tps.length;
    var md = tps.reduce(function(a,v){return a+Math.abs(v-mean);},0)/tps.length;
    return md > 0 ? (tps[tps.length-1] - mean) / (0.015 * md) : 0;
}

function mfi(candles, period) {
    if (candles.length < period + 1) return 50;
    var posFlow = 0, negFlow = 0;
    for (var i = candles.length - period; i < candles.length; i++) {
        var tp = (candles[i].high + candles[i].low + candles[i].close) / 3;
        var prevTp = (candles[i-1].high + candles[i-1].low + candles[i-1].close) / 3;
        var rawMf = tp * candles[i].volume;
        if (tp > prevTp) posFlow += rawMf;
        else if (tp < prevTp) negFlow += rawMf;
    }
    if (negFlow === 0) return 100;
    var mfRatio = posFlow / negFlow;
    return 100 - (100 / (1 + mfRatio));
}

function williamsR(candles, period) {
    if (candles.length < period) return -50;
    var s = candles.slice(-period);
    var high = Math.max.apply(null,s.map(function(c){return c.high;}));
    var low = Math.min.apply(null,s.map(function(c){return c.low;}));
    return (high-low) > 0 ? -100*(high-candles[candles.length-1].close)/(high-low) : -50;
}

// ─── Series functions (return array, one value per candle) ─────────
// These are used by custom indicators to return plottable data arrays.

function smaSeries(candles, period) {
    var result = [];
    for (var i = 0; i < candles.length; i++) {
        if (i < period - 1) { result.push(null); continue; }
        var sum = 0;
        for (var j = i - period + 1; j <= i; j++) sum += candles[j].close;
        result.push(sum / period);
    }
    return result;
}

function emaSeries(candles, period) {
    if (candles.length === 0) return [];
    var result = [];
    var mult = 2.0 / (period + 1);
    var val_ = 0;
    for (var i = 0; i < candles.length; i++) {
        if (i < period - 1) { result.push(null); continue; }
        if (i === period - 1) {
            var sum = 0;
            for (var j = 0; j < period; j++) sum += candles[j].close;
            val_ = sum / period;
            result.push(val_);
        } else {
            val_ = (candles[i].close - val_) * mult + val_;
            result.push(val_);
        }
    }
    return result;
}

function rsiSeries(candles, period) {
    var result = [];
    if (candles.length < period + 1) {
        for (var i = 0; i < candles.length; i++) result.push(null);
        return result;
    }
    var gains = 0, losses = 0;
    for (var i = 1; i <= period; i++) {
        var ch = candles[i].close - candles[i - 1].close;
        if (ch > 0) gains += ch; else losses -= ch;
    }
    var avgGain = gains / period, avgLoss = losses / period;
    for (var i = 0; i < period; i++) result.push(null);
    result.push(avgLoss === 0 ? 100 : 100 - (100 / (1 + avgGain / avgLoss)));
    for (var i = period + 1; i < candles.length; i++) {
        var ch = candles[i].close - candles[i - 1].close;
        avgGain = (avgGain * (period - 1) + (ch > 0 ? ch : 0)) / period;
        avgLoss = (avgLoss * (period - 1) + (ch < 0 ? -ch : 0)) / period;
        result.push(avgLoss === 0 ? 100 : 100 - (100 / (1 + avgGain / avgLoss)));
    }
    return result;
}

function atrSeries(candles, period) {
    var result = [null];
    if (candles.length < 2) return [null];
    var trVals = [];
    for (var i = 1; i < candles.length; i++) {
        trVals.push(Math.max(candles[i].high - candles[i].low,
            Math.abs(candles[i].high - candles[i-1].close),
            Math.abs(candles[i].low - candles[i-1].close)));
    }
    for (var i = 0; i < trVals.length; i++) {
        if (i < period - 1) { result.push(null); continue; }
        if (i === period - 1) {
            var sum = 0;
            for (var j = 0; j < period; j++) sum += trVals[j];
            result.push(sum / period);
        } else {
            var prev = result[result.length - 1];
            result.push((prev * (period - 1) + trVals[i]) / period);
        }
    }
    return result;
}

function bollingerSeries(candles, period, numStd) {
    var upper = [], middle = [], lower = [];
    for (var i = 0; i < candles.length; i++) {
        if (i < period - 1) { upper.push(null); middle.push(null); lower.push(null); continue; }
        var sum = 0;
        for (var j = i - period + 1; j <= i; j++) sum += candles[j].close;
        var mean = sum / period;
        var variance = 0;
        for (var j = i - period + 1; j <= i; j++) variance += (candles[j].close - mean) * (candles[j].close - mean);
        var sd = Math.sqrt(variance / period);
        upper.push(mean + numStd * sd);
        middle.push(mean);
        lower.push(mean - numStd * sd);
    }
    return { upper: upper, middle: middle, lower: lower };
}

function macdSeries(candles, fast, slow, sig) {
    var emaF = emaSeries(candles, fast);
    var emaS = emaSeries(candles, slow);
    var macdLine = [], signalLine = [], histogram = [];
    var macdVals = [];
    for (var i = 0; i < candles.length; i++) {
        if (emaF[i] === null || emaS[i] === null) {
            macdLine.push(null); signalLine.push(null); histogram.push(null);
        } else {
            var m = emaF[i] - emaS[i];
            macdLine.push(m);
            macdVals.push(m);
        }
    }
    // Compute signal line as EMA of MACD values
    var sigMult = 2.0 / (sig + 1);
    var sigVal = null;
    var sigIdx = 0;
    for (var i = 0; i < candles.length; i++) {
        if (macdLine[i] === null) { signalLine.push(null); histogram.push(null); continue; }
        sigIdx++;
        if (sigIdx < sig) { signalLine.push(null); histogram.push(null); continue; }
        if (sigIdx === sig) {
            var sum = 0;
            for (var j = macdVals.length - sig; j < macdVals.length; j++) sum += macdVals[j];
            sigVal = sum / sig;
        } else {
            sigVal = (macdLine[i] - sigVal) * sigMult + sigVal;
        }
        signalLine.push(sigVal);
        histogram.push(macdLine[i] - sigVal);
    }
    return { macd: macdLine, signal: signalLine, histogram: histogram };
}

function obvSeries(candles) {
    var result = [0];
    var val_ = 0;
    for (var i = 1; i < candles.length; i++) {
        if (candles[i].close > candles[i-1].close) val_ += candles[i].volume;
        else if (candles[i].close < candles[i-1].close) val_ -= candles[i].volume;
        result.push(val_);
    }
    return result;
}

function vwapSeries(candles) {
    var result = [];
    var cumVol = 0, cumTpVol = 0;
    for (var i = 0; i < candles.length; i++) {
        var tp = (candles[i].high + candles[i].low + candles[i].close) / 3;
        cumTpVol += tp * candles[i].volume;
        cumVol += candles[i].volume;
        result.push(cumVol > 0 ? cumTpVol / cumVol : candles[i].close);
    }
    return result;
}

function stochasticSeries(candles, kPeriod, dPeriod) {
    var kVals = [], dVals = [];
    for (var i = 0; i < candles.length; i++) {
        if (i < kPeriod - 1) { kVals.push(null); dVals.push(null); continue; }
        var slice = candles.slice(i - kPeriod + 1, i + 1);
        var high = Math.max.apply(null, slice.map(function(c){return c.high;}));
        var low = Math.min.apply(null, slice.map(function(c){return c.low;}));
        var k = (high - low) > 0 ? (candles[i].close - low) / (high - low) * 100 : 50;
        kVals.push(k);
        // D = SMA of last dPeriod K values
        var validK = kVals.filter(function(v){return v !== null;});
        if (validK.length >= dPeriod) {
            var sum = 0;
            for (var j = validK.length - dPeriod; j < validK.length; j++) sum += validK[j];
            dVals.push(sum / dPeriod);
        } else {
            dVals.push(null);
        }
    }
    return { k: kVals, d: dVals };
}

// ─── WMA / VWMA / HMA / DEMA / TEMA ────────────────────────────────────────

function wmaSeries(candles, period) {
    var result = [];
    var weightSum = period * (period + 1) / 2;
    for (var i = 0; i < candles.length; i++) {
        if (i < period - 1) { result.push(null); continue; }
        var sum = 0;
        for (var j = 0; j < period; j++) {
            sum += candles[i - period + 1 + j].close * (j + 1);
        }
        result.push(sum / weightSum);
    }
    return result;
}

function vwmaSeries(candles, period) {
    var result = [];
    for (var i = 0; i < candles.length; i++) {
        if (i < period - 1) { result.push(null); continue; }
        var volSum = 0, priceVolSum = 0;
        for (var j = i - period + 1; j <= i; j++) {
            volSum += candles[j].volume;
            priceVolSum += candles[j].close * candles[j].volume;
        }
        result.push(volSum > 0 ? priceVolSum / volSum : candles[i].close);
    }
    return result;
}

function hmaSeries(candles, period) {
    var half = Math.max(Math.floor(period / 2), 1);
    var wmaHalf = wmaSeries(candles, half);
    var wmaFull = wmaSeries(candles, period);
    // Raw HMA input = 2*WMA(half) - WMA(full)
    var raw = [];
    for (var i = 0; i < candles.length; i++) {
        if (wmaHalf[i] === null || wmaFull[i] === null) { raw.push(null); continue; }
        raw.push(2 * wmaHalf[i] - wmaFull[i]);
    }
    // Apply WMA(sqrt(period)) over raw
    var sqrtP = Math.max(Math.round(Math.sqrt(period)), 1);
    var weightSum = sqrtP * (sqrtP + 1) / 2;
    var result = [];
    for (var i = 0; i < raw.length; i++) {
        if (i < sqrtP - 1 || raw[i] === null) { result.push(null); continue; }
        var ok = true, sum = 0;
        for (var j = 0; j < sqrtP; j++) {
            if (raw[i - sqrtP + 1 + j] === null) { ok = false; break; }
            sum += raw[i - sqrtP + 1 + j] * (j + 1);
        }
        result.push(ok ? sum / weightSum : null);
    }
    return result;
}

function demaSeries(candles, period) {
    var ema1 = emaSeries(candles, period);
    // Build EMA of EMA
    var ema2 = [];
    var mult = 2.0 / (period + 1);
    var val_ = null, count = 0, sumAcc = 0;
    for (var i = 0; i < ema1.length; i++) {
        if (ema1[i] === null) { ema2.push(null); continue; }
        if (val_ === null) {
            sumAcc += ema1[i]; count++;
            if (count === period) { val_ = sumAcc / period; ema2.push(2 * ema1[i] - val_); }
            else ema2.push(null);
        } else {
            val_ = (ema1[i] - val_) * mult + val_;
            ema2.push(2 * ema1[i] - val_);
        }
    }
    return ema2;
}

function temaSeries(candles, period) {
    var ema1 = emaSeries(candles, period);
    var mult = 2.0 / (period + 1);
    // EMA2
    var ema2 = [];
    var v2 = null, c2 = 0, s2 = 0;
    for (var i = 0; i < ema1.length; i++) {
        if (ema1[i] === null) { ema2.push(null); continue; }
        if (v2 === null) {
            s2 += ema1[i]; c2++;
            if (c2 === period) { v2 = s2 / period; ema2.push(v2); } else ema2.push(null);
        } else {
            v2 = (ema1[i] - v2) * mult + v2; ema2.push(v2);
        }
    }
    // EMA3
    var ema3 = [];
    var v3 = null, c3 = 0, s3 = 0;
    for (var i = 0; i < ema2.length; i++) {
        if (ema2[i] === null) { ema3.push(null); continue; }
        if (v3 === null) {
            s3 += ema2[i]; c3++;
            if (c3 === period) { v3 = s3 / period; ema3.push(v3); } else ema3.push(null);
        } else {
            v3 = (ema2[i] - v3) * mult + v3; ema3.push(v3);
        }
    }
    var result = [];
    for (var i = 0; i < candles.length; i++) {
        if (ema1[i] === null || ema2[i] === null || ema3[i] === null) { result.push(null); continue; }
        result.push(3 * ema1[i] - 3 * ema2[i] + ema3[i]);
    }
    return result;
}

// ─── ADX / CCI / MFI / Williams%R / CMF ─────────────────────────────────────

function adxSeries(candles, period) {
    var n = candles.length;
    var result = [];
    if (n < 2) { for (var i = 0; i < n; i++) result.push(null); return result; }
    var trList = [], plusDmList = [], minusDmList = [];
    for (var i = 1; i < n; i++) {
        var h = candles[i].high, l = candles[i].low, pc = candles[i-1].close;
        var ph = candles[i-1].high, pl = candles[i-1].low;
        trList.push(Math.max(h - l, Math.abs(h - pc), Math.abs(l - pc)));
        var up = h - ph, down = pl - l;
        plusDmList.push(up > down && up > 0 ? up : 0);
        minusDmList.push(down > up && down > 0 ? down : 0);
    }
    // Wilder smooth
    function wilderSmooth(data, p) {
        if (data.length < p) return [];
        var out = [];
        var sum = 0;
        for (var i = 0; i < p; i++) sum += data[i];
        out.push(sum);
        for (var i = p; i < data.length; i++) {
            sum = sum - sum / p + data[i];
            out.push(sum);
        }
        return out;
    }
    var sTr = wilderSmooth(trList, period);
    var sPDm = wilderSmooth(plusDmList, period);
    var sMDm = wilderSmooth(minusDmList, period);
    var diPlus = [], diMinus = [], dx = [];
    for (var i = 0; i < sTr.length; i++) {
        var dp = sTr[i] > 0 ? 100 * sPDm[i] / sTr[i] : 0;
        var dm = sTr[i] > 0 ? 100 * sMDm[i] / sTr[i] : 0;
        diPlus.push(dp); diMinus.push(dm);
        var s = dp + dm;
        dx.push(s > 0 ? 100 * Math.abs(dp - dm) / s : 0);
    }
    var adxVals = [];
    if (dx.length >= period) {
        var sum = 0;
        for (var i = 0; i < period; i++) sum += dx[i];
        adxVals.push(sum / period);
        for (var i = period; i < dx.length; i++) {
            adxVals.push((adxVals[adxVals.length - 1] * (period - 1) + dx[i]) / period);
        }
    }
    // Align: trList starts at index 1, sTr starts at index period (relative to trList = period+1 candle index)
    var offset = period + period; // need 2*period candles for first ADX value
    for (var i = 0; i < n; i++) {
        if (i < offset) result.push(null);
        else result.push(adxVals[i - offset] !== undefined ? adxVals[i - offset] : null);
    }
    return result;
}

function cciSeries(candles, period) {
    var result = [];
    for (var i = 0; i < candles.length; i++) {
        if (i < period - 1) { result.push(null); continue; }
        var slice = candles.slice(i - period + 1, i + 1);
        var tps = slice.map(function(c){ return (c.high + c.low + c.close) / 3; });
        var mean = tps.reduce(function(a,b){return a+b;}, 0) / period;
        var meanDev = tps.map(function(v){return Math.abs(v - mean);}).reduce(function(a,b){return a+b;}, 0) / period;
        result.push(meanDev > 0 ? (tps[tps.length-1] - mean) / (0.015 * meanDev) : 0);
    }
    return result;
}

function mfiSeries(candles, period) {
    var result = [];
    for (var i = 0; i < candles.length; i++) {
        if (i < period) { result.push(null); continue; }
        var posFlow = 0, negFlow = 0;
        for (var j = i - period + 1; j <= i; j++) {
            var tp  = (candles[j].high + candles[j].low + candles[j].close) / 3;
            var ptp = (candles[j-1].high + candles[j-1].low + candles[j-1].close) / 3;
            var raw = tp * candles[j].volume;
            if (tp > ptp) posFlow += raw;
            else if (tp < ptp) negFlow += raw;
        }
        result.push(negFlow === 0 ? 100 : 100 - (100 / (1 + posFlow / negFlow)));
    }
    return result;
}

function williamsRSeries(candles, period) {
    var result = [];
    for (var i = 0; i < candles.length; i++) {
        if (i < period - 1) { result.push(null); continue; }
        var slice = candles.slice(i - period + 1, i + 1);
        var high = Math.max.apply(null, slice.map(function(c){return c.high;}));
        var low  = Math.min.apply(null, slice.map(function(c){return c.low;}));
        result.push(high - low > 0 ? -100 * (high - candles[i].close) / (high - low) : -50);
    }
    return result;
}

function cmfSeries(candles, period) {
    var result = [];
    for (var i = 0; i < candles.length; i++) {
        if (i < period - 1) { result.push(null); continue; }
        var mfVol = 0, totVol = 0;
        for (var j = i - period + 1; j <= i; j++) {
            var c = candles[j];
            var clv = c.high !== c.low ? ((c.close - c.low) - (c.high - c.close)) / (c.high - c.low) : 0;
            mfVol += clv * c.volume; totVol += c.volume;
        }
        result.push(totVol > 0 ? mfVol / totVol : 0);
    }
    return result;
}

// ─── Supertrend Series ───────────────────────────────────────────────────────

function supertrendSeries(candles, period, multiplier) {
    if (period === undefined) period = 10;
    if (multiplier === undefined) multiplier = 3.0;
    var n = candles.length;
    var upLine  = [], downLine = [], direction = [];
    for (var i = 0; i < n; i++) { upLine.push(null); downLine.push(null); direction.push(null); }
    if (n < period + 2) return { up: upLine, down: downLine, direction: direction };
    var trList = [];
    for (var i = 1; i < n; i++) {
        var h = candles[i].high, l = candles[i].low, pc = candles[i-1].close;
        trList.push(Math.max(h - l, Math.abs(h - pc), Math.abs(l - pc)));
    }
    // Initial ATR = simple average of first `period` TR values
    var atr = 0;
    for (var i = 0; i < period; i++) atr += trList[i];
    atr /= period;
    var atrSmooth = [atr];
    for (var i = period; i < trList.length; i++) {
        atr = (atrSmooth[atrSmooth.length - 1] * (period - 1) + trList[i]) / period;
        atrSmooth.push(atr);
    }
    var st = candles[period].close, isUp = true;
    for (var i = 0; i < atrSmooth.length; i++) {
        var ci = i + 1;
        if (ci >= n) break;
        var hl2 = (candles[ci].high + candles[ci].low) / 2;
        var ub = hl2 + multiplier * atrSmooth[i];
        var lb = hl2 - multiplier * atrSmooth[i];
        if (candles[ci].close > st) { st = lb; isUp = true; }
        else { st = ub; isUp = false; }
        if (isUp) { upLine[ci] = st; direction[ci] = 1; }
        else { downLine[ci] = st; direction[ci] = -1; }
    }
    return { up: upLine, down: downLine, direction: direction };
}

// ─── Ichimoku Series ─────────────────────────────────────────────────────────

function ichimokuSeries(candles, tenkanP, kijunP, senkouBP) {
    if (tenkanP  === undefined) tenkanP  = 9;
    if (kijunP   === undefined) kijunP   = 26;
    if (senkouBP === undefined) senkouBP = 52;
    var n = candles.length;
    function mid(endIdx, p) {
        if (endIdx + 1 < p) return null;
        var s = candles.slice(endIdx + 1 - p, endIdx + 1);
        var hi = s[0].high, lo = s[0].low;
        for (var i = 1; i < s.length; i++) {
            if (s[i].high > hi) hi = s[i].high;
            if (s[i].low  < lo) lo = s[i].low;
        }
        return (hi + lo) / 2;
    }
    var tenkan = [], kijun = [], senkouA = [], senkouB = [], chikou = [];
    for (var i = 0; i < n; i++) {
        var t = mid(i, tenkanP), k = mid(i, kijunP);
        tenkan.push(t); kijun.push(k);
        senkouA.push(t !== null && k !== null ? (t + k) / 2 : null);
        senkouB.push(mid(i, senkouBP));
        chikou.push(i + kijunP < n ? candles[i].close : null);
    }
    return { tenkan: tenkan, kijun: kijun, senkouA: senkouA, senkouB: senkouB, chikou: chikou };
}

// ─── Stochastic RSI Series ───────────────────────────────────────────────────

function stochasticRsiSeries(candles, rsiPeriod, stochPeriod, kSmooth, dSmooth) {
    if (rsiPeriod  === undefined) rsiPeriod  = 14;
    if (stochPeriod === undefined) stochPeriod = 14;
    if (kSmooth    === undefined) kSmooth    = 3;
    if (dSmooth    === undefined) dSmooth    = 3;
    var rsiVals = rsiSeries(candles, rsiPeriod);
    var n = rsiVals.length;
    var stochRaw = [];
    for (var i = 0; i < n; i++) {
        if (i < stochPeriod - 1 || rsiVals[i] === null) { stochRaw.push(null); continue; }
        var slice = rsiVals.slice(i - stochPeriod + 1, i + 1);
        var hi = slice[0], lo = slice[0];
        for (var j = 1; j < slice.length; j++) {
            if (slice[j] > hi) hi = slice[j];
            if (slice[j] < lo) lo = slice[j];
        }
        stochRaw.push(hi - lo > 0 ? (rsiVals[i] - lo) / (hi - lo) * 100 : 50);
    }
    // Smooth K
    var kLine = [];
    for (var i = 0; i < n; i++) {
        if (i < kSmooth - 1) { kLine.push(null); continue; }
        var window = [];
        for (var j = i - kSmooth + 1; j <= i; j++) { if (stochRaw[j] !== null) window.push(stochRaw[j]); }
        kLine.push(window.length === kSmooth ? window.reduce(function(a,b){return a+b;},0) / kSmooth : null);
    }
    // Smooth D
    var dLine = [];
    for (var i = 0; i < n; i++) {
        if (i < dSmooth - 1) { dLine.push(null); continue; }
        var window = [];
        for (var j = i - dSmooth + 1; j <= i; j++) { if (kLine[j] !== null) window.push(kLine[j]); }
        dLine.push(window.length === dSmooth ? window.reduce(function(a,b){return a+b;},0) / dSmooth : null);
    }
    return { k: kLine, d: dLine };
}

// ─── Helper Utilities ────────────────────────────────────────────────────────

/** crossover(a, b, i) — true if series a crossed above series b at index i */
function crossover(a, b, i) {
    if (i < 1 || a[i] === null || b[i] === null || a[i-1] === null || b[i-1] === null) return false;
    return a[i-1] <= b[i-1] && a[i] > b[i];
}

/** crossunder(a, b, i) — true if series a crossed below series b at index i */
function crossunder(a, b, i) {
    if (i < 1 || a[i] === null || b[i] === null || a[i-1] === null || b[i-1] === null) return false;
    return a[i-1] >= b[i-1] && a[i] < b[i];
}

/** highest(arr, period, i) — highest non-null value in arr[i-period+1 .. i] */
function highest(arr, period, i) {
    if (i < period - 1) return null;
    var best = null;
    for (var j = i - period + 1; j <= i; j++) {
        if (arr[j] !== null && (best === null || arr[j] > best)) best = arr[j];
    }
    return best;
}

/** lowest(arr, period, i) — lowest non-null value in arr[i-period+1 .. i] */
function lowest(arr, period, i) {
    if (i < period - 1) return null;
    var best = null;
    for (var j = i - period + 1; j <= i; j++) {
        if (arr[j] !== null && (best === null || arr[j] < best)) best = arr[j];
    }
    return best;
}

/** change(arr, i, lookback) — arr[i] - arr[i - lookback] */
function change(arr, i, lookback) {
    if (lookback === undefined) lookback = 1;
    if (i < lookback || arr[i] === null || arr[i - lookback] === null) return null;
    return arr[i] - arr[i - lookback];
}

// ─── Source Selector ─────────────────────────────────────────────────────────

/** source(candles, field) — extract a field ("open","high","low","close","volume","hl2","hlc3","ohlc4") */
function source(candles, field) {
    if (field === undefined) field = "close";
    return candles.map(function(c) {
        switch(field) {
            case "open":   return c.open;
            case "high":   return c.high;
            case "low":    return c.low;
            case "close":  return c.close;
            case "volume": return c.volume;
            case "hl2":    return (c.high + c.low) / 2;
            case "hlc3":   return (c.high + c.low + c.close) / 3;
            case "ohlc4":  return (c.open + c.high + c.low + c.close) / 4;
            default:       return c.close;
        }
    });
}

// ─── Color Constants & Helpers ───────────────────────────────────────────────

var color = {
    green:     "#26A69A",
    red:       "#EF5350",
    blue:      "#2196F3",
    orange:    "#FF9800",
    yellow:    "#FFD700",
    purple:    "#9C27B0",
    white:     "#FFFFFF",
    gray:      "#8B949E",
    aqua:      "#00BCD4",
    lime:      "#00E676",
    pink:      "#FF4081",
    silver:    "#C0C0C0",
    none:      "transparent",
    rgba: function(r, g, b, a) {
        if (a === undefined) a = 1.0;
        return "rgba(" + r + "," + g + "," + b + "," + a + ")";
    },
    hexAlpha: function(hex, alpha) {
        var a = Math.round(alpha * 255).toString(16);
        if (a.length === 1) a = "0" + a;
        return hex + a;
    }
};

// ─── Linear Regression ───────────────────────────────────────────────────────

/** linreg(arr, period, i) — linear regression value at index i over last `period` bars */
function linreg(arr, period, i) {
    if (i < period - 1) return null;
    var sumX = 0, sumY = 0, sumXY = 0, sumXX = 0, n = 0;
    for (var j = 0; j < period; j++) {
        var idx = i - period + 1 + j;
        if (arr[idx] === null) return null;
        sumX += j; sumY += arr[idx]; sumXY += j * arr[idx]; sumXX += j * j;
        n++;
    }
    var slope = (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX);
    var intercept = (sumY - slope * sumX) / n;
    return intercept + slope * (period - 1);
}

/** linregSeries(arr, period) — full linear regression series */
function linregSeries(arr, period) {
    var result = [];
    for (var i = 0; i < arr.length; i++) {
        result.push(linreg(arr, period, i));
    }
    return result;
}

// ─── Pivot Points ────────────────────────────────────────────────────────────

/** pivotHigh(candles, leftBars, rightBars) — array of pivot-high prices (null where no pivot) */
function pivotHigh(candles, leftBars, rightBars) {
    var result = [];
    for (var i = 0; i < candles.length; i++) {
        if (i < leftBars || i + rightBars >= candles.length) { result.push(null); continue; }
        var isHigh = true;
        for (var j = i - leftBars; j <= i + rightBars; j++) {
            if (j !== i && candles[j].high >= candles[i].high) { isHigh = false; break; }
        }
        result.push(isHigh ? candles[i].high : null);
    }
    return result;
}

/** pivotLow(candles, leftBars, rightBars) — array of pivot-low prices (null where no pivot) */
function pivotLow(candles, leftBars, rightBars) {
    var result = [];
    for (var i = 0; i < candles.length; i++) {
        if (i < leftBars || i + rightBars >= candles.length) { result.push(null); continue; }
        var isLow = true;
        for (var j = i - leftBars; j <= i + rightBars; j++) {
            if (j !== i && candles[j].low <= candles[i].low) { isLow = false; break; }
        }
        result.push(isLow ? candles[i].low : null);
    }
    return result;
}

// ═══════════════════════════════════════════════════════════════════════════════
// EXPANDED INDICATOR LIBRARY — 200+ functions
// ═══════════════════════════════════════════════════════════════════════════════

// ─── Additional Moving Averages ──────────────────────────────────────────────

function wma(candles, period) {
    if (candles.length < period) return candles[candles.length-1].close;
    var ws = period*(period+1)/2, sum=0;
    for (var i=0;i<period;i++) sum += candles[candles.length-period+i].close*(i+1);
    return sum/ws;
}

function vwma(candles, period) {
    if (candles.length < period) return candles[candles.length-1].close;
    var vSum=0,pvSum=0;
    for (var i=candles.length-period;i<candles.length;i++){
        vSum+=candles[i].volume; pvSum+=candles[i].close*candles[i].volume;
    }
    return vSum>0?pvSum/vSum:candles[candles.length-1].close;
}

function hullMa(candles, period) {
    var half=Math.max(Math.floor(period/2),1);
    var w1=wma(candles,half), w2=wma(candles,period);
    return 2*w1-w2;
}

function dema(candles, period) {
    var e1=ema(candles,period);
    // approximate DEMA
    var mult=2.0/(period+1), val_=0, cnt=0;
    for (var i=0;i<candles.length;i++){
        if(i<period-1) continue;
        if(cnt===0){var s=0;for(var j=0;j<period;j++)s+=candles[j].close;val_=s/period;cnt++;}
        else{val_=(candles[i].close-val_)*mult+val_;cnt++;}
    }
    var e2=val_;
    return 2*e1-e2;
}

function tema(candles, period) {
    var e1=ema(candles,period);
    return e1; // simplified — full TEMA uses series internally
}

/** Zero-Lag EMA */
function zlema(candles, period) {
    if (candles.length < period) return candles[candles.length-1].close;
    var lag = Math.floor((period-1)/2);
    var mult = 2.0/(period+1);
    var adjusted = [];
    for (var i=0;i<candles.length;i++){
        var idx = i-lag;
        var delagged = idx>=0 ? 2*candles[i].close - candles[idx].close : candles[i].close;
        adjusted.push({close:delagged,high:candles[i].high,low:candles[i].low,open:candles[i].open,volume:candles[i].volume,openTime:candles[i].openTime});
    }
    return ema(adjusted, period);
}

function zlemaSeries(candles, period) {
    var lag = Math.floor((period-1)/2);
    var adjusted = [];
    for (var i=0;i<candles.length;i++){
        var idx=i-lag;
        var d=idx>=0?2*candles[i].close-candles[idx].close:candles[i].close;
        adjusted.push({close:d,high:candles[i].high,low:candles[i].low,open:candles[i].open,volume:candles[i].volume,openTime:candles[i].openTime});
    }
    return emaSeries(adjusted, period);
}

/** Kaufman Adaptive Moving Average (KAMA) */
function kama(candles, period, fastPeriod, slowPeriod) {
    if (fastPeriod===undefined) fastPeriod=2;
    if (slowPeriod===undefined) slowPeriod=30;
    if (candles.length < period+1) return candles[candles.length-1].close;
    var fastSC=2.0/(fastPeriod+1), slowSC=2.0/(slowPeriod+1);
    var kamaVal=candles[period].close;
    for (var i=period+1;i<candles.length;i++){
        var direction=Math.abs(candles[i].close-candles[i-period].close);
        var volatility=0;
        for(var j=i-period+1;j<=i;j++) volatility+=Math.abs(candles[j].close-candles[j-1].close);
        var er=volatility>0?direction/volatility:0;
        var sc=er*(fastSC-slowSC)+slowSC;
        sc=sc*sc;
        kamaVal=kamaVal+sc*(candles[i].close-kamaVal);
    }
    return kamaVal;
}

function kamaSeries(candles, period, fastPeriod, slowPeriod) {
    if (fastPeriod===undefined) fastPeriod=2;
    if (slowPeriod===undefined) slowPeriod=30;
    var result=[];
    var fastSC=2.0/(fastPeriod+1), slowSC=2.0/(slowPeriod+1);
    var kamaVal=null;
    for (var i=0;i<candles.length;i++){
        if(i<period){result.push(null);continue;}
        if(i===period){kamaVal=candles[i].close;result.push(kamaVal);continue;}
        var direction=Math.abs(candles[i].close-candles[i-period].close);
        var vol=0;
        for(var j=i-period+1;j<=i;j++) vol+=Math.abs(candles[j].close-candles[j-1].close);
        var er=vol>0?direction/vol:0;
        var sc=er*(fastSC-slowSC)+slowSC; sc=sc*sc;
        kamaVal=kamaVal+sc*(candles[i].close-kamaVal);
        result.push(kamaVal);
    }
    return result;
}

/** Arnaud Legoux Moving Average (ALMA) */
function alma(candles, period, offset, sigma) {
    if (offset===undefined) offset=0.85;
    if (sigma===undefined) sigma=6;
    if (candles.length<period) return candles[candles.length-1].close;
    var m=Math.floor(offset*(period-1));
    var s=period/sigma;
    var wSum=0, norm=0;
    for(var i=0;i<period;i++){
        var w=Math.exp(-((i-m)*(i-m))/(2*s*s));
        wSum+=candles[candles.length-period+i].close*w;
        norm+=w;
    }
    return norm>0?wSum/norm:candles[candles.length-1].close;
}

function almaSeries(candles, period, offset, sigma) {
    if (offset===undefined) offset=0.85;
    if (sigma===undefined) sigma=6;
    var result=[];
    var m=Math.floor(offset*(period-1));
    var s=period/sigma;
    var weights=[];
    for(var i=0;i<period;i++) weights.push(Math.exp(-((i-m)*(i-m))/(2*s*s)));
    var norm=weights.reduce(function(a,b){return a+b;},0);
    for(var i=0;i<candles.length;i++){
        if(i<period-1){result.push(null);continue;}
        var sum=0;
        for(var j=0;j<period;j++) sum+=candles[i-period+1+j].close*weights[j];
        result.push(norm>0?sum/norm:candles[i].close);
    }
    return result;
}

/** T3 Moving Average (Tillson) */
function t3(candles, period, vfactor) {
    if (vfactor===undefined) vfactor=0.7;
    // T3 = c1*e6 + c2*e5 + c3*e4 + c4*e3
    var e1=ema(candles,period);
    // Simplified T3 using single EMA chain approximation
    var c1=-(vfactor*vfactor*vfactor);
    var c2=3*vfactor*vfactor+3*vfactor*vfactor*vfactor;
    var c3=-6*vfactor*vfactor-3*vfactor-3*vfactor*vfactor*vfactor;
    var c4=1+3*vfactor+vfactor*vfactor*vfactor+3*vfactor*vfactor;
    return c4*e1; // simplified
}

/** McGinley Dynamic */
function mcginley(candles, period) {
    if (candles.length<period) return candles[candles.length-1].close;
    var md=sma(candles.slice(0,period),period);
    for(var i=period;i<candles.length;i++){
        var c=candles[i].close;
        md=md+(c-md)/(period*Math.pow(c/md,4));
    }
    return md;
}

function mcginleySeries(candles, period) {
    var result=[];
    if(candles.length<period){for(var i=0;i<candles.length;i++)result.push(null);return result;}
    var sum=0;
    for(var i=0;i<period;i++){result.push(null);sum+=candles[i].close;}
    var md=sum/period;
    result[period-1]=md;
    for(var i=period;i<candles.length;i++){
        var c=candles[i].close;
        var ratio=c/md;
        if(ratio<=0) ratio=0.001;
        md=md+(c-md)/(period*Math.pow(ratio,4));
        result.push(md);
    }
    return result;
}

/** Fractal Adaptive Moving Average (FRAMA) */
function frama(candles, period) {
    if(candles.length<period) return candles[candles.length-1].close;
    var half=Math.floor(period/2);
    var s=candles.slice(-period);
    var h1=s.slice(0,half),h2=s.slice(half);
    function hl(arr){var hi=arr[0].high,lo=arr[0].low;for(var i=1;i<arr.length;i++){if(arr[i].high>hi)hi=arr[i].high;if(arr[i].low<lo)lo=arr[i].low;}return[hi,lo];}
    var r1=hl(h1),r2=hl(h2),r3=hl(s);
    var n1=(r1[0]-r1[1])/half,n2=(r2[0]-r2[1])/half,n3=(r3[0]-r3[1])/period;
    var d=(n1+n2>0&&n3>0)?Math.log(n3/((n1+n2)/2))/Math.log(2):1;
    var alpha=Math.exp(-4.6*(d-1));
    alpha=Math.max(0.01,Math.min(1,alpha));
    var framaVal=candles[candles.length-1].close*alpha+ema(candles,period)*(1-alpha);
    return framaVal;
}

// ─── Additional Oscillators & Momentum ───────────────────────────────────────

/** Rate of Change (ROC) */
function roc(candles, period) {
    if(candles.length<=period) return 0;
    var curr=candles[candles.length-1].close;
    var prev=candles[candles.length-1-period].close;
    return prev!==0?(curr-prev)/prev*100:0;
}

function rocSeries(candles, period) {
    var result=[];
    for(var i=0;i<candles.length;i++){
        if(i<period){result.push(null);continue;}
        var prev=candles[i-period].close;
        result.push(prev!==0?(candles[i].close-prev)/prev*100:0);
    }
    return result;
}

/** Momentum (MOM) — price change over N bars */
function momentum(candles, period) {
    if(candles.length<=period) return 0;
    return candles[candles.length-1].close-candles[candles.length-1-period].close;
}

function momentumSeries(candles, period) {
    var result=[];
    for(var i=0;i<candles.length;i++){
        if(i<period){result.push(null);continue;}
        result.push(candles[i].close-candles[i-period].close);
    }
    return result;
}

/** TRIX — 1-bar ROC of Triple-smoothed EMA */
function trix(candles, period) {
    var t=temaSeries(candles,period);
    var n=t.length;
    if(n<2||t[n-1]===null||t[n-2]===null) return 0;
    return t[n-2]!==0?(t[n-1]-t[n-2])/t[n-2]*10000:0;
}

function trixSeries(candles, period) {
    var t=temaSeries(candles,period);
    var result=[null];
    for(var i=1;i<t.length;i++){
        if(t[i]===null||t[i-1]===null){result.push(null);continue;}
        result.push(t[i-1]!==0?(t[i]-t[i-1])/t[i-1]*10000:0);
    }
    return result;
}

/** Awesome Oscillator (AO) — difference of 5-bar and 34-bar SMA of median price */
function awesomeOscillator(candles) {
    if(candles.length<34) return 0;
    function midSma(c,p){var s=0;for(var i=c.length-p;i<c.length;i++)s+=(c[i].high+c[i].low)/2;return s/p;}
    return midSma(candles,5)-midSma(candles,34);
}

function awesomeOscillatorSeries(candles) {
    var result=[];
    for(var i=0;i<candles.length;i++){
        if(i<33){result.push(null);continue;}
        var s5=0,s34=0;
        for(var j=i-4;j<=i;j++) s5+=(candles[j].high+candles[j].low)/2;
        for(var j=i-33;j<=i;j++) s34+=(candles[j].high+candles[j].low)/2;
        result.push(s5/5-s34/34);
    }
    return result;
}

/** Accelerator Oscillator — AO minus 5-bar SMA of AO */
function acceleratorOscillator(candles) {
    var ao=awesomeOscillatorSeries(candles);
    if(ao.length<5) return 0;
    var sum=0,cnt=0;
    for(var i=ao.length-5;i<ao.length;i++){if(ao[i]!==null){sum+=ao[i];cnt++;}}
    var smaAo=cnt>0?sum/cnt:0;
    return (ao[ao.length-1]||0)-smaAo;
}

function acceleratorOscillatorSeries(candles) {
    var ao=awesomeOscillatorSeries(candles);
    var result=[];
    for(var i=0;i<ao.length;i++){
        if(i<4||ao[i]===null){result.push(null);continue;}
        var sum=0;
        for(var j=i-4;j<=i;j++) sum+=(ao[j]||0);
        result.push(ao[i]-sum/5);
    }
    return result;
}

/** Ultimate Oscillator */
function ultimateOscillator(candles, p1, p2, p3) {
    if(p1===undefined) p1=7;
    if(p2===undefined) p2=14;
    if(p3===undefined) p3=28;
    var maxP=Math.max(p1,p2,p3);
    if(candles.length<maxP+1) return 50;
    function bpTr(c,i){
        var bp=c[i].close-Math.min(c[i].low,c[i-1].close);
        var tr=Math.max(c[i].high,c[i-1].close)-Math.min(c[i].low,c[i-1].close);
        return[bp,tr];
    }
    function avg(c,p){
        var bpSum=0,trSum=0;
        for(var i=c.length-p;i<c.length;i++){var r=bpTr(c,i);bpSum+=r[0];trSum+=r[1];}
        return trSum>0?bpSum/trSum:0;
    }
    var a1=avg(candles,p1),a2=avg(candles,p2),a3=avg(candles,p3);
    return 100*(4*a1+2*a2+a3)/7;
}

function ultimateOscillatorSeries(candles, p1, p2, p3) {
    if(p1===undefined) p1=7;if(p2===undefined) p2=14;if(p3===undefined) p3=28;
    var maxP=Math.max(p1,p2,p3);
    var result=[];
    for(var i=0;i<candles.length;i++){
        if(i<maxP){result.push(null);continue;}
        var slice=candles.slice(0,i+1);
        function bpTr(c,idx){var bp=c[idx].close-Math.min(c[idx].low,c[idx-1].close);var tr=Math.max(c[idx].high,c[idx-1].close)-Math.min(c[idx].low,c[idx-1].close);return[bp,tr];}
        function avg2(c,p){var bS=0,tS=0;for(var j=c.length-p;j<c.length;j++){var r=bpTr(c,j);bS+=r[0];tS+=r[1];}return tS>0?bS/tS:0;}
        result.push(100*(4*avg2(slice,p1)+2*avg2(slice,p2)+avg2(slice,p3))/7);
    }
    return result;
}

/** Chande Momentum Oscillator (CMO) */
function cmo(candles, period) {
    if(candles.length<period+1) return 0;
    var up=0,dn=0;
    for(var i=candles.length-period;i<candles.length;i++){
        var ch=candles[i].close-candles[i-1].close;
        if(ch>0)up+=ch; else dn-=ch;
    }
    return(up+dn)>0?100*(up-dn)/(up+dn):0;
}

function cmoSeries(candles, period) {
    var result=[];
    for(var i=0;i<candles.length;i++){
        if(i<period){result.push(null);continue;}
        var up=0,dn=0;
        for(var j=i-period+1;j<=i;j++){
            var ch=candles[j].close-candles[j-1].close;
            if(ch>0)up+=ch;else dn-=ch;
        }
        result.push((up+dn)>0?100*(up-dn)/(up+dn):0);
    }
    return result;
}

/** Know Sure Thing (KST) */
function kst(candles, r1,r2,r3,r4, s1,s2,s3,s4, sigPeriod) {
    if(r1===undefined){r1=10;r2=15;r3=20;r4=30;s1=10;s2=10;s3=10;s4=15;sigPeriod=9;}
    var rc1=rocSeries(candles,r1),rc2=rocSeries(candles,r2),rc3=rocSeries(candles,r3),rc4=rocSeries(candles,r4);
    // SMA smooth each ROC
    function smaArr(arr,p){var r=[];for(var i=0;i<arr.length;i++){if(i<p-1||arr[i]===null){r.push(null);continue;}var s=0;for(var j=i-p+1;j<=i;j++){if(arr[j]===null){s=null;break;}s+=arr[j];}r.push(s!==null?s/p:null);}return r;}
    var sm1=smaArr(rc1,s1),sm2=smaArr(rc2,s2),sm3=smaArr(rc3,s3),sm4=smaArr(rc4,s4);
    var kstLine=[];
    for(var i=0;i<candles.length;i++){
        if(sm1[i]===null||sm2[i]===null||sm3[i]===null||sm4[i]===null){kstLine.push(null);continue;}
        kstLine.push(sm1[i]+2*sm2[i]+3*sm3[i]+4*sm4[i]);
    }
    var sigLine=smaArr(kstLine,sigPeriod);
    var last=kstLine[kstLine.length-1];
    var lastSig=sigLine[sigLine.length-1];
    return {kst:last||0,signal:lastSig||0,histogram:(last||0)-(lastSig||0)};
}

function kstSeries(candles, r1,r2,r3,r4, s1,s2,s3,s4, sigPeriod) {
    if(r1===undefined){r1=10;r2=15;r3=20;r4=30;s1=10;s2=10;s3=10;s4=15;sigPeriod=9;}
    var rc1=rocSeries(candles,r1),rc2=rocSeries(candles,r2),rc3=rocSeries(candles,r3),rc4=rocSeries(candles,r4);
    function smaArr(arr,p){var r=[];for(var i=0;i<arr.length;i++){if(i<p-1||arr[i]===null){r.push(null);continue;}var s=0;for(var j=i-p+1;j<=i;j++){if(arr[j]===null){s=null;break;}s+=arr[j];}r.push(s!==null?s/p:null);}return r;}
    var sm1=smaArr(rc1,s1),sm2=smaArr(rc2,s2),sm3=smaArr(rc3,s3),sm4=smaArr(rc4,s4);
    var kstLine=[];
    for(var i=0;i<candles.length;i++){
        if(sm1[i]===null||sm2[i]===null||sm3[i]===null||sm4[i]===null){kstLine.push(null);continue;}
        kstLine.push(sm1[i]+2*sm2[i]+3*sm3[i]+4*sm4[i]);
    }
    var sigLine=smaArr(kstLine,sigPeriod);
    var histogram=[];
    for(var i=0;i<kstLine.length;i++){
        if(kstLine[i]===null||sigLine[i]===null){histogram.push(null);continue;}
        histogram.push(kstLine[i]-sigLine[i]);
    }
    return {kst:kstLine,signal:sigLine,histogram:histogram};
}

/** Vortex Indicator */
function vortex(candles, period) {
    if(candles.length<period+1) return {plus:1,minus:1};
    var vmP=0,vmM=0,trSum=0;
    for(var i=candles.length-period;i<candles.length;i++){
        vmP+=Math.abs(candles[i].high-candles[i-1].low);
        vmM+=Math.abs(candles[i].low-candles[i-1].high);
        trSum+=Math.max(candles[i].high-candles[i].low,Math.abs(candles[i].high-candles[i-1].close),Math.abs(candles[i].low-candles[i-1].close));
    }
    return {plus:trSum>0?vmP/trSum:1,minus:trSum>0?vmM/trSum:1};
}

function vortexSeries(candles, period) {
    var plus=[],minus=[];
    for(var i=0;i<candles.length;i++){
        if(i<period){plus.push(null);minus.push(null);continue;}
        var vmP=0,vmM=0,trSum=0;
        for(var j=i-period+1;j<=i;j++){
            vmP+=Math.abs(candles[j].high-candles[j-1].low);
            vmM+=Math.abs(candles[j].low-candles[j-1].high);
            trSum+=Math.max(candles[j].high-candles[j].low,Math.abs(candles[j].high-candles[j-1].close),Math.abs(candles[j].low-candles[j-1].close));
        }
        plus.push(trSum>0?vmP/trSum:1);
        minus.push(trSum>0?vmM/trSum:1);
    }
    return {plus:plus,minus:minus};
}

/** Aroon Indicator */
function aroon(candles, period) {
    if(candles.length<period+1) return {up:50,down:50,oscillator:0};
    var highIdx=0,lowIdx=0,s=candles.slice(-period-1);
    for(var i=0;i<s.length;i++){
        if(s[i].high>=s[highIdx].high) highIdx=i;
        if(s[i].low<=s[lowIdx].low) lowIdx=i;
    }
    var up=100*highIdx/period, down=100*lowIdx/period;
    return {up:up,down:down,oscillator:up-down};
}

function aroonSeries(candles, period) {
    var up=[],down=[],osc=[];
    for(var i=0;i<candles.length;i++){
        if(i<period){up.push(null);down.push(null);osc.push(null);continue;}
        var s=candles.slice(i-period,i+1);
        var hIdx=0,lIdx=0;
        for(var j=0;j<s.length;j++){
            if(s[j].high>=s[hIdx].high)hIdx=j;
            if(s[j].low<=s[lIdx].low)lIdx=j;
        }
        var u=100*hIdx/period,d=100*lIdx/period;
        up.push(u);down.push(d);osc.push(u-d);
    }
    return {up:up,down:down,oscillator:osc};
}

/** Parabolic SAR */
function parabolicSar(candles, step, maxStep) {
    if(step===undefined) step=0.02;
    if(maxStep===undefined) maxStep=0.2;
    if(candles.length<3) return candles[candles.length-1].close;
    var isUp=true,af=step,ep=candles[0].high,sar=candles[0].low;
    for(var i=1;i<candles.length;i++){
        var prevSar=sar;
        sar=prevSar+af*(ep-prevSar);
        if(isUp){
            if(candles[i].low<sar){isUp=false;sar=ep;ep=candles[i].low;af=step;}
            else{if(candles[i].high>ep){ep=candles[i].high;af=Math.min(af+step,maxStep);}
                sar=Math.min(sar,candles[i-1].low);if(i>1)sar=Math.min(sar,candles[i-2].low);}
        }else{
            if(candles[i].high>sar){isUp=true;sar=ep;ep=candles[i].high;af=step;}
            else{if(candles[i].low<ep){ep=candles[i].low;af=Math.min(af+step,maxStep);}
                sar=Math.max(sar,candles[i-1].high);if(i>1)sar=Math.max(sar,candles[i-2].high);}
        }
    }
    return sar;
}

function parabolicSarSeries(candles, step, maxStep) {
    if(step===undefined) step=0.02;
    if(maxStep===undefined) maxStep=0.2;
    var result=[];
    if(candles.length<3){for(var i=0;i<candles.length;i++)result.push(null);return {sar:result,direction:result};}
    var isUp=true,af=step,ep=candles[0].high,sar=candles[0].low;
    result.push(sar);
    var dirs=[1];
    for(var i=1;i<candles.length;i++){
        var prevSar=sar;
        sar=prevSar+af*(ep-prevSar);
        if(isUp){
            if(candles[i].low<sar){isUp=false;sar=ep;ep=candles[i].low;af=step;}
            else{if(candles[i].high>ep){ep=candles[i].high;af=Math.min(af+step,maxStep);}
                sar=Math.min(sar,candles[i-1].low);if(i>1)sar=Math.min(sar,candles[i-2].low);}
        }else{
            if(candles[i].high>sar){isUp=true;sar=ep;ep=candles[i].high;af=step;}
            else{if(candles[i].low<ep){ep=candles[i].low;af=Math.min(af+step,maxStep);}
                sar=Math.max(sar,candles[i-1].high);if(i>1)sar=Math.max(sar,candles[i-2].high);}
        }
        result.push(sar);
        dirs.push(isUp?1:-1);
    }
    return {sar:result,direction:dirs};
}

/** Keltner Channels */
function keltnerChannels(candles, period, mult, atrPeriod) {
    if(mult===undefined) mult=2.0;
    if(atrPeriod===undefined) atrPeriod=10;
    var mid=ema(candles,period);
    var a=atr(candles,atrPeriod);
    return {upper:mid+mult*a,middle:mid,lower:mid-mult*a};
}

function keltnerChannelsSeries(candles, period, mult, atrPeriod) {
    if(mult===undefined) mult=2.0;
    if(atrPeriod===undefined) atrPeriod=10;
    var midArr=emaSeries(candles,period);
    var atrArr=atrSeries(candles,atrPeriod);
    var upper=[],middle=[],lower=[];
    for(var i=0;i<candles.length;i++){
        if(midArr[i]===null||atrArr[i]===null){upper.push(null);middle.push(null);lower.push(null);continue;}
        upper.push(midArr[i]+mult*atrArr[i]);
        middle.push(midArr[i]);
        lower.push(midArr[i]-mult*atrArr[i]);
    }
    return {upper:upper,middle:middle,lower:lower};
}

/** Donchian Channels */
function donchianChannels(candles, period) {
    if(candles.length<period) return {upper:candles[candles.length-1].high,middle:candles[candles.length-1].close,lower:candles[candles.length-1].low};
    var s=candles.slice(-period);
    var hi=s[0].high,lo=s[0].low;
    for(var i=1;i<s.length;i++){if(s[i].high>hi)hi=s[i].high;if(s[i].low<lo)lo=s[i].low;}
    return {upper:hi,middle:(hi+lo)/2,lower:lo};
}

function donchianChannelsSeries(candles, period) {
    var upper=[],middle=[],lower=[];
    for(var i=0;i<candles.length;i++){
        if(i<period-1){upper.push(null);middle.push(null);lower.push(null);continue;}
        var hi=candles[i].high,lo=candles[i].low;
        for(var j=i-period+1;j<i;j++){if(candles[j].high>hi)hi=candles[j].high;if(candles[j].low<lo)lo=candles[j].low;}
        upper.push(hi);middle.push((hi+lo)/2);lower.push(lo);
    }
    return {upper:upper,middle:middle,lower:lower};
}

/** Mass Index */
function massIndex(candles, emaPeriod, sumPeriod) {
    if(emaPeriod===undefined) emaPeriod=9;
    if(sumPeriod===undefined) sumPeriod=25;
    if(candles.length<sumPeriod+emaPeriod*2) return 0;
    // EMA of (high - low)
    var hlDiff=candles.map(function(c){return c.high-c.low;});
    var mult=2.0/(emaPeriod+1);
    var ema1=[],sum=0;
    for(var i=0;i<hlDiff.length;i++){
        if(i<emaPeriod-1){ema1.push(null);sum+=hlDiff[i];continue;}
        if(i===emaPeriod-1){sum+=hlDiff[i];var v=sum/emaPeriod;ema1.push(v);continue;}
        var v=(hlDiff[i]-ema1[ema1.length-1])*mult+ema1[ema1.length-1];ema1.push(v);
    }
    // EMA of EMA
    var ema2=[],sum2=0,cnt=0;
    for(var i=0;i<ema1.length;i++){
        if(ema1[i]===null){ema2.push(null);continue;}
        if(cnt<emaPeriod-1){sum2+=ema1[i];cnt++;ema2.push(null);continue;}
        if(cnt===emaPeriod-1){sum2+=ema1[i];cnt++;ema2.push(sum2/emaPeriod);continue;}
        ema2.push((ema1[i]-ema2[ema2.length-1])*mult+ema2[ema2.length-1]);
    }
    // Ratio and sum
    var ratios=[];
    for(var i=0;i<ema1.length;i++){
        if(ema1[i]===null||ema2[i]===null||ema2[i]===0){ratios.push(null);continue;}
        ratios.push(ema1[i]/ema2[i]);
    }
    var total=0,cnt2=0;
    for(var i=ratios.length-1;i>=0&&cnt2<sumPeriod;i--){
        if(ratios[i]!==null){total+=ratios[i];cnt2++;}
    }
    return cnt2===sumPeriod?total:0;
}

/** Elder Ray (Bull Power, Bear Power) */
function elderRay(candles, period) {
    if(period===undefined) period=13;
    var e=ema(candles,period);
    var last=candles[candles.length-1];
    return {bullPower:last.high-e,bearPower:last.low-e};
}

function elderRaySeries(candles, period) {
    if(period===undefined) period=13;
    var e=emaSeries(candles,period);
    var bull=[],bear=[];
    for(var i=0;i<candles.length;i++){
        if(e[i]===null){bull.push(null);bear.push(null);continue;}
        bull.push(candles[i].high-e[i]);
        bear.push(candles[i].low-e[i]);
    }
    return {bullPower:bull,bearPower:bear};
}

/** Coppock Curve */
function coppockCurve(candles, longRoc, shortRoc, wmaPeriod) {
    if(longRoc===undefined) longRoc=14;
    if(shortRoc===undefined) shortRoc=11;
    if(wmaPeriod===undefined) wmaPeriod=10;
    var r1=rocSeries(candles,longRoc);
    var r2=rocSeries(candles,shortRoc);
    var combined=[];
    for(var i=0;i<candles.length;i++){
        if(r1[i]===null||r2[i]===null){combined.push(null);continue;}
        combined.push(r1[i]+r2[i]);
    }
    // WMA of combined
    var ws=wmaPeriod*(wmaPeriod+1)/2;
    var n=combined.length;
    if(n<wmaPeriod) return 0;
    var sum=0;
    for(var i=0;i<wmaPeriod;i++){
        var idx=n-wmaPeriod+i;
        if(combined[idx]===null) return 0;
        sum+=combined[idx]*(i+1);
    }
    return sum/ws;
}

function coppockCurveSeries(candles, longRoc, shortRoc, wmaPeriod) {
    if(longRoc===undefined) longRoc=14;if(shortRoc===undefined) shortRoc=11;if(wmaPeriod===undefined) wmaPeriod=10;
    var r1=rocSeries(candles,longRoc),r2=rocSeries(candles,shortRoc);
    var combined=[];
    for(var i=0;i<candles.length;i++){
        if(r1[i]===null||r2[i]===null){combined.push(null);continue;}
        combined.push(r1[i]+r2[i]);
    }
    var ws=wmaPeriod*(wmaPeriod+1)/2;
    var result=[];
    for(var i=0;i<combined.length;i++){
        if(i<wmaPeriod-1||combined[i]===null){result.push(null);continue;}
        var sum=0,ok=true;
        for(var j=0;j<wmaPeriod;j++){
            var idx=i-wmaPeriod+1+j;
            if(combined[idx]===null){ok=false;break;}
            sum+=combined[idx]*(j+1);
        }
        result.push(ok?sum/ws:null);
    }
    return result;
}

/** Detrended Price Oscillator (DPO) */
function dpo(candles, period) {
    if(candles.length<period) return 0;
    var shift=Math.floor(period/2)+1;
    var s=sma(candles,period);
    if(candles.length<=shift) return 0;
    return candles[candles.length-1-shift].close-s;
}

function dpoSeries(candles, period) {
    var shift=Math.floor(period/2)+1;
    var smaVals=smaSeries(candles,period);
    var result=[];
    for(var i=0;i<candles.length;i++){
        var refIdx=i-shift;
        if(refIdx<0||smaVals[i]===null){result.push(null);continue;}
        result.push(candles[refIdx].close-smaVals[i]);
    }
    return result;
}

/** Percentage Price Oscillator (PPO) */
function ppo(candles, fast, slow, signal) {
    if(fast===undefined) fast=12;if(slow===undefined) slow=26;if(signal===undefined) signal=9;
    var ef=ema(candles,fast),es=ema(candles,slow);
    var ppoVal=es!==0?(ef-es)/es*100:0;
    return ppoVal;
}

function ppoSeries(candles, fast, slow, signal) {
    if(fast===undefined) fast=12;if(slow===undefined) slow=26;if(signal===undefined) signal=9;
    var ef=emaSeries(candles,fast),es=emaSeries(candles,slow);
    var ppoLine=[],sigLine=[];
    var ppoVals=[];
    for(var i=0;i<candles.length;i++){
        if(ef[i]===null||es[i]===null||es[i]===0){ppoLine.push(null);continue;}
        var v=(ef[i]-es[i])/es[i]*100;
        ppoLine.push(v);ppoVals.push(v);
    }
    // Signal line
    var sigMult=2.0/(signal+1);
    var sigVal=null,cnt=0;
    for(var i=0;i<ppoLine.length;i++){
        if(ppoLine[i]===null){sigLine.push(null);continue;}
        cnt++;
        if(cnt<signal){sigLine.push(null);continue;}
        if(cnt===signal){
            var sum=0;for(var j=ppoVals.length-signal;j<ppoVals.length;j++)sum+=ppoVals[j];
            sigVal=sum/signal;sigLine.push(sigVal);
        }else{sigVal=(ppoLine[i]-sigVal)*sigMult+sigVal;sigLine.push(sigVal);}
    }
    var hist=[];
    for(var i=0;i<ppoLine.length;i++){
        if(ppoLine[i]===null||sigLine[i]===null){hist.push(null);continue;}
        hist.push(ppoLine[i]-sigLine[i]);
    }
    return {ppo:ppoLine,signal:sigLine,histogram:hist};
}

/** Z-Score */
function zScore(candles, period) {
    if(candles.length<period) return 0;
    var s=candles.slice(-period).map(function(c){return c.close;});
    var mean=s.reduce(function(a,b){return a+b;},0)/s.length;
    var variance=s.reduce(function(a,v){return a+(v-mean)*(v-mean);},0)/s.length;
    var sd=Math.sqrt(variance);
    return sd>0?(candles[candles.length-1].close-mean)/sd:0;
}

function zScoreSeries(candles, period) {
    var result=[];
    for(var i=0;i<candles.length;i++){
        if(i<period-1){result.push(null);continue;}
        var s=candles.slice(i-period+1,i+1).map(function(c){return c.close;});
        var mean=s.reduce(function(a,b){return a+b;},0)/s.length;
        var variance=s.reduce(function(a,v){return a+(v-mean)*(v-mean);},0)/s.length;
        var sd=Math.sqrt(variance);
        result.push(sd>0?(candles[i].close-mean)/sd:0);
    }
    return result;
}

/** Standard Deviation (of closes) */
function stdDev(candles, period) {
    if(typeof candles[0]==='number'){
        var arr=candles;
        var p=period||arr.length;
        var s=arr.slice(-p);
        var mean=s.reduce(function(a,b){return a+b;},0)/s.length;
        return Math.sqrt(s.reduce(function(a,v){return a+(v-mean)*(v-mean);},0)/s.length);
    }
    if(period===undefined) period=20;
    if(candles.length<period) return 0;
    var s=candles.slice(-period).map(function(c){return c.close;});
    var mean=s.reduce(function(a,b){return a+b;},0)/s.length;
    return Math.sqrt(s.reduce(function(a,v){return a+(v-mean)*(v-mean);},0)/s.length);
}

function stdDevSeries(candles, period) {
    if(period===undefined) period=20;
    var result=[];
    for(var i=0;i<candles.length;i++){
        if(i<period-1){result.push(null);continue;}
        var s=candles.slice(i-period+1,i+1).map(function(c){return c.close;});
        var mean=s.reduce(function(a,b){return a+b;},0)/s.length;
        result.push(Math.sqrt(s.reduce(function(a,v){return a+(v-mean)*(v-mean);},0)/s.length));
    }
    return result;
}

/** CMF as point function */
function cmf(candles, period) {
    if(period===undefined) period=20;
    if(candles.length<period) return 0;
    var mfVol=0,totVol=0;
    for(var i=candles.length-period;i<candles.length;i++){
        var c=candles[i];
        var clv=c.high!==c.low?((c.close-c.low)-(c.high-c.close))/(c.high-c.low):0;
        mfVol+=clv*c.volume;totVol+=c.volume;
    }
    return totVol>0?mfVol/totVol:0;
}

// ─── Volume Indicators ───────────────────────────────────────────────────────

/** Accumulation/Distribution Line */
function adLine(candles) {
    var val_=0;
    for(var i=0;i<candles.length;i++){
        var c=candles[i];
        var clv=c.high!==c.low?((c.close-c.low)-(c.high-c.close))/(c.high-c.low):0;
        val_+=clv*c.volume;
    }
    return val_;
}

function adLineSeries(candles) {
    var result=[],val_=0;
    for(var i=0;i<candles.length;i++){
        var c=candles[i];
        var clv=c.high!==c.low?((c.close-c.low)-(c.high-c.close))/(c.high-c.low):0;
        val_+=clv*c.volume;
        result.push(val_);
    }
    return result;
}

/** Chaikin Oscillator */
function chaikinOscillator(candles, fastPeriod, slowPeriod) {
    if(fastPeriod===undefined) fastPeriod=3;
    if(slowPeriod===undefined) slowPeriod=10;
    var adl=adLineSeries(candles);
    // EMA of AD line
    function emaOfArr(arr,p){
        var r=[],mult=2.0/(p+1),val_=null,cnt=0,sum=0;
        for(var i=0;i<arr.length;i++){
            if(val_===null){sum+=arr[i];cnt++;if(cnt===p){val_=sum/p;r.push(val_);}else r.push(null);}
            else{val_=(arr[i]-val_)*mult+val_;r.push(val_);}
        }
        return r;
    }
    var fast=emaOfArr(adl,fastPeriod),slow=emaOfArr(adl,slowPeriod);
    var n=candles.length-1;
    if(fast[n]===null||slow[n]===null) return 0;
    return fast[n]-slow[n];
}

function chaikinOscillatorSeries(candles, fastPeriod, slowPeriod) {
    if(fastPeriod===undefined) fastPeriod=3;if(slowPeriod===undefined) slowPeriod=10;
    var adl=adLineSeries(candles);
    function emaOfArr(arr,p){var r=[],mult=2.0/(p+1),val_=null,cnt=0,sum=0;for(var i=0;i<arr.length;i++){if(val_===null){sum+=arr[i];cnt++;if(cnt===p){val_=sum/p;r.push(val_);}else r.push(null);}else{val_=(arr[i]-val_)*mult+val_;r.push(val_);}}return r;}
    var fast=emaOfArr(adl,fastPeriod),slow=emaOfArr(adl,slowPeriod);
    var result=[];
    for(var i=0;i<candles.length;i++){
        if(fast[i]===null||slow[i]===null){result.push(null);continue;}
        result.push(fast[i]-slow[i]);
    }
    return result;
}

/** Ease of Movement (EOM) */
function eom(candles, period) {
    if(period===undefined) period=14;
    if(candles.length<period+1) return 0;
    var result=[];
    for(var i=1;i<candles.length;i++){
        var dm=(candles[i].high+candles[i].low)/2-(candles[i-1].high+candles[i-1].low)/2;
        var br=candles[i].volume/(candles[i].high-candles[i].low||1);
        result.push(dm/br*100000000);
    }
    if(result.length<period) return 0;
    var s=result.slice(-period);
    return s.reduce(function(a,b){return a+b;},0)/period;
}

function eomSeries(candles, period) {
    if(period===undefined) period=14;
    var raw=[null];
    for(var i=1;i<candles.length;i++){
        var dm=(candles[i].high+candles[i].low)/2-(candles[i-1].high+candles[i-1].low)/2;
        var br=candles[i].volume/(candles[i].high-candles[i].low||1);
        raw.push(dm/br*100000000);
    }
    var result=[];
    for(var i=0;i<raw.length;i++){
        if(i<period){result.push(null);continue;}
        var sum=0;
        for(var j=i-period+1;j<=i;j++) sum+=(raw[j]||0);
        result.push(sum/period);
    }
    return result;
}

/** Force Index */
function forceIndex(candles, period) {
    if(period===undefined) period=13;
    if(candles.length<period+1) return 0;
    var raw=[];
    for(var i=1;i<candles.length;i++) raw.push((candles[i].close-candles[i-1].close)*candles[i].volume);
    // EMA of Force Index
    var mult=2.0/(period+1);
    var sum=0;for(var i=0;i<period;i++)sum+=raw[i];
    var val_=sum/period;
    for(var i=period;i<raw.length;i++) val_=(raw[i]-val_)*mult+val_;
    return val_;
}

function forceIndexSeries(candles, period) {
    if(period===undefined) period=13;
    var raw=[null];
    for(var i=1;i<candles.length;i++) raw.push((candles[i].close-candles[i-1].close)*candles[i].volume);
    var mult=2.0/(period+1);var result=[null];
    var val_=null,cnt=0,sum=0;
    for(var i=1;i<raw.length;i++){
        if(raw[i]===null){result.push(null);continue;}
        if(val_===null){sum+=raw[i];cnt++;if(cnt===period){val_=sum/period;result.push(val_);}else result.push(null);}
        else{val_=(raw[i]-val_)*mult+val_;result.push(val_);}
    }
    return result;
}

/** Klinger Volume Oscillator */
function kvo(candles, fastPeriod, slowPeriod, signalPeriod) {
    if(fastPeriod===undefined) fastPeriod=34;if(slowPeriod===undefined) slowPeriod=55;if(signalPeriod===undefined) signalPeriod=13;
    if(candles.length<slowPeriod+1) return {kvo:0,signal:0};
    var vf=[];
    for(var i=1;i<candles.length;i++){
        var hlc=(candles[i].high+candles[i].low+candles[i].close)/3;
        var phlc=(candles[i-1].high+candles[i-1].low+candles[i-1].close)/3;
        var trend=hlc>phlc?1:-1;
        var dm=candles[i].high-candles[i].low;
        var cm=dm;// simplified
        vf.push(candles[i].volume*Math.abs(2*dm/(cm||1)-1)*trend*100);
    }
    function emaOfArr(arr,p){var mult=2.0/(p+1);if(arr.length<p) return 0;var val_=0;for(var i=0;i<p;i++)val_+=arr[i];val_/=p;for(var i=p;i<arr.length;i++)val_=(arr[i]-val_)*mult+val_;return val_;}
    var kvoVal=emaOfArr(vf,fastPeriod)-emaOfArr(vf,slowPeriod);
    return {kvo:kvoVal,signal:0};
}

/** Volume Profile (simplified) — returns the price level with highest volume */
function volumeProfile(candles, bins) {
    if(bins===undefined) bins=20;
    if(candles.length<2) return {poc:candles[0].close,valueAreaHigh:candles[0].high,valueAreaLow:candles[0].low};
    var hi=candles[0].high,lo=candles[0].low;
    for(var i=1;i<candles.length;i++){if(candles[i].high>hi)hi=candles[i].high;if(candles[i].low<lo)lo=candles[i].low;}
    var step=(hi-lo)/bins;
    if(step===0) return {poc:candles[0].close,valueAreaHigh:hi,valueAreaLow:lo};
    var buckets=[];for(var i=0;i<bins;i++)buckets.push(0);
    var totalVol=0;
    for(var i=0;i<candles.length;i++){
        var idx=Math.min(Math.floor((candles[i].close-lo)/step),bins-1);
        buckets[idx]+=candles[i].volume;
        totalVol+=candles[i].volume;
    }
    var maxIdx=0;for(var i=1;i<bins;i++){if(buckets[i]>buckets[maxIdx])maxIdx=i;}
    var poc=lo+(maxIdx+0.5)*step;
    // Value area (70% of volume)
    var vaVol=0,vaLow=maxIdx,vaHigh=maxIdx;
    vaVol=buckets[maxIdx];
    while(vaVol<totalVol*0.7){
        var below=vaLow>0?buckets[vaLow-1]:0;
        var above=vaHigh<bins-1?buckets[vaHigh+1]:0;
        if(above>=below&&vaHigh<bins-1){vaHigh++;vaVol+=above;}
        else if(vaLow>0){vaLow--;vaVol+=below;}
        else break;
    }
    return {poc:poc,valueAreaHigh:lo+(vaHigh+1)*step,valueAreaLow:lo+vaLow*step};
}

/** Net Volume */
function netVolume(candles) {
    var sum=0;
    for(var i=0;i<candles.length;i++){
        if(candles[i].close>=candles[i].open) sum+=candles[i].volume;
        else sum-=candles[i].volume;
    }
    return sum;
}

function netVolumeSeries(candles) {
    var result=[];
    for(var i=0;i<candles.length;i++){
        result.push(candles[i].close>=candles[i].open?candles[i].volume:-candles[i].volume);
    }
    return result;
}

/** Volume Weighted RSI */
function vwRsi(candles, period) {
    if(candles.length<period+1) return 50;
    var gainVol=0,lossVol=0;
    for(var i=candles.length-period;i<candles.length;i++){
        var ch=candles[i].close-candles[i-1].close;
        if(ch>0) gainVol+=ch*candles[i].volume;
        else lossVol-=ch*candles[i].volume;
    }
    if(lossVol===0) return 100;
    var ratio=gainVol/lossVol;
    return 100-(100/(1+ratio));
}

// ─── Volatility Indicators ───────────────────────────────────────────────────

/** Historical Volatility (annualized) */
function historicalVolatility(candles, period) {
    if(period===undefined) period=20;
    if(candles.length<period+1) return 0;
    var returns=[];
    for(var i=candles.length-period;i<candles.length;i++){
        var r=Math.log(candles[i].close/candles[i-1].close);
        returns.push(r);
    }
    var mean=returns.reduce(function(a,b){return a+b;},0)/returns.length;
    var variance=returns.reduce(function(a,v){return a+(v-mean)*(v-mean);},0)/(returns.length-1);
    return Math.sqrt(variance)*Math.sqrt(365)*100;
}

function historicalVolatilitySeries(candles, period) {
    if(period===undefined) period=20;
    var result=[];
    for(var i=0;i<candles.length;i++){
        if(i<period){result.push(null);continue;}
        var returns=[];
        for(var j=i-period+1;j<=i;j++) returns.push(Math.log(candles[j].close/candles[j-1].close));
        var mean=returns.reduce(function(a,b){return a+b;},0)/returns.length;
        var variance=returns.reduce(function(a,v){return a+(v-mean)*(v-mean);},0)/(returns.length-1);
        result.push(Math.sqrt(variance)*Math.sqrt(365)*100);
    }
    return result;
}

/** Chaikin Volatility */
function chaikinVolatility(candles, emaPeriod, rocPeriod) {
    if(emaPeriod===undefined) emaPeriod=10;
    if(rocPeriod===undefined) rocPeriod=10;
    var hlDiff=candles.map(function(c){return c.high-c.low;});
    // EMA of HL diff
    var mult=2.0/(emaPeriod+1);
    var emaHL=[],val_=null,cnt=0,sum=0;
    for(var i=0;i<hlDiff.length;i++){
        if(val_===null){sum+=hlDiff[i];cnt++;if(cnt===emaPeriod){val_=sum/cnt;emaHL.push(val_);}else emaHL.push(null);}
        else{val_=(hlDiff[i]-val_)*mult+val_;emaHL.push(val_);}
    }
    var n=emaHL.length-1;
    if(n<rocPeriod||emaHL[n]===null||emaHL[n-rocPeriod]===null) return 0;
    var prev=emaHL[n-rocPeriod];
    return prev!==0?(emaHL[n]-prev)/prev*100:0;
}

/** Ulcer Index */
function ulcerIndex(candles, period) {
    if(period===undefined) period=14;
    if(candles.length<period) return 0;
    var maxClose=0,sumSq=0;
    for(var i=candles.length-period;i<candles.length;i++){
        if(candles[i].close>maxClose) maxClose=candles[i].close;
        var pdd=maxClose>0?100*(candles[i].close-maxClose)/maxClose:0;
        sumSq+=pdd*pdd;
    }
    return Math.sqrt(sumSq/period);
}

/** Natr (Normalized ATR) */
function natr(candles, period) {
    if(period===undefined) period=14;
    var a=atr(candles,period);
    var close=candles[candles.length-1].close;
    return close>0?a/close*100:0;
}

function natrSeries(candles, period) {
    if(period===undefined) period=14;
    var a=atrSeries(candles,period);
    var result=[];
    for(var i=0;i<candles.length;i++){
        if(a[i]===null){result.push(null);continue;}
        result.push(candles[i].close>0?a[i]/candles[i].close*100:0);
    }
    return result;
}

/** True Range */
function trueRange(candles) {
    var n=candles.length;
    if(n<2) return candles[n-1].high-candles[n-1].low;
    var h=candles[n-1].high,l=candles[n-1].low,pc=candles[n-2].close;
    return Math.max(h-l,Math.abs(h-pc),Math.abs(l-pc));
}

function trueRangeSeries(candles) {
    var result=[candles.length>0?candles[0].high-candles[0].low:0];
    for(var i=1;i<candles.length;i++){
        result.push(Math.max(candles[i].high-candles[i].low,Math.abs(candles[i].high-candles[i-1].close),Math.abs(candles[i].low-candles[i-1].close)));
    }
    return result;
}

// ─── Trend / Directional Indicators ──────────────────────────────────────────

/** Plus DI / Minus DI */
function diPlus(candles, period) {
    if(period===undefined) period=14;
    if(candles.length<period*2) return 25;
    var pDM=[],trVals=[];
    for(var i=1;i<candles.length;i++){
        var up=candles[i].high-candles[i-1].high;
        var dn=candles[i-1].low-candles[i].low;
        pDM.push(up>dn&&up>0?up:0);
        trVals.push(Math.max(candles[i].high-candles[i].low,Math.abs(candles[i].high-candles[i-1].close),Math.abs(candles[i].low-candles[i-1].close)));
    }
    var sTr=0,sPdm=0;
    for(var i=0;i<period;i++){sTr+=trVals[i];sPdm+=pDM[i];}
    for(var i=period;i<trVals.length;i++){sTr=sTr-sTr/period+trVals[i];sPdm=sPdm-sPdm/period+pDM[i];}
    return sTr>0?100*sPdm/sTr:0;
}

function diMinus(candles, period) {
    if(period===undefined) period=14;
    if(candles.length<period*2) return 25;
    var nDM=[],trVals=[];
    for(var i=1;i<candles.length;i++){
        var up=candles[i].high-candles[i-1].high;
        var dn=candles[i-1].low-candles[i].low;
        nDM.push(dn>up&&dn>0?dn:0);
        trVals.push(Math.max(candles[i].high-candles[i].low,Math.abs(candles[i].high-candles[i-1].close),Math.abs(candles[i].low-candles[i-1].close)));
    }
    var sTr=0,sNdm=0;
    for(var i=0;i<period;i++){sTr+=trVals[i];sNdm+=nDM[i];}
    for(var i=period;i<trVals.length;i++){sTr=sTr-sTr/period+trVals[i];sNdm=sNdm-sNdm/period+nDM[i];}
    return sTr>0?100*sNdm/sTr:0;
}

function diSeries(candles, period) {
    if(period===undefined) period=14;
    var pDM=[],nDM=[],trVals=[];
    for(var i=1;i<candles.length;i++){
        var up=candles[i].high-candles[i-1].high;
        var dn=candles[i-1].low-candles[i].low;
        pDM.push(up>dn&&up>0?up:0);
        nDM.push(dn>up&&dn>0?dn:0);
        trVals.push(Math.max(candles[i].high-candles[i].low,Math.abs(candles[i].high-candles[i-1].close),Math.abs(candles[i].low-candles[i-1].close)));
    }
    var plus=[null],minus=[null];
    var sTr=0,sPdm=0,sNdm=0;
    for(var i=0;i<trVals.length;i++){
        if(i<period){sTr+=trVals[i];sPdm+=pDM[i];sNdm+=nDM[i];if(i<period-1){plus.push(null);minus.push(null);continue;}
            plus.push(sTr>0?100*sPdm/sTr:0);minus.push(sTr>0?100*sNdm/sTr:0);continue;}
        sTr=sTr-sTr/period+trVals[i];sPdm=sPdm-sPdm/period+pDM[i];sNdm=sNdm-sNdm/period+nDM[i];
        plus.push(sTr>0?100*sPdm/sTr:0);minus.push(sTr>0?100*sNdm/sTr:0);
    }
    return {plus:plus,minus:minus};
}

/** Price Channel */
function priceChannel(candles, period) {
    if(candles.length<period) return {upper:candles[candles.length-1].high,lower:candles[candles.length-1].low};
    // Excludes current bar
    var s=candles.slice(-period-1,-1);
    var hi=s[0].high,lo=s[0].low;
    for(var i=1;i<s.length;i++){if(s[i].high>hi)hi=s[i].high;if(s[i].low<lo)lo=s[i].low;}
    return {upper:hi,lower:lo,middle:(hi+lo)/2};
}

/** TRIX Signal Line  */
function trixSignal(candles, period, signalPeriod) {
    if(signalPeriod===undefined) signalPeriod=9;
    var t=trixSeries(candles,period);
    function smaArr(arr,p){var r=[];for(var i=0;i<arr.length;i++){if(i<p-1||arr[i]===null){r.push(null);continue;}var s=0;for(var j=i-p+1;j<=i;j++){if(arr[j]===null){s=null;break;}s+=arr[j];}r.push(s!==null?s/p:null);}return r;}
    var sig=smaArr(t,signalPeriod);
    return {trix:t[t.length-1]||0,signal:sig[sig.length-1]||0};
}

// ─── Candle Pattern Detection ────────────────────────────────────────────────

/** Detect common candlestick patterns for the last candle */
function candlePattern(candles) {
    if(candles.length<3) return {pattern:'none',type:'neutral'};
    var c=candles[candles.length-1],p=candles[candles.length-2],pp=candles[candles.length-3];
    var body=Math.abs(c.close-c.open),range=c.high-c.low;
    var pBody=Math.abs(p.close-p.open),pRange=p.high-p.low;
    var bullish=c.close>c.open,pBullish=p.close>p.open;
    var upperWick=bullish?c.high-c.close:c.high-c.open;
    var lowerWick=bullish?c.open-c.low:c.close-c.low;

    // Doji
    if(range>0&&body/range<0.1) return {pattern:'doji',type:'neutral'};
    // Hammer
    if(lowerWick>body*2&&upperWick<body*0.5) return {pattern:bullish?'hammer':'hangingMan',type:bullish?'bullish':'bearish'};
    // Inverted Hammer / Shooting Star
    if(upperWick>body*2&&lowerWick<body*0.5) return {pattern:bullish?'invertedHammer':'shootingStar',type:bullish?'bullish':'bearish'};
    // Bullish Engulfing
    if(!pBullish&&bullish&&c.open<p.close&&c.close>p.open) return {pattern:'bullishEngulfing',type:'bullish'};
    // Bearish Engulfing
    if(pBullish&&!bullish&&c.open>p.close&&c.close<p.open) return {pattern:'bearishEngulfing',type:'bearish'};
    // Morning Star
    if(pp.close<pp.open&&pBody<pRange*0.3&&bullish&&c.close>pp.open) return {pattern:'morningStar',type:'bullish'};
    // Evening Star
    if(pp.close>pp.open&&pBody<pRange*0.3&&!bullish&&c.close<pp.open) return {pattern:'eveningStar',type:'bearish'};
    // Three White Soldiers
    if(bullish&&pBullish&&pp.close>pp.open&&c.close>p.close&&p.close>pp.close) return {pattern:'threeWhiteSoldiers',type:'bullish'};
    // Three Black Crows
    if(!bullish&&!pBullish&&pp.close<pp.open&&c.close<p.close&&p.close<pp.close) return {pattern:'threeBlackCrows',type:'bearish'};
    // Marubozu (strong body, tiny wicks)
    if(range>0&&body/range>0.9) return {pattern:'marubozu',type:bullish?'bullish':'bearish'};
    // Spinning top
    if(range>0&&body/range<0.3&&upperWick>body&&lowerWick>body) return {pattern:'spinningTop',type:'neutral'};
    // Tweezer tops/bottoms
    if(Math.abs(c.high-p.high)<range*0.05&&!bullish&&pBullish) return {pattern:'tweezerTop',type:'bearish'};
    if(Math.abs(c.low-p.low)<range*0.05&&bullish&&!pBullish) return {pattern:'tweezerBottom',type:'bullish'};
    // Piercing line
    if(!pBullish&&bullish&&c.open<p.low&&c.close>(p.open+p.close)/2) return {pattern:'piercingLine',type:'bullish'};
    // Dark cloud cover
    if(pBullish&&!bullish&&c.open>p.high&&c.close<(p.open+p.close)/2) return {pattern:'darkCloudCover',type:'bearish'};

    return {pattern:'none',type:'neutral'};
}

/** candlePatternSeries — detect pattern at each bar */
function candlePatternSeries(candles) {
    var result=[];
    for(var i=0;i<candles.length;i++){
        if(i<2){result.push({pattern:'none',type:'neutral'});continue;}
        result.push(candlePattern(candles.slice(0,i+1)));
    }
    return result;
}

// ─── Support / Resistance ────────────────────────────────────────────────────

/** Find support and resistance levels from pivot points */
function supportResistance(candles, leftBars, rightBars, maxLevels) {
    if(leftBars===undefined) leftBars=10;
    if(rightBars===undefined) rightBars=5;
    if(maxLevels===undefined) maxLevels=5;
    var pivH=pivotHigh(candles,leftBars,rightBars);
    var pivL=pivotLow(candles,leftBars,rightBars);
    var res=[],sup=[];
    for(var i=pivH.length-1;i>=0&&res.length<maxLevels;i--) if(pivH[i]!==null) res.push(pivH[i]);
    for(var i=pivL.length-1;i>=0&&sup.length<maxLevels;i--) if(pivL[i]!==null) sup.push(pivL[i]);
    return {resistance:res,support:sup};
}

// ─── Multi-timeframe helpers ─────────────────────────────────────────────────

/** Resample candles to a higher timeframe (merge N candles into 1) */
function resample(candles, factor) {
    if(factor===undefined) factor=4;
    var result=[];
    for(var i=0;i<candles.length;i+=factor){
        var end=Math.min(i+factor,candles.length);
        var chunk=candles.slice(i,end);
        var hi=chunk[0].high,lo=chunk[0].low,vol=0;
        for(var j=0;j<chunk.length;j++){if(chunk[j].high>hi)hi=chunk[j].high;if(chunk[j].low<lo)lo=chunk[j].low;vol+=chunk[j].volume;}
        result.push({openTime:chunk[0].openTime,open:chunk[0].open,high:hi,low:lo,close:chunk[chunk.length-1].close,volume:vol});
    }
    return result;
}

// ─── Math / Statistical helpers ──────────────────────────────────────────────

/** correlation(arr1, arr2) — Pearson correlation coefficient */
function correlation(arr1, arr2) {
    var n=Math.min(arr1.length,arr2.length);
    if(n<2) return 0;
    var sum1=0,sum2=0,sum12=0,sq1=0,sq2=0;
    for(var i=0;i<n;i++){
        var a=arr1[i]!==null?arr1[i]:0;
        var b=arr2[i]!==null?arr2[i]:0;
        sum1+=a;sum2+=b;sum12+=a*b;sq1+=a*a;sq2+=b*b;
    }
    var denom=Math.sqrt((n*sq1-sum1*sum1)*(n*sq2-sum2*sum2));
    return denom>0?(n*sum12-sum1*sum2)/denom:0;
}

/** covariance(arr1, arr2) */
function covariance(arr1, arr2) {
    var n=Math.min(arr1.length,arr2.length);
    if(n<2) return 0;
    var m1=0,m2=0;
    for(var i=0;i<n;i++){m1+=(arr1[i]||0);m2+=(arr2[i]||0);}
    m1/=n;m2/=n;
    var cov=0;
    for(var i=0;i<n;i++) cov+=(arr1[i]||0-m1)*((arr2[i]||0)-m2);
    return cov/(n-1);
}

/** beta(asset candles, benchmark candles, period) — CAPM beta */
function beta(candles, benchCandles, period) {
    if(period===undefined) period=20;
    var n=Math.min(candles.length,benchCandles.length);
    if(n<period+1) return 1;
    var r1=[],r2=[];
    for(var i=n-period;i<n;i++){
        r1.push(candles[i].close/candles[i-1].close-1);
        r2.push(benchCandles[i].close/benchCandles[i-1].close-1);
    }
    var c=covariance(r1,r2);
    var v=0,m=r2.reduce(function(a,b){return a+b;},0)/r2.length;
    for(var i=0;i<r2.length;i++) v+=(r2[i]-m)*(r2[i]-m);
    v/=(r2.length-1);
    return v>0?c/v:1;
}

/** percentile(arr, pct) — e.g. percentile(closes, 75) */
function percentile(arr, pct) {
    var sorted=arr.filter(function(v){return v!==null;}).slice().sort(function(a,b){return a-b;});
    if(sorted.length===0) return 0;
    var idx=(pct/100)*(sorted.length-1);
    var lower=Math.floor(idx),upper=Math.ceil(idx);
    if(lower===upper) return sorted[lower];
    return sorted[lower]+(idx-lower)*(sorted[upper]-sorted[lower]);
}

/** median(arr) */
function median(arr) {
    return percentile(arr, 50);
}

/** sum(arr) — sum of array values */
function sum(arr) {
    var total=0;
    for(var i=0;i<arr.length;i++) if(arr[i]!==null) total+=arr[i];
    return total;
}

/** avg(arr) — average of array values */
function avg(arr) {
    var total=0,cnt=0;
    for(var i=0;i<arr.length;i++) if(arr[i]!==null){total+=arr[i];cnt++;}
    return cnt>0?total/cnt:0;
}

/** abs(val) */
function abs(val) { return Math.abs(val); }

/** max(a, b) / min(a, b) */
function max(a, b) { return Math.max(a, b); }
function min(a, b) { return Math.min(a, b); }

/** clamp(val, lo, hi) */
function clamp(val, lo, hi) { return Math.max(lo, Math.min(hi, val)); }

/** nz(val, replacement) — replace null/NaN/undefined with replacement */
function nz(val, replacement) {
    if(replacement===undefined) replacement=0;
    return (val===null||val===undefined||val!==val)?replacement:val;
}

/** na(val) — returns true if val is null/undefined/NaN */
function na(val) {
    return val===null||val===undefined||val!==val;
}

/** valuewhen(conditionArr, sourceArr, occurrence) — value of source when condition was last true */
function valuewhen(conditionArr, sourceArr, occurrence) {
    if(occurrence===undefined) occurrence=0;
    var found=0;
    for(var i=conditionArr.length-1;i>=0;i--){
        if(conditionArr[i]){
            if(found===occurrence) return sourceArr[i]||null;
            found++;
        }
    }
    return null;
}

/** barssince(conditionArr) — number of bars since condition was last true */
function barssince(conditionArr) {
    for(var i=conditionArr.length-1;i>=0;i--){if(conditionArr[i]) return conditionArr.length-1-i;}
    return conditionArr.length;
}

/** rising(arr, period, i) — true if arr has been rising for period bars */
function rising(arr, period, i) {
    if(i===undefined) i=arr.length-1;
    if(i<period) return false;
    for(var j=i-period+1;j<=i;j++){
        if(arr[j]===null||arr[j-1]===null||arr[j]<=arr[j-1]) return false;
    }
    return true;
}

/** falling(arr, period, i) — true if arr has been falling for period bars */
function falling(arr, period, i) {
    if(i===undefined) i=arr.length-1;
    if(i<period) return false;
    for(var j=i-period+1;j<=i;j++){
        if(arr[j]===null||arr[j-1]===null||arr[j]>=arr[j-1]) return false;
    }
    return true;
}

/** cum(arr) — cumulative sum series */
function cum(arr) {
    var result=[];
    var total=0;
    for(var i=0;i<arr.length;i++){
        total+=(arr[i]||0);
        result.push(total);
    }
    return result;
}

/** dayofweek(candle) — 0=Sunday..6=Saturday */
function dayofweek(candle) {
    return new Date(candle.openTime).getDay();
}

/** hour(candle) — hour of day (0-23) */
function hour(candle) {
    return new Date(candle.openTime).getHours();
}

// ═══════════════════════════════════════════════════════════════════════════════
// SWMA — Symmetrically Weighted Moving Average (PineScript ta.swma)
// Weights: [1, 2, 2, 1] over 4 bars
// ═══════════════════════════════════════════════════════════════════════════════

function swma(candles, period) {
    if (period === undefined) period = 4;
    if (candles.length < period) return candles[candles.length - 1].close;
    // Symmetric weights: triangle window
    var half = Math.floor(period / 2);
    var weights = [];
    for (var i = 0; i < period; i++) {
        weights.push(i < half ? i + 1 : period - i);
    }
    var wSum = weights.reduce(function(a, b) { return a + b; }, 0);
    var sum = 0;
    for (var i = 0; i < period; i++) {
        sum += candles[candles.length - period + i].close * weights[i];
    }
    return sum / wSum;
}

function swmaSeries(candles, period) {
    if (period === undefined) period = 4;
    var half = Math.floor(period / 2);
    var weights = [];
    for (var i = 0; i < period; i++) weights.push(i < half ? i + 1 : period - i);
    var wSum = weights.reduce(function(a, b) { return a + b; }, 0);
    var result = [];
    for (var i = 0; i < candles.length; i++) {
        if (i < period - 1) { result.push(null); continue; }
        var sum = 0;
        for (var j = 0; j < period; j++) sum += candles[i - period + 1 + j].close * weights[j];
        result.push(sum / wSum);
    }
    return result;
}

// ═══════════════════════════════════════════════════════════════════════════════
// Bollinger %B and Bandwidth (PineScript ta.bbw / manual %B)
// ═══════════════════════════════════════════════════════════════════════════════

/** bollingerPercentB — (close - lower) / (upper - lower). 0 = at lower band, 1 = at upper */
function bollingerPercentB(candles, period, numStd) {
    if (period === undefined) period = 20;
    if (numStd === undefined) numStd = 2;
    var bb = bollinger(candles, period, numStd);
    var width = bb.upper - bb.lower;
    if (width === 0) return 0.5;
    return (candles[candles.length - 1].close - bb.lower) / width;
}

function bollingerPercentBSeries(candles, period, numStd) {
    if (period === undefined) period = 20;
    if (numStd === undefined) numStd = 2;
    var bb = bollingerSeries(candles, period, numStd);
    var result = [];
    for (var i = 0; i < candles.length; i++) {
        if (bb.upper[i] === null || bb.lower[i] === null) { result.push(null); continue; }
        var w = bb.upper[i] - bb.lower[i];
        result.push(w > 0 ? (candles[i].close - bb.lower[i]) / w : 0.5);
    }
    return result;
}

/** bollingerWidth — (upper - lower) / middle. Measures band expansion/contraction */
function bollingerWidth(candles, period, numStd) {
    if (period === undefined) period = 20;
    if (numStd === undefined) numStd = 2;
    var bb = bollinger(candles, period, numStd);
    if (bb.middle === 0) return 0;
    return (bb.upper - bb.lower) / bb.middle;
}

function bollingerWidthSeries(candles, period, numStd) {
    if (period === undefined) period = 20;
    if (numStd === undefined) numStd = 2;
    var bb = bollingerSeries(candles, period, numStd);
    var result = [];
    for (var i = 0; i < candles.length; i++) {
        if (bb.upper[i] === null || bb.middle[i] === null || bb.middle[i] === 0) { result.push(null); continue; }
        result.push((bb.upper[i] - bb.lower[i]) / bb.middle[i]);
    }
    return result;
}

// ═══════════════════════════════════════════════════════════════════════════════
// Percent Rank (PineScript ta.percentrank)
// What % of the last `period` values is the current value greater than
// ═══════════════════════════════════════════════════════════════════════════════

function percentRank(candles, period) {
    if (period === undefined) period = 20;
    if (candles.length < period + 1) return 50;
    var currentClose = candles[candles.length - 1].close;
    var count = 0;
    for (var i = candles.length - period - 1; i < candles.length - 1; i++) {
        if (candles[i].close < currentClose) count++;
    }
    return count / period * 100;
}

function percentRankSeries(candles, period) {
    if (period === undefined) period = 20;
    var result = [];
    for (var i = 0; i < candles.length; i++) {
        if (i < period) { result.push(null); continue; }
        var count = 0;
        for (var j = i - period; j < i; j++) {
            if (candles[j].close < candles[i].close) count++;
        }
        result.push(count / period * 100);
    }
    return result;
}

/** Generic percentRank for any numeric array (like ta.percentrank on a source) */
function percentRankArr(arr, period, i) {
    if (i === undefined) i = arr.length - 1;
    if (period === undefined) period = 20;
    if (i < period || arr[i] === null) return null;
    var count = 0;
    for (var j = i - period; j < i; j++) {
        if (arr[j] !== null && arr[j] < arr[i]) count++;
    }
    return count / period * 100;
}

// ═══════════════════════════════════════════════════════════════════════════════
// Session-aware VWAP (resets each UTC day, like PineScript ta.vwap)
// ═══════════════════════════════════════════════════════════════════════════════

/** sessionVwap — VWAP that resets at each new UTC day boundary */
function sessionVwap(candles) {
    if (candles.length === 0) return 0;
    var cumVol = 0, cumTpVol = 0;
    var lastDay = -1;
    for (var i = 0; i < candles.length; i++) {
        var d = new Date(candles[i].openTime);
        var day = d.getUTCFullYear() * 10000 + (d.getUTCMonth() + 1) * 100 + d.getUTCDate();
        if (day !== lastDay) {
            cumVol = 0; cumTpVol = 0; lastDay = day;
        }
        var tp = (candles[i].high + candles[i].low + candles[i].close) / 3;
        cumTpVol += tp * candles[i].volume;
        cumVol += candles[i].volume;
    }
    return cumVol > 0 ? cumTpVol / cumVol : candles[candles.length - 1].close;
}

function sessionVwapSeries(candles) {
    var result = [];
    var cumVol = 0, cumTpVol = 0;
    var lastDay = -1;
    for (var i = 0; i < candles.length; i++) {
        var d = new Date(candles[i].openTime);
        var day = d.getUTCFullYear() * 10000 + (d.getUTCMonth() + 1) * 100 + d.getUTCDate();
        if (day !== lastDay) {
            cumVol = 0; cumTpVol = 0; lastDay = day;
        }
        var tp = (candles[i].high + candles[i].low + candles[i].close) / 3;
        cumTpVol += tp * candles[i].volume;
        cumVol += candles[i].volume;
        result.push(cumVol > 0 ? cumTpVol / cumVol : candles[i].close);
    }
    return result;
}

/** sessionVwapBands — VWAP ± N standard deviations (like PineScript VWAP with bands) */
function sessionVwapBands(candles, numStd) {
    if (numStd === undefined) numStd = 2;
    var vwapLine = [];
    var cumVol = 0, cumTpVol = 0, cumTp2Vol = 0;
    var lastDay = -1;
    for (var i = 0; i < candles.length; i++) {
        var d = new Date(candles[i].openTime);
        var day = d.getUTCFullYear() * 10000 + (d.getUTCMonth() + 1) * 100 + d.getUTCDate();
        if (day !== lastDay) {
            cumVol = 0; cumTpVol = 0; cumTp2Vol = 0; lastDay = day;
        }
        var tp = (candles[i].high + candles[i].low + candles[i].close) / 3;
        cumTpVol += tp * candles[i].volume;
        cumTp2Vol += tp * tp * candles[i].volume;
        cumVol += candles[i].volume;
        var v = cumVol > 0 ? cumTpVol / cumVol : candles[i].close;
        var variance = cumVol > 0 ? cumTp2Vol / cumVol - v * v : 0;
        if (variance < 0) variance = 0;
        var sd = Math.sqrt(variance);
        vwapLine.push({ vwap: v, upper: v + numStd * sd, lower: v - numStd * sd });
    }
    var vwapArr = [], upperArr = [], lowerArr = [];
    for (var i = 0; i < vwapLine.length; i++) {
        vwapArr.push(vwapLine[i].vwap);
        upperArr.push(vwapLine[i].upper);
        lowerArr.push(vwapLine[i].lower);
    }
    return { vwap: vwapArr, upper: upperArr, lower: lowerArr };
}

// ═══════════════════════════════════════════════════════════════════════════════
// Multi-Timeframe — request.security() equivalent
// Aggregates the current candles into a higher timeframe, then applies a
// function, and maps results back to the original timeframe bars.
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * security(candles, factor, fn)
 *
 * PineScript-like multi-timeframe access.
 *
 * @param candles  — the base-timeframe candle array
 * @param factor   — how many base bars form one higher-TF bar (e.g. 4 for 1h→4h)
 *                   OR a string like "4h","1d","1w" (auto-calculates factor from candle interval)
 * @param fn       — a function(htfCandles) that returns a value or array:
 *                   • If it returns a single value, that value fills all bars of the last HTF candle.
 *                   • If it returns an array (series), values are mapped back to base bars.
 *
 * Returns an array with one value per base candle (null where no HTF data yet).
 *
 * Usage examples:
 *   // Get 4h RSI while on 1h chart
 *   var htfRsi = security(_candles, 4, function(c) { return rsiSeries(c, 14); });
 *
 *   // Get daily close
 *   var dailyClose = security(_candles, "1d", function(c) {
 *       return c.map(function(bar) { return bar.close; });
 *   });
 */
function security(candles, factor, fn) {
    if (candles.length === 0) return [];

    // ─── Resolve string timeframes to numeric factor ────────────────
    if (typeof factor === "string") {
        // Auto-detect base interval from candle spacing
        var baseMs = candles.length > 1 ? candles[1].openTime - candles[0].openTime : 60000;
        var targetMs = _parseInterval(factor);
        factor = Math.max(1, Math.round(targetMs / baseMs));
    }
    factor = Math.max(1, Math.round(factor));
    if (factor === 1) {
        // Same timeframe — just run fn directly
        var directResult = fn(candles);
        if (typeof directResult === "number") {
            var arr = []; for (var i = 0; i < candles.length; i++) arr.push(directResult);
            return arr;
        }
        return directResult;
    }

    // ─── Aggregate base candles into HTF bars ───────────────────────
    var htfCandles = [];
    var htfBarIndices = []; // maps each HTF bar to [startBaseIdx, endBaseIdx]
    for (var i = 0; i < candles.length; i += factor) {
        var end = Math.min(i + factor, candles.length);
        var o = candles[i].open, h = candles[i].high, l = candles[i].low;
        var c = candles[end - 1].close, v = 0;
        for (var j = i; j < end; j++) {
            if (candles[j].high > h) h = candles[j].high;
            if (candles[j].low < l) l = candles[j].low;
            v += candles[j].volume;
        }
        htfCandles.push({
            openTime: candles[i].openTime,
            open: o, high: h, low: l, close: c, volume: v
        });
        htfBarIndices.push([i, end - 1]);
    }

    // ─── Evaluate fn on the HTF candle array ────────────────────────
    var htfResult = fn(htfCandles);

    // ─── Map HTF results back to base-timeframe bars ────────────────
    var result = [];
    for (var i = 0; i < candles.length; i++) result.push(null);

    if (typeof htfResult === "number") {
        // Single value — fill all bars
        for (var i = 0; i < candles.length; i++) result[i] = htfResult;
    } else if (Array.isArray(htfResult)) {
        // Series — one value per HTF bar, forward-fill to base bars
        for (var h = 0; h < htfBarIndices.length; h++) {
            var val = h < htfResult.length ? htfResult[h] : null;
            for (var b = htfBarIndices[h][0]; b <= htfBarIndices[h][1]; b++) {
                result[b] = val;
            }
        }
    } else if (htfResult !== null && typeof htfResult === "object") {
        // Object with named series (e.g. {upper:[], lower:[], middle:[]})
        var mapped = {};
        var keys = Object.keys(htfResult);
        for (var k = 0; k < keys.length; k++) {
            var key = keys[k];
            if (!Array.isArray(htfResult[key])) { mapped[key] = htfResult[key]; continue; }
            mapped[key] = [];
            for (var i = 0; i < candles.length; i++) mapped[key].push(null);
            for (var h = 0; h < htfBarIndices.length; h++) {
                var val = h < htfResult[key].length ? htfResult[key][h] : null;
                for (var b = htfBarIndices[h][0]; b <= htfBarIndices[h][1]; b++) {
                    mapped[key][b] = val;
                }
            }
        }
        return mapped;
    }
    return result;
}

/** Internal helper: parse interval string to milliseconds */
function _parseInterval(str) {
    str = str.toLowerCase().trim();
    var match = str.match(/^(\d+)\s*(m|min|h|hr|hour|d|day|w|week|M|mo|month)$/);
    if (!match) return 60000; // default 1m
    var n = parseInt(match[1]);
    var unit = match[2].charAt(0);
    switch (unit) {
        case 'm': return n * 60 * 1000;            // minutes
        case 'h': return n * 60 * 60 * 1000;        // hours
        case 'd': return n * 24 * 60 * 60 * 1000;   // days
        case 'w': return n * 7 * 24 * 60 * 60 * 1000; // weeks
        case 'M': return n * 30 * 24 * 60 * 60 * 1000; // months (approx)
        default:  return n * 60 * 1000;
    }
}

// ─── Input Function  ─────────────────────────────────────────────────────────
// input(name, defaultValue) — used by IndicatorEngine to extract configurable params.
// At runtime the engine pre-injects input values as JS vars, so this just returns the default.
var _inputs = {};
function input(name, defaultValue) {
    if (_inputs[name] !== undefined) return _inputs[name];
    return defaultValue;
}

// ═══════════════════════════════════════════════════════════════════════════════
//  Plot API  —  Strategies can call these to draw on the chart
// ═══════════════════════════════════════════════════════════════════════════════
//
//  Usage inside strategy(candles):
//    plot('EMA Fast', emaSeries(candles, 9), '#00E676');        // overlay line
//    plotPanel('RSI', rsiSeries(candles, 14), '#F0883E');       // sub-panel
//    hline(70, '#FF5252', 'Overbought');                       // horizontal line
//    plotFill('EMA Fast', 'EMA Slow', 'rgba(0,230,118,0.1)');  // fill between
//    plotLabel(candles.length-1, 'Entry!', 'below', '#00E676');
//    plotBgColor(candles.length-1, 'rgba(0,230,118,0.08)');
//
//  The engine reads _state.plot after evaluate() and passes it to the chart.
// ─────────────────────────────────────────────────────────────────────────────

function _ensurePlot() {
    if (typeof _state === 'undefined') _state = {};
    if (!_state.plot) _state.plot = { overlays:[], panels:[], hlines:[], fills:[], labels:[], bgcolor:[] };
    return _state.plot;
}

/**
 * plot(name, values, color, lineWidth, lineStyle)
 * Draw a line overlay on the main price chart.
 * @param values — array of numbers/nulls, one per candle
 */
function plot(name, values, color, lineWidth, lineStyle) {
    var p = _ensurePlot();
    // Replace if same name already plotted
    for (var i = 0; i < p.overlays.length; i++) { if (p.overlays[i].name === name) { p.overlays.splice(i,1); break; } }
    p.overlays.push({ name: name, values: values, color: color || '#58A6FF', lineWidth: lineWidth || 2, lineStyle: lineStyle || 0 });
}

/**
 * plotPanel(name, values, color, type, refLines, extraLines)
 * Draw in a separate sub-chart below the main chart.
 */
function plotPanel(name, values, color, type, refLines, extraLines) {
    var p = _ensurePlot();
    for (var i = 0; i < p.panels.length; i++) { if (p.panels[i].name === name) { p.panels.splice(i,1); break; } }
    p.panels.push({ name: name, values: values, color: color || '#9C27B0', type: type || 'line',
        lines: refLines || [], extraLines: extraLines || [] });
}

/**
 * hline(price, color, title, lineStyle)
 * Draw a horizontal reference line on the main chart.
 */
function hline(price, color, title, lineStyle) {
    var p = _ensurePlot();
    p.hlines.push({ price: price, color: color || '#58A6FF', title: title || '', lineStyle: lineStyle != null ? lineStyle : 2 });
}

/**
 * plotFill(line1name, line2name, color)
 * Fill between two previously plotted overlays.
 */
function plotFill(line1name, line2name, color) {
    var p = _ensurePlot();
    p.fills.push({ line1: line1name, line2: line2name, color: color || 'rgba(88,166,255,0.15)' });
}

/**
 * plotLabel(index, text, position, color)
 * Place a text label at a specific candle index. position = 'above' or 'below'.
 */
function plotLabel(index, text, position, color) {
    var p = _ensurePlot();
    p.labels.push({ index: index, text: text, position: position || 'above', color: color || '#FFFFFF' });
}

/**
 * plotBgColor(index, color)
 * Tint the background of a specific candle.
 */
function plotBgColor(index, color) {
    var p = _ensurePlot();
    p.bgcolor.push({ index: index, color: color || 'rgba(88,166,255,0.1)' });
}

/** resetPlot() — clear all plot data (call at start of strategy if you rebuild every bar). */
function resetPlot() {
    if (typeof _state !== 'undefined') _state.plot = { overlays:[], panels:[], hlines:[], fills:[], labels:[], bgcolor:[] };
}

// ═══════════════════════════════════════════════════════════════════════════════
//  TFG Algo  —  port of indicators/tfg_algo.pine (UT Bot + LinReg Candles + STC)
//  4-tier signal model:
//    Tier 1 (✓✓ strong)  : UT trigger + LinReg bias confirms + STC at extreme reversal
//    Tier 2 (✓  weak)    : UT trigger + LinReg confirms, STC neutral
//    Tier 3 (✗  weak)    : UT trigger against LinReg bias but STC supports reversal
//    Tier 4 (✗✗ counter) : UT trigger against LinReg and STC also against — avoid
// ═══════════════════════════════════════════════════════════════════════════════

/** Heikin-Ashi close series (incremental). */
function _haCloseSeries(candles) {
    var out = []; var prevO = null, prevC = null;
    for (var i = 0; i < candles.length; i++) {
        var c = candles[i];
        var hc = (c.open + c.high + c.low + c.close) / 4.0;
        var ho = (prevO === null) ? (c.open + c.close) / 2.0 : (prevO + prevC) / 2.0;
        out.push(hc);
        prevO = ho; prevC = hc;
    }
    return out;
}

/**
 * utBotSeries(candles, key, atrPeriod, useHA)
 *  Returns { xATS:[], buy:[], sell:[], pos:[] } — full series. Last index is current bar.
 */
function utBotSeries(candles, key, atrPeriod, useHA) {
    if (key == null) key = 1.0;
    if (atrPeriod == null) atrPeriod = 10;
    if (useHA == null) useHA = false;
    var n = candles.length;
    var srcArr = useHA ? _haCloseSeries(candles) : (function(){ var a=[]; for (var i=0;i<n;i++) a.push(candles[i].close); return a; })();
    // ATR (Wilder, on real OHLC)
    var atrArr = atrSeries(candles, atrPeriod);
    var xATS = []; var buy = []; var sell = []; var pos = [];
    var prevStop = 0.0; var prevPos = 0;
    var prevEma1 = null; var prevSrc = null;
    for (var i = 0; i < n; i++) {
        var src = srcArr[i];
        var atrV = atrArr[i];
        if (atrV === null || i === 0) {
            xATS.push(null); buy.push(false); sell.push(false); pos.push(0);
            prevSrc = src; prevEma1 = src; continue;
        }
        var nLoss = key * atrV;
        var stop;
        if (src > prevStop && prevSrc > prevStop)      stop = Math.max(prevStop, src - nLoss);
        else if (src < prevStop && prevSrc < prevStop) stop = Math.min(prevStop, src + nLoss);
        else if (src > prevStop)                       stop = src - nLoss;
        else                                            stop = src + nLoss;
        var p;
        if (prevSrc < prevStop && src > stop) p = 1;
        else if (prevSrc > prevStop && src < stop) p = -1;
        else p = prevPos;
        // EMA(1) of src is just src itself
        var ema1 = src;
        var crossUp   = (prevEma1 !== null) && prevEma1 <= prevStop && ema1 > stop;
        var crossDown = (prevEma1 !== null) && prevEma1 >= prevStop && ema1 < stop;
        var bSig = src > stop && crossUp;
        var sSig = src < stop && crossDown;
        xATS.push(stop); buy.push(bSig); sell.push(sSig); pos.push(p);
        prevStop = stop; prevPos = p; prevSrc = src; prevEma1 = ema1;
    }
    return { xATS: xATS, buy: buy, sell: sell, pos: pos };
}

/**
 * linregCandlesSeries(candles, lrLen, sigLen, sigType)
 *  Returns { bopen:[], bhigh:[], blow:[], bclose:[], sigLine:[] }
 */
function linregCandlesSeries(candles, lrLen, sigLen, sigType) {
    if (lrLen == null) lrLen = 11;
    if (sigLen == null) sigLen = 11;
    if (sigType == null) sigType = 'SMA';
    var n = candles.length;
    var oArr=[], hArr=[], lArr=[], cArr=[];
    for (var i = 0; i < n; i++) { oArr.push(candles[i].open); hArr.push(candles[i].high); lArr.push(candles[i].low); cArr.push(candles[i].close); }
    var bopen  = lrLen > 1 ? linregSeries(oArr, lrLen) : oArr;
    var bhigh  = lrLen > 1 ? linregSeries(hArr, lrLen) : hArr;
    var blow   = lrLen > 1 ? linregSeries(lArr, lrLen) : lArr;
    var bclose = lrLen > 1 ? linregSeries(cArr, lrLen) : cArr;
    // Build sigLine = SMA/EMA over bclose (skip nulls at start)
    var sigLine = [];
    if (sigType === 'EMA') {
        var mult = 2.0 / (sigLen + 1); var v = null; var sum = 0; var c = 0;
        for (var i = 0; i < n; i++) {
            if (bclose[i] === null) { sigLine.push(null); continue; }
            if (v === null) {
                sum += bclose[i]; c++;
                if (c === sigLen) { v = sum / sigLen; sigLine.push(v); } else sigLine.push(null);
            } else { v = (bclose[i] - v) * mult + v; sigLine.push(v); }
        }
    } else {
        // SMA over last sigLen non-null bclose
        var window = [];
        for (var i = 0; i < n; i++) {
            if (bclose[i] === null) { sigLine.push(null); continue; }
            window.push(bclose[i]);
            if (window.length > sigLen) window.shift();
            if (window.length === sigLen) {
                var s = 0; for (var j = 0; j < sigLen; j++) s += window[j];
                sigLine.push(s / sigLen);
            } else sigLine.push(null);
        }
    }
    return { bopen: bopen, bhigh: bhigh, blow: blow, bclose: bclose, sigLine: sigLine };
}

/**
 * stcSeries(candles, length, fast, slow, smooth)
 *  Returns { stc:[], rising:[] } — Schaff Trend Cycle.
 *  NOTE: Uses Pine-compatible EMA seeding (ema[0] = src[0], no SMA seed)
 *  so values match TradingView's ta.ema-based STC bar-for-bar.
 */
function stcSeries(candles, length, fast, slow, smooth) {
    if (length == null) length = 12;
    if (fast == null) fast = 26;
    if (slow == null) slow = 50;
    if (smooth == null) smooth = 0.5;
    var n = candles.length;
    if (n === 0) return { stc: [], rising: [] };
    // Pine ta.ema semantics: ema[0] = src[0]; ema[i] = alpha*src + (1-alpha)*ema[i-1]
    function _pineEmaArr(arr, period) {
        var out = []; var alpha = 2.0 / (period + 1); var prev = null;
        for (var k = 0; k < arr.length; k++) {
            if (arr[k] === null) { out.push(prev); continue; }
            if (prev === null) prev = arr[k];
            else prev = alpha * arr[k] + (1 - alpha) * prev;
            out.push(prev);
        }
        return out;
    }
    var closeArr = []; for (var k = 0; k < n; k++) closeArr.push(candles[k].close);
    var emaF = _pineEmaArr(closeArr, fast);
    var emaS = _pineEmaArr(closeArr, slow);
    // macdLine = emaF - emaS (Pine produces values from bar 0)
    var m = []; for (var i = 0; i < n; i++) {
        if (emaF[i] === null || emaS[i] === null) m.push(null); else m.push(emaF[i] - emaS[i]);
    }
    var f1 = []; var pff = []; var f2 = []; var pf = [];
    var prevF1 = null, prevPff = null, prevF2 = null, prevPf = null;
    for (var i = 0; i < n; i++) {
        if (m[i] === null) { f1.push(null); pff.push(null); f2.push(null); pf.push(null); continue; }
        // window of m for stochastic-like normalization
        var lo = Infinity, hi = -Infinity, cnt = 0;
        for (var j = Math.max(0, i - length + 1); j <= i; j++) {
            if (m[j] !== null) { if (m[j] < lo) lo = m[j]; if (m[j] > hi) hi = m[j]; cnt++; }
        }
        var v1 = lo, v2 = hi - lo;
        var f1v = v2 > 0 ? (m[i] - v1) / v2 * 100 : (prevF1 !== null ? prevF1 : 0);
        f1.push(f1v);
        var pffv = (prevF1 === null || prevPff === null) ? f1v : prevPff + smooth * (f1v - prevPff);
        pff.push(pffv);
        // Now stochastic-like over pff window
        var lo2 = Infinity, hi2 = -Infinity;
        for (var j = Math.max(0, i - length + 1); j <= i; j++) {
            if (pff[j] !== null) { if (pff[j] < lo2) lo2 = pff[j]; if (pff[j] > hi2) hi2 = pff[j]; }
        }
        var v3 = lo2, v4 = hi2 - lo2;
        var f2v = v4 > 0 ? (pffv - v3) / v4 * 100 : (prevF2 !== null ? prevF2 : 0);
        f2.push(f2v);
        var pfv = (prevF2 === null || prevPf === null) ? f2v : prevPf + smooth * (f2v - prevPf);
        pf.push(pfv);
        prevF1 = f1v; prevPff = pffv; prevF2 = f2v; prevPf = pfv;
    }
    var rising = [];
    for (var i = 0; i < n; i++) {
        if (i === 0 || pf[i] === null || pf[i-1] === null) rising.push(false);
        else rising.push(pf[i] > pf[i-1]);
    }
    return { stc: pf, rising: rising };
}

/**
 * tfgAlgoSignal(candles, opts) — high-level wrapper combining UT Bot + LinReg + STC.
 *  opts (all optional):
 *    utKey=1.0, utAtr=10, useHA=false,
 *    lrLen=11, sigLen=11, sigType='SMA',
 *    stcLen=12, stcFast=26, stcSlow=50, stcSmooth=0.5,
 *    stcOS=25, stcOB=75, useSTC=true
 *  Returns { action:'BUY'|'SELL'|'HOLD', tier:1|2|3|4|0, strong:bool, reason:string,
 *            stc, sigLine, bclose, xATS }
 */
function tfgAlgoSignal(candles, opts) {
    opts = opts || {};
    var utKey = opts.utKey != null ? opts.utKey : 1.0;
    var utAtr = opts.utAtr != null ? opts.utAtr : 10;
    var useHA = opts.useHA != null ? opts.useHA : false;
    var lrLen = opts.lrLen != null ? opts.lrLen : 11;
    var sigLen = opts.sigLen != null ? opts.sigLen : 11;
    var sigType = opts.sigType || 'SMA';
    var stcLen = opts.stcLen != null ? opts.stcLen : 12;
    var stcFast = opts.stcFast != null ? opts.stcFast : 26;
    var stcSlow = opts.stcSlow != null ? opts.stcSlow : 50;
    var stcSmooth = opts.stcSmooth != null ? opts.stcSmooth : 0.5;
    var OS = opts.stcOS != null ? opts.stcOS : 25;
    var OB = opts.stcOB != null ? opts.stcOB : 75;
    var useSTC = opts.useSTC != null ? opts.useSTC : true;

    var i = candles.length - 1;
    if (i < Math.max(utAtr, lrLen, sigLen, stcSlow) + 2) {
        return { action:'HOLD', tier:0, strong:false, reason:'warmup', stc:null, sigLine:null, bclose:null, xATS:null };
    }
    var ut = utBotSeries(candles, utKey, utAtr, useHA);
    var lr = linregCandlesSeries(candles, lrLen, sigLen, sigType);
    var st = stcSeries(candles, stcLen, stcFast, stcSlow, stcSmooth);

    var utBuy = ut.buy[i] === true;
    var utSell = ut.sell[i] === true;
    var bclose = lr.bclose[i];
    var sig = lr.sigLine[i];
    var stcVal = st.stc[i];
    var stcRising = st.rising[i];

    var lrOk = (bclose != null && sig != null);
    var stcOk = (stcVal != null);

    // ── STC-active tiers (mirror Pine t1..t4 Buy/Sell exactly) ──
    var t1Buy_a = utBuy  && lrOk && bclose >= sig && stcOk && stcVal <  OS && stcRising;
    var t2Buy_a = utBuy  && lrOk && bclose >= sig && !(stcOk && stcVal <  OS && stcRising);
    var t3Buy_a = utBuy  && lrOk && bclose <  sig && stcOk && stcVal <= OB;
    var t4Buy_a = utBuy  && lrOk && bclose <  sig && stcOk && stcVal >  OB;
    var t1Sell_a = utSell && lrOk && bclose <= sig && stcOk && stcVal >  OB && !stcRising;
    var t2Sell_a = utSell && lrOk && bclose <= sig && !(stcOk && stcVal >  OB && !stcRising);
    var t3Sell_a = utSell && lrOk && bclose >  sig && stcOk && stcVal >= OS;
    var t4Sell_a = utSell && lrOk && bclose >  sig && stcOk && stcVal <  OS;

    // ── 2-layer fallback (useSTC=false): only t1/t3 fire ──
    var t1Buy_f  = utBuy  && lrOk && bclose >= sig;
    var t3Buy_f  = utBuy  && lrOk && bclose <  sig;
    var t1Sell_f = utSell && lrOk && bclose <= sig;
    var t3Sell_f = utSell && lrOk && bclose >  sig;

    var fin1Buy  = useSTC ? t1Buy_a  : t1Buy_f;
    var fin2Buy  = useSTC ? t2Buy_a  : false;
    var fin3Buy  = useSTC ? t3Buy_a  : t3Buy_f;
    var fin4Buy  = useSTC ? t4Buy_a  : false;
    var fin1Sell = useSTC ? t1Sell_a : t1Sell_f;
    var fin2Sell = useSTC ? t2Sell_a : false;
    var fin3Sell = useSTC ? t3Sell_a : t3Sell_f;
    var fin4Sell = useSTC ? t4Sell_a : false;

    var action = 'HOLD'; var tier = 0; var strong = false; var reason = '';
    if (fin1Buy)       { action='BUY';  tier=1; strong=true;  reason='UT+LR↑+STC oversold rising'; }
    else if (fin2Buy)  { action='BUY';  tier=2; strong=false; reason='UT+LR↑'; }
    else if (fin3Buy)  { action='BUY';  tier=3; strong=false; reason='UT against LR but STC ok'; }
    else if (fin4Buy)  { action='BUY';  tier=4; strong=false; reason='UT counter-trend (avoid)'; }
    else if (fin1Sell) { action='SELL'; tier=1; strong=true;  reason='UT+LR↓+STC overbought falling'; }
    else if (fin2Sell) { action='SELL'; tier=2; strong=false; reason='UT+LR↓'; }
    else if (fin3Sell) { action='SELL'; tier=3; strong=false; reason='UT against LR but STC ok'; }
    else if (fin4Sell) { action='SELL'; tier=4; strong=false; reason='UT counter-trend (avoid)'; }

    return {
        action: action, tier: tier, strong: strong, reason: reason,
        stc: stcVal, sigLine: sig, bclose: bclose, xATS: ut.xATS[i]
    };
}
""".trimIndent()
    }
}
