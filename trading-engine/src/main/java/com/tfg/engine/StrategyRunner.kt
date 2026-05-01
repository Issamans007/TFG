package com.tfg.engine

import android.content.Context
import com.tfg.data.local.dao.CandleDao
import com.tfg.data.local.dao.SignalMarkerDao
import com.tfg.data.local.entity.SignalMarkerEntity
import com.tfg.data.local.mapper.EntityMapper
import com.tfg.domain.model.*
import com.tfg.data.remote.websocket.WebSocketManager
import com.tfg.domain.repository.AuditRepository
import com.tfg.domain.repository.FeeRepository
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
    private val scriptExecutor: ScriptExecutor,
    private val feeRepository: FeeRepository
) {

    private companion object {
        const val PREFS_NAME = "tfg_strategy_runner"
        const val KEY_POSITION = "current_position"
        // Fallback when FeeConfig load fails. Effective fee always read from FeeRepository.
        const val LIVE_FEE_PCT = 0.001
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

    private val _dashboardJson = MutableStateFlow<String?>(null)
    val dashboardJson: StateFlow<String?> = _dashboardJson

    private val _plotDataJson = MutableStateFlow<String?>(null)
    val plotDataJson: StateFlow<String?> = _plotDataJson

    // Recreated on every start() so children launched during a previous run
    // can be fully cancelled by stop(). Without this, a stop() that only
    // cancels runnerJob leaves any sibling launches inside the loop
    // (price feeds, audit logs, signal flows) running until the next start.
    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

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

    private var consecutiveFailures = 0

    @OptIn(ExperimentalCoroutinesApi::class)
    fun start() {
        if (runnerJob?.isActive == true) return
        // Fresh scope each run so any orphaned children from a prior session
        // (e.g. a stop() that only cancelled runnerJob) are guaranteed gone.
        if (!scope.isActive) {
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        }

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
        // Cancel everything launched off `scope` (price feeds, audit writes,
        // signal collectors). start() will install a fresh scope.
        scope.cancel()
        _isRunning.value = false
        _lastSignal.value = "IDLE"
    }

    private suspend fun runStrategyLoop(script: Script) {
        val symbol = script.activeSymbol ?: return
        // Read timeframe from script params instead of hardcoding
        val interval = script.params["INTERVAL"] ?: "1h"

        // Only clear persisted position if switching to a different symbol;
        // on service restart for the same symbol, preserve it to avoid duplicates
        val restoredPos = currentPosition
        if (restoredPos != null && restoredPos.symbol != symbol) {
            currentPosition = null
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

                        // B1: Refresh HTF candles periodically
                        val now = System.currentTimeMillis()
                        if (htfIntervals.isNotEmpty() && now - lastHtfFetchMs > htfRefreshMs) {
                            htfCache = fetchHtfCandles(symbol, htfIntervals)
                            lastHtfFetchMs = now
                        }
                        // All strategies execute via JS engine
                        val rawSignal = scriptExecutor.evaluate(script.code, candles, script.params, acctContext, htfCache)
                        _lastSignal.value = rawSignal.javaClass.simpleName
                        _dashboardJson.value = scriptExecutor.getLastDashboard()
                        _plotDataJson.value = scriptExecutor.getLastPlotData()
                        consecutiveFailures = 0 // Reset on successful evaluation

                        // Apply user TradingPlan: layer SL/TP/leverage/session-hours/short-allowed
                        // onto raw signal so live + backtest behave identically.
                        val plan = com.tfg.domain.model.TradingPlan.fromScript(script)
                        val lastCandle = candles.last()
                        val atrAbs = computeAtrAbs(candles, period = 14)
                        val signal = com.tfg.domain.model.PlanApplier.apply(
                            rawSignal, plan, atrAbs, lastCandle.close, lastCandle.openTime
                        )
                        _lastSignal.value = signal.javaClass.simpleName

                        // Store signal marker for chart overlay
                        val signalType = when (signal) {
                            is StrategySignal.BUY -> when (signal.label) {
                                SignalLabel.PATTERN -> SignalType.PAT_BUY
                                else -> SignalType.BUY
                            }
                            is StrategySignal.SELL -> when (signal.label) {
                                SignalLabel.PATTERN -> SignalType.PAT_SELL
                                else -> SignalType.SELL
                            }
                            is StrategySignal.CLOSE_IF_LONG, is StrategySignal.CLOSE_IF_SHORT -> SignalType.CLOSE
                            else -> null
                        }
                        val markerLabel = when (signal) {
                            is StrategySignal.BUY -> signal.labelRaw
                            is StrategySignal.SELL -> signal.labelRaw
                            else -> ""
                        }
                        val markerOrderType = when (signal) {
                            is StrategySignal.BUY -> signal.orderType.name
                            is StrategySignal.SELL -> signal.orderType.name
                            else -> "MARKET"
                        }
                        if (signalType != null) {
                            signalMarkerDao.insertAll(listOf(
                                SignalMarkerEntity(
                                    id = "${symbol}_${lastCandle.openTime}_${signalType.name}",
                                    scriptId = script.id, symbol = symbol, interval = interval,
                                    openTime = lastCandle.openTime, signalType = signalType.name,
                                    price = lastCandle.close,
                                    label = markerLabel,
                                    orderType = markerOrderType
                                )
                            ))
                        }

                        when (signal) {
                            is StrategySignal.BUY -> {
                                // Close opposite position if any
                                if (currentPosition != null && currentPosition?.side == OrderSide.SELL) {
                                    val qty = currentPosition?.quantity
                                    if (qty != null) closePosition(symbol, OrderSide.SELL, qty, script)
                                    else Timber.w("Position state cleared mid-flow on $symbol; skipping close")
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
                                    val qty = currentPosition?.quantity
                                    if (qty != null) closePosition(symbol, OrderSide.BUY, qty, script)
                                    else Timber.w("Position state cleared mid-flow on $symbol; skipping close")
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
                                val pos = currentPosition
                                if (pos?.side == OrderSide.BUY) {
                                    closePosition(symbol, OrderSide.BUY, pos.quantity, script)
                                    currentPosition = null
                                }
                            }
                            is StrategySignal.CLOSE_IF_SHORT -> {
                                val pos = currentPosition
                                if (pos?.side == OrderSide.SELL) {
                                    closePosition(symbol, OrderSide.SELL, pos.quantity, script)
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
                        notifyStrategyDeactivated(script.name, consecutiveFailures, e.message)
                        scriptRepository.deactivateStrategy(script.id)
                        throw CancellationException("Strategy auto-deactivated after $consecutiveFailures consecutive failures")
                    }
                }
            }
    }

    /**
     * Posts a high-priority notification when a strategy is auto-deactivated
     * after repeated evaluation failures, so the user is aware their bot is
     * no longer trading instead of silently going idle.
     */
    private fun notifyStrategyDeactivated(strategyName: String, failures: Int, lastError: String?) {
        try {
            val channelId = "tfg_strategy_alerts"
            val nm = context.getSystemService(android.app.NotificationManager::class.java)
            if (nm.getNotificationChannel(channelId) == null) {
                nm.createNotificationChannel(
                    android.app.NotificationChannel(
                        channelId,
                        "Strategy Alerts",
                        android.app.NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Alerts when a strategy is paused or auto-deactivated"
                    }
                )
            }
            val notif = androidx.core.app.NotificationCompat.Builder(context, channelId)
                .setContentTitle("Strategy auto-deactivated")
                .setContentText("'$strategyName' paused after $failures failures")
                .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(
                    "Strategy '$strategyName' has been auto-deactivated after $failures " +
                    "consecutive evaluation failures.\n\n" +
                    "Last error: ${lastError ?: "unknown"}\n\n" +
                    "The bot is no longer trading this strategy. Review the script and re-enable it manually."
                ))
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            nm.notify(2_000 + strategyName.hashCode().and(0xFFFF), notif)
        } catch (e: Exception) {
            Timber.w(e, "Failed to post strategy-deactivation notification")
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
        // Resolve futures context: signal overrides, else fall back to user defaults
        val mkt = signal.marketType ?: if (settingsRepository.isFuturesEnabled()) MarketType.FUTURES_USDM else MarketType.SPOT
        val lev = (signal.leverage ?: settingsRepository.getDefaultLeverage().coerceAtLeast(1)).coerceIn(1, 125)
        val mgn = signal.marginType ?: settingsRepository.getDefaultMarginType()
        val isFutures = mkt == MarketType.FUTURES_USDM
        // Calculate quantity from sizePct and real portfolio balance (fee-adjusted)
        val feeCfg = runCatching { feeRepository.getFeeConfig().first() }.getOrNull()
        val feePct = feeCfg?.let {
            if (isFutures) it.effectiveFuturesTakerRate() else it.effectiveTakerRate()
        } ?: LIVE_FEE_PCT
        val qty = if (currentPrice > 0 && signal.sizePct > 0) {
            val notional = p.totalBalance * signal.sizePct / 100.0 * (if (isFutures) lev else 1)
            (notional / currentPrice) * (1.0 - feePct)
        } else 0.001
        val mappedOrderType = when (signal.orderType) {
            SignalOrderType.MARKET -> OrderType.MARKET
            SignalOrderType.LIMIT -> OrderType.LIMIT
            SignalOrderType.STOP_LIMIT -> OrderType.STOP_LIMIT
            SignalOrderType.TRAILING_STOP -> OrderType.TRAILING_STOP
        }
        val order = Order(
            id = UUID.randomUUID().toString(),
            symbol = symbol,
            side = OrderSide.BUY,
            type = mappedOrderType,
            executionMode = ExecutionMode.SCRIPT,
            quantity = qty,
            price = if (mappedOrderType == OrderType.LIMIT) (signal.limitPrice ?: currentPrice) else null,
            stopPrice = if (mappedOrderType == OrderType.STOP_LIMIT) (signal.limitPrice ?: currentPrice) else null,
            isPaperTrade = isPaper,
            marketType = mkt,
            leverage = if (isFutures) lev else 1,
            marginType = mgn,
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
        val mkt = signal.marketType ?: if (settingsRepository.isFuturesEnabled()) MarketType.FUTURES_USDM else MarketType.SPOT
        val lev = (signal.leverage ?: settingsRepository.getDefaultLeverage().coerceAtLeast(1)).coerceIn(1, 125)
        val mgn = signal.marginType ?: settingsRepository.getDefaultMarginType()
        val isFutures = mkt == MarketType.FUTURES_USDM
        val feeCfg = runCatching { feeRepository.getFeeConfig().first() }.getOrNull()
        val feePct = feeCfg?.let {
            if (isFutures) it.effectiveFuturesTakerRate() else it.effectiveTakerRate()
        } ?: LIVE_FEE_PCT
        val qty = if (currentPrice > 0 && signal.sizePct > 0) {
            val notional = p.totalBalance * signal.sizePct / 100.0 * (if (isFutures) lev else 1)
            (notional / currentPrice) * (1.0 - feePct)
        } else 0.001
        val mappedOrderType = when (signal.orderType) {
            SignalOrderType.MARKET -> OrderType.MARKET
            SignalOrderType.LIMIT -> OrderType.LIMIT
            SignalOrderType.STOP_LIMIT -> OrderType.STOP_LIMIT
            SignalOrderType.TRAILING_STOP -> OrderType.TRAILING_STOP
        }
        val order = Order(
            id = UUID.randomUUID().toString(),
            symbol = symbol,
            side = OrderSide.SELL,
            type = mappedOrderType,
            executionMode = ExecutionMode.SCRIPT,
            quantity = qty,
            price = if (mappedOrderType == OrderType.LIMIT) (signal.limitPrice ?: currentPrice) else null,
            stopPrice = if (mappedOrderType == OrderType.STOP_LIMIT) (signal.limitPrice ?: currentPrice) else null,
            isPaperTrade = isPaper,
            marketType = mkt,
            leverage = if (isFutures) lev else 1,
            marginType = mgn,
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

    /** Wilder ATR over [period] bars; returns absolute price ATR or null if insufficient data. */
    private fun computeAtrAbs(candles: List<com.tfg.domain.model.Candle>, period: Int = 14): Double? {
        if (candles.size <= period) return null
        var atr = 0.0
        // Seed with simple TR average over first [period] bars
        for (i in 1..period) {
            val c = candles[i]; val p = candles[i - 1]
            val tr = maxOf(c.high - c.low, kotlin.math.abs(c.high - p.close), kotlin.math.abs(c.low - p.close))
            atr += tr
        }
        atr /= period
        // Wilder smoothing through remaining bars
        for (i in period + 1 until candles.size) {
            val c = candles[i]; val p = candles[i - 1]
            val tr = maxOf(c.high - c.low, kotlin.math.abs(c.high - p.close), kotlin.math.abs(c.low - p.close))
            atr = (atr * (period - 1) + tr) / period
        }
        return if (atr > 0) atr else null
    }
}
