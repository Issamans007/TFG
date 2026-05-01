package com.tfg.domain.model

/**
 * Indicator & series computation engine for chart overlays.
 * Strategy evaluation now happens entirely in the JS (QuickJS) engine.
 */
object StrategyEvaluator {

    // -------------------------------------------------------------------
    //  INDICATOR FUNCTIONS (public for chart overlays)
    // -------------------------------------------------------------------

    // --- Moving Averages --------------------------------------------

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

    // --- Oscillators ------------------------------------------------

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

    // --- Volume Indicators ------------------------------------------

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

    // --- Trend Indicators -------------------------------------------

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

    // --- Channel Indicators -----------------------------------------

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

    // --- Momentum Indicators ----------------------------------------

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

    // --- Statistical ------------------------------------------------

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

    // --- Series Functions -------------------------------------------

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

    // --- New Series Functions for Chart -----------------------------

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

    /** Supertrend series: returns Pair(greenLine, redLine) � each list has Double? per candle. */
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

}