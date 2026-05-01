package com.tfg.domain.model

import kotlin.math.abs

/**
 * Merges a [TradingPlan] onto a raw strategy signal, producing a fully-resolved
 * BUY/SELL with concrete SL/TP, market type, leverage, and size.
 *
 * Rules:
 * - Signal SL/TP overrides plan if user code set them explicitly.
 * - Plan SL/TP fills in when signal didn't provide.
 * - Market/leverage/margin: signal first, else plan.
 * - Returns null for SELL when plan disallows shorts on SPOT or `allowShort=false`.
 */
object PlanApplier {

    /**
     * @param atrAbs Absolute ATR value (in price units) at signal time. If null/<=0
     *   and plan SL mode is ATR, falls back to a 1% pct SL.
     * @param entryPrice Reference price (current/close at signal bar).
     * @return resolved signal, or null if plan blocks the trade
     */
    fun apply(
        signal: StrategySignal,
        plan: TradingPlan,
        atrAbs: Double?,
        entryPrice: Double,
        nowEpochMs: Long
    ): StrategySignal {
        if (entryPrice <= 0.0) return signal
        // Session gating
        if (!plan.isHourAllowed(nowEpochMs)) {
            return when (signal) {
                is StrategySignal.BUY, is StrategySignal.SELL -> StrategySignal.HOLD
                else -> signal
            }
        }
        return when (signal) {
            is StrategySignal.BUY -> applyToBuy(signal, plan, atrAbs, entryPrice)
            is StrategySignal.SELL -> applyToSell(signal, plan, atrAbs, entryPrice)
            else -> signal
        }
    }

    private fun applyToBuy(s: StrategySignal.BUY, plan: TradingPlan, atrAbs: Double?, price: Double): StrategySignal.BUY {
        val slPct = s.stopLossPct ?: computeSlPct(plan, atrAbs, price)
        val tpLevels = if (s.takeProfitLevels.isNotEmpty()) s.takeProfitLevels
            else computeTpLevelsLong(plan, slPct)
        val tpFallback = s.takeProfitPct ?: tpLevels.firstOrNull()?.pct
        // Plan is authoritative for market/leverage/margin: it already includes
        // the user's choice from PlanDialog and any backtest-dialog override.
        return s.copy(
            sizePct = if (s.sizePct == 2.0) plan.sizePct else s.sizePct,
            stopLossPct = slPct,
            takeProfitPct = tpFallback,
            takeProfitLevels = tpLevels,
            marketType = plan.marketType,
            leverage = plan.leverage,
            marginType = plan.marginType
        )
    }

    private fun applyToSell(s: StrategySignal.SELL, plan: TradingPlan, atrAbs: Double?, price: Double): StrategySignal {
        // Plan is authoritative — strategy-side default (e.g. 'SPOT') must NOT
        // override the user's backtest-dialog pick of FUTURES_USDM.
        val mt = plan.marketType
        // Spot can never short. Futures shorts only when plan allows.
        if (mt == MarketType.SPOT) return StrategySignal.CLOSE_IF_LONG
        if (mt == MarketType.FUTURES_USDM && !plan.allowShort) return StrategySignal.CLOSE_IF_LONG
        val slPct = s.stopLossPct ?: computeSlPct(plan, atrAbs, price)
        val tpLevels = if (s.takeProfitLevels.isNotEmpty()) s.takeProfitLevels
            else computeTpLevelsShort(plan, slPct)
        val tpFallback = s.takeProfitPct ?: tpLevels.firstOrNull()?.pct
        return s.copy(
            sizePct = if (s.sizePct == 100.0) plan.sizePct else s.sizePct,
            stopLossPct = slPct,
            takeProfitPct = tpFallback,
            takeProfitLevels = tpLevels,
            marketType = mt,
            leverage = plan.leverage,
            marginType = plan.marginType
        )
    }

    /** Compute SL distance as % of entry price from plan. */
    private fun computeSlPct(plan: TradingPlan, atrAbs: Double?, price: Double): Double {
        return when (plan.slMode) {
            TradingPlan.SlMode.PCT -> abs(plan.slValue)
            TradingPlan.SlMode.FIXED_OFFSET -> if (price > 0) abs(plan.slValue) / price * 100.0 else 1.0
            TradingPlan.SlMode.ATR -> {
                if (atrAbs != null && atrAbs > 0.0 && price > 0.0) {
                    (atrAbs * abs(plan.slValue)) / price * 100.0
                } else 1.0
            }
        }
    }

    /** Long: TP at price * (1 + tp%/100). */
    private fun computeTpLevelsLong(plan: TradingPlan, slPct: Double): List<TpSlLevel> {
        return plan.tpLevels.map { lv ->
            val tpPct = when (plan.tpMode) {
                TradingPlan.TpMode.R_MULTIPLE -> slPct * lv.value
                TradingPlan.TpMode.PCT -> abs(lv.value)
            }
            TpSlLevel(pct = tpPct, quantityPct = lv.qtyPct)
        }
    }

    /** Short: same magnitude (sign handled by direction in engine). */
    private fun computeTpLevelsShort(plan: TradingPlan, slPct: Double): List<TpSlLevel> = computeTpLevelsLong(plan, slPct)
}
