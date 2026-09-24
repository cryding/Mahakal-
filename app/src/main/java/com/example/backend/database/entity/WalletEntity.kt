package com.example.backend.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing a Virtual Coin Wallet.
 * Strictly non-monetary internal units (VIRTUAL_COIN).
 * Each eligible account has exactly one virtual wallet.
 */
@Entity(
    tableName = "wallets",
    indices = [
        Index(value = ["owner_id"], unique = false),
        Index(value = ["owner_id", "owner_role"], unique = true)
    ]
)
data class WalletEntity(
    @PrimaryKey
    @ColumnInfo(name = "wallet_id")
    val walletId: String,

    @ColumnInfo(name = "owner_id")
    val ownerId: String,

    @ColumnInfo(name = "owner_role")
    val ownerRole: String, // ADMIN, AGENT, USER

    @ColumnInfo(name = "balance")
    val balance: Long = 0L, // Smallest integer unit: 1 coin = 1 integer unit

    @ColumnInfo(name = "currency_type")
    val currencyType: String = "VIRTUAL_COIN",

    @ColumnInfo(name = "version")
    val version: Long = 0L, // Optimistic concurrency version

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
