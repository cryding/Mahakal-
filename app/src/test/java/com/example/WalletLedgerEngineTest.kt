package com.example

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.backend.api.ServerApiRouter
import com.example.backend.audit.AuditService
import com.example.backend.database.MahakalServerDatabase
import com.example.backend.model.CreateUserRequest
import com.example.backend.model.DeductCoinsRequest
import com.example.backend.model.LoginRequest
import com.example.backend.model.ReverseTransactionRequest
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
@Config(sdk = [36])
class WalletLedgerEngineTest {

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

    private lateinit var user1Token: String
    private lateinit var user1Id: String

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
            apiRouter = ServerApiRouter(
                authService = authService,
                auditService = auditService,
                adminAgentService = adminAgentService,
                agentUserService = agentUserService,
                walletTransactionService = walletService
            )

            // 1. Bootstrap Root Admin
            authService.bootstrapInitialAdmin("admin_root", "AdminSec@123", "Root Admin")
            val adminLogin = authService.login(LoginRequest("admin_root", "AdminSec@123"))
            adminToken = "Bearer ${adminLogin.data!!.accessToken}"
            adminId = adminLogin.data!!.user.id
            walletService.getOrCreateWallet(adminId, AccountRole.ADMIN.name, 1_000_000_000L)

            // 2. Provision Agent 1
            val agent1 = authService.provisionAccountForTesting(
                loginId = "AGENT_01",
                password = "AgentPassword@1",
                role = AccountRole.AGENT,
                fullName = "Agent One"
            )
            agent1Id = agent1.id
            val a1Login = authService.login(LoginRequest("AGENT_01", "AgentPassword@1"))
            agent1Token = "Bearer ${a1Login.data!!.accessToken}"
            walletService.getOrCreateWallet(agent1Id, AccountRole.AGENT.name, 0L)

            // 3. Provision Agent 2
            val agent2 = authService.provisionAccountForTesting(
                loginId = "AGENT_02",
                password = "AgentPassword@2",
                role = AccountRole.AGENT,
                fullName = "Agent Two"
            )
            agent2Id = agent2.id
            val a2Login = authService.login(LoginRequest("AGENT_02", "AgentPassword@2"))
            agent2Token = "Bearer ${a2Login.data!!.accessToken}"
            walletService.getOrCreateWallet(agent2Id, AccountRole.AGENT.name, 0L)

            // 4. Create User 1 owned by Agent 1
            val u1Res = apiRouter.handleCreateUser(
                authHeader = agent1Token,
                request = CreateUserRequest(
                    userId = "PLAYER_ONE",
                    displayName = "Player One",
                    temporaryPassword = "UserPass@123"
                )
            )
            assertTrue("Create User 1 must succeed: ${u1Res.message}", u1Res.success)
            user1Id = u1Res.data!!.id
            val u1Login = authService.login(LoginRequest("PLAYER_ONE", "UserPass@123"))
            user1Token = "Bearer ${u1Login.data!!.accessToken}"

            // 5. Create User 2 owned by Agent 2
            val u2Res = apiRouter.handleCreateUser(
                authHeader = agent2Token,
                request = CreateUserRequest(
                    userId = "PLAYER_TWO",
                    displayName = "Player Two",
                    temporaryPassword = "UserPass@456"
                )
            )
            assertTrue("Create User 2 must succeed: ${u2Res.message}", u2Res.success)
            user2Id = u2Res.data!!.id
            val u2Login = authService.login(LoginRequest("PLAYER_TWO", "UserPass@456"))
            user2Token = "Bearer ${u2Login.data!!.accessToken}"
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun test01_admin_starts_with_initial_treasury_and_agent_starts_zero() = runBlocking {
        val adminBal = apiRouter.handleGetMyWalletBalance(adminToken)
        assertTrue(adminBal.success)
        assertEquals(1_000_000_000L, adminBal.data!!.balance)

        val agent1Bal = apiRouter.handleGetMyWalletBalance(agent1Token)
        assertTrue(agent1Bal.success)
        assertEquals(0L, agent1Bal.data!!.balance)

        val user1Bal = apiRouter.handleGetMyWalletBalance(user1Token)
        assertTrue(user1Bal.success)
        assertEquals(0L, user1Bal.data!!.balance)
    }

