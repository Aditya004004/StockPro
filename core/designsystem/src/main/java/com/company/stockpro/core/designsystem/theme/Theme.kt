package com.company.stockpro.core.designsystem.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2563EB),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEFF6FF),
    onPrimaryContainer = Color(0xFF1E40AF),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF9FAFB),
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFFE2E8F0),
    outlineVariant = Color(0xFFF1F5F9),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF3B82F6),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    background = Color(0xFF0B1120),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF1E293B),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF141F32),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155),
    outlineVariant = Color(0xFF1E293B),
)

val DarkStockProStatusColors = StockProStatusColors(
    inStock = Color(0xFF22C55E),
    inStockContainer = Color(0xFF064E3B),
    lowStock = Color(0xFFFBBF24),
    lowStockContainer = Color(0xFF78350F),
    outOfStock = Color(0xFFF87171),
    outOfStockContainer = Color(0xFF7F1D1D),
    reserved = Color(0xFF14B8A6),
    reservedContainer = Color(0xFF164E63),
    inProduction = Color(0xFFA855F7),
    inProductionContainer = Color(0xFF581C87),
)

val LightStockProStatusColors = StockProStatusColors(
    inStock = Color(0xFF16A34A),
    inStockContainer = Color(0xFFDCFCE7),
    lowStock = Color(0xFFF59E0B),
    lowStockContainer = Color(0xFFFEF3C7),
    outOfStock = Color(0xFFEF4444),
    outOfStockContainer = Color(0xFFFEE2E2),
    reserved = Color(0xFF0F766E),
    reservedContainer = Color(0xFFCFFAFE),
    inProduction = Color(0xFF9333EA),
    inProductionContainer = Color(0xFFF3E8FF),
)

@Composable
fun StockProTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    language: com.company.stockpro.core.domain.model.AppLanguage = com.company.stockpro.core.domain.model.AppLanguage.ENGLISH,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val palette = if (darkTheme) DarkStockProPalette else LightStockProPalette
    val statusColors = if (darkTheme) DarkStockProStatusColors else LightStockProStatusColors
    val strings = StockProStrings.from(language)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalStockProPalette provides palette,
        LocalStockProStatusColors provides statusColors,
        LocalStockProStrings provides strings,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}
