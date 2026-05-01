# TFG Indicator Library — Complete Reference

Built-in JavaScript indicator functions available in TFG strategies and custom indicators.
**177 functions** across 13 categories.

---

## 1. Moving Averages (26)

| Function | Params | Returns | Description |
|----------|--------|---------|-------------|
| `sma(candles, period)` | candles, period | number | Simple Moving Average |
| `ema(candles, period)` | candles, period | number | Exponential Moving Average |
| `wma(candles, period)` | candles, period | number | Weighted Moving Average |
| `vwma(candles, period)` | candles, period | number | Volume-Weighted MA |
| `hullMa(candles, period)` | candles, period | number | Hull Moving Average |
| `dema(candles, period)` | candles, period | number | Double EMA |
| `tema(candles, period)` | candles, period | number | Triple EMA |
| `zlema(candles, period)` | candles, period | number | Zero-Lag EMA |
| `kama(candles, period, fast, slow)` | candles, period, fast=2, slow=30 | number | Kaufman Adaptive MA |
| `alma(candles, period, offset, sigma)` | candles, period, offset=0.85, sigma=6 | number | Arnaud Legoux MA |
| `t3(candles, period, vfactor)` | candles, period, vfactor=0.7 | number | T3 Tillson MA |
| `mcginley(candles, period)` | candles, period | number | McGinley Dynamic |
| `frama(candles, period)` | candles, period | number | Fractal Adaptive MA |
| `swma(candles, period)` | candles, period | number | Symmetrically Weighted MA |

**Series variants** (return `[{time, value}]` arrays for `plot()`):
`smaSeries`, `emaSeries`, `wmaSeries`, `vwmaSeries`, `hmaSeries`, `demaSeries`,
`temaSeries`, `zlemaSeries`, `kamaSeries`, `almaSeries`, `mcginleySeries`, `swmaSeries`

---

## 2. Oscillators & Momentum (52)

| Function | Returns | Description |
|----------|---------|-------------|
| `rsi(candles, period)` | number | Relative Strength Index |
| `stochastic(candles, kPeriod, dPeriod)` | `[k, d]` | Stochastic Oscillator |
| `stochasticRsi(candles, rsiP, stochP, kSmooth, dSmooth)` | `[k, d]` | Stochastic RSI |
| `macd(candles, fast, slow, sig)` | `[macd, signal, hist]` | MACD |
| `adx(candles, period)` | number | Average Directional Index |
| `dmi(candles, period)` | `{diPlus, diMinus, adx}` | Directional Movement Index |
| `cci(candles, period)` | number | Commodity Channel Index |
| `mfi(candles, period)` | number | Money Flow Index |
| `williamsR(candles, period)` | number | Williams %R |
| `roc(candles, period)` | number | Rate of Change |
| `momentum(candles, period)` | number | Price momentum |
| `trix(candles, period)` | number | TRIX |
| `trixSignal(candles, period, sigPeriod)` | `{trix, signal}` | TRIX + signal line |
| `awesomeOscillator(candles)` | number | Awesome Oscillator |
| `acceleratorOscillator(candles)` | number | Accelerator Oscillator |
| `ultimateOscillator(candles, p1, p2, p3)` | number | Ultimate Oscillator |
| `cmo(candles, period)` | number | Chande Momentum Oscillator |
| `kst(candles, ...)` | `{kst, signal, hist}` | Know Sure Thing |
| `vortex(candles, period)` | `{plus, minus}` | Vortex Indicator |
| `aroon(candles, period)` | `{up, down, osc}` | Aroon Indicator |
| `elderRay(candles, period)` | `{bullPower, bearPower}` | Elder Ray |
| `coppockCurve(candles, longRoc, shortRoc, wmaPeriod)` | number | Coppock Curve |
| `dpo(candles, period)` | number | Detrended Price Oscillator |
| `ppo(candles, fast, slow, signal)` | number | Percentage Price Oscillator |
| `zScore(candles, period)` | number | Z-Score |
| `percentRank(candles, period)` | number | Percent Rank |
| `bollingerPercentB(candles, period, numStd)` | number | Bollinger %B |

