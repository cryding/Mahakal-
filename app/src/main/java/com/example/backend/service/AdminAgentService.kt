package com.example.backend.service

import androidx.room.withTransaction
import com.example.backend.audit.AuditActions
import com.example.backend.audit.AuditService
import com.example.backend.database.MahakalServerDatabase
import com.example.backend.database.entity.AccountEntity
import com.example.backend.database.entity.WalletEntity
import com.example.backend.model.AgentActivityDto
import com.example.backend.model.AgentCreatedDto
import com.example.backend.model.AgentDetailsDto
import com.example.backend.model.AgentSummaryDto
import com.example.backend.model.CreateAgentRequest
import com.example.backend.model.EditAgentRequest
import com.example.backend.model.PaginatedAgentListResponse
import com.example.backend.model.ResetAgentPasswordRequest
import com.example.backend.model.ResetPasswordResponse
import com.example.backend.model.ServerResponse
import com.example.backend.rbac.AccountRole
import com.example.backend.rbac.AccountStatus
import com.example.backend.rbac.MahakalRbac
import com.example.backend.rbac.Permission
import com.example.backend.rbac.SecurityContext
import com.example.backend.security.PasswordHasher
import com.example.backend.security.RateLimiter
import java.security.SecureRandom
import java.util.UUID
import kotlin.random.asKotlinRandom

