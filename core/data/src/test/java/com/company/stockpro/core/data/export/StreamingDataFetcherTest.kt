package com.company.stockpro.core.data.export

import com.company.stockpro.core.data.repository.StreamingDataFetcherImpl
import com.company.stockpro.core.database.StockProDatabase
import com.company.stockpro.core.database.dao.ItemDao
import com.company.stockpro.core.database.dao.LedgerDao
import com.company.stockpro.core.database.entity.ItemEntity
import com.company.stockpro.core.database.entity.StockLedgerEntity
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy

class StreamingDataFetcherTest {

    private val allItemEntities = (1..12).map { i ->
        val code = "SKU-%03d".format(i)
        val name = "Item $i"
        ItemEntity(
            id = "item_%03d".format(i),
            code = code,
            codeLower = code.lowercase(),
            name = name,
            nameLower = name.lowercase(),
            itemClass = "RAW_MATERIAL",
            categoryName = "Category A",
            unitOfMeasure = "PCS",
            onHandMilli = if (i <= 4) 5000L else 50000L,
            reservedMilli = 0L,
            inProductionMilli = 0L,
            reorderLevelMilli = 10000L,
            parLevelMilli = 100000L,
            unitCostPaise = 1000L,
            locationId = if (i % 2 == 0) "plant_01" else "plant_02",
            locationCode = "LOC-$i",
            isActive = true,
        )
    }

    private val allMovementEntities = (1..7).map { i ->
        StockLedgerEntity(
            id = "mov_%03d".format(i),
            itemId = "item_%03d".format(i),
            itemCode = "SKU-%03d".format(i),
            itemName = "Item $i",
            type = "RECEIPT",
            deltaMilli = 10000L,
            balanceAfterMilli = 20000L,
            unitCostPaise = 1000L,
            reference = "REF-$i",
            actorName = "User $i",
            idempotencyKey = "key_$i",
            createdAt = 1700000000000L + (i * 1000L),
            toLocationId = if (i % 2 == 0) "plant_01" else "plant_02",
        )
    }

    private val recordedKeysetQueries = mutableListOf<String>()

    private lateinit var fakeDatabase: StockProDatabase
    private lateinit var fetcher: StreamingDataFetcherImpl

    @Before
    fun setUp() {
        recordedKeysetQueries.clear()

        val itemDaoProxy = Proxy.newProxyInstance(
            ItemDao::class.java.classLoader,
            arrayOf(ItemDao::class.java),
        ) { _, method, args ->
            when (method.name) {
                "getItemsKeyset" -> {
                    val limit = args[0] as Int
                    val lastId = args[1] as String
                    val plantId = args[2] as? String
                    recordedKeysetQueries.add("getItemsKeyset(limit=$limit, lastId=$lastId, plantId=$plantId)")

                    allItemEntities
                        .filter { it.isActive }
                        .filter { plantId.isNullOrEmpty() || it.locationId == plantId }
                        .filter { it.id > lastId }
                        .sortedBy { it.id }
                        .take(limit)
                }
                "getLowStockKeyset" -> {
                    val limit = args[0] as Int
                    val lastId = args[1] as String
                    val plantId = args[2] as? String
                    recordedKeysetQueries.add("getLowStockKeyset(limit=$limit, lastId=$lastId, plantId=$plantId)")

                    allItemEntities
                        .filter { it.isActive && it.onHandMilli <= it.reorderLevelMilli }
                        .filter { plantId.isNullOrEmpty() || it.locationId == plantId }
                        .filter { it.id > lastId }
                        .sortedBy { it.id }
                        .take(limit)
                }
                "getActiveItemsCount" -> {
                    val plantId = args.getOrNull(0) as? String
                    allItemEntities
                        .filter { it.isActive }
                        .count { plantId.isNullOrEmpty() || it.locationId == plantId }
                }
                "getLowStockCount" -> {
                    val plantId = args.getOrNull(0) as? String
                    allItemEntities
                        .filter { it.isActive && it.onHandMilli <= it.reorderLevelMilli }
                        .count { plantId.isNullOrEmpty() || it.locationId == plantId }
                }
                else -> null
            }
        } as ItemDao

        val ledgerDaoProxy = Proxy.newProxyInstance(
            LedgerDao::class.java.classLoader,
            arrayOf(LedgerDao::class.java),
        ) { _, method, args ->
            when (method.name) {
                "getMovementsKeyset" -> {
                    val limit = args[0] as Int
                    val lastId = args[1] as String
                    val plantId = args[4] as? String
                    recordedKeysetQueries.add("getMovementsKeyset(limit=$limit, lastId=$lastId, plantId=$plantId)")

                    allMovementEntities
                        .filter { plantId.isNullOrEmpty() || it.toLocationId == plantId }
                        .filter { it.id > lastId }
                        .sortedBy { it.id }
                        .take(limit)
                }
                "getMovementsCount" -> {
                    val plantId = args.getOrNull(2) as? String
                    allMovementEntities
                        .count { plantId.isNullOrEmpty() || it.toLocationId == plantId }
                }
                else -> null
            }
        } as LedgerDao

        fakeDatabase = object : StockProDatabase() {
            override fun itemDao(): ItemDao = itemDaoProxy
            override fun ledgerDao(): LedgerDao = ledgerDaoProxy
            override fun outboxDao() = throw UnsupportedOperationException()
            override fun notificationDao() = throw UnsupportedOperationException()
            override fun toolCustodyDao() = throw UnsupportedOperationException()
            override fun teamMemberDao() = throw UnsupportedOperationException()
            override fun exportHistoryDao() = throw UnsupportedOperationException()
            override fun exportScheduleDao() = throw UnsupportedOperationException()
            override fun plantDao() = throw UnsupportedOperationException()
            override fun thresholdAuditDao() = throw UnsupportedOperationException()
            override fun clearAllTables() {}
            override fun createInvalidationTracker(): androidx.room.InvalidationTracker {
                return androidx.room.InvalidationTracker(this, "items", "stock_ledger")
            }
            override fun createOpenHelper(config: androidx.room.DatabaseConfiguration) = throw UnsupportedOperationException()
        }

        fetcher = StreamingDataFetcherImpl(fakeDatabase)
    }

