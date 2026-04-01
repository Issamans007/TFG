package com.tfg.domain.repository

import com.tfg.domain.model.*
import kotlinx.coroutines.flow.Flow

interface SignalRepository {
    fun getSignals(): Flow<List<Signal>>
    fun getActiveSignals(): Flow<List<Signal>>
    fun getSignalById(id: String): Flow<Signal?>
    suspend fun acceptSignal(signalId: String): Result<Order>
    suspend fun skipSignal(signalId: String)
    suspend fun processOfflineSignals(): List<Signal>
    suspend fun getRecentSignals(limit: Int = 10): Result<List<Signal>>
}

interface AuditRepository {
    suspend fun log(auditLog: AuditLog)
    fun getLogs(category: AuditCategory? = null, limit: Int = 100): Flow<List<AuditLog>>
    suspend fun getRecentLogs(limit: Int = 50): Result<List<AuditLog>>
    fun getTradeAuditTrail(orderId: String): Flow<List<AuditLog>>
    suspend fun exportLogs(fromDate: Long, toDate: Long): String
}

interface DonationRepository {
    fun getDonations(): Flow<List<Donation>>
    fun getTotalDonated(): Flow<Double>
    suspend fun processDonation(orderId: String, profit: Double, ngoId: String): Result<Donation>
}

interface FeeRepository {
    fun getFeeConfig(): Flow<FeeConfig>
    fun getFeeHistory(): Flow<List<FeeRecord>>
    fun getTotalFees(fromDate: Long? = null): Flow<Double>
    suspend fun updateFeeConfig(config: FeeConfig)
    suspend fun recordFee(record: FeeRecord)
}

interface OfflineQueueRepository {
    fun getQueueItems(): Flow<List<OfflineQueueItem>>
    suspend fun enqueue(item: OfflineQueueItem)
    suspend fun dequeue(id: String)
    suspend fun markFailed(id: String, error: String)
    suspend fun drainQueue(): List<OfflineQueueItem>
    suspend fun clearDeadLetters()
    fun getQueueSize(): Flow<Int>
}
