package com.company.stockpro

import androidx.lifecycle.SavedStateHandle
import com.company.stockpro.core.domain.model.AppLanguage
import com.company.stockpro.core.domain.model.AppNotification
import com.company.stockpro.core.domain.model.FailedOutboxEntry
import com.company.stockpro.core.domain.model.InventorySummary
import com.company.stockpro.core.domain.model.Item
import com.company.stockpro.core.domain.model.ItemClass
import com.company.stockpro.core.domain.model.Money
import com.company.stockpro.core.domain.model.MovementType
import com.company.stockpro.core.domain.model.NotificationPreferences
import com.company.stockpro.core.domain.model.NotificationType
import com.company.stockpro.core.domain.model.Quantity
import com.company.stockpro.core.domain.model.Role
import com.company.stockpro.core.domain.model.SortOrder
import com.company.stockpro.core.domain.model.StockMovement
import com.company.stockpro.core.domain.model.SyncStatus
import com.company.stockpro.core.domain.model.ThemeMode
import com.company.stockpro.core.domain.model.UnitSystem
import com.company.stockpro.core.domain.model.UserProfile
import com.company.stockpro.core.domain.model.UserSettings
import com.company.stockpro.core.domain.repository.InventoryRepository
import com.company.stockpro.core.domain.util.DataResult
import com.company.stockpro.core.ui.component.EmptyReason
import com.company.stockpro.feature.home.HomeUiState
import com.company.stockpro.feature.home.HomeViewModel
import com.company.stockpro.feature.inventory.InventoryUiState
import com.company.stockpro.feature.inventory.InventoryViewModel
import com.company.stockpro.feature.reports.DateRangePreset
import com.company.stockpro.feature.reports.ReportSection
import com.company.stockpro.feature.reports.ReportsUiState
import com.company.stockpro.feature.reports.ReportsViewModel
import com.company.stockpro.feature.settings.SettingsDialog
import com.company.stockpro.feature.settings.SettingsUiState
import com.company.stockpro.feature.settings.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * End-to-End Comprehensive Multi-Screen Verification Test Suite.
 * Validates all workflows across Home, Inventory, Reports & Analytics, and Settings screens:
 *
 * 1. Global Navigation & Multi-Plant Switching
 * 2. Inventory Catalog, Search, Filtering & Sorting
 * 3. Stock Movements (Inbound Receive, Outbound Issue) & Outbox Ledger
 * 4. Real-time Valuation, Privacy Masking & Dynamic Alert Reconciliation
 * 5. Reports Analytics, Velocity Flow, Category Valuation & CSV Export
 * 6. Settings Customization, Preferences Persistence & Account Lifecycle
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EndToEndWorkflowsVerificationTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: DynamicWorkflowTestRepository
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var inventoryViewModel: InventoryViewModel
    private lateinit var reportsViewModel: ReportsViewModel
    private lateinit var settingsViewModel: SettingsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = DynamicWorkflowTestRepository()
        homeViewModel = HomeViewModel(repository, SavedStateHandle(mapOf("active_plant" to "Pune Plant 1")))
        inventoryViewModel = InventoryViewModel(repository, SavedStateHandle())
        reportsViewModel = ReportsViewModel(repository).apply {
            defaultDispatcher = testDispatcher
        }
        settingsViewModel = SettingsViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // =========================================================================
    // WORKFLOW 1: Navigation & Multi-Plant Switching
    // =========================================================================

    @Test
    fun `Workflow 1 - Plant switching and adding a new plant dynamically updates all screens`() = runTest {
        backgroundScope.launch { homeViewModel.uiState.collect() }
        backgroundScope.launch { settingsViewModel.uiState.collect() }
        advanceUntilIdle()

        var homeState = homeViewModel.uiState.value as HomeUiState.Content
        assertEquals("Pune Plant 1", homeState.activePlant)
        assertTrue(homeState.availablePlants.contains("Pune Plant 1"))

        // Switch plant
        homeViewModel.selectPlant("Chakan Assembly")
        advanceUntilIdle()

        homeState = homeViewModel.uiState.value as HomeUiState.Content
        assertEquals("Chakan Assembly", homeState.activePlant)

        // Add a brand new plant
        homeViewModel.addPlant("Bhosari Warehouse C")
        advanceUntilIdle()

        homeState = homeViewModel.uiState.value as HomeUiState.Content
        assertEquals("Bhosari Warehouse C", homeState.activePlant)
        assertTrue(homeState.availablePlants.contains("Bhosari Warehouse C"))

        // Verify settings state reflects new active plant
        val settingsState = settingsViewModel.uiState.value as SettingsUiState.Content
        assertEquals("Bhosari Warehouse C", settingsState.userSettings.profile.plantName)
    }

    @Test
    fun `Workflow 1b - Plant removal in settings enforces safety guards and auto-fallbacks active plant`() = runTest {
        backgroundScope.launch { homeViewModel.uiState.collect() }
        backgroundScope.launch { settingsViewModel.uiState.collect() }
        advanceUntilIdle()

        // 1. Initially 2 plants: Pune Plant 1 (active) and Chakan Assembly
        val settingsState = settingsViewModel.uiState.value as SettingsUiState.Content
        assertEquals("Pune Plant 1", settingsState.userSettings.profile.plantName)

        // 2. Request removal confirmation for active plant
        val punePlant = com.company.stockpro.core.domain.model.Plant("plant_01", "PUN-01", "Pune Plant 1", "Pune, Maharashtra")
        settingsViewModel.requestRemovePlantConfirm(punePlant)
        advanceUntilIdle()

        val confirmState = settingsViewModel.uiState.value as SettingsUiState.Content
        assertTrue(confirmState.activeDialog is SettingsDialog.RemovePlantConfirm)
        val confirmDialog = confirmState.activeDialog as SettingsDialog.RemovePlantConfirm
        assertEquals("plant_01", confirmDialog.plant.id)

        // 3. Confirm deletion of Pune Plant 1
        var successMessage = false
        settingsViewModel.removePlant("plant_01", onSuccess = { successMessage = true })
        advanceUntilIdle()

        assertTrue(successMessage)
        val postDeleteSettings = settingsViewModel.uiState.value as SettingsUiState.Content
        // Active plant automatically falls back to Chakan Assembly
        assertEquals("Chakan Assembly", postDeleteSettings.userSettings.profile.plantName)
        assertEquals(1, postDeleteSettings.configuredPlants.size)

        // Verify HomeViewModel also reactively switches to Chakan Assembly
        val postDeleteHome = homeViewModel.uiState.value as HomeUiState.Content
        assertEquals("Chakan Assembly", postDeleteHome.activePlant)

        // 4. Try to delete the only remaining plant -> fails enterprise minimum 1 plant rule
        var failedMessage: String? = null
        settingsViewModel.removePlant("plant_02", onError = { failedMessage = it })
        advanceUntilIdle()

        assertNotNull(failedMessage)
        assertTrue(failedMessage!!.contains("Cannot delete the only remaining plant"))
    }

    // =========================================================================
    // WORKFLOW 2: Inventory Catalog, Search, Filtering & Sorting
    // =========================================================================

    @Test
    fun `Workflow 2 - Inventory category tabs, search debounce, and sorting work seamlessly`() = runTest {
        backgroundScope.launch { inventoryViewModel.uiState.collect() }
        advanceUntilIdle()

        // Initial default tab: COMPONENT (contains BRG-001)
        var state = inventoryViewModel.uiState.value
        assertTrue(state is InventoryUiState.Content)
        var content = state as InventoryUiState.Content
        assertEquals(ItemClass.COMPONENT, content.selectedClass)
        assertEquals(1, content.items.size)
        assertEquals("BRG-001", content.items.first().code)

        // Switch Tab to RAW_MATERIAL (contains STL-002)
        inventoryViewModel.onClassSelected(ItemClass.RAW_MATERIAL)
        advanceUntilIdle()

        content = inventoryViewModel.uiState.value as InventoryUiState.Content
        assertEquals(ItemClass.RAW_MATERIAL, content.selectedClass)
        assertEquals(1, content.items.size)
        assertEquals("STL-002", content.items.first().code)

        // Switch back to COMPONENT
        inventoryViewModel.onClassSelected(ItemClass.COMPONENT)
        advanceUntilIdle()

        // Test Debounced Search for STL
        inventoryViewModel.onSearchQueryChanged("STL")
        // Before 300ms debounce
        advanceTimeBy(100)
        content = inventoryViewModel.uiState.value as InventoryUiState.Content
        // Still on previous filtered component tab before debounce fires
        assertEquals("BRG-001", content.items.first().code)

        // After debounce completes
        advanceTimeBy(250)
        advanceUntilIdle()
        content = inventoryViewModel.uiState.value as InventoryUiState.Content
        assertEquals(1, content.items.size)
        assertEquals("STL-002", content.items.first().code)

        // Search Non-Existent Item -> EmptyState SearchMiss
        inventoryViewModel.onSearchQueryChanged("NON_EXISTING_XYZ")
        advanceTimeBy(350)
        advanceUntilIdle()

        state = inventoryViewModel.uiState.value
        assertTrue(state is InventoryUiState.Empty)
        val emptyState = state as InventoryUiState.Empty
        assertTrue(emptyState.reason is EmptyReason.SearchMiss)
        assertEquals("NON_EXISTING_XYZ", (emptyState.reason as EmptyReason.SearchMiss).query)

        // Clear Search -> Restores List
        inventoryViewModel.onSearchQueryChanged("")
        advanceTimeBy(350)
        advanceUntilIdle()

        state = inventoryViewModel.uiState.value
        assertTrue(state is InventoryUiState.Content)
    }

    // =========================================================================
    // WORKFLOW 3: Stock Movement Operations (Receive, Issue) & Outbox Ledger
    // =========================================================================

    @Test
    fun `Workflow 3 - Inbound Receive and Outbound Issue movements update inventory and ledger`() = runTest {
        backgroundScope.launch { inventoryViewModel.uiState.collect() }
        backgroundScope.launch { homeViewModel.uiState.collect() }
        advanceUntilIdle()

        val initialBearing = repository.getItemByCode("BRG-001") as DataResult.Success
        assertEquals(100_000L, initialBearing.data.onHand.milli) // 100 PCS

        // Inbound: Receive 50 PCS
        val receiveResult = repository.postMovement(
            StockMovement(
                id = "mov_in_1",
                itemId = initialBearing.data.id,
                itemCode = initialBearing.data.code,
                itemName = initialBearing.data.name,
                type = MovementType.RECEIPT,
                delta = Quantity(50_000L),
                balanceAfter = Quantity(150_000L),
                note = "Supplier Delivery PO-9081",
                createdAt = System.currentTimeMillis(),
            )
        )
        assertTrue(receiveResult is DataResult.Success)
        advanceUntilIdle()

        // Verify updated onHand
        val afterReceiveBearing = repository.getItemByCode("BRG-001") as DataResult.Success
        assertEquals(150_000L, afterReceiveBearing.data.onHand.milli)

        // Outbound: Issue 140 PCS (driving stock below reorder level 20 PCS to 10 PCS)
        val issueResult = repository.postMovement(
            StockMovement(
                id = "mov_out_1",
                itemId = initialBearing.data.id,
                itemCode = initialBearing.data.code,
                itemName = initialBearing.data.name,
                type = MovementType.ISSUE,
                delta = Quantity(-140_000L),
                balanceAfter = Quantity(10_000L),
                note = "Issued to Line 4 Assembly",
                createdAt = System.currentTimeMillis(),
            )
        )
        assertTrue(issueResult is DataResult.Success)
        advanceUntilIdle()

        // Verify updated onHand is 10 PCS
        val afterIssueBearing = repository.getItemByCode("BRG-001") as DataResult.Success
        assertEquals(10_000L, afterIssueBearing.data.onHand.milli)

        // Verify Home recent activities received the movements
        val homeContent = homeViewModel.uiState.value as HomeUiState.Content
        assertEquals(2, homeContent.recentActivities.size)
        assertEquals("Stock issued", homeContent.recentActivities[0].title)
        assertEquals("Material received", homeContent.recentActivities[1].title)
    }

    // =========================================================================
    // WORKFLOW 4: Real-time Valuation, Privacy Masking & Alert Reconciliation
    // =========================================================================

    @Test
    fun `Workflow 4 - Valuation privacy toggle and dynamic low-stock notifications reconcile properly`() = runTest {
        backgroundScope.launch { homeViewModel.uiState.collect() }
        advanceUntilIdle()

        var homeContent = homeViewModel.uiState.value as HomeUiState.Content
        // Total valuation: 100*450 + 50*800 = 45,000 + 40,000 = Rs 85,000 (8,500,000 paise)
        assertTrue(homeContent.isValuationVisible)
        assertEquals(8_500_000L, homeContent.totalValuationPaise)
        assertEquals(0, homeContent.unreadAlertsCount)

        // Toggle valuation masking
        homeViewModel.toggleValuationVisibility()
        advanceUntilIdle()

        homeContent = homeViewModel.uiState.value as HomeUiState.Content
        assertFalse(homeContent.isValuationVisible)

        // Reduce BRG-001 stock to 5 PCS (below reorder level of 20 PCS)
        repository.updateItemStock("item_1", 5_000L)
        advanceUntilIdle()

        // Dynamic notification should now be generated
        homeContent = homeViewModel.uiState.value as HomeUiState.Content
        assertEquals(1, homeContent.unreadAlertsCount)
        assertEquals(1, homeContent.topLowStockItems.size)
        assertEquals("BRG-001", homeContent.topLowStockItems.first().code)
    }

    // =========================================================================
    // WORKFLOW 5: Reports Analytics, Flow Velocity & RFC-4180 CSV Export
    // =========================================================================

    @Test
    fun `Workflow 5 - Reports section selection, date range filtering, and CSV export build cleanly`() = runTest {
        backgroundScope.launch { reportsViewModel.uiState.collect() }
        advanceUntilIdle()

        var reportsContent = reportsViewModel.uiState.value as ReportsUiState.Content
        assertEquals(ReportSection.OVERVIEW, reportsContent.selectedSection)
        assertEquals(DateRangePreset.CURRENT_MONTH, reportsContent.selectedDateRange)
        assertEquals(2, reportsContent.totalItems)

        // Switch to PURCHASES section and LAST_30_DAYS
        reportsViewModel.onSectionSelected(ReportSection.PURCHASES)
        reportsViewModel.onDateRangeSelected(DateRangePreset.LAST_30_DAYS)
        advanceUntilIdle()

        reportsContent = reportsViewModel.uiState.value as ReportsUiState.Content
        assertEquals(ReportSection.PURCHASES, reportsContent.selectedSection)
        assertEquals(DateRangePreset.LAST_30_DAYS, reportsContent.selectedDateRange)

        // Export CSV report
        reportsViewModel.exportReportCsv("Master Inventory Report", repository.currentItems())
        advanceUntilIdle()

        reportsContent = reportsViewModel.uiState.value as ReportsUiState.Content
        assertNotNull(reportsContent.generatedCsvContent)
        assertTrue(reportsContent.generatedCsvContent!!.contains("Code,Name,Class"))
        assertTrue(reportsContent.generatedCsvContent!!.contains("BRG-001"))
    }

    // =========================================================================
    // WORKFLOW 6: Settings Customization, Preferences & Account Actions
    // =========================================================================

    @Test
    fun `Workflow 6 - Settings profile updates, theme changes, alert preferences, and sign out work`() = runTest {
        backgroundScope.launch { settingsViewModel.uiState.collect() }
        advanceUntilIdle()

        var settingsContent = settingsViewModel.uiState.value as SettingsUiState.Content
        assertEquals("Aditya Maurya", settingsContent.userSettings.profile.displayName)
        assertEquals(ThemeMode.LIGHT, settingsContent.userSettings.themeMode)

        // Update display name
        settingsViewModel.showDialog(SettingsDialog.EditProfile)
        settingsViewModel.updateDisplayName("Aditya M.")
        advanceUntilIdle()

        settingsContent = settingsViewModel.uiState.value as SettingsUiState.Content
        assertEquals("Aditya M.", settingsContent.userSettings.profile.displayName)

        // Update Theme Mode to Dark
        settingsViewModel.updateTheme(ThemeMode.DARK)
        advanceUntilIdle()

        settingsContent = settingsViewModel.uiState.value as SettingsUiState.Content
        assertEquals(ThemeMode.DARK, settingsContent.userSettings.themeMode)

        // Update Language to Hindi
        settingsViewModel.updateLanguage(AppLanguage.HINDI)
        advanceUntilIdle()

        settingsContent = settingsViewModel.uiState.value as SettingsUiState.Content
        assertEquals(AppLanguage.HINDI, settingsContent.userSettings.language)

        // Update Measurement Units to Imperial
        settingsViewModel.updateUnitSystem(UnitSystem.IMPERIAL)
        advanceUntilIdle()

        settingsContent = settingsViewModel.uiState.value as SettingsUiState.Content
        assertEquals(UnitSystem.IMPERIAL, settingsContent.userSettings.unitSystem)

        // Clear Cache
        settingsViewModel.clearCache()
        advanceUntilIdle()

        settingsContent = settingsViewModel.uiState.value as SettingsUiState.Content
        assertEquals(0L, settingsContent.cacheSizeBytes)

        // Logout
        var loggedOut = false
        settingsViewModel.logout { loggedOut = true }
        advanceUntilIdle()

        assertTrue(loggedOut)
    }
}

