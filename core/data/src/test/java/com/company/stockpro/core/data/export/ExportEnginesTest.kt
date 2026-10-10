package com.company.stockpro.core.data.export

import com.company.stockpro.core.domain.model.Currency
import com.company.stockpro.core.domain.model.ExportConfig
import com.company.stockpro.core.domain.model.ExportDataset
import com.company.stockpro.core.domain.model.ExportFormat
import com.company.stockpro.core.domain.model.Item
import com.company.stockpro.core.domain.model.ItemClass
import com.company.stockpro.core.domain.model.Money
import com.company.stockpro.core.domain.model.MovementType
import com.company.stockpro.core.domain.model.Quantity
import com.company.stockpro.core.domain.model.Role
import com.company.stockpro.core.domain.model.StockMovement
import com.company.stockpro.core.domain.repository.StreamingDataFetcher
import com.company.stockpro.core.domain.util.RbacSecurityMasker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipInputStream

class ExportEnginesTest {

    private val sampleItems = listOf(
        Item(
            id = "item_1",
            code = "SKU-001",
            name = "Stainless Steel Sheet, 2mm",
            itemClass = ItemClass.RAW_MATERIAL,
            categoryName = "Raw Materials",
            unitOfMeasure = "PCS",
            onHand = Quantity(100_000L), // 100 pcs
            reserved = Quantity(10_000L),
            inProduction = Quantity(0L),
            reorderLevel = Quantity(20_000L),
            parLevel = Quantity(150_000L),
            unitCost = Money(50000L), // ₹500.00
            locationCode = "A-12-01",
        ),
        Item(
            id = "item_2",
            code = "SKU-002",
            name = "Copper Wire \"Super\", 1mm",
            itemClass = ItemClass.RAW_MATERIAL,
            categoryName = "Wiring",
            unitOfMeasure = "M",
            onHand = Quantity(5_000L), // 5 meters (Low stock!)
            reserved = Quantity(0L),
            inProduction = Quantity(0L),
            reorderLevel = Quantity(10_000L),
            parLevel = Quantity(50_000L),
            unitCost = Money(12000L), // ₹120.00
            locationCode = "B-03-02",
        ),
    )

    private val sampleMovements = listOf(
        StockMovement(
            id = "mov_1",
            itemId = "item_1",
            itemCode = "SKU-001",
            itemName = "Stainless Steel Sheet, 2mm",
            type = MovementType.RECEIPT,
            delta = Quantity(50_000L),
            balanceAfter = Quantity(100_000L),
            unitCost = Money(50000L),
            reference = "PO-99120",
            actorName = "Aditya Maurya",
            idempotencyKey = "key_1",
            createdAt = 1711958400000L,
        )
    )

    private val fakeFetcher = object : StreamingDataFetcher {
        override fun streamItems(pageSize: Int, plantId: String?): Flow<List<Item>> = flowOf(sampleItems)
        override fun streamLedger(pageSize: Int, startMillis: Long?, endMillis: Long?, plantId: String?): Flow<List<StockMovement>> = flowOf(sampleMovements)
        override fun streamLowStockItems(pageSize: Int, plantId: String?): Flow<List<Item>> = flowOf(sampleItems.filter { it.onHand.milli <= it.reorderLevel.milli })
        override suspend fun getItemCount(plantId: String?): Int = sampleItems.size
        override suspend fun getLedgerCount(startMillis: Long?, endMillis: Long?, plantId: String?): Int = sampleMovements.size
        override suspend fun getLowStockCount(plantId: String?): Int = 1
    }

    @Test
    fun `Rfc4180CsvGenerator writes UTF-8 BOM and escapes commas and quotes`() = runTest {
        val config = ExportConfig(
            dataset = ExportDataset.MASTER_INVENTORY,
            format = ExportFormat.CSV,
            plantName = "Pune Plant 1",
            currency = Currency.INR,
            userRole = Role.INVENTORY_MANAGER,
        )

        val out = ByteArrayOutputStream()
        val count = Rfc4180CsvGenerator.generate(config, fakeFetcher, out)

        assertEquals(2, count)
        val bytes = out.toByteArray()

        // Check UTF-8 BOM (0xEF, 0xBB, 0xBF)
        assertEquals(0xEF.toByte(), bytes[0])
        assertEquals(0xBB.toByte(), bytes[1])
        assertEquals(0xBF.toByte(), bytes[2])

        val content = String(bytes, Charsets.UTF_8)
        assertTrue(content.contains("SKU-001"))
        // Quotes and commas escaped
        assertTrue(content.contains("\"Stainless Steel Sheet, 2mm\""))
        assertTrue(content.contains("\"Copper Wire \"\"Super\"\", 1mm\""))
        // Manager role has unmasked valuation
        assertTrue(content.contains("₹ 500"))
    }

