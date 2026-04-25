package com.stackbleedctrl.pollen

import com.stackbleedctrl.pollen.identity.DeviceIdentity
import com.stackbleedctrl.pollen.tasks.AlphaTaskState

data class PollenUiState(
    val lastIntent: String = "",
    val lastDecision: String = "Waiting",
    val meshStatus: String = "Idle",
    val peerCount: Int = 0,
    val debugLines: List<String> = emptyList(),

    val lastPeerLabel: String = "",
    val trustedPeerLabel: String = "",

    val aiSummary: String = "AI waiting for mesh events",
    val aiRecommendedAction: String = "OBSERVE",
    val aiConfidence: Float = 0f,
    val aiHealthScore: Int = 60,

    val peerProtocolVersion: String = "Unknown",
    val peerSupportedTasks: String = "Unknown",
    val compatibilityStatus: String = "Not checked",

    val demoSequenceRunning: Boolean = false,
    val demoSequenceStep: Int = 0,
    val demoSequenceTotal: Int = 5,

    val identity: DeviceIdentity? = null,
    val tasks: List<AlphaTaskState> = emptyList(),
    val eventLog: List<String> = emptyList(),

    val fullTestRunning: Boolean = false,
    val fullTestStartedAt: Long? = null,
    val fullTestCompletedAt: Long? = null,

    val rangeProbeRunning: Boolean = false,
    val rangeProbeSent: Int = 0,
    val rangeProbeTotal: Int = 12
)
