package com.example

import android.app.Application
import com.example.backend.api.ServerApiRouter
import com.example.backend.audit.AuditService
import com.example.backend.database.MahakalServerDatabase
import com.example.backend.rbac.AccountRole
import com.example.backend.rbac.AccountStatus
import com.example.backend.security.RateLimiter
import com.example.backend.service.AdminAgentService
import com.example.backend.service.AgentUserService
import com.example.backend.service.GameEngineService
import com.example.backend.service.ServerAuthService
import com.example.backend.service.WalletTransactionService
import com.example.backend.service.NotificationService
import com.example.backend.service.OutboxWorker
import com.example.backend.service.SecurityHardeningService
import com.example.backend.model.CreateGameRequest
import com.example.backend.model.GameOptionCreateRequest
import com.example.core.network.SessionManager
import com.example.core.security.SecureTokenStorage
import com.example.data.repository.AgentRepository
import com.example.data.repository.AgentRepositoryImpl
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthRepositoryImpl
import com.example.data.repository.GameRepository
import com.example.data.repository.GameRepositoryImpl
import com.example.data.repository.NotificationRepository
import com.example.data.repository.NotificationRepositoryImpl
import com.example.data.repository.SecurityRepository
import com.example.data.repository.SecurityRepositoryImpl
import com.example.data.repository.UserRepository
import com.example.data.repository.UserRepositoryImpl
import com.example.data.repository.WalletRepository
import com.example.data.repository.WalletRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MahakalApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var database: MahakalServerDatabase
        private set
    lateinit var auditService: AuditService
        private set
    lateinit var rateLimiter: RateLimiter
        private set
    lateinit var serverAuthService: ServerAuthService
        private set
    lateinit var adminAgentService: AdminAgentService
        private set
    lateinit var agentUserService: AgentUserService
        private set
    lateinit var walletTransactionService: WalletTransactionService
        private set
    lateinit var gameEngineService: GameEngineService
        private set
    lateinit var serverApiRouter: ServerApiRouter
        private set
    lateinit var secureTokenStorage: SecureTokenStorage
        private set
    lateinit var sessionManager: SessionManager
        private set
    lateinit var authRepository: AuthRepository
        private set
    lateinit var agentRepository: AgentRepository
        private set
    lateinit var userRepository: UserRepository
        private set
    lateinit var walletRepository: WalletRepository
        private set
    lateinit var gameRepository: GameRepository
        private set
    lateinit var notificationService: NotificationService
        private set
    lateinit var notificationRepository: NotificationRepository
        private set
    lateinit var securityHardeningService: SecurityHardeningService
        private set
    lateinit var outboxWorker: OutboxWorker
        private set
    lateinit var securityRepository: SecurityRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize Real Persistent Server Engine
        database = MahakalServerDatabase.getInstance(this)
        auditService = AuditService(database.auditLogDao())
        rateLimiter = RateLimiter(database.rateLimitDao())
        notificationService = NotificationService(database, auditService, rateLimiter)
        outboxWorker = OutboxWorker(database, notificationService)
        outboxWorker.start(intervalMs = 15_000L)
        serverAuthService = ServerAuthService(database, auditService, rateLimiter, notificationService)
        adminAgentService = AdminAgentService(database, auditService, rateLimiter)
        agentUserService = AgentUserService(database, auditService, rateLimiter)
        walletTransactionService = WalletTransactionService(database, auditService, rateLimiter, notificationService)
        gameEngineService = GameEngineService(database, auditService, rateLimiter, walletTransactionService, notificationService)
        securityHardeningService = SecurityHardeningService(database, auditService, rateLimiter, serverAuthService)
        serverApiRouter = ServerApiRouter(
            serverAuthService,
            auditService,
            adminAgentService,
            agentUserService,
            walletTransactionService,
            gameEngineService,
            notificationService,
            securityHardeningService
        )

        // Initialize Android Secure Client Storage & Session
        secureTokenStorage = SecureTokenStorage(this)
        sessionManager = SessionManager(secureTokenStorage)
        authRepository = AuthRepositoryImpl(serverApiRouter, sessionManager, secureTokenStorage)
        agentRepository = AgentRepositoryImpl(serverApiRouter, secureTokenStorage)
        userRepository = UserRepositoryImpl(serverApiRouter, secureTokenStorage)
        walletRepository = WalletRepositoryImpl(serverApiRouter, secureTokenStorage)
        gameRepository = GameRepositoryImpl(serverApiRouter, secureTokenStorage)
        notificationRepository = NotificationRepositoryImpl(serverApiRouter, secureTokenStorage, notificationService)
        securityRepository = SecurityRepositoryImpl(serverApiRouter, secureTokenStorage)

        // Seed development / initial accounts if empty
        applicationScope.launch {
            bootstrapDevelopmentAccounts()
        }
    }

    /**
     * Bootstrap default environment accounts for demonstration and verification if database is fresh.
     * Note: In production deployment, bootstrap credentials are provided via secure environment secrets.
     */
    private suspend fun bootstrapDevelopmentAccounts() {
        // Bootstrap Initial Master Admin if not present
        serverAuthService.bootstrapInitialAdmin(
            loginId = "admin",
            temporaryPassword = "AdminPassword@123",
            fullName = "Mahakal Chief Administrator"
        )

        // Provision demo Agent and User for Phase 2 validation testing
        val agentAccount = database.accountDao().findByLoginId("agent01")
        val createdAgent = if (agentAccount == null) {
            val admin = database.accountDao().findByLoginId("admin")
            serverAuthService.provisionAccountForTesting(
                loginId = "agent01",
                password = "AgentPassword@123",
                role = AccountRole.AGENT,
                parentId = admin?.id,
                fullName = "Regional Agent North"
            )
        } else agentAccount

        if (database.accountDao().findByLoginId("user01") == null) {
            serverAuthService.provisionAccountForTesting(
                loginId = "user01",
                password = "UserPassword@123",
                role = AccountRole.USER,
                parentId = createdAgent.id,
                fullName = "Alpha User"
            )
        }

        // Also provision a suspended user to test status enforcement
        if (database.accountDao().findByLoginId("user_suspended") == null) {
            serverAuthService.provisionAccountForTesting(
                loginId = "user_suspended",
                password = "UserPassword@123",
                role = AccountRole.USER,
                status = AccountStatus.SUSPENDED,
                parentId = createdAgent.id,
                fullName = "Suspended Test User"
            )
        }
        // Ensure wallets are initialized for bootstrapped demo accounts
        val adminUser = database.accountDao().findByLoginId("admin")
        if (adminUser != null) {
            walletTransactionService.getOrCreateWallet(
                accountId = adminUser.id,
                role = AccountRole.ADMIN.name,
                initialBalance = 500_000L
            )
        }

        walletTransactionService.getOrCreateWallet(
            accountId = createdAgent.id,
            role = AccountRole.AGENT.name,
            initialBalance = 50_000L
        )

        val demoUser = database.accountDao().findByLoginId("user01")
        if (demoUser != null) {
            walletTransactionService.getOrCreateWallet(
                accountId = demoUser.id,
                role = AccountRole.USER.name,
                initialBalance = 1_000L
            )
        }

        // Phase 7: Bootstrap active prediction games if empty
        if (database.gameDao().getAllGames().isEmpty() && adminUser != null) {
            val now = System.currentTimeMillis()
            val adminContext = com.example.backend.rbac.SecurityContext(
                accountId = adminUser.id,
                loginId = adminUser.loginId,
                role = AccountRole.ADMIN,
                status = AccountStatus.ACTIVE,
                permissions = com.example.backend.rbac.MahakalRbac.getPermissionsForRole(AccountRole.ADMIN)
            )

            // Game 1: Hourly Market Trend Prediction (Open now)
            gameEngineService.createGame(
                context = adminContext,
                request = CreateGameRequest(
                    gameType = "MARKET_TREND",
                    title = "Gold Index Hourly Trend",
                    description = "Predict whether the gold rate index finishes higher, lower, or unchanged in this cycle. Virtual points prediction.",
                    options = listOf(
                        GameOptionCreateRequest("BULL", "Bullish (Up)"),
                        GameOptionCreateRequest("BEAR", "Bearish (Down)"),
                        GameOptionCreateRequest("FLAT", "Range-bound (Flat)")
                    ),
                    startTime = now - 60_000L,
                    entryDeadline = now + 3600_000L, // 1 hour ahead
                    resultTime = now + 3900_000L,
                    minCoins = 10L,
                    maxCoins = 500L,
                    rewardMultiplier = 2.5
                )
            )

            // Game 2: Single Digit Prediction
            gameEngineService.createGame(
                context = adminContext,
                request = CreateGameRequest(
                    gameType = "DIGIT_PREDICTION",
                    title = "Lucky Single Digit Arena",
                    description = "Predict the winning single digit (0 through 9) in the upcoming hourly round.",
                    options = (0..9).map { digit ->
                        GameOptionCreateRequest("DIGIT_$digit", "Digit $digit")
                    },
                    startTime = now,
                    entryDeadline = now + 7200_000L, // 2 hours ahead
                    resultTime = now + 7500_000L,
                    minCoins = 5L,
                    maxCoins = 200L,
                    rewardMultiplier = 9.0
                )
            )
        }
    }

    companion object {
        lateinit var instance: MahakalApplication
            private set
    }
}
