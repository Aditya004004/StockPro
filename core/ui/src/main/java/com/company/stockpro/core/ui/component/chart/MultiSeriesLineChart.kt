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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.stockpro.core.designsystem.theme.Spacing
import com.company.stockpro.core.designsystem.theme.StockProColors

data class ChartSeries(
    val name: String,
    val color: Color,
    val dataPoints: List<Float>,
)

/**
 * MultiSeriesLineChart per docs/design.md §5.10 & screenshot media_1789794856398.png:
 * - 2 dp strokes
 * - 4 dp markers with white fill
 * - 1 dp outlineSubtle gridlines
 * - Legend below
 * - TalkBack semantics
 */
@Composable
fun MultiSeriesLineChart(
    seriesList: List<ChartSeries>,
    xLabels: List<String>,
    modifier: Modifier = Modifier,
    ySteps: List<String> = listOf("1,500", "1,000", "500", "0"),
    maxValue: Float = 1500f,
    contentDescription: String = "Monthly trend comparison chart",
) {
    if (seriesList.isEmpty() || xLabels.isEmpty()) return

    var animationPlayed by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "line_chart_progress",
    )

    LaunchedEffect(seriesList) {
        animationPlayed = true
    }

    val gridColor = StockProColors.OutlineSubtle
    val dotBgColor = StockProColors.Surface

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { this.contentDescription = contentDescription },
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Y-Axis labels
            Column(
                modifier = Modifier
                    .height(140.dp)
                    .padding(end = Spacing.x1),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
            ) {
                ySteps.forEach { step ->
                    Text(
                        text = step,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = StockProColors.TextSecondary,
                    )
                }
            }

            // Chart Canvas with Gridlines
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(140.dp),
            ) {
                val width = size.width
                val height = size.height

                // 1 dp outlineSubtle gridlines
                val gridlineCount = ySteps.size
                for (i in 0 until gridlineCount) {
                    val y = (height / (gridlineCount - 1)) * i
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx(),
                    )
                }

                // Draw each series
                val stepX = if (xLabels.size > 1) width / (xLabels.size - 1) else width
                seriesList.forEach { series ->
                    if (series.dataPoints.isEmpty()) return@forEach

                    val points = series.dataPoints.mapIndexed { index, value ->
                        val x = index * stepX
                        val normalizedY = (value / maxValue).coerceIn(0f, 1f)
                        val y = (1f - normalizedY) * height
                        Offset(x, y)
                    }

                    // Stroke path
                    val path = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            val prev = points[i - 1]
                            val curr = points[i]
                            val cpx1 = prev.x + (curr.x - prev.x) / 2f
                            val cpy1 = prev.y
                            val cpx2 = prev.x + (curr.x - prev.x) / 2f
                            val cpy2 = curr.y
                            cubicTo(cpx1, cpy1, cpx2, cpy2, curr.x, curr.y)
                        }
                    }

                    // Draw 2 dp stroke
                    drawPath(
                        path = path,
                        color = series.color.copy(alpha = progress),
                        style = Stroke(
                            width = 2.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                    )

                    // Draw 4 dp markers with white fill
                    points.forEach { pt ->
                        drawCircle(
                            color = series.color.copy(alpha = progress),
                            radius = 4.dp.toPx(),
                            center = pt,
                        )
                        drawCircle(
                            color = dotBgColor,
                            radius = 2.dp.toPx(),
                            center = pt,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.x1))

        // X-Axis labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 36.dp), // Align offset with canvas start
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            xLabels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = StockProColors.TextSecondary,
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.x2))

        // Legend below
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            seriesList.forEachIndexed { index, series ->
                if (index > 0) Spacer(modifier = Modifier.width(Spacing.x3))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(series.color),
                    )
                    Spacer(modifier = Modifier.width(Spacing.x1))
                    Text(
                        text = series.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = StockProColors.TextSecondary,
                    )
                }
            }
        }
    }
}
