package com.example.backend.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.backend.database.entity.RateLimitEntity

@Dao
interface RateLimitDao {

    @Query("SELECT * FROM rate_limits WHERE rate_key = :key LIMIT 1")
    suspend fun findByKey(key: String): RateLimitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(rateLimit: RateLimitEntity)

    @Query("DELETE FROM rate_limits WHERE rate_key = :key")
    suspend fun delete(key: String)

    @Query("DELETE FROM rate_limits WHERE locked_until IS NOT NULL AND locked_until < :now")
    suspend fun clearExpired(now: Long)
}
