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

    /** Evaluate and return multiple targeted signals (for multi-pair strategies). */
    fun evaluateMulti(code: String, candles: List<Candle>, params: Map<String, String>, account: ScriptAccount? = null): List<TargetedSignal> =
        listOf(TargetedSignal(null, evaluate(code, candles, params, account)))

    /** Returns console.log output captured during the last evaluate() call. */
    fun getLastLogs(): List<String> = emptyList()

    /** Try-compile code and return an error message, or null if valid. */
    fun validateSyntax(code: String): String? = null

    /** Reset persistent `_state` object between runs (e.g. between backtests). */
    fun resetState() {}
}
