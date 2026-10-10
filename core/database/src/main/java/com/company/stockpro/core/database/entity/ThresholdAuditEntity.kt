package com.company.stockpro.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Audit log record for tracking inventory low-stock threshold (reorder level) changes.
 */
@Entity(tableName = "threshold_audits")
data class ThresholdAuditEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "item_id")
    val itemId: String,
    @ColumnInfo(name = "sku")
    val sku: String,
    @ColumnInfo(name = "plant_id")
    val plantId: String,
    @ColumnInfo(name = "old_threshold_milli")
    val oldThresholdMilli: Long,
    @ColumnInfo(name = "new_threshold_milli")
    val newThresholdMilli: Long,
    @ColumnInfo(name = "operator")
    val operator: String,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),
)
