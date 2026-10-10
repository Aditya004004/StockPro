package com.company.stockpro.core.data.repository

import com.company.stockpro.core.database.StockProDatabase
import com.company.stockpro.core.domain.model.Item
import com.company.stockpro.core.domain.model.StockMovement
import com.company.stockpro.core.domain.repository.StreamingDataFetcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Paged streaming implementation of [StreamingDataFetcher].
 * Ensures memory-safe batch fetching of catalogue items and stock movements
 * in sequential pages (default 500 records) to prevent OutOfMemory crashes on large datasets.
 */
class StreamingDataFetcherImpl(
    private val database: StockProDatabase,
) : StreamingDataFetcher {

    private val itemDao get() = database.itemDao()
    private val ledgerDao get() = database.ledgerDao()

    override fun streamItems(pageSize: Int, plantId: String?): Flow<List<Item>> = flow {
        var lastId = ""
        while (true) {
            val entities = itemDao.getItemsKeyset(limit = pageSize, lastId = lastId, plantId = plantId)
            if (entities.isEmpty()) break
            emit(entities.map { it.toDomain() })
            if (entities.size < pageSize) break
            lastId = entities.last().id
        }
    }.flowOn(Dispatchers.IO)

    override fun streamLedger(
        pageSize: Int,
        startMillis: Long?,
        endMillis: Long?,
        plantId: String?,
    ): Flow<List<StockMovement>> = flow {
        var lastId = ""
        while (true) {
            val entities = ledgerDao.getMovementsKeyset(
                limit = pageSize,
                lastId = lastId,
                startMillis = startMillis,
                endMillis = endMillis,
                plantId = plantId,
            )
            if (entities.isEmpty()) break
            emit(entities.map { it.toDomain() })
            if (entities.size < pageSize) break
            lastId = entities.last().id
        }
    }.flowOn(Dispatchers.IO)

    override fun streamLowStockItems(pageSize: Int, plantId: String?): Flow<List<Item>> = flow {
        var lastId = ""
        while (true) {
            val entities = itemDao.getLowStockKeyset(limit = pageSize, lastId = lastId, plantId = plantId)
            if (entities.isEmpty()) break
            emit(entities.map { it.toDomain() })
            if (entities.size < pageSize) break
            lastId = entities.last().id
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getItemCount(plantId: String?): Int = withContext(Dispatchers.IO) {
        itemDao.getActiveItemsCount(plantId)
    }

    override suspend fun getLedgerCount(startMillis: Long?, endMillis: Long?, plantId: String?): Int = withContext(Dispatchers.IO) {
        ledgerDao.getMovementsCount(startMillis, endMillis, plantId)
    }

    override suspend fun getLowStockCount(plantId: String?): Int = withContext(Dispatchers.IO) {
        itemDao.getLowStockCount(plantId)
    }
}
