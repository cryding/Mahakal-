package com.example.backend.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notifications",
    indices = [
        Index(value = ["recipient_id", "created_at"]),
        Index(value = ["recipient_id", "status"]),
        Index(value = ["correlation_id", "recipient_id"]),
        Index(value = ["type"])
    ]
)
data class NotificationEntity(
    @PrimaryKey
    @ColumnInfo(name = "notification_id")
    val notificationId: String,

    @ColumnInfo(name = "recipient_id")
    val recipientId: String,

    @ColumnInfo(name = "recipient_role")
    val recipientRole: String,

    @ColumnInfo(name = "type")
    val type: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "body")
    val body: String,

    @ColumnInfo(name = "severity")
    val severity: String, // INFO, WARNING, CRITICAL

    @ColumnInfo(name = "reference_type")
    val referenceType: String? = null, // GAME, ENTRY, WALLET_TRANSACTION, ACCOUNT, SYSTEM

    @ColumnInfo(name = "reference_id")
    val referenceId: String? = null,

    @ColumnInfo(name = "status")
    val status: String, // UNREAD, READ, ARCHIVED

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "read_at")
    val readAt: Long? = null,

    @ColumnInfo(name = "expires_at")
    val expiresAt: Long? = null,

    @ColumnInfo(name = "metadata_json")
    val metadataJson: String = "{}",

    @ColumnInfo(name = "correlation_id")
    val correlationId: String? = null
)
