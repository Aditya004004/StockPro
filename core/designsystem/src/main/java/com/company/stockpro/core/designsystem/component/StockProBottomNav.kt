package com.company.stockpro.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.stockpro.core.designsystem.theme.Radius
import com.company.stockpro.core.designsystem.theme.StockProColors

enum class StockProTopLevelDestination(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
    INVENTORY("Inventory", Icons.AutoMirrored.Filled.ListAlt, Icons.AutoMirrored.Outlined.ListAlt),
    REPORTS("Reports", Icons.Filled.BarChart, Icons.Outlined.BarChart),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

@Composable
fun StockProBottomNav(
    currentDestination: StockProTopLevelDestination,
    onNavigateToDestination: (StockProTopLevelDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = StockProColors.SurfaceNav,
        modifier = modifier
            .fillMaxWidth()
            .border(width = 0.5.dp, color = StockProColors.OutlineSubtle),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(68.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StockProTopLevelDestination.entries.forEach { destination ->
                val isSelected = destination == currentDestination
                val interactionSource = remember { MutableInteractionSource() }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { onNavigateToDestination(destination) },
                        )
                        .padding(vertical = 4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .clip(Radius.pill)
                            .background(
                                if (isSelected) StockProColors.SuccessContainer else Color.Transparent
                            )
                            .padding(horizontal = 18.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                            contentDescription = destination.label,
                            tint = if (isSelected) StockProColors.Success else StockProColors.TextSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    val destinationLabel = when (destination) {
                        StockProTopLevelDestination.HOME -> com.company.stockpro.core.designsystem.theme.StockProStrings.current.navHome
                        StockProTopLevelDestination.INVENTORY -> com.company.stockpro.core.designsystem.theme.StockProStrings.current.navInventory
                        StockProTopLevelDestination.REPORTS -> com.company.stockpro.core.designsystem.theme.StockProStrings.current.navReports
                        StockProTopLevelDestination.SETTINGS -> com.company.stockpro.core.designsystem.theme.StockProStrings.current.navSettings
                    }

                    Text(
                        text = destinationLabel,
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) StockProColors.Success else StockProColors.TextSecondary,
                        ),
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(StockProColors.Success),
                        )
                    } else {
                        Spacer(modifier = Modifier.size(4.dp))
                    }
                }
            }
        }
    }
}

