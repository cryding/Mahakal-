package com.example

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.backend.api.ServerApiRouter
import com.example.backend.audit.AuditActions
import com.example.backend.audit.AuditService
import com.example.backend.database.MahakalServerDatabase
import com.example.backend.model.ChangePasswordRequest
import com.example.backend.model.CreateUserRequest
import com.example.backend.model.EditUserRequest
import com.example.backend.model.LoginRequest
import com.example.backend.model.ResetUserPasswordRequest
import com.example.backend.rbac.AccountRole
import com.example.backend.rbac.AccountStatus
import com.example.backend.security.RateLimiter
import com.example.backend.service.AdminAgentService
import com.example.backend.service.AgentUserService
import com.example.backend.service.ServerAuthService
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
class AgentUserManagementTest {

    private lateinit var database: MahakalServerDatabase
    private lateinit var auditService: AuditService
    private lateinit var rateLimiter: RateLimiter
    private lateinit var authService: ServerAuthService
    private lateinit var adminAgentService: AdminAgentService
    private lateinit var agentUserService: AgentUserService
    private lateinit var apiRouter: ServerApiRouter

    private lateinit var adminToken: String
    private lateinit var agent1Token: String
    private lateinit var agent2Token: String
    private lateinit var agent1Id: String
    private lateinit var agent2Id: String

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
            agentUserService = AgentUserService(database, auditService, rateLimiter)
            apiRouter = ServerApiRouter(authService, auditService, adminAgentService, agentUserService)

            // 1. Bootstrap Admin
            authService.bootstrapInitialAdmin("admin_root", "AdminSec@123", "Root Admin")
            val adminLogin = authService.login(LoginRequest("admin_root", "AdminSec@123"))
            adminToken = "Bearer ${adminLogin.data!!.accessToken}"

            // 2. Provision Agent 1
            val agent1Account = authService.provisionAccountForTesting(
                loginId = "AGENT_01",
                password = "AgentPassword@1",
                role = AccountRole.AGENT,
                fullName = "Agent One"
            )
            agent1Id = agent1Account.id
            val agent1Login = authService.login(LoginRequest("AGENT_01", "AgentPassword@1"))
            agent1Token = "Bearer ${agent1Login.data!!.accessToken}"

            // 3. Provision Agent 2 (for cross-agent ownership tests)
            val agent2Account = authService.provisionAccountForTesting(
                loginId = "AGENT_02",
                password = "AgentPassword@2",
                role = AccountRole.AGENT,
                fullName = "Agent Two"
            )
            agent2Id = agent2Account.id
            val agent2Login = authService.login(LoginRequest("AGENT_02", "AgentPassword@2"))
            agent2Token = "Bearer ${agent2Login.data!!.accessToken}"
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun test01_agent_creates_user_successfully() = runBlocking {
        val res = apiRouter.handleCreateUser(
            authHeader = agent1Token,
            request = CreateUserRequest(
                userId = "USR_ALPHA",
                displayName = "Alpha Player",
                temporaryPassword = "UserPassword@123",
                notes = "Test user 1"
            )
        )

        assertTrue(res.success)
        assertEquals(201, res.statusCode)
        assertNotNull(res.data)
        assertEquals("USR_ALPHA", res.data!!.userId)
        assertEquals("Alpha Player", res.data!!.displayName)
        assertEquals(agent1Id, res.data!!.agentId) // Ownership strictly derived
        assertEquals("ACTIVE", res.data!!.status)
        assertEquals("USER", res.data!!.role)
        assertEquals("UserPassword@123", res.data!!.temporaryPassword)

        // Verify in database
        val entity = database.accountDao().findByLoginId("USR_ALPHA")
        assertNotNull(entity)
        assertEquals(agent1Id, entity!!.parentId)
        assertTrue(entity.mustChangePassword)

        // Verify audit log
        val auditLogs = auditService.getRecentLogsForTarget(entity.id)
        assertTrue(auditLogs.any { it.action == AuditActions.USER_CREATED })
    }

