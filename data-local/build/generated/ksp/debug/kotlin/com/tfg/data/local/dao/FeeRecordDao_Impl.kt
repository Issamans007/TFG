package com.tfg.`data`.local.dao

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.tfg.`data`.local.entity.FeeRecordEntity
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
public class FeeRecordDao_Impl(
  __db: RoomDatabase,
) : FeeRecordDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfFeeRecordEntity: EntityInsertAdapter<FeeRecordEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfFeeRecordEntity = object : EntityInsertAdapter<FeeRecordEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `fee_records` (`id`,`orderId`,`symbol`,`feeAmount`,`feeAsset`,`feeType`,`timestamp`) VALUES (?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: FeeRecordEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.orderId)
        statement.bindText(3, entity.symbol)
        statement.bindDouble(4, entity.feeAmount)
        statement.bindText(5, entity.feeAsset)
        statement.bindText(6, entity.feeType)
        statement.bindLong(7, entity.timestamp)
      }
    }
  }

  public override suspend fun insert(record: FeeRecordEntity): Unit = performSuspending(__db, false,
      true) { _connection ->
    __insertAdapterOfFeeRecordEntity.insert(_connection, record)
  }

  public override fun getAll(): Flow<List<FeeRecordEntity>> {
    val _sql: String = "SELECT * FROM fee_records ORDER BY timestamp DESC"
    return createFlow(__db, false, arrayOf("fee_records")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfOrderId: Int = getColumnIndexOrThrow(_stmt, "orderId")
        val _columnIndexOfSymbol: Int = getColumnIndexOrThrow(_stmt, "symbol")
        val _columnIndexOfFeeAmount: Int = getColumnIndexOrThrow(_stmt, "feeAmount")
        val _columnIndexOfFeeAsset: Int = getColumnIndexOrThrow(_stmt, "feeAsset")
        val _columnIndexOfFeeType: Int = getColumnIndexOrThrow(_stmt, "feeType")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _result: MutableList<FeeRecordEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: FeeRecordEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpOrderId: String
          _tmpOrderId = _stmt.getText(_columnIndexOfOrderId)
          val _tmpSymbol: String
          _tmpSymbol = _stmt.getText(_columnIndexOfSymbol)
          val _tmpFeeAmount: Double
          _tmpFeeAmount = _stmt.getDouble(_columnIndexOfFeeAmount)
          val _tmpFeeAsset: String
          _tmpFeeAsset = _stmt.getText(_columnIndexOfFeeAsset)
          val _tmpFeeType: String
          _tmpFeeType = _stmt.getText(_columnIndexOfFeeType)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          _item =
              FeeRecordEntity(_tmpId,_tmpOrderId,_tmpSymbol,_tmpFeeAmount,_tmpFeeAsset,_tmpFeeType,_tmpTimestamp)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getTotalFees(from: Long?): Flow<Double?> {
    val _sql: String = "SELECT SUM(feeAmount) FROM fee_records WHERE (? IS NULL OR timestamp >= ?)"
    return createFlow(__db, false, arrayOf("fee_records")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        if (from == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindLong(_argIndex, from)
        }
        _argIndex = 2
        if (from == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindLong(_argIndex, from)
        }
        val _result: Double?
        if (_stmt.step()) {
          val _tmp: Double?
          if (_stmt.isNull(0)) {
            _tmp = null
          } else {
            _tmp = _stmt.getDouble(0)
          }
          _result = _tmp
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
