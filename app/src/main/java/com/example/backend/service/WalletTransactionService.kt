package com.example.backend.service

import androidx.room.withTransaction
import com.example.backend.audit.AuditActions
import com.example.backend.audit.AuditService
import com.example.backend.database.MahakalServerDatabase
import com.example.backend.database.entity.AccountEntity
import com.example.backend.database.entity.WalletEntity
import com.example.backend.database.entity.WalletTransactionEntity
import com.example.backend.model.AccountCoinSummaryDto
import com.example.backend.model.BatchBalancesResponse
import com.example.backend.model.DeductCoinsRequest
import com.example.backend.model.PaginatedTransactionListResponse
import com.example.backend.model.ServerResponse
import com.example.backend.model.TransactionDto
import com.example.backend.model.TransferCoinsRequest
import com.example.backend.model.WalletBalanceDto
import com.example.backend.model.WalletDto
import com.example.backend.rbac.AccountRole
import com.example.backend.rbac.AccountStatus
import com.example.backend.rbac.Permission
import com.example.backend.rbac.SecurityContext
import com.example.backend.security.RateLimiter
import java.util.UUID

/**
 * Production Virtual Coin Ledger & Atomic Transaction Engine.
 * Authoritative source of truth for all non-monetary virtual coin operations.
 */
