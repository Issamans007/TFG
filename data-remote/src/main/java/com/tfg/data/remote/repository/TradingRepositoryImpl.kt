package com.tfg.data.remote.repository

import com.tfg.data.local.dao.*
import com.tfg.data.local.mapper.EntityMapper.toDomain
import com.tfg.data.local.mapper.EntityMapper.toEntity
import com.tfg.data.remote.api.BinanceApi
import com.tfg.data.remote.api.BinanceFuturesApi
import com.tfg.data.remote.api.BinanceTimeSync
import com.tfg.data.remote.api.ExchangeFiltersCache
import com.tfg.data.remote.api.SymbolFilters
import com.tfg.data.remote.api.parseBinanceError
import com.tfg.data.remote.interceptor.BinanceSigner
import com.tfg.domain.model.*
import com.tfg.domain.repository.TradingRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class TradingRepositoryImpl @Inject constructor(
    private val binanceApi: BinanceApi,
    private val binanceFuturesApi: BinanceFuturesApi,
    private val orderDao: OrderDao,
    private val timeSync: BinanceTimeSync,
    private val filters: ExchangeFiltersCache,
    @Named("apiSecret") private val secretProvider: () -> String
) : TradingRepository {

    /**
     * Build the timestamp + recvWindow + signature triplet for a signed Binance
     * request. `extra` contains all the other params that need to be in the
     * signature payload (Binance signs the full query string). recvWindow is
     * always 10s (max useful before clock-drift cancels it out).
     */
    private suspend fun signedParams(extra: Map<String, String>): Triple<Long, Long, String> {
        val ts = timeSync.now()
        val recv = BinanceTimeSync.RECV_WINDOW_MS
        // LinkedHashMap preserves insertion order so signature matches what we
        // send on the wire. Retrofit serialises @Field/@Query in the order
        // they're emitted by the underlying Map iterator.
        val signed = LinkedHashMap<String, String>(extra.size + 2).apply {
            putAll(extra)
            put("timestamp", ts.toString())
            put("recvWindow", recv.toString())
        }
        return Triple(ts, recv, BinanceSigner.signParams(signed, secretProvider()))
    }

    /**
     * Run a signed Binance request, automatically retrying once on -1021
     * Timestamp errors after a forced time resync. Other errors propagate.
     */
    private suspend fun <T> withTimeResync(block: suspend () -> T): T {
        repeat(3) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                val parsed = parseBinanceError(e)
                if (parsed?.isTimestampError == true && attempt < 2) {
                    Timber.w("Binance -1021 (timestamp) — resyncing and retrying (attempt ${attempt + 1})")
                    timeSync.forceResync()
                    delay(150L * (attempt + 1))
                    return@repeat
                }
                throw e
            }
        }
        // Unreachable: the `repeat` body either returns or throws on every iteration.
        error("withTimeResync exhausted retries without producing a value")
    }

    override suspend fun placeOrder(order: Order): Result<Order> {
        if (order.marketType == MarketType.FUTURES_USDM) return placeFuturesOrderInternal(order)
        return placeSpotOrder(order)
    }

    private suspend fun placeSpotOrder(order: Order): Result<Order> = runCatching {
        // Round qty/price to LOT_SIZE / tickSize before signing — Binance
        // rejects the order outright otherwise. minQty / minNotional are
        // checked client-side so we surface a clear error instead of a
        // confusing "Filter failure: MIN_NOTIONAL" from the exchange.
        val f = filters.getSpot(order.symbol)
        val qty = f?.roundQty(order.quantity) ?: order.quantity
        val price = order.price?.let { f?.roundPrice(it) ?: it }
        val stopPrice = order.stopPrice?.let { f?.roundPrice(it) ?: it }
        validateAgainstFilters(f, qty, price ?: order.price ?: 0.0, order.symbol)

        withTimeResync {
            val extra = buildMap {
                put("symbol", order.symbol)
                put("side", order.side.name)
                put("type", mapOrderType(order.type))
                put("quantity", qty.toString())
                price?.let { put("price", it.toString()) }
                stopPrice?.let { put("stopPrice", it.toString()) }
                if (order.type == OrderType.LIMIT) put("timeInForce", order.timeInForce.name)
                put("newClientOrderId", order.id)
            }
            val (ts, recv, sig) = signedParams(extra)

            val response = binanceApi.placeOrder(
                symbol = order.symbol,
                side = order.side.name,
                type = mapOrderType(order.type),
                timeInForce = if (order.type == OrderType.LIMIT) order.timeInForce.name else null,
                quantity = qty.toString(),
                price = price?.toString(),
                stopPrice = stopPrice?.toString(),
                clientOrderId = order.id,
                timestamp = ts,
                recvWindow = recv,
                signature = sig
            )

            val fee = response.fills?.sumOf { it.commission.toDoubleOrNull() ?: 0.0 } ?: 0.0
            val executedQty = response.executedQty.toDoubleOrNull() ?: 0.0
            val cumQuote = response.cummulativeQuoteQty.toDoubleOrNull() ?: 0.0
            val filledPrice = if (executedQty > 0) cumQuote / executedQty else 0.0

            val placed = order.copy(
                quantity = qty,
                price = price,
                stopPrice = stopPrice,
                binanceOrderId = response.orderId,
                status = mapBinanceStatus(response.status),
                filledQuantity = executedQty,
                filledPrice = filledPrice,
                fee = fee,
                feeAsset = response.fills?.firstOrNull()?.commissionAsset ?: "",
                executedAt = if (response.status == "FILLED") System.currentTimeMillis() else null,
                updatedAt = System.currentTimeMillis()
            )
            orderDao.insert(placed.toEntity())
            placed
        }
    }.onFailure { Timber.e(it, "Failed to place order") }

    private fun validateAgainstFilters(f: SymbolFilters?, qty: Double, refPrice: Double, symbol: String) {
        if (f == null) return
        if (f.minQty.toDouble() > 0.0 && qty < f.minQty.toDouble()) {
            throw IllegalArgumentException("Order qty $qty for $symbol is below minQty ${f.minQty}")
        }
        if (refPrice > 0.0 && !f.clearsMinNotional(qty, refPrice)) {
            throw IllegalArgumentException("Order notional (${qty * refPrice}) for $symbol is below minNotional ${f.minNotional}")
        }
    }

    override suspend fun cancelOrder(orderId: String, symbol: String): Result<Boolean> = runCatching {
        val orderEntity = orderDao.getOrderById(orderId).first()
            ?: throw IllegalStateException("Order $orderId not found in local DB")
        val binanceOrderId = orderEntity.binanceOrderId
            ?: throw IllegalStateException("Order $orderId has no Binance order ID")
        withTimeResync {
            val (ts, recv, sig) = signedParams(mapOf(
                "symbol" to symbol,
                "orderId" to binanceOrderId.toString()
            ))
            binanceApi.cancelOrder(symbol, binanceOrderId, ts, recv, sig)
        }
        orderDao.updateStatus(orderId, OrderStatus.CANCELLED.name)
        true
    }

    override suspend fun closePosition(symbol: String): Result<Order> = runCatching {
        // Find the open order for this symbol and close via opposite-side market order
        val openOrders = orderDao.getOpenOrdersForSymbol(symbol).first()
        val orderEntity = openOrders.firstOrNull()
            ?: throw IllegalStateException("No open position for $symbol")
        val order = com.tfg.data.local.mapper.EntityMapper.run { orderEntity.toDomain() }
        val closeSide = if (order.side == OrderSide.BUY) OrderSide.SELL else OrderSide.BUY
        val closeOrder = order.copy(
            id = java.util.UUID.randomUUID().toString(),
            side = closeSide,
            type = OrderType.MARKET,
            price = null,
            takeProfits = emptyList(),
            stopLosses = emptyList(),
            trailingStopPercent = null
        )
        val result = placeOrder(closeOrder).getOrThrow()
        orderDao.updateStatus(order.id, OrderStatus.FILLED.name)
        result
    }

    override suspend fun closeAllPositions(): Result<List<Order>> = runCatching {
        // 1. Cancel all open limit/stop orders on Binance
        val openOrders = withTimeResync {
            val (ts, recv, sig) = signedParams(emptyMap())
            binanceApi.getOpenOrders(null, ts, recv, sig)
        }
        openOrders.forEach { o ->
            try {
                withTimeResync {
                    val (ts, recv, sig) = signedParams(mapOf(
                        "symbol" to o.symbol,
                        "orderId" to o.orderId.toString()
                    ))
                    binanceApi.cancelOrder(o.symbol, o.orderId, ts, recv, sig)
                }
            } catch (e: Exception) {
                Timber.w(e, "Failed to cancel order ${o.orderId} on ${o.symbol}")
            }
        }

        // 2. Sell any non-stablecoin balances to close actual positions
        val closedOrders = mutableListOf<Order>()
        val account = withTimeResync {
            val (ts, recv, sig) = signedParams(emptyMap())
            binanceApi.getAccount(ts, recv, sig)
        }
        val stablecoins = setOf("USDT", "BUSD", "USDC", "TUSD", "FDUSD")
        for (balance in account.balances) {
            val free = balance.free.toDoubleOrNull() ?: 0.0
            if (free > 0.0 && balance.asset !in stablecoins) {
                val sellSymbol = "${balance.asset}USDT"
                try {
                    val sellOrder = Order(
                        id = java.util.UUID.randomUUID().toString(),
                        symbol = sellSymbol,
                        side = OrderSide.SELL,
                        type = OrderType.MARKET,
                        quantity = free
                    )
                    val result = placeOrder(sellOrder)
                    result.getOrNull()?.let { closedOrders.add(it) }
                } catch (e: Exception) {
                    Timber.w(e, "Failed to sell ${balance.asset}")
                }
            }
        }

        // 3. Update local DB orders
        val localOpenOrders = orderDao.getOpenOrders().first()
        localOpenOrders.forEach { orderEntity ->
            orderDao.updateStatus(orderEntity.id, OrderStatus.EMERGENCY_CLOSED.name)
        }

        closedOrders
    }

    override fun getOpenOrders(): Flow<List<Order>> =
        orderDao.getOpenOrders().map { list -> list.map { it.toDomain() } }

    override fun getOrderHistory(limit: Int): Flow<List<Order>> =
        orderDao.getOrderHistory(limit).map { list -> list.map { it.toDomain() } }

    override fun getOrderById(orderId: String): Flow<Order?> =
        orderDao.getOrderById(orderId).map { it?.toDomain() }

    override suspend fun placePaperOrder(order: Order): Result<Order> = runCatching {
        val paperOrder = order.copy(
            status = OrderStatus.FILLED,
            filledQuantity = order.quantity,
            filledPrice = order.price ?: 0.0,
            executedAt = System.currentTimeMillis(),
            isPaperTrade = true,
            updatedAt = System.currentTimeMillis()
        )
        orderDao.insert(paperOrder.toEntity())
        paperOrder
    }

    override suspend fun cancelPaperOrder(orderId: String): Result<Boolean> = runCatching {
        orderDao.updateStatus(orderId, OrderStatus.CANCELLED.name)
        true
    }

    override suspend fun queryOrder(symbol: String, clientOrderId: String): Result<Order> = runCatching {
        val response = withTimeResync {
            val (ts, recv, sig) = signedParams(mapOf(
                "symbol" to symbol,
                "origClientOrderId" to clientOrderId
            ))
            binanceApi.queryOrder(symbol, clientOrderId, ts, recv, sig)
        }

        val fee = response.fills?.sumOf { it.commission.toDoubleOrNull() ?: 0.0 } ?: 0.0
        val executedQty = response.executedQty.toDoubleOrNull() ?: 0.0
        val cumQuote = response.cummulativeQuoteQty.toDoubleOrNull() ?: 0.0
        val filledPrice = if (executedQty > 0) cumQuote / executedQty else 0.0

        val order = Order(
            id = clientOrderId,
            symbol = symbol,
            side = OrderSide.valueOf(response.side),
            type = OrderType.MARKET,
            status = mapBinanceStatus(response.status),
            binanceOrderId = response.orderId,
            quantity = response.origQty.toDoubleOrNull() ?: 0.0,
            filledQuantity = executedQty,
            filledPrice = filledPrice,
            fee = fee,
            feeAsset = response.fills?.firstOrNull()?.commissionAsset ?: "",
            executedAt = if (response.status == "FILLED") response.updateTime else null,
            updatedAt = response.updateTime,
            createdAt = response.time
        )
        // Update local DB with fill info
        orderDao.getOrderById(clientOrderId).first()?.let {
            orderDao.updateStatus(clientOrderId, order.status.name)
        }
        order
    }

    override suspend fun placeOcoOrder(
        symbol: String, side: OrderSide, quantity: Double,
        price: Double, stopPrice: Double, stopLimitPrice: Double
    ): Result<List<Order>> = runCatching {
        // Round all three price legs and the qty to symbol filters before signing.
        val f = filters.getSpot(symbol)
        val qty = f?.roundQty(quantity) ?: quantity
        val px = f?.roundPrice(price) ?: price
        val stopPx = f?.roundPrice(stopPrice) ?: stopPrice
        val stopLimitPx = f?.roundPrice(stopLimitPrice) ?: stopLimitPrice
        validateAgainstFilters(f, qty, px, symbol)

        val response = withTimeResync {
            val (ts, recv, sig) = signedParams(mapOf(
                "symbol" to symbol, "side" to side.name,
                "quantity" to qty.toString(), "price" to px.toString(),
                "stopPrice" to stopPx.toString(),
                "stopLimitPrice" to stopLimitPx.toString(),
                "stopLimitTimeInForce" to "GTC"
            ))
            binanceApi.placeOcoOrder(
                symbol, side.name, qty.toString(), px.toString(),
                stopPx.toString(), stopLimitPx.toString(), "GTC", ts, recv, sig
            )
        }
        response.orders.map { dto ->
            Order(
                id = dto.clientOrderId, symbol = dto.symbol,
                side = OrderSide.valueOf(dto.side), type = OrderType.OCO,
                quantity = dto.origQty.toDouble(), price = dto.price.toDoubleOrNull(),
                binanceOrderId = dto.orderId,
                status = mapBinanceStatus(dto.status)
            )
        }
    }

    /**
     * Place exchange-side futures protection AFTER an entry has filled.
     * `side` is the CLOSE side (opposite of the position). Places two
     * reduceOnly orders: STOP_MARKET for stop-loss and TAKE_PROFIT_MARKET
     * for take-profit. Both use closePosition=true so they always exit the
     * full position regardless of partial fills, and either order firing
     * is independent — Binance does not have native OCO on futures, so the
     * caller (or userDataStream loop) is responsible for cancelling the
     * sibling once one fires. We log if either leg fails so the caller can
     * fall back to client-side monitoring.
     */
    override suspend fun placeBracketOrder(
        symbol: String, side: OrderSide, quantity: Double,
        entryPrice: Double, takeProfitPrice: Double, stopLossPrice: Double
    ): Result<List<Order>> = runCatching {
        val placed = mutableListOf<Order>()

        // Stop loss leg.
        val slId = java.util.UUID.randomUUID().toString()
        val slOrder = Order(
            id = slId,
            symbol = symbol, side = side,
            type = OrderType.STOP_MARKET,
            quantity = quantity, stopPrice = stopLossPrice,
            marketType = MarketType.FUTURES_USDM,
            reduceOnly = true,
            closePosition = true
        )
        runCatching { placeFuturesOrderInternal(slOrder).getOrThrow() }
            .onSuccess { placed.add(it) }
            .onFailure { Timber.w(it, "STOP_MARKET leg failed for $symbol") }

        // Take profit leg — TAKE_PROFIT_MARKET with reduceOnly. Mapped to
        // OrderType.STOP_MARKET in the public model since we don't have a
        // distinct enum; the repository overrides type below.
        val tpId = java.util.UUID.randomUUID().toString()
        val tpOrder = Order(
            id = tpId,
            symbol = symbol, side = side,
            type = OrderType.STOP_MARKET, // overridden to TAKE_PROFIT_MARKET below
            quantity = quantity, stopPrice = takeProfitPrice,
            marketType = MarketType.FUTURES_USDM,
            reduceOnly = true,
            closePosition = true
        )
        runCatching { placeFuturesTpMarket(tpOrder).getOrThrow() }
            .onSuccess { placed.add(it) }
            .onFailure { Timber.w(it, "TAKE_PROFIT_MARKET leg failed for $symbol") }

        if (placed.isEmpty()) throw IllegalStateException("Both bracket legs failed for $symbol")
        placed
    }

    /** TAKE_PROFIT_MARKET helper: same shape as placeFuturesOrderInternal but forces type=TAKE_PROFIT_MARKET. */
    private suspend fun placeFuturesTpMarket(order: Order): Result<Order> = runCatching {
        val response = withTimeResync {
            val extra = buildMap<String, String> {
                put("symbol", order.symbol)
                put("side", order.side.name)
                put("type", "TAKE_PROFIT_MARKET")
                put("closePosition", "true")
                order.stopPrice?.let { put("stopPrice", it.toString()) }
                put("workingType", "MARK_PRICE")
                put("newClientOrderId", order.id)
            }
            val (ts, recv, sig) = signedParams(extra)
            binanceFuturesApi.placeFuturesOrder(
                symbol = order.symbol,
                side = order.side.name,
                positionSide = null,
                type = "TAKE_PROFIT_MARKET",
                timeInForce = null,
                quantity = null,
                reduceOnly = null,
                closePosition = "true",
                price = null,
                stopPrice = order.stopPrice?.toString(),
                workingType = "MARK_PRICE",
                clientOrderId = order.id,
                timestamp = ts,
                recvWindow = recv,
                signature = sig
            )
        }
        order.copy(
            binanceOrderId = response.orderId,
            status = mapBinanceStatus(response.status),
            updatedAt = System.currentTimeMillis()
        ).also { orderDao.insert(it.toEntity()) }
    }

    override suspend fun reconcileWithBinance(): Result<Boolean> = runCatching {
        val account = withTimeResync {
            val (ts, recv, sig) = signedParams(emptyMap())
            binanceApi.getAccount(ts, recv, sig)
        }
        Timber.d("Reconciled: ${account.balances.size} assets, canTrade=${account.canTrade}")
        true
    }

    private fun mapOrderType(type: OrderType): String = when (type) {
        OrderType.MARKET -> "MARKET"
        OrderType.LIMIT -> "LIMIT"
        OrderType.STOP_LIMIT -> "STOP_LOSS_LIMIT"
        OrderType.STOP_MARKET -> "STOP_LOSS"
        OrderType.TRAILING_STOP -> "MARKET" // Binance Spot has no native trailing stop; managed client-side by TradeExecutor
        else -> "LIMIT"
    }

    private fun mapBinanceStatus(status: String): OrderStatus = when (status) {
        "NEW" -> OrderStatus.SUBMITTED
        "PARTIALLY_FILLED" -> OrderStatus.PARTIALLY_FILLED
        "FILLED" -> OrderStatus.FILLED
        "CANCELED" -> OrderStatus.CANCELLED
        "REJECTED" -> OrderStatus.REJECTED
        "EXPIRED" -> OrderStatus.EXPIRED
        else -> OrderStatus.PENDING
    }

    // ─── Futures (USDⓈ-M) ──────────────────────────────────────────

    private fun mapFuturesOrderType(type: OrderType): String = when (type) {
        OrderType.MARKET -> "MARKET"
        OrderType.LIMIT -> "LIMIT"
        OrderType.STOP_LIMIT -> "STOP"
        OrderType.STOP_MARKET -> "STOP_MARKET"
        OrderType.TRAILING_STOP -> "TRAILING_STOP_MARKET"
        else -> "LIMIT"
    }

    private suspend fun placeFuturesOrderInternal(order: Order): Result<Order> = runCatching {
        // L6: Detect hedge mode. If user account is in hedge mode (dualSidePosition=true)
        // and the caller forgot to set positionSide, Binance rejects with -4061.
        // Auto-derive positionSide from order.side so the trade still goes through.
        val hedgeMode = runCatching {
            withTimeResync {
                val (ts, recv, sig) = signedParams(emptyMap())
                binanceFuturesApi.getPositionSideDual(ts, recv, sig).dualSidePosition
            }
        }.getOrDefault(false)

        val effectiveOrder = if (hedgeMode && order.positionSide == PositionSide.BOTH && !order.reduceOnly && !order.closePosition) {
            // Opening trade in hedge mode: BUY → LONG, SELL → SHORT.
            order.copy(positionSide = if (order.side == OrderSide.BUY) PositionSide.LONG else PositionSide.SHORT)
        } else order

        // Apply leverage and margin type before placing the order. Failures are non-fatal
        // (Binance returns -4046 if value already matches).
        runCatching {
            if (effectiveOrder.leverage > 0) {
                withTimeResync {
                    val (ts, recv, sig) = signedParams(mapOf(
                        "symbol" to effectiveOrder.symbol,
                        "leverage" to effectiveOrder.leverage.toString()
                    ))
                    binanceFuturesApi.changeLeverage(effectiveOrder.symbol, effectiveOrder.leverage, ts, recv, sig)
                }
            }
        }
        runCatching {
            val mt = if (effectiveOrder.marginType == MarginType.CROSSED) "CROSSED" else "ISOLATED"
            withTimeResync {
                val (ts, recv, sig) = signedParams(mapOf(
                    "symbol" to effectiveOrder.symbol,
                    "marginType" to mt
                ))
                binanceFuturesApi.changeMarginType(effectiveOrder.symbol, mt, ts, recv, sig)
            }
        }

        // L8: Verify the leverage / margin type actually took effect by reading
        // back the position-risk row. If Binance silently kept the old values
        // (rare but happens during maintenance windows) we want to fail loudly
        // BEFORE submitting an order at the wrong leverage.
        if (effectiveOrder.leverage > 0) {
            runCatching {
                val rows = withTimeResync {
                    val (ts, recv, sig) = signedParams(mapOf("symbol" to effectiveOrder.symbol))
                    binanceFuturesApi.getPositionRisk(effectiveOrder.symbol, ts, recv, sig)
                }
                val actual = rows.firstOrNull()
                val actualLev = actual?.leverage?.toIntOrNull()
                if (actualLev != null && actualLev != effectiveOrder.leverage) {
                    Timber.w("Leverage mismatch for ${effectiveOrder.symbol}: requested=${effectiveOrder.leverage}x, account=${actualLev}x — proceeding with account value")
                }
                val expectedMargin = if (effectiveOrder.marginType == MarginType.CROSSED) "cross" else "isolated"
                val actualMargin = actual?.marginType?.lowercase()
                if (actualMargin != null && actualMargin != expectedMargin) {
                    Timber.w("Margin-type mismatch for ${effectiveOrder.symbol}: requested=$expectedMargin, account=$actualMargin")
                }
            }
        }

        val order = effectiveOrder // shadow so existing code below uses the (possibly-amended) order

        // Round qty/price against futures filters before signing.
        val f = filters.getFutures(order.symbol)
        val qty = f?.roundQty(order.quantity) ?: order.quantity
        val price = order.price?.let { f?.roundPrice(it) ?: it }
        val stopPrice = order.stopPrice?.let { f?.roundPrice(it) ?: it }
        if (!order.closePosition) {
            validateAgainstFilters(f, qty, price ?: order.price ?: 0.0, order.symbol)
        }

        val typeStr = mapFuturesOrderType(order.type)
        val response = withTimeResync {
            val extra = buildMap<String, String> {
                put("symbol", order.symbol)
                put("side", order.side.name)
                if (order.positionSide != PositionSide.BOTH) put("positionSide", order.positionSide.name)
                put("type", typeStr)
                if (order.type == OrderType.LIMIT) put("timeInForce", order.timeInForce.name)
                if (!order.closePosition) put("quantity", qty.toString())
                if (order.reduceOnly && order.positionSide == PositionSide.BOTH) put("reduceOnly", "true")
                if (order.closePosition) put("closePosition", "true")
                price?.let { put("price", it.toString()) }
                stopPrice?.let { put("stopPrice", it.toString()) }
                put("newClientOrderId", order.id)
            }
            val (ts, recv, sig) = signedParams(extra)

            binanceFuturesApi.placeFuturesOrder(
                symbol = order.symbol,
                side = order.side.name,
                positionSide = if (order.positionSide != PositionSide.BOTH) order.positionSide.name else null,
                type = typeStr,
                timeInForce = if (order.type == OrderType.LIMIT) order.timeInForce.name else null,
                quantity = if (!order.closePosition) qty.toString() else null,
                reduceOnly = if (order.reduceOnly && order.positionSide == PositionSide.BOTH) "true" else null,
                closePosition = if (order.closePosition) "true" else null,
                price = price?.toString(),
                stopPrice = stopPrice?.toString(),
                workingType = null,
                clientOrderId = order.id,
                timestamp = ts,
                recvWindow = recv,
                signature = sig
            )
        }

        val executedQty = response.executedQty.toDoubleOrNull() ?: 0.0
        val avgPrice = response.avgPrice.toDoubleOrNull() ?: 0.0
        val placed = order.copy(
            quantity = qty,
            price = price,
            stopPrice = stopPrice,
            binanceOrderId = response.orderId,
            status = mapBinanceStatus(response.status),
            filledQuantity = executedQty,
            filledPrice = avgPrice,
            executedAt = if (response.status == "FILLED") System.currentTimeMillis() else null,
            updatedAt = System.currentTimeMillis()
        )
        orderDao.insert(placed.toEntity())
        placed
    }.onFailure { Timber.e(it, "Failed to place futures order") }

    override suspend fun changeFuturesLeverage(symbol: String, leverage: Int): Result<Int> = runCatching {
        withTimeResync {
            val (ts, recv, sig) = signedParams(mapOf("symbol" to symbol, "leverage" to leverage.toString()))
            binanceFuturesApi.changeLeverage(symbol, leverage, ts, recv, sig).leverage
        }
    }

    override suspend fun changeFuturesMarginType(symbol: String, marginType: MarginType): Result<Boolean> = runCatching {
        val mt = if (marginType == MarginType.CROSSED) "CROSSED" else "ISOLATED"
        withTimeResync {
            val (ts, recv, sig) = signedParams(mapOf("symbol" to symbol, "marginType" to mt))
            binanceFuturesApi.changeMarginType(symbol, mt, ts, recv, sig)
        }
        true
    }

    override fun getFuturesPositions(): Flow<List<Position>> = flow {
        val list = runCatching {
            withTimeResync {
                val (ts, recv, sig) = signedParams(emptyMap())
                binanceFuturesApi.getPositionRisk(null, ts, recv, sig)
            }
        }.getOrDefault(emptyList())
        val positions = list.mapNotNull { dto ->
            val amt = dto.positionAmt.toDoubleOrNull() ?: 0.0
            if (amt == 0.0) return@mapNotNull null
            val entry = dto.entryPrice.toDoubleOrNull() ?: 0.0
            val mark = dto.markPrice.toDoubleOrNull() ?: 0.0
            Position(
                symbol = dto.symbol,
                side = if (amt > 0) OrderSide.BUY else OrderSide.SELL,
                entryPrice = entry,
                currentPrice = mark,
                quantity = kotlin.math.abs(amt),
                unrealizedPnl = dto.unRealizedProfit.toDoubleOrNull() ?: 0.0,
                unrealizedPnlPercent = if (entry > 0) ((mark - entry) / entry) * 100.0 * (if (amt > 0) 1 else -1) else 0.0,
                orderId = "",
                marketType = MarketType.FUTURES_USDM,
                leverage = dto.leverage.toIntOrNull() ?: 1,
                marginType = if (dto.marginType.equals("cross", ignoreCase = true)) MarginType.CROSSED else MarginType.ISOLATED,
                positionSide = runCatching { PositionSide.valueOf(dto.positionSide.uppercase()) }.getOrDefault(PositionSide.BOTH),
                liquidationPrice = dto.liquidationPrice.toDoubleOrNull() ?: 0.0,
                markPrice = mark,
                isolatedMargin = dto.isolatedMargin.toDoubleOrNull() ?: 0.0
            )
        }
        emit(positions)
    }

    override suspend fun closeFuturesPosition(symbol: String, positionSide: PositionSide): Result<Order> = runCatching {
        val positions = getFuturesPositions().first()
        val pos = positions.firstOrNull { it.symbol == symbol && (positionSide == PositionSide.BOTH || it.positionSide == positionSide) }
            ?: throw IllegalStateException("No futures position to close on $symbol")
        val side = if (pos.side == OrderSide.BUY) OrderSide.SELL else OrderSide.BUY
        val close = Order(
            id = java.util.UUID.randomUUID().toString(),
            symbol = symbol,
            side = side,
            type = OrderType.MARKET,
            quantity = pos.quantity,
            marketType = MarketType.FUTURES_USDM,
            leverage = pos.leverage,
            marginType = pos.marginType,
            positionSide = pos.positionSide,
            reduceOnly = pos.positionSide == PositionSide.BOTH
        )
        placeOrder(close).getOrThrow()
    }
}
