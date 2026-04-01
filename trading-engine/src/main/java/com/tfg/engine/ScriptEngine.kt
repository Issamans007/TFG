package com.tfg.engine

import com.tfg.domain.model.*
import app.cash.quickjs.QuickJs
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import timber.log.Timber
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * JavaScript-based script execution engine using QuickJS.
 *
 * Users write pure JavaScript. The engine injects indicator helper
 * functions, candle data, and params, then calls the user's
 * strategy() function in a sandboxed QuickJS context.
 *
 * A single QuickJS instance is cached and reused across evaluations.
 * A watchdog thread enforces execution timeouts to guard against
 * infinite loops in user code.
 */
@Singleton
class ScriptEngine @Inject constructor() : ScriptExecutor {

    private val gson = Gson()

    /** Cached QuickJS instance — reused across evaluations. */
    @Volatile private var cachedJs: QuickJs? = null

    /** The user code currently loaded in [cachedJs], to avoid re-evaluating unchanged code. */
    private var loadedCode: String? = null
    /** The params last used to build the context — rebuild if params change. */
    private var loadedParams: Map<String, String>? = null

    private val watchdog = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "quickjs-watchdog").also { it.isDaemon = true }
    }

    override fun evaluate(code: String, candles: List<Candle>, params: Map<String, String>): StrategySignal =
        execute(code, candles, params)

    /**
     * Execute user code against the provided candles and params.
     * Returns the StrategySignal produced by the user's strategy() function.
     * Falls back to HOLD on any execution error (including timeout).
     */
    @Synchronized
    fun execute(
        code: String,
        candles: List<Candle>,
        params: Map<String, String>
    ): StrategySignal {
        val js = getOrCreateJs(code, params)
        // Schedule a timeout — close() from another thread interrupts evaluate()
        val timeoutFuture = watchdog.schedule({ closeJs() }, TIMEOUT_MS, TimeUnit.MILLISECONDS)
        return try {
            // 1. Inject candles as JS array
            val candlesJs = buildCandlesJson(candles)
            js.evaluate("var _candles = $candlesJs;", "data")

            // 2. Inject params
            val paramsJs = buildParamsJson(params)
            js.evaluate("var _params = $paramsJs;", "params")

            // 3. Call strategy() and parse result
            val resultStr = js.evaluate(
                "JSON.stringify(strategy(_candles));",
                "call"
            )
            parseSignal(resultStr?.toString() ?: "")
        } catch (e: Exception) {
            Timber.w(e, "ScriptEngine execution failed, falling back to HOLD")
            // If the instance was closed by timeout or error, discard it
            closeJs()
            StrategySignal.HOLD
        } finally {
            timeoutFuture.cancel(false)
        }
    }

    // ─── Cached QuickJS lifecycle ───────────────────────────────────

    @Synchronized
    private fun getOrCreateJs(code: String, params: Map<String, String>): QuickJs {
        val existing = cachedJs
        if (existing != null && loadedCode == code && loadedParams == params) return existing

        // Code changed or first call — rebuild context
        existing?.close()
        val js = QuickJs.create()
        // Load indicator library once
        js.evaluate(JS_INDICATOR_LIBRARY, "indicators")
        // Inject param values as constants
        val paramDecls = params.entries.joinToString("\n") { (k, v) ->
            val numVal = v.toDoubleOrNull()
            if (numVal != null) "var $k = $numVal;" else "var $k = \"${v.replace("\"", "\\\"")}\";"
        }
        if (paramDecls.isNotBlank()) {
            js.evaluate(paramDecls, "paramDecls")
        }
        // Evaluate user code (defines strategy function)
        js.evaluate(code, "strategy")
        cachedJs = js
        loadedCode = code
        loadedParams = params
        return js
    }

    @Synchronized
    private fun closeJs() {
        cachedJs?.close()
        cachedJs = null
        loadedCode = null
        loadedParams = null
    }

    // ─── JSON builders ──────────────────────────────────────────────

    private fun buildCandlesJson(candles: List<Candle>): String {
        val sb = StringBuilder("[")
        candles.forEachIndexed { i, c ->
            if (i > 0) sb.append(",")
            sb.append("{\"openTime\":${c.openTime},\"open\":${c.open},\"high\":${c.high},")
            sb.append("\"low\":${c.low},\"close\":${c.close},\"volume\":${c.volume}}")
        }
        sb.append("]")
        return sb.toString()
    }

    private fun buildParamsJson(params: Map<String, String>): String {
        val entries = params.entries.joinToString(",") { (k, v) ->
            "\"$k\":\"${v.replace("\"", "\\\"")}\""
        }
        return "{$entries}"
    }

    // ─── Signal parser (Gson) ───────────────────────────────────────

    /** Flat DTO mirroring the JS object returned by strategy(). */
    private data class SignalDto(
        val type: String? = null,
        val sizePct: Double? = null,
        val stopLossPct: Double? = null,
        val takeProfitPct: Double? = null,
        val takeProfitLevels: List<LevelDto>? = null,
        val stopLossLevels: List<LevelDto>? = null
    )
    private data class LevelDto(val pct: Double? = null, val quantityPct: Double? = null)

    private fun parseSignal(json: String): StrategySignal {
        val clean = json.trim().removeSurrounding("\"").replace("\\\"", "\"")
        if (clean.isEmpty() || clean == "null" || clean == "undefined") return StrategySignal.HOLD

        val dto = try {
            gson.fromJson(clean, SignalDto::class.java) ?: return StrategySignal.HOLD
        } catch (_: Exception) {
            return StrategySignal.HOLD
        }
        val type = dto.type?.uppercase() ?: return StrategySignal.HOLD
        val sizePct = dto.sizePct ?: 2.0
        val tpLevels = dto.takeProfitLevels?.mapNotNull { lv ->
            val p = lv.pct ?: return@mapNotNull null; TpSlLevel(p, lv.quantityPct ?: 100.0)
        } ?: emptyList()
        val slLevels = dto.stopLossLevels?.mapNotNull { lv ->
            val p = lv.pct ?: return@mapNotNull null; TpSlLevel(p, lv.quantityPct ?: 100.0)
        } ?: emptyList()

        return when (type) {
            "BUY" -> StrategySignal.BUY(sizePct = sizePct, stopLossPct = dto.stopLossPct, takeProfitPct = dto.takeProfitPct,
                takeProfitLevels = tpLevels, stopLossLevels = slLevels)
            "SELL" -> StrategySignal.SELL(sizePct = sizePct, stopLossPct = dto.stopLossPct, takeProfitPct = dto.takeProfitPct,
                takeProfitLevels = tpLevels, stopLossLevels = slLevels)
            "CLOSE_IF_LONG" -> StrategySignal.CLOSE_IF_LONG
            "CLOSE_IF_SHORT" -> StrategySignal.CLOSE_IF_SHORT
            else -> StrategySignal.HOLD
        }
    }

    // ─── JavaScript indicator library (injected into QuickJS context) ─

    companion object {
        /** Maximum wall-clock time a single strategy() call may run. */
        private const val TIMEOUT_MS = 5_000L

        val JS_INDICATOR_LIBRARY = """
// ─── Indicator functions available to user scripts ─────────────

function sma(candles, period) {
    if (candles.length < period) return candles[candles.length - 1].close;
    var slice = candles.slice(-period);
    return slice.reduce(function(s, c) { return s + c.close; }, 0) / period;
}

function ema(candles, period) {
    if (candles.length < period) return candles[candles.length - 1].close;
    var mult = 2.0 / (period + 1);
    var val_ = candles.slice(0, period).reduce(function(s, c) { return s + c.close; }, 0) / period;
    for (var i = period; i < candles.length; i++) {
        val_ = (candles[i].close - val_) * mult + val_;
    }
    return val_;
}

function rsi(candles, period) {
    if (candles.length < period + 1) return 50;
    var gains = 0, losses = 0;
    for (var i = 1; i <= period; i++) {
        var change = candles[i].close - candles[i - 1].close;
        if (change > 0) gains += change; else losses -= change;
    }
    var avgGain = gains / period, avgLoss = losses / period;
    for (var i = period + 1; i < candles.length; i++) {
        var change = candles[i].close - candles[i - 1].close;
        avgGain = (avgGain * (period - 1) + (change > 0 ? change : 0)) / period;
        avgLoss = (avgLoss * (period - 1) + (change < 0 ? -change : 0)) / period;
    }
    if (avgLoss === 0) return 100;
    var rs = avgGain / avgLoss;
    return 100 - (100 / (1 + rs));
}

function atr(candles, period) {
    if (candles.length < period + 1) return 0;
    var atrVal = 0;
    for (var i = 1; i <= period; i++) {
        atrVal += Math.max(candles[i].high - candles[i].low,
                           Math.abs(candles[i].high - candles[i-1].close),
                           Math.abs(candles[i].low - candles[i-1].close));
    }
    atrVal /= period;
    for (var i = period + 1; i < candles.length; i++) {
        var tr = Math.max(candles[i].high - candles[i].low,
                          Math.abs(candles[i].high - candles[i-1].close),
                          Math.abs(candles[i].low - candles[i-1].close));
        atrVal = (atrVal * (period - 1) + tr) / period;
    }
    return atrVal;
}

function bollinger(candles, period, numStd) {
    var mid = sma(candles, period);
    var slice = candles.slice(-period);
    var closes = slice.map(function(c) { return c.close; });
    var mean = closes.reduce(function(a,b) { return a+b; }, 0) / closes.length;
    var variance = closes.reduce(function(s, v) { return s + (v - mean) * (v - mean); }, 0) / closes.length;
    var sd = Math.sqrt(variance);
    return { upper: mid + numStd * sd, middle: mid, lower: mid - numStd * sd };
}

function macd(candles, fast, slow, sig) {
    if (candles.length < slow) return { macd: 0, signal: 0, histogram: 0 };
    var mult_f = 2.0 / (fast + 1);
    var mult_s = 2.0 / (slow + 1);
    var emaF = candles.slice(0, fast).reduce(function(s,c){return s+c.close;},0) / fast;
    var emaS = candles.slice(0, slow).reduce(function(s,c){return s+c.close;},0) / slow;
    // Advance fast EMA through bars fast..slow-1 so it doesn't skip any
    for (var i = fast; i < slow; i++) {
        emaF = (candles[i].close - emaF) * mult_f + emaF;
    }
    var macdHist = [];
    for (var i = slow; i < candles.length; i++) {
        emaF = (candles[i].close - emaF) * mult_f + emaF;
        emaS = (candles[i].close - emaS) * mult_s + emaS;
        macdHist.push(emaF - emaS);
    }
    var macdLine = macdHist[macdHist.length - 1];
    // Signal line = EMA of MACD history
    var mult_sig = 2.0 / (sig + 1);
    var sigLen = Math.min(sig, macdHist.length);
    var sigVal = macdHist.slice(0, sigLen).reduce(function(s,v){return s+v;},0) / sigLen;
    for (var j = sigLen; j < macdHist.length; j++) {
        sigVal = (macdHist[j] - sigVal) * mult_sig + sigVal;
    }
    return { macd: macdLine, signal: sigVal, histogram: macdLine - sigVal };
}

function obv(candles) {
    var val_ = 0;
    for (var i = 1; i < candles.length; i++) {
        if (candles[i].close > candles[i-1].close) val_ += candles[i].volume;
        else if (candles[i].close < candles[i-1].close) val_ -= candles[i].volume;
    }
    return val_;
}

function vwap(candles) {
    var cumVol = 0, cumTpVol = 0;
    for (var i = 0; i < candles.length; i++) {
        var tp = (candles[i].high + candles[i].low + candles[i].close) / 3;
        cumTpVol += tp * candles[i].volume;
        cumVol += candles[i].volume;
    }
    return cumVol > 0 ? cumTpVol / cumVol : candles[candles.length-1].close;
}

function supertrend(candles, period, multiplier) {
    if (candles.length < period + 2) return [candles[candles.length-1].close, true];
    var atrVals = [];
    for (var i = 1; i < candles.length; i++) {
        var tr = Math.max(candles[i].high - candles[i].low,
                          Math.abs(candles[i].high - candles[i-1].close),
                          Math.abs(candles[i].low - candles[i-1].close));
        atrVals.push(tr);
    }
    if (atrVals.length < period) return [candles[candles.length-1].close, true];
    var smoothAtr = [atrVals.slice(0, period).reduce(function(a,b){return a+b;},0)/period];
    for (var i = period; i < atrVals.length; i++) {
        smoothAtr.push((smoothAtr[smoothAtr.length-1] * (period-1) + atrVals[i]) / period);
    }
    var st = candles[period].close, isUp = true;
    for (var i = 0; i < smoothAtr.length; i++) {
        var ci = i + 1;
        if (ci >= candles.length) break;
        var hl2 = (candles[ci].high + candles[ci].low) / 2;
        var upper = hl2 + multiplier * smoothAtr[i];
        var lower = hl2 - multiplier * smoothAtr[i];
        if (candles[ci].close > st) { st = lower; isUp = true; }
        else { st = upper; isUp = false; }
    }
    return [st, isUp];
}

function stochastic(candles, kPeriod, dPeriod) {
    if (candles.length < kPeriod) return [50, 50];
    var kVals = [];
    for (var i = kPeriod - 1; i < candles.length; i++) {
        var slice = candles.slice(i - kPeriod + 1, i + 1);
        var high = Math.max.apply(null, slice.map(function(c){return c.high;}));
        var low = Math.min.apply(null, slice.map(function(c){return c.low;}));
        var k = (high - low) > 0 ? (candles[i].close - low) / (high - low) * 100 : 50;
        kVals.push(k);
    }
    var k = kVals[kVals.length - 1];
    var d = kVals.length >= dPeriod ? kVals.slice(-dPeriod).reduce(function(a,b){return a+b;},0)/dPeriod : k;
    return [k, d];
}

function ichimoku(candles, tenkan, kijun, senkouB) {
    function donchianMid(data, p) {
        if (data.length < p) return data[data.length-1].close;
        var s = data.slice(-p);
        return (Math.max.apply(null,s.map(function(c){return c.high;})) + Math.min.apply(null,s.map(function(c){return c.low;}))) / 2;
    }
    var t = donchianMid(candles, tenkan), kj = donchianMid(candles, kijun);
    return {tenkan:t, kijun:kj, senkouA:(t+kj)/2, senkouB:donchianMid(candles,senkouB), chikou:candles[candles.length-1].close};
}

function stochasticRsi(candles, rsiPeriod, stochPeriod, kSmooth, dSmooth) {
    if (candles.length < rsiPeriod + stochPeriod) return [50, 50];
    var rsiVals = [];
    for (var i = rsiPeriod; i < candles.length; i++) {
        var gains = 0, losses = 0;
        for (var j = i - rsiPeriod + 1; j <= i; j++) {
            var change = candles[j].close - candles[j-1].close;
            if (change > 0) gains += change; else losses -= change;
        }
        var ag = gains / rsiPeriod, al = losses / rsiPeriod;
        rsiVals.push(al === 0 ? 100 : 100 - (100 / (1 + ag / al)));
    }
    if (rsiVals.length < stochPeriod) return [50, 50];
    var kVals = [];
    for (var i = stochPeriod - 1; i < rsiVals.length; i++) {
        var w = rsiVals.slice(i - stochPeriod + 1, i + 1);
        var hi = Math.max.apply(null, w), lo = Math.min.apply(null, w);
        kVals.push((hi - lo) > 0 ? (rsiVals[i] - lo) / (hi - lo) * 100 : 50);
    }
    var smoothK = kVals;
    if (kSmooth > 1 && kVals.length >= kSmooth) {
        smoothK = [];
        for (var i = kSmooth - 1; i < kVals.length; i++) {
            smoothK.push(kVals.slice(i - kSmooth + 1, i + 1).reduce(function(a,b){return a+b;},0) / kSmooth);
        }
    }
    var k = smoothK[smoothK.length - 1];
    var d = k;
    if (dSmooth > 1 && smoothK.length >= dSmooth) {
        d = smoothK.slice(-dSmooth).reduce(function(a,b){return a+b;},0) / dSmooth;
    }
    return [k, d];
}

function adx(candles, period) {
    if (candles.length < period * 2 + 1) return 25;
    var pDM = [], nDM = [], trVals = [];
    for (var i = 1; i < candles.length; i++) {
        var upMove = candles[i].high - candles[i-1].high;
        var downMove = candles[i-1].low - candles[i].low;
        pDM.push(upMove > downMove && upMove > 0 ? upMove : 0);
        nDM.push(downMove > upMove && downMove > 0 ? downMove : 0);
        trVals.push(Math.max(candles[i].high - candles[i].low,
                             Math.abs(candles[i].high - candles[i-1].close),
                             Math.abs(candles[i].low - candles[i-1].close)));
    }
    var smoothTR = trVals.slice(0, period).reduce(function(a,b){return a+b;},0);
    var smoothPDM = pDM.slice(0, period).reduce(function(a,b){return a+b;},0);
    var smoothNDM = nDM.slice(0, period).reduce(function(a,b){return a+b;},0);
    var dxVals = [];
    for (var i = period; i < trVals.length; i++) {
        if (i > period) {
            smoothTR = smoothTR - smoothTR / period + trVals[i];
            smoothPDM = smoothPDM - smoothPDM / period + pDM[i];
            smoothNDM = smoothNDM - smoothNDM / period + nDM[i];
        }
        var pDI = smoothTR > 0 ? 100 * smoothPDM / smoothTR : 0;
        var nDI = smoothTR > 0 ? 100 * smoothNDM / smoothTR : 0;
        var diSum = pDI + nDI;
        dxVals.push(diSum > 0 ? 100 * Math.abs(pDI - nDI) / diSum : 0);
    }
    if (dxVals.length < period) return dxVals.length > 0 ? dxVals[dxVals.length - 1] : 25;
    var adxVal = dxVals.slice(0, period).reduce(function(a,b){return a+b;},0) / period;
    for (var i = period; i < dxVals.length; i++) {
        adxVal = (adxVal * (period - 1) + dxVals[i]) / period;
    }
    return adxVal;
}

function cci(candles, period) {
    if (candles.length < period) return 0;
    var slice = candles.slice(-period);
    var tps = slice.map(function(c){return (c.high+c.low+c.close)/3;});
    var mean = tps.reduce(function(a,b){return a+b;},0)/tps.length;
    var md = tps.reduce(function(a,v){return a+Math.abs(v-mean);},0)/tps.length;
    return md > 0 ? (tps[tps.length-1] - mean) / (0.015 * md) : 0;
}

function mfi(candles, period) {
    if (candles.length < period + 1) return 50;
    var posFlow = 0, negFlow = 0;
    for (var i = candles.length - period; i < candles.length; i++) {
        var tp = (candles[i].high + candles[i].low + candles[i].close) / 3;
        var prevTp = (candles[i-1].high + candles[i-1].low + candles[i-1].close) / 3;
        var rawMf = tp * candles[i].volume;
        if (tp > prevTp) posFlow += rawMf;
        else if (tp < prevTp) negFlow += rawMf;
    }
    if (negFlow === 0) return 100;
    var mfRatio = posFlow / negFlow;
    return 100 - (100 / (1 + mfRatio));
}

function williamsR(candles, period) {
    if (candles.length < period) return -50;
    var s = candles.slice(-period);
    var high = Math.max.apply(null,s.map(function(c){return c.high;}));
    var low = Math.min.apply(null,s.map(function(c){return c.low;}));
    return (high-low) > 0 ? -100*(high-candles[candles.length-1].close)/(high-low) : -50;
}

// ─── Series functions (return array, one value per candle) ─────────
// These are used by custom indicators to return plottable data arrays.

function smaSeries(candles, period) {
    var result = [];
    for (var i = 0; i < candles.length; i++) {
        if (i < period - 1) { result.push(null); continue; }
        var sum = 0;
        for (var j = i - period + 1; j <= i; j++) sum += candles[j].close;
        result.push(sum / period);
    }
    return result;
}

function emaSeries(candles, period) {
    if (candles.length === 0) return [];
    var result = [];
    var mult = 2.0 / (period + 1);
    var val_ = 0;
    for (var i = 0; i < candles.length; i++) {
        if (i < period - 1) { result.push(null); continue; }
        if (i === period - 1) {
            var sum = 0;
            for (var j = 0; j < period; j++) sum += candles[j].close;
            val_ = sum / period;
            result.push(val_);
        } else {
            val_ = (candles[i].close - val_) * mult + val_;
            result.push(val_);
        }
    }
    return result;
}

function rsiSeries(candles, period) {
    var result = [];
    if (candles.length < period + 1) {
        for (var i = 0; i < candles.length; i++) result.push(null);
        return result;
    }
    var gains = 0, losses = 0;
    for (var i = 1; i <= period; i++) {
        var ch = candles[i].close - candles[i - 1].close;
        if (ch > 0) gains += ch; else losses -= ch;
    }
    var avgGain = gains / period, avgLoss = losses / period;
    for (var i = 0; i < period; i++) result.push(null);
    result.push(avgLoss === 0 ? 100 : 100 - (100 / (1 + avgGain / avgLoss)));
    for (var i = period + 1; i < candles.length; i++) {
        var ch = candles[i].close - candles[i - 1].close;
        avgGain = (avgGain * (period - 1) + (ch > 0 ? ch : 0)) / period;
        avgLoss = (avgLoss * (period - 1) + (ch < 0 ? -ch : 0)) / period;
        result.push(avgLoss === 0 ? 100 : 100 - (100 / (1 + avgGain / avgLoss)));
    }
    return result;
}

function atrSeries(candles, period) {
    var result = [null];
    if (candles.length < 2) return [null];
    var trVals = [];
    for (var i = 1; i < candles.length; i++) {
        trVals.push(Math.max(candles[i].high - candles[i].low,
            Math.abs(candles[i].high - candles[i-1].close),
            Math.abs(candles[i].low - candles[i-1].close)));
    }
    for (var i = 0; i < trVals.length; i++) {
        if (i < period - 1) { result.push(null); continue; }
        if (i === period - 1) {
            var sum = 0;
            for (var j = 0; j < period; j++) sum += trVals[j];
            result.push(sum / period);
        } else {
            var prev = result[result.length - 1];
            result.push((prev * (period - 1) + trVals[i]) / period);
        }
    }
    return result;
}

function bollingerSeries(candles, period, numStd) {
    var upper = [], middle = [], lower = [];
    for (var i = 0; i < candles.length; i++) {
        if (i < period - 1) { upper.push(null); middle.push(null); lower.push(null); continue; }
        var sum = 0;
        for (var j = i - period + 1; j <= i; j++) sum += candles[j].close;
        var mean = sum / period;
        var variance = 0;
        for (var j = i - period + 1; j <= i; j++) variance += (candles[j].close - mean) * (candles[j].close - mean);
        var sd = Math.sqrt(variance / period);
        upper.push(mean + numStd * sd);
        middle.push(mean);
        lower.push(mean - numStd * sd);
    }
    return { upper: upper, middle: middle, lower: lower };
}

function macdSeries(candles, fast, slow, sig) {
    var emaF = emaSeries(candles, fast);
    var emaS = emaSeries(candles, slow);
    var macdLine = [], signalLine = [], histogram = [];
    var macdVals = [];
    for (var i = 0; i < candles.length; i++) {
        if (emaF[i] === null || emaS[i] === null) {
            macdLine.push(null); signalLine.push(null); histogram.push(null);
        } else {
            var m = emaF[i] - emaS[i];
            macdLine.push(m);
            macdVals.push(m);
        }
    }
    // Compute signal line as EMA of MACD values
    var sigMult = 2.0 / (sig + 1);
    var sigVal = null;
    var sigIdx = 0;
    for (var i = 0; i < candles.length; i++) {
        if (macdLine[i] === null) { signalLine.push(null); histogram.push(null); continue; }
        sigIdx++;
        if (sigIdx < sig) { signalLine.push(null); histogram.push(null); continue; }
        if (sigIdx === sig) {
            var sum = 0;
            for (var j = macdVals.length - sig; j < macdVals.length; j++) sum += macdVals[j];
            sigVal = sum / sig;
        } else {
            sigVal = (macdLine[i] - sigVal) * sigMult + sigVal;
        }
        signalLine.push(sigVal);
        histogram.push(macdLine[i] - sigVal);
    }
    return { macd: macdLine, signal: signalLine, histogram: histogram };
}

function obvSeries(candles) {
    var result = [0];
    var val_ = 0;
    for (var i = 1; i < candles.length; i++) {
        if (candles[i].close > candles[i-1].close) val_ += candles[i].volume;
        else if (candles[i].close < candles[i-1].close) val_ -= candles[i].volume;
        result.push(val_);
    }
    return result;
}

function vwapSeries(candles) {
    var result = [];
    var cumVol = 0, cumTpVol = 0;
    for (var i = 0; i < candles.length; i++) {
        var tp = (candles[i].high + candles[i].low + candles[i].close) / 3;
        cumTpVol += tp * candles[i].volume;
        cumVol += candles[i].volume;
        result.push(cumVol > 0 ? cumTpVol / cumVol : candles[i].close);
    }
    return result;
}

function stochasticSeries(candles, kPeriod, dPeriod) {
    var kVals = [], dVals = [];
    for (var i = 0; i < candles.length; i++) {
        if (i < kPeriod - 1) { kVals.push(null); dVals.push(null); continue; }
        var slice = candles.slice(i - kPeriod + 1, i + 1);
        var high = Math.max.apply(null, slice.map(function(c){return c.high;}));
        var low = Math.min.apply(null, slice.map(function(c){return c.low;}));
        var k = (high - low) > 0 ? (candles[i].close - low) / (high - low) * 100 : 50;
        kVals.push(k);
        // D = SMA of last dPeriod K values
        var validK = kVals.filter(function(v){return v !== null;});
        if (validK.length >= dPeriod) {
            var sum = 0;
            for (var j = validK.length - dPeriod; j < validK.length; j++) sum += validK[j];
            dVals.push(sum / dPeriod);
        } else {
            dVals.push(null);
        }
    }
    return { k: kVals, d: dVals };
}

// ─── WMA / VWMA / HMA / DEMA / TEMA ────────────────────────────────────────

function wmaSeries(candles, period) {
    var result = [];
    var weightSum = period * (period + 1) / 2;
    for (var i = 0; i < candles.length; i++) {
        if (i < period - 1) { result.push(null); continue; }
        var sum = 0;
        for (var j = 0; j < period; j++) {
            sum += candles[i - period + 1 + j].close * (j + 1);
        }
        result.push(sum / weightSum);
    }
    return result;
}

function vwmaSeries(candles, period) {
    var result = [];
    for (var i = 0; i < candles.length; i++) {
        if (i < period - 1) { result.push(null); continue; }
        var volSum = 0, priceVolSum = 0;
        for (var j = i - period + 1; j <= i; j++) {
            volSum += candles[j].volume;
            priceVolSum += candles[j].close * candles[j].volume;
        }
        result.push(volSum > 0 ? priceVolSum / volSum : candles[i].close);
    }
    return result;
}

function hmaSeries(candles, period) {
    var half = Math.max(Math.floor(period / 2), 1);
    var wmaHalf = wmaSeries(candles, half);
    var wmaFull = wmaSeries(candles, period);
    // Raw HMA input = 2*WMA(half) - WMA(full)
    var raw = [];
    for (var i = 0; i < candles.length; i++) {
        if (wmaHalf[i] === null || wmaFull[i] === null) { raw.push(null); continue; }
        raw.push(2 * wmaHalf[i] - wmaFull[i]);
    }
    // Apply WMA(sqrt(period)) over raw
    var sqrtP = Math.max(Math.round(Math.sqrt(period)), 1);
    var weightSum = sqrtP * (sqrtP + 1) / 2;
    var result = [];
    for (var i = 0; i < raw.length; i++) {
        if (i < sqrtP - 1 || raw[i] === null) { result.push(null); continue; }
        var ok = true, sum = 0;
        for (var j = 0; j < sqrtP; j++) {
            if (raw[i - sqrtP + 1 + j] === null) { ok = false; break; }
            sum += raw[i - sqrtP + 1 + j] * (j + 1);
        }
        result.push(ok ? sum / weightSum : null);
    }
    return result;
}

function demaSeries(candles, period) {
    var ema1 = emaSeries(candles, period);
    // Build EMA of EMA
    var ema2 = [];
    var mult = 2.0 / (period + 1);
    var val_ = null, count = 0, sumAcc = 0;
    for (var i = 0; i < ema1.length; i++) {
        if (ema1[i] === null) { ema2.push(null); continue; }
        if (val_ === null) {
            sumAcc += ema1[i]; count++;
            if (count === period) { val_ = sumAcc / period; ema2.push(2 * ema1[i] - val_); }
            else ema2.push(null);
        } else {
            val_ = (ema1[i] - val_) * mult + val_;
            ema2.push(2 * ema1[i] - val_);
        }
    }
    return ema2;
}

function temaSeries(candles, period) {
    var ema1 = emaSeries(candles, period);
    var mult = 2.0 / (period + 1);
    // EMA2
    var ema2 = [];
    var v2 = null, c2 = 0, s2 = 0;
    for (var i = 0; i < ema1.length; i++) {
        if (ema1[i] === null) { ema2.push(null); continue; }
        if (v2 === null) {
            s2 += ema1[i]; c2++;
            if (c2 === period) { v2 = s2 / period; ema2.push(v2); } else ema2.push(null);
        } else {
            v2 = (ema1[i] - v2) * mult + v2; ema2.push(v2);
        }
    }
    // EMA3
    var ema3 = [];
    var v3 = null, c3 = 0, s3 = 0;
    for (var i = 0; i < ema2.length; i++) {
        if (ema2[i] === null) { ema3.push(null); continue; }
        if (v3 === null) {
            s3 += ema2[i]; c3++;
            if (c3 === period) { v3 = s3 / period; ema3.push(v3); } else ema3.push(null);
        } else {
            v3 = (ema2[i] - v3) * mult + v3; ema3.push(v3);
        }
    }
    var result = [];
    for (var i = 0; i < candles.length; i++) {
        if (ema1[i] === null || ema2[i] === null || ema3[i] === null) { result.push(null); continue; }
        result.push(3 * ema1[i] - 3 * ema2[i] + ema3[i]);
    }
    return result;
}

// ─── ADX / CCI / MFI / Williams%R / CMF ─────────────────────────────────────

function adxSeries(candles, period) {
    var n = candles.length;
    var result = [];
    if (n < 2) { for (var i = 0; i < n; i++) result.push(null); return result; }
    var trList = [], plusDmList = [], minusDmList = [];
    for (var i = 1; i < n; i++) {
        var h = candles[i].high, l = candles[i].low, pc = candles[i-1].close;
        var ph = candles[i-1].high, pl = candles[i-1].low;
        trList.push(Math.max(h - l, Math.abs(h - pc), Math.abs(l - pc)));
        var up = h - ph, down = pl - l;
        plusDmList.push(up > down && up > 0 ? up : 0);
        minusDmList.push(down > up && down > 0 ? down : 0);
    }
    // Wilder smooth
    function wilderSmooth(data, p) {
        if (data.length < p) return [];
        var out = [];
        var sum = 0;
        for (var i = 0; i < p; i++) sum += data[i];
        out.push(sum);
        for (var i = p; i < data.length; i++) {
            sum = sum - sum / p + data[i];
            out.push(sum);
        }
        return out;
    }
    var sTr = wilderSmooth(trList, period);
    var sPDm = wilderSmooth(plusDmList, period);
    var sMDm = wilderSmooth(minusDmList, period);
    var diPlus = [], diMinus = [], dx = [];
    for (var i = 0; i < sTr.length; i++) {
        var dp = sTr[i] > 0 ? 100 * sPDm[i] / sTr[i] : 0;
        var dm = sTr[i] > 0 ? 100 * sMDm[i] / sTr[i] : 0;
        diPlus.push(dp); diMinus.push(dm);
        var s = dp + dm;
        dx.push(s > 0 ? 100 * Math.abs(dp - dm) / s : 0);
    }
    var adxVals = [];
    if (dx.length >= period) {
        var sum = 0;
        for (var i = 0; i < period; i++) sum += dx[i];
        adxVals.push(sum / period);
        for (var i = period; i < dx.length; i++) {
            adxVals.push((adxVals[adxVals.length - 1] * (period - 1) + dx[i]) / period);
        }
    }
    // Align: trList starts at index 1, sTr starts at index period (relative to trList = period+1 candle index)
    var offset = period + period; // need 2*period candles for first ADX value
    for (var i = 0; i < n; i++) {
        if (i < offset) result.push(null);
        else result.push(adxVals[i - offset] !== undefined ? adxVals[i - offset] : null);
    }
    return result;
}

function cciSeries(candles, period) {
    var result = [];
    for (var i = 0; i < candles.length; i++) {
        if (i < period - 1) { result.push(null); continue; }
        var slice = candles.slice(i - period + 1, i + 1);
        var tps = slice.map(function(c){ return (c.high + c.low + c.close) / 3; });
        var mean = tps.reduce(function(a,b){return a+b;}, 0) / period;
        var meanDev = tps.map(function(v){return Math.abs(v - mean);}).reduce(function(a,b){return a+b;}, 0) / period;
        result.push(meanDev > 0 ? (tps[tps.length-1] - mean) / (0.015 * meanDev) : 0);
    }
    return result;
}

function mfiSeries(candles, period) {
    var result = [];
    for (var i = 0; i < candles.length; i++) {
        if (i < period) { result.push(null); continue; }
        var posFlow = 0, negFlow = 0;
        for (var j = i - period + 1; j <= i; j++) {
            var tp  = (candles[j].high + candles[j].low + candles[j].close) / 3;
            var ptp = (candles[j-1].high + candles[j-1].low + candles[j-1].close) / 3;
            var raw = tp * candles[j].volume;
            if (tp > ptp) posFlow += raw;
            else if (tp < ptp) negFlow += raw;
        }
        result.push(negFlow === 0 ? 100 : 100 - (100 / (1 + posFlow / negFlow)));
    }
    return result;
}

function williamsRSeries(candles, period) {
    var result = [];
    for (var i = 0; i < candles.length; i++) {
        if (i < period - 1) { result.push(null); continue; }
        var slice = candles.slice(i - period + 1, i + 1);
        var high = Math.max.apply(null, slice.map(function(c){return c.high;}));
        var low  = Math.min.apply(null, slice.map(function(c){return c.low;}));
        result.push(high - low > 0 ? -100 * (high - candles[i].close) / (high - low) : -50);
    }
    return result;
}

function cmfSeries(candles, period) {
    var result = [];
    for (var i = 0; i < candles.length; i++) {
        if (i < period - 1) { result.push(null); continue; }
        var mfVol = 0, totVol = 0;
        for (var j = i - period + 1; j <= i; j++) {
            var c = candles[j];
            var clv = c.high !== c.low ? ((c.close - c.low) - (c.high - c.close)) / (c.high - c.low) : 0;
            mfVol += clv * c.volume; totVol += c.volume;
        }
        result.push(totVol > 0 ? mfVol / totVol : 0);
    }
    return result;
}

// ─── Supertrend Series ───────────────────────────────────────────────────────

function supertrendSeries(candles, period, multiplier) {
    if (period === undefined) period = 10;
    if (multiplier === undefined) multiplier = 3.0;
    var n = candles.length;
    var upLine  = [], downLine = [], direction = [];
    for (var i = 0; i < n; i++) { upLine.push(null); downLine.push(null); direction.push(null); }
    if (n < period + 2) return { up: upLine, down: downLine, direction: direction };
    var trList = [];
    for (var i = 1; i < n; i++) {
        var h = candles[i].high, l = candles[i].low, pc = candles[i-1].close;
        trList.push(Math.max(h - l, Math.abs(h - pc), Math.abs(l - pc)));
    }
    // Initial ATR = simple average of first `period` TR values
    var atr = 0;
    for (var i = 0; i < period; i++) atr += trList[i];
    atr /= period;
    var atrSmooth = [atr];
    for (var i = period; i < trList.length; i++) {
        atr = (atrSmooth[atrSmooth.length - 1] * (period - 1) + trList[i]) / period;
        atrSmooth.push(atr);
    }
    var st = candles[period].close, isUp = true;
    for (var i = 0; i < atrSmooth.length; i++) {
        var ci = i + 1;
        if (ci >= n) break;
        var hl2 = (candles[ci].high + candles[ci].low) / 2;
        var ub = hl2 + multiplier * atrSmooth[i];
        var lb = hl2 - multiplier * atrSmooth[i];
        if (candles[ci].close > st) { st = lb; isUp = true; }
        else { st = ub; isUp = false; }
        if (isUp) { upLine[ci] = st; direction[ci] = 1; }
        else { downLine[ci] = st; direction[ci] = -1; }
    }
    return { up: upLine, down: downLine, direction: direction };
}

// ─── Ichimoku Series ─────────────────────────────────────────────────────────

function ichimokuSeries(candles, tenkanP, kijunP, senkouBP) {
    if (tenkanP  === undefined) tenkanP  = 9;
    if (kijunP   === undefined) kijunP   = 26;
    if (senkouBP === undefined) senkouBP = 52;
    var n = candles.length;
    function mid(endIdx, p) {
        if (endIdx + 1 < p) return null;
        var s = candles.slice(endIdx + 1 - p, endIdx + 1);
        var hi = s[0].high, lo = s[0].low;
        for (var i = 1; i < s.length; i++) {
            if (s[i].high > hi) hi = s[i].high;
            if (s[i].low  < lo) lo = s[i].low;
        }
        return (hi + lo) / 2;
    }
    var tenkan = [], kijun = [], senkouA = [], senkouB = [], chikou = [];
    for (var i = 0; i < n; i++) {
        var t = mid(i, tenkanP), k = mid(i, kijunP);
        tenkan.push(t); kijun.push(k);
        senkouA.push(t !== null && k !== null ? (t + k) / 2 : null);
        senkouB.push(mid(i, senkouBP));
        chikou.push(i + kijunP < n ? candles[i].close : null);
    }
    return { tenkan: tenkan, kijun: kijun, senkouA: senkouA, senkouB: senkouB, chikou: chikou };
}

// ─── Stochastic RSI Series ───────────────────────────────────────────────────

function stochasticRsiSeries(candles, rsiPeriod, stochPeriod, kSmooth, dSmooth) {
    if (rsiPeriod  === undefined) rsiPeriod  = 14;
    if (stochPeriod === undefined) stochPeriod = 14;
    if (kSmooth    === undefined) kSmooth    = 3;
    if (dSmooth    === undefined) dSmooth    = 3;
    var rsiVals = rsiSeries(candles, rsiPeriod);
    var n = rsiVals.length;
    var stochRaw = [];
    for (var i = 0; i < n; i++) {
        if (i < stochPeriod - 1 || rsiVals[i] === null) { stochRaw.push(null); continue; }
        var slice = rsiVals.slice(i - stochPeriod + 1, i + 1);
        var hi = slice[0], lo = slice[0];
        for (var j = 1; j < slice.length; j++) {
            if (slice[j] > hi) hi = slice[j];
            if (slice[j] < lo) lo = slice[j];
        }
        stochRaw.push(hi - lo > 0 ? (rsiVals[i] - lo) / (hi - lo) * 100 : 50);
    }
    // Smooth K
    var kLine = [];
    for (var i = 0; i < n; i++) {
        if (i < kSmooth - 1) { kLine.push(null); continue; }
        var window = [];
        for (var j = i - kSmooth + 1; j <= i; j++) { if (stochRaw[j] !== null) window.push(stochRaw[j]); }
        kLine.push(window.length === kSmooth ? window.reduce(function(a,b){return a+b;},0) / kSmooth : null);
    }
    // Smooth D
    var dLine = [];
    for (var i = 0; i < n; i++) {
        if (i < dSmooth - 1) { dLine.push(null); continue; }
        var window = [];
        for (var j = i - dSmooth + 1; j <= i; j++) { if (kLine[j] !== null) window.push(kLine[j]); }
        dLine.push(window.length === dSmooth ? window.reduce(function(a,b){return a+b;},0) / dSmooth : null);
    }
    return { k: kLine, d: dLine };
}

// ─── Helper Utilities ────────────────────────────────────────────────────────

/** crossover(a, b, i) — true if series a crossed above series b at index i */
function crossover(a, b, i) {
    if (i < 1 || a[i] === null || b[i] === null || a[i-1] === null || b[i-1] === null) return false;
    return a[i-1] <= b[i-1] && a[i] > b[i];
}

/** crossunder(a, b, i) — true if series a crossed below series b at index i */
function crossunder(a, b, i) {
    if (i < 1 || a[i] === null || b[i] === null || a[i-1] === null || b[i-1] === null) return false;
    return a[i-1] >= b[i-1] && a[i] < b[i];
}

/** highest(arr, period, i) — highest non-null value in arr[i-period+1 .. i] */
function highest(arr, period, i) {
    if (i < period - 1) return null;
    var best = null;
    for (var j = i - period + 1; j <= i; j++) {
        if (arr[j] !== null && (best === null || arr[j] > best)) best = arr[j];
    }
    return best;
}

/** lowest(arr, period, i) — lowest non-null value in arr[i-period+1 .. i] */
function lowest(arr, period, i) {
    if (i < period - 1) return null;
    var best = null;
    for (var j = i - period + 1; j <= i; j++) {
        if (arr[j] !== null && (best === null || arr[j] < best)) best = arr[j];
    }
    return best;
}

/** change(arr, i, lookback) — arr[i] - arr[i - lookback] */
function change(arr, i, lookback) {
    if (lookback === undefined) lookback = 1;
    if (i < lookback || arr[i] === null || arr[i - lookback] === null) return null;
    return arr[i] - arr[i - lookback];
}

// ─── Source Selector ─────────────────────────────────────────────────────────

/** source(candles, field) — extract a field ("open","high","low","close","volume","hl2","hlc3","ohlc4") */
function source(candles, field) {
    if (field === undefined) field = "close";
    return candles.map(function(c) {
        switch(field) {
            case "open":   return c.open;
            case "high":   return c.high;
            case "low":    return c.low;
            case "close":  return c.close;
            case "volume": return c.volume;
            case "hl2":    return (c.high + c.low) / 2;
            case "hlc3":   return (c.high + c.low + c.close) / 3;
            case "ohlc4":  return (c.open + c.high + c.low + c.close) / 4;
            default:       return c.close;
        }
    });
}

// ─── Color Constants & Helpers ───────────────────────────────────────────────

var color = {
    green:     "#26A69A",
    red:       "#EF5350",
    blue:      "#2196F3",
    orange:    "#FF9800",
    yellow:    "#FFD700",
    purple:    "#9C27B0",
    white:     "#FFFFFF",
    gray:      "#8B949E",
    aqua:      "#00BCD4",
    lime:      "#00E676",
    pink:      "#FF4081",
    silver:    "#C0C0C0",
    none:      "transparent",
    rgba: function(r, g, b, a) {
        if (a === undefined) a = 1.0;
        return "rgba(" + r + "," + g + "," + b + "," + a + ")";
    },
    hexAlpha: function(hex, alpha) {
        var a = Math.round(alpha * 255).toString(16);
        if (a.length === 1) a = "0" + a;
        return hex + a;
    }
};

// ─── Linear Regression ───────────────────────────────────────────────────────

/** linreg(arr, period, i) — linear regression value at index i over last `period` bars */
function linreg(arr, period, i) {
    if (i < period - 1) return null;
    var sumX = 0, sumY = 0, sumXY = 0, sumXX = 0, n = 0;
    for (var j = 0; j < period; j++) {
        var idx = i - period + 1 + j;
        if (arr[idx] === null) return null;
        sumX += j; sumY += arr[idx]; sumXY += j * arr[idx]; sumXX += j * j;
        n++;
    }
    var slope = (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX);
    var intercept = (sumY - slope * sumX) / n;
    return intercept + slope * (period - 1);
}

/** linregSeries(arr, period) — full linear regression series */
function linregSeries(arr, period) {
    var result = [];
    for (var i = 0; i < arr.length; i++) {
        result.push(linreg(arr, period, i));
    }
    return result;
}

// ─── Pivot Points ────────────────────────────────────────────────────────────

/** pivotHigh(candles, leftBars, rightBars) — array of pivot-high prices (null where no pivot) */
function pivotHigh(candles, leftBars, rightBars) {
    var result = [];
    for (var i = 0; i < candles.length; i++) {
        if (i < leftBars || i + rightBars >= candles.length) { result.push(null); continue; }
        var isHigh = true;
        for (var j = i - leftBars; j <= i + rightBars; j++) {
            if (j !== i && candles[j].high >= candles[i].high) { isHigh = false; break; }
        }
        result.push(isHigh ? candles[i].high : null);
    }
    return result;
}

/** pivotLow(candles, leftBars, rightBars) — array of pivot-low prices (null where no pivot) */
function pivotLow(candles, leftBars, rightBars) {
    var result = [];
    for (var i = 0; i < candles.length; i++) {
        if (i < leftBars || i + rightBars >= candles.length) { result.push(null); continue; }
        var isLow = true;
        for (var j = i - leftBars; j <= i + rightBars; j++) {
            if (j !== i && candles[j].low <= candles[i].low) { isLow = false; break; }
        }
        result.push(isLow ? candles[i].low : null);
    }
    return result;
}

// ─── Input Function  ─────────────────────────────────────────────────────────
// input(name, defaultValue) — used by IndicatorEngine to extract configurable params.
// At runtime the engine pre-injects input values as JS vars, so this just returns the default.
var _inputs = {};
function input(name, defaultValue) {
    if (_inputs[name] !== undefined) return _inputs[name];
    return defaultValue;
}
""".trimIndent()
    }
}
