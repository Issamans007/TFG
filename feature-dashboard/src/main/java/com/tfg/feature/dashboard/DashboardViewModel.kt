package com.tfg.feature.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tfg.domain.model.*
import com.tfg.domain.usecase.analytics.GetAnalyticsUseCase
import com.tfg.domain.usecase.analytics.GetPortfolioUseCase
import com.tfg.domain.repository.DonationRepository
import com.tfg.domain.repository.SignalRepository
import com.tfg.engine.EngineManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    val error: String? = null,
    val cardConfigs: List<DashboardCardConfig> = DashboardCardType.entries.mapIndexed { i, type ->
        DashboardCardConfig(type = type, visible = true, order = i)
    },
    val isCustomizing: Boolean = false
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getPortfolioUseCase: GetPortfolioUseCase,
    private val getAnalyticsUseCase: GetAnalyticsUseCase,
    private val signalRepository: SignalRepository,
    private val donationRepository: DonationRepository,
    private val engineManager: EngineManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state

    private val prefs = context.getSharedPreferences("dashboard_cards", Context.MODE_PRIVATE)

    init {
        loadCardConfig()
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

    // ─── Card Customization ─────────────────────────────────────────

    fun toggleCustomizing() {
        _state.update { it.copy(isCustomizing = !it.isCustomizing) }
    }

    fun toggleCardVisibility(type: DashboardCardType) {
        _state.update { s ->
            s.copy(cardConfigs = s.cardConfigs.map {
                if (it.type == type) it.copy(visible = !it.visible) else it
            })
        }
        saveCardConfig()
    }

    fun moveCard(fromIndex: Int, toIndex: Int) {
        _state.update { s ->
            val list = s.cardConfigs.toMutableList()
            if (fromIndex in list.indices && toIndex in list.indices) {
                val item = list.removeAt(fromIndex)
                list.add(toIndex, item)
                s.copy(cardConfigs = list.mapIndexed { i, c -> c.copy(order = i) })
            } else s
        }
        saveCardConfig()
    }

    private fun loadCardConfig() {
        val json = prefs.getString("card_order", null) ?: return
        try {
            val entries = json.split(";").mapNotNull { entry ->
                val parts = entry.split(",")
                if (parts.size == 3) {
                    val type = DashboardCardType.entries.find { it.name == parts[0] } ?: return@mapNotNull null
                    DashboardCardConfig(type, parts[1].toBoolean(), parts[2].toInt())
                } else null
            }.sortedBy { it.order }
            if (entries.isNotEmpty()) {
                // Merge: keep new card types that might have been added
                val savedTypes = entries.map { it.type }.toSet()
                val missing = DashboardCardType.entries.filter { it !in savedTypes }
                    .mapIndexed { i, t -> DashboardCardConfig(t, true, entries.size + i) }
                _state.update { it.copy(cardConfigs = entries + missing) }
            }
        } catch (_: Exception) { }
    }

    private fun saveCardConfig() {
        val json = _state.value.cardConfigs.joinToString(";") {
            "${it.type.name},${it.visible},${it.order}"
        }
        prefs.edit().putString("card_order", json).apply()
    }
}
