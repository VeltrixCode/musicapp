package com.example.musicapp.util

import com.example.musicapp.model.Track
import com.example.musicapp.model.TrackSource
import kotlin.test.Test
import kotlin.test.assertEquals

class TrackDeduplicationTest {

    @Test
    fun testExactDuplicatesCollapseInFavorOfLocalSource() {
        val remoteTrack = Track(
            id = "rem_1",
            title = "Bohemian Rhapsody",
            artist = "Queen",
            durationMs = 354000L,
            sourceProvider = TrackSource.GOOGLE_DRIVE,
            source = "https://gdrive/bohemian.mp3"
        )

        val localTrack = Track(
            id = "loc_1",
            title = "Bohemian Rhapsody",
            artist = "Queen",
            durationMs = 354200L, // 200ms difference (rounds to same 4s block)
            sourceProvider = TrackSource.LOCAL,
            source = "C:/Music/bohemian.mp3"
        )

        val deduplicated = TrackDeduplicator.deduplicate(listOf(remoteTrack, localTrack))
        assertEquals(1, deduplicated.size)
        assertEquals("loc_1", deduplicated.first().id)
        assertEquals(TrackSource.LOCAL, deduplicated.first().sourceProvider)
    }

    @Test
    fun testCachedTrackFavoredOverCloudTrack() {
        val cloudTrack = Track(
            id = "yandex_1",
            title = "Yesterday",
            artist = "The Beatles",
            durationMs = 125000L,
            sourceProvider = TrackSource.YANDEX_DISK
        )

        val cachedTrack = Track(
            id = "audius_1",
            title = "Yesterday",
            artist = "The Beatles",
            durationMs = 125100L,
            sourceProvider = TrackSource.AUDIUS,
            isOfflineAvailable = true,
            cachedFilePath = "cache/yesterday.mp3"
        )

        val deduplicated = TrackDeduplicator.deduplicate(listOf(cloudTrack, cachedTrack))
        assertEquals(1, deduplicated.size)
        assertEquals("audius_1", deduplicated.first().id)
    }

    @Test
    fun testDifferentVersionsAreNotMerged() {
        val studio = Track(
            id = "t_studio",
            title = "Hotel California",
            artist = "Eagles",
            durationMs = 390000L,
            sourceProvider = TrackSource.LOCAL
        )

        val live = Track(
            id = "t_live",
            title = "Hotel California (Live at The Forum)",
            artist = "Eagles",
            durationMs = 430000L,
            sourceProvider = TrackSource.JAMENDO
        )

        val acoustic = Track(
            id = "t_acoustic",
            title = "Hotel California [Acoustic]",
            artist = "Eagles",
            durationMs = 385000L,
            sourceProvider = TrackSource.AUDIUS
        )

        val remix = Track(
            id = "t_remix",
            title = "Hotel California - Club Remix",
            artist = "Eagles",
            durationMs = 320000L,
            sourceProvider = TrackSource.JAMENDO
        )

        val deduplicated = TrackDeduplicator.deduplicate(listOf(studio, live, acoustic, remix))
        assertEquals(4, deduplicated.size, "Versions must NOT be collapsed together")
    }

    @Test
    fun testPunctuationAndCasingNormalized() {
        val t1 = Track(
            id = "1",
            title = "Don't Stop Me Now!",
            artist = "Queen",
            durationMs = 210000L,
            sourceProvider = TrackSource.AUDIUS
        )
        val t2 = Track(
            id = "2",
            title = "dont stop me now",
            artist = "QUEEN",
            durationMs = 210500L,
            sourceProvider = TrackSource.LOCAL
        )

        val deduplicated = TrackDeduplicator.deduplicate(listOf(t1, t2))
        assertEquals(1, deduplicated.size)
        assertEquals("2", deduplicated.first().id)
    }
}
