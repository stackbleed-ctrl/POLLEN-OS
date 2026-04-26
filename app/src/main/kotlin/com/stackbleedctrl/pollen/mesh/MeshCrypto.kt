package com.stackbleedctrl.pollen.mesh

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object MeshCrypto {
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128
    private const val IV_BYTES = 12

    // Alpha fallback shared mesh key material.
    // Alpha 0.8 adds peer-derived key material beside this fallback.
    private const val ALPHA_KEY_MATERIAL = "POLLEN_OS_ALPHA_0_7_SHARED_MESH_KEY_V0"

    private val random = SecureRandom()

    private fun keySpec(keyMaterial: String = ALPHA_KEY_MATERIAL): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(keyMaterial.toByteArray(Charsets.UTF_8))

        return SecretKeySpec(digest, "AES")
    }

    fun peerKeyMaterial(localLabel: String, peerLabel: String): String {
        val ordered = listOf(localLabel.trim(), peerLabel.trim()).sorted()
        return "POLLEN_OS_ALPHA_0_8_PEER_KEY_V0|${ordered[0]}|${ordered[1]}"
    }

    fun encrypt(plainText: String, keyMaterial: String = ALPHA_KEY_MATERIAL): String {
        val iv = ByteArray(IV_BYTES)
        random.nextBytes(iv)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec(keyMaterial), GCMParameterSpec(GCM_TAG_BITS, iv))

        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        val combined = iv + encrypted
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decrypt(cipherText: String, keyMaterial: String = ALPHA_KEY_MATERIAL): String? {
        return try {
            val combined = Base64.decode(cipherText, Base64.NO_WRAP)

            if (combined.size <= IV_BYTES) {
                return null
            }

            val iv = combined.copyOfRange(0, IV_BYTES)
            val encrypted = combined.copyOfRange(IV_BYTES, combined.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, keySpec(keyMaterial), GCMParameterSpec(GCM_TAG_BITS, iv))

            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }
}
