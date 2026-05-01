package com.tfg.security

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeystoreManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    companion object {
        private const val API_KEY_ALIAS = "tfg_api_key"
        private const val API_SECRET_ALIAS = "tfg_api_secret"
        private const val PIN_ALIAS = "tfg_pin"
        private const val AUTH_TOKEN_ALIAS = "tfg_auth_token"
        private const val PREFS_NAME = "tfg_keystore_prefs"
        private const val GCM_TAG_LENGTH = 128
    }

    fun storeApiKey(apiKey: String) = encrypt(API_KEY_ALIAS, apiKey)
    fun getApiKey(): String = decrypt(API_KEY_ALIAS)
    fun storeApiSecret(apiSecret: String) = encrypt(API_SECRET_ALIAS, apiSecret)
    fun getApiSecret(): String = decrypt(API_SECRET_ALIAS)
    fun storePin(pin: String) = encrypt(PIN_ALIAS, pin)
    fun getPin(): String = decrypt(PIN_ALIAS)
    fun storeAuthToken(token: String) = encrypt(AUTH_TOKEN_ALIAS, token)
    fun getAuthToken(): String = decrypt(AUTH_TOKEN_ALIAS)
    fun clearAuthToken() {
        runCatching { keyStore.deleteEntry(AUTH_TOKEN_ALIAS) }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .remove("${AUTH_TOKEN_ALIAS}_iv").remove("${AUTH_TOKEN_ALIAS}_data").apply()
    }
    fun hasApiKey(): Boolean = keyStore.containsAlias(API_KEY_ALIAS)

    fun revokeApiKeys() {
        keyStore.deleteEntry(API_KEY_ALIAS)
        keyStore.deleteEntry(API_SECRET_ALIAS)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove("${API_KEY_ALIAS}_iv").remove("${API_KEY_ALIAS}_data")
            .remove("${API_SECRET_ALIAS}_iv").remove("${API_SECRET_ALIAS}_data").apply()
    }

    private fun encrypt(alias: String, data: String) {
        val key = getOrCreateKey(alias)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val encrypted = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
        val iv = cipher.iv
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString("${alias}_iv", android.util.Base64.encodeToString(iv, android.util.Base64.NO_WRAP))
            .putString("${alias}_data", android.util.Base64.encodeToString(encrypted, android.util.Base64.NO_WRAP))
            .apply()
    }

    private fun decrypt(alias: String): String {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val ivStr = prefs.getString("${alias}_iv", null) ?: return ""
            val dataStr = prefs.getString("${alias}_data", null) ?: return ""
            val iv = android.util.Base64.decode(ivStr, android.util.Base64.NO_WRAP)
            val data = android.util.Base64.decode(dataStr, android.util.Base64.NO_WRAP)
            val key = keyStore.getKey(alias, null) as? SecretKey ?: return ""
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
            String(cipher.doFinal(data), Charsets.UTF_8)
        } catch (e: Exception) {
            Timber.e(e, "Decryption failed for $alias")
            ""
        }
    }

    private fun getOrCreateKey(alias: String): SecretKey {
        val existingKey = keyStore.getKey(alias, null) as? SecretKey
        if (existingKey != null) return existingKey

        val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        keyGen.init(
            KeyGenParameterSpec.Builder(alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(false)
                .build()
        )
        return keyGen.generateKey()
    }
}

@Singleton
class SecurityChecker @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun isRooted(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su",
            "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su",
            "/system/sd/xbin/su", "/system/bin/failsafe/su", "/data/local/su",
            "/su/bin/su"
        )
        return paths.any { File(it).exists() } || canExecuteSu()
    }

    fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.BOARD == "QC_Reference_Phone"
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.HOST.startsWith("Build")
                || Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")
                || "google_sdk" == Build.PRODUCT)
    }

    fun isDebuggable(): Boolean =
        context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0

    fun isDebuggerAttached(): Boolean = android.os.Debug.isDebuggerConnected()

    fun hasHookFramework(): Boolean {
        val packages = arrayOf(
            "de.robv.android.xposed", "com.saurik.substrate",
            "de.robv.android.xposed.installer", "com.topjohnwu.magisk"
        )
        return packages.any { pkg ->
            try {
                context.packageManager.getPackageInfo(pkg, 0)
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    fun performAllChecks(): SecurityReport = SecurityReport(
        isRooted = isRooted(),
        isEmulator = isEmulator(),
        isDebuggable = isDebuggable(),
        isDebuggerAttached = isDebuggerAttached(),
        hasHookFramework = hasHookFramework()
    )

    private fun canExecuteSu(): Boolean = try {
        Runtime.getRuntime().exec("which su").inputStream.bufferedReader().readLine() != null
    } catch (e: Exception) { false }
}

data class SecurityReport(
    val isRooted: Boolean,
    val isEmulator: Boolean,
    val isDebuggable: Boolean,
    val isDebuggerAttached: Boolean,
    val hasHookFramework: Boolean
) {
    val isSecure: Boolean get() = !isRooted && !isEmulator && !isDebuggable && !isDebuggerAttached && !hasHookFramework
}

@Singleton
class SignalVerifier @Inject constructor() {
    fun verifyHmac(data: String, signature: String, secret: String): Boolean {
        return try {
            val mac = javax.crypto.Mac.getInstance("HmacSHA256")
            mac.init(javax.crypto.spec.SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
            val expected = mac.doFinal(data.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
            expected == signature
        } catch (e: Exception) {
            Timber.e(e, "HMAC verification failed")
            false
        }
    }
}

object ScreenshotPrevention {
    // Screenshots and screen recording are enabled.
    @Suppress("UNUSED_PARAMETER")
    fun apply(window: android.view.Window) = Unit
}
