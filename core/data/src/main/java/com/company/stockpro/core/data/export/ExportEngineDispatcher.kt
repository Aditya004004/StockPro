package com.company.stockpro.core.data.export

import com.company.stockpro.core.domain.model.ExportConfig
import com.company.stockpro.core.domain.model.ExportFormat
import com.company.stockpro.core.domain.repository.StreamingDataFetcher
import java.io.OutputStream

/**
 * Unified entry-point for triggering format-specific export engines.
 */
object ExportEngineDispatcher {

    suspend fun generateExport(
        config: ExportConfig,
        fetcher: StreamingDataFetcher,
        outputStream: OutputStream,
    ): Int {
        return when (config.format) {
            ExportFormat.CSV -> Rfc4180CsvGenerator.generate(config, fetcher, outputStream)
            ExportFormat.EXCEL -> OpenXmlSpreadsheetGenerator.generate(config, fetcher, outputStream)
            ExportFormat.PDF -> PdfReportGenerator.generate(config, fetcher, outputStream)
            ExportFormat.JSON -> EnterpriseJsonXmlGenerator.generateJson(config, fetcher, outputStream)
            ExportFormat.XML -> EnterpriseJsonXmlGenerator.generateXml(config, fetcher, outputStream)
        }
    }
}
