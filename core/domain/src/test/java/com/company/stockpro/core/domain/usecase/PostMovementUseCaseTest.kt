package com.company.stockpro.core.domain.usecase

import com.company.stockpro.core.domain.model.InventorySummary
import com.company.stockpro.core.domain.model.Item
import com.company.stockpro.core.domain.model.ItemClass
import com.company.stockpro.core.domain.model.Money
import com.company.stockpro.core.domain.model.MovementType
import com.company.stockpro.core.domain.model.Quantity
import com.company.stockpro.core.domain.model.SortOrder
import com.company.stockpro.core.domain.model.StockMovement
import com.company.stockpro.core.domain.repository.InventoryRepository
import com.company.stockpro.core.domain.util.DataResult
import com.company.stockpro.core.domain.util.UiText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for [PostMovementUseCase] covering every validation branch:
 * - Zero/negative quantity
 * - ISSUE with insufficient stock
 * - ISSUE with sufficient stock
 * - ADJUSTMENT without reason code
 * - SCRAP without reason code
 * - Successful receipt
 * - Idempotency key generation
 * - Repository error propagation
 */
class PostMovementUseCaseTest {

    private lateinit var fakeRepo: FakeInventoryRepository
    private lateinit var useCase: PostMovementUseCase

    @Before
    fun setup() {
        fakeRepo = FakeInventoryRepository()
        useCase = PostMovementUseCase(fakeRepo)
    }

    @Test
    fun `rejects zero quantity`() = runBlocking {
        val result = useCase(movement(delta = Quantity(0L)))
        assertTrue(result is DataResult.Error)
    }

    @Test
    fun `rejects negative quantity`() = runBlocking {
        val result = useCase(movement(delta = Quantity(-1_000L)))
        assertTrue(result is DataResult.Error)
    }

    @Test
    fun `rejects ISSUE when insufficient stock`() = runBlocking {
        fakeRepo.itemToReturn = testItem(onHand = Quantity(5_000L), reserved = Quantity(2_000L))
        val result = useCase(movement(type = MovementType.ISSUE, delta = Quantity(4_000L)))
        assertTrue(result is DataResult.Error)
        assertTrue((result as DataResult.Error).message is UiText.Dynamic)
    }

    @Test
    fun `accepts ISSUE when sufficient stock`() = runBlocking {
        fakeRepo.itemToReturn = testItem(onHand = Quantity(100_000L), reserved = Quantity.ZERO)
        val result = useCase(movement(type = MovementType.ISSUE, delta = Quantity(50_000L)))
        assertTrue(result is DataResult.Success)
    }

    @Test
    fun `rejects TRANSFER_OUT when insufficient stock`() = runBlocking {
        fakeRepo.itemToReturn = testItem(onHand = Quantity(5_000L), reserved = Quantity.ZERO)
        val result = useCase(movement(type = MovementType.TRANSFER_OUT, delta = Quantity(10_000L)))
        assertTrue(result is DataResult.Error)
    }

    @Test
    fun `rejects ADJUSTMENT without reason code`() = runBlocking {
        val result = useCase(movement(type = MovementType.ADJUSTMENT, reasonCode = null))
        assertTrue(result is DataResult.Error)
    }

    @Test
    fun `rejects ADJUSTMENT with blank reason code`() = runBlocking {
        val result = useCase(movement(type = MovementType.ADJUSTMENT, reasonCode = "   "))
        assertTrue(result is DataResult.Error)
    }

    @Test
    fun `rejects SCRAP without reason code`() = runBlocking {
        val result = useCase(movement(type = MovementType.SCRAP, reasonCode = null))
        assertTrue(result is DataResult.Error)
    }

    @Test
    fun `accepts ADJUSTMENT with reason code`() = runBlocking {
        val result = useCase(movement(type = MovementType.ADJUSTMENT, reasonCode = "CYCLE_COUNT"))
        assertTrue(result is DataResult.Success)
    }

    @Test
    fun `accepts signed downward ADJUSTMENT with negative delta`() = runBlocking {
        val result = useCase(movement(type = MovementType.ADJUSTMENT, delta = Quantity(-5_000L), reasonCode = "COUNT_RECONCILIATION"))
        assertTrue(result is DataResult.Success)
    }

    @Test
    fun `successful RECEIPT generates idempotency key`() = runBlocking {
        val result = useCase(movement(type = MovementType.RECEIPT))
        assertTrue(result is DataResult.Success)
        val posted = (result as DataResult.Success).data
        assertNotNull(posted.idempotencyKey)
        assertTrue(posted.idempotencyKey.isNotBlank())
        assertTrue(posted.isPending)
    }

    @Test
    fun `propagates repository error on getItem failure`() = runBlocking {
        fakeRepo.getItemError = DataResult.Error(UiText.Dynamic("DB error"))
        val result = useCase(movement(type = MovementType.ISSUE, delta = Quantity(1_000L)))
        assertTrue(result is DataResult.Error)
        assertEquals("DB error", ((result as DataResult.Error).message as UiText.Dynamic).value)
    }

    // ── Helpers ──

    private fun movement(
        type: MovementType = MovementType.RECEIPT,
        delta: Quantity = Quantity(10_000L),
        reasonCode: String? = null,
    ) = StockMovement(
        itemId = "item-1",
        type = type,
        delta = delta,
        reasonCode = reasonCode,
    )

