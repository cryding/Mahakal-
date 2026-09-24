package com.example.backend.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.backend.database.entity.OutboxEventEntity

@Dao
interface OutboxDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEvent(event: OutboxEventEntity): Long

    @Update
    suspend fun updateEvent(event: OutboxEventEntity): Int

    @Query("SELECT * FROM outbox_events WHERE event_id = :eventId LIMIT 1")
    suspend fun getEventById(eventId: String): OutboxEventEntity?

    @Query("""
        SELECT * FROM outbox_events 
        WHERE status IN ('PENDING', 'RETRY_PENDING') 
          AND available_at <= :nowTimestamp 
        ORDER BY created_at ASC 
        LIMIT :limit
    """)
    suspend fun getPendingEvents(nowTimestamp: Long, limit: Int = 50): List<OutboxEventEntity>

    @Query("""
        UPDATE outbox_events 
        SET status = :newStatus, processed_at = :processedAt, last_error_code = :errorCode 
        WHERE event_id = :eventId
    """)
    suspend fun updateStatus(
        eventId: String,
        newStatus: String,
        processedAt: Long?,
        errorCode: String? = null
    ): Int

    @Query("DELETE FROM outbox_events WHERE status = 'COMPLETED' AND processed_at < :retentionTimestamp")
    suspend fun cleanupCompletedEvents(retentionTimestamp: Long): Int

    @Query("SELECT COUNT(*) FROM outbox_events WHERE status IN ('PENDING', 'RETRY_PENDING') AND available_at <= :nowTimestamp")
    suspend fun countPendingEvents(nowTimestamp: Long): Int

    suspend fun markProcessed(eventId: String, processedAt: Long): Int {
        return updateStatus(eventId, "COMPLETED", processedAt, null)
    }

    suspend fun markFailed(eventId: String, errorCode: String, now: Long): Int {
        return updateStatus(eventId, "DEAD_LETTER", now, errorCode)
    }

    @Query("""
        UPDATE outbox_events 
        SET status = 'RETRY_PENDING', attempt_count = :attemptCount, available_at = :availableAt, last_error_code = :errorCode 
        WHERE event_id = :eventId
    """)
    suspend fun scheduleRetry(eventId: String, attemptCount: Int, availableAt: Long, errorCode: String): Int
}
