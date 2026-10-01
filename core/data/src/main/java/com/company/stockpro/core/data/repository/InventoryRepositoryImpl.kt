package com.company.stockpro.core.data.repository

import com.company.stockpro.core.database.StockProDatabase
import com.company.stockpro.core.database.entity.NotificationEntity
import com.company.stockpro.core.database.entity.OutboxEntity
import com.company.stockpro.core.database.entity.StockLedgerEntity
import com.company.stockpro.core.database.entity.TeamMemberEntity
import com.company.stockpro.core.database.entity.ToolCustodyEntity
import com.company.stockpro.core.domain.model.AppLanguage
import com.company.stockpro.core.domain.model.AppNotification
import com.company.stockpro.core.domain.model.Currency
import com.company.stockpro.core.domain.model.Item
import com.company.stockpro.core.domain.model.ItemClass
import com.company.stockpro.core.domain.model.Money
import com.company.stockpro.core.domain.model.MovementType
import com.company.stockpro.core.domain.model.NotificationPreferences
import com.company.stockpro.core.domain.model.NotificationType
import com.company.stockpro.core.domain.model.Quantity
import com.company.stockpro.core.domain.model.Role
import com.company.stockpro.core.domain.model.StockMovement
import com.company.stockpro.core.domain.model.TeamMember
import com.company.stockpro.core.domain.model.ThemeMode
import com.company.stockpro.core.domain.model.ToolCustody
import com.company.stockpro.core.domain.model.ToolStatus
import com.company.stockpro.core.domain.model.UnitSystem
import com.company.stockpro.core.domain.model.UserProfile
import com.company.stockpro.core.domain.model.UserSettings
import com.company.stockpro.core.domain.repository.InventoryRepository
import com.company.stockpro.core.domain.util.DataResult
import com.company.stockpro.core.domain.util.UiText
import androidx.room.withTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Binds Room DAOs to the domain [InventoryRepository] interface.
 *
 * Reads come from Room (ADR-004). Writes apply optimistically to Room
 * in a single transaction (item delta + provisional ledger entry + outbox row),
 * then OutboxWorker drains to the Cloud Function (ADR-002).
 */
