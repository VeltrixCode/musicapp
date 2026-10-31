package com.example.musicapp.platform

import org.junit.Assume.assumeTrue
import java.awt.GraphicsEnvironment
import java.io.File
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class JavaFxPlayerEngineTest {

    @Test
    fun testEngineInstantiationAndControls() {
        // Skip safely in headless CI / test environments without display
        assumeTrue("Skipping JavaFX MediaPlayer test in headless environment", !GraphicsEnvironment.isHeadless())

        val engine = createPlayerEngine()
        assertNotNull(engine, "PlayerEngine must instantiate")

        // Validate basic controls without hanging
        engine.setVolume(0.75f)
        engine.setPlaybackSpeed(1.25f)
        engine.setEqualizerBand(0, 2.0f)
        assertTrue(engine.positionMs() >= 0L)
        assertTrue(engine.durationMs() >= 0L)

        // Validate load and safe release
        val tempFile = File.createTempFile("test_audio", ".mp3")
        tempFile.writeBytes(ByteArray(1024))
        try {
            engine.load(tempFile.absolutePath, onReady = {}, onFinished = {})
            Thread.sleep(150)
            engine.release()
            Thread.sleep(100)
        } finally {
            tempFile.delete()
        }
    }
}
