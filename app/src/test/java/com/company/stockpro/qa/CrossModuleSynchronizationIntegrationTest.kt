package com.company.stockpro.qa

import androidx.lifecycle.SavedStateHandle
import com.company.stockpro.core.domain.model.AppLanguage
import com.company.stockpro.core.domain.model.Currency
import com.company.stockpro.core.domain.model.InventorySummary
import com.company.stockpro.core.domain.model.Item
import com.company.stockpro.core.domain.model.ItemClass
import com.company.stockpro.core.domain.model.Money
import com.company.stockpro.core.domain.model.NotificationPreferences
import com.company.stockpro.core.domain.model.Quantity
import com.company.stockpro.core.domain.model.Role
import com.company.stockpro.core.domain.model.StockMovement
import com.company.stockpro.core.domain.model.SyncStatus
import com.company.stockpro.core.domain.model.ThemeMode
import com.company.stockpro.core.domain.model.UnitSystem
import com.company.stockpro.core.domain.model.UserProfile
import com.company.stockpro.core.domain.model.UserSettings
import com.company.stockpro.core.domain.repository.InventoryRepository
import com.company.stockpro.core.domain.util.DataResult
import com.company.stockpro.feature.home.HomeUiState
import com.company.stockpro.feature.home.HomeViewModel
import com.company.stockpro.feature.inventory.InventoryUiState
import com.company.stockpro.feature.inventory.InventoryViewModel
import com.company.stockpro.feature.reports.ReportsUiState
import com.company.stockpro.feature.reports.ReportsViewModel
import com.company.stockpro.feature.settings.ExportFormat
import com.company.stockpro.feature.settings.SettingsUiState
import com.company.stockpro.feature.settings.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Phase 5 QA Test Suite: Cross-Module Reactive Synchronization Integration Test.
 * Validates real-time state synchronization across Home, Inventory, Reports, and Settings screens.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CrossModuleSynchronizationIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: SharedIntegrationRepository
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var inventoryViewModel: InventoryViewModel
    private lateinit var reportsViewModel: ReportsViewModel
    private lateinit var settingsViewModel: SettingsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = SharedIntegrationRepository()
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

    @Test
    fun `plant synchronization - changing plant in Home updates Inventory, Reports, and Settings immediately`() = runTest {
        backgroundScope.launch { homeViewModel.uiState.collect() }
        backgroundScope.launch { inventoryViewModel.uiState.collect() }
        backgroundScope.launch { reportsViewModel.uiState.collect() }
        backgroundScope.launch { settingsViewModel.uiState.collect() }
        advanceUntilIdle()

        // Verify initial plant across all screens
        assertEquals("Pune Plant 1", (homeViewModel.uiState.value as HomeUiState.Content).activePlant)
        assertEquals("Pune Plant 1", (inventoryViewModel.uiState.value as InventoryUiState.Content).activePlant)
        assertEquals("Pune Plant 1", (reportsViewModel.uiState.value as ReportsUiState.Content).activePlant)
        assertEquals("Pune Plant 1", (settingsViewModel.uiState.value as SettingsUiState.Content).userSettings.profile.plantName)

        // Select new plant from Home Screen dropdown: "Chakan Assembly"
        homeViewModel.selectPlant("Chakan Assembly")
        advanceUntilIdle()

        // Verify all four screens synchronized immediately
        assertEquals("Chakan Assembly", (homeViewModel.uiState.value as HomeUiState.Content).activePlant)
        assertEquals("Chakan Assembly", (inventoryViewModel.uiState.value as InventoryUiState.Content).activePlant)
        assertEquals("Chakan Assembly", (reportsViewModel.uiState.value as ReportsUiState.Content).activePlant)
        assertEquals("Chakan Assembly", (settingsViewModel.uiState.value as SettingsUiState.Content).userSettings.profile.plantName)

        // Now select new plant from Reports Screen: "Plant 01 Production"
        reportsViewModel.selectPlant("Plant 01 Production")
        advanceUntilIdle()

        assertEquals("Plant 01 Production", (homeViewModel.uiState.value as HomeUiState.Content).activePlant)
        assertEquals("Plant 01 Production", (inventoryViewModel.uiState.value as InventoryUiState.Content).activePlant)
        assertEquals("Plant 01 Production", (reportsViewModel.uiState.value as ReportsUiState.Content).activePlant)
        assertEquals("Plant 01 Production", (settingsViewModel.uiState.value as SettingsUiState.Content).userSettings.profile.plantName)
    }

    @Test
    fun `currency synchronization - selecting USD updates Home, Inventory, Reports, and export formats immediately`() = runTest {
        backgroundScope.launch { homeViewModel.uiState.collect() }
        backgroundScope.launch { inventoryViewModel.uiState.collect() }
        backgroundScope.launch { reportsViewModel.uiState.collect() }
        advanceUntilIdle()

        // Initial currency is INR
        assertEquals(Currency.INR, (homeViewModel.uiState.value as HomeUiState.Content).currency)
        assertEquals(Currency.INR, (inventoryViewModel.uiState.value as InventoryUiState.Content).currency)
        assertEquals(Currency.INR, (reportsViewModel.uiState.value as ReportsUiState.Content).currency)

        // Switch to USD
        homeViewModel.selectCurrency(Currency.USD)
        advanceUntilIdle()

        // Verify immediate propagation across all view models
        assertEquals(Currency.USD, (homeViewModel.uiState.value as HomeUiState.Content).currency)
        assertEquals(Currency.USD, (inventoryViewModel.uiState.value as InventoryUiState.Content).currency)
        assertEquals(Currency.USD, (reportsViewModel.uiState.value as ReportsUiState.Content).currency)

        // Export report from Reports Screen and verify currency symbol in headers
        reportsViewModel.exportReportCsv("Master Inventory Report", repository.currentItems())
        advanceUntilIdle()

        val generatedCsv = (reportsViewModel.uiState.value as ReportsUiState.Content).generatedCsvContent
        assertNotNull(generatedCsv)
        assertTrue(generatedCsv!!.contains("Unit Cost ($)"))
    }

    @Test
    fun `multi-field inventory search - matches item by name, sku, category, and warehouse location in real time`() = runTest {
        backgroundScope.launch { inventoryViewModel.uiState.collect() }
        advanceUntilIdle()

        // Initial state has 3 items
        assertEquals(3, (inventoryViewModel.uiState.value as InventoryUiState.Content).items.size)

        // 1. Search by Name: "Piston"
        inventoryViewModel.onSearchQueryChanged("Piston")
        advanceTimeBy(350)
        advanceUntilIdle()
        var content = inventoryViewModel.uiState.value as InventoryUiState.Content
        assertEquals(1, content.items.size)
        assertEquals("Piston Assembly", content.items[0].name)

        // 2. Search by SKU / Code: "ELE-01"
        inventoryViewModel.onSearchQueryChanged("ELE-01")
        advanceTimeBy(350)
        advanceUntilIdle()
        content = inventoryViewModel.uiState.value as InventoryUiState.Content
        assertEquals(1, content.items.size)
        assertEquals("Sensor Harness", content.items[0].name)

        // 3. Search by Category: "Raw Materials"
        inventoryViewModel.onSearchQueryChanged("Raw Materials")
        advanceTimeBy(350)
        advanceUntilIdle()
        content = inventoryViewModel.uiState.value as InventoryUiState.Content
        assertEquals(1, content.items.size)
        assertEquals("Aluminum Sheet", content.items[0].name)

        // 4. Search by Warehouse Location: "B2-01"
        inventoryViewModel.onSearchQueryChanged("B2-01")
        advanceTimeBy(350)
        advanceUntilIdle()
        content = inventoryViewModel.uiState.value as InventoryUiState.Content
        assertEquals(1, content.items.size)
        assertEquals("Aluminum Sheet", content.items[0].name)

        // 5. Clear search: returns all 3 items
        inventoryViewModel.onSearchQueryChanged("")
        advanceTimeBy(350)
        advanceUntilIdle()
        content = inventoryViewModel.uiState.value as InventoryUiState.Content
        assertEquals(3, content.items.size)
    }

    @Test
    fun `offline database export and import lifecycle in Settings`() = runTest {
        backgroundScope.launch { settingsViewModel.uiState.collect() }
        advanceUntilIdle()

        // 1. Export in JSON format
        settingsViewModel.exportDatabase(ExportFormat.JSON)
        advanceUntilIdle()
        var content = settingsViewModel.uiState.value as SettingsUiState.Content
        assertNotNull(content.exportedPayload)
        assertEquals(ExportFormat.JSON, content.exportedPayload?.format)
        assertTrue(content.exportedPayload?.content!!.contains("\"application\": \"StockPro\""))

        // 2. Clear payload
        settingsViewModel.clearExportedCsv()
        advanceUntilIdle()
        content = settingsViewModel.uiState.value as SettingsUiState.Content
        assertEquals(null, content.exportedPayload)

        // 3. Import database confirmation and execution
        settingsViewModel.importDatabase()
        advanceUntilIdle()
        content = settingsViewModel.uiState.value as SettingsUiState.Content
        assertNotNull(content.statusBannerMessage)
    }
}

