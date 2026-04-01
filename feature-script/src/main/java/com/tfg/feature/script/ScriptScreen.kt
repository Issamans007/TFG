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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    var showSaveDialog by remember { mutableStateOf(false) }
    var showBacktestDialog by remember { mutableStateOf(false) }
    var showTemplates by remember { mutableStateOf(false) }
    var showPairPicker by remember { mutableStateOf(false) }
    var showSaveTemplateDialog by remember { mutableStateOf(false) }
    var currentTab by remember { mutableIntStateOf(0) } // 0=Editor, 1=Templates, 2=My Strategies, 3=Docs

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
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.runBacktest(symbol, interval, days.toIntOrNull() ?: 30)
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
        TabRow(
            selectedTabIndex = currentTab,
            containerColor = DarkSurface,
            contentColor = AccentBlue
        ) {
            Tab(selected = currentTab == 0, onClick = { currentTab = 0 },
                text = { Text("Editor", fontSize = 12.sp) })
            Tab(selected = currentTab == 1, onClick = { currentTab = 1 },
                text = { Text("Templates", fontSize = 12.sp) })
            Tab(selected = currentTab == 2, onClick = { currentTab = 2 },
                text = { Text("Strategies", fontSize = 12.sp) })
            Tab(selected = currentTab == 3, onClick = { currentTab = 3 },
                text = { Text("Docs", fontSize = 12.sp) })
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
            TradingViewChart(
                candles = state.candles,
                signals = state.signalMarkers,
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

        // Code editor with syntax highlighting
        CodeEditor(
            code = state.code,
            onCodeChange = { viewModel.updateCode(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            minHeight = 300.dp
        )

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
    val mono = androidx.compose.ui.text.font.FontFamily.Monospace
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

        // ── 1. Overview ──
        DocSection("How the Code Engine Works") {
            DocText(
                "Each strategy is a pure JavaScript function evaluated by the QuickJS engine. " +
                "When you run a backtest or activate a strategy, the engine calls your " +
                "strategy(candles) function for each candle bar.\n\n" +
                "If your code differs from the built-in template, the engine executes your " +
                "custom JavaScript directly in a sandboxed QuickJS context. This means your " +
                "edits to strategy logic actually run — it's not just a facade.\n\n" +
                "Your script receives two inputs:\n" +
                "• candles – an array of price candle objects from Binance\n" +
                "• _params – an object with your configurable settings (key → value)\n\n" +
                "Your strategy() function must return a signal object " +
                "(e.g. {type:'BUY', sizePct:2.0}) for each candle bar."
            )
        }

        // ── 2. Candle Data ──
        DocSection("Candle Data from Binance") {
            DocText(
                "Candles are fetched via the Binance Klines API. Each candle is a JS object:"
            )
            DocCode(mono, """
{
  openTime,      // epoch ms – candle open time
  open,          // opening price
  high,          // highest price in period
  low,           // lowest price in period
  close,         // closing price
  volume         // base asset volume
}""".trimIndent())
            DocText(
                "Available intervals: 1m, 3m, 5m, 15m, 30m, 1h, 2h, 4h, 6h, 8h, 12h, 1d, 3d, 1w, 1M\n" +
                "Maximum: 1000 candles per API call (default 500)."
            )
        }

        // ── 3. Strategy Signals ──
        DocSection("Strategy Signals") {
            DocText("Your strategy() function must return one of these signal objects:")
            DocCode(mono, """
{type:'HOLD'}             // Do nothing
{type:'BUY',
  sizePct: 2.0,           // % of equity to use
  stopLossPct: 1.5,       // optional SL %
  takeProfitPct: 3.0      // optional TP %
}
{type:'SELL',
  sizePct: 2.0,
  stopLossPct: 1.5,
  takeProfitPct: 3.0
}
{type:'CLOSE_IF_LONG'}    // Close open long
{type:'CLOSE_IF_SHORT'}   // Close open short""".trimIndent())
            DocText(
                "BUY opens a long, SELL opens a short. " +
                "sizePct controls position sizing as a percentage of total equity. " +
                "stopLossPct / takeProfitPct are optional automatic exit levels."
            )
        }

        // ── 4. Indicator Functions ──
        DocSection("Available Indicator Functions") {
            DocText("All indicators operate on a candles array and return numbers or objects.")

            DocSubSection("Single-value (latest bar)")
            DocCode(mono, """
sma(candles, period)        // Simple Moving Average
ema(candles, period)        // Exponential Moving Average
wma(candles, period)        // Weighted Moving Average
vwma(candles, period)       // Volume-Weighted MA
hullMa(candles, period)     // Hull Moving Average
dema(candles, period)       // Double EMA
tema(candles, period)       // Triple EMA
rsi(candles, period)        // RSI (0-100)
atr(candles, period)        // Average True Range
adx(candles, period)        // ADX (Wilder smoothed)
cci(candles, period)        // Commodity Channel Index
williamsR(candles, period)  // Williams %R (-100 to 0)
obv(candles)                // On-Balance Volume
vwap(candles)               // Volume-Weighted Avg Price
mfi(candles, period)        // Money Flow Index
cmf(candles, period)        // Chaikin Money Flow
roc(candles, period)        // Rate of Change
zScore(candles, period)     // Z-Score of latest close
stdDev(values)              // Standard Deviation""".trimIndent())

            DocSubSection("Object / Array returns")
            DocCode(mono, """
stochastic(candles, kPeriod, dPeriod)
  → [k, d]  // %K and %D (0-100)

supertrend(candles, period, multiplier)
  → [line, isUptrend]  // trend line and direction

bollinger(candles, period, numStd)
  → {upper, middle, lower}

macd(candles, fast, slow, sig)
  → {macd, signal, histogram}

ichimoku(candles, tenkan, kijun, senkouB)
  → {tenkan, kijun, senkouA, senkouB, chikou}""".trimIndent())

            DocSubSection("Series (full history)")
            DocCode(mono, """
smaSeries(candles, period)   // Array of numbers
emaSeries(candles, period)   // Array of numbers
rsiSeries(candles, period)   // Array of numbers""".trimIndent())

            DocSubSection("Complex Indicators")
            DocCode(mono, """
bollingerSeries(candles, period, multiplier)
  → {upper, middle, lower}  // each Array

macdSeries(candles, fast, slow, signal)
  → {macd, signal, histogram}  // each Array""".trimIndent())
        }

        // ── 5. Param System ──
        DocSection("Parameter System") {
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
                "These are auto-detected and appear in the Settings panel on the right. " +
                "Editing a param in settings updates the code, and editing code updates the settings. " +
                "Sync is bidirectional.\n\n" +
                "Inside your strategy function, just reference the variable name directly " +
                "(e.g. SMA_SHORT). The engine also exposes a _params object with all param values.\n\n" +
                "When saving a custom template, you can add/remove/edit params in the dialog."
            )
        }

        // ── 6. Backtest Engine ──
        DocSection("Backtest Engine") {
            DocText(
                "The backtester simulates your strategy on historical candle data:"
            )
            DocCode(mono, """
Starting equity:  ${'$'}10,000
Trading fee:      0.1% per trade (buy & sell)
Starts at:        bar 50 (enough data for indicators)
Walk-forward:     evaluates each bar sequentially
TP/SL:            checked each bar using high/low
Short selling:    SELL opens short when no position
Sharpe ratio:     interval-aware annualization""".trimIndent())
            DocText("Results include:")
            DocBullets(listOf(
                "Total PnL ($) and PnL %",
                "Number of trades and win rate",
                "Max drawdown (peak-to-trough)",
                "Sharpe ratio (interval-aware annualization)",
                "Profit factor (gross profit / gross loss)",
                "Buy & hold return for comparison",
                "Average bars in trade",
                "Gross profit / gross loss",
                "Average, largest win and loss",
                "Max consecutive wins / losses",
                "Expectancy per trade",
                "Equity curve"
            ))
        }

        // ── 7. Built-in Templates ──
        DocSection("Built-in Strategy Templates") {
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
                "Fast EMA scalping – buys/sells on rapid EMA crossovers.",
                "EMA_FAST (5), EMA_MID (13), EMA_SLOW (50), SIZE_PCT (100), STOP_LOSS (1.0)"
            )
            DocTemplate(
                "Grid Trader",
                "Places buy/sell orders at grid levels around current price.",
                "GRID_SIZE (1.0), NUM_GRIDS (5), SIZE_PCT (20)"
            )
            DocTemplate(
                "GainzAlgo",
                "Multi-indicator trend-following strategy combining EMA, RSI, MACD, and ADX.",
                "EMA_FAST (8), EMA_SLOW (21), RSI_PERIOD (14), RSI_LOW (40), RSI_HIGH (60), " +
                "MACD_FAST (12), MACD_SLOW (26), MACD_SIGNAL (9), ADX_PERIOD (14), ADX_MIN (20), " +
                "SIZE_PCT (100), STOP_LOSS (2.5), TAKE_PROFIT (5.0)"
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
        }

        // ── 8. Tips ──
        DocSection("Tips & Best Practices") {
            DocBullets(listOf(
                "Write pure JavaScript — no transpilation, what you write is what runs",
                "Always use enough candles – indicators like sma(candles, 50) need at least 50 bars",
                "Use stopLossPct and takeProfitPct to manage risk automatically",
                "Start with longer timeframes (1h, 4h) for more reliable signals",
                "Test different param values using the Settings panel before going live",
                "Save working strategies as custom templates for reuse",
                "Return {type:'HOLD'} when no signal — never return undefined",
                "All strategies auto-save before running a backtest"
            ))
        }

        Spacer(modifier = Modifier.height(32.dp))
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
