package com.example

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.backend.api.ServerApiRouter
import com.example.backend.audit.AuditService
import com.example.backend.database.MahakalServerDatabase
import com.example.backend.model.CreateUserRequest
import com.example.backend.model.DeductCoinsRequest
import com.example.backend.model.LoginRequest
import com.example.backend.model.TransferCoinsRequest
import com.example.backend.rbac.AccountRole
import com.example.backend.security.RateLimiter
import com.example.backend.service.AdminAgentService
import com.example.backend.service.AgentUserService
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
class Phase6CoinManagementTest {

    private lateinit var database: MahakalServerDatabase
    private lateinit var auditService: AuditService
    private lateinit var rateLimiter: RateLimiter
    private lateinit var authService: ServerAuthService
    private lateinit var adminAgentService: AdminAgentService
    private lateinit var agentUserService: AgentUserService
    private lateinit var walletService: WalletTransactionService
    private lateinit var apiRouter: ServerApiRouter

    private lateinit var adminToken: String
    private lateinit var adminId: String

    private lateinit var agent1Token: String
    private lateinit var agent1Id: String

    private lateinit var agent2Token: String
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
            adminAgentService = AdminAgentService(database, auditService, rateLimiter)
            agentUserService = AgentUserService(database, auditService, rateLimiter)
            walletService = WalletTransactionService(database, auditService, rateLimiter)
            apiRouter = ServerApiRouter(
                authService = authService,
                auditService = auditService,
                adminAgentService = adminAgentService,
                agentUserService = agentUserService,
                walletTransactionService = walletService
            )

            // 1. Admin Bootstrap
            authService.bootstrapInitialAdmin("admin_root", "AdminSec@123", "Root Admin")
            val adminLogin = authService.login(LoginRequest("admin_root", "AdminSec@123"))
            adminToken = "Bearer ${adminLogin.data!!.accessToken}"
            adminId = adminLogin.data!!.user.id
            walletService.getOrCreateWallet(adminId, AccountRole.ADMIN.name, 50_000L)

            // 2. Provision Agent 1
            val agent1 = authService.provisionAccountForTesting(
                loginId = "AGENT_ALPHA",
                password = "AgentAlpha@123",
                role = AccountRole.AGENT,
                fullName = "Agent Alpha"
            )
            agent1Id = agent1.id
            val a1Login = authService.login(LoginRequest("AGENT_ALPHA", "AgentAlpha@123"))
            agent1Token = "Bearer ${a1Login.data!!.accessToken}"
            walletService.getOrCreateWallet(agent1Id, AccountRole.AGENT.name, 10_000L)

            // 3. Provision Agent 2
            val agent2 = authService.provisionAccountForTesting(
                loginId = "AGENT_BETA",
                password = "AgentBeta@123",
                role = AccountRole.AGENT,
                fullName = "Agent Beta"
            )
            agent2Id = agent2.id
            val a2Login = authService.login(LoginRequest("AGENT_BETA", "AgentBeta@123"))
            agent2Token = "Bearer ${a2Login.data!!.accessToken}"
            walletService.getOrCreateWallet(agent2Id, AccountRole.AGENT.name, 5_000L)

            // 4. Create User under Agent 1
            val u1Res = apiRouter.handleCreateUser(
                authHeader = agent1Token,
                request = CreateUserRequest(
                    userId = "user_alpha_1",
                    displayName = "User Alpha One",
                    temporaryPassword = "UserPass@123"
                )
            )
            assertTrue("Create user 1 must succeed: ${u1Res.message}", u1Res.success)
            user1Id = u1Res.data!!.id

            // 5. Create User under Agent 2
            val u2Res = apiRouter.handleCreateUser(
                authHeader = agent2Token,
                request = CreateUserRequest(
                    userId = "user_beta_1",
                    displayName = "User Beta One",
                    temporaryPassword = "UserPass@123"
                )
            )
            assertTrue("Create user 2 must succeed: ${u2Res.message}", u2Res.success)
            user2Id = u2Res.data!!.id

            // Explicitly set balances for all test accounts
            val wAgent1 = database.walletDao().findByOwnerId(agent1Id)!!
            database.walletDao().update(wAgent1.copy(balance = 10_000L))

            val wAgent2 = database.walletDao().findByOwnerId(agent2Id)!!
            database.walletDao().update(wAgent2.copy(balance = 5_000L))

            val wUser1 = database.walletDao().findByOwnerId(user1Id)!!
            database.walletDao().update(wUser1.copy(balance = 1_000L))

            val wUser2 = database.walletDao().findByOwnerId(user2Id)!!
            database.walletDao().update(wUser2.copy(balance = 2_000L))
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testAdminCanFetchBatchAccountBalances() = runBlocking {
        val response = apiRouter.handleGetAccountBalances(adminToken, listOf(agent1Id, agent2Id, user1Id))

        assertEquals(200, response.statusCode)
        assertNotNull(response.data)
        val balances = response.data!!.balances
        assertEquals(10_000L, balances[agent1Id])
        assertEquals(5_000L, balances[agent2Id])
        assertEquals(1_000L, balances[user1Id])
    }

    @Test
    fun testAdminCanFetchAgentAccountSummary() = runBlocking {
        val response = apiRouter.handleGetAccountCoinSummary(adminToken, agent1Id)

        assertEquals(200, response.statusCode)
        assertNotNull(response.data)
        val summary = response.data!!
        assertEquals(agent1Id, summary.accountId)
        assertEquals("AGENT_ALPHA", summary.loginId)
        assertEquals("Agent Alpha", summary.fullName)
        assertEquals("AGENT", summary.role)
        assertEquals(10_000L, summary.balance)
    }

