package com.company.stockpro.feature.reports

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.company.stockpro.core.designsystem.theme.Radius
import com.company.stockpro.core.designsystem.theme.Spacing
import com.company.stockpro.core.designsystem.theme.StockProCodeTextStyle
import com.company.stockpro.core.designsystem.theme.StockProColors
import com.company.stockpro.core.domain.model.Item
import com.company.stockpro.core.ui.component.SkeletonBox
import com.company.stockpro.core.ui.component.chart.MultiSeriesLineChart
import com.company.stockpro.core.ui.component.chart.StockCompositionDonut

/**
 * Production Reports Screen matching UI mockup media_1789794856398.png:
 * - Date Range Filter
 * - Section Pills (Overview, Inventory, Purchases, Usage)
 * - 4-Card Period KPI Grid
 * - Stock Distribution Donut & Monthly Trend Line Chart
 * - Top 5 Low Stock Items Table
 * - Generate Report Tiles with Android Share Sheet dispatch
 */
@Composable
fun ReportsRoute(
    viewModel: ReportsViewModel,
    onNavigateToInventory: () -> Unit,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Trigger Android share sheet when CSV is generated
    LaunchedEffect(uiState) {
        if (uiState is ReportsUiState.Content) {
            val content = uiState as ReportsUiState.Content
            content.generatedCsvContent?.let { csv ->
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, csv)
                    type = "text/csv"
                }
                val shareIntent = Intent.createChooser(sendIntent, "Export StockPro Report")
                context.startActivity(shareIntent)
                viewModel.clearExportedCsv()
            }
        }
    }

    ReportsScreen(
        uiState = uiState,
        onSectionSelected = { viewModel.onSectionSelected(it) },
        onDateRangeSelected = { viewModel.onDateRangeSelected(it) },
        onExportReportClick = { title, items -> viewModel.exportReportCsv(title, items) },
        onNavigateToInventory = onNavigateToInventory,
        onItemClick = onItemClick,
        modifier = modifier,
    )
}

@Composable
fun ReportsScreen(
    uiState: ReportsUiState,
    onSectionSelected: (ReportSection) -> Unit,
    onDateRangeSelected: (DateRangePreset) -> Unit,
    onExportReportClick: (String, List<Item>) -> Unit,
    onNavigateToInventory: () -> Unit,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StockProColors.Canvas),
    ) {
        when (uiState) {
            is ReportsUiState.Loading -> {
                ReportsLoadingSkeleton()
            }
            is ReportsUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Spacing.screenGutter),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = uiState.message,
                        style = MaterialTheme.typography.titleMedium,
                        color = StockProColors.Danger,
                    )
                }
            }
            is ReportsUiState.Content -> {
                ReportsContent(
                    state = uiState,
                    onSectionSelected = onSectionSelected,
                    onDateRangeSelected = onDateRangeSelected,
                    onExportReportClick = onExportReportClick,
                    onNavigateToInventory = onNavigateToInventory,
                    onItemClick = onItemClick,
                )
            }
        }
    }
}

