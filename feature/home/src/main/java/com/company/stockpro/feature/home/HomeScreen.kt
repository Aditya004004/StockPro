package com.company.stockpro.feature.home

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Factory
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.company.stockpro.core.designsystem.component.ButtonVariant
import com.company.stockpro.core.designsystem.component.StatusBadge
import com.company.stockpro.core.designsystem.component.StockProButton
import com.company.stockpro.core.designsystem.theme.Radius
import com.company.stockpro.core.designsystem.theme.Spacing
import com.company.stockpro.core.designsystem.theme.StockProCodeTextStyle
import com.company.stockpro.core.designsystem.theme.StockProColors
import com.company.stockpro.core.domain.model.Item
import com.company.stockpro.core.domain.model.MovementType
import com.company.stockpro.core.domain.model.StockLevel
import com.company.stockpro.core.domain.model.SyncStatus
import com.company.stockpro.core.ui.component.SkeletonBox
import com.company.stockpro.core.ui.component.SyncBanner
import com.company.stockpro.core.ui.component.chart.AreaSparkline
import com.company.stockpro.core.ui.component.chart.StockCompositionDonut

/**
 * Precision Industrial Home Dashboard Screen.
 * Faithful implementation of Stitch Industrial System (screenshot_stitch_home.png).
 */
