package com.stackbleedctrl.pollen.swarm

import com.stackbleedctrl.pollen.tracing.PollenTracer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SwarmCoordinator @Inject constructor(
    private val mesh: NearbyMeshCoordinator,
    private val routes: SmartRoutingTable,
    private val tracer: PollenTracer,
    private val identity: NodeIdentity
) {
    fun start() = mesh.start()
    fun stop() = mesh.stop()

    fun meshPing() {
        val message = MeshMessage(
            type = MeshMessageType.PING,
            fromNodeId = identity.nodeId,
            payload = "pollen_ping"
        )

        tracer.trace("swarm", "ping peers from=${identity.nodeId}")
        mesh.broadcast(message.encode())
    }

    fun sendIntentToPeers(rawIntent: String) {
        val message = MeshMessage(
            type = MeshMessageType.INTENT,
            fromNodeId = identity.nodeId,
            payload = rawIntent
        )

        tracer.trace("swarm", "send intent from=${identity.nodeId} intent=$rawIntent")
        mesh.broadcast(message.encode())
    }

    fun sendMeshPacket(packetJson: String) {
        val message = MeshMessage(
            type = MeshMessageType.ROUTE,
            fromNodeId = identity.nodeId,
            payload = packetJson
        )

        tracer.trace("swarm", "send mesh packet from=${identity.nodeId}")
        mesh.broadcast(message.encode())
    }

    fun forwardIfPossible(payload: String): Boolean {
        val peer = routes.choosePeer(mesh.peers()) ?: return false

        val message = MeshMessage(
            type = MeshMessageType.ROUTE,
            fromNodeId = identity.nodeId,
            toNodeId = peer.id,
            payload = payload
        )

        tracer.trace("swarm", "forward to ${peer.id}")
        mesh.broadcast(message.encode())
        return true
    }
}