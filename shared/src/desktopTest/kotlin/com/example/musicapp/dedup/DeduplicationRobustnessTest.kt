package com.example.musicapp.dedup

import com.example.musicapp.model.Track
import com.example.musicapp.model.TrackSource
import kotlin.test.Test
import kotlin.test.assertNull

class DeduplicationRobustnessTest {

    @Test
    fun testPickBestVariantWithEmptyListReturnsNull() {
        val result = DeduplicationService.pickBestVariant(emptyList())
        assertNull(result, "Empty tracks list must safely return null")
    }

    @Test
    fun testPickBestVariantWithEmptyVariantsInWorkReturnsNull() {
        val emptyWork = Work(
            id = "work-empty",
            title = "Empty Work",
            artist = "Unknown",
            variants = emptyList()
        )
        val result = DeduplicationService.pickBestVariant(emptyWork, emptyMap())
        assertNull(result, "Work with empty variants must safely return null")
    }

    @Test
    fun testPickBestVariantWhenTrackIdMissingFromMapReturnsNull() {
        val variant = TrackVariant(
            trackId = "missing-track-999",
            sourceProvider = TrackSource.AUDIUS,
            format = "mp3",
            isLossless = false,
            bitrateKbps = 320,
            durationMs = 180000L,
            isLocal = false,
            isCached = false,
            isCloud = true
        )
        val work = Work(
            id = "work-missing",
            title = "Work Missing Track",
            artist = "Artist",
            variants = listOf(variant)
        )
        // Map does NOT contain "missing-track-999"
        val tracksById = mapOf<String, Track>()
        val result = DeduplicationService.pickBestVariant(work, tracksById)
        assertNull(result, "Missing track in lookup map must safely return null without throwing exception")
    }
}
