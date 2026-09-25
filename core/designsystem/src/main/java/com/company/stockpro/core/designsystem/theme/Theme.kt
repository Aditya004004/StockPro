package com.company.stockpro.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val LightColorScheme = lightColorScheme(
    primary = StockProColors.Primary,
    onPrimary = StockProColors.Surface,
    primaryContainer = StockProColors.PrimaryContainer,
    background = StockProColors.Canvas,
    surface = StockProColors.Surface,
    surfaceVariant = StockProColors.SurfaceAlt,
    outline = StockProColors.Outline,
    onBackground = StockProColors.TextPrimary,
    onSurface = StockProColors.TextPrimary,
    onSurfaceVariant = StockProColors.TextSecondary
)

private val DarkColorScheme = darkColorScheme(
    primary = StockProColors.Primary,
    onPrimary = StockProColors.Surface,
    primaryContainer = StockProColors.PrimaryContainer,
    background = StockProColors.Canvas,
    surface = StockProColors.Surface,
    outline = StockProColors.Outline
)

@Composable
fun StockProTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val statusColors = StockProStatusColors()

    CompositionLocalProvider(
        LocalStockProStatusColors provides statusColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
