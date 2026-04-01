package com.tfg.domain.model

data class Portfolio(
    val totalBalance: Double = 0.0,
    val availableBalance: Double = 0.0,
    val lockedBalance: Double = 0.0,
    val totalPnl: Double = 0.0,
    val dailyPnl: Double = 0.0,
    val weeklyPnl: Double = 0.0,
    val monthlyPnl: Double = 0.0,
    val totalDonated: Double = 0.0,
    val totalFeesPaid: Double = 0.0,
    val assets: List<AssetBalance> = emptyList(),
    val openPositions: List<Position> = emptyList(),
    val isPaperMode: Boolean = false,
    val paperBalance: Double = 10000.0,
    val updatedAt: Long = System.currentTimeMillis()
) {
    // Computed properties for UI compatibility
    val totalValueUsdt: Double get() = totalBalance
    val totalPnlUsdt: Double get() = totalPnl
    val totalPnlPercent: Double get() = if (totalBalance > 0) (totalPnl / totalBalance) * 100.0 else 0.0
    val balances: List<AssetBalance> get() = assets
    val positions: List<Position> get() = openPositions
    val trades: List<Order> get() = emptyList() // Will be filled by ViewModel from repository
    val avgHoldTimeMinutes: Long get() = 0L // Will be computed by ViewModel from trade history
}

data class AssetBalance(
    val asset: String,
    val free: Double,
    val locked: Double,
    val usdValue: Double = 0.0,
    val allocationPercent: Double = 0.0,
    val walletType: String = "SPOT"
) {
    val totalValue: Double get() = free + locked
}

data class Position(
    val symbol: String,
    val side: OrderSide,
    val entryPrice: Double,
    val currentPrice: Double = 0.0,
    val quantity: Double,
    val unrealizedPnl: Double = 0.0,
    val unrealizedPnlPercent: Double = 0.0,
    val takeProfits: List<TakeProfit> = emptyList(),
    val stopLosses: List<StopLoss> = emptyList(),
    val orderId: String,
    val openedAt: Long = System.currentTimeMillis()
)
