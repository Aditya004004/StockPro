package com.company.stockpro.feature.inventory.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentInd
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.company.stockpro.core.designsystem.component.ButtonVariant
import com.company.stockpro.core.designsystem.component.StockProButton
import com.company.stockpro.core.designsystem.component.StockProTextField
import com.company.stockpro.core.designsystem.theme.Radius
import com.company.stockpro.core.designsystem.theme.Spacing
import com.company.stockpro.core.designsystem.theme.StockProColors
import com.company.stockpro.core.domain.model.CalibrationStatus
import com.company.stockpro.core.domain.model.Item
import com.company.stockpro.core.domain.model.ToolCustody
import com.company.stockpro.core.domain.model.ToolStatus

@Composable
fun ToolCustodySection(
    item: Item,
    custody: ToolCustody?,
    onIssueTool: (operatorName: String, notes: String?) -> Unit,
    onReturnTool: (notes: String?) -> Unit,
    onLogCalibration: (calibratedAt: Long, nextDueAt: Long, notes: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeCustody = custody ?: ToolCustody(itemId = item.id)
    val calStatus = activeCustody.deriveCalibrationStatus()
    val daysUntilDue = activeCustody.daysUntilDue()

    var showIssueDialog by remember { mutableStateOf(false) }
    var showReturnDialog by remember { mutableStateOf(false) }
    var showCalibrateDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = Radius.lg,
        colors = CardDefaults.cardColors(containerColor = StockProColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.cardPadding),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(Radius.sm)
                            .background(StockProColors.PrimaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Build,
                            contentDescription = null,
                            tint = StockProColors.Primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(Spacing.x2))
                    Column {
                        Text(
                            text = "Tool Lifecycle & Custody",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = StockProColors.TextPrimary,
                        )
                        Text(
                            text = "General manufacturing custody & calibration (PRD §7)",
                            style = MaterialTheme.typography.bodySmall,
                            color = StockProColors.TextSecondary,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.x3))

            // Status and Calibration Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.x2),
            ) {
                // Status Badge
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(Radius.md)
                        .background(
                            when (activeCustody.status) {
                                ToolStatus.AVAILABLE -> StockProColors.SuccessContainer
                                ToolStatus.ISSUED -> StockProColors.PrimaryContainer
                                ToolStatus.IN_CALIBRATION -> StockProColors.WarningContainer
                                ToolStatus.RETIRED -> StockProColors.DangerContainer
                            },
                        )
                        .padding(Spacing.x2),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Status: ${activeCustody.status.displayLabel}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = when (activeCustody.status) {
                            ToolStatus.AVAILABLE -> StockProColors.Success
                            ToolStatus.ISSUED -> StockProColors.Primary
                            ToolStatus.IN_CALIBRATION -> StockProColors.Warning
                            ToolStatus.RETIRED -> StockProColors.Danger
                        },
                    )
                }

                // Calibration Badge
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(Radius.md)
                        .background(
                            when (calStatus) {
                                CalibrationStatus.VALID -> StockProColors.SuccessContainer
                                CalibrationStatus.DUE_SOON -> StockProColors.WarningContainer
                                CalibrationStatus.OVERDUE -> StockProColors.DangerContainer
                            },
                        )
                        .padding(Spacing.x2),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Calibration: ${calStatus.displayLabel}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = when (calStatus) {
                            CalibrationStatus.VALID -> StockProColors.Success
                            CalibrationStatus.DUE_SOON -> StockProColors.Warning
                            CalibrationStatus.OVERDUE -> StockProColors.Danger
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.x3))

            // Custody Operator Details
            if (activeCustody.status == ToolStatus.ISSUED && activeCustody.assignedToOperator != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(Radius.md)
                        .background(StockProColors.SurfaceAlt)
                        .padding(Spacing.x3),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.AssignmentInd,
                        contentDescription = null,
                        tint = StockProColors.Primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(Spacing.x2))
                    Column {
                        Text(
                            text = "Assigned To: ${activeCustody.assignedToOperator}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = StockProColors.TextPrimary,
                        )
                        activeCustody.notes?.let {
                            Text(
                                text = "Notes: $it",
                                style = MaterialTheme.typography.bodySmall,
                                color = StockProColors.TextSecondary,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.x3))
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.x2),
            ) {
                if (activeCustody.status == ToolStatus.AVAILABLE) {
                    StockProButton(
                        text = "Issue Tool",
                        onClick = { showIssueDialog = true },
                        variant = ButtonVariant.PRIMARY,
                        modifier = Modifier.weight(1f),
                    )
                } else if (activeCustody.status == ToolStatus.ISSUED) {
                    StockProButton(
                        text = "Return to Store",
                        onClick = { showReturnDialog = true },
                        variant = ButtonVariant.PRIMARY,
                        modifier = Modifier.weight(1f),
                    )
                }

                StockProButton(
                    text = "Log Calibration",
                    onClick = { showCalibrateDialog = true },
                    variant = ButtonVariant.SECONDARY,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    // Issue Tool Dialog
    if (showIssueDialog) {
        var operatorName by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showIssueDialog = false },
            title = { Text("Issue Tool to Operator") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.x3)) {
                    StockProTextField(
                        value = operatorName,
                        onValueChange = { operatorName = it },
                        label = "Operator Name / Badge ID",
                        placeholder = "e.g. Ramesh P. (ID-842)",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    StockProTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = "Usage Notes / Station",
                        placeholder = "e.g. Station Line 2 Assembly",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (operatorName.isNotBlank()) {
                            onIssueTool(operatorName, notes.ifBlank { null })
                            showIssueDialog = false
                        }
                    },
                    enabled = operatorName.isNotBlank(),
                ) {
                    Text("Confirm Checkout", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showIssueDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    // Return Tool Dialog
    if (showReturnDialog) {
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showReturnDialog = false },
            title = { Text("Return Tool to Store") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.x2)) {
                    Text(
                        text = "Returning tool '${item.name}' from ${activeCustody.assignedToOperator ?: "Operator"}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = StockProColors.TextPrimary,
                    )
                    StockProTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = "Condition / Check-in Notes",
                        placeholder = "e.g. Cleaned, no visible wear",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onReturnTool(notes.ifBlank { null })
                        showReturnDialog = false
                    },
                ) {
                    Text("Confirm Return", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReturnDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    // Log Calibration Dialog
    if (showCalibrateDialog) {
        var notes by remember { mutableStateOf("") }
        val now = System.currentTimeMillis()
        val nextDue = now + (180L * 24 * 60 * 60 * 1000L) // 180 days interval

        AlertDialog(
            onDismissRequest = { showCalibrateDialog = false },
            title = { Text("Log Tool Calibration") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.x2)) {
                    Text(
                        text = "Record calibration verification. Next calibration will be scheduled 180 days from today.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = StockProColors.TextPrimary,
                    )
                    StockProTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = "Calibration Certificate / Notes",
                        placeholder = "e.g. Certified by Metrology Lab #42",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onLogCalibration(now, nextDue, notes.ifBlank { null })
                        showCalibrateDialog = false
                    },
                ) {
                    Text("Save Calibration", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCalibrateDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}
