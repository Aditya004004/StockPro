package com.company.stockpro.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persistent Room entity recording audit logs of generated exports.
 */
@Entity(tableName = "export_history")
data class ExportHistoryEntity(
    @PrimaryKey
    val id: String,
    val timestamp: Long,
    val dataset: String,
    val format: String,
    @ColumnInfo(name = "record_count")
    val recordCount: Int,
    @ColumnInfo(name = "file_size_bytes")
    val fileSizeBytes: Long,
    @ColumnInfo(name = "file_path")
    val filePath: String,
    @ColumnInfo(name = "file_name")
    val fileName: String,
    @ColumnInfo(name = "sha256_checksum")
    val sha256Checksum: String,
    @ColumnInfo(name = "initiated_by")
    val initiatedBy: String,
    @ColumnInfo(name = "plant_name")
    val plantName: String,
    val status: String = "SUCCESS",
)
