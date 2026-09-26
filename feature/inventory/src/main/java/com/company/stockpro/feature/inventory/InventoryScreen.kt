package com.company.stockpro.feature.inventory

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.company.stockpro.core.designsystem.component.ButtonVariant
import com.company.stockpro.core.designsystem.component.StockProButton
import com.company.stockpro.core.designsystem.component.StockProTextField
import com.company.stockpro.core.designsystem.theme.Radius
import com.company.stockpro.core.designsystem.theme.Spacing
import com.company.stockpro.core.designsystem.theme.StockProCodeTextStyle
import com.company.stockpro.core.designsystem.theme.StockProColors
import com.company.stockpro.core.domain.model.InventorySummary
import com.company.stockpro.core.domain.model.Item
import com.company.stockpro.core.domain.model.ItemClass
import com.company.stockpro.core.domain.model.SortOrder
import com.company.stockpro.core.domain.model.SyncStatus
import com.company.stockpro.core.ui.component.EmptyReason
import com.company.stockpro.core.ui.component.EmptyState
import com.company.stockpro.core.ui.component.ItemListRow
import com.company.stockpro.core.ui.component.ItemListRowSkeleton

/**
 * Stateful entry route collecting state from ViewModel and forwarding to stateless [InventoryScreen].
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
        onItemClick = onItemClick,
        onAddItemClick = onAddItemClick,
        onScanClick = onScanClick,
        onQuickMovementClick = onQuickMovementClick,
        onNeedsAttentionClick = onNeedsAttentionClick,
        modifier = modifier,
    )
}

/**
 * Pure, stateless Precision Industrial InventoryScreen.
 * Faithful implementation of Stitch Industrial Inventory System (screenshot_stitch_inventory.png).
 */
