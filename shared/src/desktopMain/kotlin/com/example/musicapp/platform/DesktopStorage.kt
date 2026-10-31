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

    actual fun writeAtomic(name: String, text: String) {
        val target = File(dir, name)
        val temp = File(dir, "$name.tmp")
        temp.writeText(text)
        try {
            java.nio.file.Files.move(
                temp.toPath(),
                target.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                java.nio.file.StandardCopyOption.ATOMIC_MOVE
            )
        } catch (_: Exception) {
            if (!temp.renameTo(target)) {
                temp.copyTo(target, overwrite = true)
                temp.delete()
            }
        }
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

    actual fun deleteFilesWithPrefix(prefix: String) {
        dir.listFiles()?.filter { it.name.startsWith(prefix) }?.forEach { it.delete() }
    }

    actual fun getCacheDir(): String =
        File(dir, "cache/audio").apply { mkdirs() }.absolutePath
}
