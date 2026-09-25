package com.company.stockpro.core.ui.component.chart

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.company.stockpro.core.designsystem.theme.Spacing
import com.company.stockpro.core.designsystem.theme.StockProColors

/**
 * Area Sparkline chart per docs/design.md §5.10:
 * - 2.5 dp stroke `success`
 * - Gradient fill 20% -> 0%
 * - Terminal dot 6 dp
 * - bodySmall axes / labels, no gridlines
 * - Exposes TalkBack semantics
 */
@Composable
fun AreaSparkline(
    dataPoints: List<Float>,
    modifier: Modifier = Modifier,
    labels: List<String> = emptyList(),
    lineColor: Color = StockProColors.Success,
    contentDescription: String = "Valuation trend over time",
) {
    if (dataPoints.isEmpty()) return

    var animationPlayed by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "sparkline_progress",
    )

    LaunchedEffect(dataPoints) {
        animationPlayed = true
    }

    val minVal = dataPoints.minOrNull() ?: 0f
    val maxVal = dataPoints.maxOrNull() ?: 1f
    val range = if (maxVal - minVal > 0.001f) maxVal - minVal else 1f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { this.contentDescription = contentDescription },
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
        ) {
            val width = size.width
            val height = size.height
            val topPadding = 8.dp.toPx()
            val bottomPadding = 4.dp.toPx()
            val usableHeight = height - topPadding - bottomPadding

            if (dataPoints.size == 1) {
                val y = topPadding + usableHeight / 2f
                drawLine(
                    color = lineColor,
                    start = Offset(0f, y),
                    end = Offset(width * progress, y),
                    strokeWidth = 2.5.dp.toPx(),
                )
                return@Canvas
            }

            val stepX = width / (dataPoints.size - 1)
            val points = dataPoints.mapIndexed { index, value ->
                val x = index * stepX
                val normalizedY = (value - minVal) / range
                val y = topPadding + (1f - normalizedY) * usableHeight
                Offset(x, y)
            }

            // Build stroke path
            val strokePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    val prev = points[i - 1]
                    val curr = points[i]
                    // Smooth cubic curve between points
                    val cpx1 = prev.x + (curr.x - prev.x) / 2f
                    val cpy1 = prev.y
                    val cpx2 = prev.x + (curr.x - prev.x) / 2f
                    val cpy2 = curr.y
                    cubicTo(cpx1, cpy1, cpx2, cpy2, curr.x, curr.y)
                }
            }

            // Build fill path
            val fillPath = Path().apply {
                addPath(strokePath)
                lineTo(points.last().x, height)
                lineTo(points.first().x, height)
                close()
            }

            // Fill area with gradient 20% -> 0%
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        lineColor.copy(alpha = 0.20f * progress),
                        lineColor.copy(alpha = 0.0f),
                    ),
                    startY = topPadding,
                    endY = height,
                ),
            )

            // Draw line stroke
            drawPath(
                path = strokePath,
                color = lineColor.copy(alpha = progress),
                style = Stroke(
                    width = 2.5.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )

            // Terminal dot (6 dp diameter = 3 dp radius)
            val lastPoint = points.last()
            drawCircle(
                color = StockProColors.Canvas,
                radius = 4.dp.toPx(),
                center = lastPoint,
            )
            drawCircle(
                color = lineColor.copy(alpha = progress),
                radius = 3.dp.toPx(),
                center = lastPoint,
            )
        }

        if (labels.isNotEmpty()) {
            Spacer(modifier = Modifier.height(Spacing.x1))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                labels.forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = StockProColors.TextSecondary,
                    )
                }
            }
        }
    }
}
