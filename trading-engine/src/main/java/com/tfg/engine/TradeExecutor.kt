package com.tfg.engine

import android.content.Context
import com.tfg.data.local.dao.OrderDao
import com.tfg.data.local.entity.OfflineQueueEntity
import com.tfg.data.local.mapper.EntityMapper.toEntity
import com.tfg.data.remote.api.BinanceApi
import com.tfg.data.remote.websocket.WebSocketManager
import com.tfg.domain.model.*
import com.tfg.domain.repository.TradingRepository
import com.tfg.domain.service.ConsoleBus
import com.tfg.domain.service.ConsoleSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TradeExecutor @Inject constructor(
    private val tradingRepository: TradingRepository,
    private val orderDao: OrderDao,
    private val riskEngine: RiskEngine,
    private val binanceApi: BinanceApi,
    private val webSocketManager: WebSocketManager,
    private val consoleBus: ConsoleBus,
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
        private const val FILL_WS_TIMEOUT_MS = 5_000L // wait this long for executionReport before falling back to polling
    }

    /** Per-symbol price cache to avoid N+1 API calls in the 1-second monitoring loop */
    private data class CachedTicker(val price: String, val timestamp: Long)
    private val tickerCache = java.util.concurrent.ConcurrentHashMap<String, CachedTicker>()

    /**
     * Per-order Mutex serializing TP/SL/trailing checks so concurrent monitor ticks
     * cannot read-modify-write the same in-memory level set and SharedPreferences
     * snapshot at the same time. Different orders still execute in parallel.
     */
    private val perOrderMutex = java.util.concurrent.ConcurrentHashMap<String, Mutex>()
    private fun mutexFor(orderId: String): Mutex =
        perOrderMutex.computeIfAbsent(orderId) { Mutex() }

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
        // Intentionally async (apply() vs commit()): peaks are written on every
        // ticker tick for trailing stops and a synchronous fsync on each call
        // would dominate the I/O budget. The loss is bounded \u2014 a process kill
        // between tick and write only loses the most recent peak. The next tick
        // re-derives a peak from price >= prevPeak, so accuracy self-heals
        // within one bar. Level-sets (TP/SL/breakeven) below use commit() because
        // missing one of those records would re-fire a level and double-trade.
        prefs.edit().putStringSet(KEY_PEAK_PRICES,
            peakPrices.entries.map { "${it.key}=${it.value}" }.toSet()
        ).apply()
    }

    // The level-set persists below use commit() so a process kill immediately
    // after a TP/SL fires never loses the "already-fired" record (which would
    // otherwise allow the same level to fire twice on restart).
    private fun persistTpLevels() {
        prefs.edit().putStringSet(KEY_TP_LEVELS, synchronized(executedTpLevels) { executedTpLevels.toSet() }).commit()
    }

    private fun persistSlLevels() {
        prefs.edit().putStringSet(KEY_SL_LEVELS, synchronized(executedSlLevels) { executedSlLevels.toSet() }).commit()
    }

    private fun persistBreakeven() {
        prefs.edit().putStringSet(KEY_BREAKEVEN, synchronized(breakevenActive) { breakevenActive.toSet() }).commit()
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

                // Confirm fill and record risk with actual fill data.
                // The DB row inserted by tradingRepository.placeOrder reflects
                // only the optimistic placement state \u2014 if the fill arrives
                // via WS executionReport (with updated qty/price/status), we
                // must persist that back so a process kill before the next
                // status update doesn't leave the row stuck on PENDING.
                // Wrapping a Room transaction across confirmFill is wrong
                // because confirmFill awaits the network; the durability gap
                // between exchange ack and DB write is instead covered by
                // reconcileWithBinance() at startup (see TradingForegroundService).
                val confirmed = confirmFill(placed)
                if (!confirmed.isPaperTrade && (
                        confirmed.status != placed.status ||
                        confirmed.filledQuantity != placed.filledQuantity ||
                        confirmed.filledPrice != placed.filledPrice)) {
                    runCatching { orderDao.update(confirmed.toEntity()) }
                        .onFailure { Timber.e(it, "Failed to persist confirmed fill for ${confirmed.id}") }
                }
                riskEngine.recordTradeResult(confirmed)

                val expectedPrice = order.price
                if (confirmed.filledPrice > 0 && expectedPrice != null && expectedPrice > 0) {
                    val slippage = (confirmed.filledPrice - expectedPrice) / expectedPrice * 100.0
                    Timber.i("Fill confirmed: ${confirmed.symbol} fillPrice=%.4f expected=%.4f slippage=%.4f%%"
                        .format(confirmed.filledPrice, expectedPrice, slippage))
                }

                // Place exchange-side SL/TP protection so the position is safe
                // even if the app is killed / device is in Doze mode. The
                // client-side checkTakeProfits / checkStopLosses loops still
                // run as a belt-and-braces safety net for partial TPs and
                // any condition the exchange-side bracket can't express
                // (trailing stops, time-based, conditional).
                if (!order.isPaperTrade && confirmed.status == OrderStatus.FILLED) {
                    placeExchangeSideProtection(order, confirmed)
                }
            }
            result
        } catch (e: Exception) {
            Timber.e(e, "Error executing order")
            Result.failure(e)
        }
    }

    /**
     * After an entry fills, place an exchange-side bracket so the position is
     * protected even if the app is killed.
     *  - Spot:    OCO (TP limit + SL stop-limit) on the OPPOSITE side, qty = filled qty.
     *  - Futures: STOP_MARKET + TAKE_PROFIT_MARKET reduceOnly, both as separate orders.
     *
     * Failures here are logged but do NOT bubble up — the entry already filled,
     * and the client-side monitoring loop is still active as a fallback.
     */
    private suspend fun placeExchangeSideProtection(original: Order, filled: Order) {
        // Use the nearest TP to entry as the exchange-side bracket target so the
        // position has at least partial protection even when multiple TP levels
        // are configured. Remaining levels beyond the nearest one continue to be
        // handled by the client-side monitoring loop (checkTakeProfits).
        // For a BUY: TPs are above entry → nearest = lowest price TP.
        // For a SELL: TPs are below entry → nearest = highest price TP.
        val nearestTp = if (original.side == OrderSide.BUY)
            original.takeProfits.minByOrNull { it.price }
        else
            original.takeProfits.maxByOrNull { it.price }
        val nearestTp2 = nearestTp ?: return   // No TPs at all — nothing to bracket
        val singleSl = original.stopLosses.firstOrNull() ?: return
        val qty = filled.filledQuantity.takeIf { it > 0 } ?: return
        val closeSide = if (original.side == OrderSide.BUY) OrderSide.SELL else OrderSide.BUY

        // Warn when multi-level TPs are present: levels beyond the nearest are
        // client-side only, meaning a device loss of power would miss them.
        if (original.takeProfits.size > 1) {
            consoleBus.warn(
                ConsoleSource.TRADING,
                "Multi-TP: TP1 on exchange, TP2+ client-only",
                "${original.symbol} has ${original.takeProfits.size} TP levels. " +
                "Only the nearest (${String.format("%.4f", nearestTp2.price)}) is placed on the exchange. " +
                "Remaining levels are protected by this app only — keep the app running.",
                symbol = original.symbol
            )
        }
        @Suppress("NAME_SHADOWING") val singleTp = nearestTp2

        try {
            when (original.marketType) {
                MarketType.SPOT -> {
                    // OCO stopLimitPrice slightly worse than stopPrice so the
                    // limit fills if the stop triggers in a fast market.
                    val slBuffer = if (closeSide == OrderSide.SELL) 0.995 else 1.005
                    val res = tradingRepository.placeOcoOrder(
                        symbol = original.symbol,
                        side = closeSide,
                        quantity = qty,
                        price = singleTp.price,
                        stopPrice = singleSl.price,
                        stopLimitPrice = singleSl.price * slBuffer
                    )
                    res.onSuccess {
                        Timber.i("Exchange-side OCO placed for ${original.symbol}: TP=${singleTp.price} SL=${singleSl.price}")
                    }.onFailure {
                        Timber.w(it, "Failed to place exchange-side OCO for ${original.symbol} — falling back to client-side monitoring")
                    }
                }
                MarketType.FUTURES_USDM -> {
                    val res = tradingRepository.placeBracketOrder(
                        symbol = original.symbol,
                        side = closeSide,
                        quantity = qty,
                        entryPrice = filled.filledPrice,
                        takeProfitPrice = singleTp.price,
                        stopLossPrice = singleSl.price
                    )
                    res.onSuccess {
                        Timber.i("Exchange-side bracket placed for ${original.symbol}: TP=${singleTp.price} SL=${singleSl.price}")
                    }.onFailure {
                        Timber.w(it, "Failed to place exchange-side bracket for ${original.symbol} — falling back to client-side monitoring")
                    }
                }
                else -> { /* paper / unsupported */ }
            }
        } catch (e: Exception) {
            Timber.w(e, "placeExchangeSideProtection threw for ${original.symbol}")
        }
    }

    /**
     * Confirm fill via Binance user-data stream first (push-based, real-time)
     * and only fall back to REST polling if the WS event doesn't arrive within
     * [FILL_WS_TIMEOUT_MS]. For paper trades or already-filled orders, returns
     * immediately.
     */
    private suspend fun confirmFill(placed: Order): Order {
        // Paper trades are instantly filled
        if (placed.isPaperTrade) return placed
        // Already filled at placement time (common for market orders)
        if (placed.status == OrderStatus.FILLED) return placed

        // 1) Wait for the executionReport / ORDER_TRADE_UPDATE matching this
        //    clientOrderId. This avoids burning REST quota on every fill and
        //    is the recommended Binance pattern.
        val event = withTimeoutOrNull(FILL_WS_TIMEOUT_MS) {
            webSocketManager.userDataFlow
                .filter { it.clientOrderId == placed.id || it.orderId?.toString() == placed.id }
                .filter { it.orderStatus == "FILLED" || it.orderStatus == "CANCELED" || it.orderStatus == "EXPIRED" }
                .first()
        }
        if (event != null) {
            val filledQty = event.filledQty?.toDoubleOrNull() ?: placed.filledQuantity
            val filledQuote = event.filledQuoteQty?.toDoubleOrNull() ?: 0.0
            val avgPrice = if (filledQty > 0 && filledQuote > 0) filledQuote / filledQty else placed.filledPrice
            val newStatus = when (event.orderStatus) {
                "FILLED" -> OrderStatus.FILLED
                "CANCELED", "EXPIRED" -> OrderStatus.CANCELLED
                else -> placed.status
            }
            Timber.i("Fill confirmed via WS: ${placed.symbol} status=$newStatus fillQty=$filledQty fillPrice=$avgPrice")
            return placed.copy(
                status = newStatus,
                filledQuantity = filledQty,
                filledPrice = avgPrice
            )
        }

        // 2) WS event didn't arrive in time — fall back to REST polling.
        Timber.w("No WS fill event within ${FILL_WS_TIMEOUT_MS}ms for ${placed.id} — falling back to REST polling")
        for (attempt in 1..FILL_POLL_MAX_ATTEMPTS) {
            delay(FILL_POLL_INTERVAL_MS)
            try {
                val queried = tradingRepository.queryOrder(placed.symbol, placed.id).getOrNull()
                    ?: continue
                if (queried.status == OrderStatus.FILLED || queried.status == OrderStatus.CANCELLED) {
                    Timber.i("Fill confirmed via REST after ${attempt} polls: ${queried.symbol} status=${queried.status} fillQty=${queried.filledQuantity} fillPrice=${queried.filledPrice}")
                    return queried
                }
            } catch (e: Exception) {
                Timber.w(e, "Fill poll attempt $attempt failed for ${placed.symbol}")
            }
        }
        Timber.w("Fill confirmation timed out for ${placed.symbol} (${placed.id}) after $FILL_POLL_MAX_ATTEMPTS polls")
        return placed
    }

    suspend fun checkTakeProfits(order: Order) = mutexFor(order.id).withLock {
        if (order.takeProfits.isEmpty()) return@withLock
        val priceStr = getCachedTickerPrice(order.symbol) ?: return@withLock
        // Fail-closed: an unparseable ticker (rate-limit JSON body, error
        // response, locale comma) must NOT throw and skip the rest of the
        // monitoring tick — just abandon this tick for this order.
        val currentPrice = priceStr.toBigDecimalOrNull() ?: run {
            Timber.w("checkTakeProfits: unparseable price '$priceStr' for ${order.symbol}")
            return@withLock
        }
        var allTpsExecuted = true

        for (tp in order.takeProfits) {
            val tpKey = "${order.id}:${tp.id}"
            if (tpKey in executedTpLevels) continue // Already fired this TP level

            allTpsExecuted = false
            // tp.price is a Double, so BigDecimal conversion can never fail.
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
                    val result = tradingRepository.placeOrder(
                        order.copy(
                            id = "", side = closeSide, quantity = closeQty,
                            type = OrderType.MARKET, takeProfits = emptyList(), stopLosses = emptyList(),
                            reduceOnly = order.marketType == MarketType.FUTURES_USDM
                        )
                    )
                    val placed = result.getOrNull()
                    if (placed == null) {
                        Timber.e(result.exceptionOrNull(), "TP close order rejected by exchange for ${order.symbol}")
                    } else {
                        val confirmed = confirmFill(placed)
                        if (confirmed.status == OrderStatus.FILLED) {
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
                        } else {
                            Timber.w("TP close placed but fill not yet confirmed (${confirmed.status}) for ${order.symbol} — will retry on next tick")
                        }
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

    suspend fun checkStopLosses(order: Order) = mutexFor(order.id).withLock {
        if (order.stopLosses.isEmpty()) return@withLock
        val priceStr = getCachedTickerPrice(order.symbol) ?: return@withLock
        val currentPrice = priceStr.toBigDecimalOrNull() ?: run {
            Timber.w("checkStopLosses: unparseable price '$priceStr' for ${order.symbol}")
            return@withLock
        }
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
            // effectiveSlPrice is a Double, so BigDecimal conversion can never fail.
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
                    val result = tradingRepository.placeOrder(
                        order.copy(
                            id = "", side = closeSide, quantity = closeQty,
                            type = OrderType.MARKET,
                            takeProfits = emptyList(), stopLosses = emptyList(),
                            reduceOnly = order.marketType == MarketType.FUTURES_USDM
                        )
                    )
                    val placed = result.getOrNull()
                    if (placed == null) {
                        Timber.e(result.exceptionOrNull(), "SL close order rejected by exchange for ${order.symbol}")
                    } else {
                        val confirmed = confirmFill(placed)
                        if (confirmed.status == OrderStatus.FILLED) {
                            TradeHaptics.stopLossHit(context)
                            TradeSounds.stopLossHit(context)
                            riskEngine.recordLoss(order)
                            executedSlLevels.add(slKey)
                            persistSlLevels()
                        } else {
                            Timber.w("SL close placed but fill not yet confirmed (${confirmed.status}) for ${order.symbol} — will retry on next tick")
                        }
                    }
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

    suspend fun checkTrailingStop(order: Order) = mutexFor(order.id).withLock {
        val trailingPercent = order.trailingStopPercent ?: return@withLock
        if (trailingPercent <= 0.0) return@withLock
        val priceStr = getCachedTickerPrice(order.symbol) ?: return@withLock
        val currentPrice = priceStr.toDoubleOrNull() ?: run {
            Timber.w("checkTrailingStop: unparseable price '$priceStr' for ${order.symbol}")
            return@withLock
        }

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
                val result = tradingRepository.placeOrder(
                    order.copy(
                        id = "", side = closeSide, type = OrderType.MARKET,
                        takeProfits = emptyList(), stopLosses = emptyList(), trailingStopPercent = null,
                        reduceOnly = order.marketType == MarketType.FUTURES_USDM
                    )
                )
                val placed = result.getOrNull()
                if (placed == null) {
                    Timber.e(result.exceptionOrNull(), "Trailing-stop close order rejected by exchange for ${order.symbol}")
                } else {
                    val confirmed = confirmFill(placed)
                    if (confirmed.status == OrderStatus.FILLED) {
                        orderDao.updateStatus(order.id, OrderStatus.FILLED.name)
                        peakPrices.remove(order.id)
                        persistPeakPrices()
                    } else {
                        Timber.w("Trailing-stop close placed but fill not yet confirmed (${confirmed.status}) for ${order.symbol} — will retry on next tick")
                    }
                }
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
                val result = tradingRepository.placeOrder(
                    order.copy(
                        id = "", side = closeSide, type = OrderType.MARKET,
                        takeProfits = emptyList(), stopLosses = emptyList()
                    )
                )
                val placed = result.getOrNull()
                if (placed == null) {
                    Timber.e(result.exceptionOrNull(), "Close order rejected by exchange for ${orderEntity.symbol}")
                } else {
                    val confirmed = confirmFill(placed)
                    if (confirmed.status == OrderStatus.FILLED) {
                        orderDao.updateStatus(order.id, OrderStatus.CANCELLED.name)
                    } else {
                        Timber.w("Close position placed but fill not yet confirmed (${confirmed.status}) for ${orderEntity.symbol}")
                    }
                }
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
                val cancelOrder = com.google.gson.Gson().fromJson(orderJson, Order::class.java)
                tradingRepository.cancelOrder(cancelOrder.id, cancelOrder.symbol)
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
        val currentPrice = priceStr.toDoubleOrNull() ?: run {
            Timber.w("checkConditionalOrder: unparseable price '$priceStr' for ${trigger.triggerSymbol}")
            return
        }
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
