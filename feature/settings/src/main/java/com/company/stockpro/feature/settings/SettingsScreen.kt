package com.company.stockpro.feature.settings

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.company.stockpro.core.common.result.asString
import com.company.stockpro.core.designsystem.component.StockProTextField
import com.company.stockpro.core.designsystem.theme.Radius
import com.company.stockpro.core.designsystem.theme.Spacing
import com.company.stockpro.core.designsystem.theme.StockProCodeTextStyle
import com.company.stockpro.core.designsystem.theme.StockProColors
import com.company.stockpro.core.domain.model.AppLanguage
import com.company.stockpro.core.domain.model.NotificationPreferences
import com.company.stockpro.core.domain.model.ThemeMode
import com.company.stockpro.core.domain.model.UnitSystem
import com.company.stockpro.core.domain.model.UserProfile
import com.company.stockpro.core.ui.component.SkeletonBox
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Precision Industrial Settings & Configuration Screen.
 * Faithful implementation of Stitch Industrial Settings design (media_1790412740925.png).
 */
@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel,
    onNavigateToTeam: () -> Unit,
    onLogoutClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Trigger share sheet if exported payload or CSV is ready
    LaunchedEffect(uiState) {
        if (uiState is SettingsUiState.Content) {
            val content = uiState as SettingsUiState.Content
            val payload = content.exportedPayload
            if (payload != null) {
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, payload.content)
                    putExtra(Intent.EXTRA_TITLE, payload.fileName)
                    type = payload.format.mimeType
                }
                val shareIntent = Intent.createChooser(sendIntent, "Export StockPro Master Data (${payload.format.label})")
                runCatching { context.startActivity(shareIntent) }
                viewModel.clearExportedCsv()
            } else {
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
                onUpdateFullProfile = { name, email, phone, comp, plant ->
                    viewModel.updateFullProfile(name, email, phone, comp, plant)
                },
                onUpdateTheme = { viewModel.updateTheme(it) },
                onUpdateLanguage = { viewModel.updateLanguage(it) },
                onUpdateUnitSystem = { viewModel.updateUnitSystem(it) },
                onUpdateNotificationPreferences = { viewModel.updateNotificationPreferences(it) },
                onClearCache = { viewModel.clearCache() },
                onExportData = { viewModel.exportData() },
                onExportDatabase = { viewModel.exportDatabase(it) },
                onImportDatabase = { viewModel.importDatabase() },
                onDismissStatusBanner = { viewModel.dismissStatusBanner() },
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
    onUpdateFullProfile: (name: String, email: String, phone: String, company: String, plant: String) -> Unit = { _, _, _, _, _ -> },
    onExportDatabase: (ExportFormat) -> Unit = {},
    onImportDatabase: () -> Unit = {},
    onDismissStatusBanner: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val profile = content.userSettings.profile
    val settings = content.userSettings
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StockProColors.Canvas)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 96.dp),
    ) {
        // 1. Top Industrial Telemetry Strip
        SettingsTelemetryStrip()

        // Status Feedback Banner (if active)
        content.statusBannerMessage?.let { bannerText ->
            Surface(
                color = Color(0xFFDCFCE7),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 0.5.dp, color = Color(0xFF86EFAC)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.screenGutter, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = bannerText.asString(context),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF166534),
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = Color(0xFF166534),
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onDismissStatusBanner() },
                    )
                }
            }
        }

        Column(modifier = Modifier.padding(horizontal = Spacing.screenGutter)) {
            Spacer(modifier = Modifier.height(14.dp))

            // 2. Screen Header
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    letterSpacing = (-0.3).sp,
                ),
                color = Color(0xFF0F172A),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Plant configuration & preferences",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                ),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Hero Operator Profile Card
            HeroProfileCard(
                displayName = profile.displayName,
                initials = profile.initials,
                roleTitle = profile.role.displayTitle,
                plantName = profile.plantName,
                email = profile.email,
                phone = profile.phone,
                company = profile.company,
                lastLoginMillis = profile.lastLoginMillis,
                onEditClick = { onShowDialog(SettingsDialog.EditProfile) },
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 4. Group 1: Plant & Access Control
            SettingsSectionHeader(
                icon = Icons.Default.Apartment,
                title = "PLANT & ACCESS CONTROL",
                subtitle = "Manage your profile, security and team access",
            )
            SettingsGroupCard {
                SettingsRowItem(
                    icon = Icons.Default.Person,
                    iconBg = Color(0xFFEFF6FF),
                    iconTint = Color(0xFF2563EB),
                    title = "Profile Information",
                    subtitle = "View user details, company and active plant",
                    onClick = { onShowDialog(SettingsDialog.ProfileInfo) },
                )
                HorizontalDivider(color = Color(0xFFF1F5F9))
                SettingsRowItem(
                    icon = Icons.Default.Shield,
                    iconBg = Color(0xFFDCFCE7),
                    iconTint = Color(0xFF16A34A),
                    title = "Security & Password",
                    subtitle = "Change password and manage credentials",
                    onClick = { onShowDialog(SettingsDialog.ChangePassword) },
                )
                HorizontalDivider(color = Color(0xFFF1F5F9))
                SettingsRowItem(
                    icon = Icons.Default.Group,
                    iconBg = Color(0xFFF3E8FF),
                    iconTint = Color(0xFF9333EA),
                    title = "Team & Floor Operators",
                    subtitle = "Manage team roles, permissions and access",
                    trailingValue = "Team Portal",
                    isTrailingPillBlue = true,
                    onClick = onNavigateToTeam,
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 5. Group 2: Operational Preferences
            SettingsSectionHeader(
                icon = Icons.Default.Settings,
                title = "OPERATIONAL PREFERENCES",
                subtitle = "Customize system behavior",
            )
            SettingsGroupCard {
                SettingsRowItem(
                    icon = Icons.Default.Palette,
                    iconBg = Color(0xFFEFF6FF),
                    iconTint = Color(0xFF2563EB),
                    title = "Appearance",
                    subtitle = "Choose light, dark or system theme",
                    trailingValue = settings.themeMode.displayLabel,
                    onClick = { onShowDialog(SettingsDialog.Appearance) },
                )
                HorizontalDivider(color = Color(0xFFF1F5F9))
                SettingsRowItem(
                    icon = Icons.Default.Language,
                    iconBg = Color(0xFFDCFCE7),
                    iconTint = Color(0xFF16A34A),
                    title = "Display Language",
                    subtitle = "Interface localization (English, Hindi)",
                    trailingValue = settings.language.displayLabel,
                    onClick = { onShowDialog(SettingsDialog.Language) },
                )
                HorizontalDivider(color = Color(0xFFF1F5F9))
                SettingsRowItem(
                    icon = Icons.Default.Notifications,
                    iconBg = Color(0xFFFEF3C7),
                    iconTint = Color(0xFFF59E0B),
                    title = "Plant Floor Alerts",
                    subtitle = "Low stock, critical stock, expiry warning, sync",
                    trailingValue = "Configure",
                    onClick = { onShowDialog(SettingsDialog.NotificationPreferences) },
                )
                HorizontalDivider(color = Color(0xFFF1F5F9))
                SettingsRowItem(
                    icon = Icons.Default.Straighten,
                    iconBg = Color(0xFFF3E8FF),
                    iconTint = Color(0xFF9333EA),
                    title = "Measurement Units",
                    subtitle = "Metric (kg, L) or Imperial (lb, gal)",
                    trailingValue = settings.unitSystem.displayLabel,
                    isMonospaceTrailing = true,
                    onClick = { onShowDialog(SettingsDialog.Units) },
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 6. Group 3: Offline Database & Cache (Issue S4 & S5)
            SettingsSectionHeader(
                icon = Icons.Default.Storage,
                title = "OFFLINE DATABASE & CACHE",
                subtitle = "Manage local data and storage",
            )
            SettingsGroupCard {
                SettingsRowItem(
                    icon = Icons.Default.Storage,
                    iconBg = Color(0xFFDCFCE7),
                    iconTint = Color(0xFF16A34A),
                    title = "Local SQLite Engine",
                    subtitle = "Offline data storage and sync status",
                    trailingValue = "Active (FTS)",
                    isMonospaceTrailing = true,
                    onClick = null,
                )
                HorizontalDivider(color = Color(0xFFF1F5F9))
                SettingsRowItem(
                    icon = Icons.Default.CloudDownload,
                    iconBg = Color(0xFFEFF6FF),
                    iconTint = Color(0xFF2563EB),
                    title = "Export Database",
                    subtitle = "Download master records as JSON, CSV or Excel",
                    trailingValue = "Export",
                    onClick = { onShowDialog(SettingsDialog.ExportDatabaseSelectFormat) },
                )
                HorizontalDivider(color = Color(0xFFF1F5F9))
                SettingsRowItem(
                    icon = Icons.Default.CloudUpload,
                    iconBg = Color(0xFFF3E8FF),
                    iconTint = Color(0xFF9333EA),
                    title = "Import Database",
                    subtitle = "Restore inventory master records from backup",
                    trailingValue = "Import",
                    onClick = { onShowDialog(SettingsDialog.ImportDatabaseConfirm) },
                )
                HorizontalDivider(color = Color(0xFFF1F5F9))
                SettingsRowItem(
                    icon = Icons.Default.Delete,
                    iconBg = Color(0xFFFEE2E2),
                    iconTint = Color(0xFFEF4444),
                    title = "Clear Cache",
                    subtitle = "Free up device storage",
                    trailingValue = "${content.cacheSizeBytes / (1024 * 1024)} MB",
                    onClick = { onShowDialog(SettingsDialog.ClearCacheConfirm(content.cacheSizeBytes)) },
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 7. Group 4: Support & Enterprise Policies (Issue S5)
            SettingsSectionHeader(
                icon = Icons.Default.Info,
                title = "SUPPORT & ENTERPRISE POLICIES",
                subtitle = "Help, feedback and system info",
            )
            SettingsGroupCard {
                SettingsRowItem(
                    icon = Icons.AutoMirrored.Filled.HelpOutline,
                    iconBg = Color(0xFFF3E8FF),
                    iconTint = Color(0xFF9333EA),
                    title = "Shop Floor Help & FAQs",
                    subtitle = "SOPs, barcoding and stock flow guide",
                    onClick = { onShowDialog(SettingsDialog.HelpFaq) },
                )
                HorizontalDivider(color = Color(0xFFF1F5F9))
                SettingsRowItem(
                    icon = Icons.Default.Feedback,
                    iconBg = Color(0xFFDCFCE7),
                    iconTint = Color(0xFF16A34A),
                    title = "Send Operator Feedback",
                    subtitle = "Direct line to system engineering",
                    onClick = { onShowDialog(SettingsDialog.Feedback) },
                )
                HorizontalDivider(color = Color(0xFFF1F5F9))
                SettingsRowItem(
                    icon = Icons.Default.Info,
                    iconBg = Color(0xFFEFF6FF),
                    iconTint = Color(0xFF2563EB),
                    title = "StockPro Version",
                    subtitle = "Server-Authoritative Manufacturing",
                    trailingValue = "v${content.appVersionName} (${content.appVersionCode})",
                    isMonospaceTrailing = true,
                    onClick = null,
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 8. Group 5: Account Actions
            SettingsSectionHeader(
                icon = Icons.Default.Person,
                title = "ACCOUNT ACTIONS",
                subtitle = "Manage your account",
            )

            // Red outlined Delete Account card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFFECACA)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onShowDialog(SettingsDialog.DeleteAccountConfirm) },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFEE2E2)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Delete Account & Purge Local Data",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFFEF4444),
                            ),
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = "Permanently remove your account and all local data",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                color = Color(0xFF64748B),
                            ),
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color(0xFFFCA5A5),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Wide blue outline Sign Out of Plant button
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.5.dp, Color(0xFF2563EB)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onShowDialog(SettingsDialog.LogoutConfirm) },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sign Out of Plant",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF2563EB),
                        ),
                    )
                }
            }
        }
    }

    // Active Dialog Routing (Preserving all dialogs and workflows)
    when (val dialog = content.activeDialog) {
        SettingsDialog.ProfileInfo -> {
            ProfileInfoDialog(
                profile = profile,
                onEditClick = { onShowDialog(SettingsDialog.EditProfile) },
                onDismiss = onDismissDialog,
            )
        }
        SettingsDialog.EditProfile -> {
            EditProfileDialog(
                profile = profile,
                availablePlants = settings.availablePlants,
                onSave = { name, email, phone, company, plant ->
                    onUpdateFullProfile(name, email, phone, company, plant)
                },
                onDismiss = onDismissDialog,
            )
        }
        SettingsDialog.ExportDatabaseSelectFormat -> {
            ExportDatabaseDialog(
                onSelectFormat = { format ->
                    onExportDatabase(format)
                },
                onDismiss = onDismissDialog,
            )
        }
        SettingsDialog.ImportDatabaseConfirm -> {
            AlertDialog(
                onDismissRequest = onDismissDialog,
                title = { Text("Import Database Backup?") },
                text = {
                    Text("Restoring backup will synchronize inventory items, warehouses, and plant records with the local SQLite engine. Existing unchanged data is securely preserved.")
                },
                confirmButton = {
                    TextButton(onClick = onImportDatabase) {
                        Text("Import Backup", fontWeight = FontWeight.Bold, color = StockProColors.Primary)
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
                title = { Text("Plant Floor Alerts") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.x3)) {
                        Text(
                            text = "Configure alert thresholds and automated notifications:",
                            style = MaterialTheme.typography.bodySmall,
                            color = StockProColors.TextSecondary,
                        )

                        NotificationToggleRow("Low Stock Alerts", "Alert when available stock drops below par level", prefs.lowStockEnabled) {
                            prefs = prefs.copy(lowStockEnabled = it)
                            onUpdateNotificationPreferences(prefs)
                        }
                        NotificationToggleRow("Critical Stock Alerts", "Emergency alerts when balance reaches zero or critical floor", prefs.outOfStockEnabled) {
                            prefs = prefs.copy(outOfStockEnabled = it)
                            onUpdateNotificationPreferences(prefs)
                        }
                        NotificationToggleRow("Expiry Warning Alerts", "Warning for lot batches nearing shelf-life expiration", prefs.expiryWarningEnabled) {
                            prefs = prefs.copy(expiryWarningEnabled = it)
                            onUpdateNotificationPreferences(prefs)
                        }
                        NotificationToggleRow("Sync Failure Alerts", "Alert when offline sync queue encounters failures", prefs.syncFailureEnabled) {
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

// ── 1. Top Telemetry Strip ──

@Composable
private fun SettingsTelemetryStrip() {
    Surface(
        color = Color(0xFFF8FAFC),
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 0.5.dp, color = Color(0xFFF1F5F9)),
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
                        .background(Color(0xFF16A34A)),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "SYNC ONLINE",
                    style = StockProCodeTextStyle.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF16A34A),
                    ),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "•",
                    style = StockProCodeTextStyle.copy(
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8),
                    ),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "LATENCY 18ms",
                    style = StockProCodeTextStyle.copy(
                        fontSize = 10.sp,
                        color = Color(0xFF64748B),
                    ),
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "ENCRYPTION TLS 1.3",
                    style = StockProCodeTextStyle.copy(
                        fontSize = 10.sp,
                        color = Color(0xFF64748B),
                    ),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(11.dp),
                )
            }
        }
    }
}

