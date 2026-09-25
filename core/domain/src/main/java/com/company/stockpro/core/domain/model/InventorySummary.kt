package com.company.stockpro.core.domain.model

/**
 * Summary KPI metrics for the Inventory screen header cards.
 * Matches the 4 summary pills in the approved design:
 * Total Items, Low Stock, Out of Stock, Reserved.
 */
data class InventorySummary(
    val totalItems: Int = 0,
    val lowStockCount: Int = 0,
    val outOfStockCount: Int = 0,
    val reservedCount: Int = 0,
)
