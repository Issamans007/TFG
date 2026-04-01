package com.tfg.`data`.local.dao

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.tfg.`data`.local.entity.OrderEntity
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
public class OrderDao_Impl(
  __db: RoomDatabase,
) : OrderDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfOrderEntity: EntityInsertAdapter<OrderEntity>

  private val __updateAdapterOfOrderEntity: EntityDeleteOrUpdateAdapter<OrderEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfOrderEntity = object : EntityInsertAdapter<OrderEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `orders` (`id`,`signalId`,`symbol`,`side`,`type`,`status`,`executionMode`,`quantity`,`price`,`stopPrice`,`takeProfitsJson`,`stopLossesJson`,`trailingStopPercent`,`trailingStopActivationPrice`,`ocoLinkedOrderId`,`bracketParentId`,`timeInForce`,`scheduledAt`,`filledQuantity`,`filledPrice`,`fee`,`feeAsset`,`donationAmount`,`realizedPnl`,`slippage`,`isPaperTrade`,`createdAt`,`updatedAt`,`executedAt`,`closedAt`,`binanceOrderId`,`errorMessage`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: OrderEntity) {
        statement.bindText(1, entity.id)
        val _tmpSignalId: String? = entity.signalId
        if (_tmpSignalId == null) {
          statement.bindNull(2)
        } else {
          statement.bindText(2, _tmpSignalId)
        }
        statement.bindText(3, entity.symbol)
        statement.bindText(4, entity.side)
        statement.bindText(5, entity.type)
        statement.bindText(6, entity.status)
        statement.bindText(7, entity.executionMode)
        statement.bindDouble(8, entity.quantity)
        val _tmpPrice: Double? = entity.price
        if (_tmpPrice == null) {
          statement.bindNull(9)
        } else {
          statement.bindDouble(9, _tmpPrice)
        }
        val _tmpStopPrice: Double? = entity.stopPrice
        if (_tmpStopPrice == null) {
          statement.bindNull(10)
        } else {
          statement.bindDouble(10, _tmpStopPrice)
        }
        statement.bindText(11, entity.takeProfitsJson)
        statement.bindText(12, entity.stopLossesJson)
        val _tmpTrailingStopPercent: Double? = entity.trailingStopPercent
        if (_tmpTrailingStopPercent == null) {
          statement.bindNull(13)
        } else {
          statement.bindDouble(13, _tmpTrailingStopPercent)
        }
        val _tmpTrailingStopActivationPrice: Double? = entity.trailingStopActivationPrice
        if (_tmpTrailingStopActivationPrice == null) {
          statement.bindNull(14)
        } else {
          statement.bindDouble(14, _tmpTrailingStopActivationPrice)
        }
        val _tmpOcoLinkedOrderId: String? = entity.ocoLinkedOrderId
        if (_tmpOcoLinkedOrderId == null) {
          statement.bindNull(15)
        } else {
          statement.bindText(15, _tmpOcoLinkedOrderId)
        }
        val _tmpBracketParentId: String? = entity.bracketParentId
        if (_tmpBracketParentId == null) {
          statement.bindNull(16)
        } else {
          statement.bindText(16, _tmpBracketParentId)
        }
        statement.bindText(17, entity.timeInForce)
        val _tmpScheduledAt: Long? = entity.scheduledAt
        if (_tmpScheduledAt == null) {
          statement.bindNull(18)
        } else {
          statement.bindLong(18, _tmpScheduledAt)
        }
        statement.bindDouble(19, entity.filledQuantity)
        statement.bindDouble(20, entity.filledPrice)
        statement.bindDouble(21, entity.fee)
        statement.bindText(22, entity.feeAsset)
        statement.bindDouble(23, entity.donationAmount)
        statement.bindDouble(24, entity.realizedPnl)
        statement.bindDouble(25, entity.slippage)
        val _tmp: Int = if (entity.isPaperTrade) 1 else 0
        statement.bindLong(26, _tmp.toLong())
        statement.bindLong(27, entity.createdAt)
        statement.bindLong(28, entity.updatedAt)
        val _tmpExecutedAt: Long? = entity.executedAt
        if (_tmpExecutedAt == null) {
          statement.bindNull(29)
        } else {
          statement.bindLong(29, _tmpExecutedAt)
        }
        val _tmpClosedAt: Long? = entity.closedAt
        if (_tmpClosedAt == null) {
          statement.bindNull(30)
        } else {
          statement.bindLong(30, _tmpClosedAt)
        }
        val _tmpBinanceOrderId: Long? = entity.binanceOrderId
        if (_tmpBinanceOrderId == null) {
          statement.bindNull(31)
        } else {
          statement.bindLong(31, _tmpBinanceOrderId)
        }
        val _tmpErrorMessage: String? = entity.errorMessage
        if (_tmpErrorMessage == null) {
          statement.bindNull(32)
        } else {
          statement.bindText(32, _tmpErrorMessage)
        }
      }
    }
    this.__updateAdapterOfOrderEntity = object : EntityDeleteOrUpdateAdapter<OrderEntity>() {
      protected override fun createQuery(): String =
          "UPDATE OR ABORT `orders` SET `id` = ?,`signalId` = ?,`symbol` = ?,`side` = ?,`type` = ?,`status` = ?,`executionMode` = ?,`quantity` = ?,`price` = ?,`stopPrice` = ?,`takeProfitsJson` = ?,`stopLossesJson` = ?,`trailingStopPercent` = ?,`trailingStopActivationPrice` = ?,`ocoLinkedOrderId` = ?,`bracketParentId` = ?,`timeInForce` = ?,`scheduledAt` = ?,`filledQuantity` = ?,`filledPrice` = ?,`fee` = ?,`feeAsset` = ?,`donationAmount` = ?,`realizedPnl` = ?,`slippage` = ?,`isPaperTrade` = ?,`createdAt` = ?,`updatedAt` = ?,`executedAt` = ?,`closedAt` = ?,`binanceOrderId` = ?,`errorMessage` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: OrderEntity) {
        statement.bindText(1, entity.id)
        val _tmpSignalId: String? = entity.signalId
        if (_tmpSignalId == null) {
          statement.bindNull(2)
        } else {
          statement.bindText(2, _tmpSignalId)
        }
        statement.bindText(3, entity.symbol)
        statement.bindText(4, entity.side)
        statement.bindText(5, entity.type)
        statement.bindText(6, entity.status)
        statement.bindText(7, entity.executionMode)
        statement.bindDouble(8, entity.quantity)
        val _tmpPrice: Double? = entity.price
        if (_tmpPrice == null) {
          statement.bindNull(9)
        } else {
          statement.bindDouble(9, _tmpPrice)
        }
        val _tmpStopPrice: Double? = entity.stopPrice
        if (_tmpStopPrice == null) {
          statement.bindNull(10)
        } else {
          statement.bindDouble(10, _tmpStopPrice)
        }
        statement.bindText(11, entity.takeProfitsJson)
        statement.bindText(12, entity.stopLossesJson)
        val _tmpTrailingStopPercent: Double? = entity.trailingStopPercent
        if (_tmpTrailingStopPercent == null) {
          statement.bindNull(13)
        } else {
          statement.bindDouble(13, _tmpTrailingStopPercent)
        }
        val _tmpTrailingStopActivationPrice: Double? = entity.trailingStopActivationPrice
        if (_tmpTrailingStopActivationPrice == null) {
          statement.bindNull(14)
        } else {
          statement.bindDouble(14, _tmpTrailingStopActivationPrice)
        }
        val _tmpOcoLinkedOrderId: String? = entity.ocoLinkedOrderId
        if (_tmpOcoLinkedOrderId == null) {
          statement.bindNull(15)
        } else {
          statement.bindText(15, _tmpOcoLinkedOrderId)
        }
        val _tmpBracketParentId: String? = entity.bracketParentId
        if (_tmpBracketParentId == null) {
          statement.bindNull(16)
        } else {
          statement.bindText(16, _tmpBracketParentId)
        }
        statement.bindText(17, entity.timeInForce)
        val _tmpScheduledAt: Long? = entity.scheduledAt
        if (_tmpScheduledAt == null) {
          statement.bindNull(18)
        } else {
          statement.bindLong(18, _tmpScheduledAt)
        }
        statement.bindDouble(19, entity.filledQuantity)
        statement.bindDouble(20, entity.filledPrice)
        statement.bindDouble(21, entity.fee)
        statement.bindText(22, entity.feeAsset)
        statement.bindDouble(23, entity.donationAmount)
        statement.bindDouble(24, entity.realizedPnl)
        statement.bindDouble(25, entity.slippage)
        val _tmp: Int = if (entity.isPaperTrade) 1 else 0
        statement.bindLong(26, _tmp.toLong())
        statement.bindLong(27, entity.createdAt)
        statement.bindLong(28, entity.updatedAt)
        val _tmpExecutedAt: Long? = entity.executedAt
        if (_tmpExecutedAt == null) {
          statement.bindNull(29)
        } else {
          statement.bindLong(29, _tmpExecutedAt)
        }
        val _tmpClosedAt: Long? = entity.closedAt
        if (_tmpClosedAt == null) {
          statement.bindNull(30)
        } else {
          statement.bindLong(30, _tmpClosedAt)
        }
        val _tmpBinanceOrderId: Long? = entity.binanceOrderId
        if (_tmpBinanceOrderId == null) {
          statement.bindNull(31)
        } else {
          statement.bindLong(31, _tmpBinanceOrderId)
        }
        val _tmpErrorMessage: String? = entity.errorMessage
        if (_tmpErrorMessage == null) {
          statement.bindNull(32)
        } else {
          statement.bindText(32, _tmpErrorMessage)
        }
        statement.bindText(33, entity.id)
      }
    }
  }

  public override suspend fun insert(order: OrderEntity): Unit = performSuspending(__db, false,
      true) { _connection ->
    __insertAdapterOfOrderEntity.insert(_connection, order)
  }

  public override suspend fun insertAll(orders: List<OrderEntity>): Unit = performSuspending(__db,
      false, true) { _connection ->
    __insertAdapterOfOrderEntity.insert(_connection, orders)
  }

  public override suspend fun update(order: OrderEntity): Unit = performSuspending(__db, false,
      true) { _connection ->
    __updateAdapterOfOrderEntity.handle(_connection, order)
  }

  public override fun getOpenOrders(): Flow<List<OrderEntity>> {
    val _sql: String =
        "SELECT * FROM orders WHERE status IN ('PENDING','SUBMITTED','PARTIALLY_FILLED') ORDER BY createdAt DESC"
    return createFlow(__db, false, arrayOf("orders")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfSignalId: Int = getColumnIndexOrThrow(_stmt, "signalId")
        val _columnIndexOfSymbol: Int = getColumnIndexOrThrow(_stmt, "symbol")
        val _columnIndexOfSide: Int = getColumnIndexOrThrow(_stmt, "side")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfExecutionMode: Int = getColumnIndexOrThrow(_stmt, "executionMode")
        val _columnIndexOfQuantity: Int = getColumnIndexOrThrow(_stmt, "quantity")
        val _columnIndexOfPrice: Int = getColumnIndexOrThrow(_stmt, "price")
        val _columnIndexOfStopPrice: Int = getColumnIndexOrThrow(_stmt, "stopPrice")
        val _columnIndexOfTakeProfitsJson: Int = getColumnIndexOrThrow(_stmt, "takeProfitsJson")
        val _columnIndexOfStopLossesJson: Int = getColumnIndexOrThrow(_stmt, "stopLossesJson")
        val _columnIndexOfTrailingStopPercent: Int = getColumnIndexOrThrow(_stmt,
            "trailingStopPercent")
        val _columnIndexOfTrailingStopActivationPrice: Int = getColumnIndexOrThrow(_stmt,
            "trailingStopActivationPrice")
        val _columnIndexOfOcoLinkedOrderId: Int = getColumnIndexOrThrow(_stmt, "ocoLinkedOrderId")
        val _columnIndexOfBracketParentId: Int = getColumnIndexOrThrow(_stmt, "bracketParentId")
        val _columnIndexOfTimeInForce: Int = getColumnIndexOrThrow(_stmt, "timeInForce")
        val _columnIndexOfScheduledAt: Int = getColumnIndexOrThrow(_stmt, "scheduledAt")
        val _columnIndexOfFilledQuantity: Int = getColumnIndexOrThrow(_stmt, "filledQuantity")
        val _columnIndexOfFilledPrice: Int = getColumnIndexOrThrow(_stmt, "filledPrice")
        val _columnIndexOfFee: Int = getColumnIndexOrThrow(_stmt, "fee")
        val _columnIndexOfFeeAsset: Int = getColumnIndexOrThrow(_stmt, "feeAsset")
        val _columnIndexOfDonationAmount: Int = getColumnIndexOrThrow(_stmt, "donationAmount")
        val _columnIndexOfRealizedPnl: Int = getColumnIndexOrThrow(_stmt, "realizedPnl")
        val _columnIndexOfSlippage: Int = getColumnIndexOrThrow(_stmt, "slippage")
        val _columnIndexOfIsPaperTrade: Int = getColumnIndexOrThrow(_stmt, "isPaperTrade")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfExecutedAt: Int = getColumnIndexOrThrow(_stmt, "executedAt")
        val _columnIndexOfClosedAt: Int = getColumnIndexOrThrow(_stmt, "closedAt")
        val _columnIndexOfBinanceOrderId: Int = getColumnIndexOrThrow(_stmt, "binanceOrderId")
        val _columnIndexOfErrorMessage: Int = getColumnIndexOrThrow(_stmt, "errorMessage")
        val _result: MutableList<OrderEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: OrderEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpSignalId: String?
          if (_stmt.isNull(_columnIndexOfSignalId)) {
            _tmpSignalId = null
          } else {
            _tmpSignalId = _stmt.getText(_columnIndexOfSignalId)
          }
          val _tmpSymbol: String
          _tmpSymbol = _stmt.getText(_columnIndexOfSymbol)
          val _tmpSide: String
          _tmpSide = _stmt.getText(_columnIndexOfSide)
          val _tmpType: String
          _tmpType = _stmt.getText(_columnIndexOfType)
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpExecutionMode: String
          _tmpExecutionMode = _stmt.getText(_columnIndexOfExecutionMode)
          val _tmpQuantity: Double
          _tmpQuantity = _stmt.getDouble(_columnIndexOfQuantity)
          val _tmpPrice: Double?
          if (_stmt.isNull(_columnIndexOfPrice)) {
            _tmpPrice = null
          } else {
            _tmpPrice = _stmt.getDouble(_columnIndexOfPrice)
          }
          val _tmpStopPrice: Double?
          if (_stmt.isNull(_columnIndexOfStopPrice)) {
            _tmpStopPrice = null
          } else {
            _tmpStopPrice = _stmt.getDouble(_columnIndexOfStopPrice)
          }
          val _tmpTakeProfitsJson: String
          _tmpTakeProfitsJson = _stmt.getText(_columnIndexOfTakeProfitsJson)
          val _tmpStopLossesJson: String
          _tmpStopLossesJson = _stmt.getText(_columnIndexOfStopLossesJson)
          val _tmpTrailingStopPercent: Double?
          if (_stmt.isNull(_columnIndexOfTrailingStopPercent)) {
            _tmpTrailingStopPercent = null
          } else {
            _tmpTrailingStopPercent = _stmt.getDouble(_columnIndexOfTrailingStopPercent)
          }
          val _tmpTrailingStopActivationPrice: Double?
          if (_stmt.isNull(_columnIndexOfTrailingStopActivationPrice)) {
            _tmpTrailingStopActivationPrice = null
          } else {
            _tmpTrailingStopActivationPrice =
                _stmt.getDouble(_columnIndexOfTrailingStopActivationPrice)
          }
          val _tmpOcoLinkedOrderId: String?
          if (_stmt.isNull(_columnIndexOfOcoLinkedOrderId)) {
            _tmpOcoLinkedOrderId = null
          } else {
            _tmpOcoLinkedOrderId = _stmt.getText(_columnIndexOfOcoLinkedOrderId)
          }
          val _tmpBracketParentId: String?
          if (_stmt.isNull(_columnIndexOfBracketParentId)) {
            _tmpBracketParentId = null
          } else {
            _tmpBracketParentId = _stmt.getText(_columnIndexOfBracketParentId)
          }
          val _tmpTimeInForce: String
          _tmpTimeInForce = _stmt.getText(_columnIndexOfTimeInForce)
          val _tmpScheduledAt: Long?
          if (_stmt.isNull(_columnIndexOfScheduledAt)) {
            _tmpScheduledAt = null
          } else {
            _tmpScheduledAt = _stmt.getLong(_columnIndexOfScheduledAt)
          }
          val _tmpFilledQuantity: Double
          _tmpFilledQuantity = _stmt.getDouble(_columnIndexOfFilledQuantity)
          val _tmpFilledPrice: Double
          _tmpFilledPrice = _stmt.getDouble(_columnIndexOfFilledPrice)
          val _tmpFee: Double
          _tmpFee = _stmt.getDouble(_columnIndexOfFee)
          val _tmpFeeAsset: String
          _tmpFeeAsset = _stmt.getText(_columnIndexOfFeeAsset)
          val _tmpDonationAmount: Double
          _tmpDonationAmount = _stmt.getDouble(_columnIndexOfDonationAmount)
          val _tmpRealizedPnl: Double
          _tmpRealizedPnl = _stmt.getDouble(_columnIndexOfRealizedPnl)
          val _tmpSlippage: Double
          _tmpSlippage = _stmt.getDouble(_columnIndexOfSlippage)
          val _tmpIsPaperTrade: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsPaperTrade).toInt()
          _tmpIsPaperTrade = _tmp != 0
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpExecutedAt: Long?
          if (_stmt.isNull(_columnIndexOfExecutedAt)) {
            _tmpExecutedAt = null
          } else {
            _tmpExecutedAt = _stmt.getLong(_columnIndexOfExecutedAt)
          }
          val _tmpClosedAt: Long?
          if (_stmt.isNull(_columnIndexOfClosedAt)) {
            _tmpClosedAt = null
          } else {
            _tmpClosedAt = _stmt.getLong(_columnIndexOfClosedAt)
          }
          val _tmpBinanceOrderId: Long?
          if (_stmt.isNull(_columnIndexOfBinanceOrderId)) {
            _tmpBinanceOrderId = null
          } else {
            _tmpBinanceOrderId = _stmt.getLong(_columnIndexOfBinanceOrderId)
          }
          val _tmpErrorMessage: String?
          if (_stmt.isNull(_columnIndexOfErrorMessage)) {
            _tmpErrorMessage = null
          } else {
            _tmpErrorMessage = _stmt.getText(_columnIndexOfErrorMessage)
          }
          _item =
              OrderEntity(_tmpId,_tmpSignalId,_tmpSymbol,_tmpSide,_tmpType,_tmpStatus,_tmpExecutionMode,_tmpQuantity,_tmpPrice,_tmpStopPrice,_tmpTakeProfitsJson,_tmpStopLossesJson,_tmpTrailingStopPercent,_tmpTrailingStopActivationPrice,_tmpOcoLinkedOrderId,_tmpBracketParentId,_tmpTimeInForce,_tmpScheduledAt,_tmpFilledQuantity,_tmpFilledPrice,_tmpFee,_tmpFeeAsset,_tmpDonationAmount,_tmpRealizedPnl,_tmpSlippage,_tmpIsPaperTrade,_tmpCreatedAt,_tmpUpdatedAt,_tmpExecutedAt,_tmpClosedAt,_tmpBinanceOrderId,_tmpErrorMessage)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getOrderHistory(limit: Int): Flow<List<OrderEntity>> {
    val _sql: String = "SELECT * FROM orders ORDER BY createdAt DESC LIMIT ?"
    return createFlow(__db, false, arrayOf("orders")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, limit.toLong())
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfSignalId: Int = getColumnIndexOrThrow(_stmt, "signalId")
        val _columnIndexOfSymbol: Int = getColumnIndexOrThrow(_stmt, "symbol")
        val _columnIndexOfSide: Int = getColumnIndexOrThrow(_stmt, "side")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfExecutionMode: Int = getColumnIndexOrThrow(_stmt, "executionMode")
        val _columnIndexOfQuantity: Int = getColumnIndexOrThrow(_stmt, "quantity")
        val _columnIndexOfPrice: Int = getColumnIndexOrThrow(_stmt, "price")
        val _columnIndexOfStopPrice: Int = getColumnIndexOrThrow(_stmt, "stopPrice")
        val _columnIndexOfTakeProfitsJson: Int = getColumnIndexOrThrow(_stmt, "takeProfitsJson")
        val _columnIndexOfStopLossesJson: Int = getColumnIndexOrThrow(_stmt, "stopLossesJson")
        val _columnIndexOfTrailingStopPercent: Int = getColumnIndexOrThrow(_stmt,
            "trailingStopPercent")
        val _columnIndexOfTrailingStopActivationPrice: Int = getColumnIndexOrThrow(_stmt,
            "trailingStopActivationPrice")
        val _columnIndexOfOcoLinkedOrderId: Int = getColumnIndexOrThrow(_stmt, "ocoLinkedOrderId")
        val _columnIndexOfBracketParentId: Int = getColumnIndexOrThrow(_stmt, "bracketParentId")
        val _columnIndexOfTimeInForce: Int = getColumnIndexOrThrow(_stmt, "timeInForce")
        val _columnIndexOfScheduledAt: Int = getColumnIndexOrThrow(_stmt, "scheduledAt")
        val _columnIndexOfFilledQuantity: Int = getColumnIndexOrThrow(_stmt, "filledQuantity")
        val _columnIndexOfFilledPrice: Int = getColumnIndexOrThrow(_stmt, "filledPrice")
        val _columnIndexOfFee: Int = getColumnIndexOrThrow(_stmt, "fee")
        val _columnIndexOfFeeAsset: Int = getColumnIndexOrThrow(_stmt, "feeAsset")
        val _columnIndexOfDonationAmount: Int = getColumnIndexOrThrow(_stmt, "donationAmount")
        val _columnIndexOfRealizedPnl: Int = getColumnIndexOrThrow(_stmt, "realizedPnl")
        val _columnIndexOfSlippage: Int = getColumnIndexOrThrow(_stmt, "slippage")
        val _columnIndexOfIsPaperTrade: Int = getColumnIndexOrThrow(_stmt, "isPaperTrade")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfExecutedAt: Int = getColumnIndexOrThrow(_stmt, "executedAt")
        val _columnIndexOfClosedAt: Int = getColumnIndexOrThrow(_stmt, "closedAt")
        val _columnIndexOfBinanceOrderId: Int = getColumnIndexOrThrow(_stmt, "binanceOrderId")
        val _columnIndexOfErrorMessage: Int = getColumnIndexOrThrow(_stmt, "errorMessage")
        val _result: MutableList<OrderEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: OrderEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpSignalId: String?
          if (_stmt.isNull(_columnIndexOfSignalId)) {
            _tmpSignalId = null
          } else {
            _tmpSignalId = _stmt.getText(_columnIndexOfSignalId)
          }
          val _tmpSymbol: String
          _tmpSymbol = _stmt.getText(_columnIndexOfSymbol)
          val _tmpSide: String
          _tmpSide = _stmt.getText(_columnIndexOfSide)
          val _tmpType: String
          _tmpType = _stmt.getText(_columnIndexOfType)
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpExecutionMode: String
          _tmpExecutionMode = _stmt.getText(_columnIndexOfExecutionMode)
          val _tmpQuantity: Double
          _tmpQuantity = _stmt.getDouble(_columnIndexOfQuantity)
          val _tmpPrice: Double?
          if (_stmt.isNull(_columnIndexOfPrice)) {
            _tmpPrice = null
          } else {
            _tmpPrice = _stmt.getDouble(_columnIndexOfPrice)
          }
          val _tmpStopPrice: Double?
          if (_stmt.isNull(_columnIndexOfStopPrice)) {
            _tmpStopPrice = null
          } else {
            _tmpStopPrice = _stmt.getDouble(_columnIndexOfStopPrice)
          }
          val _tmpTakeProfitsJson: String
          _tmpTakeProfitsJson = _stmt.getText(_columnIndexOfTakeProfitsJson)
          val _tmpStopLossesJson: String
          _tmpStopLossesJson = _stmt.getText(_columnIndexOfStopLossesJson)
          val _tmpTrailingStopPercent: Double?
          if (_stmt.isNull(_columnIndexOfTrailingStopPercent)) {
            _tmpTrailingStopPercent = null
          } else {
            _tmpTrailingStopPercent = _stmt.getDouble(_columnIndexOfTrailingStopPercent)
          }
          val _tmpTrailingStopActivationPrice: Double?
          if (_stmt.isNull(_columnIndexOfTrailingStopActivationPrice)) {
            _tmpTrailingStopActivationPrice = null
          } else {
            _tmpTrailingStopActivationPrice =
                _stmt.getDouble(_columnIndexOfTrailingStopActivationPrice)
          }
          val _tmpOcoLinkedOrderId: String?
          if (_stmt.isNull(_columnIndexOfOcoLinkedOrderId)) {
            _tmpOcoLinkedOrderId = null
          } else {
            _tmpOcoLinkedOrderId = _stmt.getText(_columnIndexOfOcoLinkedOrderId)
          }
          val _tmpBracketParentId: String?
          if (_stmt.isNull(_columnIndexOfBracketParentId)) {
            _tmpBracketParentId = null
          } else {
            _tmpBracketParentId = _stmt.getText(_columnIndexOfBracketParentId)
          }
          val _tmpTimeInForce: String
          _tmpTimeInForce = _stmt.getText(_columnIndexOfTimeInForce)
          val _tmpScheduledAt: Long?
          if (_stmt.isNull(_columnIndexOfScheduledAt)) {
            _tmpScheduledAt = null
          } else {
            _tmpScheduledAt = _stmt.getLong(_columnIndexOfScheduledAt)
          }
          val _tmpFilledQuantity: Double
          _tmpFilledQuantity = _stmt.getDouble(_columnIndexOfFilledQuantity)
          val _tmpFilledPrice: Double
          _tmpFilledPrice = _stmt.getDouble(_columnIndexOfFilledPrice)
          val _tmpFee: Double
          _tmpFee = _stmt.getDouble(_columnIndexOfFee)
          val _tmpFeeAsset: String
          _tmpFeeAsset = _stmt.getText(_columnIndexOfFeeAsset)
          val _tmpDonationAmount: Double
          _tmpDonationAmount = _stmt.getDouble(_columnIndexOfDonationAmount)
          val _tmpRealizedPnl: Double
          _tmpRealizedPnl = _stmt.getDouble(_columnIndexOfRealizedPnl)
          val _tmpSlippage: Double
          _tmpSlippage = _stmt.getDouble(_columnIndexOfSlippage)
          val _tmpIsPaperTrade: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsPaperTrade).toInt()
          _tmpIsPaperTrade = _tmp != 0
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpExecutedAt: Long?
          if (_stmt.isNull(_columnIndexOfExecutedAt)) {
            _tmpExecutedAt = null
          } else {
            _tmpExecutedAt = _stmt.getLong(_columnIndexOfExecutedAt)
          }
          val _tmpClosedAt: Long?
          if (_stmt.isNull(_columnIndexOfClosedAt)) {
            _tmpClosedAt = null
          } else {
            _tmpClosedAt = _stmt.getLong(_columnIndexOfClosedAt)
          }
          val _tmpBinanceOrderId: Long?
          if (_stmt.isNull(_columnIndexOfBinanceOrderId)) {
            _tmpBinanceOrderId = null
          } else {
            _tmpBinanceOrderId = _stmt.getLong(_columnIndexOfBinanceOrderId)
          }
          val _tmpErrorMessage: String?
          if (_stmt.isNull(_columnIndexOfErrorMessage)) {
            _tmpErrorMessage = null
          } else {
            _tmpErrorMessage = _stmt.getText(_columnIndexOfErrorMessage)
          }
          _item =
              OrderEntity(_tmpId,_tmpSignalId,_tmpSymbol,_tmpSide,_tmpType,_tmpStatus,_tmpExecutionMode,_tmpQuantity,_tmpPrice,_tmpStopPrice,_tmpTakeProfitsJson,_tmpStopLossesJson,_tmpTrailingStopPercent,_tmpTrailingStopActivationPrice,_tmpOcoLinkedOrderId,_tmpBracketParentId,_tmpTimeInForce,_tmpScheduledAt,_tmpFilledQuantity,_tmpFilledPrice,_tmpFee,_tmpFeeAsset,_tmpDonationAmount,_tmpRealizedPnl,_tmpSlippage,_tmpIsPaperTrade,_tmpCreatedAt,_tmpUpdatedAt,_tmpExecutedAt,_tmpClosedAt,_tmpBinanceOrderId,_tmpErrorMessage)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getOrderById(orderId: String): Flow<OrderEntity?> {
    val _sql: String = "SELECT * FROM orders WHERE id = ?"
    return createFlow(__db, false, arrayOf("orders")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, orderId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfSignalId: Int = getColumnIndexOrThrow(_stmt, "signalId")
        val _columnIndexOfSymbol: Int = getColumnIndexOrThrow(_stmt, "symbol")
        val _columnIndexOfSide: Int = getColumnIndexOrThrow(_stmt, "side")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfExecutionMode: Int = getColumnIndexOrThrow(_stmt, "executionMode")
        val _columnIndexOfQuantity: Int = getColumnIndexOrThrow(_stmt, "quantity")
        val _columnIndexOfPrice: Int = getColumnIndexOrThrow(_stmt, "price")
        val _columnIndexOfStopPrice: Int = getColumnIndexOrThrow(_stmt, "stopPrice")
        val _columnIndexOfTakeProfitsJson: Int = getColumnIndexOrThrow(_stmt, "takeProfitsJson")
        val _columnIndexOfStopLossesJson: Int = getColumnIndexOrThrow(_stmt, "stopLossesJson")
        val _columnIndexOfTrailingStopPercent: Int = getColumnIndexOrThrow(_stmt,
            "trailingStopPercent")
        val _columnIndexOfTrailingStopActivationPrice: Int = getColumnIndexOrThrow(_stmt,
            "trailingStopActivationPrice")
        val _columnIndexOfOcoLinkedOrderId: Int = getColumnIndexOrThrow(_stmt, "ocoLinkedOrderId")
        val _columnIndexOfBracketParentId: Int = getColumnIndexOrThrow(_stmt, "bracketParentId")
        val _columnIndexOfTimeInForce: Int = getColumnIndexOrThrow(_stmt, "timeInForce")
        val _columnIndexOfScheduledAt: Int = getColumnIndexOrThrow(_stmt, "scheduledAt")
        val _columnIndexOfFilledQuantity: Int = getColumnIndexOrThrow(_stmt, "filledQuantity")
        val _columnIndexOfFilledPrice: Int = getColumnIndexOrThrow(_stmt, "filledPrice")
        val _columnIndexOfFee: Int = getColumnIndexOrThrow(_stmt, "fee")
        val _columnIndexOfFeeAsset: Int = getColumnIndexOrThrow(_stmt, "feeAsset")
        val _columnIndexOfDonationAmount: Int = getColumnIndexOrThrow(_stmt, "donationAmount")
        val _columnIndexOfRealizedPnl: Int = getColumnIndexOrThrow(_stmt, "realizedPnl")
        val _columnIndexOfSlippage: Int = getColumnIndexOrThrow(_stmt, "slippage")
        val _columnIndexOfIsPaperTrade: Int = getColumnIndexOrThrow(_stmt, "isPaperTrade")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfExecutedAt: Int = getColumnIndexOrThrow(_stmt, "executedAt")
        val _columnIndexOfClosedAt: Int = getColumnIndexOrThrow(_stmt, "closedAt")
        val _columnIndexOfBinanceOrderId: Int = getColumnIndexOrThrow(_stmt, "binanceOrderId")
        val _columnIndexOfErrorMessage: Int = getColumnIndexOrThrow(_stmt, "errorMessage")
        val _result: OrderEntity?
        if (_stmt.step()) {
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpSignalId: String?
          if (_stmt.isNull(_columnIndexOfSignalId)) {
            _tmpSignalId = null
          } else {
            _tmpSignalId = _stmt.getText(_columnIndexOfSignalId)
          }
          val _tmpSymbol: String
          _tmpSymbol = _stmt.getText(_columnIndexOfSymbol)
          val _tmpSide: String
          _tmpSide = _stmt.getText(_columnIndexOfSide)
          val _tmpType: String
          _tmpType = _stmt.getText(_columnIndexOfType)
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpExecutionMode: String
          _tmpExecutionMode = _stmt.getText(_columnIndexOfExecutionMode)
          val _tmpQuantity: Double
          _tmpQuantity = _stmt.getDouble(_columnIndexOfQuantity)
          val _tmpPrice: Double?
          if (_stmt.isNull(_columnIndexOfPrice)) {
            _tmpPrice = null
          } else {
            _tmpPrice = _stmt.getDouble(_columnIndexOfPrice)
          }
          val _tmpStopPrice: Double?
          if (_stmt.isNull(_columnIndexOfStopPrice)) {
            _tmpStopPrice = null
          } else {
            _tmpStopPrice = _stmt.getDouble(_columnIndexOfStopPrice)
          }
          val _tmpTakeProfitsJson: String
          _tmpTakeProfitsJson = _stmt.getText(_columnIndexOfTakeProfitsJson)
          val _tmpStopLossesJson: String
          _tmpStopLossesJson = _stmt.getText(_columnIndexOfStopLossesJson)
          val _tmpTrailingStopPercent: Double?
          if (_stmt.isNull(_columnIndexOfTrailingStopPercent)) {
            _tmpTrailingStopPercent = null
          } else {
            _tmpTrailingStopPercent = _stmt.getDouble(_columnIndexOfTrailingStopPercent)
          }
          val _tmpTrailingStopActivationPrice: Double?
          if (_stmt.isNull(_columnIndexOfTrailingStopActivationPrice)) {
            _tmpTrailingStopActivationPrice = null
          } else {
            _tmpTrailingStopActivationPrice =
                _stmt.getDouble(_columnIndexOfTrailingStopActivationPrice)
          }
          val _tmpOcoLinkedOrderId: String?
          if (_stmt.isNull(_columnIndexOfOcoLinkedOrderId)) {
            _tmpOcoLinkedOrderId = null
          } else {
            _tmpOcoLinkedOrderId = _stmt.getText(_columnIndexOfOcoLinkedOrderId)
          }
          val _tmpBracketParentId: String?
          if (_stmt.isNull(_columnIndexOfBracketParentId)) {
            _tmpBracketParentId = null
          } else {
            _tmpBracketParentId = _stmt.getText(_columnIndexOfBracketParentId)
          }
          val _tmpTimeInForce: String
          _tmpTimeInForce = _stmt.getText(_columnIndexOfTimeInForce)
          val _tmpScheduledAt: Long?
          if (_stmt.isNull(_columnIndexOfScheduledAt)) {
            _tmpScheduledAt = null
          } else {
            _tmpScheduledAt = _stmt.getLong(_columnIndexOfScheduledAt)
          }
          val _tmpFilledQuantity: Double
          _tmpFilledQuantity = _stmt.getDouble(_columnIndexOfFilledQuantity)
          val _tmpFilledPrice: Double
          _tmpFilledPrice = _stmt.getDouble(_columnIndexOfFilledPrice)
          val _tmpFee: Double
          _tmpFee = _stmt.getDouble(_columnIndexOfFee)
          val _tmpFeeAsset: String
          _tmpFeeAsset = _stmt.getText(_columnIndexOfFeeAsset)
          val _tmpDonationAmount: Double
          _tmpDonationAmount = _stmt.getDouble(_columnIndexOfDonationAmount)
          val _tmpRealizedPnl: Double
          _tmpRealizedPnl = _stmt.getDouble(_columnIndexOfRealizedPnl)
          val _tmpSlippage: Double
          _tmpSlippage = _stmt.getDouble(_columnIndexOfSlippage)
          val _tmpIsPaperTrade: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsPaperTrade).toInt()
          _tmpIsPaperTrade = _tmp != 0
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpExecutedAt: Long?
          if (_stmt.isNull(_columnIndexOfExecutedAt)) {
            _tmpExecutedAt = null
          } else {
            _tmpExecutedAt = _stmt.getLong(_columnIndexOfExecutedAt)
          }
          val _tmpClosedAt: Long?
          if (_stmt.isNull(_columnIndexOfClosedAt)) {
            _tmpClosedAt = null
          } else {
            _tmpClosedAt = _stmt.getLong(_columnIndexOfClosedAt)
          }
          val _tmpBinanceOrderId: Long?
          if (_stmt.isNull(_columnIndexOfBinanceOrderId)) {
            _tmpBinanceOrderId = null
          } else {
            _tmpBinanceOrderId = _stmt.getLong(_columnIndexOfBinanceOrderId)
          }
          val _tmpErrorMessage: String?
          if (_stmt.isNull(_columnIndexOfErrorMessage)) {
            _tmpErrorMessage = null
          } else {
            _tmpErrorMessage = _stmt.getText(_columnIndexOfErrorMessage)
          }
          _result =
              OrderEntity(_tmpId,_tmpSignalId,_tmpSymbol,_tmpSide,_tmpType,_tmpStatus,_tmpExecutionMode,_tmpQuantity,_tmpPrice,_tmpStopPrice,_tmpTakeProfitsJson,_tmpStopLossesJson,_tmpTrailingStopPercent,_tmpTrailingStopActivationPrice,_tmpOcoLinkedOrderId,_tmpBracketParentId,_tmpTimeInForce,_tmpScheduledAt,_tmpFilledQuantity,_tmpFilledPrice,_tmpFee,_tmpFeeAsset,_tmpDonationAmount,_tmpRealizedPnl,_tmpSlippage,_tmpIsPaperTrade,_tmpCreatedAt,_tmpUpdatedAt,_tmpExecutedAt,_tmpClosedAt,_tmpBinanceOrderId,_tmpErrorMessage)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getOpenOrdersForSymbol(symbol: String): Flow<List<OrderEntity>> {
    val _sql: String =
        "SELECT * FROM orders WHERE symbol = ? AND status IN ('PENDING','SUBMITTED','PARTIALLY_FILLED')"
    return createFlow(__db, false, arrayOf("orders")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, symbol)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfSignalId: Int = getColumnIndexOrThrow(_stmt, "signalId")
        val _columnIndexOfSymbol: Int = getColumnIndexOrThrow(_stmt, "symbol")
        val _columnIndexOfSide: Int = getColumnIndexOrThrow(_stmt, "side")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfExecutionMode: Int = getColumnIndexOrThrow(_stmt, "executionMode")
        val _columnIndexOfQuantity: Int = getColumnIndexOrThrow(_stmt, "quantity")
        val _columnIndexOfPrice: Int = getColumnIndexOrThrow(_stmt, "price")
        val _columnIndexOfStopPrice: Int = getColumnIndexOrThrow(_stmt, "stopPrice")
        val _columnIndexOfTakeProfitsJson: Int = getColumnIndexOrThrow(_stmt, "takeProfitsJson")
        val _columnIndexOfStopLossesJson: Int = getColumnIndexOrThrow(_stmt, "stopLossesJson")
        val _columnIndexOfTrailingStopPercent: Int = getColumnIndexOrThrow(_stmt,
            "trailingStopPercent")
        val _columnIndexOfTrailingStopActivationPrice: Int = getColumnIndexOrThrow(_stmt,
            "trailingStopActivationPrice")
        val _columnIndexOfOcoLinkedOrderId: Int = getColumnIndexOrThrow(_stmt, "ocoLinkedOrderId")
        val _columnIndexOfBracketParentId: Int = getColumnIndexOrThrow(_stmt, "bracketParentId")
        val _columnIndexOfTimeInForce: Int = getColumnIndexOrThrow(_stmt, "timeInForce")
        val _columnIndexOfScheduledAt: Int = getColumnIndexOrThrow(_stmt, "scheduledAt")
        val _columnIndexOfFilledQuantity: Int = getColumnIndexOrThrow(_stmt, "filledQuantity")
        val _columnIndexOfFilledPrice: Int = getColumnIndexOrThrow(_stmt, "filledPrice")
        val _columnIndexOfFee: Int = getColumnIndexOrThrow(_stmt, "fee")
        val _columnIndexOfFeeAsset: Int = getColumnIndexOrThrow(_stmt, "feeAsset")
        val _columnIndexOfDonationAmount: Int = getColumnIndexOrThrow(_stmt, "donationAmount")
        val _columnIndexOfRealizedPnl: Int = getColumnIndexOrThrow(_stmt, "realizedPnl")
        val _columnIndexOfSlippage: Int = getColumnIndexOrThrow(_stmt, "slippage")
        val _columnIndexOfIsPaperTrade: Int = getColumnIndexOrThrow(_stmt, "isPaperTrade")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfExecutedAt: Int = getColumnIndexOrThrow(_stmt, "executedAt")
        val _columnIndexOfClosedAt: Int = getColumnIndexOrThrow(_stmt, "closedAt")
        val _columnIndexOfBinanceOrderId: Int = getColumnIndexOrThrow(_stmt, "binanceOrderId")
        val _columnIndexOfErrorMessage: Int = getColumnIndexOrThrow(_stmt, "errorMessage")
        val _result: MutableList<OrderEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: OrderEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpSignalId: String?
          if (_stmt.isNull(_columnIndexOfSignalId)) {
            _tmpSignalId = null
          } else {
            _tmpSignalId = _stmt.getText(_columnIndexOfSignalId)
          }
          val _tmpSymbol: String
          _tmpSymbol = _stmt.getText(_columnIndexOfSymbol)
          val _tmpSide: String
          _tmpSide = _stmt.getText(_columnIndexOfSide)
          val _tmpType: String
          _tmpType = _stmt.getText(_columnIndexOfType)
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpExecutionMode: String
          _tmpExecutionMode = _stmt.getText(_columnIndexOfExecutionMode)
          val _tmpQuantity: Double
          _tmpQuantity = _stmt.getDouble(_columnIndexOfQuantity)
          val _tmpPrice: Double?
          if (_stmt.isNull(_columnIndexOfPrice)) {
            _tmpPrice = null
          } else {
            _tmpPrice = _stmt.getDouble(_columnIndexOfPrice)
          }
          val _tmpStopPrice: Double?
          if (_stmt.isNull(_columnIndexOfStopPrice)) {
            _tmpStopPrice = null
          } else {
            _tmpStopPrice = _stmt.getDouble(_columnIndexOfStopPrice)
          }
          val _tmpTakeProfitsJson: String
          _tmpTakeProfitsJson = _stmt.getText(_columnIndexOfTakeProfitsJson)
          val _tmpStopLossesJson: String
          _tmpStopLossesJson = _stmt.getText(_columnIndexOfStopLossesJson)
          val _tmpTrailingStopPercent: Double?
          if (_stmt.isNull(_columnIndexOfTrailingStopPercent)) {
            _tmpTrailingStopPercent = null
          } else {
            _tmpTrailingStopPercent = _stmt.getDouble(_columnIndexOfTrailingStopPercent)
          }
          val _tmpTrailingStopActivationPrice: Double?
          if (_stmt.isNull(_columnIndexOfTrailingStopActivationPrice)) {
            _tmpTrailingStopActivationPrice = null
          } else {
            _tmpTrailingStopActivationPrice =
                _stmt.getDouble(_columnIndexOfTrailingStopActivationPrice)
          }
          val _tmpOcoLinkedOrderId: String?
          if (_stmt.isNull(_columnIndexOfOcoLinkedOrderId)) {
            _tmpOcoLinkedOrderId = null
          } else {
            _tmpOcoLinkedOrderId = _stmt.getText(_columnIndexOfOcoLinkedOrderId)
          }
          val _tmpBracketParentId: String?
          if (_stmt.isNull(_columnIndexOfBracketParentId)) {
            _tmpBracketParentId = null
          } else {
            _tmpBracketParentId = _stmt.getText(_columnIndexOfBracketParentId)
          }
          val _tmpTimeInForce: String
          _tmpTimeInForce = _stmt.getText(_columnIndexOfTimeInForce)
          val _tmpScheduledAt: Long?
          if (_stmt.isNull(_columnIndexOfScheduledAt)) {
            _tmpScheduledAt = null
          } else {
            _tmpScheduledAt = _stmt.getLong(_columnIndexOfScheduledAt)
          }
          val _tmpFilledQuantity: Double
          _tmpFilledQuantity = _stmt.getDouble(_columnIndexOfFilledQuantity)
          val _tmpFilledPrice: Double
          _tmpFilledPrice = _stmt.getDouble(_columnIndexOfFilledPrice)
          val _tmpFee: Double
          _tmpFee = _stmt.getDouble(_columnIndexOfFee)
          val _tmpFeeAsset: String
          _tmpFeeAsset = _stmt.getText(_columnIndexOfFeeAsset)
          val _tmpDonationAmount: Double
          _tmpDonationAmount = _stmt.getDouble(_columnIndexOfDonationAmount)
          val _tmpRealizedPnl: Double
          _tmpRealizedPnl = _stmt.getDouble(_columnIndexOfRealizedPnl)
          val _tmpSlippage: Double
          _tmpSlippage = _stmt.getDouble(_columnIndexOfSlippage)
          val _tmpIsPaperTrade: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsPaperTrade).toInt()
          _tmpIsPaperTrade = _tmp != 0
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpExecutedAt: Long?
          if (_stmt.isNull(_columnIndexOfExecutedAt)) {
            _tmpExecutedAt = null
          } else {
            _tmpExecutedAt = _stmt.getLong(_columnIndexOfExecutedAt)
          }
          val _tmpClosedAt: Long?
          if (_stmt.isNull(_columnIndexOfClosedAt)) {
            _tmpClosedAt = null
          } else {
            _tmpClosedAt = _stmt.getLong(_columnIndexOfClosedAt)
          }
          val _tmpBinanceOrderId: Long?
          if (_stmt.isNull(_columnIndexOfBinanceOrderId)) {
            _tmpBinanceOrderId = null
          } else {
            _tmpBinanceOrderId = _stmt.getLong(_columnIndexOfBinanceOrderId)
          }
          val _tmpErrorMessage: String?
          if (_stmt.isNull(_columnIndexOfErrorMessage)) {
            _tmpErrorMessage = null
          } else {
            _tmpErrorMessage = _stmt.getText(_columnIndexOfErrorMessage)
          }
          _item =
              OrderEntity(_tmpId,_tmpSignalId,_tmpSymbol,_tmpSide,_tmpType,_tmpStatus,_tmpExecutionMode,_tmpQuantity,_tmpPrice,_tmpStopPrice,_tmpTakeProfitsJson,_tmpStopLossesJson,_tmpTrailingStopPercent,_tmpTrailingStopActivationPrice,_tmpOcoLinkedOrderId,_tmpBracketParentId,_tmpTimeInForce,_tmpScheduledAt,_tmpFilledQuantity,_tmpFilledPrice,_tmpFee,_tmpFeeAsset,_tmpDonationAmount,_tmpRealizedPnl,_tmpSlippage,_tmpIsPaperTrade,_tmpCreatedAt,_tmpUpdatedAt,_tmpExecutedAt,_tmpClosedAt,_tmpBinanceOrderId,_tmpErrorMessage)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getPaperOrders(limit: Int): Flow<List<OrderEntity>> {
    val _sql: String = "SELECT * FROM orders WHERE isPaperTrade = 1 ORDER BY createdAt DESC LIMIT ?"
    return createFlow(__db, false, arrayOf("orders")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, limit.toLong())
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfSignalId: Int = getColumnIndexOrThrow(_stmt, "signalId")
        val _columnIndexOfSymbol: Int = getColumnIndexOrThrow(_stmt, "symbol")
        val _columnIndexOfSide: Int = getColumnIndexOrThrow(_stmt, "side")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfExecutionMode: Int = getColumnIndexOrThrow(_stmt, "executionMode")
        val _columnIndexOfQuantity: Int = getColumnIndexOrThrow(_stmt, "quantity")
        val _columnIndexOfPrice: Int = getColumnIndexOrThrow(_stmt, "price")
        val _columnIndexOfStopPrice: Int = getColumnIndexOrThrow(_stmt, "stopPrice")
        val _columnIndexOfTakeProfitsJson: Int = getColumnIndexOrThrow(_stmt, "takeProfitsJson")
        val _columnIndexOfStopLossesJson: Int = getColumnIndexOrThrow(_stmt, "stopLossesJson")
        val _columnIndexOfTrailingStopPercent: Int = getColumnIndexOrThrow(_stmt,
            "trailingStopPercent")
        val _columnIndexOfTrailingStopActivationPrice: Int = getColumnIndexOrThrow(_stmt,
            "trailingStopActivationPrice")
        val _columnIndexOfOcoLinkedOrderId: Int = getColumnIndexOrThrow(_stmt, "ocoLinkedOrderId")
        val _columnIndexOfBracketParentId: Int = getColumnIndexOrThrow(_stmt, "bracketParentId")
        val _columnIndexOfTimeInForce: Int = getColumnIndexOrThrow(_stmt, "timeInForce")
        val _columnIndexOfScheduledAt: Int = getColumnIndexOrThrow(_stmt, "scheduledAt")
        val _columnIndexOfFilledQuantity: Int = getColumnIndexOrThrow(_stmt, "filledQuantity")
        val _columnIndexOfFilledPrice: Int = getColumnIndexOrThrow(_stmt, "filledPrice")
        val _columnIndexOfFee: Int = getColumnIndexOrThrow(_stmt, "fee")
        val _columnIndexOfFeeAsset: Int = getColumnIndexOrThrow(_stmt, "feeAsset")
        val _columnIndexOfDonationAmount: Int = getColumnIndexOrThrow(_stmt, "donationAmount")
        val _columnIndexOfRealizedPnl: Int = getColumnIndexOrThrow(_stmt, "realizedPnl")
        val _columnIndexOfSlippage: Int = getColumnIndexOrThrow(_stmt, "slippage")
        val _columnIndexOfIsPaperTrade: Int = getColumnIndexOrThrow(_stmt, "isPaperTrade")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfExecutedAt: Int = getColumnIndexOrThrow(_stmt, "executedAt")
        val _columnIndexOfClosedAt: Int = getColumnIndexOrThrow(_stmt, "closedAt")
        val _columnIndexOfBinanceOrderId: Int = getColumnIndexOrThrow(_stmt, "binanceOrderId")
        val _columnIndexOfErrorMessage: Int = getColumnIndexOrThrow(_stmt, "errorMessage")
        val _result: MutableList<OrderEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: OrderEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpSignalId: String?
          if (_stmt.isNull(_columnIndexOfSignalId)) {
            _tmpSignalId = null
          } else {
            _tmpSignalId = _stmt.getText(_columnIndexOfSignalId)
          }
          val _tmpSymbol: String
          _tmpSymbol = _stmt.getText(_columnIndexOfSymbol)
          val _tmpSide: String
          _tmpSide = _stmt.getText(_columnIndexOfSide)
          val _tmpType: String
          _tmpType = _stmt.getText(_columnIndexOfType)
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpExecutionMode: String
          _tmpExecutionMode = _stmt.getText(_columnIndexOfExecutionMode)
          val _tmpQuantity: Double
          _tmpQuantity = _stmt.getDouble(_columnIndexOfQuantity)
          val _tmpPrice: Double?
          if (_stmt.isNull(_columnIndexOfPrice)) {
            _tmpPrice = null
          } else {
            _tmpPrice = _stmt.getDouble(_columnIndexOfPrice)
          }
          val _tmpStopPrice: Double?
          if (_stmt.isNull(_columnIndexOfStopPrice)) {
            _tmpStopPrice = null
          } else {
            _tmpStopPrice = _stmt.getDouble(_columnIndexOfStopPrice)
          }
          val _tmpTakeProfitsJson: String
          _tmpTakeProfitsJson = _stmt.getText(_columnIndexOfTakeProfitsJson)
          val _tmpStopLossesJson: String
          _tmpStopLossesJson = _stmt.getText(_columnIndexOfStopLossesJson)
          val _tmpTrailingStopPercent: Double?
          if (_stmt.isNull(_columnIndexOfTrailingStopPercent)) {
            _tmpTrailingStopPercent = null
          } else {
            _tmpTrailingStopPercent = _stmt.getDouble(_columnIndexOfTrailingStopPercent)
          }
          val _tmpTrailingStopActivationPrice: Double?
          if (_stmt.isNull(_columnIndexOfTrailingStopActivationPrice)) {
            _tmpTrailingStopActivationPrice = null
          } else {
            _tmpTrailingStopActivationPrice =
                _stmt.getDouble(_columnIndexOfTrailingStopActivationPrice)
          }
          val _tmpOcoLinkedOrderId: String?
          if (_stmt.isNull(_columnIndexOfOcoLinkedOrderId)) {
            _tmpOcoLinkedOrderId = null
          } else {
            _tmpOcoLinkedOrderId = _stmt.getText(_columnIndexOfOcoLinkedOrderId)
          }
          val _tmpBracketParentId: String?
          if (_stmt.isNull(_columnIndexOfBracketParentId)) {
            _tmpBracketParentId = null
          } else {
            _tmpBracketParentId = _stmt.getText(_columnIndexOfBracketParentId)
          }
          val _tmpTimeInForce: String
          _tmpTimeInForce = _stmt.getText(_columnIndexOfTimeInForce)
          val _tmpScheduledAt: Long?
          if (_stmt.isNull(_columnIndexOfScheduledAt)) {
            _tmpScheduledAt = null
          } else {
            _tmpScheduledAt = _stmt.getLong(_columnIndexOfScheduledAt)
          }
          val _tmpFilledQuantity: Double
          _tmpFilledQuantity = _stmt.getDouble(_columnIndexOfFilledQuantity)
          val _tmpFilledPrice: Double
          _tmpFilledPrice = _stmt.getDouble(_columnIndexOfFilledPrice)
          val _tmpFee: Double
          _tmpFee = _stmt.getDouble(_columnIndexOfFee)
          val _tmpFeeAsset: String
          _tmpFeeAsset = _stmt.getText(_columnIndexOfFeeAsset)
          val _tmpDonationAmount: Double
          _tmpDonationAmount = _stmt.getDouble(_columnIndexOfDonationAmount)
          val _tmpRealizedPnl: Double
          _tmpRealizedPnl = _stmt.getDouble(_columnIndexOfRealizedPnl)
          val _tmpSlippage: Double
          _tmpSlippage = _stmt.getDouble(_columnIndexOfSlippage)
          val _tmpIsPaperTrade: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsPaperTrade).toInt()
          _tmpIsPaperTrade = _tmp != 0
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpExecutedAt: Long?
          if (_stmt.isNull(_columnIndexOfExecutedAt)) {
            _tmpExecutedAt = null
          } else {
            _tmpExecutedAt = _stmt.getLong(_columnIndexOfExecutedAt)
          }
          val _tmpClosedAt: Long?
          if (_stmt.isNull(_columnIndexOfClosedAt)) {
            _tmpClosedAt = null
          } else {
            _tmpClosedAt = _stmt.getLong(_columnIndexOfClosedAt)
          }
          val _tmpBinanceOrderId: Long?
          if (_stmt.isNull(_columnIndexOfBinanceOrderId)) {
            _tmpBinanceOrderId = null
          } else {
            _tmpBinanceOrderId = _stmt.getLong(_columnIndexOfBinanceOrderId)
          }
          val _tmpErrorMessage: String?
          if (_stmt.isNull(_columnIndexOfErrorMessage)) {
            _tmpErrorMessage = null
          } else {
            _tmpErrorMessage = _stmt.getText(_columnIndexOfErrorMessage)
          }
          _item =
              OrderEntity(_tmpId,_tmpSignalId,_tmpSymbol,_tmpSide,_tmpType,_tmpStatus,_tmpExecutionMode,_tmpQuantity,_tmpPrice,_tmpStopPrice,_tmpTakeProfitsJson,_tmpStopLossesJson,_tmpTrailingStopPercent,_tmpTrailingStopActivationPrice,_tmpOcoLinkedOrderId,_tmpBracketParentId,_tmpTimeInForce,_tmpScheduledAt,_tmpFilledQuantity,_tmpFilledPrice,_tmpFee,_tmpFeeAsset,_tmpDonationAmount,_tmpRealizedPnl,_tmpSlippage,_tmpIsPaperTrade,_tmpCreatedAt,_tmpUpdatedAt,_tmpExecutedAt,_tmpClosedAt,_tmpBinanceOrderId,_tmpErrorMessage)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getOrderBySignalId(signalId: String): OrderEntity? {
    val _sql: String = "SELECT * FROM orders WHERE signalId = ? LIMIT 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, signalId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfSignalId: Int = getColumnIndexOrThrow(_stmt, "signalId")
        val _columnIndexOfSymbol: Int = getColumnIndexOrThrow(_stmt, "symbol")
        val _columnIndexOfSide: Int = getColumnIndexOrThrow(_stmt, "side")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfExecutionMode: Int = getColumnIndexOrThrow(_stmt, "executionMode")
        val _columnIndexOfQuantity: Int = getColumnIndexOrThrow(_stmt, "quantity")
        val _columnIndexOfPrice: Int = getColumnIndexOrThrow(_stmt, "price")
        val _columnIndexOfStopPrice: Int = getColumnIndexOrThrow(_stmt, "stopPrice")
        val _columnIndexOfTakeProfitsJson: Int = getColumnIndexOrThrow(_stmt, "takeProfitsJson")
        val _columnIndexOfStopLossesJson: Int = getColumnIndexOrThrow(_stmt, "stopLossesJson")
        val _columnIndexOfTrailingStopPercent: Int = getColumnIndexOrThrow(_stmt,
            "trailingStopPercent")
        val _columnIndexOfTrailingStopActivationPrice: Int = getColumnIndexOrThrow(_stmt,
            "trailingStopActivationPrice")
        val _columnIndexOfOcoLinkedOrderId: Int = getColumnIndexOrThrow(_stmt, "ocoLinkedOrderId")
        val _columnIndexOfBracketParentId: Int = getColumnIndexOrThrow(_stmt, "bracketParentId")
        val _columnIndexOfTimeInForce: Int = getColumnIndexOrThrow(_stmt, "timeInForce")
        val _columnIndexOfScheduledAt: Int = getColumnIndexOrThrow(_stmt, "scheduledAt")
        val _columnIndexOfFilledQuantity: Int = getColumnIndexOrThrow(_stmt, "filledQuantity")
        val _columnIndexOfFilledPrice: Int = getColumnIndexOrThrow(_stmt, "filledPrice")
        val _columnIndexOfFee: Int = getColumnIndexOrThrow(_stmt, "fee")
        val _columnIndexOfFeeAsset: Int = getColumnIndexOrThrow(_stmt, "feeAsset")
        val _columnIndexOfDonationAmount: Int = getColumnIndexOrThrow(_stmt, "donationAmount")
        val _columnIndexOfRealizedPnl: Int = getColumnIndexOrThrow(_stmt, "realizedPnl")
        val _columnIndexOfSlippage: Int = getColumnIndexOrThrow(_stmt, "slippage")
        val _columnIndexOfIsPaperTrade: Int = getColumnIndexOrThrow(_stmt, "isPaperTrade")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfExecutedAt: Int = getColumnIndexOrThrow(_stmt, "executedAt")
        val _columnIndexOfClosedAt: Int = getColumnIndexOrThrow(_stmt, "closedAt")
        val _columnIndexOfBinanceOrderId: Int = getColumnIndexOrThrow(_stmt, "binanceOrderId")
        val _columnIndexOfErrorMessage: Int = getColumnIndexOrThrow(_stmt, "errorMessage")
        val _result: OrderEntity?
        if (_stmt.step()) {
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpSignalId: String?
          if (_stmt.isNull(_columnIndexOfSignalId)) {
            _tmpSignalId = null
          } else {
            _tmpSignalId = _stmt.getText(_columnIndexOfSignalId)
          }
          val _tmpSymbol: String
          _tmpSymbol = _stmt.getText(_columnIndexOfSymbol)
          val _tmpSide: String
          _tmpSide = _stmt.getText(_columnIndexOfSide)
          val _tmpType: String
          _tmpType = _stmt.getText(_columnIndexOfType)
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpExecutionMode: String
          _tmpExecutionMode = _stmt.getText(_columnIndexOfExecutionMode)
          val _tmpQuantity: Double
          _tmpQuantity = _stmt.getDouble(_columnIndexOfQuantity)
          val _tmpPrice: Double?
          if (_stmt.isNull(_columnIndexOfPrice)) {
            _tmpPrice = null
          } else {
            _tmpPrice = _stmt.getDouble(_columnIndexOfPrice)
          }
          val _tmpStopPrice: Double?
          if (_stmt.isNull(_columnIndexOfStopPrice)) {
            _tmpStopPrice = null
          } else {
            _tmpStopPrice = _stmt.getDouble(_columnIndexOfStopPrice)
          }
          val _tmpTakeProfitsJson: String
          _tmpTakeProfitsJson = _stmt.getText(_columnIndexOfTakeProfitsJson)
          val _tmpStopLossesJson: String
          _tmpStopLossesJson = _stmt.getText(_columnIndexOfStopLossesJson)
          val _tmpTrailingStopPercent: Double?
          if (_stmt.isNull(_columnIndexOfTrailingStopPercent)) {
            _tmpTrailingStopPercent = null
          } else {
            _tmpTrailingStopPercent = _stmt.getDouble(_columnIndexOfTrailingStopPercent)
          }
          val _tmpTrailingStopActivationPrice: Double?
          if (_stmt.isNull(_columnIndexOfTrailingStopActivationPrice)) {
            _tmpTrailingStopActivationPrice = null
          } else {
            _tmpTrailingStopActivationPrice =
                _stmt.getDouble(_columnIndexOfTrailingStopActivationPrice)
          }
          val _tmpOcoLinkedOrderId: String?
          if (_stmt.isNull(_columnIndexOfOcoLinkedOrderId)) {
            _tmpOcoLinkedOrderId = null
          } else {
            _tmpOcoLinkedOrderId = _stmt.getText(_columnIndexOfOcoLinkedOrderId)
          }
          val _tmpBracketParentId: String?
          if (_stmt.isNull(_columnIndexOfBracketParentId)) {
            _tmpBracketParentId = null
          } else {
            _tmpBracketParentId = _stmt.getText(_columnIndexOfBracketParentId)
          }
          val _tmpTimeInForce: String
          _tmpTimeInForce = _stmt.getText(_columnIndexOfTimeInForce)
          val _tmpScheduledAt: Long?
          if (_stmt.isNull(_columnIndexOfScheduledAt)) {
            _tmpScheduledAt = null
          } else {
            _tmpScheduledAt = _stmt.getLong(_columnIndexOfScheduledAt)
          }
          val _tmpFilledQuantity: Double
          _tmpFilledQuantity = _stmt.getDouble(_columnIndexOfFilledQuantity)
          val _tmpFilledPrice: Double
          _tmpFilledPrice = _stmt.getDouble(_columnIndexOfFilledPrice)
          val _tmpFee: Double
          _tmpFee = _stmt.getDouble(_columnIndexOfFee)
          val _tmpFeeAsset: String
          _tmpFeeAsset = _stmt.getText(_columnIndexOfFeeAsset)
          val _tmpDonationAmount: Double
          _tmpDonationAmount = _stmt.getDouble(_columnIndexOfDonationAmount)
          val _tmpRealizedPnl: Double
          _tmpRealizedPnl = _stmt.getDouble(_columnIndexOfRealizedPnl)
          val _tmpSlippage: Double
          _tmpSlippage = _stmt.getDouble(_columnIndexOfSlippage)
          val _tmpIsPaperTrade: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsPaperTrade).toInt()
          _tmpIsPaperTrade = _tmp != 0
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpExecutedAt: Long?
          if (_stmt.isNull(_columnIndexOfExecutedAt)) {
            _tmpExecutedAt = null
          } else {
            _tmpExecutedAt = _stmt.getLong(_columnIndexOfExecutedAt)
          }
          val _tmpClosedAt: Long?
          if (_stmt.isNull(_columnIndexOfClosedAt)) {
            _tmpClosedAt = null
          } else {
            _tmpClosedAt = _stmt.getLong(_columnIndexOfClosedAt)
          }
          val _tmpBinanceOrderId: Long?
          if (_stmt.isNull(_columnIndexOfBinanceOrderId)) {
            _tmpBinanceOrderId = null
          } else {
            _tmpBinanceOrderId = _stmt.getLong(_columnIndexOfBinanceOrderId)
          }
          val _tmpErrorMessage: String?
          if (_stmt.isNull(_columnIndexOfErrorMessage)) {
            _tmpErrorMessage = null
          } else {
            _tmpErrorMessage = _stmt.getText(_columnIndexOfErrorMessage)
          }
          _result =
              OrderEntity(_tmpId,_tmpSignalId,_tmpSymbol,_tmpSide,_tmpType,_tmpStatus,_tmpExecutionMode,_tmpQuantity,_tmpPrice,_tmpStopPrice,_tmpTakeProfitsJson,_tmpStopLossesJson,_tmpTrailingStopPercent,_tmpTrailingStopActivationPrice,_tmpOcoLinkedOrderId,_tmpBracketParentId,_tmpTimeInForce,_tmpScheduledAt,_tmpFilledQuantity,_tmpFilledPrice,_tmpFee,_tmpFeeAsset,_tmpDonationAmount,_tmpRealizedPnl,_tmpSlippage,_tmpIsPaperTrade,_tmpCreatedAt,_tmpUpdatedAt,_tmpExecutedAt,_tmpClosedAt,_tmpBinanceOrderId,_tmpErrorMessage)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getOpenOrderCount(): Flow<Int> {
    val _sql: String =
        "SELECT COUNT(*) FROM orders WHERE status IN ('PENDING','SUBMITTED','PARTIALLY_FILLED')"
    return createFlow(__db, false, arrayOf("orders")) { _connection ->
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

  public override fun getDailyPnl(since: Long): Flow<Double?> {
    val _sql: String = "SELECT SUM(realizedPnl) FROM orders WHERE closedAt >= ?"
    return createFlow(__db, false, arrayOf("orders")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, since)
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

  public override fun getClosedOrders(isPaper: Boolean): Flow<List<OrderEntity>> {
    val _sql: String =
        "SELECT * FROM orders WHERE closedAt IS NOT NULL AND isPaperTrade = ? ORDER BY closedAt DESC"
    return createFlow(__db, false, arrayOf("orders")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        val _tmp: Int = if (isPaper) 1 else 0
        _stmt.bindLong(_argIndex, _tmp.toLong())
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfSignalId: Int = getColumnIndexOrThrow(_stmt, "signalId")
        val _columnIndexOfSymbol: Int = getColumnIndexOrThrow(_stmt, "symbol")
        val _columnIndexOfSide: Int = getColumnIndexOrThrow(_stmt, "side")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfExecutionMode: Int = getColumnIndexOrThrow(_stmt, "executionMode")
        val _columnIndexOfQuantity: Int = getColumnIndexOrThrow(_stmt, "quantity")
        val _columnIndexOfPrice: Int = getColumnIndexOrThrow(_stmt, "price")
        val _columnIndexOfStopPrice: Int = getColumnIndexOrThrow(_stmt, "stopPrice")
        val _columnIndexOfTakeProfitsJson: Int = getColumnIndexOrThrow(_stmt, "takeProfitsJson")
        val _columnIndexOfStopLossesJson: Int = getColumnIndexOrThrow(_stmt, "stopLossesJson")
        val _columnIndexOfTrailingStopPercent: Int = getColumnIndexOrThrow(_stmt,
            "trailingStopPercent")
        val _columnIndexOfTrailingStopActivationPrice: Int = getColumnIndexOrThrow(_stmt,
            "trailingStopActivationPrice")
        val _columnIndexOfOcoLinkedOrderId: Int = getColumnIndexOrThrow(_stmt, "ocoLinkedOrderId")
        val _columnIndexOfBracketParentId: Int = getColumnIndexOrThrow(_stmt, "bracketParentId")
        val _columnIndexOfTimeInForce: Int = getColumnIndexOrThrow(_stmt, "timeInForce")
        val _columnIndexOfScheduledAt: Int = getColumnIndexOrThrow(_stmt, "scheduledAt")
        val _columnIndexOfFilledQuantity: Int = getColumnIndexOrThrow(_stmt, "filledQuantity")
        val _columnIndexOfFilledPrice: Int = getColumnIndexOrThrow(_stmt, "filledPrice")
        val _columnIndexOfFee: Int = getColumnIndexOrThrow(_stmt, "fee")
        val _columnIndexOfFeeAsset: Int = getColumnIndexOrThrow(_stmt, "feeAsset")
        val _columnIndexOfDonationAmount: Int = getColumnIndexOrThrow(_stmt, "donationAmount")
        val _columnIndexOfRealizedPnl: Int = getColumnIndexOrThrow(_stmt, "realizedPnl")
        val _columnIndexOfSlippage: Int = getColumnIndexOrThrow(_stmt, "slippage")
        val _columnIndexOfIsPaperTrade: Int = getColumnIndexOrThrow(_stmt, "isPaperTrade")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfExecutedAt: Int = getColumnIndexOrThrow(_stmt, "executedAt")
        val _columnIndexOfClosedAt: Int = getColumnIndexOrThrow(_stmt, "closedAt")
        val _columnIndexOfBinanceOrderId: Int = getColumnIndexOrThrow(_stmt, "binanceOrderId")
        val _columnIndexOfErrorMessage: Int = getColumnIndexOrThrow(_stmt, "errorMessage")
        val _result: MutableList<OrderEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: OrderEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpSignalId: String?
          if (_stmt.isNull(_columnIndexOfSignalId)) {
            _tmpSignalId = null
          } else {
            _tmpSignalId = _stmt.getText(_columnIndexOfSignalId)
          }
          val _tmpSymbol: String
          _tmpSymbol = _stmt.getText(_columnIndexOfSymbol)
          val _tmpSide: String
          _tmpSide = _stmt.getText(_columnIndexOfSide)
          val _tmpType: String
          _tmpType = _stmt.getText(_columnIndexOfType)
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpExecutionMode: String
          _tmpExecutionMode = _stmt.getText(_columnIndexOfExecutionMode)
          val _tmpQuantity: Double
          _tmpQuantity = _stmt.getDouble(_columnIndexOfQuantity)
          val _tmpPrice: Double?
          if (_stmt.isNull(_columnIndexOfPrice)) {
            _tmpPrice = null
          } else {
            _tmpPrice = _stmt.getDouble(_columnIndexOfPrice)
          }
          val _tmpStopPrice: Double?
          if (_stmt.isNull(_columnIndexOfStopPrice)) {
            _tmpStopPrice = null
          } else {
            _tmpStopPrice = _stmt.getDouble(_columnIndexOfStopPrice)
          }
          val _tmpTakeProfitsJson: String
          _tmpTakeProfitsJson = _stmt.getText(_columnIndexOfTakeProfitsJson)
          val _tmpStopLossesJson: String
          _tmpStopLossesJson = _stmt.getText(_columnIndexOfStopLossesJson)
          val _tmpTrailingStopPercent: Double?
          if (_stmt.isNull(_columnIndexOfTrailingStopPercent)) {
            _tmpTrailingStopPercent = null
          } else {
            _tmpTrailingStopPercent = _stmt.getDouble(_columnIndexOfTrailingStopPercent)
          }
          val _tmpTrailingStopActivationPrice: Double?
          if (_stmt.isNull(_columnIndexOfTrailingStopActivationPrice)) {
            _tmpTrailingStopActivationPrice = null
          } else {
            _tmpTrailingStopActivationPrice =
                _stmt.getDouble(_columnIndexOfTrailingStopActivationPrice)
          }
          val _tmpOcoLinkedOrderId: String?
          if (_stmt.isNull(_columnIndexOfOcoLinkedOrderId)) {
            _tmpOcoLinkedOrderId = null
          } else {
            _tmpOcoLinkedOrderId = _stmt.getText(_columnIndexOfOcoLinkedOrderId)
          }
          val _tmpBracketParentId: String?
          if (_stmt.isNull(_columnIndexOfBracketParentId)) {
            _tmpBracketParentId = null
          } else {
            _tmpBracketParentId = _stmt.getText(_columnIndexOfBracketParentId)
          }
          val _tmpTimeInForce: String
          _tmpTimeInForce = _stmt.getText(_columnIndexOfTimeInForce)
          val _tmpScheduledAt: Long?
          if (_stmt.isNull(_columnIndexOfScheduledAt)) {
            _tmpScheduledAt = null
          } else {
            _tmpScheduledAt = _stmt.getLong(_columnIndexOfScheduledAt)
          }
          val _tmpFilledQuantity: Double
          _tmpFilledQuantity = _stmt.getDouble(_columnIndexOfFilledQuantity)
          val _tmpFilledPrice: Double
          _tmpFilledPrice = _stmt.getDouble(_columnIndexOfFilledPrice)
          val _tmpFee: Double
          _tmpFee = _stmt.getDouble(_columnIndexOfFee)
          val _tmpFeeAsset: String
          _tmpFeeAsset = _stmt.getText(_columnIndexOfFeeAsset)
          val _tmpDonationAmount: Double
          _tmpDonationAmount = _stmt.getDouble(_columnIndexOfDonationAmount)
          val _tmpRealizedPnl: Double
          _tmpRealizedPnl = _stmt.getDouble(_columnIndexOfRealizedPnl)
          val _tmpSlippage: Double
          _tmpSlippage = _stmt.getDouble(_columnIndexOfSlippage)
          val _tmpIsPaperTrade: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsPaperTrade).toInt()
          _tmpIsPaperTrade = _tmp_1 != 0
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpExecutedAt: Long?
          if (_stmt.isNull(_columnIndexOfExecutedAt)) {
            _tmpExecutedAt = null
          } else {
            _tmpExecutedAt = _stmt.getLong(_columnIndexOfExecutedAt)
          }
          val _tmpClosedAt: Long?
          if (_stmt.isNull(_columnIndexOfClosedAt)) {
            _tmpClosedAt = null
          } else {
            _tmpClosedAt = _stmt.getLong(_columnIndexOfClosedAt)
          }
          val _tmpBinanceOrderId: Long?
          if (_stmt.isNull(_columnIndexOfBinanceOrderId)) {
            _tmpBinanceOrderId = null
          } else {
            _tmpBinanceOrderId = _stmt.getLong(_columnIndexOfBinanceOrderId)
          }
          val _tmpErrorMessage: String?
          if (_stmt.isNull(_columnIndexOfErrorMessage)) {
            _tmpErrorMessage = null
          } else {
            _tmpErrorMessage = _stmt.getText(_columnIndexOfErrorMessage)
          }
          _item =
              OrderEntity(_tmpId,_tmpSignalId,_tmpSymbol,_tmpSide,_tmpType,_tmpStatus,_tmpExecutionMode,_tmpQuantity,_tmpPrice,_tmpStopPrice,_tmpTakeProfitsJson,_tmpStopLossesJson,_tmpTrailingStopPercent,_tmpTrailingStopActivationPrice,_tmpOcoLinkedOrderId,_tmpBracketParentId,_tmpTimeInForce,_tmpScheduledAt,_tmpFilledQuantity,_tmpFilledPrice,_tmpFee,_tmpFeeAsset,_tmpDonationAmount,_tmpRealizedPnl,_tmpSlippage,_tmpIsPaperTrade,_tmpCreatedAt,_tmpUpdatedAt,_tmpExecutedAt,_tmpClosedAt,_tmpBinanceOrderId,_tmpErrorMessage)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun updateStatus(
    orderId: String,
    status: String,
    updatedAt: Long,
  ) {
    val _sql: String = "UPDATE orders SET status = ?, updatedAt = ? WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, status)
        _argIndex = 2
        _stmt.bindLong(_argIndex, updatedAt)
        _argIndex = 3
        _stmt.bindText(_argIndex, orderId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun delete(orderId: String) {
    val _sql: String = "DELETE FROM orders WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, orderId)
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
