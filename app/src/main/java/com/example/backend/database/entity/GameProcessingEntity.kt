package com.example.backend.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Server-authoritative Game Processing Entity tracking result and reward calculation state.
 */
@Entity(
    tableName = "game_processings",
    indices = [
        Index(value = ["game_id"], unique = true),
        Index(value = ["status"])
    ]
)
data class GameProcessingEntity(
    @PrimaryKey
    @ColumnInfo(name = "processing_id")
    val processingId: String,

    @ColumnInfo(name = "game_id")
    val gameId: String,

    @ColumnInfo(name = "status")
    val status: String, // PENDING, PROCESSING, COMPLETED, FAILED, RETRY_PENDING

    @ColumnInfo(name = "started_at")
    val startedAt: Long,

    @ColumnInfo(name = "completed_at")
    val completedAt: Long? = null,

    @ColumnInfo(name = "attempt_count")
    val attemptCount: Int = 1,

    @ColumnInfo(name = "last_error_code")
    val lastErrorCode: String? = null,

    @ColumnInfo(name = "last_error_message")
    val lastErrorMessage: String? = null,

    @ColumnInfo(name = "correlation_id")
    val correlationId: String? = null,

    @ColumnInfo(name = "total_entries")
    val totalEntries: Int = 0,

    @ColumnInfo(name = "processed_entries")
    val processedEntries: Int = 0,

    @ColumnInfo(name = "failed_entries")
    val failedEntries: Int = 0,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
