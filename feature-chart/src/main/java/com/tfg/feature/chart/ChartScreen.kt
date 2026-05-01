package com.tfg.feature.chart

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tfg.core.ui.components.*
import com.tfg.core.ui.theme.*
import com.tfg.core.util.Formatters
import com.tfg.domain.model.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.window.Dialog

@Composable
fun ChartScreen(
    symbol: String,
    onBack: () -> Unit
) {
    CoinDetailScreen(onBack = onBack, onTrade = {})
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinDetailScreen(
    viewModel: CoinDetailViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onTrade: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val intervals = listOf("1m", "3m", "5m", "15m", "30m", "1h", "2h", "4h", "6h", "12h", "1d", "1w")
    val s = state.indicatorSettings

    // ─── Compute overlay data for WebView chart ─────────────────────
    val overlayIndicators = remember(state.candles, state.showSma, state.showEma, state.showWma,
        state.showVwma, state.showHullMa, state.showDema, state.showTema,
        state.showBollinger, state.showVwap, s) {
        if (state.candles.isEmpty()) emptyList()
        else buildOverlayList(state.candles, state, s)
    }

    val cloudOverlays = remember(state.candles, state.showBollinger, s) {
        if (state.candles.isEmpty()) emptyList()
        else buildCloudList(state.candles, state, s)
    }

    val supertrendData = remember(state.candles, state.showSupertrend, s) {
        if (!state.showSupertrend || state.candles.isEmpty()) null
        else buildSupertrendData(state.candles, s)
    }

    val ichimokuData = remember(state.candles, state.showIchimoku, s) {
        if (!state.showIchimoku || state.candles.isEmpty()) null
        else buildIchimokuData(state.candles, s)
    }

    val subIndicators = remember(state.candles, state.showRsi, state.showMacd, state.showStochastic,
        state.showStochRsi, state.showAtr, state.showAdx, state.showCci, state.showWilliamsR,
        state.showObv, state.showMfi, state.showCmf, s) {
        if (state.candles.isEmpty()) emptyList()
        else buildSubIndicatorList(state.candles, state, s)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // ─── Header: Price + 24h Stats ──────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = TextSecondary)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(state.symbol, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                state.ticker?.let { ticker ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            Formatters.formatPrice(ticker.price),
                            fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        PnlText(value = ticker.priceChangePercent, showPercent = true, fontSize = 14)
                    }
                }
            }
            IconButton(onClick = { onTrade(state.symbol) }) {
                Icon(Icons.Default.SwapVert, "Trade", tint = AccentBlue)
            }
        }

        // ─── Timeframe Selector ─────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            intervals.forEach { interval ->
                FilterChip(
                    selected = state.interval == interval,
                    onClick = { viewModel.changeInterval(interval) },
                    label = { Text(interval, fontSize = 11.sp) },
                    modifier = Modifier.height(28.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentBlue.copy(alpha = 0.2f),
                        selectedLabelColor = AccentBlue,
                        containerColor = DarkCard,
                        labelColor = TextSecondary
                    )
                )
            }
        }

        // ─── TradingView Lightweight Chart ──────────────────────────
        val density = LocalDensity.current
        var chartHeightDp by remember { mutableStateOf(400.dp) }

        // Keep chart always mounted to avoid WebView re-init flicker
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeightDp)
        ) {
            if (state.candles.isNotEmpty() || !state.isLoading) {
                val displayCandles = if (state.replayMode)
                    state.candles.take(state.replayBarIndex + 1) else state.candles
                val displaySignals = if (state.replayMode)
                    state.replaySignals else state.signalMarkers
                TradingViewChart(
                    candles = displayCandles,
                    signals = displaySignals,
                    overlayIndicators = overlayIndicators,
                    subIndicators = subIndicators,
                    livePrice = state.ticker?.price,
                    showDrawingTools = state.showDrawingTools,
                    supertrendData = supertrendData,
                    ichimokuData = ichimokuData,
                    cloudOverlays = cloudOverlays,
                    customIndicatorOutputs = state.indicatorOutputs,
                    strategyPlotJson = state.strategyPlotJson,
                    dashboardJson = state.dashboardOverlayJson,
                    chartType = state.chartType,
                    symbol = state.symbol,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }
            // Loading overlay on top of chart (during interval change)
            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkBackground.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = AccentBlue,
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Loading ${state.interval}...",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
        // Resize handle
        if (state.candles.isNotEmpty() || state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, dragAmount ->
                            with(density) {
                                chartHeightDp = (chartHeightDp + dragAmount.toDp())
                                    .coerceIn(200.dp, 700.dp)
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(TextTertiary.copy(alpha = 0.5f))
                )
            }
        }

        // ─── Scrollable area below chart ────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {

        // ─── Chart Type Selector ────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(
                "candle" to "Candle",
                "line" to "Line",
                "area" to "Area",
                "bar" to "Bar",
                "heikinashi" to "Heikin Ashi"
            ).forEach { (type, label) ->
                FilterChip(
                    selected = state.chartType == type,
                    onClick = { viewModel.setChartType(type) },
                    label = { Text(label, fontSize = 10.sp) },
                    modifier = Modifier.height(26.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentBlue.copy(alpha = 0.2f),
                        selectedLabelColor = AccentBlue,
                        containerColor = DarkCard,
                        labelColor = TextSecondary
                    )
                )
            }
        }

        // ─── Overlay Indicator Toggles ──────────────────────────────
        Text("Overlays", fontSize = 10.sp, color = TextTertiary,
            modifier = Modifier.padding(start = 12.dp, top = 4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IndicatorChip("SMA(${s.smaPeriod})", state.showSma, AccentBlue,
                onToggle = { viewModel.toggleSma() }, onLongPress = { viewModel.openSettings("SMA") })
            IndicatorChip("EMA(${s.emaPeriod})", state.showEma, AccentPurple,
                onToggle = { viewModel.toggleEma() }, onLongPress = { viewModel.openSettings("EMA") })
            IndicatorChip("WMA(${s.wmaPeriod})", state.showWma, Color(0xFF00BCD4),
                onToggle = { viewModel.toggleWma() }, onLongPress = { viewModel.openSettings("WMA") })
            IndicatorChip("VWMA(${s.vwmaPeriod})", state.showVwma, Color(0xFF8BC34A),
                onToggle = { viewModel.toggleVwma() }, onLongPress = { viewModel.openSettings("VWMA") })
            IndicatorChip("Hull(${s.hullMaPeriod})", state.showHullMa, Color(0xFFE91E63),
                onToggle = { viewModel.toggleHullMa() }, onLongPress = { viewModel.openSettings("HULL") })
            IndicatorChip("DEMA(${s.demaPeriod})", state.showDema, Color(0xFF9C27B0),
                onToggle = { viewModel.toggleDema() }, onLongPress = { viewModel.openSettings("DEMA") })
            IndicatorChip("TEMA(${s.temaPeriod})", state.showTema, Color(0xFF3F51B5),
                onToggle = { viewModel.toggleTema() }, onLongPress = { viewModel.openSettings("TEMA") })
            IndicatorChip("BB(${s.bollingerPeriod})", state.showBollinger, AccentGold,
                onToggle = { viewModel.toggleBollinger() }, onLongPress = { viewModel.openSettings("BB") })
            IndicatorChip("VWAP", state.showVwap, Color(0xFF00E5FF),
                onToggle = { viewModel.toggleVwap() })
            IndicatorChip("Supertrend", state.showSupertrend, Green400,
                onToggle = { viewModel.toggleSupertrend() }, onLongPress = { viewModel.openSettings("ST") })
            IndicatorChip("Ichimoku", state.showIchimoku, Color(0xFF2196F3),
                onToggle = { viewModel.toggleIchimoku() }, onLongPress = { viewModel.openSettings("ICHI") })
            IndicatorChip("VOL", state.showVolume, TextSecondary,
                onToggle = { viewModel.toggleVolume() })
        }

        // ─── Sub-chart Indicator Toggles ────────────────────────────
        Text("Indicators", fontSize = 10.sp, color = TextTertiary,
            modifier = Modifier.padding(start = 12.dp, top = 2.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IndicatorChip("RSI(${s.rsiPeriod})", state.showRsi, AccentOrange,
                onToggle = { viewModel.toggleRsi() }, onLongPress = { viewModel.openSettings("RSI") })
            IndicatorChip("MACD", state.showMacd, Green400,
                onToggle = { viewModel.toggleMacd() }, onLongPress = { viewModel.openSettings("MACD") })
            IndicatorChip("Stoch(${s.stochK})", state.showStochastic, Color(0xFFFF9800),
                onToggle = { viewModel.toggleStochastic() }, onLongPress = { viewModel.openSettings("STOCH") })
            IndicatorChip("StochRSI", state.showStochRsi, Color(0xFFFF5722),
                onToggle = { viewModel.toggleStochRsi() }, onLongPress = { viewModel.openSettings("STOCHRSI") })
            IndicatorChip("ATR(${s.atrPeriod})", state.showAtr, Color(0xFF607D8B),
                onToggle = { viewModel.toggleAtr() }, onLongPress = { viewModel.openSettings("ATR") })
            IndicatorChip("ADX(${s.adxPeriod})", state.showAdx, Color(0xFF795548),
                onToggle = { viewModel.toggleAdx() }, onLongPress = { viewModel.openSettings("ADX") })
            IndicatorChip("CCI(${s.cciPeriod})", state.showCci, Color(0xFF009688),
                onToggle = { viewModel.toggleCci() }, onLongPress = { viewModel.openSettings("CCI") })
            IndicatorChip("W%R(${s.williamsRPeriod})", state.showWilliamsR, Color(0xFFF44336),
                onToggle = { viewModel.toggleWilliamsR() }, onLongPress = { viewModel.openSettings("WR") })
            IndicatorChip("OBV", state.showObv, Color(0xFF4CAF50),
                onToggle = { viewModel.toggleObv() })
            IndicatorChip("MFI(${s.mfiPeriod})", state.showMfi, Color(0xFF03A9F4),
                onToggle = { viewModel.toggleMfi() }, onLongPress = { viewModel.openSettings("MFI") })
            IndicatorChip("CMF(${s.cmfPeriod})", state.showCmf, Color(0xFFCDDC39),
                onToggle = { viewModel.toggleCmf() }, onLongPress = { viewModel.openSettings("CMF") })
        }

        // Drawing tools toggle
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { viewModel.toggleDrawingTools() }) {
                Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp),
                    tint = if (state.showDrawingTools) AccentBlue else TextTertiary)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    if (state.showDrawingTools) "Hide Drawing Tools" else "Drawing Tools",
                    fontSize = 11.sp,
                    color = if (state.showDrawingTools) AccentBlue else TextTertiary
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            // Custom indicator editor button
            TextButton(onClick = { viewModel.openIndicatorEditor() }) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp), tint = AccentPurple)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Custom Indicator", fontSize = 11.sp, color = AccentPurple)
            }
        }

        // ─── Replay Mode & Buy/Hold Comparison Controls ────────────
        if (!state.replayMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.backtestResult != null) {
                    // Replay button — only after backtest
                    TextButton(
                        onClick = { viewModel.enterReplayMode() },
                    ) {
                        Icon(Icons.Default.Replay, null, modifier = Modifier.size(16.dp), tint = AccentOrange)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Replay Mode", fontSize = 11.sp, color = AccentOrange)
                    }
                    // Buy & Hold comparison button
                    TextButton(
                        onClick = { viewModel.toggleBuyAndHoldComparison() },
                    ) {
                        Icon(Icons.Default.CompareArrows, null, modifier = Modifier.size(16.dp), tint = AccentBlue)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (state.showBuyAndHoldComparison) "Hide Compare" else "Compare B&H",
                            fontSize = 11.sp, color = AccentBlue
                        )
                    }
                } else {
                    Text("Run a backtest to unlock Replay & Compare", fontSize = 10.sp, color = TextTertiary,
                        modifier = Modifier.padding(horizontal = 8.dp))
                }
            }
        } else {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = DarkCard,
                tonalElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Replay, null, modifier = Modifier.size(16.dp), tint = AccentOrange)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Replay", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AccentOrange)
                        }
                        Text(
                            "Bar ${state.replayBarIndex + 1} / ${state.candles.size}",
                            fontSize = 11.sp, color = TextSecondary
                        )
                        TextButton(onClick = { viewModel.exitReplayMode() }) {
                            Text("Exit", fontSize = 11.sp, color = Red400)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    // Progress slider
                    Slider(
                        value = state.replayBarIndex.toFloat(),
                        onValueChange = { viewModel.seekReplay(it.toInt()) },
                        valueRange = 1f..(state.candles.size - 1).toFloat().coerceAtLeast(2f),
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = AccentOrange,
                            activeTrackColor = AccentOrange,
                            inactiveTrackColor = DarkBorder
                        )
                    )
                    // Playback controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.replayStepBack() }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.SkipPrevious, "Step Back", tint = TextPrimary)
                        }
                        IconButton(
                            onClick = {
                                if (state.replayPlaying) viewModel.replayPause()
                                else viewModel.replayPlay()
                            },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                if (state.replayPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                "Play/Pause",
                                tint = AccentOrange,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        IconButton(onClick = { viewModel.replayStepForward() }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.SkipNext, "Step Forward", tint = TextPrimary)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        // Speed selector
                        val speeds = listOf(1000L to "1x", 500L to "2x", 250L to "4x", 100L to "10x")
                        speeds.forEach { (ms, label) ->
                            val selected = state.replaySpeedMs == ms
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 2.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (selected) AccentOrange.copy(alpha = 0.2f) else Color.Transparent)
                                    .clickable { viewModel.setReplaySpeed(ms) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    label, fontSize = 10.sp,
                                    color = if (selected) AccentOrange else TextTertiary,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                    // Signal count
                    if (state.replaySignals.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        val buys = state.replaySignals.count { it.signalType == SignalType.BUY }
                        val sells = state.replaySignals.count { it.signalType == SignalType.SELL }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("Signals: ", fontSize = 10.sp, color = TextTertiary)
                            Text("$buys BUY", fontSize = 10.sp, color = Green400)
                            Text("  •  ", fontSize = 10.sp, color = TextTertiary)
                            Text("$sells SELL", fontSize = 10.sp, color = Red400)
                        }
                    }
                }
            }
        }

        // ─── Buy & Hold Comparison Chart ────────────────────────────
        if (state.showBuyAndHoldComparison && state.strategyEquityCurve.isNotEmpty() && state.buyAndHoldEquityCurve.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = DarkCard,
                tonalElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("Strategy vs Buy & Hold", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).background(AccentBlue, RoundedCornerShape(2.dp)))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Strategy", fontSize = 10.sp, color = TextSecondary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).background(AccentOrange, RoundedCornerShape(2.dp)))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Buy & Hold", fontSize = 10.sp, color = TextSecondary)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(Color(0xFF0D0D1A), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        val strat = state.strategyEquityCurve
                        val bh = state.buyAndHoldEquityCurve
                        val allVals = strat + bh
                        val minVal = allVals.minOrNull() ?: 0.0
                        val maxVal = allVals.maxOrNull() ?: 1.0
                        val range = (maxVal - minVal).coerceAtLeast(0.001)
                        val w = size.width
                        val h = size.height

                        fun drawCurve(data: List<Double>, color: androidx.compose.ui.graphics.Color) {
                            if (data.size < 2) return
                            val path = androidx.compose.ui.graphics.Path()
                            for (i in data.indices) {
                                val x = w * i / (data.size - 1).coerceAtLeast(1)
                                val y = h - (h * ((data[i] - minVal) / range)).toFloat()
                                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))
                        }
                        drawCurve(strat, androidx.compose.ui.graphics.Color(0xFF4FC3F7))
                        drawCurve(bh, androidx.compose.ui.graphics.Color(0xFFFFA726))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    val stratReturn = if (state.strategyEquityCurve.size >= 2 && state.strategyEquityCurve.first() > 0.0)
                        ((state.strategyEquityCurve.last() - state.strategyEquityCurve.first()) / state.strategyEquityCurve.first() * 100) else 0.0
                    val bhReturn = if (state.buyAndHoldEquityCurve.size >= 2 && state.buyAndHoldEquityCurve.first() > 0.0)
                        ((state.buyAndHoldEquityCurve.last() - state.buyAndHoldEquityCurve.first()) / state.buyAndHoldEquityCurve.first() * 100) else 0.0
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Text("Strategy: ${String.format("%.2f", stratReturn)}%", fontSize = 11.sp, color = if (stratReturn >= 0) Green400 else Red400)
                        Text("B&H: ${String.format("%.2f", bhReturn)}%", fontSize = 11.sp, color = if (bhReturn >= 0) Green400 else Red400)
                        Text(
                            "Alpha: ${String.format("%.2f", stratReturn - bhReturn)}%",
                            fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            color = if (stratReturn >= bhReturn) Green400 else Red400
                        )
                    }
                }
            }
        }

        // ─── User Custom Indicators (toggle/manage) ────────────────
        if (state.userIndicators.isNotEmpty()) {
            Text("Custom Indicators", fontSize = 10.sp, color = TextTertiary,
                modifier = Modifier.padding(start = 12.dp, top = 2.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                state.userIndicators.forEach { ind ->
                    IndicatorChip(
                        label = ind.name,
                        isActive = ind.isEnabled,
                        color = AccentPurple,
                        onToggle = { viewModel.toggleIndicatorEnabled(ind.id) },
                        onLongPress = { viewModel.openIndicatorEditor(ind.id) }
                    )
                }
            }
        }

        // ─── Custom Indicator Editor Dialog ─────────────────────────
        if (state.showIndicatorEditor) {
            CustomIndicatorEditorDialog(
                name = state.indicatorEditorName,
                code = state.indicatorEditorCode,
                isEditing = state.editingIndicatorId != null,
                indicatorError = state.indicatorError,
                showHelp = state.showIndicatorHelp,
                indicatorInputs = state.indicatorInputs,
                indicatorInputValues = state.indicatorInputValues,
                livePreviewEnabled = state.livePreviewEnabled,
                onNameChange = { viewModel.updateIndicatorName(it) },
                onCodeChange = { viewModel.updateIndicatorCode(it) },
                onPreview = { viewModel.previewIndicator() },
                onSave = { viewModel.saveIndicator() },
                onDelete = {
                    state.editingIndicatorId?.let { viewModel.deleteIndicator(it) }
                    viewModel.closeIndicatorEditor()
                },
                onDismiss = { viewModel.closeIndicatorEditor() },
                onLoadTemplate = { viewModel.loadIndicatorTemplate(it) },
                onToggleHelp = { viewModel.toggleIndicatorHelp() },
                onInputValueChange = { name, value -> viewModel.updateInputValue(name, value) },
                onToggleLivePreview = { viewModel.toggleLivePreview() },
                onExport = { viewModel.exportIndicatorJson() },
                onImport = { json -> viewModel.importIndicatorJson(json) }
            )
        }

        // Custom indicators from active script
        if (state.customIndicators.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                state.customIndicators.forEach { ci ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(13.dp))
                            .background(Color(ci.color).copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(ci.name, fontSize = 10.sp, color = Color(ci.color))
                    }
                }
            }
        }

        // ─── Indicator Settings Dialog ──────────────────────────────
        if (state.showSettingsDialog) {
            IndicatorSettingsDialog(
                indicator = state.editingIndicator ?: "",
                settings = state.indicatorSettings,
                onDismiss = { viewModel.closeSettings() },
                onApply = { newSettings ->
                    viewModel.updateSettings(newSettings)
                    viewModel.closeSettings()
                }
            )
        }

        // ─── Live Trading Data ──────────────────────────────────────
        state.ticker?.let { ticker ->
            TfgCard(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem("Price", Formatters.formatPrice(ticker.price))
                    StatItem("24h Change", Formatters.formatPercent(ticker.priceChangePercent))
                    StatItem("Volume", Formatters.formatVolume(ticker.volume))
                }
            }
        }

        if (state.candles.isNotEmpty()) {
            val lastCandle = state.candles.last()
            val high24 = state.candles.takeLast(24).maxOfOrNull { it.high } ?: 0.0
            val low24 = state.candles.takeLast(24).minOfOrNull { it.low } ?: 0.0
            TfgCard(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem("Open", Formatters.formatPrice(lastCandle.open))
                    StatItem("High", Formatters.formatPrice(high24))
                    StatItem("Low", Formatters.formatPrice(low24))
                    StatItem("Close", Formatters.formatPrice(lastCandle.close))
                }
            }
        }

        // ─── Script Panel ────────────────────────────────────────────
        ChartScriptPanel(
            state = state,
            viewModel = viewModel
        )

        } // end scrollable column

        // ─── Trade Button (pinned at bottom) ────────────────────────
        Button(
            onClick = { onTrade(state.symbol) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.SwapVert, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Trade ${state.symbol}", fontWeight = FontWeight.SemiBold)
        }

        state.error?.let {
            ErrorMessage(message = it, modifier = Modifier.padding(8.dp))
        }
    }
}

