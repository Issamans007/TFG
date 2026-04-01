package com.tfg.feature.dashboard

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
            .verticalScroll(rememberScrollState())
            .padding(bottom = 80.dp)
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

        // Portfolio overview card
        TfgCard(modifier = Modifier.padding(horizontal = 16.dp)) {
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

        Spacer(modifier = Modifier.height(16.dp))

        // Quick stats row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
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

        Spacer(modifier = Modifier.height(16.dp))

        // Sharpe / Sortino / Calmar
        state.analytics?.let { a ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickStatCard("Sharpe", String.format("%.2f", a.sharpeRatio), Modifier.weight(1f))
                QuickStatCard("Sortino", String.format("%.2f", a.sortinoRatio), Modifier.weight(1f))
                QuickStatCard("Max DD", Formatters.formatPercent(a.maxDrawdownPercent), Modifier.weight(1f), valueColor = Red400)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Recent signals
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

        Spacer(modifier = Modifier.height(16.dp))

        // Positions
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

        state.error?.let {
            Spacer(modifier = Modifier.height(8.dp))
            ErrorMessage(message = it, onRetry = { viewModel.loadDashboard() })
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
