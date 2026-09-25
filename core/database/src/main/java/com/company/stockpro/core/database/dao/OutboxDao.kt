package com.company.stockpro.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.company.stockpro.core.database.entity.OutboxEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OutboxDao {

    /** Observe count of unfinished outbox entries (PENDING or POSTING). */
    @Query("SELECT COUNT(*) FROM outbox WHERE status IN ('PENDING', 'POSTING')")
    fun observePendingCount(): Flow<Int>

    /**
     * Get the next PENDING entry for a given item, ordered FIFO.
     * FIFO per item ensures balanceAfter consistency (architecture.md §6).
     */
    @Query("SELECT * FROM outbox WHERE status = 'PENDING' AND item_id = :itemId ORDER BY created_at ASC LIMIT 1")
    suspend fun nextPendingForItem(itemId: String): OutboxEntity?

    /** Get all distinct item IDs that have pending outbox entries. */
    @Query("SELECT DISTINCT item_id FROM outbox WHERE status = 'PENDING'")
    suspend fun pendingItemIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: OutboxEntity)

    @Query("UPDATE outbox SET status = :status, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, updatedAt: Long)

    @Query("UPDATE outbox SET status = :status, retry_count = retry_count + 1, error_message = :errorMessage, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateStatusWithRetry(id: Long, status: String, errorMessage: String?, updatedAt: Long)

    @Query("UPDATE outbox SET status = 'FAILED', error_message = :errorMessage, updated_at = :updatedAt WHERE id = :id")
    suspend fun markFailed(id: Long, errorMessage: String?, updatedAt: Long)

    @Query("DELETE FROM outbox WHERE status = 'DONE'")
    suspend fun cleanupDone()

    /** All failed entries for the "Needs attention" screen. */
    @Query("SELECT * FROM outbox WHERE status = 'FAILED' ORDER BY created_at DESC")
    fun observeFailedEntries(): Flow<List<OutboxEntity>>

    /** Observe count of failed outbox entries. */
    @Query("SELECT COUNT(*) FROM outbox WHERE status = 'FAILED'")
    fun observeFailedCount(): Flow<Int>

    /** Delete an outbox entry by ID (e.g. user discards failed mutation). */
    @Query("DELETE FROM outbox WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** Reset a failed entry back to PENDING for re-attempt. */
    @Query("UPDATE outbox SET status = 'PENDING', error_message = null, retry_count = 0, updated_at = :updatedAt WHERE id = :id")
    suspend fun resetToPending(id: Long, updatedAt: Long)
}
