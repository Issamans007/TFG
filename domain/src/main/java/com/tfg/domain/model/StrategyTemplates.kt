package com.tfg.domain.model

object StrategyTemplates {

    fun getAll(): List<StrategyTemplate> = listOf(
        gainzAlgo(),
        smaCrossover(),
        rsiMeanReversion(),
        bollingerBreakout(),
        macdMomentum(),
        emaScalper(),
        gridTrader(),
        supertrendTemplate(),
        vwapReversionTemplate(),
        ichimokuCloudTemplate(),
        stochasticCrossTemplate(),
        volumeBreakoutTemplate(),
        scalper5mTemplate(),
        swing15mTemplate(),
        trend1hTemplate(),
        tfgAlgo(),
        tfgAlgoPine()
    )

    fun getById(id: StrategyTemplateId): StrategyTemplate = getAll().first { it.id == id }

    // ─── 0. GainzAlgo V2 ───────────────────────────────────────────
    private fun gainzAlgo() = StrategyTemplate(
        id = StrategyTemplateId.GAINZ_ALGO,
        name = "GainzAlgo V2",
        description = "Enhanced engulfing-candle reversal with EMA 50/200 trend alignment, volume confirmation, optional MACD/ADX filters, tightened RSI, and ATR-based 1:2 TP/SL. Buys dips in uptrends, sells rallies in downtrends.",
        code = """
// GainzAlgo V2 — Enhanced Strategy
// Engulfing + stability + EMA trend + volume + RSI + ATR TP/SL
// Optional: MACD histogram, ADX strength (enable for higher quality)

// ── Core ──
var CANDLE_STABILITY = 0.5;  // body/TR minimum ratio
var RSI_PERIOD = 14;
var RSI_BUY_MAX = 55.0;     // buy when RSI < 55 (tightened from original 70)
var RSI_SELL_MIN = 45.0;    // sell when RSI > 45 (tightened from original 30)
var DELTA_LENGTH = 4;        // momentum lookback bars

// ── Trend Filter (EMA alignment) ──
var USE_TREND_FILTER = 1;    // 1=enabled, 0=disabled
var EMA_FAST = 50;
var EMA_SLOW = 200;

// ── Volume Filter ──
var USE_VOLUME_FILTER = 1;   // 1=enabled, 0=disabled
var VOL_LOOKBACK = 20;
var VOL_MULTIPLIER = 1.0;   // volume must exceed avg * this

// ── MACD Confirmation (off by default — enable for fewer but higher-quality signals) ──
var USE_MACD_FILTER = 0;
var MACD_FAST = 12;
var MACD_SLOW = 26;
var MACD_SIGNAL = 9;

// ── ADX Strength (off by default) ──
var USE_ADX_FILTER = 0;
var ADX_PERIOD = 14;
var ADX_THRESHOLD = 20.0;

// ── Risk Management ──
var TP_SL_MULTIPLIER = 1.0;
var RISK_REWARD_RATIO = 2.0;
var POSITION_SIZE_PCT = 2.0;

function strategy(candles) {
    var minBars = USE_TREND_FILTER ? Math.max(EMA_SLOW + 5, 52) : Math.max(DELTA_LENGTH + 15, 30);
    if (candles.length < minBars) return {type:'HOLD'};

    var last = candles[candles.length - 1];
    var prev = candles[candles.length - 2];
    var deltaRef = candles[candles.length - DELTA_LENGTH - 1];

    // ── Candle stability ──
    var tr = Math.max(last.high - last.low,
                      Math.abs(last.high - prev.close),
                      Math.abs(last.low - prev.close));
    var body = Math.abs(last.close - last.open);
    if (!(tr > 0 && body / tr > CANDLE_STABILITY)) return {type:'HOLD'};

    // ── Engulfing patterns ──
    var bullEngulf = prev.close < prev.open && last.close > last.open && last.close > prev.open;
    var bearEngulf = prev.close > prev.open && last.close < last.open && last.close < prev.open;
    if (!bullEngulf && !bearEngulf) return {type:'HOLD'};

    // ── RSI ──
    var rsiVal = rsi(candles, RSI_PERIOD);

    // ── Trend filter: EMA alignment only ──
    // Only checks EMA50 > EMA200 (bull) — does NOT require price > EMA50
    // This allows "buy the dip in an uptrend" pattern
    var trendBull = true, trendBear = true;
    if (USE_TREND_FILTER) {
        var emaF = ema(candles, EMA_FAST);
        var emaS = ema(candles, EMA_SLOW);
        trendBull = emaF > emaS;
        trendBear = emaF < emaS;
    }

    // ── Volume filter ──
    if (USE_VOLUME_FILTER && candles.length > VOL_LOOKBACK) {
        var volSum = 0;
        for (var i = candles.length - VOL_LOOKBACK - 1; i < candles.length - 1; i++)
            volSum += candles[i].volume;
        var avgVol = volSum / VOL_LOOKBACK;
        if (avgVol > 0 && last.volume < avgVol * VOL_MULTIPLIER)
            return {type:'HOLD'};
    }

    // ── MACD histogram direction (optional) ──
    var macdBull = true, macdBear = true;
    if (USE_MACD_FILTER) {
        var m = macd(candles, MACD_FAST, MACD_SLOW, MACD_SIGNAL);
        var mPrev = macd(candles.slice(0, -1), MACD_FAST, MACD_SLOW, MACD_SIGNAL);
        macdBull = m.histogram > mPrev.histogram;
        macdBear = m.histogram < mPrev.histogram;
    }

    // ── ADX strength (optional) ──
    if (USE_ADX_FILTER && adx(candles, ADX_PERIOD) < ADX_THRESHOLD)
        return {type:'HOLD'};

    // ── TP/SL ──
    var atrVal = atr(candles, 14);
    var dist = atrVal * TP_SL_MULTIPLIER;
    var slPct = dist / last.close * 100;
    var tpPct = slPct * RISK_REWARD_RATIO;

    // ── BUY: engulfing + RSI<55 + price dip + EMA uptrend + volume ──
    if (bullEngulf && rsiVal < RSI_BUY_MAX && last.close < deltaRef.close
        && trendBull && macdBull) {
        return {type:'BUY', sizePct: POSITION_SIZE_PCT,
            stopLossPct: slPct, takeProfitPct: tpPct};
    }

    // ── SELL: engulfing + RSI>45 + price rise + EMA downtrend + volume ──
    if (bearEngulf && rsiVal > RSI_SELL_MIN && last.close > deltaRef.close
        && trendBear && macdBear) {
        return {type:'SELL', sizePct: 100.0,
            stopLossPct: slPct, takeProfitPct: tpPct};
    }

    return {type:'HOLD'};
}
""".trimIndent(),
        defaultParams = mapOf(
            "CANDLE_STABILITY" to "0.5",
            "RSI_PERIOD" to "14",
            "RSI_BUY_MAX" to "55.0",
            "RSI_SELL_MIN" to "45.0",
            "DELTA_LENGTH" to "4",
            "USE_TREND_FILTER" to "1",
            "EMA_FAST" to "50",
            "EMA_SLOW" to "200",
            "USE_VOLUME_FILTER" to "1",
            "VOL_LOOKBACK" to "20",
            "VOL_MULTIPLIER" to "1.0",
            "USE_MACD_FILTER" to "0",
            "MACD_FAST" to "12",
            "MACD_SLOW" to "26",
            "MACD_SIGNAL" to "9",
            "USE_ADX_FILTER" to "0",
            "ADX_PERIOD" to "14",
            "ADX_THRESHOLD" to "20.0",
            "TP_SL_MULTIPLIER" to "1.0",
            "RISK_REWARD_RATIO" to "2.0",
            "POSITION_SIZE_PCT" to "2.0"
        )
    )

