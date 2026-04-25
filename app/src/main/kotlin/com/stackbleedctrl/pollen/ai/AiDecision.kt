package com.stackbleedctrl.pollen.ai

data class AiDecision(
    val summary: String,
    val confidence: Float,
    val recommendedAction: AiRecommendedAction,
    val reason: String,
    val createdAt: Long = System.currentTimeMillis()
)

enum class AiRecommendedAction {
    OBSERVE,
    START_BRAIN,
    SEND_PING,
    SEND_DEVICE_STATUS,
    MARK_PEER_STABLE,
    TRUST_REVIEW,
    REVIEW_SENSITIVE_TASK,
    RETRY_OR_RECONNECT,
    EXPORT_TESTER_LOG,
    WARN_USER
}
