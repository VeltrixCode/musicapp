package com.example.musicapp.platform

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ZipAudioExtractorTest {

    private lateinit var tempWorkingDir: File
    private lateinit var tempUploadsDir: File

    @BeforeTest
    fun setUp() {
        tempWorkingDir = Files.createTempDirectory("zip_test_work").toFile()
        tempUploadsDir = File(tempWorkingDir, "uploads").apply { mkdirs() }
        AppStorage.setCustomDataDir(tempWorkingDir)
    }

    @AfterTest
    fun tearDown() {
        AppStorage.setCustomDataDir(null)
        tempWorkingDir.deleteRecursively()
    }

    @Test
    fun testExtractTracksFromZip() {
        val mp3Data = createValidMp3Bytes()
        val wavData = createValidWavBytes()

        val zipFile = File(tempWorkingDir, "album.zip")
        createZip(
            zipFile,
            mapOf(
                "song1.mp3" to mp3Data,
                "subfolder/song2.wav" to wavData,
                "notes.txt" to "Just some text notes".toByteArray()
            )
        )

        val tracks = ZipAudioExtractor.extractTracksFromZip(zipFile, tempUploadsDir)

        assertEquals(2, tracks.size, "Should extract exactly 2 audio tracks (ignoring notes.txt)")
        assertTrue(tracks.any { it.title.contains("song1") })
        assertTrue(tracks.any { it.title.contains("song2") })

        for (track in tracks) {
            val file = File(track.source!!)
            assertTrue(file.exists(), "Extracted file must exist: ${file.absolutePath}")
            assertTrue(file.length() > 0, "Extracted file must not be empty")
        }
    }

    @Test
    fun testExtractWithAccompanyingLyrics() {
        val mp3Data = createValidMp3Bytes()
        val lyricsText = "[00:01.00]Line 1\n[00:05.00]Line 2"

        val zipFile = File(tempWorkingDir, "lyrics_bundle.zip")
        createZip(
            zipFile,
            mapOf(
                "artists/rock/track.mp3" to mp3Data,
                "artists/rock/track.lrc" to lyricsText.toByteArray(Charsets.UTF_8)
            )
        )

        val tracks = ZipAudioExtractor.extractTracksFromZip(zipFile, tempUploadsDir)
        assertEquals(1, tracks.size)

        val track = tracks.first()
        val trackFile = File(track.source!!)
        assertTrue(trackFile.exists())

        // Read LRC content using platform helper
        val readLyrics = readLrcContent(trackFile.absolutePath)
        assertNotNull(readLyrics, "LRC lyrics must be found alongside extracted audio file")
        assertEquals(lyricsText, readLyrics)
    }

    @Test
    fun testZipSlipProtection() {
        val mp3Data = createValidMp3Bytes()
        val zipFile = File(tempWorkingDir, "malicious.zip")

        // Zip entry trying to escape outside the uploads directory
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            val evilEntry = ZipEntry("../../evil_escape.mp3")
            zos.putNextEntry(evilEntry)
            zos.write(mp3Data)
            zos.closeEntry()

            val normalEntry = ZipEntry("safe_track.mp3")
            zos.putNextEntry(normalEntry)
            zos.write(mp3Data)
            zos.closeEntry()
        }

        val tracks = ZipAudioExtractor.extractTracksFromZip(zipFile, tempUploadsDir)

        // Only the safe track should be extracted
        assertEquals(1, tracks.size, "Malicious entry must be skipped")
        assertTrue(tracks.first().title.contains("safe_track"))

        // Ensure no file was created outside destination directory
        val escapedFile = File(tempWorkingDir.parentFile, "evil_escape.mp3")
        assertFalse(escapedFile.exists(), "Zip-slip file must not exist outside destination")
    }

    @Test
    fun testNestedZipExtraction() {
        val mp3Data = createValidMp3Bytes()
        val wavData = createValidWavBytes()

        // 1. Create inner zip
        val innerZipFile = File(tempWorkingDir, "inner.zip")
        createZip(innerZipFile, mapOf("nested_track.wav" to wavData))

        // 2. Create outer zip containing an MP3 and the inner zip
        val outerZipFile = File(tempWorkingDir, "outer.zip")
        createZip(
            outerZipFile,
            mapOf(
                "outer_track.mp3" to mp3Data,
                "nested_archive.zip" to innerZipFile.readBytes()
            )
        )

        val tracks = ZipAudioExtractor.extractTracksFromZip(outerZipFile, tempUploadsDir)
        assertEquals(2, tracks.size, "Both outer and nested tracks must be extracted")
        assertTrue(tracks.any { it.title.contains("outer_track") })
        assertTrue(tracks.any { it.title.contains("nested_track") })
    }

    @Test
    fun testImportDirectoryWithAudioAndZips() {
        val mp3Data = createValidMp3Bytes()
        val wavData = createValidWavBytes()
        val lyricsText = "[00:10.00]Direct lyrics"

        val musicDir = File(tempWorkingDir, "MusicCollection").apply { mkdirs() }
        val subDir = File(musicDir, "Rock").apply { mkdirs() }
        val archiveDir = File(musicDir, "Archives").apply { mkdirs() }

        // Direct audio file in root
        File(musicDir, "direct_song.mp3").writeBytes(mp3Data)

        // Nested audio file + .lrc in Rock subfolder
        val rockSong = File(subDir, "rock_song.wav")
        rockSong.writeBytes(wavData)
        File(subDir, "rock_song.lrc").writeText(lyricsText)

        // Zip archive in Archives subfolder
        val zipFile = File(archiveDir, "bundle.zip")
        createZip(zipFile, mapOf("bundled_song.mp3" to mp3Data))

        val tracks = ZipAudioExtractor.importDirectory(musicDir, tempUploadsDir)

        assertEquals(3, tracks.size, "Should import direct file, subfolder file, and archived file")
        assertTrue(tracks.any { it.title.contains("direct_song") })
        assertTrue(tracks.any { it.title.contains("rock_song") })
        assertTrue(tracks.any { it.title.contains("bundled_song") })

        val rockTrack = tracks.first { it.title.contains("rock_song") }
        val lyrics = readLrcContent(rockTrack.source!!)
        assertEquals(lyricsText, lyrics, "LRC lyrics for directory import must be copied")
    }

    @Test
    fun testImportFileOrArchiveSingleFile() {
        val mp3Data = createValidMp3Bytes()
        val singleFile = File(tempWorkingDir, "single.mp3")
        singleFile.writeBytes(mp3Data)
        val singleLrc = File(tempWorkingDir, "single.lrc")
        singleLrc.writeText("[00:00.00]Single LRC")

        val tracks = ZipAudioExtractor.importFileOrArchive(singleFile, tempUploadsDir)
        assertEquals(1, tracks.size)
        assertTrue(tracks.first().title.contains("single"))

        val lyrics = readLrcContent(tracks.first().source!!)
        assertEquals("[00:00.00]Single LRC", lyrics)
    }

    private fun createValidMp3Bytes(): ByteArray {
        val frame = ByteArray(417)
        frame[0] = 0xFF.toByte()
        frame[1] = 0xFB.toByte()
        frame[2] = 0x90.toByte()
        frame[3] = 0x64.toByte()
        val baos = ByteArrayOutputStream()
        repeat(10) { baos.write(frame) }
        return baos.toByteArray()
    }

    private fun createValidWavBytes(): ByteArray {
        val totalDataLen = 100
        val totalAudioLen = totalDataLen + 36
        val sampleRate = 44100
        val channels = 1
        val byteRate = sampleRate * channels * 2
        val header = ByteArray(44)
        header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte(); header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
        header[4] = (totalAudioLen and 0xff).toByte()
        header[5] = ((totalAudioLen shr 8) and 0xff).toByte()
        header[6] = ((totalAudioLen shr 16) and 0xff).toByte()
        header[7] = ((totalAudioLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte(); header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte(); header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0
        header[20] = 1; header[21] = 0
        header[22] = channels.toByte(); header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (channels * 2).toByte(); header[33] = 0
        header[34] = 16; header[35] = 0
        header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte(); header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
        header[40] = (totalDataLen and 0xff).toByte()
        header[41] = ((totalDataLen shr 8) and 0xff).toByte()
        header[42] = ((totalDataLen shr 16) and 0xff).toByte()
        header[43] = ((totalDataLen shr 24) and 0xff).toByte()
        return header + ByteArray(totalDataLen)
    }

    private fun createZip(zipFile: File, entries: Map<String, ByteArray>) {
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            for ((name, data) in entries) {
                val entry = ZipEntry(name)
                zos.putNextEntry(entry)
                zos.write(data)
                zos.closeEntry()
            }
        }
    }
}