@Composable
fun HomeRoute(
    viewModel: HomeViewModel,
    onNavigateToInventory: () -> Unit,
    onNavigateToReports: () -> Unit,
    onReceiveStockClick: () -> Unit,
    onIssueStockClick: () -> Unit,
    onItemClick: (String) -> Unit,
    onNotificationsClick: () -> Unit,
    onNeedsAttentionClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreen(
        uiState = uiState,
        onToggleValuation = { viewModel.toggleValuationVisibility() },
        onSelectPlant = { viewModel.selectPlant(it) },
        onNavigateToInventory = onNavigateToInventory,
        onNavigateToReports = onNavigateToReports,
        onReceiveStockClick = onReceiveStockClick,
        onIssueStockClick = onIssueStockClick,
        onItemClick = onItemClick,
        onNotificationsClick = onNotificationsClick,
        onNeedsAttentionClick = onNeedsAttentionClick,
        onRetryClick = { /* Flow in ViewModel is reactive; re-evaluates automatically */ },
        modifier = modifier,
    )
}

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onToggleValuation: () -> Unit,
    onSelectPlant: (String) -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToReports: () -> Unit,
    onReceiveStockClick: () -> Unit,
    onIssueStockClick: () -> Unit,
    onItemClick: (String) -> Unit,
    onNotificationsClick: () -> Unit,
    onNeedsAttentionClick: () -> Unit = {},
    onRetryClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StockProColors.Canvas),
    ) {
        when (uiState) {
            is HomeUiState.Loading -> {
                HomeLoadingSkeleton()
            }
            is HomeUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Spacing.screenGutter),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = StockProColors.Danger,
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(modifier = Modifier.height(Spacing.x2))
                    Text(
                        text = "Unable to load dashboard",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = StockProColors.TextPrimary,
                    )
                    Spacer(modifier = Modifier.height(Spacing.x1))
                    Text(
                        text = uiState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = StockProColors.TextSecondary,
                    )
                    Spacer(modifier = Modifier.height(Spacing.x4))
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.x2)) {
                        StockProButton(
                            text = "Retry",
                            onClick = onRetryClick,
                            variant = ButtonVariant.PRIMARY,
                        )
                        StockProButton(
                            text = "Go to Inventory",
                            onClick = onNavigateToInventory,
                            variant = ButtonVariant.GHOST,
                        )
                    }
                }
            }
            is HomeUiState.Content -> {
                HomeContent(
                    state = uiState,
                    onToggleValuation = onToggleValuation,
                    onSelectPlant = onSelectPlant,
                    onNavigateToInventory = onNavigateToInventory,
                    onNavigateToReports = onNavigateToReports,
                    onReceiveStockClick = onReceiveStockClick,
                    onIssueStockClick = onIssueStockClick,
                    onItemClick = onItemClick,
                    onNotificationsClick = onNotificationsClick,
                    onNeedsAttentionClick = onNeedsAttentionClick,
                )
            }
        }
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState.Content,
    onToggleValuation: () -> Unit,
    onSelectPlant: (String) -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToReports: () -> Unit,
    onReceiveStockClick: () -> Unit,
    onIssueStockClick: () -> Unit,
    onItemClick: (String) -> Unit,
    onNotificationsClick: () -> Unit,
    onNeedsAttentionClick: () -> Unit,
) {
    val scrollState = rememberScrollState()
    var plantDropdownExpanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 96.dp),
    ) {
        // 1. Top Industrial Telemetry Strip
        IndustrialTelemetryStrip(
            syncStatus = state.syncStatus,
            onNeedsAttentionClick = onNeedsAttentionClick,
        )

        Column(modifier = Modifier.padding(horizontal = Spacing.screenGutter)) {
            Spacer(modifier = Modifier.height(Spacing.x2))

            // 2. Industrial App Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(Radius.md)
                            .background(StockProColors.Primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Factory,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(Spacing.x2))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "StockPro",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-0.3).sp,
                                ),
                                color = StockProColors.TextPrimary,
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = Radius.xs,
                                color = StockProColors.PrimaryContainer,
                            ) {
                                Text(
                                    text = "ENTERPRISE",
                                    style = StockProCodeTextStyle.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StockProColors.Primary,
                                    ),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                )
                            }
                        }

                        // Plant Selector Dropdown Pill
                        Box {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable { plantDropdownExpanded = true }
                                    .padding(vertical = 1.dp),
                            ) {
                                Text(
                                    text = state.activePlant.uppercase(),
                                    style = StockProCodeTextStyle.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = StockProColors.TextSecondary,
                                    ),
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select Plant",
                                    tint = StockProColors.TextSecondary,
                                    modifier = Modifier.size(16.dp),
                                )
                            }

                            DropdownMenu(
                                expanded = plantDropdownExpanded,
                                onDismissRequest = { plantDropdownExpanded = false },
                            ) {
                                state.availablePlants.forEach { plant ->
                                    DropdownMenuItem(
                                        text = { Text(plant) },
                                        onClick = {
                                            onSelectPlant(plant)
                                            plantDropdownExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onNotificationsClick,
                        modifier = Modifier.size(40.dp),
                    ) {
                        BadgedBox(
                            badge = {
                                if (state.unreadAlertsCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(StockProColors.Danger),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = state.unreadAlertsCount.toString(),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                            ),
                                        )
                                    }
                                }
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = StockProColors.TextPrimary,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(Spacing.x1))

                    // User Initials Avatar with Active Ring
                    val initials = remember(state.userName) {
                        state.userName.trim().split("\\s+".toRegex())
                            .filter { it.isNotEmpty() }
                            .take(2)
                            .map { it.first().uppercase() }
                            .joinToString("")
                            .ifBlank { "OP" }
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(StockProColors.Primary.copy(alpha = 0.12f))
                            .border(1.5.dp, StockProColors.Primary, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = initials,
                            style = StockProCodeTextStyle.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = StockProColors.Primary,
                            ),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.x3))

            // 3. Operational 2x2 KPI Grid (Matching Stitch Industrial Home)
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.x2)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.x2),
                ) {
                    IndustrialKpiCard(
                        title = "TOTAL ITEMS",
                        value = String.format("%,d", state.totalItems),
                        deltaText = "+${state.totalItemsDeltaPercent}%",
                        subText = "this month",
                        accentColor = StockProColors.Primary,
                        modifier = Modifier.weight(1f),
                    )
                    IndustrialKpiCard(
                        title = "LOW STOCK",
                        value = "${state.lowStockCount}",
                        deltaText = "${state.lowStockItemsNeedReorder} Crits",
                        subText = "need reorder",
                        accentColor = StockProColors.Warning,
                        isWarning = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.x2),
                ) {
                    IndustrialKpiCard(
                        title = "IN PRODUCTION",
                        value = String.format("%,d", state.finishedGoodsCount),
                        deltaText = "+${state.finishedGoodsDeltaPercent}%",
                        subText = "active runs",
                        accentColor = StockProColors.Production,
                        modifier = Modifier.weight(1f),
                    )
                    IndustrialKpiCard(
                        title = "RAW MATERIALS",
                        value = String.format("%,d", state.rawMaterialsCount),
                        deltaText = "+${state.rawMaterialsDeltaPercent}%",
                        subText = "in warehouse",
                        accentColor = StockProColors.Success,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.x3))

            // 4. Hero Inventory Valuation Card with Area Sparkline (Full Width)
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "TOTAL VALUATION",
                                style = StockProCodeTextStyle.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StockProColors.TextSecondary,
                                ),
                            )
                            Spacer(modifier = Modifier.width(Spacing.x1))
                            Surface(
                                shape = Radius.xs,
                                color = StockProColors.SuccessContainer,
                            ) {
                                Text(
                                    text = "INR ₹",
                                    style = StockProCodeTextStyle.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StockProColors.Success,
                                    ),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                )
                            }
                        }

                        IconButton(
                            onClick = onToggleValuation,
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                imageVector = if (state.isValuationVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle valuation",
                                tint = StockProColors.TextSecondary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.x1))

                    val formattedValuation = if (state.isValuationVisible) {
                        val inRupees = state.totalValuationPaise / 100.0
                        if (inRupees >= 10_000_000) {
                            "₹ ${String.format("%.2f", inRupees / 10_000_000.0)} Cr"
                        } else if (inRupees >= 100_000) {
                            "₹ ${String.format("%.2f", inRupees / 100_000.0)} Lakh"
                        } else {
                            "₹ ${String.format("%,.0f", inRupees)}"
                        }
                    } else {
                        "₹ •••••••••"
                    }

                    Text(
                        text = formattedValuation,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp,
                        ),
                        color = StockProColors.TextPrimary,
                    )

                    Spacer(modifier = Modifier.height(Spacing.x1))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = Radius.pill,
                            color = StockProColors.SuccessContainer,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = StockProColors.Success,
                                    modifier = Modifier.size(11.dp),
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "+${state.valuationDeltaPercent}%",
                                    style = StockProCodeTextStyle.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StockProColors.Success,
                                    ),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(Spacing.x1))
                        Text(
                            text = "vs preceding 30 days",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = StockProColors.TextSecondary,
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.x2))

                    AreaSparkline(
                        dataPoints = state.sparklinePoints,
                        labels = state.sparklineLabels,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.x3))

            // 5. Stock Composition & Inventory Health (Full Width Card)
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
                                text = "STOCK HEALTH & COMPOSITION",
                                style = StockProCodeTextStyle.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StockProColors.TextSecondary,
                                ),
                            )
                            Text(
                                text = "Physical warehouse distribution",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = StockProColors.TextTertiary,
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(Radius.sm)
                                .clickable { onNavigateToInventory() }
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = "View All",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = StockProColors.Primary,
                                ),
                            )
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = StockProColors.Primary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.x3))

                    StockCompositionDonut(
                        segments = state.stockSegments,
                        totalCount = state.totalItems,
                        centerLabel = "Active SKUs",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.x3))

            // 6. Floor Operations Action Grid (4 Buttons)
            Text(
                text = "FLOOR OPERATIONS",
                style = StockProCodeTextStyle.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = StockProColors.TextSecondary,
                ),
            )
            Spacer(modifier = Modifier.height(Spacing.x1))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.x2),
            ) {
                FloorActionButton(
                    title = "Receive",
                    sub = "+ Inbound",
                    icon = Icons.Default.ArrowDownward,
                    color = StockProColors.Success,
                    containerColor = StockProColors.SuccessContainer.copy(alpha = 0.6f),
                    onClick = onReceiveStockClick,
                    modifier = Modifier.weight(1f),
                )
                FloorActionButton(
                    title = "Issue",
                    sub = "- Outbound",
                    icon = Icons.Default.ArrowUpward,
                    color = StockProColors.Primary,
                    containerColor = StockProColors.PrimaryContainer.copy(alpha = 0.6f),
                    onClick = onIssueStockClick,
                    modifier = Modifier.weight(1f),
                )
                FloorActionButton(
                    title = "Catalog",
                    sub = "Inventory",
                    icon = Icons.Default.Inventory,
                    color = StockProColors.TextPrimary,
                    containerColor = StockProColors.SurfaceAlt,
                    onClick = onNavigateToInventory,
                    modifier = Modifier.weight(1f),
                )
                FloorActionButton(
                    title = "Reports",
                    sub = "Analytics",
                    icon = Icons.Default.BarChart,
                    color = StockProColors.Production,
                    containerColor = StockProColors.ProductionContainer.copy(alpha = 0.6f),
                    onClick = onNavigateToReports,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(Spacing.x3))

            // 7. Priority Restock Alerts
            if (state.topLowStockItems.isNotEmpty()) {
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(StockProColors.Warning),
                                )
                                Spacer(modifier = Modifier.width(Spacing.x1))
                                Text(
                                    text = "PRIORITY RESTOCK ALERTS (${state.topLowStockItems.size})",
                                    style = StockProCodeTextStyle.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StockProColors.TextPrimary,
                                    ),
                                )
                            }
                            Text(
                                text = "See All",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = StockProColors.Primary,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                modifier = Modifier.clickable { onNavigateToInventory() },
                            )
                        }

                        Spacer(modifier = Modifier.height(Spacing.x2))

                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.x2)) {
                            state.topLowStockItems.take(4).forEach { item ->
                                PriorityRestockRow(
                                    item = item,
                                    onClick = { onItemClick(item.id) },
                                    onRestockClick = onReceiveStockClick,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.x3))
            }

            // 8. Recent Movements Ledger (Synchronized Activity)
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
                            text = "RECENT MOVEMENTS LEDGER",
                            style = StockProCodeTextStyle.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = StockProColors.TextSecondary,
                            ),
                        )
                        Text(
                            text = "History >",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = StockProColors.Primary,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            modifier = Modifier.clickable { onNavigateToReports() },
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.x2))

                    if (state.recentActivities.isEmpty()) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = Spacing.x4),
                        ) {
                            Text(
                                text = "No stock movements recorded yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = StockProColors.TextSecondary,
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.x2)) {
                            state.recentActivities.take(5).forEach { activity ->
                                RecentActivityRow(activity = activity)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Industrial Sub-Components ──

@Composable
fun IndustrialTelemetryStrip(
    syncStatus: SyncStatus,
    onNeedsAttentionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isOnline = syncStatus is SyncStatus.Synced
    val statusColor = if (isOnline) StockProColors.Success else StockProColors.Warning

    Surface(
        color = StockProColors.SurfaceAlt,
        modifier = modifier
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable {
                    if (!isOnline) onNeedsAttentionClick()
                },
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(statusColor),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isOnline) "SYNC ONLINE" else "OFFLINE CACHED",
                    style = StockProCodeTextStyle.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                    ),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "•  LATENCY 18ms",
                    style = StockProCodeTextStyle.copy(
                        fontSize = 10.sp,
                        color = StockProColors.TextTertiary,
                    ),
                )
            }

            Text(
                text = "RF-ID MESH 5.8GHz",
                style = StockProCodeTextStyle.copy(
                    fontSize = 10.sp,
                    color = StockProColors.TextTertiary,
                ),
            )
        }
    }
}

