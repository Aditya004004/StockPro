package com.company.stockpro

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.company.stockpro.core.designsystem.component.StockProBottomNav
import com.company.stockpro.core.designsystem.component.StockProTopLevelDestination
import com.company.stockpro.core.designsystem.theme.Radius
import com.company.stockpro.core.designsystem.theme.Spacing
import com.company.stockpro.core.designsystem.theme.StockProColors
import com.company.stockpro.core.designsystem.theme.StockProTheme
import com.company.stockpro.core.domain.model.Item
import com.company.stockpro.core.domain.model.MovementType
import com.company.stockpro.core.ui.component.NeedsAttentionDialog
import com.company.stockpro.core.ui.component.notification.NotificationCenterDialog
import com.company.stockpro.core.ui.component.scanner.BarcodeScannerDialog
import com.company.stockpro.feature.auth.AuthRoute
import com.company.stockpro.feature.auth.AuthViewModel
import com.company.stockpro.feature.home.HomeRoute
import com.company.stockpro.feature.home.HomeViewModel
import com.company.stockpro.feature.inventory.InventoryRoute
import com.company.stockpro.feature.inventory.InventoryViewModel
import com.company.stockpro.feature.inventory.detail.ItemDetailScreen
import com.company.stockpro.feature.movements.PostMovementDialog
import com.company.stockpro.feature.reports.ReportsRoute
import com.company.stockpro.feature.reports.ReportsViewModel
import com.company.stockpro.feature.settings.SettingsRoute
import com.company.stockpro.feature.settings.SettingsViewModel
import com.company.stockpro.navigation.StockProRoute
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StockProApp()
        }
    }
}

