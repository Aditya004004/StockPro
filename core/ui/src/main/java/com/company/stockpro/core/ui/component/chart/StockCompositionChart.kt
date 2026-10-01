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
import androidx.compose.ui.text.style.TextAlign
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
 * Donut chart variant matching production screenshot (media_1790410459925.png).
 * Displays donut ring on the left with centered total items numeral and Active SKUs label,
 * with companion legend on the right showing colored dots, labels, bold counts, and percentages.
 */
@Composable
fun StockCompositionDonut(
    segments: List<StockSegment>,
    modifier: Modifier = Modifier,
    totalCount: Long = segments.sumOf { it.count },
    centerLabel: String = "Items",
    isSideBySide: Boolean = true,
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
    val hasNonZeroData = segments.any { it.count > 0 }

    val donutGraphic = @Composable {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(120.dp),
        ) {
            Canvas(modifier = Modifier.size(112.dp)) {
                val strokeWidth = 14.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2f
                val arcSize = Size(radius * 2, radius * 2)
                val topLeft = Offset(
                    (size.width - radius * 2) / 2f,
                    (size.height - radius * 2) / 2f,
                )

                if (hasNonZeroData) {
                    var startAngle = -90f
                    segments.forEach { segment ->
                        val sweepAngle = (segment.count / total) * 360f * progress
                        if (sweepAngle > 0f) {
                            drawArc(
                                color = segment.color,
                                startAngle = startAngle,
                                sweepAngle = (sweepAngle - 3f).coerceAtLeast(1f),
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                            )
                        }
                        startAngle += sweepAngle
                    }
                } else {
                    // Preview ring matching screenshot: Blue, Green, Orange, Red quadrants
                    val previewColors = listOf(
                        Color(0xFF2563EB), // Blue
                        Color(0xFF16A34A), // Green
                        Color(0xFFF59E0B), // Orange
                        Color(0xFFEF4444), // Red
                    )
                    val arcSweep = 90f * progress
                    previewColors.forEachIndexed { index, color ->
                        val startAngle = -180f + (index * 90f)
                        if (arcSweep > 0f) {
                            drawArc(
                                color = color,
                                startAngle = startAngle,
                                sweepAngle = (arcSweep - 4f).coerceAtLeast(1f),
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                            )
                        }
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format("%,d", totalCount),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                    ),
                    color = StockProColors.TextPrimary,
                )
                Text(
                    text = centerLabel,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = StockProColors.TextSecondary,
                )
            }
        }
    }

    val legendContent = @Composable {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            segments.forEach { segment ->
                val percent = if (totalCount > 0) ((segment.count * 100f) / totalCount).toInt() else 0
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(segment.color),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = segment.label,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = StockProColors.TextSecondary,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = String.format("%,d", segment.count),
                        style = StockProCodeTextStyle.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                        ),
                        color = StockProColors.TextPrimary,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "$percent%",
                        style = StockProCodeTextStyle.copy(
                            fontSize = 11.sp,
                            color = StockProColors.TextTertiary,
                        ),
                        modifier = Modifier.width(28.dp),
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
    }

    if (isSideBySide) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .fillMaxWidth()
                .semantics { this.contentDescription = "Stock breakdown: $accessibilitySummary" },
        ) {
            donutGraphic()
            Spacer(modifier = Modifier.width(16.dp))
            Box(modifier = Modifier.weight(1f)) {
                legendContent()
            }
        }
    } else {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .fillMaxWidth()
                .semantics { this.contentDescription = "Stock breakdown: $accessibilitySummary" },
        ) {
            donutGraphic()
            Spacer(modifier = Modifier.height(Spacing.x2))
            legendContent()
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