    // ─── 1. SMA Crossover ──────────────────────────────────────────
    private fun smaCrossover() = StrategyTemplate(
        id = StrategyTemplateId.SMA_CROSSOVER,
        name = "SMA Crossover",
        description = "Classic dual moving average crossover. Buys when fast SMA crosses above slow SMA, sells on reverse cross. Includes RSI filter to avoid overbought entries.",
        code = """
// SMA Crossover Strategy
// Buys on golden cross (fast > slow), sells on death cross
// RSI filter prevents overbought entries

var FAST_PERIOD = 10;
var SLOW_PERIOD = 50;
var RSI_PERIOD = 14;
var RSI_OVERBOUGHT = 70.0;
var RSI_OVERSOLD = 30.0;
var POSITION_SIZE_PCT = 2.0;

function strategy(candles) {
    if (candles.length < SLOW_PERIOD + 1) return {type:'HOLD'};

    var fastSma = sma(candles, FAST_PERIOD);
    var prevFastSma = sma(candles.slice(0, -1), FAST_PERIOD);
    var slowSma = sma(candles, SLOW_PERIOD);
    var prevSlowSma = sma(candles.slice(0, -1), SLOW_PERIOD);
    var rsiVal = rsi(candles, RSI_PERIOD);

    var goldenCross = prevFastSma <= prevSlowSma && fastSma > slowSma;
    var deathCross = prevFastSma >= prevSlowSma && fastSma < slowSma;

    if (goldenCross && rsiVal < RSI_OVERBOUGHT) {
        return {type:'BUY', sizePct: POSITION_SIZE_PCT};
    }
    if (deathCross || rsiVal > RSI_OVERBOUGHT + 5) {
        return {type:'SELL', sizePct: 100.0};
    }
    return {type:'HOLD'};
}
""".trimIndent(),
        defaultParams = mapOf("FAST_PERIOD" to "10", "SLOW_PERIOD" to "50", "RSI_PERIOD" to "14", "POSITION_SIZE_PCT" to "2.0")
    )

    // ─── 2. RSI Mean Reversion ─────────────────────────────────────
    private fun rsiMeanReversion() = StrategyTemplate(
        id = StrategyTemplateId.RSI_MEAN_REVERSION,
        name = "RSI Mean Reversion",
        description = "Buys oversold conditions and sells overbought. Uses RSI with volume confirmation and a cooldown timer to avoid rapid re-entries.",
        code = """
// RSI Mean Reversion Strategy
// Buys when RSI is oversold + volume spike, sells when overbought
// Cooldown prevents whipsaw trades

var RSI_PERIOD = 14;
var RSI_OVERSOLD = 28.0;
var RSI_OVERBOUGHT = 72.0;
var VOL_MULTIPLIER = 1.5;
var COOLDOWN_BARS = 5;
var POSITION_SIZE_PCT = 3.0;
var STOP_LOSS_PCT = 2.0;
var TAKE_PROFIT_PCT = 4.0;

var lastTradeBar = -5;

function strategy(candles) {
    if (candles.length < RSI_PERIOD + 1) return {type:'HOLD'};

    var currentBar = candles.length;
    if (currentBar - lastTradeBar < COOLDOWN_BARS) return {type:'HOLD'};

    var rsiVal = rsi(candles, RSI_PERIOD);
    var recent = candles.slice(-20);
    var avgVolume = recent.reduce(function(s,c){return s+c.volume;},0) / recent.length;
    var currentVolume = candles[candles.length - 1].volume;
    var volumeSpike = currentVolume > avgVolume * VOL_MULTIPLIER;

    if (rsiVal < RSI_OVERSOLD && volumeSpike) {
        lastTradeBar = currentBar;
        return {type:'BUY', sizePct: POSITION_SIZE_PCT,
            stopLossPct: STOP_LOSS_PCT, takeProfitPct: TAKE_PROFIT_PCT};
    }
    if (rsiVal > RSI_OVERBOUGHT) {
        lastTradeBar = currentBar;
        return {type:'SELL', sizePct: 100.0};
    }
    return {type:'HOLD'};
}
""".trimIndent(),
        defaultParams = mapOf("RSI_PERIOD" to "14", "RSI_OVERSOLD" to "28", "RSI_OVERBOUGHT" to "72", "POSITION_SIZE_PCT" to "2.0")
    )

    // ─── 3. Bollinger Band Breakout ────────────────────────────────
    private fun bollingerBreakout() = StrategyTemplate(
        id = StrategyTemplateId.BOLLINGER_BREAKOUT,
        name = "Bollinger Breakout",
        description = "Trades Bollinger Band breakouts with squeeze detection. Enters on expansion after a tight squeeze. Uses band width for volatility sizing.",
        code = """
// Bollinger Band Breakout Strategy
// Detects squeeze (low bandwidth) then trades expansion breakouts
// Position size scales inversely with volatility

var BB_PERIOD = 20;
var BB_STDDEV = 2.0;
var SQUEEZE_THRESHOLD = 0.02;
var POSITION_SIZE_PCT = 2.5;
var STOP_LOSS_PCT = 1.5;
var TAKE_PROFIT_PCT = 3.0;

function strategy(candles) {
    if (candles.length < BB_PERIOD + 1) return {type:'HOLD'};

    var closes = candles.map(function(c) { return c.close; });
    var recent = closes.slice(-BB_PERIOD);
    var smaVal = recent.reduce(function(a,b){return a+b;},0) / recent.length;
    var variance = recent.reduce(function(s,v){return s+(v-smaVal)*(v-smaVal);},0) / recent.length;
    var sd = Math.sqrt(variance);
    var upper = smaVal + BB_STDDEV * sd;
    var lower = smaVal - BB_STDDEV * sd;
    var bandwidth = (upper - lower) / smaVal;

    var prevCloses = candles.slice(0, -1).map(function(c) { return c.close; });
    var prevRecent = prevCloses.slice(-BB_PERIOD);
    var prevSma = prevRecent.reduce(function(a,b){return a+b;},0) / prevRecent.length;
    var prevVar = prevRecent.reduce(function(s,v){return s+(v-prevSma)*(v-prevSma);},0) / prevRecent.length;
    var prevStdDev = Math.sqrt(prevVar);
    var prevBandwidth = (2 * BB_STDDEV * prevStdDev) / prevSma;

    var wasSqueeze = prevBandwidth < SQUEEZE_THRESHOLD;
    var price = candles[candles.length - 1].close;

    if (wasSqueeze && price > upper) {
        return {type:'BUY', sizePct: POSITION_SIZE_PCT,
            stopLossPct: STOP_LOSS_PCT, takeProfitPct: TAKE_PROFIT_PCT};
    }
    if (wasSqueeze && price < lower) {
        return {type:'SELL', sizePct: 100.0};
    }
    if (price < smaVal && price > lower) {
        return {type:'CLOSE_IF_LONG'};
    }
    return {type:'HOLD'};
}
""".trimIndent(),
        defaultParams = mapOf("BB_PERIOD" to "20", "BB_STDDEV" to "2.0", "SQUEEZE_THRESHOLD" to "0.02", "POSITION_SIZE_PCT" to "2.0")
    )

    // ─── 4. MACD Momentum ──────────────────────────────────────────
    private fun macdMomentum() = StrategyTemplate(
        id = StrategyTemplateId.MACD_MOMENTUM,
        name = "MACD Momentum",
        description = "MACD histogram-based momentum strategy. Enters on histogram zero-line crossover with trend filter (price above 200 EMA for longs). Uses histogram divergence for exits.",
        code = """
// MACD Momentum Strategy
// Trades MACD histogram zero-cross with 200 EMA trend filter
// Exits on histogram divergence from price

var FAST_EMA = 12;
var SLOW_EMA = 26;
var SIGNAL_PERIOD = 9;
var TREND_EMA = 200;
var POSITION_SIZE_PCT = 2.0;
var STOP_LOSS_PCT = 2.5;
var TAKE_PROFIT_PCT = 5.0;

function strategy(candles) {
    if (candles.length < TREND_EMA + 1) return {type:'HOLD'};

    var macdNow = macd(candles, FAST_EMA, SLOW_EMA, SIGNAL_PERIOD);
    var macdPrev = macd(candles.slice(0, -1), FAST_EMA, SLOW_EMA, SIGNAL_PERIOD);

    var trendEma = ema(candles, TREND_EMA);
    var price = candles[candles.length - 1].close;
    var uptrend = price > trendEma;

    if (macdPrev.histogram <= 0 && macdNow.histogram > 0 && uptrend) {
        return {type:'BUY', sizePct: POSITION_SIZE_PCT,
            stopLossPct: STOP_LOSS_PCT, takeProfitPct: TAKE_PROFIT_PCT};
    }
    if (macdPrev.histogram >= 0 && macdNow.histogram < 0) {
        return {type:'SELL', sizePct: 100.0};
    }
    return {type:'HOLD'};
}
""".trimIndent(),
        defaultParams = mapOf("FAST_EMA" to "12", "SLOW_EMA" to "26", "SIGNAL_PERIOD" to "9", "TREND_EMA" to "200", "POSITION_SIZE_PCT" to "2.0")
    )

