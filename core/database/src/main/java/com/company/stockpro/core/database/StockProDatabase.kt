package com.company.stockpro.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.company.stockpro.core.database.dao.ExportHistoryDao
import com.company.stockpro.core.database.dao.ExportScheduleDao
import com.company.stockpro.core.database.dao.ItemDao
import com.company.stockpro.core.database.dao.LedgerDao
import com.company.stockpro.core.database.dao.NotificationDao
import com.company.stockpro.core.database.dao.OutboxDao
import com.company.stockpro.core.database.dao.PlantDao
import com.company.stockpro.core.database.dao.TeamMemberDao
import com.company.stockpro.core.database.dao.ThresholdAuditDao
import com.company.stockpro.core.database.dao.ToolCustodyDao
import com.company.stockpro.core.database.entity.ExportHistoryEntity
import com.company.stockpro.core.database.entity.ExportScheduleEntity
import com.company.stockpro.core.database.entity.ItemEntity
import com.company.stockpro.core.database.entity.ItemFtsEntity
import com.company.stockpro.core.database.entity.NotificationEntity
import com.company.stockpro.core.database.entity.OutboxEntity
import com.company.stockpro.core.database.entity.PlantEntity
import com.company.stockpro.core.database.entity.StockLedgerEntity
import com.company.stockpro.core.database.entity.TeamMemberEntity
import com.company.stockpro.core.database.entity.ThresholdAuditEntity
import com.company.stockpro.core.database.entity.ToolCustodyEntity

@Database(
    entities = [
        ItemEntity::class,
        ItemFtsEntity::class,
        StockLedgerEntity::class,
        OutboxEntity::class,
        NotificationEntity::class,
        ToolCustodyEntity::class,
        TeamMemberEntity::class,
        ExportHistoryEntity::class,
        ExportScheduleEntity::class,
        PlantEntity::class,
        ThresholdAuditEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
abstract class StockProDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun ledgerDao(): LedgerDao
    abstract fun outboxDao(): OutboxDao
    abstract fun notificationDao(): NotificationDao
    abstract fun toolCustodyDao(): ToolCustodyDao
    abstract fun teamMemberDao(): TeamMemberDao
    abstract fun exportHistoryDao(): ExportHistoryDao
    abstract fun exportScheduleDao(): ExportScheduleDao
    abstract fun plantDao(): PlantDao
    abstract fun thresholdAuditDao(): ThresholdAuditDao

    companion object {
        @Volatile
        private var INSTANCE: StockProDatabase? = null

        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tool_custody` (
                        `item_id` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `operator_name` TEXT,
                        `issued_at` INTEGER,
                        `calibration_interval_days` INTEGER NOT NULL,
                        `last_calibrated_at` INTEGER,
                        `next_calibration_due_at` INTEGER,
                        `notes` TEXT,
                        PRIMARY KEY(`item_id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `team_members` (
                        `id` TEXT NOT NULL,
                        `display_name` TEXT NOT NULL,
                        `email` TEXT NOT NULL,
                        `role` TEXT NOT NULL,
                        `plant_id` TEXT NOT NULL,
                        `is_active` INTEGER NOT NULL,
                        `last_active_at` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `export_history` (
                        `id` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `dataset` TEXT NOT NULL,
                        `format` TEXT NOT NULL,
                        `record_count` INTEGER NOT NULL,
                        `file_size_bytes` INTEGER NOT NULL,
                        `file_path` TEXT NOT NULL,
                        `file_name` TEXT NOT NULL,
                        `sha256_checksum` TEXT NOT NULL,
                        `initiated_by` TEXT NOT NULL,
                        `plant_name` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `export_schedules` (
                        `id` TEXT NOT NULL,
                        `dataset` TEXT NOT NULL,
                        `format` TEXT NOT NULL,
                        `frequency` TEXT NOT NULL,
                        `hour_of_day` INTEGER NOT NULL,
                        `minute` INTEGER NOT NULL,
                        `is_enabled` INTEGER NOT NULL,
                        `last_run_timestamp` INTEGER,
                        `plant_name` TEXT NOT NULL,
                        `recipient_email` TEXT,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `plants` (
                        `id` TEXT NOT NULL,
                        `code` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `location` TEXT NOT NULL,
                        `is_active` INTEGER NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `threshold_audits` (
                        `id` TEXT NOT NULL,
                        `item_id` TEXT NOT NULL,
                        `sku` TEXT NOT NULL,
                        `plant_id` TEXT NOT NULL,
                        `old_threshold_milli` INTEGER NOT NULL,
                        `new_threshold_milli` INTEGER NOT NULL,
                        `operator` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // items secondary indices
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_items_is_active_item_class_name` ON `items` (`is_active`, `item_class`, `name`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_items_is_active_location_id` ON `items` (`is_active`, `location_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_items_is_active_on_hand_milli` ON `items` (`is_active`, `on_hand_milli`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_items_is_active_last_movement_at` ON `items` (`is_active`, `last_movement_at`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_items_code` ON `items` (`code`)")

                // stock_ledger secondary indices
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_ledger_item_id_created_at` ON `stock_ledger` (`item_id`, `created_at`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_ledger_created_at` ON `stock_ledger` (`created_at`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_ledger_idempotency_key` ON `stock_ledger` (`idempotency_key`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_ledger_from_location_id` ON `stock_ledger` (`from_location_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_ledger_to_location_id` ON `stock_ledger` (`to_location_id`)")

                // outbox secondary indices
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_outbox_status_item_id_created_at` ON `outbox` (`status`, `item_id`, `created_at`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_outbox_status` ON `outbox` (`status`)")

                // notifications secondary indices
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notifications_plant_id_is_read` ON `notifications` (`plant_id`, `is_read`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notifications_plant_id_created_at` ON `notifications` (`plant_id`, `created_at`)")
            }
        }

        fun getInstance(context: android.content.Context): StockProDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    StockProDatabase::class.java,
                    "stockpro.db",
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
