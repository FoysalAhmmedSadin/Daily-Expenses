package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.common.CategoryIcons
import com.example.core.common.CurrencyFormatter
import com.example.core.common.DateTimeUtils
import com.example.core.common.LocalAppStrings
import com.example.core.model.Account
import com.example.core.model.CategoryBreakdown
import com.example.core.model.CurrencyPeriodTotal
import com.example.core.model.CustomDatePreset
import com.example.core.model.UserPreferences
import com.example.core.model.ViewMode
import com.example.ui.components.BudgetProgressItem
import com.example.ui.components.CategoryDonutChart
import com.example.ui.components.CategoryExpenseBarChart
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.EmptyStateView
import com.example.ui.components.IncomeVsExpenseBarChart
import com.example.ui.components.PremiumCard
import com.example.ui.components.SpendingTrendLineChart
import com.example.ui.components.TransactionRowItem
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.WarningAmber

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    onToggleHideBalances: () -> Unit,
    onOpenAccounts: () -> Unit,
    onOpenAccountDetail: (Long) -> Unit,
    onOpenTransactions: () -> Unit,
    onOpenTransaction: (Long) -> Unit,
    onOpenBudgets: () -> Unit,
    onAddTransaction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by homeViewModel.uiState.collectAsStateWithLifecycle()
    val strings = LocalAppStrings.current
    val prefs = state.preferences
    val summary = state.summary
    val langTag = prefs.effectiveLanguageTag

    var showPeriodSheet by remember { mutableStateOf(false) }
    var showSpecificDatePicker by remember { mutableStateOf(false) }
    var showDateRangePicker by remember { mutableStateOf(false) }

    val periodDisplayLabel = remember(
        state.viewMode,
        state.anchorMillis,
        state.isCustomFilterActive,
        state.isSpecificDate,
        state.customStartMillis,
        state.customEndMillis,
        langTag,
        prefs.numberFormat
    ) {
        val cs = state.customStartMillis
        val ce = state.customEndMillis
        if (state.isCustomFilterActive && cs != null && ce != null) {
            if (state.isSpecificDate || DateTimeUtils.startOfDay(cs) == DateTimeUtils.startOfDay(ce)) {
                DateTimeUtils.formatDate(cs, langTag, prefs.numberFormat)
            } else {
                "${DateTimeUtils.formatDate(cs, langTag, prefs.numberFormat)} – ${DateTimeUtils.formatDate(ce, langTag, prefs.numberFormat)}"
            }
        } else {
            DateTimeUtils.formatViewModePeriodLabel(
                viewMode = state.viewMode,
                anchorMillis = state.anchorMillis,
                languageTag = langTag,
                numberFormat = prefs.numberFormat
            )
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .testTag("home_dashboard_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.appName,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        val modeSubtitle = if (state.isCustomFilterActive) {
                            strings.customRange
                        } else {
                            strings.viewModeLabel(state.viewMode)
                        }
                        Text(
                            text = "${strings.appSubtitle} • $modeSubtitle",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onToggleHideBalances,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("toggle_privacy_button")
                        ) {
                            Icon(
                                imageVector = if (prefs.hideBalances) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = strings.hideBalances,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            onClick = onOpenAccounts,
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp,
                            modifier = Modifier.testTag("header_accounts_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AccountBalanceWallet,
                                    contentDescription = strings.accounts,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = strings.accounts,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }

            // Compact Period / Month Navigator (matches BudgetsScreen style + Calendar button)
            item {
                PremiumCard(
                    contentPadding = 8.dp,
                    modifier = Modifier.testTag("home_period_navigator")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { homeViewModel.shiftPeriod(-1) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("home_period_prev")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                contentDescription = strings.previousPeriod
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showPeriodSheet = true }
                                .padding(vertical = 8.dp, horizontal = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = periodDisplayLabel,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.testTag("home_period_label")
                            )
                        }

                        IconButton(
                            onClick = { homeViewModel.shiftPeriod(1) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("home_period_next")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = strings.nextPeriod
                            )
                        }

                        Spacer(modifier = Modifier.width(2.dp))

                        Surface(
                            onClick = { showPeriodSheet = true },
                            shape = RoundedCornerShape(12.dp),
                            color = if (state.isCustomFilterActive) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("home_calendar_button")
                        ) {
                            Box(
                                modifier = Modifier.size(44.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CalendarMonth,
                                    contentDescription = strings.selectPeriodShortcut,
                                    tint = if (state.isCustomFilterActive) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    },
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Custom Date / Date Range Active Banner
            if (state.isCustomFilterActive) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_filter_active_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.customFilterActive,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = periodDisplayLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            TextButton(
                                onClick = { homeViewModel.clearCustomFilter() },
                                modifier = Modifier.testTag("clear_custom_filter_button")
                            ) {
                                Text(
                                    text = if (state.viewMode == ViewMode.MONTHLY) {
                                        strings.returnToMonthlyView
                                    } else {
                                        strings.returnToPeriodView(state.viewMode)
                                    },
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Future Period Notice
            if (summary.isFuturePeriod) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_future_period_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = strings.futurePeriodNote,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Hero Balance Card
            item {
                val useMonthlySavingsPill = state.viewMode == ViewMode.MONTHLY && !state.isCustomFilterActive
                val savingsAmount = if (useMonthlySavingsPill) {
                    summary.monthlySavings
                } else {
                    summary.periodIncome - summary.periodExpense
                }
                val savingsTitle = if (useMonthlySavingsPill) strings.monthlySavings else strings.netCashFlow
                HeroBalanceCard(
                    totalBalance = summary.totalBalance,
                    periodIncome = summary.periodIncome,
                    periodExpense = summary.periodExpense,
                    savingsAmount = savingsAmount,
                    savingsLabel = savingsTitle,
                    periodCurrencyCode = summary.periodDisplayCurrencyCode,
                    hasMixedCurrencies = summary.hasMixedCurrencies,
                    currencyTotals = summary.currencyTotals,
                    mixedCurrencyNote = strings.mixedCurrencyNote,
                    preferences = prefs
                )
            }

            // 80%+ Budget Limit Warning & Progress Indicator
            if (summary.budgetAlerts.isNotEmpty()) {
                item {
                    val hasOverspent = summary.budgetAlerts.any { it.isOverspent }
                    val alertTint = if (hasOverspent) ExpenseRed else WarningAmber
                    PremiumCard(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.testTag("dashboard_budget_alert_card")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(alertTint.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.WarningAmber,
                                        contentDescription = strings.budgetAlertHeader,
                                        tint = alertTint,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = strings.budgetAlertHeader,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = strings.budgetAlertSummary(
                                            summary.budgetAlerts.size,
                                            prefs.numberFormat
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = alertTint
                                    )
                                }
                            }
                            Text(
                                text = strings.manage,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(onClick = onOpenBudgets)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            summary.budgetAlerts.forEach { alertBudget ->
                                BudgetProgressItem(
                                    budgetWithProgress = alertBudget,
                                    preferences = prefs,
                                    currencyCode = summary.periodDisplayCurrencyCode,
                                    onClick = onOpenBudgets,
                                    tagPrefix = "dashboard_alert_",
                                    modifier = Modifier.testTag("dashboard_alert_item_${alertBudget.budget.id}")
                                )
                            }
                        }
                    }
                }
            }

            // Account Balance Overview Carousel
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(
                        title = strings.accounts,
                        actionText = strings.manage,
                        onAction = onOpenAccounts
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(summary.accounts, key = { it.id }) { account ->
                            AccountMiniCard(
                                account = account,
                                preferences = prefs,
                                onClick = { onOpenAccountDetail(account.id) }
                            )
                        }
                    }
                }
            }

            // Monthly Expense Summary by Category (Bar Chart)
            item {
                MonthlyExpenseSummaryCard(
                    monthYear = summary.currentMonthYear.ifBlank { DateTimeUtils.currentMonthYear() },
                    monthlyTotal = summary.monthlyExpenseTotal,
                    previousMonthTotal = summary.previousMonthExpenseTotal,
                    breakdowns = summary.monthlyCategoryBreakdowns,
                    displayCurrencyCode = summary.periodDisplayCurrencyCode,
                    preferences = prefs,
                    onAddTransaction = onAddTransaction
                )
            }

            // Income vs Expense Chart
            item {
                PremiumCard {
                    SectionHeader(
                        title = strings.incomeVsExpense,
                        actionText = null,
                        onAction = null
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (summary.incomeVsExpenseChart.all { it.income == 0.0 && it.expense == 0.0 }) {
                        EmptyStateView(
                            icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                            title = strings.noTransactionsTitle,
                            subtitle = strings.noTransactionsSubtitle,
                            actionLabel = strings.addTransaction,
                            onAction = onAddTransaction
                        )
                    } else {
                        IncomeVsExpenseBarChart(
                            points = summary.incomeVsExpenseChart,
                            preferences = prefs,
                            incomeLabel = strings.income,
                            expenseLabel = strings.expense,
                            currencyCode = summary.periodDisplayCurrencyCode
                        )
                    }
                }
            }

            // Monthly Spending Overview (6-month trend)
            item {
                PremiumCard {
                    SectionHeader(
                        title = strings.spendingOverview,
                        actionText = null,
                        onAction = null
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    SpendingTrendLineChart(
                        points = summary.monthlySpendingTrend,
                        preferences = prefs,
                        currencyCode = summary.periodDisplayCurrencyCode
                    )
                }
            }

            // Expense Category Breakdown
            if (summary.categoryBreakdowns.isNotEmpty()) {
                item {
                    PremiumCard {
                        SectionHeader(
                            title = strings.categoryBreakdown,
                            actionText = null,
                            onAction = null
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        CategoryDonutChart(
                            breakdowns = summary.categoryBreakdowns,
                            preferences = prefs,
                            totalLabel = strings.expense,
                            currencyCode = summary.periodDisplayCurrencyCode
                        )
                    }
                }
            }

            // Budget Progress
            item {
                PremiumCard(
                    modifier = Modifier.testTag("dashboard_budget_progress_card")
                ) {
                    SectionHeader(
                        title = strings.budgetProgress,
                        actionText = strings.seeAll,
                        onAction = onOpenBudgets
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (summary.topBudgets.isEmpty()) {
                        EmptyStateView(
                            icon = ImageVector.vectorResource(R.drawable.ic_chart_no_axes_combined),
                            title = strings.noBudgetsTitle,
                            subtitle = strings.noBudgetsSubtitle,
                            actionLabel = strings.addBudget,
                            iconTint = MaterialTheme.colorScheme.primary,
                            onAction = onOpenBudgets
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            summary.topBudgets.forEach { budget ->
                                BudgetProgressItem(
                                    budgetWithProgress = budget,
                                    preferences = prefs,
                                    currencyCode = summary.periodDisplayCurrencyCode,
                                    onClick = onOpenBudgets,
                                    tagPrefix = "dashboard_top_"
                                )
                            }
                        }
                    }
                }
            }

            // Recent Transactions
            item {
                PremiumCard {
                    SectionHeader(
                        title = strings.recentTransactions,
                        actionText = strings.seeAll,
                        onAction = onOpenTransactions
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (summary.recentTransactions.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                            title = strings.noTransactionsTitle,
                            subtitle = strings.noTransactionsSubtitle,
                            actionLabel = strings.addTransaction,
                            onAction = onAddTransaction
                        )
                    } else {
                        Column {
                            summary.recentTransactions.forEachIndexed { index, tx ->
                                TransactionRowItem(
                                    item = tx,
                                    preferences = prefs,
                                    onClick = { onOpenTransaction(tx.transaction.id) },
                                    modifier = Modifier.testTag("recent_tx_${tx.transaction.id}")
                                )
                                if (index < summary.recentTransactions.lastIndex) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Bottom sheet for Period & Date Shortcuts
    if (showPeriodSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPeriodSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .padding(bottom = 24.dp)
                    .testTag("home_date_filter_sheet"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.selectPeriodShortcut,
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(onClick = { showPeriodSheet = false }) {
                        Icon(Icons.Rounded.Close, contentDescription = strings.cancel)
                    }
                }

                // Specific Date & Date Range actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            showPeriodSheet = false
                            showSpecificDatePicker = true
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("shortcut_specific_date")
                    ) {
                        Icon(Icons.Rounded.Today, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.selectSpecificDate, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    FilledTonalButton(
                        onClick = {
                            showPeriodSheet = false
                            homeViewModel.clearDateRangeValidationError()
                            showDateRangePicker = true
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("shortcut_date_range")
                    ) {
                        Icon(Icons.Rounded.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.selectDateRange, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }

                // Quick Period Shortcuts: Today, This Week, This Month, This Year
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            homeViewModel.applyQuickPreset(CustomDatePreset.TODAY)
                            showPeriodSheet = false
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("shortcut_today")
                    ) { Text(strings.today) }
                    OutlinedButton(
                        onClick = {
                            homeViewModel.applyQuickPreset(CustomDatePreset.THIS_WEEK)
                            showPeriodSheet = false
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("shortcut_this_week")
                    ) { Text(strings.thisWeek) }
                    OutlinedButton(
                        onClick = {
                            homeViewModel.applyQuickPreset(CustomDatePreset.THIS_MONTH)
                            showPeriodSheet = false
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("shortcut_this_month")
                    ) { Text(strings.thisMonth) }
                    OutlinedButton(
                        onClick = {
                            homeViewModel.applyQuickPreset(CustomDatePreset.THIS_YEAR)
                            showPeriodSheet = false
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("shortcut_this_year")
                    ) { Text(strings.thisYear) }
                }

                if (state.isCustomFilterActive) {
                    TextButton(
                        onClick = {
                            homeViewModel.clearCustomFilter()
                            showPeriodSheet = false
                        },
                        modifier = Modifier
                            .align(Alignment.End)
                            .testTag("shortcut_clear_selection")
                    ) {
                        Text(strings.clearSelection)
                    }
                }
            }
        }
    }

    // Specific Date Picker Dialog
    if (showSpecificDatePicker) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = state.customStartMillis ?: state.anchorMillis
        )
        DatePickerDialog(
            onDismissRequest = { showSpecificDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        dateState.selectedDateMillis?.let { millis ->
                            homeViewModel.applySpecificDate(millis)
                        }
                        showSpecificDatePicker = false
                    },
                    enabled = dateState.selectedDateMillis != null,
                    modifier = Modifier.testTag("specific_date_apply_button")
                ) { Text(strings.apply) }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (state.isCustomFilterActive) {
                        TextButton(
                            onClick = {
                                homeViewModel.clearCustomFilter()
                                showSpecificDatePicker = false
                            }
                        ) { Text(strings.clearSelection) }
                    }
                    TextButton(
                        onClick = { showSpecificDatePicker = false },
                        modifier = Modifier.testTag("specific_date_cancel_button")
                    ) { Text(strings.cancel) }
                }
            },
            modifier = Modifier.testTag("specific_date_picker_dialog")
        ) {
            DatePicker(state = dateState)
        }
    }

    // Custom Date Range Picker Dialog
    if (showDateRangePicker) {
        val pickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = state.customStartMillis,
            initialSelectedEndDateMillis = state.customEndMillis
        )
        val selectedStart = pickerState.selectedStartDateMillis
        val selectedEnd = pickerState.selectedEndDateMillis ?: selectedStart
        val localInvalidRange = selectedStart != null && selectedEnd != null &&
            DateTimeUtils.startOfDay(selectedEnd) < DateTimeUtils.startOfDay(selectedStart)

        DatePickerDialog(
            onDismissRequest = {
                homeViewModel.clearDateRangeValidationError()
                showDateRangePicker = false
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedStart != null && selectedEnd != null) {
                            if (homeViewModel.applyDateRange(selectedStart, selectedEnd)) {
                                showDateRangePicker = false
                            }
                        }
                    },
                    enabled = selectedStart != null && !localInvalidRange,
                    modifier = Modifier.testTag("date_range_apply_button")
                ) { Text(strings.apply) }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            pickerState.setSelection(null, null)
                            homeViewModel.clearCustomFilter()
                            showDateRangePicker = false
                        },
                        modifier = Modifier.testTag("date_range_clear_button")
                    ) { Text(strings.clearSelection) }
                    TextButton(
                        onClick = {
                            homeViewModel.clearDateRangeValidationError()
                            showDateRangePicker = false
                        },
                        modifier = Modifier.testTag("date_range_cancel_button")
                    ) { Text(strings.cancel) }
                }
            },
            modifier = Modifier.testTag("date_range_picker_dialog")
        ) {
            Column {
                DateRangePicker(
                    state = pickerState,
                    modifier = Modifier.height(410.dp)
                )
                if (localInvalidRange || state.dateRangeValidationError) {
                    Text(
                        text = strings.invalidDateRange,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .padding(horizontal = 24.dp, vertical = 4.dp)
                            .testTag("date_range_error_text")
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeroBalanceCard(
    totalBalance: Double,
    periodIncome: Double,
    periodExpense: Double,
    savingsAmount: Double,
    savingsLabel: String,
    periodCurrencyCode: String,
    hasMixedCurrencies: Boolean,
    currencyTotals: List<CurrencyPeriodTotal>,
    mixedCurrencyNote: String,
    preferences: UserPreferences
) {
    val strings = LocalAppStrings.current
    val primary = MaterialTheme.colorScheme.primary
    val accent = MaterialTheme.colorScheme.tertiary
    val heroTintEnd = lerp(Color(0xFF13191D), primary, 0.16f)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = Color(0xFF14181C),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF101318),
                            heroTintEnd
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.totalBalance,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFF9EA6B0)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = CurrencyFormatter.format(
                                totalBalance,
                                preferences.effectiveCurrencyCode,
                                preferences.numberFormat,
                                hideAmount = preferences.hideBalances,
                                languageTag = preferences.effectiveLanguageTag
                            ),
                            style = MaterialTheme.typography.displaySmall,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("hero_total_balance")
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = primary.copy(alpha = 0.16f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = savingsLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = accent
                            )
                            Text(
                                text = CurrencyFormatter.format(
                                    savingsAmount,
                                    periodCurrencyCode,
                                    preferences.numberFormat,
                                    hideAmount = preferences.hideBalances,
                                    showSign = true,
                                    languageTag = preferences.effectiveLanguageTag
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (savingsAmount >= 0) accent else ExpenseRed
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HeroMetricPill(
                        label = strings.totalIncome,
                        value = CurrencyFormatter.format(
                            periodIncome,
                            periodCurrencyCode,
                            preferences.numberFormat,
                            hideAmount = preferences.hideBalances,
                            languageTag = preferences.effectiveLanguageTag
                        ),
                        icon = Icons.Rounded.ArrowDownward,
                        tint = IncomeGreen,
                        modifier = Modifier.weight(1f)
                    )
                    HeroMetricPill(
                        label = strings.totalExpenses,
                        value = CurrencyFormatter.format(
                            periodExpense,
                            periodCurrencyCode,
                            preferences.numberFormat,
                            hideAmount = preferences.hideBalances,
                            languageTag = preferences.effectiveLanguageTag
                        ),
                        icon = Icons.Rounded.ArrowUpward,
                        tint = ExpenseRed,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (hasMixedCurrencies && currencyTotals.size > 1) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_mixed_currency_note")
                    ) {
                        Text(
                            text = mixedCurrencyNote,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFB5BDC8)
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            currencyTotals.forEach { ct ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White.copy(alpha = 0.08f)
                                ) {
                                    Text(
                                        text = "${ct.currencyCode}: +${
                                            CurrencyFormatter.format(
                                                ct.income,
                                                ct.currencyCode,
                                                preferences.numberFormat,
                                                hideAmount = preferences.hideBalances,
                                                includeDecimals = false,
                                                languageTag = preferences.effectiveLanguageTag
                                            )
                                        } / -${
                                            CurrencyFormatter.format(
                                                ct.expense,
                                                ct.currencyCode,
                                                preferences.numberFormat,
                                                hideAmount = preferences.hideBalances,
                                                includeDecimals = false,
                                                languageTag = preferences.effectiveLanguageTag
                                            )
                                        }",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.9f),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroMetricPill(
    label: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.06f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = tint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF9EA6B0),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun AccountMiniCard(
    account: Account,
    preferences: UserPreferences,
    onClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    val tint = Color(account.colorHex)
    PremiumCard(
        onClick = onClick,
        contentPadding = 14.dp,
        modifier = Modifier
            .width(166.dp)
            .testTag("home_account_card_${account.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            CategoryIconBadge(
                icon = CategoryIcons.forAccountType(account.type),
                tintColor = tint,
                size = 36.dp,
                contentDescription = account.name
            )
            Text(
                text = strings.accountTypeLabel(account.type),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = account.name,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = CurrencyFormatter.format(
                account.balance,
                preferences.effectiveCurrencyCode,
                preferences.numberFormat,
                hideAmount = preferences.hideBalances,
                languageTag = preferences.effectiveLanguageTag
            ),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = if (account.balance >= 0) MaterialTheme.colorScheme.onSurface else ExpenseRed,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MonthlyExpenseSummaryCard(
    monthYear: String,
    monthlyTotal: Double,
    previousMonthTotal: Double,
    breakdowns: List<CategoryBreakdown>,
    displayCurrencyCode: String,
    preferences: UserPreferences,
    onAddTransaction: () -> Unit
) {
    val strings = LocalAppStrings.current
    val monthLabel = DateTimeUtils.formatMonthLabel(
        monthYear,
        preferences.effectiveLanguageTag,
        preferences.numberFormat
    )
    val deltaPercent = if (previousMonthTotal > 0.0) {
        ((monthlyTotal - previousMonthTotal) / previousMonthTotal) * 100.0
    } else null

    PremiumCard(
        modifier = Modifier.testTag("dashboard_monthly_expense_summary_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.monthlyExpenseSummary,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$monthLabel • ${strings.spendingByCategory}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = CurrencyFormatter.format(
                        monthlyTotal,
                        displayCurrencyCode,
                        preferences.numberFormat,
                        hideAmount = preferences.hideBalances,
                        includeDecimals = false,
                        languageTag = preferences.effectiveLanguageTag
                    ),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = ExpenseRed,
                    modifier = Modifier.testTag("monthly_expense_total")
                )
                if (deltaPercent != null) {
                    val deltaColor = if (deltaPercent <= 0.0) IncomeGreen else ExpenseRed
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = deltaColor.copy(alpha = 0.14f),
                        modifier = Modifier
                            .semantics(mergeDescendants = true) {}
                            .testTag("monthly_expense_trend_badge")
                    ) {
                        Text(
                            text = "${
                                CurrencyFormatter.formatPercent(
                                    deltaPercent,
                                    preferences.numberFormat,
                                    showSign = true
                                )
                            } ${strings.vsLastMonth}",
                            style = MaterialTheme.typography.labelSmall,
                            color = deltaColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (breakdowns.isEmpty()) {
            EmptyStateView(
                icon = Icons.Rounded.BarChart,
                title = strings.noMonthlyExpensesTitle,
                subtitle = strings.noMonthlyExpensesSubtitle,
                actionLabel = strings.addTransaction,
                iconTint = MaterialTheme.colorScheme.primary,
                onAction = onAddTransaction
            )
        } else {
            CategoryExpenseBarChart(
                breakdowns = breakdowns,
                preferences = preferences,
                otherLabel = strings.otherCategory,
                currencyCode = displayCurrencyCode,
                modifier = Modifier.testTag("monthly_category_bar_chart")
            )

            val top = breakdowns.first()
            val topColor = Color(top.category?.colorHex ?: 0xFF8A9099)
            val topName = top.category?.localizedName(preferences.effectiveLanguageTag, strings)
                ?: strings.otherCategory
            val topIcon = top.category?.let { CategoryIcons.resolve(it.iconKey) } ?: Icons.Rounded.BarChart
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("monthly_top_category_summary")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        CategoryIconBadge(
                            icon = topIcon,
                            tintColor = topColor,
                            size = 32.dp,
                            contentDescription = topName
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = strings.topSpendingCategory,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = topName,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = CurrencyFormatter.format(
                                top.totalAmount,
                                displayCurrencyCode,
                                preferences.numberFormat,
                                hideAmount = preferences.hideBalances,
                                includeDecimals = false,
                                languageTag = preferences.effectiveLanguageTag
                            ),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = CurrencyFormatter.formatPercent(
                                (top.percentage * 100f).toDouble(),
                                preferences.numberFormat
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = topColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    actionText: String?,
    onAction: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (actionText != null && onAction != null) {
            Text(
                text = actionText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}
