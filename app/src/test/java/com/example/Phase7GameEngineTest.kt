package com.example

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.backend.api.ServerApiRouter
import com.example.backend.audit.AuditService
import com.example.backend.database.MahakalServerDatabase
import com.example.backend.model.CancelGameRequest
import com.example.backend.model.CreateGameRequest
import com.example.backend.model.EntryStatus
import com.example.backend.model.FinalizeResultRequest
import com.example.backend.model.GameEntryRequest
import com.example.backend.model.GameOptionCreateRequest
import com.example.backend.model.GameStatus
import com.example.backend.model.LoginRequest
import com.example.backend.rbac.AccountRole
import com.example.backend.security.RateLimiter
import com.example.backend.service.AdminAgentService
import com.example.backend.service.AgentUserService
import com.example.backend.service.GameEngineService
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
@Config(sdk = [36])
class Phase7GameEngineTest {

    private lateinit var database: MahakalServerDatabase
    private lateinit var auditService: AuditService
    private lateinit var rateLimiter: RateLimiter
    private lateinit var authService: ServerAuthService
    private lateinit var adminAgentService: AdminAgentService
    private lateinit var agentUserService: AgentUserService
    private lateinit var walletService: WalletTransactionService
    private lateinit var gameEngineService: GameEngineService
    private lateinit var apiRouter: ServerApiRouter

    private lateinit var adminToken: String
    private lateinit var adminId: String

    private lateinit var userToken: String
    private lateinit var userId: String

    private lateinit var user2Token: String
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
            adminAgentService = AdminAgentService(database, auditService, rateLimiter)
            agentUserService = AgentUserService(database, auditService, rateLimiter)
            walletService = WalletTransactionService(database, auditService, rateLimiter)
            gameEngineService = GameEngineService(database, auditService, rateLimiter, walletService)

            apiRouter = ServerApiRouter(
                authService = authService,
                auditService = auditService,
                adminAgentService = adminAgentService,
                agentUserService = agentUserService,
                walletTransactionService = walletService,
                gameEngineService = gameEngineService
            )

            // 1. Admin Bootstrap
            authService.bootstrapInitialAdmin("admin_root", "AdminSec@123", "Root Admin")
            val adminLogin = authService.login(LoginRequest("admin_root", "AdminSec@123"))
            adminToken = "Bearer ${adminLogin.data!!.accessToken}"
            adminId = adminLogin.data!!.user.id
            walletService.getOrCreateWallet(adminId, AccountRole.ADMIN.name, 100_000L)

            // 2. Provision User 1 with 1,000 coins
            val user1 = authService.provisionAccountForTesting(
                loginId = "PLAYER_ONE",
                password = "PlayerPass@123",
                role = AccountRole.USER,
                fullName = "Player One"
            )
            userId = user1.id
            val u1Login = authService.login(LoginRequest("PLAYER_ONE", "PlayerPass@123"))
            userToken = "Bearer ${u1Login.data!!.accessToken}"
            walletService.getOrCreateWallet(userId, AccountRole.USER.name, 1_000L)

