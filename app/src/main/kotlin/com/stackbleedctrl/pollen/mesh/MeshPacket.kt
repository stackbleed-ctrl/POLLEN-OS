package com.stackbleedctrl.pollen.mesh

import org.json.JSONObject
import java.util.UUID

enum class MeshPacketType {
    PING,
    PONG,
    TASK_REQUEST,
    TASK_RESULT,
    NODE_HEALTH
}

data class MeshPacket(
    val packetId: String = UUID.randomUUID().toString(),
    val type: MeshPacketType,
    val fromNodeId: String,
    val toNodeId: String? = null,
    val taskId: String? = null,
    val taskType: String? = null,
    val payload: String? = null,
    val success: Boolean? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toJson(): String {
        return JSONObject().apply {
            put("packetId", packetId)
            put("type", type.name)
            put("fromNodeId", fromNodeId)
            put("toNodeId", toNodeId)
            put("taskId", taskId)
            put("taskType", taskType)
            put("payload", payload)
            put("success", success)
            put("createdAt", createdAt)
        }.toString()
    }

    companion object {
        fun fromJson(raw: String): MeshPacket? {
            return try {
                val json = JSONObject(raw)

                MeshPacket(
                    packetId = json.optString("packetId"),
                    type = MeshPacketType.valueOf(json.optString("type")),
                    fromNodeId = json.optString("fromNodeId"),
                    toNodeId = json.optString("toNodeId").takeIf { it.isNotBlank() && it != "null" },
                    taskId = json.optString("taskId").takeIf { it.isNotBlank() && it != "null" },
                    taskType = json.optString("taskType").takeIf { it.isNotBlank() && it != "null" },
                    payload = json.optString("payload").takeIf { it.isNotBlank() && it != "null" },
                    success = if (json.has("success") && !json.isNull("success")) {
                        json.optBoolean("success")
                    } else {
                        null
                    },
                    createdAt = json.optLong("createdAt", System.currentTimeMillis())
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}
