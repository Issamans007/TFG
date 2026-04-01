package com.tfg.data.remote.repository

import com.tfg.data.local.dao.*
import com.tfg.data.local.mapper.EntityMapper.toDomain
import com.tfg.data.local.mapper.EntityMapper.toEntity
import com.tfg.data.remote.api.BinanceApi
import com.tfg.data.remote.interceptor.BinanceSigner
import com.tfg.domain.model.*
import com.tfg.domain.repository.TradingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class TradingRepositoryImpl @Inject constructor(
    private val binanceApi: BinanceApi,
    private val orderDao: OrderDao,
    @Named("apiSecret") private val secretProvider: () -> String
) : TradingRepository {

    override suspend fun placeOrder(order: Order): Result<Order> = runCatching {
        val timestamp = System.currentTimeMillis()
        val params = buildMap {
            put("symbol", order.symbol)
            put("side", order.side.name)
            put("type", mapOrderType(order.type))
            put("quantity", order.quantity.toString())
            order.price?.let { put("price", it.toString()) }
            order.stopPrice?.let { put("stopPrice", it.toString()) }
            if (order.type == OrderType.LIMIT) put("timeInForce", order.timeInForce.name)
            put("newClientOrderId", order.id)
            put("timestamp", timestamp.toString())
        }
        val signature = BinanceSigner.signParams(params, secretProvider())

        val response = binanceApi.placeOrder(
            symbol = order.symbol,
            side = order.side.name,
            type = mapOrderType(order.type),
            timeInForce = if (order.type == OrderType.LIMIT) order.timeInForce.name else null,
            quantity = order.quantity.toString(),
            price = order.price?.toString(),
            stopPrice = order.stopPrice?.toString(),
            clientOrderId = order.id,
            timestamp = timestamp,
            signature = signature
        )

        val fee = response.fills?.sumOf { it.commission.toDoubleOrNull() ?: 0.0 } ?: 0.0
        val filledPrice = if (response.executedQty.toDouble() > 0) {
            response.cummulativeQuoteQty.toDouble() / response.executedQty.toDouble()
        } else 0.0

        val placed = order.copy(
            binanceOrderId = response.orderId,
            status = mapBinanceStatus(response.status),
            filledQuantity = response.executedQty.toDouble(),
            filledPrice = filledPrice,
            fee = fee,
            feeAsset = response.fills?.firstOrNull()?.commissionAsset ?: "",
            executedAt = if (response.status == "FILLED") System.currentTimeMillis() else null,
            updatedAt = System.currentTimeMillis()
        )
        orderDao.insert(placed.toEntity())
        placed
    }.onFailure { Timber.e(it, "Failed to place order") }

    override suspend fun cancelOrder(orderId: String, symbol: String): Result<Boolean> = runCatching {
        val orderEntity = orderDao.getOrderById(orderId).first()
            ?: throw IllegalStateException("Order $orderId not found in local DB")
        val binanceOrderId = orderEntity.binanceOrderId
            ?: throw IllegalStateException("Order $orderId has no Binance order ID")
        val timestamp = System.currentTimeMillis()
        val params = mapOf(
            "symbol" to symbol,
            "orderId" to binanceOrderId.toString(),
            "timestamp" to timestamp.toString()
        )
        val signature = BinanceSigner.signParams(params, secretProvider())
        binanceApi.cancelOrder(symbol, binanceOrderId, timestamp, signature)
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
        val timestamp = System.currentTimeMillis()
        val params = mapOf("timestamp" to timestamp.toString())
        val signature = BinanceSigner.signParams(params, secretProvider())

        // 1. Cancel all open limit/stop orders on Binance
        val openOrders = binanceApi.getOpenOrders(null, timestamp, signature)
        openOrders.forEach { o ->
            val cancelParams = mapOf(
                "symbol" to o.symbol,
                "orderId" to o.orderId.toString(),
                "timestamp" to System.currentTimeMillis().toString()
            )
            val cancelSig = BinanceSigner.signParams(cancelParams, secretProvider())
            try {
                binanceApi.cancelOrder(o.symbol, o.orderId, System.currentTimeMillis(), cancelSig)
            } catch (e: Exception) {
                Timber.w(e, "Failed to cancel order ${o.orderId} on ${o.symbol}")
            }
        }

        // 2. Sell any non-stablecoin balances to close actual positions
        val closedOrders = mutableListOf<Order>()
        val acctTimestamp = System.currentTimeMillis()
        val acctParams = mapOf("timestamp" to acctTimestamp.toString())
        val acctSig = BinanceSigner.signParams(acctParams, secretProvider())
        val account = binanceApi.getAccount(acctTimestamp, acctSig)
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

    override suspend fun placeOcoOrder(
        symbol: String, side: OrderSide, quantity: Double,
        price: Double, stopPrice: Double, stopLimitPrice: Double
    ): Result<List<Order>> = runCatching {
        val timestamp = System.currentTimeMillis()
        val params = mapOf(
            "symbol" to symbol, "side" to side.name,
            "quantity" to quantity.toString(), "price" to price.toString(),
            "stopPrice" to stopPrice.toString(),
            "stopLimitPrice" to stopLimitPrice.toString(),
            "stopLimitTimeInForce" to "GTC",
            "timestamp" to timestamp.toString()
        )
        val signature = BinanceSigner.signParams(params, secretProvider())
        val response = binanceApi.placeOcoOrder(
            symbol, side.name, quantity.toString(), price.toString(),
            stopPrice.toString(), stopLimitPrice.toString(), "GTC", timestamp, signature
        )
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

    override suspend fun placeBracketOrder(
        symbol: String, side: OrderSide, quantity: Double,
        entryPrice: Double, takeProfitPrice: Double, stopLossPrice: Double
    ): Result<List<Order>> = runCatching {
        val entryOrder = Order(
            id = java.util.UUID.randomUUID().toString(),
            symbol = symbol, side = side, type = OrderType.LIMIT,
            quantity = quantity, price = entryPrice,
            takeProfits = listOf(TakeProfit("tp1", takeProfitPrice, 100.0)),
            stopLosses = listOf(StopLoss("sl1", stopLossPrice, 100.0))
        )
        val result = placeOrder(entryOrder)
        listOf(result.getOrThrow())
    }

    override suspend fun reconcileWithBinance(): Result<Boolean> = runCatching {
        val timestamp = System.currentTimeMillis()
        val params = mapOf("timestamp" to timestamp.toString())
        val signature = BinanceSigner.signParams(params, secretProvider())
        val account = binanceApi.getAccount(timestamp, signature)
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
}