    private fun testItem(
        onHand: Quantity = Quantity(100_000L),
        reserved: Quantity = Quantity.ZERO,
    ) = Item(
        id = "item-1",
        code = "BRG-001",
        name = "Ball Bearing",
        itemClass = ItemClass.COMPONENT,
        categoryName = "Bearings",
        unitOfMeasure = "PCS",
        onHand = onHand,
        reserved = reserved,
        reorderLevel = Quantity(50_000L),
        parLevel = Quantity(500_000L),
        unitCost = Money(45_000L),
        locationCode = "A1-01",
    )
}

/**
 * Fake repository for testing the use case in isolation.
 * Uses fakes, not mocks (rules.md §8).
 */
private class FakeInventoryRepository : InventoryRepository {

    var itemToReturn: Item? = null
    var getItemError: DataResult.Error? = null
    var lastPostedMovement: StockMovement? = null

    override fun observeItems(): Flow<List<Item>> = emptyFlow()
    override fun observeItemsByClass(itemClass: ItemClass, sortOrder: SortOrder): Flow<List<Item>> = emptyFlow()
    override fun observeSummary(): Flow<InventorySummary> = emptyFlow()
    override fun observeItem(itemId: String): Flow<Item?> = emptyFlow()
    override fun searchItems(query: String): Flow<List<Item>> = emptyFlow()
    override fun observeLedger(itemId: String): Flow<List<StockMovement>> = emptyFlow()
    override fun observeRecentMovements(limit: Int): Flow<List<StockMovement>> = emptyFlow()
    override fun observePendingCount(): Flow<Int> = emptyFlow()
    override fun observeSyncStatus(): Flow<com.company.stockpro.core.domain.model.SyncStatus> = emptyFlow()
    override fun observeFailedEntries(): Flow<List<com.company.stockpro.core.domain.model.FailedOutboxEntry>> = emptyFlow()
    override suspend fun retryFailedOutbox(id: Long) {}
    override suspend fun discardFailedOutbox(id: Long) {}

    override suspend fun getItem(itemId: String): DataResult<Item> {
        getItemError?.let { return it }
        val item = itemToReturn ?: return DataResult.Error(UiText.Dynamic("Item not found."))
        return DataResult.Success(item)
    }

    override suspend fun getItemByCode(code: String): DataResult<Item> {
        getItemError?.let { return it }
        val item = itemToReturn ?: return DataResult.Error(UiText.Dynamic("Item not found."))
        return DataResult.Success(item)
    }

    override suspend fun createItem(item: Item): DataResult<Item> = DataResult.Success(item)

    override suspend fun postMovement(movement: StockMovement): DataResult<StockMovement> {
        lastPostedMovement = movement
        return DataResult.Success(movement.copy(id = "ledger-entry-1", isPending = true))
    }

    override fun observeNotifications(): Flow<List<com.company.stockpro.core.domain.model.AppNotification>> = emptyFlow()
    override fun observeUnreadNotificationCount(): Flow<Int> = emptyFlow()
    override suspend fun markNotificationAsRead(id: String) {}
    override suspend fun markAllNotificationsAsRead() {}
    override suspend fun clearNotifications() {}
    override fun observeNotificationPreferences(): Flow<com.company.stockpro.core.domain.model.NotificationPreferences> = emptyFlow()
    override suspend fun updateNotificationPreferences(preferences: com.company.stockpro.core.domain.model.NotificationPreferences) {}
    override fun observeUserSettings(): Flow<com.company.stockpro.core.domain.model.UserSettings> = emptyFlow()
    override suspend fun updateUserProfile(displayName: String): DataResult<Unit> = DataResult.Success(Unit)
    override suspend fun updateThemeMode(themeMode: com.company.stockpro.core.domain.model.ThemeMode) {}
    override suspend fun updateLanguage(language: com.company.stockpro.core.domain.model.AppLanguage) {}
    override suspend fun updateUnitSystem(unitSystem: com.company.stockpro.core.domain.model.UnitSystem) {}
    override suspend fun clearCache(): DataResult<Long> = DataResult.Success(0L)
    override fun observeTeamMembers(): Flow<List<com.company.stockpro.core.domain.model.TeamMember>> = emptyFlow()
    override suspend fun inviteMember(email: String, displayName: String, role: com.company.stockpro.core.domain.model.Role, plantId: String): DataResult<com.company.stockpro.core.domain.model.TeamMember> =
        DataResult.Success(com.company.stockpro.core.domain.model.TeamMember("m1", displayName, email, role, plantId))
    override suspend fun updateMemberRole(memberId: String, newRole: com.company.stockpro.core.domain.model.Role): DataResult<Unit> = DataResult.Success(Unit)
    override suspend fun deactivateMember(memberId: String): DataResult<Unit> = DataResult.Success(Unit)
    override fun observeToolCustody(itemId: String): Flow<com.company.stockpro.core.domain.model.ToolCustody?> = emptyFlow()
    override suspend fun issueTool(itemId: String, operatorName: String, notes: String?): DataResult<Unit> = DataResult.Success(Unit)
    override suspend fun returnTool(itemId: String, notes: String?): DataResult<Unit> = DataResult.Success(Unit)
    override suspend fun logCalibration(itemId: String, calibratedAt: Long, nextDueAt: Long, notes: String?): DataResult<Unit> = DataResult.Success(Unit)
}

