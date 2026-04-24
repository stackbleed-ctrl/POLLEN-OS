package com.stackbleedctrl.pollen.swarm

import java.util.UUID

enum class MeshMessageType {
    HELLO,
    PING,
    INTENT,
    ACK,
    LOG,
    ROUTE
}

data class MeshMessage(
    val id: String = UUID.randomUUID().toString(),
    val type: MeshMessageType,
    val fromNodeId: String,
    val toNodeId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val ttl: Int = 5,
    val payload: String
) {
    fun encode(): String {
        return listOf(
            "POLLEN_MSG",
            id,
            type.name,
            fromNodeId,
            toNodeId ?: "*",
            timestamp.toString(),
            ttl.toString(),
            payload.replace("|", "%7C").replace("\n", "%0A")
        ).joinToString("|")
    }

    companion object {
        fun decode(raw: String): MeshMessage? {
            val parts = raw.split("|", limit = 8)
            if (parts.size != 8) return null
            if (parts[0] != "POLLEN_MSG") return null

            return try {
                MeshMessage(
                    id = parts[1],
                    type = MeshMessageType.valueOf(parts[2]),
                    fromNodeId = parts[3],
                    toNodeId = parts[4].takeIf { it != "*" },
                    timestamp = parts[5].toLong(),
                    ttl = parts[6].toInt(),
                    payload = parts[7].replace("%7C", "|").replace("%0A", "\n")
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}