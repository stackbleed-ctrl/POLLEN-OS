package com.stackbleedctrl.pollyn.llm

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalLlmManager @Inject constructor(
    private val nano: GeminiNanoAdapter,
    private val fallback: RuleBasedFallbackLlm
) : LlmBackend {
    override val backendName: String
        get() = if (nano.isAvailable()) "gemini_nano" else fallback.backendName

    override suspend fun decide(prompt: String): String {
        return if (nano.isAvailable()) {
            nano.decide(prompt) ?: fallback.decide(prompt)
        } else {
            fallback.decide(prompt)
        }
    }
}
