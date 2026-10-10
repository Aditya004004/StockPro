package com.company.stockpro.feature.settings

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.company.stockpro.core.designsystem.theme.Radius
import com.company.stockpro.core.designsystem.theme.Spacing
import com.company.stockpro.core.designsystem.theme.StockProColors
import com.company.stockpro.core.domain.model.ExportDataset
import com.company.stockpro.core.domain.model.ExportFormat
import com.company.stockpro.core.domain.model.ExportHistoryRecord
import com.company.stockpro.core.domain.util.RbacSecurityMasker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ExportDatabaseRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExportDatabaseViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ExportDatabaseScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onSelectTab = { viewModel.selectTab(it) },
        onQuickExport = { dataset, format -> viewModel.quickExport(dataset, format) },
        onExecuteAdvancedExport = { viewModel.executeAdvancedExport() },
        onShareRecord = { viewModel.shareRecord(it) },
        onDeleteHistoryRecord = { viewModel.deleteHistoryRecord(it) },
        onClearAllHistory = { viewModel.clearAllHistory() },
        onSetDataset = { viewModel.setDataset(it) },
        onSetFormat = { viewModel.setFormat(it) },
        onSetDatePreset = { viewModel.setDatePreset(it) },
        onSetIncludeCharts = { viewModel.setIncludeCharts(it) },
        onShowClearHistoryDialog = { viewModel.setShowClearHistoryDialog(it) },
        onDismissStatus = { viewModel.dismissStatus() },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportDatabaseScreen(
    uiState: ExportDatabaseUiState,
    onBackClick: () -> Unit,
    onSelectTab: (ExportTab) -> Unit,
    onQuickExport: (ExportDataset, ExportFormat) -> Unit,
    onExecuteAdvancedExport: () -> Unit,
    onShareRecord: (ExportHistoryRecord) -> Unit,
    onDeleteHistoryRecord: (String) -> Unit,
    onClearAllHistory: () -> Unit,
    onSetDataset: (ExportDataset) -> Unit,
    onSetFormat: (ExportFormat) -> Unit,
    onSetDatePreset: (DatePreset) -> Unit,
    onSetIncludeCharts: (Boolean) -> Unit,
    onShowClearHistoryDialog: (Boolean) -> Unit,
    onDismissStatus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Export & Reporting Hub",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = StockProColors.TextPrimary,
                        )
                        Text(
                            text = "${uiState.userSettings.profile.plantName} • ${uiState.userSettings.profile.warehouseName}",
                            fontSize = 12.sp,
                            color = StockProColors.TextSecondary,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = StockProColors.TextPrimary,
                        )
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = StockProColors.PrimaryContainer,
                        modifier = Modifier.padding(end = Spacing.x3),
                    ) {
                        Text(
                            text = uiState.userSettings.currency.code,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = StockProColors.Primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StockProColors.Surface,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(StockProColors.Canvas),
        ) {
            // Tabs
            SecondaryTabRow(
                selectedTabIndex = uiState.selectedTab.ordinal,
                containerColor = StockProColors.Surface,
                contentColor = StockProColors.Primary,
            ) {
                ExportTab.entries.forEach { tab ->
                    Tab(
                        selected = uiState.selectedTab == tab,
                        onClick = { onSelectTab(tab) },
                        text = {
                            Text(
                                text = tab.title,
                                fontWeight = if (uiState.selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp,
                            )
                        },
                    )
                }
            }

            // Progress Banner
            if (uiState.isExporting) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = StockProColors.Primary,
                )
                Surface(
                    color = StockProColors.PrimaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = Spacing.x4, vertical = Spacing.x2),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = StockProColors.Primary,
                        )
                        Spacer(modifier = Modifier.width(Spacing.x3))
                        Text(
                            text = uiState.exportProgressMessage ?: "Generating export...",
                            fontSize = 12.sp,
                            color = StockProColors.Primary,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            // Status notification banner
            if (uiState.statusMessage != null && !uiState.isExporting) {
                Surface(
                    color = if (uiState.isError) StockProColors.DangerContainer else StockProColors.SuccessContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.x4, vertical = Spacing.x2),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = if (uiState.isError) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (uiState.isError) StockProColors.Danger else StockProColors.Success,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(Spacing.x2))
                            Text(
                                text = uiState.statusMessage,
                                fontSize = 12.sp,
                                color = if (uiState.isError) StockProColors.OnDangerContainer else StockProColors.OnSuccessContainer,
                            )
                        }
                        TextButton(onClick = onDismissStatus) {
                            Text("Dismiss", fontSize = 11.sp)
                        }
                    }
                }
            }

            // Tab Content
            Box(modifier = Modifier.weight(1f)) {
                when (uiState.selectedTab) {
                    ExportTab.QUICK_EXPORT -> QuickExportTab(
                        isExporting = uiState.isExporting,
                        lastRecord = uiState.lastExportedRecord,
                        onQuickExport = onQuickExport,
                        onShareRecord = onShareRecord,
                    )
                    ExportTab.ADVANCED_EXPORT -> AdvancedExportTab(
                        uiState = uiState,
                        onSetDataset = onSetDataset,
                        onSetFormat = onSetFormat,
                        onSetDatePreset = onSetDatePreset,
                        onSetIncludeCharts = onSetIncludeCharts,
                        onExecuteExport = onExecuteAdvancedExport,
                    )
                    ExportTab.HISTORY -> HistoryTab(
                        records = uiState.historyRecords,
                        onShareRecord = onShareRecord,
                        onDeleteRecord = onDeleteHistoryRecord,
                        onClearAll = { onShowClearHistoryDialog(true) },
                    )
                }
            }
        }
    }

    // Clear History Dialog
    if (uiState.showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { onShowClearHistoryDialog(false) },
            title = { Text("Clear Export History") },
            text = { Text("This will permanently remove all audit history records and delete generated local report files.") },
            confirmButton = {
                Button(
                    onClick = onClearAllHistory,
                    colors = ButtonDefaults.buttonColors(containerColor = StockProColors.Danger),
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { onShowClearHistoryDialog(false) }) {
                    Text("Cancel")
                }
            },
        )
    }
}

