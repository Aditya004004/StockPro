package com.company.stockpro.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for cached notifications.
 * Drives unread badge counts and notification center (prd.md §3.5, §4.1 H11).
 */
@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "plant_id")
    val plantId: String,
    val type: String,
    val title: String,
    val body: String,
    @ColumnInfo(name = "entity_ref")
    val entityRef: String? = null,
    @ColumnInfo(name = "is_read")
    val isRead: Boolean = false,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = 0L,
)
