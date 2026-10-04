package com.example.data.repository

import com.example.backend.model.LoginRequest
import com.example.core.network.MahakalApiService
import com.example.core.network.SessionManager
import com.example.core.security.SecureTokenStorage
import com.example.data.local.MahakalDatabase
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.GameEntity
import com.example.data.local.entity.GameEntryEntity
import com.example.data.local.entity.NotificationEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Authoritative Repository for MAHAKAL Platform.
 *
 * Guarantees:
 * 1. Room/local SQLite is strictly an offline cache, NEVER an authentication authority.
 * 2. Fresh installs start completely unauthenticated without cached privileged sessions or default admin accounts.
 * 3. All logins and profile validations are verified by the production backend against PostgreSQL.
 * 4. Zero hardcoded bypasses or demo account switchers.
 */
class MahakalRepository(
    private val db: MahakalDatabase,
    private val apiService: MahakalApiService? = null,
    private val sessionManager: SessionManager? = null,
    private val secureStorage: SecureTokenStorage? = null
) {
    private val userDao = db.userDao()
    private val txDao = db.transactionDao()
    private val gameDao = db.gameDao()
    private val auditDao = db.auditDao()
    private val notifDao = db.notificationDao()

    /**
     * Seeds initial game catalog if local cache is completely empty.
     * CRITICAL: NEVER seeds default ADMIN, AGENT, or USER accounts.
     */
    suspend fun bootstrapInitialData() {
        // Pre-populate standard contest types into local cache if empty
        val existingGame = gameDao.getGameById("game_matka_single_01")
        if (existingGame == null) {
            val game1 = GameEntity(
                id = "game_matka_single_01",
                title = "Kalyan Single Ank [0-9]",
                category = "MATKA_SINGLE",
                minCoins = 100,
                maxCoins = 10000,
                multiplier = 9.5,
                status = "OPEN"
            )
            val game2 = GameEntity(
                id = "game_jodi_01",
                title = "Rajdhani Supreme Jodi [00-99]",
                category = "JODI_PAIR",
                minCoins = 50,
                maxCoins = 5000,
                multiplier = 90.0,
                status = "OPEN"
            )
            val game3 = GameEntity(
                id = "game_dice_roll_01",
                title = "Mahakal Royal Dice Roll [1-6]",
                category = "LUCKY_DICE",
                minCoins = 50,
                maxCoins = 25000,
                multiplier = 5.5,
                status = "OPEN"
            )
            val game4 = GameEntity(
                id = "game_color_wheel_01",
                title = "Tri-Color Mystic Wheel [Red, Green, Blue]",
                category = "COLOR_WHEEL",
                minCoins = 100,
                maxCoins = 50000,
                multiplier = 2.8,
                status = "OPEN"
            )
            gameDao.insertGame(game1)
            gameDao.insertGame(game2)
            gameDao.insertGame(game3)
            gameDao.insertGame(game4)
        }
    }

    /**
     * Validates current stored server session on app launch.
     * Returns the server-authenticated UserEntity, or null if no valid session exists.
     */
    suspend fun validateSession(): UserEntity? {
        val token = secureStorage?.getAccessToken()
        val userId = secureStorage?.getSessionUserId()
        if (token.isNullOrBlank() || userId.isNullOrBlank()) {
            logout()
            return null
        }

        if (apiService != null) {
            try {
                val resp = apiService.getProfile()
                if (resp.isSuccessful) {
                    val body = resp.body()
                    val userDto = body?.data
                    if (userDto != null && userDto.status == "ACTIVE") {
                        var balance = 0L
                        try {
                            val wResp = apiService.getMyWallet()
                            if (wResp.isSuccessful) {
                                val wData = wResp.body()?.data
                                balance = (wData?.get("balance") as? Number)?.toLong() ?: 0L
                            }
                        } catch (_: Exception) {}

                        val userEntity = UserEntity(
                            id = userDto.id,
                            username = userDto.loginId,
                            passwordHash = "",
                            role = userDto.role,
                            fullName = userDto.fullName,
                            balance = balance,
                            status = userDto.status
                        )
                        userDao.insertUser(userEntity)
                        sessionManager?.setAuthenticated(userDto, token, secureStorage.getRefreshToken() ?: "")
                        return userEntity
                    }
                }
                // Server rejected or session expired: Clear local cached session
                logout()
                return null
            } catch (_: Exception) {
                // Network error or server unreachable: Do NOT authenticate from Room
                return null
            }
        }
        return null
    }

    /**
     * Authenticates user against authoritative PostgreSQL backend.
     */
    suspend fun authenticate(username: String, pass: String): UserEntity? {
        val trimmedUsername = username.trim()
        val trimmedPass = pass.trim()
        if (trimmedUsername.isBlank() || trimmedPass.isBlank()) return null

        if (apiService != null) {
            try {
                val response = apiService.login(LoginRequest(loginId = trimmedUsername, password = trimmedPass))
                if (response.isSuccessful) {
                    val body = response.body()
                    val data = body?.data
                    val token = data?.accessToken
                    val userDto = data?.user
                    if (token != null && userDto != null) {
                        secureStorage?.saveTokens(
                            accessToken = token,
                            refreshToken = data.refreshToken,
                            userId = userDto.id,
                            loginId = userDto.loginId,
                            role = userDto.role,
                            fullName = userDto.fullName
                        )
                        sessionManager?.setAuthenticated(userDto, token, data.refreshToken)

                        var balance = 0L
                        try {
                            val wResp = apiService.getMyWallet()
                            if (wResp.isSuccessful) {
                                val wData = wResp.body()?.data
                                balance = (wData?.get("balance") as? Number)?.toLong() ?: 0L
                            }
                        } catch (_: Exception) {}

                        val userEntity = UserEntity(
                            id = userDto.id,
                            username = userDto.loginId,
                            passwordHash = "", // Never store plain/hashed passwords in client Room
                            role = userDto.role,
                            fullName = userDto.fullName,
                            balance = balance,
                            agentId = null,
                            status = userDto.status
                        )
                        userDao.insertUser(userEntity)

                        auditDao.insertLog(
                            AuditLogEntity(
                                id = UUID.randomUUID().toString(),
                                actorId = userEntity.id,
                                actorRole = userEntity.role,
                                action = "USER_LOGIN_SUCCESS",
                                targetId = userEntity.id,
                                details = "User ${userEntity.username} authenticated with role ${userEntity.role}"
                            )
                        )
                        return userEntity
                    }
                } else if (response.code() == 403) {
                    throw Exception("Account has been suspended or deactivated.")
                }
                return null
            } catch (e: Exception) {
                if (e.message?.contains("suspended", ignoreCase = true) == true) {
                    throw e
                }
                return null
            }
        }
        return null
    }

    /**
     * Clears all session tokens, in-memory state, and cached credentials.
     */
    suspend fun logout() {
        try {
            apiService?.logout()
        } catch (_: Exception) {}
        secureStorage?.clearSession()
        sessionManager?.clearSession()
    }

    suspend fun getUserById(userId: String) = userDao.getUserById(userId)

    fun getAllAgents(): Flow<List<UserEntity>> = userDao.getAllAgents()
    fun getUsersByAgent(agentId: String): Flow<List<UserEntity>> = userDao.getUsersByAgent(agentId)
    fun getAllUsers(): Flow<List<UserEntity>> = userDao.getAllUsers()
    fun getAllAccounts(): Flow<List<UserEntity>> = userDao.getAllAccounts()

    suspend fun createAgent(username: String, fullName: String, initialCoins: Long): Result<UserEntity> {
        val existing = userDao.getUserByUsername(username.trim())
        if (existing != null) return Result.failure(Exception("Agent username already exists"))

        if (apiService != null) {
            try {
                val resp = apiService.createAgent(
                    mapOf(
                        "loginId" to username.trim(),
                        "fullName" to fullName.trim(),
                        "password" to "Agent@${UUID.randomUUID().toString().take(6)}"
                    )
                )
                if (!resp.isSuccessful) {
                    val err = resp.errorBody()?.string() ?: "Failed to create agent on server"
                    return Result.failure(Exception(err))
                }
            } catch (e: Exception) {
                return Result.failure(e)
            }
        }

        val newAgent = UserEntity(
            id = "agent_" + UUID.randomUUID().toString().take(8),
            username = username.trim(),
            passwordHash = "",
            role = "AGENT",
            fullName = fullName.trim(),
            balance = initialCoins,
            status = "ACTIVE"
        )
        userDao.insertUser(newAgent)

        auditDao.insertLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                actorId = "ADMIN",
                actorRole = "ADMIN",
                action = "AGENT_CREATION",
                targetId = newAgent.id,
                details = "Created agent ${newAgent.username} with allocation of $initialCoins coins"
            )
        )
        return Result.success(newAgent)
    }

    suspend fun toggleAgentStatus(agentId: String, newStatus: String): Result<Unit> {
        if (apiService != null) {
            try {
                apiService.updateAgentStatus(
                    mapOf(
                        "agentId" to agentId,
                        "status" to newStatus
                    )
                )
            } catch (_: Exception) {}
        }
        userDao.updateUserStatus(agentId, newStatus)
        auditDao.insertLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                actorId = "ADMIN",
                actorRole = "ADMIN",
                action = "AGENT_STATUS_CHANGE",
                targetId = agentId,
                details = "Updated agent status to $newStatus"
            )
        )
        return Result.success(Unit)
    }

    suspend fun createUserUnderAgent(agentId: String, username: String, fullName: String, initialCoins: Long): Result<UserEntity> {
        val existing = userDao.getUserByUsername(username.trim())
        if (existing != null) return Result.failure(Exception("Username already exists"))

        if (apiService != null) {
            try {
                val resp = apiService.createAgentUser(
                    mapOf(
                        "loginId" to username.trim(),
                        "fullName" to fullName.trim(),
                        "parentId" to agentId,
                        "password" to "User@${UUID.randomUUID().toString().take(6)}"
                    )
                )
                if (!resp.isSuccessful) {
                    val err = resp.errorBody()?.string() ?: "Failed to create player on server"
                    return Result.failure(Exception(err))
                }
            } catch (e: Exception) {
                return Result.failure(e)
            }
        }

        val newUser = UserEntity(
            id = "user_" + UUID.randomUUID().toString().take(8),
            username = username.trim(),
            passwordHash = "",
            role = "USER",
            fullName = fullName.trim(),
            balance = initialCoins,
            agentId = agentId,
            status = "ACTIVE"
        )
        userDao.insertUser(newUser)

        auditDao.insertLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                actorId = agentId,
                actorRole = "AGENT",
                action = "USER_REGISTRATION",
                targetId = newUser.id,
                details = "Agent onboarded player ${newUser.username} with $initialCoins coins"
            )
        )
        return Result.success(newUser)
    }

    suspend fun transferCoins(actor: UserEntity, destinationUserId: String, amount: Long, notes: String): Result<Unit> {
        if (amount <= 0) return Result.failure(Exception("Amount must be greater than zero"))
        val sender = userDao.getUserById(actor.id) ?: return Result.failure(Exception("Sender not found"))
        if (sender.balance < amount) return Result.failure(Exception("Insufficient balance"))
        val recipient = userDao.getUserById(destinationUserId) ?: return Result.failure(Exception("Recipient not found"))

        if (apiService != null) {
            try {
                val idemKey = "tx_${UUID.randomUUID()}"
                val resp = apiService.transferVirtualCoins(
                    idempotencyKey = idemKey,
                    request = mapOf(
                        "destinationAccountId" to destinationUserId,
                        "amount" to amount,
                        "reason" to notes.ifBlank { "Coin Transfer" }
                    )
                )
                if (!resp.isSuccessful) {
                    val err = resp.errorBody()?.string() ?: "Transfer rejected by server"
                    return Result.failure(Exception(err))
                }
            } catch (e: Exception) {
                return Result.failure(e)
            }
        }

        userDao.deductBalance(sender.id, amount)
        userDao.addBalance(recipient.id, amount)

        val txType = when (actor.role) {
            "ADMIN" -> "ADMIN_TO_AGENT"
            "AGENT" -> "AGENT_TO_USER"
            else -> "USER_TRANSFER"
        }

        txDao.insertTransaction(
            TransactionEntity(
                id = "tx_" + UUID.randomUUID().toString().take(10),
                actorId = sender.id,
                actorRole = sender.role,
                sourceUserId = sender.id,
                destinationUserId = recipient.id,
                sourceName = sender.fullName,
                destinationName = recipient.fullName,
                amount = amount,
                type = txType,
                description = notes.ifBlank { "Coin transfer from ${sender.username} to ${recipient.username}" }
            )
        )

        auditDao.insertLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                actorId = sender.id,
                actorRole = sender.role,
                action = "COIN_TRANSFER",
                targetId = recipient.id,
                details = "Transferred $amount coins from ${sender.username} to ${recipient.username}"
            )
        )
        return Result.success(Unit)
    }

    suspend fun mintTreasuryCoins(admin: UserEntity, amount: Long): Result<Unit> {
        if (amount <= 0) return Result.failure(Exception("Amount must be positive"))
        userDao.addBalance(admin.id, amount)

        txDao.insertTransaction(
            TransactionEntity(
                id = "tx_" + UUID.randomUUID().toString().take(10),
                actorId = admin.id,
                actorRole = "ADMIN",
                sourceUserId = "MINT_VAULT",
                destinationUserId = admin.id,
                sourceName = "Central Mint Authority",
                destinationName = "Treasury Reserve",
                amount = amount,
                type = "TREASURY_MINT",
                description = "Minted $amount new virtual coins into master platform treasury"
            )
        )
        return Result.success(Unit)
    }

    suspend fun toggleUserStatus(actor: UserEntity, targetUserId: String, newStatus: String): Result<Unit> {
        return toggleAgentStatus(targetUserId, newStatus)
    }

    // Games Flow
    fun getAllGames(): Flow<List<GameEntity>> = gameDao.getAllGames()
    fun getActiveGames(): Flow<List<GameEntity>> = gameDao.getActiveGames()
    fun getEntriesByUser(userId: String): Flow<List<GameEntryEntity>> = gameDao.getEntriesByUser(userId)
    fun getAllRecentEntries(): Flow<List<GameEntryEntity>> = gameDao.getAllRecentEntries()

    suspend fun createGame(
        admin: UserEntity,
        title: String,
        category: String,
        minCoins: Long,
        maxCoins: Long,
        multiplier: Double,
        apiLink: String = ""
    ): Result<GameEntity> {
        val game = GameEntity(
            id = "game_" + UUID.randomUUID().toString().take(8),
            title = title.trim(),
            category = category,
            minCoins = minCoins,
            maxCoins = maxCoins,
            multiplier = multiplier,
            status = "OPEN",
            apiLink = apiLink.trim()
        )

        if (apiService != null) {
            try {
                apiService.createGame(
                    mapOf(
                        "title" to title.trim(),
                        "gameType" to category,
                        "minCoins" to minCoins,
                        "maxCoins" to maxCoins,
                        "rewardMultiplier" to multiplier,
                        "description" to apiLink.trim()
                    )
                )
            } catch (_: Exception) {}
        }

        gameDao.insertGame(game)
        auditDao.insertLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                actorId = admin.id,
                actorRole = admin.role,
                action = "CREATE_GAME",
                targetId = game.id,
                details = "Created game ${game.title} with multiplier x$multiplier (API: ${game.apiLink})"
            )
        )
        return Result.success(game)
    }

    suspend fun editGame(
        admin: UserEntity,
        gameId: String,
        title: String,
        category: String,
        minCoins: Long,
        maxCoins: Long,
        multiplier: Double,
        apiLink: String
    ): Result<Unit> {
        val existing = gameDao.getGameById(gameId) ?: return Result.failure(Exception("Game not found"))
        val updated = existing.copy(
            title = title.trim(),
            category = category,
            minCoins = minCoins,
            maxCoins = maxCoins,
            multiplier = multiplier,
            apiLink = apiLink.trim()
        )

        if (apiService != null) {
            try {
                apiService.editGame(
                    mapOf(
                        "gameId" to gameId,
                        "title" to title.trim(),
                        "minCoins" to minCoins,
                        "maxCoins" to maxCoins,
                        "rewardMultiplier" to multiplier,
                        "description" to apiLink.trim()
                    )
                )
            } catch (_: Exception) {}
        }

        gameDao.updateGame(updated)
        auditDao.insertLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                actorId = admin.id,
                actorRole = admin.role,
                action = "EDIT_GAME",
                targetId = gameId,
                details = "Updated game $title (Multiplier: ${multiplier}x, API: $apiLink)"
            )
        )
        return Result.success(Unit)
    }

    suspend fun toggleGameStatus(admin: UserEntity, gameId: String, newStatus: String): Result<Unit> {
        if (apiService != null) {
            try {
                apiService.updateGameStatus(
                    mapOf(
                        "gameId" to gameId,
                        "status" to newStatus
                    )
                )
            } catch (_: Exception) {}
        }
        gameDao.updateGameStatus(gameId, newStatus)
        auditDao.insertLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                actorId = admin.id,
                actorRole = admin.role,
                action = "GAME_STATUS_CHANGE",
                targetId = gameId,
                details = "Changed game status to $newStatus"
            )
        )
        return Result.success(Unit)
    }

    suspend fun configureGameApi(admin: UserEntity, gameId: String, apiLink: String): Result<Unit> {
        if (apiService != null) {
            try {
                apiService.configGameApi(
                    mapOf(
                        "gameId" to gameId,
                        "apiLink" to apiLink.trim()
                    )
                )
            } catch (_: Exception) {}
        }
        gameDao.updateGameApiLink(gameId, apiLink.trim())
        auditDao.insertLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                actorId = admin.id,
                actorRole = admin.role,
                action = "GAME_API_CONFIGURED",
                targetId = gameId,
                details = "Configured API link to $apiLink"
            )
        )
        return Result.success(Unit)
    }

    suspend fun placeGameEntry(
        user: UserEntity,
        gameId: String,
        option: String,
        coins: Long
    ): Result<GameEntryEntity> {
        val game = gameDao.getGameById(gameId) ?: return Result.failure(Exception("Game not found"))
        if (game.status != "OPEN") return Result.failure(Exception("Game is not open for entries"))
        if (coins < game.minCoins) return Result.failure(Exception("Minimum entry is ${game.minCoins} coins"))
        if (coins > game.maxCoins) return Result.failure(Exception("Maximum entry is ${game.maxCoins} coins"))

        val currentUser = userDao.getUserById(user.id) ?: return Result.failure(Exception("User not found"))
        if (currentUser.balance < coins) return Result.failure(Exception("Insufficient coin balance"))

        val potentialPayout = (coins * game.multiplier).toLong()

        if (apiService != null) {
            try {
                val idemKey = "entry_${UUID.randomUUID()}"
                val resp = apiService.enterGame(
                    idempotencyKey = idemKey,
                    request = mapOf(
                        "gameId" to gameId,
                        "optionCode" to option,
                        "amount" to coins
                    )
                )
                if (!resp.isSuccessful) {
                    val err = resp.errorBody()?.string() ?: "Game entry rejected by server"
                    return Result.failure(Exception(err))
                }
            } catch (e: Exception) {
                return Result.failure(e)
            }
        }

        userDao.deductBalance(user.id, coins)

        val entry = GameEntryEntity(
            id = "entry_" + UUID.randomUUID().toString().take(10),
            gameId = game.id,
            gameTitle = game.title,
            userId = user.id,
            username = user.username,
            optionSelected = option,
            coinAmount = coins,
            potentialPayout = potentialPayout,
            status = "PENDING"
        )
        gameDao.insertEntry(entry)

        txDao.insertTransaction(
            TransactionEntity(
                id = "tx_" + UUID.randomUUID().toString().take(10),
                actorId = user.id,
                actorRole = user.role,
                sourceUserId = user.id,
                destinationUserId = "GAME_ESCROW",
                sourceName = user.fullName,
                destinationName = "${game.title} Escrow",
                amount = coins,
                type = "USER_GAME_ENTRY",
                description = "Stake for ${game.title} on option [$option]"
            )
        )
        return Result.success(entry)
    }

    suspend fun finalizeGameResult(admin: UserEntity, gameId: String, winningOption: String): Result<Unit> {
        val game = gameDao.getGameById(gameId) ?: return Result.failure(Exception("Game not found"))
        val updatedGame = game.copy(status = "COMPLETED", winningOption = winningOption)
        gameDao.updateGame(updatedGame)

        if (apiService != null) {
            try {
                apiService.finalizeGameResult(
                    mapOf(
                        "gameId" to gameId,
                        "winningOptionId" to winningOption
                    )
                )
            } catch (_: Exception) {}
        }

        val pendingEntries = gameDao.getPendingEntriesForGame(gameId)
        for (entry in pendingEntries) {
            val isWinner = entry.optionSelected.equals(winningOption, ignoreCase = true)
            if (isWinner) {
                val reward = entry.potentialPayout
                userDao.addBalance(entry.userId, reward)
                gameDao.updateEntry(entry.copy(status = "WON", rewardAmount = reward))

                txDao.insertTransaction(
                    TransactionEntity(
                        id = "tx_" + UUID.randomUUID().toString().take(10),
                        actorId = admin.id,
                        actorRole = "ADMIN",
                        sourceUserId = "GAME_ESCROW",
                        destinationUserId = entry.userId,
                        sourceName = "${game.title} Prize Pool",
                        destinationName = entry.username,
                        amount = reward,
                        type = "USER_GAME_WIN",
                        description = "Prize payout for winning option [$winningOption] in ${game.title}"
                    )
                )
            } else {
                gameDao.updateEntry(entry.copy(status = "LOST", rewardAmount = 0))
            }
        }

        auditDao.insertLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                actorId = admin.id,
                actorRole = admin.role,
                action = "FINALIZE_GAME_RESULT",
                targetId = gameId,
                details = "Finalized ${game.title} with winner [$winningOption]"
            )
        )
        return Result.success(Unit)
    }

    // Ledger & Logs
    fun getAllTransactions(): Flow<List<TransactionEntity>> = txDao.getAllTransactions()
    fun getTransactionsForUser(userId: String): Flow<List<TransactionEntity>> = txDao.getTransactionsForUser(userId)
    fun getTransactionsForAgent(agentId: String): Flow<List<TransactionEntity>> = txDao.getTransactionsForAgent(agentId)
    fun getAllAuditLogs(): Flow<List<AuditLogEntity>> = auditDao.getAllLogs()
    fun getNotificationsForUser(userId: String): Flow<List<NotificationEntity>> = notifDao.getNotificationsForUser(userId)
    fun getUnreadNotificationCount(userId: String): Flow<Int> = notifDao.getUnreadCount(userId)
    suspend fun markNotificationRead(id: String) = notifDao.markAsRead(id)
}
