package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.network.ApiConfig
import com.example.core.network.AppEnvironment
import com.example.core.network.MahakalRetrofitClient
import com.example.core.security.SecureTokenStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AndroidApiClientTest {

    private lateinit var context: Context
    private lateinit var tokenStorage: SecureTokenStorage

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        tokenStorage = SecureTokenStorage(context)
    }

    @Test
    fun testRetrofitClientCreationWithHttps() {
        val client = MahakalRetrofitClient.create(
            tokenStorage = tokenStorage,
            baseUrl = "https://mahakal-production.example.com/v1/"
        )
        assertNotNull("Retrofit API Service should be created successfully", client)
    }

    @Test
    fun testBaseUrlEnforcesHttpsOnly() {
        val baseUrl = ApiConfig.getBaseUrl()
        assertTrue("Base URL must use HTTPS", baseUrl.startsWith("https://"))
        assertTrue("No localhost in URL", !baseUrl.contains("localhost"))
        assertTrue("No loopback in URL", !baseUrl.contains("127.0.0.1") && !baseUrl.contains("10.0.2.2"))
    }

    @Test
    fun testNoBackendDatabaseSecretsInBuildConfig() {
        // Assert that backend database connection strings are never compiled into BuildConfig
        val buildConfigFields = com.example.BuildConfig::class.java.fields.map { it.name }
        assertTrue("DATABASE_URL must not be in BuildConfig", !buildConfigFields.contains("DATABASE_URL"))
        assertTrue("SESSION_SECRET must not be in BuildConfig", !buildConfigFields.contains("SESSION_SECRET"))
        assertTrue("TOKEN_SIGNING_SECRET must not be in BuildConfig", !buildConfigFields.contains("TOKEN_SIGNING_SECRET"))
        assertTrue("ENCRYPTION_KEY must not be in BuildConfig", !buildConfigFields.contains("ENCRYPTION_KEY"))
    }

    @Test
    fun testProductionApiUrlConfigured() {
        assertEquals("https://mahakal-qo14.onrender.com", ApiConfig.PRODUCTION_API_URL)
        assertTrue("Production URL must use HTTPS", ApiConfig.PRODUCTION_API_URL.startsWith("https://"))
        assertTrue("No localhost in production URL", !ApiConfig.PRODUCTION_API_URL.contains("localhost"))
        assertTrue("No 127.0.0.1 in production URL", !ApiConfig.PRODUCTION_API_URL.contains("127.0.0.1"))
        assertTrue("No emulator in production URL", !ApiConfig.PRODUCTION_API_URL.contains("10.0.2.2"))
    }

    @Test
    fun testProductionRetrofitClientUsesProductionUrl() {
        val client = MahakalRetrofitClient.create(
            tokenStorage = tokenStorage,
            baseUrl = ApiConfig.PRODUCTION_API_URL
        )
        assertNotNull("Production Retrofit client should be created", client)
    }

    @Test
    fun testEnvironmentMapping() {
        assertEquals(AppEnvironment.DEVELOPMENT, AppEnvironment.fromString("development"))
        assertEquals(AppEnvironment.STAGING, AppEnvironment.fromString("staging"))
        assertEquals(AppEnvironment.PRODUCTION, AppEnvironment.fromString("production"))
        assertEquals(AppEnvironment.DEVELOPMENT, AppEnvironment.fromString("unknown"))
    }
}