    @Test
    fun test02_admin_to_agent_atomic_transfer_conserves_total_balance() = runBlocking {
        val transferAmount = 50_000L
        val idempotencyKey = UUID.randomUUID().toString()

        val req = TransferCoinsRequest(
            destinationAccountId = agent1Id,
            amount = transferAmount,
            reason = "Initial Agent Working Capital"
        )

        val res = apiRouter.handleTransferCoins(adminToken, req, idempotencyKey, "10.0.0.1")
        assertTrue("Transfer must succeed: ${res.message}", res.success)
        assertEquals(transferAmount, res.data!!.amount)
        assertEquals("ADMIN_TO_AGENT", res.data!!.transactionType)

        // Verify Admin balance
        val adminBal = apiRouter.handleGetMyWalletBalance(adminToken)
        assertEquals(1_000_000_000L - transferAmount, adminBal.data!!.balance)

        // Verify Agent balance
        val agentBal = apiRouter.handleGetMyWalletBalance(agent1Token)
        assertEquals(transferAmount, agentBal.data!!.balance)

        // Verify total conservation
        assertEquals(1_000_000_000L, adminBal.data!!.balance + agentBal.data!!.balance)
    }

    @Test
    fun test03_agent_to_user_transfer_strictly_enforces_ownership() = runBlocking {
        // First allocate 10,000 coins to Agent 1
        apiRouter.handleTransferCoins(
            adminToken,
            TransferCoinsRequest(agent1Id, 10_000L, "Working Capital"),
            UUID.randomUUID().toString(),
            "10.0.0.1"
        )

        // Agent 1 transfers 2,000 to User 1 (subordinated to Agent 1) -> Must SUCCEED
        val validReq = TransferCoinsRequest(
            destinationAccountId = user1Id,
            amount = 2_000L,
            reason = "Player allocation"
        )
        val validRes = apiRouter.handleTransferCoins(agent1Token, validReq, UUID.randomUUID().toString(), "10.0.0.1")
        assertTrue(validRes.success)
        assertEquals("AGENT_TO_USER", validRes.data!!.transactionType)

        val agent1Bal = apiRouter.handleGetMyWalletBalance(agent1Token)
        assertEquals(8_000L, agent1Bal.data!!.balance)

        val user1Bal = apiRouter.handleGetMyWalletBalance(user1Token)
        assertEquals(2_000L, user1Bal.data!!.balance)

        // Agent 1 attempts to transfer to User 2 (owned by Agent 2) -> Must FAIL with RBAC / Ownership error
        val invalidReq = TransferCoinsRequest(
            destinationAccountId = user2Id,
            amount = 1_000L,
            reason = "Illicit cross-agent allocation"
        )
        val invalidRes = apiRouter.handleTransferCoins(agent1Token, invalidReq, UUID.randomUUID().toString(), "10.0.0.1")
        assertFalse("Cross-agent transfer must fail", invalidRes.success)

        // Balances must remain unchanged after failure
        assertEquals(8_000L, apiRouter.handleGetMyWalletBalance(agent1Token).data!!.balance)
        assertEquals(0L, apiRouter.handleGetMyWalletBalance(user2Token).data!!.balance)
    }

    @Test
    fun test04_overdraft_prevention_rejects_insufficient_funds() = runBlocking {
        // Agent 1 has 0 coins, attempts to transfer 500 to User 1
        val req = TransferCoinsRequest(
            destinationAccountId = user1Id,
            amount = 500L,
            reason = "Overdraft attempt"
        )

        val res = apiRouter.handleTransferCoins(agent1Token, req, UUID.randomUUID().toString(), "10.0.0.1")
        assertFalse(res.success)
        assertTrue("Error must indicate insufficient balance", (res.message ?: "").contains("Insufficient", ignoreCase = true))

        assertEquals(0L, apiRouter.handleGetMyWalletBalance(agent1Token).data!!.balance)
        assertEquals(0L, apiRouter.handleGetMyWalletBalance(user1Token).data!!.balance)
    }

