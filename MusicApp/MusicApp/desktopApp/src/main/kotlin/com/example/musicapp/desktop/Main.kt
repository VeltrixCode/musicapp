package com.example.musicapp.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.example.musicapp.ui.MusicApp

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "MusicApp") {
        MusicApp()
    }
}
