package com.example

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.core.data.AccountRepositoryImpl
import com.example.core.data.BackupAndExportRepositoryImpl
import com.example.core.data.BudgetRepositoryImpl
import com.example.core.data.CategoryRepositoryImpl
import com.example.core.data.PreferencesRepositoryImpl
import com.example.core.data.TransactionRepositoryImpl
import com.example.core.database.ExpenseDatabase
import com.example.core.database.SeedData
import com.example.core.datastore.PreferencesDataSource
import com.example.core.drive.DriveAuthException
import com.example.core.drive.DriveAuthGateway
import com.example.core.drive.DriveAuthOutcome
import com.example.core.drive.DrivePermissionOrQuotaException
import com.example.core.drive.EncryptedDriveBackupRequiresPassphraseException
import com.example.core.drive.GoogleDriveApiClient
import com.example.core.drive.GoogleDriveBackupRepositoryImpl
import com.example.core.drive.GoogleDriveRemoteDataSource
import com.example.core.model.Account
import com.example.core.model.AccountType
import com.example.core.model.DriveBackupEntry
import com.example.core.model.DriveBackupSchedule
import com.example.core.model.GoogleDriveAccount
import com.example.core.model.ThemeMode
import com.example.core.model.Transaction
import com.example.core.model.TransactionType
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SettingsViewModel
import com.example.ui.theme.ExpenseManagerTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp")
class GoogleDriveBackupTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var db: ExpenseDatabase
    private lateinit var accountRepo: AccountRepositoryImpl
    private lateinit var categoryRepo: CategoryRepositoryImpl
    private lateinit var txRepo: TransactionRepositoryImpl
    private lateinit var budgetRepo: BudgetRepositoryImpl
    private lateinit var prefsRepo: PreferencesRepositoryImpl
    private lateinit var localBackupRepo: BackupAndExportRepositoryImpl

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
        localBackupRepo = BackupAndExportRepositoryImpl(
            context, db, db.accountDao(), db.categoryDao(), db.transactionDao(), db.budgetDao(),
            txRepo, prefsRepo
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `drive api client lists uploads downloads and maps 401 and quota errors`() = runTest {
        val server = MockWebServer()
        server.start()
        try {
            val client = GoogleDriveApiClient(
                httpClient = OkHttpClient(),
                driveApiBaseUrl = server.url("/drive/v3/"),
                driveUploadBaseUrl = server.url("/upload/drive/v3/"),
                userInfoUrl = server.url("/oauth2/v3/userinfo")
            )

            // 1. Upload backup
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                        {
                          "id": "file-123",
                          "name": "daily_expenses_backup_20261113.embackup",
                          "modifiedTime": "2026-11-13T10:15:30.000Z",
                          "size": "512",
                          "appProperties": {
                            "encrypted": "true",
                            "txCount": "4",
                            "accountCount": "2",
                            "exportedAt": "1794564930000"
                          }
                        }
                        """.trimIndent()
                    )
            )
            val uploaded = client.uploadBackup(
                accessToken = "tok-abc",
                fileName = "daily_expenses_backup_20261113.embackup",
                bytes = byteArrayOf(1, 2, 3),
                isEncrypted = true,
                transactionCount = 4,
                accountCount = 2,
                exportedAt = 1794564930000L
            ).getOrThrow()

            val uploadReq = server.takeRequest()
            assertEquals("Bearer tok-abc", uploadReq.getHeader("Authorization"))
            assertTrue(uploadReq.path!!.contains("uploadType=multipart"))
            assertEquals("file-123", uploaded.fileId)
            assertTrue(uploaded.isEncrypted)
            assertEquals(4, uploaded.transactionCount)

            // 2. List backups
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                        {
                          "files": [
                            {
                              "id": "file-123",
                              "name": "daily_expenses_backup_20261113.embackup",
                              "size": "512",
                              "appProperties": {"encrypted": "true", "txCount": "4"}
                            }
                          ]
                        }
                        """.trimIndent()
                    )
            )
            val list = client.listBackups("tok-abc").getOrThrow()
            val listReq = server.takeRequest()
            assertTrue(listReq.path!!.contains("spaces=appDataFolder"))
            assertEquals(1, list.size)

            // 3. 401 Auth error
            server.enqueue(
                MockResponse()
                    .setResponseCode(401)
                    .setBody("""{"error":{"code":401,"message":"Invalid Credentials"}}""")
            )
            val unauth = client.listBackups("expired-tok")
            assertTrue(unauth.exceptionOrNull() is DriveAuthException)

            // 4. 403 Storage quota exceeded
            server.enqueue(
                MockResponse()
                    .setResponseCode(403)
                    .setBody("""{"error":{"code":403,"message":"storageQuotaExceeded"}}""")
            )
            val quotaErr = client.downloadBackup("tok-abc", "file-123")
            assertTrue(quotaErr.exceptionOrNull() is DrivePermissionOrQuotaException)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `drive backup repository creates encrypted and standard backups and validates restore`() = runTest {
        SeedData.populateDefaults(db)
        val accId = accountRepo.saveAccount(
            Account(name = "Cloud Wallet", type = AccountType.CASH, initialBalance = 5_000.0)
        )
        val catId = categoryRepo.getCategories().first { it.type == TransactionType.EXPENSE }.id
        txRepo.saveTransaction(
            Transaction(
                type = TransactionType.EXPENSE,
                amount = 420.0,
                categoryId = catId,
                accountId = accId,
                note = "Coffee & Lunch"
            )
        )

        val fakeRemote = InMemoryDriveRemote()
        val repo = GoogleDriveBackupRepositoryImpl(
            context, db.accountDao(), db.categoryDao(), db.transactionDao(), db.budgetDao(),
            prefsRepo, fakeRemote
        )

        // Encrypted cloud backup
        val encEntry = repo.backupToDrive("tok-1", passphrase = "secret123").getOrThrow()
        assertTrue(encEntry.isEncrypted)
        val encBytes = repo.downloadBackupBytes("tok-1", encEntry.fileId).getOrThrow()

        val missingPass = repo.parseBackupBytes(encBytes, passphrase = null)
        assertTrue(missingPass.exceptionOrNull() is EncryptedDriveBackupRequiresPassphraseException)

        val wrongPass = repo.parseBackupBytes(encBytes, passphrase = "wrong")
        assertTrue(wrongPass.isFailure)

        val parsedEnc = repo.parseBackupBytes(encBytes, passphrase = "secret123").getOrThrow()
        assertEquals(1, parsedEnc.transactions.size)
        assertEquals("Coffee & Lunch", parsedEnc.transactions.first().note)

        // Unencrypted automatic backup + full restore round-trip
        val jsonEntry = repo.backupToDrive("tok-1", passphrase = null).getOrThrow()
        assertTrue(!jsonEntry.isEncrypted)
        val jsonBytes = repo.downloadBackupBytes("tok-1", jsonEntry.fileId).getOrThrow()
        val parsedJson = repo.parseBackupBytes(jsonBytes, passphrase = null).getOrThrow()

        localBackupRepo.clearAllFinancialData().getOrThrow()
        assertEquals(0, txRepo.getTransactionsWithDetails().size)

        localBackupRepo.restoreBackup(parsedJson).getOrThrow()
        val restoredTxs = txRepo.getTransactionsWithDetails()
        assertEquals(1, restoredTxs.size)
        assertEquals(420.0, restoredTxs.first().transaction.amount, 0.001)
        assertNotNull(prefsRepo.preferences.first().lastDriveBackupAt)
    }

    @Test
    fun `settings screen connects google drive runs backup schedules daily sync and restores`() {
        val fakeAuth = FakeDriveAuthGateway(GoogleDriveAccount("user@example.com", "Test User"))
        val fakeRemote = InMemoryDriveRemote()
        val driveRepo = GoogleDriveBackupRepositoryImpl(
            context, db.accountDao(), db.categoryDao(), db.transactionDao(), db.budgetDao(),
            prefsRepo, fakeRemote
        )
        val settingsViewModel = runBlocking {
            SeedData.populateDefaults(db)
            val accId = accountRepo.saveAccount(
                Account(name = "Main Bank", type = AccountType.BANK, initialBalance = 8_000.0)
            )
            val catId = categoryRepo.getCategories().first { it.type == TransactionType.EXPENSE }.id
            txRepo.saveTransaction(
                Transaction(
                    type = TransactionType.EXPENSE,
                    amount = 250.0,
                    categoryId = catId,
                    accountId = accId,
                    note = "Cloud Sync Test"
                )
            )
            val vm = SettingsViewModel(
                preferencesRepository = prefsRepo,
                categoryRepository = categoryRepo,
                transactionRepository = txRepo,
                backupAndExportRepository = localBackupRepo,
                driveAuthGateway = fakeAuth,
                driveRemoteDataSource = fakeRemote,
                googleDriveBackupRepository = driveRepo,
                appContext = context
            )
            vm.uiState.first { it.categories.isNotEmpty() }
            vm
        }

        composeRule.setContent {
            ExpenseManagerTheme(themeMode = ThemeMode.LIGHT) {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onOpenCategories = {},
                    onOpenAboutDeveloper = {}
                )
            }
        }
        composeRule.waitForIdle()

        // Connect Google Drive
        composeRule.onNodeWithTag("drive_connect_button")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            settingsViewModel.uiState.value.preferences.driveAccountEmail == "user@example.com"
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("drive_connected_account_card")
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextContains("user@example.com", substring = true)

        // Schedule Daily automatic backup
        composeRule.onNodeWithTag("drive_schedule_daily")
            .performScrollTo()
            .performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            settingsViewModel.uiState.value.preferences.driveBackupSchedule == DriveBackupSchedule.DAILY
        }
        val scheduledWork = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork("google_drive_auto_backup_work")
            .get()
        assertTrue(scheduledWork.any {
            it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING
        })

        // Trigger manual backup to Google Drive
        composeRule.onNodeWithTag("drive_backup_now_button")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("drive_confirm_backup_button").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            settingsViewModel.uiState.value.driveBackups.isNotEmpty()
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("drive_last_backup_card")
            .performScrollTo()
            .assertIsDisplayed()

        // Open Drive Backup History & trigger Restore Confirmation
        composeRule.onNodeWithTag("drive_restore_button")
            .performScrollTo()
            .performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            settingsViewModel.uiState.value.driveHistoryVisible &&
                settingsViewModel.uiState.value.driveBackups.isNotEmpty()
        }
        composeRule.waitForIdle()

        val firstFileId = settingsViewModel.uiState.value.driveBackups.first().fileId
        composeRule.onNodeWithTag("drive_restore_entry_$firstFileId").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            settingsViewModel.uiState.value.pendingRestorePayload != null
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("safety backup", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun `connectGoogleDrive does not mark account connected when drive permission fails`() = runTest {
        val fakeAuth = FakeDriveAuthGateway(
            account = GoogleDriveAccount("user@example.com", "Test User"),
            authOutcome = DriveAuthOutcome.Failed("Google Drive permission was not granted.")
        )
        val fakeRemote = InMemoryDriveRemote()
        val driveRepo = GoogleDriveBackupRepositoryImpl(
            context, db.accountDao(), db.categoryDao(), db.transactionDao(), db.budgetDao(),
            prefsRepo, fakeRemote
        )
        val vm = SettingsViewModel(
            preferencesRepository = prefsRepo,
            categoryRepository = categoryRepo,
            transactionRepository = txRepo,
            backupAndExportRepository = localBackupRepo,
            driveAuthGateway = fakeAuth,
            driveRemoteDataSource = fakeRemote,
            googleDriveBackupRepository = driveRepo,
            appContext = context
        )

        vm.connectGoogleDrive(context, isSwitchAccount = false)
        val state = vm.uiState.first { !it.isDriveBusy && it.statusMessage != null }
        assertEquals(null, state.preferences.driveAccountEmail)
        assertTrue(state.statusMessage!!.contains("permission", ignoreCase = true))
    }
}

