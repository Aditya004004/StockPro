package com.company.stockpro.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.company.stockpro.core.database.entity.StockLedgerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LedgerDao {

    /** Ledger entries for a specific item, most recent first. */
    @Query("SELECT * FROM stock_ledger WHERE item_id = :itemId ORDER BY created_at DESC")
    fun observeByItem(itemId: String): Flow<List<StockLedgerEntity>>

    /** Recent plant-wide ledger entries, most recent first. */
    @Query(
        """
        SELECT * FROM stock_ledger 
        WHERE (:plantId IS NULL OR :plantId = '' OR from_location_id = :plantId OR to_location_id = :plantId)
        ORDER BY created_at DESC 
        LIMIT :limit
        """
    )
    fun observeRecent(limit: Int = 10, plantId: String? = null): Flow<List<StockLedgerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: StockLedgerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<StockLedgerEntity>)

    /** Remove the optimistic (pending) ledger entry by idempotency key. */
    @Query("DELETE FROM stock_ledger WHERE idempotency_key = :idempotencyKey AND is_pending = 1")
    suspend fun removePendingByKey(idempotencyKey: String)

    @Query(
        """
        SELECT * FROM stock_ledger 
        WHERE (:startMillis IS NULL OR created_at >= :startMillis) 
          AND (:endMillis IS NULL OR created_at <= :endMillis)
          AND (:plantId IS NULL OR :plantId = '' OR from_location_id = :plantId OR to_location_id = :plantId)
          AND id > :lastId
        ORDER BY id ASC 
        LIMIT :limit
        """
    )
    suspend fun getMovementsKeyset(
        limit: Int,
        lastId: String,
        startMillis: Long? = null,
        endMillis: Long? = null,
        plantId: String? = null,
    ): List<StockLedgerEntity>

    @Query(
        """
        SELECT * FROM stock_ledger 
        WHERE (:startMillis IS NULL OR created_at >= :startMillis) 
          AND (:endMillis IS NULL OR created_at <= :endMillis)
          AND (:plantId IS NULL OR :plantId = '' OR from_location_id = :plantId OR to_location_id = :plantId)
        ORDER BY created_at DESC 
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun getMovementsPaged(
        limit: Int,
        offset: Int,
        startMillis: Long? = null,
        endMillis: Long? = null,
        plantId: String? = null,
    ): List<StockLedgerEntity>

    @Query(
        """
        SELECT COUNT(*) FROM stock_ledger 
        WHERE (:startMillis IS NULL OR created_at >= :startMillis) 
          AND (:endMillis IS NULL OR created_at <= :endMillis)
          AND (:plantId IS NULL OR :plantId = '' OR from_location_id = :plantId OR to_location_id = :plantId)
        """
    )
    suspend fun getMovementsCount(
        startMillis: Long? = null,
        endMillis: Long? = null,
        plantId: String? = null,
    ): Int
}
