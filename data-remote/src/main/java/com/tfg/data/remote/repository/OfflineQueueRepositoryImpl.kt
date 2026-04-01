package com.tfg.data.remote.repository

import com.tfg.data.local.dao.OfflineQueueDao
import com.tfg.data.local.entity.OfflineQueueEntity
import com.tfg.domain.model.OfflineQueueItem
import com.tfg.domain.model.QueueAction
import com.tfg.domain.model.Signal
import com.tfg.domain.model.Order
import com.tfg.domain.repository.OfflineQueueRepository
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineQueueRepositoryImpl @Inject constructor(
    private val offlineQueueDao: OfflineQueueDao,
    private val gson: Gson
) : OfflineQueueRepository {

    override suspend fun enqueue(item: OfflineQueueItem) {
        offlineQueueDao.insert(
            OfflineQueueEntity(
                id = item.id.ifBlank { UUID.randomUUID().toString() },
                action = item.action.name,
                signalJson = item.signal?.let { gson.toJson(it) },
                orderJson = item.order?.let { gson.toJson(it) },
                createdAt = item.createdAt,
                priority = item.priority,
                retryCount = item.retryCount,
                maxRetries = item.maxRetries,
                lastError = item.lastError
            )
        )
    }

    override suspend fun dequeue(id: String) {
        offlineQueueDao.delete(id)
    }

    override fun getQueueItems(): Flow<List<OfflineQueueItem>> =
        offlineQueueDao.getAll().map { entities ->
            entities.map {
                OfflineQueueItem(
                    id = it.id,
                    signal = it.signalJson?.let { json -> runCatching { gson.fromJson(json, Signal::class.java) }.getOrNull() },
                    order = it.orderJson?.let { json -> runCatching { gson.fromJson(json, Order::class.java) }.getOrNull() },
                    action = QueueAction.valueOf(it.action),
                    priority = it.priority,
                    createdAt = it.createdAt,
                    retryCount = it.retryCount,
                    maxRetries = it.maxRetries,
                    lastError = it.lastError
                )
            }
        }

    override suspend fun drainQueue(): List<OfflineQueueItem> =
        offlineQueueDao.getRetryable().map {
            OfflineQueueItem(
                id = it.id,
                signal = it.signalJson?.let { json -> runCatching { gson.fromJson(json, Signal::class.java) }.getOrNull() },
                order = it.orderJson?.let { json -> runCatching { gson.fromJson(json, Order::class.java) }.getOrNull() },
                action = QueueAction.valueOf(it.action),
                priority = it.priority,
                createdAt = it.createdAt,
                retryCount = it.retryCount,
                maxRetries = it.maxRetries,
                lastError = it.lastError
            )
        }

    override suspend fun markFailed(id: String, error: String) {
        offlineQueueDao.markFailed(id, error)
    }

    override suspend fun clearDeadLetters() {
        offlineQueueDao.clearDeadLetters()
    }

    override fun getQueueSize(): Flow<Int> =
        offlineQueueDao.getAll().map { it.size }
}
