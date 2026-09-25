package com.company.stockpro.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.company.stockpro.core.designsystem.theme.Radius
import com.company.stockpro.core.designsystem.theme.Spacing
import com.company.stockpro.core.designsystem.theme.StockProColors
import com.company.stockpro.core.domain.model.StockLevel

@Composable
fun StatusBadge(
    level: StockLevel,
    modifier: Modifier = Modifier
) {
    val (containerColor, textColor, textLabel) = when (level) {
        StockLevel.IN_STOCK -> Triple(StockProColors.SuccessContainer, StockProColors.OnSuccessContainer, "In Stock")
        StockLevel.LOW_STOCK -> Triple(StockProColors.WarningContainer, StockProColors.OnWarningContainer, "Low Stock")
        StockLevel.OUT_OF_STOCK -> Triple(StockProColors.DangerContainer, StockProColors.OnDangerContainer, "Out of Stock")
        StockLevel.RESERVED -> Triple(StockProColors.ReservedContainer, StockProColors.OnReservedContainer, "Reserved")
        StockLevel.IN_PRODUCTION -> Triple(StockProColors.ProductionContainer, StockProColors.OnProductionContainer, "In Production")
    }

    Row(
        modifier = modifier
            .clip(Radius.pill)
            .background(containerColor)
            .padding(horizontal = Spacing.x2, vertical = Spacing.x1),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(textColor)
        )
        Spacer(modifier = Modifier.width(Spacing.x1))
        Text(
            text = textLabel,
            style = MaterialTheme.typography.labelMedium,
            color = textColor
        )
    }
}