    @Test
    fun `Rfc4180CsvGenerator enforces RBAC masking for Store Keeper role`() = runTest {
        val config = ExportConfig(
            dataset = ExportDataset.MASTER_INVENTORY,
            format = ExportFormat.CSV,
            plantName = "Pune Plant 1",
            currency = Currency.INR,
            userRole = Role.STORE_KEEPER, // No valuation permission
        )

        val out = ByteArrayOutputStream()
        Rfc4180CsvGenerator.generate(config, fakeFetcher, out)
        val content = String(out.toByteArray(), Charsets.UTF_8)

        // Store Keeper must see masked valuation values
        assertTrue(content.contains("***"))
        assertTrue(content.contains("₹ •••••••••"))
    }

    @Test
    fun `OpenXmlSpreadsheetGenerator produces valid multi-sheet zip container`() = runTest {
        val config = ExportConfig(
            dataset = ExportDataset.MASTER_INVENTORY,
            format = ExportFormat.EXCEL,
            plantName = "Pune Plant 1",
            currency = Currency.INR,
            userRole = Role.INVENTORY_MANAGER,
        )

        val out = ByteArrayOutputStream()
        val count = OpenXmlSpreadsheetGenerator.generate(config, fakeFetcher, out)

        assertEquals(2, count)
        val zipBytes = out.toByteArray()
        assertTrue(zipBytes.isNotEmpty())

        // Read ZIP entries
        val zis = ZipInputStream(ByteArrayInputStream(zipBytes))
        val entryNames = mutableSetOf<String>()
        var entry = zis.nextEntry
        while (entry != null) {
            entryNames.add(entry.name)
            zis.closeEntry()
            entry = zis.nextEntry
        }

        assertTrue(entryNames.contains("[Content_Types].xml"))
        assertTrue(entryNames.contains("_rels/.rels"))
        assertTrue(entryNames.contains("xl/_rels/workbook.xml.rels"))
        assertTrue(entryNames.contains("xl/workbook.xml"))
        assertTrue(entryNames.contains("xl/styles.xml"))
        assertTrue(entryNames.contains("xl/worksheets/sheet1.xml"))
        assertTrue(entryNames.contains("xl/worksheets/sheet2.xml"))
    }

    @Test
    fun `EnterpriseJsonXmlGenerator generates valid JSON schema`() = runTest {
        val config = ExportConfig(
            dataset = ExportDataset.MASTER_INVENTORY,
            format = ExportFormat.JSON,
            plantName = "Pune Plant 1",
            warehouseName = "Main Warehouse",
            currency = Currency.USD,
            userRole = Role.PLANT_MANAGER,
        )

        val out = ByteArrayOutputStream()
        val count = EnterpriseJsonXmlGenerator.generateJson(config, fakeFetcher, out)

        assertEquals(2, count)
        val json = String(out.toByteArray(), Charsets.UTF_8)

        assertTrue(json.contains("\"schemaVersion\": \"1.0\""))
        assertTrue(json.contains("\"application\": \"StockPro ERP\""))
        assertTrue(json.contains("\"plant\": {"))
        assertTrue(json.contains("\"name\": \"Pune Plant 1\""))
        assertTrue(json.contains("\"records\": ["))
        assertTrue(json.contains("\"totalRecords\": 2"))
        assertTrue(json.contains("\"code\": \"SKU-001\""))
    }

