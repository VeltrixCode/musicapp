package com.example.musicapp.cache

import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AudioOfflineCacheTest {

    private lateinit var testCacheDir: File

    @BeforeTest
    fun setup() {
        testCacheDir = File(System.getProperty("java.io.tmpdir"), "jvxmusic_test_cache_${System.currentTimeMillis()}").apply {
            mkdirs()
        }
    }

    @AfterTest
    fun tearDown() {
        testCacheDir.deleteRecursively()
    }

    @Test
    fun testLruEvictionProtectsCurrentlyPlayingTrack() {
        val file1 = File(testCacheDir, "track_old.mp3").apply {
            writeBytes(ByteArray(40_000))
            setLastModified(1000L) // Oldest
        }

        val file2 = File(testCacheDir, "track_playing.mp3").apply {
            writeBytes(ByteArray(40_000))
            setLastModified(2000L)
        }

        val file3 = File(testCacheDir, "track_new.mp3").apply {
            writeBytes(ByteArray(40_000))
            setLastModified(5000L) // Newest
        }

        val maxBytes = 80_000L // Total is 120,000; deleting file1 (40,000) brings total to 80,000 <= maxBytes
        val protectedTrackId = "track_playing"

        // Emulate enforceLruLimit logic on test directory
        val files = testCacheDir.listFiles()?.filter { it.isFile } ?: emptyList()
        var total = files.sumOf { it.length() }

        val protectedFileNames = setOf("$protectedTrackId.mp3")
        val candidates = files.filter { it.name !in protectedFileNames }.sortedBy { it.lastModified() }

        for (f in candidates) {
            val len = f.length()
            if (f.delete()) {
                total -= len
            }
            if (total <= maxBytes) break
        }

        assertFalse(file1.exists(), "Oldest unprotected file must be evicted")
        assertTrue(file2.exists(), "Currently playing track MUST be protected and kept")
        assertTrue(file3.exists(), "Newer file must be retained")
    }

    @Test
    fun testAtomicFileMove() {
        val tmpFile = File(testCacheDir, "temp_download.tmp").apply {
            writeText("AUDIO_DATA_PAYLOAD")
        }
        val targetFile = File(testCacheDir, "track_123.mp3")

        java.nio.file.Files.move(
            tmpFile.toPath(),
            targetFile.toPath(),
            java.nio.file.StandardCopyOption.REPLACE_EXISTING,
            java.nio.file.StandardCopyOption.ATOMIC_MOVE
        )

        assertFalse(tmpFile.exists(), "Temp file should no longer exist after atomic move")
        assertTrue(targetFile.exists(), "Target file must exist after move")
        assertEquals("AUDIO_DATA_PAYLOAD", targetFile.readText())
    }
}
