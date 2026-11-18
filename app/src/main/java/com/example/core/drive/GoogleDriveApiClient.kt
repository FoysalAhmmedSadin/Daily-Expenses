package com.example.core.drive

import com.example.core.model.DriveBackupEntry
import com.example.core.model.GoogleDriveAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class DriveAuthException(message: String) : Exception(message)
class DrivePermissionOrQuotaException(message: String) : Exception(message)
class DriveNetworkException(message: String, cause: Throwable? = null) : Exception(message, cause)

interface GoogleDriveRemoteDataSource {
    suspend fun fetchUserInfo(accessToken: String): Result<GoogleDriveAccount>
    suspend fun listBackups(accessToken: String): Result<List<DriveBackupEntry>>
    suspend fun uploadBackup(
        accessToken: String,
        fileName: String,
        bytes: ByteArray,
        isEncrypted: Boolean,
        transactionCount: Int,
        accountCount: Int,
        exportedAt: Long
    ): Result<DriveBackupEntry>
    suspend fun downloadBackup(accessToken: String, fileId: String): Result<ByteArray>
    suspend fun deleteBackup(accessToken: String, fileId: String): Result<Unit>
}

class GoogleDriveApiClient(
    private val httpClient: OkHttpClient = defaultHttpClient(),
    private val driveApiBaseUrl: HttpUrl = DEFAULT_DRIVE_API_URL.toHttpUrl(),
    private val driveUploadBaseUrl: HttpUrl = DEFAULT_DRIVE_UPLOAD_URL.toHttpUrl(),
    private val userInfoUrl: HttpUrl = DEFAULT_USERINFO_URL.toHttpUrl()
) : GoogleDriveRemoteDataSource {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun fetchUserInfo(accessToken: String): Result<GoogleDriveAccount> =
        withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder()
                    .url(userInfoUrl)
                    .header("Authorization", "Bearer $accessToken")
                    .get()
                    .build()

                executeString(request) { body ->
                    val dto = json.decodeFromString<UserInfoDto>(body)
                    val email = dto.email?.takeIf { it.isNotBlank() }
                        ?: error("Google account email not available.")
                    GoogleDriveAccount(email = email, displayName = dto.name)
                }
            }
        }

    override suspend fun listBackups(accessToken: String): Result<List<DriveBackupEntry>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = driveApiBaseUrl.newBuilder()
                    .addPathSegment("files")
                    .addQueryParameter("spaces", "appDataFolder")
                    .addQueryParameter(
                        "fields",
                        "files(id,name,createdTime,modifiedTime,size,appProperties)"
                    )
                    .addQueryParameter("orderBy", "modifiedTime desc")
                    .addQueryParameter("pageSize", "20")
                    .build()

                val request = Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer $accessToken")
                    .get()
                    .build()

                executeString(request) { body ->
                    val response = json.decodeFromString<DriveFileListDto>(body)
                    response.files.map { it.toDomain() }
                }
            }
        }

    override suspend fun uploadBackup(
        accessToken: String,
        fileName: String,
        bytes: ByteArray,
        isEncrypted: Boolean,
        transactionCount: Int,
        accountCount: Int,
        exportedAt: Long
    ): Result<DriveBackupEntry> = withContext(Dispatchers.IO) {
        runCatching {
            val mimeType = if (isEncrypted) "application/octet-stream" else "application/json"
            val metadata = DriveUploadMetadataDto(
                name = fileName,
                parents = listOf("appDataFolder"),
                mimeType = mimeType,
                appProperties = mapOf(
                    "app" to "daily_expenses",
                    "schemaVersion" to "2",
                    "encrypted" to isEncrypted.toString(),
                    "txCount" to transactionCount.toString(),
                    "accountCount" to accountCount.toString(),
                    "exportedAt" to exportedAt.toString()
                )
            )
            val metadataJson = json.encodeToString(metadata)

            val multipartBody = MultipartBody.Builder()
                .setType("multipart/related".toMediaType())
                .addPart(metadataJson.toRequestBody("application/json; charset=UTF-8".toMediaType()))
                .addPart(bytes.toRequestBody(mimeType.toMediaType()))
                .build()

            val url = driveUploadBaseUrl.newBuilder()
                .addPathSegment("files")
                .addQueryParameter("uploadType", "multipart")
                .addQueryParameter("fields", "id,name,createdTime,modifiedTime,size,appProperties")
                .build()

            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $accessToken")
                .post(multipartBody)
                .build()

            executeString(request) { body ->
                json.decodeFromString<DriveFileDto>(body).toDomain()
            }
        }
    }

    override suspend fun downloadBackup(accessToken: String, fileId: String): Result<ByteArray> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = driveApiBaseUrl.newBuilder()
                    .addPathSegment("files")
                    .addPathSegment(fileId)
                    .addQueryParameter("alt", "media")
                    .build()

                val request = Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer $accessToken")
                    .get()
                    .build()

                try {
                    httpClient.newCall(request).execute().use { resp ->
                        if (!resp.isSuccessful) {
                            val errText = resp.body?.string().orEmpty()
                            throw mapHttpFailure(resp.code, errText)
                        }
                        resp.body?.bytes() ?: error("Empty response from Google Drive.")
                    }
                } catch (io: IOException) {
                    throw DriveNetworkException("Network error while downloading backup from Google Drive.", io)
                }
            }
        }

    override suspend fun deleteBackup(accessToken: String, fileId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = driveApiBaseUrl.newBuilder()
                    .addPathSegment("files")
                    .addPathSegment(fileId)
                    .build()

                val request = Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer $accessToken")
                    .delete()
                    .build()

                try {
                    httpClient.newCall(request).execute().use { resp ->
                        if (!resp.isSuccessful && resp.code != 404) {
                            val errText = resp.body?.string().orEmpty()
                            throw mapHttpFailure(resp.code, errText)
                        }
                    }
                } catch (io: IOException) {
                    throw DriveNetworkException("Network error while deleting Google Drive backup.", io)
                }
            }
        }

    private inline fun <T> executeString(request: Request, parser: (String) -> T): T {
        return try {
            httpClient.newCall(request).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    throw mapHttpFailure(resp.code, body)
                }
                parser(body)
            }
        } catch (io: IOException) {
            throw DriveNetworkException("Unable to reach Google Drive. Check your internet connection.", io)
        }
    }

    private fun mapHttpFailure(code: Int, rawBody: String): Exception {
        val parsedMessage = runCatching {
            json.decodeFromString<GoogleApiErrorWrapper>(rawBody).error?.message
        }.getOrNull()
        val detail = parsedMessage?.takeIf { it.isNotBlank() } ?: "HTTP $code"
        return when (code) {
            401 -> DriveAuthException("Google Drive authorization expired. Please reconnect your account.")
            403 -> {
                if (rawBody.contains("storageQuotaExceeded", ignoreCase = true) ||
                    rawBody.contains("quota", ignoreCase = true)
                ) {
                    DrivePermissionOrQuotaException("Google Drive storage quota exceeded.")
                } else {
                    DrivePermissionOrQuotaException("Google Drive access denied: $detail")
                }
            }
            else -> IllegalStateException("Google Drive error: $detail")
        }
    }

    private fun DriveFileDto.toDomain(): DriveBackupEntry {
        val parsedTime = appProperties?.get("exportedAt")?.toLongOrNull()
            ?: parseRfc3339Millis(modifiedTime ?: createdTime)
            ?: System.currentTimeMillis()
        val encrypted = appProperties?.get("encrypted")?.toBooleanStrictOrNull()
            ?: name.endsWith(".embackup", ignoreCase = true)
        return DriveBackupEntry(
            fileId = id,
            fileName = name,
            modifiedTimeMillis = parsedTime,
            sizeBytes = size?.toLongOrNull() ?: 0L,
            isEncrypted = encrypted,
            transactionCount = appProperties?.get("txCount")?.toIntOrNull(),
            accountCount = appProperties?.get("accountCount")?.toIntOrNull()
        )
    }

    private fun parseRfc3339Millis(value: String?): Long? {
        if (value.isNullOrBlank()) return null
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'"
        )
        for (pattern in patterns) {
            val parsed = runCatching {
                SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.parse(value)?.time
            }.getOrNull()
            if (parsed != null) return parsed
        }
        return null
    }

    @Serializable
    private data class DriveFileListDto(
        val files: List<DriveFileDto> = emptyList()
    )

    @Serializable
    private data class DriveFileDto(
        val id: String,
        val name: String,
        val createdTime: String? = null,
        val modifiedTime: String? = null,
        val size: String? = null,
        val appProperties: Map<String, String>? = null
    )

    @Serializable
    private data class DriveUploadMetadataDto(
        val name: String,
        val parents: List<String>,
        val mimeType: String,
        val appProperties: Map<String, String>
    )

    @Serializable
    private data class UserInfoDto(
        val email: String? = null,
        val name: String? = null
    )

    @Serializable
    private data class GoogleApiErrorWrapper(
        val error: GoogleApiErrorBody? = null
    )

    @Serializable
    private data class GoogleApiErrorBody(
        @SerialName("code") val code: Int? = null,
        @SerialName("message") val message: String? = null
    )

    companion object {
        const val DEFAULT_DRIVE_API_URL = "https://www.googleapis.com/drive/v3/"
        const val DEFAULT_DRIVE_UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/"
        const val DEFAULT_USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo"

        fun defaultHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(45, TimeUnit.SECONDS)
            .build()
    }
}
