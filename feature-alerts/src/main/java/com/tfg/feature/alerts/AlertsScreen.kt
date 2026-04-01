package com.tfg.feature.alerts

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tfg.core.ui.theme.*
import com.tfg.domain.model.*
import com.tfg.domain.repository.AlertRepository
import com.tfg.domain.repository.MarketRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

// ─── ViewModel ──────────────────────────────────────────────────────

data class AlertsUiState(
    val alerts: List<Alert> = emptyList(),
    val watchlist: List<TradingPair> = emptyList(),
    val showCreateDialog: Boolean = false,
    val filterSymbol: String? = null
)

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val alertRepository: AlertRepository,
    private val marketRepository: MarketRepository
) : ViewModel() {

    private val _filterSymbol = MutableStateFlow<String?>(null)
    private val _showDialog = MutableStateFlow(false)

    val state: StateFlow<AlertsUiState> = combine(
        _filterSymbol.flatMapLatest { symbol ->
            if (symbol != null) alertRepository.getAlertsForSymbol(symbol)
            else alertRepository.getAllAlerts()
        },
        _showDialog,
        marketRepository.getWatchlist()
    ) { alerts, showDialog, watchlist ->
        AlertsUiState(
            alerts = alerts,
            watchlist = watchlist,
            showCreateDialog = showDialog,
            filterSymbol = _filterSymbol.value
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AlertsUiState())

    fun toggleEnabled(alert: Alert) {
        viewModelScope.launch {
            alertRepository.updateEnabled(alert.id, !alert.isEnabled)
        }
    }

    fun deleteAlert(id: String) {
        viewModelScope.launch {
            alertRepository.deleteAlert(id)
        }
    }

    fun showCreateDialog() { _showDialog.value = true }
    fun hideCreateDialog() { _showDialog.value = false }

    fun createAlert(
        symbol: String,
        name: String,
        type: AlertType,
        condition: AlertCondition,
        targetValue: Double,
        secondaryValue: Double?,
        isRepeating: Boolean,
        repeatIntervalSec: Int
    ) {
        viewModelScope.launch {
            val alert = Alert(
                id = UUID.randomUUID().toString(),
                symbol = symbol.uppercase().trim(),
                name = name.ifBlank { "${symbol.uppercase()} ${type.name}" },
                type = type,
                condition = condition,
                targetValue = targetValue,
                secondaryValue = secondaryValue,
                isRepeating = isRepeating,
                repeatIntervalSec = repeatIntervalSec
            )
            alertRepository.saveAlert(alert)
            _showDialog.value = false
        }
    }

    fun filterBySymbol(symbol: String?) {
        _filterSymbol.value = symbol
    }
}

// ─── Screen ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(
    onBack: () -> Unit = {},
    viewModel: AlertsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    if (state.showCreateDialog) {
        CreateAlertDialog(
            watchlist = state.watchlist,
            onDismiss = { viewModel.hideCreateDialog() },
            onCreate = { symbol, name, type, condition, target, secondary, repeating, repeatSec ->
                viewModel.createAlert(symbol, name, type, condition, target, secondary, repeating, repeatSec)
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // ─── Top bar ─────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkCard)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimary)
            }
            Text(
                "\uD83D\uDD14 Alerts",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                "${state.alerts.size}",
                color = TextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.padding(end = 8.dp)
            )
            FilledIconButton(
                onClick = { viewModel.showCreateDialog() },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = AccentBlue
                ),
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Add, "Create alert", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }

        if (state.alerts.isEmpty()) {
            // ─── Empty state ─────────────────────────────────
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.NotificationsNone,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("No alerts yet", color = TextSecondary, fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Tap + to create your first alert",
                        color = TextTertiary,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            // ─── Alert list ──────────────────────────────────
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.alerts, key = { it.id }) { alert ->
                    AlertCard(
                        alert = alert,
                        onToggle = { viewModel.toggleEnabled(alert) },
                        onDelete = { viewModel.deleteAlert(alert.id) }
                    )
                }
            }
        }
    }
}

// ─── Alert Card ─────────────────────────────────────────────────────

