package com.tfg.engine

import android.content.Context
import com.tfg.data.local.dao.OrderDao
import com.tfg.data.local.entity.OfflineQueueEntity
import com.tfg.data.local.mapper.EntityMapper.toEntity
import com.tfg.data.remote.api.BinanceApi
import com.tfg.domain.model.*
import com.tfg.domain.repository.TradingRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TradeExecutor @Inject constructor(
    private val tradingRepository: TradingRepository,
    private val orderDao: OrderDao,
    private val riskEngine: RiskEngine,
    private val binanceApi: BinanceApi,
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("trade_executor", Context.MODE_PRIVATE)

    /** Peak prices since entry, keyed by order ID — used for trailing stop high-water mark */
    private val peakPrices = java.util.concurrent.ConcurrentHashMap<String, Double>().also { map ->
        prefs.getStringSet(KEY_PEAK_PRICES, null)?.forEach { entry ->
            val parts = entry.split("=", limit = 2)
            if (parts.size == 2) parts[1].toDoubleOrNull()?.let { map[parts[0]] = it }
        }
    }

    /** Track which individual TP levels have already fired, keyed by "orderId:tpId" */
    private val executedTpLevels: MutableSet<String> = java.util.Collections.synchronizedSet(
        (prefs.getStringSet(KEY_TP_LEVELS, null) ?: emptySet()).toMutableSet()
    )

    /** Track which individual SL levels have already fired, keyed by "orderId:slId" */
    private val executedSlLevels: MutableSet<String> = java.util.Collections.synchronizedSet(
        (prefs.getStringSet(KEY_SL_LEVELS, null) ?: emptySet()).toMutableSet()
    )

    /** Orders where breakeven SL is active (SL moved to entry after N TPs hit) */
    private val breakevenActive: MutableSet<String> = java.util.Collections.synchronizedSet(
        (prefs.getStringSet(KEY_BREAKEVEN, null) ?: emptySet()).toMutableSet()
    )

    companion object {
        private const val KEY_PEAK_PRICES = "peak_prices"
        private const val KEY_TP_LEVELS = "executed_tp_levels"
        private const val KEY_SL_LEVELS = "executed_sl_levels"
        private const val KEY_BREAKEVEN = "breakeven_active"
        private const val TICKER_CACHE_TTL_MS = 500L // 500ms — tight enough for 1s SL/TP loop
        private const val FILL_POLL_INTERVAL_MS = 500L
        private const val FILL_POLL_MAX_ATTEMPTS = 10 // 5s total max wait
    }

    /** Per-symbol price cache to avoid N+1 API calls in the 1-second monitoring loop */
    private data class CachedTicker(val price: String, val timestamp: Long)
    private val tickerCache = java.util.concurrent.ConcurrentHashMap<String, CachedTicker>()

    private suspend fun getCachedTickerPrice(symbol: String): String? {
        val cached = tickerCache[symbol]
        if (cached != null && System.currentTimeMillis() - cached.timestamp < TICKER_CACHE_TTL_MS) {
            return cached.price
        }
        return try {
            val ticker = binanceApi.get24hrTicker(symbol)
            tickerCache[symbol] = CachedTicker(ticker.lastPrice, System.currentTimeMillis())
            ticker.lastPrice
        } catch (e: Exception) { null }
    }

    private fun persistPeakPrices() {
        prefs.edit().putStringSet(KEY_PEAK_PRICES,
            peakPrices.entries.map { "${it.key}=${it.value}" }.toSet()
        ).apply()
    }

    private fun persistTpLevels() {
        prefs.edit().putStringSet(KEY_TP_LEVELS, synchronized(executedTpLevels) { executedTpLevels.toSet() }).apply()
    }

    private fun persistSlLevels() {
        prefs.edit().putStringSet(KEY_SL_LEVELS, synchronized(executedSlLevels) { executedSlLevels.toSet() }).apply()
    }

    private fun persistBreakeven() {
        prefs.edit().putStringSet(KEY_BREAKEVEN, synchronized(breakevenActive) { breakevenActive.toSet() }).apply()
    }

    suspend fun executeOrder(order: Order): Result<Order> {
        return try {
            // Pre-trade risk checks
            val riskResult = riskEngine.checkPreTrade(order)
            if (!riskResult.allowed) {
                Timber.w("Risk check failed for ${order.symbol}: ${riskResult.violations}")
                return Result.failure(Exception("Risk check failed: ${riskResult.violations.joinToString()}"))
            }

            val result = if (order.isPaperTrade) {
                tradingRepository.placePaperOrder(order)
            } else {
                tradingRepository.placeOrder(order)
            }
            result.onSuccess { placed ->
                TradeHaptics.orderFilled(context)
                TradeSounds.orderFilled(context)
                Timber.i("Order placed${if (order.isPaperTrade) " (paper)" else ""}: ${placed.id} ${placed.symbol} ${placed.side}")

                // Confirm fill and record risk with actual fill data
                val confirmed = confirmFill(placed)
                riskEngine.recordTradeResult(confirmed)

                val expectedPrice = order.price
                if (confirmed.filledPrice > 0 && expectedPrice != null && expectedPrice > 0) {
                    val slippage = (confirmed.filledPrice - expectedPrice) / expectedPrice * 100.0
                    Timber.i("Fill confirmed: ${confirmed.symbol} fillPrice=%.4f expected=%.4f slippage=%.4f%%"
                        .format(confirmed.filledPrice, expectedPrice, slippage))
                }
            }
            result
        } catch (e: Exception) {
            Timber.e(e, "Error executing order")
            Result.failure(e)
        }
    }

    /**
     * Poll Binance for actual fill status. For paper trades or already-filled orders,
     * returns immediately. For live orders, polls up to [FILL_POLL_MAX_ATTEMPTS] times.
     */
    private suspend fun confirmFill(placed: Order): Order {
        // Paper trades are instantly filled
        if (placed.isPaperTrade) return placed
        // Already filled at placement time (common for market orders)
        if (placed.status == OrderStatus.FILLED) return placed

        for (attempt in 1..FILL_POLL_MAX_ATTEMPTS) {
            delay(FILL_POLL_INTERVAL_MS)
            try {
                val queried = tradingRepository.queryOrder(placed.symbol, placed.id).getOrNull()
                    ?: continue
                if (queried.status == OrderStatus.FILLED || queried.status == OrderStatus.CANCELLED) {
                    Timber.i("Fill confirmed after ${attempt} polls: ${queried.symbol} status=${queried.status} fillQty=${queried.filledQuantity} fillPrice=${queried.filledPrice}")
                    return queried
                }
            } catch (e: Exception) {
                Timber.w(e, "Fill poll attempt $attempt failed for ${placed.symbol}")
            }
        }
        Timber.w("Fill confirmation timed out for ${placed.symbol} (${placed.id}) after $FILL_POLL_MAX_ATTEMPTS polls")
        return placed
    }

    suspend fun checkTakeProfits(order: Order) {
        if (order.takeProfits.isEmpty()) return
        val priceStr = getCachedTickerPrice(order.symbol) ?: return
        val currentPrice = priceStr.toBigDecimal()
        var allTpsExecuted = true

        for (tp in order.takeProfits) {
            val tpKey = "${order.id}:${tp.id}"
            if (tpKey in executedTpLevels) continue // Already fired this TP level

            allTpsExecuted = false
            val tpPrice = tp.price.toBigDecimal()
            val shouldTrigger = when (order.side) {
                OrderSide.BUY -> currentPrice.compareTo(tpPrice) >= 0
                OrderSide.SELL -> currentPrice.compareTo(tpPrice) <= 0
            }

            if (shouldTrigger) {
                Timber.i("TP triggered for ${order.symbol}: price=$currentPrice, tp=${tp.price}")
                try {
                    val closeQty = order.quantity * (tp.quantityPercent / 100.0)
                    val closeSide = if (order.side == OrderSide.BUY) OrderSide.SELL else OrderSide.BUY
                    tradingRepository.placeOrder(
                        order.copy(
                            id = "", side = closeSide, quantity = closeQty,
                            type = OrderType.MARKET, takeProfits = emptyList(), stopLosses = emptyList()
                        )
                    )
                    TradeHaptics.takeProfitHit(context)
                    TradeSounds.takeProfitHit(context)
                    executedTpLevels.add(tpKey)
                    persistTpLevels()

                    // B5: Breakeven-after-TP — move SL to entry after N TPs hit
                    val config = riskEngine.getRiskConfigSnapshot()
                    val firedTpCount = order.takeProfits.count { "${order.id}:${it.id}" in executedTpLevels }
                    if (firedTpCount >= config.breakEvenAfterTpCount && order.id !in breakevenActive) {
                        breakevenActive.add(order.id)
                        persistBreakeven()
                        Timber.i("Breakeven activated for ${order.symbol} (${order.id}) after $firedTpCount TPs")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to execute TP for ${order.symbol}")
                }
            }
        }

        // If all TP levels have been executed, mark the order as filled
        if (allTpsExecuted && order.takeProfits.isNotEmpty()) {
            orderDao.updateStatus(order.id, OrderStatus.FILLED.name)
            // Clean up tracking state
            order.takeProfits.forEach { tp -> executedTpLevels.remove("${order.id}:${tp.id}") }
            persistTpLevels()
            peakPrices.remove(order.id)
            persistPeakPrices()
        } else if (executedTpLevels.any { it.startsWith("${order.id}:") }) {
            orderDao.updateStatus(order.id, OrderStatus.PARTIALLY_FILLED.name)
        }
    }

    suspend fun checkStopLosses(order: Order) {
        if (order.stopLosses.isEmpty()) return
        val priceStr = getCachedTickerPrice(order.symbol) ?: return
        val currentPrice = priceStr.toBigDecimal()
        var allSlsExecuted = true

        for (sl in order.stopLosses) {
            val slKey = "${order.id}:${sl.id}"
            if (slKey in executedSlLevels) continue

            allSlsExecuted = false
            // B5: If breakeven is active, use entry price as SL floor/ceiling
            val rawSlPrice = sl.price
            val effectiveSlPrice = if (order.id in breakevenActive) {
                val entry = order.filledPrice.takeIf { it > 0 } ?: order.price ?: rawSlPrice
                when (order.side) {
                    OrderSide.BUY -> maxOf(rawSlPrice, entry)   // long: raise SL to at least entry
                    OrderSide.SELL -> minOf(rawSlPrice, entry)   // short: lower SL to at most entry
                }
            } else rawSlPrice
            val slPrice = effectiveSlPrice.toBigDecimal()
            val shouldTrigger = when (order.side) {
                OrderSide.BUY -> currentPrice.compareTo(slPrice) <= 0
                OrderSide.SELL -> currentPrice.compareTo(slPrice) >= 0
            }

            if (shouldTrigger) {
                Timber.i("SL triggered for ${order.symbol}: price=$currentPrice, sl=${sl.price}")
                try {
                    val closeQty = order.quantity * (sl.quantityPercent / 100.0)
                    val closeSide = if (order.side == OrderSide.BUY) OrderSide.SELL else OrderSide.BUY
                    tradingRepository.placeOrder(
                        order.copy(
                            id = "", side = closeSide, quantity = closeQty,
                            type = OrderType.MARKET,
                            takeProfits = emptyList(), stopLosses = emptyList()
                        )
                    )
                    TradeHaptics.stopLossHit(context)
                    TradeSounds.stopLossHit(context)
                    riskEngine.recordLoss(order)
                    executedSlLevels.add(slKey)
                    persistSlLevels()
                } catch (e: Exception) {
                    Timber.e(e, "Failed to execute SL for ${order.symbol}")
                }
            }
        }

        if (allSlsExecuted && order.stopLosses.isNotEmpty()) {
            orderDao.updateStatus(order.id, OrderStatus.FILLED.name)
            order.stopLosses.forEach { sl -> executedSlLevels.remove("${order.id}:${sl.id}") }
            persistSlLevels()
            peakPrices.remove(order.id)
            persistPeakPrices()
            executedTpLevels.removeAll { it.startsWith("${order.id}:") }
            persistTpLevels()
            breakevenActive.remove(order.id)
            persistBreakeven()
        } else if (executedSlLevels.any { it.startsWith("${order.id}:") }) {
            orderDao.updateStatus(order.id, OrderStatus.PARTIALLY_FILLED.name)
        }
    }

    suspend fun checkTrailingStop(order: Order) {
        val trailingPercent = order.trailingStopPercent ?: return
        if (trailingPercent <= 0.0) return
        val priceStr = getCachedTickerPrice(order.symbol) ?: return
        val currentPrice = priceStr.toDouble()

        // Update high-water mark (peak price since entry)
        val peak = peakPrices.compute(order.id) { _, prev ->
            when (order.side) {
                OrderSide.BUY -> maxOf(prev ?: currentPrice, currentPrice)
                OrderSide.SELL -> minOf(prev ?: currentPrice, currentPrice)
            }
        } ?: currentPrice
        persistPeakPrices()

        // Calculate trailing stop trigger from peak, not current price
        val trailDistance = peak * (trailingPercent / 100.0)
        val triggerPrice = when (order.side) {
            OrderSide.BUY -> peak - trailDistance   // Long: trail below peak
            OrderSide.SELL -> peak + trailDistance   // Short: trail above trough
        }

        val shouldTrigger = when (order.side) {
            OrderSide.BUY -> currentPrice <= triggerPrice
            OrderSide.SELL -> currentPrice >= triggerPrice
        }

        if (shouldTrigger) {
            Timber.i("Trailing stop triggered for ${order.symbol} (peak=$peak, trigger=$triggerPrice, current=$currentPrice)")
            TradeHaptics.stopLossHit(context)
            TradeSounds.stopLossHit(context)
            try {
                val closeSide = if (order.side == OrderSide.BUY) OrderSide.SELL else OrderSide.BUY
                tradingRepository.placeOrder(
                    order.copy(
                        id = "", side = closeSide, type = OrderType.MARKET,
                        takeProfits = emptyList(), stopLosses = emptyList(), trailingStopPercent = null
                    )
                )
                orderDao.updateStatus(order.id, OrderStatus.FILLED.name)
                peakPrices.remove(order.id)
                persistPeakPrices()
            } catch (e: Exception) {
                Timber.e(e, "Failed to execute trailing stop for ${order.symbol}")
            }
        }
    }

    suspend fun closeAllPositions() {
        TradeHaptics.emergencyClose(context)
        val openOrders = orderDao.getOpenOrders().first()
        for (orderEntity in openOrders) {
            try {
                val order = com.tfg.data.local.mapper.EntityMapper.run { orderEntity.toDomain() }
                val closeSide = if (order.side == OrderSide.BUY) OrderSide.SELL else OrderSide.BUY
                tradingRepository.placeOrder(
                    order.copy(
                        id = "", side = closeSide, type = OrderType.MARKET,
                        takeProfits = emptyList(), stopLosses = emptyList()
                    )
                )
                orderDao.updateStatus(order.id, OrderStatus.CANCELLED.name)
            } catch (e: Exception) {
                Timber.e(e, "Failed to close position: ${orderEntity.symbol}")
            }
        }
    }

    suspend fun executeQueueItem(item: OfflineQueueEntity) {
        when (item.action) {
            "PLACE_ORDER" -> {
                val orderJson = item.orderJson ?: item.signalJson ?: return
                val order = com.google.gson.Gson().fromJson(orderJson, Order::class.java)
                executeOrder(order)
            }
            "CANCEL_ORDER" -> {
                val orderJson = item.orderJson ?: return
                val parts = orderJson.split(",")
                if (parts.size >= 2) {
                    tradingRepository.cancelOrder(parts[0], parts[1])
                }
            }
        }
    }

    // ─── Conditional Order Execution ────────────────────────────────

    /** Previous ticker prices used for cross-detection */
    private val prevPrices = java.util.concurrent.ConcurrentHashMap<String, Double>()

    suspend fun checkConditionalOrder(order: Order) {
        if (order.type != OrderType.CONDITIONAL) return
        val trigger = order.conditionalTrigger ?: return
        val priceStr = getCachedTickerPrice(trigger.triggerSymbol) ?: return
        val currentPrice = priceStr.toDouble()
        val prevPrice = prevPrices[trigger.triggerSymbol]
        prevPrices[trigger.triggerSymbol] = currentPrice

        val triggered = when (trigger.condition) {
            TriggerCondition.PRICE_ABOVE -> currentPrice >= trigger.triggerPrice
            TriggerCondition.PRICE_BELOW -> currentPrice <= trigger.triggerPrice
            TriggerCondition.PRICE_CROSSES_ABOVE ->
                prevPrice != null && prevPrice < trigger.triggerPrice && currentPrice >= trigger.triggerPrice
            TriggerCondition.PRICE_CROSSES_BELOW ->
                prevPrice != null && prevPrice > trigger.triggerPrice && currentPrice <= trigger.triggerPrice
        }

        if (triggered) {
            Timber.i("Conditional order triggered: ${order.id} on ${order.symbol} (${trigger.condition} ${trigger.triggerPrice})")
            val marketOrder = order.copy(
                type = trigger.thenOrderType,
                price = trigger.thenPrice,
                conditionalTrigger = null,
                status = OrderStatus.PENDING
            )
            executeOrder(marketOrder)
            orderDao.updateStatus(order.id, OrderStatus.FILLED.name)
        }
    }

    // ─── Time-Based Order Execution ─────────────────────────────────

    suspend fun checkTimeBasedOrder(order: Order) {
        if (order.type != OrderType.TIME_BASED) return
        val scheduledAt = order.scheduledAt ?: return
        if (System.currentTimeMillis() >= scheduledAt) {
            Timber.i("Time-based order triggered: ${order.id} on ${order.symbol} (scheduled at $scheduledAt)")
            val marketOrder = order.copy(
                type = OrderType.MARKET,
                scheduledAt = null,
                status = OrderStatus.PENDING
            )
            executeOrder(marketOrder)
            orderDao.updateStatus(order.id, OrderStatus.FILLED.name)
        }
    }
}
