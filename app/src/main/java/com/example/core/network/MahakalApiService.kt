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
    suspend fun getWalletTransactions(): Response<ServerResponse<List<Map<String, Any>>>>

    @POST("v1/wallets/transfer")
    suspend fun transferVirtualCoins(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: Map<String, Any>
    ): Response<ServerResponse<Map<String, Any>>>

    @POST("v1/wallets/deduct")
    suspend fun deductVirtualCoins(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: Map<String, Any>
    ): Response<ServerResponse<Map<String, Any>>>

    @GET("v1/admin/reports/reconciliation")
    suspend fun getReconciliationReport(): Response<ServerResponse<Map<String, Any>>>

    @POST("v1/auth/change-password")
    suspend fun changePassword(@Body request: Map<String, Any>): Response<ServerResponse<Map<String, Any>>>

    @GET("v1/games")
    suspend fun getGames(): Response<ServerResponse<List<Map<String, Any>>>>

    @GET("v1/games/my-entries")
    suspend fun getMyGameEntries(): Response<ServerResponse<List<Map<String, Any>>>>

    @POST("v1/games/enter")
    suspend fun enterGame(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: Map<String, Any>
    ): Response<ServerResponse<Map<String, Any>>>

    @GET("v1/admin/agents")
    suspend fun getAdminAgents(): Response<ServerResponse<List<Map<String, Any>>>>

    @POST("v1/admin/agents")
    suspend fun createAgent(@Body request: Map<String, Any>): Response<ServerResponse<Map<String, Any>>>

    @POST("v1/admin/agents/status")
    suspend fun updateAgentStatus(@Body request: Map<String, Any>): Response<ServerResponse<Map<String, Any>>>

    @GET("v1/admin/users")
    suspend fun getAdminUsers(): Response<ServerResponse<List<Map<String, Any>>>>

    @POST("v1/admin/users")
    suspend fun createAdminUser(@Body request: Map<String, Any>): Response<ServerResponse<Map<String, Any>>>

    @GET("v1/agent/users")
    suspend fun getAgentUsers(): Response<ServerResponse<List<Map<String, Any>>>>

    @POST("v1/agent/users")
    suspend fun createAgentUser(@Body request: Map<String, Any>): Response<ServerResponse<Map<String, Any>>>

    @POST("v1/admin/games/create")
    suspend fun createGame(@Body request: Map<String, Any>): Response<ServerResponse<Map<String, Any>>>

    @POST("v1/admin/games/edit")
    suspend fun editGame(@Body request: Map<String, Any>): Response<ServerResponse<Map<String, Any>>>

    @POST("v1/admin/games/status")
    suspend fun updateGameStatus(@Body request: Map<String, Any>): Response<ServerResponse<Map<String, Any>>>

    @POST("v1/admin/games/config-api")
    suspend fun configGameApi(@Body request: Map<String, Any>): Response<ServerResponse<Map<String, Any>>>

    @POST("v1/admin/games/finalize-result")
    suspend fun finalizeGameResult(@Body request: Map<String, Any>): Response<ServerResponse<Map<String, Any>>>

    @GET("v1/admin/audit-logs")
    suspend fun getAuditLogs(): Response<ServerResponse<List<Map<String, Any>>>>

    @GET("v1/admin/security/dashboard")
    suspend fun getSecurityDashboard(): Response<ServerResponse<Map<String, Any>>>

    @GET("v1/notifications")
    suspend fun getNotifications(): Response<ServerResponse<List<Map<String, Any>>>>

    @POST("v1/notifications/mark-read")
    suspend fun markNotificationRead(@Body request: Map<String, Any>): Response<ServerResponse<Map<String, Any>>>
}
