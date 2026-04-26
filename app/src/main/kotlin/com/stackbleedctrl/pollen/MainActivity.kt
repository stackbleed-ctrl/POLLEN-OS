package com.stackbleedctrl.pollen

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.stackbleedctrl.pollen.oslayer.PollenBrainService
import com.stackbleedctrl.pollen.tasks.TaskStatus
import com.stackbleedctrl.pollen.ui.PollenConsoleScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val denied = results.filterValues { granted -> !granted }.keys

        if (denied.isEmpty()) {
            vm.permissionsReady()
        } else {
            vm.permissionsDenied(denied.joinToString())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    vm.dashboardInitialized()

    requestPollenPermissions()

        setContent {
            val uiState = vm.state
            val completedCount = uiState.tasks.count { it.status == TaskStatus.COMPLETED }
            val failedCount = uiState.tasks.count { it.status == TaskStatus.FAILED }
            val pendingCount = uiState.tasks.count { it.status == TaskStatus.PENDING }
            val averageLatencyMs = uiState.tasks
                .mapNotNull { it.latencyMs }
                .filter { it >= 0L }
                .takeIf { it.isNotEmpty() }
                ?.average()
                ?.toLong()

            PollenConsoleScreen(
                connected = uiState.peerCount > 0,
                peerCount = uiState.peerCount,
                selectedPeerLabel = uiState.selectedPeerLabel,
                peerFreshnessLabel = uiState.peerFreshnessLabel,
                taskRouteReady = uiState.taskRouteReady,
                keyModeLabel = uiState.keyModeLabel,
                encryptionModeLabel = uiState.encryptionModeLabel,
                missionModeLabel = uiState.missionModeLabel,
                infrastructureLabel = uiState.infrastructureLabel,
                missionSummary = uiState.missionSummary,
                pendingCoordinateRequestLabel = uiState.pendingCoordinateRequestLabel,
                pendingCoordinateRequestTaskId = uiState.pendingCoordinateRequestTaskId,
                pendingCoordinateRequestAt = uiState.pendingCoordinateRequestAt,
                lastIntent = uiState.lastIntent.ifBlank { "Mesh Health Check" },
                lastDecision = uiState.lastDecision,
                meshStatus = uiState.meshStatus,
                debugLines = uiState.debugLines,
                identity = uiState.identity,
                tasks = uiState.tasks,
                eventLog = uiState.eventLog,
                aiSummary = uiState.aiSummary,
                aiRecommendedAction = uiState.aiRecommendedAction,
                aiConfidence = uiState.aiConfidence,
                aiHealthScore = uiState.aiHealthScore,
                fullTestRunning = uiState.fullTestRunning,
                rangeProbeRunning = uiState.rangeProbeRunning,
                rangeProbeSent = uiState.rangeProbeSent,
                rangeProbeTotal = uiState.rangeProbeTotal,
                averageLatencyMs = averageLatencyMs,
                completedCount = completedCount,
                failedCount = failedCount,
                pendingCount = pendingCount,
                onStartBrain = {
    vm.startBrain()
    ContextCompat.startForegroundService(
        this,
        Intent(this, PollenBrainService::class.java)
    )
    vm.brainServiceStarted()
},
                onRunIntent = {
                    vm.submitIntent("Mesh Health Check")
                },
                onMeshPing = vm::meshPing,
                onAlphaTask = vm::sendAlphaTask,
                onTrustSelectedPeer = vm::trustSelectedPeer,
                onClearTrustedPeer = vm::clearTrustedPeer,
                onExportLogs = ::shareTesterLog,
                onRunFullMeshTest = vm::runFullMeshTest,
                onRunRangeProbe = vm::runRangeProbe,
                onSharePendingCoordinates = vm::shareCoordinatesForPendingRequest,
                onDenyCoordinateRequest = vm::denyPendingCoordinateRequest
            )
        }
    }

    private fun shareTesterLog() {
        val logText = vm.buildTesterLog()

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "POLLEN-OS Alpha Tester Log")
            putExtra(Intent.EXTRA_TEXT, logText)
        }

        startActivity(
            Intent.createChooser(
                sendIntent,
                "Export POLLEN-OS Tester Log"
            )
        )
    }

    private fun requestPollenPermissions() {
        val permissions = mutableListOf<String>()

        permissions += Manifest.permission.ACCESS_FINE_LOCATION
        permissions += Manifest.permission.ACCESS_COARSE_LOCATION

        if (Build.VERSION.SDK_INT >= 31) {
            permissions += Manifest.permission.BLUETOOTH_SCAN
            permissions += Manifest.permission.BLUETOOTH_CONNECT
            permissions += Manifest.permission.BLUETOOTH_ADVERTISE
        }

        if (Build.VERSION.SDK_INT >= 33) {
            permissions += Manifest.permission.POST_NOTIFICATIONS
            permissions += Manifest.permission.NEARBY_WIFI_DEVICES
        }

        permissionLauncher.launch(permissions.toTypedArray())
    }
}
