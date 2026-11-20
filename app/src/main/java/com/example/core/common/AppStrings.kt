package com.example.core.common

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.R
import com.example.core.model.AccountType
import com.example.core.model.AppAccentColor
import com.example.core.model.AppLanguage
import com.example.core.model.DateFilterPeriod
import com.example.core.model.NumberFormatStyle
import com.example.core.model.RecurrenceFrequency
import com.example.core.model.SupportedLanguageCatalog
import com.example.core.model.ThemeMode
import com.example.core.model.TransactionSortOrder
import com.example.core.model.TransactionType
import com.example.core.model.ViewMode

data class CategoryLabelStrings(
    val catFoodDining: String,
    val catGroceries: String,
    val catTransport: String,
    val catHouseRent: String,
    val catUtilitiesBills: String,
    val catMobileInternet: String,
    val catHealthcare: String,
    val catShopping: String,
    val catEducation: String,
    val catEntertainment: String,
    val catTravel: String,
    val catOtherExpense: String,
    val catSalary: String,
    val catFreelance: String,
    val catBusiness: String,
    val catInvestment: String,
    val catGiftBonus: String,
    val catOtherIncome: String
)

data class EnumLabelStrings(
    val accountTypeCash: String,
    val accountTypeBank: String,
    val accountTypeSavings: String,
    val accountTypeCreditCard: String,
    val accountTypeMobileBanking: String,
    val accountTypeCustom: String,
    val recurrenceNone: String,
    val recurrenceDaily: String,
    val recurrenceWeekly: String,
    val recurrenceMonthly: String,
    val recurrenceYearly: String,
    val accentEmerald: String,
    val accentOceanBlue: String,
    val accentIndigo: String,
    val accentViolet: String,
    val accentCoralRed: String,
    val accentAmberGold: String,
    val accentTealCyan: String,
    val accentRosePink: String
)

data class ExportBackupStrings(
    val developerRole: String,
    val developerCopyrightText: String,
    val numberFormatLatinLabel: String,
    val numberFormatBanglaLabel: String,
    val rtlLabel: String,
    val exportCsvMonthSuccessTemplate: String,
    val exportCsvAllSuccessTemplate: String,
    val exportCsvFailed: String,
    val exportPdfSuccessTemplate: String,
    val exportPdfFailed: String,
    val backupSavedStatusTemplate: String,
    val backupEncryptionFailed: String,
    val backupDecryptFailed: String,
    val restoreSuccessStatusTemplate: String,
    val restoreFailedStatus: String,
    val clearAllDataDone: String,
    val pdfStatementTitle: String,
    val pdfGeneratedMetaTemplate: String,
    val pdfAccountsOverview: String,
    val pdfTransactionsCountTemplate: String,
    val pdfColDate: String,
    val pdfColDetails: String,
    val pdfColAccount: String,
    val pdfColAmount: String,
    val csvHeaderRow: String
)

