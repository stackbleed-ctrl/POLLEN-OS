package com.stackbleedctrl.pollyn

data class PollynUiState(
    val lastIntent: String = "",
    val lastDecision: String = "Waiting",
    val meshStatus: String = "Idle"
)
