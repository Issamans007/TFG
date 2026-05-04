package com.tfg.data.local.dao

import androidx.room.*
import com.tfg.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders WHERE status IN ('PENDING','SUBMITTED','PARTIALLY_FILLED') ORDER BY createdAt DESC")
    fun getOpenOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders ORDER BY createdAt DESC LIMIT :limit")
    fun getOrderHistory(limit: Int): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE id = :orderId")
    fun getOrderById(orderId: String): Flow<OrderEntity?>

    @Query("SELECT * FROM orders WHERE symbol = :symbol AND status IN ('PENDING','SUBMITTED','PARTIALLY_FILLED')")
    fun getOpenOrdersForSymbol(symbol: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE isPaperTrade = 1 ORDER BY createdAt DESC LIMIT :limit")
    fun getPaperOrders(limit: Int): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE signalId = :signalId LIMIT 1")
    suspend fun getOrderBySignalId(signalId: String): OrderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(order: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(orders: List<OrderEntity>)

    @Update
    suspend fun update(order: OrderEntity)

    @Query("UPDATE orders SET status = :status, updatedAt = :updatedAt WHERE id = :orderId")
    suspend fun updateStatus(orderId: String, status: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM orders WHERE id = :orderId")
    suspend fun delete(orderId: String)

    @Query("SELECT COUNT(*) FROM orders WHERE status IN ('PENDING','SUBMITTED','PARTIALLY_FILLED')")
    fun getOpenOrderCount(): Flow<Int>

    @Query("SELECT SUM(realizedPnl) FROM orders WHERE closedAt >= :since")
    fun getDailyPnl(since: Long): Flow<Double?>

    @Query("SELECT * FROM orders WHERE closedAt IS NOT NULL AND isPaperTrade = :isPaper ORDER BY closedAt DESC")
    fun getClosedOrders(isPaper: Boolean = false): Flow<List<OrderEntity>>
}

@Dao
interface SignalDao {
    @Query("SELECT * FROM signals ORDER BY receivedAt DESC")
    fun getSignals(): Flow<List<SignalEntity>>

    @Query("SELECT * FROM signals WHERE status = 'PENDING' AND isExpired = 0")
    fun getActiveSignals(): Flow<List<SignalEntity>>

    @Query("SELECT * FROM signals WHERE id = :id")
    fun getSignalById(id: String): Flow<SignalEntity?>

    @Query("SELECT * FROM signals WHERE missedWhileOffline = 1 AND isExpired = 0")
    suspend fun getMissedSignals(): List<SignalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(signal: SignalEntity)

    @Update
    suspend fun update(signal: SignalEntity)

    @Query("UPDATE signals SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("UPDATE signals SET isExpired = 1, status = 'EXPIRED' WHERE expiresAt < :now AND isExpired = 0")
    suspend fun expireOldSignals(now: Long = System.currentTimeMillis())
}

@Dao
interface TradingPairDao {
    @Query("SELECT * FROM trading_pairs ORDER BY volume24h DESC")
    fun getAll(): Flow<List<TradingPairEntity>>

    @Query("SELECT * FROM trading_pairs WHERE isWatchlisted = 1")
    fun getWatchlist(): Flow<List<TradingPairEntity>>

    @Query("SELECT symbol FROM trading_pairs WHERE isWatchlisted = 1")
    suspend fun getWatchlistSymbols(): List<String>

    @Query("SELECT * FROM trading_pairs WHERE symbol LIKE '%' || :query || '%' OR baseAsset LIKE '%' || :query || '%'")
    suspend fun search(query: String): List<TradingPairEntity>

    @Query("SELECT * FROM trading_pairs WHERE symbol = :symbol")
    fun getBySymbol(symbol: String): Flow<TradingPairEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pairs: List<TradingPairEntity>)

    @Query("UPDATE trading_pairs SET isWatchlisted = :watched WHERE symbol = :symbol")
    suspend fun setWatchlisted(symbol: String, watched: Boolean)

    @Query("UPDATE trading_pairs SET lastPrice = :price, priceChangePercent24h = :change, volume24h = :volume WHERE symbol = :symbol")
    suspend fun updateTicker(symbol: String, price: Double, change: Double, volume: Double)
}

@Dao
interface CandleDao {
    @Query("SELECT * FROM candles WHERE symbol = :symbol AND interval = :interval ORDER BY openTime ASC LIMIT :limit")
    fun getCandles(symbol: String, interval: String, limit: Int): Flow<List<CandleEntity>>

    @Query("SELECT * FROM candles WHERE symbol = :symbol AND interval = :interval ORDER BY openTime ASC")
    fun getAllCandles(symbol: String, interval: String): Flow<List<CandleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(candles: List<CandleEntity>)

    @Query("DELETE FROM candles WHERE symbol = :symbol AND interval = :interval")
    suspend fun deleteForPair(symbol: String, interval: String)
}

@Dao
interface AssetBalanceDao {
    @Query("SELECT * FROM asset_balances WHERE isPaper = :isPaper ORDER BY usdValue DESC")
    fun getAll(isPaper: Boolean = false): Flow<List<AssetBalanceEntity>>

    @Query("SELECT * FROM asset_balances WHERE walletType = :walletType AND isPaper = :isPaper ORDER BY usdValue DESC")
    fun getByWalletType(walletType: String, isPaper: Boolean = false): Flow<List<AssetBalanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(balances: List<AssetBalanceEntity>)

    @Query("DELETE FROM asset_balances WHERE isPaper = :isPaper")
    suspend fun deleteAll(isPaper: Boolean = false)

    @Query("DELETE FROM asset_balances WHERE walletType = :walletType AND isPaper = :isPaper")
    suspend fun deleteByWalletType(walletType: String, isPaper: Boolean = false)
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getAll(limit: Int): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs WHERE category = :category ORDER BY timestamp DESC LIMIT :limit")
    fun getByCategory(category: String, limit: Int): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs WHERE orderId = :orderId ORDER BY timestamp ASC")
    fun getTradeAuditTrail(orderId: String): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs WHERE timestamp BETWEEN :from AND :to ORDER BY timestamp ASC")
    suspend fun getLogsInRange(from: Long, to: Long): List<AuditLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: AuditLogEntity)
}

@Dao
interface FeeRecordDao {
    @Query("SELECT * FROM fee_records ORDER BY timestamp DESC")
    fun getAll(): Flow<List<FeeRecordEntity>>

    @Query("SELECT SUM(feeAmount) FROM fee_records WHERE (:from IS NULL OR timestamp >= :from)")
    fun getTotalFees(from: Long?): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: FeeRecordEntity)
}

