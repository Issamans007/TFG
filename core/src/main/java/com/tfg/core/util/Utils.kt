package com.tfg.core.util

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

object Formatters {
    private val priceHigh = DecimalFormat("#,##0.00")
    private val priceLow = DecimalFormat("0.00000000")
    private val priceMid = DecimalFormat("0.0000")
    private val percentFormat = DecimalFormat("+0.00;-0.00")
    private val volumeFormat = DecimalFormat("#,##0")
    private val dateFormat = ThreadLocal.withInitial { SimpleDateFormat("dd MMM yyyy", Locale.US) }
    private val timeFormat = ThreadLocal.withInitial { SimpleDateFormat("HH:mm:ss", Locale.US) }
    private val dateTimeFormat = ThreadLocal.withInitial { SimpleDateFormat("dd MMM yyyy HH:mm", Locale.US) }

    fun formatPrice(price: Double): String = when {
        price >= 1.0 -> priceHigh.format(price)
        price >= 0.01 -> priceMid.format(price)
        else -> priceLow.format(price)
    }

    fun formatPercent(value: Double): String = "${percentFormat.format(value)}%"

    fun formatVolume(volume: Double): String = when {
        volume >= 1_000_000_000 -> "${priceHigh.format(volume / 1_000_000_000)}B"
        volume >= 1_000_000 -> "${priceHigh.format(volume / 1_000_000)}M"
        volume >= 1_000 -> "${priceHigh.format(volume / 1_000)}K"
        else -> volumeFormat.format(volume)
    }

    // ThreadLocal.get() can return null if .withInitial's initializer ever
    // throws or after a thread is detached. Fall back to a fresh formatter
    // rather than NPE'ing the whole UI render.
    fun formatDate(timestamp: Long): String =
        (dateFormat.get() ?: SimpleDateFormat("dd MMM yyyy", Locale.US)).format(Date(timestamp))
    fun formatTime(timestamp: Long): String =
        (timeFormat.get() ?: SimpleDateFormat("HH:mm:ss", Locale.US)).format(Date(timestamp))
    fun formatDateTime(timestamp: Long): String =
        (dateTimeFormat.get() ?: SimpleDateFormat("dd MMM yyyy HH:mm", Locale.US)).format(Date(timestamp))

    fun formatUsdt(amount: Double): String = "$${priceHigh.format(amount)}"
    fun formatBtc(amount: Double): String = "${priceLow.format(amount)} BTC"
}

object Constants {
    const val BINANCE_BASE_URL = "https://api.binance.com/"
    const val BINANCE_WS_URL = "wss://stream.binance.com:9443/ws/"
    const val BINANCE_TESTNET_URL = "https://testnet.binance.vision/"
    const val BINANCE_TESTNET_WS = "wss://testnet.binance.vision/ws/"

    const val PREF_API_KEY = "pref_api_key_enc"
    const val PREF_API_SECRET = "pref_api_secret_enc"
    const val PREF_TRADING_PIN = "pref_trading_pin_enc"
    const val PREF_BIOMETRIC_ENABLED = "pref_biometric_enabled"
    const val PREF_BOT_ENABLED = "pref_bot_enabled"
    const val PREF_PAPER_TRADING = "pref_paper_trading"
    const val PREF_SELECTED_THEME = "pref_theme"
    const val PREF_DONATION_PERCENT = "pref_donation_percent"

    const val DB_NAME = "tfg_database"

    const val WS_RECONNECT_BASE_DELAY = 1000L
    const val WS_RECONNECT_MAX_DELAY = 30000L
    const val WS_MAX_RETRIES = 10
    const val HEARTBEAT_INTERVAL = 60000L

    const val MAX_PIN_ATTEMPTS = 5
    const val PIN_LOCKOUT_DURATION = 300000L // 5 minutes
}
