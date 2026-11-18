package com.example.core.drive

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.core.data.BackupCrypto
import com.example.core.data.BackupPayloadValidator
import com.example.core.database.AccountDao
import com.example.core.database.BudgetDao
import com.example.core.database.CategoryDao
import com.example.core.database.TransactionDao
import com.example.core.model.BackupPayload
import com.example.core.model.DriveBackupEntry
import com.example.core.repository.GoogleDriveBackupRepository
import com.example.core.repository.PreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EncryptedDriveBackupRequiresPassphraseException :
    Exception("This Google Drive backup is encrypted. Enter its passphrase to restore.")

class GoogleDriveBackupRepositoryImpl(
    private val context: Context,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao,
    private val preferencesRepository: PreferencesRepository,
    private val remoteDataSource: GoogleDriveRemoteDataSource
) : GoogleDriveBackupRepository {

    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return true
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override suspend fun backupToDrive(
        accessToken: String,
        passphrase: String?
    ): Result<DriveBackupEntry> = withContext(Dispatchers.IO) {
        runCatching {
            val prefs = preferencesRepository.preferences.first()
            val exportedAt = System.currentTimeMillis()
            val payload = BackupPayload(
                schemaVersion = 3,
                exportedAt = exportedAt,
                accounts = accountDao.getAll().map { it.toDomain() },
                categories = categoryDao.getAll().map { it.toDomain() },
                transactions = transactionDao.getAll().map { it.toDomain() },
                budgets = budgetDao.getAll().map { it.toDomain() },
                preferences = prefs
            )
            val encodedJson = json.encodeToString(payload)
            val isEncrypted = !passphrase.isNullOrEmpty()
            val bytes = if (isEncrypted) {
                BackupCrypto.encrypt(encodedJson, passphrase!!)
            } else {
                encodedJson.toByteArray(Charsets.UTF_8)
            }
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(exportedAt))
            val ext = if (isEncrypted) "embackup" else "json"
            val fileName = "daily_expenses_backup_$stamp.$ext"

            val entry = remoteDataSource.uploadBackup(
                accessToken = accessToken,
                fileName = fileName,
                bytes = bytes,
                isEncrypted = isEncrypted,
                transactionCount = payload.transactions.size,
                accountCount = payload.accounts.size,
                exportedAt = exportedAt
            ).getOrThrow()

            val modeLabel = if (isEncrypted) "Encrypted Drive backup" else "Google Drive backup"
            preferencesRepository.setLastDriveBackupInfo(
                timestamp = exportedAt,
                status = "$modeLabel (${payload.transactions.size} transactions)"
            )
            entry
        }
    }

    override suspend fun listBackups(accessToken: String): Result<List<DriveBackupEntry>> =
        remoteDataSource.listBackups(accessToken)

    override suspend fun downloadBackupBytes(accessToken: String, fileId: String): Result<ByteArray> =
        remoteDataSource.downloadBackup(accessToken, fileId)

    override fun parseBackupBytes(bytes: ByteArray, passphrase: String?): Result<BackupPayload> =
        runCatching {
            val rawJson = if (BackupCrypto.isEncrypted(bytes)) {
                val pass = passphrase?.takeIf { it.isNotEmpty() }
                    ?: throw EncryptedDriveBackupRequiresPassphraseException()
                BackupCrypto.decrypt(bytes, pass)
            } else {
                bytes.toString(Charsets.UTF_8)
            }
            val payload = json.decodeFromString<BackupPayload>(rawJson)
            BackupPayloadValidator.validate(payload)
            payload
        }

    override suspend fun deleteBackup(accessToken: String, fileId: String): Result<Unit> =
        remoteDataSource.deleteBackup(accessToken, fileId)
}
