package com.company.stockpro.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a plant team member (architecture.md §4.1 / prd.md §4.1 A6).
 */
@Entity(tableName = "team_members")
data class TeamMemberEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "display_name")
    val displayName: String,
    val email: String,
    val role: String,
    @ColumnInfo(name = "plant_id")
    val plantId: String,
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
    @ColumnInfo(name = "last_active_at")
    val lastActiveAt: Long = 0L,
)
