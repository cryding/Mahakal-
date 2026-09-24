package com.example.backend.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.backend.database.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNotification(notification: NotificationEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNotifications(notifications: List<NotificationEntity>): List<Long>

    @Update
    suspend fun updateNotification(notification: NotificationEntity): Int

    @Query("SELECT * FROM notifications WHERE notification_id = :notificationId LIMIT 1")
    suspend fun getNotificationById(notificationId: String): NotificationEntity?

    @Query("""
        SELECT * FROM notifications 
        WHERE recipient_id = :recipientId 
        ORDER BY created_at DESC 
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getNotificationsForRecipient(
        recipientId: String,
        limit: Int,
        offset: Int
    ): List<NotificationEntity>

    @Query("""
        SELECT * FROM notifications 
        WHERE recipient_id = :recipientId 
          AND status = :status
        ORDER BY created_at DESC 
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getNotificationsForRecipientByStatus(
        recipientId: String,
        status: String,
        limit: Int,
        offset: Int
    ): List<NotificationEntity>

    @Query("""
        SELECT * FROM notifications 
        WHERE recipient_id = :recipientId 
          AND type = :type
        ORDER BY created_at DESC 
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getNotificationsForRecipientByType(
        recipientId: String,
        type: String,
        limit: Int,
        offset: Int
    ): List<NotificationEntity>

    @Query("SELECT COUNT(*) FROM notifications WHERE recipient_id = :recipientId")
    suspend fun countNotificationsForRecipient(recipientId: String): Int

    @Query("SELECT COUNT(*) FROM notifications WHERE recipient_id = :recipientId AND status = 'UNREAD'")
    suspend fun countUnreadForRecipient(recipientId: String): Int

    @Query("SELECT COUNT(*) FROM notifications WHERE recipient_id = :recipientId AND status = 'UNREAD'")
    fun observeUnreadCountForRecipient(recipientId: String): Flow<Int>

    @Query("""
        UPDATE notifications 
        SET status = 'READ', read_at = :readAt 
        WHERE notification_id = :notificationId AND recipient_id = :recipientId
    """)
    suspend fun markAsRead(notificationId: String, recipientId: String, readAt: Long): Int

    @Query("""
        UPDATE notifications 
        SET status = 'READ', read_at = :readAt 
        WHERE recipient_id = :recipientId AND status = 'UNREAD'
    """)
    suspend fun markAllAsRead(recipientId: String, readAt: Long): Int

    @Query("""
        UPDATE notifications 
        SET status = 'ARCHIVED' 
        WHERE notification_id = :notificationId AND recipient_id = :recipientId
    """)
    suspend fun archiveNotification(notificationId: String, recipientId: String): Int

    @Query("""
        SELECT COUNT(*) FROM notifications 
        WHERE correlation_id = :correlationId AND recipient_id = :recipientId AND type = :type
    """)
    suspend fun countByCorrelationAndRecipient(
        correlationId: String,
        recipientId: String,
        type: String
    ): Int

    @Query("DELETE FROM notifications WHERE expires_at IS NOT NULL AND expires_at < :nowTimestamp")
    suspend fun deleteExpiredNotifications(nowTimestamp: Long): Int
}
