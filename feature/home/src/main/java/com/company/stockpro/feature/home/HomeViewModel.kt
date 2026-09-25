package com.company.stockpro.feature.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.stockpro.core.designsystem.theme.StockProColors
import com.company.stockpro.core.domain.model.InventorySummary
import com.company.stockpro.core.domain.model.Item
import com.company.stockpro.core.domain.model.ItemClass
import com.company.stockpro.core.domain.model.MovementType
import com.company.stockpro.core.domain.model.StockMovement
import com.company.stockpro.core.domain.model.SyncStatus
import com.company.stockpro.core.domain.repository.InventoryRepository
import com.company.stockpro.core.ui.component.chart.StockSegment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel for HomeScreen adhering to rules.md §5.2:
 * - Emits a single reactive StateFlow<HomeUiState>.
 * - Aggregates Room data without pass-through abstractions.
 * - Handles valuation privacy masking and plant selection.
 * - State survives process death via SavedStateHandle.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: InventoryRepository,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {

    companion object {
        private const val KEY_ACTIVE_PLANT = "active_plant"
        private const val KEY_IS_VALUATION_VISIBLE = "is_valuation_visible"
    }

    private val activePlant = savedStateHandle.getStateFlow(KEY_ACTIVE_PLANT, "Plant 1")
    private val isValuationVisible = savedStateHandle.getStateFlow(KEY_IS_VALUATION_VISIBLE, true)

    private val userContext = combine(
        activePlant,
        isValuationVisible,
        repository.observeSyncStatus(),
        repository.observeUserSettings(),
        repository.observeUnreadNotificationCount(),
    ) { plant, visible, syncStatus, userSettings, unreadCount ->
        UserContext(
            activePlant = plant,
            isValuationVisible = visible,
            syncStatus = syncStatus,
            userName = userSettings.profile.displayName,
            plantName = userSettings.profile.plantName,
            unreadAlertsCount = unreadCount,
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeSummary(),
        repository.observeItems(),
        repository.observeRecentMovements(5),
        userContext,
    ) { summary, items, recentMovements, userCtx ->
        val rawMaterials = items.count { it.itemClass == ItemClass.RAW_MATERIAL }.toLong()
        val finishedGoods = items.count { it.itemClass == ItemClass.FINISHED_GOOD }.toLong()

        // Valuation sum: onHand * unitCost
        val valuationPaise = items.sumOf {
            (it.onHand.toUnitsDouble * it.unitCost.paise).toLong()
        }

        // Top 5 low stock items: onHand <= reorderLevel, sorted ascending by onHand
        val topLowStock = items
            .filter { it.onHand.milli <= it.reorderLevel.milli }
            .sortedBy { it.onHand.milli }
            .take(5)

        // Stock distribution segments
        val inStockCount = (summary.totalItems - summary.lowStockCount - summary.outOfStockCount).coerceAtLeast(0)
        val inProductionCount = items.sumOf { (it.inProduction.toUnitsDouble).toLong() }
        val segments = listOf(
            StockSegment("In Stock", inStockCount.toLong(), StockProColors.Success),
            StockSegment("Low Stock", summary.lowStockCount.toLong(), StockProColors.Warning),
            StockSegment("Out of Stock", summary.outOfStockCount.toLong(), StockProColors.Danger),
            StockSegment("Reserved", summary.reservedCount.toLong(), StockProColors.BrandMark),
            StockSegment("In Production", inProductionCount, StockProColors.Primary),
        )

        // Recent activity feed mapped from ledger
        val activities = recentMovements.map { it.toHomeActivityItem() }

        // Sparkline trend points
        val sparklinePoints = emptyList<Float>()
        val sparklineLabels = emptyList<String>()

        HomeUiState.Content(
            userName = userCtx.userName.ifBlank { "User" },
            activePlant = userCtx.activePlant,
            availablePlants = listOf(userCtx.activePlant),
            unreadAlertsCount = userCtx.unreadAlertsCount,
            totalItems = summary.totalItems.toLong(),
            totalItemsDeltaPercent = 0,
            rawMaterialsCount = rawMaterials,
            rawMaterialsDeltaPercent = 0,
            lowStockCount = summary.lowStockCount.toLong(),
            lowStockItemsNeedReorder = summary.lowStockCount.toLong(),
            finishedGoodsCount = finishedGoods,
            finishedGoodsDeltaPercent = 0,
            totalValuationPaise = valuationPaise,
            valuationDeltaPercent = 0f,
            isValuationVisible = userCtx.isValuationVisible,
            sparklinePoints = sparklinePoints,
            sparklineLabels = sparklineLabels,
            stockSegments = segments,
            recentActivities = activities,
            topLowStockItems = topLowStock,
            upcomingReordersCount = topLowStock.size,
            syncStatus = userCtx.syncStatus,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = HomeUiState.Loading,
    )

    fun toggleValuationVisibility() {
        savedStateHandle[KEY_IS_VALUATION_VISIBLE] = !isValuationVisible.value
    }

    fun selectPlant(plantName: String) {
        savedStateHandle[KEY_ACTIVE_PLANT] = plantName
    }

    private fun StockMovement.toHomeActivityItem(): HomeActivityItem {
        val typeTitle = when (type) {
            MovementType.RECEIPT -> "Material received"
            MovementType.ISSUE -> "Stock issued"
            MovementType.ADJUSTMENT -> "Stock adjustment"
            MovementType.PRODUCTION -> "Production completed"
            MovementType.SCRAP -> "Scrap recorded"
            else -> "Stock movement"
        }
        val diffMilli = System.currentTimeMillis() - createdAt
        val hoursAgo = (diffMilli / (1000 * 60 * 60)).coerceAtLeast(1)
        val timeLabel = if (hoursAgo >= 24) "${hoursAgo / 24}d ago" else "${hoursAgo}h ago"

        return HomeActivityItem(
            id = id,
            title = typeTitle,
            subtitle = reference ?: "Item ID: ${itemId.take(8)}",
            timeAgo = timeLabel,
            type = type,
        )
    }

    private data class UserContext(
        val activePlant: String,
        val isValuationVisible: Boolean,
        val syncStatus: SyncStatus,
        val userName: String,
        val plantName: String,
        val unreadAlertsCount: Int,
    )
}
