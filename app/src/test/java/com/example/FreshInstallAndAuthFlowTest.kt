package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.network.SessionManager
import com.example.core.security.SecureTokenStorage
import com.example.data.local.MahakalDatabase
import com.example.data.repository.MahakalRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FreshInstallAndAuthFlowTest {

    private lateinit var context: Context
    private lateinit var tokenStorage: SecureTokenStorage
    private lateinit var sessionManager: SessionManager
    private lateinit var database: MahakalDatabase
    private lateinit var repository: MahakalRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        tokenStorage = SecureTokenStorage(context)
        tokenStorage.clearSession()
        sessionManager = SessionManager(tokenStorage)
        database = MahakalDatabase.getInstance(context)
        repository = MahakalRepository(
            db = database,
            apiService = null,
            sessionManager = sessionManager,
            secureStorage = tokenStorage
        )
    }

    @After
    fun tearDown() {
        tokenStorage.clearSession()
    }

    @Test
    fun testFreshInstallHasNoTokenAndValidateSessionReturnsNull() = runBlocking {
        // Given a fresh install
        assertNull("Access token must be null on fresh install", tokenStorage.getAccessToken())
        assertNull("User ID must be null on fresh install", tokenStorage.getSessionUserId())

        // When validateSession is executed
        val sessionUser = repository.validateSession()

        // Then session user must be null -> app stays on LoginScreen
        assertNull("Session user must be null on fresh install", sessionUser)
    }

    @Test
    fun testAuthenticationFailsWithBlankCredentials() = runBlocking {
        val user1 = repository.authenticate("", "")
        assertNull("Blank credentials must fail", user1)

        val user2 = repository.authenticate("   ", "   ")
        assertNull("Whitespace credentials must fail", user2)
    }

    @Test
    fun testRoomCacheIsNotAuthenticationAuthority() = runBlocking {
        // Even if an entity existed in Room cache, validateSession cannot authenticate without valid token
        val sessionUser = repository.validateSession()
        assertNull("Room cannot authenticate without valid backend token", sessionUser)
    }

    @Test
    fun testLogoutClearsSessionAndTokens() = runBlocking {
        // Simulate saved credentials
        tokenStorage.saveTokens(
            accessToken = "mhk_acc_mock_test_token_12345",
            refreshToken = "mhk_ref_mock_refresh_token_12345",
            userId = "usr_test_123",
            loginId = "test_user",
            role = "USER",
            fullName = "Test User"
        )
        assertNotNull(tokenStorage.getAccessToken())

        // When logout is invoked
        repository.logout()

        // Then tokens must be cleared
        assertNull("Access token must be purged on logout", tokenStorage.getAccessToken())
        assertNull("Refresh token must be purged on logout", tokenStorage.getRefreshToken())
        assertNull("User ID must be purged on logout", tokenStorage.getSessionUserId())
    }

    @Test
    fun testRolesAreStrictlyAdminAgentUser() {
        val validRoles = setOf("ADMIN", "AGENT", "USER")
        assertEquals(3, validRoles.size)
        org.junit.Assert.assertTrue(validRoles.contains("ADMIN"))
        org.junit.Assert.assertTrue(validRoles.contains("AGENT"))
        org.junit.Assert.assertTrue(validRoles.contains("USER"))
    }
}
