package com.tfg.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences("tfg_settings", Context.MODE_PRIVATE)
            val botEnabled = prefs.getBoolean("pref_bot_enabled", false)
            if (botEnabled) {
                TradingForegroundService.start(context)
            }
        }
    }
}
