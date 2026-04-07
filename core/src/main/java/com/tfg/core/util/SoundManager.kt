package com.tfg.core.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

/**
 * Configurable sound alert system for trading events.
 * Uses SoundPool for low-latency playback.
 * Sounds are generated via Android's ToneGenerator-style approach (no asset files needed).
 */
object SoundManager {

    private const val PREFS_NAME = "tfg_sounds"
    private const val KEY_ENABLED = "sound_enabled"
    private const val KEY_ORDER_FILL = "sound_order_fill"
    private const val KEY_SL_HIT = "sound_sl_hit"
    private const val KEY_TP_HIT = "sound_tp_hit"
    private const val KEY_ALERT = "sound_alert"

    private var soundPool: SoundPool? = null
    private var initialized = false

    enum class SoundEvent(val prefKey: String, val displayName: String) {
        ORDER_FILL(KEY_ORDER_FILL, "Order Filled"),
        STOP_LOSS(KEY_SL_HIT, "Stop Loss Hit"),
        TAKE_PROFIT(KEY_TP_HIT, "Take Profit Hit"),
        ALERT(KEY_ALERT, "Alert / Notification")
    }

    enum class SoundType(val displayName: String) {
        NONE("None"),
        CHIME("Chime"),
        BEEP("Beep"),
        DING("Ding"),
        ALERT_TONE("Alert Tone"),
        SUCCESS("Success"),
        ERROR("Error")
    }

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun getSoundForEvent(context: Context, event: SoundEvent): SoundType {
        val name = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(event.prefKey, getDefaultForEvent(event).name) ?: getDefaultForEvent(event).name
        return SoundType.entries.find { it.name == name } ?: getDefaultForEvent(event)
    }

    fun setSoundForEvent(context: Context, event: SoundEvent, type: SoundType) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(event.prefKey, type.name).apply()
    }

    private fun getDefaultForEvent(event: SoundEvent): SoundType = when (event) {
        SoundEvent.ORDER_FILL -> SoundType.CHIME
        SoundEvent.STOP_LOSS -> SoundType.ERROR
        SoundEvent.TAKE_PROFIT -> SoundType.SUCCESS
        SoundEvent.ALERT -> SoundType.ALERT_TONE
    }

    /** Play the configured sound for a given event */
    fun play(context: Context, event: SoundEvent) {
        if (!isEnabled(context)) return
        val type = getSoundForEvent(context, event)
        if (type == SoundType.NONE) return
        playToneForType(context, type)
    }

    /** Plays using Android ToneGenerator for zero-asset implementation */
    private fun playToneForType(context: Context, type: SoundType) {
        try {
            val toneType = when (type) {
                SoundType.CHIME -> android.media.ToneGenerator.TONE_PROP_ACK
                SoundType.BEEP -> android.media.ToneGenerator.TONE_PROP_BEEP
                SoundType.DING -> android.media.ToneGenerator.TONE_PROP_BEEP2
                SoundType.ALERT_TONE -> android.media.ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD
                SoundType.SUCCESS -> android.media.ToneGenerator.TONE_PROP_ACK
                SoundType.ERROR -> android.media.ToneGenerator.TONE_CDMA_ABBR_ALERT
                SoundType.NONE -> return
            }
            val duration = when (type) {
                SoundType.CHIME, SoundType.SUCCESS -> 200
                SoundType.BEEP, SoundType.DING -> 150
                SoundType.ALERT_TONE -> 400
                SoundType.ERROR -> 300
                SoundType.NONE -> 0
            }
            val toneGen = android.media.ToneGenerator(
                android.media.AudioManager.STREAM_NOTIFICATION, 80
            )
            toneGen.startTone(toneType, duration)
            // Release after playing
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try { toneGen.release() } catch (_: Exception) { }
            }, (duration + 100).toLong())
        } catch (_: Exception) { }
    }
}
