package com.tfg.engine

import android.content.Context
import com.tfg.domain.model.*
import com.tfg.domain.repository.RiskRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RiskEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : RiskRepository {

    private companion object {
        const val PREFS_NAME = "tfg_risk_engine"
        const val KEY_KILL_SWITCH = "kill_switch_active"
        const val KEY_DAILY_LOSS = "daily_loss"
        const val KEY_CONSECUTIVE_LOSSES = "consecutive_losses"
        const val KEY_PEAK_EQUITY = "peak_equity"
        const val KEY_LAST_RESET_DAY = "last_reset_day"
        /** Maximum allowed age for cachedPortfolio before risk checks must reject. */
        const val MAX_PORTFOLIO_AGE_MS = 30_000L
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _config = MutableStateFlow(RiskConfig())
    private val _killSwitch = MutableStateFlow(prefs.getBoolean(KEY_KILL_SWITCH, false))
    // Money stored as String-encoded BigDecimal: SharedPreferences only has
    // Float/Long primitives, and Float.MAX precision (~7 sig figs) loses cents
    // on accounts > ~$1M. Read existing Float-encoded values once for migration.
    private fun loadDoubleMoney(key: String): Double {
        val asString = prefs.getString(key, null)
        if (asString != null) return asString.toBigDecimalOrNull()?.toDouble() ?: 0.0
        // Legacy Float fallback (will be re-persisted as String on next write).
        return runCatching { prefs.getFloat(key, 0f).toDouble() }.getOrDefault(0.0)
    }

    private var dailyLoss = loadDoubleMoney(KEY_DAILY_LOSS)
    private var openTradeCount = 0
    private var consecutiveLosses = prefs.getInt(KEY_CONSECUTIVE_LOSSES, 0)
    private var peakEquity = loadDoubleMoney(KEY_PEAK_EQUITY)
    private var cachedPortfolio = Portfolio()
    @Volatile private var cachedPortfolioTimestampMs: Long = 0L
    private var lastAtrPercent = 0.0

    init {
        // Auto-reset daily counters if the day changed since last save.
        // Use UTC because Binance daily limits reset at 00:00 UTC; using local
        // time would either grant a fresh limit mid-trading-day after travel
        // or, conversely, reset hours before the exchange does.
        val today = utcDayOfYear()
        val savedDay = prefs.getInt(KEY_LAST_RESET_DAY, -1)
        if (savedDay != -1 && savedDay != today) {
            dailyLoss = 0.0
            consecutiveLosses = 0
            persistRiskState()
        }
    }

    private fun utcDayOfYear(): Int {
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        // Encode year * 1000 + day so a year rollover (Dec 31 → Jan 1) still
        // triggers a reset rather than matching the previous January.
        return cal.get(java.util.Calendar.YEAR) * 1000 + cal.get(java.util.Calendar.DAY_OF_YEAR)
    }

    // Observable state for UI
    private val _dailyLossFlow = MutableStateFlow(0.0)
    val dailyLossFlow: StateFlow<Double> = _dailyLossFlow.asStateFlow()
    private val _openTradeCountFlow = MutableStateFlow(0)
    val openTradeCountFlow: StateFlow<Int> = _openTradeCountFlow.asStateFlow()
    private val _consecutiveLossesFlow = MutableStateFlow(0)
    val consecutiveLossesFlow: StateFlow<Int> = _consecutiveLossesFlow.asStateFlow()

    /** Push real portfolio so risk checks use actual balances instead of zeros. */
    fun updatePortfolio(portfolio: Portfolio) {
        cachedPortfolio = portfolio
        cachedPortfolioTimestampMs = System.currentTimeMillis()
        updateEquity(portfolio.totalBalance)
    }

    /** Push latest ATR% so volatility pause can be checked. */
    fun updateVolatility(atrPercent: Double) {
        lastAtrPercent = atrPercent
    }

    override fun getRiskConfig(): Flow<RiskConfig> = _config.asStateFlow()

    /** Synchronous snapshot for non-suspend callers (e.g. TradeExecutor TP/SL checks). */
    fun getRiskConfigSnapshot(): RiskConfig = _config.value

    override suspend fun updateRiskConfig(config: RiskConfig) {
        _config.value = config
    }

    override suspend fun checkOrderRisk(order: Order, portfolio: Portfolio): RiskCheckResult {
        val config = _config.value
        val violations = mutableListOf<RiskViolation>()

        if (_killSwitch.value) {
            violations.add(RiskViolation("KILL_SWITCH", "Kill switch is active. All trading halted.", RiskSeverity.EMERGENCY))
            return RiskCheckResult(false, violations)
        }

        if (config.emergencyCloseAll) {
            violations.add(RiskViolation("EMERGENCY", "Emergency close all is active.", RiskSeverity.EMERGENCY))
            return RiskCheckResult(false, violations)
        }

        val effectivePrice = order.price
            ?: order.filledPrice.takeIf { it > 0.0 }
            ?: order.stopPrice
            ?: 0.0
        val orderNotional = order.quantity * effectivePrice
        val positionSizePercent = if (portfolio.totalBalance > 0) orderNotional / portfolio.totalBalance * 100 else 100.0

        // Max position size check — block if price unknown (market order with no reference)
        if (effectivePrice <= 0.0 && order.type == OrderType.MARKET) {
            violations.add(RiskViolation(
                "MAX_POSITION_SIZE",
                "Cannot determine position size for market order with unknown price",
                RiskSeverity.BLOCK
            ))
        } else if (positionSizePercent > config.maxPositionSizePercent) {
            violations.add(RiskViolation(
                "MAX_POSITION_SIZE",
                "Position size ${String.format("%.1f", positionSizePercent)}% exceeds limit ${config.maxPositionSizePercent}%",
                RiskSeverity.BLOCK
            ))
        }

        // Max open trades check
        if (openTradeCount >= config.maxOpenTrades) {
            violations.add(RiskViolation(
                "MAX_OPEN_TRADES",
                "Open trades ($openTradeCount) at limit (${config.maxOpenTrades})",
                RiskSeverity.BLOCK
            ))
        }

        // Daily loss limit check
        val dailyLossPercent = if (portfolio.totalBalance > 0) kotlin.math.abs(dailyLoss) / portfolio.totalBalance * 100 else 0.0
        if (dailyLossPercent >= config.dailyLossLimitPercent) {
            violations.add(RiskViolation(
                "DAILY_LOSS_LIMIT",
                "Daily loss ${String.format("%.1f", dailyLossPercent)}% exceeds limit ${config.dailyLossLimitPercent}%",
                RiskSeverity.BLOCK
            ))
        }

        // Max loss per trade check
        if (order.stopLosses.isNotEmpty() && order.price != null) {
            val worstSl = order.stopLosses.minByOrNull { it.price }
            if (worstSl != null) {
                val orderPrice = order.price ?: 0.0
                val maxLoss = kotlin.math.abs(orderPrice - worstSl.price) * order.quantity
                val lossPercent = if (portfolio.totalBalance > 0) maxLoss / portfolio.totalBalance * 100 else 100.0
                if (lossPercent > config.maxLossPerTradePercent) {
                    violations.add(RiskViolation(
                        "MAX_LOSS_PER_TRADE",
                        "Potential loss ${String.format("%.1f", lossPercent)}% exceeds limit ${config.maxLossPerTradePercent}%",
                        RiskSeverity.BLOCK
                    ))
                }
            }
        }

        // Consecutive loss check
        if (consecutiveLosses >= config.consecutiveLossLimit) {
            violations.add(RiskViolation(
                "CONSECUTIVE_LOSSES",
                "Hit $consecutiveLosses consecutive losses (limit ${config.consecutiveLossLimit})",
                RiskSeverity.BLOCK
            ))
        }

        // Drawdown check
        if (peakEquity > 0) {
            val drawdown = (peakEquity - portfolio.totalBalance) / peakEquity * 100
            if (drawdown >= config.drawdownPausePercent) {
                violations.add(RiskViolation(
                    "DRAWDOWN_PAUSE",
                    "Drawdown ${String.format("%.1f", drawdown)}% exceeds pause threshold ${config.drawdownPausePercent}%",
                    RiskSeverity.BLOCK
                ))
            }
        }

        // Time-based trading window check (supports overnight windows like 23:00-01:00)
        if (config.tradingWindowStart != null && config.tradingWindowEnd != null) {
            val now = java.util.Calendar.getInstance()
            val currentMinutes = now.get(java.util.Calendar.HOUR_OF_DAY) * 60 + now.get(java.util.Calendar.MINUTE)

            fun parseMinutes(hhmm: String): Int? {
                // Tolerate malformed config (saved by older builds, manually edited
                // prefs, etc.) instead of crashing the whole risk check.
                return try {
                    val parts = hhmm.split(":")
                    if (parts.size != 2) return null
                    val h = parts[0].toIntOrNull() ?: return null
                    val m = parts[1].toIntOrNull() ?: return null
                    if (h !in 0..23 || m !in 0..59) null else h * 60 + m
                } catch (_: Exception) { null }
            }

            val startMinutes = parseMinutes(config.tradingWindowStart!!)
            val endMinutes = parseMinutes(config.tradingWindowEnd!!)

            // Bad config = skip the time-window restriction rather than block
            // every trade outright; the rest of the risk checks still apply.
            if (startMinutes != null && endMinutes != null) {
                val insideWindow = if (startMinutes <= endMinutes) {
                    // Normal window, e.g. 09:00-17:00
                    currentMinutes in startMinutes..endMinutes
                } else {
                    // Overnight window, e.g. 23:00-01:00
                    currentMinutes >= startMinutes || currentMinutes <= endMinutes
                }

                if (!insideWindow) {
                    violations.add(RiskViolation(
                        "TRADING_WINDOW",
                        "Outside trading window (${config.tradingWindowStart}-${config.tradingWindowEnd})",
                        RiskSeverity.BLOCK
                    ))
                }
            }
        }

        // Weekend check
        if (!config.weekendTradingEnabled) {
            val dayOfWeek = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)
            if (dayOfWeek == java.util.Calendar.SATURDAY || dayOfWeek == java.util.Calendar.SUNDAY) {
                violations.add(RiskViolation(
                    "WEEKEND_TRADING",
                    "Weekend trading is disabled",
                    RiskSeverity.BLOCK
                ))
            }
        }

        // B6: Volatility pause — block if ATR% exceeds threshold
        if (config.volatilityPauseThreshold > 0 && lastAtrPercent > config.volatilityPauseThreshold) {
            violations.add(RiskViolation(
                "VOLATILITY_PAUSE",
                "Market volatility ${String.format("%.2f", lastAtrPercent)}% exceeds pause threshold ${config.volatilityPauseThreshold}%",
                RiskSeverity.BLOCK
            ))
        }

        // Pre-trade balance check — fail BEFORE the exchange does.
        // Submitting an order Binance will reject (e.g. insufficient USDT for
        // a BUY) burns rate-limit and produces noisy error logs. Catch it here.
        if (!order.isPaperTrade && effectivePrice > 0.0) {
            val notional = orderNotional
            when (order.marketType) {
                MarketType.SPOT -> {
                    val (base, quote) = splitSpotSymbol(order.symbol)
                    val needAsset = if (order.side == OrderSide.BUY) quote else base
                    val needAmount = if (order.side == OrderSide.BUY) notional else order.quantity
                    val free = portfolio.assets
                        .firstOrNull { it.asset.equals(needAsset, ignoreCase = true) }
                        ?.free ?: 0.0
                    if (needAmount > 0 && free < needAmount) {
                        violations.add(RiskViolation(
                            "INSUFFICIENT_BALANCE",
                            "Need ${String.format("%.6f", needAmount)} $needAsset but only ${String.format("%.6f", free)} free",
                            RiskSeverity.BLOCK
                        ))
                    }
                }
                MarketType.FUTURES_USDM -> {
                    // Futures uses USDT margin. Required margin = notional / leverage.
                    val lev = (order.leverage.takeIf { it > 0 } ?: 1).coerceAtLeast(1)
                    val required = notional / lev
                    val freeUsdt = portfolio.availableBalance.takeIf { it > 0 }
                        ?: portfolio.assets.firstOrNull { it.asset.equals("USDT", ignoreCase = true) }?.free
                        ?: 0.0
                    if (required > 0 && freeUsdt < required) {
                        violations.add(RiskViolation(
                            "INSUFFICIENT_MARGIN",
                            "Need ${String.format("%.2f", required)} USDT margin (notional=${String.format("%.2f", notional)}, leverage=${lev}x) but only ${String.format("%.2f", freeUsdt)} free",
                            RiskSeverity.BLOCK
                        ))
                    }
                }
                else -> { /* paper / unsupported */ }
            }
        }

        val hasBlocker = violations.any { it.severity == RiskSeverity.BLOCK || it.severity == RiskSeverity.EMERGENCY }
        return RiskCheckResult(!hasBlocker, violations)
    }

    /** Split a Binance spot symbol like BTCUSDT → ("BTC","USDT"). Falls back to ("","") on unknown quote. */
    private fun splitSpotSymbol(symbol: String): Pair<String, String> {
        // Order matters: longer suffixes first so "FDUSD" is matched before "USD".
        val quotes = listOf("USDT", "FDUSD", "BUSD", "USDC", "TUSD", "DAI", "BTC", "ETH", "BNB", "EUR", "GBP", "TRY", "USD")
        val up = symbol.uppercase()
        for (q in quotes) {
            if (up.endsWith(q) && up.length > q.length) return up.removeSuffix(q) to q
        }
        return "" to ""
    }

    override suspend fun activateKillSwitch() {
        _killSwitch.value = true
        prefs.edit().putBoolean(KEY_KILL_SWITCH, true).apply()
        Timber.w("Kill switch ACTIVATED - all trading halted")
    }

    override suspend fun deactivateKillSwitch() {
        _killSwitch.value = false
        prefs.edit().putBoolean(KEY_KILL_SWITCH, false).apply()
        Timber.w("Kill switch DEACTIVATED")
    }

    override fun isKillSwitchActive(): Flow<Boolean> = _killSwitch.asStateFlow()

    fun updateTradeResult(pnl: Double) {
        dailyLoss += if (pnl < 0) pnl else 0.0
        _dailyLossFlow.value = dailyLoss
        if (pnl < 0) consecutiveLosses++ else consecutiveLosses = 0
        _consecutiveLossesFlow.value = consecutiveLosses
        persistRiskState()
    }

    fun updateEquity(equity: Double) {
        if (equity > peakEquity) {
            peakEquity = equity
            persistRiskState()
        }
    }

    fun updateOpenTradeCount(count: Int) {
        openTradeCount = count
        _openTradeCountFlow.value = count
    }

    fun resetDailyCounters() {
        dailyLoss = 0.0
        consecutiveLosses = 0
        _dailyLossFlow.value = 0.0
        _consecutiveLossesFlow.value = 0
        persistRiskState()
    }

    private fun persistRiskState() {
        val today = utcDayOfYear()
        prefs.edit()
            .putString(KEY_DAILY_LOSS, dailyLoss.toBigDecimal().toPlainString())
            .putInt(KEY_CONSECUTIVE_LOSSES, consecutiveLosses)
            .putString(KEY_PEAK_EQUITY, peakEquity.toBigDecimal().toPlainString())
            .putInt(KEY_LAST_RESET_DAY, today)
            .apply()
    }

    // Alias for backwards compatibility — uses cached real portfolio
    suspend fun checkPreTrade(order: Order): RiskCheckResult {
        // Fail-closed if cachedPortfolio is missing or stale — we must NOT
        // approve trades against a 0-balance default or a snapshot from
        // minutes ago that may no longer reflect available margin.
        val age = System.currentTimeMillis() - cachedPortfolioTimestampMs
        if (cachedPortfolioTimestampMs == 0L || age > MAX_PORTFOLIO_AGE_MS) {
            Timber.w("checkPreTrade: cachedPortfolio stale (age=${age}ms) — blocking order")
            return RiskCheckResult(
                allowed = false,
                violations = listOf(
                    RiskViolation(
                        "STALE_PORTFOLIO",
                        "Portfolio snapshot stale (${age}ms). Refresh and retry.",
                        RiskSeverity.BLOCK
                    )
                )
            )
        }
        return checkOrderRisk(order, cachedPortfolio)
    }

    fun recordTradeResult(order: Order) {
        val pnl = order.realizedPnl
        updateTradeResult(pnl)
        Timber.d("Recorded trade result: ${order.id}, PnL: $pnl")
    }

    fun recordLoss(order: Order) {
        val pnl = order.realizedPnl
        if (pnl < 0) {
            updateTradeResult(pnl)
            Timber.w("Recorded loss: ${order.id}, PnL: $pnl")
        }
    }
}
