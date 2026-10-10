package com.company.stockpro.core.data.export

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.company.stockpro.core.domain.model.ExportConfig
import com.company.stockpro.core.domain.model.ExportDataset
import com.company.stockpro.core.domain.model.Item
import com.company.stockpro.core.domain.model.StockMovement
import com.company.stockpro.core.domain.repository.StreamingDataFetcher
import com.company.stockpro.core.domain.util.RbacSecurityMasker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Enterprise PDF Report Generator using native Android [PdfDocument] and vector [Canvas] drawing.
 * Produces multi-page publications featuring:
 * - Executive KPI Summary Box
 * - Vector Donut & Bar Charts (zero third-party dependencies)
 * - Repeating Table Headers on data pages
 * - Running Footers ("Page X", Confidentiality notice)
 * - Zebra-striped rows with ellipsis text clipping
 */
object PdfReportGenerator {

    private const val PAGE_WIDTH = 595 // A4 standard pt
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 36f
    private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2)

    // Palette
    private val COLOR_HEADER_BG = Color.parseColor("#0F172A") // StockPro Navy
    private val COLOR_PRIMARY = Color.parseColor("#2563EB")
    private val COLOR_ACCENT = Color.parseColor("#10B981")
    private val COLOR_WARNING = Color.parseColor("#F59E0B")
    private val COLOR_DANGER = Color.parseColor("#EF4444")
    private val COLOR_PURPLE = Color.parseColor("#8B5CF6")
    private val COLOR_CARD_BG = Color.parseColor("#F8FAFC")
    private val COLOR_BORDER = Color.parseColor("#E2E8F0")
    private val COLOR_TEXT_MAIN = Color.parseColor("#0F172A")
    private val COLOR_TEXT_MUTED = Color.parseColor("#64748B")
    private val COLOR_ZEBRA = Color.parseColor("#F8FAFC")

    const val MAX_PDF_RECORDS = 1000

    suspend fun generate(
        config: ExportConfig,
        fetcher: StreamingDataFetcher,
        outputStream: OutputStream,
    ): Int = withContext(Dispatchers.IO) {
        val document = PdfDocument()
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            val timestamp = dateFormat.format(Date())
            var totalRecordCount = 0

            // Pre-aggregate summary data for Executive Box & Charts
            var totalItems = 0
            var lowStockCount = 0
            var totalValuationPaise = 0L
            val rawMaterialsCount = mutableMapOf<String, Int>()

            val boundedItems = mutableListOf<Item>()
            val boundedMovements = mutableListOf<StockMovement>()

            when (config.dataset) {
                ExportDataset.MASTER_INVENTORY, ExportDataset.LOW_STOCK_ALERT, ExportDataset.EXECUTIVE_SUMMARY -> {
                    val flow = if (config.dataset == ExportDataset.LOW_STOCK_ALERT) {
                        fetcher.streamLowStockItems(500, config.plantId)
                    } else {
                        fetcher.streamItems(500, config.plantId)
                    }
                    flow.collect { page ->
                        for (item in page) {
                            totalRecordCount++
                            if (boundedItems.size < MAX_PDF_RECORDS) {
                                boundedItems.add(item)
                            }
                            totalItems++
                            if (item.onHand.milli <= item.reorderLevel.milli) lowStockCount++
                            val valPaise = (item.onHand.milli * item.unitCost.paise) / 1000L
                            totalValuationPaise += valPaise
                            val key = item.itemClass.name
                            rawMaterialsCount[key] = (rawMaterialsCount[key] ?: 0) + 1
                        }
                    }
                }
                ExportDataset.STOCK_LEDGER -> {
                    fetcher.streamLedger(500, config.startDateMillis, config.endDateMillis, config.plantId).collect { page ->
                        for (m in page) {
                            totalRecordCount++
                            if (boundedMovements.size < MAX_PDF_RECORDS) {
                                boundedMovements.add(m)
                            }
                        }
                    }
                }
            }

            val rowHeight = 20f
            val tableTop = 60f
            val tableBottom = 790f
            val availableHeight = tableBottom - tableTop
            val rowsPerPage = (availableHeight / rowHeight).toInt() - 2 // 34 rows per page

            val recordsToRender = if (boundedItems.isNotEmpty()) boundedItems.size else boundedMovements.size
            val dataPages = if (config.dataset == ExportDataset.EXECUTIVE_SUMMARY || recordsToRender == 0) {
                0
            } else {
                (recordsToRender + rowsPerPage - 1) / rowsPerPage
            }
            val totalPages = 1 + dataPages
            var pageNumber = 1

            // ================= PAGE 1: EXECUTIVE SUMMARY & CHARTS =================
            val pageInfo1 = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            val page1 = document.startPage(pageInfo1)
            val canvas1 = page1.canvas

            drawWatermark(canvas1, config, timestamp)
            drawHeaderBanner(canvas1, config, timestamp)

            val valuationStr = RbacSecurityMasker.formatValuation(totalValuationPaise, config.currency, config.userRole)
            var currentY = 110f
            currentY = drawExecutiveKpiBox(
                canvas = canvas1,
                topY = currentY,
                totalItems = if (config.dataset == ExportDataset.STOCK_LEDGER) totalRecordCount else totalItems,
                lowStockCount = lowStockCount,
                totalValuation = valuationStr,
                config = config,
            )

            // Native Canvas Vector Charts (Donut + Bar)
            if (config.includeCharts) {
                currentY = drawChartsSection(
                    canvas = canvas1,
                    topY = currentY + 16f,
                    rawMaterialsCount = rawMaterialsCount,
                    totalItems = totalItems,
                    lowStockCount = lowStockCount,
                )
            }

            // Running Footer
            drawPageFooter(canvas1, pageNumber, totalPages, config)
            document.finishPage(page1)
            pageNumber++

            // ================= DATA PAGES (TABLE VIEW) =================
            if (config.dataset != ExportDataset.EXECUTIVE_SUMMARY && recordsToRender > 0) {
                if (boundedItems.isNotEmpty()) {
                    val columns = listOf(
                        TableColumn("SKU Code", 85f),
                        TableColumn("Item Name", 130f),
                        TableColumn("Class", 65f),
                        TableColumn("On Hand", 55f),
                        TableColumn("Reorder", 55f),
                        TableColumn("Unit Cost", 65f),
                        TableColumn("Valuation", 68f),
                    )

                    val chunks = boundedItems.chunked(rowsPerPage)
                    for (chunk in chunks) {
                        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                        val page = document.startPage(pageInfo)
                        val canvas = page.canvas

                        drawWatermark(canvas, config, timestamp)
                        drawTableHeader(canvas, tableTop, columns)

                        var rowY = tableTop + rowHeight
                        for ((idx, item) in chunk.withIndex()) {
                            val itemValPaise = (item.onHand.milli * item.unitCost.paise) / 1000L
                            val unitCost = RbacSecurityMasker.formatUnitCost(item.unitCost, config.currency, config.userRole)
                            val itemVal = RbacSecurityMasker.formatValuation(itemValPaise, config.currency, config.userRole)

                            val rowData = listOf(
                                item.code,
                                item.name,
                                item.itemClass.name,
                                item.onHand.formatted(),
                                item.reorderLevel.formatted(),
                                unitCost,
                                itemVal,
                            )
                            drawTableRow(canvas, rowY, rowHeight, columns, rowData, isZebra = idx % 2 == 1)
                            rowY += rowHeight
                        }

                        drawPageFooter(canvas, pageNumber, totalPages, config)
                        document.finishPage(page)
                        pageNumber++
                    }
                } else if (boundedMovements.isNotEmpty()) {
                    val columns = listOf(
                        TableColumn("Date", 95f),
                        TableColumn("SKU Code", 75f),
                        TableColumn("Item Name", 120f),
                        TableColumn("Type", 65f),
                        TableColumn("Delta", 50f),
                        TableColumn("Balance", 50f),
                        TableColumn("Operator", 68f),
                    )

                    val chunks = boundedMovements.chunked(rowsPerPage)
                    for (chunk in chunks) {
                        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                        val page = document.startPage(pageInfo)
                        val canvas = page.canvas

                        drawWatermark(canvas, config, timestamp)
                        drawTableHeader(canvas, tableTop, columns)

                        var rowY = tableTop + rowHeight
                        for ((idx, m) in chunk.withIndex()) {
                            val dateStr = dateFormat.format(Date(m.createdAt))
                            val rowData = listOf(
                                dateStr,
                                m.itemCode,
                                m.itemName,
                                m.type.name,
                                m.delta.formatted(),
                                m.balanceAfter.formatted(),
                                m.actorName,
                            )
                            drawTableRow(canvas, rowY, rowHeight, columns, rowData, isZebra = idx % 2 == 1)
                            rowY += rowHeight
                        }

                        drawPageFooter(canvas, pageNumber, totalPages, config)
                        document.finishPage(page)
                        pageNumber++
                    }
                }
            }

            document.writeTo(outputStream)
            totalRecordCount
        } finally {
            document.close()
        }
    }

    private fun drawHeaderBanner(canvas: Canvas, config: ExportConfig, timestamp: String) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Navy top bar
        paint.color = COLOR_HEADER_BG
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 95f, paint)

        // Title
        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 20f
        canvas.drawText("STOCKPRO ENTERPRISE REPORT", MARGIN, 40f, paint)

        // Subtitle
        paint.color = Color.parseColor("#94A3B8")
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 10f
        canvas.drawText(
            "${config.dataset.displayTitle} • Facility: ${config.plantName} (${config.warehouseName})",
            MARGIN,
            58f,
            paint
        )

        // Right metadata
        paint.textSize = 9f
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Generated: $timestamp", PAGE_WIDTH - MARGIN, 40f, paint)
        canvas.drawText("Operator: ${config.initiatedBy} [${config.userRole.displayTitle}]", PAGE_WIDTH - MARGIN, 56f, paint)
        paint.textAlign = Paint.Align.LEFT
    }

    private fun drawExecutiveKpiBox(
        canvas: Canvas,
        topY: Float,
        totalItems: Int,
        lowStockCount: Int,
        totalValuation: String,
        config: ExportConfig,
    ): Float {
        val cardHeight = 105f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Outer Card Container
        paint.color = COLOR_CARD_BG
        val rect = RectF(MARGIN, topY, PAGE_WIDTH - MARGIN, topY + cardHeight)
        canvas.drawRoundRect(rect, 10f, 10f, paint)

        paint.color = COLOR_BORDER
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(rect, 10f, 10f, paint)
        paint.style = Paint.Style.FILL

        // Section Title
        paint.color = COLOR_TEXT_MAIN
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 12f
        canvas.drawText("EXECUTIVE KPI SUMMARY", MARGIN + 14f, topY + 22f, paint)

        // 4 KPI Columns
        val colWidth = CONTENT_WIDTH / 4f
        val metrics = listOf(
            Triple("TOTAL ITEMS", totalItems.toString(), COLOR_PRIMARY),
            Triple("LOW STOCK ALERTS", lowStockCount.toString(), if (lowStockCount > 0) COLOR_DANGER else COLOR_ACCENT),
            Triple("TOTAL VALUATION", totalValuation, COLOR_ACCENT),
            Triple("CURRENCY", "${config.currency.code} (${config.currency.symbol})", COLOR_PURPLE),
        )

        for ((i, metric) in metrics.withIndex()) {
            val startX = MARGIN + 14f + (i * colWidth)
            paint.color = COLOR_TEXT_MUTED
            paint.typeface = Typeface.DEFAULT
            paint.textSize = 9f
            canvas.drawText(metric.first, startX, topY + 48f, paint)

            paint.color = metric.third
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 15f
            canvas.drawText(metric.second, startX, topY + 70f, paint)
        }

        return topY + cardHeight
    }

    private fun drawChartsSection(
        canvas: Canvas,
        topY: Float,
        rawMaterialsCount: Map<String, Int>,
        totalItems: Int,
        lowStockCount: Int,
    ): Float {
        val sectionHeight = 220f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Outer Box
        val boxRect = RectF(MARGIN, topY, PAGE_WIDTH - MARGIN, topY + sectionHeight)
        paint.color = Color.WHITE
        canvas.drawRoundRect(boxRect, 10f, 10f, paint)
        paint.color = COLOR_BORDER
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(boxRect, 10f, 10f, paint)
        paint.style = Paint.Style.FILL

        // Section Header
        paint.color = COLOR_TEXT_MAIN
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 12f
        canvas.drawText("ANALYTIC VISUALIZATIONS", MARGIN + 14f, topY + 22f, paint)

        // ── LEFT: Donut Pie Chart (Category / Class Breakdown) ──
        val donutCenterX = MARGIN + 90f
        val donutCenterY = topY + 115f
        val donutRadius = 60f
        val donutOval = RectF(
            donutCenterX - donutRadius,
            donutCenterY - donutRadius,
            donutCenterX + donutRadius,
            donutCenterY + donutRadius
        )

        val chartColors = listOf(COLOR_PRIMARY, COLOR_ACCENT, COLOR_WARNING, COLOR_PURPLE, COLOR_DANGER)
        var startAngle = -90f
        val total = totalItems.coerceAtLeast(1)

        val entries = if (rawMaterialsCount.isNotEmpty()) {
            rawMaterialsCount.entries.toList()
        } else {
            listOf(java.util.AbstractMap.SimpleEntry("Items", totalItems))
        }

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 24f

        for ((i, entry) in entries.withIndex()) {
            val sweep = (entry.value.toFloat() / total.toFloat()) * 360f
            paint.color = chartColors[i % chartColors.size]
            canvas.drawArc(donutOval, startAngle, sweep, false, paint)
            startAngle += sweep
        }

        // Inner Cutout Center Text
        paint.style = Paint.Style.FILL
        paint.color = COLOR_TEXT_MAIN
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 14f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("$totalItems", donutCenterX, donutCenterY + 4f, paint)
        paint.textSize = 8f
        paint.color = COLOR_TEXT_MUTED
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Total Items", donutCenterX, donutCenterY + 16f, paint)
        paint.textAlign = Paint.Align.LEFT

        // Donut Legend
        var legendY = topY + 60f
        val legendX = donutCenterX + donutRadius + 20f
        paint.textSize = 9f
        for ((i, entry) in entries.take(4).withIndex()) {
            paint.color = chartColors[i % chartColors.size]
            canvas.drawCircle(legendX, legendY - 3f, 4f, paint)
            paint.color = COLOR_TEXT_MAIN
            val pct = ((entry.value.toFloat() / total.toFloat()) * 100).toInt()
            canvas.drawText("${entry.key}: ${entry.value} ($pct%)", legendX + 10f, legendY, paint)
            legendY += 18f
        }

        // ── RIGHT: Vector Bar Chart (Stock Health Status) ──
        val barStartX = MARGIN + (CONTENT_WIDTH / 2f) + 20f
        val barBottomY = topY + 175f
        val barMaxHeight = 100f
        val healthyCount = (totalItems - lowStockCount).coerceAtLeast(0)
        val maxVal = totalItems.coerceAtLeast(1)

        // Baseline axis
        paint.color = COLOR_BORDER
        paint.strokeWidth = 1f
        canvas.drawLine(barStartX, barBottomY, PAGE_WIDTH - MARGIN - 14f, barBottomY, paint)

        val barData = listOf(
            Triple("Healthy", healthyCount, COLOR_ACCENT),
            Triple("Low Stock", lowStockCount, COLOR_WARNING),
            Triple("Total", totalItems, COLOR_PRIMARY),
        )

        val barWidth = 36f
        val barGap = 35f

        for ((i, bar) in barData.withIndex()) {
            val bX = barStartX + 20f + (i * (barWidth + barGap))
            val barHeight = ((bar.second.toFloat() / maxVal.toFloat()) * barMaxHeight).coerceAtLeast(6f)
            val bY = barBottomY - barHeight

            paint.color = bar.third
            val bRect = RectF(bX, bY, bX + barWidth, barBottomY)
            canvas.drawRoundRect(bRect, 4f, 4f, paint)

            // Bar value text
            paint.color = COLOR_TEXT_MAIN
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 9f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(bar.second.toString(), bX + (barWidth / 2f), bY - 5f, paint)

            // Bar label
            paint.color = COLOR_TEXT_MUTED
            paint.typeface = Typeface.DEFAULT
            paint.textSize = 8f
            canvas.drawText(bar.first, bX + (barWidth / 2f), barBottomY + 12f, paint)
            paint.textAlign = Paint.Align.LEFT
        }

        return topY + sectionHeight
    }

    private data class TableColumn(val title: String, val width: Float)

    private fun drawTableHeader(canvas: Canvas, topY: Float, columns: List<TableColumn>) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val height = 20f

        paint.color = COLOR_HEADER_BG
        canvas.drawRect(MARGIN, topY, PAGE_WIDTH - MARGIN, topY + height, paint)

        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 9f

        var currentX = MARGIN + 6f
        for (col in columns) {
            canvas.drawText(col.title, currentX, topY + 14f, paint)
            currentX += col.width
        }
    }

    private fun drawTableRow(
        canvas: Canvas,
        topY: Float,
        height: Float,
        columns: List<TableColumn>,
        values: List<String>,
        isZebra: Boolean,
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        if (isZebra) {
            paint.color = COLOR_ZEBRA
            canvas.drawRect(MARGIN, topY, PAGE_WIDTH - MARGIN, topY + height, paint)
        }

        // Bottom border
        paint.color = COLOR_BORDER
        paint.strokeWidth = 0.5f
        canvas.drawLine(MARGIN, topY + height, PAGE_WIDTH - MARGIN, topY + height, paint)

        paint.color = COLOR_TEXT_MAIN
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 8f

        var currentX = MARGIN + 6f
        for ((idx, col) in columns.withIndex()) {
            val text = values.getOrNull(idx) ?: ""
            val truncated = truncateWithEllipsis(text, col.width - 8f, paint)
            canvas.drawText(truncated, currentX, topY + 13f, paint)
            currentX += col.width
        }
    }

    private fun drawWatermark(canvas: Canvas, config: ExportConfig, timestamp: String) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#12000000") // 7% opacity subtle watermark
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.save()
        canvas.rotate(-35f, PAGE_WIDTH / 2f, PAGE_HEIGHT / 2f)
        val watermarkText = "CONFIDENTIAL • ${config.initiatedBy} • ${config.plantName} • $timestamp"
        canvas.drawText(watermarkText, PAGE_WIDTH / 2f, PAGE_HEIGHT / 2f, paint)
        canvas.restore()
    }

    private fun drawPageFooter(canvas: Canvas, pageNumber: Int, totalPages: Int, config: ExportConfig) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val footerY = 815f

        paint.color = COLOR_BORDER
        paint.strokeWidth = 0.5f
        canvas.drawLine(MARGIN, footerY - 10f, PAGE_WIDTH - MARGIN, footerY - 10f, paint)

        paint.color = COLOR_TEXT_MUTED
        paint.textSize = 8f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("StockPro Confidential • Generated for ${config.plantName}", MARGIN, footerY, paint)

        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Page $pageNumber of $totalPages", PAGE_WIDTH - MARGIN, footerY, paint)
        paint.textAlign = Paint.Align.LEFT
    }

    private fun truncateWithEllipsis(text: String, maxWidth: Float, paint: Paint): String {
        if (paint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 0 && paint.measureText(text.substring(0, end) + "...") > maxWidth) {
            end--
        }
        return if (end <= 0) "..." else text.substring(0, end) + "..."
    }
}
