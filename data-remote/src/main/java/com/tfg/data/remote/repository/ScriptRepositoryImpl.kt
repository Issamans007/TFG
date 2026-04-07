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
        onProgress: (Float) -> Unit, makerFeePct: Double, takerFeePct: Double, slippagePct: Double,
        startDateMs: Long?, endDateMs: Long?
    ): BacktestResult = withContext(Dispatchers.Default) {
        // Calculate total candles needed for the requested period
        val effectiveEndMs = endDateMs ?: System.currentTimeMillis()
        val effectiveStartMs = startDateMs ?: (effectiveEndMs - days.toLong() * 24 * 60 * 60 * 1000)
        val spanMs = effectiveEndMs - effectiveStartMs
        val intervalMs = when (interval) {
            "1m" -> 60_000L; "5m" -> 300_000L; "15m" -> 900_000L; "1h" -> 3_600_000L
            "4h" -> 14_400_000L; "1d" -> 86_400_000L; else -> 3_600_000L
        }
        val totalCandles = (spanMs / intervalMs).toInt().coerceAtLeast(1)

        try {
            // Fetch candles in paginated batches (Binance max 1000 per request)
            val allKlines = mutableListOf<List<Any>>()
            var startTime = effectiveStartMs
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
                    // Reset JS _state before a fresh backtest run
                    scriptExecutor.resetState()

                    // Execute custom user code via JS engine per-bar for backtest
                    val trades = mutableListOf<BacktestTrade>()
                    val sigs = mutableListOf<SignalMarker>()
                    var equity = 10000.0
                    // position: side, (entryPrice, qty, entryTime)
                    var position: Pair<OrderSide, Triple<Double, Double, Long>>? = null
                    var slPrice: Double? = null   // per-bar TP/SL checking
                    var tpPrice: Double? = null
                    var entryBar = 0              // for barsInTrade tracking
                    val equityCurve = mutableListOf(equity)
                    val feePct = (makerFeePct + takerFeePct) / 2.0  // blended fee for custom code backtest
                    val slipPct = slippagePct / 100.0
                    val customTotal = candles.size - 50

                    fun closePos(exitPrice: Double, exitTime: Long, barIndex: Int) {
                        val (side, entry) = position ?: return
                        val pnl = if (side == OrderSide.BUY) (exitPrice - entry.first) * entry.second
                                  else (entry.first - exitPrice) * entry.second
                        val fee = (entry.first * entry.second + exitPrice * entry.second) * feePct
                        equity += pnl - fee
                        trades.add(BacktestTrade(entry.third, exitTime, side, entry.first, exitPrice, entry.second, pnl - fee, fee, barsInTrade = barIndex - entryBar))
                        sigs.add(SignalMarker("", scriptId, symbol, interval, exitTime, SignalType.CLOSE, exitPrice))
                        position = null; slPrice = null; tpPrice = null
                    }

                    for (i in 50 until candles.size) {
                        onProgress(0.3f + 0.7f * (i - 50) / customTotal)
                        val bar = candles[i]

                        // ── Per-bar TP/SL checking before evaluating new signals ──
                        if (position != null) {
                            val (side, _) = position!!
                            if (side == OrderSide.BUY) {
                                if (slPrice != null && bar.low <= slPrice!!) { closePos(slPrice!!, bar.openTime, i); }
                                else if (tpPrice != null && bar.high >= tpPrice!!) { closePos(tpPrice!!, bar.openTime, i); }
                            } else {
                                if (slPrice != null && bar.high >= slPrice!!) { closePos(slPrice!!, bar.openTime, i); }
                                else if (tpPrice != null && bar.low <= tpPrice!!) { closePos(tpPrice!!, bar.openTime, i); }
                            }
                        }

                        // ── Evaluate strategy with account context ──
                        val slice = candles.subList(0, i + 1)
                        val pos = position
                        val acct = ScriptAccount(
                            equity = equity,
                            balance = equity,
                            positionSide = pos?.first?.name,
                            positionPnl = if (pos != null) {
                                if (pos.first == OrderSide.BUY) (bar.close - pos.second.first) * pos.second.second
                                else (pos.second.first - bar.close) * pos.second.second
                            } else 0.0,
                            positionSize = pos?.second?.second ?: 0.0,
                            positionEntry = pos?.second?.first ?: 0.0
                        )
                        val sig = scriptExecutor.evaluate(userCode, slice, scriptParams, acct)

                        when (sig) {
                            is StrategySignal.BUY -> {
                                if (position != null && position!!.first == OrderSide.SELL) closePos(bar.close, bar.openTime, i)
                                if (position == null) {
                                    val qty = (equity * sig.sizePct / 100.0) / bar.close
                                    position = Pair(OrderSide.BUY, Triple(bar.close, qty, bar.openTime))
                                    entryBar = i
                                    slPrice = sig.stopLossPct?.let { bar.close * (1 - it / 100.0) }
                                    tpPrice = sig.takeProfitPct?.let { bar.close * (1 + it / 100.0) }
                                    sigs.add(SignalMarker("", scriptId, symbol, interval, bar.openTime, SignalType.BUY, bar.close))
                                }
                            }
                            is StrategySignal.SELL -> {
                                if (position != null && position!!.first == OrderSide.BUY) closePos(bar.close, bar.openTime, i)
                                if (position == null) {
                                    val qty = (equity * sig.sizePct / 100.0) / bar.close
                                    position = Pair(OrderSide.SELL, Triple(bar.close, qty, bar.openTime))
                                    entryBar = i
                                    slPrice = sig.stopLossPct?.let { bar.close * (1 + it / 100.0) }
                                    tpPrice = sig.takeProfitPct?.let { bar.close * (1 - it / 100.0) }
                                    sigs.add(SignalMarker("", scriptId, symbol, interval, bar.openTime, SignalType.SELL, bar.close))
                                }
                            }
                            is StrategySignal.CLOSE_IF_LONG -> {
                                if (position?.first == OrderSide.BUY) closePos(bar.close, bar.openTime, i)
                            }
                            is StrategySignal.CLOSE_IF_SHORT -> {
                                if (position?.first == OrderSide.SELL) closePos(bar.close, bar.openTime, i)
                            }
                            else -> {}
                        }
                        equityCurve.add(equity)
                    }

                    // ── Compute all backtest metrics ──
                    val totalPnl = equity - 10000.0
                    val winners = trades.filter { it.pnl > 0 }
                    val losers = trades.filter { it.pnl < 0 }
                    val winRate = if (trades.isNotEmpty()) winners.size.toDouble() / trades.size * 100 else 0.0
                    val maxDd = run { var peak = 10000.0; var dd = 0.0; for (e in equityCurve) { peak = maxOf(peak, e); dd = maxOf(dd, (peak - e) / peak * 100) }; dd }
                    val grossProfit = winners.sumOf { it.pnl }
                    val grossLoss = losers.sumOf { it.pnl }
                    val profitFactor = if (grossLoss != 0.0) Math.abs(grossProfit / grossLoss) else if (grossProfit > 0) Double.MAX_VALUE else 0.0
                    val avgWin = if (winners.isNotEmpty()) grossProfit / winners.size else 0.0
                    val avgLoss = if (losers.isNotEmpty()) grossLoss / losers.size else 0.0
                    val expectancy = if (trades.isNotEmpty()) totalPnl / trades.size else 0.0
                    val largestWin = winners.maxOfOrNull { it.pnl } ?: 0.0
                    val largestLoss = losers.minOfOrNull { it.pnl } ?: 0.0
                    val avgBarsInTrade = if (trades.isNotEmpty()) trades.sumOf { it.barsInTrade }.toDouble() / trades.size else 0.0
                    // Sharpe ratio (interval-aware)
                    val periodsPerYear = when (interval) {
                        "1m" -> 525600.0; "5m" -> 105120.0; "15m" -> 35040.0; "1h" -> 8760.0; "4h" -> 2190.0; "1d" -> 365.0; else -> 8760.0
                    }
                    val returns = equityCurve.zipWithNext { a, b -> if (a > 0) (b - a) / a else 0.0 }
                    val meanRet = if (returns.isNotEmpty()) returns.average() else 0.0
                    val stdRet = if (returns.size > 1) Math.sqrt(returns.sumOf { (it - meanRet) * (it - meanRet) } / (returns.size - 1)) else 0.0
                    val sharpe = if (stdRet > 0) meanRet / stdRet * Math.sqrt(periodsPerYear) else 0.0
                    val downsideReturns = returns.filter { it < 0.0 }
                    val downsideDev = if (downsideReturns.size > 1) Math.sqrt(downsideReturns.sumOf { it * it } / downsideReturns.size) else 0.0
                    val sortino = if (downsideDev > 0) meanRet / downsideDev * Math.sqrt(periodsPerYear) else 0.0
                    // Consecutive wins/losses
                    var maxConsW = 0; var maxConsL = 0; var cw = 0; var cl = 0
                    for (t in trades) { if (t.pnl > 0) { cw++; cl = 0; maxConsW = maxOf(maxConsW, cw) } else { cl++; cw = 0; maxConsL = maxOf(maxConsL, cl) } }
                    // Buy & hold
                    val buyHold = if (candles.size >= 51) (candles.last().close - candles[50].close) / candles[50].close * 100 else 0.0

                    val returnPct = totalPnl / 10000.0
                    Pair(BacktestResult(
                        scriptId = scriptId, symbol = symbol, timeframe = interval,
                        startDate = candles.first().openTime, endDate = candles.last().openTime,
                        totalTrades = trades.size, winRate = winRate, totalPnl = totalPnl,
                        maxDrawdown = maxDd, sharpeRatio = sharpe, sortinoRatio = sortino, trades = trades,
                        profitFactor = profitFactor, buyAndHoldReturn = buyHold,
                        avgBarsInTrade = avgBarsInTrade, grossProfit = grossProfit, grossLoss = grossLoss,
                        maxConsecutiveWins = maxConsW, maxConsecutiveLosses = maxConsL,
                        equityCurve = equityCurve, startingCapital = 10000.0,
                        expectancy = expectancy, avgWin = avgWin, avgLoss = avgLoss,
                        largestWin = largestWin, largestLoss = largestLoss,
                        backtestDays = days, entryAmount = 100.0, finalAmount = 100.0 * (1.0 + returnPct)
                    ), sigs)
                } else {
                    StrategyEvaluator.backtest(templateId, candles, symbol, interval, scriptParams, backtestDays = days,
                        makerFeePct = makerFeePct, takerFeePct = takerFeePct, slippagePct = slippagePct,
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

    // ─── Script Validation & Debugging ─────────────────────────────

    override fun validateSyntax(code: String): String? = scriptExecutor.validateSyntax(code)

    override fun getLastLogs(): List<String> = scriptExecutor.getLastLogs()

    // ─── C1: Multi-symbol backtest ──────────────────────────────────

    override suspend fun backtestMultiSymbol(
        scriptId: String, symbols: List<String>, interval: String, days: Int,
        onProgress: (Float) -> Unit, makerFeePct: Double, takerFeePct: Double,
        slippagePct: Double, startDateMs: Long?, endDateMs: Long?
    ): BacktestResult = withContext(Dispatchers.Default) {
        try {
            val effectiveEndMs = endDateMs ?: System.currentTimeMillis()
            val effectiveStartMs = startDateMs ?: (effectiveEndMs - days.toLong() * 24 * 60 * 60 * 1000)
            val spanMs = effectiveEndMs - effectiveStartMs
            val intervalMs = when (interval) {
                "1m" -> 60_000L; "5m" -> 300_000L; "15m" -> 900_000L; "1h" -> 3_600_000L
                "4h" -> 14_400_000L; "1d" -> 86_400_000L; else -> 3_600_000L
            }
            val totalCandles = (spanMs / intervalMs).toInt().coerceAtLeast(1)

            // Fetch candles for all symbols
            val allSymbolCandles = mutableMapOf<String, List<Candle>>()
            for ((idx, sym) in symbols.withIndex()) {
                val allKlines = mutableListOf<List<Any>>()
                var startTime = effectiveStartMs
                while (allKlines.size < totalCandles) {
                    val batchLimit = minOf(1000, totalCandles - allKlines.size)
                    val klines = binanceApi.getKlines(sym, interval, batchLimit, startTime)
                    if (klines.isEmpty()) break
                    allKlines.addAll(klines)
                    val lastCloseTime = (klines.last()[6] as Double).toLong()
                    startTime = lastCloseTime + 1
                    if (klines.size < batchLimit) break
                }
                allSymbolCandles[sym] = allKlines.map { k ->
                    Candle(
                        symbol = sym, interval = interval,
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
                onProgress(0.3f * (idx + 1) / symbols.size)
            }

            val primarySymbol = symbols.first()
            val primaryCandles = allSymbolCandles[primarySymbol] ?: emptyList()
            if (primaryCandles.size < 51) {
                return@withContext BacktestResult(scriptId, primarySymbol, interval, 0, 0, 0, 0.0, 0.0, 0.0, 0.0)
            }

            // Load script code and params
            scriptExecutor.resetState()
            val scriptEntity = try { scriptDao.getById(scriptId).first() } catch (_: Exception) { null }
            val userCode = scriptEntity?.code ?: ""
            val scriptParams: Map<String, String> = try {
                scriptEntity?.paramsJson?.let { gson.fromJson(it, object : TypeToken<Map<String, String>>() {}.type) } ?: emptyMap()
            } catch (_: Exception) { emptyMap() }

            // Per-symbol position tracking
            data class PosState(val side: OrderSide, val entryPrice: Double, val qty: Double, val entryTime: Long, val entryBar: Int)
            val positions = mutableMapOf<String, PosState>()
            val trades = mutableListOf<BacktestTrade>()
            val sigs = mutableListOf<SignalMarker>()
            var equity = 10000.0
            val equityCurve = mutableListOf(equity)
            val feePct = (makerFeePct + takerFeePct) / 2.0
            val customTotal = primaryCandles.size - 50

            fun closePos(sym: String, exitPrice: Double, exitTime: Long, barIndex: Int) {
                val pos = positions.remove(sym) ?: return
                val pnl = if (pos.side == OrderSide.BUY) (exitPrice - pos.entryPrice) * pos.qty
                          else (pos.entryPrice - exitPrice) * pos.qty
                val fee = (pos.entryPrice * pos.qty + exitPrice * pos.qty) * feePct
                equity += pnl - fee
                trades.add(BacktestTrade(pos.entryTime, exitTime, pos.side, pos.entryPrice, exitPrice, pos.qty, pnl - fee, fee, barsInTrade = barIndex - pos.entryBar))
                sigs.add(SignalMarker("", scriptId, sym, interval, exitTime, SignalType.CLOSE, exitPrice))
            }

            for (i in 50 until primaryCandles.size) {
                onProgress(0.3f + 0.7f * (i - 50) / customTotal)
                val primaryBar = primaryCandles[i]

                // Build account context from aggregate
                val totalPosPnl = positions.entries.sumOf { (sym, pos) ->
                    val bar = allSymbolCandles[sym]?.getOrNull(i) ?: return@sumOf 0.0
                    if (pos.side == OrderSide.BUY) (bar.close - pos.entryPrice) * pos.qty
                    else (pos.entryPrice - bar.close) * pos.qty
                }
                val acct = ScriptAccount(
                    equity = equity + totalPosPnl, balance = equity,
                    positionSide = if (positions.isNotEmpty()) positions.values.first().side.name else null,
                    positionPnl = totalPosPnl,
                    positionSize = positions.values.sumOf { it.qty },
                    positionEntry = positions.values.firstOrNull()?.entryPrice ?: 0.0
                )

                val slice = primaryCandles.subList(0, i + 1)
                val signals = scriptExecutor.evaluateMulti(userCode, slice, scriptParams, acct)

                for (ts in signals) {
                    val targetSym = ts.symbol ?: primarySymbol
                    val targetBar = allSymbolCandles[targetSym]?.getOrNull(i) ?: continue
                    val sig = ts.signal
                    when (sig) {
                        is StrategySignal.BUY -> {
                            if (positions[targetSym]?.side == OrderSide.SELL) closePos(targetSym, targetBar.close, targetBar.openTime, i)
                            if (targetSym !in positions) {
                                val qty = (equity * sig.sizePct / 100.0) / targetBar.close
                                positions[targetSym] = PosState(OrderSide.BUY, targetBar.close, qty, targetBar.openTime, i)
                                sigs.add(SignalMarker("", scriptId, targetSym, interval, targetBar.openTime, SignalType.BUY, targetBar.close))
                            }
                        }
                        is StrategySignal.SELL -> {
                            if (positions[targetSym]?.side == OrderSide.BUY) closePos(targetSym, targetBar.close, targetBar.openTime, i)
                            if (targetSym !in positions) {
                                val qty = (equity * sig.sizePct / 100.0) / targetBar.close
                                positions[targetSym] = PosState(OrderSide.SELL, targetBar.close, qty, targetBar.openTime, i)
                                sigs.add(SignalMarker("", scriptId, targetSym, interval, targetBar.openTime, SignalType.SELL, targetBar.close))
                            }
                        }
                        is StrategySignal.CLOSE_IF_LONG -> {
                            if (positions[targetSym]?.side == OrderSide.BUY) closePos(targetSym, targetBar.close, targetBar.openTime, i)
                        }
                        is StrategySignal.CLOSE_IF_SHORT -> {
                            if (positions[targetSym]?.side == OrderSide.SELL) closePos(targetSym, targetBar.close, targetBar.openTime, i)
                        }
                        else -> {}
                    }
                }
                equityCurve.add(equity)
            }

            // Close any remaining open positions at last bar price
            val lastIdx = primaryCandles.size - 1
            for ((sym, _) in positions.toMap()) {
                val lastBar = allSymbolCandles[sym]?.lastOrNull() ?: continue
                closePos(sym, lastBar.close, lastBar.openTime, lastIdx)
            }

            // Compute metrics
            val totalPnl = equity - 10000.0
            val winners = trades.filter { it.pnl > 0 }
            val losers = trades.filter { it.pnl < 0 }
            val winRate = if (trades.isNotEmpty()) winners.size.toDouble() / trades.size * 100 else 0.0
            val maxDd = run { var peak = 10000.0; var dd = 0.0; for (e in equityCurve) { peak = maxOf(peak, e); dd = maxOf(dd, (peak - e) / peak * 100) }; dd }
            val grossProfit = winners.sumOf { it.pnl }
            val grossLoss = losers.sumOf { it.pnl }
            val profitFactor = if (grossLoss != 0.0) Math.abs(grossProfit / grossLoss) else if (grossProfit > 0) Double.MAX_VALUE else 0.0
            val avgWin = if (winners.isNotEmpty()) grossProfit / winners.size else 0.0
            val avgLoss = if (losers.isNotEmpty()) grossLoss / losers.size else 0.0
            val expectancy = if (trades.isNotEmpty()) totalPnl / trades.size else 0.0
            val largestWin = winners.maxOfOrNull { it.pnl } ?: 0.0
            val largestLoss = losers.minOfOrNull { it.pnl } ?: 0.0
            val avgBarsInTrade = if (trades.isNotEmpty()) trades.sumOf { it.barsInTrade }.toDouble() / trades.size else 0.0
            val periodsPerYear = when (interval) {
                "1m" -> 525600.0; "5m" -> 105120.0; "15m" -> 35040.0; "1h" -> 8760.0; "4h" -> 2190.0; "1d" -> 365.0; else -> 8760.0
            }
            val returns = equityCurve.zipWithNext { a, b -> if (a > 0) (b - a) / a else 0.0 }
            val meanRet = if (returns.isNotEmpty()) returns.average() else 0.0
            val stdRet = if (returns.size > 1) Math.sqrt(returns.sumOf { (it - meanRet) * (it - meanRet) } / (returns.size - 1)) else 0.0
            val sharpe = if (stdRet > 0) meanRet / stdRet * Math.sqrt(periodsPerYear) else 0.0
            val downsideReturns = returns.filter { it < 0.0 }
            val downsideDev = if (downsideReturns.size > 1) Math.sqrt(downsideReturns.sumOf { it * it } / downsideReturns.size) else 0.0
            val sortino = if (downsideDev > 0) meanRet / downsideDev * Math.sqrt(periodsPerYear) else 0.0
            var maxConsW = 0; var maxConsL = 0; var cw = 0; var cl = 0
            for (t in trades) { if (t.pnl > 0) { cw++; cl = 0; maxConsW = maxOf(maxConsW, cw) } else { cl++; cw = 0; maxConsL = maxOf(maxConsL, cl) } }
            val buyHold = if (primaryCandles.size >= 51) (primaryCandles.last().close - primaryCandles[50].close) / primaryCandles[50].close * 100 else 0.0
            val returnPct = totalPnl / 10000.0

            BacktestResult(
                scriptId = scriptId, symbol = symbols.joinToString(","), timeframe = interval,
                startDate = primaryCandles.first().openTime, endDate = primaryCandles.last().openTime,
                totalTrades = trades.size, winRate = winRate, totalPnl = totalPnl,
                maxDrawdown = maxDd, sharpeRatio = sharpe, sortinoRatio = sortino, trades = trades,
                profitFactor = profitFactor, buyAndHoldReturn = buyHold,
                avgBarsInTrade = avgBarsInTrade, grossProfit = grossProfit, grossLoss = grossLoss,
                maxConsecutiveWins = maxConsW, maxConsecutiveLosses = maxConsL,
                equityCurve = equityCurve, startingCapital = 10000.0,
                expectancy = expectancy, avgWin = avgWin, avgLoss = avgLoss,
                largestWin = largestWin, largestLoss = largestLoss,
                backtestDays = days, entryAmount = 100.0, finalAmount = 100.0 * (1.0 + returnPct)
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Multi-symbol backtest failed")
            BacktestResult(scriptId, symbols.joinToString(","), interval, 0, 0, 0, 0.0, 0.0, 0.0, 0.0)
        }
    }

    override fun evaluateCode(code: String, candles: List<Candle>): Map<String, Any?>? {
        return try {
            val sig = scriptExecutor.evaluate(code, candles, emptyMap())
            when (sig) {
                is StrategySignal.BUY -> mapOf("type" to "BUY", "sizePct" to sig.sizePct, "stopLossPct" to sig.stopLossPct, "takeProfitPct" to sig.takeProfitPct)
                is StrategySignal.SELL -> mapOf("type" to "SELL", "sizePct" to sig.sizePct, "stopLossPct" to sig.stopLossPct, "takeProfitPct" to sig.takeProfitPct)
                is StrategySignal.CLOSE_IF_LONG -> mapOf("type" to "CLOSE_IF_LONG")
                is StrategySignal.CLOSE_IF_SHORT -> mapOf("type" to "CLOSE_IF_SHORT")
                else -> mapOf("type" to "HOLD")
            }
        } catch (_: Exception) {
            null
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
