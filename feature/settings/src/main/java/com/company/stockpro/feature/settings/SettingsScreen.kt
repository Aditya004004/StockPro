package com.company.stockpro.feature.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.company.stockpro.core.designsystem.component.StockProTextField
import com.company.stockpro.core.designsystem.theme.Radius
import com.company.stockpro.core.designsystem.theme.Spacing
import com.company.stockpro.core.designsystem.theme.StockProColors
import com.company.stockpro.core.domain.model.AppLanguage
import com.company.stockpro.core.domain.model.NotificationPreferences
import com.company.stockpro.core.domain.model.SyncStatus
import com.company.stockpro.core.domain.model.ThemeMode
import com.company.stockpro.core.domain.model.UnitSystem
import com.company.stockpro.core.common.result.asString
import com.company.stockpro.core.ui.component.SkeletonBox

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel,
    onNavigateToTeam: () -> Unit,
    onLogoutClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Trigger share sheet if exported CSV is ready
    LaunchedEffect(uiState) {
        if (uiState is SettingsUiState.Content) {
            val content = uiState as SettingsUiState.Content
            content.exportedCsv?.let { csv ->
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, csv)
                    type = "text/csv"
                }
                val shareIntent = Intent.createChooser(sendIntent, "Export StockPro Settings")
                runCatching { context.startActivity(shareIntent) }
                viewModel.clearExportedCsv()
            }
        }
    }

    when (val state = uiState) {
        SettingsUiState.Loading -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(StockProColors.Canvas)
                    .padding(Spacing.screenGutter),
            ) {
                Spacer(modifier = Modifier.height(Spacing.x4))
                SkeletonBox(modifier = Modifier.fillMaxWidth().height(100.dp))
                Spacer(modifier = Modifier.height(Spacing.x6))
                SkeletonBox(modifier = Modifier.fillMaxWidth().height(200.dp))
            }
        }
        is SettingsUiState.Content -> {
            SettingsScreen(
                content = state,
                onShowDialog = { viewModel.showDialog(it) },
                onDismissDialog = { viewModel.dismissDialog() },
                onUpdateDisplayName = { viewModel.updateDisplayName(it) },
                onUpdateTheme = { viewModel.updateTheme(it) },
                onUpdateLanguage = { viewModel.updateLanguage(it) },
                onUpdateUnitSystem = { viewModel.updateUnitSystem(it) },
                onUpdateNotificationPreferences = { viewModel.updateNotificationPreferences(it) },
                onClearCache = { viewModel.clearCache() },
                onExportData = { viewModel.exportData() },
                onNavigateToTeam = onNavigateToTeam,
                onLogoutClick = { viewModel.logout(onLogoutClick) },
                onDeleteAccountClick = {
                    viewModel.deleteAccount(
                        onSuccess = onLogoutClick,
                        onError = { msg ->
                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                        },
                    )
                },
                modifier = modifier,
            )
        }
        is SettingsUiState.Error -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(StockProColors.Canvas),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = state.message.asString(context), color = StockProColors.Danger)
            }
        }
    }
}

