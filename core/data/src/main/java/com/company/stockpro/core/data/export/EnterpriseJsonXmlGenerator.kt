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

/**
 * Enterprise ERP exchange generator for hierarchical JSON and XML data payloads.
 * Implements streaming chunk writing to guarantee O(1) memory during high-volume exports.
 */
object EnterpriseJsonXmlGenerator {

    suspend fun generateJson(
        config: ExportConfig,
        fetcher: StreamingDataFetcher,
        outputStream: OutputStream,
    ): Int = withContext(Dispatchers.IO) {
        val writer = BufferedWriter(OutputStreamWriter(outputStream, StandardCharsets.UTF_8))
        val now = System.currentTimeMillis()
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        val isoDate = isoFormat.format(Date(now))

        writer.write("{\n")
        writer.write("  \"schemaVersion\": \"1.0\",\n")
        writer.write("  \"application\": \"StockPro ERP\",\n")
        writer.write("  \"timestamp\": $now,\n")
        writer.write("  \"isoTimestamp\": \"$isoDate\",\n")
        writer.write("  \"plant\": {\n")
        writer.write("    \"id\": \"${escapeJson(config.plantId)}\",\n")
        writer.write("    \"name\": \"${escapeJson(config.plantName)}\",\n")
        writer.write("    \"warehouse\": \"${escapeJson(config.warehouseName)}\"\n")
        writer.write("  },\n")
        writer.write("  \"metadata\": {\n")
        writer.write("    \"dataset\": \"${config.dataset.name}\",\n")
        writer.write("    \"currency\": \"${config.currency.code}\",\n")
        writer.write("    \"initiatedBy\": \"${escapeJson(config.initiatedBy)}\",\n")
        writer.write("    \"userRole\": \"${config.userRole.name}\",\n")
        writer.write("    \"valuationPermitted\": ${RbacSecurityMasker.canViewValuation(config.userRole)}\n")
        writer.write("  },\n")
        writer.write("  \"records\": [\n")

        var recordCount = 0
        var isFirst = true

        when (config.dataset) {
            ExportDataset.MASTER_INVENTORY, ExportDataset.LOW_STOCK_ALERT, ExportDataset.EXECUTIVE_SUMMARY -> {
                val flow = if (config.dataset == ExportDataset.LOW_STOCK_ALERT) {
                    fetcher.streamLowStockItems(500, config.plantId)
                } else {
                    fetcher.streamItems(500, config.plantId)
                }

                flow.collect { page ->
                    for (item in page) {
                        if (!isFirst) {
                            writer.write(",\n")
                        } else {
                            isFirst = false
                        }
                        val totalValuationPaise = (item.onHand.milli * item.unitCost.paise) / 1000L
                        val unitCostStr = RbacSecurityMasker.formatUnitCost(item.unitCost, config.currency, config.userRole)
                        val valuationStr = RbacSecurityMasker.formatValuation(totalValuationPaise, config.currency, config.userRole)

                        writer.write("    {\n")
                        writer.write("      \"id\": \"${escapeJson(item.id)}\",\n")
                        writer.write("      \"code\": \"${escapeJson(item.code)}\",\n")
                        writer.write("      \"name\": \"${escapeJson(item.name)}\",\n")
                        writer.write("      \"class\": \"${item.itemClass.name}\",\n")
                        writer.write("      \"category\": \"${escapeJson(item.categoryName)}\",\n")
                        writer.write("      \"unitOfMeasure\": \"${escapeJson(item.unitOfMeasure)}\",\n")
                        writer.write("      \"onHand\": ${item.onHand.milli / 1000.0},\n")
                        writer.write("      \"reserved\": ${item.reserved.milli / 1000.0},\n")
                        writer.write("      \"inProduction\": ${item.inProduction.milli / 1000.0},\n")
                        writer.write("      \"reorderLevel\": ${item.reorderLevel.milli / 1000.0},\n")
                        writer.write("      \"unitCost\": \"${escapeJson(unitCostStr)}\",\n")
                        writer.write("      \"totalValuation\": \"${escapeJson(valuationStr)}\",\n")
                        writer.write("      \"locationCode\": \"${escapeJson(item.locationCode)}\",\n")
                        writer.write("      \"isActive\": ${item.isActive}\n")
                        writer.write("    }")
                        recordCount++
                    }
                }
            }

            ExportDataset.STOCK_LEDGER -> {
                fetcher.streamLedger(500, config.startDateMillis, config.endDateMillis, config.plantId).collect { page ->
                    for (m in page) {
                        if (!isFirst) {
                            writer.write(",\n")
                        } else {
                            isFirst = false
                        }
                        val unitCostStr = RbacSecurityMasker.formatUnitCost(m.unitCost, config.currency, config.userRole)

                        writer.write("    {\n")
                        writer.write("      \"id\": \"${escapeJson(m.id)}\",\n")
                        writer.write("      \"itemId\": \"${escapeJson(m.itemId)}\",\n")
                        writer.write("      \"itemCode\": \"${escapeJson(m.itemCode)}\",\n")
                        writer.write("      \"itemName\": \"${escapeJson(m.itemName)}\",\n")
                        writer.write("      \"type\": \"${m.type.name}\",\n")
                        writer.write("      \"delta\": ${m.delta.milli / 1000.0},\n")
                        writer.write("      \"balanceAfter\": ${m.balanceAfter.milli / 1000.0},\n")
                        writer.write("      \"unitCost\": \"$unitCostStr\",\n")
                        writer.write("      \"reference\": \"${escapeJson(m.reference ?: "")}\",\n")
                        writer.write("      \"reasonCode\": \"${escapeJson(m.reasonCode ?: "")}\",\n")
                        writer.write("      \"note\": \"${escapeJson(m.note ?: "")}\",\n")
                        writer.write("      \"actorName\": \"${escapeJson(m.actorName)}\",\n")
                        writer.write("      \"createdAt\": ${m.createdAt}\n")
                        writer.write("    }")
                        recordCount++
                    }
                }
            }
        }

        writer.write("\n  ],\n")
        writer.write("  \"totalRecords\": $recordCount\n")
        writer.write("}\n")
        writer.flush()
        recordCount
    }

