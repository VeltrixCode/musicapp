package com.example.musicapp.config

import com.example.musicapp.platform.getEnv
import com.example.musicapp.security.SecureStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppConfig {
    // Keys for SecureStorage / Config
    const val KEY_JAMENDO_CLIENT_ID = "config_jamendo_client_id"
    const val KEY_AUDIUS_APP_NAME = "config_audius_app_name"
    const val KEY_GOOGLE_CLIENT_ID = "config_google_client_id"
    const val KEY_GOOGLE_CLIENT_SECRET = "config_google_client_secret"
    const val KEY_YANDEX_CLIENT_ID = "config_yandex_client_id"
    const val KEY_SUBSONIC_SERVER = "config_subsonic_server"
    const val KEY_SUBSONIC_USER = "config_subsonic_user"
    const val KEY_SUBSONIC_PASSWORD = "config_subsonic_password"
    const val KEY_WEBDAV_SERVER = "config_webdav_server"
    const val KEY_WEBDAV_USER = "config_webdav_user"
    const val KEY_WEBDAV_PASSWORD = "config_webdav_password"
    const val KEY_WEBDAV_ROOT_PATH = "config_webdav_root_path"
    const val KEY_ACOUSTID_KEY = "config_acoustid_key"

    const val TOKEN_GDRIVE_ACCESS = "token_gdrive_access"
    const val TOKEN_GDRIVE_REFRESH = "token_gdrive_refresh"
    const val TOKEN_YANDEX_ACCESS = "token_yandex_access"

    private fun readJamendoClientId(): String {
        if (!AppFeatures.isOnlineSourcesEnabled) return ""
        return SecureStorage.getSecret(KEY_JAMENDO_CLIENT_ID)
            ?: getEnv("JAMENDO_CLIENT_ID")
            ?: ""
    }

    private fun readAudiusAppName(): String {
        if (!AppFeatures.isOnlineSourcesEnabled) return "JVXMusic"
        return SecureStorage.getSecret(KEY_AUDIUS_APP_NAME)
            ?: getEnv("AUDIUS_APP_NAME")
            ?: "JVXMusic"
    }

    private val _jamendoClientIdFlow = MutableStateFlow(readJamendoClientId())
    val jamendoClientIdFlow: StateFlow<String> = _jamendoClientIdFlow.asStateFlow()

    private val _audiusAppNameFlow = MutableStateFlow(readAudiusAppName())
    val audiusAppNameFlow: StateFlow<String> = _audiusAppNameFlow.asStateFlow()

    var jamendoClientId: String
        get() = _jamendoClientIdFlow.value
        set(value) {
            val trimmed = value.trim()
            SecureStorage.saveSecret(KEY_JAMENDO_CLIENT_ID, trimmed)
            _jamendoClientIdFlow.value = trimmed
        }

    var audiusAppName: String
        get() = _audiusAppNameFlow.value
        set(value) {
            val trimmed = value.trim().ifBlank { "JVXMusic" }
            SecureStorage.saveSecret(KEY_AUDIUS_APP_NAME, trimmed)
            _audiusAppNameFlow.value = trimmed
        }

    const val DEFAULT_GOOGLE_CLIENT_ID = "1071006060591-tmhssin2h21lcre235vtolojh4g403ep.apps.googleusercontent.com"

    var googleClientId: String
        get() = if (!AppFeatures.isOnlineSourcesEnabled) "" else SecureStorage.getSecret(KEY_GOOGLE_CLIENT_ID)
            ?: getEnv("GOOGLE_CLIENT_ID")
            ?: DEFAULT_GOOGLE_CLIENT_ID
        set(value) = SecureStorage.saveSecret(KEY_GOOGLE_CLIENT_ID, value.trim())

    var googleClientSecret: String
        get() = if (!AppFeatures.isOnlineSourcesEnabled) "" else SecureStorage.getSecret(KEY_GOOGLE_CLIENT_SECRET)
            ?: getEnv("GOOGLE_CLIENT_SECRET")
            ?: ""
        set(value) = SecureStorage.saveSecret(KEY_GOOGLE_CLIENT_SECRET, value.trim())

    var yandexClientId: String
        get() = if (!AppFeatures.isOnlineSourcesEnabled) "" else SecureStorage.getSecret(KEY_YANDEX_CLIENT_ID)
            ?: getEnv("YANDEX_CLIENT_ID")
            ?: ""
        set(value) = SecureStorage.saveSecret(KEY_YANDEX_CLIENT_ID, value.trim())

    var subsonicServer: String
        get() = if (!AppFeatures.isOnlineSourcesEnabled) "" else SecureStorage.getSecret(KEY_SUBSONIC_SERVER) ?: ""
        set(value) = SecureStorage.saveSecret(KEY_SUBSONIC_SERVER, value.trim())

    var subsonicUser: String
        get() = if (!AppFeatures.isOnlineSourcesEnabled) "" else SecureStorage.getSecret(KEY_SUBSONIC_USER) ?: ""
        set(value) = SecureStorage.saveSecret(KEY_SUBSONIC_USER, value.trim())

    var subsonicPassword: String
        get() = if (!AppFeatures.isOnlineSourcesEnabled) "" else SecureStorage.getSecret(KEY_SUBSONIC_PASSWORD) ?: ""
        set(value) = SecureStorage.saveSecret(KEY_SUBSONIC_PASSWORD, value)

    var webdavServer: String
        get() = if (!AppFeatures.isOnlineSourcesEnabled) "" else SecureStorage.getSecret(KEY_WEBDAV_SERVER) ?: ""
        set(value) = SecureStorage.saveSecret(KEY_WEBDAV_SERVER, value.trim())

    var webdavUser: String
        get() = if (!AppFeatures.isOnlineSourcesEnabled) "" else SecureStorage.getSecret(KEY_WEBDAV_USER) ?: ""
        set(value) = SecureStorage.saveSecret(KEY_WEBDAV_USER, value.trim())

    var webdavPassword: String
        get() = if (!AppFeatures.isOnlineSourcesEnabled) "" else SecureStorage.getSecret(KEY_WEBDAV_PASSWORD) ?: ""
        set(value) = SecureStorage.saveSecret(KEY_WEBDAV_PASSWORD, value)

    var webdavRootPath: String
        get() = if (!AppFeatures.isOnlineSourcesEnabled) "" else SecureStorage.getSecret(KEY_WEBDAV_ROOT_PATH) ?: ""
        set(value) = SecureStorage.saveSecret(KEY_WEBDAV_ROOT_PATH, value.trim())

    var acoustIdApiKey: String
        get() = if (!AppFeatures.isOnlineSourcesEnabled) "" else SecureStorage.getSecret(KEY_ACOUSTID_KEY) ?: ""
        set(value) = SecureStorage.saveSecret(KEY_ACOUSTID_KEY, value.trim())
}
