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
        sdk.brain.handleDecision { decision ->
            state = state.copy(lastDecision = decision.summary)
        }
        sdk.brain.handleMeshStatus { status ->
            state = state.copy(meshStatus = status)
        }
    }

    fun submitIntent(raw: String) {
        state = state.copy(lastIntent = raw)
        viewModelScope.launch {
            sdk.submitIntent(raw)
        }
    }

    fun meshPing() {
        viewModelScope.launch {
            sdk.meshPing()
        }
    }
}
