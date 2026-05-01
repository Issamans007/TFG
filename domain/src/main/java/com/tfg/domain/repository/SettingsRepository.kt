package com.tfg.domain.repository

import com.tfg.domain.model.*
import kotlinx.coroutines.flow.Flow

interface RiskRepository {
    fun getRiskConfig(): Flow<RiskConfig>
    suspend fun updateRiskConfig(config: RiskConfig)
    suspend fun saveRiskConfig(config: RiskConfig) = updateRiskConfig(config) // Alias for updateRiskConfig
    suspend fun checkOrderRisk(order: Order, portfolio: Portfolio): RiskCheckResult
    suspend fun activateKillSwitch()
    suspend fun deactivateKillSwitch()
    fun isKillSwitchActive(): Flow<Boolean>
}

interface SettingsRepository {
    fun getBotConfig(): Flow<BotConfig>
    suspend fun updateBotConfig(config: BotConfig)
    suspend fun setApiKey(apiKey: String, apiSecret: String): Result<Boolean>
    suspend fun revokeApiKey(): Result<Boolean>
    suspend fun validateApiPermissions(): Result<Boolean>
    fun getTheme(): Flow<String>
    suspend fun setTheme(theme: String)
    fun getLanguage(): Flow<String>
    suspend fun setLanguage(language: String)
    fun getActiveSessions(): Flow<List<String>>
    
    // Additional methods for SettingsScreen/ViewModel
    suspend fun isBotEnabled(): Boolean
    suspend fun setBotEnabled(enabled: Boolean)
    suspend fun isPaperTrading(): Boolean
    suspend fun setPaperTrading(enabled: Boolean)
    suspend fun getDonationPercent(): Double
    suspend fun setDonationPercent(percent: Double)
    suspend fun hasApiKey(): Boolean
    suspend fun isBiometricEnabled(): Boolean
    suspend fun setBiometricEnabled(enabled: Boolean)
    suspend fun saveApiCredentials(apiKey: String, apiSecret: String)
    suspend fun getApiKey(): String?
    suspend fun getApiSecret(): String?

    // ── Futures settings (defaults preserve spot behavior) ─────────
    suspend fun isFuturesEnabled(): Boolean = false
    suspend fun setFuturesEnabled(enabled: Boolean) {}
    suspend fun getDefaultLeverage(): Int = 1
    suspend fun setDefaultLeverage(leverage: Int) {}
    suspend fun getDefaultMarginType(): MarginType = MarginType.ISOLATED
    suspend fun setDefaultMarginType(type: MarginType) {}
    suspend fun getMaxLeverage(): Int = 20
    suspend fun setMaxLeverage(leverage: Int) {}
}