@Composable
fun StockProApp(
    mainViewModel: MainViewModel = hiltViewModel(),
    navController: NavHostController = rememberNavController(),
) {
    val context = LocalContext.current

    // Request notification permission on Android 13+ (API 33+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { /* Granted or dismissed */ }

        LaunchedEffect(Unit) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Modal dialog state — survives config changes via rememberSaveable
    var movementItem by remember { mutableStateOf<Item?>(null) }
    var movementType by rememberSaveable { mutableStateOf(MovementType.RECEIPT) }
    var showNeedsAttention by rememberSaveable { mutableStateOf(false) }
    var showBarcodeScanner by rememberSaveable { mutableStateOf(false) }
    var showNotificationCenter by rememberSaveable { mutableStateOf(false) }
    var showAddItemDialog by rememberSaveable { mutableStateOf(false) }

    val failedEntries by mainViewModel.failedEntries.collectAsStateWithLifecycle()

    // Derive current top-level tab from nav back stack
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentTopLevelDestination = remember(navBackStackEntry) {
        when (navBackStackEntry?.destination?.route) {
            StockProRoute.Home::class.qualifiedName -> StockProTopLevelDestination.HOME
            StockProRoute.Inventory::class.qualifiedName -> StockProTopLevelDestination.INVENTORY
            StockProRoute.Reports::class.qualifiedName -> StockProTopLevelDestination.REPORTS
            StockProRoute.Settings::class.qualifiedName,
            StockProRoute.ExportDatabase::class.qualifiedName -> StockProTopLevelDestination.SETTINGS
            else -> null
        }
    }

    val authState by mainViewModel.authState.collectAsStateWithLifecycle()
    val userSettings by mainViewModel.userSettings.collectAsStateWithLifecycle()
    val isDarkTheme = when (userSettings.themeMode) {
        com.company.stockpro.core.domain.model.ThemeMode.LIGHT -> false
        com.company.stockpro.core.domain.model.ThemeMode.DARK -> true
        com.company.stockpro.core.domain.model.ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
    }

    val currentLocale = remember(userSettings.language) {
        java.util.Locale(userSettings.language.code)
    }
    java.util.Locale.setDefault(currentLocale)
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val localizedConfiguration = remember(configuration, currentLocale) {
        android.content.res.Configuration(configuration).apply {
            setLocale(currentLocale)
            setLayoutDirection(currentLocale)
        }
    }

    if (authState is AppAuthState.Loading) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.ui.platform.LocalConfiguration provides localizedConfiguration,
        ) {
            StockProTheme(darkTheme = isDarkTheme, language = userSettings.language) {
                StockProSplashScreen()
            }
        }
        return
    }

    val startDestination = if (authState is AppAuthState.Authenticated) {
        StockProRoute.Home
    } else {
        StockProRoute.Auth
    }

    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalConfiguration provides localizedConfiguration,
    ) {
        StockProTheme(darkTheme = isDarkTheme, language = userSettings.language) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = StockProColors.Canvas,
                contentColor = StockProColors.TextPrimary,
            bottomBar = {
                // Only show bottom nav on top-level destinations
                val dest = currentTopLevelDestination
                if (dest != null) {
                    StockProBottomNav(
                        currentDestination = dest,
                        onNavigateToDestination = { destination ->
                            val route: Any = when (destination) {
                                StockProTopLevelDestination.HOME -> StockProRoute.Home
                                StockProTopLevelDestination.INVENTORY -> StockProRoute.Inventory
                                StockProTopLevelDestination.REPORTS -> StockProRoute.Reports
                                StockProTopLevelDestination.SETTINGS -> StockProRoute.Settings
                            }
                            navController.navigate(route) {
                                popUpTo(StockProRoute.Home) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            },
        ) { innerPadding ->
            val modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)

            NavHost(
                navController = navController,
                startDestination = startDestination,
            ) {
                // ── Auth ──
                composable<StockProRoute.Auth> {
                    val authViewModel = hiltViewModel<AuthViewModel>()
                    AuthRoute(
                        viewModel = authViewModel,
                        onAuthenticated = {
                            mainViewModel.onAuthenticated()
                            navController.navigate(StockProRoute.Home) {
                                popUpTo(StockProRoute.Auth) { inclusive = true }
                            }
                        },
                        modifier = modifier,
                    )
                }

                // ── Home ──
                composable<StockProRoute.Home> {
                    val homeViewModel = hiltViewModel<HomeViewModel>()
                    HomeRoute(
                        viewModel = homeViewModel,
                        onNavigateToInventory = {
                            navController.navigate(StockProRoute.Inventory) {
                                popUpTo(StockProRoute.Home) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onNavigateToReports = {
                            navController.navigate(StockProRoute.Reports) {
                                popUpTo(StockProRoute.Home) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onReceiveStockClick = {
                            movementType = MovementType.RECEIPT
                            navController.navigate(StockProRoute.Inventory) {
                                popUpTo(StockProRoute.Home) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onIssueStockClick = {
                            movementType = MovementType.ISSUE
                            navController.navigate(StockProRoute.Inventory) {
                                popUpTo(StockProRoute.Home) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onItemClick = { itemId ->
                            navController.navigate(StockProRoute.ItemDetail(itemId))
                        },
                        onNotificationsClick = { showNotificationCenter = true },
                        onNeedsAttentionClick = { showNeedsAttention = true },
                        modifier = modifier,
                    )
                }

                // ── Inventory ──
                composable<StockProRoute.Inventory> {
                    val inventoryViewModel = hiltViewModel<InventoryViewModel>()
                    InventoryRoute(
                        viewModel = inventoryViewModel,
                        onItemClick = { item ->
                            navController.navigate(StockProRoute.ItemDetail(item.id))
                        },
                        onAddItemClick = { showAddItemDialog = true },
                        onScanClick = { showBarcodeScanner = true },
                        onQuickMovementClick = {
                            showBarcodeScanner = true
                        },
                        onNeedsAttentionClick = { showNeedsAttention = true },
                        modifier = modifier,
                    )
                }

                // ── Reports ──
                composable<StockProRoute.Reports> {
                    val reportsViewModel = hiltViewModel<ReportsViewModel>()
                    ReportsRoute(
                        viewModel = reportsViewModel,
                        onNavigateToInventory = {
                            navController.navigate(StockProRoute.Inventory) {
                                popUpTo(StockProRoute.Home) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onItemClick = { itemId ->
                            navController.navigate(StockProRoute.ItemDetail(itemId))
                        },
                        modifier = modifier,
                    )
                }

                // ── Settings ──
                composable<StockProRoute.Settings> {
                    val settingsViewModel = hiltViewModel<SettingsViewModel>()
                    SettingsRoute(
                        viewModel = settingsViewModel,
                        onNavigateToTeam = {},
                        onNavigateToExportDatabase = {
                            navController.navigate(StockProRoute.ExportDatabase)
                        },
                        onLogoutClick = {
                            mainViewModel.logout {
                                navController.navigate(StockProRoute.Auth) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        },
                        modifier = modifier,
                    )
                }

                // ── Export Database Hub ──
                composable<StockProRoute.ExportDatabase> {
                    com.company.stockpro.feature.settings.ExportDatabaseRoute(
                        onBackClick = { navController.popBackStack() },
                        modifier = modifier,
                    )
                }

                // ── Item Detail ──
                composable<StockProRoute.ItemDetail> { backStackEntry ->
                    val route = backStackEntry.toRoute<StockProRoute.ItemDetail>()
                    val item by mainViewModel.observeItem(route.itemId).collectAsStateWithLifecycle(initialValue = null)
                    val currentItem = item

                    if (currentItem != null) {
                        val ledger by mainViewModel.observeLedger(currentItem.id).collectAsStateWithLifecycle(initialValue = emptyList())
                        val toolCustody by mainViewModel.observeToolCustody(currentItem.id).collectAsStateWithLifecycle(initialValue = null)

                        ItemDetailScreen(
                            item = currentItem,
                            ledger = ledger,
                            onBackClick = { navController.popBackStack() },
                            onReceiveClick = {
                                movementItem = currentItem
                                movementType = MovementType.RECEIPT
                            },
                            onIssueClick = {
                                movementItem = currentItem
                                movementType = MovementType.ISSUE
                            },
                            onAdjustClick = {
                                movementItem = currentItem
                                movementType = MovementType.ADJUSTMENT
                            },
                            currency = userSettings.currency,
                            toolCustody = toolCustody,
                            onIssueTool = { operator, notes ->
                                mainViewModel.issueTool(currentItem.id, operator, notes)
                            },
                            onReturnTool = { notes ->
                                mainViewModel.returnTool(currentItem.id, notes)
                            },
                            onLogCalibration = { cal, next, notes ->
                                mainViewModel.logCalibration(currentItem.id, cal, next, notes)
                            },
                            onUpdateThreshold = { newThreshold ->
                                mainViewModel.updateItemThreshold(currentItem.id, newThreshold)
                            },
                            modifier = modifier,
                        )
                    }
                }
            }

            // ── Modal Dialogs ──

            // Post Movement Dialog
            if (movementItem != null) {
                val activeItem = movementItem!!
                PostMovementDialog(
                    item = activeItem,
                    initialType = movementType,
                    onDismiss = { movementItem = null },
                    onConfirm = { movement ->
                        mainViewModel.postMovement(
                            movement = movement,
                            onSuccess = { movementItem = null },
                            onError = { errorMsg ->
                                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                            },
                        )
                    },
                )
            }

            // Needs Attention Modal
            if (showNeedsAttention) {
                NeedsAttentionDialog(
                    failedEntries = failedEntries,
                    onRetry = { id -> mainViewModel.retryFailedOutbox(id) },
                    onDiscard = { id -> mainViewModel.discardFailedOutbox(id) },
                    onDismiss = { showNeedsAttention = false },
                )
            }

            // Offline Barcode Scanner
            if (showBarcodeScanner) {
                BarcodeScannerDialog(
                    onDismissRequest = { showBarcodeScanner = false },
                    onBarcodeScanned = { barcode ->
                        showBarcodeScanner = false
                        mainViewModel.resolveBarcode(
                            barcode = barcode,
                            onFound = { foundItem ->
                                navController.navigate(StockProRoute.ItemDetail(foundItem.id))
                            },
                            onNotFound = {
                                Toast.makeText(
                                    context,
                                    "No item found for barcode: $barcode",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            },
                        )
                    },
                )
            }

            // Notification Center Modal
            if (showNotificationCenter) {
                val notifications by mainViewModel.notifications.collectAsStateWithLifecycle()
                NotificationCenterDialog(
                    notifications = notifications,
                    onDismissRequest = { showNotificationCenter = false },
                    onNotificationClick = { entityRef ->
                        showNotificationCenter = false
                        entityRef?.let { codeOrId ->
                            mainViewModel.resolveNotificationEntity(codeOrId) { foundItem ->
                                navController.navigate(StockProRoute.ItemDetail(foundItem.id))
                            }
                        }
                    },
                    onMarkAllAsRead = { mainViewModel.markAllNotificationsAsRead() },
                    onClearAll = { mainViewModel.clearNotifications() },
                )
            }

            // Add Item Modal
            if (showAddItemDialog) {
                com.company.stockpro.feature.inventory.AddItemDialog(
                    onDismiss = { showAddItemDialog = false },
                    onConfirm = { newItem ->
                        mainViewModel.createItem(
                            item = newItem,
                            onSuccess = { created ->
                                showAddItemDialog = false
                                Toast.makeText(context, "Item '${created.name}' created.", Toast.LENGTH_SHORT).show()
                            },
                            onError = { errorMsg ->
                                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                            },
                        )
                    },
                    currency = userSettings.currency,
                )
            }
        }
    }
}
}

/**
 * Enterprise startup splash screen displayed while authentication session and
 * persistent active plant context are being restored. Prevents UI flashing.
 */
@Composable
fun StockProSplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StockProColors.Canvas),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(Radius.md)
                    .background(StockProColors.Primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    tint = StockProColors.Surface,
                    modifier = Modifier.size(40.dp),
                )
            }
            Spacer(modifier = Modifier.height(Spacing.x3))
            Text(
                text = "StockPro",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = StockProColors.TextPrimary,
                    letterSpacing = (-0.5).sp,
                ),
            )
            Spacer(modifier = Modifier.height(Spacing.x1))
            Text(
                text = "Enterprise Inventory Intelligence",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = StockProColors.TextSecondary,
                    fontWeight = FontWeight.Medium,
                ),
            )
            Spacer(modifier = Modifier.height(Spacing.x4))
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = StockProColors.Primary,
                strokeWidth = 2.5.dp,
            )
        }
    }
}

