package com.example.musicapp.oauth

open class GoogleOAuthException(message: String, cause: Throwable? = null) : Exception(message, cause)
class GoogleOAuthInvalidGrantException(message: String = "Нужен повторный вход в Google Диск") : GoogleOAuthException(message)
class GoogleOAuthNetworkException(message: String = "Нет подключения к сети", cause: Throwable? = null) : GoogleOAuthException(message, cause)

expect object GoogleOAuth {
    suspend fun performSignIn(): Result<String>
    suspend fun refreshAccessToken(): Result<String>
}
