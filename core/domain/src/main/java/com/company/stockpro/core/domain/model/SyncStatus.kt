package com.company.stockpro.core.domain.model

/**
 * App-wide synchronization status representation (architecture.md §7.2).
 *
 * Surfaced in:
 * - App-level sync banner
 * - Settings sync-status row ("Synced · 2m ago" / "Syncing…" / "3 pending" / "1 needs attention")
 */
sealed interface SyncStatus {
    /** All local changes synced with server. */
    data class Synced(val lastSyncedEpochMs: Long) : SyncStatus

    /** Currently pulling deltas or draining the outbox. */
    data object Syncing : SyncStatus

    /** Offline or waiting with queued local mutations. */
    data class Pending(val count: Int) : SyncStatus

    /** One or more mutations rejected by server (4xx) requiring user action. */
    data class NeedsAttention(val failedCount: Int) : SyncStatus
}
