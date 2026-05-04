package com.tfg.domain.model

/**
 * Executes user-written script code against candles.
 * Implemented by the trading-engine module's JS ScriptEngine.
 */
interface ScriptExecutor {
    fun evaluate(code: String, candles: List<Candle>, params: Map<String, String>): StrategySignal

    /** Evaluate with optional account context injected as `_account` in JS. */
    fun evaluate(code: String, candles: List<Candle>, params: Map<String, String>, account: ScriptAccount?): StrategySignal =
        evaluate(code, candles, params)

    /** Evaluate with account context and higher-timeframe candle arrays injected as `_htf` in JS. */
    fun evaluate(code: String, candles: List<Candle>, params: Map<String, String>, account: ScriptAccount?, htfCandles: Map<String, List<Candle>>): StrategySignal =
        evaluate(code, candles, params, account)

    /**
     * Evaluate with account context, HTF candles (`_htf`), and related-symbol candles (`_related`).
     * Related symbols allow strategies to read other pairs' candle data, e.g.
     * `_related["BTCUSDT"]` inside the strategy JS.
     */
    fun evaluate(code: String, candles: List<Candle>, params: Map<String, String>, account: ScriptAccount?, htfCandles: Map<String, List<Candle>>, relatedCandles: Map<String, List<Candle>>): StrategySignal =
        evaluate(code, candles, params, account, htfCandles)

    /** Evaluate and return multiple targeted signals (for multi-pair strategies). */
    fun evaluateMulti(code: String, candles: List<Candle>, params: Map<String, String>, account: ScriptAccount? = null): List<TargetedSignal> =
        listOf(TargetedSignal(null, evaluate(code, candles, params, account)))

    /** Returns console.log output captured during the last evaluate() call. */
    fun getLastLogs(): List<String> = emptyList()

    /** Returns the `_state.dashboard` object from the last evaluate() as a JSON string, or null. */
    fun getLastDashboard(): String? = null

    /** Returns the `_state.plot` object from the last evaluate() as a JSON string, or null. */
    fun getLastPlotData(): String? = null

    /** Returns the `_state.diag` object from the last evaluate() as a JSON string, or null.
     *  Strategies can populate this to surface why they emitted (or didn't emit) signals. */
    fun getLastDiag(): String? = null

    /**
     * Returns the number of evaluate() calls that have thrown / timed out and
     * fallen back to HOLD since the last reset, then resets the counter to 0.
     * The backtest engine reads this at the end of a run to surface silent
     * QuickJS watchdog timeouts that would otherwise hide as "0 trades".
     */
    fun getAndResetErrorCount(): Int = 0

    /** Try-compile code and return an error message, or null if valid. */
    fun validateSyntax(code: String): String? = null

    /**
     * Runs the strategy ONCE on the supplied candles and reports any runtime
     * error along with captured console output. Used by the "Check Script"
     * button to surface errors that the backtest loop would otherwise swallow.
     */
    fun runtimeCheck(code: String, candles: List<Candle>, params: Map<String, String>): RuntimeCheckResult =
        RuntimeCheckResult(ok = false, signalType = "HOLD", errorMessage = "runtimeCheck not implemented", logs = emptyList(), elapsedMs = 0L)

    /** Reset persistent `_state` object between runs (e.g. between backtests). */
    fun resetState() {}
}

/** Result of a one-shot runtime check (see [ScriptExecutor.runtimeCheck]). */
data class RuntimeCheckResult(
    val ok: Boolean,
    val signalType: String,
    val errorMessage: String?,
    val logs: List<String>,
    val elapsedMs: Long
)