@Dao
interface DonationDao {
    @Query("SELECT * FROM donations ORDER BY timestamp DESC")
    fun getAll(): Flow<List<DonationEntity>>

    @Query("SELECT SUM(amount) FROM donations WHERE status = 'CONFIRMED'")
    fun getTotalDonated(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(donation: DonationEntity)

    @Update
    suspend fun update(donation: DonationEntity)
}

@Dao
interface ScriptDao {
    @Query("SELECT * FROM scripts ORDER BY updatedAt DESC")
    fun getAll(): Flow<List<ScriptEntity>>

    @Query("SELECT * FROM scripts WHERE id = :id")
    fun getById(id: String): Flow<ScriptEntity?>

    @Query("SELECT * FROM scripts WHERE isActive = 1")
    fun getActive(): Flow<List<ScriptEntity>>

    @Query("SELECT * FROM scripts WHERE isActive = 1 LIMIT 1")
    fun getActiveScript(): Flow<ScriptEntity?>

    @Query("SELECT activeSymbol FROM scripts WHERE isActive = 1 AND activeSymbol IS NOT NULL")
    fun getBlockedSymbols(): Flow<List<String>>

    @Query("UPDATE scripts SET isActive = 0, activeSymbol = NULL")
    suspend fun deactivateAll()

