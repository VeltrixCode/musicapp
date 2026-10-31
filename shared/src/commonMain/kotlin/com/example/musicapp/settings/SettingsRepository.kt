package com.example.musicapp.settings

import com.example.musicapp.platform.AppStorage
import com.example.musicapp.player.RepeatMode
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
)

class SettingsRepository {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val fileName = "settings.json"

    fun load(): AppSettings {
        val raw = AppStorage.read(fileName) ?: return AppSettings()
        return runCatching { json.decodeFromString<AppSettings>(raw) }.getOrDefault(AppSettings())
    }

    fun save(settings: AppSettings) {
        runCatching {
            val encoded = json.encodeToString(settings)
            AppStorage.write(fileName, encoded)
        }
    }
}
