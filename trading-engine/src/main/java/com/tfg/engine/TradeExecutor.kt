package com.tfg.engine

import android.content.Context
import com.tfg.data.local.dao.OrderDao
import com.tfg.data.local.entity.OfflineQueueEntity
import com.tfg.data.local.mapper.EntityMapper.toEntity
import com.tfg.data.remote.api.BinanceApi
import com.tfg.domain.model.*
import com.tfg.domain.repository.TradingRepository
import dagger.hilt.android.qualifiers.ApplicationContext
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

    companion object {
        private const val KEY_PEAK_PRICES = "peak_prices"
        private const val KEY_TP_LEVELS = "executed_tp_levels"
        private const val TICKER_CACHE_TTL_MS = 2_000L // cache ticker for 2s to avoid N+1
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
                riskEngine.recordTradeResult(placed)
                Timber.i("Order placed${if (order.isPaperTrade) " (paper)" else ""}: ${placed.id} ${placed.symbol} ${placed.side}")
            }
            result
        } catch (e: Exception) {
            Timber.e(e, "Error executing order")
            Result.failure(e)
        }
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
                    executedTpLevels.add(tpKey)
                    persistTpLevels()
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

        for (sl in order.stopLosses) {
            val slPrice = sl.price.toBigDecimal()
            val shouldTrigger = when (order.side) {
                OrderSide.BUY -> currentPrice.compareTo(slPrice) <= 0
                OrderSide.SELL -> currentPrice.compareTo(slPrice) >= 0
            }

            if (shouldTrigger) {
                Timber.i("SL triggered for ${order.symbol}: price=$currentPrice, sl=${sl.price}")
                try {
                    val closeSide = if (order.side == OrderSide.BUY) OrderSide.SELL else OrderSide.BUY
                    tradingRepository.placeOrder(
                        order.copy(
                            id = "", side = closeSide, type = OrderType.MARKET,
                            takeProfits = emptyList(), stopLosses = emptyList()
                        )
                    )
                    orderDao.updateStatus(order.id, OrderStatus.FILLED.name)
                    riskEngine.recordLoss(order)
                    peakPrices.remove(order.id)
                    persistPeakPrices()
                    // Clean any TP tracking for this order
                    executedTpLevels.removeAll { it.startsWith("${order.id}:") }
                    persistTpLevels()
                } catch (e: Exception) {
                    Timber.e(e, "Failed to execute SL for ${order.symbol}")
                }
            }
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
}
