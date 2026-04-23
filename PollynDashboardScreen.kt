package com.stackbleedctrl.pollyn.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stackbleedctrl.pollyn.PollynUiState

@Composable
fun PollynDashboardScreen(
    state: PollynUiState,
    onStartService: () -> Unit,
    onSubmitIntent: (String) -> Unit,
    onMeshPing: () -> Unit
) {
    val input = remember { mutableStateOf("summarize my notifications") }
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("POLLYN DEBUG BUILD 7", style = MaterialTheme.typography.headlineMedium)
        Text("DEBUG SCREEN WITH PEER COUNT + LOGS")
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Last decision: ${state.lastDecision}")
                Text("Mesh status: ${state.meshStatus}")
            }
        }
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = input.value,
            onValueChange = { input.value = it },
            label = { Text("Intent") }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
    android.util.Log.d("POLLEN_UI", "Engage Brain tapped")
    onStartService()
}) {
    Text("Engage Brain")
}
            Button(onClick = { onSubmitIntent(input.value) }) { Text("Run intent") }
            Button(onClick = onMeshPing) { Text("Mesh ping") }
        }
    }
}