data class AppStrings(
    val appName: String,
    val appSubtitle: String,
    val navHome: String,
    val navTransactions: String,
    val navAnalytics: String,
    val navBudgets: String,
    val navSettings: String,
    val totalBalance: String,
    val totalIncome: String,
    val totalExpenses: String,
    val monthlySavings: String,
    val netCashFlow: String,
    val accounts: String,
    val manage: String,
    val spendingOverview: String,
    val incomeVsExpense: String,
    val categoryBreakdown: String,
    val recentTransactions: String,
    val budgetProgress: String,
    val seeAll: String,
    val today: String,
    val yesterday: String,
    val thisWeek: String,
    val thisMonth: String,
    val thisYear: String,
    val customRange: String,
    val addTransaction: String,
    val editTransaction: String,
    val transactionDetails: String,
    val expense: String,
    val income: String,
    val transfer: String,
    val all: String,
    val amount: String,
    val category: String,
    val account: String,
    val fromAccount: String,
    val toAccount: String,
    val dateTime: String,
    val noteOptional: String,
    val recurring: String,
    val receiptAttachment: String,
    val attachReceipt: String,
    val removeReceipt: String,
    val save: String,
    val cancel: String,
    val delete: String,
    val edit: String,
    val searchTransactions: String,
    val filters: String,
    val reset: String,
    val apply: String,
    val recurringOnly: String,
    val noTransactionsTitle: String,
    val noTransactionsSubtitle: String,
    val addAccount: String,
    val editAccount: String,
    val accountName: String,
    val accountType: String,
    val openingBalance: String,
    val quickTransfer: String,
    val analytics: String,
    val daily: String,
    val weekly: String,
    val monthly: String,
    val yearly: String,
    val savingsRate: String,
    val avgDailySpend: String,
    val vsLastMonth: String,
    val spendingTrends: String,
    val budgets: String,
    val addBudget: String,
    val editBudget: String,
    val monthlyLimit: String,
    val overallMonthlyBudget: String,
    val spent: String,
    val remaining: String,
    val overspentBy: String,
    val alertThreshold: String,
    val noBudgetsTitle: String,
    val noBudgetsSubtitle: String,
    val settings: String,
    val preferences: String,
    val viewMode: String,
    val localizationDisplay: String,
    val language: String,
    val currency: String,
    val numberFormat: String,
    val theme: String,
    val hideBalances: String,
    val securityPrivacy: String,
    val biometricLock: String,
    val notificationsReminders: String,
    val dailyReminder: String,
    val budgetAlerts: String,
    val categories: String,
    val manageCategories: String,
    val addCategory: String,
    val dataBackupExport: String,
    val exportCsv: String,
    val exportPdf: String,
    val encryptedBackup: String,
    val restoreBackup: String,
    val clearAllData: String,
    val aboutLicense: String,
    val unlockTitle: String,
    val unlockSubtitle: String,
    val unlockButton: String,
    val passphraseTitle: String,
    val passphraseHint: String,
    val confirmAction: String,
    val exportMonthlyCsv: String,
    val selectedMonth: String,
    val allMonths: String,
    val noTransactionsInMonth: String,
    val aboutDeveloper: String,
    val contactDeveloper: String,
    val appVersionLabel: String,
    val openSourceLicenses: String,
    val noEmailAppError: String,
    val nextScheduled: String,
    val budgetAlertHeader: String,
    val approachingLimit: String,
    val limitExceeded: String,
    val categorySpendingCap: String,
    val updatesExistingCap: String,
    val themeSystem: String,
    val themeLight: String,
    val themeDark: String,
    val themeAmoled: String,
    val accentColor: String,
    val monthlyExpenseSummary: String,
    val spendingByCategory: String,
    val topSpendingCategory: String,
    val otherCategory: String,
    val noMonthlyExpensesTitle: String,
    val noMonthlyExpensesSubtitle: String,
    // Major UI & Feature Update additions
    val selectSpecificDate: String,
    val selectDateRange: String,
    val clearSelection: String,
    val returnToMonthlyView: String,
    val returnToPeriodViewTemplate: String,
    val customFilterActive: String,
    val invalidDateRange: String,
    val previousPeriod: String,
    val nextPeriod: String,
    val selectPeriodShortcut: String,
    val futurePeriodNote: String,
    val mixedCurrencyNote: String,
    val previousMonth: String,
    val nextMonth: String,
    val budgetsExceededBannerTemplate: String,
    val budgetsWarningBannerTemplate: String,
    val viewModeSettings: String,
    val appearanceSettings: String,
    val languageSettings: String,
    val currencySettings: String,
    val searchLanguages: String,
    val searchCurrencies: String,
    val noMatchingLanguages: String,
    val noMatchingCurrencies: String,
    val selectedLabel: String,
    val appearanceThemeSection: String,
    val biometricNotConfigured: String,
    val reminderTime: String,
    val tapToChangeTime: String,
    val notificationPermissionRequired: String,
    val categoryNamePrimary: String,
    val categoryNameSecondary: String,
    val categoriesCountTemplate: String,
    val clearAllDataConfirm: String,
    val aboutLicenseSummary: String,
    val aboutLicenseDetail: String,
    val encryptedLabel: String,
    val copyrightLabel: String,
    val ok: String,
    val dailyReminderTitle: String,
    val dailyReminderBody: String,
    val budgetMonthlyDefault: String,
    val budgetExceededNotifTitleTemplate: String,
    val budgetWarningNotifTitleTemplate: String,
    val budgetNotifBodyTemplate: String,
    val restoreBackupPreviewTemplate: String,
    val transactionsCountSingleTemplate: String,
    val transactionsCountPluralTemplate: String,
    val budgetAlertSingleTemplate: String,
    val budgetAlertPluralTemplate: String,
    val sortNewest: String,
    val sortOldest: String,
    val sortHighest: String,
    val sortLowest: String,
    val errInvalidAmount: String,
    val errSelectAccount: String,
    val errDifferentToAccount: String,
    val errSaveTxFailed: String,
    val enumLabels: EnumLabelStrings,
    val categoryLabels: CategoryLabelStrings,
    val exportBackup: ExportBackupStrings
) {
    val accountTypeCash: String get() = enumLabels.accountTypeCash
    val accountTypeBank: String get() = enumLabels.accountTypeBank
    val accountTypeSavings: String get() = enumLabels.accountTypeSavings
    val accountTypeCreditCard: String get() = enumLabels.accountTypeCreditCard
    val accountTypeMobileBanking: String get() = enumLabels.accountTypeMobileBanking
    val accountTypeCustom: String get() = enumLabels.accountTypeCustom
    val recurrenceNone: String get() = enumLabels.recurrenceNone
    val recurrenceDaily: String get() = enumLabels.recurrenceDaily
    val recurrenceWeekly: String get() = enumLabels.recurrenceWeekly
    val recurrenceMonthly: String get() = enumLabels.recurrenceMonthly
    val recurrenceYearly: String get() = enumLabels.recurrenceYearly
    val accentEmerald: String get() = enumLabels.accentEmerald
    val accentOceanBlue: String get() = enumLabels.accentOceanBlue
    val accentIndigo: String get() = enumLabels.accentIndigo
    val accentViolet: String get() = enumLabels.accentViolet
    val accentCoralRed: String get() = enumLabels.accentCoralRed
    val accentAmberGold: String get() = enumLabels.accentAmberGold
    val accentTealCyan: String get() = enumLabels.accentTealCyan
    val accentRosePink: String get() = enumLabels.accentRosePink

    val catFoodDining: String get() = categoryLabels.catFoodDining
    val catGroceries: String get() = categoryLabels.catGroceries
    val catTransport: String get() = categoryLabels.catTransport
    val catHouseRent: String get() = categoryLabels.catHouseRent
    val catUtilitiesBills: String get() = categoryLabels.catUtilitiesBills
    val catMobileInternet: String get() = categoryLabels.catMobileInternet
    val catHealthcare: String get() = categoryLabels.catHealthcare
    val catShopping: String get() = categoryLabels.catShopping
    val catEducation: String get() = categoryLabels.catEducation
    val catEntertainment: String get() = categoryLabels.catEntertainment
    val catTravel: String get() = categoryLabels.catTravel
    val catOtherExpense: String get() = categoryLabels.catOtherExpense
    val catSalary: String get() = categoryLabels.catSalary
    val catFreelance: String get() = categoryLabels.catFreelance
    val catBusiness: String get() = categoryLabels.catBusiness
    val catInvestment: String get() = categoryLabels.catInvestment
    val catGiftBonus: String get() = categoryLabels.catGiftBonus
    val catOtherIncome: String get() = categoryLabels.catOtherIncome

    val developerRole: String get() = exportBackup.developerRole
    val developerCopyrightText: String get() = exportBackup.developerCopyrightText
    val numberFormatLatinLabel: String get() = exportBackup.numberFormatLatinLabel
    val numberFormatBanglaLabel: String get() = exportBackup.numberFormatBanglaLabel
    val rtlLabel: String get() = exportBackup.rtlLabel
    val exportCsvMonthSuccessTemplate: String get() = exportBackup.exportCsvMonthSuccessTemplate
    val exportCsvAllSuccessTemplate: String get() = exportBackup.exportCsvAllSuccessTemplate
    val exportCsvFailed: String get() = exportBackup.exportCsvFailed
    val exportPdfSuccessTemplate: String get() = exportBackup.exportPdfSuccessTemplate
    val exportPdfFailed: String get() = exportBackup.exportPdfFailed
    val backupSavedStatusTemplate: String get() = exportBackup.backupSavedStatusTemplate
    val backupEncryptionFailed: String get() = exportBackup.backupEncryptionFailed
    val backupDecryptFailed: String get() = exportBackup.backupDecryptFailed
    val restoreSuccessStatusTemplate: String get() = exportBackup.restoreSuccessStatusTemplate
    val restoreFailedStatus: String get() = exportBackup.restoreFailedStatus
    val clearAllDataDone: String get() = exportBackup.clearAllDataDone
    val pdfStatementTitle: String get() = exportBackup.pdfStatementTitle
    val pdfGeneratedMetaTemplate: String get() = exportBackup.pdfGeneratedMetaTemplate
    val pdfAccountsOverview: String get() = exportBackup.pdfAccountsOverview
    val pdfTransactionsCountTemplate: String get() = exportBackup.pdfTransactionsCountTemplate
    val pdfColDate: String get() = exportBackup.pdfColDate
    val pdfColDetails: String get() = exportBackup.pdfColDetails
    val pdfColAccount: String get() = exportBackup.pdfColAccount
    val pdfColAmount: String get() = exportBackup.pdfColAmount
    val csvHeaderRow: String get() = exportBackup.csvHeaderRow
    fun exportCsvMonthSuccess(count: Int, monthLabel: String, numberFormat: NumberFormatStyle): String {
        val num = CurrencyFormatter.toLocalizedDigits(count.toString(), numberFormat)
        return exportCsvMonthSuccessTemplate
            .replace("{count}", num)
            .replace("{month}", monthLabel)
    }

    fun exportCsvAllSuccess(count: Int, numberFormat: NumberFormatStyle): String {
        val num = CurrencyFormatter.toLocalizedDigits(count.toString(), numberFormat)
        return exportCsvAllSuccessTemplate.replace("{count}", num)
    }

    fun exportPdfSuccess(count: Int, numberFormat: NumberFormatStyle): String {
        val num = CurrencyFormatter.toLocalizedDigits(count.toString(), numberFormat)
        return exportPdfSuccessTemplate.replace("{count}", num)
    }

    fun backupSavedStatus(txCount: Int, accountCount: Int, numberFormat: NumberFormatStyle): String {
        val tx = CurrencyFormatter.toLocalizedDigits(txCount.toString(), numberFormat)
        val acc = CurrencyFormatter.toLocalizedDigits(accountCount.toString(), numberFormat)
        return backupSavedStatusTemplate
            .replace("{transactions}", tx)
            .replace("{accounts}", acc)
    }

    fun restoreSuccessStatus(accountCount: Int, txCount: Int, numberFormat: NumberFormatStyle): String {
        val acc = CurrencyFormatter.toLocalizedDigits(accountCount.toString(), numberFormat)
        val tx = CurrencyFormatter.toLocalizedDigits(txCount.toString(), numberFormat)
        return restoreSuccessStatusTemplate
            .replace("{accounts}", acc)
            .replace("{transactions}", tx)
    }

    fun pdfGeneratedMeta(date: String, currencyCode: String): String =
        pdfGeneratedMetaTemplate
            .replace("{date}", date)
            .replace("{currency}", currencyCode)

    fun pdfTransactionsCount(count: Int, numberFormat: NumberFormatStyle): String {
        val num = CurrencyFormatter.toLocalizedDigits(count.toString(), numberFormat)
        return pdfTransactionsCountTemplate.replace("{count}", num)
    }
    fun transactionsCount(count: Int, numberFormat: NumberFormatStyle): String {
        val num = CurrencyFormatter.toLocalizedDigits(count.toString(), numberFormat)
        val tmpl = if (count == 1) transactionsCountSingleTemplate else transactionsCountPluralTemplate
        return tmpl.replace("{count}", num)
    }

    fun budgetAlertSummary(count: Int, numberFormat: NumberFormatStyle): String {
        val num = CurrencyFormatter.toLocalizedDigits(count.toString(), numberFormat)
        val tmpl = if (count == 1) budgetAlertSingleTemplate else budgetAlertPluralTemplate
        return tmpl.replace("{count}", num)
    }

    fun returnToPeriodView(viewMode: ViewMode): String =
        returnToPeriodViewTemplate.replace("{view_mode}", viewModeLabel(viewMode))

    fun budgetsExceededBanner(count: Int, numberFormat: NumberFormatStyle): String {
        val num = CurrencyFormatter.toLocalizedDigits(count.toString(), numberFormat)
        return budgetsExceededBannerTemplate.replace("{count}", num)
    }

    fun budgetsWarningBanner(count: Int, numberFormat: NumberFormatStyle): String {
        val num = CurrencyFormatter.toLocalizedDigits(count.toString(), numberFormat)
        return budgetsWarningBannerTemplate.replace("{count}", num)
    }

    fun categoriesCount(count: Int, numberFormat: NumberFormatStyle): String {
        val num = CurrencyFormatter.toLocalizedDigits(count.toString(), numberFormat)
        return categoriesCountTemplate.replace("{count}", num)
    }

    fun budgetNotifTitle(categoryName: String, isOver: Boolean): String {
        val tmpl = if (isOver) budgetExceededNotifTitleTemplate else budgetWarningNotifTitleTemplate
        return tmpl.replace("{cat}", categoryName)
    }

    fun budgetNotifBody(spent: String, limit: String, pct: Int, numberFormat: NumberFormatStyle): String {
        val pctText = CurrencyFormatter.toLocalizedDigits(pct.toString(), numberFormat)
        return budgetNotifBodyTemplate
            .replace("{spent}", spent)
            .replace("{limit}", limit)
            .replace("{pct}", pctText)
    }

    fun restoreBackupPreview(
        date: String,
        version: Int,
        accounts: Int,
        categories: Int,
        transactions: Int,
        budgets: Int,
        numberFormat: NumberFormatStyle
    ): String {
        fun num(n: Int) = CurrencyFormatter.toLocalizedDigits(n.toString(), numberFormat)
        return restoreBackupPreviewTemplate
            .replace("{date}", date)
            .replace("{version}", num(version))
            .replace("{accounts}", num(accounts))
            .replace("{categories}", num(categories))
            .replace("{transactions}", num(transactions))
            .replace("{budgets}", num(budgets))
    }

    fun periodLabel(period: DateFilterPeriod): String = when (period) {
        DateFilterPeriod.TODAY -> today
        DateFilterPeriod.THIS_WEEK -> thisWeek
        DateFilterPeriod.THIS_MONTH -> thisMonth
        DateFilterPeriod.THIS_YEAR -> thisYear
        DateFilterPeriod.CUSTOM -> customRange
    }

    fun viewModeLabel(mode: ViewMode): String = when (mode) {
        ViewMode.DAILY -> daily
        ViewMode.WEEKLY -> weekly
        ViewMode.MONTHLY -> monthly
        ViewMode.YEARLY -> yearly
    }

    fun sortOrderLabel(sort: TransactionSortOrder): String = when (sort) {
        TransactionSortOrder.NEWEST -> sortNewest
        TransactionSortOrder.OLDEST -> sortOldest
        TransactionSortOrder.HIGHEST_AMOUNT -> sortHighest
        TransactionSortOrder.LOWEST_AMOUNT -> sortLowest
    }

    fun transactionTypeLabel(type: TransactionType): String = when (type) {
        TransactionType.EXPENSE -> expense
        TransactionType.INCOME -> income
        TransactionType.TRANSFER -> transfer
    }

    fun accountTypeLabel(type: AccountType): String = when (type) {
        AccountType.CASH -> accountTypeCash
        AccountType.BANK -> accountTypeBank
        AccountType.SAVINGS -> accountTypeSavings
        AccountType.CREDIT_CARD -> accountTypeCreditCard
        AccountType.MOBILE_BANKING -> accountTypeMobileBanking
        AccountType.CUSTOM -> accountTypeCustom
    }

    @Suppress("UNUSED_PARAMETER")
    fun accountTypeLabel(type: AccountType, lang: AppLanguage): String = accountTypeLabel(type)

    fun recurrenceLabel(freq: RecurrenceFrequency): String = when (freq) {
        RecurrenceFrequency.NONE -> recurrenceNone
        RecurrenceFrequency.DAILY -> recurrenceDaily
        RecurrenceFrequency.WEEKLY -> recurrenceWeekly
        RecurrenceFrequency.MONTHLY -> recurrenceMonthly
        RecurrenceFrequency.YEARLY -> recurrenceYearly
    }

    @Suppress("UNUSED_PARAMETER")
    fun recurrenceLabel(freq: RecurrenceFrequency, lang: AppLanguage): String = recurrenceLabel(freq)

    fun themeModeLabel(mode: ThemeMode): String = when (mode) {
        ThemeMode.SYSTEM -> themeSystem
        ThemeMode.LIGHT -> themeLight
        ThemeMode.DARK -> themeDark
        ThemeMode.AMOLED -> themeAmoled
    }

    fun accentColorLabel(color: AppAccentColor): String = when (color) {
        AppAccentColor.EMERALD -> accentEmerald
        AppAccentColor.OCEAN_BLUE -> accentOceanBlue
        AppAccentColor.INDIGO -> accentIndigo
        AppAccentColor.VIOLET -> accentViolet
        AppAccentColor.CORAL_RED -> accentCoralRed
        AppAccentColor.AMBER_GOLD -> accentAmberGold
        AppAccentColor.TEAL_CYAN -> accentTealCyan
        AppAccentColor.ROSE_PINK -> accentRosePink
    }

    @Suppress("UNUSED_PARAMETER")
    fun accentColorLabel(color: AppAccentColor, lang: AppLanguage): String = accentColorLabel(color)

    fun localizedDefaultCategoryName(nameEn: String): String? = when (nameEn) {
        "Food & Dining" -> catFoodDining
        "Groceries" -> catGroceries
        "Transport" -> catTransport
        "House Rent" -> catHouseRent
        "Utilities & Bills" -> catUtilitiesBills
        "Mobile & Internet" -> catMobileInternet
        "Healthcare" -> catHealthcare
        "Shopping" -> catShopping
        "Education" -> catEducation
        "Entertainment" -> catEntertainment
        "Travel" -> catTravel
        "Other Expense" -> catOtherExpense
        "Salary" -> catSalary
        "Freelance" -> catFreelance
        "Business" -> catBusiness
        "Investment" -> catInvestment
        "Gift & Bonus" -> catGiftBonus
        "Other Income" -> catOtherIncome
        else -> null
    }
}

