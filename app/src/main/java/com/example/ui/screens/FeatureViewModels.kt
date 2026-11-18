package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.common.DateTimeUtils
import com.example.core.domain.GetAnalyticsReportUseCase
import com.example.core.domain.GetBudgetsWithProgressUseCase
import com.example.core.drive.DriveAuthGateway
import com.example.core.drive.DriveAuthOutcome
import com.example.core.drive.EncryptedDriveBackupRequiresPassphraseException
import com.example.core.drive.GoogleDriveRemoteDataSource
import com.example.core.drive.NoGoogleCredentialAvailableException
import com.example.core.model.Account
import com.example.core.model.AnalyticsGranularity
import com.example.core.model.AnalyticsReport
import com.example.core.model.AppAccentColor
import com.example.core.model.AppCurrency
import com.example.core.model.AppLanguage
import com.example.core.model.BackupPayload
import com.example.core.model.Budget
import com.example.core.model.BudgetWithProgress
import com.example.core.model.Category
import com.example.core.model.DriveBackupEntry
import com.example.core.model.DriveBackupSchedule
import com.example.core.model.GoogleDriveAccount
import com.example.core.model.NumberFormatStyle
import com.example.core.model.ThemeMode
import com.example.core.model.TransactionType
import com.example.core.model.TransactionWithDetails
import com.example.core.model.UserPreferences
import com.example.core.model.ViewMode
import com.example.core.repository.AccountRepository
import com.example.core.repository.BackupAndExportRepository
import com.example.core.repository.BudgetRepository
import com.example.core.repository.CategoryRepository
import com.example.core.repository.GoogleDriveBackupRepository
import com.example.core.repository.PreferencesRepository
import com.example.core.repository.TransactionRepository
import com.example.core.worker.WorkScheduler
import com.example.widget.ExpenseSummaryWidget
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountsUiState(
    val accounts: List<Account> = emptyList(),
    val totalBalance: Double = 0.0,
    val transactions: List<TransactionWithDetails> = emptyList(),
    val preferences: UserPreferences = UserPreferences()
)

