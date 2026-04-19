package com.stackbleedctrl.pollyn.swarm

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionsClient
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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NearbyMeshCoordinator @Inject constructor(
    @ApplicationContext context: Context,
    private val trustManager: NodeTrustManager,
    private val tracer: PollynTracer
) {
    private val client: ConnectionsClient = Nearby.getConnectionsClient(context)
    private val serviceId = context.packageName
    private val localName = android.os.Build.MODEL ?: "android"
    private val peers = linkedMapOf<String, PeerNode>()

    fun start() {
        tracer.trace("mesh", "start advertise + discover")
        client.startAdvertising(localName, serviceId, lifecycle, AdvertisingOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build())
        client.startDiscovery(serviceId, discovery, DiscoveryOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build())
    }

    fun stop() {
        client.stopAllEndpoints()
        client.stopAdvertising()
        client.stopDiscovery()
    }

    fun peers(): List<PeerNode> = peers.values.toList()

    fun broadcast(text: String) {
        peers.keys.forEach { id ->
            client.sendPayload(id, Payload.fromBytes(text.toByteArray()))
        }
    }

    private val lifecycle = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            client.acceptConnection(endpointId, payloads)
            peers[endpointId] = PeerNode(endpointId, info.endpointName, connected = false)
        }
        override fun onConnectionResult(endpointId: String, result: com.google.android.gms.common.api.Status) {
            peers[endpointId] = peers[endpointId]?.copy(connected = result.isSuccess) ?: PeerNode(endpointId, endpointId, result.isSuccess)
            trustManager.notePeer(endpointId)
        }
        override fun onDisconnected(endpointId: String) {
            peers[endpointId] = peers[endpointId]?.copy(connected = false) ?: PeerNode(endpointId, endpointId, false)
        }
    }

    private val discovery = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            peers[endpointId] = PeerNode(endpointId, info.endpointName, connected = false)
            client.requestConnection(localName, endpointId, lifecycle)
        }
        override fun onEndpointLost(endpointId: String) {
            peers.remove(endpointId)
        }
    }

    private val payloads = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            val text = payload.asBytes()?.decodeToString() ?: return
            tracer.trace("mesh", "received from=$endpointId msg=$text")
            trustManager.notePeer(endpointId)
        }
        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) = Unit
    }
}
