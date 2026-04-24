package com.stackbleedctrl.pollen.swarm

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import com.stackbleedctrl.pollen.oslayer.BrainEvent
import com.stackbleedctrl.pollen.oslayer.BrainEventBus
import com.stackbleedctrl.pollen.security.NodeTrustManager
import com.stackbleedctrl.pollen.tracing.PollenTracer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NearbyMeshCoordinator @Inject constructor(
    @ApplicationContext context: Context,
    private val trustManager: NodeTrustManager,
    private val tracer: PollenTracer,
    private val bus: BrainEventBus
) {
    private val client: ConnectionsClient = Nearby.getConnectionsClient(context)
    private val serviceId: String = context.packageName
    private val localName: String = android.os.Build.MODEL ?: "android"
    private val peers: LinkedHashMap<String, PeerNode> = linkedMapOf()

    fun start() {
        tracer.trace("mesh", "start advertise + discover")
        emitMeshStatus("Starting advertise + discover")

        val advertisingTask = client.startAdvertising(
            localName,
            serviceId,
            lifecycle,
            AdvertisingOptions.Builder()
                .setStrategy(Strategy.P2P_CLUSTER)
                .build()
        )

        emitMeshStatus("Advertising request submitted")

        advertisingTask
            .addOnSuccessListener {
                emitMeshStatus("Advertising started")
            }
            .addOnFailureListener { error ->
                emitMeshStatus("Advertising failed: ${error.message ?: error.javaClass.simpleName}")
            }
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    emitMeshStatus("Advertising complete: success")
                } else {
                    val message = task.exception?.message
                        ?: task.exception?.javaClass?.simpleName
                        ?: "unknown"
                    emitMeshStatus("Advertising complete: failed $message")
                }
            }

        val discoveryTask = client.startDiscovery(
            serviceId,
            discovery,
            DiscoveryOptions.Builder()
                .setStrategy(Strategy.P2P_CLUSTER)
                .build()
        )

        emitMeshStatus("Discovery request submitted")

        discoveryTask
            .addOnSuccessListener {
                emitMeshStatus("Discovery started")
            }
            .addOnFailureListener { error ->
                emitMeshStatus("Discovery failed: ${error.message ?: error.javaClass.simpleName}")
            }
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    emitMeshStatus("Discovery complete: success")
                } else {
                    val message = task.exception?.message
                        ?: task.exception?.javaClass?.simpleName
                        ?: "unknown"
                    emitMeshStatus("Discovery complete: failed $message")
                }
            }
    }

    fun stop() {
        client.stopAllEndpoints()
        client.stopAdvertising()
        client.stopDiscovery()
        peers.clear()
        emitMeshStatus("Mesh stopped")
    }

    fun peers(): List<PeerNode> = peers.values.toList()

    fun broadcast(text: String) {
        val connectedPeers = peers.filterValues { it.connected }

        if (connectedPeers.isEmpty()) {
            emitMeshStatus("No connected peers to send payload")
            return
        }

        connectedPeers.keys.forEach { endpointId ->
            client.sendPayload(endpointId, Payload.fromBytes(text.toByteArray()))
        }

        emitMeshStatus("Payload sent to ${connectedPeers.size} peer(s)")
    }

    private fun connectedPeerCount(): Int =
        peers.values.count { it.connected }

    private fun emitMeshStatus(text: String) {
        tracer.trace("mesh", text)
        bus.tryEmit(BrainEvent.MeshStatus(text))
        bus.tryEmit(BrainEvent.PeerCountChanged(connectedPeerCount()))
    }

    private val lifecycle = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            peers[endpointId] = PeerNode(
                id = endpointId,
                name = info.endpointName,
                connected = false
            )

            emitMeshStatus("Connection initiated: ${info.endpointName}")
            client.acceptConnection(endpointId, payloads)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            val connected = result.status.isSuccess

            peers[endpointId] =
                peers[endpointId]?.copy(connected = connected)
                    ?: PeerNode(
                        id = endpointId,
                        name = endpointId,
                        connected = connected
                    )

            if (connected) {
                trustManager.notePeer(endpointId)
                emitMeshStatus("Connected node: $endpointId")
            } else {
                emitMeshStatus("Connection failed: ${result.status.statusCode}")
            }
        }

        override fun onDisconnected(endpointId: String) {
            peers[endpointId] =
                peers[endpointId]?.copy(connected = false)
                    ?: PeerNode(
                        id = endpointId,
                        name = endpointId,
                        connected = false
                    )

            emitMeshStatus("Disconnected node: $endpointId")
        }
    }

    private val discovery = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            peers[endpointId] = PeerNode(
                id = endpointId,
                name = info.endpointName,
                connected = false
            )

            emitMeshStatus("Found node: ${info.endpointName}")
            client.requestConnection(localName, endpointId, lifecycle)
        }

        override fun onEndpointLost(endpointId: String) {
            peers.remove(endpointId)
            emitMeshStatus("Lost node: $endpointId")
        }
    }

    private val payloads = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            val text = payload.asBytes()?.decodeToString() ?: return

            tracer.trace("mesh", "received from=$endpointId msg=$text")
            trustManager.notePeer(endpointId)

            val message = MeshMessage.decode(text)

            if (message == null) {
                emitMeshStatus("Received raw from $endpointId: $text")
                return
            }

            when (message.type) {
                MeshMessageType.HELLO -> {
                    emitMeshStatus("HELLO from ${message.fromNodeId}")
                }

                MeshMessageType.PING -> {
                    emitMeshStatus("PING from ${message.fromNodeId}: ${message.payload}")
                }

                MeshMessageType.INTENT -> {
                    emitMeshStatus("INTENT from ${message.fromNodeId}: ${message.payload}")
                }

                MeshMessageType.ACK -> {
                    emitMeshStatus("ACK from ${message.fromNodeId}: ${message.payload}")
                }

                MeshMessageType.LOG -> {
                    emitMeshStatus("LOG from ${message.fromNodeId}: ${message.payload}")
                }

                MeshMessageType.ROUTE -> {
                    emitMeshStatus("ROUTE from ${message.fromNodeId}: ${message.payload}")
                }
            }
        }

        override fun onPayloadTransferUpdate(
            endpointId: String,
            update: PayloadTransferUpdate
        ) = Unit
    }
}