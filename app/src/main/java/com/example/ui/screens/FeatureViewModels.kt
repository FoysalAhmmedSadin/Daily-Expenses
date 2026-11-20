package com.example.ui.screens

import android.content.Context
import android.net.Uri
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.common.DateTimeUtils
import com.example.core.common.stringsFor
import com.example.core.domain.GetAnalyticsReportUseCase
import com.example.core.domain.GetBudgetsWithProgressUseCase
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
import kotlinx.coroutines.flow.first
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

data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences(),
    val categories: List<Category> = emptyList(),
    val allTransactions: List<TransactionWithDetails> = emptyList(),
    val statusMessage: String? = null,
    val pendingRestorePayload: BackupPayload? = null,
    val isBusy: Boolean = false
)

class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val backupAndExportRepository: BackupAndExportRepository,
    private val appContext: Context
) : ViewModel() {

    private val _statusMessage = MutableStateFlow<String?>(null)
    private val _pendingRestore = MutableStateFlow<BackupPayload?>(null)
    private val _isBusy = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesRepository.preferences,
        categoryRepository.observeCategories(),
        transactionRepository.observeTransactionsWithDetails(),
        _statusMessage,
        _pendingRestore,
        _isBusy
    ) { values ->
        val prefs = values[0] as UserPreferences
        @Suppress("UNCHECKED_CAST")
        val categories = values[1] as List<Category>
        @Suppress("UNCHECKED_CAST")
        val transactions = values[2] as List<TransactionWithDetails>
        val status = values[3] as String?
        val pendingRestore = values[4] as BackupPayload?
        val busy = values[5] as Boolean
        SettingsUiState(
            preferences = prefs,
            categories = categories,
            allTransactions = transactions,
            statusMessage = status,
            pendingRestorePayload = pendingRestore,
            isBusy = busy
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
        viewModelScope.launch {
            val prefs = preferencesRepository.preferences.first()
            val strings = stringsFor(appContext, prefs.effectiveLanguageTag)
            backupAndExportRepository.exportCsvToUri(uri, monthYear)
                .onSuccess { count ->
                    _statusMessage.value = if (monthYear != null) {
                        val label = DateTimeUtils.formatMonthLabel(
                            monthYear,
                            prefs.effectiveLanguageTag,
                            prefs.numberFormat
                        )
                        strings.exportCsvMonthSuccess(count, label, prefs.numberFormat)
                    } else {
                        strings.exportCsvAllSuccess(count, prefs.numberFormat)
                    }
                }
                .onFailure { err -> _statusMessage.value = err.message ?: strings.exportCsvFailed }
            _isBusy.update { false }
        }
    }

    fun exportPdf(uri: Uri) {
        _isBusy.update { true }
        viewModelScope.launch {
            val prefs = preferencesRepository.preferences.first()
            val strings = stringsFor(appContext, prefs.effectiveLanguageTag)
            backupAndExportRepository.exportPdfToUri(uri)
                .onSuccess { count ->
                    _statusMessage.value = strings.exportPdfSuccess(count, prefs.numberFormat)
                }
                .onFailure { err -> _statusMessage.value = err.message ?: strings.exportPdfFailed }
            _isBusy.update { false }
        }
    }

    fun createEncryptedBackup(uri: Uri, passphrase: String) {
        _isBusy.update { true }
        viewModelScope.launch {
            val prefs = preferencesRepository.preferences.first()
            val strings = stringsFor(appContext, prefs.effectiveLanguageTag)
            backupAndExportRepository.createEncryptedBackupToUri(uri, passphrase)
                .onSuccess { payload ->
                    _statusMessage.value = strings.backupSavedStatus(
                        txCount = payload.transactions.size,
                        accountCount = payload.accounts.size,
                        numberFormat = prefs.numberFormat
                    )
                }
                .onFailure { err ->
                    _statusMessage.value = err.message ?: strings.backupEncryptionFailed
                }
            _isBusy.update { false }
        }
    }

    fun readEncryptedBackup(uri: Uri, passphrase: String) {
        _isBusy.update { true }
        viewModelScope.launch {
            val prefs = preferencesRepository.preferences.first()
            val strings = stringsFor(appContext, prefs.effectiveLanguageTag)
            backupAndExportRepository.readEncryptedBackupFromUri(uri, passphrase)
                .onSuccess { payload -> _pendingRestore.value = payload }
                .onFailure { _statusMessage.value = strings.backupDecryptFailed }
            _isBusy.update { false }
        }
    }

    fun confirmRestoreBackup() {
        val payload = _pendingRestore.value ?: return
        _pendingRestore.value = null
        _isBusy.update { true }
        viewModelScope.launch {
            val prefs = preferencesRepository.preferences.first()
            val strings = stringsFor(appContext, prefs.effectiveLanguageTag)
            backupAndExportRepository.restoreBackup(payload)
                .onSuccess {
                    runCatching { ExpenseSummaryWidget().updateAll(appContext) }
                    _statusMessage.value = strings.restoreSuccessStatus(
                        accountCount = payload.accounts.size,
                        txCount = payload.transactions.size,
                        numberFormat = prefs.numberFormat
                    )
                }
                .onFailure { err ->
                    _statusMessage.value = err.message ?: strings.restoreFailedStatus
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
            val prefs = preferencesRepository.preferences.first()
            val strings = stringsFor(appContext, prefs.effectiveLanguageTag)
            backupAndExportRepository.clearAllFinancialData()
                .onSuccess {
                    runCatching { ExpenseSummaryWidget().updateAll(appContext) }
                    _statusMessage.value = strings.clearAllDataDone
                }
                .onFailure { err -> _statusMessage.value = err.message }
            _isBusy.update { false }
        }
    }
}