/**
 * Shared in-memory test repository backing cross-module integration tests.
 */
private class SharedIntegrationRepository : InventoryRepository {

    private val itemsMap = mutableMapOf(
        "item_1" to Item(
            id = "item_1",
            code = "MEC-01",
            name = "Piston Assembly",
            itemClass = ItemClass.COMPONENT,
            categoryName = "Mechanical",
            unitOfMeasure = "pcs",
            onHand = Quantity(20_000L),
            reorderLevel = Quantity(5_000L),
            parLevel = Quantity(50_000L),
            unitCost = Money(50_000L),
            locationCode = "A1-01",
        ),
        "item_2" to Item(
            id = "item_2",
            code = "ELE-01",
            name = "Sensor Harness",
            itemClass = ItemClass.COMPONENT,
            categoryName = "Electrical",
            unitOfMeasure = "pcs",
            onHand = Quantity(15_000L),
            reorderLevel = Quantity(5_000L),
            parLevel = Quantity(50_000L),
            unitCost = Money(30_000L),
            locationCode = "A1-02",
        ),
        "item_3" to Item(
            id = "item_3",
            code = "RAW-01",
            name = "Aluminum Sheet",
            itemClass = ItemClass.RAW_MATERIAL,
            categoryName = "Raw Materials",
            unitOfMeasure = "kg",
            onHand = Quantity(50_000L),
            reorderLevel = Quantity(5_000L),
            parLevel = Quantity(50_000L),
            unitCost = Money(80_000L),
            locationCode = "B2-01",
        ),
    )

