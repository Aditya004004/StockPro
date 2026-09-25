package com.company.stockpro.core.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.company.stockpro.core.designsystem.theme.Spacing
import com.company.stockpro.core.designsystem.theme.StockProColors
import com.company.stockpro.core.domain.model.SyncStatus

/**
 * Inline banner for synchronization and offline status per rules.md §6.4.
 */
@Composable
fun SyncBanner(
    status: SyncStatus,
    onNeedsAttentionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shouldShow = status !is SyncStatus.Synced

    AnimatedVisibility(visible = shouldShow, modifier = modifier) {
        val bannerStyle = when (status) {
            is SyncStatus.NeedsAttention -> BannerStyle(
                bgColor = StockProColors.DangerContainer,
                contentColor = StockProColors.OnDangerContainer,
                icon = Icons.Default.Warning,
                text = "${status.failedCount} mutation(s) rejected by server · Tap to resolve",
            )
            is SyncStatus.Syncing -> BannerStyle(
                bgColor = StockProColors.PrimaryContainer,
                contentColor = StockProColors.Primary,
                icon = Icons.Default.Refresh,
                text = "Syncing changes with server…",
            )
            is SyncStatus.Pending -> BannerStyle(
                bgColor = StockProColors.WarningContainer,
                contentColor = StockProColors.OnWarningContainer,
                icon = Icons.Default.Info,
                text = "Offline · ${status.count} change(s) pending sync",
            )
            is SyncStatus.Synced -> BannerStyle(
                bgColor = StockProColors.SuccessContainer,
                contentColor = StockProColors.OnSuccessContainer,
                icon = Icons.Default.CheckCircle,
                text = "All changes synced",
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .background(bannerStyle.bgColor)
                .clickable(enabled = status is SyncStatus.NeedsAttention, onClick = onNeedsAttentionClick)
                .padding(horizontal = Spacing.screenGutter, vertical = Spacing.x2),
        ) {
            Icon(
                imageVector = bannerStyle.icon,
                contentDescription = null,
                tint = bannerStyle.contentColor,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(Spacing.x2))
            Text(
                text = bannerStyle.text,
                style = MaterialTheme.typography.labelMedium,
                color = bannerStyle.contentColor,
            )
        }
    }
}

private data class BannerStyle(
    val bgColor: Color,
    val contentColor: Color,
    val icon: ImageVector,
    val text: String,
)
