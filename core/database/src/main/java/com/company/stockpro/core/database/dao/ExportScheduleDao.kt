package com.company.stockpro.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.company.stockpro.core.database.entity.ExportScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExportScheduleDao {

    @Query("SELECT * FROM export_schedules ORDER BY rowid ASC")
    fun observeAll(): Flow<List<ExportScheduleEntity>>

    @Query("SELECT * FROM export_schedules WHERE is_enabled = 1")
    suspend fun getActiveSchedules(): List<ExportScheduleEntity>

    @Query("SELECT * FROM export_schedules WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ExportScheduleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ExportScheduleEntity)

    @Query("UPDATE export_schedules SET is_enabled = :isEnabled WHERE id = :id")
    suspend fun toggleEnabled(id: String, isEnabled: Boolean)

    @Query("UPDATE export_schedules SET last_run_timestamp = :timestamp WHERE id = :id")
    suspend fun updateLastRun(id: String, timestamp: Long)

    @Query("DELETE FROM export_schedules WHERE id = :id")
    suspend fun deleteById(id: String)
}
