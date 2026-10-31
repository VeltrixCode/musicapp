package com.example.musicapp.settings

import com.example.musicapp.platform.AppStorage
import com.example.musicapp.player.RepeatMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

const val CURRENT_SETTINGS_VERSION = 2

@Serializable
data class AppSettings(
    val settingsVersion: Int = CURRENT_SETTINGS_VERSION,
    val pendingMigrationNotice: String? = null,
    val volume: Float = 0.8f,
    val shuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.Off,
    val lastTrackId: String? = null,
    val lastPositionMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val equalizerPresetName: String = "Flat",
    val equalizerCustomGains: List<Float> = emptyList(),
    val fetchOnlineLyrics: Boolean = false,
    val fetchOnlineCovers: Boolean = false,
    val offlineMode: Boolean = false,
    val librarySortColumn: String = "Number",
    val librarySortDirection: String = "Ascending",
    val sidebarFilter: String = "Все",
    val sidebarSort: String = "Недавние",
    val sidebarIsGrid: Boolean = false,
    val isRightPanelPinned: Boolean = false,
    val isRightPanelOpen: Boolean = false,
    val gdriveAccountEmail: String? = null,
    val yandexAccountEmail: String? = null,
    val gdriveSyncFolderId: String? = null,
    val yandexSyncFolderId: String? = null,
    val lastSyncTimeMs: Long? = null,
    val preferredDeduplicationSource: String? = null,
    val radioCountryCode: String? = null,
)

class SettingsRepository {
    companion object {
        const val CURRENT_SETTINGS_VERSION = com.example.musicapp.settings.CURRENT_SETTINGS_VERSION
        const val SETTINGS_FILE_NAME = "settings.json"
    }

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val fileName = SETTINGS_FILE_NAME
    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun isMigrationNoticePending(): Boolean = _settings.value.pendingMigrationNotice != null

    fun load(): AppSettings {
        val raw = AppStorage.read(fileName) ?: return AppSettings()
        val parsed = runCatching { json.decodeFromString<AppSettings>(raw) }.getOrDefault(AppSettings())
        if (parsed.settingsVersion < CURRENT_SETTINGS_VERSION) {
            val migrated = parsed.copy(
                settingsVersion = CURRENT_SETTINGS_VERSION,
                fetchOnlineCovers = false,
                pendingMigrationNotice = "В целях приватности настройка «Загружать обложки из интернета» была отключена."
            )
            save(migrated)
            return migrated
        }
        return parsed
    }

    fun save(settings: AppSettings) {
        runCatching {
            val encoded = json.encodeToString(settings)
            AppStorage.write(fileName, encoded)
            _settings.value = settings
        }
    }

    fun clearMigrationNotice() {
        val current = _settings.value
        if (current.pendingMigrationNotice != null) {
            save(current.copy(pendingMigrationNotice = null))
        }
    }
}
