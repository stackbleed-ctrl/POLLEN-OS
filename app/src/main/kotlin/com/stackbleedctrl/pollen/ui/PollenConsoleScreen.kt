package com.stackbleedctrl.pollen.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stackbleedctrl.pollen.ui.PollenColors.DeepPanel
import com.stackbleedctrl.pollen.ui.PollenColors.DeepPanel2
import com.stackbleedctrl.pollen.ui.PollenColors.Gold
import com.stackbleedctrl.pollen.ui.PollenColors.GoldBright
import com.stackbleedctrl.pollen.ui.PollenColors.GoldSoft
import com.stackbleedctrl.pollen.ui.PollenColors.TextMuted
import com.stackbleedctrl.pollen.ui.PollenColors.TextPrimary

@Composable
fun PollenConsoleScreen(
    connected: Boolean = true,
    peerCount: Int = 1,
    lastIntent: String = "summarize my notifications",
    meshStatus: String = "PING received from Moto G 5G",
    onStartBrain: () -> Unit = {},
    onRunIntent: () -> Unit = {},
    onMeshPing: () -> Unit = {}
) {
    PollenTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PollenBackgroundBrush)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Header(connected = connected, peerCount = peerCount)

            PremiumPanel {
                Text(
                    text = "BRAIN STATUS",
                    color = GoldSoft,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                InfoLine("Last intent", lastIntent)
                InfoLine("Mesh status", meshStatus)
                InfoLine("Peer count", peerCount.toString())
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GoldButton(
                    text = "Start Brain",
                    modifier = Modifier.weight(1f),
                    onClick = onStartBrain
                )
                GoldButton(
                    text = "Run Intent",
                    modifier = Modifier.weight(1f),
                    onClick = onRunIntent
                )
                GoldButton(
                    text = "Mesh Ping",
                    modifier = Modifier.weight(1f),
                    onClick = onMeshPing
                )
            }

            PremiumPanel {
                Text(
                    text = "MESH NETWORK",
                    color = GoldSoft,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                InfoLine("Device", "moto g 5G - 2023")
                InfoLine("Pollen ID", "POLLEN-465A80")
                InfoLine("Active peer", "OTKH connected")
                InfoLine("Queue", "No pending tasks")
            }

            PremiumPanel {
                Text(
                    text = "DEBUG LOG",
                    color = GoldSoft,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                InfoLine("3:11:02", "Advertising started")
                InfoLine("3:11:03", "Discovery complete")
                InfoLine("3:11:05", "PING received from Moto G 5G")
            }
        }
    }
}

@Composable
private fun Header(
    connected: Boolean,
    peerCount: Int
) {
    Column {
        Text(
            text = "POLLEN OS",
            color = Color(0xFF15171C),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Light
        )

        Text(
            text = "BRAIN MESH CONTROL",
            color = Color(0xFF55514A),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .shadow(8.dp, RoundedCornerShape(50))
                .background(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF252A31), Color(0xFF111418))
                    ),
                    shape = RoundedCornerShape(50)
                )
                .border(1.dp, GoldSoft, RoundedCornerShape(50))
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(if (connected) GoldBright else Color.Gray, CircleShape)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = if (connected) "Connected • $peerCount Peer" else "Searching",
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun PremiumPanel(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(22.dp))
            .background(
                brush = Brush.verticalGradient(
                    listOf(DeepPanel2, DeepPanel)
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .border(1.dp, Gold.copy(alpha = 0.45f), RoundedCornerShape(22.dp))
            .padding(16.dp),
        content = content
    )
}

@Composable
private fun InfoLine(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 5.dp)) {
        Text(
            text = label.uppercase(),
            color = TextMuted,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = value,
            color = TextPrimary,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun GoldButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(74.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFE7E3DA),
            contentColor = Color(0xFF101318)
        )
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold
        )
    }
}