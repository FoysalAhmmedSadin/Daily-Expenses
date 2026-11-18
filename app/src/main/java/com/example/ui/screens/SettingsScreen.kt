package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarViewMonth
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.EnhancedEncryption
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.common.CategoryIcons
import com.example.core.common.DateTimeUtils
import com.example.core.common.LocalAppStrings
import com.example.core.model.Category
import com.example.core.model.IsoCurrencyCatalog
import com.example.core.model.SupportedLanguageCatalog
import com.example.core.model.TransactionType
import com.example.core.security.BiometricAuthManager
import com.example.core.worker.NotificationChannels
import com.example.ui.components.AppSwitch
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.MonthlyCsvExportDialog
import com.example.ui.components.PassphraseDialog
import com.example.ui.components.PremiumCard
import com.example.ui.theme.ExpenseRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onOpenViewMode: () -> Unit = {},
    onOpenAppearance: () -> Unit = {},
    onOpenLanguage: () -> Unit = {},
    onOpenCurrency: () -> Unit = {},
    onOpenCategories: () -> Unit,
    onOpenAboutDeveloper: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val prefs = state.preferences
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val langTag = prefs.effectiveLanguageTag
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    var pendingBackupPassphrase by remember { mutableStateOf<String?>(null) }
    var pendingRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var showBackupPassphraseDialog by remember { mutableStateOf(false) }
    var showRestorePassphraseDialog by remember { mutableStateOf(false) }
    var showClearDataConfirm by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showReminderTimePicker by remember { mutableStateOf(false) }
    var showCsvExportDialog by remember { mutableStateOf(false) }
    var pendingCsvMonthYear by remember { mutableStateOf<String?>(null) }
    var showDriveBackupOptionsDialog by remember { mutableStateOf(false) }
    var localNotice by remember { mutableStateOf<String?>(null) }

    val driveResolutionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = { result ->
            viewModel.onDriveResolutionResult(
                activityContext = context,
                resultOk = result.resultCode == Activity.RESULT_OK,
                data = result.data
            )
        }
    )

    LaunchedEffect(state.pendingDriveResolution) {
        state.pendingDriveResolution?.let { sender ->
            driveResolutionLauncher.launch(IntentSenderRequest.Builder(sender).build())
        }
    }

    LaunchedEffect(localNotice) {
        localNotice?.let {
            snackbarHostState.showSnackbar(it)
            localNotice = null
        }
    }

    val notificationPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                viewModel.setDailyReminder(true)
            } else {
                localNotice = strings.notificationPermissionRequired
            }
        }
    )

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
        onResult = { uri ->
            val month = pendingCsvMonthYear
            pendingCsvMonthYear = null
            uri?.let { viewModel.exportCsv(it, month) }
        }
    )

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
        onResult = { uri -> uri?.let { viewModel.exportPdf(it) } }
    )

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
        onResult = { uri ->
            val pass = pendingBackupPassphrase
            pendingBackupPassphrase = null
            if (uri != null && pass != null) {
                viewModel.createEncryptedBackup(uri, pass)
            }
        }
    )

    val openBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                pendingRestoreUri = uri
                showRestorePassphraseDialog = true
            }
        }
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = strings.settings,
                        style = MaterialTheme.typography.headlineMedium
                    )
                }

                // Compact Preferences Navigation Section
                item {
                    val activeLanguage = SupportedLanguageCatalog.findByTag(langTag)
                    val activeCurrency = IsoCurrencyCatalog.resolve(prefs.effectiveCurrencyCode)
                    PremiumCard(modifier = Modifier.testTag("preferences_section")) {
                        Text(
                            text = strings.preferences,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        PreferenceNavigationRow(
                            icon = Icons.Rounded.CalendarViewMonth,
                            tint = Color(0xFF0A84FF),
                            title = strings.viewMode,
                            subtitle = strings.viewModeLabel(prefs.viewMode),
                            onClick = onOpenViewMode,
                            testTag = "prefs_row_view_mode"
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                        PreferenceNavigationRow(
                            icon = Icons.Rounded.Palette,
                            tint = MaterialTheme.colorScheme.primary,
                            title = strings.theme,
                            subtitle = strings.themeModeLabel(prefs.themeMode),
                            onClick = onOpenAppearance,
                            testTag = "prefs_row_appearance"
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                        PreferenceNavigationRow(
                            icon = Icons.Rounded.Language,
                            tint = Color(0xFF5856D6),
                            title = strings.language,
                            subtitle = activeLanguage.nativeName,
                            onClick = onOpenLanguage,
                            testTag = "prefs_row_language"
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                        PreferenceNavigationRow(
                            icon = Icons.Rounded.Payments,
                            tint = Color(0xFFFF9F0A),
                            title = strings.currency,
                            subtitle = "${activeCurrency.code} (${activeCurrency.symbol})",
                            onClick = onOpenCurrency,
                            testTag = "prefs_row_currency"
                        )
                    }
                }

                // Categories Management
                item {
                    PremiumCard(onClick = onOpenCategories, modifier = Modifier.testTag("manage_categories_row")) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CategoryIconBadge(
                                icon = Icons.Rounded.Category,
                                tintColor = MaterialTheme.colorScheme.primary,
                                size = 40.dp,
                                contentDescription = strings.categories
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(strings.manageCategories, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = strings.categoriesCount(state.categories.size, prefs.numberFormat),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Security & Notifications
                item {
                    PremiumCard {
                        Text(strings.securityPrivacy, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        SettingsSwitchRow(
                            title = strings.biometricLock,
                            checked = prefs.biometricLockEnabled,
                            onCheckedChange = { enable ->
                                if (!enable) {
                                    viewModel.setBiometricLock(false)
                                } else {
                                    if (!BiometricAuthManager.isAvailable(context)) {
                                        localNotice = strings.biometricNotConfigured
                                    } else {
                                        val act = context as? FragmentActivity
                                        if (act != null) {
                                            BiometricAuthManager.authenticate(
                                                activity = act,
                                                title = strings.biometricLock,
                                                subtitle = strings.unlockSubtitle,
                                                onSuccess = { viewModel.setBiometricLock(true) },
                                                onError = { msg -> localNotice = msg }
                                            )
                                        }
                                    }
                                }
                            },
                            testTag = "biometric_lock_switch"
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(strings.notificationsReminders, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(6.dp))
                        val formattedReminder = DateTimeUtils.formatReminderTime(
                            hour = prefs.reminderHour,
                            minute = prefs.reminderMinute,
                            context = context,
                            languageTag = langTag,
                            numberFormat = prefs.numberFormat
                        )
                        SettingsSwitchRow(
                            title = strings.dailyReminder,
                            subtitle = "$formattedReminder • ${strings.tapToChangeTime}",
                            checked = prefs.dailyReminderEnabled,
                            onCheckedChange = { enable ->
                                if (!enable) {
                                    viewModel.setDailyReminder(false)
                                } else if (
                                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    !NotificationChannels.hasNotificationPermission(context)
                                ) {
                                    notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.setDailyReminder(true)
                                }
                            },
                            onRowClick = { showReminderTimePicker = true },
                            rowTestTag = "daily_reminder_row",
                            testTag = "daily_reminder_switch"
                        )
                        SettingsSwitchRow(
                            title = strings.budgetAlerts,
                            checked = prefs.budgetAlertsEnabled,
                            onCheckedChange = viewModel::setBudgetAlerts,
                            testTag = "budget_alerts_switch"
                        )
                    }
                }

                // Export & Encrypted Backup
                item {
                    PremiumCard {
                        Text(strings.dataBackupExport, style = MaterialTheme.typography.titleMedium)
                        prefs.lastLocalBackupAt?.let { lastAt ->
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("last_backup_status_card")
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                    Text(
                                        text = DateTimeUtils.formatDateTime(lastAt, langTag, prefs.numberFormat),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (prefs.lastLocalBackupStatus.isNotBlank()) {
                                        Text(
                                            text = prefs.lastLocalBackupStatus,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        SettingsActionRow(
                            icon = Icons.Rounded.Download,
                            title = strings.exportCsv,
                            tint = MaterialTheme.colorScheme.primary,
                            onClick = { showCsvExportDialog = true },
                            testTag = "export_csv_button"
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                        SettingsActionRow(
                            icon = Icons.Rounded.PictureAsPdf,
                            title = strings.exportPdf,
                            tint = Color(0xFF0A84FF),
                            onClick = { pdfLauncher.launch("daily_expenses_statement.pdf") },
                            testTag = "export_pdf_button"
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                        SettingsActionRow(
                            icon = Icons.Rounded.EnhancedEncryption,
                            title = strings.encryptedBackup,
                            tint = Color(0xFF5856D6),
                            onClick = { showBackupPassphraseDialog = true },
                            testTag = "create_backup_button"
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                        SettingsActionRow(
                            icon = Icons.Rounded.Restore,
                            title = strings.restoreBackup,
                            tint = Color(0xFFFF9F0A),
                            onClick = { openBackupLauncher.launch(arrayOf("*/*")) },
                            testTag = "restore_backup_button"
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                        SettingsActionRow(
                            icon = Icons.Rounded.DeleteForever,
                            title = strings.clearAllData,
                            tint = ExpenseRed,
                            onClick = { showClearDataConfirm = true },
                            testTag = "clear_all_data_button"
                        )
                    }
                }

                // Google Drive Cloud Backup
                item {
                    GoogleDriveBackupCard(
                        prefs = prefs,
                        isBusy = state.isDriveBusy,
                        onConnect = { viewModel.connectGoogleDrive(context, isSwitchAccount = false) },
                        onSwitchAccount = { viewModel.connectGoogleDrive(context, isSwitchAccount = true) },
                        onDisconnect = { viewModel.disconnectGoogleDrive(context) },
                        onBackUpNowClick = { showDriveBackupOptionsDialog = true },
                        onOpenHistoryClick = { viewModel.openDriveHistory(context) },
                        onScheduleChange = viewModel::setDriveBackupSchedule
                    )
                }

                // About Developer
                item {
                    PremiumCard(
                        onClick = onOpenAboutDeveloper,
                        modifier = Modifier.testTag("about_developer_row")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CategoryIconBadge(
                                icon = Icons.Rounded.Code,
                                tintColor = MaterialTheme.colorScheme.primary,
                                size = 38.dp,
                                contentDescription = strings.aboutDeveloper
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(strings.aboutDeveloper, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "${DeveloperSupport.DEVELOPER_NAME} • ${DeveloperSupport.DEVELOPER_ROLE}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // About & Open Source License
                item {
                    PremiumCard(onClick = { showAboutDialog = true }, modifier = Modifier.testTag("about_license_row")) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CategoryIconBadge(
                                icon = Icons.Rounded.Info,
                                tintColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                size = 38.dp,
                                contentDescription = strings.aboutLicense
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(strings.aboutLicense, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = strings.aboutLicenseSummary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCsvExportDialog) {
        MonthlyCsvExportDialog(
            allTransactions = state.allTransactions,
            preferences = prefs,
            allowAllMonthsOption = true,
            onDismiss = { showCsvExportDialog = false },
            onExport = { monthYear ->
                showCsvExportDialog = false
                pendingCsvMonthYear = monthYear
                val fileName = if (monthYear != null) {
                    "daily_expenses_$monthYear.csv"
                } else {
                    "daily_expenses_transactions.csv"
                }
                csvLauncher.launch(fileName)
            }
        )
    }

    if (showBackupPassphraseDialog) {
        PassphraseDialog(
            title = strings.passphraseTitle,
            hint = strings.passphraseHint,
            confirmText = strings.confirmAction,
            cancelText = strings.cancel,
            onDismiss = { showBackupPassphraseDialog = false },
            onConfirm = { pass ->
                showBackupPassphraseDialog = false
                pendingBackupPassphrase = pass
                createBackupLauncher.launch("daily_expenses_backup.embackup")
            }
        )
    }

    if (showRestorePassphraseDialog) {
        PassphraseDialog(
            title = strings.passphraseTitle,
            hint = strings.passphraseHint,
            confirmText = strings.confirmAction,
            cancelText = strings.cancel,
            onDismiss = {
                showRestorePassphraseDialog = false
                pendingRestoreUri = null
            },
            onConfirm = { pass ->
                val uri = pendingRestoreUri
                showRestorePassphraseDialog = false
                pendingRestoreUri = null
                if (uri != null) {
                    viewModel.readEncryptedBackup(uri, pass)
                }
            }
        )
    }

    state.pendingRestorePayload?.let { payload ->
        AlertDialog(
            onDismissRequest = viewModel::cancelPendingRestore,
            title = { Text(strings.restoreBackup) },
            text = {
                val backupDate = DateTimeUtils.formatDateTime(
                    millis = payload.exportedAt,
                    languageTag = langTag,
                    numberFormat = prefs.numberFormat
                )
                Text(
                    strings.restoreBackupPreview(
                        date = backupDate,
                        version = payload.schemaVersion,
                        accounts = payload.accounts.size,
                        categories = payload.categories.size,
                        transactions = payload.transactions.size,
                        budgets = payload.budgets.size,
                        numberFormat = prefs.numberFormat
                    )
                )
            },
            confirmButton = {
                Button(onClick = viewModel::confirmRestoreBackup) {
                    Text(strings.confirmAction)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelPendingRestore) {
                    Text(strings.cancel)
                }
            }
        )
    }

    if (showClearDataConfirm) {
        AlertDialog(
            onDismissRequest = { showClearDataConfirm = false },
            title = { Text(strings.clearAllData) },
            text = { Text(strings.clearAllDataConfirm) },
            confirmButton = {
                Button(
                    onClick = {
                        showClearDataConfirm = false
                        viewModel.clearAllData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text(strings.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataConfirm = false }) { Text(strings.cancel) }
            }
        )
    }

    if (showReminderTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = prefs.reminderHour,
            initialMinute = prefs.reminderMinute,
            is24Hour = DateFormat.is24HourFormat(context)
        )
        AlertDialog(
            onDismissRequest = { showReminderTimePicker = false },
            title = { Text(strings.reminderTime) },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setDailyReminder(true, timeState.hour, timeState.minute)
                        showReminderTimePicker = false
                    },
                    modifier = Modifier.testTag("reminder_time_apply_button")
                ) { Text(strings.apply) }
            },
            dismissButton = {
                TextButton(onClick = { showReminderTimePicker = false }) { Text(strings.cancel) }
            }
        )
    }

    if (showDriveBackupOptionsDialog) {
        DriveUploadOptionsDialog(
            onDismiss = { showDriveBackupOptionsDialog = false },
            onConfirmBackup = { passphrase ->
                showDriveBackupOptionsDialog = false
                viewModel.backupToGoogleDrive(context, passphrase)
            }
        )
    }

    if (state.driveHistoryVisible) {
        DriveBackupHistoryDialog(
            backups = state.driveBackups,
            isBusy = state.isDriveBusy,
            prefs = prefs,
            onDismiss = viewModel::closeDriveHistory,
            onRestore = { entry -> viewModel.restoreFromDriveEntry(context, entry) },
            onDelete = { entry -> viewModel.deleteDriveBackupEntry(context, entry) }
        )
    }

    if (state.showDriveDecryptPassphraseDialog) {
        DriveDecryptPassphraseDialog(
            onDismiss = viewModel::cancelDriveDecryptDialog,
            onConfirm = viewModel::decryptPendingDriveBackup
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text(strings.aboutLicense) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${strings.appName} — ${strings.appSubtitle}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = strings.aboutLicenseDetail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text(strings.ok) }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val strings = LocalAppStrings.current
    val prefs = state.preferences
    var showAddDialog by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.categories) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = strings.cancel)
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }, modifier = Modifier.testTag("add_category_button")) {
                        Icon(Icons.Rounded.Add, contentDescription = strings.addCategory)
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.categories, key = { it.id }) { cat ->
                    val tint = Color(cat.colorHex)
                    val catName = cat.localizedName(prefs.effectiveLanguageTag, strings)
                    PremiumCard(contentPadding = 12.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CategoryIconBadge(
                                icon = CategoryIcons.resolve(cat.iconKey),
                                tintColor = tint,
                                size = 40.dp,
                                contentDescription = catName
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(catName, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = strings.transactionTypeLabel(cat.type),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (!cat.isDefault) {
                                IconButton(onClick = { viewModel.deleteCategory(cat.id) }) {
                                    Icon(
                                        Icons.Rounded.DeleteOutline,
                                        contentDescription = strings.delete,
                                        tint = ExpenseRed
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var nameEn by remember { mutableStateOf("") }
        var nameBn by remember { mutableStateOf("") }
        var type by remember { mutableStateOf(TransactionType.EXPENSE) }
        var iconKey by remember { mutableStateOf(CategoryIcons.allKeys.first()) }
        var colorHex by remember { mutableLongStateOf(CategoryIcons.paletteColors.first()) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(strings.addCategory) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(TransactionType.EXPENSE, TransactionType.INCOME).forEach { t ->
                            FilterChip(
                                selected = type == t,
                                onClick = { type = t },
                                label = { Text(strings.transactionTypeLabel(t)) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = nameEn,
                        onValueChange = { nameEn = it },
                        label = { Text(strings.categoryNamePrimary) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = nameBn,
                        onValueChange = { nameBn = it },
                        label = { Text(strings.categoryNameSecondary) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CategoryIcons.allKeys.forEach { key ->
                            val selected = iconKey == key
                            Surface(
                                onClick = { iconKey = key },
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) Color(colorHex).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (selected) BorderStroke(1.5.dp, Color(colorHex)) else null
                            ) {
                                Icon(
                                    imageVector = CategoryIcons.resolve(key),
                                    contentDescription = key,
                                    tint = Color(colorHex),
                                    modifier = Modifier.padding(8.dp).size(20.dp)
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CategoryIcons.paletteColors.forEach { hex ->
                            Surface(
                                shape = CircleShape,
                                color = Color(hex),
                                border = if (colorHex == hex) BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface) else null,
                                modifier = Modifier
                                    .size(26.dp)
                                    .clickable { colorHex = hex }
                            ) {}
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveCategory(
                            Category(
                                nameEn = nameEn.trim(),
                                nameBn = nameBn.trim().ifBlank { nameEn.trim() },
                                type = type,
                                iconKey = iconKey,
                                colorHex = colorHex,
                                isDefault = false
                            )
                        )
                        showAddDialog = false
                    },
                    enabled = nameEn.isNotBlank()
                ) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text(strings.cancel) }
            }
        )
    }
}

@Composable
fun AppLockScreen(
    onUnlock: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val triggerPrompt: () -> Unit = {
        val activity = context as? FragmentActivity
        if (activity != null && BiometricAuthManager.isAvailable(context)) {
            BiometricAuthManager.authenticate(
                activity = activity,
                title = strings.unlockTitle,
                subtitle = strings.unlockSubtitle,
                onSuccess = onUnlock,
                onError = { err -> errorMsg = err }
            )
        } else {
            onUnlock()
        }
    }

    LaunchedEffect(Unit) {
        triggerPrompt()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = strings.unlockTitle,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(38.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = strings.unlockTitle,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = strings.unlockSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (errorMsg != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = errorMsg!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = ExpenseRed,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = triggerPrompt,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("biometric_unlock_button")
            ) {
                Icon(Icons.Rounded.Fingerprint, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.unlockButton)
            }
        }
    }
}

@Composable
private fun PreferenceNavigationRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "$title, $subtitle" }
            .padding(vertical = 8.dp, horizontal = 2.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryIconBadge(
            icon = icon,
            tintColor = tint,
            size = 38.dp,
            contentDescription = null
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
    onRowClick: (() -> Unit)? = null,
    rowTestTag: String? = null,
    testTag: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(if (onRowClick != null) Modifier.clickable(onClick = onRowClick) else Modifier)
            .then(if (rowTestTag != null) Modifier.testTag(rowTestTag) else Modifier)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        AppSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = if (testTag != null) Modifier.testTag(testTag) else Modifier
        )
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    tint: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryIconBadge(
            icon = icon,
            tintColor = tint,
            size = 36.dp,
            contentDescription = title
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
