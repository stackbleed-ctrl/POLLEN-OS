package com.stackbleedctrl.pollyn.swarm

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import com.stackbleedctrl.pollyn.security.NodeTrustManager
import com.stackbleedctrl.pollyn.tracing.PollynTracer
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PeerState {
    DISCOVERED,
    CONNECTING,
    CONNECTED,
    DISCONNECTED
}

data class PeerNode(
    val endpointId: String,
    val displayName: String,
    val state: PeerState = PeerState.DISCOVERED
) {
    val isConnected: Boolean get() = state == PeerState.CONNECTED
}

data class MeshMessage(
    val fromEndpointId: String,
    val text: String,
    val timestampMs: Long = System.currentTimeMillis()
)

sealed class MeshEvent {
    data class PeerConnected(val peer: PeerNode) : MeshEvent()
    data class PeerDisconnected(val peer: PeerNode) : MeshEvent()
    data class MessageReceived(val message: MeshMessage) : MeshEvent()
    data class SendFailed(val endpointId: String, val reason: String) : MeshEvent()
    data class Error(val tag: String, val reason: String) : MeshEvent()
    data class Status(val tag: String, val detail: String) : MeshEvent()
}

@Singleton
class NearbyMeshCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trustManager: NodeTrustManager,
    private val tracer: PollynTracer
) {
    private val client: ConnectionsClient = Nearby.getConnectionsClient(context)
    private val serviceId = context.packageName
    private val localName = android.os.Build.MODEL.takeIf { it.isNotBlank() } ?: "android-node"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val peerMap = ConcurrentHashMap<String, PeerNode>()
    private val pendingConnections = ConcurrentHashMap.newKeySet<String>()
    private val isRunning = AtomicBoolean(false)

    private val _peers = MutableStateFlow<List<PeerNode>>(emptyList())
    val peers: StateFlow<List<PeerNode>> = _peers.asStateFlow()

    private val _events = MutableSharedFlow<MeshEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<MeshEvent> = _events.asSharedFlow()

    fun start() {
        if (!isRunning.compareAndSet(false, true)) {
            tracer.trace("mesh", "start ignored already running")
            emit(MeshEvent.Status("start", "already running"))
            return
        }

        tracer.trace("mesh", "start advertising + discovery as '$localName'")
        emit(MeshEvent.Status("start", "starting advertising + discovery"))

        val advertisingOptions = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_CLUSTER)
            .build()

        val discoveryOptions = DiscoveryOptions.Builder()
            .setStrategy(Strategy.P2P_CLUSTER)
            .build()

        client.startAdvertising(localName, serviceId, lifecycleCallback, advertisingOptions)
            .addOnSuccessListener {
                tracer.trace("mesh", "advertising started localName=$localName serviceId=$serviceId")
                emit(MeshEvent.Status("advertising", "started"))
            }
            .addOnFailureListener { e ->
                tracer.trace("mesh", "advertising failed: ${e.message}")
                emit(MeshEvent.Error("advertising", e.message ?: "unknown"))
            }

        client.startDiscovery(serviceId, discoveryCallback, discoveryOptions)
            .addOnSuccessListener {
                tracer.trace("mesh", "discovery started serviceId=$serviceId")
                emit(MeshEvent.Status("discovery", "started"))
            }
            .addOnFailureListener { e ->
                tracer.trace("mesh", "discovery failed: ${e.message}")
                emit(MeshEvent.Error("discovery", e.message ?: "unknown"))
            }
    }

    fun stop() {
        tracer.trace("mesh", "stop")
        isRunning.set(false)
        client.stopAdvertising()
        client.stopDiscovery()
        client.stopAllEndpoints()
        peerMap.clear()
        pendingConnections.clear()
        _peers.value = emptyList()
        emit(MeshEvent.Status("stop", "mesh stopped"))
    }

    fun shutdown() {
        stop()
        scope.cancel()
    }

    fun broadcast(text: String): Int {
        val targets = peerMap.values.filter { it.isConnected }
        tracer.trace("mesh", "broadcast msg=$text peerCount=${targets.size}")

        if (targets.isEmpty()) {
            emit(MeshEvent.Error("broadcast", "no connected peers"))
            return 0
        }

        targets.forEach { peer ->
            client.sendPayload(peer.endpointId, Payload.fromBytes(text.toByteArray(Charsets.UTF_8)))
                .addOnSuccessListener {
                    tracer.trace("mesh", "send success to=${peer.endpointId}")
                }
                .addOnFailureListener { e ->
                    tracer.trace("mesh", "send failed to=${peer.endpointId} err=${e.message}")
                    emit(MeshEvent.SendFailed(peer.endpointId, e.message ?: "unknown"))
                }
        }

        return targets.size
    }

    fun sendTo(endpointId: String, text: String) {
        val peer = peerMap[endpointId]
        if (peer == null || !peer.isConnected) {
            tracer.trace("mesh", "sendTo skipped endpoint=$endpointId not connected")
            emit(MeshEvent.SendFailed(endpointId, "peer not connected"))
            return
        }

        client.sendPayload(endpointId, Payload.fromBytes(text.toByteArray(Charsets.UTF_8)))
            .addOnSuccessListener {
                tracer.trace("mesh", "sendTo success endpoint=$endpointId")
            }
            .addOnFailureListener { e ->
                tracer.trace("mesh", "sendTo failed endpoint=$endpointId err=${e.message}")
                emit(MeshEvent.SendFailed(endpointId, e.message ?: "unknown"))
            }
    }

    fun connectedPeers(): List<PeerNode> = peerMap.values.filter { it.isConnected }

    fun peerCount(): Int = peerMap.size

    private val lifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            tracer.trace("mesh", "connection initiated from=$endpointId name=${info.endpointName}")
            upsertPeer(endpointId, info.endpointName, PeerState.CONNECTING)

            client.acceptConnection(endpointId, payloadCallback)
                .addOnSuccessListener {
                    tracer.trace("mesh", "acceptConnection success endpoint=$endpointId")
                }
                .addOnFailureListener { e ->
                    tracer.trace("mesh", "acceptConnection failed endpoint=$endpointId err=${e.message}")
                    pendingConnections.remove(endpointId)
                    upsertPeer(endpointId, info.endpointName, PeerState.DISCONNECTED)
                    emit(MeshEvent.Error("acceptConnection", e.message ?: "unknown"))
                }
        }

        override fun onConnectionResult(endpointId: String, resolution: ConnectionResolution) {
            val name = peerMap[endpointId]?.displayName ?: endpointId
            pendingConnections.remove(endpointId)

            when (resolution.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    tracer.trace("mesh", "connected endpointId=$endpointId")
                    val peer = upsertPeer(endpointId, name, PeerState.CONNECTED)
                    trustManager.notePeer(endpointId)
                    emit(MeshEvent.PeerConnected(peer))
                }

                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    tracer.trace("mesh", "connection rejected endpointId=$endpointId")
                    upsertPeer(endpointId, name, PeerState.DISCONNECTED)
                    emit(MeshEvent.Error("connect", "connection rejected"))
                }

                else -> {
                    val reason = resolution.status.statusMessage ?: "code ${resolution.status.statusCode}"
                    tracer.trace("mesh", "connection failed endpointId=$endpointId reason=$reason")
                    upsertPeer(endpointId, name, PeerState.DISCONNECTED)
                    emit(MeshEvent.Error("connect", reason))
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            tracer.trace("mesh", "disconnected endpointId=$endpointId")
            pendingConnections.remove(endpointId)
            val peer = upsertPeer(
                endpointId,
                peerMap[endpointId]?.displayName ?: endpointId,
                PeerState.DISCONNECTED
            )
            emit(MeshEvent.PeerDisconnected(peer))
        }
    }

    private val discoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            tracer.trace("mesh", "endpoint found id=$endpointId name=${info.endpointName}")

            if (peerMap[endpointId]?.isConnected == true || pendingConnections.contains(endpointId)) {
                tracer.trace("mesh", "skip duplicate connection attempt endpoint=$endpointId")
                return
            }

            upsertPeer(endpointId, info.endpointName, PeerState.DISCOVERED)
            pendingConnections.add(endpointId)

            client.requestConnection(localName, endpointId, lifecycleCallback)
                .addOnSuccessListener {
                    tracer.trace("mesh", "requestConnection success endpoint=$endpointId")
                }
                .addOnFailureListener { e ->
                    tracer.trace("mesh", "requestConnection failed endpoint=$endpointId err=${e.message}")
                    pendingConnections.remove(endpointId)
                    upsertPeer(endpointId, info.endpointName, PeerState.DISCONNECTED)
                    emit(MeshEvent.Error("requestConnection", e.message ?: "unknown"))
                }
        }

        override fun onEndpointLost(endpointId: String) {
            tracer.trace("mesh", "endpoint lost id=$endpointId")
            pendingConnections.remove(endpointId)
            peerMap.remove(endpointId)
            refreshPeerFlow()
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            val bytes = payload.asBytes() ?: run {
                tracer.trace("mesh", "non-bytes payload ignored from=$endpointId")
                return
            }

            val text = bytes.decodeToString()
            tracer.trace("mesh", "payload received from=$endpointId len=${bytes.size}")
            trustManager.notePeer(endpointId)
            emit(MeshEvent.MessageReceived(MeshMessage(endpointId, text)))
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            if (update.status == PayloadTransferUpdate.Status.FAILURE) {
                tracer.trace("mesh", "payload transfer failed endpoint=$endpointId")
                emit(MeshEvent.Error("payloadTransfer", "failed for $endpointId"))
            }
        }
    }

    private fun upsertPeer(endpointId: String, name: String, state: PeerState): PeerNode {
        val peer = PeerNode(endpointId, name, state)
        peerMap[endpointId] = peer
        refreshPeerFlow()
        return peer
    }

    private fun refreshPeerFlow() {
        _peers.update { peerMap.values.toList() }
    }

    private fun emit(event: MeshEvent) {
        scope.launch {
            _events.emit(event)
        }
    }
}