package com.tfg.`data`.local.dao

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.tfg.`data`.local.entity.AssetBalanceEntity
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
public class AssetBalanceDao_Impl(
  __db: RoomDatabase,
) : AssetBalanceDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfAssetBalanceEntity: EntityInsertAdapter<AssetBalanceEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfAssetBalanceEntity = object : EntityInsertAdapter<AssetBalanceEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `asset_balances` (`asset`,`free`,`locked`,`usdValue`,`allocationPercent`,`walletType`,`isPaper`,`updatedAt`) VALUES (?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: AssetBalanceEntity) {
        statement.bindText(1, entity.asset)
        statement.bindDouble(2, entity.free)
        statement.bindDouble(3, entity.locked)
        statement.bindDouble(4, entity.usdValue)
        statement.bindDouble(5, entity.allocationPercent)
        statement.bindText(6, entity.walletType)
        val _tmp: Int = if (entity.isPaper) 1 else 0
        statement.bindLong(7, _tmp.toLong())
        statement.bindLong(8, entity.updatedAt)
      }
    }
  }

  public override suspend fun insertAll(balances: List<AssetBalanceEntity>): Unit =
      performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfAssetBalanceEntity.insert(_connection, balances)
  }

  public override fun getAll(isPaper: Boolean): Flow<List<AssetBalanceEntity>> {
    val _sql: String = "SELECT * FROM asset_balances WHERE isPaper = ? ORDER BY usdValue DESC"
    return createFlow(__db, false, arrayOf("asset_balances")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        val _tmp: Int = if (isPaper) 1 else 0
        _stmt.bindLong(_argIndex, _tmp.toLong())
        val _columnIndexOfAsset: Int = getColumnIndexOrThrow(_stmt, "asset")
        val _columnIndexOfFree: Int = getColumnIndexOrThrow(_stmt, "free")
        val _columnIndexOfLocked: Int = getColumnIndexOrThrow(_stmt, "locked")
        val _columnIndexOfUsdValue: Int = getColumnIndexOrThrow(_stmt, "usdValue")
        val _columnIndexOfAllocationPercent: Int = getColumnIndexOrThrow(_stmt, "allocationPercent")
        val _columnIndexOfWalletType: Int = getColumnIndexOrThrow(_stmt, "walletType")
        val _columnIndexOfIsPaper: Int = getColumnIndexOrThrow(_stmt, "isPaper")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: MutableList<AssetBalanceEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: AssetBalanceEntity
          val _tmpAsset: String
          _tmpAsset = _stmt.getText(_columnIndexOfAsset)
          val _tmpFree: Double
          _tmpFree = _stmt.getDouble(_columnIndexOfFree)
          val _tmpLocked: Double
          _tmpLocked = _stmt.getDouble(_columnIndexOfLocked)
          val _tmpUsdValue: Double
          _tmpUsdValue = _stmt.getDouble(_columnIndexOfUsdValue)
          val _tmpAllocationPercent: Double
          _tmpAllocationPercent = _stmt.getDouble(_columnIndexOfAllocationPercent)
          val _tmpWalletType: String
          _tmpWalletType = _stmt.getText(_columnIndexOfWalletType)
          val _tmpIsPaper: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsPaper).toInt()
          _tmpIsPaper = _tmp_1 != 0
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _item =
              AssetBalanceEntity(_tmpAsset,_tmpFree,_tmpLocked,_tmpUsdValue,_tmpAllocationPercent,_tmpWalletType,_tmpIsPaper,_tmpUpdatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getByWalletType(walletType: String, isPaper: Boolean):
      Flow<List<AssetBalanceEntity>> {
    val _sql: String =
        "SELECT * FROM asset_balances WHERE walletType = ? AND isPaper = ? ORDER BY usdValue DESC"
    return createFlow(__db, false, arrayOf("asset_balances")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, walletType)
        _argIndex = 2
        val _tmp: Int = if (isPaper) 1 else 0
        _stmt.bindLong(_argIndex, _tmp.toLong())
        val _columnIndexOfAsset: Int = getColumnIndexOrThrow(_stmt, "asset")
        val _columnIndexOfFree: Int = getColumnIndexOrThrow(_stmt, "free")
        val _columnIndexOfLocked: Int = getColumnIndexOrThrow(_stmt, "locked")
        val _columnIndexOfUsdValue: Int = getColumnIndexOrThrow(_stmt, "usdValue")
        val _columnIndexOfAllocationPercent: Int = getColumnIndexOrThrow(_stmt, "allocationPercent")
        val _columnIndexOfWalletType: Int = getColumnIndexOrThrow(_stmt, "walletType")
        val _columnIndexOfIsPaper: Int = getColumnIndexOrThrow(_stmt, "isPaper")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: MutableList<AssetBalanceEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: AssetBalanceEntity
          val _tmpAsset: String
          _tmpAsset = _stmt.getText(_columnIndexOfAsset)
          val _tmpFree: Double
          _tmpFree = _stmt.getDouble(_columnIndexOfFree)
          val _tmpLocked: Double
          _tmpLocked = _stmt.getDouble(_columnIndexOfLocked)
          val _tmpUsdValue: Double
          _tmpUsdValue = _stmt.getDouble(_columnIndexOfUsdValue)
          val _tmpAllocationPercent: Double
          _tmpAllocationPercent = _stmt.getDouble(_columnIndexOfAllocationPercent)
          val _tmpWalletType: String
          _tmpWalletType = _stmt.getText(_columnIndexOfWalletType)
          val _tmpIsPaper: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsPaper).toInt()
          _tmpIsPaper = _tmp_1 != 0
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _item =
              AssetBalanceEntity(_tmpAsset,_tmpFree,_tmpLocked,_tmpUsdValue,_tmpAllocationPercent,_tmpWalletType,_tmpIsPaper,_tmpUpdatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteAll(isPaper: Boolean) {
    val _sql: String = "DELETE FROM asset_balances WHERE isPaper = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        val _tmp: Int = if (isPaper) 1 else 0
        _stmt.bindLong(_argIndex, _tmp.toLong())
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteByWalletType(walletType: String, isPaper: Boolean) {
    val _sql: String = "DELETE FROM asset_balances WHERE walletType = ? AND isPaper = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, walletType)
        _argIndex = 2
        val _tmp: Int = if (isPaper) 1 else 0
        _stmt.bindLong(_argIndex, _tmp.toLong())
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
