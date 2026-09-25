package com.company.stockpro.core.database.entity

import androidx.room.ColumnInfo

/**
 * Room query projection for inventory summary metrics.
 */
data class InventorySummaryEntity(
    @ColumnInfo(name = "totalItems")
    val totalItems: Int = 0,
    @ColumnInfo(name = "lowStockCount")
    val lowStockCount: Int = 0,
    @ColumnInfo(name = "outOfStockCount")
    val outOfStockCount: Int = 0,
    @ColumnInfo(name = "reservedCount")
    val reservedCount: Int = 0,
    @ColumnInfo(name = "rawMaterialsCount")
    val rawMaterialsCount: Int = 0,
    @ColumnInfo(name = "finishedGoodsCount")
    val finishedGoodsCount: Int = 0,
    @ColumnInfo(name = "totalValuationPaise")
    val totalValuationPaise: Long = 0L,
    @ColumnInfo(name = "inProductionCount")
    val inProductionCount: Long = 0L,
)
