package com.example.musicapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.example.musicapp.platform.AndroidBridge
import com.example.musicapp.player.PlayerController
import com.example.musicapp.ui.MusicApp

class MainActivity : ComponentActivity() {
    private var pendingMediaFiles: ((List<Uri>) -> Unit)? = null
    private var pendingAudio: ((Uri?) -> Unit)? = null
    private var pendingFolder: ((Uri?) -> Unit)? = null

    private val player: PlayerController get() = getPlayerController()
    private var controllerFuture: com.google.common.util.concurrent.ListenableFuture<androidx.media3.session.MediaController>? = null

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(
                this,
                "Разрешение на уведомления отклонено. Управление на экране блокировки и в шторке недоступно.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private val mediaFilesPicker = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach { uri ->
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) {}
        }
        pendingMediaFiles?.invoke(uris)
        pendingMediaFiles = null
    }

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
        com.example.musicapp.config.AppFeatures.devOnlineSourcesEnabled = true
        AndroidBridge.appContext = applicationContext
        runCatching { com.example.musicapp.platform.AndroidPlaybackManager.getOrCreatePlayer(applicationContext) }
        AndroidBridge.pickMediaFiles = { callback ->
            pendingMediaFiles = callback
            mediaFilesPicker.launch(arrayOf("*/*"))
        }
        AndroidBridge.pickAudio = { callback ->
            pendingMediaFiles = { uris ->
                if (uris.isNotEmpty()) {
                    uris.forEach { callback(it) }
                } else {
                    callback(null)
                }
            }
            mediaFilesPicker.launch(arrayOf("*/*"))
        }
        AndroidBridge.pickFolder = { callback ->
            pendingFolder = { treeUri ->
                if (treeUri == null) {
                    Toast.makeText(
                        this@MainActivity,
                        "Папка не выбрана. Для выбора архивов или файлов используйте «Файл / ZIP».",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                callback(treeUri)
            }
            try {
                folderPicker.launch(null)
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Выбор папки недоступен на данном устройстве. Используйте «Файл / ZIP».",
                    Toast.LENGTH_LONG
                ).show()
                callback(null)
            }
        }
        AndroidBridge.onHeadphonesUnplugged = {
            player.pause()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            MusicApp(player = player)
        }
    }

    override fun onStart() {
        super.onStart()
        runCatching {
            val sessionToken = androidx.media3.session.SessionToken(
                this,
                android.content.ComponentName(this, com.example.musicapp.playback.PlaybackService::class.java)
            )
            controllerFuture = androidx.media3.session.MediaController.Builder(this, sessionToken).buildAsync()
        }
    }

    override fun onStop() {
        super.onStop()
        controllerFuture?.let { androidx.media3.session.MediaController.releaseFuture(it) }
        controllerFuture = null
    }

    override fun onDestroy() {
        super.onDestroy()
        // Do not release player on Activity destruction if playback is ongoing in foreground service
        if (isFinishing && !player.isPlaying) {
            player.release()
            singletonPlayer = null
        }
    }

    companion object {
        private var singletonPlayer: PlayerController? = null

        fun getPlayerController(): PlayerController {
            return singletonPlayer ?: PlayerController().also { singletonPlayer = it }
        }
    }
}
