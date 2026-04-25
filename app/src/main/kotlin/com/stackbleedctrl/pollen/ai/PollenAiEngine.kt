package com.stackbleedctrl.pollen.ai

import kotlin.math.min

class PollenAiEngine {

    fun evaluate(signal: AiSignal): AiDecision {
        return when (signal.type) {
            AiSignalType.BRAIN_STARTED -> AiDecision(
                summary = "Brain layer started.",
                confidence = 0.95f,
                recommendedAction = AiRecommendedAction.OBSERVE,
                reason = "The local brain is online and ready to monitor mesh events."
            )

            AiSignalType.PERMISSIONS_READY -> AiDecision(
                summary = "Required permissions are ready.",
                confidence = 0.9f,
                recommendedAction = AiRecommendedAction.START_BRAIN,
                reason = "The app has enough permission access to begin mesh testing."
            )

            AiSignalType.PERMISSIONS_DENIED -> AiDecision(
                summary = "Some permissions are missing.",
                confidence = 0.88f,
                recommendedAction = AiRecommendedAction.WARN_USER,
                reason = "Missing permissions may limit discovery, location, notifications, or nearby connectivity."
            )

            AiSignalType.PEER_COUNT_CHANGED -> {
                val peers = signal.peerCount
                AiDecision(
                    summary = if (peers > 0) "Mesh peer detected: $peers visible." else "No mesh peers visible.",
                    confidence = if (peers > 0) 0.92f else 0.76f,
                    recommendedAction = if (peers > 0) AiRecommendedAction.SEND_PING else AiRecommendedAction.OBSERVE,
                    reason = if (peers > 0) {
                        "A visible peer should be checked with PING or DEVICE_STATUS."
                    } else {
                        "The mesh is still searching; wait for discovery before routing tasks."
                    }
                )
            }

            AiSignalType.MESH_STATUS_CHANGED -> AiDecision(
                summary = "Mesh status updated: ${signal.meshStatus.ifBlank { signal.message }}",
                confidence = 0.82f,
                recommendedAction = AiRecommendedAction.OBSERVE,
                reason = "Mesh status changed and should be reflected in the event feed."
            )

            AiSignalType.TASK_CREATED -> AiDecision(
                summary = "Task created: ${signal.taskType ?: "unknown task"}.",
                confidence = 0.86f,
                recommendedAction = AiRecommendedAction.OBSERVE,
                reason = "The task is now pending and should wait for a result or timeout."
            )

            AiSignalType.TASK_SENT -> AiDecision(
                summary = "Task sent: ${signal.taskType ?: "unknown task"}.",
                confidence = 0.88f,
                recommendedAction = AiRecommendedAction.OBSERVE,
                reason = "The mesh accepted the task for routing; wait for TASK_RESULT."
            )

            AiSignalType.TASK_RESULT -> {
                val success = signal.success == true
                AiDecision(
                    summary = if (success) {
                        "Task succeeded: ${signal.taskType ?: "task"}${signal.payload?.let { " → $it" } ?: ""}"
                    } else {
                        "Task failed: ${signal.taskType ?: "task"}"
                    },
                    confidence = if (success) 0.94f else 0.68f,
                    recommendedAction = if (success) {
                        AiRecommendedAction.MARK_PEER_STABLE
                    } else {
                        AiRecommendedAction.RETRY_OR_RECONNECT
                    },
                    reason = if (success) {
                        "A structured result was returned${signal.latencyMs?.let { " in ${it}ms" } ?: ""}."
                    } else {
                        "The result reported failure or did not provide a successful payload."
                    }
                )
            }

            AiSignalType.TASK_TIMEOUT -> AiDecision(
                summary = "Task timed out: ${signal.taskType ?: "unknown task"}.",
                confidence = 0.84f,
                recommendedAction = AiRecommendedAction.RETRY_OR_RECONNECT,
                reason = "A pending task did not return before timeout; retry or reconnect should be considered."
            )

            AiSignalType.SENSITIVE_TASK_NOTICE -> AiDecision(
                summary = "Sensitive task notice: ${signal.taskType ?: "sensitive task"}.",
                confidence = 0.9f,
                recommendedAction = if (signal.trustedPeerLabel.isBlank()) {
                    AiRecommendedAction.TRUST_REVIEW
                } else {
                    AiRecommendedAction.REVIEW_SENSITIVE_TASK
                },
                reason = if (signal.trustedPeerLabel.isBlank()) {
                    "No trusted peer is set. Sensitive mesh tasks should require manual review."
                } else {
                    "A trusted peer is set, but sensitive task results should still be reviewed."
                }
            )

            AiSignalType.TRUST_CHANGED -> AiDecision(
                summary = if (signal.trustedPeerLabel.isBlank()) "Trusted peer cleared." else "Trusted peer set.",
                confidence = 0.9f,
                recommendedAction = AiRecommendedAction.OBSERVE,
                reason = "Trust state changed and will affect future sensitive task policy."
            )

            AiSignalType.ERROR -> AiDecision(
                summary = "System warning: ${signal.message}",
                confidence = 0.7f,
                recommendedAction = AiRecommendedAction.WARN_USER,
                reason = "An error or warning event was detected."
            )
        }
    }

    fun meshHealthScore(peerCount: Int, failedTasks: Int, pendingTasks: Int): Int {
        val base = 60
        val peerBoost = min(25, peerCount * 10)
        val failurePenalty = min(30, failedTasks * 10)
        val pendingPenalty = min(15, pendingTasks * 3)
        return (base + peerBoost - failurePenalty - pendingPenalty).coerceIn(0, 100)
    }
}
