package com.company.stockpro.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.company.stockpro.core.database.entity.ItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {

    @Query("SELECT * FROM items WHERE is_active = 1 ORDER BY name ASC")
    fun observeActiveItems(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE is_active = 1 AND item_class = :itemClass ORDER BY name ASC")
    fun observeItemsByClassOrderByName(itemClass: String): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE is_active = 1 AND item_class = :itemClass ORDER BY code ASC")
    fun observeItemsByClassOrderByCode(itemClass: String): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE is_active = 1 AND item_class = :itemClass ORDER BY on_hand_milli DESC")
    fun observeItemsByClassOrderByQuantityDesc(itemClass: String): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE is_active = 1 AND item_class = :itemClass ORDER BY on_hand_milli ASC")
    fun observeItemsByClassOrderByQuantityAsc(itemClass: String): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE is_active = 1 AND item_class = :itemClass ORDER BY last_movement_at DESC")
    fun observeItemsByClassOrderByLastMoved(itemClass: String): Flow<List<ItemEntity>>

    @Query(
        """
        SELECT 
            COUNT(*) as totalItems,
            COALESCE(SUM(CASE WHEN on_hand_milli <= reorder_level_milli AND on_hand_milli > 0 THEN 1 ELSE 0 END), 0) as lowStockCount,
            COALESCE(SUM(CASE WHEN on_hand_milli <= 0 THEN 1 ELSE 0 END), 0) as outOfStockCount,
            COALESCE(SUM(CASE WHEN reserved_milli > 0 THEN 1 ELSE 0 END), 0) as reservedCount,
            COALESCE(SUM(CASE WHEN item_class = 'RAW_MATERIAL' THEN 1 ELSE 0 END), 0) as rawMaterialsCount,
            COALESCE(SUM(CASE WHEN item_class = 'FINISHED_GOOD' THEN 1 ELSE 0 END), 0) as finishedGoodsCount,
            COALESCE(SUM((on_hand_milli * unit_cost_paise) / 1000), 0) as totalValuationPaise,
            COALESCE(SUM(in_production_milli / 1000), 0) as inProductionCount
        FROM items
        WHERE is_active = 1
        """
    )
    fun observeSummary(): Flow<com.company.stockpro.core.database.entity.InventorySummaryEntity>

    @Query(
        """
        SELECT * FROM items 
        WHERE is_active = 1 AND on_hand_milli <= reorder_level_milli 
        ORDER BY on_hand_milli ASC 
        LIMIT :limit
        """
    )
    fun observeTopLowStock(limit: Int): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE id = :itemId")
    fun observeItem(itemId: String): Flow<ItemEntity?>

    @Query("SELECT * FROM items WHERE id = :itemId")
    suspend fun getItem(itemId: String): ItemEntity?

    @Query("SELECT * FROM items WHERE code = :code COLLATE NOCASE LIMIT 1")
    suspend fun getItemByCode(code: String): ItemEntity?

    /**
     * FTS4 search across name, code, and category_name.
     * Room joins the FTS virtual table with the content table automatically.
     */
    @Query(
        """
        SELECT items.* FROM items
        JOIN items_fts ON items.rowid = items_fts.rowid
        WHERE items_fts MATCH :query
        AND items.is_active = 1
        ORDER BY items.name ASC
        """
    )
    fun searchItems(query: String): Flow<List<ItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ItemEntity>)

    @Query("UPDATE items SET on_hand_milli = on_hand_milli + :deltaMilli, last_movement_at = :timestamp WHERE id = :itemId")
    suspend fun applyOptimisticDelta(itemId: String, deltaMilli: Long, timestamp: Long)

    @Query("UPDATE items SET on_hand_milli = :balanceMilli, last_movement_at = :timestamp WHERE id = :itemId")
    suspend fun reconcileBalance(itemId: String, balanceMilli: Long, timestamp: Long)
}
