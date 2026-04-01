package com.tfg.domain.model

data class TradingPair(
    val symbol: String,
    val baseAsset: String,
    val quoteAsset: String,
    val lastPrice: Double = 0.0,
    val priceChangePercent24h: Double = 0.0,
    val volume24h: Double = 0.0,
    val high24h: Double = 0.0,
    val low24h: Double = 0.0,
    val isWatchlisted: Boolean = false,
    val isActiveForTrading: Boolean = false,
    val minQty: Double = 0.0,
    val stepSize: Double = 0.0,
    val tickSize: Double = 0.0,
    val minNotional: Double = 0.0
)
