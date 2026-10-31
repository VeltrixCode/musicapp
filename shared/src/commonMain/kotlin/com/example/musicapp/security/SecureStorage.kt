package com.example.musicapp.security

expect object SecureStorage {
    fun saveSecret(key: String, value: String)
    fun getSecret(key: String): String?
    fun removeSecret(key: String)
}
