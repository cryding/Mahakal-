package com.example

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.backend.api.ServerApiRouter
import com.example.backend.audit.AuditActions
import com.example.backend.audit.AuditService
import com.example.backend.database.MahakalServerDatabase
import com.example.backend.model.CreateGameRequest
import com.example.backend.model.FinalizeResultRequest
import com.example.backend.model.GameEntryRequest
import com.example.backend.model.GameOptionCreateRequest
import com.example.backend.model.LoginRequest
import com.example.backend.model.NotificationSeverity
import com.example.backend.model.NotificationStatus
import com.example.backend.model.NotificationType
import com.example.backend.model.TransferCoinsRequest
import com.example.backend.model.UpdateNotificationPreferenceRequest
import com.example.backend.rbac.AccountRole
import com.example.backend.security.RateLimiter
import com.example.backend.service.AdminAgentService
import com.example.backend.service.AgentUserService
import com.example.backend.service.GameEngineService
import com.example.backend.service.NotificationService
import com.example.backend.service.ServerAuthService
import com.example.backend.service.WalletTransactionService
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
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NotificationSystemTest {

    private lateinit var database: MahakalServerDatabase
    private lateinit var auditService: AuditService
    private lateinit var rateLimiter: RateLimiter
    private lateinit var notificationService: NotificationService
    private lateinit var authService: ServerAuthService
    private lateinit var adminAgentService: AdminAgentService
    private lateinit var agentUserService: AgentUserService
    private lateinit var walletService: WalletTransactionService
    private lateinit var gameEngineService: GameEngineService
    private lateinit var apiRouter: ServerApiRouter

    private lateinit var adminToken: String
    private lateinit var adminId: String
    private lateinit var agentToken: String
    private lateinit var agentId: String
    private lateinit var userToken: String
    private lateinit var userId: String

    @Before
    fun setup() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
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
            gameEngineService = GameEngineService(database, auditService, rateLimiter, walletService, notificationService)

            apiRouter = ServerApiRouter(
                authService = authService,
                auditService = auditService,
                adminAgentService = adminAgentService,
                agentUserService = agentUserService,
                walletTransactionService = walletService,
                gameEngineService = gameEngineService,
                notificationService = notificationService
            )

            // 1. Admin Bootstrap
            authService.bootstrapInitialAdmin("admin_root", "AdminSec@123", "Root Admin")
            val adminLogin = authService.login(LoginRequest("admin_root", "AdminSec@123"))
            adminToken = "Bearer ${adminLogin.data!!.accessToken}"
            adminId = adminLogin.data!!.user.id
            walletService.getOrCreateWallet(adminId, AccountRole.ADMIN.name, 50_000L)

            // 2. Provision Agent
            val agent = authService.provisionAccountForTesting(
                loginId = "AGENT_NOTIF_01",
                password = "AgentPass@123",
                role = AccountRole.AGENT,
                fullName = "Agent Notification"
            )
            agentId = agent.id
            val agentLogin = authService.login(LoginRequest("AGENT_NOTIF_01", "AgentPass@123"))
            agentToken = "Bearer ${agentLogin.data!!.accessToken}"
            walletService.getOrCreateWallet(agentId, AccountRole.AGENT.name, 10_000L)

            // 3. Provision User
            val user = authService.provisionAccountForTesting(
                loginId = "PLAYER_NOTIF_01",
                password = "PlayerPass@123",
                role = AccountRole.USER,
                fullName = "Player Notification"
            )
            userId = user.id
            val userLogin = authService.login(LoginRequest("PLAYER_NOTIF_01", "PlayerPass@123"))
            userToken = "Bearer ${userLogin.data!!.accessToken}"
            walletService.getOrCreateWallet(userId, AccountRole.USER.name, 1_000L)
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testWalletTransactionNotificationDelivery() = runBlocking {
        // Admin transfers 500 virtual coins to Agent
        val transferReq = TransferCoinsRequest(
            destinationAccountId = agentId,
            amount = 500L,
            reason = "Virtual coins allocation for testing"
        )
        val transferResp = apiRouter.handleTransferCoins(
            authHeader = adminToken,
            request = transferReq,
            idempotencyKey = "tx_notif_01"
        )
        assertTrue("Transfer coins should succeed: ${transferResp.message}", transferResp.success)

        // Agent should receive a notification
        val agentNotifsResp = apiRouter.handleGetNotifications(
            authHeader = agentToken,
            page = 1,
            limit = 10
        )
        assertTrue(agentNotifsResp.success)
        assertNotNull(agentNotifsResp.data)
        assertEquals(1, agentNotifsResp.data!!.notifications.size)
        val notif = agentNotifsResp.data!!.notifications.first()
        assertEquals(NotificationType.VIRTUAL_COIN_RECEIVED, notif.type)
        assertEquals("Virtual Coins Received", notif.title)
        assertTrue(notif.body.contains("500 virtual coins"))
        assertEquals(NotificationStatus.UNREAD, notif.status)
    }

    @Test
    fun testGameResultFinalizationAndWinnerNotification() = runBlocking {
        // Admin creates a game with future startTime so it starts in SCHEDULED state
        val now = System.currentTimeMillis()
        val gameResp = apiRouter.handleCreateGame(
            authHeader = adminToken,
            request = CreateGameRequest(
                title = "Mahakal T20 Championship",
                gameType = "CRICKET_PREDICTION",
                description = "Predict tournament winner",
                minCoins = 50L,
                maxCoins = 500L,
                startTime = now + 60000L,
                entryDeadline = now + 120000L,
                resultTime = now + 180000L,
                rewardMultiplier = 2.0,
                options = listOf(
                    GameOptionCreateRequest("ROYAL", "Team Royal"),
                    GameOptionCreateRequest("TITAN", "Team Titan")
                )
            )
        )
        assertTrue("Game creation should succeed: ${gameResp.message}", gameResp.success)
        val gameId = gameResp.data!!.gameId
        val royalOptionId = gameResp.data!!.options.first { it.optionCode == "ROYAL" }.optionId

        // Open game (SCHEDULED -> OPEN)
        val openResp = apiRouter.handleOpenGame(adminToken, gameId)
        assertTrue("Game open should succeed: ${openResp.message}", openResp.success)

        // Player places an entry on Team Royal
        val entryResp = apiRouter.handleSubmitGameEntry(
            authHeader = userToken,
            gameId = gameId,
            request = GameEntryRequest(
                selectedOptionId = royalOptionId,
                virtualCoinAmount = 100L
            ),
            idempotencyKey = "entry_player_01"
        )
        assertTrue("Entry placement should succeed: ${entryResp.message}", entryResp.success)

        // Verify entry confirmed notification
        val userNotifsAfterEntry = apiRouter.handleGetNotifications(
            authHeader = userToken,
            page = 1,
            limit = 10
        )
        val entryNotif = userNotifsAfterEntry.data!!.notifications.find { it.type == NotificationType.ENTRY_CONFIRMED }
        assertNotNull("Player must receive entry confirmation notification", entryNotif)
        assertTrue(entryNotif!!.body.contains("100 virtual coins"))

        // Admin closes the game
        val closeResp = apiRouter.handleCloseGame(adminToken, gameId)
        assertTrue("Game close should succeed", closeResp.success)

        // Admin finalizes result with Team Royal as winner
        val finalizeResp = apiRouter.handleFinalizeGameResult(
            authHeader = adminToken,
            gameId = gameId,
            request = FinalizeResultRequest(
                winningOptionId = royalOptionId,
                reason = "Match ended, Royal won"
            ),
            idempotencyKey = "finalize_01"
        )
        assertTrue("Finalization should succeed: ${finalizeResp.message}", finalizeResp.success)

        // Player must receive result finalized notification with win details
        val userNotifsAfterResult = apiRouter.handleGetNotifications(
            authHeader = userToken,
            page = 1,
            limit = 10
        )
        val resultNotif = userNotifsAfterResult.data!!.notifications.find { it.type == NotificationType.RESULT_FINALIZED }
        assertNotNull("Player must receive game result notification", resultNotif)
        assertTrue(resultNotif!!.title.contains("Prediction Won"))
        assertTrue(resultNotif.body.contains("200 virtual coins")) // 100 * 2.0
    }

    @Test
    fun testNotificationPreferencesEnforcement() = runBlocking {
        // User opts out of game notifications
        val prefUpdateResp = apiRouter.handleUpdateNotificationPreferences(
            authHeader = userToken,
            request = UpdateNotificationPreferenceRequest(
                gameNotificationsEnabled = false,
                resultNotificationsEnabled = false,
                walletNotificationsEnabled = true
            )
        )
        assertTrue(prefUpdateResp.success)
        assertFalse(prefUpdateResp.data!!.gameNotificationsEnabled)

        // Admin creates a game
        val now = System.currentTimeMillis()
        apiRouter.handleCreateGame(
            authHeader = adminToken,
            request = CreateGameRequest(
                title = "Silenced Game",
                gameType = "CRICKET_PREDICTION",
                description = "Game should not notify opted-out player",
                minCoins = 10L,
                maxCoins = 100L,
                startTime = now - 1000L,
                entryDeadline = now + 60000L,
                resultTime = now + 120000L,
                rewardMultiplier = 1.8,
                options = listOf(GameOptionCreateRequest("A", "A"), GameOptionCreateRequest("B", "B"))
            )
        )

        // User should NOT receive GAME_OPENED notification
        val userNotifs = apiRouter.handleGetNotifications(
            authHeader = userToken,
            page = 1,
            limit = 10,
            type = NotificationType.GAME_OPENED
        )
        assertTrue(userNotifs.success)
        assertEquals(0, userNotifs.data!!.notifications.size)
    }

    @Test
    fun testSecurityNotificationsAreMandatoryAndCannotBeDisabled() = runBlocking {
        // Attempt to update preferences - security notifications remain forced TRUE
        val prefResp = apiRouter.handleGetNotificationPreferences(authHeader = userToken)
        assertTrue(prefResp.success)
        assertTrue("Security notifications must always be true", prefResp.data!!.securityNotificationsEnabled)

        // Change password triggers CRITICAL notification
        notificationService.onPasswordChanged(userId)

        val notifsResp = apiRouter.handleGetNotifications(
            authHeader = userToken,
            page = 1,
            limit = 10,
            type = NotificationType.PASSWORD_CHANGED
        )
        assertTrue(notifsResp.success)
        assertEquals(1, notifsResp.data!!.notifications.size)
        val notif = notifsResp.data!!.notifications.first()
        assertEquals(NotificationSeverity.CRITICAL, notif.severity)
        assertEquals("Security Alert: Password Changed", notif.title)
    }

    @Test
    fun testStrictRecipientOwnershipPreventsIDOR() = runBlocking {
        // Create notification for User
        notificationService.onPasswordChanged(userId)

        val userNotifs = apiRouter.handleGetNotifications(
            authHeader = userToken,
            page = 1,
            limit = 10
        )
        val userNotifId = userNotifs.data!!.notifications.first().notificationId

        // Agent attempts to read User's notification (IDOR attempt)
        val agentIdorResp = apiRouter.handleGetNotificationDetails(
            authHeader = agentToken,
            notificationId = userNotifId
        )
        assertFalse("Accessing another user's notification must fail", agentIdorResp.success)
        assertEquals(403, agentIdorResp.statusCode)
        assertEquals("FORBIDDEN", agentIdorResp.errorCode)

        // Agent attempts to mark User's notification as read
        val markReadResp = apiRouter.handleMarkNotificationRead(
            authHeader = agentToken,
            notificationId = userNotifId
        )
        assertFalse("Unauthorized mark-as-read must fail", markReadResp.success)
        assertEquals(403, markReadResp.statusCode)

        // Verify audit log recorded PERMISSION_DENIED
        val auditLogs = database.auditLogDao().getRecentLogs(10)
        val idorLog = auditLogs.find { it.action == AuditActions.PERMISSION_DENIED && it.targetId == userNotifId }
        assertNotNull("IDOR attempt must be logged in security audit", idorLog)
    }

    @Test
    fun testMarkAsReadAndUnreadCount() = runBlocking {
        // Trigger 2 notifications
        notificationService.onPasswordChanged(userId)
        notificationService.onAccountStatusChanged(userId, "ACTIVE", "SUSPENDED", adminId)

        val countResp1 = apiRouter.handleGetUnreadNotificationCount(authHeader = userToken)
        assertEquals(2, countResp1.data!!.unreadCount)

        val notifs = apiRouter.handleGetNotifications(authHeader = userToken, page = 1, limit = 10)
        val firstId = notifs.data!!.notifications.first().notificationId

        // Mark first as read
        val markResp = apiRouter.handleMarkNotificationRead(authHeader = userToken, notificationId = firstId)
        assertTrue(markResp.success)

        val countResp2 = apiRouter.handleGetUnreadNotificationCount(authHeader = userToken)
        assertEquals(1, countResp2.data!!.unreadCount)

        // Mark all as read
        val markAllResp = apiRouter.handleMarkAllNotificationsRead(authHeader = userToken)
        assertTrue(markAllResp.success)

        val countResp3 = apiRouter.handleGetUnreadNotificationCount(authHeader = userToken)
        assertEquals(0, countResp3.data!!.unreadCount)
    }

    @Test
    fun testIdempotentNotificationDeduplication() = runBlocking {
        // Trigger same event twice with same correlation ID
        val correlationId = "test_idempotent_event_99"
        notificationService.onAdminOperationalAlert(
            title = "High CPU Load",
            body = "Backend cluster CPU exceeded 90%",
            severity = NotificationSeverity.WARNING,
            correlationId = correlationId
        )
        // Repeat
        notificationService.onAdminOperationalAlert(
            title = "High CPU Load",
            body = "Backend cluster CPU exceeded 90%",
            severity = NotificationSeverity.WARNING,
            correlationId = correlationId
        )

        // Admin should only have ONE notification for this event
        val adminNotifs = apiRouter.handleGetNotifications(
            authHeader = adminToken,
            page = 1,
            limit = 10,
            type = NotificationType.ADMIN_OPERATIONAL_ALERT
        )
        assertTrue(adminNotifs.success)
        assertEquals(1, adminNotifs.data!!.notifications.size)
    }

    @Test
    fun testArchiveNotification() = runBlocking {
        notificationService.onPasswordChanged(userId)

        val notifs = apiRouter.handleGetNotifications(authHeader = userToken, page = 1, limit = 10)
        val notifId = notifs.data!!.notifications.first().notificationId

        // Archive notification
        val archiveResp = apiRouter.handleArchiveNotification(
            authHeader = userToken,
            notificationId = notifId
        )
        assertTrue(archiveResp.success)

        // Active list should not return it
        val activeNotifs = apiRouter.handleGetNotifications(
            authHeader = userToken,
            page = 1,
            limit = 10,
            status = NotificationStatus.UNREAD
        )
        assertEquals(0, activeNotifs.data!!.notifications.size)

        // Archived filter should return it
        val archivedNotifs = apiRouter.handleGetNotifications(
            authHeader = userToken,
            page = 1,
            limit = 10,
            status = NotificationStatus.ARCHIVED
        )
        assertEquals(1, archivedNotifs.data!!.notifications.size)
    }
}
