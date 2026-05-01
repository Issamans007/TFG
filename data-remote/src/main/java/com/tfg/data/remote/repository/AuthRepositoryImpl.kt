package com.tfg.data.remote.repository

import android.content.SharedPreferences
import com.tfg.data.remote.api.TfgServerApi
import com.tfg.data.remote.dto.*
import com.tfg.domain.model.KycStatus
import com.tfg.domain.model.User
import com.tfg.domain.repository.AuthRepository
import com.tfg.security.KeystoreManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val tfgServerApi: TfgServerApi,
    private val prefs: SharedPreferences,
    private val keystoreManager: KeystoreManager
) : AuthRepository {

    private val currentUserFlow = MutableStateFlow<User?>(loadCachedUser())
    private val kycStatusFlow = MutableStateFlow(loadKycStatus())

    override suspend fun login(email: String, password: String): Result<User> = runCatching {
        val response = tfgServerApi.login(LoginRequestDto(email, password))
        val user = User(
            id = response.user.id,
            email = response.user.email,
            displayName = response.user.displayName,
            kycStatus = try { KycStatus.valueOf(response.user.kycStatus) } catch (_: Exception) { KycStatus.PENDING }
        )
        saveAuthState(response.token, user)
        currentUserFlow.value = user
        user
    }

    override suspend fun register(email: String, password: String, displayName: String): Result<User> = runCatching {
        val response = tfgServerApi.register(RegisterRequestDto(email, password, displayName))
        val user = User(
            id = response.user.id,
            email = response.user.email,
            displayName = response.user.displayName,
            kycStatus = try { KycStatus.valueOf(response.user.kycStatus) } catch (_: Exception) { KycStatus.PENDING }
        )
        saveAuthState(response.token, user)
        currentUserFlow.value = user
        user
    }

    override suspend fun verifyOtp(email: String, otp: String): Result<Boolean> = runCatching {
        val response = tfgServerApi.verifyOtp(OtpVerifyDto(email, otp))
        response.success
    }

    override suspend fun sendOtp(email: String): Result<Boolean> = runCatching {
        val response = tfgServerApi.sendOtp(OtpRequestDto(email))
        response.success
    }

    override suspend fun submitKyc(documentUrl: String): Result<Boolean> = runCatching {
        val response = tfgServerApi.submitKyc(KycRequestDto(documentUrl))
        val newStatus = try { KycStatus.valueOf(response.status) } catch (_: Exception) { KycStatus.SUBMITTED }
        kycStatusFlow.value = newStatus
        prefs.edit().putString(KEY_KYC_STATUS, newStatus.name).apply()
        true
    }

    override fun getKycStatus(): Flow<KycStatus> = kycStatusFlow

    override suspend fun setupTradingPin(pin: String): Result<Boolean> = runCatching {
        keystoreManager.storePin(pin)
        prefs.edit().putBoolean(KEY_PIN_SET, true).apply()
        currentUserFlow.value = currentUserFlow.value?.copy(tradingPinSet = true)
        true
    }

    override suspend fun verifyTradingPin(pin: String): Result<Boolean> = runCatching {
        // Brute-force protection: lock out for an exponentially-growing window
        // after repeated wrong PIN attempts. State is persisted so a process
        // restart cannot reset the counter.
        val now = System.currentTimeMillis()
        val lockedUntil = prefs.getLong(KEY_PIN_LOCKED_UNTIL, 0L)
        if (lockedUntil > now) {
            val remainingSec = (lockedUntil - now) / 1000
            throw IllegalStateException("Too many wrong PIN attempts. Try again in ${remainingSec}s.")
        }
        val stored = keystoreManager.getPin()
        if (stored.isNotEmpty() && stored == pin) {
            // Reset failure counter on success.
            prefs.edit()
                .putInt(KEY_PIN_FAILS, 0)
                .putLong(KEY_PIN_LOCKED_UNTIL, 0L)
                .apply()
            true
        } else {
            val fails = prefs.getInt(KEY_PIN_FAILS, 0) + 1
            // 1–2 wrong: no lockout. 3–4: 30s. 5–6: 5 min. 7+: 1 hour.
            val lockoutMs: Long = when {
                fails >= 7 -> 60L * 60_000L
                fails >= 5 -> 5L * 60_000L
                fails >= 3 -> 30_000L
                else -> 0L
            }
            prefs.edit()
                .putInt(KEY_PIN_FAILS, fails)
                .putLong(KEY_PIN_LOCKED_UNTIL, if (lockoutMs > 0) now + lockoutMs else 0L)
                .apply()
            throw IllegalArgumentException(
                if (lockoutMs > 0) "Invalid PIN. Locked for ${lockoutMs / 1000}s after $fails wrong attempts."
                else "Invalid PIN ($fails wrong attempts)."
            )
        }
    }

    override suspend fun enableBiometric(): Result<Boolean> = runCatching {
        prefs.edit().putBoolean(KEY_BIOMETRIC, true).apply()
        currentUserFlow.value = currentUserFlow.value?.copy(biometricEnabled = true)
        true
    }

    override suspend fun logout() {
        keystoreManager.clearAuthToken()
        prefs.edit()
            .remove(KEY_USER_ID)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_USER_NAME)
            .remove(KEY_LOGGED_IN)
            .apply()
        currentUserFlow.value = null
    }

    override fun getCurrentUser(): Flow<User?> = currentUserFlow

    override fun isLoggedIn(): Flow<Boolean> = currentUserFlow.map { it != null }

    private fun saveAuthState(token: String, user: User) {
        // Token is bearer credentials — encrypt via Keystore rather than store
        // it as plaintext in SharedPreferences (readable on rooted devices /
        // by adb backup on debug builds).
        keystoreManager.storeAuthToken(token)
        prefs.edit()
            .putString(KEY_USER_ID, user.id)
            .putString(KEY_USER_EMAIL, user.email)
            .putString(KEY_USER_NAME, user.displayName)
            .putBoolean(KEY_LOGGED_IN, true)
            .apply()
    }

    private fun loadCachedUser(): User? {
        if (!prefs.getBoolean(KEY_LOGGED_IN, false)) return null
        val id = prefs.getString(KEY_USER_ID, null) ?: return null
        return User(
            id = id,
            email = prefs.getString(KEY_USER_EMAIL, "") ?: "",
            displayName = prefs.getString(KEY_USER_NAME, "") ?: "",
            kycStatus = loadKycStatus(),
            biometricEnabled = prefs.getBoolean(KEY_BIOMETRIC, false),
            tradingPinSet = prefs.getBoolean(KEY_PIN_SET, false)
        )
    }

    private fun loadKycStatus(): KycStatus {
        val status = prefs.getString(KEY_KYC_STATUS, null) ?: return KycStatus.PENDING
        return try { KycStatus.valueOf(status) } catch (_: Exception) { KycStatus.PENDING }
    }

    companion object {
        private const val KEY_TOKEN = "auth_token" // legacy — cleared on first launch after upgrade
        private const val KEY_USER_ID = "auth_user_id"
        private const val KEY_USER_EMAIL = "auth_user_email"
        private const val KEY_USER_NAME = "auth_user_name"
        private const val KEY_LOGGED_IN = "auth_logged_in"
        private const val KEY_KYC_STATUS = "auth_kyc_status"
        private const val KEY_BIOMETRIC = "auth_biometric"
        private const val KEY_PIN_SET = "auth_pin_set"
        private const val KEY_PIN_FAILS = "auth_pin_fails"
        private const val KEY_PIN_LOCKED_UNTIL = "auth_pin_locked_until"
    }
}
