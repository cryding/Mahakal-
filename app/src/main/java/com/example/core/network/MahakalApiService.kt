package com.example.core.network

import com.example.backend.model.LoginRequest
import com.example.backend.model.LoginResponse
import com.example.backend.model.ServerResponse
import com.example.backend.model.UserProfileDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Authoritative Production HTTPS Retrofit API Service for MAHAKAL Cloud Backend.
 * All mutations and authoritative data flows go through this interface to Supabase PostgreSQL.
 */
interface MahakalApiService {

    @GET("health")
    suspend fun getHealth(): Response<Map<String, Any>>

    @GET("readiness")
    suspend fun getReadiness(): Response<Map<String, Any>>

    @POST("v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ServerResponse<LoginResponse>>

    @GET("v1/auth/me")
    suspend fun getProfile(): Response<ServerResponse<UserProfileDto>>

    @POST("v1/auth/logout")
    suspend fun logout(): Response<ServerResponse<Unit>>

    @GET("v1/wallets/me")
    suspend fun getMyWallet(): Response<ServerResponse<Map<String, Any>>>

    @GET("v1/wallets/transactions")
    suspend fun getWalletTransactions(): Response<ServerResponse<Map<String, Any>>>

    @POST("v1/wallets/transfer")
    suspend fun transferVirtualCoins(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: Map<String, Any>
    ): Response<ServerResponse<Map<String, Any>>>
}
