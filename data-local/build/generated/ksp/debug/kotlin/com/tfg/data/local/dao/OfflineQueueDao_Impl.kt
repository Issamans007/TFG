package com.tfg.`data`.local.dao

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.getTotalChangedRows
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.tfg.`data`.local.entity.OfflineQueueEntity
import javax.`annotation`.processing.Generated
import kotlin.Boolean
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
public class OfflineQueueDao_Impl(
  __db: RoomDatabase,
) : OfflineQueueDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfOfflineQueueEntity: EntityInsertAdapter<OfflineQueueEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfOfflineQueueEntity = object : EntityInsertAdapter<OfflineQueueEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `offline_queue` (`id`,`signalJson`,`orderJson`,`action`,`priority`,`createdAt`,`retryCount`,`maxRetries`,`lastError`,`isProcessing`) VALUES (?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: OfflineQueueEntity) {
        statement.bindText(1, entity.id)
        val _tmpSignalJson: String? = entity.signalJson
        if (_tmpSignalJson == null) {
          statement.bindNull(2)
        } else {
          statement.bindText(2, _tmpSignalJson)
        }
        val _tmpOrderJson: String? = entity.orderJson
        if (_tmpOrderJson == null) {
          statement.bindNull(3)
        } else {
          statement.bindText(3, _tmpOrderJson)
        }
        statement.bindText(4, entity.action)
        statement.bindLong(5, entity.priority.toLong())
        statement.bindLong(6, entity.createdAt)
        statement.bindLong(7, entity.retryCount.toLong())
        statement.bindLong(8, entity.maxRetries.toLong())
        val _tmpLastError: String? = entity.lastError
        if (_tmpLastError == null) {
          statement.bindNull(9)
        } else {
          statement.bindText(9, _tmpLastError)
        }
        val _tmp: Int = if (entity.isProcessing) 1 else 0
        statement.bindLong(10, _tmp.toLong())
      }
    }
  }

  public override suspend fun insert(item: OfflineQueueEntity): Unit = performSuspending(__db,
      false, true) { _connection ->
    __insertAdapterOfOfflineQueueEntity.insert(_connection, item)
  }

  public override fun getAll(): Flow<List<OfflineQueueEntity>> {
    val _sql: String = "SELECT * FROM offline_queue ORDER BY priority DESC, createdAt ASC"
    return createFlow(__db, false, arrayOf("offline_queue")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfSignalJson: Int = getColumnIndexOrThrow(_stmt, "signalJson")
        val _columnIndexOfOrderJson: Int = getColumnIndexOrThrow(_stmt, "orderJson")
        val _columnIndexOfAction: Int = getColumnIndexOrThrow(_stmt, "action")
        val _columnIndexOfPriority: Int = getColumnIndexOrThrow(_stmt, "priority")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfRetryCount: Int = getColumnIndexOrThrow(_stmt, "retryCount")
        val _columnIndexOfMaxRetries: Int = getColumnIndexOrThrow(_stmt, "maxRetries")
        val _columnIndexOfLastError: Int = getColumnIndexOrThrow(_stmt, "lastError")
        val _columnIndexOfIsProcessing: Int = getColumnIndexOrThrow(_stmt, "isProcessing")
        val _result: MutableList<OfflineQueueEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: OfflineQueueEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpSignalJson: String?
          if (_stmt.isNull(_columnIndexOfSignalJson)) {
            _tmpSignalJson = null
          } else {
            _tmpSignalJson = _stmt.getText(_columnIndexOfSignalJson)
          }
          val _tmpOrderJson: String?
          if (_stmt.isNull(_columnIndexOfOrderJson)) {
            _tmpOrderJson = null
          } else {
            _tmpOrderJson = _stmt.getText(_columnIndexOfOrderJson)
          }
          val _tmpAction: String
          _tmpAction = _stmt.getText(_columnIndexOfAction)
          val _tmpPriority: Int
          _tmpPriority = _stmt.getLong(_columnIndexOfPriority).toInt()
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpRetryCount: Int
          _tmpRetryCount = _stmt.getLong(_columnIndexOfRetryCount).toInt()
          val _tmpMaxRetries: Int
          _tmpMaxRetries = _stmt.getLong(_columnIndexOfMaxRetries).toInt()
          val _tmpLastError: String?
          if (_stmt.isNull(_columnIndexOfLastError)) {
            _tmpLastError = null
          } else {
            _tmpLastError = _stmt.getText(_columnIndexOfLastError)
          }
          val _tmpIsProcessing: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsProcessing).toInt()
          _tmpIsProcessing = _tmp != 0
          _item =
              OfflineQueueEntity(_tmpId,_tmpSignalJson,_tmpOrderJson,_tmpAction,_tmpPriority,_tmpCreatedAt,_tmpRetryCount,_tmpMaxRetries,_tmpLastError,_tmpIsProcessing)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getCount(): Flow<Int> {
    val _sql: String = "SELECT COUNT(*) FROM offline_queue"
    return createFlow(__db, false, arrayOf("offline_queue")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Int
        if (_stmt.step()) {
          val _tmp: Int
          _tmp = _stmt.getLong(0).toInt()
          _result = _tmp
        } else {
          _result = 0
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getRetryable(): List<OfflineQueueEntity> {
    val _sql: String =
        "SELECT * FROM offline_queue WHERE retryCount < maxRetries AND isProcessing = 0 ORDER BY priority DESC, createdAt ASC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfSignalJson: Int = getColumnIndexOrThrow(_stmt, "signalJson")
        val _columnIndexOfOrderJson: Int = getColumnIndexOrThrow(_stmt, "orderJson")
        val _columnIndexOfAction: Int = getColumnIndexOrThrow(_stmt, "action")
        val _columnIndexOfPriority: Int = getColumnIndexOrThrow(_stmt, "priority")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfRetryCount: Int = getColumnIndexOrThrow(_stmt, "retryCount")
        val _columnIndexOfMaxRetries: Int = getColumnIndexOrThrow(_stmt, "maxRetries")
        val _columnIndexOfLastError: Int = getColumnIndexOrThrow(_stmt, "lastError")
        val _columnIndexOfIsProcessing: Int = getColumnIndexOrThrow(_stmt, "isProcessing")
        val _result: MutableList<OfflineQueueEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: OfflineQueueEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpSignalJson: String?
          if (_stmt.isNull(_columnIndexOfSignalJson)) {
            _tmpSignalJson = null
          } else {
            _tmpSignalJson = _stmt.getText(_columnIndexOfSignalJson)
          }
          val _tmpOrderJson: String?
          if (_stmt.isNull(_columnIndexOfOrderJson)) {
            _tmpOrderJson = null
          } else {
            _tmpOrderJson = _stmt.getText(_columnIndexOfOrderJson)
          }
          val _tmpAction: String
          _tmpAction = _stmt.getText(_columnIndexOfAction)
          val _tmpPriority: Int
          _tmpPriority = _stmt.getLong(_columnIndexOfPriority).toInt()
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpRetryCount: Int
          _tmpRetryCount = _stmt.getLong(_columnIndexOfRetryCount).toInt()
          val _tmpMaxRetries: Int
          _tmpMaxRetries = _stmt.getLong(_columnIndexOfMaxRetries).toInt()
          val _tmpLastError: String?
          if (_stmt.isNull(_columnIndexOfLastError)) {
            _tmpLastError = null
          } else {
            _tmpLastError = _stmt.getText(_columnIndexOfLastError)
          }
          val _tmpIsProcessing: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsProcessing).toInt()
          _tmpIsProcessing = _tmp != 0
          _item =
              OfflineQueueEntity(_tmpId,_tmpSignalJson,_tmpOrderJson,_tmpAction,_tmpPriority,_tmpCreatedAt,_tmpRetryCount,_tmpMaxRetries,_tmpLastError,_tmpIsProcessing)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun delete(id: String) {
    val _sql: String = "DELETE FROM offline_queue WHERE id = ?"
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

  public override suspend fun claimForProcessing(id: String): Int {
    val _sql: String = "UPDATE offline_queue SET isProcessing = 1 WHERE id = ? AND isProcessing = 0"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        _stmt.step()
        getTotalChangedRows(_connection)
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun markFailed(id: String, error: String) {
    val _sql: String =
        "UPDATE offline_queue SET retryCount = retryCount + 1, lastError = ?, isProcessing = 0 WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, error)
        _argIndex = 2
        _stmt.bindText(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun clearDeadLetters() {
    val _sql: String = "DELETE FROM offline_queue WHERE retryCount >= maxRetries"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
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
