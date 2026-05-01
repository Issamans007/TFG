package com.tfg.feature.settings

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tfg.core.ui.components.*
import com.tfg.core.ui.theme.*
import com.tfg.core.util.SoundManager
import com.tfg.domain.model.RiskConfig

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateToApiKey: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.saved) {
        if (state.saved) {
            snackbarHostState.showSnackbar("Settings saved")
            viewModel.clearSaved()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 80.dp)
    ) {
        Text(
            "Settings", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary,
            modifier = Modifier.padding(16.dp)
        )

        // Bot Control
        SectionHeader(title = "Trading Bot")
        TfgCard(modifier = Modifier.padding(horizontal = 16.dp)) {
            SettingsToggle("Bot Enabled", state.botEnabled) { viewModel.toggleBot(it) }
            Spacer(modifier = Modifier.height(8.dp))
            SettingsToggle("Paper Trading", state.paperTrading) { viewModel.togglePaperTrading(it) }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Donations
        SectionHeader(title = "Donations")
        TfgCard(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text("Donate ${state.donationPercent.toInt()}% of profits to NGOs", color = TextSecondary, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = state.donationPercent.toFloat(),
                onValueChange = { viewModel.setDonationPercent(it.toDouble()) },
                valueRange = 0f..50f,
                steps = 9,
                colors = SliderDefaults.colors(
                    thumbColor = AccentGold,
                    activeTrackColor = AccentGold,
                    inactiveTrackColor = DarkBorder
                )
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("0%", fontSize = 11.sp, color = TextTertiary)
                Text("${state.donationPercent.toInt()}%", fontSize = 14.sp, color = AccentGold, fontWeight = FontWeight.SemiBold)
                Text("50%", fontSize = 11.sp, color = TextTertiary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Risk management
        SectionHeader(title = "Risk Management")
        RiskConfigSection(
            config = state.riskConfig,
            onUpdate = { viewModel.updateRiskConfig(it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Appearance
        SectionHeader(title = "Appearance")
        TfgCard(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text("Theme", color = TextPrimary, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("dark" to "Dark", "light" to "Light", "system" to "System").forEach { (value, label) ->
                    val selected = state.theme == value
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) AccentBlue.copy(alpha = 0.15f) else DarkCard)
                            .border(
                                1.dp,
                                if (selected) AccentBlue else DarkBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { viewModel.setTheme(value) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) AccentBlue else TextSecondary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "System theme uses Material You dynamic colors on Android 12+",
                fontSize = 10.sp,
                color = TextTertiary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Haptic Feedback
        SectionHeader(title = "Haptic Feedback")
        TfgCard(modifier = Modifier.padding(horizontal = 16.dp)) {
            SettingsToggle("Vibrate on Events", state.hapticEnabled) { viewModel.toggleHaptic(it) }
            Text("Vibrate on order fills, SL/TP hits, and emergency alerts", fontSize = 11.sp, color = TextTertiary)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sound Alerts
        SectionHeader(title = "Sound Alerts")
        TfgCard(modifier = Modifier.padding(horizontal = 16.dp)) {
            SettingsToggle("Sound Alerts", state.soundEnabled) { viewModel.toggleSound(it) }
            if (state.soundEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                SoundEventPicker("Order Filled", state.soundOrderFill, SoundManager.SoundEvent.ORDER_FILL, viewModel)
                SoundEventPicker("Stop Loss Hit", state.soundSlHit, SoundManager.SoundEvent.STOP_LOSS, viewModel)
                SoundEventPicker("Take Profit Hit", state.soundTpHit, SoundManager.SoundEvent.TAKE_PROFIT, viewModel)
                SoundEventPicker("Alert", state.soundAlert, SoundManager.SoundEvent.ALERT, viewModel)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Security
        SectionHeader(title = "Security")
        TfgCard(modifier = Modifier.padding(horizontal = 16.dp)) {
            SettingsToggle("Biometric Auth", state.biometricEnabled) { viewModel.toggleBiometric(it) }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("API Keys", color = TextPrimary, fontSize = 14.sp)
                StatusChip(
                    text = if (state.apiKeyConfigured) "Configured" else "Not Set",
                    color = if (state.apiKeyConfigured) Green500 else Red400
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onNavigateToApiKey) {
                Text("Manage API Keys", color = AccentBlue)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Emergency
        SectionHeader(title = "Emergency")
        TfgButton(
            text = "KILL SWITCH - Close All Positions",
            onClick = { viewModel.activateKillSwitch() },
            modifier = Modifier.padding(horizontal = 16.dp),
            type = ButtonType.SELL
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.align(Alignment.BottomCenter)
    )
    }
}

@Composable
private fun SettingsToggle(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextPrimary, fontSize = 14.sp)
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedTrackColor = AccentBlue,
                uncheckedTrackColor = DarkBorder
            )
        )
    }
}

@Composable
private fun RiskConfigSection(config: RiskConfig, onUpdate: (RiskConfig) -> Unit) {
    // Key the local edit state to `config` so when the saved RiskConfig flow
    // emits a new value (e.g. after Save, after kill-switch reset), the text
    // fields refresh from disk instead of showing stale defaults.
    var maxPositionPercent by remember(config) { mutableStateOf(config.maxPositionSizePercent.toString()) }
    var maxOpenTrades by remember(config) { mutableStateOf(config.maxOpenTrades.toString()) }
    var dailyLossLimit by remember(config) { mutableStateOf(config.dailyLossLimitPercent.toString()) }
    var maxLossPerTrade by remember(config) { mutableStateOf(config.maxLossPerTradePercent.toString()) }
    var drawdownPause by remember(config) { mutableStateOf(config.drawdownPausePercent.toString()) }
    var consecutiveLossLimit by remember(config) { mutableStateOf(config.consecutiveLossLimit.toString()) }

    TfgCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        RiskField("Max Position Size %", maxPositionPercent) { maxPositionPercent = it }
        RiskField("Max Open Trades", maxOpenTrades) { maxOpenTrades = it }
        RiskField("Daily Loss Limit %", dailyLossLimit) { dailyLossLimit = it }
        RiskField("Max Loss Per Trade %", maxLossPerTrade) { maxLossPerTrade = it }
        RiskField("Drawdown Pause %", drawdownPause) { drawdownPause = it }
        RiskField("Consecutive Loss Limit", consecutiveLossLimit) { consecutiveLossLimit = it }

        Spacer(modifier = Modifier.height(12.dp))
        TfgButton(
            text = "Save Risk Config",
            onClick = {
                onUpdate(
                    config.copy(
                        maxPositionSizePercent = maxPositionPercent.toDoubleOrNull() ?: config.maxPositionSizePercent,
                        maxOpenTrades = maxOpenTrades.toIntOrNull() ?: config.maxOpenTrades,
                        dailyLossLimitPercent = dailyLossLimit.toDoubleOrNull() ?: config.dailyLossLimitPercent,
                        maxLossPerTradePercent = maxLossPerTrade.toDoubleOrNull() ?: config.maxLossPerTradePercent,
                        drawdownPausePercent = drawdownPause.toDoubleOrNull() ?: config.drawdownPausePercent,
                        consecutiveLossLimit = consecutiveLossLimit.toIntOrNull() ?: config.consecutiveLossLimit
                    )
                )
            }
        )
    }
}

@Composable
private fun RiskField(label: String, value: String, onChange: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = TextSecondary, modifier = Modifier.weight(1f))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.width(100.dp).height(48.dp),
            singleLine = true,
            colors = com.tfg.core.ui.tfgTextFieldColors(),
            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
        )
    }
}

@Composable
private fun SoundEventPicker(
    label: String,
    current: SoundManager.SoundType,
    event: SoundManager.SoundEvent,
    viewModel: SettingsViewModel
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = TextSecondary, modifier = Modifier.weight(1f))
        Box {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurface)
                    .clickable { expanded = true }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(current.displayName, fontSize = 12.sp, color = TextPrimary)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                SoundManager.SoundType.entries.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.displayName, fontSize = 13.sp) },
                        onClick = {
                            viewModel.setSoundForEvent(event, type)
                            expanded = false
                        },
                        trailingIcon = {
                            if (type != SoundManager.SoundType.NONE) {
                                IconButton(onClick = {
                                    viewModel.setSoundForEvent(event, type)
                                    viewModel.previewSound(event)
                                }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Preview", tint = AccentBlue, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
