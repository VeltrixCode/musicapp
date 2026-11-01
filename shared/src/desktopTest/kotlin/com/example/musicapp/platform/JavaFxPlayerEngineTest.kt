package com.example.musicapp.platform

import javafx.application.Platform
import org.junit.Assume.assumeTrue
import java.awt.GraphicsEnvironment
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class JavaFxPlayerEngineTest {

    @org.junit.Before
    fun setUp() {
        try {
            Platform.startup {}
        } catch (_: Throwable) {}
    }

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
            val loadLatch = CountDownLatch(1)
            engine.load(
                path = tempFile.absolutePath,
                onReady = { loadLatch.countDown() },
                onFinished = { loadLatch.countDown() },
                onError = { loadLatch.countDown() }
            )
            loadLatch.await(3, TimeUnit.SECONDS)

            engine.release()
            val releaseLatch = CountDownLatch(1)
            Platform.runLater { releaseLatch.countDown() }
            releaseLatch.await(2, TimeUnit.SECONDS)
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun testStressLoadReleaseCyclesWithoutThreadLeaks() {
        assumeTrue("Skipping JavaFX MediaPlayer stress test in headless environment", !GraphicsEnvironment.isHeadless())

        val engine = createPlayerEngine()
        val spectrumUpdateCount = AtomicInteger(0)

        // Attach spectrum listener
        engine.setSpectrumListener { magnitudes ->
            if (magnitudes.isNotEmpty()) {
                spectrumUpdateCount.incrementAndGet()
            }
        }

        val tempFile = File.createTempFile("stress_test", ".mp3")
        tempFile.writeBytes(ByteArray(2048))

        // Baseline thread count before stress iterations
        val baselineThreadCount = Thread.activeCount()

        try {
            repeat(100) { index ->
                val cycleLatch = CountDownLatch(1)
                engine.load(
                    path = tempFile.absolutePath,
                    onReady = { cycleLatch.countDown() },
                    onFinished = { cycleLatch.countDown() },
                    onError = { cycleLatch.countDown() }
                )

                // Rapid release to stress test concurrency and cleanup
                engine.release()

                // Flush JavaFX Application Thread queue for this cycle
                Platform.runLater {
                    cycleLatch.countDown()
                }
                cycleLatch.await(1, TimeUnit.SECONDS)
            }
        } finally {
            engine.setSpectrumListener(null)
            engine.release()
            tempFile.delete()
        }

        // Allow any asynchronous finalizer or GC cleanup
        System.gc()
        val finalDrainLatch = CountDownLatch(1)
        Platform.runLater { finalDrainLatch.countDown() }
        finalDrainLatch.await(2, TimeUnit.SECONDS)

        val finalThreadCount = Thread.activeCount()
        // Ensure no unbounded thread growth (allow small standard pool fluctuation <= 15 threads)
        val threadDelta = finalThreadCount - baselineThreadCount
        assertTrue(
            threadDelta <= 15,
            "Detected native/JVM thread leak after 100 load/release cycles: baseline=$baselineThreadCount, final=$finalThreadCount, delta=$threadDelta"
        )
    }
}
