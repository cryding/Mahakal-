package com.example.backend.model

/**
 * Representation of a non-monetary virtual coin wallet.
 */
data class WalletDto(
    val walletId: String,
    val ownerId: String,
    val ownerRole: String,
    val balance: Long,
    val currencyType: String = "VIRTUAL_COIN",
    val version: Long,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Public response for GET /wallets/me.
 * Strictly non-monetary balance representation.
 */
data class WalletBalanceDto(
    val walletId: String,
    val balance: Long,
    val currencyType: String = "VIRTUAL_COIN",
    val updatedAt: Long
)

/**
 * Ledger Transaction representation.
 */
data class TransactionDto(
    val transactionId: String,
    val idempotencyKey: String,
    val timestamp: Long,
    val actorId: String,
    val actorRole: String,
    val sourceWalletId: String?,
    val destinationWalletId: String?,
    val amount: Long,
    val balanceBeforeSource: Long?,
    val balanceAfterSource: Long?,
    val balanceBeforeDestination: Long?,
    val balanceAfterDestination: Long?,
    val transactionType: String,
    val reason: String,
    val referenceId: String?,
    val status: String,
    val metadataJson: String = "{}",
    val createdAt: Long
)

/**
 * Request payload for POST /wallets/transfer.
 * Note: sourceAccountId is NEVER accepted from client.
 * Server strictly derives source from authenticated security context.
 */
data class TransferCoinsRequest(
    val destinationAccountId: String,
    val amount: Long,
    val reason: String
)

/**
 * Request payload for POST /wallets/deduct.
 * Server strictly validates actor permissions and hierarchy ownership.
 */
data class DeductCoinsRequest(
    val targetAccountId: String,
    val amount: Long,
    val reason: String
)

/**
 * Request payload for POST /transactions/{id}/reverse (Admin only).
 */
data class ReverseTransactionRequest(
    val reason: String
)

/**
 * Paginated ledger list response.
 */
data class PaginatedTransactionListResponse(
    val items: List<TransactionDto>,
    val totalCount: Int,
    val page: Int,
    val limit: Int,
    val totalPages: Int
)

/**
 * Informational preview calculated by server or client helper.
 */
data class CoinTransactionPreviewDto(
    val sourceAccountId: String,
    val destinationAccountId: String,
    val currentAvailableBalance: Long,
    val requestedAmount: Long,
    val remainingBalance: Long,
    val reason: String
)

/**
 * Detailed coin summary for a specific subordinate account.
 */
data class AccountCoinSummaryDto(
    val accountId: String,
    val loginId: String,
    val fullName: String,
    val role: String,
    val status: String,
    val balance: Long,
    val currencyType: String = "VIRTUAL_COIN",
    val lastTransaction: TransactionDto?,
    val recentTransactions: List<TransactionDto>
)

/**
 * Batch balances map response (accountId -> coinBalance).
 */
data class BatchBalancesResponse(
    val balances: Map<String, Long>
)
