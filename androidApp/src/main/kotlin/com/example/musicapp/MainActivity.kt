package com.example.musicapp

import android.content.Intent
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
    private var pendingAudio: ((Uri?) -> Unit)? = null
    private var pendingFolder: ((Uri?) -> Unit)? = null
    private var mediaSession: MediaSession? = null
    private val player = PlayerController()

    private val audioPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) {}
        }
        pendingAudio?.invoke(uri)
        pendingAudio = null
    }

    private val folderPicker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { treeUri ->
        if (treeUri != null) {
            try {
                contentResolver.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) {}
        }
        pendingFolder?.invoke(treeUri)
        pendingFolder = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidBridge.appContext = applicationContext
        AndroidBridge.pickAudio = { callback ->
            pendingAudio = callback
            audioPicker.launch(arrayOf("audio/*"))
        }
        AndroidBridge.pickFolder = { callback ->
            pendingFolder = callback
            folderPicker.launch(null)
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
