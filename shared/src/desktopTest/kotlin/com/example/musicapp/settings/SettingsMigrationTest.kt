package com.example.musicapp.settings

import com.example.musicapp.platform.AppStorage
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsMigrationTest {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    @BeforeTest
    fun setup() {
        val repo = SettingsRepository()
        repo.clearMigrationNotice()
    }

    @Test
    fun testMigrationFromV1ResetsOnlineCoversToFalseAndNotifies() {
        val legacySettings = AppSettings(
            settingsVersion = 1,
            fetchOnlineCovers = true,
            fetchOnlineLyrics = true,
            offlineMode = false
        )
        // Store legacy settings directly to disk
        AppStorage.write(SettingsRepository.SETTINGS_FILE_NAME, json.encodeToString(legacySettings))

        val repo = SettingsRepository()
        val loaded = repo.load()

        assertEquals(SettingsRepository.CURRENT_SETTINGS_VERSION, loaded.settingsVersion, "Version must be bumped to v2")
        assertFalse(loaded.fetchOnlineCovers, "fetchOnlineCovers must be reset to false for privacy by default")
        assertTrue(loaded.fetchOnlineLyrics, "Other settings must be preserved")
        assertTrue(repo.isMigrationNoticePending(), "User notice should be pending after migration")

        // Clear notice
        repo.clearMigrationNotice()
        assertFalse(repo.isMigrationNoticePending(), "Notice should be cleared")
    }

    @Test
    fun testV2SettingsAreNotMutated() {
        val v2Settings = AppSettings(
            settingsVersion = SettingsRepository.CURRENT_SETTINGS_VERSION,
            fetchOnlineCovers = true,
            fetchOnlineLyrics = false,
            offlineMode = false
        )
        AppStorage.write(SettingsRepository.SETTINGS_FILE_NAME, json.encodeToString(v2Settings))

        val repo = SettingsRepository()
        val loaded = repo.load()

        assertEquals(SettingsRepository.CURRENT_SETTINGS_VERSION, loaded.settingsVersion)
        assertTrue(loaded.fetchOnlineCovers, "v2 settings configured by user must remain true")
        assertFalse(repo.isMigrationNoticePending(), "No migration notice for v2 profiles")
    }
}
