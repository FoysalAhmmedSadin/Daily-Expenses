package com.example.core.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.example.core.common.DateTimeUtils
import com.example.core.database.AccountDao
import com.example.core.database.AccountEntity
import com.example.core.database.BudgetDao
import com.example.core.database.BudgetEntity
import com.example.core.database.CategoryDao
import com.example.core.database.CategoryEntity
import com.example.core.database.ExpenseDatabase
import com.example.core.database.SeedData
import com.example.core.database.TransactionDao
import com.example.core.database.TransactionEntity
import com.example.core.datastore.PreferencesDataSource
import com.example.core.model.Account
import com.example.core.model.AppAccentColor
import com.example.core.model.AppCurrency
import com.example.core.model.AppLanguage
import com.example.core.model.BackupPayload
import com.example.core.model.Budget
import com.example.core.common.stringsFor
import com.example.core.model.Category
import com.example.core.model.NumberFormatStyle
import com.example.core.model.RecurrenceFrequency
import com.example.core.model.ThemeMode
import com.example.core.model.Transaction
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

class AccountRepositoryImpl(
    private val database: ExpenseDatabase,
    private val accountDao: AccountDao
) : AccountRepository {

    override fun observeAccounts(): Flow<List<Account>> =
        accountDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeAccountById(id: Long): Flow<Account?> =
        accountDao.observeById(id).map { it?.toDomain() }

    override suspend fun getAccounts(): List<Account> =
        accountDao.getAll().map { it.toDomain() }

    override suspend fun saveAccount(account: Account): Long = database.withTransaction {
        require(account.name.isNotBlank()) { "Account name cannot be blank" }
        if (account.id == 0L) {
            val entity = AccountEntity.fromDomain(
                account.copy(balance = account.initialBalance)
            )
            accountDao.insert(entity)
        } else {
            val existing = accountDao.getById(account.id)
            if (existing == null) {
                accountDao.insert(AccountEntity.fromDomain(account))
            } else {
                val delta = account.initialBalance - existing.initialBalance
                val updated = AccountEntity.fromDomain(
                    account.copy(balance = existing.balance + delta)
                )
                accountDao.update(updated)
                account.id
            }
        }
    }

    override suspend fun deleteAccount(id: Long) {
        accountDao.deleteById(id)
    }
}

class CategoryRepositoryImpl(
    private val categoryDao: CategoryDao
) : CategoryRepository {

    override fun observeCategories(): Flow<List<Category>> =
        categoryDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getCategories(): List<Category> =
        categoryDao.getAll().map { it.toDomain() }

    override suspend fun saveCategory(category: Category): Long {
        require(category.nameEn.isNotBlank()) { "Category name cannot be blank" }
        val normalized = category.copy(
            nameBn = category.nameBn.ifBlank { category.nameEn }
        )
        return if (normalized.id == 0L) {
            categoryDao.insert(CategoryEntity.fromDomain(normalized))
        } else {
            categoryDao.update(CategoryEntity.fromDomain(normalized))
            normalized.id
        }
    }

    override suspend fun deleteCategory(id: Long) {
        categoryDao.deleteById(id)
    }
}