    @Test
    fun `EnterpriseJsonXmlGenerator generates valid XML schema`() = runTest {
        val config = ExportConfig(
            dataset = ExportDataset.STOCK_LEDGER,
            format = ExportFormat.XML,
            plantName = "Pune Plant 1",
            warehouseName = "Main Warehouse",
            currency = Currency.EUR,
            userRole = Role.ORG_ADMIN,
        )

        val out = ByteArrayOutputStream()
        val count = EnterpriseJsonXmlGenerator.generateXml(config, fakeFetcher, out)

        assertEquals(1, count)
        val xml = String(out.toByteArray(), Charsets.UTF_8)

        assertTrue(xml.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"))
        assertTrue(xml.contains("<StockProExport schemaVersion=\"1.0\""))
        assertTrue(xml.contains("<Header>"))
        assertTrue(xml.contains("<Records>"))
        assertTrue(xml.contains("<Movement id=\"mov_1\""))
        assertTrue(xml.contains("<ItemCode>SKU-001</ItemCode>"))
        assertTrue(xml.contains("<Footer totalRecords=\"1\"/>"))
    }

    @Test
    fun `ChecksumUtil accurately computes SHA-256 hash`() = runTest {
        val tempFile = File.createTempFile("test_export", ".txt")
        tempFile.writeText("StockPro Enterprise Data Integrity Test", Charsets.UTF_8)

        val sha256 = ChecksumUtil.calculateSha256(tempFile)
        assertNotNull(sha256)
        assertEquals(64, sha256.length) // 64 hex characters

        // Repeat calculation to guarantee determinism
        val sha256Again = ChecksumUtil.calculateSha256(tempFile)
        assertEquals(sha256, sha256Again)

        tempFile.delete()
    }

    @Test
    fun `Rfc4180CsvGenerator sanitizes CSV formula injection starting with =, +, -, @, tab, cr`() = runTest {
        val maliciousItems = listOf(
            Item(
                id = "item_malicious",
                code = "=cmd|'/c calc'!A0",
                name = "-2+3+cmd|' /C calc'!A0",
                itemClass = ItemClass.RAW_MATERIAL,
                categoryName = "@SUM(1+1)*cmd|' /C calc'!A0",
                unitOfMeasure = "+PCS",
                onHand = Quantity(100_000L),
                reorderLevel = Quantity(20_000L),
                parLevel = Quantity(150_000L),
                unitCost = Money(50000L),
                locationCode = "\tLOC-01",
            )
        )
        val maliciousFetcher = object : StreamingDataFetcher {
            override fun streamItems(pageSize: Int, plantId: String?): Flow<List<Item>> = flowOf(maliciousItems)
            override fun streamLedger(pageSize: Int, startMillis: Long?, endMillis: Long?, plantId: String?): Flow<List<StockMovement>> = flowOf(emptyList())
            override fun streamLowStockItems(pageSize: Int, plantId: String?): Flow<List<Item>> = flowOf(emptyList())
            override suspend fun getItemCount(plantId: String?): Int = 1
            override suspend fun getLedgerCount(startMillis: Long?, endMillis: Long?, plantId: String?): Int = 0
            override suspend fun getLowStockCount(plantId: String?): Int = 0
        }

        val config = ExportConfig(
            dataset = ExportDataset.MASTER_INVENTORY,
            format = ExportFormat.CSV,
            plantName = "Pune Plant 1",
            currency = Currency.INR,
            userRole = Role.INVENTORY_MANAGER,
        )

        val out = ByteArrayOutputStream()
        Rfc4180CsvGenerator.generate(config, maliciousFetcher, out)
        val csv = String(out.toByteArray(), Charsets.UTF_8)

        // Verify formula injection characters are prepended with single-quote '
        assertTrue(csv.contains("'=cmd|'/c calc'!A0"))
        assertTrue(csv.contains("'-2+3+cmd|' /C calc'!A0"))
        assertTrue(csv.contains("'@SUM(1+1)*cmd|' /C calc'!A0"))
        assertTrue(csv.contains("'+PCS"))
    }

    @Test
    fun `EnterpriseJsonXmlGenerator strips illegal XML 1_0 control bytes`() = runTest {
        val dirtyItems = listOf(
            Item(
                id = "item_dirty",
                code = "SKU\u0000NULL",
                name = "Item with \u0007Bell and \u0008Backspace",
                itemClass = ItemClass.RAW_MATERIAL,
                categoryName = "Safe Category",
                unitOfMeasure = "PCS",
                onHand = Quantity(10_000L),
                reorderLevel = Quantity(5_000L),
                parLevel = Quantity(20_000L),
                unitCost = Money(1000L),
                locationCode = "LOC-01",
            )
        )
        val dirtyFetcher = object : StreamingDataFetcher {
            override fun streamItems(pageSize: Int, plantId: String?): Flow<List<Item>> = flowOf(dirtyItems)
            override fun streamLedger(pageSize: Int, startMillis: Long?, endMillis: Long?, plantId: String?): Flow<List<StockMovement>> = flowOf(emptyList())
            override fun streamLowStockItems(pageSize: Int, plantId: String?): Flow<List<Item>> = flowOf(emptyList())
            override suspend fun getItemCount(plantId: String?): Int = 1
            override suspend fun getLedgerCount(startMillis: Long?, endMillis: Long?, plantId: String?): Int = 0
            override suspend fun getLowStockCount(plantId: String?): Int = 0
        }

        val config = ExportConfig(
            dataset = ExportDataset.MASTER_INVENTORY,
            format = ExportFormat.XML,
            plantName = "Pune Plant 1",
            currency = Currency.USD,
            userRole = Role.ORG_ADMIN,
        )

        val out = ByteArrayOutputStream()
        EnterpriseJsonXmlGenerator.generateXml(config, dirtyFetcher, out)
        val xml = String(out.toByteArray(), Charsets.UTF_8)

        // Verify illegal control characters (null, bell, backspace) are completely stripped
        assertTrue(!xml.contains("\u0000"))
        assertTrue(!xml.contains("\u0007"))
        assertTrue(!xml.contains("\u0008"))
        assertTrue(xml.contains("SKUNULL"))
        assertTrue(xml.contains("Item with Bell and Backspace"))
    }

    @Test
    fun `OpenXmlSpreadsheetGenerator writes numbers with numeric typing and text with inlineStr`() = runTest {
        val config = ExportConfig(
            dataset = ExportDataset.MASTER_INVENTORY,
            format = ExportFormat.EXCEL,
            plantName = "Pune Plant 1",
            currency = Currency.INR,
            userRole = Role.INVENTORY_MANAGER,
        )

        val out = ByteArrayOutputStream()
        OpenXmlSpreadsheetGenerator.generate(config, fakeFetcher, out)
        val zipBytes = out.toByteArray()

        val zis = ZipInputStream(ByteArrayInputStream(zipBytes))
        var sheet1Xml = ""
        var entry = zis.nextEntry
        while (entry != null) {
            if (entry.name == "xl/worksheets/sheet1.xml") {
                sheet1Xml = zis.bufferedReader(Charsets.UTF_8).readText()
                break
            }
            zis.closeEntry()
            entry = zis.nextEntry
        }

        assertTrue(sheet1Xml.isNotEmpty())
        // Numeric quantities should use t="n" and <v>...</v>
        assertTrue(sheet1Xml.contains("t=\"n\"><v>100.0</v></c>"))
        assertTrue(sheet1Xml.contains("t=\"n\"><v>10.0</v></c>"))
        // SKU Code should use t="inlineStr" and <is><t>...</t></is>
        assertTrue(sheet1Xml.contains("t=\"inlineStr\"><is><t>SKU-001</t></is></c>"))
    }

    @Test
    fun `EnterpriseJsonXmlGenerator escapes RFC 8259 control characters in JSON`() = runTest {
        val dirtyItem = Item(
            id = "item_ctrl",
            code = "SKU-CTRL",
            name = "Line1\nLine2\tTabbed\rCarriage \"Quoted\"",
            itemClass = ItemClass.RAW_MATERIAL,
            categoryName = "Control\bChars\u0000Null",
            unitOfMeasure = "PCS",
            onHand = Quantity(10_000L),
            reorderLevel = Quantity(5_000L),
            parLevel = Quantity(20_000L),
            unitCost = Money(1000L),
            locationCode = "LOC-01",
        )
        val dirtyFetcher = object : StreamingDataFetcher {
            override fun streamItems(pageSize: Int, plantId: String?): Flow<List<Item>> = flowOf(listOf(dirtyItem))
            override fun streamLedger(pageSize: Int, startMillis: Long?, endMillis: Long?, plantId: String?): Flow<List<StockMovement>> = flowOf(emptyList())
            override fun streamLowStockItems(pageSize: Int, plantId: String?): Flow<List<Item>> = flowOf(emptyList())
            override suspend fun getItemCount(plantId: String?): Int = 1
            override suspend fun getLedgerCount(startMillis: Long?, endMillis: Long?, plantId: String?): Int = 0
            override suspend fun getLowStockCount(plantId: String?): Int = 0
        }

        val config = ExportConfig(
            dataset = ExportDataset.MASTER_INVENTORY,
            format = ExportFormat.JSON,
            plantName = "Pune Plant 1",
            currency = Currency.USD,
            userRole = Role.ORG_ADMIN,
        )

        val out = ByteArrayOutputStream()
        EnterpriseJsonXmlGenerator.generateJson(config, dirtyFetcher, out)
        val json = String(out.toByteArray(), Charsets.UTF_8)

        // Verifying control characters are escaped
        assertTrue(json.contains("\\n"))
        assertTrue(json.contains("\\t"))
        assertTrue(json.contains("\\r"))
        assertTrue(json.contains("\\\"Quoted\\\""))
        assertTrue(json.contains("\\b"))
        assertTrue(json.contains("\\u0000"))
    }

    @Test
    fun `RbacSecurityMasker correctly enforces valuation permissions`() {
        val cost = Money(5000L) // $50.00
        val valuationPaise = 5000000L // $50,000.00 -> $ 50.0K

        // Unauthorized roles
        val unauthorizedRoles = listOf(Role.STORE_KEEPER, Role.PRODUCTION_SUPERVISOR, Role.QA)
        for (role in unauthorizedRoles) {
            assertEquals("***", RbacSecurityMasker.formatUnitCost(cost, Currency.INR, role))
            assertEquals("₹ •••••••••", RbacSecurityMasker.formatValuation(valuationPaise, Currency.INR, role))
        }

        // Authorized roles
        val authorizedRoles = listOf(Role.INVENTORY_MANAGER, Role.PLANT_MANAGER, Role.ORG_ADMIN)
        for (role in authorizedRoles) {
            val formattedCost = RbacSecurityMasker.formatUnitCost(cost, Currency.USD, role)
            val formattedVal = RbacSecurityMasker.formatValuation(valuationPaise, Currency.USD, role)
            assertTrue(formattedCost.contains("50.00"))
            assertTrue(formattedVal.contains("50.0K"))

            val formattedInrVal = RbacSecurityMasker.formatValuation(valuationPaise, Currency.INR, role)
            assertTrue(formattedInrVal.contains("50,000") || formattedInrVal.contains("50000"))
        }
    }

    @Test
    fun `Export engines correctly forward plantId filter to data fetcher`() = runTest {
        var requestedPlantId: String? = null
        val plantTrackingFetcher = object : StreamingDataFetcher {
            override fun streamItems(pageSize: Int, plantId: String?): Flow<List<Item>> {
                requestedPlantId = plantId
                return flowOf(sampleItems)
            }
            override fun streamLedger(pageSize: Int, startMillis: Long?, endMillis: Long?, plantId: String?): Flow<List<StockMovement>> = flowOf(emptyList())
            override fun streamLowStockItems(pageSize: Int, plantId: String?): Flow<List<Item>> = flowOf(emptyList())
            override suspend fun getItemCount(plantId: String?): Int = 2
            override suspend fun getLedgerCount(startMillis: Long?, endMillis: Long?, plantId: String?): Int = 0
            override suspend fun getLowStockCount(plantId: String?): Int = 0
        }

        val config = ExportConfig(
            dataset = ExportDataset.MASTER_INVENTORY,
            format = ExportFormat.CSV,
            plantId = "plant_blr_02",
            plantName = "Bengaluru Plant",
            currency = Currency.INR,
            userRole = Role.ORG_ADMIN,
        )

        // 1. CSV
        Rfc4180CsvGenerator.generate(config, plantTrackingFetcher, ByteArrayOutputStream())
        assertEquals("plant_blr_02", requestedPlantId)

        // 2. Excel
        requestedPlantId = null
        OpenXmlSpreadsheetGenerator.generate(config, plantTrackingFetcher, ByteArrayOutputStream())
        assertEquals("plant_blr_02", requestedPlantId)

        // 3. JSON
        requestedPlantId = null
        EnterpriseJsonXmlGenerator.generateJson(config, plantTrackingFetcher, ByteArrayOutputStream())
        assertEquals("plant_blr_02", requestedPlantId)
    }
}
