package com.example.musicapp.dedup

import com.example.musicapp.model.Track
import com.example.musicapp.model.TrackSource
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DeduplicationTest {

    @BeforeTest
    fun setup() {
        DeduplicationStore.save(DedupDecisions())
    }

    @Test
    fun testVersionMarkerExtraction() {
        assertEquals(VersionMarker.REMIX, VersionMarker.extract("Get Lucky (Daft Punk Remix)"))
        assertEquals(VersionMarker.LIVE, VersionMarker.extract("Hotel California (Live at the Forum)"))
        assertEquals(VersionMarker.ACOUSTIC, VersionMarker.extract("Layla (Acoustic Unplugged)"))
        assertEquals(VersionMarker.INSTRUMENTAL, VersionMarker.extract("Numb (Instrumental)"))
        assertEquals(VersionMarker.COVER, VersionMarker.extract("Hurt (Johnny Cash Cover)"))
        assertEquals(VersionMarker.REMASTER, VersionMarker.extract("Bohemian Rhapsody (2011 Remaster)"))
        assertEquals(VersionMarker.RADIO_EDIT, VersionMarker.extract("One More Time (Radio Edit)"))
        assertEquals(VersionMarker.EXTENDED, VersionMarker.extract("Around the World (Extended Mix)"))
        assertEquals(VersionMarker.ORIGINAL, VersionMarker.extract("Smells Like Teen Spirit"))
    }

    @Test
    fun testVersionMarkersDoNotAutoMerge() {
        val original = Track(
            id = "t_orig",
            title = "Get Lucky",
            artist = "Daft Punk",
            durationMs = 240000L,
            sourceProvider = TrackSource.LOCAL
        )
        val remix = Track(
            id = "t_rmx",
            title = "Get Lucky (Remix)",
            artist = "Daft Punk",
            durationMs = 240000L,
            sourceProvider = TrackSource.LOCAL
        )
        val live = Track(
            id = "t_live",
            title = "Get Lucky (Live)",
            artist = "Daft Punk",
            durationMs = 240000L,
            sourceProvider = TrackSource.LOCAL
        )

        assertFalse(DeduplicationService.canAutoMerge(original, remix), "Original and Remix must not merge")
        assertFalse(DeduplicationService.canAutoMerge(original, live), "Original and Live must not merge")
        assertFalse(DeduplicationService.canAutoMerge(remix, live), "Remix and Live must not merge")
    }

    @Test
    fun testCrossSourceMergeHierarchy() {
        val local = Track(
            id = "t_local",
            title = "Starboy",
            artist = "The Weeknd",
            durationMs = 230000L,
            sourceProvider = TrackSource.LOCAL,
            format = "flac",
            isLossless = true
        )
        val cloudDrive = Track(
            id = "t_drive",
            title = "Starboy",
            artist = "The Weeknd",
            durationMs = 232000L, // 2s difference (within 3s tolerance)
            sourceProvider = TrackSource.GOOGLE_DRIVE,
            format = "mp3",
            bitrateKbps = 320
        )
        val cloudSubsonic = Track(
            id = "t_subsonic",
            title = "Starboy",
            artist = "The Weeknd",
            durationMs = 245000L, // 15s difference -> does not auto merge by title alone
            sourceProvider = TrackSource.SUBSONIC
        )

        // 1. Title + artist + within 3s merges
        assertTrue(DeduplicationService.canAutoMerge(local, cloudDrive), "Local and cloud track within 2s should auto merge")
        // 2. Beyond 3s difference does not merge
        assertFalse(DeduplicationService.canAutoMerge(local, cloudSubsonic), "Duration difference >3s must not auto merge without MBID/ISRC")

        // 3. MusicBrainz ID match merges even if duration is different
        val cloudWithMbid = cloudSubsonic.copy(musicBrainzId = "mbid-12345")
        val localWithMbid = local.copy(musicBrainzId = "mbid-12345")
        assertTrue(DeduplicationService.canAutoMerge(localWithMbid, cloudWithMbid), "Matching MusicBrainz ID must merge")

        // 4. ISRC match merges
        val cloudWithIsrc = cloudSubsonic.copy(isrc = "USUM71600001")
        val localWithIsrc = local.copy(isrc = "USUM71600001")
        assertTrue(DeduplicationService.canAutoMerge(localWithIsrc, cloudWithIsrc), "Matching ISRC must merge")

        // 5. AcoustID fingerprint match merges
        val cloudWithFp = cloudSubsonic.copy(audioFingerprint = "fp_abc123")
        val localWithFp = local.copy(audioFingerprint = "fp_abc123")
        assertTrue(DeduplicationService.canAutoMerge(localWithFp, cloudWithFp), "Matching Audio Fingerprint must merge")
    }

    @Test
    fun testBestVariantRanking() {
        val cloudMp3 = Track(
            id = "t_cloud_mp3",
            title = "Instant Crush",
            artist = "Daft Punk",
            sourceProvider = TrackSource.JAMENDO,
            format = "mp3",
            bitrateKbps = 128,
            durationMs = 300000L
        )
        val cloudFlac = Track(
            id = "t_cloud_flac",
            title = "Instant Crush",
            artist = "Daft Punk",
            sourceProvider = TrackSource.SUBSONIC,
            format = "flac",
            isLossless = true,
            bitrateKbps = 950,
            durationMs = 300000L
        )
        val cachedMp3 = Track(
            id = "t_cached_mp3",
            title = "Instant Crush",
            artist = "Daft Punk",
            sourceProvider = TrackSource.AUDIUS,
            format = "mp3",
            bitrateKbps = 320,
            durationMs = 300000L,
            cachedFilePath = "C:/cache/audio/123.mp3"
        )
        val localLossless = Track(
            id = "t_local_flac",
            title = "Instant Crush",
            artist = "Daft Punk",
            sourceProvider = TrackSource.LOCAL,
            format = "flac",
            isLossless = true,
            bitrateKbps = 1411,
            durationMs = 300000L
        )

        // Lossless beats lossy
        val best1 = DeduplicationService.pickBestVariant(listOf(cloudMp3, cloudFlac))
        assertEquals(cloudFlac.id, best1?.id)

        // Cached beats remote cloud lossy
        val best2 = DeduplicationService.pickBestVariant(listOf(cloudMp3, cachedMp3))
        assertEquals(cachedMp3.id, best2?.id)

        // Local beats everything
        val best3 = DeduplicationService.pickBestVariant(listOf(cloudFlac, cachedMp3, localLossless))
        assertEquals(localLossless.id, best3?.id)
    }

    @Test
    fun testManualDecisionsPersistenceAndOverrides() {
        val t1 = Track(id = "tr1", title = "Track A", artist = "Artist A", durationMs = 200000L)
        val t2 = Track(id = "tr2", title = "Track B", artist = "Artist B", durationMs = 300000L)

        // 1. Manual merge two different tracks
        val workId = DeduplicationStore.mergeTracks(setOf("tr1", "tr2"))
        val decisions = DeduplicationStore.load()
        assertEquals(workId, decisions.manualMerges["tr1"])
        assertEquals(workId, decisions.manualMerges["tr2"])

        // 2. Set preferred variant
        DeduplicationStore.setPreferredVariant(workId, "tr2")
        val updatedDecisions = DeduplicationStore.load()
        assertEquals("tr2", updatedDecisions.preferredVariants[workId])

        // 3. Grouping respects manual merge and preferred variant
        val works = DeduplicationService.groupTracksIntoWorks(listOf(t1, t2), updatedDecisions)
        assertEquals(1, works.size, "Manually merged tracks should produce exactly 1 Work")
        val work = works.first()
        assertEquals("tr2", work.preferredVariantId)
        assertEquals(2, work.variants.size)

        // 4. Split variant
        DeduplicationStore.splitVariant(workId, "tr2")
        val afterSplitDecisions = DeduplicationStore.load()
        assertFalse(afterSplitDecisions.manualMerges.containsKey("tr2"))
        assertTrue(afterSplitDecisions.manualSplits.any { it.contains("tr1") && it.contains("tr2") })

        // 5. Once split, canAutoMerge is strictly forbidden
        assertFalse(DeduplicationService.canAutoMerge(t1, t2, afterSplitDecisions.manualSplits))
    }
}
