package com.company.stockpro.core.ui.component

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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.stockpro.core.designsystem.component.StatusBadge
import com.company.stockpro.core.designsystem.theme.Radius
import com.company.stockpro.core.designsystem.theme.Spacing
import com.company.stockpro.core.designsystem.theme.StockProCodeTextStyle
import com.company.stockpro.core.designsystem.theme.StockProColors
import com.company.stockpro.core.domain.model.Item
import com.company.stockpro.core.domain.model.StockLevel

/**
 * Production ItemListRow matching the approved design screenshot.
 * Displays item thumbnail, code, category, par level, location, status badge, and quantity.
 */
@Composable
fun ItemListRow(
    item: Item,
    onClick: () -> Unit,
    onOverflowClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = Radius.md,
        color = StockProColors.Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        shadowElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.cardPadding),
        ) {
            // 1. Thumbnail Placeholder
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .clip(Radius.md)
                    .background(StockProColors.SurfaceAlt)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), Radius.md),
            ) {
                Icon(
                    imageVector = Icons.Default.Category,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.size(28.dp),
                )
            }

            Spacer(modifier = Modifier.width(Spacing.x3))

            // 2. Middle Information Column
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Code + Category
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.code,
                        style = StockProCodeTextStyle.copy(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant),
                    )
                    if (item.categoryName.isNotBlank()) {
                        Text(
                            text = " | ${item.categoryName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.x1))

                // Par level + Location
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.x2),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(13.dp),
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "${item.parLevel.toUnitsDouble.toLong()} ${item.unitOfMeasure.lowercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    if (item.locationCode.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.size(13.dp),
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = item.locationCode,
                                style = StockProCodeTextStyle.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(Spacing.x2))

            // 3. Right Status & Quantity Column
            Column(
                horizontalAlignment = Alignment.End,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StatusBadge(level = item.stockLevel)
                    IconButton(
                        onClick = onOverflowClick,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options for ${item.name}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.x2))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val qtyColor = when (item.stockLevel) {
                        StockLevel.OUT_OF_STOCK -> StockProColors.Danger
                        StockLevel.LOW_STOCK -> StockProColors.Warning
                        else -> MaterialTheme.colorScheme.onSurface
                    }

                    Text(
                        text = "${item.onHand.toUnitsDouble.toLong()} ${item.unitOfMeasure.lowercase()}",
                        style = StockProCodeTextStyle.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = qtyColor,
                        ),
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "View detail",
                        tint = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

/**
 * 72dp shimmer row for inventory loading skeleton (rules.md §6.3).
 */
@Composable
fun ItemListRowSkeleton(
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = Radius.md,
        color = StockProColors.Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = modifier
            .fillMaxWidth()
            .height(Spacing.rowMinHeight),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.cardPadding),
        ) {
            SkeletonBox(modifier = Modifier.size(48.dp), shape = Radius.md)
            Spacer(modifier = Modifier.width(Spacing.x3))
            Column(modifier = Modifier.weight(1f)) {
                SkeletonBox(modifier = Modifier.fillMaxWidth(0.6f).height(16.dp))
                Spacer(modifier = Modifier.height(Spacing.x1))
                SkeletonBox(modifier = Modifier.fillMaxWidth(0.4f).height(12.dp))
            }
            Spacer(modifier = Modifier.width(Spacing.x3))
            Column(horizontalAlignment = Alignment.End) {
                SkeletonBox(modifier = Modifier.width(64.dp).height(20.dp), shape = Radius.pill)
                Spacer(modifier = Modifier.height(Spacing.x1))
                SkeletonBox(modifier = Modifier.width(48.dp).height(16.dp))
            }
        }
    }
}
