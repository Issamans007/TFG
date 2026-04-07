package com.tfg.domain.repository

import com.tfg.domain.model.BacktestResult
import com.tfg.domain.model.Candle
import com.tfg.domain.model.CustomTemplate
import com.tfg.domain.model.Script
import com.tfg.domain.model.SignalMarker
import kotlinx.coroutines.flow.Flow

interface ScriptRepository {
    fun getAllScripts(): Flow<List<Script>>
    suspend fun saveScript(script: Script)
    suspend fun deleteScript(id: String)
    suspend fun backtestScript(scriptId: String, templateId: String, symbol: String, interval: String, days: Int, onProgress: (Float) -> Unit = {}, makerFeePct: Double = 0.001, takerFeePct: Double = 0.001, slippagePct: Double = 0.0, startDateMs: Long? = null, endDateMs: Long? = null): BacktestResult

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
    suspend fun backtest(scriptId: String, templateId: String, symbol: String, interval: String, days: Int, onProgress: (Float) -> Unit = {}, makerFeePct: Double = 0.001, takerFeePct: Double = 0.001, slippagePct: Double = 0.0, startDateMs: Long? = null, endDateMs: Long? = null) =
        backtestScript(scriptId, templateId, symbol, interval, days, onProgress, makerFeePct, takerFeePct, slippagePct, startDateMs, endDateMs)

    /** C1: Multi-symbol backtest — evaluates custom code using evaluateMulti() across multiple pairs. */
    suspend fun backtestMultiSymbol(
        scriptId: String, symbols: List<String>, interval: String, days: Int,
        onProgress: (Float) -> Unit = {}, makerFeePct: Double = 0.001,
        takerFeePct: Double = 0.001, slippagePct: Double = 0.0,
        startDateMs: Long? = null, endDateMs: Long? = null
    ): BacktestResult = backtestScript(scriptId, "", symbols.first(), interval, days, onProgress, makerFeePct, takerFeePct, slippagePct, startDateMs, endDateMs)
}
