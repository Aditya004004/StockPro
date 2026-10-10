package com.company.stockpro.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.company.stockpro.core.database.entity.ExportHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExportHistoryDao {

    @Query("SELECT * FROM export_history ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<ExportHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ExportHistoryEntity)

    @Query("SELECT * FROM export_history WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ExportHistoryEntity?

    @Query("DELETE FROM export_history WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM export_history")
    suspend fun getAll(): List<ExportHistoryEntity>

    @Query("DELETE FROM export_history")
    suspend fun clearAll()
}
