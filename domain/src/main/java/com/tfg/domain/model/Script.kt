package com.tfg.domain.model

data class Script(
    val id: String,
    val name: String,
    val code: String,
    val isActive: Boolean = false,
    val activeSymbol: String? = null,
    val strategyTemplateId: String? = null,
    val params: Map<String, String> = emptyMap(),
    val lastRun: Long? = null,
    val backtestResult: BacktestResult? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class StrategyTemplateId {
    SMA_CROSSOVER,
    RSI_MEAN_REVERSION,
    BOLLINGER_BREAKOUT,
    MACD_MOMENTUM,
    EMA_SCALPER,
    GRID_TRADER,
    GAINZ_ALGO,
    SUPERTREND,
    VWAP_REVERSION,
    ICHIMOKU_CLOUD,
    STOCHASTIC_CROSS,
    VOLUME_BREAKOUT,
    SCALPER_5M,
    SWING_15M,
    TREND_1H
}

data class StrategyTemplate(
    val id: StrategyTemplateId,
    val name: String,
    val description: String,
    val code: String,
    val defaultParams: Map<String, String> = emptyMap()
)

data class BacktestResult(
    val scriptId: String,
    val symbol: String,
    val timeframe: String,
    val startDate: Long,
    val endDate: Long,
    val totalTrades: Int,
    val winRate: Double,
    val totalPnl: Double,
    val maxDrawdown: Double,
    val sharpeRatio: Double,
    val trades: List<BacktestTrade> = emptyList(),
    val profitFactor: Double = 0.0,
    val buyAndHoldReturn: Double = 0.0,
    val avgBarsInTrade: Double = 0.0,
    val grossProfit: Double = 0.0,
    val grossLoss: Double = 0.0,
    val maxConsecutiveWins: Int = 0,
    val maxConsecutiveLosses: Int = 0,
    val equityCurve: List<Double> = emptyList(),
    val startingCapital: Double = 10000.0,
    val expectancy: Double = 0.0,
    val avgWin: Double = 0.0,
    val avgLoss: Double = 0.0,
    val largestWin: Double = 0.0,
    val largestLoss: Double = 0.0,
    val backtestDays: Int = 0,
    val entryAmount: Double = 100.0,
    val finalAmount: Double = 100.0
)

data class BacktestTrade(
    val entryTime: Long,
    val exitTime: Long,
    val side: OrderSide,
    val entryPrice: Double,
    val exitPrice: Double,
    val quantity: Double,
    val pnl: Double,
    val fees: Double,
    val barsInTrade: Int = 0
)

/** Persisted signal marker for chart overlay */
data class SignalMarker(
    val id: String,
    val scriptId: String,
    val symbol: String,
    val interval: String,
    val openTime: Long,
    val signalType: SignalType,
    val price: Double,
    val timestamp: Long = System.currentTimeMillis()
)

enum class SignalType { BUY, SELL, CLOSE }

// ─── Custom Template (user-created presets) ─────────────────────────────

data class CustomTemplate(
    val id: String,
    val name: String,
    val description: String,
    val baseTemplateId: String,   // maps to StrategyTemplateId.name
    val code: String,
    val defaultParams: Map<String, String> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis()
)

// ─── Param ↔ Code sync helpers ──────────────────────────────────────────

/**
 * Replace `val KEY = oldValue` in code with `val KEY = newValue`.
 * Preserves trailing comments.
 */
fun injectParamIntoCode(code: String, key: String, value: String): String {
    return code.replace(
        Regex("""((?:var|let|const|val)\s+${Regex.escape(key)}\s*=\s*)([^\s;/]+)"""),
        "$1$value"
    )
}

/**
 * Extract all UPPER_SNAKE_CASE `var` declarations with numeric values.
 * Skips commented-out lines.
 */
fun extractParamsFromCode(code: String): Map<String, String> {
    val params = mutableMapOf<String, String>()
    code.lines().forEach { line ->
        val trimmed = line.trimStart()
        if (!trimmed.startsWith("//")) {
            Regex("""(?:var|let|const|val)\s+([A-Z][A-Z0-9_]*)\s*=\s*(-?\d+\.?\d*)""").find(trimmed)?.let {
                params[it.groupValues[1]] = it.groupValues[2]
            }
        }
    }
    return params
}
