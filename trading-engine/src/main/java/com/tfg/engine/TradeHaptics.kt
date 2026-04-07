package com.tfg.engine

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Trade-event haptic feedback, usable from trading-engine module.
 * Reads enabled/disabled from SharedPreferences.
 */
internal object TradeHaptics {

    private const val PREFS_NAME = "tfg_haptics"
    private const val KEY_ENABLED = "haptic_enabled"

    private fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, true)

    fun orderFilled(context: Context) {
        if (!isEnabled(context)) return
        vibrate(context, longArrayOf(0, 50, 80, 50))
    }

    fun stopLossHit(context: Context) {
        if (!isEnabled(context)) return
        vibrate(context, longArrayOf(0, 200))
    }

    fun takeProfitHit(context: Context) {
        if (!isEnabled(context)) return
        vibrate(context, longArrayOf(0, 40, 60, 40, 60, 40))
    }

    fun emergencyClose(context: Context) {
        if (!isEnabled(context)) return
        vibrate(context, longArrayOf(0, 300, 100, 300))
    }

    private fun vibrate(context: Context, pattern: LongArray) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val amps = IntArray(pattern.size) { if (it == 0) 0 else VibrationEffect.DEFAULT_AMPLITUDE }
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, amps, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        } catch (_: Exception) { }
    }
}