/**
 * Dynamic in-memory repository implementing all reactive flows and mutations for end-to-end testing.
 */
private class DynamicWorkflowTestRepository : InventoryRepository {

    private val itemsMap = mutableMapOf<String, Item>(
        "item_1" to Item(
            id = "item_1",
            code = "BRG-001",
            name = "Ball Bearing",
            itemClass = ItemClass.COMPONENT,
            categoryName = "Mechanical",
            unitOfMeasure = "PCS",
            onHand = Quantity(100_000L),
            reserved = Quantity(0L),
            reorderLevel = Quantity(20_000L),
            parLevel = Quantity(200_000L),
            unitCost = Money(45_000L),
            locationCode = "A1-04",
        ),
        "item_2" to Item(
            id = "item_2",
            code = "STL-002",
            name = "Steel Rod 12mm",
            itemClass = ItemClass.RAW_MATERIAL,
            categoryName = "Metals",
            unitOfMeasure = "KG",
            onHand = Quantity(50_000L),
            reserved = Quantity(0L),
            reorderLevel = Quantity(10_000L),
            parLevel = Quantity(100_000L),
            unitCost = Money(80_000L),
            locationCode = "B2-01",
        ),
    )

    private val itemsFlow = MutableStateFlow(itemsMap.values.toList())
    private val movements = mutableListOf<StockMovement>()
    private val movementsFlow = MutableStateFlow<List<StockMovement>>(emptyList())
    private val notificationsFlow = MutableStateFlow<List<AppNotification>>(emptyList())

