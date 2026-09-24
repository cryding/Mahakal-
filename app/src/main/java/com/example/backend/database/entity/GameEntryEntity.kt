package com.example.backend.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Immutable User Game Entry Entity.
 * Atomic association with a Virtual Coin deduction transaction.
 */
@Entity(
    tableName = "game_entries",
    indices = [
        Index(value = ["game_id"]),
        Index(value = ["user_id"]),
        Index(value = ["idempotency_key"], unique = true),
        Index(value = ["status"])
    ]
)
data class GameEntryEntity(
    @PrimaryKey
    @ColumnInfo(name = "entry_id")
    val entryId: String,

    @ColumnInfo(name = "game_id")
    val gameId: String,

    @ColumnInfo(name = "user_id")
    val userId: String,

    @ColumnInfo(name = "selected_option_id")
    val selectedOptionId: String,

    @ColumnInfo(name = "virtual_coin_amount")
    val virtualCoinAmount: Long,

    @ColumnInfo(name = "status")
    val status: String, // CONFIRMED, WON, LOST, REFUNDED, CANCELLED

    @ColumnInfo(name = "idempotency_key")
    val idempotencyKey: String,

    @ColumnInfo(name = "deduction_transaction_id")
    val deductionTransactionId: String,

    @ColumnInfo(name = "reward_transaction_id")
    val rewardTransactionId: String? = null,

    @ColumnInfo(name = "reward_amount")
    val rewardAmount: Long? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
