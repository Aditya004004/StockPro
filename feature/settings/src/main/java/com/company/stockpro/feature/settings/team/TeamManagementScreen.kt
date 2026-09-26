package com.company.stockpro.feature.settings.team

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.company.stockpro.core.designsystem.component.ButtonVariant
import com.company.stockpro.core.designsystem.component.StockProButton
import com.company.stockpro.core.designsystem.component.StockProTextField
import com.company.stockpro.core.designsystem.theme.Radius
import com.company.stockpro.core.designsystem.theme.Spacing
import com.company.stockpro.core.designsystem.theme.StockProCodeTextStyle
import com.company.stockpro.core.designsystem.theme.StockProColors
import com.company.stockpro.core.domain.model.Role
import com.company.stockpro.core.domain.model.TeamMember

/**
 * Precision Industrial Team Management & RBAC Screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamManagementScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TeamViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val members = uiState.members
    val filterActiveOnly = uiState.filterActiveOnly

    var showInviteDialog by remember { mutableStateOf(false) }
    var showRoleChangeDialog by remember { mutableStateOf<TeamMember?>(null) }
    var showDeactivateConfirmDialog by remember { mutableStateOf<TeamMember?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StockProColors.Canvas),
    ) {
        // 1. Top Industrial Telemetry Strip
        TeamTelemetryStrip()

        // 2. Header
        Surface(
            color = StockProColors.Surface,
            border = androidx.compose.foundation.BorderStroke(width = 0.5.dp, color = StockProColors.OutlineSubtle),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.x2, vertical = Spacing.x3),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = StockProColors.TextPrimary,
                    )
                }
                Spacer(modifier = Modifier.width(Spacing.x1))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TEAM & ACCESS CONTROL",
                        style = StockProCodeTextStyle.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = StockProColors.TextSecondary,
                        ),
                    )
                    Text(
                        text = "Plant 01 Operator Roster",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = StockProColors.TextPrimary,
                    )
                }

                StockProButton(
                    text = "+ Invite",
                    onClick = { showInviteDialog = true },
                    minHeight = 36.dp,
                )
            }
        }

        // 3. Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.screenGutter, vertical = Spacing.x2),
            horizontalArrangement = Arrangement.spacedBy(Spacing.x2),
        ) {
            FilterChip(
                selected = filterActiveOnly == null,
                onClick = { viewModel.setFilter(null) },
                label = {
                    Text(
                        "All (${members.size})",
                        style = StockProCodeTextStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = StockProColors.Primary,
                    selectedLabelColor = Color.White,
                ),
            )
            FilterChip(
                selected = filterActiveOnly == true,
                onClick = { viewModel.setFilter(true) },
                label = {
                    Text(
                        "Active (${members.count { it.isActive }})",
                        style = StockProCodeTextStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = StockProColors.Success,
                    selectedLabelColor = Color.White,
                ),
            )
            FilterChip(
                selected = filterActiveOnly == false,
                onClick = { viewModel.setFilter(false) },
                label = {
                    Text(
                        "Deactivated (${members.count { !it.isActive }})",
                        style = StockProCodeTextStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = StockProColors.Danger,
                    selectedLabelColor = Color.White,
                ),
            )
        }

        // 4. Member List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = Spacing.screenGutter),
            verticalArrangement = Arrangement.spacedBy(Spacing.x2),
        ) {
            items(
                items = members,
                key = { it.id },
                contentType = { "team_member" },
            ) { member ->
                IndustrialTeamMemberRow(
                    member = member,
                    onRoleChangeClick = { showRoleChangeDialog = member },
                    onDeactivateClick = { showDeactivateConfirmDialog = member },
                )
            }
            item { Spacer(modifier = Modifier.height(Spacing.x8)) }
        }
    }

    // Invite Member Dialog
    if (showInviteDialog) {
        InviteMemberDialog(
            onDismiss = { showInviteDialog = false },
            onInvite = { email, name, role ->
                viewModel.inviteMember(email, name, role)
                showInviteDialog = false
            },
        )
    }

    // Role Change Dialog
    if (showRoleChangeDialog != null) {
        val member = showRoleChangeDialog!!
        RoleChangeDialog(
            member = member,
            onDismiss = { showRoleChangeDialog = null },
            onConfirmRole = { newRole ->
                viewModel.updateRole(member.id, newRole)
                showRoleChangeDialog = null
            },
        )
    }

    // Deactivate Confirmation Dialog
    if (showDeactivateConfirmDialog != null) {
        val member = showDeactivateConfirmDialog!!
        DeactivateMemberConfirmDialog(
            member = member,
            onDismiss = { showDeactivateConfirmDialog = null },
            onConfirm = {
                viewModel.deactivateMember(member.id)
                showDeactivateConfirmDialog = null
            },
        )
    }
}

@Composable
private fun TeamTelemetryStrip() {
    Surface(
        color = StockProColors.SurfaceAlt,
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 0.5.dp, color = StockProColors.OutlineSubtle),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.screenGutter, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(StockProColors.Success),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "SYNC ONLINE",
                    style = StockProCodeTextStyle.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = StockProColors.Success,
                    ),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "•  RBAC ENFORCED",
                    style = StockProCodeTextStyle.copy(
                        fontSize = 10.sp,
                        color = StockProColors.TextTertiary,
                    ),
                )
            }

            Text(
                text = "PLANT 01 FACILITY",
                style = StockProCodeTextStyle.copy(
                    fontSize = 10.sp,
                    color = StockProColors.TextTertiary,
                ),
            )
        }
    }
}

@Composable
private fun IndustrialTeamMemberRow(
    member: TeamMember,
    onRoleChangeClick: () -> Unit,
    onDeactivateClick: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        shape = Radius.lg,
        color = if (member.isActive) StockProColors.Surface else StockProColors.SurfaceAlt,
        border = androidx.compose.foundation.BorderStroke(1.dp, StockProColors.OutlineSubtle),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.cardPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Avatar with initials
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (member.isActive) StockProColors.Primary else StockProColors.Outline,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = member.initials,
                    style = StockProCodeTextStyle.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    ),
                )
            }

            Spacer(modifier = Modifier.width(Spacing.x3))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = member.displayName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (member.isActive) StockProColors.TextPrimary else StockProColors.TextSecondary,
                    )
                    if (!member.isActive) {
                        Spacer(modifier = Modifier.width(Spacing.x2))
                        Surface(
                            shape = Radius.xs,
                            color = StockProColors.DangerContainer,
                        ) {
                            Text(
                                text = "DEACTIVATED",
                                style = StockProCodeTextStyle.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StockProColors.Danger,
                                ),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            )
                        }
                    }
                }
                Text(
                    text = member.email,
                    style = StockProCodeTextStyle.copy(fontSize = 11.sp, color = StockProColors.TextSecondary),
                )
                Spacer(modifier = Modifier.height(Spacing.x1))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = Radius.xs,
                        color = StockProColors.PrimaryContainer,
                    ) {
                        Text(
                            text = "✓ ${member.role.displayTitle}",
                            style = StockProCodeTextStyle.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = StockProColors.Primary,
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }

            if (member.isActive) {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Options for ${member.displayName}",
                            tint = StockProColors.TextSecondary,
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Change Role", style = StockProCodeTextStyle.copy(fontSize = 12.sp)) },
                            onClick = {
                                showMenu = false
                                onRoleChangeClick()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Deactivate User", style = StockProCodeTextStyle.copy(fontSize = 12.sp, color = StockProColors.Danger)) },
                            onClick = {
                                showMenu = false
                                onDeactivateClick()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InviteMemberDialog(
    onDismiss: () -> Unit,
    onInvite: (email: String, name: String, role: Role) -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(Role.STORE_KEEPER) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Invite Team Member",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.x3)) {
                StockProTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Full Name",
                    placeholder = "e.g. John Doe",
                )
                StockProTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Work Email",
                    placeholder = "john.doe@company.com",
                )
                Text(
                    text = "Assign Role",
                    style = StockProCodeTextStyle.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = StockProColors.TextSecondary,
                    ),
                )
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.x1)) {
                    Role.entries.forEach { role ->
                        val isSelected = role == selectedRole
                        Surface(
                            shape = Radius.sm,
                            color = if (isSelected) StockProColors.PrimaryContainer else StockProColors.SurfaceAlt,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) StockProColors.Primary else StockProColors.OutlineSubtle,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(Radius.sm)
                                .clickable { selectedRole = role },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = Spacing.x2, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = role.displayTitle,
                                    style = StockProCodeTextStyle.copy(
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) StockProColors.Primary else StockProColors.TextPrimary,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onInvite(email.trim(), name.trim(), selectedRole) },
                enabled = email.isNotBlank() && name.isNotBlank(),
            ) {
                Text("Send Invite", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun RoleChangeDialog(
    member: TeamMember,
    onDismiss: () -> Unit,
    onConfirmRole: (Role) -> Unit,
) {
    var selectedRole by remember { mutableStateOf(member.role) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Role for ${member.displayName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.x2)) {
                Role.entries.forEach { role ->
                    val isSelected = role == selectedRole
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(Radius.md)
                            .background(if (isSelected) StockProColors.PrimaryContainer else Color.Transparent)
                            .clickable { selectedRole = role }
                            .padding(Spacing.cardPadding),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = role.displayTitle,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) StockProColors.Primary else StockProColors.TextPrimary,
                            )
                            Text(
                                text = when (role) {
                                    Role.ORG_ADMIN -> "Full access to inventory, reports, plant configuration, and team management"
                                    Role.PLANT_MANAGER -> "Can manage team, adjust stock, view valuation, and export data"
                                    Role.INVENTORY_MANAGER -> "Can manage inventory, post stock movements, and view valuation"
                                    Role.PRODUCTION_SUPERVISOR -> "Can request materials, issue production runs, and view logs"
                                    Role.QA -> "Can inspect items, log calibrations, and verify audit records"
                                    Role.STORE_KEEPER -> "Floor execution: log movements, receive stock, and verify bin counts"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = StockProColors.TextSecondary,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirmRole(selectedRole) }) {
                Text("Update Role", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun DeactivateMemberConfirmDialog(
    member: TeamMember,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Deactivate User?") },
        text = {
            Text("${member.displayName} (${member.email}) will no longer be able to log in or record stock movements in StockPro. Historic movements posted by this operator will remain preserved in the audit ledger.")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Deactivate", color = StockProColors.Danger, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
