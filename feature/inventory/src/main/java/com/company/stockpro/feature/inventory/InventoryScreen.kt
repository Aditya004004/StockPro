package com.company.stockpro.feature.inventory

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CancelPresentation
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import com.company.stockpro.core.domain.model.Currency
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.company.stockpro.core.designsystem.theme.StockProStrings
import com.company.stockpro.core.domain.model.InventorySummary
import com.company.stockpro.core.domain.model.Item
import com.company.stockpro.core.domain.model.ItemClass
import com.company.stockpro.core.domain.model.SortOrder
import com.company.stockpro.core.domain.model.SyncStatus
import com.company.stockpro.core.ui.component.ItemListRowSkeleton

/**
 * Modern High-Precision StockPro Inventory Screen.
 * Faithful implementation of the screenshot UI with identical colors, text styling, and layout.
 */
@Composable
fun InventoryRoute(
    viewModel: InventoryViewModel,
    onItemClick: (Item) -> Unit,
    onAddItemClick: () -> Unit,
    onScanClick: () -> Unit,
    onQuickMovementClick: () -> Unit,
    onNeedsAttentionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    InventoryScreen(
        uiState = uiState,
        onClassSelected = viewModel::onClassSelected,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onSortOrderChanged = viewModel::onSortOrderChanged,
        onSelectPlant = viewModel::selectPlant,
        onSyncClick = viewModel::triggerSync,
        onItemClick = onItemClick,
        onAddItemClick = onAddItemClick,
        onScanClick = onScanClick,
        onQuickMovementClick = onQuickMovementClick,
        onNeedsAttentionClick = onNeedsAttentionClick,
        modifier = modifier,
    )
}

