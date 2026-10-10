package com.company.stockpro.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Factory
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.company.stockpro.core.designsystem.component.ButtonVariant
import com.company.stockpro.core.designsystem.component.StatusBadge
import com.company.stockpro.core.designsystem.component.StockProButton
import com.company.stockpro.core.designsystem.component.StockProTextField
import com.company.stockpro.core.designsystem.theme.Radius
import com.company.stockpro.core.designsystem.theme.Spacing
import com.company.stockpro.core.designsystem.theme.StockProCodeTextStyle
import com.company.stockpro.core.designsystem.theme.StockProColors
import com.company.stockpro.core.designsystem.theme.StockProStrings
import com.company.stockpro.core.domain.model.Currency
import com.company.stockpro.core.domain.model.Item
import com.company.stockpro.core.domain.model.Money
import com.company.stockpro.core.domain.model.MovementType
import com.company.stockpro.core.domain.model.StockLevel
import com.company.stockpro.core.domain.model.SyncStatus
import com.company.stockpro.core.ui.component.SkeletonBox
import com.company.stockpro.core.ui.component.chart.StockCompositionDonut

/**
 * Modern High-Precision StockPro Home Screen.
 * Faithful implementation of the screenshot UI with identical colors, text styles, and layout.
 */
@Composable
fun HomeRoute(
    viewModel: HomeViewModel,
    onNavigateToInventory: () -> Unit,
    onNavigateToReports: () -> Unit,
    onReceiveStockClick: () -> Unit,
    onIssueStockClick: () -> Unit,
    onItemClick: (String) -> Unit,
    onNotificationsClick: () -> Unit,
    onNeedsAttentionClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreen(
        uiState = uiState,
        onToggleValuation = { viewModel.toggleValuationVisibility() },
        onSelectPlant = { viewModel.selectPlant(it) },
        onAddPlant = { viewModel.addPlant(it) },
        onAddPlantFull = { name, code, location, onSuccess, onError ->
            viewModel.addPlant(name, code, location, onSuccess, onError)
        },
        onSelectCurrency = { viewModel.selectCurrency(it) },
        onNavigateToInventory = onNavigateToInventory,
        onNavigateToReports = onNavigateToReports,
        onReceiveStockClick = onReceiveStockClick,
        onIssueStockClick = onIssueStockClick,
        onItemClick = onItemClick,
        onNotificationsClick = onNotificationsClick,
        onNeedsAttentionClick = onNeedsAttentionClick,
        onRetryClick = { /* Flow in ViewModel is reactive; re-evaluates automatically */ },
        modifier = modifier,
    )
}

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onToggleValuation: () -> Unit,
    onSelectPlant: (String) -> Unit,
    onAddPlant: (String) -> Unit = {},
    onAddPlantFull: (name: String, code: String, location: String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit = { _, _, _, _, _ -> },
    onSelectCurrency: (Currency) -> Unit = {},
    onNavigateToInventory: () -> Unit,
    onNavigateToReports: () -> Unit,
    onReceiveStockClick: () -> Unit,
    onIssueStockClick: () -> Unit,
    onItemClick: (String) -> Unit,
    onNotificationsClick: () -> Unit,
    onNeedsAttentionClick: () -> Unit = {},
    onRetryClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StockProColors.Canvas),
    ) {
        when (uiState) {
            is HomeUiState.Loading -> {
                HomeLoadingSkeleton()
            }
            is HomeUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Spacing.screenGutter),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = StockProColors.Danger,
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(modifier = Modifier.height(Spacing.x2))
                    Text(
                        text = "Unable to load dashboard",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = StockProColors.TextPrimary,
                    )
                    Spacer(modifier = Modifier.height(Spacing.x1))
                    Text(
                        text = uiState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = StockProColors.TextSecondary,
                    )
                    Spacer(modifier = Modifier.height(Spacing.x4))
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.x2)) {
                        StockProButton(
                            text = "Retry",
                            onClick = onRetryClick,
                            variant = ButtonVariant.PRIMARY,
                        )
                        StockProButton(
                            text = "Go to Inventory",
                            onClick = onNavigateToInventory,
                            variant = ButtonVariant.GHOST,
                        )
                    }
                }
            }
            is HomeUiState.Content -> {
                HomeContent(
                    state = uiState,
                    onToggleValuation = onToggleValuation,
                    onSelectPlant = onSelectPlant,
                    onAddPlant = onAddPlant,
                    onAddPlantFull = onAddPlantFull,
                    onSelectCurrency = onSelectCurrency,
                    onNavigateToInventory = onNavigateToInventory,
                    onNavigateToReports = onNavigateToReports,
                    onReceiveStockClick = onReceiveStockClick,
                    onIssueStockClick = onIssueStockClick,
                    onItemClick = onItemClick,
                    onNotificationsClick = onNotificationsClick,
                    onNeedsAttentionClick = onNeedsAttentionClick,
                )
            }
        }
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState.Content,
    onToggleValuation: () -> Unit,
    onSelectPlant: (String) -> Unit,
    onAddPlant: (String) -> Unit,
    onAddPlantFull: (name: String, code: String, location: String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit = { _, _, _, _, _ -> },
    onSelectCurrency: (Currency) -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToReports: () -> Unit,
    onReceiveStockClick: () -> Unit,
    onIssueStockClick: () -> Unit,
    onItemClick: (String) -> Unit,
    onNotificationsClick: () -> Unit,
    onNeedsAttentionClick: () -> Unit,
) {
    val scrollState = rememberScrollState()
    val strings = StockProStrings.current
    var plantDropdownExpanded by rememberSaveable { mutableStateOf(false) }
    var currencyDropdownExpanded by rememberSaveable { mutableStateOf(false) }
    var showAddPlantDialog by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 96.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = Spacing.screenGutter)) {
            Spacer(modifier = Modifier.height(12.dp))

            // 1. App Header: Factory Icon, Active Plant Title + Dropdown, Warehouse Name, Notifications, Avatar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false),
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(StockProColors.Primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Factory,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        // Active Plant Name as Primary Header Title with Instant Dropdown
                        Box {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { plantDropdownExpanded = true }
                                    .padding(vertical = 1.dp),
                            ) {
                                Text(
                                    text = state.activePlant.ifBlank { strings.selectPlant },
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 19.sp,
                                        letterSpacing = (-0.3).sp,
                                    ),
                                    color = StockProColors.TextPrimary,
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = strings.selectPlant,
                                    tint = StockProColors.TextPrimary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }

                            DropdownMenu(
                                expanded = plantDropdownExpanded,
                                onDismissRequest = { plantDropdownExpanded = false },
                            ) {
                                if (state.availablePlants.isEmpty()) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "No plants configured",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = StockProColors.TextSecondary,
                                            )
                                        },
                                        enabled = false,
                                        onClick = {},
                                    )
                                } else {
                                    state.availablePlants.forEach { plant ->
                                        val isSelected = plant.equals(state.activePlant, ignoreCase = true)
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = plant,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) StockProColors.Primary else StockProColors.TextPrimary,
                                                )
                                            },
                                            trailingIcon = if (isSelected) {
                                                {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Selected",
                                                        tint = StockProColors.Primary,
                                                        modifier = Modifier.size(16.dp),
                                                    )
                                                }
                                            } else null,
                                            onClick = {
                                                plantDropdownExpanded = false
                                                onSelectPlant(plant)
                                            },
                                        )
                                    }
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = strings.addNewPlant,
                                            color = StockProColors.Primary,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = strings.addPlant,
                                            tint = StockProColors.Primary,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    },
                                    onClick = {
                                        plantDropdownExpanded = false
                                        showAddPlantDialog = true
                                    },
                                )
                            }
                        }

                        // Dynamic Warehouse Name from Configuration
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 1.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                tint = StockProColors.TextSecondary,
                                modifier = Modifier.size(13.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = state.activeWarehouse.ifBlank { "Main Warehouse" },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = StockProColors.TextSecondary,
                                ),
                            )
                        }
                    }
                }

                if (showAddPlantDialog) {
                    var newPlantName by rememberSaveable { mutableStateOf("") }
                    var newPlantCode by rememberSaveable { mutableStateOf("") }
                    var newPlantLocation by rememberSaveable { mutableStateOf("") }
                    var isSaving by rememberSaveable { mutableStateOf(false) }
                    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

                    AlertDialog(
                        onDismissRequest = {
                            if (!isSaving) {
                                showAddPlantDialog = false
                            }
                        },
                        title = {
                            Text(
                                text = strings.addNewPlant,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = StockProColors.TextPrimary,
                            )
                        },
                        text = {
                            Column {
                                Text(
                                    text = "Register an enterprise facility or warehouse location:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = StockProColors.TextSecondary,
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                StockProTextField(
                                    value = newPlantName,
                                    onValueChange = {
                                        newPlantName = it
                                        if (errorMessage != null) errorMessage = null
                                    },
                                    label = "${strings.plantName} *",
                                    placeholder = "e.g. Chakan Warehouse B",
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                StockProTextField(
                                    value = newPlantCode,
                                    onValueChange = {
                                        newPlantCode = it.uppercase()
                                        if (errorMessage != null) errorMessage = null
                                    },
                                    label = "${strings.plantCode} *",
                                    placeholder = "e.g. CHK-02",
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                StockProTextField(
                                    value = newPlantLocation,
                                    onValueChange = {
                                        newPlantLocation = it
                                        if (errorMessage != null) errorMessage = null
                                    },
                                    label = "${strings.facilityLocation} *",
                                    placeholder = "e.g. Pune, Maharashtra",
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                if (errorMessage != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = errorMessage.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = StockProColors.Danger,
                                            fontWeight = FontWeight.Medium,
                                        ),
                                    )
                                }
                                if (isSaving) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    LinearProgressIndicator(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = StockProColors.Primary,
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            StockProButton(
                                text = strings.addPlant,
                                onClick = {
                                    val trimmedName = newPlantName.trim()
                                    val trimmedCode = newPlantCode.trim()
                                    val trimmedLoc = newPlantLocation.trim()
                                    if (trimmedName.isBlank()) {
                                        errorMessage = "Plant name cannot be empty"
                                    } else if (trimmedCode.isBlank()) {
                                        errorMessage = "Plant code cannot be empty"
                                    } else if (trimmedLoc.isBlank()) {
                                        errorMessage = "Plant location cannot be empty"
                                    } else {
                                        isSaving = true
                                        errorMessage = null
                                        onAddPlantFull(
                                            trimmedName,
                                            trimmedCode,
                                            trimmedLoc,
                                            {
                                                isSaving = false
                                                showAddPlantDialog = false
                                            },
                                            { err ->
                                                isSaving = false
                                                errorMessage = err
                                            }
                                        )
                                    }
                                },
                                enabled = !isSaving,
                                isLoading = isSaving,
                                variant = ButtonVariant.PRIMARY,
                            )
                        },
                        dismissButton = {
                            StockProButton(
                                text = strings.cancel,
                                onClick = {
                                    if (!isSaving) {
                                        showAddPlantDialog = false
                                    }
                                },
                                enabled = !isSaving,
                                variant = ButtonVariant.GHOST,
                            )
                        },
                        containerColor = StockProColors.Surface,
                        shape = Radius.lg,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onNotificationsClick,
                        modifier = Modifier.size(40.dp),
                    ) {
                        BadgedBox(
                            badge = {
                                if (state.unreadAlertsCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(StockProColors.Danger),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = state.unreadAlertsCount.toString(),
                                            style = TextStyle(
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                            ),
                                        )
                                    }
                                }
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = StockProColors.TextPrimary,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // User Initials Avatar with Blue Border
                    val initials = remember(state.userName) {
                        state.userName.trim().split("\\s+".toRegex())
                            .filter { it.isNotEmpty() }
                            .take(1)
                            .map { it.first().uppercase() }
                            .firstOrNull() ?: "A"
                    }
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(StockProColors.PrimaryContainer)
                            .border(1.5.dp, StockProColors.Primary, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = initials,
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = StockProColors.Primary,
                            ),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. 2x2 Operational KPI Grid with Mini Bar Graphics
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    HomeKpiCard(
                        title = strings.totalItems.uppercase(),
                        value = String.format("%,d", state.totalItems),
                        icon = Icons.Default.Inventory2,
                        iconColor = StockProColors.Primary,
                        iconBgColor = Color(0xFFEEF2FF),
                        barColor = Color(0xFFDBEAFE),
                        barHeights = listOf(0.40f, 0.85f, 0.30f, 0.65f),
                        deltaText = "${state.totalItemsDeltaPercent}%",
                        subText = "this month",
                        isPill = true,
                        modifier = Modifier.weight(1f),
                    )
                    HomeKpiCard(
                        title = strings.lowStock.uppercase(),
                        value = "${state.lowStockCount}",
                        icon = Icons.Default.Warning,
                        iconColor = StockProColors.Warning,
                        iconBgColor = Color(0xFFFEF3C7),
                        barColor = Color(0xFFFED7AA),
                        barHeights = listOf(0.65f, 0.40f, 0.90f, 0.50f),
                        deltaText = "${state.lowStockItemsNeedReorder} Critical",
                        subText = "need reorder",
                        isPill = false,
                        warningText = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    HomeKpiCard(
                        title = strings.inProduction.uppercase(),
                        value = String.format("%,d", state.finishedGoodsCount),
                        icon = Icons.Default.Settings,
                        iconColor = StockProColors.Production,
                        iconBgColor = Color(0xFFF3E8FF),
                        barColor = Color(0xFFDDD6FE),
                        barHeights = listOf(0.35f, 0.60f, 0.90f, 0.50f),
                        deltaText = "${state.finishedGoodsDeltaPercent}%",
                        subText = "active runs",
                        isPill = true,
                        modifier = Modifier.weight(1f),
                    )
                    HomeKpiCard(
                        title = strings.rawMaterials.uppercase(),
                        value = String.format("%,d", state.rawMaterialsCount),
                        icon = Icons.Default.Storefront,
                        iconColor = StockProColors.Success,
                        iconBgColor = Color(0xFFDCFCE7),
                        barColor = Color(0xFFBBF7D0),
                        barHeights = listOf(0.40f, 0.70f, 0.90f, 0.50f),
                        deltaText = "${state.rawMaterialsDeltaPercent}%",
                        subText = "in warehouse",
                        isPill = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Hero Inventory Valuation Card with Area Sparkline & Trend
            Surface(
                shape = Radius.lg,
                color = StockProColors.Surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StockProColors.OutlineSubtle, Radius.lg),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(StockProColors.SuccessContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = state.currency.symbol,
                                    style = TextStyle(
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StockProColors.Success,
                                    ),
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = strings.totalValuation.uppercase(),
                                        style = StockProCodeTextStyle.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = StockProColors.TextSecondary,
                                            letterSpacing = 0.3.sp,
                                        ),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box {
                                        Surface(
                                            shape = Radius.xs,
                                            color = StockProColors.SuccessContainer,
                                            modifier = Modifier
                                                .clip(Radius.xs)
                                                .clickable { currencyDropdownExpanded = true },
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            ) {
                                                Text(
                                                    text = "${state.currency.code}  ${state.currency.symbol}",
                                                    style = StockProCodeTextStyle.copy(
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = StockProColors.Success,
                                                    ),
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowDown,
                                                    contentDescription = "Select Currency",
                                                    tint = StockProColors.Success,
                                                    modifier = Modifier.size(13.dp),
                                                )
                                            }
                                        }

                                        DropdownMenu(
                                            expanded = currencyDropdownExpanded,
                                            onDismissRequest = { currencyDropdownExpanded = false },
                                        ) {
                                            Currency.entries.forEach { curr ->
                                                val isSelected = curr == state.currency
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            text = "${curr.displayName} (${curr.symbol})",
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (isSelected) StockProColors.Primary else StockProColors.TextPrimary,
                                                        )
                                                    },
                                                    leadingIcon = {
                                                        Text(
                                                            text = curr.symbol,
                                                            style = StockProCodeTextStyle.copy(
                                                                fontSize = 14.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isSelected) StockProColors.Primary else StockProColors.TextSecondary,
                                                            ),
                                                        )
                                                    },
                                                    trailingIcon = if (isSelected) {
                                                        {
                                                            Icon(
                                                                imageVector = Icons.Default.Check,
                                                                contentDescription = "Selected",
                                                                tint = StockProColors.Primary,
                                                                modifier = Modifier.size(16.dp),
                                                            )
                                                        }
                                                    } else null,
                                                    onClick = {
                                                        currencyDropdownExpanded = false
                                                        onSelectCurrency(curr)
                                                    },
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                val formattedValuation = if (state.isValuationVisible) {
                                    state.currency.formatMoney(Money(state.totalValuationPaise))
                                } else {
                                    state.currency.maskedValue()
                                }

                                Text(
                                    text = formattedValuation,
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = (-0.5).sp,
                                    ),
                                    color = StockProColors.TextPrimary,
                                )
                            }
                        }

                        // Toggle visibility button
                        Surface(
                            shape = Radius.sm,
                            color = StockProColors.OutlineSubtle,
                            modifier = Modifier
                                .clip(Radius.sm)
                                .clickable(onClick = onToggleValuation),
                        ) {
                            Box(
                                modifier = Modifier.size(30.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = if (state.isValuationVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle valuation",
                                    tint = StockProColors.TextSecondary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 2.dp),
                        ) {
                            Surface(
                                shape = Radius.pill,
                                color = StockProColors.SuccessContainer,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                        contentDescription = null,
                                        tint = StockProColors.Success,
                                        modifier = Modifier.size(12.dp),
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "${state.valuationDeltaPercent}%",
                                        style = StockProCodeTextStyle.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = StockProColors.Success,
                                        ),
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "vs preceding 30 days",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = StockProColors.TextSecondary,
                            )
                        }

                        // Sparkline graphic in bottom right
                        ValuationSparklineGraphic(
                            modifier = Modifier
                                .width(135.dp)
                                .height(36.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Stock Health & Composition Card (Donut on left, Legend on right)
            Surface(
                shape = Radius.lg,
                color = StockProColors.Surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StockProColors.OutlineSubtle, Radius.lg),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = strings.stockComposition,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.2.sp,
                                ),
                                color = StockProColors.TextPrimary,
                            )
                            Text(
                                text = "Physical warehouse distribution",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                color = StockProColors.TextTertiary,
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(Radius.sm)
                                .clickable { onNavigateToInventory() }
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = strings.viewAll,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StockProColors.Primary,
                                ),
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = StockProColors.Primary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    StockCompositionDonut(
                        segments = state.stockSegments,
                        totalCount = state.totalItems,
                        centerLabel = "Active SKUs",
                        isSideBySide = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Floor Operations Action Grid (4 Buttons)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = strings.quickActions.uppercase(),
                    style = StockProCodeTextStyle.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = StockProColors.TextSecondary,
                        letterSpacing = 0.5.sp,
                    ),
                )
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.5.sp,
                        color = StockProColors.TextTertiary,
                    ),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FloorActionButton(
                    title = "Receive",
                    sub = "+ Inbound",
                    icon = Icons.Default.ArrowDownward,
                    iconColor = StockProColors.Success,
                    containerColor = StockProColors.SuccessContainer,
                    onClick = onReceiveStockClick,
                    modifier = Modifier.weight(1f),
                )
                FloorActionButton(
                    title = "Issue",
                    sub = "- Outbound",
                    icon = Icons.Default.ArrowUpward,
                    iconColor = StockProColors.Primary,
                    containerColor = StockProColors.PrimaryContainer,
                    onClick = onIssueStockClick,
                    modifier = Modifier.weight(1f),
                )
                FloorActionButton(
                    title = "Catalog",
                    sub = "Inventory",
                    icon = Icons.Default.Inventory2,
                    iconColor = StockProColors.TextPrimary,
                    containerColor = StockProColors.SurfaceAlt,
                    onClick = onNavigateToInventory,
                    modifier = Modifier.weight(1f),
                )
                FloorActionButton(
                    title = "Reports",
                    sub = "Analytics",
                    icon = Icons.Default.BarChart,
                    iconColor = StockProColors.Production,
                    containerColor = StockProColors.ProductionContainer,
                    onClick = onNavigateToReports,
                    modifier = Modifier.weight(1f),
                )
            }

            // Priority Restock Alerts (Conditional when items need attention)
            if (state.topLowStockItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = Radius.lg,
                    color = StockProColors.Surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, StockProColors.OutlineSubtle, Radius.lg),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(StockProColors.Warning),
                                )
                                Spacer(modifier = Modifier.width(Spacing.x1))
                                Text(
                                    text = "${strings.lowStockAlerts} (${state.topLowStockItems.size})",
                                    style = StockProCodeTextStyle.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StockProColors.TextPrimary,
                                    ),
                                )
                            }
                            Text(
                                text = strings.viewAll,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = StockProColors.Primary,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                modifier = Modifier.clickable { onNavigateToInventory() },
                            )
                        }

                        Spacer(modifier = Modifier.height(Spacing.x2))

                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.x2)) {
                            state.topLowStockItems.take(4).forEach { item ->
                                PriorityRestockRow(
                                    item = item,
                                    onClick = { onItemClick(item.id) },
                                    onRestockClick = onReceiveStockClick,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 6. Recent Movements Ledger (Synchronized Activity / Empty State)
            Surface(
                shape = Radius.lg,
                color = StockProColors.Surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StockProColors.OutlineSubtle, Radius.lg),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = strings.recentActivity,
                            style = StockProCodeTextStyle.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF475569),
                                letterSpacing = 0.5.sp,
                            ),
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable(onClick = onNavigateToReports),
                        ) {
                            Text(
                                text = strings.viewAll,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = StockProColors.Primary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                ),
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = StockProColors.Primary,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (state.recentActivities.isEmpty()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(StockProColors.OutlineSubtle),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.Article,
                                    contentDescription = null,
                                    tint = StockProColors.TextTertiary,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = strings.noRecentActivity,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = Color(0xFF475569),
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Receive, issue or transfer items to see them here.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                color = StockProColors.TextTertiary,
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.x2)) {
                            state.recentActivities.take(5).forEach { activity ->
                                RecentActivityRow(activity = activity)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Sub-Components ──

@Composable
private fun HomeKpiCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    iconBgColor: Color,
    barColor: Color,
    barHeights: List<Float>,
    deltaText: String,
    subText: String,
    isPill: Boolean = true,
    warningText: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = Radius.lg,
        color = StockProColors.Surface,
        modifier = modifier.border(1.dp, StockProColors.OutlineSubtle, Radius.lg),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconBgColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp),
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = StockProCodeTextStyle.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = StockProColors.TextSecondary,
                            letterSpacing = 0.2.sp,
                        ),
                        maxLines = 1,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = value,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.3).sp,
                        ),
                        color = StockProColors.TextPrimary,
                        maxLines = 1,
                    )
                }

                MiniBarGraphic(
                    barColor = barColor,
                    heights = barHeights,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isPill) {
                    Surface(
                        shape = Radius.pill,
                        color = StockProColors.SuccessContainer,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = StockProColors.Success,
                                modifier = Modifier.size(11.dp),
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = deltaText,
                                style = StockProCodeTextStyle.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StockProColors.Success,
                                ),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = subText,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = StockProColors.TextSecondary,
                        maxLines = 1,
                    )
                } else {
                    Text(
                        text = deltaText,
                        style = StockProCodeTextStyle.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (warningText) Color(0xFFD97706) else StockProColors.TextPrimary,
                        ),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = subText,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = StockProColors.TextSecondary,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniBarGraphic(
    barColor: Color,
    heights: List<Float> = listOf(0.40f, 0.85f, 0.30f, 0.65f),
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.height(26.dp),
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        heights.forEach { fraction ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((26 * fraction).dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(barColor),
            )
        }
    }
}

