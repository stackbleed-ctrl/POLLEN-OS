package com.stackbleedctrl.pollen

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stackbleedctrl.pollen.identity.DeviceIdProvider
import com.stackbleedctrl.pollen.mesh.MeshPacket
import com.stackbleedctrl.pollen.mesh.MeshPacketType
import com.stackbleedctrl.pollen.sdk.PollenSdk
import com.stackbleedctrl.pollen.tasks.AlphaTaskState
import com.stackbleedctrl.pollen.tasks.AlphaTaskType
import com.stackbleedctrl.pollen.tasks.TaskStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    private val sdk: PollenSdk,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val taskTimeoutMs = 8_000L

    var state by mutableStateOf(
        PollenUiState(
            identity = DeviceIdProvider.getIdentity(appContext)
        )
    )
        private set

    init {
        appendDebug("MainViewModel started")
        logEvent("Alpha 0.2 dashboard initialized")

        sdk.brain.handleDecision { decision ->
            appendDebug("decision: ${decision.summary}")
            state = state.copy(lastDecision = decision.summary)
        }

        sdk.brain.handleMeshStatus { status ->
            appendDebug("brain mesh status: $status")

            if (status.startsWith("POLLEN_TASK_RESULT|")) {
                val rawPacket = status.removePrefix("POLLEN_TASK_RESULT|")
                MeshPacket.fromJson(rawPacket)?.let { packet ->
                    onTaskResult(packet)
                }
            } else {
                state = state.copy(meshStatus = status)
            }
        }

        sdk.brain.handlePeerCount { count ->
            appendDebug("peer count: $count")
            state = state.copy(peerCount = count)
        }

        appendDebug("Waiting for Start brain")
    }

    fun submitIntent(raw: String) {
        state = state.copy(lastIntent = raw)
        appendDebug("intent pressed: $raw")

        viewModelScope.launch {
            sdk.submitIntent(raw)
            appendDebug("intent submitted")
        }
    }

    fun startBrain() {
        appendDebug("START BRAIN pressed")
        logEvent("Start brain pressed")
        state = state.copy(meshStatus = "Starting brain service...")
    }

    fun permissionsReady() {
        appendDebug("permissions ready")
        logEvent("Permissions ready")
        state = state.copy(meshStatus = "Permissions ready")
    }

    fun permissionsDenied(denied: String) {
        appendDebug("permissions denied: $denied")
        logEvent("Permissions denied: $denied")
        state = state.copy(meshStatus = "Permissions missing")
    }

    fun meshPing() {
        appendDebug("PING pressed")
        logEvent("Ping requested")

        viewModelScope.launch {
            sdk.meshPing()
            appendDebug("sdk.meshPing called")
            logEvent("sdk.meshPing called")
        }
    }

    fun createTaskPacket(taskType: AlphaTaskType, targetNodeId: String? = null): MeshPacket {
        val identity = state.identity ?: DeviceIdProvider.getIdentity(appContext)
        val taskId = UUID.randomUUID().toString().take(8)

        val packet = MeshPacket(
            type = MeshPacketType.TASK_REQUEST,
            fromNodeId = identity.nodeId,
            toNodeId = targetNodeId,
            taskId = taskId,
            taskType = taskType.name,
            payload = when (taskType) {
                AlphaTaskType.MESH_ECHO -> "POLLEN mesh echo from ${identity.displayName}"
                else -> null
            }
        )

        val newTask = AlphaTaskState(
            taskId = taskId,
            taskType = taskType.name,
            targetNodeId = targetNodeId,
            status = TaskStatus.PENDING
        )

        state = state.copy(
            tasks = listOf(newTask) + state.tasks
        )

        appendDebug("task created: ${taskType.name}")
        logEvent("Task created: ${taskType.name}")

        return packet
    }

    fun onTaskResult(packet: MeshPacket) {
        val taskId = packet.taskId ?: return
        val existingTask = state.tasks.firstOrNull { it.taskId == taskId }

        if (existingTask == null) {
            appendDebug("unknown task result ignored: ${packet.taskType}")
            logEvent("Unknown task result ignored: ${packet.taskType}")
            return
        }

        if (existingTask.status != TaskStatus.PENDING) {
            appendDebug("duplicate task result ignored: ${packet.taskType}")
            logEvent("Duplicate result ignored: ${packet.taskType}")
            return
        }

        val completedAt = System.currentTimeMillis()

        val updatedTasks = state.tasks.map { task ->
            if (task.taskId == taskId) {
                task.copy(
                    status = if (packet.success == true) TaskStatus.COMPLETED else TaskStatus.FAILED,
                    result = packet.payload,
                    completedAt = completedAt,
                    latencyMs = completedAt - task.createdAt
                )
            } else {
                task
            }
        }

        state = state.copy(tasks = updatedTasks)

        appendDebug("task result: ${packet.taskType} ${packet.payload}")
        logEvent("Task result: ${packet.taskType} → ${packet.payload}")
    }

    fun sendAlphaTask(taskType: AlphaTaskType) {
        val packet = createTaskPacket(taskType)
        val taskId = packet.taskId

        viewModelScope.launch {
            val sent = sdk.sendMeshPacketToBestPeer(packet.toJson())

            if (sent) {
                appendDebug("targeted mesh task sent: ${packet.taskType}")
                logEvent("Targeted task sent: ${packet.taskType}")
            } else {
                appendDebug("targeted mesh task failed: no peer")
                logEvent("No peer available for task: ${packet.taskType}")
            }
        }

        if (taskId != null) {
            scheduleTaskTimeout(taskId)
        }
    }

    private fun scheduleTaskTimeout(taskId: String) {
        viewModelScope.launch {
            delay(taskTimeoutMs)

            val targetTask = state.tasks.firstOrNull { it.taskId == taskId }
            if (targetTask == null || targetTask.status != TaskStatus.PENDING) {
                return@launch
            }

            val completedAt = System.currentTimeMillis()

            val updatedTasks = state.tasks.map { task ->
                if (task.taskId == taskId) {
                    task.copy(
                        status = TaskStatus.FAILED,
                        result = "Timed out waiting for mesh result",
                        completedAt = completedAt,
                        latencyMs = completedAt - task.createdAt
                    )
                } else {
                    task
                }
            }

            state = state.copy(tasks = updatedTasks)

            appendDebug("task timeout: ${targetTask.taskType}")
            logEvent("Task timeout: ${targetTask.taskType}")
        }
    }

    fun simulateLocalTask(taskType: AlphaTaskType) {
        val packet = createTaskPacket(taskType)

        val simulatedResult = MeshPacket(
            type = MeshPacketType.TASK_RESULT,
            fromNodeId = packet.fromNodeId,
            taskId = packet.taskId,
            taskType = packet.taskType,
            payload = when (taskType) {
                AlphaTaskType.DEVICE_STATUS -> "ONLINE"
                AlphaTaskType.BATTERY_STATUS -> "Battery check ready for mesh wiring"
                AlphaTaskType.LOCAL_TIMESTAMP -> System.currentTimeMillis().toString()
                AlphaTaskType.MESH_ECHO -> packet.payload ?: "EMPTY_ECHO"
                AlphaTaskType.NODE_HEALTH -> "Node healthy · peers=${state.peerCount} · mesh=${state.meshStatus}"
            },
            success = true
        )

        onTaskResult(simulatedResult)
    }

    fun logEvent(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val updated = listOf("[$timestamp] $message") + state.eventLog
        state = state.copy(eventLog = updated.take(50))
    }

    private fun appendDebug(line: String) {
        val updated = (state.debugLines + line).takeLast(30)
        state = state.copy(debugLines = updated)
    }
}
