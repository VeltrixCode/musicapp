package com.example.musicapp.player

import com.example.musicapp.model.Track
import com.example.musicapp.model.TrackSource
import com.example.musicapp.platform.PlayerEngine
import com.example.musicapp.settings.AppSettings
import com.example.musicapp.settings.SettingsRepository
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.swing.SwingUtilities
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QueueContextBehaviorTest {

    private class MockEngine : PlayerEngine {
        private var _isPlaying = false
        override fun load(path: String, onReady: (Long) -> Unit, onFinished: () -> Unit) {
            onReady(200_000L)
        }
        override fun load(path: String, onReady: (Long) -> Unit, onFinished: () -> Unit, onError: ((String) -> Unit)?) {
            onReady(200_000L)
        }
        override fun play() { _isPlaying = true }
        override fun pause() { _isPlaying = false }
        override fun seekTo(ms: Long) {}
        override fun setVolume(volume: Float) {}
        override fun setPlaybackSpeed(speed: Float) {}
        override fun setEqualizerBand(bandIndex: Int, gainDb: Float) {}
        override fun setSpectrumListener(listener: ((FloatArray) -> Unit)?) {}
        override fun positionMs(): Long = 0L
        override fun durationMs(): Long = 200_000L
        override fun release() { _isPlaying = false }
    }

    private fun sampleTrack(id: String) = Track(
        id = id,
        title = "Track $id",
        artist = "Artist $id",
        source = "file:///music/$id.mp3",
        sourceProvider = TrackSource.LOCAL,
        durationMs = 200_000L
    )

    private fun flushSwing() {
        val latch = CountDownLatch(1)
        SwingUtilities.invokeLater { latch.countDown() }
        latch.await(2, TimeUnit.SECONDS)
    }

    private lateinit var player: PlayerController
    private lateinit var settingsRepo: SettingsRepository

    @BeforeTest
    fun setUp() {
        settingsRepo = SettingsRepository()
        settingsRepo.save(AppSettings())
        player = PlayerController(
            customEngine = MockEngine(),
            settingsRepository = settingsRepo
        )
    }

    @AfterTest
    fun tearDown() {
        player.release()
        flushSwing()
    }

    @Test
    fun testPlaylistQueueOfTwoTracksNextTransitionsToSecondTrack() {
        val t1 = sampleTrack("track_p1")
        val t2 = sampleTrack("track_p2")
        val playlistTracks = listOf(t1, t2)

        // Launch from playlist: queue must strictly be the 2 playlist tracks
        player.play(t1, playlistTracks)
        flushSwing()

        assertEquals(2, player.queue.size)
        assertEquals("track_p1", player.current?.id)

        // Next -> advances to track_p2
        player.next()
        flushSwing()

        assertEquals("track_p2", player.current?.id)

        // Repeat is Off: next on last track ends queue
        player.setRepeat(RepeatMode.Off)
        player.next()
        flushSwing()

        // Current stays on last track or playback pauses
        assertFalse(player.isPlaying)

        // Repeat is All: next on last track loops to first track
        player.setRepeat(RepeatMode.All)
        player.next()
        flushSwing()

        assertEquals("track_p1", player.current?.id)
    }

    @Test
    fun testSearchQueueMatchesSearchResultsOnly() {
        val s1 = sampleTrack("search_1")
        val s2 = sampleTrack("search_2")
        val searchResults = listOf(s1, s2)

        // Launch from search: queue must be searchResults, NOT the whole library
        player.play(
            s1,
            searchResults,
            PlaybackContext(PlaybackContextType.Search, "evan", "Поиск: evan")
        )
        flushSwing()

        assertEquals(2, player.queue.size, "Queue must contain only the search results")
        assertEquals(listOf("search_1", "search_2"), player.queue.map { it.id })
        assertEquals("search_1", player.current?.id)

        player.next()
        flushSwing()

        assertEquals("search_2", player.current?.id)
    }

    @Test
    fun testShuffleAndRepeatOperateStrictlyWithinContextQueue() {
        val tracks = (1..5).map { sampleTrack("ctx_$it") }
        player.play(tracks[0], tracks)
        flushSwing()

        assertEquals(5, player.queue.size)
        player.setShuffle(true)
        player.setRepeat(RepeatMode.All)

        val playedTrackIds = mutableSetOf<String>()
        playedTrackIds.add(player.current!!.id)

        // Advance 10 times in shuffled + repeat-all mode
        repeat(10) {
            player.next()
            flushSwing()
            val curr = player.current!!.id
            assertTrue(tracks.any { it.id == curr }, "Every played track must belong to the initial context list")
            playedTrackIds.add(curr)
        }

        // All 5 context tracks should have been reached
        assertEquals(5, playedTrackIds.size, "Shuffle must cycle through all context queue tracks")
    }
}
