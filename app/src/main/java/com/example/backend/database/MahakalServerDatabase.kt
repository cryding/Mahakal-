package com.example.backend.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.backend.database.dao.AccountDao
import com.example.backend.database.dao.AuditLogDao
import com.example.backend.database.dao.DeviceSessionDao
import com.example.backend.database.dao.GameDao
import com.example.backend.database.dao.NotificationDao
import com.example.backend.database.dao.NotificationPreferenceDao
import com.example.backend.database.dao.OutboxDao
import com.example.backend.database.dao.RateLimitDao
import com.example.backend.database.dao.SecurityEventDao
import com.example.backend.database.dao.SessionDao
import com.example.backend.database.dao.WalletDao
import com.example.backend.database.dao.WalletTransactionDao
import com.example.backend.database.entity.AccountEntity
import com.example.backend.database.entity.AuditLogEntity
import com.example.backend.database.entity.DeviceSessionEntity
import com.example.backend.database.entity.GameEntity
import com.example.backend.database.entity.GameEntryEntity
import com.example.backend.database.entity.GameEventEntity
import com.example.backend.database.entity.GameOptionEntity
import com.example.backend.database.entity.GameProcessingEntity
import com.example.backend.database.entity.GameResultEntity
import com.example.backend.database.entity.NotificationEntity
import com.example.backend.database.entity.NotificationPreferenceEntity
import com.example.backend.database.entity.OutboxEventEntity
import com.example.backend.database.entity.RateLimitEntity
import com.example.backend.database.entity.SecurityEventEntity
import com.example.backend.database.entity.SessionEntity
import com.example.backend.database.entity.WalletEntity
import com.example.backend.database.entity.WalletTransactionEntity

