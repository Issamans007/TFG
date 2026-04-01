package com.tfg.data.remote.repository

import com.tfg.data.local.dao.CandleDao
import com.tfg.data.local.dao.CustomTemplateDao
import com.tfg.data.local.dao.ScriptDao
import com.tfg.data.local.dao.SignalMarkerDao
import com.tfg.data.local.entity.CandleEntity
import com.tfg.data.local.entity.CustomTemplateEntity
import com.tfg.data.local.entity.ScriptEntity
import com.tfg.data.local.entity.SignalMarkerEntity
import com.tfg.data.remote.api.BinanceApi
import com.tfg.domain.model.*
import com.tfg.domain.repository.ScriptRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScriptRepositoryImpl @Inject constructor(
    private val scriptDao: ScriptDao,
    private val signalMarkerDao: SignalMarkerDao,
    private val customTemplateDao: CustomTemplateDao,
    private val candleDao: CandleDao,
    private val binanceApi: BinanceApi,
    private val scriptExecutor: ScriptExecutor
) : ScriptRepository {

    private val gson = Gson()
    private val stringMapType = object : TypeToken<Map<String, String>>() {}.type

    override fun getAllScripts(): Flow<List<Script>> =
        scriptDao.getAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun saveScript(script: Script) {
        val entity = ScriptEntity(
            id = script.id.ifBlank { UUID.randomUUID().toString() },
            name = script.name, code = script.code,
            isActive = script.isActive, activeSymbol = script.activeSymbol,
            strategyTemplateId = script.strategyTemplateId,
            paramsJson = if (script.params.isNotEmpty()) gson.toJson(script.params) else null,
            createdAt = script.createdAt, updatedAt = System.currentTimeMillis()
        )
        scriptDao.insert(entity)
    }

    override suspend fun deleteScript(id: String) = scriptDao.delete(id)

    override suspend fun activateStrategy(scriptId: String, symbol: String) {
        scriptDao.deactivateAll()
        scriptDao.activate(scriptId, symbol)
    }

    override suspend fun deactivateStrategy(scriptId: String) = scriptDao.deactivateAll()

    override fun getActiveStrategy(): Flow<Script?> =
        scriptDao.getActiveScript().map { it?.toDomain() }

    override fun getBlockedPairs(): Flow<Set<String>> =
        scriptDao.getBlockedSymbols().map { it.toSet() }

    // ─── Signal Markers ─────────────────────────────────────────────

    override fun getSignalMarkers(symbol: String, interval: String): Flow<List<SignalMarker>> =
        signalMarkerDao.getForChart(symbol, interval).map { list ->
            list.map { SignalMarker(it.id, it.scriptId, it.symbol, it.interval, it.openTime, SignalType.valueOf(it.signalType), it.price, it.timestamp) }
        }

    override suspend fun saveSignalMarkers(markers: List<SignalMarker>) {
        signalMarkerDao.insertAll(markers.map {
            SignalMarkerEntity(it.id, it.scriptId, it.symbol, it.interval, it.openTime, it.signalType.name, it.price, it.timestamp)
        })
    }

    override suspend fun clearSignalMarkers(symbol: String) =
        signalMarkerDao.deleteForSymbol(symbol)

    // ─── Custom Templates ───────────────────────────────────────────

    override fun getCustomTemplates(): Flow<List<CustomTemplate>> =
        customTemplateDao.getAll().map { entities ->
            entities.map { e ->
                CustomTemplate(
                    id = e.id, name = e.name, description = e.description,
                    baseTemplateId = e.baseTemplateId, code = e.code,
                    defaultParams = e.defaultParamsJson?.let {
                        try { gson.fromJson<Map<String, String>>(it, stringMapType) }
                        catch (_: Exception) { emptyMap() }
                    } ?: emptyMap(),
                    createdAt = e.createdAt
                )
            }
        }

    override suspend fun saveCustomTemplate(template: CustomTemplate) {
        customTemplateDao.insert(
            CustomTemplateEntity(
                id = template.id.ifBlank { UUID.randomUUID().toString() },
                name = template.name, description = template.description,
                baseTemplateId = template.baseTemplateId, code = template.code,
                defaultParamsJson = if (template.defaultParams.isNotEmpty()) gson.toJson(template.defaultParams) else null,
                createdAt = template.createdAt
            )
        )
    }

    override suspend fun deleteCustomTemplate(id: String) =
        customTemplateDao.delete(id)

    // ─── Backtesting ────────────────────────────────────────────────

    override suspend fun backtestScript(
        scriptId: String, templateId: String, symbol: String, interval: String, days: Int,
        onProgress: (Float) -> Unit
    ): BacktestResult = withContext(Dispatchers.Default) {
        // Calculate total candles needed for the requested period
        val totalCandles = when (interval) {
            "1m" -> days * 24 * 60
            "5m" -> days * 24 * 12
            "15m" -> days * 24 * 4
            "1h" -> days * 24
            "4h" -> days * 6
            "1d" -> days
            else -> days * 24
        }

        try {
            // Fetch candles in paginated batches (Binance max 1000 per request)
            val allKlines = mutableListOf<List<Any>>()
            var startTime = System.currentTimeMillis() - days.toLong() * 24 * 60 * 60 * 1000
            while (allKlines.size < totalCandles) {
                val batchLimit = minOf(1000, totalCandles - allKlines.size)
                val klines = binanceApi.getKlines(symbol, interval, batchLimit, startTime)
                if (klines.isEmpty()) break
                allKlines.addAll(klines)
                onProgress(0.3f * allKlines.size / totalCandles)
                // Advance startTime past the last candle's close time
                val lastCloseTime = (klines.last()[6] as Double).toLong()
                startTime = lastCloseTime + 1
                if (klines.size < batchLimit) break // No more data available
            }

            val candles = allKlines.map { k ->
                Candle(
                    symbol = symbol, interval = interval,
                    openTime = (k[0] as Double).toLong(),
                    open = (k[1] as String).toDouble(),
                    high = (k[2] as String).toDouble(),
                    low = (k[3] as String).toDouble(),
                    close = (k[4] as String).toDouble(),
                    volume = (k[5] as String).toDouble(),
                    closeTime = (k[6] as Double).toLong(),
                    quoteVolume = (k[7] as String).toDouble(),
                    numberOfTrades = (k[8] as Double).toInt()
                )
            }

            // Store candles in DB for chart display
            candleDao.deleteForPair(symbol, interval)
            candleDao.insertAll(candles.map { c ->
                CandleEntity(
                    symbol = c.symbol, interval = c.interval, openTime = c.openTime,
                    open = c.open, high = c.high, low = c.low, close = c.close,
                    volume = c.volume, closeTime = c.closeTime,
                    quoteVolume = c.quoteVolume, numberOfTrades = c.numberOfTrades
                )
            })

            // Run backtest using StrategyEvaluator
            // Load script params if available
            val scriptParams = try {
                val entity = scriptDao.getById(scriptId).first()
                entity?.paramsJson?.let {
                    gson.fromJson<Map<String, String>>(it, object : TypeToken<Map<String, String>>() {}.type)
                } ?: emptyMap()
            } catch (_: Exception) { emptyMap<String, String>() }
            val (result, signals) = run {
                // Check if code differs from template -> use JS engine for custom code
                val scriptEntity = try { scriptDao.getById(scriptId).first() } catch (_: Exception) { null }
                val userCode = scriptEntity?.code ?: ""
                val templateCode = try { StrategyTemplates.getAll().firstOrNull { it.id.name == templateId }?.code ?: "" } catch (_: Exception) { "" }
                val isCustomCode = userCode.isNotBlank() && userCode.trimIndent() != templateCode.trimIndent()

                if (isCustomCode) {
                    // Execute custom user code via JS engine per-bar for backtest
                    val trades = mutableListOf<BacktestTrade>()
                    val sigs = mutableListOf<SignalMarker>()
                    var equity = 10000.0
                    var position: Pair<OrderSide, Triple<Double, Double, Long>>? = null // side, (price, qty, time)
                    val equityCurve = mutableListOf(equity)
                    val feePct = 0.001
                    val customTotal = candles.size - 50
                    for (i in 50 until candles.size) {
                        onProgress(0.3f + 0.7f * (i - 50) / customTotal)
                        val slice = candles.subList(0, i + 1)
                        val sig = scriptExecutor.evaluate(userCode, slice, scriptParams)
                        val bar = candles[i]
                        when (sig) {
                            is StrategySignal.BUY -> {
                                // Close any existing short position first
                                if (position != null && position!!.first == OrderSide.SELL) {
                                    val (_, entry) = position!!
                                    val pnl = (entry.first - bar.close) * entry.second // short PnL
                                    val fee = (entry.first * entry.second + bar.close * entry.second) * feePct
                                    equity += pnl - fee
                                    trades.add(BacktestTrade(entry.third, bar.openTime, OrderSide.SELL, entry.first, bar.close, entry.second, pnl - fee, fee))
                                    sigs.add(SignalMarker("", scriptId, symbol, interval, bar.openTime, SignalType.CLOSE, bar.close))
                                    position = null
                                }
                                // Open long if no position
                                if (position == null) {
                                    val qty = (equity * sig.sizePct / 100.0) / bar.close
                                    position = Pair(OrderSide.BUY, Triple(bar.close, qty, bar.openTime))
                                    sigs.add(SignalMarker("", scriptId, symbol, interval, bar.openTime, SignalType.BUY, bar.close))
                                }
                            }
                            is StrategySignal.SELL -> {
                                // Close any existing long position first
                                if (position != null && position!!.first == OrderSide.BUY) {
                                    val (_, entry) = position!!
                                    val pnl = (bar.close - entry.first) * entry.second // long PnL
                                    val fee = (entry.first * entry.second + bar.close * entry.second) * feePct
                                    equity += pnl - fee
                                    trades.add(BacktestTrade(entry.third, bar.openTime, OrderSide.BUY, entry.first, bar.close, entry.second, pnl - fee, fee))
                                    sigs.add(SignalMarker("", scriptId, symbol, interval, bar.openTime, SignalType.CLOSE, bar.close))
                                    position = null
                                }
                                // Open short if no position
                                if (position == null) {
                                    val qty = (equity * sig.sizePct / 100.0) / bar.close
                                    position = Pair(OrderSide.SELL, Triple(bar.close, qty, bar.openTime))
                                    sigs.add(SignalMarker("", scriptId, symbol, interval, bar.openTime, SignalType.SELL, bar.close))
                                }
                            }
                            is StrategySignal.CLOSE_IF_LONG -> {
                                if (position != null && position!!.first == OrderSide.BUY) {
                                    val (_, entry) = position!!
                                    val pnl = (bar.close - entry.first) * entry.second
                                    val fee = (entry.first * entry.second + bar.close * entry.second) * feePct
                                    equity += pnl - fee
                                    trades.add(BacktestTrade(entry.third, bar.openTime, OrderSide.BUY, entry.first, bar.close, entry.second, pnl - fee, fee))
                                    sigs.add(SignalMarker("", scriptId, symbol, interval, bar.openTime, SignalType.CLOSE, bar.close))
                                    position = null
                                }
                            }
                            is StrategySignal.CLOSE_IF_SHORT -> {
                                if (position != null && position!!.first == OrderSide.SELL) {
                                    val (_, entry) = position!!
                                    val pnl = (entry.first - bar.close) * entry.second
                                    val fee = (entry.first * entry.second + bar.close * entry.second) * feePct
                                    equity += pnl - fee
                                    trades.add(BacktestTrade(entry.third, bar.openTime, OrderSide.SELL, entry.first, bar.close, entry.second, pnl - fee, fee))
                                    sigs.add(SignalMarker("", scriptId, symbol, interval, bar.openTime, SignalType.CLOSE, bar.close))
                                    position = null
                                }
                            }
                            else -> {}
                        }
                        equityCurve.add(equity)
                    }
                    val totalPnl = equity - 10000.0
                    val winners = trades.filter { it.pnl > 0 }
                    val winRate = if (trades.isNotEmpty()) winners.size.toDouble() / trades.size * 100 else 0.0
                    val maxDd = run { var peak = 10000.0; var dd = 0.0; for (e in equityCurve) { peak = maxOf(peak, e); dd = maxOf(dd, (peak - e) / peak * 100) }; dd }
                    val returnPct = totalPnl / 10000.0
                    Pair(BacktestResult(scriptId, symbol, interval, candles.first().openTime, candles.last().openTime,
                        trades.size, winRate, totalPnl, maxDd, 0.0, trades, equityCurve = equityCurve, startingCapital = 10000.0,
                        backtestDays = days, entryAmount = 100.0, finalAmount = 100.0 * (1.0 + returnPct)), sigs)
                } else {
                    StrategyEvaluator.backtest(templateId, candles, symbol, interval, scriptParams, backtestDays = days,
                        onProgress = { p -> onProgress(0.3f + 0.7f * p) })
                }
            }

            // Persist signal markers for chart overlay
            val taggedSignals = signals.map { it.copy(scriptId = scriptId) }
            signalMarkerDao.deleteForScriptAndSymbol(scriptId, symbol)
            signalMarkerDao.insertAll(taggedSignals.map {
                SignalMarkerEntity(it.id, it.scriptId, it.symbol, it.interval, it.openTime, it.signalType.name, it.price, it.timestamp)
            })

            result.copy(scriptId = scriptId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Backtest failed for $symbol")
            BacktestResult(scriptId, symbol, interval, 0, 0, 0, 0.0, 0.0, 0.0, 0.0)
        }
    }

    private fun ScriptEntity.toDomain(): Script {
        val p: Map<String, String> = paramsJson?.let {
            try { gson.fromJson(it, object : TypeToken<Map<String, String>>() {}.type) } catch (_: Exception) { emptyMap() }
        } ?: emptyMap()
        return Script(
            id = id, name = name, code = code,
            isActive = isActive, activeSymbol = activeSymbol,
            strategyTemplateId = strategyTemplateId,
            params = p,
            createdAt = createdAt, updatedAt = updatedAt
        )
    }
}
