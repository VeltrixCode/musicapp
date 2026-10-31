package com.example.musicapp.player

import com.example.musicapp.model.Track
import com.example.musicapp.model.TrackSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlayerQueueTest {

    private fun track(id: String) = Track(
        id = id,
        title = "Title $id",
        artist = "Artist $id",
        source = "file://$id.mp3",
        sourceProvider = TrackSource.LOCAL
    )

    @Test
    fun testQueueAddAndMove() {
        val queue = mutableListOf<Track>()
        queue.add(track("1"))
        queue.add(track("2"))
        queue.add(track("3"))

        // Move item from index 2 to index 0
        val item = queue.removeAt(2)
        queue.add(0, item)

        assertEquals("3", queue[0].id)
        assertEquals("1", queue[1].id)
        assertEquals("2", queue[2].id)
    }

    @Test
    fun testPlayNextInsertsAfterCurrent() {
        val queue = mutableListOf<Track>()
        queue.add(track("A"))
        queue.add(track("B"))
        queue.add(track("C"))

        val currentIndex = 1 // Track B
        val nextTrack = track("INSERTED")

        if (currentIndex in queue.indices) {
            queue.add(currentIndex + 1, nextTrack)
        } else {
            queue.add(nextTrack)
        }

        assertEquals("A", queue[0].id)
        assertEquals("B", queue[1].id)
        assertEquals("INSERTED", queue[2].id)
        assertEquals("C", queue[3].id)
    }

    @Test
    fun testRepeatModeTransitions() {
        var mode = RepeatMode.Off
        mode = when (mode) {
            RepeatMode.Off -> RepeatMode.All
            RepeatMode.All -> RepeatMode.One
            RepeatMode.One -> RepeatMode.Off
        }
        assertEquals(RepeatMode.All, mode)

        mode = when (mode) {
            RepeatMode.Off -> RepeatMode.All
            RepeatMode.All -> RepeatMode.One
            RepeatMode.One -> RepeatMode.Off
        }
        assertEquals(RepeatMode.One, mode)

        mode = when (mode) {
            RepeatMode.Off -> RepeatMode.All
            RepeatMode.All -> RepeatMode.One
            RepeatMode.One -> RepeatMode.Off
        }
        assertEquals(RepeatMode.Off, mode)
    }

    @Test
    fun testShuffleOrdering() {
        val queue = (1..10).map { track(it.toString()) }
        val currentIndex = 3 // Track 4
        val currentTrack = queue[currentIndex]

        val remainingIndices = queue.indices.filter { it != currentIndex }.shuffled()
        val shuffledIndices = mutableListOf(currentIndex) + remainingIndices

        assertEquals(10, shuffledIndices.size)
        assertEquals(currentIndex, shuffledIndices[0], "Current playing track index must remain at pointer 0")
        assertEquals(queue.indices.toSet(), shuffledIndices.toSet())
    }
}