    @Test
    fun `streamItems executes cursor-based keyset pagination with lastId across pages`() = runTest {
        // Page size = 5, total 12 items -> should produce pages: 5, 5, 2
        val pages = fetcher.streamItems(pageSize = 5, plantId = null).toList()

        assertEquals(3, pages.size)
        assertEquals(5, pages[0].size)
        assertEquals(5, pages[1].size)
        assertEquals(2, pages[2].size)

        // Verify keyset progression
        assertEquals("item_001", pages[0].first().id)
        assertEquals("item_005", pages[0].last().id)

        assertEquals("item_006", pages[1].first().id)
        assertEquals("item_010", pages[1].last().id)

        assertEquals("item_011", pages[2].first().id)
        assertEquals("item_012", pages[2].last().id)

        // Verify query invocations tracked lastId
        assertEquals("getItemsKeyset(limit=5, lastId=, plantId=null)", recordedKeysetQueries[0])
        assertEquals("getItemsKeyset(limit=5, lastId=item_005, plantId=null)", recordedKeysetQueries[1])
        assertEquals("getItemsKeyset(limit=5, lastId=item_010, plantId=null)", recordedKeysetQueries[2])
    }

    @Test
    fun `streamItems enforces plantId multi-facility isolation`() = runTest {
        val pages = fetcher.streamItems(pageSize = 10, plantId = "plant_01").toList()

        val allRetrieved = pages.flatten()
        assertEquals(6, allRetrieved.size)
        assertTrue(allRetrieved.all { it.id.substringAfter("_").toInt() % 2 == 0 })
        assertTrue(recordedKeysetQueries.all { it.contains("plantId=plant_01") })
    }

    @Test
    fun `streamLowStockItems queries keyset cursor accurately`() = runTest {
        val pages = fetcher.streamLowStockItems(pageSize = 2, plantId = null).toList()

        val allLowStock = pages.flatten()
        assertEquals(4, allLowStock.size) // items 1, 2, 3, 4
        assertEquals("item_001", allLowStock[0].id)
        assertEquals("item_004", allLowStock[3].id)

        // Verify queries
        assertEquals("getLowStockKeyset(limit=2, lastId=, plantId=null)", recordedKeysetQueries[0])
        assertEquals("getLowStockKeyset(limit=2, lastId=item_002, plantId=null)", recordedKeysetQueries[1])
    }

    @Test
    fun `streamLedger paginates stock movements with cursor and plantId filter`() = runTest {
        val pages = fetcher.streamLedger(pageSize = 3, plantId = "plant_02").toList()

        val allMovements = pages.flatten()
        assertEquals(4, allMovements.size) // mov 1, 3, 5, 7
        assertEquals("mov_001", allMovements[0].id)
        assertEquals("mov_007", allMovements.last().id)

        assertEquals("getMovementsKeyset(limit=3, lastId=, plantId=plant_02)", recordedKeysetQueries[0])
        assertEquals("getMovementsKeyset(limit=3, lastId=mov_005, plantId=plant_02)", recordedKeysetQueries[1])
    }
}
