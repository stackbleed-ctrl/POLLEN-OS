package com.stackbleedctrl.pollyn.swarm

import com.stackbleedctrl.pollyn.tracing.PollynTracer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SwarmCoordinator @Inject constructor(
    private val mesh: NearbyMeshCoordinator,
    private val routes: SmartRoutingTable,
    private val tracer: PollynTracer
) {
    fun start() = mesh.start()
    fun stop() = mesh.stop()

    fun meshPing() {
        tracer.trace("swarm", "ping peers")
        mesh.broadcast("pollyn_ping")
    }

    fun forwardIfPossible(payload: String): Boolean {
        val peer = routes.choosePeer(mesh.peers()) ?: return false
        tracer.trace("swarm", "forward to ${peer.id}")
        mesh.broadcast(payload)
        return true
    }
}
