package com.company.stockpro.core.data.export

import com.company.stockpro.core.data.repository.ExportHistoryRepositoryImpl
import com.company.stockpro.core.database.StockProDatabase
import com.company.stockpro.core.database.dao.ExportHistoryDao
import com.company.stockpro.core.database.dao.ExportScheduleDao
import com.company.stockpro.core.database.entity.ExportHistoryEntity
import com.company.stockpro.core.domain.model.ExportDataset
import com.company.stockpro.core.domain.model.ExportFormat
import com.company.stockpro.core.domain.model.ExportHistoryRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.UUID

class ExportHistoryRepositoryTest {

    private lateinit var fakeDao: FakeExportHistoryDao
    private lateinit var fakeDatabase: FakeStockProDatabase
    private lateinit var repository: ExportHistoryRepositoryImpl

    @Before
    fun setUp() {
        fakeDao = FakeExportHistoryDao()
        fakeDatabase = FakeStockProDatabase(fakeDao)
        repository = ExportHistoryRepositoryImpl(fakeDatabase)
    }

    @Test
    fun `deleteExportHistory physically deletes file from filesystem and deletes DB row`() = runTest {
        val tempFile = File.createTempFile("stockpro_test_export_", ".csv")
        tempFile.writeText("sample data")
        assertTrue(tempFile.exists())

        val recordId = UUID.randomUUID().toString()
        val entity = ExportHistoryEntity(
            id = recordId,
            timestamp = System.currentTimeMillis(),
            dataset = ExportDataset.MASTER_INVENTORY.name,
            format = ExportFormat.CSV.extension,
            recordCount = 10,
            fileSizeBytes = tempFile.length(),
            filePath = tempFile.absolutePath,
            fileName = tempFile.name,
            sha256Checksum = "dummy_sha256",
            initiatedBy = "Admin",
            plantName = "Pune Plant",
            status = "SUCCESS",
        )
        fakeDao.insert(entity)

        repository.deleteExportHistory(recordId)

        // DB record should be deleted
        assertEquals(null, fakeDao.getById(recordId))
        // Physical file should be purged from disk
        assertFalse(tempFile.exists())
    }

    @Test
    fun `clearExportHistory physically purges all files on disk before clearing database`() = runTest {
        val file1 = File.createTempFile("export_1_", ".xlsx").apply { writeText("file1 data") }
        val file2 = File.createTempFile("export_2_", ".pdf").apply { writeText("file2 data") }
        assertTrue(file1.exists())
        assertTrue(file2.exists())

        val entity1 = ExportHistoryEntity(
            id = "id_1",
            timestamp = System.currentTimeMillis(),
            dataset = ExportDataset.MASTER_INVENTORY.name,
            format = ExportFormat.EXCEL.extension,
            recordCount = 20,
            fileSizeBytes = file1.length(),
            filePath = file1.absolutePath,
            fileName = file1.name,
            sha256Checksum = "dummy1",
            initiatedBy = "Admin",
            plantName = "Pune Plant",
            status = "SUCCESS",
        )
        val entity2 = ExportHistoryEntity(
            id = "id_2",
            timestamp = System.currentTimeMillis(),
            dataset = ExportDataset.STOCK_LEDGER.name,
            format = ExportFormat.PDF.extension,
            recordCount = 5,
            fileSizeBytes = file2.length(),
            filePath = file2.absolutePath,
            fileName = file2.name,
            sha256Checksum = "dummy2",
            initiatedBy = "Admin",
            plantName = "Pune Plant",
            status = "SUCCESS",
        )
        fakeDao.insert(entity1)
        fakeDao.insert(entity2)

        repository.clearExportHistory()

        // DB records should all be gone
        assertTrue(fakeDao.getAll().isEmpty())
        // Files on disk should be physically deleted
        assertFalse(file1.exists())
        assertFalse(file2.exists())
    }

    @Test
    fun `recordExport properly writes entity to DAO`() = runTest {
        val record = ExportHistoryRecord(
            id = "rec_001",
            timestamp = 1700000000000L,
            dataset = ExportDataset.LOW_STOCK_ALERT,
            format = ExportFormat.JSON,
            recordCount = 15,
            fileSizeBytes = 2048L,
            filePath = "/data/export.json",
            fileName = "export.json",
            sha256Checksum = "sha256_hash",
            initiatedBy = "Supervisor",
            plantName = "Mumbai Plant",
            status = "SUCCESS",
        )

        repository.recordExport(record)

        val retrieved = fakeDao.getById("rec_001")
        assertTrue(retrieved != null)
        assertEquals("rec_001", retrieved?.id)
        assertEquals("LOW_STOCK_ALERT", retrieved?.dataset)
        assertEquals("json", retrieved?.format)
        assertEquals(15, retrieved?.recordCount)
    }
}

private class FakeExportHistoryDao : ExportHistoryDao {
    private val records = mutableMapOf<String, ExportHistoryEntity>()

    override fun observeAll(): Flow<List<ExportHistoryEntity>> = flowOf(records.values.toList())

    override suspend fun insert(entity: ExportHistoryEntity) {
        records[entity.id] = entity
    }

    override suspend fun getById(id: String): ExportHistoryEntity? = records[id]

    override suspend fun deleteById(id: String) {
        records.remove(id)
    }

    override suspend fun getAll(): List<ExportHistoryEntity> = records.values.toList()

    override suspend fun clearAll() {
        records.clear()
    }
}

private class FakeStockProDatabase(
    private val historyDao: ExportHistoryDao,
) : StockProDatabase() {
    override fun exportHistoryDao(): ExportHistoryDao = historyDao
    override fun exportScheduleDao(): ExportScheduleDao = throw UnsupportedOperationException()
    override fun itemDao() = throw UnsupportedOperationException()
    override fun ledgerDao() = throw UnsupportedOperationException()
    override fun outboxDao() = throw UnsupportedOperationException()
    override fun notificationDao() = throw UnsupportedOperationException()
    override fun toolCustodyDao() = throw UnsupportedOperationException()
    override fun teamMemberDao() = throw UnsupportedOperationException()
    override fun plantDao() = throw UnsupportedOperationException()
    override fun thresholdAuditDao() = throw UnsupportedOperationException()
    override fun clearAllTables() {}
    override fun createInvalidationTracker(): androidx.room.InvalidationTracker {
        return androidx.room.InvalidationTracker(this, "export_history", "export_schedules")
    }
    override fun createOpenHelper(config: androidx.room.DatabaseConfiguration) = throw UnsupportedOperationException()
}
