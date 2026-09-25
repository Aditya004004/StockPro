package com.company.stockpro.feature.home

import com.company.stockpro.core.domain.model.InventorySummary
import com.company.stockpro.core.domain.model.Item
import com.company.stockpro.core.domain.model.ItemClass
import com.company.stockpro.core.domain.model.Money
import com.company.stockpro.core.domain.model.MovementType
import com.company.stockpro.core.domain.model.Quantity
import com.company.stockpro.core.domain.model.SortOrder
import com.company.stockpro.core.domain.model.StockMovement
import com.company.stockpro.core.domain.model.SyncStatus
import com.company.stockpro.core.domain.repository.InventoryRepository
import com.company.stockpro.core.domain.util.DataResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeHomeRepository
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeHomeRepository()
        viewModel = HomeViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loads active summary, items, and activities`() = runTest {
        backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Content state, got: $state", state is HomeUiState.Content)
        val content = state as HomeUiState.Content
        assertEquals(3, content.totalItems)
        assertEquals(1, content.rawMaterialsCount)
        assertEquals(1, content.lowStockCount)
        assertEquals(1, content.upcomingReordersCount)
        assertEquals(0, content.totalItemsDeltaPercent)
        assertEquals("User", content.userName)
        assertTrue(content.isValuationVisible)
        assertEquals("Plant 1", content.activePlant)
    }

    @Test
    fun `toggleValuationVisibility switches privacy state`() = runTest {
        backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        assertTrue((viewModel.uiState.value as HomeUiState.Content).isValuationVisible)

        viewModel.toggleValuationVisibility()
        advanceUntilIdle()
        assertFalse((viewModel.uiState.value as HomeUiState.Content).isValuationVisible)

        viewModel.toggleValuationVisibility()
        advanceUntilIdle()
        assertTrue((viewModel.uiState.value as HomeUiState.Content).isValuationVisible)
    }

    @Test
    fun `selectPlant updates active plant in state`() = runTest {
        backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.selectPlant("Plant 2")
        advanceUntilIdle()

        val state = viewModel.uiState.value as HomeUiState.Content
        assertEquals("Plant 2", state.activePlant)
    }
}

private class FakeHomeRepository : InventoryRepository {
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

    override fun observeItems(): Flow<List<Item>> = flowOf(items)

    override fun observeItemsByClass(itemClass: ItemClass, sortOrder: SortOrder): Flow<List<Item>> =
        flowOf(items.filter { it.itemClass == itemClass })

    override fun searchItems(query: String): Flow<List<Item>> = flowOf(emptyList())

    override fun observeSummary(): Flow<InventorySummary> =
        flowOf(InventorySummary(totalItems = items.size, lowStockCount = 1, outOfStockCount = 0, reservedCount = 0))

    override fun observeItem(itemId: String): Flow<Item?> = flowOf(items.find { it.id == itemId })

    override fun observeLedger(itemId: String): Flow<List<StockMovement>> = flowOf(emptyList())

    override fun observeRecentMovements(limit: Int): Flow<List<StockMovement>> = flowOf(emptyList())

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
    override fun observeUserSettings(): Flow<com.company.stockpro.core.domain.model.UserSettings> = flowOf(
        com.company.stockpro.core.domain.model.UserSettings(
            profile = com.company.stockpro.core.domain.model.UserProfile("u1", "User", "u@co.com", com.company.stockpro.core.domain.model.Role.INVENTORY_MANAGER, "p1", "Plant 1"),
        ),
    )
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

