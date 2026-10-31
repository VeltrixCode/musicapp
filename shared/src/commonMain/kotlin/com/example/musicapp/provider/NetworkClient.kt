package com.example.musicapp.provider

import com.example.musicapp.config.AppFeatures
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

val appJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
    prettyPrint = false
}

private val appHttpClientLock = Any()
private var _appHttpClient: HttpClient? = null

/**
 * Returns true if the shared [appHttpClient] has been instantiated.
 */
val isAppHttpClientCreated: Boolean
    get() = synchronized(appHttpClientLock) { _appHttpClient != null }

/**
 * Shared HTTP client for online services.
 * Must NOT be accessed when [AppFeatures.isOnlineSourcesEnabled] is false.
 */
val appHttpClient: HttpClient
    get() = synchronized(appHttpClientLock) {
        if (!AppFeatures.isOnlineSourcesEnabled) {
            error("Cannot instantiate shared appHttpClient when AppFeatures.ONLINE_SOURCES is disabled.")
        }
        _appHttpClient ?: HttpClient {
            install(ContentNegotiation) {
                json(appJson)
            }
        }.also { _appHttpClient = it }
    }

/**
 * Closes the shared HTTP client when the application process terminates.
 * Safe to call even if the client was never instantiated (zero network/allocation side-effects).
 */
fun closeAppHttpClient() {
    synchronized(appHttpClientLock) {
        if (_appHttpClient != null) {
            runCatching { _appHttpClient?.close() }
            _appHttpClient = null
        }
    }
}