@Composable
fun InventoryScreen(
    uiState: InventoryUiState,
    onClassSelected: (ItemClass) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onSortOrderChanged: (SortOrder) -> Unit,
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = StockProColors.Canvas,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onQuickMovementClick,
                containerColor = StockProColors.Primary,
                contentColor = StockProColors.Surface,
                shape = Radius.pill,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Post Stock Movement")
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // 1. Top Industrial Telemetry Strip
            InventoryTelemetryStrip(
                syncStatus = syncStatus,
                onNeedsAttentionClick = onNeedsAttentionClick,
            )

            // 2. Industrial Catalog Header
            InventoryHeader(
                onScanClick = onScanClick,
                onAddItemClick = onAddItemClick,
                modifier = Modifier.padding(horizontal = Spacing.screenGutter, vertical = Spacing.x2),
            )

            // 3. Search Bar & Filter Button with Integrated Scan Trigger
            val currentQuery = when (uiState) {
                is InventoryUiState.Content -> uiState.searchQuery
                else -> ""
            }
            var showSortMenu by rememberSaveable { mutableStateOf(false) }

            Box(modifier = Modifier.padding(horizontal = Spacing.screenGutter)) {
                SearchAndFilterBar(
                    searchQuery = currentQuery,
                    onSearchQueryChanged = onSearchQueryChanged,
                    onScanClick = onScanClick,
                    onFilterClick = { showSortMenu = true },
                    modifier = Modifier.fillMaxWidth(),
                )

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false },
                ) {
                    val currentSort = (uiState as? InventoryUiState.Content)?.sortOrder ?: SortOrder.NAME_ASC
                    DropdownMenuItem(
                        text = { Text("Name (A to Z)" + if (currentSort == SortOrder.NAME_ASC) "  ✓" else "") },
                        onClick = {
                            onSortOrderChanged(SortOrder.NAME_ASC)
                            showSortMenu = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("SKU Code (A to Z)" + if (currentSort == SortOrder.CODE_ASC) "  ✓" else "") },
                        onClick = {
                            onSortOrderChanged(SortOrder.CODE_ASC)
                            showSortMenu = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Quantity: High to Low" + if (currentSort == SortOrder.QUANTITY_DESC) "  ✓" else "") },
                        onClick = {
                            onSortOrderChanged(SortOrder.QUANTITY_DESC)
                            showSortMenu = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Quantity: Low to High" + if (currentSort == SortOrder.QUANTITY_ASC) "  ✓" else "") },
                        onClick = {
                            onSortOrderChanged(SortOrder.QUANTITY_ASC)
                            showSortMenu = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Recently Moved" + if (currentSort == SortOrder.LAST_MOVED_DESC) "  ✓" else "") },
                        onClick = {
                            onSortOrderChanged(SortOrder.LAST_MOVED_DESC)
                            showSortMenu = false
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.x2))

            // 4. Category Filter Chips with Live Item Counts
            val selectedClass = when (uiState) {
                is InventoryUiState.Content -> uiState.selectedClass
                is InventoryUiState.Empty -> uiState.selectedClass
                else -> ItemClass.COMPONENT
            }
            ItemClassChips(
                selectedClass = selectedClass,
                onClassSelected = onClassSelected,
                modifier = Modifier.padding(horizontal = Spacing.screenGutter),
            )

            Spacer(modifier = Modifier.height(Spacing.x2))

            // 5. 4 Summary Metric Cards (Total, Low Stock, Out of Stock, Reserved)
            val summary = when (uiState) {
                is InventoryUiState.Content -> uiState.summary
                is InventoryUiState.Empty -> uiState.summary
                else -> InventorySummary()
            }
            InventorySummaryCards(
                summary = summary,
                modifier = Modifier.padding(horizontal = Spacing.screenGutter),
            )

            Spacer(modifier = Modifier.height(Spacing.x2))

            // 6. Section Header with Sort Selector
            val sortOrder = when (uiState) {
                is InventoryUiState.Content -> uiState.sortOrder
                else -> SortOrder.NAME_ASC
            }
            val itemCount = when (uiState) {
                is InventoryUiState.Content -> uiState.items.size
                else -> 0
            }
            SectionHeaderWithSort(
                title = "${selectedClass.toDisplayTitle().uppercase()} ($itemCount)",
                currentSort = sortOrder,
                onSortSelected = onSortOrderChanged,
                modifier = Modifier.padding(horizontal = Spacing.screenGutter),
            )

            Spacer(modifier = Modifier.height(Spacing.x1))

            // 7. Dynamic Inventory List
            when (uiState) {
                is InventoryUiState.Loading -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = Spacing.screenGutter, vertical = Spacing.x1),
                        verticalArrangement = Arrangement.spacedBy(Spacing.x2),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(5) {
                            ItemListRowSkeleton()
                        }
                    }
                }

                is InventoryUiState.Empty -> {
                    EmptyState(
                        reason = uiState.reason,
                        onActionClick = {
                            when (uiState.reason) {
                                is EmptyReason.SearchMiss -> onSearchQueryChanged("")
                                is EmptyReason.NoItems -> onAddItemClick()
                                else -> onClassSelected(ItemClass.COMPONENT)
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                is InventoryUiState.Error -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(Spacing.screenGutter),
                    ) {
                        Text(
                            text = "Something went wrong loading inventory.",
                            style = MaterialTheme.typography.titleMedium,
                            color = StockProColors.Danger,
                        )
                        Spacer(modifier = Modifier.height(Spacing.x4))
                        StockProButton(
                            text = "Retry",
                            onClick = { onClassSelected(selectedClass) },
                            variant = ButtonVariant.SECONDARY,
                        )
                    }
                }

                is InventoryUiState.Content -> {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = Spacing.screenGutter,
                            end = Spacing.screenGutter,
                            top = Spacing.x1,
                            bottom = 80.dp, // Clearance for FAB
                        ),
                        verticalArrangement = Arrangement.spacedBy(Spacing.x2),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(
                            items = uiState.items,
                            key = { it.id },
                            contentType = { "item_card" },
                        ) { item ->
                            ItemListRow(
                                item = item,
                                onClick = { onItemClick(item) },
                                onOverflowClick = { onItemClick(item) },
                                onReceiveClick = onQuickMovementClick,
                                onIssueClick = onQuickMovementClick,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Industrial Sub-Components ──

@Composable
private fun InventoryTelemetryStrip(
    syncStatus: SyncStatus,
    onNeedsAttentionClick: () -> Unit,
) {
    val isOnline = syncStatus is SyncStatus.Synced
    val statusColor = if (isOnline) StockProColors.Success else StockProColors.Warning

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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable {
                    if (!isOnline) onNeedsAttentionClick()
                },
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(Radius.pill)
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
                text = "BARCODE / RFID READY",
                style = StockProCodeTextStyle.copy(
                    fontSize = 10.sp,
                    color = StockProColors.TextTertiary,
                ),
            )
        }
    }
}

@Composable
private fun InventoryHeader(
    onScanClick: () -> Unit,
    onAddItemClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            Text(
                text = "INVENTORY CATALOG",
                style = StockProCodeTextStyle.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = StockProColors.TextSecondary,
                ),
            )
            Text(
                text = "Plant 01 Warehouse",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.3).sp,
                ),
                color = StockProColors.TextPrimary,
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = Radius.sm,
                color = StockProColors.Surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, StockProColors.OutlineSubtle),
                modifier = Modifier
                    .size(38.dp)
                    .clip(Radius.sm)
                    .clickable(onClick = onScanClick),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Scan Item Barcode",
                        tint = StockProColors.TextPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(Spacing.x2))

            StockProButton(
                text = "+ Add Item",
                onClick = onAddItemClick,
                minHeight = 38.dp,
            )
        }
    }
}

