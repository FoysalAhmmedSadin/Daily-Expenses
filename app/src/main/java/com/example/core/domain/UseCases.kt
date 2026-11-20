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
                val endRange = DateTimeUtils.endOfDay(now)
                val dayStarts = LongArray(daysCount)
                val labels = Array(daysCount) { "" }
                val sdf = SimpleDateFormat("dd", locale)
                for (offset in 0 until daysCount) {
                    val dayCal = (startCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offset) }
                    val dStart = DateTimeUtils.startOfDay(dayCal.timeInMillis)
                    dayStarts[offset] = dStart
                    labels[offset] = CurrencyFormatter.toLocalizedDigits(sdf.format(Date(dStart)), prefs.numberFormat)
                }
                val incomeBuckets = DoubleArray(daysCount)
                val expenseBuckets = DoubleArray(daysCount)
                transactions.forEach { item ->
                    val tx = item.transaction
                    if (tx.timestamp in rangeStart..endRange) {
                        val idx = ((tx.timestamp - rangeStart) / 86_400_000L).toInt().coerceIn(0, daysCount - 1)
                        var bucket = idx
                        while (bucket > 0 && tx.timestamp < dayStarts[bucket]) bucket--
                        while (bucket < daysCount - 1 && tx.timestamp >= dayStarts[bucket + 1]) bucket++
                        when (tx.type) {
                            TransactionType.INCOME -> incomeBuckets[bucket] += tx.amount
                            TransactionType.EXPENSE -> expenseBuckets[bucket] += tx.amount
                            TransactionType.TRANSFER -> Unit
                        }
                    }
                }
                chartPoints = (0 until daysCount).map { i ->
                    ChartPoint(label = labels[i], income = incomeBuckets[i], expense = expenseBuckets[i])
                }
            }
            AnalyticsGranularity.WEEKLY -> {
                val weeks = 8
                daysCount = weeks * 7
                val endOfCurrentDay = DateTimeUtils.endOfDay(now)
                rangeStart = DateTimeUtils.startOfDay(now - (daysCount - 1L) * 86_400_000L)
                val incomeBuckets = DoubleArray(weeks)
                val expenseBuckets = DoubleArray(weeks)
                val weekMillis = 7L * 86_400_000L
                transactions.forEach { item ->
                    val tx = item.transaction
                    if (tx.timestamp in rangeStart..endOfCurrentDay) {
                        val bucket = ((tx.timestamp - rangeStart) / weekMillis).toInt().coerceIn(0, weeks - 1)
                        when (tx.type) {
                            TransactionType.INCOME -> incomeBuckets[bucket] += tx.amount
                            TransactionType.EXPENSE -> expenseBuckets[bucket] += tx.amount
                            TransactionType.TRANSFER -> Unit
                        }
                    }
                }
                chartPoints = (0 until weeks).map { idx ->
                    val wStart = rangeStart + idx * weekMillis
                    ChartPoint(
                        label = DateTimeUtils.formatShortDate(wStart, langTag, prefs.numberFormat),
                        income = incomeBuckets[idx],
                        expense = expenseBuckets[idx]
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
                val endCalMillis = Calendar.getInstance().apply {
                    set(currentYear, Calendar.DECEMBER, 31, 23, 59, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis
                daysCount = (((now - rangeStart) / 86_400_000L).toInt() + 1).coerceAtLeast(1)
                val incomeBuckets = DoubleArray(years)
                val expenseBuckets = DoubleArray(years)
                val tmpCal = Calendar.getInstance()
                transactions.forEach { item ->
                    val tx = item.transaction
                    if (tx.timestamp in rangeStart..endCalMillis) {
                        tmpCal.timeInMillis = tx.timestamp
                        val idx = (tmpCal.get(Calendar.YEAR) - firstYear).coerceIn(0, years - 1)
                        when (tx.type) {
                            TransactionType.INCOME -> incomeBuckets[idx] += tx.amount
                            TransactionType.EXPENSE -> expenseBuckets[idx] += tx.amount
                            TransactionType.TRANSFER -> Unit
                        }
                    }
                }
                chartPoints = (0 until years).map { i ->
                    val yr = firstYear + i
                    ChartPoint(
                        label = CurrencyFormatter.toLocalizedDigits(yr.toString(), prefs.numberFormat),
                        income = incomeBuckets[i],
                        expense = expenseBuckets[i]
                    )
                }
            }
        }

        val curMonth = DateTimeUtils.currentMonthYear(now)
        val prevMonth = DateTimeUtils.previousMonthYear(curMonth)
        val (curMStart, curMEnd) = DateTimeUtils.monthYearRange(curMonth)
        val (prevMStart, prevMEnd) = DateTimeUtils.monthYearRange(prevMonth)

        var totalIncome = 0.0
        var totalExpense = 0.0
        var curIncome = 0.0
        var curExpense = 0.0
        var prevIncome = 0.0
        var prevExpense = 0.0
        val periodExpenses = ArrayList<TransactionWithDetails>()
        val periodIncomes = ArrayList<TransactionWithDetails>()

        transactions.forEach { item ->
            val tx = item.transaction
            val ts = tx.timestamp
            if (ts in rangeStart..now) {
                when (tx.type) {
                    TransactionType.INCOME -> {
                        totalIncome += tx.amount
                        periodIncomes.add(item)
                    }
                    TransactionType.EXPENSE -> {
                        totalExpense += tx.amount
                        periodExpenses.add(item)
                    }
                    TransactionType.TRANSFER -> Unit
                }
            }
            if (ts in curMStart..curMEnd) {
                when (tx.type) {
                    TransactionType.INCOME -> curIncome += tx.amount
                    TransactionType.EXPENSE -> curExpense += tx.amount
                    TransactionType.TRANSFER -> Unit
                }
            } else if (ts in prevMStart..prevMEnd) {
                when (tx.type) {
                    TransactionType.INCOME -> prevIncome += tx.amount
                    TransactionType.EXPENSE -> prevExpense += tx.amount
                    TransactionType.TRANSFER -> Unit
                }
            }
        }

        val netSavings = totalIncome - totalExpense
        val savingsRate = if (totalIncome > 0.0) (netSavings / totalIncome) * 100.0 else 0.0
        val avgDailyExpense = if (daysCount > 0) totalExpense / daysCount else 0.0

        val comparison = PeriodComparison(
            currentIncome = curIncome,
            previousIncome = prevIncome,
            currentExpense = curExpense,
            previousExpense = prevExpense
        )

        AnalyticsReport(
            granularity = granularity,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netSavings = netSavings,
            savingsRatePercent = savingsRate,
            averageDailyExpense = avgDailyExpense,
            chartPoints = chartPoints,
            expenseBreakdown = buildCategoryBreakdown(periodExpenses, totalExpense),
            incomeBreakdown = buildCategoryBreakdown(periodIncomes, totalIncome),
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
    val startYear = startCal.get(Calendar.YEAR)
    val startMonth = startCal.get(Calendar.MONTH)
    val firstRangeStart = startCal.timeInMillis
    val lastYm = String.format(Locale.US, "%04d-%02d", endCal.get(Calendar.YEAR), endCal.get(Calendar.MONTH) + 1)
    val lastRangeEnd = DateTimeUtils.monthYearRange(lastYm).second

    val incomeBuckets = DoubleArray(monthsCount)
    val expenseBuckets = DoubleArray(monthsCount)
    val tmpCal = Calendar.getInstance()
    transactions.forEach { item ->
        val tx = item.transaction
        if (tx.timestamp in firstRangeStart..lastRangeEnd) {
            tmpCal.timeInMillis = tx.timestamp
            val idx = (tmpCal.get(Calendar.YEAR) - startYear) * 12 + (tmpCal.get(Calendar.MONTH) - startMonth)
            if (idx in 0 until monthsCount) {
                when (tx.type) {
                    TransactionType.INCOME -> incomeBuckets[idx] += tx.amount
                    TransactionType.EXPENSE -> expenseBuckets[idx] += tx.amount
                    TransactionType.TRANSFER -> Unit
                }
            }
        }
    }

    return (0 until monthsCount).map { offset ->
        val mCal = (startCal.clone() as Calendar).apply { add(Calendar.MONTH, offset) }
        val ym = String.format(Locale.US, "%04d-%02d", mCal.get(Calendar.YEAR), mCal.get(Calendar.MONTH) + 1)
        ChartPoint(
            label = DateTimeUtils.formatShortMonthLabel(ym, languageTag, numberFormat),
            income = incomeBuckets[offset],
            expense = expenseBuckets[offset]
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
            val labels = when {
                norm == "bn" -> listOf("ভোর", "সকাল", "দুপুর", "রাত")
                norm.startsWith("zh-") -> listOf("凌晨", "上午", "下午", "夜晚")
                else -> listOf("00–06", "06–12", "12–18", "18–24").map {
                    CurrencyFormatter.toLocalizedDigits(it, numberFormat)
                }
            }
            val incomeBuckets = DoubleArray(4)
            val expenseBuckets = DoubleArray(4)
            val cal = Calendar.getInstance()
            transactions.forEach { item ->
                val tx = item.transaction
                cal.timeInMillis = tx.timestamp
                val bucket = (cal.get(Calendar.HOUR_OF_DAY) / 6).coerceIn(0, 3)
                when (tx.type) {
                    TransactionType.INCOME -> incomeBuckets[bucket] += tx.amount
                    TransactionType.EXPENSE -> expenseBuckets[bucket] += tx.amount
                    TransactionType.TRANSFER -> Unit
                }
            }
            (0..3).map { bucket ->
                ChartPoint(label = labels[bucket], income = incomeBuckets[bucket], expense = expenseBuckets[bucket])
            }
        }
        DateFilterPeriod.THIS_WEEK -> {
            val sdf = SimpleDateFormat("EEE", locale)
            val dayStarts = LongArray(7)
            val labels = Array(7) { "" }
            for (offset in 0..6) {
                val dStart = DateTimeUtils.startOfDay(start + offset * 86_400_000L)
                dayStarts[offset] = dStart
                labels[offset] = sdf.format(Date(dStart)).take(3)
            }
            val incomeBuckets = DoubleArray(7)
            val expenseBuckets = DoubleArray(7)
            val weekEnd = DateTimeUtils.endOfDay(dayStarts[6])
            transactions.forEach { item ->
                val tx = item.transaction
                if (tx.timestamp in dayStarts[0]..weekEnd) {
                    var bucket = ((tx.timestamp - dayStarts[0]) / 86_400_000L).toInt().coerceIn(0, 6)
                    while (bucket > 0 && tx.timestamp < dayStarts[bucket]) bucket--
                    while (bucket < 6 && tx.timestamp >= dayStarts[bucket + 1]) bucket++
                    when (tx.type) {
                        TransactionType.INCOME -> incomeBuckets[bucket] += tx.amount
                        TransactionType.EXPENSE -> expenseBuckets[bucket] += tx.amount
                        TransactionType.TRANSFER -> Unit
                    }
                }
            }
            (0..6).map { offset ->
                ChartPoint(label = labels[offset], income = incomeBuckets[offset], expense = expenseBuckets[offset])
            }
        }
        DateFilterPeriod.THIS_MONTH -> {
            val totalMillis = (end - start).coerceAtLeast(1L)
            val step = totalMillis / 4L
            val incomeBuckets = DoubleArray(4)
            val expenseBuckets = DoubleArray(4)
            transactions.forEach { item ->
                val tx = item.transaction
                if (tx.timestamp in start..end) {
                    val idx = ((tx.timestamp - start) / step.coerceAtLeast(1L)).toInt().coerceIn(0, 3)
                    when (tx.type) {
                        TransactionType.INCOME -> incomeBuckets[idx] += tx.amount
                        TransactionType.EXPENSE -> expenseBuckets[idx] += tx.amount
                        TransactionType.TRANSFER -> Unit
                    }
                }
            }
            (0..3).map { idx ->
                ChartPoint(
                    label = CurrencyFormatter.toLocalizedDigits("W${idx + 1}", numberFormat),
                    income = incomeBuckets[idx],
                    expense = expenseBuckets[idx]
                )
            }
        }
        DateFilterPeriod.THIS_YEAR -> {
            val cal = Calendar.getInstance().apply { timeInMillis = start }
            val year = cal.get(Calendar.YEAR)
            val yearRangeStart = DateTimeUtils.monthYearRange(String.format(Locale.US, "%04d-01", year)).first
            val yearRangeEnd = DateTimeUtils.monthYearRange(String.format(Locale.US, "%04d-12", year)).second
            val incomeBuckets = DoubleArray(12)
            val expenseBuckets = DoubleArray(12)
            val tmpCal = Calendar.getInstance()
            transactions.forEach { item ->
                val tx = item.transaction
                if (tx.timestamp in yearRangeStart..yearRangeEnd) {
                    tmpCal.timeInMillis = tx.timestamp
                    val m = tmpCal.get(Calendar.MONTH).coerceIn(0, 11)
                    when (tx.type) {
                        TransactionType.INCOME -> incomeBuckets[m] += tx.amount
                        TransactionType.EXPENSE -> expenseBuckets[m] += tx.amount
                        TransactionType.TRANSFER -> Unit
                    }
                }
            }
            (0..11).map { m ->
                val mStr = String.format(Locale.US, "%04d-%02d", year, m + 1)
                val shortMonth = DateTimeUtils.formatShortMonthLabel(mStr, langTag = norm, numberFormat = numberFormat)
                val mLabel = when {
                    norm == "en" -> shortMonth.take(1)
                    norm.startsWith("zh-") -> shortMonth
                    else -> shortMonth.take(2)
                }
                ChartPoint(label = mLabel, income = incomeBuckets[m], expense = expenseBuckets[m])
            }
        }
        DateFilterPeriod.CUSTOM -> {
            val span = (end - start).coerceAtLeast(1L)
            val buckets = 6
            val step = (span / buckets).coerceAtLeast(1L)
            val incomeBuckets = DoubleArray(buckets)
            val expenseBuckets = DoubleArray(buckets)
            transactions.forEach { item ->
                val tx = item.transaction
                if (tx.timestamp in start..end) {
                    val idx = ((tx.timestamp - start) / step).toInt().coerceIn(0, buckets - 1)
                    when (tx.type) {
                        TransactionType.INCOME -> incomeBuckets[idx] += tx.amount
                        TransactionType.EXPENSE -> expenseBuckets[idx] += tx.amount
                        TransactionType.TRANSFER -> Unit
                    }
                }
            }
            (0 until buckets).map { idx ->
                val bStart = start + idx * step
                ChartPoint(
                    label = DateTimeUtils.formatShortDate(bStart, norm, numberFormat),
                    income = incomeBuckets[idx],
                    expense = expenseBuckets[idx]
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
