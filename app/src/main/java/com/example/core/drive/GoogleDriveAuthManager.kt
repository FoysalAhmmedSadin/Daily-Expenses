package com.example.core.drive

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentSender
import android.content.MutableContextWrapper
import android.util.Base64
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.BuildConfig
import com.example.core.model.GoogleDriveAccount
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.suspendCancellableCoroutine
import java.security.SecureRandom
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed interface DriveAuthOutcome {
    data class Authorized(val accessToken: String) : DriveAuthOutcome
    data class NeedsResolution(val intentSender: IntentSender) : DriveAuthOutcome
    data class Failed(val message: String, val cause: Throwable? = null) : DriveAuthOutcome
}

interface DriveAuthGateway {
    suspend fun signInWithGoogle(activityContext: Context): Result<GoogleDriveAccount>
    suspend fun authorizeDriveAccess(context: Context): DriveAuthOutcome
    fun extractAccessTokenFromIntent(context: Context, data: Intent?): Result<String>
    suspend fun signOut(context: Context)
}

class GoogleDriveAuthManager : DriveAuthGateway {

    private val requestedScopes = listOf(
        Scope(SCOPE_DRIVE_APPDATA),
        Scope(SCOPE_USERINFO_EMAIL)
    )

    override suspend fun signInWithGoogle(activityContext: Context): Result<GoogleDriveAccount> {
        val clientId = resolveWebClientId(activityContext)
        if (clientId.isBlank()) {
            return Result.failure(
                IllegalStateException("Google OAuth Client ID is not configured for this build.")
            )
        }
        return runCatching {
            val hostContext = activityContext.findActivity() ?: activityContext
            val credentialManager = CredentialManager.create(hostContext)
            val option = GetSignInWithGoogleOption.Builder(serverClientId = clientId)
                .setNonce(generateSecureNonce())
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(option)
                .build()

            val mutableContext = MutableContextWrapper(hostContext)
            val response = credentialManager.getCredential(
                context = mutableContext,
                request = request
            )
            val credential = response.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
                GoogleDriveAccount(
                    email = googleCred.id,
                    displayName = googleCred.displayName
                )
            } else {
                error("Unexpected credential type returned by Google Sign-In.")
            }
        }.recoverCatching { err ->
            when (err) {
                is GetCredentialCancellationException ->
                    throw IllegalStateException("Google Sign-In was cancelled.", err)
                is NoCredentialException ->
                    throw NoGoogleCredentialAvailableException(
                        "No Google Account credential available in Credential Manager.",
                        err
                    )
                is GetCredentialException ->
                    throw IllegalStateException(err.errorMessage?.toString() ?: "Google Sign-In failed.", err)
                else -> throw err
            }
        }
    }

    override suspend fun authorizeDriveAccess(context: Context): DriveAuthOutcome {
        return try {
            val hostContext = context.findActivity() ?: context
            val requestBuilder = AuthorizationRequest.builder()
                .setRequestedScopes(requestedScopes)
            val result = Identity.getAuthorizationClient(hostContext)
                .authorize(requestBuilder.build())
                .awaitResult()

            if (result.hasResolution()) {
                val pendingIntent = result.pendingIntent
                if (pendingIntent != null) {
                    DriveAuthOutcome.NeedsResolution(pendingIntent.intentSender)
                } else {
                    DriveAuthOutcome.Failed("Google Drive permission resolution unavailable.")
                }
            } else {
                val granted = result.grantedScopes
                if (granted.isNotEmpty() && granted.none { it.equals(SCOPE_DRIVE_APPDATA, ignoreCase = true) }) {
                    DriveAuthOutcome.Failed(
                        "Google Drive permission was not granted. Please allow Google Drive access."
                    )
                } else {
                    val token = result.accessToken
                    if (!token.isNullOrBlank()) {
                        DriveAuthOutcome.Authorized(token)
                    } else {
                        DriveAuthOutcome.Failed("Google Drive did not return an access token.")
                    }
                }
            }
        } catch (e: ApiException) {
            DriveAuthOutcome.Failed(mapApiExceptionMessage(e), e)
        } catch (t: Throwable) {
            DriveAuthOutcome.Failed(t.message ?: "Google Drive authorization failed.", t)
        }
    }

    override fun extractAccessTokenFromIntent(context: Context, data: Intent?): Result<String> = runCatching {
        val hostContext = context.findActivity() ?: context
        val result = Identity.getAuthorizationClient(hostContext)
            .getAuthorizationResultFromIntent(data)
        val granted = result.grantedScopes
        require(granted.isEmpty() || granted.any { it.equals(SCOPE_DRIVE_APPDATA, ignoreCase = true) }) {
            "Google Drive permission was not granted. Please allow Google Drive access when prompted."
        }
        val token = result.accessToken
        require(!token.isNullOrBlank()) { "Authorization completed without an access token." }
        token
    }.recoverCatching { err ->
        if (err is ApiException) {
            throw IllegalStateException(mapApiExceptionMessage(err), err)
        }
        throw err
    }

    override suspend fun signOut(context: Context) {
        runCatching {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        }
    }

    private fun resolveWebClientId(context: Context): String {
        val configured = BuildConfig.GOOGLE_OAUTH_CLIENT_ID
        if (configured.isNotBlank()) return configured
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        return if (resId != 0) context.getString(resId) else ""
    }

    private fun mapApiExceptionMessage(e: ApiException): String = when (e.statusCode) {
        CommonStatusCodes.DEVELOPER_ERROR ->
            "Google Drive OAuth configuration mismatch (code 10). Verify Android SHA-1 and OAuth client."
        CommonStatusCodes.CANCELED ->
            "Google Drive permission request was cancelled or not allowed."
        CommonStatusCodes.NETWORK_ERROR ->
            "Network error while verifying Google Drive permissions."
        CommonStatusCodes.SIGN_IN_REQUIRED ->
            "Please sign in with a Google account to allow Google Drive backup."
        else ->
            "Google Drive authorization failed (code ${e.statusCode})."
    }

    private fun generateSecureNonce(): String {
        val bytes = ByteArray(24)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING)
    }

    companion object {
        const val SCOPE_DRIVE_APPDATA = "https://www.googleapis.com/auth/drive.appdata"
        const val SCOPE_USERINFO_EMAIL = "https://www.googleapis.com/auth/userinfo.email"
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

class NoGoogleCredentialAvailableException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result ->
        if (cont.isActive) cont.resume(result)
    }
    addOnFailureListener { exception ->
        if (cont.isActive) cont.resumeWithException(exception)
    }
    addOnCanceledListener {
        if (cont.isActive) cont.cancel()
    }
}
