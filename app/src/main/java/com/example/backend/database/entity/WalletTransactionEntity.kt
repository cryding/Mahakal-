package com.example.backend.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Append-only immutable Transaction Ledger Entity.
 * Records every virtual coin transfer, deduction, reward, and reversal.
 */
@Entity(
    tableName = "wallet_transactions",
    indices = [
        Index(value = ["idempotency_key"], unique = true),
        Index(value = ["actor_id"]),
        Index(value = ["source_wallet_id"]),
        Index(value = ["destination_wallet_id"]),
        Index(value = ["timestamp"]),
        Index(value = ["transaction_type"]),
        Index(value = ["status"]),
        Index(value = ["reference_id"])
    ]
)
data class WalletTransactionEntity(
    @PrimaryKey
    @ColumnInfo(name = "transaction_id")
    val transactionId: String,

    @ColumnInfo(name = "idempotency_key")
    val idempotencyKey: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "actor_id")
    val actorId: String,

    @ColumnInfo(name = "actor_role")
    val actorRole: String,

    @ColumnInfo(name = "source_wallet_id")
    val sourceWalletId: String? = null,

    @ColumnInfo(name = "destination_wallet_id")
    val destinationWalletId: String? = null,

    @ColumnInfo(name = "amount")
    val amount: Long,

    @ColumnInfo(name = "balance_before_source")
    val balanceBeforeSource: Long? = null,

    @ColumnInfo(name = "balance_after_source")
    val balanceAfterSource: Long? = null,

    @ColumnInfo(name = "balance_before_destination")
    val balanceBeforeDestination: Long? = null,

    @ColumnInfo(name = "balance_after_destination")
    val balanceAfterDestination: Long? = null,

    @ColumnInfo(name = "transaction_type")
    val transactionType: String, // ADMIN_TO_AGENT, AGENT_TO_USER, ADMIN_DEDUCTION, AGENT_DEDUCTION, GAME_REWARD, GAME_DEDUCTION, SYSTEM_ADJUSTMENT

    @ColumnInfo(name = "reason")
    val reason: String,

    @ColumnInfo(name = "reference_id")
    val referenceId: String? = null,

    @ColumnInfo(name = "status")
    val status: String, // PENDING, COMPLETED, FAILED, REVERSED

    @ColumnInfo(name = "metadata_json")
    val metadataJson: String = "{}",

    @ColumnInfo(name = "created_at")
    val createdAt: Long
)
