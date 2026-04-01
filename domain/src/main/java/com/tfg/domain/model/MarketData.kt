package com.tfg.domain.model

data class Candle(
    val symbol: String,
    val interval: String,
    val openTime: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double,
    val closeTime: Long,
    val quoteVolume: Double = 0.0,
    val numberOfTrades: Int = 0
)

data class Ticker(
    val symbol: String,
    val price: Double,
    val volume: Double = 0.0,
    val priceChangePercent: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)

data class MarketData(
    val fearGreedIndex: Int = 50,
    val fearGreedLabel: String = "Neutral",
    val btcDominance: Double = 0.0,
    val totalMarketCap: Double = 0.0,
    val btcPrice: Double = 0.0,
    val updatedAt: Long = System.currentTimeMillis()
)
