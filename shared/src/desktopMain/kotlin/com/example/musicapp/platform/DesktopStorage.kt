package com.example.musicapp.platform

import java.io.File

actual object AppStorage {
    private val dir: File by lazy {
        File(System.getProperty("user.home"), "JVXMusic").apply { mkdirs() }
    }

    actual fun read(name: String): String? =
        File(dir, name).takeIf { it.exists() }?.readText()

    actual fun write(name: String, text: String) {
        File(dir, name).writeText(text)
    }

    actual fun deleteFile(absolutePath: String) {
        File(absolutePath).delete()
    }

    actual fun getCacheDir(): String =
        File(dir, "cache/audio").apply { mkdirs() }.absolutePath
}
