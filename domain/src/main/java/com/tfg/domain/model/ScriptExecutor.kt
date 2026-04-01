package com.tfg.domain.model

/**
 * Executes user-written script code against candles.
 * Implemented by the trading-engine module's JS ScriptEngine.
 */
interface ScriptExecutor {
    fun evaluate(code: String, candles: List<Candle>, params: Map<String, String>): StrategySignal
}
