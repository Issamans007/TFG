package com.tfg.`data`.local.dao

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.tfg.`data`.local.entity.CustomTemplateEntity
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
public class CustomTemplateDao_Impl(
  __db: RoomDatabase,
) : CustomTemplateDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfCustomTemplateEntity: EntityInsertAdapter<CustomTemplateEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfCustomTemplateEntity = object :
        EntityInsertAdapter<CustomTemplateEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `custom_templates` (`id`,`name`,`description`,`baseTemplateId`,`code`,`defaultParamsJson`,`createdAt`) VALUES (?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: CustomTemplateEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.name)
        statement.bindText(3, entity.description)
        statement.bindText(4, entity.baseTemplateId)
        statement.bindText(5, entity.code)
        val _tmpDefaultParamsJson: String? = entity.defaultParamsJson
        if (_tmpDefaultParamsJson == null) {
          statement.bindNull(6)
        } else {
          statement.bindText(6, _tmpDefaultParamsJson)
        }
        statement.bindLong(7, entity.createdAt)
      }
    }
  }

  public override suspend fun insert(template: CustomTemplateEntity): Unit = performSuspending(__db,
      false, true) { _connection ->
    __insertAdapterOfCustomTemplateEntity.insert(_connection, template)
  }

  public override fun getAll(): Flow<List<CustomTemplateEntity>> {
    val _sql: String = "SELECT * FROM custom_templates ORDER BY createdAt DESC"
    return createFlow(__db, false, arrayOf("custom_templates")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfBaseTemplateId: Int = getColumnIndexOrThrow(_stmt, "baseTemplateId")
        val _columnIndexOfCode: Int = getColumnIndexOrThrow(_stmt, "code")
        val _columnIndexOfDefaultParamsJson: Int = getColumnIndexOrThrow(_stmt, "defaultParamsJson")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _result: MutableList<CustomTemplateEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: CustomTemplateEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          val _tmpBaseTemplateId: String
          _tmpBaseTemplateId = _stmt.getText(_columnIndexOfBaseTemplateId)
          val _tmpCode: String
          _tmpCode = _stmt.getText(_columnIndexOfCode)
          val _tmpDefaultParamsJson: String?
          if (_stmt.isNull(_columnIndexOfDefaultParamsJson)) {
            _tmpDefaultParamsJson = null
          } else {
            _tmpDefaultParamsJson = _stmt.getText(_columnIndexOfDefaultParamsJson)
          }
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          _item =
              CustomTemplateEntity(_tmpId,_tmpName,_tmpDescription,_tmpBaseTemplateId,_tmpCode,_tmpDefaultParamsJson,_tmpCreatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun delete(id: String) {
    val _sql: String = "DELETE FROM custom_templates WHERE id = ?"
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
