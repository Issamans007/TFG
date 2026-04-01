package com.tfg.`data`.local.dao

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.tfg.`data`.local.entity.AuditLogEntity
import javax.`annotation`.processing.Generated
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
public class AuditLogDao_Impl(
  __db: RoomDatabase,
) : AuditLogDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfAuditLogEntity: EntityInsertAdapter<AuditLogEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfAuditLogEntity = object : EntityInsertAdapter<AuditLogEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `audit_logs` (`id`,`action`,`category`,`details`,`oldValue`,`newValue`,`orderId`,`symbol`,`userId`,`ipAddress`,`timestamp`) VALUES (?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: AuditLogEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.action)
        statement.bindText(3, entity.category)
        statement.bindText(4, entity.details)
        val _tmpOldValue: String? = entity.oldValue
        if (_tmpOldValue == null) {
          statement.bindNull(5)
        } else {
          statement.bindText(5, _tmpOldValue)
        }
        val _tmpNewValue: String? = entity.newValue
        if (_tmpNewValue == null) {
          statement.bindNull(6)
        } else {
          statement.bindText(6, _tmpNewValue)
        }
        val _tmpOrderId: String? = entity.orderId
        if (_tmpOrderId == null) {
          statement.bindNull(7)
        } else {
          statement.bindText(7, _tmpOrderId)
        }
        val _tmpSymbol: String? = entity.symbol
        if (_tmpSymbol == null) {
          statement.bindNull(8)
        } else {
          statement.bindText(8, _tmpSymbol)
        }
        statement.bindText(9, entity.userId)
        val _tmpIpAddress: String? = entity.ipAddress
        if (_tmpIpAddress == null) {
          statement.bindNull(10)
        } else {
          statement.bindText(10, _tmpIpAddress)
        }
        statement.bindLong(11, entity.timestamp)
      }
    }
  }

  public override suspend fun insert(log: AuditLogEntity): Unit = performSuspending(__db, false,
      true) { _connection ->
    __insertAdapterOfAuditLogEntity.insert(_connection, log)
  }

  public override fun getAll(limit: Int): Flow<List<AuditLogEntity>> {
    val _sql: String = "SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT ?"
    return createFlow(__db, false, arrayOf("audit_logs")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, limit.toLong())
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfAction: Int = getColumnIndexOrThrow(_stmt, "action")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfDetails: Int = getColumnIndexOrThrow(_stmt, "details")
        val _columnIndexOfOldValue: Int = getColumnIndexOrThrow(_stmt, "oldValue")
        val _columnIndexOfNewValue: Int = getColumnIndexOrThrow(_stmt, "newValue")
        val _columnIndexOfOrderId: Int = getColumnIndexOrThrow(_stmt, "orderId")
        val _columnIndexOfSymbol: Int = getColumnIndexOrThrow(_stmt, "symbol")
        val _columnIndexOfUserId: Int = getColumnIndexOrThrow(_stmt, "userId")
        val _columnIndexOfIpAddress: Int = getColumnIndexOrThrow(_stmt, "ipAddress")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _result: MutableList<AuditLogEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: AuditLogEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpAction: String
          _tmpAction = _stmt.getText(_columnIndexOfAction)
          val _tmpCategory: String
          _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          val _tmpDetails: String
          _tmpDetails = _stmt.getText(_columnIndexOfDetails)
          val _tmpOldValue: String?
          if (_stmt.isNull(_columnIndexOfOldValue)) {
            _tmpOldValue = null
          } else {
            _tmpOldValue = _stmt.getText(_columnIndexOfOldValue)
          }
          val _tmpNewValue: String?
          if (_stmt.isNull(_columnIndexOfNewValue)) {
            _tmpNewValue = null
          } else {
            _tmpNewValue = _stmt.getText(_columnIndexOfNewValue)
          }
          val _tmpOrderId: String?
          if (_stmt.isNull(_columnIndexOfOrderId)) {
            _tmpOrderId = null
          } else {
            _tmpOrderId = _stmt.getText(_columnIndexOfOrderId)
          }
          val _tmpSymbol: String?
          if (_stmt.isNull(_columnIndexOfSymbol)) {
            _tmpSymbol = null
          } else {
            _tmpSymbol = _stmt.getText(_columnIndexOfSymbol)
          }
          val _tmpUserId: String
          _tmpUserId = _stmt.getText(_columnIndexOfUserId)
          val _tmpIpAddress: String?
          if (_stmt.isNull(_columnIndexOfIpAddress)) {
            _tmpIpAddress = null
          } else {
            _tmpIpAddress = _stmt.getText(_columnIndexOfIpAddress)
          }
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          _item =
              AuditLogEntity(_tmpId,_tmpAction,_tmpCategory,_tmpDetails,_tmpOldValue,_tmpNewValue,_tmpOrderId,_tmpSymbol,_tmpUserId,_tmpIpAddress,_tmpTimestamp)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getByCategory(category: String, limit: Int): Flow<List<AuditLogEntity>> {
    val _sql: String = "SELECT * FROM audit_logs WHERE category = ? ORDER BY timestamp DESC LIMIT ?"
    return createFlow(__db, false, arrayOf("audit_logs")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, category)
        _argIndex = 2
        _stmt.bindLong(_argIndex, limit.toLong())
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfAction: Int = getColumnIndexOrThrow(_stmt, "action")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfDetails: Int = getColumnIndexOrThrow(_stmt, "details")
        val _columnIndexOfOldValue: Int = getColumnIndexOrThrow(_stmt, "oldValue")
        val _columnIndexOfNewValue: Int = getColumnIndexOrThrow(_stmt, "newValue")
        val _columnIndexOfOrderId: Int = getColumnIndexOrThrow(_stmt, "orderId")
        val _columnIndexOfSymbol: Int = getColumnIndexOrThrow(_stmt, "symbol")
        val _columnIndexOfUserId: Int = getColumnIndexOrThrow(_stmt, "userId")
        val _columnIndexOfIpAddress: Int = getColumnIndexOrThrow(_stmt, "ipAddress")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _result: MutableList<AuditLogEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: AuditLogEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpAction: String
          _tmpAction = _stmt.getText(_columnIndexOfAction)
          val _tmpCategory: String
          _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          val _tmpDetails: String
          _tmpDetails = _stmt.getText(_columnIndexOfDetails)
          val _tmpOldValue: String?
          if (_stmt.isNull(_columnIndexOfOldValue)) {
            _tmpOldValue = null
          } else {
            _tmpOldValue = _stmt.getText(_columnIndexOfOldValue)
          }
          val _tmpNewValue: String?
          if (_stmt.isNull(_columnIndexOfNewValue)) {
            _tmpNewValue = null
          } else {
            _tmpNewValue = _stmt.getText(_columnIndexOfNewValue)
          }
          val _tmpOrderId: String?
          if (_stmt.isNull(_columnIndexOfOrderId)) {
            _tmpOrderId = null
          } else {
            _tmpOrderId = _stmt.getText(_columnIndexOfOrderId)
          }
          val _tmpSymbol: String?
          if (_stmt.isNull(_columnIndexOfSymbol)) {
            _tmpSymbol = null
          } else {
            _tmpSymbol = _stmt.getText(_columnIndexOfSymbol)
          }
          val _tmpUserId: String
          _tmpUserId = _stmt.getText(_columnIndexOfUserId)
          val _tmpIpAddress: String?
          if (_stmt.isNull(_columnIndexOfIpAddress)) {
            _tmpIpAddress = null
          } else {
            _tmpIpAddress = _stmt.getText(_columnIndexOfIpAddress)
          }
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          _item =
              AuditLogEntity(_tmpId,_tmpAction,_tmpCategory,_tmpDetails,_tmpOldValue,_tmpNewValue,_tmpOrderId,_tmpSymbol,_tmpUserId,_tmpIpAddress,_tmpTimestamp)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getTradeAuditTrail(orderId: String): Flow<List<AuditLogEntity>> {
    val _sql: String = "SELECT * FROM audit_logs WHERE orderId = ? ORDER BY timestamp ASC"
    return createFlow(__db, false, arrayOf("audit_logs")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, orderId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfAction: Int = getColumnIndexOrThrow(_stmt, "action")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfDetails: Int = getColumnIndexOrThrow(_stmt, "details")
        val _columnIndexOfOldValue: Int = getColumnIndexOrThrow(_stmt, "oldValue")
        val _columnIndexOfNewValue: Int = getColumnIndexOrThrow(_stmt, "newValue")
        val _columnIndexOfOrderId: Int = getColumnIndexOrThrow(_stmt, "orderId")
        val _columnIndexOfSymbol: Int = getColumnIndexOrThrow(_stmt, "symbol")
        val _columnIndexOfUserId: Int = getColumnIndexOrThrow(_stmt, "userId")
        val _columnIndexOfIpAddress: Int = getColumnIndexOrThrow(_stmt, "ipAddress")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _result: MutableList<AuditLogEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: AuditLogEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpAction: String
          _tmpAction = _stmt.getText(_columnIndexOfAction)
          val _tmpCategory: String
          _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          val _tmpDetails: String
          _tmpDetails = _stmt.getText(_columnIndexOfDetails)
          val _tmpOldValue: String?
          if (_stmt.isNull(_columnIndexOfOldValue)) {
            _tmpOldValue = null
          } else {
            _tmpOldValue = _stmt.getText(_columnIndexOfOldValue)
          }
          val _tmpNewValue: String?
          if (_stmt.isNull(_columnIndexOfNewValue)) {
            _tmpNewValue = null
          } else {
            _tmpNewValue = _stmt.getText(_columnIndexOfNewValue)
          }
          val _tmpOrderId: String?
          if (_stmt.isNull(_columnIndexOfOrderId)) {
            _tmpOrderId = null
          } else {
            _tmpOrderId = _stmt.getText(_columnIndexOfOrderId)
          }
          val _tmpSymbol: String?
          if (_stmt.isNull(_columnIndexOfSymbol)) {
            _tmpSymbol = null
          } else {
            _tmpSymbol = _stmt.getText(_columnIndexOfSymbol)
          }
          val _tmpUserId: String
          _tmpUserId = _stmt.getText(_columnIndexOfUserId)
          val _tmpIpAddress: String?
          if (_stmt.isNull(_columnIndexOfIpAddress)) {
            _tmpIpAddress = null
          } else {
            _tmpIpAddress = _stmt.getText(_columnIndexOfIpAddress)
          }
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          _item =
              AuditLogEntity(_tmpId,_tmpAction,_tmpCategory,_tmpDetails,_tmpOldValue,_tmpNewValue,_tmpOrderId,_tmpSymbol,_tmpUserId,_tmpIpAddress,_tmpTimestamp)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getLogsInRange(from: Long, to: Long): List<AuditLogEntity> {
    val _sql: String =
        "SELECT * FROM audit_logs WHERE timestamp BETWEEN ? AND ? ORDER BY timestamp ASC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, from)
        _argIndex = 2
        _stmt.bindLong(_argIndex, to)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfAction: Int = getColumnIndexOrThrow(_stmt, "action")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfDetails: Int = getColumnIndexOrThrow(_stmt, "details")
        val _columnIndexOfOldValue: Int = getColumnIndexOrThrow(_stmt, "oldValue")
        val _columnIndexOfNewValue: Int = getColumnIndexOrThrow(_stmt, "newValue")
        val _columnIndexOfOrderId: Int = getColumnIndexOrThrow(_stmt, "orderId")
        val _columnIndexOfSymbol: Int = getColumnIndexOrThrow(_stmt, "symbol")
        val _columnIndexOfUserId: Int = getColumnIndexOrThrow(_stmt, "userId")
        val _columnIndexOfIpAddress: Int = getColumnIndexOrThrow(_stmt, "ipAddress")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _result: MutableList<AuditLogEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: AuditLogEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpAction: String
          _tmpAction = _stmt.getText(_columnIndexOfAction)
          val _tmpCategory: String
          _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          val _tmpDetails: String
          _tmpDetails = _stmt.getText(_columnIndexOfDetails)
          val _tmpOldValue: String?
          if (_stmt.isNull(_columnIndexOfOldValue)) {
            _tmpOldValue = null
          } else {
            _tmpOldValue = _stmt.getText(_columnIndexOfOldValue)
          }
          val _tmpNewValue: String?
          if (_stmt.isNull(_columnIndexOfNewValue)) {
            _tmpNewValue = null
          } else {
            _tmpNewValue = _stmt.getText(_columnIndexOfNewValue)
          }
          val _tmpOrderId: String?
          if (_stmt.isNull(_columnIndexOfOrderId)) {
            _tmpOrderId = null
          } else {
            _tmpOrderId = _stmt.getText(_columnIndexOfOrderId)
          }
          val _tmpSymbol: String?
          if (_stmt.isNull(_columnIndexOfSymbol)) {
            _tmpSymbol = null
          } else {
            _tmpSymbol = _stmt.getText(_columnIndexOfSymbol)
          }
          val _tmpUserId: String
          _tmpUserId = _stmt.getText(_columnIndexOfUserId)
          val _tmpIpAddress: String?
          if (_stmt.isNull(_columnIndexOfIpAddress)) {
            _tmpIpAddress = null
          } else {
            _tmpIpAddress = _stmt.getText(_columnIndexOfIpAddress)
          }
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          _item =
              AuditLogEntity(_tmpId,_tmpAction,_tmpCategory,_tmpDetails,_tmpOldValue,_tmpNewValue,_tmpOrderId,_tmpSymbol,_tmpUserId,_tmpIpAddress,_tmpTimestamp)
          _result.add(_item)
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
