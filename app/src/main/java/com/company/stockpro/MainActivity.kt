package com.company.stockpro

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.company.stockpro.core.designsystem.component.StockProBottomNav
import com.company.stockpro.core.designsystem.component.StockProTopLevelDestination
import com.company.stockpro.core.designsystem.theme.StockProTheme
import com.company.stockpro.core.domain.model.Item
import com.company.stockpro.core.domain.model.MovementType
import com.company.stockpro.core.domain.repository.InventoryRepository
import com.company.stockpro.core.domain.usecase.PostMovementUseCase
import com.company.stockpro.core.domain.util.DataResult
import com.company.stockpro.core.domain.util.UiText
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
import com.company.stockpro.feature.settings.team.TeamManagementScreen
import com.company.stockpro.navigation.StockProRoute
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var repository: InventoryRepository
    @Inject lateinit var postMovementUseCase: PostMovementUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StockProApp(
                repository = repository,
                postMovementUseCase = postMovementUseCase,
            )
        }
    }
}

@Composable
fun StockProApp(
    repository: InventoryRepository,
    postMovementUseCase: PostMovementUseCase,
    navController: NavHostController = rememberNavController(),
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Modal dialog state — survives config changes via rememberSaveable where possible
    var movementItem by remember { mutableStateOf<Item?>(null) }
    var movementType by rememberSaveable { mutableStateOf(MovementType.RECEIPT) }
    var showNeedsAttention by rememberSaveable { mutableStateOf(false) }
    var showBarcodeScanner by rememberSaveable { mutableStateOf(false) }
    var showNotificationCenter by rememberSaveable { mutableStateOf(false) }
    var showAddItemDialog by rememberSaveable { mutableStateOf(false) }

    val failedEntries by repository.observeFailedEntries().collectAsState(initial = emptyList())

    // Derive current top-level tab from nav back stack
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentTopLevelDestination = remember(navBackStackEntry) {
        when (navBackStackEntry?.destination?.route) {
            StockProRoute.Home::class.qualifiedName -> StockProTopLevelDestination.HOME
            StockProRoute.Inventory::class.qualifiedName -> StockProTopLevelDestination.INVENTORY
            StockProRoute.Reports::class.qualifiedName -> StockProTopLevelDestination.REPORTS
            StockProRoute.Settings::class.qualifiedName,
            StockProRoute.TeamManagement::class.qualifiedName -> StockProTopLevelDestination.SETTINGS
            else -> null
        }
    }

    StockProTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                // Only show bottom nav on top-level destinations (not detail screens or auth)
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
            val modifier = Modifier.padding(innerPadding)

            NavHost(
                navController = navController,
                startDestination = StockProRoute.Auth,
            ) {
                // ── Auth ──
                composable<StockProRoute.Auth> {
                    val authViewModel = hiltViewModel<AuthViewModel>()
                    AuthRoute(
                        viewModel = authViewModel,
                        onAuthenticated = {
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
                            // Open barcode scanner to pick an item for quick movement
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
                        onNavigateToTeam = {
                            navController.navigate(StockProRoute.TeamManagement)
                        },
                        onLogoutClick = {
                            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                            navController.navigate(StockProRoute.Auth) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        modifier = modifier,
                    )
                }

                // ── Team Management ──
                composable<StockProRoute.TeamManagement> {
                    TeamManagementScreen(
                        repository = repository,
                        onBackClick = { navController.popBackStack() },
                        modifier = modifier,
                    )
                }

                // ── Item Detail ──
                composable<StockProRoute.ItemDetail> { backStackEntry ->
                    val route = backStackEntry.toRoute<StockProRoute.ItemDetail>()
                    val item by repository.observeItem(route.itemId).collectAsState(initial = null)
                    val currentItem = item

                    if (currentItem != null) {
                        val ledger by repository.observeLedger(currentItem.id).collectAsState(initial = emptyList())
                        val toolCustody by repository.observeToolCustody(currentItem.id).collectAsState(initial = null)

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
                            toolCustody = toolCustody,
                            onIssueTool = { operator, notes ->
                                coroutineScope.launch { repository.issueTool(currentItem.id, operator, notes) }
                            },
                            onReturnTool = { notes ->
                                coroutineScope.launch { repository.returnTool(currentItem.id, notes) }
                            },
                            onLogCalibration = { cal, next, notes ->
                                coroutineScope.launch { repository.logCalibration(currentItem.id, cal, next, notes) }
                            },
                            modifier = modifier,
                        )
                    }
                }
            }

            // ── Modal Dialogs (overlay on any screen) ──

            // Post Movement Dialog
            if (movementItem != null) {
                val activeItem = movementItem!!
                PostMovementDialog(
                    item = activeItem,
                    initialType = movementType,
                    onDismiss = { movementItem = null },
                    onConfirm = { movement ->
                        coroutineScope.launch {
                            when (val result = postMovementUseCase(movement)) {
                                is DataResult.Success -> movementItem = null
                                is DataResult.Error -> {
                                    val msg = (result.message as? UiText.Dynamic)?.value
                                        ?: result.message.toString()
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                )
            }

            // Needs Attention Modal
            if (showNeedsAttention) {
                NeedsAttentionDialog(
                    failedEntries = failedEntries,
                    onRetry = { id ->
                        coroutineScope.launch { repository.retryFailedOutbox(id) }
                    },
                    onDiscard = { id ->
                        coroutineScope.launch { repository.discardFailedOutbox(id) }
                    },
                    onDismiss = { showNeedsAttention = false },
                )
            }

            // Offline Barcode Scanner
            if (showBarcodeScanner) {
                BarcodeScannerDialog(
                    onDismissRequest = { showBarcodeScanner = false },
                    onBarcodeScanned = { barcode ->
                        showBarcodeScanner = false
                        coroutineScope.launch {
                            val result = repository.getItemByCode(barcode)
                            if (result is DataResult.Success) {
                                navController.navigate(StockProRoute.ItemDetail(result.data.id))
                            } else {
                                Toast.makeText(
                                    context,
                                    "No item found for barcode: $barcode",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        }
                    },
                )
            }

            // Notification Center Modal
            if (showNotificationCenter) {
                val notifications by repository.observeNotifications().collectAsState(initial = emptyList())
                NotificationCenterDialog(
                    notifications = notifications,
                    onDismissRequest = { showNotificationCenter = false },
                    onNotificationClick = { entityRef ->
                        showNotificationCenter = false
                        entityRef?.let { codeOrId ->
                            coroutineScope.launch {
                                val res = repository.getItem(codeOrId)
                                if (res is DataResult.Success) {
                                    navController.navigate(StockProRoute.ItemDetail(res.data.id))
                                } else {
                                    val codeRes = repository.getItemByCode(codeOrId)
                                    if (codeRes is DataResult.Success) {
                                        navController.navigate(StockProRoute.ItemDetail(codeRes.data.id))
                                    }
                                }
                            }
                        }
                    },
                    onMarkAllAsRead = {
                        coroutineScope.launch { repository.markAllNotificationsAsRead() }
                    },
                    onClearAll = {
                        coroutineScope.launch { repository.clearNotifications() }
                    },
                )
            }

            // Add Item Modal
            if (showAddItemDialog) {
                com.company.stockpro.feature.inventory.AddItemDialog(
                    onDismiss = { showAddItemDialog = false },
                    onConfirm = { newItem ->
                        coroutineScope.launch {
                            val result = repository.createItem(newItem)
                            if (result is DataResult.Success) {
                                showAddItemDialog = false
                                Toast.makeText(context, "Item '${newItem.name}' created.", Toast.LENGTH_SHORT).show()
                            } else if (result is DataResult.Error) {
                                val msg = (result.message as? UiText.Dynamic)?.value ?: result.message.toString()
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                )
            }
        }
    }
}
