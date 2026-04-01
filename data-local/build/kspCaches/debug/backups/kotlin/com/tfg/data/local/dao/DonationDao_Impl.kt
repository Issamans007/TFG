package com.tfg.`data`.local.dao

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.tfg.`data`.local.entity.DonationEntity
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
public class DonationDao_Impl(
  __db: RoomDatabase,
) : DonationDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfDonationEntity: EntityInsertAdapter<DonationEntity>

  private val __updateAdapterOfDonationEntity: EntityDeleteOrUpdateAdapter<DonationEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfDonationEntity = object : EntityInsertAdapter<DonationEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `donations` (`id`,`orderId`,`amount`,`currency`,`ngoName`,`ngoId`,`status`,`timestamp`) VALUES (?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: DonationEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.orderId)
        statement.bindDouble(3, entity.amount)
        statement.bindText(4, entity.currency)
        statement.bindText(5, entity.ngoName)
        statement.bindText(6, entity.ngoId)
        statement.bindText(7, entity.status)
        statement.bindLong(8, entity.timestamp)
      }
    }
    this.__updateAdapterOfDonationEntity = object : EntityDeleteOrUpdateAdapter<DonationEntity>() {
      protected override fun createQuery(): String =
          "UPDATE OR ABORT `donations` SET `id` = ?,`orderId` = ?,`amount` = ?,`currency` = ?,`ngoName` = ?,`ngoId` = ?,`status` = ?,`timestamp` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: DonationEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.orderId)
        statement.bindDouble(3, entity.amount)
        statement.bindText(4, entity.currency)
        statement.bindText(5, entity.ngoName)
        statement.bindText(6, entity.ngoId)
        statement.bindText(7, entity.status)
        statement.bindLong(8, entity.timestamp)
        statement.bindText(9, entity.id)
      }
    }
  }

  public override suspend fun insert(donation: DonationEntity): Unit = performSuspending(__db,
      false, true) { _connection ->
    __insertAdapterOfDonationEntity.insert(_connection, donation)
  }

  public override suspend fun update(donation: DonationEntity): Unit = performSuspending(__db,
      false, true) { _connection ->
    __updateAdapterOfDonationEntity.handle(_connection, donation)
  }

  public override fun getAll(): Flow<List<DonationEntity>> {
    val _sql: String = "SELECT * FROM donations ORDER BY timestamp DESC"
    return createFlow(__db, false, arrayOf("donations")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfOrderId: Int = getColumnIndexOrThrow(_stmt, "orderId")
        val _columnIndexOfAmount: Int = getColumnIndexOrThrow(_stmt, "amount")
        val _columnIndexOfCurrency: Int = getColumnIndexOrThrow(_stmt, "currency")
        val _columnIndexOfNgoName: Int = getColumnIndexOrThrow(_stmt, "ngoName")
        val _columnIndexOfNgoId: Int = getColumnIndexOrThrow(_stmt, "ngoId")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _result: MutableList<DonationEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: DonationEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpOrderId: String
          _tmpOrderId = _stmt.getText(_columnIndexOfOrderId)
          val _tmpAmount: Double
          _tmpAmount = _stmt.getDouble(_columnIndexOfAmount)
          val _tmpCurrency: String
          _tmpCurrency = _stmt.getText(_columnIndexOfCurrency)
          val _tmpNgoName: String
          _tmpNgoName = _stmt.getText(_columnIndexOfNgoName)
          val _tmpNgoId: String
          _tmpNgoId = _stmt.getText(_columnIndexOfNgoId)
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          _item =
              DonationEntity(_tmpId,_tmpOrderId,_tmpAmount,_tmpCurrency,_tmpNgoName,_tmpNgoId,_tmpStatus,_tmpTimestamp)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getTotalDonated(): Flow<Double?> {
    val _sql: String = "SELECT SUM(amount) FROM donations WHERE status = 'CONFIRMED'"
    return createFlow(__db, false, arrayOf("donations")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
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