class TransactionRepositoryImpl(
    private val database: ExpenseDatabase,
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao
) : TransactionRepository {

    override fun observeTransactionsWithDetails(): Flow<List<TransactionWithDetails>> = combine(
        transactionDao.observeAll(),
        categoryDao.observeAll(),
        accountDao.observeAll()
    ) { transactions, categories, accounts ->
        val categoryMap = categories.associateBy({ it.id }, { it.toDomain() })
        val accountMap = accounts.associateBy({ it.id }, { it.toDomain() })
        transactions.map { entity ->
            val domain = entity.toDomain()
            TransactionWithDetails(
                transaction = domain,
                category = domain.categoryId?.let { categoryMap[it] },
                account = accountMap[domain.accountId],
                toAccount = domain.toAccountId?.let { accountMap[it] }
            )
        }
    }

    override fun observeTransactionById(id: Long): Flow<TransactionWithDetails?> = combine(
        transactionDao.observeById(id),
        categoryDao.observeAll(),
        accountDao.observeAll()
    ) { entity, categories, accounts ->
        entity?.let {
            val domain = it.toDomain()
            val categoryMap = categories.associateBy({ c -> c.id }, { c -> c.toDomain() })
            val accountMap = accounts.associateBy({ a -> a.id }, { a -> a.toDomain() })
            TransactionWithDetails(
                transaction = domain,
                category = domain.categoryId?.let { cid -> categoryMap[cid] },
                account = accountMap[domain.accountId],
                toAccount = domain.toAccountId?.let { tid -> accountMap[tid] }
            )
        }
    }

    override suspend fun getTransactionsWithDetails(): List<TransactionWithDetails> {
        val transactions = transactionDao.getAll()
        val categoryMap = categoryDao.getAll().associateBy({ it.id }, { it.toDomain() })
        val accountMap = accountDao.getAll().associateBy({ it.id }, { it.toDomain() })
        return transactions.map { entity ->
            val domain = entity.toDomain()
            TransactionWithDetails(
                transaction = domain,
                category = domain.categoryId?.let { categoryMap[it] },
                account = accountMap[domain.accountId],
                toAccount = domain.toAccountId?.let { accountMap[it] }
            )
        }
    }

    override suspend fun saveTransaction(transaction: Transaction): Long = database.withTransaction {
        require(transaction.amount > 0.0) { "Amount must be greater than zero" }
        if (transaction.type == TransactionType.TRANSFER) {
            require(transaction.toAccountId != null && transaction.toAccountId != transaction.accountId) {
                "Transfer requires two distinct accounts"
            }
        }

        val computedNextRecurrence = when {
            transaction.recurrence == RecurrenceFrequency.NONE -> null
            transaction.nextRecurrenceAt != null -> transaction.nextRecurrenceAt
            else -> DateTimeUtils.nextRecurrenceTimestamp(transaction.timestamp, transaction.recurrence)
        }
        val normalized = transaction.copy(
            categoryId = if (transaction.type == TransactionType.TRANSFER) null else transaction.categoryId,
            toAccountId = if (transaction.type == TransactionType.TRANSFER) transaction.toAccountId else null,
            nextRecurrenceAt = computedNextRecurrence
        )

        if (normalized.id == 0L) {
            applyBalanceEffect(normalized, multiplier = 1.0)
            transactionDao.insert(TransactionEntity.fromDomain(normalized))
        } else {
            val existing = transactionDao.getById(normalized.id)?.toDomain()
            if (existing != null) {
                applyBalanceEffect(existing, multiplier = -1.0)
            }
            applyBalanceEffect(normalized, multiplier = 1.0)
            transactionDao.update(TransactionEntity.fromDomain(normalized))
            normalized.id
        }
    }

    override suspend fun deleteTransaction(id: Long) = database.withTransaction {
        val existing = transactionDao.getById(id)?.toDomain() ?: return@withTransaction
        applyBalanceEffect(existing, multiplier = -1.0)
        transactionDao.deleteById(id)
    }

    override suspend fun processDueRecurringTransactions(nowMillis: Long): Int =
        database.withTransaction {
            var createdCount = 0
            val dueList = transactionDao.getDueRecurring(nowMillis)
            for (entity in dueList) {
                val freq = runCatching { RecurrenceFrequency.valueOf(entity.recurrence) }
                    .getOrDefault(RecurrenceFrequency.NONE)
                if (freq == RecurrenceFrequency.NONE) {
                    transactionDao.update(entity.copy(nextRecurrenceAt = null))
                    continue
                }
                var scheduledAt = entity.nextRecurrenceAt ?: continue
                var guard = 0
                while (scheduledAt <= nowMillis && guard < 24) {
                    guard++
                    val alreadyExists = transactionDao.countRecurringInstance(entity.id, scheduledAt) > 0
                    if (!alreadyExists) {
                        val instance = Transaction(
                            id = 0L,
                            type = runCatching { TransactionType.valueOf(entity.type) }
                                .getOrDefault(TransactionType.EXPENSE),
                            amount = entity.amount,
                            categoryId = entity.categoryId,
                            accountId = entity.accountId,
                            toAccountId = entity.toAccountId,
                            note = entity.note,
                            timestamp = scheduledAt,
                            receiptUri = null,
                            recurrence = RecurrenceFrequency.NONE,
                            nextRecurrenceAt = null,
                            parentRecurringId = entity.id,
                            currencyCode = entity.currencyCode.ifBlank { "BDT" }
                        )
                        applyBalanceEffect(instance, multiplier = 1.0)
                        transactionDao.insert(TransactionEntity.fromDomain(instance))
                        createdCount++
                    }
                    scheduledAt = DateTimeUtils.nextRecurrenceTimestamp(scheduledAt, freq) ?: break
                }
                transactionDao.update(entity.copy(nextRecurrenceAt = scheduledAt))
            }
            createdCount
        }

    private suspend fun applyBalanceEffect(tx: Transaction, multiplier: Double) {
        val amt = tx.amount * multiplier
        when (tx.type) {
            TransactionType.EXPENSE -> accountDao.adjustBalance(tx.accountId, -amt)
            TransactionType.INCOME -> accountDao.adjustBalance(tx.accountId, +amt)
            TransactionType.TRANSFER -> {
                accountDao.adjustBalance(tx.accountId, -amt)
                tx.toAccountId?.let { targetId ->
                    accountDao.adjustBalance(targetId, +amt)
                }
            }
        }
    }
}

