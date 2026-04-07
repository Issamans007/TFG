package com.tfg.feature.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.tfg.core.util.Formatters
import com.tfg.domain.model.DashboardCardType
import com.tfg.domain.model.Signal
import com.tfg.domain.model.SignalStatus

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToTrade: (String) -> Unit,
    onNavigateToPortfolio: () -> Unit,
    onNavigateToMarkets: () -> Unit,
    onNavigateToConsole: () -> Unit = {},
    onNavigateToAlerts: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()

    if (state.isLoading) {
        LoadingIndicator()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("TradeForGood", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AccentBlue)
                Text("Dashboard", fontSize = 14.sp, color = TextSecondary)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Customize button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (state.isCustomizing) AccentBlue.copy(alpha = 0.2f) else DarkSurface)
                        .clickable { viewModel.toggleCustomizing() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.Dashboard,
                        contentDescription = "Customize",
                        tint = if (state.isCustomizing) AccentBlue else TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurface)
                        .clickable { onNavigateToAlerts() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.NotificationsActive,
                            contentDescription = "Alerts",
                            tint = AccentGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Alerts", fontSize = 11.sp, color = AccentGold, fontWeight = FontWeight.Medium)
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurface)
                        .clickable { onNavigateToConsole() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Terminal,
                            contentDescription = "Console",
                            tint = AccentBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Console", fontSize = 11.sp, color = AccentBlue, fontWeight = FontWeight.Medium)
                    }
                }
                StatusChip(
                    text = if (state.botActive) "BOT ACTIVE" else "BOT OFF",
                    color = if (state.botActive) Green500 else Red400
                )
            }
        }

        // Customization panel (toggle cards on/off + reorder)
        AnimatedVisibility(visible = state.isCustomizing) {
            CustomizePanel(state, viewModel)
        }

        // Cards list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            val visibleCards = state.cardConfigs.filter { it.visible }.sortedBy { it.order }
            items(visibleCards, key = { it.type.name }) { card ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    when (card.type) {
                        DashboardCardType.PORTFOLIO_OVERVIEW -> PortfolioCard(state)
                        DashboardCardType.QUICK_STATS -> QuickStatsRow(state)
                        DashboardCardType.RISK_METRICS -> RiskMetricsRow(state)
                        DashboardCardType.RECENT_SIGNALS -> RecentSignalsSection(state, onNavigateToTrade)
                        DashboardCardType.OPEN_POSITIONS -> PositionsSection(state, onNavigateToTrade, onNavigateToPortfolio)
                        DashboardCardType.PNL_CHART -> PnlChartCard(state)
                        DashboardCardType.BOT_STATUS -> BotStatusCard(state)
                        DashboardCardType.DONATION_PROGRESS -> DonationCard(state)
                    }
                }
            }

            // Error
            state.error?.let {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    ErrorMessage(message = it, onRetry = { viewModel.loadDashboard() })
                }
            }
        }
    }
}

// ─── Customize panel (toggle visibility + reorder arrows) ───────────

@Composable
private fun CustomizePanel(state: DashboardUiState, viewModel: DashboardViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .padding(12.dp)
    ) {
        Text("Customize Dashboard", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Text("Toggle cards on/off, tap arrows to reorder", fontSize = 11.sp, color = TextTertiary)
        Spacer(modifier = Modifier.height(8.dp))

        state.cardConfigs.forEachIndexed { index, card ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkCard)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = card.visible,
                    onCheckedChange = { viewModel.toggleCardVisibility(card.type) },
                    colors = CheckboxDefaults.colors(
                        checkedColor = AccentBlue,
                        uncheckedColor = DarkBorder
                    ),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    card.type.displayName,
                    fontSize = 13.sp,
                    color = if (card.visible) TextPrimary else TextTertiary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { if (index > 0) viewModel.moveCard(index, index - 1) }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { if (index < state.cardConfigs.lastIndex) viewModel.moveCard(index, index + 1) }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// ─── Individual dashboard cards ─────────────────────────────────────

@Composable
private fun PortfolioCard(state: DashboardUiState) {
    TfgCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text("Portfolio Value", fontSize = 13.sp, color = TextSecondary)
        Spacer(modifier = Modifier.height(4.dp))
        state.portfolio?.let { p ->
            Text(
                Formatters.formatUsdt(p.totalValueUsdt),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PnlText(value = p.totalPnlPercent, showPercent = true, fontSize = 16)
                Spacer(modifier = Modifier.width(8.dp))
                PnlText(value = p.totalPnlUsdt, fontSize = 14)
            }
        } ?: Text("--", fontSize = 32.sp, color = TextTertiary)
    }
}

@Composable
private fun QuickStatsRow(state: DashboardUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickStatCard(
            title = "Win Rate",
            value = state.analytics?.let { "${String.format("%.1f", it.winRate)}%" } ?: "--",
            modifier = Modifier.weight(1f)
        )
        QuickStatCard(
            title = "Total Trades",
            value = state.analytics?.totalTrades?.toString() ?: "--",
            modifier = Modifier.weight(1f)
        )
        QuickStatCard(
            title = "Donated",
            value = Formatters.formatUsdt(state.totalDonated),
            modifier = Modifier.weight(1f),
            valueColor = AccentGold
        )
    }
}