@Composable
fun InventoryScreen(
    uiState: InventoryUiState,
    onClassSelected: (ItemClass) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onSortOrderChanged: (SortOrder) -> Unit,
    onSelectPlant: (String) -> Unit = {},
    onSyncClick: () -> Unit = {},
    onItemClick: (Item) -> Unit,
    onAddItemClick: () -> Unit,
    onScanClick: () -> Unit,
    onQuickMovementClick: () -> Unit,
    onNeedsAttentionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val syncStatus = when (uiState) {
        is InventoryUiState.Content -> uiState.syncStatus
        is InventoryUiState.Empty -> uiState.syncStatus
        else -> SyncStatus.Synced(System.currentTimeMillis())
    }

    val summary = when (uiState) {
        is InventoryUiState.Content -> uiState.summary
        is InventoryUiState.Empty -> uiState.summary
        else -> InventorySummary()
    }

    val selectedClass = when (uiState) {
        is InventoryUiState.Content -> uiState.selectedClass
        is InventoryUiState.Empty -> uiState.selectedClass
        else -> ItemClass.COMPONENT
    }

    val currentQuery = when (uiState) {
        is InventoryUiState.Content -> uiState.searchQuery
        else -> ""
    }

    val sortOrder = when (uiState) {
        is InventoryUiState.Content -> uiState.sortOrder
        else -> SortOrder.NAME_ASC
    }

    val itemCount = when (uiState) {
        is InventoryUiState.Content -> uiState.items.size
        else -> 0
    }

    val activePlant = when (uiState) {
        is InventoryUiState.Content -> uiState.activePlant
        is InventoryUiState.Empty -> uiState.activePlant
        else -> "Plant 1"
    }

    val activeWarehouse = when (uiState) {
        is InventoryUiState.Content -> uiState.activeWarehouse
        is InventoryUiState.Empty -> uiState.activeWarehouse
        else -> "Central Warehouse"
    }

    val availablePlants = when (uiState) {
        is InventoryUiState.Content -> uiState.availablePlants
        is InventoryUiState.Empty -> uiState.availablePlants
        else -> listOf(activePlant)
    }

    val currency = when (uiState) {
        is InventoryUiState.Content -> uiState.currency
        is InventoryUiState.Empty -> uiState.currency
        else -> Currency.INR
    }

    var showSortMenu by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(StockProColors.Canvas),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            // 1. Top Telemetry Strip (Professional status bar above Plant Selector)
            InventoryTelemetryStrip(
                syncStatus = syncStatus,
                onSyncClick = onSyncClick,
                onNeedsAttentionClick = onNeedsAttentionClick,
            )

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 96.dp),
            ) {
                Column(modifier = Modifier.padding(horizontal = Spacing.screenGutter)) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. Inventory Catalog Header with Global Plant & Warehouse Selector
                    InventoryHeader(
                        activePlant = activePlant,
                        activeWarehouse = activeWarehouse,
                        availablePlants = availablePlants,
                        onSelectPlant = onSelectPlant,
                        onScanClick = onScanClick,
                        onAddItemClick = onAddItemClick,
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. Search Bar (Full width without filter button, supports real-time search)
                    SearchAndFilterBar(
                        searchQuery = currentQuery,
                        onSearchQueryChanged = onSearchQueryChanged,
                        onScanClick = onScanClick,
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 4. Category Filter Tabs
                    ItemClassChips(
                        selectedClass = selectedClass,
                        onClassSelected = onClassSelected,
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 5. 4 Summary Metric Cards in 1x4 Row
                    InventorySummaryCards(
                        summary = summary,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 6. Section Header with Sort Selector
                    SectionHeaderWithSort(
                        categoryName = selectedClass.toDisplayTitle(),
                        itemCount = itemCount,
                        currentSort = sortOrder,
                        onSortSelected = onSortOrderChanged,
                        showSortMenu = showSortMenu,
                        onDismissSortMenu = { showSortMenu = false },
                        onSortClick = { showSortMenu = true },
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 7. Table Header Row (ITEM, SKU, STOCK, LOCATION, STATUS)
                    InventoryTableHeader()

                    Spacer(modifier = Modifier.height(12.dp))

                    // 8. Main Body (Empty State Card or Item Rows)
                    when (uiState) {
                        is InventoryUiState.Loading -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                repeat(4) {
                                    ItemListRowSkeleton()
                                }
                            }
                        }

                        is InventoryUiState.Empty -> {
                            InventoryEmptyStateCard(
                                onAddItemClick = onAddItemClick,
                            )
                        }

                        is InventoryUiState.Error -> {
                            Surface(
                                shape = Radius.lg,
                                color = Color.White,
                                border = androidx.compose.foundation.BorderStroke(1.dp, StockProColors.OutlineSubtle),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(Spacing.x4),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = StockProColors.Danger,
                                        modifier = Modifier.size(36.dp),
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Unable to load inventory items.",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = StockProColors.TextPrimary,
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    StockProButton(
                                        text = "Retry",
                                        onClick = { onClassSelected(selectedClass) },
                                        variant = ButtonVariant.SECONDARY,
                                    )
                                }
                            }
                        }

                        is InventoryUiState.Content -> {
                            if (uiState.items.isEmpty()) {
                                InventoryEmptyStateCard(
                                    onAddItemClick = onAddItemClick,
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    uiState.items.forEach { item ->
                                        InventoryItemTableRow(
                                            item = item,
                                            onClick = { onItemClick(item) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = onAddItemClick,
            containerColor = StockProColors.Primary,
            contentColor = Color.White,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Item",
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

// ── Sub-Components ──

@Composable
private fun InventoryTelemetryStrip(
    syncStatus: SyncStatus,
    onSyncClick: () -> Unit,
    onNeedsAttentionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = StockProStrings.current
    val isOnline = syncStatus !is SyncStatus.NeedsAttention
    val isSyncing = syncStatus is SyncStatus.Syncing
    val statusColor = when (syncStatus) {
        is SyncStatus.NeedsAttention -> StockProColors.Danger
        is SyncStatus.Pending -> StockProColors.Warning
        is SyncStatus.Syncing -> StockProColors.Primary
        is SyncStatus.Synced -> StockProColors.Success
    }
    val statusLabel = when (syncStatus) {
        is SyncStatus.NeedsAttention -> "OFFLINE (${syncStatus.failedCount})"
        is SyncStatus.Pending -> "PENDING (${syncStatus.count})"
        is SyncStatus.Syncing -> "SYNCING..."
        is SyncStatus.Synced -> "ONLINE"
    }
    val lastSyncLabel = when (syncStatus) {
        is SyncStatus.Synced -> {
            if (syncStatus.lastSyncedEpochMs > 0L) {
                val minsAgo = ((System.currentTimeMillis() - syncStatus.lastSyncedEpochMs) / 60000L).coerceAtLeast(0L)
                if (minsAgo == 0L) strings.justNow else "${minsAgo}m ago"
            } else strings.justNow
        }
        else -> strings.pending
    }

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
            // Online/Offline status + Latency
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable {
                    if (syncStatus is SyncStatus.NeedsAttention) onNeedsAttentionClick()
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
                    text = statusLabel,
                    style = StockProCodeTextStyle.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                    ),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "•",
                    style = StockProCodeTextStyle.copy(fontSize = 10.sp, color = StockProColors.TextTertiary),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "LATENCY 18ms",
                    style = StockProCodeTextStyle.copy(
                        fontSize = 10.sp,
                        color = StockProColors.TextSecondary,
                    ),
                )
            }

            // Last Sync timestamp + Sync Button
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${strings.lastSync}: $lastSyncLabel",
                    style = StockProCodeTextStyle.copy(
                        fontSize = 10.sp,
                        color = StockProColors.TextSecondary,
                    ),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = StockProColors.Surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, StockProColors.Outline),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(enabled = !isSyncing, onClick = onSyncClick),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Sync Now",
                            tint = if (isSyncing) StockProColors.Primary else StockProColors.TextSecondary,
                            modifier = Modifier.size(12.dp),
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (isSyncing) strings.syncing else strings.sync,
                            style = StockProCodeTextStyle.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSyncing) StockProColors.Primary else StockProColors.TextSecondary,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InventoryHeader(
    activePlant: String,
    activeWarehouse: String,
    availablePlants: List<String>,
    onSelectPlant: (String) -> Unit,
    onScanClick: () -> Unit,
    onAddItemClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = StockProStrings.current
    var plantDropdownExpanded by rememberSaveable { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            Text(
                text = "INVENTORY  CATALOG",
                style = StockProCodeTextStyle.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = StockProColors.TextSecondary,
                    letterSpacing = 0.5.sp,
                ),
            )
            Spacer(modifier = Modifier.height(2.dp))

            // Plant Selector with Instant Dropdown
            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { plantDropdownExpanded = true }
                        .padding(vertical = 1.dp),
                ) {
                    Text(
                        text = activePlant.ifBlank { strings.selectPlant },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            letterSpacing = (-0.3).sp,
                        ),
                        color = StockProColors.TextPrimary,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = strings.selectPlant,
                        tint = StockProColors.TextPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                }

                DropdownMenu(
                    expanded = plantDropdownExpanded,
                    onDismissRequest = { plantDropdownExpanded = false },
                ) {
                    if (availablePlants.isEmpty()) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "No plants configured",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = StockProColors.TextSecondary,
                                )
                            },
                            enabled = false,
                            onClick = {},
                        )
                    } else {
                        availablePlants.forEach { plant ->
                            val isSelected = plant.equals(activePlant, ignoreCase = true)
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = plant,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) StockProColors.Primary else StockProColors.TextPrimary,
                                    )
                                },
                                trailingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = StockProColors.Primary,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                } else null,
                                onClick = {
                                    plantDropdownExpanded = false
                                    onSelectPlant(plant)
                                },
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Storefront,
                    contentDescription = null,
                    tint = StockProColors.TextSecondary,
                    modifier = Modifier.size(13.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = activeWarehouse.ifBlank { "Main Warehouse" },
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = StockProColors.TextSecondary,
                    ),
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = StockProColors.Surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, StockProColors.Outline),
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onScanClick),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = strings.scanBarcode,
                        tint = StockProColors.TextPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = StockProColors.Primary,
                modifier = Modifier
                    .height(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onAddItemClick),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = strings.addItem,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchAndFilterBar(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = StockProStrings.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = StockProColors.Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, StockProColors.Outline),
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = StockProColors.TextSecondary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            BasicTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = StockProColors.TextPrimary,
                    fontSize = 13.sp,
                ),
                singleLine = true,
                cursorBrush = SolidColor(StockProColors.Primary),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = strings.searchInventoryPlaceholder,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = StockProColors.TextTertiary,
                                fontSize = 13.sp,
                            ),
                        )
                    }
                    innerTextField()
                },
            )
            if (searchQuery.isNotEmpty()) {
                IconButton(
                    onClick = { onSearchQueryChanged("") },
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = StockProColors.TextSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            } else {
                IconButton(
                    onClick = onScanClick,
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = strings.scanBarcode,
                        tint = StockProColors.Primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ItemClassChips(
    selectedClass: ItemClass,
    onClassSelected: (ItemClass) -> Unit,
    modifier: Modifier = Modifier,
) {
    val classes = listOf(
        Pair(ItemClass.COMPONENT, "Components"),
        Pair(ItemClass.RAW_MATERIAL, "Raw Materials"),
        Pair(ItemClass.FINISHED_GOOD, "Finished Goods"),
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        classes.forEach { (itemClass, label) ->
            val isSelected = selectedClass == itemClass
            val bgColor = if (isSelected) StockProColors.Primary else StockProColors.Surface
            val contentColor = if (isSelected) Color.White else StockProColors.TextSecondary
            val iconTint = if (isSelected) Color.White else StockProColors.TextSecondary
            val borderColor = if (isSelected) StockProColors.Primary else StockProColors.Outline

            Surface(
                shape = Radius.pill,
                color = bgColor,
                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                modifier = Modifier
                    .clip(Radius.pill)
                    .clickable { onClassSelected(itemClass) },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    val icon = when (itemClass) {
                        ItemClass.COMPONENT -> Icons.Default.Inventory2
                        ItemClass.RAW_MATERIAL -> Icons.Default.Category
                        ItemClass.FINISHED_GOOD -> Icons.Default.PrecisionManufacturing
                        else -> Icons.Default.Inventory2
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 12.5.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        ),
                        color = contentColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun InventorySummaryCards(
    summary: InventorySummary,
    modifier: Modifier = Modifier,
) {
    val strings = StockProStrings.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MetricSummaryCard(
            label = strings.totalItems,
            value = "${summary.totalItems}",
            icon = Icons.Default.Inventory2,
            iconColor = StockProColors.Primary,
            containerColor = StockProColors.PrimaryContainer,
            borderColor = StockProColors.Primary.copy(alpha = 0.25f),
            modifier = Modifier.weight(1f),
        )
        MetricSummaryCard(
            label = strings.lowStock,
            value = "${summary.lowStockCount}",
            icon = Icons.Default.Warning,
            iconColor = StockProColors.Warning,
            containerColor = StockProColors.WarningContainer,
            borderColor = StockProColors.Warning.copy(alpha = 0.25f),
            modifier = Modifier.weight(1f),
        )
        MetricSummaryCard(
            label = strings.outOfStock,
            value = "${summary.outOfStockCount}",
            icon = Icons.Default.CancelPresentation,
            iconColor = StockProColors.Danger,
            containerColor = StockProColors.DangerContainer,
            borderColor = StockProColors.Danger.copy(alpha = 0.25f),
            modifier = Modifier.weight(1f),
        )
        MetricSummaryCard(
            label = strings.reserved,
            value = "${summary.reservedCount}",
            icon = Icons.Default.BookmarkBorder,
            iconColor = StockProColors.Reserved,
            containerColor = StockProColors.ReservedContainer,
            borderColor = StockProColors.Reserved.copy(alpha = 0.25f),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MetricSummaryCard(
    label: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    containerColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = StockProColors.TextPrimary,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = StockProColors.TextSecondary,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SectionHeaderWithSort(
    categoryName: String,
    itemCount: Int,
    currentSort: SortOrder,
    onSortSelected: (SortOrder) -> Unit,
    showSortMenu: Boolean,
    onDismissSortMenu: () -> Unit,
    onSortClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sortDisplayName = when (currentSort) {
        SortOrder.NAME_ASC -> "Name"
        SortOrder.CODE_ASC -> "SKU"
        SortOrder.QUANTITY_DESC -> "Qty (High)"
        SortOrder.QUANTITY_ASC -> "Qty (Low)"
        SortOrder.LAST_MOVED_DESC -> "Recent"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = categoryName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = StockProColors.TextPrimary,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "($itemCount)",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = StockProColors.TextPrimary,
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "All ${categoryName.lowercase()} items in this warehouse",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                ),
                color = StockProColors.TextSecondary,
            )
        }

        Box {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = StockProColors.Surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, StockProColors.Outline),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onSortClick),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapVert,
                        contentDescription = null,
                        tint = StockProColors.Primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Sort: ",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                        ),
                        color = StockProColors.TextSecondary,
                    )
                    Text(
                        text = sortDisplayName,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = StockProColors.TextPrimary,
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Sort options",
                        tint = StockProColors.TextSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            DropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = onDismissSortMenu,
            ) {
                DropdownMenuItem(
                    text = { Text("Name (A to Z)") },
                    onClick = {
                        onSortSelected(SortOrder.NAME_ASC)
                        onDismissSortMenu()
                    },
                )
                DropdownMenuItem(
                    text = { Text("SKU Code (A to Z)") },
                    onClick = {
                        onSortSelected(SortOrder.CODE_ASC)
                        onDismissSortMenu()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Quantity: High to Low") },
                    onClick = {
                        onSortSelected(SortOrder.QUANTITY_DESC)
                        onDismissSortMenu()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Quantity: Low to High") },
                    onClick = {
                        onSortSelected(SortOrder.QUANTITY_ASC)
                        onDismissSortMenu()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Recently Moved") },
                    onClick = {
                        onSortSelected(SortOrder.LAST_MOVED_DESC)
                        onDismissSortMenu()
                    },
                )
            }
        }
    }
}

@Composable
private fun InventoryTableHeader(modifier: Modifier = Modifier) {
    val strings = StockProStrings.current
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = StockProColors.SurfaceAlt,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .border(1.5.dp, StockProColors.Outline, RoundedCornerShape(4.dp)),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = strings.tableHeaderItem,
                style = StockProCodeTextStyle.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = StockProColors.TextSecondary,
                    letterSpacing = 0.5.sp,
                ),
                modifier = Modifier.weight(1.8f),
            )
            Text(
                text = strings.tableHeaderSku,
                style = StockProCodeTextStyle.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = StockProColors.TextSecondary,
                    letterSpacing = 0.5.sp,
                ),
                modifier = Modifier.weight(1.1f),
            )
            Text(
                text = strings.tableHeaderOnHand,
                style = StockProCodeTextStyle.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = StockProColors.TextSecondary,
                    letterSpacing = 0.5.sp,
                ),
                modifier = Modifier.weight(1.0f),
            )
            Text(
                text = strings.tableHeaderLocation,
                style = StockProCodeTextStyle.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = StockProColors.TextSecondary,
                    letterSpacing = 0.5.sp,
                ),
                modifier = Modifier.weight(1.3f),
            )
            Text(
                text = strings.tableHeaderStatus,
                style = StockProCodeTextStyle.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = StockProColors.TextSecondary,
                    letterSpacing = 0.5.sp,
                ),
                modifier = Modifier.weight(1.1f),
            )
        }
    }
}

