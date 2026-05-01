package com.tfg.data.remote.repository

import com.tfg.data.local.dao.*
import com.tfg.data.local.mapper.EntityMapper.toDomain
import com.tfg.data.local.mapper.EntityMapper.toEntity
import com.tfg.data.remote.api.BinanceApi
import com.tfg.data.remote.api.BinanceFuturesApi
import com.tfg.data.remote.api.BinanceTimeSync
import com.tfg.data.remote.interceptor.BinanceSigner
import com.tfg.domain.model.*
import com.tfg.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class PortfolioRepositoryImpl @Inject constructor(
    private val binanceApi: BinanceApi,
    private val binanceFuturesApi: BinanceFuturesApi,
    private val assetBalanceDao: AssetBalanceDao,
    private val orderDao: OrderDao,
    private val auditLogDao: AuditLogDao,
    private val timeSync: BinanceTimeSync,
    @Named("apiSecret") private val secretProvider: () -> String
) : PortfolioRepository {

    private suspend fun logError(wallet: String, error: Exception) {
        Timber.e(error, "Failed to fetch $wallet balances")
        try {
            auditLogDao.insert(
                com.tfg.data.local.entity.AuditLogEntity(
                    id = UUID.randomUUID().toString(),
                    action = "API_ERROR",
                    category = "SYSTEM",
                    details = "[$wallet] ${error.javaClass.simpleName}: ${error.message ?: "Unknown error"}",
                    userId = "system",
                    timestamp = System.currentTimeMillis()
                )
            )
        } catch (_: Exception) { }
    }

    override fun getPortfolio(): Flow<Portfolio> =
        assetBalanceDao.getAll(false).map { balances ->
            val assets = balances.map { it.toDomain() }
            val total = assets.sumOf { it.usdValue }
            Portfolio(
                totalBalance = total,
                availableBalance = assets.sumOf { it.free * (it.usdValue / (it.free + it.locked).coerceAtLeast(0.001)) },
                lockedBalance = assets.sumOf { it.locked * (it.usdValue / (it.free + it.locked).coerceAtLeast(0.001)) },
                assets = assets.map { it.copy(allocationPercent = if (total > 0) it.usdValue / total * 100 else 0.0) }
            )
        }

    override fun getAssetBalances(): Flow<List<AssetBalance>> =
        assetBalanceDao.getAll(false).map { list -> list.map { it.toDomain() } }

    override fun getOpenPositions(): Flow<List<Position>> =
        orderDao.getOpenOrders().map { orders ->
            orders.map { o ->
                val order = o.toDomain()
                Position(
                    symbol = order.symbol,
                    side = order.side,
                    entryPrice = order.filledPrice,
                    quantity = order.filledQuantity,
                    takeProfits = order.takeProfits,
                    stopLosses = order.stopLosses,
                    orderId = order.id,
                    openedAt = order.executedAt ?: order.createdAt
                )
            }
        }

    override fun getPaperPortfolio(): Flow<Portfolio> =
        assetBalanceDao.getAll(true).map { balances ->
            val assets = balances.map { it.toDomain() }
            Portfolio(
                totalBalance = assets.sumOf { it.usdValue },
                assets = assets,
                isPaperMode = true
            )
        }

    /** Build a fresh signed (timestamp, recvWindow, signature) triplet for a single Binance call. */
    private suspend fun signed(extra: Map<String, String> = emptyMap()): Triple<Long, Long, String> {
        val ts = timeSync.now()
        val recv = BinanceTimeSync.RECV_WINDOW_MS
        val params = LinkedHashMap<String, String>(extra.size + 2).apply {
            putAll(extra)
            put("timestamp", ts.toString())
            put("recvWindow", recv.toString())
        }
        return Triple(ts, recv, BinanceSigner.signParams(params, secretProvider()))
    }

    override suspend fun refreshPortfolio() {
        val errors = mutableListOf<String>()

        // ── Fetch all USDT prices in one call ──
        val priceMap = try {
            val tickers = binanceApi.get24hrTickers()
            val map = mutableMapOf<String, Double>()
            for (t in tickers) {
                // e.g. BTCUSDT → BTC = lastPrice
                if (t.symbol.endsWith("USDT")) {
                    val base = t.symbol.removeSuffix("USDT")
                    map[base] = t.lastPrice.toDoubleOrNull() ?: 0.0
                }
            }
            map["USDT"] = 1.0
            map["BUSD"] = 1.0
            map["FDUSD"] = 1.0
            map["USDC"] = 1.0
            map
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch price tickers")
            mapOf("USDT" to 1.0, "BUSD" to 1.0, "FDUSD" to 1.0, "USDC" to 1.0)
        }

        // ── Spot wallet ──
        val spotEntities = try {
            val (ts, recv, sig) = signed()
            val account = binanceApi.getAccount(ts, recv, sig)
            account.balances
                .filter { it.free.toDouble() > 0 || it.locked.toDouble() > 0 }
                .map { b ->
                    val total = b.free.toDouble() + b.locked.toDouble()
                    val price = priceMap[b.asset] ?: 0.0
                    com.tfg.data.local.entity.AssetBalanceEntity(
                        asset = b.asset,
                        free = b.free.toDouble(),
                        locked = b.locked.toDouble(),
                        usdValue = total * price,
                        walletType = "SPOT",
                        isPaper = false
                    )
                }
        } catch (e: Exception) {
            logError("Spot", e)
            errors.add("Spot: ${e.message}")
            emptyList()
        }

        // ── Funding wallet ──
        val fundingEntities = try {
            val (ts2, recv2, sig2) = signed()
            val funding = binanceApi.getFundingAsset(ts2, recv2, sig2)
            funding
                .filter { it.free.toDouble() > 0 || it.locked.toDouble() > 0 || it.freeze.toDouble() > 0 }
                .map { f ->
                    val free = f.free.toDouble()
                    val locked = f.locked.toDouble() + f.freeze.toDouble()
                    val price = priceMap[f.asset] ?: 0.0
                    com.tfg.data.local.entity.AssetBalanceEntity(
                        asset = f.asset,
                        free = free,
                        locked = locked,
                        usdValue = (free + locked) * price,
                        walletType = "FUNDING",
                        isPaper = false
                    )
                }
        } catch (e: Exception) {
            logError("Funding", e)
            errors.add("Funding: ${e.message}")
            emptyList()
        }

        // ── Futures (USDT-M) wallet ──
        val futuresEntities = try {
            val (ts3, recv3, sig3) = signed()
            val futures = binanceFuturesApi.getFuturesBalance(ts3, recv3, sig3)
            futures
                .filter { it.balance.toDouble() != 0.0 || it.availableBalance.toDouble() != 0.0 }
                .map { f ->
                    val avail = f.availableBalance.toDouble()
                    val total = f.balance.toDouble()
                    val price = priceMap[f.asset] ?: 0.0
                    com.tfg.data.local.entity.AssetBalanceEntity(
                        asset = f.asset,
                        free = avail,
                        locked = (total - avail).coerceAtLeast(0.0),
                        usdValue = total * price,
                        walletType = "FUTURES",
                        isPaper = false
                    )
                }
        } catch (e: Exception) {
            logError("Futures", e)
            errors.add("Futures: ${e.message}")
            emptyList()
        }

        // Clear and save all
        assetBalanceDao.deleteAll(false)
        val all = spotEntities + fundingEntities + futuresEntities
        if (all.isNotEmpty()) {
            assetBalanceDao.insertAll(all)
        }

        // If all wallets failed, throw so UI shows error
        if (errors.size == 3) {
            throw Exception("All wallet fetches failed. Check API keys & permissions.\n${errors.joinToString("\n")}")
        }
    }

    override suspend fun resetPaperAccount(initialBalance: Double) {
        assetBalanceDao.deleteAll(true)
        assetBalanceDao.insertAll(listOf(
            com.tfg.data.local.entity.AssetBalanceEntity(
                asset = "USDT", free = initialBalance, locked = 0.0,
                usdValue = initialBalance, isPaper = true
            )
        ))
    }
}

