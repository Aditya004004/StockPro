package com.company.stockpro.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Immutable token palette for the active StockPro theme mode (Light or Dark).
 */
data class StockProPalette(
    val canvas: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val surfaceNav: Color,
    val outline: Color,
    val outlineSubtle: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textDisabled: Color,
    val primaryContainer: Color,
    val accentContainer: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val dangerContainer: Color,
    val onDangerContainer: Color,
    val reservedContainer: Color,
    val onReservedContainer: Color,
    val productionContainer: Color,
    val onProductionContainer: Color,
    val isDark: Boolean = false,
)

val LightStockProPalette = StockProPalette(
    canvas = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),
    surfaceAlt = Color(0xFFF9FAFB),
    surfaceNav = Color(0xFFFFFFFF),
    outline = Color(0xFFE2E8F0),
    outlineSubtle = Color(0xFFF1F5F9),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF64748B),
    textTertiary = Color(0xFF94A3B8),
    textDisabled = Color(0xFFCBD5E1),
    primaryContainer = Color(0xFFEFF6FF),
    accentContainer = Color(0xFFDCFCE7),
    successContainer = Color(0xFFDCFCE7),
    onSuccessContainer = Color(0xFF065F46),
    warningContainer = Color(0xFFFEF3C7),
    onWarningContainer = Color(0xFF92400E),
    dangerContainer = Color(0xFFFEE2E2),
    onDangerContainer = Color(0xFF991B1B),
    reservedContainer = Color(0xFFCFFAFE),
    onReservedContainer = Color(0xFF155E75),
    productionContainer = Color(0xFFF3E8FF),
    onProductionContainer = Color(0xFF5B21B6),
    isDark = false,
)

val DarkStockProPalette = StockProPalette(
    canvas = Color(0xFF0B1120),
    surface = Color(0xFF1E293B),
    surfaceAlt = Color(0xFF141F32),
    surfaceNav = Color(0xFF0F172A),
    outline = Color(0xFF334155),
    outlineSubtle = Color(0xFF1E293B),
    textPrimary = Color(0xFFF8FAFC),
    textSecondary = Color(0xFF94A3B8),
    textTertiary = Color(0xFF64748B),
    textDisabled = Color(0xFF475569),
    primaryContainer = Color(0xFF1E3A8A),
    accentContainer = Color(0xFF064E3B),
    successContainer = Color(0xFF064E3B),
    onSuccessContainer = Color(0xFFA7F3D0),
    warningContainer = Color(0xFF78350F),
    onWarningContainer = Color(0xFFFDE68A),
    dangerContainer = Color(0xFF7F1D1D),
    onDangerContainer = Color(0xFFFECACA),
    reservedContainer = Color(0xFF164E63),
    onReservedContainer = Color(0xFFA5F3FC),
    productionContainer = Color(0xFF581C87),
    onProductionContainer = Color(0xFFE9D5FF),
    isDark = true,
)

val LocalStockProPalette = staticCompositionLocalOf { LightStockProPalette }

object StockProColors {
    // Brand & Status constants (accessible in both Composable and non-Composable code like ViewModels)
    val Primary = Color(0xFF2563EB)
    val PrimaryPressed = Color(0xFF1E40AF)
    val BrandMark = Color(0xFF065F46)
    val Accent = Color(0xFF10B981)
    val Success = Color(0xFF16A34A)
    val Warning = Color(0xFFF59E0B)
    val Danger = Color(0xFFEF4444)
    val Reserved = Color(0xFF0F766E)
    val Production = Color(0xFF9333EA)

    val isDark: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalStockProPalette.current.isDark

    // Dynamic, theme-aware palette tokens (automatically adapt to light / dark mode)
    val Canvas: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalStockProPalette.current.canvas

    val Surface: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalStockProPalette.current.surface

    val SurfaceAlt: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalStockProPalette.current.surfaceAlt

    val SurfaceNav: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalStockProPalette.current.surfaceNav

    val Outline: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalStockProPalette.current.outline

    val OutlineSubtle: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalStockProPalette.current.outlineSubtle

    val TextPrimary: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalStockProPalette.current.textPrimary

    val TextSecondary: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalStockProPalette.current.textSecondary

    val TextTertiary: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalStockProPalette.current.textTertiary

    val TextDisabled: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalStockProPalette.current.textDisabled

    val PrimaryContainer: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalStockProPalette.current.primaryContainer

    val AccentContainer: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalStockProPalette.current.accentContainer

    val SuccessContainer: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalStockProPalette.current.successContainer

    val OnSuccessContainer: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalStockProPalette.current.onSuccessContainer

    val WarningContainer: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalStockProPalette.current.warningContainer

    val OnWarningContainer: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalStockProPalette.current.onWarningContainer

    val DangerContainer: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalStockProPalette.current.dangerContainer

    val OnDangerContainer: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalStockProPalette.current.onDangerContainer

    val ReservedContainer: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalStockProPalette.current.reservedContainer

    val OnReservedContainer: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalStockProPalette.current.onReservedContainer

    val ProductionContainer: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalStockProPalette.current.productionContainer

    val OnProductionContainer: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalStockProPalette.current.onProductionContainer
}

data class StockProStatusColors(
    val inStock: Color = StockProColors.Success,
    val inStockContainer: Color = Color(0xFFDCFCE7),
    val lowStock: Color = StockProColors.Warning,
    val lowStockContainer: Color = Color(0xFFFEF3C7),
    val outOfStock: Color = StockProColors.Danger,
    val outOfStockContainer: Color = Color(0xFFFEE2E2),
    val reserved: Color = StockProColors.Reserved,
    val reservedContainer: Color = Color(0xFFCFFAFE),
    val inProduction: Color = StockProColors.Production,
    val inProductionContainer: Color = Color(0xFFF3E8FF),
)

val LocalStockProStatusColors = staticCompositionLocalOf { StockProStatusColors() }
