package com.company.stockpro.feature.settings.team

import androidx.compose.foundation.background
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.stockpro.core.designsystem.component.ButtonVariant
import com.company.stockpro.core.designsystem.component.StatusBadge
import com.company.stockpro.core.designsystem.component.StockProButton
import com.company.stockpro.core.designsystem.component.StockProTextField
import com.company.stockpro.core.designsystem.theme.Radius
import com.company.stockpro.core.designsystem.theme.Spacing
import com.company.stockpro.core.designsystem.theme.StockProColors
import com.company.stockpro.core.domain.model.Role
import com.company.stockpro.core.domain.model.StockLevel
import com.company.stockpro.core.domain.model.TeamMember
import com.company.stockpro.core.domain.repository.InventoryRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamManagementScreen(
    repository: InventoryRepository,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val members by repository.observeTeamMembers().collectAsState(initial = emptyList())

    var filterActiveOnly by remember { mutableStateOf<Boolean?>(null) } // null = All, true = Active, false = Deactivated
    var showInviteDialog by remember { mutableStateOf(false) }
    var selectedMemberForAction by remember { mutableStateOf<TeamMember?>(null) }
    var showRoleChangeDialog by remember { mutableStateOf<TeamMember?>(null) }
    var showDeactivateConfirmDialog by remember { mutableStateOf<TeamMember?>(null) }

    val filteredMembers = remember(members, filterActiveOnly) {
        when (filterActiveOnly) {
            true -> members.filter { it.isActive }
            false -> members.filter { !it.isActive }
            null -> members
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StockProColors.Canvas),
    ) {
        // Top App Bar
        Surface(
            color = StockProColors.Surface,
            shadowElevation = 2.dp,
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
                Spacer(modifier = Modifier.width(Spacing.x2))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Team & Permissions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = StockProColors.TextPrimary,
                    )
                    Text(
                        text = "${members.count { it.isActive }} Active Members",
                        style = MaterialTheme.typography.bodySmall,
                        color = StockProColors.TextSecondary,
                    )
                }

                IconButton(onClick = { showInviteDialog = true }) {
                    Icon(
                        imageVector = Icons.Filled.PersonAdd,
                        contentDescription = "Invite Member",
                        tint = StockProColors.Primary,
                    )
                }
            }
        }

        // Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.screenGutter, vertical = Spacing.x3),
            horizontalArrangement = Arrangement.spacedBy(Spacing.x2),
        ) {
            FilterChip(
                selected = filterActiveOnly == null,
                onClick = { filterActiveOnly = null },
                label = { Text("All (${members.size})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = StockProColors.PrimaryContainer,
                    selectedLabelColor = StockProColors.Primary,
                ),
            )
            FilterChip(
                selected = filterActiveOnly == true,
                onClick = { filterActiveOnly = true },
                label = { Text("Active (${members.count { it.isActive }})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = StockProColors.SuccessContainer,
                    selectedLabelColor = StockProColors.Success,
                ),
            )
            FilterChip(
                selected = filterActiveOnly == false,
                onClick = { filterActiveOnly = false },
                label = { Text("Deactivated (${members.count { !it.isActive }})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = StockProColors.DangerContainer,
                    selectedLabelColor = StockProColors.Danger,
                ),
            )
        }

        // Member List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = Spacing.screenGutter),
            verticalArrangement = Arrangement.spacedBy(Spacing.x2),
        ) {
            items(
                items = filteredMembers,
                key = { it.id },
                contentType = { "team_member" },
            ) { member ->
                TeamMemberRow(
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
                coroutineScope.launch {
                    repository.inviteMember(email, name, role, "plant_01")
                    showInviteDialog = false
                }
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
                coroutineScope.launch {
                    repository.updateMemberRole(member.id, newRole)
                    showRoleChangeDialog = null
                }
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
                coroutineScope.launch {
                    repository.deactivateMember(member.id)
                    showDeactivateConfirmDialog = null
                }
            },
        )
    }
}

@Composable
private fun TeamMemberRow(
    member: TeamMember,
    onRoleChangeClick: () -> Unit,
    onDeactivateClick: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Radius.lg,
        colors = CardDefaults.cardColors(
            containerColor = if (member.isActive) StockProColors.Surface else StockProColors.SurfaceAlt,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
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
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (member.isActive) StockProColors.Primary else StockProColors.OutlineSubtle,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = member.initials,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
            }

            Spacer(modifier = Modifier.width(Spacing.x3))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = member.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (member.isActive) StockProColors.TextPrimary else StockProColors.TextSecondary,
                    )
                    if (!member.isActive) {
                        Spacer(modifier = Modifier.width(Spacing.x2))
                        Text(
                            text = "(Deactivated)",
                            style = MaterialTheme.typography.bodySmall,
                            color = StockProColors.Danger,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Text(
                    text = member.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = StockProColors.TextSecondary,
                )
                Spacer(modifier = Modifier.height(Spacing.x1))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = member.role.displayTitle,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = StockProColors.Primary,
                    )
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
                            text = { Text("Change Role") },
                            onClick = {
                                showMenu = false
                                onRoleChangeClick()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Deactivate User", color = StockProColors.Danger) },
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
    var isRoleDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Invite Team Member") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.x3)) {
                StockProTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email Address",
                    placeholder = "colleague@company.com",
                    modifier = Modifier.fillMaxWidth(),
                )
                StockProTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Full Name",
                    placeholder = "e.g. Ramesh Patil",
                    modifier = Modifier.fillMaxWidth(),
                )
                Column {
                    Text(
                        text = "Assigned Role",
                        style = MaterialTheme.typography.bodySmall,
                        color = StockProColors.TextSecondary,
                    )
                    Spacer(modifier = Modifier.height(Spacing.x1))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(Radius.md)
                            .background(StockProColors.SurfaceAlt)
                            .clickable { isRoleDropdownExpanded = true }
                            .padding(Spacing.cardPadding),
                    ) {
                        Text(
                            text = selectedRole.displayTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = StockProColors.TextPrimary,
                        )
                        DropdownMenu(
                            expanded = isRoleDropdownExpanded,
                            onDismissRequest = { isRoleDropdownExpanded = false },
                        ) {
                            Role.entries.forEach { role ->
                                DropdownMenuItem(
                                    text = { Text(role.displayTitle) },
                                    onClick = {
                                        selectedRole = role
                                        isRoleDropdownExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (email.isNotBlank()) onInvite(email, name, selectedRole) },
                enabled = email.isNotBlank(),
            ) {
                Text("Send Invite", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
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
                Text(
                    text = "Role changes update custom security claims and trigger an immediate server token refresh.",
                    style = MaterialTheme.typography.bodySmall,
                    color = StockProColors.TextSecondary,
                )
                Spacer(modifier = Modifier.height(Spacing.x2))
                Role.entries.forEach { role ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(Radius.md)
                            .background(
                                if (role == selectedRole) StockProColors.PrimaryContainer else Color.Transparent,
                            )
                            .clickable { selectedRole = role }
                            .padding(Spacing.cardPadding),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = role.displayTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (role == selectedRole) FontWeight.Bold else FontWeight.Normal,
                            color = if (role == selectedRole) StockProColors.Primary else StockProColors.TextPrimary,
                            modifier = Modifier.weight(1f),
                        )
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
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
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
        title = { Text("Deactivate ${member.displayName}?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.x2)) {
                Text(
                    text = "Are you sure you want to deactivate this account?",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = StockProColors.TextPrimary,
                )
                Text(
                    text = "User access will be revoked immediately. All historical movements, cycle counts, and audit logs created by this user remain preserved in the ledger.",
                    style = MaterialTheme.typography.bodySmall,
                    color = StockProColors.TextSecondary,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Deactivate User", color = StockProColors.Danger, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
