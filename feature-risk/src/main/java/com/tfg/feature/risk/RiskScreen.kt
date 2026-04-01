package com.tfg.feature.risk

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tfg.core.ui.components.*
import com.tfg.core.ui.theme.*
import com.tfg.core.util.Formatters
import com.tfg.domain.model.RiskCheckResult
import com.tfg.domain.model.RiskSeverity
import com.tfg.domain.repository.AuditRepository
import com.tfg.domain.repository.RiskRepository
import com.tfg.engine.RiskEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RiskUiState(
    val killSwitchActive: Boolean = false,
    val openTradeCount: Int = 0,
    val dailyPnl: Double = 0.0,
    val consecutiveLosses: Int = 0,
    val recentViolations: List<String> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class RiskViewModel @Inject constructor(
    private val riskRepository: RiskRepository,
    private val riskEngine: RiskEngine,
    private val auditRepository: AuditRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RiskUiState())
    val state: StateFlow<RiskUiState> = _state

    init { load() }

    private fun load() {
        viewModelScope.launch {
            riskRepository.isKillSwitchActive().collect { active ->
                _state.update { it.copy(killSwitchActive = active) }
            }
        }
        viewModelScope.launch {
            riskEngine.openTradeCountFlow.collect { count ->
                _state.update { it.copy(openTradeCount = count) }
            }
        }
        viewModelScope.launch {
            riskEngine.dailyLossFlow.collect { loss ->
                _state.update { it.copy(dailyPnl = loss) }
            }
        }
        viewModelScope.launch {
            riskEngine.consecutiveLossesFlow.collect { losses ->
                _state.update { it.copy(consecutiveLosses = losses, isLoading = false) }
            }
        }
        viewModelScope.launch {
            try {
                val logs = auditRepository.getRecentLogs(20).getOrElse { emptyList() }
                val violations = logs
                    .filter { it.details.contains("exceeds", true) || it.details.contains("limit", true) }
                    .map { it.details }
                _state.update { it.copy(recentViolations = violations) }
            } catch (_: Exception) {}
        }
    }

    fun deactivateKillSwitch() {
        viewModelScope.launch {
            riskRepository.deactivateKillSwitch()
            _state.update { it.copy(killSwitchActive = false) }
        }
    }
}

@Composable
fun RiskScreen(viewModel: RiskViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Risk Dashboard", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(modifier = Modifier.height(16.dp))

        // Kill switch status
        TfgCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Kill Switch", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text(
                        if (state.killSwitchActive) "ACTIVE - All trading halted" else "Inactive",
                        fontSize = 12.sp,
                        color = if (state.killSwitchActive) Red500 else Green500
                    )
                }
                if (state.killSwitchActive) {
                    TextButton(onClick = { viewModel.deactivateKillSwitch() }) {
                        Text("Deactivate", color = AccentBlue)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Risk metrics
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RiskMetric("Open Trades", state.openTradeCount.toString(), Modifier.weight(1f))
            RiskMetric("Daily P&L", Formatters.formatUsdt(state.dailyPnl), Modifier.weight(1f),
                if (state.dailyPnl >= 0) Green500 else Red500)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RiskMetric("Consec. Losses", state.consecutiveLosses.toString(), Modifier.weight(1f),
                if (state.consecutiveLosses > 3) Red500 else TextPrimary)
            RiskMetric("Violations", state.recentViolations.size.toString(), Modifier.weight(1f),
                if (state.recentViolations.isNotEmpty()) AccentOrange else Green500)
        }

        // Recent violations
        if (state.recentViolations.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            SectionHeader(title = "Recent Violations")
            state.recentViolations.forEach { violation ->
                TfgCard(modifier = Modifier.padding(vertical = 3.dp)) {
                    Text(violation, fontSize = 13.sp, color = AccentOrange)
                }
            }
        }
    }
}

@Composable
private fun RiskMetric(
    label: String,
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
        Text(label, fontSize = 11.sp, color = TextTertiary)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}
