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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
 * Precision Industrial Item Card matching Stitch Design System (screenshot_stitch_inventory.png).
 * Displays SKU badge, location coordinate, par level buffer progress bar, on-hand quantity,
 * and quick floor movement action triggers.
 */
@Composable
fun ItemListRow(
    item: Item,
    onClick: () -> Unit,
    onOverflowClick: () -> Unit,
    modifier: Modifier = Modifier,
    onReceiveClick: (() -> Unit)? = null,
    onIssueClick: (() -> Unit)? = null,
) {
    val currentUnits = item.onHand.toUnitsDouble
    val parUnits = item.parLevel.toUnitsDouble
    val ratio = if (parUnits > 0) (currentUnits / (parUnits * 1.5)).toFloat().coerceIn(0f, 1f) else 0.5f

    val progressColor = when (item.stockLevel) {
        StockLevel.OUT_OF_STOCK -> StockProColors.Danger
        StockLevel.LOW_STOCK -> StockProColors.Warning
        else -> StockProColors.Success
    }

    Surface(
        shape = Radius.lg,
        color = StockProColors.Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, StockProColors.OutlineSubtle),
        shadowElevation = 0.5.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(Radius.lg)
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.cardPadding),
        ) {
            // 1. Top Line: SKU Monospace Badge, Item Name & Status Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Surface(
                        shape = Radius.xs,
                        color = StockProColors.SurfaceAlt,
                        border = androidx.compose.foundation.BorderStroke(1.dp, StockProColors.Outline),
                    ) {
                        Text(
                            text = item.code,
                            style = StockProCodeTextStyle.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = StockProColors.TextPrimary,
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }

                    Spacer(modifier = Modifier.width(Spacing.x2))

                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = StockProColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(modifier = Modifier.width(Spacing.x1))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(level = item.stockLevel)

                    IconButton(
                        onClick = onOverflowClick,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options for ${item.name}",
                            tint = StockProColors.TextSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.x1))

            // 2. Metadata Line: Category pill, Location coordinate, Par level
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.x2),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (item.categoryName.isNotBlank()) {
                    Surface(
                        shape = Radius.xs,
                        color = StockProColors.PrimaryContainer.copy(alpha = 0.5f),
                    ) {
                        Text(
                            text = item.categoryName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                color = StockProColors.Primary,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            maxLines = 1,
                        )
                    }
                }

                if (item.locationCode.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = StockProColors.TextSecondary,
                            modifier = Modifier.size(13.dp),
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = item.locationCode,
                            style = StockProCodeTextStyle.copy(
                                fontSize = 11.sp,
                                color = StockProColors.TextSecondary,
                                fontWeight = FontWeight.Medium,
                            ),
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Archive,
                        contentDescription = null,
                        tint = StockProColors.TextTertiary,
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "Par: ${parUnits.toLong()} ${item.unitOfMeasure.lowercase()}",
                        style = StockProCodeTextStyle.copy(
                            fontSize = 10.sp,
                            color = StockProColors.TextSecondary,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.x2))

            // 3. Buffer Capacity Gauge
            LinearProgressIndicator(
                progress = { ratio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(Radius.pill),
                color = progressColor,
                trackColor = StockProColors.OutlineSubtle,
            )

            Spacer(modifier = Modifier.height(Spacing.x2))

            // 4. Bottom Line: Large On Hand Qty + Unit Cost + Quick Movement CTAs
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    val qtyColor = when (item.stockLevel) {
                        StockLevel.OUT_OF_STOCK -> StockProColors.Danger
                        StockLevel.LOW_STOCK -> StockProColors.Warning
                        else -> StockProColors.TextPrimary
                    }

                    Text(
                        text = "${currentUnits.toLong()}",
                        style = StockProCodeTextStyle.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = qtyColor,
                        ),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = item.unitOfMeasure.lowercase(),
                        style = StockProCodeTextStyle.copy(
                            fontSize = 12.sp,
                            color = StockProColors.TextSecondary,
                            fontWeight = FontWeight.Medium,
                        ),
                        modifier = Modifier.padding(bottom = 1.dp),
                    )

                    if (item.unitCost.paise > 0L) {
                        Spacer(modifier = Modifier.width(Spacing.x2))
                        Text(
                            text = "•  ₹ %.2f / unit".format(item.unitCost.toRupeesDouble),
                            style = StockProCodeTextStyle.copy(
                                fontSize = 11.sp,
                                color = StockProColors.TextTertiary,
                            ),
                            modifier = Modifier.padding(bottom = 1.dp),
                        )
                    }
                }

                // Quick Floor Movement Action Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.x1),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (onReceiveClick != null) {
                        Surface(
                            shape = Radius.xs,
                            color = StockProColors.SuccessContainer,
                            modifier = Modifier
                                .clip(Radius.xs)
                                .clickable(onClick = onReceiveClick),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Receive",
                                    tint = StockProColors.Success,
                                    modifier = Modifier.size(12.dp),
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "Recv",
                                    style = StockProCodeTextStyle.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StockProColors.Success,
                                    ),
                                )
                            }
                        }
                    }

                    if (onIssueClick != null) {
                        Surface(
                            shape = Radius.xs,
                            color = StockProColors.PrimaryContainer,
                            modifier = Modifier
                                .clip(Radius.xs)
                                .clickable(onClick = onIssueClick),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Issue",
                                    tint = StockProColors.Primary,
                                    modifier = Modifier.size(12.dp),
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "Issue",
                                    style = StockProCodeTextStyle.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StockProColors.Primary,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Modern industrial shimmer row for inventory loading skeleton.
 */
@Composable
fun ItemListRowSkeleton(
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = Radius.lg,
        color = StockProColors.Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, StockProColors.OutlineSubtle),
        modifier = modifier
            .fillMaxWidth()
            .height(108.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.cardPadding),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                SkeletonBox(modifier = Modifier.width(80.dp).height(18.dp), shape = Radius.xs)
                SkeletonBox(modifier = Modifier.width(60.dp).height(18.dp), shape = Radius.pill)
            }
            SkeletonBox(modifier = Modifier.fillMaxWidth(0.7f).height(16.dp))
            SkeletonBox(modifier = Modifier.fillMaxWidth().height(4.dp), shape = Radius.pill)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                SkeletonBox(modifier = Modifier.width(90.dp).height(20.dp))
                SkeletonBox(modifier = Modifier.width(70.dp).height(24.dp), shape = Radius.xs)
            }
        }
    }
}
