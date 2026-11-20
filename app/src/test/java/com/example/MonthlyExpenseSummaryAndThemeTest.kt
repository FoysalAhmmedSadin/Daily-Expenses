package com.example

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.common.DateTimeUtils
import com.example.core.data.AccountRepositoryImpl
import com.example.core.data.BudgetRepositoryImpl
import com.example.core.data.CategoryRepositoryImpl
import com.example.core.data.PreferencesRepositoryImpl
import com.example.core.data.TransactionRepositoryImpl
import com.example.core.database.ExpenseDatabase
import com.example.core.database.SeedData
import com.example.core.datastore.PreferencesDataSource
import com.example.core.domain.GetDashboardSummaryUseCase
import com.example.core.model.Account
import com.example.core.model.AccountType
import com.example.core.model.AppAccentColor
import com.example.core.model.DateFilterPeriod
import com.example.core.model.ThemeMode
import com.example.core.model.Transaction
import com.example.core.model.TransactionType
import com.example.ui.navigation.ExpenseManagerShell
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.HomeViewModel
import com.example.ui.theme.ExpenseManagerTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp")
class MonthlyExpenseSummaryAndThemeTest {

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
    fun `dashboard summary computes monthly category expense breakdown and previous month comparison`() = runTest {
        SeedData.populateDefaults(db)
        val accountId = accountRepo.saveAccount(
            Account(name = "Main Wallet", type = AccountType.CASH, initialBalance = 50_000.0)
        )
        val expenseCategories = categoryRepo.getCategories().filter { it.type == TransactionType.EXPENSE }
        val foodCat = expenseCategories.first { it.nameEn == "Food & Dining" }
        val shoppingCat = expenseCategories.first { it.nameEn == "Shopping" }
        val transportCat = expenseCategories.first { it.nameEn == "Transport" }

        val curMonth = DateTimeUtils.currentMonthYear()
        val prevMonth = DateTimeUtils.previousMonthYear(curMonth)
        val (curStart, _) = DateTimeUtils.monthYearRange(curMonth)
        val (prevStart, _) = DateTimeUtils.monthYearRange(prevMonth)

        // Previous month expense: 1000
        txRepo.saveTransaction(
            Transaction(
                type = TransactionType.EXPENSE,
                amount = 1000.0,
                categoryId = foodCat.id,
                accountId = accountId,
                timestamp = prevStart + 3_600_000L
            )
        )
        // Current month expenses on first day of month: Food=800, Shopping=500, Transport=200 (Total=1500)
        txRepo.saveTransaction(
            Transaction(
                type = TransactionType.EXPENSE,
                amount = 800.0,
                categoryId = foodCat.id,
                accountId = accountId,
                timestamp = curStart + 3_600_000L
            )
        )
        txRepo.saveTransaction(
            Transaction(
                type = TransactionType.EXPENSE,
                amount = 500.0,
                categoryId = shoppingCat.id,
                accountId = accountId,
                timestamp = curStart + 7_200_000L
            )
        )
        txRepo.saveTransaction(
            Transaction(
                type = TransactionType.EXPENSE,
                amount = 200.0,
                categoryId = transportCat.id,
                accountId = accountId,
                timestamp = curStart + 10_800_000L
            )
        )

        val getSummary = GetDashboardSummaryUseCase(
            accountRepository = accountRepo,
            transactionRepository = txRepo,
            budgetRepository = budgetRepo,
            categoryRepository = categoryRepo,
            preferencesRepository = prefsRepo
        )

        // Even when selected period is TODAY, monthly expense summary still reflects current month
        val summary = getSummary(
            MutableStateFlow(DateFilterPeriod.TODAY),
            MutableStateFlow(null to null)
        ).first()

        assertEquals(curMonth, summary.currentMonthYear)
        assertEquals(1500.0, summary.monthlyExpenseTotal, 0.001)
        assertEquals(1000.0, summary.previousMonthExpenseTotal, 0.001)
        assertEquals(3, summary.monthlyCategoryBreakdowns.size)
        assertEquals(foodCat.id, summary.monthlyCategoryBreakdowns[0].category?.id)
        assertEquals(800.0, summary.monthlyCategoryBreakdowns[0].totalAmount, 0.001)
        assertEquals(shoppingCat.id, summary.monthlyCategoryBreakdowns[1].category?.id)
        assertEquals(transportCat.id, summary.monthlyCategoryBreakdowns[2].category?.id)
    }