val EnglishStrings = AppStrings(
    appName = "Daily Expenses",
    appSubtitle = "Personal Finance Manager",
    navHome = "Home",
    navTransactions = "Transactions",
    navAnalytics = "Analytics",
    navBudgets = "Budgets",
    navSettings = "Settings",
    totalBalance = "Total Balance",
    totalIncome = "Total Income",
    totalExpenses = "Total Expenses",
    monthlySavings = "Monthly Savings",
    netCashFlow = "Net Cash Flow",
    accounts = "Accounts",
    manage = "Manage",
    spendingOverview = "Spending Overview",
    incomeVsExpense = "Income vs Expense",
    categoryBreakdown = "Expense Breakdown",
    recentTransactions = "Recent Transactions",
    budgetProgress = "Budget Progress",
    seeAll = "See All",
    today = "Today",
    yesterday = "Yesterday",
    thisWeek = "This Week",
    thisMonth = "This Month",
    thisYear = "This Year",
    customRange = "Custom",
    addTransaction = "Add Transaction",
    editTransaction = "Edit Transaction",
    transactionDetails = "Transaction Details",
    expense = "Expense",
    income = "Income",
    transfer = "Transfer",
    all = "All",
    amount = "Amount",
    category = "Category",
    account = "Account",
    fromAccount = "From Account",
    toAccount = "To Account",
    dateTime = "Date & Time",
    noteOptional = "Note (optional)",
    recurring = "Recurring",
    receiptAttachment = "Receipt Attachment",
    attachReceipt = "Attach Receipt Photo",
    removeReceipt = "Remove Photo",
    save = "Save",
    cancel = "Cancel",
    delete = "Delete",
    edit = "Edit",
    searchTransactions = "Search notes, categories, accounts...",
    filters = "Filters",
    reset = "Reset",
    apply = "Apply",
    recurringOnly = "Recurring Only",
    noTransactionsTitle = "No transactions yet",
    noTransactionsSubtitle = "Tap + to record an expense, income, or account transfer.",
    addAccount = "Add Account",
    editAccount = "Edit Account",
    accountName = "Account Name",
    accountType = "Account Type",
    openingBalance = "Opening Balance",
    quickTransfer = "Transfer",
    analytics = "Analytics & Reports",
    daily = "Daily",
    weekly = "Weekly",
    monthly = "Monthly",
    yearly = "Yearly",
    savingsRate = "Savings Rate",
    avgDailySpend = "Avg Daily Spend",
    vsLastMonth = "vs Last Month",
    spendingTrends = "Spending Trends",
    budgets = "Budgets",
    addBudget = "Add Budget",
    editBudget = "Edit Budget",
    monthlyLimit = "Monthly Limit",
    overallMonthlyBudget = "Overall Monthly Budget",
    spent = "Spent",
    remaining = "Remaining",
    overspentBy = "Overspent by",
    alertThreshold = "Alert Threshold",
    noBudgetsTitle = "No budgets configured",
    noBudgetsSubtitle = "Set a monthly or category spending limit to track progress and receive alerts.",
    settings = "Settings",
    preferences = "Preferences",
    viewMode = "View Mode",
    localizationDisplay = "Localization & Display",
    language = "Language",
    currency = "Currency",
    numberFormat = "Number Format",
    theme = "Appearance",
    hideBalances = "Hide Balances",
    securityPrivacy = "Security & Privacy",
    biometricLock = "Biometric App Lock",
    notificationsReminders = "Notifications & Reminders",
    dailyReminder = "Daily Expense Reminder",
    budgetAlerts = "Overspending Budget Alerts",
    categories = "Categories",
    manageCategories = "Manage Custom Categories",
    addCategory = "Add Category",
    dataBackupExport = "Export & Encrypted Backup",
    exportCsv = "Export CSV",
    exportPdf = "Export PDF Statement",
    encryptedBackup = "Create Encrypted Backup",
    restoreBackup = "Restore Encrypted Backup",
    clearAllData = "Clear All Data",
    aboutLicense = "About & Open Source License",
    unlockTitle = "Daily Expenses Locked",
    unlockSubtitle = "Authenticate with biometrics or device credentials to access your financial records.",
    unlockButton = "Unlock",
    passphraseTitle = "Backup Passphrase",
    passphraseHint = "Enter passphrase (min 4 chars)",
    confirmAction = "Confirm",
    exportMonthlyCsv = "Export Monthly CSV",
    selectedMonth = "Selected Month",
    allMonths = "All Months",
    noTransactionsInMonth = "No transactions recorded in this month.",
    aboutDeveloper = "About Developer",
    contactDeveloper = "Contact Developer",
    appVersionLabel = "App Version",
    openSourceLicenses = "Third-Party Open-Source Licenses",
    noEmailAppError = "No email app found on this device. Please send your inquiry to foysal24x7@gmail.com.",
    nextScheduled = "Next Scheduled Entry",
    budgetAlertHeader = "80% Budget Limit Alert",
    approachingLimit = "80%+ Reached",
    limitExceeded = "Limit Exceeded",
    categorySpendingCap = "Category Spending Cap",
    updatesExistingCap = "A cap already exists for this month; saving will update it.",
    themeSystem = "System Default",
    themeLight = "Light",
    themeDark = "Dark",
    themeAmoled = "AMOLED",
    accentColor = "Accent Color",
    monthlyExpenseSummary = "Monthly Expense Summary",
    spendingByCategory = "Spending by category",
    topSpendingCategory = "Top Category",
    otherCategory = "Other",
    noMonthlyExpensesTitle = "No expenses this month",
    noMonthlyExpensesSubtitle = "Add an expense transaction to visualize your category spending trends.",
    selectSpecificDate = "Select Specific Date",
    selectDateRange = "Select Date Range",
    clearSelection = "Clear Selection",
    returnToMonthlyView = "Return to Monthly View",
    returnToPeriodViewTemplate = "Return to {view_mode} View",
    customFilterActive = "Custom filter active",
    invalidDateRange = "End date cannot be earlier than start date.",
    previousPeriod = "Previous period",
    nextPeriod = "Next period",
    selectPeriodShortcut = "Period & Date Selection",
    futurePeriodNote = "Future period selected — showing scheduled or future-dated entries only.",
    mixedCurrencyNote = "Mixed currencies recorded in this period (totals shown per currency without conversion).",
    previousMonth = "Previous month",
    nextMonth = "Next month",
    budgetsExceededBannerTemplate = "{count} budget(s) exceeded limit this month.",
    budgetsWarningBannerTemplate = "{count} budget(s) approaching spending limit.",
    viewModeSettings = "View Mode",
    appearanceSettings = "Appearance",
    languageSettings = "Language",
    currencySettings = "Currency",
    searchLanguages = "Search languages...",
    searchCurrencies = "Search currency code, name, or country...",
    noMatchingLanguages = "No matching languages found",
    noMatchingCurrencies = "No matching currencies found",
    selectedLabel = "Selected",
    appearanceThemeSection = "Theme",
    biometricNotConfigured = "Biometric or device credential lock is not configured on this device.",
    reminderTime = "Reminder Time",
    tapToChangeTime = "Tap to change reminder time",
    notificationPermissionRequired = "Notification permission is required for reminders.",
    categoryNamePrimary = "Name (English)",
    categoryNameSecondary = "Local Name (optional)",
    categoriesCountTemplate = "{count} categories",
    clearAllDataConfirm = "This will delete all transactions, budgets, and custom accounts and restore default starter accounts with zero balances.",
    aboutLicenseSummary = "Apache License 2.0 • Open Source Components",
    aboutLicenseDetail = "Licensed under the Apache License, Version 2.0. Offline-first storage via Room with AES-256-GCM encrypted backup support.",
    encryptedLabel = "Encrypted",
    copyrightLabel = "Copyright",
    ok = "OK",
    dailyReminderTitle = "Daily Expenses Reminder",
    dailyReminderBody = "Don't forget to record your expenses for today.",
    budgetMonthlyDefault = "Monthly Budget",
    budgetExceededNotifTitleTemplate = "Budget Exceeded: {cat}",
    budgetWarningNotifTitleTemplate = "Budget Warning: {cat}",
    budgetNotifBodyTemplate = "Spent {spent} of {limit} limit ({pct}%)",
    restoreBackupPreviewTemplate = "Backup from {date} (v{version}) contains {accounts} accounts, {categories} categories, {transactions} transactions, and {budgets} budgets. A safety backup will be created before replacing your current data.",
    transactionsCountSingleTemplate = "{count} transaction",
    transactionsCountPluralTemplate = "{count} transactions",
    budgetAlertSingleTemplate = "{count} budget has reached 80% or more of its monthly cap",
    budgetAlertPluralTemplate = "{count} budgets have reached 80% or more of their monthly cap",
    sortNewest = "Newest",
    sortOldest = "Oldest",
    sortHighest = "Highest Amount",
    sortLowest = "Lowest Amount",
    errInvalidAmount = "Please enter a valid amount greater than 0.",
    errSelectAccount = "Please create or select an account first.",
    errDifferentToAccount = "Select a different destination account for transfer.",
    errSaveTxFailed = "Failed to save transaction.",
    enumLabels = EnumLabelStrings(
        accountTypeCash = "Cash",
        accountTypeBank = "Bank Account",
        accountTypeSavings = "Savings",
        accountTypeCreditCard = "Credit Card",
        accountTypeMobileBanking = "Mobile Banking",
        accountTypeCustom = "Custom",
        recurrenceNone = "One-time",
        recurrenceDaily = "Daily",
        recurrenceWeekly = "Weekly",
        recurrenceMonthly = "Monthly",
        recurrenceYearly = "Yearly",
        accentEmerald = "Emerald",
        accentOceanBlue = "Ocean Blue",
        accentIndigo = "Indigo",
        accentViolet = "Violet",
        accentCoralRed = "Coral Red",
        accentAmberGold = "Amber Gold",
        accentTealCyan = "Teal Cyan",
        accentRosePink = "Rose Pink"
    ),
    categoryLabels = CategoryLabelStrings(
        catFoodDining = "Food & Dining",
        catGroceries = "Groceries",
        catTransport = "Transport",
        catHouseRent = "House Rent",
        catUtilitiesBills = "Utilities & Bills",
        catMobileInternet = "Mobile & Internet",
        catHealthcare = "Healthcare",
        catShopping = "Shopping",
        catEducation = "Education",
        catEntertainment = "Entertainment",
        catTravel = "Travel",
        catOtherExpense = "Other Expense",
        catSalary = "Salary",
        catFreelance = "Freelance",
        catBusiness = "Business",
        catInvestment = "Investment",
        catGiftBonus = "Gift & Bonus",
        catOtherIncome = "Other Income"
    ),
    exportBackup = ExportBackupStrings(
        developerRole = "App Developer",
        developerCopyrightText = "Copyright © 2026 Foysal Ahmmed Sadin.",
        numberFormatLatinLabel = "English (123)",
        numberFormatBanglaLabel = "বাংলা (১২৩)",
        rtlLabel = "RTL",
        exportCsvMonthSuccessTemplate = "Exported {count} transactions for {month} to CSV.",
        exportCsvAllSuccessTemplate = "Exported {count} transactions to CSV.",
        exportCsvFailed = "CSV export failed.",
        exportPdfSuccessTemplate = "Exported PDF statement ({count} transactions).",
        exportPdfFailed = "PDF export failed.",
        backupSavedStatusTemplate = "Encrypted backup saved ({transactions} transactions, {accounts} accounts).",
        backupEncryptionFailed = "Backup encryption failed.",
        backupDecryptFailed = "Could not decrypt backup. Check passphrase or file.",
        restoreSuccessStatusTemplate = "Restored {accounts} accounts and {transactions} transactions.",
        restoreFailedStatus = "Restore failed.",
        clearAllDataDone = "All financial data reset.",
        pdfStatementTitle = "Daily Expenses — Financial Statement",
        pdfGeneratedMetaTemplate = "Generated: {date}  |  Currency: {currency}",
        pdfAccountsOverview = "Accounts Overview",
        pdfTransactionsCountTemplate = "Transactions ({count})",
        pdfColDate = "Date",
        pdfColDetails = "Category / Details",
        pdfColAccount = "Account",
        pdfColAmount = "Amount",
        csvHeaderRow = "Date,Time,Type,Category,Account,To Account,Amount,Currency,Note,Recurring"
    )
)

