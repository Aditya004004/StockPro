package com.company.stockpro.feature.inventory.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.stockpro.core.designsystem.component.ButtonVariant
import com.company.stockpro.core.designsystem.component.StatusBadge
import com.company.stockpro.core.designsystem.component.StockProButton
import com.company.stockpro.core.designsystem.theme.Radius
import com.company.stockpro.core.designsystem.theme.Spacing
import com.company.stockpro.core.designsystem.theme.StockProCodeTextStyle
import com.company.stockpro.core.designsystem.theme.StockProColors
import com.company.stockpro.core.domain.model.Item
import com.company.stockpro.core.domain.model.MovementType
import com.company.stockpro.core.domain.model.ItemClass
import com.company.stockpro.core.domain.model.StockLevel
import com.company.stockpro.core.domain.model.StockMovement
import com.company.stockpro.core.domain.model.ToolCustody

/**
 * Detailed view of an inventory item with stock metrics, valuation, and chronological ledger history.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(
    item: Item,
    ledger: List<StockMovement>,
    onBackClick: () -> Unit,
    onReceiveClick: () -> Unit,
    onIssueClick: () -> Unit,
    onAdjustClick: () -> Unit,
    toolCustody: ToolCustody? = null,
    onIssueTool: (operatorName: String, notes: String?) -> Unit = { _, _ -> },
    onReturnTool: (notes: String?) -> Unit = {},
    onLogCalibration: (calibratedAt: Long, nextDueAt: Long, notes: String?) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = item.code,
                        style = StockProCodeTextStyle.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StockProColors.Surface,
                ),
            )
        },
        bottomBar = {
            Surface(
                color = StockProColors.Surface,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.x2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.cardPadding),
                ) {
                    StockProButton(
                        text = "Receive",
                        onClick = onReceiveClick,
                        variant = ButtonVariant.PRIMARY,
                        modifier = Modifier.weight(1f),
                    )
                    StockProButton(
                        text = "Issue",
                        onClick = onIssueClick,
                        variant = ButtonVariant.SECONDARY,
                        modifier = Modifier.weight(1f),
                    )
                    StockProButton(
                        text = "Adjust",
                        onClick = onAdjustClick,
                        variant = ButtonVariant.GHOST,
                        modifier = Modifier.weight(0.8f),
                    )
                }
            }
        },
        containerColor = StockProColors.Canvas,
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        LazyColumn(
            contentPadding = innerPadding,
            modifier = Modifier.fillMaxSize(),
        ) {
            // Header Info Card
            item {
                Surface(
                    color = StockProColors.Surface,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.cardPadding),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(modifier = Modifier.width(Spacing.x2))
                            StatusBadge(level = item.stockLevel)
                        }

                        Spacer(modifier = Modifier.height(Spacing.x2))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.x3),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Category,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = item.categoryName.ifBlank { item.itemClass.name },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            if (item.locationCode.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outlineVariant,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = item.locationCode,
                                        style = StockProCodeTextStyle.copy(fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Metric Overview Grid
            item {
                Spacer(modifier = Modifier.height(Spacing.x3))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.screenGutter),
                ) {
                    Text(
                        text = "Stock Quantities",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(Spacing.x2))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.x2),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        MetricCard(
                            label = "On Hand",
                            value = "${item.onHand.formatted()} ${item.unitOfMeasure.lowercase()}",
                            modifier = Modifier.weight(1f),
                        )
                        MetricCard(
                            label = "Available",
                            value = "${item.available.formatted()} ${item.unitOfMeasure.lowercase()}",
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.x2))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.x2),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        MetricCard(
                            label = "Reserved",
                            value = "${item.reserved.formatted()} ${item.unitOfMeasure.lowercase()}",
                            modifier = Modifier.weight(1f),
                        )
                        MetricCard(
                            label = "In Production",
                            value = "${item.inProduction.formatted()} ${item.unitOfMeasure.lowercase()}",
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // Valuation & Thresholds
            item {
                Spacer(modifier = Modifier.height(Spacing.x3))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.screenGutter),
                ) {
                    Text(
                        text = "Valuation & Reorder Thresholds",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(Spacing.x2))

                    Surface(
                        shape = Radius.md,
                        color = StockProColors.Surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(Spacing.cardPadding)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Unit Cost", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("₹%.2f".format(item.unitCost.toRupeesDouble), style = StockProCodeTextStyle.copy(fontWeight = FontWeight.SemiBold))
                            }
                            Spacer(modifier = Modifier.height(Spacing.x2))
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Reorder Point", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${item.reorderLevel.formatted()} ${item.unitOfMeasure.lowercase()}", style = StockProCodeTextStyle)
                            }
                            Spacer(modifier = Modifier.height(Spacing.x2))
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Par Level", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${item.parLevel.formatted()} ${item.unitOfMeasure.lowercase()}", style = StockProCodeTextStyle)
                            }
                        }
                    }
                }
            }

            // Tool Custody & Calibration (PRD §7 Option b)
            if (item.itemClass == ItemClass.TOOL) {
                item {
                    Spacer(modifier = Modifier.height(Spacing.x3))
                    Box(modifier = Modifier.padding(horizontal = Spacing.screenGutter)) {
                        ToolCustodySection(
                            item = item,
                            custody = toolCustody,
                            onIssueTool = onIssueTool,
                            onReturnTool = onReturnTool,
                            onLogCalibration = onLogCalibration,
                        )
                    }
                }
            }

            // Ledger Timeline Header
            item {
                Spacer(modifier = Modifier.height(Spacing.x4))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.screenGutter),
                ) {
                    Icon(Icons.Default.History, contentDescription = null, tint = StockProColors.Primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(Spacing.x2))
                    Text(
                        text = "Ledger Activity History",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(modifier = Modifier.height(Spacing.x2))
            }

            // Ledger Entries
            if (ledger.isEmpty()) {
                item {
                    Text(
                        text = "No movements recorded for this item yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = Spacing.screenGutter),
                    )
                }
            } else {
                items(ledger, key = { it.id }) { entry ->
                    LedgerTimelineRow(
                        movement = entry,
                        unitOfMeasure = item.unitOfMeasure,
                        modifier = Modifier.padding(horizontal = Spacing.screenGutter, vertical = Spacing.x1),
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp)) // Clearance for bottom bar
            }
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = Radius.md,
        color = StockProColors.Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(Spacing.cardPadding),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(Spacing.x1))
            Text(
                text = value,
                style = StockProCodeTextStyle.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
            )
        }
    }
}

@Composable
private fun LedgerTimelineRow(
    movement: StockMovement,
    unitOfMeasure: String,
    modifier: Modifier = Modifier,
) {
    val isPositive = movement.delta.milli > 0
    val deltaColor = if (isPositive) StockProColors.Success else StockProColors.Danger
    val sign = if (isPositive) "+" else ""

    Surface(
        shape = Radius.sm,
        color = StockProColors.Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.x3),
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = movement.type.name,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (movement.isPending) {
                        Spacer(modifier = Modifier.width(Spacing.x1))
                        Surface(
                            shape = Radius.pill,
                            color = StockProColors.WarningContainer,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        ) {
                            Text(
                                text = "Pending",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = StockProColors.OnWarningContainer,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            )
                        }
                    }
                }
                movement.reference?.let {
                    Text(
                        text = "Ref: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                movement.reasonCode?.let {
                    Text(
                        text = "Reason: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$sign${movement.delta.formatted()} ${unitOfMeasure.lowercase()}",
                    style = StockProCodeTextStyle.copy(
                        fontWeight = FontWeight.Bold,
                        color = deltaColor,
                    ),
                )
                Text(
                    text = "Bal: ${movement.balanceAfter.formatted()}",
                    style = StockProCodeTextStyle.copy(
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}
