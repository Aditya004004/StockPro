package com.company.stockpro.feature.inventory

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.stockpro.core.designsystem.component.ButtonVariant
import com.company.stockpro.core.designsystem.component.StockProButton
import com.company.stockpro.core.designsystem.component.StockProTextField
import com.company.stockpro.core.designsystem.theme.Radius
import com.company.stockpro.core.designsystem.theme.Spacing
import com.company.stockpro.core.designsystem.theme.StockProColors
import com.company.stockpro.core.domain.model.Item
import com.company.stockpro.core.domain.model.ItemClass
import com.company.stockpro.core.domain.model.Money
import com.company.stockpro.core.domain.model.Quantity
import java.util.UUID

/**
 * Modal dialog for creating a new inventory master item per W-6 remediation.
 * Uses design system components (StockProButton, StockProTextField, StockProColors).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (Item) -> Unit,
    modifier: Modifier = Modifier,
) {
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var itemClass by remember { mutableStateOf(ItemClass.COMPONENT) }
    var categoryName by remember { mutableStateOf("") }
    var unitOfMeasure by remember { mutableStateOf("PCS") }
    var locationCode by remember { mutableStateOf("") }
    var onHandText by remember { mutableStateOf("0") }
    var reorderLevelText by remember { mutableStateOf("10") }
    var parLevelText by remember { mutableStateOf("50") }
    var unitCostRupeesText by remember { mutableStateOf("0.00") }
    var classDropdownExpanded by remember { mutableStateOf(false) }

    val isValid = code.isNotBlank() && name.isNotBlank() && categoryName.isNotBlank()

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
    ) {
        Surface(
            shape = Radius.lg,
            color = StockProColors.Surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.x4),
        ) {
            Column(
                modifier = Modifier
                    .padding(Spacing.x4)
                    .verticalScroll(rememberScrollState()),
            ) {
                // Header
                Text(
                    text = "Add New Item",
                    style = MaterialTheme.typography.titleMedium,
                    color = StockProColors.TextPrimary,
                )
                Text(
                    text = "Create master data entry for inventory tracking",
                    style = MaterialTheme.typography.bodySmall,
                    color = StockProColors.TextSecondary,
                )

                Spacer(modifier = Modifier.height(Spacing.x4))

                // Item Code
                StockProTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    label = "Item Code *",
                    placeholder = "e.g. BRG-002",
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(Spacing.x3))

                // Item Name
                StockProTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Item Name *",
                    placeholder = "e.g. Roller Bearing 50mm",
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(Spacing.x3))

                // Item Class Selector
                Text(
                    text = "Item Class",
                    style = MaterialTheme.typography.labelSmall,
                    color = StockProColors.TextSecondary,
                )
                Spacer(modifier = Modifier.height(Spacing.x1))
                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(Radius.sm)
                            .background(StockProColors.SurfaceAlt)
                            .border(1.dp, StockProColors.Outline, Radius.sm)
                            .clickable { classDropdownExpanded = true }
                            .padding(horizontal = Spacing.x3, vertical = Spacing.x2),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = itemClass.name.replace("_", " "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = StockProColors.TextPrimary,
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Class",
                            tint = StockProColors.TextSecondary,
                        )
                    }

                    DropdownMenu(
                        expanded = classDropdownExpanded,
                        onDismissRequest = { classDropdownExpanded = false },
                    ) {
                        ItemClass.values().forEach { cls ->
                            DropdownMenuItem(
                                text = { Text(cls.name.replace("_", " ")) },
                                onClick = {
                                    itemClass = cls
                                    classDropdownExpanded = false
                                },
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.x3))

                // Category & Unit of Measure Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.x2),
                ) {
                    StockProTextField(
                        value = categoryName,
                        onValueChange = { categoryName = it },
                        label = "Category *",
                        placeholder = "e.g. Fasteners",
                        modifier = Modifier.weight(1f),
                    )
                    StockProTextField(
                        value = unitOfMeasure,
                        onValueChange = { unitOfMeasure = it.uppercase() },
                        label = "UOM",
                        placeholder = "PCS",
                        modifier = Modifier.weight(0.7f),
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.x3))

                // Location Code
                StockProTextField(
                    value = locationCode,
                    onValueChange = { locationCode = it },
                    label = "Location / Bin",
                    placeholder = "e.g. BIN-A-12",
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(Spacing.x3))

                // Quantities Row (Initial On Hand, Reorder Level, Par Level)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.x2),
                ) {
                    StockProTextField(
                        value = onHandText,
                        onValueChange = { onHandText = it },
                        label = "On Hand",
                        modifier = Modifier.weight(1f),
                    )
                    StockProTextField(
                        value = reorderLevelText,
                        onValueChange = { reorderLevelText = it },
                        label = "Reorder",
                        modifier = Modifier.weight(1f),
                    )
                    StockProTextField(
                        value = parLevelText,
                        onValueChange = { parLevelText = it },
                        label = "Par Level",
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.x3))

                // Unit Cost (Rupees)
                StockProTextField(
                    value = unitCostRupeesText,
                    onValueChange = { unitCostRupeesText = it },
                    label = "Unit Cost (₹)",
                    placeholder = "0.00",
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(Spacing.x5))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.x2),
                ) {
                    StockProButton(
                        text = "Cancel",
                        variant = ButtonVariant.SECONDARY,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    )
                    StockProButton(
                        text = "Create Item",
                        variant = ButtonVariant.PRIMARY,
                        enabled = isValid,
                        onClick = {
                            val onHandDouble = onHandText.toDoubleOrNull() ?: 0.0
                            val reorderDouble = reorderLevelText.toDoubleOrNull() ?: 10.0
                            val parDouble = parLevelText.toDoubleOrNull() ?: 50.0
                            val unitCostPaise = ((unitCostRupeesText.toDoubleOrNull() ?: 0.0) * 100).toLong()

                            val newItem = Item(
                                id = UUID.randomUUID().toString(),
                                code = code.trim(),
                                name = name.trim(),
                                itemClass = itemClass,
                                categoryName = categoryName.trim(),
                                unitOfMeasure = unitOfMeasure.trim().ifBlank { "PCS" },
                                onHand = Quantity((onHandDouble * 1000).toLong()),
                                reserved = Quantity.ZERO,
                                inProduction = Quantity.ZERO,
                                reorderLevel = Quantity((reorderDouble * 1000).toLong()),
                                parLevel = Quantity((parDouble * 1000).toLong()),
                                unitCost = Money(unitCostPaise),
                                locationCode = locationCode.trim().ifBlank { "STORE" },
                            )
                            onConfirm(newItem)
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
