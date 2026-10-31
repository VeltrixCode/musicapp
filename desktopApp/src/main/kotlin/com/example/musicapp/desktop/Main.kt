package com.example.musicapp.desktop

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.example.musicapp.ui.MusicApp

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "MusicApp",
        state = rememberWindowState(width = 1000.dp, height = 700.dp),
    ) {
        MusicApp()
    }
}
