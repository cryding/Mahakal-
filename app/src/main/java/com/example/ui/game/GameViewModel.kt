package com.example.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.backend.model.AdminGameDetailsDto
import com.example.backend.model.AdminOperationsSummaryDto
import com.example.backend.model.AdminReconciliationReportDto
import com.example.backend.model.AuditLogDto
import com.example.backend.model.CreateGameRequest
import com.example.backend.model.EntriesReportDto
import com.example.backend.model.GameDto
import com.example.backend.model.GameEntryDto
import com.example.backend.model.GameProcessingDto
import com.example.backend.model.GameResultDto
import com.example.backend.model.GamesReportDto
import com.example.backend.model.TransactionDetailDto
import com.example.backend.model.TransactionsReportDto
import com.example.data.repository.GameRepository
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GameUiState(
    val games: List<GameDto> = emptyList(),
    val isLoadingGames: Boolean = false,
    val myEntries: List<GameEntryDto> = emptyList(),
    val isLoadingEntries: Boolean = false,
    val selectedGame: GameDto? = null,
    val selectedGameEntries: List<GameEntryDto> = emptyList(),
    val isLoadingGameEntries: Boolean = false,
    val isSubmitting: Boolean = false,
    val actionSuccessMessage: String? = null,
    val actionErrorMessage: String? = null,
    val lastSubmittedEntry: GameEntryDto? = null,

    // Phase 8 Admin Operations State
    val adminSummary: AdminOperationsSummaryDto? = null,
    val isLoadingSummary: Boolean = false,
    val adminGames: List<GameDto> = emptyList(),
    val adminGamesTotalCount: Int = 0,
    val adminGamesPage: Int = 1,
    val adminGamesPageSize: Int = 20,
    val adminGamesTotalPages: Int = 1,
    val adminGamesFilterStatus: String? = null,
    val adminGamesSearchQuery: String = "",
    val adminGamesSortBy: String = "newest",
    val isLoadingAdminGames: Boolean = false,
    val selectedGameDetails: AdminGameDetailsDto? = null,
    val isLoadingGameDetails: Boolean = false,
    val reconciliationReport: AdminReconciliationReportDto? = null,
    val isReconciling: Boolean = false,
    val isCorrectingReconciliation: Boolean = false,
    val isRetryingProcessing: Boolean = false,
    val selectedTransactionDetail: TransactionDetailDto? = null,
    val isLoadingTransactionDetail: Boolean = false,
    val adminAuditLogs: List<AuditLogDto> = emptyList(),
    val adminAuditLogsTotalCount: Int = 0,
    val isLoadingAuditLogs: Boolean = false,
    val gamesReport: GamesReportDto? = null,
    val entriesReport: EntriesReportDto? = null,
    val transactionsReport: TransactionsReportDto? = null,
    val isLoadingReports: Boolean = false,
    val exportedCsvData: String? = null,
    val isExportingCsv: Boolean = false
)

