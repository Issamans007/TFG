package com.tfg.data.remote.api

import com.tfg.data.remote.dto.*
import retrofit2.http.*

interface BinanceApi {

    @GET("api/v3/time")
    suspend fun getServerTime(): ServerTimeDto

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
        @Query("recvWindow") recvWindow: Long? = null,
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
        @Field("recvWindow") recvWindow: Long? = null,
        @Field("signature") signature: String
    ): OrderResponseDto

    @DELETE("api/v3/order")
    suspend fun cancelOrder(
        @Query("symbol") symbol: String,
        @Query("orderId") orderId: Long,
        @Query("timestamp") timestamp: Long,
        @Query("recvWindow") recvWindow: Long? = null,
        @Query("signature") signature: String
    ): CancelOrderResponseDto

    @GET("api/v3/openOrders")
    suspend fun getOpenOrders(
        @Query("symbol") symbol: String? = null,
        @Query("timestamp") timestamp: Long,
        @Query("recvWindow") recvWindow: Long? = null,
        @Query("signature") signature: String
    ): List<OrderResponseDto>

    @GET("api/v3/allOrders")
    suspend fun getAllOrders(
        @Query("symbol") symbol: String,
        @Query("limit") limit: Int = 50,
        @Query("timestamp") timestamp: Long,
        @Query("recvWindow") recvWindow: Long? = null,
        @Query("signature") signature: String
    ): List<OrderResponseDto>

    @GET("api/v3/order")
    suspend fun queryOrder(
        @Query("symbol") symbol: String,
        @Query("origClientOrderId") clientOrderId: String,
        @Query("timestamp") timestamp: Long,
        @Query("recvWindow") recvWindow: Long? = null,
        @Query("signature") signature: String
    ): OrderResponseDto

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
        @Field("recvWindow") recvWindow: Long? = null,
        @Field("signature") signature: String
    ): OcoOrderResponseDto

    @POST("sapi/v1/asset/get-funding-asset")
    @FormUrlEncoded
    suspend fun getFundingAsset(
        @Field("timestamp") timestamp: Long,
        @Field("recvWindow") recvWindow: Long? = null,
        @Field("signature") signature: String
    ): List<FundingAssetDto>

    // ─── User Data Stream (listenKey) ───
    // These endpoints only require X-MBX-APIKEY — not signed.
    @POST("api/v3/userDataStream")
    suspend fun createSpotListenKey(): ListenKeyDto

    @PUT("api/v3/userDataStream")
    suspend fun keepaliveSpotListenKey(@Query("listenKey") listenKey: String): retrofit2.Response<Unit>

    @DELETE("api/v3/userDataStream")
    suspend fun closeSpotListenKey(@Query("listenKey") listenKey: String): retrofit2.Response<Unit>
}

interface BinanceFuturesApi {
    @GET("fapi/v1/time")
    suspend fun getFuturesServerTime(): ServerTimeDto

    @GET("fapi/v1/exchangeInfo")
    suspend fun getFuturesExchangeInfo(): ExchangeInfoDto

    @GET("fapi/v2/balance")
    suspend fun getFuturesBalance(
        @Query("timestamp") timestamp: Long,
        @Query("recvWindow") recvWindow: Long? = null,
        @Query("signature") signature: String
    ): List<FuturesBalanceDto>

    @GET("fapi/v2/account")
    suspend fun getFuturesAccount(
        @Query("timestamp") timestamp: Long,
        @Query("recvWindow") recvWindow: Long? = null,
        @Query("signature") signature: String
    ): FuturesAccountDto

    @GET("fapi/v2/positionRisk")
    suspend fun getPositionRisk(
        @Query("symbol") symbol: String? = null,
        @Query("timestamp") timestamp: Long,
        @Query("recvWindow") recvWindow: Long? = null,
        @Query("signature") signature: String
    ): List<FuturesPositionDto>

