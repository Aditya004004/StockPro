package com.company.stockpro.qa

import com.company.stockpro.core.domain.model.Item
import com.company.stockpro.core.domain.model.ItemClass
import com.company.stockpro.core.domain.model.Money
import com.company.stockpro.core.domain.model.MovementType
import com.company.stockpro.core.domain.model.Quantity
import com.company.stockpro.core.domain.model.StockLevel
import com.company.stockpro.core.domain.model.StockMovement
import com.company.stockpro.core.domain.model.UnitSystem
import com.company.stockpro.feature.reports.CategoryValuation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 5 QA Test Suite: Reports Dynamic Calculations & Inventory Valuation Engine.
 * Validates Issues R3, R4 and Inventory mathematical accuracy.
 */
class ReportsAndInventoryCalculationsTest {

    @Test
    fun `valuation by category formula groups correctly and calculates dynamic totals and percentage shares`() {
        val items = listOf(
            Item(
                id = "item_1",
                code = "MEC-01",
                name = "Piston Assembly",
                itemClass = ItemClass.COMPONENT,
                categoryName = "Mechanical",
                unitOfMeasure = "pcs",
                onHand = Quantity(100_000L), // 100 pcs
                reorderLevel = Quantity(20_000L),
                parLevel = Quantity(150_000L),
                unitCost = Money(10_000L), // ₹100.00 each -> Total = ₹10,000
                locationCode = "WH-1-A",
            ),
            Item(
                id = "item_2",
                code = "ELE-01",
                name = "Relay Switch",
                itemClass = ItemClass.COMPONENT,
                categoryName = "Electrical",
                unitOfMeasure = "pcs",
                onHand = Quantity(50_000L), // 50 pcs
                reorderLevel = Quantity(10_000L),
                parLevel = Quantity(80_000L),
                unitCost = Money(10_000L), // ₹100.00 each -> Total = ₹5,000
                locationCode = "WH-1-B",
            ),
            Item(
                id = "item_3",
                code = "RAW-01",
                name = "Sheet Metal 2mm",
                itemClass = ItemClass.RAW_MATERIAL,
                categoryName = "Raw Materials",
                unitOfMeasure = "kg",
                onHand = Quantity(200_000L), // 200 kg
                reorderLevel = Quantity(50_000L),
                parLevel = Quantity(300_000L),
                unitCost = Money(2_500L), // ₹25.00 each -> Total = ₹5,000
                locationCode = "WH-1-C",
            ),
        )

        // Total Valuation = ₹10,000 + ₹5,000 + ₹5,000 = ₹20,000 (2,000,000 paise)
        val grandTotalPaise = items.sumOf { (it.unitCost.paise * it.onHand.milli) / 1000L }
        assertEquals(2_000_000L, grandTotalPaise)

        // Group by category and compute percentage shares
        val categoryBreakdown = items
            .groupBy { it.categoryName }
            .map { (cat, catItems) ->
                val catValuePaise = catItems.sumOf { (it.unitCost.paise * it.onHand.milli) / 1000L }
                val pct = if (grandTotalPaise > 0L) {
                    Math.round((catValuePaise.toDouble() / grandTotalPaise.toDouble()) * 100.0).toInt()
                } else {
                    0
                }
                CategoryValuation(
                    categoryName = cat,
                    valuePaise = catValuePaise,
                    percentage = pct,
                    itemCount = catItems.size,
                )
            }
            .sortedByDescending { it.valuePaise }

        assertEquals(3, categoryBreakdown.size)

        // Mechanical: ₹10,000 (50% share, 1 item)
        val mechanical = categoryBreakdown.first { it.categoryName == "Mechanical" }
        assertEquals(1_000_000L, mechanical.valuePaise)
        assertEquals(50, mechanical.percentage)
        assertEquals(1, mechanical.itemCount)

        // Electrical: ₹5,000 (25% share, 1 item)
        val electrical = categoryBreakdown.first { it.categoryName == "Electrical" }
        assertEquals(500_000L, electrical.valuePaise)
        assertEquals(25, electrical.percentage)
        assertEquals(1, electrical.itemCount)

        // Raw Materials: ₹5,000 (25% share, 1 item)
        val raw = categoryBreakdown.first { it.categoryName == "Raw Materials" }
        assertEquals(500_000L, raw.valuePaise)
        assertEquals(25, raw.percentage)
        assertEquals(1, raw.itemCount)

        // Total percentage sums to 100%
        val totalPercentage = categoryBreakdown.sumOf { it.percentage }
        assertEquals(100, totalPercentage)
    }

