package com.company.stockpro.feature.inventory

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.company.stockpro.core.designsystem.theme.StockProStrings
import com.company.stockpro.core.domain.model.Currency
import com.company.stockpro.core.domain.model.Item
import com.company.stockpro.core.domain.model.ItemClass
import com.company.stockpro.core.domain.model.Money
import com.company.stockpro.core.domain.model.Quantity
import java.util.UUID

/**
 * Modern Industrial Modal Dialog for creating master inventory items.
 * Matches precision Figma/design specifications with leading semantic icons,
 * dedicated KPI metric pill cards, styled definition banners, and action controls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (Item) -> Unit,
    currency: Currency = Currency.INR,
    modifier: Modifier = Modifier,
) {
    val strings = StockProStrings.current

    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var itemClass by remember { mutableStateOf(ItemClass.COMPONENT) }
    var categoryName by remember { mutableStateOf("") }
    var unitOfMeasure by remember { mutableStateOf("PCS") }
    var locationCode by remember { mutableStateOf("") }
    var onHandText by remember { mutableStateOf("0") }
    var reorderLevelText by remember { mutableStateOf("10") }
    var parLevelText by remember { mutableStateOf("50") }
    var unitCostText by remember { mutableStateOf("0.00") }

    var classDropdownExpanded by remember { mutableStateOf(false) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var uomDropdownExpanded by remember { mutableStateOf(false) }

    val onHandValid = onHandText.toDoubleOrNull()?.let { it >= 0 } == true
    val reorderValid = reorderLevelText.toDoubleOrNull()?.let { it >= 0 } == true
    val parValid = parLevelText.toDoubleOrNull()?.let { it >= 0 } == true
    val unitCostValid = unitCostText.toDoubleOrNull()?.let { it >= 0 } == true
    val codeValid = code.isNotBlank()
    val nameValid = name.isNotBlank()
    val categoryValid = categoryName.isNotBlank()

    val isValid = codeValid && nameValid && categoryValid && onHandValid && reorderValid && parValid && unitCostValid

    val inputColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        disabledContainerColor = Color.White,
        focusedBorderColor = Color(0xFF2563EB),
        unfocusedBorderColor = Color(0xFFE2E8F0),
        cursorColor = Color(0xFF2563EB),
        focusedTextColor = Color(0xFF1E293B),
        unfocusedTextColor = Color(0xFF1E293B),
    )

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = modifier.padding(horizontal = 16.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 10.dp,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                // 1. Header with Badge, Title/Subtitle, and Circular Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFE8F1FC)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = strings.addNewItemTitle,
                                style = TextStyle(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                ),
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = strings.createMasterDataSubtitle,
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B),
                                ),
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier
                            .size(32.dp)
                            .clickable { onDismiss() },
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Item Code Field
                AddItemFieldLabel(label = strings.itemCodeLabel, isRequired = true)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    placeholder = {
                        Text(
                            text = strings.enterItemCodePlaceholder,
                            color = Color(0xFF94A3B8),
                            fontSize = 14.sp,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = inputColors,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Item Name Field
                AddItemFieldLabel(label = strings.itemNameLabel, isRequired = true)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = {
                        Text(
                            text = strings.enterItemNamePlaceholder,
                            color = Color(0xFF94A3B8),
                            fontSize = 14.sp,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = inputColors,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 4. Item Class Selector
                AddItemFieldLabel(label = strings.itemClassLabel)
                Spacer(modifier = Modifier.height(6.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clickable { classDropdownExpanded = true },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = itemClass.name.replace("_", " "),
                                    style = TextStyle(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E293B),
                                    ),
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = classDropdownExpanded,
                        onDismissRequest = { classDropdownExpanded = false },
                    ) {
                        ItemClass.entries.forEach { cls ->
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

                Spacer(modifier = Modifier.height(14.dp))

                // 5. Category & UOM Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Category
                    Column(modifier = Modifier.weight(1f)) {
                        AddItemFieldLabel(label = strings.categoryLabel, isRequired = true)
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .clickable { categoryDropdownExpanded = true },
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocalOffer,
                                            contentDescription = null,
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = categoryName.ifBlank { strings.selectCategoryPlaceholder },
                                            style = TextStyle(
                                                fontSize = 13.sp,
                                                fontWeight = if (categoryName.isBlank()) FontWeight.Normal else FontWeight.Medium,
                                                color = if (categoryName.isBlank()) Color(0xFF94A3B8) else Color(0xFF1E293B),
                                            ),
                                            maxLines = 1,
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = categoryDropdownExpanded,
                                onDismissRequest = { categoryDropdownExpanded = false },
                            ) {
                                val commonCategories = listOf(
                                    "Components",
                                    "Fasteners",
                                    "Raw Materials",
                                    "Packaging",
                                    "Finished Goods",
                                    "Electronics",
                                    "Chemicals",
                                    "Consumables",
                                )
                                commonCategories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat) },
                                        onClick = {
                                            categoryName = cat
                                            categoryDropdownExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }

                    // UOM
                    Column(modifier = Modifier.weight(0.75f)) {
                        AddItemFieldLabel(label = strings.uomLabel)
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .clickable { uomDropdownExpanded = true },
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Inventory2,
                                            contentDescription = null,
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = unitOfMeasure,
                                            style = TextStyle(
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1E293B),
                                            ),
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = uomDropdownExpanded,
                                onDismissRequest = { uomDropdownExpanded = false },
                            ) {
                                listOf("PCS", "KG", "LTR", "MTR", "BOX", "SET", "ROLL").forEach { uom ->
                                    DropdownMenuItem(
                                        text = { Text(uom) },
                                        onClick = {
                                            unitOfMeasure = uom
                                            uomDropdownExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 6. Location / Bin Field
                AddItemFieldLabel(label = strings.locationBinLabel)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = locationCode,
                    onValueChange = { locationCode = it },
                    placeholder = {
                        Text(
                            text = strings.enterLocationBinPlaceholder,
                            color = Color(0xFF94A3B8),
                            fontSize = 14.sp,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = inputColors,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 7. The 3-Metric KPI Pill Cards (On Hand, Low Stock, Par Level)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MetricInputCard(
                        title = strings.onHandLabel,
                        icon = Icons.Default.Inventory2,
                        iconTint = Color(0xFF2563EB),
                        value = onHandText,
                        onValueChange = { onHandText = it },
                        leadingVisual = {
                            Icon(
                                imageVector = Icons.Default.Adjust,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(14.dp),
                            )
                        },
                        modifier = Modifier.weight(1f),
                    )

                    MetricInputCard(
                        title = strings.lowStock,
                        icon = Icons.Default.Warning,
                        iconTint = Color(0xFFF59E0B),
                        value = reorderLevelText,
                        onValueChange = { reorderLevelText = it },
                        modifier = Modifier.weight(1f),
                    )

                    MetricInputCard(
                        title = strings.parLevelLabel,
                        icon = Icons.Default.TrackChanges,
                        iconTint = Color(0xFF10B981),
                        value = parLevelText,
                        onValueChange = { parLevelText = it },
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 8. Definition Info Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFEBF3FE),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "• ${strings.lowStockTooltipDesc}",
                                style = TextStyle(
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF475569),
                                    lineHeight = 16.sp,
                                ),
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "• ${strings.parLevelTooltipDesc}",
                                style = TextStyle(
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF475569),
                                    lineHeight = 16.sp,
                                ),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 9. Unit Cost
                AddItemFieldLabel(label = strings.unitCostLabel)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = unitCostText,
                    onValueChange = { unitCostText = it },
                    leadingIcon = {
                        Text(
                            text = currency.symbol.ifBlank { "$" },
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B),
                            ),
                        )
                    },
                    placeholder = {
                        Text(
                            text = "0.00",
                            color = Color(0xFF94A3B8),
                            fontSize = 14.sp,
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = inputColors,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 10. Action Buttons (Cancel & Create Item)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, Color(0xFF2563EB)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF2563EB),
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                    ) {
                        Text(
                            text = strings.cancel,
                            style = TextStyle(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2563EB),
                            ),
                        )
                    }

                    Button(
                        onClick = {
                            val onHandDouble = onHandText.toDoubleOrNull() ?: 0.0
                            val reorderDouble = reorderLevelText.toDoubleOrNull() ?: 10.0
                            val parDouble = parLevelText.toDoubleOrNull() ?: 50.0
                            val unitCostMinor = ((unitCostText.toDoubleOrNull() ?: 0.0) * currency.minorUnitFactor).toLong()

                            val newItem = Item(
                                id = UUID.randomUUID().toString(),
                                code = code.trim(),
                                name = name.trim(),
                                itemClass = itemClass,
                                categoryName = categoryName.trim().ifBlank { "General" },
                                unitOfMeasure = unitOfMeasure.trim().ifBlank { "PCS" },
                                onHand = Quantity((onHandDouble * 1000).toLong()),
                                reserved = Quantity.ZERO,
                                inProduction = Quantity.ZERO,
                                reorderLevel = Quantity((reorderDouble * 1000).toLong()),
                                parLevel = Quantity((parDouble * 1000).toLong()),
                                unitCost = Money(unitCostMinor),
                                locationCode = locationCode.trim().ifBlank { "STORE" },
                            )
                            onConfirm(newItem)
                        },
                        enabled = isValid,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1D4ED8),
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFF93C5FD),
                            disabledContentColor = Color.White,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.createItemButton,
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddItemFieldLabel(
    label: String,
    isRequired: Boolean = false,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B),
            ),
        )
        if (isRequired) {
            Text(
                text = " *",
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEF4444),
                ),
            )
        }
    }
}

@Composable
private fun MetricInputCard(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    leadingVisual: (@Composable () -> Unit)? = null,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF0F6FE),
        border = BorderStroke(1.dp, Color(0xFFE2EDFB)),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = title,
                    style = TextStyle(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B),
                    ),
                    maxLines = 1,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    leadingVisual?.let {
                        it()
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        textStyle = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B),
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
