package com.tfg.feature.portfolio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tfg.domain.model.*
import com.tfg.domain.repository.PortfolioRepository
import com.tfg.domain.usecase.analytics.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PortfolioUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val portfolio: Portfolio? = null,
    val analytics: AnalyticsSnapshot? = null,
    val equityCurve: List<EquityPoint> = emptyList(),
    val dailyPnl: List<DailyPnlEntry> = emptyList(),
    val pairPerformance: List<PairPerformance> = emptyList(),
    val selectedTab: PortfolioTab = PortfolioTab.OVERVIEW,
    val isPaper: Boolean = false,
    val csvContent: String? = null,
    val error: String? = null
)

enum class PortfolioTab { OVERVIEW, ANALYTICS, POSITIONS, HISTORY }

@HiltViewModel
class PortfolioViewModel @Inject constructor(
    private val portfolioRepository: PortfolioRepository,
    private val getPortfolioUseCase: GetPortfolioUseCase,
    private val getAnalyticsUseCase: GetAnalyticsUseCase,
    private val getEquityCurveUseCase: GetEquityCurveUseCase,
    private val getDailyPnlUseCase: GetDailyPnlUseCase,
    private val getPairPerformanceUseCase: GetPairPerformanceUseCase,
    private val exportTradesUseCase: ExportTradesUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(PortfolioUiState())
    val state: StateFlow<PortfolioUiState> = _state

    init {
        load()
    }

    fun refreshBalances() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true, error = null) }
            try {
                // Await the refresh so subsequent Flow emissions reflect fresh
                // data; previously we returned before refresh completed and the
                // UI showed stale numbers until the next tick.
                portfolioRepository.refreshPortfolio()
                // Also pull one fresh snapshot synchronously so the UI updates
                // immediately even if the underlying Flow hasn't re-emitted yet.
                val fresh = try { portfolioRepository.getPortfolio().first() } catch (_: Exception) { null }
                if (fresh != null) {
                    _state.update { it.copy(portfolio = fresh) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Refresh failed: ${e.message}") }
            } finally {
                _state.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            // Refresh portfolio data in parallel (don't block UI)
            launch {
                try {
                    portfolioRepository.refreshPortfolio()
                } catch (e: Exception) {
                    timber.log.Timber.e(e, "Portfolio refresh failed")
                }
            }

            // Get portfolio — stop loading as soon as this arrives
            launch {
                try {
                    getPortfolioUseCase().collect { portfolio ->
                        _state.update { it.copy(portfolio = portfolio, isLoading = false) }
                    }
                } catch (_: Exception) {
                    _state.update { it.copy(isLoading = false) }
                }
            }
            
            // Get analytics for last 30 days
            val now = System.currentTimeMillis()
            val thirtyDaysAgo = now - (30L * 24 * 60 * 60 * 1000)
            launch {
                try {
                    getAnalyticsUseCase(thirtyDaysAgo, now, _state.value.isPaper).collect { analytics ->
                        _state.update { it.copy(analytics = analytics) }
                    }
                } catch (_: Exception) { }
            }
            
            // Get equity curve
            launch {
                try {
                    getEquityCurveUseCase(_state.value.isPaper).collect { equity ->
                        _state.update { it.copy(equityCurve = equity) }
                    }
                } catch (_: Exception) { }
            }
            
            // Get daily P&L
            launch {
                try {
                    getDailyPnlUseCase(30, _state.value.isPaper).collect { pnl ->
                        _state.update { it.copy(dailyPnl = pnl) }
                    }
                } catch (_: Exception) { }
            }
            
            // Get pair performance
            launch {
                try {
                    getPairPerformanceUseCase(_state.value.isPaper).collect { pairs ->
                        _state.update { it.copy(pairPerformance = pairs) }
                    }
                } catch (_: Exception) { }
            }

            // Fallback: stop loading after 3s no matter what
            launch {
                kotlinx.coroutines.delay(3_000)
                if (_state.value.isLoading) _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun selectTab(tab: PortfolioTab) = _state.update { it.copy(selectedTab = tab) }
    fun togglePaper() {
        _state.update { it.copy(isPaper = !it.isPaper) }
        load()
    }

    fun exportCsv() {
        viewModelScope.launch {
            try {
                val csv = exportTradesUseCase.exportCsv()
                _state.update { it.copy(csvContent = csv) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearCsvContent() {
        _state.update { it.copy(csvContent = null) }
    }
}
