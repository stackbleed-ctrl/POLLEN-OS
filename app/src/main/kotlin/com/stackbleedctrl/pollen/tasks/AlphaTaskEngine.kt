package com.stackbleedctrl.pollen.tasks

import android.content.Context
import android.os.BatteryManager
import com.stackbleedctrl.pollen.identity.DeviceIdProvider
import com.stackbleedctrl.pollen.mesh.MeshPacket
import com.stackbleedctrl.pollen.mesh.MeshPacketType

enum class AlphaTaskType {
    DEVICE_STATUS,
    BATTERY_STATUS,
    LOCAL_TIMESTAMP,
    MESH_ECHO,
    NODE_HEALTH
}

class AlphaTaskEngine(
    private val context: Context
) {

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
}