    @Test
    fun test02_user_cannot_create_user_returns_403() = runBlocking {
        // Create user first
        val createRes = apiRouter.handleCreateUser(
            authHeader = agent1Token,
            request = CreateUserRequest("USR_BETA", "Beta Player", "UserPassword@123")
        )
        assertTrue(createRes.success)

        // Login as User
        val userLogin = authService.login(LoginRequest("USR_BETA", "UserPassword@123"))
        val userToken = "Bearer ${userLogin.data!!.accessToken}"

        // User attempts to create another user
        val failRes = apiRouter.handleCreateUser(
            authHeader = userToken,
            request = CreateUserRequest("USR_GAMMA", "Gamma Player", "UserPassword@123")
        )
        assertFalse(failRes.success)
        assertEquals(403, failRes.statusCode)
    }

    @Test
    fun test03_agent_cannot_create_agent_returns_403() = runBlocking {
        val failRes = apiRouter.handleCreateAgent(
            authHeader = agent1Token,
            request = com.example.backend.model.CreateAgentRequest("AGENT_SUB", "Sub Agent", "AgentPass@1")
        )
        assertFalse(failRes.success)
        assertEquals(403, failRes.statusCode)
    }

    @Test
    fun test04_unauthenticated_create_user_returns_401() = runBlocking {
        val failRes = apiRouter.handleCreateUser(
            authHeader = null,
            request = CreateUserRequest("USR_NOAUTH", "No Auth", "UserPassword@123")
        )
        assertFalse(failRes.success)
        assertEquals(401, failRes.statusCode)
    }

    @Test
    fun test05_duplicate_user_id_returns_409() = runBlocking {
        apiRouter.handleCreateUser(
            authHeader = agent1Token,
            request = CreateUserRequest("USR_DUP", "Original", "UserPassword@123")
        )

        val dupRes = apiRouter.handleCreateUser(
            authHeader = agent1Token,
            request = CreateUserRequest("USR_DUP", "Duplicate", "UserPassword@456")
        )
        assertFalse(dupRes.success)
        assertEquals(409, dupRes.statusCode)
        assertEquals("USER_ID_ALREADY_EXISTS", dupRes.errorCode)
    }

    @Test
    fun test06_invalid_user_id_returns_422() = runBlocking {
        // Short ID (< 3 chars)
        val res1 = apiRouter.handleCreateUser(
            authHeader = agent1Token,
            request = CreateUserRequest("U1", "Short ID", "UserPassword@123")
        )
        assertFalse(res1.success)
        assertEquals(422, res1.statusCode)

        // Invalid characters (spaces, special symbols)
        val res2 = apiRouter.handleCreateUser(
            authHeader = agent1Token,
            request = CreateUserRequest("USER!@#", "Invalid Chars", "UserPassword@123")
        )
        assertFalse(res2.success)
        assertEquals(422, res2.statusCode)
    }

    @Test
    fun test07_weak_password_returns_422() = runBlocking {
        val res = apiRouter.handleCreateUser(
            authHeader = agent1Token,
            request = CreateUserRequest("USR_WEAK", "Weak Pwd", "weak")
        )
        assertFalse(res.success)
        assertEquals(422, res.statusCode)
        assertEquals("VALIDATION_ERROR", res.errorCode)
    }

    @Test
    fun test08_cross_agent_isolation_agent_cannot_access_other_agents_user() = runBlocking {
        // Agent 1 creates user 1
        val user1Res = apiRouter.handleCreateUser(
            authHeader = agent1Token,
            request = CreateUserRequest("USR_AGENT1", "Player 1", "UserPassword@123")
        )
        assertTrue(user1Res.success)
        val userId = user1Res.data!!.id

        // Agent 2 attempts to view Agent 1's user details -> 403
        val viewRes = apiRouter.handleGetUserDetails(authHeader = agent2Token, userId = userId)
        assertFalse(viewRes.success)
        assertEquals(403, viewRes.statusCode)

        // Agent 2 attempts to edit Agent 1's user -> 403
        val editRes = apiRouter.handleUpdateUser(
            authHeader = agent2Token,
            userId = userId,
            request = EditUserRequest(displayName = "Hacked Name")
        )
        assertFalse(editRes.success)
        assertEquals(403, editRes.statusCode)

        // Agent 2 attempts to suspend Agent 1's user -> 403
        val suspendRes = apiRouter.handleSuspendUser(authHeader = agent2Token, userId = userId)
        assertFalse(suspendRes.success)
        assertEquals(403, suspendRes.statusCode)

        // Agent 2 attempts to reset password of Agent 1's user -> 403
        val resetRes = apiRouter.handleResetUserPassword(
            authHeader = agent2Token,
            userId = userId,
            request = ResetUserPasswordRequest()
        )
        assertFalse(resetRes.success)
        assertEquals(403, resetRes.statusCode)
    }