private class FakeDriveAuthGateway(
    private val account: GoogleDriveAccount,
    private val authOutcome: DriveAuthOutcome = DriveAuthOutcome.Authorized("fake-access-token")
) : DriveAuthGateway {
    override suspend fun signInWithGoogle(activityContext: Context): Result<GoogleDriveAccount> =
        Result.success(account)

    override suspend fun authorizeDriveAccess(context: Context): DriveAuthOutcome = authOutcome

    override fun extractAccessTokenFromIntent(context: Context, data: Intent?): Result<String> =
        Result.success("fake-access-token")

    override suspend fun signOut(context: Context) {}
}

private class InMemoryDriveRemote : GoogleDriveRemoteDataSource {
    private var counter = 1
    private val store = LinkedHashMap<String, Pair<DriveBackupEntry, ByteArray>>()

    override suspend fun fetchUserInfo(accessToken: String): Result<GoogleDriveAccount> =
        Result.success(GoogleDriveAccount("user@example.com", "Test User"))

    override suspend fun listBackups(accessToken: String): Result<List<DriveBackupEntry>> =
        Result.success(store.values.map { it.first }.reversed())

    override suspend fun uploadBackup(
        accessToken: String,
        fileName: String,
        bytes: ByteArray,
        isEncrypted: Boolean,
        transactionCount: Int,
        accountCount: Int,
        exportedAt: Long
    ): Result<DriveBackupEntry> {
        val id = "drive-file-${counter++}"
        val entry = DriveBackupEntry(
            fileId = id,
            fileName = fileName,
            modifiedTimeMillis = exportedAt,
            sizeBytes = bytes.size.toLong(),
            isEncrypted = isEncrypted,
            transactionCount = transactionCount,
            accountCount = accountCount
        )
        store[id] = entry to bytes
        return Result.success(entry)
    }

    override suspend fun downloadBackup(accessToken: String, fileId: String): Result<ByteArray> {
        val bytes = store[fileId]?.second ?: return Result.failure(IllegalArgumentException("Not found"))
        return Result.success(bytes)
    }

    override suspend fun deleteBackup(accessToken: String, fileId: String): Result<Unit> {
        store.remove(fileId)
        return Result.success(Unit)
    }
}
