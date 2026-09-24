package com.example.backend.api

import com.example.backend.audit.AuditActions
import com.example.backend.audit.AuditService
import com.example.backend.model.AccountCoinSummaryDto
import com.example.backend.model.AgentCreatedDto
import com.example.backend.model.AgentDetailsDto
import com.example.backend.model.AgentSummaryDto
import com.example.backend.model.BatchBalancesResponse
import com.example.backend.model.ChangePasswordRequest
import com.example.backend.model.CreateAgentRequest
import com.example.backend.model.CreateUserRequest
import com.example.backend.model.EditAgentRequest
import com.example.backend.model.EditUserRequest
import com.example.backend.model.LoginRequest
import com.example.backend.model.PaginatedAgentListResponse
import com.example.backend.model.PaginatedUserListResponse
import com.example.backend.model.ResetAgentPasswordRequest
import com.example.backend.model.ResetPasswordResponse
import com.example.backend.model.ResetUserPasswordRequest
import com.example.backend.model.ServerResponse
import com.example.backend.model.UserCreatedDto
import com.example.backend.model.UserDetailsDto
import com.example.backend.model.UserSummaryDto
import com.example.backend.rbac.AccountRole
import com.example.backend.rbac.AccountStatus
import com.example.backend.rbac.MahakalRbac
import com.example.backend.rbac.Permission
import com.example.backend.rbac.SecurityContext
import com.example.backend.service.AdminAgentService
import com.example.backend.service.AgentUserService
import com.example.backend.service.GameEngineService
import com.example.backend.service.ServerAuthService
import com.example.backend.service.WalletTransactionService
import com.example.backend.service.NotificationService
import com.example.backend.service.SecurityHardeningService
import com.example.backend.model.RefreshTokenRequest
import com.example.backend.model.SecurityDashboardSummaryDto
import com.example.backend.model.SecurityEventsQueryResponse
import com.example.backend.model.SessionInfoDto
import com.example.backend.model.HealthCheckDto
import com.example.backend.model.ReadinessCheckDto
import com.example.backend.model.NotificationDto
import com.example.backend.model.NotificationListResponse
import com.example.backend.model.NotificationPreferenceDto
import com.example.backend.model.RegisterDeviceRequest
import com.example.backend.model.UnreadCountResponse
import com.example.backend.model.UpdateNotificationPreferenceRequest
import com.example.backend.model.AdminGameDetailsDto
import com.example.backend.model.AdminGameEntriesResponse
import com.example.backend.model.AdminOperationsSummaryDto
import com.example.backend.model.AdminReconciliationReportDto
import com.example.backend.model.AuditLogListResponse
import com.example.backend.model.CancelGameRequest
import com.example.backend.model.CreateGameRequest
import com.example.backend.model.DeductCoinsRequest
import com.example.backend.model.EntriesReportDto
import com.example.backend.model.FinalizeResultRequest
import com.example.backend.model.GameDto
import com.example.backend.model.GameEntryDto
import com.example.backend.model.GameEntryRequest
import com.example.backend.model.GameListResponse
import com.example.backend.model.GameProcessingDto
import com.example.backend.model.GameResultDto
import com.example.backend.model.GamesReportDto
import com.example.backend.model.PaginatedAdminGameListResponse
import com.example.backend.model.PaginatedTransactionListResponse
import com.example.backend.model.ReverseTransactionRequest
import com.example.backend.model.TransactionDetailDto
import com.example.backend.model.TransactionDto
import com.example.backend.model.TransactionsReportDto
import com.example.backend.model.TransferCoinsRequest
import com.example.backend.model.UserEntriesResponse
import com.example.backend.model.WalletBalanceDto
import com.example.backend.model.WalletDto
import java.util.UUID

/**
 * Server API Router handling HTTP routes, RBAC middleware, and response serialization.
 */