@Composable
private fun RiskMetricsRow(state: DashboardUiState) {
    state.analytics?.let { a ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickStatCard("Sharpe", String.format("%.2f", a.sharpeRatio), Modifier.weight(1f))
            QuickStatCard("Sortino", String.format("%.2f", a.sortinoRatio), Modifier.weight(1f))
            QuickStatCard("Max DD", Formatters.formatPercent(a.maxDrawdownPercent), Modifier.weight(1f), valueColor = Red400)
        }
    }
}

@Composable
private fun RecentSignalsSection(state: DashboardUiState, onNavigateToTrade: (String) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        SectionHeader(title = "Recent Signals", action = "View All", onAction = {})
        if (state.recentSignals.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("No signals yet", color = TextTertiary, fontSize = 14.sp)
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.recentSignals) { signal ->
                    SignalCard(signal = signal, onClick = { onNavigateToTrade(signal.symbol) })
                }
            }
        }
    }
}

@Composable
private fun PositionsSection(state: DashboardUiState, onNavigateToTrade: (String) -> Unit, onNavigateToPortfolio: () -> Unit) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        SectionHeader(title = "Open Positions", action = "See All", onAction = onNavigateToPortfolio)
        state.portfolio?.positions?.take(3)?.forEach { position ->
            TfgCard(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                onClick = { onNavigateToTrade(position.symbol) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(position.symbol, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text("${position.quantity} @ ${Formatters.formatPrice(position.entryPrice)}", fontSize = 12.sp, color = TextSecondary)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        PriceText(price = position.currentPrice)
                        PnlText(value = position.unrealizedPnl)
                    }
                }
            }
        }
        if (state.portfolio?.positions.isNullOrEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("No open positions", color = TextTertiary, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun PnlChartCard(state: DashboardUiState) {
    TfgCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text("Performance", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Spacer(modifier = Modifier.height(4.dp))
        state.analytics?.let { a ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total Return", fontSize = 11.sp, color = TextTertiary)
                    PnlText(value = a.totalReturnPercent, showPercent = true, fontSize = 18)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Profit Factor", fontSize = 11.sp, color = TextTertiary)
                    Text(String.format("%.2f", a.profitFactor), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            }
        } ?: Text("No data yet", fontSize = 13.sp, color = TextTertiary)
    }
}

@Composable
private fun BotStatusCard(state: DashboardUiState) {
    TfgCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (state.botActive) Icons.Default.PlayCircle else Icons.Default.PauseCircle,
                contentDescription = null,
                tint = if (state.botActive) Green500 else Red400,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    if (state.botActive) "Bot is Running" else "Bot is Stopped",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    if (state.botActive) "Monitoring positions & executing strategies" else "Start the bot from Settings",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun DonationCard(state: DashboardUiState) {
    TfgCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Favorite, contentDescription = null, tint = AccentGold, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Donations", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text("Total donated to NGOs", fontSize = 11.sp, color = TextSecondary)
            }
            Text(
                Formatters.formatUsdt(state.totalDonated),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = AccentGold
            )
        }
    }
}

@Composable
private fun QuickStatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: androidx.compose.ui.graphics.Color = TextPrimary
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard)
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(title, fontSize = 11.sp, color = TextTertiary)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}

@Composable
private fun SignalCard(signal: Signal, onClick: () -> Unit) {
    TfgCard(
        modifier = Modifier.width(200.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(signal.symbol, fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 14.sp)
            StatusChip(
                text = signal.side.name,
                color = if (signal.side == com.tfg.domain.model.OrderSide.BUY) Green500 else Red500
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Entry: ${Formatters.formatPrice(signal.entryPrice)}", fontSize = 12.sp, color = TextSecondary)
        Text("Confidence: ${String.format("%.0f", signal.confidence * 100)}%", fontSize = 12.sp, color = TextSecondary)
    }
}
