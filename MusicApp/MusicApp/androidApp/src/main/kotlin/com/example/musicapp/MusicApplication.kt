package com.example.musicapp

import android.app.Application
import com.example.musicapp.platform.AndroidBridge

class MusicApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidBridge.appContext = this
    }
}
