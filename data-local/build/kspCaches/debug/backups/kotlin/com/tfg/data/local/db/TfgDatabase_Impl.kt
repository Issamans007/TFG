package com.tfg.`data`.local.db

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.tfg.`data`.local.dao.AlertDao
import com.tfg.`data`.local.dao.AlertDao_Impl
import com.tfg.`data`.local.dao.AssetBalanceDao
import com.tfg.`data`.local.dao.AssetBalanceDao_Impl
import com.tfg.`data`.local.dao.AuditLogDao
import com.tfg.`data`.local.dao.AuditLogDao_Impl
import com.tfg.`data`.local.dao.CandleDao
import com.tfg.`data`.local.dao.CandleDao_Impl
import com.tfg.`data`.local.dao.CustomTemplateDao
import com.tfg.`data`.local.dao.CustomTemplateDao_Impl
import com.tfg.`data`.local.dao.DonationDao
import com.tfg.`data`.local.dao.DonationDao_Impl
import com.tfg.`data`.local.dao.DrawingSnapshotDao
import com.tfg.`data`.local.dao.DrawingSnapshotDao_Impl
import com.tfg.`data`.local.dao.FeeRecordDao
import com.tfg.`data`.local.dao.FeeRecordDao_Impl
import com.tfg.`data`.local.dao.IndicatorDao
import com.tfg.`data`.local.dao.IndicatorDao_Impl
import com.tfg.`data`.local.dao.OfflineQueueDao
import com.tfg.`data`.local.dao.OfflineQueueDao_Impl
import com.tfg.`data`.local.dao.OrderDao
import com.tfg.`data`.local.dao.OrderDao_Impl
import com.tfg.`data`.local.dao.ScriptDao
import com.tfg.`data`.local.dao.ScriptDao_Impl
import com.tfg.`data`.local.dao.SignalDao
import com.tfg.`data`.local.dao.SignalDao_Impl
import com.tfg.`data`.local.dao.SignalMarkerDao
import com.tfg.`data`.local.dao.SignalMarkerDao_Impl
import com.tfg.`data`.local.dao.TradingPairDao
import com.tfg.`data`.local.dao.TradingPairDao_Impl
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class TfgDatabase_Impl : TfgDatabase() {
  private val _orderDao: Lazy<OrderDao> = lazy {
    OrderDao_Impl(this)
  }

  private val _signalDao: Lazy<SignalDao> = lazy {
    SignalDao_Impl(this)
  }

  private val _tradingPairDao: Lazy<TradingPairDao> = lazy {
    TradingPairDao_Impl(this)
  }

  private val _candleDao: Lazy<CandleDao> = lazy {
    CandleDao_Impl(this)
  }

  private val _assetBalanceDao: Lazy<AssetBalanceDao> = lazy {
    AssetBalanceDao_Impl(this)
  }

  private val _auditLogDao: Lazy<AuditLogDao> = lazy {
    AuditLogDao_Impl(this)
  }

  private val _feeRecordDao: Lazy<FeeRecordDao> = lazy {
    FeeRecordDao_Impl(this)
  }

  private val _donationDao: Lazy<DonationDao> = lazy {
    DonationDao_Impl(this)
  }

  private val _scriptDao: Lazy<ScriptDao> = lazy {
    ScriptDao_Impl(this)
  }

  private val _offlineQueueDao: Lazy<OfflineQueueDao> = lazy {
    OfflineQueueDao_Impl(this)
  }

  private val _signalMarkerDao: Lazy<SignalMarkerDao> = lazy {
    SignalMarkerDao_Impl(this)
  }

  private val _customTemplateDao: Lazy<CustomTemplateDao> = lazy {
    CustomTemplateDao_Impl(this)
  }

  private val _alertDao: Lazy<AlertDao> = lazy {
    AlertDao_Impl(this)
  }

  private val _indicatorDao: Lazy<IndicatorDao> = lazy {
    IndicatorDao_Impl(this)
  }

  private val _drawingSnapshotDao: Lazy<DrawingSnapshotDao> = lazy {
    DrawingSnapshotDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(13,
        "38c51a439111f84d71da0fb8f3f6a9de", "ad2db7d21b6e138c4f9739865fd94ef8") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `orders` (`id` TEXT NOT NULL, `signalId` TEXT, `symbol` TEXT NOT NULL, `side` TEXT NOT NULL, `type` TEXT NOT NULL, `status` TEXT NOT NULL, `executionMode` TEXT NOT NULL, `quantity` REAL NOT NULL, `price` REAL, `stopPrice` REAL, `takeProfitsJson` TEXT NOT NULL, `stopLossesJson` TEXT NOT NULL, `trailingStopPercent` REAL, `trailingStopActivationPrice` REAL, `ocoLinkedOrderId` TEXT, `bracketParentId` TEXT, `timeInForce` TEXT NOT NULL, `scheduledAt` INTEGER, `filledQuantity` REAL NOT NULL, `filledPrice` REAL NOT NULL, `fee` REAL NOT NULL, `feeAsset` TEXT NOT NULL, `donationAmount` REAL NOT NULL, `realizedPnl` REAL NOT NULL, `slippage` REAL NOT NULL, `isPaperTrade` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `executedAt` INTEGER, `closedAt` INTEGER, `binanceOrderId` INTEGER, `errorMessage` TEXT, `marketType` TEXT NOT NULL, `leverage` INTEGER NOT NULL, `marginType` TEXT NOT NULL, `positionSide` TEXT NOT NULL, `reduceOnly` INTEGER NOT NULL, `closePosition` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_orders_status` ON `orders` (`status`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_orders_symbol` ON `orders` (`symbol`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_orders_signalId` ON `orders` (`signalId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_orders_closedAt` ON `orders` (`closedAt`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `signals` (`id` TEXT NOT NULL, `symbol` TEXT NOT NULL, `side` TEXT NOT NULL, `entryPrice` REAL NOT NULL, `takeProfitsJson` TEXT NOT NULL, `stopLossesJson` TEXT NOT NULL, `confidence` REAL NOT NULL, `riskRewardRatio` REAL NOT NULL, `expiresAt` INTEGER NOT NULL, `receivedAt` INTEGER NOT NULL, `status` TEXT NOT NULL, `hmacSignature` TEXT NOT NULL, `isExpired` INTEGER NOT NULL, `wasExecuted` INTEGER NOT NULL, `missedWhileOffline` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_signals_status` ON `signals` (`status`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `trading_pairs` (`symbol` TEXT NOT NULL, `baseAsset` TEXT NOT NULL, `quoteAsset` TEXT NOT NULL, `lastPrice` REAL NOT NULL, `priceChangePercent24h` REAL NOT NULL, `volume24h` REAL NOT NULL, `high24h` REAL NOT NULL, `low24h` REAL NOT NULL, `isWatchlisted` INTEGER NOT NULL, `isActiveForTrading` INTEGER NOT NULL, `minQty` REAL NOT NULL, `stepSize` REAL NOT NULL, `tickSize` REAL NOT NULL, `minNotional` REAL NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`symbol`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `candles` (`symbol` TEXT NOT NULL, `interval` TEXT NOT NULL, `openTime` INTEGER NOT NULL, `open` REAL NOT NULL, `high` REAL NOT NULL, `low` REAL NOT NULL, `close` REAL NOT NULL, `volume` REAL NOT NULL, `closeTime` INTEGER NOT NULL, `quoteVolume` REAL NOT NULL, `numberOfTrades` INTEGER NOT NULL, PRIMARY KEY(`symbol`, `interval`, `openTime`))")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_candles_symbol_interval` ON `candles` (`symbol`, `interval`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `asset_balances` (`asset` TEXT NOT NULL, `free` REAL NOT NULL, `locked` REAL NOT NULL, `usdValue` REAL NOT NULL, `allocationPercent` REAL NOT NULL, `walletType` TEXT NOT NULL, `isPaper` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`asset`, `walletType`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `audit_logs` (`id` TEXT NOT NULL, `action` TEXT NOT NULL, `category` TEXT NOT NULL, `details` TEXT NOT NULL, `oldValue` TEXT, `newValue` TEXT, `orderId` TEXT, `symbol` TEXT, `userId` TEXT NOT NULL, `ipAddress` TEXT, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `fee_records` (`id` TEXT NOT NULL, `orderId` TEXT NOT NULL, `symbol` TEXT NOT NULL, `feeAmount` REAL NOT NULL, `feeAsset` TEXT NOT NULL, `feeType` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `donations` (`id` TEXT NOT NULL, `orderId` TEXT NOT NULL, `amount` REAL NOT NULL, `currency` TEXT NOT NULL, `ngoName` TEXT NOT NULL, `ngoId` TEXT NOT NULL, `status` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `scripts` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `code` TEXT NOT NULL, `isActive` INTEGER NOT NULL, `activeSymbol` TEXT, `strategyTemplateId` TEXT, `paramsJson` TEXT, `relatedSymbolsJson` TEXT, `lastRun` INTEGER, `backtestResultJson` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `offline_queue` (`id` TEXT NOT NULL, `signalJson` TEXT, `orderJson` TEXT, `action` TEXT NOT NULL, `priority` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `retryCount` INTEGER NOT NULL, `maxRetries` INTEGER NOT NULL, `lastError` TEXT, `isProcessing` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `signal_markers` (`id` TEXT NOT NULL, `scriptId` TEXT NOT NULL, `symbol` TEXT NOT NULL, `interval` TEXT NOT NULL, `openTime` INTEGER NOT NULL, `signalType` TEXT NOT NULL, `price` REAL NOT NULL, `label` TEXT NOT NULL, `orderType` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `custom_templates` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `description` TEXT NOT NULL, `baseTemplateId` TEXT NOT NULL, `code` TEXT NOT NULL, `defaultParamsJson` TEXT, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `alerts` (`id` TEXT NOT NULL, `symbol` TEXT NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `condition` TEXT NOT NULL, `targetValue` REAL NOT NULL, `secondaryValue` REAL, `interval` TEXT NOT NULL, `isEnabled` INTEGER NOT NULL, `isRepeating` INTEGER NOT NULL, `repeatIntervalSec` INTEGER NOT NULL, `lastTriggeredAt` INTEGER, `triggerCount` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `indicators` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `code` TEXT NOT NULL, `isEnabled` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `chart_drawings_snapshot` (`symbol` TEXT NOT NULL, `drawingsJson` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`symbol`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '38c51a439111f84d71da0fb8f3f6a9de')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `orders`")
        connection.execSQL("DROP TABLE IF EXISTS `signals`")
        connection.execSQL("DROP TABLE IF EXISTS `trading_pairs`")
        connection.execSQL("DROP TABLE IF EXISTS `candles`")
        connection.execSQL("DROP TABLE IF EXISTS `asset_balances`")
        connection.execSQL("DROP TABLE IF EXISTS `audit_logs`")
        connection.execSQL("DROP TABLE IF EXISTS `fee_records`")
        connection.execSQL("DROP TABLE IF EXISTS `donations`")
        connection.execSQL("DROP TABLE IF EXISTS `scripts`")
        connection.execSQL("DROP TABLE IF EXISTS `offline_queue`")
        connection.execSQL("DROP TABLE IF EXISTS `signal_markers`")
        connection.execSQL("DROP TABLE IF EXISTS `custom_templates`")
        connection.execSQL("DROP TABLE IF EXISTS `alerts`")
        connection.execSQL("DROP TABLE IF EXISTS `indicators`")
        connection.execSQL("DROP TABLE IF EXISTS `chart_drawings_snapshot`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection):
          RoomOpenDelegate.ValidationResult {
        val _columnsOrders: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsOrders.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsOrders.put("signalId", TableInfo.Column("signalId", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsOrders.put("symbol", TableInfo.Column("symbol", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsOrders.put("side", TableInfo.Column("side", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsOrders.put("type", TableInfo.Column("type", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsOrders.put("status", TableInfo.Column("status", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsOrders.put("executionMode", TableInfo.Column("executionMode", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsOrders.put("quantity", TableInfo.Column("quantity", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsOrders.put("price", TableInfo.Column("price", "REAL", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsOrders.put("stopPrice", TableInfo.Column("stopPrice", "REAL", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsOrders.put("takeProfitsJson", TableInfo.Column("takeProfitsJson", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsOrders.put("stopLossesJson", TableInfo.Column("stopLossesJson", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsOrders.put("trailingStopPercent", TableInfo.Column("trailingStopPercent", "REAL",
            false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsOrders.put("trailingStopActivationPrice",
            TableInfo.Column("trailingStopActivationPrice", "REAL", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsOrders.put("ocoLinkedOrderId", TableInfo.Column("ocoLinkedOrderId", "TEXT", false,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsOrders.put("bracketParentId", TableInfo.Column("bracketParentId", "TEXT", false, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsOrders.put("timeInForce", TableInfo.Column("timeInForce", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsOrders.put("scheduledAt", TableInfo.Column("scheduledAt", "INTEGER", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsOrders.put("filledQuantity", TableInfo.Column("filledQuantity", "REAL", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsOrders.put("filledPrice", TableInfo.Column("filledPrice", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsOrders.put("fee", TableInfo.Column("fee", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsOrders.put("feeAsset", TableInfo.Column("feeAsset", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsOrders.put("donationAmount", TableInfo.Column("donationAmount", "REAL", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsOrders.put("realizedPnl", TableInfo.Column("realizedPnl", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsOrders.put("slippage", TableInfo.Column("slippage", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsOrders.put("isPaperTrade", TableInfo.Column("isPaperTrade", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsOrders.put("createdAt", TableInfo.Column("createdAt", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsOrders.put("updatedAt", TableInfo.Column("updatedAt", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsOrders.put("executedAt", TableInfo.Column("executedAt", "INTEGER", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsOrders.put("closedAt", TableInfo.Column("closedAt", "INTEGER", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsOrders.put("binanceOrderId", TableInfo.Column("binanceOrderId", "INTEGER", false, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsOrders.put("errorMessage", TableInfo.Column("errorMessage", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsOrders.put("marketType", TableInfo.Column("marketType", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsOrders.put("leverage", TableInfo.Column("leverage", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsOrders.put("marginType", TableInfo.Column("marginType", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsOrders.put("positionSide", TableInfo.Column("positionSide", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsOrders.put("reduceOnly", TableInfo.Column("reduceOnly", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsOrders.put("closePosition", TableInfo.Column("closePosition", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysOrders: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesOrders: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesOrders.add(TableInfo.Index("index_orders_status", false, listOf("status"),
            listOf("ASC")))
        _indicesOrders.add(TableInfo.Index("index_orders_symbol", false, listOf("symbol"),
            listOf("ASC")))
        _indicesOrders.add(TableInfo.Index("index_orders_signalId", false, listOf("signalId"),
            listOf("ASC")))
        _indicesOrders.add(TableInfo.Index("index_orders_closedAt", false, listOf("closedAt"),
            listOf("ASC")))
        val _infoOrders: TableInfo = TableInfo("orders", _columnsOrders, _foreignKeysOrders,
            _indicesOrders)
        val _existingOrders: TableInfo = read(connection, "orders")
        if (!_infoOrders.equals(_existingOrders)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |orders(com.tfg.data.local.entity.OrderEntity).
              | Expected:
              |""".trimMargin() + _infoOrders + """
              |
              | Found:
              |""".trimMargin() + _existingOrders)
        }
        val _columnsSignals: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsSignals.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsSignals.put("symbol", TableInfo.Column("symbol", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsSignals.put("side", TableInfo.Column("side", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsSignals.put("entryPrice", TableInfo.Column("entryPrice", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsSignals.put("takeProfitsJson", TableInfo.Column("takeProfitsJson", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSignals.put("stopLossesJson", TableInfo.Column("stopLossesJson", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSignals.put("confidence", TableInfo.Column("confidence", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsSignals.put("riskRewardRatio", TableInfo.Column("riskRewardRatio", "REAL", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSignals.put("expiresAt", TableInfo.Column("expiresAt", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsSignals.put("receivedAt", TableInfo.Column("receivedAt", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsSignals.put("status", TableInfo.Column("status", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsSignals.put("hmacSignature", TableInfo.Column("hmacSignature", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSignals.put("isExpired", TableInfo.Column("isExpired", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsSignals.put("wasExecuted", TableInfo.Column("wasExecuted", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsSignals.put("missedWhileOffline", TableInfo.Column("missedWhileOffline", "INTEGER",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysSignals: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesSignals: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesSignals.add(TableInfo.Index("index_signals_status", false, listOf("status"),
            listOf("ASC")))
        val _infoSignals: TableInfo = TableInfo("signals", _columnsSignals, _foreignKeysSignals,
            _indicesSignals)
        val _existingSignals: TableInfo = read(connection, "signals")
        if (!_infoSignals.equals(_existingSignals)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |signals(com.tfg.data.local.entity.SignalEntity).
              | Expected:
              |""".trimMargin() + _infoSignals + """
              |
              | Found:
              |""".trimMargin() + _existingSignals)
        }
        val _columnsTradingPairs: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsTradingPairs.put("symbol", TableInfo.Column("symbol", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTradingPairs.put("baseAsset", TableInfo.Column("baseAsset", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTradingPairs.put("quoteAsset", TableInfo.Column("quoteAsset", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTradingPairs.put("lastPrice", TableInfo.Column("lastPrice", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTradingPairs.put("priceChangePercent24h", TableInfo.Column("priceChangePercent24h",
            "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTradingPairs.put("volume24h", TableInfo.Column("volume24h", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTradingPairs.put("high24h", TableInfo.Column("high24h", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTradingPairs.put("low24h", TableInfo.Column("low24h", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTradingPairs.put("isWatchlisted", TableInfo.Column("isWatchlisted", "INTEGER", true,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTradingPairs.put("isActiveForTrading", TableInfo.Column("isActiveForTrading",
            "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTradingPairs.put("minQty", TableInfo.Column("minQty", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTradingPairs.put("stepSize", TableInfo.Column("stepSize", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTradingPairs.put("tickSize", TableInfo.Column("tickSize", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTradingPairs.put("minNotional", TableInfo.Column("minNotional", "REAL", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTradingPairs.put("updatedAt", TableInfo.Column("updatedAt", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysTradingPairs: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesTradingPairs: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoTradingPairs: TableInfo = TableInfo("trading_pairs", _columnsTradingPairs,
            _foreignKeysTradingPairs, _indicesTradingPairs)
        val _existingTradingPairs: TableInfo = read(connection, "trading_pairs")
        if (!_infoTradingPairs.equals(_existingTradingPairs)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |trading_pairs(com.tfg.data.local.entity.TradingPairEntity).
              | Expected:
              |""".trimMargin() + _infoTradingPairs + """
              |
              | Found:
              |""".trimMargin() + _existingTradingPairs)
        }
        val _columnsCandles: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsCandles.put("symbol", TableInfo.Column("symbol", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsCandles.put("interval", TableInfo.Column("interval", "TEXT", true, 2, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsCandles.put("openTime", TableInfo.Column("openTime", "INTEGER", true, 3, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsCandles.put("open", TableInfo.Column("open", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsCandles.put("high", TableInfo.Column("high", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsCandles.put("low", TableInfo.Column("low", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsCandles.put("close", TableInfo.Column("close", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsCandles.put("volume", TableInfo.Column("volume", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsCandles.put("closeTime", TableInfo.Column("closeTime", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsCandles.put("quoteVolume", TableInfo.Column("quoteVolume", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsCandles.put("numberOfTrades", TableInfo.Column("numberOfTrades", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysCandles: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesCandles: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesCandles.add(TableInfo.Index("index_candles_symbol_interval", false, listOf("symbol",
            "interval"), listOf("ASC", "ASC")))
        val _infoCandles: TableInfo = TableInfo("candles", _columnsCandles, _foreignKeysCandles,
            _indicesCandles)
        val _existingCandles: TableInfo = read(connection, "candles")
        if (!_infoCandles.equals(_existingCandles)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |candles(com.tfg.data.local.entity.CandleEntity).
              | Expected:
              |""".trimMargin() + _infoCandles + """
              |
              | Found:
              |""".trimMargin() + _existingCandles)
        }
        val _columnsAssetBalances: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsAssetBalances.put("asset", TableInfo.Column("asset", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsAssetBalances.put("free", TableInfo.Column("free", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsAssetBalances.put("locked", TableInfo.Column("locked", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsAssetBalances.put("usdValue", TableInfo.Column("usdValue", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsAssetBalances.put("allocationPercent", TableInfo.Column("allocationPercent", "REAL",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsAssetBalances.put("walletType", TableInfo.Column("walletType", "TEXT", true, 2,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsAssetBalances.put("isPaper", TableInfo.Column("isPaper", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsAssetBalances.put("updatedAt", TableInfo.Column("updatedAt", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysAssetBalances: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesAssetBalances: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoAssetBalances: TableInfo = TableInfo("asset_balances", _columnsAssetBalances,
            _foreignKeysAssetBalances, _indicesAssetBalances)
        val _existingAssetBalances: TableInfo = read(connection, "asset_balances")
        if (!_infoAssetBalances.equals(_existingAssetBalances)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |asset_balances(com.tfg.data.local.entity.AssetBalanceEntity).
              | Expected:
              |""".trimMargin() + _infoAssetBalances + """
              |
              | Found:
              |""".trimMargin() + _existingAssetBalances)
        }
        val _columnsAuditLogs: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsAuditLogs.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsAuditLogs.put("action", TableInfo.Column("action", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsAuditLogs.put("category", TableInfo.Column("category", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsAuditLogs.put("details", TableInfo.Column("details", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsAuditLogs.put("oldValue", TableInfo.Column("oldValue", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsAuditLogs.put("newValue", TableInfo.Column("newValue", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsAuditLogs.put("orderId", TableInfo.Column("orderId", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsAuditLogs.put("symbol", TableInfo.Column("symbol", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsAuditLogs.put("userId", TableInfo.Column("userId", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsAuditLogs.put("ipAddress", TableInfo.Column("ipAddress", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsAuditLogs.put("timestamp", TableInfo.Column("timestamp", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysAuditLogs: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesAuditLogs: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoAuditLogs: TableInfo = TableInfo("audit_logs", _columnsAuditLogs,
            _foreignKeysAuditLogs, _indicesAuditLogs)
        val _existingAuditLogs: TableInfo = read(connection, "audit_logs")
        if (!_infoAuditLogs.equals(_existingAuditLogs)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |audit_logs(com.tfg.data.local.entity.AuditLogEntity).
              | Expected:
              |""".trimMargin() + _infoAuditLogs + """
              |
              | Found:
              |""".trimMargin() + _existingAuditLogs)
        }
        val _columnsFeeRecords: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsFeeRecords.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsFeeRecords.put("orderId", TableInfo.Column("orderId", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsFeeRecords.put("symbol", TableInfo.Column("symbol", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsFeeRecords.put("feeAmount", TableInfo.Column("feeAmount", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsFeeRecords.put("feeAsset", TableInfo.Column("feeAsset", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsFeeRecords.put("feeType", TableInfo.Column("feeType", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsFeeRecords.put("timestamp", TableInfo.Column("timestamp", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysFeeRecords: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesFeeRecords: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoFeeRecords: TableInfo = TableInfo("fee_records", _columnsFeeRecords,
            _foreignKeysFeeRecords, _indicesFeeRecords)
        val _existingFeeRecords: TableInfo = read(connection, "fee_records")
        if (!_infoFeeRecords.equals(_existingFeeRecords)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |fee_records(com.tfg.data.local.entity.FeeRecordEntity).
              | Expected:
              |""".trimMargin() + _infoFeeRecords + """
              |
              | Found:
              |""".trimMargin() + _existingFeeRecords)
        }
        val _columnsDonations: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsDonations.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsDonations.put("orderId", TableInfo.Column("orderId", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsDonations.put("amount", TableInfo.Column("amount", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsDonations.put("currency", TableInfo.Column("currency", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsDonations.put("ngoName", TableInfo.Column("ngoName", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsDonations.put("ngoId", TableInfo.Column("ngoId", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsDonations.put("status", TableInfo.Column("status", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsDonations.put("timestamp", TableInfo.Column("timestamp", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysDonations: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesDonations: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoDonations: TableInfo = TableInfo("donations", _columnsDonations,
            _foreignKeysDonations, _indicesDonations)
        val _existingDonations: TableInfo = read(connection, "donations")
        if (!_infoDonations.equals(_existingDonations)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |donations(com.tfg.data.local.entity.DonationEntity).
              | Expected:
              |""".trimMargin() + _infoDonations + """
              |
              | Found:
              |""".trimMargin() + _existingDonations)
        }
        val _columnsScripts: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsScripts.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsScripts.put("name", TableInfo.Column("name", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsScripts.put("code", TableInfo.Column("code", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsScripts.put("isActive", TableInfo.Column("isActive", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsScripts.put("activeSymbol", TableInfo.Column("activeSymbol", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsScripts.put("strategyTemplateId", TableInfo.Column("strategyTemplateId", "TEXT",
            false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsScripts.put("paramsJson", TableInfo.Column("paramsJson", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsScripts.put("relatedSymbolsJson", TableInfo.Column("relatedSymbolsJson", "TEXT",
            false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsScripts.put("lastRun", TableInfo.Column("lastRun", "INTEGER", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsScripts.put("backtestResultJson", TableInfo.Column("backtestResultJson", "TEXT",
            false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsScripts.put("createdAt", TableInfo.Column("createdAt", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsScripts.put("updatedAt", TableInfo.Column("updatedAt", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysScripts: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesScripts: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoScripts: TableInfo = TableInfo("scripts", _columnsScripts, _foreignKeysScripts,
            _indicesScripts)
        val _existingScripts: TableInfo = read(connection, "scripts")
        if (!_infoScripts.equals(_existingScripts)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |scripts(com.tfg.data.local.entity.ScriptEntity).
              | Expected:
              |""".trimMargin() + _infoScripts + """
              |
              | Found:
              |""".trimMargin() + _existingScripts)
        }
        val _columnsOfflineQueue: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsOfflineQueue.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsOfflineQueue.put("signalJson", TableInfo.Column("signalJson", "TEXT", false, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsOfflineQueue.put("orderJson", TableInfo.Column("orderJson", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsOfflineQueue.put("action", TableInfo.Column("action", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsOfflineQueue.put("priority", TableInfo.Column("priority", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsOfflineQueue.put("createdAt", TableInfo.Column("createdAt", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsOfflineQueue.put("retryCount", TableInfo.Column("retryCount", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsOfflineQueue.put("maxRetries", TableInfo.Column("maxRetries", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsOfflineQueue.put("lastError", TableInfo.Column("lastError", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsOfflineQueue.put("isProcessing", TableInfo.Column("isProcessing", "INTEGER", true,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysOfflineQueue: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesOfflineQueue: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoOfflineQueue: TableInfo = TableInfo("offline_queue", _columnsOfflineQueue,
            _foreignKeysOfflineQueue, _indicesOfflineQueue)
        val _existingOfflineQueue: TableInfo = read(connection, "offline_queue")
        if (!_infoOfflineQueue.equals(_existingOfflineQueue)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |offline_queue(com.tfg.data.local.entity.OfflineQueueEntity).
              | Expected:
              |""".trimMargin() + _infoOfflineQueue + """
              |
              | Found:
              |""".trimMargin() + _existingOfflineQueue)
        }
        val _columnsSignalMarkers: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsSignalMarkers.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsSignalMarkers.put("scriptId", TableInfo.Column("scriptId", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsSignalMarkers.put("symbol", TableInfo.Column("symbol", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsSignalMarkers.put("interval", TableInfo.Column("interval", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsSignalMarkers.put("openTime", TableInfo.Column("openTime", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsSignalMarkers.put("signalType", TableInfo.Column("signalType", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSignalMarkers.put("price", TableInfo.Column("price", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsSignalMarkers.put("label", TableInfo.Column("label", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsSignalMarkers.put("orderType", TableInfo.Column("orderType", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsSignalMarkers.put("timestamp", TableInfo.Column("timestamp", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysSignalMarkers: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesSignalMarkers: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoSignalMarkers: TableInfo = TableInfo("signal_markers", _columnsSignalMarkers,
            _foreignKeysSignalMarkers, _indicesSignalMarkers)
        val _existingSignalMarkers: TableInfo = read(connection, "signal_markers")
        if (!_infoSignalMarkers.equals(_existingSignalMarkers)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |signal_markers(com.tfg.data.local.entity.SignalMarkerEntity).
              | Expected:
              |""".trimMargin() + _infoSignalMarkers + """
              |
              | Found:
              |""".trimMargin() + _existingSignalMarkers)
        }
        val _columnsCustomTemplates: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsCustomTemplates.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsCustomTemplates.put("name", TableInfo.Column("name", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsCustomTemplates.put("description", TableInfo.Column("description", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCustomTemplates.put("baseTemplateId", TableInfo.Column("baseTemplateId", "TEXT",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCustomTemplates.put("code", TableInfo.Column("code", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsCustomTemplates.put("defaultParamsJson", TableInfo.Column("defaultParamsJson",
            "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCustomTemplates.put("createdAt", TableInfo.Column("createdAt", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysCustomTemplates: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesCustomTemplates: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoCustomTemplates: TableInfo = TableInfo("custom_templates", _columnsCustomTemplates,
            _foreignKeysCustomTemplates, _indicesCustomTemplates)
        val _existingCustomTemplates: TableInfo = read(connection, "custom_templates")
        if (!_infoCustomTemplates.equals(_existingCustomTemplates)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |custom_templates(com.tfg.data.local.entity.CustomTemplateEntity).
              | Expected:
              |""".trimMargin() + _infoCustomTemplates + """
              |
              | Found:
              |""".trimMargin() + _existingCustomTemplates)
        }
        val _columnsAlerts: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsAlerts.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsAlerts.put("symbol", TableInfo.Column("symbol", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsAlerts.put("name", TableInfo.Column("name", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsAlerts.put("type", TableInfo.Column("type", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsAlerts.put("condition", TableInfo.Column("condition", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsAlerts.put("targetValue", TableInfo.Column("targetValue", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsAlerts.put("secondaryValue", TableInfo.Column("secondaryValue", "REAL", false, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsAlerts.put("interval", TableInfo.Column("interval", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsAlerts.put("isEnabled", TableInfo.Column("isEnabled", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsAlerts.put("isRepeating", TableInfo.Column("isRepeating", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsAlerts.put("repeatIntervalSec", TableInfo.Column("repeatIntervalSec", "INTEGER",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsAlerts.put("lastTriggeredAt", TableInfo.Column("lastTriggeredAt", "INTEGER", false,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsAlerts.put("triggerCount", TableInfo.Column("triggerCount", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsAlerts.put("createdAt", TableInfo.Column("createdAt", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsAlerts.put("updatedAt", TableInfo.Column("updatedAt", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysAlerts: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesAlerts: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoAlerts: TableInfo = TableInfo("alerts", _columnsAlerts, _foreignKeysAlerts,
            _indicesAlerts)
        val _existingAlerts: TableInfo = read(connection, "alerts")
        if (!_infoAlerts.equals(_existingAlerts)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |alerts(com.tfg.data.local.entity.AlertEntity).
              | Expected:
              |""".trimMargin() + _infoAlerts + """
              |
              | Found:
              |""".trimMargin() + _existingAlerts)
        }
        val _columnsIndicators: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsIndicators.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsIndicators.put("name", TableInfo.Column("name", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsIndicators.put("code", TableInfo.Column("code", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsIndicators.put("isEnabled", TableInfo.Column("isEnabled", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsIndicators.put("createdAt", TableInfo.Column("createdAt", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsIndicators.put("updatedAt", TableInfo.Column("updatedAt", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysIndicators: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesIndicators: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoIndicators: TableInfo = TableInfo("indicators", _columnsIndicators,
            _foreignKeysIndicators, _indicesIndicators)
        val _existingIndicators: TableInfo = read(connection, "indicators")
        if (!_infoIndicators.equals(_existingIndicators)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |indicators(com.tfg.data.local.entity.IndicatorEntity).
              | Expected:
              |""".trimMargin() + _infoIndicators + """
              |
              | Found:
              |""".trimMargin() + _existingIndicators)
        }
        val _columnsChartDrawingsSnapshot: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsChartDrawingsSnapshot.put("symbol", TableInfo.Column("symbol", "TEXT", true, 1,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsChartDrawingsSnapshot.put("drawingsJson", TableInfo.Column("drawingsJson", "TEXT",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsChartDrawingsSnapshot.put("updatedAt", TableInfo.Column("updatedAt", "INTEGER",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysChartDrawingsSnapshot: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesChartDrawingsSnapshot: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoChartDrawingsSnapshot: TableInfo = TableInfo("chart_drawings_snapshot",
            _columnsChartDrawingsSnapshot, _foreignKeysChartDrawingsSnapshot,
            _indicesChartDrawingsSnapshot)
        val _existingChartDrawingsSnapshot: TableInfo = read(connection, "chart_drawings_snapshot")
        if (!_infoChartDrawingsSnapshot.equals(_existingChartDrawingsSnapshot)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |chart_drawings_snapshot(com.tfg.data.local.entity.DrawingSnapshotEntity).
              | Expected:
              |""".trimMargin() + _infoChartDrawingsSnapshot + """
              |
              | Found:
              |""".trimMargin() + _existingChartDrawingsSnapshot)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "orders", "signals",
        "trading_pairs", "candles", "asset_balances", "audit_logs", "fee_records", "donations",
        "scripts", "offline_queue", "signal_markers", "custom_templates", "alerts", "indicators",
        "chart_drawings_snapshot")
  }

  public override fun clearAllTables() {
    super.performClear(false, "orders", "signals", "trading_pairs", "candles", "asset_balances",
        "audit_logs", "fee_records", "donations", "scripts", "offline_queue", "signal_markers",
        "custom_templates", "alerts", "indicators", "chart_drawings_snapshot")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(OrderDao::class, OrderDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(SignalDao::class, SignalDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(TradingPairDao::class, TradingPairDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(CandleDao::class, CandleDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(AssetBalanceDao::class, AssetBalanceDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(AuditLogDao::class, AuditLogDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(FeeRecordDao::class, FeeRecordDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(DonationDao::class, DonationDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(ScriptDao::class, ScriptDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(OfflineQueueDao::class, OfflineQueueDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(SignalMarkerDao::class, SignalMarkerDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(CustomTemplateDao::class, CustomTemplateDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(AlertDao::class, AlertDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(IndicatorDao::class, IndicatorDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(DrawingSnapshotDao::class,
        DrawingSnapshotDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override
      fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>):
      List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun orderDao(): OrderDao = _orderDao.value

  public override fun signalDao(): SignalDao = _signalDao.value

  public override fun tradingPairDao(): TradingPairDao = _tradingPairDao.value

  public override fun candleDao(): CandleDao = _candleDao.value

  public override fun assetBalanceDao(): AssetBalanceDao = _assetBalanceDao.value

  public override fun auditLogDao(): AuditLogDao = _auditLogDao.value

  public override fun feeRecordDao(): FeeRecordDao = _feeRecordDao.value

  public override fun donationDao(): DonationDao = _donationDao.value

  public override fun scriptDao(): ScriptDao = _scriptDao.value

  public override fun offlineQueueDao(): OfflineQueueDao = _offlineQueueDao.value

  public override fun signalMarkerDao(): SignalMarkerDao = _signalMarkerDao.value

  public override fun customTemplateDao(): CustomTemplateDao = _customTemplateDao.value

  public override fun alertDao(): AlertDao = _alertDao.value

  public override fun indicatorDao(): IndicatorDao = _indicatorDao.value

  public override fun drawingSnapshotDao(): DrawingSnapshotDao = _drawingSnapshotDao.value
}
