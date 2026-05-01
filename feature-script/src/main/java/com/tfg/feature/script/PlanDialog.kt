package com.tfg.feature.script

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tfg.core.ui.theme.*
import com.tfg.domain.model.MarketType
import com.tfg.domain.model.MarginType
import com.tfg.domain.model.TradingPlan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanDialog(
    initial: TradingPlan,
    onDismiss: () -> Unit,
    onSave: (TradingPlan) -> Unit
) {
    var marketType  by remember { mutableStateOf(initial.marketType) }
    var leverage    by remember { mutableStateOf(initial.leverage) }
    var marginType  by remember { mutableStateOf(initial.marginType) }
    var allowShort  by remember { mutableStateOf(initial.allowShort) }
    var sizePct     by remember { mutableStateOf(initial.sizePct.toString()) }
    var slMode      by remember { mutableStateOf(initial.slMode) }
    var slValue     by remember { mutableStateOf(initial.slValue.toString()) }
    var tpMode      by remember { mutableStateOf(initial.tpMode) }
    var tpRows      by remember { mutableStateOf(initial.tpLevels.toMutableList()) }
    var moveBE      by remember { mutableStateOf(initial.moveSlToBreakeven) }
    var sessEnabled by remember { mutableStateOf(initial.sessionEnabled) }
    var startHour   by remember { mutableStateOf(initial.sessionStartHour.toString()) }
    var endHour     by remember { mutableStateOf(initial.sessionEndHour.toString()) }
    var cooldown    by remember { mutableStateOf(initial.cooldownBars.toString()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(16.dp),
            color = DarkSurface,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ── Title bar ───────────────────────────────────────
                Text(
                    "Trading Plan",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                )
                HorizontalDivider(color = TextTertiary.copy(alpha = 0.3f))

                // ── Scrollable body ──────────────────────────────────
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    // ── Market ──────────────────────────────────────
                    SectionLabel("Market")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = marketType == MarketType.SPOT,
                            onClick  = { marketType = MarketType.SPOT },
                            label    = { Text("Spot") }
                        )
                        FilterChip(
                            selected = marketType == MarketType.FUTURES_USDM,
                            onClick  = { marketType = MarketType.FUTURES_USDM },
                            label    = { Text("Futures") }
                        )
                    }

                    if (marketType == MarketType.FUTURES_USDM) {
                        Text("Leverage: ${leverage}x", fontSize = 12.sp, color = TextSecondary)
                        Slider(
                            value         = leverage.toFloat(),
                            onValueChange = { leverage = it.toInt().coerceIn(1, 125) },
                            valueRange    = 1f..125f,
                            modifier      = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = marginType == MarginType.ISOLATED,
                                onClick  = { marginType = MarginType.ISOLATED },
                                label    = { Text("Isolated") }
                            )
                            FilterChip(
                                selected = marginType == MarginType.CROSSED,
                                onClick  = { marginType = MarginType.CROSSED },
                                label    = { Text("Cross") }
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(checked = allowShort, onCheckedChange = { allowShort = it })
                            Spacer(Modifier.width(10.dp))
                            Text("Allow SELL signal to open SHORT", fontSize = 13.sp, color = TextPrimary)
                        }
                    }

                    PlanDivider()

                    // ── Sizing ──────────────────────────────────────
                    SectionLabel("Position Size (% of equity)")
                    OutlinedTextField(
                        value          = sizePct,
                        onValueChange  = { sizePct = it.filter { c -> c.isDigit() || c == '.' } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine     = true,
                        suffix         = { Text("%", color = TextSecondary) },
                        modifier       = Modifier.fillMaxWidth()
                    )

                    PlanDivider()

                    // ── Stop Loss ───────────────────────────────────
                    SectionHeader("Stop Loss")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TradingPlan.SlMode.values().forEach { m ->
                            FilterChip(
                                selected = slMode == m,
                                onClick  = { slMode = m },
                                label    = { Text(m.name) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value           = slValue,
                        onValueChange   = { slValue = it.filter { c -> c.isDigit() || c == '.' } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        label           = {
                            Text(when (slMode) {
                                TradingPlan.SlMode.ATR          -> "ATR multiplier"
                                TradingPlan.SlMode.PCT          -> "Percent (%)"
                                TradingPlan.SlMode.FIXED_OFFSET -> "Price offset"
                            })
                        },
                        singleLine  = true,
                        modifier    = Modifier.fillMaxWidth()
                    )

                    PlanDivider()

                    // ── Take Profit levels ──────────────────────────
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionHeader("Take Profit Levels")
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = {
                            tpRows = (tpRows + TradingPlan.TpLevel(2.0, 50.0)).toMutableList()
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Add TP", tint = AccentBlue)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TradingPlan.TpMode.values().forEach { m ->
                            FilterChip(
                                selected = tpMode == m,
                                onClick  = { tpMode = m },
                                label    = { Text(if (m == TradingPlan.TpMode.R_MULTIPLE) "R-multiple" else "Percent") }
                            )
                        }
                    }
                    // Column headers
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            if (tpMode == TradingPlan.TpMode.R_MULTIPLE) "Risk ×" else "%",
                            fontSize = 11.sp, color = TextSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        Text("Qty %", fontSize = 11.sp, color = TextSecondary, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(40.dp))
                    }
                    tpRows.forEachIndexed { idx, lv ->
                        var v by remember(idx) { mutableStateOf(lv.value.toString()) }
                        var q by remember(idx) { mutableStateOf(lv.qtyPct.toString()) }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value           = v,
                                onValueChange   = {
                                    v = it.filter { c -> c.isDigit() || c == '.' }
                                    tpRows = tpRows.toMutableList().also { list ->
                                        list[idx] = list[idx].copy(value = v.toDoubleOrNull() ?: 0.0)
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine      = true,
                                modifier        = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value           = q,
                                onValueChange   = {
                                    q = it.filter { c -> c.isDigit() || c == '.' }
                                    tpRows = tpRows.toMutableList().also { list ->
                                        list[idx] = list[idx].copy(qtyPct = q.toDoubleOrNull() ?: 0.0)
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine      = true,
                                modifier        = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick  = {
                                    tpRows = tpRows.toMutableList().also { list -> list.removeAt(idx) }
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = TextSecondary)
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = moveBE, onCheckedChange = { moveBE = it })
                        Spacer(Modifier.width(10.dp))
                        Text("Move SL to breakeven after first TP", fontSize = 13.sp, color = TextPrimary)
                    }

                    PlanDivider()

                    // ── Trading hours ───────────────────────────────
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = sessEnabled, onCheckedChange = { sessEnabled = it })
                        Spacer(Modifier.width(10.dp))
                        SectionHeader("Restrict trading hours (UTC)")
                    }
                    if (sessEnabled) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value           = startHour,
                                onValueChange   = { startHour = it.filter { c -> c.isDigit() }.take(2) },
                                label           = { Text("From (0-23)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine      = true,
                                modifier        = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value           = endHour,
                                onValueChange   = { endHour = it.filter { c -> c.isDigit() }.take(2) },
                                label           = { Text("To (1-24)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine      = true,
                                modifier        = Modifier.weight(1f)
                            )
                        }
                    }

                    PlanDivider()

                    // ── Cooldown ────────────────────────────────────
                    SectionLabel("Cooldown bars after a trade closes")
                    OutlinedTextField(
                        value           = cooldown,
                        onValueChange   = { cooldown = it.filter { c -> c.isDigit() } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine      = true,
                        modifier        = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))
                }

                // ── Button row ───────────────────────────────────────
                HorizontalDivider(color = TextTertiary.copy(alpha = 0.3f))
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextSecondary)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(
                                TradingPlan(
                                    marketType         = marketType,
                                    leverage           = leverage.coerceIn(1, 125),
                                    marginType         = marginType,
                                    allowShort         = allowShort,
                                    sizePct            = sizePct.toDoubleOrNull()?.coerceIn(0.01, 100.0) ?: 2.0,
                                    slMode             = slMode,
                                    slValue            = slValue.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 1.5,
                                    tpMode             = tpMode,
                                    tpLevels           = tpRows.filter { it.value > 0.0 && it.qtyPct > 0.0 },
                                    moveSlToBreakeven  = moveBE,
                                    sessionEnabled     = sessEnabled,
                                    sessionStartHour   = startHour.toIntOrNull()?.coerceIn(0, 23) ?: 0,
                                    sessionEndHour     = endHour.toIntOrNull()?.coerceIn(0, 24) ?: 24,
                                    cooldownBars       = cooldown.toIntOrNull()?.coerceAtLeast(0) ?: 0
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                    ) {
                        Text("Save", color = DarkSurface)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) =
    Text(text, fontSize = 12.sp, color = TextSecondary)

@Composable
private fun SectionHeader(text: String) =
    Text(text, fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)

@Composable
private fun PlanDivider() =
    HorizontalDivider(color = TextTertiary.copy(alpha = 0.25f), thickness = 0.5.dp)