    suspend fun generateXml(
        config: ExportConfig,
        fetcher: StreamingDataFetcher,
        outputStream: OutputStream,
    ): Int = withContext(Dispatchers.IO) {
        val writer = BufferedWriter(OutputStreamWriter(outputStream, StandardCharsets.UTF_8))
        val now = System.currentTimeMillis()
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        val isoDate = isoFormat.format(Date(now))

        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        writer.write("<StockProExport schemaVersion=\"1.0\" generatedAt=\"$isoDate\" timestamp=\"$now\">\n")
        writer.write("  <Header>\n")
        writer.write("    <Plant id=\"${escapeXml(config.plantId)}\" name=\"${escapeXml(config.plantName)}\" warehouse=\"${escapeXml(config.warehouseName)}\"/>\n")
        writer.write("    <Security user=\"${escapeXml(config.initiatedBy)}\" role=\"${config.userRole.name}\" valuationPermitted=\"${RbacSecurityMasker.canViewValuation(config.userRole)}\"/>\n")
        writer.write("    <Currency code=\"${config.currency.code}\" symbol=\"${escapeXml(config.currency.symbol)}\"/>\n")
        writer.write("    <Dataset name=\"${config.dataset.name}\"/>\n")
        writer.write("  </Header>\n")
        writer.write("  <Records>\n")

        var recordCount = 0

        when (config.dataset) {
            ExportDataset.MASTER_INVENTORY, ExportDataset.LOW_STOCK_ALERT, ExportDataset.EXECUTIVE_SUMMARY -> {
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

                        writer.write("    <Item id=\"${escapeXml(item.id)}\" code=\"${escapeXml(item.code)}\">\n")
                        writer.write("      <Name>${escapeXml(item.name)}</Name>\n")
                        writer.write("      <Class>${item.itemClass.name}</Class>\n")
                        writer.write("      <Category>${escapeXml(item.categoryName)}</Category>\n")
                        writer.write("      <UnitOfMeasure>${escapeXml(item.unitOfMeasure)}</UnitOfMeasure>\n")
                        writer.write("      <OnHand>${item.onHand.formatted()}</OnHand>\n")
                        writer.write("      <Reserved>${item.reserved.formatted()}</Reserved>\n")
                        writer.write("      <InProduction>${item.inProduction.formatted()}</InProduction>\n")
                        writer.write("      <ReorderLevel>${item.reorderLevel.formatted()}</ReorderLevel>\n")
                        writer.write("      <UnitCost>${escapeXml(unitCostStr)}</UnitCost>\n")
                        writer.write("      <TotalValuation>${escapeXml(valuationStr)}</TotalValuation>\n")
                        writer.write("      <LocationCode>${escapeXml(item.locationCode)}</LocationCode>\n")
                        writer.write("      <IsActive>${item.isActive}</IsActive>\n")
                        writer.write("    </Item>\n")
                        recordCount++
                    }
                }
            }

            ExportDataset.STOCK_LEDGER -> {
                fetcher.streamLedger(500, config.startDateMillis, config.endDateMillis, config.plantId).collect { page ->
                    for (m in page) {
                        val unitCostStr = RbacSecurityMasker.formatUnitCost(m.unitCost, config.currency, config.userRole)

                        writer.write("    <Movement id=\"${escapeXml(m.id)}\" createdAt=\"${m.createdAt}\">\n")
                        writer.write("      <ItemCode>${escapeXml(m.itemCode)}</ItemCode>\n")
                        writer.write("      <ItemName>${escapeXml(m.itemName)}</ItemName>\n")
                        writer.write("      <Type>${m.type.name}</Type>\n")
                        writer.write("      <Delta>${m.delta.formatted()}</Delta>\n")
                        writer.write("      <BalanceAfter>${m.balanceAfter.formatted()}</BalanceAfter>\n")
                        writer.write("      <UnitCost>${escapeXml(unitCostStr)}</UnitCost>\n")
                        writer.write("      <Reference>${escapeXml(m.reference ?: "")}</Reference>\n")
                        writer.write("      <ReasonCode>${escapeXml(m.reasonCode ?: "")}</ReasonCode>\n")
                        writer.write("      <Note>${escapeXml(m.note ?: "")}</Note>\n")
                        writer.write("      <ActorName>${escapeXml(m.actorName)}</ActorName>\n")
                        writer.write("    </Movement>\n")
                        recordCount++
                    }
                }
            }
        }

        writer.write("  </Records>\n")
        writer.write("  <Footer totalRecords=\"$recordCount\"/>\n")
        writer.write("</StockProExport>\n")
        writer.flush()
        recordCount
    }

    private fun escapeJson(value: String): String {
        return buildString(value.length + 8) {
            for (ch in value) {
                when (ch) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\b' -> append("\\b")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    '\u000C' -> append("\\f")
                    in '\u0000'..'\u001F' -> append(String.format(Locale.US, "\\u%04x", ch.code))
                    else -> append(ch)
                }
            }
        }
    }

    private fun escapeXml(value: String): String {
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
