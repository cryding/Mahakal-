package com.example.backend.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.backend.database.entity.GameEntity
import com.example.backend.database.entity.GameEntryEntity
import com.example.backend.database.entity.GameEventEntity
import com.example.backend.database.entity.GameOptionEntity
import com.example.backend.database.entity.GameProcessingEntity
import com.example.backend.database.entity.GameResultEntity

@Dao
interface GameDao {

    // --- Games ---
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertGame(game: GameEntity)

    @Update
    suspend fun updateGame(game: GameEntity): Int

    @Query("SELECT * FROM games WHERE game_id = :gameId LIMIT 1")
    suspend fun findGameById(gameId: String): GameEntity?

    @Query("SELECT * FROM games ORDER BY created_at DESC")
    suspend fun getAllGames(): List<GameEntity>

    @Query("SELECT * FROM games WHERE status IN (:statuses) ORDER BY start_time ASC, created_at DESC")
    suspend fun getGamesByStatus(statuses: List<String>): List<GameEntity>

    @Query("""
        UPDATE games 
        SET status = :newStatus, version = :newVersion, updated_at = :updatedAt 
        WHERE game_id = :gameId AND version = :expectedVersion
    """)
    suspend fun updateGameStatusWithVersion(
        gameId: String,
        newStatus: String,
        expectedVersion: Long,
        newVersion: Long,
        updatedAt: Long
    ): Int

    @Query("SELECT COUNT(*) FROM games")
    suspend fun countAllGames(): Int

    @Query("SELECT COUNT(*) FROM games WHERE status = :status")
    suspend fun countGamesByStatus(status: String): Int

