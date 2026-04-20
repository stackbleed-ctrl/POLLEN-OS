package com.stackbleedctrl.pollen

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import com.stackbleedctrl.pollen.oslayer.PollenBrainService
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PollenApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ContextCompat.startForegroundService(this, Intent(this, PollenBrainService::class.java))
    }
}
