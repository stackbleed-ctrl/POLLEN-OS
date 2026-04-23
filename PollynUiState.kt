package com.stackbleedctrl.pollyn

data class PollynUiState(
    val lastIntent: String = "",
    val lastDecision: String = "Waiting",
    val meshStatus: String = "Idle",
    val peerCount: Int = 0,
    val debugLines: List<String> = emptyList()
)