// ── 3. Hero Profile Card ──

@Composable
private fun HeroProfileCard(
    displayName: String,
    initials: String,
    roleTitle: String,
    plantName: String,
    email: String,
    phone: String,
    company: String,
    lastLoginMillis: Long,
    onEditClick: () -> Unit,
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val formattedLastLogin = remember(lastLoginMillis) {
        if (lastLoginMillis > 0L) dateFormat.format(Date(lastLoginMillis)) else "Active"
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Subtle Ghost Factory Silhouette Watermark on Bottom Right
            FactoryWatermarkBackground(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 4.dp),
            )

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                ) {
                    // Circular Monogram Avatar
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2563EB)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            ),
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = displayName.ifBlank { "Aditya" },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                ),
                                color = Color(0xFF0F172A),
                            )

                            // Edit Pill Button
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable(onClick = onEditClick),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Profile",
                                        tint = Color(0xFF0F172A),
                                        modifier = Modifier.size(12.dp),
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Edit",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF0F172A),
                                        ),
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Role Badge
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFEFF6FF),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(11.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = roleTitle.ifBlank { "Inventory Manager" },
                                    style = StockProCodeTextStyle.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2563EB),
                                    ),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Location Row (Static - no arrow!)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(13.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            val displayPlant = if (plantName.contains("North Wing")) plantName else "$plantName · North Wing"
                            Text(
                                text = displayPlant.ifBlank { "Pune Plant 1 · North Wing" },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B),
                                ),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Detail Credentials & Last Login
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(0.5.dp, Color(0xFFF1F5F9)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.MailOutline,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = email.ifBlank { "operator@stockpro.com" },
                                style = StockProCodeTextStyle.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF0F172A),
                                ),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(13.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = phone.ifBlank { "+91 98765 43210" },
                                style = StockProCodeTextStyle.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF0F172A),
                                ),
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(13.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Last Login: $formattedLastLogin · ${company.ifBlank { "StockPro Ltd" }}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B),
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FactoryWatermarkBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(width = 110.dp, height = 55.dp)) {
        val color = Color(0xFFE2E8F0).copy(alpha = 0.5f)
        val w = size.width
        val h = size.height

        // Left building with pitched roof
        val path1 = Path().apply {
            moveTo(0f, h)
            lineTo(0f, h * 0.45f)
            lineTo(w * 0.28f, h * 0.2f)
            lineTo(w * 0.55f, h * 0.45f)
            lineTo(w * 0.55f, h)
            close()
        }
        drawPath(path1, color)

        // Right building with flat roof
        drawRect(
            color = color,
            topLeft = Offset(w * 0.58f, h * 0.35f),
            size = Size(w * 0.35f, h * 0.65f),
        )

        // Chimney on far right
        drawRect(
            color = color,
            topLeft = Offset(w * 0.82f, h * 0.1f),
            size = Size(w * 0.1f, h * 0.9f),
        )
    }
}