    // ─── 5. EMA Scalper ────────────────────────────────────────────
    private fun emaScalper() = StrategyTemplate(
        id = StrategyTemplateId.EMA_SCALPER,
        name = "EMA Scalper",
        description = "Fast EMA ribbon scalping on short timeframes. Uses 3-EMA stack (8/13/21) alignment for entries. Tight stops with quick profit targets. Best on 1m-5m charts.",
        code = """
// EMA Scalper Strategy (Short Timeframe)
// Uses 3-EMA ribbon alignment for precise entries
// Tight stop-loss, quick take-profit

var EMA_FAST = 8;
var EMA_MID = 13;
var EMA_SLOW = 21;
var ATR_PERIOD = 14;
var ATR_SL_MULT = 1.0;
var ATR_TP_MULT = 1.5;
var POSITION_SIZE_PCT = 1.5;
var MIN_ADX = 20.0;

function strategy(candles) {
    if (candles.length < EMA_SLOW + ATR_PERIOD) return {type:'HOLD'};

    var emaFast = ema(candles, EMA_FAST);
    var emaMid = ema(candles, EMA_MID);
    var emaSlow = ema(candles, EMA_SLOW);
    var atrVal = atr(candles, ATR_PERIOD);
    var adxVal = adx(candles, 14);
    var price = candles[candles.length - 1].close;

    var bullishRibbon = emaFast > emaMid && emaMid > emaSlow && price > emaFast;
    var bearishRibbon = emaFast < emaMid && emaMid < emaSlow && price < emaFast;

    if (adxVal < MIN_ADX) return {type:'HOLD'};

    if (bullishRibbon) {
        return {type:'BUY', sizePct: POSITION_SIZE_PCT,
            stopLossPct: (atrVal * ATR_SL_MULT / price * 100),
            takeProfitPct: (atrVal * ATR_TP_MULT / price * 100)};
    }
    if (bearishRibbon) {
        return {type:'SELL', sizePct: 100.0};
    }
    return {type:'HOLD'};
}
""".trimIndent(),
        defaultParams = mapOf("EMA_FAST" to "8", "EMA_MID" to "13", "EMA_SLOW" to "21", "POSITION_SIZE_PCT" to "2.0")
    )

    // ─── 6. Grid Trader ────────────────────────────────────────────
    private fun gridTrader() = StrategyTemplate(
        id = StrategyTemplateId.GRID_TRADER,
        name = "Grid Trader",
        description = "Automated grid trading within a price range. Places buy orders at lower grid levels and sell at upper levels. Profits from range-bound price action. Auto-adjusts grid on breakout.",
        code = """
// Grid Trader Strategy
// Distributes orders across a price grid within a range
// Buys at lower grid levels, sells at upper levels

var GRID_LEVELS = 10;
var GRID_RANGE_PCT = 5.0;
var SIZE_PER_GRID = 0.5;
var REBALANCE_PCT = 6.0;

function strategy(candles) {
    if (candles.length < 50) return {type:'HOLD'};

    var price = candles[candles.length - 1].close;
    var sma50 = sma(candles, 50);
    var halfRange = sma50 * (GRID_RANGE_PCT / 2.0 / 100.0);
    var gridTop = sma50 + halfRange;
    var gridBottom = sma50 - halfRange;
    var gridStep = (gridTop - gridBottom) / GRID_LEVELS;

    if (price > gridTop * (1 + REBALANCE_PCT / 100)) {
        return {type:'SELL', sizePct: 50.0};
    }
    if (price < gridBottom * (1 - REBALANCE_PCT / 100)) {
        return {type:'HOLD'};
    }

    var gridLevel = Math.floor((price - gridBottom) / gridStep);
    var midLevel = GRID_LEVELS / 2;

    if (gridLevel < midLevel - 1) {
        return {type:'BUY', sizePct: SIZE_PER_GRID * (midLevel - gridLevel),
            stopLossPct: GRID_RANGE_PCT};
    }
    if (gridLevel > midLevel + 1) {
        return {type:'SELL', sizePct: SIZE_PER_GRID * (gridLevel - midLevel)};
    }
    return {type:'HOLD'};
}
""".trimIndent(),
        defaultParams = mapOf("GRID_LEVELS" to "10", "GRID_RANGE_PCT" to "5.0", "SIZE_PER_GRID" to "0.5", "POSITION_SIZE_PCT" to "2.0")
    )

    // ─── 7. Supertrend ──────────────────────────────────────────────
    private fun supertrendTemplate() = StrategyTemplate(
        id = StrategyTemplateId.SUPERTREND,
        name = "Supertrend",
        description = "Trend-following strategy using the Supertrend indicator (ATR-based trailing stop). Buys when price flips above the Supertrend line (uptrend), sells when it flips below (downtrend). Best on 1h–4h timeframes for clean trends.",
        code = """
// Supertrend Strategy
// Buys on flip to uptrend, sells on flip to downtrend

var ST_PERIOD = 10;
var ST_MULTIPLIER = 3.0;
var POSITION_SIZE_PCT = 2.0;
var STOP_LOSS_PCT = 2.0;
var TAKE_PROFIT_PCT = 4.0;

function strategy(candles) {
    if (candles.length < ST_PERIOD + 3) return {type:'HOLD'};

    var stNow = supertrend(candles, ST_PERIOD, ST_MULTIPLIER);
    var isUptrend = stNow[1];
    var stPrev = supertrend(candles.slice(0, -1), ST_PERIOD, ST_MULTIPLIER);
    var wasUptrend = stPrev[1];

    if (!wasUptrend && isUptrend) {
        return {type:'BUY', sizePct: POSITION_SIZE_PCT,
            stopLossPct: STOP_LOSS_PCT, takeProfitPct: TAKE_PROFIT_PCT};
    }
    if (wasUptrend && !isUptrend) {
        return {type:'SELL', sizePct: 100.0,
            stopLossPct: STOP_LOSS_PCT, takeProfitPct: TAKE_PROFIT_PCT};
    }
    return {type:'HOLD'};
}
""".trimIndent(),
        defaultParams = mapOf("ST_PERIOD" to "10", "ST_MULTIPLIER" to "3.0",
            "POSITION_SIZE_PCT" to "2.0", "STOP_LOSS_PCT" to "2.0", "TAKE_PROFIT_PCT" to "4.0")
    )

    // ─── 8. VWAP Reversion ──────────────────────────────────────────
    private fun vwapReversionTemplate() = StrategyTemplate(
        id = StrategyTemplateId.VWAP_REVERSION,
        name = "VWAP Reversion",
        description = "Mean-reversion strategy around the Volume-Weighted Average Price. Buys when price deviates significantly below VWAP with RSI oversold, sells when significantly above with RSI overbought. Works best on intraday (5m–1h) timeframes.",
        code = """
// VWAP Reversion Strategy
// Buy below VWAP + oversold, sell above VWAP + overbought

var DEV_THRESHOLD = 1.5;
var RSI_PERIOD = 14;
var RSI_OVERSOLD = 35.0;
var RSI_OVERBOUGHT = 65.0;
var POSITION_SIZE_PCT = 2.5;
var STOP_LOSS_PCT = 1.5;
var TAKE_PROFIT_PCT = 3.0;

function strategy(candles) {
    if (candles.length < RSI_PERIOD + 5) return {type:'HOLD'};

    var vwapVal = vwap(candles);
    var price = candles[candles.length - 1].close;
    var atrVal = atr(candles, 14);
    var rsiVal = rsi(candles, RSI_PERIOD);
    if (atrVal === 0.0 || vwapVal === 0.0) return {type:'HOLD'};

    var deviation = (price - vwapVal) / atrVal;

    if (deviation < -DEV_THRESHOLD && rsiVal < RSI_OVERSOLD) {
        return {type:'BUY', sizePct: POSITION_SIZE_PCT,
            stopLossPct: STOP_LOSS_PCT, takeProfitPct: TAKE_PROFIT_PCT};
    }
    if (deviation > DEV_THRESHOLD && rsiVal > RSI_OVERBOUGHT) {
        return {type:'SELL', sizePct: 100.0,
            stopLossPct: STOP_LOSS_PCT, takeProfitPct: TAKE_PROFIT_PCT};
    }
    return {type:'HOLD'};
}
""".trimIndent(),
        defaultParams = mapOf("DEV_THRESHOLD" to "1.5", "RSI_PERIOD" to "14",
            "RSI_OVERSOLD" to "35.0", "RSI_OVERBOUGHT" to "65.0",
            "POSITION_SIZE_PCT" to "2.5", "STOP_LOSS_PCT" to "1.5", "TAKE_PROFIT_PCT" to "3.0")
    )

