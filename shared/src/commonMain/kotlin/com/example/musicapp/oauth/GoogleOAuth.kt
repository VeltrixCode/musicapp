package com.example.musicapp.oauth

expect object GoogleOAuth {
    suspend fun performSignIn(): Result<String>
    suspend fun refreshAccessToken(): Result<String>
}