class BudgetRepositoryImpl(
    private val database: ExpenseDatabase,
    private val budgetDao: BudgetDao
) : BudgetRepository {

    override fun observeBudgetsForMonth(monthYear: String): Flow<List<Budget>> =
        budgetDao.observeByMonth(monthYear).map { list -> list.map { it.toDomain() } }

    override fun observeAllBudgets(): Flow<List<Budget>> =
        budgetDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun saveBudget(budget: Budget): Long = database.withTransaction {
        require(budget.amountLimit > 0.0) { "Budget limit must be positive" }
        val existingForCategory = budgetDao.findByMonthAndCategory(budget.monthYear, budget.categoryId)
        if (budget.id == 0L && existingForCategory != null) {
            val updated = existingForCategory.copy(
                amountLimit = budget.amountLimit,
                alertThresholdPercent = budget.alertThresholdPercent
            )
            budgetDao.update(updated)
            updated.id
        } else if (budget.id == 0L) {
            budgetDao.insert(BudgetEntity.fromDomain(budget))
        } else {
            budgetDao.update(BudgetEntity.fromDomain(budget))
            budget.id
        }
    }

    override suspend fun deleteBudget(id: Long) {
        budgetDao.deleteById(id)
    }
}

class PreferencesRepositoryImpl(
    private val dataSource: PreferencesDataSource
) : PreferencesRepository {
    override val preferences: Flow<UserPreferences> = dataSource.preferencesFlow
    override suspend fun setCurrency(currency: AppCurrency) = dataSource.setCurrency(currency)
    override suspend fun setCurrencyCode(code: String) = dataSource.setCurrencyCode(code)
    override suspend fun setLanguage(language: AppLanguage) = dataSource.setLanguage(language)
    override suspend fun setLanguageTag(tag: String) = dataSource.setLanguageTag(tag)
    override suspend fun setViewMode(viewMode: ViewMode) = dataSource.setViewMode(viewMode)
    override suspend fun setNumberFormat(style: NumberFormatStyle) = dataSource.setNumberFormat(style)
    override suspend fun setThemeMode(mode: ThemeMode) = dataSource.setThemeMode(mode)
    override suspend fun setAccentColor(color: AppAccentColor) = dataSource.setAccentColor(color)
    override suspend fun setBiometricLock(enabled: Boolean) = dataSource.setBiometricLock(enabled)
    override suspend fun setDailyReminder(enabled: Boolean, hour: Int?, minute: Int?) =
        dataSource.setDailyReminder(enabled, hour, minute)
    override suspend fun setBudgetAlerts(enabled: Boolean) = dataSource.setBudgetAlerts(enabled)
    override suspend fun setHideBalances(hidden: Boolean) = dataSource.setHideBalances(hidden)
    override suspend fun setLastBackupInfo(timestamp: Long, status: String) =
        dataSource.setLastBackupInfo(timestamp, status)
}

