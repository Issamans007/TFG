package com.tfg.feature.portfolio

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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.platform.LocalContext
import com.tfg.core.ui.components.*
import com.tfg.core.ui.theme.*
import com.tfg.core.util.Formatters
import com.tfg.domain.model.EquityPoint

@Composable
fun PortfolioScreen(
    viewModel: PortfolioViewModel = hiltViewModel(),
    onNavigateToTrade: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    // Share CSV when it's ready
    LaunchedEffect(state.csvContent) {
        val csv = state.csvContent ?: return@LaunchedEffect
        val file = java.io.File(context.cacheDir, "trades_export.csv")
        file.writeText(csv)
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(shareIntent, "Export Trades"))
        viewModel.clearCsvContent()
    }

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
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Portfolio", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { viewModel.refreshBalances() },
                    modifier = Modifier.size(32.dp)
                ) {
                    if (state.isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = AccentBlue,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = AccentBlue, modifier = Modifier.size(20.dp))
                    }
                }
                Text("Paper", fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.width(4.dp))
                Switch(
                    checked = state.isPaper,
                    onCheckedChange = { viewModel.togglePaper() },
                    colors = SwitchDefaults.colors(checkedTrackColor = AccentOrange),
                    modifier = Modifier.height(28.dp)
                )
            }
        }

        // Portfolio value
        state.portfolio?.let { p ->
            TfgCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text("Total Value", fontSize = 12.sp, color = TextSecondary)
                Text(
                    Formatters.formatUsdt(p.totalValueUsdt),
                    fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    PnlText(value = p.totalPnlUsdt, fontSize = 16)
                    Spacer(modifier = Modifier.width(8.dp))
                    PnlText(value = p.totalPnlPercent, showPercent = true, fontSize = 14)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tabs
        ScrollableTabRow(
            selectedTabIndex = PortfolioTab.entries.indexOf(state.selectedTab),
            containerColor = DarkBackground,
            edgePadding = 16.dp,
            divider = {}
        ) {
            PortfolioTab.entries.forEach { tab ->
                Tab(
                    selected = state.selectedTab == tab,
                    onClick = { viewModel.selectTab(tab) },
                    text = { Text(tab.name, fontSize = 13.sp,
                        color = if (state.selectedTab == tab) AccentBlue else TextSecondary) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (state.selectedTab) {
            PortfolioTab.OVERVIEW -> OverviewTab(state)
            PortfolioTab.ANALYTICS -> AnalyticsTab(state)
            PortfolioTab.POSITIONS -> PositionsTab(state, onNavigateToTrade)
            PortfolioTab.HISTORY -> HistoryTab(state, viewModel)
        }

        state.error?.let {
            ErrorMessage(message = it, onRetry = { viewModel.load() })
        }
    }
}

@Composable
private fun OverviewTab(state: PortfolioUiState) {
    // Equity curve chart
    if (state.equityCurve.isNotEmpty()) {
        SectionHeader(title = "Equity Curve")
        EquityChart(
            points = state.equityCurve,
            modifier = Modifier.fillMaxWidth().height(200.dp).padding(horizontal = 16.dp)
        )
    }

    // Wallet balances grouped by type
    state.portfolio?.balances?.filter { it.totalValue > 0 }?.let { allBalances ->
        val grouped = allBalances.groupBy { it.walletType }
        val walletOrder = listOf("SPOT", "FUNDING", "FUTURES")

        walletOrder.forEach { walletType ->
            val balances = grouped[walletType] ?: return@forEach
            val walletTotal = balances.sumOf { it.usdValue }

            Spacer(modifier = Modifier.height(16.dp))

            // Wallet section header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        when (walletType) {
                            "SPOT" -> Icons.Default.AccountBalanceWallet
                            "FUNDING" -> Icons.Default.Savings
                            "FUTURES" -> Icons.Default.TrendingUp
                            else -> Icons.Default.AccountBalance
                        },
                        contentDescription = null,
                        tint = when (walletType) {
                            "SPOT" -> AccentBlue
                            "FUNDING" -> AccentGold
                            "FUTURES" -> AccentOrange
                            else -> TextSecondary
                        },
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "$walletType Wallet",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
                Text(
                    Formatters.formatUsdt(walletTotal),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = when (walletType) {
                        "SPOT" -> AccentBlue
                        "FUNDING" -> AccentGold
                        "FUTURES" -> AccentOrange
                        else -> TextPrimary
                    }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Assets in this wallet
            balances.sortedByDescending { it.usdValue }.forEach { balance ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(balance.asset, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Row {
                            Text("Free: ${String.format("%.6f", balance.free)}", fontSize = 11.sp, color = TextTertiary)
                            if (balance.locked > 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Locked: ${String.format("%.6f", balance.locked)}", fontSize = 11.sp, color = AccentOrange.copy(alpha = 0.7f))
                            }
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            Formatters.formatUsdt(balance.usdValue),
                            color = TextPrimary,
                            fontSize = 14.sp
                        )
                        Text(
                            "${String.format("%.6f", balance.free + balance.locked)}",
                            fontSize = 11.sp,
                            color = TextTertiary
                        )
                    }
                }
            }
        }

        // Show message if no balances at all
        if (allBalances.isEmpty()) {
            Spacer(modifier = Modifier.height(32.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No assets found. Tap refresh to load from Binance.", color = TextTertiary, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun AnalyticsTab(state: PortfolioUiState) {
    state.analytics?.let { a ->
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            // Key metrics grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard("Win Rate", "${String.format("%.1f", a.winRate)}%", Modifier.weight(1f))
                MetricCard("Profit Factor", String.format("%.2f", a.profitFactor), Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard("Sharpe Ratio", String.format("%.2f", a.sharpeRatio), Modifier.weight(1f))
                MetricCard("Sortino Ratio", String.format("%.2f", a.sortinoRatio), Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard("Calmar Ratio", String.format("%.2f", a.calmarRatio), Modifier.weight(1f))
                MetricCard("Max Drawdown", "${String.format("%.2f", a.maxDrawdownPercent)}%", Modifier.weight(1f), LossRed)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard("Total Trades", a.totalTrades.toString(), Modifier.weight(1f))
                MetricCard("Avg Hold Time", "${a.avgHoldTimeMinutes}m", Modifier.weight(1f))
            }
        }

        // Pair performance
        if (state.pairPerformance.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            SectionHeader(title = "Performance by Pair")
            state.pairPerformance.forEach { pp ->
                TfgCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 3.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(pp.symbol, fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 14.sp)
                            Text("${pp.trades} trades | WR: ${String.format("%.0f", pp.winRate)}%", fontSize = 11.sp, color = TextTertiary)
                        }
                        PnlText(value = pp.totalPnl, fontSize = 16)
                    }
                }
            }
        }
    }
}

@Composable
private fun PositionsTab(state: PortfolioUiState, onNavigateToTrade: (String) -> Unit) {
    val positions = state.portfolio?.positions ?: emptyList()
    if (positions.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("No open positions", color = TextTertiary)
        }
    } else {
        positions.forEach { pos ->
            TfgCard(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                onClick = { onNavigateToTrade(pos.symbol) }
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(pos.symbol, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text("${pos.quantity} @ ${Formatters.formatPrice(pos.entryPrice)}", fontSize = 12.sp, color = TextSecondary)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        PriceText(price = pos.currentPrice)
                        PnlText(value = pos.unrealizedPnl)
                        PnlText(value = pos.unrealizedPnlPercent, showPercent = true, fontSize = 11)
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryTab(state: PortfolioUiState, viewModel: PortfolioViewModel) {
    // Daily PnL
    SectionHeader(title = "Daily P&L (30d)", action = "Export CSV", onAction = { viewModel.exportCsv() })
    state.dailyPnl.forEach { entry ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(Formatters.formatDate(entry.date), fontSize = 13.sp, color = TextSecondary)
            PnlText(value = entry.pnl, fontSize = 14)
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = TextPrimary) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard)
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(label, fontSize = 11.sp, color = TextTertiary)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}

@Composable
private fun EquityChart(points: List<EquityPoint>, modifier: Modifier = Modifier) {
    if (points.size < 2) return

    val values = points.map { it.equity }
    val minVal = values.minOrNull() ?: 0.0
    val maxVal = values.maxOrNull() ?: 0.0
    val range = maxVal - minVal

    Canvas(modifier = modifier.clip(RoundedCornerShape(12.dp)).background(DarkCard)) {
        val w = size.width
        val h = size.height
        val stepX = w / (points.size - 1)

        val path = Path()
        points.forEachIndexed { i, point ->
            val x = i * stepX
            val y = h - ((point.equity - minVal) / range * h).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        val isPositive = values.last() >= values.first()
        drawPath(
            path = path,
            color = if (isPositive) Green500 else Red500,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}