@Composable
private fun AlertCard(
    alert: Alert,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = BorderStroke(1.dp, DarkBorder)
    ) {
        Column(Modifier.padding(14.dp)) {
            // Row 1: Icon + name + switch
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(alertTypeColor(alert.type).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        alertTypeIcon(alert.type),
                        contentDescription = null,
                        tint = alertTypeColor(alert.type),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        alert.name,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        alert.symbol,
                        color = AccentGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Switch(
                    checked = alert.isEnabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Green400,
                        uncheckedThumbColor = TextTertiary,
                        uncheckedTrackColor = DarkBorder
                    )
                )
            }

            Spacer(Modifier.height(10.dp))

            // Row 2: Type + condition + target
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(DarkBackground, RoundedCornerShape(8.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoChip(alert.type.label(), alertTypeColor(alert.type))
                InfoChip(alert.condition.label(), AccentBlue)
                Text(
                    formatTarget(alert),
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(8.dp))

            // Row 3: Metadata + delete
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (alert.isRepeating) {
                    Icon(Icons.Default.Alarm, null, tint = AccentOrange, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Every ${alert.repeatIntervalSec}s", color = AccentOrange, fontSize = 11.sp)
                    Spacer(Modifier.width(12.dp))
                }
                if (alert.triggerCount > 0) {
                    Text(
                        "Triggered ${alert.triggerCount}x",
                        color = TextTertiary,
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.width(4.dp))
                    alert.lastTriggeredAt?.let {
                        Text(
                            "• ${formatTime(it)}",
                            color = TextTertiary,
                            fontSize = 11.sp
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                if (showDeleteConfirm) {
                    TextButton(
                        onClick = { onDelete(); showDeleteConfirm = false },
                        colors = ButtonDefaults.textButtonColors(contentColor = Red400)
                    ) { Text("Confirm", fontSize = 12.sp) }
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("Cancel", color = TextTertiary, fontSize = 12.sp)
                    }
                } else {
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Delete, "Delete", tint = TextTertiary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoChip(text: String, color: Color) {
    Text(
        text,
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

// ─── Create Alert Dialog ────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateAlertDialog(
    watchlist: List<TradingPair>,
    onDismiss: () -> Unit,
    onCreate: (symbol: String, name: String, type: AlertType, condition: AlertCondition,
               target: Double, secondary: Double?,
               repeating: Boolean, repeatSec: Int) -> Unit
) {
    var selectedSymbol by remember { mutableStateOf(watchlist.firstOrNull()?.symbol ?: "") }
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(AlertType.PRICE) }
    var selectedCondition by remember { mutableStateOf(AlertCondition.CROSSES_ABOVE) }
    var targetValue by remember { mutableStateOf("") }
    var secondaryValue by remember { mutableStateOf("") }
    var isRepeating by remember { mutableStateOf(false) }
    var repeatSec by remember { mutableStateOf("60") }
    var symbolExpanded by remember { mutableStateOf(false) }
    var typeExpanded by remember { mutableStateOf(false) }
    var conditionExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text("Create Alert", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Symbol from watchlist
                if (watchlist.isEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AccentOrange.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, null, tint = AccentOrange, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Add pairs to your watchlist in Markets first",
                            color = AccentOrange,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    DialogField("Pair") {
                        ExposedDropdownMenuBox(
                            expanded = symbolExpanded,
                            onExpandedChange = { symbolExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedSymbol,
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(symbolExpanded) },
                                colors = dialogFieldColors()
                            )
                            ExposedDropdownMenu(
                                expanded = symbolExpanded,
                                onDismissRequest = { symbolExpanded = false },
                                containerColor = DarkCard
                            ) {
                                watchlist.forEach { pair ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(pair.symbol, color = TextPrimary, fontWeight = FontWeight.Medium)
                                                Spacer(Modifier.width(8.dp))
                                                pair.lastPrice?.let { price ->
                                                    Text(
                                                        "%.6g".format(price),
                                                        color = TextSecondary,
                                                        fontSize = 12.sp
                                                    )
                                                }
                                            }
                                        },
                                        onClick = { selectedSymbol = pair.symbol; symbolExpanded = false }
                                    )
                                }
                            }
                        }
                    }
                }

                // Name (optional)
                DialogField("Label (optional)") {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = dialogFieldColors(),
                        placeholder = { Text("My Alert", color = TextTertiary) }
                    )
                }

                // Alert Type dropdown (only PRICE / PRICE_PERCENT)
                DialogField("Type") {
                    ExposedDropdownMenuBox(
                        expanded = typeExpanded,
                        onExpandedChange = { typeExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedType.label(),
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) },
                            colors = dialogFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = typeExpanded,
                            onDismissRequest = { typeExpanded = false },
                            containerColor = DarkCard
                        ) {
                            AlertType.entries.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.label(), color = TextPrimary) },
                                    onClick = { selectedType = type; typeExpanded = false }
                                )
                            }
                        }
                    }
                }

                // Condition dropdown
                DialogField("Condition") {
                    ExposedDropdownMenuBox(
                        expanded = conditionExpanded,
                        onExpandedChange = { conditionExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedCondition.label(),
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(conditionExpanded) },
                            colors = dialogFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = conditionExpanded,
                            onDismissRequest = { conditionExpanded = false },
                            containerColor = DarkCard
                        ) {
                            AlertCondition.entries.forEach { cond ->
                                DropdownMenuItem(
                                    text = { Text(cond.label(), color = TextPrimary) },
                                    onClick = { selectedCondition = cond; conditionExpanded = false }
                                )
                            }
                        }
                    }
                }

                // Target value
                DialogField(targetLabel(selectedType)) {
                    OutlinedTextField(
                        value = targetValue,
                        onValueChange = { targetValue = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = dialogFieldColors(),
                        placeholder = { Text("0.0", color = TextTertiary) }
                    )
                }

                // Secondary value (for BETWEEN/OUTSIDE, or base price for PRICE_PERCENT)
                if (selectedCondition == AlertCondition.BETWEEN || selectedCondition == AlertCondition.OUTSIDE) {
                    DialogField("Secondary Value") {
                        OutlinedTextField(
                            value = secondaryValue,
                            onValueChange = { secondaryValue = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = dialogFieldColors(),
                            placeholder = { Text("0.0", color = TextTertiary) }
                        )
                    }
                }

                if (selectedType == AlertType.PRICE_PERCENT) {
                    DialogField("Base Price (current price at creation)") {
                        val currentPrice = watchlist.find { it.symbol == selectedSymbol }?.lastPrice
                        OutlinedTextField(
                            value = secondaryValue.ifBlank { currentPrice?.let { "%.6g".format(it) } ?: "" },
                            onValueChange = { secondaryValue = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = dialogFieldColors(),
                            placeholder = { Text("Auto-filled from current price", color = TextTertiary) }
                        )
                        // Auto-fill if user hasn't typed anything
                        LaunchedEffect(selectedSymbol, selectedType) {
                            if (selectedType == AlertType.PRICE_PERCENT && secondaryValue.isBlank()) {
                                currentPrice?.let { secondaryValue = "%.6g".format(it) }
                            }
                        }
                    }
                }

                // Repeating alarm toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Alarm, null, tint = AccentOrange, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Alarm (repeat notification)", color = TextPrimary, fontSize = 13.sp)
                    Spacer(Modifier.weight(1f))
                    Switch(
                        checked = isRepeating,
                        onCheckedChange = { isRepeating = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AccentOrange,
                            uncheckedThumbColor = TextTertiary,
                            uncheckedTrackColor = DarkBorder
                        )
                    )
                }

                if (isRepeating) {
                    DialogField("Repeat every (seconds)") {
                        OutlinedTextField(
                            value = repeatSec,
                            onValueChange = { repeatSec = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = dialogFieldColors()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedSymbol.isBlank()) return@Button
                    val target = targetValue.toDoubleOrNull() ?: return@Button
                    val secondary = secondaryValue.toDoubleOrNull()
                    val rSec = repeatSec.toIntOrNull() ?: 60
                    onCreate(selectedSymbol, name, selectedType, selectedCondition, target, secondary, isRepeating, rSec)
                },
                enabled = watchlist.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun DialogField(label: String, content: @Composable () -> Unit) {
    Column {
        Text(label, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun dialogFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = AccentBlue,
    focusedBorderColor = AccentBlue,
    unfocusedBorderColor = DarkBorder,
    focusedContainerColor = DarkBackground,
    unfocusedContainerColor = DarkBackground
)

// ─── Helpers ────────────────────────────────────────────────────────

private fun AlertType.label(): String = when (this) {
    AlertType.PRICE -> "Price"
    AlertType.PRICE_PERCENT -> "Price %"
}

private fun AlertCondition.label(): String = when (this) {
    AlertCondition.CROSSES_ABOVE -> "Crosses Above"
    AlertCondition.CROSSES_BELOW -> "Crosses Below"
    AlertCondition.BETWEEN -> "Between"
    AlertCondition.OUTSIDE -> "Outside"
    AlertCondition.EQUALS -> "Equals"
}

private fun alertTypeColor(type: AlertType): Color = when (type) {
    AlertType.PRICE -> AccentBlue
    AlertType.PRICE_PERCENT -> AccentPurple
}

private fun alertTypeIcon(type: AlertType): ImageVector = when (type) {
    AlertType.PRICE -> Icons.Default.AttachMoney
    AlertType.PRICE_PERCENT -> Icons.Default.Percent
}

private fun targetLabel(type: AlertType): String = when (type) {
    AlertType.PRICE -> "Target Price"
    AlertType.PRICE_PERCENT -> "Change %"
}

private fun formatTarget(alert: Alert): String {
    return when (alert.type) {
        AlertType.PRICE -> "%.6g".format(alert.targetValue)
        AlertType.PRICE_PERCENT -> "${alert.targetValue}%"
    }
}

private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)
private fun formatTime(millis: Long): String = timeFormat.format(Date(millis))
