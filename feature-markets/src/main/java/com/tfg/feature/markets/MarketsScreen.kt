package com.tfg.feature.markets

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
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
import com.tfg.domain.model.TradingPair

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketsScreen(
    viewModel: MarketsViewModel = hiltViewModel(),
    onPairClick: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val filteredPairs by viewModel.filteredPairs.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Fear & Greed
        state.fearGreedIndex?.let { idx ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkCard)
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Fear & Greed Index", fontSize = 13.sp, color = TextSecondary)
                    Text(
                        text = "$idx",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            idx <= 25 -> Red500
                            idx <= 50 -> AccentOrange
                            idx <= 75 -> AccentGold
                            else -> Green500
                        }
                    )
                }
            }
        }

        // Search bar
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { viewModel.search(it) },
            placeholder = { Text("Search markets...", color = TextTertiary) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = TextSecondary) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentBlue,
                unfocusedBorderColor = DarkBorder,
                cursorColor = AccentBlue,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Tabs
        ScrollableTabRow(
            selectedTabIndex = MarketTab.entries.indexOf(state.selectedTab),
            containerColor = DarkBackground,
            edgePadding = 16.dp,
            divider = {}
        ) {
            MarketTab.entries.forEach { tab ->
                Tab(
                    selected = state.selectedTab == tab,
                    onClick = { viewModel.selectTab(tab) },
                    text = {
                        Text(
                            tab.name.replace("_", " "),
                            color = if (state.selectedTab == tab) AccentBlue else TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                )
            }
        }

        // List
        if (state.isLoading) {
            LoadingIndicator()
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filteredPairs, key = { it.symbol }) { pair ->
                    val ticker = state.tickers[pair.symbol]
                    val isWatchlisted = state.watchlist.contains(pair.symbol)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkCard)
                            .clickable { onPairClick(pair.symbol) }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { viewModel.toggleWatchlist(pair.symbol) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = if (isWatchlisted) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                    contentDescription = "Watchlist",
                                    tint = if (isWatchlisted) AccentGold else TextTertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(pair.baseAsset, fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 14.sp)
                                Text("/${pair.quoteAsset}", fontSize = 11.sp, color = TextTertiary)
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            PriceText(price = ticker?.price ?: 0.0, fontSize = 14)
                            ticker?.let {
                                PnlText(value = it.priceChangePercent, showPercent = true, fontSize = 12)
                            }
                        }
                    }
                }
            }
        }

        state.error?.let {
            ErrorMessage(message = it, onRetry = { viewModel.loadMarkets() })
        }
    }
}