val BanglaStrings = AppStrings(
    appName = "Daily Expenses",
    appSubtitle = "ব্যক্তিগত অর্থ ব্যবস্থাপনা",
    navHome = "হোম",
    navTransactions = "লেনদেন",
    navAnalytics = "বিশ্লেষণ",
    navBudgets = "বাজেট",
    navSettings = "সেটিংস",
    totalBalance = "মোট ব্যালেন্স",
    totalIncome = "মোট আয়",
    totalExpenses = "মোট খরচ",
    monthlySavings = "মাসিক সঞ্চয়",
    netCashFlow = "নিট নগদ প্রবাহ",
    accounts = "অ্যাকাউন্টসমূহ",
    manage = "পরিচালনা",
    spendingOverview = "খরচের সারসংক্ষেপ",
    incomeVsExpense = "আয় বনাম খরচ",
    categoryBreakdown = "খাতভিত্তিক খরচ",
    recentTransactions = "সাম্প্রতিক লেনদেন",
    budgetProgress = "বাজেট অগ্রগতি",
    seeAll = "সব দেখুন",
    today = "আজ",
    yesterday = "গতকাল",
    thisWeek = "এই সপ্তাহ",
    thisMonth = "এই মাস",
    thisYear = "এই বছর",
    customRange = "কাস্টম",
    addTransaction = "নতুন লেনদেন",
    editTransaction = "লেনদেন সম্পাদনা",
    transactionDetails = "লেনদেনের বিবরণ",
    expense = "খরচ",
    income = "আয়",
    transfer = "ট্রান্সফার",
    all = "সব",
    amount = "পরিমাণ",
    category = "ক্যাটাগরি",
    account = "অ্যাকাউন্ট",
    fromAccount = "যে অ্যাকাউন্ট থেকে",
    toAccount = "যে অ্যাকাউন্টে",
    dateTime = "তারিখ ও সময়",
    noteOptional = "নোট (ঐচ্ছিক)",
    recurring = "পুনরাবৃত্তি",
    receiptAttachment = "রসিদের ছবি",
    attachReceipt = "রসিদের ছবি যুক্ত করুন",
    removeReceipt = "ছবি মুছুন",
    save = "সংরক্ষণ করুন",
    cancel = "বাতিল",
    delete = "মুছে ফেলুন",
    edit = "সম্পাদনা",
    searchTransactions = "নোট, ক্যাটাগরি বা অ্যাকাউন্ট খুঁজুন...",
    filters = "ফিল্টার",
    reset = "রিসেট",
    apply = "প্রয়োগ করুন",
    recurringOnly = "শুধু পুনরাবৃত্ত লেনদেন",
    noTransactionsTitle = "এখনও কোনো লেনদেন নেই",
    noTransactionsSubtitle = "খরচ, আয় বা ট্রান্সফার যোগ করতে + বাটনে ট্যাপ করুন।",
    addAccount = "অ্যাকাউন্ট যোগ করুন",
    editAccount = "অ্যাকাউন্ট সম্পাদনা",
    accountName = "অ্যাকাউন্টের নাম",
    accountType = "অ্যাকাউন্টের ধরন",
    openingBalance = "প্রাথমিক ব্যালেন্স",
    quickTransfer = "ট্রান্সফার",
    analytics = "বিশ্লেষণ ও প্রতিবেদন",
    daily = "দৈনিক",
    weekly = "সাপ্তাহিক",
    monthly = "মাসিক",
    yearly = "বার্ষিক",
    savingsRate = "সঞ্চয়ের হার",
    avgDailySpend = "দৈনিক গড় খরচ",
    vsLastMonth = "গত মাসের তুলনায়",
    spendingTrends = "খরচের প্রবণতা",
    budgets = "বাজেট",
    addBudget = "বাজেট যোগ করুন",
    editBudget = "বাজেট সম্পাদনা",
    monthlyLimit = "মাসিক সীমা",
    overallMonthlyBudget = "মোট মাসিক বাজেট",
    spent = "খরচ হয়েছে",
    remaining = "অবশিষ্ট",
    overspentBy = "অতিরিক্ত খরচ",
    alertThreshold = "সতর্কতা সীমা",
    noBudgetsTitle = "কোনো বাজেট সেট করা নেই",
    noBudgetsSubtitle = "খরচ নিয়ন্ত্রণে রাখতে মাসিক বা ক্যাটাগরি বাজেট নির্ধারণ করুন।",
    settings = "সেটিংস",
    preferences = "পছন্দসমূহ",
    viewMode = "ভিউ মোড",
    localizationDisplay = "ভাষা, মুদ্রা ও প্রদর্শন",
    language = "ভাষা",
    currency = "মুদ্রা",
    numberFormat = "সংখ্যার ফরম্যাট",
    theme = "অ্যাপিয়ারেন্স",
    hideBalances = "ব্যালেন্স গোপন রাখুন",
    securityPrivacy = "নিরাপত্তা ও গোপনীয়তা",
    biometricLock = "বায়োমেট্রিক লক",
    notificationsReminders = "নোটিফিকেশন ও রিমাইন্ডার",
    dailyReminder = "দৈনিক খরচের রিমাইন্ডার",
    budgetAlerts = "বাজেট অতিক্রম সতর্কতা",
    categories = "ক্যাটাগরিসমূহ",
    manageCategories = "কাস্টম ক্যাটাগরি পরিচালনা",
    addCategory = "ক্যাটাগরি যোগ করুন",
    dataBackupExport = "এক্সপোর্ট ও এনক্রিপ্টেড ব্যাকআপ",
    exportCsv = "CSV এক্সপোর্ট করুন",
    exportPdf = "PDF স্টেটমেন্ট এক্সপোর্ট",
    encryptedBackup = "এনক্রিপ্টেড ব্যাকআপ তৈরি",
    restoreBackup = "এনক্রিপ্টেড ব্যাকআপ পুনরুদ্ধার",
    clearAllData = "সব ডেটা মুছে ফেলুন",
    aboutLicense = "অ্যাপ তথ্য ও ওপেন সোর্স লাইসেন্স",
    unlockTitle = "অ্যাপ লক করা আছে",
    unlockSubtitle = "আপনার আর্থিক তথ্য দেখতে বায়োমেট্রিক বা ডিভাইস পিন দিয়ে আনলক করুন।",
    unlockButton = "আনলক করুন",
    passphraseTitle = "ব্যাকআপ পাসওয়ার্ড",
    passphraseHint = "পাসওয়ার্ড লিখুন (কমপক্ষে ৪ অক্ষর)",
    confirmAction = "নিশ্চিত করুন",
    exportMonthlyCsv = "মাসিক CSV এক্সপোর্ট",
    selectedMonth = "নির্বাচিত মাস",
    allMonths = "সব মাস",
    noTransactionsInMonth = "এই মাসে কোনো লেনদেন রেকর্ড করা হয়নি।",
    aboutDeveloper = "ডেভেলপার পরিচিতি",
    contactDeveloper = "ডেভেলপারের সাথে যোগাযোগ",
    appVersionLabel = "অ্যাপ সংস্করণ",
    openSourceLicenses = "তৃতীয়-পক্ষের ওপেন সোর্স লাইসেন্স",
    noEmailAppError = "এই ডিভাইসে কোনো ইমেইল অ্যাপ পাওয়া যায়নি। অনুগ্রহ করে foysal24x7@gmail.com ঠিকানায় ইমেইল পাঠান।",
    nextScheduled = "পরবর্তী স্বয়ংক্রিয় এন্ট্রি",
    budgetAlertHeader = "৮০% বাজেট সীমা সতর্কতা",
    approachingLimit = "৮০%+ পূর্ণ হয়েছে",
    limitExceeded = "সীমা অতিক্রম করেছে",
    categorySpendingCap = "ক্যাটাগরি খরচের সীমা",
    updatesExistingCap = "এই মাসের জন্য একটি সীমা সেট করা আছে; সংরক্ষণ করলে তা আপডেট হবে।",
    themeSystem = "সিস্টেম ডিফল্ট",
    themeLight = "লাইট",
    themeDark = "ডার্ক",
    themeAmoled = "AMOLED",
    accentColor = "অ্যাকসেন্ট কালার",
    monthlyExpenseSummary = "মাসিক খরচের সারসংক্ষেপ",
    spendingByCategory = "খাতভিত্তিক খরচের প্রবণতা",
    topSpendingCategory = "সর্বোচ্চ খাত",
    otherCategory = "অন্যান্য",
    noMonthlyExpensesTitle = "এই মাসে কোনো খরচ নেই",
    noMonthlyExpensesSubtitle = "এই মাসের খাতভিত্তিক খরচের বার চার্ট দেখতে একটি খরচ যোগ করুন।",
    selectSpecificDate = "নির্দিষ্ট তারিখ নির্বাচন",
    selectDateRange = "তারিখের পরিসর নির্বাচন",
    clearSelection = "নির্বাচন মুছুন",
    returnToMonthlyView = "মাসিক ভিউতে ফিরুন",
    returnToPeriodViewTemplate = "{view_mode} ভিউতে ফিরুন",
    customFilterActive = "কাস্টম ফিল্টার সক্রিয়",
    invalidDateRange = "শেষ তারিখ শুরুর তারিখের আগে হতে পারবে না।",
    previousPeriod = "আগের সময়কাল",
    nextPeriod = "পরবর্তী সময়কাল",
    selectPeriodShortcut = "সময়কাল ও তারিখ নির্বাচন",
    futurePeriodNote = "ভবিষ্যৎ সময়কাল নির্বাচিত — শুধুমাত্র নির্ধারিত বা ভবিষ্যতের এন্ট্রি দেখানো হচ্ছে।",
    mixedCurrencyNote = "এই সময়কালে একাধিক মুদ্রা রেকর্ড করা হয়েছে (রূপান্তর ছাড়াই মুদ্রা অনুযায়ী মোট দেখানো হচ্ছে)।",
    previousMonth = "আগের মাস",
    nextMonth = "পরবর্তী মাস",
    budgetsExceededBannerTemplate = "এই মাসে {count}টি বাজেট সীমা অতিক্রম করেছে।",
    budgetsWarningBannerTemplate = "{count}টি বাজেট ব্যয়ের সীমার কাছাকাছি পৌঁছেছে।",
    viewModeSettings = "ভিউ মোড",
    appearanceSettings = "অ্যাপিয়ারেন্স",
    languageSettings = "ভাষা",
    currencySettings = "মুদ্রা",
    searchLanguages = "ভাষা খুঁজুন...",
    searchCurrencies = "মুদ্রার কোড, নাম বা দেশ খুঁজুন...",
    noMatchingLanguages = "কোনো মিল থাকা ভাষা পাওয়া যায়নি",
    noMatchingCurrencies = "কোনো মিল থাকা মুদ্রা পাওয়া যায়নি",
    selectedLabel = "নির্বাচিত",
    appearanceThemeSection = "থিম",
    biometricNotConfigured = "এই ডিভাইসে বায়োমেট্রিক বা স্ক্রিন লক সেট করা নেই।",
    reminderTime = "রিমাইন্ডারের সময়",
    tapToChangeTime = "রিমাইন্ডারের সময় বদলাতে ট্যাপ করুন",
    notificationPermissionRequired = "রিমাইন্ডারের জন্য নোটিফিকেশন অনুমতি প্রয়োজন।",
    categoryNamePrimary = "নাম (ইংরেজি)",
    categoryNameSecondary = "স্থানীয় নাম (ঐচ্ছিক)",
    categoriesCountTemplate = "{count}টি ক্যাটাগরি",
    clearAllDataConfirm = "এটি সব লেনদেন, বাজেট এবং কাস্টম অ্যাকাউন্ট মুছে ফেলবে এবং শূন্য ব্যালেন্সসহ ডিফল্ট অ্যাকাউন্ট ফিরিয়ে আনবে।",
    aboutLicenseSummary = "Apache License 2.0 • ওপেন সোর্স কম্পোনেন্ট",
    aboutLicenseDetail = "Apache License 2.0-এর অধীনে লাইসেন্সকৃত। AES-256-GCM এনক্রিপ্টেড ব্যাকআপসহ Room-এর মাধ্যমে অফলাইন-প্রথম সংরক্ষণ।",
    encryptedLabel = "এনক্রিপ্টেড",
    copyrightLabel = "কপিরাইট",
    ok = "ঠিক আছে",
    dailyReminderTitle = "Daily Expenses রিমাইন্ডার",
    dailyReminderBody = "আজকের খরচগুলো লিখে রাখতে ভুলবেন না।",
    budgetMonthlyDefault = "মাসিক বাজেট",
    budgetExceededNotifTitleTemplate = "বাজেট অতিক্রম করেছে: {cat}",
    budgetWarningNotifTitleTemplate = "বাজেট সতর্কতা: {cat}",
    budgetNotifBodyTemplate = "সীমা {limit}-এর মধ্যে {spent} খরচ হয়েছে ({pct}%)",
    restoreBackupPreviewTemplate = "{date} (v{version})-এর ব্যাকআপে {accounts}টি অ্যাকাউন্ট, {categories}টি ক্যাটাগরি, {transactions}টি লেনদেন এবং {budgets}টি বাজেট রয়েছে। বর্তমান ডেটা প্রতিস্থাপনের আগে একটি সুরক্ষা ব্যাকআপ তৈরি করা হবে।",
    transactionsCountSingleTemplate = "{count}টি লেনদেন",
    transactionsCountPluralTemplate = "{count}টি লেনদেন",
    budgetAlertSingleTemplate = "{count}টি বাজেট মাসিক সীমার ৮০% বা তার বেশি পৌঁছেছে",
    budgetAlertPluralTemplate = "{count}টি বাজেট মাসিক সীমার ৮০% বা তার বেশি পৌঁছেছে",
    sortNewest = "নতুনতম",
    sortOldest = "পুরাতনতম",
    sortHighest = "সর্বোচ্চ পরিমাণ",
    sortLowest = "সর্বনিম্ন পরিমাণ",
    errInvalidAmount = "অনুগ্রহ করে ০-এর বেশি একটি বৈধ পরিমাণ লিখুন।",
    errSelectAccount = "অনুগ্রহ করে আগে একটি অ্যাকাউন্ট তৈরি বা নির্বাচন করুন।",
    errDifferentToAccount = "ট্রান্সফারের জন্য ভিন্ন একটি গন্তব্য অ্যাকাউন্ট নির্বাচন করুন।",
    errSaveTxFailed = "লেনদেন সংরক্ষণ করা যায়নি।",
    enumLabels = EnumLabelStrings(
        accountTypeCash = "নগদ",
        accountTypeBank = "ব্যাংক অ্যাকাউন্ট",
        accountTypeSavings = "সঞ্চয়",
        accountTypeCreditCard = "ক্রেডিট কার্ড",
        accountTypeMobileBanking = "মোবাইল ব্যাংকিং",
        accountTypeCustom = "কাস্টম",
        recurrenceNone = "একবার",
        recurrenceDaily = "প্রতিদিন",
        recurrenceWeekly = "সাপ্তাহিক",
        recurrenceMonthly = "মাসিক",
        recurrenceYearly = "বার্ষিক",
        accentEmerald = "এমেরাল্ড",
        accentOceanBlue = "ওশান ব্লু",
        accentIndigo = "ইন্ডিগো",
        accentViolet = "ভায়োলেট",
        accentCoralRed = "কোরাল রেড",
        accentAmberGold = "অ্যাম্বার গোল্ড",
        accentTealCyan = "টিল সায়ান",
        accentRosePink = "রোজ পিংক"
    ),
    categoryLabels = CategoryLabelStrings(
        catFoodDining = "খাবার ও রেস্টুরেন্ট",
        catGroceries = "বাজার ও মুদিখানা",
        catTransport = "যাতায়াত",
        catHouseRent = "বাড়ি ভাড়া",
        catUtilitiesBills = "বিদ্যুৎ ও বিল",
        catMobileInternet = "মোবাইল ও ইন্টারনেট",
        catHealthcare = "চিকিৎসা ও ওষুধ",
        catShopping = "কেনাকাটা",
        catEducation = "শিক্ষা",
        catEntertainment = "বিনোদন",
        catTravel = "ভ্রমণ",
        catOtherExpense = "অন্যান্য খরচ",
        catSalary = "বেতন",
        catFreelance = "ফ্রিল্যান্সিং",
        catBusiness = "ব্যবসা",
        catInvestment = "বিনিয়োগ",
        catGiftBonus = "উপহার ও বোনাস",
        catOtherIncome = "অন্যান্য আয়"
    ),
    exportBackup = ExportBackupStrings(
        developerRole = "অ্যাপ ডেভেলপার",
        developerCopyrightText = "কপিরাইট © ২০২৬ ফয়সাল আহমেদ সাদিন।",
        numberFormatLatinLabel = "ইংরেজি (123)",
        numberFormatBanglaLabel = "বাংলা (১২৩)",
        rtlLabel = "RTL",
        exportCsvMonthSuccessTemplate = "{month}-এর {count}টি লেনদেন CSV-তে এক্সপোর্ট করা হয়েছে।",
        exportCsvAllSuccessTemplate = "{count}টি লেনদেন CSV-তে এক্সপোর্ট করা হয়েছে।",
        exportCsvFailed = "CSV এক্সপোর্ট ব্যর্থ হয়েছে।",
        exportPdfSuccessTemplate = "PDF স্টেটমেন্ট এক্সপোর্ট করা হয়েছে ({count}টি লেনদেন)।",
        exportPdfFailed = "PDF এক্সপোর্ট ব্যর্থ হয়েছে।",
        backupSavedStatusTemplate = "এনক্রিপ্টেড ব্যাকআপ সংরক্ষিত হয়েছে ({transactions}টি লেনদেন, {accounts}টি অ্যাকাউন্ট)।",
        backupEncryptionFailed = "ব্যাকআপ এনক্রিপ্ট করা যায়নি।",
        backupDecryptFailed = "ব্যাকআপ ডিক্রিপ্ট করা যায়নি। পাসওয়ার্ড বা ফাইল যাচাই করুন।",
        restoreSuccessStatusTemplate = "{accounts}টি অ্যাকাউন্ট ও {transactions}টি লেনদেন পুনরুদ্ধার করা হয়েছে।",
        restoreFailedStatus = "পুনরুদ্ধার ব্যর্থ হয়েছে।",
        clearAllDataDone = "সব আর্থিক ডেটা রিসেট করা হয়েছে।",
        pdfStatementTitle = "Daily Expenses — আর্থিক বিবরণী",
        pdfGeneratedMetaTemplate = "তৈরির তারিখ: {date}  |  মুদ্রা: {currency}",
        pdfAccountsOverview = "অ্যাকাউন্টের সারসংক্ষেপ",
        pdfTransactionsCountTemplate = "লেনদেন ({count})",
        pdfColDate = "তারিখ",
        pdfColDetails = "ক্যাটাগরি / বিবরণ",
        pdfColAccount = "অ্যাকাউন্ট",
        pdfColAmount = "পরিমাণ",
        csvHeaderRow = "তারিখ,সময়,ধরন,ক্যাটাগরি,অ্যাকাউন্ট,গন্তব্য অ্যাকাউন্ট,পরিমাণ,মুদ্রা,নোট,পুনরাবৃত্তি"
    )
)

