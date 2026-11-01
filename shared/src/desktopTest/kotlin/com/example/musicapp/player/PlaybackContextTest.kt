package com.example.musicapp.player

import com.example.musicapp.model.Track
import com.example.musicapp.model.TrackSource
import com.example.musicapp.platform.PlayerEngine
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaybackContextTest {

    private class InstantReadyMockEngine : PlayerEngine {
        private var _isPlaying = false
        var playLatch = CountDownLatch(1)

        override fun load(path: String, onReady: (Long) -> Unit, onFinished: () -> Unit) {
            load(path, onReady, onFinished, null)
        }
        override fun load(path: String, onReady: (Long) -> Unit, onFinished: () -> Unit, onError: ((String) -> Unit)?) {
            onReady(180_000L)
        }
        override fun play() {
            _isPlaying = true
            playLatch.countDown()
        }
        override fun pause() {
            _isPlaying = false
        }
        override fun seekTo(ms: Long) {}
        override fun setVolume(volume: Float) {}
        override fun setPlaybackSpeed(speed: Float) {}
        override fun setEqualizerBand(bandIndex: Int, gainDb: Float) {}
        override fun setSpectrumListener(listener: ((FloatArray) -> Unit)?) {}
        override fun positionMs(): Long = 10_000L
        override fun durationMs(): Long = 180_000L
        override fun release() { _isPlaying = false }
    }

    private fun sampleTrack(id: String) = Track(
        id = id,
        title = "Track $id",
        artist = "Artist",
        source = "file:///dummy/$id.mp3",
        sourceProvider = TrackSource.LOCAL,
        durationMs = 180_000L
    )

    private fun flushSwing() {
        val flushLatch = CountDownLatch(1)
        SwingUtilities.invokeLater { flushLatch.countDown() }
        flushLatch.await(2, TimeUnit.SECONDS)
    }

    @Test
    fun testPlaybackContextTracksSourceCardAndOtherCardsShowPlay() {
        val engine = InstantReadyMockEngine()
        val player = PlayerController(customEngine = engine)

        val track1 = sampleTrack("1")
        val track2 = sampleTrack("2")
        val queue = listOf(track1, track2)

        // 1. Play from Playlist "pl-fav"
        val playlistContext = PlaybackContext(PlaybackContextType.Playlist, "pl-fav", "Избранное")
        player.play(track1, queue, playlistContext)

        assertTrue(engine.playLatch.await(3, TimeUnit.SECONDS), "Player engine should receive play()")
        flushSwing()

        assertTrue(player.isPlaying, "Player should be playing")
        assertEquals(playlistContext, player.playbackContext.value)

        // Source card shows pause state (isContextPlaying == true)
        assertTrue(player.isContextPlaying(PlaybackContextType.Playlist, "pl-fav"), "Source card should report isContextPlaying = true")

        // Other cards/contexts report false (meaning they show Play on hover, not Pause)
        assertFalse(player.isContextPlaying(PlaybackContextType.Playlist, "pl-other"), "Different playlist ID should report isContextPlaying = false")
        assertFalse(player.isContextPlaying(PlaybackContextType.SmartMix, "pl-fav"), "Different context type with same ID should report isContextPlaying = false")
        assertFalse(player.isContextPlaying(PlaybackContextType.Album, "album-1"), "Album card should report isContextPlaying = false")
        assertFalse(player.isContextPlaying(PlaybackContextType.TrackShelf, "shelf-1"), "Shelf card should report isContextPlaying = false")

        // 2. Pause playback
        player.pause()
        flushSwing()
        assertFalse(player.isPlaying, "Player should be paused")
        assertFalse(player.isContextPlaying(PlaybackContextType.Playlist, "pl-fav"), "When paused, isContextPlaying should be false so source card shows Play")

        // 3. Resume playback
        player.resume()
        flushSwing()
        assertTrue(player.isPlaying, "Player should be resumed")
        assertTrue(player.isContextPlaying(PlaybackContextType.Playlist, "pl-fav"), "When resumed, isContextPlaying should be true again")

        // 4. Switch to Album "alb-night"
        engine.playLatch = CountDownLatch(1)
        val albumContext = PlaybackContext(PlaybackContextType.Album, "alb-night", "Night Drive")
        player.play(track2, queue, albumContext)

        assertTrue(engine.playLatch.await(3, TimeUnit.SECONDS), "Player engine should receive play() for album track")
        flushSwing()

        assertTrue(player.isContextPlaying(PlaybackContextType.Album, "alb-night"), "New source card should report isContextPlaying = true")
        assertFalse(player.isContextPlaying(PlaybackContextType.Playlist, "pl-fav"), "Previous source playlist should now report isContextPlaying = false")

        player.release()
    }
}
