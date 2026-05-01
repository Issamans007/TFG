package com.tfg.`data`.local.dao

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.tfg.`data`.local.entity.SignalMarkerEntity
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
public class SignalMarkerDao_Impl(
  __db: RoomDatabase,
) : SignalMarkerDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfSignalMarkerEntity: EntityInsertAdapter<SignalMarkerEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfSignalMarkerEntity = object : EntityInsertAdapter<SignalMarkerEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `signal_markers` (`id`,`scriptId`,`symbol`,`interval`,`openTime`,`signalType`,`price`,`label`,`orderType`,`timestamp`) VALUES (?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: SignalMarkerEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.scriptId)
        statement.bindText(3, entity.symbol)
        statement.bindText(4, entity.interval)
        statement.bindLong(5, entity.openTime)
        statement.bindText(6, entity.signalType)
        statement.bindDouble(7, entity.price)
        statement.bindText(8, entity.label)
        statement.bindText(9, entity.orderType)
        statement.bindLong(10, entity.timestamp)
      }
    }
  }

  public override suspend fun insertAll(markers: List<SignalMarkerEntity>): Unit =
      performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfSignalMarkerEntity.insert(_connection, markers)
  }

  public override fun getForChart(symbol: String, interval: String):
      Flow<List<SignalMarkerEntity>> {
    val _sql: String =
        "SELECT * FROM signal_markers WHERE symbol = ? AND interval = ? ORDER BY openTime ASC"
    return createFlow(__db, false, arrayOf("signal_markers")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, symbol)
        _argIndex = 2
        _stmt.bindText(_argIndex, interval)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfScriptId: Int = getColumnIndexOrThrow(_stmt, "scriptId")
        val _columnIndexOfSymbol: Int = getColumnIndexOrThrow(_stmt, "symbol")
        val _columnIndexOfInterval: Int = getColumnIndexOrThrow(_stmt, "interval")
        val _columnIndexOfOpenTime: Int = getColumnIndexOrThrow(_stmt, "openTime")
        val _columnIndexOfSignalType: Int = getColumnIndexOrThrow(_stmt, "signalType")
        val _columnIndexOfPrice: Int = getColumnIndexOrThrow(_stmt, "price")
        val _columnIndexOfLabel: Int = getColumnIndexOrThrow(_stmt, "label")
        val _columnIndexOfOrderType: Int = getColumnIndexOrThrow(_stmt, "orderType")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _result: MutableList<SignalMarkerEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: SignalMarkerEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpScriptId: String
          _tmpScriptId = _stmt.getText(_columnIndexOfScriptId)
          val _tmpSymbol: String
          _tmpSymbol = _stmt.getText(_columnIndexOfSymbol)
          val _tmpInterval: String
          _tmpInterval = _stmt.getText(_columnIndexOfInterval)
          val _tmpOpenTime: Long
          _tmpOpenTime = _stmt.getLong(_columnIndexOfOpenTime)
          val _tmpSignalType: String
          _tmpSignalType = _stmt.getText(_columnIndexOfSignalType)
          val _tmpPrice: Double
          _tmpPrice = _stmt.getDouble(_columnIndexOfPrice)
          val _tmpLabel: String
          _tmpLabel = _stmt.getText(_columnIndexOfLabel)
          val _tmpOrderType: String
          _tmpOrderType = _stmt.getText(_columnIndexOfOrderType)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          _item =
              SignalMarkerEntity(_tmpId,_tmpScriptId,_tmpSymbol,_tmpInterval,_tmpOpenTime,_tmpSignalType,_tmpPrice,_tmpLabel,_tmpOrderType,_tmpTimestamp)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteForScript(scriptId: String) {
    val _sql: String = "DELETE FROM signal_markers WHERE scriptId = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, scriptId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteForSymbol(symbol: String) {
    val _sql: String = "DELETE FROM signal_markers WHERE symbol = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, symbol)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteForScriptAndSymbol(scriptId: String, symbol: String) {
    val _sql: String = "DELETE FROM signal_markers WHERE scriptId = ? AND symbol = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, scriptId)
        _argIndex = 2
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
