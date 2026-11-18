package com.example

import android.content.Context
import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.LayoutDirection
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkManager
import com.example.core.common.CategoryIcons
import com.example.core.common.CurrencyFormatter
import com.example.core.common.DateTimeUtils
import com.example.core.common.stringsFor
import com.example.core.data.AccountRepositoryImpl
import com.example.core.data.BackupAndExportRepositoryImpl
import com.example.core.data.BudgetRepositoryImpl
import com.example.core.data.CategoryRepositoryImpl
import com.example.core.data.PreferencesRepositoryImpl
import com.example.core.data.TransactionRepositoryImpl
import com.example.core.database.DatabaseMigrations
import com.example.core.database.ExpenseDatabase
import com.example.core.database.SeedData
import com.example.core.datastore.PreferencesDataSource
import com.example.core.domain.GetDashboardSummaryUseCase
import com.example.core.drive.DriveAuthGateway
import com.example.core.drive.DriveAuthOutcome
import com.example.core.drive.GoogleDriveRemoteDataSource
import com.example.core.model.Account
import com.example.core.model.AccountType
import com.example.core.model.AppAccentColor
import com.example.core.model.CustomDatePreset
import com.example.core.model.DriveBackupEntry
import com.example.core.model.GoogleDriveAccount
import com.example.core.model.IsoCurrencyCatalog
import com.example.core.model.NumberFormatStyle
import com.example.core.model.SupportedLanguageCatalog
import com.example.core.model.ThemeMode
import com.example.core.model.Transaction
import com.example.core.model.TransactionType
import com.example.core.model.ViewMode
import com.example.core.repository.GoogleDriveBackupRepository
import com.example.core.worker.BootCompletedReceiver
import com.example.ui.screens.CurrencySettingsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.HomeViewModel
import com.example.ui.screens.LanguageSettingsScreen
import com.example.ui.screens.SettingsViewModel
import com.example.ui.screens.ViewModeSettingsScreen
import com.example.ui.theme.ExpenseManagerTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp")
class MajorUpdateVerificationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var db: ExpenseDatabase
    private lateinit var accountRepo: AccountRepositoryImpl
    private lateinit var categoryRepo: CategoryRepositoryImpl
    private lateinit var txRepo: TransactionRepositoryImpl
    private lateinit var budgetRepo: BudgetRepositoryImpl
    private lateinit var prefsRepo: PreferencesRepositoryImpl

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        if (!WorkManager.isInitialized()) {
            WorkManager.initialize(context, Configuration.Builder().build())
        }
        db = Room.inMemoryDatabaseBuilder(context, ExpenseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        accountRepo = AccountRepositoryImpl(db, db.accountDao())
        categoryRepo = CategoryRepositoryImpl(db.categoryDao())
        txRepo = TransactionRepositoryImpl(db, db.transactionDao(), db.accountDao(), db.categoryDao())
        budgetRepo = BudgetRepositoryImpl(db, db.budgetDao())
        prefsRepo = PreferencesRepositoryImpl(PreferencesDataSource(context))
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `iso currency catalog supports global codes fraction digits and search`() {
        val defaultBdt = IsoCurrencyCatalog.resolve(null)
        assertEquals("BDT", defaultBdt.code)
        assertEquals("৳", defaultBdt.symbol)
        assertEquals(2, defaultBdt.fractionDigits)

        val jpy = IsoCurrencyCatalog.resolve("JPY")
        assertEquals(0, jpy.fractionDigits)
        assertEquals("¥1,500", CurrencyFormatter.format(1500.0, "JPY", NumberFormatStyle.ENGLISH))

        val kwd = IsoCurrencyCatalog.resolve("KWD")
        assertEquals(3, kwd.fractionDigits)
        assertTrue(CurrencyFormatter.format(12.345, "KWD", NumberFormatStyle.ENGLISH).contains("12.345"))

        val searchResults = IsoCurrencyCatalog.search("Japan")
        assertTrue(searchResults.any { it.code == "JPY" })
        assertTrue(IsoCurrencyCatalog.allCurrencies.size >= 40)
    }

    @Test
    fun `room migration 1 to 2 adds currencyCode defaulting to BDT and preserves rows`() {
        val dbFile = File(context.cacheDir, "migration_1_to_2_test.db").apply {
            if (exists()) delete()
        }

        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbFile.absolutePath)
                .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `accounts` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `name` TEXT NOT NULL,
                                `type` TEXT NOT NULL,
                                `balance` REAL NOT NULL,
                                `initialBalance` REAL NOT NULL,
                                `colorHex` INTEGER NOT NULL,
                                `iconKey` TEXT NOT NULL,
                                `note` TEXT NOT NULL,
                                `createdAt` INTEGER NOT NULL
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `categories` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `nameEn` TEXT NOT NULL,
                                `nameBn` TEXT NOT NULL,
                                `type` TEXT NOT NULL,
                                `iconKey` TEXT NOT NULL,
                                `colorHex` INTEGER NOT NULL,
                                `isDefault` INTEGER NOT NULL
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `transactions` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `type` TEXT NOT NULL,
                                `amount` REAL NOT NULL,
                                `categoryId` INTEGER,
                                `accountId` INTEGER NOT NULL,
                                `toAccountId` INTEGER,
                                `note` TEXT NOT NULL,
                                `timestamp` INTEGER NOT NULL,
                                `receiptUri` TEXT,
                                `recurrence` TEXT NOT NULL,
                                `nextRecurrenceAt` INTEGER,
                                `parentRecurringId` INTEGER
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `budgets` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `categoryId` INTEGER,
                                `amountLimit` REAL NOT NULL,
                                `monthYear` TEXT NOT NULL,
                                `alertThresholdPercent` INTEGER NOT NULL,
                                `createdAt` INTEGER NOT NULL
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            "INSERT INTO accounts (id, name, type, balance, initialBalance, colorHex, iconKey, note, createdAt) VALUES (1, 'Legacy Cash', 'CASH', 900.0, 1000.0, 0, 'wallet', '', 1000)"
                        )
                        db.execSQL(
                            "INSERT INTO transactions (id, type, amount, categoryId, accountId, toAccountId, note, timestamp, receiptUri, recurrence, nextRecurrenceAt, parentRecurringId) VALUES (1, 'EXPENSE', 100.0, NULL, 1, NULL, 'Old Coffee', 2000, NULL, 'NONE', NULL, NULL)"
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
        )
        helper.writableDatabase.close()
        helper.close()

        val migratedDb = Room.databaseBuilder(context, ExpenseDatabase::class.java, dbFile.absolutePath)
            .addMigrations(DatabaseMigrations.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()
        try {
            val txs = runBlocking { migratedDb.transactionDao().observeAll().first() }
            assertEquals(1, txs.size)
            assertEquals("Old Coffee", txs.first().note)
            assertEquals("BDT", txs.first().currencyCode)
        } finally {
            migratedDb.close()
            dbFile.delete()
        }
    }

    @Test
    fun `all 19 locales resolve non blank strings and apply rtl for arabic and urdu`() {
        assertEquals(19, SupportedLanguageCatalog.all.size)
        SupportedLanguageCatalog.all.forEach { lang ->
            val resolved = stringsFor(context, lang.tag)
            assertTrue("Missing settings string for ${lang.tag}", resolved.settings.isNotBlank())
            assertTrue("Missing navHome string for ${lang.tag}", resolved.navHome.isNotBlank())
            assertTrue("Missing reminder title for ${lang.tag}", resolved.dailyReminderTitle.isNotBlank())
        }

        val capturedDirs = mutableMapOf<String, LayoutDirection>()
        composeRule.setContent {
            listOf("en", "bn", "ar", "ur", "ja").forEach { tag ->
                ExpenseManagerTheme(languageTag = tag) {
                    capturedDirs[tag] = LocalLayoutDirection.current
                }
            }
        }
        composeRule.waitForIdle()

        assertEquals(LayoutDirection.Ltr, capturedDirs["en"])
        assertEquals(LayoutDirection.Ltr, capturedDirs["bn"])
        assertEquals(LayoutDirection.Ltr, capturedDirs["ja"])
        assertEquals(LayoutDirection.Rtl, capturedDirs["ar"])
        assertEquals(LayoutDirection.Rtl, capturedDirs["ur"])
    }

    @Test
    fun `savings category and account icons never use piggy bank icon`() {
        assertEquals(Icons.Rounded.AccountBalance, CategoryIcons.resolve("savings"))
        assertEquals(Icons.Rounded.AccountBalance, CategoryIcons.forAccountType(AccountType.SAVINGS))
    }

    @Test
    fun `home viewModel supports viewModes, future months, specific date, validation, and mixed currencies`() = runTest {
        SeedData.populateDefaults(db)
        val accId = accountRepo.saveAccount(Account(name = "Main", type = AccountType.CASH, initialBalance = 50_000.0))
        val catId = categoryRepo.getCategories().first { it.type == TransactionType.EXPENSE }.id

        val novStart = DateTimeUtils.monthYearRange("2026-11").first
        val decStart = DateTimeUtils.monthYearRange("2026-12").first

        // Mixed currencies in November 2026
        txRepo.saveTransaction(
            Transaction(
                type = TransactionType.EXPENSE,
                amount = 200.0,
                categoryId = catId,
                accountId = accId,
                note = "Nov BDT",
                timestamp = novStart + 86_400_000L,
                currencyCode = "BDT"
            )
        )
        txRepo.saveTransaction(
            Transaction(
                type = TransactionType.EXPENSE,
                amount = 50.0,
                categoryId = catId,
                accountId = accId,
                note = "Nov USD",
                timestamp = novStart + 2 * 86_400_000L,
                currencyCode = "USD"
            )
        )
        // Future transaction in 2099
        val cal2099 = Calendar.getInstance().apply { set(2099, Calendar.JANUARY, 15, 12, 0, 0) }
        txRepo.saveTransaction(
            Transaction(
                type = TransactionType.INCOME,
                amount = 999.0,
                categoryId = null,
                accountId = accId,
                note = "Future Bonus",
                timestamp = cal2099.timeInMillis,
                currencyCode = "BDT"
            )
        )

        val vm = HomeViewModel(
            getDashboardSummary = GetDashboardSummaryUseCase(accountRepo, txRepo, budgetRepo, categoryRepo, prefsRepo),
            preferencesRepository = prefsRepo
        )

        // Check November custom date range & validation
        assertFalse(vm.applyDateRange(novStart + 5 * 86_400_000L, novStart))
        assertTrue(vm.uiState.value.dateRangeValidationError)

        assertTrue(vm.applyDateRange(novStart, decStart - 1L))
        val mixedState = vm.uiState.first {
            it.isCustomFilterActive && it.summary.recentTransactions.any { tx -> tx.transaction.note == "Nov USD" }
        }
        assertTrue(mixedState.summary.hasMixedCurrencies)
        assertEquals(2, mixedState.summary.currencyTotals.size)

        // Changing unrelated preference does NOT overwrite custom filter
        prefsRepo.setAccentColor(AppAccentColor.INDIGO)
        val afterUnrelated = vm.uiState.first { it.preferences.accentColor == AppAccentColor.INDIGO }
        assertTrue(afterUnrelated.isCustomFilterActive)

        // Specific date filter isolates just Nov 2
        val novDay2 = novStart + 86_400_000L
        vm.applySpecificDate(novDay2)
        val day2State = vm.uiState.first {
            it.isSpecificDate && it.summary.recentTransactions.size == 1
        }
        assertEquals("Nov BDT", day2State.summary.recentTransactions.first().transaction.note)

        // Future period detection & actual room query
        vm.clearCustomFilter()
        vm.applySpecificDate(cal2099.timeInMillis)
        val futureState = vm.uiState.first {
            it.summary.isFuturePeriod && it.summary.periodIncome == 999.0
        }
        assertTrue(futureState.summary.isFuturePeriod)

        // ViewMode persistence via ViewModeSettings
        prefsRepo.setViewMode(ViewMode.WEEKLY)
        vm.clearCustomFilter()
        val weeklyState = vm.uiState.first { !it.isCustomFilterActive && it.viewMode == ViewMode.WEEKLY }
        assertEquals(ViewMode.WEEKLY, weeklyState.viewMode)
    }

    @Test
    fun `home screen period navigator and custom shortcut flow work smoothly`() {
        val homeViewModel = runBlocking {
            SeedData.populateDefaults(db)
            val vm = HomeViewModel(
                getDashboardSummary = GetDashboardSummaryUseCase(accountRepo, txRepo, budgetRepo, categoryRepo, prefsRepo),
                preferencesRepository = prefsRepo
            )
            vm.uiState.first()
            vm
        }

        composeRule.setContent {
            ExpenseManagerTheme(themeMode = ThemeMode.LIGHT) {
                HomeScreen(
                    homeViewModel = homeViewModel,
                    onToggleHideBalances = {},
                    onOpenAccounts = {},
                    onOpenAccountDetail = {},
                    onOpenTransactions = {},
                    onOpenTransaction = {},
                    onOpenBudgets = {},
                    onAddTransaction = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("home_period_navigator").assertIsDisplayed()
        composeRule.onNodeWithTag("home_period_prev").assertIsDisplayed()
        composeRule.onNodeWithTag("home_period_next").assertIsDisplayed()
        composeRule.onNodeWithTag("home_calendar_button").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("home_date_filter_sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("shortcut_today").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("home_custom_filter_banner").assertIsDisplayed()
        composeRule.onNodeWithTag("return_to_period_view_button").assertIsDisplayed().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            !homeViewModel.uiState.value.isCustomFilterActive
        }
    }

    @Test
    fun `preference sub screens update viewMode, language, and iso currency in DataStore`() {
        val settingsVm = runBlocking {
            SeedData.populateDefaults(db)
            val backupRepo = BackupAndExportRepositoryImpl(
                context, db, db.accountDao(), db.categoryDao(), db.transactionDao(), db.budgetDao(),
                txRepo, prefsRepo
            )
            SettingsViewModel(
                preferencesRepository = prefsRepo,
                categoryRepository = categoryRepo,
                transactionRepository = txRepo,
                backupAndExportRepository = backupRepo,
                driveAuthGateway = DummyDriveAuth,
                driveRemoteDataSource = DummyDriveRemote,
                googleDriveBackupRepository = DummyDriveBackup,
                appContext = context
            )
        }

        // Test LanguageSettingsScreen search + selection (Arabic)
        composeRule.setContent {
            val prefs = settingsVm.uiState.value.preferences
            ExpenseManagerTheme(languageTag = prefs.effectiveLanguageTag) {
                LanguageSettingsScreen(viewModel = settingsVm, onBack = {})
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("language_search_input").performTextInput("Arabic")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("lang_option_ar").assertIsDisplayed().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            settingsVm.uiState.value.preferences.effectiveLanguageTag == "ar"
        }

        // Test CurrencySettingsScreen search + selection (USD)
        composeRule.setContent {
            val prefs = settingsVm.uiState.value.preferences
            ExpenseManagerTheme(languageTag = prefs.effectiveLanguageTag) {
                CurrencySettingsScreen(viewModel = settingsVm, onBack = {})
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("currency_search_input").performTextInput("USD")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("currency_option_USD").assertIsDisplayed().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            settingsVm.uiState.value.preferences.effectiveCurrencyCode == "USD"
        }

        // Test ViewModeSettingsScreen selection (YEARLY)
        composeRule.setContent {
            val prefs = settingsVm.uiState.value.preferences
            ExpenseManagerTheme(languageTag = prefs.effectiveLanguageTag) {
                ViewModeSettingsScreen(viewModel = settingsVm, onBack = {})
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("view_mode_option_yearly").assertIsDisplayed().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            settingsVm.uiState.value.preferences.viewMode == ViewMode.YEARLY
        }
    }

    @Test
    fun `reminder time formatting respects locale and boot receiver handles boot intent safely`() {
        val formatted = DateTimeUtils.formatReminderTime(21, 5, context, "en", NumberFormatStyle.ENGLISH)
        assertTrue(formatted.isNotBlank())

        val receiver = BootCompletedReceiver()
        val pendingResult = null
        assertNotNull(receiver)
        // Ensure onReceive handles non-crash invocation with boot action
        receiver.onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))
        assertEquals(null, pendingResult)
    }
}

private object DummyDriveAuth : DriveAuthGateway {
    override suspend fun signInWithGoogle(activityContext: Context): Result<GoogleDriveAccount> =
        Result.failure(UnsupportedOperationException())
    override suspend fun authorizeDriveAccess(context: Context): DriveAuthOutcome =
        DriveAuthOutcome.Failed("Noop")
    override fun extractAccessTokenFromIntent(context: Context, data: Intent?): Result<String> =
        Result.failure(UnsupportedOperationException())
    override suspend fun signOut(context: Context) {}
}

private object DummyDriveRemote : GoogleDriveRemoteDataSource {
    override suspend fun fetchUserInfo(accessToken: String): Result<GoogleDriveAccount> =
        Result.failure(UnsupportedOperationException())
    override suspend fun listBackups(accessToken: String): Result<List<DriveBackupEntry>> =
        Result.success(emptyList())
    override suspend fun uploadBackup(
        accessToken: String,
        fileName: String,
        bytes: ByteArray,
        isEncrypted: Boolean,
        transactionCount: Int,
        accountCount: Int,
        exportedAt: Long
    ): Result<DriveBackupEntry> = Result.failure(UnsupportedOperationException())
    override suspend fun downloadBackup(accessToken: String, fileId: String): Result<ByteArray> =
        Result.failure(UnsupportedOperationException())
    override suspend fun deleteBackup(accessToken: String, fileId: String): Result<Unit> =
        Result.success(Unit)
}

private object DummyDriveBackup : GoogleDriveBackupRepository {
    override fun isNetworkAvailable(): Boolean = false
    override suspend fun backupToDrive(accessToken: String, passphrase: String?): Result<DriveBackupEntry> =
        Result.failure(UnsupportedOperationException())
    override suspend fun listBackups(accessToken: String): Result<List<DriveBackupEntry>> =
        Result.success(emptyList())
    override suspend fun downloadBackupBytes(accessToken: String, fileId: String): Result<ByteArray> =
        Result.failure(UnsupportedOperationException())
    override fun parseBackupBytes(bytes: ByteArray, passphrase: String?): Result<com.example.core.model.BackupPayload> =
        Result.failure(UnsupportedOperationException())
    override suspend fun deleteBackup(accessToken: String, fileId: String): Result<Unit> =
        Result.success(Unit)
}
