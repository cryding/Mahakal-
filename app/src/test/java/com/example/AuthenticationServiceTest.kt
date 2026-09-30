package com.example

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.backend.api.ServerApiRouter
import com.example.backend.audit.AuditService
import com.example.backend.database.MahakalServerDatabase
import com.example.backend.model.ChangePasswordRequest
import com.example.backend.model.LoginRequest
import com.example.backend.rbac.AccountRole
import com.example.backend.rbac.AccountStatus
import com.example.backend.security.RateLimiter
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
class AuthenticationServiceTest {

    private lateinit var database: MahakalServerDatabase
    private lateinit var auditService: AuditService
    private lateinit var rateLimiter: RateLimiter
    private lateinit var authService: ServerAuthService
    private lateinit var apiRouter: ServerApiRouter

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, MahakalServerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        auditService = AuditService(database.auditLogDao())
        rateLimiter = RateLimiter(database.rateLimitDao())
        authService = ServerAuthService(database, auditService, rateLimiter)
        apiRouter = ServerApiRouter(authService, auditService)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun test1_valid_admin_login() = runBlocking {
        authService.bootstrapInitialAdmin("master_admin", "AdminPass@999", "Master Admin")

        val response = authService.login(LoginRequest("master_admin", "AdminPass@999"))
        assertTrue(response.success)
        assertEquals(200, response.statusCode)
        assertNotNull(response.data)
        assertEquals("ADMIN", response.data?.user?.role)
        assertTrue(response.data?.accessToken?.startsWith("mhk_acc_") == true)
    }

    @Test
    fun test2_valid_agent_login() = runBlocking {
        authService.provisionAccountForTesting("test_agent", "AgentPass@123", AccountRole.AGENT)

        val response = authService.login(LoginRequest("test_agent", "AgentPass@123"))
        assertTrue(response.success)
        assertEquals(200, response.statusCode)
        assertEquals("AGENT", response.data?.user?.role)
    }

    @Test
    fun test3_valid_user_login() = runBlocking {
        authService.provisionAccountForTesting("test_user", "UserPass@123", AccountRole.USER)

        val response = authService.login(LoginRequest("test_user", "UserPass@123"))
        assertTrue(response.success)
        assertEquals(200, response.statusCode)
        assertEquals("USER", response.data?.user?.role)
    }

    @Test
    fun test4_invalid_password_returns_generic_401() = runBlocking {
        authService.provisionAccountForTesting("test_user", "UserPass@123", AccountRole.USER)

        val response = authService.login(LoginRequest("test_user", "WrongPassword!"))
        assertFalse(response.success)
        assertEquals(401, response.statusCode)
        assertEquals("INVALID_CREDENTIALS", response.errorCode)
        assertEquals("Invalid ID or password.", response.message)
    }

    @Test
    fun test5_invalid_login_id_returns_generic_401() = runBlocking {
        val response = authService.login(LoginRequest("non_existent_account", "AnyPassword!"))
        assertFalse(response.success)
        assertEquals(401, response.statusCode)
        assertEquals("INVALID_CREDENTIALS", response.errorCode)
        assertEquals("Invalid ID or password.", response.message)
    }

    @Test
    fun test6_suspended_account_rejected_with_403() = runBlocking {
        authService.provisionAccountForTesting(
            "suspended_user",
            "UserPass@123",
            AccountRole.USER,
            status = AccountStatus.SUSPENDED
        )

        val response = authService.login(LoginRequest("suspended_user", "UserPass@123"))
        assertFalse(response.success)
        assertEquals(403, response.statusCode)
        assertEquals("ACCOUNT_SUSPENDED", response.errorCode)
    }

    @Test
    fun test7_disabled_account_rejected_with_403() = runBlocking {
        authService.provisionAccountForTesting(
            "disabled_user",
            "UserPass@123",
            AccountRole.USER,
            status = AccountStatus.DISABLED
        )

        val response = authService.login(LoginRequest("disabled_user", "UserPass@123"))
        assertFalse(response.success)
        assertEquals(403, response.statusCode)
        assertEquals("ACCOUNT_DISABLED", response.errorCode)
    }

    @Test
    fun test8_expired_session_returns_null() = runBlocking {
        authService.provisionAccountForTesting("exp_user", "UserPass@123", AccountRole.USER)
        val loginRes = authService.login(LoginRequest("exp_user", "UserPass@123"))
        val token = loginRes.data!!.accessToken

        // Mutate session expiration in database
        val session = database.sessionDao().findActiveByAccessToken(token)!!
        database.sessionDao().insert(session.copy(expiresAt = System.currentTimeMillis() - 1000L))

        val context = authService.validateSession(token)
        assertNull(context)
    }

    @Test
    fun test9_revoked_session_returns_null() = runBlocking {
        authService.provisionAccountForTesting("rev_user", "UserPass@123", AccountRole.USER)
        val loginRes = authService.login(LoginRequest("rev_user", "UserPass@123"))
        val token = loginRes.data!!.accessToken

        database.sessionDao().revokeByAccessToken(token)

        val context = authService.validateSession(token)
        assertNull(context)
    }

    @Test
    fun test10_logout_invalidates_session() = runBlocking {
        authService.provisionAccountForTesting("logout_user", "UserPass@123", AccountRole.USER)
        val loginRes = authService.login(LoginRequest("logout_user", "UserPass@123"))
        val token = loginRes.data!!.accessToken

        val logoutRes = authService.logout(token)
        assertTrue(logoutRes.success)

        val contextAfterLogout = authService.validateSession(token)
        assertNull(contextAfterLogout)
    }

    @Test
    fun test11_password_change_flow() = runBlocking {
        authService.provisionAccountForTesting("pwd_user", "OldPassword@123", AccountRole.USER)
        val loginRes = authService.login(LoginRequest("pwd_user", "OldPassword@123"))
        val token = loginRes.data!!.accessToken

        val changeRes = authService.changePassword(
            accessToken = token,
            request = ChangePasswordRequest(
                currentPassword = "OldPassword@123",
                newPassword = "NewComplexPassword@456"
            )
        )
        assertTrue(changeRes.success)

        // Login with old password fails
        val oldLogin = authService.login(LoginRequest("pwd_user", "OldPassword@123"))
        assertFalse(oldLogin.success)

        // Login with new password succeeds
        val newLogin = authService.login(LoginRequest("pwd_user", "NewComplexPassword@456"))
        assertTrue(newLogin.success)
    }
}
