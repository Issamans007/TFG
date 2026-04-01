package com.tfg.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tfg.domain.model.*
import com.tfg.domain.usecase.analytics.GetAnalyticsUseCase
import com.tfg.domain.usecase.analytics.GetPortfolioUseCase
import com.tfg.domain.repository.DonationRepository
import com.tfg.domain.repository.SignalRepository
import com.tfg.engine.EngineManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = true,
    val portfolio: Portfolio? = null,
    val analytics: AnalyticsSnapshot? = null,
    val recentSignals: List<Signal> = emptyList(),
    val totalDonated: Double = 0.0,
    val botActive: Boolean = false,
    val connectionState: ConnectionState? = null,
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getPortfolioUseCase: GetPortfolioUseCase,
    private val getAnalyticsUseCase: GetAnalyticsUseCase,
    private val signalRepository: SignalRepository,
    private val donationRepository: DonationRepository,
    private val engineManager: EngineManager
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state

    init {
        loadDashboard()
        // Observe engine active state
        viewModelScope.launch {
            engineManager.hasActiveEngine.collect { active ->
                _state.update { it.copy(botActive = active) }
            }
        }
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            // Get portfolio
            launch {
                try {
                    getPortfolioUseCase().collect { portfolio ->
                        _state.update { it.copy(portfolio = portfolio) }
                    }
                } catch (e: Exception) {
                    _state.update { it.copy(error = e.message) }
                }
            }
            
            // Get analytics for last 30 days
            val now = System.currentTimeMillis()
            val thirtyDaysAgo = now - (30L * 24 * 60 * 60 * 1000)
            launch {
                try {
                    getAnalyticsUseCase(thirtyDaysAgo, now, false).collect { analytics ->
                        _state.update { it.copy(analytics = analytics) }
                    }
                } catch (_: Exception) { }
            }
            
            // Get total donated
            launch {
                try {
                    donationRepository.getTotalDonated().collect { total ->
                        _state.update { it.copy(totalDonated = total, isLoading = false) }
                    }
                } catch (_: Exception) {
                    _state.update { it.copy(isLoading = false) }
                }
            }

            // Fallback timeout
            launch {
                kotlinx.coroutines.delay(10_000)
                if (_state.value.isLoading) _state.update { it.copy(isLoading = false) }
            }
        }

        viewModelScope.launch {
            try {
                val signals = signalRepository.getRecentSignals(10).getOrElse { emptyList() }
                _state.update { it.copy(recentSignals = signals) }
            } catch (e: Exception) {
                // Silent failure for signals
            }
        }
    }
}
