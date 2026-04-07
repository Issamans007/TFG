# TFG Script Engine — Indicator Library Reference

Complete reference for all built-in JavaScript indicator functions available in user scripts.
Functions are injected automatically — no imports needed. Just call them inside `strategy(_candles)`.

---

## Table of Contents

1. [Moving Averages (16)](#1-moving-averages)
2. [Oscillators & Momentum (22)](#2-oscillators--momentum)
3. [Volatility & Bands (14)](#3-volatility--bands)
4. [Volume Indicators (12)](#4-volume-indicators)
5. [Trend & Directional (10)](#5-trend--directional)
6. [Helpers & Utilities (18)](#6-helpers--utilities)
7. [Multi-Timeframe](#7-multi-timeframe)
8. [Math & Statistics (8)](#8-math--statistics)
9. [Pattern Recognition (2)](#9-pattern-recognition)
10. [Data Utilities (3)](#10-data-utilities)
11. [Time & Session (4)](#11-time--session)
12. [PineScript Compatibility (6)](#12-pinescript-compatibility)
13. [Color Constants](#13-color-constants)

---

## Conventions

| Convention | Meaning |
|---|---|
| `foo(candles, period)` | **Point function** — returns a single current value |
| `fooSeries(candles, period)` | **Series function** — returns an array with one value per candle (`null` where insufficient data) |
| `candles` | Array of `{openTime, open, high, low, close, volume}` objects |
| `arr` | Any numeric array (e.g. from a series function) |
| `i` | Bar index (0-based) into an array |

---

## 1. Moving Averages

| Function | Signature | PineScript Equivalent | Description |
|---|---|---|---|
| `sma` | `sma(candles, period)` | `ta.sma` | Simple Moving Average |
| `smaSeries` | `smaSeries(candles, period)` | `ta.sma` series | SMA full series |
| `ema` | `ema(candles, period)` | `ta.ema` | Exponential Moving Average |
| `emaSeries` | `emaSeries(candles, period)` | `ta.ema` series | EMA full series |
| `wma` | `wma(candles, period)` | `ta.wma` | Weighted Moving Average |
| `wmaSeries` | `wmaSeries(candles, period)` | `ta.wma` series | WMA full series |
| `vwma` | `vwma(candles, period)` | `ta.vwma` | Volume-Weighted Moving Average |
| `vwmaSeries` | `vwmaSeries(candles, period)` | `ta.vwma` series | VWMA full series |
| `hullMa` | `hullMa(candles, period)` | `ta.hma` | Hull Moving Average (point) |
| `hmaSeries` | `hmaSeries(candles, period)` | `ta.hma` series | HMA full series |
| `dema` | `dema(candles, period)` | `ta.dema` | Double EMA |
| `demaSeries` | `demaSeries(candles, period)` | `ta.dema` series | DEMA full series |
| `tema` | `tema(candles, period)` | `ta.tema` | Triple EMA |
| `temaSeries` | `temaSeries(candles, period)` | `ta.tema` series | TEMA full series |
| `swma` | `swma(candles, period?)` | `ta.swma` | Symmetrically Weighted MA (default period=4) |
| `swmaSeries` | `swmaSeries(candles, period?)` | `ta.swma` series | SWMA full series |
| `zlema` | `zlema(candles, period)` | — | Zero-Lag EMA |
| `zlemaSeries` | `zlemaSeries(candles, period)` | — | ZLEMA full series |
| `kama` | `kama(candles, period, fast?, slow?)` | — | Kaufman Adaptive MA |
| `kamaSeries` | `kamaSeries(candles, period, fast?, slow?)` | — | KAMA full series |
| `alma` | `alma(candles, period, offset?, sigma?)` | `ta.alma` | Arnaud Legoux MA |
| `almaSeries` | `almaSeries(candles, period, offset?, sigma?)` | `ta.alma` series | ALMA full series |
| `t3` | `t3(candles, period, vfactor?)` | — | Tillson T3 MA |
| `mcginley` | `mcginley(candles, period)` | — | McGinley Dynamic |
| `mcginleySeries` | `mcginleySeries(candles, period)` | — | McGinley full series |
| `frama` | `frama(candles, period)` | — | Fractal Adaptive MA |

---

## 2. Oscillators & Momentum

| Function | Signature | PineScript Equivalent | Description |
|---|---|---|---|
| `rsi` | `rsi(candles, period)` | `ta.rsi` | Relative Strength Index |
| `rsiSeries` | `rsiSeries(candles, period)` | `ta.rsi` series | RSI full series |
| `macd` | `macd(candles, fast, slow, sig)` | `ta.macd` | MACD line, signal, histogram |
| `macdSeries` | `macdSeries(candles, fast, slow, sig)` | `ta.macd` series | Returns `{macd[], signal[], histogram[]}` |
| `stochastic` | `stochastic(candles, kPeriod, dPeriod)` | `ta.stoch` | Returns `[%K, %D]` |
| `stochasticSeries` | `stochasticSeries(candles, kPeriod, dPeriod)` | `ta.stoch` series | Returns `{k[], d[]}` |
| `stochasticRsi` | `stochasticRsi(candles, rsi, stoch, kSmooth, dSmooth)` | `ta.stochrsi` | Returns `[%K, %D]` |
| `stochasticRsiSeries` | `stochasticRsiSeries(...)` | `ta.stochrsi` series | Returns `{k[], d[]}` |
| `cci` | `cci(candles, period)` | `ta.cci` | Commodity Channel Index |
| `cciSeries` | `cciSeries(candles, period)` | `ta.cci` series | CCI full series |
| `mfi` | `mfi(candles, period)` | `ta.mfi` | Money Flow Index |
| `mfiSeries` | `mfiSeries(candles, period)` | `ta.mfi` series | MFI full series |
| `williamsR` | `williamsR(candles, period)` | `ta.wpr` | Williams %R |
| `williamsRSeries` | `williamsRSeries(candles, period)` | `ta.wpr` series | Williams %R full series |
| `adx` | `adx(candles, period)` | `ta.adx` | Average Directional Index |
| `adxSeries` | `adxSeries(candles, period)` | `ta.adx` series | ADX full series |
| `cmo` | `cmo(candles, period)` | `ta.cmo` | Chande Momentum Oscillator |
| `cmoSeries` | `cmoSeries(candles, period)` | `ta.cmo` series | CMO full series |
| `roc` | `roc(candles, period)` | `ta.roc` | Rate of Change (%) |
| `rocSeries` | `rocSeries(candles, period)` | `ta.roc` series | ROC full series |
| `momentum` | `momentum(candles, period)` | `ta.mom` | Price Momentum (absolute change) |
| `momentumSeries` | `momentumSeries(candles, period)` | `ta.mom` series | Momentum full series |
| `trix` | `trix(candles, period)` | `ta.trix` | TRIX (1-bar ROC of triple-smoothed EMA) |
| `trixSeries` | `trixSeries(candles, period)` | — | TRIX full series |
| `trixSignal` | `trixSignal(candles, period, signalPeriod)` | — | TRIX with signal line |
| `awesomeOscillator` | `awesomeOscillator(candles)` | — | Awesome Oscillator (AO) |
| `awesomeOscillatorSeries` | `awesomeOscillatorSeries(candles)` | — | AO full series |
| `acceleratorOscillator` | `acceleratorOscillator(candles)` | — | Accelerator Oscillator |
| `acceleratorOscillatorSeries` | `acceleratorOscillatorSeries(candles)` | — | AC full series |
| `ultimateOscillator` | `ultimateOscillator(candles, p1?, p2?, p3?)` | — | Ultimate Oscillator |
| `ultimateOscillatorSeries` | `ultimateOscillatorSeries(candles, p1?, p2?, p3?)` | — | UO full series |
| `kst` | `kst(candles, ...)` | — | Know Sure Thing. Returns `{kst, signal, histogram}` |
| `kstSeries` | `kstSeries(candles, ...)` | — | KST full series |
| `ppo` | `ppo(candles, fast?, slow?, signal?)` | — | Percentage Price Oscillator |
| `ppoSeries` | `ppoSeries(candles, fast?, slow?, signal?)` | — | Returns `{ppo[], signal[], histogram[]}` |
| `dpo` | `dpo(candles, period)` | `ta.dpo` | Detrended Price Oscillator |
| `dpoSeries` | `dpoSeries(candles, period)` | — | DPO full series |
| `coppockCurve` | `coppockCurve(candles, longRoc?, shortRoc?, wma?)` | — | Coppock Curve |
| `coppockCurveSeries` | `coppockCurveSeries(candles, ...)` | — | Coppock full series |
| `percentRank` | `percentRank(candles, period?)` | `ta.percentrank` | Percent Rank of close |
| `percentRankSeries` | `percentRankSeries(candles, period?)` | `ta.percentrank` series | Percent Rank full series |
| `percentRankArr` | `percentRankArr(arr, period, i?)` | `ta.percentrank` generic | Percent Rank of any array at index i |

---

## 3. Volatility & Bands

| Function | Signature | PineScript Equivalent | Description |
|---|---|---|---|
| `atr` | `atr(candles, period)` | `ta.atr` | Average True Range |
| `atrSeries` | `atrSeries(candles, period)` | `ta.atr` series | ATR full series |
| `trueRange` | `trueRange(candles)` | `ta.tr` | True Range (last bar) |
| `trueRangeSeries` | `trueRangeSeries(candles)` | `ta.tr` series | TR full series |
| `natr` | `natr(candles, period?)` | — | Normalized ATR (ATR/close × 100) |
| `natrSeries` | `natrSeries(candles, period?)` | — | NATR full series |
| `bollinger` | `bollinger(candles, period, numStd)` | `ta.bb` | Returns `{upper, middle, lower}` |
| `bollingerSeries` | `bollingerSeries(candles, period, numStd)` | `ta.bb` series | Returns `{upper[], middle[], lower[]}` |
| `bollingerPercentB` | `bollingerPercentB(candles, period?, numStd?)` | `ta.bbw` / manual | Bollinger %B: `(close - lower) / (upper - lower)` |
| `bollingerPercentBSeries` | `bollingerPercentBSeries(candles, period?, numStd?)` | — | %B full series |
| `bollingerWidth` | `bollingerWidth(candles, period?, numStd?)` | BBW | Bollinger Bandwidth: `(upper - lower) / middle` |
| `bollingerWidthSeries` | `bollingerWidthSeries(candles, period?, numStd?)` | — | BBW full series |
| `keltnerChannels` | `keltnerChannels(candles, period, mult?, atrPeriod?)` | `ta.kc` | Returns `{upper, middle, lower}` |
| `keltnerChannelsSeries` | `keltnerChannelsSeries(...)` | `ta.kc` series | Keltner full series |
| `donchianChannels` | `donchianChannels(candles, period)` | `ta.dc` | Returns `{upper, middle, lower}` |
| `donchianChannelsSeries` | `donchianChannelsSeries(candles, period)` | `ta.dc` series | Donchian full series |
| `supertrend` | `supertrend(candles, period, multiplier)` | `ta.supertrend` | Returns `[value, isUp]` |
| `supertrendSeries` | `supertrendSeries(candles, period?, mult?)` | `ta.supertrend` series | Returns `{up[], down[], direction[]}` |
| `stdDev` | `stdDev(candles, period?)` | `ta.stdev` | Standard deviation of closes |
| `stdDevSeries` | `stdDevSeries(candles, period?)` | `ta.stdev` series | Std dev full series |
| `historicalVolatility` | `historicalVolatility(candles, period?)` | — | Annualized HV (%) |
| `historicalVolatilitySeries` | `historicalVolatilitySeries(candles, period?)` | — | HV full series |
| `chaikinVolatility` | `chaikinVolatility(candles, emaPeriod?, rocPeriod?)` | — | Chaikin Volatility |
| `ulcerIndex` | `ulcerIndex(candles, period?)` | — | Ulcer Index |

---

## 4. Volume Indicators

| Function | Signature | PineScript Equivalent | Description |
|---|---|---|---|
| `obv` | `obv(candles)` | `ta.obv` | On-Balance Volume |
| `obvSeries` | `obvSeries(candles)` | `ta.obv` series | OBV full series |
| `vwap` | `vwap(candles)` | — | Cumulative VWAP (no session reset) |
| `vwapSeries` | `vwapSeries(candles)` | — | Cumulative VWAP full series |
| `sessionVwap` | `sessionVwap(candles)` | `ta.vwap` | Session VWAP (resets each UTC day) |
| `sessionVwapSeries` | `sessionVwapSeries(candles)` | `ta.vwap` series | Session VWAP full series |
| `sessionVwapBands` | `sessionVwapBands(candles, numStd?)` | VWAP bands | Returns `{vwap[], upper[], lower[]}` |
| `cmf` | `cmf(candles, period?)` | `ta.cmf` | Chaikin Money Flow |
| `cmfSeries` | `cmfSeries(candles, period)` | `ta.cmf` series | CMF full series |
| `adLine` | `adLine(candles)` | — | Accumulation/Distribution Line |
| `adLineSeries` | `adLineSeries(candles)` | — | A/D full series |
| `chaikinOscillator` | `chaikinOscillator(candles, fast?, slow?)` | — | Chaikin Oscillator |
| `chaikinOscillatorSeries` | `chaikinOscillatorSeries(candles, fast?, slow?)` | — | Chaikin Osc full series |
| `eom` | `eom(candles, period?)` | — | Ease of Movement |
| `eomSeries` | `eomSeries(candles, period?)` | — | EOM full series |
| `forceIndex` | `forceIndex(candles, period?)` | — | Force Index |
| `forceIndexSeries` | `forceIndexSeries(candles, period?)` | — | Force Index full series |
| `kvo` | `kvo(candles, fast?, slow?, signal?)` | — | Klinger Volume Oscillator |
| `volumeProfile` | `volumeProfile(candles, bins?)` | — | Returns `{poc, valueAreaHigh, valueAreaLow}` |
| `netVolume` | `netVolume(candles)` | — | Net Volume |
| `netVolumeSeries` | `netVolumeSeries(candles)` | — | Net Volume full series |
| `vwRsi` | `vwRsi(candles, period)` | — | Volume-Weighted RSI |

---

## 5. Trend & Directional

| Function | Signature | PineScript Equivalent | Description |
|---|---|---|---|
| `ichimoku` | `ichimoku(candles, tenkan?, kijun?, senkouB?)` | `ta.ichimoku` | Returns `{tenkan, kijun, senkouA, senkouB, chikou}` |
| `ichimokuSeries` | `ichimokuSeries(candles, ...)` | — | Ichimoku full series (each field is an array) |
| `parabolicSar` | `parabolicSar(candles, step?, maxStep?)` | `ta.sar` | Parabolic SAR (current value) |
| `parabolicSarSeries` | `parabolicSarSeries(candles, step?, maxStep?)` | `ta.sar` series | Returns `{sar[], direction[]}` |
| `aroon` | `aroon(candles, period)` | `ta.aroon` | Returns `{up, down, oscillator}` |
| `aroonSeries` | `aroonSeries(candles, period)` | — | Returns `{up[], down[], oscillator[]}` |
| `vortex` | `vortex(candles, period)` | `ta.vortex` | Returns `{plus, minus}` |
| `vortexSeries` | `vortexSeries(candles, period)` | — | Returns `{plus[], minus[]}` |
| `diPlus` | `diPlus(candles, period?)` | `ta.dmi` | Directional Indicator + |
| `diMinus` | `diMinus(candles, period?)` | `ta.dmi` | Directional Indicator − |
| `diSeries` | `diSeries(candles, period?)` | — | Returns `{plus[], minus[]}` |
| `elderRay` | `elderRay(candles, period?)` | — | Returns `{bullPower, bearPower}` |
| `elderRaySeries` | `elderRaySeries(candles, period?)` | — | Returns `{bullPower[], bearPower[]}` |
| `massIndex` | `massIndex(candles, emaPeriod?, sumPeriod?)` | — | Mass Index (reversal signal) |
| `priceChannel` | `priceChannel(candles, period)` | — | Returns `{upper, lower, middle}` |

---

## 6. Helpers & Utilities

### Cross Detection

| Function | Signature | PineScript Equivalent | Description |
|---|---|---|---|
| `crossover` | `crossover(a, b, i)` | `ta.crossover` | True if `a[i-1] <= b[i-1]` and `a[i] > b[i]` |
| `crossunder` | `crossunder(a, b, i)` | `ta.crossunder` | True if `a[i-1] >= b[i-1]` and `a[i] < b[i]` |

### Highest / Lowest

| Function | Signature | PineScript Equivalent | Description |
|---|---|---|---|
| `highest` | `highest(arr, period, i)` | `ta.highest` | Highest value in `arr[i-period+1..i]` |
| `lowest` | `lowest(arr, period, i)` | `ta.lowest` | Lowest value in `arr[i-period+1..i]` |

### Value Functions

| Function | Signature | PineScript Equivalent | Description |
|---|---|---|---|
| `change` | `change(arr, i, lookback?)` | `ta.change` | `arr[i] - arr[i-lookback]` |
| `nz` | `nz(val, replacement?)` | `nz` | Replace null/NaN/undefined with replacement (default 0) |
| `na` | `na(val)` | `na` | True if value is null, undefined, or NaN |
| `valuewhen` | `valuewhen(condArr, srcArr, occurrence)` | `ta.valuewhen` | Value of src when condition was true, Nth time back |
| `barssince` | `barssince(condArr)` | `ta.barssince` | Bars since last true in condition array |
| `rising` | `rising(arr, period, i)` | `ta.rising` | True if arr has been rising for `period` bars at index i |
| `falling` | `falling(arr, period, i)` | `ta.falling` | True if arr has been falling for `period` bars at index i |
| `cum` | `cum(arr)` | `ta.cum` | Cumulative sum array |

### Pivots

| Function | Signature | PineScript Equivalent | Description |
|---|---|---|---|
| `pivotHigh` | `pivotHigh(candles, leftBars, rightBars)` | `ta.pivothigh` | Array of pivot highs (null where no pivot) |
| `pivotLow` | `pivotLow(candles, leftBars, rightBars)` | `ta.pivotlow` | Array of pivot lows (null where no pivot) |

### Regression

| Function | Signature | PineScript Equivalent | Description |
|---|---|---|---|
| `linreg` | `linreg(arr, period, i)` | `ta.linreg` | Linear regression value at index i |
| `linregSeries` | `linregSeries(arr, period)` | `ta.linreg` series | Full linear regression series |

### Source Selector

| Function | Signature | Description |
|---|---|---|
| `source` | `source(candles, field?)` | Extract field as array. Fields: `"open"`, `"high"`, `"low"`, `"close"` (default), `"volume"`, `"hl2"`, `"hlc3"`, `"ohlc4"` |

---

## 7. Multi-Timeframe

| Function | Signature | PineScript Equivalent | Description |
|---|---|---|---|
| `security` | `security(candles, factor, fn)` | `request.security` | Multi-timeframe access |

### Usage

```javascript
// factor can be a number (how many base bars per HTF bar)
// or a string like "4h", "1d", "1w"

// Example 1: Get 4h RSI while on a 1h chart
var htfRsi = security(_candles, 4, function(c) {
    return rsiSeries(c, 14);
});
// htfRsi is now an array aligned to base 1h bars

// Example 2: Get daily SMA(50) while on 15m chart
var dailySma = security(_candles, "1d", function(c) {
    return smaSeries(c, 50);
});

// Example 3: Get daily Bollinger Bands
var dailyBB = security(_candles, "1d", function(c) {
    return bollingerSeries(c, 20, 2);
});
// dailyBB.upper[], dailyBB.middle[], dailyBB.lower[] — all aligned to base bars

// Example 4: Simple higher-TF close
var dailyClose = security(_candles, "1d", function(c) {
    return c.map(function(bar) { return bar.close; });
});
```

**Supported timeframe strings:** `"5m"`, `"15m"`, `"30m"`, `"1h"`, `"2h"`, `"4h"`, `"6h"`, `"12h"`, `"1d"`, `"1w"`, `"1M"`

The `security` function:
1. Aggregates base candles into higher-TF OHLCV bars
2. Calls your function on the HTF bars
3. Maps the result back to base-timeframe bars (forward-fills within each HTF bar)
4. Handles single values, arrays, and objects with named series

---

## 8. Math & Statistics

| Function | Signature | Description |
|---|---|---|
| `correlation` | `correlation(arr1, arr2)` | Pearson correlation coefficient between two arrays |
| `covariance` | `covariance(arr1, arr2)` | Covariance of two arrays |
| `beta` | `beta(candles, benchCandles, period)` | Beta coefficient vs benchmark |
| `percentile` | `percentile(arr, pct)` | Pth percentile of an array (0–100) |
| `median` | `median(arr)` | Median of an array |
| `sum` | `sum(arr)` | Sum of all non-null values |
| `avg` | `avg(arr)` | Average of all non-null values |
| `abs` | `abs(val)` | Absolute value |
| `max` | `max(a, b)` | Maximum of two values |
| `min` | `min(a, b)` | Minimum of two values |
| `clamp` | `clamp(val, lo, hi)` | Clamp value to [lo, hi] |
| `zScore` | `zScore(candles, period)` | Z-score of current close |
| `zScoreSeries` | `zScoreSeries(candles, period)` | Z-score full series |

---

## 9. Pattern Recognition

| Function | Signature | Description |
|---|---|---|
| `candlePattern` | `candlePattern(candles)` | Detects basic candle patterns on last bars. Returns pattern name string |
| `candlePatternSeries` | `candlePatternSeries(candles)` | Pattern name for each bar |
| `supportResistance` | `supportResistance(candles, leftBars?, rightBars?, maxLevels?)` | Returns `{support[], resistance[]}` price levels |

---

## 10. Data Utilities

| Function | Signature | Description |
|---|---|---|
| `resample` | `resample(candles, factor)` | Aggregate candles by factor into lower-frequency OHLCV bars |
| `source` | `source(candles, field?)` | Extract a named field as a numeric array |
| `input` | `input(name, defaultValue)` | Retrieve a configurable parameter (used by indicator system) |

---

## 11. Time & Session

| Function | Signature | Description |
|---|---|---|
| `dayofweek` | `dayofweek(candle)` | Day of week (0=Sunday, 6=Saturday) |
| `hour` | `hour(candle)` | Hour of day (0–23) |
| `sessionVwap` | `sessionVwap(candles)` | VWAP resetting each UTC day |
| `sessionVwapSeries` | `sessionVwapSeries(candles)` | Session VWAP full series |
| `sessionVwapBands` | `sessionVwapBands(candles, numStd?)` | Session VWAP ± std dev bands |

---

## 12. PineScript Compatibility

Functions designed to match PineScript behavior:

| PineScript | TFG Equivalent | Notes |
|---|---|---|
| `ta.sma(src, length)` | `sma(candles, length)` | Operates on candle closes |
| `ta.ema(src, length)` | `ema(candles, length)` | |
| `ta.rsi(src, length)` | `rsi(candles, length)` | |
| `ta.macd(src, fast, slow, sig)` | `macd(candles, fast, slow, sig)` | Returns `{macd, signal, histogram}` |
| `ta.bb(src, length, mult)` | `bollinger(candles, length, mult)` | Returns `{upper, middle, lower}` |
| `ta.bbw(src, length, mult)` | `bollingerWidth(candles, length, mult)` | Bandwidth |
| `ta.atr(length)` | `atr(candles, length)` | |
| `ta.stoch(close, high, low, length)` | `stochastic(candles, k, d)` | |
| `ta.cci(src, length)` | `cci(candles, length)` | |
| `ta.mfi(src, length)` | `mfi(candles, length)` | |
| `ta.wpr(length)` | `williamsR(candles, length)` | |
| `ta.adx(length)` | `adx(candles, length)` | |
| `ta.obv` | `obv(candles)` | |
| `ta.vwap` | `sessionVwap(candles)` | Session-resetting |
| `ta.cmf(src, length)` | `cmf(candles, length)` | |
| `ta.supertrend(factor, length)` | `supertrend(candles, length, factor)` | Note: param order differs |
| `ta.sar(start, inc, max)` | `parabolicSar(candles, step, max)` | |
| `ta.kc(src, length, mult)` | `keltnerChannels(candles, length, mult)` | |
| `ta.dc(length)` | `donchianChannels(candles, length)` | |
| `ta.dmi(length)` | `diPlus(c, l)` / `diMinus(c, l)` / `adx(c, l)` | Split into separate functions |
| `ta.swma(src)` | `swma(candles)` | |
| `ta.percentrank(src, length)` | `percentRank(candles, length)` | |
| `ta.crossover(a, b)` | `crossover(a, b, i)` | Requires explicit index |
| `ta.crossunder(a, b)` | `crossunder(a, b, i)` | Requires explicit index |
| `ta.highest(src, length)` | `highest(arr, length, i)` | Requires explicit index |
| `ta.lowest(src, length)` | `lowest(arr, length, i)` | Requires explicit index |
| `ta.change(src, length)` | `change(arr, i, length)` | Requires explicit index |
| `ta.linreg(src, length, offset)` | `linreg(arr, length, i)` | |
| `ta.pivothigh(left, right)` | `pivotHigh(candles, left, right)` | |
| `ta.pivotlow(left, right)` | `pivotLow(candles, left, right)` | |
| `ta.valuewhen(cond, src, occ)` | `valuewhen(condArr, srcArr, occ)` | |
| `ta.barssince(cond)` | `barssince(condArr)` | |
| `ta.rising(src, length)` | `rising(arr, period, i)` | |
| `ta.falling(src, length)` | `falling(arr, period, i)` | |
| `ta.cum(src)` | `cum(arr)` | |
| `request.security(sym, tf, expr)` | `security(candles, tf, fn)` | Same chart symbol, different timeframe |
| `ta.hma(src, length)` | `hullMa(candles, length)` | |
| `ta.wma(src, length)` | `wma(candles, length)` | |
| `ta.vwma(src, length)` | `vwma(candles, length)` | |
| `ta.dema(src, length)` | `dema(candles, length)` | |
| `ta.tema(src, length)` | `tema(candles, length)` | |
| `ta.alma(src, length, offset, sigma)` | `alma(candles, length, offset, sigma)` | |
| `ta.cmo(src, length)` | `cmo(candles, length)` | |
| `ta.roc(src, length)` | `roc(candles, length)` | |
| `ta.mom(src, length)` | `momentum(candles, length)` | |
| `ta.trix(src, length)` | `trix(candles, length)` | |
| `ta.dpo(src, length)` | `dpo(candles, length)` | |
| `ta.stdev(src, length)` | `stdDev(candles, length)` | |
| `ta.tr` | `trueRange(candles)` | |
| `nz(x, y)` | `nz(val, replacement)` | |
| `na(x)` | `na(val)` | |

---

## 13. Color Constants

Available via the `color` object:

```javascript
color.green    // "#26A69A"
color.red      // "#EF5350"
color.blue     // "#2196F3"
color.orange   // "#FF9800"
color.yellow   // "#FFD700"
color.purple   // "#9C27B0"
color.white    // "#FFFFFF"
color.gray     // "#8B949E"
color.aqua     // "#00BCD4"
color.lime     // "#00E676"
color.pink     // "#FF4081"
color.silver   // "#C0C0C0"
color.none     // "transparent"
color.rgba(r, g, b, a)     // e.g. color.rgba(255, 0, 0, 0.5)
color.hexAlpha(hex, alpha)  // e.g. color.hexAlpha("#FF0000", 0.5)
```

---

## Total Function Count

| Category | Count |
|---|---|
| Moving Averages | 26 (13 point + 13 series) |
| Oscillators & Momentum | 40 (20 point + 20 series) |
| Volatility & Bands | 24 (12 point + 12 series) |
| Volume Indicators | 22 (11 point + 11 series) |
| Trend & Directional | 15 |
| Helpers & Utilities | 18 |
| Multi-Timeframe | 1 (`security`) |
| Math & Statistics | 13 |
| Pattern Recognition | 3 |
| Data Utilities | 3 |
| Time & Session | 5 |
| **Total** | **~170 functions** |

---

## Quick Example: Full Strategy

```javascript
function strategy(candles) {
    var i = candles.length - 1;
    if (i < 50) return { type: "HOLD" };

    // Indicators
    var rsiVal     = rsi(candles, 14);
    var macdVal    = macd(candles, 12, 26, 9);
    var bb         = bollinger(candles, 20, 2);
    var bbPctB     = bollingerPercentB(candles, 20, 2);
    var stVal      = supertrend(candles, 10, 3);
    var sVwap      = sessionVwap(candles);
    var price      = candles[i].close;

    // Multi-timeframe: daily RSI
    var dailyRsi = security(candles, "1d", function(c) {
        return rsiSeries(c, 14);
    });

    // BUY: RSI oversold + MACD bullish + below lower BB + daily RSI not overbought
    if (rsiVal < 30 && macdVal.histogram > 0 && bbPctB < 0.1
        && stVal[1] === true && (dailyRsi[i] === null || dailyRsi[i] < 70)) {
        return { type: "BUY", sizePct: 5, stopLossPct: 2, takeProfitPct: 4 };
    }

    // SELL: RSI overbought + MACD bearish + above upper BB
    if (rsiVal > 70 && macdVal.histogram < 0 && bbPctB > 0.9 && stVal[1] === false) {
        return { type: "SELL", sizePct: 5, stopLossPct: 2, takeProfitPct: 4 };
    }

    return { type: "HOLD" };
}
```
