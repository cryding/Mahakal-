package com.example.ui.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.backend.model.AccountCoinSummaryDto
import com.example.backend.model.TransactionDto
import com.example.backend.model.WalletBalanceDto
import com.example.data.repository.WalletRepository
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WalletUiState(
    val myBalance: WalletBalanceDto? = null,
    val isLoadingBalance: Boolean = false,
    val transactions: List<TransactionDto> = emptyList(),
    val isLoadingTransactions: Boolean = false,
    val totalTransactionsCount: Int = 0,
    val currentTransactionsPage: Int = 1,
    val totalTransactionsPages: Int = 1,
    val filterType: String? = null,
    val filterStatus: String? = null,
    val searchQuery: String = "",
    val isSubmittingAction: Boolean = false,
    val actionSuccessMessage: String? = null,
    val actionErrorMessage: String? = null,
    val selectedTransaction: TransactionDto? = null,

    // Phase 6 Coin Management State
    val accountBalances: Map<String, Long> = emptyMap(),
    val isLoadingBalances: Boolean = false,
    val selectedAccountSummary: AccountCoinSummaryDto? = null,
    val isLoadingAccountSummary: Boolean = false,
    val activeCompletedTransaction: TransactionDto? = null,
    val activeFailureDetails: Pair<String, String>? = null, // (errorCode, friendlyMessage)
    val isVerifyingTimeout: Boolean = false,
    val pendingIdempotencyKey: String? = null
)

