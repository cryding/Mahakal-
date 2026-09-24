package com.example.backend.service

import androidx.room.withTransaction
import com.example.backend.audit.AuditActions
import com.example.backend.audit.AuditService
import com.example.backend.database.MahakalServerDatabase
import com.example.backend.database.entity.AccountEntity
import com.example.backend.database.entity.WalletEntity
import com.example.backend.model.CreateUserRequest
import com.example.backend.model.EditUserRequest
import com.example.backend.model.PaginatedUserListResponse
import com.example.backend.model.ResetPasswordResponse
import com.example.backend.model.ResetUserPasswordRequest
import com.example.backend.model.ServerResponse
import com.example.backend.model.UserActivityDto
import com.example.backend.model.UserCreatedDto
import com.example.backend.model.UserDetailsDto
import com.example.backend.model.UserSummaryDto
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

class AgentUserService(
    private val database: MahakalServerDatabase,
    private val auditService: AuditService,
    private val rateLimiter: RateLimiter
) {
    private val accountDao = database.accountDao()
    private val sessionDao = database.sessionDao()

    private val userIdRegex = Regex("^[A-Z0-9_]{3,20}$")

    /**
     * POST /agent/users
     * Strictly creates a User subordinated to the authenticated Agent.
     */
    suspend fun createUser(
        context: SecurityContext,
        request: CreateUserRequest,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<UserCreatedDto> {
        // 1. RBAC Guard: Role + Permission
        try {
            MahakalRbac.requireRole(context, AccountRole.AGENT)
            MahakalRbac.requirePermission(context, Permission.AGENT_CREATE_USER)
        } catch (e: SecurityException) {
            logPermissionDenied(context, "CREATE_USER", requestId)
            return ServerResponse(false, 403, null, "FORBIDDEN", e.message, requestId)
        }

        // Verify that agent account in DB is still ACTIVE
        val agentAccount = accountDao.findById(context.accountId)
        if (agentAccount == null || agentAccount.status != AccountStatus.ACTIVE.name) {
            logPermissionDenied(context, "CREATE_USER_AGENT_SUSPENDED", requestId)
            return ServerResponse(
                false, 403, null, "FORBIDDEN",
                "Agent account is suspended or disabled. User creation is denied.",
                requestId
            )
        }

        // 2. Server-side Rate Limiting
        val rateLimitKey = "agent:create_user:${context.accountId}"
        if (rateLimiter.isLocked(rateLimitKey)) {
            return ServerResponse(
                false, 429, null, "RATE_LIMIT_EXCEEDED",
                "Too many user creation requests. Please wait a few moments.", requestId
            )
        }

        // 3. Normalization & Field Validation
        val normalizedUserId = request.userId.trim().uppercase()
        val displayName = request.displayName.trim()
        val tempPassword = request.temporaryPassword
        val notes = request.notes?.trim()

        if (!userIdRegex.matches(normalizedUserId)) {
            return ServerResponse(
                false, 422, null, "VALIDATION_ERROR",
                "User ID must be 3-20 characters long and contain only uppercase letters, numbers, and underscores.",
                requestId
            )
        }

        if (displayName.length < 2 || displayName.length > 50) {
            return ServerResponse(
                false, 422, null, "VALIDATION_ERROR",
                "Display Name must be between 2 and 50 characters long.",
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
        val existing = accountDao.findByLoginId(normalizedUserId)
        if (existing != null) {
            auditService.logEvent(
                actorId = context.accountId,
                actorRole = context.role.name,
                action = AuditActions.USER_CREATE_FAILED_DUPLICATE,
                targetId = normalizedUserId,
                targetType = "USER",
                requestId = requestId,
                metadataJson = "{\"requestedUserId\":\"$normalizedUserId\"}"
            )
            return ServerResponse(
                false, 409, null, "USER_ID_ALREADY_EXISTS",
                "User ID is already in use.",
                requestId
            )
        }

        // 5. Password Hashing (PBKDF2 HMAC-SHA256 + 16-byte cryptographic salt)
        val salt = PasswordHasher.generateSalt()
        val hash = PasswordHasher.hashPassword(tempPassword, salt)
        val now = System.currentTimeMillis()
        val newUserId = UUID.randomUUID().toString()

        // Critical: user.agentId = authenticatedAgent.id (derived strictly from context)
        val userEntity = AccountEntity(
            id = newUserId,
            loginId = normalizedUserId,
            passwordHash = hash,
            salt = salt,
            role = AccountRole.USER.name,
            status = AccountStatus.ACTIVE.name,
            parentId = context.accountId, // Explicitly bound to authenticated Agent ID
            fullName = displayName,
            createdAt = now,
            updatedAt = now,
            mustChangePassword = true,
            notes = notes
        )

        // 6. Atomic Database Transaction
        try {
            database.withTransaction {
                accountDao.insert(userEntity)
                val wallet = WalletEntity(
                    walletId = UUID.randomUUID().toString(),
                    ownerId = newUserId,
                    ownerRole = AccountRole.USER.name,
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
                    false, 409, null, "USER_ID_ALREADY_EXISTS",
                    "User ID is already in use.",
                    requestId
                )
            }
            return ServerResponse(
                false, 500, null, "DATABASE_ERROR",
                "Failed to persist user account: ${e.localizedMessage}",
                requestId
            )
        }

        // 7. Safe Audit Logging (Zero plain-text password leakage)
        auditService.logEvent(
            actorId = context.accountId,
            actorRole = context.role.name,
            action = AuditActions.USER_CREATED,
            targetId = newUserId,
            targetType = "USER",
            requestId = requestId,
            metadataJson = "{\"userId\":\"$normalizedUserId\",\"displayName\":\"$displayName\",\"agentId\":\"${context.accountId}\",\"status\":\"ACTIVE\"}"
        )

        return ServerResponse(
            success = true,
            statusCode = 201,
            data = UserCreatedDto(
                id = newUserId,
                userId = normalizedUserId,
                displayName = displayName,
                agentId = context.accountId,
                role = AccountRole.USER.name,
                status = AccountStatus.ACTIVE.name,
                createdAt = now,
                notes = notes,
                temporaryPassword = tempPassword
            ),
            message = "User successfully created.",
            requestId = requestId
        )
    }

    /**
     * GET /agent/users or GET /admin/users
     * Returns paginated, status-filtered, searchable list of users belonging strictly to this Agent
     * (or all users if supervisory Admin).
     */
    suspend fun getUsers(
        context: SecurityContext,
        page: Int = 1,
        limit: Int = 20,
        statusFilter: String? = null,
        searchQuery: String? = null,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<PaginatedUserListResponse> {
        val isAdmin = context.role == AccountRole.ADMIN
        val isAgent = context.role == AccountRole.AGENT

        if (!isAdmin && !isAgent) {
            logPermissionDenied(context, "GET_USERS", requestId)
            return ServerResponse(false, 403, null, "FORBIDDEN", "Unauthorized role for user listing.", requestId)
        }

        if (isAgent) {
            try {
                MahakalRbac.requirePermission(context, Permission.AGENT_VIEW_OWN_USERS)
            } catch (e: SecurityException) {
                logPermissionDenied(context, "GET_USERS", requestId)
                return ServerResponse(false, 403, null, "FORBIDDEN", e.message, requestId)
            }
        } else {
            try {
                MahakalRbac.requirePermission(context, Permission.ADMIN_MANAGE_USERS)
            } catch (e: SecurityException) {
                logPermissionDenied(context, "ADMIN_GET_USERS", requestId)
                return ServerResponse(false, 403, null, "FORBIDDEN", e.message, requestId)
            }
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

        val (rawUsers, totalCount) = if (isAgent) {
            val count = accountDao.countUsers(
                agentId = context.accountId,
                statusFilter = normalizedStatus,
                searchQuery = normalizedSearch
            )
            val users = accountDao.findUsersPaged(
                agentId = context.accountId,
                statusFilter = normalizedStatus,
                searchQuery = normalizedSearch,
                limit = safeLimit,
                offset = offset
            )
            Pair(users, count)
        } else {
            val count = accountDao.countAllUsers(
                statusFilter = normalizedStatus,
                searchQuery = normalizedSearch
            )
            val users = accountDao.findAllUsersPaged(
                statusFilter = normalizedStatus,
                searchQuery = normalizedSearch,
                limit = safeLimit,
                offset = offset
            )
            Pair(users, count)
        }

        val userSummaries = rawUsers.map { user ->
            UserSummaryDto(
                id = user.id,
                userId = user.loginId,
                displayName = user.fullName,
                agentId = user.parentId ?: "",
                status = user.status,
                createdAt = user.createdAt,
                lastLoginAt = user.lastLoginAt,
                notes = user.notes
            )
        }

        val totalPages = if (totalCount == 0) 1 else Math.ceil(totalCount.toDouble() / safeLimit).toInt()

        return ServerResponse(
            success = true,
            statusCode = 200,
            data = PaginatedUserListResponse(
                users = userSummaries,
                totalCount = totalCount,
                page = safePage,
                limit = safeLimit,
                totalPages = totalPages
            ),
            requestId = requestId
        )
    }

    /**
     * GET /agent/users/{id}
     * Retrieves detailed User profile and recent audit history. Enforces strict ownership.
     */
    suspend fun getUserDetails(
        context: SecurityContext,
        targetUserId: String,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<UserDetailsDto> {
        val isAdmin = context.role == AccountRole.ADMIN
        val isAgent = context.role == AccountRole.AGENT

        if (!isAdmin && !isAgent) {
            logPermissionDenied(context, "GET_USER_DETAILS", requestId)
            return ServerResponse(false, 403, null, "FORBIDDEN", "Unauthorized role.", requestId)
        }

        val user = accountDao.findById(targetUserId) ?: accountDao.findByLoginId(targetUserId)
        if (user == null || user.role != AccountRole.USER.name) {
            return ServerResponse(false, 404, null, "NOT_FOUND", "User not found.", requestId)
        }

        // Cross-Agent Ownership Enforcement:
        if (isAgent && user.parentId != context.accountId) {
            logPermissionDenied(context, "USER_OWNERSHIP_MISMATCH_GET", requestId)
            return ServerResponse(
                false, 403, null, "FORBIDDEN",
                "Access denied: You do not have permission to access a user owned by another agent.",
                requestId
            )
        }

        val parentAgent = user.parentId?.let { accountDao.findById(it) }
        val recentAuditLogs = auditService.getRecentLogsForTarget(user.id, 10).map { log ->
            UserActivityDto(
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
            data = UserDetailsDto(
                id = user.id,
                userId = user.loginId,
                displayName = user.fullName,
                agentId = user.parentId ?: "",
                agentName = parentAgent?.fullName,
                role = user.role,
                status = user.status,
                createdAt = user.createdAt,
                updatedAt = user.updatedAt,
                lastLoginAt = user.lastLoginAt,
                mustChangePassword = user.mustChangePassword,
                virtualCoinBalance = 0L,
                notes = user.notes,
                recentActivity = recentAuditLogs
            ),
            requestId = requestId
        )
    }

    /**
     * PATCH /agent/users/{id}
     * Edits permitted profile fields (displayName, notes).
     */
    suspend fun updateUser(
        context: SecurityContext,
        targetUserId: String,
        request: EditUserRequest,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<UserSummaryDto> {
        val isAdmin = context.role == AccountRole.ADMIN
        val isAgent = context.role == AccountRole.AGENT

        if (!isAdmin && !isAgent) {
            logPermissionDenied(context, "UPDATE_USER", requestId)
            return ServerResponse(false, 403, null, "FORBIDDEN", "Unauthorized role.", requestId)
        }

        val user = accountDao.findById(targetUserId) ?: accountDao.findByLoginId(targetUserId)
        if (user == null || user.role != AccountRole.USER.name) {
            return ServerResponse(false, 404, null, "NOT_FOUND", "User not found.", requestId)
        }

        if (isAgent && user.parentId != context.accountId) {
            logPermissionDenied(context, "USER_OWNERSHIP_MISMATCH_UPDATE", requestId)
            return ServerResponse(
                false, 403, null, "FORBIDDEN",
                "Access denied: You do not have permission to modify a user owned by another agent.",
                requestId
            )
        }

        val newDisplayName = request.displayName?.trim() ?: user.fullName
        val newNotes = if (request.notes != null) request.notes.trim() else user.notes

        if (newDisplayName.length < 2 || newDisplayName.length > 50) {
            return ServerResponse(
                false, 422, null, "VALIDATION_ERROR",
                "Display Name must be between 2 and 50 characters long.",
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
        accountDao.updateUserProfile(user.id, newDisplayName, newNotes, now)

        auditService.logEvent(
            actorId = context.accountId,
            actorRole = context.role.name,
            action = AuditActions.USER_UPDATED,
            targetId = user.id,
            targetType = "USER",
            requestId = requestId,
            beforeState = "{\"displayName\":\"${user.fullName}\"}",
            afterState = "{\"displayName\":\"$newDisplayName\"}"
        )

        return ServerResponse(
            success = true,
            statusCode = 200,
            data = UserSummaryDto(
                id = user.id,
                userId = user.loginId,
                displayName = newDisplayName,
                agentId = user.parentId ?: "",
                status = user.status,
                createdAt = user.createdAt,
                lastLoginAt = user.lastLoginAt,
                notes = newNotes
            ),
            message = "User profile updated.",
            requestId = requestId
        )
    }

    /**
     * POST /agent/users/{id}/suspend
     * Suspends user and revokes all active sessions immediately.
     */
    suspend fun suspendUser(
        context: SecurityContext,
        targetUserId: String,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<Unit> {
        val isAdmin = context.role == AccountRole.ADMIN
        val isAgent = context.role == AccountRole.AGENT

        if (!isAdmin && !isAgent) {
            logPermissionDenied(context, "SUSPEND_USER", requestId)
            return ServerResponse(false, 403, null, "FORBIDDEN", "Unauthorized role.", requestId)
        }

        val rateLimitKey = "user_mgmt:suspend:${context.accountId}"
        if (rateLimiter.isLocked(rateLimitKey)) {
            return ServerResponse(false, 429, null, "RATE_LIMIT_EXCEEDED", "Rate limit exceeded.", requestId)
        }

        val user = accountDao.findById(targetUserId) ?: accountDao.findByLoginId(targetUserId)
        if (user == null || user.role != AccountRole.USER.name) {
            return ServerResponse(false, 404, null, "NOT_FOUND", "User not found.", requestId)
        }

        if (isAgent && user.parentId != context.accountId) {
            logPermissionDenied(context, "USER_OWNERSHIP_MISMATCH_SUSPEND", requestId)
            return ServerResponse(
                false, 403, null, "FORBIDDEN",
                "Access denied: You cannot suspend a user owned by another agent.",
                requestId
            )
        }

        val now = System.currentTimeMillis()
        database.withTransaction {
            accountDao.updateStatus(user.id, AccountStatus.SUSPENDED.name, now)
            sessionDao.revokeAllForAccount(user.id)
        }

        auditService.logEvent(
            actorId = context.accountId,
            actorRole = context.role.name,
            action = AuditActions.USER_SUSPENDED,
            targetId = user.id,
            targetType = "USER",
            requestId = requestId,
            metadataJson = "{\"userId\":\"${user.loginId}\"}"
        )

        return ServerResponse(true, 200, Unit, message = "User suspended and sessions revoked.", requestId = requestId)
    }

    /**
     * POST /agent/users/{id}/activate
     * Restores active status for the user.
     */
    suspend fun activateUser(
        context: SecurityContext,
        targetUserId: String,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<Unit> {
        val isAdmin = context.role == AccountRole.ADMIN
        val isAgent = context.role == AccountRole.AGENT

        if (!isAdmin && !isAgent) {
            logPermissionDenied(context, "ACTIVATE_USER", requestId)
            return ServerResponse(false, 403, null, "FORBIDDEN", "Unauthorized role.", requestId)
        }

        val rateLimitKey = "user_mgmt:activate:${context.accountId}"
        if (rateLimiter.isLocked(rateLimitKey)) {
            return ServerResponse(false, 429, null, "RATE_LIMIT_EXCEEDED", "Rate limit exceeded.", requestId)
        }

        val user = accountDao.findById(targetUserId) ?: accountDao.findByLoginId(targetUserId)
        if (user == null || user.role != AccountRole.USER.name) {
            return ServerResponse(false, 404, null, "NOT_FOUND", "User not found.", requestId)
        }

        if (isAgent && user.parentId != context.accountId) {
            logPermissionDenied(context, "USER_OWNERSHIP_MISMATCH_ACTIVATE", requestId)
            return ServerResponse(
                false, 403, null, "FORBIDDEN",
                "Access denied: You cannot activate a user owned by another agent.",
                requestId
            )
        }

        val now = System.currentTimeMillis()
        accountDao.updateStatus(user.id, AccountStatus.ACTIVE.name, now)

        auditService.logEvent(
            actorId = context.accountId,
            actorRole = context.role.name,
            action = AuditActions.USER_ACTIVATED,
            targetId = user.id,
            targetType = "USER",
            requestId = requestId,
            metadataJson = "{\"userId\":\"${user.loginId}\"}"
        )

        return ServerResponse(true, 200, Unit, message = "User status restored to ACTIVE.", requestId = requestId)
    }

    /**
     * POST /agent/users/{id}/reset-password
     * Resets user password to a temporary password and revokes all active sessions.
     */
    suspend fun resetPassword(
        context: SecurityContext,
        targetUserId: String,
        request: ResetUserPasswordRequest,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<ResetPasswordResponse> {
        val isAdmin = context.role == AccountRole.ADMIN
        val isAgent = context.role == AccountRole.AGENT

        if (!isAdmin && !isAgent) {
            logPermissionDenied(context, "RESET_USER_PASSWORD", requestId)
            return ServerResponse(false, 403, null, "FORBIDDEN", "Unauthorized role.", requestId)
        }

        val rateLimitKey = "user_mgmt:reset_pwd:${context.accountId}"
        if (rateLimiter.isLocked(rateLimitKey)) {
            return ServerResponse(false, 429, null, "RATE_LIMIT_EXCEEDED", "Rate limit exceeded.", requestId)
        }

        val user = accountDao.findById(targetUserId) ?: accountDao.findByLoginId(targetUserId)
        if (user == null || user.role != AccountRole.USER.name) {
            return ServerResponse(false, 404, null, "NOT_FOUND", "User not found.", requestId)
        }

        if (isAgent && user.parentId != context.accountId) {
            logPermissionDenied(context, "USER_OWNERSHIP_MISMATCH_RESET", requestId)
            return ServerResponse(
                false, 403, null, "FORBIDDEN",
                "Access denied: You cannot reset password for a user owned by another agent.",
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
            accountDao.resetPassword(user.id, hash, salt, now)
            sessionDao.revokeAllForAccount(user.id)
        }

        auditService.logEvent(
            actorId = context.accountId,
            actorRole = context.role.name,
            action = AuditActions.USER_PASSWORD_RESET,
            targetId = user.id,
            targetType = "USER",
            requestId = requestId,
            metadataJson = "{\"userId\":\"${user.loginId}\"}"
        )

        return ServerResponse(
            success = true,
            statusCode = 200,
            data = ResetPasswordResponse(
                temporaryPassword = tempPassword,
                message = "Temporary password generated. The user must change this password upon first login."
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
