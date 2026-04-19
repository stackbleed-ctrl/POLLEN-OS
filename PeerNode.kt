package com.stackbleedctrl.pollyn.swarm

data class PeerNode(
    val id: String,
    val name: String,
    val connected: Boolean = false
)
