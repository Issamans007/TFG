package com.tfg.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.tfg.R
import com.tfg.MainActivity
import java.text.SimpleDateFormat
import java.util.*

class TradingWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val PREFS_NAME = "tfg_widget"
        private const val KEY_PNL = "widget_pnl"
        private const val KEY_PNL_PCT = "widget_pnl_pct"
        private const val KEY_BOT_ACTIVE = "widget_bot_active"
        private const val KEY_STRATEGY = "widget_strategy"
        private const val KEY_POSITIONS = "widget_positions"

        /** Called from inside the app to push fresh data to the widget */
        fun updateWidgetData(
            context: Context,
            pnl: Double,
            pnlPercent: Double,
            botActive: Boolean,
            strategyName: String,
            openPositions: Int
        ) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putFloat(KEY_PNL, pnl.toFloat())
                .putFloat(KEY_PNL_PCT, pnlPercent.toFloat())
                .putBoolean(KEY_BOT_ACTIVE, botActive)
                .putString(KEY_STRATEGY, strategyName)
                .putInt(KEY_POSITIONS, openPositions)
                .apply()

            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, TradingWidgetProvider::class.java))
            if (ids.isNotEmpty()) {
                val intent = Intent(context, TradingWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                context.sendBroadcast(intent)
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val pnl = prefs.getFloat(KEY_PNL, 0f).toDouble()
        val pnlPct = prefs.getFloat(KEY_PNL_PCT, 0f).toDouble()
        val botActive = prefs.getBoolean(KEY_BOT_ACTIVE, false)
        val strategy = prefs.getString(KEY_STRATEGY, "None") ?: "None"
        val positions = prefs.getInt(KEY_POSITIONS, 0)

        val views = RemoteViews(context.packageName, R.layout.widget_trading)

        // Bot status
        views.setTextViewText(R.id.widget_bot_status, if (botActive) "BOT ACTIVE" else "BOT OFF")
        views.setTextColor(R.id.widget_bot_status, if (botActive) 0xFF00C853.toInt() else 0xFFEF5350.toInt())

        // PnL
        val sign = if (pnl >= 0) "+" else ""
        val pnlText = "${sign}$${String.format("%.2f", pnl)} (${sign}${String.format("%.2f", pnlPct)}%)"
        views.setTextViewText(R.id.widget_pnl_value, pnlText)
        views.setTextColor(R.id.widget_pnl_value, if (pnl >= 0) 0xFF00C853.toInt() else 0xFFEF5350.toInt())

        // Strategy & positions
        views.setTextViewText(R.id.widget_strategy, strategy)
        views.setTextViewText(R.id.widget_positions, positions.toString())

        // Updated time
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        views.setTextViewText(R.id.widget_updated, "Updated: $time")

        // Click → open app
        val openIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

        appWidgetManager.updateAppWidget(widgetId, views)
    }
}
