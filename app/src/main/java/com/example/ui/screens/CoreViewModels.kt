package com.example.ui.screens

import android.content.Context
import android.net.Uri
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.common.DateTimeUtils
import com.example.core.common.stringsFor
import com.example.core.domain.GetDashboardSummaryUseCase
import com.example.core.domain.HomePeriodQuery
import com.example.core.model.Account
import com.example.core.model.Category
import com.example.core.model.CustomDatePreset
import com.example.core.model.DashboardSummary
import com.example.core.model.DateFilterPeriod
import com.example.core.model.RecurrenceFrequency
import com.example.core.model.Transaction
import com.example.core.model.TransactionFilter
import com.example.core.model.TransactionSortOrder
import com.example.core.model.TransactionType
import com.example.core.model.TransactionWithDetails
import com.example.core.model.UserPreferences
import com.example.core.model.ViewMode
import com.example.core.repository.AccountRepository
import com.example.core.repository.BackupAndExportRepository
import com.example.core.repository.CategoryRepository
import com.example.core.repository.PreferencesRepository
import com.example.core.repository.TransactionRepository
import com.example.core.worker.BudgetAlertNotifier
import com.example.widget.ExpenseSummaryWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val transactionRepository: TransactionRepository,
    private val appContext: Context
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = preferencesRepository.preferences
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserPreferences())

    private val _sessionUnlocked = MutableStateFlow(false)
    private val _lockEvaluated = MutableStateFlow(false)

    val isAppLocked: StateFlow<Boolean> = combine(
        preferences,
        _sessionUnlocked,
        _lockEvaluated
    ) { prefs, unlocked, evaluated ->
        if (!evaluated) {
            prefs.biometricLockEnabled
        } else {
            prefs.biometricLockEnabled && !unlocked
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        viewModelScope.launch {
            val prefs = preferencesRepository.preferences.first()
            if (!prefs.biometricLockEnabled) {
                _sessionUnlocked.value = true
            }
            _lockEvaluated.value = true
            val created = transactionRepository.processDueRecurringTransactions()
            if (created > 0) {
                runCatching { ExpenseSummaryWidget().updateAll(appContext) }
            }
        }
    }

    fun onUnlocked() {
        _sessionUnlocked.value = true
        _lockEvaluated.value = true
    }

    fun toggleHideBalances() {
        viewModelScope.launch {
            val current = preferences.value.hideBalances
            preferencesRepository.setHideBalances(!current)
            runCatching { ExpenseSummaryWidget().updateAll(appContext) }
        }
    }
}

private data class HomeCustomFilterState(
    val startMillis: Long? = null,
    val endMillis: Long? = null,
    val isSpecificDate: Boolean = false
) {
    val isActive: Boolean
        get() = startMillis != null && endMillis != null
}

data class HomeUiState(
    val viewMode: ViewMode = ViewMode.MONTHLY,
    val anchorMillis: Long = System.currentTimeMillis(),
    val selectedPeriod: DateFilterPeriod = DateFilterPeriod.THIS_MONTH,
    val customStartMillis: Long? = null,
    val customEndMillis: Long? = null,
    val isSpecificDate: Boolean = false,
    val isCustomFilterActive: Boolean = false,
    val dateRangeValidationError: Boolean = false,
    val summary: DashboardSummary = DashboardSummary(),
    val preferences: UserPreferences = UserPreferences()
)