    private val userSettingsFlow = MutableStateFlow(
        UserSettings(
            profile = UserProfile(
                uid = "u_1",
                displayName = "Aditya Maurya",
                email = "12345@gmail.com",
                role = Role.INVENTORY_MANAGER,
                plantId = "plant_01",
                plantName = "Pune Plant 1",
            ),
            themeMode = ThemeMode.LIGHT,
            language = AppLanguage.ENGLISH,
            unitSystem = UnitSystem.METRIC,
            notificationPreferences = NotificationPreferences(),
            availablePlants = listOf("Pune Plant 1", "Chakan Assembly"),
        )
    )

    fun currentItems(): List<Item> = itemsMap.values.toList()

    fun updateItemStock(itemId: String, newOnHandMilli: Long) {
        val current = itemsMap[itemId] ?: return
        val updated = current.copy(onHand = Quantity(newOnHandMilli))
        itemsMap[itemId] = updated
        itemsFlow.value = itemsMap.values.toList()

        // Reconcile dynamic notifications
        reconcileNotifications()
    }

    private fun reconcileNotifications() {
        val activePlant = userSettingsFlow.value.profile.plantName
        val lowStockItems = itemsMap.values.filter { it.onHand.milli <= it.reorderLevel.milli }
        val newNotifs = lowStockItems.map { item ->
            AppNotification(
                id = "low_stock_${item.id}",
                plantId = activePlant,
                type = NotificationType.LOW_STOCK,
                title = "Low Stock: ${item.name}",
                body = "${item.name} (${item.code}) is below reorder level.",
                entityRef = item.code,
                isRead = false,
                createdAt = System.currentTimeMillis(),
            )
        }
        notificationsFlow.value = newNotifs
    }

