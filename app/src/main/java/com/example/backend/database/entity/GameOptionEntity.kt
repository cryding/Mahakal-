package com.example.backend.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Options available for a Game.
 */
@Entity(
    tableName = "game_options",
    indices = [
        Index(value = ["game_id"]),
        Index(value = ["game_id", "option_code"], unique = true)
    ]
)
data class GameOptionEntity(
    @PrimaryKey
    @ColumnInfo(name = "option_id")
    val optionId: String,

    @ColumnInfo(name = "game_id")
    val gameId: String,

    @ColumnInfo(name = "option_code")
    val optionCode: String,

    @ColumnInfo(name = "display_name")
    val displayName: String,

    @ColumnInfo(name = "status")
    val status: String = "ACTIVE", // ACTIVE, DISABLED

    @ColumnInfo(name = "metadata_json")
    val metadataJson: String = "{}"
)
