package com.example.musicapp.player

import com.example.musicapp.library.Library
import com.example.musicapp.model.AppError
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MissingFilesAndErrorHandlingTest {

    private fun testTrack(id: String, isMissing: Boolean = false, source: String = "test://$id.mp3") = Track(
        id = id,
        title = "Track $id",
        artist = "Artist $id",
        album = "Album $id",
        source = source,
        sourceProvider = TrackSource.LOCAL,
        durationMs = 60_000L,
        isMissing = isMissing,
        addedAt = 1000L
    )

    private class MockPlayerEngine(
        val onEngineLoad: (path: String, onReady: (Long) -> Unit, onError: ((String) -> Unit)?) -> Unit = { _, onReady, _ -> onReady(60_000L) }
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
        override fun setEqualizerEnabled(enabled: Boolean) {}
        override fun getEqualizerBandCount(): Int = 10
        override fun getEqualizerGainRange(): ClosedFloatingPointRange<Float> = -24f..12f
        override fun getEqualizerBandFrequencies(): List<Int> = listOf(32, 64, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)
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

    private fun waitUntil(timeoutMs: Long = 4000L, condition: () -> Boolean): Boolean {
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
    fun testMissingTracksExcludedFromMixesAndPlayedStats() {
        val library = Library()
        library.tracks.clear()

        val validTrack = testTrack("v1", isMissing = false)
        val missingTrack = testTrack("m1", isMissing = true)

        library.tracks.addAll(listOf(validTrack, missingTrack))

        // Mixes, recently added, and frequently played must not include missing tracks
        val mix = library.getMixOfTheDay()
        assertTrue(mix.none { it.isMissing }, "Mix of the day must exclude missing tracks")
        assertTrue(mix.any { it.id == "v1" })

        val recent = library.getRecentlyAdded()
        assertTrue(recent.none { it.isMissing }, "Recently added must exclude missing tracks")

        val frequent = library.getFrequentlyPlayed()
        assertTrue(frequent.none { it.isMissing }, "Frequently played must exclude missing tracks")

        // recordTrackPlayed must ignore missing track
        library.recordTrackPlayed(missingTrack)
        assertFalse(library.recentlyPlayedIds.contains(missingTrack.id), "Missing track should not be recorded in play history")
    }

    @Test
    fun testPlayingMissingTrackRejectedAndErrorPosted() {
        val mockEngine = MockPlayerEngine()
        val controller = PlayerController(customEngine = mockEngine)
        try {
            val missingTrack = testTrack("m1", isMissing = true, source = "C:/Music/song.mp3")
            controller.play(missingTrack)

            // Must not call engine.load or set isPlaying
            assertEquals(0, mockEngine.loadCount.get(), "Engine should not load missing track")
            assertFalse(controller.isPlaying, "Player should not be playing missing track")

            val err = controller.appError.value
            assertNotNull(err, "AppError must be set when trying to play missing track")
            assertTrue(err.message.contains("файл не найден"), "Error message must state 'файл не найден'")
        } finally {
            controller.release()
        }
    }

    @Test
    fun testRevertingToPreviousStateOnPlaybackError() {
        var failOnSecond = false
        val mockEngine = MockPlayerEngine { path, onReady, onError ->
            if (failOnSecond && path.contains("t2")) {
                onError?.invoke("File missing or unreadable")
            } else {
                onReady(60_000L)
            }
        }

        val controller = PlayerController(customEngine = mockEngine)
        val t1 = testTrack("t1")
        val t2 = testTrack("t2")

        try {
            // First track plays successfully
            controller.play(t1, listOf(t1, t2))
            waitUntil { controller.current?.id == "t1" && controller.isPlaying }
            assertEquals("t1", controller.current?.id)
            assertTrue(controller.isPlaying)

            // Now play second track which fails
            failOnSecond = true
            controller.play(t2, listOf(t1, t2))

            // Should revert back to previous state (t1) and isPlaying = false
            waitUntil { !controller.isPlaying && controller.appError.value != null }
            assertFalse(controller.isPlaying, "isPlaying must be false on playback error")
            assertNotNull(controller.appError.value)
        } finally {
            controller.release()
        }
    }

    @Test
    fun testErrorMessageDeduplicationWithinFourSeconds() {
        val controller = PlayerController(customEngine = MockPlayerEngine())
        try {
            val error = AppError.Generic("Test Error Message")

            // Post same error twice in quick succession
            controller.postError(error)
            val firstError = controller.appError.value
            assertNotNull(firstError)
            assertEquals("Test Error Message", firstError.message)

            // Second post should be deduplicated
            controller.postError(error)

            // Dismiss active error - if deduplicated, queue should be empty and value becomes null
            controller.dismissError()
            assertNull(controller.appError.value, "Deduplicated error should not appear in queue")
        } finally {
            controller.release()
        }
    }

    @Test
    fun testErrorMessageQueueAndAutoHide() {
        val controller = PlayerController(customEngine = MockPlayerEngine())
        try {
            val err1 = AppError.Generic("First Error")
            val err2 = AppError.Generic("Second Error")

            controller.postError(err1)
            controller.postError(err2)

            // Active should be err1
            assertEquals("First Error", controller.appError.value?.message)

            // Dismissing first advances to second
            controller.dismissError()
            assertEquals("Second Error", controller.appError.value?.message)

            // dismissAllErrors clears everything
            controller.dismissAllErrors()
            assertNull(controller.appError.value)
        } finally {
            controller.release()
        }
    }
}