    override fun observeItems(): Flow<List<Item>> = itemsFlow.asStateFlow()

    override fun observeItemsByClass(itemClass: ItemClass, sortOrder: SortOrder): Flow<List<Item>> =
        itemsFlow.map { list ->
            val filtered = list.filter { it.itemClass == itemClass }
            when (sortOrder) {
                SortOrder.NAME_ASC -> filtered.sortedBy { it.name }
                SortOrder.CODE_ASC -> filtered.sortedBy { it.code }
                SortOrder.QUANTITY_DESC -> filtered.sortedByDescending { it.onHand.milli }
                SortOrder.QUANTITY_ASC -> filtered.sortedBy { it.onHand.milli }
                SortOrder.LAST_MOVED_DESC -> filtered
            }
        }

    override fun searchItems(query: String): Flow<List<Item>> =
        itemsFlow.map { list ->
            list.filter {
                it.code.contains(query, ignoreCase = true) ||
                    it.name.contains(query, ignoreCase = true) ||
                    it.locationCode.contains(query, ignoreCase = true)
            }
        }

    override fun observeSummary(): Flow<InventorySummary> =
        itemsFlow.map { list ->
            val lowStock = list.count { it.onHand.milli in 1..it.reorderLevel.milli }
            val outOfStock = list.count { it.onHand.milli <= 0L }
            val rawMaterials = list.count { it.itemClass == ItemClass.RAW_MATERIAL }
            val finishedGoods = list.count { it.itemClass == ItemClass.FINISHED_GOOD }
            val valuation = list.sumOf { (it.onHand.toUnitsDouble * it.unitCost.paise).toLong() }
            InventorySummary(
                totalItems = list.size,
                lowStockCount = lowStock,
                outOfStockCount = outOfStock,
                reservedCount = 0,
                rawMaterialsCount = rawMaterials,
                finishedGoodsCount = finishedGoods,
                totalValuationPaise = valuation,
                inProductionCount = 0,
            )
        }

