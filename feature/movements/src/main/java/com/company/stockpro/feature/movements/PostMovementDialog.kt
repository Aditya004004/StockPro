package com.company.stockpro.feature.movements

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.stockpro.core.designsystem.component.ButtonVariant
import com.company.stockpro.core.designsystem.component.NumericStepper
import com.company.stockpro.core.designsystem.component.StockProButton
import com.company.stockpro.core.designsystem.component.StockProTextField
import com.company.stockpro.core.designsystem.theme.Radius
import com.company.stockpro.core.designsystem.theme.Spacing
import com.company.stockpro.core.designsystem.theme.StockProCodeTextStyle
import com.company.stockpro.core.designsystem.theme.StockProColors
import com.company.stockpro.core.domain.model.Item
import com.company.stockpro.core.domain.model.MovementType
import com.company.stockpro.core.domain.model.Quantity
import com.company.stockpro.core.domain.model.StockMovement

/**
 * Modal dialog for posting stock movements (Receive, Issue, Adjust) per architecture.md §6.
 * Implements pre-validation, reorder threshold alerts, and optimistic submission.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostMovementDialog(
    item: Item,
    initialType: MovementType,
    onDismiss: () -> Unit,
    onConfirm: (StockMovement) -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    errorMessage: String? = null,
) {
    var selectedType by remember { mutableStateOf(initialType) }
    var quantityUnits by remember { mutableLongStateOf(10L) }
    var reference by remember { mutableStateOf("") }
    var selectedReason by remember { mutableStateOf("CYCLE_COUNT") }
    var reasonDropdownExpanded by remember { mutableStateOf(false) }

    val reqMilli = quantityUnits * 1000L
    val isIssue = selectedType == MovementType.ISSUE
    val isAdjustment = selectedType == MovementType.ADJUSTMENT
    val hasInsufficientStock = isIssue && reqMilli > item.available.milli
    val dropsBelowReorder = isIssue && (item.onHand.milli - reqMilli) <= item.reorderLevel.milli

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
    ) {
        Surface(
            shape = Radius.lg,
            color = StockProColors.Surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.cardPadding),
            ) {
                // Header
                Text(
                    text = "Post Stock Movement",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${item.code} · ${item.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(Spacing.x4))

                // Movement Type Segmented Selector
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.x2),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    val types = listOf(MovementType.RECEIPT, MovementType.ISSUE, MovementType.ADJUSTMENT)
                    types.forEach { type ->
                        val isSelected = selectedType == type
                        Surface(
                            shape = Radius.sm,
                            color = if (isSelected) StockProColors.Primary else StockProColors.SurfaceAlt,
                            modifier = Modifier
                                .weight(1f)
                                .clip(Radius.sm)
                                .clickable { selectedType = type },
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = Spacing.x2),
                            ) {
                                Text(
                                    text = type.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) StockProColors.Surface else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.x4))

                // Quantity Stepper
                Text(
                    text = if (isAdjustment) "New Physical Count" else "Quantity",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(Spacing.x1))
                NumericStepper(
                    value = quantityUnits,
                    onValueChange = { quantityUnits = it },
                    unitLabel = item.unitOfMeasure.lowercase(),
                    step = 10L,
                    minValue = 1L,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Warning / Error Banners
                if (hasInsufficientStock) {
                    Spacer(modifier = Modifier.height(Spacing.x2))
                    Surface(
                        shape = Radius.sm,
                        color = StockProColors.DangerContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "Insufficient stock. Only ${item.available.formatted()} ${item.unitOfMeasure.lowercase()} available.",
                            style = MaterialTheme.typography.bodySmall,
                            color = StockProColors.Danger,
                            modifier = Modifier.padding(Spacing.x2),
                        )
                    }
                } else if (dropsBelowReorder) {
                    Spacer(modifier = Modifier.height(Spacing.x2))
                    Surface(
                        shape = Radius.sm,
                        color = StockProColors.WarningContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(Spacing.x2),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = StockProColors.Warning,
                            )
                            Spacer(modifier = Modifier.width(Spacing.x1))
                            Text(
                                text = "Warning: This will reduce stock below reorder point (${item.reorderLevel.toUnitsDouble.toLong()}).",
                                style = MaterialTheme.typography.bodySmall,
                                color = StockProColors.OnWarningContainer,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.x3))

                // Reference Input
                StockProTextField(
                    value = reference,
                    onValueChange = { reference = it },
                    label = if (selectedType == MovementType.RECEIPT) "PO / Vendor Reference" else "Department / Job Reference",
                    placeholder = "e.g. PO-88219",
                )

                // Reason Code Selector (Mandatory for ADJUSTMENT)
                if (isAdjustment) {
                    Spacer(modifier = Modifier.height(Spacing.x3))
                    Text(
                        text = "Reason Code (Mandatory)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(Spacing.x1))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            shape = Radius.sm,
                            color = StockProColors.Surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(Radius.sm)
                                .clickable { reasonDropdownExpanded = true }
                                .padding(horizontal = Spacing.x3, vertical = Spacing.x3),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(selectedReason, style = StockProCodeTextStyle.copy(fontSize = 14.sp))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }

                        DropdownMenu(
                            expanded = reasonDropdownExpanded,
                            onDismissRequest = { reasonDropdownExpanded = false },
                        ) {
                            val reasons = listOf("CYCLE_COUNT", "DAMAGE", "SPOILED", "FOUND_STOCK", "DATA_CORRECTION")
                            reasons.forEach { r ->
                                DropdownMenuItem(
                                    text = { Text(r) },
                                    onClick = {
                                        selectedReason = r
                                        reasonDropdownExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }

                errorMessage?.let {
                    Spacer(modifier = Modifier.height(Spacing.x2))
                    Text(it, color = StockProColors.Danger, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(Spacing.x6))

                // Actions
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    StockProButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        variant = ButtonVariant.GHOST,
                        enabled = !isLoading,
                    )
                    Spacer(modifier = Modifier.width(Spacing.x2))
                    StockProButton(
                        text = "Post Movement",
                        onClick = {
                            val deltaMilli = if (isAdjustment) {
                                (quantityUnits * 1000L) - item.onHand.milli
                            } else {
                                quantityUnits * 1000L
                            }

                            val movement = StockMovement(
                                itemId = item.id,
                                itemCode = item.code,
                                itemName = item.name,
                                type = selectedType,
                                delta = Quantity(deltaMilli),
                                unitCost = item.unitCost,
                                reference = reference.ifBlank { null },
                                reasonCode = if (isAdjustment) selectedReason else null,
                            )
                            onConfirm(movement)
                        },
                        enabled = !hasInsufficientStock && quantityUnits > 0,
                        isLoading = isLoading,
                        variant = ButtonVariant.PRIMARY,
                    )
                }
            }
        }
    }
}
