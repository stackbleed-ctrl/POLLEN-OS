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

        requestPollenPermissions()

        setContent {
            PollenConsoleScreen(
                connected = vm.state.peerCount > 0,
                peerCount = vm.state.peerCount,
                lastIntent = vm.state.lastIntent.ifBlank { "Mesh Health Check" },
                lastDecision = vm.state.lastDecision,
                meshStatus = vm.state.meshStatus,
                debugLines = vm.state.debugLines,
                identity = vm.state.identity,
                tasks = vm.state.tasks,
                eventLog = vm.state.eventLog,
                fullTestRunning = vm.state.fullTestRunning,
                rangeProbeRunning = vm.state.rangeProbeRunning,
                rangeProbeSent = vm.state.rangeProbeSent,
                rangeProbeTotal = vm.state.rangeProbeTotal,
                averageLatencyMs = vm.averageLatencyMs(),
                completedCount = vm.completedTaskCount(),
                failedCount = vm.failedTaskCount(),
                pendingCount = vm.pendingTaskCount(),
                onStartBrain = {
                    vm.startBrain()
                    ContextCompat.startForegroundService(
                        this,
                        Intent(this, PollenBrainService::class.java)
                    )
                },
                onRunIntent = {
                    vm.submitIntent("Mesh Health Check")
                },
                onMeshPing = vm::meshPing,
                onAlphaTask = vm::sendAlphaTask,
                onExportLogs = ::shareTesterLog,
                onRunFullMeshTest = vm::runFullMeshTest,
                onRunRangeProbe = vm::runRangeProbe
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