    @Test
    fun test05_idempotency_key_prevents_duplicate_debits() = runBlocking {
        // Fund Agent 1 with 5,000
        apiRouter.handleTransferCoins(
            adminToken,
            TransferCoinsRequest(agent1Id, 5_000L, "Fund Agent"),
            UUID.randomUUID().toString(),
            "10.0.0.1"
        )

        val fixedIdempotencyKey = "unique-tx-idempotency-key-12345"
        val req = TransferCoinsRequest(
            destinationAccountId = user1Id,
            amount = 1_500L,
            reason = "First submit"
        )

        // First call
        val res1 = apiRouter.handleTransferCoins(agent1Token, req, fixedIdempotencyKey, "10.0.0.1")
        assertTrue(res1.success)
        val txId1 = res1.data!!.transactionId

        // Immediate duplicate call with exact same idempotency key
        val res2 = apiRouter.handleTransferCoins(agent1Token, req, fixedIdempotencyKey, "10.0.0.1")
        assertTrue(res2.success)
        assertEquals("Duplicate call must return exact same transaction ID", txId1, res2.data!!.transactionId)

        // Verify balance was only deducted ONCE
        val agentBal = apiRouter.handleGetMyWalletBalance(agent1Token)
        assertEquals(3_500L, agentBal.data!!.balance)

        val userBal = apiRouter.handleGetMyWalletBalance(user1Token)
        assertEquals(1_500L, userBal.data!!.balance)
    }

    @Test
    fun test06_non_positive_amounts_are_rejected() = runBlocking {
        val zeroReq = TransferCoinsRequest(
            destinationAccountId = agent1Id,
            amount = 0L,
            reason = "Zero amount test"
        )
        val zeroRes = apiRouter.handleTransferCoins(adminToken, zeroReq, UUID.randomUUID().toString(), "10.0.0.1")
        assertFalse(zeroRes.success)

        val negReq = TransferCoinsRequest(
            destinationAccountId = agent1Id,
            amount = -500L,
            reason = "Negative amount test"
        )
        val negRes = apiRouter.handleTransferCoins(adminToken, negReq, UUID.randomUUID().toString(), "10.0.0.1")
        assertFalse(negRes.success)
    }

    @Test
    fun test07_agent_deduction_from_subordinated_user() = runBlocking {
        // Step 1: Admin funds Agent 1 with 10,000
        apiRouter.handleTransferCoins(
            adminToken,
            TransferCoinsRequest(agent1Id, 10_000L, "Working Capital"),
            UUID.randomUUID().toString(),
            "10.0.0.1"
        )

        // Step 2: Agent 1 transfers 3,000 to User 1
        apiRouter.handleTransferCoins(
            agent1Token,
            TransferCoinsRequest(user1Id, 3_000L, "Player Allocation"),
            UUID.randomUUID().toString(),
            "10.0.0.1"
        )
        assertEquals(3_000L, apiRouter.handleGetMyWalletBalance(user1Token).data!!.balance)
        assertEquals(7_000L, apiRouter.handleGetMyWalletBalance(agent1Token).data!!.balance)

        // Step 3: Agent 1 deducts 1,200 from User 1
        val deductReq = DeductCoinsRequest(
            targetAccountId = user1Id,
            amount = 1_200L,
            reason = "User withdrawal / balance return"
        )
        val deductRes = apiRouter.handleDeductCoins(agent1Token, deductReq, UUID.randomUUID().toString())
        assertTrue(deductRes.success)
        assertEquals("AGENT_DEDUCTION", deductRes.data!!.transactionType)

        // User balance should be 3000 - 1200 = 1800
        assertEquals(1_800L, apiRouter.handleGetMyWalletBalance(user1Token).data!!.balance)
        // Agent balance remains 7000 (deduction reduces user balance and is recorded in immutable ledger)
        assertEquals(7_000L, apiRouter.handleGetMyWalletBalance(agent1Token).data!!.balance)

        // Agent 1 attempts to deduct from User 2 (not owned by Agent 1) -> Must FAIL
        val invalidDeduct = DeductCoinsRequest(
            targetAccountId = user2Id,
            amount = 100L,
            reason = "Unowned user deduction"
        )
        val failDeductRes = apiRouter.handleDeductCoins(agent1Token, invalidDeduct, UUID.randomUUID().toString())
        assertFalse(failDeductRes.success)
    }

