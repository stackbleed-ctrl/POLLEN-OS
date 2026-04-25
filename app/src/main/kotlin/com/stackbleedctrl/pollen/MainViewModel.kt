package com.stackbleedctrl.pollen

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stackbleedctrl.pollen.ai.AiSignal
import com.stackbleedctrl.pollen.ai.AiSignalType
import com.stackbleedctrl.pollen.ai.PollenAiEngine
import com.stackbleedctrl.pollen.identity.DeviceIdProvider
import com.stackbleedctrl.pollen.mesh.MeshPacket
import com.stackbleedctrl.pollen.mesh.MeshPacketType
import com.stackbleedctrl.pollen.sdk.PollenSdk
import com.stackbleedctrl.pollen.tasks.AlphaTaskState
import com.stackbleedctrl.pollen.tasks.AlphaTaskType
import com.stackbleedctrl.pollen.tasks.TaskStatus
import com.stackbleedctrl.pollen.version.PollenBuildInfo
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
    private val aiEngine = PollenAiEngine()
    private var lastAiPeerCount: Int? = null

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

            val effectiveCount = if (count == 0 && state.lastPeerLabel.isNotBlank()) {
                1
            } else {
                count
            }

            state = state.copy(peerCount = effectiveCount)

            if (lastAiPeerCount != effectiveCount) {
                lastAiPeerCount = effectiveCount

                runAi(
                    AiSignal(
                        type = AiSignalType.PEER_COUNT_CHANGED,
                        message = "Peer count changed",
                        peerCount = effectiveCount,
                        meshStatus = state.meshStatus,
                        trustedPeerLabel = state.trustedPeerLabel
                    )
                )
            }
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
        runAi(
            AiSignal(
                type = AiSignalType.BRAIN_STARTED,
                message = "Brain started",
                peerCount = state.peerCount,
                meshStatus = state.meshStatus,
                trustedPeerLabel = state.trustedPeerLabel
            )
        )
    }

    fun permissionsReady() {
        appendDebug("permissions ready")
        logEvent("Permissions ready")
        state = state.copy(meshStatus = "Permissions ready")
        runAi(
            AiSignal(
                type = AiSignalType.PERMISSIONS_READY,
                message = "Permissions ready",
                peerCount = state.peerCount,
                meshStatus = state.meshStatus,
                trustedPeerLabel = state.trustedPeerLabel
            )
        )
    }

    fun permissionsDenied(denied: String) {
        appendDebug("permissions denied: $denied")
        logEvent("Permissions denied: $denied")
        state = state.copy(meshStatus = "Permissions missing")
        runAi(
            AiSignal(
                type = AiSignalType.PERMISSIONS_DENIED,
                message = denied,
                peerCount = state.peerCount,
                meshStatus = state.meshStatus,
                trustedPeerLabel = state.trustedPeerLabel
            )
        )
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
                AlphaTaskType.PING -> "PING from ${identity.displayName}"
                AlphaTaskType.PROTOCOL_VERSION -> "Protocol version request from ${identity.displayName}"
                AlphaTaskType.SUPPORTED_TASKS -> "Supported tasks request from ${identity.displayName}"
                AlphaTaskType.FIELD_NOTE -> "Field note from ${identity.displayName}: Alpha test note received over POLLEN mesh."
                AlphaTaskType.SIMULATED_HELP_SIGNAL -> "SIMULATION ONLY: Help signal test from ${identity.displayName}"
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
        runAi(
            AiSignal(
                type = AiSignalType.TASK_CREATED,
                message = "Task created",
                peerCount = state.peerCount,
                meshStatus = state.meshStatus,
                trustedPeerLabel = state.trustedPeerLabel,
                taskType = taskType.name
            )
        )

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
        updatePeerCompatibility(packet)

        val updatedTask = updatedTasks.firstOrNull { it.taskId == taskId }

        appendDebug("task result: ${packet.taskType} ${packet.payload}")
        logEvent("Task result: ${packet.taskType} → ${packet.payload}")
        runAi(
            AiSignal(
                type = AiSignalType.TASK_RESULT,
                message = "Task result received",
                peerCount = state.peerCount,
                meshStatus = state.meshStatus,
                trustedPeerLabel = state.trustedPeerLabel,
                taskType = packet.taskType,
                payload = packet.payload,
                success = packet.success,
                latencyMs = updatedTask?.latencyMs
            )
        )
    }




    private fun rangeProbeTaskType(): AlphaTaskType {
        return when (state.compatibilityStatus) {
            "Modern peer" -> AlphaTaskType.NODE_HEALTH
            "Compatible peer" -> AlphaTaskType.MESH_ECHO
            "Legacy echo-only peer" -> AlphaTaskType.MESH_ECHO
            "Limited / older peer" -> AlphaTaskType.MESH_ECHO
            else -> AlphaTaskType.MESH_ECHO
        }
    }

    fun runRangeProbe() {
        if (state.rangeProbeRunning) {
            appendDebug("range probe already running")
            logEvent("Range probe already running")
            return
        }

        val total = state.rangeProbeTotal

        state = state.copy(
            rangeProbeRunning = true,
            rangeProbeSent = 0
        )

        appendDebug("range probe started")
        logEvent("Range probe started: $total checks")

        viewModelScope.launch {
            repeat(total) { index ->
                sendAlphaTask(AlphaTaskType.NODE_HEALTH)

                state = state.copy(
                    rangeProbeSent = index + 1
                )

                logEvent("Range probe check ${index + 1}/$total sent")

                if (index < total - 1) {
                    delay(5_000L)
                }
            }

            delay(taskTimeoutMs + 1_000L)

            state = state.copy(
                rangeProbeRunning = false
            )

            appendDebug("range probe completed")
            logEvent("Range probe completed")
        }
    }



    private fun supportedTaskCount(): Int {
        val raw = state.peerSupportedTasks
        if (raw.isBlank() || raw == "Unknown") return 0
        return raw.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .size
    }

    private fun recommendedSafeTask(): String {
        val supported = state.peerSupportedTasks

        return when {
            supported.contains(AlphaTaskType.PING.name) -> AlphaTaskType.PING.name
            supported.contains(AlphaTaskType.MESH_ECHO.name) -> AlphaTaskType.MESH_ECHO.name
            state.compatibilityStatus.contains("Legacy", ignoreCase = true) -> AlphaTaskType.MESH_ECHO.name
            state.peerCount > 0 -> AlphaTaskType.MESH_ECHO.name
            else -> "WAIT_FOR_PEER"
        }
    }

    fun peerCapabilitySummary(): String {
        val trust = if (state.trustedPeerLabel.isBlank()) "UNTRUSTED" else "TRUSTED"
        val taskCount = supportedTaskCount()
        val safeTask = recommendedSafeTask()

        return "Peer=${state.lastPeerLabel.ifBlank { "Unknown" }} · $trust · Tasks=$taskCount · Safe=$safeTask"
    }


    fun runAlphaVerification() {
        if (state.alphaVerifyRunning) {
            appendDebug("alpha verification already running")
            logEvent("Alpha verification already running")
            return
        }

        state = state.copy(
            alphaVerifyRunning = true,
            alphaVerifyStep = 0,
            alphaVerifyTotal = 6,
            lastIntent = "Alpha Verification"
        )

        appendDebug("alpha verification started")
        logEvent("Alpha verification started")

        viewModelScope.launch {
            state = state.copy(alphaVerifyStep = 1, lastDecision = "Verify 1/6: compatibility")
            logEvent("Verify 1/6: compatibility check")
            runCompatibilityCheck()

            delay(1_700L)

            val checks = listOf(
                AlphaTaskType.PING,
                AlphaTaskType.MESH_ECHO,
                AlphaTaskType.DEVICE_STATUS,
                AlphaTaskType.FIELD_NOTE,
                recommendedSafeTask().let { safe ->
                    runCatching { AlphaTaskType.valueOf(safe) }.getOrDefault(AlphaTaskType.MESH_ECHO)
                }
            )

            checks.forEachIndexed { index, taskType ->
                val step = index + 2
                state = state.copy(
                    alphaVerifyStep = step,
                    lastDecision = "Verify $step/6: ${taskType.name}"
                )

                logEvent("Verify $step/6: ${taskType.name}")
                sendAlphaTask(taskType)
                delay(900L)
            }

            delay(taskTimeoutMs + 1_000L)

            state = state.copy(
                alphaVerifyRunning = false,
                lastDecision = "Alpha verification completed"
            )

            appendDebug("alpha verification completed")
            logEvent("Alpha verification completed")
        }
    }

    fun runDemoSequence() {
        if (state.demoSequenceRunning) {
            appendDebug("demo sequence already running")
            logEvent("Demo sequence already running")
            return
        }

        val diagnosticTask = when (state.compatibilityStatus) {
            "Modern peer" -> AlphaTaskType.NODE_HEALTH
            else -> AlphaTaskType.MESH_ECHO
        }

        val sequence = listOf(
            AlphaTaskType.PING,
            AlphaTaskType.MESH_ECHO,
            AlphaTaskType.DEVICE_STATUS,
            diagnosticTask,
            AlphaTaskType.FIELD_NOTE
        )

        state = state.copy(
            demoSequenceRunning = true,
            demoSequenceStep = 0,
            demoSequenceTotal = sequence.size,
            lastIntent = "Demo Sequence"
        )

        appendDebug("demo sequence started with compatibility=${state.compatibilityStatus}")
        logEvent("Demo sequence started: ${sequence.size} tasks · diagnostic=${diagnosticTask.name}")

        viewModelScope.launch {
            sequence.forEachIndexed { index, taskType ->
                state = state.copy(
                    demoSequenceStep = index + 1,
                    lastDecision = "Demo step ${index + 1}/${sequence.size}: ${taskType.name}"
                )

                logEvent("Demo step ${index + 1}/${sequence.size}: ${taskType.name}")
                sendAlphaTask(taskType)

                delay(900L)
            }

            delay(taskTimeoutMs + 1_000L)

            state = state.copy(
                demoSequenceRunning = false,
                lastDecision = "Demo sequence completed"
            )

            appendDebug("demo sequence completed")
            logEvent("Demo sequence completed")
        }
    }

    fun runFullMeshTest() {
        if (state.fullTestRunning) {
            appendDebug("full mesh test already running")
            logEvent("Full mesh test already running")
            return
        }

        state = state.copy(
            fullTestRunning = true,
            fullTestStartedAt = System.currentTimeMillis(),
            fullTestCompletedAt = null
        )

        appendDebug("full mesh test started")
        logEvent("Full mesh test started")

        viewModelScope.launch {
            val tests = listOf(
                AlphaTaskType.DEVICE_STATUS,
                AlphaTaskType.BATTERY_STATUS,
                AlphaTaskType.DEVICE_VITALS,
                AlphaTaskType.LOCATION_SNAPSHOT,
                AlphaTaskType.FIELD_NOTE,
                AlphaTaskType.MESH_ECHO,
                AlphaTaskType.PING,
                AlphaTaskType.PROTOCOL_VERSION,
                AlphaTaskType.SUPPORTED_TASKS,
                AlphaTaskType.NODE_HEALTH,
                AlphaTaskType.LOCAL_TIMESTAMP
            )

            tests.forEach { taskType ->
                sendAlphaTask(taskType)
                delay(750L)
            }

            delay(taskTimeoutMs + 1_000L)

            state = state.copy(
                fullTestRunning = false,
                fullTestCompletedAt = System.currentTimeMillis()
            )

            appendDebug("full mesh test completed")
            logEvent("Full mesh test completed")
        }
    }

    fun sendAlphaTask(taskType: AlphaTaskType) {
        if (taskType == AlphaTaskType.LOCATION_SNAPSHOT && state.trustedPeerLabel.isBlank()) {
            appendDebug("blocked sensitive task: LOCATION_SNAPSHOT requires trusted peer")
            logEvent("Blocked LOCATION_SNAPSHOT: trust peer first")
            runAi(
                AiSignal(
                    type = AiSignalType.SENSITIVE_TASK_NOTICE,
                    message = "Blocked LOCATION_SNAPSHOT without trusted peer",
                    peerCount = state.peerCount,
                    meshStatus = state.meshStatus,
                    trustedPeerLabel = state.trustedPeerLabel,
                    taskType = taskType.name
                )
            )
            return
        }

        logSensitiveTaskNotice(taskType)

        val packet = createTaskPacket(taskType)
        val taskId = packet.taskId

        viewModelScope.launch {
            val sent = sdk.sendMeshPacketToBestPeer(packet.toJson())

            if (sent) {
                appendDebug("targeted mesh task sent: ${packet.taskType}")
                logEvent("Targeted task sent: ${packet.taskType}")
                runAi(
                    AiSignal(
                        type = AiSignalType.TASK_SENT,
                        message = "Task sent",
                        peerCount = state.peerCount,
                        meshStatus = state.meshStatus,
                        trustedPeerLabel = state.trustedPeerLabel,
                        taskType = packet.taskType
                    )
                )
            } else {
                appendDebug("targeted mesh task failed: no peer")
                logEvent("No peer available for task: ${packet.taskType}")
                runAi(
                    AiSignal(
                        type = AiSignalType.ERROR,
                        message = "No peer available for task: ${packet.taskType}",
                        peerCount = state.peerCount,
                        meshStatus = state.meshStatus,
                        trustedPeerLabel = state.trustedPeerLabel,
                        taskType = packet.taskType
                    )
                )
            }
        }

        if (taskId != null) {
            scheduleTaskTimeout(taskId)
        }
    }

    private fun logSensitiveTaskNotice(taskType: AlphaTaskType) {
        when (taskType) {
            AlphaTaskType.LOCATION_SNAPSHOT -> {
                if (state.trustedPeerLabel.isBlank()) {
                    appendDebug("sensitive task warning: LOCATION_SNAPSHOT without trusted peer")
                    logEvent("Sensitive alpha task: LOCATION_SNAPSHOT sent without trusted peer")
                    runAi(
                        AiSignal(
                            type = AiSignalType.SENSITIVE_TASK_NOTICE,
                            message = "LOCATION_SNAPSHOT without trusted peer",
                            peerCount = state.peerCount,
                            meshStatus = state.meshStatus,
                            trustedPeerLabel = state.trustedPeerLabel,
                            taskType = taskType.name
                        )
                    )
                } else {
                    appendDebug("sensitive task approved for trusted peer: LOCATION_SNAPSHOT")
                    logEvent("Sensitive alpha task: LOCATION_SNAPSHOT for trusted peer")
                    runAi(
                        AiSignal(
                            type = AiSignalType.SENSITIVE_TASK_NOTICE,
                            message = "LOCATION_SNAPSHOT for trusted peer",
                            peerCount = state.peerCount,
                            meshStatus = state.meshStatus,
                            trustedPeerLabel = state.trustedPeerLabel,
                            taskType = taskType.name
                        )
                    )
                }
            }

            AlphaTaskType.SIMULATED_HELP_SIGNAL -> {
                appendDebug("simulation notice: SIMULATED_HELP_SIGNAL is not an emergency alert")
                logEvent("Simulation only: help signal test")
            }

            AlphaTaskType.BEACON_PEER -> {
                appendDebug("beacon notice: BEACON_PEER may trigger local vibration")
                logEvent("Beacon peer requested: local vibration alert")
            }

            else -> Unit
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
            runAi(
                AiSignal(
                    type = AiSignalType.TASK_TIMEOUT,
                    message = "Task timeout",
                    peerCount = state.peerCount,
                    meshStatus = state.meshStatus,
                    trustedPeerLabel = state.trustedPeerLabel,
                    taskType = targetTask.taskType
                )
            )
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
                AlphaTaskType.DEVICE_VITALS -> "Device vitals ready for mesh wiring"
                AlphaTaskType.BEACON_PEER -> "Beacon simulation complete"
                AlphaTaskType.LOCATION_SNAPSHOT -> "Location snapshot ready for mesh wiring"
                AlphaTaskType.FIELD_NOTE -> "Field note simulation complete"
                AlphaTaskType.SIMULATED_HELP_SIGNAL -> "SIMULATION ONLY: Help signal simulation complete"
                AlphaTaskType.LOCAL_TIMESTAMP -> System.currentTimeMillis().toString()
                AlphaTaskType.MESH_ECHO -> packet.payload ?: "EMPTY_ECHO"
                AlphaTaskType.PING -> "PONG · local simulation · peers=${state.peerCount}"
                AlphaTaskType.PROTOCOL_VERSION -> PollenBuildInfo.protocolPayload()
                AlphaTaskType.SUPPORTED_TASKS -> AlphaTaskType.entries.joinToString(",") { it.name }
                AlphaTaskType.NODE_HEALTH -> "Node healthy · peers=${state.peerCount} · mesh=${state.meshStatus}"
            },
            success = true
        )

        onTaskResult(simulatedResult)
    }



    fun completedTaskCount(): Int =
        state.tasks.count { it.status == TaskStatus.COMPLETED }

    fun failedTaskCount(): Int =
        state.tasks.count { it.status == TaskStatus.FAILED }

    fun pendingTaskCount(): Int =
        state.tasks.count { it.status == TaskStatus.PENDING }

    fun averageLatencyMs(): Long? {
        val latencies = state.tasks
            .mapNotNull { it.latencyMs }
            .filter { it >= 0L }

        if (latencies.isEmpty()) return null

        return latencies.average().toLong()
    }


    private fun runAi(signal: AiSignal) {
        val decision = aiEngine.evaluate(signal)

        val failedTasks = state.tasks.count { it.status == TaskStatus.FAILED }
        val pendingTasks = state.tasks.count { it.status == TaskStatus.PENDING }
        val healthScore = aiEngine.meshHealthScore(
            peerCount = state.peerCount,
            failedTasks = failedTasks,
            pendingTasks = pendingTasks
        )

        state = state.copy(
            aiSummary = decision.summary,
            aiRecommendedAction = decision.recommendedAction.name,
            aiConfidence = decision.confidence,
            aiHealthScore = healthScore
        )

        appendDebug("AI: ${decision.summary}")
        appendDebug("AI action: ${decision.recommendedAction}")
        appendDebug("AI health score: $healthScore")
        logEvent("AI: ${decision.summary}")
    }



    private fun refreshPeerCapabilityState() {
        state = state.copy(
            peerCapabilitySummary = peerCapabilitySummary(),
            recommendedSafeTask = recommendedSafeTask()
        )
    }

    private fun updatePeerCompatibility(packet: MeshPacket) {
        when (packet.taskType) {
            AlphaTaskType.PROTOCOL_VERSION.name -> {
                val value = packet.payload ?: "Unknown"
                state = state.copy(
                    peerProtocolVersion = value,
                    compatibilityStatus = "Protocol detected"
                )
                refreshPeerCapabilityState()
                appendDebug("peer protocol: $value")
                logEvent("Peer protocol detected: $value")
            }

            AlphaTaskType.SUPPORTED_TASKS.name -> {
                val value = packet.payload ?: "Unknown"
                val supportsPing = value.contains(AlphaTaskType.PING.name)
                val supportsEcho = value.contains(AlphaTaskType.MESH_ECHO.name)
                val supportsHealth = value.contains(AlphaTaskType.NODE_HEALTH.name)

                val status = when {
                    supportsPing && supportsEcho && supportsHealth -> "Modern peer"
                    supportsPing && supportsEcho -> "Compatible peer"
                    supportsEcho -> "Legacy echo-only peer"
                    else -> "Limited / older peer"
                }

                state = state.copy(
                    peerSupportedTasks = value,
                    compatibilityStatus = status
                )

                refreshPeerCapabilityState()
                appendDebug("peer supported tasks: $value")
                logEvent("Peer compatibility: $status")
            }
        }
    }

    fun runCompatibilityCheck() {
        appendDebug("compatibility check started")
        logEvent("Compatibility check started")
        sendAlphaTask(AlphaTaskType.PROTOCOL_VERSION)

        viewModelScope.launch {
            delay(750L)
            sendAlphaTask(AlphaTaskType.SUPPORTED_TASKS)
        }
    }

    fun buildTesterLog(): String {
        val identity = state.identity

        val taskLines = if (state.tasks.isEmpty()) {
            listOf("No tasks recorded")
        } else {
            state.tasks.take(20).map { task ->
                "- ${task.taskType} | ${task.status} | latency=${task.latencyMs ?: "-"}ms | result=${task.result ?: "-"}"
            }
        }

        val eventLines = if (state.eventLog.isEmpty()) {
            listOf("No events recorded")
        } else {
            state.eventLog.take(30)
        }

        val debugLines = if (state.debugLines.isEmpty()) {
            listOf("No debug lines recorded")
        } else {
            state.debugLines.takeLast(30)
        }

        return buildString {
            appendLine("POLLEN-OS ALPHA TESTER LOG")
            appendLine("================================")
            appendLine("Version: ${PollenBuildInfo.APP_VERSION_LABEL}")
            appendLine("Protocol: ${PollenBuildInfo.PROTOCOL_VERSION}")
            appendLine("Task layer: ${PollenBuildInfo.TASK_LAYER_VERSION}")
            appendLine("AI layer: ${PollenBuildInfo.AI_LAYER_VERSION}")
            appendLine()
            appendLine("DEVICE")
            appendLine("Name: ${identity?.displayName ?: "Unknown"}")
            appendLine("Node ID: ${identity?.nodeId ?: "Unknown"}")
            appendLine("Model: ${identity?.modelName ?: "Unknown"}")
            appendLine("Role: ${identity?.role?.name ?: "Unknown"}")
            appendLine()
            appendLine("MESH")
            appendLine("Status: ${state.meshStatus}")
            appendLine("Peer count: ${state.peerCount}")
            appendLine("Last intent: ${state.lastIntent}")
            appendLine("Last decision: ${state.lastDecision}")
            appendLine()
            appendLine("TASKS")
            taskLines.forEach { appendLine(it) }
            appendLine()
            appendLine("EVENT FEED")
            eventLines.forEach { appendLine(it) }
            appendLine()
            appendLine("DEBUG LOG")
            debugLines.forEach { appendLine(it) }
        }
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
