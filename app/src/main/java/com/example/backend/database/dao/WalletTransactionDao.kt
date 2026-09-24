package com.example.backend.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.backend.database.entity.WalletTransactionEntity

@Dao
interface WalletTransactionDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(transaction: WalletTransactionEntity)

    @Update
    suspend fun update(transaction: WalletTransactionEntity)

    @Query("SELECT * FROM wallet_transactions WHERE transaction_id = :transactionId LIMIT 1")
    suspend fun findById(transactionId: String): WalletTransactionEntity?

    @Query("SELECT * FROM wallet_transactions WHERE idempotency_key = :key LIMIT 1")
    suspend fun findByIdempotencyKey(key: String): WalletTransactionEntity?

    @Query("""
        SELECT * FROM wallet_transactions 
        WHERE source_wallet_id = :walletId OR destination_wallet_id = :walletId 
        ORDER BY timestamp DESC 
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getTransactionsForWallet(walletId: String, limit: Int, offset: Int): List<WalletTransactionEntity>

    @Query("""
        SELECT COUNT(*) FROM wallet_transactions 
        WHERE source_wallet_id = :walletId OR destination_wallet_id = :walletId
    """)
    suspend fun countTransactionsForWallet(walletId: String): Int

    @Query("""
        SELECT * FROM wallet_transactions 
        WHERE source_wallet_id IN (:walletIds) OR destination_wallet_id IN (:walletIds) 
        ORDER BY timestamp DESC 
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getTransactionsForWallets(walletIds: List<String>, limit: Int, offset: Int): List<WalletTransactionEntity>

    @Query("""
        SELECT COUNT(*) FROM wallet_transactions 
        WHERE source_wallet_id IN (:walletIds) OR destination_wallet_id IN (:walletIds)
    """)
    suspend fun countTransactionsForWallets(walletIds: List<String>): Int

    @Query("SELECT * FROM wallet_transactions ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getAllTransactions(limit: Int, offset: Int): List<WalletTransactionEntity>

    @Query("SELECT COUNT(*) FROM wallet_transactions")
    suspend fun countAllTransactions(): Int

    @Query("SELECT * FROM wallet_transactions WHERE reference_id = :referenceId LIMIT 1")
    suspend fun findByReferenceId(referenceId: String): WalletTransactionEntity?

    @Query("SELECT * FROM wallet_transactions WHERE reference_id = :referenceId ORDER BY timestamp DESC")
    suspend fun findAllByReferenceId(referenceId: String): List<WalletTransactionEntity>

    @Query("SELECT * FROM wallet_transactions WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    suspend fun getTransactionsByDateRange(startTime: Long, endTime: Long): List<WalletTransactionEntity>

    @Query("SELECT * FROM wallet_transactions WHERE transaction_type = :type ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getTransactionsByType(type: String, limit: Int): List<WalletTransactionEntity>
}