            // 3. Provision User 2 with 1,000 coins
            val user2 = authService.provisionAccountForTesting(
                loginId = "PLAYER_TWO",
                password = "PlayerPass@123",
                role = AccountRole.USER,
                fullName = "Player Two"
            )
            user2Id = user2.id
            val u2Login = authService.login(LoginRequest("PLAYER_TWO", "PlayerPass@123"))
            user2Token = "Bearer ${u2Login.data!!.accessToken}"
            walletService.getOrCreateWallet(user2Id, AccountRole.USER.name, 1_000L)
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testAdminCanCreateAndOpenGame() = runBlocking {
        // Admin creates prediction game with future start time (SCHEDULED state)
        val options = (0..9).map { digit ->
            GameOptionCreateRequest(optionCode = "$digit", displayName = "Digit $digit")
        }
        val createResp = apiRouter.handleCreateGame(
            authHeader = adminToken,
            request = CreateGameRequest(
                title = "Lucky Single Digit",
                description = "Predict 0 to 9",
                gameType = "SINGLE_DIGIT",
                options = options,
                startTime = System.currentTimeMillis() + 60000L,
                entryDeadline = System.currentTimeMillis() + 120000L,
                resultTime = System.currentTimeMillis() + 180000L,
                minCoins = 10L,
                maxCoins = 500L,
                rewardMultiplier = 9.0
            )
        )

        assertTrue(createResp.success)
        assertNotNull(createResp.data)
        val gameId = createResp.data!!.gameId
        assertEquals(GameStatus.SCHEDULED, createResp.data!!.status)
        assertEquals(10, createResp.data!!.options.size)

        // Non-admin cannot open the game
        val forbiddenOpen = apiRouter.handleOpenGame(
            authHeader = userToken,
            gameId = gameId
        )
        assertFalse(forbiddenOpen.success)
        assertEquals(403, forbiddenOpen.statusCode)

        // Admin opens the game
        val openResp = apiRouter.handleOpenGame(
            authHeader = adminToken,
            gameId = gameId
        )
        assertTrue(openResp.success)
        assertEquals(GameStatus.OPEN, openResp.data!!.status)
    }

    @Test
    fun testUserCanPlaceEntryAndBalanceDeductedAtomically() = runBlocking {
        // 1. Create and open game
        val options = (0..9).map { GameOptionCreateRequest("$it", "Digit $it") }
        val createResp = apiRouter.handleCreateGame(
            authHeader = adminToken,
            request = CreateGameRequest(
                title = "Evening Single Digit",
                description = "Single digit prediction",
                options = options,
                startTime = System.currentTimeMillis() + 60000L,
                entryDeadline = System.currentTimeMillis() + 120000L,
                resultTime = System.currentTimeMillis() + 180000L,
                minCoins = 50L,
                maxCoins = 500L,
                rewardMultiplier = 9.0
            )
        )
        val game = createResp.data!!
        val gameId = game.gameId
        val openResp = apiRouter.handleOpenGame(adminToken, gameId)
        assertTrue(openResp.success)

        val selectedOptionId = game.options.first { it.optionCode == "7" }.optionId

        // Initial balance: 1,000 coins
        val initialBalance = walletService.getOrCreateWallet(userId, AccountRole.USER.name).balance
        assertEquals(1000L, initialBalance)

        // 2. User places entry: 100 coins on option "7"
        val entryResp = apiRouter.handleSubmitGameEntry(
            authHeader = userToken,
            gameId = gameId,
            request = GameEntryRequest(
                selectedOptionId = selectedOptionId,
                virtualCoinAmount = 100L
            ),
            idempotencyKey = UUID.randomUUID().toString()
        )

        assertTrue(entryResp.success)
        assertNotNull(entryResp.data)
        assertEquals(EntryStatus.CONFIRMED, entryResp.data!!.status)
        assertEquals(selectedOptionId, entryResp.data!!.selectedOptionId)
        assertEquals(100L, entryResp.data!!.virtualCoinAmount)

        // Verify balance decreased by exactly 100 coins
        val newBalance = walletService.getOrCreateWallet(userId, AccountRole.USER.name).balance
        assertEquals(900L, newBalance)
    }

