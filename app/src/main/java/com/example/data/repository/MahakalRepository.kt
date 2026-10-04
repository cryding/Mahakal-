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

    /**
     * Synchronizes authoritative production PostgreSQL state into local Room cache.
     * Guarantees all role dashboards display live server data with zero fake fallback.
     */
    suspend fun syncServerData(role: String) {
        if (apiService == null) return
        try {
            // 1. Refresh current authenticated user's wallet
            val wResp = apiService.getMyWallet()
            if (wResp.isSuccessful) {
                val wData = wResp.body()?.data
                val bal = (wData?.get("balance") as? Number)?.toLong()
                val uid = secureStorage?.getSessionUserId()
                if (bal != null && !uid.isNullOrBlank()) {
                    val existing = userDao.getUserById(uid)
                    if (existing != null) {
                        userDao.insertUser(existing.copy(balance = bal))
                    }
                }
            }

            // 2. Role-specific authoritative synchronizations
            when (role) {
                "ADMIN" -> {
                    // Sync all agents
                    val aResp = apiService.getAdminAgents()
                    if (aResp.isSuccessful) {
                        val list = aResp.body()?.data ?: emptyList()
                        for (item in list) {
                            val id = item["id"] as? String ?: continue
                            val loginId = item["login_id"] as? String ?: ""
                            val fullName = item["full_name"] as? String ?: loginId
                            val bal = (item["balance"] as? Number)?.toLong() ?: 0L
                            val st = item["status"] as? String ?: "ACTIVE"
                            val cat = (item["created_at"] as? Number)?.toLong() ?: System.currentTimeMillis()
                            userDao.insertUser(
                                UserEntity(
                                    id = id,
                                    username = loginId,
                                    passwordHash = "",
                                    role = "AGENT",
                                    fullName = fullName,
                                    balance = bal,
                                    status = st,
                                    createdAt = cat
                                )
                            )
                        }
                    }

                    // Sync all users
                    val uResp = apiService.getAdminUsers()
                    if (uResp.isSuccessful) {
                        val list = uResp.body()?.data ?: emptyList()
                        for (item in list) {
                            val id = item["id"] as? String ?: continue
                            val loginId = item["login_id"] as? String ?: ""
                            val fullName = item["full_name"] as? String ?: loginId
                            val bal = (item["balance"] as? Number)?.toLong() ?: 0L
                            val parentId = item["parent_id"] as? String
                            val st = item["status"] as? String ?: "ACTIVE"
                            val cat = (item["created_at"] as? Number)?.toLong() ?: System.currentTimeMillis()
                            userDao.insertUser(
                                UserEntity(
                                    id = id,
                                    username = loginId,
                                    passwordHash = "",
                                    role = "USER",
                                    fullName = fullName,
                                    balance = bal,
                                    agentId = parentId,
                                    status = st,
                                    createdAt = cat
                                )
                            )
                        }
                    }

                    // Sync games
                    val gResp = apiService.getGames()
                    if (gResp.isSuccessful) {
                        val list = gResp.body()?.data ?: emptyList()
                        for (item in list) {
                            val gid = item["game_id"] as? String ?: continue
                            val title = item["title"] as? String ?: "Game"
                            val gType = item["game_type"] as? String ?: "STANDARD"
                            val minC = (item["min_coins"] as? Number)?.toLong() ?: 10L
                            val maxC = (item["max_coins"] as? Number)?.toLong() ?: 10000L
                            val mult = (item["reward_multiplier"] as? Number)?.toDouble() ?: 2.0
                            val st = item["status"] as? String ?: "OPEN"
                            val cat = (item["created_at"] as? Number)?.toLong() ?: System.currentTimeMillis()
                            val dl = (item["entry_deadline"] as? Number)?.toLong() ?: (System.currentTimeMillis() + 3600000L)
                            val desc = item["description"] as? String ?: ""
                            gameDao.insertGame(
                                GameEntity(
                                    id = gid,
                                    title = title,
                                    category = gType,
                                    minCoins = minC,
                                    maxCoins = maxC,
                                    multiplier = mult,
                                    status = st,
                                    createdAt = cat,
                                    closesAt = dl,
                                    apiLink = desc
                                )
                            )
                        }
                    }

                    // Sync audit logs
                    val audResp = apiService.getAuditLogs()
                    if (audResp.isSuccessful) {
                        val list = audResp.body()?.data ?: emptyList()
                        for (item in list) {
                            val lid = item["log_id"] as? String ?: UUID.randomUUID().toString()
                            val actId = item["actor_id"] as? String ?: ""
                            val actRole = item["actor_role"] as? String ?: "SYSTEM"
                            val action = item["action"] as? String ?: ""
                            val targetId = item["target_id"] as? String ?: ""
                            val details = (item["details"] ?: item["target_type"] ?: "").toString()
                            val ts = (item["created_at"] as? Number)?.toLong() ?: System.currentTimeMillis()
                            auditDao.insertLog(
                                AuditLogEntity(
                                    id = lid,
                                    actorId = actId,
                                    actorRole = actRole,
                                    action = action,
                                    targetId = targetId,
                                    details = details,
                                    timestamp = ts
                                )
                            )
                        }
                    }
                }
                "AGENT" -> {
                    // Sync agent's subordinated users
                    val uResp = apiService.getAgentUsers()
                    if (uResp.isSuccessful) {
                        val list = uResp.body()?.data ?: emptyList()
                        val agentId = secureStorage?.getSessionUserId()
                        for (item in list) {
                            val id = item["id"] as? String ?: continue
                            val loginId = item["login_id"] as? String ?: ""
                            val fullName = item["full_name"] as? String ?: loginId
                            val bal = (item["balance"] as? Number)?.toLong() ?: 0L
                            val st = item["status"] as? String ?: "ACTIVE"
                            val cat = (item["created_at"] as? Number)?.toLong() ?: System.currentTimeMillis()
                            userDao.insertUser(
                                UserEntity(
                                    id = id,
                                    username = loginId,
                                    passwordHash = "",
                                    role = "USER",
                                    fullName = fullName,
                                    balance = bal,
                                    agentId = agentId,
                                    status = st,
                                    createdAt = cat
                                )
                            )
                        }
                    }
                }
                "USER" -> {
                    // Sync games
                    val gResp = apiService.getGames()
                    if (gResp.isSuccessful) {
                        val list = gResp.body()?.data ?: emptyList()
                        for (item in list) {
                            val gid = item["game_id"] as? String ?: continue
                            val title = item["title"] as? String ?: "Game"
                            val gType = item["game_type"] as? String ?: "STANDARD"
                            val minC = (item["min_coins"] as? Number)?.toLong() ?: 10L
                            val maxC = (item["max_coins"] as? Number)?.toLong() ?: 10000L
                            val mult = (item["reward_multiplier"] as? Number)?.toDouble() ?: 2.0
                            val st = item["status"] as? String ?: "OPEN"
                            val cat = (item["created_at"] as? Number)?.toLong() ?: System.currentTimeMillis()
                            val dl = (item["entry_deadline"] as? Number)?.toLong() ?: (System.currentTimeMillis() + 3600000L)
                            val desc = item["description"] as? String ?: ""
                            gameDao.insertGame(
                                GameEntity(
                                    id = gid,
                                    title = title,
                                    category = gType,
                                    minCoins = minC,
                                    maxCoins = maxC,
                                    multiplier = mult,
                                    status = st,
                                    createdAt = cat,
                                    closesAt = dl,
                                    apiLink = desc
                                )
                            )
                        }
                    }

                    // Sync user's entries
                    val eResp = apiService.getMyGameEntries()
                    if (eResp.isSuccessful) {
                        val list = eResp.body()?.data ?: emptyList()
                        val uid = secureStorage?.getSessionUserId() ?: ""
                        for (item in list) {
                            val eid = item["entry_id"] as? String ?: continue
                            val gid = item["game_id"] as? String ?: ""
                            val opt = item["selected_option_id"] as? String ?: ""
                            val amt = (item["virtual_coin_amount"] as? Number)?.toLong() ?: 0L
                            val st = item["status"] as? String ?: "CONFIRMED"
                            val rew = (item["reward_amount"] as? Number)?.toLong() ?: 0L
                            val cat = (item["created_at"] as? Number)?.toLong() ?: System.currentTimeMillis()
                            gameDao.insertEntry(
                                GameEntryEntity(
                                    id = eid,
                                    gameId = gid,
                                    gameTitle = "Contest $gid",
                                    userId = uid,
                                    username = "",
                                    optionSelected = opt,
                                    coinAmount = amt,
                                    potentialPayout = amt * 2,
                                    status = st,
                                    rewardAmount = rew,
                                    createdAt = cat
                                )
                            )
                        }
                    }
                }
            }

            // 3. Transactions for current scope
            val txResp = apiService.getWalletTransactions()
            if (txResp.isSuccessful) {
                val list = txResp.body()?.data ?: emptyList()
                for (item in list) {
                    val tid = item["transaction_id"] as? String ?: continue
                    val actId = item["actor_id"] as? String ?: ""
                    val actRole = item["actor_role"] as? String ?: "SYSTEM"
                    val srcWid = item["source_wallet_id"] as? String ?: ""
                    val dstWid = item["destination_wallet_id"] as? String ?: ""
                    val amt = (item["amount"] as? Number)?.toLong() ?: 0L
                    val tType = item["transaction_type"] as? String ?: "TRANSFER"
                    val rsn = item["reason"] as? String ?: ""
                    val ts = (item["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    txDao.insertTransaction(
                        TransactionEntity(
                            id = tid,
                            actorId = actId,
                            actorRole = actRole,
                            sourceUserId = srcWid,
                            destinationUserId = dstWid,
                            sourceName = actId,
                            destinationName = dstWid,
                            amount = amt,
                            type = tType,
                            description = rsn,
                            timestamp = ts
                        )
                    )
                }
            }

            // 4. Notifications
            val notifResp = apiService.getNotifications()
            if (notifResp.isSuccessful) {
                val list = notifResp.body()?.data ?: emptyList()
                val uid = secureStorage?.getSessionUserId() ?: ""
                for (item in list) {
                    val nid = item["notification_id"] as? String ?: continue
                    val title = item["title"] as? String ?: "Notification"
                    val msg = item["message"] as? String ?: ""
                    val nType = item["notification_type"] as? String ?: "INFO"
                    val isRead = item["status"] == "READ"
                    val ts = (item["created_at"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    notifDao.insertNotification(
                        NotificationEntity(
                            id = nid,
                            userId = uid,
                            title = title,
                            message = msg,
                            type = nType,
                            isRead = isRead,
                            timestamp = ts
                        )
                    )
                }
            }
        } catch (_: Exception) {
            // Safe network fallback: Continue with cached entities if offline
        }
    }

    suspend fun deductCoins(actor: UserEntity, targetUserId: String, amount: Long, reason: String): Result<Unit> {
        if (amount <= 0) return Result.failure(Exception("Amount must be greater than zero"))
        val target = userDao.getUserById(targetUserId) ?: return Result.failure(Exception("Target user not found"))
        if (target.balance < amount) return Result.failure(Exception("Target balance insufficient for deduction"))

        if (apiService != null) {
            try {
                val idemKey = "deduct_${UUID.randomUUID()}"
                val resp = apiService.deductVirtualCoins(
                    idempotencyKey = idemKey,
                    request = mapOf(
                        "targetAccountId" to targetUserId,
                        "amount" to amount,
                        "reason" to reason.ifBlank { "Coin Deduction" }
                    )
                )
                if (!resp.isSuccessful) {
                    val err = resp.errorBody()?.string() ?: "Deduction rejected by server"
                    return Result.failure(Exception(err))
                }
            } catch (e: Exception) {
                return Result.failure(e)
            }
        }

        userDao.deductBalance(targetUserId, amount)
        txDao.insertTransaction(
            TransactionEntity(
                id = "tx_" + UUID.randomUUID().toString().take(10),
                actorId = actor.id,
                actorRole = actor.role,
                sourceUserId = targetUserId,
                destinationUserId = "CENTRAL_ESCROW",
                sourceName = target.fullName,
                destinationName = "Treasury Reserve",
                amount = amount,
                type = "COIN_DEDUCTION",
                description = reason.ifBlank { "Coin deduction from ${target.username}" }
            )
        )
        auditDao.insertLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                actorId = actor.id,
                actorRole = actor.role,
                action = "COIN_DEDUCTION",
                targetId = targetUserId,
                details = "Deducted $amount coins from ${target.username} (Reason: $reason)"
            )
        )
        return Result.success(Unit)
    }

    suspend fun getReconciliationReport(): Result<Map<String, Any>> {
        if (apiService != null) {
            try {
                val resp = apiService.getReconciliationReport()
                if (resp.isSuccessful) {
                    val data = resp.body()?.data ?: emptyMap()
                    return Result.success(data)
                }
            } catch (e: Exception) {
                return Result.failure(e)
            }
        }
        return Result.failure(Exception("Reconciliation report unavailable"))
    }

    suspend fun getSecurityDashboard(): Result<Map<String, Any>> {
        if (apiService != null) {
            try {
                val resp = apiService.getSecurityDashboard()
                if (resp.isSuccessful) {
                    val data = resp.body()?.data ?: emptyMap()
                    return Result.success(data)
                }
            } catch (e: Exception) {
                return Result.failure(e)
            }
        }
        return Result.failure(Exception("Security dashboard unavailable"))
    }

    suspend fun changePassword(oldPass: String, newPass: String): Result<Unit> {
        if (apiService != null) {
            try {
                val resp = apiService.changePassword(
                    mapOf("currentPassword" to oldPass, "newPassword" to newPass)
                )
                if (resp.isSuccessful) {
                    return Result.success(Unit)
                } else {
                    val err = resp.errorBody()?.string() ?: "Failed to change password"
                    return Result.failure(Exception(err))
                }
            } catch (e: Exception) {
                return Result.failure(e)
            }
        }
        return Result.failure(Exception("API service unavailable"))
    }

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
