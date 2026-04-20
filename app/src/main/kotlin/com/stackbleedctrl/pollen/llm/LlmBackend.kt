package com.stackbleedctrl.pollen.llm

interface LlmBackend {
    suspend fun decide(prompt: String): String
    val backendName: String
}
