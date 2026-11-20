package com.example.core.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.R
import com.example.core.common.CurrencyFormatter
import com.example.core.common.DateTimeUtils
import com.example.core.common.stringsFor
import com.example.core.datastore.PreferencesDataSource
import com.example.core.model.TransactionType
import com.example.core.repository.BudgetRepository
import com.example.core.repository.PreferencesRepository
import com.example.core.repository.TransactionRepository
import com.example.widget.ExpenseSummaryWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.GlobalContext
import java.util.Calendar
import java.util.concurrent.TimeUnit

object NotificationChannels {
    const val REMINDERS_CHANNEL_ID = "expense_daily_reminders"
    const val BUDGETS_CHANNEL_ID = "expense_budget_alerts"

    fun ensureCreated(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val reminderChannel = NotificationChannel(
                REMINDERS_CHANNEL_ID,
                context.getString(R.string.notification_channel_reminders_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_reminders_desc)
            }
            val budgetChannel = NotificationChannel(
                BUDGETS_CHANNEL_ID,
                context.getString(R.string.notification_channel_budgets_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_budgets_desc)
            }
            manager.createNotificationChannel(reminderChannel)
            manager.createNotificationChannel(budgetChannel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}

class BudgetAlertNotifier(
    private val context: Context,
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val preferencesRepository: PreferencesRepository
) {
    suspend fun evaluateAndNotify(categoryId: Long?, timestamp: Long) {
        val prefs = preferencesRepository.preferences.first()
        if (!prefs.budgetAlertsEnabled) return
        if (!NotificationChannels.hasNotificationPermission(context)) return

        val langTag = prefs.effectiveLanguageTag
        val strings = stringsFor(context, langTag)
        val currencyCode = prefs.effectiveCurrencyCode

        val monthYear = DateTimeUtils.currentMonthYear(timestamp)
        val budgets = budgetRepository.observeBudgetsForMonth(monthYear).first()
        if (budgets.isEmpty()) return

        val (mStart, mEnd) = DateTimeUtils.monthYearRange(monthYear)
        val monthExpenses = transactionRepository.getTransactionsWithDetails().filter {
            it.transaction.type == TransactionType.EXPENSE && it.transaction.timestamp in mStart..mEnd
        }

        val relevantBudgets = budgets.filter { it.categoryId == null || it.categoryId == categoryId }
        for (budget in relevantBudgets) {
            val spent = if (budget.categoryId == null) {
                monthExpenses.sumOf { it.transaction.amount }
            } else {
                monthExpenses.filter { it.transaction.categoryId == budget.categoryId }
                    .sumOf { it.transaction.amount }
            }
            if (budget.amountLimit <= 0.0) continue
            val ratio = (spent / budget.amountLimit) * 100.0
            if (ratio >= budget.alertThresholdPercent) {
                val categoryName = budget.categoryId?.let { cid ->
                    monthExpenses.firstOrNull { it.category?.id == cid }?.category?.localizedName(langTag, strings)
                } ?: strings.budgetMonthlyDefault

                val spentText = CurrencyFormatter.format(spent, currencyCode, prefs.numberFormat, languageTag = langTag)
                val limitText = CurrencyFormatter.format(budget.amountLimit, currencyCode, prefs.numberFormat, languageTag = langTag)
                val isOver = spent > budget.amountLimit
                val title = strings.budgetNotifTitle(categoryName, isOver)
                val body = strings.budgetNotifBody(spentText, limitText, ratio.toInt(), prefs.numberFormat)

                NotificationChannels.ensureCreated(context)
                val intent = Intent(context, MainActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    budget.id.toInt(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val notification = NotificationCompat.Builder(context, NotificationChannels.BUDGETS_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build()

                try {
                    NotificationManagerCompat.from(context)
                        .notify(2000 + budget.id.toInt(), notification)
                } catch (_: SecurityException) {
                }
            }
        }
    }
}

class DailyReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params), KoinComponent {

    private val preferencesRepository: PreferencesRepository by inject()

    override suspend fun doWork(): Result {
        val prefs = preferencesRepository.preferences.first()
        if (!prefs.dailyReminderEnabled || !NotificationChannels.hasNotificationPermission(applicationContext)) {
            return Result.success()
        }
        NotificationChannels.ensureCreated(applicationContext)

        val strings = stringsFor(applicationContext, prefs.effectiveLanguageTag)
        val title = strings.dailyReminderTitle
        val body = strings.dailyReminderBody

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_OPEN_ADD_TX, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, NotificationChannels.REMINDERS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(applicationContext).notify(1001, notification)
        } catch (_: SecurityException) {
        }
        return Result.success()
    }
}

class RecurringTransactionWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params), KoinComponent {

    private val transactionRepository: TransactionRepository by inject()

    override suspend fun doWork(): Result = runCatching {
        val added = transactionRepository.processDueRecurringTransactions()
        if (added > 0) {
            runCatching { ExpenseSummaryWidget().updateAll(applicationContext) }
        }
        Result.success()
    }.getOrElse {
        Result.retry()
    }
}

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }
        val pendingResult: PendingResult? = goAsync()
        val appContext = context.applicationContext ?: context
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = runCatching {
                    val koin = GlobalContext.getOrNull()
                    val repo = koin?.getOrNull<PreferencesRepository>()
                    if (repo != null) {
                        repo.preferences.first()
                    } else {
                        PreferencesDataSource(appContext).preferencesFlow.first()
                    }
                }.getOrNull()

                runCatching { WorkScheduler.scheduleRecurringWorker(appContext) }
                if (prefs != null) {
                    runCatching {
                        WorkScheduler.updateDailyReminder(
                            context = appContext,
                            enabled = prefs.dailyReminderEnabled,
                            hour = prefs.reminderHour,
                            minute = prefs.reminderMinute
                        )
                    }
                }
            } finally {
                pendingResult?.finish()
            }
        }
    }
}

object WorkScheduler {
    private const val DAILY_REMINDER_WORK = "daily_expense_reminder_work"
    private const val RECURRING_TX_WORK = "recurring_transactions_work"

    fun scheduleRecurringWorker(context: Context) {
        val request = PeriodicWorkRequestBuilder<RecurringTransactionWorker>(12, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            RECURRING_TX_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun updateDailyReminder(context: Context, enabled: Boolean, hour: Int, minute: Int) {
        val wm = WorkManager.getInstance(context)
        if (!enabled) {
            wm.cancelUniqueWork(DAILY_REMINDER_WORK)
            return
        }
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        val initialDelayMillis = (target.timeInMillis - now.timeInMillis).coerceAtLeast(60_000L)
        val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .build()
        wm.enqueueUniquePeriodicWork(
            DAILY_REMINDER_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}
