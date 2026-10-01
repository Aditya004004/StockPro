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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Color
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
import com.company.stockpro.core.domain.model.Currency
import com.company.stockpro.core.domain.model.Item
import com.company.stockpro.core.domain.model.MovementType
import com.company.stockpro.core.domain.model.StockLevel
import com.company.stockpro.core.domain.model.StockMovement
import com.company.stockpro.core.domain.model.ToolCustody

/**
 * Precision Industrial Item Detail Screen.
 * Displays SKU metadata, stock capacity buffer gauge, 2x2 metric grid, valuation thresholds,
 * tool custody (if applicable), and chronological movement ledger.
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
    currency: Currency = Currency.INR,
    toolCustody: ToolCustody? = null,
    onIssueTool: (operatorName: String, notes: String?) -> Unit = { _, _ -> },
    onReturnTool: (notes: String?) -> Unit = {},
    onLogCalibration: (calibratedAt: Long, nextDueAt: Long, notes: String?) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier,
) {
    val currentUnits = item.onHand.toUnitsDouble
    val parUnits = item.parLevel.toUnitsDouble
    val ratio = if (parUnits > 0) (currentUnits / (parUnits * 1.5)).toFloat().coerceIn(0f, 1f) else 0.5f

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = Radius.xs,
                            color = StockProColors.SurfaceAlt,
                            border = androidx.compose.foundation.BorderStroke(1.dp, StockProColors.Outline),
                        ) {
                            Text(
                                text = item.code,
                                style = StockProCodeTextStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(Spacing.x2))
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = StockProColors.TextPrimary)
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
                border = androidx.compose.foundation.BorderStroke(1.dp, StockProColors.OutlineSubtle),
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.x2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.screenGutter, vertical = Spacing.x3),
                ) {
                    StockProButton(
                        text = "+ Receive",
                        onClick = onReceiveClick,
                        variant = ButtonVariant.PRIMARY,
                        modifier = Modifier.weight(1f),
                    )
                    StockProButton(
                        text = "- Issue",
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
            // 1. Hero Item Specification Card
            item {
                Surface(
                    color = StockProColors.Surface,
                    border = androidx.compose.foundation.BorderStroke(width = 0.5.dp, color = StockProColors.OutlineSubtle),
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = (-0.3).sp,
                                    ),
                                    color = StockProColors.TextPrimary,
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.x2),
                                ) {
                                    Surface(
                                        shape = Radius.xs,
                                        color = StockProColors.PrimaryContainer,
                                    ) {
                                        Text(
                                            text = item.categoryName.ifBlank { item.itemClass.name },
                                            style = StockProCodeTextStyle.copy(
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = StockProColors.Primary,
                                            ),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        )
                                    }

                                    if (item.locationCode.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.LocationOn,
                                                contentDescription = null,
                                                tint = StockProColors.TextSecondary,
                                                modifier = Modifier.size(14.dp),
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(
                                                text = item.locationCode,
                                                style = StockProCodeTextStyle.copy(
                                                    fontSize = 11.sp,
                                                    color = StockProColors.TextSecondary,
                                                    fontWeight = FontWeight.Medium,
                                                ),
                                            )
                                        }
                                    }
                                }
                            }

                            StatusBadge(level = item.stockLevel)
                        }

                        Spacer(modifier = Modifier.height(Spacing.x3))

                        // Capacity Progress Bar
                        val progressColor = when (item.stockLevel) {
                            StockLevel.OUT_OF_STOCK -> StockProColors.Danger
                            StockLevel.LOW_STOCK -> StockProColors.Warning
                            else -> StockProColors.Success
                        }

                        LinearProgressIndicator(
                            progress = { ratio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(Radius.pill),
                            color = progressColor,
                            trackColor = StockProColors.OutlineSubtle,
                        )

                        Spacer(modifier = Modifier.height(Spacing.x1))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Current Stock: ${currentUnits.toLong()} ${item.unitOfMeasure.lowercase()}",
                                style = StockProCodeTextStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = progressColor),
                            )
                            Text(
                                text = "Target Par: ${parUnits.toLong()} ${item.unitOfMeasure.lowercase()}",
                                style = StockProCodeTextStyle.copy(fontSize = 11.sp, color = StockProColors.TextSecondary),
                            )
                        }
                    }
                }
            }

            // 2. 2x2 Industrial Stock Quantities Grid
            item {
                Spacer(modifier = Modifier.height(Spacing.x3))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.screenGutter),
                ) {
                    Text(
                        text = "PHYSICAL STOCK BALANCES",
                        style = StockProCodeTextStyle.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = StockProColors.TextSecondary,
                        ),
                    )
                    Spacer(modifier = Modifier.height(Spacing.x2))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.x2),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        IndustrialDetailMetricCard(
                            label = "ON HAND",
                            value = "${item.onHand.formatted()} ${item.unitOfMeasure.lowercase()}",
                            accentColor = StockProColors.Primary,
                            modifier = Modifier.weight(1f),
                        )
                        IndustrialDetailMetricCard(
                            label = "AVAILABLE",
                            value = "${item.available.formatted()} ${item.unitOfMeasure.lowercase()}",
                            accentColor = StockProColors.Success,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.x2))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.x2),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        IndustrialDetailMetricCard(
                            label = "RESERVED",
                            value = "${item.reserved.formatted()} ${item.unitOfMeasure.lowercase()}",
                            accentColor = StockProColors.Reserved,
                            modifier = Modifier.weight(1f),
                        )
                        IndustrialDetailMetricCard(
                            label = "IN PRODUCTION",
                            value = "${item.inProduction.formatted()} ${item.unitOfMeasure.lowercase()}",
                            accentColor = StockProColors.Production,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // 3. Valuation & Thresholds
            item {
                Spacer(modifier = Modifier.height(Spacing.x3))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.screenGutter),
                ) {
                    Text(
                        text = "VALUATION & SAFETY THRESHOLDS",
                        style = StockProCodeTextStyle.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = StockProColors.TextSecondary,
                        ),
                    )
                    Spacer(modifier = Modifier.height(Spacing.x2))

                    Surface(
                        shape = Radius.lg,
                        color = StockProColors.Surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, StockProColors.OutlineSubtle),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(Spacing.cardPadding)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Unit Cost", style = MaterialTheme.typography.bodyMedium, color = StockProColors.TextSecondary)
                                Text(
                                    currency.formatMoney(item.unitCost),
                                    style = StockProCodeTextStyle.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                                )
                            }
                            Spacer(modifier = Modifier.height(Spacing.x2))
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Reorder Point", style = MaterialTheme.typography.bodyMedium, color = StockProColors.TextSecondary)
                                Text(
                                    "${item.reorderLevel.formatted()} ${item.unitOfMeasure.lowercase()}",
                                    style = StockProCodeTextStyle.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                                )
                            }
                            Spacer(modifier = Modifier.height(Spacing.x2))
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Par Safety Level", style = MaterialTheme.typography.bodyMedium, color = StockProColors.TextSecondary)
                                Text(
                                    "${item.parLevel.formatted()} ${item.unitOfMeasure.lowercase()}",
                                    style = StockProCodeTextStyle.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                                )
                            }
                        }
                    }
                }
            }

            // 4. Tool Custody & Calibration Section (if applicable)
            toolCustody?.let { custody ->
                item {
                    Spacer(modifier = Modifier.height(Spacing.x3))
                    ToolCustodySection(
                        item = item,
                        custody = custody,
                        onIssueTool = onIssueTool,
                        onReturnTool = onReturnTool,
                        onLogCalibration = onLogCalibration,
                        modifier = Modifier.padding(horizontal = Spacing.screenGutter),
                    )
                }
            }

            // 5. Chronological Ledger History
            item {
                Spacer(modifier = Modifier.height(Spacing.x3))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.screenGutter),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "CHRONOLOGICAL LEDGER (${ledger.size})",
                            style = StockProCodeTextStyle.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = StockProColors.TextSecondary,
                            ),
                        )
                        Text(
                            text = "Audit Verified",
                            style = StockProCodeTextStyle.copy(fontSize = 10.sp, color = StockProColors.Success),
                        )
                    }
                    Spacer(modifier = Modifier.height(Spacing.x2))
                }
            }

            if (ledger.isEmpty()) {
                item {
                    Surface(
                        shape = Radius.lg,
                        color = StockProColors.Surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, StockProColors.OutlineSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.screenGutter),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.x4),
                        ) {
                            Text(
                                text = "No movements recorded for this SKU.",
                                style = MaterialTheme.typography.bodySmall,
                                color = StockProColors.TextSecondary,
                            )
                        }
                    }
                }
            } else {
                items(
                    items = ledger,
                    key = { it.id },
                ) { movement ->
                    IndustrialLedgerTimelineRow(
                        movement = movement,
                        unitOfMeasure = item.unitOfMeasure,
                        modifier = Modifier.padding(horizontal = Spacing.screenGutter, vertical = 3.dp),
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
private fun IndustrialDetailMetricCard(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = Radius.lg,
        color = StockProColors.Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, StockProColors.OutlineSubtle),
        modifier = modifier,
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(68.dp)
                    .background(accentColor),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = Spacing.x2, vertical = Spacing.x2),
            ) {
                Text(
                    text = label,
                    style = StockProCodeTextStyle.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = StockProColors.TextSecondary,
                    ),
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    style = StockProCodeTextStyle.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = StockProColors.TextPrimary,
                    ),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun IndustrialLedgerTimelineRow(
    movement: StockMovement,
    unitOfMeasure: String,
    modifier: Modifier = Modifier,
) {
    val isPositive = movement.delta.milli > 0
    val deltaColor = if (isPositive) StockProColors.Success else StockProColors.Danger
    val sign = if (isPositive) "+" else ""

    val iconColor = when (movement.type) {
        MovementType.RECEIPT -> StockProColors.Success
        MovementType.ISSUE -> StockProColors.Primary
        MovementType.ADJUSTMENT -> StockProColors.Warning
        MovementType.PRODUCTION -> StockProColors.Production
        else -> StockProColors.TextSecondary
    }

    Surface(
        shape = Radius.md,
        color = StockProColors.Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, StockProColors.OutlineSubtle),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.x3, vertical = Spacing.x2),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = when (movement.type) {
                            MovementType.RECEIPT -> Icons.Default.ArrowDownward
                            MovementType.ISSUE -> Icons.Default.ArrowUpward
                            MovementType.ADJUSTMENT -> Icons.Default.Tune
                            MovementType.PRODUCTION -> Icons.Default.PrecisionManufacturing
                            else -> Icons.Default.Archive
                        },
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(14.dp),
                    )
                }

                Spacer(modifier = Modifier.width(Spacing.x2))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = movement.type.name,
                            style = StockProCodeTextStyle.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = StockProColors.TextPrimary,
                            ),
                        )
                        if (movement.isPending) {
                            Spacer(modifier = Modifier.width(Spacing.x1))
                            Surface(
                                shape = Radius.xs,
                                color = StockProColors.WarningContainer,
                            ) {
                                Text(
                                    text = "Pending",
                                    style = StockProCodeTextStyle.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StockProColors.OnWarningContainer,
                                    ),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                )
                            }
                        }
                    }

                    movement.reference?.let {
                        Text(
                            text = "Ref: $it",
                            style = StockProCodeTextStyle.copy(fontSize = 10.sp, color = StockProColors.TextSecondary),
                        )
                    }
                    movement.reasonCode?.let {
                        Text(
                            text = "Reason: $it",
                            style = StockProCodeTextStyle.copy(fontSize = 10.sp, color = StockProColors.TextTertiary),
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$sign${movement.delta.formatted()} ${unitOfMeasure.lowercase()}",
                    style = StockProCodeTextStyle.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = deltaColor,
                    ),
                )
                Text(
                    text = "Bal: ${movement.balanceAfter.formatted()}",
                    style = StockProCodeTextStyle.copy(
                        fontSize = 10.sp,
                        color = StockProColors.TextTertiary,
                    ),
                )
            }
        }
    }
}
