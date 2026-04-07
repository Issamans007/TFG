package com.tfg.domain.model

/**
 * Pure strategy evaluation engine — no dependencies, no mutable shared state.
 * Used by both StrategyRunner (live) and backtesting.
 */
object StrategyEvaluator {

    /**
     * Per-evaluation mutable context. Each backtest run and each live strategy
     * instance creates its own context so there is no shared mutable state.
     */
    class EvalContext {
        var lastTradeBar: Int = -10
        var lastSignalType: String = ""
    }

    fun evaluate(
        templateId: String?,
        candles: List<Candle>,
        params: Map<String, String> = emptyMap(),
        ctx: EvalContext = EvalContext()
    ): StrategySignal {
        if (candles.isEmpty()) return StrategySignal.HOLD
        return try {
            when (templateId) {
                StrategyTemplateId.SMA_CROSSOVER.name     -> evaluateSmaCrossover(candles, params)
                StrategyTemplateId.RSI_MEAN_REVERSION.name -> evaluateRsiMeanReversion(candles, params, ctx)
                StrategyTemplateId.BOLLINGER_BREAKOUT.name -> evaluateBollingerBreakout(candles, params)
                StrategyTemplateId.MACD_MOMENTUM.name      -> evaluateMacdMomentum(candles, params)
                StrategyTemplateId.EMA_SCALPER.name        -> evaluateEmaScalper(candles, params)
                StrategyTemplateId.GRID_TRADER.name        -> evaluateGridTrader(candles, params)
                StrategyTemplateId.GAINZ_ALGO.name         -> evaluateGainzAlgo(candles, params, ctx)
                StrategyTemplateId.SUPERTREND.name         -> evaluateSupertrend(candles, params)
                StrategyTemplateId.VWAP_REVERSION.name     -> evaluateVwapReversion(candles, params)
                StrategyTemplateId.ICHIMOKU_CLOUD.name     -> evaluateIchimokuCloud(candles, params)
                StrategyTemplateId.STOCHASTIC_CROSS.name   -> evaluateStochasticCross(candles, params)
                StrategyTemplateId.VOLUME_BREAKOUT.name    -> evaluateVolumeBreakout(candles, params)
                StrategyTemplateId.SCALPER_5M.name         -> evaluateScalper5m(candles, params)
                StrategyTemplateId.SWING_15M.name          -> evaluateSwing15m(candles, params)
                StrategyTemplateId.TREND_1H.name           -> evaluateTrend1h(candles, params)
                else -> StrategySignal.HOLD
            }
        } catch (_: Exception) {
            StrategySignal.HOLD
        }
    }

    // ─── Param helpers ──────────────────────────────────────────────

    private fun Map<String, String>.int(key: String, default: Int): Int =
        this[key]?.toIntOrNull() ?: default
    private fun Map<String, String>.dbl(key: String, default: Double): Double =
        this[key]?.toDoubleOrNull() ?: default

    // ═══════════════════════════════════════════════════════════════════
    //  INDICATOR FUNCTIONS (public for chart overlays)
    // ═══════════════════════════════════════════════════════════════════

    // ─── Moving Averages ────────────────────────────────────────────

    fun sma(candles: List<Candle>, period: Int): Double {
        if (candles.size < period) return candles.last().close
        return candles.takeLast(period).map { it.close }.average()
    }

    fun ema(candles: List<Candle>, period: Int): Double {
        if (candles.size < period) return candles.last().close
        val multiplier = 2.0 / (period + 1)
        var emaVal = candles.take(period).map { it.close }.average()
        for (i in period until candles.size) {
            emaVal = (candles[i].close - emaVal) * multiplier + emaVal
        }
        return emaVal
    }

    fun wma(candles: List<Candle>, period: Int): Double {
        if (candles.size < period) return candles.last().close
        val slice = candles.takeLast(period)
        val weightSum = period * (period + 1) / 2.0
        return slice.mapIndexed { i, c -> c.close * (i + 1) }.sum() / weightSum
    }

    fun vwma(candles: List<Candle>, period: Int): Double {
        if (candles.size < period) return candles.last().close
        val slice = candles.takeLast(period)
        val volSum = slice.sumOf { it.volume }
        if (volSum == 0.0) return sma(candles, period)
        return slice.sumOf { it.close * it.volume } / volSum
    }

    fun hullMa(candles: List<Candle>, period: Int): Double {
        if (candles.size < period) return candles.last().close
        val half = maxOf(period / 2, 1)
        val wmaHalf = wma(candles, half)
        val wmaFull = wma(candles, period)
        return 2.0 * wmaHalf - wmaFull
    }

    fun dema(candles: List<Candle>, period: Int): Double {
        val ema1 = ema(candles, period)
        val emaSer = emaSeries(candles, period)
        val validEma = emaSer.filterNotNull()
        if (validEma.size < period) return ema1
        val mult = 2.0 / (period + 1)
        var ema2 = validEma.take(period).average()
        for (i in period until validEma.size) {
            ema2 = (validEma[i] - ema2) * mult + ema2
        }
        return 2 * ema1 - ema2
    }

    fun tema(candles: List<Candle>, period: Int): Double {
        val ema1 = ema(candles, period)
        val ema1Series = emaSeries(candles, period).filterNotNull()
        if (ema1Series.size < period * 2) return ema1
        val mult = 2.0 / (period + 1)
        // EMA2 = EMA of EMA
        val ema2Series = mutableListOf<Double>()
        var e2 = ema1Series.take(period).average()
        ema2Series.add(e2)
        for (i in period until ema1Series.size) {
            e2 = (ema1Series[i] - e2) * mult + e2
            ema2Series.add(e2)
        }
        if (ema2Series.size < period) return ema1
        // EMA3 = EMA of EMA2
        var e3 = ema2Series.take(period).average()
        for (i in period until ema2Series.size) {
            e3 = (ema2Series[i] - e3) * mult + e3
        }
        return 3 * ema1 - 3 * e2 + e3
    }

    // ─── Oscillators ────────────────────────────────────────────────

    fun rsi(candles: List<Candle>, period: Int): Double {
        if (candles.size < period + 1) return 50.0
        val closes = candles.map { it.close }
        var avgGain = 0.0; var avgLoss = 0.0
        for (i in 1..period) {
            val change = closes[i] - closes[i - 1]
            if (change > 0) avgGain += change else avgLoss -= change
        }
        avgGain /= period; avgLoss /= period
        for (i in period + 1 until closes.size) {
            val change = closes[i] - closes[i - 1]
            avgGain = (avgGain * (period - 1) + if (change > 0) change else 0.0) / period
            avgLoss = (avgLoss * (period - 1) + if (change < 0) -change else 0.0) / period
        }
        if (avgLoss == 0.0) return 100.0
        return 100.0 - (100.0 / (1.0 + avgGain / avgLoss))
    }

    /** Stochastic Oscillator: Pair(K, D). */
    fun stochastic(candles: List<Candle>, kPeriod: Int = 14, dPeriod: Int = 3): Pair<Double, Double> {
        if (candles.size < kPeriod) return Pair(50.0, 50.0)
        val kValues = mutableListOf<Double>()
        for (i in kPeriod - 1 until candles.size) {
            val slice = candles.subList(i - kPeriod + 1, i + 1)
            val high = slice.maxOf { it.high }; val low = slice.minOf { it.low }
            kValues.add(if (high - low > 0) (candles[i].close - low) / (high - low) * 100 else 50.0)
        }
        val k = kValues.last()
        val d = if (kValues.size >= dPeriod) kValues.takeLast(dPeriod).average() else k
        return Pair(k, d)
    }

    /** Stochastic RSI: Pair(K, D). */
    fun stochasticRsi(candles: List<Candle>, rsiPeriod: Int = 14, stochPeriod: Int = 14,
                      kSmooth: Int = 3, dSmooth: Int = 3): Pair<Double, Double> {
        val rsiValues = rsiSeries(candles, rsiPeriod)
        if (rsiValues.size < stochPeriod) return Pair(50.0, 50.0)
        val stochValues = mutableListOf<Double>()
        for (i in stochPeriod - 1 until rsiValues.size) {
            val slice = rsiValues.subList(i - stochPeriod + 1, i + 1)
            val high = slice.max(); val low = slice.min()
            stochValues.add(if (high - low > 0) (rsiValues[i] - low) / (high - low) * 100 else 50.0)
        }
        val k = if (stochValues.size >= kSmooth) stochValues.takeLast(kSmooth).average()
                else stochValues.lastOrNull() ?: 50.0
        val d = if (stochValues.size >= kSmooth + dSmooth - 1) {
            val kSer = (kSmooth - 1 until stochValues.size).map { i ->
                stochValues.subList(i - kSmooth + 1, i + 1).average()
            }
            kSer.takeLast(dSmooth).average()
        } else k
        return Pair(k, d)
    }

    /** Commodity Channel Index. */
    fun cci(candles: List<Candle>, period: Int = 20): Double {
        if (candles.size < period) return 0.0
        val slice = candles.takeLast(period)
        val tps = slice.map { (it.high + it.low + it.close) / 3.0 }
        val mean = tps.average()
        val meanDev = tps.map { Math.abs(it - mean) }.average()
        return if (meanDev > 0) (tps.last() - mean) / (0.015 * meanDev) else 0.0
    }

    /** Williams %R. */
    fun williamsR(candles: List<Candle>, period: Int = 14): Double {
        if (candles.size < period) return -50.0
        val slice = candles.takeLast(period)
        val high = slice.maxOf { it.high }; val low = slice.minOf { it.low }
        return if (high - low > 0) -100.0 * (high - candles.last().close) / (high - low) else -50.0
    }

    // ─── Volume Indicators ──────────────────────────────────────────

    /** On-Balance Volume (latest value). */
    fun obv(candles: List<Candle>): Double {
        if (candles.size < 2) return 0.0
        var obvVal = 0.0
        for (i in 1 until candles.size) {
            obvVal += when {
                candles[i].close > candles[i - 1].close -> candles[i].volume
                candles[i].close < candles[i - 1].close -> -candles[i].volume
                else -> 0.0
            }
        }
        return obvVal
    }

    /** OBV series for chart overlay. */
    fun obvSeries(candles: List<Candle>): List<Double> {
        if (candles.size < 2) return candles.map { 0.0 }
        val result = mutableListOf(0.0)
        for (i in 1 until candles.size) {
            result.add(result.last() + when {
                candles[i].close > candles[i - 1].close ->  candles[i].volume
                candles[i].close < candles[i - 1].close -> -candles[i].volume
                else -> 0.0
            })
        }
        return result
    }

    /** Volume Weighted Average Price (cumulative session). */
    fun vwap(candles: List<Candle>): Double {
        if (candles.isEmpty()) return 0.0
        var cumVol = 0.0; var cumTpVol = 0.0
        for (c in candles) {
            val tp = (c.high + c.low + c.close) / 3.0
            cumTpVol += tp * c.volume; cumVol += c.volume
        }
        return if (cumVol > 0) cumTpVol / cumVol else candles.last().close
    }

