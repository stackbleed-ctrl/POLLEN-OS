package com.stackbleedctrl.pollyn.tracing

import android.util.Log
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PollynTracer @Inject constructor() {
    fun trace(name: String, message: String) {
        Log.d("Pollyn", "[$name] $message")
    }
    fun newTraceId(): String = UUID.randomUUID().toString()
}
