package com.stackbleedctrl.pollen.mesh

import org.json.JSONObject
import java.security.MessageDigest
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
    val createdAt: Long = System.currentTimeMillis(),
    val protocolVersion: String = "0.6",
    val senderLabel: String? = null,
    val packetNonce: String = UUID.randomUUID().toString(),
    val integrityTag: String? = null,
    val encryptionMode: String = "none"
) {
    fun expectedIntegrityTag(): String {
        return computeIntegrityTag(
            packetId = packetId,
            type = type.name,
            fromNodeId = fromNodeId,
            toNodeId = toNodeId,
            taskId = taskId,
            taskType = taskType,
            payload = payload,
            success = success,
            createdAt = createdAt,
            protocolVersion = protocolVersion,
            senderLabel = senderLabel,
            packetNonce = packetNonce
        )
    }

    fun isEncrypted(): Boolean {
        return encryptionMode == "aes-gcm-v0"
    }

    fun encryptPayload(): MeshPacket {
        val plain = payload ?: return this

        if (isEncrypted()) {
            return this
        }

        return copy(
            payload = MeshCrypto.encrypt(plain),
            encryptionMode = "aes-gcm-v0",
            integrityTag = null
        )
    }

    fun decryptPayload(): MeshPacket? {
        if (!isEncrypted()) {
            return this
        }

        val cipherText = payload ?: return null
        val plainText = MeshCrypto.decrypt(cipherText) ?: return null

        return copy(
            payload = plainText,
            encryptionMode = "none",
            integrityTag = null
        )
    }

    fun resolvedIntegrityTag(): String {
        return integrityTag ?: expectedIntegrityTag()
    }

    fun hasIntegrityTag(): Boolean {
        return integrityTag?.isNotBlank() == true
    }

    fun integrityValid(): Boolean {
        return integrityTag == null || integrityTag == expectedIntegrityTag()
    }

    fun ageMs(now: Long = System.currentTimeMillis()): Long {
        return now - createdAt
    }

    fun isStale(maxAgeMs: Long): Boolean {
        return ageMs() > maxAgeMs
    }

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
            put("createdAtMs", createdAt)
            put("protocolVersion", protocolVersion)
            put("senderLabel", senderLabel)
            put("packetNonce", packetNonce)
            put("integrityTag", resolvedIntegrityTag())
            put("encryptionMode", encryptionMode)
        }.toString()
    }

    companion object {
        fun fromJson(raw: String): MeshPacket? {
            return try {
                val json = JSONObject(raw)

                val parsedPacketId = json.optString("packetId")
                    .takeIf { it.isNotBlank() && it != "null" }
                    ?: UUID.randomUUID().toString()

                val createdAtValue = when {
                    json.has("createdAtMs") -> json.optLong("createdAtMs", System.currentTimeMillis())
                    else -> json.optLong("createdAt", System.currentTimeMillis())
                }

                MeshPacket(
                    packetId = parsedPacketId,
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
                    createdAt = createdAtValue,
                    protocolVersion = json.optString("protocolVersion", "legacy"),
                    senderLabel = json.optString("senderLabel").takeIf { it.isNotBlank() && it != "null" },
                    packetNonce = json.optString("packetNonce")
                        .takeIf { it.isNotBlank() && it != "null" }
                        ?: UUID.randomUUID().toString(),
                    integrityTag = json.optString("integrityTag").takeIf { it.isNotBlank() && it != "null" },
                    encryptionMode = json.optString("encryptionMode", "none")
                )
            } catch (_: Exception) {
                null
            }
        }

        private fun computeIntegrityTag(
            packetId: String,
            type: String,
            fromNodeId: String,
            toNodeId: String?,
            taskId: String?,
            taskType: String?,
            payload: String?,
            success: Boolean?,
            createdAt: Long,
            protocolVersion: String,
            senderLabel: String?,
            packetNonce: String
        ): String {
            val source = listOf(
                packetId,
                type,
                fromNodeId,
                toNodeId.orEmpty(),
                taskId.orEmpty(),
                taskType.orEmpty(),
                payload.orEmpty(),
                success?.toString().orEmpty(),
                createdAt.toString(),
                protocolVersion,
                senderLabel.orEmpty(),
                packetNonce
            ).joinToString("|")

            val digest = MessageDigest.getInstance("SHA-256")
                .digest(source.toByteArray(Charsets.UTF_8))

            return digest.joinToString("") { "%02x".format(it) }.take(24)
        }
    }
}
