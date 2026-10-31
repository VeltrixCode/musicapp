package com.example.musicapp.platform

import java.io.File

actual object AppStorage {
    private val dir: File get() = AndroidBridge.appContext.filesDir

    actual fun read(name: String): String? =
        File(dir, name).takeIf { it.exists() }?.readText()

    actual fun write(name: String, text: String) {
        File(dir, name).writeText(text)
    }

    actual fun readBytes(name: String): ByteArray? =
        File(dir, name).takeIf { it.exists() }?.readBytes()

    actual fun writeBytes(name: String, bytes: ByteArray) {
        val f = File(dir, name)
        f.parentFile?.mkdirs()
        f.writeBytes(bytes)
    }

    actual fun deleteFile(absolutePath: String) {
        File(absolutePath).delete()
    }

    actual fun getCacheDir(): String =
        File(AndroidBridge.appContext.cacheDir, "audio").apply { mkdirs() }.absolutePath
}
