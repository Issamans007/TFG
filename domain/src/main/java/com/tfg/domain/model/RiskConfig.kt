package com.tfg.domain.model

data class RiskConfig(
    val dailyLossLimitPercent: Double = 5.0,
    val maxPositionSizePercent: Double = 10.0,
    val maxOpenTrades: Int = 5,
    val symbolExposureLimitPercent: Double = 20.0,
    val maxSlippagePercent: Double = 0.5,
    val maxSpreadPercent: Double = 0.3,
    val breakEvenAfterTpCount: Int = 1,
    val volatilityPauseThreshold: Double = 5.0,
    val consecutiveLossLimit: Int = 3,
    val maxLossPerTradePercent: Double = 2.0,
    val drawdownPausePercent: Double = 10.0,
    val correlationLimit: Int = 2,
    val tradingWindowStart: String? = null,
    val tradingWindowEnd: String? = null,
    val fearGreedPauseBelow: Int = 20,
    val fearGreedPauseAbove: Int = 80,
    val weekendTradingEnabled: Boolean = true,
    val killSwitchEnabled: Boolean = false,
    val emergencyCloseAll: Boolean = false
)

data class RiskCheckResult(
    val allowed: Boolean,
    val violations: List<RiskViolation> = emptyList()
)

data class RiskViolation(
    val rule: String,
    val message: String,
    val severity: RiskSeverity
)

enum class RiskSeverity { WARNING, BLOCK, EMERGENCY }
