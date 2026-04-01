package com.tfg.data.remote.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Named

class BinanceAuthInterceptor @Inject constructor(
    @Named("apiKey") private val apiKeyProvider: () -> String
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val apiKey = apiKeyProvider()
        if (apiKey.isBlank()) return chain.proceed(original)

        val newRequest = original.newBuilder()
            .addHeader("X-MBX-APIKEY", apiKey)
            .build()
        return chain.proceed(newRequest)
    }
}

object BinanceSigner {
    fun sign(queryString: String, secretKey: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secretKey.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(queryString.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    fun signParams(params: Map<String, String>, secretKey: String): String {
        val queryString = params.entries.joinToString("&") { "${it.key}=${it.value}" }
        return sign(queryString, secretKey)
    }
}
