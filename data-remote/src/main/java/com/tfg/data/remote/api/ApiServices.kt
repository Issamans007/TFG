package com.tfg.data.remote.api

import com.tfg.data.remote.dto.*
import retrofit2.http.*

interface BinanceApi {

    @GET("api/v3/exchangeInfo")
    suspend fun getExchangeInfo(): ExchangeInfoDto

    @GET("api/v3/ticker/24hr")
    suspend fun get24hrTickers(): List<TickerDto>

    @GET("api/v3/ticker/24hr")
    suspend fun get24hrTicker(@Query("symbol") symbol: String): TickerDto

    @GET("api/v3/ticker/price")
    suspend fun getPriceTicker(@Query("symbol") symbol: String): PriceTickerDto

    @GET("api/v3/klines")
    suspend fun getKlines(
        @Query("symbol") symbol: String,
        @Query("interval") interval: String,
        @Query("limit") limit: Int = 500,
        @Query("startTime") startTime: Long? = null,
        @Query("endTime") endTime: Long? = null
    ): List<List<Any>>

    @GET("api/v3/account")
    suspend fun getAccount(
        @Query("timestamp") timestamp: Long,
        @Query("signature") signature: String
    ): AccountDto

    @POST("api/v3/order")
    @FormUrlEncoded
    suspend fun placeOrder(
        @Field("symbol") symbol: String,
        @Field("side") side: String,
        @Field("type") type: String,
        @Field("timeInForce") timeInForce: String? = null,
        @Field("quantity") quantity: String,
        @Field("price") price: String? = null,
        @Field("stopPrice") stopPrice: String? = null,
        @Field("newClientOrderId") clientOrderId: String? = null,
        @Field("timestamp") timestamp: Long,
        @Field("signature") signature: String
    ): OrderResponseDto

    @DELETE("api/v3/order")
    suspend fun cancelOrder(
        @Query("symbol") symbol: String,
        @Query("orderId") orderId: Long,
        @Query("timestamp") timestamp: Long,
        @Query("signature") signature: String
    ): CancelOrderResponseDto

    @GET("api/v3/openOrders")
    suspend fun getOpenOrders(
        @Query("symbol") symbol: String? = null,
        @Query("timestamp") timestamp: Long,
        @Query("signature") signature: String
    ): List<OrderResponseDto>

    @GET("api/v3/allOrders")
    suspend fun getAllOrders(
        @Query("symbol") symbol: String,
        @Query("limit") limit: Int = 50,
        @Query("timestamp") timestamp: Long,
        @Query("signature") signature: String
    ): List<OrderResponseDto>

    @POST("api/v3/order/oco")
    @FormUrlEncoded
    suspend fun placeOcoOrder(
        @Field("symbol") symbol: String,
        @Field("side") side: String,
        @Field("quantity") quantity: String,
        @Field("price") price: String,
        @Field("stopPrice") stopPrice: String,
        @Field("stopLimitPrice") stopLimitPrice: String,
        @Field("stopLimitTimeInForce") stopLimitTimeInForce: String = "GTC",
        @Field("timestamp") timestamp: Long,
        @Field("signature") signature: String
    ): OcoOrderResponseDto

    @POST("sapi/v1/asset/get-funding-asset")
    @FormUrlEncoded
    suspend fun getFundingAsset(
        @Field("timestamp") timestamp: Long,
        @Field("signature") signature: String
    ): List<FundingAssetDto>
}

interface BinanceFuturesApi {
    @GET("fapi/v2/balance")
    suspend fun getFuturesBalance(
        @Query("timestamp") timestamp: Long,
        @Query("signature") signature: String
    ): List<FuturesBalanceDto>
}

interface TfgServerApi {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequestDto): LoginResponseDto

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequestDto): LoginResponseDto

    @POST("auth/otp/send")
    suspend fun sendOtp(@Body request: OtpRequestDto): OtpResponseDto

    @POST("auth/otp/verify")
    suspend fun verifyOtp(@Body request: OtpVerifyDto): OtpResponseDto

    @POST("auth/kyc")
    suspend fun submitKyc(@Body request: KycRequestDto): KycResponseDto

    @GET("market/fear-greed")
    suspend fun getFearGreedIndex(): FearGreedDto

    @GET("market/btc-dominance")
    suspend fun getBtcDominance(): BtcDominanceDto
}
