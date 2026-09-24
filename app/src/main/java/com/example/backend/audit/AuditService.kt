package com.example.backend.audit

import com.example.backend.database.dao.AuditLogDao
import com.example.backend.database.entity.AuditLogEntity
import java.util.UUID

object AuditActions {
    const val ADMIN_LOGIN = "ADMIN_LOGIN"
    const val AGENT_LOGIN = "AGENT_LOGIN"
    const val USER_LOGIN = "USER_LOGIN"
    const val LOGIN_FAILED = "LOGIN_FAILED"
    const val LOGOUT = "LOGOUT"
    const val PASSWORD_CHANGED = "PASSWORD_CHANGED"
    const val SESSION_REVOKED = "SESSION_REVOKED"
    const val ACCOUNT_SUSPENDED_ACCESS_ATTEMPT = "ACCOUNT_SUSPENDED_ACCESS_ATTEMPT"
    const val PERMISSION_DENIED = "PERMISSION_DENIED"
    const val ACCOUNT_LOCKED = "ACCOUNT_LOCKED"
    const val INITIAL_ADMIN_BOOTSTRAP = "INITIAL_ADMIN_BOOTSTRAP"

    // Phase 3: Admin -> Agent Management
    const val AGENT_CREATED = "AGENT_CREATED"
    const val AGENT_CREATE_FAILED_DUPLICATE = "AGENT_CREATE_FAILED_DUPLICATE"
    const val AGENT_UPDATED = "AGENT_UPDATED"
    const val AGENT_SUSPENDED = "AGENT_SUSPENDED"
    const val AGENT_ACTIVATED = "AGENT_ACTIVATED"
    const val AGENT_PASSWORD_RESET = "AGENT_PASSWORD_RESET"

    // Phase 4: Agent -> User Management
    const val USER_CREATED = "USER_CREATED"
    const val USER_CREATE_FAILED_DUPLICATE = "USER_CREATE_FAILED_DUPLICATE"
    const val USER_UPDATED = "USER_UPDATED"
    const val USER_SUSPENDED = "USER_SUSPENDED"
    const val USER_ACTIVATED = "USER_ACTIVATED"
    const val USER_PASSWORD_RESET = "USER_PASSWORD_RESET"

    // Phase 5: Virtual Coin Ledger & Transactions
    const val COIN_TRANSFER_REQUESTED = "COIN_TRANSFER_REQUESTED"
    const val COIN_TRANSFER_COMPLETED = "COIN_TRANSFER_COMPLETED"
    const val COIN_TRANSFER_FAILED = "COIN_TRANSFER_FAILED"
    const val COIN_DEDUCTION_COMPLETED = "COIN_DEDUCTION_COMPLETED"
    const val COIN_TRANSACTION_REVERSED = "COIN_TRANSACTION_REVERSED"
    const val INSUFFICIENT_BALANCE = "INSUFFICIENT_BALANCE"
    const val UNAUTHORIZED_COIN_OPERATION = "UNAUTHORIZED_COIN_OPERATION"

    // Phase 7: Prediction/Game Engine
    const val GAME_CREATED = "GAME_CREATED"
    const val GAME_UPDATED = "GAME_UPDATED"
    const val GAME_OPENED = "GAME_OPENED"
    const val GAME_CLOSED = "GAME_CLOSED"
    const val GAME_CANCELLED = "GAME_CANCELLED"
    const val ENTRY_CREATED = "ENTRY_CREATED"
    const val ENTRY_REJECTED = "ENTRY_REJECTED"
    const val RESULT_SUBMITTED = "RESULT_SUBMITTED"
    const val RESULT_FINALIZED = "RESULT_FINALIZED"
    const val REWARD_PROCESSED = "REWARD_PROCESSED"
    const val REWARD_REVERSED = "REWARD_REVERSED"
    const val GAME_ACCESS_DENIED = "GAME_ACCESS_DENIED"

    // Phase 8: Admin Game Operations, Monitoring & Reconciliation
    const val RESULT_PROCESSING_RETRY = "RESULT_PROCESSING_RETRY"
    const val RESULT_PROCESSING_FAILED = "RESULT_PROCESSING_FAILED"
    const val RECONCILIATION_PERFORMED = "RECONCILIATION_PERFORMED"
    const val RECONCILIATION_CORRECTION = "RECONCILIATION_CORRECTION"
    const val DATA_EXPORTED = "DATA_EXPORTED"
    const val REPORT_GENERATED = "REPORT_GENERATED"
    const val TRANSACTION_INSPECTED = "TRANSACTION_INSPECTED"