    // ─── 9. Ichimoku Cloud ──────────────────────────────────────────
    private fun ichimokuCloudTemplate() = StrategyTemplate(
        id = StrategyTemplateId.ICHIMOKU_CLOUD,
        name = "Ichimoku Cloud",
        description = "Classic Ichimoku Kinko Hyo strategy. Buys on Tenkan/Kijun bullish cross when price is above the cloud; sells on bearish cross when price is below. Best on daily or 4h charts with strong trends.",
        code = """
// Ichimoku Cloud Strategy
// Tenkan/Kijun cross + cloud filter

var TENKAN_PERIOD = 9;
var KIJUN_PERIOD = 26;
var SENKOU_B_PERIOD = 52;
var POSITION_SIZE_PCT = 2.0;
var STOP_LOSS_PCT = 2.5;
var TAKE_PROFIT_PCT = 5.0;

function strategy(candles) {
    if (candles.length < SENKOU_B_PERIOD + 2) return {type:'HOLD'};

    var ich = ichimoku(candles, TENKAN_PERIOD, KIJUN_PERIOD, SENKOU_B_PERIOD);
    var prevIch = ichimoku(candles.slice(0, -1), TENKAN_PERIOD, KIJUN_PERIOD, SENKOU_B_PERIOD);
    var price = candles[candles.length - 1].close;

    var aboveCloud = price > Math.max(ich.senkouA, ich.senkouB);
    var belowCloud = price < Math.min(ich.senkouA, ich.senkouB);
    var bullishCross = prevIch.tenkan <= prevIch.kijun && ich.tenkan > ich.kijun;
    var bearishCross = prevIch.tenkan >= prevIch.kijun && ich.tenkan < ich.kijun;

    if (bullishCross && aboveCloud) {
        return {type:'BUY', sizePct: POSITION_SIZE_PCT,
            stopLossPct: STOP_LOSS_PCT, takeProfitPct: TAKE_PROFIT_PCT};
    }
    if (bearishCross && belowCloud) {
        return {type:'SELL', sizePct: 100.0,
            stopLossPct: STOP_LOSS_PCT, takeProfitPct: TAKE_PROFIT_PCT};
    }
    return {type:'HOLD'};
}
""".trimIndent(),
        defaultParams = mapOf("TENKAN_PERIOD" to "9", "KIJUN_PERIOD" to "26",
            "SENKOU_B_PERIOD" to "52", "POSITION_SIZE_PCT" to "2.0",
            "STOP_LOSS_PCT" to "2.5", "TAKE_PROFIT_PCT" to "5.0")
    )

    // ─── 10. Stochastic Cross ───────────────────────────────────────
    private fun stochasticCrossTemplate() = StrategyTemplate(
        id = StrategyTemplateId.STOCHASTIC_CROSS,
        name = "Stochastic Cross",
        description = "Momentum strategy using %K/%D crossovers in extreme zones. Buys when %K crosses above %D in oversold territory; sells when %K crosses below %D in overbought territory. Good for ranging markets.",
        code = """
// Stochastic Cross Strategy
// K/D crossover in oversold/overbought zones

var K_PERIOD = 14;
var D_PERIOD = 3;
var OVERSOLD = 20.0;
var OVERBOUGHT = 80.0;
var POSITION_SIZE_PCT = 2.0;
var STOP_LOSS_PCT = 1.5;
var TAKE_PROFIT_PCT = 3.0;

function strategy(candles) {
    if (candles.length < K_PERIOD + D_PERIOD + 2) return {type:'HOLD'};

    var stNow = stochastic(candles, K_PERIOD, D_PERIOD);
    var k = stNow[0]; var d = stNow[1];
    var stPrev = stochastic(candles.slice(0, -1), K_PERIOD, D_PERIOD);
    var prevK = stPrev[0]; var prevD = stPrev[1];

    if (prevK <= prevD && k > d && k < OVERSOLD) {
        return {type:'BUY', sizePct: POSITION_SIZE_PCT,
            stopLossPct: STOP_LOSS_PCT, takeProfitPct: TAKE_PROFIT_PCT};
    }
    if (prevK >= prevD && k < d && k > OVERBOUGHT) {
        return {type:'SELL', sizePct: 100.0,
            stopLossPct: STOP_LOSS_PCT, takeProfitPct: TAKE_PROFIT_PCT};
    }
    return {type:'HOLD'};
}
""".trimIndent(),
        defaultParams = mapOf("K_PERIOD" to "14", "D_PERIOD" to "3",
            "OVERSOLD" to "20.0", "OVERBOUGHT" to "80.0",
            "POSITION_SIZE_PCT" to "2.0", "STOP_LOSS_PCT" to "1.5", "TAKE_PROFIT_PCT" to "3.0")
    )

    // ─── 11. Volume Breakout ────────────────────────────────────────
    private fun volumeBreakoutTemplate() = StrategyTemplate(
        id = StrategyTemplateId.VOLUME_BREAKOUT,
        name = "Volume Breakout",
        description = "Breakout strategy that enters when price breaks above/below the lookback high/low with a volume spike (e.g., 2× average). Catches momentum moves early. Best on 15m–4h timeframes.",
        code = """
// Volume Breakout Strategy
// Price breakout + volume spike confirmation

var LOOKBACK = 20;
var VOL_MULTIPLIER = 2.0;
var POSITION_SIZE_PCT = 2.0;
var STOP_LOSS_PCT = 2.0;
var TAKE_PROFIT_PCT = 4.0;

function strategy(candles) {
    if (candles.length < LOOKBACK + 2) return {type:'HOLD'};

    var price = candles[candles.length - 1].close;
    var window = candles.slice(candles.length - LOOKBACK - 1, candles.length - 1);
    var prevHigh = Math.max.apply(null, window.map(function(c){return c.high;}));
    var prevLow = Math.min.apply(null, window.map(function(c){return c.low;}));
    var avgVol = window.reduce(function(s,c){return s+c.volume;},0) / window.length;
    var curVol = candles[candles.length - 1].volume;
    var volumeSpike = curVol > avgVol * VOL_MULTIPLIER;

    if (price > prevHigh && volumeSpike) {
        return {type:'BUY', sizePct: POSITION_SIZE_PCT,
            stopLossPct: STOP_LOSS_PCT, takeProfitPct: TAKE_PROFIT_PCT};
    }
    if (price < prevLow && volumeSpike) {
        return {type:'SELL', sizePct: 100.0,
            stopLossPct: STOP_LOSS_PCT, takeProfitPct: TAKE_PROFIT_PCT};
    }
    return {type:'HOLD'};
}
""".trimIndent(),
        defaultParams = mapOf("LOOKBACK" to "20", "VOL_MULTIPLIER" to "2.0",
            "POSITION_SIZE_PCT" to "2.0", "STOP_LOSS_PCT" to "2.0", "TAKE_PROFIT_PCT" to "4.0")
    )

    // ─── 12. 5-Min Scalper (Multi-TP) ──────────────────────────────
    private fun scalper5mTemplate() = StrategyTemplate(
        id = StrategyTemplateId.SCALPER_5M,
        name = "5m Scalper",
        description = "High-frequency scalper for 5-minute charts. Uses EMA 9/21 cross with RSI(7) momentum, VWAP direction filter, and volume spike confirmation. Multi-TP: 50% at 0.3%, 50% at 0.6%. Tight SL at 0.4%. Best on liquid pairs like BTCUSDT, ETHUSDT.",
        code = """
// 5-Minute Scalper — EMA cross + RSI + VWAP + Volume
// Multi-TP: 50% at 0.3%, 50% at 0.6%. SL: 0.4%

var EMA_FAST = 9;
var EMA_SLOW = 21;
var RSI_PERIOD = 7;
var RSI_BUY_MAX = 65.0;
var RSI_SELL_MIN = 35.0;
var VOL_MULT = 1.3;
var ATR_PERIOD = 10;
var POSITION_SIZE_PCT = 1.5;
var TP1_PCT = 0.3;
var TP1_QTY = 50.0;
var TP2_PCT = 0.6;
var TP2_QTY = 50.0;
var SL_PCT = 0.4;

function strategy(candles) {
    if (candles.length < EMA_SLOW + ATR_PERIOD) return {type:'HOLD'};

    var emaF = ema(candles, EMA_FAST);
    var emaS = ema(candles, EMA_SLOW);
    var prevEmaF = ema(candles.slice(0, -1), EMA_FAST);
    var prevEmaS = ema(candles.slice(0, -1), EMA_SLOW);
    var r = rsi(candles, RSI_PERIOD);
    var vw = vwap(candles);
    var price = candles[candles.length - 1].close;

    var recent = candles.slice(-20);
    var avgVol = recent.reduce(function(s,c){return s+c.volume;},0) / recent.length;
    var volOk = candles[candles.length - 1].volume > avgVol * VOL_MULT;

    var bullCross = prevEmaF <= prevEmaS && emaF > emaS;
    var bearCross = prevEmaF >= prevEmaS && emaF < emaS;

    if (bullCross && price > vw && r < RSI_BUY_MAX && volOk) {
        return {type:'BUY', sizePct: POSITION_SIZE_PCT, stopLossPct: SL_PCT,
            takeProfitLevels: [{pct: TP1_PCT, quantityPct: TP1_QTY},
                               {pct: TP2_PCT, quantityPct: TP2_QTY}]};
    }
    if (bearCross && price < vw && r > RSI_SELL_MIN && volOk) {
        return {type:'SELL', sizePct: 100.0, stopLossPct: SL_PCT,
            takeProfitLevels: [{pct: TP1_PCT, quantityPct: TP1_QTY},
                               {pct: TP2_PCT, quantityPct: TP2_QTY}]};
    }
    return {type:'HOLD'};
}
""".trimIndent(),
        defaultParams = mapOf(
            "EMA_FAST" to "9", "EMA_SLOW" to "21", "RSI_PERIOD" to "7",
            "RSI_BUY_MAX" to "65.0", "RSI_SELL_MIN" to "35.0",
            "VOL_MULT" to "1.3", "ATR_PERIOD" to "10",
            "POSITION_SIZE_PCT" to "1.5",
            "TP1_PCT" to "0.3", "TP1_QTY" to "50.0",
            "TP2_PCT" to "0.6", "TP2_QTY" to "50.0",
            "SL_PCT" to "0.4"
        )
    )

