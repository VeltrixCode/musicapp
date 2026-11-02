package com.example.musicapp.library

import com.example.musicapp.config.AppFeatures
import com.example.musicapp.model.Playlist
import com.example.musicapp.model.Track
import com.example.musicapp.model.TrackSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VisibleTracksCounterTest {

    @Test
    fun testVisibleTracksOnlyReturnsLocalTracksWhenOnlineSourcesDisabled() {
        val library = Library()
        library.tracks.clear()
        library.playlists.clear()

        val localTrack1 = Track(
            id = "loc-1",
            title = "Local 1",
            artist = "Artist A",
            album = "Album 1",
            sourceProvider = TrackSource.LOCAL,
            source = "C:/music/1.mp3"
        )
        val localTrack2 = Track(
            id = "loc-2",
            title = "Local 2",
            artist = "Artist B",
            album = "Album 2",
            sourceProvider = TrackSource.LOCAL,
            source = "C:/music/2.mp3"
        )
        val cloudTrack1 = Track(
            id = "cloud-1",
            title = "Cloud 1",
            artist = "Cloud Artist",
            album = "Cloud Album",
            sourceProvider = TrackSource.AUDIUS,
            source = "https://audius.co/stream/1"
        )
        val cloudTrack2 = Track(
            id = "cloud-2",
            title = "Cloud 2",
            artist = "Jamendo Artist",
            album = "Jamendo Album",
            sourceProvider = TrackSource.JAMENDO,
            source = "https://jamendo.com/stream/2"
        )

        library.addTrack(localTrack1)
        library.addTrack(localTrack2)
        library.addTrack(cloudTrack1)
        library.addTrack(cloudTrack2)

        val prevDev = AppFeatures.devOnlineSourcesEnabled
        AppFeatures.devOnlineSourcesEnabled = false
        try {
            // When ONLINE_SOURCES = false
            assertFalse(AppFeatures.isOnlineSourcesEnabled, "Online sources must be disabled when overridden")

            // 1. Library visible tracks count
            val visible = library.visibleTracks
            assertEquals(2, visible.size, "Visible tracks must only contain local tracks")
            assertTrue(visible.all { it.sourceProvider == TrackSource.LOCAL })

            // 2. Playlists counting
            val mixedPlaylist = Playlist(
                id = "p-mixed",
                name = "Mixed Playlist",
                trackIds = listOf("loc-1", "cloud-1", "cloud-2")
            )
            library.createPlaylist("Mixed Playlist")
            library.addToPlaylist("p-mixed", "loc-1")
            library.addToPlaylist("p-mixed", "cloud-1")
            library.addToPlaylist("p-mixed", "cloud-2")

            val playlistTracks = library.tracksIn(mixedPlaylist)
            assertEquals(1, playlistTracks.size, "Playlist tracksIn must only return visible local tracks")
            assertEquals("loc-1", playlistTracks.first().id)

            // 3. Header and Table row count agreement
            // The table renders rows from sorted/filtered visibleTracks:
            val tableRowCount = visible.size
            val headerCounter = library.visibleTracks.size
            assertEquals(headerCounter, tableRowCount, "Header counter must strictly equal visible table rows count")
        } finally {
            AppFeatures.devOnlineSourcesEnabled = prevDev
        }
    }
}
