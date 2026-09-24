package com.example.backend.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Server-authoritative Game domain entity.
 */
@Entity(
    tableName = "games",
    indices = [
        Index(value = ["status"]),
        Index(value = ["entry_deadline"]),
        Index(value = ["created_at"])
    ]
)
data class GameEntity(
    @PrimaryKey
    @ColumnInfo(name = "game_id")
    val gameId: String,

    @ColumnInfo(name = "game_type")
    val gameType: String, // e.g., "PREDICTION", "DIGIT_PREDICTION"

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "status")
    val status: String, // DRAFT, SCHEDULED, OPEN, CLOSED, RESULT_PENDING, RESULT_FINALIZED, CANCELLED, ARCHIVED

    @ColumnInfo(name = "start_time")
    val startTime: Long,

    @ColumnInfo(name = "entry_deadline")
    val entryDeadline: Long,

    @ColumnInfo(name = "result_time")
    val resultTime: Long,

    @ColumnInfo(name = "min_coins")
    val minCoins: Long = 10L,

    @ColumnInfo(name = "max_coins")
    val maxCoins: Long = 10_000L,

    @ColumnInfo(name = "reward_multiplier")
    val rewardMultiplier: Double = 2.0,

    @ColumnInfo(name = "created_by")
    val createdBy: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,

    @ColumnInfo(name = "version")
    val version: Long = 0L
)