// ─────────────────────────────────────────────────────────
// TAB 1: QUICK EXPORT
// ─────────────────────────────────────────────────────────

@Composable
private fun QuickExportTab(
    isExporting: Boolean,
    lastRecord: ExportHistoryRecord?,
    onQuickExport: (ExportDataset, ExportFormat) -> Unit,
    onShareRecord: (ExportHistoryRecord) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.x4),
        verticalArrangement = Arrangement.spacedBy(Spacing.x3),
    ) {
        item {
            Text(
                text = "Instant 1-Tap Presets",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = StockProColors.TextPrimary,
            )
            Text(
                text = "Generate commercial-grade exports instantly with default enterprise presets.",
                fontSize = 12.sp,
                color = StockProColors.TextSecondary,
            )
            Spacer(modifier = Modifier.height(Spacing.x2))
        }

        // Active generated card banner if available
        if (lastRecord != null) {
            item {
                ExportSuccessCard(record = lastRecord, onShare = { onShareRecord(lastRecord) })
                Spacer(modifier = Modifier.height(Spacing.x2))
            }
        }

        item {
            QuickPresetCard(
                title = "Catalogue Spreadsheet (.csv)",
                subtitle = "RFC 4180 standard with UTF-8 BOM byte marker for Excel compatibility. Master items & valuation.",
                format = "CSV",
                icon = Icons.Default.TableChart,
                badgeColor = StockProColors.Success,
                enabled = !isExporting,
                onClick = { onQuickExport(ExportDataset.MASTER_INVENTORY, ExportFormat.CSV) },
            )
        }

        item {
            QuickPresetCard(
                title = "Executive Summary (.pdf)",
                subtitle = "Multi-page printable publication with KPI box, Donut & Bar canvas charts, and repeating headers.",
                format = "PDF",
                icon = Icons.Default.Description,
                badgeColor = StockProColors.Danger,
                enabled = !isExporting,
                onClick = { onQuickExport(ExportDataset.EXECUTIVE_SUMMARY, ExportFormat.PDF) },
            )
        }

        item {
            QuickPresetCard(
                title = "Master Multi-Tab Workbook (.xlsx)",
                subtitle = "Zero-dependency OpenXML zip stream with dataset separation and executive summary sheet.",
                format = "EXCEL",
                icon = Icons.Default.FolderZip,
                badgeColor = StockProColors.Accent,
                enabled = !isExporting,
                onClick = { onQuickExport(ExportDataset.MASTER_INVENTORY, ExportFormat.EXCEL) },
            )
        }

        item {
            QuickPresetCard(
                title = "ERP Integration Payload (.json)",
                subtitle = "Hierarchical, normalized enterprise JSON schema with audit metadata and valuation RBAC context.",
                format = "JSON",
                icon = Icons.Default.Tune,
                badgeColor = StockProColors.Primary,
                enabled = !isExporting,
                onClick = { onQuickExport(ExportDataset.MASTER_INVENTORY, ExportFormat.JSON) },
            )
        }

        item {
            QuickPresetCard(
                title = "Stock Movement Ledger (.xml)",
                subtitle = "Chronological audit trail of all receipts, issues, adjustments, and location transfers.",
                format = "XML",
                icon = Icons.Default.History,
                badgeColor = StockProColors.Production,
                enabled = !isExporting,
                onClick = { onQuickExport(ExportDataset.STOCK_LEDGER, ExportFormat.XML) },
            )
        }
    }
}

