package com.tfg.data.remote.repository

import android.content.SharedPreferences
import com.tfg.core.util.Constants
import com.tfg.domain.model.BotConfig
import com.tfg.domain.model.MarginType
import com.tfg.domain.repository.SettingsRepository
import com.tfg.security.KeystoreManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val prefs: SharedPreferences,
    private val keystoreManager: KeystoreManager
) : SettingsRepository {

    // Reactive theme flow — emits immediately and whenever setTheme() is called
    private val _themeFlow = MutableStateFlow(
        prefs.getString(Constants.PREF_SELECTED_THEME, "dark") ?: "dark"
    )

    // Reactive language flow. The UI layer (which has AppCompat available)
    // observes this and calls AppCompatDelegate.setApplicationLocales(...);
    // this repository merely persists the chosen tag so the choice survives
    // restarts and is single-source-of-truth.
    private val _languageFlow = MutableStateFlow(
        prefs.getString(PREF_LANGUAGE, "en") ?: "en"
    )

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

    // ─── Futures settings ──────────────────────────────────────────
    override suspend fun isFuturesEnabled(): Boolean =
        prefs.getBoolean("pref_futures_enabled", false)

    override suspend fun setFuturesEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("pref_futures_enabled", enabled).apply()
    }

    override suspend fun getDefaultLeverage(): Int =
        prefs.getInt("pref_default_leverage", 1).coerceAtLeast(1)

    override suspend fun setDefaultLeverage(leverage: Int) {
        prefs.edit().putInt("pref_default_leverage", leverage.coerceIn(1, 125)).apply()
    }

    override suspend fun getDefaultMarginType(): MarginType {
        val v = prefs.getString("pref_default_margin_type", MarginType.ISOLATED.name)
            ?: MarginType.ISOLATED.name
        return runCatching { MarginType.valueOf(v) }.getOrDefault(MarginType.ISOLATED)
    }

    override suspend fun setDefaultMarginType(type: MarginType) {
        prefs.edit().putString("pref_default_margin_type", type.name).apply()
    }

    override suspend fun getMaxLeverage(): Int =
        prefs.getInt("pref_max_leverage", 20).coerceAtLeast(1)

    override suspend fun setMaxLeverage(leverage: Int) {
        prefs.edit().putInt("pref_max_leverage", leverage.coerceIn(1, 125)).apply()
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

    override fun getTheme(): Flow<String> = _themeFlow.asStateFlow()

    override suspend fun setTheme(theme: String) {
        prefs.edit().putString(Constants.PREF_SELECTED_THEME, theme).apply()
        _themeFlow.value = theme
    }

    override fun getLanguage(): Flow<String> = _languageFlow.asStateFlow()

    override suspend fun setLanguage(language: String) {
        // Whitelist BCP-47-ish tags so we don't poison prefs with arbitrary
        // input. Empty string → fall back to system default ("").
        val tag = language.trim()
        if (tag.isNotEmpty() && !LANGUAGE_TAG_REGEX.matches(tag)) return
        prefs.edit().putString(PREF_LANGUAGE, tag).apply()
        _languageFlow.value = tag
    }

    override fun getActiveSessions(): Flow<List<String>> = flowOf(emptyList())

    private companion object {
        const val PREF_LANGUAGE = "pref_language"
        // BCP-47 subset: 2–3 letter primary tag, optional region/script.
        val LANGUAGE_TAG_REGEX = Regex("^[A-Za-z]{2,3}(-[A-Za-z0-9]{2,8})*$")
    }
}
