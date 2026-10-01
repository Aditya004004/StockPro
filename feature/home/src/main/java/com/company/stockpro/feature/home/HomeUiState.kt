package com.company.stockpro.feature.home

import com.company.stockpro.core.domain.model.Currency
import com.company.stockpro.core.domain.model.Item
import com.company.stockpro.core.domain.model.MovementType
import com.company.stockpro.core.domain.model.SyncStatus
import com.company.stockpro.core.ui.component.chart.StockSegment

/**
 * MVI UI State for HomeScreen.
 * Adheres to rules.md §5.2 with all 4 async states (Loading, Content, Error).
 */
sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Content(
        val userName: String = "Aditya",
        val activePlant: String = "Plant 1",
        val activeWarehouse: String = "Main Warehouse",
        val availablePlants: List<String> = listOf("Plant 1", "Plant 2"),
        val currency: Currency = Currency.INR,
        val unreadAlertsCount: Int = 3,
        // 4-Card KPI Carousel (Total Items, Raw Materials, Low Stock, Finished Goods)
        val totalItems: Long,
        val totalItemsDeltaPercent: Int = 12,
        val rawMaterialsCount: Long,
        val rawMaterialsDeltaPercent: Int = 8,
        val lowStockCount: Long,
        val lowStockItemsNeedReorder: Long,
        val finishedGoodsCount: Long,
        val finishedGoodsDeltaPercent: Int = 15,
        // Valuation Card + Sparkline
        val totalValuationPaise: Long,
        val valuationDeltaPercent: Float = 6.2f,
        val isValuationVisible: Boolean = true,
        val sparklinePoints: List<Float>,
        val sparklineLabels: List<String>,
        // Stock Status / Composition
        val stockSegments: List<StockSegment>,
        // Recent Activity Feed
        val recentActivities: List<HomeActivityItem>,
        // Top 5 Low Stock Items
        val topLowStockItems: List<Item>,
        val upcomingReordersCount: Int = 3,
        val syncStatus: SyncStatus = SyncStatus.Synced(System.currentTimeMillis()),
    ) : HomeUiState

    data class Error(val message: String) : HomeUiState
}

data class HomeActivityItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val timeAgo: String,
    val type: MovementType,
)
