package com.company.stockpro.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.company.stockpro.core.designsystem.theme.Radius
import com.company.stockpro.core.designsystem.theme.Spacing
import com.company.stockpro.core.designsystem.theme.StockProColors

enum class ButtonVariant {
    PRIMARY,
    SECONDARY,
    DANGER,
    GHOST
}

/**
 * Standard StockPro button conforming to design rules:
 * - Minimum touch target 48dp (or 56dp for Store Keeper / shop floor paths per rules.md §7.4).
 * - Fixed-width loading spinner in place to eliminate layout shifts mid-tap (rules.md §6.3).
 * - Distinct variants: PRIMARY, SECONDARY, DANGER, GHOST.
 */
@Composable
fun StockProButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.PRIMARY,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    minHeight: Dp = Spacing.storeKeeperMinHeight,
) {
    val buttonModifier = modifier.defaultMinSize(minHeight = minHeight)

    when (variant) {
        ButtonVariant.PRIMARY -> {
            Button(
                onClick = onClick,
                enabled = enabled && !isLoading,
                shape = Radius.sm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = StockProColors.Primary,
                    contentColor = Color.White,
                    disabledContainerColor = StockProColors.Primary.copy(alpha = 0.4f),
                    disabledContentColor = Color.White.copy(alpha = 0.7f),
                ),
                contentPadding = PaddingValues(horizontal = Spacing.x4, vertical = Spacing.x2),
                modifier = buttonModifier,
            ) {
                ButtonContent(text = text, isLoading = isLoading, contentColor = Color.White)
            }
        }

        ButtonVariant.SECONDARY -> {
            OutlinedButton(
                onClick = onClick,
                enabled = enabled && !isLoading,
                shape = Radius.sm,
                border = BorderStroke(1.dp, if (enabled && !isLoading) StockProColors.Primary else StockProColors.Primary.copy(alpha = 0.4f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = StockProColors.Primary,
                    disabledContentColor = StockProColors.Primary.copy(alpha = 0.4f),
                ),
                contentPadding = PaddingValues(horizontal = Spacing.x4, vertical = Spacing.x2),
                modifier = buttonModifier,
            ) {
                ButtonContent(text = text, isLoading = isLoading, contentColor = StockProColors.Primary)
            }
        }

        ButtonVariant.DANGER -> {
            Button(
                onClick = onClick,
                enabled = enabled && !isLoading,
                shape = Radius.sm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = StockProColors.Danger,
                    contentColor = Color.White,
                    disabledContainerColor = StockProColors.Danger.copy(alpha = 0.4f),
                    disabledContentColor = Color.White.copy(alpha = 0.7f),
                ),
                contentPadding = PaddingValues(horizontal = Spacing.x4, vertical = Spacing.x2),
                modifier = buttonModifier,
            ) {
                ButtonContent(text = text, isLoading = isLoading, contentColor = Color.White)
            }
        }

        ButtonVariant.GHOST -> {
            TextButton(
                onClick = onClick,
                enabled = enabled && !isLoading,
                shape = Radius.sm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = StockProColors.Primary,
                    disabledContentColor = StockProColors.Primary.copy(alpha = 0.4f),
                ),
                contentPadding = PaddingValues(horizontal = Spacing.x4, vertical = Spacing.x2),
                modifier = buttonModifier,
            ) {
                ButtonContent(text = text, isLoading = isLoading, contentColor = StockProColors.Primary)
            }
        }
    }
}

@Composable
private fun ButtonContent(
    text: String,
    isLoading: Boolean,
    contentColor: Color,
) {
    Box(contentAlignment = Alignment.Center) {
        // Keep text rendered invisibly during loading so button width does not shift
        Text(
            text = text,
            modifier = Modifier.alpha(if (isLoading) 0f else 1f),
        )
        if (isLoading) {
            CircularProgressIndicator(
                color = contentColor,
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
