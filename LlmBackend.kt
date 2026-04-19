package com.stackbleedctrl.pollyn.llm

interface LlmBackend {
    suspend fun decide(prompt: String): String
    val backendName: String
}
