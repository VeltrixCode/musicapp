package com.example.musicapp.config

/**
 * Feature flags controlling application modules and online capabilities.
 */
object AppFeatures {
    /**
     * Master feature flag: controls whether online sources (Google Drive, Yandex Disk, etc.)
     * and network streaming capabilities are enabled.
     * Enabled by default for online cloud storage support.
     */
    var ONLINE_SOURCES: Boolean = true

    /**
     * Optional test / developer override. When non-null, overrides ONLINE_SOURCES.
     */
    @Volatile
    var devOnlineSourcesEnabled: Boolean? = null

    /**
     * Effective state: true if ONLINE_SOURCES is enabled, unless explicitly overridden
     * by devOnlineSourcesEnabled.
     */
    val isOnlineSourcesEnabled: Boolean
        get() = devOnlineSourcesEnabled ?: ONLINE_SOURCES
}
