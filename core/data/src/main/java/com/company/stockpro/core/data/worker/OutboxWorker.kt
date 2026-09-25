package com.company.stockpro.core.data.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.company.stockpro.core.database.StockProDatabase
import com.company.stockpro.core.database.entity.OutboxEntity
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

/**
 * WorkManager worker that drains the outbox FIFO per item.
 *
 * Behaviour per architecture.md §6:
 * - 2xx → replace optimistic ledger entry with server entry,
 *         reconcile item balance, mark outbox DONE.
 * - 4xx → roll back the optimistic change, mark outbox FAILED,
 *         surface in "Needs attention".
 * - 5xx / network → leave PENDING with bumped retryCount,
 *         exponential backoff via WorkManager retry.
 *
 * Ordering: FIFO per item. Different items drain in parallel
 * because their balanceAfter values are independent.
 */
class OutboxWorker(
    appContext: Context,
    params: WorkerParameters,
    private val database: StockProDatabase,
    private val functions: FirebaseFunctions,
) : CoroutineWorker(appContext, params) {

    companion object {
        const val TAG = "OutboxWorker"
        private const val MAX_RETRIES = 10
    }

    override suspend fun doWork(): Result {
        val outboxDao = database.outboxDao()
        val itemDao = database.itemDao()
        val ledgerDao = database.ledgerDao()
        val now = System.currentTimeMillis()

        val pendingItemIds = outboxDao.pendingItemIds()
        if (pendingItemIds.isEmpty()) return Result.success()

        var hasRetryable = false

        for (itemId in pendingItemIds) {
            // FIFO drain per item
            var entry = outboxDao.nextPendingForItem(itemId)
            while (entry != null) {
                // Mark as POSTING
                outboxDao.updateStatus(entry.id, OutboxEntity.STATUS_POSTING, now)

                try {
                    val result = functions
                        .getHttpsCallable("postMovement")
                        .call(mapOf("payload" to entry.payload, "idempotencyKey" to entry.idempotencyKey))
                        .await()

                    // ── 2xx: reconcile ──
                    @Suppress("UNCHECKED_CAST")
                    val data = result.getData() as? Map<String, Any>
                    val serverBalance = (data?.get("balanceAfterMilli") as? Number)?.toLong()

                    // Remove the provisional ledger entry
                    ledgerDao.removePendingByKey(entry.idempotencyKey)

                    // Reconcile item balance with server-authoritative value
                    if (serverBalance != null) {
                        itemDao.reconcileBalance(itemId, serverBalance, now)
                    }

                    outboxDao.updateStatus(entry.id, OutboxEntity.STATUS_DONE, now)

                } catch (e: com.google.firebase.functions.FirebaseFunctionsException) {
                    when (e.code) {
                        com.google.firebase.functions.FirebaseFunctionsException.Code.INVALID_ARGUMENT,
                        com.google.firebase.functions.FirebaseFunctionsException.Code.FAILED_PRECONDITION,
                        com.google.firebase.functions.FirebaseFunctionsException.Code.PERMISSION_DENIED,
                        com.google.firebase.functions.FirebaseFunctionsException.Code.NOT_FOUND -> {
                            // ── 4xx equivalent: roll back optimistic change ──
                            Log.w(TAG, "Server rejected movement: ${e.message}", e)

                            // Roll back the optimistic delta from the item
                            // (re-apply the inverse)
                            rollbackOptimistic(entry, itemDao, ledgerDao, now)

                            outboxDao.markFailed(entry.id, e.message, now)
                        }
                        com.google.firebase.functions.FirebaseFunctionsException.Code.ALREADY_EXISTS -> {
                            // Idempotent replay — the server already processed this
                            ledgerDao.removePendingByKey(entry.idempotencyKey)
                            outboxDao.updateStatus(entry.id, OutboxEntity.STATUS_DONE, now)
                            Log.d(TAG, "Idempotent replay for ${entry.idempotencyKey}")
                        }
                        else -> {
                            // ── 5xx equivalent: retry with backoff ──
                            handleRetryable(entry, e, outboxDao, now)
                            hasRetryable = true
                        }
                    }
                } catch (e: Exception) {
                    // Network or unexpected failure — retry with backoff
                    Log.e(TAG, "Network/unexpected error for ${entry.idempotencyKey}", e)
                    handleRetryable(entry, e, outboxDao, now)
                    hasRetryable = true
                }

                // Next FIFO entry for this item
                entry = outboxDao.nextPendingForItem(itemId)
            }
        }

        // Cleanup completed entries
        outboxDao.cleanupDone()

        return if (hasRetryable) Result.retry() else Result.success()
    }

    private suspend fun handleRetryable(
        entry: OutboxEntity,
        error: Exception,
        outboxDao: com.company.stockpro.core.database.dao.OutboxDao,
        now: Long,
    ) {
        if (entry.retryCount >= MAX_RETRIES) {
            outboxDao.markFailed(entry.id, "Max retries exceeded: ${error.message}", now)
        } else {
            outboxDao.updateStatusWithRetry(
                entry.id, OutboxEntity.STATUS_PENDING, error.message, now
            )
        }
    }

    private suspend fun rollbackOptimistic(
        entry: OutboxEntity,
        itemDao: com.company.stockpro.core.database.dao.ItemDao,
        ledgerDao: com.company.stockpro.core.database.dao.LedgerDao,
        now: Long,
    ) {
        // Parse deltaMilli from payload to compute inverse
        val deltaMilliMatch = Regex("\"deltaMilli\":(-?\\d+)").find(entry.payload)
        val originalDelta = deltaMilliMatch?.groupValues?.get(1)?.toLongOrNull() ?: 0L

        // Apply inverse delta to roll back optimistic update
        itemDao.applyOptimisticDelta(entry.itemId, -originalDelta, now)

        // Remove the provisional ledger entry
        ledgerDao.removePendingByKey(entry.idempotencyKey)
    }
}