class WalletTransactionService(
    private val database: MahakalServerDatabase,
    private val auditService: AuditService,
    private val rateLimiter: RateLimiter,
    private val notificationService: NotificationService? = null
) {

    private val accountDao = database.accountDao()
    private val walletDao = database.walletDao()
    private val transactionDao = database.walletTransactionDao()

    companion object {
        const val MAX_COIN_BALANCE = 1_000_000_000_000L // 1 Trillion units max to prevent overflow
        const val CURRENCY_VIRTUAL_COIN = "VIRTUAL_COIN"

        // Transaction types
        const val TYPE_ADMIN_TO_AGENT = "ADMIN_TO_AGENT"
        const val TYPE_AGENT_TO_USER = "AGENT_TO_USER"
        const val TYPE_ADMIN_DEDUCTION = "ADMIN_DEDUCTION"
        const val TYPE_AGENT_DEDUCTION = "AGENT_DEDUCTION"
        const val TYPE_GAME_REWARD = "GAME_REWARD"
        const val TYPE_GAME_DEDUCTION = "GAME_DEDUCTION"
        const val TYPE_SYSTEM_ADJUSTMENT = "SYSTEM_ADJUSTMENT"

        // Transaction statuses
        const val STATUS_PENDING = "PENDING"
        const val STATUS_COMPLETED = "COMPLETED"
        const val STATUS_FAILED = "FAILED"
        const val STATUS_REVERSED = "REVERSED"
    }

    /**
     * Retrieves or atomically creates a wallet for an eligible account.
     */
    suspend fun getOrCreateWallet(
        accountId: String,
        role: String,
        initialBalance: Long = 0L
    ): WalletEntity {
        val existing = walletDao.findByOwnerId(accountId)
        if (existing != null) return existing

        val now = System.currentTimeMillis()
        val newWallet = WalletEntity(
            walletId = UUID.randomUUID().toString(),
            ownerId = accountId,
            ownerRole = role,
            balance = initialBalance.coerceAtLeast(0L),
            currencyType = CURRENCY_VIRTUAL_COIN,
            version = 0L,
            createdAt = now,
            updatedAt = now
        )
        try {
            walletDao.insert(newWallet)
            return newWallet
        } catch (e: Exception) {
            // In case of concurrent insertion, fetch existing
            return walletDao.findByOwnerId(accountId)
                ?: throw IllegalStateException("Failed to initialize wallet for account $accountId")
        }
    }

    /**
     * GET /wallets/me: Authenticated actor's wallet balance.
     */
    suspend fun getMyWalletBalance(
        context: SecurityContext,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<WalletBalanceDto> {
        val wallet = getOrCreateWallet(context.accountId, context.role.name)
        return ServerResponse(
            success = true,
            statusCode = 200,
            data = WalletBalanceDto(
                walletId = wallet.walletId,
                balance = wallet.balance,
                currencyType = wallet.currencyType,
                updatedAt = wallet.updatedAt
            ),
            requestId = requestId
        )
    }

    /**
     * GET /wallets/{walletId}: Wallet details with strict ownership verification.
     */
    suspend fun getWalletById(
        context: SecurityContext,
        walletId: String,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<WalletDto> {
        val wallet = walletDao.findByWalletId(walletId)
            ?: return ServerResponse(false, 404, null, "WALLET_NOT_FOUND", "Wallet '$walletId' not found.", requestId)

        // Ownership enforcement
        val isAuthorized = when (context.role) {
            AccountRole.ADMIN -> true
            AccountRole.AGENT -> {
                if (wallet.ownerId == context.accountId) true
                else {
                    val ownerAccount = accountDao.findById(wallet.ownerId)
                    ownerAccount?.parentId == context.accountId
                }
            }
            AccountRole.USER -> wallet.ownerId == context.accountId
        }

        if (!isAuthorized) {
            auditService.logEvent(
                actorId = context.accountId,
                actorRole = context.role.name,
                action = AuditActions.UNAUTHORIZED_COIN_OPERATION,
                targetId = walletId,
                requestId = requestId,
                metadataJson = "{\"attempted\":\"GET_WALLET\",\"ownerId\":\"${wallet.ownerId}\"}"
            )
            return ServerResponse(false, 403, null, "RESOURCE_NOT_OWNED", "Access denied to requested wallet.", requestId)
        }

        return ServerResponse(
            success = true,
            statusCode = 200,
            data = wallet.toDto(),
            requestId = requestId
        )
    }

    /**
     * POST /wallets/transfer: Atomic coin transfer with strict RBAC, validation, idempotency, and immutable ledger recording.
     */
    suspend fun transferCoins(
        context: SecurityContext,
        request: TransferCoinsRequest,
        idempotencyKey: String?,
        clientIp: String? = null,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<TransactionDto> {
        val trimmedKey = idempotencyKey?.trim()
        if (trimmedKey.isNullOrEmpty()) {
            return ServerResponse(false, 400, null, "MISSING_IDEMPOTENCY_KEY", "Idempotency-Key header is required.", requestId)
        }

        // Check if transaction with this idempotency key already completed (Idempotency Protection)
        val existingTx = transactionDao.findByIdempotencyKey(trimmedKey)
        if (existingTx != null) {
            return ServerResponse(
                success = true,
                statusCode = 200,
                data = existingTx.toDto(),
                requestId = requestId
            )
        }

        // Rate limiting
        val rateLimitKey = "tx:${context.accountId}"
        if (rateLimiter.isLocked(rateLimitKey)) {
            return ServerResponse(false, 429, null, "RATE_LIMITED", "Too many transaction attempts. Please wait.", requestId)
        }

        // 1. Account Status
        if (context.status != AccountStatus.ACTIVE) {
            return ServerResponse(false, 403, null, "ACCOUNT_SUSPENDED", "Suspended accounts cannot initiate transactions.", requestId)
        }

        // 2. Validate Amount
        val amount = request.amount
        if (amount <= 0L) {
            return ServerResponse(false, 400, null, "INVALID_AMOUNT", "Transfer amount must be strictly greater than 0.", requestId)
        }
        if (amount > MAX_COIN_BALANCE) {
            return ServerResponse(false, 400, null, "INVALID_AMOUNT", "Transfer amount exceeds maximum supported threshold.", requestId)
        }

        val reason = request.reason.trim()
        if (reason.isEmpty()) {
            return ServerResponse(false, 422, null, "INVALID_TRANSACTION_REQUEST", "Transfer reason must be specified.", requestId)
        }

        // 3. Authorize Role & Scope
        val destinationAccountId = request.destinationAccountId.trim()
        val destinationAccount = accountDao.findById(destinationAccountId)
            ?: return ServerResponse(false, 404, null, "ACCOUNT_NOT_FOUND", "Recipient account not found.", requestId)

        if (destinationAccount.status != AccountStatus.ACTIVE.name) {
            return ServerResponse(false, 403, null, "RECIPIENT_INACTIVE", "Recipient account is not active.", requestId)
        }

        val transactionType: String = when (context.role) {
            AccountRole.ADMIN -> {
                if (!context.permissions.contains(Permission.ADMIN_MANAGE_AGENT_COINS)) {
                    return ServerResponse(false, 403, null, "INSUFFICIENT_PERMISSION", "Missing ADMIN_MANAGE_AGENT_COINS permission.", requestId)
                }
                if (destinationAccount.role != AccountRole.AGENT.name) {
                    return ServerResponse(false, 403, null, "INVALID_RECIPIENT", "Admin can only transfer coins to Agents.", requestId)
                }
                TYPE_ADMIN_TO_AGENT
            }
            AccountRole.AGENT -> {
                if (!context.permissions.contains(Permission.AGENT_MANAGE_OWN_USER_COINS)) {
                    return ServerResponse(false, 403, null, "INSUFFICIENT_PERMISSION", "Missing AGENT_MANAGE_OWN_USER_COINS permission.", requestId)
                }
                if (destinationAccount.role != AccountRole.USER.name) {
                    return ServerResponse(false, 403, null, "INVALID_RECIPIENT", "Agents can only transfer coins to Users.", requestId)
                }
                // Hierarchy isolation: destination user must belong to this Agent
                if (destinationAccount.parentId != context.accountId) {
                    auditService.logEvent(
                        actorId = context.accountId,
                        actorRole = context.role.name,
                        action = AuditActions.UNAUTHORIZED_COIN_OPERATION,
                        targetId = destinationAccountId,
                        requestId = requestId,
                        metadataJson = "{\"reason\":\"Cross-agent transfer attempt rejected\"}"
                    )
                    return ServerResponse(false, 403, null, "RESOURCE_NOT_OWNED", "Agent cannot transfer coins to another Agent's User.", requestId)
                }
                TYPE_AGENT_TO_USER
            }
            AccountRole.USER -> {
                // Users cannot transfer coins
                auditService.logEvent(
                    actorId = context.accountId,
                    actorRole = context.role.name,
                    action = AuditActions.UNAUTHORIZED_COIN_OPERATION,
                    targetId = destinationAccountId,
                    requestId = requestId,
                    metadataJson = "{\"reason\":\"User transfer denied\"}"
                )
                return ServerResponse(false, 403, null, "INSUFFICIENT_PERMISSION", "Users are not authorized to transfer coins.", requestId)
            }
        }

        // 4. Atomic Execution
        try {
            val result = database.withTransaction {
                // Check idempotency again inside transaction
                val txCheck = transactionDao.findByIdempotencyKey(trimmedKey)
                if (txCheck != null) {
                    return@withTransaction txCheck
                }

                // Load source wallet (derived strictly from authenticated actor)
                val sourceWallet = getOrCreateWallet(context.accountId, context.role.name)
                // Load destination wallet
                val destWallet = getOrCreateWallet(destinationAccount.id, destinationAccount.role)

                // Verify sufficient balance
                if (sourceWallet.balance < amount) {
                    auditService.logEvent(
                        actorId = context.accountId,
                        actorRole = context.role.name,
                        action = AuditActions.INSUFFICIENT_BALANCE,
                        targetId = destinationAccountId,
                        requestId = requestId,
                        metadataJson = "{\"available\":${sourceWallet.balance},\"requested\":$amount}"
                    )
                    throw InsufficientBalanceException(
                        "Insufficient balance. Available: ${sourceWallet.balance} coins, requested: $amount coins."
                    )
                }

                val balanceBeforeSource = sourceWallet.balance
                val balanceAfterSource = balanceBeforeSource - amount
                val balanceBeforeDest = destWallet.balance
                val balanceAfterDest = balanceBeforeDest + amount

                // Validate Invariants
                require(balanceAfterSource == balanceBeforeSource - amount) { "Source balance invariant violation" }
                require(balanceAfterDest == balanceBeforeDest + amount) { "Destination balance invariant violation" }
                require(balanceAfterSource >= 0L) { "Balance cannot be negative" }
                require(balanceAfterDest <= MAX_COIN_BALANCE) { "Recipient balance exceeds maximum limit" }

                val now = System.currentTimeMillis()

                // Optimistic concurrency update for source
                val rowsDeducted = walletDao.deductBalanceWithVersion(
                    walletId = sourceWallet.walletId,
                    amount = amount,
                    expectedVersion = sourceWallet.version,
                    newVersion = sourceWallet.version + 1,
                    updatedAt = now
                )
                if (rowsDeducted == 0) {
                    throw ConcurrencyConflictException("Concurrent balance modification detected on source wallet.")
                }

                // Update destination
                val rowsAdded = walletDao.addBalanceWithVersion(
                    walletId = destWallet.walletId,
                    amount = amount,
                    expectedVersion = destWallet.version,
                    newVersion = destWallet.version + 1,
                    updatedAt = now
                )
                if (rowsAdded == 0) {
                    throw ConcurrencyConflictException("Concurrent balance modification detected on destination wallet.")
                }

                // Append-only ledger recording
                val txEntity = WalletTransactionEntity(
                    transactionId = UUID.randomUUID().toString(),
                    idempotencyKey = trimmedKey,
                    timestamp = now,
                    actorId = context.accountId,
                    actorRole = context.role.name,
                    sourceWalletId = sourceWallet.walletId,
                    destinationWalletId = destWallet.walletId,
                    amount = amount,
                    balanceBeforeSource = balanceBeforeSource,
                    balanceAfterSource = balanceAfterSource,
                    balanceBeforeDestination = balanceBeforeDest,
                    balanceAfterDestination = balanceAfterDest,
                    transactionType = transactionType,
                    reason = reason,
                    referenceId = null,
                    status = STATUS_COMPLETED,
                    metadataJson = "{\"recipientLoginId\":\"${destinationAccount.loginId}\",\"recipientName\":\"${destinationAccount.fullName}\"}",
                    createdAt = now
                )
                transactionDao.insert(txEntity)

                // Audit event
                auditService.logEvent(
                    actorId = context.accountId,
                    actorRole = context.role.name,
                    action = AuditActions.COIN_TRANSFER_COMPLETED,
                    targetId = destinationAccountId,
                    targetType = "ACCOUNT",
                    requestId = requestId,
                    metadataJson = "{\"transactionId\":\"${txEntity.transactionId}\",\"amount\":$amount,\"type\":\"$transactionType\"}",
                    beforeState = "{\"sourceBalance\":$balanceBeforeSource,\"destBalance\":$balanceBeforeDest}",
                    afterState = "{\"sourceBalance\":$balanceAfterSource,\"destBalance\":$balanceAfterDest}"
                )

                txEntity
            }

            notificationService?.onWalletTransaction(result, context.accountId, destinationAccountId)

            return ServerResponse(
                success = true,
                statusCode = 200,
                data = result.toDto(),
                requestId = requestId
            )
        } catch (e: InsufficientBalanceException) {
            return ServerResponse(false, 403, null, "INSUFFICIENT_BALANCE", e.message, requestId)
        } catch (e: ConcurrencyConflictException) {
            return ServerResponse(false, 409, null, "CONCURRENT_BALANCE_UPDATE", e.message, requestId)
        } catch (e: Exception) {
            auditService.logEvent(
                actorId = context.accountId,
                actorRole = context.role.name,
                action = AuditActions.COIN_TRANSFER_FAILED,
                targetId = destinationAccountId,
                requestId = requestId,
                metadataJson = "{\"error\":\"${e.message}\"}"
            )
            return ServerResponse(false, 500, null, "TRANSACTION_FAILED", "Transaction failed: ${e.localizedMessage}", requestId)
        }
    }

    /**
     * POST /wallets/deduct: Controlled deduction with atomic ledger record and hierarchy ownership validation.
     */
    suspend fun deductCoins(
        context: SecurityContext,
        request: DeductCoinsRequest,
        idempotencyKey: String?,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<TransactionDto> {
        val trimmedKey = idempotencyKey?.trim()
        if (trimmedKey.isNullOrEmpty()) {
            return ServerResponse(false, 400, null, "MISSING_IDEMPOTENCY_KEY", "Idempotency-Key header is required.", requestId)
        }

        val existingTx = transactionDao.findByIdempotencyKey(trimmedKey)
        if (existingTx != null) {
            return ServerResponse(true, 200, existingTx.toDto(), null, null, requestId)
        }

        if (context.status != AccountStatus.ACTIVE) {
            return ServerResponse(false, 403, null, "ACCOUNT_SUSPENDED", "Account is suspended.", requestId)
        }

        val amount = request.amount
        if (amount <= 0L) {
            return ServerResponse(false, 400, null, "INVALID_AMOUNT", "Deduction amount must be strictly greater than 0.", requestId)
        }

        val targetAccountId = request.targetAccountId.trim()
        val targetAccount = accountDao.findById(targetAccountId)
            ?: return ServerResponse(false, 404, null, "ACCOUNT_NOT_FOUND", "Target account not found.", requestId)

        val transactionType: String = when (context.role) {
            AccountRole.ADMIN -> {
                if (!context.permissions.contains(Permission.ADMIN_MANAGE_AGENT_COINS)) {
                    return ServerResponse(false, 403, null, "INSUFFICIENT_PERMISSION", "Missing ADMIN_MANAGE_AGENT_COINS permission.", requestId)
                }
                if (targetAccount.role != AccountRole.AGENT.name) {
                    return ServerResponse(false, 403, null, "INVALID_TARGET", "Admin can only deduct coins from Agents.", requestId)
                }
                TYPE_ADMIN_DEDUCTION
            }
            AccountRole.AGENT -> {
                if (!context.permissions.contains(Permission.AGENT_MANAGE_OWN_USER_COINS)) {
                    return ServerResponse(false, 403, null, "INSUFFICIENT_PERMISSION", "Missing AGENT_MANAGE_OWN_USER_COINS permission.", requestId)
                }
                if (targetAccount.role != AccountRole.USER.name) {
                    return ServerResponse(false, 403, null, "INVALID_TARGET", "Agents can only deduct coins from Users.", requestId)
                }
                if (targetAccount.parentId != context.accountId) {
                    auditService.logEvent(
                        actorId = context.accountId,
                        actorRole = context.role.name,
                        action = AuditActions.UNAUTHORIZED_COIN_OPERATION,
                        targetId = targetAccountId,
                        requestId = requestId,
                        metadataJson = "{\"reason\":\"Cross-agent deduction attempt rejected\"}"
                    )
                    return ServerResponse(false, 403, null, "RESOURCE_NOT_OWNED", "Agent cannot deduct coins from another Agent's User.", requestId)
                }
                TYPE_AGENT_DEDUCTION
            }
            AccountRole.USER -> {
                return ServerResponse(false, 403, null, "INSUFFICIENT_PERMISSION", "Users cannot execute deductions.", requestId)
            }
        }

        val reason = request.reason.trim().ifEmpty { "Administrative deduction" }

        try {
            val result = database.withTransaction {
                val targetWallet = getOrCreateWallet(targetAccount.id, targetAccount.role)
                if (targetWallet.balance < amount) {
                    auditService.logEvent(
                        actorId = context.accountId,
                        actorRole = context.role.name,
                        action = AuditActions.INSUFFICIENT_BALANCE,
                        targetId = targetAccountId,
                        requestId = requestId,
                        metadataJson = "{\"targetBalance\":${targetWallet.balance},\"requestedDeduction\":$amount}"
                    )
                    throw InsufficientBalanceException("Target account has insufficient balance (${targetWallet.balance} coins).")
                }

                val balanceBefore = targetWallet.balance
                val balanceAfter = balanceBefore - amount
                require(balanceAfter >= 0L) { "Balance cannot be negative" }

                val now = System.currentTimeMillis()
                val updated = walletDao.deductBalanceWithVersion(
                    walletId = targetWallet.walletId,
                    amount = amount,
                    expectedVersion = targetWallet.version,
                    newVersion = targetWallet.version + 1,
                    updatedAt = now
                )
                if (updated == 0) {
                    throw ConcurrencyConflictException("Concurrent balance update on target wallet.")
                }

                val txEntity = WalletTransactionEntity(
                    transactionId = UUID.randomUUID().toString(),
                    idempotencyKey = trimmedKey,
                    timestamp = now,
                    actorId = context.accountId,
                    actorRole = context.role.name,
                    sourceWalletId = targetWallet.walletId,
                    destinationWalletId = null,
                    amount = amount,
                    balanceBeforeSource = balanceBefore,
                    balanceAfterSource = balanceAfter,
                    balanceBeforeDestination = null,
                    balanceAfterDestination = null,
                    transactionType = transactionType,
                    reason = reason,
                    referenceId = null,
                    status = STATUS_COMPLETED,
                    metadataJson = "{\"targetLoginId\":\"${targetAccount.loginId}\"}",
                    createdAt = now
                )
                transactionDao.insert(txEntity)

                auditService.logEvent(
                    actorId = context.accountId,
                    actorRole = context.role.name,
                    action = AuditActions.COIN_DEDUCTION_COMPLETED,
                    targetId = targetAccountId,
                    targetType = "ACCOUNT",
                    requestId = requestId,
                    metadataJson = "{\"transactionId\":\"${txEntity.transactionId}\",\"amount\":$amount,\"type\":\"$transactionType\"}",
                    beforeState = "{\"balance\":$balanceBefore}",
                    afterState = "{\"balance\":$balanceAfter}"
                )

                txEntity
            }

            notificationService?.onWalletTransaction(result, targetAccountId, null)

            return ServerResponse(true, 200, result.toDto(), null, null, requestId)
        } catch (e: InsufficientBalanceException) {
            return ServerResponse(false, 403, null, "INSUFFICIENT_BALANCE", e.message, requestId)
        } catch (e: ConcurrencyConflictException) {
            return ServerResponse(false, 409, null, "CONCURRENT_BALANCE_UPDATE", e.message, requestId)
        } catch (e: Exception) {
            return ServerResponse(false, 500, null, "DEDUCTION_FAILED", "Deduction failed: ${e.localizedMessage}", requestId)
        }
    }

    /**
     * POST /transactions/{id}/reverse: Admin-only reversal creating a compensating ledger transaction.
     * Historical transactions remain completely immutable.
     */
    suspend fun reverseTransaction(
        context: SecurityContext,
        transactionId: String,
        reason: String,
        idempotencyKey: String?,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<TransactionDto> {
        val trimmedKey = idempotencyKey?.trim()
        if (trimmedKey.isNullOrEmpty()) {
            return ServerResponse(false, 400, null, "MISSING_IDEMPOTENCY_KEY", "Idempotency-Key header is required.", requestId)
        }

        // Reversal requires ADMIN role
        if (context.role != AccountRole.ADMIN) {
            return ServerResponse(false, 403, null, "FORBIDDEN", "Only Administrators can reverse transactions.", requestId)
        }

        val existingReversal = transactionDao.findByIdempotencyKey(trimmedKey)
        if (existingReversal != null) {
            return ServerResponse(true, 200, existingReversal.toDto(), null, null, requestId)
        }

        val originalTx = transactionDao.findById(transactionId)
            ?: return ServerResponse(false, 404, null, "TRANSACTION_NOT_FOUND", "Original transaction '$transactionId' not found.", requestId)

        if (originalTx.status != STATUS_COMPLETED) {
            return ServerResponse(false, 422, null, "INVALID_STATE_TRANSITION", "Only COMPLETED transactions can be reversed.", requestId)
        }

        // Check if already reversed
        val alreadyReversed = transactionDao.findByReferenceId(originalTx.transactionId)
        if (alreadyReversed != null) {
            return ServerResponse(false, 409, null, "TRANSACTION_ALREADY_REVERSED", "Transaction has already been reversed.", requestId)
        }

        try {
            val compensatingTx = database.withTransaction {
                val now = System.currentTimeMillis()

                var sourceAfter: Long? = null
                var destAfter: Long? = null

                // Reverse destination wallet (deduct amount credited)
                if (originalTx.destinationWalletId != null) {
                    val destWallet = walletDao.findByWalletId(originalTx.destinationWalletId)
                        ?: throw IllegalStateException("Destination wallet not found")
                    if (destWallet.balance < originalTx.amount) {
                        throw InsufficientBalanceException("Recipient balance has already been spent. Cannot reverse transaction.")
                    }
                    val updated = walletDao.deductBalanceWithVersion(
                        walletId = destWallet.walletId,
                        amount = originalTx.amount,
                        expectedVersion = destWallet.version,
                        newVersion = destWallet.version + 1,
                        updatedAt = now
                    )
                    if (updated == 0) throw ConcurrencyConflictException("Concurrency conflict reversing destination wallet.")
                    destAfter = destWallet.balance - originalTx.amount
                }

                // Restore source wallet (credit amount originally deducted)
                if (originalTx.sourceWalletId != null) {
                    val sourceWallet = walletDao.findByWalletId(originalTx.sourceWalletId)
                        ?: throw IllegalStateException("Source wallet not found")
                    val updated = walletDao.addBalanceWithVersion(
                        walletId = sourceWallet.walletId,
                        amount = originalTx.amount,
                        expectedVersion = sourceWallet.version,
                        newVersion = sourceWallet.version + 1,
                        updatedAt = now
                    )
                    if (updated == 0) throw ConcurrencyConflictException("Concurrency conflict reversing source wallet.")
                    sourceAfter = sourceWallet.balance + originalTx.amount
                }

                // Update original transaction status to REVERSED (controlled transition COMPLETED -> REVERSED)
                val updatedOriginal = originalTx.copy(status = STATUS_REVERSED)
                transactionDao.update(updatedOriginal)

                // Create new compensating transaction linking via referenceId
                val newCompensatingTx = WalletTransactionEntity(
                    transactionId = UUID.randomUUID().toString(),
                    idempotencyKey = trimmedKey,
                    timestamp = now,
                    actorId = context.accountId,
                    actorRole = context.role.name,
                    sourceWalletId = originalTx.destinationWalletId,
                    destinationWalletId = originalTx.sourceWalletId,
                    amount = originalTx.amount,
                    balanceBeforeSource = originalTx.balanceAfterDestination,
                    balanceAfterSource = destAfter,
                    balanceBeforeDestination = originalTx.balanceAfterSource,
                    balanceAfterDestination = sourceAfter,
                    transactionType = TYPE_SYSTEM_ADJUSTMENT,
                    reason = "Reversal: $reason (ref: ${originalTx.transactionId})",
                    referenceId = originalTx.transactionId,
                    status = STATUS_COMPLETED,
                    metadataJson = "{\"reversedTransactionId\":\"${originalTx.transactionId}\"}",
                    createdAt = now
                )
                transactionDao.insert(newCompensatingTx)

                auditService.logEvent(
                    actorId = context.accountId,
                    actorRole = context.role.name,
                    action = AuditActions.COIN_TRANSACTION_REVERSED,
                    targetId = originalTx.transactionId,
                    targetType = "TRANSACTION",
                    requestId = requestId,
                    metadataJson = "{\"compensatingTxId\":\"${newCompensatingTx.transactionId}\",\"originalTxId\":\"${originalTx.transactionId}\"}"
                )

                newCompensatingTx
            }

            return ServerResponse(true, 200, compensatingTx.toDto(), null, null, requestId)
        } catch (e: InsufficientBalanceException) {
            return ServerResponse(false, 403, null, "INSUFFICIENT_BALANCE", e.message, requestId)
        } catch (e: ConcurrencyConflictException) {
            return ServerResponse(false, 409, null, "CONCURRENT_BALANCE_UPDATE", e.message, requestId)
        } catch (e: Exception) {
            return ServerResponse(false, 500, null, "REVERSAL_FAILED", "Reversal failed: ${e.localizedMessage}", requestId)
        }
    }

    /**
     * GET /transactions: Paginated, filtered transaction history with strict scope and ownership isolation.
     */
    suspend fun getTransactions(
        context: SecurityContext,
        page: Int = 1,
        limit: Int = 20,
        type: String? = null,
        status: String? = null,
        search: String? = null,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<PaginatedTransactionListResponse> {
        val safePage = if (page < 1) 1 else page
        val safeLimit = limit.coerceIn(1, 100)
        val offset = (safePage - 1) * safeLimit

        val (items, totalCount) = when (context.role) {
            AccountRole.ADMIN -> {
                // Admin can view global ledger
                val rawItems = transactionDao.getAllTransactions(safeLimit, offset)
                val count = transactionDao.countAllTransactions()
                Pair(rawItems, count)
            }
            AccountRole.AGENT -> {
                // Agent can view transactions involving own wallet OR their users' wallets
                val agentWallet = getOrCreateWallet(context.accountId, context.role.name)
                val userAccounts = accountDao.findByParentId(context.accountId)
                val userWallets = walletDao.findByOwnerIds(userAccounts.map { it.id })
                val allScopedWalletIds = listOf(agentWallet.walletId) + userWallets.map { it.walletId }

                val rawItems = transactionDao.getTransactionsForWallets(allScopedWalletIds, safeLimit, offset)
                val count = transactionDao.countTransactionsForWallets(allScopedWalletIds)
                Pair(rawItems, count)
            }
            AccountRole.USER -> {
                // User can view ONLY transactions involving their own wallet
                val userWallet = getOrCreateWallet(context.accountId, context.role.name)
                val rawItems = transactionDao.getTransactionsForWallet(userWallet.walletId, safeLimit, offset)
                val count = transactionDao.countTransactionsForWallet(userWallet.walletId)
                Pair(rawItems, count)
            }
        }

        // Apply in-memory type/status filtering if requested
        val filtered = items.filter { tx ->
            (type.isNullOrBlank() || tx.transactionType.equals(type, ignoreCase = true)) &&
            (status.isNullOrBlank() || tx.status.equals(status, ignoreCase = true)) &&
            (search.isNullOrBlank() || tx.transactionId.contains(search, ignoreCase = true) || tx.reason.contains(search, ignoreCase = true))
        }

        val totalPages = if (totalCount == 0) 1 else ((totalCount + safeLimit - 1) / safeLimit)

        return ServerResponse(
            success = true,
            statusCode = 200,
            data = PaginatedTransactionListResponse(
                items = filtered.map { it.toDto() },
                totalCount = totalCount,
                page = safePage,
                limit = safeLimit,
                totalPages = totalPages
            ),
            requestId = requestId
        )
    }

    /**
     * GET /transactions/{transactionId}: Transaction details with strict ownership verification.
     */
    suspend fun getTransactionById(
        context: SecurityContext,
        transactionId: String,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<TransactionDto> {
        val tx = transactionDao.findById(transactionId)
            ?: return ServerResponse(false, 404, null, "TRANSACTION_NOT_FOUND", "Transaction not found.", requestId)

        val isAuthorized = when (context.role) {
            AccountRole.ADMIN -> true
            AccountRole.AGENT -> {
                val agentWallet = getOrCreateWallet(context.accountId, context.role.name)
                if (tx.sourceWalletId == agentWallet.walletId || tx.destinationWalletId == agentWallet.walletId) {
                    true
                } else {
                    val userAccounts = accountDao.findByParentId(context.accountId)
                    val userWallets = walletDao.findByOwnerIds(userAccounts.map { it.id })
                    val userWalletIds = userWallets.map { it.walletId }.toSet()
                    userWalletIds.contains(tx.sourceWalletId) || userWalletIds.contains(tx.destinationWalletId)
                }
            }
            AccountRole.USER -> {
                val userWallet = getOrCreateWallet(context.accountId, context.role.name)
                tx.sourceWalletId == userWallet.walletId || tx.destinationWalletId == userWallet.walletId
            }
        }

        if (!isAuthorized) {
            auditService.logEvent(
                actorId = context.accountId,
                actorRole = context.role.name,
                action = AuditActions.UNAUTHORIZED_COIN_OPERATION,
                targetId = transactionId,
                requestId = requestId,
                metadataJson = "{\"reason\":\"Unauthorized transaction detail query\"}"
            )
            return ServerResponse(false, 403, null, "RESOURCE_NOT_OWNED", "Access denied to requested transaction.", requestId)
        }

        return ServerResponse(true, 200, tx.toDto(), null, null, requestId)
    }

    /**
     * Server-side Internal Gameplay Reward.
     * Restricted strictly to authorized server-side game engines. Never called directly by Android clients.
     */
    suspend fun executeServerGameplayReward(
        destinationAccountId: String,
        amount: Long,
        gameReferenceId: String,
        reason: String,
        idempotencyKey: String
    ): WalletTransactionEntity {
        require(amount > 0L) { "Reward amount must be > 0" }
        return database.withTransaction {
            val account = accountDao.findById(destinationAccountId)
                ?: throw IllegalArgumentException("Player account not found")
            val destWallet = getOrCreateWallet(account.id, account.role)
            val now = System.currentTimeMillis()

            val balanceBefore = destWallet.balance
            val balanceAfter = balanceBefore + amount

            val updated = walletDao.addBalanceWithVersion(
                walletId = destWallet.walletId,
                amount = amount,
                expectedVersion = destWallet.version,
                newVersion = destWallet.version + 1,
                updatedAt = now
            )
            if (updated == 0) throw ConcurrencyConflictException("Concurrency error in game reward balance credit.")

            val tx = WalletTransactionEntity(
                transactionId = UUID.randomUUID().toString(),
                idempotencyKey = idempotencyKey,
                timestamp = now,
                actorId = "SYSTEM_GAME_ENGINE",
                actorRole = "SYSTEM",
                sourceWalletId = null,
                destinationWalletId = destWallet.walletId,
                amount = amount,
                balanceBeforeSource = null,
                balanceAfterSource = null,
                balanceBeforeDestination = balanceBefore,
                balanceAfterDestination = balanceAfter,
                transactionType = TYPE_GAME_REWARD,
                reason = reason,
                referenceId = gameReferenceId,
                status = STATUS_COMPLETED,
                metadataJson = "{\"gameReferenceId\":\"$gameReferenceId\"}",
                createdAt = now
            )
            transactionDao.insert(tx)
            tx
        }
    }

    /**
     * Server-side Internal Gameplay Deduction.
     * Restricted strictly to authorized server-side game engines.
     */
    suspend fun executeServerGameplayDeduction(
        sourceAccountId: String,
        amount: Long,
        gameReferenceId: String,
        reason: String,
        idempotencyKey: String
    ): WalletTransactionEntity {
        require(amount > 0L) { "Deduction amount must be > 0" }
        return database.withTransaction {
            val account = accountDao.findById(sourceAccountId)
                ?: throw IllegalArgumentException("Player account not found")
            val sourceWallet = getOrCreateWallet(account.id, account.role)
            if (sourceWallet.balance < amount) {
                throw InsufficientBalanceException("Player has insufficient coin balance.")
            }
            val now = System.currentTimeMillis()
            val balanceBefore = sourceWallet.balance
            val balanceAfter = balanceBefore - amount

            val updated = walletDao.deductBalanceWithVersion(
                walletId = sourceWallet.walletId,
                amount = amount,
                expectedVersion = sourceWallet.version,
                newVersion = sourceWallet.version + 1,
                updatedAt = now
            )
            if (updated == 0) throw ConcurrencyConflictException("Concurrency error in game deduction.")

            val tx = WalletTransactionEntity(
                transactionId = UUID.randomUUID().toString(),
                idempotencyKey = idempotencyKey,
                timestamp = now,
                actorId = "SYSTEM_GAME_ENGINE",
                actorRole = "SYSTEM",
                sourceWalletId = sourceWallet.walletId,
                destinationWalletId = null,
                amount = amount,
                balanceBeforeSource = balanceBefore,
                balanceAfterSource = balanceAfter,
                balanceBeforeDestination = null,
                balanceAfterDestination = null,
                transactionType = TYPE_GAME_DEDUCTION,
                reason = reason,
                referenceId = gameReferenceId,
                status = STATUS_COMPLETED,
                metadataJson = "{\"gameReferenceId\":\"$gameReferenceId\"}",
                createdAt = now
            )
            transactionDao.insert(tx)
            tx
        }
    }

    /**
     * GET /wallets/accounts/{accountId}/summary:
     * Authoritative coin summary for an Agent or User, including balance and recent ledger activity.
     */
    suspend fun getAccountCoinSummary(
        context: SecurityContext,
        targetAccountId: String,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<AccountCoinSummaryDto> {
        val target = accountDao.findById(targetAccountId.trim())
            ?: return ServerResponse(false, 404, null, "ACCOUNT_NOT_FOUND", "Account not found.", requestId)

        val isAuthorized = when (context.role) {
            AccountRole.ADMIN -> true
            AccountRole.AGENT -> {
                target.id == context.accountId || target.parentId == context.accountId
            }
            AccountRole.USER -> {
                target.id == context.accountId
            }
        }

        if (!isAuthorized) {
            auditService.logEvent(
                actorId = context.accountId,
                actorRole = context.role.name,
                action = AuditActions.UNAUTHORIZED_COIN_OPERATION,
                targetId = targetAccountId,
                requestId = requestId,
                metadataJson = "{\"reason\":\"Unauthorized account coin summary query\"}"
            )
            return ServerResponse(false, 403, null, "RESOURCE_NOT_OWNED", "Access denied to requested account coins.", requestId)
        }

        val wallet = getOrCreateWallet(target.id, target.role)
        val recentRaw = transactionDao.getTransactionsForWallet(wallet.walletId, limit = 10, offset = 0)
        val recentDtos = recentRaw.map { it.toDto() }

        val summary = AccountCoinSummaryDto(
            accountId = target.id,
            loginId = target.loginId,
            fullName = target.fullName,
            role = target.role,
            status = target.status,
            balance = wallet.balance,
            currencyType = wallet.currencyType,
            lastTransaction = recentDtos.firstOrNull(),
            recentTransactions = recentDtos
        )

        return ServerResponse(true, 200, summary, null, null, requestId)
    }

    /**
     * GET /wallets/accounts/balances:
     * Batch balances query for efficient directory rendering.
     */
    suspend fun getAccountBalances(
        context: SecurityContext,
        accountIds: List<String>,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<BatchBalancesResponse> {
        val safeIds = accountIds.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (safeIds.isEmpty()) {
            return ServerResponse(true, 200, BatchBalancesResponse(emptyMap()), null, null, requestId)
        }

        val authorizedIds: List<String> = when (context.role) {
            AccountRole.ADMIN -> safeIds
            AccountRole.AGENT -> {
                val ownedAccounts = accountDao.findByParentId(context.accountId).map { it.id }.toSet()
                safeIds.filter { it == context.accountId || ownedAccounts.contains(it) }
            }
            AccountRole.USER -> {
                safeIds.filter { it == context.accountId }
            }
        }

        val wallets = walletDao.findByOwnerIds(authorizedIds)
        val balanceMap = wallets.associate { it.ownerId to it.balance }

        return ServerResponse(true, 200, BatchBalancesResponse(balanceMap), null, null, requestId)
    }

    private fun WalletEntity.toDto() = WalletDto(
        walletId = walletId,
        ownerId = ownerId,
        ownerRole = ownerRole,
        balance = balance,
        currencyType = currencyType,
        version = version,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun WalletTransactionEntity.toDto() = TransactionDto(
        transactionId = transactionId,
        idempotencyKey = idempotencyKey,
        timestamp = timestamp,
        actorId = actorId,
        actorRole = actorRole,
        sourceWalletId = sourceWalletId,
        destinationWalletId = destinationWalletId,
        amount = amount,
        balanceBeforeSource = balanceBeforeSource,
        balanceAfterSource = balanceAfterSource,
        balanceBeforeDestination = balanceBeforeDestination,
        balanceAfterDestination = balanceAfterDestination,
        transactionType = transactionType,
        reason = reason,
        referenceId = referenceId,
        status = status,
        metadataJson = metadataJson,
        createdAt = createdAt
    )
}

class InsufficientBalanceException(message: String) : Exception(message)
class ConcurrencyConflictException(message: String) : Exception(message)
