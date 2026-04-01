package com.tfg.domain.repository

import com.tfg.domain.model.*
import kotlinx.coroutines.flow.Flow

interface MarketRepository {
    fun getTradingPairs(): Flow<List<TradingPair>>
    fun getWatchlist(): Flow<List<TradingPair>>
    fun getTicker(symbol: String): Flow<Ticker>
    fun getAllTickers(): Flow<List<Ticker>>
    fun getCandles(symbol: String, interval: String, limit: Int = 500): Flow<List<Candle>>
    fun getCandlesFromDb(symbol: String, interval: String): Flow<List<Candle>>
    fun getMarketData(): Flow<MarketData>

    suspend fun addToWatchlist(symbol: String)
    suspend fun removeFromWatchlist(symbol: String)
    suspend fun searchPairs(query: String): List<TradingPair>
    suspend fun refreshPairs()
}
