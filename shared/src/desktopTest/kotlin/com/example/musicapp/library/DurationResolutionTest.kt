package com.example.musicapp.library

import com.example.musicapp.model.Track
import com.example.musicapp.util.getEffectiveDuration
import kotlin.test.Test
import kotlin.test.assertEquals

class DurationResolutionTest {

    @Test
    fun testGetEffectiveDurationPrioritizesAuthoritativePlayerDuration() {
        val track = Track(
            id = "t1",
            title = "Test Song",
            artist = "Artist",
            album = "Album",
            durationMs = 180_000L
        )

        // If player has not yet resolved duration or stopped, use tag duration
        assertEquals(180_000L, getEffectiveDuration(track, 0L))

        // When player engine resolves exact hardware duration, use it
        assertEquals(182_450L, getEffectiveDuration(track, 182_450L))
    }

    @Test
    fun testGetEffectiveDurationWithZeroTagDuration() {
        val track = Track(
            id = "t2",
            title = "Live Stream",
            artist = "Artist",
            album = "Album",
            durationMs = 0L
        )

        assertEquals(0L, getEffectiveDuration(track, 0L))
        assertEquals(240_000L, getEffectiveDuration(track, 240_000L))
    }

    @Test
    fun testLibraryUpdateTrackDuration() {
        val library = Library()
        val track = Track(
            id = "track_dur_test",
            title = "Dynamic Duration Track",
            artist = "Veltrix",
            album = "JVX Album",
            durationMs = 0L
        )
        library.addTrack(track)

        assertEquals(0L, library.tracks.find { it.id == "track_dur_test" }?.durationMs)

        // When player resolves authoritative duration during first playback:
        library.updateTrackDuration("track_dur_test", 215_000L)

        val updated = library.tracks.find { it.id == "track_dur_test" }
        assertEquals(215_000L, updated?.durationMs)

        // Clean up
        library.removeTrack("track_dur_test")
    }
}