    // Phase 10: Security Hardening & Monitoring Events
    const val TOKEN_REFRESHED = "TOKEN_REFRESHED"
    const val TOKEN_REUSE_DETECTED = "TOKEN_REUSE_DETECTED"
    const val IDOR_ATTEMPT = "IDOR_ATTEMPT"
    const val RATE_LIMIT_EXCEEDED = "RATE_LIMIT_EXCEEDED"
    const val SUSPICIOUS_REQUEST = "SUSPICIOUS_REQUEST"
    const val DEVICE_REVOKED = "DEVICE_REVOKED"
    const val SECURITY_METRICS_INSPECTED = "SECURITY_METRICS_INSPECTED"
}

class AuditService(private val auditLogDao: AuditLogDao) {

    suspend fun logEvent(
        actorId: String,
        actorRole: String,
        action: String,
        targetId: String? = null,
        targetType: String? = null,
        requestId: String = UUID.randomUUID().toString(),
        metadataJson: String = "{}",
        beforeState: String? = null,
        afterState: String? = null
    ) {
        val auditEntity = AuditLogEntity(
            id = UUID.randomUUID().toString(),
            actorId = actorId,
            actorRole = actorRole,
            action = action,
            targetId = targetId,
            targetType = targetType,
            requestId = requestId,
            metadataJson = sanitizeMetadata(metadataJson),
            beforeState = beforeState,
            afterState = afterState,
            createdAt = System.currentTimeMillis()
        )
        auditLogDao.insert(auditEntity)
    }

    suspend fun getRecentLogsForTarget(targetId: String, limit: Int = 10): List<AuditLogEntity> {
        return auditLogDao.getLogsForTarget(targetId, limit)
    }

    suspend fun getAuditLogsPaginated(limit: Int, offset: Int): Pair<List<AuditLogEntity>, Int> {
        val list = auditLogDao.getAllLogsPaginated(limit, offset)
        val total = auditLogDao.countAllLogs()
        return Pair(list, total)
    }

    suspend fun queryAuditLogs(
        actorId: String? = null,
        action: String? = null,
        targetId: String? = null,
        targetType: String? = null,
        startTime: Long = 0L,
        endTime: Long = Long.MAX_VALUE,
        limit: Int = 50,
        offset: Int = 0
    ): Pair<List<AuditLogEntity>, Int> {
        val list = auditLogDao.queryLogs(
            actorId = actorId?.ifBlank { null },
            action = action?.ifBlank { null },
            targetId = targetId?.ifBlank { null },
            targetType = targetType?.ifBlank { null },
            startTime = startTime,
            endTime = endTime,
            limit = limit,
            offset = offset
        )
        val count = auditLogDao.countQueryLogs(
            actorId = actorId?.ifBlank { null },
            action = action?.ifBlank { null },
            targetId = targetId?.ifBlank { null },
            targetType = targetType?.ifBlank { null },
            startTime = startTime,
            endTime = endTime
        )
        return Pair(list, count)
    }

    /**
     * Sanitizer ensuring no passwords or bearer tokens leak into audit persistence.
     */
    private fun sanitizeMetadata(input: String): String {
        return input
            .replace(Regex("\"password\"\\s*:\\s*\"[^\"]*\""), "\"password\":\"[REDACTED]\"")
            .replace(Regex("\"temporaryPassword\"\\s*:\\s*\"[^\"]*\""), "\"temporaryPassword\":\"[REDACTED]\"")
            .replace(Regex("\"newPassword\"\\s*:\\s*\"[^\"]*\""), "\"newPassword\":\"[REDACTED]\"")
            .replace(Regex("\"confirmPassword\"\\s*:\\s*\"[^\"]*\""), "\"confirmPassword\":\"[REDACTED]\"")
            .replace(Regex("\"passwordHash\"\\s*:\\s*\"[^\"]*\""), "\"passwordHash\":\"[REDACTED]\"")
            .replace(Regex("\"token\"\\s*:\\s*\"[^\"]*\""), "\"token\":\"[REDACTED]\"")
            .replace(Regex("\"accessToken\"\\s*:\\s*\"[^\"]*\""), "\"accessToken\":\"[REDACTED]\"")
            .replace(Regex("\"refreshToken\"\\s*:\\s*\"[^\"]*\""), "\"refreshToken\":\"[REDACTED]\"")
    }
}
