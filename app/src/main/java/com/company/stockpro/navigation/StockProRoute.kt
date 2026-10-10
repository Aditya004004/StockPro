package com.company.stockpro.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes for StockPro (architecture.md §5).
 * Uses kotlinx.serialization for compile-time safe argument passing.
 */
sealed interface StockProRoute {
    @Serializable data object Auth : StockProRoute
    @Serializable data object Home : StockProRoute
    @Serializable data object Inventory : StockProRoute
    @Serializable data object Reports : StockProRoute
    @Serializable data object Settings : StockProRoute
    @Serializable data class ItemDetail(val itemId: String) : StockProRoute
    @Serializable data object ExportDatabase : StockProRoute
}
