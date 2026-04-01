package com.tfg.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tfg.domain.model.*
import com.tfg.domain.repository.RiskRepository
import com.tfg.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val botEnabled: Boolean = false,
    val paperTrading: Boolean = false,
    val donationPercent: Double = 10.0,
    val riskConfig: RiskConfig = RiskConfig(),
    val apiKeyConfigured: Boolean = false,
    val biometricEnabled: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val riskRepository: RiskRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state

    init { loadSettings() }

    private fun loadSettings() {
        viewModelScope.launch {
            val botEnabled = settingsRepository.isBotEnabled()
            val paperTrading = settingsRepository.isPaperTrading()
            val donationPercent = settingsRepository.getDonationPercent()
            val apiKeyConfigured = settingsRepository.hasApiKey()
            val biometricEnabled = settingsRepository.isBiometricEnabled()
            
            // Set non-flow values immediately so UI reflects them right away
            _state.update {
                it.copy(
                    botEnabled = botEnabled,
                    paperTrading = paperTrading,
                    donationPercent = donationPercent,
                    apiKeyConfigured = apiKeyConfigured,
                    biometricEnabled = biometricEnabled
                )
            }
            
            // Collect riskConfig separately — only update the riskConfig field
            launch {
                riskRepository.getRiskConfig().collect { risk ->
                    _state.update { it.copy(riskConfig = risk) }
                }
            }
        }
    }

    fun toggleBot(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBotEnabled(enabled)
            _state.update { it.copy(botEnabled = enabled) }
        }
    }

    fun togglePaperTrading(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setPaperTrading(enabled)
            _state.update { it.copy(paperTrading = enabled) }
        }
    }

    fun setDonationPercent(percent: Double) {
        viewModelScope.launch {
            settingsRepository.setDonationPercent(percent)
            _state.update { it.copy(donationPercent = percent) }
        }
    }

    fun updateRiskConfig(config: RiskConfig) {
        viewModelScope.launch {
            riskRepository.saveRiskConfig(config)
            _state.update { it.copy(riskConfig = config, saved = true) }
        }
    }

    fun toggleBiometric(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBiometricEnabled(enabled)
            _state.update { it.copy(biometricEnabled = enabled) }
        }
    }

    fun activateKillSwitch() {
        viewModelScope.launch {
            riskRepository.activateKillSwitch()
            _state.update { it.copy(botEnabled = false) }
        }
    }

    fun clearSaved() {
        _state.update { it.copy(saved = false) }
    }

    fun saveApiKeys(apiKey: String, apiSecret: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            settingsRepository.setApiKey(apiKey, apiSecret)
                .onSuccess {
                    _state.update { it.copy(apiKeyConfigured = true, saved = true, error = null) }
                    onSuccess()
                }
                .onFailure { e ->
                    _state.update { it.copy(error = e.message) }
                    onError(e.message ?: "Failed to save API keys")
                }
        }
    }

    fun revokeApiKeys(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            settingsRepository.revokeApiKey()
                .onSuccess {
                    _state.update { it.copy(apiKeyConfigured = false, saved = true) }
                    onSuccess()
                }
                .onFailure { e ->
                    onError(e.message ?: "Failed to revoke API keys")
                }
        }
    }
}