@Composable
private fun ReportsContent(
    state: ReportsUiState.Content,
    onSectionSelected: (ReportSection) -> Unit,
    onDateRangeSelected: (DateRangePreset) -> Unit,
    onExportReportClick: (String, List<Item>) -> Unit,
    onNavigateToInventory: () -> Unit,
    onItemClick: (String) -> Unit,
) {
    val scrollState = rememberScrollState()
    var dateDropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = Spacing.screenGutter)
            .padding(bottom = Spacing.x6),
    ) {
        Spacer(modifier = Modifier.height(Spacing.x3))

        // 1. Header: Title + Date Range Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "Reports",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = StockProColors.TextPrimary,
                )
                Text(
                    text = "Insights into your inventory and operations",
                    style = MaterialTheme.typography.bodySmall,
                    color = StockProColors.TextSecondary,
                )
            }

            Box {
                Surface(
                    shape = Radius.pill,
                    color = StockProColors.SurfaceAlt,
                    modifier = Modifier
                        .clip(Radius.pill)
                        .clickable { dateDropdownExpanded = true },
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = Spacing.x2, vertical = Spacing.x1),
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = StockProColors.TextSecondary,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(Spacing.x1))
                        Text(
                            text = state.selectedDateRange.label,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = StockProColors.TextPrimary,
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = StockProColors.TextSecondary,
                        )
                    }
                }

                DropdownMenu(
                    expanded = dateDropdownExpanded,
                    onDismissRequest = { dateDropdownExpanded = false },
                ) {
                    state.availableDateRanges.forEach { range ->
                        DropdownMenuItem(
                            text = { Text(range.label) },
                            onClick = {
                                onDateRangeSelected(range)
                                dateDropdownExpanded = false
                            },
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.x3))

        // 2. Section Pills (Overview, Inventory, Purchases, Usage)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Spacing.x2),
        ) {
            ReportSection.entries.forEach { section ->
                val isSelected = section == state.selectedSection
                val containerColor = if (isSelected) StockProColors.BrandMark else StockProColors.SurfaceAlt
                val contentColor = if (isSelected) Color.White else StockProColors.TextSecondary

                Surface(
                    shape = Radius.pill,
                    color = containerColor,
                    modifier = Modifier
                        .clip(Radius.pill)
                        .clickable { onSectionSelected(section) },
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = Spacing.x3, vertical = Spacing.x1),
                    ) {
                        val icon = when (section) {
                            ReportSection.OVERVIEW -> Icons.Default.Widgets
                            ReportSection.INVENTORY -> Icons.Default.Inventory
                            ReportSection.PURCHASES -> Icons.Default.ShoppingCart
                            ReportSection.USAGE -> Icons.Default.BarChart
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(Spacing.x1))
                        Text(
                            text = section.label,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                            color = contentColor,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.x3))

        // 3. 4-Card Period KPI Grid (2x2)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.x2),
        ) {
            PeriodKpiCard(
                title = "Total Items",
                value = String.format("%,d", state.totalItems),
                deltaText = "↑ +${state.totalItemsDeltaPercent}%",
                icon = Icons.Default.Widgets,
                iconTint = StockProColors.BrandMark,
                containerColor = StockProColors.PrimaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.weight(1f),
            )
            PeriodKpiCard(
                title = "Inventory Value",
                value = formatRupees(state.inventoryValuePaise),
                deltaText = "↑ +${state.inventoryValueDeltaPercent}%",
                icon = Icons.Default.ShoppingBag,
                iconTint = StockProColors.Success,
                containerColor = StockProColors.SuccessContainer.copy(alpha = 0.5f),
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(Spacing.x2))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.x2),
        ) {
            PeriodKpiCard(
                title = "Items Purchased",
                value = String.format("%,d", state.itemsPurchased),
                deltaText = "↑ +${state.itemsPurchasedDeltaPercent}%",
                icon = Icons.Default.ShoppingCart,
                iconTint = StockProColors.Warning,
                containerColor = StockProColors.WarningContainer.copy(alpha = 0.5f),
                modifier = Modifier.weight(1f),
            )
            PeriodKpiCard(
                title = "Items Issued",
                value = String.format("%,d", state.itemsIssued),
                deltaText = "↑ +${state.itemsIssuedDeltaPercent}%",
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                iconTint = StockProColors.Primary,
                containerColor = StockProColors.SurfaceAlt,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(Spacing.x3))

        // 4. Charts Row: Stock Distribution & Monthly Trend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.x2),
        ) {
            // Left: Stock Distribution Donut
            Surface(
                shape = Radius.lg,
                color = StockProColors.Surface,
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, StockProColors.OutlineSubtle, Radius.lg),
            ) {
                Column(modifier = Modifier.padding(Spacing.x3)) {
                    Text(
                        text = "Stock Distribution",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = StockProColors.TextPrimary,
                    )
                    Spacer(modifier = Modifier.height(Spacing.x2))
                    StockCompositionDonut(
                        segments = state.stockSegments,
                        totalCount = state.totalItems,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // Right: Monthly Trend Multi-Series Line Chart
            Surface(
                shape = Radius.lg,
                color = StockProColors.Surface,
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, StockProColors.OutlineSubtle, Radius.lg),
            ) {
                Column(modifier = Modifier.padding(Spacing.x3)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Monthly Trend",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = StockProColors.TextPrimary,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = state.trendRangeSelector,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = StockProColors.TextSecondary,
                                ),
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = StockProColors.TextSecondary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.x2))

                    MultiSeriesLineChart(
                        seriesList = state.trendSeries,
                        xLabels = state.trendMonths,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.x3))

        // 5. Top 5 Low Stock Items Table (docs/design.md §5.12 data table spec)
        Surface(
            shape = Radius.lg,
            color = StockProColors.Surface,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, StockProColors.OutlineSubtle, Radius.lg),
        ) {
            Column(modifier = Modifier.padding(Spacing.x3)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Top 5 Low Stock Items",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = StockProColors.TextPrimary,
                    )
                    Text(
                        text = "View All",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = StockProColors.BrandMark,
                            fontSize = 11.sp,
                        ),
                        modifier = Modifier.clickable { onNavigateToInventory() },
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.x2))

                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StockProColors.SurfaceAlt, Radius.xs)
                        .padding(horizontal = Spacing.x2, vertical = Spacing.x1),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "#",
                        style = MaterialTheme.typography.labelSmall,
                        color = StockProColors.TextSecondary,
                        modifier = Modifier.width(24.dp),
                    )
                    Text(
                        text = "Item Name",
                        style = MaterialTheme.typography.labelSmall,
                        color = StockProColors.TextSecondary,
                        modifier = Modifier.weight(1.2f),
                    )
                    Text(
                        text = "Code",
                        style = MaterialTheme.typography.labelSmall,
                        color = StockProColors.TextSecondary,
                        modifier = Modifier.weight(0.8f),
                    )
                    Text(
                        text = "Current Stock",
                        style = MaterialTheme.typography.labelSmall,
                        color = StockProColors.TextSecondary,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "Reorder Level",
                        style = MaterialTheme.typography.labelSmall,
                        color = StockProColors.TextSecondary,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f),
                    )
                }

                // Table Rows
                state.lowStockItems.take(5).forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onItemClick(item.id) }
                            .padding(horizontal = Spacing.x2, vertical = Spacing.x2),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = StockProCodeTextStyle.copy(fontSize = 11.sp),
                            color = StockProColors.TextSecondary,
                            modifier = Modifier.width(24.dp),
                        )
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = StockProColors.TextPrimary,
                            maxLines = 1,
                            modifier = Modifier.weight(1.2f),
                        )
                        Text(
                            text = item.code,
                            style = StockProCodeTextStyle.copy(
                                fontSize = 11.sp,
                                color = StockProColors.BrandMark,
                            ),
                            modifier = Modifier.weight(0.8f),
                        )
                        Text(
                            text = item.onHand.toUnitsDouble.toLong().toString(),
                            style = StockProCodeTextStyle.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = StockProColors.Danger,
                            ),
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = item.reorderLevel.toUnitsDouble.toLong().toString(),
                            style = StockProCodeTextStyle.copy(fontSize = 12.sp),
                            color = StockProColors.TextSecondary,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (index < 4) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(StockProColors.OutlineSubtle),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.x3))

        // 6. Generate Report Section (4 Action Tiles)
        Surface(
            shape = Radius.lg,
            color = StockProColors.Surface,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, StockProColors.OutlineSubtle, Radius.lg),
        ) {
            Column(modifier = Modifier.padding(Spacing.x3)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Generate Report",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = StockProColors.TextPrimary,
                    )
                    Text(
                        text = "View All",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = StockProColors.BrandMark,
                            fontSize = 11.sp,
                        ),
                        modifier = Modifier.clickable { onNavigateToInventory() },
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.x2))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.x2),
                ) {
                    ReportActionTile(
                        title = "Inventory\nReport",
                        icon = Icons.Default.Description,
                        iconTint = StockProColors.BrandMark,
                        containerColor = StockProColors.PrimaryContainer.copy(alpha = 0.5f),
                        onClick = { onExportReportClick("Inventory Report", state.lowStockItems) },
                        modifier = Modifier.weight(1f),
                    )
                    ReportActionTile(
                        title = "Purchase\nReport",
                        icon = Icons.Default.ShoppingCart,
                        iconTint = StockProColors.Success,
                        containerColor = StockProColors.SuccessContainer.copy(alpha = 0.5f),
                        onClick = { onExportReportClick("Purchase Report", state.lowStockItems) },
                        modifier = Modifier.weight(1f),
                    )
                    ReportActionTile(
                        title = "Usage\nReport",
                        icon = Icons.Default.BarChart,
                        iconTint = StockProColors.Warning,
                        containerColor = StockProColors.WarningContainer.copy(alpha = 0.5f),
                        onClick = { onExportReportClick("Usage Report", state.lowStockItems) },
                        modifier = Modifier.weight(1f),
                    )
                    ReportActionTile(
                        title = "Stock\nValuation",
                        icon = Icons.Default.PieChart,
                        iconTint = StockProColors.Primary,
                        containerColor = StockProColors.SurfaceAlt,
                        onClick = { onExportReportClick("Stock Valuation", state.lowStockItems) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun PeriodKpiCard(
    title: String,
    value: String,
    deltaText: String,
    icon: ImageVector,
    iconTint: Color,
    containerColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = Radius.lg,
        color = containerColor,
        modifier = modifier.border(1.dp, StockProColors.OutlineSubtle, Radius.lg),
    ) {
        Column(modifier = Modifier.padding(Spacing.x3)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(Radius.sm)
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp),
                )
            }

            Spacer(modifier = Modifier.height(Spacing.x2))

            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = StockProColors.TextSecondary,
            )

            Spacer(modifier = Modifier.height(Spacing.x1))

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = StockProColors.TextPrimary,
            )

            Spacer(modifier = Modifier.height(Spacing.x1))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = deltaText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = StockProColors.Success,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                    ),
                )
                Spacer(modifier = Modifier.width(Spacing.x1))
                Text(
                    text = "vs last month",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                    color = StockProColors.TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun ReportActionTile(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = Radius.md,
        color = containerColor,
        modifier = modifier
            .clip(Radius.md)
            .clickable(onClick = onClick)
            .border(1.dp, StockProColors.OutlineSubtle, Radius.md),
    ) {
        Column(
            modifier = Modifier.padding(vertical = Spacing.x3, horizontal = Spacing.x1),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.height(Spacing.x1))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = StockProColors.TextPrimary,
                textAlign = TextAlign.Center,
                fontSize = 10.sp,
                maxLines = 2,
            )
        }
    }
}

