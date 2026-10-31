package com.example.musicapp.provider

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

val appHttpClient: HttpClient by lazy {
    HttpClient {
        install(ContentNegotiation) {
            json(appJson)
        }
    }
}
