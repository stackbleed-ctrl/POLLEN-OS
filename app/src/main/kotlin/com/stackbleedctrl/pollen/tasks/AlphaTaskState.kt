package com.stackbleedctrl.pollen.tasks

data class AlphaTaskState(
    val taskId: String,
    val taskType: String,
    val targetNodeId: String? = null,
    val targetPeerLabel: String? = null,
    val status: TaskStatus,
    val result: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val latencyMs: Long? = null
)

enum class TaskStatus {
    PENDING,
    COMPLETED,
    FAILED
}
