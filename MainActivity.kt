package com.stackbleedctrl.pollyn

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
import com.stackbleedctrl.pollyn.oslayer.PollynBrainService
import com.stackbleedctrl.pollyn.ui.PollynDashboardScreen
import com.stackbleedctrl.pollyn.ui.theme.PollynTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= 33) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            PollynTheme {
                PollynDashboardScreen(
                    state = vm.state,
                    onStartService = {
                        ContextCompat.startForegroundService(this, Intent(this, PollynBrainService::class.java))
                    },
                    onSubmitIntent = vm::submitIntent,
                    onMeshPing = vm::meshPing
                )
            }
        }
    }
}