class BackupAndExportRepositoryImpl(
    private val context: Context,
    private val database: ExpenseDatabase,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao,
    private val transactionRepository: TransactionRepository,
    private val preferencesRepository: PreferencesRepository
) : BackupAndExportRepository {

    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun exportCsvToUri(uri: Uri, monthYear: String?): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val allTransactions = transactionRepository.getTransactionsWithDetails()
            val transactions = if (monthYear != null) {
                val (startMillis, endMillis) = DateTimeUtils.monthYearRange(monthYear)
                allTransactions.filter { it.transaction.timestamp in startMillis..endMillis }
            } else {
                allTransactions
            }
            val prefs = preferencesRepository.preferences.first()
            val strings = stringsFor(context, prefs.effectiveLanguageTag)
            context.contentResolver.openOutputStream(uri)?.use { out ->
                ExportManager.writeCsv(out, transactions, prefs, strings)
            } ?: error("Unable to open destination file.")
        }
    }

    override suspend fun exportPdfToUri(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val accounts = accountDao.getAll().map { it.toDomain() }
            val transactions = transactionRepository.getTransactionsWithDetails()
            val prefs = preferencesRepository.preferences.first()
            val strings = stringsFor(context, prefs.effectiveLanguageTag)
            context.contentResolver.openOutputStream(uri)?.use { out ->
                ExportManager.writePdf(out, accounts, transactions, prefs, strings)
            } ?: error("Unable to open destination file.")
        }
    }

    override suspend fun createEncryptedBackupToUri(uri: Uri, passphrase: String): Result<BackupPayload> =
        withContext(Dispatchers.IO) {
            runCatching {
                val prefs = preferencesRepository.preferences.first()
                val payload = BackupPayload(
                    schemaVersion = 3,
                    exportedAt = System.currentTimeMillis(),
                    accounts = accountDao.getAll().map { it.toDomain() },
                    categories = categoryDao.getAll().map { it.toDomain() },
                    transactions = transactionDao.getAll().map { it.toDomain() },
                    budgets = budgetDao.getAll().map { it.toDomain() },
                    preferences = prefs
                )
                val encoded = json.encodeToString(payload)
                val encrypted = BackupCrypto.encrypt(encoded, passphrase)
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(encrypted)
                    out.flush()
                } ?: error("Unable to write backup file.")
                preferencesRepository.setLastBackupInfo(
                    timestamp = payload.exportedAt,
                    status = "Encrypted backup (${payload.transactions.size} transactions)"
                )
                payload
            }
        }

    override suspend fun readEncryptedBackupFromUri(uri: Uri, passphrase: String): Result<BackupPayload> =
        withContext(Dispatchers.IO) {
            runCatching {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: error("Unable to read backup file.")
                val decoded = BackupCrypto.decrypt(bytes, passphrase)
                val payload = json.decodeFromString<BackupPayload>(decoded)
                validateBackupPayload(payload)
                payload
            }
        }

    override suspend fun restoreBackup(payload: BackupPayload): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            validateBackupPayload(payload)

            // Create a local safety snapshot before overwriting existing database records.
            val currentPrefs = preferencesRepository.preferences.first()
            val safetyPayload = BackupPayload(
                schemaVersion = 3,
                exportedAt = System.currentTimeMillis(),
                accounts = accountDao.getAll().map { it.toDomain() },
                categories = categoryDao.getAll().map { it.toDomain() },
                transactions = transactionDao.getAll().map { it.toDomain() },
                budgets = budgetDao.getAll().map { it.toDomain() },
                preferences = currentPrefs
            )
            val safetyDir = File(context.filesDir, "safety_backups").apply { mkdirs() }
            val safetyFile = File(safetyDir, "pre_restore_safety_${System.currentTimeMillis()}.json")
            safetyFile.writeText(json.encodeToString(safetyPayload))

            database.withTransaction {
                transactionDao.deleteAll()
                budgetDao.deleteAll()
                accountDao.deleteAll()
                categoryDao.deleteAll()

                categoryDao.insertAll(payload.categories.map { CategoryEntity.fromDomain(it) })
                accountDao.insertAll(payload.accounts.map { AccountEntity.fromDomain(it) })
                transactionDao.insertAll(payload.transactions.map { TransactionEntity.fromDomain(it) })
                budgetDao.insertAll(payload.budgets.map { BudgetEntity.fromDomain(it) })
            }

            payload.preferences?.let { restoredPrefs ->
                val resolvedCurrencyCode = if (payload.schemaVersion <= 2) {
                    restoredPrefs.currency.code
                } else {
                    restoredPrefs.effectiveCurrencyCode
                }
                val resolvedLanguageTag = if (payload.schemaVersion <= 2) {
                    restoredPrefs.language.code
                } else {
                    restoredPrefs.effectiveLanguageTag
                }
                preferencesRepository.setCurrencyCode(resolvedCurrencyCode)
                preferencesRepository.setLanguageTag(resolvedLanguageTag)
                preferencesRepository.setViewMode(restoredPrefs.viewMode)
                preferencesRepository.setNumberFormat(restoredPrefs.numberFormat)
                preferencesRepository.setThemeMode(restoredPrefs.themeMode)
                preferencesRepository.setAccentColor(restoredPrefs.accentColor)
                preferencesRepository.setDailyReminder(
                    restoredPrefs.dailyReminderEnabled,
                    restoredPrefs.reminderHour,
                    restoredPrefs.reminderMinute
                )
                preferencesRepository.setBudgetAlerts(restoredPrefs.budgetAlertsEnabled)
                preferencesRepository.setHideBalances(restoredPrefs.hideBalances)
            }
            preferencesRepository.setLastBackupInfo(
                timestamp = System.currentTimeMillis(),
                status = "Restored backup (${payload.transactions.size} transactions)"
            )
        }
    }

    override suspend fun clearAllFinancialData(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            database.withTransaction {
                transactionDao.deleteAll()
                budgetDao.deleteAll()
                accountDao.deleteAll()
                categoryDao.deleteAll()
                SeedData.populateDefaults(database)
            }
        }
    }

    override suspend fun copyReceiptImageToInternal(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val receiptsDir = File(context.filesDir, "receipts").apply { mkdirs() }
            val destFile = File(receiptsDir, "receipt_${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: error("Could not read selected image.")
            Uri.fromFile(destFile).toString()
        }
    }

    private fun validateBackupPayload(payload: BackupPayload) {
        BackupPayloadValidator.validate(payload)
    }
}

