package com.tfg.`data`.local.dao

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.tfg.`data`.local.entity.SignalEntity
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
public class SignalDao_Impl(
  __db: RoomDatabase,
) : SignalDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfSignalEntity: EntityInsertAdapter<SignalEntity>

  private val __updateAdapterOfSignalEntity: EntityDeleteOrUpdateAdapter<SignalEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfSignalEntity = object : EntityInsertAdapter<SignalEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `signals` (`id`,`symbol`,`side`,`entryPrice`,`takeProfitsJson`,`stopLossesJson`,`confidence`,`riskRewardRatio`,`expiresAt`,`receivedAt`,`status`,`hmacSignature`,`isExpired`,`wasExecuted`,`missedWhileOffline`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: SignalEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.symbol)
        statement.bindText(3, entity.side)
        statement.bindDouble(4, entity.entryPrice)
        statement.bindText(5, entity.takeProfitsJson)
        statement.bindText(6, entity.stopLossesJson)
        statement.bindDouble(7, entity.confidence)
        statement.bindDouble(8, entity.riskRewardRatio)
        statement.bindLong(9, entity.expiresAt)
        statement.bindLong(10, entity.receivedAt)
        statement.bindText(11, entity.status)
        statement.bindText(12, entity.hmacSignature)
        val _tmp: Int = if (entity.isExpired) 1 else 0
        statement.bindLong(13, _tmp.toLong())
        val _tmp_1: Int = if (entity.wasExecuted) 1 else 0
        statement.bindLong(14, _tmp_1.toLong())
        val _tmp_2: Int = if (entity.missedWhileOffline) 1 else 0
        statement.bindLong(15, _tmp_2.toLong())
      }
    }
    this.__updateAdapterOfSignalEntity = object : EntityDeleteOrUpdateAdapter<SignalEntity>() {
      protected override fun createQuery(): String =
          "UPDATE OR ABORT `signals` SET `id` = ?,`symbol` = ?,`side` = ?,`entryPrice` = ?,`takeProfitsJson` = ?,`stopLossesJson` = ?,`confidence` = ?,`riskRewardRatio` = ?,`expiresAt` = ?,`receivedAt` = ?,`status` = ?,`hmacSignature` = ?,`isExpired` = ?,`wasExecuted` = ?,`missedWhileOffline` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: SignalEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.symbol)
        statement.bindText(3, entity.side)
        statement.bindDouble(4, entity.entryPrice)
        statement.bindText(5, entity.takeProfitsJson)
        statement.bindText(6, entity.stopLossesJson)
        statement.bindDouble(7, entity.confidence)
        statement.bindDouble(8, entity.riskRewardRatio)
        statement.bindLong(9, entity.expiresAt)
        statement.bindLong(10, entity.receivedAt)
        statement.bindText(11, entity.status)
        statement.bindText(12, entity.hmacSignature)
        val _tmp: Int = if (entity.isExpired) 1 else 0
        statement.bindLong(13, _tmp.toLong())
        val _tmp_1: Int = if (entity.wasExecuted) 1 else 0
        statement.bindLong(14, _tmp_1.toLong())
        val _tmp_2: Int = if (entity.missedWhileOffline) 1 else 0
        statement.bindLong(15, _tmp_2.toLong())
        statement.bindText(16, entity.id)
      }
    }
  }

  public override suspend fun insert(signal: SignalEntity): Unit = performSuspending(__db, false,
      true) { _connection ->
    __insertAdapterOfSignalEntity.insert(_connection, signal)
  }

  public override suspend fun update(signal: SignalEntity): Unit = performSuspending(__db, false,
      true) { _connection ->
    __updateAdapterOfSignalEntity.handle(_connection, signal)
  }

  public override fun getSignals(): Flow<List<SignalEntity>> {
    val _sql: String = "SELECT * FROM signals ORDER BY receivedAt DESC"
    return createFlow(__db, false, arrayOf("signals")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfSymbol: Int = getColumnIndexOrThrow(_stmt, "symbol")
        val _columnIndexOfSide: Int = getColumnIndexOrThrow(_stmt, "side")
        val _columnIndexOfEntryPrice: Int = getColumnIndexOrThrow(_stmt, "entryPrice")
        val _columnIndexOfTakeProfitsJson: Int = getColumnIndexOrThrow(_stmt, "takeProfitsJson")
        val _columnIndexOfStopLossesJson: Int = getColumnIndexOrThrow(_stmt, "stopLossesJson")
        val _columnIndexOfConfidence: Int = getColumnIndexOrThrow(_stmt, "confidence")
        val _columnIndexOfRiskRewardRatio: Int = getColumnIndexOrThrow(_stmt, "riskRewardRatio")
        val _columnIndexOfExpiresAt: Int = getColumnIndexOrThrow(_stmt, "expiresAt")
        val _columnIndexOfReceivedAt: Int = getColumnIndexOrThrow(_stmt, "receivedAt")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfHmacSignature: Int = getColumnIndexOrThrow(_stmt, "hmacSignature")
        val _columnIndexOfIsExpired: Int = getColumnIndexOrThrow(_stmt, "isExpired")
        val _columnIndexOfWasExecuted: Int = getColumnIndexOrThrow(_stmt, "wasExecuted")
        val _columnIndexOfMissedWhileOffline: Int = getColumnIndexOrThrow(_stmt,
            "missedWhileOffline")
        val _result: MutableList<SignalEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: SignalEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpSymbol: String
          _tmpSymbol = _stmt.getText(_columnIndexOfSymbol)
          val _tmpSide: String
          _tmpSide = _stmt.getText(_columnIndexOfSide)
          val _tmpEntryPrice: Double
          _tmpEntryPrice = _stmt.getDouble(_columnIndexOfEntryPrice)
          val _tmpTakeProfitsJson: String
          _tmpTakeProfitsJson = _stmt.getText(_columnIndexOfTakeProfitsJson)
          val _tmpStopLossesJson: String
          _tmpStopLossesJson = _stmt.getText(_columnIndexOfStopLossesJson)
          val _tmpConfidence: Double
          _tmpConfidence = _stmt.getDouble(_columnIndexOfConfidence)
          val _tmpRiskRewardRatio: Double
          _tmpRiskRewardRatio = _stmt.getDouble(_columnIndexOfRiskRewardRatio)
          val _tmpExpiresAt: Long
          _tmpExpiresAt = _stmt.getLong(_columnIndexOfExpiresAt)
          val _tmpReceivedAt: Long
          _tmpReceivedAt = _stmt.getLong(_columnIndexOfReceivedAt)
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpHmacSignature: String
          _tmpHmacSignature = _stmt.getText(_columnIndexOfHmacSignature)
          val _tmpIsExpired: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsExpired).toInt()
          _tmpIsExpired = _tmp != 0
          val _tmpWasExecuted: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfWasExecuted).toInt()
          _tmpWasExecuted = _tmp_1 != 0
          val _tmpMissedWhileOffline: Boolean
          val _tmp_2: Int
          _tmp_2 = _stmt.getLong(_columnIndexOfMissedWhileOffline).toInt()
          _tmpMissedWhileOffline = _tmp_2 != 0
          _item =
              SignalEntity(_tmpId,_tmpSymbol,_tmpSide,_tmpEntryPrice,_tmpTakeProfitsJson,_tmpStopLossesJson,_tmpConfidence,_tmpRiskRewardRatio,_tmpExpiresAt,_tmpReceivedAt,_tmpStatus,_tmpHmacSignature,_tmpIsExpired,_tmpWasExecuted,_tmpMissedWhileOffline)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getActiveSignals(): Flow<List<SignalEntity>> {
    val _sql: String = "SELECT * FROM signals WHERE status = 'PENDING' AND isExpired = 0"
    return createFlow(__db, false, arrayOf("signals")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfSymbol: Int = getColumnIndexOrThrow(_stmt, "symbol")
        val _columnIndexOfSide: Int = getColumnIndexOrThrow(_stmt, "side")
        val _columnIndexOfEntryPrice: Int = getColumnIndexOrThrow(_stmt, "entryPrice")
        val _columnIndexOfTakeProfitsJson: Int = getColumnIndexOrThrow(_stmt, "takeProfitsJson")
        val _columnIndexOfStopLossesJson: Int = getColumnIndexOrThrow(_stmt, "stopLossesJson")
        val _columnIndexOfConfidence: Int = getColumnIndexOrThrow(_stmt, "confidence")
        val _columnIndexOfRiskRewardRatio: Int = getColumnIndexOrThrow(_stmt, "riskRewardRatio")
        val _columnIndexOfExpiresAt: Int = getColumnIndexOrThrow(_stmt, "expiresAt")
        val _columnIndexOfReceivedAt: Int = getColumnIndexOrThrow(_stmt, "receivedAt")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfHmacSignature: Int = getColumnIndexOrThrow(_stmt, "hmacSignature")
        val _columnIndexOfIsExpired: Int = getColumnIndexOrThrow(_stmt, "isExpired")
        val _columnIndexOfWasExecuted: Int = getColumnIndexOrThrow(_stmt, "wasExecuted")
        val _columnIndexOfMissedWhileOffline: Int = getColumnIndexOrThrow(_stmt,
            "missedWhileOffline")
        val _result: MutableList<SignalEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: SignalEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpSymbol: String
          _tmpSymbol = _stmt.getText(_columnIndexOfSymbol)
          val _tmpSide: String
          _tmpSide = _stmt.getText(_columnIndexOfSide)
          val _tmpEntryPrice: Double
          _tmpEntryPrice = _stmt.getDouble(_columnIndexOfEntryPrice)
          val _tmpTakeProfitsJson: String
          _tmpTakeProfitsJson = _stmt.getText(_columnIndexOfTakeProfitsJson)
          val _tmpStopLossesJson: String
          _tmpStopLossesJson = _stmt.getText(_columnIndexOfStopLossesJson)
          val _tmpConfidence: Double
          _tmpConfidence = _stmt.getDouble(_columnIndexOfConfidence)
          val _tmpRiskRewardRatio: Double
          _tmpRiskRewardRatio = _stmt.getDouble(_columnIndexOfRiskRewardRatio)
          val _tmpExpiresAt: Long
          _tmpExpiresAt = _stmt.getLong(_columnIndexOfExpiresAt)
          val _tmpReceivedAt: Long
          _tmpReceivedAt = _stmt.getLong(_columnIndexOfReceivedAt)
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpHmacSignature: String
          _tmpHmacSignature = _stmt.getText(_columnIndexOfHmacSignature)
          val _tmpIsExpired: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsExpired).toInt()
          _tmpIsExpired = _tmp != 0
          val _tmpWasExecuted: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfWasExecuted).toInt()
          _tmpWasExecuted = _tmp_1 != 0
          val _tmpMissedWhileOffline: Boolean
          val _tmp_2: Int
          _tmp_2 = _stmt.getLong(_columnIndexOfMissedWhileOffline).toInt()
          _tmpMissedWhileOffline = _tmp_2 != 0
          _item =
              SignalEntity(_tmpId,_tmpSymbol,_tmpSide,_tmpEntryPrice,_tmpTakeProfitsJson,_tmpStopLossesJson,_tmpConfidence,_tmpRiskRewardRatio,_tmpExpiresAt,_tmpReceivedAt,_tmpStatus,_tmpHmacSignature,_tmpIsExpired,_tmpWasExecuted,_tmpMissedWhileOffline)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getSignalById(id: String): Flow<SignalEntity?> {
    val _sql: String = "SELECT * FROM signals WHERE id = ?"
    return createFlow(__db, false, arrayOf("signals")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfSymbol: Int = getColumnIndexOrThrow(_stmt, "symbol")
        val _columnIndexOfSide: Int = getColumnIndexOrThrow(_stmt, "side")
        val _columnIndexOfEntryPrice: Int = getColumnIndexOrThrow(_stmt, "entryPrice")
        val _columnIndexOfTakeProfitsJson: Int = getColumnIndexOrThrow(_stmt, "takeProfitsJson")
        val _columnIndexOfStopLossesJson: Int = getColumnIndexOrThrow(_stmt, "stopLossesJson")
        val _columnIndexOfConfidence: Int = getColumnIndexOrThrow(_stmt, "confidence")
        val _columnIndexOfRiskRewardRatio: Int = getColumnIndexOrThrow(_stmt, "riskRewardRatio")
        val _columnIndexOfExpiresAt: Int = getColumnIndexOrThrow(_stmt, "expiresAt")
        val _columnIndexOfReceivedAt: Int = getColumnIndexOrThrow(_stmt, "receivedAt")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfHmacSignature: Int = getColumnIndexOrThrow(_stmt, "hmacSignature")
        val _columnIndexOfIsExpired: Int = getColumnIndexOrThrow(_stmt, "isExpired")
        val _columnIndexOfWasExecuted: Int = getColumnIndexOrThrow(_stmt, "wasExecuted")
        val _columnIndexOfMissedWhileOffline: Int = getColumnIndexOrThrow(_stmt,
            "missedWhileOffline")
        val _result: SignalEntity?
        if (_stmt.step()) {
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpSymbol: String
          _tmpSymbol = _stmt.getText(_columnIndexOfSymbol)
          val _tmpSide: String
          _tmpSide = _stmt.getText(_columnIndexOfSide)
          val _tmpEntryPrice: Double
          _tmpEntryPrice = _stmt.getDouble(_columnIndexOfEntryPrice)
          val _tmpTakeProfitsJson: String
          _tmpTakeProfitsJson = _stmt.getText(_columnIndexOfTakeProfitsJson)
          val _tmpStopLossesJson: String
          _tmpStopLossesJson = _stmt.getText(_columnIndexOfStopLossesJson)
          val _tmpConfidence: Double
          _tmpConfidence = _stmt.getDouble(_columnIndexOfConfidence)
          val _tmpRiskRewardRatio: Double
          _tmpRiskRewardRatio = _stmt.getDouble(_columnIndexOfRiskRewardRatio)
          val _tmpExpiresAt: Long
          _tmpExpiresAt = _stmt.getLong(_columnIndexOfExpiresAt)
          val _tmpReceivedAt: Long
          _tmpReceivedAt = _stmt.getLong(_columnIndexOfReceivedAt)
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpHmacSignature: String
          _tmpHmacSignature = _stmt.getText(_columnIndexOfHmacSignature)
          val _tmpIsExpired: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsExpired).toInt()
          _tmpIsExpired = _tmp != 0
          val _tmpWasExecuted: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfWasExecuted).toInt()
          _tmpWasExecuted = _tmp_1 != 0
          val _tmpMissedWhileOffline: Boolean
          val _tmp_2: Int
          _tmp_2 = _stmt.getLong(_columnIndexOfMissedWhileOffline).toInt()
          _tmpMissedWhileOffline = _tmp_2 != 0
          _result =
              SignalEntity(_tmpId,_tmpSymbol,_tmpSide,_tmpEntryPrice,_tmpTakeProfitsJson,_tmpStopLossesJson,_tmpConfidence,_tmpRiskRewardRatio,_tmpExpiresAt,_tmpReceivedAt,_tmpStatus,_tmpHmacSignature,_tmpIsExpired,_tmpWasExecuted,_tmpMissedWhileOffline)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getMissedSignals(): List<SignalEntity> {
    val _sql: String = "SELECT * FROM signals WHERE missedWhileOffline = 1 AND isExpired = 0"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfSymbol: Int = getColumnIndexOrThrow(_stmt, "symbol")
        val _columnIndexOfSide: Int = getColumnIndexOrThrow(_stmt, "side")
        val _columnIndexOfEntryPrice: Int = getColumnIndexOrThrow(_stmt, "entryPrice")
        val _columnIndexOfTakeProfitsJson: Int = getColumnIndexOrThrow(_stmt, "takeProfitsJson")
        val _columnIndexOfStopLossesJson: Int = getColumnIndexOrThrow(_stmt, "stopLossesJson")
        val _columnIndexOfConfidence: Int = getColumnIndexOrThrow(_stmt, "confidence")
        val _columnIndexOfRiskRewardRatio: Int = getColumnIndexOrThrow(_stmt, "riskRewardRatio")
        val _columnIndexOfExpiresAt: Int = getColumnIndexOrThrow(_stmt, "expiresAt")
        val _columnIndexOfReceivedAt: Int = getColumnIndexOrThrow(_stmt, "receivedAt")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfHmacSignature: Int = getColumnIndexOrThrow(_stmt, "hmacSignature")
        val _columnIndexOfIsExpired: Int = getColumnIndexOrThrow(_stmt, "isExpired")
        val _columnIndexOfWasExecuted: Int = getColumnIndexOrThrow(_stmt, "wasExecuted")
        val _columnIndexOfMissedWhileOffline: Int = getColumnIndexOrThrow(_stmt,
            "missedWhileOffline")
        val _result: MutableList<SignalEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: SignalEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpSymbol: String
          _tmpSymbol = _stmt.getText(_columnIndexOfSymbol)
          val _tmpSide: String
          _tmpSide = _stmt.getText(_columnIndexOfSide)
          val _tmpEntryPrice: Double
          _tmpEntryPrice = _stmt.getDouble(_columnIndexOfEntryPrice)
          val _tmpTakeProfitsJson: String
          _tmpTakeProfitsJson = _stmt.getText(_columnIndexOfTakeProfitsJson)
          val _tmpStopLossesJson: String
          _tmpStopLossesJson = _stmt.getText(_columnIndexOfStopLossesJson)
          val _tmpConfidence: Double
          _tmpConfidence = _stmt.getDouble(_columnIndexOfConfidence)
          val _tmpRiskRewardRatio: Double
          _tmpRiskRewardRatio = _stmt.getDouble(_columnIndexOfRiskRewardRatio)
          val _tmpExpiresAt: Long
          _tmpExpiresAt = _stmt.getLong(_columnIndexOfExpiresAt)
          val _tmpReceivedAt: Long
          _tmpReceivedAt = _stmt.getLong(_columnIndexOfReceivedAt)
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpHmacSignature: String
          _tmpHmacSignature = _stmt.getText(_columnIndexOfHmacSignature)
          val _tmpIsExpired: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsExpired).toInt()
          _tmpIsExpired = _tmp != 0
          val _tmpWasExecuted: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfWasExecuted).toInt()
          _tmpWasExecuted = _tmp_1 != 0
          val _tmpMissedWhileOffline: Boolean
          val _tmp_2: Int
          _tmp_2 = _stmt.getLong(_columnIndexOfMissedWhileOffline).toInt()
          _tmpMissedWhileOffline = _tmp_2 != 0
          _item =
              SignalEntity(_tmpId,_tmpSymbol,_tmpSide,_tmpEntryPrice,_tmpTakeProfitsJson,_tmpStopLossesJson,_tmpConfidence,_tmpRiskRewardRatio,_tmpExpiresAt,_tmpReceivedAt,_tmpStatus,_tmpHmacSignature,_tmpIsExpired,_tmpWasExecuted,_tmpMissedWhileOffline)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun updateStatus(id: String, status: String) {
    val _sql: String = "UPDATE signals SET status = ? WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, status)
        _argIndex = 2
        _stmt.bindText(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun expireOldSignals(now: Long) {
    val _sql: String =
        "UPDATE signals SET isExpired = 1, status = 'EXPIRED' WHERE expiresAt < ? AND isExpired = 0"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, now)
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