@Singleton
class SignalRepositoryImpl @Inject constructor(
    private val signalDao: SignalDao,
    private val signalVerifier: com.tfg.security.SignalVerifier
) : SignalRepository {

    override fun getSignals(): Flow<List<Signal>> =
        signalDao.getSignals().map { list -> list.map { it.toDomain() } }

    override fun getActiveSignals(): Flow<List<Signal>> =
        signalDao.getActiveSignals().map { list ->
            list.map { it.toDomain() }.filter { signal ->
                // Skip HMAC check for locally-generated signals (empty signature)
                signal.hmacSignature.isBlank() || verifySignal(signal)
            }
        }

    override fun getSignalById(id: String): Flow<Signal?> =
        signalDao.getSignalById(id).map { it?.toDomain() }

    override suspend fun acceptSignal(signalId: String): Result<Order> {
        throw UnsupportedOperationException("Use ExecuteSignalUseCase to execute signals")
    }

    override suspend fun skipSignal(signalId: String) {
        signalDao.updateStatus(signalId, SignalStatus.SKIPPED.name)
    }

    override suspend fun processOfflineSignals(): List<Signal> {
        signalDao.expireOldSignals()
        return signalDao.getMissedSignals().map { it.toDomain() }
            .filter { signal -> signal.hmacSignature.isBlank() || verifySignal(signal) }
    }

    override suspend fun getRecentSignals(limit: Int): Result<List<Signal>> = runCatching {
        signalDao.getSignals().map { list -> list.take(limit).map { it.toDomain() } }
            .first()
    }

    private fun verifySignal(signal: Signal): Boolean {
        val data = "${signal.id}:${signal.symbol}:${signal.side}:${signal.entryPrice}:${signal.expiresAt}"
        val secret = "tfg_signal_secret" // Should come from secure config
        return signalVerifier.verifyHmac(data, signal.hmacSignature, secret)
    }
}

