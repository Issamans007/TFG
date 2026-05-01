package com.tfg.data.remote.api

import com.tfg.data.remote.dto.SymbolInfoDto
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Per-symbol Binance trading filters. Quantities and prices that don't match
 * these constraints are rejected exchange-side as `LOT_SIZE`, `PRICE_FILTER`,
 * or `MIN_NOTIONAL` filter failures. We fetch them once at startup (and on
 * cache miss) then round every order before placing it.
 */
data class SymbolFilters(
    val symbol: String,
    /** Increment for `quantity` (LOT_SIZE.stepSize for spot, MARKET_LOT_SIZE for futures market orders). */
    val stepSize: BigDecimal,
    /** Minimum quantity (LOT_SIZE.minQty). */
    val minQty: BigDecimal,
    /** Increment for `price` (PRICE_FILTER.tickSize). */
    val tickSize: BigDecimal,
    /** Minimum notional value (qty * price >= minNotional). */
    val minNotional: BigDecimal
) {
    /** Floor `qty` to the nearest valid step size. */
    fun roundQty(qty: Double): Double {
        if (stepSize <= BigDecimal.ZERO) return qty
        val q = BigDecimal.valueOf(qty)
        return q.divide(stepSize, 0, RoundingMode.DOWN).multiply(stepSize).toDouble()
    }

    /** Round `price` to the nearest valid tick size (toward zero). */
    fun roundPrice(price: Double): Double {
        if (tickSize <= BigDecimal.ZERO) return price
        val p = BigDecimal.valueOf(price)
        return p.divide(tickSize, 0, RoundingMode.DOWN).multiply(tickSize).toDouble()
    }

    /** True if `qty * price` clears the minNotional filter. */
    fun clearsMinNotional(qty: Double, price: Double): Boolean {
        if (minNotional <= BigDecimal.ZERO) return true
        return BigDecimal.valueOf(qty).multiply(BigDecimal.valueOf(price)) >= minNotional
    }
}

@Singleton
class ExchangeFiltersCache @Inject constructor(
    private val spotApi: BinanceApi,
    private val futuresApi: BinanceFuturesApi
) {
    private val spotCache = java.util.concurrent.ConcurrentHashMap<String, SymbolFilters>()
    private val futuresCache = java.util.concurrent.ConcurrentHashMap<String, SymbolFilters>()
    private val spotMutex = Mutex()
    private val futuresMutex = Mutex()
    @Volatile private var spotLoaded = false
    @Volatile private var futuresLoaded = false

    suspend fun getSpot(symbol: String): SymbolFilters? {
        if (!spotLoaded) loadSpot()
        return spotCache[symbol]
    }

    suspend fun getFutures(symbol: String): SymbolFilters? {
        if (!futuresLoaded) loadFutures()
        return futuresCache[symbol]
    }

    private suspend fun loadSpot() = spotMutex.withLock {
        if (spotLoaded) return@withLock
        try {
            val info = spotApi.getExchangeInfo()
            info.symbols.forEach { sym -> parse(sym, isFutures = false)?.let { spotCache[sym.symbol] = it } }
            spotLoaded = true
            Timber.d("Loaded spot filters for ${spotCache.size} symbols")
        } catch (e: Exception) {
            Timber.e(e, "Failed to load spot exchangeInfo")
        }
    }

    private suspend fun loadFutures() = futuresMutex.withLock {
        if (futuresLoaded) return@withLock
        try {
            val info = futuresApi.getFuturesExchangeInfo()
            info.symbols.forEach { sym -> parse(sym, isFutures = true)?.let { futuresCache[sym.symbol] = it } }
            futuresLoaded = true
            Timber.d("Loaded futures filters for ${futuresCache.size} symbols")
        } catch (e: Exception) {
            Timber.e(e, "Failed to load futures exchangeInfo")
        }
    }

    private fun parse(sym: SymbolInfoDto, isFutures: Boolean): SymbolFilters? {
        // Prefer MARKET_LOT_SIZE (futures market orders) when present, fall back to LOT_SIZE.
        val lot = sym.filters.firstOrNull { it.filterType == "MARKET_LOT_SIZE" }
            ?: sym.filters.firstOrNull { it.filterType == "LOT_SIZE" }
            ?: return null
        val priceF = sym.filters.firstOrNull { it.filterType == "PRICE_FILTER" } ?: return null
        val notionalF = sym.filters.firstOrNull {
            it.filterType == "MIN_NOTIONAL" || it.filterType == "NOTIONAL"
        }
        return SymbolFilters(
            symbol = sym.symbol,
            stepSize = lot.stepSize?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
            minQty = lot.minQty?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
            tickSize = priceF.tickSize?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
            minNotional = notionalF?.minNotional?.toBigDecimalOrNull() ?: BigDecimal.ZERO
        )
    }
}
