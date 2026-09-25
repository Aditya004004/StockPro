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
    @Query("SELECT * FROM stock_ledger ORDER BY created_at DESC LIMIT :limit")
    fun observeRecent(limit: Int = 10): Flow<List<StockLedgerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: StockLedgerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<StockLedgerEntity>)

    /** Remove the optimistic (pending) ledger entry by idempotency key. */
    @Query("DELETE FROM stock_ledger WHERE idempotency_key = :idempotencyKey AND is_pending = 1")
    suspend fun removePendingByKey(idempotencyKey: String)
}