    @Test
    fun testAgentCanFetchSubordinatedUserSummary() = runBlocking {
        val response = apiRouter.handleGetAccountCoinSummary(agent1Token, user1Id)

        assertEquals(200, response.statusCode)
        assertNotNull(response.data)
        val summary = response.data!!
        assertEquals(user1Id, summary.accountId)
        assertEquals("USER_ALPHA_1", summary.loginId)
        assertEquals("USER", summary.role)
        assertEquals(1_000L, summary.balance)
    }

    @Test
    fun testCrossAgentCannotFetchUnownedUserSummary() = runBlocking {
        // Agent 1 attempting to inspect Agent 2's user
        val response = apiRouter.handleGetAccountCoinSummary(agent1Token, user2Id)

        assertEquals(403, response.statusCode)
        assertFalse(response.success)
        assertEquals("RESOURCE_NOT_OWNED", response.errorCode)
    }

    @Test
    fun testAdminTransferToAgentWithIdempotency() = runBlocking {
        val key = UUID.randomUUID().toString()
        val transferReq = TransferCoinsRequest(
            destinationAccountId = agent1Id,
            amount = 5_000L,
            reason = "Allocating operating virtual coins to Agent Alpha"
        )

        val firstResponse = apiRouter.handleTransferCoins(adminToken, transferReq, key)
        assertEquals(200, firstResponse.statusCode)
        assertTrue(firstResponse.success)
        val tx = firstResponse.data!!
        assertEquals(50_000L, tx.balanceBeforeSource)
        assertEquals(45_000L, tx.balanceAfterSource)
        assertEquals(10_000L, tx.balanceBeforeDestination)
        assertEquals(15_000L, tx.balanceAfterDestination)

        // Resubmit with the same idempotency key (network retry scenario)
        val replayResponse = apiRouter.handleTransferCoins(adminToken, transferReq, key)
        assertEquals(200, replayResponse.statusCode)
        assertEquals(tx.transactionId, replayResponse.data!!.transactionId)

        // Verify balances were not mutated twice
        val balances = apiRouter.handleGetAccountBalances(adminToken, listOf(adminId, agent1Id))
        assertEquals(45_000L, balances.data!!.balances[adminId])
        assertEquals(15_000L, balances.data!!.balances[agent1Id])
    }

    @Test
    fun testAdminDeductFromAgent() = runBlocking {
        val deductReq = DeductCoinsRequest(
            targetAccountId = agent1Id,
            amount = 3_000L,
            reason = "Admin coin balance reclamation"
        )

        val res = apiRouter.handleDeductCoins(adminToken, deductReq, UUID.randomUUID().toString())
        assertEquals(200, res.statusCode)
        val tx = res.data!!
        assertEquals(10_000L, tx.balanceBeforeSource)
        assertEquals(7_000L, tx.balanceAfterSource)
    }

    @Test
    fun testDeductFailsWhenInsufficientBalance() = runBlocking {
        val deductReq = DeductCoinsRequest(
            targetAccountId = agent1Id,
            amount = 99_999L, // Greater than Agent 1's 10,000 balance
            reason = "Excess deduction attempt"
        )

        val res = apiRouter.handleDeductCoins(adminToken, deductReq, UUID.randomUUID().toString())
        assertEquals(403, res.statusCode)
        assertEquals("INSUFFICIENT_BALANCE", res.errorCode)
    }

    @Test
    fun testAgentTransferToSubordinatedUser() = runBlocking {
        val transferReq = TransferCoinsRequest(
            destinationAccountId = user1Id,
            amount = 2_500L,
            reason = "Agent Alpha allocating virtual coins to User 1"
        )

        val res = apiRouter.handleTransferCoins(agent1Token, transferReq, UUID.randomUUID().toString())
        assertEquals(200, res.statusCode)
        val tx = res.data!!
        assertEquals(10_000L, tx.balanceBeforeSource)
        assertEquals(7_500L, tx.balanceAfterSource)
        assertEquals(1_000L, tx.balanceBeforeDestination)
        assertEquals(3_500L, tx.balanceAfterDestination)
    }

    @Test
    fun testAgentCrossTransferToUnownedUserForbidden() = runBlocking {
        // Agent 1 attempting to transfer coins to Agent 2's user
        val transferReq = TransferCoinsRequest(
            destinationAccountId = user2Id,
            amount = 500L,
            reason = "Illegal cross-agent transfer attempt"
        )

        val res = apiRouter.handleTransferCoins(agent1Token, transferReq, UUID.randomUUID().toString())
        assertEquals(403, res.statusCode)
        assertEquals("RESOURCE_NOT_OWNED", res.errorCode)
    }

    @Test
    fun testTransferRejectsZeroOrNegativeAmount() = runBlocking {
        val zeroReq = TransferCoinsRequest(
            destinationAccountId = agent1Id,
            amount = 0L,
            reason = "Zero test"
        )
        val resZero = apiRouter.handleTransferCoins(adminToken, zeroReq, UUID.randomUUID().toString())
        assertEquals(400, resZero.statusCode)
        assertEquals("INVALID_AMOUNT", resZero.errorCode)

        val negReq = TransferCoinsRequest(
            destinationAccountId = agent1Id,
            amount = -500L,
            reason = "Negative test"
        )
        val resNeg = apiRouter.handleTransferCoins(adminToken, negReq, UUID.randomUUID().toString())
        assertEquals(400, resNeg.statusCode)
        assertEquals("INVALID_AMOUNT", resNeg.errorCode)
    }
}