// ─── Backtest Panel for Chart Page (no editor) ─────────────────────

@Composable
private fun ChartScriptPanel(
    state: CoinDetailUiState,
    viewModel: CoinDetailViewModel
) {
    // Toggle button
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = { viewModel.toggleScriptPanel() }) {
            Icon(
                if (state.showScriptPanel) Icons.Default.ExpandLess else Icons.Default.PlayArrow,
                null, modifier = Modifier.size(18.dp), tint = AccentBlue
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                if (state.showScriptPanel) "Hide Backtest" else "Backtest",
                fontSize = 12.sp, color = AccentBlue
            )
        }
    }

    AnimatedVisibility(visible = state.showScriptPanel) {
        Column(modifier = Modifier.padding(horizontal = 8.dp)) {
            // Template selector
            Text("Strategy", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 4.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                items(state.scriptTemplates) { template ->
                    val isSelected = state.selectedTemplateId == template.id
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) AccentBlue.copy(alpha = 0.2f) else DarkCard,
                        modifier = Modifier.clickable { viewModel.loadScriptTemplate(template) }
                    ) {
                        Text(
                            template.name,
                            fontSize = 11.sp,
                            color = if (isSelected) AccentBlue else TextSecondary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // ─── Strategy Settings (editable params) ─────────
            if (state.scriptParams.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { viewModel.toggleScriptSettings() }) {
                        Icon(
                            Icons.Default.Settings, null,
                            tint = if (state.showScriptSettings) AccentBlue else TextTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (state.showScriptSettings) "Hide Settings" else "Strategy Settings",
                            fontSize = 11.sp, color = if (state.showScriptSettings) AccentBlue else TextTertiary
                        )
                    }
                }
                AnimatedVisibility(visible = state.showScriptSettings) {
                    Column(modifier = Modifier.padding(bottom = 8.dp)) {
                        val paramEntries = state.scriptParams.entries.toList()
                        val rows = paramEntries.chunked(2)
                        rows.forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                row.forEach { (key, value) ->
                                    OutlinedTextField(
                                        value = value,
                                        onValueChange = { viewModel.updateScriptParam(key, it) },
                                        label = {
                                            Text(
                                                key.replace("_", " ").lowercase()
                                                    .replaceFirstChar { it.uppercase() },
                                                fontSize = 9.sp, maxLines = 1
                                            )
                                        },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = AccentBlue,
                                            unfocusedBorderColor = DarkBorder,
                                            cursorColor = AccentBlue,
                                            focusedContainerColor = DarkCard,
                                            unfocusedContainerColor = DarkCard
                                        ),
                                        singleLine = true,
                                        textStyle = androidx.compose.ui.text.TextStyle(
                                            fontSize = 12.sp, color = TextPrimary
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(52.dp)
                                    )
                                }
                                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // Backtest days selector
            Text("Backtest Period", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(3, 7, 30, 90, 180).forEach { d ->
                    val selected = state.backtestDays == d
                    Surface(
                        onClick = { viewModel.updateBacktestDays(d) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (selected) AccentBlue else DarkCard,
                        border = if (selected) null else BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (d < 30) "${d}d" else if (d < 365) "${d}d" else "${d / 365}y",
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) Color.White else TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Backtest button + stop
            if (state.isBacktesting) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                if (state.backtestProgress < 0.3f) "Fetching data…" else "Running backtest…",
                                fontSize = 10.sp, color = TextSecondary
                            )
                            Text(
                                "${(state.backtestProgress * 100).toInt()}%",
                                fontSize = 10.sp, color = AccentBlue, fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = state.backtestProgress,
                            modifier = Modifier.fillMaxWidth(),
                            color = AccentBlue
                        )
                    }
                    TextButton(onClick = { viewModel.cancelBacktest() }) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp), tint = Red400)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Stop", color = Red400, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Button(
                    onClick = { viewModel.runChartBacktest() },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Run Backtest", fontSize = 12.sp)
                }
            }

            // Backtest results inline
            state.backtestResult?.let { result ->
                TfgCard(modifier = Modifier.padding(bottom = 8.dp)) {
                    Column {
                        Text("Backtest Results", fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 13.sp)
                        if (result.backtestDays > 0) {
                            Text("Period: ${result.backtestDays} days  •  ${result.timeframe}", fontSize = 10.sp, color = TextTertiary)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        // ── Virtual entry amount ──
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = if (result.finalAmount >= result.entryAmount) Green400.copy(alpha = 0.1f) else Red400.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Entry", fontSize = 10.sp, color = TextTertiary)
                                Text("$${String.format("%.2f", result.entryAmount)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Text("→", fontSize = 16.sp, color = TextSecondary)
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Final", fontSize = 10.sp, color = TextTertiary)
                                Text(
                                    "$${String.format("%.2f", result.finalAmount)}",
                                    fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                    color = if (result.finalAmount >= result.entryAmount) Green400 else Red400
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("PnL", fontSize = 10.sp, color = TextTertiary)
                                PnlText(value = result.totalPnl, fontSize = 14)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Win Rate", fontSize = 10.sp, color = TextTertiary)
                                Text("${String.format("%.1f", result.winRate)}%", color = TextPrimary, fontSize = 14.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Trades", fontSize = 10.sp, color = TextTertiary)
                                Text("${result.totalTrades}", color = TextPrimary, fontSize = 14.sp)
                            }
                        }
                        if (result.totalTrades > 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Sharpe", fontSize = 10.sp, color = TextTertiary)
                                    Text(String.format("%.2f", result.sharpeRatio), color = TextPrimary, fontSize = 12.sp)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Max DD", fontSize = 10.sp, color = TextTertiary)
                                    Text("${String.format("%.1f", result.maxDrawdown)}%", color = Red400, fontSize = 12.sp)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("P. Factor", fontSize = 10.sp, color = TextTertiary)
                                    Text(String.format("%.2f", result.profitFactor), color = TextPrimary, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // Backtest vs Live comparison
                if (result.equityCurve.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(onClick = { viewModel.toggleBacktestComparison() }) {
                        Icon(Icons.Default.CompareArrows, null, modifier = Modifier.size(14.dp),
                            tint = AccentPurple)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (state.showBacktestComparison) "Hide Comparison" else "Compare vs Buy & Hold",
                            fontSize = 11.sp, color = AccentPurple
                        )
                    }

                    if (state.showBacktestComparison) {
                        BacktestComparisonChart(
                            backtestEquity = result.equityCurve,
                            liveEquity = state.liveEquityCurve,
                            startingCapital = result.startingCapital,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .padding(top = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        // Summary metrics
                        val btReturn = if (result.startingCapital > 0)
                            ((result.equityCurve.lastOrNull() ?: result.startingCapital) - result.startingCapital) / result.startingCapital * 100.0
                        else 0.0
                        val bhReturn = if (state.liveEquityCurve.isNotEmpty() && result.startingCapital > 0)
                            (state.liveEquityCurve.last() - result.startingCapital) / result.startingCapital * 100.0
                        else 0.0
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).background(AccentBlue, RoundedCornerShape(2.dp)))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Strategy", fontSize = 10.sp, color = TextSecondary)
                                }
                                Text(
                                    "${String.format("%+.1f", btReturn)}%",
                                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                                    color = if (btReturn >= 0) Green400 else Red400
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).background(AccentGold, RoundedCornerShape(2.dp)))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Buy & Hold", fontSize = 10.sp, color = TextSecondary)
                                }
                                Text(
                                    "${String.format("%+.1f", bhReturn)}%",
                                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                                    color = if (bhReturn >= 0) Green400 else Red400
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Alpha", fontSize = 10.sp, color = TextSecondary)
                                Text(
                                    "${String.format("%+.1f", btReturn - bhReturn)}%",
                                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                                    color = if (btReturn - bhReturn >= 0) Green400 else Red400
                                )
                            }
                        }
                    }
                }
            }

            // ─── TFG ALGO Dashboard ────────────────────────────────
            state.tfgDashboard?.let { dash ->
                TfgDashboardPanel(dash)
            }
        }
    }
}

