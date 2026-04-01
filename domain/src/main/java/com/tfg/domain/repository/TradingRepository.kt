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

    suspend fun placeOcoOrder(symbol: String, side: OrderSide, quantity: Double,
                              price: Double, stopPrice: Double,
                              stopLimitPrice: Double): Result<List<Order>>

    suspend fun placeBracketOrder(symbol: String, side: OrderSide, quantity: Double,
                                   entryPrice: Double, takeProfitPrice: Double,
                                   stopLossPrice: Double): Result<List<Order>>

    suspend fun reconcileWithBinance(): Result<Boolean>
}