@Composable
private fun QuickPresetCard(
    title: String,
    subtitle: String,
    format: String,
    icon: ImageVector,
    badgeColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        shape = Radius.md,
        colors = CardDefaults.cardColors(containerColor = StockProColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.x4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = badgeColor.copy(alpha = 0.12f),
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(Spacing.x3))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = StockProColors.TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeColor.copy(alpha = 0.15f),
                    ) {
                        Text(
                            text = format,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.x1))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = StockProColors.TextSecondary,
                    lineHeight = 16.sp,
                )
            }

            Spacer(modifier = Modifier.width(Spacing.x2))

            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = "Export",
                tint = if (enabled) StockProColors.Primary else StockProColors.TextDisabled,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun ExportSuccessCard(
    record: ExportHistoryRecord,
    onShare: () -> Unit,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Radius.md,
        colors = CardDefaults.cardColors(containerColor = StockProColors.SuccessContainer),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(StockProColors.Success)),
    ) {
        Column(modifier = Modifier.padding(Spacing.x4)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = StockProColors.Success,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(Spacing.x2))
                Text(
                    text = "Report Ready: ${record.fileName}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = StockProColors.OnSuccessContainer,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(Spacing.x2))

            Text(
                text = "${record.recordCount} items • ${formatBytes(record.fileSizeBytes)} • SHA-256 verified",
                fontSize = 12.sp,
                color = StockProColors.OnSuccessContainer,
            )

            Spacer(modifier = Modifier.height(Spacing.x1))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.6f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "SHA: ${record.sha256Checksum.take(16)}...",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = StockProColors.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(record.sha256Checksum))
                        Toast.makeText(context, "SHA-256 copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(18.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy hash",
                        tint = StockProColors.TextSecondary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.x3))

            Button(
                onClick = onShare,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = StockProColors.Success),
                shape = Radius.sm,
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(Spacing.x2))
                Text("Share via Android ShareSheet", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// TAB 2: ADVANCED EXPORT
// ─────────────────────────────────────────────────────────

@Composable
private fun AdvancedExportTab(
    uiState: ExportDatabaseUiState,
    onSetDataset: (ExportDataset) -> Unit,
    onSetFormat: (ExportFormat) -> Unit,
    onSetDatePreset: (DatePreset) -> Unit,
    onSetIncludeCharts: (Boolean) -> Unit,
    onExecuteExport: () -> Unit,
) {
    val role = uiState.userSettings.profile.role
    val canValuation = RbacSecurityMasker.canViewValuation(role)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.x4),
        verticalArrangement = Arrangement.spacedBy(Spacing.x4),
    ) {
        // Section: Select Dataset
        item {
            Text(
                text = "1. Select Dataset",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = StockProColors.TextPrimary,
            )
            Spacer(modifier = Modifier.height(Spacing.x2))

            ExportDataset.entries.forEach { dataset ->
                val selected = uiState.selectedDataset == dataset
                Surface(
                    shape = Radius.md,
                    color = if (selected) StockProColors.PrimaryContainer else StockProColors.Surface,
                    border = CardDefaults.outlinedCardBorder().copy(
                        width = if (selected) 2.dp else 1.dp,
                        brush = androidx.compose.ui.graphics.SolidColor(
                            if (selected) StockProColors.Primary else StockProColors.OutlineSubtle
                        ),
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onSetDataset(dataset) },
                ) {
                    Column(modifier = Modifier.padding(Spacing.x3)) {
                        Text(
                            text = dataset.displayTitle,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = if (selected) StockProColors.Primary else StockProColors.TextPrimary,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = dataset.description,
                            fontSize = 11.sp,
                            color = StockProColors.TextSecondary,
                        )
                    }
                }
            }
        }

        // Section: Format Selection
        item {
            Text(
                text = "2. Select Format",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = StockProColors.TextPrimary,
            )
            Spacer(modifier = Modifier.height(Spacing.x2))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.x2),
            ) {
                ExportFormat.entries.forEach { fmt ->
                    FilterChip(
                        selected = uiState.selectedFormat == fmt,
                        onClick = { onSetFormat(fmt) },
                        label = { Text(fmt.name, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StockProColors.Primary,
                            selectedLabelColor = Color.White,
                        ),
                    )
                }
            }
        }

        // Section: Security & RBAC Status
        item {
            Text(
                text = "3. Security & Valuation Context",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = StockProColors.TextPrimary,
            )
            Spacer(modifier = Modifier.height(Spacing.x2))

            Card(
                shape = Radius.md,
                colors = CardDefaults.cardColors(containerColor = StockProColors.Surface),
            ) {
                Column(modifier = Modifier.padding(Spacing.x4)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = if (canValuation) StockProColors.Success else StockProColors.Warning,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(Spacing.x2))
                        Text(
                            text = "Role: ${role.displayTitle}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = StockProColors.TextPrimary,
                        )
                    }
                    Spacer(modifier = Modifier.height(Spacing.x2))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (canValuation) StockProColors.SuccessContainer else StockProColors.WarningContainer,
                    ) {
                        Text(
                            text = if (canValuation) {
                                "Full Financial Access: Unit costs & valuations visible"
                            } else {
                                "RBAC Field Masking Active: Financial valuations masked for this role"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (canValuation) StockProColors.OnSuccessContainer else StockProColors.OnWarningContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }

        // Section: Date Range Filter
        item {
            Text(
                text = "4. Date Range Filter",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = StockProColors.TextPrimary,
            )
            Spacer(modifier = Modifier.height(Spacing.x2))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.x2),
            ) {
                DatePreset.entries.forEach { preset ->
                    FilterChip(
                        selected = uiState.datePreset == preset,
                        onClick = { onSetDatePreset(preset) },
                        label = { Text(preset.label, fontSize = 11.sp) },
                    )
                }
            }
        }

        // Section: PDF Visualizations Toggle
        if (uiState.selectedFormat == ExportFormat.PDF) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(Radius.md)
                        .background(StockProColors.Surface)
                        .padding(Spacing.x4),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Vector Analytics Charts",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = StockProColors.TextPrimary,
                        )
                        Text(
                            text = "Include native Canvas Donut & Bar charts in PDF report",
                            fontSize = 11.sp,
                            color = StockProColors.TextSecondary,
                        )
                    }
                    Switch(
                        checked = uiState.includeCharts,
                        onCheckedChange = onSetIncludeCharts,
                        colors = SwitchDefaults.colors(checkedThumbColor = StockProColors.Primary),
                    )
                }
            }
        }

        // Execute Button
        item {
            Spacer(modifier = Modifier.height(Spacing.x2))
            Button(
                onClick = onExecuteExport,
                enabled = !uiState.isExporting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = Radius.md,
                colors = ButtonDefaults.buttonColors(containerColor = StockProColors.Primary),
            ) {
                Icon(imageVector = Icons.Default.Download, contentDescription = null)
                Spacer(modifier = Modifier.width(Spacing.x2))
                Text(
                    text = "Generate & Export Dataset",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
            }
            Spacer(modifier = Modifier.height(Spacing.x6))
        }
    }
}

