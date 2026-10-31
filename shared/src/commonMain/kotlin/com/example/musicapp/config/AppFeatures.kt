package com.example.musicapp.config

/**
 * Feature flags controlling application modules and online capabilities.
 */
object AppFeatures {
    /**
     * Master feature flag: when false, all remote providers, online cover/lyrics lookups,
     * streaming proxy and network clients remain completely dormant.
     * Default is false (strictly local playback).
     */
    const val ONLINE_SOURCES: Boolean = false

    /**
     * Optional hidden developer / test toggle. Defaults to false.
     */
    @Volatile
    var devOnlineSourcesEnabled: Boolean = false

    /**
     * Effective state: true only if compile-time ONLINE_SOURCES is true or
     * developer override is explicitly turned on.
     */
    val isOnlineSourcesEnabled: Boolean
        get() = devOnlineSourcesEnabled || ONLINE_SOURCES
}
