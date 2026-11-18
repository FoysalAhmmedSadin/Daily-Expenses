package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.common.CategoryIcons
import com.example.core.common.CurrencyFormatter
import com.example.core.common.LocalAppStrings
import com.example.core.model.AnalyticsGranularity
import com.example.ui.components.CategoryDonutChart
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.EmptyStateView
import com.example.ui.components.IncomeVsExpenseBarChart
import com.example.ui.components.PremiumCard
import com.example.ui.components.SpendingTrendLineChart
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    onAddTransaction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val strings = LocalAppStrings.current
    val prefs = state.preferences
    val langTag = prefs.effectiveLanguageTag
    val currencyCode = prefs.effectiveCurrencyCode
    val report = state.report
    var breakdownTab by remember { mutableIntStateOf(0) } // 0 = Expense, 1 = Income

    Box(
        modifier = modifier.fillMaxSize(),
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
                    text = strings.analytics,
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            // Granularity Switcher: Daily, Weekly, Monthly, Yearly
            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    AnalyticsGranularity.entries.forEachIndexed { idx, gran ->
                        val label = when (gran) {
                            AnalyticsGranularity.DAILY -> strings.daily
                            AnalyticsGranularity.WEEKLY -> strings.weekly
                            AnalyticsGranularity.MONTHLY -> strings.monthly
                            AnalyticsGranularity.YEARLY -> strings.yearly
                        }
                        SegmentedButton(
                            selected = state.granularity == gran,
                            onClick = { viewModel.setGranularity(gran) },
                            shape = SegmentedButtonDefaults.itemShape(idx, AnalyticsGranularity.entries.size),
                            modifier = Modifier.testTag("analytics_gran_${gran.name.lowercase()}")
                        ) {
                            Text(label, maxLines = 1)
                        }
                    }
                }
            }

            // KPI Summary Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatTileCard(
                            label = strings.totalIncome,
                            value = CurrencyFormatter.format(
                                report.totalIncome,
                                currencyCode,
                                prefs.numberFormat,
                                hideAmount = prefs.hideBalances
                            ),
                            accent = IncomeGreen,
                            modifier = Modifier.weight(1f)
                        )
                        StatTileCard(
                            label = strings.totalExpenses,
                            value = CurrencyFormatter.format(
                                report.totalExpense,
                                currencyCode,
                                prefs.numberFormat,
                                hideAmount = prefs.hideBalances
                            ),
                            accent = ExpenseRed,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatTileCard(
                            label = strings.savingsRate,
                            value = CurrencyFormatter.formatPercent(
                                report.savingsRatePercent,
                                prefs.numberFormat,
                                showSign = false
                            ),
                            accent = if (report.savingsRatePercent >= 0) IncomeGreen else ExpenseRed,
                            modifier = Modifier.weight(1f)
                        )
                        StatTileCard(
                            label = strings.avgDailySpend,
                            value = CurrencyFormatter.format(
                                report.averageDailyExpense,
                                currencyCode,
                                prefs.numberFormat,
                                hideAmount = prefs.hideBalances
                            ),
                            accent = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Previous Month Comparison Card
            item {
                val comp = report.monthComparison
                PremiumCard {
                    Text(
                        text = strings.vsLastMonth,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ComparisonPill(
                            label = strings.income,
                            currentValue = CurrencyFormatter.format(
                                comp.currentIncome,
                                currencyCode,
                                prefs.numberFormat,
                                hideAmount = prefs.hideBalances,
                                includeDecimals = false
                            ),
                            changePercent = comp.incomeChangePercent,
                            positiveIsGood = true,
                            prefs = prefs,
                            modifier = Modifier.weight(1f)
                        )
                        ComparisonPill(
                            label = strings.expense,
                            currentValue = CurrencyFormatter.format(
                                comp.currentExpense,
                                currencyCode,
                                prefs.numberFormat,
                                hideAmount = prefs.hideBalances,
                                includeDecimals = false
                            ),
                            changePercent = comp.expenseChangePercent,
                            positiveIsGood = false,
                            prefs = prefs,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Income vs Expense Bar Chart
            item {
                PremiumCard {
                    Text(
                        text = strings.incomeVsExpense,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (report.chartPoints.all { it.income == 0.0 && it.expense == 0.0 }) {
                        EmptyStateView(
                            icon = Icons.Rounded.BarChart,
                            title = strings.noTransactionsTitle,
                            subtitle = strings.noTransactionsSubtitle,
                            actionLabel = strings.addTransaction,
                            onAction = onAddTransaction
                        )
                    } else {
                        IncomeVsExpenseBarChart(
                            points = report.chartPoints,
                            preferences = prefs,
                            incomeLabel = strings.income,
                            expenseLabel = strings.expense,
                            currencyCode = currencyCode
                        )
                    }
                }
            }

            // Spending Trends Line Chart
            item {
                PremiumCard {
                    Text(
                        text = strings.spendingTrends,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SpendingTrendLineChart(
                        points = report.chartPoints,
                        preferences = prefs,
                        currencyCode = currencyCode
                    )
                }
            }

            // Category Breakdown (Expense / Income switch)
            item {
                val activeBreakdown = if (breakdownTab == 0) report.expenseBreakdown else report.incomeBreakdown
                PremiumCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.categoryBreakdown,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                onClick = { breakdownTab = 0 },
                                shape = RoundedCornerShape(10.dp),
                                color = if (breakdownTab == 0) ExpenseRed.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = strings.expense,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (breakdownTab == 0) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                            Surface(
                                onClick = { breakdownTab = 1 },
                                shape = RoundedCornerShape(10.dp),
                                color = if (breakdownTab == 1) IncomeGreen.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = strings.income,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (breakdownTab == 1) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    if (activeBreakdown.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Rounded.BarChart,
                            title = strings.noTransactionsTitle,
                            subtitle = strings.noTransactionsSubtitle
                        )
                    } else {
                        CategoryDonutChart(
                            breakdowns = activeBreakdown,
                            preferences = prefs,
                            totalLabel = if (breakdownTab == 0) strings.expense else strings.income,
                            currencyCode = currencyCode
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            activeBreakdown.forEach { item ->
                                val tint = Color(item.category?.colorHex ?: 0xFF8A9099)
                                val name = item.category?.localizedName(langTag, strings) ?: strings.otherCategory
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CategoryIconBadge(
                                            icon = CategoryIcons.resolve(item.category?.iconKey),
                                            tintColor = tint,
                                            size = 34.dp,
                                            contentDescription = name
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = CurrencyFormatter.format(
                                                item.totalAmount,
                                                currencyCode,
                                                prefs.numberFormat,
                                                hideAmount = prefs.hideBalances
                                            ),
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { item.percentage.coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = tint,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                        strokeCap = StrokeCap.Round
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
private fun StatTileCard(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    PremiumCard(modifier = modifier, contentPadding = 14.dp) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = accent
        )
    }
}

@Composable
private fun ComparisonPill(
    label: String,
    currentValue: String,
    changePercent: Double?,
    positiveIsGood: Boolean,
    prefs: com.example.core.model.UserPreferences,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = currentValue,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(4.dp))
            val badgeText = if (changePercent == null) {
                "—"
            } else {
                CurrencyFormatter.formatPercent(changePercent, prefs.numberFormat, showSign = true)
            }
            val badgeColor = when {
                changePercent == null -> MaterialTheme.colorScheme.onSurfaceVariant
                positiveIsGood -> if (changePercent >= 0) IncomeGreen else ExpenseRed
                else -> if (changePercent <= 0) IncomeGreen else ExpenseRed
            }
            Text(
                text = badgeText,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = badgeColor
            )
        }
    }
}
