package com.stackbleedctrl.pollyn

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.stackbleedctrl.pollyn.oslayer.PollynBrainService
import com.stackbleedctrl.pollyn.ui.theme.PollynTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val meshPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            Log.d("POLLEN_PERMS", "permission results=$results")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestMeshPermissions()

        setContent {
            PollynTheme {
                val vm: MainViewModel = hiltViewModel()

                PollynDashboardScreen(
                    state = vm.state,
                    onStartService = {
                        Log.d("POLLEN_UI", "Starting PollynBrainService")
                        ContextCompat.startForegroundService(
                            this,
                            Intent(this, PollynBrainService::class.java)
                        )
                    },
                    onSubmitIntent = vm::submitIntent,
                    onMeshPing = vm::meshPing
                )
            }
        }
    }

    private fun requestMeshPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE
        )

        if (Build.VERSION.SDK_INT <= 32) {
            permissions += Manifest.permission.ACCESS_FINE_LOCATION
        }

        if (Build.VERSION.SDK_INT >= 33) {
            permissions += Manifest.permission.NEARBY_WIFI_DEVICES
            permissions += Manifest.permission.POST_NOTIFICATIONS
        }

        Log.d("POLLEN_PERMS", "requesting permissions=$permissions")
        meshPermissionLauncher.launch(permissions.toTypedArray())
    }
}