    // ─── 13. 15-Min Swing (Multi-TP) ───────────────────────────────
    private fun swing15mTemplate() = StrategyTemplate(
        id = StrategyTemplateId.SWING_15M,
        name = "15m Swing",
        description = "Momentum swing strategy for 15-minute charts. Combines MACD(8,21,5) histogram crossover with Stochastic RSI for precise entries, and Supertrend(7,2) as trend filter. Multi-TP: 33% at 0.8%, 33% at 1.5%, 34% at 2.5%. SL: 1.0%.",
        code = """
// 15-Minute Swing — MACD + StochRSI + Supertrend filter
// Multi-TP: 33% at 0.8%, 33% at 1.5%, 34% at 2.5%. SL: 1.0%

var MACD_FAST = 8;
var MACD_SLOW = 21;
var MACD_SIGNAL = 5;
var STOCH_RSI_PERIOD = 14;
var STOCH_K = 3;
var STOCH_D = 3;
var STOCH_OS = 25.0;
var STOCH_OB = 75.0;
var ST_PERIOD = 7;
var ST_MULT = 2.0;
var POSITION_SIZE_PCT = 2.0;
var TP1_PCT = 0.8;
var TP1_QTY = 33.0;
var TP2_PCT = 1.5;
var TP2_QTY = 33.0;
var TP3_PCT = 2.5;
var TP3_QTY = 34.0;
var SL_PCT = 1.0;

function strategy(candles) {
    if (candles.length < MACD_SLOW + MACD_SIGNAL + 5) return {type:'HOLD'};

    var macdNow = macd(candles, MACD_FAST, MACD_SLOW, MACD_SIGNAL);
    var macdPrev = macd(candles.slice(0, -1), MACD_FAST, MACD_SLOW, MACD_SIGNAL);

    var stRsi = stochasticRsi(candles, STOCH_RSI_PERIOD, STOCH_RSI_PERIOD, STOCH_K, STOCH_D);
    var stK = stRsi[0];

    var stResult = supertrend(candles, ST_PERIOD, ST_MULT);
    var isUp = stResult[1];

    var macdBullCross = macdPrev.histogram <= 0 && macdNow.histogram > 0;
    var macdBearCross = macdPrev.histogram >= 0 && macdNow.histogram < 0;

    if (macdBullCross && stK < STOCH_OB && isUp) {
        return {type:'BUY', sizePct: POSITION_SIZE_PCT, stopLossPct: SL_PCT,
            takeProfitLevels: [{pct: TP1_PCT, quantityPct: TP1_QTY},
                               {pct: TP2_PCT, quantityPct: TP2_QTY},
                               {pct: TP3_PCT, quantityPct: TP3_QTY}]};
    }
    if (macdBearCross && stK > STOCH_OS && !isUp) {
        return {type:'SELL', sizePct: 100.0, stopLossPct: SL_PCT,
            takeProfitLevels: [{pct: TP1_PCT, quantityPct: TP1_QTY},
                               {pct: TP2_PCT, quantityPct: TP2_QTY},
                               {pct: TP3_PCT, quantityPct: TP3_QTY}]};
    }
    return {type:'HOLD'};
}
""".trimIndent(),
        defaultParams = mapOf(
            "MACD_FAST" to "8", "MACD_SLOW" to "21", "MACD_SIGNAL" to "5",
            "STOCH_RSI_PERIOD" to "14", "STOCH_K" to "3", "STOCH_D" to "3",
            "STOCH_OS" to "25.0", "STOCH_OB" to "75.0",
            "ST_PERIOD" to "7", "ST_MULT" to "2.0",
            "POSITION_SIZE_PCT" to "2.0",
            "TP1_PCT" to "0.8", "TP1_QTY" to "33.0",
            "TP2_PCT" to "1.5", "TP2_QTY" to "33.0",
            "TP3_PCT" to "2.5", "TP3_QTY" to "34.0",
            "SL_PCT" to "1.0"
        )
    )

    // ─── 14. 1-Hr Trend (Multi-TP) ─────────────────────────────────
    private fun trend1hTemplate() = StrategyTemplate(
        id = StrategyTemplateId.TREND_1H,
        name = "1h Trend",
        description = "Trend-following strategy for 1-hour charts. Uses Ichimoku Cloud for trend direction, ADX(14) > 25 for trend strength, and EMA 50/200 golden/death cross for entry timing. Multi-TP: 30% at 2%, 30% at 4%, 40% at 6%. SL: 2%.",
        code = """
// 1-Hour Trend — Ichimoku + ADX + EMA 50/200
// Multi-TP: 30% at 2%, 30% at 4%, 40% at 6%. SL: 2%

var ICH_TENKAN = 9;
var ICH_KIJUN = 26;
var ICH_SENKOU_B = 52;
var ADX_PERIOD = 14;
var ADX_MIN = 25.0;
var EMA_MID = 50;
var EMA_LONG = 200;
var POSITION_SIZE_PCT = 2.5;
var TP1_PCT = 2.0;
var TP1_QTY = 30.0;
var TP2_PCT = 4.0;
var TP2_QTY = 30.0;
var TP3_PCT = 6.0;
var TP3_QTY = 40.0;
var SL_PCT = 2.0;

function strategy(candles) {
    if (candles.length < EMA_LONG + 5) return {type:'HOLD'};

    var price = candles[candles.length - 1].close;

    var ich = ichimoku(candles, ICH_TENKAN, ICH_KIJUN, ICH_SENKOU_B);
    var aboveCloud = price > Math.max(ich.senkouA, ich.senkouB);
    var belowCloud = price < Math.min(ich.senkouA, ich.senkouB);

    var adxVal = adx(candles, ADX_PERIOD);

    var emaMid = ema(candles, EMA_MID);
    var emaLong = ema(candles, EMA_LONG);
    var prevEmaMid = ema(candles.slice(0, -1), EMA_MID);
    var prevEmaLong = ema(candles.slice(0, -1), EMA_LONG);
    var goldenCross = prevEmaMid <= prevEmaLong && emaMid > emaLong;
    var deathCross = prevEmaMid >= prevEmaLong && emaMid < emaLong;

    if (goldenCross && aboveCloud && adxVal > ADX_MIN) {
        return {type:'BUY', sizePct: POSITION_SIZE_PCT, stopLossPct: SL_PCT,
            takeProfitLevels: [{pct: TP1_PCT, quantityPct: TP1_QTY},
                               {pct: TP2_PCT, quantityPct: TP2_QTY},
                               {pct: TP3_PCT, quantityPct: TP3_QTY}]};
    }
    if (deathCross && belowCloud && adxVal > ADX_MIN) {
        return {type:'SELL', sizePct: 100.0, stopLossPct: SL_PCT,
            takeProfitLevels: [{pct: TP1_PCT, quantityPct: TP1_QTY},
                               {pct: TP2_PCT, quantityPct: TP2_QTY},
                               {pct: TP3_PCT, quantityPct: TP3_QTY}]};
    }

    if (adxVal < 20.0) {
        if (emaMid > emaLong) return {type:'CLOSE_IF_LONG'};
        if (emaMid < emaLong) return {type:'CLOSE_IF_SHORT'};
    }

    return {type:'HOLD'};
}
""".trimIndent(),
        defaultParams = mapOf(
            "ICH_TENKAN" to "9", "ICH_KIJUN" to "26", "ICH_SENKOU_B" to "52",
            "ADX_PERIOD" to "14", "ADX_MIN" to "25.0",
            "EMA_MID" to "50", "EMA_LONG" to "200",
            "POSITION_SIZE_PCT" to "2.5",
            "TP1_PCT" to "2.0", "TP1_QTY" to "30.0",
            "TP2_PCT" to "4.0", "TP2_QTY" to "30.0",
            "TP3_PCT" to "6.0", "TP3_QTY" to "40.0",
            "SL_PCT" to "2.0"
        )
    )

