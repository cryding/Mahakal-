package com.example

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.backend.api.ServerApiRouter
import com.example.backend.audit.AuditActions
import com.example.backend.audit.AuditService
import com.example.backend.database.MahakalServerDatabase
import com.example.backend.model.CreateAgentRequest
import com.example.backend.model.EditAgentRequest
import com.example.backend.model.LoginRequest
import com.example.backend.model.ResetAgentPasswordRequest
import com.example.backend.rbac.AccountRole
import com.example.backend.security.RateLimiter
import com.example.backend.service.AdminAgentService
import com.example.backend.service.ServerAuthService
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AdminAgentManagementTest {

    private lateinit var database: MahakalServerDatabase
    private lateinit var auditService: AuditService
    private lateinit var rateLimiter: RateLimiter
    private lateinit var authService: ServerAuthService
    private lateinit var adminAgentService: AdminAgentService
    private lateinit var apiRouter: ServerApiRouter

    private lateinit var admin1Token: String
    private lateinit var admin2Token: String
    private lateinit var nonAdminToken: String

    @Before
    fun setup() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            database = Room.inMemoryDatabaseBuilder(context, MahakalServerDatabase::class.java)
                .allowMainThreadQueries()
                .build()
            auditService = AuditService(database.auditLogDao())
            rateLimiter = RateLimiter(database.rateLimitDao())
            authService = ServerAuthService(database, auditService, rateLimiter)
            adminAgentService = AdminAgentService(database, auditService, rateLimiter)
            apiRouter = ServerApiRouter(authService, auditService, adminAgentService)

            // Bootstrap Admin 1
            authService.bootstrapInitialAdmin("admin_primary", "AdminSec@123", "Primary Admin")
            val admin1Login = authService.login(LoginRequest("admin_primary", "AdminSec@123"))
            admin1Token = "Bearer ${admin1Login.data!!.accessToken}"

            // Bootstrap Admin 2 (for cross-admin ownership tests)
            authService.provisionAccountForTesting(
                loginId = "admin_secondary",
                password = "AdminSec@456",
                role = AccountRole.ADMIN,
                fullName = "Secondary Admin"
            )
            val admin2Login = authService.login(LoginRequest("admin_secondary", "AdminSec@456"))
            admin2Token = "Bearer ${admin2Login.data!!.accessToken}"
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun test01_admin_creates_agent_successfully() = runBlocking {
        val createRes = apiRouter.handleCreateAgent(
            authHeader = admin1Token,
            request = CreateAgentRequest(
                agentId = "AGENT_ALPHA",
                agentName = "Alpha Agent",
                temporaryPassword = "TempPassword@1",
                notes = "First test agent"
            )
        )

        assertTrue(createRes.success)
        assertEquals(201, createRes.statusCode)
        assertNotNull(createRes.data)
        assertEquals("AGENT_ALPHA", createRes.data!!.agentId)
        assertEquals("Alpha Agent", createRes.data!!.agentName)
        assertEquals("ACTIVE", createRes.data!!.status)
    }

    @Test
    fun test02_non_admin_cannot_create_agent_returns_403() = runBlocking {
        // Create an agent first under admin1
        apiRouter.handleCreateAgent(
            authHeader = admin1Token,
            request = CreateAgentRequest("AGENT_BETA", "Beta Agent", "BetaPass@123")
        )
        // Login as this agent
        val agentLogin = authService.login(LoginRequest("AGENT_BETA", "BetaPass@123"))
        nonAdminToken = "Bearer ${agentLogin.data!!.accessToken}"

        // Attempt to create another agent using the agent token
        val res = apiRouter.handleCreateAgent(
            authHeader = nonAdminToken,
            request = CreateAgentRequest("AGENT_GAMMA", "Gamma Agent", "GammaPass@123")
        )

        assertFalse(res.success)
        assertEquals(403, res.statusCode)
        assertEquals("FORBIDDEN", res.errorCode)
    }

    @Test
    fun test03_unauthenticated_request_returns_401() = runBlocking {
        val res = apiRouter.handleCreateAgent(
            authHeader = null,
            request = CreateAgentRequest("AGENT_UNAUTH", "Unauth Agent", "UnauthPass@123")
        )

        assertFalse(res.success)
        assertEquals(401, res.statusCode)
        assertEquals("UNAUTHORIZED", res.errorCode)
    }

    @Test
    fun test04_duplicate_agent_id_returns_409() = runBlocking {
        val req = CreateAgentRequest("AGENT_DUP", "Original", "DupPass@123")
        val res1 = apiRouter.handleCreateAgent(admin1Token, req)
        assertTrue(res1.success)

        val res2 = apiRouter.handleCreateAgent(admin1Token, req)
        assertFalse(res2.success)
        assertEquals(409, res2.statusCode)
        assertEquals("AGENT_ID_ALREADY_EXISTS", res2.errorCode)
    }

    @Test
    fun test05_invalid_agent_id_format_returns_422() = runBlocking {
        // Too short (<3)
        val resShort = apiRouter.handleCreateAgent(admin1Token, CreateAgentRequest("AG", "Name", "ValidPass@123"))
        assertFalse(resShort.success)
        assertEquals(422, resShort.statusCode)

        // Invalid characters (special char '@')
        val resInvalidChar = apiRouter.handleCreateAgent(admin1Token, CreateAgentRequest("AGENT@99", "Name", "ValidPass@123"))
        assertFalse(resInvalidChar.success)
        assertEquals(422, resInvalidChar.statusCode)
    }

    @Test
    fun test06_weak_temporary_password_returns_422() = runBlocking {
        // Short password (<8)
        val resShort = apiRouter.handleCreateAgent(admin1Token, CreateAgentRequest("AGENT_WEAK1", "Name", "short1"))
        assertFalse(resShort.success)
        assertEquals(422, resShort.statusCode)

        // Password without digits
        val resNoDigits = apiRouter.handleCreateAgent(admin1Token, CreateAgentRequest("AGENT_WEAK2", "Name", "NoDigitsHere"))
        assertFalse(resNoDigits.success)
        assertEquals(422, resNoDigits.statusCode)
    }

    @Test
    fun test07_and_08_admin_lists_own_agents_and_excludes_other_admins() = runBlocking {
        // Admin 1 creates Agent A1
        apiRouter.handleCreateAgent(admin1Token, CreateAgentRequest("AGENT_A1", "Agent A1", "PassA1@123"))
        // Admin 2 creates Agent B1
        apiRouter.handleCreateAgent(admin2Token, CreateAgentRequest("AGENT_B1", "Agent B1", "PassB1@123"))

        // Admin 1 lists agents
        val list1 = apiRouter.handleGetAgents(admin1Token)
        assertTrue(list1.success)
        val agents1 = list1.data!!.agents
        assertTrue(agents1.any { it.agentId == "AGENT_A1" })
        assertFalse(agents1.any { it.agentId == "AGENT_B1" })

        // Admin 2 lists agents
        val list2 = apiRouter.handleGetAgents(admin2Token)
        assertTrue(list2.success)
        val agents2 = list2.data!!.agents
        assertTrue(agents2.any { it.agentId == "AGENT_B1" })
        assertFalse(agents2.any { it.agentId == "AGENT_A1" })
    }

    @Test
    fun test09_admin_modifying_another_admins_agent_returns_403() = runBlocking {
        // Admin 2 creates Agent B2
        val created = apiRouter.handleCreateAgent(admin2Token, CreateAgentRequest("AGENT_B2", "Agent B2", "PassB2@123"))
        val agentId = created.data!!.id

        // Admin 1 attempts to update Agent B2
        val updateRes = apiRouter.handleUpdateAgent(
            authHeader = admin1Token,
            agentId = agentId,
            request = EditAgentRequest(agentName = "Malicious Hijack")
        )
        assertFalse(updateRes.success)
        assertEquals(403, updateRes.statusCode)
        assertEquals("FORBIDDEN", updateRes.errorCode)

        // Admin 1 attempts to suspend Agent B2
        val suspendRes = apiRouter.handleSuspendAgent(admin1Token, agentId)
        assertFalse(suspendRes.success)
        assertEquals(403, suspendRes.statusCode)

        // Admin 1 attempts to reset password of Agent B2
        val resetRes = apiRouter.handleResetAgentPassword(admin1Token, agentId, ResetAgentPasswordRequest())
        assertFalse(resetRes.success)
        assertEquals(403, resetRes.statusCode)
    }

    @Test
    fun test10_11_12_admin_suspends_agent_blocks_login_and_invalidates_sessions() = runBlocking {
        // Create Agent S1
        val created = apiRouter.handleCreateAgent(admin1Token, CreateAgentRequest("AGENT_S1", "Agent S1", "PassS1@123"))
        val agentId = created.data!!.id

        // Login as Agent S1
        val loginBefore = authService.login(LoginRequest("AGENT_S1", "PassS1@123"))
        assertTrue(loginBefore.success)
        val agentSessionToken = loginBefore.data!!.accessToken

        // Verify session is valid before suspension
        val sessionBefore = authService.validateSession(agentSessionToken)
        assertNotNull(sessionBefore)

        // Admin suspends Agent S1
        val suspendRes = apiRouter.handleSuspendAgent(admin1Token, agentId)
        assertTrue(suspendRes.success)

        // Verify active session was immediately invalidated
        val sessionAfter = authService.validateSession(agentSessionToken)
        org.junit.Assert.assertNull(sessionAfter)

        // Verify login is blocked
        val loginAfter = authService.login(LoginRequest("AGENT_S1", "PassS1@123"))
        assertFalse(loginAfter.success)
        assertEquals(403, loginAfter.statusCode)
        assertEquals("ACCOUNT_SUSPENDED", loginAfter.errorCode)
    }

    @Test
    fun test13_14_admin_activates_agent_allows_login() = runBlocking {
        // Create and suspend
        val created = apiRouter.handleCreateAgent(admin1Token, CreateAgentRequest("AGENT_ACT", "Agent Act", "PassAct@123"))
        val agentId = created.data!!.id
        apiRouter.handleSuspendAgent(admin1Token, agentId)

        // Activate
        val activateRes = apiRouter.handleActivateAgent(admin1Token, agentId)
        assertTrue(activateRes.success)

        // Login works
        val loginRes = authService.login(LoginRequest("AGENT_ACT", "PassAct@123"))
        assertTrue(loginRes.success)
        assertEquals(200, loginRes.statusCode)
    }

    @Test
    fun test15_to_18_admin_resets_agent_password_lifecycle() = runBlocking {
        // Create Agent
        val created = apiRouter.handleCreateAgent(admin1Token, CreateAgentRequest("AGENT_RST", "Agent Reset", "InitialPass@123"))
        val agentId = created.data!!.id

        // Initial login session
        val loginInit = authService.login(LoginRequest("AGENT_RST", "InitialPass@123"))
        val oldToken = loginInit.data!!.accessToken

        // Admin resets password with custom temporary password
        val resetRes = apiRouter.handleResetAgentPassword(
            admin1Token,
            agentId,
            ResetAgentPasswordRequest(temporaryPassword = "NewTempPass@789")
        )
        assertTrue(resetRes.success)
        assertEquals("NewTempPass@789", resetRes.data!!.temporaryPassword)

        // 16. Old password invalid
        val oldLogin = authService.login(LoginRequest("AGENT_RST", "InitialPass@123"))
        assertFalse(oldLogin.success)
        assertEquals(401, oldLogin.statusCode)

        // Old session token revoked
        val oldSession = authService.validateSession(oldToken)
        org.junit.Assert.assertNull(oldSession)

        // 17. New temporary password works
        val newLogin = authService.login(LoginRequest("AGENT_RST", "NewTempPass@789"))
        assertTrue(newLogin.success)

        // 18. mustChangePassword is true
        assertTrue(newLogin.data!!.user.mustChangePassword)
    }

    @Test
    fun test19_and_20_verify_audit_log_and_no_password_logged() = runBlocking {
        val created = apiRouter.handleCreateAgent(
            admin1Token,
            CreateAgentRequest("AGENT_AUDIT", "Agent Audit", "SensitivePwd@999")
        )
        val agentId = created.data!!.id

        apiRouter.handleUpdateAgent(admin1Token, agentId, EditAgentRequest(agentName = "Audit Updated"))
        apiRouter.handleSuspendAgent(admin1Token, agentId)
        apiRouter.handleActivateAgent(admin1Token, agentId)
        apiRouter.handleResetAgentPassword(admin1Token, agentId, ResetAgentPasswordRequest("NewSensitivePwd@111"))

        // Retrieve audit logs for this target
        val logs = database.auditLogDao().getLogsForTarget(agentId, limit = 50)
        assertTrue(logs.isNotEmpty())

        val actions = logs.map { it.action }
        assertTrue(actions.contains(AuditActions.AGENT_CREATED))
        assertTrue(actions.contains(AuditActions.AGENT_UPDATED))
        assertTrue(actions.contains(AuditActions.AGENT_SUSPENDED))
        assertTrue(actions.contains(AuditActions.AGENT_ACTIVATED))
        assertTrue(actions.contains(AuditActions.AGENT_PASSWORD_RESET))

        // Check that NO audit log contains the temporary passwords
        logs.forEach { log ->
            val meta = log.metadataJson ?: ""
            assertFalse(meta.contains("SensitivePwd@999"))
            assertFalse(meta.contains("NewSensitivePwd@111"))
            assertFalse(meta.contains("password", ignoreCase = true))
        }
    }
}