// ─── TFG ALGO 10-Factor Dashboard ──────────────────────────────────

@Composable
private fun TfgDashboardPanel(dashboard: TfgDashboardData) {
    var expanded by remember { mutableStateOf(true) }
    TfgCard(modifier = Modifier.padding(vertical = 4.dp)) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("TFG ALGO", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AccentBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (dashboard.inPosition) Green400.copy(alpha = 0.2f) else DarkSurface
                    ) {
                        Text(
                            if (dashboard.inPosition) "IN TRADE" else "WATCHING",
                            fontSize = 9.sp, fontWeight = FontWeight.SemiBold,
                            color = if (dashboard.inPosition) Green400 else TextTertiary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Score badge
                    val scoreColor = when {
                        dashboard.score >= 8 -> Green400
                        dashboard.score >= 6 -> AccentGold
                        else -> Red400
                    }
                    Surface(shape = RoundedCornerShape(6.dp), color = scoreColor.copy(alpha = 0.15f)) {
                        Text(
                            "${dashboard.score}/10",
                            fontSize = 13.sp, fontWeight = FontWeight.Bold, color = scoreColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        null, modifier = Modifier.size(18.dp), tint = TextTertiary
                    )
                }
            }

            // P&L row
            if (dashboard.inPosition) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (dashboard.pnlPct >= 0) Green400.copy(alpha = 0.08f) else Red400.copy(alpha = 0.08f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Unrealized P&L", fontSize = 10.sp, color = TextSecondary)
                    Text(
                        "${String.format("%+.2f", dashboard.pnlPct)}%",
                        fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        color = if (dashboard.pnlPct >= 0) Green400 else Red400
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 6.dp)) {
                    // Table header
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                        Text("Factor", fontSize = 9.sp, color = TextTertiary, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1.4f))
                        Text("", fontSize = 9.sp, modifier = Modifier.weight(0.4f))
                        Text("Status", fontSize = 9.sp, color = TextTertiary, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1.2f))
                    }
                    HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)

                    // 10 confluence rows
                    DashRow("EMA Trend", dashboard.cTrend, if (dashboard.cTrend) "Bullish" else "Bearish")
                    DashRow("VWAP", dashboard.cVwap, if (dashboard.cVwap) "Above" else "Below")
                    DashRow("Volume", dashboard.cVolume, if (dashboard.cVolume) "Spike" else "Normal")
                    DashRow("RSI Momentum", dashboard.cRsi, String.format("%.1f", dashboard.rsiVal))
                    DashRow("MACD", dashboard.cMacd, if (dashboard.cMacd) "Bullish X" else "Bearish")
                    DashRow("Candle Pattern", dashboard.cCandle,
                        when {
                            dashboard.bullEngulf -> "Engulfing"
                            dashboard.bullPinBar -> "Pin Bar"
                            dashboard.strongBull -> "Strong Bull"
                            else -> "None"
                        }
                    )
                    DashRow("S/R Proximity", dashboard.cSupport,
                        if (dashboard.supportLevel != null) String.format("S:%.2f", dashboard.supportLevel) else "—"
                    )
                    DashRow("DI+/DI−", dashboard.cDi,
                        String.format("+%.1f / −%.1f", dashboard.diPlus, dashboard.diMinus)
                    )
                    DashRow("ADX Trend", dashboard.cAdx, String.format("%.1f", dashboard.adxVal))
                    DashRow("Breakout", dashboard.cBreakout, if (dashboard.cBreakout) "Yes" else "No")

                    HorizontalDivider(color = DarkBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 2.dp))

                    // Score row
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Score", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.weight(1.4f))
                        Spacer(modifier = Modifier.weight(0.4f))
                        val readyColor = if (dashboard.score >= 6) Green400 else AccentGold
                        Text(
                            "${dashboard.score}/10 ${if (dashboard.score >= 6) "READY" else "WAIT"}",
                            fontSize = 11.sp, fontWeight = FontWeight.Bold, color = readyColor,
                            modifier = Modifier.weight(1.2f)
                        )
                    }

                    // HTF row
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("HTF Trend", fontSize = 10.sp, color = TextSecondary, modifier = Modifier.weight(1.4f))
                        Spacer(modifier = Modifier.weight(0.4f))
                        Text(
                            if (dashboard.htfBull) "BULL" else "BEAR",
                            fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                            color = if (dashboard.htfBull) Green400 else Red400,
                            modifier = Modifier.weight(1.2f)
                        )
                    }

                    // S/R levels
                    if (dashboard.supportLevel != null || dashboard.resistanceLevel != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("S/R Levels", fontSize = 10.sp, color = TextSecondary, modifier = Modifier.weight(1.4f))
                            Spacer(modifier = Modifier.weight(0.4f))
                            Text(
                                buildString {
                                    dashboard.supportLevel?.let { append("S:${String.format("%.2f", it)}") }
                                    if (dashboard.supportLevel != null && dashboard.resistanceLevel != null) append(" / ")
                                    dashboard.resistanceLevel?.let { append("R:${String.format("%.2f", it)}") }
                                },
                                fontSize = 10.sp, color = AccentGold,
                                modifier = Modifier.weight(1.2f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashRow(label: String, pass: Boolean, status: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 10.sp, color = TextSecondary, modifier = Modifier.weight(1.4f))
        Text(
            if (pass) "✓" else "✗",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (pass) Green400 else Red400,
            modifier = Modifier.weight(0.4f)
        )
        Text(status, fontSize = 10.sp, color = if (pass) Green400 else TextTertiary, modifier = Modifier.weight(1.2f))
    }
}

// ─── Backtest vs Live Comparison Chart ──────────────────────────────

@Composable
private fun BacktestComparisonChart(
    backtestEquity: List<Double>,
    liveEquity: List<Double>,
    startingCapital: Double,
    modifier: Modifier = Modifier
) {
    if (backtestEquity.size < 2) return

    val allValues = backtestEquity + liveEquity
    val minVal = allValues.minOrNull() ?: 0.0
    val maxVal = allValues.maxOrNull() ?: 0.0
    val range = if (maxVal - minVal > 0) maxVal - minVal else 1.0

    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DarkCard)
            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        val w = size.width
        val h = size.height

        // Starting capital reference line
        val baselineY = h - ((startingCapital - minVal) / range * h).toFloat()
        drawLine(
            color = Color(0xFF30363D),
            start = Offset(0f, baselineY),
            end = Offset(w, baselineY),
            strokeWidth = 1f,
            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
        )

        // Draw backtest equity (blue)
        val btPath = Path()
        val btStep = w / (backtestEquity.size - 1)
        backtestEquity.forEachIndexed { i, eq ->
            val x = i * btStep
            val y = h - ((eq - minVal) / range * h).toFloat()
            if (i == 0) btPath.moveTo(x, y) else btPath.lineTo(x, y)
        }
        drawPath(btPath, color = Color(0xFF58A6FF), style = Stroke(width = 2.dp.toPx()))

        // Draw live/buy-hold equity (gold)
        if (liveEquity.size >= 2) {
            val livePath = Path()
            val liveStep = w / (liveEquity.size - 1)
            liveEquity.forEachIndexed { i, eq ->
                val x = i * liveStep
                val y = h - ((eq - minVal) / range * h).toFloat()
                if (i == 0) livePath.moveTo(x, y) else livePath.lineTo(x, y)
            }
            drawPath(livePath, color = Color(0xFFFFD700), style = Stroke(width = 2.dp.toPx()))
        }
    }
}

