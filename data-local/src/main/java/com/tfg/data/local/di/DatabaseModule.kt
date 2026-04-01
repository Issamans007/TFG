package com.tfg.data.local.di

import android.content.Context
import androidx.room.Room
import com.tfg.data.local.dao.*
import com.tfg.data.local.db.TfgDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TfgDatabase =
        Room.databaseBuilder(context, TfgDatabase::class.java, TfgDatabase.DATABASE_NAME)
            .addMigrations(
                TfgDatabase.MIGRATION_1_2, TfgDatabase.MIGRATION_2_3,
                TfgDatabase.MIGRATION_3_4, TfgDatabase.MIGRATION_4_5,
                TfgDatabase.MIGRATION_5_6, TfgDatabase.MIGRATION_6_7,
                TfgDatabase.MIGRATION_7_8, TfgDatabase.MIGRATION_8_9
            )
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideOrderDao(db: TfgDatabase): OrderDao = db.orderDao()
    @Provides fun provideSignalDao(db: TfgDatabase): SignalDao = db.signalDao()
    @Provides fun provideTradingPairDao(db: TfgDatabase): TradingPairDao = db.tradingPairDao()
    @Provides fun provideCandleDao(db: TfgDatabase): CandleDao = db.candleDao()
    @Provides fun provideAssetBalanceDao(db: TfgDatabase): AssetBalanceDao = db.assetBalanceDao()
    @Provides fun provideAuditLogDao(db: TfgDatabase): AuditLogDao = db.auditLogDao()
    @Provides fun provideFeeRecordDao(db: TfgDatabase): FeeRecordDao = db.feeRecordDao()
    @Provides fun provideDonationDao(db: TfgDatabase): DonationDao = db.donationDao()
    @Provides fun provideScriptDao(db: TfgDatabase): ScriptDao = db.scriptDao()
    @Provides fun provideOfflineQueueDao(db: TfgDatabase): OfflineQueueDao = db.offlineQueueDao()
    @Provides fun provideSignalMarkerDao(db: TfgDatabase): SignalMarkerDao = db.signalMarkerDao()
    @Provides fun provideCustomTemplateDao(db: TfgDatabase): CustomTemplateDao = db.customTemplateDao()
    @Provides fun provideAlertDao(db: TfgDatabase): AlertDao = db.alertDao()
    @Provides fun provideIndicatorDao(db: TfgDatabase): IndicatorDao = db.indicatorDao()
}
