package com.example.backend.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.backend.database.entity.AuditLogEntity

@Dao
interface AuditLogDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(auditLog: AuditLogEntity)

    @Query("SELECT * FROM audit_logs ORDER BY created_at DESC LIMIT :limit")
    suspend fun getRecentLogs(limit: Int): List<AuditLogEntity>

    @Query("SELECT * FROM audit_logs WHERE actor_id = :actorId ORDER BY created_at DESC LIMIT :limit")
    suspend fun getLogsByActor(actorId: String, limit: Int): List<AuditLogEntity>

    @Query("SELECT * FROM audit_logs WHERE action = :action ORDER BY created_at DESC LIMIT :limit")
    suspend fun getLogsByAction(action: String, limit: Int): List<AuditLogEntity>

    @Query("SELECT * FROM audit_logs WHERE target_id = :targetId OR actor_id = :targetId ORDER BY created_at DESC LIMIT :limit")
    suspend fun getLogsForTarget(targetId: String, limit: Int): List<AuditLogEntity>

    @Query("SELECT * FROM audit_logs ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    suspend fun getAllLogsPaginated(limit: Int, offset: Int): List<AuditLogEntity>

    @Query("SELECT COUNT(*) FROM audit_logs")
    suspend fun countAllLogs(): Int

    @Query("""
        SELECT * FROM audit_logs 
        WHERE (:actorId IS NULL OR actor_id = :actorId)
          AND (:action IS NULL OR action = :action)
          AND (:targetId IS NULL OR target_id = :targetId)
          AND (:targetType IS NULL OR target_type = :targetType)
          AND created_at >= :startTime 
          AND created_at <= :endTime
        ORDER BY created_at DESC 
        LIMIT :limit OFFSET :offset
    """)
    suspend fun queryLogs(
        actorId: String?,
        action: String?,
        targetId: String?,
        targetType: String?,
        startTime: Long,
        endTime: Long,
        limit: Int,
        offset: Int
    ): List<AuditLogEntity>

    @Query("""
        SELECT COUNT(*) FROM audit_logs 
        WHERE (:actorId IS NULL OR actor_id = :actorId)
          AND (:action IS NULL OR action = :action)
          AND (:targetId IS NULL OR target_id = :targetId)
          AND (:targetType IS NULL OR target_type = :targetType)
          AND created_at >= :startTime 
          AND created_at <= :endTime
    """)
    suspend fun countQueryLogs(
        actorId: String?,
        action: String?,
        targetId: String?,
        targetType: String?,
        startTime: Long,
        endTime: Long
    ): Int
}
