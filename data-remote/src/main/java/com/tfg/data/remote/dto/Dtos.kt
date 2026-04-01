package com.tfg.data.remote.dto

import com.google.gson.annotations.SerializedName

// === Binance DTOs ===

data class ExchangeInfoDto(
    val symbols: List<SymbolInfoDto>
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
