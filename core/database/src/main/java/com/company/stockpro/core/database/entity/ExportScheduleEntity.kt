package com.company.stockpro.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persistent Room entity storing background automated export schedules.
 */
@Entity(tableName = "export_schedules")
data class ExportScheduleEntity(
    @PrimaryKey
    val id: String,
    val dataset: String,
    val format: String,
    val frequency: String,
    @ColumnInfo(name = "hour_of_day")
    val hourOfDay: Int = 6,
    val minute: Int = 0,
    @ColumnInfo(name = "is_enabled")
    val isEnabled: Boolean = true,
    @ColumnInfo(name = "last_run_timestamp")
    val lastRunTimestamp: Long? = null,
    @ColumnInfo(name = "plant_name")
    val plantName: String = "Pune Plant 1",
    @ColumnInfo(name = "recipient_email")
    val recipientEmail: String? = null,
)
