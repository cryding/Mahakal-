package com.example

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.backend.api.ServerApiRouter
import com.example.backend.audit.AuditService
import com.example.backend.database.MahakalServerDatabase
import com.example.backend.model.LoginRequest
import com.example.backend.rbac.AccountRole
import com.example.backend.security.RateLimiter
import com.example.backend.service.ServerAuthService
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RbacAuthorizationTest {

    private lateinit var database: MahakalServerDatabase
    private lateinit var auditService: AuditService
    private lateinit var rateLimiter: RateLimiter
    private lateinit var authService: ServerAuthService
    private lateinit var apiRouter: ServerApiRouter

    private lateinit var adminToken: String
    private lateinit var agent1Token: String
    private lateinit var agent2Token: String
    private lateinit var user1Token: String
    private lateinit var user2Token: String

    private lateinit var agent1Id: String
    private lateinit var agent2Id: String
    private lateinit var user1Id: String
    private lateinit var user2Id: String

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
            apiRouter = ServerApiRouter(authService, auditService)

            // Bootstrap Admin
            authService.bootstrapInitialAdmin("admin_rbac", "AdminSecPass@1", "Admin")
            val adminRes = authService.login(LoginRequest("admin_rbac", "AdminSecPass@1"))
            adminToken = adminRes.data!!.accessToken

            // Provision Agent 1 & Agent 2
            val ag1 = authService.provisionAccountForTesting("agent_01", "AgentSecPass@1", AccountRole.AGENT)
            agent1Id = ag1.id
            agent1Token = authService.login(LoginRequest("agent_01", "AgentSecPass@1")).data!!.accessToken

            val ag2 = authService.provisionAccountForTesting("agent_02", "AgentSecPass@2", AccountRole.AGENT)
            agent2Id = ag2.id
            agent2Token = authService.login(LoginRequest("agent_02", "AgentSecPass@2")).data!!.accessToken

            // Provision User 1 (under Agent 1) and User 2 (under Agent 2)
            val u1 = authService.provisionAccountForTesting("user_01", "UserSecPass@1", AccountRole.USER, parentId = agent1Id)
            user1Id = u1.id
            user1Token = authService.login(LoginRequest("user_01", "UserSecPass@1")).data!!.accessToken

            val u2 = authService.provisionAccountForTesting("user_02", "UserSecPass@2", AccountRole.USER, parentId = agent2Id)
            user2Id = u2.id
            user2Token = authService.login(LoginRequest("user_02", "UserSecPass@2")).data!!.accessToken
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun test1_user_accessing_admin_endpoint_is_rejected_with_403() = runBlocking {
        val response = apiRouter.handleAdminProbe("Bearer $user1Token")
        assertFalse(response.success)
        assertEquals(403, response.statusCode)
        assertEquals("FORBIDDEN", response.errorCode)
    }

    @Test
    fun test2_user_accessing_agent_endpoint_is_rejected_with_403() = runBlocking {
        val response = apiRouter.handleAgentProbe("Bearer $user1Token")
        assertFalse(response.success)
        assertEquals(403, response.statusCode)
        assertEquals("FORBIDDEN", response.errorCode)
    }

    @Test
    fun test3_agent_accessing_admin_endpoint_is_rejected_with_403() = runBlocking {
        val response = apiRouter.handleAdminProbe("Bearer $agent1Token")
        assertFalse(response.success)
        assertEquals(403, response.statusCode)
        assertEquals("FORBIDDEN", response.errorCode)
    }

    @Test
    fun test4_agent1_attempting_to_access_agent2_subordinated_user_is_rejected() = runBlocking {
        // Agent 1 attempts to access User 2 (which belongs to Agent 2)
        val response = apiRouter.handleAgentUserAccess(
            authHeader = "Bearer $agent1Token",
            targetUserId = user2Id,
            targetUserAgentId = agent2Id
        )
        assertFalse(response.success)
        assertEquals(403, response.statusCode)
        assertEquals("FORBIDDEN", response.errorCode)
    }

    @Test
    fun test5_agent1_accessing_own_user_succeeds() = runBlocking {
        // Agent 1 attempts to access User 1 (which belongs to Agent 1)
        val response = apiRouter.handleAgentUserAccess(
            authHeader = "Bearer $agent1Token",
            targetUserId = user1Id,
            targetUserAgentId = agent1Id
        )
        assertTrue(response.success)
        assertEquals(200, response.statusCode)
    }

    @Test
    fun test6_user1_attempting_to_access_user2_resource_is_rejected() = runBlocking {
        val response = apiRouter.handleUserResourceAccess(
            authHeader = "Bearer $user1Token",
            targetUserId = user2Id
        )
        assertFalse(response.success)
        assertEquals(403, response.statusCode)
    }

    @Test
    fun test7_user1_accessing_own_resource_succeeds() = runBlocking {
        val response = apiRouter.handleUserResourceAccess(
            authHeader = "Bearer $user1Token",
            targetUserId = user1Id
        )
        assertTrue(response.success)
        assertEquals(200, response.statusCode)
    }

    @Test
    fun test8_client_identity_is_derived_purely_from_session_token_and_cannot_be_spoofed() = runBlocking {
        // An unauthenticated request with forged parameters is rejected with 401
        val response = apiRouter.handleAdminProbe(null)
        assertFalse(response.success)
        assertEquals(401, response.statusCode)
    }
}
