package com.tfg.feature.script

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.tfg.core.ui.components.*
import com.tfg.core.ui.theme.*
import com.tfg.domain.model.Candle
import com.tfg.domain.model.CustomTemplate
import com.tfg.domain.model.Script
import com.tfg.domain.model.SignalMarker
import com.tfg.domain.model.SignalType
import com.tfg.feature.chart.TradingViewChart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptScreen(viewModel: ScriptViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    var showSaveDialog by remember { mutableStateOf(false) }
    var showBacktestDialog by remember { mutableStateOf(false) }
    var showTemplates by remember { mutableStateOf(false) }
    var showPairPicker by remember { mutableStateOf(false) }
    var showSaveTemplateDialog by remember { mutableStateOf(false) }
    var currentTab by remember { mutableIntStateOf(0) } // 0=Editor, 1=Templates, 2=My Strategies, 3=Docs, 4=Visual Builder

    // C5: Share backtest CSV when ready
    LaunchedEffect(state.backtestCsvContent) {
        val csv = state.backtestCsvContent ?: return@LaunchedEffect
        val file = java.io.File(context.cacheDir, "backtest_report.csv")
        file.writeText(csv)
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(shareIntent, "Export Backtest"))
        viewModel.clearBacktestCsv()
    }

    // Save dialog
    if (showSaveDialog) {
        var name by remember { mutableStateOf(state.selectedScript?.name ?: "") }
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Strategy", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Strategy Name") },
                    placeholder = { Text("e.g. My SMA Strategy") },
                    colors = com.tfg.core.ui.tfgTextFieldColors(),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (name.isNotBlank() && !state.isSaving) {
                            viewModel.saveScript(name.trim())
                            showSaveDialog = false
                        }
                    },
                    enabled = !state.isSaving
                ) { Text(if (state.isSaving) "Saving..." else "Save", color = if (state.isSaving) TextTertiary else AccentBlue) }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Cancel", color = TextSecondary) }
            },
            containerColor = DarkSurface
        )
    }

    // Backtest dialog
    if (showBacktestDialog) {
        var symbol by remember { mutableStateOf(state.selectedPair) }
        var interval by remember { mutableStateOf(state.backtestInterval) }
        var days by remember { mutableStateOf("30") }
        var expandedPair by remember { mutableStateOf(false) }
        var expandedInterval by remember { mutableStateOf(false) }
        var useDateRange by remember { mutableStateOf(state.backtestStartDateMs != null) }
        var useMultiSymbol by remember { mutableStateOf(state.backtestSymbols.isNotEmpty()) }
        var multiSymbolText by remember { mutableStateOf(state.backtestSymbols.joinToString(",").ifEmpty { "BTCUSDT,ETHUSDT" }) }
        // Date range state — default to last 30 days
        var startDateMs by remember { mutableStateOf(state.backtestStartDateMs ?: (System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000)) }
        var endDateMs by remember { mutableStateOf(state.backtestEndDateMs ?: System.currentTimeMillis()) }
        val dateFormat = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US) }
        AlertDialog(
            onDismissRequest = { showBacktestDialog = false },
            title = { Text("Backtest Configuration", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // ── Pair selector ──
                    Text("Trading Pair", fontSize = 12.sp, color = TextSecondary)
                    ExposedDropdownMenuBox(
                        expanded = expandedPair,
                        onExpandedChange = { expandedPair = it }
                    ) {
                        OutlinedTextField(
                            value = symbol,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPair) },
                            colors = com.tfg.core.ui.tfgTextFieldColors(),
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = expandedPair,
                            onDismissRequest = { expandedPair = false }
                        ) {
                            POPULAR_PAIRS.forEach { pair ->
                                DropdownMenuItem(
                                    text = { Text(pair) },
                                    onClick = {
                                        symbol = pair
                                        expandedPair = false
                                    }
                                )
                            }
                        }
                    }

                    // ── C1: Multi-symbol toggle ──
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Multi-symbol", fontSize = 12.sp, color = TextSecondary)
                        Switch(checked = useMultiSymbol, onCheckedChange = { useMultiSymbol = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = AccentBlue))
                    }
                    if (useMultiSymbol) {
                        OutlinedTextField(
                            value = multiSymbolText,
                            onValueChange = { multiSymbolText = it.uppercase().filter { c -> c.isLetter() || c == ',' } },
                            label = { Text("Symbols (comma-separated)") },
                            colors = com.tfg.core.ui.tfgTextFieldColors(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // ── Timeframe selector ──
                    Text("Timeframe", fontSize = 12.sp, color = TextSecondary)
                    ExposedDropdownMenuBox(
                        expanded = expandedInterval,
                        onExpandedChange = { expandedInterval = it }
                    ) {
                        OutlinedTextField(
                            value = interval,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedInterval) },
                            colors = com.tfg.core.ui.tfgTextFieldColors(),
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = expandedInterval,
                            onDismissRequest = { expandedInterval = false }
                        ) {
                            BACKTEST_INTERVALS.forEach { tf ->
                                DropdownMenuItem(
                                    text = { Text(tf) },
                                    onClick = {
                                        interval = tf
                                        expandedInterval = false
                                    }
                                )
                            }
                        }
                    }

                    // ── Period mode toggle ──
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Use date range", fontSize = 12.sp, color = TextSecondary)
                        Switch(checked = useDateRange, onCheckedChange = { useDateRange = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = AccentBlue))
                    }

                    if (!useDateRange) {
                        // ── Days input ──
                        Text("Days to Test", fontSize = 12.sp, color = TextSecondary)
                        OutlinedTextField(
                            value = days, onValueChange = { days = it.filter { c -> c.isDigit() } },
                            label = { Text("Days") },
                            colors = com.tfg.core.ui.tfgTextFieldColors(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        // ── Start date ──
                        Text("Start Date", fontSize = 12.sp, color = TextSecondary)
                        OutlinedTextField(
                            value = dateFormat.format(java.util.Date(startDateMs)),
                            onValueChange = { str ->
                                try { dateFormat.parse(str)?.let { startDateMs = it.time } } catch (_: Exception) {}
                            },
                            label = { Text("yyyy-MM-dd") },
                            colors = com.tfg.core.ui.tfgTextFieldColors(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        // ── End date ──
                        Text("End Date", fontSize = 12.sp, color = TextSecondary)
                        OutlinedTextField(
                            value = dateFormat.format(java.util.Date(endDateMs)),
                            onValueChange = { str ->
                                try { dateFormat.parse(str)?.let { endDateMs = it.time } } catch (_: Exception) {}
                            },
                            label = { Text("yyyy-MM-dd") },
                            colors = com.tfg.core.ui.tfgTextFieldColors(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val effectiveDays = if (useDateRange) {
                        viewModel.updateBacktestStartDate(startDateMs)
                        viewModel.updateBacktestEndDate(endDateMs)
                        ((endDateMs - startDateMs) / (24 * 60 * 60 * 1000)).toInt().coerceAtLeast(1)
                    } else {
                        viewModel.updateBacktestStartDate(null)
                        viewModel.updateBacktestEndDate(null)
                        days.toIntOrNull() ?: 30
                    }
                    if (useMultiSymbol) {
                        val syms = multiSymbolText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        viewModel.updateBacktestSymbols(syms)
                        viewModel.runMultiBacktest(syms, interval, effectiveDays)
                    } else {
                        viewModel.updateBacktestSymbols(emptyList())
                        viewModel.runBacktest(symbol, interval, effectiveDays)
                    }
                    showBacktestDialog = false
                }) { Text("Run", color = AccentBlue) }
            },
            dismissButton = {
                TextButton(onClick = { showBacktestDialog = false }) { Text("Cancel", color = TextSecondary) }
            },
            containerColor = DarkSurface
        )
    }

    // Pair picker dialog
    if (showPairPicker) {
        AlertDialog(
            onDismissRequest = { showPairPicker = false },
            title = { Text("Select Trading Pair", color = TextPrimary) },
            text = {
                Column {
                    POPULAR_PAIRS.forEach { pair ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.selectPair(pair)
                                    showPairPicker = false
                                }
                                .background(
                                    if (pair == state.selectedPair) AccentBlue.copy(alpha = 0.15f)
                                    else Color.Transparent
                                )
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(pair, color = TextPrimary, fontSize = 14.sp)
                            if (pair == state.selectedPair) {
                                Icon(Icons.Default.Check, null, tint = AccentBlue, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = DarkSurface
        )
    }

    // Save as template dialog
    if (showSaveTemplateDialog) {
        var templateName by remember { mutableStateOf("") }
        var templateDesc by remember { mutableStateOf("") }
        // Editable copy of current params
        var editParams by remember { mutableStateOf(state.editableParams) }
        // New param fields
        var newParamKey by remember { mutableStateOf("") }
        var newParamValue by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSaveTemplateDialog = false },
            title = { Text("Save as Template", color = TextPrimary) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = templateName,
                        onValueChange = { templateName = it },
                        label = { Text("Template Name") },
                        placeholder = { Text("e.g. My Fast SMA") },
                        colors = com.tfg.core.ui.tfgTextFieldColors(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = templateDesc,
                        onValueChange = { templateDesc = it },
                        label = { Text("Description") },
                        placeholder = { Text("Short description of the strategy") },
                        colors = com.tfg.core.ui.tfgTextFieldColors(),
                        maxLines = 3,
                        modifier = Modifier.heightIn(min = 72.dp)
                    )

                    // ── Default Settings (editable) ─────────────────────
                    Text("Default Settings", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AccentBlue)
                    val paramEntries = editParams.entries.toList()
                    val rows = paramEntries.chunked(2)
                    rows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            row.forEach { (key, value) ->
                                OutlinedTextField(
                                    value = value,
                                    onValueChange = { editParams = editParams + (key to it) },
                                    label = {
                                        Text(
                                            key.replace("_", " ").lowercase()
                                                .replaceFirstChar { it.uppercase() },
                                            fontSize = 9.sp, maxLines = 1
                                        )
                                    },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Default.Close, "Remove",
                                            tint = Red400,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable { editParams = editParams - key }
                                        )
                                    },
                                    colors = com.tfg.core.ui.tfgTextFieldColors(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp),
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 12.sp, color = TextPrimary
                                    )
                                )
                            }
                            if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }

                    // ── Add new param ────────────────────────────────────
                    Text("Add Parameter", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newParamKey,
                            onValueChange = { newParamKey = it.uppercase().replace(" ", "_") },
                            label = { Text("KEY", fontSize = 9.sp) },
                            placeholder = { Text("MY_PARAM", fontSize = 10.sp) },
                            colors = com.tfg.core.ui.tfgTextFieldColors(),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 12.sp, color = TextPrimary
                            )
                        )
                        OutlinedTextField(
                            value = newParamValue,
                            onValueChange = { newParamValue = it },
                            label = { Text("Value", fontSize = 9.sp) },
                            placeholder = { Text("0.0", fontSize = 10.sp) },
                            colors = com.tfg.core.ui.tfgTextFieldColors(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 12.sp, color = TextPrimary
                            )
                        )
                        IconButton(
                            onClick = {
                                if (newParamKey.isNotBlank() && newParamValue.isNotBlank()) {
                                    editParams = editParams + (newParamKey.trim() to newParamValue.trim())
                                    newParamKey = ""
                                    newParamValue = ""
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Add, "Add", tint = Green500, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (templateName.isNotBlank()) {
                            viewModel.saveAsCustomTemplate(templateName.trim(), templateDesc.trim(), editParams)
                            showSaveTemplateDialog = false
                        }
                    }
                ) { Text("Save", color = AccentBlue) }
            },
            dismissButton = {
                TextButton(onClick = { showSaveTemplateDialog = false }) { Text("Cancel", color = TextSecondary) }
            },
            containerColor = DarkSurface
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Active strategy banner
        state.activeStrategy?.let { active ->
            TfgCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Green500)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "Active: ${active.name}",
                                fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Green500
                            )
                            Text(
                                "Running on ${active.activeSymbol ?: "—"}",
                                fontSize = 11.sp, color = TextSecondary
                            )
                        }
                    }
                    TextButton(
                        onClick = { viewModel.deactivateStrategy() },
                        enabled = !state.isActivating
                    ) {
                        Text("Stop", color = Red400, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Tab row
        ScrollableTabRow(
            selectedTabIndex = currentTab,
            containerColor = DarkSurface,
            contentColor = AccentBlue,
            edgePadding = 0.dp
        ) {
            Tab(selected = currentTab == 0, onClick = { currentTab = 0 },
                text = { Text("Editor", fontSize = 12.sp) })
            Tab(selected = currentTab == 1, onClick = { currentTab = 1 },
                text = { Text("Templates", fontSize = 12.sp) })
            Tab(selected = currentTab == 2, onClick = { currentTab = 2 },
                text = { Text("Strategies", fontSize = 12.sp) })
            Tab(selected = currentTab == 3, onClick = { currentTab = 3 },
                text = { Text("Docs", fontSize = 12.sp) })
            Tab(selected = currentTab == 4, onClick = { currentTab = 4 },
                text = { Text("Visual", fontSize = 12.sp) })
        }

        when (currentTab) {
            0 -> EditorTab(
                state = state,
                viewModel = viewModel,
                onSave = { showSaveDialog = true },
                onBacktest = { showBacktestDialog = true },
                onPickPair = { showPairPicker = true },
                onSaveAsTemplate = { showSaveTemplateDialog = true }
            )
            1 -> TemplatesTab(
                state = state,
                onLoadTemplate = { template ->
                    viewModel.loadTemplate(template)
                    currentTab = 0 // Switch to editor
                },
                onLoadCustomTemplate = { custom ->
                    viewModel.loadCustomTemplate(custom)
                    currentTab = 0
                },
                onDeleteCustomTemplate = { viewModel.deleteCustomTemplate(it) },
                onUpdateCustomTemplate = { template, name, desc, params ->
                    viewModel.updateCustomTemplate(template, name, desc, params)
                }
            )
            2 -> MyStrategiesTab(
                state = state,
                viewModel = viewModel,
                onPickPair = { showPairPicker = true },
                onEdit = { script ->
                    viewModel.selectScript(script)
                    currentTab = 0
                }
            )
            3 -> DocsTab()
            4 -> VisualBuilderTab(
                onGenerateCode = { code ->
                    viewModel.updateCode(code)
                    currentTab = 0
                }
            )
        }
    }
}

