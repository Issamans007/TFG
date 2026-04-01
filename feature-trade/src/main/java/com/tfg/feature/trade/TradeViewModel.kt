package com.tfg.feature.trade

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tfg.domain.model.*
import com.tfg.domain.usecase.trading.*
import com.tfg.domain.repository.MarketRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TradeUiState(
    val isLoading: Boolean = false,
    val symbol: String = "",
    val ticker: Ticker? = null,
    val orderSide: OrderSide = OrderSide.BUY,
    val orderType: OrderType = OrderType.MARKET,
    val quantity: String = "",
    val price: String = "",
    val slPrice: String = "",
    val tpPrice: String = "",
    val trailingPercent: String = "",
    val stopPrice: String = "",
    val isPaper: Boolean = false,
    val openOrders: List<Order> = emptyList(),
    val orderHistory: List<Order> = emptyList(),
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class TradeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val placeOrderUseCase: PlaceOrderUseCase,
    private val cancelOrderUseCase: CancelOrderUseCase,
    private val getOpenOrdersUseCase: GetOpenOrdersUseCase,
    private val getOrderHistoryUseCase: GetOrderHistoryUseCase,
    private val marketRepository: MarketRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TradeUiState())
    val state: StateFlow<TradeUiState> = _state

    init {
        val sym = savedStateHandle.get<String>("symbol") ?: "BTCUSDT"
        _state.update { it.copy(symbol = sym) }
        loadSymbolData(sym)
        loadOrders(sym)
    }

    private fun loadSymbolData(symbol: String) {
        viewModelScope.launch {
            try {
                marketRepository.getAllTickers().collect { tickers ->
                    val ticker = tickers.find { it.symbol == symbol }
                    _state.update { it.copy(ticker = ticker, price = ticker?.price?.toString() ?: "") }
                }
            } catch (_: Exception) {}
        }
    }

    private fun loadOrders(symbol: String) {
        viewModelScope.launch {
            try {
                getOpenOrdersUseCase().collect { orders ->
                    val filtered = orders.filter { order -> order.symbol == symbol }
                    _state.update { it.copy(openOrders = filtered) }
                }
            } catch (_: Exception) { }
        }
        viewModelScope.launch {
            try {
                getOrderHistoryUseCase(50).collect { orders ->
                    val filtered = orders.filter { order -> order.symbol == symbol }
                    _state.update { it.copy(orderHistory = filtered) }
                }
            } catch (_: Exception) { }
        }
    }

    fun setSide(side: OrderSide) = _state.update { it.copy(orderSide = side) }
    fun setType(type: OrderType) = _state.update { it.copy(orderType = type) }
    fun setQuantity(q: String) = _state.update { it.copy(quantity = q) }
    fun setPrice(p: String) = _state.update { it.copy(price = p) }
    fun setSlPrice(sl: String) = _state.update { it.copy(slPrice = sl) }
    fun setTpPrice(tp: String) = _state.update { it.copy(tpPrice = tp) }
    fun setTrailingPercent(p: String) = _state.update { it.copy(trailingPercent = p) }
    fun setStopPrice(p: String) = _state.update { it.copy(stopPrice = p) }
    fun togglePaper() = _state.update { it.copy(isPaper = !it.isPaper) }

    fun placeOrder() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val s = _state.value
            val qty = s.quantity.toDoubleOrNull() ?: run {
                _state.update { it.copy(isLoading = false, error = "Invalid quantity") }
                return@launch
            }
            val prc = s.price.toDoubleOrNull() ?: 0.0

            val tpParsed = if (s.tpPrice.isNotBlank()) {
                s.tpPrice.toDoubleOrNull() ?: run {
                    _state.update { it.copy(isLoading = false, error = "Invalid take-profit price") }
                    return@launch
                }
            } else null
            val slParsed = if (s.slPrice.isNotBlank()) {
                s.slPrice.toDoubleOrNull() ?: run {
                    _state.update { it.copy(isLoading = false, error = "Invalid stop-loss price") }
                    return@launch
                }
            } else null

            val order = Order(
                id = "",
                symbol = s.symbol,
                side = s.orderSide,
                type = s.orderType,
                quantity = qty,
                price = prc,
                stopPrice = s.stopPrice.toDoubleOrNull(),
                takeProfits = if (tpParsed != null) listOf(
                    TakeProfit("tp1", tpParsed, 100.0)
                ) else emptyList(),
                stopLosses = if (slParsed != null) listOf(
                    StopLoss("sl1", slParsed, 100.0)
                ) else emptyList(),
                trailingStopPercent = s.trailingPercent.toDoubleOrNull(),
                executionMode = if (s.isPaper) ExecutionMode.PAPER else ExecutionMode.LIVE,
                isPaperTrade = s.isPaper,
                status = OrderStatus.NEW
            )

            placeOrderUseCase(order)
                .onSuccess { placed ->
                    _state.update {
                        it.copy(isLoading = false, successMessage = "Order placed: ${placed.id}")
                    }
                    loadOrders(s.symbol)
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    fun cancelOrder(orderId: String) {
        viewModelScope.launch {
            val sym = _state.value.symbol
            cancelOrderUseCase(sym, orderId)
                .onSuccess { loadOrders(sym) }
                .onFailure { e -> _state.update { it.copy(error = e.message) } }
        }
    }

    fun clearMessages() = _state.update { it.copy(error = null, successMessage = null) }
}
