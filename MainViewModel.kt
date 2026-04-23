package com.stackbleedctrl.pollyn

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stackbleedctrl.pollyn.sdk.PollynSdk
import com.stackbleedctrl.pollyn.swarm.MeshEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    private val sdk: PollynSdk
) : ViewModel() {

    var state by mutableStateOf(PollynUiState())
        private set

    init {
        // Existing brain hooks
        sdk.brain.handleDecision { decision ->
            appendDebug("decision: ${decision.summary}")
            state = state.copy(lastDecision = decision.summary)
        }

        sdk.brain.handleMeshStatus { status ->
            appendDebug("brain mesh status: $status")
            state = state.copy(meshStatus = status)
        }

        // 🔥 NEW: listen to REAL mesh events
        viewModelScope.launch {
            sdk.mesh.events.collect { event ->
                when (event) {
                    is MeshEvent.Status -> {
                        appendDebug("[STATUS] ${event.tag}: ${event.detail}")
                    }
                    is MeshEvent.Error -> {
                        appendDebug("[ERROR] ${event.tag}: ${event.reason}")
                    }
                    is MeshEvent.PeerConnected -> {
                        appendDebug("[PEER CONNECTED] ${event.peer.displayName}")
                    }
                    is MeshEvent.PeerDisconnected -> {
                        appendDebug("[PEER DISCONNECTED] ${event.peer.displayName}")
                    }
                    is MeshEvent.MessageReceived -> {
                        appendDebug("[MSG] ${event.message.text}")
                    }
                    is MeshEvent.SendFailed -> {
                        appendDebug("[SEND FAIL] ${event.reason}")
                    }
                }
            }
        }

        // 🔥 NEW: observe peer count
        viewModelScope.launch {
            sdk.mesh.peers.collect { peers ->
                state = state.copy(peerCount = peers.size)
                appendDebug("peerCount=${peers.size}")
            }
        }
    }

    fun submitIntent(raw: String) {
        state = state.copy(lastIntent = raw)
        appendDebug("intent: $raw")

        viewModelScope.launch {
            sdk.submitIntent(raw)
        }
    }

    fun meshPing() {
        appendDebug("PING pressed")

        viewModelScope.launch {
            val sent = sdk.mesh.broadcast("ping")
            appendDebug("broadcast sent to $sent peers")
        }
    }

    // 🔥 Helper: keep last 20 debug lines
    private fun appendDebug(line: String) {
        val updated = (state.debugLines + line).takeLast(20)
        state = state.copy(debugLines = updated)
    }
}