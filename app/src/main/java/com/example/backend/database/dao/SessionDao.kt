package com.example.backend.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.backend.database.entity.SessionEntity

@Dao
interface SessionDao {

    @Query("SELECT * FROM sessions WHERE access_token = :token AND is_revoked = 0 LIMIT 1")
    suspend fun findActiveByAccessToken(token: String): SessionEntity?

    @Query("SELECT * FROM sessions WHERE refresh_token = :refreshToken AND is_revoked = 0 LIMIT 1")
    suspend fun findActiveByRefreshToken(refreshToken: String): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: SessionEntity)

    @Query("UPDATE sessions SET is_revoked = 1 WHERE access_token = :token")
    suspend fun revokeByAccessToken(token: String)

    @Query("UPDATE sessions SET is_revoked = 1 WHERE account_id = :accountId")
    suspend fun revokeAllForAccount(accountId: String)

    @Query("UPDATE sessions SET last_activity_at = :timestamp WHERE access_token = :token")
    suspend fun updateActivity(token: String, timestamp: Long)

    @Query("SELECT * FROM sessions WHERE refresh_token = :refreshToken LIMIT 1")
    suspend fun findAnyByRefreshToken(refreshToken: String): SessionEntity?

    @Query("SELECT COUNT(*) FROM sessions WHERE is_revoked = 0 AND expires_at > :now")
    suspend fun countActiveSessions(now: Long): Int

    @Query("SELECT COUNT(*) FROM sessions WHERE is_revoked = 1")
    suspend fun countRevokedSessions(): Int

    @Query("SELECT * FROM sessions ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    suspend fun getSessionsPaged(limit: Int, offset: Int): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE account_id = :accountId AND is_revoked = 0 AND expires_at > :now")
    suspend fun getActiveSessionsForAccount(accountId: String, now: Long): List<SessionEntity>

    @Query("DELETE FROM sessions WHERE expires_at < :now OR is_revoked = 1")
    suspend fun cleanExpired(now: Long)
}
