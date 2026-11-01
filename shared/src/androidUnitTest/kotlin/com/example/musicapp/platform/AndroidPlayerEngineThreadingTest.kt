package com.example.musicapp.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class AndroidPlayerEngineThreadingTest {

    @Test
    fun testBackgroundCoroutineEngineCallsDoNotThrowThreadException() = runBlocking {
        val engine = AndroidPlayerEngine()

        // Calls from background worker thread (Dispatchers.Default)
        withContext(Dispatchers.Default) {
            // Public getters reading cached volatile state without touching ExoPlayer directly
            assertEquals(0L, engine.positionMs())
            assertEquals(0L, engine.durationMs())
            assertFalse(engine.isPlaying())

            // Methods that dispatch to Main Looper
            engine.pause()
            engine.seekTo(1000L)
            engine.setVolume(0.5f)
            engine.setPlaybackSpeed(1.2f)
            engine.setEqualizerEnabled(false)
            engine.release()
        }

        // Verify getters remain stable and thread-safe
        assertFalse(engine.isPlaying())
        assertEquals(1000L, engine.positionMs())
    }

    @Test
    fun testIsPlayingResetOnError() {
        val engine = AndroidPlayerEngine()
        var errorReported = false

        engine.load(
            path = "non_existent.mp3",
            onReady = {},
            onFinished = {},
            onError = {
                errorReported = true
            }
        )

        // isPlaying must be false
        assertFalse(engine.isPlaying(), "isPlaying must remain false when file cannot be loaded")
        engine.release()
    }
}
