package com.stackbleedctrl.pollen.core.model

data class BrainDecision(
    val type: DecisionType,
    val summary: String,
    val actionPayload: String = "",
    val routeToPeer: String? = null
)

enum class DecisionType {
    IGNORE,
    SUMMARIZE,
    OPEN_APP,
    REPLY,
    BLOCK_CALL,
    MESH_FORWARD,
    LOG_ONLY
}
