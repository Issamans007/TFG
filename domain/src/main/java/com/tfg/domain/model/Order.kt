package com.tfg.domain.model

data class Order(
    val id: String,
    val signalId: String? = null,
    val symbol: String,
    val side: OrderSide,
    val type: OrderType,
    val status: OrderStatus = OrderStatus.PENDING,
    val executionMode: ExecutionMode = ExecutionMode.MANUAL,
    val quantity: Double,
    val price: Double? = null,
    val stopPrice: Double? = null,
    val takeProfits: List<TakeProfit> = emptyList(),
    val stopLosses: List<StopLoss> = emptyList(),
    val trailingStopPercent: Double? = null,
    val trailingStopActivationPrice: Double? = null,
    val ocoLinkedOrderId: String? = null,
    val bracketParentId: String? = null,
    val timeInForce: TimeInForce = TimeInForce.GTC,
    val scheduledAt: Long? = null,
    val conditionalTrigger: ConditionalTrigger? = null,
    val filledQuantity: Double = 0.0,
    val filledPrice: Double = 0.0,
    val fee: Double = 0.0,
    val feeAsset: String = "",
    val donationAmount: Double = 0.0,
    val realizedPnl: Double = 0.0,
    val slippage: Double = 0.0,
    val isPaperTrade: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val executedAt: Long? = null,
    val closedAt: Long? = null,
    val binanceOrderId: Long? = null,
    val errorMessage: String? = null,
    // ── Futures-specific (backward-compatible; ignored for spot) ──
    val marketType: MarketType = MarketType.SPOT,
    val leverage: Int = 1,
    val marginType: MarginType = MarginType.ISOLATED,
    val positionSide: PositionSide = PositionSide.BOTH,
    val reduceOnly: Boolean = false,
    val closePosition: Boolean = false
)

data class ConditionalTrigger(
    val triggerSymbol: String,
    val condition: TriggerCondition,
    val triggerPrice: Double,
    val thenOrderType: OrderType = OrderType.MARKET,
    val thenPrice: Double? = null
)

enum class TriggerCondition {
    PRICE_ABOVE,
    PRICE_BELOW,
    PRICE_CROSSES_ABOVE,
    PRICE_CROSSES_BELOW
}

data class TakeProfit(
    val id: String,
    val price: Double,
    val quantityPercent: Double,
    val isHit: Boolean = false,
    val hitAt: Long? = null
)

data class StopLoss(
    val id: String,
    val price: Double,
    val quantityPercent: Double = 100.0,
    val isHit: Boolean = false,
    val hitAt: Long? = null
)

enum class OrderSide { BUY, SELL }

enum class OrderType {
    MARKET, LIMIT, STOP_LIMIT, STOP_MARKET,
    OCO, BRACKET, CONDITIONAL, TRAILING_STOP,
    SCALE, POST_ONLY, TIME_BASED
}

enum class OrderStatus {
    NEW, PENDING, QUEUED_OFFLINE, SUBMITTED, PARTIALLY_FILLED,
    FILLED, CANCELLED, REJECTED, EXPIRED, EMERGENCY_CLOSED
}

enum class ExecutionMode { MANUAL, BOT, SCRIPT, SIGNAL, PAPER, LIVE }

enum class TimeInForce { GTC, IOC, FOK }

/** Market type for the order. SPOT is default; FUTURES_USDM uses USDT-margined perps on fapi.binance.com. */
enum class MarketType { SPOT, FUTURES_USDM }

/** Margin model for futures positions. Ignored for spot. */
enum class MarginType { ISOLATED, CROSSED }

/** Position side. BOTH = one-way mode. LONG/SHORT = hedge mode. Spot orders always use BOTH. */
enum class PositionSide { BOTH, LONG, SHORT }
