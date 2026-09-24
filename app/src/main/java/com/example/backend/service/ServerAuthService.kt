package com.example.backend.service

import com.example.backend.audit.AuditActions
import com.example.backend.audit.AuditService
import com.example.backend.database.MahakalServerDatabase
import com.example.backend.database.entity.AccountEntity
import com.example.backend.database.entity.SessionEntity
import com.example.backend.model.ChangePasswordRequest
import com.example.backend.model.LoginRequest
import com.example.backend.model.LoginResponse
import com.example.backend.model.ServerResponse
import com.example.backend.model.UserProfileDto
import com.example.backend.rbac.AccountRole
import com.example.backend.rbac.AccountStatus
import com.example.backend.rbac.MahakalRbac
import com.example.backend.rbac.SecurityContext
import com.example.backend.security.PasswordHasher
import com.example.backend.security.RateLimiter
import com.example.backend.security.TokenProvider
import java.util.UUID

class ServerAuthService(
    private val database: MahakalServerDatabase,
    private val auditService: AuditService,
    private val rateLimiter: RateLimiter,
    private val notificationService: NotificationService? = null
) {

    private val accountDao = database.accountDao()
    private val sessionDao = database.sessionDao()

    /**
     * Authenticates credentials and issues a secure session.
     */
    suspend fun login(
        request: LoginRequest,
        clientIp: String? = null,
        userAgent: String? = null,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<LoginResponse> {
        val loginId = request.loginId.trim()
        val password = request.password

        // 1. Validation
        if (loginId.isEmpty() || password.isEmpty()) {
            return ServerResponse(
                success = false,
                statusCode = 422,
                errorCode = "VALIDATION_ERROR",
                message = "Login ID and Password are required.",
                requestId = requestId
            )
        }

        // 2. Rate Limiting Protection (Brute Force / Credential Stuffing)
        val rateLimitKey = "login:${loginId.lowercase()}"
        if (rateLimiter.isLocked(rateLimitKey)) {
            auditService.logEvent(
                actorId = "anonymous",
                actorRole = "ANONYMOUS",
                action = AuditActions.ACCOUNT_LOCKED,
                targetId = loginId,
                requestId = requestId,
                metadataJson = "{\"reason\":\"Excessive failed attempts triggered rate-limiting\"}"
            )
            return ServerResponse(
                success = false,
                statusCode = 429,
                errorCode = "RATE_LIMIT_EXCEEDED",
                message = "Too many failed attempts. Temporary lockout in effect. Please try again later.",
                requestId = requestId
            )
        }

        // 3. Lookup Account
        val account = accountDao.findByLoginId(loginId)
        if (account == null) {
            rateLimiter.recordFailure(rateLimitKey)
            auditService.logEvent(
                actorId = "anonymous",
                actorRole = "ANONYMOUS",
                action = AuditActions.LOGIN_FAILED,
                targetId = loginId,
                requestId = requestId,
                metadataJson = "{\"reason\":\"Account not found\"}"
            )
            // Generic error message to prevent account enumeration
            return ServerResponse(
                success = false,
                statusCode = 401,
                errorCode = "INVALID_CREDENTIALS",
                message = "Invalid ID or password.",
                requestId = requestId
            )
        }

        // Check explicit account lockout timestamp
        val now = System.currentTimeMillis()
        if (account.lockedUntil != null && account.lockedUntil > now) {
            return ServerResponse(
                success = false,
                statusCode = 401,
                errorCode = "ACCOUNT_LOCKED",
                message = "Account is temporarily locked. Please try again later.",
                requestId = requestId
            )
        }

        // 4. Constant-Time Password Verification
        val isPasswordValid = PasswordHasher.verifyPassword(
            password = password,
            saltHex = account.salt,
            expectedHashHex = account.passwordHash
        )

        if (!isPasswordValid) {
            val isLockedOut = rateLimiter.recordFailure(rateLimitKey)
            val newAttempts = account.failedLoginAttempts + 1
            val lockedUntil = if (newAttempts >= RateLimiter.MAX_FAILED_ATTEMPTS || isLockedOut) {
                now + RateLimiter.LOCKOUT_DURATION_MS
            } else null

            accountDao.updateLockout(account.id, newAttempts, lockedUntil)

            auditService.logEvent(
                actorId = account.id,
                actorRole = account.role,
                action = AuditActions.LOGIN_FAILED,
                targetId = account.id,
                requestId = requestId,
                metadataJson = "{\"reason\":\"Invalid password\",\"attempts\":$newAttempts}"
            )

            return ServerResponse(
                success = false,
                statusCode = 401,
                errorCode = "INVALID_CREDENTIALS",
                message = "Invalid ID or password.",
                requestId = requestId
            )
        }

        // 5. Account Status Enforcement
        val status = try {
            AccountStatus.valueOf(account.status)
        } catch (e: Exception) {
            AccountStatus.DISABLED
        }

        if (status == AccountStatus.SUSPENDED) {
            auditService.logEvent(
                actorId = account.id,
                actorRole = account.role,
                action = AuditActions.ACCOUNT_SUSPENDED_ACCESS_ATTEMPT,
                targetId = account.id,
                requestId = requestId
            )
            return ServerResponse(
                success = false,
                statusCode = 403,
                errorCode = "ACCOUNT_SUSPENDED",
                message = "Account has been suspended. Please contact your system administrator.",
                requestId = requestId
            )
        }

        if (status == AccountStatus.DISABLED) {
            return ServerResponse(
                success = false,
                statusCode = 403,
                errorCode = "ACCOUNT_DISABLED",
                message = "Account has been disabled.",
                requestId = requestId
            )
        }

        // 6. Reset Rate Limiter & Record Login
        rateLimiter.recordSuccess(rateLimitKey)
        accountDao.recordSuccessfulLogin(account.id, now)

        // 7. Create Session & Tokens
        val accessToken = TokenProvider.generateAccessToken()
        val refreshToken = TokenProvider.generateRefreshToken()
        val expiresAt = now + TokenProvider.ACCESS_TOKEN_VALIDITY_MS

        val sessionEntity = SessionEntity(
            id = UUID.randomUUID().toString(),
            accountId = account.id,
            role = account.role,
            accessToken = accessToken,
            refreshToken = refreshToken,
            isRevoked = false,
            createdAt = now,
            expiresAt = expiresAt,
            lastActivityAt = now,
            userAgent = userAgent,
            ipAddress = clientIp
        )
        sessionDao.insert(sessionEntity)

        // 8. Audit Logging
        val loginAction = when (account.role) {
            AccountRole.ADMIN.name -> AuditActions.ADMIN_LOGIN
            AccountRole.AGENT.name -> AuditActions.AGENT_LOGIN
            else -> AuditActions.USER_LOGIN
        }
        auditService.logEvent(
            actorId = account.id,
            actorRole = account.role,
            action = loginAction,
            targetId = account.id,
            requestId = requestId,
            metadataJson = "{\"ip\":\"${clientIp ?: "unknown"}\"}"
        )

        val userDto = UserProfileDto(
            id = account.id,
            loginId = account.loginId,
            role = account.role,
            status = account.status,
            fullName = account.fullName,
            mustChangePassword = account.mustChangePassword
        )

        return ServerResponse(
            success = true,
            statusCode = 200,
            data = LoginResponse(
                accessToken = accessToken,
                refreshToken = refreshToken,
                expiresIn = TokenProvider.ACCESS_TOKEN_VALIDITY_MS / 1000,
                user = userDto
            ),
            requestId = requestId
        )
    }

    /**
     * Resolves and verifies an active session, enforcing account status and role integrity.
     */
    suspend fun validateSession(accessToken: String): SecurityContext? {
        val cleanToken = accessToken.removePrefix("Bearer ").trim()
        val session = sessionDao.findActiveByAccessToken(cleanToken) ?: return null

        val now = System.currentTimeMillis()
        if (now > session.expiresAt) {
            sessionDao.revokeByAccessToken(cleanToken)
            return null
        }

        // Query fresh account record directly from authoritative database
        val account = accountDao.findById(session.accountId) ?: return null

        val status = try {
            AccountStatus.valueOf(account.status)
        } catch (e: Exception) {
            AccountStatus.DISABLED
        }

        // Account status check: Suspended or Disabled accounts immediately lose access
        if (status != AccountStatus.ACTIVE) {
            sessionDao.revokeAllForAccount(account.id)
            return null
        }

        val role = try {
            AccountRole.valueOf(account.role)
        } catch (e: Exception) {
            AccountRole.USER
        }

        // Update session activity timestamp
        sessionDao.updateActivity(cleanToken, now)

        val permissions = MahakalRbac.getPermissionsForRole(role)

        return SecurityContext(
            accountId = account.id,
            loginId = account.loginId,
            role = role,
            status = status,
            permissions = permissions,
            parentId = account.parentId
        )
    }

    /**
     * Server-side session invalidation upon logout.
     */
    suspend fun logout(
        accessToken: String,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<Unit> {
        val cleanToken = accessToken.removePrefix("Bearer ").trim()
        val session = sessionDao.findActiveByAccessToken(cleanToken)
        if (session != null) {
            sessionDao.revokeByAccessToken(cleanToken)
            auditService.logEvent(
                actorId = session.accountId,
                actorRole = session.role,
                action = AuditActions.LOGOUT,
                targetId = session.id,
                requestId = requestId
            )
        }
        return ServerResponse(
            success = true,
            statusCode = 200,
            data = Unit,
            message = "Session successfully invalidated.",
            requestId = requestId
        )
    }

    /**
     * Refresh session with strict token rotation and reuse detection.
     * If a previously revoked refresh token is presented, this constitutes a reuse attack;
     * the entire account session family is revoked immediately and a security alert is recorded.
     */
    suspend fun refreshSession(
        refreshToken: String,
        clientIp: String? = null,
        userAgent: String? = null,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<LoginResponse> {
        val cleanRefresh = refreshToken.trim()
        val now = System.currentTimeMillis()

        // 1. Find if this refresh token exists anywhere (even if revoked)
        val existingSession = sessionDao.findAnyByRefreshToken(cleanRefresh)
        if (existingSession == null) {
            auditService.logEvent(
                actorId = "anonymous",
                actorRole = "ANONYMOUS",
                action = AuditActions.TOKEN_REUSE_DETECTED,
                requestId = requestId,
                metadataJson = "{\"reason\":\"Unknown refresh token presented\"}"
            )
            return ServerResponse(
                success = false,
                statusCode = 401,
                errorCode = "INVALID_TOKEN",
                message = "Invalid or expired session token.",
                requestId = requestId
            )
        }

        // 2. Reuse Detection: If this session was already revoked or expired, revoke ALL sessions for this account!
        if (existingSession.isRevoked || existingSession.expiresAt < now) {
            sessionDao.revokeAllForAccount(existingSession.accountId)
            auditService.logEvent(
                actorId = existingSession.accountId,
                actorRole = existingSession.role,
                action = AuditActions.TOKEN_REUSE_DETECTED,
                targetId = existingSession.id,
                requestId = requestId,
                metadataJson = "{\"alert\":\"Rotated or revoked token reuse detected. All sessions terminated.\"}"
            )
            return ServerResponse(
                success = false,
                statusCode = 401,
                errorCode = "TOKEN_REUSE_DETECTED",
                message = "Security violation detected. Please sign in again.",
                requestId = requestId
            )
        }

        // 3. Verify Account Status
        val account = accountDao.findById(existingSession.accountId)
        if (account == null || account.status != AccountStatus.ACTIVE.name) {
            sessionDao.revokeAllForAccount(existingSession.accountId)
            return ServerResponse(
                success = false,
                statusCode = 403,
                errorCode = "ACCOUNT_INACTIVE",
                message = "Account is inactive or suspended.",
                requestId = requestId
            )
        }

        // 4. Revoke the old session as part of rotation
        sessionDao.revokeByAccessToken(existingSession.accessToken)

        // 5. Issue new rotated Access and Refresh tokens
        val newAccessToken = TokenProvider.generateAccessToken()
        val newRefreshToken = TokenProvider.generateRefreshToken()
        val newExpiresAt = now + TokenProvider.ACCESS_TOKEN_VALIDITY_MS

        val newSession = SessionEntity(
            id = UUID.randomUUID().toString(),
            accountId = account.id,
            role = account.role,
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
            isRevoked = false,
            createdAt = now,
            expiresAt = newExpiresAt,
            lastActivityAt = now,
            userAgent = userAgent ?: existingSession.userAgent,
            ipAddress = clientIp ?: existingSession.ipAddress
        )
        sessionDao.insert(newSession)

        auditService.logEvent(
            actorId = account.id,
            actorRole = account.role,
            action = AuditActions.TOKEN_REFRESHED,
            targetId = newSession.id,
            requestId = requestId
        )

        val userDto = UserProfileDto(
            id = account.id,
            loginId = account.loginId,
            role = account.role,
            status = account.status,
            fullName = account.fullName,
            mustChangePassword = account.mustChangePassword
        )

        return ServerResponse(
            success = true,
            statusCode = 200,
            data = LoginResponse(
                accessToken = newAccessToken,
                refreshToken = newRefreshToken,
                expiresIn = TokenProvider.ACCESS_TOKEN_VALIDITY_MS / 1000,
                user = userDto
            ),
            requestId = requestId
        )
    }

    /**
     * Secure password change. Validates old password, applies salt and slow hashing to new password.
     */
    suspend fun changePassword(
        accessToken: String,
        request: ChangePasswordRequest,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<Unit> {
        val context = validateSession(accessToken)
            ?: return ServerResponse(
                success = false,
                statusCode = 401,
                errorCode = "UNAUTHORIZED",
                message = "Invalid or expired session.",
                requestId = requestId
            )

        val newPassword = request.newPassword
        if (newPassword.length < 8) {
            return ServerResponse(
                success = false,
                statusCode = 422,
                errorCode = "WEAK_PASSWORD",
                message = "Password must be at least 8 characters long.",
                requestId = requestId
            )
        }
        if (!newPassword.any { it.isDigit() } || !newPassword.any { it.isLetter() }) {
            return ServerResponse(
                success = false,
                statusCode = 422,
                errorCode = "WEAK_PASSWORD",
                message = "Password must contain both letters and digits.",
                requestId = requestId
            )
        }

        val account = accountDao.findById(context.accountId)
            ?: return ServerResponse(
                success = false,
                statusCode = 404,
                errorCode = "ACCOUNT_NOT_FOUND",
                message = "Account not found.",
                requestId = requestId
            )

        val currentValid = PasswordHasher.verifyPassword(
            password = request.currentPassword,
            saltHex = account.salt,
            expectedHashHex = account.passwordHash
        )
        if (!currentValid) {
            return ServerResponse(
                success = false,
                statusCode = 401,
                errorCode = "INVALID_CREDENTIALS",
                message = "Current password verification failed.",
                requestId = requestId
            )
        }

        val newSalt = PasswordHasher.generateSalt()
        val newHash = PasswordHasher.hashPassword(newPassword, newSalt)
        val now = System.currentTimeMillis()

        accountDao.updatePassword(account.id, newHash, newSalt, now)

        notificationService?.onPasswordChanged(account.id)

        auditService.logEvent(
            actorId = account.id,
            actorRole = account.role,
            action = AuditActions.PASSWORD_CHANGED,
            targetId = account.id,
            requestId = requestId
        )

        return ServerResponse(
            success = true,
            statusCode = 200,
            data = Unit,
            message = "Password successfully updated.",
            requestId = requestId
        )
    }

    /**
     * Resolves authenticated profile safely from verified session context.
     */
    suspend fun getMe(accessToken: String, requestId: String = UUID.randomUUID().toString()): ServerResponse<UserProfileDto> {
        val context = validateSession(accessToken)
            ?: return ServerResponse(
                success = false,
                statusCode = 401,
                errorCode = "UNAUTHORIZED",
                message = "Invalid or expired session.",
                requestId = requestId
            )

        val account = accountDao.findById(context.accountId)
            ?: return ServerResponse(
                success = false,
                statusCode = 404,
                errorCode = "NOT_FOUND",
                message = "Account record not found.",
                requestId = requestId
            )

        return ServerResponse(
            success = true,
            statusCode = 200,
            data = UserProfileDto(
                id = account.id,
                loginId = account.loginId,
                role = account.role,
                status = account.status,
                fullName = account.fullName,
                mustChangePassword = account.mustChangePassword
            ),
            requestId = requestId
        )
    }

    /**
     * Bootstraps the initial Admin account. Runs ONLY if zero admin accounts exist.
     */
    suspend fun bootstrapInitialAdmin(
        loginId: String,
        temporaryPassword: String,
        fullName: String
    ): Boolean {
        if (accountDao.countAdmins() > 0) {
            return false // Admin already provisioned
        }
        val salt = PasswordHasher.generateSalt()
        val hash = PasswordHasher.hashPassword(temporaryPassword, salt)
        val now = System.currentTimeMillis()
        val admin = AccountEntity(
            id = UUID.randomUUID().toString(),
            loginId = loginId,
            passwordHash = hash,
            salt = salt,
            role = AccountRole.ADMIN.name,
            status = AccountStatus.ACTIVE.name,
            parentId = null,
            fullName = fullName,
            createdAt = now,
            updatedAt = now,
            mustChangePassword = false
        )
        accountDao.insert(admin)
        auditService.logEvent(
            actorId = admin.id,
            actorRole = AccountRole.ADMIN.name,
            action = AuditActions.INITIAL_ADMIN_BOOTSTRAP,
            targetId = admin.id,
            metadataJson = "{\"loginId\":\"$loginId\"}"
        )
        return true
    }

    /**
     * Provision helper for development and testing: provisions an account under parent hierarchy.
     */
    suspend fun provisionAccountForTesting(
        loginId: String,
        password: String,
        role: AccountRole,
        status: AccountStatus = AccountStatus.ACTIVE,
        parentId: String? = null,
        fullName: String = loginId
    ): AccountEntity {
        val salt = PasswordHasher.generateSalt()
        val hash = PasswordHasher.hashPassword(password, salt)
        val now = System.currentTimeMillis()
        val account = AccountEntity(
            id = UUID.randomUUID().toString(),
            loginId = loginId,
            passwordHash = hash,
            salt = salt,
            role = role.name,
            status = status.name,
            parentId = parentId,
            fullName = fullName,
            createdAt = now,
            updatedAt = now,
            mustChangePassword = false
        )
        accountDao.insert(account)
        return account
    }

    /**
     * Terminate an active session explicitly by session ID (Admin or User initiated).
     */
    suspend fun revokeSessionById(
        sessionId: String,
        actorId: String,
        actorRole: String,
        requestId: String = UUID.randomUUID().toString()
    ): Boolean {
        // Query session from DB
        val sessions = sessionDao.getSessionsPaged(100, 0)
        val target = sessions.find { it.id == sessionId } ?: return false
        sessionDao.revokeByAccessToken(target.accessToken)
        auditService.logEvent(
            actorId = actorId,
            actorRole = actorRole,
            action = AuditActions.SESSION_REVOKED,
            targetId = target.id,
            targetType = "SESSION",
            requestId = requestId,
            metadataJson = "{\"accountId\":\"${target.accountId}\"}"
        )
        return true
    }

    /**
     * Terminate all active sessions for an account (e.g. security reset or admin force logout).
     */
    suspend fun revokeAllSessionsForAccount(
        targetAccountId: String,
        actorId: String,
        actorRole: String,
        requestId: String = UUID.randomUUID().toString()
    ) {
        sessionDao.revokeAllForAccount(targetAccountId)
        auditService.logEvent(
            actorId = actorId,
            actorRole = actorRole,
            action = AuditActions.SESSION_REVOKED,
            targetId = targetAccountId,
            targetType = "ACCOUNT_ALL_SESSIONS",
            requestId = requestId
        )
    }
}
