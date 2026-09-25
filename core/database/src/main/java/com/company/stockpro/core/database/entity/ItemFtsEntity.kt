package com.company.stockpro.core.database.entity

import androidx.room.Entity
import androidx.room.Fts4

/**
 * FTS4 virtual table indexing item name, code, and category for
 * sub-100ms offline substring search (ADR-004).
 *
 * Room FTS4 entities must mirror the content table's searchable columns.
 * The `contentEntity` links this FTS index to [ItemEntity].
 */
@Fts4(contentEntity = ItemEntity::class)
@Entity(tableName = "items_fts")
data class ItemFtsEntity(
    val name: String,
    val code: String,
    @androidx.room.ColumnInfo(name = "category_name")
    val categoryName: String,
)
