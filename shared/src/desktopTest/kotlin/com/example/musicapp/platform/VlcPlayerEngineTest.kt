package com.example.musicapp.platform

import org.junit.Assume
import org.junit.Test
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class VlcPlayerEngineTest {

    @Test
    fun testVlcDiscoveryAndEngineInitialization() {
        val available = VlcBridge.isAvailable
        Assume.assumeTrue("VLC native libraries must be available for this test", available)

        val engine = createPlayerEngine()
        assertNotNull(engine)
        assertTrue(engine is VlcPlayerEngine, "Expected VlcPlayerEngine when bundled VLC is available")
        assertEquals("VLC", engine.engineName)
        assertEquals("Движок: VLC", engine.engineStatusText)
        assertEquals(false, engine.isFallback)

        // Test equalizer band count and gain range
        assertEquals(10, engine.getEqualizerBandCount())
        assertEquals(-20f..20f, engine.getEqualizerGainRange())
        assertEquals(10, engine.getEqualizerBandFrequencies().size)

        // Test volume, speed, and equalizer controls without throwing
        engine.setVolume(0.5f)
        engine.setPlaybackSpeed(1.25f)
        engine.setPlaybackSpeed(2.0f)
        engine.setPlaybackSpeed(0.5f)

        engine.setEqualizerBand(0, 3.5f)
        engine.setEqualizerBand(9, -2.0f)
        engine.setEqualizerEnabled(true)
        engine.setEqualizerEnabled(false)

        assertEquals(0L, engine.positionMs())
        assertEquals(0L, engine.durationMs())

        engine.release()
    }

    @Test
    fun testRapidTrackChangesWithoutDeadlock() {
        Assume.assumeTrue("VLC native libraries must be available for this test", VlcBridge.isAvailable)

        val engine = VlcPlayerEngine()
        val silentWav = createSilentWav(durationSeconds = 1)
        val tempFiles = (1..5).map { i ->
            File.createTempFile("vlc_stress_$i", ".wav").apply {
                writeBytes(silentWav)
            }
        }

        try {
            // 100 rapid load/play/seek/pause cycles to verify no deadlocks between event dispatcher and player controls
            repeat(100) { iteration ->
                val file = tempFiles[iteration % tempFiles.size]
                val latch = CountDownLatch(1)

                engine.stop()
                engine.load(
                    path = file.absolutePath,
                    onReady = { latch.countDown() },
                    onFinished = { latch.countDown() },
                    onError = { latch.countDown() }
                )
                engine.play()
                engine.seekTo((iteration * 50).toLong())
                engine.setVolume(0.7f)
                engine.setPlaybackSpeed(1.25f)

                // Quick latch wait or continue without blocking
                latch.await(10, TimeUnit.MILLISECONDS)
            }
        } finally {
            engine.release()
            tempFiles.forEach { it.delete() }
        }
    }

    private fun createSilentWav(durationSeconds: Int): ByteArray {
        val sampleRate = 44100
        val numChannels = 1
        val bitsPerSample = 16
        val subchunk2Size = durationSeconds * sampleRate * numChannels * (bitsPerSample / 8)
        val chunkSize = 36 + subchunk2Size
        val byteRate = sampleRate * numChannels * (bitsPerSample / 8)
        val blockAlign = numChannels * (bitsPerSample / 8)

        val buffer = java.nio.ByteBuffer.allocate(44 + subchunk2Size).order(java.nio.ByteOrder.LITTLE_ENDIAN)
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

    @Test
    fun testSafeEngineInstantiationWhenVlcAbsent() {
        // When bundled VLC is not present, JavaFxPlayerEngine is safely used without crashing
        val javaFxEngine = JavaFxPlayerEngine()
        assertNotNull(javaFxEngine)
        assertEquals(10, javaFxEngine.getEqualizerBandCount())
        assertEquals("JavaFX", javaFxEngine.engineName)
        assertTrue(javaFxEngine.engineStatusText.startsWith("Движок: JavaFX"))
        assertEquals(true, javaFxEngine.isFallback)
        javaFxEngine.release()
    }

    @Test
    fun testEngineLogCreated() {
        VlcBridge.initVlc()
        val userHome = System.getProperty("user.home")
        val logFile = File(userHome, "JVXMusic/logs/engine.log")
        assertTrue(logFile.exists(), "engine.log should be created in %USERPROFILE%/JVXMusic/logs")
        val content = logFile.readText()
        assertTrue(content.contains("JVXMusic Audio Engine Event"), "Log should contain engine event header")
    }
}
