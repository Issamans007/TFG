package com.tfg.data.remote.dto

import com.google.gson.annotations.SerializedName

// === Binance DTOs ===

data class ExchangeInfoDto(
    val symbols: List<SymbolInfoDto>
)

data class ServerTimeDto(
    val serverTime: Long
)

data class SymbolInfoDto(
    val symbol: String,
    val baseAsset: String,
    val quoteAsset: String,
    val status: String,
    val filters: List<FilterDto>
)

data class FilterDto(
    val filterType: String,
    val minQty: String? = null,
    val stepSize: String? = null,
    val tickSize: String? = null,
    val minNotional: String? = null,
    val minPrice: String? = null
)

data class TickerDto(
    val symbol: String,
    val lastPrice: String,
    val priceChangePercent: String,
    val volume: String,
    val highPrice: String,
    val lowPrice: String,
    val quoteVolume: String
)

data class PriceTickerDto(
    val symbol: String,
    val price: String
)

data class AccountDto(
    val balances: List<BalanceDto>,
    val canTrade: Boolean,
    val canWithdraw: Boolean,
    val canDeposit: Boolean,
    val makerCommission: Int,
    val takerCommission: Int
)

data class BalanceDto(
    val asset: String,
    val free: String,
    val locked: String
)

data class FundingAssetDto(
    val asset: String,
    val free: String,
    val locked: String,
    val freeze: String,
    val withdrawing: String,
    val btcValuation: String
)

data class FuturesBalanceDto(
    val accountAlias: String = "",
    val asset: String,
    val balance: String,
    val crossWalletBalance: String = "0",
    val crossUnPnl: String = "0",
    val availableBalance: String,
    val maxWithdrawAmount: String = "0",
    val marginAvailable: Boolean = false,
    val updateTime: Long = 0
)

data class OrderResponseDto(
    val symbol: String,
    val orderId: Long,
    val clientOrderId: String,
    val price: String,
    val origQty: String,
    val executedQty: String,
    val cummulativeQuoteQty: String,
    val status: String,
    val type: String,
    val side: String,
    val stopPrice: String? = null,
    val time: Long,
    val updateTime: Long,
    val fills: List<FillDto>? = null
)

data class FillDto(
    val price: String,
    val qty: String,
    val commission: String,
    val commissionAsset: String
)

data class CancelOrderResponseDto(
    val symbol: String,
    val orderId: Long,
    val status: String
)

data class OcoOrderResponseDto(
    val orderListId: Long,
    val listStatusType: String,
    val orders: List<OrderResponseDto>
)

// === Binance Futures (USDⓈ-M) DTOs ===

data class FuturesAccountDto(
    val totalWalletBalance: String = "0",
    val totalUnrealizedProfit: String = "0",
    val totalMarginBalance: String = "0",
    val totalInitialMargin: String = "0",
    val totalMaintMargin: String = "0",
    val totalCrossWalletBalance: String = "0",
    val availableBalance: String = "0",
    val maxWithdrawAmount: String = "0",
    val canTrade: Boolean = false,
    val multiAssetsMargin: Boolean = false,
    val positions: List<FuturesPositionDto> = emptyList()
)

data class FuturesPositionDto(
    val symbol: String,
    val positionAmt: String = "0",
    val entryPrice: String = "0",
    val markPrice: String = "0",
    val unRealizedProfit: String = "0",
    val liquidationPrice: String = "0",
    val leverage: String = "1",
    val maxNotionalValue: String? = null,
    val marginType: String = "isolated",
    val isolatedMargin: String = "0",
    val isAutoAddMargin: String = "false",
    val positionSide: String = "BOTH",
    val notional: String = "0",
    val isolatedWallet: String = "0",
    val updateTime: Long = 0
)

data class FuturesOrderResponseDto(
    val symbol: String,
    val orderId: Long,
    val clientOrderId: String = "",
    val price: String = "0",
    val origQty: String = "0",
    val executedQty: String = "0",
    val cumQuote: String = "0",
    val avgPrice: String = "0",
    val status: String = "NEW",
    val type: String = "MARKET",
    val side: String = "BUY",
    val positionSide: String = "BOTH",
    val stopPrice: String? = null,
    val workingType: String = "CONTRACT_PRICE",
    val reduceOnly: Boolean = false,
    val closePosition: Boolean = false,
    val time: Long = 0,
    val updateTime: Long = 0
)

data class FundingRateDto(
    val symbol: String,
    val fundingRate: String,
    val fundingTime: Long
)

data class LeverageChangeResponseDto(
    val leverage: Int,
    val maxNotionalValue: String? = null,
    val symbol: String
)

data class MarginTypeChangeResponseDto(
    val code: Int = 200,
    val msg: String = "success"
)

// === TFG Server DTOs ===

data class LoginRequestDto(val email: String, val password: String)
data class RegisterRequestDto(val email: String, val password: String, val displayName: String)
data class LoginResponseDto(
    val token: String,
    val user: UserDto
)

data class UserDto(
    val id: String,
    val email: String,
    val displayName: String,
    val kycStatus: String
)

data class OtpRequestDto(val email: String)
data class OtpVerifyDto(val email: String, val otp: String)
data class OtpResponseDto(val success: Boolean, val message: String)

data class KycRequestDto(val documentUrl: String)
data class KycResponseDto(val status: String, val message: String)

data class FearGreedDto(val value: Int, val classification: String)
data class BtcDominanceDto(val dominance: Double, val marketCap: Double)

// === WebSocket DTOs ===

data class TickerWsDto(
    @SerializedName("s") val symbol: String,
    @SerializedName("c") val lastPrice: String,
    @SerializedName("P") val priceChangePercent: String,
    @SerializedName("v") val volume: String,
    @SerializedName("h") val highPrice: String,
    @SerializedName("l") val lowPrice: String
)

data class KlineWsDto(
    @SerializedName("s") val symbol: String,
    val k: KlineDataDto
)

data class KlineDataDto(
    @SerializedName("t") val openTime: Long,
    @SerializedName("o") val open: String,
    @SerializedName("h") val high: String,
    @SerializedName("l") val low: String,
    @SerializedName("c") val close: String,
    @SerializedName("v") val volume: String,
    @SerializedName("T") val closeTime: Long,
    @SerializedName("q") val quoteVolume: String,
    @SerializedName("n") val numberOfTrades: Int,
    @SerializedName("x") val isFinal: Boolean
)

data class UserDataWsDto(
    @SerializedName("e") val eventType: String,
    @SerializedName("s") val symbol: String? = null,
    @SerializedName("S") val side: String? = null,
    @SerializedName("o") val orderType: String? = null,
    @SerializedName("c") val clientOrderId: String? = null,
    @SerializedName("q") val quantity: String? = null,
    @SerializedName("p") val price: String? = null,
    @SerializedName("X") val orderStatus: String? = null,
    @SerializedName("i") val orderId: Long? = null,
    @SerializedName("z") val filledQty: String? = null,
    @SerializedName("Z") val filledQuoteQty: String? = null,
    @SerializedName("n") val commission: String? = null,
    @SerializedName("N") val commissionAsset: String? = null,
    @SerializedName("T") val transactionTime: Long? = null
)

/** Response from POST /api/v3/userDataStream and POST /fapi/v1/listenKey. */
data class ListenKeyDto(
    @SerializedName("listenKey") val listenKey: String
)

/** Response from GET /fapi/v1/positionSide/dual. dualSidePosition=true → hedge mode. */
data class PositionSideDualDto(
    @SerializedName("dualSidePosition") val dualSidePosition: Boolean
)
