package com.company.stockpro.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for cached item catalogue.
 * Mirrors Firestore `items/{itemId}` (architecture.md §4.2).
 *
 * All quantities are Long milli-units. All money values are Long paise.
 * Never floating-point (rules.md §4).
 */
@Entity(
    tableName = "items",
    indices = [
        Index(value = ["is_active", "item_class", "name"]),
        Index(value = ["is_active", "location_id"]),
        Index(value = ["is_active", "on_hand_milli"]),
        Index(value = ["is_active", "last_movement_at"]),
        Index(value = ["code"]),
    ],
)
data class ItemEntity(
    @PrimaryKey
    val id: String,
    val code: String,
    @ColumnInfo(name = "code_lower")
    val codeLower: String,
    val name: String,
    @ColumnInfo(name = "name_lower")
    val nameLower: String,
    @ColumnInfo(name = "item_class")
    val itemClass: String,
    @ColumnInfo(name = "category_id")
    val categoryId: String = "",
    @ColumnInfo(name = "category_name")
    val categoryName: String = "",
    @ColumnInfo(name = "unit_of_measure")
    val unitOfMeasure: String = "PCS",
    @ColumnInfo(name = "on_hand_milli")
    val onHandMilli: Long = 0L,
    @ColumnInfo(name = "reserved_milli")
    val reservedMilli: Long = 0L,
    @ColumnInfo(name = "in_production_milli")
    val inProductionMilli: Long = 0L,
    @ColumnInfo(name = "reorder_level_milli")
    val reorderLevelMilli: Long = 0L,
    @ColumnInfo(name = "par_level_milli")
    val parLevelMilli: Long = 0L,
    @ColumnInfo(name = "unit_cost_paise")
    val unitCostPaise: Long = 0L,
    @ColumnInfo(name = "location_id")
    val locationId: String = "",
    @ColumnInfo(name = "location_code")
    val locationCode: String = "",
    val barcode: String? = null,
    @ColumnInfo(name = "image_path")
    val imagePath: String? = null,
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = 0L,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = 0L,
    @ColumnInfo(name = "last_movement_at")
    val lastMovementAt: Long = 0L,
)
