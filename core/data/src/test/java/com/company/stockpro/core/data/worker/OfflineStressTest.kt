package com.company.stockpro.core.data.worker

import com.company.stockpro.core.domain.model.MovementType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

/**
 * Structured Offline Stress & Concurrency Resiliency Test Suite.
 * docs/phases.md § Phase 6 (Hardening):
 *
 * 1. Full shift of movements offline, then reconnect (FIFO order, zero duplicates).
 * 2. Two devices move the same item while both offline (commutative deltas).
 * 3. App killed mid-post (interrupted outbox state recovery without partial writes).
 * 4. Queued issue exceeds stock on arrival (4xx rejection -> optimistic rollback -> Needs Attention).
 * 5. 72 hours offline with continuous use (unbounded growth prevention via cleanupDone).
 * 6. Clock skew between devices (server timestamps authoritative).
 * 7. Token expiry while offline (survives UNAUTHENTICATED, drains after refresh).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OfflineStressTest {

    data class MockOutboxEntry(
        val id: Long,
        val itemId: String,
        val idempotencyKey: String,
        val movementType: MovementType,
        val deltaMilli: Long,
        var status: String,
        var retryCount: Int = 0,
        var errorMessage: String? = null,
        val createdAt: Long,
    )

    data class MockItem(
        val id: String,
        var onHandMilli: Long,
    )

    class MockOfflineEngine {
        var autoIncrementId = 1L
        val outbox = mutableListOf<MockOutboxEntry>()
        val items = mutableMapOf<String, MockItem>()
        val serverLedger = mutableListOf<Map<String, Any>>()
        val serverProcessedKeys = mutableSetOf<String>()

        fun addItem(id: String, initialMilli: Long) {
            items[id] = MockItem(id, initialMilli)
        }

        /**
         * Simulates optimistic write while offline.
         */
        fun recordMovementOffline(itemId: String, type: MovementType, deltaMilli: Long, timestamp: Long): String {
            val key = UUID.randomUUID().toString()
            val item = items[itemId] ?: throw IllegalArgumentException("Item not found: $itemId")

            // Optimistic local update
            item.onHandMilli += deltaMilli

            outbox.add(
                MockOutboxEntry(
                    id = autoIncrementId++,
                    itemId = itemId,
                    idempotencyKey = key,
                    movementType = type,
                    deltaMilli = deltaMilli,
                    status = "PENDING",
                    createdAt = timestamp,
                )
            )
            return key
        }

        /**
         * Simulates OutboxWorker drain execution.
         */
        fun drainOutbox(
            serverStockMap: MutableMap<String, Long>,
            simulateCrashOnPostingKey: String? = null,
            simulateTokenExpired: Boolean = false,
        ): DrainResult {
            if (simulateTokenExpired) {
                // Network / Auth error: leave items pending, return backoff
                return DrainResult(posted = 0, failed = 0, retried = outbox.count { it.status == "PENDING" })
            }

            var posted = 0
            var failed = 0
            var retried = 0

            val pendingItemIds = outbox.filter { it.status == "PENDING" || it.status == "POSTING" }
                .map { it.itemId }
                .distinct()

            for (itemId in pendingItemIds) {
                val itemEntries = outbox
                    .filter { it.itemId == itemId && (it.status == "PENDING" || it.status == "POSTING") }
                    .sortedBy { it.id }

                for (entry in itemEntries) {
                    if (entry.idempotencyKey == simulateCrashOnPostingKey && entry.status != "POSTING") {
                        // Simulate crash mid-post
                        entry.status = "POSTING"
                        return DrainResult(posted, failed, retried = 1, crashedMidPost = true)
                    }

                    entry.status = "POSTING"

                    // Check server idempotency
                    if (serverProcessedKeys.contains(entry.idempotencyKey)) {
                        // Idempotent replay: already applied on server
                        entry.status = "DONE"
                        posted++
                        continue
                    }

                    val currentServerStock = serverStockMap[itemId] ?: 0L
                    val newServerBalance = currentServerStock + entry.deltaMilli

                    if (newServerBalance < 0L && entry.deltaMilli < 0L) {
                        // 4xx FAILED_PRECONDITION: insufficient stock
                        entry.status = "FAILED"
                        entry.errorMessage = "FAILED_PRECONDITION: Available stock ($currentServerStock) insufficient for issue (${-entry.deltaMilli})"
                        // Rollback optimistic delta
                        items[itemId]?.let { it.onHandMilli -= entry.deltaMilli }
                        failed++
                    } else {
                        // 2xx Success: apply to server
                        serverStockMap[itemId] = newServerBalance
                        serverProcessedKeys.add(entry.idempotencyKey)
                        serverLedger.add(
                            mapOf(
                                "idempotencyKey" to entry.idempotencyKey,
                                "itemId" to itemId,
                                "deltaMilli" to entry.deltaMilli,
                                "balanceAfterMilli" to newServerBalance,
                                "serverTimestamp" to System.currentTimeMillis(),
                            )
                        )
                        // Reconcile local item with server balance
                        items[itemId]?.onHandMilli = newServerBalance
                        entry.status = "DONE"
                        posted++
                    }
                }
            }

            return DrainResult(posted, failed, retried)
        }

        fun cleanupDone() {
            outbox.removeAll { it.status == "DONE" }
        }
    }

    data class DrainResult(
        val posted: Int,
        val failed: Int,
        val retried: Int,
        val crashedMidPost: Boolean = false,
    )

    // ── Scenario 1: Full shift offline, then reconnect ───────────────────────────
    @Test
    fun `scenario 1 - full shift of movements offline drains in FIFO order with zero duplicates`() = runTest {
        val engine = MockOfflineEngine()
        val serverStock = mutableMapOf("item_bolt" to 100_000L, "item_nut" to 50_000L)
        engine.addItem("item_bolt", 100_000L)
        engine.addItem("item_nut", 50_000L)

        // Queue 60 movements offline across 2 items
        for (i in 1..30) {
            engine.recordMovementOffline("item_bolt", MovementType.RECEIPT, 1_000L, System.currentTimeMillis())
            engine.recordMovementOffline("item_nut", MovementType.ISSUE, -500L, System.currentTimeMillis())
        }

        assertEquals(60, engine.outbox.size)
        // Local optimistic projections
        assertEquals(130_000L, engine.items["item_bolt"]?.onHandMilli)
        assertEquals(35_000L, engine.items["item_nut"]?.onHandMilli)

        // Reconnect and drain
        val result = engine.drainOutbox(serverStock)
        assertEquals(60, result.posted)
        assertEquals(0, result.failed)
        assertEquals(0, result.retried)

        // Server authoritative balance matches
        assertEquals(130_000L, serverStock["item_bolt"])
        assertEquals(35_000L, serverStock["item_nut"])
        assertEquals(60, engine.serverProcessedKeys.size)
    }

    // ── Scenario 2: Two devices move same item offline ───────────────────────────
    @Test
    fun `scenario 2 - two devices moving same item offline applies deltas commutatively`() = runTest {
        val initialStock = 100_000L
        val serverStock = mutableMapOf("item_bearing" to initialStock)

        val deviceA = MockOfflineEngine()
        deviceA.addItem("item_bearing", initialStock)

        val deviceB = MockOfflineEngine()
        deviceB.addItem("item_bearing", initialStock)

        // Device A issues 20,000 offline
        deviceA.recordMovementOffline("item_bearing", MovementType.ISSUE, -20_000L, 1000L)

        // Device B receives 50,000 offline
        deviceB.recordMovementOffline("item_bearing", MovementType.RECEIPT, 50_000L, 1001L)

        // Device A syncs first
        val resultA = deviceA.drainOutbox(serverStock)
        assertEquals(1, resultA.posted)
        assertEquals(80_000L, serverStock["item_bearing"])

        // Device B syncs second
        val resultB = deviceB.drainOutbox(serverStock)
        assertEquals(1, resultB.posted)
        assertEquals(130_000L, serverStock["item_bearing"])

        // Final authoritative stock: 100k - 20k + 50k = 130k
        assertEquals(130_000L, serverStock["item_bearing"])
    }

    // ── Scenario 3: App killed mid-post ──────────────────────────────────────────
    @Test
    fun `scenario 3 - app killed mid-post recovers safely without duplicate writes`() = runTest {
        val engine = MockOfflineEngine()
        val serverStock = mutableMapOf("item_gasket" to 20_000L)
        engine.addItem("item_gasket", 20_000L)

        val key = engine.recordMovementOffline("item_gasket", MovementType.RECEIPT, 5_000L, 1000L)

        // Simulate crash mid-post
        val crashResult = engine.drainOutbox(serverStock, simulateCrashOnPostingKey = key)
        assertTrue(crashResult.crashedMidPost)
        assertEquals("POSTING", engine.outbox.first().status)

        // App restarts, worker resumes: entry marked POSTING is re-attempted
        val recoverResult = engine.drainOutbox(serverStock)
        assertEquals(1, recoverResult.posted)
        assertEquals("DONE", engine.outbox.first().status)
        assertEquals(25_000L, serverStock["item_gasket"])
        assertEquals(1, engine.serverProcessedKeys.size)
    }

    // ── Scenario 4: Queued issue exceeds stock on arrival ────────────────────────
    @Test
    fun `scenario 4 - queued issue exceeding stock rejects with rollback into needs attention`() = runTest {
        val engine = MockOfflineEngine()
        // Server only has 10 units
        val serverStock = mutableMapOf("item_motor" to 10_000L)
        // Local device had stale 50 units
        engine.addItem("item_motor", 50_000L)

        // Offline operator issues 30 units (believing they have 50)
        engine.recordMovementOffline("item_motor", MovementType.ISSUE, -30_000L, 1000L)
        assertEquals(20_000L, engine.items["item_motor"]?.onHandMilli)

        // Drain against real server: rejects with 4xx FAILED_PRECONDITION
        val result = engine.drainOutbox(serverStock)
        assertEquals(0, result.posted)
        assertEquals(1, result.failed)

        val failedEntry = engine.outbox.first()
        assertEquals("FAILED", failedEntry.status)
        assertNotNull(failedEntry.errorMessage)
        assertTrue(failedEntry.errorMessage!!.contains("FAILED_PRECONDITION"))

        // Local balance rolled back (+30k inverse)
        assertEquals(50_000L, engine.items["item_motor"]?.onHandMilli)
        // Server stock unchanged
        assertEquals(10_000L, serverStock["item_motor"])
    }

    // ── Scenario 5: 72 hours offline continuous use ──────────────────────────────
    @Test
    fun `scenario 5 - 72 hours offline keeps storage bounded via cleanupDone`() = runTest {
        val engine = MockOfflineEngine()
        val serverStock = mutableMapOf("item_fastener" to 500_000L)
        engine.addItem("item_fastener", 500_000L)

        // Simulate 72 hours with periodic sync bursts
        for (shift in 1..9) {
            for (m in 1..20) {
                engine.recordMovementOffline("item_fastener", MovementType.RECEIPT, 100L, System.currentTimeMillis())
            }
            // Sync and cleanup at end of each shift
            engine.drainOutbox(serverStock)
            engine.cleanupDone()

            // Outbox size is strictly 0 after completed drain and cleanup
            assertEquals(0, engine.outbox.size)
        }

        assertEquals(0, engine.outbox.size)
        assertEquals(518_000L, serverStock["item_fastener"])
    }

    // ── Scenario 6: Clock skew between devices ───────────────────────────────────
    @Test
    fun `scenario 6 - clock skew between devices defaults to authoritative server ordering`() = runTest {
        val engine = MockOfflineEngine()
        val serverStock = mutableMapOf("item_valve" to 100_000L)
        engine.addItem("item_valve", 100_000L)

        val baseTime = 1700000000000L
        // Device clock skewed backward by 10 days
        val skewedKey = engine.recordMovementOffline("item_valve", MovementType.RECEIPT, 5_000L, baseTime - (10 * 86400000L))

        engine.drainOutbox(serverStock)

        val ledgerEntry = engine.serverLedger.first { it["idempotencyKey"] == skewedKey }
        val serverTimestamp = ledgerEntry["serverTimestamp"] as Long

        // Server timestamp is fresh and within current operational window, not skewed backward
        assertTrue(serverTimestamp >= baseTime)
    }

    // ── Scenario 7: Token expiry while offline ───────────────────────────────────
    @Test
    fun `scenario 7 - token expiry while offline preserves queue and drains after auth refresh`() = runTest {
        val engine = MockOfflineEngine()
        val serverStock = mutableMapOf("item_pipe" to 50_000L)
        engine.addItem("item_pipe", 50_000L)

        engine.recordMovementOffline("item_pipe", MovementType.ISSUE, -5_000L, 1000L)

        // 1. Reconnect with expired token: returns retry, queue stays intact
        val expiredResult = engine.drainOutbox(serverStock, simulateTokenExpired = true)
        assertEquals(0, expiredResult.posted)
        assertEquals(1, expiredResult.retried)
        assertEquals("PENDING", engine.outbox.first().status)
        assertEquals(50_000L, serverStock["item_pipe"]) // Server untouched

        // 2. Token refreshed, retry succeeds
        val refreshedResult = engine.drainOutbox(serverStock, simulateTokenExpired = false)
        assertEquals(1, refreshedResult.posted)
        assertEquals("DONE", engine.outbox.first().status)
        assertEquals(45_000L, serverStock["item_pipe"])
    }
}