class WalletViewModel(
    private val walletRepository: WalletRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WalletUiState())
    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()

    init {
        loadBalance()
        loadTransactions()
    }

    fun loadBalance() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingBalance = true) }
            val response = walletRepository.getMyBalance()
            if (response.success && response.data != null) {
                _uiState.update {
                    it.copy(
                        myBalance = response.data,
                        isLoadingBalance = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoadingBalance = false,
                        actionErrorMessage = response.message ?: "Failed to retrieve wallet balance"
                    )
                }
            }
        }
    }

    fun loadAccountBalances(accountIds: List<String>) {
        if (accountIds.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingBalances = true) }
            val response = walletRepository.getAccountBalances(accountIds)
            if (response.success && response.data != null) {
                _uiState.update { current ->
                    val merged = current.accountBalances.toMutableMap().apply {
                        putAll(response.data.balances)
                    }
                    current.copy(
                        accountBalances = merged,
                        isLoadingBalances = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoadingBalances = false) }
            }
        }
    }

    fun loadAccountSummary(accountId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingAccountSummary = true, selectedAccountSummary = null) }
            val response = walletRepository.getAccountCoinSummary(accountId)
            if (response.success && response.data != null) {
                _uiState.update {
                    it.copy(
                        selectedAccountSummary = response.data,
                        isLoadingAccountSummary = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoadingAccountSummary = false,
                        actionErrorMessage = response.message ?: "Failed to load account coin summary"
                    )
                }
            }
        }
    }

    fun clearAccountSummary() {
        _uiState.update { it.copy(selectedAccountSummary = null) }
    }

    fun loadTransactions(
        page: Int = 1,
        type: String? = _uiState.value.filterType,
        status: String? = _uiState.value.filterStatus,
        search: String? = _uiState.value.searchQuery
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingTransactions = true,
                    filterType = type,
                    filterStatus = status,
                    searchQuery = search ?: ""
                )
            }
            val response = walletRepository.getTransactions(
                page = page,
                limit = 20,
                type = type,
                status = status,
                search = if (search.isNullOrBlank()) null else search
            )
            if (response.success && response.data != null) {
                val data = response.data
                _uiState.update {
                    it.copy(
                        transactions = data.items,
                        totalTransactionsCount = data.totalCount,
                        currentTransactionsPage = data.page,
                        totalTransactionsPages = data.totalPages,
                        isLoadingTransactions = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoadingTransactions = false,
                        actionErrorMessage = response.message ?: "Failed to fetch transactions"
                    )
                }
            }
        }
    }

    /**
     * Executes transfer with robust idempotency preservation and authoritative balance reload.
     */
    fun transferCoins(
        destinationAccountId: String,
        amount: Long,
        reason: String,
        existingIdempotencyKey: String? = null,
        onSuccess: ((TransactionDto) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val key = existingIdempotencyKey ?: UUID.randomUUID().toString()
            _uiState.update {
                it.copy(
                    isSubmittingAction = true,
                    actionErrorMessage = null,
                    pendingIdempotencyKey = key,
                    activeFailureDetails = null,
                    activeCompletedTransaction = null
                )
            }

            val response = walletRepository.transferCoins(
                destinationAccountId = destinationAccountId,
                amount = amount,
                reason = reason,
                idempotencyKey = key
            )

            if (response.success && response.data != null) {
                val tx = response.data
                _uiState.update {
                    it.copy(
                        isSubmittingAction = false,
                        activeCompletedTransaction = tx,
                        pendingIdempotencyKey = null,
                        actionSuccessMessage = "Transferred $amount coins successfully (Tx: ${tx.transactionId.take(8)}...)"
                    )
                }
                loadBalance()
                loadTransactions(page = 1)
                onSuccess?.invoke(tx)
            } else {
                val mappedError = mapErrorCodeToFriendlyMessage(response.errorCode, response.message)
                _uiState.update {
                    it.copy(
                        isSubmittingAction = false,
                        activeFailureDetails = Pair(response.errorCode ?: "UNKNOWN_ERROR", mappedError),
                        actionErrorMessage = mappedError
                    )
                }
            }
        }
    }

    /**
     * Executes deduction with robust idempotency preservation and authoritative balance reload.
     */
    fun deductCoins(
        targetAccountId: String,
        amount: Long,
        reason: String,
        existingIdempotencyKey: String? = null,
        onSuccess: ((TransactionDto) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val key = existingIdempotencyKey ?: UUID.randomUUID().toString()
            _uiState.update {
                it.copy(
                    isSubmittingAction = true,
                    actionErrorMessage = null,
                    pendingIdempotencyKey = key,
                    activeFailureDetails = null,
                    activeCompletedTransaction = null
                )
            }

            val response = walletRepository.deductCoins(
                targetAccountId = targetAccountId,
                amount = amount,
                reason = reason,
                idempotencyKey = key
            )

            if (response.success && response.data != null) {
                val tx = response.data
                _uiState.update {
                    it.copy(
                        isSubmittingAction = false,
                        activeCompletedTransaction = tx,
                        pendingIdempotencyKey = null,
                        actionSuccessMessage = "Deducted $amount coins successfully (Tx: ${tx.transactionId.take(8)}...)"
                    )
                }
                loadBalance()
                loadTransactions(page = 1)
                onSuccess?.invoke(tx)
            } else {
                val mappedError = mapErrorCodeToFriendlyMessage(response.errorCode, response.message)
                _uiState.update {
                    it.copy(
                        isSubmittingAction = false,
                        activeFailureDetails = Pair(response.errorCode ?: "UNKNOWN_ERROR", mappedError),
                        actionErrorMessage = mappedError
                    )
                }
            }
        }
    }

    /**
     * Simulates or handles network timeout verification:
     * Keeps same idempotency key, queries status, and prevents duplicate transfer submission.
     */
    fun verifyTimeoutTransaction(
        idempotencyKey: String,
        onResolved: (TransactionDto?) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isVerifyingTimeout = true) }
            delay(1000) // Verification check delay
            // Query transactions to see if this key already executed
            val found = _uiState.value.transactions.firstOrNull { it.idempotencyKey == idempotencyKey }
            _uiState.update {
                it.copy(
                    isVerifyingTimeout = false,
                    activeCompletedTransaction = found
                )
            }
            onResolved(found)
        }
    }

    fun clearActiveResultScreens() {
        _uiState.update {
            it.copy(
                activeCompletedTransaction = null,
                activeFailureDetails = null,
                pendingIdempotencyKey = null,
                isVerifyingTimeout = false
            )
        }
    }

    fun reverseTransaction(
        transactionId: String,
        reason: String,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingAction = true, actionErrorMessage = null) }
            val idempotencyKey = UUID.randomUUID().toString()
            val response = walletRepository.reverseTransaction(
                transactionId = transactionId,
                reason = reason,
                idempotencyKey = idempotencyKey
            )
            if (response.success && response.data != null) {
                _uiState.update {
                    it.copy(
                        isSubmittingAction = false,
                        actionSuccessMessage = "Transaction reversed successfully."
                    )
                }
                loadBalance()
                loadTransactions(page = 1)
                onSuccess?.invoke()
            } else {
                _uiState.update {
                    it.copy(
                        isSubmittingAction = false,
                        actionErrorMessage = response.message ?: "Reversal failed [${response.errorCode}]"
                    )
                }
            }
        }
    }

    fun selectTransaction(transaction: TransactionDto?) {
        _uiState.update { it.copy(selectedTransaction = transaction) }
    }

    fun clearFeedback() {
        _uiState.update {
            it.copy(actionSuccessMessage = null, actionErrorMessage = null)
        }
    }

    private fun mapErrorCodeToFriendlyMessage(errorCode: String?, rawMessage: String?): String {
        return when (errorCode) {
            "INSUFFICIENT_BALANCE" -> "Your available coin balance is not sufficient for this operation."
            "PERMISSION_DENIED", "RESOURCE_NOT_OWNED" -> "You do not have permission to perform this operation."
            "ACCOUNT_SUSPENDED", "INACTIVE_ACCOUNT" -> "This account is currently suspended or inactive."
            "ACCOUNT_NOT_FOUND", "TARGET_NOT_FOUND", "WALLET_NOT_FOUND" -> "Target account or wallet was not found."
            "CONCURRENT_UPDATE", "OPTIMISTIC_LOCK_FAILED" -> "Another update occurred simultaneously. Please retry."
            "DUPLICATE_REQUEST" -> "This request has already been processed."
            "RATE_LIMIT_EXCEEDED" -> "Rate limit exceeded. Please wait a moment before trying again."
            "NETWORK_ERROR" -> "Network communication failed. Please check connection."
            else -> rawMessage ?: "An unexpected error occurred. Please try again."
        }
    }

    class Factory(private val walletRepository: WalletRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(WalletViewModel::class.java)) {
                return WalletViewModel(walletRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
