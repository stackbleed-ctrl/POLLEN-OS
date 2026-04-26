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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.stackbleedctrl.pollen.identity.DeviceIdentity
import com.stackbleedctrl.pollen.tasks.AlphaTaskState
import com.stackbleedctrl.pollen.tasks.AlphaTaskType
import com.stackbleedctrl.pollen.ui.PollenColors.DeepPanel
import com.stackbleedctrl.pollen.ui.PollenColors.DeepPanel2
import com.stackbleedctrl.pollen.ui.PollenColors.Gold
import com.stackbleedctrl.pollen.ui.PollenColors.GoldBright
import com.stackbleedctrl.pollen.ui.PollenColors.GoldSoft
import com.stackbleedctrl.pollen.ui.PollenColors.TextMuted
import com.stackbleedctrl.pollen.ui.PollenColors.TextPrimary

@Composable
fun PollenConsoleScreen(
    connected: Boolean = false,
    peerCount: Int = 0,
    selectedPeerLabel: String = "",
    peerFreshnessLabel: String = "No peer",
    taskRouteReady: Boolean = false,
    keyModeLabel: String = "Unpaired",
    encryptionModeLabel: String = "Alpha fallback",
    missionModeLabel: String = "OFFLINE_READY",
    infrastructureLabel: String = "Not required",
    missionSummary: String = "Ready for infrastructureless operation",
    pendingCoordinateRequestLabel: String = "",
    pendingCoordinateRequestTaskId: String = "",
    pendingCoordinateRequestAt: Long? = null,
    lastPeerCoordinateLabel: String = "None",
    lastPeerCoordinateDistanceLabel: String = "Unknown",
    lastPeerCoordinateBearingLabel: String = "Unknown",
    lastPeerCoordinateFreshnessLabel: String = "No coordinate fix",
    lastPeerCoordinateFixAgeLabel: String = "Unknown",
    lastPeerCoordinateQualityLabel: String = "No fix",
    lastPeerCoordinateAccuracyLabel: String = "Unknown",
    lastPeerCoordinateConfidenceLabel: String = "Unknown",
    lastPeerCoordinateNavigationSummary: String = "No peer navigation fix",
    lastPeerCoordinateReceivedAt: Long? = null,
    lastIntent: String = "Mesh Health Check",
    lastDecision: String = "Waiting",
    meshStatus: String = "Idle",
    debugLines: List<String> = emptyList(),
    identity: DeviceIdentity? = null,
    tasks: List<AlphaTaskState> = emptyList(),
    eventLog: List<String> = emptyList(),
    trustedPeerLabel: String = "",
    aiSummary: String = "AI waiting for mesh events",
    aiRecommendedAction: String = "OBSERVE",
    aiConfidence: Float = 0f,
    aiHealthScore: Int = 60,
    peerProtocolVersion: String = "Unknown",
    peerSupportedTasks: String = "Unknown",
    compatibilityStatus: String = "Not checked",
    demoSequenceRunning: Boolean = false,
    demoSequenceStep: Int = 0,
    demoSequenceTotal: Int = 5,
    peerCapabilitySummary: String = "Peer capability not checked",
    recommendedSafeTask: String = "WAIT_FOR_PEER",
    alphaVerifyRunning: Boolean = false,
    alphaVerifyStep: Int = 0,
    alphaVerifyTotal: Int = 6,
    fieldTestRunning: Boolean = false,
    fieldTestCheckCount: Int = 0,
    fieldTestDistanceLabel: String = "Unmarked",
    fieldTestEnvironment: String = "Indoor/outdoor",
    connectionStartedAt: Long? = null,
    lastDisconnectedAt: Long? = null,
    lastReconnectedAt: Long? = null,
    reconnectCount: Int = 0,
    buildLabel: String = "Alpha 0.3-dev",
    protocolLabel: String = "0.2",
    fullTestRunning: Boolean = false,
    rangeProbeRunning: Boolean = false,
    rangeProbeSent: Int = 0,
    rangeProbeTotal: Int = 12,
    averageLatencyMs: Long? = null,
    completedCount: Int = 0,
    failedCount: Int = 0,
    pendingCount: Int = 0,
    onStartBrain: () -> Unit = {},
    onRunIntent: () -> Unit = {},
    onMeshPing: () -> Unit = {},
    onAlphaTask: (AlphaTaskType) -> Unit = {},
    onTrustSelectedPeer: () -> Unit = {},
    onClearTrustedPeer: () -> Unit = {},
    onExportLogs: () -> Unit = {},
    onRunFullMeshTest: () -> Unit = {},
    onRunRangeProbe: () -> Unit = {},
    onRunCompatibilityCheck: () -> Unit = {},
    onRunDemoSequence: () -> Unit = {},
    onRunAlphaVerification: () -> Unit = {},
    onSharePendingCoordinates: () -> Unit = {},
    onDenyCoordinateRequest: () -> Unit = {},
    onStartFieldTest: () -> Unit = {},
    onMarkFieldDistanceCheck: () -> Unit = {},
    onEndFieldTest: () -> Unit = {}
) {
    val visibleIntent = lastIntent.ifBlank { "Mesh Health Check" }
    val visibleDecision = lastDecision.ifBlank { "Waiting" }
    val visibleStatus = meshStatus.ifBlank { "Idle" }

    PollenTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PollenBackgroundBrush)
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Header(
                connected = connected,
                peerCount = peerCount,
                buildLabel = buildLabel
            )

            PremiumPanel {
                SectionTitle("SYSTEM BADGES")

                Spacer(modifier = Modifier.height(12.dp))

                InfoLine("Build", buildLabel)
                InfoLine("Protocol", protocolLabel)
                InfoLine("Trust", trustedPeerLabel.ifBlank { "Untrusted" })
                InfoLine("Pairing", keyModeLabel)
                InfoLine("Encryption", encryptionModeLabel)
                InfoLine("Compatibility", compatibilityStatus)
                InfoLine("Location policy", "Trusted peer only")
                InfoLine("AI action", aiRecommendedAction)
            }

            PremiumPanel {
                SectionTitle("MISSION LAYER")

                Spacer(modifier = Modifier.height(12.dp))

                InfoLine("Mission mode", missionModeLabel)
                InfoLine("Infrastructure", infrastructureLabel)
                InfoLine("Mission summary", missionSummary)
                InfoLine("Route", if (taskRouteReady) "Fresh route ready" else "Waiting for route")
                InfoLine("Peer", selectedPeerLabel.ifBlank { "No selected peer" })
                InfoLine("Freshness", peerFreshnessLabel)
                InfoLine("Encryption", encryptionModeLabel)
                InfoLine("Navigation", lastPeerCoordinateNavigationSummary)
                InfoLine("Peer coordinates", lastPeerCoordinateLabel)
                InfoLine("Distance estimate", lastPeerCoordinateDistanceLabel)
                InfoLine("Bearing", lastPeerCoordinateBearingLabel)
                InfoLine("Peer fix age", lastPeerCoordinateFixAgeLabel)
                InfoLine("Fix quality", lastPeerCoordinateQualityLabel)
                InfoLine("Accuracy", lastPeerCoordinateAccuracyLabel)
                InfoLine("Fix confidence", lastPeerCoordinateConfidenceLabel)
                InfoLine("Coordinate freshness", lastPeerCoordinateFreshnessLabel)
                InfoLine("Received age", displayDurationFrom(lastPeerCoordinateReceivedAt))
            }

            PremiumPanel {
                SectionTitle("COORDINATE CONSENT")

                Spacer(modifier = Modifier.height(12.dp))

                if (pendingCoordinateRequestLabel.isBlank()) {
                    InfoLine("Status", "No pending coordinate request")
                    InfoLine("Policy", "Coordinates require explicit local share")
                } else {
                    InfoLine("Status", "Approval required")
                    InfoLine("Requester", pendingCoordinateRequestLabel)
                    InfoLine("Request task", pendingCoordinateRequestTaskId.ifBlank { "Unknown" })
                    InfoLine("Received", displayTimestamp(pendingCoordinateRequestAt))
                    InfoLine("Policy", "Use explicit share only if safe")

                    Spacer(modifier = Modifier.height(8.dp))

                    GoldButton(
                        text = "Approve · Share My Coordinates",
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        onSharePendingCoordinates()
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    GoldButton(
                        text = "Deny Coordinate Request",
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        onDenyCoordinateRequest()
                    }
                }
            }

            PremiumPanel {
                SectionTitle("MESH TEST SUMMARY")

                Spacer(modifier = Modifier.height(12.dp))

                InfoLine("Health", meshHealthLabel(connected, failedCount, pendingCount))
                InfoLine("Peers", peerCount.toString())
                InfoLine("Completed", completedCount.toString())
                InfoLine("Failed", failedCount.toString())
                InfoLine("Pending", pendingCount.toString())
                InfoLine("Average latency", averageLatencyMs?.let { "${it}ms" } ?: "No latency yet")
                InfoLine("Full test", if (fullTestRunning) "Running" else "Ready")
                InfoLine("Demo sequence", if (demoSequenceRunning) "$demoSequenceStep/$demoSequenceTotal running" else "Ready")
                InfoLine("Alpha verify", if (alphaVerifyRunning) "$alphaVerifyStep/$alphaVerifyTotal running" else "Ready")
                InfoLine("Field test", if (fieldTestRunning) "Running · $fieldTestDistanceLabel" else "Ready")
                InfoLine("Range probe", if (rangeProbeRunning) "$rangeProbeSent/$rangeProbeTotal running" else "Ready")
            }

            PremiumPanel {
                SectionTitle("FIELD DISTANCE TEST")

                Spacer(modifier = Modifier.height(12.dp))

                InfoLine("Status", if (fieldTestRunning) "Running" else "Ready")
                InfoLine("Checks", fieldTestCheckCount.toString())
                InfoLine("Distance", fieldTestDistanceLabel)
                InfoLine("Environment", fieldTestEnvironment)
                InfoLine("Peer count", peerCount.toString())
                InfoLine("AI health", "$aiHealthScore/100")

                Spacer(modifier = Modifier.height(8.dp))

                GoldButton(
                    text = if (fieldTestRunning) "Field Test Running" else "Start Field Test",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onStartFieldTest
                )

                Spacer(modifier = Modifier.height(8.dp))

                GoldButton(
                    text = "Mark Distance Check",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onMarkFieldDistanceCheck
                )

                Spacer(modifier = Modifier.height(8.dp))

                GoldButton(
                    text = "End Field Test",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onEndFieldTest
                )
            }

            PremiumPanel {
                SectionTitle("CONNECTION STABILITY")

                Spacer(modifier = Modifier.height(12.dp))

                InfoLine("Uptime", displayDurationFrom(connectionStartedAt))
                InfoLine("Reconnects", reconnectCount.toString())
                InfoLine("Last drop", displayTimestamp(lastDisconnectedAt))
                InfoLine("Last reconnect", displayTimestamp(lastReconnectedAt))
            }

            PremiumPanel {
                SectionTitle("TESTER REPORT")

                Spacer(modifier = Modifier.height(12.dp))

                InfoLine("Build", buildLabel)
                InfoLine("Peer", if (peerCount > 0) "Visible" else "Not visible")
                InfoLine("Tasks", "Completed $completedCount · Failed $failedCount · Pending $pendingCount")
                InfoLine("Average latency", averageLatencyMs?.let { "${it}ms" } ?: "No latency yet")
                InfoLine("AI health", "$aiHealthScore/100")
                InfoLine("Compatibility", compatibilityStatus)
                InfoLine("Trust", trustedPeerLabel.ifBlank { "Untrusted" })
            }

            PremiumPanel {
                SectionTitle("DEVICE IDENTITY")

                Spacer(modifier = Modifier.height(12.dp))

                InfoLine("Name", identity?.displayName ?: "POLLEN Node")
                InfoLine("Node ID", identity?.nodeId ?: "Creating identity")
                InfoLine("Model", identity?.modelName ?: "Unknown")
                InfoLine("Role", identity?.role?.name ?: "WORKER")
                InfoLine("Build", buildLabel)
                InfoLine("Protocol", protocolLabel)
            }

            PremiumPanel {
                SectionTitle("BRAIN STATUS")

                Spacer(modifier = Modifier.height(12.dp))

                InfoLine("Current test", visibleIntent)
                InfoLine("Last decision", visibleDecision)
                InfoLine("Mesh status", visibleStatus)
                InfoLine("Peer count", peerCount.toString())
            }

            PremiumPanel {
                SectionTitle("AI MESH HEALTH")

                Spacer(modifier = Modifier.height(12.dp))

                InfoLine("Score", "$aiHealthScore/100")
                InfoLine("Assessment", aiHealthLabel(aiHealthScore))
                InfoLine("AI summary", aiSummary)
                InfoLine("Recommended action", aiRecommendedAction)
                InfoLine("Confidence", "${(aiConfidence * 100).toInt()}%")
            }

            PremiumPanel {
                SectionTitle("PEER COMPATIBILITY")

                Spacer(modifier = Modifier.height(12.dp))

                InfoLine("Status", compatibilityStatus)
                InfoLine("Capability", peerCapabilitySummary)
                InfoLine("Recommended safe task", recommendedSafeTask)
                InfoLine("Protocol", peerProtocolVersion)
                InfoLine(
                    "Supported tasks",
                    if (peerSupportedTasks.length > 120) {
                        peerSupportedTasks.take(120) + "..."
                    } else {
                        peerSupportedTasks
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                GoldButton(
                    text = "Check Peer Compatibility",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onRunCompatibilityCheck
                )
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
                    text = "Mesh Health",
                    modifier = Modifier.weight(1f),
                    onClick = onRunIntent
                )

                GoldButton(
                    text = "Ping Mesh",
                    modifier = Modifier.weight(1f),
                    onClick = onMeshPing
                )
            }

            PremiumPanel {
                SectionTitle("DEMO CONTROLS")

                Spacer(modifier = Modifier.height(12.dp))

                InfoLine("Recommended flow", "Start Brain → Check Compatibility → Run Demo Sequence")
                InfoLine("Safe task", recommendedSafeTask)
                InfoLine("Compatibility", compatibilityStatus)
            }

            GoldButton(
                text = if (fullTestRunning) "Full Mesh Test Running" else "Run Full Mesh Test",
                modifier = Modifier.fillMaxWidth(),
                onClick = onRunFullMeshTest
            )

            GoldButton(
                text = if (demoSequenceRunning) "Demo Sequence $demoSequenceStep/$demoSequenceTotal" else "Run Demo Sequence",
                modifier = Modifier.fillMaxWidth(),
                onClick = onRunDemoSequence
            )

            GoldButton(
                text = if (alphaVerifyRunning) "Alpha Verification $alphaVerifyStep/$alphaVerifyTotal" else "Run Alpha Verification",
                modifier = Modifier.fillMaxWidth(),
                onClick = onRunAlphaVerification
            )

            GoldButton(
                text = if (rangeProbeRunning) "Range Probe $rangeProbeSent/$rangeProbeTotal" else "Run Range Probe",
                modifier = Modifier.fillMaxWidth(),
                onClick = onRunRangeProbe
            )

            GoldButton(
                text = "Export Tester Log",
                modifier = Modifier.fillMaxWidth(),
                onClick = onExportLogs
            )

            PremiumPanel {
                SectionTitle("MESH NETWORK")

                Spacer(modifier = Modifier.height(12.dp))

                InfoLine("Connection", if (connected) "Connected" else "Searching")
                InfoLine("Visible peers", peerCount.toString())
                InfoLine("Active test", "Peer Discovery Test")
                InfoLine("Task routing", if (connected) "Peer route available" else "Local device")
                InfoLine("Queue", "${tasks.count { it.status.name == "PENDING" }} pending")
            }

            PremiumPanel {
                SectionTitle("ALPHA TASK CONTROLS")

                Spacer(modifier = Modifier.height(12.dp))

                GoldButton("Device Status") {
                    onAlphaTask(AlphaTaskType.DEVICE_STATUS)
                }

                Spacer(modifier = Modifier.height(8.dp))

                GoldButton("Battery Status") {
                    onAlphaTask(AlphaTaskType.BATTERY_STATUS)
                }

                Spacer(modifier = Modifier.height(8.dp))

                GoldButton("Device Vitals") {
                    onAlphaTask(AlphaTaskType.DEVICE_VITALS)
                }

                Spacer(modifier = Modifier.height(8.dp))

                GoldButton("Beacon Peer · Vibration") {
                    onAlphaTask(AlphaTaskType.BEACON_PEER)
                }

                Spacer(modifier = Modifier.height(8.dp))

                GoldButton("Trust Selected Peer") {
                    onTrustSelectedPeer()
                }

                GoldButton("Clear Trust") {
                    onClearTrustedPeer()
                }

                GoldButton("Location Snapshot · Trusted Only") {
                    onAlphaTask(AlphaTaskType.LOCATION_SNAPSHOT)
                }

                Spacer(modifier = Modifier.height(8.dp))

                GoldButton("Mission Status") {
                    onAlphaTask(AlphaTaskType.MISSION_STATUS)
                }

                Spacer(modifier = Modifier.height(8.dp))

                GoldButton("Node Check-In") {
                    onAlphaTask(AlphaTaskType.NODE_CHECKIN)
                }

                Spacer(modifier = Modifier.height(8.dp))

                GoldButton("Request Peer Coordinates") {
                    onAlphaTask(AlphaTaskType.REQUEST_COORDINATES)
                }

                Spacer(modifier = Modifier.height(8.dp))

                GoldButton("Share My Coordinates · Explicit") {
                    onAlphaTask(AlphaTaskType.SHARE_COORDINATES)
                }

                Spacer(modifier = Modifier.height(8.dp))

                GoldButton("Resource Status") {
                    onAlphaTask(AlphaTaskType.RESOURCE_STATUS)
                }

                Spacer(modifier = Modifier.height(8.dp))

                GoldButton("Evac Marker") {
                    onAlphaTask(AlphaTaskType.EVAC_MARKER)
                }

                Spacer(modifier = Modifier.height(8.dp))

                GoldButton("Send Field Note") {
                    onAlphaTask(AlphaTaskType.FIELD_NOTE)
                }

                Spacer(modifier = Modifier.height(8.dp))

                GoldButton("Sim Help Signal · Test Only") {
                    onAlphaTask(AlphaTaskType.SIMULATED_HELP_SIGNAL)
                }

                Spacer(modifier = Modifier.height(8.dp))

                GoldButton("Mesh Echo") {
                    onAlphaTask(AlphaTaskType.MESH_ECHO)
                }

                Spacer(modifier = Modifier.height(8.dp))

                GoldButton("Ping Peer") {
                    onAlphaTask(AlphaTaskType.PING)
                }

                Spacer(modifier = Modifier.height(8.dp))

                GoldButton("Protocol Version") {
                    onAlphaTask(AlphaTaskType.PROTOCOL_VERSION)
                }

                Spacer(modifier = Modifier.height(8.dp))

                GoldButton("Supported Tasks") {
                    onAlphaTask(AlphaTaskType.SUPPORTED_TASKS)
                }

                Spacer(modifier = Modifier.height(8.dp))

                GoldButton("Node Health") {
                    onAlphaTask(AlphaTaskType.NODE_HEALTH)
                }
            }

            PremiumPanel {
                SectionTitle("TASK CONSOLE")

                Spacer(modifier = Modifier.height(12.dp))

                if (tasks.isEmpty()) {
                    InfoLine("Status", "No alpha tasks yet")
                } else {
                    tasks.take(6).forEach { task ->
                        TaskLine(task)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            PremiumPanel {
                SectionTitle("EVENT FEED")

                Spacer(modifier = Modifier.height(12.dp))

                if (eventLog.isEmpty()) {
                    InfoLine("Status", "Waiting for mesh events")
                } else {
                    eventLog.take(10).forEachIndexed { index, line ->
                        InfoLine("#${index + 1}", line)
                    }
                }
            }

            PremiumPanel {
                SectionTitle("DEBUG LOG")

                Spacer(modifier = Modifier.height(12.dp))

                if (debugLines.isEmpty()) {
                    InfoLine("Status", "Waiting for tester activity")
                } else {
                    debugLines.takeLast(8).forEachIndexed { index, line ->
                        InfoLine("#${index + 1}", line)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

private fun meshHealthLabel(
    connected: Boolean,
    failedCount: Int,
    pendingCount: Int
): String {
    return when {
        pendingCount > 0 -> "Testing"
        failedCount > 0 -> "Needs review"
        connected -> "Good"
        else -> "Searching"
    }
}

private fun aiHealthLabel(score: Int): String {
    return when {
        score >= 85 -> "Strong"
        score >= 70 -> "Stable"
        score >= 50 -> "Needs observation"
        else -> "Needs review"
    }
}

private fun displayDurationFrom(startedAt: Long?): String {
    if (startedAt == null) return "Unknown"

    val elapsedMs = System.currentTimeMillis() - startedAt
    val totalSeconds = elapsedMs / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L

    return if (minutes > 0) {
        "${minutes}m ${seconds}s"
    } else {
        "${seconds}s"
    }
}

private fun displayTimestamp(value: Long?): String {
    return value?.toString() ?: "None"
}

@Composable
private fun Header(
    connected: Boolean,
    peerCount: Int,
    buildLabel: String
) {
    Column {
        Text(
            text = "POLLEN OS",
            color = Color(0xFF15171C),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Light
        )

        Text(
            text = "$buildLabel · LIVE MESH TASK LAYER",
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
                text = if (connected) {
                    "Connected • $peerCount Peer"
                } else {
                    "Searching"
                },
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
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = GoldSoft,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
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
private fun TaskLine(task: AlphaTaskState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF111418), RoundedCornerShape(14.dp))
            .border(1.dp, Gold.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Text(
            text = task.taskType,
            color = TextPrimary,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Status: ${task.status}",
            color = GoldSoft,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )

        task.latencyMs?.let { latency ->
            Text(
                text = "Latency: ${latency}ms",
                color = GoldSoft,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        task.result?.let { result ->
            Text(
                text = "Result: $result",
                color = TextMuted,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
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
        modifier = modifier.height(64.dp),
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
