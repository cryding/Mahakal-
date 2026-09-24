package com.example.backend.service

import androidx.room.withTransaction
import com.example.backend.audit.AuditActions
import com.example.backend.audit.AuditService
import com.example.backend.database.MahakalServerDatabase
import com.example.backend.database.entity.AuditLogEntity
import com.example.backend.database.entity.GameEntity
import com.example.backend.database.entity.GameEntryEntity
import com.example.backend.database.entity.GameEventEntity
import com.example.backend.database.entity.GameOptionEntity
import com.example.backend.database.entity.GameProcessingEntity
import com.example.backend.database.entity.GameResultEntity
import com.example.backend.database.entity.WalletTransactionEntity
import com.example.backend.model.AdminEntriesReportDto
import com.example.backend.model.AdminGameDetailsDto
import com.example.backend.model.AdminGameOperationsSummaryDto
import com.example.backend.model.AdminGamesReportDto
import com.example.backend.model.AdminTransactionsReportDto
import com.example.backend.model.AuditLogDto
import com.example.backend.model.CancelGameRequest
import com.example.backend.model.CreateGameRequest
import com.example.backend.model.EntryStatus
import com.example.backend.model.FinalizeResultRequest
import com.example.backend.model.GameDto
import com.example.backend.model.GameEntryDto
import com.example.backend.model.GameEntryRequest
import com.example.backend.model.GameEntryStatisticsDto
import com.example.backend.model.GameEventDto
import com.example.backend.model.GameListResponse
import com.example.backend.model.GameOptionDto
import com.example.backend.model.GameProcessingDto
import com.example.backend.model.GameReportRowDto
import com.example.backend.model.GameResultDto
import com.example.backend.model.GameStatus
import com.example.backend.model.OptionStatisticsDto
import com.example.backend.model.PaginatedAdminGameListResponse
import com.example.backend.model.PaginatedAuditEventsResponse
import com.example.backend.model.PaginatedGameEntriesResponse
import com.example.backend.model.ReconciliationCorrectionRequest
import com.example.backend.model.ReconciliationCorrectionResponse
import com.example.backend.model.ReconciliationDiscrepancyDto
import com.example.backend.model.RewardReconciliationResultDto
import com.example.backend.model.ServerResponse
import com.example.backend.model.TransactionDto
import com.example.backend.model.TransactionReferenceDto
import com.example.backend.model.UserEntriesResponse
import com.example.backend.rbac.AccountRole
import com.example.backend.rbac.AccountStatus
import com.example.backend.rbac.Permission
import com.example.backend.rbac.SecurityContext
import com.example.backend.security.RateLimiter
import java.util.UUID

/**
 * Production-grade Server-Authoritative Game & Prediction Engine.
 * Strictly uses non-monetary virtual coin accounting via existing WalletTransactionService mechanisms.
 */
