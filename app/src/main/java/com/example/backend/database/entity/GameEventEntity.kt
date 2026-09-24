package com.example.backend.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Audit and state history event for Games.
 */
@Entity(
    tableName = "game_events",
    indices = [
        Index(value = ["game_id"]),
        Index(value = ["timestamp"])
    ]
)
data class GameEventEntity(
    @PrimaryKey
    @ColumnInfo(name = "event_id")
    val eventId: String,

    @ColumnInfo(name = "game_id")
    val gameId: String,

    @ColumnInfo(name = "event_type")
    val eventType: String,

    @ColumnInfo(name = "actor_id")
    val actorId: String,

    @ColumnInfo(name = "actor_role")
    val actorRole: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "metadata_json")
    val metadataJson: String = "{}",

    @ColumnInfo(name = "correlation_id")
    val correlationId: String? = null
)