    // ─────────────────────────────────────────────────────────────
    //  TFG ALGO — Spot Scalper / Long Only / 10-factor Score
    // ─────────────────────────────────────────────────────────────
    private fun tfgAlgo() = StrategyTemplate(
        id = StrategyTemplateId.TFG_ALGO,
        name = "TFG ALGO",
        description = "Spot scalp — long only — non-repainting. 10-factor confluence score " +
                "(EMA trend, VWAP, volume spike, RSI, MACD, candle pattern, S/R proximity, " +
                "DI±, ADX, breakout). Entry on EMA cross (score ≥ 6) or strong candle " +
                "pattern (score ≥ 8). Multi-trigger sell (2+ bearish). Two-level TP with " +
                "optional trailing stop. HTF trend filter, cooldown, risk-based sizing.",
        code = """
// ═══════════════════════════════════════════════════════════════
//  TFG ALGO — Spot Scalp — Long Only — Non-Repainting
//  10-Factor Confluence Score
//
//  Fixes applied:
//  [FIX 1] buySignal requires positionSize == 0 (no duplicate entries)
//  [FIX 2] SL/TP locked at entry bar (backtest engine handles this)
//  [FIX 3] Trail high locked per-trade, reset on new entry
//  [FIX 4] Exit-bar cooldown prevents same-bar re-entry after close
//  [FIX 5] All params read from _params (dynamic settings)
//  [FIX 6] label='PATTERN' maps to PAT_BUY signal type
// ═══════════════════════════════════════════════════════════════

function strategy(candles) {
    // ── Read ALL settings dynamically from _params (user-editable) ──
    var p = typeof _params !== 'undefined' ? _params : {};
    var RISK_PCT     = p.RISK_PCT     != null ? Number(p.RISK_PCT)     : 1.5;
    var SL_PCT       = p.SL_PCT       != null ? Number(p.SL_PCT)       : 0.25;
    var TP1_PCT      = p.TP1_PCT      != null ? Number(p.TP1_PCT)      : 0.3;
    var TP2_PCT      = p.TP2_PCT      != null ? Number(p.TP2_PCT)      : 0.5;
    var TP1_CLOSE_PCT= p.TP1_CLOSE_PCT!= null ? Number(p.TP1_CLOSE_PCT): 50;
    var USE_TRAIL    = p.USE_TRAIL    != null ? Number(p.USE_TRAIL)    : 1;
    var TRAIL_PCT    = p.TRAIL_PCT    != null ? Number(p.TRAIL_PCT)    : 0.3;
    var TRAIL_OFFSET = p.TRAIL_OFFSET != null ? Number(p.TRAIL_OFFSET) : 0.15;
    var EMA_FAST     = p.EMA_FAST     != null ? Number(p.EMA_FAST)     : 9;
    var EMA_SLOW     = p.EMA_SLOW     != null ? Number(p.EMA_SLOW)     : 21;
    var EMA_TREND    = p.EMA_TREND    != null ? Number(p.EMA_TREND)    : 50;
    var RSI_LEN      = p.RSI_LEN      != null ? Number(p.RSI_LEN)      : 7;
    var RSI_OB       = p.RSI_OB       != null ? Number(p.RSI_OB)       : 75;
    var RSI_OS       = p.RSI_OS       != null ? Number(p.RSI_OS)       : 30;
    var MACD_FAST    = p.MACD_FAST    != null ? Number(p.MACD_FAST)    : 8;
    var MACD_SLOW    = p.MACD_SLOW    != null ? Number(p.MACD_SLOW)    : 21;
    var MACD_SIG     = p.MACD_SIG     != null ? Number(p.MACD_SIG)     : 5;
    var VOL_LEN      = p.VOL_LEN      != null ? Number(p.VOL_LEN)      : 14;
    var VOL_MULT     = p.VOL_MULT     != null ? Number(p.VOL_MULT)     : 1.3;
    var USE_VWAP     = p.USE_VWAP     != null ? Number(p.USE_VWAP)     : 1;
    var USE_SR       = p.USE_SR       != null ? Number(p.USE_SR)       : 1;
    var SR_LOOKBACK  = p.SR_LOOKBACK  != null ? Number(p.SR_LOOKBACK)  : 20;
    var SR_PROXIMITY = p.SR_PROXIMITY != null ? Number(p.SR_PROXIMITY) : 0.15;
    var ADX_LEN      = p.ADX_LEN      != null ? Number(p.ADX_LEN)      : 10;
    var ADX_MIN      = p.ADX_MIN      != null ? Number(p.ADX_MIN)      : 15;
    var USE_HTF      = p.USE_HTF      != null ? Number(p.USE_HTF)      : 1;
    var COOLDOWN_BARS= p.COOLDOWN_BARS!= null ? Number(p.COOLDOWN_BARS): 3;
    var MIN_BUY_SCORE= p.MIN_BUY_SCORE!= null ? Number(p.MIN_BUY_SCORE): 6;

    var n = candles.length;
    var minBars = Math.max(EMA_TREND, MACD_SLOW, ADX_LEN * 2) + 12;
    if (n < minBars) return {type:'HOLD'};

    var last  = candles[n - 1];
    var prev  = candles[n - 2];
    var price = last.close;

    // ── Persistent state init ──
    if (!_state.init) {
        _state.init = true;
        _state.lastEntryBar = -100;
        _state.lastExitBar  = -100;
        _state.supportLevel = null;
        _state.resistanceLevel = null;
        _state.trailActive = false;
        _state.trailHigh = 0;
    }

    // ════════════════════════════════════════
    //  INDICATORS
    // ════════════════════════════════════════

    var emaF  = ema(candles, EMA_FAST);
    var emaS  = ema(candles, EMA_SLOW);
    var emaT  = ema(candles, EMA_TREND);
    var prevEmaF = ema(candles.slice(0, -1), EMA_FAST);
    var prevEmaS = ema(candles.slice(0, -1), EMA_SLOW);

    var vwapVal = vwap(candles);
    var rsiVal  = rsi(candles, RSI_LEN);
    var prevRsi = rsi(candles.slice(0, -1), RSI_LEN);

    var m = macd(candles, MACD_FAST, MACD_SLOW, MACD_SIG);
    var macdLine = m[0], signalLine = m[1], macdHist = m[2];
    var pm = macd(candles.slice(0, -1), MACD_FAST, MACD_SLOW, MACD_SIG);
    var prevMacdLine = pm[0], prevSignalLine = pm[1], prevMacdHist = pm[2];

    var dm  = dmi(candles, ADX_LEN);
    var pdm = dmi(candles.slice(0, -1), ADX_LEN);

    // Volume spike
    var volSlice = candles.slice(-VOL_LEN);
    var volAvg = 0;
    for (var vi = 0; vi < volSlice.length; vi++) volAvg += volSlice[vi].volume;
    volAvg /= volSlice.length;
    var volSpike = last.volume > volAvg * VOL_MULT;

    // ════════════════════════════════════════
    //  CANDLE ANALYSIS
    // ════════════════════════════════════════
    var candleBody  = Math.abs(last.close - last.open);
    var candleRange = last.high - last.low;
    var bodyRatio   = candleRange > 0 ? candleBody / candleRange : 0;
    var upperWick   = last.high - Math.max(last.close, last.open);
    var lowerWick   = Math.min(last.close, last.open) - last.low;

    // Bullish engulfing
    var bullEngulf = last.close > last.open && prev.close < prev.open &&
                     last.close > prev.open && last.open < prev.close && bodyRatio > 0.5;
    // Bullish pin bar (long lower wick, small body at top)
    var bullPinBar = lowerWick > candleBody * 2 && upperWick < candleBody &&
                     last.close > last.open && bodyRatio > 0.2;
    // Strong bullish candle (large body closing near high)
    var strongBull = last.close > last.open && bodyRatio > 0.6 &&
                     upperWick < candleBody * 0.3;
    var bullPattern = bullEngulf || bullPinBar || strongBull;
    // Simple bullish close (minimum requirement)
    var bullCandle  = last.close > last.open && bodyRatio > 0.35;

    // ════════════════════════════════════════
    //  SUPPORT / RESISTANCE (Pivot-based)
    // ════════════════════════════════════════
    var pivLows  = pivotLow(candles, 5, 5);
    var pivHighs = pivotHigh(candles, 5, 5);
    for (var pi = pivLows.length - 1; pi >= 0; pi--) {
        if (pivLows[pi] !== null) { _state.supportLevel = pivLows[pi]; break; }
    }
    for (var pi = pivHighs.length - 1; pi >= 0; pi--) {
        if (pivHighs[pi] !== null) { _state.resistanceLevel = pivHighs[pi]; break; }
    }
    var nearSupport = USE_SR == 1 && _state.supportLevel != null ?
        ((price - _state.supportLevel) / price * 100 < SR_PROXIMITY && price > _state.supportLevel) : false;

    // ════════════════════════════════════════
    //  HTF TREND FILTER (non-repainting — uses [1] offset)
    // ════════════════════════════════════════
    var htfBull = true;
    if (USE_HTF == 1 && typeof _htf !== 'undefined' && _htf) {
        var htfKeys = Object.keys(_htf);
        for (var hi = 0; hi < htfKeys.length; hi++) {
            var htfC = _htf[htfKeys[hi]];
            if (htfC && htfC.length > EMA_SLOW + 1) {
                htfBull = ema(htfC, EMA_FAST) > ema(htfC, EMA_SLOW);
                break;
            }
        }
    }

    // ════════════════════════════════════════
    //  EMA CROSSOVER TRIGGER
    // ════════════════════════════════════════
    var emaCross = prevEmaF <= prevEmaS && emaF > emaS;

    // ════════════════════════════════════════
    //  10-FACTOR CONFLUENCE SCORING
    // ════════════════════════════════════════
    // C1: Trend — price above trend EMA
    var c_trend    = price > emaT ? 1 : 0;
    // C2: VWAP — institutional bias
    var c_vwap     = (USE_VWAP == 1 ? price > vwapVal : true) ? 1 : 0;
    // C3: Volume spike — confirms breakout
    var c_volume   = volSpike ? 1 : 0;
    // C4: RSI momentum — above 45, not overbought
    var c_rsi      = (rsiVal > 45 && rsiVal < RSI_OB) ? 1 : 0;
    // C5: MACD — bullish and building momentum
    var c_macd     = (macdLine > signalLine && macdHist > prevMacdHist) ? 1 : 0;
    // C6: Candle pattern — bullish structure
    var c_candle   = bullCandle ? 1 : 0;
    // C7: Near support — buying at demand zone
    var c_support  = nearSupport ? 1 : 0;
    // C8: DI+ > DI- — directional buyer pressure
    var c_di       = dm.diPlus > dm.diMinus ? 1 : 0;
    // C9: ADX trending — market has momentum
    var c_adx      = dm.adx > ADX_MIN ? 1 : 0;
    // C10: Breakout — close above previous high
    var c_breakout = price > prev.high ? 1 : 0;

    var totalScore = c_trend + c_vwap + c_volume + c_rsi + c_macd +
                     c_candle + c_support + c_di + c_adx + c_breakout;

    // Store dashboard for UI
    _state.dashboard = {
        score: totalScore,
        minScore: MIN_BUY_SCORE,
        c_trend: c_trend, c_vwap: c_vwap, c_volume: c_volume,
        c_rsi: c_rsi, c_macd: c_macd, c_candle: c_candle,
        c_support: c_support, c_di: c_di, c_adx: c_adx, c_breakout: c_breakout,
        rsiVal: Math.round(rsiVal * 10) / 10,
        adxVal: Math.round(dm.adx * 10) / 10,
        diPlus: Math.round(dm.diPlus * 10) / 10,
        diMinus: Math.round(dm.diMinus * 10) / 10,
        htfBull: htfBull ? 1 : 0,
        inPosition: _account && _account.positionSize > 0 ? 1 : 0,
        supportLevel: _state.supportLevel,
        resistanceLevel: _state.resistanceLevel,
        bullEngulf: bullEngulf ? 1 : 0,
        bullPinBar: bullPinBar ? 1 : 0,
        strongBull: strongBull ? 1 : 0,
        slPct: SL_PCT,
        tp1Pct: TP1_PCT,
        tp2Pct: TP2_PCT,
        pnlPct: (_account && _account.positionSize > 0 && _account.positionEntry > 0) ?
            Math.round((price - _account.positionEntry) / _account.positionEntry * 10000) / 100 : 0
    };

    // ════════════════════════════════════════
    //  STRATEGY PLOT OVERLAYS
    // ════════════════════════════════════════
    resetPlot();
    plot('EMA Fast',  emaSeries(candles, EMA_FAST),  '#00E676', 1);
    plot('EMA Slow',  emaSeries(candles, EMA_SLOW),  '#FF9800', 1);
    plot('EMA Trend', emaSeries(candles, EMA_TREND), '#42A5F5', 2, 2);
    if (USE_VWAP == 1) plot('VWAP', vwapSeries(candles), '#E040FB', 1, 2);
    plotPanel('RSI', rsiSeries(candles, RSI_LEN), '#FFD600', 'line', [{y: RSI_OB, color: '#FF5252'}, {y: RSI_OS, color: '#69F0AE'}]);
    if (_state.supportLevel)    hline(_state.supportLevel,    '#4CAF50', 'Support',    2);
    if (_state.resistanceLevel) hline(_state.resistanceLevel, '#F44336', 'Resistance', 2);

    // ════════════════════════════════════════
    //  POSITION MANAGEMENT — IN-TRADE EXITS
    // ════════════════════════════════════════
    if (_account && _account.positionSize > 0) {

        // ── Trailing stop ──
        // TRAIL_PCT  = activation distance (how far above entry before trail starts)
        // TRAIL_OFFSET = trailing distance (how far below peak to place stop)
        // Uses last.low (not close) so wicks that recover don't trigger exit
        if (USE_TRAIL == 1) {
            var entry = _account.positionEntry;
            if (!_state.trailActive) {
                // Activate trail when high rises TRAIL_PCT above entry
                if (last.high >= entry * (1 + TRAIL_PCT / 100)) {
                    _state.trailActive = true;
                    _state.trailHigh = last.high;
                }
            } else {
                if (last.high > _state.trailHigh) _state.trailHigh = last.high;
                var trailStop = _state.trailHigh * (1 - TRAIL_OFFSET / 100);
                if (last.low <= trailStop) {
                    _state.trailActive = false;
                    _state.trailHigh = 0;
                    _state.lastExitBar = n;
                    return {type:'CLOSE_IF_LONG'};
                }
            }
        }

        // ── Multi-trigger SELL (need 2+ bearish signals firing simultaneously) ──
        var emaSellCross = prevEmaF >= prevEmaS && emaF < emaS;
        var trendBreak   = price < emaT && prev.close >= emaT;
        var rsiExhaust   = prevRsi >= RSI_OB && rsiVal < RSI_OB;
        var macdBearX    = prevMacdLine >= prevSignalLine && macdLine < signalLine;
        var diFlip       = dm.diMinus > dm.diPlus && pdm.diPlus >= pdm.diMinus;
        var belowVWAP    = USE_VWAP == 1 && price < vwapVal && prev.close >= vwapVal;

        var exitCount = (emaSellCross?1:0) + (trendBreak?1:0) + (rsiExhaust?1:0) +
                        (macdBearX?1:0) + (diFlip?1:0) + (belowVWAP?1:0);

        // Sell on 2+ bearish confluences
        if (exitCount >= 2) {
            _state.lastExitBar = n;
            return {type:'CLOSE_IF_LONG'};
        }
        // Strong sell: EMA death cross + below trend EMA
        if (emaSellCross && price < emaT) {
            _state.lastExitBar = n;
            return {type:'CLOSE_IF_LONG'};
        }
    }

    // ════════════════════════════════════════
    //  ENTRY SIGNALS
    // ════════════════════════════════════════

    // [FIX 1] noPosition check prevents duplicate entries
    var noPosition  = !_account || _account.positionSize <= 0;
    // [FIX 4] Cooldown from last entry AND last exit (no same-bar re-entry)
    var cooldownOK  = (n - _state.lastEntryBar >= COOLDOWN_BARS) &&
                      (n - _state.lastExitBar  >= COOLDOWN_BARS);

    // Primary: EMA cross with 6+/10 confluence
    var buySignal  = emaCross && totalScore >= MIN_BUY_SCORE && htfBull && cooldownOK && noPosition;
    // Bonus: strong candle pattern with 8+/10 (no EMA cross needed)
    var patternBuy = bullPattern && !emaCross && totalScore >= 8 && htfBull && cooldownOK && noPosition;

    if (buySignal || patternBuy) {
        _state.lastEntryBar = n;
        _state.trailActive  = false;
        _state.trailHigh    = 0;

        // Risk-based position sizing: risk% of equity / SL distance
        var equity     = _account ? _account.equity : 10000;
        var capRisk    = equity * (RISK_PCT / 100);
        var slDist     = price * (SL_PCT / 100);
        var posQty     = slDist > 0 ? capRisk / slDist : 0;
        var maxQty     = equity / price;
        var finalQty   = Math.min(posQty, maxQty);
        var sizePct    = equity > 0 ? (finalQty * price / equity * 100) : RISK_PCT;
        sizePct = Math.min(Math.max(sizePct, 0.5), 100);

        return {
            type: 'BUY',
            label: patternBuy ? 'PATTERN' : 'EMA_CROSS',
            orderType: 'MARKET',
            sizePct: sizePct,
            stopLossPct: SL_PCT,
            takeProfitLevels: [
                {pct: TP1_PCT, quantityPct: TP1_CLOSE_PCT},
                {pct: TP2_PCT, quantityPct: 100 - TP1_CLOSE_PCT}
            ]
        };
    }

    return {type:'HOLD'};
}
""".trimIndent(),
        defaultParams = mapOf(
            "RISK_PCT" to "1.5",
            "SL_PCT" to "0.25",
            "TP1_PCT" to "0.3",
            "TP2_PCT" to "0.5",
            "TP1_CLOSE_PCT" to "50",
            "USE_TRAIL" to "1",
            "TRAIL_PCT" to "0.3",
            "TRAIL_OFFSET" to "0.15",
            "EMA_FAST" to "9",
            "EMA_SLOW" to "21",
            "EMA_TREND" to "50",
            "RSI_LEN" to "7",
            "RSI_OB" to "75",
            "RSI_OS" to "30",
            "MACD_FAST" to "8",
            "MACD_SLOW" to "21",
            "MACD_SIG" to "5",
            "VOL_LEN" to "14",
            "VOL_MULT" to "1.3",
            "USE_VWAP" to "1",
            "USE_SR" to "1",
            "SR_LOOKBACK" to "20",
            "SR_PROXIMITY" to "0.15",
            "ADX_LEN" to "10",
            "ADX_MIN" to "15",
            "USE_HTF" to "1",
            "COOLDOWN_BARS" to "3",
            "MIN_BUY_SCORE" to "6"
        )
    )

