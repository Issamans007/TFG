package com.tfg.feature.notifications

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tfg.core.ui.components.*
import com.tfg.core.ui.theme.*
import com.tfg.core.util.Formatters
import com.tfg.domain.model.AuditAction
import com.tfg.domain.model.AuditCategory
import com.tfg.domain.model.AuditLog
import com.tfg.domain.repository.AuditRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

// ─── ViewModel ──────────────────────────────────────────────────────

data class ConsoleState(
    val logs: List<AuditLog> = emptyList(),
    val selectedCategory: AuditCategory? = null, // null = ALL
    val isLive: Boolean = true,
    val logCount: Int = 0
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val auditRepository: AuditRepository
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow<AuditCategory?>(null)
    private val _isLive = MutableStateFlow(true)

    val state: StateFlow<ConsoleState> = _selectedCategory
        .flatMapLatest { category ->
            auditRepository.getLogs(category = category, limit = 500)
        }
        .combine(_isLive) { logs, isLive ->
            ConsoleState(
                logs = logs,
                selectedCategory = _selectedCategory.value,
                isLive = isLive,
                logCount = logs.size
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ConsoleState())

    fun selectCategory(category: AuditCategory?) {
        _selectedCategory.value = category
    }

    fun toggleLive() {
        _isLive.value = !_isLive.value
    }

    fun clearLogs() {
        // Not destructive - just a UI placeholder; logs persist in DB
    }
}

// ─── Filter chip data ───────────────────────────────────────────────

private data class CategoryFilter(
    val label: String,
    val category: AuditCategory?,
    val icon: ImageVector,
    val color: Color
)

private val categoryFilters = listOf(
    CategoryFilter("All", null, Icons.Default.List, AccentBlue),
    CategoryFilter("Trading", AuditCategory.TRADING, Icons.Default.ShowChart, Green500),
    CategoryFilter("Signals", AuditCategory.SIGNAL, Icons.Default.Notifications, AccentPurple),
    CategoryFilter("System", AuditCategory.SYSTEM, Icons.Default.Settings, AccentOrange),
    CategoryFilter("Security", AuditCategory.SECURITY, Icons.Default.Lock, Red400),
    CategoryFilter("Config", AuditCategory.CONFIG, Icons.Default.Tune, AccentGold),
    CategoryFilter("Auth", AuditCategory.AUTH, Icons.Default.Person, AccentBlue),
    CategoryFilter("Compliance", AuditCategory.COMPLIANCE, Icons.Default.VerifiedUser, Green500)
)

// ─── Console Screen ─────────────────────────────────────────────────

@Composable
fun NotificationsScreen(viewModel: NotificationsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()

    // Auto-scroll to top when new logs arrive and live mode is on
    LaunchedEffect(state.logs.size, state.isLive) {
        if (state.isLive && state.logs.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // ── Header ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Console",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    "${state.logCount} events",
                    fontSize = 12.sp,
                    color = TextTertiary
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Live indicator
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (state.isLive) Green500.copy(alpha = 0.15f) else DarkSurface)
                        .clickable { viewModel.toggleLive() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (state.isLive) Green500 else TextTertiary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (state.isLive) "LIVE" else "PAUSED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (state.isLive) Green500 else TextTertiary
                        )
                    }
                }
            }
        }

        // ── Category filter chips ──
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            items(categoryFilters) { filter ->
                val selected = state.selectedCategory == filter.category
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (selected) filter.color.copy(alpha = 0.2f)
                            else DarkSurface
                        )
                        .border(
                            width = 1.dp,
                            color = if (selected) filter.color else DarkBorder,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable { viewModel.selectCategory(filter.category) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            filter.icon,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (selected) filter.color else TextTertiary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            filter.label,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) filter.color else TextSecondary
                        )
                    }
                }
            }
        }

        // ── Divider ──
        Divider(color = DarkBorder, thickness = 1.dp)

        // ── Log list ──
        if (state.logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Terminal,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = TextTertiary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "No activity recorded",
                        fontSize = 16.sp,
                        color = TextTertiary
                    )
                    Text(
                        if (state.selectedCategory != null)
                            "No events in this category yet"
                        else
                            "Events will appear here as the bot operates",
                        fontSize = 13.sp,
                        color = TextTertiary.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.logs, key = { it.id }) { log ->
                    ConsoleLogEntry(log = log)
                }
            }
        }
    }
}

// ─── Individual log entry ───────────────────────────────────────────

