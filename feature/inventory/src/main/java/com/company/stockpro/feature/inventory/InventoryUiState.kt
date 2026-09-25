package com.company.stockpro.feature.inventory

import com.company.stockpro.core.domain.model.InventorySummary
import com.company.stockpro.core.domain.model.Item
import com.company.stockpro.core.domain.model.ItemClass
import com.company.stockpro.core.domain.model.SortOrder
import com.company.stockpro.core.domain.model.SyncStatus
import com.company.stockpro.core.domain.util.UiText
import com.company.stockpro.core.ui.component.EmptyReason

/**
 * Immutable screen state for InventoryScreen conforming to rules.md §3.5 & §1.7.
 * Handles the 4 core async states: Loading, Content, Empty, and Error.
 */
sealed interface InventoryUiState {
    /** Loading skeleton state with 1200ms shimmer rows. */
    data object Loading : InventoryUiState

    /** Active inventory catalogue content with summary KPI cards and item rows. */
    data class Content(
        val items: List<Item>,
        val summary: InventorySummary = InventorySummary(),
        val selectedClass: ItemClass = ItemClass.COMPONENT,
        val searchQuery: String = "",
        val sortOrder: SortOrder = SortOrder.NAME_ASC,
        val syncStatus: SyncStatus = SyncStatus.Synced(0L),
    ) : InventoryUiState

    /** Contextual empty state naming the cause (no items, search miss, filter miss). */
    data class Empty(
        val reason: EmptyReason,
        val summary: InventorySummary = InventorySummary(),
        val selectedClass: ItemClass = ItemClass.COMPONENT,
        val syncStatus: SyncStatus = SyncStatus.Synced(0L),
    ) : InventoryUiState

    /** Error state with localized user-friendly message and retry capability. */
    data class Error(
        val message: UiText,
    ) : InventoryUiState
}
