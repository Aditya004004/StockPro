package com.company.stockpro.feature.settings

import com.company.stockpro.core.domain.model.AppLanguage
import com.company.stockpro.core.domain.model.AppNotification
import com.company.stockpro.core.domain.model.Currency
import com.company.stockpro.core.domain.model.ExportConfig
import com.company.stockpro.core.domain.model.ExportDataset
import com.company.stockpro.core.domain.model.ExportFormat
import com.company.stockpro.core.domain.model.ExportHistoryRecord
import com.company.stockpro.core.domain.model.ExportSchedule
import com.company.stockpro.core.domain.model.Item
import com.company.stockpro.core.domain.model.NotificationPreferences
import com.company.stockpro.core.domain.model.Role
import com.company.stockpro.core.domain.model.StockMovement
import com.company.stockpro.core.domain.model.SyncStatus
import com.company.stockpro.core.domain.model.ThemeMode
import com.company.stockpro.core.domain.model.UnitSystem
import com.company.stockpro.core.domain.model.UserProfile
import com.company.stockpro.core.domain.model.UserSettings
import com.company.stockpro.core.domain.repository.ExportHistoryRepository
import com.company.stockpro.core.domain.repository.ExportManager
import com.company.stockpro.core.domain.repository.InventoryRepository
import com.company.stockpro.core.domain.util.DataResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class ExportDatabaseViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeInventoryRepo: FakeInventoryRepo
    private lateinit var fakeExportManager: FakeExportManager
    private lateinit var fakeHistoryRepo: FakeExportHistoryRepo
    private lateinit var viewModel: ExportDatabaseViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeInventoryRepo = FakeInventoryRepo()
        fakeHistoryRepo = FakeExportHistoryRepo()
        fakeExportManager = FakeExportManager(fakeHistoryRepo)

        viewModel = ExportDatabaseViewModel(
            inventoryRepository = fakeInventoryRepo,
            exportManager = fakeExportManager,
            historyRepository = fakeHistoryRepo,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loads default values and user settings`() = runTest {
        backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(ExportTab.QUICK_EXPORT, state.selectedTab)
        assertEquals("Aditya Maurya", state.userSettings.profile.displayName)
        assertEquals(Role.INVENTORY_MANAGER, state.userSettings.profile.role)
        assertEquals(ExportDataset.MASTER_INVENTORY, state.selectedDataset)
        assertEquals(ExportFormat.CSV, state.selectedFormat)
    }

    @Test
    fun `selectTab updates current tab`() = runTest {
        backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.selectTab(ExportTab.ADVANCED_EXPORT)
        advanceUntilIdle()
        assertEquals(ExportTab.ADVANCED_EXPORT, viewModel.uiState.value.selectedTab)

        viewModel.selectTab(ExportTab.HISTORY)
        advanceUntilIdle()
        assertEquals(ExportTab.HISTORY, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun `quickExport executes export and sets lastExportedRecord`() = runTest {
        backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.quickExport(ExportDataset.MASTER_INVENTORY, ExportFormat.PDF)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.lastExportedRecord)
        assertEquals(ExportFormat.PDF, state.lastExportedRecord?.format)
        assertEquals(ExportDataset.MASTER_INVENTORY, state.lastExportedRecord?.dataset)
        assertTrue(state.statusMessage?.contains("Generated") == true)
    }

    @Test
    fun `advanced export configuration updates options and executes`() = runTest {
        backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.setDataset(ExportDataset.STOCK_LEDGER)
        viewModel.setFormat(ExportFormat.XML)
        viewModel.setDatePreset(DatePreset.LAST_30_DAYS)
        viewModel.setIncludeCharts(false)
        advanceUntilIdle()

        assertEquals(ExportDataset.STOCK_LEDGER, viewModel.uiState.value.selectedDataset)
        assertEquals(ExportFormat.XML, viewModel.uiState.value.selectedFormat)
        assertEquals(DatePreset.LAST_30_DAYS, viewModel.uiState.value.datePreset)
        assertEquals(false, viewModel.uiState.value.includeCharts)

        viewModel.executeAdvancedExport()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.lastExportedRecord)
        assertEquals(ExportDataset.STOCK_LEDGER, state.lastExportedRecord?.dataset)
        assertEquals(ExportFormat.XML, state.lastExportedRecord?.format)
    }

    @Test
    fun `history deletion and clearing works properly`() = runTest {
        backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        // Generate an export
        viewModel.quickExport(ExportDataset.MASTER_INVENTORY, ExportFormat.CSV)
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.historyRecords.size)
        val recordId = viewModel.uiState.value.historyRecords[0].id

        // Clear all history
        viewModel.clearAllHistory()
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.historyRecords.size)
        assertNull(viewModel.uiState.value.lastExportedRecord)
    }
}

private class FakeExportManager(
    private val historyRepo: FakeExportHistoryRepo,
) : ExportManager {
    val cancelledScheduleIds = mutableListOf<String>()
    var lastSharedRecord: ExportHistoryRecord? = null

    override suspend fun executeExport(config: ExportConfig): DataResult<ExportHistoryRecord> {
        val record = ExportHistoryRecord(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            dataset = config.dataset,
            format = config.format,
            recordCount = 42,
            fileSizeBytes = 1024L,
            filePath = "/fake/path/file.${config.format.extension}",
            fileName = "StockPro_${config.dataset.name}_test.${config.format.extension}",
            sha256Checksum = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            initiatedBy = config.initiatedBy,
            plantName = config.plantName,
            status = "SUCCESS",
        )
        historyRepo.recordExport(record)
        return DataResult.Success(record)
    }

    override fun shareExport(record: ExportHistoryRecord) {
        lastSharedRecord = record
    }

    override suspend fun scheduleExport(schedule: ExportSchedule): DataResult<Unit> {
        historyRepo.saveSchedule(schedule)
        return DataResult.Success(Unit)
    }

    override suspend fun triggerScheduleNow(schedule: ExportSchedule): DataResult<Unit> = DataResult.Success(Unit)

    override suspend fun cancelSchedule(scheduleId: String): DataResult<Unit> {
        cancelledScheduleIds.add(scheduleId)
        historyRepo.toggleSchedule(scheduleId, false)
        return DataResult.Success(Unit)
    }
}

private class FakeExportHistoryRepo : ExportHistoryRepository {
    private val historyList = mutableListOf<ExportHistoryRecord>()
    private val historyFlow = MutableStateFlow<List<ExportHistoryRecord>>(emptyList())
    private val scheduleList = mutableListOf<ExportSchedule>()
    private val scheduleFlow = MutableStateFlow<List<ExportSchedule>>(emptyList())

    override fun observeExportHistory(): Flow<List<ExportHistoryRecord>> = historyFlow.asStateFlow()

    override suspend fun recordExport(record: ExportHistoryRecord): DataResult<Unit> {
        historyList.add(0, record)
        historyFlow.value = historyList.toList()
        return DataResult.Success(Unit)
    }

    override suspend fun deleteExportHistory(id: String): DataResult<Unit> {
        historyList.removeAll { it.id == id }
        historyFlow.value = historyList.toList()
        return DataResult.Success(Unit)
    }

    override suspend fun clearExportHistory(): DataResult<Unit> {
        historyList.clear()
        historyFlow.value = emptyList()
        return DataResult.Success(Unit)
    }

    override fun observeSchedules(): Flow<List<ExportSchedule>> = scheduleFlow.asStateFlow()

    override suspend fun saveSchedule(schedule: ExportSchedule): DataResult<Unit> {
        scheduleList.removeAll { it.id == schedule.id }
        scheduleList.add(schedule)
        scheduleFlow.value = scheduleList.toList()
        return DataResult.Success(Unit)
    }

    override suspend fun toggleSchedule(id: String, isEnabled: Boolean): DataResult<Unit> {
        val index = scheduleList.indexOfFirst { it.id == id }
        if (index != -1) {
            scheduleList[index] = scheduleList[index].copy(isEnabled = isEnabled)
            scheduleFlow.value = scheduleList.toList()
        }
        return DataResult.Success(Unit)
    }

    override suspend fun updateScheduleLastRun(id: String, timestamp: Long): DataResult<Unit> {
        val index = scheduleList.indexOfFirst { it.id == id }
        if (index != -1) {
            scheduleList[index] = scheduleList[index].copy(lastRunTimestamp = timestamp)
            scheduleFlow.value = scheduleList.toList()
        }
        return DataResult.Success(Unit)
    }

    override suspend fun deleteSchedule(id: String): DataResult<Unit> {
        scheduleList.removeAll { it.id == id }
        scheduleFlow.value = scheduleList.toList()
        return DataResult.Success(Unit)
    }
}

private class FakeInventoryRepo : InventoryRepository {
    private val userSettingsFlow = MutableStateFlow(
        UserSettings(
            profile = UserProfile(
                uid = "u1",
                displayName = "Aditya Maurya",
                email = "aditya@company.com",
                role = Role.INVENTORY_MANAGER,
                plantId = "plant_01",
                plantName = "Pune Plant 1",
            ),
            themeMode = ThemeMode.LIGHT,
            language = AppLanguage.ENGLISH,
            unitSystem = UnitSystem.METRIC,
            currency = Currency.INR,
        )
    )

    override fun observeItems(): Flow<List<Item>> = emptyFlow()
    override fun observeItemsByClass(itemClass: com.company.stockpro.core.domain.model.ItemClass, sortOrder: com.company.stockpro.core.domain.model.SortOrder) = emptyFlow<List<Item>>()
    override fun searchItems(query: String): Flow<List<Item>> = emptyFlow()
    override fun observeSummary() = emptyFlow<com.company.stockpro.core.domain.model.InventorySummary>()
    override fun observeTopLowStock(limit: Int) = emptyFlow<List<Item>>()
    override fun observeItem(itemId: String) = emptyFlow<Item?>()
    override fun observeLedger(itemId: String) = emptyFlow<List<StockMovement>>()
    override fun observeRecentMovements(limit: Int) = emptyFlow<List<StockMovement>>()
    override suspend fun getItem(itemId: String) = DataResult.Error(com.company.stockpro.core.domain.util.UiText.Dynamic("Not implemented"))
    override suspend fun getItemByCode(code: String) = DataResult.Error(com.company.stockpro.core.domain.util.UiText.Dynamic("Not implemented"))
    override suspend fun createItem(item: Item) = DataResult.Success(item)
    override suspend fun postMovement(movement: StockMovement) = DataResult.Success(movement)
    override fun observePendingCount(): Flow<Int> = emptyFlow()
    override fun observeSyncStatus(): Flow<SyncStatus> = emptyFlow()
    override fun observeFailedEntries() = emptyFlow<List<com.company.stockpro.core.domain.model.FailedOutboxEntry>>()
    override suspend fun retryFailedOutbox(id: Long) {}
    override suspend fun discardFailedOutbox(id: Long) {}
    override fun observeNotifications() = emptyFlow<List<AppNotification>>()
    override fun observeUnreadNotificationCount(): Flow<Int> = emptyFlow()
    override suspend fun markNotificationAsRead(id: String) {}
    override suspend fun markAllNotificationsAsRead() {}
    override suspend fun clearNotifications() {}
    override fun observeNotificationPreferences(): Flow<NotificationPreferences> = emptyFlow()
    override suspend fun updateNotificationPreferences(preferences: NotificationPreferences) {}
    override fun observeUserSettings(): Flow<UserSettings> = userSettingsFlow.asStateFlow()
    override suspend fun updateUserProfile(displayName: String) = DataResult.Success(Unit)
    override suspend fun updateThemeMode(themeMode: ThemeMode) {}
    override suspend fun updateLanguage(language: AppLanguage) {}
    override suspend fun updateUnitSystem(unitSystem: UnitSystem) {}
    override suspend fun clearCache() = DataResult.Success(0L)
    override suspend fun logout() = DataResult.Success(Unit)
    override suspend fun deleteAccount() = DataResult.Success(Unit)
    override fun observeTeamMembers() = emptyFlow<List<com.company.stockpro.core.domain.model.TeamMember>>()
    override suspend fun inviteMember(email: String, displayName: String, role: Role, plantId: String) = DataResult.Error(com.company.stockpro.core.domain.util.UiText.Dynamic("Not implemented"))
    override suspend fun updateMemberRole(memberId: String, newRole: Role) = DataResult.Success(Unit)
    override suspend fun deactivateMember(memberId: String) = DataResult.Success(Unit)
    override fun observeToolCustody(itemId: String) = emptyFlow<com.company.stockpro.core.domain.model.ToolCustody?>()
    override suspend fun issueTool(itemId: String, operatorName: String, notes: String?) = DataResult.Success(Unit)
    override suspend fun returnTool(itemId: String, notes: String?) = DataResult.Success(Unit)
    override suspend fun logCalibration(itemId: String, calibratedAt: Long, nextDueAt: Long, notes: String?) = DataResult.Success(Unit)
}
