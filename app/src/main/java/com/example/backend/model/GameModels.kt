package com.example.backend.model

/**
 * Valid states for the Game State Machine.
 */
object GameStatus {
    const val DRAFT = "DRAFT"
    const val SCHEDULED = "SCHEDULED"
    const val OPEN = "OPEN"
    const val CLOSED = "CLOSED"
    const val RESULT_PENDING = "RESULT_PENDING"
    const val RESULT_FINALIZED = "RESULT_FINALIZED"
    const val CANCELLED = "CANCELLED"
    const val ARCHIVED = "ARCHIVED"

    val ACTIVE_DISCOVERY_STATUSES = listOf(SCHEDULED, OPEN, CLOSED, RESULT_PENDING, RESULT_FINALIZED)
}

/**
 * Entry status values.
 */
object EntryStatus {
    const val CONFIRMED = "CONFIRMED"
    const val WON = "WON"
    const val LOST = "LOST"
    const val REFUNDED = "REFUNDED"
    const val CANCELLED = "CANCELLED"
}

data class GameOptionCreateRequest(
    val optionCode: String,
    val displayName: String,
    val metadataJson: String = "{}"
)

data class CreateGameRequest(
    val gameType: String = "PREDICTION",
    val title: String,
    val description: String,
    val options: List<GameOptionCreateRequest>,
    val startTime: Long,
    val entryDeadline: Long,
    val resultTime: Long,
    val minCoins: Long = 10L,
    val maxCoins: Long = 10_000L,
    val rewardMultiplier: Double = 2.0
)

data class GameOptionDto(
    val optionId: String,
    val gameId: String,
    val optionCode: String,
    val displayName: String,
    val status: String = "ACTIVE",
    val metadataJson: String = "{}"
)

data class GameResultDto(
    val resultId: String,
    val gameId: String,
    val winningOptionId: String,
    val winningOptionName: String? = null,
    val resultStatus: String = "FINALIZED",
    val finalizedBy: String,
    val finalizedAt: Long,
    val resultVersion: Long = 1L,
    val metadataJson: String = "{}",
    val totalWinners: Int = 0,
    val totalRewardsPaid: Long = 0L
)

