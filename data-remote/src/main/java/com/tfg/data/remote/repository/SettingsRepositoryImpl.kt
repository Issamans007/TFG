package com.tfg.data.remote.repository

import android.content.SharedPreferences
import com.tfg.core.util.Constants
import com.tfg.domain.model.BotConfig
import com.tfg.domain.repository.SettingsRepository
import com.tfg.security.KeystoreManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val prefs: SharedPreferences,
    private val keystoreManager: KeystoreManager
) : SettingsRepository {

    override suspend fun isBotEnabled(): Boolean =
        prefs.getBoolean(Constants.PREF_BOT_ENABLED, false)

    override suspend fun setBotEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(Constants.PREF_BOT_ENABLED, enabled).apply()
    }

    override suspend fun isPaperTrading(): Boolean =
        prefs.getBoolean(Constants.PREF_PAPER_TRADING, false)

    override suspend fun setPaperTrading(enabled: Boolean) {
        prefs.edit().putBoolean(Constants.PREF_PAPER_TRADING, enabled).apply()
    }

    override suspend fun getDonationPercent(): Double =
        prefs.getFloat(Constants.PREF_DONATION_PERCENT, 10f).toDouble()

    override suspend fun setDonationPercent(percent: Double) {
        prefs.edit().putFloat(Constants.PREF_DONATION_PERCENT, percent.toFloat()).apply()
    }

    override suspend fun hasApiKey(): Boolean =
        keystoreManager.hasApiKey()

    override suspend fun isBiometricEnabled(): Boolean =
        prefs.getBoolean(Constants.PREF_BIOMETRIC_ENABLED, false)

    override suspend fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(Constants.PREF_BIOMETRIC_ENABLED, enabled).apply()
    }

    override suspend fun saveApiCredentials(apiKey: String, apiSecret: String) {
        keystoreManager.storeApiKey(apiKey)
        keystoreManager.storeApiSecret(apiSecret)
        prefs.edit()
            .putBoolean(Constants.PREF_API_KEY, true)
            .apply()
    }

    override suspend fun getApiKey(): String? {
        if (!keystoreManager.hasApiKey()) return null
        return keystoreManager.getApiKey()
    }

    override suspend fun getApiSecret(): String? {
        if (!keystoreManager.hasApiKey()) return null
        return keystoreManager.getApiSecret()
    }

    override fun getBotConfig(): Flow<BotConfig> = flow {
        emit(BotConfig(
            isEnabled = isBotEnabled(),
            paperTradingEnabled = isPaperTrading(),
            donationPercent = getDonationPercent(),
            donationEnabled = getDonationPercent() > 0,
            maxConcurrentOrders = 3,
            defaultOrderSizePercent = 5.0
        ))
    }

    override suspend fun updateBotConfig(config: BotConfig) {
        setBotEnabled(config.isEnabled)
        setPaperTrading(config.paperTradingEnabled)
        setDonationPercent(config.donationPercent)
    }

    override suspend fun setApiKey(apiKey: String, apiSecret: String): Result<Boolean> {
        return try {
            saveApiCredentials(apiKey, apiSecret)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun revokeApiKey(): Result<Boolean> {
        return try {
            keystoreManager.revokeApiKeys()
            prefs.edit().remove(Constants.PREF_API_KEY).apply()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun validateApiPermissions(): Result<Boolean> {
        return if (hasApiKey()) {
            Result.success(true)
        } else {
            Result.failure(Exception("No API key configured"))
        }
    }

    override fun getTheme(): Flow<String> = flowOf(
        prefs.getString(Constants.PREF_SELECTED_THEME, "dark") ?: "dark"
    )

    override suspend fun setTheme(theme: String) {
        prefs.edit().putString(Constants.PREF_SELECTED_THEME, theme).apply()
    }

    override fun getLanguage(): Flow<String> = flowOf("en")

    override suspend fun setLanguage(language: String) {
        // TODO: Implement language settings
    }

    override fun getActiveSessions(): Flow<List<String>> = flowOf(emptyList())
}