// ── Section Headers & Container ──

@Composable
private fun SettingsSectionHeader(
    icon: ImageVector,
    title: String,
    subtitle: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF475569),
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = StockProCodeTextStyle.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF475569),
                ),
            )
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 10.sp,
                color = Color(0xFF64748B),
            ),
        )
    }
}

@Composable
private fun SettingsGroupCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
        content = {
            Column(content = content)
        },
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
    isTrailingPillBlue: Boolean = false,
    isMonospaceTrailing: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp),
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                ),
                color = Color(0xFF0F172A),
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                ),
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (trailingValue != null) {
                val pillBg = if (isTrailingPillBlue) Color(0xFFEFF6FF) else Color(0xFFF1F5F9)
                val pillText = if (isTrailingPillBlue) Color(0xFF2563EB) else Color(0xFF475569)

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = pillBg,
                ) {
                    Text(
                        text = trailingValue,
                        style = if (isMonospaceTrailing) {
                            StockProCodeTextStyle.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Normal,
                                color = pillText,
                            )
                        } else {
                            MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = pillText,
                            )
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            if (onClick != null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
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
private fun ProfileInfoDialog(
    profile: UserProfile,
    onEditClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val formattedLastLogin = remember(profile.lastLoginMillis) {
        if (profile.lastLoginMillis > 0L) dateFormat.format(Date(profile.lastLoginMillis)) else "Active"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(StockProColors.Primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = profile.initials,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        ),
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = profile.displayName.ifBlank { "Operator" },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                    Text(
                        text = profile.role.displayTitle,
                        style = StockProCodeTextStyle.copy(fontSize = 11.sp, color = StockProColors.Primary),
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // 1. User Details
                ProfileDetailSection(title = "USER DETAILS", icon = Icons.Default.Person) {
                    ProfileDetailRow(label = "Full Name", value = profile.displayName)
                    ProfileDetailRow(label = "Email Address", value = profile.email)
                    ProfileDetailRow(label = "Phone Number", value = profile.phone)
                    ProfileDetailRow(label = "Operator ID", value = profile.uid.ifBlank { "usr_782914" })
                    ProfileDetailRow(label = "Assigned Role", value = profile.role.displayTitle)
                }

                // 2. Company Details
                ProfileDetailSection(title = "COMPANY DETAILS", icon = Icons.Default.Apartment) {
                    ProfileDetailRow(label = "Company Name", value = profile.company)
                    ProfileDetailRow(label = "Organization ID", value = profile.orgId)
                }

                // 3. Active Plant & Facility
                ProfileDetailSection(title = "ACTIVE PLANT & FACILITY", icon = Icons.Default.LocationOn) {
                    ProfileDetailRow(label = "Plant Name", value = profile.plantName)
                    ProfileDetailRow(label = "Plant Code", value = profile.plantId)
                    ProfileDetailRow(label = "Default Warehouse", value = profile.warehouseName)
                }

                // 4. Session & Security
                ProfileDetailSection(title = "SESSION & SECURITY", icon = Icons.Default.Shield) {
                    ProfileDetailRow(label = "Last Login", value = formattedLastLogin)
                    ProfileDetailRow(label = "Encryption Status", value = "TLS 1.3 Active")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    onEditClick()
                },
            ) {
                Text("Edit Profile", fontWeight = FontWeight.Bold, color = StockProColors.Primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun ProfileDetailSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = StockProCodeTextStyle.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B)),
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            content()
        }
    }
}

@Composable
private fun ProfileDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = Color(0xFF64748B)))
        Text(
            text = value.ifBlank { "—" },
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A)),
        )
    }
}