    @Test
    fun testEntryValidationFailures() = runBlocking {
        // Create and open game
        val options = listOf(
            GameOptionCreateRequest("UP", "Market Up"),
            GameOptionCreateRequest("DOWN", "Market Down")
        )
        val createResp = apiRouter.handleCreateGame(
            authHeader = adminToken,
            request = CreateGameRequest(
                title = "Validation Test Game",
                description = "Min 50, Max 200",
                options = options,
                startTime = System.currentTimeMillis() + 60000L,
                entryDeadline = System.currentTimeMillis() + 120000L,
                resultTime = System.currentTimeMillis() + 180000L,
                minCoins = 50L,
                maxCoins = 200L,
                rewardMultiplier = 2.0
            )
        )
        val game = createResp.data!!
        val gameId = game.gameId
        val openResp = apiRouter.handleOpenGame(adminToken, gameId)
        assertTrue(openResp.success)

        val validOptionId = game.options.first().optionId

        // 1. Below min wager (20 < 50)
        val lowWager = apiRouter.handleSubmitGameEntry(
            authHeader = userToken,
            gameId = gameId,
            request = GameEntryRequest(
                selectedOptionId = validOptionId,
                virtualCoinAmount = 20L
            ),
            idempotencyKey = UUID.randomUUID().toString()
        )
        assertFalse(lowWager.success)

        // 2. Above max wager (500 > 200)
        val highWager = apiRouter.handleSubmitGameEntry(
            authHeader = userToken,
            gameId = gameId,
            request = GameEntryRequest(
                selectedOptionId = validOptionId,
                virtualCoinAmount = 500L
            ),
            idempotencyKey = UUID.randomUUID().toString()
        )
        assertFalse(highWager.success)

        // 3. Disallowed/invalid option ID
        val invalidOption = apiRouter.handleSubmitGameEntry(
            authHeader = userToken,
            gameId = gameId,
            request = GameEntryRequest(
                selectedOptionId = "non_existent_option_id",
                virtualCoinAmount = 100L
            ),
            idempotencyKey = UUID.randomUUID().toString()
        )
        assertFalse(invalidOption.success)

        // Balance should still be 1,000
        val balance = walletService.getOrCreateWallet(userId, AccountRole.USER.name).balance
        assertEquals(1000L, balance)
    }

    @Test
    fun testGameFinalizationAndPayoutDistribution() = runBlocking {
        // 1. Create and open game
        val options = (0..9).map { GameOptionCreateRequest("$it", "Digit $it") }
        val createResp = apiRouter.handleCreateGame(
            authHeader = adminToken,
            request = CreateGameRequest(
                title = "Grand Jackpot",
                description = "Single digit prediction with 9x multiplier",
                options = options,
                startTime = System.currentTimeMillis() + 60000L,
                entryDeadline = System.currentTimeMillis() + 120000L,
                resultTime = System.currentTimeMillis() + 180000L,
                minCoins = 50L,
                maxCoins = 500L,
                rewardMultiplier = 9.0
            )
        )
        val game = createResp.data!!
        val gameId = game.gameId
        val openResp = apiRouter.handleOpenGame(adminToken, gameId)
        assertTrue(openResp.success)

        val winningOptionId = game.options.first { it.optionCode == "7" }.optionId
        val losingOptionId = game.options.first { it.optionCode == "3" }.optionId

        // 2. User 1 wagers 100 coins on "7" (Will win)
        val u1Entry = apiRouter.handleSubmitGameEntry(
            authHeader = userToken,
            gameId = gameId,
            request = GameEntryRequest(
                selectedOptionId = winningOptionId,
                virtualCoinAmount = 100L
            ),
            idempotencyKey = UUID.randomUUID().toString()
        )
        assertTrue(u1Entry.success)

        // 3. User 2 wagers 100 coins on "3" (Will lose)
        val u2Entry = apiRouter.handleSubmitGameEntry(
            authHeader = user2Token,
            gameId = gameId,
            request = GameEntryRequest(
                selectedOptionId = losingOptionId,
                virtualCoinAmount = 100L
            ),
            idempotencyKey = UUID.randomUUID().toString()
        )
        assertTrue(u2Entry.success)

        // Balances right now: User1 = 900, User2 = 900
        assertEquals(900L, walletService.getOrCreateWallet(userId, AccountRole.USER.name).balance)
        assertEquals(900L, walletService.getOrCreateWallet(user2Id, AccountRole.USER.name).balance)

        // 4. Admin closes the game
        val closeResp = apiRouter.handleCloseGame(adminToken, gameId)
        assertTrue(closeResp.success)
        assertEquals(GameStatus.CLOSED, closeResp.data!!.status)

        // 5. Admin finalizes result: Winning choice is option "7"
        val finalizeResp = apiRouter.handleFinalizeGameResult(
            authHeader = adminToken,
            gameId = gameId,
            request = FinalizeResultRequest(
                winningOptionId = winningOptionId,
                reason = "Audited final result declaration"
            ),
            idempotencyKey = UUID.randomUUID().toString()
        )
        assertTrue(finalizeResp.success)
        assertEquals(winningOptionId, finalizeResp.data!!.winningOptionId)
        assertEquals(1, finalizeResp.data!!.totalWinners)
        assertEquals(900L, finalizeResp.data!!.totalRewardsPaid) // 100 * 9.0

        // 6. Verify User 1 received 900 coins payout: 900 initial remaining + 900 payout = 1,800 coins
        val user1FinalBalance = walletService.getOrCreateWallet(userId, AccountRole.USER.name).balance
        assertEquals(1800L, user1FinalBalance)

        // 7. Verify User 2 received 0 payout: 900 remaining
        val user2FinalBalance = walletService.getOrCreateWallet(user2Id, AccountRole.USER.name).balance
        assertEquals(900L, user2FinalBalance)

        // 8. Verify entry statuses
        val u1Entries = apiRouter.handleGetMyEntries(userToken)
        assertTrue(u1Entries.success)
        assertEquals(EntryStatus.WON, u1Entries.data!!.entries.first { it.gameId == gameId }.status)
        assertEquals(900L, u1Entries.data!!.entries.first { it.gameId == gameId }.rewardAmount)

        val u2Entries = apiRouter.handleGetMyEntries(user2Token)
        assertTrue(u2Entries.success)
        assertEquals(EntryStatus.LOST, u2Entries.data!!.entries.first { it.gameId == gameId }.status)
    }

