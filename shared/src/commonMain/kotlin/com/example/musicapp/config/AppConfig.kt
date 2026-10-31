package com.example.musicapp.config

import com.example.musicapp.platform.getEnv
import com.example.musicapp.security.SecureStorage

object AppConfig {
    // Keys for SecureStorage / Config
    const val KEY_JAMENDO_CLIENT_ID = "config_jamendo_client_id"
    const val KEY_AUDIUS_APP_NAME = "config_audius_app_name"
    const val KEY_GOOGLE_CLIENT_ID = "config_google_client_id"
    const val KEY_GOOGLE_CLIENT_SECRET = "config_google_client_secret"
    const val KEY_YANDEX_CLIENT_ID = "config_yandex_client_id"

    const val TOKEN_GDRIVE_ACCESS = "token_gdrive_access"
    const val TOKEN_GDRIVE_REFRESH = "token_gdrive_refresh"
    const val TOKEN_YANDEX_ACCESS = "token_yandex_access"

    var jamendoClientId: String
        get() = SecureStorage.getSecret(KEY_JAMENDO_CLIENT_ID)
            ?: getEnv("JAMENDO_CLIENT_ID")
            ?: ""
        set(value) = SecureStorage.saveSecret(KEY_JAMENDO_CLIENT_ID, value.trim())

    var audiusAppName: String
        get() = SecureStorage.getSecret(KEY_AUDIUS_APP_NAME)
            ?: getEnv("AUDIUS_APP_NAME")
            ?: "JVXMusic"
        set(value) = SecureStorage.saveSecret(KEY_AUDIUS_APP_NAME, value.trim())

    var googleClientId: String
        get() = SecureStorage.getSecret(KEY_GOOGLE_CLIENT_ID)
            ?: getEnv("GOOGLE_CLIENT_ID")
            ?: ""
        set(value) = SecureStorage.saveSecret(KEY_GOOGLE_CLIENT_ID, value.trim())

    var googleClientSecret: String
        get() = SecureStorage.getSecret(KEY_GOOGLE_CLIENT_SECRET)
            ?: getEnv("GOOGLE_CLIENT_SECRET")
            ?: ""
        set(value) = SecureStorage.saveSecret(KEY_GOOGLE_CLIENT_SECRET, value.trim())

    var yandexClientId: String
        get() = SecureStorage.getSecret(KEY_YANDEX_CLIENT_ID)
            ?: getEnv("YANDEX_CLIENT_ID")
            ?: ""
        set(value) = SecureStorage.saveSecret(KEY_YANDEX_CLIENT_ID, value.trim())
}

