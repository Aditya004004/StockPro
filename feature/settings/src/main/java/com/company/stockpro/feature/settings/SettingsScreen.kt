package com.company.stockpro.feature.settings

import android.content.Intent
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Store
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.company.stockpro.core.common.result.asString
import com.company.stockpro.core.designsystem.component.StockProTextField
import com.company.stockpro.core.designsystem.theme.Radius
import com.company.stockpro.core.designsystem.theme.Spacing
import com.company.stockpro.core.designsystem.theme.StockProCodeTextStyle
import com.company.stockpro.core.designsystem.theme.StockProColors
import com.company.stockpro.core.designsystem.theme.StockProStrings
import com.company.stockpro.core.domain.model.AppLanguage
import com.company.stockpro.core.domain.model.NotificationPreferences
import com.company.stockpro.core.domain.model.Plant
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
    onNavigateToExportDatabase: () -> Unit = {},
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
                onRequestRemovePlantConfirm = { viewModel.requestRemovePlantConfirm(it) },
                onRemovePlant = { plantId ->
                    viewModel.removePlant(
                        plantId = plantId,
                        onError = { msg ->
                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                        },
                    )
                },
                onNavigateToTeam = onNavigateToTeam,
                onNavigateToExportDatabase = onNavigateToExportDatabase,
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
    onNavigateToExportDatabase: (() -> Unit)? = null,
    onImportDatabase: () -> Unit = {},
    onDismissStatusBanner: () -> Unit = {},
    onRequestRemovePlantConfirm: (Plant) -> Unit = {},
    onRemovePlant: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val profile = content.userSettings.profile
    val settings = content.userSettings
    val context = LocalContext.current
    val strings = StockProStrings.current

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
                text = strings.settingsHeader,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    letterSpacing = (-0.3).sp,
                ),
                color = Color(0xFF0F172A),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = strings.settingsSubtitle,
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
                title = strings.plantAndAccessControl,
                subtitle = "Manage your profile, security and team access",
            )
            SettingsGroupCard {
                SettingsRowItem(
                    icon = Icons.Default.Person,
                    iconBg = Color(0xFFEFF6FF),
                    iconTint = Color(0xFF2563EB),
                    title = strings.profileInformation,
                    subtitle = "View user details, company and active plant",
                    onClick = { onShowDialog(SettingsDialog.ProfileInfo) },
                )
                HorizontalDivider(color = Color(0xFFF1F5F9))
                SettingsRowItem(
                    icon = Icons.Default.Shield,
                    iconBg = Color(0xFFDCFCE7),
                    iconTint = Color(0xFF16A34A),
                    title = strings.securityAndPassword,
                    subtitle = "Change password and manage credentials",
                    onClick = { onShowDialog(SettingsDialog.ChangePassword) },
                )
                HorizontalDivider(color = Color(0xFFF1F5F9))
                SettingsRowItem(
                    icon = Icons.Default.Delete,
                    iconBg = Color(0xFFFEE2E2),
                    iconTint = Color(0xFFDC2626),
                    title = strings.removePlant,
                    subtitle = "Decommission and remove an existing plant",
                    onClick = { onShowDialog(SettingsDialog.RemovePlant) },
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 5. Group 2: Operational Preferences
            SettingsSectionHeader(
                icon = Icons.Default.Settings,
                title = strings.operationalPreferences,
                subtitle = strings.appearanceSubtitle,
            )
            SettingsGroupCard {
                val themeLabel = when (settings.themeMode) {
                    ThemeMode.SYSTEM -> strings.systemDefault
                    ThemeMode.LIGHT -> strings.lightMode
                    ThemeMode.DARK -> strings.darkMode
                }
                SettingsRowItem(
                    icon = Icons.Default.Palette,
                    iconBg = Color(0xFFEFF6FF),
                    iconTint = Color(0xFF2563EB),
                    title = strings.appearanceGroup,
                    subtitle = "Choose light, dark or system theme",
                    trailingValue = themeLabel,
                    onClick = { onShowDialog(SettingsDialog.Appearance) },
                )
                HorizontalDivider(color = Color(0xFFF1F5F9))
                SettingsRowItem(
                    icon = Icons.Default.Language,
                    iconBg = Color(0xFFDCFCE7),
                    iconTint = Color(0xFF16A34A),
                    title = strings.displayLanguage,
                    subtitle = strings.displayLanguageSubtitle,
                    trailingValue = settings.language.displayLabel,
                    onClick = { onShowDialog(SettingsDialog.Language) },
                )
                HorizontalDivider(color = Color(0xFFF1F5F9))
                SettingsRowItem(
                    icon = Icons.Default.Notifications,
                    iconBg = Color(0xFFFEF3C7),
                    iconTint = Color(0xFFF59E0B),
                    title = strings.floorAlerts,
                    subtitle = "Low stock, critical stock, expiry warning, sync",
                    trailingValue = "Configure",
                    onClick = { onShowDialog(SettingsDialog.NotificationPreferences) },
                )
                HorizontalDivider(color = Color(0xFFF1F5F9))
                SettingsRowItem(
                    icon = Icons.Default.Straighten,
                    iconBg = Color(0xFFF3E8FF),
                    iconTint = Color(0xFF9333EA),
                    title = strings.measurementUnits,
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
                title = strings.offlineDatabaseGroup,
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
                    title = strings.exportDatabase,
                    subtitle = "Download master records as JSON, CSV or Excel",
                    trailingValue = "Export",
                    onClick = { onNavigateToExportDatabase?.invoke() ?: onShowDialog(SettingsDialog.ExportDatabaseSelectFormat) },
                )
                HorizontalDivider(color = Color(0xFFF1F5F9))
                SettingsRowItem(
                    icon = Icons.Default.Delete,
                    iconBg = Color(0xFFFEE2E2),
                    iconTint = Color(0xFFEF4444),
                    title = strings.clearCache,
                    subtitle = "Free up device storage",
                    trailingValue = "${content.cacheSizeBytes / (1024 * 1024)} MB",
                    onClick = { onShowDialog(SettingsDialog.ClearCacheConfirm(content.cacheSizeBytes)) },
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 7. Group 4: Support & Enterprise Policies (Issue S5)
            SettingsSectionHeader(
                icon = Icons.Default.Info,
                title = strings.supportAndEnterprise,
                subtitle = "Help, feedback and system info",
            )
            SettingsGroupCard {
                SettingsRowItem(
                    icon = Icons.AutoMirrored.Filled.HelpOutline,
                    iconBg = Color(0xFFF3E8FF),
                    iconTint = Color(0xFF9333EA),
                    title = strings.helpAndFaqs,
                    subtitle = "SOPs, barcoding and stock flow guide",
                    onClick = { onShowDialog(SettingsDialog.HelpFaq) },
                )
                HorizontalDivider(color = Color(0xFFF1F5F9))
                SettingsRowItem(
                    icon = Icons.Default.Feedback,
                    iconBg = Color(0xFFDCFCE7),
                    iconTint = Color(0xFF16A34A),
                    title = strings.sendFeedback,
                    subtitle = "Direct line to system engineering",
                    onClick = { onShowDialog(SettingsDialog.Feedback) },
                )
                HorizontalDivider(color = Color(0xFFF1F5F9))
                SettingsRowItem(
                    icon = Icons.Default.Info,
                    iconBg = Color(0xFFEFF6FF),
                    iconTint = Color(0xFF2563EB),
                    title = strings.appVersion,
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
                title = strings.accountActions,
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
                            text = strings.deleteAccountAndData,
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

            // Sign Out Account Action
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onShowDialog(SettingsDialog.LogoutConfirm) },
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
                            .background(Color(0xFFEFF6FF)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.signOut,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF2563EB),
                            ),
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = strings.signOutSubtitle,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                color = Color(0xFF64748B),
                            ),
                        )
                    }
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
                title = { Text(strings.selectAppearance) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.x2)) {
                        ThemeMode.entries.forEach { mode ->
                            val label = when (mode) {
                                ThemeMode.SYSTEM -> strings.systemDefault
                                ThemeMode.LIGHT -> strings.lightMode
                                ThemeMode.DARK -> strings.darkMode
                            }
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
                                    text = label,
                                    fontWeight = if (mode == settings.themeMode) FontWeight.Bold else FontWeight.Normal,
                                    color = if (mode == settings.themeMode) StockProColors.Primary else StockProColors.TextPrimary,
                                )
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = onDismissDialog) { Text(strings.close) }
                },
            )
        }
        SettingsDialog.Language -> {
            AlertDialog(
                onDismissRequest = onDismissDialog,
                title = { Text(strings.selectLanguage) },
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
                    TextButton(onClick = onDismissDialog) { Text(strings.close) }
                },
            )
        }
        SettingsDialog.Units -> {
            AlertDialog(
                onDismissRequest = onDismissDialog,
                title = { Text(strings.measurementUnits) },
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
                    TextButton(onClick = onDismissDialog) { Text(strings.close) }
                },
            )
        }
        SettingsDialog.NotificationPreferences -> {
            var prefs by remember(settings.notificationPreferences) { mutableStateOf(settings.notificationPreferences) }

            AlertDialog(
                onDismissRequest = onDismissDialog,
                title = { Text(strings.floorAlerts) },
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
                    TextButton(onClick = onDismissDialog) { Text(strings.confirm, fontWeight = FontWeight.Bold) }
                },
                dismissButton = null,
            )
        }
        is SettingsDialog.ClearCacheConfirm -> {
            AlertDialog(
                onDismissRequest = onDismissDialog,
                title = { Text("${strings.clearCache}?") },
                text = {
                    Text("This will clear temporary previews and free approximately ${(dialog.estimatedBytes / (1024 * 1024))} MB. Active inventory data in Room will remain intact.")
                },
                confirmButton = {
                    TextButton(onClick = onClearCache) {
                        Text(strings.clearCache, color = StockProColors.Danger, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismissDialog) { Text(strings.cancel) }
                },
            )
        }
        SettingsDialog.LogoutConfirm -> {
            AlertDialog(
                onDismissRequest = onDismissDialog,
                title = { Text("${strings.signOut}?") },
                text = {
                    Text("Your device cached data will be cleared securely and outbox entries preserved until next sign-in.")
                },
                confirmButton = {
                    TextButton(onClick = {
                        onDismissDialog()
                        onLogoutClick()
                    }) {
                        Text(strings.signOut, color = StockProColors.Danger, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismissDialog) { Text(strings.cancel) }
                },
            )
        }
        SettingsDialog.DeleteAccountConfirm -> {
            AlertDialog(
                onDismissRequest = onDismissDialog,
                title = { Text("${strings.deleteAccount}?") },
                text = {
                    Text("This action cannot be undone. Per Google Play and data privacy policies, your account and all associated personal data will be completely deleted.")
                },
                confirmButton = {
                    TextButton(onClick = {
                        onDismissDialog()
                        onDeleteAccountClick()
                    }) {
                        Text(strings.deleteAccount, color = StockProColors.Danger, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismissDialog) { Text(strings.cancel) }
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
            val keyboardController = LocalSoftwareKeyboardController.current
            val focusManager = LocalFocusManager.current

            AlertDialog(
                onDismissRequest = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    onDismissDialog()
                },
                properties = DialogProperties(decorFitsSystemWindows = false),
                modifier = Modifier.imePadding(),
                title = { Text("Send Feedback") },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(Spacing.x2),
                    ) {
                        Text("We value your input from the plant shop floor:")
                        StockProTextField(
                            value = feedbackText,
                            onValueChange = { feedbackText = it },
                            label = "Your Thoughts",
                            placeholder = "Describe issues or feature requests...",
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                }
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            onDismissDialog()
                        },
                        enabled = feedbackText.isNotBlank(),
                    ) {
                        Text("Submit", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        onDismissDialog()
                    }) { Text("Cancel") }
                },
            )
        }
        SettingsDialog.RemovePlant -> {
            val plantsToDisplay = if (content.configuredPlants.isNotEmpty()) {
                content.configuredPlants
            } else {
                settings.availablePlants.mapIndexed { idx, name ->
                    Plant(
                        id = "plant_${idx + 1}",
                        code = name.take(3).uppercase() + "-${(idx + 1).toString().padStart(2, '0')}",
                        name = name,
                        location = name,
                    )
                }
            }
            RemovePlantSelectionDialog(
                plants = plantsToDisplay,
                activePlantName = profile.plantName,
                onSelectPlantToDelete = { plant ->
                    onRequestRemovePlantConfirm(plant)
                },
                onDismiss = onDismissDialog,
            )
        }
        is SettingsDialog.RemovePlantConfirm -> {
            RemovePlantConfirmDialog(
                plant = dialog.plant,
                itemCount = dialog.itemCount,
                isActivePlant = dialog.plant.id == profile.plantId || dialog.plant.name.equals(profile.plantName, ignoreCase = true),
                onConfirmDelete = {
                    onRemovePlant(dialog.plant.id)
                },
                onDismiss = onDismissDialog,
            )
        }
        SettingsDialog.ImportDatabaseConfirm -> {}
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
        color = StockProColors.Surface,
        border = BorderStroke(1.dp, StockProColors.OutlineSubtle),
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
                            .background(StockProColors.Primary),
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
                                color = StockProColors.TextPrimary,
                            )

                            // Edit Pill Button
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = StockProColors.SurfaceAlt,
                                border = BorderStroke(1.dp, StockProColors.Outline),
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
                                        tint = StockProColors.TextPrimary,
                                        modifier = Modifier.size(12.dp),
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Edit",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = StockProColors.TextPrimary,
                                        ),
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Role Badge
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = StockProColors.PrimaryContainer,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = StockProColors.Primary,
                                    modifier = Modifier.size(11.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = roleTitle.ifBlank { "Inventory Manager" },
                                    style = StockProCodeTextStyle.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StockProColors.Primary,
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
                                tint = StockProColors.Danger,
                                modifier = Modifier.size(13.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            val displayPlant = if (plantName.contains("North Wing")) plantName else "$plantName · North Wing"
                            Text(
                                text = displayPlant.ifBlank { "Pune Plant 1 · North Wing" },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = StockProColors.TextSecondary,
                                ),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Detail Credentials & Last Login
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = StockProColors.SurfaceAlt,
                    border = BorderStroke(0.5.dp, StockProColors.OutlineSubtle),
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
                                tint = StockProColors.TextSecondary,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = email.ifBlank { "operator@stockpro.com" },
                                style = StockProCodeTextStyle.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = StockProColors.TextPrimary,
                                ),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = StockProColors.TextSecondary,
                                modifier = Modifier.size(13.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = phone.ifBlank { "+91 98765 43210" },
                                style = StockProCodeTextStyle.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = StockProColors.TextPrimary,
                                ),
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = StockProColors.TextSecondary,
                                modifier = Modifier.size(13.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Last Login: $formattedLastLogin · ${company.ifBlank { "StockPro Ltd" }}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 10.sp,
                                    color = StockProColors.TextSecondary,
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
    val watermarkColor = StockProColors.Outline.copy(alpha = 0.5f)
    Canvas(modifier = modifier.size(width = 110.dp, height = 55.dp)) {
        val color = watermarkColor
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
                tint = StockProColors.TextSecondary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = StockProCodeTextStyle.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = StockProColors.TextSecondary,
                ),
            )
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 10.sp,
                color = StockProColors.TextTertiary,
            ),
        )
    }
}

