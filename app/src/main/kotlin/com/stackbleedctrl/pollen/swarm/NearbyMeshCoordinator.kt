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
import com.stackbleedctrl.pollen.identity.DeviceIdProvider
import com.stackbleedctrl.pollen.mesh.MeshCrypto
import com.stackbleedctrl.pollen.mesh.MeshPacket
import com.stackbleedctrl.pollen.mesh.MeshPacketType
import com.stackbleedctrl.pollen.oslayer.BrainEvent
import com.stackbleedctrl.pollen.oslayer.BrainEventBus
import com.stackbleedctrl.pollen.security.NodeTrustManager
import com.stackbleedctrl.pollen.tasks.AlphaTaskType
import com.stackbleedctrl.pollen.tasks.AlphaTaskEngine
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
    private val localIdentity = DeviceIdProvider.getIdentity(context)
    private val localName: String = "${localIdentity.displayName} · ${localIdentity.nodeId.takeLast(4)}"
    private val alphaTaskEngine = AlphaTaskEngine(context)
    private val peers: LinkedHashMap<String, PeerNode> = linkedMapOf()
    private val maxPacketAgeMs = 60_000L
    private val recentPacketNonces: LinkedHashMap<String, Long> = linkedMapOf()
    private val maxRecentPacketNonces = 200

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

    fun sendToPeer(endpointId: String, text: String): Boolean {
        val peer = peers[endpointId]

        if (peer?.connected != true) {
            emitMeshStatus("Target peer unavailable: ${peerLabel(endpointId)}")
            return false
        }

        client.sendPayload(endpointId, Payload.fromBytes(text.toByteArray()))
        emitMeshStatus("Payload sent directly to ${peerLabel(endpointId)}")
        return true
    }

    private fun isReplayPacket(packetNonce: String): Boolean {
        val now = System.currentTimeMillis()

        val expired = recentPacketNonces
            .filterValues { now - it > maxPacketAgeMs }
            .keys

        expired.forEach { recentPacketNonces.remove(it) }

        if (recentPacketNonces.containsKey(packetNonce)) {
            return true
        }

        recentPacketNonces[packetNonce] = now

        while (recentPacketNonces.size > maxRecentPacketNonces) {
            val oldestKey = recentPacketNonces.entries.firstOrNull()?.key ?: break
            recentPacketNonces.remove(oldestKey)
        }

        return false
    }

    private fun connectedPeerCount(): Int =
        peers.values.count { it.connected }

    private fun peerLabel(endpointId: String): String {
        val peerName = peers[endpointId]?.name
            ?.takeIf { it.isNotBlank() }

        return peerName ?: endpointId
    }

    private fun isSensitiveTask(packet: MeshPacket): Boolean {
        return packet.taskType == AlphaTaskType.LOCATION_SNAPSHOT.name
    }

    private fun sensitiveTaskSecurityFailure(
        packet: MeshPacket,
        endpointId: String
    ): String? {
        val taskName = packet.taskType ?: packet.type.name
        val peerName = peerLabel(endpointId)

        if (!packet.usesPeerKey()) {
            return "Sensitive task rejected: $taskName requires peer-key encryption · from=$peerName"
        }

        if (packet.senderLabel.isNullOrBlank()) {
            return "Sensitive task rejected: $taskName missing sender label · from=$peerName"
        }

        if (peers[endpointId]?.connected != true) {
            return "Sensitive task rejected: $taskName route not fresh/connected · from=$peerName"
        }

        if (!trustManager.trusted(endpointId)) {
            return "Sensitive task rejected: $taskName peer not trusted · from=$peerName"
        }

        return null
    }

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
            emitMeshStatus("POLLEN_PEER_LABEL|${info.endpointName}")
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
                emitMeshStatus("Connected node: ${peerLabel(endpointId)}")
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

            emitMeshStatus("Disconnected node: ${peerLabel(endpointId)}")
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
            emitMeshStatus("POLLEN_PEER_LABEL|${info.endpointName}")
            client.requestConnection(localName, endpointId, lifecycle)
        }

        override fun onEndpointLost(endpointId: String) {
            peers.remove(endpointId)
            emitMeshStatus("Lost node: ${peerLabel(endpointId)}")
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
                    val packet = MeshPacket.fromJson(message.payload)

                    if (packet == null) {
                        emitMeshStatus("ROUTE from ${message.fromNodeId}: ${message.payload}")
                        return
                    }

                    if (packet.usesPeerKey() && packet.senderLabel.isNullOrBlank()) {
                        emitMeshStatus(
                            "Packet rejected: peer-key packet missing sender label ${packet.taskType ?: packet.type.name}"
                        )
                        return
                    }

                    val inboundPeerKeyMaterial = if (packet.usesPeerKey()) {
                        packet.senderLabel?.let { senderLabel ->
                            MeshCrypto.peerKeyMaterial(localName, senderLabel)
                        }
                    } else {
                        null
                    }

                    val decryptedPacket = packet.decryptPayload(inboundPeerKeyMaterial)

                    if (decryptedPacket == null) {
                        emitMeshStatus(
                            "Packet rejected: decrypt failed ${packet.taskType ?: packet.type.name}"
                        )
                        return
                    }

                    val packetAgeMs = decryptedPacket.ageMs()
                    val integrityState = when {
                        !decryptedPacket.hasIntegrityTag() -> "missing"
                        decryptedPacket.integrityValid() -> "valid"
                        else -> "invalid"
                    }

                    val packetSender = decryptedPacket.senderLabel ?: decryptedPacket.fromNodeId

                    val encryptionState = when {
                        packet.usesPeerKey() -> "peer-key"
                        packet.isEncrypted() -> "alpha-fallback"
                        else -> "none"
                    }

                    emitMeshStatus(
                        "Packet security: ${decryptedPacket.taskType ?: decryptedPacket.type.name} from $packetSender · age=${packetAgeMs}ms · integrity=$integrityState · encryption=$encryptionState"
                    )

                    if (!decryptedPacket.integrityValid()) {
                        emitMeshStatus(
                            "Packet rejected: invalid integrity ${decryptedPacket.taskType ?: decryptedPacket.type.name}"
                        )
                        return
                    }

                    if (isReplayPacket(decryptedPacket.packetNonce)) {
                        emitMeshStatus(
                            "Packet rejected: replay ${decryptedPacket.taskType ?: decryptedPacket.type.name} · nonce=${decryptedPacket.packetNonce.takeLast(6)}"
                        )
                        return
                    }

                    if (decryptedPacket.isStale(maxPacketAgeMs)) {
                        emitMeshStatus(
                            "Packet rejected: stale ${decryptedPacket.taskType ?: decryptedPacket.type.name} · age=${packetAgeMs}ms"
                        )
                        return
                    }

                    when (decryptedPacket.type) {
                        MeshPacketType.TASK_REQUEST -> {
                            emitMeshStatus("TASK_REQUEST ${decryptedPacket.taskType} from ${decryptedPacket.fromNodeId}")

                            if (isSensitiveTask(decryptedPacket)) {
                                val failure = sensitiveTaskSecurityFailure(
                                    packet = packet,
                                    endpointId = endpointId
                                )

                                if (failure != null) {
                                    emitMeshStatus(failure)
                                    return
                                }

                                emitMeshStatus(
                                    "Sensitive task approved: ${decryptedPacket.taskType} · peer-key · trusted · fresh route"
                                )
                            }

                            val responsePeerKeyMaterial = decryptedPacket.senderLabel?.let { senderLabel ->
                                MeshCrypto.peerKeyMaterial(localName, senderLabel)
                            }

                            val result = alphaTaskEngine.handleTask(decryptedPacket)
                                .copy(senderLabel = localName)
                                .encryptPayload(responsePeerKeyMaterial)

                            val response = MeshMessage(
                                type = MeshMessageType.ROUTE,
                                fromNodeId = result.fromNodeId,
                                toNodeId = decryptedPacket.fromNodeId,
                                payload = result.toJson()
                            )

                            broadcast(response.encode())
                            emitMeshStatus("TASK_RESULT sent: ${result.taskType}")
                        }

                        MeshPacketType.TASK_RESULT -> {
                            if (isSensitiveTask(decryptedPacket) && !packet.usesPeerKey()) {
                                emitMeshStatus(
                                    "Sensitive result rejected: ${decryptedPacket.taskType} requires peer-key encryption"
                                )
                                return
                            }

                            if (isSensitiveTask(decryptedPacket) && decryptedPacket.senderLabel.isNullOrBlank()) {
                                emitMeshStatus(
                                    "Sensitive result rejected: ${decryptedPacket.taskType} missing sender label"
                                )
                                return
                            }

                            emitMeshStatus(
                                "Sensitive result accepted: ${decryptedPacket.taskType ?: decryptedPacket.type.name} · encryption=$encryptionState · integrity=$integrityState"
                            )

                            bus.tryEmit(BrainEvent.MeshStatus("POLLEN_TASK_RESULT|${decryptedPacket.toJson()}"))
                            emitMeshStatus("TASK_RESULT from ${decryptedPacket.fromNodeId}: ${decryptedPacket.payload}")
                        }

                        else -> {
                            emitMeshStatus("PACKET ${decryptedPacket.type} from ${decryptedPacket.fromNodeId}")
                        }
                    }
                }
            }
        }

        override fun onPayloadTransferUpdate(
            endpointId: String,
            update: PayloadTransferUpdate
        ) = Unit
    }
}