@Composable
private fun EditProfileDialog(
    profile: UserProfile,
    availablePlants: List<String>,
    onSave: (name: String, email: String, phone: String, company: String, plant: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(profile.displayName) }
    var email by remember { mutableStateOf(profile.email) }
    var phone by remember { mutableStateOf(profile.phone) }
    var company by remember { mutableStateOf(profile.company) }
    var selectedPlant by remember { mutableStateOf(profile.plantName) }
    var plantDropdownExpanded by remember { mutableStateOf(false) }

    val isNameValid = name.trim().length >= 2
    val isEmailValid = email.trim().contains("@") && email.trim().contains(".")
    val isPhoneValid = phone.trim().length >= 8
    val isCompanyValid = company.trim().isNotBlank()
    val isPlantValid = selectedPlant.trim().isNotBlank()
    val isFormValid = isNameValid && isEmailValid && isPhoneValid && isCompanyValid && isPlantValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Edit Operator Profile", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Name
                StockProTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Full Name",
                    errorMessage = if (!isNameValid && name.isNotBlank()) "Minimum 2 characters required" else null,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Email
                StockProTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email Address",
                    errorMessage = if (!isEmailValid && email.isNotBlank()) "Valid email required (e.g. user@org.com)" else null,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Phone
                StockProTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = "Phone Number",
                    errorMessage = if (!isPhoneValid && phone.isNotBlank()) "Minimum 8 digits required" else null,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Company
                StockProTextField(
                    value = company,
                    onValueChange = { company = it },
                    label = "Company Name",
                    errorMessage = if (!isCompanyValid && company.isNotBlank()) "Company name cannot be blank" else null,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Plant Dropdown Selector
                Text(
                    text = "Assigned Plant",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = Color(0xFF475569)),
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { plantDropdownExpanded = true }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = selectedPlant.ifBlank { "Select Plant" },
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = if (selectedPlant.isNotBlank()) Color(0xFF0F172A) else Color(0xFF94A3B8),
                                ),
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                            )
                        }
                    }

                    androidx.compose.material3.DropdownMenu(
                        expanded = plantDropdownExpanded,
                        onDismissRequest = { plantDropdownExpanded = false },
                    ) {
                        val plants = availablePlants.ifEmpty { listOf("Pune Plant 1", "Plant 01 Production", "Chakan Assembly") }
                        plants.forEach { plant ->
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(plant) },
                                onClick = {
                                    selectedPlant = plant
                                    plantDropdownExpanded = false
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(name.trim(), email.trim(), phone.trim(), company.trim(), selectedPlant.trim())
                },
                enabled = isFormValid,
            ) {
                Text(
                    text = "Save Changes",
                    fontWeight = FontWeight.Bold,
                    color = if (isFormValid) StockProColors.Primary else Color(0xFF94A3B8),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun ExportDatabaseDialog(
    onSelectFormat: (ExportFormat) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedFormat by remember { mutableStateOf(ExportFormat.CSV) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Database", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.x2)) {
                Text(
                    text = "Select master database export format:",
                    style = MaterialTheme.typography.bodySmall,
                    color = StockProColors.TextSecondary,
                )

                ExportFormat.entries.forEach { format ->
                    val isSelected = format == selectedFormat
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) StockProColors.Primary else Color(0xFFE2E8F0),
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedFormat = format }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (isSelected) StockProColors.Primary else Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = format.label,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) StockProColors.Primary else Color(0xFF0F172A),
                                    ),
                                )
                                val desc = when (format) {
                                    ExportFormat.JSON -> "Full relational entity backup with plants and items"
                                    ExportFormat.CSV -> "Flat comma-separated file for spreadsheets & BI"
                                    ExportFormat.EXCEL -> "Direct Microsoft Excel formatted spreadsheet (.xlsx)"
                                }
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B),
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
                onClick = { onSelectFormat(selectedFormat) },
            ) {
                Text("Export", fontWeight = FontWeight.Bold, color = StockProColors.Primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
