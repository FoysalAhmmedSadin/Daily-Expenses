package com.example.core.repository

import android.net.Uri
import com.example.core.model.Account
import com.example.core.model.AppAccentColor
import com.example.core.model.AppCurrency
import com.example.core.model.AppLanguage
import com.example.core.model.BackupPayload
import com.example.core.model.Budget
import com.example.core.model.Category
import com.example.core.model.DriveBackupEntry
import com.example.core.model.DriveBackupSchedule
import com.example.core.model.NumberFormatStyle
import com.example.core.model.ThemeMode
import com.example.core.model.Transaction
import com.example.core.model.TransactionWithDetails
import com.example.core.model.UserPreferences
import com.example.core.model.ViewMode
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun observeAccounts(): Flow<List<Account>>
    fun observeAccountById(id: Long): Flow<Account?>
    suspend fun getAccounts(): List<Account>
    suspend fun saveAccount(account: Account): Long
    suspend fun deleteAccount(id: Long)
}

interface CategoryRepository {
    fun observeCategories(): Flow<List<Category>>
    suspend fun getCategories(): List<Category>
    suspend fun saveCategory(category: Category): Long
    suspend fun deleteCategory(id: Long)
}

interface TransactionRepository {
    fun observeTransactionsWithDetails(): Flow<List<TransactionWithDetails>>
    fun observeTransactionById(id: Long): Flow<TransactionWithDetails?>
    suspend fun getTransactionsWithDetails(): List<TransactionWithDetails>
    suspend fun saveTransaction(transaction: Transaction): Long
    suspend fun deleteTransaction(id: Long)
    suspend fun processDueRecurringTransactions(nowMillis: Long = System.currentTimeMillis()): Int
}

interface BudgetRepository {
    fun observeBudgetsForMonth(monthYear: String): Flow<List<Budget>>
    fun observeAllBudgets(): Flow<List<Budget>>
    suspend fun saveBudget(budget: Budget): Long
    suspend fun deleteBudget(id: Long)
}

interface PreferencesRepository {
    val preferences: Flow<UserPreferences>
    suspend fun setCurrency(currency: AppCurrency)
    suspend fun setCurrencyCode(code: String)
    suspend fun setLanguage(language: AppLanguage)
    suspend fun setLanguageTag(tag: String)
    suspend fun setViewMode(viewMode: ViewMode)
    suspend fun setNumberFormat(style: NumberFormatStyle)
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setAccentColor(color: AppAccentColor)
    suspend fun setBiometricLock(enabled: Boolean)
    suspend fun setDailyReminder(enabled: Boolean, hour: Int? = null, minute: Int? = null)
    suspend fun setBudgetAlerts(enabled: Boolean)
    suspend fun setHideBalances(hidden: Boolean)
    suspend fun setLastBackupInfo(timestamp: Long, status: String)
    suspend fun setDriveAccount(email: String?, displayName: String?)
    suspend fun setDriveBackupSchedule(schedule: DriveBackupSchedule)
    suspend fun setLastDriveBackupInfo(timestamp: Long, status: String)
}

interface BackupAndExportRepository {
    suspend fun exportCsvToUri(uri: Uri, monthYear: String? = null): Result<Int>
    suspend fun exportPdfToUri(uri: Uri): Result<Int>
    suspend fun createEncryptedBackupToUri(uri: Uri, passphrase: String): Result<BackupPayload>
    suspend fun readEncryptedBackupFromUri(uri: Uri, passphrase: String): Result<BackupPayload>
    suspend fun restoreBackup(payload: BackupPayload): Result<Unit>
    suspend fun clearAllFinancialData(): Result<Unit>
    suspend fun copyReceiptImageToInternal(uri: Uri): Result<String>
}

interface GoogleDriveBackupRepository {
    fun isNetworkAvailable(): Boolean
    suspend fun backupToDrive(accessToken: String, passphrase: String? = null): Result<DriveBackupEntry>
    suspend fun listBackups(accessToken: String): Result<List<DriveBackupEntry>>
    suspend fun downloadBackupBytes(accessToken: String, fileId: String): Result<ByteArray>
    fun parseBackupBytes(bytes: ByteArray, passphrase: String? = null): Result<BackupPayload>
    suspend fun deleteBackup(accessToken: String, fileId: String): Result<Unit>
}