    private val itemsFlow = MutableStateFlow(itemsMap.values.toList())
    private val userSettingsFlow = MutableStateFlow(
        UserSettings(
            profile = UserProfile(
                uid = "u1",
                displayName = "Aditya Maurya",
                email = "aditya@stockpro.com",
                role = Role.INVENTORY_MANAGER,
                plantId = "plant_01",
                plantName = "Pune Plant 1",
                phone = "+91 98765 43210",
                company = "StockPro Manufacturing Ltd",
            ),
            themeMode = ThemeMode.LIGHT,
            language = AppLanguage.ENGLISH,
            unitSystem = UnitSystem.METRIC,
            currency = Currency.INR,
            notificationPreferences = NotificationPreferences(),
            availablePlants = listOf("Pune Plant 1", "Chakan Assembly", "Plant 01 Production"),
        ),
    )

    fun currentItems(): List<Item> = itemsMap.values.toList()

    override fun observeUserSettings(): Flow<UserSettings> = userSettingsFlow.asStateFlow()

    override suspend fun selectPlant(plantName: String): DataResult<Unit> {
        val clean = plantName.trim()
        val plantId = "plant_" + clean.lowercase().replace("[^a-z0-9]".toRegex(), "_")
        userSettingsFlow.update { current ->
            current.copy(
                profile = current.profile.copy(
                    plantName = clean,
                    plantId = plantId,
                ),
            )
        }
        return DataResult.Success(Unit)
    }

    override suspend fun updateCurrency(currency: Currency) {
        userSettingsFlow.update { it.copy(currency = currency) }
    }

    override suspend fun updateUserProfile(displayName: String): DataResult<Unit> {
        userSettingsFlow.update { it.copy(profile = it.profile.copy(displayName = displayName)) }
        return DataResult.Success(Unit)
    }

