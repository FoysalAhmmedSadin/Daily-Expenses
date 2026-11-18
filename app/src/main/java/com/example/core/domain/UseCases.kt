package com.example.core.domain

import com.example.core.common.CurrencyFormatter
import com.example.core.common.DateTimeUtils
import com.example.core.model.AnalyticsGranularity
import com.example.core.model.AnalyticsReport
import com.example.core.model.BudgetWithProgress
import com.example.core.model.CategoryBreakdown
import com.example.core.model.ChartPoint
import com.example.core.model.CurrencyPeriodTotal
import com.example.core.model.DashboardSummary
import com.example.core.model.DateFilterPeriod
import com.example.core.model.NumberFormatStyle
import com.example.core.model.PeriodComparison
import com.example.core.model.SupportedLanguageCatalog
import com.example.core.model.TransactionType
import com.example.core.model.TransactionWithDetails
import com.example.core.model.ViewMode
import com.example.core.repository.AccountRepository
import com.example.core.repository.BudgetRepository
import com.example.core.repository.CategoryRepository
import com.example.core.repository.PreferencesRepository
import com.example.core.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class HomePeriodQuery(
    val viewMode: ViewMode = ViewMode.MONTHLY,
    val anchorMillis: Long = System.currentTimeMillis(),
    val customStartMillis: Long? = null,
    val customEndMillis: Long? = null,
    val isSpecificDate: Boolean = false
) {
    val isCustomActive: Boolean
        get() = customStartMillis != null && customEndMillis != null
}