@Composable
private fun SettingsGroupCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = StockProColors.Surface,
        border = BorderStroke(1.dp, StockProColors.OutlineSubtle),
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
                color = StockProColors.TextPrimary,
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    color = StockProColors.TextSecondary,
                ),
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (trailingValue != null) {
                val pillBg = if (isTrailingPillBlue) StockProColors.PrimaryContainer else StockProColors.SurfaceAlt
                val pillText = if (isTrailingPillBlue) StockProColors.Primary else StockProColors.TextSecondary

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

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ProfileInfoDialog(
    profile: UserProfile,
    onEditClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = StockProStrings.current
    val isDark = StockProColors.isDark
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val formattedLastLogin = remember(profile.lastLoginMillis) {
        if (profile.lastLoginMillis > 0L) dateFormat.format(Date(profile.lastLoginMillis)) else "01 Apr 2024, 01:30 PM"
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = StockProColors.Surface,
            shadowElevation = 10.dp,
            border = BorderStroke(1.dp, StockProColors.Outline),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                // 1. Header Hero with Dual-ring Avatar, Status Badges, and Circular Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        // Dual-ring Avatar
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.5f) else Color(0xFFE8F1FC)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(StockProColors.Primary),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = profile.initials,
                                    style = TextStyle(
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                    ),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = profile.displayName.ifBlank { "Operator" },
                                style = TextStyle(
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StockProColors.TextPrimary,
                                ),
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = profile.role.displayTitle,
                                style = TextStyle(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDark) Color(0xFF93C5FD) else Color(0xFF2563EB),
                                ),
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            // Status Badges
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.5f) else Color(0xFFE8F1FC),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = if (isDark) Color(0xFF93C5FD) else Color(0xFF2563EB),
                                            modifier = Modifier.size(12.dp),
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Active User",
                                            style = TextStyle(
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) Color(0xFF93C5FD) else Color(0xFF2563EB),
                                            ),
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isDark) Color(0xFF064E3B).copy(alpha = 0.5f) else Color(0xFFE8F8F0),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Shield,
                                            contentDescription = null,
                                            tint = if (isDark) Color(0xFF6EE7B7) else Color(0xFF10B981),
                                            modifier = Modifier.size(12.dp),
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = profile.role.displayTitle,
                                            style = TextStyle(
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF10B981),
                                            ),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Circular Close Button
                    Surface(
                        shape = CircleShape,
                        color = if (isDark) StockProColors.SurfaceAlt else Color(0xFFF1F5F9),
                        modifier = Modifier
                            .size(32.dp)
                            .clickable { onDismiss() },
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = StockProColors.TextSecondary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 2. USER DETAILS Section
                ProfileDetailSection(
                    title = "USER DETAILS",
                    icon = Icons.Default.Person,
                    badgeBackground = if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.5f) else Color(0xFFE8F1FC),
                    iconTint = if (isDark) Color(0xFF93C5FD) else Color(0xFF2563EB),
                    headerAction = {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.5f) else Color(0xFFE8F1FC),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF3B82F6).copy(alpha = 0.5f) else Color(0xFFBFDBFE)),
                            modifier = Modifier.clickable {
                                onDismiss()
                                onEditClick()
                            },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF93C5FD) else Color(0xFF2563EB),
                                    modifier = Modifier.size(12.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Edit",
                                    style = TextStyle(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFF93C5FD) else Color(0xFF2563EB),
                                    ),
                                )
                            }
                        }
                    },
                ) {
                    ProfileDetailRow(icon = Icons.Default.Person, label = "Full Name", value = profile.displayName)
                    ProfileDetailRow(icon = Icons.Default.MailOutline, label = "Email Address", value = profile.email)
                    ProfileDetailRow(icon = Icons.Default.Phone, label = "Phone Number", value = profile.phone)
                    ProfileDetailRow(icon = Icons.Default.Fingerprint, label = "Operator ID", value = profile.uid.ifBlank { "PhZidPIWYSasYqLS4Wcv03eUGTd2" }, isCode = true)
                    ProfileDetailRow(icon = Icons.Default.Shield, label = "Assigned Role", value = profile.role.displayTitle)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. COMPANY DETAILS Section
                ProfileDetailSection(
                    title = "COMPANY DETAILS",
                    icon = Icons.Default.Apartment,
                    badgeBackground = if (isDark) Color(0xFF064E3B).copy(alpha = 0.5f) else Color(0xFFE8F8F0),
                    iconTint = if (isDark) Color(0xFF6EE7B7) else Color(0xFF10B981),
                ) {
                    ProfileDetailRow(icon = Icons.Default.Apartment, label = "Company Name", value = profile.company)
                    ProfileDetailRow(icon = Icons.Default.Description, label = "Organization ID", value = profile.orgId.ifBlank { "org_demo" }, isCode = true)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 4. ACTIVE PLANT & FACILITY Section
                ProfileDetailSection(
                    title = "ACTIVE PLANT & FACILITY",
                    icon = Icons.Default.LocationOn,
                    badgeBackground = if (isDark) Color(0xFF581C87).copy(alpha = 0.5f) else Color(0xFFF3E8FF),
                    iconTint = if (isDark) Color(0xFFD8B4FE) else Color(0xFF9333EA),
                ) {
                    ProfileDetailRow(icon = Icons.Default.Apartment, label = "Plant Name", value = profile.plantName.ifBlank { "Pune Plant 1" })
                    ProfileDetailRow(icon = Icons.Default.QrCode, label = "Plant Code", value = profile.plantId.ifBlank { "plant_01" }, isCode = true)
                    ProfileDetailRow(icon = Icons.Default.Store, label = "Default Warehouse", value = profile.warehouseName.ifBlank { "Main Warehouse" })
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 5. SESSION & SECURITY Section
                ProfileDetailSection(
                    title = "SESSION & SECURITY",
                    icon = Icons.Default.Shield,
                    badgeBackground = if (isDark) Color(0xFF78350F).copy(alpha = 0.5f) else Color(0xFFFEF3C7),
                    iconTint = if (isDark) Color(0xFFFDE68A) else Color(0xFFD97706),
                ) {
                    ProfileDetailRow(icon = Icons.Default.AccessTime, label = "Last Login", value = formattedLastLogin)
                    ProfileDetailRow(
                        icon = Icons.Default.Lock,
                        label = "Encryption Status",
                        value = "TLS 1.3 Active",
                        trailingBadge = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isDark) Color(0xFF34D399) else Color(0xFF10B981)),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "TLS 1.3 Active",
                                    style = TextStyle(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StockProColors.TextPrimary,
                                    ),
                                )
                            }
                        },
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 6. Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, StockProColors.Primary),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = StockProColors.Primary,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                    ) {
                        Text(
                            text = strings.close,
                            style = TextStyle(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = StockProColors.Primary,
                            ),
                        )
                    }

                    Button(
                        onClick = {
                            onDismiss()
                            onEditClick()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StockProColors.Primary,
                            contentColor = Color.White,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Edit Profile",
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
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
private fun ProfileDetailSection(
    title: String,
    icon: ImageVector,
    badgeBackground: Color = Color(0xFFE8F1FC),
    iconTint: Color = Color(0xFF2563EB),
    headerAction: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val isDark = StockProColors.isDark
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isDark) StockProColors.SurfaceAlt else Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, StockProColors.Outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(badgeBackground),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = title,
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = StockProColors.TextPrimary,
                            letterSpacing = 0.5.sp,
                        ),
                    )
                }

                headerAction?.invoke()
            }
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun ProfileDetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    isCode: Boolean = false,
    trailingBadge: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = StockProColors.TextSecondary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                style = TextStyle(
                    fontSize = 12.sp,
                    color = StockProColors.TextSecondary,
                ),
            )
        }

        if (trailingBadge != null) {
            trailingBadge()
        } else {
            Text(
                text = value.ifBlank { "—" },
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = StockProColors.TextPrimary,
                    fontFamily = if (isCode) FontFamily.Monospace else null,
                ),
                maxLines = 1,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileDialog(
    profile: UserProfile,
    availablePlants: List<String>,
    onSave: (name: String, email: String, phone: String, company: String, plant: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = StockProStrings.current
    val isDark = StockProColors.isDark
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var name by remember { mutableStateOf(profile.displayName) }
    val email = profile.email
    var phone by remember { mutableStateOf(profile.phone) }
    var company by remember { mutableStateOf(profile.company) }
    var selectedPlant by remember { mutableStateOf(profile.plantName) }
    var plantDropdownExpanded by remember { mutableStateOf(false) }

    val isNameValid = name.trim().length >= 2
    val isPhoneValid = phone.trim().length >= 8
    val isCompanyValid = company.trim().isNotBlank()
    val isPlantValid = selectedPlant.trim().isNotBlank()
    val isFormValid = isNameValid && isPhoneValid && isCompanyValid && isPlantValid

    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val isImeVisible = WindowInsets.isImeVisible
    val bottomInset = if (isImeVisible) 16.dp else maxOf(navBarBottom, 24.dp) + 16.dp
    val topInset = maxOf(statusBarTop, 16.dp) + 8.dp

    BasicAlertDialog(
        onDismissRequest = {
            focusManager.clearFocus()
            keyboardController?.hide()
            onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = topInset, end = 16.dp, bottom = bottomInset)
            .imePadding(),
    ) {
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            @Suppress("DEPRECATION")
            dialogWindow?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = StockProColors.Surface,
            shadowElevation = 10.dp,
            border = BorderStroke(1.dp, StockProColors.Outline),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                // 1. Header with Badge, Title/Subtitle, and Circular Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.5f) else Color(0xFFE8F1FC)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF93C5FD) else Color(0xFF2563EB),
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = strings.editProfileTitle,
                                style = TextStyle(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StockProColors.TextPrimary,
                                    letterSpacing = (-0.2).sp,
                                ),
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = strings.editProfileSubtitle,
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    color = StockProColors.TextSecondary,
                                ),
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = if (isDark) StockProColors.SurfaceAlt else Color(0xFFF1F5F9),
                        modifier = Modifier
                            .size(32.dp)
                            .clickable {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                onDismiss()
                            },
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = StockProColors.TextSecondary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 2. Full Name
                ProfileFieldLabel(label = strings.fullNameLabel)
                Spacer(modifier = Modifier.height(6.dp))
                ProfileTextInput(
                    value = name,
                    onValueChange = { name = it },
                    icon = Icons.Default.Person,
                    placeholder = "Aditya",
                    imeAction = ImeAction.Next,
                    errorMessage = if (!isNameValid && name.isNotBlank()) "Minimum 2 characters required" else null,
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Email Address (Registered Account ID - Non-editable)
                ProfileFieldLabel(label = strings.emailAddressLabel)
                Spacer(modifier = Modifier.height(6.dp))
                ProfileTextInput(
                    value = email,
                    onValueChange = {},
                    icon = Icons.Default.MailOutline,
                    placeholder = "operator@company.com",
                    keyboardType = KeyboardType.Email,
                    enabled = false,
                    readOnly = true,
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Non-editable",
                            tint = StockProColors.TextTertiary,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    supportingText = "Registered operator email cannot be changed",
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 4. Phone Number
                ProfileFieldLabel(label = strings.phoneNumberLabel)
                Spacer(modifier = Modifier.height(6.dp))
                ProfileTextInput(
                    value = phone,
                    onValueChange = { phone = it },
                    icon = Icons.Default.Phone,
                    placeholder = "+91 98765 43210",
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next,
                    errorMessage = if (!isPhoneValid && phone.isNotBlank()) "Minimum 8 digits required" else null,
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 5. Company Name
                ProfileFieldLabel(label = strings.companyNameLabel)
                Spacer(modifier = Modifier.height(6.dp))
                ProfileTextInput(
                    value = company,
                    onValueChange = { company = it },
                    icon = Icons.Default.Apartment,
                    placeholder = "StockPro Manufacturing Ltd",
                    imeAction = ImeAction.Done,
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        }
                    ),
                    errorMessage = if (!isCompanyValid && company.isNotBlank()) "Company name cannot be blank" else null,
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 6. Assigned Plant Dropdown Selector
                ProfileFieldLabel(label = strings.assignedPlantLabel)
                Spacer(modifier = Modifier.height(6.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDark) StockProColors.SurfaceAlt else Color.White,
                        border = BorderStroke(1.dp, StockProColors.Outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clickable { plantDropdownExpanded = true },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isDark) StockProColors.Surface else Color(0xFFF1F5F9)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Apartment,
                                        contentDescription = null,
                                        tint = StockProColors.TextSecondary,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = selectedPlant.ifBlank { "Select Plant" },
                                    style = TextStyle(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (selectedPlant.isNotBlank()) StockProColors.TextPrimary else StockProColors.TextTertiary,
                                    ),
                                    maxLines = 1,
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = StockProColors.TextSecondary,
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

                Spacer(modifier = Modifier.height(22.dp))

                // 7. Action Buttons (Cancel & Save Changes)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, StockProColors.Primary),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = StockProColors.Primary,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                    ) {
                        Text(
                            text = strings.cancel,
                            style = TextStyle(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = StockProColors.Primary,
                            ),
                        )
                    }

                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            onSave(name.trim(), email.trim(), phone.trim(), company.trim(), selectedPlant.trim())
                        },
                        enabled = isFormValid,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StockProColors.Primary,
                            contentColor = Color.White,
                            disabledContainerColor = if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.5f) else Color(0xFF93C5FD),
                            disabledContentColor = Color.White.copy(alpha = 0.7f),
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.saveChangesButton,
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                ),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun ProfileFieldLabel(label: String) {
    Text(
        text = label,
        style = TextStyle(
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = StockProColors.TextPrimary,
        ),
    )
}

@Composable
private fun ProfileTextInput(
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null,
    supportingText: String? = null,
    errorMessage: String? = null,
    modifier: Modifier = Modifier,
) {
    val isDark = StockProColors.isDark
    val fieldBg = if (isDark) StockProColors.SurfaceAlt else Color.White
    val inputColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = fieldBg,
        unfocusedContainerColor = fieldBg,
        disabledContainerColor = if (isDark) StockProColors.SurfaceAlt.copy(alpha = 0.6f) else Color(0xFFF1F5F9),
        disabledBorderColor = StockProColors.Outline.copy(alpha = 0.5f),
        focusedBorderColor = StockProColors.Primary,
        unfocusedBorderColor = StockProColors.Outline,
        cursorColor = StockProColors.Primary,
        focusedTextColor = StockProColors.TextPrimary,
        unfocusedTextColor = StockProColors.TextPrimary,
        disabledTextColor = StockProColors.TextSecondary,
        disabledLeadingIconColor = StockProColors.TextTertiary,
        disabledTrailingIconColor = StockProColors.TextTertiary,
    )

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = StockProColors.TextTertiary, fontSize = 13.sp) },
            leadingIcon = {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDark) StockProColors.Surface else Color(0xFFF1F5F9)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (enabled) StockProColors.TextSecondary else StockProColors.TextTertiary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            },
            trailingIcon = trailingIcon,
            enabled = enabled,
            readOnly = readOnly,
            shape = RoundedCornerShape(10.dp),
            colors = inputColors,
            singleLine = true,
            isError = !errorMessage.isNullOrBlank(),
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction,
            ),
            keyboardActions = keyboardActions,
            modifier = Modifier.fillMaxWidth(),
        )
        val infoText = errorMessage ?: supportingText
        if (!infoText.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = infoText,
                color = if (!errorMessage.isNullOrBlank()) StockProColors.Danger else StockProColors.TextTertiary,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            )
        }
    }
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
                        color = if (isSelected) StockProColors.PrimaryContainer else StockProColors.SurfaceAlt,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) StockProColors.Primary else StockProColors.Outline,
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
                                tint = if (isSelected) StockProColors.Primary else StockProColors.TextTertiary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = format.label,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) StockProColors.Primary else StockProColors.TextPrimary,
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
                                        color = StockProColors.TextSecondary,
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