class AdminAgentService(
    private val database: MahakalServerDatabase,
    private val auditService: AuditService,
    private val rateLimiter: RateLimiter
) {
    private val accountDao = database.accountDao()
    private val sessionDao = database.sessionDao()

    private val agentIdRegex = Regex("^[A-Z0-9_]{3,20}$")

    /**
     * POST /admin/agents
     * Strictly creates an Agent subordinated to the authenticated Admin.
     */
    suspend fun createAgent(
        context: SecurityContext,
        request: CreateAgentRequest,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<AgentCreatedDto> {
        // 1. RBAC Guard: Role + Permission
        try {
            MahakalRbac.requireRole(context, AccountRole.ADMIN)
            MahakalRbac.requirePermission(context, Permission.ADMIN_MANAGE_AGENTS)
        } catch (e: SecurityException) {
            logPermissionDenied(context, "CREATE_AGENT", requestId)
            return ServerResponse(false, 403, null, "FORBIDDEN", e.message, requestId)
        }

        // 2. Server-side Rate Limiting
        val rateLimitKey = "admin:create_agent:${context.accountId}"
        if (rateLimiter.isLocked(rateLimitKey)) {
            return ServerResponse(
                false, 429, null, "RATE_LIMIT_EXCEEDED",
                "Too many agent creation requests. Please wait a few moments.", requestId
            )
        }

        // 3. Normalization & Field Validation
        val normalizedAgentId = request.agentId.trim().uppercase()
        val agentName = request.agentName.trim()
        val tempPassword = request.temporaryPassword
        val notes = request.notes?.trim()

        if (!agentIdRegex.matches(normalizedAgentId)) {
            return ServerResponse(
                false, 422, null, "VALIDATION_ERROR",
                "Agent ID must be 3-20 characters long and contain only uppercase letters, numbers, and underscores.",
                requestId
            )
        }

        if (agentName.length < 2 || agentName.length > 50) {
            return ServerResponse(
                false, 422, null, "VALIDATION_ERROR",
                "Agent Name must be between 2 and 50 characters long.",
                requestId
            )
        }

        if (tempPassword.length < 8 || !tempPassword.any { it.isDigit() } || !tempPassword.any { it.isLetter() }) {
            return ServerResponse(
                false, 422, null, "VALIDATION_ERROR",
                "Temporary password must be at least 8 characters long and contain both letters and digits.",
                requestId
            )
        }

        if (notes != null && notes.length > 500) {
            return ServerResponse(
                false, 422, null, "VALIDATION_ERROR",
                "Notes cannot exceed 500 characters.",
                requestId
            )
        }

        // 4. Duplicate Check
        val existing = accountDao.findByLoginId(normalizedAgentId)
        if (existing != null) {
            auditService.logEvent(
                actorId = context.accountId,
                actorRole = context.role.name,
                action = AuditActions.AGENT_CREATE_FAILED_DUPLICATE,
                targetId = normalizedAgentId,
                targetType = "AGENT",
                requestId = requestId,
                metadataJson = "{\"requestedAgentId\":\"$normalizedAgentId\"}"
            )
            return ServerResponse(
                false, 409, null, "AGENT_ID_ALREADY_EXISTS",
                "Agent ID '$normalizedAgentId' already exists.",
                requestId
            )
        }

        // 5. Password Hashing (PBKDF2 HMAC-SHA256 + 16-byte cryptographic salt)
        val salt = PasswordHasher.generateSalt()
        val hash = PasswordHasher.hashPassword(tempPassword, salt)
        val now = System.currentTimeMillis()
        val newAgentId = UUID.randomUUID().toString()

        val agentEntity = AccountEntity(
            id = newAgentId,
            loginId = normalizedAgentId,
            passwordHash = hash,
            salt = salt,
            role = AccountRole.AGENT.name,
            status = AccountStatus.ACTIVE.name,
            parentId = context.accountId, // Explicitly bound to authenticated Admin ID
            fullName = agentName,
            createdAt = now,
            updatedAt = now,
            mustChangePassword = true,
            notes = notes
        )

        // 6. Atomic Database Transaction
        try {
            database.withTransaction {
                accountDao.insert(agentEntity)
                val wallet = WalletEntity(
                    walletId = UUID.randomUUID().toString(),
                    ownerId = newAgentId,
                    ownerRole = AccountRole.AGENT.name,
                    balance = 0L,
                    currencyType = "VIRTUAL_COIN",
                    version = 0L,
                    createdAt = now,
                    updatedAt = now
                )
                database.walletDao().insert(wallet)
            }
        } catch (e: Exception) {
            if (e.message?.contains("constraint", ignoreCase = true) == true ||
                e.message?.contains("unique", ignoreCase = true) == true) {
                return ServerResponse(
                    false, 409, null, "AGENT_ID_ALREADY_EXISTS",
                    "An account with ID '$normalizedAgentId' already exists.",
                    requestId
                )
            }
            return ServerResponse(
                false, 500, null, "DATABASE_ERROR",
                "Failed to persist agent account: ${e.localizedMessage}",
                requestId
            )
        }

        // 7. Audit Logging (Zero plain-text password leakage)
        auditService.logEvent(
            actorId = context.accountId,
            actorRole = context.role.name,
            action = AuditActions.AGENT_CREATED,
            targetId = newAgentId,
            targetType = "AGENT",
            requestId = requestId,
            metadataJson = "{\"agentId\":\"$normalizedAgentId\",\"agentName\":\"$agentName\"}"
        )

        return ServerResponse(
            success = true,
            statusCode = 201,
            data = AgentCreatedDto(
                id = newAgentId,
                agentId = normalizedAgentId,
                agentName = agentName,
                role = AccountRole.AGENT.name,
                status = AccountStatus.ACTIVE.name,
                createdAt = now,
                notes = notes
            ),
            message = "Agent successfully created.",
            requestId = requestId
        )
    }

    /**
     * GET /admin/agents
     * Returns paginated, status-filtered, searchable list of agents belonging strictly to this Admin.
     */
    suspend fun getAgents(
        context: SecurityContext,
        page: Int = 1,
        limit: Int = 20,
        statusFilter: String? = null,
        searchQuery: String? = null,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<PaginatedAgentListResponse> {
        try {
            MahakalRbac.requireRole(context, AccountRole.ADMIN)
            MahakalRbac.requirePermission(context, Permission.ADMIN_MANAGE_AGENTS)
        } catch (e: SecurityException) {
            logPermissionDenied(context, "GET_AGENTS", requestId)
            return ServerResponse(false, 403, null, "FORBIDDEN", e.message, requestId)
        }

        val safePage = if (page < 1) 1 else page
        val safeLimit = when {
            limit < 1 -> 20
            limit > 100 -> 100
            else -> limit
        }
        val offset = (safePage - 1) * safeLimit

        val normalizedStatus = if (!statusFilter.isNullOrBlank() && statusFilter != "ALL") {
            statusFilter.uppercase().trim()
        } else null

        val normalizedSearch = if (!searchQuery.isNullOrBlank()) {
            searchQuery.trim()
        } else null

        val totalCount = accountDao.countAgents(
            adminId = context.accountId,
            statusFilter = normalizedStatus,
            searchQuery = normalizedSearch
        )

        val rawAgents = accountDao.findAgentsPaged(
            adminId = context.accountId,
            statusFilter = normalizedStatus,
            searchQuery = normalizedSearch,
            limit = safeLimit,
            offset = offset
        )

        val agentSummaries = rawAgents.map { agent ->
            val userCount = accountDao.countSubordinatedUsers(agent.id)
            AgentSummaryDto(
                id = agent.id,
                agentId = agent.loginId,
                agentName = agent.fullName,
                status = agent.status,
                userCount = userCount,
                createdAt = agent.createdAt,
                lastLoginAt = agent.lastLoginAt,
                notes = agent.notes
            )
        }

        val totalPages = if (totalCount == 0) 1 else Math.ceil(totalCount.toDouble() / safeLimit).toInt()

        return ServerResponse(
            success = true,
            statusCode = 200,
            data = PaginatedAgentListResponse(
                agents = agentSummaries,
                totalCount = totalCount,
                page = safePage,
                limit = safeLimit,
                totalPages = totalPages
            ),
            requestId = requestId
        )
    }

    /**
     * GET /admin/agents/{id}
     * Retrieves detailed Agent profile and recent audit history. Enforces strict ownership.
     */
    suspend fun getAgentDetails(
        context: SecurityContext,
        targetAgentId: String,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<AgentDetailsDto> {
        try {
            MahakalRbac.requireRole(context, AccountRole.ADMIN)
            MahakalRbac.requirePermission(context, Permission.ADMIN_MANAGE_AGENTS)
        } catch (e: SecurityException) {
            logPermissionDenied(context, "GET_AGENT_DETAILS", requestId)
            return ServerResponse(false, 403, null, "FORBIDDEN", e.message, requestId)
        }

        val agent = accountDao.findById(targetAgentId)
        if (agent == null || agent.role != AccountRole.AGENT.name) {
            return ServerResponse(false, 404, null, "NOT_FOUND", "Agent not found.", requestId)
        }

        // Enforce Ownership: Agent must be owned by the authenticated Admin
        if (agent.parentId != context.accountId) {
            logPermissionDenied(context, "AGENT_OWNERSHIP_MISMATCH_GET", requestId)
            return ServerResponse(
                false, 403, null, "FORBIDDEN",
                "Access denied: You do not have permission to access an agent owned by another administrator.",
                requestId
            )
        }

        val userCount = accountDao.countSubordinatedUsers(agent.id)
        val recentAuditLogs = auditService.getRecentLogsForTarget(agent.id, 10).map { log ->
            AgentActivityDto(
                id = log.id,
                action = log.action,
                actorRole = log.actorRole,
                createdAt = log.createdAt,
                metadata = log.metadataJson
            )
        }

        return ServerResponse(
            success = true,
            statusCode = 200,
            data = AgentDetailsDto(
                id = agent.id,
                agentId = agent.loginId,
                agentName = agent.fullName,
                status = agent.status,
                createdAt = agent.createdAt,
                lastLoginAt = agent.lastLoginAt,
                userCount = userCount,
                virtualCoinBalance = 0L,
                notes = agent.notes,
                recentActivity = recentAuditLogs
            ),
            requestId = requestId
        )
    }

    /**
     * PATCH /admin/agents/{id}
     * Edits permitted profile fields (agentName, notes).
     */
    suspend fun updateAgent(
        context: SecurityContext,
        targetAgentId: String,
        request: EditAgentRequest,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<AgentSummaryDto> {
        try {
            MahakalRbac.requireRole(context, AccountRole.ADMIN)
            MahakalRbac.requirePermission(context, Permission.ADMIN_MANAGE_AGENTS)
        } catch (e: SecurityException) {
            logPermissionDenied(context, "UPDATE_AGENT", requestId)
            return ServerResponse(false, 403, null, "FORBIDDEN", e.message, requestId)
        }

        val agent = accountDao.findById(targetAgentId)
        if (agent == null || agent.role != AccountRole.AGENT.name) {
            return ServerResponse(false, 404, null, "NOT_FOUND", "Agent not found.", requestId)
        }

        if (agent.parentId != context.accountId) {
            logPermissionDenied(context, "AGENT_OWNERSHIP_MISMATCH_UPDATE", requestId)
            return ServerResponse(
                false, 403, null, "FORBIDDEN",
                "Access denied: You do not have permission to modify an agent owned by another administrator.",
                requestId
            )
        }

        val newName = request.agentName?.trim() ?: agent.fullName
        val newNotes = if (request.notes != null) request.notes.trim() else agent.notes

        if (newName.length < 2 || newName.length > 50) {
            return ServerResponse(
                false, 422, null, "VALIDATION_ERROR",
                "Agent Name must be between 2 and 50 characters long.",
                requestId
            )
        }

        if (newNotes != null && newNotes.length > 500) {
            return ServerResponse(
                false, 422, null, "VALIDATION_ERROR",
                "Notes cannot exceed 500 characters.",
                requestId
            )
        }

        val now = System.currentTimeMillis()
        accountDao.updateAgentProfile(agent.id, newName, newNotes, now)

        auditService.logEvent(
            actorId = context.accountId,
            actorRole = context.role.name,
            action = AuditActions.AGENT_UPDATED,
            targetId = agent.id,
            targetType = "AGENT",
            requestId = requestId,
            beforeState = "{\"name\":\"${agent.fullName}\"}",
            afterState = "{\"name\":\"$newName\"}"
        )

        val userCount = accountDao.countSubordinatedUsers(agent.id)
        return ServerResponse(
            success = true,
            statusCode = 200,
            data = AgentSummaryDto(
                id = agent.id,
                agentId = agent.loginId,
                agentName = newName,
                status = agent.status,
                userCount = userCount,
                createdAt = agent.createdAt,
                lastLoginAt = agent.lastLoginAt,
                notes = newNotes
            ),
            message = "Agent profile updated.",
            requestId = requestId
        )
    }

    /**
     * POST /admin/agents/{id}/suspend
     * Suspends agent and revokes all active sessions immediately.
     */
    suspend fun suspendAgent(
        context: SecurityContext,
        targetAgentId: String,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<Unit> {
        try {
            MahakalRbac.requireRole(context, AccountRole.ADMIN)
            MahakalRbac.requirePermission(context, Permission.ADMIN_MANAGE_AGENTS)
        } catch (e: SecurityException) {
            logPermissionDenied(context, "SUSPEND_AGENT", requestId)
            return ServerResponse(false, 403, null, "FORBIDDEN", e.message, requestId)
        }

        val rateLimitKey = "admin:suspend:${context.accountId}"
        if (rateLimiter.isLocked(rateLimitKey)) {
            return ServerResponse(false, 429, null, "RATE_LIMIT_EXCEEDED", "Rate limit exceeded.", requestId)
        }

        val agent = accountDao.findById(targetAgentId)
        if (agent == null || agent.role != AccountRole.AGENT.name) {
            return ServerResponse(false, 404, null, "NOT_FOUND", "Agent not found.", requestId)
        }

        if (agent.parentId != context.accountId) {
            logPermissionDenied(context, "AGENT_OWNERSHIP_MISMATCH_SUSPEND", requestId)
            return ServerResponse(
                false, 403, null, "FORBIDDEN",
                "Access denied: You cannot suspend an agent owned by another administrator.",
                requestId
            )
        }

        val now = System.currentTimeMillis()
        database.withTransaction {
            accountDao.updateStatus(agent.id, AccountStatus.SUSPENDED.name, now)
            sessionDao.revokeAllForAccount(agent.id)
        }

        auditService.logEvent(
            actorId = context.accountId,
            actorRole = context.role.name,
            action = AuditActions.AGENT_SUSPENDED,
            targetId = agent.id,
            targetType = "AGENT",
            requestId = requestId
        )

        return ServerResponse(true, 200, Unit, message = "Agent suspended and sessions revoked.", requestId = requestId)
    }

    /**
     * POST /admin/agents/{id}/activate
     * Restores active status for the agent.
     */
    suspend fun activateAgent(
        context: SecurityContext,
        targetAgentId: String,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<Unit> {
        try {
            MahakalRbac.requireRole(context, AccountRole.ADMIN)
            MahakalRbac.requirePermission(context, Permission.ADMIN_MANAGE_AGENTS)
        } catch (e: SecurityException) {
            logPermissionDenied(context, "ACTIVATE_AGENT", requestId)
            return ServerResponse(false, 403, null, "FORBIDDEN", e.message, requestId)
        }

        val rateLimitKey = "admin:activate:${context.accountId}"
        if (rateLimiter.isLocked(rateLimitKey)) {
            return ServerResponse(false, 429, null, "RATE_LIMIT_EXCEEDED", "Rate limit exceeded.", requestId)
        }

        val agent = accountDao.findById(targetAgentId)
        if (agent == null || agent.role != AccountRole.AGENT.name) {
            return ServerResponse(false, 404, null, "NOT_FOUND", "Agent not found.", requestId)
        }

        if (agent.parentId != context.accountId) {
            logPermissionDenied(context, "AGENT_OWNERSHIP_MISMATCH_ACTIVATE", requestId)
            return ServerResponse(
                false, 403, null, "FORBIDDEN",
                "Access denied: You cannot activate an agent owned by another administrator.",
                requestId
            )
        }

        val now = System.currentTimeMillis()
        accountDao.updateStatus(agent.id, AccountStatus.ACTIVE.name, now)

        auditService.logEvent(
            actorId = context.accountId,
            actorRole = context.role.name,
            action = AuditActions.AGENT_ACTIVATED,
            targetId = agent.id,
            targetType = "AGENT",
            requestId = requestId
        )

        return ServerResponse(true, 200, Unit, message = "Agent status restored to ACTIVE.", requestId = requestId)
    }

    /**
     * POST /admin/agents/{id}/reset-password
     * Resets agent password to a temporary password and revokes all active sessions.
     */
    suspend fun resetPassword(
        context: SecurityContext,
        targetAgentId: String,
        request: ResetAgentPasswordRequest,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<ResetPasswordResponse> {
        try {
            MahakalRbac.requireRole(context, AccountRole.ADMIN)
            MahakalRbac.requirePermission(context, Permission.ADMIN_MANAGE_AGENTS)
        } catch (e: SecurityException) {
            logPermissionDenied(context, "RESET_AGENT_PASSWORD", requestId)
            return ServerResponse(false, 403, null, "FORBIDDEN", e.message, requestId)
        }

        val rateLimitKey = "admin:reset_pwd:${context.accountId}"
        if (rateLimiter.isLocked(rateLimitKey)) {
            return ServerResponse(false, 429, null, "RATE_LIMIT_EXCEEDED", "Rate limit exceeded.", requestId)
        }

        val agent = accountDao.findById(targetAgentId)
        if (agent == null || agent.role != AccountRole.AGENT.name) {
            return ServerResponse(false, 404, null, "NOT_FOUND", "Agent not found.", requestId)
        }

        if (agent.parentId != context.accountId) {
            logPermissionDenied(context, "AGENT_OWNERSHIP_MISMATCH_RESET", requestId)
            return ServerResponse(
                false, 403, null, "FORBIDDEN",
                "Access denied: You cannot reset password for an agent owned by another administrator.",
                requestId
            )
        }

        val tempPassword = if (!request.temporaryPassword.isNullOrBlank()) {
            request.temporaryPassword.trim()
        } else {
            generateSecureTemporaryPassword()
        }

        if (tempPassword.length < 8 || !tempPassword.any { it.isDigit() } || !tempPassword.any { it.isLetter() }) {
            return ServerResponse(
                false, 422, null, "VALIDATION_ERROR",
                "Temporary password must be at least 8 characters long and contain both letters and digits.",
                requestId
            )
        }

        val salt = PasswordHasher.generateSalt()
        val hash = PasswordHasher.hashPassword(tempPassword, salt)
        val now = System.currentTimeMillis()

        database.withTransaction {
            accountDao.resetPassword(agent.id, hash, salt, now)
            sessionDao.revokeAllForAccount(agent.id)
        }

        auditService.logEvent(
            actorId = context.accountId,
            actorRole = context.role.name,
            action = AuditActions.AGENT_PASSWORD_RESET,
            targetId = agent.id,
            targetType = "AGENT",
            requestId = requestId,
            metadataJson = "{\"agentId\":\"${agent.loginId}\"}"
        )

        return ServerResponse(
            success = true,
            statusCode = 200,
            data = ResetPasswordResponse(
                temporaryPassword = tempPassword,
                message = "Temporary password generated. The agent must change this password upon first login."
            ),
            message = "Password successfully reset.",
            requestId = requestId
        )
    }

    private fun generateSecureTemporaryPassword(): String {
        val upper = "ABCDEFGHJKLMNPQRSTUVWXYZ"
        val lower = "abcdefghijkmnopqrstuvwxyz"
        val digits = "23456789"
        val special = "!@#$%"
        val all = upper + lower + digits + special
        val random = SecureRandom()
        val passwordChars = charArrayOf(
            upper[random.nextInt(upper.length)],
            lower[random.nextInt(lower.length)],
            digits[random.nextInt(digits.length)],
            special[random.nextInt(special.length)],
            all[random.nextInt(all.length)],
            all[random.nextInt(all.length)],
            all[random.nextInt(all.length)],
            all[random.nextInt(all.length)],
            all[random.nextInt(all.length)],
            all[random.nextInt(all.length)]
        )
        passwordChars.shuffle(random.asKotlinRandom())
        return String(passwordChars)
    }

    private suspend fun logPermissionDenied(context: SecurityContext, resource: String, requestId: String) {
        auditService.logEvent(
            actorId = context.accountId,
            actorRole = context.role.name,
            action = AuditActions.PERMISSION_DENIED,
            targetId = resource,
            requestId = requestId,
            metadataJson = "{\"deniedAction\":\"$resource\"}"
        )
    }
}
