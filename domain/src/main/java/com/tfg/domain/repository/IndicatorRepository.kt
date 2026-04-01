package com.tfg.domain.repository

import com.tfg.domain.model.Indicator
import kotlinx.coroutines.flow.Flow

interface IndicatorRepository {
    fun getAllIndicators(): Flow<List<Indicator>>
    fun getEnabledIndicators(): Flow<List<Indicator>>
    suspend fun getIndicatorById(id: String): Indicator?
    suspend fun saveIndicator(indicator: Indicator)
    suspend fun deleteIndicator(id: String)
    suspend fun setEnabled(id: String, enabled: Boolean)
}
