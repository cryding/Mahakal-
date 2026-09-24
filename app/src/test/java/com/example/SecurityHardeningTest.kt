package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.backend.api.ServerApiRouter
import com.example.backend.audit.AuditService
import com.example.backend.database.MahakalServerDatabase
import com.example.backend.model.ChangePasswordRequest
import com.example.backend.model.CreateUserRequest
import com.example.backend.model.LoginRequest
import com.example.backend.rbac.AccountRole
import com.example.backend.rbac.AccountStatus
import com.example.backend.rbac.MahakalRbac
import com.example.backend.security.RateLimiter
import com.example.backend.security.TokenProvider
import com.example.backend.service.AdminAgentService
import com.example.backend.service.AgentUserService
import com.example.backend.service.NotificationService
import com.example.backend.service.SecurityHardeningService
import com.example.backend.service.ServerAuthService
import com.example.backend.service.WalletTransactionService
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SecurityHardeningTest {

    private lateinit var database: MahakalServerDatabase
    private lateinit var auditService: AuditService
    private lateinit var rateLimiter: RateLimiter
    private lateinit var notificationService: NotificationService
    private lateinit var authService: ServerAuthService
    private lateinit var adminAgentService: AdminAgentService
    private lateinit var agentUserService: AgentUserService
    private lateinit var walletService: WalletTransactionService
    private lateinit var hardeningService: SecurityHardeningService
    private lateinit var apiRouter: ServerApiRouter

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MahakalServerDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        auditService = AuditService(database.auditLogDao())
        rateLimiter = RateLimiter(database.rateLimitDao())
        notificationService = NotificationService(database, auditService, rateLimiter)
        authService = ServerAuthService(database, auditService, rateLimiter, notificationService)
        adminAgentService = AdminAgentService(database, auditService, rateLimiter)
        agentUserService = AgentUserService(database, auditService, rateLimiter)
        walletService = WalletTransactionService(database, auditService, rateLimiter, notificationService)
        hardeningService = SecurityHardeningService(database, auditService, rateLimiter, authService)

        apiRouter = ServerApiRouter(
            authService = authService,
            auditService = auditService,
            adminAgentService = adminAgentService,
            agentUserService = agentUserService,
            walletTransactionService = walletService,
            gameEngineService = null,
            notificationService = notificationService,
            securityHardeningService = hardeningService
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testInitialAdminBootstrapAndSecureLogin() = runBlocking {
        // Initial Admin bootstrap
        val bootstrapped = authService.bootstrapInitialAdmin("admin_sec", "AdminPass@123", "Super Admin")
        assertTrue(bootstrapped)

        // Login with correct credentials
        val loginResp = authService.login(LoginRequest("admin_sec", "AdminPass@123"))
        assertTrue(loginResp.success)
        assertNotNull(loginResp.data?.accessToken)

        val accessToken = loginResp.data!!.accessToken

        // Validate session
        val context = authService.validateSession(accessToken)
        assertNotNull(context)
        assertEquals("admin_sec", context?.loginId)
        assertEquals(AccountRole.ADMIN, context?.role)
    }

    @Test
    fun testBruteForceProtectionAndLockout() = runBlocking {
        authService.bootstrapInitialAdmin("admin_test", "AdminPass@123", "Admin")

        // 5 consecutive invalid logins should trigger lockout
        for (i in 1..5) {
            val resp = authService.login(LoginRequest("admin_test", "WrongPassword!"))
            assertFalse(resp.success)
        }

        // 6th attempt should be blocked with rate limit exceeded
        val lockedResp = authService.login(LoginRequest("admin_test", "WrongPassword!"))
        assertFalse(lockedResp.success)
        assertTrue(lockedResp.errorCode == "RATE_LIMIT_EXCEEDED" || lockedResp.errorCode == "ACCOUNT_LOCKED")
    }

    @Test
    fun testSessionRevocationAndDeviceSecurity() = runBlocking {
        authService.bootstrapInitialAdmin("admin_sess", "AdminPass@123", "Admin")
        val loginResp = authService.login(LoginRequest("admin_sess", "AdminPass@123"))
        val token = loginResp.data!!.accessToken

        // Active session check
        val activeContext = authService.validateSession(token)
        assertNotNull(activeContext)

        // Revoke session via Hardening Service
        val revokeResp = hardeningService.revokeAllAccountSessions(activeContext!!, activeContext.accountId)
        assertTrue(revokeResp.success)

        // Session should now be invalid
        val revokedContext = authService.validateSession(token)
        assertNull(revokedContext)
    }

    @Test
    fun testSecurityDashboardSummaryMetrics() = runBlocking {
        authService.bootstrapInitialAdmin("admin_dash", "AdminPass@123", "Admin")
        val loginResp = authService.login(LoginRequest("admin_dash", "AdminPass@123"))
        val token = loginResp.data!!.accessToken
        val authHeader = "Bearer $token"

        val summaryResp = apiRouter.handleGetSecurityDashboardSummary(authHeader)
        assertTrue(summaryResp.success)
        assertNotNull(summaryResp.data)
        assertEquals("HEALTHY", summaryResp.data?.systemStatus)
        assertTrue((summaryResp.data?.activeSessionsCount ?: 0) >= 1)
    }

    @Test
    fun testIdorProtectionAcrossAccounts() = runBlocking {
        authService.bootstrapInitialAdmin("admin_idor", "AdminPass@123", "Admin")
        val agent = authService.provisionAccountForTesting("agent_idor", "AgentPass@123", AccountRole.AGENT)

        val agentLogin = authService.login(LoginRequest("agent_idor", "AgentPass@123"))
        val agentToken = agentLogin.data!!.accessToken
        val agentHeader = "Bearer $agentToken"

        // Agent attempts to view Admin's active sessions (cross-account IDOR)
        val idorResp = apiRouter.handleGetActiveSessions(agentHeader, targetAccountId = "admin_idor")
        assertFalse(idorResp.success)
        assertEquals(403, idorResp.statusCode)
        assertEquals("FORBIDDEN", idorResp.errorCode)
    }

    @Test
    fun testSystemHealthCheck() = runBlocking {
        val health = apiRouter.handleHealthCheck()
        assertEquals("OK", health.status)
        assertEquals("UP", health.database)
    }
}
