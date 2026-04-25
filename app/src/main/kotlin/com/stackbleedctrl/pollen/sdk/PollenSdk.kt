package com.stackbleedctrl.pollen.sdk

import com.stackbleedctrl.pollen.core.model.BrainDecision
import com.stackbleedctrl.pollen.core.model.PhoneEvent
import com.stackbleedctrl.pollen.oslayer.BrainEvent
import com.stackbleedctrl.pollen.oslayer.BrainEventBus
import com.stackbleedctrl.pollen.swarm.SwarmCoordinator
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@Singleton
class PollenSdk @Inject constructor(
    private val bus: BrainEventBus,
    private val swarm: SwarmCoordinator
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val brain = BrainHandle()

    inner class BrainHandle {
        private var decisionHandler: ((BrainDecision) -> Unit)? = null
        private var meshHandler: ((String) -> Unit)? = null
        private var peerCountHandler: ((Int) -> Unit)? = null

        fun handleDecision(block: (BrainDecision) -> Unit) {
            decisionHandler = block
        }

        fun handleMeshStatus(block: (String) -> Unit) {
            meshHandler = block
        }

        fun handlePeerCount(block: (Int) -> Unit) {
            peerCountHandler = block
        }

        init {
            bus.events.onEach { event ->
                when (event) {
                    is BrainEvent.DecisionMade -> decisionHandler?.invoke(event.decision)
                    is BrainEvent.MeshStatus -> meshHandler?.invoke(event.text)
                    is BrainEvent.PeerCountChanged -> peerCountHandler?.invoke(event.count)
                    else -> Unit
                }
            }.launchIn(scope)
        }
    }

    suspend fun submitIntent(raw: String) {
        bus.emit(BrainEvent.InputEvent(PhoneEvent.UserIntent(raw)))
    }
    suspend fun sendIntentToPeers(raw: String) {
    swarm.sendIntentToPeers(raw)
    bus.emit(BrainEvent.MeshStatus("Intent sent to mesh peers"))
}
    suspend fun meshPing() {
        swarm.meshPing()
        bus.emit(BrainEvent.MeshStatus("Mesh ping sent"))
    }

    suspend fun sendMeshPacket(packetJson: String) {
        swarm.sendMeshPacket(packetJson)
        bus.emit(BrainEvent.MeshStatus("Mesh task packet sent"))
    }

    suspend fun sendMeshPacketToBestPeer(packetJson: String): Boolean {
        val sent = swarm.sendMeshPacketToBestPeer(packetJson)

        if (sent) {
            bus.emit(BrainEvent.MeshStatus("Targeted mesh task packet sent"))
        } else {
            bus.emit(BrainEvent.MeshStatus("No target peer available for mesh task"))
        }

        return sent
    }
}