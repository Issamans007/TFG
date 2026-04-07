package com.tfg.engine

import android.content.Context
import com.tfg.data.local.dao.CandleDao
import com.tfg.data.local.dao.SignalMarkerDao
import com.tfg.data.local.entity.SignalMarkerEntity
import com.tfg.data.local.mapper.EntityMapper
import com.tfg.domain.model.*
import com.tfg.data.remote.websocket.WebSocketManager
import com.tfg.domain.repository.AuditRepository
import com.tfg.domain.repository.MarketRepository
import com.tfg.domain.repository.PortfolioRepository
import com.tfg.domain.repository.ScriptRepository
import com.tfg.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StrategyRunner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scriptRepository: ScriptRepository,
    private val settingsRepository: SettingsRepository,
    private val candleDao: CandleDao,
    private val signalMarkerDao: SignalMarkerDao,
    private val tradeExecutor: TradeExecutor,
    private val riskEngine: RiskEngine,
    private val auditRepository: AuditRepository,
    private val portfolioRepository: PortfolioRepository,
    private val marketRepository: MarketRepository,
    private val webSocketManager: WebSocketManager,
    private val scriptExecutor: ScriptExecutor
) {

    private companion object {
        const val PREFS_NAME = "tfg_strategy_runner"
        const val KEY_POSITION = "current_position"
        const val LIVE_FEE_PCT = 0.001 // 0.1% — matches backtester fee
        const val DEFAULT_LOOKBACK = 200
        const val MIN_BARS = 50
        const val MAX_CONSECUTIVE_FAILURES = 5 // B7: auto-deactivate after this many
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = com.google.gson.Gson()

    private var runnerJob: Job? = null
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    private val _lastSignal = MutableStateFlow<String>("IDLE")
    val lastSignal: StateFlow<String> = _lastSignal

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // ─── Position tracking — prevents duplicate entries ─────────
    private data class TrackedPosition(val side: OrderSide, val entryPrice: Double, val symbol: String, val quantity: Double, val entries: Int = 1)

    private var currentPosition: TrackedPosition?
        get() {
            val json = prefs.getString(KEY_POSITION, null) ?: return null
            return try { gson.fromJson(json, TrackedPosition::class.java) } catch (_: Exception) { null }
        }
        set(value) {
            if (value != null) {
                prefs.edit().putString(KEY_POSITION, gson.toJson(value)).apply()
            } else {
                prefs.edit().remove(KEY_POSITION).apply()
            }
        }

    private var evalContext = StrategyEvaluator.EvalContext()
    private var consecutiveFailures = 0

    @OptIn(ExperimentalCoroutinesApi::class)
    fun start() {
        if (runnerJob?.isActive == true) return

        runnerJob = scope.launch {
            scriptRepository.getActiveStrategy().flatMapLatest<_, Unit> { script ->
                flow {
                    if (script != null && script.isActive && script.activeSymbol != null) {
                        _isRunning.value = true
                        Timber.i("Strategy active: '${script.name}' on ${script.activeSymbol}")
                        auditRepository.log(AuditLog(
                            id = UUID.randomUUID().toString(),
                            action = AuditAction.CONFIG_CHANGED,
                            category = AuditCategory.SIGNAL,
                            details = "Strategy '${script.name}' activated on ${script.activeSymbol}",
                            symbol = script.activeSymbol,
                            userId = "system"
                        ))
                        runStrategyLoop(script)
                    } else {
                        if (_isRunning.value) {
                            auditRepository.log(AuditLog(
                                id = UUID.randomUUID().toString(),
                                action = AuditAction.CONFIG_CHANGED,
                                category = AuditCategory.SIGNAL,
                                details = "Strategy deactivated",
                                userId = "system"
                            ))
                        }
                        _isRunning.value = false
                        _lastSignal.value = "IDLE"
                        webSocketManager.disconnectAll()
                    }
                }
            }.collect()
        }
    }

    fun stop() {
        runnerJob?.cancel()
        runnerJob = null
        _isRunning.value = false
        _lastSignal.value = "IDLE"
        evalContext = StrategyEvaluator.EvalContext()
    }

    private suspend fun runStrategyLoop(script: Script) {
        val symbol = script.activeSymbol ?: return
        val templateId = script.strategyTemplateId
        // Read timeframe from script params instead of hardcoding
        val interval = script.params["INTERVAL"] ?: "1h"
        // Fresh context per strategy activation
        evalContext = StrategyEvaluator.EvalContext()

        // Only clear persisted position if switching to a different symbol;
        // on service restart for the same symbol, preserve it to avoid duplicates
        val restoredPos = currentPosition
        if (restoredPos != null && restoredPos.symbol != symbol) {
            currentPosition = null
        }

        // Determine once whether the user edited the template code
        val isCustomCode = script.code.isNotBlank() && run {
            val templateCode = try {
                StrategyTemplates.getAll()
                    .firstOrNull { it.id.name == templateId }?.code ?: ""
            } catch (_: Exception) { "" }
            script.code.trimIndent() != templateCode.trimIndent()
        }

        // B1: Determine higher timeframes to fetch
        val htfIntervals = getHigherTimeframes(interval)
        var htfCache: Map<String, List<Candle>> = emptyMap()
        var lastHtfFetchMs = 0L
        val htfRefreshMs = intervalToMs(interval) // Refresh HTF candles once per base bar period

        // Subscribe to the Binance kline WebSocket for this symbol/interval
        webSocketManager.connectBinanceKline(symbol, interval)

        // React to each closed bar via klineFlow (isFinal = true)
        webSocketManager.klineFlow
            .filter { it.symbol.equals(symbol, ignoreCase = true) && it.k.isFinal }
            .collect { kline ->
                try {
                    val lookback = script.params["LOOKBACK"]?.toIntOrNull()?.coerceIn(MIN_BARS, 1000) ?: DEFAULT_LOOKBACK
                    val maxPyramid = script.params["MAX_PYRAMID"]?.toIntOrNull()?.coerceIn(1, 20) ?: 1
                    val candles = candleDao.getCandles(symbol, interval, lookback)
                        .first()
                        .map { EntityMapper.run { it.toDomain() } }

                    if (candles.size >= MIN_BARS) {
                        // B6: Update volatility (14-bar ATR as % of price)
                        val atrPct = calculateAtrPercent(candles, 14)
                        riskEngine.updateVolatility(atrPct)

                        // Build account context for JS scripts
                        val pos = currentPosition
                        val lastClose = candles.last().close
                        // Single portfolio fetch per bar — reused for ScriptAccount + trade execution
                        val portfolio = try {
                            portfolioRepository.refreshPortfolio()
                            portfolioRepository.getPortfolio().first()
                        } catch (_: Exception) { null }
                        val acctContext = ScriptAccount(
                            equity = portfolio?.totalBalance ?: 0.0,
                            balance = portfolio?.totalBalance ?: 0.0,
                            positionSide = pos?.side?.name,
                            positionPnl = if (pos != null) {
                                if (pos.side == OrderSide.BUY) (lastClose - pos.entryPrice) * pos.quantity
                                else (pos.entryPrice - lastClose) * pos.quantity
                            } else 0.0,
                            positionSize = pos?.quantity ?: 0.0,
                            positionEntry = pos?.entryPrice ?: 0.0
                        )

                        val signal = if (isCustomCode) {
                            // B1: Refresh HTF candles periodically
                            val now = System.currentTimeMillis()
                            if (htfIntervals.isNotEmpty() && now - lastHtfFetchMs > htfRefreshMs) {
                                htfCache = fetchHtfCandles(symbol, htfIntervals)
                                lastHtfFetchMs = now
                            }
                            scriptExecutor.evaluate(script.code, candles, script.params, acctContext, htfCache)
                        } else {
                            StrategyEvaluator.evaluate(templateId, candles, script.params, evalContext)
                        }
                        _lastSignal.value = signal.javaClass.simpleName
                        consecutiveFailures = 0 // Reset on successful evaluation

                        // Store signal marker for chart overlay
                        val lastCandle = candles.last()
                        val signalType = when (signal) {
                            is StrategySignal.BUY -> SignalType.BUY
                            is StrategySignal.SELL -> SignalType.SELL
                            is StrategySignal.CLOSE_IF_LONG, is StrategySignal.CLOSE_IF_SHORT -> SignalType.CLOSE
                            else -> null
                        }
                        if (signalType != null) {
                            signalMarkerDao.insertAll(listOf(
                                SignalMarkerEntity(
                                    id = "${symbol}_${lastCandle.openTime}_${signalType.name}",
                                    scriptId = script.id, symbol = symbol, interval = interval,
                                    openTime = lastCandle.openTime, signalType = signalType.name,
                                    price = lastCandle.close
                                )
                            ))
                        }

                        when (signal) {
                            is StrategySignal.BUY -> {
                                // Close opposite position if any
                                if (currentPosition != null && currentPosition?.side == OrderSide.SELL) {
                                    closePosition(symbol, OrderSide.SELL, currentPosition!!.quantity, script)
                                    currentPosition = null
                                }
                                val existing = currentPosition
                                if (existing != null && existing.side == OrderSide.BUY && existing.entries >= maxPyramid) {
                                    Timber.d("Pyramid limit reached ($maxPyramid) for BUY on $symbol, skipping")
                                } else {
                                    val qty = executeBuy(symbol, signal, script, lastCandle.close, portfolio)
                                    currentPosition = if (existing != null && existing.side == OrderSide.BUY) {
                                        val totalQty = existing.quantity + qty
                                        val avgEntry = (existing.entryPrice * existing.quantity + lastCandle.close * qty) / totalQty
                                        TrackedPosition(OrderSide.BUY, avgEntry, symbol, totalQty, existing.entries + 1)
                                    } else {
                                        TrackedPosition(OrderSide.BUY, lastCandle.close, symbol, qty)
                                    }
                                }
                            }
                            is StrategySignal.SELL -> {
                                // Close opposite position if any
                                if (currentPosition != null && currentPosition?.side == OrderSide.BUY) {
                                    closePosition(symbol, OrderSide.BUY, currentPosition!!.quantity, script)
                                    currentPosition = null
                                }
                                val existing = currentPosition
                                if (existing != null && existing.side == OrderSide.SELL && existing.entries >= maxPyramid) {
                                    Timber.d("Pyramid limit reached ($maxPyramid) for SELL on $symbol, skipping")
                                } else {
                                    val qty = executeSell(symbol, signal, script, lastCandle.close, portfolio)
                                    currentPosition = if (existing != null && existing.side == OrderSide.SELL) {
                                        val totalQty = existing.quantity + qty
                                        val avgEntry = (existing.entryPrice * existing.quantity + lastCandle.close * qty) / totalQty
                                        TrackedPosition(OrderSide.SELL, avgEntry, symbol, totalQty, existing.entries + 1)
                                    } else {
                                        TrackedPosition(OrderSide.SELL, lastCandle.close, symbol, qty)
                                    }
                                }
                            }
                            is StrategySignal.CLOSE_IF_LONG -> {
                                if (currentPosition?.side == OrderSide.BUY) {
                                    closePosition(symbol, OrderSide.BUY, currentPosition!!.quantity, script)
                                    currentPosition = null
                                }
                            }
                            is StrategySignal.CLOSE_IF_SHORT -> {
                                if (currentPosition?.side == OrderSide.SELL) {
                                    closePosition(symbol, OrderSide.SELL, currentPosition!!.quantity, script)
                                    currentPosition = null
                                }
                            }
                            is StrategySignal.HOLD -> { /* do nothing */ }
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    consecutiveFailures++
                    Timber.e(e, "Strategy evaluation error for ${script.name} (failure $consecutiveFailures/$MAX_CONSECUTIVE_FAILURES)")
                    if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                        Timber.w("Auto-deactivating strategy '${script.name}' after $consecutiveFailures consecutive failures")
                        auditRepository.log(AuditLog(
                            id = UUID.randomUUID().toString(),
                            action = AuditAction.CONFIG_CHANGED,
                            category = AuditCategory.SIGNAL,
                            details = "Strategy '${script.name}' auto-deactivated after $consecutiveFailures consecutive failures: ${e.message}",
                            symbol = symbol,
                            userId = "system"
                        ))
                        scriptRepository.deactivateStrategy(script.id)
                        throw CancellationException("Strategy auto-deactivated after $consecutiveFailures consecutive failures")
                    }
                }
            }
    }

    // ─── Trade execution helpers ────────────────────────────────────

    private suspend fun executeBuy(symbol: String, signal: StrategySignal.BUY, script: Script, currentPrice: Double = 0.0, cachedPortfolio: Portfolio? = null): Double {
        val isPaper = settingsRepository.isPaperTrading()
        // Reuse portfolio from caller; only fetch if not provided
        val p = cachedPortfolio ?: run {
            portfolioRepository.refreshPortfolio()
            portfolioRepository.getPortfolio().first()
        }
        riskEngine.updatePortfolio(p)
        // Calculate quantity from sizePct and real portfolio balance (fee-adjusted)
        val qty = if (currentPrice > 0 && signal.sizePct > 0) {
            val notional = p.totalBalance * signal.sizePct / 100.0
            (notional / currentPrice) * (1.0 - LIVE_FEE_PCT)
        } else 0.001
        val order = Order(
            id = UUID.randomUUID().toString(),
            symbol = symbol,
            side = OrderSide.BUY,
            type = OrderType.MARKET,
            executionMode = ExecutionMode.SCRIPT,
            quantity = qty,
            isPaperTrade = isPaper,
            takeProfits = if (signal.takeProfitLevels.isNotEmpty() && currentPrice > 0) {
                signal.takeProfitLevels.map { lvl ->
                    TakeProfit(UUID.randomUUID().toString(),
                        currentPrice * (1.0 + lvl.pct / 100.0), lvl.quantityPct)
                }
            } else {
                signal.takeProfitPct?.let { tp ->
                    if (currentPrice > 0) listOf(TakeProfit(UUID.randomUUID().toString(),
                        currentPrice * (1.0 + tp / 100.0), 100.0)) else emptyList()
                } ?: emptyList()
            },
            stopLosses = if (signal.stopLossLevels.isNotEmpty() && currentPrice > 0) {
                signal.stopLossLevels.map { lvl ->
                    StopLoss(UUID.randomUUID().toString(),
                        currentPrice * (1.0 - lvl.pct / 100.0), lvl.quantityPct)
                }
            } else {
                signal.stopLossPct?.let { sl ->
                    if (currentPrice > 0) listOf(StopLoss(UUID.randomUUID().toString(),
                        currentPrice * (1.0 - sl / 100.0), 100.0)) else emptyList()
                } ?: emptyList()
            }
        )
        Timber.i("Strategy '${script.name}' BUY signal on $symbol (size=${signal.sizePct}%)${if (isPaper) " [PAPER]" else ""}")
        auditRepository.log(AuditLog(
            id = UUID.randomUUID().toString(),
            action = AuditAction.ORDER_PLACED,
            category = AuditCategory.TRADING,
            details = "Strategy '${script.name}' BUY (size=${signal.sizePct}%)${if (isPaper) " [PAPER]" else ""}",
            symbol = symbol,
            userId = "system"
        ))
        tradeExecutor.executeOrder(order)
        return qty
    }

    private suspend fun executeSell(symbol: String, signal: StrategySignal.SELL, script: Script, currentPrice: Double = 0.0, cachedPortfolio: Portfolio? = null): Double {
        val isPaper = settingsRepository.isPaperTrading()
        // Reuse portfolio from caller; only fetch if not provided
        val p = cachedPortfolio ?: run {
            portfolioRepository.refreshPortfolio()
            portfolioRepository.getPortfolio().first()
        }
        riskEngine.updatePortfolio(p)
        val qty = if (currentPrice > 0 && signal.sizePct > 0) {
            val notional = p.totalBalance * signal.sizePct / 100.0
            (notional / currentPrice) * (1.0 - LIVE_FEE_PCT)
        } else 0.001
        val order = Order(
            id = UUID.randomUUID().toString(),
            symbol = symbol,
            side = OrderSide.SELL,
            type = OrderType.MARKET,
            executionMode = ExecutionMode.SCRIPT,
            quantity = qty,
            isPaperTrade = isPaper,
            takeProfits = if (signal.takeProfitLevels.isNotEmpty() && currentPrice > 0) {
                signal.takeProfitLevels.map { lvl ->
                    TakeProfit(UUID.randomUUID().toString(),
                        currentPrice * (1.0 - lvl.pct / 100.0), lvl.quantityPct)
                }
            } else {
                signal.takeProfitPct?.let { tp ->
                    if (currentPrice > 0) listOf(TakeProfit(UUID.randomUUID().toString(),
                        currentPrice * (1.0 - tp / 100.0), 100.0)) else emptyList()
                } ?: emptyList()
            },
            stopLosses = if (signal.stopLossLevels.isNotEmpty() && currentPrice > 0) {
                signal.stopLossLevels.map { lvl ->
                    StopLoss(UUID.randomUUID().toString(),
                        currentPrice * (1.0 + lvl.pct / 100.0), lvl.quantityPct)
                }
            } else {
                signal.stopLossPct?.let { sl ->
                    if (currentPrice > 0) listOf(StopLoss(UUID.randomUUID().toString(),
                        currentPrice * (1.0 + sl / 100.0), 100.0)) else emptyList()
                } ?: emptyList()
            }
        )
        Timber.i("Strategy '${script.name}' SELL signal on $symbol (size=${signal.sizePct}%)${if (isPaper) " [PAPER]" else ""}")
        auditRepository.log(AuditLog(
            id = UUID.randomUUID().toString(),
            action = AuditAction.ORDER_PLACED,
            category = AuditCategory.TRADING,
            details = "Strategy '${script.name}' SELL (size=${signal.sizePct}%)${if (isPaper) " [PAPER]" else ""}",
            symbol = symbol,
            userId = "system"
        ))
        tradeExecutor.executeOrder(order)
        return qty
    }

    private suspend fun closePosition(symbol: String, originalSide: OrderSide, quantity: Double, script: Script) {
        val isPaper = settingsRepository.isPaperTrading()
        val closeSide = if (originalSide == OrderSide.BUY) OrderSide.SELL else OrderSide.BUY
        val order = Order(
            id = UUID.randomUUID().toString(),
            symbol = symbol,
            side = closeSide,
            type = OrderType.MARKET,
            executionMode = ExecutionMode.SCRIPT,
            quantity = quantity,
            isPaperTrade = isPaper
        )
        Timber.i("Strategy '${script.name}' CLOSE signal on $symbol${if (isPaper) " [PAPER]" else ""}")
        auditRepository.log(AuditLog(
            id = UUID.randomUUID().toString(),
            action = AuditAction.ORDER_PLACED,
            category = AuditCategory.TRADING,
            details = "Strategy '${script.name}' CLOSE position${if (isPaper) " [PAPER]" else ""}",
            symbol = symbol,
            userId = "system"
        ))
        tradeExecutor.executeOrder(order)
    }

    /** Calculate ATR as a percentage of last close price over [period] bars. */
    private fun calculateAtrPercent(candles: List<Candle>, period: Int): Double {
        if (candles.size < period + 1) return 0.0
        val recent = candles.takeLast(period + 1)
        var atrSum = 0.0
        for (i in 1 until recent.size) {
            val tr = maxOf(
                recent[i].high - recent[i].low,
                kotlin.math.abs(recent[i].high - recent[i - 1].close),
                kotlin.math.abs(recent[i].low - recent[i - 1].close)
            )
            atrSum += tr
        }
        val atr = atrSum / period
        val lastClose = candles.last().close
        return if (lastClose > 0) atr / lastClose * 100.0 else 0.0
    }

    // ─── B1: Multi-timeframe helpers ────────────────────────────────

    /** Return the higher timeframes worth fetching for a given base interval. */
    private fun getHigherTimeframes(base: String): List<String> = when (base) {
        "1m"  -> listOf("5m", "15m", "1h")
        "3m"  -> listOf("15m", "1h")
        "5m"  -> listOf("15m", "1h", "4h")
        "15m" -> listOf("1h", "4h", "1d")
        "30m" -> listOf("1h", "4h", "1d")
        "1h"  -> listOf("4h", "1d")
        "2h"  -> listOf("4h", "1d")
        "4h"  -> listOf("1d", "1w")
        "1d"  -> listOf("1w")
        else  -> emptyList()
    }

    /** Fetch candles for each higher timeframe from the exchange. */
    private suspend fun fetchHtfCandles(symbol: String, intervals: List<String>): Map<String, List<Candle>> {
        val result = mutableMapOf<String, List<Candle>>()
        for (htfInterval in intervals) {
            try {
                val candles = marketRepository.getCandles(symbol, htfInterval, 200).first()
                if (candles.isNotEmpty()) {
                    result[htfInterval] = candles
                }
            } catch (e: Exception) {
                Timber.w(e, "Failed to fetch HTF candles for $symbol/$htfInterval")
            }
        }
        return result
    }

    /** Convert interval string to approximate milliseconds. */
    private fun intervalToMs(interval: String): Long = when (interval) {
        "1m" -> 60_000L; "3m" -> 180_000L; "5m" -> 300_000L
        "15m" -> 900_000L; "30m" -> 1_800_000L; "1h" -> 3_600_000L
        "2h" -> 7_200_000L; "4h" -> 14_400_000L; "6h" -> 21_600_000L
        "8h" -> 28_800_000L; "12h" -> 43_200_000L; "1d" -> 86_400_000L
        "3d" -> 259_200_000L; "1w" -> 604_800_000L
        else -> 3_600_000L
    }
}
