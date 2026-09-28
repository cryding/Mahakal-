package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.backend.api.ServerApiRouter
import com.example.backend.audit.AuditService
import com.example.backend.database.MahakalServerDatabase
import com.example.backend.database.entity.OutboxEventEntity
import com.example.backend.security.RateLimiter
import com.example.backend.service.NotificationService
import com.example.backend.service.OutboxWorker
import com.example.backend.service.SecurityHardeningService
import com.example.backend.service.ServerAuthService
import com.example.core.network.ApiConfig
import com.example.core.network.AppEnvironment
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProductionInfrastructureTest {

    private lateinit var database: MahakalServerDatabase
    private lateinit var auditService: AuditService
    private lateinit var rateLimiter: RateLimiter
    private lateinit var notificationService: NotificationService
    private lateinit var authService: ServerAuthService
    private lateinit var hardeningService: SecurityHardeningService
    private lateinit var outboxWorker: OutboxWorker
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
        hardeningService = SecurityHardeningService(database, auditService, rateLimiter, authService)
        outboxWorker = OutboxWorker(database, notificationService)

        apiRouter = ServerApiRouter(
            authService = authService,
            auditService = auditService,
            securityHardeningService = hardeningService
        )
    }

    @After
    fun tearDown() {
        outboxWorker.stop()
        database.close()
    }

    @Test
    fun testHealthCheckEndpointReportsHealthy() = runBlocking {
        val health = apiRouter.handleHealthCheck()
        assertNotNull(health)
        assertEquals("OK", health.status)
        assertEquals("UP", health.database)
        assertTrue(health.checks.containsKey("database"))
    }

    @Test
    fun testReadinessCheckEndpointReportsReady() = runBlocking {
        val readiness = apiRouter.handleReadinessCheck()
        assertNotNull(readiness)
        assertTrue(readiness.ready)
        assertEquals("READY", readiness.database)
        assertEquals("READY", readiness.outboxWorker)
    }

    @Test
    fun testEnvironmentIsolationConfig() {
        val env = ApiConfig.currentEnvironment
        assertNotNull(env)
        val baseUrl = ApiConfig.getBaseUrl()
        assertTrue(baseUrl.startsWith("https://"))
        assertTrue(baseUrl.contains("mahakal.internal"))

        val dbName = ApiConfig.getDatabaseName()
        assertTrue(dbName.endsWith(".db"))
    }

    @Test
    fun testSecretsNeverExposedInClient() {
        // Strict security assertion: Client codebase must never expose private backend credentials
        val apiConfigFields = ApiConfig::class.java.declaredFields.map { it.name }
        assertTrue("DATABASE_URL must not be exposed in ApiConfig", !apiConfigFields.contains("databaseUrl"))
        assertTrue("SESSION_SECRET must not be exposed in ApiConfig", !apiConfigFields.contains("sessionSecret"))
        assertTrue("TOKEN_SIGNING_SECRET must not be exposed in ApiConfig", !apiConfigFields.contains("tokenSigningSecret"))
        assertTrue("ENCRYPTION_KEY must not be exposed in ApiConfig", !apiConfigFields.contains("encryptionKey"))

        val baseUrl = ApiConfig.getBaseUrl()
        assertTrue("Base URL must be HTTPS", baseUrl.startsWith("https://"))
        assertTrue("Base URL must not use insecure localhost or loopback", !baseUrl.contains("localhost") && !baseUrl.contains("127.0.0.1") && !baseUrl.contains("10.0.2.2"))
    }

    @Test
    fun testOutboxWorkerBatchProcessingAndStatus() = runBlocking {
        val outboxDao = database.outboxDao()
        val now = System.currentTimeMillis()

        // Insert mock outbox events
        val event1 = OutboxEventEntity(
            eventId = UUID.randomUUID().toString(),
            eventType = "TEST_EVENT",
            aggregateType = "SYSTEM",
            aggregateId = "SYS-1",
            payload = "{\"test\":true}",
            status = "PENDING",
            attemptCount = 0,
            createdAt = now - 1000,
            availableAt = now - 500,
            processedAt = null,
            lastErrorCode = null,
            correlationId = "corr-1"
        )
        outboxDao.insertEvent(event1)

        val pendingBefore = outboxDao.countPendingEvents(now)
        assertEquals(1, pendingBefore)

        val processed = outboxWorker.processOutboxBatch()
        assertEquals(1, processed)

        val status = outboxWorker.getStatus()
        assertNotNull(status)
        assertEquals(1L, status.totalProcessed)
        assertEquals(0, status.pendingOutboxCount)
    }
}