@Composable
private fun InventoryItemTableRow(
    item: Item,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = StockProColors.Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, StockProColors.OutlineSubtle),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .border(1.5.dp, StockProColors.Outline, RoundedCornerShape(4.dp)),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1.8f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                    color = StockProColors.TextPrimary,
                    maxLines = 1,
                )
                Text(
                    text = item.categoryName,
                    style = StockProCodeTextStyle.copy(fontSize = 10.sp, color = StockProColors.TextTertiary),
                    maxLines = 1,
                )
            }
            Text(
                text = item.code,
                style = StockProCodeTextStyle.copy(fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = StockProColors.TextSecondary),
                modifier = Modifier.weight(1.1f),
                maxLines = 1,
            )
            Text(
                text = "${item.onHand.toUnitsDouble.toLong()} ${item.unitOfMeasure.lowercase()}",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.5.sp),
                color = StockProColors.TextPrimary,
                modifier = Modifier.weight(1.0f),
                maxLines = 1,
            )
            Text(
                text = item.locationCode ?: "—",
                style = StockProCodeTextStyle.copy(fontSize = 10.5.sp, color = StockProColors.TextSecondary),
                modifier = Modifier.weight(1.3f),
                maxLines = 1,
            )
            Box(modifier = Modifier.weight(1.1f)) {
                StatusBadge(level = item.stockLevel)
            }
        }
    }
}

