/*
 * Copyright 2026 Expense Manager Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.core.model

import com.example.core.common.AppStrings
import kotlinx.serialization.Serializable
import java.util.Currency
import java.util.Locale

@Serializable
enum class TransactionType {
    EXPENSE,
    INCOME,
    TRANSFER
}

@Serializable
enum class AccountType {
    CASH,
    BANK,
    SAVINGS,
    CREDIT_CARD,
    MOBILE_BANKING,
    CUSTOM
}

@Serializable
enum class RecurrenceFrequency {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}

@Serializable
enum class ViewMode {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}

enum class DateFilterPeriod {
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    THIS_YEAR,
    CUSTOM
}

enum class CustomDatePreset {
    SPECIFIC_DATE,
    DATE_RANGE,
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    THIS_YEAR
}

enum class AnalyticsGranularity {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}

enum class TransactionSortOrder {
    NEWEST,
    OLDEST,
    HIGHEST_AMOUNT,
    LOWEST_AMOUNT
}

@Serializable
enum class AppLanguage(val code: String, val nativeName: String) {
    ENGLISH("en", "English"),
    BANGLA("bn", "বাংলা")
}

@Serializable
data class SupportedLanguage(
    val tag: String,
    val nativeName: String,
    val englishName: String,
    val isRtl: Boolean = false
)

object SupportedLanguageCatalog {
    val all: List<SupportedLanguage> = listOf(
        SupportedLanguage("en", "English", "English", isRtl = false),
        SupportedLanguage("bn", "বাংলা", "Bengali", isRtl = false),
        SupportedLanguage("hi", "हिन्दी", "Hindi", isRtl = false),
        SupportedLanguage("ur", "اردو", "Urdu", isRtl = true),
        SupportedLanguage("ar", "العربية", "Arabic", isRtl = true),
        SupportedLanguage("es", "Español", "Spanish", isRtl = false),
        SupportedLanguage("fr", "Français", "French", isRtl = false),
        SupportedLanguage("pt", "Português", "Portuguese", isRtl = false),
        SupportedLanguage("de", "Deutsch", "German", isRtl = false),
        SupportedLanguage("ru", "Русский", "Russian", isRtl = false),
        SupportedLanguage("zh-CN", "中文（简体）", "Chinese (Simplified)", isRtl = false),
        SupportedLanguage("zh-TW", "中文（繁體）", "Chinese (Traditional)", isRtl = false),
        SupportedLanguage("ja", "日本語", "Japanese", isRtl = false),
        SupportedLanguage("ko", "한국어", "Korean", isRtl = false),
        SupportedLanguage("id", "Bahasa Indonesia", "Indonesian", isRtl = false),
        SupportedLanguage("tr", "Türkçe", "Turkish", isRtl = false),
        SupportedLanguage("it", "Italiano", "Italian", isRtl = false),
        SupportedLanguage("vi", "Tiếng Việt", "Vietnamese", isRtl = false),
        SupportedLanguage("th", "ไทย", "Thai", isRtl = false)
    )

    fun normalizeTag(raw: String?): String {
        val cleaned = raw?.trim().orEmpty()
        if (cleaned.isEmpty()) return "en"
        val lower = cleaned.replace('_', '-').lowercase(Locale.US)
        return when {
            lower == "in" || lower.startsWith("in-") || lower == "id" || lower.startsWith("id-") -> "id"
            lower == "zh-cn" || lower == "zh-hans" || lower.startsWith("zh-hans-") -> "zh-CN"
            lower == "zh-tw" || lower == "zh-hk" || lower == "zh-hant" || lower.startsWith("zh-hant-") -> "zh-TW"
            else -> {
                all.firstOrNull { it.tag.equals(cleaned, ignoreCase = true) }?.tag
                    ?: all.firstOrNull { lower.startsWith("${it.tag.lowercase(Locale.US)}-") }?.tag
                    ?: "en"
            }
        }
    }

    fun findByTag(tag: String?): SupportedLanguage {
        val normalized = normalizeTag(tag)
        return all.firstOrNull { it.tag == normalized } ?: all.first()
    }

    fun isRtl(tag: String?): Boolean = findByTag(tag).isRtl

    fun localeForTag(tag: String?): Locale {
        return when (normalizeTag(tag)) {
            "zh-CN" -> Locale.SIMPLIFIED_CHINESE
            "zh-TW" -> Locale.TRADITIONAL_CHINESE
            "id" -> Locale("in")
            else -> Locale.forLanguageTag(normalizeTag(tag))
        }
    }

    fun toAppLanguage(tag: String?): AppLanguage =
        if (normalizeTag(tag) == "bn") AppLanguage.BANGLA else AppLanguage.ENGLISH
}

@Serializable
enum class AppCurrency(
    val code: String,
    val symbol: String,
    val displayName: String,
    val usesSouthAsianGrouping: Boolean
) {
    BDT("BDT", "৳", "Bangladeshi Taka (৳)", true),
    USD("USD", "$", "US Dollar ($)", false),
    EUR("EUR", "€", "Euro (€)", false),
    GBP("GBP", "£", "British Pound (£)", false),
    INR("INR", "₹", "Indian Rupee (₹)", true)
}

@Serializable
data class CurrencyInfo(
    val code: String,
    val name: String,
    val symbol: String,
    val countryOrRegion: String,
    val fractionDigits: Int = 2,
    val usesSouthAsianGrouping: Boolean = false
)

object IsoCurrencyCatalog {
    private val curMetadataOverrides: Map<String, CurrencyInfo> = mapOf(
        "BDT" to CurrencyInfo("BDT", "Bangladeshi Taka", "৳", "Bangladesh", 2, true),
        "USD" to CurrencyInfo("USD", "US Dollar", "$", "United States", 2, false),
        "EUR" to CurrencyInfo("EUR", "Euro", "€", "Eurozone", 2, false),
        "GBP" to CurrencyInfo("GBP", "British Pound", "£", "United Kingdom", 2, false),
        "INR" to CurrencyInfo("INR", "Indian Rupee", "₹", "India", 2, true),
        "PKR" to CurrencyInfo("PKR", "Pakistani Rupee", "₨", "Pakistan", 2, true),
        "NPR" to CurrencyInfo("NPR", "Nepalese Rupee", "रू", "Nepal", 2, true),
        "LKR" to CurrencyInfo("LKR", "Sri Lankan Rupee", "Rs", "Sri Lanka", 2, true),
        "JPY" to CurrencyInfo("JPY", "Japanese Yen", "¥", "Japan", 0, false),
        "CNY" to CurrencyInfo("CNY", "Chinese Yuan", "¥", "China", 2, false),
        "TWD" to CurrencyInfo("TWD", "New Taiwan Dollar", "NT$", "Taiwan", 2, false),
        "KRW" to CurrencyInfo("KRW", "South Korean Won", "₩", "South Korea", 0, false),
        "SAR" to CurrencyInfo("SAR", "Saudi Riyal", "﷼", "Saudi Arabia", 2, false),
        "AED" to CurrencyInfo("AED", "UAE Dirham", "د.إ", "United Arab Emirates", 2, false),
        "KWD" to CurrencyInfo("KWD", "Kuwaiti Dinar", "د.ك", "Kuwait", 3, false),
        "BHD" to CurrencyInfo("BHD", "Bahraini Dinar", "د.ب", "Bahrain", 3, false),
        "OMR" to CurrencyInfo("OMR", "Omani Rial", "ر.ع.", "Oman", 3, false),
        "QAR" to CurrencyInfo("QAR", "Qatari Riyal", "ر.ق", "Qatar", 2, false),
        "AUD" to CurrencyInfo("AUD", "Australian Dollar", "A$", "Australia", 2, false),
        "CAD" to CurrencyInfo("CAD", "Canadian Dollar", "CA$", "Canada", 2, false),
        "CHF" to CurrencyInfo("CHF", "Swiss Franc", "CHF", "Switzerland", 2, false),
        "SGD" to CurrencyInfo("SGD", "Singapore Dollar", "S$", "Singapore", 2, false),
        "MYR" to CurrencyInfo("MYR", "Malaysian Ringgit", "RM", "Malaysia", 2, false),
        "IDR" to CurrencyInfo("IDR", "Indonesian Rupiah", "Rp", "Indonesia", 0, false),
        "THB" to CurrencyInfo("THB", "Thai Baht", "฿", "Thailand", 2, false),
        "VND" to CurrencyInfo("VND", "Vietnamese Dong", "₫", "Vietnam", 0, false),
        "TRY" to CurrencyInfo("TRY", "Turkish Lira", "₺", "Türkiye", 2, false),
        "RUB" to CurrencyInfo("RUB", "Russian Ruble", "₽", "Russia", 2, false),
        "BRL" to CurrencyInfo("BRL", "Brazilian Real", "R$", "Brazil", 2, false),
        "MXN" to CurrencyInfo("MXN", "Mexican Peso", "MX$", "Mexico", 2, false),
        "PHP" to CurrencyInfo("PHP", "Philippine Peso", "₱", "Philippines", 2, false),
        "ZAR" to CurrencyInfo("ZAR", "South African Rand", "R", "South Africa", 2, false),
        "NGN" to CurrencyInfo("NGN", "Nigerian Naira", "₦", "Nigeria", 2, false),
        "EGP" to CurrencyInfo("EGP", "Egyptian Pound", "E£", "Egypt", 2, false),
        "PLN" to CurrencyInfo("PLN", "Polish Zloty", "zł", "Poland", 2, false),
        "SEK" to CurrencyInfo("SEK", "Swedish Krona", "kr", "Sweden", 2, false),
        "NOK" to CurrencyInfo("NOK", "Norwegian Krone", "kr", "Norway", 2, false),
        "DKK" to CurrencyInfo("DKK", "Danish Krone", "kr", "Denmark", 2, false),
        "NZD" to CurrencyInfo("NZD", "New Zealand Dollar", "NZ$", "New Zealand", 2, false),
        "HKD" to CurrencyInfo("HKD", "Hong Kong Dollar", "HK$", "Hong Kong", 2, false),
        "ILS" to CurrencyInfo("ILS", "Israeli New Shekel", "₪", "Israel", 2, false),
        "UAH" to CurrencyInfo("UAH", "Ukrainian Hryvnia", "₴", "Ukraine", 2, false)
    )

    private val excludedCodes = setOf(
        "XAU", "XAG", "XPT", "XPD", "XTS", "XXX", "XBA", "XBB", "XBC", "XBD",
        "XSU", "XUA", "XDR", "USN", "USS", "UYW", "CHW", "CHE", "COU", "MXV",
        "CLF", "BOV", "UYI"
    )

    private val southAsianCodes = setOf("BDT", "INR", "PKR", "NPR", "LKR")

    val allCurrencies: List<CurrencyInfo> by lazy {
        val locales = Locale.getAvailableLocales()
        val regionByCurrency = mutableMapOf<String, MutableSet<String>>()
        val symbolLocaleByCurrency = mutableMapOf<String, Locale>()
        for (locale in locales) {
            if (locale.country.isNullOrBlank()) continue
            val curr = runCatching { Currency.getInstance(locale) }.getOrNull() ?: continue
            val code = curr.currencyCode
            if (locale.displayCountry.isNotBlank()) {
                regionByCurrency.getOrPut(code) { linkedSetOf() }.add(locale.getDisplayCountry(Locale.ENGLISH))
            }
            symbolLocaleByCurrency.putIfAbsent(code, locale)
        }

        val collected = linkedMapOf<String, CurrencyInfo>()
        curMetadataOverrides.forEach { (code, info) ->
            collected[code] = info
        }

        val available = runCatching { Currency.getAvailableCurrencies() }.getOrElse { emptySet() }
        for (curr in available) {
            val code = curr.currencyCode
            if (code in collected) continue
            if (code.length != 3 || code in excludedCodes) continue
            if (curr.defaultFractionDigits < 0) continue

            val name = curr.getDisplayName(Locale.ENGLISH).ifBlank { code }
            // Skip clearly historical/replaced currencies tagged with year spans like "(1994–2005)"
            if (Regex("\\(\\d{4}[–-]\\d{4}\\)").containsMatchIn(name) || name.startsWith("Old ", ignoreCase = true)) {
                continue
            }

            val symbolLocale = symbolLocaleByCurrency[code] ?: Locale.US
            val symbol = curr.getSymbol(symbolLocale).ifBlank { code }
            val regions = regionByCurrency[code]?.take(3)?.joinToString(", ").orEmpty()
                .ifBlank { code }
            val digits = curr.defaultFractionDigits.coerceIn(0, 3)

            collected[code] = CurrencyInfo(
                code = code,
                name = name,
                symbol = symbol,
                countryOrRegion = regions,
                fractionDigits = digits,
                usesSouthAsianGrouping = code in southAsianCodes
            )
        }

        collected.values.sortedBy { it.name.lowercase(Locale.US) }
    }

    fun resolve(code: String?): CurrencyInfo {
        val normalized = code?.trim()?.uppercase(Locale.US).orEmpty()
        if (normalized.isEmpty()) return curMetadataOverrides.getValue("BDT")
        return curMetadataOverrides[normalized]
            ?: allCurrencies.firstOrNull { it.code == normalized }
            ?: runCatching {
                val curr = Currency.getInstance(normalized)
                CurrencyInfo(
                    code = curr.currencyCode,
                    name = curr.getDisplayName(Locale.ENGLISH).ifBlank { normalized },
                    symbol = curr.getSymbol(Locale.US).ifBlank { normalized },
                    countryOrRegion = normalized,
                    fractionDigits = curr.defaultFractionDigits.coerceIn(0, 3),
                    usesSouthAsianGrouping = normalized in southAsianCodes
                )
            }.getOrNull()
            ?: curMetadataOverrides.getValue("BDT")
    }

    fun fromAppCurrency(appCurrency: AppCurrency): CurrencyInfo = resolve(appCurrency.code)

    fun toAppCurrencyOrNull(code: String?): AppCurrency? =
        AppCurrency.entries.firstOrNull { it.code.equals(code?.trim(), ignoreCase = true) }

    fun search(query: String): List<CurrencyInfo> {
        val q = query.trim().lowercase(Locale.US)
        if (q.isEmpty()) return allCurrencies
        return allCurrencies.filter {
            it.code.lowercase(Locale.US).contains(q) ||
                it.name.lowercase(Locale.US).contains(q) ||
                it.symbol.lowercase(Locale.US).contains(q) ||
                it.countryOrRegion.lowercase(Locale.US).contains(q)
        }
    }
}

@Serializable
enum class NumberFormatStyle {
    ENGLISH,
    BANGLA
}

@Serializable
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    AMOLED
}

@Serializable
enum class AppAccentColor(
    val primaryHex: Long,
    val accentHex: Long,
    val labelEn: String,
    val labelBn: String
) {
    EMERALD(0xFF34C759, 0xFF00FF88, "Emerald", "এমেরাল্ড"),
    OCEAN_BLUE(0xFF0A84FF, 0xFF5AC8FA, "Ocean Blue", "ওশান ব্লু"),
    INDIGO(0xFF5856D6, 0xFF918FFB, "Indigo", "ইন্ডিগো"),
    VIOLET(0xFFAF52DE, 0xFFD98BFF, "Violet", "ভায়োলেট"),
    CORAL_RED(0xFFFF3B30, 0xFFFF6961, "Coral Red", "কোরাল রেড"),
    AMBER_GOLD(0xFFFF9F0A, 0xFFFFD60A, "Amber Gold", "অ্যাম্বার গোল্ড"),
    TEAL_CYAN(0xFF00B8B0, 0xFF5CE1E6, "Teal Cyan", "টিল সায়ান"),
    ROSE_PINK(0xFFFF2D55, 0xFFFF6482, "Rose Pink", "রোজ পিংক")
}

@Serializable
data class Account(
    val id: Long = 0L,
    val name: String,
    val type: AccountType,
    val balance: Double = 0.0,
    val initialBalance: Double = 0.0,
    val colorHex: Long = 0xFF34C759,
    val iconKey: String = "wallet",
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class Category(
    val id: Long = 0L,
    val nameEn: String,
    val nameBn: String,
    val type: TransactionType,
    val iconKey: String,
    val colorHex: Long,
    val isDefault: Boolean = false
) {
    fun localizedName(language: AppLanguage): String =
        if (language == AppLanguage.BANGLA && nameBn.isNotBlank()) nameBn else nameEn

    fun localizedName(languageTag: String, strings: AppStrings? = null): String {
        val norm = SupportedLanguageCatalog.normalizeTag(languageTag)
        if (isDefault && strings != null) {
            return strings.localizedDefaultCategoryName(nameEn) ?: if (norm == "bn" && nameBn.isNotBlank()) nameBn else nameEn
        }
        return if (norm == "bn" && nameBn.isNotBlank()) nameBn else nameEn
    }
}

@Serializable
data class Transaction(
    val id: Long = 0L,
    val type: TransactionType,
    val amount: Double,
    val categoryId: Long?,
    val accountId: Long,
    val toAccountId: Long? = null,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val receiptUri: String? = null,
    val recurrence: RecurrenceFrequency = RecurrenceFrequency.NONE,
    val nextRecurrenceAt: Long? = null,
    val parentRecurringId: Long? = null,
    val currencyCode: String = "BDT"
)

data class TransactionWithDetails(
    val transaction: Transaction,
    val category: Category?,
    val account: Account?,
    val toAccount: Account?
)

@Serializable
data class Budget(
    val id: Long = 0L,
    val categoryId: Long?, // null means overall monthly budget
    val amountLimit: Double,
    val monthYear: String, // "YYYY-MM"
    val alertThresholdPercent: Int = 80,
    val createdAt: Long = System.currentTimeMillis()
)

data class BudgetWithProgress(
    val budget: Budget,
    val category: Category?,
    val spentAmount: Double
) {
    val remainingAmount: Double get() = budget.amountLimit - spentAmount
    val progressRatio: Float
        get() = if (budget.amountLimit <= 0.0) 0f else (spentAmount / budget.amountLimit).toFloat()
    val progressPercent: Int
        get() = if (budget.amountLimit <= 0.0) 0 else kotlin.math.round((spentAmount / budget.amountLimit) * 100.0).toInt()
    val isReachedEightyPercent: Boolean
        get() = budget.amountLimit > 0.0 && ((spentAmount / budget.amountLimit) + 1e-9 >= 0.8)
    val isWarning: Boolean
        get() = budget.amountLimit > 0.0 &&
            ((spentAmount / budget.amountLimit) + 1e-9 >= (budget.alertThresholdPercent / 100.0)) &&
            !isOverspent
    val isOverspent: Boolean
        get() = spentAmount > budget.amountLimit && budget.amountLimit > 0.0
}

data class CategoryBreakdown(
    val category: Category?,
    val totalAmount: Double,
    val percentage: Float,
    val transactionCount: Int
)

data class ChartPoint(
    val label: String,
    val income: Double,
    val expense: Double
)

data class PeriodComparison(
    val currentIncome: Double,
    val previousIncome: Double,
    val currentExpense: Double,
    val previousExpense: Double
) {
    val incomeChangePercent: Double?
        get() = if (previousIncome > 0.0) ((currentIncome - previousIncome) / previousIncome) * 100.0 else null

    val expenseChangePercent: Double?
        get() = if (previousExpense > 0.0) ((currentExpense - previousExpense) / previousExpense) * 100.0 else null
}

data class CurrencyPeriodTotal(
    val currencyCode: String,
    val income: Double,
    val expense: Double
) {
    val net: Double get() = income - expense
}

data class DashboardSummary(
    val totalBalance: Double = 0.0,
    val periodIncome: Double = 0.0,
    val periodExpense: Double = 0.0,
    val monthlySavings: Double = 0.0,
    val currentMonthYear: String = "",
    val monthlyExpenseTotal: Double = 0.0,
    val previousMonthExpenseTotal: Double = 0.0,
    val monthlyCategoryBreakdowns: List<CategoryBreakdown> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val recentTransactions: List<TransactionWithDetails> = emptyList(),
    val topBudgets: List<BudgetWithProgress> = emptyList(),
    val budgetAlerts: List<BudgetWithProgress> = emptyList(),
    val categoryBreakdowns: List<CategoryBreakdown> = emptyList(),
    val incomeVsExpenseChart: List<ChartPoint> = emptyList(),
    val monthlySpendingTrend: List<ChartPoint> = emptyList(),
    val periodDisplayCurrencyCode: String = "BDT",
    val hasMixedCurrencies: Boolean = false,
    val currencyTotals: List<CurrencyPeriodTotal> = emptyList(),
    val isFuturePeriod: Boolean = false
)

data class AnalyticsReport(
    val granularity: AnalyticsGranularity = AnalyticsGranularity.MONTHLY,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netSavings: Double = 0.0,
    val savingsRatePercent: Double = 0.0,
    val averageDailyExpense: Double = 0.0,
    val chartPoints: List<ChartPoint> = emptyList(),
    val expenseBreakdown: List<CategoryBreakdown> = emptyList(),
    val incomeBreakdown: List<CategoryBreakdown> = emptyList(),
    val monthComparison: PeriodComparison = PeriodComparison(0.0, 0.0, 0.0, 0.0)
)

data class TransactionFilter(
    val query: String = "",
    val type: TransactionType? = null,
    val accountId: Long? = null,
    val categoryId: Long? = null,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val recurringOnly: Boolean = false,
    val sortOrder: TransactionSortOrder = TransactionSortOrder.NEWEST
)

@Serializable
enum class DriveBackupSchedule {
    MANUAL_ONLY,
    DAILY,
    WEEKLY
}

data class GoogleDriveAccount(
    val email: String,
    val displayName: String? = null
)

data class DriveBackupEntry(
    val fileId: String,
    val fileName: String,
    val modifiedTimeMillis: Long,
    val sizeBytes: Long,
    val isEncrypted: Boolean = false,
    val transactionCount: Int? = null,
    val accountCount: Int? = null
)

@Serializable
data class UserPreferences(
    val currency: AppCurrency = AppCurrency.BDT,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val numberFormat: NumberFormatStyle = NumberFormatStyle.ENGLISH,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentColor: AppAccentColor = AppAccentColor.EMERALD,
    val biometricLockEnabled: Boolean = false,
    val dailyReminderEnabled: Boolean = false,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val budgetAlertsEnabled: Boolean = true,
    val hideBalances: Boolean = false,
    val lastLocalBackupAt: Long? = null,
    val lastLocalBackupStatus: String = "",
    val driveAccountEmail: String? = null,
    val driveAccountName: String? = null,
    val driveBackupSchedule: DriveBackupSchedule = DriveBackupSchedule.MANUAL_ONLY,
    val lastDriveBackupAt: Long? = null,
    val lastDriveBackupStatus: String = "",
    val viewMode: ViewMode = ViewMode.MONTHLY,
    val languageTag: String = "en",
    val currencyCode: String = "BDT"
) {
    val effectiveLanguageTag: String
        get() = SupportedLanguageCatalog.normalizeTag(languageTag.ifBlank { language.code })

    val effectiveCurrencyCode: String
        get() = currencyCode.ifBlank { currency.code }.uppercase(Locale.US)
}

@Serializable
data class BackupPayload(
    val schemaVersion: Int = 3,
    val exportedAt: Long,
    val accounts: List<Account>,
    val categories: List<Category>,
    val transactions: List<Transaction>,
    val budgets: List<Budget>,
    val preferences: UserPreferences? = null
)
