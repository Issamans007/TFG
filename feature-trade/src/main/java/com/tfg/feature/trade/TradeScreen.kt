package com.tfg.feature.trade

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.tfg.domain.model.*

@Composable
fun TradeScreen(
    viewModel: TradeViewModel = hiltViewModel(),
    onNavigateToChart: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 80.dp)
    ) {
        // Symbol header
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(state.symbol, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                state.ticker?.let {
                    Row {
                        PriceText(price = it.price, fontSize = 18)
                        Spacer(modifier = Modifier.width(8.dp))
                        PnlText(value = it.priceChangePercent, showPercent = true, fontSize = 14)
                    }
                }
            }
            TextButton(onClick = { onNavigateToChart(state.symbol) }) {
                Text("Chart", color = AccentBlue)
            }
        }

        // Paper trading toggle
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Paper Trading", color = TextSecondary, fontSize = 13.sp)
                if (state.isPaper) {
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusChip(text = "DEMO", color = AccentOrange)
                }
            }
            Switch(
                checked = state.isPaper,
                onCheckedChange = { viewModel.togglePaper() },
                colors = SwitchDefaults.colors(checkedTrackColor = AccentOrange)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Buy / Sell tabs
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            TfgButton(
                text = "BUY",
                onClick = { viewModel.setSide(OrderSide.BUY) },
                modifier = Modifier.weight(1f),
                type = if (state.orderSide == OrderSide.BUY) ButtonType.BUY else ButtonType.OUTLINE
            )
            Spacer(modifier = Modifier.width(8.dp))
            TfgButton(
                text = "SELL",
                onClick = { viewModel.setSide(OrderSide.SELL) },
                modifier = Modifier.weight(1f),
                type = if (state.orderSide == OrderSide.SELL) ButtonType.SELL else ButtonType.OUTLINE
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Order type selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(OrderType.MARKET, OrderType.LIMIT, OrderType.STOP_LIMIT, OrderType.OCO).forEach { type ->
                FilterChip(
                    selected = state.orderType == type,
                    onClick = { viewModel.setType(type) },
                    label = { Text(type.name.replace("_", " "), fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentBlue.copy(alpha = 0.2f),
                        selectedLabelColor = AccentBlue
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Order form fields
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            if (state.orderType != OrderType.MARKET) {
                OutlinedTextField(
                    value = state.price,
                    onValueChange = { viewModel.setPrice(it) },
                    label = { Text(if (state.orderType == OrderType.OCO) "Limit Price (USDT)" else "Price (USDT)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = com.tfg.core.ui.tfgTextFieldColors()
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (state.orderType == OrderType.OCO) {
                OutlinedTextField(
                    value = state.stopPrice,
                    onValueChange = { viewModel.setStopPrice(it) },
                    label = { Text("Stop Price (USDT)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = com.tfg.core.ui.tfgTextFieldColors()
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = state.quantity,
                onValueChange = { viewModel.setQuantity(it) },
                label = { Text("Quantity") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = com.tfg.core.ui.tfgTextFieldColors()
            )
            Spacer(modifier = Modifier.height(12.dp))

            // SL/TP row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.slPrice,
                    onValueChange = { viewModel.setSlPrice(it) },
                    label = { Text("Stop Loss") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = com.tfg.core.ui.tfgTextFieldColors()
                )
                OutlinedTextField(
                    value = state.tpPrice,
                    onValueChange = { viewModel.setTpPrice(it) },
                    label = { Text("Take Profit") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = com.tfg.core.ui.tfgTextFieldColors()
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = state.trailingPercent,
                onValueChange = { viewModel.setTrailingPercent(it) },
                label = { Text("Trailing Stop %") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = com.tfg.core.ui.tfgTextFieldColors()
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Place order button
        TfgButton(
            text = "${state.orderSide.name} ${state.symbol}",
            onClick = { viewModel.placeOrder() },
            modifier = Modifier.padding(horizontal = 16.dp),
            isLoading = state.isLoading,
            enabled = state.quantity.isNotBlank(),
            type = if (state.orderSide == OrderSide.BUY) ButtonType.BUY else ButtonType.SELL
        )

        state.error?.let {
            Spacer(modifier = Modifier.height(8.dp))
            ErrorMessage(message = it)
        }
        state.successMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = Green500, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Open orders
        SectionHeader(title = "Open Orders")
        state.openOrders.forEach { order ->
            OrderRow(order = order, onCancel = { viewModel.cancelOrder(order.id) })
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Recent history
        SectionHeader(title = "Order History")
        state.orderHistory.take(10).forEach { order ->
            OrderRow(order = order)
        }
    }
}

@Composable
private fun OrderRow(order: Order, onCancel: (() -> Unit)? = null) {
    TfgCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusChip(
                        text = order.side.name,
                        color = if (order.side == OrderSide.BUY) Green500 else Red500
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(order.type.name, fontSize = 11.sp, color = TextTertiary)
                    if (order.executionMode == ExecutionMode.PAPER) {
                        Spacer(modifier = Modifier.width(4.dp))
                        StatusChip(text = "PAPER", color = AccentOrange)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${order.quantity} @ ${Formatters.formatPrice(order.price ?: 0.0)}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                StatusChip(
                    text = order.status.name,
                    color = when (order.status) {
                        OrderStatus.FILLED -> Green500
                        OrderStatus.CANCELLED -> Red400
                        OrderStatus.PENDING, OrderStatus.NEW -> AccentBlue
                        else -> TextSecondary
                    }
                )
                if (onCancel != null && order.status in listOf(OrderStatus.NEW, OrderStatus.PENDING, OrderStatus.PARTIALLY_FILLED)) {
                    TextButton(onClick = onCancel) {
                        Text("Cancel", color = Red400, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
