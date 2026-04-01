package com.tfg.domain.model

data class AnalyticsSnapshot(
    val totalReturnPercent: Double = 0.0,
    val totalReturnAmount: Double = 0.0,
    val winRate: Double = 0.0,
    val totalTrades: Int = 0,
    val winningTrades: Int = 0,
    val losingTrades: Int = 0,
    val profitFactor: Double = 0.0,
    val sharpeRatio: Double = 0.0,
    val sortinoRatio: Double = 0.0,
    val calmarRatio: Double = 0.0,
    val maxDrawdownPercent: Double = 0.0,
    val maxDrawdownAmount: Double = 0.0,
    val avgTradeDurationMs: Long = 0,
    val avgHoldTimeMinutes: Long = 0,
    val avgWin: Double = 0.0,
    val avgLoss: Double = 0.0,
    val payoffRatio: Double = 0.0,
    val bestTradePnl: Double = 0.0,
    val worstTradePnl: Double = 0.0,
    val longestWinStreak: Int = 0,
    val longestLossStreak: Int = 0,
    val totalFeesPaid: Double = 0.0,
    val totalDonations: Double = 0.0,
    val periodStart: Long = 0,
    val periodEnd: Long = 0,
    val isPaperMode: Boolean = false
)

data class PairPerformance(
    val symbol: String,
    val totalTrades: Int,
    val trades: Int = totalTrades,
    val winRate: Double,
    val totalPnl: Double,
    val avgPnl: Double,
    val totalFees: Double
)

data class StrategyPerformance(
    val strategyName: String,
    val executionMode: ExecutionMode,
    val totalTrades: Int,
    val winRate: Double,
    val totalPnl: Double,
    val sharpeRatio: Double,
    val maxDrawdown: Double
)

data class EquityPoint(
    val timestamp: Long,
    val equity: Double,
    val drawdown: Double = 0.0
)

data class DailyPnlEntry(
    val date: Long,
    val pnl: Double,
    val cumulativePnl: Double,
    val tradeCount: Int
)
