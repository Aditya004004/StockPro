package com.company.stockpro.core.ui.component.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.stockpro.core.designsystem.theme.Radius
import com.company.stockpro.core.designsystem.theme.Spacing
import com.company.stockpro.core.designsystem.theme.StockProColors
import com.company.stockpro.core.domain.model.AppNotification
import com.company.stockpro.core.domain.model.NotificationType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterDialog(
    notifications: List<AppNotification>,
    onDismissRequest: () -> Unit,
    onNotificationClick: (String?) -> Unit,
    onMarkAllAsRead: () -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showUnreadOnly by remember { mutableStateOf(false) }

    val unreadCount = remember(notifications) { notifications.count { !it.isRead } }
    val displayList = remember(notifications, showUnreadOnly) {
        if (showUnreadOnly) notifications.filter { !it.isRead } else notifications
    }

    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier.fillMaxWidth(),
    ) {
        Surface(
            shape = Radius.xl,
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
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Notifications",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = StockProColors.TextPrimary,
                            )
                            if (unreadCount > 0) {
                                Spacer(modifier = Modifier.width(Spacing.x2))
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(StockProColors.Primary)
                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                ) {
                                    Text(
                                        text = "$unreadCount",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                    )
                                }
                            }
                        }
                    }

                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = StockProColors.TextSecondary,
                        )
                    }
                }

                // Filter & Mark All Read Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.x2)) {
                        FilterChip(
                            selected = !showUnreadOnly,
                            onClick = { showUnreadOnly = false },
                            label = { Text("All (${notifications.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StockProColors.PrimaryContainer,
                                selectedLabelColor = StockProColors.Primary,
                            ),
                        )
                        FilterChip(
                            selected = showUnreadOnly,
                            onClick = { showUnreadOnly = true },
                            label = { Text("Unread ($unreadCount)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StockProColors.PrimaryContainer,
                                selectedLabelColor = StockProColors.Primary,
                            ),
                        )
                    }

                    if (unreadCount > 0) {
                        TextButton(onClick = onMarkAllAsRead) {
                            Text("Mark read", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.x2))
                HorizontalDivider(color = StockProColors.OutlineSubtle)
                Spacer(modifier = Modifier.height(Spacing.x2))

                // Notifications List
                if (displayList.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsNone,
                            contentDescription = null,
                            tint = StockProColors.TextTertiary,
                            modifier = Modifier.size(48.dp),
                        )
                        Spacer(modifier = Modifier.height(Spacing.x2))
                        Text(
                            text = if (showUnreadOnly) "No unread alerts" else "No notifications",
                            style = MaterialTheme.typography.bodyMedium,
                            color = StockProColors.TextSecondary,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(Spacing.x2),
                    ) {
                        items(displayList, key = { it.id }) { notification ->
                            NotificationItemRow(
                                notification = notification,
                                onClick = { onNotificationClick(notification.entityRef) },
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.x2))
                HorizontalDivider(color = StockProColors.OutlineSubtle)

                // Bottom Clear All Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    if (notifications.isNotEmpty()) {
                        TextButton(onClick = onClearAll) {
                            Text("Clear all", color = StockProColors.Danger)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationItemRow(
    notification: AppNotification,
    onClick: () -> Unit,
) {
    val (icon, iconBg, iconTint) = when (notification.type) {
        NotificationType.LOW_STOCK, NotificationType.OUT_OF_STOCK ->
            Triple(Icons.Filled.Warning, StockProColors.WarningContainer, StockProColors.Warning)
        NotificationType.PO_DUE ->
            Triple(Icons.Filled.Notifications, StockProColors.PrimaryContainer, StockProColors.Primary)
        NotificationType.LARGE_ADJUSTMENT, NotificationType.AUDIT_DRIFT ->
            Triple(Icons.Filled.Warning, StockProColors.DangerContainer, StockProColors.Danger)
        NotificationType.SYNC_FAILURE ->
            Triple(Icons.Filled.Sync, StockProColors.ProductionContainer, StockProColors.Production)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radius.md)
            .background(
                if (notification.isRead) StockProColors.Surface else StockProColors.SurfaceAlt,
            )
            .clickable(onClick = onClick)
            .padding(Spacing.cardPadding),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(Radius.sm)
                .background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp),
            )
        }

        Spacer(modifier = Modifier.width(Spacing.x3))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (notification.isRead) FontWeight.Medium else FontWeight.Bold,
                    color = StockProColors.TextPrimary,
                )
                if (!notification.isRead) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(StockProColors.Primary),
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = notification.body,
                style = MaterialTheme.typography.bodySmall,
                color = StockProColors.TextSecondary,
                lineHeight = 16.sp,
            )
        }
    }
}
