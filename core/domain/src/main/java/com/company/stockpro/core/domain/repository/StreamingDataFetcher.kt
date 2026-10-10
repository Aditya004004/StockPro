package com.company.stockpro.core.domain.repository

import com.company.stockpro.core.domain.model.Item
import com.company.stockpro.core.domain.model.StockMovement
import kotlinx.coroutines.flow.Flow

/**
 * Paged streaming data fetcher that queries Room in small chunks (default 500 records)
 * to maintain strict O(1) memory overhead during large dataset extractions (100,000+ items).
 */
interface StreamingDataFetcher {

    /**
     * Streams items in sequential batches.
     * Direct emitted Flow emits pages of [pageSize] records until EOF.
     */
    fun streamItems(
        pageSize: Int = 500,
        plantId: String? = null,
    ): Flow<List<Item>>

    /**
     * Streams stock ledger movements in sequential batches, ordered by created_at DESC.
     * Optional [startMillis] and [endMillis] filter by date range.
     * Optional [plantId] isolates movements for a specific plant/warehouse.
     */
    fun streamLedger(
        pageSize: Int = 500,
        startMillis: Long? = null,
        endMillis: Long? = null,
        plantId: String? = null,
    ): Flow<List<StockMovement>>

    /**
     * Streams items currently at or below reorder level in sequential batches.
     * Optional [plantId] isolates items for a specific facility.
     */
    fun streamLowStockItems(
        pageSize: Int = 500,
        plantId: String? = null,
    ): Flow<List<Item>>

    /** Total count of active items in the catalogue. */
    suspend fun getItemCount(plantId: String? = null): Int

    /** Total count of ledger movements matching the optional date window. */
    suspend fun getLedgerCount(
        startMillis: Long? = null,
        endMillis: Long? = null,
        plantId: String? = null,
    ): Int

    /** Total count of items needing replenishment. */
    suspend fun getLowStockCount(plantId: String? = null): Int
}
