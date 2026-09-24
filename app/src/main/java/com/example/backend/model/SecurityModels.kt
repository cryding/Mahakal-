package com.example.backend.model

data class RefreshTokenRequest(
    val refreshToken: String
)

data class SessionInfoDto(
    val sessionId: String,
    val accountId: String,
    val role: String,
    val createdAt: Long,
    val expiresAt: Long,
    val lastActivityAt: Long,
    val userAgent: String?,
    val ipAddress: String?,
    val isRevoked: Boolean
)

data class SecurityEventDto(
    val id: String,
    val timestamp: Long,
    val eventType: String,
    val actorId: String,
    val actorRole: String,
    val targetId: String?,
    val targetType: String?,
    val requestId: String,
    val detailsJson: String
)

data class SecurityDashboardSummaryDto(
    val failedLoginCount: Int,
    val lockedAccountsCount: Int,
    val activeSessionsCount: Int,
    val revokedSessionsCount: Int,
    val authorizationFailuresCount: Int,
    val idorAttemptsCount: Int,
    val rateLimitExceededCount: Int,
    val tokenReuseCount: Int,
    val recentSecurityEvents: List<SecurityEventDto>,
    val systemStatus: String = "HEALTHY"
)

data class SecurityEventsQueryResponse(
    val events: List<SecurityEventDto>,
    val totalCount: Int,
    val page: Int,
    val totalPages: Int
)

data class HealthCheckDto(
    val status: String,
    val timestamp: Long,
    val version: String = "1.0.0",
    val database: String = "UP",
    val checks: Map<String, String> = emptyMap()
)

data class ReadinessCheckDto(
    val ready: Boolean,
    val timestamp: Long,
    val database: String,
    val outboxWorker: String,
    val services: Map<String, Boolean> = emptyMap()
)
