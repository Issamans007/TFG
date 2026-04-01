package com.tfg.`data`.local.dao

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.tfg.`data`.local.entity.TradingPairEntity
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Double
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class TradingPairDao_Impl(
  __db: RoomDatabase,
) : TradingPairDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfTradingPairEntity: EntityInsertAdapter<TradingPairEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfTradingPairEntity = object : EntityInsertAdapter<TradingPairEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `trading_pairs` (`symbol`,`baseAsset`,`quoteAsset`,`lastPrice`,`priceChangePercent24h`,`volume24h`,`high24h`,`low24h`,`isWatchlisted`,`isActiveForTrading`,`minQty`,`stepSize`,`tickSize`,`minNotional`,`updatedAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: TradingPairEntity) {
        statement.bindText(1, entity.symbol)
        statement.bindText(2, entity.baseAsset)
        statement.bindText(3, entity.quoteAsset)
        statement.bindDouble(4, entity.lastPrice)
        statement.bindDouble(5, entity.priceChangePercent24h)
        statement.bindDouble(6, entity.volume24h)
        statement.bindDouble(7, entity.high24h)
        statement.bindDouble(8, entity.low24h)
        val _tmp: Int = if (entity.isWatchlisted) 1 else 0
        statement.bindLong(9, _tmp.toLong())
        val _tmp_1: Int = if (entity.isActiveForTrading) 1 else 0
        statement.bindLong(10, _tmp_1.toLong())
        statement.bindDouble(11, entity.minQty)
        statement.bindDouble(12, entity.stepSize)
        statement.bindDouble(13, entity.tickSize)
        statement.bindDouble(14, entity.minNotional)
        statement.bindLong(15, entity.updatedAt)
      }
    }
  }

  public override suspend fun insertAll(pairs: List<TradingPairEntity>): Unit =
      performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfTradingPairEntity.insert(_connection, pairs)
  }

  public override fun getAll(): Flow<List<TradingPairEntity>> {
    val _sql: String = "SELECT * FROM trading_pairs ORDER BY volume24h DESC"
    return createFlow(__db, false, arrayOf("trading_pairs")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfSymbol: Int = getColumnIndexOrThrow(_stmt, "symbol")
        val _columnIndexOfBaseAsset: Int = getColumnIndexOrThrow(_stmt, "baseAsset")
        val _columnIndexOfQuoteAsset: Int = getColumnIndexOrThrow(_stmt, "quoteAsset")
        val _columnIndexOfLastPrice: Int = getColumnIndexOrThrow(_stmt, "lastPrice")
        val _columnIndexOfPriceChangePercent24h: Int = getColumnIndexOrThrow(_stmt,
            "priceChangePercent24h")
        val _columnIndexOfVolume24h: Int = getColumnIndexOrThrow(_stmt, "volume24h")
        val _columnIndexOfHigh24h: Int = getColumnIndexOrThrow(_stmt, "high24h")
        val _columnIndexOfLow24h: Int = getColumnIndexOrThrow(_stmt, "low24h")
        val _columnIndexOfIsWatchlisted: Int = getColumnIndexOrThrow(_stmt, "isWatchlisted")
        val _columnIndexOfIsActiveForTrading: Int = getColumnIndexOrThrow(_stmt,
            "isActiveForTrading")
        val _columnIndexOfMinQty: Int = getColumnIndexOrThrow(_stmt, "minQty")
        val _columnIndexOfStepSize: Int = getColumnIndexOrThrow(_stmt, "stepSize")
        val _columnIndexOfTickSize: Int = getColumnIndexOrThrow(_stmt, "tickSize")
        val _columnIndexOfMinNotional: Int = getColumnIndexOrThrow(_stmt, "minNotional")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: MutableList<TradingPairEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: TradingPairEntity
          val _tmpSymbol: String
          _tmpSymbol = _stmt.getText(_columnIndexOfSymbol)
          val _tmpBaseAsset: String
          _tmpBaseAsset = _stmt.getText(_columnIndexOfBaseAsset)
          val _tmpQuoteAsset: String
          _tmpQuoteAsset = _stmt.getText(_columnIndexOfQuoteAsset)
          val _tmpLastPrice: Double
          _tmpLastPrice = _stmt.getDouble(_columnIndexOfLastPrice)
          val _tmpPriceChangePercent24h: Double
          _tmpPriceChangePercent24h = _stmt.getDouble(_columnIndexOfPriceChangePercent24h)
          val _tmpVolume24h: Double
          _tmpVolume24h = _stmt.getDouble(_columnIndexOfVolume24h)
          val _tmpHigh24h: Double
          _tmpHigh24h = _stmt.getDouble(_columnIndexOfHigh24h)
          val _tmpLow24h: Double
          _tmpLow24h = _stmt.getDouble(_columnIndexOfLow24h)
          val _tmpIsWatchlisted: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsWatchlisted).toInt()
          _tmpIsWatchlisted = _tmp != 0
          val _tmpIsActiveForTrading: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsActiveForTrading).toInt()
          _tmpIsActiveForTrading = _tmp_1 != 0
          val _tmpMinQty: Double
          _tmpMinQty = _stmt.getDouble(_columnIndexOfMinQty)
          val _tmpStepSize: Double
          _tmpStepSize = _stmt.getDouble(_columnIndexOfStepSize)
          val _tmpTickSize: Double
          _tmpTickSize = _stmt.getDouble(_columnIndexOfTickSize)
          val _tmpMinNotional: Double
          _tmpMinNotional = _stmt.getDouble(_columnIndexOfMinNotional)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _item =
              TradingPairEntity(_tmpSymbol,_tmpBaseAsset,_tmpQuoteAsset,_tmpLastPrice,_tmpPriceChangePercent24h,_tmpVolume24h,_tmpHigh24h,_tmpLow24h,_tmpIsWatchlisted,_tmpIsActiveForTrading,_tmpMinQty,_tmpStepSize,_tmpTickSize,_tmpMinNotional,_tmpUpdatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getWatchlist(): Flow<List<TradingPairEntity>> {
    val _sql: String = "SELECT * FROM trading_pairs WHERE isWatchlisted = 1"
    return createFlow(__db, false, arrayOf("trading_pairs")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfSymbol: Int = getColumnIndexOrThrow(_stmt, "symbol")
        val _columnIndexOfBaseAsset: Int = getColumnIndexOrThrow(_stmt, "baseAsset")
        val _columnIndexOfQuoteAsset: Int = getColumnIndexOrThrow(_stmt, "quoteAsset")
        val _columnIndexOfLastPrice: Int = getColumnIndexOrThrow(_stmt, "lastPrice")
        val _columnIndexOfPriceChangePercent24h: Int = getColumnIndexOrThrow(_stmt,
            "priceChangePercent24h")
        val _columnIndexOfVolume24h: Int = getColumnIndexOrThrow(_stmt, "volume24h")
        val _columnIndexOfHigh24h: Int = getColumnIndexOrThrow(_stmt, "high24h")
        val _columnIndexOfLow24h: Int = getColumnIndexOrThrow(_stmt, "low24h")
        val _columnIndexOfIsWatchlisted: Int = getColumnIndexOrThrow(_stmt, "isWatchlisted")
        val _columnIndexOfIsActiveForTrading: Int = getColumnIndexOrThrow(_stmt,
            "isActiveForTrading")
        val _columnIndexOfMinQty: Int = getColumnIndexOrThrow(_stmt, "minQty")
        val _columnIndexOfStepSize: Int = getColumnIndexOrThrow(_stmt, "stepSize")
        val _columnIndexOfTickSize: Int = getColumnIndexOrThrow(_stmt, "tickSize")
        val _columnIndexOfMinNotional: Int = getColumnIndexOrThrow(_stmt, "minNotional")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: MutableList<TradingPairEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: TradingPairEntity
          val _tmpSymbol: String
          _tmpSymbol = _stmt.getText(_columnIndexOfSymbol)
          val _tmpBaseAsset: String
          _tmpBaseAsset = _stmt.getText(_columnIndexOfBaseAsset)
          val _tmpQuoteAsset: String
          _tmpQuoteAsset = _stmt.getText(_columnIndexOfQuoteAsset)
          val _tmpLastPrice: Double
          _tmpLastPrice = _stmt.getDouble(_columnIndexOfLastPrice)
          val _tmpPriceChangePercent24h: Double
          _tmpPriceChangePercent24h = _stmt.getDouble(_columnIndexOfPriceChangePercent24h)
          val _tmpVolume24h: Double
          _tmpVolume24h = _stmt.getDouble(_columnIndexOfVolume24h)
          val _tmpHigh24h: Double
          _tmpHigh24h = _stmt.getDouble(_columnIndexOfHigh24h)
          val _tmpLow24h: Double
          _tmpLow24h = _stmt.getDouble(_columnIndexOfLow24h)
          val _tmpIsWatchlisted: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsWatchlisted).toInt()
          _tmpIsWatchlisted = _tmp != 0
          val _tmpIsActiveForTrading: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsActiveForTrading).toInt()
          _tmpIsActiveForTrading = _tmp_1 != 0
          val _tmpMinQty: Double
          _tmpMinQty = _stmt.getDouble(_columnIndexOfMinQty)
          val _tmpStepSize: Double
          _tmpStepSize = _stmt.getDouble(_columnIndexOfStepSize)
          val _tmpTickSize: Double
          _tmpTickSize = _stmt.getDouble(_columnIndexOfTickSize)
          val _tmpMinNotional: Double
          _tmpMinNotional = _stmt.getDouble(_columnIndexOfMinNotional)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _item =
              TradingPairEntity(_tmpSymbol,_tmpBaseAsset,_tmpQuoteAsset,_tmpLastPrice,_tmpPriceChangePercent24h,_tmpVolume24h,_tmpHigh24h,_tmpLow24h,_tmpIsWatchlisted,_tmpIsActiveForTrading,_tmpMinQty,_tmpStepSize,_tmpTickSize,_tmpMinNotional,_tmpUpdatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getWatchlistSymbols(): List<String> {
    val _sql: String = "SELECT symbol FROM trading_pairs WHERE isWatchlisted = 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: MutableList<String> = mutableListOf()
        while (_stmt.step()) {
          val _item: String
          _item = _stmt.getText(0)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun search(query: String): List<TradingPairEntity> {
    val _sql: String =
        "SELECT * FROM trading_pairs WHERE symbol LIKE '%' || ? || '%' OR baseAsset LIKE '%' || ? || '%'"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, query)
        _argIndex = 2
        _stmt.bindText(_argIndex, query)
        val _columnIndexOfSymbol: Int = getColumnIndexOrThrow(_stmt, "symbol")
        val _columnIndexOfBaseAsset: Int = getColumnIndexOrThrow(_stmt, "baseAsset")
        val _columnIndexOfQuoteAsset: Int = getColumnIndexOrThrow(_stmt, "quoteAsset")
        val _columnIndexOfLastPrice: Int = getColumnIndexOrThrow(_stmt, "lastPrice")
        val _columnIndexOfPriceChangePercent24h: Int = getColumnIndexOrThrow(_stmt,
            "priceChangePercent24h")
        val _columnIndexOfVolume24h: Int = getColumnIndexOrThrow(_stmt, "volume24h")
        val _columnIndexOfHigh24h: Int = getColumnIndexOrThrow(_stmt, "high24h")
        val _columnIndexOfLow24h: Int = getColumnIndexOrThrow(_stmt, "low24h")
        val _columnIndexOfIsWatchlisted: Int = getColumnIndexOrThrow(_stmt, "isWatchlisted")
        val _columnIndexOfIsActiveForTrading: Int = getColumnIndexOrThrow(_stmt,
            "isActiveForTrading")
        val _columnIndexOfMinQty: Int = getColumnIndexOrThrow(_stmt, "minQty")
        val _columnIndexOfStepSize: Int = getColumnIndexOrThrow(_stmt, "stepSize")
        val _columnIndexOfTickSize: Int = getColumnIndexOrThrow(_stmt, "tickSize")
        val _columnIndexOfMinNotional: Int = getColumnIndexOrThrow(_stmt, "minNotional")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: MutableList<TradingPairEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: TradingPairEntity
          val _tmpSymbol: String
          _tmpSymbol = _stmt.getText(_columnIndexOfSymbol)
          val _tmpBaseAsset: String
          _tmpBaseAsset = _stmt.getText(_columnIndexOfBaseAsset)
          val _tmpQuoteAsset: String
          _tmpQuoteAsset = _stmt.getText(_columnIndexOfQuoteAsset)
          val _tmpLastPrice: Double
          _tmpLastPrice = _stmt.getDouble(_columnIndexOfLastPrice)
          val _tmpPriceChangePercent24h: Double
          _tmpPriceChangePercent24h = _stmt.getDouble(_columnIndexOfPriceChangePercent24h)
          val _tmpVolume24h: Double
          _tmpVolume24h = _stmt.getDouble(_columnIndexOfVolume24h)
          val _tmpHigh24h: Double
          _tmpHigh24h = _stmt.getDouble(_columnIndexOfHigh24h)
          val _tmpLow24h: Double
          _tmpLow24h = _stmt.getDouble(_columnIndexOfLow24h)
          val _tmpIsWatchlisted: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsWatchlisted).toInt()
          _tmpIsWatchlisted = _tmp != 0
          val _tmpIsActiveForTrading: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsActiveForTrading).toInt()
          _tmpIsActiveForTrading = _tmp_1 != 0
          val _tmpMinQty: Double
          _tmpMinQty = _stmt.getDouble(_columnIndexOfMinQty)
          val _tmpStepSize: Double
          _tmpStepSize = _stmt.getDouble(_columnIndexOfStepSize)
          val _tmpTickSize: Double
          _tmpTickSize = _stmt.getDouble(_columnIndexOfTickSize)
          val _tmpMinNotional: Double
          _tmpMinNotional = _stmt.getDouble(_columnIndexOfMinNotional)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _item =
              TradingPairEntity(_tmpSymbol,_tmpBaseAsset,_tmpQuoteAsset,_tmpLastPrice,_tmpPriceChangePercent24h,_tmpVolume24h,_tmpHigh24h,_tmpLow24h,_tmpIsWatchlisted,_tmpIsActiveForTrading,_tmpMinQty,_tmpStepSize,_tmpTickSize,_tmpMinNotional,_tmpUpdatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getBySymbol(symbol: String): Flow<TradingPairEntity?> {
    val _sql: String = "SELECT * FROM trading_pairs WHERE symbol = ?"
    return createFlow(__db, false, arrayOf("trading_pairs")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, symbol)
        val _columnIndexOfSymbol: Int = getColumnIndexOrThrow(_stmt, "symbol")
        val _columnIndexOfBaseAsset: Int = getColumnIndexOrThrow(_stmt, "baseAsset")
        val _columnIndexOfQuoteAsset: Int = getColumnIndexOrThrow(_stmt, "quoteAsset")
        val _columnIndexOfLastPrice: Int = getColumnIndexOrThrow(_stmt, "lastPrice")
        val _columnIndexOfPriceChangePercent24h: Int = getColumnIndexOrThrow(_stmt,
            "priceChangePercent24h")
        val _columnIndexOfVolume24h: Int = getColumnIndexOrThrow(_stmt, "volume24h")
        val _columnIndexOfHigh24h: Int = getColumnIndexOrThrow(_stmt, "high24h")
        val _columnIndexOfLow24h: Int = getColumnIndexOrThrow(_stmt, "low24h")
        val _columnIndexOfIsWatchlisted: Int = getColumnIndexOrThrow(_stmt, "isWatchlisted")
        val _columnIndexOfIsActiveForTrading: Int = getColumnIndexOrThrow(_stmt,
            "isActiveForTrading")
        val _columnIndexOfMinQty: Int = getColumnIndexOrThrow(_stmt, "minQty")
        val _columnIndexOfStepSize: Int = getColumnIndexOrThrow(_stmt, "stepSize")
        val _columnIndexOfTickSize: Int = getColumnIndexOrThrow(_stmt, "tickSize")
        val _columnIndexOfMinNotional: Int = getColumnIndexOrThrow(_stmt, "minNotional")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: TradingPairEntity?
        if (_stmt.step()) {
          val _tmpSymbol: String
          _tmpSymbol = _stmt.getText(_columnIndexOfSymbol)
          val _tmpBaseAsset: String
          _tmpBaseAsset = _stmt.getText(_columnIndexOfBaseAsset)
          val _tmpQuoteAsset: String
          _tmpQuoteAsset = _stmt.getText(_columnIndexOfQuoteAsset)
          val _tmpLastPrice: Double
          _tmpLastPrice = _stmt.getDouble(_columnIndexOfLastPrice)
          val _tmpPriceChangePercent24h: Double
          _tmpPriceChangePercent24h = _stmt.getDouble(_columnIndexOfPriceChangePercent24h)
          val _tmpVolume24h: Double
          _tmpVolume24h = _stmt.getDouble(_columnIndexOfVolume24h)
          val _tmpHigh24h: Double
          _tmpHigh24h = _stmt.getDouble(_columnIndexOfHigh24h)
          val _tmpLow24h: Double
          _tmpLow24h = _stmt.getDouble(_columnIndexOfLow24h)
          val _tmpIsWatchlisted: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsWatchlisted).toInt()
          _tmpIsWatchlisted = _tmp != 0
          val _tmpIsActiveForTrading: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsActiveForTrading).toInt()
          _tmpIsActiveForTrading = _tmp_1 != 0
          val _tmpMinQty: Double
          _tmpMinQty = _stmt.getDouble(_columnIndexOfMinQty)
          val _tmpStepSize: Double
          _tmpStepSize = _stmt.getDouble(_columnIndexOfStepSize)
          val _tmpTickSize: Double
          _tmpTickSize = _stmt.getDouble(_columnIndexOfTickSize)
          val _tmpMinNotional: Double
          _tmpMinNotional = _stmt.getDouble(_columnIndexOfMinNotional)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _result =
              TradingPairEntity(_tmpSymbol,_tmpBaseAsset,_tmpQuoteAsset,_tmpLastPrice,_tmpPriceChangePercent24h,_tmpVolume24h,_tmpHigh24h,_tmpLow24h,_tmpIsWatchlisted,_tmpIsActiveForTrading,_tmpMinQty,_tmpStepSize,_tmpTickSize,_tmpMinNotional,_tmpUpdatedAt)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun setWatchlisted(symbol: String, watched: Boolean) {
    val _sql: String = "UPDATE trading_pairs SET isWatchlisted = ? WHERE symbol = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        val _tmp: Int = if (watched) 1 else 0
        _stmt.bindLong(_argIndex, _tmp.toLong())
        _argIndex = 2
        _stmt.bindText(_argIndex, symbol)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun updateTicker(
    symbol: String,
    price: Double,
    change: Double,
    volume: Double,
  ) {
    val _sql: String =
        "UPDATE trading_pairs SET lastPrice = ?, priceChangePercent24h = ?, volume24h = ? WHERE symbol = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindDouble(_argIndex, price)
        _argIndex = 2
        _stmt.bindDouble(_argIndex, change)
        _argIndex = 3
        _stmt.bindDouble(_argIndex, volume)
        _argIndex = 4
        _stmt.bindText(_argIndex, symbol)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
