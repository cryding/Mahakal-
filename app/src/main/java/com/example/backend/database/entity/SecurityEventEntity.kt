package com.example.backend.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "security_events",
    indices = [
        Index(value = ["event_type"]),
        Index(value = ["severity"]),
        Index(value = ["actor_id"]),
        Index(value = ["created_at"]),
        Index(value = ["correlation_id"])
    ]
)
data class SecurityEventEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "event_type")
    val eventType: String,

    @ColumnInfo(name = "severity")
    val severity: String,

    @ColumnInfo(name = "actor_id")
    val actorId: String?,

    @ColumnInfo(name = "actor_role")
    val actorRole: String?,

    @ColumnInfo(name = "target_id")
    val targetId: String?,

    @ColumnInfo(name = "target_type")
    val targetType: String?,

    @ColumnInfo(name = "ip_address")
    val ipAddress: String?,

    @ColumnInfo(name = "user_agent")
    val userAgent: String?,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "correlation_id")
    val correlationId: String,

    @ColumnInfo(name = "metadata_json")
    val metadataJson: String = "{}",

    @ColumnInfo(name = "created_at")
    val createdAt: Long
)
