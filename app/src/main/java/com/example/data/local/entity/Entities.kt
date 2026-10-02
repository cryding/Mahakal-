package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val username: String,
    val passwordHash: String,
    val role: String, // "ADMIN", "AGENT", "USER"
    val fullName: String,
    val balance: Long,
    val agentId: String? = null, // for USER, refers to their AGENT
    val status: String = "ACTIVE", // "ACTIVE", "FROZEN", "LOCKED"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val actorId: String,
    val actorRole: String,
    val sourceUserId: String,
    val destinationUserId: String,
    val sourceName: String,
    val destinationName: String,
    val amount: Long,
    val type: String, // "TREASURY_MINT", "ADMIN_TO_AGENT", "AGENT_TO_USER", "USER_GAME_ENTRY", "USER_GAME_WIN", "AGENT_RECALL"
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey val id: String,
    val title: String,
    val category: String, // "MATKA_SINGLE", "JODI_PAIR", "PATTI_TRIO", "COLOR_WHEEL", "LUCKY_DICE"
    val minCoins: Long,
    val maxCoins: Long,
    val multiplier: Double,
    val status: String, // "OPEN", "LOCKED", "COMPLETED", "CANCELLED"
    val winningOption: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val closesAt: Long = System.currentTimeMillis() + 3600000,
    val apiLink: String = ""
)

@Entity(tableName = "game_entries")
data class GameEntryEntity(
    @PrimaryKey val id: String,
    val gameId: String,
    val gameTitle: String,
    val userId: String,
    val username: String,
    val optionSelected: String,
    val coinAmount: Long,
    val potentialPayout: Long,
    val status: String, // "PENDING", "WON", "LOST", "REFUNDED"
    val rewardAmount: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey val id: String,
    val actorId: String,
    val actorRole: String,
    val action: String,
    val targetId: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val message: String,
    val type: String, // "COIN_CREDIT", "COIN_DEBIT", "GAME_WIN", "SYSTEM_ALERT"
    val isRead: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
