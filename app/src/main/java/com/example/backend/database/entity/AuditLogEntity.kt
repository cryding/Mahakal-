package com.example.backend.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "audit_logs",
    indices = [
        Index(value = ["actor_id"]),
        Index(value = ["action"]),
        Index(value = ["created_at"])
    ]
)
data class AuditLogEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "actor_id")
    val actorId: String,

    @ColumnInfo(name = "actor_role")
    val actorRole: String,

    @ColumnInfo(name = "action")
    val action: String,

    @ColumnInfo(name = "target_id")
    val targetId: String? = null,

    @ColumnInfo(name = "target_type")
    val targetType: String? = null,

    @ColumnInfo(name = "request_id")
    val requestId: String,

    @ColumnInfo(name = "metadata_json")
    val metadataJson: String = "{}",

    @ColumnInfo(name = "before_state")
    val beforeState: String? = null,

    @ColumnInfo(name = "after_state")
    val afterState: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long
)
