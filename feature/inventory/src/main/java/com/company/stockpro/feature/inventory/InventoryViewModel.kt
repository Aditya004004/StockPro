package com.company.stockpro.feature.inventory

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.stockpro.core.domain.model.InventorySummary
import com.company.stockpro.core.domain.model.Item
import com.company.stockpro.core.domain.model.ItemClass
import com.company.stockpro.core.domain.model.SortOrder
import com.company.stockpro.core.domain.model.SyncStatus
import com.company.stockpro.core.domain.repository.InventoryRepository
import com.company.stockpro.core.domain.util.UiText
import com.company.stockpro.core.ui.component.EmptyReason
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel for InventoryScreen.
 *
 * Enforces rules.md §5.2:
 * - Only the selected tab's data is collected, never tripling local queries.
 * - Search query is debounced for 300ms before triggering FTS.
 * - Produces a single StateFlow<InventoryUiState> covering all 4 async states.
 * - State survives process death via SavedStateHandle.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val repository: InventoryRepository,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {

    companion object {
        private const val KEY_SELECTED_CLASS = "selected_class"
        private const val KEY_SEARCH_QUERY = "search_query"
        private const val KEY_SORT_ORDER = "sort_order"
    }

    private val selectedClass = savedStateHandle.getStateFlow(
        KEY_SELECTED_CLASS,
        ItemClass.COMPONENT,
    )
    private val searchQuery = savedStateHandle.getStateFlow(
        KEY_SEARCH_QUERY,
        "",
    )
    private val sortOrder = savedStateHandle.getStateFlow(
        KEY_SORT_ORDER,
        SortOrder.NAME_ASC,
    )

    // Debounced search query (300ms per rules.md §5.2)
    private val debouncedSearchQuery = searchQuery
        .debounce(300L)
        .distinctUntilChanged()

    // Tab-keyed item observation: collects ONLY the selected class, or FTS results if query present
    private val itemsFlow = combine(
        selectedClass,
        debouncedSearchQuery,
        sortOrder,
    ) { itemClass, query, sort ->
        Triple(itemClass, query, sort)
    }.flatMapLatest { (itemClass, query, sort) ->
        if (query.isNotBlank()) {
            repository.searchItems(query)
        } else {
            repository.observeItemsByClass(itemClass, sort)
        }
    }

    private data class FilterState(
        val itemClass: ItemClass,
        val query: String,
        val sort: SortOrder,
    )

    private val filterState = combine(
        selectedClass,
        searchQuery,
        sortOrder,
    ) { itemClass, query, sort ->
        FilterState(itemClass, query, sort)
    }

    val uiState: StateFlow<InventoryUiState> = combine(
        itemsFlow,
        repository.observeSummary(),
        filterState,
        repository.observeSyncStatus(),
    ) { items, summary, filter, syncStatus ->
        when {
            items.isEmpty() && filter.query.isNotBlank() -> {
                InventoryUiState.Empty(
                    reason = EmptyReason.SearchMiss(filter.query),
                    summary = summary,
                    selectedClass = filter.itemClass,
                    syncStatus = syncStatus,
                )
            }
            items.isEmpty() && summary.totalItems == 0 -> {
                InventoryUiState.Empty(
                    reason = EmptyReason.NoItems,
                    summary = summary,
                    selectedClass = filter.itemClass,
                    syncStatus = syncStatus,
                )
            }
            items.isEmpty() -> {
                InventoryUiState.Empty(
                    reason = EmptyReason.FilterMiss(filter.itemClass.name.lowercase().replaceFirstChar { it.uppercase() }),
                    summary = summary,
                    selectedClass = filter.itemClass,
                    syncStatus = syncStatus,
                )
            }
            else -> {
                InventoryUiState.Content(
                    items = items,
                    summary = summary,
                    selectedClass = filter.itemClass,
                    searchQuery = filter.query,
                    sortOrder = filter.sort,
                    syncStatus = syncStatus,
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = InventoryUiState.Loading,
    )

    fun onClassSelected(itemClass: ItemClass) {
        savedStateHandle[KEY_SELECTED_CLASS] = itemClass
    }

    fun onSearchQueryChanged(query: String) {
        savedStateHandle[KEY_SEARCH_QUERY] = query
    }

    fun onSortOrderChanged(newSort: SortOrder) {
        savedStateHandle[KEY_SORT_ORDER] = newSort
    }

    fun retryFailedOutbox(id: Long) {
        viewModelScope.launch {
            runCatching {
                repository.retryFailedOutbox(id)
            }
        }
    }

    fun discardFailedOutbox(id: Long) {
        viewModelScope.launch {
            runCatching {
                repository.discardFailedOutbox(id)
            }
        }
    }
}
