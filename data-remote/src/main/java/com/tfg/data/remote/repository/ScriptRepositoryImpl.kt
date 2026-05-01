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
import com.tfg.data.remote.api.BinanceFuturesApi
import com.tfg.domain.model.*
import com.tfg.domain.repository.FeeRepository
import com.tfg.domain.repository.ScriptRepository
import com.tfg.domain.service.ConsoleBus
import com.tfg.domain.service.ConsoleSource
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
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
    private val binanceFuturesApi: BinanceFuturesApi,
    private val scriptExecutor: ScriptExecutor,
    private val feeRepository: FeeRepository,
    private val consoleBus: ConsoleBus
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
            list.map { SignalMarker(it.id, it.scriptId, it.symbol, it.interval, it.openTime, SignalType.valueOf(it.signalType), it.price, it.label, it.orderType, it.timestamp) }
        }

    override suspend fun saveSignalMarkers(markers: List<SignalMarker>) {
        signalMarkerDao.insertAll(markers.map {
            SignalMarkerEntity(it.id, it.scriptId, it.symbol, it.interval, it.openTime, it.signalType.name, it.price, it.label, it.orderType, it.timestamp)
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
        startDateMs: Long?, endDateMs: Long?,
        marketTypeOverride: MarketType?, leverageOverride: Int?,
        startingCapital: Double
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
            consoleBus.info(
                ConsoleSource.BACKTEST,
                title = "Fetching candles",
                message = "$symbol $interval — need ~$totalCandles bars",
                symbol = symbol
            )
            // Fetch candles in paginated batches (Binance max 1000 per request)
            val allKlines = mutableListOf<List<Any>>()
            var startTime = effectiveStartMs
            while (allKlines.size < totalCandles) {
                val batchLimit = minOf(1000, totalCandles - allKlines.size)
                val klines = try {
                    binanceApi.getKlines(symbol, interval, batchLimit, startTime)
                } catch (e: Exception) {
                    consoleBus.warn(
                        ConsoleSource.NETWORK,
                        title = "Kline fetch error",
                        message = "${e::class.simpleName}: ${e.message ?: ""}",
                        symbol = symbol
                    )
                    Timber.w(e, "Binance kline fetch failed")
                    emptyList()
                }
                if (klines.isEmpty()) break
                allKlines.addAll(klines)
                onProgress(0.3f * allKlines.size / totalCandles)
                // Advance startTime past the last candle's close time
                val lastCloseTime = (klines.last()[6] as Double).toLong()
                startTime = lastCloseTime + 1
                if (klines.size < batchLimit) break // No more data available
            }

            val candles = if (allKlines.isNotEmpty()) {
                allKlines.map { k ->
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
            } else {
                // Network failed — fall back to whatever's already cached in DB so
                // the user still sees a result instead of a silent zero-bar run.
                val cached = candleDao.getAllCandles(symbol, interval).first().map { c ->
                    Candle(c.symbol, c.interval, c.openTime, c.open, c.high, c.low, c.close,
                        c.volume, c.closeTime, c.quoteVolume, c.numberOfTrades)
                }
                if (cached.isNotEmpty()) {
                    consoleBus.warn(
                        ConsoleSource.BACKTEST,
                        title = "Using cached candles",
                        message = "Network returned 0 klines — running on ${cached.size} cached bars instead.",
                        symbol = symbol
                    )
                }
                cached
            }

            // ── Hard guards: bail out loudly instead of silently returning 0/0 ──
            if (candles.isEmpty()) {
                onProgress(1.0f)
                consoleBus.error(
                    ConsoleSource.BACKTEST,
                    title = "No candles available",
                    message = "Binance returned no klines for $symbol $interval and the local cache is empty. " +
                        "Check your network / symbol name / time window and try again.",
                    symbol = symbol
                )
                return@withContext BacktestResult(scriptId, symbol, interval, 0, 0, 0, 0.0, 0.0, 0.0, 0.0)
            }
            if (candles.size <= 50) {
                onProgress(1.0f)
                consoleBus.error(
                    ConsoleSource.BACKTEST,
                    title = "Not enough candles",
                    message = "Got ${candles.size} bars but the engine needs > 50 to warm up indicators. " +
                        "Increase the backtest window or pick a smaller interval.",
                    symbol = symbol
                )
                return@withContext BacktestResult(scriptId, symbol, interval, 0, 0, 0, 0.0, 0.0, 0.0, 0.0)
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

            // Run backtest via JS engine
            // Load script params if available
            val scriptParams = try {
                val entity = scriptDao.getById(scriptId).first()
                entity?.paramsJson?.let {
                    gson.fromJson<Map<String, String>>(it, object : TypeToken<Map<String, String>>() {}.type)
                } ?: emptyMap()
            } catch (_: Exception) { emptyMap<String, String>() }
            // All strategies (template and custom) execute via JS engine
            val scriptEntity = try { scriptDao.getById(scriptId).first() } catch (_: Exception) { null }
            val userCode = scriptEntity?.code ?: ""
            if (userCode.isBlank()) {
                onProgress(1.0f)
                consoleBus.error(
                    ConsoleSource.BACKTEST,
                    title = "Empty strategy code",
                    message = "The script (id=$scriptId) has no JavaScript code attached. " +
                        "Re-select a template or paste code in the editor.",
                    symbol = symbol
                )
                return@withContext BacktestResult(scriptId, symbol, interval, 0, 0, 0, 0.0, 0.0, 0.0, 0.0)
            }

            // Load user's TradingPlan from script params (with optional UI overrides for backtest dialog)
            val basePlan = TradingPlan.fromJson(scriptParams[TradingPlan.PARAM_KEY])
            val effectiveMt = marketTypeOverride ?: basePlan.marketType
            val plan = basePlan.copy(
                marketType = effectiveMt,
                leverage = leverageOverride ?: basePlan.leverage,
                // When the user explicitly switches to Futures from the backtest
                // dialog, automatically allow shorts — otherwise SELL signals would
                // be silently dropped because TradingPlan.allowShort defaults to false.
                allowShort = if (marketTypeOverride == MarketType.FUTURES_USDM) true else basePlan.allowShort
            )

            // Reset JS _state before a fresh backtest run
            scriptExecutor.resetState()

            // Wipe ALL signal markers for this symbol BEFORE running so that
            // stale markers from prior runs / prior scripts don't stack on the
            // chart. After this, any markers loaded for (symbol, interval) are
            // guaranteed to belong to the run we're about to execute.
            signalMarkerDao.deleteForSymbol(symbol)

            val trades = mutableListOf<BacktestTrade>()
            val signals = mutableListOf<SignalMarker>()
            val initialCapital = if (startingCapital > 0.0) startingCapital else 10_000.0
            var equity = initialCapital
            // Position now tracks marketType + leverage so closes apply correct fee
            data class PosState(val side: OrderSide, val entry: Double, val qty: Double, val time: Long, val marketType: MarketType, val leverage: Int, val entryReason: String? = null)
            var position: PosState? = null
            var slPrice: Double? = null
            var tpPrice: Double? = null
            // Multi-level TP: list of (tpPrice, qtyFraction 0-1 of ORIGINAL qty)
            var tpLevels: MutableList<Pair<Double, Double>>? = null
            var originalQty = 0.0
            var entryBar = 0
            var lastCloseBar = -10_000
            val equityCurve = mutableListOf(equity)
            // B3: Funding-rate simulation for FUTURES positions.
            // Binance funding settles every 8h (00:00, 08:00, 16:00 UTC).
            // Long pays the rate when positive, short receives. Use the
            // historical rate per symbol if available; fall back to a neutral
            // 0.01% when not.
            val fundingHistory: List<Pair<Long, Double>> = if (candles.isNotEmpty()) {
                runCatching {
                    binanceFuturesApi.getFundingRate(symbol = symbol, limit = 1000)
                        .map { Pair(it.fundingTime, it.fundingRate.toDoubleOrNull() ?: 0.0001) }
                        .filter { it.first in candles.first().openTime..candles.last().closeTime }
                }.getOrDefault(emptyList())
            } else emptyList()
            var fundingIdx = 0
            // Pull live fee config so backtest matches real fills
            val feeCfg = runCatching { feeRepository.getFeeConfig().first() }.getOrNull()
            val spotTaker = feeCfg?.effectiveTakerRate() ?: takerFeePct
            val futuresTaker = feeCfg?.effectiveFuturesTakerRate() ?: 0.0004
            // Caller-supplied takerFeePct still wins when feeCfg is unavailable.
            fun feeRate(mt: MarketType): Double = if (mt == MarketType.FUTURES_USDM) futuresTaker else spotTaker
            val slipPct = slippagePct / 100.0
            // Reset JS execution error counter so we can surface a count at
            // end-of-run (silent HOLD-on-timeout would otherwise hide it).
            scriptExecutor.getAndResetErrorCount()
            val barTotal = candles.size - 50
            consoleBus.info(
                ConsoleSource.BACKTEST,
                title = "Evaluating strategy",
                message = "$symbol $interval — looping $barTotal bars (${candles.size} total) • " +
                    "market=${plan.marketType} short=${plan.allowShort}",
                symbol = symbol
            )
            // Diagnostic counters surfaced at end of run
            var rawBuyCount = 0
            var rawSellCount = 0
            var spotShortSkipped = 0

            fun closePos(exitPrice: Double, exitTime: Long, barIndex: Int, partialQty: Double? = null, closeType: SignalType = SignalType.CLOSE) {
                val pos = position ?: return
                val closeQty = partialQty ?: pos.qty
                // Slippage on exit: long sells INTO the book → fill below trigger;
                // short buys to cover → fill above trigger. Always against the trader.
                val filledExit = if (pos.side == OrderSide.BUY) exitPrice * (1.0 - slipPct)
                                 else exitPrice * (1.0 + slipPct)
                val pnlGross = if (pos.side == OrderSide.BUY) (filledExit - pos.entry) * closeQty
                          else (pos.entry - filledExit) * closeQty
                // Fee on notional (entry leg + exit leg) — leverage does NOT inflate fee per unit qty
                // because qty already encodes leveraged exposure on futures.
                val r = feeRate(pos.marketType)
                val fee = (pos.entry * closeQty + filledExit * closeQty) * r
                equity += pnlGross - fee
                val exitReasonStr = when (closeType) {
                    SignalType.TP_HIT -> if (partialQty != null) "TP partial" else "Take profit"
                    SignalType.SL_HIT -> "Stop loss"
                    SignalType.CLOSE -> "Strategy close"
                    else -> closeType.name
                }
                trades.add(BacktestTrade(
                    pos.time, exitTime, pos.side, pos.entry, filledExit, closeQty, pnlGross - fee, fee,
                    barsInTrade = barIndex - entryBar,
                    entryReason = pos.entryReason,
                    exitReason = exitReasonStr
                ))
                if (partialQty != null && partialQty < pos.qty) {
                    val remaining = pos.qty - partialQty
                    position = pos.copy(qty = remaining)
                    // Move SL to breakeven after first TP if plan asks
                    if (plan.moveSlToBreakeven && closeType == SignalType.TP_HIT) {
                        slPrice = pos.entry
                    }
                    signals.add(SignalMarker(UUID.randomUUID().toString(), scriptId, symbol, interval, exitTime, closeType, exitPrice))
                } else {
                    signals.add(SignalMarker(UUID.randomUUID().toString(), scriptId, symbol, interval, exitTime, closeType, exitPrice))
                    position = null; slPrice = null; tpPrice = null; tpLevels = null; originalQty = 0.0
                    lastCloseBar = barIndex
                }
            }

            for (i in 50 until candles.size) {
                // Cooperative cancellation: if the user starts a new backtest
                // (or hits Cancel), this throws CancellationException and the
                // loop bails out cleanly so workers are released.
                coroutineContext.ensureActive()
                if (barTotal > 0) onProgress(0.3f + 0.7f * (i - 50) / barTotal.toFloat())
                val bar = candles[i]

                // B3: Apply funding for FUTURES positions on every funding
                // event that elapsed in this bar (00:00 / 08:00 / 16:00 UTC).
                // Long pays positive funding, short receives.
                val pos0 = position
                if (pos0 != null && pos0.marketType == MarketType.FUTURES_USDM) {
                    while (fundingIdx < fundingHistory.size && fundingHistory[fundingIdx].first <= bar.closeTime) {
                        val rate = fundingHistory[fundingIdx].second
                        val notional = bar.close * pos0.qty
                        val sideSign = if (pos0.side == OrderSide.BUY) 1.0 else -1.0
                        equity -= notional * rate * sideSign
                        fundingIdx++
                    }
                }

                // ── Per-bar TP/SL checking before evaluating new signals ──
                if (position != null) {
                    val side = position!!.side

                    // SL check first (closes entire remaining position)
                    if (side == OrderSide.BUY && slPrice != null && bar.low <= slPrice!!) {
                        closePos(slPrice!!, bar.openTime, i, closeType = SignalType.SL_HIT); continue
                    } else if (side == OrderSide.SELL && slPrice != null && bar.high >= slPrice!!) {
                        closePos(slPrice!!, bar.openTime, i, closeType = SignalType.SL_HIT); continue
                    }

                    // Multi-level TP check
                    if (tpLevels != null && tpLevels!!.isNotEmpty()) {
                        val iter = tpLevels!!.iterator()
                        while (iter.hasNext()) {
                            val (tpPx, qtyFrac) = iter.next()
                            val hit = if (side == OrderSide.BUY) bar.high >= tpPx else bar.low <= tpPx
                            if (hit && position != null) {
                                val closeQty = originalQty * qtyFrac
                                closePos(tpPx, bar.openTime, i, partialQty = closeQty, closeType = SignalType.TP_HIT)
                                iter.remove()
                            }
                        }
                    } else if (tpPrice != null) {
                        // Fallback single TP
                        if (side == OrderSide.BUY && bar.high >= tpPrice!!) { closePos(tpPrice!!, bar.openTime, i, closeType = SignalType.TP_HIT) }
                        else if (side == OrderSide.SELL && bar.low <= tpPrice!!) { closePos(tpPrice!!, bar.openTime, i, closeType = SignalType.TP_HIT) }
                    }

                    // If TP fully closed the position, skip strategy eval to prevent
                    // same-bar re-entry (matches Pine Script behavior)
                    if (position == null) {
                        equityCurve.add(equity)
                        continue
                    }
                }

                // ── Evaluate strategy via JS engine with account context ──
                // Cap slice to a sliding window of the most recent bars. Heavy
                // strategies (e.g. ISSA ALGO) compute full indicator *series*
                // every bar; on a growing slice that's O(n²) and quickly
                // exceeds the QuickJS 5s/bar watchdog, after which every
                // subsequent bar silently returns HOLD. 500 bars is enough
                // warmup for the slowest defaults (EMA200, STC_SLOW=50).
                val sliceStart = maxOf(0, i + 1 - 500)
                val slice = candles.subList(sliceStart, i + 1)
                val pos = position
                val acct = ScriptAccount(
                    equity = equity,
                    balance = equity,
                    positionSide = pos?.side?.name,
                    positionPnl = if (pos != null) {
                        if (pos.side == OrderSide.BUY) (bar.close - pos.entry) * pos.qty
                        else (pos.entry - bar.close) * pos.qty
                    } else 0.0,
                    positionSize = pos?.qty ?: 0.0,
                    positionEntry = pos?.entry ?: 0.0
                )
                val rawSig = scriptExecutor.evaluate(userCode, slice, scriptParams, acct)
                when (rawSig) {
                    is StrategySignal.BUY -> rawBuyCount++
                    is StrategySignal.SELL -> rawSellCount++
                    else -> {}
                }
                // Per-bar ATR for SL sizing (Wilder)
                val atrAbs = computeAtrAbs(slice, period = 14)
                val sig = com.tfg.domain.model.PlanApplier.apply(rawSig, plan, atrAbs, bar.close, bar.openTime)

                when (sig) {
                    is StrategySignal.BUY -> {
                        if (position != null && position!!.side == OrderSide.SELL) closePos(bar.close, bar.openTime, i)
                        val cooldownActive = plan.cooldownBars > 0 && (i - lastCloseBar) < plan.cooldownBars
                        if (position == null && !cooldownActive) {
                            val mt = sig.marketType ?: MarketType.SPOT
                            val lev = (sig.leverage ?: 1).coerceIn(1, 125)
                            // Buy entry pays slippage above the close.
                            val entryFill = bar.close * (1.0 + slipPct)
                            val qty = (equity * sig.sizePct / 100.0 * (if (mt == MarketType.FUTURES_USDM) lev else 1)) / entryFill
                            val entryReasonStr = sig.labelRaw?.takeIf { it.isNotBlank() }
                                ?: if (sig.label == SignalLabel.PATTERN) "Pattern BUY" else "BUY signal"
                            position = PosState(OrderSide.BUY, entryFill, qty, bar.openTime, mt, lev, entryReason = entryReasonStr)
                            entryBar = i
                            originalQty = qty
                            slPrice = sig.stopLossPct?.let { entryFill * (1 - it / 100.0) }
                            if (sig.takeProfitLevels.isNotEmpty()) {
                                tpLevels = sig.takeProfitLevels.map { lv ->
                                    Pair(entryFill * (1 + lv.pct / 100.0), lv.quantityPct / 100.0)
                                }.sortedBy { it.first }.toMutableList()
                                tpPrice = null
                            } else {
                                tpPrice = sig.takeProfitPct?.let { entryFill * (1 + it / 100.0) }
                                tpLevels = null
                            }
                            val buyType = if (sig.label == SignalLabel.PATTERN) SignalType.PAT_BUY else SignalType.BUY
                            signals.add(SignalMarker(UUID.randomUUID().toString(), scriptId, symbol, interval, bar.openTime, buyType, entryFill, label = sig.labelRaw, orderType = sig.orderType.name))
                        }
                    }
                    is StrategySignal.SELL -> {
                        if (position != null && position!!.side == OrderSide.BUY) closePos(bar.close, bar.openTime, i)
                        val cooldownActiveSell = plan.cooldownBars > 0 && (i - lastCloseBar) < plan.cooldownBars
                        if (position == null && !cooldownActiveSell) {
                            val mt = sig.marketType ?: MarketType.SPOT
                            val lev = (sig.leverage ?: 1).coerceIn(1, 125)
                            // Spot can't go short — skip pure short opens on SPOT (only close existing long)
                            if (mt == MarketType.SPOT) { spotShortSkipped++; equityCurve.add(equity); continue }
                            // Sell entry receives slippage below the close.
                            val entryFill = bar.close * (1.0 - slipPct)
                            val qty = (equity * sig.sizePct / 100.0 * lev) / entryFill
                            val entryReasonStr = sig.labelRaw?.takeIf { it.isNotBlank() }
                                ?: if (sig.label == SignalLabel.PATTERN) "Pattern SELL" else "SELL signal"
                            position = PosState(OrderSide.SELL, entryFill, qty, bar.openTime, mt, lev, entryReason = entryReasonStr)
                            entryBar = i
                            originalQty = qty
                            slPrice = sig.stopLossPct?.let { entryFill * (1 + it / 100.0) }
                            if (sig.takeProfitLevels.isNotEmpty()) {
                                tpLevels = sig.takeProfitLevels.map { lv ->
                                    Pair(entryFill * (1 - lv.pct / 100.0), lv.quantityPct / 100.0)
                                }.sortedByDescending { it.first }.toMutableList()
                                tpPrice = null
                            } else {
                                tpPrice = sig.takeProfitPct?.let { entryFill * (1 - it / 100.0) }
                                tpLevels = null
                            }
                            val sellType = if (sig.label == SignalLabel.PATTERN) SignalType.PAT_SELL else SignalType.SELL
                            signals.add(SignalMarker(UUID.randomUUID().toString(), scriptId, symbol, interval, bar.openTime, sellType, entryFill, label = sig.labelRaw, orderType = sig.orderType.name))
                        }
                    }
                    is StrategySignal.CLOSE_IF_LONG -> {
                        if (position?.side == OrderSide.BUY) closePos(bar.close, bar.openTime, i)
                    }
                    is StrategySignal.CLOSE_IF_SHORT -> {
                        if (position?.side == OrderSide.SELL) closePos(bar.close, bar.openTime, i)
                    }
                    else -> {}
                }
                equityCurve.add(equity)
            }

            // ── Compute all backtest metrics ──
            val totalPnl = equity - initialCapital
            val winners = trades.filter { it.pnl > 0 }
            val losers = trades.filter { it.pnl < 0 }
            val winRate = if (trades.isNotEmpty()) winners.size.toDouble() / trades.size * 100 else 0.0
            val maxDd = run { var peak = initialCapital; var dd = 0.0; for (e in equityCurve) { peak = maxOf(peak, e); dd = maxOf(dd, (peak - e) / peak * 100) }; dd }
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
            val buyHold = if (candles.size >= 51) (candles.last().close - candles[50].close) / candles[50].close * 100 else 0.0

            val returnPct = totalPnl / initialCapital
            val result = BacktestResult(
                scriptId = scriptId, symbol = symbol, timeframe = interval,
                startDate = candles.first().openTime, endDate = candles.last().openTime,
                totalTrades = trades.size, winRate = winRate, totalPnl = totalPnl,
                maxDrawdown = maxDd, sharpeRatio = sharpe, sortinoRatio = sortino, trades = trades,
                profitFactor = profitFactor, buyAndHoldReturn = buyHold,
                avgBarsInTrade = avgBarsInTrade, grossProfit = grossProfit, grossLoss = grossLoss,
                maxConsecutiveWins = maxConsW, maxConsecutiveLosses = maxConsL,
                equityCurve = equityCurve, startingCapital = initialCapital,
                expectancy = expectancy, avgWin = avgWin, avgLoss = avgLoss,
                largestWin = largestWin, largestLoss = largestLoss,
                backtestDays = days, entryAmount = 100.0, finalAmount = 100.0 * (1.0 + returnPct)
            )

            // Persist signal markers for chart overlay
            val taggedSignals = signals.map { it.copy(scriptId = scriptId) }
            signalMarkerDao.deleteForScriptAndSymbol(scriptId, symbol)
            signalMarkerDao.insertAll(taggedSignals.map {
                SignalMarkerEntity(it.id, it.scriptId, it.symbol, it.interval, it.openTime, it.signalType.name, it.price, it.label, it.orderType, it.timestamp)
            })

            onProgress(1.0f)
            Timber.i("Backtest done: $symbol $interval candles=${candles.size} bars=${barTotal} signals=${signals.size} trades=${trades.size}")
            // If the strategy populated _state.diag (e.g. ISSA ALGO tier counts),
            // surface the breakdown so the user can see WHY a run produced 0 trades.
            val diagJson = scriptExecutor.getLastDiag()
            if (!diagJson.isNullOrBlank()) {
                consoleBus.info(
                    ConsoleSource.STRATEGY,
                    title = "Strategy diag",
                    message = diagJson,
                    symbol = symbol
                )
            }
            consoleBus.info(
                ConsoleSource.BACKTEST,
                title = "Strategy diagnostics",
                message = "raw BUY=$rawBuyCount, raw SELL=$rawSellCount, " +
                    "kept signals=${signals.size}, trades=${trades.size}" +
                    if (spotShortSkipped > 0) ", SELL-on-SPOT skipped=$spotShortSkipped (enable Futures to allow shorts)"
                    else "",
                symbol = symbol
            )
            if (rawBuyCount == 0 && rawSellCount == 0) {
                consoleBus.warn(
                    ConsoleSource.STRATEGY,
                    title = "Strategy emitted no signals",
                    message = "The script never returned BUY/SELL across $barTotal bars. " +
                        "Check your input thresholds (e.g. MIN_TIER) — they may be too strict for this data.",
                    symbol = symbol
                )
            }
            // Surface JS execution errors / watchdog timeouts so a "successful"
            // backtest with 0 trades isn't silently caused by per-bar timeouts.
            val jsErrCount = scriptExecutor.getAndResetErrorCount()
            if (jsErrCount > 0) {
                val pctBars = if (barTotal > 0) (jsErrCount * 100.0 / barTotal) else 0.0
                consoleBus.warn(
                    ConsoleSource.STRATEGY,
                    title = "Strategy errors during backtest",
                    message = "$jsErrCount of $barTotal bars (${"%.1f".format(java.util.Locale.US, pctBars)}%) " +
                        "threw or timed out and silently fell back to HOLD. Consider simplifying " +
                        "the strategy or reducing per-bar work (the QuickJS watchdog is 5s/bar).",
                    symbol = symbol
                )
            }
            result.copy(scriptId = scriptId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Backtest failed for $symbol")
            consoleBus.error(
                ConsoleSource.BACKTEST,
                title = "Backtest crashed",
                message = "${e::class.simpleName}: ${e.message ?: ""}",
                symbol = symbol
            )
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
        slippagePct: Double, startDateMs: Long?, endDateMs: Long?,
        startingCapital: Double, applyFundingRate: Boolean
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
            data class PosState(val side: OrderSide, val entryPrice: Double, val qty: Double, val entryTime: Long, val entryBar: Int, val entryReason: String? = null)
            val positions = mutableMapOf<String, PosState>()
            val trades = mutableListOf<BacktestTrade>()
            val sigs = mutableListOf<SignalMarker>()
            var equity = startingCapital
            val equityCurve = mutableListOf(equity)
            val feePct = (makerFeePct + takerFeePct) / 2.0
            val customTotal = primaryCandles.size - 50

            fun closePos(sym: String, exitPrice: Double, exitTime: Long, barIndex: Int, reason: String = "Strategy close") {
                val pos = positions.remove(sym) ?: return
                val pnl = if (pos.side == OrderSide.BUY) (exitPrice - pos.entryPrice) * pos.qty
                          else (pos.entryPrice - exitPrice) * pos.qty
                val fee = (pos.entryPrice * pos.qty + exitPrice * pos.qty) * feePct
                equity += pnl - fee
                trades.add(BacktestTrade(
                    pos.entryTime, exitTime, pos.side, pos.entryPrice, exitPrice, pos.qty, pnl - fee, fee,
                    barsInTrade = barIndex - pos.entryBar,
                    entryReason = pos.entryReason,
                    exitReason = reason
                ))
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
                            if (positions[targetSym]?.side == OrderSide.SELL) closePos(targetSym, targetBar.close, targetBar.openTime, i, reason = "Reverse to LONG")
                            if (targetSym !in positions) {
                                val qty = (equity * sig.sizePct / 100.0) / targetBar.close
                                val entryReasonStr = sig.labelRaw?.takeIf { it.isNotBlank() }
                                    ?: if (sig.label == SignalLabel.PATTERN) "Pattern BUY" else "BUY signal"
                                positions[targetSym] = PosState(OrderSide.BUY, targetBar.close, qty, targetBar.openTime, i, entryReason = entryReasonStr)
                                val buyType = if (sig.label == SignalLabel.PATTERN) SignalType.PAT_BUY else SignalType.BUY
                                sigs.add(SignalMarker("", scriptId, targetSym, interval, targetBar.openTime, buyType, targetBar.close, label = sig.labelRaw, orderType = sig.orderType.name))
                            }
                        }
                        is StrategySignal.SELL -> {
                            if (positions[targetSym]?.side == OrderSide.BUY) closePos(targetSym, targetBar.close, targetBar.openTime, i, reason = "Reverse to SHORT")
                            if (targetSym !in positions) {
                                val qty = (equity * sig.sizePct / 100.0) / targetBar.close
                                val entryReasonStr = sig.labelRaw?.takeIf { it.isNotBlank() }
                                    ?: if (sig.label == SignalLabel.PATTERN) "Pattern SELL" else "SELL signal"
                                positions[targetSym] = PosState(OrderSide.SELL, targetBar.close, qty, targetBar.openTime, i, entryReason = entryReasonStr)
                                val sellType = if (sig.label == SignalLabel.PATTERN) SignalType.PAT_SELL else SignalType.SELL
                                sigs.add(SignalMarker("", scriptId, targetSym, interval, targetBar.openTime, sellType, targetBar.close, label = sig.labelRaw, orderType = sig.orderType.name))
                            }
                        }
                        is StrategySignal.CLOSE_IF_LONG -> {
                            if (positions[targetSym]?.side == OrderSide.BUY) closePos(targetSym, targetBar.close, targetBar.openTime, i, reason = "Strategy close long")
                        }
                        is StrategySignal.CLOSE_IF_SHORT -> {
                            if (positions[targetSym]?.side == OrderSide.SELL) closePos(targetSym, targetBar.close, targetBar.openTime, i, reason = "Strategy close short")
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
                closePos(sym, lastBar.close, lastBar.openTime, lastIdx, reason = "End of test")
            }

            // Compute metrics
            val totalPnl = equity - startingCapital
            val winners = trades.filter { it.pnl > 0 }
            val losers = trades.filter { it.pnl < 0 }
            val winRate = if (trades.isNotEmpty()) winners.size.toDouble() / trades.size * 100 else 0.0
            val maxDd = run { var peak = startingCapital; var dd = 0.0; for (e in equityCurve) { peak = maxOf(peak, e); dd = maxOf(dd, (peak - e) / peak * 100) }; dd }
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
            val returnPct = totalPnl / startingCapital

            BacktestResult(
                scriptId = scriptId, symbol = symbols.joinToString(","), timeframe = interval,
                startDate = primaryCandles.first().openTime, endDate = primaryCandles.last().openTime,
                totalTrades = trades.size, winRate = winRate, totalPnl = totalPnl,
                maxDrawdown = maxDd, sharpeRatio = sharpe, sortinoRatio = sortino, trades = trades,
                profitFactor = profitFactor, buyAndHoldReturn = buyHold,
                avgBarsInTrade = avgBarsInTrade, grossProfit = grossProfit, grossLoss = grossLoss,
                maxConsecutiveWins = maxConsW, maxConsecutiveLosses = maxConsL,
                equityCurve = equityCurve, startingCapital = startingCapital,
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

    /** Wilder ATR over [period] bars; returns absolute price ATR or null if insufficient data. */
    private fun computeAtrAbs(candles: List<Candle>, period: Int = 14): Double? {
        if (candles.size <= period) return null
        var atr = 0.0
        for (i in 1..period) {
            val c = candles[i]; val p = candles[i - 1]
            val tr = maxOf(c.high - c.low, kotlin.math.abs(c.high - p.close), kotlin.math.abs(c.low - p.close))
            atr += tr
        }
        atr /= period
        for (i in period + 1 until candles.size) {
            val c = candles[i]; val p = candles[i - 1]
            val tr = maxOf(c.high - c.low, kotlin.math.abs(c.high - p.close), kotlin.math.abs(c.low - p.close))
            atr = (atr * (period - 1) + tr) / period
        }
        return if (atr > 0) atr else null
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
