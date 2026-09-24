package com.example.data.repository

import com.example.backend.api.ServerApiRouter
import com.example.backend.model.CancelGameRequest
import com.example.backend.model.CreateGameRequest
import com.example.backend.model.FinalizeResultRequest
import com.example.backend.model.GameDto
import com.example.backend.model.GameEntryDto
import com.example.backend.model.GameEntryRequest
import com.example.backend.model.GameListResponse
import com.example.backend.model.GameResultDto
import com.example.backend.model.ServerResponse
import com.example.backend.model.UserEntriesResponse
import com.example.core.security.SecureTokenStorage

interface GameRepository {
    suspend fun getGames(): ServerResponse<GameListResponse>
    suspend fun getGameDetails(gameId: String): ServerResponse<GameDto>
    suspend fun submitGameEntry(gameId: String, selectedOptionId: String, amount: Long, idempotencyKey: String): ServerResponse<GameEntryDto>
    suspend fun getMyEntries(): ServerResponse<UserEntriesResponse>

    // Admin operations
    suspend fun createGame(request: CreateGameRequest): ServerResponse<GameDto>
    suspend fun openGame(gameId: String): ServerResponse<GameDto>
    suspend fun closeGame(gameId: String): ServerResponse<GameDto>
    suspend fun cancelGame(gameId: String, reason: String, idempotencyKey: String): ServerResponse<GameDto>
    suspend fun finalizeResult(gameId: String, winningOptionId: String, reason: String, idempotencyKey: String): ServerResponse<GameResultDto>
    suspend fun getGameEntriesAdmin(gameId: String): ServerResponse<UserEntriesResponse>

    // Agent operations
    suspend fun getAgentSubordinatedUserEntries(userId: String): ServerResponse<UserEntriesResponse>

    // Phase 8: Admin Game Operations
    suspend fun getAdminOperationsSummary(): ServerResponse<com.example.backend.model.AdminOperationsSummaryDto>
    suspend fun getAdminGamesPaginated(
        status: String? = null,
        gameType: String? = null,
        search: String? = null,
        page: Int = 1,
        limit: Int = 20,
        sortBy: String = "newest"
    ): ServerResponse<com.example.backend.model.PaginatedAdminGameListResponse>
    suspend fun getAdminGameDetails(gameId: String): ServerResponse<com.example.backend.model.AdminGameDetailsDto>
    suspend fun getAdminGameEntriesPaginated(
        gameId: String,
        status: String? = null,
        page: Int = 1,
        limit: Int = 25
    ): ServerResponse<com.example.backend.model.AdminGameEntriesResponse>
    suspend fun retryFailedProcessing(gameId: String): ServerResponse<com.example.backend.model.GameProcessingDto>
    suspend fun reconcileGameRewards(gameId: String): ServerResponse<com.example.backend.model.AdminReconciliationReportDto>
    suspend fun correctReconciliationDiscrepancies(gameId: String, idempotencyKey: String? = null): ServerResponse<com.example.backend.model.AdminReconciliationReportDto>
    suspend fun getTransactionByReference(reference: String): ServerResponse<com.example.backend.model.TransactionDetailDto>
    suspend fun getAuditLogsAdmin(
        actorId: String? = null,
        action: String? = null,
        targetId: String? = null,
        startTime: Long? = null,
        endTime: Long? = null,
        page: Int = 1,
        limit: Int = 50
    ): ServerResponse<com.example.backend.model.AuditLogListResponse>
    suspend fun getAdminGamesReport(startTime: Long? = null, endTime: Long? = null): ServerResponse<com.example.backend.model.GamesReportDto>
    suspend fun getAdminEntriesReport(startTime: Long? = null, endTime: Long? = null): ServerResponse<com.example.backend.model.EntriesReportDto>
    suspend fun getAdminTransactionsReport(startTime: Long? = null, endTime: Long? = null): ServerResponse<com.example.backend.model.TransactionsReportDto>
    suspend fun exportDataCsv(exportType: String): ServerResponse<String>
}