// ─────────────────────────────────────────────────────────
// TAB 3: AUDIT HISTORY
// ─────────────────────────────────────────────────────────

@Composable
private fun HistoryTab(
    records: List<ExportHistoryRecord>,
    onShareRecord: (ExportHistoryRecord) -> Unit,
    onDeleteRecord: (String) -> Unit,
    onClearAll: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.x4),
        verticalArrangement = Arrangement.spacedBy(Spacing.x3),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Cryptographic Audit Trail",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = StockProColors.TextPrimary,
                    )
                    Text(
                        text = "${records.size} verified export events",
                        fontSize = 11.sp,
                        color = StockProColors.TextSecondary,
                    )
                }

                if (records.isNotEmpty()) {
                    TextButton(onClick = onClearAll) {
                        Text("Clear History", color = StockProColors.Danger, fontSize = 12.sp)
                    }
                }
            }
        }

        if (records.isEmpty()) {
            item {
                Surface(
                    shape = Radius.md,
                    color = StockProColors.Surface,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.x8),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = StockProColors.TextDisabled,
                            modifier = Modifier.size(44.dp),
                        )
                        Spacer(modifier = Modifier.height(Spacing.x2))
                        Text(
                            text = "No Export History",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = StockProColors.TextPrimary,
                        )
                        Text(
                            text = "Exports generated from Quick Export or Advanced Export will appear here.",
                            fontSize = 12.sp,
                            color = StockProColors.TextSecondary,
                        )
                    }
                }
            }
        } else {
            items(records, key = { it.id }) { record ->
                HistoryRecordCard(
                    record = record,
                    onShare = { onShareRecord(record) },
                    onDelete = { onDeleteRecord(record.id) },
                )
            }
        }
    }
}

