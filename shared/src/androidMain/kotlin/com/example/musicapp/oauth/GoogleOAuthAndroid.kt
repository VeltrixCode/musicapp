package com.example.musicapp.oauth

import android.content.Intent
import android.net.Uri
import android.util.Base64
import com.example.musicapp.config.AppConfig
import com.example.musicapp.platform.AndroidBridge
import com.example.musicapp.provider.appHttpClient
import com.example.musicapp.security.SecureStorage
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.http.parameters
import kotlinx.serialization.Serializable
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom

@Serializable
private data class TokenResponse(
    val access_token: String? = null,
    val refresh_token: String? = null,
    val expires_in: Long? = null,
    val error: String? = null,
    val error_description: String? = null,
)

object GoogleOAuthAndroid {
    private const val DRIVE_READONLY_SCOPE = "https://www.googleapis.com/auth/drive.readonly"
    private const val REDIRECT_URI = "com.example.musicapp:/oauth2callback"

    private fun generateCodeVerifier(): String {
        val bytes = ByteArray(48)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private fun generateCodeChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(verifier.toByteArray(StandardCharsets.US_ASCII))
        return Base64.encodeToString(hash, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    /**
     * Launches the Google Drive OAuth authorization flow (scope drive.readonly) with PKCE in the Android browser.
     */
    suspend fun performSignIn(
        clientId: String = AppConfig.googleClientId,
        clientSecret: String = AppConfig.googleClientSecret,
    ): Result<String> {
        if (clientId.isBlank()) {
            return Result.failure(IllegalStateException("Google Client ID не настроен."))
        }

        return try {
            val codeVerifier = generateCodeVerifier()
            val codeChallenge = generateCodeChallenge(codeVerifier)
            SecureStorage.saveSecret("google_oauth_verifier", codeVerifier)

            val authUrl = "https://accounts.google.com/o/oauth2/v2/auth?" +
                    "client_id=$clientId&" +
                    "redirect_uri=$REDIRECT_URI&" +
                    "response_type=code&" +
                    "scope=$DRIVE_READONLY_SCOPE&" +
                    "access_type=offline&" +
                    "prompt=consent&" +
                    "code_challenge=$codeChallenge&" +
                    "code_challenge_method=S256"

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            AndroidBridge.appContext.startActivity(intent)

            Result.success("OAuth flow initiated")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Exchanges authorization code received in deep link callback for tokens.
     */
    suspend fun handleAuthCode(
        code: String,
        clientId: String = AppConfig.googleClientId,
        clientSecret: String = AppConfig.googleClientSecret,
    ): Result<String> {
        val codeVerifier = SecureStorage.getSecret("google_oauth_verifier") ?: ""
        return try {
            val response = appHttpClient.submitForm(
                url = "https://oauth2.googleapis.com/token",
                formParameters = parameters {
                    append("code", code)
                    append("client_id", clientId)
                    if (clientSecret.isNotBlank()) append("client_secret", clientSecret)
                    append("redirect_uri", REDIRECT_URI)
                    append("grant_type", "authorization_code")
                    if (codeVerifier.isNotBlank()) append("code_verifier", codeVerifier)
                }
            ).body<TokenResponse>()

            val token = response.access_token
                ?: return Result.failure(IllegalStateException("Failed to exchange code: ${response.error_description ?: response.error}"))

            SecureStorage.saveSecret(AppConfig.TOKEN_GDRIVE_ACCESS, token)
            response.refresh_token?.let {
                SecureStorage.saveSecret(AppConfig.TOKEN_GDRIVE_REFRESH, it)
            }
            SecureStorage.removeSecret("google_oauth_verifier")
            Result.success(token)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Refreshes the Google Drive access token using the stored refresh token.
     */
    suspend fun refreshAccessToken(
        clientId: String = AppConfig.googleClientId,
        clientSecret: String = AppConfig.googleClientSecret,
    ): Result<String> {
        val refreshToken = SecureStorage.getSecret(AppConfig.TOKEN_GDRIVE_REFRESH)
            ?: return Result.failure(IllegalStateException("Refresh token отсутствует."))

        return try {
            val response = appHttpClient.submitForm(
                url = "https://oauth2.googleapis.com/token",
                formParameters = parameters {
                    append("client_id", clientId)
                    if (clientSecret.isNotBlank()) append("client_secret", clientSecret)
                    append("refresh_token", refreshToken)
                    append("grant_type", "refresh_token")
                }
            ).body<TokenResponse>()

            val token = response.access_token
                ?: return Result.failure(IllegalStateException("Ошибка обновления токена: ${response.error_description ?: response.error}"))

            SecureStorage.saveSecret(AppConfig.TOKEN_GDRIVE_ACCESS, token)
            response.refresh_token?.let {
                SecureStorage.saveSecret(AppConfig.TOKEN_GDRIVE_REFRESH, it)
            }
            Result.success(token)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
