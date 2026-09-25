package com.company.stockpro.feature.reports

import com.company.stockpro.core.domain.model.Item
import com.company.stockpro.core.ui.component.chart.ChartSeries
import com.company.stockpro.core.ui.component.chart.StockSegment

enum class ReportSection(val label: String) {
    OVERVIEW("Overview"),
    INVENTORY("Inventory"),
    PURCHASES("Purchases"),
    USAGE("Usage"),
}

enum class DateRangePreset(val label: String) {
    CURRENT_MONTH("Jun 1, 2024 - Jun 30, 2024"),
    LAST_MONTH("May 1, 2024 - May 31, 2024"),
    LAST_30_DAYS("Last 30 Days"),
    LAST_6_MONTHS("Last 6 Months"),
}

sealed interface ReportsUiState {
    data object Loading : ReportsUiState

    data class Content(
        val selectedSection: ReportSection = ReportSection.OVERVIEW,
        val selectedDateRange: DateRangePreset = DateRangePreset.CURRENT_MONTH,
        val availableDateRanges: List<DateRangePreset> = DateRangePreset.entries,
        // 4-Card Period KPI Grid (Total Items, Inventory Value, Items Purchased, Items Issued)
        val totalItems: Long,
        val totalItemsDeltaPercent: Int = 0,
        val inventoryValuePaise: Long,
        val inventoryValueDeltaPercent: Float = 0f,
        val itemsPurchased: Long,
        val itemsPurchasedDeltaPercent: Int = 0,
        val itemsIssued: Long,
        val itemsIssuedDeltaPercent: Int = 0,
        // Visual Charts
        val stockSegments: List<StockSegment>,
        val trendSeries: List<ChartSeries>,
        val trendMonths: List<String>,
        val trendRangeSelector: String = "6 Months",
        // Top 5 Low Stock Items Table
        val lowStockItems: List<Item>,
        // Export Status
        val isExporting: Boolean = false,
        val generatedCsvContent: String? = null,
    ) : ReportsUiState

    data class Error(val message: String) : ReportsUiState
}
