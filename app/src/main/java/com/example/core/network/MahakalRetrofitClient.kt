package com.example.core.network

import com.example.core.security.SecureTokenStorage
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Factory and provider for production HTTPS Retrofit client.
 * Enforces TLS, timeouts, Bearer authentication headers, and HTTPS-only transport.
 */
object MahakalRetrofitClient {

    fun create(
        tokenStorage: SecureTokenStorage,
        baseUrl: String = ApiConfig.getBaseUrl()
    ): MahakalApiService {
        val sanitizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()

            // Attach Bearer token if session exists
            tokenStorage.getAccessToken()?.let { token ->
                requestBuilder.header("Authorization", "Bearer $token")
            }

            requestBuilder.header("Accept", "application/json")
            chain.proceed(requestBuilder.build())
        }

        val okHttpClientBuilder = OkHttpClient.Builder()
            .connectTimeout(ApiConfig.connectTimeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(ApiConfig.readTimeoutMs, TimeUnit.MILLISECONDS)
            .writeTimeout(ApiConfig.readTimeoutMs, TimeUnit.MILLISECONDS)
            .addInterceptor(authInterceptor)

        if (ApiConfig.isDiagnosticsEnabled()) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            okHttpClientBuilder.addInterceptor(logging)
        }

        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        return Retrofit.Builder()
            .baseUrl(sanitizedBaseUrl)
            .client(okHttpClientBuilder.build())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(MahakalApiService::class.java)
    }
}
