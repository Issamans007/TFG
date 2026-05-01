package com.tfg.domain.repository

import com.tfg.domain.model.*
import kotlinx.coroutines.flow.Flow

interface TradingRepository {
    suspend fun placeOrder(order: Order): Result<Order>
    suspend fun cancelOrder(orderId: String, symbol: String): Result<Boolean>
    suspend fun closePosition(symbol: String): Result<Order>
    suspend fun closeAllPositions(): Result<List<Order>>

    fun getOpenOrders(): Flow<List<Order>>
    fun getOrderHistory(limit: Int = 50): Flow<List<Order>>
    fun getOrderById(orderId: String): Flow<Order?>

    suspend fun placePaperOrder(order: Order): Result<Order>
    suspend fun cancelPaperOrder(orderId: String): Result<Boolean>

    /** Poll Binance for the current status/fill info of a placed order. */
    suspend fun queryOrder(symbol: String, clientOrderId: String): Result<Order>

    suspend fun placeOcoOrder(symbol: String, side: OrderSide, quantity: Double,
                              price: Double, stopPrice: Double,
                              stopLimitPrice: Double): Result<List<Order>>

    suspend fun placeBracketOrder(symbol: String, side: OrderSide, quantity: Double,
                                   entryPrice: Double, takeProfitPrice: Double,
                                   stopLossPrice: Double): Result<List<Order>>

    suspend fun reconcileWithBinance(): Result<Boolean>

    // ── Futures (USDⓈ-M) ───────────────────────────────────────────
    /** Change leverage for a futures symbol. Returns the actual leverage applied. */
    suspend fun changeFuturesLeverage(symbol: String, leverage: Int): Result<Int> =
        Result.failure(UnsupportedOperationException("Futures not supported"))

    /** Change margin type for a futures symbol. */
    suspend fun changeFuturesMarginType(symbol: String, marginType: MarginType): Result<Boolean> =
        Result.failure(UnsupportedOperationException("Futures not supported"))

    /** Get current futures positions from /fapi/v2/positionRisk. */
    fun getFuturesPositions(): Flow<List<Position>> = kotlinx.coroutines.flow.flowOf(emptyList())

    /** Close an open futures position with a market reduce-only order. */
    suspend fun closeFuturesPosition(symbol: String, positionSide: PositionSide = PositionSide.BOTH): Result<Order> =
        Result.failure(UnsupportedOperationException("Futures not supported"))
}