@Database(
    entities = [
        AccountEntity::class,
        SessionEntity::class,
        AuditLogEntity::class,
        RateLimitEntity::class,
        WalletEntity::class,
        WalletTransactionEntity::class,
        GameEntity::class,
        GameOptionEntity::class,
        GameEntryEntity::class,
        GameResultEntity::class,
        GameEventEntity::class,
        GameProcessingEntity::class,
        NotificationEntity::class,
        NotificationPreferenceEntity::class,
        OutboxEventEntity::class,
        DeviceSessionEntity::class,
        SecurityEventEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class MahakalServerDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun sessionDao(): SessionDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun rateLimitDao(): RateLimitDao
    abstract fun walletDao(): WalletDao
    abstract fun walletTransactionDao(): WalletTransactionDao
    abstract fun gameDao(): GameDao
    abstract fun notificationDao(): NotificationDao
    abstract fun notificationPreferenceDao(): NotificationPreferenceDao
    abstract fun outboxDao(): OutboxDao
    abstract fun deviceSessionDao(): DeviceSessionDao
    abstract fun securityEventDao(): SecurityEventDao

    companion object {
        @Volatile
        private var INSTANCE: MahakalServerDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE accounts ADD COLUMN notes TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_accounts_status ON accounts(status)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_accounts_created_at ON accounts(created_at)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_accounts_parent_id_status ON accounts(parent_id, status)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_accounts_parent_id_created_at ON accounts(parent_id, created_at)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `wallets` (
                        `wallet_id` TEXT NOT NULL,
                        `owner_id` TEXT NOT NULL,
                        `owner_role` TEXT NOT NULL,
                        `balance` INTEGER NOT NULL,
                        `currency_type` TEXT NOT NULL,
                        `version` INTEGER NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        PRIMARY KEY(`wallet_id`)
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_wallets_owner_id` ON `wallets` (`owner_id`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_wallets_owner_id_owner_role` ON `wallets` (`owner_id`, `owner_role`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `wallet_transactions` (
                        `transaction_id` TEXT NOT NULL,
                        `idempotency_key` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `actor_id` TEXT NOT NULL,
                        `actor_role` TEXT NOT NULL,
                        `source_wallet_id` TEXT,
                        `destination_wallet_id` TEXT,
                        `amount` INTEGER NOT NULL,
                        `balance_before_source` INTEGER,
                        `balance_after_source` INTEGER,
                        `balance_before_destination` INTEGER,
                        `balance_after_destination` INTEGER,
                        `transaction_type` TEXT NOT NULL,
                        `reason` TEXT NOT NULL,
                        `reference_id` TEXT,
                        `status` TEXT NOT NULL,
                        `metadata_json` TEXT NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        PRIMARY KEY(`transaction_id`)
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_wallet_transactions_idempotency_key` ON `wallet_transactions` (`idempotency_key`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_wallet_transactions_actor_id` ON `wallet_transactions` (`actor_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_wallet_transactions_source_wallet_id` ON `wallet_transactions` (`source_wallet_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_wallet_transactions_destination_wallet_id` ON `wallet_transactions` (`destination_wallet_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_wallet_transactions_timestamp` ON `wallet_transactions` (`timestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_wallet_transactions_transaction_type` ON `wallet_transactions` (`transaction_type`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_wallet_transactions_status` ON `wallet_transactions` (`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_wallet_transactions_reference_id` ON `wallet_transactions` (`reference_id`)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `games` (
                        `game_id` TEXT NOT NULL,
                        `game_type` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `start_time` INTEGER NOT NULL,
                        `entry_deadline` INTEGER NOT NULL,
                        `result_time` INTEGER NOT NULL,
                        `min_coins` INTEGER NOT NULL,
                        `max_coins` INTEGER NOT NULL,
                        `reward_multiplier` REAL NOT NULL,
                        `created_by` TEXT NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        `version` INTEGER NOT NULL,
                        PRIMARY KEY(`game_id`)
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_games_status` ON `games` (`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_games_entry_deadline` ON `games` (`entry_deadline`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_games_created_at` ON `games` (`created_at`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `game_options` (
                        `option_id` TEXT NOT NULL,
                        `game_id` TEXT NOT NULL,
                        `option_code` TEXT NOT NULL,
                        `display_name` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `metadata_json` TEXT NOT NULL,
                        PRIMARY KEY(`option_id`)
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_game_options_game_id` ON `game_options` (`game_id`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_game_options_game_id_option_code` ON `game_options` (`game_id`, `option_code`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `game_entries` (
                        `entry_id` TEXT NOT NULL,
                        `game_id` TEXT NOT NULL,
                        `user_id` TEXT NOT NULL,
                        `selected_option_id` TEXT NOT NULL,
                        `virtual_coin_amount` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `idempotency_key` TEXT NOT NULL,
                        `deduction_transaction_id` TEXT NOT NULL,
                        `reward_transaction_id` TEXT,
                        `reward_amount` INTEGER,
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        PRIMARY KEY(`entry_id`)
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_game_entries_game_id` ON `game_entries` (`game_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_game_entries_user_id` ON `game_entries` (`user_id`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_game_entries_idempotency_key` ON `game_entries` (`idempotency_key`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_game_entries_status` ON `game_entries` (`status`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `game_results` (
                        `result_id` TEXT NOT NULL,
                        `game_id` TEXT NOT NULL,
                        `winning_option_id` TEXT NOT NULL,
                        `result_status` TEXT NOT NULL,
                        `finalized_by` TEXT NOT NULL,
                        `finalized_at` INTEGER NOT NULL,
                        `result_version` INTEGER NOT NULL,
                        `metadata_json` TEXT NOT NULL,
                        PRIMARY KEY(`result_id`)
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_game_results_game_id` ON `game_results` (`game_id`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `game_events` (
                        `event_id` TEXT NOT NULL,
                        `game_id` TEXT NOT NULL,
                        `event_type` TEXT NOT NULL,
                        `actor_id` TEXT NOT NULL,
                        `actor_role` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `metadata_json` TEXT NOT NULL,
                        `correlation_id` TEXT,
                        PRIMARY KEY(`event_id`)
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_game_events_game_id` ON `game_events` (`game_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_game_events_timestamp` ON `game_events` (`timestamp`)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `game_processings` (
                        `processing_id` TEXT NOT NULL,
                        `game_id` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `started_at` INTEGER NOT NULL,
                        `completed_at` INTEGER,
                        `attempt_count` INTEGER NOT NULL,
                        `last_error_code` TEXT,
                        `last_error_message` TEXT,
                        `correlation_id` TEXT,
                        `total_entries` INTEGER NOT NULL,
                        `processed_entries` INTEGER NOT NULL,
                        `failed_entries` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        PRIMARY KEY(`processing_id`)
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_game_processings_game_id` ON `game_processings` (`game_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_game_processings_status` ON `game_processings` (`status`)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `notifications` (
                        `notification_id` TEXT NOT NULL,
                        `recipient_id` TEXT NOT NULL,
                        `recipient_role` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `body` TEXT NOT NULL,
                        `severity` TEXT NOT NULL,
                        `reference_type` TEXT,
                        `reference_id` TEXT,
                        `status` TEXT NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        `read_at` INTEGER,
                        `expires_at` INTEGER,
                        `metadata_json` TEXT NOT NULL,
                        `correlation_id` TEXT,
                        PRIMARY KEY(`notification_id`)
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notifications_recipient_id_created_at` ON `notifications` (`recipient_id`, `created_at`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notifications_recipient_id_status` ON `notifications` (`recipient_id`, `status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notifications_correlation_id_recipient_id` ON `notifications` (`correlation_id`, `recipient_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notifications_type` ON `notifications` (`type`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `notification_preferences` (
                        `user_id` TEXT NOT NULL,
                        `game_notifications_enabled` INTEGER NOT NULL DEFAULT 1,
                        `result_notifications_enabled` INTEGER NOT NULL DEFAULT 1,
                        `wallet_notifications_enabled` INTEGER NOT NULL DEFAULT 1,
                        `security_notifications_enabled` INTEGER NOT NULL DEFAULT 1,
                        `operational_notifications_enabled` INTEGER NOT NULL DEFAULT 1,
                        `push_enabled` INTEGER NOT NULL DEFAULT 1,
                        `updated_at` INTEGER NOT NULL,
                        PRIMARY KEY(`user_id`)
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `outbox_events` (
                        `event_id` TEXT NOT NULL,
                        `event_type` TEXT NOT NULL,
                        `aggregate_type` TEXT NOT NULL,
                        `aggregate_id` TEXT NOT NULL,
                        `payload` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `attempt_count` INTEGER NOT NULL DEFAULT 0,
                        `available_at` INTEGER NOT NULL,
                        `processed_at` INTEGER,
                        `last_error_code` TEXT,
                        `correlation_id` TEXT,
                        `created_at` INTEGER NOT NULL,
                        PRIMARY KEY(`event_id`)
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_outbox_events_status_available_at` ON `outbox_events` (`status`, `available_at`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_outbox_events_created_at` ON `outbox_events` (`created_at`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `device_sessions` (
                        `device_id` TEXT NOT NULL,
                        `user_id` TEXT NOT NULL,
                        `push_token` TEXT NOT NULL,
                        `platform` TEXT NOT NULL,
                        `app_version` TEXT NOT NULL,
                        `last_seen_at` INTEGER NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        `revoked_at` INTEGER,
                        PRIMARY KEY(`device_id`, `user_id`)
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_device_sessions_user_id` ON `device_sessions` (`user_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_device_sessions_push_token` ON `device_sessions` (`push_token`)")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `security_events` (
                        `id` TEXT NOT NULL,
                        `event_type` TEXT NOT NULL,
                        `severity` TEXT NOT NULL,
                        `actor_id` TEXT,
                        `actor_role` TEXT,
                        `target_id` TEXT,
                        `target_type` TEXT,
                        `ip_address` TEXT,
                        `user_agent` TEXT,
                        `description` TEXT NOT NULL,
                        `correlation_id` TEXT NOT NULL,
                        `metadata_json` TEXT NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_security_events_event_type` ON `security_events` (`event_type`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_security_events_severity` ON `security_events` (`severity`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_security_events_actor_id` ON `security_events` (`actor_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_security_events_created_at` ON `security_events` (`created_at`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_security_events_correlation_id` ON `security_events` (`correlation_id`)")
            }
        }

        fun getInstance(context: Context): MahakalServerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MahakalServerDatabase::class.java,
                    "mahakal_server_ledger.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                 .fallbackToDestructiveMigration(false)
                 .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
