package com.company.stockpro.feature.reports

import android.content.Intent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.outlined.Inventory2
import com.company.stockpro.core.domain.model.Currency
import com.company.stockpro.core.domain.model.Money
import com.company.stockpro.core.ui.component.chart.ChartSeries
import java.time.YearMonth
import java.util.Locale
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.company.stockpro.core.ui.component.chart.StockCompositionDonut

/**
 * Precision Industrial Reports & Analytics Screen.
 * Exact implementation matching Stitch Industrial Reports design (media_1790411923555.png).
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
        onPreviousMonth = { viewModel.onPreviousMonth() },
        onNextMonth = { viewModel.onNextMonth() },
        onMonthSelected = { viewModel.onMonthSelected(it) },
        onSelectPlant = { viewModel.selectPlant(it) },
        onExportReportClick = { title, items ->
            val curr = (uiState as? ReportsUiState.Content)?.currency ?: Currency.INR
            viewModel.exportReportCsv(title, items, curr)
        },
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
    onPreviousMonth: () -> Unit = {},
    onNextMonth: () -> Unit = {},
    onMonthSelected: (YearMonth) -> Unit = {},
    onSelectPlant: (String) -> Unit = {},
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
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                    onMonthSelected = onMonthSelected,
                    onSelectPlant = onSelectPlant,
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
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onMonthSelected: (YearMonth) -> Unit,
    onSelectPlant: (String) -> Unit,
    onExportReportClick: (String, List<Item>) -> Unit,
    onNavigateToInventory: () -> Unit,
    onItemClick: (String) -> Unit,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 96.dp),
    ) {
        // 1. Top Industrial Telemetry Ribbon
        ReportsTelemetryStrip()

        Column(modifier = Modifier.padding(horizontal = Spacing.screenGutter)) {
            Spacer(modifier = Modifier.height(14.dp))

            // 2. Header Row: Title + Plant Selector + Export CSV Button
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
                            color = Color(0xFF64748B),
                        ),
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    var plantDropdownExpanded by remember { mutableStateOf(false) }
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { plantDropdownExpanded = true }
                                .padding(vertical = 2.dp),
                        ) {
                            Text(
                                text = state.activePlant,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    letterSpacing = (-0.3).sp,
                                ),
                                color = Color(0xFF0F172A),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Select plant",
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(20.dp),
                            )
                        }

                        DropdownMenu(
                            expanded = plantDropdownExpanded,
                            onDismissRequest = { plantDropdownExpanded = false },
                        ) {
                            state.availablePlants.forEach { plant ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = plant,
                                            style = StockProCodeTextStyle.copy(
                                                fontWeight = if (plant == state.activePlant) FontWeight.Bold else FontWeight.Normal,
                                                color = if (plant == state.activePlant) Color(0xFF2563EB) else Color(0xFF0F172A),
                                            ),
                                        )
                                    },
                                    onClick = {
                                        onSelectPlant(plant)
                                        plantDropdownExpanded = false
                                    },
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Track inventory performance and insights",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                        ),
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF2563EB),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onExportReportClick("Full Inventory Report", state.lowStockItems) },
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Export CSV",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Export CSV",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                            ),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Date Range Selector Pills
            DateFiltersRow(
                selectedRange = state.selectedDateRange,
                availableRanges = state.availableDateRanges,
                selectedMonthLabel = state.selectedMonthLabel,
                canGoNextMonth = state.canGoNextMonth,
                onDateRangeSelected = onDateRangeSelected,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                onMonthSelected = onMonthSelected,
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 4. 2x2 Operational KPI Grid (with Mini Bar Graphics)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ReportsKpiCard(
                        title = "TOTAL ITEMS",
                        value = String.format("%,d", state.totalItems),
                        deltaText = "+${state.totalItemsDeltaPercent}%",
                        subText = "active catalog",
                        icon = Icons.Default.Inventory2,
                        iconColor = Color(0xFF2563EB),
                        iconContainerColor = Color(0xFFEEF2FF),
                        miniBarColor = Color(0xFFBFDBFE),
                        modifier = Modifier.weight(1f),
                    )
                    ReportsKpiCard(
                        title = "VALUATION",
                        value = formatCompactMoney(state.inventoryValuePaise, state.currency),
                        deltaText = "+${String.format("%.1f", state.inventoryValueDeltaPercent)}%",
                        subText = "asset value",
                        icon = Icons.Default.Paid,
                        iconColor = Color(0xFF16A34A),
                        iconContainerColor = Color(0xFFDCFCE7),
                        miniBarColor = Color(0xFFBBF7D0),
                        modifier = Modifier.weight(1f),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ReportsKpiCard(
                        title = "INBOUND RECEIVED",
                        value = String.format("%,d", state.itemsPurchased),
                        deltaText = "+${state.itemsPurchasedDeltaPercent}%",
                        subText = "units received",
                        icon = Icons.Default.Download,
                        iconColor = Color(0xFFF59E0B),
                        iconContainerColor = Color(0xFFFEF3C7),
                        miniBarColor = Color(0xFFFED7AA),
                        leftStripeColor = Color(0xFFF59E0B),
                        modifier = Modifier.weight(1f),
                    )
                    ReportsKpiCard(
                        title = "DISPATCHED",
                        value = String.format("%,d", state.itemsIssued),
                        deltaText = "+${state.itemsIssuedDeltaPercent}%",
                        subText = "units issued",
                        icon = Icons.Default.Upload,
                        iconColor = Color(0xFF9333EA),
                        iconContainerColor = Color(0xFFF3E8FF),
                        miniBarColor = Color(0xFFDDD6FE),
                        leftStripeColor = Color(0xFF9333EA),
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Inbound vs Outbound Flow Card (Dual Smooth Gradient Area Curves)
            InboundOutboundChartCard(
                trendSeries = state.trendSeries,
                trendMonths = state.trendMonths,
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 6. Valuation by Category Progress Breakdown (with % Share / Value toggle)
            ValuationByCategoryCard(
                categoryValuations = state.categoryValuations,
                currency = state.currency,
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 7. Stock Composition Donut Card (Side by Side Donut & Legend Table)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = StockProColors.Surface,
                border = BorderStroke(1.dp, StockProColors.OutlineSubtle),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "STOCK COMPOSITION DISTRIBUTION",
                        style = StockProCodeTextStyle.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = StockProColors.TextSecondary,
                        ),
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    StockCompositionDonut(
                        segments = state.stockSegments,
                        totalCount = state.totalItems,
                        centerLabel = "Total SKUs",
                        isSideBySide = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 8. Top Reorder Priorities Card (Empty State / Critical Table)
            TopReorderPrioritiesCard(
                lowStockItems = state.lowStockItems,
                onNavigateToInventory = onNavigateToInventory,
                onItemClick = onItemClick,
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 9. Export Report Action Tiles
            ExportReportPacksCard(
                onExportReportClick = { title -> onExportReportClick(title, state.lowStockItems) },
            )
        }
    }
}

// ── 1. Telemetry Strip ──

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
                    text = "•",
                    style = StockProCodeTextStyle.copy(
                        fontSize = 10.sp,
                        color = StockProColors.TextTertiary,
                    ),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "LEDGER VERIFIED",
                    style = StockProCodeTextStyle.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = StockProColors.TextSecondary,
                    ),
                )
            }

            Text(
                text = "EXPORT READY (CSV)",
                style = StockProCodeTextStyle.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = StockProColors.Primary,
                ),
            )
        }
    }
}

// ── 3. Date Filters Row ──

@Composable
private fun DateFiltersRow(
    selectedRange: DateRangePreset,
    availableRanges: List<DateRangePreset>,
    selectedMonthLabel: String,
    canGoNextMonth: Boolean,
    onDateRangeSelected: (DateRangePreset) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onMonthSelected: (YearMonth) -> Unit,
) {
    var dateDropdownExpanded by remember { mutableStateOf(false) }
    var monthDropdownExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Preset Range Pill
        Box(modifier = Modifier.weight(1f)) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = StockProColors.Surface,
                border = BorderStroke(1.dp, StockProColors.Outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { dateDropdownExpanded = true },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false),
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = StockProColors.Primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = selectedRange.label,
                            color = StockProColors.Primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = StockProColors.Primary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            DropdownMenu(
                expanded = dateDropdownExpanded,
                onDismissRequest = { dateDropdownExpanded = false },
            ) {
                availableRanges.forEach { preset ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = preset.label,
                                style = StockProCodeTextStyle.copy(
                                    fontWeight = if (preset == selectedRange) FontWeight.Bold else FontWeight.Normal,
                                    color = if (preset == selectedRange) StockProColors.Primary else StockProColors.TextPrimary,
                                ),
                            )
                        },
                        onClick = {
                            onDateRangeSelected(preset)
                            dateDropdownExpanded = false
                        },
                    )
                }
            }
        }

        // Month Navigation Pill (Previous Month < · Month Label · Next Month >)
        Box(modifier = Modifier.weight(1.2f)) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = StockProColors.Surface,
                border = BorderStroke(1.dp, StockProColors.Outline),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    // Previous Month Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onPreviousMonth)
                            .padding(6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Previous Month",
                            tint = StockProColors.Primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }

                    // Month Name (Clickable for quick jump)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { monthDropdownExpanded = true }
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = selectedMonthLabel.ifBlank { "Month" },
                            color = StockProColors.TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                    }

                    // Next Month Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(enabled = canGoNextMonth, onClick = onNextMonth)
                            .padding(6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Next Month",
                            tint = if (canGoNextMonth) StockProColors.Primary else StockProColors.TextTertiary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            DropdownMenu(
                expanded = monthDropdownExpanded,
                onDismissRequest = { monthDropdownExpanded = false },
            ) {
                val currentYearMonth = YearMonth.now()
                val monthFormatter = java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)
                (0..11).forEach { offset ->
                    val ym = currentYearMonth.minusMonths(offset.toLong())
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = ym.format(monthFormatter),
                                style = StockProCodeTextStyle.copy(
                                    fontWeight = if (ym.format(monthFormatter) == selectedMonthLabel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (ym.format(monthFormatter) == selectedMonthLabel) StockProColors.Primary else StockProColors.TextPrimary,
                                ),
                            )
                        },
                        onClick = {
                            onMonthSelected(ym)
                            monthDropdownExpanded = false
                        },
                    )
                }
            }
        }
    }
}

// ── 4. 2x2 KPI Card ──

@Composable
private fun ReportsKpiCard(
    title: String,
    value: String,
    deltaText: String,
    subText: String,
    icon: ImageVector,
    iconColor: Color,
    iconContainerColor: Color,
    miniBarColor: Color,
    leftStripeColor: Color? = null,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = StockProColors.Surface,
        border = BorderStroke(1.dp, StockProColors.OutlineSubtle),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leftStripeColor?.let { stripeColor ->
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(84.dp)
                        .background(stripeColor),
                )
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(iconContainerColor),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = title,
                            style = StockProCodeTextStyle.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = StockProColors.TextSecondary,
                            ),
                            maxLines = 1,
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = value,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                            ),
                            color = StockProColors.TextPrimary,
                            maxLines = 1,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = StockProColors.SuccessContainer,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                        contentDescription = null,
                                        tint = StockProColors.Success,
                                        modifier = Modifier.size(10.dp),
                                    )
                                    Spacer(modifier = Modifier.width(1.dp))
                                    Text(
                                        text = deltaText,
                                        style = StockProCodeTextStyle.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = StockProColors.Success,
                                        ),
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = subText,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 9.sp,
                                    color = StockProColors.TextSecondary,
                                ),
                                maxLines = 1,
                            )
                        }
                    }
                }

                // Mini 4-bar ascending chart graphic
                Row(
                    modifier = Modifier.height(26.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    val barHeights = listOf(8.dp, 13.dp, 19.dp, 26.dp)
                    barHeights.forEach { height ->
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(height)
                                .clip(RoundedCornerShape(topStart = 1.5.dp, topEnd = 1.5.dp))
                                .background(miniBarColor),
                        )
                    }
                }
            }
        }
    }
}

// ── 5. Inbound vs Outbound Line Chart Card ──

@Composable
private fun InboundOutboundChartCard(
    trendSeries: List<ChartSeries> = emptyList(),
    trendMonths: List<String> = emptyList(),
) {
    val gridLineColor = StockProColors.OutlineSubtle
    val inboundColor = StockProColors.Success
    val outboundColor = StockProColors.Primary

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = StockProColors.Surface,
        border = BorderStroke(1.dp, StockProColors.OutlineSubtle),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
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
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = "Monthly material turnover velocity",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = StockProColors.TextSecondary,
                        ),
                    )
                }

                // Legend
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(inboundColor),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Inbound",
                            style = StockProCodeTextStyle.copy(
                                fontSize = 10.sp,
                                color = StockProColors.TextSecondary,
                            ),
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(outboundColor),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Outbound",
                            style = StockProCodeTextStyle.copy(
                                fontSize = 10.sp,
                                color = StockProColors.TextSecondary,
                            ),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Chart Canvas + Gridlines
            var animationPlayed by remember { mutableStateOf(false) }
            val progress by animateFloatAsState(
                targetValue = if (animationPlayed) 1f else 0f,
                animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                label = "flow_chart_progress",
            )
            LaunchedEffect(Unit) { animationPlayed = true }

            val inboundSeries = trendSeries.find { it.name.equals("Inbound", ignoreCase = true) }
            val outboundSeries = trendSeries.find { it.name.equals("Outbound", ignoreCase = true) }
            val inboundRaw = inboundSeries?.dataPoints ?: emptyList()
            val outboundRaw = outboundSeries?.dataPoints ?: emptyList()
            val maxVal = maxOf(
                (inboundRaw + outboundRaw).maxOrNull() ?: 10f,
                10f,
            )
            val ySteps = listOf(
                String.format(Locale.US, "%.0f", maxVal),
                String.format(Locale.US, "%.0f", maxVal * 0.75f),
                String.format(Locale.US, "%.0f", maxVal * 0.50f),
                String.format(Locale.US, "%.0f", maxVal * 0.25f),
                "0",
            )
            val xLabels = if (trendMonths.isNotEmpty()) trendMonths else listOf("1", "5", "10", "15", "20", "25", "30")

            Row(modifier = Modifier.fillMaxWidth()) {
                // Y-Axis Labels
                Column(
                    modifier = Modifier
                        .height(130.dp)
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End,
                ) {
                    ySteps.forEach { step ->
                        Text(
                            text = step,
                            style = StockProCodeTextStyle.copy(
                                fontSize = 10.sp,
                                color = StockProColors.TextTertiary,
                            ),
                        )
                    }
                }

                // Canvas Area
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .height(130.dp),
                ) {
                    val width = size.width
                    val height = size.height

                    // Gridlines
                    val gridCount = ySteps.size
                    for (i in 0 until gridCount) {
                        val y = (height / (gridCount - 1)) * i
                        drawLine(
                            color = gridLineColor,
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }

                    val outboundPoints = if (outboundRaw.isEmpty()) emptyList() else outboundRaw.mapIndexed { index, value ->
                        val x = if (outboundRaw.size > 1) index * (width / (outboundRaw.size - 1)) else width / 2f
                        val y = (1f - ((value / maxVal) * progress)).coerceIn(0f, 1f) * height
                        Offset(x, y)
                    }

                    val inboundPoints = if (inboundRaw.isEmpty()) emptyList() else inboundRaw.mapIndexed { index, value ->
                        val x = if (inboundRaw.size > 1) index * (width / (inboundRaw.size - 1)) else width / 2f
                        val y = (1f - ((value / maxVal) * progress)).coerceIn(0f, 1f) * height
                        Offset(x, y)
                    }

                    // 1. Draw Outbound Area & Line
                    if (outboundPoints.size > 1) {
                        val outboundPath = Path().apply {
                            moveTo(outboundPoints.first().x, outboundPoints.first().y)
                            for (i in 1 until outboundPoints.size) {
                                val prev = outboundPoints[i - 1]
                                val curr = outboundPoints[i]
                                val cpx1 = prev.x + (curr.x - prev.x) / 2f
                                val cpy1 = prev.y
                                val cpx2 = prev.x + (curr.x - prev.x) / 2f
                                val cpy2 = curr.y
                                cubicTo(cpx1, cpy1, cpx2, cpy2, curr.x, curr.y)
                            }
                        }
                        val outboundArea = Path().apply {
                            addPath(outboundPath)
                            lineTo(width, height)
                            lineTo(0f, height)
                            close()
                        }
                        drawPath(
                            path = outboundArea,
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0x332563EB), Color(0x002563EB)),
                                startY = 0f,
                                endY = height,
                            ),
                        )
                        drawPath(
                            path = outboundPath,
                            color = Color(0xFF2563EB),
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                        )
                        // Terminal dot
                        drawCircle(
                            color = Color(0xFF2563EB),
                            radius = 4.dp.toPx(),
                            center = outboundPoints.last(),
                        )
                    }

                    // 2. Draw Inbound Area & Line
                    if (inboundPoints.size > 1) {
                        val inboundPath = Path().apply {
                            moveTo(inboundPoints.first().x, inboundPoints.first().y)
                            for (i in 1 until inboundPoints.size) {
                                val prev = inboundPoints[i - 1]
                                val curr = inboundPoints[i]
                                val cpx1 = prev.x + (curr.x - prev.x) / 2f
                                val cpy1 = prev.y
                                val cpx2 = prev.x + (curr.x - prev.x) / 2f
                                val cpy2 = curr.y
                                cubicTo(cpx1, cpy1, cpx2, cpy2, curr.x, curr.y)
                            }
                        }
                        val inboundArea = Path().apply {
                            addPath(inboundPath)
                            lineTo(width, height)
                            lineTo(0f, height)
                            close()
                        }
                        drawPath(
                            path = inboundArea,
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0x3316A34A), Color(0x0016A34A)),
                                startY = 0f,
                                endY = height,
                            ),
                        )
                        drawPath(
                            path = inboundPath,
                            color = Color(0xFF16A34A),
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                        )
                        // Terminal dot
                        drawCircle(
                            color = Color(0xFF16A34A),
                            radius = 4.dp.toPx(),
                            center = inboundPoints.last(),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // X-Axis Labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 28.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                xLabels.forEach { label ->
                    Text(
                        text = label,
                        style = StockProCodeTextStyle.copy(
                            fontSize = 10.sp,
                            color = StockProColors.TextTertiary,
                        ),
                    )
                }
            }
        }
    }
}

// ── 6. Valuation by Category Card ──

@Composable
private fun ValuationByCategoryCard(
    categoryValuations: List<CategoryValuation> = emptyList(),
    currency: Currency = Currency.INR,
) {
    var showShareSelected by remember { mutableStateOf(true) }

    val categoryColors = listOf(
        Pair(Color(0xFF2563EB), Color(0xFFEEF2FF)),
        Pair(Color(0xFF16A34A), Color(0xFFDCFCE7)),
        Pair(Color(0xFF9333EA), Color(0xFFF3E8FF)),
        Pair(Color(0xFFF59E0B), Color(0xFFFEF3C7)),
        Pair(Color(0xFF0D9488), Color(0xFFCCFBF1)),
        Pair(Color(0xFFE11D48), Color(0xFFFFE4E6)),
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = StockProColors.Surface,
        border = BorderStroke(1.dp, StockProColors.OutlineSubtle),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header + Segmented Toggle Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "VALUATION BY CATEGORY",
                    style = StockProCodeTextStyle.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = StockProColors.TextSecondary,
                    ),
                )

                // % Share vs Value (<Currency>) Toggle
                Row(
                    modifier = Modifier
                        .background(StockProColors.SurfaceAlt, RoundedCornerShape(8.dp))
                        .padding(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (showShareSelected) StockProColors.Primary else Color.Transparent)
                            .clickable { showShareSelected = true }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = "% Share",
                            style = StockProCodeTextStyle.copy(
                                fontSize = 11.sp,
                                fontWeight = if (showShareSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (showShareSelected) Color.White else StockProColors.TextSecondary,
                            ),
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (!showShareSelected) StockProColors.Primary else Color.Transparent)
                            .clickable { showShareSelected = false }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = "Value (${currency.symbol})",
                            style = StockProCodeTextStyle.copy(
                                fontSize = 11.sp,
                                fontWeight = if (!showShareSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (!showShareSelected) Color.White else StockProColors.TextSecondary,
                            ),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (categoryValuations.isEmpty()) {
                Text(
                    text = "No category valuation data available.",
                    style = MaterialTheme.typography.bodySmall.copy(color = StockProColors.TextTertiary),
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            } else {
                categoryValuations.forEachIndexed { index, cat ->
                    val colorPair = categoryColors[index % categoryColors.size]
                    val formattedMoney = formatCompactMoney(cat.valuePaise, currency)
                    CategoryValuationItem(
                        name = cat.categoryName,
                        primaryText = if (showShareSelected) "${cat.percentage}%" else formattedMoney,
                        secondaryText = if (showShareSelected) "($formattedMoney)" else "(${cat.percentage}%)",
                        percentage = cat.percentage,
                        color = colorPair.first,
                        icon = when (index % 4) {
                            0 -> Icons.Default.Inventory2
                            1 -> Icons.Default.Shield
                            2 -> Icons.Default.BarChart
                            else -> Icons.Default.Build
                        },
                        containerColor = colorPair.second,
                    )
                    if (index < categoryValuations.lastIndex) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryValuationItem(
    name: String,
    primaryText: String,
    secondaryText: String,
    percentage: Int,
    color: Color,
    icon: ImageVector,
    containerColor: Color,
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
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(containerColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                    ),
                    color = StockProColors.TextPrimary,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = primaryText,
                    style = StockProCodeTextStyle.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = color,
                    ),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = secondaryText,
                    style = StockProCodeTextStyle.copy(
                        fontSize = 11.sp,
                        color = StockProColors.TextSecondary,
                    ),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = StockProColors.TextTertiary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        LinearProgressIndicator(
            progress = { percentage / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = StockProColors.OutlineSubtle,
        )
    }
}

// ── 8. Top Reorder Priorities Card ──

@Composable
private fun TopReorderPrioritiesCard(
    lowStockItems: List<Item>,
    onNavigateToInventory: () -> Unit,
    onItemClick: (String) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = StockProColors.Surface,
        border = BorderStroke(1.dp, StockProColors.OutlineSubtle),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
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
                    text = "View Catalog >",
                    style = StockProCodeTextStyle.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = StockProColors.Primary,
                    ),
                    modifier = Modifier.clickable { onNavigateToInventory() },
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Table Column Headers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StockProColors.SurfaceAlt, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
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

            if (lowStockItems.isEmpty()) {
                // Empty State Box matching screenshot
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Inventory2,
                        contentDescription = null,
                        tint = StockProColors.TextTertiary,
                        modifier = Modifier.size(34.dp),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No reorder priorities",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                        ),
                        color = StockProColors.TextPrimary,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Items reaching reorder levels will appear here.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = StockProColors.TextTertiary,
                        ),
                    )
                }
            } else {
                lowStockItems.take(5).forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onItemClick(item.id) }
                            .padding(horizontal = 8.dp, vertical = 8.dp),
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

                    if (index < lowStockItems.take(5).lastIndex) {
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
    }
}

// ── 9. Export Report Action Tiles ──

@Composable
private fun ExportReportPacksCard(
    onExportReportClick: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "EXPORT REPORT PACKS",
            style = StockProCodeTextStyle.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = StockProColors.TextSecondary,
            ),
        )
        Spacer(modifier = Modifier.height(10.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ReportActionTile(
                    title = "Valuation\nLedger",
                    icon = Icons.Default.Description,
                    color = StockProColors.Primary,
                    containerColor = StockProColors.PrimaryContainer,
                    borderColor = StockProColors.Primary.copy(alpha = 0.25f),
                    textColor = StockProColors.TextPrimary,
                    onClick = { onExportReportClick("Valuation Ledger") },
                    modifier = Modifier.weight(1f),
                )
                ReportActionTile(
                    title = "Purchase\nHistory",
                    icon = Icons.Default.ShoppingCart,
                    color = StockProColors.Success,
                    containerColor = StockProColors.SuccessContainer,
                    borderColor = StockProColors.Success.copy(alpha = 0.25f),
                    textColor = StockProColors.TextPrimary,
                    onClick = { onExportReportClick("Purchase History") },
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ReportActionTile(
                    title = "Usage &\nIssues",
                    icon = Icons.Default.BarChart,
                    color = StockProColors.Warning,
                    containerColor = StockProColors.WarningContainer,
                    borderColor = StockProColors.Warning.copy(alpha = 0.25f),
                    textColor = StockProColors.TextPrimary,
                    onClick = { onExportReportClick("Usage & Issues") },
                    modifier = Modifier.weight(1f),
                )
                ReportActionTile(
                    title = "Reorder\nAudit",
                    icon = Icons.Default.PieChart,
                    color = StockProColors.Reserved,
                    containerColor = StockProColors.ReservedContainer,
                    borderColor = StockProColors.Reserved.copy(alpha = 0.25f),
                    textColor = StockProColors.TextPrimary,
                    onClick = { onExportReportClick("Reorder Audit") },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ReportActionTile(
    title: String,
    icon: ImageVector,
    color: Color,
    containerColor: Color,
    borderColor: Color,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        border = BorderStroke(0.5.dp, borderColor),
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = StockProCodeTextStyle.copy(
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        lineHeight = 15.sp,
                    ),
                    maxLines = 2,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

fun formatCompactMoney(paise: Long, currency: Currency): String {
    val units = paise / currency.minorUnitFactor.toDouble()
    return when (currency) {
        Currency.INR -> when {
            units >= 10_000_000.0 -> "₹ ${String.format(Locale.US, "%.1f", units / 10_000_000.0)} Cr"
            units >= 100_000.0 -> "₹ ${String.format(Locale.US, "%.1f", units / 100_000.0)} Lakh"
            units == 0.0 -> "₹ 0"
            else -> "${currency.symbol} ${String.format(Locale.US, "%,.0f", units)}"
        }
        else -> when {
            units >= 1_000_000.0 -> "${currency.symbol}${String.format(Locale.US, "%.1f", units / 1_000_000.0)}M"
            units >= 1_000.0 -> "${currency.symbol}${String.format(Locale.US, "%.1f", units / 1_000.0)}k"
            units == 0.0 -> "${currency.symbol}0"
            else -> "${currency.symbol}${String.format(Locale.US, "%,.2f", units)}"
        }
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
            repeat(2) {
                SkeletonBox(modifier = Modifier.weight(1f).height(36.dp), shape = Radius.pill)
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
