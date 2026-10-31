package com.example.musicapp.oauth

import com.example.musicapp.config.AppConfig
import com.example.musicapp.provider.appHttpClient
import com.example.musicapp.security.SecureStorage
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.http.parameters
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import java.awt.Desktop
import java.net.ServerSocket
import java.net.URI
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

@Serializable
private data class TokenResponse(
    val access_token: String? = null,
    val refresh_token: String? = null,
    val expires_in: Long? = null,
    val error: String? = null,
    val error_description: String? = null,
)

object GoogleOAuthDesktop {

    private fun generateCodeVerifier(): String {
        val bytes = ByteArray(48)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun generateCodeChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(verifier.toByteArray(StandardCharsets.US_ASCII))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash)
    }

    private fun findFreePort(): Int {
        ServerSocket(0).use { ss ->
            return ss.localPort
        }
    }

    suspend fun performSignIn(
        clientId: String = AppConfig.googleClientId,
        clientSecret: String = AppConfig.googleClientSecret,
    ): Result<String> {
        if (clientId.isBlank()) {
            return Result.failure(IllegalStateException("Google Client ID не настроен. Задайте его в настройках или GOOGLE_CLIENT_ID."))
        }

        val dynamicPort = findFreePort()
        val redirectUri = "http://127.0.0.1:$dynamicPort/oauth2callback"
        val codeVerifier = generateCodeVerifier()
        val codeChallenge = generateCodeChallenge(codeVerifier)
        val authCodeDeferred = CompletableDeferred<String>()

        val server = embeddedServer(CIO, port = dynamicPort, host = "127.0.0.1") {
            routing {
                get("/oauth2callback") {
                    val code = call.request.queryParameters["code"]
                    val error = call.request.queryParameters["error"]
                    if (code != null) {
                        authCodeDeferred.complete(code)
                        call.respondText("<html><body style='background:#121212;color:#fff;font-family:sans-serif;padding:40px;text-align:center;'><h2>Авторизация JVXMusic успешна!</h2><p>Вы можете закрыть эту вкладку и вернуться в плеер.</p></body></html>", io.ktor.http.ContentType.Text.Html)
                    } else {
                        authCodeDeferred.completeExceptionally(IllegalStateException(error ?: "User cancelled OAuth"))
                        call.respondText("Ошибка авторизации: $error", status = io.ktor.http.HttpStatusCode.BadRequest)
                    }
                }
            }
        }.start(wait = false)

        try {
            val scope = "https://www.googleapis.com/auth/drive.readonly"
            val authUrl = "https://accounts.google.com/o/oauth2/v2/auth?" +
                    "client_id=$clientId&" +
                    "redirect_uri=$redirectUri&" +
                    "response_type=code&" +
                    "scope=$scope&" +
                    "access_type=offline&" +
                    "prompt=consent&" +
                    "code_challenge=$codeChallenge&" +
                    "code_challenge_method=S256"

            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI(authUrl))
            }

            val code = withTimeoutOrNull(180_000L) { authCodeDeferred.await() }
                ?: return Result.failure(IllegalStateException("Время ожидания входа истекло (3 минуты)."))

            // Exchange authorization code for tokens
            val tokenResponse = appHttpClient.submitForm(
                url = "https://oauth2.googleapis.com/token",
                formParameters = parameters {
                    append("code", code)
                    append("client_id", clientId)
                    if (clientSecret.isNotBlank()) append("client_secret", clientSecret)
                    append("redirect_uri", redirectUri)
                    append("grant_type", "authorization_code")
                    append("code_verifier", codeVerifier)
                }
            ).body<TokenResponse>()

            val accessToken = tokenResponse.access_token
                ?: return Result.failure(IllegalStateException("Не удалось получить access token: ${tokenResponse.error_description ?: tokenResponse.error}"))

            // Securely persist tokens
            SecureStorage.saveSecret(AppConfig.TOKEN_GDRIVE_ACCESS, accessToken)
            tokenResponse.refresh_token?.let {
                SecureStorage.saveSecret(AppConfig.TOKEN_GDRIVE_REFRESH, it)
            }

            return Result.success(accessToken)
        } catch (e: Exception) {
            return Result.failure(e)
        } finally {
            server.stop(gracePeriodMillis = 200, timeoutMillis = 1000)
        }
    }

    suspend fun refreshAccessToken(
        clientId: String = AppConfig.googleClientId,
        clientSecret: String = AppConfig.googleClientSecret,
    ): Result<String> {
        val refreshToken = SecureStorage.getSecret(AppConfig.TOKEN_GDRIVE_REFRESH)
            ?: return Result.failure(IllegalStateException("Refresh token отсутствует. Требуется повторный вход."))

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

            if (response.error == "invalid_grant") {
                return Result.failure(IllegalStateException(
                    "Токен Google Drive истек (ограничение 7 дней в режиме Testing). Авторизуйтесь снова или переведите статус проекта в 'In production' в Google Cloud Console."
                ))
            }

            val newAccessToken = response.access_token
                ?: return Result.failure(IllegalStateException("Ошибка обновления токена: ${response.error_description ?: response.error}"))

            SecureStorage.saveSecret(AppConfig.TOKEN_GDRIVE_ACCESS, newAccessToken)
            response.refresh_token?.let {
                SecureStorage.saveSecret(AppConfig.TOKEN_GDRIVE_REFRESH, it)
            }

            Result.success(newAccessToken)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
