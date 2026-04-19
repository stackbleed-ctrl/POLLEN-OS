package com.stackbleedctrl.pollyn

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import com.stackbleedctrl.pollyn.oslayer.PollynBrainService
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class PollynApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        ContextCompat.startForegroundService(this, Intent(this, PollynBrainService::class.java))
    }
}
