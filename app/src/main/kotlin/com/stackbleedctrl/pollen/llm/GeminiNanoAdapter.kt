package com.stackbleedctrl.pollen.llm

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reflection-first hook so unsupported devices still build.
 * Replace internals with a real AICore integration on supported phones.
 */
@Singleton
class GeminiNanoAdapter @Inject constructor() {
    fun isAvailable(): Boolean = false
    suspend fun decide(prompt: String): String? = null
}