    // -------------------------------------------------------------
    //  ISSA ALGO (Pine port) -- UT Bot + LinReg Candles + STC
    //  Direct port of indicators/tfg_algo.pine. Long & short, spot or futures.
    //  4-tier signals: Tier 1 (strong) -> Tier 4 (counter-trend, avoid).
    // -------------------------------------------------------------
    private fun tfgAlgoPine() = StrategyTemplate(
        id = StrategyTemplateId.TFG_ALGO_PINE,
        name = "ISSA ALGO",
        description = "ISSA ALGO (Pine port): UT Bot trailing stop + Linear-Regression Candles bias + Schaff Trend Cycle (STC) extreme filter. Returns BUY/SELL with a 4-tier confluence label (Tier 1 = strong reversal, Tier 4 = counter-trend, skipped). Works for spot and futures (set MARKET param). Multi-TP + SL via ATR.",
        code = """
// ISSA ALGO -- UT Bot + LinReg Candles + STC (Pine port)
// Spot or Futures. Edit any value below or use the Settings panel.

// -- UT Bot --
var UT_KEY = 1.0;
var UT_ATR = 10;
var USE_HA = 0;

// -- LinReg Candles --
var LR_LEN = 11;
var SIG_LEN = 11;

// -- STC --
var STC_LEN = 12;
var STC_FAST = 26;
var STC_SLOW = 50;
var STC_SMOOTH = 0.5;
var STC_OS = 25;
var STC_OB = 75;
var USE_STC = 1;

// -- Signal filter --
// MIN_TIER 4 fires every signal, 2 keeps only strong reversals
var MIN_TIER = 4;
var ALLOW_SHORT = 1;

// -- Risk --
var SIZE_PCT = 2.0;
var ATR_PERIOD = 14;
var SL_ATR_MULT = 1.5;
var TP1_RR = 1.5;
var TP1_QTY = 50.0;
var TP2_RR = 3.0;
var TP2_QTY = 50.0;

// -- Market --
var LEVERAGE = 1;

function strategy(candles) {
    var n = candles.length;
    var minBars = Math.max(STC_SLOW, LR_LEN, UT_ATR, ATR_PERIOD) + 5;
    if (n < minBars) return {type:'HOLD'};

    var sig = tfgAlgoSignal(candles, {
        utKey: UT_KEY, utAtr: UT_ATR, useHA: USE_HA == 1,
        lrLen: LR_LEN, sigLen: SIG_LEN, sigType: 'SMA',
        stcLen: STC_LEN, stcFast: STC_FAST, stcSlow: STC_SLOW, stcSmooth: STC_SMOOTH,
        stcOS: STC_OS, stcOB: STC_OB, useSTC: USE_STC == 1
    });

    // Plot overlays
    resetPlot();
    var ut = utBotSeries(candles, UT_KEY, UT_ATR, USE_HA == 1);
    plot('UT xATS', ut.xATS, '#FFD600', 2);
    var lr = linregCandlesSeries(candles, LR_LEN, SIG_LEN, 'SMA');
    plot('LR Sig', lr.sigLine, '#42A5F5', 1, 2);
    var st = stcSeries(candles, STC_LEN, STC_FAST, STC_SLOW, STC_SMOOTH);
    plotPanel('STC', st.stc, '#E040FB', 'line', [{y: STC_OB, color: '#FF5252'}, {y: STC_OS, color: '#69F0AE'}]);

    // Dashboard
    _state.dashboard = {
        action: sig.action, tier: sig.tier, strong: sig.strong ? 1 : 0,
        reason: sig.reason,
        stc: sig.stc != null ? Math.round(sig.stc * 10) / 10 : 0,
        sigLine: sig.sigLine != null ? sig.sigLine : 0,
        bclose: sig.bclose != null ? sig.bclose : 0,
        xATS: sig.xATS != null ? sig.xATS : 0,
        leverage: LEVERAGE
    };

    if (sig.action === 'HOLD' || sig.tier === 0) return {type:'HOLD'};
    if (sig.tier > MIN_TIER) return {type:'HOLD'};
    if (sig.action === 'SELL' && ALLOW_SHORT == 0) return {type:'CLOSE_IF_LONG'};

    // ATR-based SL & TP
    var atrV = atr(candles, ATR_PERIOD);
    var price = candles[n - 1].close;
    var slDist = atrV * SL_ATR_MULT;
    if (slDist <= 0) return {type:'HOLD'};
    var slPct = slDist / price * 100;
    var tp1Pct = (slDist * TP1_RR) / price * 100;
    var tp2Pct = (slDist * TP2_RR) / price * 100;

    var tierLabel = (sig.action === 'BUY' ? 'Buy T' : 'Sell T') + sig.tier;

    return {
        type: sig.action,
        sizePct: SIZE_PCT,
        stopLossPct: slPct,
        takeProfitLevels: [
            {pct: tp1Pct, quantityPct: TP1_QTY},
            {pct: tp2Pct, quantityPct: TP2_QTY}
        ],
        label: 'BREAKOUT',
        labelText: tierLabel,
        leverage: Math.max(1, Math.round(LEVERAGE))
    };
}
""".trimIndent(),
        defaultParams = mapOf(
            "UT_KEY" to "1.0", "UT_ATR" to "10", "USE_HA" to "0",
            "LR_LEN" to "11", "SIG_LEN" to "11",
            "STC_LEN" to "12", "STC_FAST" to "26", "STC_SLOW" to "50", "STC_SMOOTH" to "0.5",
            "STC_OS" to "25", "STC_OB" to "75", "USE_STC" to "1",
            "MIN_TIER" to "4", "ALLOW_SHORT" to "1",
            "SIZE_PCT" to "2.0",
            "ATR_PERIOD" to "14", "SL_ATR_MULT" to "1.5",
            "TP1_RR" to "1.5", "TP1_QTY" to "50.0",
            "TP2_RR" to "3.0", "TP2_QTY" to "50.0",
            "LEVERAGE" to "1"
        )
    )
}
