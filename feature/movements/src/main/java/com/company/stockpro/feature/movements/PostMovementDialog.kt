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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
import com.company.stockpro.core.designsystem.theme.StockProStrings
import com.company.stockpro.core.domain.model.Item
import com.company.stockpro.core.domain.model.MovementType
import com.company.stockpro.core.domain.model.Quantity
import com.company.stockpro.core.domain.model.StockMovement

/**
 * Precision Industrial Modal Dialog for posting stock movements (Receive, Issue, Adjust).
 * Implements live balance impact preview, pre-validation alerts, and optimistic ledger submission.
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
    val strings = StockProStrings.current
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

    // Projected Stock Balance calculation
    val currentUnits = item.onHand.toUnitsDouble
    val projectedUnits = when (selectedType) {
        MovementType.RECEIPT -> currentUnits + quantityUnits
        MovementType.ISSUE -> (currentUnits - quantityUnits).coerceAtLeast(0.0)
        MovementType.ADJUSTMENT -> quantityUnits.toDouble()
        else -> currentUnits
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
    ) {
        Surface(
            shape = Radius.lg,
            color = StockProColors.Surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, StockProColors.OutlineSubtle),
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.cardPadding),
            ) {
                // 1. Industrial Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = strings.postMovementTitle.uppercase(),
                            style = StockProCodeTextStyle.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = StockProColors.TextSecondary,
                            ),
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = StockProColors.TextPrimary,
                            maxLines = 1,
                        )
                    }

                    Surface(
                        shape = Radius.xs,
                        color = StockProColors.SurfaceAlt,
                        border = androidx.compose.foundation.BorderStroke(1.dp, StockProColors.Outline),
                    ) {
                        Text(
                            text = item.code,
                            style = StockProCodeTextStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.x3))

                // 2. Movement Type Segmented Selector with Semantic Colors
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.x2),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    val types = listOf(
                        Triple(MovementType.RECEIPT, "${strings.movementReceipt} (+)", StockProColors.Success),
                        Triple(MovementType.ISSUE, "${strings.movementIssue} (-)", StockProColors.Primary),
                        Triple(MovementType.ADJUSTMENT, "${strings.movementAdjustment} (~)", StockProColors.Warning),
                    )

                    types.forEach { (type, label, accent) ->
                        val isSelected = selectedType == type
                        Surface(
                            shape = Radius.sm,
                            color = if (isSelected) accent else StockProColors.SurfaceAlt,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) accent else StockProColors.OutlineSubtle,
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clip(Radius.sm)
                                .clickable { selectedType = type },
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 8.dp),
                            ) {
                                Text(
                                    text = label,
                                    style = StockProCodeTextStyle.copy(
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else StockProColors.TextSecondary,
                                    ),
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.x3))

                // 3. Live Balance Impact Preview Card
                Surface(
                    shape = Radius.md,
                    color = StockProColors.SurfaceAlt,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, StockProColors.Outline),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.x3, vertical = Spacing.x2),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "CURRENT ON HAND",
                                style = StockProCodeTextStyle.copy(fontSize = 9.sp, color = StockProColors.TextTertiary, fontWeight = FontWeight.Bold),
                            )
                            Text(
                                text = "${currentUnits.toLong()} ${item.unitOfMeasure.lowercase()}",
                                style = StockProCodeTextStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = StockProColors.TextPrimary),
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = StockProColors.TextSecondary,
                            modifier = Modifier.size(16.dp),
                        )

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "PROJECTED BALANCE",
                                style = StockProCodeTextStyle.copy(fontSize = 9.sp, color = StockProColors.TextTertiary, fontWeight = FontWeight.Bold),
                            )
                            Text(
                                text = "${projectedUnits.toLong()} ${item.unitOfMeasure.lowercase()}",
                                style = StockProCodeTextStyle.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (projectedUnits <= item.reorderLevel.toUnitsDouble) StockProColors.Warning else StockProColors.Success,
                                ),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.x3))

                // 4. Quantity Stepper
                Text(
                    text = if (isAdjustment) "PHYSICAL COUNT IN WAREHOUSE" else "MOVEMENT QUANTITY",
                    style = StockProCodeTextStyle.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = StockProColors.TextSecondary,
                    ),
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
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
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
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(Spacing.x1))
                            Text(
                                text = "Warning: Balance will fall below reorder threshold (${item.reorderLevel.toUnitsDouble.toLong()}).",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = StockProColors.OnWarningContainer,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.x3))

                // 5. Reference Input
                StockProTextField(
                    value = reference,
                    onValueChange = { reference = it },
                    label = if (selectedType == MovementType.RECEIPT) "PO / Vendor Reference" else "Department / Job Reference",
                    placeholder = "e.g. PO-88219 or JOB-401",
                )

                // 6. Reason Code (Mandatory for ADJUSTMENT)
                if (isAdjustment) {
                    Spacer(modifier = Modifier.height(Spacing.x3))
                    Text(
                        text = "REASON CODE (MANDATORY)",
                        style = StockProCodeTextStyle.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = StockProColors.TextSecondary,
                        ),
                    )
                    Spacer(modifier = Modifier.height(Spacing.x1))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            shape = Radius.sm,
                            color = StockProColors.Surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, StockProColors.Outline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(Radius.sm)
                                .clickable { reasonDropdownExpanded = true }
                                .padding(horizontal = Spacing.x3, vertical = 10.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(selectedReason, style = StockProCodeTextStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = StockProColors.TextSecondary)
                            }
                        }

                        DropdownMenu(
                            expanded = reasonDropdownExpanded,
                            onDismissRequest = { reasonDropdownExpanded = false },
                        ) {
                            val reasons = listOf("CYCLE_COUNT", "DAMAGE", "SPOILED", "FOUND_STOCK", "DATA_CORRECTION")
                            reasons.forEach { r ->
                                DropdownMenuItem(
                                    text = { Text(r, style = StockProCodeTextStyle.copy(fontSize = 12.sp)) },
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

                Spacer(modifier = Modifier.height(Spacing.x4))

                // 7. Actions
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    StockProButton(
                        text = strings.cancel,
                        onClick = onDismiss,
                        variant = ButtonVariant.GHOST,
                        enabled = !isLoading,
                    )
                    Spacer(modifier = Modifier.width(Spacing.x2))
                    StockProButton(
                        text = strings.postMovementSubmit,
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
