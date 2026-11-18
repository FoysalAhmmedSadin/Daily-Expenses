package com.example

import android.content.Context
import android.content.Intent
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkManager
import com.example.core.data.AccountRepositoryImpl
import com.example.core.data.BackupAndExportRepositoryImpl
import com.example.core.data.CategoryRepositoryImpl
import com.example.core.data.PreferencesRepositoryImpl
import com.example.core.data.TransactionRepositoryImpl
import com.example.core.database.ExpenseDatabase
import com.example.core.database.SeedData
import com.example.core.datastore.PreferencesDataSource
import com.example.core.drive.DriveAuthGateway
import com.example.core.drive.DriveAuthOutcome
import com.example.core.drive.GoogleDriveRemoteDataSource
import com.example.core.model.AppAccentColor
import com.example.core.model.BackupPayload
import com.example.core.model.DriveBackupEntry
import com.example.core.model.GoogleDriveAccount
import com.example.core.model.ThemeMode
import com.example.core.repository.GoogleDriveBackupRepository
import com.example.ui.screens.AppearanceSettingsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SettingsViewModel
import com.example.ui.theme.ExpenseManagerTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp")
class AppearanceAndSwitchTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var db: ExpenseDatabase
    private lateinit var prefsRepo: PreferencesRepositoryImpl
    private lateinit var settingsViewModel: SettingsViewModel

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        if (!WorkManager.isInitialized()) {
            WorkManager.initialize(context, Configuration.Builder().build())
        }
        db = Room.inMemoryDatabaseBuilder(context, ExpenseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val accountRepo = AccountRepositoryImpl(db, db.accountDao())
        val categoryRepo = CategoryRepositoryImpl(db.categoryDao())
        val txRepo = TransactionRepositoryImpl(db, db.transactionDao(), db.accountDao(), db.categoryDao())
        prefsRepo = PreferencesRepositoryImpl(PreferencesDataSource(context))
        val backupRepo = BackupAndExportRepositoryImpl(
            context, db, db.accountDao(), db.categoryDao(), db.transactionDao(), db.budgetDao(),
            txRepo, prefsRepo
        )
        runBlocking { SeedData.populateDefaults(db) }
        settingsViewModel = SettingsViewModel(
            preferencesRepository = prefsRepo,
            categoryRepository = categoryRepo,
            transactionRepository = txRepo,
            backupAndExportRepository = backupRepo,
            driveAuthGateway = NoopDriveAuthGateway,
            driveRemoteDataSource = NoopDriveRemote,
            googleDriveBackupRepository = NoopDriveBackupRepo,
            appContext = context
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `amoled theme renders pure black background and dynamic accent updates color scheme`() {
        var capturedBg: Color? = null
        var capturedPrimary: Color? = null

        composeRule.setContent {
            ExpenseManagerTheme(
                themeMode = ThemeMode.AMOLED,
                accentColor = AppAccentColor.VIOLET
            ) {
                capturedBg = MaterialTheme.colorScheme.background
                capturedPrimary = MaterialTheme.colorScheme.primary
            }
        }
        composeRule.waitForIdle()

        assertEquals(Color(0xFF000000), capturedBg)
        assertEquals(Color(AppAccentColor.VIOLET.primaryHex), capturedPrimary)
    }

    @Test
    fun `appearance settings screen selects amoled black and all 8 accent colors`() {
        composeRule.setContent {
            val prefs = settingsViewModel.uiState.value.preferences
            ExpenseManagerTheme(
                themeMode = prefs.themeMode,
                accentColor = prefs.accentColor,
                languageTag = prefs.effectiveLanguageTag
            ) {
                AppearanceSettingsScreen(
                    viewModel = settingsViewModel,
                    onBack = {}
                )
            }
        }
        composeRule.waitForIdle()

        // Verify AMOLED Black theme option & select it
        composeRule.onNodeWithTag("theme_amoled")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            settingsViewModel.uiState.value.preferences.themeMode == ThemeMode.AMOLED
        }

        // Verify all 8 accent colors exist and select Ocean Blue and Rose Pink
        AppAccentColor.entries.forEach { accent ->
            composeRule.onNodeWithTag("accent_color_${accent.name.lowercase()}")
                .performScrollTo()
                .assertIsDisplayed()
        }
        composeRule.onNodeWithTag("accent_color_ocean_blue").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            settingsViewModel.uiState.value.preferences.accentColor == AppAccentColor.OCEAN_BLUE
        }
        composeRule.onNodeWithTag("accent_color_rose_pink").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            settingsViewModel.uiState.value.preferences.accentColor == AppAccentColor.ROSE_PINK
        }

        val persisted = runBlocking { prefsRepo.preferences.first() }
        assertEquals(ThemeMode.AMOLED, persisted.themeMode)
        assertEquals(AppAccentColor.ROSE_PINK, persisted.accentColor)
    }

    @Test
    fun `compact settings screen renders preference rows and off switches cleanly`() {
        runBlocking { prefsRepo.setThemeMode(ThemeMode.AMOLED) }
        composeRule.setContent {
            val prefs = settingsViewModel.uiState.value.preferences
            ExpenseManagerTheme(
                themeMode = prefs.themeMode,
                accentColor = prefs.accentColor,
                languageTag = prefs.effectiveLanguageTag
            ) {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onOpenCategories = {},
                    onOpenAboutDeveloper = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("preferences_section").assertIsDisplayed()
        composeRule.onNodeWithTag("prefs_row_view_mode").assertIsDisplayed()
        composeRule.onNodeWithTag("prefs_row_appearance").assertIsDisplayed()
        composeRule.onNodeWithTag("prefs_row_language").assertIsDisplayed()
        composeRule.onNodeWithTag("prefs_row_currency").assertIsDisplayed()

        // Verify OFF-state switches render cleanly in AMOLED mode
        composeRule.onNodeWithTag("biometric_lock_switch")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("daily_reminder_switch")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("budget_alerts_switch")
            .performScrollTo()
            .assertIsDisplayed()
    }
}

private object NoopDriveAuthGateway : DriveAuthGateway {
    override suspend fun signInWithGoogle(activityContext: Context): Result<GoogleDriveAccount> =
        Result.failure(UnsupportedOperationException())

    override suspend fun authorizeDriveAccess(context: Context): DriveAuthOutcome =
        DriveAuthOutcome.Failed("Noop")

    override fun extractAccessTokenFromIntent(context: Context, data: Intent?): Result<String> =
        Result.failure(UnsupportedOperationException())

    override suspend fun signOut(context: Context) {}
}

private object NoopDriveRemote : GoogleDriveRemoteDataSource {
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

private object NoopDriveBackupRepo : GoogleDriveBackupRepository {
    override fun isNetworkAvailable(): Boolean = false
    override suspend fun backupToDrive(accessToken: String, passphrase: String?): Result<DriveBackupEntry> =
        Result.failure(UnsupportedOperationException())
    override suspend fun listBackups(accessToken: String): Result<List<DriveBackupEntry>> =
        Result.success(emptyList())
    override suspend fun downloadBackupBytes(accessToken: String, fileId: String): Result<ByteArray> =
        Result.failure(UnsupportedOperationException())
    override fun parseBackupBytes(bytes: ByteArray, passphrase: String?): Result<BackupPayload> =
        Result.failure(UnsupportedOperationException())
    override suspend fun deleteBackup(accessToken: String, fileId: String): Result<Unit> =
        Result.success(Unit)
}