    /** Money Flow Index (volume-weighted RSI). */
    fun mfi(candles: List<Candle>, period: Int = 14): Double {
        if (candles.size < period + 1) return 50.0
        var posFlow = 0.0; var negFlow = 0.0
        for (i in candles.size - period until candles.size) {
            val tp  = (candles[i].high + candles[i].low + candles[i].close) / 3.0
            val ptp = (candles[i-1].high + candles[i-1].low + candles[i-1].close) / 3.0
            val raw = tp * candles[i].volume
            if (tp > ptp) posFlow += raw else if (tp < ptp) negFlow += raw
        }
        if (negFlow == 0.0) return 100.0
        return 100.0 - (100.0 / (1.0 + posFlow / negFlow))
    }

    /** Chaikin Money Flow. */
    fun cmf(candles: List<Candle>, period: Int = 20): Double {
        if (candles.size < period) return 0.0
        val slice = candles.takeLast(period)
        var mfVol = 0.0; var totVol = 0.0
        for (c in slice) {
            val clv = if (c.high != c.low) ((c.close - c.low) - (c.high - c.close)) / (c.high - c.low) else 0.0
            mfVol += clv * c.volume; totVol += c.volume
        }
        return if (totVol > 0) mfVol / totVol else 0.0
    }

    // ─── Trend Indicators ───────────────────────────────────────────

    fun atr(candles: List<Candle>, period: Int): Double {
        if (candles.size < period + 1) return 0.0
        val trueRanges = mutableListOf<Double>()
        for (i in 1 until candles.size) {
            val h = candles[i].high; val l = candles[i].low; val pc = candles[i-1].close
            trueRanges.add(maxOf(h - l, Math.abs(h - pc), Math.abs(l - pc)))
        }
        return trueRanges.takeLast(period).average()
    }

    /** ADX with correct Wilder smoothing. */
    fun adx(candles: List<Candle>, period: Int): Double {
        if (candles.size < period * 2 + 1) return 0.0
        val trList = mutableListOf<Double>()
        val plusDmList = mutableListOf<Double>()
        val minusDmList = mutableListOf<Double>()
        for (i in 1 until candles.size) {
            val h = candles[i].high; val l = candles[i].low
            val pc = candles[i-1].close; val ph = candles[i-1].high; val pl = candles[i-1].low
            trList.add(maxOf(h - l, Math.abs(h - pc), Math.abs(l - pc)))
            val up = h - ph; val down = pl - l
            plusDmList.add(if (up > down && up > 0) up else 0.0)
            minusDmList.add(if (down > up && down > 0) down else 0.0)
        }
        fun wilderSmooth(data: List<Double>, p: Int): List<Double> {
            if (data.size < p) return emptyList()
            val r = mutableListOf(data.take(p).sum())
            for (i in p until data.size) { r.add(r.last() - r.last() / p + data[i]) }
            return r
        }
        val sTr = wilderSmooth(trList, period)
        val sPDm = wilderSmooth(plusDmList, period)
        val sMDm = wilderSmooth(minusDmList, period)
        if (sTr.isEmpty()) return 0.0
        val diPlus = sPDm.zip(sTr).map { (d, t) -> if (t > 0) 100.0 * d / t else 0.0 }
        val diMinus = sMDm.zip(sTr).map { (d, t) -> if (t > 0) 100.0 * d / t else 0.0 }
        val dx = diPlus.zip(diMinus).map { (p, m) ->
            val s = p + m; if (s > 0) 100.0 * Math.abs(p - m) / s else 0.0
        }
        if (dx.size < period) return dx.lastOrNull() ?: 0.0
        var adxVal = dx.take(period).average()
        for (i in period until dx.size) { adxVal = (adxVal * (period - 1) + dx[i]) / period }
        return adxVal
    }

    /** Supertrend: Pair(line, isUptrend). */
    fun supertrend(candles: List<Candle>, period: Int = 10, multiplier: Double = 3.0): Pair<Double, Boolean> {
        if (candles.size < period + 2) return Pair(candles.last().close, true)
        // ATR via Wilder smoothing
        val trList = (1 until candles.size).map { i ->
            val h = candles[i].high; val l = candles[i].low; val pc = candles[i-1].close
            maxOf(h - l, Math.abs(h - pc), Math.abs(l - pc))
        }
        if (trList.size < period) return Pair(candles.last().close, true)
        val atrSmooth = mutableListOf(trList.take(period).average())
        for (i in period until trList.size) {
            atrSmooth.add((atrSmooth.last() * (period - 1) + trList[i]) / period)
        }
        var st = candles[period].close
        var isUp = true
        for (i in atrSmooth.indices) {
            val ci = i + 1
            if (ci >= candles.size) break
            val hl2 = (candles[ci].high + candles[ci].low) / 2.0
            val ub = hl2 + multiplier * atrSmooth[i]
            val lb = hl2 - multiplier * atrSmooth[i]
            if (candles[ci].close > st) { st = lb; isUp = true } else { st = ub; isUp = false }
        }
        return Pair(st, isUp)
    }

    /** Ichimoku Cloud components. */
    data class IchimokuResult(
        val tenkan: Double, val kijun: Double,
        val senkouA: Double, val senkouB: Double, val chikou: Double
    )

    fun ichimoku(candles: List<Candle>, tenkan: Int = 9, kijun: Int = 26, senkouB: Int = 52): IchimokuResult {
        fun mid(data: List<Candle>, p: Int): Double {
            if (data.size < p) return data.last().close
            val s = data.takeLast(p); return (s.maxOf { it.high } + s.minOf { it.low }) / 2.0
        }
        val t = mid(candles, tenkan); val k = mid(candles, kijun)
        return IchimokuResult(t, k, (t + k) / 2.0, mid(candles, senkouB), candles.last().close)
    }

    /** Parabolic SAR. */
    fun parabolicSar(candles: List<Candle>, afStart: Double = 0.02, afStep: Double = 0.02, afMax: Double = 0.2): Double {
        if (candles.size < 3) return candles.last().close
        var isLong = candles[1].close > candles[0].close
        var sar = if (isLong) candles[0].low else candles[0].high
        var ep = if (isLong) candles[1].high else candles[1].low
        var af = afStart
        for (i in 2 until candles.size) {
            sar += af * (ep - sar)
            if (isLong) {
                sar = minOf(sar, candles[i-1].low, candles[i-2].low)
                if (candles[i].low < sar) { isLong = false; sar = ep; ep = candles[i].low; af = afStart }
                else if (candles[i].high > ep) { ep = candles[i].high; af = minOf(af + afStep, afMax) }
            } else {
                sar = maxOf(sar, candles[i-1].high, candles[i-2].high)
                if (candles[i].high > sar) { isLong = true; sar = ep; ep = candles[i].high; af = afStart }
                else if (candles[i].low < ep) { ep = candles[i].low; af = minOf(af + afStep, afMax) }
            }
        }
        return sar
    }

    // ─── Channel Indicators ─────────────────────────────────────────

    /** Keltner Channels: Triple(upper, middle, lower). */
    fun keltnerChannels(candles: List<Candle>, emaPeriod: Int = 20, atrPeriod: Int = 10,
                        mult: Double = 2.0): Triple<Double, Double, Double> {
        val mid = ema(candles, emaPeriod); val a = atr(candles, atrPeriod)
        return Triple(mid + mult * a, mid, mid - mult * a)
    }

    /** Donchian Channels: Triple(upper, middle, lower). */
    fun donchianChannels(candles: List<Candle>, period: Int = 20): Triple<Double, Double, Double> {
        if (candles.size < period) return Triple(candles.last().high, candles.last().close, candles.last().low)
        val s = candles.takeLast(period)
        val u = s.maxOf { it.high }; val l = s.minOf { it.low }
        return Triple(u, (u + l) / 2.0, l)
    }

    // ─── Momentum Indicators ────────────────────────────────────────

    /** Rate of Change. */
    fun roc(candles: List<Candle>, period: Int = 12): Double {
        if (candles.size <= period) return 0.0
        val cur = candles.last().close; val past = candles[candles.size - 1 - period].close
        return if (past != 0.0) (cur - past) / past * 100 else 0.0
    }

    /** TRIX (triple-smoothed EMA percentage change). */
    fun trix(candles: List<Candle>, period: Int = 15): Double {
        val e1 = emaSeries(candles, period).filterNotNull()
        if (e1.size < period * 2) return 0.0
        val m = 2.0 / (period + 1)
        val e2 = mutableListOf<Double>()
        var v2 = e1.take(period).average(); e2.add(v2)
        for (i in period until e1.size) { v2 = (e1[i] - v2) * m + v2; e2.add(v2) }
        if (e2.size < period) return 0.0
        val e3 = mutableListOf<Double>()
        var v3 = e2.take(period).average(); e3.add(v3)
        for (i in period until e2.size) { v3 = (e2[i] - v3) * m + v3; e3.add(v3) }
        if (e3.size < 2) return 0.0
        return if (e3[e3.size - 2] != 0.0) (e3.last() - e3[e3.size - 2]) / e3[e3.size - 2] * 100 else 0.0
    }

    /** Aroon: Pair(up, down). */
    fun aroon(candles: List<Candle>, period: Int = 25): Pair<Double, Double> {
        if (candles.size < period + 1) return Pair(50.0, 50.0)
        val s = candles.takeLast(period + 1)
        val hi = s.indices.maxByOrNull { s[it].high } ?: period
        val lo = s.indices.minByOrNull { s[it].low } ?: period
        return Pair((period.toDouble() - (period - hi)) / period * 100,
                    (period.toDouble() - (period - lo)) / period * 100)
    }

    // ─── Statistical ────────────────────────────────────────────────

    fun stdDev(values: List<Double>): Double {
        val avg = values.average()
        return Math.sqrt(values.map { (it - avg) * (it - avg) }.average())
    }

    /** Linear regression: Triple(slope, intercept, rSquared). */
    fun linearRegression(values: List<Double>): Triple<Double, Double, Double> {
        if (values.size < 2) return Triple(0.0, values.firstOrNull() ?: 0.0, 0.0)
        val n = values.size.toDouble(); val xm = (n - 1) / 2.0; val ym = values.average()
        var ssxy = 0.0; var ssxx = 0.0; var sstot = 0.0
        for (i in values.indices) {
            ssxy += (i - xm) * (values[i] - ym)
            ssxx += (i - xm) * (i - xm)
            sstot += (values[i] - ym) * (values[i] - ym)
        }
        val slope = if (ssxx > 0) ssxy / ssxx else 0.0
        val intercept = ym - slope * xm
        val ssres = values.indices.sumOf { i -> val p = slope * i + intercept; (values[i] - p) * (values[i] - p) }
        val r2 = if (sstot > 0) 1.0 - ssres / sstot else 0.0
        return Triple(slope, intercept, r2)
    }

    /** Z-Score of latest close. */
    fun zScore(candles: List<Candle>, period: Int = 20): Double {
        if (candles.size < period) return 0.0
        val closes = candles.takeLast(period).map { it.close }
        val sd = stdDev(closes)
        return if (sd > 0) (candles.last().close - closes.average()) / sd else 0.0
    }

