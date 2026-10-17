package com.company.stockpro.core.data.sync

import android.util.Log
import com.company.stockpro.core.database.StockProDatabase
import com.company.stockpro.core.database.entity.ItemEntity
import com.company.stockpro.core.domain.model.SyncStatus
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.tasks.await
import java.util.concurrent.atomic.AtomicLong

/**
 * Manages reactive read sync from Firestore to Room (architecture.md §7.1).
 *
 * Provides:
 * - Delta pull (`where updatedAt > cursor`)
 * - Full initial or forced resync
 * - App-wide composite `SyncStatus` stream combining worker queue and network state
 */
class InventorySyncManager(
    private val database: StockProDatabase,
    private val firestore: FirebaseFirestore,
) {
    companion object {
        private const val TAG = "InventorySyncManager"
    }

    private val itemDao get() = database.itemDao()
    private val outboxDao get() = database.outboxDao()

    private val isSyncing = MutableStateFlow(false)
    private val lastSyncedTime = AtomicLong(0L)
    private val syncMutex = kotlinx.coroutines.sync.Mutex()

    /**
     * Combined reactive sync status stream (architecture.md §7.2).
     *
     * Precedence:
     * 1. NeedsAttention (if any failed outbox items)
     * 2. Syncing (if actively pulling or posting)
     * 3. Pending (if pending mutations queued)
     * 4. Synced (idle)
     */
    fun observeSyncStatus(): Flow<SyncStatus> =
        combine(
            isSyncing,
            outboxDao.observePendingCount(),
            outboxDao.observeFailedCount(),
        ) { syncing, pendingCount, failedCount ->
            when {
                failedCount > 0 -> SyncStatus.NeedsAttention(failedCount)
                syncing -> SyncStatus.Syncing
                pendingCount > 0 -> SyncStatus.Pending(pendingCount)
                else -> SyncStatus.Synced(if (lastSyncedTime.get() == 0L) System.currentTimeMillis() else lastSyncedTime.get())
            }
        }

    /**
     * Pulls changed documents since the last cursor timestamp (architecture.md §7.1).
     */
    suspend fun pullDelta(orgId: String, plantId: String): Result<Int> {
        if (!syncMutex.tryLock()) {
            return Result.success(0)
        }
        isSyncing.value = true
        return try {
            var totalCount = 0
            var hasMore = true
            val pageSize = 100L

            while (hasMore) {
                val cursor = lastSyncedTime.get()
                val querySnapshot = firestore
                    .collection("organizations").document(orgId)
                    .collection("plants").document(plantId)
                    .collection("items")
                    .whereGreaterThan("updatedAt", cursor)
                    .orderBy("updatedAt", Query.Direction.ASCENDING)
                    .limit(pageSize)
                    .get()
                    .await()

                val entities = querySnapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    ItemEntity(
                        id = doc.id,
                        code = data["code"] as? String ?: "",
                        codeLower = data["codeLower"] as? String ?: "",
                        name = data["name"] as? String ?: "",
                        nameLower = data["nameLower"] as? String ?: "",
                        itemClass = data["itemClass"] as? String ?: "UNKNOWN",
                        categoryId = data["categoryId"] as? String ?: "",
                        categoryName = data["categoryName"] as? String ?: "",
                        unitOfMeasure = data["unitOfMeasure"] as? String ?: "PCS",
                        onHandMilli = (data["onHandMilli"] as? Number)?.toLong() ?: 0L,
                        reservedMilli = (data["reservedMilli"] as? Number)?.toLong() ?: 0L,
                        inProductionMilli = (data["inProductionMilli"] as? Number)?.toLong() ?: 0L,
                        reorderLevelMilli = (data["reorderLevelMilli"] as? Number)?.toLong() ?: 0L,
                        parLevelMilli = (data["parLevelMilli"] as? Number)?.toLong() ?: 0L,
                        unitCostPaise = (data["unitCostPaise"] as? Number)?.toLong() ?: 0L,
                        locationId = data["locationId"] as? String ?: "",
                        locationCode = data["locationCode"] as? String ?: "",
                        barcode = data["barcode"] as? String,
                        imagePath = data["imagePath"] as? String,
                        isActive = data["isActive"] as? Boolean ?: true,
                        createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0L,
                        updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 0L,
                        lastMovementAt = (data["lastMovementAt"] as? Number)?.toLong() ?: 0L,
                    )
                }

                if (entities.isNotEmpty()) {
                    itemDao.upsertAll(entities)
                    val maxUpdated = entities.maxOf { it.updatedAt }
                    lastSyncedTime.set(maxOf(lastSyncedTime.get(), maxUpdated))
                    totalCount += entities.size
                }

                hasMore = querySnapshot.documents.size >= pageSize.toInt()
            }

            Result.success(totalCount)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e(TAG, "Failed to pull delta sync: ${e.message}", e)
            Result.failure(e)
        } finally {
            isSyncing.value = false
            syncMutex.unlock()
        }
    }

    /**
     * Performs a full resync on first login, plant switch, or on demand (architecture.md §7.1).
     */
    suspend fun fullResync(orgId: String, plantId: String): Result<Int> {
        if (!syncMutex.tryLock()) {
            return Result.success(0)
        }
        isSyncing.value = true
        return try {
            val querySnapshot = firestore
                .collection("organizations").document(orgId)
                .collection("plants").document(plantId)
                .collection("items")
                .whereEqualTo("isActive", true)
                .get()
                .await()

            val entities = querySnapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                ItemEntity(
                    id = doc.id,
                    code = data["code"] as? String ?: "",
                    codeLower = data["codeLower"] as? String ?: "",
                    name = data["name"] as? String ?: "",
                    nameLower = data["nameLower"] as? String ?: "",
                    itemClass = data["itemClass"] as? String ?: "UNKNOWN",
                    categoryId = data["categoryId"] as? String ?: "",
                    categoryName = data["categoryName"] as? String ?: "",
                    unitOfMeasure = data["unitOfMeasure"] as? String ?: "PCS",
                    onHandMilli = (data["onHandMilli"] as? Number)?.toLong() ?: 0L,
                    reservedMilli = (data["reservedMilli"] as? Number)?.toLong() ?: 0L,
                    inProductionMilli = (data["inProductionMilli"] as? Number)?.toLong() ?: 0L,
                    reorderLevelMilli = (data["reorderLevelMilli"] as? Number)?.toLong() ?: 0L,
                    parLevelMilli = (data["parLevelMilli"] as? Number)?.toLong() ?: 0L,
                    unitCostPaise = (data["unitCostPaise"] as? Number)?.toLong() ?: 0L,
                    locationId = data["locationId"] as? String ?: "",
                    locationCode = data["locationCode"] as? String ?: "",
                    barcode = data["barcode"] as? String,
                    imagePath = data["imagePath"] as? String,
                    isActive = data["isActive"] as? Boolean ?: true,
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0L,
                    updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 0L,
                    lastMovementAt = (data["lastMovementAt"] as? Number)?.toLong() ?: 0L,
                )
            }

            itemDao.upsertAll(entities)
            lastSyncedTime.set(System.currentTimeMillis())
            Result.success(entities.size)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e(TAG, "Full resync failed: ${e.message}", e)
            Result.failure(e)
        } finally {
            isSyncing.value = false
            syncMutex.unlock()
        }
    }
}