@Composable
private fun IndustrialKpiCard(
    title: String,
    value: String,
    deltaText: String,
    subText: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    isWarning: Boolean = false,
) {
    Surface(
        shape = Radius.lg,
        color = StockProColors.Surface,
        modifier = modifier
            .border(1.dp, StockProColors.OutlineSubtle, Radius.lg),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Left Accent Color Bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(96.dp)
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

                Spacer(modifier = Modifier.height(Spacing.x1))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = Radius.xs,
                        color = if (isWarning) StockProColors.WarningContainer else StockProColors.SuccessContainer,
                    ) {
                        Text(
                            text = deltaText,
                            style = StockProCodeTextStyle.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isWarning) StockProColors.OnWarningContainer else StockProColors.Success,
                            ),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = subText,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                        color = StockProColors.TextTertiary,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun FloorActionButton(
    title: String,
    sub: String,
    icon: ImageVector,
    color: Color,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = Radius.md,
        color = StockProColors.Surface,
        modifier = modifier
            .clip(Radius.md)
            .clickable(onClick = onClick)
            .border(1.dp, StockProColors.OutlineSubtle, Radius.md),
    ) {
        Column(
            modifier = Modifier.padding(vertical = Spacing.x2, horizontal = 6.dp),
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
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = StockProColors.TextPrimary,
                maxLines = 1,
            )
            Text(
                text = sub,
                style = StockProCodeTextStyle.copy(fontSize = 9.sp, color = StockProColors.TextSecondary),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun PriorityRestockRow(
    item: Item,
    onClick: () -> Unit,
    onRestockClick: () -> Unit,
) {
    val currentUnits = item.onHand.toUnitsDouble
    val reorderUnits = item.reorderLevel.toUnitsDouble
    val ratio = if (reorderUnits > 0) (currentUnits / (reorderUnits * 1.5)).toFloat().coerceIn(0f, 1f) else 0f

    Surface(
        shape = Radius.sm,
        color = StockProColors.SurfaceAlt,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(Spacing.x2)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = Radius.xs,
                        color = StockProColors.Surface,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, StockProColors.Outline),
                    ) {
                        Text(
                            text = item.code,
                            style = StockProCodeTextStyle.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(Spacing.x1))
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = StockProColors.TextPrimary,
                        maxLines = 1,
                    )
                }

                StatusBadge(level = item.stockLevel)
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Capacity progress bar
            LinearProgressIndicator(
                progress = { ratio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(Radius.pill),
                color = if (item.stockLevel == StockLevel.OUT_OF_STOCK) StockProColors.Danger else StockProColors.Warning,
                trackColor = StockProColors.OutlineSubtle,
            )

            Spacer(modifier = Modifier.height(Spacing.x1))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Stock: ${currentUnits.toLong()} / Min: ${reorderUnits.toLong()} ${item.unitOfMeasure.lowercase()}",
                    style = StockProCodeTextStyle.copy(fontSize = 10.sp, color = StockProColors.TextSecondary),
                )

                Surface(
                    shape = Radius.pill,
                    color = StockProColors.Primary,
                    modifier = Modifier
                        .clip(Radius.pill)
                        .clickable(onClick = onRestockClick),
                ) {
                    Text(
                        text = "+ Restock",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentActivityRow(
    activity: HomeActivityItem,
) {
    val iconColor = when (activity.type) {
        MovementType.RECEIPT -> StockProColors.Success
        MovementType.ISSUE -> StockProColors.Primary
        MovementType.ADJUSTMENT -> StockProColors.Warning
        MovementType.PRODUCTION -> StockProColors.Production
        else -> StockProColors.TextSecondary
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = when (activity.type) {
                    MovementType.RECEIPT -> Icons.Default.ArrowDownward
                    MovementType.ISSUE -> Icons.Default.ArrowUpward
                    MovementType.ADJUSTMENT -> Icons.Default.Warning
                    MovementType.PRODUCTION -> Icons.Default.PrecisionManufacturing
                    else -> Icons.Default.Inventory
                },
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(15.dp),
            )
        }
        Spacer(modifier = Modifier.width(Spacing.x2))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = activity.title,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = StockProColors.TextPrimary,
                maxLines = 1,
            )
            Text(
                text = activity.subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = StockProColors.TextSecondary,
                maxLines = 1,
            )
        }
        Text(
            text = activity.timeAgo,
            style = StockProCodeTextStyle.copy(fontSize = 10.sp, color = StockProColors.TextTertiary),
        )
    }
}

@Composable
private fun HomeLoadingSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.screenGutter),
    ) {
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(28.dp),
        )
        Spacer(modifier = Modifier.height(Spacing.x3))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.x2)) {
            SkeletonBox(modifier = Modifier.weight(1f).height(90.dp))
            SkeletonBox(modifier = Modifier.weight(1f).height(90.dp))
        }
        Spacer(modifier = Modifier.height(Spacing.x2))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.x2)) {
            SkeletonBox(modifier = Modifier.weight(1f).height(90.dp))
            SkeletonBox(modifier = Modifier.weight(1f).height(90.dp))
        }
        Spacer(modifier = Modifier.height(Spacing.x3))
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
        )
        Spacer(modifier = Modifier.height(Spacing.x3))
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
        )
    }
}
