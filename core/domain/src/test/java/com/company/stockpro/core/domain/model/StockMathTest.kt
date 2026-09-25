package com.company.stockpro.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 100% branch coverage on domain arithmetic — rules.md §8.
 *
 * Tests cover:
 * - Quantity: addition, subtraction, comparison, zero, fromUnits, toUnitsDouble
 * - Money: addition, subtraction, comparison, zero, fromRupees, toRupeesDouble
 * - StockLevel.derive: all three branches (OUT_OF_STOCK, LOW_STOCK, IN_STOCK)
 * - Item.available: correct clamping at zero
 * - MovementType.fromString: valid, case-insensitive, unknown
 */
class StockMathTest {

    // ── Quantity ──

    @Test
    fun `quantity addition`() {
        val a = Quantity(12_300L)
        val b = Quantity(7_700L)
        assertEquals(Quantity(20_000L), a + b)
    }

    @Test
    fun `quantity subtraction`() {
        val a = Quantity(20_000L)
        val b = Quantity(7_700L)
        assertEquals(Quantity(12_300L), a - b)
    }

    @Test
    fun `quantity comparison - greater`() {
        assertTrue(Quantity(20_000L) > Quantity(10_000L))
    }

    @Test
    fun `quantity comparison - equal`() {
        assertEquals(0, Quantity(10_000L).compareTo(Quantity(10_000L)))
    }

    @Test
    fun `quantity comparison - less`() {
        assertTrue(Quantity(5_000L) < Quantity(10_000L))
    }

    @Test
    fun `quantity ZERO is zero`() {
        assertEquals(0L, Quantity.ZERO.milli)
    }

    @Test
    fun `quantity fromUnits converts correctly`() {
        assertEquals(Quantity(12_000L), Quantity.fromUnits(12L))
    }

    @Test
    fun `quantity toUnitsDouble converts correctly`() {
        assertEquals(12.3, Quantity(12_300L).toUnitsDouble, 0.001)
    }

    // ── Money ──

    @Test
    fun `money addition`() {
        val a = Money(45_000L)
        val b = Money(5_050L)
        assertEquals(Money(50_050L), a + b)
    }

    @Test
    fun `money subtraction`() {
        val a = Money(50_050L)
        val b = Money(5_050L)
        assertEquals(Money(45_000L), a - b)
    }

    @Test
    fun `money comparison - greater`() {
        assertTrue(Money(50_000L) > Money(10_000L))
    }

    @Test
    fun `money comparison - equal`() {
        assertEquals(0, Money(10_000L).compareTo(Money(10_000L)))
    }

    @Test
    fun `money comparison - less`() {
        assertTrue(Money(5_000L) < Money(10_000L))
    }

    @Test
    fun `money ZERO is zero`() {
        assertEquals(0L, Money.ZERO.paise)
    }

    @Test
    fun `money fromRupees converts correctly`() {
        assertEquals(Money(45_000L), Money.fromRupees(450L))
    }

    @Test
    fun `money toRupeesDouble converts correctly`() {
        assertEquals(450.5, Money(45_050L).toRupeesDouble, 0.001)
    }

    // ── StockLevel.derive ──

    @Test
    fun `derive OUT_OF_STOCK when onHand is zero`() {
        assertEquals(
            StockLevel.OUT_OF_STOCK,
            StockLevel.derive(Quantity(0L), Quantity(50_000L))
        )
    }

    @Test
    fun `derive OUT_OF_STOCK when onHand is negative`() {
        assertEquals(
            StockLevel.OUT_OF_STOCK,
            StockLevel.derive(Quantity(-1_000L), Quantity(50_000L))
        )
    }

    @Test
    fun `derive LOW_STOCK when onHand equals reorderLevel`() {
        assertEquals(
            StockLevel.LOW_STOCK,
            StockLevel.derive(Quantity(50_000L), Quantity(50_000L))
        )
    }

    @Test
    fun `derive LOW_STOCK when onHand just below reorderLevel`() {
        assertEquals(
            StockLevel.LOW_STOCK,
            StockLevel.derive(Quantity(49_999L), Quantity(50_000L))
        )
    }

    @Test
    fun `derive IN_STOCK when onHand above reorderLevel`() {
        assertEquals(
            StockLevel.IN_STOCK,
            StockLevel.derive(Quantity(50_001L), Quantity(50_000L))
        )
    }

    @Test
    fun `derive IN_STOCK when reorderLevel is zero`() {
        assertEquals(
            StockLevel.IN_STOCK,
            StockLevel.derive(Quantity(1_000L), Quantity(0L))
        )
    }

    // ── Item.available ──

    @Test
    fun `available is onHand minus reserved`() {
        val item = createItem(onHand = Quantity(100_000L), reserved = Quantity(30_000L))
        assertEquals(Quantity(70_000L), item.available)
    }

    @Test
    fun `available is clamped at zero`() {
        val item = createItem(onHand = Quantity(10_000L), reserved = Quantity(30_000L))
        assertEquals(Quantity.ZERO, item.available)
    }

    @Test
    fun `available is full when no reservations`() {
        val item = createItem(onHand = Quantity(100_000L), reserved = Quantity.ZERO)
        assertEquals(Quantity(100_000L), item.available)
    }

    // ── MovementType.fromString ──

    @Test
    fun `fromString parses valid type`() {
        assertEquals(MovementType.RECEIPT, MovementType.fromString("RECEIPT"))
    }

    @Test
    fun `fromString is case insensitive`() {
        assertEquals(MovementType.ISSUE, MovementType.fromString("issue"))
    }

    @Test
    fun `fromString returns UNKNOWN for null`() {
        assertEquals(MovementType.UNKNOWN, MovementType.fromString(null))
    }

    @Test
    fun `fromString returns UNKNOWN for garbage`() {
        assertEquals(MovementType.UNKNOWN, MovementType.fromString("MAGIC_TELEPORT"))
    }

    // ── ItemClass.fromString ──

    @Test
    fun `itemClass fromString parses valid class`() {
        assertEquals(ItemClass.RAW_MATERIAL, ItemClass.fromString("RAW_MATERIAL"))
    }

    @Test
    fun `itemClass fromString is case insensitive`() {
        assertEquals(ItemClass.TOOL, ItemClass.fromString("tool"))
    }

    @Test
    fun `itemClass fromString returns UNKNOWN for null`() {
        assertEquals(ItemClass.UNKNOWN, ItemClass.fromString(null))
    }

    @Test
    fun `itemClass fromString returns UNKNOWN for garbage`() {
        assertEquals(ItemClass.UNKNOWN, ItemClass.fromString("UNICORN"))
    }

    // ── Helper ──

    private fun createItem(
        onHand: Quantity = Quantity.ZERO,
        reserved: Quantity = Quantity.ZERO,
    ) = Item(
        id = "test-item",
        code = "TST-001",
        name = "Test Item",
        itemClass = ItemClass.COMPONENT,
        categoryName = "Test",
        unitOfMeasure = "PCS",
        onHand = onHand,
        reserved = reserved,
        reorderLevel = Quantity(50_000L),
        parLevel = Quantity(500_000L),
        unitCost = Money(45_000L),
        locationCode = "A1-01",
    )
}
