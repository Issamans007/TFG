package com.tfg.data.remote.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.tfg.data.remote.BuildConfig
import com.tfg.data.remote.api.BinanceApi
import com.tfg.data.remote.api.BinanceFuturesApi
import com.tfg.data.remote.api.TfgServerApi
import com.tfg.data.remote.interceptor.BinanceAuthInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.CertificatePinner
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
        HttpLoggingInterceptor().apply {
            // Body-level logging exposes balances, order payloads, and signed
            // query strings (signature, apiKey). Logcat is readable by ADB
            // pulls, OEM telemetry and crash uploaders, so disable in release.
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
            // Even at BASIC, never print these headers.
            redactHeader("X-MBX-APIKEY")
            redactHeader("Authorization")
        }

    @Provides
    @Singleton
    @Named("binance")
    fun provideBinanceOkHttp(
        loggingInterceptor: HttpLoggingInterceptor,
        binanceAuthInterceptor: BinanceAuthInterceptor
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(binanceAuthInterceptor)
        .addInterceptor(loggingInterceptor)
        .certificatePinner(binanceCertificatePinner())
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    @Named("websocket")
    fun provideWebSocketOkHttp(): OkHttpClient = OkHttpClient.Builder()
        .certificatePinner(binanceCertificatePinner())
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MINUTES)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    /**
     * Pins TLS to Binance's leaf + backup intermediate fingerprints so a
     * compromised CA (e.g. a malicious one trusted by the device or pushed via
     * MDM) cannot man-in-the-middle the trading API. Pins below are SHA-256
     * fingerprints of the SubjectPublicKeyInfo for the Let's Encrypt / DigiCert
     * intermediates currently chaining api.binance.com / fapi.binance.com /
     * stream.binance.com (rotated every ~3 months \u2014 update before expiry).
     *
     * Backup pins are included so a single forced rotation doesn't brick the
     * app; ship a build-time check that fails CI if all pins are within 30
     * days of expiry.
     */
    private fun binanceCertificatePinner(): CertificatePinner = CertificatePinner.Builder()
        // DigiCert Global Root G2 (long-lived root used by Binance leaf chain)
        .add("api.binance.com", "sha256/CLOmM1/OXvSPjw5UOYbAf9GKOxImEp9hhku9W90fHMk=")
        .add("api.binance.com", "sha256/i7WTqTvh0OioIruIfFR4kMPnBqrS2rdiVPl/s2uC/CY=")
        .add("fapi.binance.com", "sha256/CLOmM1/OXvSPjw5UOYbAf9GKOxImEp9hhku9W90fHMk=")
        .add("fapi.binance.com", "sha256/i7WTqTvh0OioIruIfFR4kMPnBqrS2rdiVPl/s2uC/CY=")
        .add("stream.binance.com", "sha256/CLOmM1/OXvSPjw5UOYbAf9GKOxImEp9hhku9W90fHMk=")
        .add("stream.binance.com", "sha256/i7WTqTvh0OioIruIfFR4kMPnBqrS2rdiVPl/s2uC/CY=")
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
