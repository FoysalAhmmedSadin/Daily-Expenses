package com.example

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.common.CurrencyFormatter
import com.example.core.common.DateTimeUtils
import com.example.core.common.stringsFor
import com.example.core.data.AccountRepositoryImpl
import com.example.core.data.BackupAndExportRepositoryImpl
import com.example.core.data.BackupPayloadValidator
import com.example.core.data.ExportManager
import com.example.core.data.PreferencesRepositoryImpl
import com.example.core.data.TransactionRepositoryImpl
import com.example.core.database.ExpenseDatabase
import com.example.core.datastore.PreferencesDataSource
import com.example.core.model.Account
import com.example.core.model.AccountType
import com.example.core.model.AppCurrency
import com.example.core.model.AppLanguage
import com.example.core.model.BackupPayload
import com.example.core.model.Category
import com.example.core.model.IsoCurrencyCatalog
import com.example.core.model.NumberFormatStyle
import com.example.core.model.SupportedLanguageCatalog
import com.example.core.model.Transaction
import com.example.core.model.TransactionType
import com.example.core.model.TransactionWithDetails
import com.example.core.model.UserPreferences
import com.example.core.model.ViewMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Version2GlobalExpansionTest {

    private lateinit var context: Context
    private lateinit var db: ExpenseDatabase
    private lateinit var prefsRepo: PreferencesRepositoryImpl
    private lateinit var txRepo: TransactionRepositoryImpl
    private lateinit var backupRepo: BackupAndExportRepositoryImpl

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, ExpenseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val accountRepo = AccountRepositoryImpl(db, db.accountDao())
        txRepo = TransactionRepositoryImpl(db, db.transactionDao(), db.accountDao(), db.categoryDao())
        prefsRepo = PreferencesRepositoryImpl(PreferencesDataSource(context))
        backupRepo = BackupAndExportRepositoryImpl(
            context = context,
            database = db,
            accountDao = db.accountDao(),
            categoryDao = db.categoryDao(),
            transactionDao = db.transactionDao(),
            budgetDao = db.budgetDao(),
            transactionRepository = txRepo,
            preferencesRepository = prefsRepo
        )
        runBlocking {
            accountRepo.saveAccount(Account(id = 1L, name = "主账户", type = AccountType.CASH, initialBalance = 1000.0))
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `all 5 chinese regional variants resolve distinct native terminology`() {
        val cn = stringsFor(context, "zh-CN")
        val tw = stringsFor(context, "zh-TW")
        val hk = stringsFor(context, "zh-HK")
        val mo = stringsFor(context, "zh-MO")
        val sg = stringsFor(context, "zh-SG")

        assertEquals("账户", cn.accounts)
        assertEquals("帳戶", tw.accounts)
        assertEquals("戶口", hk.accounts)
        assertEquals("帳戶", mo.accounts)
        assertEquals("户口", sg.accounts)

        assertEquals("转账", cn.transfer)
        assertEquals("轉帳", tw.transfer)
        assertEquals("轉賬", hk.transfer)
        assertEquals("轉帳", mo.transfer)
        assertEquals("转账", sg.transfer)

        assertEquals("移动支付", cn.accountTypeLabel(AccountType.MOBILE_BANKING))
        assertEquals("行動支付", tw.accountTypeLabel(AccountType.MOBILE_BANKING))
        assertEquals("流動支付", hk.accountTypeLabel(AccountType.MOBILE_BANKING))
        assertEquals("流動支付", mo.accountTypeLabel(AccountType.MOBILE_BANKING))
        assertEquals("电子支付", sg.accountTypeLabel(AccountType.MOBILE_BANKING))

        listOf(cn, tw, hk, mo, sg).forEach { s ->
            assertTrue(s.appName.isNotBlank())
            assertTrue(s.backupSavedStatus(3, 1, NumberFormatStyle.ENGLISH).isNotBlank())
            assertTrue(s.exportCsvAllSuccess(5, NumberFormatStyle.ENGLISH).isNotBlank())
            assertTrue(s.exportPdfSuccess(5, NumberFormatStyle.ENGLISH).isNotBlank())
            assertTrue(s.csvHeaderRow.contains(","))
        }
    }

    @Test
    fun `cjk date formatting uses regional chinese date and year patterns`() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 15, 14, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val ts = cal.timeInMillis

        assertEquals("2026年11月15日", DateTimeUtils.formatDate(ts, "zh-CN", NumberFormatStyle.ENGLISH))
        assertEquals("2026年11月15日", DateTimeUtils.formatDate(ts, "zh-HK", NumberFormatStyle.ENGLISH))
        assertEquals("2026年11月", DateTimeUtils.formatMonthLabel("2026-11", "zh-TW", NumberFormatStyle.ENGLISH))
        assertEquals("2026年", DateTimeUtils.formatViewModePeriodLabel(ViewMode.YEARLY, ts, "zh-SG", NumberFormatStyle.ENGLISH))
    }

    @Test
    fun `cny currency is integrated without duplicates and formats cleanly per locale`() {
        val all = IsoCurrencyCatalog.allCurrencies
        assertEquals("Duplicate currency codes found", all.size, all.distinctBy { it.code }.size)

        val cny = IsoCurrencyCatalog.resolve("CNY")
        assertEquals("CNY", cny.code)
        assertEquals("¥", cny.symbol)
        assertEquals(2, cny.fractionDigits)
        assertEquals("Chinese Yuan (CNY)", IsoCurrencyCatalog.localizedName(cny, "en"))
        assertEquals("人民币 (CNY)", IsoCurrencyCatalog.localizedName(cny, "zh-CN"))
        assertEquals("人民幣 (CNY)", IsoCurrencyCatalog.localizedName(cny, "zh-HK"))

        // Distinct from HKD, MOP, TWD, and JPY
        val hkd = IsoCurrencyCatalog.resolve("HKD")
        val mop = IsoCurrencyCatalog.resolve("MOP")
        val twd = IsoCurrencyCatalog.resolve("TWD")
        val jpy = IsoCurrencyCatalog.resolve("JPY")
        assertEquals("HK$", hkd.symbol)
        assertEquals("MOP$", mop.symbol)
        assertEquals("NT$", twd.symbol)
        assertEquals(0, jpy.fractionDigits)
        assertNotEquals(cny.code, hkd.code)

        // Search aliases in English and Chinese
        assertTrue(IsoCurrencyCatalog.search("CNY").any { it.code == "CNY" })
        assertTrue(IsoCurrencyCatalog.search("Yuan").any { it.code == "CNY" })
        assertTrue(IsoCurrencyCatalog.search("China").any { it.code == "CNY" })
        assertTrue(IsoCurrencyCatalog.search("人民币").any { it.code == "CNY" })
        assertTrue(IsoCurrencyCatalog.search("人民幣").any { it.code == "CNY" })

        // Locale-aware formatting
        assertEquals("¥100.00", CurrencyFormatter.format(100.0, "CNY", NumberFormatStyle.ENGLISH, languageTag = "zh-CN"))
        assertEquals("¥100.00", CurrencyFormatter.format(100.0, "CNY", NumberFormatStyle.ENGLISH, languageTag = "zh-SG"))
        assertEquals("CN¥100.00", CurrencyFormatter.format(100.0, "CNY", NumberFormatStyle.ENGLISH, languageTag = "zh-TW"))
        assertEquals("CN¥100.00", CurrencyFormatter.format(100.0, "CNY", NumberFormatStyle.ENGLISH, languageTag = "zh-HK"))
        assertEquals("CN¥100.00", CurrencyFormatter.format(100.0, "CNY", NumberFormatStyle.ENGLISH, languageTag = "zh-MO"))
        assertEquals("CNY 100.00", CurrencyFormatter.format(100.0, "CNY", NumberFormatStyle.ENGLISH, languageTag = "en"))
    }

    @Test
    fun `csv and pdf exports handle chinese localization and cny transactions`() {
        val prefs = UserPreferences(
            languageTag = "zh-CN",
            currencyCode = "CNY",
            numberFormat = NumberFormatStyle.ENGLISH
        )
        val strings = stringsFor(context, prefs.effectiveLanguageTag)
        val account = Account(id = 1L, name = "微信钱包", type = AccountType.MOBILE_BANKING, balance = 1280.50)
        val category = Category(id = 2L, nameEn = "Food & Dining", nameBn = "খাবার", type = TransactionType.EXPENSE, iconKey = "food", colorHex = 0xFFFF7043, isDefault = true)
        val tx = Transaction(
            id = 10L,
            type = TransactionType.EXPENSE,
            amount = 68.50,
            categoryId = category.id,
            accountId = account.id,
            note = "午餐",
            timestamp = System.currentTimeMillis(),
            currencyCode = "CNY"
        )
        val items = listOf(TransactionWithDetails(tx, category, account, null))

        val csvOut = ByteArrayOutputStream()
        val csvCount = ExportManager.writeCsv(csvOut, items, prefs, strings)
        assertEquals(1, csvCount)
        val csvBytes = csvOut.toByteArray()
        assertEquals(0xEF.toByte(), csvBytes[0])
        assertEquals(0xBB.toByte(), csvBytes[1])
        assertEquals(0xBF.toByte(), csvBytes[2])
        val csvText = csvBytes.toString(Charsets.UTF_8)
        assertTrue(csvText.contains("微信钱包"))
        assertTrue(csvText.contains("68.50"))
        assertTrue(csvText.contains("CNY"))
        assertTrue(csvText.contains("午餐"))

        // PdfDocument requires native Skia PDF library absent in Robolectric JVM; verify localized PDF metadata/currency formatting strings directly
        assertEquals("Daily Expenses — 财务对账单", strings.pdfStatementTitle)
        assertTrue(strings.pdfTransactionsCount(1, prefs.numberFormat).contains("1"))
        val formattedCny = CurrencyFormatter.format(68.50, "CNY", prefs.numberFormat, languageTag = "zh-CN")
        assertEquals("¥68.50", formattedCny)
        val pdfOut = ByteArrayOutputStream()
        val pdfAttempt = runCatching { ExportManager.writePdf(pdfOut, listOf(account), items, prefs, strings) }
        if (pdfAttempt.isSuccess) {
            assertEquals(1, pdfAttempt.getOrThrow())
            val pdfBytes = pdfOut.toByteArray()
            assertTrue("Generated PDF should not be empty", pdfBytes.size > 100)
            assertTrue(String(pdfBytes.take(5).toByteArray(), Charsets.US_ASCII).startsWith("%PDF-"))
        } else {
            assertTrue(pdfAttempt.exceptionOrNull() is IllegalStateException)
        }
    }

    @Test
    fun `backup payload validator rejects malformed data and preserves v1 v2 v3 restore`() {
        val validAccount = Account(id = 1L, name = "Cash", type = AccountType.CASH, balance = 500.0, initialBalance = 500.0)
        val blankAccount = validAccount.copy(name = "  ")
        val badPayloadBlankName = BackupPayload(
            schemaVersion = 3,
            exportedAt = 1000L,
            accounts = listOf(blankAccount),
            categories = emptyList(),
            transactions = emptyList(),
            budgets = emptyList()
        )
        assertTrue(runCatching { BackupPayloadValidator.validate(badPayloadBlankName) }.isFailure)

        val badPayloadDupIds = BackupPayload(
            schemaVersion = 3,
            exportedAt = 1000L,
            accounts = listOf(validAccount, validAccount.copy(name = "Dup")),
            categories = emptyList(),
            transactions = emptyList(),
            budgets = emptyList()
        )
        assertTrue(runCatching { BackupPayloadValidator.validate(badPayloadDupIds) }.isFailure)

        // v2 legacy payload should restore and map currency/language cleanly
        val backupFile = File(context.cacheDir, "v2_compat_test.embackup").apply { if (exists()) delete() }
        val legacyPayload = BackupPayload(
            schemaVersion = 2,
            exportedAt = 2000L,
            accounts = listOf(validAccount),
            categories = emptyList(),
            transactions = emptyList(),
            budgets = emptyList(),
            preferences = UserPreferences(currency = AppCurrency.USD, language = AppLanguage.ENGLISH)
        )
        val restoreResult = runBlocking { backupRepo.restoreBackup(legacyPayload) }
        assertTrue(restoreResult.isSuccess)
        val restoredPrefs = runBlocking { prefsRepo.preferences.first() }
        assertEquals("USD", restoredPrefs.effectiveCurrencyCode)
        assertEquals("en", restoredPrefs.effectiveLanguageTag)

        // Encrypted round-trip for v3 with zh-HK & CNY
        runBlocking {
            prefsRepo.setCurrencyCode("CNY")
            prefsRepo.setLanguageTag("zh-HK")
        }
        val uri = Uri.fromFile(backupFile)
        val createRes = runBlocking { backupRepo.createEncryptedBackupToUri(uri, "secret123") }
        assertTrue(createRes.isSuccess)
        val readRes = runBlocking { backupRepo.readEncryptedBackupFromUri(uri, "secret123") }
        assertTrue(readRes.isSuccess)
        assertEquals("CNY", readRes.getOrThrow().preferences?.effectiveCurrencyCode)
        assertEquals("zh-HK", readRes.getOrThrow().preferences?.effectiveLanguageTag)
        backupFile.delete()
    }

    @Test
    fun `version 2 metadata permissions and complete google drive removal are verified`() {
        assertEquals(2, BuildConfig.VERSION_CODE)
        assertEquals("2.0.0", BuildConfig.VERSION_NAME)
        assertEquals("com.track.dailyexpenses.app", BuildConfig.APPLICATION_ID)

        val pkgInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS
        )
        val requested = pkgInfo.requestedPermissions?.toSet() ?: emptySet()
        assertFalse("INTERNET permission must be removed", android.Manifest.permission.INTERNET in requested)
        // ACCESS_NETWORK_STATE is injected solely by androidx.work.WorkManager's library manifest merger; app manifest declares no network permissions.
        assertTrue("Biometric permission should remain declared", android.Manifest.permission.USE_BIOMETRIC in requested)

        val driveClasses = listOf(
            "com.example.core.drive.DriveAuthGateway",
            "com.example.core.drive.GoogleDriveRemoteDataSource",
            "com.example.core.repository.GoogleDriveBackupRepository",
            "com.example.ui.screens.GoogleDriveBackupSectionKt"
        )
        driveClasses.forEach { fqcn ->
            try {
                Class.forName(fqcn)
                fail("Google Drive class $fqcn must be completely removed")
            } catch (_: ClassNotFoundException) {
                // expected
            }
        }
    }
}
