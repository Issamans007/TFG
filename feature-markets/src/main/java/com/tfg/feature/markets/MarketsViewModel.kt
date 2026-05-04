package com.tfg.feature.markets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tfg.domain.model.TradingPair
import com.tfg.domain.model.Ticker
import com.tfg.domain.repository.MarketRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class MarketsUiState(
    val isLoading: Boolean = true,
    val pairs: List<TradingPair> = emptyList(),
    val tickers: Map<String, Ticker> = emptyMap(),
    val watchlist: List<String> = emptyList(),
    val searchQuery: String = "",
    val selectedTab: MarketTab = MarketTab.ALL,
    val fearGreedIndex: Int? = null,
    val error: String? = null
)

enum class MarketTab { ALL, WATCHLIST, GAINERS, LOSERS }

@HiltViewModel
class MarketsViewModel @Inject constructor(
    private val marketRepository: MarketRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MarketsUiState())
    val state: StateFlow<MarketsUiState> = _state

    init {
        loadMarkets()
    }

    fun loadMarkets() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            // Refresh pairs in parallel (don't block other collectors)
            launch {
                try {
                    marketRepository.refreshPairs()
                } catch (e: Exception) {
                    _state.update { it.copy(error = "Failed to refresh: ${e.message}") }
                }
            }

            // Collect trading pairs (Room Flow – auto-updates)
            launch {
                try {
                    marketRepository.getTradingPairs().collect { pairs ->
                        _state.update {
                            it.copy(
                                pairs = pairs,
                                isLoading = if (pairs.isNotEmpty()) false else it.isLoading
                            )
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to load trading pairs")
                    _state.update { it.copy(isLoading = false) }
                }
            }

            // Collect tickers and convert to map
            launch {
                try {
                    marketRepository.getAllTickers().collect { tickers ->
                        val tickerMap = tickers.associateBy { it.symbol }
                        _state.update { it.copy(tickers = tickerMap, isLoading = false) }
                    }
                } catch (e: Exception) {
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
            }

            // Collect watchlist
            launch {
                try {
                    marketRepository.getWatchlist().collect { watchlist ->
                        _state.update { it.copy(watchlist = watchlist.map { it.symbol }) }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to load watchlist")
                }
            }

            // Collect market data
            launch {
                try {
                    marketRepository.getMarketData().collect { marketData ->
                        _state.update { it.copy(fearGreedIndex = marketData.fearGreedIndex) }
                    }
                } catch (e: Exception) { Timber.w(e, "Failed to load market data") }
            }

            // Fallback: stop loading after 5s no matter what
            launch {
                kotlinx.coroutines.delay(5_000)
                if (_state.value.isLoading) {
                    _state.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun search(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun selectTab(tab: MarketTab) {
        _state.update { it.copy(selectedTab = tab) }
    }

    fun toggleWatchlist(symbol: String) {
        viewModelScope.launch {
            val current = _state.value.watchlist.toMutableList()
            if (current.contains(symbol)) {
                current.remove(symbol)
                marketRepository.removeFromWatchlist(symbol)
            } else {
                current.add(symbol)
                marketRepository.addToWatchlist(symbol)
            }
            _state.update { it.copy(watchlist = current) }
        }
    }

    val filteredPairs: StateFlow<List<TradingPair>> = _state
        // Debounce only the searchQuery so list filter recomputes don't run on every keystroke;
        // ticker / watchlist updates still flow through immediately.
        .debounce { if (it.searchQuery.isBlank()) 0L else 200L }
        .map { s ->
            var list = s.pairs
            if (s.searchQuery.isNotBlank()) {
                list = list.filter {
                    it.symbol.contains(s.searchQuery, ignoreCase = true) ||
                    it.baseAsset.contains(s.searchQuery, ignoreCase = true)
                }
            }
            when (s.selectedTab) {
                MarketTab.WATCHLIST -> list.filter { s.watchlist.contains(it.symbol) }
                MarketTab.GAINERS -> {
                    list.filter { p -> s.tickers[p.symbol]?.let { it.priceChangePercent > 0 } == true }
                        .sortedByDescending { p -> s.tickers[p.symbol]?.priceChangePercent ?: 0.0 }
                }
                MarketTab.LOSERS -> {
                    list.filter { p -> s.tickers[p.symbol]?.let { it.priceChangePercent < 0 } == true }
                        .sortedBy { p -> s.tickers[p.symbol]?.priceChangePercent ?: 0.0 }
                }
                else -> list
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
