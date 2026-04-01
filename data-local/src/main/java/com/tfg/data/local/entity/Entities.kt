package com.tfg.data.local.entity

import androidx.room.*

@Entity(
    tableName = "orders",
    indices = [
        Index(value = ["status"]),
        Index(value = ["symbol"]),
        Index(value = ["signalId"]),
        Index(value = ["closedAt"])
    ]
)
data class OrderEntity(
    @PrimaryKey val id: String,
    val signalId: String? = null,
    val symbol: String,
    val side: String,
    val type: String,
    val status: String,
    val executionMode: String,
    val quantity: Double,
    val price: Double? = null,
    val stopPrice: Double? = null,
    val takeProfitsJson: String = "[]",
    val stopLossesJson: String = "[]",
    val trailingStopPercent: Double? = null,
    val trailingStopActivationPrice: Double? = null,
    val ocoLinkedOrderId: String? = null,
    val bracketParentId: String? = null,
    val timeInForce: String = "GTC",
    val scheduledAt: Long? = null,
    val filledQuantity: Double = 0.0,
    val filledPrice: Double = 0.0,
    val fee: Double = 0.0,
    val feeAsset: String = "",
    val donationAmount: Double = 0.0,
    val realizedPnl: Double = 0.0,
    val slippage: Double = 0.0,
    val isPaperTrade: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val executedAt: Long? = null,
    val closedAt: Long? = null,
    val binanceOrderId: Long? = null,
    val errorMessage: String? = null
)

@Entity(
    tableName = "signals",
    indices = [Index(value = ["status"])]
)
data class SignalEntity(
    @PrimaryKey val id: String,
    val symbol: String,
    val side: String,
    val entryPrice: Double,
    val takeProfitsJson: String,
    val stopLossesJson: String,
    val confidence: Double,
    val riskRewardRatio: Double,
    val expiresAt: Long,
    val receivedAt: Long,
    val status: String,
    val hmacSignature: String,
    val isExpired: Boolean = false,
    val wasExecuted: Boolean = false,
    val missedWhileOffline: Boolean = false
)

@Entity(tableName = "trading_pairs")
data class TradingPairEntity(
    @PrimaryKey val symbol: String,
    val baseAsset: String,
    val quoteAsset: String,
    val lastPrice: Double = 0.0,
    val priceChangePercent24h: Double = 0.0,
    val volume24h: Double = 0.0,
    val high24h: Double = 0.0,
    val low24h: Double = 0.0,
    val isWatchlisted: Boolean = false,
    val isActiveForTrading: Boolean = false,
    val minQty: Double = 0.0,
    val stepSize: Double = 0.0,
    val tickSize: Double = 0.0,
    val minNotional: Double = 0.0,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "candles",
    primaryKeys = ["symbol", "interval", "openTime"],
    indices = [Index(value = ["symbol", "interval"])]
)
data class CandleEntity(
    val symbol: String,
    val interval: String,
    val openTime: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double,
    val closeTime: Long,
    val quoteVolume: Double = 0.0,
    val numberOfTrades: Int = 0
)

@Entity(tableName = "asset_balances", primaryKeys = ["asset", "walletType"])
data class AssetBalanceEntity(
    val asset: String,
    val free: Double,
    val locked: Double,
    val usdValue: Double = 0.0,
    val allocationPercent: Double = 0.0,
    val walletType: String = "SPOT",
    val isPaper: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey val id: String,
    val action: String,
    val category: String,
    val details: String,
    val oldValue: String? = null,
    val newValue: String? = null,
    val orderId: String? = null,
    val symbol: String? = null,
    val userId: String,
    val ipAddress: String? = null,
    val timestamp: Long
)

@Entity(tableName = "fee_records")
data class FeeRecordEntity(
    @PrimaryKey val id: String,
    val orderId: String,
    val symbol: String,
    val feeAmount: Double,
    val feeAsset: String,
    val feeType: String,
    val timestamp: Long
)

@Entity(tableName = "donations")
data class DonationEntity(
    @PrimaryKey val id: String,
    val orderId: String,
    val amount: Double,
    val currency: String,
    val ngoName: String,
    val ngoId: String,
    val status: String,
    val timestamp: Long
)

@Entity(tableName = "scripts")
data class ScriptEntity(
    @PrimaryKey val id: String,
    val name: String,
    val code: String,
    val isActive: Boolean = false,
    val activeSymbol: String? = null,
    val strategyTemplateId: String? = null,
    val paramsJson: String? = null,
    val lastRun: Long? = null,
    val backtestResultJson: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "offline_queue")
data class OfflineQueueEntity(
    @PrimaryKey val id: String,
    val signalJson: String? = null,
    val orderJson: String? = null,
    val action: String,
    val priority: Int = 0,
    val createdAt: Long,
    val retryCount: Int = 0,
    val maxRetries: Int = 5,
    val lastError: String? = null
)

@Entity(tableName = "signal_markers")
data class SignalMarkerEntity(
    @PrimaryKey val id: String,
    val scriptId: String,
    val symbol: String,
    val interval: String,
    val openTime: Long,
    val signalType: String,
    val price: Double,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "custom_templates")
data class CustomTemplateEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val baseTemplateId: String,
    val code: String,
    val defaultParamsJson: String? = null,
    val createdAt: Long
)

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey val id: String,
    val symbol: String,
    val name: String,
    val type: String,
    val condition: String,
    val targetValue: Double,
    val secondaryValue: Double? = null,
    val interval: String = "1h",
    val isEnabled: Boolean = true,
    val isRepeating: Boolean = false,
    val repeatIntervalSec: Int = 60,
    val lastTriggeredAt: Long? = null,
    val triggerCount: Int = 0,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "indicators")
data class IndicatorEntity(
    @PrimaryKey val id: String,
    val name: String,
    val code: String,
    val isEnabled: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long
)
