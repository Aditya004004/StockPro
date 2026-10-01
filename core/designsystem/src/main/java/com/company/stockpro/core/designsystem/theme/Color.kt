package com.company.stockpro.core.designsystem.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

object StockProColors {
    val Primary = Color(0xFF2563EB)
    val PrimaryPressed = Color(0xFF1E40AF)
    val PrimaryContainer = Color(0xFFEFF6FF)

    val BrandMark = Color(0xFF065F46)
    val Accent = Color(0xFF10B981)
    val AccentContainer = Color(0xFFDCFCE7)

    val Success = Color(0xFF16A34A)
    val SuccessContainer = Color(0xFFDCFCE7)
    val OnSuccessContainer = Color(0xFF065F46)

    val Warning = Color(0xFFF59E0B)
    val WarningContainer = Color(0xFFFEF3C7)
    val OnWarningContainer = Color(0xFF92400E)

    val Danger = Color(0xFFEF4444)
    val DangerContainer = Color(0xFFFEE2E2)
    val OnDangerContainer = Color(0xFF991B1B)

    // Reserved is Teal to decouple from action Primary Blue
    val Reserved = Color(0xFF0F766E)
    val ReservedContainer = Color(0xFFCFFAFE)
    val OnReservedContainer = Color(0xFF155E75)

    val Production = Color(0xFF9333EA)
    val ProductionContainer = Color(0xFFF3E8FF)
    val OnProductionContainer = Color(0xFF5B21B6)

    val Canvas = Color(0xFFF8FAFC)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceAlt = Color(0xFFF9FAFB)
    val SurfaceNav = Color(0xFFFFFFFF)

    val Outline = Color(0xFFE2E8F0)
    val OutlineSubtle = Color(0xFFF1F5F9)

    val TextPrimary = Color(0xFF0F172A)
    val TextSecondary = Color(0xFF64748B)
    val TextTertiary = Color(0xFF94A3B8)
    val TextDisabled = Color(0xFFCBD5E1)
}

data class StockProStatusColors(
    val inStock: Color = StockProColors.Success,
    val inStockContainer: Color = StockProColors.SuccessContainer,
    val lowStock: Color = StockProColors.Warning,
    val lowStockContainer: Color = StockProColors.WarningContainer,
    val outOfStock: Color = StockProColors.Danger,
    val outOfStockContainer: Color = StockProColors.DangerContainer,
    val reserved: Color = StockProColors.Reserved,
    val reservedContainer: Color = StockProColors.ReservedContainer,
    val inProduction: Color = StockProColors.Production,
    val inProductionContainer: Color = StockProColors.ProductionContainer
)

val LocalStockProStatusColors = staticCompositionLocalOf { StockProStatusColors() }
