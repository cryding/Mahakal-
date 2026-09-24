package com.example.backend.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.backend.database.entity.WalletEntity

@Dao
interface WalletDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(wallet: WalletEntity)

    @Query("SELECT * FROM wallets WHERE owner_id = :ownerId LIMIT 1")
    suspend fun findByOwnerId(ownerId: String): WalletEntity?

    @Query("SELECT * FROM wallets WHERE wallet_id = :walletId LIMIT 1")
    suspend fun findByWalletId(walletId: String): WalletEntity?

    @Update
    suspend fun update(wallet: WalletEntity): Int

    @Query("""
        UPDATE wallets 
        SET balance = balance - :amount, version = :newVersion, updated_at = :updatedAt 
        WHERE wallet_id = :walletId AND version = :expectedVersion AND balance >= :amount
    """)
    suspend fun deductBalanceWithVersion(
        walletId: String,
        amount: Long,
        expectedVersion: Long,
        newVersion: Long,
        updatedAt: Long
    ): Int

    @Query("""
        UPDATE wallets 
        SET balance = balance + :amount, version = :newVersion, updated_at = :updatedAt 
        WHERE wallet_id = :walletId AND version = :expectedVersion
    """)
    suspend fun addBalanceWithVersion(
        walletId: String,
        amount: Long,
        expectedVersion: Long,
        newVersion: Long,
        updatedAt: Long
    ): Int

    @Query("SELECT * FROM wallets WHERE owner_id IN (:ownerIds)")
    suspend fun findByOwnerIds(ownerIds: List<String>): List<WalletEntity>
}