@Composable
private fun SearchAndFilterBar(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onScanClick: () -> Unit,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        StockProTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            placeholder = "Search SKU, description, bin location…",
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = StockProColors.TextTertiary,
                    modifier = Modifier.size(20.dp),
                )
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { onSearchQueryChanged("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear search",
                            tint = StockProColors.TextSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                } else {
                    IconButton(onClick = onScanClick) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan Barcode",
                            tint = StockProColors.Primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            },
            modifier = Modifier.weight(1f),
        )

        Spacer(modifier = Modifier.width(Spacing.x2))

        Surface(
            shape = Radius.md,
            color = StockProColors.Surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, StockProColors.OutlineSubtle),
            modifier = Modifier
                .size(48.dp)
                .clip(Radius.md)
                .clickable(onClick = onFilterClick),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filter & Sort",
                    tint = StockProColors.TextPrimary,
                    modifier = Modifier.size(20.dp),
                )
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
        Pair(ItemClass.TOOL, "Tools"),
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing.x2),
    ) {
        classes.forEach { (itemClass, label) ->
            val isSelected = selectedClass == itemClass
            val bgColor = if (isSelected) StockProColors.Primary else StockProColors.Surface
            val contentColor = if (isSelected) Color.White else StockProColors.TextSecondary
            val borderColor = if (isSelected) StockProColors.Primary else StockProColors.OutlineSubtle

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
                    modifier = Modifier.padding(horizontal = Spacing.x3, vertical = 6.dp),
                ) {
                    val icon = when (itemClass) {
                        ItemClass.COMPONENT -> Icons.Default.Widgets
                        ItemClass.RAW_MATERIAL -> Icons.Default.Category
                        ItemClass.FINISHED_GOOD -> Icons.Default.PrecisionManufacturing
                        ItemClass.TOOL -> Icons.Default.Build
                        else -> Icons.Default.Widgets
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = label,
                        style = StockProCodeTextStyle.copy(
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
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
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.x1),
    ) {
        SummaryPillCard(
            label = "TOTAL",
            value = "${summary.totalItems}",
            accentColor = StockProColors.Primary,
            modifier = Modifier.weight(1f),
        )
        SummaryPillCard(
            label = "LOW STOCK",
            value = "${summary.lowStockCount}",
            accentColor = StockProColors.Warning,
            modifier = Modifier.weight(1f),
        )
        SummaryPillCard(
            label = "OUT OF STOCK",
            value = "${summary.outOfStockCount}",
            accentColor = StockProColors.Danger,
            modifier = Modifier.weight(1f),
        )
        SummaryPillCard(
            label = "RESERVED",
            value = "${summary.reservedCount}",
            accentColor = StockProColors.Reserved,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SummaryPillCard(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = Radius.md,
        color = StockProColors.Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, StockProColors.OutlineSubtle),
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = Spacing.x2, horizontal = 4.dp),
        ) {
            Text(
                text = value,
                style = StockProCodeTextStyle.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor,
                ),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = StockProCodeTextStyle.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = StockProColors.TextTertiary,
                ),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SectionHeaderWithSort(
    title: String,
    currentSort: SortOrder,
    onSortSelected: (SortOrder) -> Unit,
    modifier: Modifier = Modifier,
) {
    var sortExpanded by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = title,
            style = StockProCodeTextStyle.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = StockProColors.TextSecondary,
            ),
        )

        Box {
            Surface(
                shape = Radius.pill,
                color = StockProColors.SurfaceAlt,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, StockProColors.Outline),
                modifier = Modifier
                    .clip(Radius.pill)
                    .clickable { sortExpanded = true },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = Spacing.x2, vertical = 4.dp),
                ) {
                    Text(
                        text = "Sort: ${currentSort.displayName}",
                        style = StockProCodeTextStyle.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = StockProColors.TextPrimary,
                        ),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Sort options",
                        tint = StockProColors.TextSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            DropdownMenu(
                expanded = sortExpanded,
                onDismissRequest = { sortExpanded = false },
            ) {
                SortOrder.entries.forEach { sort ->
                    DropdownMenuItem(
                        text = { Text(sort.displayName) },
                        onClick = {
                            onSortSelected(sort)
                            sortExpanded = false
                        },
                    )
                }
            }
        }
    }
}

private fun ItemClass.toDisplayTitle(): String = when (this) {
    ItemClass.COMPONENT -> "Components"
    ItemClass.RAW_MATERIAL -> "Raw Materials"
    ItemClass.FINISHED_GOOD -> "Finished Goods"
    ItemClass.TOOL -> "Tools"
    else -> "Items"
}
