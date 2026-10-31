package com.example.musicapp.oauth

actual object GoogleOAuth {
    actual suspend fun performSignIn(): Result<String> = GoogleOAuthAndroid.performSignIn()
    actual suspend fun refreshAccessToken(): Result<String> = GoogleOAuthAndroid.refreshAccessToken()
}