@Composable
private fun ValuationSparklineGraphic(
    modifier: Modifier = Modifier,
    lineColor: Color = StockProColors.Success,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Smooth upward curve matching screenshot
        val path = Path().apply {
            moveTo(0f, h * 0.72f)
            cubicTo(
                w * 0.20f, h * 0.76f,
                w * 0.35f, h * 0.66f,
                w * 0.50f, h * 0.68f,
            )
            cubicTo(
                w * 0.65f, h * 0.70f,
                w * 0.78f, h * 0.45f,
                w * 0.96f, h * 0.16f,
            )
        }

        val fillPath = Path().apply {
            addPath(path)
            lineTo(w * 0.96f, h)
            lineTo(0f, h)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    lineColor.copy(alpha = 0.25f),
                    lineColor.copy(alpha = 0.02f),
                ),
                startY = 0f,
                endY = h,
            ),
        )

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(
                width = 2.5.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )

        val endPoint = Offset(w * 0.96f, h * 0.16f)
        drawCircle(
            color = Color.White,
            radius = 4.dp.toPx(),
            center = endPoint,
        )
        drawCircle(
            color = lineColor,
            radius = 3.dp.toPx(),
            center = endPoint,
        )
    }
}

@Composable
private fun FloorActionButton(
    title: String,
    sub: String,
    icon: ImageVector,
    iconColor: Color,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = Radius.lg,
        color = StockProColors.Surface,
        modifier = modifier
            .clip(Radius.lg)
            .clickable(onClick = onClick)
            .border(1.dp, StockProColors.OutlineSubtle, Radius.lg),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(Radius.sm)
                    .background(containerColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                ),
                color = StockProColors.TextPrimary,
                maxLines = 1,
            )
            Text(
                text = sub,
                style = StockProCodeTextStyle.copy(
                    fontSize = 9.5.sp,
                    color = StockProColors.TextTertiary,
                ),
                maxLines = 1,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(containerColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun IndustrialTelemetryStrip(
    syncStatus: SyncStatus,
    onNeedsAttentionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isOnline = syncStatus is SyncStatus.Synced
    val statusColor = if (isOnline) StockProColors.Success else StockProColors.Warning

    Surface(
        color = StockProColors.SurfaceAlt,
        modifier = modifier
            .fillMaxWidth()
            .border(width = 0.5.dp, color = StockProColors.OutlineSubtle),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.screenGutter, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable {
                    if (!isOnline) onNeedsAttentionClick()
                },
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(statusColor),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isOnline) "SYNC ONLINE" else "OFFLINE CACHED",
                    style = StockProCodeTextStyle.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                    ),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "•  LATENCY 18ms",
                    style = StockProCodeTextStyle.copy(
                        fontSize = 10.sp,
                        color = StockProColors.TextTertiary,
                    ),
                )
            }

            Text(
                text = "RF-ID MESH 5.8GHz",
                style = StockProCodeTextStyle.copy(
                    fontSize = 10.sp,
                    color = StockProColors.TextTertiary,
                ),
            )
        }
    }
}

