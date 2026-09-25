package com.company.stockpro.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolCustodyTest {

    @Test
    fun `deriveCalibrationStatus returns VALID when next due date is null`() {
        val custody = ToolCustody(itemId = "tool-1", nextCalibrationDueAt = null)
        assertEquals(CalibrationStatus.VALID, custody.deriveCalibrationStatus(System.currentTimeMillis()))
    }

    @Test
    fun `deriveCalibrationStatus returns OVERDUE when current time exceeds due date`() {
        val now = 1_700_000_000_000L
        val pastDue = now - 1000L
        val custody = ToolCustody(itemId = "tool-1", nextCalibrationDueAt = pastDue)
        assertEquals(CalibrationStatus.OVERDUE, custody.deriveCalibrationStatus(now))
    }

    @Test
    fun `deriveCalibrationStatus returns DUE_SOON when due within 7 days`() {
        val now = 1_700_000_000_000L
        val dueIn3Days = now + (3L * 24 * 60 * 60 * 1000L)
        val custody = ToolCustody(itemId = "tool-1", nextCalibrationDueAt = dueIn3Days)
        assertEquals(CalibrationStatus.DUE_SOON, custody.deriveCalibrationStatus(now))
    }

    @Test
    fun `deriveCalibrationStatus returns VALID when due beyond 7 days`() {
        val now = 1_700_000_000_000L
        val dueIn30Days = now + (30L * 24 * 60 * 60 * 1000L)
        val custody = ToolCustody(itemId = "tool-1", nextCalibrationDueAt = dueIn30Days)
        assertEquals(CalibrationStatus.VALID, custody.deriveCalibrationStatus(now))
    }

    @Test
    fun `daysUntilDue calculates remaining days accurately`() {
        val now = 1_700_000_000_000L
        val dueIn5Days = now + (5L * 24 * 60 * 60 * 1000L)
        val custody = ToolCustody(itemId = "tool-1", nextCalibrationDueAt = dueIn5Days)
        assertEquals(5L, custody.daysUntilDue(now))
    }

    @Test
    fun `role permission matrix enforces architectural security boundaries`() {
        // STORE_KEEPER
        assertFalse(Role.STORE_KEEPER.canManageTeam)
        assertFalse(Role.STORE_KEEPER.canViewValuation)
        assertFalse(Role.STORE_KEEPER.canAdjustStock)

        // PRODUCTION_SUPERVISOR
        assertFalse(Role.PRODUCTION_SUPERVISOR.canManageTeam)
        assertFalse(Role.PRODUCTION_SUPERVISOR.canViewValuation)
        assertFalse(Role.PRODUCTION_SUPERVISOR.canAdjustStock)

        // INVENTORY_MANAGER
        assertFalse(Role.INVENTORY_MANAGER.canManageTeam)
        assertTrue(Role.INVENTORY_MANAGER.canViewValuation)
        assertTrue(Role.INVENTORY_MANAGER.canAdjustStock)

        // PLANT_MANAGER
        assertTrue(Role.PLANT_MANAGER.canManageTeam)
        assertTrue(Role.PLANT_MANAGER.canViewValuation)
        assertTrue(Role.PLANT_MANAGER.canAdjustStock)
        assertTrue(Role.PLANT_MANAGER.canDeactivateItem)

        // ORG_ADMIN
        assertTrue(Role.ORG_ADMIN.canManageTeam)
        assertTrue(Role.ORG_ADMIN.canViewValuation)
        assertTrue(Role.ORG_ADMIN.canAdjustStock)
        assertTrue(Role.ORG_ADMIN.canDeactivateItem)
    }

    @Test
    fun `notification preferences correctly filter discrete alert categories per PRD 8 2`() {
        val prefs = NotificationPreferences(
            lowStockEnabled = true,
            outOfStockEnabled = false,
            poDueEnabled = true,
            largeAdjustmentEnabled = false,
            syncFailureEnabled = true,
        )

        assertTrue(prefs.isTypeEnabled(NotificationType.LOW_STOCK))
        assertFalse(prefs.isTypeEnabled(NotificationType.OUT_OF_STOCK))
        assertTrue(prefs.isTypeEnabled(NotificationType.PO_DUE))
        assertFalse(prefs.isTypeEnabled(NotificationType.LARGE_ADJUSTMENT))
        assertTrue(prefs.isTypeEnabled(NotificationType.SYNC_FAILURE))
    }
}
