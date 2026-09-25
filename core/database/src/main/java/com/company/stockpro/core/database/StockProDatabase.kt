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
}

