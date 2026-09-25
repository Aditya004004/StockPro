package com.company.stockpro.core.ui.component

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.company.stockpro.core.designsystem.theme.Radius
import com.company.stockpro.core.designsystem.theme.Spacing
import com.company.stockpro.core.designsystem.theme.StockProColors
import com.company.stockpro.core.domain.model.FailedOutboxEntry

/**
 * Dialog surfacing failed mutations from the Room Outbox (architecture.md §6, §7.3).
 *
 * Allows shop floor supervisors and managers to review server rejections (e.g.
 * INSUFFICIENT_STOCK or PERMISSION_DENIED) and either retry or discard them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeedsAttentionDialog(
    failedEntries: List<FailedOutboxEntry>,
    onRetry: (Long) -> Unit,
    onDiscard: (Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(Radius.md)
                            .background(StockProColors.DangerContainer),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Needs attention",
                            tint = StockProColors.Danger,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(Spacing.x3))
                    Column {
                        Text(
                            text = "Needs Attention",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "${failedEntries.size} mutation(s) rejected by server",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.x4))

                // List of failed items
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(Spacing.x2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                ) {
                    items(failedEntries, key = { it.id }) { entry ->
                        FailedEntryCard(
                            entry = entry,
                            onRetry = { onRetry(entry.id) },
                            onDiscard = { onDiscard(entry.id) },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.x4))

                // Dismiss action
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
private fun FailedEntryCard(
    entry: FailedOutboxEntry,
    onRetry: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = Radius.md,
        color = StockProColors.SurfaceAlt,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.x3),
        ) {
            Text(
                text = "Item: ${entry.itemId}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            entry.errorMessage?.let { errorMsg ->
                Spacer(modifier = Modifier.height(Spacing.x1))
                Text(
                    text = errorMsg,
                    style = MaterialTheme.typography.bodySmall,
                    color = StockProColors.Danger,
                )
            }

            Spacer(modifier = Modifier.height(Spacing.x2))

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedButton(
                    onClick = onDiscard,
                    shape = Radius.sm,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StockProColors.Danger),
                ) {
                    Text("Discard")
                }
                Spacer(modifier = Modifier.width(Spacing.x2))
                Button(
                    onClick = onRetry,
                    shape = Radius.sm,
                    colors = ButtonDefaults.buttonColors(containerColor = StockProColors.Primary),
                ) {
                    Text("Retry")
                }
            }
        }
    }
}