class GetBudgetsWithProgressUseCase(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) {
    operator fun invoke(monthYear: String): Flow<List<BudgetWithProgress>> {
        val (monthStart, monthEnd) = DateTimeUtils.monthYearRange(monthYear)
        return combine(
            budgetRepository.observeBudgetsForMonth(monthYear),
            transactionRepository.observeTransactionsWithDetails(),
            categoryRepository.observeCategories()
        ) { budgets, transactions, categories ->
            val monthExpenses = transactions.filter {
                it.transaction.type == TransactionType.EXPENSE &&
                    it.transaction.timestamp in monthStart..monthEnd
            }
            val totalMonthExpense = monthExpenses.sumOf { it.transaction.amount }
            val byCategory = monthExpenses.groupBy { it.transaction.categoryId }
                .mapValues { (_, list) -> list.sumOf { it.transaction.amount } }
            val categoryObjects = categories.associateBy { it.id }

            budgets.map { budget ->
                val spent = if (budget.categoryId == null) {
                    totalMonthExpense
                } else {
                    byCategory[budget.categoryId] ?: 0.0
                }
                BudgetWithProgress(
                    budget = budget,
                    category = budget.categoryId?.let { categoryObjects[it] },
                    spentAmount = spent
                )
            }
        }.flowOn(Dispatchers.Default)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class GetDashboardSummaryUseCase(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    private val preferencesRepository: PreferencesRepository
) {
    operator fun invoke(
        periodFlow: Flow<DateFilterPeriod>,
        customRangeFlow: Flow<Pair<Long?, Long?>>
    ): Flow<DashboardSummary> {
        val currentMonth = DateTimeUtils.currentMonthYear()
        return combine(
            accountRepository.observeAccounts(),
            transactionRepository.observeTransactionsWithDetails(),
            combine(
                budgetRepository.observeBudgetsForMonth(currentMonth),
                categoryRepository.observeCategories()
            ) { b, c -> b to c },
            preferencesRepository.preferences,
            combine(periodFlow, customRangeFlow) { p, r -> p to r }
        ) { accounts, transactions, (budgets, categories), prefs, (period, customRange) ->
            val (start, end) = DateTimeUtils.rangeForPeriod(
                period = period,
                customStart = customRange.first,
                customEnd = customRange.second
            )
            computeDashboardSummary(
                accounts = accounts,
                transactions = transactions,
                budgets = budgets,
                categories = categories,
                anchorMonth = currentMonth,
                rangeStart = start,
                rangeEnd = end,
                chartMode = period,
                languageTag = prefs.effectiveLanguageTag,
                numberFormat = prefs.numberFormat,
                preferredCurrencyCode = prefs.effectiveCurrencyCode
            )
        }.flowOn(Dispatchers.Default)
    }

    operator fun invoke(queryFlow: Flow<HomePeriodQuery>): Flow<DashboardSummary> {
        val anchorMonthFlow = queryFlow
            .map { query ->
                val refMillis = if (query.isCustomActive) query.customEndMillis!! else query.anchorMillis
                DateTimeUtils.currentMonthYear(refMillis)
            }
            .distinctUntilChanged()

        val budgetsWithCatsFlow = anchorMonthFlow.flatMapLatest { month ->
            combine(
                budgetRepository.observeBudgetsForMonth(month),
                categoryRepository.observeCategories()
            ) { b, c -> Triple(month, b, c) }
        }

        return combine(
            accountRepository.observeAccounts(),
            transactionRepository.observeTransactionsWithDetails(),
            budgetsWithCatsFlow,
            preferencesRepository.preferences,
            queryFlow
        ) { accounts, transactions, (anchorMonth, budgets, categories), prefs, query ->
            val (start, end) = if (query.isCustomActive) {
                val s = DateTimeUtils.startOfDay(query.customStartMillis!!)
                val e = DateTimeUtils.endOfDay(query.customEndMillis!!)
                minOf(s, e) to maxOf(s, e)
            } else {
                DateTimeUtils.rangeForViewMode(query.viewMode, query.anchorMillis)
            }

            val chartMode = when {
                query.isCustomActive && query.isSpecificDate -> DateFilterPeriod.TODAY
                query.isCustomActive -> DateFilterPeriod.CUSTOM
                else -> when (query.viewMode) {
                    ViewMode.DAILY -> DateFilterPeriod.TODAY
                    ViewMode.WEEKLY -> DateFilterPeriod.THIS_WEEK
                    ViewMode.MONTHLY -> DateFilterPeriod.THIS_MONTH
                    ViewMode.YEARLY -> DateFilterPeriod.THIS_YEAR
                }
            }

            computeDashboardSummary(
                accounts = accounts,
                transactions = transactions,
                budgets = budgets,
                categories = categories,
                anchorMonth = anchorMonth,
                rangeStart = start,
                rangeEnd = end,
                chartMode = chartMode,
                languageTag = prefs.effectiveLanguageTag,
                numberFormat = prefs.numberFormat,
                preferredCurrencyCode = prefs.effectiveCurrencyCode
            )
        }.flowOn(Dispatchers.Default)
    }

    private fun computeDashboardSummary(
        accounts: List<com.example.core.model.Account>,
        transactions: List<TransactionWithDetails>,
        budgets: List<com.example.core.model.Budget>,
        categories: List<com.example.core.model.Category>,
        anchorMonth: String,
        rangeStart: Long,
        rangeEnd: Long,
        chartMode: DateFilterPeriod,
        languageTag: String,
        numberFormat: NumberFormatStyle,
        preferredCurrencyCode: String
    ): DashboardSummary {
        val totalBalance = accounts.sumOf { it.balance }
        val periodTx = transactions.filter { it.transaction.timestamp in rangeStart..rangeEnd }

        // Group income/expense transactions in period by recorded currency code without fake FX conversion
        val periodIncomeExpense = periodTx.filter {
            it.transaction.type == TransactionType.INCOME || it.transaction.type == TransactionType.EXPENSE
        }
        val groupedByCurrency = periodIncomeExpense.groupBy {
            it.transaction.currencyCode.ifBlank { preferredCurrencyCode }.uppercase(Locale.US)
        }
        val currencyTotals = groupedByCurrency.entries
            .map { (code, items) ->
                CurrencyPeriodTotal(
                    currencyCode = code,
                    income = items.filter { it.transaction.type == TransactionType.INCOME }.sumOf { it.transaction.amount },
                    expense = items.filter { it.transaction.type == TransactionType.EXPENSE }.sumOf { it.transaction.amount }
                )
            }
            .sortedWith(
                compareByDescending<CurrencyPeriodTotal> { it.currencyCode == preferredCurrencyCode }
                    .thenByDescending { it.income + it.expense }
            )

        val hasMixedCurrencies = currencyTotals.size > 1
        val periodDisplayCurrencyCode = when {
            currencyTotals.isEmpty() -> preferredCurrencyCode
            currencyTotals.size == 1 -> currencyTotals.first().currencyCode
            else -> currencyTotals.firstOrNull { it.currencyCode == preferredCurrencyCode }?.currencyCode
                ?: currencyTotals.first().currencyCode
        }

        val activePeriodTxForPrimaryTotals = if (hasMixedCurrencies) {
            periodTx.filter {
                it.transaction.currencyCode.ifBlank { preferredCurrencyCode }.uppercase(Locale.US) == periodDisplayCurrencyCode
            }
        } else {
            periodTx
        }

        val periodIncome = activePeriodTxForPrimaryTotals
            .filter { it.transaction.type == TransactionType.INCOME }
            .sumOf { it.transaction.amount }
        val periodExpense = activePeriodTxForPrimaryTotals
            .filter { it.transaction.type == TransactionType.EXPENSE }
            .sumOf { it.transaction.amount }

        val (mStart, mEnd) = DateTimeUtils.monthYearRange(anchorMonth)
        val monthTxRaw = transactions.filter { it.transaction.timestamp in mStart..mEnd }
        val monthCurrencyCodes = monthTxRaw
            .filter { it.transaction.type != TransactionType.TRANSFER }
            .map { it.transaction.currencyCode.ifBlank { preferredCurrencyCode }.uppercase(Locale.US) }
            .distinct()
        val monthPrimaryCode = when {
            monthCurrencyCodes.isEmpty() -> preferredCurrencyCode
            monthCurrencyCodes.size == 1 -> monthCurrencyCodes.first()
            preferredCurrencyCode in monthCurrencyCodes -> preferredCurrencyCode
            else -> monthCurrencyCodes.first()
        }
        val monthTx = if (monthCurrencyCodes.size > 1) {
            monthTxRaw.filter {
                it.transaction.currencyCode.ifBlank { preferredCurrencyCode }.uppercase(Locale.US) == monthPrimaryCode
            }
        } else {
            monthTxRaw
        }
        val monthIncome = monthTx.filter { it.transaction.type == TransactionType.INCOME }
            .sumOf { it.transaction.amount }
        val monthExpense = monthTx.filter { it.transaction.type == TransactionType.EXPENSE }
            .sumOf { it.transaction.amount }
        val monthlySavings = monthIncome - monthExpense

        val expensesInPeriod = activePeriodTxForPrimaryTotals.filter { it.transaction.type == TransactionType.EXPENSE }
        val categoryBreakdowns = buildCategoryBreakdown(expensesInPeriod, periodExpense)

        val monthExpensesList = monthTx.filter { it.transaction.type == TransactionType.EXPENSE }
        val monthlyCategoryBreakdowns = buildCategoryBreakdown(monthExpensesList, monthExpense)
        val prevMonth = DateTimeUtils.previousMonthYear(anchorMonth)
        val (prevStart, prevEnd) = DateTimeUtils.monthYearRange(prevMonth)
        val previousMonthExpenseTotal = transactions
            .filter {
                it.transaction.type == TransactionType.EXPENSE &&
                    it.transaction.timestamp in prevStart..prevEnd &&
                    (monthCurrencyCodes.size <= 1 ||
                        it.transaction.currencyCode.ifBlank { preferredCurrencyCode }.uppercase(Locale.US) == monthPrimaryCode)
            }
            .sumOf { it.transaction.amount }

        val monthExpenseByCat = monthExpensesList
            .groupBy { it.transaction.categoryId }
            .mapValues { (_, list) -> list.sumOf { it.transaction.amount } }
        val allCats = categories.associateBy { it.id }
        val allBudgetsProgress = budgets.map { budget ->
            val spent = if (budget.categoryId == null) monthExpense else (monthExpenseByCat[budget.categoryId] ?: 0.0)
            BudgetWithProgress(
                budget = budget,
                category = budget.categoryId?.let { allCats[it] },
                spentAmount = spent
            )
        }.sortedByDescending { it.progressRatio }

        val topBudgets = allBudgetsProgress.take(4)
        val budgetAlerts = allBudgetsProgress.filter { it.isReachedEightyPercent }

        val monthlyTrend = buildLastNMonthsPoints(
            transactions = transactions,
            monthsCount = 6,
            languageTag = languageTag,
            numberFormat = numberFormat,
            endMonthYear = anchorMonth
        )
        val periodChart = buildPeriodChartPoints(
            transactions = activePeriodTxForPrimaryTotals,
            period = chartMode,
            start = rangeStart,
            end = rangeEnd,
            languageTag = languageTag,
            numberFormat = numberFormat
        )

        val isFuturePeriod = rangeStart > DateTimeUtils.endOfDay(System.currentTimeMillis())

        return DashboardSummary(
            totalBalance = totalBalance,
            periodIncome = periodIncome,
            periodExpense = periodExpense,
            monthlySavings = monthlySavings,
            currentMonthYear = anchorMonth,
            monthlyExpenseTotal = monthExpense,
            previousMonthExpenseTotal = previousMonthExpenseTotal,
            monthlyCategoryBreakdowns = monthlyCategoryBreakdowns,
            accounts = accounts,
            recentTransactions = periodTx.take(6),
            topBudgets = topBudgets,
            budgetAlerts = budgetAlerts,
            categoryBreakdowns = categoryBreakdowns,
            incomeVsExpenseChart = periodChart,
            monthlySpendingTrend = monthlyTrend,
            periodDisplayCurrencyCode = periodDisplayCurrencyCode,
            hasMixedCurrencies = hasMixedCurrencies,
            currencyTotals = currencyTotals,
            isFuturePeriod = isFuturePeriod
        )
    }
}

class GetAnalyticsReportUseCase(
    private val transactionRepository: TransactionRepository,
    private val preferencesRepository: PreferencesRepository
) {
    operator fun invoke(granularityFlow: Flow<AnalyticsGranularity>): Flow<AnalyticsReport> = combine(
        transactionRepository.observeTransactionsWithDetails(),
        preferencesRepository.preferences,
        granularityFlow
    ) { transactions, prefs, granularity ->
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val langTag = prefs.effectiveLanguageTag
        val locale = SupportedLanguageCatalog.localeForTag(langTag)

        val chartPoints: List<ChartPoint>
        val rangeStart: Long
        val daysCount: Int

        when (granularity) {
            AnalyticsGranularity.DAILY -> {
                daysCount = 14
                val startCal = (cal.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_YEAR, -(daysCount - 1))
                }
                rangeStart = DateTimeUtils.startOfDay(startCal.timeInMillis)
                chartPoints = (0 until daysCount).map { offset ->
                    val dayCal = (startCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offset) }
                    val dStart = DateTimeUtils.startOfDay(dayCal.timeInMillis)
                    val dEnd = DateTimeUtils.endOfDay(dayCal.timeInMillis)
                    val slice = transactions.filter { it.transaction.timestamp in dStart..dEnd }
                    val label = CurrencyFormatter.toLocalizedDigits(
                        SimpleDateFormat("dd", locale).format(Date(dStart)),
                        prefs.numberFormat
                    )
                    ChartPoint(
                        label = label,
                        income = slice.filter { it.transaction.type == TransactionType.INCOME }.sumOf { it.transaction.amount },
                        expense = slice.filter { it.transaction.type == TransactionType.EXPENSE }.sumOf { it.transaction.amount }
                    )
                }
            }
            AnalyticsGranularity.WEEKLY -> {
                val weeks = 8
                daysCount = weeks * 7
                val endOfCurrentDay = DateTimeUtils.endOfDay(now)
                rangeStart = DateTimeUtils.startOfDay(now - (daysCount - 1L) * 86_400_000L)
                chartPoints = (0 until weeks).map { idx ->
                    val wStart = rangeStart + idx * 7L * 86_400_000L
                    val wEnd = minOf(endOfCurrentDay, wStart + 7L * 86_400_000L - 1L)
                    val slice = transactions.filter { it.transaction.timestamp in wStart..wEnd }
                    ChartPoint(
                        label = DateTimeUtils.formatShortDate(wStart, langTag, prefs.numberFormat),
                        income = slice.filter { it.transaction.type == TransactionType.INCOME }.sumOf { it.transaction.amount },
                        expense = slice.filter { it.transaction.type == TransactionType.EXPENSE }.sumOf { it.transaction.amount }
                    )
                }
            }
            AnalyticsGranularity.MONTHLY -> {
                val months = 6
                chartPoints = buildLastNMonthsPoints(transactions, months, langTag, prefs.numberFormat)
                val startCal = Calendar.getInstance().apply {
                    add(Calendar.MONTH, -(months - 1))
                    set(Calendar.DAY_OF_MONTH, 1)
                }
                rangeStart = DateTimeUtils.startOfDay(startCal.timeInMillis)
                daysCount = (((now - rangeStart) / 86_400_000L).toInt() + 1).coerceAtLeast(1)
            }
            AnalyticsGranularity.YEARLY -> {
                val years = 5
                val currentYear = cal.get(Calendar.YEAR)
                val firstYear = currentYear - (years - 1)
                val startCal = Calendar.getInstance().apply {
                    set(firstYear, Calendar.JANUARY, 1, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                rangeStart = startCal.timeInMillis
                daysCount = (((now - rangeStart) / 86_400_000L).toInt() + 1).coerceAtLeast(1)
                chartPoints = (firstYear..currentYear).map { yr ->
                    val yStart = Calendar.getInstance().apply {
                        set(yr, Calendar.JANUARY, 1, 0, 0, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    val yEnd = Calendar.getInstance().apply {
                        set(yr, Calendar.DECEMBER, 31, 23, 59, 59)
                        set(Calendar.MILLISECOND, 999)
                    }.timeInMillis
                    val slice = transactions.filter { it.transaction.timestamp in yStart..yEnd }
                    ChartPoint(
                        label = CurrencyFormatter.toLocalizedDigits(yr.toString(), prefs.numberFormat),
                        income = slice.filter { it.transaction.type == TransactionType.INCOME }.sumOf { it.transaction.amount },
                        expense = slice.filter { it.transaction.type == TransactionType.EXPENSE }.sumOf { it.transaction.amount }
                    )
                }
            }
        }

        val periodTx = transactions.filter { it.transaction.timestamp in rangeStart..now }
        val totalIncome = periodTx.filter { it.transaction.type == TransactionType.INCOME }
            .sumOf { it.transaction.amount }
        val totalExpense = periodTx.filter { it.transaction.type == TransactionType.EXPENSE }
            .sumOf { it.transaction.amount }
        val netSavings = totalIncome - totalExpense
        val savingsRate = if (totalIncome > 0.0) (netSavings / totalIncome) * 100.0 else 0.0
        val avgDailyExpense = if (daysCount > 0) totalExpense / daysCount else 0.0

        val curMonth = DateTimeUtils.currentMonthYear(now)
        val prevMonth = DateTimeUtils.previousMonthYear(curMonth)
        val (curMStart, curMEnd) = DateTimeUtils.monthYearRange(curMonth)
        val (prevMStart, prevMEnd) = DateTimeUtils.monthYearRange(prevMonth)

        val curMonthTx = transactions.filter { it.transaction.timestamp in curMStart..curMEnd }
        val prevMonthTx = transactions.filter { it.transaction.timestamp in prevMStart..prevMEnd }

        val comparison = PeriodComparison(
            currentIncome = curMonthTx.filter { it.transaction.type == TransactionType.INCOME }.sumOf { it.transaction.amount },
            previousIncome = prevMonthTx.filter { it.transaction.type == TransactionType.INCOME }.sumOf { it.transaction.amount },
            currentExpense = curMonthTx.filter { it.transaction.type == TransactionType.EXPENSE }.sumOf { it.transaction.amount },
            previousExpense = prevMonthTx.filter { it.transaction.type == TransactionType.EXPENSE }.sumOf { it.transaction.amount }
        )

        AnalyticsReport(
            granularity = granularity,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netSavings = netSavings,
            savingsRatePercent = savingsRate,
            averageDailyExpense = avgDailyExpense,
            chartPoints = chartPoints,
            expenseBreakdown = buildCategoryBreakdown(
                periodTx.filter { it.transaction.type == TransactionType.EXPENSE },
                totalExpense
            ),
            incomeBreakdown = buildCategoryBreakdown(
                periodTx.filter { it.transaction.type == TransactionType.INCOME },
                totalIncome
            ),
            monthComparison = comparison
        )
    }.flowOn(Dispatchers.Default)
}

private fun buildCategoryBreakdown(
    items: List<TransactionWithDetails>,
    total: Double
): List<CategoryBreakdown> {
    if (items.isEmpty()) return emptyList()
    return items.groupBy { it.transaction.categoryId }
        .map { (_, group) ->
            val sum = group.sumOf { it.transaction.amount }
            CategoryBreakdown(
                category = group.firstOrNull()?.category,
                totalAmount = sum,
                percentage = if (total > 0.0) (sum / total).toFloat() else 0f,
                transactionCount = group.size
            )
        }
        .sortedByDescending { it.totalAmount }
}

private fun buildLastNMonthsPoints(
    transactions: List<TransactionWithDetails>,
    monthsCount: Int,
    languageTag: String,
    numberFormat: NumberFormatStyle,
    endMonthYear: String = DateTimeUtils.currentMonthYear()
): List<ChartPoint> {
    val parts = endMonthYear.split("-")
    val endCal = Calendar.getInstance().apply {
        val yr = parts.getOrNull(0)?.toIntOrNull() ?: get(Calendar.YEAR)
        val mo = (parts.getOrNull(1)?.toIntOrNull() ?: (get(Calendar.MONTH) + 1)) - 1
        set(yr, mo, 1, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val startCal = (endCal.clone() as Calendar).apply {
        add(Calendar.MONTH, -(monthsCount - 1))
        set(Calendar.DAY_OF_MONTH, 1)
    }
    return (0 until monthsCount).map { offset ->
        val mCal = (startCal.clone() as Calendar).apply { add(Calendar.MONTH, offset) }
        val ym = String.format(Locale.US, "%04d-%02d", mCal.get(Calendar.YEAR), mCal.get(Calendar.MONTH) + 1)
        val (mStart, mEnd) = DateTimeUtils.monthYearRange(ym)
        val slice = transactions.filter { it.transaction.timestamp in mStart..mEnd }
        val label = DateTimeUtils.formatShortMonthLabel(ym, languageTag, numberFormat)
        ChartPoint(
            label = label,
            income = slice.filter { it.transaction.type == TransactionType.INCOME }.sumOf { it.transaction.amount },
            expense = slice.filter { it.transaction.type == TransactionType.EXPENSE }.sumOf { it.transaction.amount }
        )
    }
}

private fun buildPeriodChartPoints(
    transactions: List<TransactionWithDetails>,
    period: DateFilterPeriod,
    start: Long,
    end: Long,
    languageTag: String,
    numberFormat: NumberFormatStyle
): List<ChartPoint> {
    val norm = SupportedLanguageCatalog.normalizeTag(languageTag)
    val locale = SupportedLanguageCatalog.localeForTag(norm)
    return when (period) {
        DateFilterPeriod.TODAY -> {
            val labels = if (norm == "bn") {
                listOf("ভোর", "সকাল", "দুপুর", "রাত")
            } else {
                listOf("00–06", "06–12", "12–18", "18–24").map {
                    CurrencyFormatter.toLocalizedDigits(it, numberFormat)
                }
            }
            val cal = Calendar.getInstance()
            (0..3).map { bucket ->
                val slice = transactions.filter {
                    cal.timeInMillis = it.transaction.timestamp
                    cal.get(Calendar.HOUR_OF_DAY) / 6 == bucket
                }
                ChartPoint(
                    label = labels[bucket],
                    income = slice.filter { it.transaction.type == TransactionType.INCOME }.sumOf { it.transaction.amount },
                    expense = slice.filter { it.transaction.type == TransactionType.EXPENSE }.sumOf { it.transaction.amount }
                )
            }
        }
        DateFilterPeriod.THIS_WEEK -> {
            val sdf = SimpleDateFormat("EEE", locale)
            (0..6).map { offset ->
                val dStart = DateTimeUtils.startOfDay(start + offset * 86_400_000L)
                val dEnd = DateTimeUtils.endOfDay(dStart)
                val slice = transactions.filter { it.transaction.timestamp in dStart..dEnd }
                ChartPoint(
                    label = sdf.format(Date(dStart)).take(3),
                    income = slice.filter { it.transaction.type == TransactionType.INCOME }.sumOf { it.transaction.amount },
                    expense = slice.filter { it.transaction.type == TransactionType.EXPENSE }.sumOf { it.transaction.amount }
                )
            }
        }
        DateFilterPeriod.THIS_MONTH -> {
            val totalMillis = (end - start).coerceAtLeast(1L)
            val step = totalMillis / 4L
            (0..3).map { idx ->
                val bStart = start + idx * step
                val bEnd = if (idx == 3) end else (start + (idx + 1) * step - 1L)
                val slice = transactions.filter { it.transaction.timestamp in bStart..bEnd }
                ChartPoint(
                    label = CurrencyFormatter.toLocalizedDigits("W${idx + 1}", numberFormat),
                    income = slice.filter { it.transaction.type == TransactionType.INCOME }.sumOf { it.transaction.amount },
                    expense = slice.filter { it.transaction.type == TransactionType.EXPENSE }.sumOf { it.transaction.amount }
                )
            }
        }
        DateFilterPeriod.THIS_YEAR -> {
            val cal = Calendar.getInstance().apply { timeInMillis = start }
            val year = cal.get(Calendar.YEAR)
            (0..11).map { m ->
                val mStr = String.format(Locale.US, "%04d-%02d", year, m + 1)
                val (mStart, mEnd) = DateTimeUtils.monthYearRange(mStr)
                val slice = transactions.filter { it.transaction.timestamp in mStart..mEnd }
                val mLabel = DateTimeUtils.formatShortMonthLabel(mStr, langTag = norm, numberFormat = numberFormat)
                    .take(if (norm == "en") 1 else 2)
                ChartPoint(
                    label = mLabel,
                    income = slice.filter { it.transaction.type == TransactionType.INCOME }.sumOf { it.transaction.amount },
                    expense = slice.filter { it.transaction.type == TransactionType.EXPENSE }.sumOf { it.transaction.amount }
                )
            }
        }
        DateFilterPeriod.CUSTOM -> {
            val span = (end - start).coerceAtLeast(1L)
            val buckets = 6
            val step = span / buckets
            (0 until buckets).map { idx ->
                val bStart = start + idx * step
                val bEnd = if (idx == buckets - 1) end else (start + (idx + 1) * step - 1L)
                val slice = transactions.filter { it.transaction.timestamp in bStart..bEnd }
                ChartPoint(
                    label = DateTimeUtils.formatShortDate(bStart, norm, numberFormat),
                    income = slice.filter { it.transaction.type == TransactionType.INCOME }.sumOf { it.transaction.amount },
                    expense = slice.filter { it.transaction.type == TransactionType.EXPENSE }.sumOf { it.transaction.amount }
                )
            }
        }
    }
}

private fun DateTimeUtils.formatShortMonthLabel(
    monthYear: String,
    langTag: String,
    numberFormat: NumberFormatStyle
): String = formatShortMonthLabel(monthYear = monthYear, languageTag = langTag, numberFormat = numberFormat)