    @Query("UPDATE scripts SET isActive = 1, activeSymbol = :symbol WHERE id = :id")
    suspend fun activate(id: String, symbol: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(script: ScriptEntity)

    @Update
    suspend fun update(script: ScriptEntity)

    @Query("DELETE FROM scripts WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface OfflineQueueDao {
    @Query("SELECT * FROM offline_queue ORDER BY priority DESC, createdAt ASC")
    fun getAll(): Flow<List<OfflineQueueEntity>>

    @Query("SELECT COUNT(*) FROM offline_queue")
    fun getCount(): Flow<Int>

    @Query("SELECT * FROM offline_queue WHERE retryCount < maxRetries AND isProcessing = 0 ORDER BY priority DESC, createdAt ASC")
    suspend fun getRetryable(): List<OfflineQueueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: OfflineQueueEntity)

    @Query("DELETE FROM offline_queue WHERE id = :id")
    suspend fun delete(id: String)

    /**
     * Atomically claim the row for processing. Returns the number of rows updated
     * (1 if claimed, 0 if another drainer already owns it).
     */
    @Query("UPDATE offline_queue SET isProcessing = 1 WHERE id = :id AND isProcessing = 0")
    suspend fun claimForProcessing(id: String): Int

    @Query("UPDATE offline_queue SET retryCount = retryCount + 1, lastError = :error, isProcessing = 0 WHERE id = :id")
    suspend fun markFailed(id: String, error: String)

    @Query("DELETE FROM offline_queue WHERE retryCount >= maxRetries")
    suspend fun clearDeadLetters()
}

@Dao
interface SignalMarkerDao {
    @Query("SELECT * FROM signal_markers WHERE symbol = :symbol AND interval = :interval ORDER BY openTime ASC")
    fun getForChart(symbol: String, interval: String): Flow<List<SignalMarkerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(markers: List<SignalMarkerEntity>)

    @Query("DELETE FROM signal_markers WHERE scriptId = :scriptId")
    suspend fun deleteForScript(scriptId: String)

    @Query("DELETE FROM signal_markers WHERE symbol = :symbol")
    suspend fun deleteForSymbol(symbol: String)

    @Query("DELETE FROM signal_markers WHERE scriptId = :scriptId AND symbol = :symbol")
    suspend fun deleteForScriptAndSymbol(scriptId: String, symbol: String)
}

@Dao
interface CustomTemplateDao {
    @Query("SELECT * FROM custom_templates ORDER BY createdAt DESC")
    fun getAll(): Flow<List<CustomTemplateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(template: CustomTemplateEntity)

    @Query("DELETE FROM custom_templates WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface DrawingSnapshotDao {
    @Query("SELECT * FROM chart_drawings_snapshot WHERE symbol = :symbol")
    suspend fun getForSymbol(symbol: String): DrawingSnapshotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(snapshot: DrawingSnapshotEntity)

    @Query("DELETE FROM chart_drawings_snapshot WHERE symbol = :symbol")
    suspend fun deleteForSymbol(symbol: String)
}

@Dao
interface AlertDao {
    @Query("SELECT * FROM alerts ORDER BY createdAt DESC")
    fun getAll(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts WHERE isEnabled = 1 ORDER BY createdAt DESC")
    fun getEnabled(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts WHERE id = :id")
    fun getById(id: String): Flow<AlertEntity?>

    @Query("SELECT * FROM alerts WHERE symbol = :symbol ORDER BY createdAt DESC")
    fun getForSymbol(symbol: String): Flow<List<AlertEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alert: AlertEntity)

    @Query("DELETE FROM alerts WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE alerts SET isEnabled = :enabled, updatedAt = :now WHERE id = :id")
    suspend fun updateEnabled(id: String, enabled: Boolean, now: Long = System.currentTimeMillis())

    @Query("UPDATE alerts SET lastTriggeredAt = :timestamp, triggerCount = triggerCount + 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun recordTrigger(id: String, timestamp: Long)
}

@Dao
interface IndicatorDao {
    @Query("SELECT * FROM indicators ORDER BY updatedAt DESC")
    fun getAll(): Flow<List<IndicatorEntity>>

    @Query("SELECT * FROM indicators WHERE isEnabled = 1 ORDER BY updatedAt DESC")
    fun getEnabled(): Flow<List<IndicatorEntity>>

    @Query("SELECT * FROM indicators WHERE id = :id")
    suspend fun getById(id: String): IndicatorEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(indicator: IndicatorEntity)

    @Query("DELETE FROM indicators WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE indicators SET isEnabled = :enabled, updatedAt = :now WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean, now: Long = System.currentTimeMillis())
}
