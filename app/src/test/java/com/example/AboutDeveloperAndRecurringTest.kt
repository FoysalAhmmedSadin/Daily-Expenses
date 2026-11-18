package com.example

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.data.AccountRepositoryImpl
import com.example.core.data.CategoryRepositoryImpl
import com.example.core.data.TransactionRepositoryImpl
import com.example.core.database.ExpenseDatabase
import com.example.core.database.SeedData
import com.example.core.model.Account
import com.example.core.model.AccountType
import com.example.core.model.RecurrenceFrequency
import com.example.core.model.ThemeMode
import com.example.core.model.Transaction
import com.example.core.model.TransactionType
import com.example.ui.screens.AboutDeveloperScreen
import com.example.ui.screens.DeveloperSupport
import com.example.ui.theme.ExpenseManagerTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AboutDeveloperAndRecurringTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `about developer screen renders all fields in light mode and handles missing email client`() {
        var backClicked = false
        composeRule.setContent {
            ExpenseManagerTheme(themeMode = ThemeMode.LIGHT) {
                AboutDeveloperScreen(onBack = { backClicked = true })
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("about_dev_back_button")
            .assertIsDisplayed()
            .performClick()
        assertTrue(backClicked)

        composeRule.onNodeWithTag("developer_name")
            .assertIsDisplayed()
            .assertTextContains(DeveloperSupport.DEVELOPER_NAME)
        composeRule.onNodeWithTag("developer_role")
            .assertIsDisplayed()
            .assertTextContains(DeveloperSupport.DEVELOPER_ROLE)
        composeRule.onNodeWithTag("developer_email")
            .assertIsDisplayed()
            .assertTextContains(DeveloperSupport.DEVELOPER_EMAIL)
        composeRule.onNodeWithTag("about_app_name")
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextContains("Daily Expenses")
        composeRule.onNodeWithTag("about_app_tagline")
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextContains("Personal Finance Manager")
        composeRule.onNodeWithTag("app_version_text")
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextContains("v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        composeRule.onNodeWithTag("copyright_text")
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextContains(DeveloperSupport.COPYRIGHT_TEXT)
        composeRule.onNodeWithTag("open_source_licenses_title")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("nkuppan", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("github.com/nkuppan", substring = true).assertDoesNotExist()

        // Tap Contact Developer without an email client installed -> shows error banner
        composeRule.onNodeWithTag("contact_developer_button")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("no_email_error_banner")
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextContains("No email app found", substring = true)
    }

    @Test
    fun `about developer screen renders in dark mode and launches support email intent`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val shadowPm = Shadows.shadowOf(context.packageManager)
        val mailComponent = ComponentName("com.mock.email", "com.mock.email.ComposeActivity")
        shadowPm.addActivityIfNotPresent(mailComponent)
        val mailFilter = IntentFilter(Intent.ACTION_SENDTO).apply {
            addDataScheme("mailto")
            addCategory(Intent.CATEGORY_DEFAULT)
        }
        @Suppress("DEPRECATION")
        shadowPm.addIntentFilterForActivity(mailComponent, mailFilter)

        composeRule.setContent {
            ExpenseManagerTheme(themeMode = ThemeMode.DARK) {
                AboutDeveloperScreen(onBack = {})
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("developer_name")
            .assertIsDisplayed()
            .assertTextContains(DeveloperSupport.DEVELOPER_NAME)
        composeRule.onNodeWithTag("contact_developer_button")
            .performClick()
        composeRule.waitForIdle()

        val launched = Shadows.shadowOf(context).nextStartedActivity
        assertNotNull(launched)
        assertEquals(Intent.ACTION_SENDTO, launched.action)
        assertTrue(launched.data.toString().startsWith("mailto:${DeveloperSupport.DEVELOPER_EMAIL}"))
        assertEquals(DeveloperSupport.SUPPORT_SUBJECT, launched.getStringExtra(Intent.EXTRA_SUBJECT))
        val recipients = launched.getStringArrayExtra(Intent.EXTRA_EMAIL)
        assertEquals(DeveloperSupport.DEVELOPER_EMAIL, recipients?.firstOrNull())
    }

    @Test
    fun `monthly recurring rent expense schedules and automates entry in room`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, ExpenseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            SeedData.populateDefaults(db)
            val accountRepo = AccountRepositoryImpl(db, db.accountDao())
            val categoryRepo = CategoryRepositoryImpl(db.categoryDao())
            val txRepo = TransactionRepositoryImpl(db, db.transactionDao(), db.accountDao(), db.categoryDao())

            val bankId = accountRepo.saveAccount(
                Account(name = "Salary Bank", type = AccountType.BANK, initialBalance = 50_000.0)
            )
            val rentCatId = categoryRepo.getCategories().first { it.nameEn == "House Rent" }.id

            val oneMonthAndTwoDaysAgo = Calendar.getInstance().apply {
                add(Calendar.MONTH, -1)
                add(Calendar.DAY_OF_YEAR, -2)
            }.timeInMillis

            val templateId = txRepo.saveTransaction(
                Transaction(
                    type = TransactionType.EXPENSE,
                    amount = 15_000.0,
                    categoryId = rentCatId,
                    accountId = bankId,
                    note = "Monthly Apartment Rent",
                    timestamp = oneMonthAndTwoDaysAgo,
                    recurrence = RecurrenceFrequency.MONTHLY
                )
            )

            // Template deducted 15,000 -> 35,000 remaining
            assertEquals(35_000.0, accountRepo.observeAccountById(bankId).first()!!.balance, 0.001)

            // Automated worker entry processes the due 1-month recurring rent
            val added = txRepo.processDueRecurringTransactions()
            assertEquals(1, added)
            assertEquals(20_000.0, accountRepo.observeAccountById(bankId).first()!!.balance, 0.001)

            val allTxs = txRepo.getTransactionsWithDetails()
            val generatedChild = allTxs.firstOrNull { it.transaction.parentRecurringId == templateId }
            assertNotNull(generatedChild)
            assertEquals("Monthly Apartment Rent", generatedChild?.transaction?.note)
        } finally {
            db.close()
        }
    }
}
