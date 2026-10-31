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

@Serializable
data class AppSettings(
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
)

class SettingsRepository {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val fileName = "settings.json"
    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun load(): AppSettings {
        val raw = AppStorage.read(fileName) ?: return AppSettings()
        return runCatching { json.decodeFromString<AppSettings>(raw) }.getOrDefault(AppSettings())
    }

    fun save(settings: AppSettings) {
        runCatching {
            val encoded = json.encodeToString(settings)
            AppStorage.write(fileName, encoded)
            _settings.value = settings
        }
    }
}