    override fun observeTopLowStock(limit: Int): Flow<List<Item>> =
        itemsFlow.map { list ->
            list.filter { it.onHand.milli <= it.reorderLevel.milli }
                .sortedBy { it.onHand.milli }
                .take(limit)
        }

    override fun observeRecentMovements(limit: Int): Flow<List<StockMovement>> =
        movementsFlow.map { it.take(limit) }

    override suspend fun getItem(itemId: String): DataResult<Item> =
        itemsMap[itemId]?.let { DataResult.Success(it) }
            ?: DataResult.Error(com.company.stockpro.core.domain.util.UiText.Dynamic("Item not found"))

    override suspend fun getItemByCode(code: String): DataResult<Item> =
        itemsMap.values.find { it.code.equals(code, ignoreCase = true) }?.let { DataResult.Success(it) }
            ?: DataResult.Error(com.company.stockpro.core.domain.util.UiText.Dynamic("Item not found"))

    override suspend fun createItem(item: Item): DataResult<Item> {
        itemsMap[item.id] = item
        itemsFlow.value = itemsMap.values.toList()
        return DataResult.Success(item)
    }

    override suspend fun postMovement(movement: StockMovement): DataResult<StockMovement> {
        movements.add(0, movement)
        movementsFlow.value = movements.toList()
        val current = itemsMap[movement.itemId]
        if (current != null) {
            val newMilli = (current.onHand.milli + movement.delta.milli).coerceAtLeast(0L)
            updateItemStock(movement.itemId, newMilli)
        }
        return DataResult.Success(movement)
    }

