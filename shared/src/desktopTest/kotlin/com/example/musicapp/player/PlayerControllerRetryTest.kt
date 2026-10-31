package com.example.musicapp.player

import com.example.musicapp.model.Track
import com.example.musicapp.model.TrackSource
import com.example.musicapp.platform.PlayerEngine
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PlayerControllerRetryTest {

    private fun testTrack(id: String) = Track(
        id = id,
        title = "Track $id",
        artist = "Artist",
        source = "test://$id.mp3",
        sourceProvider = TrackSource.LOCAL,
        durationMs = 60_000L
    )

    private class MockPlayerEngine(
        private val onEngineLoad: (path: String, onReady: (Long) -> Unit, onError: ((String) -> Unit)?) -> Unit
    ) : PlayerEngine {
        val loadCount = AtomicInteger(0)

        override fun load(path: String, onReady: (Long) -> Unit, onFinished: () -> Unit) {
            load(path, onReady, onFinished, null)
        }

        override fun load(path: String, onReady: (Long) -> Unit, onFinished: () -> Unit, onError: ((String) -> Unit)?) {
            loadCount.incrementAndGet()
            onEngineLoad(path, onReady, onError)
        }

        override fun play() {}
        override fun pause() {}
        override fun seekTo(ms: Long) {}
        override fun setVolume(volume: Float) {}
        override fun setPlaybackSpeed(speed: Float) {}
        override fun setEqualizerBand(bandIndex: Int, gainDb: Float) {}
        override fun setSpectrumListener(listener: ((FloatArray) -> Unit)?) {}
        override fun positionMs(): Long = 0L
        override fun durationMs(): Long = 60_000L
        override fun release() {}
    }

    private fun flushEdt() {
        val latch = CountDownLatch(1)
        SwingUtilities.invokeLater { latch.countDown() }
        latch.await(2, TimeUnit.SECONDS)
    }

    private fun waitUntil(timeoutMs: Long = 5000L, condition: () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            flushEdt()
            if (condition()) return true
            val waitLatch = CountDownLatch(1)
            waitLatch.await(50, TimeUnit.MILLISECONDS)
        }
        flushEdt()
        return condition()
    }

    @Test
    fun testCircuitBreakerStopsAfterExactlyThreeConsecutiveErrors() {
        val mockEngine = MockPlayerEngine { _, _, onError ->
            // Engine always reports error
            onError?.invoke("Simulated playback failure")
        }

        val controller = PlayerController(customEngine = mockEngine)
        val tracks = (1..5).map { testTrack("t$it") }

        try {
            // Queue 5 tracks and play first
            controller.play(tracks[0], tracks)

            // Wait until circuit breaker trips: isPlaying goes false and appError is populated
            val stopped = waitUntil {
                !controller.isPlaying && controller.appError.value != null
            }

            assertTrue(stopped, "Playback should stop and publish AppError when circuit breaker trips")
            // With queue of 5 tracks and max 3 attempts:
            // Attempt 1 (t1) -> fails -> advance
            // Attempt 2 (t2) -> fails -> advance
            // Attempt 3 (t3) -> fails -> circuit breaker trips, stop!
            assertEquals(3, mockEngine.loadCount.get(), "Engine should be loaded exactly 3 times before halting")
            assertFalse(controller.isPlaying, "Player should not be playing after 3 errors")
            assertNotNull(controller.appError.value, "AppError must be set after failure")
            assertTrue(controller.appError.value?.message?.contains("Ошибка воспроизведения") == true)
        } finally {
            controller.release()
        }
    }

    @Test
    fun testConsecutiveErrorCounterResetsAfterSuccessfulTrack() {
        var failMode = false

        val mockEngine = MockPlayerEngine { _, onReady, onError ->
            if (failMode) {
                onError?.invoke("Simulated failure after reset")
            } else {
                onReady(60_000L) // Success
            }
        }

        val controller = PlayerController(customEngine = mockEngine)
        val tracks = (1..5).map { testTrack("success_then_fail_$it") }

        try {
            // Step 1: Play track 1 successfully
            failMode = false
            controller.play(tracks[0], tracks)

            val started = waitUntil { controller.isPlaying && controller.durationMs == 60_000L }
            assertTrue(started, "Track 1 should start playing successfully")
            assertEquals(1, mockEngine.loadCount.get())

            // Step 2: Switch engine to fail mode and advance to next track
            failMode = true
            controller.next()

            // After resetting upon success of track 1, the failure circuit breaker has a fresh budget of 3 errors:
            // Attempt 2 (track 2) -> fails
            // Attempt 3 (track 3) -> fails
            // Attempt 4 (track 4) -> fails -> trips and stops
            val stoppedAgain = waitUntil {
                !controller.isPlaying && controller.appError.value != null
            }

            assertTrue(stoppedAgain, "Playback should stop after fresh 3 errors following previous success")
            // 1 successful load + 3 failing loads = 4 total loads
            assertEquals(4, mockEngine.loadCount.get(), "Total loads must be 1 (success) + 3 (retries after reset) = 4")
            assertFalse(controller.isPlaying)
        } finally {
            controller.release()
        }
    }
}
