package com.tfg.`data`.local.dao

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.tfg.`data`.local.entity.CandleEntity
import javax.`annotation`.processing.Generated
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
public class CandleDao_Impl(
  __db: RoomDatabase,
) : CandleDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfCandleEntity: EntityInsertAdapter<CandleEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfCandleEntity = object : EntityInsertAdapter<CandleEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `candles` (`symbol`,`interval`,`openTime`,`open`,`high`,`low`,`close`,`volume`,`closeTime`,`quoteVolume`,`numberOfTrades`) VALUES (?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: CandleEntity) {
        statement.bindText(1, entity.symbol)
        statement.bindText(2, entity.interval)
        statement.bindLong(3, entity.openTime)
        statement.bindDouble(4, entity.open)
        statement.bindDouble(5, entity.high)
        statement.bindDouble(6, entity.low)
        statement.bindDouble(7, entity.close)
        statement.bindDouble(8, entity.volume)
        statement.bindLong(9, entity.closeTime)
        statement.bindDouble(10, entity.quoteVolume)
        statement.bindLong(11, entity.numberOfTrades.toLong())
      }
    }
  }

  public override suspend fun insertAll(candles: List<CandleEntity>): Unit = performSuspending(__db,
      false, true) { _connection ->
    __insertAdapterOfCandleEntity.insert(_connection, candles)
  }

  public override fun getCandles(
    symbol: String,
    interval: String,
    limit: Int,
  ): Flow<List<CandleEntity>> {
    val _sql: String =
        "SELECT * FROM candles WHERE symbol = ? AND interval = ? ORDER BY openTime ASC LIMIT ?"
    return createFlow(__db, false, arrayOf("candles")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, symbol)
        _argIndex = 2
        _stmt.bindText(_argIndex, interval)
        _argIndex = 3
        _stmt.bindLong(_argIndex, limit.toLong())
        val _columnIndexOfSymbol: Int = getColumnIndexOrThrow(_stmt, "symbol")
        val _columnIndexOfInterval: Int = getColumnIndexOrThrow(_stmt, "interval")
        val _columnIndexOfOpenTime: Int = getColumnIndexOrThrow(_stmt, "openTime")
        val _columnIndexOfOpen: Int = getColumnIndexOrThrow(_stmt, "open")
        val _columnIndexOfHigh: Int = getColumnIndexOrThrow(_stmt, "high")
        val _columnIndexOfLow: Int = getColumnIndexOrThrow(_stmt, "low")
        val _columnIndexOfClose: Int = getColumnIndexOrThrow(_stmt, "close")
        val _columnIndexOfVolume: Int = getColumnIndexOrThrow(_stmt, "volume")
        val _columnIndexOfCloseTime: Int = getColumnIndexOrThrow(_stmt, "closeTime")
        val _columnIndexOfQuoteVolume: Int = getColumnIndexOrThrow(_stmt, "quoteVolume")
        val _columnIndexOfNumberOfTrades: Int = getColumnIndexOrThrow(_stmt, "numberOfTrades")
        val _result: MutableList<CandleEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: CandleEntity
          val _tmpSymbol: String
          _tmpSymbol = _stmt.getText(_columnIndexOfSymbol)
          val _tmpInterval: String
          _tmpInterval = _stmt.getText(_columnIndexOfInterval)
          val _tmpOpenTime: Long
          _tmpOpenTime = _stmt.getLong(_columnIndexOfOpenTime)
          val _tmpOpen: Double
          _tmpOpen = _stmt.getDouble(_columnIndexOfOpen)
          val _tmpHigh: Double
          _tmpHigh = _stmt.getDouble(_columnIndexOfHigh)
          val _tmpLow: Double
          _tmpLow = _stmt.getDouble(_columnIndexOfLow)
          val _tmpClose: Double
          _tmpClose = _stmt.getDouble(_columnIndexOfClose)
          val _tmpVolume: Double
          _tmpVolume = _stmt.getDouble(_columnIndexOfVolume)
          val _tmpCloseTime: Long
          _tmpCloseTime = _stmt.getLong(_columnIndexOfCloseTime)
          val _tmpQuoteVolume: Double
          _tmpQuoteVolume = _stmt.getDouble(_columnIndexOfQuoteVolume)
          val _tmpNumberOfTrades: Int
          _tmpNumberOfTrades = _stmt.getLong(_columnIndexOfNumberOfTrades).toInt()
          _item =
              CandleEntity(_tmpSymbol,_tmpInterval,_tmpOpenTime,_tmpOpen,_tmpHigh,_tmpLow,_tmpClose,_tmpVolume,_tmpCloseTime,_tmpQuoteVolume,_tmpNumberOfTrades)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getAllCandles(symbol: String, interval: String): Flow<List<CandleEntity>> {
    val _sql: String =
        "SELECT * FROM candles WHERE symbol = ? AND interval = ? ORDER BY openTime ASC"
    return createFlow(__db, false, arrayOf("candles")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, symbol)
        _argIndex = 2
        _stmt.bindText(_argIndex, interval)
        val _columnIndexOfSymbol: Int = getColumnIndexOrThrow(_stmt, "symbol")
        val _columnIndexOfInterval: Int = getColumnIndexOrThrow(_stmt, "interval")
        val _columnIndexOfOpenTime: Int = getColumnIndexOrThrow(_stmt, "openTime")
        val _columnIndexOfOpen: Int = getColumnIndexOrThrow(_stmt, "open")
        val _columnIndexOfHigh: Int = getColumnIndexOrThrow(_stmt, "high")
        val _columnIndexOfLow: Int = getColumnIndexOrThrow(_stmt, "low")
        val _columnIndexOfClose: Int = getColumnIndexOrThrow(_stmt, "close")
        val _columnIndexOfVolume: Int = getColumnIndexOrThrow(_stmt, "volume")
        val _columnIndexOfCloseTime: Int = getColumnIndexOrThrow(_stmt, "closeTime")
        val _columnIndexOfQuoteVolume: Int = getColumnIndexOrThrow(_stmt, "quoteVolume")
        val _columnIndexOfNumberOfTrades: Int = getColumnIndexOrThrow(_stmt, "numberOfTrades")
        val _result: MutableList<CandleEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: CandleEntity
          val _tmpSymbol: String
          _tmpSymbol = _stmt.getText(_columnIndexOfSymbol)
          val _tmpInterval: String
          _tmpInterval = _stmt.getText(_columnIndexOfInterval)
          val _tmpOpenTime: Long
          _tmpOpenTime = _stmt.getLong(_columnIndexOfOpenTime)
          val _tmpOpen: Double
          _tmpOpen = _stmt.getDouble(_columnIndexOfOpen)
          val _tmpHigh: Double
          _tmpHigh = _stmt.getDouble(_columnIndexOfHigh)
          val _tmpLow: Double
          _tmpLow = _stmt.getDouble(_columnIndexOfLow)
          val _tmpClose: Double
          _tmpClose = _stmt.getDouble(_columnIndexOfClose)
          val _tmpVolume: Double
          _tmpVolume = _stmt.getDouble(_columnIndexOfVolume)
          val _tmpCloseTime: Long
          _tmpCloseTime = _stmt.getLong(_columnIndexOfCloseTime)
          val _tmpQuoteVolume: Double
          _tmpQuoteVolume = _stmt.getDouble(_columnIndexOfQuoteVolume)
          val _tmpNumberOfTrades: Int
          _tmpNumberOfTrades = _stmt.getLong(_columnIndexOfNumberOfTrades).toInt()
          _item =
              CandleEntity(_tmpSymbol,_tmpInterval,_tmpOpenTime,_tmpOpen,_tmpHigh,_tmpLow,_tmpClose,_tmpVolume,_tmpCloseTime,_tmpQuoteVolume,_tmpNumberOfTrades)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteForPair(symbol: String, interval: String) {
    val _sql: String = "DELETE FROM candles WHERE symbol = ? AND interval = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, symbol)
        _argIndex = 2
        _stmt.bindText(_argIndex, interval)
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
