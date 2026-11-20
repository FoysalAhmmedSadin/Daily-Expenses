package com.example

import android.content.Context
import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
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
import com.example.core.domain.GetBudgetsWithProgressUseCase
import com.example.core.domain.GetDashboardSummaryUseCase
import com.example.core.datastore.PreferencesDataSource
import com.example.core.model.Account
import com.example.core.model.AccountType
import com.example.core.model.Budget
import com.example.core.model.DateFilterPeriod
import com.example.core.model.ThemeMode
import com.example.core.model.Transaction
import com.example.core.model.TransactionType
import com.example.ui.screens.BudgetsScreen
import com.example.ui.screens.BudgetsViewModel
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.HomeViewModel
import com.example.ui.theme.ExpenseManagerTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp")
class CategoryBudgetDashboardTest {

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
    fun `dashboard summary triggers budgetAlerts when category spending reaches 80 percent`() = runTest {
        SeedData.populateDefaults(db)
        val accountId = accountRepo.saveAccount(
            Account(name = "Main Bank", type = AccountType.BANK, initialBalance = 20_000.0)
        )
        val categories = categoryRepo.getCategories().filter { it.type == TransactionType.EXPENSE }
        val foodCat = categories.first { it.nameEn == "Food & Dining" }
        val transportCat = categories.first { it.nameEn == "Transport" }
        val month = DateTimeUtils.currentMonthYear()

        val foodBudgetId = budgetRepo.saveBudget(
            Budget(categoryId = foodCat.id, amountLimit = 1000.0, monthYear = month)
        )
        val transportBudgetId = budgetRepo.saveBudget(
            Budget(categoryId = transportCat.id, amountLimit = 500.0, monthYear = month)
        )

        val getSummary = GetDashboardSummaryUseCase(
            accountRepository = accountRepo,
            transactionRepository = txRepo,
            budgetRepository = budgetRepo,
            categoryRepository = categoryRepo,
            preferencesRepository = prefsRepo
        )
        val periodFlow = MutableStateFlow(DateFilterPeriod.THIS_MONTH)
        val customRangeFlow = MutableStateFlow<Pair<Long?, Long?>>(null to null)

        // Spend 75% of Food cap (750 / 1000) -> no 80% alert yet
        txRepo.saveTransaction(
            Transaction(
                type = TransactionType.EXPENSE,
                amount = 750.0,
                categoryId = foodCat.id,
                accountId = accountId,
                note = "Groceries"
            )
        )
        val beforeThreshold = getSummary(periodFlow, customRangeFlow).first()
        assertTrue(beforeThreshold.budgetAlerts.isEmpty())

        // Spend another 50 in Food -> 800 / 1000 = 80% -> Food alert appears
        txRepo.saveTransaction(
            Transaction(
                type = TransactionType.EXPENSE,
                amount = 50.0,
                categoryId = foodCat.id,
                accountId = accountId,
                note = "Snacks"
            )
        )
        val atThreshold = getSummary(periodFlow, customRangeFlow).first()
        assertEquals(1, atThreshold.budgetAlerts.size)
        val foodAlert = atThreshold.budgetAlerts.first()
        assertEquals(foodBudgetId, foodAlert.budget.id)
        assertEquals(80, foodAlert.progressPercent)
        assertTrue(foodAlert.isReachedEightyPercent)
        assertTrue(!foodAlert.isOverspent)

        // Spend 550 in Transport -> 110% (overspent) -> both appear, Transport sorted first
        txRepo.saveTransaction(
            Transaction(
                type = TransactionType.EXPENSE,
                amount = 550.0,
                categoryId = transportCat.id,
                accountId = accountId,
                note = "Taxi pass"
            )
        )
        val afterOver = getSummary(periodFlow, customRangeFlow).first()
        assertEquals(2, afterOver.budgetAlerts.size)
        assertEquals(transportBudgetId, afterOver.budgetAlerts.first().budget.id)
        assertTrue(afterOver.budgetAlerts.first().isOverspent)
    }

    @Test
    fun `dashboard renders 80 percent category budget progress indicator when threshold reached`() {
        val (budgetId, homeViewModel) = runBlocking {
            SeedData.populateDefaults(db)
            val accountId = accountRepo.saveAccount(
                Account(name = "Cash Wallet", type = AccountType.CASH, initialBalance = 10_000.0)
            )
            val foodCat = categoryRepo.getCategories().first { it.nameEn == "Food & Dining" }
            val month = DateTimeUtils.currentMonthYear()
            val id = budgetRepo.saveBudget(
                Budget(categoryId = foodCat.id, amountLimit = 1000.0, monthYear = month)
            )
            txRepo.saveTransaction(
                Transaction(
                    type = TransactionType.EXPENSE,
                    amount = 850.0,
                    categoryId = foodCat.id,
                    accountId = accountId,
                    note = "Dinner"
                )
            )
            val vm = HomeViewModel(
                getDashboardSummary = GetDashboardSummaryUseCase(
                    accountRepo, txRepo, budgetRepo, categoryRepo, prefsRepo
                ),
                preferencesRepository = prefsRepo
            )
            id to vm
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
        composeRule.waitUntil(timeoutMillis = 5_000) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            homeViewModel.uiState.value.summary.budgetAlerts.isNotEmpty()
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("home_dashboard_list")
            .performScrollToIndex(3)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("dashboard_budget_alert_card").assertIsDisplayed()
        composeRule.onNodeWithTag("dashboard_alert_item_$budgetId").assertIsDisplayed()
        composeRule.onNodeWithTag("dashboard_alert_budget_percent_$budgetId")
            .assertIsDisplayed()
            .assertTextContains("85%")
        composeRule.onNodeWithTag("dashboard_alert_budget_progress_bar_$budgetId").assertIsDisplayed()
        composeRule.onNodeWithTag("dashboard_alert_budget_status_$budgetId")
            .assertIsDisplayed()
            .assertTextContains("80%+ Reached")
    }

    @Test
    fun `budgets screen allows setting a monthly spending cap for a specific category`() {
        val (shoppingCat, budgetsViewModel) = runBlocking {
            SeedData.populateDefaults(db)
            val cat = categoryRepo.getCategories().first { it.nameEn == "Shopping" }
            val vm = BudgetsViewModel(
                budgetRepository = budgetRepo,
                getBudgetsWithProgress = GetBudgetsWithProgressUseCase(budgetRepo, txRepo, categoryRepo),
                categoryRepository = categoryRepo,
                preferencesRepository = prefsRepo
            )
            cat to vm
        }

        composeRule.setContent {
            ExpenseManagerTheme(themeMode = ThemeMode.DARK) {
                BudgetsScreen(viewModel = budgetsViewModel)
            }
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            budgetsViewModel.uiState.value.expenseCategories.isNotEmpty()
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("add_budget_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("budget_category_chip_${shoppingCat.id}")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag("budget_limit_input")
            .performTextInput("2500")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("save_budget_button")
            .assertIsEnabled()
            .performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            budgetsViewModel.uiState.value.budgets.any {
                it.budget.categoryId == shoppingCat.id && it.budget.amountLimit == 2500.0
            }
        }
        composeRule.waitForIdle()

        val savedBudget = budgetsViewModel.uiState.value.budgets.first { it.budget.categoryId == shoppingCat.id }
        composeRule.onNodeWithTag("budget_card_${savedBudget.budget.id}").assertIsDisplayed()
        composeRule.onNodeWithText("Shopping").assertIsDisplayed()
        composeRule.onNodeWithTag("budget_progress_bar_${savedBudget.budget.id}").assertIsDisplayed()
    }
}