@Composable
private fun PriorityRestockRow(
    item: Item,
    onClick: () -> Unit,
    onRestockClick: () -> Unit,
) {
    val currentUnits = item.onHand.toUnitsDouble
    val reorderUnits = item.reorderLevel.toUnitsDouble
    val ratio = if (reorderUnits > 0) (currentUnits / (reorderUnits * 1.5)).toFloat().coerceIn(0f, 1f) else 0f

    Surface(
        shape = Radius.sm,
        color = StockProColors.SurfaceAlt,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(Spacing.x2)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = Radius.xs,
                        color = StockProColors.Surface,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, StockProColors.Outline),
                    ) {
                        Text(
                            text = item.code,
                            style = StockProCodeTextStyle.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(Spacing.x1))
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = StockProColors.TextPrimary,
                        maxLines = 1,
                    )
                }

                StatusBadge(level = item.stockLevel)
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { ratio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(Radius.pill),
                color = if (item.stockLevel == StockLevel.OUT_OF_STOCK) StockProColors.Danger else StockProColors.Warning,
                trackColor = StockProColors.OutlineSubtle,
            )

            Spacer(modifier = Modifier.height(Spacing.x1))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Stock: ${currentUnits.toLong()} / Min: ${reorderUnits.toLong()} ${item.unitOfMeasure.lowercase()}",
                    style = StockProCodeTextStyle.copy(fontSize = 10.sp, color = StockProColors.TextSecondary),
                )

                Surface(
                    shape = Radius.pill,
                    color = StockProColors.Primary,
                    modifier = Modifier
                        .clip(Radius.pill)
                        .clickable(onClick = onRestockClick),
                ) {
                    Text(
                        text = "+ Restock",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentActivityRow(
    activity: HomeActivityItem,
) {
    val iconColor = when (activity.type) {
        MovementType.RECEIPT -> StockProColors.Success
        MovementType.ISSUE -> StockProColors.Primary
        MovementType.ADJUSTMENT -> StockProColors.Warning
        MovementType.PRODUCTION -> StockProColors.Production
        else -> StockProColors.TextSecondary
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = when (activity.type) {
                    MovementType.RECEIPT -> Icons.Default.ArrowDownward
                    MovementType.ISSUE -> Icons.Default.ArrowUpward
                    MovementType.ADJUSTMENT -> Icons.Default.Warning
                    MovementType.PRODUCTION -> Icons.Default.PrecisionManufacturing
                    else -> Icons.Default.Inventory
                },
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(15.dp),
            )
        }
        Spacer(modifier = Modifier.width(Spacing.x2))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = activity.title,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = StockProColors.TextPrimary,
                maxLines = 1,
            )
            Text(
                text = activity.subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = StockProColors.TextSecondary,
                maxLines = 1,
            )
        }
        Text(
            text = activity.timeAgo,
            style = StockProCodeTextStyle.copy(fontSize = 10.sp, color = StockProColors.TextTertiary),
        )
    }
}

@Composable
private fun HomeLoadingSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.screenGutter),
    ) {
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(28.dp),
        )
        Spacer(modifier = Modifier.height(Spacing.x3))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SkeletonBox(modifier = Modifier.weight(1f).height(90.dp))
            SkeletonBox(modifier = Modifier.weight(1f).height(90.dp))
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SkeletonBox(modifier = Modifier.weight(1f).height(90.dp))
            SkeletonBox(modifier = Modifier.weight(1f).height(90.dp))
        }
        Spacer(modifier = Modifier.height(Spacing.x3))
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
        )
        Spacer(modifier = Modifier.height(Spacing.x3))
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
        )
    }
}
