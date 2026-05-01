package com.tfg.feature.settings

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tfg.core.util.HapticManager
import com.tfg.core.util.SoundManager
import com.tfg.domain.model.*
import com.tfg.domain.repository.RiskRepository
import com.tfg.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    val theme: String = "dark",
    val hapticEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val soundOrderFill: SoundManager.SoundType = SoundManager.SoundType.CHIME,
    val soundSlHit: SoundManager.SoundType = SoundManager.SoundType.ERROR,
    val soundTpHit: SoundManager.SoundType = SoundManager.SoundType.SUCCESS,
    val soundAlert: SoundManager.SoundType = SoundManager.SoundType.ALERT_TONE,
    val error: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val riskRepository: RiskRepository,
    @ApplicationContext private val context: Context
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
                    biometricEnabled = biometricEnabled,
                    hapticEnabled = HapticManager.isEnabled(context),
                    soundEnabled = SoundManager.isEnabled(context),
                    soundOrderFill = SoundManager.getSoundForEvent(context, SoundManager.SoundEvent.ORDER_FILL),
                    soundSlHit = SoundManager.getSoundForEvent(context, SoundManager.SoundEvent.STOP_LOSS),
                    soundTpHit = SoundManager.getSoundForEvent(context, SoundManager.SoundEvent.TAKE_PROFIT),
                    soundAlert = SoundManager.getSoundForEvent(context, SoundManager.SoundEvent.ALERT)
                )
            }
            
            // Collect theme
            launch {
                settingsRepository.getTheme().collect { theme ->
                    _state.update { it.copy(theme = theme) }
                }
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
        // Update UI immediately so the Switch reflects the user's tap.
        _state.update { it.copy(botEnabled = enabled) }
        // Actually start/stop the 24/7 foreground service (was missing — bot
        // would never start unless the device rebooted).
        startOrStopTradingService(enabled)
        viewModelScope.launch {
            settingsRepository.setBotEnabled(enabled)
        }
    }

    private fun startOrStopTradingService(enabled: Boolean) {
        // Reference TradingForegroundService by class name to avoid a module
        // dependency on trading-engine from feature-settings.
        val component = ComponentName(
            context.packageName,
            "com.tfg.engine.TradingForegroundService"
        )
        val intent = Intent().apply {
            this.component = component
            action = if (enabled) "com.tfg.engine.START" else "com.tfg.engine.STOP"
        }
        try {
            if (enabled) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            _state.update { it.copy(error = "Failed to ${if (enabled) "start" else "stop"} bot: ${e.message}") }
        }
    }

    fun togglePaperTrading(enabled: Boolean) {
        // Update UI first for immediate feedback.
        _state.update { it.copy(paperTrading = enabled) }
        viewModelScope.launch {
            settingsRepository.setPaperTrading(enabled)
        }
    }

    fun setDonationPercent(percent: Double) {
        // Apply state immediately so the slider thumb tracks the gesture
        // smoothly (previously every drag-tick suspended on the prefs save).
        _state.update { it.copy(donationPercent = percent) }
        viewModelScope.launch {
            settingsRepository.setDonationPercent(percent)
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

    fun setTheme(theme: String) {
        viewModelScope.launch {
            settingsRepository.setTheme(theme)
            _state.update { it.copy(theme = theme) }
        }
    }

    fun activateKillSwitch() {
        viewModelScope.launch {
            riskRepository.activateKillSwitch()
            settingsRepository.setBotEnabled(false)
            _state.update { it.copy(botEnabled = false) }
            // Make sure the 24/7 trading service is actually stopped, not
            // just the in-memory flag.
            startOrStopTradingService(false)
        }
    }

    // ─── Haptic & Sound ──────────────────────────────────────────────

    fun toggleHaptic(enabled: Boolean) {
        HapticManager.setEnabled(context, enabled)
        _state.update { it.copy(hapticEnabled = enabled) }
    }

    fun toggleSound(enabled: Boolean) {
        SoundManager.setEnabled(context, enabled)
        _state.update { it.copy(soundEnabled = enabled) }
    }

    fun setSoundForEvent(event: SoundManager.SoundEvent, type: SoundManager.SoundType) {
        SoundManager.setSoundForEvent(context, event, type)
        _state.update {
            when (event) {
                SoundManager.SoundEvent.ORDER_FILL -> it.copy(soundOrderFill = type)
                SoundManager.SoundEvent.STOP_LOSS -> it.copy(soundSlHit = type)
                SoundManager.SoundEvent.TAKE_PROFIT -> it.copy(soundTpHit = type)
                SoundManager.SoundEvent.ALERT -> it.copy(soundAlert = type)
            }
        }
    }

    fun previewSound(event: SoundManager.SoundEvent) {
        SoundManager.play(context, event)
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
