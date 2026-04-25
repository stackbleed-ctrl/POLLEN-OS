package com.stackbleedctrl.pollen.ai

data class AiSignal(
    val type: AiSignalType,
    val message: String,
    val peerCount: Int = 0,
    val meshStatus: String = "",
    val trustedPeerLabel: String = "",
    val taskType: String? = null,
    val payload: String? = null,
    val success: Boolean? = null,
    val latencyMs: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class AiSignalType {
    BRAIN_STARTED,
    PERMISSIONS_READY,
    PERMISSIONS_DENIED,
    PEER_COUNT_CHANGED,
    MESH_STATUS_CHANGED,
    TASK_CREATED,
    TASK_SENT,
    TASK_RESULT,
    TASK_TIMEOUT,
    SENSITIVE_TASK_NOTICE,
    TRUST_CHANGED,
    ERROR
}
