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
import com.stackbleedctrl.pollen.location.LocationSnapshotProvider
import com.stackbleedctrl.pollen.mesh.MeshCrypto
import com.stackbleedctrl.pollen.mesh.MeshPacket
import com.stackbleedctrl.pollen.mesh.MeshPacketType
import com.stackbleedctrl.pollen.sdk.PollenSdk
import com.stackbleedctrl.pollen.security.SensitiveTaskPolicy
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
import kotlin.math.sqrt
import kotlin.math.sin
import kotlin.math.roundToInt
import kotlin.math.pow
import kotlin.math.cos
import kotlin.math.atan2
import kotlin.math.PI

@HiltViewModel
class MainViewModel @Inject constructor(
    private val sdk: PollenSdk,
    private val sensitiveTaskPolicy: SensitiveTaskPolicy,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val taskTimeoutMs = 8_000L
    private val peerFreshMs = 10_000L
    private val aiEngine = PollenAiEngine()
    private val locationProvider = LocationSnapshotProvider(appContext)
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
            } else if (status.startsWith("POLLEN_PEER_LABEL|")) {
                val label = status.removePrefix("POLLEN_PEER_LABEL|").trim()
                if (label.isNotBlank()) {
                    val now = System.currentTimeMillis()
                    state = state.copy(
                        lastPeerLabel = label,
                        selectedPeerLabel = label,
                        selectedPeerLastSeenMs = now,
                        peerFreshnessLabel = "Fresh",
                        taskRouteReady = state.peerCount > 0
                    )
                    appendDebug("peer label updated: $label")
                    logEvent("Peer target identified: $label")
                }
            } else if (status.startsWith("POLLEN_COORDINATE_REQUEST|")) {
                val parts = status.removePrefix("POLLEN_COORDINATE_REQUEST|").split("|")
                val requester = parts.getOrNull(0).orEmpty().trim()
                val requestTaskId = parts.getOrNull(1).orEmpty().trim()

                if (requester.isNotBlank()) {
                    state = state.copy(
                        pendingCoordinateRequestLabel = requester,
                        pendingCoordinateRequestTaskId = requestTaskId,
                        pendingCoordinateRequestAt = System.currentTimeMillis(),
                        missionSummary = "Coordinate request pending approval"
                    )

                    appendDebug("coordinate request pending from: $requester")
                    logEvent("Coordinate request pending approval: $requester")
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

            updateConnectionStability(rawCount = count, effectiveCount = effectiveCount)

            val now = System.currentTimeMillis()
            val selectedLabel = when {
                effectiveCount > 0 && state.lastPeerLabel.isNotBlank() -> state.lastPeerLabel
                effectiveCount > 0 -> "Visible peer"
                else -> ""
            }

            state = state.copy(
                peerCount = effectiveCount,
                selectedPeerLabel = selectedLabel,
                selectedPeerLastSeenMs = if (effectiveCount > 0) now else state.selectedPeerLastSeenMs,
                peerFreshnessLabel = if (effectiveCount > 0) "Fresh" else "No peer",
                taskRouteReady = effectiveCount > 0
            )

            refreshPeerCapabilityState()
            refreshMissionState()

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
        refreshMissionState()
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
fun dashboardInitialized() {
    state = state.copy(
        eventLog = listOf("Alpha 0.3 dashboard initialized") + state.eventLog,
        debugLines = listOf("dashboard initialized") + state.debugLines
    )
}

fun brainServiceStarted() {
    state = state.copy(
        eventLog = listOf("Brain foreground service started") + state.eventLog,
        debugLines = listOf("foreground service start requested") + state.debugLines
    )
}
    fun permissionsReady() {
        appendDebug("permissions ready")
        logEvent("Permissions ready")
        state = state.copy(meshStatus = "Permissions ready")
        refreshMissionState()
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

    private fun refreshSecurityModeLabels() {
        val keyActive = state.trustedPeerLabel.isNotBlank() &&
            state.taskRouteReady &&
            state.peerFreshnessLabel == "Fresh"

        state = state.copy(
            keyModeLabel = if (keyActive) "Key Active" else "Unpaired",
            encryptionModeLabel = if (keyActive) "Peer-to-peer" else "Alpha fallback"
        )
    }

    private fun refreshMissionState() {
        val missionMode = when {
            state.trustedPeerLabel.isNotBlank() &&
                state.taskRouteReady &&
                state.peerFreshnessLabel == "Fresh" -> "MISSION_ACTIVE"

            state.trustedPeerLabel.isNotBlank() -> "TRUSTED"
            state.taskRouteReady && state.peerFreshnessLabel == "Fresh" -> "CONNECTED"
            state.peerCount > 0 -> "SCANNING"
            else -> "OFFLINE_READY"
        }

        val summary = when (missionMode) {
            "MISSION_ACTIVE" -> "Trusted encrypted mesh active"
            "TRUSTED" -> "Peer trusted, waiting for fresh route"
            "CONNECTED" -> "Fresh peer route available"
            "SCANNING" -> "Peer detected, establishing route"
            else -> "Ready without tower/cloud infrastructure"
        }

        state = state.copy(
            missionModeLabel = missionMode,
            infrastructureLabel = "Not required",
            missionSummary = summary
        )
    }

    private fun outboundPeerKeyMaterial(): String? {
        val identity = state.identity ?: DeviceIdProvider.getIdentity(appContext)
        val localLabel = "${identity.displayName} · ${identity.nodeId.takeLast(4)}"
        val peerLabel = state.trustedPeerLabel.ifBlank { state.selectedPeerLabel }

        if (peerLabel.isBlank()) {
            return null
        }

        if (state.trustedPeerLabel.isBlank()) {
            return null
        }

        return MeshCrypto.peerKeyMaterial(localLabel, peerLabel)
    }

    private fun requiresPeerKeyOnly(taskType: AlphaTaskType): Boolean {
        return taskType == AlphaTaskType.LOCATION_SNAPSHOT ||
            taskType == AlphaTaskType.REQUEST_COORDINATES ||
            taskType == AlphaTaskType.SHARE_COORDINATES ||
            taskType == AlphaTaskType.COORDINATE_REQUEST_DENIED
    }

    private fun hasPeerKeyReadyForSensitiveTask(): Boolean {
        return state.trustedPeerLabel.isNotBlank() &&
            state.taskRouteReady &&
            state.peerFreshnessLabel == "Fresh" &&
            outboundPeerKeyMaterial() != null
    }

    private fun peerSupportsSensitiveTask(taskType: AlphaTaskType): Boolean {
        if (!requiresPeerKeyOnly(taskType)) {
            return true
        }

        return peerSupportsTask(taskType)
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
            senderLabel = "${identity.displayName} · ${identity.nodeId.takeLast(4)}",
            payload = when (taskType) {
                AlphaTaskType.MESH_ECHO -> "POLLEN mesh echo from ${identity.displayName}"
                AlphaTaskType.PING -> "PING from ${identity.displayName}"
                AlphaTaskType.PROTOCOL_VERSION -> "Protocol version request from ${identity.displayName}"
                AlphaTaskType.SUPPORTED_TASKS -> "Supported tasks request from ${identity.displayName}"
                AlphaTaskType.FIELD_NOTE -> "Field note from ${identity.displayName}: Alpha test note received over POLLEN mesh."
                AlphaTaskType.SIMULATED_HELP_SIGNAL -> "SIMULATION ONLY: Help signal test from ${identity.displayName}"
                AlphaTaskType.MISSION_STATUS -> "Mission status request from ${identity.displayName}"
                AlphaTaskType.NODE_CHECKIN -> "Node check-in request from ${identity.displayName}"
                AlphaTaskType.REQUEST_COORDINATES -> "Coordinate request from trusted peer ${identity.displayName}. Approval required on receiving node."
                AlphaTaskType.SHARE_COORDINATES -> "Explicit coordinate share from ${identity.displayName}"
                AlphaTaskType.COORDINATE_REQUEST_DENIED -> "Coordinate request denied by ${identity.displayName}"
                AlphaTaskType.FIELD_REPORT -> "Field report from ${identity.displayName}: Alpha 1.0 mission packet."
                AlphaTaskType.RESOURCE_STATUS -> "Resource status request from ${identity.displayName}"
                AlphaTaskType.EVAC_MARKER -> "Evac marker test from ${identity.displayName}"
                else -> null
            }
        )

        val newTask = AlphaTaskState(
            taskId = taskId,
            taskType = taskType.name,
            targetNodeId = targetNodeId,
            targetPeerLabel = state.trustedPeerLabel
                .ifBlank { state.selectedPeerLabel }
                .takeIf { it.isNotBlank() },
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

        return packet.encryptPayload(outboundPeerKeyMaterial())
    }

    private fun extractCoordinateValue(payload: String?, label: String): Double? {
        if (payload.isNullOrBlank()) return null

        val pattern = Regex("$label=([-0-9.]+)")
        return pattern.find(payload)?.groupValues?.getOrNull(1)?.toDoubleOrNull()
    }

    private fun distanceMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val earthRadiusMeters = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val rLat1 = Math.toRadians(lat1)
        val rLat2 = Math.toRadians(lat2)

        val a = sin(dLat / 2).pow(2.0) +
            cos(rLat1) * cos(rLat2) * sin(dLon / 2).pow(2.0)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusMeters * c
    }

    private fun formatDistance(meters: Double): String {
        return if (meters >= 1000.0) {
            "${"%.2f".format(meters / 1000.0)} km"
        } else {
            "${meters.roundToInt()} m"
        }
    }

    private fun bearingDegrees(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val rLat1 = Math.toRadians(lat1)
        val rLat2 = Math.toRadians(lat2)
        val dLon = Math.toRadians(lon2 - lon1)

        val y = sin(dLon) * cos(rLat2)
        val x = cos(rLat1) * sin(rLat2) -
            sin(rLat1) * cos(rLat2) * cos(dLon)

        val bearing = atan2(y, x) * 180.0 / PI
        return (bearing + 360.0) % 360.0
    }

    private fun cardinalDirection(degrees: Double): String {
        val directions = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val index = (((degrees + 22.5) % 360.0) / 45.0).toInt()
        return directions[index]
    }

    private fun formatBearing(degrees: Double): String {
        return "${cardinalDirection(degrees)} · ${degrees.roundToInt()}°"
    }

    private fun updatePeerCoordinateState(packet: MeshPacket) {
        if (packet.taskType != AlphaTaskType.REQUEST_COORDINATES.name || packet.success != true) {
            return
        }

        val peerLat = extractCoordinateValue(packet.payload, "Lat")
        val peerLng = extractCoordinateValue(packet.payload, "Lng")

        if (peerLat == null || peerLng == null) {
            appendDebug("coordinate result parse skipped: missing lat/lng")
            return
        }

        val localSnapshot = locationProvider.getLastKnownLocation()

        val distanceLabel = if (localSnapshot != null) {
            formatDistance(
                distanceMeters(
                    lat1 = localSnapshot.latitude,
                    lon1 = localSnapshot.longitude,
                    lat2 = peerLat,
                    lon2 = peerLng
                )
            )
        } else {
            "Local location unavailable"
        }

        val bearingLabel = if (localSnapshot != null) {
            formatBearing(
                bearingDegrees(
                    lat1 = localSnapshot.latitude,
                    lon1 = localSnapshot.longitude,
                    lat2 = peerLat,
                    lon2 = peerLng
                )
            )
        } else {
            "Local location unavailable"
        }

        val sender = packet.senderLabel ?: packet.fromNodeId
        val coordinateLabel = "Lat=$peerLat, Lng=$peerLng"

        state = state.copy(
            lastPeerCoordinateLabel = "$sender · $coordinateLabel",
            lastPeerCoordinateDistanceLabel = distanceLabel,
            lastPeerCoordinateBearingLabel = bearingLabel,
            lastPeerCoordinateFreshnessLabel = "Received ${System.currentTimeMillis()}"
        )

        appendDebug("peer coordinates received: $coordinateLabel distance=$distanceLabel bearing=$bearingLabel")
        logEvent("Peer coordinates received: $sender · distance=$distanceLabel · bearing=$bearingLabel")
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

        if (existingTask.taskType != packet.taskType) {
            appendDebug("task result rejected: task type mismatch expected=${existingTask.taskType} got=${packet.taskType}")
            logEvent("Task result rejected: type mismatch · expected=${existingTask.taskType} got=${packet.taskType}")
            runAi(
                AiSignal(
                    type = AiSignalType.ERROR,
                    message = "Rejected task result type mismatch",
                    peerCount = state.peerCount,
                    meshStatus = state.meshStatus,
                    trustedPeerLabel = state.trustedPeerLabel,
                    taskType = packet.taskType
                )
            )
            return
        }

        if (existingTask.taskType == AlphaTaskType.LOCATION_SNAPSHOT.name ||
            existingTask.taskType == AlphaTaskType.REQUEST_COORDINATES.name ||
            existingTask.taskType == AlphaTaskType.SHARE_COORDINATES.name ||
            existingTask.taskType == AlphaTaskType.COORDINATE_REQUEST_DENIED.name
        ) {
            val expectedPeerLabel = existingTask.targetPeerLabel
            val resultSenderLabel = packet.senderLabel

            if (expectedPeerLabel.isNullOrBlank()) {
                appendDebug("sensitive result rejected: ${existingTask.taskType} missing expected peer binding")
                logEvent("Sensitive result rejected: ${existingTask.taskType} · missing expected peer binding")
                runAi(
                    AiSignal(
                        type = AiSignalType.ERROR,
                        message = "Rejected sensitive result: missing expected peer binding",
                        peerCount = state.peerCount,
                        meshStatus = state.meshStatus,
                        trustedPeerLabel = state.trustedPeerLabel,
                        taskType = packet.taskType
                    )
                )
                return
            }

            if (resultSenderLabel.isNullOrBlank()) {
                appendDebug("sensitive result rejected: ${existingTask.taskType} missing sender label")
                logEvent("Sensitive result rejected: ${existingTask.taskType} · missing sender label")
                runAi(
                    AiSignal(
                        type = AiSignalType.ERROR,
                        message = "Rejected sensitive result: missing sender label",
                        peerCount = state.peerCount,
                        meshStatus = state.meshStatus,
                        trustedPeerLabel = state.trustedPeerLabel,
                        taskType = packet.taskType
                    )
                )
                return
            }

            if (resultSenderLabel != expectedPeerLabel) {
                appendDebug("sensitive result rejected: ${existingTask.taskType} sender mismatch expected=$expectedPeerLabel got=$resultSenderLabel")
                logEvent("Sensitive result rejected: ${existingTask.taskType} · sender mismatch")
                runAi(
                    AiSignal(
                        type = AiSignalType.ERROR,
                        message = "Rejected sensitive result: sender mismatch",
                        peerCount = state.peerCount,
                        meshStatus = state.meshStatus,
                        trustedPeerLabel = state.trustedPeerLabel,
                        taskType = packet.taskType
                    )
                )
                return
            }

            appendDebug("sensitive result route-bound: ${existingTask.taskType} from $resultSenderLabel")
            logEvent("Sensitive result route-bound: ${existingTask.taskType} · $resultSenderLabel")
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
        updatePeerCoordinateState(packet)

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


    private fun peerSupportsTask(taskType: AlphaTaskType): Boolean {
        val raw = state.peerSupportedTasks
        if (raw.isBlank() || raw == "Unknown") return false

        return raw.split(",")
            .map { it.trim() }
            .any { it == taskType.name }
    }

    private fun safeTaskOrEcho(taskType: AlphaTaskType): AlphaTaskType {
        return if (peerSupportsTask(taskType)) {
            taskType
        } else {
            AlphaTaskType.MESH_ECHO
        }
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
                safeTaskOrEcho(AlphaTaskType.PING),
                AlphaTaskType.MESH_ECHO,
                safeTaskOrEcho(AlphaTaskType.DEVICE_STATUS),
                safeTaskOrEcho(AlphaTaskType.FIELD_NOTE),
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
            safeTaskOrEcho(AlphaTaskType.PING),
            AlphaTaskType.MESH_ECHO,
            safeTaskOrEcho(AlphaTaskType.DEVICE_STATUS),
            safeTaskOrEcho(diagnosticTask),
            safeTaskOrEcho(AlphaTaskType.FIELD_NOTE)
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

    private fun sendCoordinateDenialResult(
        requester: String,
        requestTaskId: String
    ) {
        if (requestTaskId.isBlank() || requestTaskId == "unknown") {
            appendDebug("coordinate denial result blocked: missing original task id")
            logEvent("Coordinate denial result blocked: missing original task id")
            return
        }

        val peerKeyMaterial = outboundPeerKeyMaterial()

        if (peerKeyMaterial.isNullOrBlank()) {
            appendDebug("coordinate denial result blocked: peer-key unavailable")
            logEvent("Coordinate denial result blocked: peer-key unavailable")
            return
        }

        val identity = state.identity ?: DeviceIdProvider.getIdentity(appContext)
        val localLabel = "${identity.displayName} · ${identity.nodeId.takeLast(4)}"

        val denialResult = MeshPacket(
            type = MeshPacketType.TASK_RESULT,
            fromNodeId = identity.nodeId,
            taskId = requestTaskId,
            taskType = AlphaTaskType.REQUEST_COORDINATES.name,
            senderLabel = localLabel,
            payload = "COORDINATE_REQUEST_DENIED · $localLabel declined to share coordinates with $requester",
            success = false
        ).encryptPayload(peerKeyMaterial)

        if (!denialResult.usesPeerKey()) {
            appendDebug("coordinate denial result blocked: result was not peer-key encrypted")
            logEvent("Coordinate denial result blocked: result was not peer-key encrypted")
            return
        }

        viewModelScope.launch {
            val sent = sdk.sendMeshPacketToBestPeer(denialResult.toJson())

            if (sent) {
                appendDebug("coordinate denial result sent for request=$requestTaskId")
                logEvent("Coordinate denial sent: request=$requestTaskId")
            } else {
                appendDebug("coordinate denial result failed: no peer route")
                logEvent("Coordinate denial failed: no peer route")
            }
        }
    }

    private fun sendCoordinateShareResult(
        requester: String,
        requestTaskId: String
    ) {
        if (requestTaskId.isBlank() || requestTaskId == "unknown") {
            appendDebug("coordinate share result blocked: missing original task id")
            logEvent("Coordinate share result blocked: missing original task id")
            return
        }

        val peerKeyMaterial = outboundPeerKeyMaterial()

        if (peerKeyMaterial.isNullOrBlank()) {
            appendDebug("coordinate share result blocked: peer-key unavailable")
            logEvent("Coordinate share result blocked: peer-key unavailable")
            return
        }

        val identity = state.identity ?: DeviceIdProvider.getIdentity(appContext)
        val localLabel = "${identity.displayName} · ${identity.nodeId.takeLast(4)}"
        val snapshot = locationProvider.getLastKnownLocation()

        val shareResult = MeshPacket(
            type = MeshPacketType.TASK_RESULT,
            fromNodeId = identity.nodeId,
            taskId = requestTaskId,
            taskType = AlphaTaskType.REQUEST_COORDINATES.name,
            senderLabel = localLabel,
            payload = snapshot?.toDisplayString()
                ?: "Coordinates unavailable: permission missing or no last known fix",
            success = snapshot != null
        ).encryptPayload(peerKeyMaterial)

        if (!shareResult.usesPeerKey()) {
            appendDebug("coordinate share result blocked: result was not peer-key encrypted")
            logEvent("Coordinate share result blocked: result was not peer-key encrypted")
            return
        }

        viewModelScope.launch {
            val sent = sdk.sendMeshPacketToBestPeer(shareResult.toJson())

            if (sent) {
                appendDebug("coordinate share result sent for request=$requestTaskId")
                logEvent("Coordinate share sent: request=$requestTaskId")
            } else {
                appendDebug("coordinate share result failed: no peer route")
                logEvent("Coordinate share failed: no peer route")
            }
        }
    }

    fun shareCoordinatesForPendingRequest() {
        val requester = state.pendingCoordinateRequestLabel
        val requestTaskId = state.pendingCoordinateRequestTaskId

        if (requester.isBlank()) {
            appendDebug("share coordinates blocked: no pending coordinate request")
            logEvent("Share coordinates blocked: no pending request")
            return
        }

        appendDebug("coordinate request approved for: $requester")
        logEvent("Coordinate request approved: $requester")

        sendCoordinateShareResult(
            requester = requester,
            requestTaskId = requestTaskId
        )

        state = state.copy(
            pendingCoordinateRequestLabel = "",
            pendingCoordinateRequestTaskId = "",
            pendingCoordinateRequestAt = null,
            missionSummary = "Coordinate request approved and share attempted"
        )
    }

    fun denyPendingCoordinateRequest() {
        val requester = state.pendingCoordinateRequestLabel.ifBlank { "unknown requester" }
        val requestTaskId = state.pendingCoordinateRequestTaskId

        appendDebug("coordinate request denied: $requester")
        logEvent("Coordinate request denied: $requester")

        sendCoordinateDenialResult(
            requester = requester,
            requestTaskId = requestTaskId
        )

        state = state.copy(
            pendingCoordinateRequestLabel = "",
            pendingCoordinateRequestTaskId = "",
            pendingCoordinateRequestAt = null,
            missionSummary = "Coordinate request denied and response attempted"
        )

        runAi(
            AiSignal(
                type = AiSignalType.SENSITIVE_TASK_NOTICE,
                message = "Coordinate request denied",
                peerCount = state.peerCount,
                meshStatus = state.meshStatus,
                trustedPeerLabel = state.trustedPeerLabel,
                taskType = AlphaTaskType.REQUEST_COORDINATES.name
            )
        )
    }

    fun sendAlphaTask(taskType: AlphaTaskType) {
        if (requiresPeerKeyOnly(taskType) && !hasPeerKeyReadyForSensitiveTask()) {
            appendDebug("blocked sensitive task: ${taskType.name} requires trusted peer + fresh route + peer-key mode")
            logEvent("Sensitive task blocked: ${taskType.name} · requires trusted peer + fresh route + peer-key encryption")
            runAi(
                AiSignal(
                    type = AiSignalType.SENSITIVE_TASK_NOTICE,
                    message = "Blocked ${taskType.name}: peer-key-only enforcement failed",
                    peerCount = state.peerCount,
                    meshStatus = state.meshStatus,
                    trustedPeerLabel = state.trustedPeerLabel,
                    taskType = taskType.name
                )
            )
            return
        }

        if (requiresPeerKeyOnly(taskType) && !peerSupportsSensitiveTask(taskType)) {
            appendDebug("blocked sensitive task: ${taskType.name} not advertised by peer")
            logEvent("Sensitive task blocked: ${taskType.name} · peer does not advertise support")
            runAi(
                AiSignal(
                    type = AiSignalType.SENSITIVE_TASK_NOTICE,
                    message = "Blocked ${taskType.name}: peer does not advertise support",
                    peerCount = state.peerCount,
                    meshStatus = state.meshStatus,
                    trustedPeerLabel = state.trustedPeerLabel,
                    taskType = taskType.name
                )
            )
            return
        }

        logSensitiveTaskNotice(taskType)

        if (!hasUsablePeerForTask(taskType)) {
            appendDebug("blocked task: ${taskType.name} / no usable peer")
            logEvent("Blocked task: ${taskType.name} · waiting for peer")

            runAi(
                AiSignal(
                    type = AiSignalType.ERROR,
                    message = "No usable peer for task: ${taskType.name}",
                    peerCount = state.peerCount,
                    meshStatus = state.meshStatus,
                    trustedPeerLabel = state.trustedPeerLabel,
                    taskType = taskType.name
                )
            )

            return
        }

        val outboundTaskType = routeTaskTypeForSend(taskType)

        if (requiresPeerKeyOnly(taskType) && outboundTaskType != taskType) {
            appendDebug("blocked sensitive task downgrade: requested=${taskType.name} routed=${outboundTaskType.name}")
            logEvent("Sensitive task blocked: ${taskType.name} · no fallback/downgrade allowed")
            runAi(
                AiSignal(
                    type = AiSignalType.SENSITIVE_TASK_NOTICE,
                    message = "Blocked ${taskType.name}: sensitive task downgrade rejected",
                    peerCount = state.peerCount,
                    meshStatus = state.meshStatus,
                    trustedPeerLabel = state.trustedPeerLabel,
                    taskType = taskType.name
                )
            )
            return
        }

        val packet = createTaskPacket(outboundTaskType)

        if (requiresPeerKeyOnly(taskType) && !packet.usesPeerKey()) {
            appendDebug("blocked sensitive task: ${taskType.name} packet was not peer-key encrypted")
            logEvent("Sensitive task blocked: ${taskType.name} · packet encryption mode=${packet.encryptionMode}")
            return
        }

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


    private fun hasUsablePeerForTask(taskType: AlphaTaskType): Boolean {
        val lastSeen = state.selectedPeerLastSeenMs
        val fresh = lastSeen != null && System.currentTimeMillis() - lastSeen <= peerFreshMs

        if (state.peerCount > 0 && fresh) {
            state = state.copy(
                peerFreshnessLabel = "Fresh",
                taskRouteReady = true
            )
            return true
        }

        if (state.peerCount > 0 && !fresh) {
            state = state.copy(
                peerFreshnessLabel = "Stale",
                taskRouteReady = false
            )
            appendDebug("blocked task: ${taskType.name} / peer stale")
            logEvent("Blocked task: ${taskType.name} · peer target stale")
            return false
        }

        if (state.lastPeerLabel.isNotBlank() && fresh) {
            appendDebug("peer fallback available for ${taskType.name}: ${state.lastPeerLabel}")
            state = state.copy(
                selectedPeerLabel = state.lastPeerLabel,
                peerFreshnessLabel = "Fresh",
                taskRouteReady = true
            )
            return true
        }

        state = state.copy(
            peerFreshnessLabel = "No peer",
            taskRouteReady = false
        )

        return false
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
                    appendDebug("sensitive task approved: LOCATION_SNAPSHOT for ${state.trustedPeerLabel}")
                    logEvent("Sensitive task approved: LOCATION_SNAPSHOT · trusted peer ${state.trustedPeerLabel}")
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
                logEvent("Simulation only: SIMULATED_HELP_SIGNAL is not an emergency alert")
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
                AlphaTaskType.MISSION_STATUS -> "MISSION_READY · local simulation · infrastructure=not required"
                AlphaTaskType.NODE_CHECKIN -> "CHECKIN_OK · local simulation · peers=${state.peerCount}"
                AlphaTaskType.REQUEST_COORDINATES -> "Coordinate request simulation complete"
                AlphaTaskType.SHARE_COORDINATES -> "Coordinate share simulation complete"
                AlphaTaskType.COORDINATE_REQUEST_DENIED -> "COORDINATE_REQUEST_DENIED · local simulation"
                AlphaTaskType.FIELD_REPORT -> "FIELD_REPORT_ACK · local simulation"
                AlphaTaskType.RESOURCE_STATUS -> "RESOURCE_STATUS · local simulation · battery/status ready"
                AlphaTaskType.EVAC_MARKER -> "EVAC_MARKER_ACK · local simulation"
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

    fun trustSelectedPeer() {
        val label = state.selectedPeerLabel.ifBlank { state.lastPeerLabel }

        if (label.isBlank()) {
            appendDebug("trust peer blocked: no selected peer")
            logEvent("Trust blocked: no selected peer")
            return
        }

        if (!state.taskRouteReady || state.peerFreshnessLabel != "Fresh") {
            appendDebug("trust peer blocked: route not ready for $label")
            logEvent("Trust blocked: $label is not fresh/route-ready")
            return
        }

        state = state.copy(
            trustedPeerLabel = label
        )
        sensitiveTaskPolicy.trustPeerLabel(label)
        refreshSecurityModeLabels()
        refreshMissionState()

        appendDebug("trusted peer set: $label")
        logEvent("Trusted peer set: $label")

        runAi(
            AiSignal(
                type = AiSignalType.TRUST_CHANGED,
                message = "Trusted peer set",
                peerCount = state.peerCount,
                meshStatus = state.meshStatus,
                trustedPeerLabel = state.trustedPeerLabel
            )
        )
    }

    fun clearTrustedPeer() {
        val oldLabel = state.trustedPeerLabel

        state = state.copy(
            trustedPeerLabel = ""
        )
        sensitiveTaskPolicy.clearTrustedPeerLabel()
        refreshSecurityModeLabels()
        refreshMissionState()

        appendDebug("trusted peer cleared: ${oldLabel.ifBlank { "none" }}")
        logEvent("Trusted peer cleared")

        runAi(
            AiSignal(
                type = AiSignalType.TRUST_CHANGED,
                message = "Trusted peer cleared",
                peerCount = state.peerCount,
                meshStatus = state.meshStatus,
                trustedPeerLabel = state.trustedPeerLabel
            )
        )
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


    private fun formatDuration(ms: Long?): String {
        if (ms == null || ms <= 0L) return "Unknown"

        val totalSeconds = ms / 1000L
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L

        return if (minutes > 0) {
            "${minutes}m ${seconds}s"
        } else {
            "${seconds}s"
        }
    }

    private fun currentConnectionUptimeMs(): Long? {
        val startedAt = state.connectionStartedAt ?: return null
        return System.currentTimeMillis() - startedAt
    }

    private fun updateConnectionStability(rawCount: Int, effectiveCount: Int) {
        val now = System.currentTimeMillis()
        val wasConnected = state.peerCount > 0
        val isConnected = effectiveCount > 0

        state = when {
            !wasConnected && isConnected -> {
                state.copy(
                    connectionStartedAt = state.connectionStartedAt ?: now,
                    lastReconnectedAt = now,
                    reconnectCount = if (state.connectionStartedAt == null) {
                        state.reconnectCount
                    } else {
                        state.reconnectCount + 1
                    }
                )
            }

            wasConnected && rawCount == 0 && state.lastPeerLabel.isBlank() -> {
                state.copy(
                    lastDisconnectedAt = now
                )
            }

            else -> state
        }
    }

    private fun routeTaskTypeForSend(requested: AlphaTaskType): AlphaTaskType {
        val alwaysAllowed = setOf(
            AlphaTaskType.MESH_ECHO,
            AlphaTaskType.PROTOCOL_VERSION,
            AlphaTaskType.SUPPORTED_TASKS
        )

        if (requested in alwaysAllowed) return requested

        val knownCapabilities = state.peerSupportedTasks.isNotBlank() && state.peerSupportedTasks != "Unknown"

        if (knownCapabilities && !peerSupportsTask(requested)) {
            appendDebug("task fallback: ${requested.name} -> MESH_ECHO")
            logEvent("Fallback task: ${requested.name} unsupported, using MESH_ECHO")
            return AlphaTaskType.MESH_ECHO
        }

        if (!knownCapabilities && state.compatibilityStatus == "Not checked") {
            if (requested == AlphaTaskType.PING || requested == AlphaTaskType.NODE_HEALTH || requested == AlphaTaskType.LOCAL_TIMESTAMP) {
                appendDebug("task fallback: ${requested.name} -> MESH_ECHO while compatibility unknown")
                logEvent("Compatibility unknown: using safe MESH_ECHO instead of ${requested.name}")
                return AlphaTaskType.MESH_ECHO
            }
        }

        return requested
    }

    fun startFieldTest() {
        if (state.fieldTestRunning) {
            appendDebug("field test already running")
            logEvent("Field test already running")
            return
        }

        state = state.copy(
            fieldTestRunning = true,
            fieldTestStartedAt = System.currentTimeMillis(),
            fieldTestEndedAt = null,
            fieldTestCheckCount = 0,
            fieldTestDistanceLabel = "Start point",
            fieldTestEnvironment = "Indoor/outdoor",
            lastIntent = "Field Distance Test"
        )

        appendDebug("field distance test started")
        logEvent("Field distance test started")
    }

    fun markFieldDistanceCheck() {
        val nextCheck = state.fieldTestCheckCount + 1
        val label = when (nextCheck) {
            1 -> "50 ft"
            2 -> "100 ft"
            3 -> "250 ft"
            4 -> "500 ft"
            5 -> "750 ft"
            else -> "Extended range"
        }

        state = state.copy(
            fieldTestCheckCount = nextCheck,
            fieldTestDistanceLabel = label
        )

        appendDebug("field distance check $nextCheck at $label")
        logEvent("Field distance check $nextCheck: $label · peers=${state.peerCount} · health=${state.aiHealthScore}/100")

        sendAlphaTask(AlphaTaskType.MESH_ECHO)
    }

    fun endFieldTest() {
        if (!state.fieldTestRunning) {
            appendDebug("field test not running")
            logEvent("Field test not running")
            return
        }

        state = state.copy(
            fieldTestRunning = false,
            fieldTestEndedAt = System.currentTimeMillis()
        )

        appendDebug("field distance test ended")
        logEvent("Field distance test ended after ${state.fieldTestCheckCount} checks")
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
            appendLine("Selected peer: ${state.selectedPeerLabel.ifBlank { "None" }}")
            appendLine("Peer freshness: ${state.peerFreshnessLabel}")
            appendLine("Task route ready: ${state.taskRouteReady}")
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