@Singleton
class AuditRepositoryImpl @Inject constructor(
    private val auditLogDao: AuditLogDao
) : AuditRepository {

    override suspend fun log(auditLog: AuditLog) =
        auditLogDao.insert(auditLog.toEntity())

    override fun getLogs(category: AuditCategory?, limit: Int): Flow<List<AuditLog>> =
        if (category != null) {
            auditLogDao.getByCategory(category.name, limit).map { list -> list.map { it.toDomain() } }
        } else {
            auditLogDao.getAll(limit).map { list -> list.map { it.toDomain() } }
        }

    override fun getTradeAuditTrail(orderId: String): Flow<List<AuditLog>> =
        auditLogDao.getTradeAuditTrail(orderId).map { list -> list.map { it.toDomain() } }

    override suspend fun getRecentLogs(limit: Int): Result<List<AuditLog>> = runCatching {
        auditLogDao.getAll(limit).map { list -> list.map { it.toDomain() } }
            .first()
    }

    override suspend fun exportLogs(fromDate: Long, toDate: Long): String {
        val logs = auditLogDao.getLogsInRange(fromDate, toDate)
        val sb = StringBuilder("Timestamp,Action,Category,Details,OrderId,Symbol\n")
        logs.forEach { l ->
            sb.appendLine("${l.timestamp},${l.action},${l.category},\"${l.details}\",${l.orderId ?: ""},${l.symbol ?: ""}")
        }
        return sb.toString()
    }

    private fun com.tfg.data.local.entity.AuditLogEntity.toDomain(): AuditLog = AuditLog(
        id = id, action = AuditAction.valueOf(action), category = AuditCategory.valueOf(category),
        details = details, oldValue = oldValue, newValue = newValue,
        orderId = orderId, symbol = symbol, userId = userId,
        ipAddress = ipAddress, timestamp = timestamp
    )

    private fun AuditLog.toEntity() = com.tfg.data.local.entity.AuditLogEntity(
        id = id, action = action.name, category = category.name,
        details = details, oldValue = oldValue, newValue = newValue,
        orderId = orderId, symbol = symbol, userId = userId,
        ipAddress = ipAddress, timestamp = timestamp
    )
}

@Singleton
class FeeRepositoryImpl @Inject constructor(
    private val feeRecordDao: FeeRecordDao
) : FeeRepository {
    private val _feeConfig = kotlinx.coroutines.flow.MutableStateFlow(FeeConfig())

    override fun getFeeConfig(): Flow<FeeConfig> = _feeConfig

    override fun getFeeHistory(): Flow<List<FeeRecord>> =
        feeRecordDao.getAll().map { list -> list.map { it.toDomain() } }

    override fun getTotalFees(fromDate: Long?): Flow<Double> =
        feeRecordDao.getTotalFees(fromDate).map { it ?: 0.0 }

    override suspend fun updateFeeConfig(config: FeeConfig) { _feeConfig.value = config }

    override suspend fun recordFee(record: FeeRecord) = feeRecordDao.insert(record.toEntity())

    private fun com.tfg.data.local.entity.FeeRecordEntity.toDomain() = FeeRecord(
        id = id, orderId = orderId, symbol = symbol,
        feeAmount = feeAmount, feeAsset = feeAsset,
        feeType = FeeType.valueOf(feeType), timestamp = timestamp
    )

    private fun FeeRecord.toEntity() = com.tfg.data.local.entity.FeeRecordEntity(
        id = id, orderId = orderId, symbol = symbol,
        feeAmount = feeAmount, feeAsset = feeAsset,
        feeType = feeType.name, timestamp = timestamp
    )
}

@Singleton
class DonationRepositoryImpl @Inject constructor(
    private val donationDao: DonationDao
) : DonationRepository {

    override fun getDonations(): Flow<List<Donation>> =
        donationDao.getAll().map { list -> list.map { it.toDomain() } }

    override fun getTotalDonated(): Flow<Double> =
        donationDao.getTotalDonated().map { it ?: 0.0 }

    override suspend fun processDonation(orderId: String, profit: Double, ngoId: String): Result<Donation> = runCatching {
        val donation = Donation(
            id = java.util.UUID.randomUUID().toString(),
            orderId = orderId,
            amount = profit * 0.05,
            currency = "USDT",
            ngoName = "TradeForGood NGO",
            ngoId = ngoId,
            status = DonationStatus.PENDING
        )
        donationDao.insert(donation.toEntity())
        donation
    }

    private fun com.tfg.data.local.entity.DonationEntity.toDomain() = Donation(
        id = id, orderId = orderId, amount = amount, currency = currency,
        ngoName = ngoName, ngoId = ngoId,
        status = DonationStatus.valueOf(status), timestamp = timestamp
    )

    private fun Donation.toEntity() = com.tfg.data.local.entity.DonationEntity(
        id = id, orderId = orderId, amount = amount, currency = currency,
        ngoName = ngoName, ngoId = ngoId, status = status.name, timestamp = timestamp
    )
}