private fun formatRupees(paise: Long): String {
    val inRupees = paise / 100.0
    return if (inRupees >= 10_000_000) {
        "₹ ${String.format("%.1f", inRupees / 10_000_000.0)}M"
    } else if (inRupees >= 100_000) {
        "₹ ${String.format("%.1f", inRupees / 100_000.0)}L"
    } else {
        "₹ ${String.format("%,.0f", inRupees)}"
    }
}

@Composable
private fun ReportsLoadingSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.screenGutter),
    ) {
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(28.dp),
        )
        Spacer(modifier = Modifier.height(Spacing.x3))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.x2)) {
            repeat(4) {
                SkeletonBox(
                    modifier = Modifier
                        .width(70.dp)
                        .height(32.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(Spacing.x3))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.x2)) {
            SkeletonBox(
                modifier = Modifier
                    .weight(1f)
                    .height(110.dp),
            )
            SkeletonBox(
                modifier = Modifier
                    .weight(1f)
                    .height(110.dp),
            )
        }
        Spacer(modifier = Modifier.height(Spacing.x3))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.x2)) {
            SkeletonBox(
                modifier = Modifier
                    .weight(1f)
                    .height(160.dp),
            )
            SkeletonBox(
                modifier = Modifier
                    .weight(1f)
                    .height(160.dp),
            )
        }
    }
}
