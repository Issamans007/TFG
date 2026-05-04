package com.tfg.`data`.local.dao

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.tfg.`data`.local.entity.ScriptEntity
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
public class ScriptDao_Impl(
  __db: RoomDatabase,
) : ScriptDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfScriptEntity: EntityInsertAdapter<ScriptEntity>

  private val __updateAdapterOfScriptEntity: EntityDeleteOrUpdateAdapter<ScriptEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfScriptEntity = object : EntityInsertAdapter<ScriptEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `scripts` (`id`,`name`,`code`,`isActive`,`activeSymbol`,`strategyTemplateId`,`paramsJson`,`relatedSymbolsJson`,`lastRun`,`backtestResultJson`,`createdAt`,`updatedAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: ScriptEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.name)
        statement.bindText(3, entity.code)
        val _tmp: Int = if (entity.isActive) 1 else 0
        statement.bindLong(4, _tmp.toLong())
        val _tmpActiveSymbol: String? = entity.activeSymbol
        if (_tmpActiveSymbol == null) {
          statement.bindNull(5)
        } else {
          statement.bindText(5, _tmpActiveSymbol)
        }
        val _tmpStrategyTemplateId: String? = entity.strategyTemplateId
        if (_tmpStrategyTemplateId == null) {
          statement.bindNull(6)
        } else {
          statement.bindText(6, _tmpStrategyTemplateId)
        }
        val _tmpParamsJson: String? = entity.paramsJson
        if (_tmpParamsJson == null) {
          statement.bindNull(7)
        } else {
          statement.bindText(7, _tmpParamsJson)
        }
        val _tmpRelatedSymbolsJson: String? = entity.relatedSymbolsJson
        if (_tmpRelatedSymbolsJson == null) {
          statement.bindNull(8)
        } else {
          statement.bindText(8, _tmpRelatedSymbolsJson)
        }
        val _tmpLastRun: Long? = entity.lastRun
        if (_tmpLastRun == null) {
          statement.bindNull(9)
        } else {
          statement.bindLong(9, _tmpLastRun)
        }
        val _tmpBacktestResultJson: String? = entity.backtestResultJson
        if (_tmpBacktestResultJson == null) {
          statement.bindNull(10)
        } else {
          statement.bindText(10, _tmpBacktestResultJson)
        }
        statement.bindLong(11, entity.createdAt)
        statement.bindLong(12, entity.updatedAt)
      }
    }
    this.__updateAdapterOfScriptEntity = object : EntityDeleteOrUpdateAdapter<ScriptEntity>() {
      protected override fun createQuery(): String =
          "UPDATE OR ABORT `scripts` SET `id` = ?,`name` = ?,`code` = ?,`isActive` = ?,`activeSymbol` = ?,`strategyTemplateId` = ?,`paramsJson` = ?,`relatedSymbolsJson` = ?,`lastRun` = ?,`backtestResultJson` = ?,`createdAt` = ?,`updatedAt` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: ScriptEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.name)
        statement.bindText(3, entity.code)
        val _tmp: Int = if (entity.isActive) 1 else 0
        statement.bindLong(4, _tmp.toLong())
        val _tmpActiveSymbol: String? = entity.activeSymbol
        if (_tmpActiveSymbol == null) {
          statement.bindNull(5)
        } else {
          statement.bindText(5, _tmpActiveSymbol)
        }
        val _tmpStrategyTemplateId: String? = entity.strategyTemplateId
        if (_tmpStrategyTemplateId == null) {
          statement.bindNull(6)
        } else {
          statement.bindText(6, _tmpStrategyTemplateId)
        }
        val _tmpParamsJson: String? = entity.paramsJson
        if (_tmpParamsJson == null) {
          statement.bindNull(7)
        } else {
          statement.bindText(7, _tmpParamsJson)
        }
        val _tmpRelatedSymbolsJson: String? = entity.relatedSymbolsJson
        if (_tmpRelatedSymbolsJson == null) {
          statement.bindNull(8)
        } else {
          statement.bindText(8, _tmpRelatedSymbolsJson)
        }
        val _tmpLastRun: Long? = entity.lastRun
        if (_tmpLastRun == null) {
          statement.bindNull(9)
        } else {
          statement.bindLong(9, _tmpLastRun)
        }
        val _tmpBacktestResultJson: String? = entity.backtestResultJson
        if (_tmpBacktestResultJson == null) {
          statement.bindNull(10)
        } else {
          statement.bindText(10, _tmpBacktestResultJson)
        }
        statement.bindLong(11, entity.createdAt)
        statement.bindLong(12, entity.updatedAt)
        statement.bindText(13, entity.id)
      }
    }
  }

  public override suspend fun insert(script: ScriptEntity): Unit = performSuspending(__db, false,
      true) { _connection ->
    __insertAdapterOfScriptEntity.insert(_connection, script)
  }

  public override suspend fun update(script: ScriptEntity): Unit = performSuspending(__db, false,
      true) { _connection ->
    __updateAdapterOfScriptEntity.handle(_connection, script)
  }

  public override fun getAll(): Flow<List<ScriptEntity>> {
    val _sql: String = "SELECT * FROM scripts ORDER BY updatedAt DESC"
    return createFlow(__db, false, arrayOf("scripts")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfCode: Int = getColumnIndexOrThrow(_stmt, "code")
        val _columnIndexOfIsActive: Int = getColumnIndexOrThrow(_stmt, "isActive")
        val _columnIndexOfActiveSymbol: Int = getColumnIndexOrThrow(_stmt, "activeSymbol")
        val _columnIndexOfStrategyTemplateId: Int = getColumnIndexOrThrow(_stmt,
            "strategyTemplateId")
        val _columnIndexOfParamsJson: Int = getColumnIndexOrThrow(_stmt, "paramsJson")
        val _columnIndexOfRelatedSymbolsJson: Int = getColumnIndexOrThrow(_stmt,
            "relatedSymbolsJson")
        val _columnIndexOfLastRun: Int = getColumnIndexOrThrow(_stmt, "lastRun")
        val _columnIndexOfBacktestResultJson: Int = getColumnIndexOrThrow(_stmt,
            "backtestResultJson")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: MutableList<ScriptEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: ScriptEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpCode: String
          _tmpCode = _stmt.getText(_columnIndexOfCode)
          val _tmpIsActive: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsActive).toInt()
          _tmpIsActive = _tmp != 0
          val _tmpActiveSymbol: String?
          if (_stmt.isNull(_columnIndexOfActiveSymbol)) {
            _tmpActiveSymbol = null
          } else {
            _tmpActiveSymbol = _stmt.getText(_columnIndexOfActiveSymbol)
          }
          val _tmpStrategyTemplateId: String?
          if (_stmt.isNull(_columnIndexOfStrategyTemplateId)) {
            _tmpStrategyTemplateId = null
          } else {
            _tmpStrategyTemplateId = _stmt.getText(_columnIndexOfStrategyTemplateId)
          }
          val _tmpParamsJson: String?
          if (_stmt.isNull(_columnIndexOfParamsJson)) {
            _tmpParamsJson = null
          } else {
            _tmpParamsJson = _stmt.getText(_columnIndexOfParamsJson)
          }
          val _tmpRelatedSymbolsJson: String?
          if (_stmt.isNull(_columnIndexOfRelatedSymbolsJson)) {
            _tmpRelatedSymbolsJson = null
          } else {
            _tmpRelatedSymbolsJson = _stmt.getText(_columnIndexOfRelatedSymbolsJson)
          }
          val _tmpLastRun: Long?
          if (_stmt.isNull(_columnIndexOfLastRun)) {
            _tmpLastRun = null
          } else {
            _tmpLastRun = _stmt.getLong(_columnIndexOfLastRun)
          }
          val _tmpBacktestResultJson: String?
          if (_stmt.isNull(_columnIndexOfBacktestResultJson)) {
            _tmpBacktestResultJson = null
          } else {
            _tmpBacktestResultJson = _stmt.getText(_columnIndexOfBacktestResultJson)
          }
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _item =
              ScriptEntity(_tmpId,_tmpName,_tmpCode,_tmpIsActive,_tmpActiveSymbol,_tmpStrategyTemplateId,_tmpParamsJson,_tmpRelatedSymbolsJson,_tmpLastRun,_tmpBacktestResultJson,_tmpCreatedAt,_tmpUpdatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getById(id: String): Flow<ScriptEntity?> {
    val _sql: String = "SELECT * FROM scripts WHERE id = ?"
    return createFlow(__db, false, arrayOf("scripts")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfCode: Int = getColumnIndexOrThrow(_stmt, "code")
        val _columnIndexOfIsActive: Int = getColumnIndexOrThrow(_stmt, "isActive")
        val _columnIndexOfActiveSymbol: Int = getColumnIndexOrThrow(_stmt, "activeSymbol")
        val _columnIndexOfStrategyTemplateId: Int = getColumnIndexOrThrow(_stmt,
            "strategyTemplateId")
        val _columnIndexOfParamsJson: Int = getColumnIndexOrThrow(_stmt, "paramsJson")
        val _columnIndexOfRelatedSymbolsJson: Int = getColumnIndexOrThrow(_stmt,
            "relatedSymbolsJson")
        val _columnIndexOfLastRun: Int = getColumnIndexOrThrow(_stmt, "lastRun")
        val _columnIndexOfBacktestResultJson: Int = getColumnIndexOrThrow(_stmt,
            "backtestResultJson")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: ScriptEntity?
        if (_stmt.step()) {
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpCode: String
          _tmpCode = _stmt.getText(_columnIndexOfCode)
          val _tmpIsActive: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsActive).toInt()
          _tmpIsActive = _tmp != 0
          val _tmpActiveSymbol: String?
          if (_stmt.isNull(_columnIndexOfActiveSymbol)) {
            _tmpActiveSymbol = null
          } else {
            _tmpActiveSymbol = _stmt.getText(_columnIndexOfActiveSymbol)
          }
          val _tmpStrategyTemplateId: String?
          if (_stmt.isNull(_columnIndexOfStrategyTemplateId)) {
            _tmpStrategyTemplateId = null
          } else {
            _tmpStrategyTemplateId = _stmt.getText(_columnIndexOfStrategyTemplateId)
          }
          val _tmpParamsJson: String?
          if (_stmt.isNull(_columnIndexOfParamsJson)) {
            _tmpParamsJson = null
          } else {
            _tmpParamsJson = _stmt.getText(_columnIndexOfParamsJson)
          }
          val _tmpRelatedSymbolsJson: String?
          if (_stmt.isNull(_columnIndexOfRelatedSymbolsJson)) {
            _tmpRelatedSymbolsJson = null
          } else {
            _tmpRelatedSymbolsJson = _stmt.getText(_columnIndexOfRelatedSymbolsJson)
          }
          val _tmpLastRun: Long?
          if (_stmt.isNull(_columnIndexOfLastRun)) {
            _tmpLastRun = null
          } else {
            _tmpLastRun = _stmt.getLong(_columnIndexOfLastRun)
          }
          val _tmpBacktestResultJson: String?
          if (_stmt.isNull(_columnIndexOfBacktestResultJson)) {
            _tmpBacktestResultJson = null
          } else {
            _tmpBacktestResultJson = _stmt.getText(_columnIndexOfBacktestResultJson)
          }
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _result =
              ScriptEntity(_tmpId,_tmpName,_tmpCode,_tmpIsActive,_tmpActiveSymbol,_tmpStrategyTemplateId,_tmpParamsJson,_tmpRelatedSymbolsJson,_tmpLastRun,_tmpBacktestResultJson,_tmpCreatedAt,_tmpUpdatedAt)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getActive(): Flow<List<ScriptEntity>> {
    val _sql: String = "SELECT * FROM scripts WHERE isActive = 1"
    return createFlow(__db, false, arrayOf("scripts")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfCode: Int = getColumnIndexOrThrow(_stmt, "code")
        val _columnIndexOfIsActive: Int = getColumnIndexOrThrow(_stmt, "isActive")
        val _columnIndexOfActiveSymbol: Int = getColumnIndexOrThrow(_stmt, "activeSymbol")
        val _columnIndexOfStrategyTemplateId: Int = getColumnIndexOrThrow(_stmt,
            "strategyTemplateId")
        val _columnIndexOfParamsJson: Int = getColumnIndexOrThrow(_stmt, "paramsJson")
        val _columnIndexOfRelatedSymbolsJson: Int = getColumnIndexOrThrow(_stmt,
            "relatedSymbolsJson")
        val _columnIndexOfLastRun: Int = getColumnIndexOrThrow(_stmt, "lastRun")
        val _columnIndexOfBacktestResultJson: Int = getColumnIndexOrThrow(_stmt,
            "backtestResultJson")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: MutableList<ScriptEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: ScriptEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpCode: String
          _tmpCode = _stmt.getText(_columnIndexOfCode)
          val _tmpIsActive: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsActive).toInt()
          _tmpIsActive = _tmp != 0
          val _tmpActiveSymbol: String?
          if (_stmt.isNull(_columnIndexOfActiveSymbol)) {
            _tmpActiveSymbol = null
          } else {
            _tmpActiveSymbol = _stmt.getText(_columnIndexOfActiveSymbol)
          }
          val _tmpStrategyTemplateId: String?
          if (_stmt.isNull(_columnIndexOfStrategyTemplateId)) {
            _tmpStrategyTemplateId = null
          } else {
            _tmpStrategyTemplateId = _stmt.getText(_columnIndexOfStrategyTemplateId)
          }
          val _tmpParamsJson: String?
          if (_stmt.isNull(_columnIndexOfParamsJson)) {
            _tmpParamsJson = null
          } else {
            _tmpParamsJson = _stmt.getText(_columnIndexOfParamsJson)
          }
          val _tmpRelatedSymbolsJson: String?
          if (_stmt.isNull(_columnIndexOfRelatedSymbolsJson)) {
            _tmpRelatedSymbolsJson = null
          } else {
            _tmpRelatedSymbolsJson = _stmt.getText(_columnIndexOfRelatedSymbolsJson)
          }
          val _tmpLastRun: Long?
          if (_stmt.isNull(_columnIndexOfLastRun)) {
            _tmpLastRun = null
          } else {
            _tmpLastRun = _stmt.getLong(_columnIndexOfLastRun)
          }
          val _tmpBacktestResultJson: String?
          if (_stmt.isNull(_columnIndexOfBacktestResultJson)) {
            _tmpBacktestResultJson = null
          } else {
            _tmpBacktestResultJson = _stmt.getText(_columnIndexOfBacktestResultJson)
          }
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _item =
              ScriptEntity(_tmpId,_tmpName,_tmpCode,_tmpIsActive,_tmpActiveSymbol,_tmpStrategyTemplateId,_tmpParamsJson,_tmpRelatedSymbolsJson,_tmpLastRun,_tmpBacktestResultJson,_tmpCreatedAt,_tmpUpdatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getActiveScript(): Flow<ScriptEntity?> {
    val _sql: String = "SELECT * FROM scripts WHERE isActive = 1 LIMIT 1"
    return createFlow(__db, false, arrayOf("scripts")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfCode: Int = getColumnIndexOrThrow(_stmt, "code")
        val _columnIndexOfIsActive: Int = getColumnIndexOrThrow(_stmt, "isActive")
        val _columnIndexOfActiveSymbol: Int = getColumnIndexOrThrow(_stmt, "activeSymbol")
        val _columnIndexOfStrategyTemplateId: Int = getColumnIndexOrThrow(_stmt,
            "strategyTemplateId")
        val _columnIndexOfParamsJson: Int = getColumnIndexOrThrow(_stmt, "paramsJson")
        val _columnIndexOfRelatedSymbolsJson: Int = getColumnIndexOrThrow(_stmt,
            "relatedSymbolsJson")
        val _columnIndexOfLastRun: Int = getColumnIndexOrThrow(_stmt, "lastRun")
        val _columnIndexOfBacktestResultJson: Int = getColumnIndexOrThrow(_stmt,
            "backtestResultJson")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: ScriptEntity?
        if (_stmt.step()) {
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpCode: String
          _tmpCode = _stmt.getText(_columnIndexOfCode)
          val _tmpIsActive: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsActive).toInt()
          _tmpIsActive = _tmp != 0
          val _tmpActiveSymbol: String?
          if (_stmt.isNull(_columnIndexOfActiveSymbol)) {
            _tmpActiveSymbol = null
          } else {
            _tmpActiveSymbol = _stmt.getText(_columnIndexOfActiveSymbol)
          }
          val _tmpStrategyTemplateId: String?
          if (_stmt.isNull(_columnIndexOfStrategyTemplateId)) {
            _tmpStrategyTemplateId = null
          } else {
            _tmpStrategyTemplateId = _stmt.getText(_columnIndexOfStrategyTemplateId)
          }
          val _tmpParamsJson: String?
          if (_stmt.isNull(_columnIndexOfParamsJson)) {
            _tmpParamsJson = null
          } else {
            _tmpParamsJson = _stmt.getText(_columnIndexOfParamsJson)
          }
          val _tmpRelatedSymbolsJson: String?
          if (_stmt.isNull(_columnIndexOfRelatedSymbolsJson)) {
            _tmpRelatedSymbolsJson = null
          } else {
            _tmpRelatedSymbolsJson = _stmt.getText(_columnIndexOfRelatedSymbolsJson)
          }
          val _tmpLastRun: Long?
          if (_stmt.isNull(_columnIndexOfLastRun)) {
            _tmpLastRun = null
          } else {
            _tmpLastRun = _stmt.getLong(_columnIndexOfLastRun)
          }
          val _tmpBacktestResultJson: String?
          if (_stmt.isNull(_columnIndexOfBacktestResultJson)) {
            _tmpBacktestResultJson = null
          } else {
            _tmpBacktestResultJson = _stmt.getText(_columnIndexOfBacktestResultJson)
          }
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _result =
              ScriptEntity(_tmpId,_tmpName,_tmpCode,_tmpIsActive,_tmpActiveSymbol,_tmpStrategyTemplateId,_tmpParamsJson,_tmpRelatedSymbolsJson,_tmpLastRun,_tmpBacktestResultJson,_tmpCreatedAt,_tmpUpdatedAt)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getBlockedSymbols(): Flow<List<String>> {
    val _sql: String =
        "SELECT activeSymbol FROM scripts WHERE isActive = 1 AND activeSymbol IS NOT NULL"
    return createFlow(__db, false, arrayOf("scripts")) { _connection ->
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

  public override suspend fun deactivateAll() {
    val _sql: String = "UPDATE scripts SET isActive = 0, activeSymbol = NULL"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun activate(id: String, symbol: String) {
    val _sql: String = "UPDATE scripts SET isActive = 1, activeSymbol = ? WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, symbol)
        _argIndex = 2
        _stmt.bindText(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun delete(id: String) {
    val _sql: String = "DELETE FROM scripts WHERE id = ?"
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

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