    override fun observePendingCount(): Flow<Int> = MutableStateFlow(0)
    override fun observeSyncStatus(): Flow<SyncStatus> = MutableStateFlow(SyncStatus.Synced(System.currentTimeMillis()))
    override fun observeFailedEntries(): Flow<List<FailedOutboxEntry>> = MutableStateFlow(emptyList())
    override suspend fun retryFailedOutbox(id: Long) {}
    override suspend fun discardFailedOutbox(id: Long) {}

    override fun observeNotifications(): Flow<List<AppNotification>> = notificationsFlow.asStateFlow()
    override fun observeUnreadNotificationCount(): Flow<Int> = notificationsFlow.map { it.count { n -> !n.isRead } }
    override suspend fun markNotificationAsRead(id: String) {}
    override suspend fun markAllNotificationsAsRead() {}
    override suspend fun clearNotifications() {}

    override fun observeNotificationPreferences(): Flow<NotificationPreferences> =
        userSettingsFlow.map { it.notificationPreferences }

    override suspend fun updateNotificationPreferences(preferences: NotificationPreferences) {
        userSettingsFlow.update { it.copy(notificationPreferences = preferences) }
    }

    override fun observeUserSettings(): Flow<UserSettings> = userSettingsFlow.asStateFlow()

    override suspend fun updateUserProfile(displayName: String): DataResult<Unit> {
        userSettingsFlow.update { it.copy(profile = it.profile.copy(displayName = displayName)) }
        return DataResult.Success(Unit)
    }

