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
        trend1hTemplate()
    )

    fun getById(id: StrategyTemplateId): StrategyTemplate = getAll().first { it.id == id }

    // ─── 0. GainzAlgo V2 ───────────────────────────────────────────
    private fun gainzAlgo() = StrategyTemplate(
        id = StrategyTemplateId.GAINZ_ALGO,
        name = "GainzAlgo V2",
        description = "Engulfing-candle reversal strategy with candle stability filter, RSI confirmation, and ATR-based TP/SL. Detects bullish/bearish engulfing patterns, confirms with candle body > 50% of true range, filters with RSI(14), and uses 1:2 risk-reward ratio.",
        code = """
// GainzAlgo V2 Strategy
// Engulfing pattern + candle stability + RSI filter + ATR TP/SL

var CANDLE_STABILITY_INDEX = 0.5;
var RSI_PERIOD = 14;
var RSI_UPPER = 70.0;
var RSI_LOWER = 30.0;
var CANDLE_DELTA_LENGTH = 4;
var TP_SL_MULTIPLIER = 1.0;
var RISK_REWARD_RATIO = 2.0;
var POSITION_SIZE_PCT = 2.0;

function strategy(candles) {
    if (candles.length < 15) return {type:'HOLD'};

    var last = candles[candles.length - 1];
    var prev = candles[candles.length - 2];
    var deltaRef = candles[candles.length - CANDLE_DELTA_LENGTH - 1];

    var tr = Math.max(last.high - last.low,
                      Math.abs(last.high - prev.close),
                      Math.abs(last.low - prev.close));

    var candleBody = Math.abs(last.close - last.open);
    var stable = tr > 0.0 && candleBody / tr > CANDLE_STABILITY_INDEX;

    var rsiVal = rsi(candles, RSI_PERIOD);

    var atrVal = atr(candles, RSI_PERIOD);
    var dist = atrVal * TP_SL_MULTIPLIER;
    var slPct = dist / last.close * 100;
    var tpPct = slPct * RISK_REWARD_RATIO;

    var bullishEngulfing = prev.close < prev.open
                        && last.close > last.open
                        && last.close > prev.open;
    var priceDip = last.close < deltaRef.close;

    if (bullishEngulfing && stable && rsiVal < RSI_UPPER && priceDip) {
        return {type:'BUY', sizePct: POSITION_SIZE_PCT,
            stopLossPct: slPct, takeProfitPct: tpPct};
    }

    var bearishEngulfing = prev.close > prev.open
                        && last.close < last.open
                        && last.close < prev.open;
    var priceRise = last.close > deltaRef.close;

    if (bearishEngulfing && stable && rsiVal > RSI_LOWER && priceRise) {
        return {type:'SELL', sizePct: 100.0,
            stopLossPct: slPct, takeProfitPct: tpPct};
    }

    return {type:'HOLD'};
}
""".trimIndent(),
        defaultParams = mapOf(
            "CANDLE_STABILITY_INDEX" to "0.5",
            "RSI_UPPER" to "70.0",
            "RSI_LOWER" to "30.0",
            "CANDLE_DELTA_LENGTH" to "4",
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
}
