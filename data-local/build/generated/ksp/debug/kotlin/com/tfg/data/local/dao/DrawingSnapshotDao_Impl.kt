package com.tfg.`data`.local.dao

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.tfg.`data`.local.entity.DrawingSnapshotEntity
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class DrawingSnapshotDao_Impl(
  __db: RoomDatabase,
) : DrawingSnapshotDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfDrawingSnapshotEntity: EntityInsertAdapter<DrawingSnapshotEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfDrawingSnapshotEntity = object :
        EntityInsertAdapter<DrawingSnapshotEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `chart_drawings_snapshot` (`symbol`,`drawingsJson`,`updatedAt`) VALUES (?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: DrawingSnapshotEntity) {
        statement.bindText(1, entity.symbol)
        statement.bindText(2, entity.drawingsJson)
        statement.bindLong(3, entity.updatedAt)
      }
    }
  }

  public override suspend fun upsert(snapshot: DrawingSnapshotEntity): Unit =
      performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfDrawingSnapshotEntity.insert(_connection, snapshot)
  }

  public override suspend fun getForSymbol(symbol: String): DrawingSnapshotEntity? {
    val _sql: String = "SELECT * FROM chart_drawings_snapshot WHERE symbol = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, symbol)
        val _columnIndexOfSymbol: Int = getColumnIndexOrThrow(_stmt, "symbol")
        val _columnIndexOfDrawingsJson: Int = getColumnIndexOrThrow(_stmt, "drawingsJson")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: DrawingSnapshotEntity?
        if (_stmt.step()) {
          val _tmpSymbol: String
          _tmpSymbol = _stmt.getText(_columnIndexOfSymbol)
          val _tmpDrawingsJson: String
          _tmpDrawingsJson = _stmt.getText(_columnIndexOfDrawingsJson)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _result = DrawingSnapshotEntity(_tmpSymbol,_tmpDrawingsJson,_tmpUpdatedAt)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteForSymbol(symbol: String) {
    val _sql: String = "DELETE FROM chart_drawings_snapshot WHERE symbol = ?"
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

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
