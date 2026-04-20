package com.stackbleedctrl.pollen.llm

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RuleBasedFallbackLlm @Inject constructor() : LlmBackend {
    override val backendName: String = "rule_fallback"

    override suspend fun decide(prompt: String): String {
        val p = prompt.lowercase()
        return when {
            "open maps" in p -> "OPEN_APP:com.google.android.apps.maps"
            "block" in p && "call" in p -> "BLOCK_CALL"
            "mesh" in p || "nearby" in p -> "MESH_FORWARD:hello-nearby"
            "summarize" in p || "summary" in p -> "SUMMARIZE"
            else -> "LOG_ONLY"
        }
    }
}