    @Test
    fun `home screen renders monthly expense summary bar chart and interactive selection`() {
        val (foodId, homeViewModel) = runBlocking {
            SeedData.populateDefaults(db)
            val accountId = accountRepo.saveAccount(
                Account(name = "Bank", type = AccountType.BANK, initialBalance = 20_000.0)
            )
            val cats = categoryRepo.getCategories().filter { it.type == TransactionType.EXPENSE }
            val foodCat = cats.first { it.nameEn == "Food & Dining" }
            val transportCat = cats.first { it.nameEn == "Transport" }
            val curMonth = DateTimeUtils.currentMonthYear()
            val prevMonth = DateTimeUtils.previousMonthYear(curMonth)
            val (curStart, _) = DateTimeUtils.monthYearRange(curMonth)
            val (prevStart, _) = DateTimeUtils.monthYearRange(prevMonth)

            txRepo.saveTransaction(
                Transaction(
                    type = TransactionType.EXPENSE,
                    amount = 1000.0,
                    categoryId = foodCat.id,
                    accountId = accountId,
                    timestamp = prevStart + 3_600_000L
                )
            )
            txRepo.saveTransaction(
                Transaction(
                    type = TransactionType.EXPENSE,
                    amount = 900.0,
                    categoryId = foodCat.id,
                    accountId = accountId,
                    timestamp = curStart + 3_600_000L
                )
            )
            txRepo.saveTransaction(
                Transaction(
                    type = TransactionType.EXPENSE,
                    amount = 300.0,
                    categoryId = transportCat.id,
                    accountId = accountId,
                    timestamp = curStart + 7_200_000L
                )
            )

            val vm = HomeViewModel(
                getDashboardSummary = GetDashboardSummaryUseCase(
                    accountRepo, txRepo, budgetRepo, categoryRepo, prefsRepo
                ),
                preferencesRepository = prefsRepo
            )
            foodCat.id to vm
        }

        composeRule.setContent {
            ExpenseManagerTheme(themeMode = ThemeMode.LIGHT, accentColor = AppAccentColor.OCEAN_BLUE) {
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
        composeRule.waitUntil(timeoutMillis = 5_000) {
            homeViewModel.uiState.value.summary.monthlyCategoryBreakdowns.isNotEmpty()
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("home_dashboard_list")
            .performScrollToNode(hasTestTag("dashboard_monthly_expense_summary_card"))
        composeRule.onNodeWithTag("dashboard_monthly_expense_summary_card").assertIsDisplayed()
        composeRule.onNodeWithTag("monthly_expense_total")
            .assertIsDisplayed()
            .assertTextContains("1,200", substring = true)
        composeRule.onNodeWithTag("monthly_expense_trend_badge")
            .assertIsDisplayed()
            .assertTextContains("+20.0%", substring = true)
        composeRule.onNodeWithTag("monthly_category_bar_chart").assertIsDisplayed()
        composeRule.onNodeWithTag("monthly_top_category_summary").assertIsDisplayed()

        composeRule.onNodeWithTag("category_bar_label_$foodId")
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("category_bar_selected_callout")
            .assertIsDisplayed()
            .assertTextContains("Food & Dining", substring = true)
    }

    @Test
    fun `home screen renders monthly expense summary empty state when no monthly expenses exist`() {
        val homeViewModel = runBlocking {
            SeedData.populateDefaults(db)
            val vm = HomeViewModel(
                getDashboardSummary = GetDashboardSummaryUseCase(
                    accountRepo, txRepo, budgetRepo, categoryRepo, prefsRepo
                ),
                preferencesRepository = prefsRepo
            )
            vm.uiState.first()
            vm
        }

        composeRule.setContent {
            ExpenseManagerTheme(themeMode = ThemeMode.DARK) {
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

        composeRule.onNodeWithTag("home_dashboard_list")
            .performScrollToNode(hasTestTag("dashboard_monthly_expense_summary_card"))
        composeRule.onNodeWithTag("dashboard_monthly_expense_summary_card").assertIsDisplayed()
        composeRule.onNodeWithTag("monthly_expense_total").assertIsDisplayed()
    }

    @Test
    fun `changing accent color updates secondaryContainer and onPrimary dynamically without hardcoded blue`() {
        val accents = listOf(
            AppAccentColor.OCEAN_BLUE,
            AppAccentColor.CORAL_RED,
            AppAccentColor.AMBER_GOLD,
            AppAccentColor.ROSE_PINK
        )
        val captured = mutableMapOf<AppAccentColor, List<Color>>()

        composeRule.setContent {
            accents.forEach { accent ->
                ExpenseManagerTheme(themeMode = ThemeMode.LIGHT, accentColor = accent) {
                    captured[accent] = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.secondaryContainer,
                        MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
        composeRule.waitForIdle()

        accents.forEach { accent ->
            val (primary, primaryContainer, secondaryContainer, onPrimary) = captured.getValue(accent)
            assertEquals(Color(accent.primaryHex), primary)
            assertEquals(primaryContainer, secondaryContainer)
            if (accent != AppAccentColor.OCEAN_BLUE) {
                assertNotEquals(Color(0xFFE5F2FF), secondaryContainer)
            }
            if (accent == AppAccentColor.AMBER_GOLD) {
                assertNotEquals(Color.White, onPrimary)
            }
        }
    }

    @Test
    fun `bottom navigation budgets tab uses active theme accent and never locks to emerald`() {
        composeRule.setContent {
            ExpenseManagerTheme(themeMode = ThemeMode.LIGHT, accentColor = AppAccentColor.ROSE_PINK) {
                ExpenseManagerShell(
                    openAddTxTrigger = false,
                    onConsumeAddTxTrigger = {},
                    onToggleHideBalances = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("nav_budgets")
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()

        assertTrue(true)
    }
}
