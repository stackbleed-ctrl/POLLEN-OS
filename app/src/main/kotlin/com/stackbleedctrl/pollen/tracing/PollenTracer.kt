package com.stackbleedctrl.pollen.tracing

import android.util.Log
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PollenTracer @Inject constructor() {
    fun trace(name: String, message: String) {
        Log.d("Pollen", "[$name] $message")
    }
    fun newTraceId(): String = UUID.randomUUID().toString()
}