    @Test
    fun test09_suspended_agent_cannot_create_user() = runBlocking {
        // Suspend Agent 1
        database.accountDao().updateStatus(agent1Id, AccountStatus.SUSPENDED.name, System.currentTimeMillis())

        // Active session becomes invalid -> 401 Unauthorized / Revoked
        val res = apiRouter.handleCreateUser(
            authHeader = agent1Token,
            request = CreateUserRequest("USR_BLOCKED", "Blocked Player", "UserPassword@123")
        )
        assertFalse(res.success)
        assertTrue(res.statusCode == 401 || res.statusCode == 403)

        // Attempting to login as suspended agent returns 403 ACCOUNT_SUSPENDED
        val loginRes = authService.login(LoginRequest("AGENT_01", "AgentPassword@1"))
        assertFalse(loginRes.success)
        assertEquals(403, loginRes.statusCode)
        assertEquals("ACCOUNT_SUSPENDED", loginRes.errorCode)
    }

    @Test
    fun test10_user_suspension_and_login_blocking() = runBlocking {
        // Create user
        val userRes = apiRouter.handleCreateUser(
            authHeader = agent1Token,
            request = CreateUserRequest("USR_SUSPENDABLE", "Active Player", "UserPassword@123")
        )
        val userId = userRes.data!!.id

        // User can login
        val login1 = authService.login(LoginRequest("USR_SUSPENDABLE", "UserPassword@123"))
        assertTrue(login1.success)

        // Agent suspends user
        val suspRes = apiRouter.handleSuspendUser(authHeader = agent1Token, userId = userId)
        assertTrue(suspRes.success)

        // User cannot login when suspended
        val login2 = authService.login(LoginRequest("USR_SUSPENDABLE", "UserPassword@123"))
        assertFalse(login2.success)
        assertEquals(403, login2.statusCode)
        assertEquals("ACCOUNT_SUSPENDED", login2.errorCode)

        // Agent activates user
        val actRes = apiRouter.handleActivateUser(authHeader = agent1Token, userId = userId)
        assertTrue(actRes.success)

        // User can login again
        val login3 = authService.login(LoginRequest("USR_SUSPENDABLE", "UserPassword@123"))
        assertTrue(login3.success)
    }

    @Test
    fun test11_password_reset_invalidates_old_password_and_sessions() = runBlocking {
        val userRes = apiRouter.handleCreateUser(
            authHeader = agent1Token,
            request = CreateUserRequest("USR_RESET_TEST", "Reset Player", "InitialPassword@123")
        )
        val userId = userRes.data!!.id

        // Login with initial password
        val login1 = authService.login(LoginRequest("USR_RESET_TEST", "InitialPassword@123"))
        assertTrue(login1.success)
        val oldSessionToken = login1.data!!.accessToken

        // Old session is valid
        val validCheck1 = authService.validateSession(oldSessionToken)
        assertNotNull(validCheck1)

        // Reset password
        val resetRes = apiRouter.handleResetUserPassword(
            authHeader = agent1Token,
            userId = userId,
            request = ResetUserPasswordRequest()
        )
        assertTrue(resetRes.success)
        val newTempPassword = resetRes.data!!.temporaryPassword

        // Old session is revoked
        val validCheck2 = authService.validateSession(oldSessionToken)
        assertNull(validCheck2)

        // Old password fails
        val loginOld = authService.login(LoginRequest("USR_RESET_TEST", "InitialPassword@123"))
        assertFalse(loginOld.success)

        // New temp password succeeds
        val loginNew = authService.login(LoginRequest("USR_RESET_TEST", newTempPassword))
        assertTrue(loginNew.success)
    }