    @GET("fapi/v1/klines")
    suspend fun getFuturesKlines(
        @Query("symbol") symbol: String,
        @Query("interval") interval: String,
        @Query("limit") limit: Int = 500,
        @Query("startTime") startTime: Long? = null,
        @Query("endTime") endTime: Long? = null
    ): List<List<Any>>

    @GET("fapi/v1/premiumIndex")
    suspend fun getMarkPrice(
        @Query("symbol") symbol: String? = null
    ): Any

    @GET("fapi/v1/fundingRate")
    suspend fun getFundingRate(
        @Query("symbol") symbol: String,
        @Query("limit") limit: Int = 100
    ): List<FundingRateDto>

    @POST("fapi/v1/order")
    @FormUrlEncoded
    suspend fun placeFuturesOrder(
        @Field("symbol") symbol: String,
        @Field("side") side: String,
        @Field("positionSide") positionSide: String? = null,
        @Field("type") type: String,
        @Field("timeInForce") timeInForce: String? = null,
        @Field("quantity") quantity: String? = null,
        @Field("reduceOnly") reduceOnly: String? = null,
        @Field("closePosition") closePosition: String? = null,
        @Field("price") price: String? = null,
        @Field("stopPrice") stopPrice: String? = null,
        @Field("workingType") workingType: String? = null,
        @Field("newClientOrderId") clientOrderId: String? = null,
        @Field("timestamp") timestamp: Long,
        @Field("recvWindow") recvWindow: Long? = null,
        @Field("signature") signature: String
    ): FuturesOrderResponseDto

    @DELETE("fapi/v1/order")
    suspend fun cancelFuturesOrder(
        @Query("symbol") symbol: String,
        @Query("orderId") orderId: Long,
        @Query("timestamp") timestamp: Long,
        @Query("recvWindow") recvWindow: Long? = null,
        @Query("signature") signature: String
    ): FuturesOrderResponseDto

    @GET("fapi/v1/order")
    suspend fun queryFuturesOrder(
        @Query("symbol") symbol: String,
        @Query("origClientOrderId") clientOrderId: String,
        @Query("timestamp") timestamp: Long,
        @Query("recvWindow") recvWindow: Long? = null,
        @Query("signature") signature: String
    ): FuturesOrderResponseDto

    @GET("fapi/v1/openOrders")
    suspend fun getFuturesOpenOrders(
        @Query("symbol") symbol: String? = null,
        @Query("timestamp") timestamp: Long,
        @Query("recvWindow") recvWindow: Long? = null,
        @Query("signature") signature: String
    ): List<FuturesOrderResponseDto>

    @POST("fapi/v1/leverage")
    @FormUrlEncoded
    suspend fun changeLeverage(
        @Field("symbol") symbol: String,
        @Field("leverage") leverage: Int,
        @Field("timestamp") timestamp: Long,
        @Field("recvWindow") recvWindow: Long? = null,
        @Field("signature") signature: String
    ): LeverageChangeResponseDto

    @POST("fapi/v1/marginType")
    @FormUrlEncoded
    suspend fun changeMarginType(
        @Field("symbol") symbol: String,
        @Field("marginType") marginType: String,
        @Field("timestamp") timestamp: Long,
        @Field("recvWindow") recvWindow: Long? = null,
        @Field("signature") signature: String
    ): MarginTypeChangeResponseDto

    // ─── Hedge mode detection ───
    @GET("fapi/v1/positionSide/dual")
    suspend fun getPositionSideDual(
        @Query("timestamp") timestamp: Long,
        @Query("recvWindow") recvWindow: Long? = null,
        @Query("signature") signature: String
    ): PositionSideDualDto

    // ─── Historical funding rate ───
    // Already declared above at fundingRate — reuse for backtest funding sim.

    // ─── User Data Stream (listenKey) ───
    @POST("fapi/v1/listenKey")
    suspend fun createFuturesListenKey(): ListenKeyDto

    @PUT("fapi/v1/listenKey")
    suspend fun keepaliveFuturesListenKey(): retrofit2.Response<Unit>

    @DELETE("fapi/v1/listenKey")
    suspend fun closeFuturesListenKey(): retrofit2.Response<Unit>
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
