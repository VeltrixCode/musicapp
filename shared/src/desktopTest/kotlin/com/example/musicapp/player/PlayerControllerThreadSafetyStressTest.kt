package com.example.musicapp.player

import com.example.musicapp.model.Track
import com.example.musicapp.model.TrackSource
import com.example.musicapp.platform.VlcBridge
import com.example.musicapp.platform.VlcPlayerEngine
import com.example.musicapp.settings.SettingsRepository
import org.junit.Assume
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.swing.SwingUtilities
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlayerControllerThreadSafetyStressTest {

    private lateinit var tempFiles: List<File>
    private lateinit var tracks: List<Track>
    private lateinit var controller: PlayerController
    private lateinit var settingsRepo: SettingsRepository

    @BeforeTest
    fun setUp() {
        Assume.assumeTrue("VLC native libraries must be available for this test", VlcBridge.isAvailable)

        val silentWav = createSilentWav(durationSeconds = 60)
        tempFiles = (1..5).map { i ->
            File.createTempFile("vlc_thread_stress_$i", ".wav").apply {
                writeBytes(silentWav)
            }
        }

        tracks = tempFiles.mapIndexed { index, file ->
            Track(
                id = "track_$index",
                title = "Stress Track $index",
                artist = "Artist",
                source = file.absolutePath,
                sourceProvider = TrackSource.LOCAL,
                durationMs = 60000L
            )
        }

        settingsRepo = SettingsRepository()
        controller = PlayerController(
            settingsRepository = settingsRepo,
            customEngine = VlcPlayerEngine()
        )
    }

    @AfterTest
    fun tearDown() {
        if (::controller.isInitialized) {
            controller.release()
        }
        if (::tempFiles.isInitialized) {
            tempFiles.forEach { runCatching { it.delete() } }
        }
        flushSwing()
    }

    private fun flushSwing() {
        val latch = CountDownLatch(1)
        SwingUtilities.invokeLater { latch.countDown() }
        latch.await(2, TimeUnit.SECONDS)
    }

    @Test
    fun testRapidTrackSwitchesWithVlcThreadSafety() {
        // Queue all tracks
        controller.play(tracks.first(), tracks)
        flushSwing()

        // Perform 10 rapid track switches consecutively without delay
        repeat(10) { index ->
            val nextTrack = tracks[(index + 1) % tracks.size]
            controller.play(nextTrack, tracks)
            // Tiny jitter between 0 and 15ms to induce race conditions if uncoordinated
            if (index % 2 == 0) {
                Thread.sleep(10)
            }
        }

        // Wait up to 3 seconds for the final track to settle and start playing
        val settled = CountDownLatch(1)
        val expectedTrack = tracks[(10) % tracks.size]

        // Poll condition on swing thread
        val deadline = System.currentTimeMillis() + 4000
        while (System.currentTimeMillis() < deadline) {
            flushSwing()
            if (controller.current?.id == expectedTrack.id && controller.isPlaying) {
                settled.countDown()
                break
            }
            Thread.sleep(50)
        }

        assertEquals(expectedTrack.id, controller.current?.id, "Final current track must match the last requested track")
        assertTrue(controller.isPlaying, "Player should be actively playing the target track")
        assertFalse(controller.isEngineFallback, "VLC engine must be active and not in fallback")
    }

    private fun createSilentWav(durationSeconds: Int): ByteArray {
        val sampleRate = 44100
        val numChannels = 1
        val bitsPerSample = 16
        val subchunk2Size = durationSeconds * sampleRate * numChannels * (bitsPerSample / 8)
        val chunkSize = 36 + subchunk2Size
        val byteRate = sampleRate * numChannels * (bitsPerSample / 8)
        val blockAlign = numChannels * (bitsPerSample / 8)

        val buffer = ByteBuffer.allocate(44 + subchunk2Size).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray())
        buffer.putInt(chunkSize)
        buffer.put("WAVE".toByteArray())
        buffer.put("fmt ".toByteArray())
        buffer.putInt(16) // Subchunk1Size
        buffer.putShort(1) // AudioFormat (PCM)
        buffer.putShort(numChannels.toShort())
        buffer.putInt(sampleRate)
        buffer.putInt(byteRate)
        buffer.putShort(blockAlign.toShort())
        buffer.putShort(bitsPerSample.toShort())
        buffer.put("data".toByteArray())
        buffer.putInt(subchunk2Size)
        buffer.put(ByteArray(subchunk2Size))
        return buffer.array()
    }
}
