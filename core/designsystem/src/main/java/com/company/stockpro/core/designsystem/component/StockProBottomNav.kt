package com.company.stockpro.core.designsystem.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.company.stockpro.core.designsystem.theme.StockProColors

enum class StockProTopLevelDestination(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
    INVENTORY("Inventory", Icons.Filled.ListAlt, Icons.Outlined.ListAlt),
    REPORTS("Reports", Icons.Filled.BarChart, Icons.Outlined.BarChart),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

@Composable
fun StockProBottomNav(
    currentDestination: StockProTopLevelDestination,
    onNavigateToDestination: (StockProTopLevelDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = StockProColors.SurfaceNav
    ) {
        StockProTopLevelDestination.entries.forEach { destination ->
            val isSelected = destination == currentDestination
            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigateToDestination(destination) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                        contentDescription = destination.label
                    )
                },
                label = { Text(text = destination.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = StockProColors.Accent,
                    selectedTextColor = StockProColors.Accent,
                    indicatorColor = StockProColors.AccentContainer,
                    unselectedIconColor = StockProColors.TextSecondary,
                    unselectedTextColor = StockProColors.TextSecondary
                )
            )
        }
    }
}