class HomeViewModel(
    getDashboardSummary: GetDashboardSummaryUseCase,
    preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _anchorMillis = MutableStateFlow(System.currentTimeMillis())
    private val _customFilter = MutableStateFlow(HomeCustomFilterState())
    private val _dateRangeValidationError = MutableStateFlow(false)

    private val queryFlow = combine(
        preferencesRepository.preferences,
        _anchorMillis,
        _customFilter
    ) { prefs, anchor, custom ->
        HomePeriodQuery(
            viewMode = prefs.viewMode,
            anchorMillis = anchor,
            customStartMillis = custom.startMillis,
            customEndMillis = custom.endMillis,
            isSpecificDate = custom.isSpecificDate
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(
        queryFlow,
        getDashboardSummary(queryFlow),
        preferencesRepository.preferences,
        _dateRangeValidationError
    ) { query, summary, prefs, rangeError ->
        val selectedPeriod = when {
            query.isCustomActive -> DateFilterPeriod.CUSTOM
            query.viewMode == ViewMode.DAILY -> DateFilterPeriod.TODAY
            query.viewMode == ViewMode.WEEKLY -> DateFilterPeriod.THIS_WEEK
            query.viewMode == ViewMode.MONTHLY -> DateFilterPeriod.THIS_MONTH
            else -> DateFilterPeriod.THIS_YEAR
        }
        HomeUiState(
            viewMode = query.viewMode,
            anchorMillis = query.anchorMillis,
            selectedPeriod = selectedPeriod,
            customStartMillis = query.customStartMillis,
            customEndMillis = query.customEndMillis,
            isSpecificDate = query.isSpecificDate,
            isCustomFilterActive = query.isCustomActive,
            dateRangeValidationError = rangeError,
            summary = summary,
            preferences = prefs
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun shiftPeriod(delta: Int) {
        val currentMode = uiState.value.viewMode
        _customFilter.value = HomeCustomFilterState()
        _dateRangeValidationError.value = false
        _anchorMillis.update { current ->
            DateTimeUtils.shiftAnchorForViewMode(current, currentMode, delta)
        }
    }

    fun applySpecificDate(dateMillis: Long) {
        val s = DateTimeUtils.startOfDay(dateMillis)
        val e = DateTimeUtils.endOfDay(dateMillis)
        _dateRangeValidationError.value = false
        _anchorMillis.value = dateMillis
        _customFilter.value = HomeCustomFilterState(
            startMillis = s,
            endMillis = e,
            isSpecificDate = true
        )
    }

    fun applyDateRange(startMillis: Long, endMillis: Long): Boolean {
        val startDay = DateTimeUtils.startOfDay(startMillis)
        val endDay = DateTimeUtils.startOfDay(endMillis)
        if (endDay < startDay) {
            _dateRangeValidationError.value = true
            return false
        }
        _dateRangeValidationError.value = false
        _anchorMillis.value = endMillis
        _customFilter.value = HomeCustomFilterState(
            startMillis = DateTimeUtils.startOfDay(startMillis),
            endMillis = DateTimeUtils.endOfDay(endMillis),
            isSpecificDate = startDay == endDay
        )
        return true
    }

    fun clearDateRangeValidationError() {
        _dateRangeValidationError.value = false
    }

    fun applyQuickPreset(preset: CustomDatePreset, nowMillis: Long = System.currentTimeMillis()) {
        _dateRangeValidationError.value = false
        when (preset) {
            CustomDatePreset.TODAY -> applySpecificDate(nowMillis)
            CustomDatePreset.THIS_WEEK -> {
                val (s, e) = DateTimeUtils.rangeForPeriod(DateFilterPeriod.THIS_WEEK, now = nowMillis)
                _anchorMillis.value = nowMillis
                _customFilter.value = HomeCustomFilterState(startMillis = s, endMillis = e, isSpecificDate = false)
            }
            CustomDatePreset.THIS_MONTH -> {
                val (s, e) = DateTimeUtils.rangeForPeriod(DateFilterPeriod.THIS_MONTH, now = nowMillis)
                _anchorMillis.value = nowMillis
                _customFilter.value = HomeCustomFilterState(startMillis = s, endMillis = e, isSpecificDate = false)
            }
            CustomDatePreset.THIS_YEAR -> {
                val (s, e) = DateTimeUtils.rangeForPeriod(DateFilterPeriod.THIS_YEAR, now = nowMillis)
                _anchorMillis.value = nowMillis
                _customFilter.value = HomeCustomFilterState(startMillis = s, endMillis = e, isSpecificDate = false)
            }
            else -> Unit
        }
    }

    fun clearCustomFilter() {
        _dateRangeValidationError.value = false
        _customFilter.value = HomeCustomFilterState()
    }

    fun selectPeriod(period: DateFilterPeriod) {
        when (period) {
            DateFilterPeriod.TODAY -> applyQuickPreset(CustomDatePreset.TODAY)
            DateFilterPeriod.THIS_WEEK -> applyQuickPreset(CustomDatePreset.THIS_WEEK)
            DateFilterPeriod.THIS_MONTH -> applyQuickPreset(CustomDatePreset.THIS_MONTH)
            DateFilterPeriod.THIS_YEAR -> applyQuickPreset(CustomDatePreset.THIS_YEAR)
            DateFilterPeriod.CUSTOM -> Unit
        }
    }

    fun setCustomDateRange(startMillis: Long, endMillis: Long) {
        applyDateRange(startMillis, endMillis)
    }
}

data class TransactionsUiState(
    val filter: TransactionFilter = TransactionFilter(),
    val items: List<TransactionWithDetails> = emptyList(),
    val allTransactions: List<TransactionWithDetails> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val preferences: UserPreferences = UserPreferences(),
    val statusMessage: String? = null
)

class TransactionsViewModel(
    private val transactionRepository: TransactionRepository,
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
    private val preferencesRepository: PreferencesRepository,
    private val backupAndExportRepository: BackupAndExportRepository,
    private val appContext: Context
) : ViewModel() {

    private val _filter = MutableStateFlow(TransactionFilter())
    private val _statusMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<TransactionsUiState> = combine(
        _filter,
        transactionRepository.observeTransactionsWithDetails(),
        accountRepository.observeAccounts(),
        categoryRepository.observeCategories(),
        preferencesRepository.preferences,
        _statusMessage
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val filter = values[0] as TransactionFilter
        @Suppress("UNCHECKED_CAST")
        val allItems = values[1] as List<TransactionWithDetails>
        @Suppress("UNCHECKED_CAST")
        val accounts = values[2] as List<Account>
        @Suppress("UNCHECKED_CAST")
        val categories = values[3] as List<Category>
        val prefs = values[4] as UserPreferences
        val statusMsg = values[5] as String?

        val query = filter.query.trim().lowercase()
        val langTag = prefs.effectiveLanguageTag
        val strings = stringsFor(appContext, langTag)
        val filtered = allItems.filter { item ->
            val tx = item.transaction
            val matchesType = filter.type == null || tx.type == filter.type
            val matchesAccount = filter.accountId == null ||
                tx.accountId == filter.accountId ||
                tx.toAccountId == filter.accountId
            val matchesCategory = filter.categoryId == null || tx.categoryId == filter.categoryId
            val matchesRecurring = !filter.recurringOnly || tx.recurrence != RecurrenceFrequency.NONE
            val matchesStart = filter.startTime == null || tx.timestamp >= DateTimeUtils.startOfDay(filter.startTime)
            val matchesEnd = filter.endTime == null || tx.timestamp <= DateTimeUtils.endOfDay(filter.endTime)
            val matchesQuery = if (query.isEmpty()) true else {
                tx.note.lowercase().contains(query) ||
                    (item.category?.nameEn?.lowercase()?.contains(query) == true) ||
                    (item.category?.nameBn?.lowercase()?.contains(query) == true) ||
                    (item.category?.localizedName(langTag, strings)?.lowercase()?.contains(query) == true) ||
                    (item.account?.name?.lowercase()?.contains(query) == true) ||
                    (item.toAccount?.name?.lowercase()?.contains(query) == true)
            }
            matchesType && matchesAccount && matchesCategory && matchesRecurring &&
                matchesStart && matchesEnd && matchesQuery
        }

        val sorted = when (filter.sortOrder) {
            TransactionSortOrder.NEWEST -> filtered.sortedByDescending { it.transaction.timestamp }
            TransactionSortOrder.OLDEST -> filtered.sortedBy { it.transaction.timestamp }
            TransactionSortOrder.HIGHEST_AMOUNT -> filtered.sortedByDescending { it.transaction.amount }
            TransactionSortOrder.LOWEST_AMOUNT -> filtered.sortedBy { it.transaction.amount }
        }

        TransactionsUiState(
            filter = filter,
            items = sorted,
            allTransactions = allItems,
            accounts = accounts,
            categories = categories,
            preferences = prefs,
            statusMessage = statusMsg
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TransactionsUiState())

    fun setSearchQuery(query: String) {
        _filter.update { it.copy(query = query) }
    }

    fun updateFilter(transform: (TransactionFilter) -> TransactionFilter) {
        _filter.update(transform)
    }

    fun resetFilters() {
        _filter.value = TransactionFilter()
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(id)
            runCatching { ExpenseSummaryWidget().updateAll(appContext) }
        }
    }

    fun exportCsv(uri: Uri, monthYear: String?) {
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
                .onFailure { err ->
                    _statusMessage.value = err.message ?: strings.exportCsvFailed
                }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}

data class TransactionEditorUiState(
    val editingId: Long = 0L,
    val type: TransactionType = TransactionType.EXPENSE,
    val amountInput: String = "",
    val selectedCategoryId: Long? = null,
    val selectedAccountId: Long? = null,
    val selectedToAccountId: Long? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = "",
    val receiptUri: String? = null,
    val recurrence: RecurrenceFrequency = RecurrenceFrequency.NONE,
    val currencyCode: String = "BDT",
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val preferences: UserPreferences = UserPreferences(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val savedSuccessfully: Boolean = false
)

class TransactionEditorViewModel(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val preferencesRepository: PreferencesRepository,
    private val backupAndExportRepository: BackupAndExportRepository,
    private val budgetAlertNotifier: BudgetAlertNotifier,
    private val appContext: Context
) : ViewModel() {

    private val _state = MutableStateFlow(TransactionEditorUiState())
    val uiState: StateFlow<TransactionEditorUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                accountRepository.observeAccounts(),
                categoryRepository.observeCategories(),
                preferencesRepository.preferences
            ) { accounts, categories, prefs ->
                Triple(accounts, categories, prefs)
            }.collect { (accounts, categories, prefs) ->
                _state.update { curr ->
                    val defaultAccount = curr.selectedAccountId ?: accounts.firstOrNull()?.id
                    val defaultToAccount = curr.selectedToAccountId
                        ?: accounts.firstOrNull { it.id != defaultAccount }?.id
                    val matchingCats = categories.filter { it.type == curr.type }
                    val defaultCat = if (curr.type == TransactionType.TRANSFER) null
                    else curr.selectedCategoryId?.takeIf { id -> matchingCats.any { it.id == id } }
                        ?: matchingCats.firstOrNull()?.id
                    val nextCurrency = if (curr.editingId == 0L) prefs.effectiveCurrencyCode else curr.currencyCode

                    curr.copy(
                        accounts = accounts,
                        categories = categories,
                        preferences = prefs,
                        currencyCode = nextCurrency,
                        selectedAccountId = defaultAccount,
                        selectedToAccountId = defaultToAccount,
                        selectedCategoryId = defaultCat
                    )
                }
            }
        }
    }

    fun initialize(
        transactionId: Long?,
        initialType: TransactionType?,
        initialAccountId: Long?
    ) {
        viewModelScope.launch {
            if (transactionId != null && transactionId > 0L) {
                val existing = transactionRepository.observeTransactionById(transactionId).first()?.transaction
                if (existing != null) {
                    _state.update {
                        it.copy(
                            editingId = existing.id,
                            type = existing.type,
                            amountInput = if (existing.amount % 1.0 == 0.0) {
                                existing.amount.toLong().toString()
                            } else {
                                existing.amount.toString()
                            },
                            selectedCategoryId = existing.categoryId,
                            selectedAccountId = existing.accountId,
                            selectedToAccountId = existing.toAccountId,
                            timestamp = existing.timestamp,
                            note = existing.note,
                            receiptUri = existing.receiptUri,
                            recurrence = existing.recurrence,
                            currencyCode = existing.currencyCode.ifBlank { it.preferences.effectiveCurrencyCode },
                            savedSuccessfully = false,
                            errorMessage = null
                        )
                    }
                    return@launch
                }
            }
            val type = initialType ?: TransactionType.EXPENSE
            val cats = _state.value.categories.filter { it.type == type }
            val accs = _state.value.accounts
            val fromAcc = initialAccountId ?: accs.firstOrNull()?.id
            val toAcc = accs.firstOrNull { it.id != fromAcc }?.id
            val currentPrefs = preferencesRepository.preferences.first()
            _state.update {
                it.copy(
                    editingId = 0L,
                    type = type,
                    amountInput = "",
                    selectedCategoryId = if (type == TransactionType.TRANSFER) null else cats.firstOrNull()?.id,
                    selectedAccountId = fromAcc,
                    selectedToAccountId = toAcc,
                    timestamp = System.currentTimeMillis(),
                    note = "",
                    receiptUri = null,
                    recurrence = RecurrenceFrequency.NONE,
                    currencyCode = currentPrefs.effectiveCurrencyCode,
                    preferences = currentPrefs,
                    savedSuccessfully = false,
                    errorMessage = null
                )
            }
        }
    }

    fun setType(type: TransactionType) {
        _state.update { curr ->
            val cats = curr.categories.filter { it.type == type }
            curr.copy(
                type = type,
                selectedCategoryId = if (type == TransactionType.TRANSFER) null else cats.firstOrNull()?.id,
                selectedToAccountId = curr.selectedToAccountId
                    ?: curr.accounts.firstOrNull { it.id != curr.selectedAccountId }?.id,
                errorMessage = null
            )
        }
    }

    fun setAmount(value: String) {
        val sanitized = value.filter { it.isDigit() || it == '.' }
        if (sanitized.count { it == '.' } <= 1) {
            _state.update { it.copy(amountInput = sanitized, errorMessage = null) }
        }
    }

    fun setCategory(categoryId: Long) {
        _state.update { it.copy(selectedCategoryId = categoryId, errorMessage = null) }
    }

    fun setAccount(accountId: Long) {
        _state.update { curr ->
            val nextTo = if (curr.selectedToAccountId == accountId) {
                curr.accounts.firstOrNull { it.id != accountId }?.id
            } else {
                curr.selectedToAccountId
            }
            curr.copy(selectedAccountId = accountId, selectedToAccountId = nextTo, errorMessage = null)
        }
    }

    fun setToAccount(accountId: Long) {
        _state.update { it.copy(selectedToAccountId = accountId, errorMessage = null) }
    }

    fun setTimestamp(millis: Long) {
        _state.update { it.copy(timestamp = millis) }
    }

    fun setNote(note: String) {
        _state.update { it.copy(note = note) }
    }

    fun setRecurrence(freq: RecurrenceFrequency) {
        _state.update { it.copy(recurrence = freq) }
    }

    fun attachReceiptFromPicker(uri: Uri) {
        viewModelScope.launch {
            backupAndExportRepository.copyReceiptImageToInternal(uri)
                .onSuccess { internalPath ->
                    _state.update { it.copy(receiptUri = internalPath) }
                }
                .onFailure { err ->
                    _state.update { it.copy(errorMessage = err.message) }
                }
        }
    }

    fun removeReceipt() {
        _state.update { it.copy(receiptUri = null) }
    }

    fun save() {
        val curr = _state.value
        if (curr.isSaving) return
        val strings = stringsFor(appContext, curr.preferences.effectiveLanguageTag)
        val amount = curr.amountInput.toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            _state.update { it.copy(errorMessage = strings.errInvalidAmount) }
            return
        }
        val accountId = curr.selectedAccountId
        if (accountId == null) {
            _state.update { it.copy(errorMessage = strings.errSelectAccount) }
            return
        }
        if (curr.type == TransactionType.TRANSFER) {
            if (curr.selectedToAccountId == null || curr.selectedToAccountId == accountId) {
                _state.update { it.copy(errorMessage = strings.errDifferentToAccount) }
                return
            }
        }

        _state.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching {
                val tx = Transaction(
                    id = curr.editingId,
                    type = curr.type,
                    amount = amount,
                    categoryId = if (curr.type == TransactionType.TRANSFER) null else curr.selectedCategoryId,
                    accountId = accountId,
                    toAccountId = if (curr.type == TransactionType.TRANSFER) curr.selectedToAccountId else null,
                    note = curr.note.trim(),
                    timestamp = curr.timestamp,
                    receiptUri = curr.receiptUri,
                    recurrence = curr.recurrence,
                    currencyCode = curr.currencyCode.ifBlank { curr.preferences.effectiveCurrencyCode }
                )
                transactionRepository.saveTransaction(tx)
                if (tx.type == TransactionType.EXPENSE) {
                    budgetAlertNotifier.evaluateAndNotify(tx.categoryId, tx.timestamp)
                }
                runCatching { ExpenseSummaryWidget().updateAll(appContext) }
            }.onSuccess {
                _state.update { it.copy(isSaving = false, savedSuccessfully = true) }
            }.onFailure { err ->
                _state.update { it.copy(isSaving = false, errorMessage = err.message ?: strings.errSaveTxFailed) }
            }
        }
    }

    fun delete() {
        val id = _state.value.editingId
        if (id <= 0L) return
        viewModelScope.launch {
            transactionRepository.deleteTransaction(id)
            runCatching { ExpenseSummaryWidget().updateAll(appContext) }
            _state.update { it.copy(savedSuccessfully = true) }
        }
    }
}