All oscillators have `*Series` variants returning `[{time, value}]` arrays.

---

## 3. Trend & Bands (22)

| Function | Returns | Description |
|----------|---------|-------------|
| `bollinger(candles, period, numStd)` | `{upper, middle, lower}` | Bollinger Bands |
| `bollingerWidth(candles, period, numStd)` | number | Bollinger Bandwidth |
| `supertrend(candles, period, mult)` | `[value, isUp]` | Supertrend |
| `ichimoku(candles, tenkan, kijun, senkouB)` | `{tenkan, kijun, senkouA, senkouB, chikou}` | Ichimoku Cloud |
| `parabolicSar(candles, step, maxStep)` | number | Parabolic SAR |
| `keltnerChannels(candles, period, mult, atrP)` | `{upper, middle, lower}` | Keltner Channels |
| `donchianChannels(candles, period)` | `{upper, middle, lower}` | Donchian Channels |
| `priceChannel(candles, period)` | `{upper, lower, middle}` | Price Channel |
| `diPlus(candles, period)` / `diMinus(...)` | number | Directional Indicators |
| `massIndex(candles, emaP, sumP)` | number | Mass Index |
| `sessionVwapBands(candles, numStd)` | `{vwap, upper, lower}` | VWAP ± N σ bands |
| `atr(candles, period)` | number | Average True Range |

Series variants: `bollingerSeries`, `supertrendSeries`, `ichimokuSeries`,
`parabolicSarSeries`, `keltnerChannelsSeries`, `donchianChannelsSeries`,
`bollingerPercentBSeries`, `bollingerWidthSeries`, `atrSeries`, `diSeries`

---

## 4. Volume (21)

| Function | Returns | Description |
|----------|---------|-------------|
| `obv(candles)` | number | On-Balance Volume |
| `vwap(candles)` | number | VWAP |
| `sessionVwap(candles)` | number | Session VWAP (daily reset) |
| `adLine(candles)` | number | Accumulation/Distribution Line |
| `chaikinOscillator(candles, fast, slow)` | number | Chaikin Oscillator |
| `cmf(candles, period)` | number | Chaikin Money Flow |
| `eom(candles, period)` | number | Ease of Movement |
| `forceIndex(candles, period)` | number | Force Index |
| `kvo(candles, fast, slow, sig)` | `{kvo, signal}` | Klinger Volume Oscillator |
| `volumeProfile(candles, bins)` | `{poc, vaHigh, vaLow}` | Volume Profile |
| `netVolume(candles)` | number | Net Volume |
| `vwRsi(candles, period)` | number | Volume-Weighted RSI |

Series variants: `obvSeries`, `vwapSeries`, `sessionVwapSeries`, `adLineSeries`,
`chaikinOscillatorSeries`, `cmfSeries`, `eomSeries`, `forceIndexSeries`, `netVolumeSeries`

---

## 5. Volatility (12)

| Function | Returns | Description |
|----------|---------|-------------|
| `historicalVolatility(candles, period)` | number | Annualized HV (log returns) |
| `chaikinVolatility(candles, emaP, rocP)` | number | Chaikin Volatility |
| `ulcerIndex(candles, period)` | number | Ulcer Index |
| `natr(candles, period)` | number | Normalized ATR |
| `trueRange(candles)` | number | True Range |
| `stdDev(candles, period)` | number | Standard Deviation |

Series variants: `historicalVolatilitySeries`, `natrSeries`, `trueRangeSeries`, `stdDevSeries`

---

## 6. Pattern Detection (2)

| Function | Returns | Description |
|----------|---------|-------------|
| `candlePattern(candles)` | `{pattern, type}` | Last candle pattern |
| `candlePatternSeries(candles)` | `[{pattern, type}]` | All candle patterns |

Detected patterns: doji, hammer, hangingMan, invertedHammer, shootingStar,
bullishEngulfing, bearishEngulfing, morningStar, eveningStar, threeWhiteSoldiers,
threeBlackCrows, marubozu, spinningTop, tweezerTop, tweezerBottom, piercingLine,
darkCloudCover.