class GameRepositoryImpl(
    private val apiRouter: ServerApiRouter,
    private val secureStorage: SecureTokenStorage
) : GameRepository {

    private fun getAuthHeader(): String? {
        val token = secureStorage.getAccessToken() ?: return null
        return "Bearer $token"
    }

    private fun <T> unauthorized(): ServerResponse<T> {
        return ServerResponse(false, 401, null, "UNAUTHORIZED", "User is not authenticated.")
    }

    override suspend fun getGames(): ServerResponse<GameListResponse> {
        val authHeader = getAuthHeader() ?: return unauthorized()
        return apiRouter.handleGetGames(authHeader)
    }

    override suspend fun getGameDetails(gameId: String): ServerResponse<GameDto> {
        val authHeader = getAuthHeader() ?: return unauthorized()
        return apiRouter.handleGetGameDetails(authHeader, gameId)
    }

    override suspend fun submitGameEntry(
        gameId: String,
        selectedOptionId: String,
        amount: Long,
        idempotencyKey: String
    ): ServerResponse<GameEntryDto> {
        val authHeader = getAuthHeader() ?: return unauthorized()
        val request = GameEntryRequest(selectedOptionId, amount)
        return apiRouter.handleSubmitGameEntry(authHeader, gameId, request, idempotencyKey)
    }

    override suspend fun getMyEntries(): ServerResponse<UserEntriesResponse> {
        val authHeader = getAuthHeader() ?: return unauthorized()
        return apiRouter.handleGetMyEntries(authHeader)
    }

    override suspend fun createGame(request: CreateGameRequest): ServerResponse<GameDto> {
        val authHeader = getAuthHeader() ?: return unauthorized()
        return apiRouter.handleCreateGame(authHeader, request)
    }

    override suspend fun openGame(gameId: String): ServerResponse<GameDto> {
        val authHeader = getAuthHeader() ?: return unauthorized()
        return apiRouter.handleOpenGame(authHeader, gameId)
    }

    override suspend fun closeGame(gameId: String): ServerResponse<GameDto> {
        val authHeader = getAuthHeader() ?: return unauthorized()
        return apiRouter.handleCloseGame(authHeader, gameId)
    }

    override suspend fun cancelGame(
        gameId: String,
        reason: String,
        idempotencyKey: String
    ): ServerResponse<GameDto> {
        val authHeader = getAuthHeader() ?: return unauthorized()
        return apiRouter.handleCancelGame(authHeader, gameId, CancelGameRequest(reason), idempotencyKey)
    }

    override suspend fun finalizeResult(
        gameId: String,
        winningOptionId: String,
        reason: String,
        idempotencyKey: String
    ): ServerResponse<GameResultDto> {
        val authHeader = getAuthHeader() ?: return unauthorized()
        return apiRouter.handleFinalizeGameResult(authHeader, gameId, FinalizeResultRequest(winningOptionId, reason), idempotencyKey)
    }

    override suspend fun getGameEntriesAdmin(gameId: String): ServerResponse<UserEntriesResponse> {
        val authHeader = getAuthHeader() ?: return unauthorized()
        return apiRouter.handleGetGameEntriesAdmin(authHeader, gameId)
    }

    override suspend fun getAgentSubordinatedUserEntries(userId: String): ServerResponse<UserEntriesResponse> {
        val authHeader = getAuthHeader() ?: return unauthorized()
        return apiRouter.handleGetAgentSubordinatedUserEntries(authHeader, userId)
    }

    // Phase 8 implementations
    override suspend fun getAdminOperationsSummary(): ServerResponse<com.example.backend.model.AdminOperationsSummaryDto> {
        val authHeader = getAuthHeader() ?: return unauthorized()
        return apiRouter.handleGetAdminOperationsSummary(authHeader)
    }

    override suspend fun getAdminGamesPaginated(
        status: String?,
        gameType: String?,
        search: String?,
        page: Int,
        limit: Int,
        sortBy: String
    ): ServerResponse<com.example.backend.model.PaginatedAdminGameListResponse> {
        val authHeader = getAuthHeader() ?: return unauthorized()
        return apiRouter.handleGetAdminGamesPaginated(authHeader, status, gameType, search, page, limit, sortBy)
    }

    override suspend fun getAdminGameDetails(gameId: String): ServerResponse<com.example.backend.model.AdminGameDetailsDto> {
        val authHeader = getAuthHeader() ?: return unauthorized()
        return apiRouter.handleGetAdminGameDetails(authHeader, gameId)
    }

    override suspend fun getAdminGameEntriesPaginated(
        gameId: String,
        status: String?,
        page: Int,
        limit: Int
    ): ServerResponse<com.example.backend.model.AdminGameEntriesResponse> {
        val authHeader = getAuthHeader() ?: return unauthorized()
        return apiRouter.handleGetAdminGameEntriesPaginated(authHeader, gameId, status, page, limit)
    }

    override suspend fun retryFailedProcessing(gameId: String): ServerResponse<com.example.backend.model.GameProcessingDto> {
        val authHeader = getAuthHeader() ?: return unauthorized()
        return apiRouter.handleRetryFailedProcessing(authHeader, gameId)
    }

    override suspend fun reconcileGameRewards(gameId: String): ServerResponse<com.example.backend.model.AdminReconciliationReportDto> {
        val authHeader = getAuthHeader() ?: return unauthorized()
        return apiRouter.handleReconcileGameRewards(authHeader, gameId)
    }

    override suspend fun correctReconciliationDiscrepancies(
        gameId: String,
        idempotencyKey: String?
    ): ServerResponse<com.example.backend.model.AdminReconciliationReportDto> {
        val authHeader = getAuthHeader() ?: return unauthorized()
        return apiRouter.handleCorrectReconciliationDiscrepancies(authHeader, gameId, idempotencyKey)
    }

    override suspend fun getTransactionByReference(reference: String): ServerResponse<com.example.backend.model.TransactionDetailDto> {
        val authHeader = getAuthHeader() ?: return unauthorized()
        return apiRouter.handleGetTransactionByReference(authHeader, reference)
    }

    override suspend fun getAuditLogsAdmin(
        actorId: String?,
        action: String?,
        targetId: String?,
        startTime: Long?,
        endTime: Long?,
        page: Int,
        limit: Int
    ): ServerResponse<com.example.backend.model.AuditLogListResponse> {
        val authHeader = getAuthHeader() ?: return unauthorized()
        return apiRouter.handleGetAuditLogsAdmin(authHeader, actorId, action, targetId, startTime, endTime, page, limit)
    }

    override suspend fun getAdminGamesReport(
        startTime: Long?,
        endTime: Long?
    ): ServerResponse<com.example.backend.model.GamesReportDto> {
        val authHeader = getAuthHeader() ?: return unauthorized()
        return apiRouter.handleGetAdminGamesReport(authHeader, startTime, endTime)
    }

    override suspend fun getAdminEntriesReport(
        startTime: Long?,
        endTime: Long?
    ): ServerResponse<com.example.backend.model.EntriesReportDto> {
        val authHeader = getAuthHeader() ?: return unauthorized()
        return apiRouter.handleGetAdminEntriesReport(authHeader, startTime, endTime)
    }

    override suspend fun getAdminTransactionsReport(
        startTime: Long?,
        endTime: Long?
    ): ServerResponse<com.example.backend.model.TransactionsReportDto> {
        val authHeader = getAuthHeader() ?: return unauthorized()
        return apiRouter.handleGetAdminTransactionsReport(authHeader, startTime, endTime)
    }

    override suspend fun exportDataCsv(exportType: String): ServerResponse<String> {
        val authHeader = getAuthHeader() ?: return unauthorized()
        return apiRouter.handleExportDataCsv(authHeader, exportType)
    }
}
