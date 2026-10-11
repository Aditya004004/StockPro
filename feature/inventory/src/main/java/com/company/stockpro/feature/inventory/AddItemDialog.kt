package com.company.stockpro.feature.inventory

import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.safeDrawingPadding
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.company.stockpro.core.designsystem.theme.StockProColors
import com.company.stockpro.core.designsystem.theme.StockProStrings
import com.company.stockpro.core.domain.model.Currency
import com.company.stockpro.core.domain.model.Item
import com.company.stockpro.core.domain.model.ItemClass
import com.company.stockpro.core.domain.model.Money
import com.company.stockpro.core.domain.model.Quantity
import java.util.UUID

/**
 * Modern Industrial Modal Dialog for creating master inventory items.
 * Fully supports dynamic Light and Dark mode theming through design tokens.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (Item) -> Unit,
    currency: Currency = Currency.INR,
    modifier: Modifier = Modifier,
) {
    val strings = StockProStrings.current
    val isDark = StockProColors.isDark
    val fieldBg = if (isDark) StockProColors.SurfaceAlt else Color.White

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
        focusedContainerColor = fieldBg,
        unfocusedContainerColor = fieldBg,
        disabledContainerColor = fieldBg,
        focusedBorderColor = StockProColors.Primary,
        unfocusedBorderColor = StockProColors.Outline,
        cursorColor = StockProColors.Primary,
        focusedTextColor = StockProColors.TextPrimary,
        unfocusedTextColor = StockProColors.TextPrimary,
    )

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    Dialog(
        onDismissRequest = {
            focusManager.clearFocus()
            keyboardController?.hide()
            onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            dialogWindow?.let { win ->
                win.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                )
                @Suppress("DEPRECATION")
                win.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        onDismiss()
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = StockProColors.Surface,
                    shadowElevation = 10.dp,
                    border = BorderStroke(1.dp, StockProColors.Outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { /* Consume clicks inside card */ },
                        ),
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
                                .background(if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.5f) else Color(0xFFE8F1FC)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB),
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
                                    color = StockProColors.TextPrimary,
                                ),
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = strings.createMasterDataSubtitle,
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    color = StockProColors.TextSecondary,
                                ),
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = StockProColors.SurfaceAlt,
                        modifier = Modifier
                            .size(32.dp)
                            .clickable {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                onDismiss()
                            },
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = StockProColors.TextSecondary,
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
                            color = StockProColors.TextTertiary,
                            fontSize = 14.sp,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = null,
                            tint = StockProColors.TextSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                    ),
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
                            color = StockProColors.TextTertiary,
                            fontSize = 14.sp,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = StockProColors.TextSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                    ),
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
                        color = fieldBg,
                        border = BorderStroke(1.dp, StockProColors.Outline),
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
                                    tint = StockProColors.TextSecondary,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = itemClass.name.replace("_", " "),
                                    style = TextStyle(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StockProColors.TextPrimary,
                                    ),
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = StockProColors.TextSecondary,
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
                                color = fieldBg,
                                border = BorderStroke(1.dp, StockProColors.Outline),
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
                                            tint = StockProColors.TextSecondary,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = categoryName.ifBlank { strings.selectCategoryPlaceholder },
                                            style = TextStyle(
                                                fontSize = 13.sp,
                                                fontWeight = if (categoryName.isBlank()) FontWeight.Normal else FontWeight.Medium,
                                                color = if (categoryName.isBlank()) StockProColors.TextTertiary else StockProColors.TextPrimary,
                                            ),
                                            maxLines = 1,
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = StockProColors.TextSecondary,
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
                                color = fieldBg,
                                border = BorderStroke(1.dp, StockProColors.Outline),
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
                                            tint = StockProColors.TextSecondary,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = unitOfMeasure,
                                            style = TextStyle(
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = StockProColors.TextPrimary,
                                            ),
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = StockProColors.TextSecondary,
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
                            color = StockProColors.TextTertiary,
                            fontSize = 14.sp,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = StockProColors.TextSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                    ),
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
                        iconTint = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB),
                        value = onHandText,
                        onValueChange = { onHandText = it },
                        leadingVisual = {
                            Icon(
                                imageVector = Icons.Default.Adjust,
                                contentDescription = null,
                                tint = StockProColors.TextTertiary,
                                modifier = Modifier.size(14.dp),
                            )
                        },
                        modifier = Modifier.weight(1f),
                    )

                    MetricInputCard(
                        title = strings.lowStock,
                        icon = Icons.Default.Warning,
                        iconTint = if (isDark) Color(0xFFFBBF24) else Color(0xFFF59E0B),
                        value = reorderLevelText,
                        onValueChange = { reorderLevelText = it },
                        modifier = Modifier.weight(1f),
                    )

                    MetricInputCard(
                        title = strings.parLevelLabel,
                        icon = Icons.Default.TrackChanges,
                        iconTint = if (isDark) Color(0xFF34D399) else Color(0xFF10B981),
                        value = parLevelText,
                        onValueChange = { parLevelText = it },
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 8. Definition Info Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) StockProColors.PrimaryContainer.copy(alpha = 0.35f) else Color(0xFFEBF3FE),
                    border = BorderStroke(1.dp, if (isDark) StockProColors.Primary.copy(alpha = 0.3f) else Color(0xFFBFDBFE)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB),
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "• ${strings.lowStockTooltipDesc}",
                                style = TextStyle(
                                    fontSize = 11.5.sp,
                                    color = StockProColors.TextSecondary,
                                    lineHeight = 16.sp,
                                ),
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "• ${strings.parLevelTooltipDesc}",
                                style = TextStyle(
                                    fontSize = 11.5.sp,
                                    color = StockProColors.TextSecondary,
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
                                color = StockProColors.TextSecondary,
                            ),
                        )
                    },
                    placeholder = {
                        Text(
                            text = "0.00",
                            color = StockProColors.TextTertiary,
                            fontSize = 14.sp,
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = inputColors,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        }
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 10. Action Buttons (Cancel & Create Item)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, StockProColors.Primary),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = StockProColors.Primary,
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
                                color = StockProColors.Primary,
                            ),
                        )
                    }

                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
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
                            containerColor = StockProColors.Primary,
                            contentColor = Color.White,
                            disabledContainerColor = if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.5f) else Color(0xFF93C5FD),
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

                Spacer(modifier = Modifier.height(20.dp))
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
                color = StockProColors.TextPrimary,
            ),
        )
        if (isRequired) {
            Text(
                text = " *",
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = StockProColors.Danger,
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
    val isDark = StockProColors.isDark
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isDark) StockProColors.SurfaceAlt else Color(0xFFF0F6FE),
        border = BorderStroke(1.dp, if (isDark) StockProColors.Outline else Color(0xFFE2EDFB)),
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
                        color = StockProColors.TextPrimary,
                    ),
                    maxLines = 1,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isDark) StockProColors.Surface else Color.White,
                border = BorderStroke(1.dp, StockProColors.Outline),
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
                            color = StockProColors.TextPrimary,
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
