package com.tfg.domain.repository

import com.tfg.domain.model.Alert
import kotlinx.coroutines.flow.Flow

interface AlertRepository {
    fun getAllAlerts(): Flow<List<Alert>>
    fun getEnabledAlerts(): Flow<List<Alert>>
    fun getAlertById(id: String): Flow<Alert?>
    fun getAlertsForSymbol(symbol: String): Flow<List<Alert>>
    suspend fun saveAlert(alert: Alert)
    suspend fun deleteAlert(id: String)
    suspend fun updateEnabled(id: String, enabled: Boolean)
    suspend fun recordTrigger(id: String, timestamp: Long)
}