@Composable
private fun RemovePlantSelectionDialog(
    plants: List<Plant>,
    activePlantName: String,
    onSelectPlantToDelete: (Plant) -> Unit,
    onDismiss: () -> Unit,
) {
    val canDelete = plants.size > 1
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = StockProColors.Danger,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Plant Remove",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = StockProColors.TextPrimary,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!canDelete) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = StockProColors.DangerContainer,
                        border = BorderStroke(1.dp, StockProColors.Outline),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "Cannot remove plant: StockPro requires at least one active plant facility.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = StockProColors.OnDangerContainer,
                                fontWeight = FontWeight.Medium,
                            ),
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                } else {
                    Text(
                        text = "Select a plant facility to decommission and remove from this device:",
                        style = MaterialTheme.typography.bodySmall,
                        color = StockProColors.TextSecondary,
                    )
                }

                plants.forEach { plant ->
                    val isActive = plant.name.equals(activePlantName, ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = StockProColors.SurfaceAlt,
                        border = BorderStroke(1.dp, if (isActive) StockProColors.Primary else StockProColors.Outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(enabled = canDelete) { onSelectPlantToDelete(plant) },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = plant.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = StockProColors.TextPrimary,
                                    )
                                    if (isActive) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = StockProColors.PrimaryContainer,
                                        ) {
                                            Text(
                                                text = "ACTIVE",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 9.sp,
                                                    color = StockProColors.Primary,
                                                ),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${plant.code} • ${plant.location.ifBlank { "Default Location" }}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = StockProColors.TextSecondary,
                                )
                            }
                            if (canDelete) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove ${plant.name}",
                                    tint = StockProColors.Danger,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", fontWeight = FontWeight.SemiBold)
            }
        },
    )
}

@Composable
private fun RemovePlantConfirmDialog(
    plant: Plant,
    itemCount: Int,
    isActivePlant: Boolean,
    onConfirmDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = StockProColors.Danger,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Decommission Plant?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = StockProColors.Danger,
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Are you sure you want to remove plant '${plant.name}' (${plant.code})?",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = StockProColors.TextPrimary,
                )
                if (itemCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = StockProColors.WarningContainer,
                        border = BorderStroke(1.dp, StockProColors.Outline),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "⚠️ Warning: This plant has $itemCount inventory item(s) associated with it in local database.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = StockProColors.OnWarningContainer,
                                fontWeight = FontWeight.Medium,
                            ),
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                }
                if (isActivePlant) {
                    Text(
                        text = "Because this is your currently active plant, your session will automatically switch to another available plant facility.",
                        style = MaterialTheme.typography.bodySmall,
                        color = StockProColors.TextSecondary,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirmDelete) {
                Text(
                    text = "Remove Plant",
                    color = StockProColors.Danger,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
