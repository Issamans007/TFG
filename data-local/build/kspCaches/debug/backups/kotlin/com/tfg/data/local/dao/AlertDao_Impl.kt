package com.tfg.`data`.local.dao

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.tfg.`data`.local.entity.AlertEntity
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
public class AlertDao_Impl(
  __db: RoomDatabase,
) : AlertDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfAlertEntity: EntityInsertAdapter<AlertEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfAlertEntity = object : EntityInsertAdapter<AlertEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `alerts` (`id`,`symbol`,`name`,`type`,`condition`,`targetValue`,`secondaryValue`,`interval`,`isEnabled`,`isRepeating`,`repeatIntervalSec`,`lastTriggeredAt`,`triggerCount`,`createdAt`,`updatedAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: AlertEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.symbol)
        statement.bindText(3, entity.name)
        statement.bindText(4, entity.type)
        statement.bindText(5, entity.condition)
        statement.bindDouble(6, entity.targetValue)
        val _tmpSecondaryValue: Double? = entity.secondaryValue
        if (_tmpSecondaryValue == null) {
          statement.bindNull(7)
        } else {
          statement.bindDouble(7, _tmpSecondaryValue)
        }
        statement.bindText(8, entity.interval)
        val _tmp: Int = if (entity.isEnabled) 1 else 0
        statement.bindLong(9, _tmp.toLong())
        val _tmp_1: Int = if (entity.isRepeating) 1 else 0
        statement.bindLong(10, _tmp_1.toLong())
        statement.bindLong(11, entity.repeatIntervalSec.toLong())
        val _tmpLastTriggeredAt: Long? = entity.lastTriggeredAt
        if (_tmpLastTriggeredAt == null) {
          statement.bindNull(12)
        } else {
          statement.bindLong(12, _tmpLastTriggeredAt)
        }
        statement.bindLong(13, entity.triggerCount.toLong())
        statement.bindLong(14, entity.createdAt)
        statement.bindLong(15, entity.updatedAt)
      }
    }
  }

  public override suspend fun insert(alert: AlertEntity): Unit = performSuspending(__db, false,
      true) { _connection ->
    __insertAdapterOfAlertEntity.insert(_connection, alert)
  }

  public override fun getAll(): Flow<List<AlertEntity>> {
    val _sql: String = "SELECT * FROM alerts ORDER BY createdAt DESC"
    return createFlow(__db, false, arrayOf("alerts")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfSymbol: Int = getColumnIndexOrThrow(_stmt, "symbol")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfCondition: Int = getColumnIndexOrThrow(_stmt, "condition")
        val _columnIndexOfTargetValue: Int = getColumnIndexOrThrow(_stmt, "targetValue")
        val _columnIndexOfSecondaryValue: Int = getColumnIndexOrThrow(_stmt, "secondaryValue")
        val _columnIndexOfInterval: Int = getColumnIndexOrThrow(_stmt, "interval")
        val _columnIndexOfIsEnabled: Int = getColumnIndexOrThrow(_stmt, "isEnabled")
        val _columnIndexOfIsRepeating: Int = getColumnIndexOrThrow(_stmt, "isRepeating")
        val _columnIndexOfRepeatIntervalSec: Int = getColumnIndexOrThrow(_stmt, "repeatIntervalSec")
        val _columnIndexOfLastTriggeredAt: Int = getColumnIndexOrThrow(_stmt, "lastTriggeredAt")
        val _columnIndexOfTriggerCount: Int = getColumnIndexOrThrow(_stmt, "triggerCount")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: MutableList<AlertEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: AlertEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpSymbol: String
          _tmpSymbol = _stmt.getText(_columnIndexOfSymbol)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpType: String
          _tmpType = _stmt.getText(_columnIndexOfType)
          val _tmpCondition: String
          _tmpCondition = _stmt.getText(_columnIndexOfCondition)
          val _tmpTargetValue: Double
          _tmpTargetValue = _stmt.getDouble(_columnIndexOfTargetValue)
          val _tmpSecondaryValue: Double?
          if (_stmt.isNull(_columnIndexOfSecondaryValue)) {
            _tmpSecondaryValue = null
          } else {
            _tmpSecondaryValue = _stmt.getDouble(_columnIndexOfSecondaryValue)
          }
          val _tmpInterval: String
          _tmpInterval = _stmt.getText(_columnIndexOfInterval)
          val _tmpIsEnabled: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsEnabled).toInt()
          _tmpIsEnabled = _tmp != 0
          val _tmpIsRepeating: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsRepeating).toInt()
          _tmpIsRepeating = _tmp_1 != 0
          val _tmpRepeatIntervalSec: Int
          _tmpRepeatIntervalSec = _stmt.getLong(_columnIndexOfRepeatIntervalSec).toInt()
          val _tmpLastTriggeredAt: Long?
          if (_stmt.isNull(_columnIndexOfLastTriggeredAt)) {
            _tmpLastTriggeredAt = null
          } else {
            _tmpLastTriggeredAt = _stmt.getLong(_columnIndexOfLastTriggeredAt)
          }
          val _tmpTriggerCount: Int
          _tmpTriggerCount = _stmt.getLong(_columnIndexOfTriggerCount).toInt()
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _item =
              AlertEntity(_tmpId,_tmpSymbol,_tmpName,_tmpType,_tmpCondition,_tmpTargetValue,_tmpSecondaryValue,_tmpInterval,_tmpIsEnabled,_tmpIsRepeating,_tmpRepeatIntervalSec,_tmpLastTriggeredAt,_tmpTriggerCount,_tmpCreatedAt,_tmpUpdatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getEnabled(): Flow<List<AlertEntity>> {
    val _sql: String = "SELECT * FROM alerts WHERE isEnabled = 1 ORDER BY createdAt DESC"
    return createFlow(__db, false, arrayOf("alerts")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfSymbol: Int = getColumnIndexOrThrow(_stmt, "symbol")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfCondition: Int = getColumnIndexOrThrow(_stmt, "condition")
        val _columnIndexOfTargetValue: Int = getColumnIndexOrThrow(_stmt, "targetValue")
        val _columnIndexOfSecondaryValue: Int = getColumnIndexOrThrow(_stmt, "secondaryValue")
        val _columnIndexOfInterval: Int = getColumnIndexOrThrow(_stmt, "interval")
        val _columnIndexOfIsEnabled: Int = getColumnIndexOrThrow(_stmt, "isEnabled")
        val _columnIndexOfIsRepeating: Int = getColumnIndexOrThrow(_stmt, "isRepeating")
        val _columnIndexOfRepeatIntervalSec: Int = getColumnIndexOrThrow(_stmt, "repeatIntervalSec")
        val _columnIndexOfLastTriggeredAt: Int = getColumnIndexOrThrow(_stmt, "lastTriggeredAt")
        val _columnIndexOfTriggerCount: Int = getColumnIndexOrThrow(_stmt, "triggerCount")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: MutableList<AlertEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: AlertEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpSymbol: String
          _tmpSymbol = _stmt.getText(_columnIndexOfSymbol)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpType: String
          _tmpType = _stmt.getText(_columnIndexOfType)
          val _tmpCondition: String
          _tmpCondition = _stmt.getText(_columnIndexOfCondition)
          val _tmpTargetValue: Double
          _tmpTargetValue = _stmt.getDouble(_columnIndexOfTargetValue)
          val _tmpSecondaryValue: Double?
          if (_stmt.isNull(_columnIndexOfSecondaryValue)) {
            _tmpSecondaryValue = null
          } else {
            _tmpSecondaryValue = _stmt.getDouble(_columnIndexOfSecondaryValue)
          }
          val _tmpInterval: String
          _tmpInterval = _stmt.getText(_columnIndexOfInterval)
          val _tmpIsEnabled: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsEnabled).toInt()
          _tmpIsEnabled = _tmp != 0
          val _tmpIsRepeating: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsRepeating).toInt()
          _tmpIsRepeating = _tmp_1 != 0
          val _tmpRepeatIntervalSec: Int
          _tmpRepeatIntervalSec = _stmt.getLong(_columnIndexOfRepeatIntervalSec).toInt()
          val _tmpLastTriggeredAt: Long?
          if (_stmt.isNull(_columnIndexOfLastTriggeredAt)) {
            _tmpLastTriggeredAt = null
          } else {
            _tmpLastTriggeredAt = _stmt.getLong(_columnIndexOfLastTriggeredAt)
          }
          val _tmpTriggerCount: Int
          _tmpTriggerCount = _stmt.getLong(_columnIndexOfTriggerCount).toInt()
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _item =
              AlertEntity(_tmpId,_tmpSymbol,_tmpName,_tmpType,_tmpCondition,_tmpTargetValue,_tmpSecondaryValue,_tmpInterval,_tmpIsEnabled,_tmpIsRepeating,_tmpRepeatIntervalSec,_tmpLastTriggeredAt,_tmpTriggerCount,_tmpCreatedAt,_tmpUpdatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getById(id: String): Flow<AlertEntity?> {
    val _sql: String = "SELECT * FROM alerts WHERE id = ?"
    return createFlow(__db, false, arrayOf("alerts")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfSymbol: Int = getColumnIndexOrThrow(_stmt, "symbol")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfCondition: Int = getColumnIndexOrThrow(_stmt, "condition")
        val _columnIndexOfTargetValue: Int = getColumnIndexOrThrow(_stmt, "targetValue")
        val _columnIndexOfSecondaryValue: Int = getColumnIndexOrThrow(_stmt, "secondaryValue")
        val _columnIndexOfInterval: Int = getColumnIndexOrThrow(_stmt, "interval")
        val _columnIndexOfIsEnabled: Int = getColumnIndexOrThrow(_stmt, "isEnabled")
        val _columnIndexOfIsRepeating: Int = getColumnIndexOrThrow(_stmt, "isRepeating")
        val _columnIndexOfRepeatIntervalSec: Int = getColumnIndexOrThrow(_stmt, "repeatIntervalSec")
        val _columnIndexOfLastTriggeredAt: Int = getColumnIndexOrThrow(_stmt, "lastTriggeredAt")
        val _columnIndexOfTriggerCount: Int = getColumnIndexOrThrow(_stmt, "triggerCount")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: AlertEntity?
        if (_stmt.step()) {
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpSymbol: String
          _tmpSymbol = _stmt.getText(_columnIndexOfSymbol)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpType: String
          _tmpType = _stmt.getText(_columnIndexOfType)
          val _tmpCondition: String
          _tmpCondition = _stmt.getText(_columnIndexOfCondition)
          val _tmpTargetValue: Double
          _tmpTargetValue = _stmt.getDouble(_columnIndexOfTargetValue)
          val _tmpSecondaryValue: Double?
          if (_stmt.isNull(_columnIndexOfSecondaryValue)) {
            _tmpSecondaryValue = null
          } else {
            _tmpSecondaryValue = _stmt.getDouble(_columnIndexOfSecondaryValue)
          }
          val _tmpInterval: String
          _tmpInterval = _stmt.getText(_columnIndexOfInterval)
          val _tmpIsEnabled: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsEnabled).toInt()
          _tmpIsEnabled = _tmp != 0
          val _tmpIsRepeating: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsRepeating).toInt()
          _tmpIsRepeating = _tmp_1 != 0
          val _tmpRepeatIntervalSec: Int
          _tmpRepeatIntervalSec = _stmt.getLong(_columnIndexOfRepeatIntervalSec).toInt()
          val _tmpLastTriggeredAt: Long?
          if (_stmt.isNull(_columnIndexOfLastTriggeredAt)) {
            _tmpLastTriggeredAt = null
          } else {
            _tmpLastTriggeredAt = _stmt.getLong(_columnIndexOfLastTriggeredAt)
          }
          val _tmpTriggerCount: Int
          _tmpTriggerCount = _stmt.getLong(_columnIndexOfTriggerCount).toInt()
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _result =
              AlertEntity(_tmpId,_tmpSymbol,_tmpName,_tmpType,_tmpCondition,_tmpTargetValue,_tmpSecondaryValue,_tmpInterval,_tmpIsEnabled,_tmpIsRepeating,_tmpRepeatIntervalSec,_tmpLastTriggeredAt,_tmpTriggerCount,_tmpCreatedAt,_tmpUpdatedAt)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getForSymbol(symbol: String): Flow<List<AlertEntity>> {
    val _sql: String = "SELECT * FROM alerts WHERE symbol = ? ORDER BY createdAt DESC"
    return createFlow(__db, false, arrayOf("alerts")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, symbol)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfSymbol: Int = getColumnIndexOrThrow(_stmt, "symbol")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfCondition: Int = getColumnIndexOrThrow(_stmt, "condition")
        val _columnIndexOfTargetValue: Int = getColumnIndexOrThrow(_stmt, "targetValue")
        val _columnIndexOfSecondaryValue: Int = getColumnIndexOrThrow(_stmt, "secondaryValue")
        val _columnIndexOfInterval: Int = getColumnIndexOrThrow(_stmt, "interval")
        val _columnIndexOfIsEnabled: Int = getColumnIndexOrThrow(_stmt, "isEnabled")
        val _columnIndexOfIsRepeating: Int = getColumnIndexOrThrow(_stmt, "isRepeating")
        val _columnIndexOfRepeatIntervalSec: Int = getColumnIndexOrThrow(_stmt, "repeatIntervalSec")
        val _columnIndexOfLastTriggeredAt: Int = getColumnIndexOrThrow(_stmt, "lastTriggeredAt")
        val _columnIndexOfTriggerCount: Int = getColumnIndexOrThrow(_stmt, "triggerCount")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: MutableList<AlertEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: AlertEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpSymbol: String
          _tmpSymbol = _stmt.getText(_columnIndexOfSymbol)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpType: String
          _tmpType = _stmt.getText(_columnIndexOfType)
          val _tmpCondition: String
          _tmpCondition = _stmt.getText(_columnIndexOfCondition)
          val _tmpTargetValue: Double
          _tmpTargetValue = _stmt.getDouble(_columnIndexOfTargetValue)
          val _tmpSecondaryValue: Double?
          if (_stmt.isNull(_columnIndexOfSecondaryValue)) {
            _tmpSecondaryValue = null
          } else {
            _tmpSecondaryValue = _stmt.getDouble(_columnIndexOfSecondaryValue)
          }
          val _tmpInterval: String
          _tmpInterval = _stmt.getText(_columnIndexOfInterval)
          val _tmpIsEnabled: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsEnabled).toInt()
          _tmpIsEnabled = _tmp != 0
          val _tmpIsRepeating: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsRepeating).toInt()
          _tmpIsRepeating = _tmp_1 != 0
          val _tmpRepeatIntervalSec: Int
          _tmpRepeatIntervalSec = _stmt.getLong(_columnIndexOfRepeatIntervalSec).toInt()
          val _tmpLastTriggeredAt: Long?
          if (_stmt.isNull(_columnIndexOfLastTriggeredAt)) {
            _tmpLastTriggeredAt = null
          } else {
            _tmpLastTriggeredAt = _stmt.getLong(_columnIndexOfLastTriggeredAt)
          }
          val _tmpTriggerCount: Int
          _tmpTriggerCount = _stmt.getLong(_columnIndexOfTriggerCount).toInt()
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _item =
              AlertEntity(_tmpId,_tmpSymbol,_tmpName,_tmpType,_tmpCondition,_tmpTargetValue,_tmpSecondaryValue,_tmpInterval,_tmpIsEnabled,_tmpIsRepeating,_tmpRepeatIntervalSec,_tmpLastTriggeredAt,_tmpTriggerCount,_tmpCreatedAt,_tmpUpdatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun delete(id: String) {
    val _sql: String = "DELETE FROM alerts WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun updateEnabled(
    id: String,
    enabled: Boolean,
    now: Long,
  ) {
    val _sql: String = "UPDATE alerts SET isEnabled = ?, updatedAt = ? WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        val _tmp: Int = if (enabled) 1 else 0
        _stmt.bindLong(_argIndex, _tmp.toLong())
        _argIndex = 2
        _stmt.bindLong(_argIndex, now)
        _argIndex = 3
        _stmt.bindText(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun recordTrigger(id: String, timestamp: Long) {
    val _sql: String =
        "UPDATE alerts SET lastTriggeredAt = ?, triggerCount = triggerCount + 1, updatedAt = ? WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, timestamp)
        _argIndex = 2
        _stmt.bindLong(_argIndex, timestamp)
        _argIndex = 3
        _stmt.bindText(_argIndex, id)
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
