package com.tfg.domain.usecase.trading

import com.tfg.domain.model.*
import com.tfg.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class PlaceOrderUseCase @Inject constructor(
    private val tradingRepository: TradingRepository,
    private val riskRepository: RiskRepository,
    private val portfolioRepository: PortfolioRepository,
    private val feeRepository: FeeRepository,
    private val auditRepository: AuditRepository
) {
    suspend operator fun invoke(order: Order): Result<Order> {
        val portfolio = portfolioRepository.getPortfolio().first()
        val riskCheck = riskRepository.checkOrderRisk(order, portfolio)
        if (!riskCheck.allowed) {
            val msg = riskCheck.violations.joinToString("; ") { it.message }
            auditRepository.log(
                AuditLog(
                    id = java.util.UUID.randomUUID().toString(),
                    action = AuditAction.RISK_VIOLATION,
                    category = AuditCategory.TRADING,
                    details = "Order blocked: $msg",
                    symbol = order.symbol,
                    userId = ""
                )
            )
            return Result.failure(SecurityException("Risk check failed: $msg"))
        }

        val feeConfig = feeRepository.getFeeConfig().first()
        val notional = order.quantity * (order.price ?: 0.0) * order.leverage.coerceAtLeast(1)
        val estimatedFee = feeConfig.estimateFee(
            notional,
            order.type == OrderType.LIMIT,
            order.marketType
        )
        val orderWithFee = order.copy(fee = estimatedFee)

        val result = if (order.isPaperTrade) {
            tradingRepository.placePaperOrder(orderWithFee)
        } else {
            tradingRepository.placeOrder(orderWithFee)
        }

        result.onSuccess { placed ->
            auditRepository.log(
                AuditLog(
                    id = java.util.UUID.randomUUID().toString(),
                    action = AuditAction.ORDER_PLACED,
                    category = AuditCategory.TRADING,
                    details = "${placed.side} ${placed.type} ${placed.symbol} qty=${placed.quantity}",
                    orderId = placed.id,
                    symbol = placed.symbol,
                    userId = ""
                )
            )
            feeRepository.recordFee(
                FeeRecord(
                    id = java.util.UUID.randomUUID().toString(),
                    orderId = placed.id,
                    symbol = placed.symbol,
                    feeAmount = estimatedFee,
                    feeAsset = placed.feeAsset.ifEmpty { "USDT" },
                    feeType = FeeType.TRADING
                )
            )
        }
        return result
    }
}

class CancelOrderUseCase @Inject constructor(
    private val tradingRepository: TradingRepository,
    private val auditRepository: AuditRepository
) {
    suspend operator fun invoke(orderId: String, symbol: String): Result<Boolean> {
        val result = tradingRepository.cancelOrder(orderId, symbol)
        result.onSuccess {
            auditRepository.log(
                AuditLog(
                    id = java.util.UUID.randomUUID().toString(),
                    action = AuditAction.ORDER_CANCELLED,
                    category = AuditCategory.TRADING,
                    details = "Cancelled order $orderId",
                    orderId = orderId,
                    symbol = symbol,
                    userId = ""
                )
            )
        }
        return result
    }
}

class CloseAllPositionsUseCase @Inject constructor(
    private val tradingRepository: TradingRepository,
    private val auditRepository: AuditRepository
) {
    suspend operator fun invoke(): Result<List<Order>> {
        auditRepository.log(
            AuditLog(
                id = java.util.UUID.randomUUID().toString(),
                action = AuditAction.ORDER_EMERGENCY_CLOSED,
                category = AuditCategory.TRADING,
                details = "Emergency close all positions triggered",
                userId = ""
            )
        )
        return tradingRepository.closeAllPositions()
    }
}

class ExecuteSignalUseCase @Inject constructor(
    private val signalRepository: SignalRepository,
    private val placeOrderUseCase: PlaceOrderUseCase,
    private val auditRepository: AuditRepository
) {
    suspend operator fun invoke(signalId: String, isPaper: Boolean = false): Result<Order> {
        val signal = signalRepository.getSignalById(signalId).first()
            ?: return Result.failure(IllegalStateException("Signal not found"))

        if (!signal.isValid()) {
            signalRepository.skipSignal(signalId)
            return Result.failure(IllegalStateException("Signal expired"))
        }

        val order = Order(
            id = java.util.UUID.randomUUID().toString(),
            signalId = signal.id,
            symbol = signal.symbol,
            side = signal.side,
            type = OrderType.LIMIT,
            executionMode = ExecutionMode.SIGNAL,
            quantity = 0.0,
            price = signal.entryPrice,
            takeProfits = signal.takeProfits,
            stopLosses = signal.stopLosses,
            isPaperTrade = isPaper
        )

        auditRepository.log(
            AuditLog(
                id = java.util.UUID.randomUUID().toString(),
                action = AuditAction.SIGNAL_EXECUTED,
                category = AuditCategory.SIGNAL,
                details = "Executing signal ${signal.id} for ${signal.symbol}",
                symbol = signal.symbol,
                userId = ""
            )
        )

        return placeOrderUseCase(order)
    }
}

class GetOrderHistoryUseCase @Inject constructor(private val tradingRepository: TradingRepository) {
    operator fun invoke(limit: Int = 50): Flow<List<Order>> =
        tradingRepository.getOrderHistory(limit)
}

class GetOpenOrdersUseCase @Inject constructor(private val tradingRepository: TradingRepository) {
    operator fun invoke(): Flow<List<Order>> = tradingRepository.getOpenOrders()
}