    @Test
    fun testGameCancellationRefundsAllWagers() = runBlocking {
        // 1. Create and open game
        val options = listOf(
            GameOptionCreateRequest("1", "Choice 1"),
            GameOptionCreateRequest("2", "Choice 2")
        )
        val createResp = apiRouter.handleCreateGame(
            authHeader = adminToken,
            request = CreateGameRequest(
                title = "Rainy Day Game",
                description = "Game to be cancelled",
                options = options,
                startTime = System.currentTimeMillis() + 60000L,
                entryDeadline = System.currentTimeMillis() + 120000L,
                resultTime = System.currentTimeMillis() + 180000L,
                minCoins = 50L,
                maxCoins = 500L,
                rewardMultiplier = 3.0
            )
        )
        val game = createResp.data!!
        val gameId = game.gameId
        val openResp = apiRouter.handleOpenGame(adminToken, gameId)
        assertTrue(openResp.success)

        val opt1 = game.options[0].optionId
        val opt2 = game.options[1].optionId

        // 2. Both users place wagers
        apiRouter.handleSubmitGameEntry(
            authHeader = userToken,
            gameId = gameId,
            request = GameEntryRequest(opt1, 200L),
            idempotencyKey = UUID.randomUUID().toString()
        )
        apiRouter.handleSubmitGameEntry(
            authHeader = user2Token,
            gameId = gameId,
            request = GameEntryRequest(opt2, 300L),
            idempotencyKey = UUID.randomUUID().toString()
        )

        // Pre-cancellation balances: User1 = 800, User2 = 700
        assertEquals(800L, walletService.getOrCreateWallet(userId, AccountRole.USER.name).balance)
        assertEquals(700L, walletService.getOrCreateWallet(user2Id, AccountRole.USER.name).balance)

        // 3. Admin cancels game
        val cancelResp = apiRouter.handleCancelGame(
            authHeader = adminToken,
            gameId = gameId,
            request = CancelGameRequest(reason = "Technical feed interrupted"),
            idempotencyKey = UUID.randomUUID().toString()
        )
        assertTrue(cancelResp.success)
        assertEquals(GameStatus.CANCELLED, cancelResp.data!!.status)

        // 4. Balances should be completely restored via refund
        val u1Restored = walletService.getOrCreateWallet(userId, AccountRole.USER.name).balance
        val u2Restored = walletService.getOrCreateWallet(user2Id, AccountRole.USER.name).balance
        assertEquals(1000L, u1Restored)
        assertEquals(1000L, u2Restored)

        // 5. Check entry statuses are REFUNDED
        val u1Entries = apiRouter.handleGetMyEntries(userToken)
        assertEquals(EntryStatus.REFUNDED, u1Entries.data!!.entries.first { it.gameId == gameId }.status)
    }
}