    @Test
    fun test12_temporary_password_never_logged_in_audit() = runBlocking {
        val secretPassword = "SuperSecretPassword@999"
        apiRouter.handleCreateUser(
            authHeader = agent1Token,
            request = CreateUserRequest("USR_NO_LEAK", "No Leak User", secretPassword)
        )

        val userEntity = database.accountDao().findByLoginId("USR_NO_LEAK")!!
        val auditLogs = auditService.getRecentLogsForTarget(userEntity.id, 20)

        for (log in auditLogs) {
            assertFalse("Audit log metadata leaked plain password!", log.metadataJson.contains(secretPassword))
            assertFalse("Audit log beforeState leaked plain password!", log.beforeState?.contains(secretPassword) == true)
            assertFalse("Audit log afterState leaked plain password!", log.afterState?.contains(secretPassword) == true)
        }
    }

    @Test
    fun test13_pagination_and_search_for_agent_users() = runBlocking {
        // Create 3 users for Agent 1
        apiRouter.handleCreateUser(agent1Token, CreateUserRequest("USR_TEST_A", "Alice Alpha", "UserPassword@123"))
        apiRouter.handleCreateUser(agent1Token, CreateUserRequest("USR_TEST_B", "Bob Bravo", "UserPassword@123"))
        apiRouter.handleCreateUser(agent1Token, CreateUserRequest("USR_TEST_C", "Charlie Charlie", "UserPassword@123"))

        // Create 1 user for Agent 2
        apiRouter.handleCreateUser(agent2Token, CreateUserRequest("USR_TEST_D", "Dave Delta", "UserPassword@123"))

        // Agent 1 lists users: should see exactly 3, not Dave Delta
        val listRes = apiRouter.handleGetUsers(agent1Token, page = 1, limit = 10)
        assertTrue(listRes.success)
        assertEquals(3, listRes.data!!.totalCount)
        assertEquals(3, listRes.data!!.users.size)
        assertTrue(listRes.data!!.users.none { it.userId == "USR_TEST_D" })

        // Search for "Alice"
        val searchRes = apiRouter.handleGetUsers(agent1Token, search = "Alice")
        assertTrue(searchRes.success)
        assertEquals(1, searchRes.data!!.totalCount)
        assertEquals("USR_TEST_A", searchRes.data!!.users.first().userId)
    }

    @Test
    fun test14_admin_supervisory_access() = runBlocking {
        // Agent 1 creates user
        val userRes = apiRouter.handleCreateUser(
            agent1Token,
            CreateUserRequest("USR_SUPERVISORY", "Supervised Player", "UserPassword@123")
        )
        val userId = userRes.data!!.id

        // Admin can list all users across agents
        val adminList = apiRouter.handleAdminGetUsers(adminToken)
        assertTrue(adminList.success)
        assertTrue(adminList.data!!.users.any { it.userId == "USR_SUPERVISORY" })

        // Admin can view details
        val adminDetail = apiRouter.handleAdminGetUserDetails(adminToken, userId)
        assertTrue(adminDetail.success)
        assertEquals(agent1Id, adminDetail.data!!.agentId) // Ownership preserved!

        // Admin can suspend
        val adminSuspend = apiRouter.handleAdminSuspendUser(adminToken, userId)
        assertTrue(adminSuspend.success)
        val dbUser = database.accountDao().findById(userId)
        assertEquals(AccountStatus.SUSPENDED.name, dbUser!!.status)
    }

    @Test
    fun test15_user_first_login_password_change() = runBlocking {
        // Agent creates user
        apiRouter.handleCreateUser(
            agent1Token,
            CreateUserRequest("USR_PW_CHANGE", "Player Pass Change", "TempPass@123")
        )

        // Login with temp password
        val loginRes = authService.login(LoginRequest("USR_PW_CHANGE", "TempPass@123"))
        assertTrue(loginRes.success)
        assertTrue(loginRes.data!!.user.mustChangePassword)
        val userToken = "Bearer ${loginRes.data!!.accessToken}"

        // User changes password
        val changeRes = apiRouter.handleChangePassword(
            authHeader = userToken,
            request = ChangePasswordRequest(
                currentPassword = "TempPass@123",
                newPassword = "PermanentPassword@456"
            )
        )
        assertTrue(changeRes.success)

        // Verify mustChangePassword is now false
        val meRes = apiRouter.handleGetMe(userToken)
        assertTrue(meRes.success)
        val meDto = meRes.data as com.example.backend.model.UserProfileDto
        assertFalse(meDto.mustChangePassword)
    }
}