---

## 7. Support / Resistance (3)

| Function | Returns | Description |
|----------|---------|-------------|
| `pivotHigh(candles, left, right)` | `[number\|null]` | Pivot high array |
| `pivotLow(candles, left, right)` | `[number\|null]` | Pivot low array |
| `supportResistance(candles, left, right, maxLevels)` | `{resistance, support}` | S/R levels |

---

## 8. Linear Regression (2)

| Function | Returns | Description |
|----------|---------|-------------|
| `linreg(arr, period, i)` | number | Linear regression at index |
| `linregSeries(arr, period)` | `[number]` | Full regression series |

---

## 9. Multi-Timeframe (2)

| Function | Returns | Description |
|----------|---------|-------------|
| `resample(candles, factor)` | `[candle]` | Merge N candles into 1 |
| `security(candles, factor, fn)` | number | Apply function on HTF |

---

## 10. Math & Statistics (20)

| Function | Description |
|----------|-------------|
| `correlation(arr1, arr2)` | Pearson correlation |
| `covariance(arr1, arr2)` | Covariance |
| `beta(candles, benchmark, period)` | CAPM beta |
| `percentile(arr, pct)` | Percentile value |
| `median(arr)` | Median |
| `sum(arr)` | Sum |
| `avg(arr)` | Average |
| `abs(val)` | Absolute value |
| `max(a, b)` / `min(a, b)` | Min / Max |
| `clamp(val, lo, hi)` | Clamp between bounds |
| `nz(val, replacement)` | Replace null/NaN |
| `na(val)` | Check if null/NaN |
| `valuewhen(cond, src, occ)` | Value when condition was true |
| `barssince(cond)` | Bars since condition |
| `rising(arr, period, i)` | Is rising for N bars |
| `falling(arr, period, i)` | Is falling for N bars |
| `cum(arr)` | Cumulative sum |
| `dayofweek(candle)` | Day of week (0–6) |
| `hour(candle)` | Hour (0–23) |

---

## 11. Crossover & Series Utilities (5)

| Function | Description |
|----------|-------------|
| `crossover(a, b, i)` | a crossed above b at index i |
| `crossunder(a, b, i)` | a crossed below b at index i |
| `highest(arr, period, i)` | Highest in window |
| `lowest(arr, period, i)` | Lowest in window |
| `change(arr, i, lookback)` | Difference over N bars |

---

## 12. Source & Input (2)

| Function | Description |
|----------|-------------|
| `source(candles, field)` | Extract field array: open/high/low/close/volume/hl2/hlc3/ohlc4 |
| `input(name, defaultValue)` | Read configurable parameter |

---

## 13. Plot API (8)

See [STRATEGY_API.md](STRATEGY_API.md#3-chart-overlay-api-plot-functions) for full plot documentation.

| Function | Description |
|----------|-------------|
| `resetPlot()` | Clear all plot data |
| `plot(name, values, color, lineWidth, lineStyle)` | Overlay line on price chart |
| `plotPanel(name, values, color, type, refLines, extraLines)` | Sub-panel indicator |
| `hline(price, color, title, lineStyle)` | Horizontal line |
| `plotFill(line1, line2, color)` | Fill between two lines |
| `plotLabel(index, text, position, color)` | Text label at bar |
| `plotBgColor(index, color)` | Background colour at bar |

---

## 14. Color Helpers

```javascript
color.green    // '#26A69A'
color.red      // '#EF5350'
color.blue     // '#42A5F5'
color.orange   // '#FF9800'
color.yellow   // '#FFEB3B'
color.purple   // '#AB47BC'
color.white    // '#FFFFFF'
color.gray     // '#787B86'
color.aqua     // '#00BCD4'
color.lime     // '#00E676'
color.pink     // '#FF4081'
color.silver   // '#B2B5BE'
color.none     // 'transparent'

color.rgba(r, g, b, a)       // → 'rgba(r,g,b,a)'
color.hexAlpha(hex, alpha)   // → '#RRGGBBAA'
```

---

*Total: 177 functions | TFG Trading Platform*
