package com.tfg.di

import com.tfg.data.remote.repository.*
import com.tfg.domain.model.ScriptExecutor
import com.tfg.domain.model.IndicatorExecutor
import com.tfg.domain.repository.*
import com.tfg.engine.IndicatorEngine
import com.tfg.engine.RiskEngine
import com.tfg.engine.ScriptEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTradingRepository(impl: TradingRepositoryImpl): TradingRepository

    @Binds
    @Singleton
    abstract fun bindMarketRepository(impl: MarketRepositoryImpl): MarketRepository

    @Binds
    @Singleton
    abstract fun bindPortfolioRepository(impl: PortfolioRepositoryImpl): PortfolioRepository

    @Binds
    @Singleton
    abstract fun bindSignalRepository(impl: SignalRepositoryImpl): SignalRepository

    @Binds
    @Singleton
    abstract fun bindAuditRepository(impl: AuditRepositoryImpl): AuditRepository

    @Binds
    @Singleton
    abstract fun bindFeeRepository(impl: FeeRepositoryImpl): FeeRepository

    @Binds
    @Singleton
    abstract fun bindDonationRepository(impl: DonationRepositoryImpl): DonationRepository

    @Binds
    @Singleton
    abstract fun bindRiskRepository(impl: RiskEngine): RiskRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindScriptRepository(impl: ScriptRepositoryImpl): ScriptRepository

    @Binds
    @Singleton
    abstract fun bindOfflineQueueRepository(impl: OfflineQueueRepositoryImpl): OfflineQueueRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindAnalyticsRepository(impl: AnalyticsRepositoryImpl): AnalyticsRepository

    @Binds
    @Singleton
    abstract fun bindScriptExecutor(impl: ScriptEngine): ScriptExecutor

    @Binds
    @Singleton
    abstract fun bindAlertRepository(impl: AlertRepositoryImpl): AlertRepository

    @Binds
    @Singleton
    abstract fun bindIndicatorRepository(impl: IndicatorRepositoryImpl): IndicatorRepository

    @Binds
    @Singleton
    abstract fun bindIndicatorExecutor(impl: IndicatorEngine): IndicatorExecutor
}
