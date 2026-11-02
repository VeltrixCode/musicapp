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
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        handleOAuthIntent(intent)

        setContent {
            MusicApp(player = player)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOAuthIntent(intent)
    }

    private fun handleOAuthIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "com.example.musicapp" && (uri.host == "oauth2callback" || uri.path?.contains("oauth2callback") == true)) {
            val code = uri.getQueryParameter("code")
            val error = uri.getQueryParameter("error")
            if (!code.isNullOrBlank()) {
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                    val result = com.example.musicapp.oauth.GoogleOAuthAndroid.handleAuthCode(code)
                    if (result.isSuccess) {
                        val token = result.getOrNull()
                        if (!token.isNullOrBlank()) {
                            player.providerRegistry.gdrive.updateToken(token)
                            Toast.makeText(this@MainActivity, "Google Диск успешно подключен!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@MainActivity, "Ошибка входа Google: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } else if (error != null) {
                Toast.makeText(this, "Вход Google отменен: $error", Toast.LENGTH_SHORT).show()
            }
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