    @Query("SELECT * FROM games ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    suspend fun getGamesPaginated(limit: Int, offset: Int): List<GameEntity>

    @Query("""
        SELECT * FROM games
        WHERE (:status IS NULL OR status = :status)
          AND (:gameType IS NULL OR game_type = :gameType)
          AND (:fromDate IS NULL OR created_at >= :fromDate)
          AND (:toDate IS NULL OR created_at <= :toDate)
        ORDER BY created_at DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getGamesFiltered(
        status: String?,
        gameType: String?,
        fromDate: Long?,
        toDate: Long?,
        limit: Int,
        offset: Int
    ): List<GameEntity>

    @Query("""
        SELECT COUNT(*) FROM games
        WHERE (:status IS NULL OR status = :status)
          AND (:gameType IS NULL OR game_type = :gameType)
          AND (:fromDate IS NULL OR created_at >= :fromDate)
          AND (:toDate IS NULL OR created_at <= :toDate)
    """)
    suspend fun countGamesFiltered(
        status: String?,
        gameType: String?,
        fromDate: Long?,
        toDate: Long?
    ): Int

    @Query("""
        SELECT * FROM games
        WHERE (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR game_id LIKE '%' || :query || '%')
        ORDER BY created_at DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun searchGames(query: String, limit: Int, offset: Int): List<GameEntity>

    @Query("""
        SELECT COUNT(*) FROM games
        WHERE (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR game_id LIKE '%' || :query || '%')
    """)
    suspend fun countSearchGames(query: String): Int

    @Query("SELECT * FROM games WHERE created_at >= :startTime AND created_at <= :endTime ORDER BY created_at DESC")
    suspend fun getGamesByDateRange(startTime: Long, endTime: Long): List<GameEntity>

    // --- Options ---
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertOptions(options: List<GameOptionEntity>)

    @Query("SELECT * FROM game_options WHERE game_id = :gameId ORDER BY option_code ASC")
    suspend fun getOptionsForGame(gameId: String): List<GameOptionEntity>

    @Query("SELECT * FROM game_options WHERE option_id = :optionId LIMIT 1")
    suspend fun findOptionById(optionId: String): GameOptionEntity?

    // --- Entries ---
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEntry(entry: GameEntryEntity)

    @Update
    suspend fun updateEntry(entry: GameEntryEntity): Int

    @Query("SELECT * FROM game_entries WHERE entry_id = :entryId LIMIT 1")
    suspend fun findEntryById(entryId: String): GameEntryEntity?

    @Query("SELECT * FROM game_entries WHERE idempotency_key = :key LIMIT 1")
    suspend fun findEntryByIdempotencyKey(key: String): GameEntryEntity?

    @Query("SELECT * FROM game_entries WHERE user_id = :userId ORDER BY created_at DESC LIMIT :limit")
    suspend fun getEntriesForUser(userId: String, limit: Int = 100): List<GameEntryEntity>

    @Query("SELECT * FROM game_entries WHERE game_id = :gameId ORDER BY created_at DESC")
    suspend fun getEntriesForGame(gameId: String): List<GameEntryEntity>

    @Query("SELECT COUNT(*) FROM game_entries WHERE game_id = :gameId")
    suspend fun countEntriesForGame(gameId: String): Int

    @Query("SELECT * FROM game_entries WHERE game_id = :gameId AND status = :status")
    suspend fun getEntriesForGameWithStatus(gameId: String, status: String): List<GameEntryEntity>

    @Query("SELECT * FROM game_entries WHERE game_id = :gameId ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    suspend fun getEntriesForGamePaginated(gameId: String, limit: Int, offset: Int): List<GameEntryEntity>

    @Query("""
        SELECT * FROM game_entries 
        WHERE game_id = :gameId 
          AND (:status IS NULL OR status = :status)
          AND (:optionId IS NULL OR selected_option_id = :optionId)
        ORDER BY created_at DESC 
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getEntriesForGameFiltered(
        gameId: String,
        status: String?,
        optionId: String?,
        limit: Int,
        offset: Int
    ): List<GameEntryEntity>

    @Query("""
        SELECT COUNT(*) FROM game_entries 
        WHERE game_id = :gameId 
          AND (:status IS NULL OR status = :status)
          AND (:optionId IS NULL OR selected_option_id = :optionId)
    """)
    suspend fun countEntriesForGameFiltered(
        gameId: String,
        status: String?,
        optionId: String?
    ): Int

    @Query("""
        SELECT * FROM game_entries 
        WHERE game_id = :gameId 
          AND (entry_id LIKE '%' || :query || '%' OR user_id LIKE '%' || :query || '%' OR deduction_transaction_id LIKE '%' || :query || '%')
        ORDER BY created_at DESC 
        LIMIT :limit OFFSET :offset
    """)
    suspend fun searchEntriesForGame(gameId: String, query: String, limit: Int, offset: Int): List<GameEntryEntity>

    @Query("""
        SELECT COUNT(*) FROM game_entries 
        WHERE game_id = :gameId 
          AND (entry_id LIKE '%' || :query || '%' OR user_id LIKE '%' || :query || '%' OR deduction_transaction_id LIKE '%' || :query || '%')
    """)
    suspend fun countSearchEntriesForGame(gameId: String, query: String): Int

    @Query("SELECT COUNT(*) FROM game_entries WHERE game_id = :gameId AND status = :status")
    suspend fun countEntriesByStatus(gameId: String, status: String): Int

    @Query("SELECT SUM(virtual_coin_amount) FROM game_entries WHERE game_id = :gameId AND status != 'CANCELLED'")
    suspend fun sumCoinsEnteredForGame(gameId: String): Long?

    @Query("SELECT SUM(reward_amount) FROM game_entries WHERE game_id = :gameId AND status = 'WON'")
    suspend fun sumCoinsWonForGame(gameId: String): Long?

    @Query("SELECT COUNT(*) FROM game_entries WHERE game_id = :gameId AND selected_option_id = :optionId")
    suspend fun countEntriesForGameAndOption(gameId: String, optionId: String): Int

    @Query("SELECT SUM(virtual_coin_amount) FROM game_entries WHERE game_id = :gameId AND selected_option_id = :optionId AND status != 'CANCELLED'")
    suspend fun sumCoinsForGameAndOption(gameId: String, optionId: String): Long?

    @Query("SELECT SUM(virtual_coin_amount) FROM game_entries WHERE status IN ('CONFIRMED', 'WON', 'LOST')")
    suspend fun sumAllCoinsEntered(): Long?

    @Query("SELECT SUM(reward_amount) FROM game_entries WHERE status = 'WON'")
    suspend fun sumAllCoinsRewarded(): Long?

    @Query("SELECT COUNT(DISTINCT user_id) FROM game_entries")
    suspend fun countUniqueParticipants(): Int

    @Query("SELECT * FROM game_entries ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    suspend fun getAllEntries(limit: Int, offset: Int): List<GameEntryEntity>

    @Query("SELECT COUNT(*) FROM game_entries")
    suspend fun countAllEntries(): Int

    @Query("SELECT COUNT(*) FROM game_entries WHERE status = :status")
    suspend fun countEntriesByStatusGlobal(status: String): Int

    @Query("SELECT * FROM game_entries WHERE created_at >= :startTime AND created_at <= :endTime ORDER BY created_at DESC")
    suspend fun getEntriesByDateRange(startTime: Long, endTime: Long): List<GameEntryEntity>

    // --- Results ---
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertResult(result: GameResultEntity)

    @Query("SELECT * FROM game_results WHERE game_id = :gameId LIMIT 1")
    suspend fun findResultByGameId(gameId: String): GameResultEntity?

    // --- Events ---
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEvent(event: GameEventEntity)

    @Query("SELECT * FROM game_events WHERE game_id = :gameId ORDER BY timestamp ASC")
    suspend fun getEventsForGame(gameId: String): List<GameEventEntity>

    @Query("SELECT * FROM game_events ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentEvents(limit: Int): List<GameEventEntity>

    // --- Game Processing ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProcessing(processing: GameProcessingEntity)

    @Query("SELECT * FROM game_processings WHERE game_id = :gameId LIMIT 1")
    suspend fun findProcessingByGameId(gameId: String): GameProcessingEntity?

    @Query("SELECT * FROM game_processings WHERE status IN ('FAILED', 'RETRY_PENDING') ORDER BY updated_at DESC")
    suspend fun getFailedOrPendingProcessings(): List<GameProcessingEntity>

    @Query("SELECT COUNT(*) FROM game_processings WHERE status IN ('FAILED', 'RETRY_PENDING')")
    suspend fun countFailedProcessings(): Int

    @Query("SELECT COUNT(*) FROM game_processings WHERE status = :status")
    suspend fun countProcessingsByStatus(status: String): Int
}