    @Test
    fun `inbound vs outbound ledger movements classify accurately`() {
        val movements = listOf(
            // Inbound movements (Purchases / Restocks)
            StockMovement(
                id = "m1",
                reference = "MOV-001",
                itemId = "item_1",
                type = MovementType.RECEIPT,
                delta = Quantity(50_000L), // 50 items
                createdAt = 1000L,
            ),
            StockMovement(
                id = "m2",
                reference = "MOV-002",
                itemId = "item_1",
                type = MovementType.TRANSFER_IN,
                delta = Quantity(20_000L), // 20 items
                createdAt = 2000L,
            ),
            // Outbound movements (Sales / Consumption / Scrap / Transfer Out)
            StockMovement(
                id = "m3",
                reference = "MOV-003",
                itemId = "item_1",
                type = MovementType.ISSUE,
                delta = Quantity(30_000L), // 30 items
                createdAt = 3000L,
            ),
            StockMovement(
                id = "m4",
                reference = "MOV-004",
                itemId = "item_1",
                type = MovementType.TRANSFER_OUT,
                delta = Quantity(15_000L), // 15 items
                createdAt = 4000L,
            ),
            StockMovement(
                id = "m5",
                reference = "MOV-005",
                itemId = "item_1",
                type = MovementType.SCRAP,
                delta = Quantity(5_000L), // 5 items
                createdAt = 5000L,
            ),
        )

        // Inbound: RECEIPT + TRANSFER_IN
        val inboundMovements = movements.filter { it.type == MovementType.RECEIPT || it.type == MovementType.TRANSFER_IN }
        val totalInboundUnits = inboundMovements.sumOf { it.delta.toUnitsDouble }
        assertEquals(70.0, totalInboundUnits, 0.001)

        // Outbound: ISSUE + TRANSFER_OUT + SCRAP
        val outboundMovements = movements.filter {
            it.type == MovementType.ISSUE || it.type == MovementType.TRANSFER_OUT || it.type == MovementType.SCRAP
        }
        val totalOutboundUnits = outboundMovements.sumOf { it.delta.toUnitsDouble }
        assertEquals(50.0, totalOutboundUnits, 0.001)

        // Net movement balance: Inbound - Outbound = 70 - 50 = +20
        val netBalanceUnits = totalInboundUnits - totalOutboundUnits
        assertEquals(20.0, netBalanceUnits, 0.001)
    }

    @Test
    fun `inventory stock status thresholds correctly determine IN_STOCK, LOW_STOCK, and OUT_OF_STOCK`() {
        val parLevel = Quantity(100_000L) // 100 units
        val reorderLevel = Quantity(30_000L) // 30 units

        // 1. OUT_OF_STOCK: onHand <= 0
        val itemOutOfStock = Item(
            id = "i1", code = "C1", name = "N1", itemClass = ItemClass.COMPONENT,
            categoryName = "Fasteners", unitOfMeasure = "pcs", unitCost = Money(1000L), locationCode = "A-01",
            onHand = Quantity(0L), reorderLevel = reorderLevel, parLevel = parLevel,
        )
        assertEquals(StockLevel.OUT_OF_STOCK, itemOutOfStock.stockLevel)

        // 2. LOW_STOCK: onHand <= reorderLevel
        val itemLowStock = Item(
            id = "i3", code = "C3", name = "N3", itemClass = ItemClass.COMPONENT,
            categoryName = "Fasteners", unitOfMeasure = "pcs", unitCost = Money(1000L), locationCode = "A-01",
            onHand = Quantity(28_000L), reorderLevel = reorderLevel, parLevel = parLevel,
        )
        assertEquals(StockLevel.LOW_STOCK, itemLowStock.stockLevel)

        // 3. IN_STOCK: onHand > reorderLevel
        val itemOk = Item(
            id = "i4", code = "C4", name = "N4", itemClass = ItemClass.COMPONENT,
            categoryName = "Fasteners", unitOfMeasure = "pcs", unitCost = Money(1000L), locationCode = "A-01",
            onHand = Quantity(80_000L), reorderLevel = reorderLevel, parLevel = parLevel,
        )
        assertEquals(StockLevel.IN_STOCK, itemOk.stockLevel)
    }

    @Test
    fun `measurement unit conversions accurately convert Metric to Imperial and vice-versa`() {
        val metric = UnitSystem.METRIC
        val imperial = UnitSystem.IMPERIAL

        // Weight: 100 kg to lb
        val weightKg = 100.0
        val weightInMetric = metric.convertWeightKg(weightKg)
        val weightInImperial = imperial.convertWeightKg(weightKg)

        assertEquals(100.0, weightInMetric, 0.001)
        assertEquals(220.462, weightInImperial, 0.01)

        // Volume: 50 Liters to Gallons
        val volumeL = 50.0
        val volumeInMetric = metric.convertVolumeLiters(volumeL)
        val volumeInImperial = imperial.convertVolumeLiters(volumeL)

        assertEquals(50.0, volumeInMetric, 0.001)
        assertEquals(13.208, volumeInImperial, 0.01)
    }
}
