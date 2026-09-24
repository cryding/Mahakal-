package com.example.backend.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Server-authoritative finalized Game Result Entity.
 */
@Entity(
    tableName = "game_results",
    indices = [
        Index(value = ["game_id"], unique = true)
    ]
)
data class GameResultEntity(
    @PrimaryKey
    @ColumnInfo(name = "result_id")
    val resultId: String,

    @ColumnInfo(name = "game_id")
    val gameId: String,

    @ColumnInfo(name = "winning_option_id")
    val winningOptionId: String,

    @ColumnInfo(name = "result_status")
    val resultStatus: String = "FINALIZED", // FINALIZED

    @ColumnInfo(name = "finalized_by")
    val finalizedBy: String, // Admin account ID

    @ColumnInfo(name = "finalized_at")
    val finalizedAt: Long,

    @ColumnInfo(name = "result_version")
    val resultVersion: Long = 1L,

    @ColumnInfo(name = "metadata_json")
    val metadataJson: String = "{}"
)
