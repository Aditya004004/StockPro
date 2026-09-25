package com.company.stockpro.feature.inventory

import androidx.compose.foundation.background
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
import com.company.stockpro.core.ui.component.EmptyState
import com.company.stockpro.core.ui.component.ItemListRow
import com.company.stockpro.core.ui.component.ItemListRowSkeleton
import com.company.stockpro.core.ui.component.SyncBanner

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
 * Pure, stateless InventoryScreen taking immutable state and lambda callbacks.
 * Conforms to rules.md §3.5 (no ViewModel in composables, preview-friendly).
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
            // Sync banner (visible if offline or needs attention)
            when (uiState) {
                is InventoryUiState.Content -> SyncBanner(status = uiState.syncStatus, onNeedsAttentionClick = onNeedsAttentionClick)
                is InventoryUiState.Empty -> SyncBanner(status = uiState.syncStatus, onNeedsAttentionClick = onNeedsAttentionClick)
                else -> Unit
            }

            // Top Header: Title, Scan Button, Add Item Pill
            InventoryHeader(
                onScanClick = onScanClick,
                onAddItemClick = onAddItemClick,
                modifier = Modifier.padding(horizontal = Spacing.screenGutter, vertical = Spacing.x3),
            )

            // Search Bar & Filter Button with Sort Menu
            val currentQuery = when (uiState) {
                is InventoryUiState.Content -> uiState.searchQuery
                else -> ""
            }
            var showSortMenu by rememberSaveable { mutableStateOf(false) }

            Box(modifier = Modifier.padding(horizontal = Spacing.screenGutter)) {
                SearchAndFilterBar(
                    searchQuery = currentQuery,
                    onSearchQueryChanged = onSearchQueryChanged,
                    onFilterClick = { showSortMenu = true },
                    modifier = Modifier.fillMaxWidth(),
                )

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false },
                ) {
                    val currentSort = (uiState as? InventoryUiState.Content)?.sortOrder ?: SortOrder.NAME_ASC
                    DropdownMenuItem(
                        text = { Text("Name (A to Z)" + if (currentSort == SortOrder.NAME_ASC) " ✓" else "") },
                        onClick = {
                            onSortOrderChanged(SortOrder.NAME_ASC)
                            showSortMenu = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Code (A to Z)" + if (currentSort == SortOrder.CODE_ASC) " ✓" else "") },
                        onClick = {
                            onSortOrderChanged(SortOrder.CODE_ASC)
                            showSortMenu = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Quantity (High to Low)" + if (currentSort == SortOrder.QUANTITY_DESC) " ✓" else "") },
                        onClick = {
                            onSortOrderChanged(SortOrder.QUANTITY_DESC)
                            showSortMenu = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Quantity (Low to High)" + if (currentSort == SortOrder.QUANTITY_ASC) " ✓" else "") },
                        onClick = {
                            onSortOrderChanged(SortOrder.QUANTITY_ASC)
                            showSortMenu = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Recently Moved" + if (currentSort == SortOrder.LAST_MOVED_DESC) " ✓" else "") },
                        onClick = {
                            onSortOrderChanged(SortOrder.LAST_MOVED_DESC)
                            showSortMenu = false
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.x3))

            // Item Class Category Filter Chips
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

            Spacer(modifier = Modifier.height(Spacing.x3))

            // 4 KPI Summary Cards
            val summary = when (uiState) {
                is InventoryUiState.Content -> uiState.summary
                is InventoryUiState.Empty -> uiState.summary
                else -> InventorySummary()
            }
            InventorySummaryCards(
                summary = summary,
                modifier = Modifier.padding(horizontal = Spacing.screenGutter),
            )

            Spacer(modifier = Modifier.height(Spacing.x3))

            // Section Header & Sort Dropdown
            val sortOrder = when (uiState) {
                is InventoryUiState.Content -> uiState.sortOrder
                else -> SortOrder.NAME_ASC
            }
            val itemCount = when (uiState) {
                is InventoryUiState.Content -> uiState.items.size
                else -> 0
            }
            SectionHeaderWithSort(
                title = "${selectedClass.toDisplayTitle()} ($itemCount)",
                currentSort = sortOrder,
                onSortSelected = onSortOrderChanged,
                modifier = Modifier.padding(horizontal = Spacing.screenGutter),
            )

            Spacer(modifier = Modifier.height(Spacing.x2))

            // Async State Body
            when (uiState) {
                is InventoryUiState.Loading -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = Spacing.screenGutter, vertical = Spacing.x1),
                        verticalArrangement = Arrangement.spacedBy(Spacing.x2),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(6) {
                            ItemListRowSkeleton()
                        }
                    }
                }

                is InventoryUiState.Empty -> {
                    EmptyState(
                        reason = uiState.reason,
                        onActionClick = {
                            when (uiState.reason) {
                                is com.company.stockpro.core.ui.component.EmptyReason.SearchMiss -> onSearchQueryChanged("")
                                is com.company.stockpro.core.ui.component.EmptyReason.NoItems -> onAddItemClick()
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
                        modifier = Modifier.fillMaxSize().padding(Spacing.screenGutter),
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
                        contentPadding = PaddingValues(horizontal = Spacing.screenGutter, vertical = Spacing.x1),
                        verticalArrangement = Arrangement.spacedBy(Spacing.x2),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(
                            items = uiState.items,
                            key = { it.id },
                            contentType = { "item_row" },
                        ) { item ->
                            ItemListRow(
                                item = item,
                                onClick = { onItemClick(item) },
                                onOverflowClick = { onItemClick(item) },
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Sub-components ──

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
                text = "Inventory",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Manage and track your stock items",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = Radius.md,
                color = StockProColors.PrimaryContainer,
                modifier = Modifier
                    .size(40.dp)
                    .clip(Radius.md)
                    .clickable(onClick = onScanClick),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Scan Item Barcode",
                        tint = StockProColors.Primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(Spacing.x2))

            StockProButton(
                text = "+ Add Item",
                onClick = onAddItemClick,
                minHeight = 40.dp,
            )
        }
    }
}

@Composable
private fun SearchAndFilterBar(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
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
            placeholder = "Search by name, code or category…",
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outlineVariant,
                )
            },
            trailingIcon = if (searchQuery.isNotBlank()) {
                {
                    IconButton(onClick = { onSearchQueryChanged("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                    }
                }
            } else null,
            modifier = Modifier.weight(1f),
        )

        Spacer(modifier = Modifier.width(Spacing.x2))

        Surface(
            shape = Radius.sm,
            color = StockProColors.Surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .size(48.dp)
                .clip(Radius.sm)
                .clickable(onClick = onFilterClick),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filter",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
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
            val contentColor = if (isSelected) StockProColors.Surface else MaterialTheme.colorScheme.onSurfaceVariant
            val borderColor = if (isSelected) StockProColors.Primary else MaterialTheme.colorScheme.outlineVariant

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
                    modifier = Modifier.padding(horizontal = Spacing.x3, vertical = Spacing.x2),
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
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(Spacing.x1))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
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
        horizontalArrangement = Arrangement.spacedBy(Spacing.x2),
    ) {
        SummaryPillCard(
            label = "Total Items",
            value = "${summary.totalItems}",
            bgColor = StockProColors.SuccessContainer,
            accentColor = StockProColors.BrandMark,
            modifier = Modifier.weight(1f),
        )
        SummaryPillCard(
            label = "Low Stock",
            value = "${summary.lowStockCount}",
            bgColor = StockProColors.WarningContainer,
            accentColor = StockProColors.Warning,
            modifier = Modifier.weight(1f),
        )
        SummaryPillCard(
            label = "Out of Stock",
            value = "${summary.outOfStockCount}",
            bgColor = StockProColors.DangerContainer,
            accentColor = StockProColors.Danger,
            modifier = Modifier.weight(1f),
        )
        SummaryPillCard(
            label = "Reserved",
            value = "${summary.reservedCount}",
            bgColor = StockProColors.ReservedContainer,
            accentColor = StockProColors.Reserved,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SummaryPillCard(
    label: String,
    value: String,
    bgColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = Radius.md,
        color = bgColor.copy(alpha = 0.5f),
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = Spacing.x2, horizontal = Spacing.x1),
        ) {
            Text(
                text = value,
                style = StockProCodeTextStyle.copy(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                ),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )

        Box {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(Radius.sm)
                    .clickable { sortExpanded = true }
                    .padding(horizontal = Spacing.x2, vertical = Spacing.x1),
            ) {
                Text(
                    text = "Sort by: ${currentSort.displayName}",
                    style = MaterialTheme.typography.labelMedium,
                    color = StockProColors.Primary,
                )
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
