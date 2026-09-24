package com.example.backend.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.backend.database.entity.NotificationPreferenceEntity

@Dao
interface NotificationPreferenceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePreferences(preferences: NotificationPreferenceEntity)

    @Query("SELECT * FROM notification_preferences WHERE user_id = :userId LIMIT 1")
    suspend fun getPreferencesForUser(userId: String): NotificationPreferenceEntity?
}
