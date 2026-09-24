package com.example.data.repository

import com.example.backend.api.ServerApiRouter
import com.example.backend.model.HealthCheckDto
import com.example.backend.model.SecurityDashboardSummaryDto
import com.example.backend.model.SecurityEventsQueryResponse
import com.example.backend.model.ServerResponse
import com.example.backend.model.SessionInfoDto
import com.example.core.security.SecureTokenStorage

interface SecurityRepository {
    suspend fun getSecurityDashboardSummary(): ServerResponse<SecurityDashboardSummaryDto>
    suspend fun getActiveSessions(targetAccountId: String? = null): ServerResponse<List<SessionInfoDto>>
    suspend fun revokeSession(sessionId: String): ServerResponse<Boolean>
    suspend fun revokeAllAccountSessions(targetAccountId: String): ServerResponse<Boolean>
    suspend fun querySecurityEvents(
        eventType: String? = null,
        actorId: String? = null,
        page: Int = 1,
        limit: Int = 20
    ): ServerResponse<SecurityEventsQueryResponse>
    suspend fun getHealthCheck(): HealthCheckDto
}

class SecurityRepositoryImpl(
    private val apiRouter: ServerApiRouter,
    private val secureStorage: SecureTokenStorage
) : SecurityRepository {

    private fun getAuthHeader(): String? {
        val token = secureStorage.getAccessToken() ?: return null
        return "Bearer $token"
    }

    private fun <T> unauthorized(): ServerResponse<T> {
        return ServerResponse(
            success = false,
            statusCode = 401,
            errorCode = "UNAUTHORIZED",
            message = "Authentication required."
        )
    }

    override suspend fun getSecurityDashboardSummary(): ServerResponse<SecurityDashboardSummaryDto> {
        val authHeader = getAuthHeader() ?: return unauthorized()
        return apiRouter.handleGetSecurityDashboardSummary(authHeader)
    }

    override suspend fun getActiveSessions(targetAccountId: String?): ServerResponse<List<SessionInfoDto>> {
        val authHeader = getAuthHeader() ?: return unauthorized()
        return apiRouter.handleGetActiveSessions(authHeader, targetAccountId)
    }

    override suspend fun revokeSession(sessionId: String): ServerResponse<Boolean> {
        val authHeader = getAuthHeader() ?: return unauthorized()
        return apiRouter.handleRevokeSession(authHeader, sessionId)
    }

    override suspend fun revokeAllAccountSessions(targetAccountId: String): ServerResponse<Boolean> {
        val authHeader = getAuthHeader() ?: return unauthorized()
        return apiRouter.handleRevokeAllAccountSessions(authHeader, targetAccountId)
    }

    override suspend fun querySecurityEvents(
        eventType: String?,
        actorId: String?,
        page: Int,
        limit: Int
    ): ServerResponse<SecurityEventsQueryResponse> {
        val authHeader = getAuthHeader() ?: return unauthorized()
        return apiRouter.handleQuerySecurityEvents(authHeader, eventType, actorId, page, limit)
    }

    override suspend fun getHealthCheck(): HealthCheckDto {
        return apiRouter.handleHealthCheck()
    }
}
