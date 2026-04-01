package com.tfg.domain.usecase.analytics

import com.tfg.domain.model.*
import com.tfg.domain.repository.AnalyticsRepository
import com.tfg.domain.repository.PortfolioRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAnalyticsUseCase @Inject constructor(private val analyticsRepository: AnalyticsRepository) {
    operator fun invoke(periodStart: Long, periodEnd: Long, isPaper: Boolean = false): Flow<AnalyticsSnapshot> =
        analyticsRepository.getAnalytics(periodStart, periodEnd, isPaper)
}

class GetDailyPnlUseCase @Inject constructor(private val analyticsRepository: AnalyticsRepository) {
    operator fun invoke(days: Int = 30, isPaper: Boolean = false): Flow<List<DailyPnlEntry>> =
        analyticsRepository.getDailyPnl(days, isPaper)
}

class GetEquityCurveUseCase @Inject constructor(private val analyticsRepository: AnalyticsRepository) {
    operator fun invoke(isPaper: Boolean = false): Flow<List<EquityPoint>> =
        analyticsRepository.getEquityCurve(isPaper)
}

class GetPairPerformanceUseCase @Inject constructor(private val analyticsRepository: AnalyticsRepository) {
    operator fun invoke(isPaper: Boolean = false): Flow<List<PairPerformance>> =
        analyticsRepository.getPairPerformance(isPaper)
}

class GetStrategyPerformanceUseCase @Inject constructor(private val analyticsRepository: AnalyticsRepository) {
    operator fun invoke(isPaper: Boolean = false): Flow<List<StrategyPerformance>> =
        analyticsRepository.getStrategyPerformance(isPaper)
}

class ExportTradesUseCase @Inject constructor(private val analyticsRepository: AnalyticsRepository) {
    suspend fun exportCsv(): String = analyticsRepository.exportCsv()
    suspend fun exportPdf(): ByteArray = analyticsRepository.exportPdfReport()
}

class GetPortfolioUseCase @Inject constructor(private val portfolioRepository: PortfolioRepository) {
    operator fun invoke(): Flow<Portfolio> = portfolioRepository.getPortfolio()
}

class GetPaperPortfolioUseCase @Inject constructor(private val portfolioRepository: PortfolioRepository) {
    operator fun invoke(): Flow<Portfolio> = portfolioRepository.getPaperPortfolio()
}
