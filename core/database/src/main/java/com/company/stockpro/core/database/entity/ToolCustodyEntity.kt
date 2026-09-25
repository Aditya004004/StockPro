package com.company.stockpro.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity tracking custody and calibration lifecycle for tools (prd.md §7 Option b).
 */
@Entity(tableName = "tool_custody")
data class ToolCustodyEntity(
    @PrimaryKey
    @ColumnInfo(name = "item_id")
    val itemId: String,
    val status: String,
    @ColumnInfo(name = "operator_name")
    val operatorName: String? = null,
    @ColumnInfo(name = "issued_at")
    val issuedAt: Long? = null,
    @ColumnInfo(name = "calibration_interval_days")
    val calibrationIntervalDays: Int = 180,
    @ColumnInfo(name = "last_calibrated_at")
    val lastCalibratedAt: Long? = null,
    @ColumnInfo(name = "next_calibration_due_at")
    val nextCalibrationDueAt: Long? = null,
    val notes: String? = null,
)
