package com.tfg.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tfg.data.local.dao.*
import com.tfg.data.local.entity.*

@Database(
    entities = [
        OrderEntity::class,
        SignalEntity::class,
        TradingPairEntity::class,
        CandleEntity::class,
        AssetBalanceEntity::class,
        AuditLogEntity::class,
        FeeRecordEntity::class,
        DonationEntity::class,
        ScriptEntity::class,
        OfflineQueueEntity::class,
        SignalMarkerEntity::class,
        CustomTemplateEntity::class,
        AlertEntity::class,
        IndicatorEntity::class
    ],
    version = 9,
    exportSchema = true
)
abstract class TfgDatabase : RoomDatabase() {
    abstract fun orderDao(): OrderDao
    abstract fun signalDao(): SignalDao
    abstract fun tradingPairDao(): TradingPairDao
    abstract fun candleDao(): CandleDao
    abstract fun assetBalanceDao(): AssetBalanceDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun feeRecordDao(): FeeRecordDao
    abstract fun donationDao(): DonationDao
    abstract fun scriptDao(): ScriptDao
    abstract fun offlineQueueDao(): OfflineQueueDao
    abstract fun signalMarkerDao(): SignalMarkerDao
    abstract fun customTemplateDao(): CustomTemplateDao
    abstract fun alertDao(): AlertDao
    abstract fun indicatorDao(): IndicatorDao

    companion object {
        const val DATABASE_NAME = "tfg_database"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE scripts ADD COLUMN activeSymbol TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE scripts ADD COLUMN strategyTemplateId TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Bridge migration — no schema changes between v2 and v3
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS signal_markers (
                        id TEXT NOT NULL PRIMARY KEY,
                        scriptId TEXT NOT NULL,
                        symbol TEXT NOT NULL,
                        `interval` TEXT NOT NULL,
                        openTime INTEGER NOT NULL,
                        signalType TEXT NOT NULL,
                        price REAL NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE scripts ADD COLUMN paramsJson TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS custom_templates (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL,
                        baseTemplateId TEXT NOT NULL,
                        code TEXT NOT NULL,
                        defaultParamsJson TEXT DEFAULT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS alerts (
                        id TEXT NOT NULL PRIMARY KEY,
                        symbol TEXT NOT NULL,
                        name TEXT NOT NULL,
                        type TEXT NOT NULL,
                        condition TEXT NOT NULL,
                        targetValue REAL NOT NULL,
                        secondaryValue REAL,
                        `interval` TEXT NOT NULL DEFAULT '1h',
                        isEnabled INTEGER NOT NULL DEFAULT 1,
                        isRepeating INTEGER NOT NULL DEFAULT 0,
                        repeatIntervalSec INTEGER NOT NULL DEFAULT 60,
                        lastTriggeredAt INTEGER,
                        triggerCount INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS indicators (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        code TEXT NOT NULL,
                        isEnabled INTEGER NOT NULL DEFAULT 1,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Recreate candles with composite PK (symbol+interval+openTime)
                db.execSQL("CREATE TABLE IF NOT EXISTS candles_new (symbol TEXT NOT NULL, `interval` TEXT NOT NULL, openTime INTEGER NOT NULL, open REAL NOT NULL, high REAL NOT NULL, low REAL NOT NULL, close REAL NOT NULL, volume REAL NOT NULL, closeTime INTEGER NOT NULL, quoteVolume REAL NOT NULL DEFAULT 0.0, numberOfTrades INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(symbol, `interval`, openTime))")
                db.execSQL("INSERT OR REPLACE INTO candles_new (symbol, `interval`, openTime, open, high, low, close, volume, closeTime, quoteVolume, numberOfTrades) SELECT symbol, `interval`, openTime, open, high, low, close, volume, closeTime, quoteVolume, numberOfTrades FROM candles")
                db.execSQL("DROP TABLE candles")
                db.execSQL("ALTER TABLE candles_new RENAME TO candles")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_candles_symbol_interval ON candles(symbol, `interval`)")

                // 2. Add indices on orders
                db.execSQL("CREATE INDEX IF NOT EXISTS index_orders_status ON orders(status)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_orders_symbol ON orders(symbol)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_orders_signalId ON orders(signalId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_orders_closedAt ON orders(closedAt)")

                // 3. Add index on signals
                db.execSQL("CREATE INDEX IF NOT EXISTS index_signals_status ON signals(status)")
            }
        }
    }
}
