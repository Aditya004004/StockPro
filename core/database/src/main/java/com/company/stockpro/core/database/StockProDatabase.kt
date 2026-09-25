package com.company.stockpro.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.company.stockpro.core.database.dao.ItemDao
import com.company.stockpro.core.database.dao.LedgerDao
import com.company.stockpro.core.database.dao.NotificationDao
import com.company.stockpro.core.database.dao.OutboxDao
import com.company.stockpro.core.database.dao.TeamMemberDao
import com.company.stockpro.core.database.dao.ToolCustodyDao
import com.company.stockpro.core.database.entity.ItemEntity
import com.company.stockpro.core.database.entity.ItemFtsEntity
import com.company.stockpro.core.database.entity.NotificationEntity
import com.company.stockpro.core.database.entity.OutboxEntity
import com.company.stockpro.core.database.entity.StockLedgerEntity
import com.company.stockpro.core.database.entity.TeamMemberEntity
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
    ],
    version = 2,
    exportSchema = true,
)
abstract class StockProDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun ledgerDao(): LedgerDao
    abstract fun outboxDao(): OutboxDao
    abstract fun notificationDao(): NotificationDao
    abstract fun toolCustodyDao(): ToolCustodyDao
    abstract fun teamMemberDao(): TeamMemberDao

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

        fun getInstance(context: android.content.Context): StockProDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    StockProDatabase::class.java,
                    "stockpro.db",
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