class AccountsViewModel(
    private val accountRepository: AccountRepository,
    transactionRepository: TransactionRepository,
    preferencesRepository: PreferencesRepository,
    private val appContext: Context
) : ViewModel() {

    val uiState: StateFlow<AccountsUiState> = combine(
        accountRepository.observeAccounts(),
        transactionRepository.observeTransactionsWithDetails(),
        preferencesRepository.preferences
    ) { accounts, transactions, prefs ->
        AccountsUiState(
            accounts = accounts,
            totalBalance = accounts.sumOf { it.balance },
            transactions = transactions,
            preferences = prefs
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AccountsUiState())

    fun saveAccount(account: Account) {
        viewModelScope.launch {
            accountRepository.saveAccount(account)
            runCatching { ExpenseSummaryWidget().updateAll(appContext) }
        }
    }

    fun deleteAccount(accountId: Long) {
        viewModelScope.launch {
            accountRepository.deleteAccount(accountId)
            runCatching { ExpenseSummaryWidget().updateAll(appContext) }
        }
    }
}

data class AnalyticsUiState(
    val granularity: AnalyticsGranularity = AnalyticsGranularity.MONTHLY,
    val report: AnalyticsReport = AnalyticsReport(),
    val preferences: UserPreferences = UserPreferences()
)

class AnalyticsViewModel(
    getAnalyticsReportUseCase: GetAnalyticsReportUseCase,
    preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _granularity = MutableStateFlow(AnalyticsGranularity.MONTHLY)

    val uiState: StateFlow<AnalyticsUiState> = combine(
        _granularity,
        getAnalyticsReportUseCase(_granularity),
        preferencesRepository.preferences
    ) { gran, report, prefs ->
        AnalyticsUiState(
            granularity = gran,
            report = report,
            preferences = prefs
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsUiState())

    fun setGranularity(granularity: AnalyticsGranularity) {
        _granularity.value = granularity
    }
}

data class BudgetsUiState(
    val monthYear: String = DateTimeUtils.currentMonthYear(),
    val budgets: List<BudgetWithProgress> = emptyList(),
    val expenseCategories: List<Category> = emptyList(),
    val preferences: UserPreferences = UserPreferences()
)

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetsViewModel(
    private val budgetRepository: BudgetRepository,
    private val getBudgetsWithProgress: GetBudgetsWithProgressUseCase,
    categoryRepository: CategoryRepository,
    preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _monthYear = MutableStateFlow(DateTimeUtils.currentMonthYear())

    val uiState: StateFlow<BudgetsUiState> = combine(
        _monthYear,
        _monthYear.flatMapLatest { month -> getBudgetsWithProgress(month) },
        categoryRepository.observeCategories(),
        preferencesRepository.preferences
    ) { month, budgets, categories, prefs ->
        BudgetsUiState(
            monthYear = month,
            budgets = budgets,
            expenseCategories = categories.filter { it.type == TransactionType.EXPENSE },
            preferences = prefs
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BudgetsUiState())

    fun shiftMonth(delta: Int) {
        _monthYear.value = DateTimeUtils.shiftMonthYear(_monthYear.value, delta)
    }

    fun saveBudget(budget: Budget) {
        viewModelScope.launch {
            budgetRepository.saveBudget(budget)
        }
    }

    fun deleteBudget(id: Long) {
        viewModelScope.launch {
            budgetRepository.deleteBudget(id)
        }
    }
}

private sealed interface PendingDriveAction {
    data object FetchAccountAndRefresh : PendingDriveAction
    data class RefreshAfterConnect(val account: GoogleDriveAccount) : PendingDriveAction
    data object OpenHistory : PendingDriveAction
    data class Backup(val passphrase: String?) : PendingDriveAction
    data class Restore(val entry: DriveBackupEntry) : PendingDriveAction
    data class Delete(val entry: DriveBackupEntry) : PendingDriveAction
}

private data class DriveUiSubState(
    val backups: List<DriveBackupEntry> = emptyList(),
    val isBusy: Boolean = false,
    val historyVisible: Boolean = false,
    val pendingResolution: IntentSender? = null,
    val showDecryptPassphraseDialog: Boolean = false
)

data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences(),
    val categories: List<Category> = emptyList(),
    val allTransactions: List<TransactionWithDetails> = emptyList(),
    val statusMessage: String? = null,
    val pendingRestorePayload: BackupPayload? = null,
    val isBusy: Boolean = false,
    val driveBackups: List<DriveBackupEntry> = emptyList(),
    val isDriveBusy: Boolean = false,
    val driveHistoryVisible: Boolean = false,
    val pendingDriveResolution: IntentSender? = null,
    val showDriveDecryptPassphraseDialog: Boolean = false
)

class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val backupAndExportRepository: BackupAndExportRepository,
    private val driveAuthGateway: DriveAuthGateway,
    private val driveRemoteDataSource: GoogleDriveRemoteDataSource,
    private val googleDriveBackupRepository: GoogleDriveBackupRepository,
    private val appContext: Context
) : ViewModel() {

    private val _statusMessage = MutableStateFlow<String?>(null)
    private val _pendingRestore = MutableStateFlow<BackupPayload?>(null)
    private val _isBusy = MutableStateFlow(false)
    private val _driveState = MutableStateFlow(DriveUiSubState())

    private var pendingDriveAction: PendingDriveAction? = null
    private var pendingEncryptedDriveBytes: ByteArray? = null

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesRepository.preferences,
        categoryRepository.observeCategories(),
        transactionRepository.observeTransactionsWithDetails(),
        _statusMessage,
        _pendingRestore,
        _isBusy,
        _driveState
    ) { values ->
        val prefs = values[0] as UserPreferences
        @Suppress("UNCHECKED_CAST")
        val categories = values[1] as List<Category>
        @Suppress("UNCHECKED_CAST")
        val transactions = values[2] as List<TransactionWithDetails>
        val status = values[3] as String?
        val pendingRestore = values[4] as BackupPayload?
        val busy = values[5] as Boolean
        val drive = values[6] as DriveUiSubState
        SettingsUiState(
            preferences = prefs,
            categories = categories,
            allTransactions = transactions,
            statusMessage = status,
            pendingRestorePayload = pendingRestore,
            isBusy = busy,
            driveBackups = drive.backups,
            isDriveBusy = drive.isBusy,
            driveHistoryVisible = drive.historyVisible,
            pendingDriveResolution = drive.pendingResolution,
            showDriveDecryptPassphraseDialog = drive.showDecryptPassphraseDialog
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun setCurrency(currency: AppCurrency) {
        viewModelScope.launch {
            preferencesRepository.setCurrency(currency)
            runCatching { ExpenseSummaryWidget().updateAll(appContext) }
        }
    }

    fun setCurrencyCode(currencyCode: String) {
        viewModelScope.launch {
            preferencesRepository.setCurrencyCode(currencyCode)
            runCatching { ExpenseSummaryWidget().updateAll(appContext) }
        }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            preferencesRepository.setLanguage(language)
            runCatching { ExpenseSummaryWidget().updateAll(appContext) }
        }
    }

    fun setLanguageTag(languageTag: String) {
        viewModelScope.launch {
            preferencesRepository.setLanguageTag(languageTag)
            runCatching { ExpenseSummaryWidget().updateAll(appContext) }
        }
    }

    fun setViewMode(viewMode: ViewMode) {
        viewModelScope.launch {
            preferencesRepository.setViewMode(viewMode)
        }
    }

    fun setNumberFormat(style: NumberFormatStyle) {
        viewModelScope.launch {
            preferencesRepository.setNumberFormat(style)
            runCatching { ExpenseSummaryWidget().updateAll(appContext) }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    fun setAccentColor(color: AppAccentColor) {
        viewModelScope.launch {
            preferencesRepository.setAccentColor(color)
        }
    }

    fun setHideBalances(hidden: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setHideBalances(hidden)
            runCatching { ExpenseSummaryWidget().updateAll(appContext) }
        }
    }

    fun setBiometricLock(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setBiometricLock(enabled)
        }
    }

    fun setDailyReminder(enabled: Boolean, hour: Int? = null, minute: Int? = null) {
        viewModelScope.launch {
            val prefs = uiState.value.preferences
            val h = hour ?: prefs.reminderHour
            val m = minute ?: prefs.reminderMinute
            preferencesRepository.setDailyReminder(enabled, h, m)
            WorkScheduler.updateDailyReminder(appContext, enabled, h, m)
        }
    }

    fun setBudgetAlerts(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setBudgetAlerts(enabled)
        }
    }

    fun saveCategory(category: Category) {
        viewModelScope.launch {
            categoryRepository.saveCategory(category)
        }
    }

    fun deleteCategory(id: Long) {
        viewModelScope.launch {
            categoryRepository.deleteCategory(id)
        }
    }

    fun exportCsv(uri: Uri, monthYear: String? = null) {
        _isBusy.update { true }
        val prefs = uiState.value.preferences
        viewModelScope.launch {
            backupAndExportRepository.exportCsvToUri(uri, monthYear)
                .onSuccess { count ->
                    _statusMessage.value = if (monthYear != null) {
                        val label = DateTimeUtils.formatMonthLabel(
                            monthYear,
                            prefs.effectiveLanguageTag,
                            prefs.numberFormat
                        )
                        "Exported $count transactions for $label to CSV."
                    } else {
                        "Exported $count transactions to CSV."
                    }
                }
                .onFailure { err -> _statusMessage.value = err.message ?: "CSV export failed." }
            _isBusy.update { false }
        }
    }

    fun exportPdf(uri: Uri) {
        _isBusy.update { true }
        viewModelScope.launch {
            backupAndExportRepository.exportPdfToUri(uri)
                .onSuccess { count -> _statusMessage.value = "Exported PDF statement ($count transactions)." }
                .onFailure { err -> _statusMessage.value = err.message ?: "PDF export failed." }
            _isBusy.update { false }
        }
    }

    fun createEncryptedBackup(uri: Uri, passphrase: String) {
        _isBusy.update { true }
        viewModelScope.launch {
            backupAndExportRepository.createEncryptedBackupToUri(uri, passphrase)
                .onSuccess { payload ->
                    _statusMessage.value =
                        "Encrypted backup saved (${payload.transactions.size} transactions, ${payload.accounts.size} accounts)."
                }
                .onFailure { err ->
                    _statusMessage.value = err.message ?: "Backup encryption failed."
                }
            _isBusy.update { false }
        }
    }

    fun readEncryptedBackup(uri: Uri, passphrase: String) {
        _isBusy.update { true }
        viewModelScope.launch {
            backupAndExportRepository.readEncryptedBackupFromUri(uri, passphrase)
                .onSuccess { payload -> _pendingRestore.value = payload }
                .onFailure { _statusMessage.value = "Could not decrypt backup. Check passphrase or file." }
            _isBusy.update { false }
        }
    }

    fun confirmRestoreBackup() {
        val payload = _pendingRestore.value ?: return
        _pendingRestore.value = null
        _isBusy.update { true }
        viewModelScope.launch {
            backupAndExportRepository.restoreBackup(payload)
                .onSuccess {
                    runCatching { ExpenseSummaryWidget().updateAll(appContext) }
                    _statusMessage.value =
                        "Restored ${payload.accounts.size} accounts and ${payload.transactions.size} transactions."
                }
                .onFailure { err ->
                    _statusMessage.value = err.message ?: "Restore failed."
                }
            _isBusy.update { false }
        }
    }

    fun cancelPendingRestore() {
        _pendingRestore.value = null
    }

    fun clearAllData() {
        _isBusy.update { true }
        viewModelScope.launch {
            backupAndExportRepository.clearAllFinancialData()
                .onSuccess {
                    runCatching { ExpenseSummaryWidget().updateAll(appContext) }
                    _statusMessage.value = "All financial data reset."
                }
                .onFailure { err -> _statusMessage.value = err.message }
            _isBusy.update { false }
        }
    }

    fun connectGoogleDrive(activityContext: Context, isSwitchAccount: Boolean = false) {
        if (!googleDriveBackupRepository.isNetworkAvailable()) {
            _statusMessage.value = "No internet connection. Check your network and try again."
            return
        }
        viewModelScope.launch {
            _driveState.update { it.copy(isBusy = true) }
            if (isSwitchAccount) {
                driveAuthGateway.signOut(activityContext)
            }
            val signInResult = driveAuthGateway.signInWithGoogle(activityContext)
            if (signInResult.isSuccess) {
                val account = signInResult.getOrThrow()
                authorizeAndRun(activityContext, PendingDriveAction.RefreshAfterConnect(account))
            } else {
                val err = signInResult.exceptionOrNull()
                if (err is NoGoogleCredentialAvailableException) {
                    authorizeAndRun(activityContext, PendingDriveAction.FetchAccountAndRefresh)
                } else {
                    _statusMessage.value = err?.message ?: "Google Sign-In failed."
                    _driveState.update { it.copy(isBusy = false) }
                }
            }
        }
    }

    fun disconnectGoogleDrive(context: Context) {
        viewModelScope.launch {
            driveAuthGateway.signOut(context)
            preferencesRepository.setDriveAccount(null, null)
            preferencesRepository.setDriveBackupSchedule(DriveBackupSchedule.MANUAL_ONLY)
            WorkScheduler.updateDriveBackupSchedule(appContext, DriveBackupSchedule.MANUAL_ONLY)
            _driveState.value = DriveUiSubState()
            _statusMessage.value = "Google Drive disconnected."
        }
    }

    fun setDriveBackupSchedule(schedule: DriveBackupSchedule) {
        viewModelScope.launch {
            preferencesRepository.setDriveBackupSchedule(schedule)
            WorkScheduler.updateDriveBackupSchedule(appContext, schedule)
        }
    }

    fun backupToGoogleDrive(activityContext: Context, passphrase: String? = null) {
        if (uiState.value.preferences.driveAccountEmail.isNullOrBlank()) {
            connectGoogleDrive(activityContext)
            return
        }
        if (!googleDriveBackupRepository.isNetworkAvailable()) {
            _statusMessage.value = "No internet connection. Check your network and try again."
            return
        }
        viewModelScope.launch {
            _driveState.update { it.copy(isBusy = true) }
            authorizeAndRun(activityContext, PendingDriveAction.Backup(passphrase))
        }
    }

    fun openDriveHistory(activityContext: Context) {
        if (!googleDriveBackupRepository.isNetworkAvailable()) {
            _statusMessage.value = "No internet connection. Check your network and try again."
            return
        }
        viewModelScope.launch {
            _driveState.update { it.copy(isBusy = true) }
            authorizeAndRun(activityContext, PendingDriveAction.OpenHistory)
        }
    }

    fun closeDriveHistory() {
        _driveState.update { it.copy(historyVisible = false) }
    }

    fun restoreFromDriveEntry(activityContext: Context, entry: DriveBackupEntry) {
        if (!googleDriveBackupRepository.isNetworkAvailable()) {
            _statusMessage.value = "No internet connection. Check your network and try again."
            return
        }
        viewModelScope.launch {
            _driveState.update { it.copy(isBusy = true) }
            authorizeAndRun(activityContext, PendingDriveAction.Restore(entry))
        }
    }

    fun deleteDriveBackupEntry(activityContext: Context, entry: DriveBackupEntry) {
        if (!googleDriveBackupRepository.isNetworkAvailable()) {
            _statusMessage.value = "No internet connection. Check your network and try again."
            return
        }
        viewModelScope.launch {
            _driveState.update { it.copy(isBusy = true) }
            authorizeAndRun(activityContext, PendingDriveAction.Delete(entry))
        }
    }

    fun onDriveResolutionResult(activityContext: Context, resultOk: Boolean, data: Intent?) {
        val action = pendingDriveAction
        pendingDriveAction = null
        _driveState.update { it.copy(pendingResolution = null) }
        if (!resultOk) {
            _driveState.update { it.copy(isBusy = false) }
            _statusMessage.value = "Google Drive permission request was cancelled."
            return
        }
        viewModelScope.launch {
            val tokenResult = driveAuthGateway.extractAccessTokenFromIntent(activityContext, data)
            tokenResult
                .onSuccess { token ->
                    if (action != null) executeDriveAction(token, action)
                    else _driveState.update { it.copy(isBusy = false) }
                }
                .onFailure { err ->
                    _driveState.update { it.copy(isBusy = false) }
                    _statusMessage.value = err.message ?: "Google Drive authorization failed."
                }
        }
    }

    fun decryptPendingDriveBackup(passphrase: String) {
        val bytes = pendingEncryptedDriveBytes ?: return
        googleDriveBackupRepository.parseBackupBytes(bytes, passphrase)
            .onSuccess { payload ->
                pendingEncryptedDriveBytes = null
                _driveState.update { it.copy(showDecryptPassphraseDialog = false, historyVisible = false) }
                _pendingRestore.value = payload
            }
            .onFailure { err ->
                _statusMessage.value = err.message ?: "Incorrect passphrase or corrupted Drive backup."
            }
    }

    fun cancelDriveDecryptDialog() {
        pendingEncryptedDriveBytes = null
        _driveState.update { it.copy(showDecryptPassphraseDialog = false) }
    }

    private suspend fun authorizeAndRun(context: Context, action: PendingDriveAction) {
        when (val outcome = driveAuthGateway.authorizeDriveAccess(context)) {
            is DriveAuthOutcome.Authorized -> executeDriveAction(outcome.accessToken, action)
            is DriveAuthOutcome.NeedsResolution -> {
                pendingDriveAction = action
                _driveState.update { it.copy(pendingResolution = outcome.intentSender) }
            }
            is DriveAuthOutcome.Failed -> {
                _driveState.update { it.copy(isBusy = false) }
                _statusMessage.value = outcome.message
            }
        }
    }

    private suspend fun executeDriveAction(token: String, action: PendingDriveAction) {
        when (action) {
            is PendingDriveAction.FetchAccountAndRefresh -> {
                driveRemoteDataSource.fetchUserInfo(token)
                    .onSuccess { account ->
                        googleDriveBackupRepository.listBackups(token)
                            .onSuccess { list ->
                                preferencesRepository.setDriveAccount(account.email, account.displayName)
                                _driveState.update { it.copy(backups = list, isBusy = false) }
                                _statusMessage.value = "Connected Google Drive (${account.email})."
                            }
                            .onFailure { err ->
                                _driveState.update { it.copy(isBusy = false) }
                                _statusMessage.value = err.message ?: "Google Drive permission check failed."
                            }
                    }
                    .onFailure { err ->
                        _driveState.update { it.copy(isBusy = false) }
                        _statusMessage.value = err.message ?: "Unable to fetch Google account info."
                    }
            }
            is PendingDriveAction.RefreshAfterConnect -> {
                googleDriveBackupRepository.listBackups(token)
                    .onSuccess { list ->
                        preferencesRepository.setDriveAccount(action.account.email, action.account.displayName)
                        _driveState.update { it.copy(backups = list, isBusy = false) }
                        _statusMessage.value = "Connected Google Drive (${action.account.email})."
                    }
                    .onFailure { err ->
                        _driveState.update { it.copy(isBusy = false) }
                        _statusMessage.value = err.message ?: "Google Drive permission check failed."
                    }
            }
            is PendingDriveAction.OpenHistory -> {
                googleDriveBackupRepository.listBackups(token)
                    .onSuccess { list ->
                        _driveState.update { it.copy(backups = list, isBusy = false, historyVisible = true) }
                    }
                    .onFailure { err ->
                        _driveState.update { it.copy(isBusy = false) }
                        _statusMessage.value = err.message ?: "Failed to load Google Drive backups."
                    }
            }
            is PendingDriveAction.Backup -> {
                googleDriveBackupRepository.backupToDrive(token, action.passphrase)
                    .onSuccess { entry ->
                        val updatedList = googleDriveBackupRepository.listBackups(token).getOrNull()
                            ?: listOf(entry) + _driveState.value.backups
                        _driveState.update { it.copy(backups = updatedList, isBusy = false) }
                        val txLabel = entry.transactionCount?.let { " ($it transactions)" }.orEmpty()
                        _statusMessage.value = "Backed up to Google Drive$txLabel."
                    }
                    .onFailure { err ->
                        _driveState.update { it.copy(isBusy = false) }
                        _statusMessage.value = err.message ?: "Google Drive backup failed."
                    }
            }
            is PendingDriveAction.Restore -> {
                googleDriveBackupRepository.downloadBackupBytes(token, action.entry.fileId)
                    .onSuccess { bytes ->
                        googleDriveBackupRepository.parseBackupBytes(bytes, passphrase = null)
                            .onSuccess { payload ->
                                _driveState.update { it.copy(isBusy = false, historyVisible = false) }
                                _pendingRestore.value = payload
                            }
                            .onFailure { parseErr ->
                                _driveState.update { it.copy(isBusy = false) }
                                if (parseErr is EncryptedDriveBackupRequiresPassphraseException) {
                                    pendingEncryptedDriveBytes = bytes
                                    _driveState.update { it.copy(showDecryptPassphraseDialog = true) }
                                } else {
                                    _statusMessage.value = parseErr.message ?: "Invalid backup file."
                                }
                            }
                    }
                    .onFailure { err ->
                        _driveState.update { it.copy(isBusy = false) }
                        _statusMessage.value = err.message ?: "Failed to download backup from Google Drive."
                    }
            }
            is PendingDriveAction.Delete -> {
                googleDriveBackupRepository.deleteBackup(token, action.entry.fileId)
                    .onSuccess {
                        _driveState.update { state ->
                            state.copy(
                                backups = state.backups.filterNot { it.fileId == action.entry.fileId },
                                isBusy = false
                            )
                        }
                        _statusMessage.value = "Backup deleted from Google Drive."
                    }
                    .onFailure { err ->
                        _driveState.update { it.copy(isBusy = false) }
                        _statusMessage.value = err.message ?: "Failed to delete Google Drive backup."
                    }
            }
        }
    }
}
