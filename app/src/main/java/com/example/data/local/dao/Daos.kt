package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.GameEntity
import com.example.data.local.entity.GameEntryEntity
import com.example.data.local.entity.NotificationEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE role = 'AGENT' ORDER BY createdAt DESC")
    fun getAllAgents(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE role = 'USER' AND agentId = :agentId ORDER BY createdAt DESC")
    fun getUsersByAgent(agentId: String): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE role = 'USER' ORDER BY createdAt DESC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun getAllAccounts(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET balance = balance + :amount WHERE id = :userId")
    suspend fun addBalance(userId: String, amount: Long)

    @Query("UPDATE users SET balance = balance - :amount WHERE id = :userId")
    suspend fun deductBalance(userId: String, amount: Long)

    @Query("UPDATE users SET status = :status WHERE id = :userId")
    suspend fun updateUserStatus(userId: String, status: String)

    @Query("SELECT SUM(balance) FROM users")
    suspend fun getTotalCirculatingCoins(): Long?
}

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE sourceUserId = :userId OR destinationUserId = :userId ORDER BY timestamp DESC")
    fun getTransactionsForUser(userId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE actorId = :agentId OR sourceUserId = :agentId OR destinationUserId = :agentId ORDER BY timestamp DESC")
    fun getTransactionsForAgent(agentId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int): Flow<List<TransactionEntity>>
}

@Dao
interface GameDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: GameEntity)

    @Update
    suspend fun updateGame(game: GameEntity)

    @Query("SELECT * FROM games ORDER BY createdAt DESC")
    fun getAllGames(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE status = 'OPEN' ORDER BY createdAt DESC")
    fun getActiveGames(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE id = :gameId")
    suspend fun getGameById(gameId: String): GameEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: GameEntryEntity)

    @Update
    suspend fun updateEntry(entry: GameEntryEntity)

    @Query("SELECT * FROM game_entries WHERE userId = :userId ORDER BY createdAt DESC")
    fun getEntriesByUser(userId: String): Flow<List<GameEntryEntity>>

    @Query("SELECT * FROM game_entries WHERE gameId = :gameId ORDER BY createdAt DESC")
    fun getEntriesForGame(gameId: String): Flow<List<GameEntryEntity>>

    @Query("SELECT * FROM game_entries WHERE gameId = :gameId AND status = 'PENDING'")
    suspend fun getPendingEntriesForGame(gameId: String): List<GameEntryEntity>

    @Query("SELECT * FROM game_entries ORDER BY createdAt DESC LIMIT 50")
    fun getAllRecentEntries(): Flow<List<GameEntryEntity>>
}

@Dao
interface AuditDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AuditLogEntity)

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs WHERE actorRole = :role ORDER BY timestamp DESC")
    fun getLogsByRole(role: String): Flow<List<AuditLogEntity>>
}

@Dao
interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY timestamp DESC")
    fun getNotificationsForUser(userId: String): Flow<List<NotificationEntity>>

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)

    @Query("SELECT COUNT(*) FROM notifications WHERE userId = :userId AND isRead = 0")
    fun getUnreadCount(userId: String): Flow<Int>
}
