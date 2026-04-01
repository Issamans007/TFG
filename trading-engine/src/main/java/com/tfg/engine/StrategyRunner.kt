package com.tfg.engine

import android.content.Context
import com.tfg.data.local.dao.CandleDao
import com.tfg.data.local.dao.SignalMarkerDao
import com.tfg.data.local.entity.SignalMarkerEntity
import com.tfg.data.local.mapper.EntityMapper
import com.tfg.domain.model.*
import com.tfg.data.remote.websocket.WebSocketManager
import com.tfg.domain.repository.AuditRepository
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
    private val webSocketManager: WebSocketManager,
    private val scriptExecutor: ScriptExecutor
) {

    private companion object {
        const val PREFS_NAME = "tfg_strategy_runner"
        const val KEY_POSITION = "current_position"
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
    private data class TrackedPosition(val side: OrderSide, val entryPrice: Double, val symbol: String, val quantity: Double)

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

        // Subscribe to the Binance kline WebSocket for this symbol/interval
        webSocketManager.connectBinanceKline(symbol, interval)

        // React to each closed bar via klineFlow (isFinal = true)
        webSocketManager.klineFlow
            .filter { it.symbol.equals(symbol, ignoreCase = true) && it.k.isFinal }
            .collect { kline ->
                try {
                    val candles = candleDao.getCandles(symbol, interval, 200)
                        .first()
                        .map { EntityMapper.run { it.toDomain() } }

                    if (candles.size >= 50) {
                        val signal = if (isCustomCode) {
                            scriptExecutor.evaluate(script.code, candles, script.params)
                        } else {
                            StrategyEvaluator.evaluate(templateId, candles, script.params, evalContext)
                        }
                        _lastSignal.value = signal.javaClass.simpleName

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
                                if (currentPosition == null || currentPosition?.side != OrderSide.BUY) {
                                    if (currentPosition != null && currentPosition?.side == OrderSide.SELL) {
                                        closePosition(symbol, OrderSide.SELL, currentPosition!!.quantity, script)
                                    }
                                    val qty = executeBuy(symbol, signal, script, lastCandle.close)
                                    currentPosition = TrackedPosition(OrderSide.BUY, lastCandle.close, symbol, qty)
                                }
                            }
                            is StrategySignal.SELL -> {
                                if (currentPosition == null || currentPosition?.side != OrderSide.SELL) {
                                    if (currentPosition != null && currentPosition?.side == OrderSide.BUY) {
                                        closePosition(symbol, OrderSide.BUY, currentPosition!!.quantity, script)
                                    }
                                    val qty = executeSell(symbol, signal, script, lastCandle.close)
                                    currentPosition = TrackedPosition(OrderSide.SELL, lastCandle.close, symbol, qty)
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
                    Timber.e(e, "Strategy evaluation error for ${script.name}")
                }
            }
    }

    // ─── Trade execution helpers ────────────────────────────────────

    private suspend fun executeBuy(symbol: String, signal: StrategySignal.BUY, script: Script, currentPrice: Double = 0.0): Double {
        val isPaper = settingsRepository.isPaperTrading()
        // Fetch real portfolio and feed it to the risk engine
        portfolioRepository.refreshPortfolio()
        val portfolio = portfolioRepository.getPortfolio().first()
        riskEngine.updatePortfolio(portfolio)
        // Calculate quantity from sizePct and real portfolio balance
        val qty = if (currentPrice > 0 && signal.sizePct > 0) {
            val notional = portfolio.totalBalance * signal.sizePct / 100.0
            notional / currentPrice
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

    private suspend fun executeSell(symbol: String, signal: StrategySignal.SELL, script: Script, currentPrice: Double = 0.0): Double {
        val isPaper = settingsRepository.isPaperTrading()
        // Fetch real portfolio and feed it to the risk engine
        portfolioRepository.refreshPortfolio()
        val portfolio = portfolioRepository.getPortfolio().first()
        riskEngine.updatePortfolio(portfolio)
        val qty = if (currentPrice > 0 && signal.sizePct > 0) {
            val notional = portfolio.totalBalance * signal.sizePct / 100.0
            notional / currentPrice
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
}
