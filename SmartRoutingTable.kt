package com.stackbleedctrl.pollyn.swarm

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmartRoutingTable @Inject constructor() {
    fun choosePeer(peers: Collection<PeerNode>): PeerNode? = peers.firstOrNull { it.connected } ?: peers.firstOrNull()
}