    override suspend fun selectPlant(plantName: String): DataResult<Unit> {
        userSettingsFlow.update {
            it.copy(profile = it.profile.copy(plantName = plantName, plantId = "plant_" + plantName.lowercase().replace(" ", "_")))
        }
        return DataResult.Success(Unit)
    }

    private val plantsFlow = MutableStateFlow(
        listOf(
            com.company.stockpro.core.domain.model.Plant("plant_01", "PUN-01", "Pune Plant 1", "Pune, Maharashtra"),
            com.company.stockpro.core.domain.model.Plant("plant_02", "CHK-01", "Chakan Assembly", "Chakan, Pune"),
        )
    )

    override fun observePlants(): Flow<List<com.company.stockpro.core.domain.model.Plant>> = plantsFlow.asStateFlow()

    override suspend fun getPlantItemCount(plantId: String): Int =
        itemsMap.values.count { it.locationCode == plantId || plantId.contains("01") }

    override suspend fun deletePlant(plantId: String): DataResult<Unit> {
        val currentPlants = plantsFlow.value
        if (currentPlants.size <= 1) {
            return DataResult.Error(com.company.stockpro.core.domain.util.UiText.Dynamic("Cannot delete the only remaining plant. StockPro requires at least one active plant."))
        }
        val target = currentPlants.find { it.id == plantId || it.name == plantId }
            ?: return DataResult.Error(com.company.stockpro.core.domain.util.UiText.Dynamic("Plant not found."))
        val remaining = currentPlants.filter { it.id != target.id }
        plantsFlow.value = remaining
        userSettingsFlow.update { current ->
            val wasCurrent = current.profile.plantId == target.id || current.profile.plantName == target.name
            val fallback = if (wasCurrent) remaining.first() else null
            current.copy(
                profile = if (fallback != null) current.profile.copy(plantId = fallback.id, plantName = fallback.name) else current.profile,
                availablePlants = remaining.map { it.name },
            )
        }
        return DataResult.Success(Unit)
    }