    /** Pivot Points: Standard. */
    data class PivotResult(val pivot: Double, val r1: Double, val r2: Double, val s1: Double, val s2: Double)
    fun pivotPoints(candles: List<Candle>): PivotResult {
        val prev = if (candles.size >= 2) candles[candles.size - 2] else candles.last()
        val p = (prev.high + prev.low + prev.close) / 3.0
        return PivotResult(p, 2 * p - prev.low, p + (prev.high - prev.low), 2 * p - prev.high, p - (prev.high - prev.low))
    }

    // ─── Series Functions ───────────────────────────────────────────

    fun rsiSeries(candles: List<Candle>, period: Int): List<Double> {
        return candles.indices.map { i ->
            if (i < period) 50.0 else rsi(candles.subList(0, i + 1), period)
        }
    }

    fun smaSeries(candles: List<Candle>, period: Int): List<Double?> {
        return candles.indices.map { i ->
            if (i + 1 < period) null
            else candles.subList(0, i + 1).takeLast(period).map { it.close }.average()
        }
    }

    fun emaSeries(candles: List<Candle>, period: Int): List<Double?> {
        if (candles.size < period) return candles.map { null }
        val mult = 2.0 / (period + 1)
        val result = mutableListOf<Double?>()
        for (i in 0 until period - 1) result.add(null)
        var emaVal = candles.take(period).map { it.close }.average()
        result.add(emaVal)
        for (i in period until candles.size) {
            emaVal = (candles[i].close - emaVal) * mult + emaVal; result.add(emaVal)
        }
        return result
    }

    fun bollingerSeries(candles: List<Candle>, period: Int = 20, mult: Double = 2.0):
            Triple<List<Double?>, List<Double?>, List<Double?>> {
        val upper = mutableListOf<Double?>(); val middle = mutableListOf<Double?>(); val lower = mutableListOf<Double?>()
        for (i in candles.indices) {
            if (i + 1 < period) { upper.add(null); middle.add(null); lower.add(null) }
            else {
                val s = candles.subList(i + 1 - period, i + 1).map { it.close }
                val m = s.average(); val sd = stdDev(s)
                upper.add(m + mult * sd); middle.add(m); lower.add(m - mult * sd)
            }
        }
        return Triple(upper, middle, lower)
    }

    fun macdSeries(candles: List<Candle>, fast: Int = 12, slow: Int = 26, signal: Int = 9):
            Triple<List<Double?>, List<Double?>, List<Double?>> {
        val ef = emaSeries(candles, fast); val es = emaSeries(candles, slow)
        val macdLine = ef.zip(es).map { (f, s) -> if (f != null && s != null) f - s else null }
        val nonNull = macdLine.filterNotNull()
        if (nonNull.size < signal) return Triple(macdLine, macdLine.map { null }, macdLine.map { null })
        val mult = 2.0 / (signal + 1)
        val signalLine = mutableListOf<Double?>()
        var idx = 0; var ev: Double? = null
        for (v in macdLine) {
            if (v == null) { signalLine.add(null); continue }
            idx++
            if (idx < signal) { signalLine.add(null); continue }
            if (idx == signal) { ev = macdLine.filterNotNull().take(signal).average() }
            else { ev = (v - ev!!) * mult + ev }
            signalLine.add(ev)
        }
        val hist = macdLine.zip(signalLine).map { (m, s) -> if (m != null && s != null) m - s else null }
        return Triple(macdLine, signalLine, hist)
    }

    fun atrSeries(candles: List<Candle>, period: Int): List<Double?> {
        return candles.indices.map { i ->
            if (i < period) null else atr(candles.subList(0, i + 1), period)
        }
    }

    // ─── New Series Functions for Chart ─────────────────────────────

    fun wmaSeries(candles: List<Candle>, period: Int): List<Double?> {
        return candles.indices.map { i ->
            if (i + 1 < period) null else wma(candles.subList(0, i + 1), period)
        }
    }

    fun vwmaSeries(candles: List<Candle>, period: Int): List<Double?> {
        return candles.indices.map { i ->
            if (i + 1 < period) null else vwma(candles.subList(0, i + 1), period)
        }
    }

    fun hullMaSeries(candles: List<Candle>, period: Int): List<Double?> {
        return candles.indices.map { i ->
            if (i + 1 < period) null else hullMa(candles.subList(0, i + 1), period)
        }
    }

    fun demaSeries(candles: List<Candle>, period: Int): List<Double?> {
        return candles.indices.map { i ->
            if (i + 1 < period * 2) null else dema(candles.subList(0, i + 1), period)
        }
    }

    fun temaSeries(candles: List<Candle>, period: Int): List<Double?> {
        return candles.indices.map { i ->
            if (i + 1 < period * 3) null else tema(candles.subList(0, i + 1), period)
        }
    }

    fun vwapSeries(candles: List<Candle>): List<Double> {
        var cumVol = 0.0; var cumTpVol = 0.0
        return candles.map { c ->
            val tp = (c.high + c.low + c.close) / 3.0
            cumTpVol += tp * c.volume; cumVol += c.volume
            if (cumVol > 0) cumTpVol / cumVol else c.close
        }
    }

    /** Supertrend series: returns Pair(greenLine, redLine) — each list has Double? per candle. */
    fun supertrendSeries(candles: List<Candle>, period: Int = 10, multiplier: Double = 3.0):
            Pair<List<Double?>, List<Double?>> {
        val n = candles.size
        val green = MutableList<Double?>(n) { null }
        val red = MutableList<Double?>(n) { null }
        if (n < period + 2) return Pair(green, red)

        val trList = (1 until n).map { i ->
            val h = candles[i].high; val l = candles[i].low; val pc = candles[i - 1].close
            maxOf(h - l, Math.abs(h - pc), Math.abs(l - pc))
        }
        val atrSmooth = mutableListOf(trList.take(period).average())
        for (i in period until trList.size) {
            atrSmooth.add((atrSmooth.last() * (period - 1) + trList[i]) / period)
        }

        var st = candles[period].close
        var isUp = true
        for (i in atrSmooth.indices) {
            val ci = i + 1
            if (ci >= n) break
            val hl2 = (candles[ci].high + candles[ci].low) / 2.0
            val ub = hl2 + multiplier * atrSmooth[i]
            val lb = hl2 - multiplier * atrSmooth[i]
            if (candles[ci].close > st) { st = lb; isUp = true } else { st = ub; isUp = false }
            if (isUp) green[ci] = st else red[ci] = st
        }
        return Pair(green, red)
    }

    /** Ichimoku series: returns 5 lists (tenkan, kijun, senkouA, senkouB, chikou). */
    data class IchimokuSeriesResult(
        val tenkan: List<Double?>,
        val kijun: List<Double?>,
        val senkouA: List<Double?>,
        val senkouB: List<Double?>,
        val chikou: List<Double?>
    )

    fun ichimokuSeries(candles: List<Candle>, tenkanP: Int = 9, kijunP: Int = 26, senkouBP: Int = 52): IchimokuSeriesResult {
        val n = candles.size
        fun mid(end: Int, p: Int): Double? {
            if (end + 1 < p) return null
            val s = candles.subList(end + 1 - p, end + 1)
            return (s.maxOf { it.high } + s.minOf { it.low }) / 2.0
        }
        val tenkan = (0 until n).map { mid(it, tenkanP) }
        val kijun = (0 until n).map { mid(it, kijunP) }
        val senkouA = tenkan.zip(kijun).map { (t, k) -> if (t != null && k != null) (t + k) / 2.0 else null }
        val senkouB = (0 until n).map { mid(it, senkouBP) }
        val chikou = candles.mapIndexed { i, c -> if (i + kijunP < n) c.close else null }
        return IchimokuSeriesResult(tenkan, kijun, senkouA, senkouB, chikou)
    }

    /** Stochastic series: Pair(K line, D line). */
    fun stochasticSeries(candles: List<Candle>, kPeriod: Int = 14, dPeriod: Int = 3):
            Pair<List<Double?>, List<Double?>> {
        val n = candles.size
        val rawK = MutableList<Double?>(n) { null }
        for (i in kPeriod - 1 until n) {
            val slice = candles.subList(i - kPeriod + 1, i + 1)
            val high = slice.maxOf { it.high }; val low = slice.minOf { it.low }
            rawK[i] = if (high - low > 0) (candles[i].close - low) / (high - low) * 100 else 50.0
        }
        val dLine = MutableList<Double?>(n) { null }
        for (i in kPeriod - 1 + dPeriod - 1 until n) {
            val window = (i - dPeriod + 1..i).mapNotNull { rawK[it] }
            if (window.size == dPeriod) dLine[i] = window.average()
        }
        return Pair(rawK, dLine)
    }

    /** Stochastic RSI series: Pair(K, D). */
    fun stochasticRsiSeries(candles: List<Candle>, rsiPeriod: Int = 14, stochPeriod: Int = 14,
                            kSmooth: Int = 3, dSmooth: Int = 3): Pair<List<Double?>, List<Double?>> {
        val rsiVals = rsiSeries(candles, rsiPeriod)
        val n = rsiVals.size
        val stochRaw = MutableList<Double?>(n) { null }
        for (i in stochPeriod - 1 until n) {
            val slice = rsiVals.subList(i - stochPeriod + 1, i + 1)
            val high = slice.max(); val low = slice.min()
            stochRaw[i] = if (high - low > 0) (rsiVals[i] - low) / (high - low) * 100 else 50.0
        }
        // Smooth K
        val kLine = MutableList<Double?>(n) { null }
        for (i in 0 until n) {
            if (i < kSmooth - 1) continue
            val window = (i - kSmooth + 1..i).mapNotNull { stochRaw[it] }
            if (window.size == kSmooth) kLine[i] = window.average()
        }
        // Smooth D
        val dLine = MutableList<Double?>(n) { null }
        for (i in 0 until n) {
            if (i < dSmooth - 1) continue
            val window = (i - dSmooth + 1..i).mapNotNull { kLine[it] }
            if (window.size == dSmooth) dLine[i] = window.average()
        }
        return Pair(kLine, dLine)
    }

    fun cciSeries(candles: List<Candle>, period: Int = 20): List<Double?> {
        return candles.indices.map { i ->
            if (i + 1 < period) null else cci(candles.subList(0, i + 1), period)
        }
    }

    fun williamsRSeries(candles: List<Candle>, period: Int = 14): List<Double?> {
        return candles.indices.map { i ->
            if (i + 1 < period) null else williamsR(candles.subList(0, i + 1), period)
        }
    }

    fun mfiSeries(candles: List<Candle>, period: Int = 14): List<Double?> {
        return candles.indices.map { i ->
            if (i + 1 < period + 1) null else mfi(candles.subList(0, i + 1), period)
        }
    }

    fun cmfSeries(candles: List<Candle>, period: Int = 20): List<Double?> {
        return candles.indices.map { i ->
            if (i + 1 < period) null else cmf(candles.subList(0, i + 1), period)
        }
    }

