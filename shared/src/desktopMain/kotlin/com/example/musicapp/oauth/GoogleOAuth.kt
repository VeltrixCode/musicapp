package com.example.musicapp.oauth

actual object GoogleOAuth {
    actual suspend fun performSignIn(): Result<String> = GoogleOAuthDesktop.performSignIn()
    actual suspend fun refreshAccessToken(): Result<String> = GoogleOAuthDesktop.refreshAccessToken()
}
