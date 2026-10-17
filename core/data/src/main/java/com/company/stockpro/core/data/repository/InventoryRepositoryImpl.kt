package com.company.stockpro.core.data.repository

import com.company.stockpro.core.database.StockProDatabase
import com.company.stockpro.core.database.entity.NotificationEntity
import com.company.stockpro.core.database.entity.OutboxEntity
import com.company.stockpro.core.database.entity.PlantEntity
import com.company.stockpro.core.database.entity.StockLedgerEntity
import com.company.stockpro.core.database.entity.TeamMemberEntity
import com.company.stockpro.core.database.entity.ThresholdAuditEntity
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
import com.company.stockpro.core.domain.model.Plant
import com.company.stockpro.core.domain.model.Quantity
import com.company.stockpro.core.domain.model.Role
import com.company.stockpro.core.domain.model.StockMovement
import com.company.stockpro.core.domain.model.TeamMember
import com.company.stockpro.core.domain.model.ThemeMode
import com.company.stockpro.core.domain.model.ThresholdAudit
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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
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
    private val plantDao get() = database.plantDao()
    private val thresholdAuditDao get() = database.thresholdAuditDao()

    private val prefs by lazy {
        context?.getSharedPreferences("stockpro_user_prefs", android.content.Context.MODE_PRIVATE)
    }

    private fun persistSettings(settings: UserSettings) {
        prefs?.edit()?.apply {
            putString("uid", settings.profile.uid)
            putString("display_name", settings.profile.displayName)
            putString("email", settings.profile.email)
            putString("phone", settings.profile.phone)
            putString("company", settings.profile.company)
            putString("plant_id", settings.profile.plantId)
            putString("plant_name", settings.profile.plantName)
            putString("warehouse_name", settings.profile.warehouseName)
            putString("theme_mode", settings.themeMode.name)
            putString("language", settings.language.name)
            putString("unit_system", settings.unitSystem.name)
            putString("currency", settings.currency.name)
            apply()
        }
    }

    private fun loadInitialSettings(): UserSettings {
        val p = prefs ?: return UserSettings(
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
        )

        val uid = p.getString("uid", null) ?: "usr_aditya"
        val displayName = p.getString("display_name", null) ?: "Aditya Maurya"
        val email = p.getString("email", null) ?: "aditya@company.com"
        val phone = p.getString("phone", null) ?: "+91 98765 43210"
        val company = p.getString("company", null) ?: "StockPro Manufacturing Ltd"
        val plantId = p.getString("plant_id", null) ?: "plant_01"
        val plantName = p.getString("plant_name", null) ?: "Pune Plant 1"
        val warehouseName = p.getString("warehouse_name", null) ?: "Main Warehouse"
        val themeModeStr = p.getString("theme_mode", null) ?: ThemeMode.LIGHT.name
        val languageStr = p.getString("language", null) ?: AppLanguage.ENGLISH.name
        val unitSystemStr = p.getString("unit_system", null) ?: UnitSystem.METRIC.name
        val currencyStr = p.getString("currency", null) ?: Currency.INR.name

        val themeMode = runCatching { ThemeMode.valueOf(themeModeStr) }.getOrDefault(ThemeMode.LIGHT)
        val language = runCatching { AppLanguage.valueOf(languageStr) }.getOrDefault(AppLanguage.ENGLISH)
        val unitSystem = runCatching { UnitSystem.valueOf(unitSystemStr) }.getOrDefault(UnitSystem.METRIC)
        val currency = runCatching { Currency.valueOf(currencyStr) }.getOrDefault(Currency.INR)

        return UserSettings(
            profile = UserProfile(
                uid = uid,
                displayName = displayName,
                email = email,
                role = Role.INVENTORY_MANAGER,
                plantId = plantId,
                plantName = plantName,
                warehouseName = warehouseName,
                phone = phone,
                company = company,
            ),
            themeMode = themeMode,
            language = language,
            unitSystem = unitSystem,
            currency = currency,
            notificationPreferences = NotificationPreferences(),
        )
    }

    private val userSettingsFlow = MutableStateFlow(loadInitialSettings())

    private val activePlantIdFlow: Flow<String> = userSettingsFlow
        .map { it.profile.plantId }
        .distinctUntilChanged()

    /** Reads the current plant ID from user settings to avoid hardcoding. */
    private val currentPlantId: String get() = userSettingsFlow.value.profile.plantId

    /** Reads the current organization ID from user settings to avoid hardcoding. */
    private val currentOrgId: String get() = userSettingsFlow.value.profile.orgId

    init {
        appScope.launch {
            seedDefaultsIfEmpty()
        }
        appScope.launch {
            // Keep availablePlants and active plant aligned with Room plants table
            plantDao.observeAllPlants().collect { plants ->
                if (plants.isNotEmpty()) {
                    val plantNames = plants.map { it.name }
                    userSettingsFlow.update { current ->
                        val matchedPlant = plants.find {
                            it.id == current.profile.plantId || it.name.equals(current.profile.plantName, ignoreCase = true)
                        }
                        val next = if (matchedPlant != null) {
                            current.copy(
                                profile = current.profile.copy(
                                    plantId = matchedPlant.id,
                                    plantName = matchedPlant.name,
                                ),
                                availablePlants = plantNames,
                            )
                        } else {
                            val first = plants.first()
                            current.copy(
                                profile = current.profile.copy(
                                    plantId = first.id,
                                    plantName = first.name,
                                ),
                                availablePlants = plantNames,
                            )
                        }
                        persistSettings(next)
                        next
                    }
                }
            }
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
                            } catch (e: Exception) {
                                if (e is kotlinx.coroutines.CancellationException) throw e
                            }
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
        if (plantDao.count() == 0) {
            val now = System.currentTimeMillis()
            plantDao.upsertPlants(
                listOf(
                    PlantEntity("plant_01", "PUN-01", "Pune Plant 1", "Pune, Maharashtra", true, now),
                    PlantEntity("plant_02", "PRD-01", "Plant 01 Production", "Bhosari MIDC, Pune", true, now),
                    PlantEntity("plant_03", "CHK-01", "Chakan Assembly", "Chakan Phase II, Pune", true, now),
                ),
            )
        }

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

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private suspend fun observeAndReconcileNotifications() {
        combine(
            itemDao.observeActiveItems(),
            outboxDao.observeFailedEntries(),
            userSettingsFlow,
        ) { items, failedEntries, settings ->
            Triple(items, failedEntries, settings)
        }.debounce(1000L).collect { (items, failedEntries, settings) ->
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

        for (item in items) {
            val availableMilli = item.onHandMilli - item.reservedMilli
            val thresholdMilli = item.reorderLevelMilli

            if (availableMilli <= 0L) {
                // Out of Stock condition: Q <= 0
                if (prefs.isTypeEnabled(NotificationType.OUT_OF_STOCK)) {
                    val notifId = "out_of_stock_${item.id}"
                    desiredIds.add(notifId)
                    val existing = existingNotifications[notifId]
                    if (existing == null) {
                        notificationDao.insert(
                            NotificationEntity(
                                id = notifId,
                                plantId = currentPlant,
                                type = NotificationType.OUT_OF_STOCK.name,
                                title = "Out of Stock: ${item.name}",
                                body = "${item.name} (${item.code}) is completely out of stock.",
                                entityRef = item.code,
                                isRead = false,
                                createdAt = now,
                            ),
                        )
                    }
                }
            } else if (availableMilli <= thresholdMilli) {
                // Low Stock condition: 0 < Q <= T
                if (prefs.isTypeEnabled(NotificationType.LOW_STOCK)) {
                    val notifId = "low_stock_${item.id}"
                    desiredIds.add(notifId)
                    val existing = existingNotifications[notifId]
                    if (existing == null) {
                        val availQty = Quantity(availableMilli).formatted()
                        val reorderQty = Quantity(thresholdMilli).formatted()
                        notificationDao.insert(
                            NotificationEntity(
                                id = notifId,
                                plantId = currentPlant,
                                type = NotificationType.LOW_STOCK.name,
                                title = "Low Stock: ${item.name}",
                                body = "${item.name} (${item.code}) is below threshold ($availQty vs $reorderQty).",
                                entityRef = item.code,
                                isRead = false,
                                createdAt = now,
                            ),
                        )
                    }
                }
            }
        }

        // 3. Check Sync Failures
        if (prefs.isTypeEnabled(NotificationType.SYNC_FAILURE) && failedEntries.isNotEmpty()) {
            val notifId = "sync_failure"
            desiredIds.add(notifId)
            val existing = existingNotifications[notifId]
            if (existing == null) {
                notificationDao.insert(
                    NotificationEntity(
                        id = notifId,
                        plantId = currentPlant,
                        type = NotificationType.SYNC_FAILURE.name,
                        title = "Sync Alert",
                        body = "${failedEntries.size} offline update(s) failed to sync. Tap to review.",
                        entityRef = null,
                        isRead = false,
                        createdAt = now,
                    ),
                )
            }
        }

        // 4. Remove any automated notifications that are no longer valid (e.g. stock restored > threshold)
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

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeItems(): Flow<List<Item>> =
        activePlantIdFlow.flatMapLatest { plantId ->
            itemDao.observeActiveItems(plantId).map { entities -> entities.map { it.toDomain() } }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeItemsByClass(
        itemClass: ItemClass,
        sortOrder: com.company.stockpro.core.domain.model.SortOrder,
    ): Flow<List<Item>> =
        activePlantIdFlow.flatMapLatest { plantId ->
            val flow = when (sortOrder) {
                com.company.stockpro.core.domain.model.SortOrder.NAME_ASC ->
                    itemDao.observeItemsByClassOrderByName(itemClass.name, plantId)
                com.company.stockpro.core.domain.model.SortOrder.CODE_ASC ->
                    itemDao.observeItemsByClassOrderByCode(itemClass.name, plantId)
                com.company.stockpro.core.domain.model.SortOrder.QUANTITY_DESC ->
                    itemDao.observeItemsByClassOrderByQuantityDesc(itemClass.name, plantId)
                com.company.stockpro.core.domain.model.SortOrder.QUANTITY_ASC ->
                    itemDao.observeItemsByClassOrderByQuantityAsc(itemClass.name, plantId)
                com.company.stockpro.core.domain.model.SortOrder.LAST_MOVED_DESC ->
                    itemDao.observeItemsByClassOrderByLastMoved(itemClass.name, plantId)
            }
            flow.map { entities -> entities.map { it.toDomain() } }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeSummary(): Flow<com.company.stockpro.core.domain.model.InventorySummary> =
        activePlantIdFlow.flatMapLatest { plantId ->
            itemDao.observeSummary(plantId).map { it.toDomain() }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeTopLowStock(limit: Int): Flow<List<Item>> =
        activePlantIdFlow.flatMapLatest { plantId ->
            itemDao.observeTopLowStock(limit, plantId).map { entities -> entities.map { it.toDomain() } }
        }

    override fun observeItem(itemId: String): Flow<Item?> =
        itemDao.observeItem(itemId).map { it?.toDomain() }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun searchItems(query: String): Flow<List<Item>> =
        activePlantIdFlow.flatMapLatest { plantId ->
            val sanitized = query.trim()
                .split(Regex("[^a-zA-Z0-9]+"))
                .filter { it.isNotBlank() }
                .joinToString(" ") { "$it*" }
            if (sanitized.isBlank()) {
                itemDao.observeActiveItems(plantId).map { entities -> entities.map { it.toDomain() } }
            } else {
                itemDao.searchItems(sanitized, plantId).map { entities -> entities.map { it.toDomain() } }
            }
        }

    override fun observeLedger(itemId: String): Flow<List<StockMovement>> =
        ledgerDao.observeByItem(itemId).map { entities -> entities.map { it.toDomain() } }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeRecentMovements(limit: Int): Flow<List<StockMovement>> =
        activePlantIdFlow.flatMapLatest { plantId ->
            ledgerDao.observeRecent(limit, plantId).map { entities -> entities.map { it.toDomain() } }
        }

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
            val plantId = currentPlantId
            val existing = itemDao.getItemByCode(item.code.trim())
            if (existing != null && (existing.locationId == plantId || existing.locationId.isBlank())) {
                return DataResult.Error(UiText.Dynamic("An item with code '${item.code}' already exists in this plant."))
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
                locationId = plantId,
                locationCode = item.locationCode.trim().ifBlank { userSettingsFlow.value.profile.plantName },
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
                    fromLocationId = movement.fromLocationId?.ifBlank { null } ?: currentPlantId,
                    toLocationId = movement.toLocationId?.ifBlank { null } ?: currentPlantId,
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
        userSettingsFlow.update { current ->
            val next = current.copy(profile = current.profile.copy(displayName = displayName.trim()))
            persistSettings(next)
            next
        }
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
        val plant = if (cleanPlant.isNotBlank()) {
            plantDao.getPlantByName(cleanPlant) ?: plantDao.getPlantById(cleanPlant)
        } else null

        val plantId = plant?.id ?: if (cleanPlant.isNotBlank()) {
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
            val next = current.copy(
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
            persistSettings(next)
            next
        }
        return DataResult.Success(Unit)
    }

    override suspend fun selectPlant(plantName: String): DataResult<Unit> {
        val cleanName = plantName.trim()
        if (cleanName.isBlank()) return DataResult.Error(UiText.Dynamic("Plant name cannot be empty"))
        val plant = plantDao.getPlantByName(cleanName)
            ?: plantDao.getPlantById(cleanName)
            ?: plantDao.getPlantByCode(cleanName)
        val plantId = plant?.id ?: ("plant_" + cleanName.lowercase().replace("[^a-z0-9]".toRegex(), "_"))
        val resolvedName = plant?.name ?: cleanName

        userSettingsFlow.update { current ->
            val next = current.copy(
                profile = current.profile.copy(
                    plantName = resolvedName,
                    plantId = plantId,
                ),
            )
            persistSettings(next)
            next
        }
        return DataResult.Success(Unit)
    }

    override suspend fun addPlant(plantName: String): DataResult<Unit> {
        val cleanName = plantName.trim()
        if (cleanName.isBlank()) return DataResult.Error(UiText.Dynamic("Plant name cannot be empty"))
        val code = cleanName.take(3).uppercase() + "-" + (plantDao.count() + 1).toString().padStart(2, '0')
        return when (val result = addPlant(name = cleanName, code = code, location = cleanName)) {
            is DataResult.Success -> DataResult.Success(Unit)
            is DataResult.Error -> DataResult.Error(result.message)
        }
    }

    override suspend fun addPlant(name: String, code: String, location: String): DataResult<Plant> {
        val cleanName = name.trim()
        val cleanCode = code.trim().uppercase()
        val cleanLocation = location.trim()

        if (cleanName.isBlank()) return DataResult.Error(UiText.Dynamic("Plant name is required."))
        if (cleanCode.isBlank()) return DataResult.Error(UiText.Dynamic("Plant code is required."))
        if (cleanLocation.isBlank()) return DataResult.Error(UiText.Dynamic("Plant location is required."))

        val existingByCode = plantDao.getPlantByCode(cleanCode)
        if (existingByCode != null) {
            return DataResult.Error(UiText.Dynamic("Plant code '$cleanCode' already exists."))
        }
        val existingByName = plantDao.getPlantByName(cleanName)
        if (existingByName != null) {
            return DataResult.Error(UiText.Dynamic("Plant named '$cleanName' already exists."))
        }

        val plantId = "plant_" + cleanCode.lowercase().replace("[^a-z0-9]".toRegex(), "_")
        val now = System.currentTimeMillis()
        val entity = PlantEntity(
            id = plantId,
            code = cleanCode,
            name = cleanName,
            location = cleanLocation,
            isActive = true,
            createdAt = now,
        )
        plantDao.insertPlant(entity)

        userSettingsFlow.update { current ->
            val updatedPlants = if (current.availablePlants.contains(cleanName)) current.availablePlants else current.availablePlants + cleanName
            val next = current.copy(
                profile = current.profile.copy(
                    plantId = plantId,
                    plantName = cleanName,
                ),
                availablePlants = updatedPlants,
            )
            persistSettings(next)
            next
        }

        return DataResult.Success(
            Plant(
                id = entity.id,
                code = entity.code,
                name = entity.name,
                location = entity.location,
                isActive = entity.isActive,
                createdAt = entity.createdAt,
            ),
        )
    }

    override fun observePlants(): Flow<List<Plant>> =
        plantDao.observeAllPlants().map { entities ->
            entities.map {
                Plant(
                    id = it.id,
                    code = it.code,
                    name = it.name,
                    location = it.location,
                    isActive = it.isActive,
                    createdAt = it.createdAt,
                )
            }
        }

    override suspend fun getPlantItemCount(plantId: String): Int {
        val clean = plantId.trim()
        val plant = plantDao.getPlantById(clean)
            ?: plantDao.getPlantByName(clean)
            ?: plantDao.getPlantByCode(clean)
        val targetId = plant?.id ?: clean
        return itemDao.countItemsByPlant(targetId)
    }

    override suspend fun deletePlant(plantId: String): DataResult<Unit> {
        val clean = plantId.trim()
        if (clean.isBlank()) {
            return DataResult.Error(UiText.Dynamic("Invalid plant identifier."))
        }

        val activeCount = plantDao.countActivePlants()
        if (activeCount <= 1) {
            return DataResult.Error(UiText.Dynamic("Cannot delete the only remaining plant. StockPro requires at least one active plant."))
        }

        val plant = plantDao.getPlantById(clean)
            ?: plantDao.getPlantByName(clean)
            ?: plantDao.getPlantByCode(clean)
            ?: return DataResult.Error(UiText.Dynamic("Plant not found."))

        plantDao.deletePlantById(plant.id)

        val remainingPlants = plantDao.getAllPlants()
        val remainingNames = remainingPlants.map { it.name }

        userSettingsFlow.update { current ->
            val isCurrent = current.profile.plantId == plant.id ||
                current.profile.plantName.equals(plant.name, ignoreCase = true)
            val fallbackPlant = if (isCurrent) remainingPlants.firstOrNull() else null

            val next = current.copy(
                profile = if (fallbackPlant != null) {
                    current.profile.copy(
                        plantId = fallbackPlant.id,
                        plantName = fallbackPlant.name,
                    )
                } else {
                    current.profile
                },
                availablePlants = remainingNames,
            )
            persistSettings(next)
            next
        }

        return DataResult.Success(Unit)
    }

    override suspend fun updateItemThreshold(
        itemId: String,
        newThresholdMilli: Long,
        operator: String,
    ): DataResult<Unit> {
        if (newThresholdMilli < 0) {
            return DataResult.Error(UiText.Dynamic("Threshold must be a non-negative number."))
        }
        val item = itemDao.getItem(itemId)
            ?: return DataResult.Error(UiText.Dynamic("Item not found."))

        val oldThreshold = item.reorderLevelMilli
        val now = System.currentTimeMillis()

        // 1. Audit trail record
        val auditEntity = ThresholdAuditEntity(
            id = UUID.randomUUID().toString(),
            itemId = item.id,
            sku = item.code,
            plantId = item.locationId.ifBlank { currentPlantId },
            oldThresholdMilli = oldThreshold,
            newThresholdMilli = newThresholdMilli,
            operator = operator.ifBlank { userSettingsFlow.value.profile.displayName },
            timestamp = now,
        )
        thresholdAuditDao.insert(auditEntity)

        // 2. Persist updated threshold to ItemEntity
        val updatedItem = item.copy(
            reorderLevelMilli = newThresholdMilli,
            updatedAt = now,
        )
        itemDao.upsert(updatedItem)

        return DataResult.Success(Unit)
    }

    override fun observeThresholdAudits(itemId: String): Flow<List<ThresholdAudit>> =
        thresholdAuditDao.observeAuditsByItem(itemId).map { entities ->
            entities.map {
                ThresholdAudit(
                    id = it.id,
                    itemId = it.itemId,
                    sku = it.sku,
                    plantId = it.plantId,
                    oldThreshold = Quantity(it.oldThresholdMilli),
                    newThreshold = Quantity(it.newThresholdMilli),
                    operator = it.operator,
                    timestamp = it.timestamp,
                )
            }
        }

    override suspend fun updateThemeMode(themeMode: ThemeMode) {
        userSettingsFlow.update { current ->
            val next = current.copy(themeMode = themeMode)
            persistSettings(next)
            next
        }
    }

    override suspend fun updateLanguage(language: AppLanguage) {
        userSettingsFlow.update { current ->
            val next = current.copy(language = language)
            persistSettings(next)
            next
        }
    }

    override suspend fun updateUnitSystem(unitSystem: UnitSystem) {
        userSettingsFlow.update { current ->
            val next = current.copy(unitSystem = unitSystem)
            persistSettings(next)
            next
        }
    }

    override suspend fun updateCurrency(currency: Currency) {
        userSettingsFlow.update { current ->
            val next = current.copy(currency = currency)
            persistSettings(next)
            next
        }
    }

    override suspend fun clearCache(): DataResult<Long> {
        return DataResult.Success(0L)
    }

    override suspend fun logout(): DataResult<Unit> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        try {
            prefs?.edit()?.clear()?.apply()
            userSettingsFlow.update {
                loadInitialSettings().copy(
                    profile = UserProfile(
                        uid = "",
                        displayName = "User",
                        email = "",
                        role = Role.INVENTORY_MANAGER,
                        plantId = "plant_01",
                        plantName = "Pune Plant 1",
                        orgId = "org_demo",
                    ),
                )
            }
            try {
                com.google.firebase.messaging.FirebaseMessaging.getInstance()
                    .unsubscribeFromTopic("plant_$currentPlantId")
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            }
            try {
                com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            }
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
