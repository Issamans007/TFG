package com.tfg.domain.repository

import com.tfg.domain.model.BacktestResult
import com.tfg.domain.model.Candle
import com.tfg.domain.model.CustomTemplate
import com.tfg.domain.model.MarketType
import com.tfg.domain.model.Script
import com.tfg.domain.model.SignalMarker
import kotlinx.coroutines.flow.Flow

interface ScriptRepository {
    fun getAllScripts(): Flow<List<Script>>
    suspend fun saveScript(script: Script)
    suspend fun deleteScript(id: String)
    suspend fun backtestScript(scriptId: String, templateId: String, symbol: String, interval: String, days: Int, onProgress: (Float) -> Unit = {}, makerFeePct: Double = 0.001, takerFeePct: Double = 0.001, slippagePct: Double = 0.0, startDateMs: Long? = null, endDateMs: Long? = null, marketTypeOverride: MarketType? = null, leverageOverride: Int? = null, startingCapital: Double = 10000.0): BacktestResult

    suspend fun activateStrategy(scriptId: String, symbol: String)
    suspend fun deactivateStrategy(scriptId: String)
    fun getActiveStrategy(): Flow<Script?>
    fun getBlockedPairs(): Flow<Set<String>>

    // Signal markers
    fun getSignalMarkers(symbol: String, interval: String): Flow<List<SignalMarker>>
    suspend fun saveSignalMarkers(markers: List<SignalMarker>)
    suspend fun clearSignalMarkers(symbol: String)

    // Custom templates
    fun getCustomTemplates(): Flow<List<CustomTemplate>>
    suspend fun saveCustomTemplate(template: CustomTemplate)
    suspend fun deleteCustomTemplate(id: String)

    // Script validation & debugging
    fun validateSyntax(code: String): String? = null
    fun getLastLogs(): List<String> = emptyList()

    /** Evaluate user code against candles and return the raw result map (type, sizePct, etc.). Used by replay mode. */
    fun evaluateCode(code: String, candles: List<Candle>): Map<String, Any?>? = null

    // Alias methods for convenience
    fun getAll(): Flow<List<Script>> = getAllScripts()
    suspend fun save(script: Script) = saveScript(script)
    suspend fun delete(id: String) = deleteScript(id)
    suspend fun backtest(scriptId: String, templateId: String, symbol: String, interval: String, days: Int, onProgress: (Float) -> Unit = {}, makerFeePct: Double = 0.001, takerFeePct: Double = 0.001, slippagePct: Double = 0.0, startDateMs: Long? = null, endDateMs: Long? = null, marketTypeOverride: MarketType? = null, leverageOverride: Int? = null, startingCapital: Double = 10000.0) =
        backtestScript(scriptId, templateId, symbol, interval, days, onProgress, makerFeePct, takerFeePct, slippagePct, startDateMs, endDateMs, marketTypeOverride, leverageOverride, startingCapital)

    /** C1: Multi-symbol backtest — evaluates custom code using evaluateMulti() across multiple pairs. */
    suspend fun backtestMultiSymbol(
        scriptId: String, symbols: List<String>, interval: String, days: Int,
        onProgress: (Float) -> Unit = {}, makerFeePct: Double = 0.001,
        takerFeePct: Double = 0.001, slippagePct: Double = 0.0,
        startDateMs: Long? = null, endDateMs: Long? = null,
        startingCapital: Double = 10000.0,
        applyFundingRate: Boolean = false
    ): BacktestResult = backtestScript(scriptId, "", symbols.first(), interval, days, onProgress, makerFeePct, takerFeePct, slippagePct, startDateMs, endDateMs, null, null, startingCapital)

    /**
     * B6: Walk-forward backtest. Splits the full date range into [windows] equal segments
     * and runs an independent backtest on each, returning the aggregated trade list and
     * a flat equity curve so the user can see whether the strategy generalises out-of-sample.
     * Each window starts fresh at [startingCapital] (no compounding across windows).
     */
    suspend fun backtestWalkForward(
        scriptId: String, templateId: String, symbol: String, interval: String,
        startDateMs: Long, endDateMs: Long, windows: Int,
        onProgress: (Float) -> Unit = {},
        makerFeePct: Double = 0.001, takerFeePct: Double = 0.001, slippagePct: Double = 0.0,
        marketTypeOverride: MarketType? = null, leverageOverride: Int? = null,
        startingCapital: Double = 10000.0
    ): BacktestResult {
        require(windows >= 2) { "walk-forward needs at least 2 windows" }
        require(endDateMs > startDateMs) { "endDateMs must be > startDateMs" }
        val span = endDateMs - startDateMs
        val winSize = span / windows
        val allTrades = mutableListOf<com.tfg.domain.model.BacktestTrade>()
        val combinedEquity = mutableListOf<Double>()
        var totalPnl = 0.0
        var winners = 0
        var losers = 0
        var grossProfit = 0.0
        var grossLoss = 0.0
        var maxDd = 0.0
        for (w in 0 until windows) {
            val from = startDateMs + w * winSize
            val to = if (w == windows - 1) endDateMs else from + winSize
            val r = backtestScript(
                scriptId, templateId, symbol, interval,
                days = ((to - from) / 86_400_000L).toInt().coerceAtLeast(1),
                onProgress = { p -> onProgress((w + p) / windows) },
                makerFeePct = makerFeePct, takerFeePct = takerFeePct, slippagePct = slippagePct,
                startDateMs = from, endDateMs = to,
                marketTypeOverride = marketTypeOverride, leverageOverride = leverageOverride,
                startingCapital = startingCapital
            )
            allTrades.addAll(r.trades)
            combinedEquity.addAll(r.equityCurve)
            totalPnl += r.totalPnl
            winners += r.trades.count { it.pnl > 0 }
            losers += r.trades.count { it.pnl < 0 }
            grossProfit += r.grossProfit
            grossLoss += r.grossLoss
            maxDd = maxOf(maxDd, r.maxDrawdown)
        }
        val totalTrades = allTrades.size
        return BacktestResult(
            scriptId = scriptId, symbol = symbol, timeframe = interval,
            startDate = startDateMs, endDate = endDateMs,
            totalTrades = totalTrades,
            winRate = if (totalTrades > 0) winners.toDouble() / totalTrades * 100 else 0.0,
            totalPnl = totalPnl,
            maxDrawdown = maxDd,
            sharpeRatio = 0.0, sortinoRatio = 0.0, trades = allTrades,
            profitFactor = if (grossLoss != 0.0) kotlin.math.abs(grossProfit / grossLoss) else 0.0,
            equityCurve = combinedEquity,
            startingCapital = startingCapital,
            grossProfit = grossProfit, grossLoss = grossLoss,
            backtestDays = ((endDateMs - startDateMs) / 86_400_000L).toInt()
        )
    }
}
