package com.company.stockpro.core.data.repository

import com.company.stockpro.core.database.StockProDatabase
import com.company.stockpro.core.database.entity.NotificationEntity
import com.company.stockpro.core.database.entity.OutboxEntity
import com.company.stockpro.core.database.entity.StockLedgerEntity
import com.company.stockpro.core.database.entity.TeamMemberEntity
import com.company.stockpro.core.database.entity.ToolCustodyEntity
import com.company.stockpro.core.domain.model.AppLanguage
import com.company.stockpro.core.domain.model.AppNotification
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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

    init {
        appScope.launch {
            seedDefaultsIfEmpty()
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
                                syncManager?.pullDelta("org_01", currentPlantId)
                            } catch (e: Exception) {
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

        if (notificationDao.count() == 0) {
            val now = System.currentTimeMillis()
            notificationDao.insertAll(
                listOf(
                    NotificationEntity("notif_1", "plant_01", NotificationType.LOW_STOCK.name, "Low Stock Alert", "Ball Bearing is below minimum par level (5 on hand vs 500 par).", "BRG-001", false, now - 1800000),
                    NotificationEntity("notif_2", "plant_01", NotificationType.PO_DUE.name, "Purchase Order Due", "PO-2024-0142 scheduled delivery expected this week.", null, false, now - 7200000),
                    NotificationEntity("notif_3", "plant_01", NotificationType.SYNC_FAILURE.name, "Sync Alert", "1 background movement queued offline and awaiting network sync.", null, true, now - 86400000),
                ),
            )
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

    override fun observeItem(itemId: String): Flow<Item?> =
        itemDao.observeItem(itemId).map { it?.toDomain() }

    override fun searchItems(query: String): Flow<List<Item>> {
        // Room FTS MATCH requires a sanitised query with trailing wildcard
        val ftsQuery = query.trim().replace("\"", "").let { "$it*" }
        return itemDao.searchItems(ftsQuery).map { entities -> entities.map { it.toDomain() } }
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
                    syncManager?.pushItem("org_01", currentPlantId, entity)
                } catch (e: Exception) {
                    android.util.Log.w("InventoryRepo", "Background pushItem failed: ${e.message}")
                }
            }
            DataResult.Success(entity.toDomain())
        } catch (e: Exception) {
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

    override fun observeNotifications(): Flow<List<AppNotification>> =
        notificationDao.observeAll(currentPlantId).map { entities -> entities.map { it.toDomain() } }

    override fun observeUnreadNotificationCount(): Flow<Int> =
        notificationDao.observeUnreadCount(currentPlantId)

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

    override suspend fun updateThemeMode(themeMode: ThemeMode) {
        userSettingsFlow.update { it.copy(themeMode = themeMode) }
    }

    override suspend fun updateLanguage(language: AppLanguage) {
        userSettingsFlow.update { it.copy(language = language) }
    }

    override suspend fun updateUnitSystem(unitSystem: UnitSystem) {
        userSettingsFlow.update { it.copy(unitSystem = unitSystem) }
    }

    override suspend fun clearCache(): DataResult<Long> {
        // No real cache to clear in Room-only architecture
        return DataResult.Success(0L)
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
            append("\"orgId\":\"org_01\",")
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
