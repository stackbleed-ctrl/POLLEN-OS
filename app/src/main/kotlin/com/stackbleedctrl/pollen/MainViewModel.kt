package com.stackbleedctrl.pollen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stackbleedctrl.pollen.sdk.PollenSdk
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    private val sdk: PollenSdk
) : ViewModel() {

    var state by mutableStateOf(PollenUiState())
        private set

    init {
        appendDebug("MainViewModel started")

        sdk.brain.handleDecision { decision ->
            appendDebug("decision: ${decision.summary}")
            state = state.copy(lastDecision = decision.summary)
        }

        sdk.brain.handleMeshStatus { status ->
            appendDebug("brain mesh status: $status")
            state = state.copy(meshStatus = status)
        }

        appendDebug("Waiting for Start brain")
    }

    fun submitIntent(raw: String) {
        state = state.copy(lastIntent = raw)
        appendDebug("intent pressed: $raw")

        viewModelScope.launch {
            sdk.submitIntent(raw)
            appendDebug("intent submitted")
        }
    }
    fun startBrain() {
    appendDebug("START BRAIN pressed")
    state = state.copy(meshStatus = "Starting brain service...")
}

fun permissionsReady() {
    appendDebug("permissions ready")
    state = state.copy(meshStatus = "Permissions ready")
}

fun permissionsDenied(denied: String) {
    appendDebug("permissions denied: $denied")
    state = state.copy(meshStatus = "Permissions missing")
}
    fun meshPing() {
        appendDebug("PING pressed")

        viewModelScope.launch {
            sdk.meshPing()
            appendDebug("sdk.meshPing called")
        }
    }

    private fun appendDebug(line: String) {
        val updated = (state.debugLines + line).takeLast(30)
        state = state.copy(debugLines = updated)
    }
}