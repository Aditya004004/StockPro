package com.company.stockpro.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.company.stockpro.core.database.entity.ThresholdAuditEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ThresholdAuditDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(audit: ThresholdAuditEntity)

    @Query("SELECT * FROM threshold_audits WHERE item_id = :itemId ORDER BY timestamp DESC")
    fun observeAuditsByItem(itemId: String): Flow<List<ThresholdAuditEntity>>

    @Query("SELECT * FROM threshold_audits WHERE plant_id = :plantId ORDER BY timestamp DESC")
    fun observeAuditsByPlant(plantId: String): Flow<List<ThresholdAuditEntity>>

    @Query("SELECT * FROM threshold_audits ORDER BY timestamp DESC")
    fun observeAllAudits(): Flow<List<ThresholdAuditEntity>>
}
