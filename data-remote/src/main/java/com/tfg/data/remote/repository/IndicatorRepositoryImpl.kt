package com.tfg.data.remote.repository

import com.tfg.data.local.dao.IndicatorDao
import com.tfg.data.local.mapper.EntityMapper.toDomain
import com.tfg.data.local.mapper.EntityMapper.toEntity
import com.tfg.domain.model.Indicator
import com.tfg.domain.repository.IndicatorRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IndicatorRepositoryImpl @Inject constructor(
    private val indicatorDao: IndicatorDao
) : IndicatorRepository {

    override fun getAllIndicators(): Flow<List<Indicator>> =
        indicatorDao.getAll().map { list -> list.map { it.toDomain() } }

    override fun getEnabledIndicators(): Flow<List<Indicator>> =
        indicatorDao.getEnabled().map { list -> list.map { it.toDomain() } }

    override suspend fun getIndicatorById(id: String): Indicator? =
        indicatorDao.getById(id)?.toDomain()

    override suspend fun saveIndicator(indicator: Indicator) =
        indicatorDao.insert(indicator.toEntity())

    override suspend fun deleteIndicator(id: String) =
        indicatorDao.delete(id)

    override suspend fun setEnabled(id: String, enabled: Boolean) =
        indicatorDao.setEnabled(id, enabled)
}