    fun adxSeries(candles: List<Candle>, period: Int = 14): List<Double?> {
        return candles.indices.map { i ->
            if (i + 1 < period * 2 + 1) null else adx(candles.subList(0, i + 1), period)
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  STRATEGY EVALUATORS
    // ═══════════════════════════════════════════════════════════════════

    private fun evaluateSmaCrossover(candles: List<Candle>, params: Map<String, String>): StrategySignal {
        val fast = params.int("FAST_PERIOD", 10); val slow = params.int("SLOW_PERIOD", 50)
        val rsiPeriod = params.int("RSI_PERIOD", 14); val rsiOb = params.dbl("RSI_OVERBOUGHT", 70.0)
        val size = params.dbl("POSITION_SIZE_PCT", 2.0)
        if (candles.size < slow + 1) return StrategySignal.HOLD
        val fSma = sma(candles, fast); val pfSma = sma(candles.dropLast(1), fast)
        val sSma = sma(candles, slow); val psSma = sma(candles.dropLast(1), slow)
        val r = rsi(candles, rsiPeriod)
        if (pfSma <= psSma && fSma > sSma && r < rsiOb) return StrategySignal.BUY(sizePct = size)
        if ((pfSma >= psSma && fSma < sSma) || r > rsiOb + 5) return StrategySignal.SELL(sizePct = 100.0)
        return StrategySignal.HOLD
    }

    private fun evaluateRsiMeanReversion(candles: List<Candle>, params: Map<String, String>, ctx: EvalContext): StrategySignal {
        val rsiPeriod = params.int("RSI_PERIOD", 14)
        val rsiOs = params.dbl("RSI_OVERSOLD", 28.0); val rsiOb = params.dbl("RSI_OVERBOUGHT", 72.0)
        val volMult = params.dbl("VOL_MULTIPLIER", 1.5); val cooldown = params.int("COOLDOWN_BARS", 5)
        val size = params.dbl("POSITION_SIZE_PCT", 3.0)
        val sl = params.dbl("STOP_LOSS_PCT", 2.0); val tp = params.dbl("TAKE_PROFIT_PCT", 4.0)
        if (candles.size < rsiPeriod + 7) return StrategySignal.HOLD
        val currentBar = candles.size
        if (currentBar - ctx.lastTradeBar < cooldown) return StrategySignal.HOLD
        val r = rsi(candles, rsiPeriod)
        val avgVol = candles.takeLast(20).map { it.volume }.average()
        val spike = candles.last().volume > avgVol * volMult
        if (r < rsiOs && spike) {
            ctx.lastTradeBar = currentBar
            return StrategySignal.BUY(sizePct = size, stopLossPct = sl, takeProfitPct = tp)
        }
        if (r > rsiOb) {
            ctx.lastTradeBar = currentBar
            return StrategySignal.SELL(sizePct = 100.0)
        }
        return StrategySignal.HOLD
    }

    private fun evaluateBollingerBreakout(candles: List<Candle>, params: Map<String, String>): StrategySignal {
        val bbP = params.int("BB_PERIOD", 20); val bbSd = params.dbl("BB_STDDEV", 2.0)
        val sqTh = params.dbl("SQUEEZE_THRESHOLD", 0.02)
        val size = params.dbl("POSITION_SIZE_PCT", 2.5)
        val sl = params.dbl("STOP_LOSS_PCT", 1.5); val tp = params.dbl("TAKE_PROFIT_PCT", 3.0)
        if (candles.size < bbP + 2) return StrategySignal.HOLD
        val closes = candles.map { it.close }
        val smaV = closes.takeLast(bbP).average(); val sd = stdDev(closes.takeLast(bbP))
        val upper = smaV + bbSd * sd; val lower = smaV - bbSd * sd
        val prevC = candles.dropLast(1).map { it.close }
        val prevSma = prevC.takeLast(bbP).average(); val prevSd = stdDev(prevC.takeLast(bbP))
        val prevBw = (2 * bbSd * prevSd) / prevSma
        val wasSq = prevBw < sqTh; val price = candles.last().close
        if (wasSq && price > upper) return StrategySignal.BUY(sizePct = size, stopLossPct = sl, takeProfitPct = tp)
        if (wasSq && price < lower) return StrategySignal.SELL(sizePct = 100.0)
        if (price < smaV && price > lower) return StrategySignal.CLOSE_IF_LONG
        return StrategySignal.HOLD
    }

    private fun evaluateMacdMomentum(candles: List<Candle>, params: Map<String, String>): StrategySignal {
        val fastE = params.int("FAST_EMA", 12); val slowE = params.int("SLOW_EMA", 26)
        val trendP = params.int("TREND_EMA", 200)
        val size = params.dbl("POSITION_SIZE_PCT", 2.0)
        val sl = params.dbl("STOP_LOSS_PCT", 2.5); val tp = params.dbl("TAKE_PROFIT_PCT", 5.0)
        if (candles.size < trendP + 1) return StrategySignal.HOLD
        val macd = ema(candles, fastE) - ema(candles, slowE)
        val prevMacd = ema(candles.dropLast(1), fastE) - ema(candles.dropLast(1), slowE)
        val trendEma = ema(candles, trendP); val price = candles.last().close
        if (prevMacd <= 0 && macd > 0 && price > trendEma) return StrategySignal.BUY(sizePct = size, stopLossPct = sl, takeProfitPct = tp)
        if (prevMacd >= 0 && macd < 0) return StrategySignal.SELL(sizePct = 100.0)
        return StrategySignal.HOLD
    }

    private fun evaluateEmaScalper(candles: List<Candle>, params: Map<String, String>): StrategySignal {
        val fast = params.int("EMA_FAST", 8); val mid = params.int("EMA_MID", 13); val slow = params.int("EMA_SLOW", 21)
        val atrP = params.int("ATR_PERIOD", 14); val slM = params.dbl("ATR_SL_MULT", 1.0); val tpM = params.dbl("ATR_TP_MULT", 1.5)
        val minAdx = params.dbl("MIN_ADX", 20.0); val size = params.dbl("POSITION_SIZE_PCT", 1.5)
        if (candles.size < slow + 14) return StrategySignal.HOLD
        val ef = ema(candles, fast); val em = ema(candles, mid); val es = ema(candles, slow)
        val a = atr(candles, atrP); val adxV = adx(candles, atrP); val price = candles.last().close
        if (adxV < minAdx) return StrategySignal.HOLD
        if (ef > em && em > es && price > ef) return StrategySignal.BUY(sizePct = size,
            stopLossPct = if (price > 0) a * slM / price * 100 else 1.0,
            takeProfitPct = if (price > 0) a * tpM / price * 100 else 1.5)
        if (ef < em && em < es && price < ef) return StrategySignal.SELL(sizePct = 100.0)
        return StrategySignal.HOLD
    }

    private fun evaluateGainzAlgo(candles: List<Candle>, params: Map<String, String>, ctx: EvalContext): StrategySignal {
        // ── Core params ──
        val stabTh = params.dbl("CANDLE_STABILITY", 0.5)
        val rsiP = params.int("RSI_PERIOD", 14)
        val rsiBuyMax = params.dbl("RSI_BUY_MAX", 55.0)
        val rsiSellMin = params.dbl("RSI_SELL_MIN", 45.0)
        val deltaLen = params.int("DELTA_LENGTH", 4)

        // ── Trend filter ──
        val useTrend = params.int("USE_TREND_FILTER", 1) == 1
        val emaFastLen = params.int("EMA_FAST", 50)
        val emaSlowLen = params.int("EMA_SLOW", 200)

        // ── Volume filter ──
        val useVol = params.int("USE_VOLUME_FILTER", 1) == 1
        val volLookback = params.int("VOL_LOOKBACK", 20)
        val volMult = params.dbl("VOL_MULTIPLIER", 1.0)

        // ── MACD filter ──
        val useMacd = params.int("USE_MACD_FILTER", 0) == 1
        val macdFast = params.int("MACD_FAST", 12)
        val macdSlow = params.int("MACD_SLOW", 26)

        // ── ADX filter ──
        val useAdx = params.int("USE_ADX_FILTER", 0) == 1
        val adxPeriod = params.int("ADX_PERIOD", 14)
        val adxThresh = params.dbl("ADX_THRESHOLD", 20.0)

        // ── Risk ──
        val tpSlM = params.dbl("TP_SL_MULTIPLIER", 1.0)
        val rrr = params.dbl("RISK_REWARD_RATIO", 2.0)
        val size = params.dbl("POSITION_SIZE_PCT", 2.0)

        // Only require EMA_SLOW bars when trend filter is on
        val minBars = if (useTrend) maxOf(emaSlowLen + 5, 52) else maxOf(deltaLen + 15, 30)
        if (candles.size < minBars) return StrategySignal.HOLD

        val last = candles.last(); val prev = candles[candles.size - 2]
        val deltaRef = candles[candles.size - 1 - deltaLen]
        val price = last.close

        // ── Candle stability ──
        val tr = maxOf(last.high - last.low, Math.abs(last.high - prev.close), Math.abs(last.low - prev.close))
        val stable = tr > 0.0 && Math.abs(last.close - last.open) / tr > stabTh
        if (!stable) return StrategySignal.HOLD

        // ── Engulfing patterns ──
        val bullEng = prev.close < prev.open && last.close > last.open && last.close > prev.open
        val bearEng = prev.close > prev.open && last.close < last.open && last.close < prev.open
        if (!bullEng && !bearEng) return StrategySignal.HOLD

        // ── RSI ──
        val r = rsi(candles, rsiP)

        // ── Trend filter: EMA alignment only (allows pullback entries) ──
        var trendBull = true; var trendBear = true
        if (useTrend) {
            val ef = ema(candles, emaFastLen); val es = ema(candles, emaSlowLen)
            // Only require EMA alignment — do NOT require price > EMA
            // This allows "buy the dip in an uptrend" (engulfing + dip + uptrend)
            trendBull = ef > es
            trendBear = ef < es
        }

        // ── Volume confirmation ──
        if (useVol && candles.size > volLookback + 1) {
            val avgVol = candles.subList(candles.size - volLookback - 1, candles.size - 1).map { it.volume }.average()
            if (avgVol > 0 && last.volume < avgVol * volMult) return StrategySignal.HOLD
        }

        // ── MACD histogram direction ──
        var macdBull = true; var macdBear = true
        if (useMacd) {
            val macdNow = ema(candles, macdFast) - ema(candles, macdSlow)
            val macdPrev = ema(candles.dropLast(1), macdFast) - ema(candles.dropLast(1), macdSlow)
            macdBull = macdNow > macdPrev
            macdBear = macdNow < macdPrev
        }

        // ── ADX strength ──
        if (useAdx && adx(candles, adxPeriod) < adxThresh) return StrategySignal.HOLD

        // ── TP/SL ──
        val a = atr(candles, 14); val dist = a * tpSlM
        val slPct = if (price > 0) dist / price * 100 else 2.0

        // ── BUY: engulfing + RSI<55 + price dip + EMA uptrend + volume ──
        if (bullEng && r < rsiBuyMax && last.close < deltaRef.close
            && trendBull && macdBull && ctx.lastSignalType != "buy") {
            ctx.lastSignalType = "buy"
            return StrategySignal.BUY(sizePct = size, stopLossPct = slPct, takeProfitPct = slPct * rrr)
        }

        // ── SELL: engulfing + RSI>45 + price rise + EMA downtrend + volume ──
        if (bearEng && r > rsiSellMin && last.close > deltaRef.close
            && trendBear && macdBear && ctx.lastSignalType != "sell") {
            ctx.lastSignalType = "sell"
            return StrategySignal.SELL(sizePct = 100.0, stopLossPct = slPct, takeProfitPct = slPct * rrr)
        }

        return StrategySignal.HOLD
    }

    private fun evaluateGridTrader(candles: List<Candle>, params: Map<String, String>): StrategySignal {
        val levels = params.int("GRID_LEVELS", 10); val rangePct = params.dbl("GRID_RANGE_PCT", 5.0)
        val sizePerGrid = params.dbl("SIZE_PER_GRID", 0.5); val rebalPct = params.dbl("REBALANCE_PCT", 6.0)
        if (candles.size < 50) return StrategySignal.HOLD
        val price = candles.last().close; val sma50 = sma(candles, 50)
        val half = sma50 * (rangePct / 2.0 / 100.0)
        val top = sma50 + half; val bottom = sma50 - half; val step = (top - bottom) / levels
        if (price > top * (1 + rebalPct / 100)) return StrategySignal.SELL(sizePct = 50.0)
        if (price < bottom * (1 - rebalPct / 100)) return StrategySignal.HOLD
        if (step <= 0) return StrategySignal.HOLD
        val gl = ((price - bottom) / step).toInt().coerceIn(0, levels); val mid = levels / 2
        if (gl < mid - 1) return StrategySignal.BUY(sizePct = sizePerGrid * (mid - gl), stopLossPct = rangePct)
        if (gl > mid + 1) return StrategySignal.SELL(sizePct = sizePerGrid * (gl - mid))
        return StrategySignal.HOLD
    }

    // ─── NEW: Supertrend Strategy ───────────────────────────────────

    private fun evaluateSupertrend(candles: List<Candle>, params: Map<String, String>): StrategySignal {
        val period = params.int("ST_PERIOD", 10); val mult = params.dbl("ST_MULTIPLIER", 3.0)
        val size = params.dbl("POSITION_SIZE_PCT", 2.0)
        val sl = params.dbl("STOP_LOSS_PCT", 2.0); val tp = params.dbl("TAKE_PROFIT_PCT", 4.0)
        if (candles.size < period + 3) return StrategySignal.HOLD
        val (_, isUp) = supertrend(candles, period, mult)
        val (_, wasUp) = supertrend(candles.dropLast(1), period, mult)
        if (!wasUp && isUp) return StrategySignal.BUY(sizePct = size, stopLossPct = sl, takeProfitPct = tp)
        if (wasUp && !isUp) return StrategySignal.SELL(sizePct = 100.0, stopLossPct = sl, takeProfitPct = tp)
        return StrategySignal.HOLD
    }

    // ─── NEW: VWAP Reversion Strategy ───────────────────────────────

    private fun evaluateVwapReversion(candles: List<Candle>, params: Map<String, String>): StrategySignal {
        val devTh = params.dbl("DEV_THRESHOLD", 1.5); val rsiP = params.int("RSI_PERIOD", 14)
        val rsiOs = params.dbl("RSI_OVERSOLD", 35.0); val rsiOb = params.dbl("RSI_OVERBOUGHT", 65.0)
        val size = params.dbl("POSITION_SIZE_PCT", 2.5)
        val sl = params.dbl("STOP_LOSS_PCT", 1.5); val tp = params.dbl("TAKE_PROFIT_PCT", 3.0)
        if (candles.size < rsiP + 5) return StrategySignal.HOLD
        val vw = vwap(candles); val price = candles.last().close; val a = atr(candles, 14); val r = rsi(candles, rsiP)
        if (a == 0.0 || vw == 0.0) return StrategySignal.HOLD
        val dev = (price - vw) / a
        if (dev < -devTh && r < rsiOs) return StrategySignal.BUY(sizePct = size, stopLossPct = sl, takeProfitPct = tp)
        if (dev > devTh && r > rsiOb) return StrategySignal.SELL(sizePct = 100.0, stopLossPct = sl, takeProfitPct = tp)
        return StrategySignal.HOLD
    }

    // ─── NEW: Ichimoku Cloud Strategy ───────────────────────────────

    private fun evaluateIchimokuCloud(candles: List<Candle>, params: Map<String, String>): StrategySignal {
        val tP = params.int("TENKAN_PERIOD", 9); val kP = params.int("KIJUN_PERIOD", 26)
        val sbP = params.int("SENKOU_B_PERIOD", 52)
        val size = params.dbl("POSITION_SIZE_PCT", 2.0)
        val sl = params.dbl("STOP_LOSS_PCT", 2.5); val tp = params.dbl("TAKE_PROFIT_PCT", 5.0)
        if (candles.size < sbP + 2) return StrategySignal.HOLD
        val ich = ichimoku(candles, tP, kP, sbP)
        val prevIch = ichimoku(candles.dropLast(1), tP, kP, sbP)
        val price = candles.last().close
        val aboveCloud = price > maxOf(ich.senkouA, ich.senkouB)
        val belowCloud = price < minOf(ich.senkouA, ich.senkouB)
        val tkCross = prevIch.tenkan <= prevIch.kijun && ich.tenkan > ich.kijun
        val tkDeath = prevIch.tenkan >= prevIch.kijun && ich.tenkan < ich.kijun
        if (tkCross && aboveCloud) return StrategySignal.BUY(sizePct = size, stopLossPct = sl, takeProfitPct = tp)
        if (tkDeath && belowCloud) return StrategySignal.SELL(sizePct = 100.0, stopLossPct = sl, takeProfitPct = tp)
        return StrategySignal.HOLD
    }

    // ─── NEW: Stochastic Cross Strategy ─────────────────────────────

    private fun evaluateStochasticCross(candles: List<Candle>, params: Map<String, String>): StrategySignal {
        val kP = params.int("K_PERIOD", 14); val dP = params.int("D_PERIOD", 3)
        val os = params.dbl("OVERSOLD", 20.0); val ob = params.dbl("OVERBOUGHT", 80.0)
        val size = params.dbl("POSITION_SIZE_PCT", 2.0)
        val sl = params.dbl("STOP_LOSS_PCT", 1.5); val tp = params.dbl("TAKE_PROFIT_PCT", 3.0)
        if (candles.size < kP + dP + 2) return StrategySignal.HOLD
        val (k, d) = stochastic(candles, kP, dP)
        val (pk, pd) = stochastic(candles.dropLast(1), kP, dP)
        if (pk <= pd && k > d && k < os) return StrategySignal.BUY(sizePct = size, stopLossPct = sl, takeProfitPct = tp)
        if (pk >= pd && k < d && k > ob) return StrategySignal.SELL(sizePct = 100.0, stopLossPct = sl, takeProfitPct = tp)
        return StrategySignal.HOLD
    }

    // ─── NEW: Volume Breakout Strategy ──────────────────────────────

    private fun evaluateVolumeBreakout(candles: List<Candle>, params: Map<String, String>): StrategySignal {
        val lookback = params.int("LOOKBACK", 20); val volMul = params.dbl("VOL_MULTIPLIER", 2.0)
        val size = params.dbl("POSITION_SIZE_PCT", 2.0)
        val sl = params.dbl("STOP_LOSS_PCT", 2.0); val tp = params.dbl("TAKE_PROFIT_PCT", 4.0)
        if (candles.size < lookback + 2) return StrategySignal.HOLD
        val price = candles.last().close
        val window = candles.subList(candles.size - lookback - 1, candles.size - 1)
        val prevHigh = window.maxOf { it.high }; val prevLow = window.minOf { it.low }
        val avgVol = window.map { it.volume }.average(); val curVol = candles.last().volume
        val spike = curVol > avgVol * volMul
        if (price > prevHigh && spike) return StrategySignal.BUY(sizePct = size, stopLossPct = sl, takeProfitPct = tp)
        if (price < prevLow && spike) return StrategySignal.SELL(sizePct = 100.0, stopLossPct = sl, takeProfitPct = tp)
        return StrategySignal.HOLD
    }

    // ─── NEW: 5-Min Scalper ─────────────────────────────────────────

    private fun evaluateScalper5m(candles: List<Candle>, params: Map<String, String>): StrategySignal {
        val emaFast = params.int("EMA_FAST", 9); val emaSlow = params.int("EMA_SLOW", 21)
        val rsiP = params.int("RSI_PERIOD", 7)
        val rsiBuyMax = params.dbl("RSI_BUY_MAX", 65.0); val rsiSellMin = params.dbl("RSI_SELL_MIN", 35.0)
        val volMult = params.dbl("VOL_MULT", 1.3)
        val size = params.dbl("POSITION_SIZE_PCT", 1.5)
        val tp1Pct = params.dbl("TP1_PCT", 0.3); val tp1Qty = params.dbl("TP1_QTY", 50.0)
        val tp2Pct = params.dbl("TP2_PCT", 0.6); val tp2Qty = params.dbl("TP2_QTY", 50.0)
        val slPct = params.dbl("SL_PCT", 0.4)

        if (candles.size < emaSlow + 15) return StrategySignal.HOLD

        val ef = ema(candles, emaFast); val es = ema(candles, emaSlow)
        val pef = ema(candles.dropLast(1), emaFast); val pes = ema(candles.dropLast(1), emaSlow)
        val r = rsi(candles, rsiP)
        val vw = vwap(candles); val price = candles.last().close
        val avgVol = candles.takeLast(20).map { it.volume }.average()
        val volOk = candles.last().volume > avgVol * volMult

        val tpLevels = listOf(TpSlLevel(tp1Pct, tp1Qty), TpSlLevel(tp2Pct, tp2Qty))

        if (pef <= pes && ef > es && price > vw && r < rsiBuyMax && volOk) {
            return StrategySignal.BUY(sizePct = size, stopLossPct = slPct, takeProfitLevels = tpLevels)
        }
        if (pef >= pes && ef < es && price < vw && r > rsiSellMin && volOk) {
            return StrategySignal.SELL(sizePct = 100.0, stopLossPct = slPct, takeProfitLevels = tpLevels)
        }
        return StrategySignal.HOLD
    }

    // ─── NEW: 15-Min Swing ──────────────────────────────────────────

    private fun evaluateSwing15m(candles: List<Candle>, params: Map<String, String>): StrategySignal {
        val macdFast = params.int("MACD_FAST", 8); val macdSlow = params.int("MACD_SLOW", 21)
        val stochP = params.int("STOCH_RSI_PERIOD", 14)
        val stochK = params.int("STOCH_K", 3); val stochD = params.int("STOCH_D", 3)
        val stochOs = params.dbl("STOCH_OS", 25.0); val stochOb = params.dbl("STOCH_OB", 75.0)
        val stP = params.int("ST_PERIOD", 7); val stM = params.dbl("ST_MULT", 2.0)
        val size = params.dbl("POSITION_SIZE_PCT", 2.0)
        val tp1Pct = params.dbl("TP1_PCT", 0.8); val tp1Qty = params.dbl("TP1_QTY", 33.0)
        val tp2Pct = params.dbl("TP2_PCT", 1.5); val tp2Qty = params.dbl("TP2_QTY", 33.0)
        val tp3Pct = params.dbl("TP3_PCT", 2.5); val tp3Qty = params.dbl("TP3_QTY", 34.0)
        val slPct = params.dbl("SL_PCT", 1.0)

        if (candles.size < macdSlow + 10) return StrategySignal.HOLD

        val macdNow = ema(candles, macdFast) - ema(candles, macdSlow)
        val macdPrev = ema(candles.dropLast(1), macdFast) - ema(candles.dropLast(1), macdSlow)
        val (sk, _) = stochasticRsi(candles, stochP, stochP, stochK, stochD)
        val (_, isUp) = supertrend(candles, stP, stM)

        val tpLevels = listOf(TpSlLevel(tp1Pct, tp1Qty), TpSlLevel(tp2Pct, tp2Qty), TpSlLevel(tp3Pct, tp3Qty))

        if (macdPrev <= 0 && macdNow > 0 && sk < stochOb && isUp) {
            return StrategySignal.BUY(sizePct = size, stopLossPct = slPct, takeProfitLevels = tpLevels)
        }
        if (macdPrev >= 0 && macdNow < 0 && sk > stochOs && !isUp) {
            return StrategySignal.SELL(sizePct = 100.0, stopLossPct = slPct, takeProfitLevels = tpLevels)
        }
        return StrategySignal.HOLD
    }

    // ─── NEW: 1-Hr Trend ────────────────────────────────────────────

    private fun evaluateTrend1h(candles: List<Candle>, params: Map<String, String>): StrategySignal {
        val ichT = params.int("ICH_TENKAN", 9); val ichK = params.int("ICH_KIJUN", 26)
        val ichSb = params.int("ICH_SENKOU_B", 52)
        val adxP = params.int("ADX_PERIOD", 14); val adxMin = params.dbl("ADX_MIN", 25.0)
        val emaMid = params.int("EMA_MID", 50); val emaLong = params.int("EMA_LONG", 200)
        val size = params.dbl("POSITION_SIZE_PCT", 2.5)
        val tp1Pct = params.dbl("TP1_PCT", 2.0); val tp1Qty = params.dbl("TP1_QTY", 30.0)
        val tp2Pct = params.dbl("TP2_PCT", 4.0); val tp2Qty = params.dbl("TP2_QTY", 30.0)
        val tp3Pct = params.dbl("TP3_PCT", 6.0); val tp3Qty = params.dbl("TP3_QTY", 40.0)
        val slPct = params.dbl("SL_PCT", 2.0)

        if (candles.size < emaLong + 5) return StrategySignal.HOLD

        val price = candles.last().close
        val ich = ichimoku(candles, ichT, ichK, ichSb)
        val aboveCloud = price > maxOf(ich.senkouA, ich.senkouB)
        val belowCloud = price < minOf(ich.senkouA, ich.senkouB)
        val adxVal = adx(candles, adxP)

        val em = ema(candles, emaMid); val el = ema(candles, emaLong)
        val pem = ema(candles.dropLast(1), emaMid); val pel = ema(candles.dropLast(1), emaLong)
        val goldenCross = pem <= pel && em > el
        val deathCross = pem >= pel && em < el

        val tpLevels = listOf(TpSlLevel(tp1Pct, tp1Qty), TpSlLevel(tp2Pct, tp2Qty), TpSlLevel(tp3Pct, tp3Qty))

        if (goldenCross && aboveCloud && adxVal > adxMin) {
            return StrategySignal.BUY(sizePct = size, stopLossPct = slPct, takeProfitLevels = tpLevels)
        }
        if (deathCross && belowCloud && adxVal > adxMin) {
            return StrategySignal.SELL(sizePct = 100.0, stopLossPct = slPct, takeProfitLevels = tpLevels)
        }
        if (adxVal < 20.0) {
            if (em > el) return StrategySignal.CLOSE_IF_LONG
            if (em < el) return StrategySignal.CLOSE_IF_SHORT
        }
        return StrategySignal.HOLD
    }

    // ═══════════════════════════════════════════════════════════════════
    // ██  BATCH SIGNAL PRECOMPUTATION — O(n) instead of O(n²)        ██
    // ═══════════════════════════════════════════════════════════════════

    /** O(n) RSI series using Wilder smoothing */
    private fun rsiSeriesFast(candles: List<Candle>, period: Int): List<Double> {
        if (candles.size < period + 1) return List(candles.size) { 50.0 }
        val result = MutableList(candles.size) { 50.0 }
        val closes = candles.map { it.close }
        var avgGain = 0.0; var avgLoss = 0.0
        for (i in 1..period) {
            val change = closes[i] - closes[i - 1]
            if (change > 0) avgGain += change else avgLoss -= change
        }
        avgGain /= period; avgLoss /= period
        result[period] = if (avgLoss == 0.0) 100.0 else 100.0 - (100.0 / (1.0 + avgGain / avgLoss))
        for (i in period + 1 until candles.size) {
            val change = closes[i] - closes[i - 1]
            avgGain = (avgGain * (period - 1) + if (change > 0) change else 0.0) / period
            avgLoss = (avgLoss * (period - 1) + if (change < 0) -change else 0.0) / period
            result[i] = if (avgLoss == 0.0) 100.0 else 100.0 - (100.0 / (1.0 + avgGain / avgLoss))
        }
        return result
    }

    /** O(n) Stochastic RSI K series */
    private fun stochRsiKSeries(candles: List<Candle>, rsiPeriod: Int, stochPeriod: Int, kSmooth: Int): List<Double> {
        val rsiVals = rsiSeriesFast(candles, rsiPeriod)
        val result = MutableList(candles.size) { 50.0 }
        if (rsiVals.size < stochPeriod + rsiPeriod) return result
        val stochRaw = MutableList(rsiVals.size) { 50.0 }
        for (i in rsiPeriod + stochPeriod - 1 until rsiVals.size) {
            val slice = rsiVals.subList(i - stochPeriod + 1, i + 1)
            val high = slice.max(); val low = slice.min()
            stochRaw[i] = if (high - low > 0) (rsiVals[i] - low) / (high - low) * 100 else 50.0
        }
        // Smooth K
        for (i in rsiPeriod + stochPeriod + kSmooth - 2 until rsiVals.size) {
            result[i] = stochRaw.subList(i - kSmooth + 1, i + 1).average()
        }
        return result
    }

    /** O(n) Supertrend direction series */
    private fun supertrendDirSeries(candles: List<Candle>, period: Int, multiplier: Double): List<Boolean> {
        val result = MutableList(candles.size) { true }
        if (candles.size < period + 2) return result
        val trList = (1 until candles.size).map { i ->
            val h = candles[i].high; val l = candles[i].low; val pc = candles[i - 1].close
            maxOf(h - l, Math.abs(h - pc), Math.abs(l - pc))
        }
        val atrSmooth = mutableListOf(trList.take(period).average())
        for (i in period until trList.size) {
            atrSmooth.add((atrSmooth.last() * (period - 1) + trList[i]) / period)
        }
        var st = candles[period].close; var isUp = true
        for (i in atrSmooth.indices) {
            val ci = i + 1
            if (ci >= candles.size) break
            val hl2 = (candles[ci].high + candles[ci].low) / 2.0
            val ub = hl2 + multiplier * atrSmooth[i]
            val lb = hl2 - multiplier * atrSmooth[i]
            if (candles[ci].close > st) { st = lb; isUp = true } else { st = ub; isUp = false }
            result[ci] = isUp
        }
        return result
    }

    /**
     * Precompute signals for all bars at once using O(n) indicator series.
     * Returns null for templates without batch support (falls back to per-bar).
     */
    private fun batchEvaluateSignals(
        templateId: String, candles: List<Candle>, params: Map<String, String>
    ): List<StrategySignal>? {
        return when (templateId) {
            StrategyTemplateId.SWING_15M.name -> batchSwing15m(candles, params)
            StrategyTemplateId.SCALPER_5M.name -> batchScalper5m(candles, params)
            StrategyTemplateId.TREND_1H.name -> batchTrend1h(candles, params)
            else -> null
        }
    }

    private fun batchSwing15m(candles: List<Candle>, params: Map<String, String>): List<StrategySignal> {
        val macdFast = params.int("MACD_FAST", 8); val macdSlow = params.int("MACD_SLOW", 21)
        val stochP = params.int("STOCH_RSI_PERIOD", 14)
        val stochK = params.int("STOCH_K", 3)
        val stochOs = params.dbl("STOCH_OS", 25.0); val stochOb = params.dbl("STOCH_OB", 75.0)
        val stP = params.int("ST_PERIOD", 7); val stM = params.dbl("ST_MULT", 2.0)
        val size = params.dbl("POSITION_SIZE_PCT", 2.0)
        val tp1Pct = params.dbl("TP1_PCT", 0.8); val tp1Qty = params.dbl("TP1_QTY", 33.0)
        val tp2Pct = params.dbl("TP2_PCT", 1.5); val tp2Qty = params.dbl("TP2_QTY", 33.0)
        val tp3Pct = params.dbl("TP3_PCT", 2.5); val tp3Qty = params.dbl("TP3_QTY", 34.0)
        val slPct = params.dbl("SL_PCT", 1.0)

        val emaFastSer = emaSeries(candles, macdFast)
        val emaSlowSer = emaSeries(candles, macdSlow)
        val stochRsiK = stochRsiKSeries(candles, stochP, stochP, stochK)
        val stDir = supertrendDirSeries(candles, stP, stM)

        val tpLevels = listOf(TpSlLevel(tp1Pct, tp1Qty), TpSlLevel(tp2Pct, tp2Qty), TpSlLevel(tp3Pct, tp3Qty))

        return candles.indices.map { i ->
            val ef = emaFastSer[i]; val es = emaSlowSer[i]
            val pef = if (i > 0) emaFastSer[i - 1] else null
            val pes = if (i > 0) emaSlowSer[i - 1] else null
            if (ef == null || es == null || pef == null || pes == null || i < macdSlow + 10) {
                StrategySignal.HOLD
            } else {
                val macdNow = ef - es; val macdPrev = pef - pes
                val sk = stochRsiK[i]; val isUp = stDir[i]
                if (macdPrev <= 0 && macdNow > 0 && sk < stochOb && isUp)
                    StrategySignal.BUY(sizePct = size, stopLossPct = slPct, takeProfitLevels = tpLevels)
                else if (macdPrev >= 0 && macdNow < 0 && sk > stochOs && !isUp)
                    StrategySignal.SELL(sizePct = 100.0, stopLossPct = slPct, takeProfitLevels = tpLevels)
                else StrategySignal.HOLD
            }
        }
    }

    private fun batchScalper5m(candles: List<Candle>, params: Map<String, String>): List<StrategySignal> {
        val emaFast = params.int("EMA_FAST", 9); val emaSlow = params.int("EMA_SLOW", 21)
        val rsiP = params.int("RSI_PERIOD", 7)
        val rsiBuyMax = params.dbl("RSI_BUY_MAX", 65.0); val rsiSellMin = params.dbl("RSI_SELL_MIN", 35.0)
        val volMult = params.dbl("VOL_MULT", 1.3)
        val size = params.dbl("POSITION_SIZE_PCT", 1.5)
        val tp1Pct = params.dbl("TP1_PCT", 0.3); val tp1Qty = params.dbl("TP1_QTY", 50.0)
        val tp2Pct = params.dbl("TP2_PCT", 0.6); val tp2Qty = params.dbl("TP2_QTY", 50.0)
        val slPct = params.dbl("SL_PCT", 0.4)

        val efSer = emaSeries(candles, emaFast); val esSer = emaSeries(candles, emaSlow)
        val rsiArr = rsiSeriesFast(candles, rsiP)
        val tpLevels = listOf(TpSlLevel(tp1Pct, tp1Qty), TpSlLevel(tp2Pct, tp2Qty))

        // Precompute rolling VWAP + avg volume
        val vwapArr = DoubleArray(candles.size)
        val volAvgArr = DoubleArray(candles.size)
        var cumPV = 0.0; var cumVol = 0.0
        for (i in candles.indices) {
            val c = candles[i]
            cumPV += c.close * c.volume; cumVol += c.volume
            vwapArr[i] = if (cumVol > 0) cumPV / cumVol else c.close
            volAvgArr[i] = if (i >= 19) candles.subList(i - 19, i + 1).map { it.volume }.average() else candles.subList(0, i + 1).map { it.volume }.average()
        }

        return candles.indices.map { i ->
            val ef = efSer[i]; val es = esSer[i]
            val pef = if (i > 0) efSer[i - 1] else null
            val pes = if (i > 0) esSer[i - 1] else null
            if (ef == null || es == null || pef == null || pes == null || i < emaSlow + 15) {
                StrategySignal.HOLD
            } else {
                val r = rsiArr[i]; val price = candles[i].close
                val vw = vwapArr[i]; val volOk = candles[i].volume > volAvgArr[i] * volMult
                if (pef <= pes && ef > es && price > vw && r < rsiBuyMax && volOk)
                    StrategySignal.BUY(sizePct = size, stopLossPct = slPct, takeProfitLevels = tpLevels)
                else if (pef >= pes && ef < es && price < vw && r > rsiSellMin && volOk)
                    StrategySignal.SELL(sizePct = 100.0, stopLossPct = slPct, takeProfitLevels = tpLevels)
                else StrategySignal.HOLD
            }
        }
    }

    private fun batchTrend1h(candles: List<Candle>, params: Map<String, String>): List<StrategySignal> {
        val adxP = params.int("ADX_PERIOD", 14); val adxMin = params.dbl("ADX_MIN", 25.0)
        val emaMid = params.int("EMA_MID", 50); val emaLong = params.int("EMA_LONG", 200)
        val ichT = params.int("ICH_TENKAN", 9); val ichK = params.int("ICH_KIJUN", 26)
        val ichSb = params.int("ICH_SENKOU_B", 52)
        val size = params.dbl("POSITION_SIZE_PCT", 2.5)
        val tp1Pct = params.dbl("TP1_PCT", 2.0); val tp1Qty = params.dbl("TP1_QTY", 30.0)
        val tp2Pct = params.dbl("TP2_PCT", 4.0); val tp2Qty = params.dbl("TP2_QTY", 30.0)
        val tp3Pct = params.dbl("TP3_PCT", 6.0); val tp3Qty = params.dbl("TP3_QTY", 40.0)
        val slPct = params.dbl("SL_PCT", 2.0)

        val emMidSer = emaSeries(candles, emaMid); val emLongSer = emaSeries(candles, emaLong)
        val tpLevels = listOf(TpSlLevel(tp1Pct, tp1Qty), TpSlLevel(tp2Pct, tp2Qty), TpSlLevel(tp3Pct, tp3Qty))

        return candles.indices.map { i ->
            val em = emMidSer[i]; val el = emLongSer[i]
            val pem = if (i > 0) emMidSer[i - 1] else null
            val pel = if (i > 0) emLongSer[i - 1] else null
            if (em == null || el == null || pem == null || pel == null || i < emaLong + 5) {
                StrategySignal.HOLD
            } else {
                val price = candles[i].close
                val ich = ichimoku(candles.subList(0, i + 1), ichT, ichK, ichSb)
                val aboveCloud = price > maxOf(ich.senkouA, ich.senkouB)
                val belowCloud = price < minOf(ich.senkouA, ich.senkouB)
                val adxVal = adx(candles.subList(0, i + 1), adxP)
                val goldenCross = pem <= pel && em > el; val deathCross = pem >= pel && em < el
                if (goldenCross && aboveCloud && adxVal > adxMin)
                    StrategySignal.BUY(sizePct = size, stopLossPct = slPct, takeProfitLevels = tpLevels)
                else if (deathCross && belowCloud && adxVal > adxMin)
                    StrategySignal.SELL(sizePct = 100.0, stopLossPct = slPct, takeProfitLevels = tpLevels)
                else if (adxVal < 20.0) {
                    if (em > el) StrategySignal.CLOSE_IF_LONG
                    else if (em < el) StrategySignal.CLOSE_IF_SHORT
                    else StrategySignal.HOLD
                } else StrategySignal.HOLD
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // ██  BACKTESTING ENGINE  ──  TP/SL sim · Short selling · Sharpe  ██
    // ═══════════════════════════════════════════════════════════════════

    private data class BacktestPosition(
        val side: OrderSide, val entryPrice: Double, val qty: Double,
        val entryTime: Long, val entryBar: Int,
        val stopLossPrice: Double? = null, val takeProfitPrice: Double? = null,
        val tpLevels: List<Pair<Double, Double>> = emptyList(),  // (price, qtyFraction 0..1)
        val slLevels: List<Pair<Double, Double>> = emptyList()   // (price, qtyFraction 0..1)
    )

    fun backtest(
        templateId: String, candles: List<Candle>, symbol: String, interval: String,
        params: Map<String, String> = emptyMap(), startingCapital: Double = 10000.0,
        backtestDays: Int = 0, makerFeePct: Double = 0.001, takerFeePct: Double = 0.001,
        slippagePct: Double = 0.0, onProgress: (Float) -> Unit = {}
    ): Pair<BacktestResult, List<SignalMarker>> {
        if (candles.size < 50) return Pair(
            BacktestResult("", symbol, interval, 0, 0, 0, 0.0, 0.0, 0.0, 0.0), emptyList()
        )

        val trades = mutableListOf<BacktestTrade>()
        val signals = mutableListOf<SignalMarker>()
        var position: BacktestPosition? = null
        val feePct = (makerFeePct + takerFeePct) / 2.0  // blended entry/exit fee
        val slipFraction = slippagePct / 100.0
        var equity = startingCapital
        val equityCurve = mutableListOf(equity)
        val ctx = EvalContext()

        // Precompute signals for supported templates (O(n) vs O(n²))
        val precomputedSignals = batchEvaluateSignals(templateId, candles, params)

        val totalBars = candles.size - 50
        var lastReportedPct = -1
        for (i in 50 until candles.size) {
            val pct = ((i - 50) * 100) / totalBars
            if (pct != lastReportedPct) { lastReportedPct = pct; onProgress(pct / 100f) }
            val bar = candles[i]

            // ── Check TP/SL before new signals ──
            if (position != null) {
                val pos = position!!
                val isLong = pos.side == OrderSide.BUY

                // ── Multi-level TP/SL ──
                if (pos.tpLevels.isNotEmpty() || pos.slLevels.isNotEmpty()) {
                    var remainQty = pos.qty
                    val hitTp = mutableListOf<Int>()
                    val hitSl = mutableListOf<Int>()

                    // Check SL levels first (pessimistic)
                    for ((idx, sl) in pos.slLevels.withIndex()) {
                        val (slPrice, slFrac) = sl
                        val triggered = if (isLong) bar.low <= slPrice else bar.high >= slPrice
                        if (triggered) {
                            val closeQty = pos.qty * slFrac
                            val pnl = if (isLong) (slPrice - pos.entryPrice) * closeQty
                                      else (pos.entryPrice - slPrice) * closeQty
                            val fee = (pos.entryPrice * closeQty + slPrice * closeQty) * feePct
                            equity += pnl - fee
                            remainQty -= closeQty
                            trades.add(BacktestTrade(pos.entryTime, bar.openTime, pos.side, pos.entryPrice,
                                slPrice, closeQty, pnl - fee, fee, i - pos.entryBar))
                            signals.add(SignalMarker("__SL", "", symbol, interval,
                                bar.openTime, if (isLong) SignalType.SELL else SignalType.BUY, slPrice))
                            hitSl.add(idx)
                        }
                    }
                    // Check TP levels
                    for ((idx, tp) in pos.tpLevels.withIndex()) {
                        val (tpPrice, tpFrac) = tp
                        val triggered = if (isLong) bar.high >= tpPrice else bar.low <= tpPrice
                        if (triggered && remainQty > 0) {
                            val closeQty = minOf(pos.qty * tpFrac, remainQty)
                            val pnl = if (isLong) (tpPrice - pos.entryPrice) * closeQty
                                      else (pos.entryPrice - tpPrice) * closeQty
                            val fee = (pos.entryPrice * closeQty + tpPrice * closeQty) * feePct
                            equity += pnl - fee
                            remainQty -= closeQty
                            trades.add(BacktestTrade(pos.entryTime, bar.openTime, pos.side, pos.entryPrice,
                                tpPrice, closeQty, pnl - fee, fee, i - pos.entryBar))
                            signals.add(SignalMarker("__TP", "", symbol, interval,
                                bar.openTime, if (isLong) SignalType.SELL else SignalType.BUY, tpPrice))
                            hitTp.add(idx)
                        }
                    }
                    if (remainQty <= pos.qty * 0.01) {
                        // Position fully closed
                        position = null
                    } else if (hitTp.isNotEmpty() || hitSl.isNotEmpty()) {
                        // Partial close — rebuild position with remaining levels
                        val newTp = pos.tpLevels.filterIndexed { idx, _ -> idx !in hitTp }
                        val newSl = pos.slLevels.filterIndexed { idx, _ -> idx !in hitSl }
                        position = pos.copy(qty = remainQty, tpLevels = newTp, slLevels = newSl)
                    }
                } else {
                    // ── Legacy single TP/SL ──
                    val slP = pos.stopLossPrice; val tpP = pos.takeProfitPrice
                    var exitPrice: Double? = null

                    if (slP != null) {
                        if (isLong && bar.low <= slP) exitPrice = slP
                        else if (!isLong && bar.high >= slP) exitPrice = slP
                    }
                    if (tpP != null && exitPrice == null) {
                        if (isLong && bar.high >= tpP) exitPrice = tpP
                        else if (!isLong && bar.low <= tpP) exitPrice = tpP
                    }

                    if (exitPrice != null) {
                        val pnl = if (isLong) (exitPrice - pos.entryPrice) * pos.qty
                                  else (pos.entryPrice - exitPrice) * pos.qty
                        val fee = (pos.entryPrice * pos.qty + exitPrice * pos.qty) * feePct
                        equity += pnl - fee
                        trades.add(BacktestTrade(pos.entryTime, bar.openTime, pos.side, pos.entryPrice,
                            exitPrice, pos.qty, pnl - fee, fee, i - pos.entryBar))
                        signals.add(SignalMarker("__SL_TP", "", symbol, interval,
                            bar.openTime, if (isLong) SignalType.SELL else SignalType.BUY, exitPrice))
                        position = null
                    }
                }
            }

            // ── Evaluate strategy signal ──
            val signal = if (precomputedSignals != null) {
                precomputedSignals[i]
            } else {
                try {
                    evaluate(templateId, candles.subList(0, i + 1), params, ctx)
                } catch (_: Exception) {
                    StrategySignal.HOLD
                }
            }

            when (signal) {
                is StrategySignal.BUY -> {
                    if (position != null && position!!.side == OrderSide.SELL) {
                        // Close short first — slippage worsens exit (buy to cover at higher price)
                        val pos = position!!
                        val exitPrice = bar.close * (1.0 + slipFraction)
                        val pnl = (pos.entryPrice - exitPrice) * pos.qty
                        val fee = (pos.entryPrice * pos.qty + exitPrice * pos.qty) * feePct
                        equity += pnl - fee
                        trades.add(BacktestTrade(pos.entryTime, bar.openTime, pos.side, pos.entryPrice,
                            exitPrice, pos.qty, pnl - fee, fee, i - pos.entryBar))
                        signals.add(SignalMarker("__CS", "", symbol, interval,
                            bar.openTime, SignalType.BUY, bar.close))
                        position = null
                    }
                    if (position == null) {
                        // Slippage worsens entry: buy at higher price
                        val entryPrice = bar.close * (1.0 + slipFraction)
                        val qty = (equity * signal.sizePct / 100.0) / entryPrice
                        val slPrice = signal.stopLossPct?.let { entryPrice * (1.0 - it / 100.0) }
                        val tpPrice = signal.takeProfitPct?.let { entryPrice * (1.0 + it / 100.0) }
                        val tpLevels = signal.takeProfitLevels.map { lvl ->
                            Pair(entryPrice * (1.0 + lvl.pct / 100.0), lvl.quantityPct / 100.0)
                        }
                        val slLevels = signal.stopLossLevels.map { lvl ->
                            Pair(entryPrice * (1.0 - lvl.pct / 100.0), lvl.quantityPct / 100.0)
                        }
                        position = BacktestPosition(OrderSide.BUY, entryPrice, qty, bar.openTime, i,
                            slPrice, tpPrice, tpLevels, slLevels)
                        signals.add(SignalMarker("__B", "", symbol, interval,
                            bar.openTime, SignalType.BUY, bar.close))
                    }
                }
                is StrategySignal.SELL -> {
                    if (position != null && position!!.side == OrderSide.BUY) {
                        // Close long — slippage worsens exit (sell at lower price)
                        val pos = position!!
                        val exitPrice = bar.close * (1.0 - slipFraction)
                        val pnl = (exitPrice - pos.entryPrice) * pos.qty
                        val fee = (pos.entryPrice * pos.qty + exitPrice * pos.qty) * feePct
                        equity += pnl - fee
                        trades.add(BacktestTrade(pos.entryTime, bar.openTime, pos.side, pos.entryPrice,
                            exitPrice, pos.qty, pnl - fee, fee, i - pos.entryBar))
                        signals.add(SignalMarker("__S", "", symbol, interval,
                            bar.openTime, SignalType.SELL, bar.close))
                        position = null
                    }
                    if (position == null) {
                        // Open short — slippage worsens entry (sell at lower price)
                        val entryPrice = bar.close * (1.0 - slipFraction)
                        val qty = (equity * signal.sizePct / 100.0) / entryPrice
                        val slPrice = signal.stopLossPct?.let { entryPrice * (1.0 + it / 100.0) }
                        val tpPrice = signal.takeProfitPct?.let { entryPrice * (1.0 - it / 100.0) }
                        val tpLevels = signal.takeProfitLevels.map { lvl ->
                            Pair(entryPrice * (1.0 - lvl.pct / 100.0), lvl.quantityPct / 100.0)
                        }
                        val slLevels = signal.stopLossLevels.map { lvl ->
                            Pair(entryPrice * (1.0 + lvl.pct / 100.0), lvl.quantityPct / 100.0)
                        }
                        position = BacktestPosition(OrderSide.SELL, entryPrice, qty, bar.openTime, i,
                            slPrice, tpPrice, tpLevels, slLevels)
                        signals.add(SignalMarker("__SH", "", symbol, interval,
                            bar.openTime, SignalType.SELL, bar.close))
                    }
                }
                is StrategySignal.CLOSE_IF_LONG -> {
                    if (position != null && position!!.side == OrderSide.BUY) {
                        val pos = position!!
                        val exitPrice = bar.close * (1.0 - slipFraction)
                        val pnl = (exitPrice - pos.entryPrice) * pos.qty
                        val fee = (pos.entryPrice * pos.qty + exitPrice * pos.qty) * feePct
                        equity += pnl - fee
                        trades.add(BacktestTrade(pos.entryTime, bar.openTime, pos.side, pos.entryPrice,
                            exitPrice, pos.qty, pnl - fee, fee, i - pos.entryBar))
                        signals.add(SignalMarker("__CL", "", symbol, interval,
                            bar.openTime, SignalType.CLOSE, bar.close))
                        position = null
                    }
                }
                is StrategySignal.CLOSE_IF_SHORT -> {
                    if (position != null && position!!.side == OrderSide.SELL) {
                        val pos = position!!
                        val exitPrice = bar.close * (1.0 + slipFraction)
                        val pnl = (pos.entryPrice - exitPrice) * pos.qty
                        val fee = (pos.entryPrice * pos.qty + exitPrice * pos.qty) * feePct
                        equity += pnl - fee
                        trades.add(BacktestTrade(pos.entryTime, bar.openTime, pos.side, pos.entryPrice,
                            exitPrice, pos.qty, pnl - fee, fee, i - pos.entryBar))
                        signals.add(SignalMarker("__CSH", "", symbol, interval,
                            bar.openTime, SignalType.CLOSE, bar.close))
                        position = null
                    }
                }
                is StrategySignal.HOLD -> { }
            }
            equityCurve.add(equity)
        }

        // Close open position at end of data
        if (position != null) {
            val last = candles.last(); val pos = position!!
            val isLong = pos.side == OrderSide.BUY
            val pnl = if (isLong) (last.close - pos.entryPrice) * pos.qty
                      else (pos.entryPrice - last.close) * pos.qty
            val fee = (pos.entryPrice * pos.qty + last.close * pos.qty) * feePct
            equity += pnl - fee
            trades.add(BacktestTrade(pos.entryTime, last.openTime, pos.side, pos.entryPrice,
                last.close, pos.qty, pnl - fee, fee, candles.size - 1 - pos.entryBar))
        }

        // ── Compute metrics ──
        val totalPnl = equity - startingCapital
        val winners = trades.filter { it.pnl > 0 }; val losers = trades.filter { it.pnl <= 0 }
        val winRate = if (trades.isNotEmpty()) winners.size.toDouble() / trades.size * 100 else 0.0
        val grossProfit = winners.sumOf { it.pnl }; val grossLoss = losers.sumOf { it.pnl }
        val profitFactor = if (grossLoss != 0.0) Math.abs(grossProfit / grossLoss)
                           else if (grossProfit > 0) Double.MAX_VALUE else 0.0
        val avgWin = if (winners.isNotEmpty()) winners.map { it.pnl }.average() else 0.0
        val avgLoss = if (losers.isNotEmpty()) losers.map { it.pnl }.average() else 0.0
        val largestWin = winners.maxOfOrNull { it.pnl } ?: 0.0
        val largestLoss = losers.minOfOrNull { it.pnl } ?: 0.0
        val avgBars = if (trades.isNotEmpty()) trades.map { it.barsInTrade.toDouble() }.average() else 0.0

        // Max consecutive wins/losses
        var maxCW = 0; var maxCL = 0; var cW = 0; var cL = 0
        for (t in trades) {
            if (t.pnl > 0) { cW++; cL = 0; maxCW = maxOf(maxCW, cW) }
            else { cL++; cW = 0; maxCL = maxOf(maxCL, cL) }
        }

        val buyAndHold = (candles.last().close - candles[50].close) / candles[50].close * 100

        // Max drawdown from equity curve
        val maxDrawdown = run {
            var peak = equityCurve[0]; var maxDd = 0.0
            for (eq in equityCurve) { peak = maxOf(peak, eq); maxDd = maxOf(maxDd, (peak - eq) / peak * 100) }
            maxDd
        }

        // Sharpe ratio — interval-aware annualization
        val periodsPerYear = when (interval) {
            "1m" -> 525600.0; "3m" -> 175200.0; "5m" -> 105120.0
            "15m" -> 35040.0; "30m" -> 17520.0; "1h" -> 8760.0
            "2h" -> 4380.0; "4h" -> 2190.0; "6h" -> 1460.0
            "8h" -> 1095.0; "12h" -> 730.0; "1d" -> 365.0
            "3d" -> 121.67; "1w" -> 52.0; "1M" -> 12.0; else -> 365.0
        }
        val returns = equityCurve.zipWithNext().map { (a, b) -> if (a != 0.0) (b - a) / a else 0.0 }
        val avgRet = if (returns.isNotEmpty()) returns.average() else 0.0
        val stdRet = if (returns.size > 1) stdDev(returns) else 1.0
        val sharpe = if (stdRet > 0) avgRet / stdRet * Math.sqrt(periodsPerYear) else 0.0
        val downsideReturns = returns.filter { it < 0.0 }
        val downsideDev = if (downsideReturns.size > 1) Math.sqrt(downsideReturns.sumOf { it * it } / downsideReturns.size) else 0.0
        val sortino = if (downsideDev > 0) avgRet / downsideDev * Math.sqrt(periodsPerYear) else 0.0

        val expectancy = if (trades.isNotEmpty())
            (winRate / 100.0 * avgWin) + ((100.0 - winRate) / 100.0 * avgLoss) else 0.0

        // Virtual entry amount: scale from startingCapital to $100
        val entryAmount = 100.0
        val returnPct = if (startingCapital > 0) totalPnl / startingCapital else 0.0
        val finalAmount = entryAmount * (1.0 + returnPct)

        return Pair(BacktestResult(
            scriptId = "", symbol = symbol, timeframe = interval,
            startDate = candles.first().openTime, endDate = candles.last().openTime,
            totalTrades = trades.size, winRate = winRate, totalPnl = totalPnl,
            maxDrawdown = maxDrawdown, sharpeRatio = sharpe, sortinoRatio = sortino, trades = trades,
            profitFactor = profitFactor, buyAndHoldReturn = buyAndHold,
            avgBarsInTrade = avgBars, grossProfit = grossProfit, grossLoss = grossLoss,
            maxConsecutiveWins = maxCW, maxConsecutiveLosses = maxCL,
            equityCurve = equityCurve, startingCapital = startingCapital,
            expectancy = expectancy, avgWin = avgWin, avgLoss = avgLoss,
            largestWin = largestWin, largestLoss = largestLoss,
            backtestDays = backtestDays, entryAmount = entryAmount, finalAmount = finalAmount
        ), signals)
    }
}
