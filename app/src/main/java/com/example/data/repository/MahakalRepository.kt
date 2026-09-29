package com.example.data.repository

import com.example.data.local.MahakalDatabase
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.GameEntity
import com.example.data.local.entity.GameEntryEntity
import com.example.data.local.entity.NotificationEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class MahakalRepository(private val db: MahakalDatabase) {
    private val userDao = db.userDao()
    private val txDao = db.transactionDao()
    private val gameDao = db.gameDao()
    private val auditDao = db.auditDao()
    private val notifDao = db.notificationDao()

    suspend fun bootstrapInitialData() {
        val existingAdmin = userDao.getUserByUsername("admin")
        if (existingAdmin == null) {
            val admin = UserEntity(
                id = "admin_master",
                username = "admin",
                passwordHash = "admin123",
                role = "ADMIN",
                fullName = "Mahakal Supreme Master",
                balance = 10_000_000L,
                status = "ACTIVE"
            )
            userDao.insertUser(admin)

            val agent1 = UserEntity(
                id = "agent_delhi",
                username = "agent_delhi",
                passwordHash = "agent123",
                role = "AGENT",
                fullName = "Delhi NCR Regional Agent",
                balance = 500_000L,
                agentId = "admin_master",
                status = "ACTIVE"
            )
            val agent2 = UserEntity(
                id = "agent_mumbai",
                username = "agent_mumbai",
                passwordHash = "agent123",
                role = "AGENT",
                fullName = "Mumbai West Agent",
                balance = 750_000L,
                agentId = "admin_master",
                status = "ACTIVE"
            )
            userDao.insertUser(agent1)
            userDao.insertUser(agent2)

            val player1 = UserEntity(
                id = "player_raj",
                username = "player_raj",
                passwordHash = "user123",
                role = "USER",
                fullName = "Rajesh Sharma",
                balance = 15_000L,
                agentId = "agent_delhi",
                status = "ACTIVE"
            )
            val player2 = UserEntity(
                id = "player_vikram",
                username = "player_vikram",
                passwordHash = "user123",
                role = "USER",
                fullName = "Vikram Singhania",
                balance = 25_000L,
                agentId = "agent_mumbai",
                status = "ACTIVE"
            )
            userDao.insertUser(player1)
            userDao.insertUser(player2)

            // Seed Games
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

            // Seed Audit log
            auditDao.insertLog(
                AuditLogEntity(
                    id = UUID.randomUUID().toString(),
                    actorId = "SYSTEM",
                    actorRole = "SYSTEM",
                    action = "PLATFORM_INITIALIZATION",
                    targetId = "admin_master",
                    details = "Master system bootstrap with 10M treasury reserve"
                )
            )

            // Seed Welcome Notifications
            notifDao.insertNotification(
                NotificationEntity(
                    id = UUID.randomUUID().toString(),
                    userId = "player_raj",
                    title = "Welcome to MAHAKAL",
                    message = "Your player account is verified. 15,000 Coins credited by Agent Delhi.",
                    type = "COIN_CREDIT"
                )
            )
            notifDao.insertNotification(
                NotificationEntity(
                    id = UUID.randomUUID().toString(),
                    userId = "player_vikram",
                    title = "Welcome Bonus Active",
                    message = "Enjoy high-yield multiplier games with verified cryptographic ledger transparency.",
                    type = "SYSTEM_ALERT"
                )
            )
        }
    }

    suspend fun authenticate(username: String, password: String): UserEntity? {
        val user = userDao.getUserByUsername(username.trim()) ?: return null
        if (user.passwordHash == password.trim() && user.status == "ACTIVE") {
            auditDao.insertLog(
                AuditLogEntity(
                    id = UUID.randomUUID().toString(),
                    actorId = user.id,
                    actorRole = user.role,
                    action = "USER_LOGIN_SUCCESS",
                    targetId = user.id,
                    details = "User ${user.username} logged in successfully"
                )
            )
            return user
        }
        return null
    }

    suspend fun getUserById(userId: String) = userDao.getUserById(userId)

    fun getAllAgents(): Flow<List<UserEntity>> = userDao.getAllAgents()
    fun getUsersByAgent(agentId: String): Flow<List<UserEntity>> = userDao.getUsersByAgent(agentId)
    fun getAllUsers(): Flow<List<UserEntity>> = userDao.getAllUsers()
    fun getAllAccounts(): Flow<List<UserEntity>> = userDao.getAllAccounts()

    suspend fun createAgent(username: String, fullName: String, initialCoins: Long): Result<UserEntity> {
        val existing = userDao.getUserByUsername(username)
        if (existing != null) return Result.failure(Exception("Agent username already exists"))
        val admin = userDao.getUserByUsername("admin") ?: return Result.failure(Exception("Admin not found"))
        if (admin.balance < initialCoins) return Result.failure(Exception("Insufficient treasury balance to allocate coins"))

        val newAgent = UserEntity(
            id = "agent_" + UUID.randomUUID().toString().take(8),
            username = username,
            passwordHash = "agent123",
            role = "AGENT",
            fullName = fullName,
            balance = initialCoins,
            agentId = admin.id,
            status = "ACTIVE"
        )
        userDao.deductBalance(admin.id, initialCoins)
        userDao.insertUser(newAgent)

        txDao.insertTransaction(
            TransactionEntity(
                id = "tx_" + UUID.randomUUID().toString().take(10),
                actorId = admin.id,
                actorRole = admin.role,
                sourceUserId = admin.id,
                destinationUserId = newAgent.id,
                sourceName = "Treasury",
                destinationName = newAgent.fullName,
                amount = initialCoins,
                type = "ADMIN_TO_AGENT",
                description = "Initial coin allocation for newly appointed Agent $username"
            )
        )

        auditDao.insertLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                actorId = admin.id,
                actorRole = admin.role,
                action = "AGENT_CREATION",
                targetId = newAgent.id,
                details = "Created agent $username with allocation of $initialCoins coins"
            )
        )

        return Result.success(newAgent)
    }

    suspend fun createUserUnderAgent(agentId: String, username: String, fullName: String, initialCoins: Long): Result<UserEntity> {
        val existing = userDao.getUserByUsername(username)
        if (existing != null) return Result.failure(Exception("Username already exists"))
        val agent = userDao.getUserById(agentId) ?: return Result.failure(Exception("Agent not found"))
        if (agent.balance < initialCoins) return Result.failure(Exception("Insufficient Agent balance to allocate coins"))

        val newUser = UserEntity(
            id = "user_" + UUID.randomUUID().toString().take(8),
            username = username,
            passwordHash = "user123",
            role = "USER",
            fullName = fullName,
            balance = initialCoins,
            agentId = agentId,
            status = "ACTIVE"
        )
        userDao.deductBalance(agent.id, initialCoins)
        userDao.insertUser(newUser)

        txDao.insertTransaction(
            TransactionEntity(
                id = "tx_" + UUID.randomUUID().toString().take(10),
                actorId = agent.id,
                actorRole = agent.role,
                sourceUserId = agent.id,
                destinationUserId = newUser.id,
                sourceName = agent.fullName,
                destinationName = newUser.fullName,
                amount = initialCoins,
                type = "AGENT_TO_USER",
                description = "Player activation deposit credited by agent ${agent.username}"
            )
        )

        auditDao.insertLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                actorId = agent.id,
                actorRole = agent.role,
                action = "USER_REGISTRATION",
                targetId = newUser.id,
                details = "Agent ${agent.username} onboarded player $username with $initialCoins coins"
            )
        )

        notifDao.insertNotification(
            NotificationEntity(
                id = UUID.randomUUID().toString(),
                userId = newUser.id,
                title = "Account Activated",
                message = "Your account has been credited with $initialCoins coins by ${agent.fullName}.",
                type = "COIN_CREDIT"
            )
        )

        return Result.success(newUser)
    }

    suspend fun transferCoins(actor: UserEntity, destinationUserId: String, amount: Long, notes: String): Result<Unit> {
        if (amount <= 0) return Result.failure(Exception("Amount must be greater than zero"))
        val sender = userDao.getUserById(actor.id) ?: return Result.failure(Exception("Sender not found"))
        if (sender.balance < amount) return Result.failure(Exception("Insufficient balance"))
        val recipient = userDao.getUserById(destinationUserId) ?: return Result.failure(Exception("Recipient not found"))

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

        notifDao.insertNotification(
            NotificationEntity(
                id = UUID.randomUUID().toString(),
                userId = recipient.id,
                title = "Coins Received",
                message = "Received $amount coins from ${sender.fullName}.",
                type = "COIN_CREDIT"
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

        auditDao.insertLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                actorId = admin.id,
                actorRole = "ADMIN",
                action = "TREASURY_MINT",
                targetId = admin.id,
                details = "Minted $amount coins into platform reserve"
            )
        )

        return Result.success(Unit)
    }

    suspend fun toggleUserStatus(actor: UserEntity, targetUserId: String, newStatus: String): Result<Unit> {
        val target = userDao.getUserById(targetUserId) ?: return Result.failure(Exception("Target not found"))
        userDao.updateUserStatus(targetUserId, newStatus)

        auditDao.insertLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                actorId = actor.id,
                actorRole = actor.role,
                action = "USER_STATUS_CHANGE",
                targetId = target.id,
                details = "Changed ${target.username} status from ${target.status} to $newStatus"
            )
        )
        return Result.success(Unit)
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
        multiplier: Double
    ): Result<GameEntity> {
        val game = GameEntity(
            id = "game_" + UUID.randomUUID().toString().take(8),
            title = title,
            category = category,
            minCoins = minCoins,
            maxCoins = maxCoins,
            multiplier = multiplier,
            status = "OPEN"
        )
        gameDao.insertGame(game)
        auditDao.insertLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                actorId = admin.id,
                actorRole = admin.role,
                action = "CREATE_GAME",
                targetId = game.id,
                details = "Created game ${game.title} with multiplier x$multiplier"
            )
        )
        return Result.success(game)
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

                notifDao.insertNotification(
                    NotificationEntity(
                        id = UUID.randomUUID().toString(),
                        userId = entry.userId,
                        title = "Congratulations! You Won!",
                        message = "You won $reward coins in ${game.title} with winning pick [$winningOption]!",
                        type = "GAME_WIN"
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
                details = "Finalized ${game.title} with winner [$winningOption], processed ${pendingEntries.size} entries"
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
