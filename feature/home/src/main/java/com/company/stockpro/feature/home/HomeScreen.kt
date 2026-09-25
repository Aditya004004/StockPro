package com.company.stockpro.feature.home

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Factory
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.company.stockpro.core.designsystem.component.StockProButton
import com.company.stockpro.core.designsystem.theme.Radius
import com.company.stockpro.core.designsystem.theme.Spacing
import com.company.stockpro.core.designsystem.theme.StockProCodeTextStyle
import com.company.stockpro.core.designsystem.theme.StockProColors
import com.company.stockpro.core.domain.model.Item
import com.company.stockpro.core.domain.model.MovementType
import com.company.stockpro.core.ui.component.SkeletonBox
import com.company.stockpro.core.ui.component.SyncBanner
import com.company.stockpro.core.ui.component.chart.AreaSparkline
import com.company.stockpro.core.ui.component.chart.StockCompositionDonut

/**
 * Production Home Dashboard Screen.
 * Faithful implementation of UI mockup media_1789794856417.png.
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
    var plantDropdownExpanded by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = Spacing.x6),
    ) {
        // Sync Banner
        SyncBanner(
            status = state.syncStatus,
            onNeedsAttentionClick = onNeedsAttentionClick,
            modifier = Modifier.fillMaxWidth(),
        )

        Column(modifier = Modifier.padding(horizontal = Spacing.screenGutter)) {
            Spacer(modifier = Modifier.height(Spacing.x2))

            // 1. Top App Bar Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(Radius.md)
                            .background(StockProColors.BrandMark),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Factory,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(Spacing.x2))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Stock",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = StockProColors.TextPrimary,
                            )
                            Text(
                                text = "Pro",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = StockProColors.BrandMark,
                            )
                        }
                        Text(
                            text = "Manufacturing Inventory",
                            style = MaterialTheme.typography.bodySmall,
                            color = StockProColors.TextSecondary,
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNotificationsClick) {
                        BadgedBox(
                            badge = {
                                if (state.unreadAlertsCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(StockProColors.Danger),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = state.unreadAlertsCount.toString(),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color.White,
                                                fontSize = 10.sp,
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
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(Spacing.x1))

                    // User Initials Avatar
                    val initials = remember(state.userName) {
                        state.userName.trim().split("\\s+".toRegex())
                            .filter { it.isNotEmpty() }
                            .take(2)
                            .map { it.first().uppercase() }
                            .joinToString("")
                            .ifBlank { "SP" }
                    }
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(StockProColors.BrandMark.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = StockProColors.BrandMark,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.x3))

            // 2. Greeting & Plant Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val greeting = remember {
                        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                        when {
                            hour < 12 -> "Good Morning"
                            hour < 17 -> "Good Afternoon"
                            else -> "Good Evening"
                        }
                    }
                    Text(
                        text = "$greeting, ${state.userName} 👋",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = StockProColors.TextPrimary,
                    )
                    Spacer(modifier = Modifier.height(Spacing.x1))
                    Text(
                        text = "Here's what's happening in your inventory today.",
                        style = MaterialTheme.typography.bodySmall,
                        color = StockProColors.TextSecondary,
                    )
                }

                // Plant Selector Dropdown Chip
                Box {
                    Surface(
                        shape = Radius.pill,
                        color = StockProColors.SurfaceAlt,
                        modifier = Modifier
                            .clip(Radius.pill)
                            .clickable { plantDropdownExpanded = true },
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = Spacing.x2, vertical = Spacing.x1),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Factory,
                                contentDescription = "Active Plant",
                                tint = StockProColors.TextSecondary,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(Spacing.x1))
                            Text(
                                text = state.activePlant,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                color = StockProColors.TextPrimary,
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select Plant",
                                tint = StockProColors.TextSecondary,
                            )
                        }
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

            Spacer(modifier = Modifier.height(Spacing.x3))

            // 3. Horizontal 4-Card KPI Row (scrollable)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Spacing.x2),
            ) {
                KpiCard(
                    title = "Total Items",
                    value = String.format("%,d", state.totalItems),
                    deltaText = "↑ +${state.totalItemsDeltaPercent}%",
                    subText = "vs last month",
                    icon = Icons.Default.Widgets,
                    iconTint = StockProColors.BrandMark,
                    containerColor = StockProColors.PrimaryContainer.copy(alpha = 0.5f),
                )
                KpiCard(
                    title = "Raw Materials",
                    value = String.format("%,d", state.rawMaterialsCount),
                    deltaText = "↑ +${state.rawMaterialsDeltaPercent}%",
                    subText = "vs last month",
                    icon = Icons.Default.Category,
                    iconTint = StockProColors.Success,
                    containerColor = StockProColors.SuccessContainer.copy(alpha = 0.5f),
                )
                KpiCard(
                    title = "Low Stock",
                    value = String.format("%,d", state.lowStockCount),
                    deltaText = "↑ +${state.lowStockItemsNeedReorder}",
                    subText = "items need reorder",
                    icon = Icons.Default.Warning,
                    iconTint = StockProColors.Warning,
                    containerColor = StockProColors.WarningContainer.copy(alpha = 0.5f),
                    isWarning = true,
                )
                KpiCard(
                    title = "Finished Goods",
                    value = String.format("%,d", state.finishedGoodsCount),
                    deltaText = "↑ +${state.finishedGoodsDeltaPercent}%",
                    subText = "vs last month",
                    icon = Icons.Default.PrecisionManufacturing,
                    iconTint = StockProColors.Primary,
                    containerColor = StockProColors.SurfaceAlt,
                )
            }

            Spacer(modifier = Modifier.height(Spacing.x3))

            // 4. Mid-Section 2-Card Row (Valuation + Stock Status)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.x2),
            ) {
                // Left Card: Inventory Value with Sparkline
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Inventory,
                                    contentDescription = null,
                                    tint = StockProColors.BrandMark,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(Spacing.x1))
                                Text(
                                    text = "Inventory Value",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = StockProColors.TextPrimary,
                                )
                            }
                            IconButton(
                                onClick = onToggleValuation,
                                modifier = Modifier.size(24.dp),
                            ) {
                                Icon(
                                    imageVector = if (state.isValuationVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle valuation visibility",
                                    tint = StockProColors.TextSecondary,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(Spacing.x1))

                        val formattedValuation = if (state.isValuationVisible) {
                            val inRupees = state.totalValuationPaise / 100.0
                            if (inRupees >= 10_000_000) {
                                "₹ ${String.format("%.1f", inRupees / 10_000_000.0)}Cr"
                            } else if (inRupees >= 100_000) {
                                "₹ ${String.format("%.1f", inRupees / 100_000.0)}L"
                            } else {
                                "₹ ${String.format("%,.0f", inRupees)}"
                            }
                        } else {
                            "₹ ••••••••"
                        }

                        Text(
                            text = formattedValuation,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = StockProColors.TextPrimary,
                        )

                        Spacer(modifier = Modifier.height(Spacing.x1))

                        Text(
                            text = "↑ +${state.valuationDeltaPercent}% vs last month",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = StockProColors.Success,
                        )

                        Spacer(modifier = Modifier.height(Spacing.x2))

                        // Area Sparkline Canvas
                        AreaSparkline(
                            dataPoints = state.sparklinePoints,
                            labels = state.sparklineLabels,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                // Right Card: Stock Status Donut
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
                                text = "Stock Status",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = StockProColors.TextPrimary,
                            )
                            Text(
                                text = "View All >",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = StockProColors.BrandMark,
                                    fontSize = 11.sp,
                                ),
                                modifier = Modifier.clickable { onNavigateToInventory() },
                            )
                        }

                        Spacer(modifier = Modifier.height(Spacing.x2))

                        StockCompositionDonut(
                            segments = state.stockSegments,
                            totalCount = state.totalItems,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.x3))

            // 5. Quick Actions Row (4 Action Tiles)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.x2),
            ) {
                QuickActionTile(
                    title = "Add Item",
                    icon = Icons.Default.Add,
                    iconTint = StockProColors.BrandMark,
                    containerColor = StockProColors.PrimaryContainer.copy(alpha = 0.5f),
                    onClick = onNavigateToInventory,
                    modifier = Modifier.weight(1f),
                )
                QuickActionTile(
                    title = "Receive Stock",
                    icon = Icons.Default.ArrowDownward,
                    iconTint = StockProColors.Success,
                    containerColor = StockProColors.SuccessContainer.copy(alpha = 0.5f),
                    onClick = onReceiveStockClick,
                    modifier = Modifier.weight(1f),
                )
                QuickActionTile(
                    title = "Issue Stock",
                    icon = Icons.Default.ArrowUpward,
                    iconTint = StockProColors.Warning,
                    containerColor = StockProColors.WarningContainer.copy(alpha = 0.5f),
                    onClick = onIssueStockClick,
                    modifier = Modifier.weight(1f),
                )
                QuickActionTile(
                    title = "View Reports",
                    icon = Icons.Default.BarChart,
                    iconTint = StockProColors.Primary,
                    containerColor = StockProColors.SurfaceAlt,
                    onClick = onNavigateToReports,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(Spacing.x3))

            // 6. Bottom Split: Recent Activity & Top 5 Low Stock / Upcoming Reorders
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.x2),
            ) {
                // Left: Recent Activity Feed
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
                                text = "Recent Activity",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = StockProColors.TextPrimary,
                            )
                            Text(
                                text = "View All >",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = StockProColors.BrandMark,
                                    fontSize = 11.sp,
                                ),
                                modifier = Modifier.clickable { onNavigateToInventory() },
                            )
                        }

                        Spacer(modifier = Modifier.height(Spacing.x2))

                        if (state.recentActivities.isEmpty()) {
                            Text(
                                text = "No recent stock movements.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = StockProColors.TextSecondary,
                                modifier = Modifier.padding(vertical = Spacing.x2),
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(Spacing.x2)) {
                                state.recentActivities.take(5).forEach { activity ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        val iconColor = when (activity.type) {
                                            MovementType.RECEIPT -> StockProColors.Success
                                            MovementType.ISSUE -> StockProColors.BrandMark
                                            MovementType.ADJUSTMENT -> StockProColors.Warning
                                            MovementType.PRODUCTION -> StockProColors.Primary
                                            else -> StockProColors.TextSecondary
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(iconColor.copy(alpha = 0.15f)),
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
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(Spacing.x2))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = activity.title,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
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
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                            color = StockProColors.TextSecondary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Right Column: Top 5 Low Stock Items & Upcoming Reorders
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.x2),
                ) {
                    // Top 5 Low Stock Card
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
                                    text = "See All >",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = StockProColors.BrandMark,
                                        fontSize = 11.sp,
                                    ),
                                    modifier = Modifier.clickable { onNavigateToInventory() },
                                )
                            }

                            Spacer(modifier = Modifier.height(Spacing.x2))

                            Column(verticalArrangement = Arrangement.spacedBy(Spacing.x1)) {
                                state.topLowStockItems.take(5).forEach { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onItemClick(item.id) }
                                            .padding(vertical = Spacing.x1),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.name,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                                color = StockProColors.TextPrimary,
                                                maxLines = 1,
                                            )
                                            Text(
                                                text = item.code,
                                                style = StockProCodeTextStyle.copy(
                                                    fontSize = 10.sp,
                                                    color = StockProColors.TextSecondary,
                                                ),
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = item.onHand.toUnitsDouble.toLong().toString(),
                                                style = StockProCodeTextStyle.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = StockProColors.Danger,
                                                ),
                                            )
                                            Icon(
                                                imageVector = Icons.Default.ChevronRight,
                                                contentDescription = null,
                                                tint = StockProColors.TextSecondary,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Upcoming Reorders Card
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
                                    text = "Upcoming Reorders",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = StockProColors.TextPrimary,
                                )
                                Text(
                                    text = "See All >",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = StockProColors.BrandMark,
                                        fontSize = 11.sp,
                                    ),
                                    modifier = Modifier.clickable { onNavigateToInventory() },
                                )
                            }

                            Spacer(modifier = Modifier.height(Spacing.x2))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(Radius.sm)
                                        .background(StockProColors.SurfaceAlt),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = StockProColors.TextSecondary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.width(Spacing.x2))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${state.upcomingReordersCount} purchase orders",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                        color = StockProColors.TextPrimary,
                                    )
                                    Text(
                                        text = "due this week",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                        color = StockProColors.TextSecondary,
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = StockProColors.TextSecondary,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    deltaText: String,
    subText: String,
    icon: ImageVector,
    iconTint: Color,
    containerColor: Color,
    modifier: Modifier = Modifier,
    isWarning: Boolean = false,
) {
    Surface(
        shape = Radius.lg,
        color = containerColor,
        modifier = modifier
            .widthIn(min = 135.dp)
            .border(1.dp, StockProColors.OutlineSubtle, Radius.lg),
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
                maxLines = 1,
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
                        color = if (isWarning) StockProColors.Warning else StockProColors.Success,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                    ),
                )
                Spacer(modifier = Modifier.width(Spacing.x1))
                Text(
                    text = subText,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                    color = StockProColors.TextSecondary,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun QuickActionTile(
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
            modifier = Modifier.padding(vertical = Spacing.x3, horizontal = Spacing.x2),
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
                maxLines = 1,
            )
        }
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
            repeat(3) {
                SkeletonBox(
                    modifier = Modifier
                        .width(135.dp)
                        .height(110.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(Spacing.x3))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.x2)) {
            SkeletonBox(
                modifier = Modifier
                    .weight(1f)
                    .height(180.dp),
            )
            SkeletonBox(
                modifier = Modifier
                    .weight(1f)
                    .height(180.dp),
            )
        }
        Spacer(modifier = Modifier.height(Spacing.x3))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.x2)) {
            repeat(4) {
                SkeletonBox(
                    modifier = Modifier
                        .weight(1f)
                        .height(70.dp),
                )
            }
        }
    }
}
