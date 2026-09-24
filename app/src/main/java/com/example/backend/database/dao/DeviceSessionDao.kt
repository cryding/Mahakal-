package com.example.backend.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.backend.database.entity.DeviceSessionEntity

@Dao
interface DeviceSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun registerDevice(session: DeviceSessionEntity)

    @Update
    suspend fun updateDevice(session: DeviceSessionEntity): Int

    @Query("SELECT * FROM device_sessions WHERE device_id = :deviceId AND user_id = :userId LIMIT 1")
    suspend fun getDevice(deviceId: String, userId: String): DeviceSessionEntity?

    @Query("SELECT * FROM device_sessions WHERE user_id = :userId AND revoked_at IS NULL")
    suspend fun getActiveSessionsForUser(userId: String): List<DeviceSessionEntity>

    @Query("""
        UPDATE device_sessions 
        SET revoked_at = :revokedAt, updated_at = :revokedAt 
        WHERE device_id = :deviceId AND user_id = :userId
    """)
    suspend fun revokeDevice(deviceId: String, userId: String, revokedAt: Long): Int

    @Query("""
        UPDATE device_sessions 
        SET revoked_at = :revokedAt, updated_at = :revokedAt 
        WHERE user_id = :userId AND revoked_at IS NULL
    """)
    suspend fun revokeAllForUser(userId: String, revokedAt: Long): Int
}
