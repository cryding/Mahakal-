package com.example.backend.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "device_sessions",
    primaryKeys = ["device_id", "user_id"],
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["push_token"])
    ]
)
data class DeviceSessionEntity(
    @ColumnInfo(name = "device_id")
    val deviceId: String,

    @ColumnInfo(name = "user_id")
    val userId: String,

    @ColumnInfo(name = "push_token")
    val pushToken: String,

    @ColumnInfo(name = "platform")
    val platform: String = "ANDROID",

    @ColumnInfo(name = "app_version")
    val appVersion: String,

    @ColumnInfo(name = "last_seen_at")
    val lastSeenAt: Long,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,

    @ColumnInfo(name = "revoked_at")
    val revokedAt: Long? = null
)
