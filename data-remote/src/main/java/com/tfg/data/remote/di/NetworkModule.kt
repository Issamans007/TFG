package com.tfg.data.remote.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.tfg.data.remote.api.BinanceApi
import com.tfg.data.remote.api.BinanceFuturesApi
import com.tfg.data.remote.api.TfgServerApi
import com.tfg.data.remote.interceptor.BinanceAuthInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder().setLenient().create()

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }

    @Provides
    @Singleton
    @Named("binance")
    fun provideBinanceOkHttp(
        loggingInterceptor: HttpLoggingInterceptor,
        binanceAuthInterceptor: BinanceAuthInterceptor
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(binanceAuthInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    @Named("websocket")
    fun provideWebSocketOkHttp(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MINUTES)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideOkHttpClient(@Named("websocket") webSocketClient: OkHttpClient): OkHttpClient = webSocketClient

    @Provides
    @Singleton
    fun provideBinanceApi(
        @Named("binance") okHttpClient: OkHttpClient,
        gson: Gson
    ): BinanceApi = Retrofit.Builder()
        .baseUrl("https://api.binance.com/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
        .create(BinanceApi::class.java)

    @Provides
    @Singleton
    fun provideBinanceFuturesApi(
        @Named("binance") okHttpClient: OkHttpClient,
        gson: Gson
    ): BinanceFuturesApi = Retrofit.Builder()
        .baseUrl("https://fapi.binance.com/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
        .create(BinanceFuturesApi::class.java)

    @Provides
    @Singleton
    fun provideTfgServerApi(
        @Named("binance") okHttpClient: OkHttpClient,
        gson: Gson
    ): TfgServerApi = Retrofit.Builder()
        .baseUrl("https://api.tradeforgood.com/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
        .create(TfgServerApi::class.java)
}
