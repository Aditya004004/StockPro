package com.company.stockpro.feature.settings

import com.company.stockpro.core.domain.model.AppLanguage
import com.company.stockpro.core.domain.model.NotificationPreferences
import com.company.stockpro.core.domain.model.Role
import com.company.stockpro.core.domain.model.SyncStatus
import com.company.stockpro.core.domain.model.ThemeMode
import com.company.stockpro.core.domain.model.UnitSystem
import com.company.stockpro.core.domain.model.UserProfile
import com.company.stockpro.core.domain.model.UserSettings
import com.company.stockpro.core.domain.repository.InventoryRepository
import com.company.stockpro.core.domain.util.DataResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
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

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepo: FakeSettingsRepository
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeSettingsRepository()
        viewModel = SettingsViewModel(fakeRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state emits content with user profile and preferences`() = runTest {
        backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is SettingsUiState.Content)
        val content = state as SettingsUiState.Content
        assertEquals("Aditya Maurya", content.userSettings.profile.displayName)
        assertEquals("aditya@company.com", content.userSettings.profile.email)
        assertEquals(Role.INVENTORY_MANAGER, content.userSettings.profile.role)
        assertEquals(ThemeMode.LIGHT, content.userSettings.themeMode)
        assertEquals(AppLanguage.ENGLISH, content.userSettings.language)
        assertEquals(UnitSystem.METRIC, content.userSettings.unitSystem)
    }

    @Test
    fun `showDialog and dismissDialog update activeDialog state`() = runTest {
        backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.showDialog(SettingsDialog.EditProfile)
        advanceUntilIdle()
        assertEquals(SettingsDialog.EditProfile, (viewModel.uiState.value as SettingsUiState.Content).activeDialog)

        viewModel.dismissDialog()
        advanceUntilIdle()
        assertNull((viewModel.uiState.value as SettingsUiState.Content).activeDialog)
    }

    @Test
    fun `updateDisplayName updates profile name and dismisses dialog`() = runTest {
        backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.showDialog(SettingsDialog.EditProfile)
        viewModel.updateDisplayName("Vikramaditya")
        advanceUntilIdle()

        val content = viewModel.uiState.value as SettingsUiState.Content
        assertEquals("Vikramaditya", content.userSettings.profile.displayName)
        assertNull(content.activeDialog)
    }

    @Test
    fun `updateTheme, language, and unitSystem propagate to settings`() = runTest {
        backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.updateTheme(ThemeMode.DARK)
        viewModel.updateLanguage(AppLanguage.HINDI)
        viewModel.updateUnitSystem(UnitSystem.IMPERIAL)
        advanceUntilIdle()

        val content = viewModel.uiState.value as SettingsUiState.Content
        assertEquals(ThemeMode.DARK, content.userSettings.themeMode)
        assertEquals(AppLanguage.HINDI, content.userSettings.language)
        assertEquals(UnitSystem.IMPERIAL, content.userSettings.unitSystem)
    }

    @Test
    fun `updateNotificationPreferences toggles discrete alert preferences per PRD 8 2`() = runTest {
        backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        val updatedPrefs = NotificationPreferences(
            lowStockEnabled = false,
            outOfStockEnabled = true,
            poDueEnabled = false,
            largeAdjustmentEnabled = true,
            syncFailureEnabled = false,
        )

        viewModel.updateNotificationPreferences(updatedPrefs)
        advanceUntilIdle()

        val content = viewModel.uiState.value as SettingsUiState.Content
        assertEquals(false, content.userSettings.notificationPreferences.lowStockEnabled)
        assertEquals(true, content.userSettings.notificationPreferences.outOfStockEnabled)
        assertEquals(false, content.userSettings.notificationPreferences.poDueEnabled)
        assertEquals(true, content.userSettings.notificationPreferences.largeAdjustmentEnabled)
        assertEquals(false, content.userSettings.notificationPreferences.syncFailureEnabled)
    }

    @Test
    fun `clearCache resets cache size and dismisses dialog`() = runTest {
        backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.showDialog(SettingsDialog.ClearCacheConfirm(2560000L))
        viewModel.clearCache()
        advanceUntilIdle()

        val content = viewModel.uiState.value as SettingsUiState.Content
        assertEquals(0L, content.cacheSizeBytes)
        assertNull(content.activeDialog)
    }

    @Test
    fun `exportData populates exportedCsv content`() = runTest {
        backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.exportData()
        advanceUntilIdle()

        val content = viewModel.uiState.value as SettingsUiState.Content
        assertNotNull(content.exportedCsv)
        assertTrue(content.exportedCsv!!.contains("StockPro Master Data"))

        viewModel.clearExportedCsv()
        advanceUntilIdle()
        assertNull((viewModel.uiState.value as SettingsUiState.Content).exportedCsv)
    }
}

private class FakeSettingsRepository : InventoryRepository {

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
            notificationPreferences = NotificationPreferences(),
        ),
    )

    override fun observeUserSettings(): Flow<UserSettings> = userSettingsFlow.asStateFlow()

    override suspend fun updateUserProfile(displayName: String): DataResult<Unit> {
        userSettingsFlow.update { it.copy(profile = it.profile.copy(displayName = displayName)) }
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

    override suspend fun clearCache(): DataResult<Long> = DataResult.Success(0L)

    override fun observeSyncStatus(): Flow<SyncStatus> =
        flowOf(SyncStatus.Synced(System.currentTimeMillis()))

    override fun observeItems(): Flow<List<com.company.stockpro.core.domain.model.Item>> = emptyFlow()
    override fun observeItemsByClass(itemClass: com.company.stockpro.core.domain.model.ItemClass, sortOrder: com.company.stockpro.core.domain.model.SortOrder) = emptyFlow<List<com.company.stockpro.core.domain.model.Item>>()
    override fun searchItems(query: String) = emptyFlow<List<com.company.stockpro.core.domain.model.Item>>()
    override fun observeSummary() = emptyFlow<com.company.stockpro.core.domain.model.InventorySummary>()
    override fun observeTopLowStock(limit: Int) = emptyFlow<List<com.company.stockpro.core.domain.model.Item>>()
    override fun observeItem(itemId: String) = emptyFlow<com.company.stockpro.core.domain.model.Item?>()
    override fun observeLedger(itemId: String) = emptyFlow<List<com.company.stockpro.core.domain.model.StockMovement>>()
    override fun observeRecentMovements(limit: Int) = emptyFlow<List<com.company.stockpro.core.domain.model.StockMovement>>()
    override suspend fun getItem(itemId: String) = DataResult.Error(com.company.stockpro.core.domain.util.UiText.Dynamic("Not found"))
    override suspend fun getItemByCode(code: String) = DataResult.Error(com.company.stockpro.core.domain.util.UiText.Dynamic("Not found"))
    override suspend fun createItem(item: com.company.stockpro.core.domain.model.Item) = DataResult.Success(item)
    override suspend fun postMovement(movement: com.company.stockpro.core.domain.model.StockMovement) = DataResult.Success(movement)
    override suspend fun logout(): DataResult<Unit> = DataResult.Success(Unit)
    override suspend fun deleteAccount(): DataResult<Unit> = DataResult.Success(Unit)
    override fun observePendingCount() = flowOf(0)
    override fun observeFailedEntries() = emptyFlow<List<com.company.stockpro.core.domain.model.FailedOutboxEntry>>()
    override suspend fun retryFailedOutbox(id: Long) {}
    override suspend fun discardFailedOutbox(id: Long) {}
    override fun observeNotifications() = emptyFlow<List<com.company.stockpro.core.domain.model.AppNotification>>()
    override fun observeUnreadNotificationCount() = flowOf(0)
    override suspend fun markNotificationAsRead(id: String) {}
    override suspend fun markAllNotificationsAsRead() {}
    override suspend fun clearNotifications() {}
    override fun observeNotificationPreferences() = userSettingsFlow.asStateFlow().let { flow -> flowOf(NotificationPreferences()) }
    override fun observeTeamMembers() = emptyFlow<List<com.company.stockpro.core.domain.model.TeamMember>>()
    override suspend fun inviteMember(email: String, displayName: String, role: Role, plantId: String) =
        DataResult.Success(com.company.stockpro.core.domain.model.TeamMember("m1", displayName, email, role, plantId))
    override suspend fun updateMemberRole(memberId: String, newRole: Role) = DataResult.Success(Unit)
    override suspend fun deactivateMember(memberId: String) = DataResult.Success(Unit)
    override fun observeToolCustody(itemId: String) = emptyFlow<com.company.stockpro.core.domain.model.ToolCustody?>()
    override suspend fun issueTool(itemId: String, operatorName: String, notes: String?) = DataResult.Success(Unit)
    override suspend fun returnTool(itemId: String, notes: String?) = DataResult.Success(Unit)
    override suspend fun logCalibration(itemId: String, calibratedAt: Long, nextDueAt: Long, notes: String?) = DataResult.Success(Unit)
}
