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
import com.stackbleedctrl.pollen.ui.PollenDashboardScreen
import com.stackbleedctrl.pollen.ui.theme.PollenTheme
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
            PollenTheme {
                PollenDashboardScreen(
                    state = vm.state,
                    onStartService = {
    vm.startBrain()
    ContextCompat.startForegroundService(
        this,
        Intent(this, PollenBrainService::class.java)
    )
},
                    onSubmitIntent = vm::submitIntent,
                    onMeshPing = vm::meshPing
                )
            }
        }
    }
}
