package com.company.stockpro.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.company.stockpro.core.database.entity.ToolCustodyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ToolCustodyDao {

    @Query("SELECT * FROM tool_custody WHERE item_id = :itemId")
    fun observe(itemId: String): Flow<ToolCustodyEntity?>

    @Query("SELECT * FROM tool_custody WHERE item_id = :itemId")
    suspend fun get(itemId: String): ToolCustodyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ToolCustodyEntity)

    @Query(
        """
        UPDATE tool_custody 
        SET status = :status, operator_name = :operatorName, issued_at = :issuedAt, notes = :notes 
        WHERE item_id = :itemId
        """
    )
    suspend fun updateCustody(
        itemId: String,
        status: String,
        operatorName: String?,
        issuedAt: Long?,
        notes: String?,
    )

    @Query(
        """
        UPDATE tool_custody 
        SET last_calibrated_at = :calibratedAt, next_calibration_due_at = :nextDueAt, notes = :notes 
        WHERE item_id = :itemId
        """
    )
    suspend fun updateCalibration(
        itemId: String,
        calibratedAt: Long,
        nextDueAt: Long,
        notes: String?,
    )
}