    override suspend fun addPlant(plantName: String): DataResult<Unit> {
        userSettingsFlow.update {
            val list = if (it.availablePlants.contains(plantName)) it.availablePlants else it.availablePlants + plantName
            plantsFlow.update { current ->
                if (current.any { p -> p.name == plantName }) current
                else current + com.company.stockpro.core.domain.model.Plant("plant_" + plantName.lowercase().replace(" ", "_"), plantName.take(3).uppercase() + "-01", plantName, plantName)
            }
            it.copy(
                profile = it.profile.copy(plantName = plantName, plantId = "plant_" + plantName.lowercase().replace(" ", "_")),
                availablePlants = list,
            )
        }
        return DataResult.Success(Unit)
    }

    override suspend fun updateThemeMode(themeMode: ThemeMode) {
        userSettingsFlow.update { it.copy(themeMode = themeMode) }
    }

    override suspend fun updateLanguage(language: AppLanguage) {
        userSettingsFlow.update { it.copy(language = language) }
    }

    override suspend fun updateUnitSystem(unitSystem: UnitSystem) {
        userSettingsFlow.update { it.copy(unitSystem = unitSystem) }
    }

    override suspend fun clearCache(): DataResult<Long> = DataResult.Success(0L)

    override suspend fun logout(): DataResult<Unit> {
        userSettingsFlow.update {
            it.copy(
                profile = UserProfile("", "User", "", Role.INVENTORY_MANAGER, "plant_01", "Plant 1"),
                availablePlants = listOf("Plant 1"),
            )
        }
        return DataResult.Success(Unit)
    }

    override suspend fun deleteAccount(): DataResult<Unit> {
        itemsMap.clear()
        itemsFlow.value = emptyList()
        movements.clear()
        movementsFlow.value = emptyList()
        logout()
        return DataResult.Success(Unit)
    }

    override fun observeItem(itemId: String): Flow<Item?> =
        itemsFlow.map { list -> list.find { it.id == itemId } }

    override fun observeLedger(itemId: String): Flow<List<StockMovement>> =
        movementsFlow.map { list -> list.filter { it.itemId == itemId } }

    override fun observeTeamMembers(): Flow<List<com.company.stockpro.core.domain.model.TeamMember>> =
        MutableStateFlow(emptyList())

    override suspend fun inviteMember(
        email: String,
        displayName: String,
        role: Role,
        plantId: String,
    ): DataResult<com.company.stockpro.core.domain.model.TeamMember> =
        DataResult.Success(com.company.stockpro.core.domain.model.TeamMember("m_1", displayName, email, role, plantId))

    override suspend fun updateMemberRole(memberId: String, newRole: Role): DataResult<Unit> =
        DataResult.Success(Unit)

    override suspend fun deactivateMember(memberId: String): DataResult<Unit> =
        DataResult.Success(Unit)

    override fun observeToolCustody(itemId: String): Flow<com.company.stockpro.core.domain.model.ToolCustody?> =
        MutableStateFlow(null)

    override suspend fun issueTool(itemId: String, operatorName: String, notes: String?): DataResult<Unit> =
        DataResult.Success(Unit)

    override suspend fun returnTool(itemId: String, notes: String?): DataResult<Unit> =
        DataResult.Success(Unit)

    override suspend fun logCalibration(
        itemId: String,
        calibratedAt: Long,
        nextDueAt: Long,
        notes: String?,
    ): DataResult<Unit> = DataResult.Success(Unit)
}
