package com.company.stockpro.feature.reports

import com.company.stockpro.core.domain.model.Currency
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
    CURRENT_MONTH("Current Month"),
    LAST_MONTH("Last Month"),
    LAST_30_DAYS("Last 30 Days"),
    LAST_6_MONTHS("Last 6 Months"),
    ALL_TIME("All Time");

    companion object {
        val THIS_MONTH = CURRENT_MONTH
    }
}

data class CategoryValuation(
    val categoryName: String,
    val valuePaise: Long,
    val percentage: Int,
    val itemCount: Int,
)

sealed interface ReportsUiState {
    data object Loading : ReportsUiState

    data class Content(
        val selectedSection: ReportSection = ReportSection.OVERVIEW,
        val selectedDateRange: DateRangePreset = DateRangePreset.CURRENT_MONTH,
        val availableDateRanges: List<DateRangePreset> = DateRangePreset.entries,
        val selectedMonthLabel: String = "",
        val canGoNextMonth: Boolean = true,
        val activePlant: String = "Plant 1",
        val availablePlants: List<String> = emptyList(),
        val currency: Currency = Currency.INR,
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
        // Category Valuations
        val categoryValuations: List<CategoryValuation> = emptyList(),
        // Top 5 Low Stock Items Table
        val lowStockItems: List<Item>,
        // Export Status
        val isExporting: Boolean = false,
        val generatedCsvContent: String? = null,
    ) : ReportsUiState

    data class Error(val message: String) : ReportsUiState
}
