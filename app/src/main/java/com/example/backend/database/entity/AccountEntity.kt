package com.example.backend.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "accounts",
    indices = [
        Index(value = ["login_id"], unique = true),
        Index(value = ["role"]),
        Index(value = ["parent_id"]),
        Index(value = ["status"]),
        Index(value = ["created_at"]),
        Index(value = ["parent_id", "status"]),
        Index(value = ["parent_id", "created_at"])
    ]
)
data class AccountEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "login_id")
    val loginId: String,

    @ColumnInfo(name = "password_hash")
    val passwordHash: String,

    @ColumnInfo(name = "salt")
    val salt: String,

    @ColumnInfo(name = "role")
    val role: String, // ADMIN, AGENT, USER

    @ColumnInfo(name = "status")
    val status: String, // ACTIVE, SUSPENDED, DISABLED

    @ColumnInfo(name = "parent_id")
    val parentId: String? = null,

    @ColumnInfo(name = "full_name")
    val fullName: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,

    @ColumnInfo(name = "last_login_at")
    val lastLoginAt: Long? = null,

    @ColumnInfo(name = "password_changed_at")
    val passwordChangedAt: Long? = null,

    @ColumnInfo(name = "must_change_password")
    val mustChangePassword: Boolean = false,

    @ColumnInfo(name = "failed_login_attempts")
    val failedLoginAttempts: Int = 0,

    @ColumnInfo(name = "locked_until")
    val lockedUntil: Long? = null,

    @ColumnInfo(name = "notes")
    val notes: String? = null
)
