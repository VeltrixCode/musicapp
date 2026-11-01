package com.example.musicapp.playback

import android.content.Intent
import androidx.media3.common.Player
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.musicapp.platform.AndroidPlaybackManager

class PlaybackService : MediaSessionService() {

    override fun onCreate() {
        super.onCreate()
        AndroidPlaybackManager.createNotificationChannel(this)
        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(AndroidPlaybackManager.CHANNEL_ID)
            .build()
        setMediaNotificationProvider(notificationProvider)

        val session = AndroidPlaybackManager.getOrCreateMediaSession(this)
        addSession(session)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return AndroidPlaybackManager.getOrCreateMediaSession(this)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = AndroidPlaybackManager.getOrCreatePlayer(this)
        if (!player.playWhenReady || player.mediaItemCount == 0 || player.playbackState == Player.STATE_ENDED) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        AndroidPlaybackManager.release()
        super.onDestroy()
    }
}
