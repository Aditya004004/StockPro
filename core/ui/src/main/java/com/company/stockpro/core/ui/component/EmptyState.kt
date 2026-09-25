package com.company.stockpro.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.company.stockpro.core.designsystem.component.ButtonVariant
import com.company.stockpro.core.designsystem.component.StockProButton
import com.company.stockpro.core.designsystem.theme.Radius
import com.company.stockpro.core.designsystem.theme.Spacing
import com.company.stockpro.core.designsystem.theme.StockProColors

sealed interface EmptyReason {
    data object NoItems : EmptyReason
    data class SearchMiss(val query: String) : EmptyReason
    data class FilterMiss(val className: String) : EmptyReason
}

/**
 * Standard StockPro empty state naming the exact cause per rules.md §6.2.
 */
@Composable
fun EmptyState(
    reason: EmptyReason,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val details = when (reason) {
        is EmptyReason.NoItems -> EmptyDetails(
            icon = Icons.Default.Inbox,
            title = "No items yet",
            message = "Add your first item to start tracking stock in this plant.",
            actionLabel = "Add item",
        )

        is EmptyReason.SearchMiss -> EmptyDetails(
            icon = Icons.Default.SearchOff,
            title = "No results found",
            message = "No items match '${reason.query}'. Check the spelling or try searching by item code.",
            actionLabel = "Clear search",
        )

        is EmptyReason.FilterMiss -> EmptyDetails(
            icon = Icons.Default.FilterAlt,
            title = "No ${reason.className.lowercase()} found",
            message = "No items match these filters. Try switching category tabs or clearing filters.",
            actionLabel = "Clear filters",
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.screenGutter),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(64.dp)
                .clip(Radius.xl)
                .background(StockProColors.PrimaryContainer),
        ) {
            Icon(
                imageVector = details.icon,
                contentDescription = details.title,
                tint = StockProColors.Primary,
                modifier = Modifier.size(32.dp),
            )
        }

        Spacer(modifier = Modifier.height(Spacing.x4))

        Text(
            text = details.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(Spacing.x2))

        Text(
            text = details.message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = Spacing.x4),
        )

        Spacer(modifier = Modifier.height(Spacing.x6))

        StockProButton(
            text = details.actionLabel,
            onClick = onActionClick,
            variant = ButtonVariant.SECONDARY,
        )
    }
}

private data class EmptyDetails(
    val icon: ImageVector,
    val title: String,
    val message: String,
    val actionLabel: String,
)