object BackupPayloadValidator {
    fun validate(payload: BackupPayload) {
        require(payload.schemaVersion in 1..3) { "Unsupported backup schema version: ${payload.schemaVersion}" }
        require(payload.accounts.isNotEmpty()) { "Backup contains no accounts." }
        require(payload.accounts.distinctBy { it.id }.size == payload.accounts.size) {
            "Backup contains duplicate account IDs."
        }
        require(payload.categories.distinctBy { it.id }.size == payload.categories.size) {
            "Backup contains duplicate category IDs."
        }
        require(payload.transactions.distinctBy { it.id }.size == payload.transactions.size) {
            "Backup contains duplicate transaction IDs."
        }
        require(payload.budgets.distinctBy { it.id }.size == payload.budgets.size) {
            "Backup contains duplicate budget IDs."
        }
        payload.accounts.forEach { acc ->
            require(acc.name.isNotBlank()) { "Backup contains an account with blank name." }
            require(acc.balance.isFinite() && acc.initialBalance.isFinite()) {
                "Backup contains invalid account balance."
            }
        }
        val accountIds = payload.accounts.map { it.id }.toSet()
        payload.transactions.forEach { tx ->
            require(tx.amount.isFinite() && tx.amount > 0.0) { "Backup contains invalid transaction amount." }
            require(tx.accountId in accountIds) { "Backup transaction references missing account." }
            if (tx.type == TransactionType.TRANSFER) {
                require(tx.toAccountId != null && tx.toAccountId in accountIds && tx.toAccountId != tx.accountId) {
                    "Backup transfer references invalid target account."
                }
            }
        }
    }
}
