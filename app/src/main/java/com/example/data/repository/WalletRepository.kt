package com.example.data.repository

import com.example.backend.api.ServerApiRouter
import com.example.backend.model.AccountCoinSummaryDto
import com.example.backend.model.BatchBalancesResponse
import com.example.backend.model.DeductCoinsRequest
import com.example.backend.model.PaginatedTransactionListResponse
import com.example.backend.model.ReverseTransactionRequest
import com.example.backend.model.ServerResponse
import com.example.backend.model.TransactionDto
import com.example.backend.model.TransferCoinsRequest
import com.example.backend.model.WalletBalanceDto
import com.example.backend.model.WalletDto
import com.example.core.security.SecureTokenStorage

interface WalletRepository {
    suspend fun getMyBalance(): ServerResponse<WalletBalanceDto>
    suspend fun getWalletById(walletId: String): ServerResponse<WalletDto>
    suspend fun transferCoins(
        destinationAccountId: String,
        amount: Long,
        reason: String,
        idempotencyKey: String
    ): ServerResponse<TransactionDto>
    suspend fun deductCoins(
        targetAccountId: String,
        amount: Long,
        reason: String,
        idempotencyKey: String
    ): ServerResponse<TransactionDto>
    suspend fun getTransactions(
        page: Int = 1,
        limit: Int = 20,
        type: String? = null,
        status: String? = null,
        search: String? = null
    ): ServerResponse<PaginatedTransactionListResponse>
    suspend fun getTransactionById(transactionId: String): ServerResponse<TransactionDto>
    suspend fun reverseTransaction(
        transactionId: String,
        reason: String,
        idempotencyKey: String
    ): ServerResponse<TransactionDto>
    suspend fun getAccountCoinSummary(targetAccountId: String): ServerResponse<AccountCoinSummaryDto>
    suspend fun getAccountBalances(accountIds: List<String>): ServerResponse<BatchBalancesResponse>
}

class WalletRepositoryImpl(
    private val apiRouter: ServerApiRouter,
    private val secureStorage: SecureTokenStorage
) : WalletRepository {

    private fun getAuthHeader(): String? {
        val token = secureStorage.getAccessToken() ?: return null
        return "Bearer $token"
    }

    private fun <T> unauthorized(): ServerResponse<T> {
        return ServerResponse(false, 401, null, "UNAUTHORIZED", "User is not authenticated.")
    }

    override suspend fun getMyBalance(): ServerResponse<WalletBalanceDto> {
        val authHeader = getAuthHeader() ?: return unauthorized()
        return apiRouter.handleGetMyWalletBalance(authHeader)
    }

    override suspend fun getWalletById(walletId: String): ServerResponse<WalletDto> {
        val authHeader = getAuthHeader() ?: return unauthorized()
        return apiRouter.handleGetWalletById(authHeader, walletId)
    }

    override suspend fun transferCoins(
        destinationAccountId: String,
        amount: Long,
        reason: String,
        idempotencyKey: String
    ): ServerResponse<TransactionDto> {
        val authHeader = getAuthHeader() ?: return unauthorized()
        val request = TransferCoinsRequest(
            destinationAccountId = destinationAccountId,
            amount = amount,
            reason = reason
        )
        return apiRouter.handleTransferCoins(authHeader, request, idempotencyKey)
    }

    override suspend fun deductCoins(
        targetAccountId: String,
        amount: Long,
        reason: String,
        idempotencyKey: String
    ): ServerResponse<TransactionDto> {
        val authHeader = getAuthHeader() ?: return unauthorized()
        val request = DeductCoinsRequest(
            targetAccountId = targetAccountId,
            amount = amount,
            reason = reason
        )
        return apiRouter.handleDeductCoins(authHeader, request, idempotencyKey)
    }

    override suspend fun getTransactions(
        page: Int,
        limit: Int,
        type: String?,
        status: String?,
        search: String?
    ): ServerResponse<PaginatedTransactionListResponse> {
        val authHeader = getAuthHeader() ?: return unauthorized()
        return apiRouter.handleGetTransactions(authHeader, page, limit, type, status, search)
    }

    override suspend fun getTransactionById(transactionId: String): ServerResponse<TransactionDto> {
        val authHeader = getAuthHeader() ?: return unauthorized()
        return apiRouter.handleGetTransactionById(authHeader, transactionId)
    }

    override suspend fun reverseTransaction(
        transactionId: String,
        reason: String,
        idempotencyKey: String
    ): ServerResponse<TransactionDto> {
        val authHeader = getAuthHeader() ?: return unauthorized()
        val request = ReverseTransactionRequest(reason = reason)
        return apiRouter.handleReverseTransaction(authHeader, transactionId, request, idempotencyKey)
    }

    override suspend fun getAccountCoinSummary(targetAccountId: String): ServerResponse<AccountCoinSummaryDto> {
        val authHeader = getAuthHeader() ?: return unauthorized()
        return apiRouter.handleGetAccountCoinSummary(authHeader, targetAccountId)
    }

    override suspend fun getAccountBalances(accountIds: List<String>): ServerResponse<BatchBalancesResponse> {
        val authHeader = getAuthHeader() ?: return unauthorized()
        return apiRouter.handleGetAccountBalances(authHeader, accountIds)
    }
}
