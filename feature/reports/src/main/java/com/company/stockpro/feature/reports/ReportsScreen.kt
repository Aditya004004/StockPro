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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import com.company.stockpro.core.designsystem.component.ButtonVariant
import com.company.stockpro.core.designsystem.component.StockProButton
import com.company.stockpro.core.designsystem.theme.Radius
import com.company.stockpro.core.designsystem.theme.Spacing
import com.company.stockpro.core.designsystem.theme.StockProCodeTextStyle
import com.company.stockpro.core.designsystem.theme.StockProColors
import com.company.stockpro.core.domain.model.Item
import com.company.stockpro.core.ui.component.SkeletonBox
import com.company.stockpro.core.ui.component.chart.MultiSeriesLineChart
import com.company.stockpro.core.ui.component.chart.StockCompositionDonut

/**
 * Precision Industrial Reports & Analytics Screen.
 * Faithful implementation of Stitch Industrial Reports (screenshot_stitch_reports.png).
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
            .padding(bottom = 96.dp),
    ) {
        // 1. Top Industrial Telemetry Ribbon
        ReportsTelemetryStrip()

        Column(modifier = Modifier.padding(horizontal = Spacing.screenGutter)) {
            Spacer(modifier = Modifier.height(Spacing.x2))

            // 2. Header: Title + Export CSV Trigger
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "REPORTS & ANALYTICS",
                        style = StockProCodeTextStyle.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = StockProColors.TextSecondary,
                        ),
                    )
                    Text(
                        text = "Plant 01 Production",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.3).sp,
                        ),
                        color = StockProColors.TextPrimary,
                    )
                }

                Surface(
                    shape = Radius.pill,
                    color = StockProColors.Primary,
                    modifier = Modifier
                        .clip(Radius.pill)
                        .clickable { onExportReportClick("Full Inventory Report", state.lowStockItems) },
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = Spacing.x3, vertical = 6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Export CSV",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Export CSV",
                            style = StockProCodeTextStyle.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            ),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.x2))

            // 3. Date Range Selector Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Spacing.x2),
            ) {
                state.availableDateRanges.forEach { range ->
                    val isSelected = range == state.selectedDateRange
                    val bgColor = if (isSelected) StockProColors.Primary else StockProColors.Surface
                    val textColor = if (isSelected) Color.White else StockProColors.TextSecondary
                    val borderColor = if (isSelected) StockProColors.Primary else StockProColors.OutlineSubtle

                    Surface(
                        shape = Radius.pill,
                        color = bgColor,
                        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                        modifier = Modifier
                            .clip(Radius.pill)
                            .clickable { onDateRangeSelected(range) },
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = Spacing.x3, vertical = 6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = textColor,
                                modifier = Modifier.size(13.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = range.label,
                                style = StockProCodeTextStyle.copy(
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = textColor,
                                ),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.x3))

            // 4. 2x2 Operational KPI Grid
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.x2)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.x2),
                ) {
                    ReportsKpiCard(
                        title = "TOTAL ITEMS",
                        value = String.format("%,d", state.totalItems),
                        deltaText = "+${state.totalItemsDeltaPercent}%",
                        subText = "active catalog",
                        accentColor = StockProColors.Primary,
                        modifier = Modifier.weight(1f),
                    )
                    ReportsKpiCard(
                        title = "VALUATION",
                        value = formatRupees(state.inventoryValuePaise),
                        deltaText = "+${state.inventoryValueDeltaPercent}%",
                        subText = "asset value",
                        accentColor = StockProColors.Success,
                        modifier = Modifier.weight(1f),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.x2),
                ) {
                    ReportsKpiCard(
                        title = "INBOUND RECV",
                        value = String.format("%,d", state.itemsPurchased),
                        deltaText = "+${state.itemsPurchasedDeltaPercent}%",
                        subText = "units received",
                        accentColor = StockProColors.Warning,
                        modifier = Modifier.weight(1f),
                    )
                    ReportsKpiCard(
                        title = "DISPATCHED",
                        value = String.format("%,d", state.itemsIssued),
                        deltaText = "+${state.itemsIssuedDeltaPercent}%",
                        subText = "units issued",
                        accentColor = StockProColors.Production,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.x3))

            // 5. Inbound vs Outbound Trend Line Chart (Full Width Card)
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
                        Column {
                            Text(
                                text = "INBOUND VS OUTBOUND FLOW",
                                style = StockProCodeTextStyle.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StockProColors.TextSecondary,
                                ),
                            )
                            Text(
                                text = "Monthly material turnover velocity",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = StockProColors.TextTertiary,
                            )
                        }

                        // Chart Legend
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.x2),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(StockProColors.Success),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Inbound",
                                    style = StockProCodeTextStyle.copy(fontSize = 10.sp, color = StockProColors.TextSecondary),
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(StockProColors.Primary),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Outbound",
                                    style = StockProCodeTextStyle.copy(fontSize = 10.sp, color = StockProColors.TextSecondary),
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.x3))

                    MultiSeriesLineChart(
                        seriesList = state.trendSeries,
                        xLabels = state.trendMonths,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.x3))

            // 6. Valuation by Category Progress Breakdown (Full Width Card)
            Surface(
                shape = Radius.lg,
                color = StockProColors.Surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StockProColors.OutlineSubtle, Radius.lg),
            ) {
                Column(modifier = Modifier.padding(Spacing.x3)) {
                    Text(
                        text = "VALUATION BY CATEGORY",
                        style = StockProCodeTextStyle.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = StockProColors.TextSecondary,
                        ),
                    )
                    Spacer(modifier = Modifier.height(Spacing.x2))

                    CategoryValuationRow(name = "Components", percentage = 42, amount = "₹ 20.2 Lakh", color = StockProColors.Primary)
                    Spacer(modifier = Modifier.height(Spacing.x2))
                    CategoryValuationRow(name = "Raw Materials", percentage = 30, amount = "₹ 14.5 Lakh", color = StockProColors.Success)
                    Spacer(modifier = Modifier.height(Spacing.x2))
                    CategoryValuationRow(name = "Finished Goods", percentage = 21, amount = "₹ 10.1 Lakh", color = StockProColors.Production)
                    Spacer(modifier = Modifier.height(Spacing.x2))
                    CategoryValuationRow(name = "Tools & Equipment", percentage = 7, amount = "₹ 3.4 Lakh", color = StockProColors.Warning)
                }
            }

            Spacer(modifier = Modifier.height(Spacing.x3))

            // 7. Stock Distribution Donut Card (Full Width)
            Surface(
                shape = Radius.lg,
                color = StockProColors.Surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StockProColors.OutlineSubtle, Radius.lg),
            ) {
                Column(modifier = Modifier.padding(Spacing.x3)) {
                    Text(
                        text = "STOCK COMPOSITION DISTRIBUTION",
                        style = StockProCodeTextStyle.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = StockProColors.TextSecondary,
                        ),
                    )
                    Spacer(modifier = Modifier.height(Spacing.x2))

                    StockCompositionDonut(
                        segments = state.stockSegments,
                        totalCount = state.totalItems,
                        centerLabel = "Total SKUs",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.x3))

            // 8. Top 5 Low Stock Critical Table
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
                            text = "TOP REORDER PRIORITIES",
                            style = StockProCodeTextStyle.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = StockProColors.TextSecondary,
                            ),
                        )
                        Text(
                            text = "Catalog >",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = StockProColors.Primary,
                                fontWeight = FontWeight.SemiBold,
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
                            .padding(horizontal = Spacing.x2, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "#",
                            style = StockProCodeTextStyle.copy(fontSize = 10.sp, color = StockProColors.TextTertiary),
                            modifier = Modifier.width(22.dp),
                        )
                        Text(
                            text = "Item Name",
                            style = StockProCodeTextStyle.copy(fontSize = 10.sp, color = StockProColors.TextTertiary),
                            modifier = Modifier.weight(1.3f),
                        )
                        Text(
                            text = "SKU",
                            style = StockProCodeTextStyle.copy(fontSize = 10.sp, color = StockProColors.TextTertiary),
                            modifier = Modifier.weight(0.9f),
                        )
                        Text(
                            text = "On Hand",
                            style = StockProCodeTextStyle.copy(fontSize = 10.sp, color = StockProColors.TextTertiary),
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(0.8f),
                        )
                        Text(
                            text = "Min Reorder",
                            style = StockProCodeTextStyle.copy(fontSize = 10.sp, color = StockProColors.TextTertiary),
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(0.9f),
                        )
                    }

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
                                style = StockProCodeTextStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                color = StockProColors.TextSecondary,
                                modifier = Modifier.width(22.dp),
                            )
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = StockProColors.TextPrimary,
                                maxLines = 1,
                                modifier = Modifier.weight(1.3f),
                            )
                            Text(
                                text = item.code,
                                style = StockProCodeTextStyle.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StockProColors.Primary,
                                ),
                                modifier = Modifier.weight(0.9f),
                            )
                            Text(
                                text = item.onHand.toUnitsDouble.toLong().toString(),
                                style = StockProCodeTextStyle.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = StockProColors.Danger,
                                ),
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(0.8f),
                            )
                            Text(
                                text = item.reorderLevel.toUnitsDouble.toLong().toString(),
                                style = StockProCodeTextStyle.copy(fontSize = 11.sp, color = StockProColors.TextSecondary),
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(0.9f),
                            )
                        }

                        if (index < 4) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(0.5.dp)
                                    .background(StockProColors.OutlineSubtle),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.x3))

            // 9. Export Report Action Tiles
            Surface(
                shape = Radius.lg,
                color = StockProColors.Surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StockProColors.OutlineSubtle, Radius.lg),
            ) {
                Column(modifier = Modifier.padding(Spacing.x3)) {
                    Text(
                        text = "EXPORT REPORT PACKS",
                        style = StockProCodeTextStyle.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = StockProColors.TextSecondary,
                        ),
                    )
                    Spacer(modifier = Modifier.height(Spacing.x2))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.x2),
                    ) {
                        ReportActionTile(
                            title = "Valuation\nLedger",
                            icon = Icons.Default.Description,
                            color = StockProColors.Primary,
                            containerColor = StockProColors.PrimaryContainer.copy(alpha = 0.5f),
                            onClick = { onExportReportClick("Valuation Ledger", state.lowStockItems) },
                            modifier = Modifier.weight(1f),
                        )
                        ReportActionTile(
                            title = "Purchase\nHistory",
                            icon = Icons.Default.ShoppingCart,
                            color = StockProColors.Success,
                            containerColor = StockProColors.SuccessContainer.copy(alpha = 0.5f),
                            onClick = { onExportReportClick("Purchase Report", state.lowStockItems) },
                            modifier = Modifier.weight(1f),
                        )
                        ReportActionTile(
                            title = "Usage &\nIssues",
                            icon = Icons.Default.BarChart,
                            color = StockProColors.Warning,
                            containerColor = StockProColors.WarningContainer.copy(alpha = 0.5f),
                            onClick = { onExportReportClick("Usage Report", state.lowStockItems) },
                            modifier = Modifier.weight(1f),
                        )
                        ReportActionTile(
                            title = "Reorder\nAudit",
                            icon = Icons.Default.PieChart,
                            color = StockProColors.Production,
                            containerColor = StockProColors.ProductionContainer.copy(alpha = 0.5f),
                            onClick = { onExportReportClick("Reorder Audit", state.lowStockItems) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

// ── Reports Sub-Components ──

@Composable
private fun ReportsTelemetryStrip() {
    Surface(
        color = StockProColors.SurfaceAlt,
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 0.5.dp, color = StockProColors.OutlineSubtle),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.screenGutter, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(StockProColors.Success),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "SYNC ONLINE",
                    style = StockProCodeTextStyle.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = StockProColors.Success,
                    ),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "•  LEDGER VERIFIED",
                    style = StockProCodeTextStyle.copy(
                        fontSize = 10.sp,
                        color = StockProColors.TextTertiary,
                    ),
                )
            }

            Text(
                text = "EXPORT READY (CSV)",
                style = StockProCodeTextStyle.copy(
                    fontSize = 10.sp,
                    color = StockProColors.TextTertiary,
                ),
            )
        }
    }
}

@Composable
private fun ReportsKpiCard(
    title: String,
    value: String,
    deltaText: String,
    subText: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = Radius.lg,
        color = StockProColors.Surface,
        modifier = modifier.border(1.dp, StockProColors.OutlineSubtle, Radius.lg),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(86.dp)
                    .background(accentColor),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = Spacing.x2, vertical = Spacing.x2),
            ) {
                Text(
                    text = title,
                    style = StockProCodeTextStyle.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = StockProColors.TextSecondary,
                    ),
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.3).sp,
                    ),
                    color = StockProColors.TextPrimary,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = Radius.xs,
                        color = StockProColors.SuccessContainer,
                    ) {
                        Text(
                            text = deltaText,
                            style = StockProCodeTextStyle.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = StockProColors.Success,
                            ),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = subText,
                        style = StockProCodeTextStyle.copy(fontSize = 9.sp, color = StockProColors.TextTertiary),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryValuationRow(
    name: String,
    percentage: Int,
    amount: String,
    color: Color,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = StockProColors.TextPrimary,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$percentage%  ",
                    style = StockProCodeTextStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color),
                )
                Text(
                    text = "($amount)",
                    style = StockProCodeTextStyle.copy(fontSize = 11.sp, color = StockProColors.TextSecondary),
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { percentage / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(Radius.pill),
            color = color,
            trackColor = StockProColors.OutlineSubtle,
        )
    }
}

@Composable
private fun ReportActionTile(
    title: String,
    icon: ImageVector,
    color: Color,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = Radius.md,
        color = StockProColors.Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, StockProColors.OutlineSubtle),
        modifier = modifier
            .clip(Radius.md)
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(vertical = Spacing.x2, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(Radius.sm)
                    .background(containerColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(modifier = Modifier.height(Spacing.x1))
            Text(
                text = title,
                style = StockProCodeTextStyle.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = StockProColors.TextPrimary,
                ),
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}

private fun formatRupees(paise: Long): String {
    val inRupees = paise / 100.0
    return if (inRupees >= 10_000_000) {
        "₹ ${String.format("%.1f", inRupees / 10_000_000.0)} Cr"
    } else if (inRupees >= 100_000) {
        "₹ ${String.format("%.1f", inRupees / 100_000.0)} Lakh"
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
        SkeletonBox(modifier = Modifier.fillMaxWidth(0.5f).height(24.dp))
        Spacer(modifier = Modifier.height(Spacing.x3))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.x2)) {
            repeat(4) {
                SkeletonBox(modifier = Modifier.width(80.dp).height(30.dp), shape = Radius.pill)
            }
        }
        Spacer(modifier = Modifier.height(Spacing.x3))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.x2)) {
            SkeletonBox(modifier = Modifier.weight(1f).height(90.dp))
            SkeletonBox(modifier = Modifier.weight(1f).height(90.dp))
        }
        Spacer(modifier = Modifier.height(Spacing.x3))
        SkeletonBox(modifier = Modifier.fillMaxWidth().height(160.dp))
    }
}