fun stringsFor(language: AppLanguage): AppStrings =
    if (language == AppLanguage.BANGLA) BanglaStrings else EnglishStrings

fun stringsFor(context: Context, languageTag: String): AppStrings {
    val normalized = SupportedLanguageCatalog.normalizeTag(languageTag)
    val locale = SupportedLanguageCatalog.localeForTag(normalized)
    val config = Configuration(context.resources.configuration).apply {
        setLocale(locale)
    }
    val res = context.createConfigurationContext(config).resources
    fun s(id: Int): String = res.getString(id)
    return AppStrings(
        appName = s(R.string.app_name),
        appSubtitle = s(R.string.app_subtitle),
        navHome = s(R.string.nav_home),
        navTransactions = s(R.string.nav_transactions),
        navAnalytics = s(R.string.nav_analytics),
        navBudgets = s(R.string.nav_budgets),
        navSettings = s(R.string.nav_settings),
        totalBalance = s(R.string.total_balance),
        totalIncome = s(R.string.total_income),
        totalExpenses = s(R.string.total_expenses),
        monthlySavings = s(R.string.monthly_savings),
        netCashFlow = s(R.string.net_cash_flow),
        accounts = s(R.string.accounts),
        manage = s(R.string.manage),
        spendingOverview = s(R.string.spending_overview),
        incomeVsExpense = s(R.string.income_vs_expense),
        categoryBreakdown = s(R.string.category_breakdown),
        recentTransactions = s(R.string.recent_transactions),
        budgetProgress = s(R.string.budget_progress),
        seeAll = s(R.string.see_all),
        today = s(R.string.today),
        yesterday = s(R.string.yesterday),
        thisWeek = s(R.string.this_week),
        thisMonth = s(R.string.this_month),
        thisYear = s(R.string.this_year),
        customRange = s(R.string.custom_range),
        addTransaction = s(R.string.add_transaction),
        editTransaction = s(R.string.edit_transaction),
        transactionDetails = s(R.string.transaction_details),
        expense = s(R.string.expense),
        income = s(R.string.income),
        transfer = s(R.string.transfer),
        all = s(R.string.all),
        amount = s(R.string.amount),
        category = s(R.string.category),
        account = s(R.string.account),
        fromAccount = s(R.string.from_account),
        toAccount = s(R.string.to_account),
        dateTime = s(R.string.date_time),
        noteOptional = s(R.string.note_optional),
        recurring = s(R.string.recurring),
        receiptAttachment = s(R.string.receipt_attachment),
        attachReceipt = s(R.string.attach_receipt),
        removeReceipt = s(R.string.remove_receipt),
        save = s(R.string.save),
        cancel = s(R.string.cancel),
        delete = s(R.string.delete),
        edit = s(R.string.edit),
        searchTransactions = s(R.string.search_transactions),
        filters = s(R.string.filters),
        reset = s(R.string.reset),
        apply = s(R.string.apply),
        recurringOnly = s(R.string.recurring_only),
        noTransactionsTitle = s(R.string.no_transactions_title),
        noTransactionsSubtitle = s(R.string.no_transactions_subtitle),
        addAccount = s(R.string.add_account),
        editAccount = s(R.string.edit_account),
        accountName = s(R.string.account_name),
        accountType = s(R.string.account_type),
        openingBalance = s(R.string.opening_balance),
        quickTransfer = s(R.string.quick_transfer),
        analytics = s(R.string.analytics),
        daily = s(R.string.daily),
        weekly = s(R.string.weekly),
        monthly = s(R.string.monthly),
        yearly = s(R.string.yearly),
        savingsRate = s(R.string.savings_rate),
        avgDailySpend = s(R.string.avg_daily_spend),
        vsLastMonth = s(R.string.vs_last_month),
        spendingTrends = s(R.string.spending_trends),
        budgets = s(R.string.budgets),
        addBudget = s(R.string.add_budget),
        editBudget = s(R.string.edit_budget),
        monthlyLimit = s(R.string.monthly_limit),
        overallMonthlyBudget = s(R.string.overall_monthly_budget),
        spent = s(R.string.spent),
        remaining = s(R.string.remaining),
        overspentBy = s(R.string.overspent_by),
        alertThreshold = s(R.string.alert_threshold),
        noBudgetsTitle = s(R.string.no_budgets_title),
        noBudgetsSubtitle = s(R.string.no_budgets_subtitle),
        settings = s(R.string.settings),
        preferences = s(R.string.preferences),
        viewMode = s(R.string.view_mode),
        localizationDisplay = s(R.string.localization_display),
        language = s(R.string.language),
        currency = s(R.string.currency),
        numberFormat = s(R.string.number_format),
        theme = s(R.string.appearance),
        hideBalances = s(R.string.hide_balances),
        securityPrivacy = s(R.string.security_privacy),
        biometricLock = s(R.string.biometric_lock),
        notificationsReminders = s(R.string.notifications_reminders),
        dailyReminder = s(R.string.daily_reminder),
        budgetAlerts = s(R.string.budget_alerts),
        categories = s(R.string.categories),
        manageCategories = s(R.string.manage_categories),
        addCategory = s(R.string.add_category),
        dataBackupExport = s(R.string.data_backup_export),
        exportCsv = s(R.string.export_csv),
        exportPdf = s(R.string.export_pdf),
        encryptedBackup = s(R.string.encrypted_backup),
        restoreBackup = s(R.string.restore_backup),
        clearAllData = s(R.string.clear_all_data),
        aboutLicense = s(R.string.about_license),
        unlockTitle = s(R.string.unlock_title),
        unlockSubtitle = s(R.string.unlock_subtitle),
        unlockButton = s(R.string.unlock_button),
        passphraseTitle = s(R.string.passphrase_title),
        passphraseHint = s(R.string.passphrase_hint),
        confirmAction = s(R.string.confirm_action),
        exportMonthlyCsv = s(R.string.export_monthly_csv),
        selectedMonth = s(R.string.selected_month),
        allMonths = s(R.string.all_months),
        noTransactionsInMonth = s(R.string.no_transactions_in_month),
        aboutDeveloper = s(R.string.about_developer),
        contactDeveloper = s(R.string.contact_developer),
        appVersionLabel = s(R.string.app_version_label),
        openSourceLicenses = s(R.string.open_source_licenses),
        noEmailAppError = s(R.string.no_email_app_error),
        nextScheduled = s(R.string.next_scheduled),
        budgetAlertHeader = s(R.string.budget_alert_header),
        approachingLimit = s(R.string.approaching_limit),
        limitExceeded = s(R.string.limit_exceeded),
        categorySpendingCap = s(R.string.category_spending_cap),
        updatesExistingCap = s(R.string.updates_existing_cap),
        themeSystem = s(R.string.theme_system),
        themeLight = s(R.string.theme_light),
        themeDark = s(R.string.theme_dark),
        themeAmoled = s(R.string.theme_amoled),
        accentColor = s(R.string.accent_color),
        monthlyExpenseSummary = s(R.string.monthly_expense_summary),
        spendingByCategory = s(R.string.spending_by_category),
        topSpendingCategory = s(R.string.top_spending_category),
        otherCategory = s(R.string.other_category),
        noMonthlyExpensesTitle = s(R.string.no_monthly_expenses_title),
        noMonthlyExpensesSubtitle = s(R.string.no_monthly_expenses_subtitle),
        selectSpecificDate = s(R.string.select_specific_date),
        selectDateRange = s(R.string.select_date_range),
        clearSelection = s(R.string.clear_selection),
        returnToMonthlyView = s(R.string.return_to_monthly_view),
        returnToPeriodViewTemplate = s(R.string.return_to_period_view),
        customFilterActive = s(R.string.custom_filter_active),
        invalidDateRange = s(R.string.invalid_date_range),
        previousPeriod = s(R.string.previous_period),
        nextPeriod = s(R.string.next_period),
        selectPeriodShortcut = s(R.string.select_period_shortcut),
        futurePeriodNote = s(R.string.future_period_note),
        mixedCurrencyNote = s(R.string.mixed_currency_note),
        previousMonth = s(R.string.previous_month),
        nextMonth = s(R.string.next_month),
        budgetsExceededBannerTemplate = s(R.string.budgets_exceeded_banner),
        budgetsWarningBannerTemplate = s(R.string.budgets_warning_banner),
        viewModeSettings = s(R.string.view_mode_settings),
        appearanceSettings = s(R.string.appearance_settings),
        languageSettings = s(R.string.language_settings),
        currencySettings = s(R.string.currency_settings),
        searchLanguages = s(R.string.search_languages),
        searchCurrencies = s(R.string.search_currencies),
        noMatchingLanguages = s(R.string.no_matching_languages),
        noMatchingCurrencies = s(R.string.no_matching_currencies),
        selectedLabel = s(R.string.selected_label),
        appearanceThemeSection = s(R.string.appearance_theme_section),
        biometricNotConfigured = s(R.string.biometric_not_configured),
        reminderTime = s(R.string.reminder_time),
        tapToChangeTime = s(R.string.tap_to_change_time),
        notificationPermissionRequired = s(R.string.notification_permission_required),
        categoryNamePrimary = s(R.string.category_name_primary),
        categoryNameSecondary = s(R.string.category_name_secondary),
        categoriesCountTemplate = s(R.string.categories_count_fmt),
        clearAllDataConfirm = s(R.string.clear_all_data_confirm),
        aboutLicenseSummary = s(R.string.about_license_summary),
        aboutLicenseDetail = s(R.string.about_license_detail),
        encryptedLabel = s(R.string.encrypted_label),
        copyrightLabel = s(R.string.copyright_label),
        ok = s(R.string.ok),
        dailyReminderTitle = s(R.string.daily_reminder_title),
        dailyReminderBody = s(R.string.daily_reminder_body),
        budgetMonthlyDefault = s(R.string.budget_monthly_default),
        budgetExceededNotifTitleTemplate = s(R.string.budget_exceeded_notif_title),
        budgetWarningNotifTitleTemplate = s(R.string.budget_warning_notif_title),
        budgetNotifBodyTemplate = s(R.string.budget_notif_body),
        restoreBackupPreviewTemplate = s(R.string.restore_backup_preview),
        transactionsCountSingleTemplate = s(R.string.transactions_count_single),
        transactionsCountPluralTemplate = s(R.string.transactions_count_plural),
        budgetAlertSingleTemplate = s(R.string.budget_alert_single),
        budgetAlertPluralTemplate = s(R.string.budget_alert_plural),
        sortNewest = s(R.string.sort_newest),
        sortOldest = s(R.string.sort_oldest),
        sortHighest = s(R.string.sort_highest),
        sortLowest = s(R.string.sort_lowest),
        errInvalidAmount = s(R.string.err_invalid_amount),
        errSelectAccount = s(R.string.err_select_account),
        errDifferentToAccount = s(R.string.err_different_to_account),
        errSaveTxFailed = s(R.string.err_save_tx_failed),
        enumLabels = EnumLabelStrings(
            accountTypeCash = s(R.string.account_type_cash),
            accountTypeBank = s(R.string.account_type_bank),
            accountTypeSavings = s(R.string.account_type_savings),
            accountTypeCreditCard = s(R.string.account_type_credit_card),
            accountTypeMobileBanking = s(R.string.account_type_mobile_banking),
            accountTypeCustom = s(R.string.account_type_custom),
            recurrenceNone = s(R.string.recurrence_none),
            recurrenceDaily = s(R.string.recurrence_daily),
            recurrenceWeekly = s(R.string.recurrence_weekly),
            recurrenceMonthly = s(R.string.recurrence_monthly),
            recurrenceYearly = s(R.string.recurrence_yearly),
            accentEmerald = s(R.string.accent_emerald),
            accentOceanBlue = s(R.string.accent_ocean_blue),
            accentIndigo = s(R.string.accent_indigo),
            accentViolet = s(R.string.accent_violet),
            accentCoralRed = s(R.string.accent_coral_red),
            accentAmberGold = s(R.string.accent_amber_gold),
            accentTealCyan = s(R.string.accent_teal_cyan),
            accentRosePink = s(R.string.accent_rose_pink)
        ),
        categoryLabels = CategoryLabelStrings(
            catFoodDining = s(R.string.cat_food_dining),
            catGroceries = s(R.string.cat_groceries),
            catTransport = s(R.string.cat_transport),
            catHouseRent = s(R.string.cat_house_rent),
            catUtilitiesBills = s(R.string.cat_utilities_bills),
            catMobileInternet = s(R.string.cat_mobile_internet),
            catHealthcare = s(R.string.cat_healthcare),
            catShopping = s(R.string.cat_shopping),
            catEducation = s(R.string.cat_education),
            catEntertainment = s(R.string.cat_entertainment),
            catTravel = s(R.string.cat_travel),
            catOtherExpense = s(R.string.cat_other_expense),
            catSalary = s(R.string.cat_salary),
            catFreelance = s(R.string.cat_freelance),
            catBusiness = s(R.string.cat_business),
            catInvestment = s(R.string.cat_investment),
            catGiftBonus = s(R.string.cat_gift_bonus),
            catOtherIncome = s(R.string.cat_other_income)
        ),
        exportBackup = ExportBackupStrings(
            developerRole = s(R.string.developer_role),
            developerCopyrightText = s(R.string.developer_copyright_text),
            numberFormatLatinLabel = s(R.string.number_format_latin_label),
            numberFormatBanglaLabel = s(R.string.number_format_bangla_label),
            rtlLabel = s(R.string.rtl_label),
            exportCsvMonthSuccessTemplate = s(R.string.export_csv_month_success),
            exportCsvAllSuccessTemplate = s(R.string.export_csv_all_success),
            exportCsvFailed = s(R.string.export_csv_failed),
            exportPdfSuccessTemplate = s(R.string.export_pdf_success),
            exportPdfFailed = s(R.string.export_pdf_failed),
            backupSavedStatusTemplate = s(R.string.backup_saved_status),
            backupEncryptionFailed = s(R.string.backup_encryption_failed),
            backupDecryptFailed = s(R.string.backup_decrypt_failed),
            restoreSuccessStatusTemplate = s(R.string.restore_success_status),
            restoreFailedStatus = s(R.string.restore_failed_status),
            clearAllDataDone = s(R.string.clear_all_data_done),
            pdfStatementTitle = s(R.string.pdf_statement_title),
            pdfGeneratedMetaTemplate = s(R.string.pdf_generated_meta),
            pdfAccountsOverview = s(R.string.pdf_accounts_overview),
            pdfTransactionsCountTemplate = s(R.string.pdf_transactions_count),
            pdfColDate = s(R.string.pdf_col_date),
            pdfColDetails = s(R.string.pdf_col_details),
            pdfColAccount = s(R.string.pdf_col_account),
            pdfColAmount = s(R.string.pdf_col_amount),
            csvHeaderRow = s(R.string.csv_header_row)
        )
    )
}

val LocalAppStrings = staticCompositionLocalOf { EnglishStrings }
