package com.example.backend.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.backend.database.entity.SecurityEventEntity

@Dao
interface SecurityEventDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(event: SecurityEventEntity)

    @Query("SELECT * FROM security_events ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    suspend fun getRecentEventsPaginated(limit: Int, offset: Int): List<SecurityEventEntity>

    @Query("SELECT COUNT(*) FROM security_events")
    suspend fun countAll(): Int

    @Query("""
        SELECT * FROM security_events
        WHERE (:eventType IS NULL OR event_type = :eventType)
          AND (:severity IS NULL OR severity = :severity)
          AND (:actorId IS NULL OR actor_id = :actorId)
          AND (:correlationId IS NULL OR correlation_id = :correlationId)
          AND created_at >= :startTime
          AND created_at <= :endTime
        ORDER BY created_at DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun queryEvents(
        eventType: String?,
        severity: String?,
        actorId: String?,
        correlationId: String?,
        startTime: Long,
        endTime: Long,
        limit: Int,
        offset: Int
    ): List<SecurityEventEntity>

    @Query("""
        SELECT COUNT(*) FROM security_events
        WHERE (:eventType IS NULL OR event_type = :eventType)
          AND (:severity IS NULL OR severity = :severity)
          AND (:actorId IS NULL OR actor_id = :actorId)
          AND (:correlationId IS NULL OR correlation_id = :correlationId)
          AND created_at >= :startTime
          AND created_at <= :endTime
    """)
    suspend fun countQueryEvents(
        eventType: String?,
        severity: String?,
        actorId: String?,
        correlationId: String?,
        startTime: Long,
        endTime: Long
    ): Int

    @Query("SELECT COUNT(*) FROM security_events WHERE event_type = 'LOGIN_FAILURE' AND created_at >= :since")
    suspend fun countLoginFailuresSince(since: Long): Int

    @Query("SELECT COUNT(*) FROM security_events WHERE event_type = 'ACCOUNT_LOCKED' AND created_at >= :since")
    suspend fun countAccountLockoutsSince(since: Long): Int

    @Query("SELECT COUNT(*) FROM security_events WHERE event_type = 'PRIVILEGE_DENIED' AND created_at >= :since")
    suspend fun countPrivilegeDeniedSince(since: Long): Int

    @Query("SELECT COUNT(*) FROM security_events WHERE event_type = 'IDOR_ATTEMPT' AND created_at >= :since")
    suspend fun countIdorAttemptsSince(since: Long): Int

    @Query("SELECT COUNT(*) FROM security_events WHERE event_type = 'RATE_LIMIT_EXCEEDED' AND created_at >= :since")
    suspend fun countRateLimitExceededSince(since: Long): Int

    @Query("SELECT COUNT(*) FROM security_events WHERE event_type = 'TOKEN_REUSE_DETECTED' AND created_at >= :since")
    suspend fun countTokenReuseSince(since: Long): Int

    @Query("SELECT COUNT(*) FROM security_events WHERE severity IN ('HIGH', 'CRITICAL') AND created_at >= :since")
    suspend fun countHighSeveritySince(since: Long): Int
}