class GameEngineService(
    private val database: MahakalServerDatabase,
    private val auditService: AuditService,
    private val rateLimiter: RateLimiter,
    private val walletTransactionService: WalletTransactionService,
    private val notificationService: NotificationService? = null
) {

    private val gameDao = database.gameDao()
    private val walletDao = database.walletDao()
    private val accountDao = database.accountDao()
    private val transactionDao = database.walletTransactionDao()

    /**
     * POST /admin/games: Administrator creates a new Game with options atomically.
     */
    suspend fun createGame(
        context: SecurityContext,
        request: CreateGameRequest,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<GameDto> {
        // 1. RBAC Guard: ADMIN role only
        if (context.role != AccountRole.ADMIN) {
            logAccessDenied(context, "CREATE_GAME", requestId)
            return ServerResponse(false, 403, null, "FORBIDDEN", "Only Administrators can create games.", requestId)
        }
        if (context.status != AccountStatus.ACTIVE) {
            return ServerResponse(false, 403, null, "ACCOUNT_INACTIVE", "Admin account is not active.", requestId)
        }

        // 2. Validate Parameters
        val title = request.title.trim()
        if (title.length < 3 || title.length > 120) {
            return ServerResponse(false, 422, null, "INVALID_TITLE", "Title must be between 3 and 120 characters.", requestId)
        }
        val description = request.description.trim()

        if (request.options.size < 2) {
            return ServerResponse(false, 422, null, "INSUFFICIENT_OPTIONS", "A game must have at least 2 distinct options.", requestId)
        }
        val optionCodes = request.options.map { it.optionCode.trim().uppercase() }
        if (optionCodes.distinct().size != optionCodes.size) {
            return ServerResponse(false, 422, null, "DUPLICATE_OPTION_CODES", "Each option must have a unique optionCode.", requestId)
        }

        val now = System.currentTimeMillis()
        if (request.entryDeadline <= request.startTime) {
            return ServerResponse(false, 422, null, "INVALID_SCHEDULE", "Entry deadline must be after start time.", requestId)
        }
        if (request.resultTime < request.entryDeadline) {
            return ServerResponse(false, 422, null, "INVALID_SCHEDULE", "Result time must be on or after entry deadline.", requestId)
        }

        if (request.minCoins < 1L) {
            return ServerResponse(false, 422, null, "INVALID_LIMITS", "Minimum virtual coins must be at least 1.", requestId)
        }
        if (request.maxCoins < request.minCoins) {
            return ServerResponse(false, 422, null, "INVALID_LIMITS", "Maximum virtual coins must be >= minimum coins.", requestId)
        }
        if (request.rewardMultiplier < 1.0 || request.rewardMultiplier > 100.0) {
            return ServerResponse(false, 422, null, "INVALID_MULTIPLIER", "Reward multiplier must be between 1.0 and 100.0.", requestId)
        }

        val initialStatus = when {
            request.startTime <= now && now < request.entryDeadline -> GameStatus.OPEN
            request.startTime > now -> GameStatus.SCHEDULED
            else -> GameStatus.CLOSED
        }

        val gameId = UUID.randomUUID().toString()
        val gameEntity = GameEntity(
            gameId = gameId,
            gameType = request.gameType.trim().uppercase(),
            title = title,
            description = description,
            status = initialStatus,
            startTime = request.startTime,
            entryDeadline = request.entryDeadline,
            resultTime = request.resultTime,
            minCoins = request.minCoins,
            maxCoins = request.maxCoins,
            rewardMultiplier = request.rewardMultiplier,
            createdBy = context.accountId,
            createdAt = now,
            updatedAt = now,
            version = 0L
        )

        val optionEntities = request.options.mapIndexed { idx, opt ->
            GameOptionEntity(
                optionId = UUID.randomUUID().toString(),
                gameId = gameId,
                optionCode = opt.optionCode.trim().uppercase(),
                displayName = opt.displayName.trim(),
                status = "ACTIVE",
                metadataJson = opt.metadataJson
            )
        }

        try {
            database.withTransaction {
                gameDao.insertGame(gameEntity)
                gameDao.insertOptions(optionEntities)
                gameDao.insertEvent(
                    GameEventEntity(
                        eventId = UUID.randomUUID().toString(),
                        gameId = gameId,
                        eventType = "GAME_CREATED",
                        actorId = context.accountId,
                        actorRole = context.role.name,
                        timestamp = now,
                        metadataJson = "{\"status\":\"$initialStatus\",\"optionsCount\":${optionEntities.size}}"
                    )
                )
            }

            auditService.logEvent(
                actorId = context.accountId,
                actorRole = context.role.name,
                action = AuditActions.GAME_CREATED,
                targetId = gameId,
                targetType = "GAME",
                requestId = requestId,
                metadataJson = "{\"title\":\"$title\",\"initialStatus\":\"$initialStatus\"}"
            )

            val dto = gameEntity.toDto(options = optionEntities.map { it.toDto() })
            if (initialStatus == GameStatus.OPEN) {
                notificationService?.onGameOpened(gameEntity, context.accountId)
            }
            return ServerResponse(true, 201, dto, null, "Game created successfully.", requestId)
        } catch (e: Exception) {
            return ServerResponse(false, 500, null, "GAME_CREATION_FAILED", "Failed to create game: ${e.localizedMessage}", requestId)
        }
    }

    /**
     * POST /admin/games/{gameId}/open: Administrator transitions game to OPEN.
     */
    suspend fun openGame(
        context: SecurityContext,
        gameId: String,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<GameDto> {
        if (context.role != AccountRole.ADMIN) {
            logAccessDenied(context, "OPEN_GAME", requestId)
            return ServerResponse(false, 403, null, "FORBIDDEN", "Only Administrators can open games.", requestId)
        }

        val game = gameDao.findGameById(gameId)
            ?: return ServerResponse(false, 404, null, "GAME_NOT_FOUND", "Game '$gameId' not found.", requestId)

        val now = System.currentTimeMillis()
        if (now >= game.entryDeadline) {
            return ServerResponse(false, 422, null, "DEADLINE_PASSED", "Cannot open game because entry deadline has passed.", requestId)
        }

        // Strict state machine validation: Only DRAFT or SCHEDULED can transition to OPEN
        if (game.status != GameStatus.DRAFT && game.status != GameStatus.SCHEDULED) {
            return ServerResponse(false, 422, null, "INVALID_STATE_TRANSITION", "Cannot open game in state '${game.status}'. Only DRAFT or SCHEDULED can be opened.", requestId)
        }

        try {
            database.withTransaction {
                val updatedRows = gameDao.updateGameStatusWithVersion(
                    gameId = gameId,
                    newStatus = GameStatus.OPEN,
                    expectedVersion = game.version,
                    newVersion = game.version + 1,
                    updatedAt = now
                )
                if (updatedRows == 0) throw ConcurrencyConflictException("Concurrent game state update detected.")

                gameDao.insertEvent(
                    GameEventEntity(
                        eventId = UUID.randomUUID().toString(),
                        gameId = gameId,
                        eventType = "GAME_OPENED",
                        actorId = context.accountId,
                        actorRole = context.role.name,
                        timestamp = now
                    )
                )
            }

            auditService.logEvent(
                actorId = context.accountId,
                actorRole = context.role.name,
                action = AuditActions.GAME_OPENED,
                targetId = gameId,
                targetType = "GAME",
                requestId = requestId
            )

            val updatedGame = gameDao.findGameById(gameId)!!
            val options = gameDao.getOptionsForGame(gameId).map { it.toDto() }
            notificationService?.onGameOpened(updatedGame, context.accountId)
            return ServerResponse(true, 200, updatedGame.toDto(options), null, "Game opened successfully.", requestId)
        } catch (e: Exception) {
            return ServerResponse(false, 409, null, "STATE_UPDATE_FAILED", e.localizedMessage, requestId)
        }
    }

    /**
     * POST /admin/games/{gameId}/close: Administrator manually closes game entries.
     */
    suspend fun closeGame(
        context: SecurityContext,
        gameId: String,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<GameDto> {
        if (context.role != AccountRole.ADMIN) {
            logAccessDenied(context, "CLOSE_GAME", requestId)
            return ServerResponse(false, 403, null, "FORBIDDEN", "Only Administrators can close games.", requestId)
        }

        val game = gameDao.findGameById(gameId)
            ?: return ServerResponse(false, 404, null, "GAME_NOT_FOUND", "Game '$gameId' not found.", requestId)

        if (game.status != GameStatus.OPEN) {
            return ServerResponse(false, 422, null, "INVALID_STATE_TRANSITION", "Cannot close game in state '${game.status}'. Only OPEN games can be closed.", requestId)
        }

        val now = System.currentTimeMillis()
        try {
            database.withTransaction {
                val updatedRows = gameDao.updateGameStatusWithVersion(
                    gameId = gameId,
                    newStatus = GameStatus.CLOSED,
                    expectedVersion = game.version,
                    newVersion = game.version + 1,
                    updatedAt = now
                )
                if (updatedRows == 0) throw ConcurrencyConflictException("Concurrent game state update detected.")

                gameDao.insertEvent(
                    GameEventEntity(
                        eventId = UUID.randomUUID().toString(),
                        gameId = gameId,
                        eventType = "GAME_CLOSED",
                        actorId = context.accountId,
                        actorRole = context.role.name,
                        timestamp = now
                    )
                )
            }

            auditService.logEvent(
                actorId = context.accountId,
                actorRole = context.role.name,
                action = AuditActions.GAME_CLOSED,
                targetId = gameId,
                targetType = "GAME",
                requestId = requestId
            )

            val updatedGame = gameDao.findGameById(gameId)!!
            val options = gameDao.getOptionsForGame(gameId).map { it.toDto() }
            notificationService?.onGameClosing(updatedGame)
            return ServerResponse(true, 200, updatedGame.toDto(options), null, "Game closed successfully.", requestId)
        } catch (e: Exception) {
            return ServerResponse(false, 409, null, "STATE_UPDATE_FAILED", e.localizedMessage, requestId)
        }
    }

    /**
     * POST /admin/games/{gameId}/cancel: Administrator cancels game and refunds all entries atomically.
     */
    suspend fun cancelGame(
        context: SecurityContext,
        gameId: String,
        request: CancelGameRequest,
        idempotencyKey: String?,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<GameDto> {
        if (context.role != AccountRole.ADMIN) {
            logAccessDenied(context, "CANCEL_GAME", requestId)
            return ServerResponse(false, 403, null, "FORBIDDEN", "Only Administrators can cancel games.", requestId)
        }

        val game = gameDao.findGameById(gameId)
            ?: return ServerResponse(false, 404, null, "GAME_NOT_FOUND", "Game '$gameId' not found.", requestId)

        if (game.status == GameStatus.CANCELLED) {
            val options = gameDao.getOptionsForGame(gameId).map { it.toDto() }
            return ServerResponse(true, 200, game.toDto(options), null, "Game was already cancelled.", requestId)
        }
        if (game.status == GameStatus.RESULT_FINALIZED || game.status == GameStatus.ARCHIVED) {
            return ServerResponse(false, 422, null, "INVALID_STATE_TRANSITION", "Cannot cancel finalized or archived game.", requestId)
        }

        val now = System.currentTimeMillis()
        val reason = request.reason.trim().ifEmpty { "Game cancelled by administrator" }
        var refundedEntries: List<GameEntryEntity> = emptyList()

        try {
            database.withTransaction {
                val updatedRows = gameDao.updateGameStatusWithVersion(
                    gameId = gameId,
                    newStatus = GameStatus.CANCELLED,
                    expectedVersion = game.version,
                    newVersion = game.version + 1,
                    updatedAt = now
                )
                if (updatedRows == 0) throw ConcurrencyConflictException("Concurrent game state update detected.")

                // Refund all CONFIRMED entries
                val confirmedEntries = gameDao.getEntriesForGameWithStatus(gameId, EntryStatus.CONFIRMED)
                refundedEntries = confirmedEntries
                for (entry in confirmedEntries) {
                    val userWallet = walletDao.findByOwnerId(entry.userId)
                        ?: throw IllegalStateException("User wallet not found for user ${entry.userId}")

                    val refundIdempotencyKey = "refund_${entry.entryId}"
                    val existingRefund = transactionDao.findByIdempotencyKey(refundIdempotencyKey)
                    if (existingRefund == null) {
                        val addSuccess = walletDao.addBalanceWithVersion(
                            walletId = userWallet.walletId,
                            amount = entry.virtualCoinAmount,
                            expectedVersion = userWallet.version,
                            newVersion = userWallet.version + 1,
                            updatedAt = now
                        )
                        if (addSuccess == 0) throw ConcurrencyConflictException("Concurrent wallet update refunding user ${entry.userId}.")

                        val refundTx = WalletTransactionEntity(
                            transactionId = UUID.randomUUID().toString(),
                            idempotencyKey = refundIdempotencyKey,
                            timestamp = now,
                            actorId = context.accountId,
                            actorRole = context.role.name,
                            sourceWalletId = null,
                            destinationWalletId = userWallet.walletId,
                            amount = entry.virtualCoinAmount,
                            balanceBeforeSource = null,
                            balanceAfterSource = null,
                            balanceBeforeDestination = userWallet.balance,
                            balanceAfterDestination = userWallet.balance + entry.virtualCoinAmount,
                            transactionType = WalletTransactionService.TYPE_SYSTEM_ADJUSTMENT,
                            reason = "Refund for cancelled game '${game.title}': $reason",
                            referenceId = entry.entryId,
                            status = WalletTransactionService.STATUS_COMPLETED,
                            metadataJson = "{\"gameId\":\"$gameId\",\"entryId\":\"${entry.entryId}\"}",
                            createdAt = now
                        )
                        transactionDao.insert(refundTx)
                    }

                    gameDao.updateEntry(entry.copy(status = EntryStatus.REFUNDED, updatedAt = now))
                }

                gameDao.insertEvent(
                    GameEventEntity(
                        eventId = UUID.randomUUID().toString(),
                        gameId = gameId,
                        eventType = "GAME_CANCELLED",
                        actorId = context.accountId,
                        actorRole = context.role.name,
                        timestamp = now,
                        metadataJson = "{\"reason\":\"$reason\",\"refundedCount\":${confirmedEntries.size}}"
                    )
                )
            }

            auditService.logEvent(
                actorId = context.accountId,
                actorRole = context.role.name,
                action = AuditActions.GAME_CANCELLED,
                targetId = gameId,
                targetType = "GAME",
                requestId = requestId,
                metadataJson = "{\"reason\":\"$reason\"}"
            )

            val updatedGame = gameDao.findGameById(gameId)!!
            val options = gameDao.getOptionsForGame(gameId).map { it.toDto() }
            notificationService?.onGameCancelled(updatedGame, refundedEntries)
            return ServerResponse(true, 200, updatedGame.toDto(options), null, "Game cancelled and entries refunded successfully.", requestId)
        } catch (e: Exception) {
            return ServerResponse(false, 500, null, "CANCELLATION_FAILED", "Failed to cancel game: ${e.localizedMessage}", requestId)
        }
    }

    /**
     * GET /games: List active games for discovery.
     * Evaluates schedule deadlines server-side.
     */
    suspend fun getGames(
        context: SecurityContext,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<GameListResponse> {
        val now = System.currentTimeMillis()

        // Auto-promote schedules if needed
        val allGames = if (context.role == AccountRole.ADMIN) {
            gameDao.getAllGames()
        } else {
            gameDao.getGamesByStatus(GameStatus.ACTIVE_DISCOVERY_STATUSES)
        }

        // Server-side schedule maintenance: auto-close expired OPEN games
        for (game in allGames) {
            if (game.status == GameStatus.OPEN && now >= game.entryDeadline) {
                gameDao.updateGameStatusWithVersion(
                    gameId = game.gameId,
                    newStatus = GameStatus.CLOSED,
                    expectedVersion = game.version,
                    newVersion = game.version + 1,
                    updatedAt = now
                )
            } else if (game.status == GameStatus.SCHEDULED && now >= game.startTime && now < game.entryDeadline) {
                gameDao.updateGameStatusWithVersion(
                    gameId = game.gameId,
                    newStatus = GameStatus.OPEN,
                    expectedVersion = game.version,
                    newVersion = game.version + 1,
                    updatedAt = now
                )
            }
        }

        // Refresh list
        val refreshedGames = if (context.role == AccountRole.ADMIN) {
            gameDao.getAllGames()
        } else {
            gameDao.getGamesByStatus(GameStatus.ACTIVE_DISCOVERY_STATUSES)
        }

        val gameDtos = refreshedGames.map { g ->
            val options = gameDao.getOptionsForGame(g.gameId).map { it.toDto() }
            val entryCount = gameDao.countEntriesForGame(g.gameId)
            val result = gameDao.findResultByGameId(g.gameId)?.toDto()
            g.toDto(options, entryCount, result)
        }

        return ServerResponse(true, 200, GameListResponse(gameDtos), null, null, requestId)
    }

    /**
     * GET /games/{gameId}: Get single game with options and results.
     */
    suspend fun getGameDetails(
        context: SecurityContext,
        gameId: String,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<GameDto> {
        val game = gameDao.findGameById(gameId)
            ?: return ServerResponse(false, 404, null, "GAME_NOT_FOUND", "Game '$gameId' not found.", requestId)

        val options = gameDao.getOptionsForGame(gameId).map { it.toDto() }
        val entryCount = gameDao.countEntriesForGame(gameId)
        val result = gameDao.findResultByGameId(gameId)?.toDto()

        return ServerResponse(true, 200, game.toDto(options, entryCount, result), null, null, requestId)
    }

    /**
     * POST /games/{gameId}/entries: User places a game entry.
     * Atomic Operation:
     * Validates Rules + Deducts Virtual Coins via WalletDao + Inserts Ledger Tx + Inserts GameEntry.
     */
    suspend fun submitGameEntry(
        context: SecurityContext,
        gameId: String,
        request: GameEntryRequest,
        idempotencyKey: String?,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<GameEntryDto> {
        // 1. Authenticate & Authorize role USER
        if (context.role != AccountRole.USER) {
            logAccessDenied(context, "SUBMIT_GAME_ENTRY", requestId)
            return ServerResponse(false, 403, null, "FORBIDDEN", "Only Users can submit game entries.", requestId)
        }
        if (context.status != AccountStatus.ACTIVE) {
            return ServerResponse(false, 403, null, "ACCOUNT_INACTIVE", "User account is not active.", requestId)
        }

        // 2. Validate Idempotency Key
        val trimmedKey = idempotencyKey?.trim()
        if (trimmedKey.isNullOrEmpty()) {
            return ServerResponse(false, 400, null, "MISSING_IDEMPOTENCY_KEY", "Idempotency-Key header is required.", requestId)
        }

        // Check if entry already processed under this idempotency key
        val existingEntry = gameDao.findEntryByIdempotencyKey(trimmedKey)
        if (existingEntry != null) {
            val game = gameDao.findGameById(existingEntry.gameId)
            val option = gameDao.findOptionById(existingEntry.selectedOptionId)
            return ServerResponse(
                true,
                200,
                existingEntry.toDto(gameTitle = game?.title, optionName = option?.displayName),
                null,
                "Idempotent response: Entry already processed.",
                requestId
            )
        }

        // 3. Validate Amount
        val amount = request.virtualCoinAmount
        if (amount <= 0L) {
            return ServerResponse(false, 422, null, "INVALID_AMOUNT", "Virtual coin amount must be greater than 0.", requestId)
        }

        val now = System.currentTimeMillis()

        try {
            val createdEntry = database.withTransaction {
                // Secondary check inside transaction for concurrency safety
                val innerCheck = gameDao.findEntryByIdempotencyKey(trimmedKey)
                if (innerCheck != null) {
                    return@withTransaction innerCheck
                }

                // 4. Validate Game & Status
                val game = gameDao.findGameById(gameId)
                    ?: throw IllegalArgumentException("Game '$gameId' not found.")

                if (now >= game.entryDeadline) {
                    // Automatically transition game to CLOSED if past deadline
                    if (game.status == GameStatus.OPEN) {
                        gameDao.updateGameStatusWithVersion(gameId, GameStatus.CLOSED, game.version, game.version + 1, now)
                    }
                    throw IllegalStateException("Entry deadline has already passed for game '${game.title}'.")
                }

                if (game.status != GameStatus.OPEN) {
                    throw IllegalStateException("Game is not accepting entries. Current status is ${game.status}.")
                }

                // 5. Validate Limits
                if (amount < game.minCoins) {
                    throw IllegalArgumentException("Entry amount ($amount) is below game minimum of ${game.minCoins} virtual coins.")
                }
                if (amount > game.maxCoins) {
                    throw IllegalArgumentException("Entry amount ($amount) exceeds game maximum of ${game.maxCoins} virtual coins.")
                }

                // 6. Validate Selected Option
                val option = gameDao.findOptionById(request.selectedOptionId)
                    ?: throw IllegalArgumentException("Selected option '${request.selectedOptionId}' not found.")
                if (option.gameId != gameId) {
                    throw IllegalArgumentException("Selected option does not belong to this game.")
                }
                if (option.status != "ACTIVE") {
                    throw IllegalStateException("Selected option is not active.")
                }

                // 7. Atomic Virtual Coin Deduction from User's Wallet
                val userWallet = walletTransactionService.getOrCreateWallet(context.accountId, context.role.name)
                if (userWallet.balance < amount) {
                    auditService.logEvent(
                        actorId = context.accountId,
                        actorRole = context.role.name,
                        action = AuditActions.INSUFFICIENT_BALANCE,
                        targetId = gameId,
                        requestId = requestId,
                        metadataJson = "{\"available\":${userWallet.balance},\"requested\":$amount}"
                    )
                    throw InsufficientBalanceException("Insufficient virtual coins. Balance: ${userWallet.balance}, required: $amount.")
                }

                val balanceBefore = userWallet.balance
                val balanceAfter = balanceBefore - amount

                val updatedRows = walletDao.deductBalanceWithVersion(
                    walletId = userWallet.walletId,
                    amount = amount,
                    expectedVersion = userWallet.version,
                    newVersion = userWallet.version + 1,
                    updatedAt = now
                )
                if (updatedRows == 0) {
                    throw ConcurrencyConflictException("Concurrent balance modification detected on wallet. Please retry.")
                }

                // 8. Record Ledger Deduction Transaction
                val txEntity = WalletTransactionEntity(
                    transactionId = UUID.randomUUID().toString(),
                    idempotencyKey = "entry_tx_$trimmedKey",
                    timestamp = now,
                    actorId = context.accountId,
                    actorRole = context.role.name,
                    sourceWalletId = userWallet.walletId,
                    destinationWalletId = null,
                    amount = amount,
                    balanceBeforeSource = balanceBefore,
                    balanceAfterSource = balanceAfter,
                    balanceBeforeDestination = null,
                    balanceAfterDestination = null,
                    transactionType = WalletTransactionService.TYPE_GAME_DEDUCTION,
                    reason = "Game entry for '${game.title}' on '${option.displayName}'",
                    referenceId = null, // will link to entryId
                    status = WalletTransactionService.STATUS_COMPLETED,
                    metadataJson = "{\"gameId\":\"$gameId\",\"optionId\":\"${option.optionId}\"}",
                    createdAt = now
                )
                transactionDao.insert(txEntity)

                // 9. Record Game Entry
                val entryEntity = GameEntryEntity(
                    entryId = UUID.randomUUID().toString(),
                    gameId = gameId,
                    userId = context.accountId,
                    selectedOptionId = option.optionId,
                    virtualCoinAmount = amount,
                    status = EntryStatus.CONFIRMED,
                    idempotencyKey = trimmedKey,
                    deductionTransactionId = txEntity.transactionId,
                    rewardTransactionId = null,
                    rewardAmount = null,
                    createdAt = now,
                    updatedAt = now
                )
                gameDao.insertEntry(entryEntity)

                // 10. Record Game Event
                gameDao.insertEvent(
                    GameEventEntity(
                        eventId = UUID.randomUUID().toString(),
                        gameId = gameId,
                        eventType = "ENTRY_CREATED",
                        actorId = context.accountId,
                        actorRole = context.role.name,
                        timestamp = now,
                        metadataJson = "{\"entryId\":\"${entryEntity.entryId}\",\"amount\":$amount,\"option\":\"${option.optionCode}\"}"
                    )
                )

                entryEntity
            }

            auditService.logEvent(
                actorId = context.accountId,
                actorRole = context.role.name,
                action = AuditActions.ENTRY_CREATED,
                targetId = createdEntry.entryId,
                targetType = "GAME_ENTRY",
                requestId = requestId,
                metadataJson = "{\"gameId\":\"$gameId\",\"amount\":$amount}"
            )

            val game = gameDao.findGameById(gameId)
            val option = gameDao.findOptionById(createdEntry.selectedOptionId)
            val dto = createdEntry.toDto(gameTitle = game?.title, optionName = option?.displayName)
            notificationService?.onGameEntryConfirmed(createdEntry, game?.title ?: "Game")
            return ServerResponse(true, 201, dto, null, "Game entry confirmed successfully.", requestId)
        } catch (e: InsufficientBalanceException) {
            return ServerResponse(false, 403, null, "INSUFFICIENT_BALANCE", e.message, requestId)
        } catch (e: ConcurrencyConflictException) {
            return ServerResponse(false, 409, null, "CONCURRENT_UPDATE", e.message, requestId)
        } catch (e: IllegalArgumentException) {
            return ServerResponse(false, 422, null, "VALIDATION_ERROR", e.message, requestId)
        } catch (e: IllegalStateException) {
            return ServerResponse(false, 400, null, "INVALID_GAME_STATE", e.message, requestId)
        } catch (e: Exception) {
            return ServerResponse(false, 500, null, "ENTRY_FAILED", "Failed to submit entry: ${e.localizedMessage}", requestId)
        }
    }

    /**
     * POST /admin/games/{gameId}/result: Administrator finalizes winning result.
     * Evaluates winning entries and credits rewards through the existing Wallet ledger.
     */
    suspend fun finalizeGameResult(
        context: SecurityContext,
        gameId: String,
        request: FinalizeResultRequest,
        idempotencyKey: String?,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<GameResultDto> {
        // 1. RBAC Guard: ADMIN role only
        if (context.role != AccountRole.ADMIN) {
            logAccessDenied(context, "FINALIZE_RESULT", requestId)
            return ServerResponse(false, 403, null, "FORBIDDEN", "Only Administrators can finalize game results.", requestId)
        }

        val trimmedKey = idempotencyKey?.trim()
        if (trimmedKey.isNullOrEmpty()) {
            return ServerResponse(false, 400, null, "MISSING_IDEMPOTENCY_KEY", "Idempotency-Key header is required.", requestId)
        }

        val rateLimitKey = "finalize_${context.accountId}_$gameId"
        if (rateLimiter.isActionRateLimited(rateLimitKey, maxRequests = 5, windowMs = 60_000L)) {
            return ServerResponse(false, 429, null, "RATE_LIMIT_EXCEEDED", "Too many finalization requests. Please wait.", requestId)
        }

        // Check if already finalized
        val existingResult = gameDao.findResultByGameId(gameId)
        if (existingResult != null) {
            val winningOpt = gameDao.findOptionById(existingResult.winningOptionId)
            return ServerResponse(
                true,
                200,
                existingResult.toDto(winningOptionName = winningOpt?.displayName),
                null,
                "Game result was already finalized.",
                requestId
            )
        }

        val now = System.currentTimeMillis()
        var targetGame: GameEntity? = null
        var processedEntries: List<GameEntryEntity> = emptyList()

        try {
            val resultEntity = database.withTransaction {
                val game = gameDao.findGameById(gameId)
                    ?: throw IllegalArgumentException("Game '$gameId' not found.")
                targetGame = game

                if (game.status == GameStatus.CANCELLED || game.status == GameStatus.ARCHIVED) {
                    throw IllegalStateException("Cannot finalize result for ${game.status} game.")
                }

                // Verify winning option exists and belongs to game
                val winningOption = gameDao.findOptionById(request.winningOptionId)
                    ?: throw IllegalArgumentException("Winning option '${request.winningOptionId}' not found.")
                if (winningOption.gameId != gameId) {
                    throw IllegalArgumentException("Winning option does not belong to this game.")
                }

                // Process all CONFIRMED entries
                val confirmedEntries = gameDao.getEntriesForGameWithStatus(gameId, EntryStatus.CONFIRMED)
                processedEntries = confirmedEntries

                // Track initial processing state
                val processing = GameProcessingEntity(
                    processingId = UUID.randomUUID().toString(),
                    gameId = gameId,
                    status = "PROCESSING",
                    startedAt = now,
                    completedAt = null,
                    attemptCount = 1,
                    lastErrorCode = null,
                    lastErrorMessage = null,
                    correlationId = requestId,
                    totalEntries = confirmedEntries.size,
                    processedEntries = 0,
                    failedEntries = 0,
                    updatedAt = now
                )
                gameDao.insertOrUpdateProcessing(processing)

                // Update game status to RESULT_FINALIZED
                gameDao.updateGameStatusWithVersion(
                    gameId = gameId,
                    newStatus = GameStatus.RESULT_FINALIZED,
                    expectedVersion = game.version,
                    newVersion = game.version + 1,
                    updatedAt = now
                )
                var winnersCount = 0
                var totalRewardsPaid = 0L

                for (entry in confirmedEntries) {
                    if (entry.selectedOptionId == request.winningOptionId) {
                        // WINNER
                        winnersCount++
                        val rewardCoins = (entry.virtualCoinAmount * game.rewardMultiplier).toLong()
                        totalRewardsPaid += rewardCoins

                        val userWallet = walletTransactionService.getOrCreateWallet(entry.userId, AccountRole.USER.name)

                        // Reward transaction
                        val rewardIdempotencyKey = "reward_${entry.entryId}"
                        val existingRewardTx = transactionDao.findByIdempotencyKey(rewardIdempotencyKey)
                        val rewardTxId = if (existingRewardTx == null) {
                            val addSuccess = walletDao.addBalanceWithVersion(
                                walletId = userWallet.walletId,
                                amount = rewardCoins,
                                expectedVersion = userWallet.version,
                                newVersion = userWallet.version + 1,
                                updatedAt = now
                            )
                            if (addSuccess == 0) throw ConcurrencyConflictException("Concurrent wallet update awarding user ${entry.userId}.")

                            val rewardTx = WalletTransactionEntity(
                                transactionId = UUID.randomUUID().toString(),
                                idempotencyKey = rewardIdempotencyKey,
                                timestamp = now,
                                actorId = context.accountId,
                                actorRole = context.role.name,
                                sourceWalletId = null,
                                destinationWalletId = userWallet.walletId,
                                amount = rewardCoins,
                                balanceBeforeSource = null,
                                balanceAfterSource = null,
                                balanceBeforeDestination = userWallet.balance,
                                balanceAfterDestination = userWallet.balance + rewardCoins,
                                transactionType = WalletTransactionService.TYPE_GAME_REWARD,
                                reason = "Reward for game '${game.title}' (Option '${winningOption.displayName}')",
                                referenceId = entry.entryId,
                                status = WalletTransactionService.STATUS_COMPLETED,
                                metadataJson = "{\"gameId\":\"$gameId\",\"entryId\":\"${entry.entryId}\",\"multiplier\":${game.rewardMultiplier}}",
                                createdAt = now
                            )
                            transactionDao.insert(rewardTx)
                            rewardTx.transactionId
                        } else {
                            existingRewardTx.transactionId
                        }

                        gameDao.updateEntry(
                            entry.copy(
                                status = EntryStatus.WON,
                                rewardAmount = rewardCoins,
                                rewardTransactionId = rewardTxId,
                                updatedAt = now
                            )
                        )
                    } else {
                        // NON-WINNING ENTRY
                        gameDao.updateEntry(
                            entry.copy(
                                status = EntryStatus.LOST,
                                updatedAt = now
                            )
                        )
                    }
                }

                val newResult = GameResultEntity(
                    resultId = UUID.randomUUID().toString(),
                    gameId = gameId,
                    winningOptionId = request.winningOptionId,
                    resultStatus = "FINALIZED",
                    finalizedBy = context.accountId,
                    finalizedAt = now,
                    resultVersion = 1L,
                    metadataJson = "{\"winners\":$winnersCount,\"totalRewards\":$totalRewardsPaid,\"reason\":\"${request.reason}\"}"
                )
                gameDao.insertResult(newResult)

                gameDao.insertEvent(
                    GameEventEntity(
                        eventId = UUID.randomUUID().toString(),
                        gameId = gameId,
                        eventType = "RESULT_FINALIZED",
                        actorId = context.accountId,
                        actorRole = context.role.name,
                        timestamp = now,
                        metadataJson = "{\"winningOption\":\"${winningOption.optionCode}\",\"winners\":$winnersCount,\"rewards\":$totalRewardsPaid}"
                    )
                )

                // Update processing to COMPLETED
                val completedProcessing = processing.copy(
                    status = "COMPLETED",
                    completedAt = now,
                    processedEntries = confirmedEntries.size,
                    updatedAt = now
                )
                gameDao.insertOrUpdateProcessing(completedProcessing)

                newResult
            }

            auditService.logEvent(
                actorId = context.accountId,
                actorRole = context.role.name,
                action = AuditActions.RESULT_FINALIZED,
                targetId = gameId,
                targetType = "GAME",
                requestId = requestId,
                metadataJson = "{\"winningOptionId\":\"${request.winningOptionId}\"}"
            )

            val winningOpt = gameDao.findOptionById(resultEntity.winningOptionId)
            val dto = resultEntity.toDto(winningOptionName = winningOpt?.displayName)
            val finalGame = targetGame
            if (finalGame != null) {
                val updatedEntries = gameDao.getEntriesForGame(gameId)
                notificationService?.onGameResultFinalized(finalGame, winningOpt?.displayName ?: "Option", updatedEntries)
            }
            return ServerResponse(true, 200, dto, null, "Game result finalized and rewards distributed successfully.", requestId)
        } catch (e: Exception) {
            val failedProc = GameProcessingEntity(
                processingId = UUID.randomUUID().toString(),
                gameId = gameId,
                status = "FAILED",
                startedAt = now,
                completedAt = null,
                attemptCount = 1,
                lastErrorCode = "FINALIZATION_FAILED",
                lastErrorMessage = e.localizedMessage,
                correlationId = requestId,
                totalEntries = 0,
                processedEntries = 0,
                failedEntries = 0,
                updatedAt = System.currentTimeMillis()
            )
            gameDao.insertOrUpdateProcessing(failedProc)
            auditService.logEvent(
                actorId = context.accountId,
                actorRole = context.role.name,
                action = AuditActions.RESULT_PROCESSING_FAILED,
                targetId = gameId,
                targetType = "GAME",
                requestId = requestId,
                metadataJson = "{\"error\":\"${e.localizedMessage}\"}"
            )
            return ServerResponse(false, 500, null, "FINALIZATION_FAILED", "Failed to finalize result: ${e.localizedMessage}", requestId)
        }
    }

    /**
     * GET /games/my-entries: Authenticated user views their own entry history.
     */
    suspend fun getMyEntries(
        context: SecurityContext,
        limit: Int = 100,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<UserEntriesResponse> {
        if (context.role != AccountRole.USER) {
            return ServerResponse(false, 403, null, "FORBIDDEN", "Only Users have personal game entries.", requestId)
        }

        val entries = gameDao.getEntriesForUser(context.accountId, limit)
        val entryDtos = entries.map { entry ->
            val game = gameDao.findGameById(entry.gameId)
            val option = gameDao.findOptionById(entry.selectedOptionId)
            entry.toDto(gameTitle = game?.title, optionName = option?.displayName)
        }

        return ServerResponse(true, 200, UserEntriesResponse(entryDtos), null, null, requestId)
    }

    /**
     * GET /admin/games/{gameId}/entries: Admin views all entries for a specific game.
     */
    suspend fun getGameEntriesAdmin(
        context: SecurityContext,
        gameId: String,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<UserEntriesResponse> {
        if (context.role != AccountRole.ADMIN) {
            logAccessDenied(context, "GET_GAME_ENTRIES_ADMIN", requestId)
            return ServerResponse(false, 403, null, "FORBIDDEN", "Only Administrators can view all game entries.", requestId)
        }

        val game = gameDao.findGameById(gameId)
            ?: return ServerResponse(false, 404, null, "GAME_NOT_FOUND", "Game '$gameId' not found.", requestId)

        val entries = gameDao.getEntriesForGame(gameId)
        val entryDtos = entries.map { entry ->
            val option = gameDao.findOptionById(entry.selectedOptionId)
            entry.toDto(gameTitle = game.title, optionName = option?.displayName)
        }

        return ServerResponse(true, 200, UserEntriesResponse(entryDtos), null, null, requestId)
    }

    /**
     * GET /agent/users/{userId}/game-entries: Agent views entries for a subordinated user under their hierarchy.
     */
    suspend fun getAgentSubordinatedUserEntries(
        context: SecurityContext,
        userId: String,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<UserEntriesResponse> {
        if (context.role != AccountRole.AGENT) {
            logAccessDenied(context, "GET_AGENT_USER_ENTRIES", requestId)
            return ServerResponse(false, 403, null, "FORBIDDEN", "Only Agents can inspect subordinated user entries.", requestId)
        }

        val userAccount = accountDao.findById(userId)
            ?: return ServerResponse(false, 404, null, "USER_NOT_FOUND", "User '$userId' not found.", requestId)

        // Hierarchy isolation: user must belong to this Agent
        if (userAccount.parentId != context.accountId) {
            logAccessDenied(context, "CROSS_AGENT_USER_ENTRIES_DENIED", requestId)
            return ServerResponse(false, 403, null, "RESOURCE_NOT_OWNED", "Access denied: User does not belong to your agency.", requestId)
        }

        val entries = gameDao.getEntriesForUser(userId)
        val entryDtos = entries.map { entry ->
            val game = gameDao.findGameById(entry.gameId)
            val option = gameDao.findOptionById(entry.selectedOptionId)
            entry.toDto(gameTitle = game?.title, optionName = option?.displayName)
        }

        return ServerResponse(true, 200, UserEntriesResponse(entryDtos), null, null, requestId)
    }

    private suspend fun logAccessDenied(context: SecurityContext, action: String, requestId: String) {
        auditService.logEvent(
            actorId = context.accountId,
            actorRole = context.role.name,
            action = AuditActions.GAME_ACCESS_DENIED,
            targetId = action,
            requestId = requestId,
            metadataJson = "{\"attempted\":\"$action\"}"
        )
    }

    private fun GameEntity.toDto(
        options: List<GameOptionDto> = emptyList(),
        entryCount: Int = 0,
        result: GameResultDto? = null
    ) = GameDto(
        gameId = gameId,
        gameType = gameType,
        title = title,
        description = description,
        status = status,
        startTime = startTime,
        entryDeadline = entryDeadline,
        resultTime = resultTime,
        minCoins = minCoins,
        maxCoins = maxCoins,
        rewardMultiplier = rewardMultiplier,
        options = options,
        entryCount = entryCount,
        result = result,
        createdBy = createdBy,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun GameOptionEntity.toDto() = GameOptionDto(
        optionId = optionId,
        gameId = gameId,
        optionCode = optionCode,
        displayName = displayName,
        status = status,
        metadataJson = metadataJson
    )

    private fun GameEntryEntity.toDto(gameTitle: String? = null, optionName: String? = null) = GameEntryDto(
        entryId = entryId,
        gameId = gameId,
        gameTitle = gameTitle,
        userId = userId,
        selectedOptionId = selectedOptionId,
        selectedOptionName = optionName,
        virtualCoinAmount = virtualCoinAmount,
        status = status,
        idempotencyKey = idempotencyKey,
        deductionTransactionId = deductionTransactionId,
        rewardTransactionId = rewardTransactionId,
        rewardAmount = rewardAmount,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun GameResultEntity.toDto(
        winningOptionName: String? = null,
        winnersCount: Int? = null,
        rewardsPaid: Long? = null
    ): GameResultDto {
        var winners = winnersCount ?: 0
        var rewards = rewardsPaid ?: 0L
        if (winnersCount == null || rewardsPaid == null) {
            try {
                val winnersMatch = "\"winners\":(\\d+)".toRegex().find(metadataJson)
                if (winnersMatch != null) {
                    winners = winnersMatch.groupValues[1].toInt()
                }
                val rewardsMatch = "\"totalRewards\":(\\d+)".toRegex().find(metadataJson)
                if (rewardsMatch != null) {
                    rewards = rewardsMatch.groupValues[1].toLong()
                }
            } catch (_: Exception) {}
        }
        return GameResultDto(
            resultId = resultId,
            gameId = gameId,
            winningOptionId = winningOptionId,
            winningOptionName = winningOptionName,
            resultStatus = resultStatus,
            finalizedBy = finalizedBy,
            finalizedAt = finalizedAt,
            resultVersion = resultVersion,
            metadataJson = metadataJson,
            totalWinners = winners,
            totalRewardsPaid = rewards
        )
    }

    /**
     * POST /admin/games/{gameId}/processing/retry: Admin retries failed/pending result settlement.
     */
    suspend fun retryFailedProcessing(
        context: SecurityContext,
        gameId: String,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<GameProcessingDto> {
        if (context.role != AccountRole.ADMIN) {
            logAccessDenied(context, "RETRY_PROCESSING", requestId)
            return ServerResponse(false, 403, null, "FORBIDDEN", "Only Administrators can retry processing.", requestId)
        }

        val rateLimitKey = "retry_processing_${context.accountId}_$gameId"
        if (rateLimiter.isActionRateLimited(rateLimitKey, maxRequests = 5, windowMs = 60_000L)) {
            return ServerResponse(false, 429, null, "RATE_LIMIT_EXCEEDED", "Too many retry attempts. Please wait.", requestId)
        }

        val game = gameDao.findGameById(gameId)
            ?: return ServerResponse(false, 404, null, "GAME_NOT_FOUND", "Game '$gameId' not found.", requestId)

        val result = gameDao.findResultByGameId(gameId)
            ?: return ServerResponse(false, 400, null, "RESULT_NOT_FINALIZED", "Cannot retry processing on a game without a finalized result.", requestId)

        val winningOption = gameDao.findOptionById(result.winningOptionId)
            ?: return ServerResponse(false, 400, null, "OPTION_NOT_FOUND", "Winning option not found.", requestId)

        val currentProcessing = gameDao.findProcessingByGameId(gameId)
        val now = System.currentTimeMillis()

        try {
            val updatedProcessing = database.withTransaction {
                val confirmedEntries = gameDao.getEntriesForGameWithStatus(gameId, EntryStatus.CONFIRMED)
                var newlyProcessed = 0

                for (entry in confirmedEntries) {
                    if (entry.selectedOptionId == result.winningOptionId) {
                        val rewardCoins = (entry.virtualCoinAmount * game.rewardMultiplier).toLong()
                        val userWallet = walletTransactionService.getOrCreateWallet(entry.userId, AccountRole.USER.name)
                        val rewardIdempotencyKey = "reward_${entry.entryId}"
                        val existingRewardTx = transactionDao.findByIdempotencyKey(rewardIdempotencyKey)
                        val rewardTxId = if (existingRewardTx == null) {
                            val addSuccess = walletDao.addBalanceWithVersion(
                                walletId = userWallet.walletId,
                                amount = rewardCoins,
                                expectedVersion = userWallet.version,
                                newVersion = userWallet.version + 1,
                                updatedAt = now
                            )
                            if (addSuccess == 0) throw ConcurrencyConflictException("Concurrent wallet update rewarding user ${entry.userId}.")

                            val rewardTx = WalletTransactionEntity(
                                transactionId = UUID.randomUUID().toString(),
                                idempotencyKey = rewardIdempotencyKey,
                                timestamp = now,
                                actorId = context.accountId,
                                actorRole = context.role.name,
                                sourceWalletId = null,
                                destinationWalletId = userWallet.walletId,
                                amount = rewardCoins,
                                balanceBeforeSource = null,
                                balanceAfterSource = null,
                                balanceBeforeDestination = userWallet.balance,
                                balanceAfterDestination = userWallet.balance + rewardCoins,
                                transactionType = WalletTransactionService.TYPE_GAME_REWARD,
                                reason = "Reward for game '${game.title}' (Option '${winningOption.displayName}') - Retry Recovery",
                                referenceId = entry.entryId,
                                status = WalletTransactionService.STATUS_COMPLETED,
                                metadataJson = "{\"gameId\":\"$gameId\",\"entryId\":\"${entry.entryId}\",\"retry\":true}",
                                createdAt = now
                            )
                            transactionDao.insert(rewardTx)
                            rewardTx.transactionId
                        } else {
                            existingRewardTx.transactionId
                        }

                        gameDao.updateEntry(
                            entry.copy(
                                status = EntryStatus.WON,
                                rewardAmount = rewardCoins,
                                rewardTransactionId = rewardTxId,
                                updatedAt = now
                            )
                        )
                    } else {
                        gameDao.updateEntry(
                            entry.copy(
                                status = EntryStatus.LOST,
                                updatedAt = now
                            )
                        )
                    }
                    newlyProcessed++
                }

                val newAttemptCount = (currentProcessing?.attemptCount ?: 1) + 1
                val totalEntriesCount = gameDao.countEntriesForGame(gameId)
                val wonCount = gameDao.countEntriesByStatus(gameId, EntryStatus.WON)
                val lostCount = gameDao.countEntriesByStatus(gameId, EntryStatus.LOST)
                val totalProcessed = wonCount + lostCount

                val proc = GameProcessingEntity(
                    processingId = currentProcessing?.processingId ?: UUID.randomUUID().toString(),
                    gameId = gameId,
                    status = "COMPLETED",
                    startedAt = currentProcessing?.startedAt ?: now,
                    completedAt = now,
                    attemptCount = newAttemptCount,
                    lastErrorCode = null,
                    lastErrorMessage = null,
                    correlationId = requestId,
                    totalEntries = totalEntriesCount,
                    processedEntries = totalProcessed,
                    failedEntries = 0,
                    updatedAt = now
                )
                gameDao.insertOrUpdateProcessing(proc)

                gameDao.insertEvent(
                    GameEventEntity(
                        eventId = UUID.randomUUID().toString(),
                        gameId = gameId,
                        eventType = "PROCESSING_RETRY_COMPLETED",
                        actorId = context.accountId,
                        actorRole = context.role.name,
                        timestamp = now,
                        metadataJson = "{\"recoveredEntries\":$newlyProcessed,\"attempt\":$newAttemptCount}"
                    )
                )

                proc
            }

            auditService.logEvent(
                actorId = context.accountId,
                actorRole = context.role.name,
                action = AuditActions.RESULT_PROCESSING_RETRY,
                targetId = gameId,
                targetType = "GAME",
                requestId = requestId,
                metadataJson = "{\"status\":\"COMPLETED\"}"
            )

            return ServerResponse(true, 200, updatedProcessing.toDto(), null, "Retry processing completed successfully.", requestId)
        } catch (e: Exception) {
            val proc = (currentProcessing ?: GameProcessingEntity(
                processingId = UUID.randomUUID().toString(),
                gameId = gameId,
                status = "FAILED",
                startedAt = now,
                attemptCount = 1,
                updatedAt = now
            )).copy(
                status = "FAILED",
                lastErrorCode = "RETRY_FAILED",
                lastErrorMessage = e.localizedMessage,
                updatedAt = now
            )
            gameDao.insertOrUpdateProcessing(proc)

            auditService.logEvent(
                actorId = context.accountId,
                actorRole = context.role.name,
                action = AuditActions.RESULT_PROCESSING_FAILED,
                targetId = gameId,
                targetType = "GAME",
                requestId = requestId,
                metadataJson = "{\"error\":\"${e.localizedMessage}\"}"
            )

            return ServerResponse(false, 500, null, "RETRY_FAILED", "Failed to retry processing: ${e.localizedMessage}", requestId)
        }
    }

    /**
     * GET /admin/games/{gameId}/reconciliation: Verifies reward credits against ledger transactions.
     */
    suspend fun reconcileGameRewards(
        context: SecurityContext,
        gameId: String,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<RewardReconciliationResultDto> {
        if (context.role != AccountRole.ADMIN) {
            logAccessDenied(context, "RECONCILE_REWARDS", requestId)
            return ServerResponse(false, 403, null, "FORBIDDEN", "Only Administrators can reconcile rewards.", requestId)
        }

        val game = gameDao.findGameById(gameId)
            ?: return ServerResponse(false, 404, null, "GAME_NOT_FOUND", "Game '$gameId' not found.", requestId)

        val result = gameDao.findResultByGameId(gameId)
        val winningOption = if (result != null) gameDao.findOptionById(result.winningOptionId) else null
        val entries = gameDao.getEntriesForGame(gameId)
        val now = System.currentTimeMillis()

        val discrepancies = mutableListOf<ReconciliationDiscrepancyDto>()
        var expectedWinningEntriesCount = 0
        var matchedRewardTxs = 0
        var totalExpectedRewardCoins = 0L
        var totalActualRewardCoins = 0L

        if (game.status == GameStatus.RESULT_FINALIZED && result != null) {
            for (entry in entries) {
                val shouldHaveWon = entry.selectedOptionId == result.winningOptionId
                val expectedCoins = if (shouldHaveWon) (entry.virtualCoinAmount * game.rewardMultiplier).toLong() else 0L

                if (shouldHaveWon) {
                    expectedWinningEntriesCount++
                    totalExpectedRewardCoins += expectedCoins

                    if (entry.status != EntryStatus.WON) {
                        discrepancies.add(
                            ReconciliationDiscrepancyDto(
                                entryId = entry.entryId,
                                userId = entry.userId,
                                discrepancyType = "STATUS_INCONSISTENCY",
                                expectedAmount = expectedCoins,
                                actualAmount = entry.rewardAmount ?: 0L,
                                entryStatus = entry.status,
                                transactionId = entry.rewardTransactionId,
                                details = "Entry selected winning option but has status '${entry.status}' instead of 'WON'."
                            )
                        )
                    } else {
                        val txId = entry.rewardTransactionId
                        val tx = if (txId != null) transactionDao.findById(txId) else transactionDao.findByReferenceId(entry.entryId)
                        if (tx == null) {
                            discrepancies.add(
                                ReconciliationDiscrepancyDto(
                                    entryId = entry.entryId,
                                    userId = entry.userId,
                                    discrepancyType = "MISSING_REWARD_TRANSACTION",
                                    expectedAmount = expectedCoins,
                                    actualAmount = 0L,
                                    entryStatus = entry.status,
                                    transactionId = null,
                                    details = "Entry is WON but no corresponding ledger reward transaction was found."
                                )
                            )
                        } else {
                            matchedRewardTxs++
                            totalActualRewardCoins += tx.amount
                            if (tx.amount != expectedCoins) {
                                discrepancies.add(
                                    ReconciliationDiscrepancyDto(
                                        entryId = entry.entryId,
                                        userId = entry.userId,
                                        discrepancyType = "AMOUNT_MISMATCH",
                                        expectedAmount = expectedCoins,
                                        actualAmount = tx.amount,
                                        entryStatus = entry.status,
                                        transactionId = tx.transactionId,
                                        details = "Ledger transaction amount (${tx.amount}) does not match expected reward ($expectedCoins)."
                                    )
                                )
                            }
                        }
                    }
                } else {
                    if (entry.status == EntryStatus.WON) {
                        discrepancies.add(
                            ReconciliationDiscrepancyDto(
                                entryId = entry.entryId,
                                userId = entry.userId,
                                discrepancyType = "UNEXPECTED_REWARD",
                                expectedAmount = 0L,
                                actualAmount = entry.rewardAmount ?: 0L,
                                entryStatus = entry.status,
                                transactionId = entry.rewardTransactionId,
                                details = "Entry did not select winning option but has status 'WON'."
                            )
                        )
                    }
                }
            }
        } else if (game.status == GameStatus.CANCELLED) {
            for (entry in entries) {
                if (entry.status != EntryStatus.REFUNDED && entry.status != EntryStatus.CANCELLED) {
                    discrepancies.add(
                        ReconciliationDiscrepancyDto(
                            entryId = entry.entryId,
                            userId = entry.userId,
                            discrepancyType = "STATUS_INCONSISTENCY",
                            expectedAmount = entry.virtualCoinAmount,
                            actualAmount = 0L,
                            entryStatus = entry.status,
                            details = "Cancelled game has entry with unrefunded status '${entry.status}'."
                        )
                    )
                }
            }
        }

        val reconStatus = when {
            game.status != GameStatus.RESULT_FINALIZED && game.status != GameStatus.CANCELLED -> "PENDING_FINALIZATION"
            discrepancies.isEmpty() -> "CONSISTENT"
            else -> "DISCREPANCY_DETECTED"
        }

        auditService.logEvent(
            actorId = context.accountId,
            actorRole = context.role.name,
            action = AuditActions.RECONCILIATION_PERFORMED,
            targetId = gameId,
            targetType = "GAME",
            requestId = requestId,
            metadataJson = "{\"status\":\"$reconStatus\",\"discrepancies\":${discrepancies.size}}"
        )

        val resultDto = RewardReconciliationResultDto(
            gameId = gameId,
            gameTitle = game.title,
            gameStatus = game.status,
            winningOptionId = winningOption?.optionId,
            winningOptionName = winningOption?.displayName,
            reconciliationStatus = reconStatus,
            totalEligibleWinningEntries = expectedWinningEntriesCount,
            totalMatchedRewardTransactions = matchedRewardTxs,
            totalExpectedRewardCoins = totalExpectedRewardCoins,
            totalActualRewardedCoins = totalActualRewardCoins,
            discrepancies = discrepancies,
            checkedAt = now
        )

        return ServerResponse(true, 200, resultDto, null, null, requestId)
    }

    /**
     * POST /admin/games/{gameId}/reconciliation/correct: Administrator applies corrections to discrepancies.
     */
    suspend fun correctReconciliationDiscrepancies(
        context: SecurityContext,
        gameId: String,
        request: ReconciliationCorrectionRequest,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<RewardReconciliationResultDto> {
        if (context.role != AccountRole.ADMIN) {
            logAccessDenied(context, "CORRECT_DISCREPANCIES", requestId)
            return ServerResponse(false, 403, null, "FORBIDDEN", "Only Administrators can apply reconciliation corrections.", requestId)
        }

        val rateLimitKey = "correct_reconciliation_${context.accountId}_$gameId"
        if (rateLimiter.isActionRateLimited(rateLimitKey, maxRequests = 5, windowMs = 60_000L)) {
            return ServerResponse(false, 429, null, "RATE_LIMIT_EXCEEDED", "Too many correction attempts. Please wait.", requestId)
        }

        val game = gameDao.findGameById(gameId)
            ?: return ServerResponse(false, 404, null, "GAME_NOT_FOUND", "Game '$gameId' not found.", requestId)

        val result = gameDao.findResultByGameId(gameId)
            ?: return ServerResponse(false, 400, null, "RESULT_NOT_FINALIZED", "Game result not finalized.", requestId)

        val winningOption = gameDao.findOptionById(result.winningOptionId)
            ?: return ServerResponse(false, 400, null, "OPTION_NOT_FOUND", "Winning option not found.", requestId)

        val now = System.currentTimeMillis()
        var correctedCount = 0
        var totalCoinsAdjusted = 0L

        try {
            database.withTransaction {
                val targetEntries = if (request.entryId != null) {
                    listOfNotNull(gameDao.findEntryById(request.entryId))
                } else {
                    gameDao.getEntriesForGameWithStatus(gameId, EntryStatus.CONFIRMED)
                }

                for (entry in targetEntries) {
                    if (entry.selectedOptionId == result.winningOptionId) {
                        val rewardCoins = (entry.virtualCoinAmount * game.rewardMultiplier).toLong()
                        val userWallet = walletTransactionService.getOrCreateWallet(entry.userId, AccountRole.USER.name)

                        val rewardIdempotencyKey = "reward_correction_${entry.entryId}"
                        val existingRewardTx = transactionDao.findByIdempotencyKey(rewardIdempotencyKey)
                        val rewardTxId = if (existingRewardTx == null) {
                            val addSuccess = walletDao.addBalanceWithVersion(
                                walletId = userWallet.walletId,
                                amount = rewardCoins,
                                expectedVersion = userWallet.version,
                                newVersion = userWallet.version + 1,
                                updatedAt = now
                            )
                            if (addSuccess == 0) throw ConcurrencyConflictException("Concurrent balance update during correction.")

                            val rewardTx = WalletTransactionEntity(
                                transactionId = UUID.randomUUID().toString(),
                                idempotencyKey = rewardIdempotencyKey,
                                timestamp = now,
                                actorId = context.accountId,
                                actorRole = context.role.name,
                                sourceWalletId = null,
                                destinationWalletId = userWallet.walletId,
                                amount = rewardCoins,
                                balanceBeforeSource = null,
                                balanceAfterSource = null,
                                balanceBeforeDestination = userWallet.balance,
                                balanceAfterDestination = userWallet.balance + rewardCoins,
                                transactionType = WalletTransactionService.TYPE_SYSTEM_ADJUSTMENT,
                                reason = "Reconciliation correction for game '${game.title}': ${request.reason}",
                                referenceId = entry.entryId,
                                status = WalletTransactionService.STATUS_COMPLETED,
                                metadataJson = "{\"correction\":true,\"gameId\":\"$gameId\",\"entryId\":\"${entry.entryId}\"}",
                                createdAt = now
                            )
                            transactionDao.insert(rewardTx)
                            totalCoinsAdjusted += rewardCoins
                            rewardTx.transactionId
                        } else {
                            existingRewardTx.transactionId
                        }

                        gameDao.updateEntry(
                            entry.copy(
                                status = EntryStatus.WON,
                                rewardAmount = rewardCoins,
                                rewardTransactionId = rewardTxId,
                                updatedAt = now
                            )
                        )
                        correctedCount++
                    } else if (entry.status == EntryStatus.CONFIRMED) {
                        gameDao.updateEntry(entry.copy(status = EntryStatus.LOST, updatedAt = now))
                        correctedCount++
                    }
                }

                gameDao.insertEvent(
                    GameEventEntity(
                        eventId = UUID.randomUUID().toString(),
                        gameId = gameId,
                        eventType = "RECONCILIATION_CORRECTED",
                        actorId = context.accountId,
                        actorRole = context.role.name,
                        timestamp = now,
                        metadataJson = "{\"corrected\":$correctedCount,\"coinsAdjusted\":$totalCoinsAdjusted,\"reason\":\"${request.reason}\"}"
                    )
                )
            }

            val eventId = UUID.randomUUID().toString()
            auditService.logEvent(
                actorId = context.accountId,
                actorRole = context.role.name,
                action = AuditActions.RECONCILIATION_CORRECTION,
                targetId = gameId,
                targetType = "GAME",
                requestId = requestId,
                metadataJson = "{\"corrected\":$correctedCount,\"totalCoins\":$totalCoinsAdjusted}"
            )

            return reconcileGameRewards(context, gameId, requestId)
        } catch (e: Exception) {
            return ServerResponse(false, 500, null, "CORRECTION_FAILED", "Failed to apply correction: ${e.localizedMessage}", requestId)
        }
    }

    /**
     * GET /admin/games/operations-summary: Authoritative statistics across all games.
     */
    suspend fun getAdminOperationsSummary(
        context: SecurityContext,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<AdminGameOperationsSummaryDto> {
        if (context.role != AccountRole.ADMIN) {
            logAccessDenied(context, "GET_OPERATIONS_SUMMARY", requestId)
            return ServerResponse(false, 403, null, "FORBIDDEN", "Only Administrators can view operations summary.", requestId)
        }

        val totalGames = gameDao.countAllGames()
        val scheduled = gameDao.countGamesByStatus(GameStatus.SCHEDULED)
        val open = gameDao.countGamesByStatus(GameStatus.OPEN)
        val closed = gameDao.countGamesByStatus(GameStatus.CLOSED)
        val resultPending = gameDao.countGamesByStatus(GameStatus.RESULT_PENDING)
        val finalized = gameDao.countGamesByStatus(GameStatus.RESULT_FINALIZED)
        val cancelled = gameDao.countGamesByStatus(GameStatus.CANCELLED)
        val failedProc = gameDao.countFailedProcessings()

        val totalCirculating = gameDao.sumAllCoinsEntered() ?: 0L
        val totalWon = gameDao.sumAllCoinsRewarded() ?: 0L
        val activeParticipants = gameDao.countUniqueParticipants()

        val recentEvents = gameDao.getRecentEvents(10).map { it.toDto() }

        val summary = AdminGameOperationsSummaryDto(
            totalGames = totalGames,
            scheduledGames = scheduled,
            openGames = open,
            closedGames = closed,
            resultPendingGames = resultPending,
            finalizedGames = finalized,
            cancelledGames = cancelled,
            processingFailures = failedProc,
            totalVirtualCoinsCirculatingInGames = totalCirculating,
            totalVirtualCoinsWon = totalWon,
            activeParticipantsCount = activeParticipants,
            recentEvents = recentEvents
        )

        return ServerResponse(true, 200, summary, null, null, requestId)
    }

    /**
     * GET /admin/games: Paginated, filtered, sorted game listing for administrator.
     */
    suspend fun getAdminGamesPaginated(
        context: SecurityContext,
        page: Int = 1,
        limit: Int = 20,
        status: String? = null,
        gameType: String? = null,
        search: String? = null,
        sortBy: String = "newest",
        fromDate: Long? = null,
        toDate: Long? = null,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<PaginatedAdminGameListResponse> {
        if (context.role != AccountRole.ADMIN) {
            logAccessDenied(context, "GET_ADMIN_GAMES_PAGINATED", requestId)
            return ServerResponse(false, 403, null, "FORBIDDEN", "Only Administrators can access this endpoint.", requestId)
        }

        val safePage = if (page < 1) 1 else page
        val safeLimit = limit.coerceIn(1, 100)
        val offset = (safePage - 1) * safeLimit

        val now = System.currentTimeMillis()

        // Maintain schedules server-side
        val allActive = gameDao.getGamesByStatus(listOf(GameStatus.SCHEDULED, GameStatus.OPEN))
        for (game in allActive) {
            if (game.status == GameStatus.OPEN && now >= game.entryDeadline) {
                gameDao.updateGameStatusWithVersion(
                    gameId = game.gameId,
                    newStatus = GameStatus.CLOSED,
                    expectedVersion = game.version,
                    newVersion = game.version + 1,
                    updatedAt = now
                )
            } else if (game.status == GameStatus.SCHEDULED && now >= game.startTime && now < game.entryDeadline) {
                gameDao.updateGameStatusWithVersion(
                    gameId = game.gameId,
                    newStatus = GameStatus.OPEN,
                    expectedVersion = game.version,
                    newVersion = game.version + 1,
                    updatedAt = now
                )
            }
        }

        val (entities, totalCount) = if (!search.isNullOrBlank()) {
            val list = gameDao.searchGames(search.trim(), safeLimit, offset)
            val count = gameDao.countSearchGames(search.trim())
            Pair(list, count)
        } else {
            val list = gameDao.getGamesFiltered(
                status = status?.ifBlank { null },
                gameType = gameType?.ifBlank { null },
                fromDate = fromDate,
                toDate = toDate,
                limit = safeLimit,
                offset = offset
            )
            val count = gameDao.countGamesFiltered(
                status = status?.ifBlank { null },
                gameType = gameType?.ifBlank { null },
                fromDate = fromDate,
                toDate = toDate
            )
            Pair(list, count)
        }

        val totalPages = if (totalCount == 0) 1 else ((totalCount + safeLimit - 1) / safeLimit)

        val gameDtos = entities.map { g ->
            val options = gameDao.getOptionsForGame(g.gameId).map { it.toDto() }
            val entryCount = gameDao.countEntriesForGame(g.gameId)
            val result = gameDao.findResultByGameId(g.gameId)?.toDto()
            g.toDto(options, entryCount, result)
        }

        val sortedDtos = when (sortBy.lowercase()) {
            "oldest" -> gameDtos.sortedBy { it.createdAt }
            "deadline" -> gameDtos.sortedBy { it.entryDeadline }
            "entries" -> gameDtos.sortedByDescending { it.entryCount }
            "title" -> gameDtos.sortedBy { it.title.lowercase() }
            else -> gameDtos.sortedByDescending { it.createdAt } // "newest" default
        }

        val response = PaginatedAdminGameListResponse(
            games = sortedDtos,
            totalCount = totalCount,
            page = safePage,
            pageSize = safeLimit,
            totalPages = totalPages
        )

        return ServerResponse(true, 200, response, null, null, requestId)
    }

    /**
     * GET /admin/games/{gameId}: Full administration inspection of a game.
     */
    suspend fun getAdminGameDetails(
        context: SecurityContext,
        gameId: String,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<AdminGameDetailsDto> {
        if (context.role != AccountRole.ADMIN) {
            logAccessDenied(context, "GET_ADMIN_GAME_DETAILS", requestId)
            return ServerResponse(false, 403, null, "FORBIDDEN", "Only Administrators can access game details.", requestId)
        }

        val game = gameDao.findGameById(gameId)
            ?: return ServerResponse(false, 404, null, "GAME_NOT_FOUND", "Game '$gameId' not found.", requestId)

        val options = gameDao.getOptionsForGame(gameId).map { it.toDto() }
        val entryCount = gameDao.countEntriesForGame(gameId)
        val resultEntity = gameDao.findResultByGameId(gameId)
        val winningOption = resultEntity?.let { gameDao.findOptionById(it.winningOptionId) }
        val resultDto = resultEntity?.toDto(winningOptionName = winningOption?.displayName)

        val gameDto = game.toDto(options, entryCount, resultDto)
        val processing = gameDao.findProcessingByGameId(gameId)?.toDto()

        val totalCoinsEntered = gameDao.sumCoinsEnteredForGame(gameId) ?: 0L
        val confirmedCount = gameDao.countEntriesByStatus(gameId, EntryStatus.CONFIRMED)
        val wonCount = gameDao.countEntriesByStatus(gameId, EntryStatus.WON)
        val lostCount = gameDao.countEntriesByStatus(gameId, EntryStatus.LOST)
        val refundedCount = gameDao.countEntriesByStatus(gameId, EntryStatus.REFUNDED)
        val totalRewards = gameDao.sumCoinsWonForGame(gameId) ?: 0L

        val optionBreakdowns = options.map { opt ->
            val optEntries = gameDao.countEntriesForGameAndOption(gameId, opt.optionId)
            val optCoins = gameDao.sumCoinsForGameAndOption(gameId, opt.optionId) ?: 0L
            val pct = if (totalCoinsEntered > 0) (optCoins.toDouble() / totalCoinsEntered * 100.0) else 0.0
            OptionStatisticsDto(
                optionId = opt.optionId,
                optionCode = opt.optionCode,
                displayName = opt.displayName,
                entriesCount = optEntries,
                totalCoins = optCoins,
                percentageOfTotalCoins = pct
            )
        }

        val statistics = GameEntryStatisticsDto(
            totalEntries = entryCount,
            totalCoinsEntered = totalCoinsEntered,
            confirmedEntries = confirmedCount,
            winningEntries = wonCount,
            losingEntries = lostCount,
            refundedEntries = refundedCount,
            totalRewardsDistributed = totalRewards,
            optionsBreakdown = optionBreakdowns
        )

        val events = gameDao.getEventsForGame(gameId).map { it.toDto() }

        val detailsDto = AdminGameDetailsDto(
            game = gameDto,
            processing = processing,
            statistics = statistics,
            auditEvents = events
        )

        return ServerResponse(true, 200, detailsDto, null, null, requestId)
    }

    /**
     * GET /admin/games/{gameId}/entries: Paginated and filtered game entries.
     */
    suspend fun getAdminGameEntriesPaginated(
        context: SecurityContext,
        gameId: String,
        page: Int = 1,
        limit: Int = 20,
        status: String? = null,
        optionId: String? = null,
        search: String? = null,
        sortBy: String = "newest",
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<PaginatedGameEntriesResponse> {
        if (context.role != AccountRole.ADMIN) {
            logAccessDenied(context, "GET_ADMIN_GAME_ENTRIES", requestId)
            return ServerResponse(false, 403, null, "FORBIDDEN", "Only Administrators can view all game entries.", requestId)
        }

        val game = gameDao.findGameById(gameId)
            ?: return ServerResponse(false, 404, null, "GAME_NOT_FOUND", "Game '$gameId' not found.", requestId)

        val safePage = if (page < 1) 1 else page
        val safeLimit = limit.coerceIn(1, 100)
        val offset = (safePage - 1) * safeLimit

        val (entities, totalCount) = if (!search.isNullOrBlank()) {
            val list = gameDao.searchEntriesForGame(gameId, search.trim(), safeLimit, offset)
            val count = gameDao.countSearchEntriesForGame(gameId, search.trim())
            Pair(list, count)
        } else {
            val list = gameDao.getEntriesForGameFiltered(
                gameId = gameId,
                status = status?.ifBlank { null },
                optionId = optionId?.ifBlank { null },
                limit = safeLimit,
                offset = offset
            )
            val count = gameDao.countEntriesForGameFiltered(
                gameId = gameId,
                status = status?.ifBlank { null },
                optionId = optionId?.ifBlank { null }
            )
            Pair(list, count)
        }

        val totalPages = if (totalCount == 0) 1 else ((totalCount + safeLimit - 1) / safeLimit)

        val dtos = entities.map { entry ->
            val opt = gameDao.findOptionById(entry.selectedOptionId)
            entry.toDto(gameTitle = game.title, optionName = opt?.displayName)
        }

        val sortedDtos = when (sortBy.lowercase()) {
            "oldest" -> dtos.sortedBy { it.createdAt }
            "amount_desc" -> dtos.sortedByDescending { it.virtualCoinAmount }
            "amount_asc" -> dtos.sortedBy { it.virtualCoinAmount }
            else -> dtos.sortedByDescending { it.createdAt }
        }

        val totalCoinsEntered = gameDao.sumCoinsEnteredForGame(gameId) ?: 0L
        val confirmedCount = gameDao.countEntriesByStatus(gameId, EntryStatus.CONFIRMED)
        val wonCount = gameDao.countEntriesByStatus(gameId, EntryStatus.WON)
        val lostCount = gameDao.countEntriesByStatus(gameId, EntryStatus.LOST)
        val refundedCount = gameDao.countEntriesByStatus(gameId, EntryStatus.REFUNDED)
        val totalRewards = gameDao.sumCoinsWonForGame(gameId) ?: 0L

        val statistics = GameEntryStatisticsDto(
            totalEntries = gameDao.countEntriesForGame(gameId),
            totalCoinsEntered = totalCoinsEntered,
            confirmedEntries = confirmedCount,
            winningEntries = wonCount,
            losingEntries = lostCount,
            refundedEntries = refundedCount,
            totalRewardsDistributed = totalRewards,
            optionsBreakdown = emptyList()
        )

        val response = PaginatedGameEntriesResponse(
            entries = sortedDtos,
            statistics = statistics,
            totalCount = totalCount,
            page = safePage,
            pageSize = safeLimit,
            totalPages = totalPages
        )

        return ServerResponse(true, 200, response, null, null, requestId)
    }

    /**
     * GET /admin/transactions/reference/{referenceId}: Inspect ledger transaction with linked game & entry.
     */
    suspend fun getTransactionByReference(
        context: SecurityContext,
        referenceId: String,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<TransactionReferenceDto> {
        if (context.role != AccountRole.ADMIN) {
            logAccessDenied(context, "GET_TRANSACTION_BY_REF", requestId)
            return ServerResponse(false, 403, null, "FORBIDDEN", "Only Administrators can inspect transactions.", requestId)
        }

        val trimmedRef = referenceId.trim()
        val tx = transactionDao.findById(trimmedRef)
            ?: transactionDao.findByReferenceId(trimmedRef)
            ?: return ServerResponse(false, 404, null, "TRANSACTION_NOT_FOUND", "No transaction found for reference '$trimmedRef'.", requestId)

        val entry = tx.referenceId?.let { gameDao.findEntryById(it) }
        val game = entry?.let { gameDao.findGameById(it.gameId) }

        val sourceWallet = if (tx.sourceWalletId != null) walletDao.findByWalletId(tx.sourceWalletId) else null
        val sourceName = if (sourceWallet != null) {
            val account = accountDao.findById(sourceWallet.ownerId)
            account?.fullName ?: sourceWallet.ownerId
        } else null

        val destWallet = if (tx.destinationWalletId != null) walletDao.findByWalletId(tx.destinationWalletId) else null
        val destName = if (destWallet != null) {
            val account = accountDao.findById(destWallet.ownerId)
            account?.fullName ?: destWallet.ownerId
        } else null

        val option = entry?.let { gameDao.findOptionById(it.selectedOptionId) }
        val entryDto = entry?.toDto(gameTitle = game?.title, optionName = option?.displayName)
        val gameDto = game?.let { g ->
            val opts = gameDao.getOptionsForGame(g.gameId).map { it.toDto() }
            val res = gameDao.findResultByGameId(g.gameId)?.toDto()
            g.toDto(opts, 0, res)
        }

        auditService.logEvent(
            actorId = context.accountId,
            actorRole = context.role.name,
            action = AuditActions.TRANSACTION_INSPECTED,
            targetId = tx.transactionId,
            targetType = "WALLET_TRANSACTION",
            requestId = requestId,
            metadataJson = "{\"referenceId\":\"$trimmedRef\",\"type\":\"${tx.transactionType}\"}"
        )

        val dto = TransactionReferenceDto(
            transaction = tx.toDto(),
            relatedGame = gameDto,
            relatedEntry = entryDto,
            sourceAccountName = sourceName,
            destinationAccountName = destName
        )

        return ServerResponse(true, 200, dto, null, null, requestId)
    }

    /**
     * GET /admin/audit-events: Paginated audit event log query.
     */
    suspend fun getAuditLogsAdmin(
        context: SecurityContext,
        page: Int = 1,
        limit: Int = 25,
        actorId: String? = null,
        action: String? = null,
        targetId: String? = null,
        targetType: String? = null,
        startDate: Long? = null,
        endDate: Long? = null,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<PaginatedAuditEventsResponse> {
        if (context.role != AccountRole.ADMIN) {
            logAccessDenied(context, "GET_AUDIT_LOGS_ADMIN", requestId)
            return ServerResponse(false, 403, null, "FORBIDDEN", "Only Administrators can view system audit logs.", requestId)
        }

        val safePage = if (page < 1) 1 else page
        val safeLimit = limit.coerceIn(1, 100)
        val offset = (safePage - 1) * safeLimit

        val (entities, totalCount) = auditService.queryAuditLogs(
            actorId = actorId,
            action = action,
            targetId = targetId,
            targetType = targetType,
            startTime = startDate ?: 0L,
            endTime = endDate ?: Long.MAX_VALUE,
            limit = safeLimit,
            offset = offset
        )

        val totalPages = if (totalCount == 0) 1 else ((totalCount + safeLimit - 1) / safeLimit)
        val dtos = entities.map { it.toDto() }

        val response = PaginatedAuditEventsResponse(
            events = dtos,
            totalCount = totalCount,
            page = safePage,
            pageSize = safeLimit,
            totalPages = totalPages
        )

        return ServerResponse(true, 200, response, null, null, requestId)
    }

    /**
     * GET /admin/reports/games: Aggregated performance and volume reports for games.
     */
    suspend fun getAdminGamesReport(
        context: SecurityContext,
        startDate: Long? = null,
        endDate: Long? = null,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<AdminGamesReportDto> {
        if (context.role != AccountRole.ADMIN) {
            logAccessDenied(context, "GET_GAMES_REPORT", requestId)
            return ServerResponse(false, 403, null, "FORBIDDEN", "Only Administrators can generate game reports.", requestId)
        }

        val start = startDate ?: 0L
        val end = endDate ?: Long.MAX_VALUE
        val games = gameDao.getGamesByDateRange(start, end)

        var totalVolume = 0L
        var totalRewards = 0L
        val statusMap = mutableMapOf<String, Int>()
        val rows = mutableListOf<GameReportRowDto>()

        for (game in games) {
            statusMap[game.status] = (statusMap[game.status] ?: 0) + 1
            val entriesCount = gameDao.countEntriesForGame(game.gameId)
            val coinsEntered = gameDao.sumCoinsEnteredForGame(game.gameId) ?: 0L
            val coinsWon = gameDao.sumCoinsWonForGame(game.gameId) ?: 0L
            val result = gameDao.findResultByGameId(game.gameId)
            val winningOpt = result?.let { gameDao.findOptionById(it.winningOptionId)?.displayName }

            totalVolume += coinsEntered
            totalRewards += coinsWon

            rows.add(
                GameReportRowDto(
                    gameId = game.gameId,
                    title = game.title,
                    status = game.status,
                    entryCount = entriesCount,
                    totalCoinsEntered = coinsEntered,
                    totalRewardsPaid = coinsWon,
                    netCoinDelta = coinsEntered - coinsWon,
                    winningOption = winningOpt,
                    createdAt = game.createdAt
                )
            )
        }

        auditService.logEvent(
            actorId = context.accountId,
            actorRole = context.role.name,
            action = AuditActions.REPORT_GENERATED,
            targetId = "GAMES_REPORT",
            targetType = "REPORT",
            requestId = requestId,
            metadataJson = "{\"gamesCount\":${games.size},\"totalVolume\":$totalVolume}"
        )

        val reportDto = AdminGamesReportDto(
            totalGamesAnalyzed = games.size,
            totalVolumeEntered = totalVolume,
            totalRewardsPaid = totalRewards,
            netPlatformVolume = totalVolume - totalRewards,
            gamesByStatus = statusMap,
            gameRows = rows
        )

        return ServerResponse(true, 200, reportDto, null, null, requestId)
    }

    /**
     * GET /admin/reports/entries: Aggregated entry status, counts, and volumes.
     */
    suspend fun getAdminEntriesReport(
        context: SecurityContext,
        startDate: Long? = null,
        endDate: Long? = null,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<AdminEntriesReportDto> {
        if (context.role != AccountRole.ADMIN) {
            logAccessDenied(context, "GET_ENTRIES_REPORT", requestId)
            return ServerResponse(false, 403, null, "FORBIDDEN", "Only Administrators can generate entry reports.", requestId)
        }

        val start = startDate ?: 0L
        val end = endDate ?: Long.MAX_VALUE
        val entries = gameDao.getEntriesByDateRange(start, end)

        var confirmed = 0
        var won = 0
        var lost = 0
        var refunded = 0
        var coinsEntered = 0L
        var coinsWon = 0L

        for (e in entries) {
            when (e.status) {
                EntryStatus.CONFIRMED -> confirmed++
                EntryStatus.WON -> {
                    won++
                    coinsWon += (e.rewardAmount ?: 0L)
                }
                EntryStatus.LOST -> lost++
                EntryStatus.REFUNDED -> refunded++
            }
            if (e.status != EntryStatus.CANCELLED) {
                coinsEntered += e.virtualCoinAmount
            }
        }

        val avgEntry = if (entries.isNotEmpty()) coinsEntered.toDouble() / entries.size else 0.0

        auditService.logEvent(
            actorId = context.accountId,
            actorRole = context.role.name,
            action = AuditActions.REPORT_GENERATED,
            targetId = "ENTRIES_REPORT",
            targetType = "REPORT",
            requestId = requestId,
            metadataJson = "{\"entriesCount\":${entries.size},\"totalCoins\":$coinsEntered}"
        )

        val report = AdminEntriesReportDto(
            totalEntries = entries.size,
            confirmedEntries = confirmed,
            winningEntries = won,
            losingEntries = lost,
            refundedEntries = refunded,
            totalCoinsEntered = coinsEntered,
            totalCoinsWon = coinsWon,
            averageEntryCoins = avgEntry
        )

        return ServerResponse(true, 200, report, null, null, requestId)
    }

    /**
     * GET /admin/reports/transactions: Aggregated financial ledger transaction volume by type.
     */
    suspend fun getAdminTransactionsReport(
        context: SecurityContext,
        startDate: Long? = null,
        endDate: Long? = null,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<AdminTransactionsReportDto> {
        if (context.role != AccountRole.ADMIN) {
            logAccessDenied(context, "GET_TRANSACTIONS_REPORT", requestId)
            return ServerResponse(false, 403, null, "FORBIDDEN", "Only Administrators can generate transaction reports.", requestId)
        }

        val start = startDate ?: 0L
        val end = endDate ?: Long.MAX_VALUE
        val txs = transactionDao.getTransactionsByDateRange(start, end)

        var totalVolume = 0L
        val txByType = mutableMapOf<String, Int>()
        val volByType = mutableMapOf<String, Long>()

        for (tx in txs) {
            totalVolume += tx.amount
            txByType[tx.transactionType] = (txByType[tx.transactionType] ?: 0) + 1
            volByType[tx.transactionType] = (volByType[tx.transactionType] ?: 0L) + tx.amount
        }

        auditService.logEvent(
            actorId = context.accountId,
            actorRole = context.role.name,
            action = AuditActions.REPORT_GENERATED,
            targetId = "TRANSACTIONS_REPORT",
            targetType = "REPORT",
            requestId = requestId,
            metadataJson = "{\"txCount\":${txs.size},\"totalVolume\":$totalVolume}"
        )

        val report = AdminTransactionsReportDto(
            totalTransactions = txs.size,
            totalCoinVolume = totalVolume,
            transactionsByType = txByType,
            volumeByType = volByType
        )

        return ServerResponse(true, 200, report, null, null, requestId)
    }

    /**
     * GET /admin/export/{exportType}: Export operational records to CSV.
     */
    suspend fun exportDataCsv(
        context: SecurityContext,
        exportType: String,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<String> {
        if (context.role != AccountRole.ADMIN) {
            logAccessDenied(context, "EXPORT_CSV", requestId)
            return ServerResponse(false, 403, null, "FORBIDDEN", "Only Administrators can export data.", requestId)
        }

        val csv = when (exportType.lowercase()) {
            "games" -> {
                val games = gameDao.getAllGames()
                val sb = StringBuilder("GameId,Type,Title,Status,MinCoins,MaxCoins,Multiplier,CreatedAt\n")
                for (g in games) {
                    sb.append("\"${g.gameId}\",\"${g.gameType}\",\"${g.title.replace("\"", "\"\"")}\",\"${g.status}\",${g.minCoins},${g.maxCoins},${g.rewardMultiplier},${g.createdAt}\n")
                }
                sb.toString()
            }
            "entries" -> {
                val entries = gameDao.getAllEntries(limit = 1000, offset = 0)
                val sb = StringBuilder("EntryId,GameId,UserId,OptionId,VirtualCoinAmount,Status,RewardAmount,CreatedAt\n")
                for (e in entries) {
                    sb.append("\"${e.entryId}\",\"${e.gameId}\",\"${e.userId}\",\"${e.selectedOptionId}\",${e.virtualCoinAmount},\"${e.status}\",${e.rewardAmount ?: 0},${e.createdAt}\n")
                }
                sb.toString()
            }
            "audit-events", "audit" -> {
                val (logs, _) = auditService.queryAuditLogs(limit = 1000)
                val sb = StringBuilder("LogId,ActorId,ActorRole,Action,TargetId,TargetType,CreatedAt\n")
                for (l in logs) {
                    sb.append("\"${l.id}\",\"${l.actorId}\",\"${l.actorRole}\",\"${l.action}\",\"${l.targetId ?: ""}\",\"${l.targetType ?: ""}\",${l.createdAt}\n")
                }
                sb.toString()
            }
            "transactions" -> {
                val txs = transactionDao.getAllTransactions(limit = 1000, offset = 0)
                val sb = StringBuilder("TransactionId,ActorId,SourceWalletId,DestinationWalletId,Amount,Type,Status,CreatedAt\n")
                for (t in txs) {
                    sb.append("\"${t.transactionId}\",\"${t.actorId}\",\"${t.sourceWalletId ?: ""}\",\"${t.destinationWalletId ?: ""}\",${t.amount},\"${t.transactionType}\",\"${t.status}\",${t.createdAt}\n")
                }
                sb.toString()
            }
            else -> return ServerResponse(false, 400, null, "INVALID_EXPORT_TYPE", "Invalid export type '$exportType'. Valid types: games, entries, audit-events, transactions.", requestId)
        }

        auditService.logEvent(
            actorId = context.accountId,
            actorRole = context.role.name,
            action = AuditActions.DATA_EXPORTED,
            targetId = exportType.uppercase(),
            targetType = "EXPORT",
            requestId = requestId,
            metadataJson = "{\"exportType\":\"$exportType\"}"
        )

        return ServerResponse(true, 200, csv, null, "Data exported successfully.", requestId)
    }

    private fun GameEventEntity.toDto() = GameEventDto(
        eventId = eventId,
        gameId = gameId,
        eventType = eventType,
        actorId = actorId,
        actorRole = actorRole,
        timestamp = timestamp,
        metadataJson = metadataJson,
        correlationId = correlationId
    )

    private fun GameProcessingEntity.toDto() = GameProcessingDto(
        processingId = processingId,
        gameId = gameId,
        status = status,
        startedAt = startedAt,
        completedAt = completedAt,
        attemptCount = attemptCount,
        lastErrorCode = lastErrorCode,
        lastErrorMessage = lastErrorMessage,
        correlationId = correlationId,
        totalEntries = totalEntries,
        processedEntries = processedEntries,
        failedEntries = failedEntries,
        updatedAt = updatedAt
    )

    private fun AuditLogEntity.toDto() = AuditLogDto(
        id = id,
        actorId = actorId,
        actorRole = actorRole,
        action = action,
        targetId = targetId,
        targetType = targetType,
        requestId = requestId,
        metadataJson = metadataJson,
        beforeState = beforeState,
        afterState = afterState,
        createdAt = createdAt
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