@Composable
private fun InventoryEmptyStateCard(
    onAddItemClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = Radius.lg,
        color = StockProColors.Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, StockProColors.OutlineSubtle),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 44.dp, horizontal = 24.dp),
        ) {
            IsometricCubeGraphic(
                modifier = Modifier.size(120.dp),
            )
            Spacer(modifier = Modifier.height(18.dp))
            val strings = StockProStrings.current
            Text(
                text = strings.noItemsYet,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = StockProColors.TextPrimary,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Add your first item to start tracking stock\nin this plant.",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                ),
                color = StockProColors.TextSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(20.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = StockProColors.Primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onAddItemClick),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 11.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.addItem,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun IsometricCubeGraphic(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        // Soft radial glow background
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFDBEAFE).copy(alpha = 0.55f),
                    Color(0xFFEFF6FF).copy(alpha = 0.20f),
                    Color.Transparent,
                ),
                center = Offset(cx, cy),
                radius = w * 0.45f,
            ),
            center = Offset(cx, cy),
            radius = w * 0.45f,
        )

        // Accent spark lines
        drawLine(
            color = Color(0xFF3B82F6),
            start = Offset(cx + 34.dp.toPx(), cy - 22.dp.toPx()),
            end = Offset(cx + 42.dp.toPx(), cy - 28.dp.toPx()),
            strokeWidth = 2.5.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = Color(0xFF3B82F6),
            start = Offset(cx - 32.dp.toPx(), cy - 24.dp.toPx()),
            end = Offset(cx - 38.dp.toPx(), cy - 28.dp.toPx()),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = Color(0xFF60A5FA),
            start = Offset(cx - 38.dp.toPx(), cy - 2.dp.toPx()),
            end = Offset(cx - 44.dp.toPx(), cy - 2.dp.toPx()),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )

        // Cube coordinates
        val cubeRadius = 32.dp.toPx()
        val hOffset = cubeRadius * 0.58f
        val vOffset = cubeRadius * 1.0f

        val topPoint = Offset(cx, cy - vOffset * 0.75f)
        val leftPoint = Offset(cx - cubeRadius, cy - vOffset * 0.75f + hOffset)
        val rightPoint = Offset(cx + cubeRadius, cy - vOffset * 0.75f + hOffset)
        val centerPoint = Offset(cx, cy - vOffset * 0.75f + hOffset * 2f)

        val bottomLeft = Offset(cx - cubeRadius, cy + vOffset * 0.35f)
        val bottomCenter = Offset(cx, cy + vOffset * 0.35f + hOffset)
        val bottomRight = Offset(cx + cubeRadius, cy + vOffset * 0.35f)

        // Top Face (light pastel blue)
        val topFace = Path().apply {
            moveTo(topPoint.x, topPoint.y)
            lineTo(rightPoint.x, rightPoint.y)
            lineTo(centerPoint.x, centerPoint.y)
            lineTo(leftPoint.x, leftPoint.y)
            close()
        }
        drawPath(topFace, Color(0xFFBFDBFE))
        drawPath(topFace, Color.White, style = Stroke(width = 1.5.dp.toPx(), join = StrokeJoin.Round))

        // Left Face (medium bright blue)
        val leftFace = Path().apply {
            moveTo(leftPoint.x, leftPoint.y)
            lineTo(centerPoint.x, centerPoint.y)
            lineTo(bottomCenter.x, bottomCenter.y)
            lineTo(bottomLeft.x, bottomLeft.y)
            close()
        }
        drawPath(leftFace, Color(0xFF60A5FA))
        drawPath(leftFace, Color.White, style = Stroke(width = 1.5.dp.toPx(), join = StrokeJoin.Round))

        // Right Face (deep royal blue)
        val rightFace = Path().apply {
            moveTo(centerPoint.x, centerPoint.y)
            lineTo(rightPoint.x, rightPoint.y)
            lineTo(bottomRight.x, bottomRight.y)
            lineTo(bottomCenter.x, bottomCenter.y)
            close()
        }
        drawPath(rightFace, Color(0xFF2563EB))
        drawPath(rightFace, Color.White, style = Stroke(width = 1.5.dp.toPx(), join = StrokeJoin.Round))
    }
}

private fun ItemClass.toDisplayTitle(): String = when (this) {
    ItemClass.COMPONENT -> "Components"
    ItemClass.RAW_MATERIAL -> "Raw Materials"
    ItemClass.FINISHED_GOOD -> "Finished Goods"
    ItemClass.TOOL -> "Tools"
    else -> "Items"
}
