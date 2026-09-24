package com.example.backend.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rate_limits")
data class RateLimitEntity(
    @PrimaryKey
    @ColumnInfo(name = "rate_key")
    val rateKey: String,

    @ColumnInfo(name = "attempt_count")
    val attemptCount: Int,

    @ColumnInfo(name = "window_start")
    val windowStart: Long,

    @ColumnInfo(name = "locked_until")
    val lockedUntil: Long? = null
)
