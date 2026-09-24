package com.example.backend.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "outbox_events",
    indices = [
        Index(value = ["status", "available_at"]),
        Index(value = ["created_at"])
    ]
)
data class OutboxEventEntity(
    @PrimaryKey
    @ColumnInfo(name = "event_id")
    val eventId: String,

    @ColumnInfo(name = "event_type")
    val eventType: String,

    @ColumnInfo(name = "aggregate_type")
    val aggregateType: String,

    @ColumnInfo(name = "aggregate_id")
    val aggregateId: String,

    @ColumnInfo(name = "payload")
    val payload: String,

    @ColumnInfo(name = "status")
    val status: String, // PENDING, PROCESSING, COMPLETED, FAILED, RETRY_PENDING

    @ColumnInfo(name = "attempt_count")
    val attemptCount: Int = 0,

    @ColumnInfo(name = "available_at")
    val availableAt: Long,

    @ColumnInfo(name = "processed_at")
    val processedAt: Long? = null,

    @ColumnInfo(name = "last_error_code")
    val lastErrorCode: String? = null,

    @ColumnInfo(name = "correlation_id")
    val correlationId: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long
)