@Composable
private fun ConsoleLogEntry(log: AuditLog) {
    val actionColor = actionColor(log.action)
    val actionIcon = actionIcon(log.action)
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Timestamp column
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.width(54.dp)
        ) {
            Text(
                timeFormat.format(Date(log.timestamp)),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = TextTertiary
            )
            Text(
                dateFormat.format(Date(log.timestamp)),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = TextTertiary.copy(alpha = 0.5f)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Colored indicator line
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(actionColor)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Content
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    actionIcon,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = actionColor
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    formatActionName(log.action),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = actionColor
                )
                if (log.symbol != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        log.symbol!!,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(DarkSurface)
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    log.category.name,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextTertiary.copy(alpha = 0.5f)
                )
            }
            if (log.details.isNotBlank()) {
                Text(
                    log.details,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 15.sp
                )
            }
            if (log.oldValue != null || log.newValue != null) {
                Row {
                    log.oldValue?.let {
                        Text(
                            "- $it",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Red400.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    log.newValue?.let {
                        Text(
                            "+ $it",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Green500.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }

    Divider(
        color = DarkBorder.copy(alpha = 0.3f),
        thickness = 0.5.dp,
        modifier = Modifier.padding(start = 70.dp)
    )
}

// ─── Helpers ────────────────────────────────────────────────────────

private fun actionColor(action: AuditAction): Color = when (action) {
    AuditAction.ORDER_PLACED -> AccentBlue
    AuditAction.ORDER_FILLED -> Green500
    AuditAction.ORDER_CANCELLED -> Red400
    AuditAction.ORDER_EMERGENCY_CLOSED -> Red500
    AuditAction.KILL_SWITCH_ACTIVATED -> Red500
    AuditAction.RISK_VIOLATION -> AccentOrange
    AuditAction.DONATION_SENT -> AccentGold
    AuditAction.SIGNAL_RECEIVED -> AccentPurple
    AuditAction.SIGNAL_EXECUTED -> Green500
    AuditAction.SIGNAL_SKIPPED -> AccentOrange
    AuditAction.LOGIN, AuditAction.LOGOUT -> AccentBlue
    AuditAction.API_KEY_SET, AuditAction.API_KEY_REVOKED -> AccentGold
    AuditAction.CONFIG_CHANGED -> AccentGold
    AuditAction.PIN_ENTERED, AuditAction.BIOMETRIC_AUTH -> AccentBlue
    AuditAction.PIN_FAILED -> Red400
    AuditAction.APP_CRASH -> Red500
    AuditAction.OFFLINE_SYNC -> AccentOrange
    AuditAction.EXPORT_GENERATED -> AccentBlue
    else -> TextSecondary
}

private fun actionIcon(action: AuditAction): ImageVector = when (action) {
    AuditAction.ORDER_PLACED -> Icons.Default.AddCircle
    AuditAction.ORDER_FILLED -> Icons.Default.CheckCircle
    AuditAction.ORDER_CANCELLED -> Icons.Default.Cancel
    AuditAction.ORDER_EMERGENCY_CLOSED -> Icons.Default.Warning
    AuditAction.KILL_SWITCH_ACTIVATED -> Icons.Default.PowerSettingsNew
    AuditAction.RISK_VIOLATION -> Icons.Default.Shield
    AuditAction.DONATION_SENT -> Icons.Default.Favorite
    AuditAction.SIGNAL_RECEIVED -> Icons.Default.Notifications
    AuditAction.SIGNAL_EXECUTED -> Icons.Default.PlayArrow
    AuditAction.SIGNAL_SKIPPED -> Icons.Default.SkipNext
    AuditAction.LOGIN -> Icons.Default.Login
    AuditAction.LOGOUT -> Icons.Default.Logout
    AuditAction.API_KEY_SET, AuditAction.API_KEY_REVOKED -> Icons.Default.Key
    AuditAction.CONFIG_CHANGED -> Icons.Default.Tune
    AuditAction.PIN_ENTERED, AuditAction.PIN_FAILED -> Icons.Default.Lock
    AuditAction.BIOMETRIC_AUTH -> Icons.Default.Fingerprint
    AuditAction.APP_CRASH -> Icons.Default.BugReport
    AuditAction.OFFLINE_SYNC -> Icons.Default.Sync
    AuditAction.EXPORT_GENERATED -> Icons.Default.FileDownload
    else -> Icons.Default.Info
}

private fun formatActionName(action: AuditAction): String =
    action.name.replace("_", " ")
        .lowercase()
        .replaceFirstChar { it.uppercase() }