@Composable
private fun EditorTab(
    state: ScriptUiState,
    viewModel: ScriptViewModel,
    onSave: () -> Unit,
    onBacktest: () -> Unit,
    onPickPair: () -> Unit,
    onSaveAsTemplate: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                state.selectedScript?.name ?: state.currentTemplateName ?: "New Script",
                fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Pair selector chip
                AssistChip(
                    onClick = onPickPair,
                    label = { Text(state.selectedPair, fontSize = 10.sp) },
                    modifier = Modifier
                        .height(28.dp)
                        .padding(end = 2.dp)
                )
                // Settings toggle (gear icon)
                if (state.editableParams.isNotEmpty()) {
                    IconButton(onClick = { viewModel.toggleSettings() }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Default.Settings, "Settings",
                            tint = if (state.showSettings) AccentBlue else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                IconButton(onClick = onSave, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Save, "Save", tint = AccentBlue, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onSaveAsTemplate, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Bookmark, "Save as Template", tint = AccentOrange, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onBacktest, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.PlayArrow, "Backtest", tint = Green500, modifier = Modifier.size(20.dp))
                }
            }
        }

        // ─── Scrollable content ─────────────────────────────────────
        val density = LocalDensity.current
        var miniChartHeight by remember { mutableStateOf(150.dp) }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {

        // ─── Mini Chart Preview ─────────────────────────────────────
        if (state.candles.isNotEmpty()) {
            // In replay mode, slice candles/signals up to current bar (same as chart page)
            val displayCandles = if (state.replayMode)
                state.candles.take(state.replayBarIndex + 1) else state.candles
            val displaySignals = if (state.replayMode)
                state.replaySignals else state.signalMarkers
            TradingViewChart(
                candles = displayCandles,
                signals = displaySignals,
                overlayIndicators = emptyList(),
                subIndicators = emptyList(),
                livePrice = null,
                showDrawingTools = false,
                supertrendData = null,
                ichimokuData = null,
                cloudOverlays = emptyList(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(miniChartHeight)
                    .padding(horizontal = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            // Resize handle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, dragAmount ->
                            with(density) {
                                miniChartHeight = (miniChartHeight + dragAmount.toDp())
                                    .coerceIn(100.dp, 400.dp)
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(TextTertiary.copy(alpha = 0.5f))
                )
            }
        } else if (state.isLoadingChart) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AccentBlue, modifier = Modifier.size(24.dp))
            }
        }

        // ─── Replay Controls ────────────────────────────────────────
        if (state.candles.isNotEmpty()) {
            TfgCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Column {
                    if (state.replayMode) {
                        // Replay header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.SlowMotionVideo, contentDescription = null, tint = AccentOrange, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Replay Mode", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AccentOrange)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Bar ${state.replayBarIndex}/${state.candles.size - 1}", fontSize = 10.sp, color = TextTertiary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Signals: ${state.replaySignals.size}", fontSize = 10.sp, color = Green400)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        // Seek slider
                        Slider(
                            value = state.replayBarIndex.toFloat(),
                            onValueChange = { viewModel.seekReplay(it.toInt()) },
                            valueRange = 1f..(state.candles.size - 1).toFloat().coerceAtLeast(2f),
                            modifier = Modifier.fillMaxWidth().height(24.dp),
                            colors = SliderDefaults.colors(thumbColor = AccentBlue, activeTrackColor = AccentBlue)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        // Transport controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { viewModel.replayStepBack() }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.SkipPrevious, "Step Back", tint = TextPrimary, modifier = Modifier.size(22.dp))
                            }
                            IconButton(
                                onClick = { if (state.replayPlaying) viewModel.replayPause() else viewModel.replayPlay() },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    if (state.replayPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    if (state.replayPlaying) "Pause" else "Play",
                                    tint = AccentBlue, modifier = Modifier.size(28.dp)
                                )
                            }
                            IconButton(onClick = { viewModel.replayStepForward() }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.SkipNext, "Step Forward", tint = TextPrimary, modifier = Modifier.size(22.dp))
                            }
                            // Speed selector
                            val speeds = listOf(1000L to "1x", 500L to "2x", 250L to "4x", 100L to "10x")
                            speeds.forEach { (ms, label) ->
                                val selected = state.replaySpeedMs == ms
                                TextButton(
                                    onClick = { viewModel.setReplaySpeed(ms) },
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text(
                                        label, fontSize = 10.sp,
                                        color = if (selected) AccentBlue else TextTertiary,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        // Exit button
                        TextButton(
                            onClick = { viewModel.exitReplayMode() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Red400, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Exit Replay", color = Red400, fontSize = 11.sp)
                        }
                    } else {
                        // Replay + Compare + Optimizer buttons (replay only after backtest)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (state.backtestResult != null) {
                                TextButton(
                                    onClick = { viewModel.enterReplayMode() },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(Icons.Default.SlowMotionVideo, contentDescription = null, tint = AccentOrange, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Replay", fontSize = 11.sp, color = AccentOrange)
                                }
                                TextButton(
                                    onClick = { viewModel.toggleBuyAndHoldComparison() },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(Icons.Default.CompareArrows, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        if (state.showBuyAndHoldComparison) "Hide Compare" else "Compare B&H",
                                        fontSize = 11.sp, color = AccentBlue
                                    )
                                }
                            } else {
                                // Show disabled hint before backtest
                                Text("Run backtest to unlock Replay & Compare", fontSize = 10.sp, color = TextTertiary,
                                    modifier = Modifier.padding(horizontal = 8.dp))
                            }
                            TextButton(
                                onClick = { viewModel.toggleOptimizer() },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = null, tint = Green500, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Optimize", fontSize = 11.sp, color = Green500)
                            }
                        }
                    }
                }
            }
        }

        // ─── Buy & Hold Comparison ──────────────────────────────────
        if (state.showBuyAndHoldComparison && state.strategyEquityCurve.isNotEmpty() && state.buyAndHoldEquityCurve.isNotEmpty()) {
            TfgCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Column {
                    Text("Equity Curve Comparison", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
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
                    // Simple equity curve display
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
                    val stratReturn = if (state.strategyEquityCurve.size >= 2)
                        ((state.strategyEquityCurve.last() - state.strategyEquityCurve.first()) / state.strategyEquityCurve.first() * 100) else 0.0
                    val bhReturn = if (state.buyAndHoldEquityCurve.size >= 2)
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

        // ─── Optimizer Panel ────────────────────────────────────────
        AnimatedVisibility(visible = state.showOptimizer) {
            TfgCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = Green500, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Parameter Optimizer", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Green500)
                        }
                        IconButton(onClick = { viewModel.toggleOptimizer() }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, "Close", tint = TextTertiary, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    // Parameter range inputs
                    state.optimizationParams.forEach { param ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                param.name.replace("_", " ").take(12),
                                fontSize = 10.sp, color = TextSecondary,
                                modifier = Modifier.width(70.dp),
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            OutlinedTextField(
                                value = String.format("%.1f", param.min),
                                onValueChange = { v -> v.toDoubleOrNull()?.let { viewModel.updateOptimizationParam(param.name, it, param.max, param.step) } },
                                label = { Text("Min", fontSize = 8.sp) },
                                colors = com.tfg.core.ui.tfgTextFieldColors(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f).height(48.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = TextPrimary)
                            )
                            OutlinedTextField(
                                value = String.format("%.1f", param.max),
                                onValueChange = { v -> v.toDoubleOrNull()?.let { viewModel.updateOptimizationParam(param.name, param.min, it, param.step) } },
                                label = { Text("Max", fontSize = 8.sp) },
                                colors = com.tfg.core.ui.tfgTextFieldColors(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f).height(48.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = TextPrimary)
                            )
                            OutlinedTextField(
                                value = String.format("%.1f", param.step),
                                onValueChange = { v -> v.toDoubleOrNull()?.let { viewModel.updateOptimizationParam(param.name, param.min, param.max, it) } },
                                label = { Text("Step", fontSize = 8.sp) },
                                colors = com.tfg.core.ui.tfgTextFieldColors(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f).height(48.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = TextPrimary)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    // Run / Cancel button
                    if (state.isOptimizing) {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Optimizing…", fontSize = 10.sp, color = TextSecondary)
                                Text("${(state.optimizationProgress * 100).toInt()}%", fontSize = 10.sp, color = Green500, fontWeight = FontWeight.Bold)
                            }
                            LinearProgressIndicator(
                                progress = state.optimizationProgress,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                color = Green500
                            )
                            TextButton(onClick = { viewModel.cancelOptimization() }, modifier = Modifier.fillMaxWidth()) {
                                Text("Cancel", color = Red400, fontSize = 11.sp)
                            }
                        }
                    } else {
                        Button(
                            onClick = { viewModel.runOptimization(state.selectedPair, state.backtestInterval, 30) },
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Green500)
                        ) {
                            Text("Run Optimization", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    // Results table
                    if (state.optimizationResults.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(color = DarkBorder, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Results (${state.optimizationResults.size} combinations)", fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        // Sort buttons
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            listOf("pnlPercent" to "PnL", "winRate" to "Win%", "sharpe" to "Sharpe", "maxDrawdown" to "DD", "profitFactor" to "PF").forEach { (field, label) ->
                                val selected = state.optimizationSortBy == field
                                TextButton(
                                    onClick = { viewModel.sortOptimizationBy(field) },
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                    modifier = Modifier.height(24.dp)
                                ) {
                                    Text(label, fontSize = 9.sp, color = if (selected) AccentBlue else TextTertiary, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                        // Top results (show first 10)
                        state.optimizationResults.take(10).forEachIndexed { index, result ->
                            val isBest = index == 0
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .background(
                                        if (isBest) Green400.copy(alpha = 0.08f) else Color.Transparent,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .clickable { viewModel.applyOptimizationResult(result) }
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        result.params.entries.joinToString(", ") { "${it.key}=${it.value}" },
                                        fontSize = 9.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("${String.format("%.1f", result.pnlPercent)}%", fontSize = 10.sp, color = if (result.pnlPercent >= 0) Green400 else Red400)
                                    Text("W${String.format("%.0f", result.winRate)}%", fontSize = 10.sp, color = TextSecondary)
                                    Text("S${String.format("%.1f", result.sharpe)}", fontSize = 10.sp, color = TextTertiary)
                                }
                            }
                        }
                        if (state.optimizationResults.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            TextButton(
                                onClick = { viewModel.applyOptimizationResult(state.optimizationResults.first()) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Apply Best Parameters", color = Green500, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // ─── Dynamic Settings Panel (TradingView-style) ───────────
        AnimatedVisibility(visible = state.showSettings && state.editableParams.isNotEmpty()) {
            TfgCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Strategy Settings",
                            fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AccentBlue
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = {
                                    viewModel.applyParamsToCode()
                                    viewModel.toggleSettings()
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Apply", fontSize = 11.sp, color = Green500, fontWeight = FontWeight.Bold)
                            }
                            IconButton(
                                onClick = { viewModel.toggleSettings() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, "Close", tint = TextTertiary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    // Grid of param fields (2 columns)
                    val paramEntries = state.editableParams.entries.toList()
                    val rows = paramEntries.chunked(2)
                    rows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            row.forEach { (key, value) ->
                                OutlinedTextField(
                                    value = value,
                                    onValueChange = { viewModel.updateParam(key, it) },
                                    label = {
                                        Text(
                                            key.replace("_", " ").lowercase()
                                                .replaceFirstChar { it.uppercase() },
                                            fontSize = 9.sp, maxLines = 1
                                        )
                                    },
                                    colors = com.tfg.core.ui.tfgTextFieldColors(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp),
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 12.sp, color = TextPrimary
                                    )
                                )
                            }
                            // If odd number of items, add spacer for alignment
                            if (row.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // Saved scripts chips
        if (state.scripts.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.scripts) { script ->
                    FilterChip(
                        selected = state.selectedScript?.id == script.id,
                        onClick = { viewModel.selectScript(script) },
                        label = { Text(script.name, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentBlue.copy(alpha = 0.2f),
                            selectedLabelColor = AccentBlue
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Code editor with CodeMirror (autocomplete + inline errors)
        MonacoEditor(
            code = state.code,
            onCodeChange = { viewModel.updateCode(it) },
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
                .padding(horizontal = 8.dp),
            minHeight = 350.dp
        )

        // ─── Syntax Error Bar ───────────────────────────────────────
        state.syntaxError?.let { err ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                    .background(Red400.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Red400, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(err, fontSize = 11.sp, color = Red400, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }

        // ─── Console Panel ──────────────────────────────────────────
        if (state.console.isNotEmpty()) {
            var consoleExpanded by remember { mutableStateOf(true) }
            TfgCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { consoleExpanded = !consoleExpanded }
                        ) {
                            Icon(
                                if (consoleExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null, tint = AccentOrange, modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Console", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AccentOrange)
                            Text(" (${state.console.size})", fontSize = 10.sp, color = TextTertiary)
                        }
                        IconButton(onClick = { viewModel.clearConsole() }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Delete, "Clear", tint = TextTertiary, modifier = Modifier.size(14.dp))
                        }
                    }
                    AnimatedVisibility(visible = consoleExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 180.dp)
                                .verticalScroll(rememberScrollState())
                                .background(Color(0xFF0D0D1A), RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            state.console.forEach { line ->
                                val isWarn = line.startsWith("[WARN]")
                                val isErr = line.startsWith("[ERROR]")
                                Text(
                                    line,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = when {
                                        isErr -> Red400
                                        isWarn -> AccentOrange
                                        else -> Color(0xFFA9DC76)
                                    },
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Backtest results
        state.backtestResult?.let { result ->
            TfgCard(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
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
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Text("Sharpe: ${String.format("%.2f", result.sharpeRatio)}", fontSize = 11.sp, color = TextSecondary)
                        Text("Sortino: ${String.format("%.2f", result.sortinoRatio)}", fontSize = 11.sp, color = TextSecondary)
                        Text("Max DD: ${String.format("%.2f", result.maxDrawdown)}%", fontSize = 11.sp, color = Red400)
                    }
                    // ── Extended metrics ──
                    Spacer(modifier = Modifier.height(6.dp))
                    Divider(color = DarkBorder, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Profit Factor", fontSize = 9.sp, color = TextTertiary)
                            val pf = if (result.profitFactor == Double.MAX_VALUE) "∞" else String.format("%.2f", result.profitFactor)
                            Text(pf, fontSize = 12.sp, color = if (result.profitFactor > 1) Green400 else Red400)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Buy & Hold", fontSize = 9.sp, color = TextTertiary)
                            Text("${String.format("%.1f", result.buyAndHoldReturn)}%", fontSize = 12.sp, color = TextPrimary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Expectancy", fontSize = 9.sp, color = TextTertiary)
                            PnlText(value = result.expectancy, fontSize = 12)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Avg Win", fontSize = 9.sp, color = TextTertiary)
                            PnlText(value = result.avgWin, fontSize = 11)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Avg Loss", fontSize = 9.sp, color = TextTertiary)
                            PnlText(value = result.avgLoss, fontSize = 11)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Avg Bars", fontSize = 9.sp, color = TextTertiary)
                            Text(String.format("%.1f", result.avgBarsInTrade), fontSize = 11.sp, color = TextPrimary)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Text("Gross +: ${String.format("%.2f", result.grossProfit)}", fontSize = 10.sp, color = Green400)
                        Text("Gross −: ${String.format("%.2f", result.grossLoss)}", fontSize = 10.sp, color = Red400)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Text("Best: ${String.format("%.2f", result.largestWin)}", fontSize = 10.sp, color = Green400)
                        Text("Worst: ${String.format("%.2f", result.largestLoss)}", fontSize = 10.sp, color = Red400)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Text("Consec W: ${result.maxConsecutiveWins}", fontSize = 10.sp, color = Green400)
                        Text("Consec L: ${result.maxConsecutiveLosses}", fontSize = 10.sp, color = Red400)
                    }
                    // ── C5: Export button ──
                    Spacer(modifier = Modifier.height(6.dp))
                    Divider(color = DarkBorder, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        TextButton(onClick = { viewModel.exportBacktestCsv() }) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export CSV", color = AccentBlue, fontSize = 12.sp)
                        }
                        TextButton(onClick = { viewModel.runMonteCarloSimulation() }) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Monte Carlo", color = AccentBlue, fontSize = 12.sp)
                        }
                    }
                    // ── C4: Monte Carlo results ──
                    state.monteCarloResult?.let { mc ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Divider(color = DarkBorder, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Monte Carlo (${mc.simulations} sims)", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = TextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Equity (5th)", fontSize = 9.sp, color = TextTertiary)
                                Text("$${String.format("%.0f", mc.p5FinalEquity)}", fontSize = 11.sp, color = Red400)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Equity (Med)", fontSize = 9.sp, color = TextTertiary)
                                Text("$${String.format("%.0f", mc.medianFinalEquity)}", fontSize = 11.sp, color = TextPrimary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Equity (95th)", fontSize = 9.sp, color = TextTertiary)
                                Text("$${String.format("%.0f", mc.p95FinalEquity)}", fontSize = 11.sp, color = Green400)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("DD Best", fontSize = 9.sp, color = TextTertiary)
                                Text("${String.format("%.1f", mc.p95MaxDrawdown)}%", fontSize = 11.sp, color = Green400)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("DD Median", fontSize = 9.sp, color = TextTertiary)
                                Text("${String.format("%.1f", mc.medianMaxDrawdown)}%", fontSize = 11.sp, color = TextPrimary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("DD Worst", fontSize = 9.sp, color = TextTertiary)
                                Text("${String.format("%.1f", mc.p5MaxDrawdown)}%", fontSize = 11.sp, color = Red400)
                            }
                        }
                    }
                }
            }
        }

        if (state.isRunning) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
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
                TextButton(
                    onClick = { viewModel.cancelBacktest() },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp), tint = Red400)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Stop", color = Red400, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        state.error?.let {
            ErrorMessage(message = it, modifier = Modifier.padding(8.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        } // end scrollable column
    }
}

@Composable
private fun TemplatesTab(
    state: ScriptUiState,
    onLoadTemplate: (com.tfg.domain.model.StrategyTemplate) -> Unit,
    onLoadCustomTemplate: (CustomTemplate) -> Unit,
    onDeleteCustomTemplate: (String) -> Unit,
    onUpdateCustomTemplate: (CustomTemplate, String, String, Map<String, String>) -> Unit
) {
    // Edit template dialog state
    var editingTemplate by remember { mutableStateOf<CustomTemplate?>(null) }

    // Edit Custom Template Dialog
    editingTemplate?.let { template ->
        var editName by remember(template.id) { mutableStateOf(template.name) }
        var editDesc by remember(template.id) { mutableStateOf(template.description) }
        var editParams by remember(template.id) { mutableStateOf(template.defaultParams) }
        var newParamKey by remember(template.id) { mutableStateOf("") }
        var newParamValue by remember(template.id) { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { editingTemplate = null },
            title = { Text("Edit Template", color = TextPrimary) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Template Name") },
                        colors = com.tfg.core.ui.tfgTextFieldColors(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editDesc,
                        onValueChange = { editDesc = it },
                        label = { Text("Description") },
                        colors = com.tfg.core.ui.tfgTextFieldColors(),
                        maxLines = 3,
                        modifier = Modifier.heightIn(min = 72.dp)
                    )

                    // ── Settings (editable) ─────────────────────────────
                    Text("Settings", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AccentOrange)
                    val paramEntries = editParams.entries.toList()
                    val rows = paramEntries.chunked(2)
                    rows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            row.forEach { (key, value) ->
                                OutlinedTextField(
                                    value = value,
                                    onValueChange = { editParams = editParams + (key to it) },
                                    label = {
                                        Text(
                                            key.replace("_", " ").lowercase()
                                                .replaceFirstChar { it.uppercase() },
                                            fontSize = 9.sp, maxLines = 1
                                        )
                                    },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Default.Close, "Remove",
                                            tint = Red400,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable { editParams = editParams - key }
                                        )
                                    },
                                    colors = com.tfg.core.ui.tfgTextFieldColors(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp),
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 12.sp, color = TextPrimary
                                    )
                                )
                            }
                            if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }

                    // ── Add new param ────────────────────────────────────
                    Text("Add Parameter", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newParamKey,
                            onValueChange = { newParamKey = it.uppercase().replace(" ", "_") },
                            label = { Text("KEY", fontSize = 9.sp) },
                            placeholder = { Text("MY_PARAM", fontSize = 10.sp) },
                            colors = com.tfg.core.ui.tfgTextFieldColors(),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 12.sp, color = TextPrimary
                            )
                        )
                        OutlinedTextField(
                            value = newParamValue,
                            onValueChange = { newParamValue = it },
                            label = { Text("Value", fontSize = 9.sp) },
                            placeholder = { Text("0.0", fontSize = 10.sp) },
                            colors = com.tfg.core.ui.tfgTextFieldColors(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 12.sp, color = TextPrimary
                            )
                        )
                        IconButton(
                            onClick = {
                                if (newParamKey.isNotBlank() && newParamValue.isNotBlank()) {
                                    editParams = editParams + (newParamKey.trim() to newParamValue.trim())
                                    newParamKey = ""
                                    newParamValue = ""
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Add, "Add", tint = Green500, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editName.isNotBlank()) {
                            onUpdateCustomTemplate(template, editName.trim(), editDesc.trim(), editParams)
                            editingTemplate = null
                        }
                    }
                ) { Text("Save", color = AccentBlue) }
            },
            dismissButton = {
                TextButton(onClick = { editingTemplate = null }) { Text("Cancel", color = TextSecondary) }
            },
            containerColor = DarkSurface
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ─── Custom Templates Section ───────────────────────────────
        if (state.customTemplates.isNotEmpty()) {
            Text(
                "My Templates",
                fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AccentOrange,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                "Custom templates you created with your own settings.",
                fontSize = 12.sp, color = TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            state.customTemplates.forEach { custom ->
                var expanded by remember { mutableStateOf(false) }
                TfgCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    null, tint = TextTertiary, modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Column {
                                    Text(
                                        custom.name,
                                        fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AccentOrange
                                    )
                                    Text(
                                        "Based on: ${custom.baseTemplateId.replace("_", " ")}",
                                        fontSize = 10.sp, color = TextTertiary
                                    )
                                }
                            }
                            Row {
                                IconButton(
                                    onClick = { onDeleteCustomTemplate(custom.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Delete, "Delete", tint = Red400, modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = { editingTemplate = custom },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Edit, "Edit", tint = AccentBlue, modifier = Modifier.size(16.dp))
                                }
                                Button(
                                    onClick = { onLoadCustomTemplate(custom) },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Use", fontSize = 12.sp)
                                }
                            }
                        }
                        if (custom.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                custom.description,
                                fontSize = 12.sp, color = TextSecondary, lineHeight = 16.sp
                            )
                        }
                        if (custom.defaultParams.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.horizontalScroll(rememberScrollState())
                            ) {
                                custom.defaultParams.forEach { (key, value) ->
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = DarkBackground
                                    ) {
                                        Text(
                                            "$key=$value",
                                            fontSize = 10.sp, color = TextTertiary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Expandable code preview
                        AnimatedVisibility(visible = expanded) {
                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                Divider(color = DarkBorder, thickness = 0.5.dp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Code Preview", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = TextTertiary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = DarkBackground,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        custom.code,
                                        fontSize = 10.sp,
                                        color = TextPrimary,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        lineHeight = 14.sp,
                                        modifier = Modifier
                                            .padding(8.dp)
                                            .horizontalScroll(rememberScrollState())
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = DarkBorder, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))
        }

        // ─── Built-in Templates Section ─────────────────────────────
        Text(
            "Strategy Templates",
            fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            "Tap a template to preview code. Use to load into editor.",
            fontSize = 12.sp, color = TextSecondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        state.templates.forEach { template ->
            var expanded by remember { mutableStateOf(false) }
            TfgCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                null, tint = TextTertiary, modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                template.name,
                                fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AccentBlue
                            )
                        }
                        Button(
                            onClick = { onLoadTemplate(template) },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Use", fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        template.description,
                        fontSize = 12.sp, color = TextSecondary, lineHeight = 16.sp
                    )
                    if (template.defaultParams.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            template.defaultParams.forEach { (key, value) ->
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = DarkBackground
                                ) {
                                    Text(
                                        "$key=$value",
                                        fontSize = 10.sp, color = TextTertiary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Expandable code preview
                    AnimatedVisibility(visible = expanded) {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            Divider(color = DarkBorder, thickness = 0.5.dp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Code Preview", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = TextTertiary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = DarkBackground,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    template.code,
                                    fontSize = 10.sp,
                                    color = TextPrimary,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    lineHeight = 14.sp,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .horizontalScroll(rememberScrollState())
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MyStrategiesTab(
    state: ScriptUiState,
    viewModel: ScriptViewModel,
    onPickPair: () -> Unit,
    onEdit: (Script) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Pair selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("My Saved Strategies", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            AssistChip(
                onClick = onPickPair,
                label = { Text("Pair: ${state.selectedPair}", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.SwapHoriz, null, modifier = Modifier.size(16.dp)) }
            )
        }

        if (state.activeStrategy != null) {
            Text(
                "Only one strategy can run at a time. Stop the active strategy to start another.",
                fontSize = 11.sp, color = TextTertiary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        if (state.scripts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Code, null, tint = TextTertiary, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No strategies yet", color = TextSecondary, fontSize = 14.sp)
                    Text("Use a template or write your own", color = TextTertiary, fontSize = 12.sp)
                }
            }
        }

        state.scripts.forEach { script ->
            val isActive = state.activeStrategy?.id == script.id
            TfgCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isActive) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Green500)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Column {
                                Text(script.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                if (isActive && script.activeSymbol != null) {
                                    Text("Running on ${script.activeSymbol}", fontSize = 11.sp, color = Green500)
                                }
                            }
                        }
                        Row {
                            IconButton(onClick = { onEdit(script) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Edit, "Edit", tint = TextSecondary, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { viewModel.deleteScript(script.id) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Delete, "Delete", tint = Red400, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Activate / Deactivate button
                    if (isActive) {
                        Button(
                            onClick = { viewModel.deactivateStrategy() },
                            enabled = !state.isActivating,
                            colors = ButtonDefaults.buttonColors(containerColor = Red400),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (state.isActivating) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Stop, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Stop Strategy")
                            }
                        }
                    } else {
                        Button(
                            onClick = { viewModel.activateStrategy(script.id) },
                            enabled = !state.isActivating && state.activeStrategy == null,
                            colors = ButtonDefaults.buttonColors(containerColor = Green500),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (state.isActivating) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Run on ${state.selectedPair}")
                            }
                        }
                    }

                    script.strategyTemplateId?.let { tid ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(shape = RoundedCornerShape(4.dp), color = DarkBackground) {
                            Text(
                                "Template: ${tid.replace("_", " ")}",
                                fontSize = 10.sp, color = TextTertiary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Documentation Tab ────────────────────────────────────────────────
@Composable
private fun DocsTab() {
    val mono = FontFamily.Monospace
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Title
        Text(
            "Strategy Engine Documentation",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = AccentBlue
        )

        // ── 1. Overview & Execution Model ──
        DocSection("1. How the Code Engine Works") {
            DocText(
                "Each strategy is a pure JavaScript function evaluated by the QuickJS engine. " +
                "When you run a backtest or activate a strategy, the engine calls your " +
                "strategy(candles) function once per candle bar."
            )
            DocText(
                "If your code differs from the built-in template, the engine executes your " +
                "custom JavaScript directly in a sandboxed QuickJS context. This means your " +
                "edits to strategy logic actually run — it's not just a facade."
            )
            DocSubSection("Per-bar lifecycle")
            DocBullets(listOf(
                "1. _log is reset to []",
                "2. _candles is injected with the full candle history up to the current bar",
                "3. _params is injected with your current parameter values",
                "4. _account is injected with equity and position data (or null)",
                "5. strategy(_candles) is called — your function runs",
                "6. The returned signal object is JSON-parsed",
                "7. Console logs from _log are captured"
            ))
            DocSubSection("Timeout & Error Handling")
            DocText(
                "Each strategy() call has a 5-second timeout. If your code takes longer, " +
                "the QuickJS instance is destroyed and the result defaults to HOLD.\n\n" +
                "Any JavaScript error during execution also defaults to HOLD and the engine " +
                "rebuilds the context on the next call. " +
                "Syntax errors are shown inline below the editor as you type (debounced 500ms)."
            )
            DocSubSection("Context Caching")
            DocText(
                "The QuickJS instance (including all indicator functions and your code) is " +
                "cached and reused as long as your code and params haven't changed. " +
                "If you edit either, the context is rebuilt from scratch. " +
                "This means _state persists between bars but resets when you change your code."
            )
        }

        // ── 2. Global Variables ──
        DocSection("2. Global Variables Available in strategy()") {
            DocText(
                "These variables are automatically injected into the JavaScript context " +
                "and available inside your strategy() function:"
            )
            DocSubSection("_candles")
            DocText(
                "Array of candle objects — also passed as the argument to strategy(candles). " +
                "candles[0] is the oldest bar, candles[candles.length - 1] is the latest."
            )
            DocSubSection("_params")
            DocText(
                "Object with your parameter values as string keys → string values. " +
                "Example: _params.RSI_PERIOD → \"14\" (always strings).\n\n" +
                "Additionally, each parameter is also injected as a top-level variable " +
                "with automatic number conversion. So if RSI_PERIOD = \"14\", both " +
                "_params.RSI_PERIOD === \"14\" and RSI_PERIOD === 14 are available."
            )
            DocSubSection("_state")
            DocText(
                "Persistent state object — survives across strategy() calls (bar to bar) " +
                "within the same backtest or live session. Initialized as {}. " +
                "Reset to {} at the start of each new backtest run."
            )
            DocCode(mono, """
function strategy(candles) {
    if (!_state.tradeCount) _state.tradeCount = 0;
    if (!_state.prevClose) _state.prevClose = 0;
    var last = candles[candles.length - 1];
    // Track changes across bars
    var delta = last.close - _state.prevClose;
    _state.prevClose = last.close;
    return {type: 'HOLD'};
}""".trimIndent())
            DocSubSection("_account")
            DocText(
                "Account/portfolio data injected each bar. In live mode uses real data; " +
                "in backtesting uses simulated equity and position tracking. " +
                "May be null if not provided."
            )
            DocCode(mono, """
_account = {
  equity:        10245.50,  // total account equity
  balance:       9800.00,   // cash balance
  positionSide:  'BUY',     // 'BUY','SELL', or null
  positionPnl:   445.50,    // unrealised PnL
  positionSize:  0.05,      // position quantity
  positionEntry: 62000.00   // entry price
}""".trimIndent())
            DocSubSection("console")
            DocText(
                "Standard-like console object with four methods for debug output. " +
                "Logs appear in the Console panel after a backtest completes."
            )
            DocCode(mono, """
console.log('Price:', price)     // plain log
console.warn('RSI high:', rsi)   // [WARN] prefix
console.error('Failed:', msg)    // [ERROR] prefix
console.info('Trade #:', count)  // [INFO] prefix""".trimIndent())
            DocSubSection("color")
            DocText(
                "Object with built-in color constants and utility functions. " +
                "Useful for indicator overlays and custom visualization."
            )
            DocCode(mono, """
// Constants
color.green   // '#26A69A'
color.red     // '#EF5350'
color.blue    // '#2196F3'
color.orange  // '#FF9800'
color.yellow  // '#FFD700'
color.purple  // '#9C27B0'
color.white   // '#FFFFFF'
color.gray    // '#8B949E'
color.aqua    // '#00BCD4'
color.lime    // '#00E676'
color.pink    // '#FF4081'
color.silver  // '#C0C0C0'
color.none    // 'transparent'

// Functions
color.rgba(255, 0, 0, 0.5)      // 'rgba(255,0,0,0.5)'
color.hexAlpha('#FF0000', 0.5)   // '#FF000080'""".trimIndent())
        }

        // ── 3. Candle Data ──
        DocSection("3. Candle Object Structure") {
            DocText(
                "Candles are fetched from the Binance Klines API. Each candle object:"
            )
            DocCode(mono, """
{
  openTime: 1704067200000,  // epoch ms
  open:     42150.00,       // opening price
  high:     42500.00,       // highest price
  low:      41900.00,       // lowest price
  close:    42350.00,       // closing price
  volume:   1234.56         // base asset volume
}""".trimIndent())
            DocText(
                "candles[0] is the oldest bar. candles[candles.length - 1] is the current/latest bar.\n\n" +
                "Available intervals: 1m, 3m, 5m, 15m, 30m, 1h, 2h, 4h, 6h, 8h, 12h, 1d, 3d, 1w, 1M\n" +
                "Maximum: 1000 candles per API call. Backtests paginate automatically for longer periods."
            )
        }

        // ── 4. Strategy Signals ──
        DocSection("4. Signal Return Format") {
            DocText("Your strategy() function must return a signal object (or array). Available signal types:")
            DocCode(mono, """
// ── Signal types ──
{type:'HOLD'}               // Do nothing (default)
{type:'BUY',  ...options}   // Open/add to long
{type:'SELL', ...options}   // Open/add to short
{type:'CLOSE_IF_LONG'}      // Close if currently long
{type:'CLOSE_IF_SHORT'}     // Close if currently short""".trimIndent())
            DocText(
                "Returning null, undefined, or an unrecognized type all default to HOLD."
            )
            DocSubSection("Signal Options")
            DocCode(mono, """
{
  type: 'BUY',
  // Position sizing (default: 2.0)
  sizePct: 5.0,             // % of equity
  // Simple TP/SL (single level)
  stopLossPct: 1.5,         // SL as % from entry
  takeProfitPct: 3.0,       // TP as % from entry
  // Optional: symbol for multi-signal mode
  symbol: 'BTCUSDT'
}""".trimIndent())
            DocSubSection("Graduated TP/SL (Multi-Level)")
            DocText(
                "For advanced exit strategies, use takeProfitLevels and/or stopLossLevels " +
                "arrays instead of a single percentage. Each level specifies a percentage " +
                "from entry and what portion of the position to close."
            )
            DocCode(mono, """
return {
  type: 'BUY',
  sizePct: 2.0,
  stopLossPct: 1.0,           // fallback SL
  takeProfitLevels: [
    {pct: 0.5, quantityPct: 30},  // close 30% at +0.5%
    {pct: 1.0, quantityPct: 30},  // close 30% at +1.0%
    {pct: 2.0, quantityPct: 40}   // close 40% at +2.0%
  ],
  stopLossLevels: [
    {pct: 0.3, quantityPct: 50},  // close 50% at -0.3%
    {pct: 0.8, quantityPct: 50}   // close 50% at -0.8%
  ]
};""".trimIndent())
            DocSubSection("Multi-Signal (Array Return)")
            DocText(
                "Return an array to target multiple pairs simultaneously. " +
                "Each signal's symbol field determines which pair it applies to."
            )
            DocCode(mono, """
return [
  {type:'BUY',  symbol:'BTCUSDT', sizePct: 2.0},
  {type:'SELL', symbol:'ETHUSDT', sizePct: 1.5,
   stopLossPct: 2.0},
  {type:'HOLD', symbol:'BNBUSDT'}
];""".trimIndent())
        }

        // ── 5. Indicator Functions ──
        DocSection("5. Single-Value Indicators") {
            DocText(
                "These return a single number (or object) for the latest bar. " +
                "All take a candles array as the first argument."
            )
            DocSubSection("Moving Averages")
            DocCode(mono, """
sma(candles, period)       // Simple MA
ema(candles, period)       // Exponential MA (Wilder seed)
wma(candles, period)       // Weighted MA (linear)
vwma(candles, period)      // Volume-Weighted MA
hullMa(candles, period)    // Hull MA
dema(candles, period)      // Double EMA
tema(candles, period)      // Triple EMA
zlema(candles, period)     // Zero-Lag EMA
kama(candles, period, fast?, slow?)  // Kaufman Adaptive
alma(candles, period, offset?, sigma?) // Arnaud Legoux
t3(candles, period, vfactor?)  // Tillson T3
mcginley(candles, period)  // McGinley Dynamic
frama(candles, period)     // Fractal Adaptive MA""".trimIndent())
            DocSubSection("Oscillators & Momentum")
            DocCode(mono, """
rsi(candles, period)       // RSI (0-100)
atr(candles, period)       // Average True Range
adx(candles, period)       // ADX (0-100)
cci(candles, period)       // Commodity Channel Index
williamsR(candles, period) // Williams %R (-100 to 0)
mfi(candles, period)       // Money Flow Index
roc(candles, period)       // Rate of Change (%)
momentum(candles, period)  // Price change over N bars
cmo(candles, period)       // Chande Momentum Osc
zScore(candles, period)    // Z-Score of latest close
ppo(candles, fast?, slow?, signal?)  // % Price Osc
dpo(candles, period)       // Detrended Price Osc
trix(candles, period)      // Triple EMA ROC
natr(candles, period)      // Normalized ATR (%)
trueRange(candles)         // True Range (latest)
awesomeOscillator(candles) // AO (5/34 median SMA diff)
acceleratorOscillator(candles) // AC (AO - SMA(AO,5))
ultimateOscillator(candles, p1?, p2?, p3?)""".trimIndent())
            DocSubSection("Volume")
            DocCode(mono, """
obv(candles)               // On-Balance Volume
vwap(candles)              // Vol-Weighted Avg Price
cmf(candles, period?)      // Chaikin Money Flow
adLine(candles)            // Accumulation/Distribution
forceIndex(candles, period?) // Force Index (EMA)
chaikinOscillator(candles, fast?, slow?)
eom(candles, period?)      // Ease of Movement
netVolume(candles)         // Net Volume
vwRsi(candles, period)     // Volume-Weighted RSI""".trimIndent())
            DocSubSection("Volatility")
            DocCode(mono, """
stdDev(candles, period?)   // Standard Deviation
historicalVolatility(candles, period?) // Annualized HV
chaikinVolatility(candles, ema?, roc?)
ulcerIndex(candles, period?)""".trimIndent())
            DocSubSection("Trend / Directional")
            DocCode(mono, """
diPlus(candles, period?)   // +DI
diMinus(candles, period?)  // -DI
massIndex(candles, ema?, sum?)""".trimIndent())
            DocSubSection("Object Returns")
            DocCode(mono, """
stochastic(candles, kPeriod, dPeriod)
  → [k, d]
stochasticRsi(candles, rsiP, stochP, kSmooth, dSmooth)
  → [k, d]
supertrend(candles, period, mult) → [price, isUptrend]
bollinger(candles, period, numStd) → {upper, middle, lower}
macd(candles, fast, slow, sig) → {macd, signal, histogram}
ichimoku(candles, tenkan, kijun, senkouB)
  → {tenkan, kijun, senkouA, senkouB, chikou}
parabolicSar(candles, step?, maxStep?)
keltnerChannels(candles, period?, mult?, atrPeriod?)
  → {upper, middle, lower}
donchianChannels(candles, period?) → {upper, middle, lower}
aroon(candles, period?) → {up, down, oscillator}
vortex(candles, period?) → {plus, minus}
elderRay(candles, period?) → {bullPower, bearPower}
kst(candles, ...) → {kst, signal, histogram}
volumeProfile(candles, bins?)
  → {poc, valueAreaHigh, valueAreaLow}
ppoSeries → {ppo:[], signal:[], histogram:[]}
candlePattern(candles) → {pattern, type}
  // e.g. {pattern:'hammer', type:'bullish'}
supportResistance(candles, leftBars?, rightBars?)
  → {resistance:[], support:[]}
priceChannel(candles, period?) → {upper, lower, middle}
coppockCurve(candles, longRoc?, shortRoc?, wma?)""".trimIndent())
        }

        // ── 6. Series Indicators ──
        DocSection("6. Series Indicators (Full History)") {
            DocText(
                "Series functions return an array with one value per candle. " +
                "Warmup bars where insufficient data exists are null. " +
                "Use these with crossover()/crossunder() and other utility functions."
            )
            DocSubSection("Moving Average Series")
            DocCode(mono, """
smaSeries(candles, period)     → number[]
emaSeries(candles, period)     → number[]
wmaSeries(candles, period)     → number[]
vwmaSeries(candles, period)    → number[]
hmaSeries(candles, period)     → number[]
demaSeries(candles, period)    → number[]
temaSeries(candles, period)    → number[]
zlemaSeries(candles, period)   → number[]
kamaSeries(candles, period, fast?, slow?) → number[]
almaSeries(candles, period, offset?, sigma?) → number[]
mcginleySeries(candles, period) → number[]""".trimIndent())
            DocSubSection("Oscillator / Momentum Series")
            DocCode(mono, """
rsiSeries(candles, period)     → number[]
atrSeries(candles, period)     → number[]
adxSeries(candles, period)     → number[]
cciSeries(candles, period)     → number[]
mfiSeries(candles, period)     → number[]
williamsRSeries(candles, period) → number[]
cmfSeries(candles, period)     → number[]
rocSeries(candles, period)     → number[]
momentumSeries(candles, period) → number[]
cmoSeries(candles, period)     → number[]
zScoreSeries(candles, period)  → number[]
stdDevSeries(candles, period)  → number[]
dpoSeries(candles, period)     → number[]
trixSeries(candles, period)    → number[]
natrSeries(candles, period)    → number[]
trueRangeSeries(candles)       → number[]
awesomeOscillatorSeries(candles) → number[]
acceleratorOscillatorSeries(candles) → number[]
historicalVolatilitySeries(candles, period?) → number[]""".trimIndent())
            DocSubSection("Volume Series")
            DocCode(mono, """
obvSeries(candles)             → number[]
vwapSeries(candles)            → number[]
adLineSeries(candles)          → number[]
forceIndexSeries(candles, period?) → number[]
chaikinOscillatorSeries(candles, fast?, slow?)
eomSeries(candles, period?)    → number[]
netVolumeSeries(candles)       → number[]""".trimIndent())
            DocSubSection("Complex Series (Object Returns)")
            DocCode(mono, """
bollingerSeries(candles, period, numStd)
  → {upper:[], middle:[], lower:[]}
macdSeries(candles, fast, slow, signal)
  → {macd:[], signal:[], histogram:[]}
stochasticSeries(candles, kPeriod, dPeriod)
  → {k:[], d:[]}
stochasticRsiSeries(candles, rsiP, stochP, kSmooth, dSmooth)
  → {k:[], d:[]}
supertrendSeries(candles, period, mult)
  → {up:[], down:[], direction:[]}
ichimokuSeries(candles, tenkan, kijun, senkouB)
  → {tenkan:[], kijun:[], senkouA:[], senkouB:[], chikou:[]}
parabolicSarSeries(candles, step?, maxStep?)
  → {sar:[], direction:[]}
keltnerChannelsSeries(candles, period?, mult?, atrP?)
  → {upper:[], middle:[], lower:[]}
donchianChannelsSeries(candles, period?)
  → {upper:[], middle:[], lower:[]}
aroonSeries(candles, period?)
  → {up:[], down:[], oscillator:[]}
vortexSeries(candles, period?) → {plus:[], minus:[]}
elderRaySeries(candles, period?)
  → {bullPower:[], bearPower:[]}
diSeries(candles, period?) → {plus:[], minus:[]}
ppoSeries(candles, fast?, slow?, signal?)
  → {ppo:[], signal:[], histogram:[]}
kstSeries(candles, ...) → {kst:[], signal:[], histogram:[]}
coppockCurveSeries(candles, long?, short?, wma?)
linregSeries(numericArray, period) → number[]
candlePatternSeries(candles) → {pattern, type}[]""".trimIndent())
        }

        // ── 7. Utility & Helper Functions ──
        DocSection("7. Utility & Helper Functions") {
            DocText(
                "These functions work with series arrays and candle data " +
                "to simplify crossover detection, pivots, and data extraction."
            )
            DocSubSection("Crossover Detection")
            DocCode(mono, """
crossover(seriesA, seriesB, index)
  // true if a[i-1] <= b[i-1] && a[i] > b[i]

crossunder(seriesA, seriesB, index)
  // true if a[i-1] >= b[i-1] && a[i] < b[i]

// Example: EMA crossover
var emaF = emaSeries(candles, 9);
var emaS = emaSeries(candles, 21);
var i = candles.length - 1;
if (crossover(emaF, emaS, i)) {
    return {type: 'BUY', sizePct: 2.0};
}""".trimIndent())
            DocSubSection("Lookback Functions")
            DocCode(mono, """
highest(array, period, index)
lowest(array, period, index)
change(array, index, lookback?)
rising(array, period, index?)   // true if values rising
falling(array, period, index?)  // true if values falling
barssince(conditionArr)   // bars since last true
valuewhen(conditionArr, sourceArr, occurrence?)""".trimIndent())
            DocSubSection("Data Extraction")
            DocCode(mono, """
source(candles, field?)
  // field: 'open','high','low','close' (default),
  //        'volume','hl2','hlc3','ohlc4'""".trimIndent())
            DocSubSection("Math / Statistical")
            DocCode(mono, """
sum(arr)          // sum of array values
avg(arr)          // average of array values
median(arr)       // median value
percentile(arr, pct)  // e.g. percentile(closes, 75)
correlation(arr1, arr2)  // Pearson correlation
covariance(arr1, arr2)
beta(candles, benchCandles, period?)  // CAPM beta
nz(val, replacement?)  // replace null/NaN with 0
na(val)            // true if null/undefined/NaN
abs(val)  max(a,b)  min(a,b)  clamp(val,lo,hi)
cum(arr)  // cumulative sum series
resample(candles, factor?)  // merge N bars into 1
dayofweek(candle)  hour(candle)  // time helpers""".trimIndent())
            DocSubSection("Pivot Detection")
            DocCode(mono, """
pivotHigh(candles, leftBars, rightBars)
pivotLow(candles, leftBars, rightBars)
supportResistance(candles, leftBars?, rightBars?, maxLevels?)
  → {resistance:[], support:[]}""".trimIndent())
            DocSubSection("Candle Patterns")
            DocCode(mono, """
candlePattern(candles) → {pattern, type}
  // Detects: doji, hammer, hangingMan, invertedHammer,
  // shootingStar, bullishEngulfing, bearishEngulfing,
  // morningStar, eveningStar, threeWhiteSoldiers,
  // threeBlackCrows, marubozu, spinningTop,
  // tweezerTop, tweezerBottom, piercingLine,
  // darkCloudCover
  // type: 'bullish', 'bearish', or 'neutral'""".trimIndent())
            DocSubSection("Linear Regression & Input")
            DocCode(mono, """
linreg(numericArray, period, index)
linregSeries(numericArray, period)
input(name, defaultValue)""".trimIndent())
        }

        // ── 8. Param System ──
        DocSection("8. Parameter System") {
            DocText(
                "Parameters let you tune strategy settings without editing code logic. " +
                "Declare them at the top of your script as UPPER_SNAKE_CASE var declarations:"
            )
            DocCode(mono, """
var SMA_SHORT = 10;
var SMA_LONG = 50;
var RSI_PERIOD = 14;
var STOP_LOSS = 2.0;""".trimIndent())
            DocText(
                "These are auto-detected and appear in the Settings panel (gear icon). " +
                "Editing a param in settings updates the code, and editing code updates the settings — " +
                "sync is bidirectional.\n\n" +
                "Inside strategy(), reference the variable directly (e.g. SMA_SHORT). " +
                "The engine also exposes a _params object with all values as strings: " +
                "_params.SMA_SHORT === \"10\".\n\n" +
                "When saving a custom template, you can add, remove, or edit params in the dialog."
            )
        }

        // ── 9. Backtest Engine ──
        DocSection("9. Backtest Engine") {
            DocText(
                "The backtester simulates your strategy on historical candle data:"
            )
            DocCode(mono, """
Starting equity:  ${'$'}10,000
Trading fee:      0.1% per trade (entry + exit)
Starts at:        bar 50 (warmup for indicators)
Walk-forward:     evaluates each bar sequentially
TP/SL:            checked per bar using high/low
                  (not just close price)
Short selling:    SELL opens short when no position
_state:           reset to {} at start of backtest
_account:         injected each bar with simulated data
Sharpe ratio:     interval-aware annualization""".trimIndent())
            DocText("Results include:")
            DocBullets(listOf(
                "Total PnL (\$) and PnL %",
                "Number of trades and win rate",
                "Max drawdown (peak-to-trough equity)",
                "Sharpe ratio (annualized per interval)",
                "Profit factor (gross profit / gross loss)",
                "Buy & hold return for comparison",
                "Average bars in trade",
                "Gross profit / gross loss",
                "Average, largest win and loss",
                "Max consecutive wins / losses",
                "Expectancy per trade",
                "Equity curve with chart overlay"
            ))
            DocText(
                "Signal markers (BUY/SELL/CLOSE) are plotted on the chart automatically. " +
                "Console logs from the backtest run appear in the Console panel."
            )
        }

        // ── 10. Built-in Templates ──
        DocSection("10. Built-in Strategy Templates (15)") {
            DocTemplate(
                "GainzAlgo V2",
                "Enhanced engulfing-candle reversal with EMA 50/200 trend filter, volume confirmation, MACD histogram alignment, tightened RSI (buy<45, sell>55), optional ADX strength filter, and ATR-based 1:2 risk-reward TP/SL. Only buys in uptrends, only sells in downtrends.",
                "CANDLE_STABILITY (0.5), RSI_PERIOD (14), RSI_BUY_MAX (45), RSI_SELL_MIN (55), DELTA_LENGTH (4), USE_TREND_FILTER (1), EMA_FAST (50), EMA_SLOW (200), USE_VOLUME_FILTER (1), VOL_LOOKBACK (20), VOL_MULTIPLIER (1.2), USE_MACD_FILTER (1), USE_ADX_FILTER (0), ADX_THRESHOLD (20), TP_SL_MULTIPLIER (1.0), RISK_REWARD_RATIO (2.0), POSITION_SIZE_PCT (2.0)"
            )
            DocTemplate(
                "SMA Crossover",
                "Buys when short SMA crosses above long SMA, sells on cross below.",
                "SMA_SHORT (10), SMA_LONG (50), SIZE_PCT (100), STOP_LOSS (2.0), TAKE_PROFIT (4.0)"
            )
            DocTemplate(
                "RSI Mean Reversion",
                "Buys when RSI drops below oversold, sells when above overbought.",
                "RSI_PERIOD (14), OVERSOLD (30), OVERBOUGHT (70), SIZE_PCT (100)"
            )
            DocTemplate(
                "Bollinger Breakout",
                "Buys on upper band breakout, sells on lower band breakdown.",
                "BB_PERIOD (20), BB_MULT (2.0), SIZE_PCT (100)"
            )
            DocTemplate(
                "MACD Momentum",
                "Buys when MACD crosses above signal line, sells on cross below.",
                "FAST (12), SLOW (26), SIGNAL (9), SIZE_PCT (100)"
            )
            DocTemplate(
                "EMA Scalper",
                "Fast EMA scalping — buys/sells on rapid EMA crossovers.",
                "EMA_FAST (5), EMA_MID (13), EMA_SLOW (50), SIZE_PCT (100), STOP_LOSS (1.0)"
            )
            DocTemplate(
                "Grid Trader",
                "Places buy/sell orders at grid levels around current price.",
                "GRID_SIZE (1.0), NUM_GRIDS (5), SIZE_PCT (20)"
            )
            DocTemplate(
                "Supertrend",
                "Trend-following: buys when price flips above Supertrend line, sells below.",
                "ST_PERIOD (10), ST_MULTIPLIER (3.0), POSITION_SIZE_PCT (2.0), STOP_LOSS_PCT (2.0), TAKE_PROFIT_PCT (4.0)"
            )
            DocTemplate(
                "VWAP Reversion",
                "Mean-reversion around VWAP. Buys when price deviates below VWAP with RSI oversold.",
                "DEV_THRESHOLD (1.5), RSI_PERIOD (14), RSI_OVERSOLD (35), RSI_OVERBOUGHT (65)"
            )
            DocTemplate(
                "Ichimoku Cloud",
                "Tenkan/Kijun cross with cloud filter. Best on 4h/1d charts.",
                "TENKAN_PERIOD (9), KIJUN_PERIOD (26), SENKOU_B_PERIOD (52)"
            )
            DocTemplate(
                "Stochastic Cross",
                "%K/%D crossover in oversold/overbought zones.",
                "K_PERIOD (14), D_PERIOD (3), OVERSOLD (20), OVERBOUGHT (80)"
            )
            DocTemplate(
                "Volume Breakout",
                "Price breakout above/below lookback range with volume spike confirmation.",
                "LOOKBACK (20), VOL_MULTIPLIER (2.0)"
            )
            DocTemplate(
                "5m Scalper ★",
                "High-frequency scalper for 5-min charts. EMA 9/21 cross + RSI(7) + VWAP filter + volume spike. Multi-TP: 50% at 0.3%, 50% at 0.6%. SL: 0.4%.",
                "EMA_FAST (9), EMA_SLOW (21), RSI_PERIOD (7), RSI_BUY_MAX (65), RSI_SELL_MIN (35), VOL_MULT (1.3), TP1_PCT (0.3), TP2_PCT (0.6), SL_PCT (0.4)"
            )
            DocTemplate(
                "15m Swing ★",
                "Momentum swing for 15-min charts. MACD(8,21,5) histogram cross + StochRSI + Supertrend filter. Multi-TP: 33% at 0.8%, 33% at 1.5%, 34% at 2.5%. SL: 1.0%.",
                "MACD_FAST (8), MACD_SLOW (21), MACD_SIGNAL (5), STOCH_RSI_PERIOD (14), ST_PERIOD (7), ST_MULT (2.0), TP1-3_PCT, SL_PCT (1.0)"
            )
            DocTemplate(
                "1h Trend ★",
                "Trend-following for 1-hour charts. Ichimoku Cloud + ADX(14) > 25 + EMA 50/200 golden/death cross. Multi-TP: 30% at 2%, 30% at 4%, 40% at 6%. SL: 2%.",
                "ICH_TENKAN (9), ICH_KIJUN (26), ICH_SENKOU_B (52), ADX_PERIOD (14), ADX_MIN (25), EMA_MID (50), EMA_LONG (200), TP1-3_PCT, SL_PCT (2.0)"
            )
            DocText("★ Templates with graduated multi-level take-profit using takeProfitLevels.")
        }

        // ── 11. Conditional & Time-Based Orders ──
        DocSection("11. Conditional & Time-Based Orders") {
            DocText(
                "Beyond standard signals, you can create orders that trigger based on " +
                "external price conditions or at a scheduled time."
            )
            DocSubSection("Conditional Orders (If-Then)")
            DocText(
                "A conditional order watches a trigger symbol's price. When the condition " +
                "is met, a 'then' order is automatically executed. Conditions:"
            )
            DocBullets(listOf(
                "PRICE_ABOVE — triggers when price rises above the trigger price",
                "PRICE_BELOW — triggers when price drops below the trigger price",
                "PRICE_CROSSES_ABOVE — triggers on the exact crossover moment (was below, now above)",
                "PRICE_CROSSES_BELOW — triggers on the exact crossunder moment (was above, now below)"
            ))
            DocText(
                "Conditional orders are monitored every second by the background trading service. " +
                "Cross-detection uses previous price tracking to detect exact crossing events."
            )
            DocSubSection("Time-Based Orders")
            DocText(
                "Schedule an order to execute at a specific future time. " +
                "Set the scheduledAt field (epoch milliseconds) on an order. " +
                "The trading service checks each second and executes as a market order " +
                "when the current time reaches the scheduled time."
            )
        }

        // ── 12. Theme Switching ──
        DocSection("12. Theme Switching") {
            DocText(
                "The app supports three appearance modes: Dark, Light, and System. " +
                "Navigate to Settings → Appearance to switch themes."
            )
            DocBullets(listOf(
                "Dark — classic dark theme with GitHub-dark inspired palette",
                "Light — clean light theme for daylight usage",
                "System — follows your device's dark/light mode setting"
            ))
            DocText(
                "Theme preference is persisted and applied globally across all screens " +
                "including the navigation bar and chart views."
            )
        }

        // ── 13. Portfolio Allocation Chart ──
        DocSection("13. Portfolio Allocation Chart") {
            DocText(
                "The Portfolio screen's Overview tab displays a donut-style allocation " +
                "pie chart showing the distribution of your holdings by USD value."
            )
            DocBullets(listOf(
                "Assets with value below 2% of total are grouped into 'Other'",
                "Up to 12 distinct colors for clear visual separation",
                "Legend shows asset name and allocation percentage",
                "Only assets with value > \$0.01 are included"
            ))
        }

        // ── 14. Drawing Tools ──
        DocSection("14. Drawing Tools on Chart") {
            DocText(
                "The chart includes 15 drawing tools accessible via the toolbar toggle " +
                "button (pencil icon). Long-press any tool for quick access."
            )
            DocBullets(listOf(
                "Horizontal Line, Vertical Line — static reference levels",
                "Trend Line, Ray, Extended Line, Horizontal Ray — directional lines",
                "Fibonacci Retracement, Fibonacci Extension — key price levels",
                "Rectangle, Channel — area/range markup",
                "Anchored VWAP — volume-weighted average from a chosen point",
                "Text Label — annotate the chart with notes",
                "Price Range, Measure Tool — calculate distances and percentages",
                "Alert Line — set visual alerts at price levels"
            ))
            DocText(
                "Drawing tools support undo, clear all, color picker, " +
                "selection/move of existing drawings, and delete."
            )
        }

        // ── 15. Backtest vs Buy & Hold Comparison ──
        DocSection("15. Backtest vs Buy & Hold Comparison") {
            DocText(
                "After running a backtest, tap 'Compare vs Buy & Hold' to overlay " +
                "your strategy's equity curve against a simple buy-and-hold benchmark."
            )
            DocBullets(listOf(
                "Blue line — your strategy's equity over time",
                "Gold line — buy-and-hold equity (starting capital × price ratio)",
                "Summary shows Strategy Return %, Buy & Hold Return %, and Alpha (difference)"
            ))
            DocText(
                "Alpha > 0 means your strategy outperformed simply holding the asset. " +
                "This helps evaluate whether active trading adds value over passive holding."
            )
        }

        // ── 16. Monaco Code Editor ──
        DocSection("16. Monaco Code Editor") {
            DocText(
                "The strategy editor uses the Monaco Editor (same engine as VS Code) " +
                "with full IntelliSense support for the TFG strategy API."
            )
            DocBullets(listOf(
                "Syntax highlighting with custom 'tfg-strategy' language definition",
                "60+ autocomplete suggestions with documentation for all indicator functions",
                "Inline error detection — bracket matching and basic syntax validation",
                "Dracula-inspired dark theme matching the app's aesthetic",
                "Smart bracket completion and auto-indentation"
            ))
            DocText(
                "The editor loads via CDN and requires internet connectivity on first load. " +
                "Type any function name (e.g. 'sma', 'crossover', 'buy') to see autocomplete suggestions."
            )
        }

        // ── 17. Tips ──
        DocSection("17. Tips & Best Practices") {
            DocBullets(listOf(
                "Write pure JavaScript — no transpilation, what you write is what runs",
                "Always use enough candles — indicators like sma(candles, 50) need at least 50 bars",
                "Use stopLossPct and takeProfitPct to manage risk automatically",
                "Use takeProfitLevels for graduated exits (scale out partially at each level)",
                "Start with longer timeframes (1h, 4h) for more reliable signals",
                "Test different param values using the Settings panel (gear icon) before going live",
                "Save working strategies as custom templates for reuse",
                "Return {type:'HOLD'} when no signal — never return undefined or nothing",
                "Use _state to track variables across bars (e.g. _state.tradeCount, _state.prevRsi)",
                "Use console.log() to debug — output appears in the Console panel after backtest",
                "Check _account.positionSide before opening new positions to avoid doubling up",
                "Use source(candles, 'hlc3') to get typical price series for custom calculations",
                "Use crossover()/crossunder() with series for clean signal detection",
                "Use pivotHigh()/pivotLow() to detect support and resistance levels",
                "All strategies auto-save before running a backtest",
                "5-second execution timeout — avoid heavy loops over all candles",
                "The QuickJS context caches between bars — editing code or params rebuilds it"
            ))
        }

        // ── 18. Quick Reference ──
        DocSection("18. Quick Reference — Complete Function List") {
            DocCode(mono, """
// ── SINGLE-VALUE ──────────────────────
sma  ema  wma  vwma  hullMa  dema  tema
rsi  atr  adx  cci  williamsR  mfi  roc
zScore  obv  vwap  cmf  stdDev
bollinger  macd  stochastic  supertrend
ichimoku  stochasticRsi

// ── SERIES ────────────────────────────
smaSeries  emaSeries  wmaSeries  vwmaSeries
hmaSeries  demaSeries  temaSeries  rsiSeries
atrSeries  adxSeries  cciSeries  mfiSeries
williamsRSeries  cmfSeries  obvSeries
vwapSeries  bollingerSeries  macdSeries
stochasticSeries  stochasticRsiSeries
supertrendSeries  ichimokuSeries
linregSeries

// ── UTILITIES ─────────────────────────
crossover(a, b, i)  crossunder(a, b, i)
highest(arr, n, i)  lowest(arr, n, i)
change(arr, i, lb)  source(candles, field)
linreg(arr, n, i)   pivotHigh(c, l, r)
pivotLow(c, l, r)   input(name, default)

// ── GLOBALS ───────────────────────────
_candles  _params  _state  _account
console.log/warn/error/info  color.*""".trimIndent())
        }

        DocSection("19. Replay Mode") {
            DocCode(mono, """
Replay Mode lets you step through historical
candles bar-by-bar, watching your strategy
decide in real time.

HOW TO USE:
1. Open any chart on the Chart screen
2. Tap "▶ Replay Mode" (orange button)
3. Controls appear:
   ⏮ Step Back  |  ▶ Play / ⏸ Pause  |  ⏭ Step Forward
4. Use the slider to seek to any bar
5. Adjust speed: 1x / 2x / 4x / 10x
6. Signal counter shows BUY/SELL counts

HOW IT WORKS:
• When you enter Replay, the chart shows only
  the first 50 candles (or all if fewer)
• Stepping forward reveals one more candle
• If a strategy is loaded, it evaluates signals
  at each bar using StrategyEvaluator
• Signal markers (BUY ▲, SELL ▼) appear on chart
• Play mode auto-advances using the selected speed
• Exit returns to full chart view""".trimIndent())
        }

        DocSection("20. Visual Strategy Builder") {
            DocCode(mono, """
Build strategies without writing any code
using drag-and-drop blocks.

TAB: Script → Visual (5th tab)

WORKFLOW:
1. Switch to the "Visual" tab
2. Each Rule has:
   • Action: BUY, SELL, CLOSE_IF_LONG, CLOSE_IF_SHORT
   • Conditions: add blocks from 20 indicators
   • Logic: AND / OR to combine conditions
   • Parameters: Size %, Stop Loss %, Take Profit %
3. Press "Add Condition" to open the block picker
4. Choose from categories:
   - Trend: SMA, EMA, Supertrend, ADX
   - Crossover: SMA/EMA/MACD crossover/crossunder
   - Oscillator: RSI, Stochastic
   - Volatility: Bollinger Bands
   - Volume: Volume Spike
5. Configure each block's parameters
6. Add multiple Rules for complex strategies
7. Press "Generate" to create JavaScript code
8. Review the code preview and tap
   "Use This Strategy" to send it to the Editor

EXAMPLE: RSI Reversal Strategy
 Rule 1: IF RSI(14) < 30 AND Price < BB Lower
          → BUY  Size:2%  SL:3%  TP:5%
 Rule 2: IF RSI(14) > 70 AND Price > BB Upper
          → SELL Size:2%  SL:3%  TP:5%""".trimIndent())
        }

        DocSection("21. Custom Indicator Plotting Pipeline") {
            DocCode(mono, """
Write custom JS indicators and see them
plotted directly on the chart WebView.

HOW IT WORKS:
1. Define an indicator function in script:
   function indicator(candles) {
     // Return an IndicatorOutput object
     return {
       name: "My RSI",
       type: "line",    // line, histogram, area
       panel: "below",  // overlay, below
       color: "#FF6B6B",
       values: rsiSeries(candles, 14)
     };
   }
2. Save to "My Indicators" in the app
3. The pipeline automatically:
   a) IndicatorEngine (QuickJS) evaluates your JS
   b) Produces IndicatorOutput with values array
   c) chart.html receives setCustomIndicators()
   d) Renders overlay on price chart or new panel

INDICATOR OUTPUT FORMAT:
{
  name: string,        // Display name
  type: "line" | "histogram" | "area",
  panel: "overlay" | "below",
  color: "#RRGGBB",
  values: number[]     // One value per candle
}

AUTOMATIC REFRESH:
Custom indicators re-render when:
• Candle data changes (interval switch / refresh)
• Indicator list is modified
• Backtest candles are loaded""".trimIndent())
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ─── Visual Strategy Builder ────────────────────────────────────────

private enum class BlockType(val label: String, val category: String) {
    RSI_BELOW("RSI < Value", "Oscillator"),
    RSI_ABOVE("RSI > Value", "Oscillator"),
    PRICE_ABOVE_SMA("Price > SMA", "Trend"),
    PRICE_BELOW_SMA("Price < SMA", "Trend"),
    PRICE_ABOVE_EMA("Price > EMA", "Trend"),
    PRICE_BELOW_EMA("Price < EMA", "Trend"),
    SMA_CROSS_ABOVE("SMA Fast ↗ SMA Slow", "Crossover"),
    SMA_CROSS_BELOW("SMA Fast ↘ SMA Slow", "Crossover"),
    EMA_CROSS_ABOVE("EMA Fast ↗ EMA Slow", "Crossover"),
    EMA_CROSS_BELOW("EMA Fast ↘ EMA Slow", "Crossover"),
    MACD_CROSS_ABOVE("MACD ↗ Signal", "Crossover"),
    MACD_CROSS_BELOW("MACD ↘ Signal", "Crossover"),
    PRICE_BELOW_BB_LOWER("Price < BB Lower", "Volatility"),
    PRICE_ABOVE_BB_UPPER("Price > BB Upper", "Volatility"),
    VOLUME_SPIKE("Volume Spike", "Volume"),
    STOCH_OVERSOLD("Stoch %K < 20", "Oscillator"),
    STOCH_OVERBOUGHT("Stoch %K > 80", "Oscillator"),
    ADX_STRONG("ADX > Value", "Trend"),
    SUPERTREND_BULLISH("Supertrend Bullish", "Trend"),
    SUPERTREND_BEARISH("Supertrend Bearish", "Trend"),
}

private data class ConditionBlock(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: BlockType,
    val params: MutableMap<String, String> = mutableMapOf()
)

private data class VisualRule(
    val id: String = java.util.UUID.randomUUID().toString(),
    val action: String = "BUY", // BUY, SELL, CLOSE_IF_LONG, CLOSE_IF_SHORT
    val conditions: List<ConditionBlock> = emptyList(),
    val logic: String = "AND", // AND, OR
    val sizePct: String = "2.0",
    val stopLossPct: String = "",
    val takeProfitPct: String = ""
)

@Composable
private fun VisualBuilderTab(onGenerateCode: (String) -> Unit) {
    var rules by remember { mutableStateOf(listOf(VisualRule())) }
    var showBlockPicker by remember { mutableStateOf<String?>(null) } // rule ID requesting new block
    var generatedCode by remember { mutableStateOf("") }
    var showPreview by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Visual Strategy Builder", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AccentBlue)
                Text("Build strategies without writing code", fontSize = 11.sp, color = TextSecondary)
            }
            Button(
                onClick = {
                    generatedCode = generateStrategyCode(rules)
                    showPreview = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Code, null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Generate", fontSize = 12.sp)
            }
        }

        // Rules
        rules.forEachIndexed { ruleIndex, rule ->
            VisualRuleCard(
                rule = rule,
                ruleIndex = ruleIndex,
                onUpdateRule = { updated ->
                    rules = rules.toMutableList().also { it[ruleIndex] = updated }
                },
                onDeleteRule = {
                    if (rules.size > 1) rules = rules.toMutableList().also { it.removeAt(ruleIndex) }
                },
                onAddCondition = { showBlockPicker = rule.id },
                onRemoveCondition = { condId ->
                    val updatedConds = rule.conditions.filter { it.id != condId }
                    rules = rules.toMutableList().also { it[ruleIndex] = rule.copy(conditions = updatedConds) }
                }
            )
        }

        // Add rule button
        OutlinedButton(
            onClick = { rules = rules + VisualRule() },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.3f))
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp), tint = AccentBlue)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add Rule", color = AccentBlue, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Block picker dialog
    showBlockPicker?.let { ruleId ->
        BlockPickerDialog(
            onSelect = { blockType ->
                val ruleIdx = rules.indexOfFirst { it.id == ruleId }
                if (ruleIdx >= 0) {
                    val rule = rules[ruleIdx]
                    val newBlock = ConditionBlock(type = blockType, params = getDefaultParams(blockType).toMutableMap())
                    rules = rules.toMutableList().also {
                        it[ruleIdx] = rule.copy(conditions = rule.conditions + newBlock)
                    }
                }
                showBlockPicker = null
            },
            onDismiss = { showBlockPicker = null }
        )
    }

    // Code preview dialog
    if (showPreview && generatedCode.isNotEmpty()) {
        Dialog(onDismissRequest = { showPreview = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DarkSurface,
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.7f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Generated Strategy Code", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        IconButton(onClick = { showPreview = false }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, "Close", tint = TextTertiary, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        generatedCode,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFFA9DC76),
                        lineHeight = 15.sp,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Color(0xFF1A1A2E), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                            .verticalScroll(rememberScrollState())
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            onGenerateCode(generatedCode)
                            showPreview = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Send, null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Use This Strategy", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun VisualRuleCard(
    rule: VisualRule,
    ruleIndex: Int,
    onUpdateRule: (VisualRule) -> Unit,
    onDeleteRule: () -> Unit,
    onAddCondition: () -> Unit,
    onRemoveCondition: (String) -> Unit
) {
    val actionColor = when (rule.action) {
        "BUY" -> Green400
        "SELL" -> Red400
        else -> AccentOrange
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DarkCard,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Rule header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Rule ${ruleIndex + 1}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                IconButton(onClick = onDeleteRule, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, "Delete", tint = Red400, modifier = Modifier.size(16.dp))
                }
            }

            // Action selector
            Text("Action", fontSize = 10.sp, color = TextTertiary)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("BUY", "SELL", "CLOSE_IF_LONG", "CLOSE_IF_SHORT").forEach { action ->
                    val sel = rule.action == action
                    val c = when (action) {
                        "BUY" -> Green400; "SELL" -> Red400; else -> AccentOrange
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (sel) c.copy(alpha = 0.2f) else DarkSurface)
                            .border(1.dp, if (sel) c else DarkBorder, RoundedCornerShape(6.dp))
                            .clickable { onUpdateRule(rule.copy(action = action)) }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            action.replace("_", " "),
                            fontSize = 10.sp,
                            color = if (sel) c else TextSecondary,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // Logic selector (AND/OR)
            if (rule.conditions.size > 1) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Combine with: ", fontSize = 10.sp, color = TextTertiary)
                    listOf("AND", "OR").forEach { logic ->
                        val sel = rule.logic == logic
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (sel) AccentBlue.copy(alpha = 0.2f) else Color.Transparent)
                                .clickable { onUpdateRule(rule.copy(logic = logic)) }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(logic, fontSize = 11.sp,
                                color = if (sel) AccentBlue else TextTertiary,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }

            // Conditions
            Text("IF", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = actionColor)
            rule.conditions.forEachIndexed { idx, cond ->
                if (idx > 0) {
                    Text(rule.logic, fontSize = 10.sp, color = AccentBlue,
                        modifier = Modifier.padding(start = 16.dp))
                }
                ConditionBlockCard(
                    block = cond,
                    onUpdateBlock = { updated ->
                        val updatedConds = rule.conditions.toMutableList()
                        updatedConds[idx] = updated
                        onUpdateRule(rule.copy(conditions = updatedConds))
                    },
                    onRemove = { onRemoveCondition(cond.id) }
                )
            }

            // Add condition button
            OutlinedButton(
                onClick = onAddCondition,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(6.dp),
                border = BorderStroke(1.dp, DarkBorder)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp), tint = TextSecondary)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Condition", fontSize = 11.sp, color = TextSecondary)
            }

            // Size / SL / TP (for BUY/SELL)
            if (rule.action == "BUY" || rule.action == "SELL") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CompactField("Size %", rule.sizePct, Modifier.weight(1f)) {
                        onUpdateRule(rule.copy(sizePct = it))
                    }
                    CompactField("SL %", rule.stopLossPct, Modifier.weight(1f)) {
                        onUpdateRule(rule.copy(stopLossPct = it))
                    }
                    CompactField("TP %", rule.takeProfitPct, Modifier.weight(1f)) {
                        onUpdateRule(rule.copy(takeProfitPct = it))
                    }
                }
            }

            // THEN action label
            Text("→ THEN ${rule.action.replace("_", " ")}", fontSize = 12.sp,
                fontWeight = FontWeight.Bold, color = actionColor)
        }
    }
}

@Composable
private fun ConditionBlockCard(
    block: ConditionBlock,
    onUpdateBlock: (ConditionBlock) -> Unit,
    onRemove: () -> Unit
) {
    val blockColor = when (block.type.category) {
        "Trend" -> Color(0xFF2196F3)
        "Crossover" -> AccentPurple
        "Oscillator" -> AccentOrange
        "Volatility" -> AccentGold
        "Volume" -> Green400
        else -> TextSecondary
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = blockColor.copy(alpha = 0.08f),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, blockColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(blockColor, RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(block.type.label, fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                // Parameter fields
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    block.params.forEach { (key, value) ->
                        CompactField(key, value, Modifier.width(70.dp)) { newVal ->
                            val updated = block.params.toMutableMap()
                            updated[key] = newVal
                            onUpdateBlock(block.copy(params = updated))
                        }
                    }
                }
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, "Remove", tint = TextTertiary, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun CompactField(label: String, value: String, modifier: Modifier, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 8.sp, maxLines = 1) },
        singleLine = true,
        modifier = modifier.height(46.dp),
        textStyle = androidx.compose.ui.text.TextStyle(
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = TextPrimary
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AccentBlue,
            unfocusedBorderColor = DarkBorder,
            cursorColor = AccentBlue,
            focusedContainerColor = DarkSurface,
            unfocusedContainerColor = DarkSurface
        )
    )
}

@Composable
private fun BlockPickerDialog(onSelect: (BlockType) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = DarkSurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Add Condition", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(12.dp))
                val grouped = BlockType.entries.groupBy { it.category }
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    grouped.forEach { (category, blocks) ->
                        val catColor = when (category) {
                            "Trend" -> Color(0xFF2196F3)
                            "Crossover" -> AccentPurple
                            "Oscillator" -> AccentOrange
                            "Volatility" -> AccentGold
                            "Volume" -> Green400
                            else -> TextSecondary
                        }
                        Text(category, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = catColor)
                        blocks.forEach { bt ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = catColor.copy(alpha = 0.08f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(bt) }
                                    .border(1.dp, catColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(Modifier.size(6.dp).background(catColor, RoundedCornerShape(3.dp)))
                                    Spacer(Modifier.width(8.dp))
                                    Text(bt.label, fontSize = 12.sp, color = TextPrimary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getDefaultParams(type: BlockType): Map<String, String> = when (type) {
    BlockType.RSI_BELOW, BlockType.RSI_ABOVE -> mapOf("Period" to "14", "Value" to if (type == BlockType.RSI_BELOW) "30" else "70")
    BlockType.PRICE_ABOVE_SMA, BlockType.PRICE_BELOW_SMA -> mapOf("Period" to "50")
    BlockType.PRICE_ABOVE_EMA, BlockType.PRICE_BELOW_EMA -> mapOf("Period" to "21")
    BlockType.SMA_CROSS_ABOVE, BlockType.SMA_CROSS_BELOW -> mapOf("Fast" to "10", "Slow" to "50")
    BlockType.EMA_CROSS_ABOVE, BlockType.EMA_CROSS_BELOW -> mapOf("Fast" to "9", "Slow" to "21")
    BlockType.MACD_CROSS_ABOVE, BlockType.MACD_CROSS_BELOW -> mapOf("Fast" to "12", "Slow" to "26", "Sig" to "9")
    BlockType.PRICE_BELOW_BB_LOWER, BlockType.PRICE_ABOVE_BB_UPPER -> mapOf("Period" to "20", "Mult" to "2.0")
    BlockType.VOLUME_SPIKE -> mapOf("Mult" to "2.0")
    BlockType.STOCH_OVERSOLD, BlockType.STOCH_OVERBOUGHT -> mapOf("K" to "14", "D" to "3")
    BlockType.ADX_STRONG -> mapOf("Period" to "14", "Value" to "25")
    BlockType.SUPERTREND_BULLISH, BlockType.SUPERTREND_BEARISH -> mapOf("Period" to "10", "Mult" to "3.0")
}

private fun generateStrategyCode(rules: List<VisualRule>): String {
    val sb = StringBuilder()
    sb.appendLine("// Auto-generated by Visual Strategy Builder")
    sb.appendLine("// Modify freely in the Editor tab")
    sb.appendLine()

    // Collect all needed params as var declarations
    val allParams = mutableSetOf<String>()
    rules.forEach { rule ->
        rule.conditions.forEach { cond ->
            cond.params.forEach { (key, value) ->
                val paramName = "${cond.type.name}_${key}".uppercase()
                allParams.add("var $paramName = $value;")
            }
        }
        if (rule.sizePct.isNotBlank()) allParams.add("var POSITION_SIZE_PCT = ${rule.sizePct};")
        if (rule.stopLossPct.isNotBlank()) allParams.add("var STOP_LOSS_PCT = ${rule.stopLossPct};")
        if (rule.takeProfitPct.isNotBlank()) allParams.add("var TAKE_PROFIT_PCT = ${rule.takeProfitPct};")
    }
    allParams.sorted().forEach { sb.appendLine(it) }
    sb.appendLine()

    sb.appendLine("function strategy(candles) {")
    sb.appendLine("    var i = candles.length - 1;")
    sb.appendLine("    var last = candles[i];")
    sb.appendLine("    if (candles.length < 60) return {type: 'HOLD'};")
    sb.appendLine()

    rules.forEach { rule ->
        val conditions = rule.conditions.map { cond -> generateConditionCode(cond) }
        if (conditions.isEmpty()) return@forEach

        val joiner = if (rule.logic == "AND") " && " else " || "
        val condExpr = conditions.joinToString(joiner)
        sb.appendLine("    // Rule: ${rule.action}")
        sb.appendLine("    if ($condExpr) {")

        when (rule.action) {
            "BUY", "SELL" -> {
                sb.append("        return {type: '${rule.action}'")
                if (rule.sizePct.isNotBlank()) sb.append(", sizePct: POSITION_SIZE_PCT")
                if (rule.stopLossPct.isNotBlank()) sb.append(", stopLossPct: STOP_LOSS_PCT")
                if (rule.takeProfitPct.isNotBlank()) sb.append(", takeProfitPct: TAKE_PROFIT_PCT")
                sb.appendLine("};")
            }
            else -> sb.appendLine("        return {type: '${rule.action}'};")
        }
        sb.appendLine("    }")
        sb.appendLine()
    }

    sb.appendLine("    return {type: 'HOLD'};")
    sb.appendLine("}")
    return sb.toString()
}

private fun generateConditionCode(cond: ConditionBlock): String {
    val p = cond.params
    return when (cond.type) {
        BlockType.RSI_BELOW -> "rsi(candles, ${p["Period"] ?: "14"}) < ${p["Value"] ?: "30"}"
        BlockType.RSI_ABOVE -> "rsi(candles, ${p["Period"] ?: "14"}) > ${p["Value"] ?: "70"}"
        BlockType.PRICE_ABOVE_SMA -> "last.close > sma(candles, ${p["Period"] ?: "50"})"
        BlockType.PRICE_BELOW_SMA -> "last.close < sma(candles, ${p["Period"] ?: "50"})"
        BlockType.PRICE_ABOVE_EMA -> "last.close > ema(candles, ${p["Period"] ?: "21"})"
        BlockType.PRICE_BELOW_EMA -> "last.close < ema(candles, ${p["Period"] ?: "21"})"
        BlockType.SMA_CROSS_ABOVE -> {
            val f = p["Fast"] ?: "10"; val s = p["Slow"] ?: "50"
            "crossover(smaSeries(candles, $f), smaSeries(candles, $s), i)"
        }
        BlockType.SMA_CROSS_BELOW -> {
            val f = p["Fast"] ?: "10"; val s = p["Slow"] ?: "50"
            "crossunder(smaSeries(candles, $f), smaSeries(candles, $s), i)"
        }
        BlockType.EMA_CROSS_ABOVE -> {
            val f = p["Fast"] ?: "9"; val s = p["Slow"] ?: "21"
            "crossover(emaSeries(candles, $f), emaSeries(candles, $s), i)"
        }
        BlockType.EMA_CROSS_BELOW -> {
            val f = p["Fast"] ?: "9"; val s = p["Slow"] ?: "21"
            "crossunder(emaSeries(candles, $f), emaSeries(candles, $s), i)"
        }
        BlockType.MACD_CROSS_ABOVE -> {
            val f = p["Fast"] ?: "12"; val s = p["Slow"] ?: "26"; val sig = p["Sig"] ?: "9"
            "(function(){ var m = macdSeries(candles, $f, $s, $sig); return crossover(m.macd, m.signal, i); })()"
        }
        BlockType.MACD_CROSS_BELOW -> {
            val f = p["Fast"] ?: "12"; val s = p["Slow"] ?: "26"; val sig = p["Sig"] ?: "9"
            "(function(){ var m = macdSeries(candles, $f, $s, $sig); return crossunder(m.macd, m.signal, i); })()"
        }
        BlockType.PRICE_BELOW_BB_LOWER -> {
            val per = p["Period"] ?: "20"; val mult = p["Mult"] ?: "2.0"
            "last.close < bollinger(candles, $per, $mult).lower"
        }
        BlockType.PRICE_ABOVE_BB_UPPER -> {
            val per = p["Period"] ?: "20"; val mult = p["Mult"] ?: "2.0"
            "last.close > bollinger(candles, $per, $mult).upper"
        }
        BlockType.VOLUME_SPIKE -> {
            val mult = p["Mult"] ?: "2.0"
            "(function(){ var vs = source(candles, 'volume'); var avg = 0; for(var j = Math.max(0, i-20); j < i; j++) avg += vs[j]; avg /= Math.min(20, i); return last.volume > avg * $mult; })()"
        }
        BlockType.STOCH_OVERSOLD -> {
            val k = p["K"] ?: "14"; val d = p["D"] ?: "3"
            "stochastic(candles, $k, $d)[0] < 20"
        }
        BlockType.STOCH_OVERBOUGHT -> {
            val k = p["K"] ?: "14"; val d = p["D"] ?: "3"
            "stochastic(candles, $k, $d)[0] > 80"
        }
        BlockType.ADX_STRONG -> "adx(candles, ${p["Period"] ?: "14"}) > ${p["Value"] ?: "25"}"
        BlockType.SUPERTREND_BULLISH -> {
            val per = p["Period"] ?: "10"; val mult = p["Mult"] ?: "3.0"
            "supertrend(candles, $per, $mult)[1] === true"
        }
        BlockType.SUPERTREND_BEARISH -> {
            val per = p["Period"] ?: "10"; val mult = p["Mult"] ?: "3.0"
            "supertrend(candles, $per, $mult)[1] === false"
        }
    }
}

@Composable
private fun DocSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AccentBlue)
        content()
    }
}

@Composable
private fun DocSubSection(title: String) {
    Text(
        title,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextPrimary,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun DocText(text: String) {
    Text(text, fontSize = 12.sp, color = TextSecondary, lineHeight = 18.sp)
}

@Composable
private fun DocCode(mono: androidx.compose.ui.text.font.FontFamily, code: String) {
    Text(
        text = code,
        fontSize = 11.sp,
        fontFamily = mono,
        color = Color(0xFFA9DC76),
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A2E), RoundedCornerShape(8.dp))
            .padding(10.dp)
    )
}

@Composable
private fun DocBullets(items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEach { item ->
            Row {
                Text("•  ", fontSize = 12.sp, color = AccentBlue)
                Text(item, fontSize = 12.sp, color = TextSecondary, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
private fun DocTemplate(name: String, desc: String, params: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A2E), RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(desc, fontSize = 11.sp, color = TextSecondary)
        Text("Params: $params", fontSize = 10.sp, color = TextTertiary)
    }
}
