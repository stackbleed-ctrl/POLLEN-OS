package com.stackbleedctrl.pollen

import com.stackbleedctrl.pollen.identity.DeviceIdentity
import com.stackbleedctrl.pollen.tasks.AlphaTaskState

data class PollenUiState(
    val lastIntent: String = "",
    val lastDecision: String = "Waiting",
    val meshStatus: String = "Idle",
    val peerCount: Int = 0,
    val debugLines: List<String> = emptyList(),

    val identity: DeviceIdentity? = null,
    val tasks: List<AlphaTaskState> = emptyList(),
    val eventLog: List<String> = emptyList()
)
