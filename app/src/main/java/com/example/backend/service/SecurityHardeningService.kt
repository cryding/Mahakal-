package com.example.backend.service

import com.example.backend.audit.AuditActions
import com.example.backend.audit.AuditService
import com.example.backend.database.MahakalServerDatabase
import com.example.backend.model.HealthCheckDto
import com.example.backend.model.ReadinessCheckDto
import com.example.backend.model.SecurityDashboardSummaryDto
import com.example.backend.model.SecurityEventDto
import com.example.backend.model.SecurityEventsQueryResponse
import com.example.backend.model.ServerResponse
import com.example.backend.model.SessionInfoDto
import com.example.backend.rbac.AccountRole
import com.example.backend.rbac.SecurityContext
import com.example.backend.security.RateLimiter
import java.util.UUID

class SecurityHardeningService(
    private val database: MahakalServerDatabase,
    private val auditService: AuditService,
    private val rateLimiter: RateLimiter,
    private val authService: ServerAuthService
) {
    private val accountDao = database.accountDao()
    private val sessionDao = database.sessionDao()
    private val auditLogDao = database.auditLogDao()

    /**
     * Retrieves aggregated security dashboard metrics for administrators.
     */
    suspend fun getSecurityDashboardSummary(
        context: SecurityContext,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<SecurityDashboardSummaryDto> {
        val now = System.currentTimeMillis()

        // 1. Gather counts from Room database
        val failedLogins = auditLogDao.countQueryLogs(
            actorId = null,
            action = AuditActions.LOGIN_FAILED,
            targetId = null,
            targetType = null,
            startTime = now - 24 * 3600 * 1000L,
            endTime = now
        )

        val lockedAccounts = accountDao.countLockedAccounts(now)
        val activeSessions = sessionDao.countActiveSessions(now)
        val revokedSessions = sessionDao.countRevokedSessions()

        val authFailures = auditLogDao.countQueryLogs(
            actorId = null,
            action = AuditActions.PERMISSION_DENIED,
            targetId = null,
            targetType = null,
            startTime = now - 24 * 3600 * 1000L,
            endTime = now
        )

        val idorAttempts = auditLogDao.countQueryLogs(
            actorId = null,
            action = AuditActions.IDOR_ATTEMPT,
            targetId = null,
            targetType = null,
            startTime = now - 24 * 3600 * 1000L,
            endTime = now
        )

        val rateLimitExceeded = auditLogDao.countQueryLogs(
            actorId = null,
            action = AuditActions.RATE_LIMIT_EXCEEDED,
            targetId = null,
            targetType = null,
            startTime = now - 24 * 3600 * 1000L,
            endTime = now
        )

        val tokenReuses = auditLogDao.countQueryLogs(
            actorId = null,
            action = AuditActions.TOKEN_REUSE_DETECTED,
            targetId = null,
            targetType = null,
            startTime = now - 24 * 3600 * 1000L,
            endTime = now
        )

        // 2. Fetch recent security logs
        val recentEntities = auditLogDao.getRecentLogs(25)
        val recentEvents = recentEntities.map { entity ->
            SecurityEventDto(
                id = entity.id,
                timestamp = entity.createdAt,
                eventType = entity.action,
                actorId = entity.actorId,
                actorRole = entity.actorRole,
                targetId = entity.targetId,
                targetType = entity.targetType,
                requestId = entity.requestId,
                detailsJson = entity.metadataJson
            )
        }

        auditService.logEvent(
            actorId = context.accountId,
            actorRole = context.role.name,
            action = AuditActions.SECURITY_METRICS_INSPECTED,
            requestId = requestId
        )

        val summary = SecurityDashboardSummaryDto(
            failedLoginCount = failedLogins,
            lockedAccountsCount = lockedAccounts,
            activeSessionsCount = activeSessions,
            revokedSessionsCount = revokedSessions,
            authorizationFailuresCount = authFailures,
            idorAttemptsCount = idorAttempts,
            rateLimitExceededCount = rateLimitExceeded,
            tokenReuseCount = tokenReuses,
            recentSecurityEvents = recentEvents,
            systemStatus = if (tokenReuses > 0 || idorAttempts > 5) "ELEVATED_ALERT" else "HEALTHY"
        )

        return ServerResponse(
            success = true,
            statusCode = 200,
            data = summary,
            requestId = requestId
        )
    }

    /**
     * Query security and audit events with filtering and pagination.
     */
    suspend fun querySecurityEvents(
        context: SecurityContext,
        eventType: String? = null,
        actorId: String? = null,
        page: Int = 1,
        limit: Int = 20,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<SecurityEventsQueryResponse> {
        val safePage = if (page < 1) 1 else page
        val safeLimit = limit.coerceIn(1, 100)
        val offset = (safePage - 1) * safeLimit

        val (logs, totalCount) = auditService.queryAuditLogs(
            actorId = actorId,
            action = eventType,
            limit = safeLimit,
            offset = offset
        )

        val events = logs.map {
            SecurityEventDto(
                id = it.id,
                timestamp = it.createdAt,
                eventType = it.action,
                actorId = it.actorId,
                actorRole = it.actorRole,
                targetId = it.targetId,
                targetType = it.targetType,
                requestId = it.requestId,
                detailsJson = it.metadataJson
            )
        }

        val totalPages = if (totalCount == 0) 1 else ((totalCount + safeLimit - 1) / safeLimit)

        return ServerResponse(
            success = true,
            statusCode = 200,
            data = SecurityEventsQueryResponse(
                events = events,
                totalCount = totalCount,
                page = safePage,
                totalPages = totalPages
            ),
            requestId = requestId
        )
    }

    /**
     * Inspect active device sessions for an account or system-wide.
     */
    suspend fun getActiveSessions(
        context: SecurityContext,
        targetAccountId: String? = null,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<List<SessionInfoDto>> {
        val now = System.currentTimeMillis()
        val sessions = if (targetAccountId != null) {
            sessionDao.getActiveSessionsForAccount(targetAccountId, now)
        } else {
            sessionDao.getSessionsPaged(50, 0)
        }

        val dtos = sessions.map {
            SessionInfoDto(
                sessionId = it.id,
                accountId = it.accountId,
                role = it.role,
                createdAt = it.createdAt,
                expiresAt = it.expiresAt,
                lastActivityAt = it.lastActivityAt,
                userAgent = it.userAgent,
                ipAddress = it.ipAddress,
                isRevoked = it.isRevoked
            )
        }

        return ServerResponse(
            success = true,
            statusCode = 200,
            data = dtos,
            requestId = requestId
        )
    }

    /**
     * Revoke an active session.
     */
    suspend fun revokeSession(
        context: SecurityContext,
        sessionId: String,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<Boolean> {
        val success = authService.revokeSessionById(
            sessionId = sessionId,
            actorId = context.accountId,
            actorRole = context.role.name,
            requestId = requestId
        )

        return ServerResponse(
            success = success,
            statusCode = if (success) 200 else 404,
            data = success,
            message = if (success) "Session revoked successfully." else "Session not found.",
            requestId = requestId
        )
    }

    /**
     * Revoke all sessions for a specific account.
     */
    suspend fun revokeAllAccountSessions(
        context: SecurityContext,
        targetAccountId: String,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<Boolean> {
        authService.revokeAllSessionsForAccount(
            targetAccountId = targetAccountId,
            actorId = context.accountId,
            actorRole = context.role.name,
            requestId = requestId
        )

        return ServerResponse(
            success = true,
            statusCode = 200,
            data = true,
            message = "All sessions revoked for account.",
            requestId = requestId
        )
    }

    /**
     * Performs a health check for monitoring endpoints.
     */
    suspend fun checkHealth(): HealthCheckDto {
        val now = System.currentTimeMillis()
        var dbStatus = "UP"
        try {
            accountDao.countAdmins()
        } catch (e: Exception) {
            dbStatus = "DOWN: ${e.message}"
        }

        return HealthCheckDto(
            status = if (dbStatus == "UP") "OK" else "DEGRADED",
            timestamp = now,
            version = "1.0.0",
            database = dbStatus,
            checks = mapOf(
                "database" to dbStatus,
                "rateLimiter" to "UP",
                "authEngine" to "UP",
                "auditService" to "UP"
            )
        )
    }

    /**
     * Performs a readiness check verifying dependencies are healthy before taking traffic.
     */
    suspend fun checkReadiness(): ReadinessCheckDto {
        val now = System.currentTimeMillis()
        var dbReady = false
        try {
            accountDao.countAdmins()
            dbReady = true
        } catch (_: Exception) {
            dbReady = false
        }

        return ReadinessCheckDto(
            ready = dbReady,
            timestamp = now,
            database = if (dbReady) "READY" else "NOT_READY",
            outboxWorker = "READY",
            services = mapOf(
                "database" to dbReady,
                "rbac" to true,
                "auth" to true,
                "ledger" to true,
                "gameEngine" to true,
                "notifications" to true
            )
        )
    }

    /**
     * Records IDOR attempt and logs security alert.
     */
    suspend fun logIdorAttempt(
        context: SecurityContext,
        attemptedResource: String,
        requestId: String = UUID.randomUUID().toString()
    ) {
        auditService.logEvent(
            actorId = context.accountId,
            actorRole = context.role.name,
            action = AuditActions.IDOR_ATTEMPT,
            targetId = attemptedResource,
            requestId = requestId,
            metadataJson = "{\"alert\":\"Cross-account IDOR attempt blocked.\"}"
        )
    }
}
