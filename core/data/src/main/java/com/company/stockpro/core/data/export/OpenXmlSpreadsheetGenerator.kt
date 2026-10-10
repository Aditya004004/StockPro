package com.company.stockpro.core.data.export

import com.company.stockpro.core.domain.model.ExportConfig
import com.company.stockpro.core.domain.model.ExportDataset
import com.company.stockpro.core.domain.repository.StreamingDataFetcher
import com.company.stockpro.core.domain.util.RbacSecurityMasker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * High-performance, zero-dependency OpenXML spreadsheet (.xlsx) generator.
 * Streams data directly into a [ZipOutputStream] without heavy third-party POI libraries,
 * ensuring fast rendering and minimal memory footprint on mobile devices.
 */
object OpenXmlSpreadsheetGenerator {

    suspend fun generate(
        config: ExportConfig,
        fetcher: StreamingDataFetcher,
        outputStream: OutputStream,
    ): Int = withContext(Dispatchers.IO) {
        val zip = ZipOutputStream(outputStream)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

        // 1. [Content_Types].xml
        zip.putNextEntry(ZipEntry("[Content_Types].xml"))
        zip.write(
            """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
              <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
              <Default Extension="xml" ContentType="application/xml"/>
              <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
              <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
              <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
              <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
            </Types>
            """.trimIndent().toByteArray(StandardCharsets.UTF_8)
        )
        zip.closeEntry()

        // 2. _rels/.rels
        zip.putNextEntry(ZipEntry("_rels/.rels"))
        zip.write(
            """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
              <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
            </Relationships>
            """.trimIndent().toByteArray(StandardCharsets.UTF_8)
        )
        zip.closeEntry()

        // 3. xl/_rels/workbook.xml.rels
        zip.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
        zip.write(
            """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
              <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
              <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
              <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
            </Relationships>
            """.trimIndent().toByteArray(StandardCharsets.UTF_8)
        )
        zip.closeEntry()

        // 4. xl/styles.xml
        zip.putNextEntry(ZipEntry("xl/styles.xml"))
        zip.write(
            """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
              <fonts count="2">
                <font><sz val="11"/><name val="Calibri"/></font>
                <font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>
              </fonts>
              <fills count="3">
                <fill><patternFill patternType="none"/></fill>
                <fill><patternFill patternType="gray125"/></fill>
                <fill><patternFill patternType="solid"><fgColor rgb="FF0F172A"/></patternFill></fill>
              </fills>
              <borders count="1">
                <border><left/><right/><top/><bottom/><diagonal/></border>
              </borders>
              <cellStyleXfs count="1">
                <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
              </cellStyleXfs>
              <cellXfs count="2">
                <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
                <xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1"/>
              </cellXfs>
            </styleSheet>
            """.trimIndent().toByteArray(StandardCharsets.UTF_8)
        )
        zip.closeEntry()

        // 5. xl/workbook.xml
        val sheet1Title = when (config.dataset) {
            ExportDataset.MASTER_INVENTORY -> "Master Inventory"
            ExportDataset.STOCK_LEDGER -> "Stock Ledger"
            ExportDataset.LOW_STOCK_ALERT -> "Low Stock Alerts"
            ExportDataset.EXECUTIVE_SUMMARY -> "Data Items"
        }
        zip.putNextEntry(ZipEntry("xl/workbook.xml"))
        zip.write(
            """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
              <sheets>
                <sheet name="$sheet1Title" sheetId="1" r:id="rId1"/>
                <sheet name="Executive Summary" sheetId="2" r:id="rId2"/>
              </sheets>
            </workbook>
            """.trimIndent().toByteArray(StandardCharsets.UTF_8)
        )
        zip.closeEntry()

        // 6. xl/worksheets/sheet1.xml (Main Data Sheet)
        var totalRecords = 0
        zip.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
        val sheet1Writer = BufferedWriter(OutputStreamWriter(zip, StandardCharsets.UTF_8))
        sheet1Writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
        sheet1Writer.write("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">\n")
        sheet1Writer.write("  <sheetData>\n")

        var rowIndex = 1

        when (config.dataset) {
            ExportDataset.MASTER_INVENTORY, ExportDataset.LOW_STOCK_ALERT, ExportDataset.EXECUTIVE_SUMMARY -> {
                val headers = listOf(
                    "Item ID", "SKU Code", "Item Name", "Classification", "Category",
                    "Unit of Measure", "On Hand", "Reserved", "In Production",
                    "Reorder Level", "Par Level", "Unit Cost (${config.currency.code})",
                    "Total Valuation (${config.currency.code})", "Location Code", "Barcode", "Status"
                )
                writeOpenXmlRow(sheet1Writer, rowIndex++, headers, isHeader = true)

                val flow = if (config.dataset == ExportDataset.LOW_STOCK_ALERT) {
                    fetcher.streamLowStockItems(500, config.plantId)
                } else {
                    fetcher.streamItems(500, config.plantId)
                }

                flow.collect { page ->
                    for (item in page) {
                        val totalValuationPaise = (item.onHand.milli * item.unitCost.paise) / 1000L
                        val unitCostStr = RbacSecurityMasker.formatUnitCost(item.unitCost, config.currency, config.userRole)
                        val valuationStr = RbacSecurityMasker.formatValuation(totalValuationPaise, config.currency, config.userRole)

                        val values = listOf(
                            item.id,
                            item.code,
                            item.name,
                            item.itemClass.name,
                            item.categoryName,
                            item.unitOfMeasure,
                            (item.onHand.milli / 1000.0).toString(),
                            (item.reserved.milli / 1000.0).toString(),
                            (item.inProduction.milli / 1000.0).toString(),
                            (item.reorderLevel.milli / 1000.0).toString(),
                            (item.parLevel.milli / 1000.0).toString(),
                            unitCostStr,
                            valuationStr,
                            item.locationCode,
                            item.barcode ?: "",
                            if (item.isActive) "ACTIVE" else "INACTIVE",
                        )
                        writeOpenXmlRow(sheet1Writer, rowIndex++, values, isHeader = false)
                        totalRecords++
                    }
                }
            }

            ExportDataset.STOCK_LEDGER -> {
                val headers = listOf(
                    "Entry ID", "Timestamp", "Date & Time", "SKU Code", "Item Name",
                    "Movement Type", "Quantity Delta", "Balance After",
                    "Unit Cost (${config.currency.code})", "Reference", "Reason Code", "Note", "Operator"
                )
                writeOpenXmlRow(sheet1Writer, rowIndex++, headers, isHeader = true)

                fetcher.streamLedger(500, config.startDateMillis, config.endDateMillis, config.plantId).collect { page ->
                    for (m in page) {
                        val dateStr = dateFormat.format(Date(m.createdAt))
                        val unitCostStr = RbacSecurityMasker.formatUnitCost(m.unitCost, config.currency, config.userRole)

                        val values = listOf(
                            m.id,
                            m.createdAt.toString(),
                            dateStr,
                            m.itemCode,
                            m.itemName,
                            m.type.name,
                            (m.delta.milli / 1000.0).toString(),
                            (m.balanceAfter.milli / 1000.0).toString(),
                            unitCostStr,
                            m.reference ?: "",
                            m.reasonCode ?: "",
                            m.note ?: "",
                            m.actorName,
                        )
                        writeOpenXmlRow(sheet1Writer, rowIndex++, values, isHeader = false)
                        totalRecords++
                    }
                }
            }
        }

        sheet1Writer.write("  </sheetData>\n")
        sheet1Writer.write("</worksheet>\n")
        sheet1Writer.flush()
        zip.closeEntry()

        // 7. xl/worksheets/sheet2.xml (Executive Summary Tab)
        zip.putNextEntry(ZipEntry("xl/worksheets/sheet2.xml"))
        val sheet2Writer = BufferedWriter(OutputStreamWriter(zip, StandardCharsets.UTF_8))
        sheet2Writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
        sheet2Writer.write("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">\n")
        sheet2Writer.write("  <sheetData>\n")

        var s2Row = 1
        writeOpenXmlRow(sheet2Writer, s2Row++, listOf("StockPro Enterprise Inventory Report - Executive Summary"), isHeader = true)
        writeOpenXmlRow(sheet2Writer, s2Row++, listOf("Metric Key", "Value", "Description"), isHeader = true)

        writeOpenXmlRow(sheet2Writer, s2Row++, listOf("Facility Plant", config.plantName, "Operational plant"), isHeader = false)
        writeOpenXmlRow(sheet2Writer, s2Row++, listOf("Warehouse Location", config.warehouseName, "Active warehouse"), isHeader = false)
        writeOpenXmlRow(sheet2Writer, s2Row++, listOf("Export Timestamp", dateFormat.format(Date()), "Generation time"), isHeader = false)
        writeOpenXmlRow(sheet2Writer, s2Row++, listOf("Reporting Currency", "${config.currency.code} (${config.currency.symbol})", config.currency.displayName), isHeader = false)
        writeOpenXmlRow(sheet2Writer, s2Row++, listOf("Generated By", config.initiatedBy, "User Role: ${config.userRole.displayTitle}"), isHeader = false)
        writeOpenXmlRow(sheet2Writer, s2Row++, listOf("Total Records Exported", totalRecords.toString(), "Primary dataset items"), isHeader = false)

        sheet2Writer.write("  </sheetData>\n")
        sheet2Writer.write("</worksheet>\n")
        sheet2Writer.flush()
        zip.closeEntry()

        zip.finish()
        zip.flush()
        totalRecords
    }

    private fun writeOpenXmlRow(
        writer: BufferedWriter,
        rowIndex: Int,
        values: List<String>,
        isHeader: Boolean,
    ) {
        val styleAttr = if (isHeader) " s=\"1\"" else ""
        writer.write("    <row r=\"$rowIndex\">\n")
        for ((colIdx, cellVal) in values.withIndex()) {
            val cellRef = "${getColLetter(colIdx)}$rowIndex"
            val isNumber = !isHeader && isNumeric(cellVal)
            if (isNumber) {
                writer.write("      <c r=\"$cellRef\"$styleAttr t=\"n\"><v>$cellVal</v></c>\n")
            } else {
                val escaped = sanitizeXml(cellVal)
                writer.write("      <c r=\"$cellRef\"$styleAttr t=\"inlineStr\"><is><t>$escaped</t></is></c>\n")
            }
        }
        writer.write("    </row>\n")
    }

    private fun isNumeric(str: String): Boolean {
        if (str.isEmpty() || str.length > 20) return false
        if (str.startsWith("0") && str.length > 1 && !str.startsWith("0.")) return false
        return str.toDoubleOrNull() != null
    }

    private fun getColLetter(index: Int): String {
        var n = index
        var result = ""
        while (n >= 0) {
            result = ('A'.code + (n % 26)).toChar() + result
            n = (n / 26) - 1
        }
        return result
    }

    private fun sanitizeXml(value: String): String {
        val sanitized = buildString(value.length) {
            for (ch in value) {
                if (ch == '\t' || ch == '\n' || ch == '\r' || (ch in ' '..'\uD7FF') || (ch in '\uE000'..'\uFFFD')) {
                    append(ch)
                }
            }
        }
        return sanitized.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
