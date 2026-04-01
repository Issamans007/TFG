package com.tfg.data.remote.repository

import com.tfg.data.local.dao.OrderDao
import com.tfg.data.local.dao.FeeRecordDao
import com.tfg.data.local.dao.DonationDao
import com.tfg.data.local.mapper.EntityMapper.toDomain
import com.tfg.domain.model.*
import com.tfg.domain.repository.AnalyticsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class AnalyticsRepositoryImpl @Inject constructor(
    private val orderDao: OrderDao,
    private val feeRecordDao: FeeRecordDao,
    private val donationDao: DonationDao
) : AnalyticsRepository {

    override fun getAnalytics(periodStart: Long, periodEnd: Long, isPaper: Boolean): Flow<AnalyticsSnapshot> =
        orderDao.getClosedOrders(isPaper).map { entities ->
            val orders = entities.map { it.toDomain() }
                .filter { o -> (o.closedAt ?: 0) in periodStart..periodEnd }

            if (orders.isEmpty()) return@map AnalyticsSnapshot(periodStart = periodStart, periodEnd = periodEnd, isPaperMode = isPaper)

            val wins = orders.filter { it.realizedPnl > 0 }
            val losses = orders.filter { it.realizedPnl < 0 }
            val totalPnl = orders.sumOf { it.realizedPnl }
            val grossProfit = wins.sumOf { it.realizedPnl }
            val grossLoss = losses.sumOf { kotlin.math.abs(it.realizedPnl) }
            val pnls = orders.map { it.realizedPnl }
            val avgPnl = if (pnls.isNotEmpty()) pnls.average() else 0.0
            val stdDev = if (pnls.size > 1) {
                sqrt(pnls.map { (it - avgPnl) * (it - avgPnl) }.average())
            } else 0.0
            val downsideDev = if (pnls.size > 1) {
                sqrt(pnls.filter { it < 0 }.map { it * it }.average().takeIf { !it.isNaN() } ?: 0.0)
            } else 0.0

            var maxDrawdown = 0.0
            var peak = 0.0
            var cumPnl = 0.0
            for (o in orders) {
                cumPnl += o.realizedPnl
                if (cumPnl > peak) peak = cumPnl
                val dd = peak - cumPnl
                if (dd > maxDrawdown) maxDrawdown = dd
            }

            var winStreak = 0; var lossStreak = 0; var maxWin = 0; var maxLoss = 0; var curWin = 0; var curLoss = 0
            for (o in orders) {
                if (o.realizedPnl > 0) { curWin++; curLoss = 0; if (curWin > maxWin) maxWin = curWin }
                else if (o.realizedPnl < 0) { curLoss++; curWin = 0; if (curLoss > maxLoss) maxLoss = curLoss }
            }

            AnalyticsSnapshot(
                totalReturnAmount = totalPnl,
                totalReturnPercent = if (orders.isNotEmpty()) totalPnl / orders.sumOf { it.filledPrice * it.filledQuantity }.coerceAtLeast(1.0) * 100 else 0.0,
                winRate = if (orders.isNotEmpty()) wins.size.toDouble() / orders.size * 100 else 0.0,
                totalTrades = orders.size,
                winningTrades = wins.size,
                losingTrades = losses.size,
                profitFactor = if (grossLoss > 0) grossProfit / grossLoss else if (grossProfit > 0) Double.MAX_VALUE else 0.0,
                sharpeRatio = if (stdDev > 0) avgPnl / stdDev else 0.0,
                sortinoRatio = if (downsideDev > 0) avgPnl / downsideDev else 0.0,
                calmarRatio = if (maxDrawdown > 0) totalPnl / maxDrawdown else 0.0,
                maxDrawdownAmount = maxDrawdown,
                maxDrawdownPercent = if (peak > 0) maxDrawdown / peak * 100 else 0.0,
                avgWin = if (wins.isNotEmpty()) wins.map { it.realizedPnl }.average() else 0.0,
                avgLoss = if (losses.isNotEmpty()) losses.map { it.realizedPnl }.average() else 0.0,
                payoffRatio = if (losses.isNotEmpty() && wins.isNotEmpty()) (wins.map { it.realizedPnl }.average()) / kotlin.math.abs(losses.map { it.realizedPnl }.average()) else 0.0,
                bestTradePnl = orders.maxOfOrNull { it.realizedPnl } ?: 0.0,
                worstTradePnl = orders.minOfOrNull { it.realizedPnl } ?: 0.0,
                longestWinStreak = maxWin,
                longestLossStreak = maxLoss,
                totalFeesPaid = orders.sumOf { it.fee },
                totalDonations = orders.sumOf { it.donationAmount },
                avgHoldTimeMinutes = orders.mapNotNull { o -> o.closedAt?.let { c -> o.executedAt?.let { e -> c - e } } }.let { if (it.isNotEmpty()) it.average().toLong() / 60000 else 0L },
                periodStart = periodStart,
                periodEnd = periodEnd,
                isPaperMode = isPaper
            )
        }

    override fun getDailyPnl(days: Int, isPaper: Boolean): Flow<List<DailyPnlEntry>> =
        orderDao.getClosedOrders(isPaper).map { entities ->
            val orders = entities.map { it.toDomain() }
            val cutoff = System.currentTimeMillis() - days * 86400000L
            val filtered = orders.filter { (it.closedAt ?: 0) >= cutoff }
            val grouped = filtered.groupBy { ((it.closedAt ?: 0) / 86400000L) * 86400000L }
            var cumPnl = 0.0
            grouped.entries.sortedBy { it.key }.map { (date, dayOrders) ->
                val dayPnl = dayOrders.sumOf { it.realizedPnl }
                cumPnl += dayPnl
                DailyPnlEntry(date = date, pnl = dayPnl, cumulativePnl = cumPnl, tradeCount = dayOrders.size)
            }
        }

    override fun getEquityCurve(isPaper: Boolean): Flow<List<EquityPoint>> =
        orderDao.getClosedOrders(isPaper).map { entities ->
            val orders = entities.map { it.toDomain() }.sortedBy { it.closedAt }
            var equity = 0.0
            var peak = 0.0
            orders.mapNotNull { o ->
                val ts = o.closedAt ?: return@mapNotNull null
                equity += o.realizedPnl
                if (equity > peak) peak = equity
                EquityPoint(timestamp = ts, equity = equity, drawdown = if (peak > 0) (peak - equity) / peak * 100 else 0.0)
            }
        }

    override fun getPairPerformance(isPaper: Boolean): Flow<List<PairPerformance>> =
        orderDao.getClosedOrders(isPaper).map { entities ->
            val orders = entities.map { it.toDomain() }
            orders.groupBy { it.symbol }.map { (symbol, symbolOrders) ->
                val wins = symbolOrders.count { it.realizedPnl > 0 }
                PairPerformance(
                    symbol = symbol,
                    totalTrades = symbolOrders.size,
                    winRate = if (symbolOrders.isNotEmpty()) wins.toDouble() / symbolOrders.size * 100 else 0.0,
                    totalPnl = symbolOrders.sumOf { it.realizedPnl },
                    avgPnl = if (symbolOrders.isNotEmpty()) symbolOrders.map { it.realizedPnl }.average() else 0.0,
                    totalFees = symbolOrders.sumOf { it.fee }
                )
            }
        }

    override fun getStrategyPerformance(isPaper: Boolean): Flow<List<StrategyPerformance>> =
        orderDao.getClosedOrders(isPaper).map { entities ->
            val orders = entities.map { it.toDomain() }
            orders.groupBy { it.executionMode }.map { (mode, modeOrders) ->
                val wins = modeOrders.count { it.realizedPnl > 0 }
                val pnls = modeOrders.map { it.realizedPnl }
                val avgPnl = if (pnls.isNotEmpty()) pnls.average() else 0.0
                val stdDev = if (pnls.size > 1) sqrt(pnls.map { (it - avgPnl) * (it - avgPnl) }.average()) else 0.0
                var maxDd = 0.0; var peak = 0.0; var cum = 0.0
                for (o in modeOrders) { cum += o.realizedPnl; if (cum > peak) peak = cum; val dd = peak - cum; if (dd > maxDd) maxDd = dd }
                StrategyPerformance(
                    strategyName = mode.name,
                    executionMode = mode,
                    totalTrades = modeOrders.size,
                    winRate = if (modeOrders.isNotEmpty()) wins.toDouble() / modeOrders.size * 100 else 0.0,
                    totalPnl = pnls.sum(),
                    sharpeRatio = if (stdDev > 0) avgPnl / stdDev else 0.0,
                    maxDrawdown = maxDd
                )
            }
        }

    override suspend fun exportCsv(): String {
        val sb = StringBuilder("ID,Symbol,Side,Type,Status,Quantity,Price,Fee,PnL,CreatedAt,ClosedAt\n")
        val entities = orderDao.getClosedOrders(false).first()
        entities.map { it.toDomain() }.forEach { o ->
            sb.appendLine("${o.id},${o.symbol},${o.side},${o.type},${o.status},${o.filledQuantity},${o.filledPrice},${o.fee},${o.realizedPnl},${o.createdAt},${o.closedAt ?: ""}")
        }
        return sb.toString()
    }

    override suspend fun exportPdfReport(): ByteArray {
        // PDF generation placeholder - returns CSV as bytes
        return exportCsv().toByteArray(Charsets.UTF_8)
    }
}
