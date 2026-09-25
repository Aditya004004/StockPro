package com.company.stockpro.core.domain.repository

import com.company.stockpro.core.domain.model.Item
import com.company.stockpro.core.domain.model.StockMovement
import com.company.stockpro.core.domain.util.DataResult
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for inventory operations.
 *
 * Reads come from Room (ADR-004). Writes go through the outbox (ADR-002).
 * ViewModel calls this directly for reads; PostMovementUseCase orchestrates writes.
 */
interface InventoryRepository {

    /** Reactive stream of all active items for the current plant. */
    fun observeItems(): Flow<List<Item>>

    /**
     * Reactive stream of active items filtered by ItemClass with explicit SortOrder.
     * Only the visible tab's data is observed per rules.md §5.2.
     */
    fun observeItemsByClass(
        itemClass: com.company.stockpro.core.domain.model.ItemClass,
        sortOrder: com.company.stockpro.core.domain.model.SortOrder = com.company.stockpro.core.domain.model.SortOrder.NAME_ASC,
    ): Flow<List<Item>>

    /** Full-text search over item name, code, and category. Sub-100ms, offline. */
    fun searchItems(query: String): Flow<List<Item>>

    /** Aggregate summary metrics for the current plant. */
    fun observeSummary(): Flow<com.company.stockpro.core.domain.model.InventorySummary>

    /** Top items at or below reorder level, sorted by on-hand ascending. */
    fun observeTopLowStock(limit: Int = 5): Flow<List<Item>>

    /** Reactive stream of a single item by ID. */
    fun observeItem(itemId: String): Flow<Item?>

    /** Ledger history for a specific item, most recent first. */
    fun observeLedger(itemId: String): Flow<List<StockMovement>>

    /** Recent plant-wide ledger movements, most recent first. */
    fun observeRecentMovements(limit: Int = 10): Flow<List<StockMovement>>

    /** Lookup a single item by ID (one-shot). */
    suspend fun getItem(itemId: String): DataResult<Item>

    /** Lookup a single item by its code (one-shot). Used for barcode resolution. */
    suspend fun getItemByCode(code: String): DataResult<Item>

    /** Create a new inventory item in Room and return the created domain item. */
    suspend fun createItem(item: Item): DataResult<Item>

    /**
     * Apply a movement optimistically to Room and enqueue in the outbox.
     * Called by [com.company.stockpro.core.domain.usecase.PostMovementUseCase].
     *
     * @param movement The validated movement with idempotencyKey set.
     * @return [DataResult.Success] with the movement, or [DataResult.Error].
     */
    suspend fun postMovement(movement: StockMovement): DataResult<StockMovement>

    /** Count of outbox entries in PENDING or POSTING state. */
    fun observePendingCount(): Flow<Int>

    /** Full synchronization status stream. */
    fun observeSyncStatus(): Flow<com.company.stockpro.core.domain.model.SyncStatus>

    /** Observe failed outbox entries for the "Needs Attention" view. */
    fun observeFailedEntries(): Flow<List<com.company.stockpro.core.domain.model.FailedOutboxEntry>>

    /** Reset a failed outbox entry back to PENDING to retry execution. */
    suspend fun retryFailedOutbox(id: Long)

    /** Permanently remove a failed outbox entry from the queue. */
    suspend fun discardFailedOutbox(id: Long)

    // ==========================================
    // Phase 5: Notifications & Preferences
    // ==========================================

    /** Reactive stream of notifications, newest first. */
    fun observeNotifications(): Flow<List<com.company.stockpro.core.domain.model.AppNotification>>

    /** Reactive stream of unread notification count. */
    fun observeUnreadNotificationCount(): Flow<Int>

    /** Mark a single notification as read. */
    suspend fun markNotificationAsRead(id: String)

    /** Mark all notifications as read. */
    suspend fun markAllNotificationsAsRead()

    /** Clear / delete all notifications. */
    suspend fun clearNotifications()

    /** Reactive stream of per-type notification preferences (prd.md §8.2). */
    fun observeNotificationPreferences(): Flow<com.company.stockpro.core.domain.model.NotificationPreferences>

    /** Update per-type notification preferences. */
    suspend fun updateNotificationPreferences(preferences: com.company.stockpro.core.domain.model.NotificationPreferences)

    // ==========================================
    // Phase 5: User Settings & App Preferences
    // ==========================================

    /** Reactive stream of current user settings and profile. */
    fun observeUserSettings(): Flow<com.company.stockpro.core.domain.model.UserSettings>

    /** Update user display name. */
    suspend fun updateUserProfile(displayName: String): DataResult<Unit>

    /** Update app theme preference. */
    suspend fun updateThemeMode(themeMode: com.company.stockpro.core.domain.model.ThemeMode)

    /** Update app language preference. */
    suspend fun updateLanguage(language: com.company.stockpro.core.domain.model.AppLanguage)

    /** Update app unit system preference. */
    suspend fun updateUnitSystem(unitSystem: com.company.stockpro.core.domain.model.UnitSystem)

    /** Clear app cache and return number of bytes freed. */
    suspend fun clearCache(): DataResult<Long>

    /** Purge all local cached Room data, clear tables, and reset session state on logout. */
    suspend fun logout(): DataResult<Unit>

    /** Delete user account and purge local cached data. */
    suspend fun deleteAccount(): DataResult<Unit>

    // ==========================================
    // Phase 5: Team Management
    // ==========================================

    /** Reactive stream of plant team members. */
    fun observeTeamMembers(): Flow<List<com.company.stockpro.core.domain.model.TeamMember>>

    /** Invite a new member to the team. */
    suspend fun inviteMember(
        email: String,
        displayName: String,
        role: com.company.stockpro.core.domain.model.Role,
        plantId: String,
    ): DataResult<com.company.stockpro.core.domain.model.TeamMember>

    /** Update a member's role (forces token refresh). */
    suspend fun updateMemberRole(
        memberId: String,
        newRole: com.company.stockpro.core.domain.model.Role,
    ): DataResult<Unit>

    /** Deactivate a team member while preserving their ledger audit trail. */
    suspend fun deactivateMember(memberId: String): DataResult<Unit>

    // ==========================================
    // Phase 5: Tool Custody & Calibration (PRD §7)
    // ==========================================

    /** Reactive stream of tool custody and calibration status by item ID. */
    fun observeToolCustody(itemId: String): Flow<com.company.stockpro.core.domain.model.ToolCustody?>

    /** Issue a tool to an operator. */
    suspend fun issueTool(
        itemId: String,
        operatorName: String,
        notes: String? = null,
    ): DataResult<Unit>

    /** Return a tool back to store / available stock. */
    suspend fun returnTool(
        itemId: String,
        notes: String? = null,
    ): DataResult<Unit>

    /** Log calibration for a tool and advance the next calibration due date. */
    suspend fun logCalibration(
        itemId: String,
        calibratedAt: Long,
        nextDueAt: Long,
        notes: String? = null,
    ): DataResult<Unit>
}