data class GameDto(
    val gameId: String,
    val gameType: String,
    val title: String,
    val description: String,
    val status: String,
    val startTime: Long,
    val entryDeadline: Long,
    val resultTime: Long,
    val minCoins: Long,
    val maxCoins: Long,
    val rewardMultiplier: Double,
    val options: List<GameOptionDto> = emptyList(),
    val entryCount: Int = 0,
    val result: GameResultDto? = null,
    val createdBy: String,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Client request for POST /games/{gameId}/entries.
 * Identity (userId, walletId, balances) is strictly resolved server-side from session.
 */
data class GameEntryRequest(
    val selectedOptionId: String,
    val virtualCoinAmount: Long
)

data class GameEntryDto(
    val entryId: String,
    val gameId: String,
    val gameTitle: String? = null,
    val userId: String,
    val selectedOptionId: String,
    val selectedOptionName: String? = null,
    val virtualCoinAmount: Long,
    val status: String, // CONFIRMED, WON, LOST, REFUNDED, CANCELLED
    val idempotencyKey: String,
    val deductionTransactionId: String,
    val rewardTransactionId: String? = null,
    val rewardAmount: Long? = null,
    val createdAt: Long,
    val updatedAt: Long
)

data class FinalizeResultRequest(
    val winningOptionId: String,
    val reason: String = "Admin finalized winning result"
)

data class CancelGameRequest(
    val reason: String = "Game cancelled by administrator"
)

data class GameListResponse(
    val games: List<GameDto>
)

data class UserEntriesResponse(
    val entries: List<GameEntryDto>
)

data class GameEventDto(
    val eventId: String,
    val gameId: String,
    val eventType: String,
    val actorId: String,
    val actorRole: String,
    val timestamp: Long,
    val metadataJson: String = "{}",
    val correlationId: String? = null
)

data class GameProcessingDto(
    val processingId: String,
    val gameId: String,
    val status: String, // PENDING, PROCESSING, COMPLETED, FAILED, RETRY_PENDING
    val startedAt: Long,
    val completedAt: Long? = null,
    val attemptCount: Int = 1,
    val lastErrorCode: String? = null,
    val lastErrorMessage: String? = null,
    val correlationId: String? = null,
    val totalEntries: Int = 0,
    val processedEntries: Int = 0,
    val failedEntries: Int = 0,
    val updatedAt: Long
)

data class OptionStatisticsDto(
    val optionId: String,
    val optionCode: String,
    val displayName: String,
    val entriesCount: Int,
    val totalCoins: Long,
    val percentageOfTotalCoins: Double
)

data class GameEntryStatisticsDto(
    val totalEntries: Int,
    val totalCoinsEntered: Long,
    val confirmedEntries: Int,
    val winningEntries: Int,
    val losingEntries: Int,
    val refundedEntries: Int,
    val totalRewardsDistributed: Long,
    val optionsBreakdown: List<OptionStatisticsDto> = emptyList()
)

data class PaginatedAdminGameListResponse(
    val games: List<GameDto>,
    val totalCount: Int,
    val page: Int,
    val pageSize: Int,
    val totalPages: Int
)

data class PaginatedGameEntriesResponse(
    val entries: List<GameEntryDto>,
    val statistics: GameEntryStatisticsDto,
    val totalCount: Int,
    val page: Int,
    val pageSize: Int,
    val totalPages: Int
)

data class AdminGameDetailsDto(
    val game: GameDto,
    val processing: GameProcessingDto?,
    val statistics: GameEntryStatisticsDto,
    val auditEvents: List<GameEventDto>
)

data class AdminGameOperationsSummaryDto(
    val totalGames: Int,
    val scheduledGames: Int,
    val openGames: Int,
    val closedGames: Int,
    val resultPendingGames: Int,
    val finalizedGames: Int,
    val cancelledGames: Int,
    val processingFailures: Int,
    val totalVirtualCoinsCirculatingInGames: Long,
    val totalVirtualCoinsWon: Long,
    val activeParticipantsCount: Int,
    val recentEvents: List<GameEventDto>
) {
    val activeGames: Int get() = openGames + scheduledGames
    val completedGames: Int get() = finalizedGames
    val totalEntries: Int get() = activeParticipantsCount
    val totalCoinsEntered: Long get() = totalVirtualCoinsCirculatingInGames
    val totalCoinsDistributed: Long get() = totalVirtualCoinsWon
    val discrepanciesCount: Int get() = 0
    val pendingProcessingCount: Int get() = processingFailures
}

data class ReconciliationDiscrepancyDto(
    val entryId: String,
    val userId: String,
    val discrepancyType: String, // MISSING_REWARD_TRANSACTION, AMOUNT_MISMATCH, UNEXPECTED_REWARD, STATUS_INCONSISTENCY
    val expectedAmount: Long,
    val actualAmount: Long,
    val entryStatus: String,
    val transactionId: String? = null,
    val details: String
) {
    val ledgerAmount: Long get() = actualAmount
    val reason: String get() = details.ifBlank { discrepancyType }
}

data class RewardReconciliationResultDto(
    val gameId: String,
    val gameTitle: String,
    val gameStatus: String,
    val winningOptionId: String?,
    val winningOptionName: String?,
    val reconciliationStatus: String, // CONSISTENT, DISCREPANCY_DETECTED, PENDING_FINALIZATION
    val totalEligibleWinningEntries: Int,
    val totalMatchedRewardTransactions: Int,
    val totalExpectedRewardCoins: Long,
    val totalActualRewardedCoins: Long,
    val discrepancies: List<ReconciliationDiscrepancyDto> = emptyList(),
    val checkedAt: Long
) {
    val totalWinnersClaimed: Int get() = totalEligibleWinningEntries
    val totalExpectedRewards: Long get() = totalExpectedRewardCoins
    val totalPaidInLedger: Long get() = totalActualRewardedCoins
    val reconciledAt: Long get() = checkedAt
    val hasDiscrepancies: Boolean get() = discrepancies.isNotEmpty() || reconciliationStatus != "CONSISTENT"
}

data class ReconciliationCorrectionRequest(
    val reason: String = "Administrative reconciliation correction",
    val entryId: String? = null // Specific entry or null to fix all
)

data class ReconciliationCorrectionResponse(
    val correctedCount: Int,
    val totalCoinsAdjusted: Long,
    val auditEventId: String,
    val message: String
)

data class TransactionReferenceDto(
    val transaction: TransactionDto,
    val relatedGame: GameDto? = null,
    val relatedEntry: GameEntryDto? = null,
    val sourceAccountName: String? = null,
    val destinationAccountName: String? = null
)

data class AuditLogDto(
    val id: String,
    val actorId: String,
    val actorRole: String,
    val action: String,
    val targetId: String?,
    val targetType: String?,
    val requestId: String,
    val metadataJson: String,
    val beforeState: String?,
    val afterState: String?,
    val createdAt: Long
)

data class PaginatedAuditEventsResponse(
    val events: List<AuditLogDto>,
    val totalCount: Int,
    val page: Int,
    val pageSize: Int,
    val totalPages: Int
) {
    val logs: List<AuditLogDto> get() = events
}

data class GameReportRowDto(
    val gameId: String,
    val title: String,
    val status: String,
    val entryCount: Int,
    val totalCoinsEntered: Long,
    val totalRewardsPaid: Long,
    val netCoinDelta: Long,
    val winningOption: String?,
    val createdAt: Long
)

data class AdminGamesReportDto(
    val totalGamesAnalyzed: Int,
    val totalVolumeEntered: Long,
    val totalRewardsPaid: Long,
    val netPlatformVolume: Long,
    val gamesByStatus: Map<String, Int>,
    val gameRows: List<GameReportRowDto>
) {
    val totalGames: Int get() = totalGamesAnalyzed
    val activeGames: Int get() = (gamesByStatus[GameStatus.OPEN] ?: 0) + (gamesByStatus[GameStatus.SCHEDULED] ?: 0)
    val completedGames: Int get() = gamesByStatus[GameStatus.RESULT_FINALIZED] ?: 0
    val cancelledGames: Int get() = gamesByStatus[GameStatus.CANCELLED] ?: 0
    val totalCoinsEntered: Long get() = totalVolumeEntered
    val totalCoinsDistributed: Long get() = totalRewardsPaid
}

data class AdminEntriesReportDto(
    val totalEntries: Int,
    val confirmedEntries: Int,
    val winningEntries: Int,
    val losingEntries: Int,
    val refundedEntries: Int,
    val totalCoinsEntered: Long,
    val totalCoinsWon: Long,
    val averageEntryCoins: Double
)

data class AdminTransactionsReportDto(
    val totalTransactions: Int,
    val totalCoinVolume: Long,
    val transactionsByType: Map<String, Int>,
    val volumeByType: Map<String, Long>
) {
    val totalVolume: Long get() = totalCoinVolume
    val entryDeductionsCount: Int get() = transactionsByType["GAME_ENTRY_DEDUCTION"] ?: 0
    val rewardPayoutsCount: Int get() = transactionsByType["GAME_REWARD_CREDIT"] ?: 0
    val refundsCount: Int get() = transactionsByType["GAME_ENTRY_REFUND"] ?: 0
}

// Phase 8 Canonical Typealiases
typealias AdminOperationsSummaryDto = AdminGameOperationsSummaryDto
typealias AdminReconciliationReportDto = RewardReconciliationResultDto
typealias AdminReconciliationDiscrepancyDto = ReconciliationDiscrepancyDto
typealias AdminGameEntriesResponse = PaginatedGameEntriesResponse
typealias TransactionDetailDto = TransactionReferenceDto
typealias AuditLogListResponse = PaginatedAuditEventsResponse
typealias GamesReportDto = AdminGamesReportDto
typealias EntriesReportDto = AdminEntriesReportDto
typealias TransactionsReportDto = AdminTransactionsReportDto
