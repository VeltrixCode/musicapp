package com.example.musicapp.platform

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Looper
import androidx.media3.common.AudioAttributes as Media3AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession

class JvxForwardingPlayer(player: Player) : ForwardingPlayer(player) {
    override fun getAvailableCommands(): Player.Commands {
        return super.getAvailableCommands().buildUpon()
            .add(Player.COMMAND_SEEK_TO_NEXT)
            .add(Player.COMMAND_SEEK_TO_PREVIOUS)
            .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
            .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
            .build()
    }

    override fun isCommandAvailable(command: Int): Boolean {
        return when (command) {
            Player.COMMAND_SEEK_TO_NEXT,
            Player.COMMAND_SEEK_TO_PREVIOUS,
            Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
            Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> true
            else -> super.isCommandAvailable(command)
        }
    }

    override fun hasNextMediaItem(): Boolean = true
    override fun hasPreviousMediaItem(): Boolean = true

    override fun seekToNext() {
        AndroidBridge.onNextTrackRequested?.invoke()
    }

    override fun seekToNextMediaItem() {
        AndroidBridge.onNextTrackRequested?.invoke()
    }

    override fun seekToPrevious() {
        AndroidBridge.onPreviousTrackRequested?.invoke()
    }

    override fun seekToPreviousMediaItem() {
        AndroidBridge.onPreviousTrackRequested?.invoke()
    }
}

object AndroidPlaybackManager {
    const val CHANNEL_ID = "jvx_playback_channel"
    const val NOTIFICATION_ID = 1001

    private var exoPlayer: ExoPlayer? = null
    private var forwardingPlayer: JvxForwardingPlayer? = null
    private var mediaSession: MediaSession? = null

    fun getOrCreatePlayer(ctx: Context): ExoPlayer {
        require(Looper.myLooper() == Looper.getMainLooper()) { "ExoPlayer must be accessed on the Main Looper" }
        exoPlayer?.let { return it }

        val httpDataSourceFactory = OkHttpDataSource.Factory(streamingOkHttpClient)
        val dataSourceFactory = DefaultDataSource.Factory(ctx, httpDataSourceFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(ctx)
            .setDataSourceFactory(dataSourceFactory)

        val media3AudioAttributes = Media3AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val renderersFactory = DefaultRenderersFactory(ctx)
            .setEnableAudioTrackPlaybackParams(false)

        val player = ExoPlayer.Builder(ctx, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(media3AudioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        val fwd = JvxForwardingPlayer(player)
        forwardingPlayer = fwd
        exoPlayer = player

        createNotificationChannel(ctx)
        return player
    }

    fun getOrCreateMediaSession(ctx: Context): MediaSession {
        require(Looper.myLooper() == Looper.getMainLooper()) { "MediaSession must be accessed on the Main Looper" }
        mediaSession?.let { return it }
        val player = getOrCreatePlayer(ctx)
        val fwd = forwardingPlayer ?: JvxForwardingPlayer(player).also { forwardingPlayer = it }

        val sessionActivityIntent = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val sessionActivityPendingIntent = sessionActivityIntent?.let {
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            PendingIntent.getActivity(ctx, 0, it, flags)
        }

        val sessionBuilder = MediaSession.Builder(ctx, fwd)
        sessionActivityPendingIntent?.let { sessionBuilder.setSessionActivity(it) }

        val session = sessionBuilder.build()
        mediaSession = session
        return session
    }

    fun ensureServiceStarted(ctx: Context) {
        runCatching {
            val serviceIntent = Intent().setClassName(ctx.packageName, "com.example.musicapp.playback.PlaybackService")
            // Use standard startService. MediaSessionService automatically handles foreground transition when playback starts.
            ctx.startService(serviceIntent)
        }
    }

    fun createNotificationChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "JVXMusic Воспроизведение",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Управление воспроизведением и экран блокировки"
                setShowBadge(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            val nm = ctx.getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }
    }

    fun release() {
        runCatching {
            mediaSession?.run {
                player.release()
                release()
            }
            mediaSession = null
            exoPlayer = null
            forwardingPlayer = null
        }
    }
}
