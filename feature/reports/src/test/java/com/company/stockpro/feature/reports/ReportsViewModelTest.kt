package com.company.stockpro.feature.reports

import com.company.stockpro.core.domain.model.Currency
import com.company.stockpro.core.domain.model.InventorySummary
import com.company.stockpro.core.domain.model.Item
import com.company.stockpro.core.domain.model.ItemClass
import com.company.stockpro.core.domain.model.Money
import com.company.stockpro.core.domain.model.MovementType
import com.company.stockpro.core.domain.model.Quantity
import com.company.stockpro.core.domain.model.SortOrder
import com.company.stockpro.core.domain.model.StockMovement
import com.company.stockpro.core.domain.model.SyncStatus
import com.company.stockpro.core.domain.model.UserProfile
import com.company.stockpro.core.domain.model.UserSettings
import com.company.stockpro.core.domain.repository.InventoryRepository
import com.company.stockpro.core.domain.util.DataResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeReportsRepository
    private lateinit var viewModel: ReportsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeReportsRepository()
        viewModel = ReportsViewModel(fakeRepository).apply {
            defaultDispatcher = testDispatcher
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loads period metrics, charts, and low stock items`() = runTest {
        backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Content state, got: $state", state is ReportsUiState.Content)
        val content = state as ReportsUiState.Content
        assertEquals(ReportSection.OVERVIEW, content.selectedSection)
        assertEquals(DateRangePreset.CURRENT_MONTH, content.selectedDateRange)
        assertEquals(3, content.totalItems)
        assertEquals(1, content.lowStockItems.size)
        assertEquals(2, content.trendSeries.size)
        assertEquals(Currency.INR, content.currency)
    }

    @Test
    fun `onSectionSelected updates active section`() = runTest {
        backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.onSectionSelected(ReportSection.PURCHASES)
        advanceUntilIdle()

        val state = viewModel.uiState.value as ReportsUiState.Content
        assertEquals(ReportSection.PURCHASES, state.selectedSection)
    }

    @Test
    fun `onDateRangeSelected updates active date range`() = runTest {
        backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.onDateRangeSelected(DateRangePreset.LAST_30_DAYS)
        advanceUntilIdle()

        val state = viewModel.uiState.value as ReportsUiState.Content
        assertEquals(DateRangePreset.LAST_30_DAYS, state.selectedDateRange)
    }

    @Test
    fun `exportReportCsv builds valid CSV formatted content with global currency`() = runTest {
        backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.exportReportCsv("Inventory Report", fakeRepository.items, Currency.INR)
        advanceUntilIdle()

        val state = viewModel.uiState.value as ReportsUiState.Content
        assertNotNull(state.generatedCsvContent)
        val csv = state.generatedCsvContent!!
        assertTrue(csv.contains("Code,Name,Class,Category,Location,Unit,On Hand,Reserved,Reorder Level,Unit Cost (₹),Total Value (₹)"))
        assertTrue(csv.contains("BRG-001"))
        assertTrue(csv.contains("Ball Bearing"))
    }

    @Test
    fun `exportReportCsv with USD uses dollar symbol in headers`() = runTest {
        backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.exportReportCsv("USD Report", fakeRepository.items, Currency.USD)
        advanceUntilIdle()

        val state = viewModel.uiState.value as ReportsUiState.Content
        val csv = state.generatedCsvContent!!
        assertTrue(csv.contains("Unit Cost ($),Total Value ($)"))
    }

    @Test
    fun `currency update reflects in reports state`() = runTest {
        backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        fakeRepository.updateCurrency(Currency.USD)
        advanceUntilIdle()

        val state = viewModel.uiState.value as ReportsUiState.Content
        assertEquals(Currency.USD, state.currency)
    }

    @Test
    fun `month navigation updates selectedMonthLabel and state`() = runTest {
        backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        val initialMonthLabel = (viewModel.uiState.value as ReportsUiState.Content).selectedMonthLabel
        viewModel.onPreviousMonth()
        advanceUntilIdle()

        val prevMonthLabel = (viewModel.uiState.value as ReportsUiState.Content).selectedMonthLabel
        assertNotEquals(initialMonthLabel, prevMonthLabel)

        viewModel.onNextMonth()
        advanceUntilIdle()
        val restoredMonthLabel = (viewModel.uiState.value as ReportsUiState.Content).selectedMonthLabel
        assertEquals(initialMonthLabel, restoredMonthLabel)
    }

    @Test
    fun `valuation by category groups dynamic items correctly`() = runTest {
        backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        val state = viewModel.uiState.value as ReportsUiState.Content
        val valuations = state.categoryValuations
        assertTrue(valuations.isNotEmpty())
        val categories = valuations.map { it.categoryName }
        assertTrue(categories.contains("Mechanical"))
        assertTrue(categories.contains("Metals"))
    }

    @Test
    fun `inbound and outbound flow computes live series from movements`() = runTest {
        backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        val state = viewModel.uiState.value as ReportsUiState.Content
        assertEquals(2, state.trendSeries.size)
        val inbound = state.trendSeries.find { it.name == "Inbound" }
        val outbound = state.trendSeries.find { it.name == "Outbound" }
        assertNotNull(inbound)
        assertNotNull(outbound)
        assertTrue(inbound!!.dataPoints.sum() > 0f)
        assertTrue(outbound!!.dataPoints.sum() > 0f)
    }

    @Test
    fun `selecting plant updates active plant in reports`() = runTest {
        backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.selectPlant("Plant 2")
        advanceUntilIdle()

        val state = viewModel.uiState.value as ReportsUiState.Content
        assertEquals("Plant 2", state.activePlant)
    }
}

private class FakeReportsRepository : InventoryRepository {
    val items = listOf(
        Item(
            id = "item_1",
            code = "BRG-001",
            name = "Ball Bearing",
            itemClass = ItemClass.COMPONENT,
            categoryName = "Mechanical",
            unitOfMeasure = "PCS",
            onHand = Quantity(320_000L),
            reserved = Quantity(0L),
            reorderLevel = Quantity(50_000L),
            parLevel = Quantity(500_000L),
            unitCost = Money(45_000L),
            locationCode = "A1-01",
        ),
        Item(
            id = "item_2",
            code = "GH-002",
            name = "Gear Housing",
            itemClass = ItemClass.COMPONENT,
            categoryName = "Mechanical",
            unitOfMeasure = "PCS",
            onHand = Quantity(8_000L),
            reserved = Quantity(0L),
            reorderLevel = Quantity(50_000L),
            parLevel = Quantity(120_000L),
            unitCost = Money(150_000L),
            locationCode = "A1-02",
        ),
        Item(
            id = "item_3",
            code = "STL-003",
            name = "Steel Rod",
            itemClass = ItemClass.RAW_MATERIAL,
            categoryName = "Metals",
            unitOfMeasure = "KG",
            onHand = Quantity(100_000L),
            reserved = Quantity(0L),
            reorderLevel = Quantity(20_000L),
            parLevel = Quantity(200_000L),
            unitCost = Money(80_000L),
            locationCode = "B1-01",
        ),
    )

    val movements = listOf(
        StockMovement(
            id = "m1",
            itemId = "item_1",
            type = MovementType.RECEIPT,
            delta = Quantity(50_000L),
            createdAt = System.currentTimeMillis() - 1000L,
        ),
        StockMovement(
            id = "m2",
            itemId = "item_2",
            type = MovementType.ISSUE,
            delta = Quantity(-20_000L),
            createdAt = System.currentTimeMillis() - 2000L,
        ),
    )

    val userSettingsFlow = MutableStateFlow(
        UserSettings(
            profile = UserProfile(
                uid = "u1",
                displayName = "User",
                email = "u@co.com",
                role = com.company.stockpro.core.domain.model.Role.INVENTORY_MANAGER,
                plantId = "p1",
                plantName = "Plant 1",
                warehouseName = "Main Warehouse",
            ),
            availablePlants = listOf("Plant 1", "Plant 2"),
            currency = Currency.INR,
        ),
    )

    override fun observeItems(): Flow<List<Item>> = flowOf(items)

    override fun observeItemsByClass(itemClass: ItemClass, sortOrder: SortOrder): Flow<List<Item>> =
        flowOf(items.filter { it.itemClass == itemClass })

    override fun searchItems(query: String): Flow<List<Item>> = flowOf(emptyList())

    override fun observeSummary(): Flow<InventorySummary> =
        flowOf(InventorySummary(totalItems = items.size, lowStockCount = 1, outOfStockCount = 0, reservedCount = 0))

    override fun observeTopLowStock(limit: Int): Flow<List<Item>> =
        flowOf(items.filter { it.onHand.milli <= it.reorderLevel.milli }.sortedBy { it.onHand.milli }.take(limit))

    override suspend fun logout(): DataResult<Unit> = DataResult.Success(Unit)
    override suspend fun deleteAccount(): DataResult<Unit> = DataResult.Success(Unit)

    override fun observeItem(itemId: String): Flow<Item?> = flowOf(items.find { it.id == itemId })

    override fun observeLedger(itemId: String): Flow<List<StockMovement>> = flowOf(emptyList())

    override fun observeRecentMovements(limit: Int): Flow<List<StockMovement>> = flowOf(movements)

    override suspend fun getItem(itemId: String): DataResult<Item> =
        items.find { it.id == itemId }?.let { DataResult.Success(it) }
            ?: DataResult.Error(com.company.stockpro.core.domain.util.UiText.Dynamic("Not found"))

    override suspend fun getItemByCode(code: String): DataResult<Item> =
        items.find { it.code.equals(code, ignoreCase = true) }?.let { DataResult.Success(it) }
            ?: DataResult.Error(com.company.stockpro.core.domain.util.UiText.Dynamic("No item matches this code."))

    override suspend fun createItem(item: Item): DataResult<Item> = DataResult.Success(item)

    override suspend fun postMovement(movement: StockMovement): DataResult<StockMovement> =
        DataResult.Success(movement)

    override fun observePendingCount(): Flow<Int> = flowOf(0)

    override fun observeSyncStatus(): Flow<SyncStatus> = flowOf(SyncStatus.Synced(System.currentTimeMillis()))

    override fun observeFailedEntries(): Flow<List<com.company.stockpro.core.domain.model.FailedOutboxEntry>> =
        flowOf(emptyList())

    override suspend fun retryFailedOutbox(id: Long) {}

    override suspend fun discardFailedOutbox(id: Long) {}

    override fun observeNotifications(): Flow<List<com.company.stockpro.core.domain.model.AppNotification>> = flowOf(emptyList())
    override fun observeUnreadNotificationCount(): Flow<Int> = flowOf(0)
    override suspend fun markNotificationAsRead(id: String) {}
    override suspend fun markAllNotificationsAsRead() {}
    override suspend fun clearNotifications() {}
    override fun observeNotificationPreferences(): Flow<com.company.stockpro.core.domain.model.NotificationPreferences> = flowOf(com.company.stockpro.core.domain.model.NotificationPreferences())
    override suspend fun updateNotificationPreferences(preferences: com.company.stockpro.core.domain.model.NotificationPreferences) {}
    override fun observeUserSettings(): Flow<UserSettings> = userSettingsFlow
    override suspend fun selectPlant(plantName: String): DataResult<Unit> {
        userSettingsFlow.value = userSettingsFlow.value.copy(
            profile = userSettingsFlow.value.profile.copy(plantName = plantName),
        )
        return DataResult.Success(Unit)
    }
    override suspend fun updateCurrency(currency: Currency) {
        userSettingsFlow.value = userSettingsFlow.value.copy(currency = currency)
    }
    override suspend fun updateUserProfile(displayName: String): DataResult<Unit> = DataResult.Success(Unit)
    override suspend fun updateThemeMode(themeMode: com.company.stockpro.core.domain.model.ThemeMode) {}
    override suspend fun updateLanguage(language: com.company.stockpro.core.domain.model.AppLanguage) {}
    override suspend fun updateUnitSystem(unitSystem: com.company.stockpro.core.domain.model.UnitSystem) {}
    override suspend fun clearCache(): DataResult<Long> = DataResult.Success(0L)
    override fun observeTeamMembers(): Flow<List<com.company.stockpro.core.domain.model.TeamMember>> = flowOf(emptyList())
    override suspend fun inviteMember(email: String, displayName: String, role: com.company.stockpro.core.domain.model.Role, plantId: String): DataResult<com.company.stockpro.core.domain.model.TeamMember> =
        DataResult.Success(com.company.stockpro.core.domain.model.TeamMember("m1", displayName, email, role, plantId))
    override suspend fun updateMemberRole(memberId: String, newRole: com.company.stockpro.core.domain.model.Role): DataResult<Unit> = DataResult.Success(Unit)
    override suspend fun deactivateMember(memberId: String): DataResult<Unit> = DataResult.Success(Unit)
    override fun observeToolCustody(itemId: String): Flow<com.company.stockpro.core.domain.model.ToolCustody?> = flowOf(null)
    override suspend fun issueTool(itemId: String, operatorName: String, notes: String?): DataResult<Unit> = DataResult.Success(Unit)
    override suspend fun returnTool(itemId: String, notes: String?): DataResult<Unit> = DataResult.Success(Unit)
    override suspend fun logCalibration(itemId: String, calibratedAt: Long, nextDueAt: Long, notes: String?): DataResult<Unit> = DataResult.Success(Unit)
}
