package com.company.stockpro.core.data.export

import com.company.stockpro.core.domain.model.Currency
import com.company.stockpro.core.domain.model.ExportConfig
import com.company.stockpro.core.domain.model.ExportDataset
import com.company.stockpro.core.domain.model.Item
import com.company.stockpro.core.domain.model.StockMovement
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

/**
 * RFC 4180 compliant CSV generator.
 * Automatically prefixes the stream with the UTF-8 Byte Order Mark (\uFEFF)
 * ensuring Microsoft Excel renders non-ASCII glyphs (e.g. ₹, €, £) correctly.
 */
object Rfc4180CsvGenerator {

    private const val CRLF = "\r\n"
    private const val UTF8_BOM = "\uFEFF"

    suspend fun generate(
        config: ExportConfig,
        fetcher: StreamingDataFetcher,
        outputStream: OutputStream,
    ): Int = withContext(Dispatchers.IO) {
        val writer = BufferedWriter(OutputStreamWriter(outputStream, StandardCharsets.UTF_8))
        // Write UTF-8 BOM
        writer.write(UTF8_BOM)

        var totalRecords = 0
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

        when (config.dataset) {
            ExportDataset.MASTER_INVENTORY -> {
                writeHeader(writer, listOf(
                    "Item ID", "SKU Code", "Item Name", "Classification", "Category",
                    "Unit of Measure", "On Hand", "Reserved", "In Production",
                    "Reorder Level", "Par Level", "Unit Cost (${config.currency.code})",
                    "Total Valuation (${config.currency.code})", "Location Code", "Barcode", "Active Status"
                ))

                fetcher.streamItems(500, config.plantId).collect { page ->
                    for (item in page) {
                        val totalValuationPaise = (item.onHand.milli * item.unitCost.paise) / 1000L
                        val unitCostStr = RbacSecurityMasker.formatUnitCost(item.unitCost, config.currency, config.userRole)
                        val valuationStr = RbacSecurityMasker.formatValuation(totalValuationPaise, config.currency, config.userRole)

                        writeRow(writer, listOf(
                            item.id,
                            item.code,
                            item.name,
                            item.itemClass.name,
                            item.categoryName,
                            item.unitOfMeasure,
                            item.onHand.formatted(),
                            item.reserved.formatted(),
                            item.inProduction.formatted(),
                            item.reorderLevel.formatted(),
                            item.parLevel.formatted(),
                            unitCostStr,
                            valuationStr,
                            item.locationCode,
                            item.barcode ?: "",
                            if (item.isActive) "ACTIVE" else "INACTIVE",
                        ))
                        totalRecords++
                    }
                }
            }

            ExportDataset.LOW_STOCK_ALERT -> {
                writeHeader(writer, listOf(
                    "SKU Code", "Item Name", "Classification", "Category",
                    "Current On Hand", "Reorder Level", "Deficit / Shortfall",
                    "Unit Cost (${config.currency.code})", "Estimated Restock Cost (${config.currency.code})", "Location"
                ))

                fetcher.streamLowStockItems(500, config.plantId).collect { page ->
                    for (item in page) {
                        val deficitMilli = (item.reorderLevel.milli - item.onHand.milli).coerceAtLeast(0L)
                        val restockCostPaise = (deficitMilli * item.unitCost.paise) / 1000L
                        val unitCostStr = RbacSecurityMasker.formatUnitCost(item.unitCost, config.currency, config.userRole)
                        val restockCostStr = RbacSecurityMasker.formatValuation(restockCostPaise, config.currency, config.userRole)

                        writeRow(writer, listOf(
                            item.code,
                            item.name,
                            item.itemClass.name,
                            item.categoryName,
                            item.onHand.formatted(),
                            item.reorderLevel.formatted(),
                            String.format(Locale.US, "%.2f", deficitMilli / 1000.0),
                            unitCostStr,
                            restockCostStr,
                            item.locationCode,
                        ))
                        totalRecords++
                    }
                }
            }

            ExportDataset.STOCK_LEDGER -> {
                writeHeader(writer, listOf(
                    "Entry ID", "Timestamp", "Date & Time", "SKU Code", "Item Name",
                    "Movement Type", "Quantity Delta", "Balance After",
                    "Unit Cost (${config.currency.code})", "Reference", "Reason Code", "Note", "Operator"
                ))

                fetcher.streamLedger(500, config.startDateMillis, config.endDateMillis, config.plantId).collect { page ->
                    for (m in page) {
                        val dateStr = dateFormat.format(Date(m.createdAt))
                        val unitCostStr = RbacSecurityMasker.formatUnitCost(m.unitCost, config.currency, config.userRole)

                        writeRow(writer, listOf(
                            m.id,
                            m.createdAt.toString(),
                            dateStr,
                            m.itemCode,
                            m.itemName,
                            m.type.name,
                            m.delta.formatted(),
                            m.balanceAfter.formatted(),
                            unitCostStr,
                            m.reference ?: "",
                            m.reasonCode ?: "",
                            m.note ?: "",
                            m.actorName,
                        ))
                        totalRecords++
                    }
                }
            }

            ExportDataset.EXECUTIVE_SUMMARY -> {
                writeHeader(writer, listOf("Metric", "Value", "Notes"))
                writeRow(writer, listOf("Plant Name", config.plantName, "Target facility"))
                writeRow(writer, listOf("Warehouse", config.warehouseName, "Active location"))
                writeRow(writer, listOf("Export Generated At", dateFormat.format(Date()), "System time"))
                writeRow(writer, listOf("Currency Code", config.currency.code, config.currency.displayName))
                writeRow(writer, listOf("Initiated By", config.initiatedBy, "User Role: ${config.userRole.displayTitle}"))

                var itemCount = 0
                var lowStockCount = 0
                var totalValuationPaise = 0L
                val categoryMap = mutableMapOf<String, Long>()

                fetcher.streamItems(500, config.plantId).collect { page ->
                    for (item in page) {
                        itemCount++
                        if (item.onHand.milli <= item.reorderLevel.milli) {
                            lowStockCount++
                        }
                        val itemValuation = (item.onHand.milli * item.unitCost.paise) / 1000L
                        totalValuationPaise += itemValuation
                        val cat = item.categoryName.ifBlank { "Uncategorized" }
                        categoryMap[cat] = (categoryMap[cat] ?: 0L) + itemValuation
                    }
                }

                val formattedTotalValuation = RbacSecurityMasker.formatValuation(totalValuationPaise, config.currency, config.userRole)
                writeRow(writer, listOf("Total Active Items", itemCount.toString(), "Catalogue count"))
                writeRow(writer, listOf("Low Stock Count", lowStockCount.toString(), "Items at/below reorder threshold"))
                writeRow(writer, listOf("Total Valuation", formattedTotalValuation, "Minor unit sum"))

                writeRow(writer, listOf("", "", ""))
                writeRow(writer, listOf("Category Valuation Breakdown", "", ""))
                for ((cat, catPaise) in categoryMap) {
                    val catValStr = RbacSecurityMasker.formatValuation(catPaise, config.currency, config.userRole)
                    writeRow(writer, listOf("Category: $cat", catValStr, ""))
                }
                totalRecords = itemCount
            }
        }

        writer.flush()
        totalRecords
    }

    private fun writeHeader(writer: BufferedWriter, columns: List<String>) {
        writeRow(writer, columns)
    }

    private fun writeRow(writer: BufferedWriter, values: List<String>) {
        val row = values.joinToString(",") { escapeCsv(it) }
        writer.write(row)
        writer.write(CRLF)
    }

    private fun sanitizeFormulaCell(value: String): String {
        if (value.isEmpty()) return value
        val firstChar = value[0]
        return if (firstChar == '=' || firstChar == '+' || firstChar == '-' || firstChar == '@' || firstChar == '\t' || firstChar == '\r') {
            "'$value"
        } else {
            value
        }
    }

    private fun escapeCsv(value: String): String {
        val sanitized = sanitizeFormulaCell(value)
        val containsSpecial = sanitized.contains(',') || sanitized.contains('"') || sanitized.contains('\n') || sanitized.contains('\r')
        return if (containsSpecial) {
            "\"" + sanitized.replace("\"", "\"\"") + "\""
        } else {
            sanitized
        }
    }
}
