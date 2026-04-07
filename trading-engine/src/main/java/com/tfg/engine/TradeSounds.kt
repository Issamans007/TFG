package com.tfg.engine

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper

/**
 * Sound alerts for trade events, usable from trading-engine module.
 * Reads enabled/disabled + per-event config from SharedPreferences.
 */
internal object TradeSounds {

    private const val PREFS_NAME = "tfg_sounds"
    private const val KEY_ENABLED = "sound_enabled"
    private const val KEY_ORDER_FILL = "sound_order_fill"
    private const val KEY_SL_HIT = "sound_sl_hit"
    private const val KEY_TP_HIT = "sound_tp_hit"

    private fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, true)

    fun orderFilled(context: Context) {
        if (!isEnabled(context)) return
        val type = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ORDER_FILL, "CHIME") ?: "CHIME"
        playTone(type)
    }

    fun stopLossHit(context: Context) {
        if (!isEnabled(context)) return
        val type = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SL_HIT, "ERROR") ?: "ERROR"
        playTone(type)
    }

    fun takeProfitHit(context: Context) {
        if (!isEnabled(context)) return
        val type = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_TP_HIT, "SUCCESS") ?: "SUCCESS"
        playTone(type)
    }

    private fun playTone(type: String) {
        if (type == "NONE") return
        try {
            val toneType = when (type) {
                "CHIME", "SUCCESS" -> ToneGenerator.TONE_PROP_ACK
                "BEEP" -> ToneGenerator.TONE_PROP_BEEP
                "DING" -> ToneGenerator.TONE_PROP_BEEP2
                "ALERT_TONE" -> ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD
                "ERROR" -> ToneGenerator.TONE_CDMA_ABBR_ALERT
                else -> ToneGenerator.TONE_PROP_ACK
            }
            val duration = when (type) {
                "CHIME", "SUCCESS" -> 200
                "BEEP", "DING" -> 150
                "ALERT_TONE" -> 400
                "ERROR" -> 300
                else -> 200
            }
            val gen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
            gen.startTone(toneType, duration)
            Handler(Looper.getMainLooper()).postDelayed({
                try { gen.release() } catch (_: Exception) { }
            }, (duration + 100).toLong())
        } catch (_: Exception) { }
    }
}