@Composable
fun SettingsScreen(
    content: SettingsUiState.Content,
    onShowDialog: (SettingsDialog) -> Unit,
    onDismissDialog: () -> Unit,
    onUpdateDisplayName: (String) -> Unit,
    onUpdateTheme: (ThemeMode) -> Unit,
    onUpdateLanguage: (AppLanguage) -> Unit,
    onUpdateUnitSystem: (UnitSystem) -> Unit,
    onUpdateNotificationPreferences: (NotificationPreferences) -> Unit,
    onClearCache: () -> Unit,
    onExportData: () -> Unit,
    onNavigateToTeam: () -> Unit,
    onLogoutClick: () -> Unit,
    onDeleteAccountClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val profile = content.userSettings.profile
    val settings = content.userSettings

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StockProColors.Canvas)
            .verticalScroll(rememberScrollState())
            .padding(Spacing.screenGutter),
    ) {
        Spacer(modifier = Modifier.height(Spacing.x4))

        // Title
        Text(
            text = "Settings",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = StockProColors.TextPrimary,
        )
        Text(
            text = "Manage your account and plant preferences",
            style = MaterialTheme.typography.bodyMedium,
            color = StockProColors.TextSecondary,
        )

        Spacer(modifier = Modifier.height(Spacing.x4))

        // User Profile Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onShowDialog(SettingsDialog.EditProfile) },
            shape = Radius.lg,
            colors = CardDefaults.cardColors(containerColor = StockProColors.Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Row(
                modifier = Modifier.padding(Spacing.cardPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(StockProColors.Primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = profile.initials,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 18.sp,
                    )
                }

                Spacer(modifier = Modifier.width(Spacing.x3))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = StockProColors.TextPrimary,
                    )
                    Text(
                        text = profile.email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = StockProColors.TextSecondary,
                    )
                    Text(
                        text = "${profile.plantName}  |  ${profile.role.displayTitle}",
                        style = MaterialTheme.typography.bodySmall,
                        color = StockProColors.Primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = "Edit Profile",
                    tint = StockProColors.TextTertiary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.x6))

        // Account Settings Group
        SettingsSectionHeader("Account Settings")
        SettingsGroupCard {
            SettingsRowItem(
                icon = Icons.Filled.Person,
                iconBg = StockProColors.PrimaryContainer,
                iconTint = StockProColors.Primary,
                title = "Profile Information",
                subtitle = "Update your display name",
                onClick = { onShowDialog(SettingsDialog.EditProfile) },
            )
            HorizontalDivider(color = StockProColors.OutlineSubtle)
            SettingsRowItem(
                icon = Icons.Filled.Security,
                iconBg = StockProColors.PrimaryContainer,
                iconTint = StockProColors.Primary,
                title = "Change Password",
                subtitle = "Keep your account secure",
                onClick = { onShowDialog(SettingsDialog.ChangePassword) },
            )
            HorizontalDivider(color = StockProColors.OutlineSubtle)
            SettingsRowItem(
                icon = Icons.Filled.Group,
                iconBg = StockProColors.SuccessContainer,
                iconTint = StockProColors.Success,
                title = "Team & Permissions",
                subtitle = "Manage users and roles",
                onClick = onNavigateToTeam,
            )
        }

        Spacer(modifier = Modifier.height(Spacing.x6))

        // App Preferences Group
        SettingsSectionHeader("App Preferences")
        SettingsGroupCard {
            SettingsRowItem(
                icon = Icons.Filled.Palette,
                iconBg = StockProColors.PrimaryContainer,
                iconTint = StockProColors.Primary,
                title = "Appearance",
                subtitle = "Choose theme mode",
                trailingValue = settings.themeMode.displayLabel,
                onClick = { onShowDialog(SettingsDialog.Appearance) },
            )
            HorizontalDivider(color = StockProColors.OutlineSubtle)
            SettingsRowItem(
                icon = Icons.Filled.Language,
                iconBg = StockProColors.SuccessContainer,
                iconTint = StockProColors.Success,
                title = "Language",
                subtitle = "Choose app language",
                trailingValue = settings.language.displayLabel,
                onClick = { onShowDialog(SettingsDialog.Language) },
            )
            HorizontalDivider(color = StockProColors.OutlineSubtle)
            SettingsRowItem(
                icon = Icons.Filled.Notifications,
                iconBg = StockProColors.WarningContainer,
                iconTint = StockProColors.Warning,
                title = "Notifications",
                subtitle = "Per-type alert preferences",
                trailingValue = "Configure",
                onClick = { onShowDialog(SettingsDialog.NotificationPreferences) },
            )
            HorizontalDivider(color = StockProColors.OutlineSubtle)
            SettingsRowItem(
                icon = Icons.Filled.Straighten,
                iconBg = StockProColors.ProductionContainer,
                iconTint = StockProColors.Production,
                title = "Units",
                subtitle = "Set default measurement system",
                trailingValue = settings.unitSystem.displayLabel,
                onClick = { onShowDialog(SettingsDialog.Units) },
            )
        }

        Spacer(modifier = Modifier.height(Spacing.x6))

        // Data & Storage Group
        SettingsSectionHeader("Data & Storage")
        SettingsGroupCard {
            val syncSubtitle = when (content.syncStatus) {
                is SyncStatus.Synced -> "Synced just now"
                SyncStatus.Syncing -> "Synchronizing..."
                is SyncStatus.Pending -> "${(content.syncStatus as SyncStatus.Pending).count} pending writes"
                is SyncStatus.NeedsAttention -> "${(content.syncStatus as SyncStatus.NeedsAttention).failedCount} items need attention"
            }
            SettingsRowItem(
                icon = Icons.Filled.CloudUpload,
                iconBg = StockProColors.SuccessContainer,
                iconTint = StockProColors.Success,
                title = "Backup & Sync",
                subtitle = syncSubtitle,
                trailingValue = "Auto",
            )
            HorizontalDivider(color = StockProColors.OutlineSubtle)
            SettingsRowItem(
                icon = Icons.Filled.CloudDownload,
                iconBg = StockProColors.PrimaryContainer,
                iconTint = StockProColors.Primary,
                title = "Export Data",
                subtitle = "Download configurations & master records",
                onClick = onExportData,
            )
            HorizontalDivider(color = StockProColors.OutlineSubtle)
            SettingsRowItem(
                icon = Icons.Filled.Delete,
                iconBg = StockProColors.DangerContainer,
                iconTint = StockProColors.Danger,
                title = "Clear Cache",
                subtitle = "Free up storage space",
                trailingValue = "${content.cacheSizeBytes / (1024 * 1024)} MB",
                onClick = { onShowDialog(SettingsDialog.ClearCacheConfirm(content.cacheSizeBytes)) },
            )
        }

        Spacer(modifier = Modifier.height(Spacing.x6))

        // Support Group
        SettingsSectionHeader("Support")
        SettingsGroupCard {
            SettingsRowItem(
                icon = Icons.AutoMirrored.Filled.HelpOutline,
                iconBg = StockProColors.ProductionContainer,
                iconTint = StockProColors.Production,
                title = "Help & FAQs",
                subtitle = "Get help and standard procedures",
                onClick = { onShowDialog(SettingsDialog.HelpFaq) },
            )
            HorizontalDivider(color = StockProColors.OutlineSubtle)
            SettingsRowItem(
                icon = Icons.Filled.Feedback,
                iconBg = StockProColors.SuccessContainer,
                iconTint = StockProColors.Success,
                title = "Send Feedback",
                subtitle = "Help us improve StockPro",
                onClick = { onShowDialog(SettingsDialog.Feedback) },
            )
            HorizontalDivider(color = StockProColors.OutlineSubtle)
            SettingsRowItem(
                icon = Icons.Filled.Mail,
                iconBg = StockProColors.WarningContainer,
                iconTint = StockProColors.Warning,
                title = "Contact Support",
                subtitle = "support@company.com",
                onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "message/rfc822"
                        putExtra(Intent.EXTRA_EMAIL, arrayOf("support@company.com"))
                        putExtra(Intent.EXTRA_SUBJECT, "StockPro Support Inquiry")
                    }
                    val chooser = Intent.createChooser(intent, "Send Email")
                    if (runCatching { context.startActivity(chooser) }.isFailure) {
                        onShowDialog(SettingsDialog.Feedback)
                    }
                },
            )
        }

        Spacer(modifier = Modifier.height(Spacing.x6))

        // About & Legal Group
        SettingsSectionHeader("About & Legal")
        SettingsGroupCard {
            SettingsRowItem(
                icon = Icons.Filled.Info,
                iconBg = StockProColors.PrimaryContainer,
                iconTint = StockProColors.Primary,
                title = "StockPro",
                subtitle = "Server-Authoritative Manufacturing Inventory",
                trailingValue = "v${content.appVersionName} (${content.appVersionCode})",
            )
            HorizontalDivider(color = StockProColors.OutlineSubtle)
            SettingsRowItem(
                icon = Icons.Filled.Security,
                iconBg = StockProColors.SuccessContainer,
                iconTint = StockProColors.Success,
                title = "Privacy Policy",
                subtitle = "DPDP Act 2023 compliance & employee data notice",
                onClick = {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://stockpro-manufacturing.com/privacy"))
                    if (runCatching { context.startActivity(browserIntent) }.isFailure) {
                        onShowDialog(SettingsDialog.PrivacyPolicy)
                    }
                },
            )
            HorizontalDivider(color = StockProColors.OutlineSubtle)
            SettingsRowItem(
                icon = Icons.Filled.Description,
                iconBg = StockProColors.PrimaryContainer,
                iconTint = StockProColors.Primary,
                title = "Terms of Service",
                subtitle = "System terms & data ownership policy",
                onClick = {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://stockpro-manufacturing.com/terms"))
                    if (runCatching { context.startActivity(browserIntent) }.isFailure) {
                        onShowDialog(SettingsDialog.TermsOfService)
                    }
                },
            )
        }

        Spacer(modifier = Modifier.height(Spacing.x6))

        // Logout Button
        Button(
            onClick = { onShowDialog(SettingsDialog.LogoutConfirm) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = StockProColors.DangerContainer),
            shape = Radius.lg,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = null,
                tint = StockProColors.Danger,
            )
            Spacer(modifier = Modifier.width(Spacing.x2))
            Text(
                text = "Logout",
                color = StockProColors.Danger,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
            )
        }

        Spacer(modifier = Modifier.height(Spacing.x2))

        // Delete Account (Play Store Compliance)
        TextButton(
            onClick = { onShowDialog(SettingsDialog.DeleteAccountConfirm) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = StockProColors.Danger,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(Spacing.x2))
            Text(
                text = "Delete Account & Data",
                color = StockProColors.Danger,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Spacer(modifier = Modifier.height(Spacing.x8))
    }

    // Active Dialog Routing
    when (val dialog = content.activeDialog) {
        SettingsDialog.EditProfile -> {
            var newName by remember { mutableStateOf(profile.displayName) }
            AlertDialog(
                onDismissRequest = onDismissDialog,
                title = { Text("Edit Display Name") },
                text = {
                    StockProTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = "Full Name",
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = { onUpdateDisplayName(newName) },
                        enabled = newName.isNotBlank(),
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismissDialog) { Text("Cancel") }
                },
            )
        }
        SettingsDialog.ChangePassword -> {
            AlertDialog(
                onDismissRequest = onDismissDialog,
                title = { Text("Change Password") },
                text = {
                    Text("Password reset instructions will be dispatched to your registered email (${profile.email}).")
                },
                confirmButton = {
                    TextButton(onClick = onDismissDialog) {
                        Text("Send Reset Email", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismissDialog) { Text("Cancel") }
                },
            )
        }
        SettingsDialog.Appearance -> {
            AlertDialog(
                onDismissRequest = onDismissDialog,
                title = { Text("Select Appearance") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.x2)) {
                        ThemeMode.entries.forEach { mode ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(Radius.md)
                                    .background(
                                        if (mode == settings.themeMode) StockProColors.PrimaryContainer else Color.Transparent,
                                    )
                                    .clickable { onUpdateTheme(mode) }
                                    .padding(Spacing.cardPadding),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = mode.displayLabel,
                                    fontWeight = if (mode == settings.themeMode) FontWeight.Bold else FontWeight.Normal,
                                    color = if (mode == settings.themeMode) StockProColors.Primary else StockProColors.TextPrimary,
                                )
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = onDismissDialog) { Text("Close") }
                },
            )
        }
        SettingsDialog.Language -> {
            AlertDialog(
                onDismissRequest = onDismissDialog,
                title = { Text("Select Language") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.x2)) {
                        AppLanguage.entries.forEach { lang ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(Radius.md)
                                    .background(
                                        if (lang == settings.language) StockProColors.PrimaryContainer else Color.Transparent,
                                    )
                                    .clickable { onUpdateLanguage(lang) }
                                    .padding(Spacing.cardPadding),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = lang.displayLabel,
                                    fontWeight = if (lang == settings.language) FontWeight.Bold else FontWeight.Normal,
                                    color = if (lang == settings.language) StockProColors.Primary else StockProColors.TextPrimary,
                                )
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = onDismissDialog) { Text("Close") }
                },
            )
        }
        SettingsDialog.Units -> {
            AlertDialog(
                onDismissRequest = onDismissDialog,
                title = { Text("Measurement Units") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.x2)) {
                        UnitSystem.entries.forEach { unit ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(Radius.md)
                                    .background(
                                        if (unit == settings.unitSystem) StockProColors.PrimaryContainer else Color.Transparent,
                                    )
                                    .clickable { onUpdateUnitSystem(unit) }
                                    .padding(Spacing.cardPadding),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = unit.displayLabel,
                                    fontWeight = if (unit == settings.unitSystem) FontWeight.Bold else FontWeight.Normal,
                                    color = if (unit == settings.unitSystem) StockProColors.Primary else StockProColors.TextPrimary,
                                )
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = onDismissDialog) { Text("Close") }
                },
            )
        }
        SettingsDialog.NotificationPreferences -> {
            var prefs by remember(settings.notificationPreferences) { mutableStateOf(settings.notificationPreferences) }

            AlertDialog(
                onDismissRequest = onDismissDialog,
                title = { Text("Notification Preferences") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.x3)) {
                        Text(
                            text = "Configure discrete alert categories per PRD §8.2:",
                            style = MaterialTheme.typography.bodySmall,
                            color = StockProColors.TextSecondary,
                        )

                        NotificationToggleRow("Low Stock Alerts", "When items cross below par", prefs.lowStockEnabled) {
                            prefs = prefs.copy(lowStockEnabled = it)
                            onUpdateNotificationPreferences(prefs)
                        }
                        NotificationToggleRow("Out of Stock Alerts", "Zero balance events", prefs.outOfStockEnabled) {
                            prefs = prefs.copy(outOfStockEnabled = it)
                            onUpdateNotificationPreferences(prefs)
                        }
                        NotificationToggleRow("PO Due Alerts", "Expected deliveries this week", prefs.poDueEnabled) {
                            prefs = prefs.copy(poDueEnabled = it)
                            onUpdateNotificationPreferences(prefs)
                        }
                        NotificationToggleRow("Large Adjustments", "Adjustments exceeding ₹10,000", prefs.largeAdjustmentEnabled) {
                            prefs = prefs.copy(largeAdjustmentEnabled = it)
                            onUpdateNotificationPreferences(prefs)
                        }
                        NotificationToggleRow("Sync Failures", "Offline queue errors", prefs.syncFailureEnabled) {
                            prefs = prefs.copy(syncFailureEnabled = it)
                            onUpdateNotificationPreferences(prefs)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = onDismissDialog) { Text("Done", fontWeight = FontWeight.Bold) }
                },
                dismissButton = null,
            )
        }
        is SettingsDialog.ClearCacheConfirm -> {
            AlertDialog(
                onDismissRequest = onDismissDialog,
                title = { Text("Clear Local Cache?") },
                text = {
                    Text("This will clear temporary previews and free approximately ${(dialog.estimatedBytes / (1024 * 1024))} MB. Active inventory data in Room will remain intact.")
                },
                confirmButton = {
                    TextButton(onClick = onClearCache) {
                        Text("Clear Cache", color = StockProColors.Danger, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismissDialog) { Text("Cancel") }
                },
            )
        }
        SettingsDialog.LogoutConfirm -> {
            AlertDialog(
                onDismissRequest = onDismissDialog,
                title = { Text("Logout of StockPro?") },
                text = {
                    Text("Your device cached data will be cleared securely and outbox entries preserved until next sign-in.")
                },
                confirmButton = {
                    TextButton(onClick = {
                        onDismissDialog()
                        onLogoutClick()
                    }) {
                        Text("Logout", color = StockProColors.Danger, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismissDialog) { Text("Cancel") }
                },
            )
        }
        SettingsDialog.DeleteAccountConfirm -> {
            AlertDialog(
                onDismissRequest = onDismissDialog,
                title = { Text("Delete Account Permanently?") },
                text = {
                    Text("This action cannot be undone. Per Google Play and data privacy policies, your account and all associated personal data will be completely deleted.")
                },
                confirmButton = {
                    TextButton(onClick = {
                        onDismissDialog()
                        onDeleteAccountClick()
                    }) {
                        Text("Delete Permanently", color = StockProColors.Danger, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismissDialog) { Text("Cancel") }
                },
            )
        }
        SettingsDialog.PrivacyPolicy -> {
            AlertDialog(
                onDismissRequest = onDismissDialog,
                title = { Text("Privacy Policy") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.x2)) {
                        Text("StockPro Manufacturing Inventory Policy", fontWeight = FontWeight.Bold)
                        Text("StockPro complies with the Digital Personal Data Protection (DPDP) Act 2023. We only process operational inventory, tool calibration, and audit movements necessary for factory plant operations.")
                        Text("All transactions are encrypted in transit and at rest. Account deletion purges personal identifiers from the system.")
                    }
                },
                confirmButton = {
                    TextButton(onClick = onDismissDialog) { Text("Close") }
                },
                dismissButton = null,
            )
        }
        SettingsDialog.TermsOfService -> {
            AlertDialog(
                onDismissRequest = onDismissDialog,
                title = { Text("Terms of Service") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.x2)) {
                        Text("StockPro Enterprise Terms", fontWeight = FontWeight.Bold)
                        Text("StockPro provides server-authoritative inventory and stock management services for authorized plant personnel.")
                        Text("Users must safeguard plant credentials and ensure ledger records accurately reflect physical movements.")
                    }
                },
                confirmButton = {
                    TextButton(onClick = onDismissDialog) { Text("Close") }
                },
                dismissButton = null,
            )
        }
        SettingsDialog.HelpFaq -> {
            AlertDialog(
                onDismissRequest = onDismissDialog,
                title = { Text("StockPro Help & FAQs") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.x2)) {
                        Text("1. How does offline sync work?", fontWeight = FontWeight.Bold)
                        Text("Movements post optimistically to the local ledger. When network restores, the OutboxWorker drains them to Firestore.")
                        Spacer(modifier = Modifier.height(Spacing.x1))
                        Text("2. What are par levels?", fontWeight = FontWeight.Bold)
                        Text("Par level is your target safety buffer. Stock levels derive warning badges when available balance falls below reorder points.")
                    }
                },
                confirmButton = {
                    TextButton(onClick = onDismissDialog) { Text("Close") }
                },
                dismissButton = null,
            )
        }
        SettingsDialog.Feedback -> {
            var feedbackText by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = onDismissDialog,
                title = { Text("Send Feedback") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.x2)) {
                        Text("We value your input from the plant shop floor:")
                        StockProTextField(
                            value = feedbackText,
                            onValueChange = { feedbackText = it },
                            label = "Your Thoughts",
                            placeholder = "Describe issues or feature requests...",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = onDismissDialog,
                        enabled = feedbackText.isNotBlank(),
                    ) {
                        Text("Submit", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismissDialog) { Text("Cancel") }
                },
            )
        }
        null -> {}
    }
}

@Composable
private fun NotificationToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = StockProColors.TextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = StockProColors.Primary,
            ),
        )
    }
}

@Composable
private fun SettingsSectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = StockProColors.TextSecondary,
        modifier = Modifier.padding(bottom = Spacing.x2),
    )
}

@Composable
private fun SettingsGroupCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Radius.lg,
        colors = CardDefaults.cardColors(containerColor = StockProColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        content = content,
    )
}

@Composable
private fun SettingsRowItem(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    trailingValue: String? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(Spacing.cardPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(Radius.md)
                .background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.width(Spacing.x3))

        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = StockProColors.TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = StockProColors.TextSecondary)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (trailingValue != null) {
                Text(
                    text = trailingValue,
                    style = MaterialTheme.typography.bodyMedium,
                    color = StockProColors.TextSecondary,
                )
                Spacer(modifier = Modifier.width(Spacing.x2))
            }
            if (onClick != null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = StockProColors.TextTertiary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
