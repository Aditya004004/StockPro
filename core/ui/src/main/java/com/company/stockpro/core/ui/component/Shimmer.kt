package com.company.stockpro.core.ui.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.company.stockpro.core.designsystem.theme.Radius

/**
 * 1200ms shimmer effect for loading skeletons per rules.md §6.3.
 */
@Composable
fun Modifier.shimmerEffect(): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_translation",
    )

    val shimmerColors = listOf(
        Color(0xFFE5E7EB).copy(alpha = 0.6f),
        Color(0xFFF3F4F6).copy(alpha = 0.9f),
        Color(0xFFE5E7EB).copy(alpha = 0.6f),
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value),
    )

    return this.background(brush)
}

/**
 * Skeleton placeholder block for structural content loading.
 */
@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    height: Dp = 20.dp,
    shape: Shape = Radius.sm,
) {
    Box(
        modifier = modifier
            .height(height)
            .clip(shape)
            .shimmerEffect(),
    )
}