    override suspend fun updateFullUserProfile(
        displayName: String,
        email: String,
        phone: String,
        company: String,
        plantName: String,
    ): DataResult<Unit> {
        userSettingsFlow.update {
            it.copy(
                profile = it.profile.copy(
                    displayName = displayName,
                    email = email,
                    phone = phone,
                    company = company,
                    plantName = plantName,
                ),
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

    override suspend fun updateNotificationPreferences(preferences: NotificationPreferences) {
        userSettingsFlow.update { it.copy(notificationPreferences = preferences) }
    }

    override fun observeItems(): Flow<List<Item>> = itemsFlow.asStateFlow()

    override fun searchItems(query: String): Flow<List<Item>> = itemsFlow.map { list ->
        val q = query.trim().lowercase()
        if (q.isBlank()) list
        else list.filter {
            it.name.lowercase().contains(q) ||
                it.code.lowercase().contains(q) ||
                it.categoryName.lowercase().contains(q) ||
                it.locationCode.lowercase().contains(q)
        }
    }

    override fun observeSummary(): Flow<InventorySummary> = itemsFlow.map { list ->
        InventorySummary(
            totalItems = list.size,
            lowStockCount = list.count { it.onHand.milli <= it.reorderLevel.milli },
            outOfStockCount = list.count { it.onHand.milli <= 0L },
            reservedCount = 0,
            rawMaterialsCount = list.count { it.itemClass == ItemClass.RAW_MATERIAL },
            finishedGoodsCount = list.count { it.itemClass == ItemClass.FINISHED_GOOD },
            totalValuationPaise = list.sumOf { (it.unitCost.paise * it.onHand.milli) / 1000L },
        )
    }

    override fun observeRecentMovements(limit: Int): Flow<List<StockMovement>> = flowOf(emptyList())
    override fun observeLedger(itemId: String): Flow<List<StockMovement>> = flowOf(emptyList())
    override fun observeItem(itemId: String): Flow<Item?> = flowOf(itemsMap[itemId])
    override suspend fun getItem(itemId: String): DataResult<Item> =
        itemsMap[itemId]?.let { DataResult.Success(it) } ?: DataResult.Error(com.company.stockpro.core.domain.util.UiText.Dynamic("Not found"))
    override suspend fun getItemByCode(code: String): DataResult<Item> =
        itemsMap.values.firstOrNull { it.code == code }?.let { DataResult.Success(it) } ?: DataResult.Error(com.company.stockpro.core.domain.util.UiText.Dynamic("Not found"))
    override suspend fun createItem(item: Item): DataResult<Item> {
        itemsMap[item.id] = item
        itemsFlow.value = itemsMap.values.toList()
        return DataResult.Success(item)
    }
    override suspend fun postMovement(movement: StockMovement): DataResult<StockMovement> = DataResult.Success(movement)
    override fun observeSyncStatus(): Flow<SyncStatus> = flowOf(SyncStatus.Synced(System.currentTimeMillis()))
    override suspend fun clearCache(): DataResult<Long> = DataResult.Success(0L)
    override suspend fun logout(): DataResult<Unit> = DataResult.Success(Unit)
    override suspend fun deleteAccount(): DataResult<Unit> = DataResult.Success(Unit)
    override fun observeItemsByClass(itemClass: ItemClass, sortOrder: com.company.stockpro.core.domain.model.SortOrder) = itemsFlow
    override fun observeTopLowStock(limit: Int) = itemsFlow.map { list -> list.filter { it.onHand.milli <= it.reorderLevel.milli }.take(limit) }
    override fun observePendingCount() = flowOf(0)
    override fun observeFailedEntries() = flowOf(emptyList<com.company.stockpro.core.domain.model.FailedOutboxEntry>())
    override suspend fun retryFailedOutbox(id: Long) {}
    override suspend fun discardFailedOutbox(id: Long) {}
    override fun observeNotifications() = flowOf(emptyList<com.company.stockpro.core.domain.model.AppNotification>())
    override fun observeUnreadNotificationCount() = flowOf(0)
    override suspend fun markNotificationAsRead(id: String) {}
    override suspend fun markAllNotificationsAsRead() {}
    override suspend fun clearNotifications() {}
    override fun observeNotificationPreferences() = userSettingsFlow.asStateFlow().let { flowOf(NotificationPreferences()) }
    override fun observeTeamMembers() = flowOf(emptyList<com.company.stockpro.core.domain.model.TeamMember>())
    override suspend fun inviteMember(email: String, displayName: String, role: Role, plantId: String) =
        DataResult.Success(com.company.stockpro.core.domain.model.TeamMember("m1", displayName, email, role, plantId))
    override suspend fun updateMemberRole(memberId: String, newRole: Role) = DataResult.Success(Unit)
    override suspend fun deactivateMember(memberId: String) = DataResult.Success(Unit)
    override fun observeToolCustody(itemId: String) = emptyFlow<com.company.stockpro.core.domain.model.ToolCustody?>()
    override suspend fun issueTool(itemId: String, operatorName: String, notes: String?) = DataResult.Success(Unit)
    override suspend fun returnTool(itemId: String, notes: String?) = DataResult.Success(Unit)
    override suspend fun logCalibration(itemId: String, calibratedAt: Long, nextDueAt: Long, notes: String?) = DataResult.Success(Unit)
}
