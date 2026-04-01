package com.tfg.data.remote.repository

import com.tfg.data.local.dao.AlertDao
import com.tfg.data.local.mapper.EntityMapper.toDomain
import com.tfg.data.local.mapper.EntityMapper.toEntity
import com.tfg.domain.model.Alert
import com.tfg.domain.repository.AlertRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertRepositoryImpl @Inject constructor(
    private val alertDao: AlertDao
) : AlertRepository {

    override fun getAllAlerts(): Flow<List<Alert>> =
        alertDao.getAll().map { list -> list.mapNotNull { it.toDomain() } }

    override fun getEnabledAlerts(): Flow<List<Alert>> =
        alertDao.getEnabled().map { list -> list.mapNotNull { it.toDomain() } }

    override fun getAlertById(id: String): Flow<Alert?> =
        alertDao.getById(id).map { it?.toDomain() }

    override fun getAlertsForSymbol(symbol: String): Flow<List<Alert>> =
        alertDao.getForSymbol(symbol).map { list -> list.mapNotNull { it.toDomain() } }

    override suspend fun saveAlert(alert: Alert) =
        alertDao.insert(alert.toEntity())

    override suspend fun deleteAlert(id: String) =
        alertDao.delete(id)

    override suspend fun updateEnabled(id: String, enabled: Boolean) =
        alertDao.updateEnabled(id, enabled)

    override suspend fun recordTrigger(id: String, timestamp: Long) =
        alertDao.recordTrigger(id, timestamp)
}