    @Test
    fun test08_admin_reversal_of_transaction_restores_balances() = runBlocking {
        // Admin transfers 20,000 to Agent 1
        val transferRes = apiRouter.handleTransferCoins(
            adminToken,
            TransferCoinsRequest(agent1Id, 20_000L, "Accidental allocation"),
            UUID.randomUUID().toString(),
            "10.0.0.1"
        )
        assertTrue(transferRes.success)
        val txId = transferRes.data!!.transactionId

        assertEquals(999_980_000L, apiRouter.handleGetMyWalletBalance(adminToken).data!!.balance)
        assertEquals(20_000L, apiRouter.handleGetMyWalletBalance(agent1Token).data!!.balance)

        // Admin reverses the transaction
        val revRes = apiRouter.handleReverseTransaction(
            adminToken,
            txId,
            ReverseTransactionRequest("Reversal due to operator typo"),
            UUID.randomUUID().toString()
        )
        assertTrue("Reversal must succeed: ${revRes.message}", revRes.success)
        assertEquals("SYSTEM_ADJUSTMENT", revRes.data!!.transactionType)
        assertEquals(txId, revRes.data!!.referenceId)

        // Balances must be fully restored
        assertEquals(1_000_000_000L, apiRouter.handleGetMyWalletBalance(adminToken).data!!.balance)
        assertEquals(0L, apiRouter.handleGetMyWalletBalance(agent1Token).data!!.balance)

        // Attempting to reverse again must fail
        val duplicateRevRes = apiRouter.handleReverseTransaction(
            adminToken,
            txId,
            ReverseTransactionRequest("Second reversal attempt"),
            UUID.randomUUID().toString()
        )
        assertFalse("Cannot reverse an already reversed transaction", duplicateRevRes.success)

        // Agent or User attempting to reverse must be forbidden
        val agentRevRes = apiRouter.handleReverseTransaction(
            agent1Token,
            txId,
            ReverseTransactionRequest("Agent reversal attempt"),
            UUID.randomUUID().toString()
        )
        assertFalse("Agent cannot execute reversals", agentRevRes.success)
    }

    @Test
    fun test09_ledger_audit_log_verification() = runBlocking {
        // Execute a transfer
        apiRouter.handleTransferCoins(
            adminToken,
            TransferCoinsRequest(agent1Id, 15_000L, "Audit verification transfer"),
            UUID.randomUUID().toString(),
            "10.0.0.1"
        )

        // Query global ledger as Admin
        val ledgerRes = apiRouter.handleGetTransactions(adminToken, page = 1, limit = 20)
        assertTrue(ledgerRes.success)
        assertTrue(ledgerRes.data!!.items.isNotEmpty())

        val latestTx = ledgerRes.data!!.items.first()
        assertEquals(15_000L, latestTx.amount)
        assertEquals("Audit verification transfer", latestTx.reason)

        // Query ledger as Agent -> should only see agent's transactions
        val agentLedgerRes = apiRouter.handleGetTransactions(agent1Token, page = 1, limit = 20)
        assertTrue(agentLedgerRes.success)
        val agentWalletId = apiRouter.handleGetMyWalletBalance(agent1Token).data!!.walletId
        assertTrue(agentLedgerRes.data!!.items.all {
            it.sourceWalletId == agentWalletId || it.destinationWalletId == agentWalletId
        })
    }
}
