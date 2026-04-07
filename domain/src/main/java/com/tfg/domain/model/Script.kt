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
    val sortinoRatio: Double = 0.0,
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

/** Export backtest results as CSV text (C5). */
fun BacktestResult.toCsv(): String {
    val df = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US)
    val sb = StringBuilder()
    // Summary section
    sb.appendLine("# Backtest Report")
    sb.appendLine("Symbol,$symbol")
    sb.appendLine("Timeframe,$timeframe")
    sb.appendLine("Period,${df.format(java.util.Date(startDate))} - ${df.format(java.util.Date(endDate))}")
    sb.appendLine("Total Trades,$totalTrades")
    sb.appendLine("Win Rate,${String.format("%.2f", winRate)}%")
    sb.appendLine("Total PnL,${String.format("%.2f", totalPnl)}")
    sb.appendLine("Max Drawdown,${String.format("%.2f", maxDrawdown)}%")
    sb.appendLine("Sharpe,${String.format("%.2f", sharpeRatio)}")
    sb.appendLine("Sortino,${String.format("%.2f", sortinoRatio)}")
    sb.appendLine("Profit Factor,${if (profitFactor == Double.MAX_VALUE) "Inf" else String.format("%.2f", profitFactor)}")
    sb.appendLine("Expectancy,${String.format("%.2f", expectancy)}")
    sb.appendLine("Buy & Hold,${String.format("%.2f", buyAndHoldReturn)}%")
    sb.appendLine()
    // Trades section
    sb.appendLine("EntryTime,ExitTime,Side,EntryPrice,ExitPrice,Quantity,PnL,Fees,BarsInTrade")
    trades.forEach { t ->
        sb.appendLine("${df.format(java.util.Date(t.entryTime))},${df.format(java.util.Date(t.exitTime))},${t.side},${t.entryPrice},${t.exitPrice},${t.quantity},${String.format("%.4f", t.pnl)},${String.format("%.4f", t.fees)},${t.barsInTrade}")
    }
    return sb.toString()
}

/** C4: Monte Carlo simulation result — confidence intervals on equity outcomes. */
data class MonteCarloResult(
    val simulations: Int,
    val medianFinalEquity: Double,
    val p5FinalEquity: Double,
    val p95FinalEquity: Double,
    val medianMaxDrawdown: Double,
    val p5MaxDrawdown: Double,       // worst-case (higher DD)
    val p95MaxDrawdown: Double       // best-case (lower DD)
)

/** Run Monte Carlo simulation by shuffling trade PnLs. */
fun BacktestResult.runMonteCarlo(simulations: Int = 1000): MonteCarloResult {
    if (trades.isEmpty()) return MonteCarloResult(simulations, startingCapital, startingCapital, startingCapital, 0.0, 0.0, 0.0)
    val pnls = trades.map { it.pnl }
    val random = java.util.Random(42)
    val finalEquities = mutableListOf<Double>()
    val maxDrawdowns = mutableListOf<Double>()

    repeat(simulations) {
        val shuffled = pnls.toMutableList().also { list ->
            for (i in list.indices.reversed()) {
                val j = random.nextInt(i + 1)
                val tmp = list[i]; list[i] = list[j]; list[j] = tmp
            }
        }
        var equity = startingCapital
        var peak = equity
        var maxDd = 0.0
        for (pnl in shuffled) {
            equity += pnl
            peak = maxOf(peak, equity)
            val dd = if (peak > 0) (peak - equity) / peak * 100.0 else 0.0
            maxDd = maxOf(maxDd, dd)
        }
        finalEquities.add(equity)
        maxDrawdowns.add(maxDd)
    }

    finalEquities.sort()
    maxDrawdowns.sort()
    fun percentile(sorted: List<Double>, p: Double): Double {
        val idx = (p / 100.0 * (sorted.size - 1)).toInt().coerceIn(0, sorted.size - 1)
        return sorted[idx]
    }
    return MonteCarloResult(
        simulations = simulations,
        medianFinalEquity = percentile(finalEquities, 50.0),
        p5FinalEquity = percentile(finalEquities, 5.0),
        p95FinalEquity = percentile(finalEquities, 95.0),
        medianMaxDrawdown = percentile(maxDrawdowns, 50.0),
        p5MaxDrawdown = percentile(maxDrawdowns, 95.0),   // 95th percentile of DD = worst case
        p95MaxDrawdown = percentile(maxDrawdowns, 5.0)    // 5th percentile of DD = best case
    )
}

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
