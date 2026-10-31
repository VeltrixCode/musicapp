package com.example.musicapp

import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.example.musicapp.platform.AndroidBridge
import com.example.musicapp.player.PlayerController
import com.example.musicapp.ui.MusicApp

class MainActivity : ComponentActivity() {
    private var pending: ((Uri?) -> Unit)? = null
    private var mediaSession: MediaSession? = null
    private val player = PlayerController()

    private val picker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        pending?.invoke(uri)
        pending = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidBridge.appContext = applicationContext
        AndroidBridge.pickAudio = { callback ->
            pending = callback
            picker.launch(arrayOf("audio/*"))
        }
        AndroidBridge.onHeadphonesUnplugged = {
            player.pause()
        }

        // Android MediaSession integration with seek support
        mediaSession = MediaSession(this, "MusicAppMediaSession").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() { player.resume() }
                override fun onPause() { player.pause() }
                override fun onSkipToNext() { player.next() }
                override fun onSkipToPrevious() { player.previous() }
                override fun onSeekTo(pos: Long) { player.seekTo(pos) }
                override fun onStop() { player.pause() }
            })
            setPlaybackState(
                PlaybackState.Builder()
                    .setActions(
                        PlaybackState.ACTION_PLAY or
                        PlaybackState.ACTION_PAUSE or
                        PlaybackState.ACTION_PLAY_PAUSE or
                        PlaybackState.ACTION_SKIP_TO_NEXT or
                        PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackState.ACTION_SEEK_TO
                    )
                    .build()
            )
            isActive = true
        }

        setContent {
            MusicApp(player = player)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSession?.release()
        mediaSession = null
        player.release()
    }
}