// ─── Helpers ────────────────────────────────────────────────────────

@Composable
private fun IndicatorChip(
    label: String,
    isActive: Boolean,
    color: Color,
    onToggle: () -> Unit,
    onLongPress: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(13.dp))
            .background(if (isActive) color.copy(alpha = 0.2f) else DarkCard)
            .border(
                width = 1.dp,
                color = if (isActive) color.copy(alpha = 0.5f) else DarkBorder,
                shape = RoundedCornerShape(13.dp)
            )
            .clickable { onToggle() }
            .padding(start = 8.dp, end = if (onLongPress != null) 4.dp else 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 10.sp, color = if (isActive) color else TextSecondary)
        if (onLongPress != null) {
            Icon(
                Icons.Default.Settings,
                contentDescription = "Settings",
                modifier = Modifier
                    .size(14.dp)
                    .padding(start = 2.dp)
                    .clickable { onLongPress() },
                tint = if (isActive) color.copy(alpha = 0.7f) else TextTertiary
            )
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 10.sp, color = TextTertiary)
        Text(value, fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
    }
}

// ─── Indicator Settings Dialog (Full) ───────────────────────────────

@Composable
private fun IndicatorSettingsDialog(
    indicator: String,
    settings: IndicatorSettings,
    onDismiss: () -> Unit,
    onApply: (IndicatorSettings) -> Unit
) {
    var smaPeriod by remember { mutableStateOf(settings.smaPeriod.toString()) }
    var emaPeriod by remember { mutableStateOf(settings.emaPeriod.toString()) }
    var wmaPeriod by remember { mutableStateOf(settings.wmaPeriod.toString()) }
    var vwmaPeriod by remember { mutableStateOf(settings.vwmaPeriod.toString()) }
    var hullPeriod by remember { mutableStateOf(settings.hullMaPeriod.toString()) }
    var demaPeriod by remember { mutableStateOf(settings.demaPeriod.toString()) }
    var temaPeriod by remember { mutableStateOf(settings.temaPeriod.toString()) }
    var rsiPeriod by remember { mutableStateOf(settings.rsiPeriod.toString()) }
    var bbPeriod by remember { mutableStateOf(settings.bollingerPeriod.toString()) }
    var bbMult by remember { mutableStateOf(settings.bollingerMult.toString()) }
    var macdFast by remember { mutableStateOf(settings.macdFast.toString()) }
    var macdSlow by remember { mutableStateOf(settings.macdSlow.toString()) }
    var macdSignal by remember { mutableStateOf(settings.macdSignal.toString()) }
    var stPeriod by remember { mutableStateOf(settings.supertrendPeriod.toString()) }
    var stMult by remember { mutableStateOf(settings.supertrendMult.toString()) }
    var ichiTenkan by remember { mutableStateOf(settings.ichimokuTenkan.toString()) }
    var ichiKijun by remember { mutableStateOf(settings.ichimokuKijun.toString()) }
    var ichiSenkouB by remember { mutableStateOf(settings.ichimokuSenkouB.toString()) }
    var stochK by remember { mutableStateOf(settings.stochK.toString()) }
    var stochD by remember { mutableStateOf(settings.stochD.toString()) }
    var srsiPeriod by remember { mutableStateOf(settings.stochRsiPeriod.toString()) }
    var srsiStoch by remember { mutableStateOf(settings.stochRsiStoch.toString()) }
    var srsiK by remember { mutableStateOf(settings.stochRsiK.toString()) }
    var srsiD by remember { mutableStateOf(settings.stochRsiD.toString()) }
    var atrPeriod by remember { mutableStateOf(settings.atrPeriod.toString()) }
    var adxPeriod by remember { mutableStateOf(settings.adxPeriod.toString()) }
    var cciPeriod by remember { mutableStateOf(settings.cciPeriod.toString()) }
    var wrPeriod by remember { mutableStateOf(settings.williamsRPeriod.toString()) }
    var mfiPeriod by remember { mutableStateOf(settings.mfiPeriod.toString()) }
    var cmfPeriod by remember { mutableStateOf(settings.cmfPeriod.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = { Text("$indicator Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (indicator) {
                    "SMA" -> SettingsField("Period", smaPeriod) { smaPeriod = it }
                    "EMA" -> SettingsField("Period", emaPeriod) { emaPeriod = it }
                    "WMA" -> SettingsField("Period", wmaPeriod) { wmaPeriod = it }
                    "VWMA" -> SettingsField("Period", vwmaPeriod) { vwmaPeriod = it }
                    "HULL" -> SettingsField("Period", hullPeriod) { hullPeriod = it }
                    "DEMA" -> SettingsField("Period", demaPeriod) { demaPeriod = it }
                    "TEMA" -> SettingsField("Period", temaPeriod) { temaPeriod = it }
                    "RSI" -> SettingsField("Period", rsiPeriod) { rsiPeriod = it }
                    "BB" -> {
                        SettingsField("Period", bbPeriod) { bbPeriod = it }
                        SettingsField("Multiplier", bbMult) { bbMult = it }
                    }
                    "MACD" -> {
                        SettingsField("Fast", macdFast) { macdFast = it }
                        SettingsField("Slow", macdSlow) { macdSlow = it }
                        SettingsField("Signal", macdSignal) { macdSignal = it }
                    }
                    "ST" -> {
                        SettingsField("Period", stPeriod) { stPeriod = it }
                        SettingsField("Multiplier", stMult) { stMult = it }
                    }
                    "ICHI" -> {
                        SettingsField("Tenkan", ichiTenkan) { ichiTenkan = it }
                        SettingsField("Kijun", ichiKijun) { ichiKijun = it }
                        SettingsField("Senkou B", ichiSenkouB) { ichiSenkouB = it }
                    }
                    "STOCH" -> {
                        SettingsField("K Period", stochK) { stochK = it }
                        SettingsField("D Period", stochD) { stochD = it }
                    }
                    "STOCHRSI" -> {
                        SettingsField("RSI Period", srsiPeriod) { srsiPeriod = it }
                        SettingsField("Stoch Period", srsiStoch) { srsiStoch = it }
                        SettingsField("K Smooth", srsiK) { srsiK = it }
                        SettingsField("D Smooth", srsiD) { srsiD = it }
                    }
                    "ATR" -> SettingsField("Period", atrPeriod) { atrPeriod = it }
                    "ADX" -> SettingsField("Period", adxPeriod) { adxPeriod = it }
                    "CCI" -> SettingsField("Period", cciPeriod) { cciPeriod = it }
                    "WR" -> SettingsField("Period", wrPeriod) { wrPeriod = it }
                    "MFI" -> SettingsField("Period", mfiPeriod) { mfiPeriod = it }
                    "CMF" -> SettingsField("Period", cmfPeriod) { cmfPeriod = it }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val newSettings = settings.copy(
                    smaPeriod = smaPeriod.toIntOrNull()?.coerceIn(2, 500) ?: settings.smaPeriod,
                    emaPeriod = emaPeriod.toIntOrNull()?.coerceIn(2, 500) ?: settings.emaPeriod,
                    wmaPeriod = wmaPeriod.toIntOrNull()?.coerceIn(2, 500) ?: settings.wmaPeriod,
                    vwmaPeriod = vwmaPeriod.toIntOrNull()?.coerceIn(2, 500) ?: settings.vwmaPeriod,
                    hullMaPeriod = hullPeriod.toIntOrNull()?.coerceIn(2, 500) ?: settings.hullMaPeriod,
                    demaPeriod = demaPeriod.toIntOrNull()?.coerceIn(2, 500) ?: settings.demaPeriod,
                    temaPeriod = temaPeriod.toIntOrNull()?.coerceIn(2, 500) ?: settings.temaPeriod,
                    rsiPeriod = rsiPeriod.toIntOrNull()?.coerceIn(2, 200) ?: settings.rsiPeriod,
                    bollingerPeriod = bbPeriod.toIntOrNull()?.coerceIn(2, 500) ?: settings.bollingerPeriod,
                    bollingerMult = bbMult.toDoubleOrNull()?.coerceIn(0.5, 5.0) ?: settings.bollingerMult,
                    macdFast = macdFast.toIntOrNull()?.coerceIn(2, 100) ?: settings.macdFast,
                    macdSlow = macdSlow.toIntOrNull()?.coerceIn(2, 200) ?: settings.macdSlow,
                    macdSignal = macdSignal.toIntOrNull()?.coerceIn(2, 100) ?: settings.macdSignal,
                    supertrendPeriod = stPeriod.toIntOrNull()?.coerceIn(2, 100) ?: settings.supertrendPeriod,
                    supertrendMult = stMult.toDoubleOrNull()?.coerceIn(0.5, 10.0) ?: settings.supertrendMult,
                    ichimokuTenkan = ichiTenkan.toIntOrNull()?.coerceIn(2, 100) ?: settings.ichimokuTenkan,
                    ichimokuKijun = ichiKijun.toIntOrNull()?.coerceIn(2, 100) ?: settings.ichimokuKijun,
                    ichimokuSenkouB = ichiSenkouB.toIntOrNull()?.coerceIn(2, 200) ?: settings.ichimokuSenkouB,
                    stochK = stochK.toIntOrNull()?.coerceIn(2, 100) ?: settings.stochK,
                    stochD = stochD.toIntOrNull()?.coerceIn(2, 100) ?: settings.stochD,
                    stochRsiPeriod = srsiPeriod.toIntOrNull()?.coerceIn(2, 100) ?: settings.stochRsiPeriod,
                    stochRsiStoch = srsiStoch.toIntOrNull()?.coerceIn(2, 100) ?: settings.stochRsiStoch,
                    stochRsiK = srsiK.toIntOrNull()?.coerceIn(1, 50) ?: settings.stochRsiK,
                    stochRsiD = srsiD.toIntOrNull()?.coerceIn(1, 50) ?: settings.stochRsiD,
                    atrPeriod = atrPeriod.toIntOrNull()?.coerceIn(2, 200) ?: settings.atrPeriod,
                    adxPeriod = adxPeriod.toIntOrNull()?.coerceIn(2, 200) ?: settings.adxPeriod,
                    cciPeriod = cciPeriod.toIntOrNull()?.coerceIn(2, 200) ?: settings.cciPeriod,
                    williamsRPeriod = wrPeriod.toIntOrNull()?.coerceIn(2, 200) ?: settings.williamsRPeriod,
                    mfiPeriod = mfiPeriod.toIntOrNull()?.coerceIn(2, 200) ?: settings.mfiPeriod,
                    cmfPeriod = cmfPeriod.toIntOrNull()?.coerceIn(2, 200) ?: settings.cmfPeriod
                )
                onApply(newSettings)
            }) { Text("Apply", color = AccentBlue) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

@Composable
private fun SettingsField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AccentBlue,
            unfocusedBorderColor = DarkBorder,
            focusedLabelColor = AccentBlue,
            unfocusedLabelColor = TextTertiary,
            cursorColor = AccentBlue,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
        )
    )
}

// ─── Indicator Data Builders ────────────────────────────────────────

private fun buildOverlayList(
    candles: List<Candle>,
    state: CoinDetailUiState,
    s: IndicatorSettings
): List<OverlayData> {
    val list = mutableListOf<OverlayData>()
    fun addOverlay(show: Boolean, name: String, color: String, series: List<Double?>, lineWidth: Int = 2, lineStyle: Int = 0) {
        if (!show) return
        val data = series.mapIndexedNotNull { i, v ->
            if (v == null) null else TimeValue(candles[i].openTime, v)
        }
        if (data.isNotEmpty()) list.add(OverlayData(name, data, color, lineWidth, lineStyle))
    }

    addOverlay(state.showSma, "SMA", "#58A6FF", StrategyEvaluator.smaSeries(candles, s.smaPeriod))
    addOverlay(state.showEma, "EMA", "#BC8CFF", StrategyEvaluator.emaSeries(candles, s.emaPeriod))
    addOverlay(state.showWma, "WMA", "#00BCD4", StrategyEvaluator.wmaSeries(candles, s.wmaPeriod))
    addOverlay(state.showVwma, "VWMA", "#8BC34A", StrategyEvaluator.vwmaSeries(candles, s.vwmaPeriod))
    addOverlay(state.showHullMa, "Hull MA", "#E91E63", StrategyEvaluator.hullMaSeries(candles, s.hullMaPeriod))
    addOverlay(state.showDema, "DEMA", "#9C27B0", StrategyEvaluator.demaSeries(candles, s.demaPeriod))
    addOverlay(state.showTema, "TEMA", "#3F51B5", StrategyEvaluator.temaSeries(candles, s.temaPeriod))
    addOverlay(state.showVwap, "VWAP", "#00E5FF", StrategyEvaluator.vwapSeries(candles), lineStyle = 2)

    return list
}

private fun buildCloudList(
    candles: List<Candle>,
    state: CoinDetailUiState,
    s: IndicatorSettings
): List<CloudOverlayData> {
    val list = mutableListOf<CloudOverlayData>()
    if (state.showBollinger) {
        val (upper, mid, lower) = StrategyEvaluator.bollingerSeries(candles, s.bollingerPeriod, s.bollingerMult)
        val u = upper.mapIndexedNotNull { i, v -> if (v != null) TimeValue(candles[i].openTime, v) else null }
        val l = lower.mapIndexedNotNull { i, v -> if (v != null) TimeValue(candles[i].openTime, v) else null }
        if (u.isNotEmpty()) list.add(CloudOverlayData("BB", u, l, "#FFD700"))
    }
    return list
}

private fun buildSupertrendData(
    candles: List<Candle>,
    s: IndicatorSettings
): SupertrendData? {
    val (green, red) = StrategyEvaluator.supertrendSeries(candles, s.supertrendPeriod, s.supertrendMult)
    val gl = green.mapIndexedNotNull { i, v -> if (v != null) TimeValue(candles[i].openTime, v) else null }
    val rl = red.mapIndexedNotNull { i, v -> if (v != null) TimeValue(candles[i].openTime, v) else null }
    return if (gl.isNotEmpty() || rl.isNotEmpty()) SupertrendData(gl, rl) else null
}

private fun buildIchimokuData(
    candles: List<Candle>,
    s: IndicatorSettings
): IchimokuFullData? {
    val ichi = StrategyEvaluator.ichimokuSeries(candles, s.ichimokuTenkan, s.ichimokuKijun, s.ichimokuSenkouB)
    fun toTV(series: List<Double?>): List<TimeValue> =
        series.mapIndexedNotNull { i, v -> if (v != null) TimeValue(candles[i].openTime, v) else null }
    val t = toTV(ichi.tenkan); val k = toTV(ichi.kijun)
    val sa = toTV(ichi.senkouA); val sb = toTV(ichi.senkouB); val c = toTV(ichi.chikou)
    return if (t.isNotEmpty()) IchimokuFullData(t, k, sa, sb, c) else null
}

private fun buildSubIndicatorList(
    candles: List<Candle>,
    state: CoinDetailUiState,
    s: IndicatorSettings
): List<SubIndicatorData> {
    val list = mutableListOf<SubIndicatorData>()
    val n = candles.size
    var scaleIdx = 0
    fun nextScale(): Pair<Float, Float> {
        val top = 0.82f - scaleIdx * 0.15f
        scaleIdx++
        return Pair(top.coerceAtLeast(0.1f), (top - 0.12f).coerceAtLeast(0.0f))
    }

    if (state.showRsi) {
        val (top, bot) = nextScale()
        val rsi = StrategyEvaluator.rsiSeries(candles, s.rsiPeriod)
        val data = rsi.mapIndexed { i, v -> TimeValue(candles[i].openTime, v) }
        list.add(SubIndicatorData("RSI", data, "#F0883E", "line", top, bot))
    }

    if (state.showMacd) {
        val (top, bot) = nextScale()
        val (macdLine, signalLine, hist) = StrategyEvaluator.macdSeries(candles, s.macdFast, s.macdSlow, s.macdSignal)
        val mainData = macdLine.mapIndexedNotNull { i, v -> if (v != null) TimeValue(candles[i].openTime, v) else null }
        val sigData = signalLine.mapIndexedNotNull { i, v -> if (v != null) TimeValue(candles[i].openTime, v) else null }
        val histData = hist.mapIndexedNotNull { i, v -> if (v != null) TimeValue(candles[i].openTime, v) else null }
        list.add(SubIndicatorData("MACD", mainData, "#26A69A", "line", top, bot,
            extraLines = listOf(Triple("Signal", sigData, "#EF5350"))))
    }

    if (state.showStochastic) {
        val (top, bot) = nextScale()
        val (kLine, dLine) = StrategyEvaluator.stochasticSeries(candles, s.stochK, s.stochD)
        val kData = kLine.mapIndexedNotNull { i, v -> if (v != null) TimeValue(candles[i].openTime, v) else null }
        val dData = dLine.mapIndexedNotNull { i, v -> if (v != null) TimeValue(candles[i].openTime, v) else null }
        list.add(SubIndicatorData("Stoch", kData, "#FF9800", "line", top, bot,
            extraLines = listOf(Triple("Stoch D", dData, "#2196F3"))))
    }

    if (state.showStochRsi) {
        val (top, bot) = nextScale()
        val (kLine, dLine) = StrategyEvaluator.stochasticRsiSeries(candles, s.stochRsiPeriod, s.stochRsiStoch, s.stochRsiK, s.stochRsiD)
        val kData = kLine.mapIndexedNotNull { i, v -> if (v != null) TimeValue(candles[i].openTime, v) else null }
        val dData = dLine.mapIndexedNotNull { i, v -> if (v != null) TimeValue(candles[i].openTime, v) else null }
        list.add(SubIndicatorData("StochRSI", kData, "#FF5722", "line", top, bot,
            extraLines = listOf(Triple("StochRSI D", dData, "#00BCD4"))))
    }

    if (state.showAtr) {
        val (top, bot) = nextScale()
        val atr = StrategyEvaluator.atrSeries(candles, s.atrPeriod)
        val data = atr.mapIndexedNotNull { i, v -> if (v != null) TimeValue(candles[i].openTime, v) else null }
        list.add(SubIndicatorData("ATR", data, "#607D8B", "line", top, bot))
    }

    if (state.showAdx) {
        val (top, bot) = nextScale()
        val adx = StrategyEvaluator.adxSeries(candles, s.adxPeriod)
        val data = adx.mapIndexedNotNull { i, v -> if (v != null) TimeValue(candles[i].openTime, v) else null }
        list.add(SubIndicatorData("ADX", data, "#795548", "line", top, bot))
    }

    if (state.showCci) {
        val (top, bot) = nextScale()
        val cci = StrategyEvaluator.cciSeries(candles, s.cciPeriod)
        val data = cci.mapIndexedNotNull { i, v -> if (v != null) TimeValue(candles[i].openTime, v) else null }
        list.add(SubIndicatorData("CCI", data, "#009688", "line", top, bot))
    }

    if (state.showWilliamsR) {
        val (top, bot) = nextScale()
        val wr = StrategyEvaluator.williamsRSeries(candles, s.williamsRPeriod)
        val data = wr.mapIndexedNotNull { i, v -> if (v != null) TimeValue(candles[i].openTime, v) else null }
        list.add(SubIndicatorData("W%R", data, "#F44336", "line", top, bot))
    }

    if (state.showObv) {
        val (top, bot) = nextScale()
        val obv = StrategyEvaluator.obvSeries(candles)
        val data = obv.mapIndexed { i, v -> TimeValue(candles[i].openTime, v) }
        list.add(SubIndicatorData("OBV", data, "#4CAF50", "line", top, bot))
    }

    if (state.showMfi) {
        val (top, bot) = nextScale()
        val mfi = StrategyEvaluator.mfiSeries(candles, s.mfiPeriod)
        val data = mfi.mapIndexedNotNull { i, v -> if (v != null) TimeValue(candles[i].openTime, v) else null }
        list.add(SubIndicatorData("MFI", data, "#03A9F4", "line", top, bot))
    }

    if (state.showCmf) {
        val (top, bot) = nextScale()
        val cmf = StrategyEvaluator.cmfSeries(candles, s.cmfPeriod)
        val data = cmf.mapIndexedNotNull { i, v -> if (v != null) TimeValue(candles[i].openTime, v) else null }
        list.add(SubIndicatorData("CMF", data, "#CDDC39", "line", top, bot))
    }

    return list
}

// ─── Custom Indicator Editor Dialog ─────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomIndicatorEditorDialog(
    name: String,
    code: String,
    isEditing: Boolean,
    indicatorError: String?,
    showHelp: Boolean,
    indicatorInputs: List<IndicatorInputDef>,
    indicatorInputValues: Map<String, String>,
    livePreviewEnabled: Boolean,
    onNameChange: (String) -> Unit,
    onCodeChange: (String) -> Unit,
    onPreview: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    onLoadTemplate: (IndicatorTemplateItem) -> Unit,
    onToggleHelp: () -> Unit,
    onInputValueChange: (String, String) -> Unit,
    onToggleLivePreview: () -> Unit,
    onExport: () -> String,
    onImport: (String) -> Boolean
) {
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var showImportField by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(16.dp),
            color = DarkSurface,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ── Header ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkCard)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Create, null, tint = AccentPurple, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isEditing) "Edit Indicator" else "New Custom Indicator",
                        color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onToggleHelp, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Info, "Help",
                            tint = if (showHelp) AccentBlue else TextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, "Close", tint = TextTertiary, modifier = Modifier.size(18.dp))
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // ── Name field ──
                    OutlinedTextField(
                        value = name,
                        onValueChange = onNameChange,
                        label = { Text("Indicator Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentPurple,
                            unfocusedBorderColor = DarkBorder,
                            focusedLabelColor = AccentPurple,
                            cursorColor = AccentPurple
                        )
                    )

                    // ── Template Selector ──
                    Text("Templates", fontSize = 11.sp, color = TextTertiary, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        INDICATOR_TEMPLATES.forEach { tpl ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (name == tpl.name) AccentPurple.copy(alpha = 0.2f) else DarkCard,
                                modifier = Modifier.clickable { onLoadTemplate(tpl) }
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                    Text(tpl.name, fontSize = 11.sp,
                                        color = if (name == tpl.name) AccentPurple else TextSecondary)
                                    Text(tpl.description, fontSize = 8.sp, color = TextTertiary, maxLines = 1)
                                }
                            }
                        }
                    }

                    // ── Input Parameters UI (auto-extracted from input() calls) ──
                    if (indicatorInputs.isNotEmpty()) {
                        Text("Parameters", fontSize = 11.sp, color = TextTertiary, fontWeight = FontWeight.SemiBold)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DarkCard,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                indicatorInputs.forEach { input ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            input.name,
                                            fontSize = 11.sp,
                                            color = TextSecondary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = indicatorInputValues[input.name] ?: input.defaultValue,
                                            onValueChange = { onInputValueChange(input.name, it) },
                                            singleLine = true,
                                            modifier = Modifier.width(100.dp),
                                            textStyle = androidx.compose.ui.text.TextStyle(
                                                fontSize = 11.sp,
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                color = TextPrimary
                                            ),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = AccentPurple,
                                                unfocusedBorderColor = DarkBorder,
                                                cursorColor = AccentPurple,
                                                focusedContainerColor = Color(0xFF0D1117),
                                                unfocusedContainerColor = Color(0xFF0D1117)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── Help / Documentation ──
                    AnimatedVisibility(visible = showHelp) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DarkCard,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("API Documentation", fontSize = 12.sp, color = AccentBlue, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Your code must define: function indicator(candles) { ... }\n" +
                                    "Return an object with: { overlays, panels, signals, fills, hlines, bgcolor, labels }\n\n" +
                                    "Each candle: { openTime, open, high, low, close, volume }\n\n" +
                                    "━━ OVERLAY SERIES (plotted on price chart) ━━\n" +
                                    "{ name: \"label\", values: [...], color: \"#hex\", lineWidth: 2, lineStyle: 0 }\n" +
                                    "  lineStyle: 0=solid, 1=dotted, 2=dashed\n\n" +
                                    "━━ PANEL SERIES (separate sub-pane below chart) ━━\n" +
                                    "{ name: \"RSI\", values: [...], color: \"#hex\", type: \"line\",\n" +
                                    "  lines: [{ value: 70, color: \"#red\" }],\n" +
                                    "  extraLines: [{ name: \"signal\", values: [...], color: \"#hex\" }] }\n" +
                                    "  type: \"line\" or \"histogram\"\n\n" +
                                    "━━ SIGNALS (visual markers on chart) ━━\n" +
                                    "{ type: \"BUY\"|\"SELL\", index: candleIndex, price: value }\n\n" +
                                    "━━ FILLS (shade between two overlays) ━━\n" +
                                    "{ line1: \"name1\", line2: \"name2\", color: \"rgba(88,166,255,0.2)\" }\n\n" +
                                    "━━ HLINES (horizontal reference lines) ━━\n" +
                                    "{ price: 50000, color: \"#FFD700\", lineStyle: 2, title: \"Target\" }\n\n" +
                                    "━━ BGCOLOR (background color bars) ━━\n" +
                                    "{ index: candleIndex, color: \"rgba(255,0,0,0.2)\" }\n\n" +
                                    "━━ LABELS (text at candle positions) ━━\n" +
                                    "{ index: candleIndex, text: \"High\", position: \"above\", color: \"#FFF\" }\n\n" +
                                    "━━ INPUT PARAMETERS ━━\n" +
                                    "var period = input(\"Period\", 14);  // creates editable setting\n" +
                                    "var mult = input(\"Multiplier\", 2.0);\n\n" +
                                    "━━ DATA SOURCE HELPERS ━━\n" +
                                    "source(candles, \"close\")  → [close prices]\n" +
                                    "  Fields: open, high, low, close, volume, hl2, hlc3, ohlc4\n\n" +
                                    "━━ AVAILABLE SERIES FUNCTIONS ━━\n" +
                                    "smaSeries, emaSeries, wmaSeries, vwmaSeries, hmaSeries,\n" +
                                    "demaSeries, temaSeries, rsiSeries, atrSeries,\n" +
                                    "bollingerSeries, macdSeries, stochasticSeries,\n" +
                                    "obvSeries, vwapSeries, adxSeries, cciSeries,\n" +
                                    "mfiSeries, williamsRSeries, cmfSeries,\n" +
                                    "supertrendSeries, ichimokuSeries, stochasticRsiSeries\n\n" +
                                    "━━ MATH & UTILITY ━━\n" +
                                    "crossover(a, b, i), crossunder(a, b, i)\n" +
                                    "highest(arr, period, i), lowest(arr, period, i)\n" +
                                    "change(arr, i), linreg(arr, period, i)\n" +
                                    "linregSeries(arr, period)\n" +
                                    "pivotHigh(candles, left, right), pivotLow(...)\n\n" +
                                    "━━ COLOR HELPERS ━━\n" +
                                    "color.red, color.green, color.blue, ...\n" +
                                    "color.rgba(r, g, b, a), color.hexAlpha(\"#hex\", a)",
                                    fontSize = 10.sp,
                                    color = TextSecondary,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }

                    // ── Code Editor ──
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("JavaScript Code", fontSize = 11.sp, color = TextTertiary,
                            fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        // Live preview toggle
                        Text("Live", fontSize = 10.sp,
                            color = if (livePreviewEnabled) AccentPurple else TextTertiary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Switch(
                            checked = livePreviewEnabled,
                            onCheckedChange = { onToggleLivePreview() },
                            modifier = Modifier.height(20.dp),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AccentPurple,
                                checkedTrackColor = AccentPurple.copy(alpha = 0.3f)
                            )
                        )
                    }

                    OutlinedTextField(
                        value = code,
                        onValueChange = onCodeChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 250.dp, max = 450.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = TextPrimary,
                            lineHeight = 16.sp
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentPurple,
                            unfocusedBorderColor = DarkBorder,
                            cursorColor = AccentPurple,
                            focusedContainerColor = Color(0xFF0D1117),
                            unfocusedContainerColor = Color(0xFF0D1117)
                        )
                    )

                    // ── Error Display with line number highlighting ──
                    if (indicatorError != null) {
                        val errorLine = remember(indicatorError) {
                            Regex("""line (\d+)""", RegexOption.IGNORE_CASE)
                                .find(indicatorError)?.groupValues?.get(1)?.toIntOrNull()
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Red400.copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, null, tint = Red400, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    if (errorLine != null) {
                                        Text(
                                            "Error on line $errorLine",
                                            color = Red400,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        indicatorError,
                                        color = Red400,
                                        fontSize = 10.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        maxLines = 5
                                    )
                                }
                            }
                        }
                    }

                    // ── Export / Import ──
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val json = onExport()
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(json))
                            },
                            border = BorderStroke(1.dp, DarkBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export", color = TextSecondary, fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { showImportField = !showImportField },
                            border = BorderStroke(1.dp, DarkBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Import", color = TextSecondary, fontSize = 11.sp)
                        }
                    }

                    AnimatedVisibility(visible = showImportField) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedTextField(
                                value = importText,
                                onValueChange = { importText = it },
                                label = { Text("Paste JSON") },
                                modifier = Modifier.fillMaxWidth().heightIn(max = 120.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = TextPrimary
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentBlue,
                                    unfocusedBorderColor = DarkBorder,
                                    cursorColor = AccentBlue
                                )
                            )
                            Button(
                                onClick = {
                                    if (onImport(importText)) {
                                        showImportField = false
                                        importText = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Load", fontSize = 11.sp)
                            }
                        }
                    }
                }

                // ── Bottom Action Bar ──
                Divider(color = DarkBorder)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkCard)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isEditing) {
                        TextButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, null, tint = Red400, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete", color = Red400, fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    OutlinedButton(
                        onClick = onPreview,
                        border = BorderStroke(1.dp, AccentPurple.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Visibility, null, tint = AccentPurple, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Preview", color = AccentPurple, fontSize = 12.sp)
                    }
                    Button(
                        onClick = onSave,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                    ) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