class GameViewModel(
    private val gameRepository: GameRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    init {
        loadGames()
        loadMyEntries()
    }

    fun loadGames() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingGames = true) }
            val response = gameRepository.getGames()
            if (response.success && response.data != null) {
                _uiState.update { it.copy(games = response.data.games, isLoadingGames = false) }
            } else {
                _uiState.update { it.copy(isLoadingGames = false) }
            }
        }
    }

    fun loadMyEntries() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingEntries = true) }
            val response = gameRepository.getMyEntries()
            if (response.success && response.data != null) {
                _uiState.update { it.copy(myEntries = response.data.entries, isLoadingEntries = false) }
            } else {
                _uiState.update { it.copy(isLoadingEntries = false) }
            }
        }
    }

    fun selectGame(gameId: String) {
        viewModelScope.launch {
            val cached = _uiState.value.games.find { it.gameId == gameId }
            _uiState.update { it.copy(selectedGame = cached) }
            val response = gameRepository.getGameDetails(gameId)
            if (response.success && response.data != null) {
                _uiState.update { it.copy(selectedGame = response.data) }
            }
        }
    }

    fun submitEntry(
        gameId: String,
        selectedOptionId: String,
        amount: Long,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, actionErrorMessage = null, actionSuccessMessage = null) }
            val idempotencyKey = UUID.randomUUID().toString()
            val response = gameRepository.submitGameEntry(gameId, selectedOptionId, amount, idempotencyKey)
            if (response.success && response.data != null) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        actionSuccessMessage = "Entry placed successfully with $amount virtual coins!",
                        lastSubmittedEntry = response.data
                    )
                }
                loadGames()
                loadMyEntries()
                onSuccess?.invoke()
            } else {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        actionErrorMessage = response.message ?: "Failed to place entry."
                    )
                }
            }
        }
    }

    fun createGame(request: CreateGameRequest, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, actionErrorMessage = null, actionSuccessMessage = null) }
            val response = gameRepository.createGame(request)
            if (response.success) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        actionSuccessMessage = "Game '${request.title}' created successfully!"
                    )
                }
                loadGames()
                onSuccess?.invoke()
            } else {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        actionErrorMessage = response.message ?: "Failed to create game."
                    )
                }
            }
        }
    }

    fun openGame(gameId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, actionErrorMessage = null, actionSuccessMessage = null) }
            val response = gameRepository.openGame(gameId)
            if (response.success) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        actionSuccessMessage = "Game opened for entries."
                    )
                }
                loadGames()
            } else {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        actionErrorMessage = response.message ?: "Failed to open game."
                    )
                }
            }
        }
    }

    fun closeGame(gameId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, actionErrorMessage = null, actionSuccessMessage = null) }
            val response = gameRepository.closeGame(gameId)
            if (response.success) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        actionSuccessMessage = "Game closed."
                    )
                }
                loadGames()
            } else {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        actionErrorMessage = response.message ?: "Failed to close game."
                    )
                }
            }
        }
    }

    fun cancelGame(gameId: String, reason: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, actionErrorMessage = null, actionSuccessMessage = null) }
            val idempotencyKey = UUID.randomUUID().toString()
            val response = gameRepository.cancelGame(gameId, reason, idempotencyKey)
            if (response.success) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        actionSuccessMessage = "Game cancelled and all confirmed entries refunded!"
                    )
                }
                loadGames()
            } else {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        actionErrorMessage = response.message ?: "Failed to cancel game."
                    )
                }
            }
        }
    }

    fun finalizeResult(
        gameId: String,
        winningOptionId: String,
        reason: String,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, actionErrorMessage = null, actionSuccessMessage = null) }
            val idempotencyKey = UUID.randomUUID().toString()
            val response = gameRepository.finalizeResult(gameId, winningOptionId, reason, idempotencyKey)
            if (response.success) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        actionSuccessMessage = "Result finalized! Virtual coin rewards distributed."
                    )
                }
                loadGames()
                onSuccess?.invoke()
            } else {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        actionErrorMessage = response.message ?: "Failed to finalize result."
                    )
                }
            }
        }
    }

    fun loadGameEntriesAdmin(gameId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingGameEntries = true) }
            val response = gameRepository.getGameEntriesAdmin(gameId)
            if (response.success && response.data != null) {
                _uiState.update { it.copy(selectedGameEntries = response.data.entries, isLoadingGameEntries = false) }
            } else {
                _uiState.update { it.copy(isLoadingGameEntries = false) }
            }
        }
    }

    fun loadAgentUserEntries(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingGameEntries = true) }
            val response = gameRepository.getAgentSubordinatedUserEntries(userId)
            if (response.success && response.data != null) {
                _uiState.update { it.copy(selectedGameEntries = response.data.entries, isLoadingGameEntries = false) }
            } else {
                _uiState.update { it.copy(isLoadingGameEntries = false) }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(actionSuccessMessage = null, actionErrorMessage = null) }
    }

    // ==========================================
    // Phase 8: Admin Game Operations Actions
    // ==========================================

    fun loadAdminSummary() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSummary = true) }
            val response = gameRepository.getAdminOperationsSummary()
            if (response.success && response.data != null) {
                _uiState.update { it.copy(adminSummary = response.data, isLoadingSummary = false) }
            } else {
                _uiState.update { it.copy(isLoadingSummary = false) }
            }
        }
    }

    fun loadAdminGames(
        status: String? = _uiState.value.adminGamesFilterStatus,
        search: String = _uiState.value.adminGamesSearchQuery,
        page: Int = _uiState.value.adminGamesPage,
        limit: Int = 20,
        sortBy: String = _uiState.value.adminGamesSortBy
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingAdminGames = true,
                    adminGamesFilterStatus = status,
                    adminGamesSearchQuery = search,
                    adminGamesPage = page,
                    adminGamesSortBy = sortBy
                )
            }
            val response = gameRepository.getAdminGamesPaginated(
                status = status,
                gameType = null,
                search = if (search.isBlank()) null else search,
                page = page,
                limit = limit,
                sortBy = sortBy
            )
            if (response.success && response.data != null) {
                _uiState.update {
                    it.copy(
                        adminGames = response.data.games,
                        adminGamesTotalCount = response.data.totalCount,
                        adminGamesPage = response.data.page,
                        adminGamesPageSize = response.data.pageSize,
                        adminGamesTotalPages = response.data.totalPages,
                        isLoadingAdminGames = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoadingAdminGames = false) }
            }
        }
    }

    fun loadAdminGameDetails(gameId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingGameDetails = true, reconciliationReport = null) }
            val response = gameRepository.getAdminGameDetails(gameId)
            if (response.success && response.data != null) {
                _uiState.update { it.copy(selectedGameDetails = response.data, isLoadingGameDetails = false) }
            } else {
                _uiState.update {
                    it.copy(
                        isLoadingGameDetails = false,
                        actionErrorMessage = response.message ?: "Failed to load game details."
                    )
                }
            }
        }
    }

    fun clearSelectedGameDetails() {
        _uiState.update { it.copy(selectedGameDetails = null, reconciliationReport = null) }
    }

    fun loadAdminGameEntries(
        gameId: String,
        status: String? = null,
        page: Int = 1,
        limit: Int = 25
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingGameEntries = true) }
            val response = gameRepository.getAdminGameEntriesPaginated(gameId, status, page, limit)
            if (response.success && response.data != null) {
                _uiState.update {
                    it.copy(
                        selectedGameEntries = response.data.entries,
                        isLoadingGameEntries = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoadingGameEntries = false) }
            }
        }
    }

    fun retryProcessing(gameId: String, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRetryingProcessing = true, actionErrorMessage = null, actionSuccessMessage = null) }
            val response = gameRepository.retryFailedProcessing(gameId)
            if (response.success && response.data != null) {
                _uiState.update {
                    it.copy(
                        isRetryingProcessing = false,
                        actionSuccessMessage = "Settlement processing retry triggered (status: ${response.data.status})."
                    )
                }
                loadAdminGameDetails(gameId)
                loadAdminSummary()
                onSuccess?.invoke()
            } else {
                _uiState.update {
                    it.copy(
                        isRetryingProcessing = false,
                        actionErrorMessage = response.message ?: "Failed to retry processing."
                    )
                }
            }
        }
    }

    fun reconcileGame(gameId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isReconciling = true, actionErrorMessage = null, actionSuccessMessage = null) }
            val response = gameRepository.reconcileGameRewards(gameId)
            if (response.success && response.data != null) {
                _uiState.update {
                    it.copy(
                        isReconciling = false,
                        reconciliationReport = response.data,
                        actionSuccessMessage = "Reconciliation complete. Status: ${response.data.reconciliationStatus}."
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isReconciling = false,
                        actionErrorMessage = response.message ?: "Failed to run reconciliation."
                    )
                }
            }
        }
    }

    fun correctDiscrepancies(gameId: String, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCorrectingReconciliation = true, actionErrorMessage = null, actionSuccessMessage = null) }
            val idempotencyKey = UUID.randomUUID().toString()
            val response = gameRepository.correctReconciliationDiscrepancies(gameId, idempotencyKey)
            if (response.success && response.data != null) {
                _uiState.update {
                    it.copy(
                        isCorrectingReconciliation = false,
                        reconciliationReport = response.data,
                        actionSuccessMessage = "Reconciliation corrections applied successfully! Status: ${response.data.reconciliationStatus}."
                    )
                }
                loadAdminGameDetails(gameId)
                loadAdminSummary()
                onSuccess?.invoke()
            } else {
                _uiState.update {
                    it.copy(
                        isCorrectingReconciliation = false,
                        actionErrorMessage = response.message ?: "Failed to correct discrepancies."
                    )
                }
            }
        }
    }

    fun loadTransactionDetail(reference: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingTransactionDetail = true, actionErrorMessage = null) }
            val response = gameRepository.getTransactionByReference(reference)
            if (response.success && response.data != null) {
                _uiState.update {
                    it.copy(
                        selectedTransactionDetail = response.data,
                        isLoadingTransactionDetail = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoadingTransactionDetail = false,
                        actionErrorMessage = response.message ?: "Transaction not found for reference '$reference'."
                    )
                }
            }
        }
    }

    fun clearTransactionDetail() {
        _uiState.update { it.copy(selectedTransactionDetail = null) }
    }

    fun loadAuditLogs(
        actorId: String? = null,
        action: String? = null,
        targetId: String? = null,
        page: Int = 1,
        limit: Int = 50
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingAuditLogs = true) }
            val response = gameRepository.getAuditLogsAdmin(
                actorId = actorId,
                action = action,
                targetId = targetId,
                startTime = null,
                endTime = null,
                page = page,
                limit = limit
            )
            if (response.success && response.data != null) {
                _uiState.update {
                    it.copy(
                        adminAuditLogs = response.data.logs,
                        adminAuditLogsTotalCount = response.data.totalCount,
                        isLoadingAuditLogs = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoadingAuditLogs = false) }
            }
        }
    }

    fun loadReports(startTime: Long? = null, endTime: Long? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingReports = true) }
            val gamesResp = gameRepository.getAdminGamesReport(startTime, endTime)
            val entriesResp = gameRepository.getAdminEntriesReport(startTime, endTime)
            val txsResp = gameRepository.getAdminTransactionsReport(startTime, endTime)

            _uiState.update {
                it.copy(
                    gamesReport = gamesResp.data,
                    entriesReport = entriesResp.data,
                    transactionsReport = txsResp.data,
                    isLoadingReports = false
                )
            }
        }
    }

    fun exportCsv(exportType: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExportingCsv = true, actionErrorMessage = null, actionSuccessMessage = null) }
            val response = gameRepository.exportDataCsv(exportType)
            if (response.success && response.data != null) {
                _uiState.update {
                    it.copy(
                        isExportingCsv = false,
                        exportedCsvData = response.data,
                        actionSuccessMessage = "Exported ${exportType.uppercase()} CSV data successfully (${response.data.lines().size} lines)."
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isExportingCsv = false,
                        actionErrorMessage = response.message ?: "Failed to export data."
                    )
                }
            }
        }
    }

    fun clearExportedCsv() {
        _uiState.update { it.copy(exportedCsvData = null) }
    }

    class Factory(private val gameRepository: GameRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GameViewModel(gameRepository) as T
        }
    }
}