class ServerApiRouter(
    private val authService: ServerAuthService,
    private val auditService: AuditService,
    private val adminAgentService: AdminAgentService? = null,
    private val agentUserService: AgentUserService? = null,
    private val walletTransactionService: WalletTransactionService? = null,
    private val gameEngineService: GameEngineService? = null,
    private val notificationService: NotificationService? = null,
    private val securityHardeningService: SecurityHardeningService? = null
) {

    suspend fun handleLogin(
        request: LoginRequest,
        clientIp: String? = null,
        userAgent: String? = null,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<*> {
        return authService.login(request, clientIp, userAgent, requestId)
    }

    suspend fun handleRefreshToken(
        request: RefreshTokenRequest,
        clientIp: String? = null,
        userAgent: String? = null,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<*> {
        return authService.refreshSession(request.refreshToken, clientIp, userAgent, requestId)
    }

    suspend fun handleLogout(
        authHeader: String?,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<Unit> {
        val token = extractBearerToken(authHeader)
            ?: return ServerResponse(false, 401, null, "UNAUTHORIZED", "Missing Bearer token.", requestId)
        return authService.logout(token, requestId)
    }

    suspend fun handleGetMe(
        authHeader: String?,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<*> {
        val token = extractBearerToken(authHeader)
            ?: return ServerResponse(false, 401, null, "UNAUTHORIZED", "Missing Bearer token.", requestId)
        return authService.getMe(token, requestId)
    }

    suspend fun handleChangePassword(
        authHeader: String?,
        request: ChangePasswordRequest,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<Unit> {
        val token = extractBearerToken(authHeader)
            ?: return ServerResponse(false, 401, null, "UNAUTHORIZED", "Missing Bearer token.", requestId)
        return authService.changePassword(token, request, requestId)
    }

    /**
     * RBAC Protected Endpoint: Admin Only
     */
    suspend fun handleAdminProbe(
        authHeader: String?,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<String> {
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        try {
            MahakalRbac.requireRole(context, AccountRole.ADMIN)
            MahakalRbac.requirePermission(context, Permission.ADMIN_MANAGE_AGENTS)
            return ServerResponse(true, 200, "ADMIN_ACCESS_GRANTED", null, null, requestId)
        } catch (e: SecurityException) {
            logPermissionDenied(context, "ADMIN_PROBE", requestId)
            return ServerResponse(false, 403, null, "FORBIDDEN", e.message, requestId)
        }
    }

    /**
     * RBAC Protected Endpoint: Agent Only
     */
    suspend fun handleAgentProbe(
        authHeader: String?,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<String> {
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        try {
            MahakalRbac.requireRole(context, AccountRole.AGENT)
            MahakalRbac.requirePermission(context, Permission.AGENT_VIEW_OWN_USERS)
            return ServerResponse(true, 200, "AGENT_ACCESS_GRANTED", null, null, requestId)
        } catch (e: SecurityException) {
            logPermissionDenied(context, "AGENT_PROBE", requestId)
            return ServerResponse(false, 403, null, "FORBIDDEN", e.message, requestId)
        }
    }

    /**
     * RBAC Protected Endpoint: Hierarchical Ownership Verification
     * Verifies that the target user resource belongs strictly to the authenticated Agent.
     */
    suspend fun handleAgentUserAccess(
        authHeader: String?,
        targetUserId: String,
        targetUserAgentId: String,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<String> {
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        try {
            MahakalRbac.requireRole(context, AccountRole.AGENT)
            val isOwner = MahakalRbac.validateResourceOwnership(
                context = context,
                resourceOwnerId = targetUserId,
                resourceAgentId = targetUserAgentId
            )
            if (!isOwner) {
                logPermissionDenied(context, "AGENT_HIERARCHICAL_CHECK_FAILED", requestId)
                return ServerResponse(
                    false,
                    403,
                    null,
                    "FORBIDDEN",
                    "Agent is not authorized to access resources belonging to other agents.",
                    requestId
                )
            }
            return ServerResponse(true, 200, "AGENT_RESOURCE_ACCESS_GRANTED", null, null, requestId)
        } catch (e: SecurityException) {
            logPermissionDenied(context, "AGENT_HIERARCHICAL_ACCESS", requestId)
            return ServerResponse(false, 403, null, "FORBIDDEN", e.message, requestId)
        }
    }

    /**
     * RBAC Protected Endpoint: User Ownership Verification
     * Verifies that the user can only access their own profile/resource.
     */
    suspend fun handleUserResourceAccess(
        authHeader: String?,
        targetUserId: String,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<String> {
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        try {
            val isOwner = MahakalRbac.validateResourceOwnership(
                context = context,
                resourceOwnerId = targetUserId
            )
            if (!isOwner) {
                logPermissionDenied(context, "USER_OWNERSHIP_MISMATCH", requestId)
                return ServerResponse(
                    false,
                    403,
                    null,
                    "FORBIDDEN",
                    "User is not authorized to access resources of other users.",
                    requestId
                )
            }
            return ServerResponse(true, 200, "USER_RESOURCE_ACCESS_GRANTED", null, null, requestId)
        } catch (e: SecurityException) {
            return ServerResponse(false, 403, null, "FORBIDDEN", e.message, requestId)
        }
    }

    // =========================================================================
    // PHASE 3 — ADMIN -> AGENT MANAGEMENT ENDPOINTS
    // =========================================================================

    /**
     * POST /admin/agents
     */
    suspend fun handleCreateAgent(
        authHeader: String?,
        request: CreateAgentRequest,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<AgentCreatedDto> {
        val service = adminAgentService ?: return serviceUnavailable(requestId)
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        return service.createAgent(context, request, requestId)
    }

    /**
     * GET /admin/agents
     */
    suspend fun handleGetAgents(
        authHeader: String?,
        page: Int = 1,
        limit: Int = 20,
        status: String? = null,
        search: String? = null,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<PaginatedAgentListResponse> {
        val service = adminAgentService ?: return serviceUnavailable(requestId)
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        return service.getAgents(context, page, limit, status, search, requestId)
    }

    /**
     * GET /admin/agents/{id}
     */
    suspend fun handleGetAgentDetails(
        authHeader: String?,
        agentId: String,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<AgentDetailsDto> {
        val service = adminAgentService ?: return serviceUnavailable(requestId)
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        return service.getAgentDetails(context, agentId, requestId)
    }

    /**
     * PATCH /admin/agents/{id}
     */
    suspend fun handleUpdateAgent(
        authHeader: String?,
        agentId: String,
        request: EditAgentRequest,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<AgentSummaryDto> {
        val service = adminAgentService ?: return serviceUnavailable(requestId)
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        return service.updateAgent(context, agentId, request, requestId)
    }

    /**
     * POST /admin/agents/{id}/suspend
     */
    suspend fun handleSuspendAgent(
        authHeader: String?,
        agentId: String,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<Unit> {
        val service = adminAgentService ?: return serviceUnavailable(requestId)
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        return service.suspendAgent(context, agentId, requestId)
    }

    /**
     * POST /admin/agents/{id}/activate
     */
    suspend fun handleActivateAgent(
        authHeader: String?,
        agentId: String,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<Unit> {
        val service = adminAgentService ?: return serviceUnavailable(requestId)
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        return service.activateAgent(context, agentId, requestId)
    }

    /**
     * POST /admin/agents/{id}/reset-password
     */
     suspend fun handleResetAgentPassword(
         authHeader: String?,
         agentId: String,
         request: ResetAgentPasswordRequest,
         requestId: String = UUID.randomUUID().toString()
     ): ServerResponse<ResetPasswordResponse> {
         val service = adminAgentService ?: return serviceUnavailable(requestId)
         val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
         return service.resetPassword(context, agentId, request, requestId)
     }

    // =========================================================================
    // PHASE 4 — AGENT -> USER CREATION & MANAGEMENT ENDPOINTS
    // =========================================================================

    /**
     * POST /agent/users
     * Creates a new User owned strictly by the authenticated Agent.
     */
    suspend fun handleCreateUser(
        authHeader: String?,
        request: CreateUserRequest,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<UserCreatedDto> {
        val service = agentUserService ?: return userManagementUnavailable(requestId)
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        return service.createUser(context, request, requestId)
    }

    /**
     * GET /agent/users
     * Returns paginated list of Users owned by the authenticated Agent.
     */
    suspend fun handleGetUsers(
        authHeader: String?,
        page: Int = 1,
        limit: Int = 20,
        status: String? = null,
        search: String? = null,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<PaginatedUserListResponse> {
        val service = agentUserService ?: return userManagementUnavailable(requestId)
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        return service.getUsers(context, page, limit, status, search, requestId)
    }

    /**
     * GET /agent/users/{id}
     * Returns User details with strict ownership verification.
     */
    suspend fun handleGetUserDetails(
        authHeader: String?,
        userId: String,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<UserDetailsDto> {
        val service = agentUserService ?: return userManagementUnavailable(requestId)
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        return service.getUserDetails(context, userId, requestId)
    }

    /**
     * PATCH /agent/users/{id}
     * Updates permitted profile fields for the target User.
     */
    suspend fun handleUpdateUser(
        authHeader: String?,
        userId: String,
        request: EditUserRequest,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<UserSummaryDto> {
        val service = agentUserService ?: return userManagementUnavailable(requestId)
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        return service.updateUser(context, userId, request, requestId)
    }

    /**
     * POST /agent/users/{id}/suspend
     * Suspends User and revokes all active sessions immediately.
     */
    suspend fun handleSuspendUser(
        authHeader: String?,
        userId: String,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<Unit> {
        val service = agentUserService ?: return userManagementUnavailable(requestId)
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        return service.suspendUser(context, userId, requestId)
    }

    /**
     * POST /agent/users/{id}/activate
     * Restores User status to ACTIVE.
     */
    suspend fun handleActivateUser(
        authHeader: String?,
        userId: String,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<Unit> {
        val service = agentUserService ?: return userManagementUnavailable(requestId)
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        return service.activateUser(context, userId, requestId)
    }

    /**
     * POST /agent/users/{id}/reset-password
     * Resets User password and revokes all active sessions.
     */
    suspend fun handleResetUserPassword(
        authHeader: String?,
        userId: String,
        request: ResetUserPasswordRequest,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<ResetPasswordResponse> {
        val service = agentUserService ?: return userManagementUnavailable(requestId)
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        return service.resetPassword(context, userId, request, requestId)
    }

    // =========================================================================
    // ADMIN SUPERVISORY USER MANAGEMENT ENDPOINTS
    // =========================================================================

    suspend fun handleAdminGetUsers(
        authHeader: String?,
        page: Int = 1,
        limit: Int = 20,
        status: String? = null,
        search: String? = null,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<PaginatedUserListResponse> {
        val service = agentUserService ?: return userManagementUnavailable(requestId)
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        return service.getUsers(context, page, limit, status, search, requestId)
    }

    suspend fun handleAdminGetUserDetails(
        authHeader: String?,
        userId: String,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<UserDetailsDto> {
        val service = agentUserService ?: return userManagementUnavailable(requestId)
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        return service.getUserDetails(context, userId, requestId)
    }

    suspend fun handleAdminUpdateUser(
        authHeader: String?,
        userId: String,
        request: EditUserRequest,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<UserSummaryDto> {
        val service = agentUserService ?: return userManagementUnavailable(requestId)
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        return service.updateUser(context, userId, request, requestId)
    }

    suspend fun handleAdminSuspendUser(
        authHeader: String?,
        userId: String,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<Unit> {
        val service = agentUserService ?: return userManagementUnavailable(requestId)
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        return service.suspendUser(context, userId, requestId)
    }

    suspend fun handleAdminActivateUser(
        authHeader: String?,
        userId: String,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<Unit> {
        val service = agentUserService ?: return userManagementUnavailable(requestId)
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        return service.activateUser(context, userId, requestId)
    }

    suspend fun handleAdminResetUserPassword(
        authHeader: String?,
        userId: String,
        request: ResetUserPasswordRequest,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<ResetPasswordResponse> {
        val service = agentUserService ?: return userManagementUnavailable(requestId)
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        return service.resetPassword(context, userId, request, requestId)
    }

    // =========================================================================
    // PHASE 5 — VIRTUAL COIN LEDGER & TRANSACTION ENDPOINTS
    // =========================================================================

    /**
     * GET /wallets/me
     * Returns authenticated actor's wallet balance.
     */
    suspend fun handleGetMyWalletBalance(
        authHeader: String?,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<WalletBalanceDto> {
        val service = walletTransactionService ?: return walletServiceUnavailable(requestId)
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        return service.getMyWalletBalance(context, requestId)
    }

    /**
     * GET /wallets/{walletId}
     * Returns wallet details with strict ownership verification.
     */
    suspend fun handleGetWalletById(
        authHeader: String?,
        walletId: String,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<WalletDto> {
        val service = walletTransactionService ?: return walletServiceUnavailable(requestId)
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        return service.getWalletById(context, walletId, requestId)
    }

    /**
     * POST /wallets/transfer
     * Transfers non-monetary virtual coins atomically between authorized accounts.
     */
    suspend fun handleTransferCoins(
        authHeader: String?,
        request: TransferCoinsRequest,
        idempotencyKey: String?,
        clientIp: String? = null,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<TransactionDto> {
        val service = walletTransactionService ?: return walletServiceUnavailable(requestId)
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        return service.transferCoins(context, request, idempotencyKey, clientIp, requestId)
    }

    /**
     * POST /wallets/deduct
     * Controlled deduction with hierarchy ownership check.
     */
    suspend fun handleDeductCoins(
        authHeader: String?,
        request: DeductCoinsRequest,
        idempotencyKey: String?,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<TransactionDto> {
        val service = walletTransactionService ?: return walletServiceUnavailable(requestId)
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        return service.deductCoins(context, request, idempotencyKey, requestId)
    }

    /**
     * POST /transactions/{id}/reverse
     * Admin-only transaction reversal creating compensating transaction.
     */
    suspend fun handleReverseTransaction(
        authHeader: String?,
        transactionId: String,
        request: ReverseTransactionRequest,
        idempotencyKey: String?,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<TransactionDto> {
        val service = walletTransactionService ?: return walletServiceUnavailable(requestId)
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        return service.reverseTransaction(context, transactionId, request.reason, idempotencyKey, requestId)
    }

    /**
     * GET /transactions
     * Paginated transaction ledger scoped strictly by actor role and ownership.
     */
    suspend fun handleGetTransactions(
        authHeader: String?,
        page: Int = 1,
        limit: Int = 20,
        type: String? = null,
        status: String? = null,
        search: String? = null,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<PaginatedTransactionListResponse> {
        val service = walletTransactionService ?: return walletServiceUnavailable(requestId)
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        return service.getTransactions(context, page, limit, type, status, search, requestId)
    }

    /**
     * GET /transactions/{id}
     * Returns transaction details with strict ownership verification.
     */
    suspend fun handleGetTransactionById(
        authHeader: String?,
        transactionId: String,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<TransactionDto> {
        val service = walletTransactionService ?: return walletServiceUnavailable(requestId)
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        return service.getTransactionById(context, transactionId, requestId)
    }

    /**
     * GET /wallets/accounts/{accountId}/summary
     * Returns authoritative coin summary for an Agent or User.
     */
    suspend fun handleGetAccountCoinSummary(
        authHeader: String?,
        targetAccountId: String,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<AccountCoinSummaryDto> {
        val service = walletTransactionService ?: return walletServiceUnavailable(requestId)
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        return service.getAccountCoinSummary(context, targetAccountId, requestId)
    }

    /**
     * GET /wallets/accounts/balances
     * Returns batch balances map for directory screens.
     */
    suspend fun handleGetAccountBalances(
        authHeader: String?,
        accountIds: List<String>,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<BatchBalancesResponse> {
        val service = walletTransactionService ?: return walletServiceUnavailable(requestId)
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        return service.getAccountBalances(context, accountIds, requestId)
    }

    // ==========================================
    // PHASE 7: Game & Prediction Engine Handlers
    // ==========================================

    suspend fun handleCreateGame(
        authHeader: String?,
        request: CreateGameRequest,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<GameDto> {
        val service = gameEngineService ?: return gameServiceUnavailable(requestId)
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        return service.createGame(context, request, requestId)
    }

    suspend fun handleOpenGame(
        authHeader: String?,
        gameId: String,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<GameDto> {
        val service = gameEngineService ?: return gameServiceUnavailable(requestId)
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        return service.openGame(context, gameId, requestId)
    }

    suspend fun handleCloseGame(
        authHeader: String?,
        gameId: String,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<GameDto> {
        val service = gameEngineService ?: return gameServiceUnavailable(requestId)
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        return service.closeGame(context, gameId, requestId)
    }

    suspend fun handleCancelGame(
        authHeader: String?,
        gameId: String,
        request: CancelGameRequest,
        idempotencyKey: String?,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<GameDto> {
        val service = gameEngineService ?: return gameServiceUnavailable(requestId)
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        return service.cancelGame(context, gameId, request, idempotencyKey, requestId)
    }

    suspend fun handleGetGames(
        authHeader: String?,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<GameListResponse> {
        val service = gameEngineService ?: return gameServiceUnavailable(requestId)
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        return service.getGames(context, requestId)
    }

    suspend fun handleGetGameDetails(
        authHeader: String?,
        gameId: String,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<GameDto> {
        val service = gameEngineService ?: return gameServiceUnavailable(requestId)
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        return service.getGameDetails(context, gameId, requestId)
    }

    suspend fun handleSubmitGameEntry(
        authHeader: String?,
        gameId: String,
        request: GameEntryRequest,
        idempotencyKey: String?,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<GameEntryDto> {
        val service = gameEngineService ?: return gameServiceUnavailable(requestId)
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        return service.submitGameEntry(context, gameId, request, idempotencyKey, requestId)
    }

    suspend fun handleFinalizeGameResult(
        authHeader: String?,
        gameId: String,
        request: FinalizeResultRequest,
        idempotencyKey: String?,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<GameResultDto> {
        val service = gameEngineService ?: return gameServiceUnavailable(requestId)
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        return service.finalizeGameResult(context, gameId, request, idempotencyKey, requestId)
    }

    suspend fun handleGetMyEntries(
        authHeader: String?,
        limit: Int = 100,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<UserEntriesResponse> {
        val service = gameEngineService ?: return gameServiceUnavailable(requestId)
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        return service.getMyEntries(context, limit, requestId)
    }

    suspend fun handleGetGameEntriesAdmin(
        authHeader: String?,
        gameId: String,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<UserEntriesResponse> {
        val service = gameEngineService ?: return gameServiceUnavailable(requestId)
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        return service.getGameEntriesAdmin(context, gameId, requestId)
    }

    suspend fun handleGetAgentSubordinatedUserEntries(
        authHeader: String?,
        userId: String,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<UserEntriesResponse> {
        val service = gameEngineService ?: return gameServiceUnavailable(requestId)
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        return service.getAgentSubordinatedUserEntries(context, userId, requestId)
    }

    // ==========================================
    // Phase 8: Admin Game Operations Endpoints
    // ==========================================

    suspend fun handleGetAdminOperationsSummary(
        authHeader: String?,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<AdminOperationsSummaryDto> {
        val service = gameEngineService ?: return gameServiceUnavailable(requestId)
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        return service.getAdminOperationsSummary(context, requestId)
    }

    suspend fun handleGetAdminGamesPaginated(
        authHeader: String?,
        status: String? = null,
        gameType: String? = null,
        search: String? = null,
        page: Int = 1,
        limit: Int = 20,
        sortBy: String = "newest",
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<PaginatedAdminGameListResponse> {
        val service = gameEngineService ?: return gameServiceUnavailable(requestId)
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        return service.getAdminGamesPaginated(
            context = context,
            page = page,
            limit = limit,
            status = status,
            gameType = gameType,
            search = search,
            sortBy = sortBy,
            requestId = requestId
        )
    }

    suspend fun handleGetAdminGameDetails(
        authHeader: String?,
        gameId: String,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<AdminGameDetailsDto> {
        val service = gameEngineService ?: return gameServiceUnavailable(requestId)
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        return service.getAdminGameDetails(context, gameId, requestId)
    }

    suspend fun handleGetAdminGameEntriesPaginated(
        authHeader: String?,
        gameId: String,
        status: String? = null,
        page: Int = 1,
        limit: Int = 25,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<AdminGameEntriesResponse> {
        val service = gameEngineService ?: return gameServiceUnavailable(requestId)
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        return service.getAdminGameEntriesPaginated(
            context = context,
            gameId = gameId,
            page = page,
            limit = limit,
            status = status,
            requestId = requestId
        )
    }

    suspend fun handleRetryFailedProcessing(
        authHeader: String?,
        gameId: String,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<GameProcessingDto> {
        val service = gameEngineService ?: return gameServiceUnavailable(requestId)
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        return service.retryFailedProcessing(context, gameId, requestId)
    }

    suspend fun handleReconcileGameRewards(
        authHeader: String?,
        gameId: String,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<AdminReconciliationReportDto> {
        val service = gameEngineService ?: return gameServiceUnavailable(requestId)
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        return service.reconcileGameRewards(context, gameId, requestId)
    }

    suspend fun handleCorrectReconciliationDiscrepancies(
        authHeader: String?,
        gameId: String,
        idempotencyKey: String? = null,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<AdminReconciliationReportDto> {
        val service = gameEngineService ?: return gameServiceUnavailable(requestId)
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        val request = com.example.backend.model.ReconciliationCorrectionRequest(reason = "Administrative correction")
        return service.correctReconciliationDiscrepancies(context, gameId, request, requestId)
    }

    suspend fun handleGetTransactionByReference(
        authHeader: String?,
        reference: String,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<TransactionDetailDto> {
        val service = gameEngineService ?: return gameServiceUnavailable(requestId)
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        return service.getTransactionByReference(context, reference, requestId)
    }

    suspend fun handleGetAuditLogsAdmin(
        authHeader: String?,
        actorId: String? = null,
        action: String? = null,
        targetId: String? = null,
        startTime: Long? = null,
        endTime: Long? = null,
        page: Int = 1,
        limit: Int = 50,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<AuditLogListResponse> {
        val service = gameEngineService ?: return gameServiceUnavailable(requestId)
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        return service.getAuditLogsAdmin(
            context = context,
            page = page,
            limit = limit,
            actorId = actorId,
            action = action,
            targetId = targetId,
            startDate = startTime,
            endDate = endTime,
            requestId = requestId
        )
    }

    suspend fun handleGetAdminGamesReport(
        authHeader: String?,
        startTime: Long? = null,
        endTime: Long? = null,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<GamesReportDto> {
        val service = gameEngineService ?: return gameServiceUnavailable(requestId)
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        return service.getAdminGamesReport(context, startTime, endTime, requestId)
    }

    suspend fun handleGetAdminEntriesReport(
        authHeader: String?,
        startTime: Long? = null,
        endTime: Long? = null,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<EntriesReportDto> {
        val service = gameEngineService ?: return gameServiceUnavailable(requestId)
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        return service.getAdminEntriesReport(context, startTime, endTime, requestId)
    }

    suspend fun handleGetAdminTransactionsReport(
        authHeader: String?,
        startTime: Long? = null,
        endTime: Long? = null,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<TransactionsReportDto> {
        val service = gameEngineService ?: return gameServiceUnavailable(requestId)
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        return service.getAdminTransactionsReport(context, startTime, endTime, requestId)
    }

    suspend fun handleExportDataCsv(
        authHeader: String?,
        exportType: String,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<String> {
        val service = gameEngineService ?: return gameServiceUnavailable(requestId)
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        return service.exportDataCsv(context, exportType, requestId)
    }

    private fun <T> gameServiceUnavailable(requestId: String): ServerResponse<T> {
        return ServerResponse(
            success = false,
            statusCode = 500,
            data = null,
            errorCode = "SERVICE_UNAVAILABLE",
            message = "Game Engine Service is not configured.",
            requestId = requestId
        )
    }

    private fun <T> walletServiceUnavailable(requestId: String): ServerResponse<T> {
        return ServerResponse(
            success = false,
            statusCode = 500,
            data = null,
            errorCode = "SERVICE_UNAVAILABLE",
            message = "Wallet Transaction Service is not configured.",
            requestId = requestId
        )
    }

    private fun <T> userManagementUnavailable(requestId: String): ServerResponse<T> {
        return ServerResponse(
            success = false,
            statusCode = 500,
            data = null,
            errorCode = "SERVICE_UNAVAILABLE",
            message = "Agent User Service is not configured.",
            requestId = requestId
        )
    }

    // ==========================================
    // NOTIFICATIONS & EVENT DELIVERY ENDPOINTS
    // ==========================================

    suspend fun handleGetNotifications(
        authHeader: String?,
        page: Int = 1,
        limit: Int = 20,
        status: String? = null,
        type: String? = null,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<NotificationListResponse> {
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        val notifService = notificationService ?: return notificationServiceUnavailable(requestId)
        return notifService.getNotifications(context, page, limit, status, type, requestId)
    }

    suspend fun handleGetNotificationDetails(
        authHeader: String?,
        notificationId: String,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<NotificationDto> {
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        val notifService = notificationService ?: return notificationServiceUnavailable(requestId)
        return notifService.getNotificationDetails(context, notificationId, requestId)
    }

    suspend fun handleMarkNotificationRead(
        authHeader: String?,
        notificationId: String,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<Boolean> {
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        val notifService = notificationService ?: return notificationServiceUnavailable(requestId)
        return notifService.markAsRead(context, notificationId, requestId)
    }

    suspend fun handleMarkAllNotificationsRead(
        authHeader: String?,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<Int> {
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        val notifService = notificationService ?: return notificationServiceUnavailable(requestId)
        return notifService.markAllAsRead(context, requestId)
    }

    suspend fun handleArchiveNotification(
        authHeader: String?,
        notificationId: String,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<Boolean> {
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        val notifService = notificationService ?: return notificationServiceUnavailable(requestId)
        return notifService.archiveNotification(context, notificationId, requestId)
    }

    suspend fun handleGetUnreadNotificationCount(
        authHeader: String?,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<UnreadCountResponse> {
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        val notifService = notificationService ?: return notificationServiceUnavailable(requestId)
        return notifService.getUnreadCount(context, requestId)
    }

    suspend fun handleGetNotificationPreferences(
        authHeader: String?,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<NotificationPreferenceDto> {
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        val notifService = notificationService ?: return notificationServiceUnavailable(requestId)
        return notifService.getPreferences(context, requestId)
    }

    suspend fun handleUpdateNotificationPreferences(
        authHeader: String?,
        request: UpdateNotificationPreferenceRequest,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<NotificationPreferenceDto> {
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        val notifService = notificationService ?: return notificationServiceUnavailable(requestId)
        return notifService.updatePreferences(context, request, requestId)
    }

    suspend fun handleRegisterDeviceSession(
        authHeader: String?,
        request: RegisterDeviceRequest,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<Boolean> {
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        val notifService = notificationService ?: return notificationServiceUnavailable(requestId)
        return notifService.registerDevice(context, request, requestId)
    }

    suspend fun handleRevokeDeviceSession(
        authHeader: String?,
        deviceId: String,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<Boolean> {
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        val notifService = notificationService ?: return notificationServiceUnavailable(requestId)
        return notifService.revokeDevice(context, deviceId, requestId)
    }

    // =========================================================================
    // Phase 10: Security Hardening, Audit & System Monitoring Endpoints
    // =========================================================================

    suspend fun handleGetSecurityDashboardSummary(
        authHeader: String?,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<SecurityDashboardSummaryDto> {
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        try {
            MahakalRbac.requireRole(context, AccountRole.ADMIN)
        } catch (e: SecurityException) {
            logPermissionDenied(context, "GET_SECURITY_DASHBOARD", requestId)
            return ServerResponse(false, 403, null, "FORBIDDEN", e.message, requestId)
        }
        val hardeningService = securityHardeningService ?: return securityServiceUnavailable(requestId)
        return hardeningService.getSecurityDashboardSummary(context, requestId)
    }

    suspend fun handleQuerySecurityEvents(
        authHeader: String?,
        eventType: String? = null,
        actorId: String? = null,
        page: Int = 1,
        limit: Int = 20,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<SecurityEventsQueryResponse> {
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        try {
            MahakalRbac.requireRole(context, AccountRole.ADMIN)
        } catch (e: SecurityException) {
            logPermissionDenied(context, "QUERY_SECURITY_EVENTS", requestId)
            return ServerResponse(false, 403, null, "FORBIDDEN", e.message, requestId)
        }
        val hardeningService = securityHardeningService ?: return securityServiceUnavailable(requestId)
        return hardeningService.querySecurityEvents(context, eventType, actorId, page, limit, requestId)
    }

    suspend fun handleGetActiveSessions(
        authHeader: String?,
        targetAccountId: String? = null,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<List<SessionInfoDto>> {
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        // Only Admin can view all sessions or other user sessions
        if (targetAccountId != null && targetAccountId != context.accountId && context.role != AccountRole.ADMIN) {
            securityHardeningService?.logIdorAttempt(context, "ACTIVE_SESSIONS:$targetAccountId", requestId)
            return ServerResponse(false, 403, null, "FORBIDDEN", "Unauthorized account access.", requestId)
        }
        val hardeningService = securityHardeningService ?: return securityServiceUnavailable(requestId)
        return hardeningService.getActiveSessions(context, targetAccountId ?: if (context.role != AccountRole.ADMIN) context.accountId else null, requestId)
    }

    suspend fun handleRevokeSession(
        authHeader: String?,
        sessionId: String,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<Boolean> {
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        val hardeningService = securityHardeningService ?: return securityServiceUnavailable(requestId)
        return hardeningService.revokeSession(context, sessionId, requestId)
    }

    suspend fun handleRevokeAllAccountSessions(
        authHeader: String?,
        targetAccountId: String,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<Boolean> {
        val context = authenticate(authHeader, requestId) ?: return unauthorizedResponse(requestId)
        if (targetAccountId != context.accountId && context.role != AccountRole.ADMIN) {
            securityHardeningService?.logIdorAttempt(context, "REVOKE_ALL_SESSIONS:$targetAccountId", requestId)
            return ServerResponse(false, 403, null, "FORBIDDEN", "Unauthorized account access.", requestId)
        }
        val hardeningService = securityHardeningService ?: return securityServiceUnavailable(requestId)
        return hardeningService.revokeAllAccountSessions(context, targetAccountId, requestId)
    }

    suspend fun handleHealthCheck(): HealthCheckDto {
        return securityHardeningService?.checkHealth()
            ?: HealthCheckDto(status = "OK", timestamp = System.currentTimeMillis())
    }

    suspend fun handleReadinessCheck(): ReadinessCheckDto {
        return securityHardeningService?.checkReadiness()
            ?: ReadinessCheckDto(ready = true, timestamp = System.currentTimeMillis(), database = "READY", outboxWorker = "READY")
    }

    private fun <T> securityServiceUnavailable(requestId: String): ServerResponse<T> {
        return ServerResponse(
            success = false,
            statusCode = 500,
            data = null,
            errorCode = "SERVICE_UNAVAILABLE",
            message = "Security Hardening Service is not configured.",
            requestId = requestId
        )
    }

    private fun <T> notificationServiceUnavailable(requestId: String): ServerResponse<T> {
        return ServerResponse(
            success = false,
            statusCode = 500,
            data = null,
            errorCode = "SERVICE_UNAVAILABLE",
            message = "Notification Service is not configured.",
            requestId = requestId
        )
    }

    private fun <T> serviceUnavailable(requestId: String): ServerResponse<T> {
        return ServerResponse(
            success = false,
            statusCode = 500,
            data = null,
            errorCode = "SERVICE_UNAVAILABLE",
            message = "Admin Agent Service is not configured.",
            requestId = requestId
        )
    }

    private suspend fun authenticate(authHeader: String?, requestId: String): SecurityContext? {
        val token = extractBearerToken(authHeader) ?: return null
        return authService.validateSession(token)
    }

    private fun extractBearerToken(authHeader: String?): String? {
        if (authHeader.isNullOrBlank()) return null
        return if (authHeader.startsWith("Bearer ", ignoreCase = true)) {
            authHeader.substring(7).trim()
        } else {
            authHeader.trim()
        }
    }

    private fun <T> unauthorizedResponse(requestId: String): ServerResponse<T> {
        return ServerResponse(
            success = false,
            statusCode = 401,
            data = null,
            errorCode = "UNAUTHORIZED",
            message = "Authentication required or session expired.",
            requestId = requestId
        )
    }

    private suspend fun logPermissionDenied(context: SecurityContext, resource: String, requestId: String) {
        auditService.logEvent(
            actorId = context.accountId,
            actorRole = context.role.name,
            action = AuditActions.PERMISSION_DENIED,
            targetId = resource,
            requestId = requestId,
            metadataJson = "{\"deniedAction\":\"$resource\"}"
        )
    }
}
