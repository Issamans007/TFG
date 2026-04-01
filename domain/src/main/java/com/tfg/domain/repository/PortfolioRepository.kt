package com.tfg.domain.repository

import com.tfg.domain.model.*
import kotlinx.coroutines.flow.Flow

interface PortfolioRepository {
    fun getPortfolio(): Flow<Portfolio>
    fun getAssetBalances(): Flow<List<AssetBalance>>
    fun getOpenPositions(): Flow<List<Position>>
    fun getPaperPortfolio(): Flow<Portfolio>
    suspend fun refreshPortfolio()
    suspend fun resetPaperAccount(initialBalance: Double = 10000.0)
}

interface AnalyticsRepository {
    fun getAnalytics(periodStart: Long, periodEnd: Long, isPaper: Boolean = false): Flow<AnalyticsSnapshot>
    fun getDailyPnl(days: Int = 30, isPaper: Boolean = false): Flow<List<DailyPnlEntry>>
    fun getEquityCurve(isPaper: Boolean = false): Flow<List<EquityPoint>>
    fun getPairPerformance(isPaper: Boolean = false): Flow<List<PairPerformance>>
    fun getStrategyPerformance(isPaper: Boolean = false): Flow<List<StrategyPerformance>>
    suspend fun exportCsv(): String
    suspend fun exportPdfReport(): ByteArray
}
