package com.company.stockpro.core.ui.component.chart

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.stockpro.core.designsystem.theme.Radius
import com.company.stockpro.core.designsystem.theme.Spacing
import com.company.stockpro.core.designsystem.theme.StockProCodeTextStyle
import com.company.stockpro.core.designsystem.theme.StockProColors

data class StockSegment(
    val label: String,
    val count: Long,
    val color: Color,
)

/**
 * Donut chart variant matching production screenshot (media_1789794856417.png).
 * Displays centered total items numeral with ring segments and companion legend.
 */
@Composable
fun StockCompositionDonut(
    segments: List<StockSegment>,
    modifier: Modifier = Modifier,
    totalCount: Long = segments.sumOf { it.count },
    centerLabel: String = "Items",
) {
    var animationPlayed by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "donut_progress",
    )

    LaunchedEffect(segments) {
        animationPlayed = true
    }

    val total = if (totalCount > 0) totalCount.toFloat() else 1f
    val accessibilitySummary = segments.joinToString(", ") { "${it.label}: ${it.count}" }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .semantics { this.contentDescription = "Stock breakdown: $accessibilitySummary" },
    ) {
        // Donut ring with centered text
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(110.dp),
        ) {
            Canvas(modifier = Modifier.size(100.dp)) {
                val strokeWidth = 12.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2f
                val arcSize = Size(radius * 2, radius * 2)
                val topLeft = Offset(
                    (size.width - radius * 2) / 2f,
                    (size.height - radius * 2) / 2f,
                )

                var startAngle = -90f
                segments.forEach { segment ->
                    val sweepAngle = (segment.count / total) * 360f * progress
                    if (sweepAngle > 0f) {
                        drawArc(
                            color = segment.color,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle - 2f, // 2-degree visual gap
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        )
                    }
                    startAngle += sweepAngle
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format("%,d", totalCount),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = StockProColors.TextPrimary,
                )
                Text(
                    text = centerLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = StockProColors.TextSecondary,
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.x2))

        // Legend list
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.x1),
            modifier = Modifier.fillMaxWidth(),
        ) {
            segments.forEach { segment ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(segment.color),
                        )
                        Spacer(modifier = Modifier.width(Spacing.x1))
                        Text(
                            text = segment.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = StockProColors.TextSecondary,
                        )
                    }
                    Text(
                        text = String.format("%,d", segment.count),
                        style = StockProCodeTextStyle.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                        ),
                        color = StockProColors.TextPrimary,
                    )
                }
            }
        }
    }
}

/**
 * Horizontal Stacked Bar per docs/design.md §5.10:
 * - Height 32 dp, radius `xs`, 2 dp segment gaps
 * - Proportional bar segments with direct legend listing quantity and percentage share
 */
@Composable
fun StockCompositionBar(
    segments: List<StockSegment>,
    modifier: Modifier = Modifier,
    totalCount: Long = segments.sumOf { it.count },
) {
    var animationPlayed by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "bar_progress",
    )

    LaunchedEffect(segments) {
        animationPlayed = true
    }

    val total = if (totalCount > 0) totalCount.toFloat() else 1f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                this.contentDescription = "Stock composition bar: ${segments.joinToString { "${it.label}: ${it.count}" }}"
            },
    ) {
        // Stacked horizontal bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .clip(Radius.xs)
                .background(StockProColors.SurfaceAlt),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            segments.forEach { segment ->
                val weight = ((segment.count / total) * progress).coerceAtLeast(0.001f)
                Box(
                    modifier = Modifier
                        .weight(weight)
                        .height(28.dp)
                        .background(segment.color),
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.x2))

        // Legend below
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.x1)) {
            segments.forEach { segment ->
                val percent = if (total > 0) (segment.count / total * 100f).toInt() else 0
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(segment.color),
                        )
                        Spacer(modifier = Modifier.width(Spacing.x1))
                        Text(
                            text = segment.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = StockProColors.TextSecondary,
                        )
                    }
                    Text(
                        text = "${String.format("%,d", segment.count)} ($percent%)",
                        style = StockProCodeTextStyle.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                        ),
                        color = StockProColors.TextPrimary,
                    )
                }
            }
        }
    }
}
