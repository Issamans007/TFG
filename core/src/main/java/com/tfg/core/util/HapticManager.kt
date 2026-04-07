package com.tfg.core.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Centralized haptic feedback manager for trading events.
 * Provides distinct vibration patterns for different trade outcomes.
 */
object HapticManager {

    private const val PREFS_NAME = "tfg_haptics"
    private const val KEY_ENABLED = "haptic_enabled"

    fun isEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, true)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    /** Short double-tap — order filled successfully */
    fun orderFilled(context: Context) {
        if (!isEnabled(context)) return
        vibrate(context, longArrayOf(0, 50, 80, 50), -1)
    }

    /** Heavy single pulse — stop loss hit */
    fun stopLossHit(context: Context) {
        if (!isEnabled(context)) return
        vibrate(context, longArrayOf(0, 200), -1)
    }

    /** Triple light tap — take profit hit */
    fun takeProfitHit(context: Context) {
        if (!isEnabled(context)) return
        vibrate(context, longArrayOf(0, 40, 60, 40, 60, 40), -1)
    }

    /** Long buzz — emergency close / kill switch */
    fun emergencyAlert(context: Context) {
        if (!isEnabled(context)) return
        vibrate(context, longArrayOf(0, 300, 100, 300), -1)
    }

    /** Single light tick — button feedback */
    fun tick(context: Context) {
        if (!isEnabled(context)) return
        vibrate(context, longArrayOf(0, 20), -1)
    }

    private fun vibrate(context: Context, pattern: LongArray, repeat: Int) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val mgr = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                mgr.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val amplitudes = IntArray(pattern.size) { if (it == 0) 0 else VibrationEffect.DEFAULT_AMPLITUDE }
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, repeat))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, repeat)
            }
        } catch (_: Exception) { }
    }
}