@Composable
private fun HistoryRecordCard(
    record: ExportHistoryRecord,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Radius.md,
        colors = CardDefaults.cardColors(containerColor = StockProColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(Spacing.x4)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.dataset.displayTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = StockProColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${dateFormat.format(Date(record.timestamp))} • Initiated by ${record.initiatedBy}",
                        fontSize = 11.sp,
                        color = StockProColors.TextSecondary,
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = StockProColors.PrimaryContainer,
                ) {
                    Text(
                        text = record.format.name,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = StockProColors.Primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.x2))

            Text(
                text = "${record.recordCount} records • ${formatBytes(record.fileSizeBytes)} • ${record.fileName}",
                fontSize = 11.sp,
                color = StockProColors.TextPrimary,
            )

            Spacer(modifier = Modifier.height(Spacing.x2))

            // SHA-256 box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(StockProColors.Canvas)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "SHA: ${record.sha256Checksum.take(24)}...",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = StockProColors.TextSecondary,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(record.sha256Checksum))
                        Toast.makeText(context, "Full SHA-256 hash copied", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(20.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy hash",
                        tint = StockProColors.TextSecondary,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.x2))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = StockProColors.TextTertiary,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(modifier = Modifier.width(Spacing.x2))
                Button(
                    onClick = onShare,
                    modifier = Modifier.height(32.dp),
                    shape = Radius.sm,
                    colors = ButtonDefaults.buttonColors(containerColor = StockProColors.Primary),
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share", fontSize = 11.sp)
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format(Locale.US, "%.1f MB", mb)
}
