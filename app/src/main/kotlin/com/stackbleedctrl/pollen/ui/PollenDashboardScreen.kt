package com.stackbleedctrl.pollen.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stackbleedctrl.pollen.PollenUiState

@Composable
fun PollenDashboardScreen(
    state: PollenUiState,
    onStartService: () -> Unit,
    onSubmitIntent: (String) -> Unit,
    onMeshPing: () -> Unit
) {
    val input = remember { mutableStateOf("summarize my notifications") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text("🔥 REAL BUILD CONFIRMED", style = MaterialTheme.typography.headlineMedium)
        Text("If you see this, you are NOT crazy anymore")

        Card {
            Column(Modifier.padding(16.dp)) {
                Text("Last intent: ${state.lastIntent}")
                Text("Last decision: ${state.lastDecision}")
                Text("Mesh status: ${state.meshStatus}")
                Text("Peer count: ${state.peerCount}")
                Text("Debug lines: ${state.debugLines.size}")
            }
        }

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = input.value,
            onValueChange = { input.value = it },
            label = { Text("Intent") }
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onStartService) { Text("Start brain") }
            Button(onClick = { onSubmitIntent(input.value) }) { Text("Run intent") }
            Button(onClick = onMeshPing) { Text("Mesh ping") }
        }

        Card {
            Column(Modifier.padding(16.dp)) {
                Text("DEBUG LOG")

                if (state.debugLines.isEmpty()) {
                    Text("No debug yet")
                } else {
                    state.debugLines.forEach {
                        Text(it)
                    }
                }
            }
        }
    }
}