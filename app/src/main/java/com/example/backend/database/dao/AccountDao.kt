package com.example.backend.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.backend.database.entity.AccountEntity

@Dao
interface AccountDao {

    @Query("SELECT * FROM accounts WHERE login_id = :loginId LIMIT 1")
    suspend fun findByLoginId(loginId: String): AccountEntity?

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): AccountEntity?

    @Query("SELECT * FROM accounts WHERE role = :role")
    suspend fun findByRole(role: String): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE parent_id = :parentId")
    suspend fun findByParentId(parentId: String): List<AccountEntity>

    @Query("SELECT COUNT(*) FROM accounts WHERE role = 'ADMIN'")
    suspend fun countAdmins(): Int

    @Query("SELECT COUNT(*) FROM accounts WHERE locked_until IS NOT NULL AND locked_until > :now")
    suspend fun countLockedAccounts(now: Long): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(account: AccountEntity)

    @Update
    suspend fun update(account: AccountEntity)

    @Query("UPDATE accounts SET failed_login_attempts = :attempts, locked_until = :lockedUntil WHERE id = :id")
    suspend fun updateLockout(id: String, attempts: Int, lockedUntil: Long?)

    @Query("UPDATE accounts SET failed_login_attempts = 0, locked_until = NULL, last_login_at = :loginTime WHERE id = :id")
    suspend fun recordSuccessfulLogin(id: String, loginTime: Long)

    @Query("UPDATE accounts SET password_hash = :newHash, salt = :newSalt, password_changed_at = :changedAt, must_change_password = 0, updated_at = :changedAt WHERE id = :id")
    suspend fun updatePassword(id: String, newHash: String, newSalt: String, changedAt: Long)

    @Query("UPDATE accounts SET status = :status, updated_at = :timestamp WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, timestamp: Long)

    @Query("SELECT COUNT(*) FROM accounts WHERE role = 'USER' AND parent_id = :agentId")
    suspend fun countSubordinatedUsers(agentId: String): Int

    @Query("""
        SELECT * FROM accounts 
        WHERE role = 'AGENT' 
        AND parent_id = :adminId 
        AND (:statusFilter IS NULL OR status = :statusFilter)
        AND (:searchQuery IS NULL OR login_id LIKE '%' || :searchQuery || '%' OR full_name LIKE '%' || :searchQuery || '%')
        ORDER BY created_at DESC 
        LIMIT :limit OFFSET :offset
    """)
    suspend fun findAgentsPaged(
        adminId: String,
        statusFilter: String?,
        searchQuery: String?,
        limit: Int,
        offset: Int
    ): List<AccountEntity>

    @Query("""
        SELECT COUNT(*) FROM accounts 
        WHERE role = 'AGENT' 
        AND parent_id = :adminId 
        AND (:statusFilter IS NULL OR status = :statusFilter)
        AND (:searchQuery IS NULL OR login_id LIKE '%' || :searchQuery || '%' OR full_name LIKE '%' || :searchQuery || '%')
    """)
    suspend fun countAgents(
        adminId: String,
        statusFilter: String?,
        searchQuery: String?
    ): Int

    @Query("UPDATE accounts SET full_name = :fullName, notes = :notes, updated_at = :timestamp WHERE id = :id")
    suspend fun updateAgentProfile(id: String, fullName: String, notes: String?, timestamp: Long)

    @Query("UPDATE accounts SET full_name = :fullName, notes = :notes, updated_at = :timestamp WHERE id = :id")
    suspend fun updateUserProfile(id: String, fullName: String, notes: String?, timestamp: Long)

    @Query("""
        SELECT * FROM accounts 
        WHERE role = 'USER' 
        AND parent_id = :agentId 
        AND (:statusFilter IS NULL OR status = :statusFilter)
        AND (:searchQuery IS NULL OR login_id LIKE '%' || :searchQuery || '%' OR full_name LIKE '%' || :searchQuery || '%')
        ORDER BY created_at DESC 
        LIMIT :limit OFFSET :offset
    """)
    suspend fun findUsersPaged(
        agentId: String,
        statusFilter: String?,
        searchQuery: String?,
        limit: Int,
        offset: Int
    ): List<AccountEntity>

    @Query("""
        SELECT COUNT(*) FROM accounts 
        WHERE role = 'USER' 
        AND parent_id = :agentId 
        AND (:statusFilter IS NULL OR status = :statusFilter)
        AND (:searchQuery IS NULL OR login_id LIKE '%' || :searchQuery || '%' OR full_name LIKE '%' || :searchQuery || '%')
    """)
    suspend fun countUsers(
        agentId: String,
        statusFilter: String?,
        searchQuery: String?
    ): Int

    @Query("""
        SELECT * FROM accounts 
        WHERE role = 'USER' 
        AND (:statusFilter IS NULL OR status = :statusFilter)
        AND (:searchQuery IS NULL OR login_id LIKE '%' || :searchQuery || '%' OR full_name LIKE '%' || :searchQuery || '%')
        ORDER BY created_at DESC 
        LIMIT :limit OFFSET :offset
    """)
    suspend fun findAllUsersPaged(
        statusFilter: String?,
        searchQuery: String?,
        limit: Int,
        offset: Int
    ): List<AccountEntity>

    @Query("""
        SELECT COUNT(*) FROM accounts 
        WHERE role = 'USER' 
        AND (:statusFilter IS NULL OR status = :statusFilter)
        AND (:searchQuery IS NULL OR login_id LIKE '%' || :searchQuery || '%' OR full_name LIKE '%' || :searchQuery || '%')
    """)
    suspend fun countAllUsers(
        statusFilter: String?,
        searchQuery: String?
    ): Int

    @Query("UPDATE accounts SET password_hash = :newHash, salt = :newSalt, password_changed_at = :changedAt, must_change_password = 1, updated_at = :changedAt WHERE id = :id")
    suspend fun resetPassword(id: String, newHash: String, newSalt: String, changedAt: Long)
}
