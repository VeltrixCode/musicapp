package com.example.musicapp

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.example.musicapp.platform.AndroidBridge
import com.example.musicapp.ui.MusicApp

class MainActivity : ComponentActivity() {
    private var pending: ((Uri?) -> Unit)? = null

    private val picker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        pending?.invoke(uri)
        pending = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidBridge.pickAudio = { callback ->
            pending = callback
            picker.launch(arrayOf("audio/*"))
        }
        setContent { MusicApp() }
    }
}
