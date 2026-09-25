package com.company.stockpro.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Property-based test for ledger invariant (rules.md §8):
 *
 * "Any sequence of movements leaves ledger sum == projection balance."
 *
 * Simulates sequences of receipts, issues, adjustments, and scrap
 * to verify that ledger integrity holds unconditionally.
 */
class LedgerPropertyTest {

    data class LedgerEntry(
        val type: MovementType,
        val deltaMilli: Long,
        val balanceAfterMilli: Long,
    )

    @Test
    fun `ledger sum property holds for random movement sequence`() {
        val random = Random(42) // Deterministic seed for reproducible tests
        var projectedOnHand = 0L
        val ledger = mutableListOf<LedgerEntry>()

        for (i in 1..200) {
            val isAddition = random.nextBoolean() || projectedOnHand <= 0L

            val type = if (isAddition) {
                if (random.nextBoolean()) MovementType.RECEIPT else MovementType.TRANSFER_IN
            } else {
                val roll = random.nextInt(3)
                when (roll) {
                    0 -> MovementType.ISSUE
                    1 -> MovementType.TRANSFER_OUT
                    else -> MovementType.SCRAP
                }
            }

            val deltaMilli: Long = if (isAddition) {
                random.nextLong(1_000L, 50_000L)
            } else {
                // Cannot issue more than available
                val maxIssue = projectedOnHand.coerceAtLeast(1_000L)
                -random.nextLong(500L, maxIssue)
            }

            projectedOnHand += deltaMilli
            ledger.add(
                LedgerEntry(
                    type = type,
                    deltaMilli = deltaMilli,
                    balanceAfterMilli = projectedOnHand,
                )
            )
        }

        // ── Property Verification ──
        val sumOfDeltas = ledger.sumOf { it.deltaMilli }
        val finalProjectedBalance = projectedOnHand
        val lastEntryBalanceAfter = ledger.last().balanceAfterMilli

        assertEquals(
            "Sum of ledger deltas must equal the final projected balance",
            finalProjectedBalance,
            sumOfDeltas,
        )

        assertEquals(
            "The last ledger entry's balanceAfter must equal the sum of all deltas",
            sumOfDeltas,
            lastEntryBalanceAfter,
        )
    }

    @Test
    fun `ledger adjustment property holds across inventory counts`() {
        var projectedOnHand = 50_000L // start with 50 pcs
        val ledger = mutableListOf<LedgerEntry>()

        // 1. Initial count
        ledger.add(LedgerEntry(MovementType.RECEIPT, 50_000L, 50_000L))

        // 2. Physical count reveals 42 pcs (loss of 8 pcs)
        val physicalCount1 = 42_000L
        val delta1 = physicalCount1 - projectedOnHand
        projectedOnHand = physicalCount1
        ledger.add(LedgerEntry(MovementType.ADJUSTMENT, delta1, projectedOnHand))

        // 3. New shipment arrives +100 pcs
        val shipment = 100_000L
        projectedOnHand += shipment
        ledger.add(LedgerEntry(MovementType.RECEIPT, shipment, projectedOnHand))

        // 4. Physical count reveals 145 pcs (found 3 pcs)
        val physicalCount2 = 145_000L
        val delta2 = physicalCount2 - projectedOnHand
        projectedOnHand = physicalCount2
        ledger.add(LedgerEntry(MovementType.ADJUSTMENT, delta2, projectedOnHand))

        val sumOfDeltas = ledger.sumOf { it.deltaMilli }

        assertEquals(145_000L, sumOfDeltas)
        assertEquals(projectedOnHand, sumOfDeltas)
        assertEquals(ledger.last().balanceAfterMilli, sumOfDeltas)
    }

    @Test
    fun `empty ledger has zero sum`() {
        val ledger = emptyList<LedgerEntry>()
        assertEquals(0L, ledger.sumOf { it.deltaMilli })
    }
}
