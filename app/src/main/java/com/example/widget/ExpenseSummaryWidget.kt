package com.example.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.MainActivity
import com.example.core.common.CurrencyFormatter
import com.example.core.common.DateTimeUtils
import com.example.core.common.stringsFor
import com.example.core.database.ExpenseDatabase
import com.example.core.datastore.PreferencesDataSource
import com.example.core.model.DateFilterPeriod
import com.example.core.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.koin.core.context.GlobalContext

class ExpenseSummaryWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = withContext(Dispatchers.IO) {
            val koin = GlobalContext.getOrNull()
            val db = koin?.get<ExpenseDatabase>() ?: ExpenseDatabase.build(context)
            val prefsSource = koin?.get<PreferencesDataSource>() ?: PreferencesDataSource(context)
            val prefs = prefsSource.preferencesFlow.first()
            val accounts = db.accountDao().getAll()
            val transactions = db.transactionDao().getAll()

            val totalBalance = accounts.sumOf { it.balance }
            val (todayStart, todayEnd) = DateTimeUtils.rangeForPeriod(DateFilterPeriod.TODAY)
            val (monthStart, monthEnd) = DateTimeUtils.rangeForPeriod(DateFilterPeriod.THIS_MONTH)
            val currencyCode = prefs.effectiveCurrencyCode
            val strings = stringsFor(context, prefs.effectiveLanguageTag)

            val todayExpense = transactions
                .filter { it.type == TransactionType.EXPENSE.name && it.timestamp in todayStart..todayEnd }
                .sumOf { it.amount }
            val monthExpense = transactions
                .filter { it.type == TransactionType.EXPENSE.name && it.timestamp in monthStart..monthEnd }
                .sumOf { it.amount }

            WidgetData(
                balanceText = CurrencyFormatter.format(
                    totalBalance,
                    currencyCode,
                    prefs.numberFormat,
                    hideAmount = prefs.hideBalances,
                    languageTag = prefs.effectiveLanguageTag
                ),
                todayExpenseText = CurrencyFormatter.format(
                    todayExpense,
                    currencyCode,
                    prefs.numberFormat,
                    hideAmount = prefs.hideBalances,
                    languageTag = prefs.effectiveLanguageTag
                ),
                monthExpenseText = CurrencyFormatter.format(
                    monthExpense,
                    currencyCode,
                    prefs.numberFormat,
                    hideAmount = prefs.hideBalances,
                    languageTag = prefs.effectiveLanguageTag
                ),
                balanceLabel = strings.totalBalance,
                todayLabel = strings.today,
                monthLabel = strings.thisMonth,
                addLabel = "+ ${strings.addTransaction}"
            )
        }

        provideContent {
            GlanceTheme {
                WidgetContent(context, state)
            }
        }
    }

    @Composable
    private fun WidgetContent(context: Context, data: WidgetData) {
        val openHomeIntent = Intent(context, MainActivity::class.java)
        val openAddIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_OPEN_ADD_TX, true)
        }

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0xFF101114))
                .cornerRadius(20.dp)
                .padding(16.dp)
                .clickable(actionStartActivity(openHomeIntent)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = data.balanceLabel,
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF8A9099)),
                            fontSize = 12.sp
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(2.dp))
                    Text(
                        text = data.balanceText,
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFFFFFFF)),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Column(
                    modifier = GlanceModifier
                        .background(Color(0xFF34C759))
                        .cornerRadius(14.dp)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .clickable(actionStartActivity(openAddIntent)),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = data.addLabel,
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF101114)),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(12.dp))

            Row(modifier = GlanceModifier.fillMaxWidth()) {
                Column(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .background(Color(0xFF181A1F))
                        .cornerRadius(12.dp)
                        .padding(10.dp)
                ) {
                    Text(
                        text = data.todayLabel,
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF8A9099)),
                            fontSize = 11.sp
                        )
                    )
                    Text(
                        text = data.todayExpenseText,
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFFF3B30)),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Spacer(modifier = GlanceModifier.width(8.dp))
                Column(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .background(Color(0xFF181A1F))
                        .cornerRadius(12.dp)
                        .padding(10.dp)
                ) {
                    Text(
                        text = data.monthLabel,
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF8A9099)),
                            fontSize = 11.sp
                        )
                    )
                    Text(
                        text = data.monthExpenseText,
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF00FF88)),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }

    private data class WidgetData(
        val balanceText: String,
        val todayExpenseText: String,
        val monthExpenseText: String,
        val balanceLabel: String,
        val todayLabel: String,
        val monthLabel: String,
        val addLabel: String
    )
}

class ExpenseSummaryWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ExpenseSummaryWidget()
}
