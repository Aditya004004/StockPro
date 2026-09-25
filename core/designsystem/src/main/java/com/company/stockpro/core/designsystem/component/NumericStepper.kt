package com.company.stockpro.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.company.stockpro.core.designsystem.theme.Radius
import com.company.stockpro.core.designsystem.theme.Spacing
import com.company.stockpro.core.designsystem.theme.StockProCodeTextStyle
import com.company.stockpro.core.designsystem.theme.StockProColors

/**
 * NumericStepper with 56dp touch targets conforming to Store Keeper shop floor ergonomics (rules.md §7.4).
 *
 * @param value Current quantity in whole units
 * @param onValueChange Callback with updated value
 * @param step Amount to increment/decrement per tap
 * @param minValue Lower bound (default 0)
 * @param maxValue Optional upper bound
 * @param unitLabel Optional unit label (e.g., "pcs", "kg")
 */
@Composable
fun NumericStepper(
    value: Long,
    onValueChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
    step: Long = 1L,
    minValue: Long = 0L,
    maxValue: Long = Long.MAX_VALUE,
    unitLabel: String? = null,
) {
    val canDecrement = value > minValue
    val canIncrement = value < maxValue

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(Radius.md)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, Radius.md)
            .background(StockProColors.Surface),
    ) {
        // Decrement button (56dp touch target)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(Spacing.storeKeeperMinHeight)
                .clickable(
                    enabled = canDecrement,
                    role = Role.Button,
                    onClick = { onValueChange((value - step).coerceAtLeast(minValue)) },
                ),
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = "Decrease quantity",
                tint = if (canDecrement) StockProColors.Primary else MaterialTheme.colorScheme.outlineVariant,
            )
        }

        // Display area with tabular numbers
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .widthIn(min = 72.dp)
                .padding(horizontal = Spacing.x2),
        ) {
            val text = if (unitLabel != null) "$value $unitLabel" else "$value"
            Text(
                text = text,
                style = StockProCodeTextStyle.copy(
                    fontSize = MaterialTheme.typography.titleMedium.fontSize,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                ),
            )
        }

        // Increment button (56dp touch target)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(Spacing.storeKeeperMinHeight)
                .clickable(
                    enabled = canIncrement,
                    role = Role.Button,
                    onClick = { onValueChange((value + step).coerceAtMost(maxValue)) },
                ),
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Increase quantity",
                tint = if (canIncrement) StockProColors.Primary else MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}