class InventoryRepositoryImpl(
    private val database: StockProDatabase,
    private val syncManager: com.company.stockpro.core.data.sync.InventorySyncManager? = null,
    private val context: android.content.Context? = null,
    private val appScope: CoroutineScope = CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.IO),
) : InventoryRepository {

    private val itemDao get() = database.itemDao()
    private val ledgerDao get() = database.ledgerDao()
    private val outboxDao get() = database.outboxDao()
    private val notificationDao get() = database.notificationDao()
    private val toolCustodyDao get() = database.toolCustodyDao()
    private val teamMemberDao get() = database.teamMemberDao()

    private val userSettingsFlow = MutableStateFlow(
        UserSettings(
            profile = UserProfile(
                uid = "usr_aditya",
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

    /** Reads the current plant ID from user settings to avoid hardcoding. */
    private val currentPlantId: String get() = userSettingsFlow.value.profile.plantId

    /** Reads the current organization ID from user settings to avoid hardcoding. */
    private val currentOrgId: String get() = userSettingsFlow.value.profile.orgId

    init {
        appScope.launch {
            seedDefaultsIfEmpty()
        }
        appScope.launch {
            observeAndReconcileNotifications()
        }
        appScope.launch {
            try {
                com.google.firebase.auth.FirebaseAuth.getInstance().addAuthStateListener { auth ->
                    val user = auth.currentUser
                    if (user != null) {
                        userSettingsFlow.update { current ->
                            current.copy(
                                profile = current.profile.copy(
                                    uid = user.uid,
                                    displayName = user.displayName?.ifBlank { null }
                                        ?: user.email?.substringBefore('@')?.replaceFirstChar { it.uppercase() }
                                        ?: current.profile.displayName,
                                    email = user.email ?: current.profile.email,
                                )
                            )
                        }
                        appScope.launch {
                            try {
                                com.google.firebase.messaging.FirebaseMessaging.getInstance()
                                    .subscribeToTopic("plant_$currentPlantId")
                            } catch (_: Exception) {}
                            try {
                                syncManager?.pullDelta(currentOrgId, currentPlantId)
                            } catch (e: Exception) {
                                if (e is kotlinx.coroutines.CancellationException) throw e
                                android.util.Log.w("InventoryRepo", "Delta pull failed: ${e.message}")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Firebase Auth not initialized or offline
            }
        }
    }

    private suspend fun seedDefaultsIfEmpty() {
        if (teamMemberDao.count() == 0) {
            val now = System.currentTimeMillis()
            teamMemberDao.insertAll(
                listOf(
                    TeamMemberEntity("usr_1", "Aditya Maurya", "aditya@company.com", Role.INVENTORY_MANAGER.name, "plant_01", true, now),
                    TeamMemberEntity("usr_2", "Rajesh Sharma", "rajesh@company.com", Role.PLANT_MANAGER.name, "plant_01", true, now - 3600000),
                    TeamMemberEntity("usr_3", "Priya Nair", "priya@company.com", Role.QA.name, "plant_01", true, now - 7200000),
                    TeamMemberEntity("usr_4", "Vikram Patel", "vikram@company.com", Role.PRODUCTION_SUPERVISOR.name, "plant_01", true, now - 14400000),
                    TeamMemberEntity("usr_5", "Sunita Rao", "sunita@company.com", Role.STORE_KEEPER.name, "plant_01", true, now - 86400000),
                ),
            )
        }

        // Clean up any legacy mock notifications that might have been seeded previously
        notificationDao.deleteById("notif_1")
        notificationDao.deleteById("notif_2")
        notificationDao.deleteById("notif_3")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun observeAndReconcileNotifications() {
        combine(
            itemDao.observeActiveItems(),
            outboxDao.observeFailedEntries(),
            userSettingsFlow,
        ) { items, failedEntries, settings ->
            Triple(items, failedEntries, settings)
        }.collect { (items, failedEntries, settings) ->
            reconcileNotifications(items, failedEntries, settings)
        }
    }

    private suspend fun reconcileNotifications(
        items: List<com.company.stockpro.core.database.entity.ItemEntity>,
        failedEntries: List<OutboxEntity>,
        settings: UserSettings,
    ) {
        val currentPlant = settings.profile.plantId
        val existingNotifications = notificationDao.getAll(currentPlant).associateBy { it.id }
        val now = System.currentTimeMillis()
        val prefs = settings.notificationPreferences

        val desiredIds = mutableSetOf<String>()

        // 1. Check Out of Stock items
        if (prefs.isTypeEnabled(NotificationType.OUT_OF_STOCK)) {
            for (item in items) {
                if (item.onHandMilli <= 0) {
                    val notifId = "out_of_stock_${item.id}"
                    desiredIds.add(notifId)
                    val existing = existingNotifications[notifId]
                    notificationDao.insert(
                        NotificationEntity(
                            id = notifId,
                            plantId = currentPlant,
                            type = NotificationType.OUT_OF_STOCK.name,
                            title = "Out of Stock: ${item.name}",
                            body = "${item.name} (${item.code}) is completely out of stock.",
                            entityRef = item.code,
                            isRead = existing?.isRead ?: false,
                            createdAt = existing?.createdAt ?: now,
                        ),
                    )
                }
            }
        }

        // 2. Check Low Stock items
        if (prefs.isTypeEnabled(NotificationType.LOW_STOCK)) {
            for (item in items) {
                if (item.onHandMilli > 0 && item.reorderLevelMilli > 0 && item.onHandMilli <= item.reorderLevelMilli) {
                    val notifId = "low_stock_${item.id}"
                    desiredIds.add(notifId)
                    val existing = existingNotifications[notifId]
                    val onHandQty = Quantity(item.onHandMilli).formatted()
                    val reorderQty = Quantity(item.reorderLevelMilli).formatted()
                    notificationDao.insert(
                        NotificationEntity(
                            id = notifId,
                            plantId = currentPlant,
                            type = NotificationType.LOW_STOCK.name,
                            title = "Low Stock: ${item.name}",
                            body = "${item.name} (${item.code}) is below reorder level ($onHandQty vs $reorderQty).",
                            entityRef = item.code,
                            isRead = existing?.isRead ?: false,
                            createdAt = existing?.createdAt ?: now,
                        ),
                    )
                }
            }
        }

        // 3. Check Sync Failures
        if (prefs.isTypeEnabled(NotificationType.SYNC_FAILURE) && failedEntries.isNotEmpty()) {
            val notifId = "sync_failure"
            desiredIds.add(notifId)
            val existing = existingNotifications[notifId]
            notificationDao.insert(
                NotificationEntity(
                    id = notifId,
                    plantId = currentPlant,
                    type = NotificationType.SYNC_FAILURE.name,
                    title = "Sync Alert",
                    body = "${failedEntries.size} offline update(s) failed to sync. Tap to review.",
                    entityRef = null,
                    isRead = existing?.isRead ?: false,
                    createdAt = existing?.createdAt ?: now,
                ),
            )
        }

        // 4. Remove any automated notifications that are no longer valid
        for (existing in existingNotifications.values) {
            if (existing.id.startsWith("low_stock_") ||
                existing.id.startsWith("out_of_stock_") ||
                existing.id == "sync_failure" ||
                existing.id in listOf("notif_1", "notif_2", "notif_3")
            ) {
                if (!desiredIds.contains(existing.id)) {
                    notificationDao.deleteById(existing.id)
                }
            }
        }
    }

    override fun observeItems(): Flow<List<Item>> =
        itemDao.observeActiveItems().map { entities -> entities.map { it.toDomain() } }

    override fun observeItemsByClass(
        itemClass: ItemClass,
        sortOrder: com.company.stockpro.core.domain.model.SortOrder,
    ): Flow<List<Item>> {
        val flow = when (sortOrder) {
            com.company.stockpro.core.domain.model.SortOrder.NAME_ASC ->
                itemDao.observeItemsByClassOrderByName(itemClass.name)
            com.company.stockpro.core.domain.model.SortOrder.CODE_ASC ->
                itemDao.observeItemsByClassOrderByCode(itemClass.name)
            com.company.stockpro.core.domain.model.SortOrder.QUANTITY_DESC ->
                itemDao.observeItemsByClassOrderByQuantityDesc(itemClass.name)
            com.company.stockpro.core.domain.model.SortOrder.QUANTITY_ASC ->
                itemDao.observeItemsByClassOrderByQuantityAsc(itemClass.name)
            com.company.stockpro.core.domain.model.SortOrder.LAST_MOVED_DESC ->
                itemDao.observeItemsByClassOrderByLastMoved(itemClass.name)
        }
        return flow.map { entities -> entities.map { it.toDomain() } }
    }

    override fun observeSummary(): Flow<com.company.stockpro.core.domain.model.InventorySummary> =
        itemDao.observeSummary().map { it.toDomain() }

    override fun observeTopLowStock(limit: Int): Flow<List<Item>> =
        itemDao.observeTopLowStock(limit).map { entities -> entities.map { it.toDomain() } }

    override fun observeItem(itemId: String): Flow<Item?> =
        itemDao.observeItem(itemId).map { it?.toDomain() }

    override fun searchItems(query: String): Flow<List<Item>> {
        val sanitized = query.trim()
            .split(Regex("[^a-zA-Z0-9]+"))
            .filter { it.isNotBlank() }
            .joinToString(" ") { "$it*" }
        return if (sanitized.isBlank()) {
            itemDao.observeActiveItems().map { entities -> entities.map { it.toDomain() } }
        } else {
            itemDao.searchItems(sanitized).map { entities -> entities.map { it.toDomain() } }
        }
    }

    override fun observeLedger(itemId: String): Flow<List<StockMovement>> =
        ledgerDao.observeByItem(itemId).map { entities -> entities.map { it.toDomain() } }

    override fun observeRecentMovements(limit: Int): Flow<List<StockMovement>> =
        ledgerDao.observeRecent(limit).map { entities -> entities.map { it.toDomain() } }

    override suspend fun getItem(itemId: String): DataResult<Item> {
        val entity = itemDao.getItem(itemId)
            ?: return DataResult.Error(UiText.Dynamic("Item not found."))
        return DataResult.Success(entity.toDomain())
    }

    override suspend fun getItemByCode(code: String): DataResult<Item> {
        val entity = itemDao.getItemByCode(code)
            ?: return DataResult.Error(UiText.Dynamic("No item matches this code."))
        return DataResult.Success(entity.toDomain())
    }

    override suspend fun createItem(item: Item): DataResult<Item> {
        return try {
            val existing = itemDao.getItemByCode(item.code.trim())
            if (existing != null) {
                return DataResult.Error(UiText.Dynamic("An item with code '${item.code}' already exists."))
            }
            val now = System.currentTimeMillis()
            val entity = com.company.stockpro.core.database.entity.ItemEntity(
                id = item.id.ifBlank { UUID.randomUUID().toString() },
                code = item.code.trim().uppercase(),
                codeLower = item.code.trim().lowercase(),
                name = item.name.trim(),
                nameLower = item.name.trim().lowercase(),
                itemClass = item.itemClass.name,
                categoryId = "",
                categoryName = item.categoryName.trim(),
                unitOfMeasure = item.unitOfMeasure.trim().ifBlank { "PCS" },
                onHandMilli = item.onHand.milli,
                reservedMilli = item.reserved.milli,
                inProductionMilli = item.inProduction.milli,
                reorderLevelMilli = item.reorderLevel.milli,
                parLevelMilli = item.parLevel.milli,
                unitCostPaise = item.unitCost.paise,
                locationId = "",
                locationCode = item.locationCode.trim(),
                barcode = item.barcode,
                imagePath = item.imagePath,
                isActive = item.isActive,
                createdAt = now,
                updatedAt = now,
                lastMovementAt = now,
            )
            itemDao.upsert(entity)
            appScope.launch {
                try {
                    val functions = com.google.firebase.functions.FirebaseFunctions.getInstance()
                    functions.getHttpsCallable("createItem").call(
                        mapOf(
                            "orgId" to currentOrgId,
                            "plantId" to currentPlantId,
                            "code" to entity.code,
                            "name" to entity.name,
                            "itemClass" to entity.itemClass,
                            "categoryName" to entity.categoryName,
                            "unitOfMeasure" to entity.unitOfMeasure,
                            "reorderLevelMilli" to entity.reorderLevelMilli,
                            "parLevelMilli" to entity.parLevelMilli,
                            "unitCostPaise" to entity.unitCostPaise,
                            "locationCode" to entity.locationCode,
                        )
                    )
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    android.util.Log.w("InventoryRepo", "Background Cloud Function createItem failed or offline: ${e.message}")
                }
            }
            DataResult.Success(entity.toDomain())
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            DataResult.Error(UiText.Dynamic(e.message ?: "Failed to create item."))
        }
    }

    override suspend fun postMovement(movement: StockMovement): DataResult<StockMovement> {
        val now = System.currentTimeMillis()
        val ledgerEntryId = UUID.randomUUID().toString()

        // Determine signed delta: outgoing types are negative
        val signedDelta = when (movement.type) {
            MovementType.ISSUE, MovementType.TRANSFER_OUT, MovementType.SCRAP ->
                -movement.delta.milli
            else -> movement.delta.milli
        }

        database.withTransaction {
            // 1. Optimistic delta on item balance
            itemDao.applyOptimisticDelta(movement.itemId, signedDelta, now)

            // 2. Provisional ledger entry (isPending = true)
            ledgerDao.insert(
                StockLedgerEntity(
                    id = ledgerEntryId,
                    itemId = movement.itemId,
                    itemCode = movement.itemCode,
                    itemName = movement.itemName,
                    type = movement.type.name,
                    deltaMilli = signedDelta,
                    unitCostPaise = movement.unitCost.paise,
                    reference = movement.reference,
                    reasonCode = movement.reasonCode,
                    note = movement.note,
                    fromLocationId = movement.fromLocationId,
                    toLocationId = movement.toLocationId,
                    actorUid = movement.actorUid,
                    actorName = movement.actorName,
                    idempotencyKey = movement.idempotencyKey,
                    clientCreatedAt = now,
                    createdAt = now,
                    isPending = true,
                )
            )

            // 3. Outbox row for the worker to drain
            outboxDao.insert(
                OutboxEntity(
                    itemId = movement.itemId,
                    idempotencyKey = movement.idempotencyKey,
                    payload = buildPayloadJson(movement, signedDelta),
                    createdAt = now,
                    updatedAt = now,
                )
            )
        }

        context?.let { ctx ->
            try {
                val workRequest = androidx.work.OneTimeWorkRequestBuilder<com.company.stockpro.core.data.worker.OutboxWorker>()
                    .setConstraints(
                        androidx.work.Constraints.Builder()
                            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                            .build()
                    )
                    .build()
                androidx.work.WorkManager.getInstance(ctx).enqueueUniqueWork(
                    "OutboxWorker_${movement.itemId}",
                    androidx.work.ExistingWorkPolicy.APPEND_OR_REPLACE,
                    workRequest,
                )
            } catch (e: Exception) {
                android.util.Log.w("InventoryRepo", "Failed to enqueue OutboxWorker: ${e.message}")
            }
        }

        return DataResult.Success(
            movement.copy(
                id = ledgerEntryId,
                clientCreatedAt = now,
                isPending = true,
            )
        )
    }

    override fun observePendingCount(): Flow<Int> = outboxDao.observePendingCount()

    override fun observeSyncStatus(): Flow<com.company.stockpro.core.domain.model.SyncStatus> {
        return syncManager?.observeSyncStatus() ?: kotlinx.coroutines.flow.combine(
            outboxDao.observePendingCount(),
            outboxDao.observeFailedCount(),
        ) { pending, failed ->
            when {
                failed > 0 -> com.company.stockpro.core.domain.model.SyncStatus.NeedsAttention(failed)
                pending > 0 -> com.company.stockpro.core.domain.model.SyncStatus.Pending(pending)
                else -> com.company.stockpro.core.domain.model.SyncStatus.Synced(System.currentTimeMillis())
            }
        }
    }

    override suspend fun triggerSync(): DataResult<Unit> {
        return try {
            syncManager?.pullDelta(currentOrgId, currentPlantId)
            DataResult.Success(Unit)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            DataResult.Error(UiText.Dynamic(e.localizedMessage ?: "Sync failed"))
        }
    }

    override fun observeFailedEntries(): Flow<List<com.company.stockpro.core.domain.model.FailedOutboxEntry>> =
        outboxDao.observeFailedEntries().map { entities ->
            entities.map {
                com.company.stockpro.core.domain.model.FailedOutboxEntry(
                    id = it.id,
                    itemId = it.itemId,
                    idempotencyKey = it.idempotencyKey,
                    errorMessage = it.errorMessage,
                    createdAt = it.createdAt,
                )
            }
        }

    override suspend fun retryFailedOutbox(id: Long) {
        outboxDao.resetToPending(id, System.currentTimeMillis())
    }

    override suspend fun discardFailedOutbox(id: Long) {
        outboxDao.deleteById(id)
    }

    // ==========================================
    // Phase 5: Notifications & Preferences
    // ==========================================

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeNotifications(): Flow<List<AppNotification>> =
        userSettingsFlow.map { it.profile.plantId }.flatMapLatest { plantId ->
            notificationDao.observeAll(plantId).map { entities -> entities.map { it.toDomain() } }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeUnreadNotificationCount(): Flow<Int> =
        userSettingsFlow.map { it.profile.plantId }.flatMapLatest { plantId ->
            notificationDao.observeUnreadCount(plantId)
        }

    override suspend fun markNotificationAsRead(id: String) {
        notificationDao.markAsRead(id)
    }

    override suspend fun markAllNotificationsAsRead() {
        notificationDao.markAllAsRead(currentPlantId)
    }

    override suspend fun clearNotifications() {
        notificationDao.deleteAll(currentPlantId)
    }

    override fun observeNotificationPreferences(): Flow<NotificationPreferences> =
        userSettingsFlow.map { it.notificationPreferences }

    override suspend fun updateNotificationPreferences(preferences: NotificationPreferences) {
        userSettingsFlow.update { it.copy(notificationPreferences = preferences) }
    }

    // ==========================================
    // Phase 5: User Settings & App Preferences
    // ==========================================

    override fun observeUserSettings(): Flow<UserSettings> =
        userSettingsFlow.asStateFlow()

    override suspend fun updateUserProfile(displayName: String): DataResult<Unit> {
        userSettingsFlow.update { it.copy(profile = it.profile.copy(displayName = displayName.trim())) }
        return DataResult.Success(Unit)
    }

    override suspend fun updateFullUserProfile(
        displayName: String,
        email: String,
        phone: String,
        company: String,
        plantName: String,
    ): DataResult<Unit> {
        val cleanPlant = plantName.trim()
        val plantId = if (cleanPlant.isNotBlank()) {
            "plant_" + cleanPlant.lowercase().replace("[^a-z0-9]".toRegex(), "_")
        } else {
            userSettingsFlow.value.profile.plantId
        }
        userSettingsFlow.update { current ->
            val updatedPlants = if (cleanPlant.isNotBlank() && !current.availablePlants.contains(cleanPlant)) {
                current.availablePlants + cleanPlant
            } else {
                current.availablePlants
            }
            current.copy(
                profile = current.profile.copy(
                    displayName = displayName.trim(),
                    email = email.trim(),
                    phone = phone.trim(),
                    company = company.trim(),
                    plantName = if (cleanPlant.isNotBlank()) cleanPlant else current.profile.plantName,
                    plantId = plantId,
                ),
                availablePlants = updatedPlants,
            )
        }
        return DataResult.Success(Unit)
    }

    override suspend fun selectPlant(plantName: String): DataResult<Unit> {
        val cleanName = plantName.trim()
        if (cleanName.isBlank()) return DataResult.Error(UiText.Dynamic("Plant name cannot be empty"))
        val plantId = "plant_" + cleanName.lowercase().replace("[^a-z0-9]".toRegex(), "_")
        userSettingsFlow.update { current ->
            current.copy(
                profile = current.profile.copy(
                    plantName = cleanName,
                    plantId = plantId,
                ),
            )
        }
        return DataResult.Success(Unit)
    }

    override suspend fun addPlant(plantName: String): DataResult<Unit> {
        val cleanName = plantName.trim()
        if (cleanName.isBlank()) return DataResult.Error(UiText.Dynamic("Plant name cannot be empty"))
        val plantId = "plant_" + cleanName.lowercase().replace("[^a-z0-9]".toRegex(), "_")
        userSettingsFlow.update { current ->
            val updatedList = if (current.availablePlants.contains(cleanName)) {
                current.availablePlants
            } else {
                current.availablePlants + cleanName
            }
            current.copy(
                profile = current.profile.copy(
                    plantName = cleanName,
                    plantId = plantId,
                ),
                availablePlants = updatedList,
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

    override suspend fun updateCurrency(currency: Currency) {
        userSettingsFlow.update { it.copy(currency = currency) }
    }

    override suspend fun clearCache(): DataResult<Long> {
        // No real cache to clear in Room-only architecture
        return DataResult.Success(0L)
    }

    override suspend fun logout(): DataResult<Unit> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        try {
            database.clearAllTables()
            userSettingsFlow.update { current ->
                current.copy(
                    profile = UserProfile(
                        uid = "",
                        displayName = "User",
                        email = "",
                        role = Role.INVENTORY_MANAGER,
                        plantId = "plant_01",
                        plantName = "Plant 1",
                        orgId = "org_demo",
                    )
                )
            }
            try {
                com.google.firebase.messaging.FirebaseMessaging.getInstance()
                    .unsubscribeFromTopic("plant_$currentPlantId")
            } catch (_: Exception) {}
            try {
                com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
            } catch (_: Exception) {}
            DataResult.Success(Unit)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            DataResult.Error(UiText.Dynamic(e.localizedMessage ?: "Logout failed."))
        }
    }

    override suspend fun deleteAccount(): DataResult<Unit> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        try {
            val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (user != null) {
                user.delete().await()
            }
            logout()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            DataResult.Error(UiText.Dynamic(e.localizedMessage ?: "Failed to delete account. Please re-authenticate and try again."))
        }
    }

    // ==========================================
    // Phase 5: Team Management
    // ==========================================

    override fun observeTeamMembers(): Flow<List<TeamMember>> =
        teamMemberDao.observeAll(currentPlantId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun inviteMember(
        email: String,
        displayName: String,
        role: Role,
        plantId: String,
    ): DataResult<TeamMember> {
        val member = TeamMemberEntity(
            id = UUID.randomUUID().toString(),
            displayName = displayName.ifBlank { email.substringBefore("@") },
            email = email.trim(),
            role = role.name,
            plantId = plantId,
            isActive = true,
            lastActiveAt = System.currentTimeMillis(),
        )
        teamMemberDao.insert(member)
        return DataResult.Success(member.toDomain())
    }

    override suspend fun updateMemberRole(
        memberId: String,
        newRole: Role,
    ): DataResult<Unit> {
        teamMemberDao.updateRole(memberId, newRole.name)
        return DataResult.Success(Unit)
    }

    override suspend fun deactivateMember(memberId: String): DataResult<Unit> {
        teamMemberDao.updateActiveStatus(memberId, false)
        return DataResult.Success(Unit)
    }

    // ==========================================
    // Phase 5: Tool Custody & Calibration (PRD §7)
    // ==========================================

    override fun observeToolCustody(itemId: String): Flow<ToolCustody?> =
        toolCustodyDao.observe(itemId).map { it?.toDomain() }

    override suspend fun issueTool(
        itemId: String,
        operatorName: String,
        notes: String?,
    ): DataResult<Unit> {
        val now = System.currentTimeMillis()
        val existing = toolCustodyDao.get(itemId)
        if (existing == null) {
            toolCustodyDao.upsert(
                ToolCustodyEntity(
                    itemId = itemId,
                    status = ToolStatus.ISSUED.name,
                    operatorName = operatorName,
                    issuedAt = now,
                    notes = notes,
                ),
            )
        } else {
            toolCustodyDao.updateCustody(
                itemId = itemId,
                status = ToolStatus.ISSUED.name,
                operatorName = operatorName,
                issuedAt = now,
                notes = notes,
            )
        }
        return DataResult.Success(Unit)
    }

    override suspend fun returnTool(
        itemId: String,
        notes: String?,
    ): DataResult<Unit> {
        toolCustodyDao.updateCustody(
            itemId = itemId,
            status = ToolStatus.AVAILABLE.name,
            operatorName = null,
            issuedAt = null,
            notes = notes,
        )
        return DataResult.Success(Unit)
    }

    override suspend fun logCalibration(
        itemId: String,
        calibratedAt: Long,
        nextDueAt: Long,
        notes: String?,
    ): DataResult<Unit> {
        val existing = toolCustodyDao.get(itemId)
        if (existing == null) {
            toolCustodyDao.upsert(
                ToolCustodyEntity(
                    itemId = itemId,
                    status = ToolStatus.AVAILABLE.name,
                    lastCalibratedAt = calibratedAt,
                    nextCalibrationDueAt = nextDueAt,
                    notes = notes,
                ),
            )
        } else {
            toolCustodyDao.updateCalibration(
                itemId = itemId,
                calibratedAt = calibratedAt,
                nextDueAt = nextDueAt,
                notes = notes,
            )
        }
        return DataResult.Success(Unit)
    }

    /**
     * Builds a safe JSON payload for the outbox.
     * Escapes all string inputs to prevent JSON injection or format corruption.
     */
    private fun buildPayloadJson(movement: StockMovement, signedDelta: Long): String {
        return buildString {
            append("{")
            append("\"orgId\":\"${escapeJson(currentOrgId)}\",")
            append("\"plantId\":\"${escapeJson(currentPlantId)}\",")
            append("\"itemId\":\"${escapeJson(movement.itemId)}\",")
            append("\"type\":\"${escapeJson(movement.type.name)}\",")
            append("\"deltaMilli\":$signedDelta,")
            append("\"idempotencyKey\":\"${escapeJson(movement.idempotencyKey)}\",")
            movement.reference?.let { append("\"reference\":\"${escapeJson(it)}\",") }
            movement.reasonCode?.let { append("\"reasonCode\":\"${escapeJson(it)}\",") }
            movement.note?.let { append("\"note\":\"${escapeJson(it)}\",") }
            movement.locationId?.let { append("\"locationId\":\"${escapeJson(it)}\",") }
            movement.fromLocationId?.let { append("\"fromLocationId\":\"${escapeJson(it)}\",") }
            movement.toLocationId?.let { append("\"toLocationId\":\"${escapeJson(it)}\",") }
            append("\"unitCostPaise\":${movement.unitCost.paise}")
            append("}")
        }
    }

    private fun escapeJson(value: String): String {
        val sb = StringBuilder()
        for (ch in value) {
            when (ch) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\b' -> sb.append("\\b")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> {
                    if (ch.code < 0x20) {
                        sb.append(String.format("\\u%04x", ch.code))
                    } else {
                        sb.append(ch)
                    }
                }
            }
        }
        return sb.toString()
    }
}

// ── Mappers ──

private fun com.company.stockpro.core.database.entity.ItemEntity.toDomain(): Item = Item(
    id = id,
    code = code,
    name = name,
    itemClass = ItemClass.fromString(itemClass),
    categoryName = categoryName,
    unitOfMeasure = unitOfMeasure,
    onHand = Quantity(onHandMilli),
    reserved = Quantity(reservedMilli),
    inProduction = Quantity(inProductionMilli),
    reorderLevel = Quantity(reorderLevelMilli),
    parLevel = Quantity(parLevelMilli),
    unitCost = Money(unitCostPaise),
    locationCode = locationCode,
    barcode = barcode,
    imagePath = imagePath,
    isActive = isActive,
    lastMovementAt = lastMovementAt,
)

private fun StockLedgerEntity.toDomain(): StockMovement = StockMovement(
    id = id,
    itemId = itemId,
    itemCode = itemCode,
    itemName = itemName,
    type = MovementType.fromString(type),
    delta = Quantity(deltaMilli),
    balanceAfter = Quantity(balanceAfterMilli),
    unitCost = Money(unitCostPaise),
    reference = reference,
    reasonCode = reasonCode,
    note = note,
    fromLocationId = fromLocationId,
    toLocationId = toLocationId,
    actorUid = actorUid,
    actorName = actorName,
    idempotencyKey = idempotencyKey,
    clientCreatedAt = clientCreatedAt,
    createdAt = createdAt,
    isPending = isPending,
)

private fun com.company.stockpro.core.database.entity.InventorySummaryEntity.toDomain(): com.company.stockpro.core.domain.model.InventorySummary =
    com.company.stockpro.core.domain.model.InventorySummary(
        totalItems = totalItems,
        lowStockCount = lowStockCount,
        outOfStockCount = outOfStockCount,
        reservedCount = reservedCount,
        rawMaterialsCount = rawMaterialsCount,
        finishedGoodsCount = finishedGoodsCount,
        totalValuationPaise = totalValuationPaise,
        inProductionCount = inProductionCount,
    )

private fun NotificationEntity.toDomain(): AppNotification = AppNotification(
    id = id,
    plantId = plantId,
    type = NotificationType.fromString(type),
    title = title,
    body = body,
    entityRef = entityRef,
    isRead = isRead,
    createdAt = createdAt,
)

private fun TeamMemberEntity.toDomain(): TeamMember = TeamMember(
    id = id,
    displayName = displayName,
    email = email,
    role = Role.fromString(role),
    plantId = plantId,
    isActive = isActive,
    lastActiveAt = lastActiveAt,
)

private fun ToolCustodyEntity.toDomain(): ToolCustody = ToolCustody(
    itemId = itemId,
    status = ToolStatus.fromString(status),
    assignedToOperator = operatorName,
    issuedAt = issuedAt,
    calibrationIntervalDays = calibrationIntervalDays,
    lastCalibratedAt = lastCalibratedAt,
    nextCalibrationDueAt = nextCalibrationDueAt,
    notes = notes,
)
