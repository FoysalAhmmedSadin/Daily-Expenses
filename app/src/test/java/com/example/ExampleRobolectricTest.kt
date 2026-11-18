package com.example

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.common.DateTimeUtils
import com.example.core.data.AccountRepositoryImpl
import com.example.core.data.BackupAndExportRepositoryImpl
import com.example.core.data.BudgetRepositoryImpl
import com.example.core.data.CategoryRepositoryImpl
import com.example.core.data.PreferencesRepositoryImpl
import com.example.core.data.TransactionRepositoryImpl
import com.example.core.database.ExpenseDatabase
import com.example.core.database.SeedData
import com.example.core.datastore.PreferencesDataSource
import com.example.core.model.Account
import com.example.core.model.AccountType
import com.example.core.model.AppCurrency
import com.example.core.model.Budget
import com.example.core.model.RecurrenceFrequency
import com.example.core.model.ThemeMode
import com.example.core.model.Transaction
import com.example.core.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: ExpenseDatabase
    private lateinit var accountRepo: AccountRepositoryImpl
    private lateinit var categoryRepo: CategoryRepositoryImpl
    private lateinit var txRepo: TransactionRepositoryImpl
    private lateinit var budgetRepo: BudgetRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ExpenseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        accountRepo = AccountRepositoryImpl(db, db.accountDao())
        categoryRepo = CategoryRepositoryImpl(db.categoryDao())
        txRepo = TransactionRepositoryImpl(db, db.transactionDao(), db.accountDao(), db.categoryDao())
        budgetRepo = BudgetRepositoryImpl(db, db.budgetDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Daily Expenses", appName)
    }

    @Test
    fun `income expense transfer and edit delete maintain exact account balances`() = runTest {
        SeedData.populateDefaults(db)
        val cashId = accountRepo.saveAccount(
            Account(name = "Test Cash", type = AccountType.CASH, initialBalance = 1000.0)
        )
        val bankId = accountRepo.saveAccount(
            Account(name = "Test Bank", type = AccountType.BANK, initialBalance = 5000.0)
        )
        val expenseCatId = categoryRepo.getCategories().first { it.type == TransactionType.EXPENSE }.id
        val incomeCatId = categoryRepo.getCategories().first { it.type == TransactionType.INCOME }.id

        // Income +2000 to Cash -> 3000
        val incomeId = txRepo.saveTransaction(
            Transaction(
                type = TransactionType.INCOME,
                amount = 2000.0,
                categoryId = incomeCatId,
                accountId = cashId
            )
        )
        assertEquals(3000.0, accountRepo.observeAccountById(cashId).first()!!.balance, 0.001)

        // Expense -500 from Cash -> 2500
        val expId = txRepo.saveTransaction(
            Transaction(
                type = TransactionType.EXPENSE,
                amount = 500.0,
                categoryId = expenseCatId,
                accountId = cashId
            )
        )
        assertEquals(2500.0, accountRepo.observeAccountById(cashId).first()!!.balance, 0.001)

        // Transfer 1500 from Bank -> Cash (Bank=3500, Cash=4000)
        txRepo.saveTransaction(
            Transaction(
                type = TransactionType.TRANSFER,
                amount = 1500.0,
                categoryId = null,
                accountId = bankId,
                toAccountId = cashId
            )
        )
        assertEquals(3500.0, accountRepo.observeAccountById(bankId).first()!!.balance, 0.001)
        assertEquals(4000.0, accountRepo.observeAccountById(cashId).first()!!.balance, 0.001)

        // Edit Expense from 500 -> 800 (Cash -> 3700)
        val existingExp = txRepo.observeTransactionById(expId).first()!!.transaction
        txRepo.saveTransaction(existingExp.copy(amount = 800.0))
        assertEquals(3700.0, accountRepo.observeAccountById(cashId).first()!!.balance, 0.001)

        // Delete Income 2000 (Cash -> 1700)
        txRepo.deleteTransaction(incomeId)
        assertEquals(1700.0, accountRepo.observeAccountById(cashId).first()!!.balance, 0.001)
    }

    @Test
    fun `recurring transactions process without duplicates and budgets enforce unique month category`() = runTest {
        val accId = accountRepo.saveAccount(
            Account(name = "Wallet", type = AccountType.CASH, initialBalance = 5000.0)
        )
        val twoDaysAgo = System.currentTimeMillis() - 2 * 86_400_000L
        txRepo.saveTransaction(
            Transaction(
                type = TransactionType.EXPENSE,
                amount = 100.0,
                categoryId = null,
                accountId = accId,
                timestamp = twoDaysAgo,
                recurrence = RecurrenceFrequency.DAILY,
                nextRecurrenceAt = twoDaysAgo + 86_400_000L
            )
        )

        val createdFirst = txRepo.processDueRecurringTransactions()
        assertEquals(2, createdFirst)
        val createdSecond = txRepo.processDueRecurringTransactions()
        assertEquals(0, createdSecond)
        assertEquals(4700.0, accountRepo.observeAccountById(accId).first()!!.balance, 0.001)

        // Overall monthly budget deduplication
        val b1 = budgetRepo.saveBudget(Budget(categoryId = null, amountLimit = 10000.0, monthYear = "2026-11"))
        val b2 = budgetRepo.saveBudget(Budget(categoryId = null, amountLimit = 12000.0, monthYear = "2026-11"))
        assertEquals(b1, b2)
        val budgets = budgetRepo.observeBudgetsForMonth("2026-11").first()
        assertTrue(budgets.size == 1 && budgets.first().amountLimit == 12000.0)
    }

    @Test
    fun `encrypted v2 backup creates safety snapshot and restores data and preferences`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefsRepo = PreferencesRepositoryImpl(PreferencesDataSource(context))
        val backupRepo = BackupAndExportRepositoryImpl(
            context = context,
            database = db,
            accountDao = db.accountDao(),
            categoryDao = db.categoryDao(),
            transactionDao = db.transactionDao(),
            budgetDao = db.budgetDao(),
            transactionRepository = txRepo,
            preferencesRepository = prefsRepo
        )

        SeedData.populateDefaults(db)
        val cashId = accountRepo.saveAccount(
            Account(name = "Primary Cash", type = AccountType.CASH, initialBalance = 2500.0)
        )
        txRepo.saveTransaction(
            Transaction(
                type = TransactionType.EXPENSE,
                amount = 250.0,
                categoryId = categoryRepo.getCategories().first { it.type == TransactionType.EXPENSE }.id,
                accountId = cashId,
                note = "Coffee"
            )
        )
        prefsRepo.setCurrency(AppCurrency.USD)
        prefsRepo.setThemeMode(ThemeMode.DARK)

        val backupFile = File(context.cacheDir, "test_v2_backup.embackup")
        val uri = Uri.fromFile(backupFile)
        val created = backupRepo.createEncryptedBackupToUri(uri, "MyPass2026").getOrThrow()
        assertEquals(3, created.schemaVersion)
        assertNotNull(created.preferences)
        assertEquals(AppCurrency.USD, created.preferences?.currency)

        // Mutate current data and preferences before restoring
        prefsRepo.setCurrency(AppCurrency.EUR)
        prefsRepo.setThemeMode(ThemeMode.LIGHT)
        backupRepo.clearAllFinancialData().getOrThrow()

        val safetyDir = File(context.filesDir, "safety_backups").apply {
            listFiles()?.forEach { it.delete() }
        }
        val readPayload = backupRepo.readEncryptedBackupFromUri(uri, "MyPass2026").getOrThrow()
        backupRepo.restoreBackup(readPayload).getOrThrow()

        val safetyFiles = safetyDir.listFiles()?.toList().orEmpty()
        assertTrue(safetyFiles.any { it.name.startsWith("pre_restore_safety_") })

        val restoredAccounts = accountRepo.getAccounts()
        assertTrue(restoredAccounts.any { it.name == "Primary Cash" && it.balance == 2250.0 })
        val restoredPrefs = prefsRepo.preferences.first()
        assertEquals(AppCurrency.USD, restoredPrefs.currency)
        assertEquals(ThemeMode.DARK, restoredPrefs.themeMode)
        assertNotNull(restoredPrefs.lastLocalBackupAt)
    }

    @Test
    fun `exportCsvToUri filters transactions by selected month`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefsRepo = PreferencesRepositoryImpl(PreferencesDataSource(context))
        val backupRepo = BackupAndExportRepositoryImpl(
            context = context,
            database = db,
            accountDao = db.accountDao(),
            categoryDao = db.categoryDao(),
            transactionDao = db.transactionDao(),
            budgetDao = db.budgetDao(),
            transactionRepository = txRepo,
            preferencesRepository = prefsRepo
        )

        SeedData.populateDefaults(db)
        val cashId = accountRepo.saveAccount(
            Account(name = "Wallet", type = AccountType.CASH, initialBalance = 5000.0)
        )
        val expenseCatId = categoryRepo.getCategories().first { it.type == TransactionType.EXPENSE }.id

        val (octStart, _) = DateTimeUtils.monthYearRange("2026-10")
        val (novStart, _) = DateTimeUtils.monthYearRange("2026-11")

        txRepo.saveTransaction(
            Transaction(
                type = TransactionType.EXPENSE,
                amount = 180.0,
                categoryId = expenseCatId,
                accountId = cashId,
                note = "October Groceries",
                timestamp = octStart + 86_400_000L
            )
        )
        txRepo.saveTransaction(
            Transaction(
                type = TransactionType.EXPENSE,
                amount = 420.0,
                categoryId = expenseCatId,
                accountId = cashId,
                note = "November Rent",
                timestamp = novStart + 86_400_000L
            )
        )

        val octCsvFile = File(context.cacheDir, "export_2026_10.csv")
        val octCount = backupRepo.exportCsvToUri(Uri.fromFile(octCsvFile), monthYear = "2026-10").getOrThrow()
        assertEquals(1, octCount)
        val octText = octCsvFile.readText(Charsets.UTF_8)
        assertTrue(octText.contains("Date,Time,Type,Category,Account,To Account,Amount,Currency,Note,Recurring"))
        assertTrue(octText.contains("October Groceries"))
        assertTrue(!octText.contains("November Rent"))

        val allCsvFile = File(context.cacheDir, "export_all.csv")
        val allCount = backupRepo.exportCsvToUri(Uri.fromFile(allCsvFile), monthYear = null).getOrThrow()
        assertEquals(2, allCount)
    }
}
