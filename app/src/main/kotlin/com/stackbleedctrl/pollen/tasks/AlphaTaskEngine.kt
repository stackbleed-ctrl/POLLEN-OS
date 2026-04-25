package com.stackbleedctrl.pollen.tasks

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.stackbleedctrl.pollen.identity.DeviceIdProvider
import com.stackbleedctrl.pollen.location.LocationSnapshotProvider
import com.stackbleedctrl.pollen.mesh.MeshPacket
import com.stackbleedctrl.pollen.mesh.MeshPacketType
import com.stackbleedctrl.pollen.version.PollenBuildInfo

enum class AlphaTaskType {
    DEVICE_STATUS,
    BATTERY_STATUS,
    DEVICE_VITALS,
    BEACON_PEER,
    LOCATION_SNAPSHOT,
    FIELD_NOTE,
    SIMULATED_HELP_SIGNAL,
    LOCAL_TIMESTAMP,
    MESH_ECHO,
    PING,
    PROTOCOL_VERSION,
    SUPPORTED_TASKS,
    NODE_HEALTH
}

class AlphaTaskEngine(
    private val context: Context
) {
    private val locationProvider = LocationSnapshotProvider(context)


    fun handleTask(packet: MeshPacket): MeshPacket {
        val nodeId = DeviceIdProvider.getNodeId(context)
        val taskId = packet.taskId ?: "unknown-task"
        val taskType = packet.taskType ?: "UNKNOWN"

        return when (taskType) {
            AlphaTaskType.DEVICE_STATUS.name -> {
                result(
                    nodeId = nodeId,
                    taskId = taskId,
                    taskType = taskType,
                    success = true,
                    payload = "ONLINE"
                )
            }

            AlphaTaskType.BATTERY_STATUS.name -> {
                result(
                    nodeId = nodeId,
                    taskId = taskId,
                    taskType = taskType,
                    success = true,
                    payload = "Battery ${batteryPercent()}%"
                )
            }

            AlphaTaskType.DEVICE_VITALS.name -> {
                val identity = DeviceIdProvider.getIdentity(context)

                result(
                    nodeId = nodeId,
                    taskId = taskId,
                    taskType = taskType,
                    success = true,
                    payload = "Device=${identity.displayName}, Model=${identity.modelName}, Android=${Build.VERSION.RELEASE}, SDK=${Build.VERSION.SDK_INT}, Battery=${batteryPercent()}%, Charging=${isCharging()}"
                )
            }

            AlphaTaskType.BEACON_PEER.name -> {
                val beaconed = runBeacon()

                result(
                    nodeId = nodeId,
                    taskId = taskId,
                    taskType = taskType,
                    success = beaconed,
                    payload = if (beaconed) {
                        "Beacon activated: vibration signal sent"
                    } else {
                        "Beacon unavailable on this device"
                    }
                )
            }

            AlphaTaskType.LOCATION_SNAPSHOT.name -> {
                val snapshot = locationProvider.getLastKnownLocation()

                result(
                    nodeId = nodeId,
                    taskId = taskId,
                    taskType = taskType,
                    success = snapshot != null,
                    payload = snapshot?.toDisplayString()
                        ?: "Location unavailable: permission missing or no last known fix"
                )
            }

            AlphaTaskType.FIELD_NOTE.name -> {
                result(
                    nodeId = nodeId,
                    taskId = taskId,
                    taskType = taskType,
                    success = true,
                    payload = "FIELD_NOTE_RECEIVED: ${packet.payload ?: "No note payload"}"
                )
            }

            AlphaTaskType.SIMULATED_HELP_SIGNAL.name -> {
                result(
                    nodeId = nodeId,
                    taskId = taskId,
                    taskType = taskType,
                    success = true,
                    payload = "SIMULATION_ACK: help signal received by node $nodeId"
                )
            }

            AlphaTaskType.LOCAL_TIMESTAMP.name -> {
                result(
                    nodeId = nodeId,
                    taskId = taskId,
                    taskType = taskType,
                    success = true,
                    payload = System.currentTimeMillis().toString()
                )
            }

            AlphaTaskType.MESH_ECHO.name -> {
                result(
                    nodeId = nodeId,
                    taskId = taskId,
                    taskType = taskType,
                    success = true,
                    payload = packet.payload ?: "EMPTY_ECHO"
                )
            }

            AlphaTaskType.PING.name -> {
                result(
                    nodeId = nodeId,
                    taskId = taskId,
                    taskType = taskType,
                    success = true,
                    payload = "PONG · node=$nodeId · battery=${batteryPercent()}%"
                )
            }

            AlphaTaskType.PROTOCOL_VERSION.name -> {
                result(
                    nodeId = nodeId,
                    taskId = taskId,
                    taskType = taskType,
                    success = true,
                    payload = PollenBuildInfo.protocolPayload()
                )
            }

            AlphaTaskType.SUPPORTED_TASKS.name -> {
                result(
                    nodeId = nodeId,
                    taskId = taskId,
                    taskType = taskType,
                    success = true,
                    payload = AlphaTaskType.entries.joinToString(",") { it.name }
                )
            }

            AlphaTaskType.NODE_HEALTH.name -> {
                val identity = DeviceIdProvider.getIdentity(context)

                result(
                    nodeId = nodeId,
                    taskId = taskId,
                    taskType = taskType,
                    success = true,
                    payload = "Node=${identity.displayName}, Role=${identity.role}, Battery=${batteryPercent()}%"
                )
            }

            else -> {
                result(
                    nodeId = nodeId,
                    taskId = taskId,
                    taskType = taskType,
                    success = false,
                    payload = "Unsupported task type: $taskType"
                )
            }
        }
    }

    private fun result(
        nodeId: String,
        taskId: String,
        taskType: String,
        success: Boolean,
        payload: String
    ): MeshPacket {
        return MeshPacket(
            type = MeshPacketType.TASK_RESULT,
            fromNodeId = nodeId,
            taskId = taskId,
            taskType = taskType,
            payload = payload,
            success = success
        )
    }

    private fun batteryPercent(): Int {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun isCharging(): Boolean {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.isCharging
    }

    private fun runBeacon(): Boolean {
        return try {
            val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                manager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (vibrator == null || !vibrator.hasVibrator()) {
                false
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val pattern = longArrayOf(0, 180, 120, 180, 120, 420)
                    vibrator.vibrate(
                        VibrationEffect.createWaveform(pattern, -1)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 180, 120, 180, 120, 420), -1)
                }

                true
            }
        } catch (_: Exception) {
            false
        }
    }
}
