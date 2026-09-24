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
class SecurityBruteForceTest {

    private lateinit var database: MahakalServerDatabase
    private lateinit var auditService: AuditService
    private lateinit var rateLimiter: RateLimiter
    private lateinit var authService: ServerAuthService
    private lateinit var apiRouter: ServerApiRouter

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

            authService.provisionAccountForTesting(
                "target_account",
                "TargetPass@123",
                AccountRole.USER
            )
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun brute_force_failed_attempts_trigger_lockout() = runBlocking {
        // Submit 4 incorrect passwords (each returns 401)
        for (i in 1..4) {
            val res = authService.login(LoginRequest("target_account", "WrongPass_$i"))
            assertFalse(res.success)
            assertEquals(401, res.statusCode)
        }

        // 5th failed attempt triggers rate limit lockout
        val fifthRes = authService.login(LoginRequest("target_account", "WrongPass_5"))
        assertFalse(fifthRes.success)

        // 6th attempt is blocked by rate limiter with 429 Too Many Requests
        val sixthRes = authService.login(LoginRequest("target_account", "TargetPass@123"))
        assertFalse(sixthRes.success)
        assertEquals(429, sixthRes.statusCode)
        assertEquals("RATE_LIMIT_EXCEEDED", sixthRes.errorCode)
    }

    @Test
    fun empty_or_whitespace_credentials_rejected_with_422() = runBlocking {
        val res = authService.login(LoginRequest("   ", "   "))
        assertFalse(res.success)
        assertEquals(422, res.statusCode)
        assertEquals("VALIDATION_ERROR", res.errorCode)
    }

    @Test
    fun malformed_bearer_token_rejected_with_401() = runBlocking {
        val res = apiRouter.handleAdminProbe("Bearer invalid_corrupted_token_xyz")
        assertFalse(res.success)
        assertEquals(401, res.statusCode)
        assertEquals("UNAUTHORIZED", res.errorCode)
    }
}
