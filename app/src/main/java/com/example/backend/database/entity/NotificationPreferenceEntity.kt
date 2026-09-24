package com.example.backend.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification_preferences")
data class NotificationPreferenceEntity(
    @PrimaryKey
    @ColumnInfo(name = "user_id")
    val userId: String,

    @ColumnInfo(name = "game_notifications_enabled")
    val gameNotificationsEnabled: Boolean = true,

    @ColumnInfo(name = "result_notifications_enabled")
    val resultNotificationsEnabled: Boolean = true,

    @ColumnInfo(name = "wallet_notifications_enabled")
    val walletNotificationsEnabled: Boolean = true,

    @ColumnInfo(name = "security_notifications_enabled")
    val securityNotificationsEnabled: Boolean = true, // Mandatory: security alerts cannot be disabled

    @ColumnInfo(name = "operational_notifications_enabled")
    val operationalNotificationsEnabled: Boolean = true,

    @ColumnInfo(name = "push_enabled")
    val pushEnabled: Boolean = true,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
