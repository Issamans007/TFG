package com.tfg.data.remote.repository

import com.tfg.data.local.dao.*
import com.tfg.data.local.entity.*
import com.tfg.data.local.mapper.EntityMapper.toDomain
import com.tfg.data.remote.api.BinanceApi
import com.tfg.data.remote.api.TfgServerApi
import com.tfg.data.remote.interceptor.BinanceSigner
import com.tfg.domain.model.*
import com.tfg.domain.repository.MarketRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MarketRepositoryImpl @Inject constructor(
    private val binanceApi: BinanceApi,
    private val tfgServerApi: TfgServerApi,
    private val tradingPairDao: TradingPairDao,
    private val candleDao: CandleDao
) : MarketRepository {

    override fun getTradingPairs(): Flow<List<TradingPair>> =
        tradingPairDao.getAll().map { list -> list.map { it.toDomain() } }

    override fun getWatchlist(): Flow<List<TradingPair>> =
        tradingPairDao.getWatchlist().map { list -> list.map { it.toDomain() } }

    override fun getTicker(symbol: String): Flow<Ticker> = flow {
        val dto = binanceApi.get24hrTicker(symbol)
        emit(Ticker(
            symbol = dto.symbol,
            price = dto.lastPrice.toDouble(),
            volume = dto.volume.toDouble(),
            priceChangePercent = dto.priceChangePercent.toDouble()
        ))
    }

    override fun getAllTickers(): Flow<List<Ticker>> = flow {
        val tickers = binanceApi.get24hrTickers().map { dto ->
            Ticker(
                symbol = dto.symbol,
                price = dto.lastPrice.toDouble(),
                volume = dto.volume.toDouble(),
                priceChangePercent = dto.priceChangePercent.toDouble()
            )
        }
        emit(tickers)
    }

    override fun getCandles(symbol: String, interval: String, limit: Int): Flow<List<Candle>> = flow {
        val klines = binanceApi.getKlines(symbol, interval, limit)
        val candles = klines.map { k ->
            Candle(
                symbol = symbol, interval = interval,
                openTime = (k[0] as Number).toLong(),
                open = k[1].toString().toDouble(),
                high = k[2].toString().toDouble(),
                low = k[3].toString().toDouble(),
                close = k[4].toString().toDouble(),
                volume = k[5].toString().toDouble(),
                closeTime = (k[6] as Number).toLong(),
                quoteVolume = k[7].toString().toDouble(),
                numberOfTrades = (k[8] as Number).toInt()
            )
        }
        candleDao.deleteForPair(symbol, interval)
        candleDao.insertAll(candles.map { c ->
            CandleEntity(
                symbol = c.symbol, interval = c.interval, openTime = c.openTime,
                open = c.open, high = c.high, low = c.low, close = c.close,
                volume = c.volume, closeTime = c.closeTime,
                quoteVolume = c.quoteVolume, numberOfTrades = c.numberOfTrades
            )
        })
        emit(candles)
    }

    override fun getCandlesFromDb(symbol: String, interval: String): Flow<List<Candle>> =
        candleDao.getAllCandles(symbol, interval).map { entities -> entities.map { it.toDomain() } }

    override fun getMarketData(): Flow<MarketData> = flow {
        try {
            val fearGreed = tfgServerApi.getFearGreedIndex()
            val btcDom = tfgServerApi.getBtcDominance()
            val btcPrice = binanceApi.getPriceTicker("BTCUSDT")
            emit(MarketData(
                fearGreedIndex = fearGreed.value,
                fearGreedLabel = fearGreed.classification,
                btcDominance = btcDom.dominance,
                totalMarketCap = btcDom.marketCap,
                btcPrice = btcPrice.price.toDouble()
            ))
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch market data")
            emit(MarketData())
        }
    }

    override suspend fun addToWatchlist(symbol: String) =
        tradingPairDao.setWatchlisted(symbol, true)

    override suspend fun removeFromWatchlist(symbol: String) =
        tradingPairDao.setWatchlisted(symbol, false)

    override suspend fun searchPairs(query: String): List<TradingPair> =
        tradingPairDao.search(query).map { it.toDomain() }

    override suspend fun refreshPairs() {
        try {
            val info = binanceApi.getExchangeInfo()
            val tickers = binanceApi.get24hrTickers().associateBy { it.symbol }

            // Preserve watchlist state
            val watchlisted = mutableSetOf<String>()
            tradingPairDao.getWatchlistSymbols().forEach { watchlisted.add(it) }

            val entities = info.symbols
                .filter { it.status == "TRADING" && it.quoteAsset == "USDT" }
                .map { sym ->
                    val ticker = tickers[sym.symbol]
                    val lotSize = sym.filters.find { it.filterType == "LOT_SIZE" }
                    val priceFilter = sym.filters.find { it.filterType == "PRICE_FILTER" }
                    val notional = sym.filters.find { it.filterType == "NOTIONAL" }
                        ?: sym.filters.find { it.filterType == "MIN_NOTIONAL" }
                    TradingPairEntity(
                        symbol = sym.symbol,
                        baseAsset = sym.baseAsset,
                        quoteAsset = sym.quoteAsset,
                        lastPrice = ticker?.lastPrice?.toDoubleOrNull() ?: 0.0,
                        priceChangePercent24h = ticker?.priceChangePercent?.toDoubleOrNull() ?: 0.0,
                        volume24h = ticker?.quoteVolume?.toDoubleOrNull() ?: 0.0,
                        high24h = ticker?.highPrice?.toDoubleOrNull() ?: 0.0,
                        low24h = ticker?.lowPrice?.toDoubleOrNull() ?: 0.0,
                        isWatchlisted = watchlisted.contains(sym.symbol),
                        minQty = lotSize?.minQty?.toDoubleOrNull() ?: 0.0,
                        stepSize = lotSize?.stepSize?.toDoubleOrNull() ?: 0.0,
                        tickSize = priceFilter?.tickSize?.toDoubleOrNull() ?: 0.0,
                        minNotional = notional?.minNotional?.toDoubleOrNull() ?: 0.0
                    )
                }
            tradingPairDao.insertAll(entities)
        } catch (e: Exception) {
            Timber.e(e, "Failed to refresh pairs")
        }
    }
}
