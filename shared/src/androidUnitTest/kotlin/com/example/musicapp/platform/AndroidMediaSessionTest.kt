package com.example.musicapp.platform

import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import com.example.musicapp.model.Track
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AndroidMediaSessionTest {

    @Test
    fun testJvxForwardingPlayerInterceptsNextAndPrevious() {
        var nextCalled = false
        var prevCalled = false

        AndroidBridge.onNextTrackRequested = { nextCalled = true }
        AndroidBridge.onPreviousTrackRequested = { prevCalled = true }

        val dummyPlayer = object : SimpleBasePlayer(Looper.getMainLooper()) {
            override fun getState(): State {
                return State.Builder()
                    .setAvailableCommands(Player.Commands.Builder().addAllCommands().build())
                    .build()
            }
        }
        val forwardingPlayer = JvxForwardingPlayer(dummyPlayer)

        // Verify available commands expose seekToNext and seekToPrevious
        assertTrue(forwardingPlayer.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT))
        assertTrue(forwardingPlayer.isCommandAvailable(Player.COMMAND_SEEK_TO_PREVIOUS))
        assertTrue(forwardingPlayer.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM))
        assertTrue(forwardingPlayer.isCommandAvailable(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM))

        // Intercept seekToNext
        forwardingPlayer.seekToNext()
        assertTrue(nextCalled, "seekToNext() should trigger AndroidBridge.onNextTrackRequested")

        forwardingPlayer.seekToNextMediaItem()
        assertTrue(nextCalled)

        // Intercept seekToPrevious
        forwardingPlayer.seekToPrevious()
        assertTrue(prevCalled, "seekToPrevious() should trigger AndroidBridge.onPreviousTrackRequested")

        forwardingPlayer.seekToPreviousMediaItem()
        assertTrue(prevCalled)
    }

    @Test
    fun testMediaMetadataBuildAndMediaItemAttachment() {
        val testTitle = "Song Title"
        val testArtist = "Test Artist"
        val testAlbum = "Album Name"
        val testArtwork = byteArrayOf(0x01, 0x02, 0x03, 0x04)

        val metadata = MediaMetadata.Builder()
            .setTitle(testTitle)
            .setArtist(testArtist)
            .setAlbumTitle(testAlbum)
            .setArtworkData(testArtwork, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri("file:///android_asset/sample.mp3")
            .setMediaMetadata(metadata)
            .build()

        assertEquals(testTitle, mediaItem.mediaMetadata.title.toString())
        assertEquals(testArtist, mediaItem.mediaMetadata.artist.toString())
        assertEquals(testAlbum, mediaItem.mediaMetadata.albumTitle.toString())
        assertContentEquals(testArtwork, mediaItem.mediaMetadata.artworkData)
    }

    @Test
    fun testRegisterPlatformPlayerCallbacks() {
        var nextTriggered = false
        var prevTriggered = false
        val track = Track(id = "1", title = "Test", artist = "Artist", durationMs = 120_000L)

        registerPlatformPlayerCallbacks(
            onNext = { nextTriggered = true },
            onPrevious = { prevTriggered = true },
            currentTrack = { track }
        )

        AndroidBridge.onNextTrackRequested?.invoke()
        AndroidBridge.onPreviousTrackRequested?.invoke()
        val supplierTrack = AndroidBridge.currentTrackSupplier?.invoke()

        assertTrue(nextTriggered)
        assertTrue(prevTriggered)
        assertEquals(track, supplierTrack)
    }
}
