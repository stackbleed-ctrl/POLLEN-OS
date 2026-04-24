package com.stackbleedctrl.pollen.identity

import android.content.Context
import android.os.Build
import java.util.UUID

object DeviceIdProvider {

    private const val PREFS_NAME = "pollen_identity"
    private const val KEY_NODE_ID = "node_id"
    private const val KEY_DISPLAY_NAME = "display_name"

    fun getNodeId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val existing = prefs.getString(KEY_NODE_ID, null)
        if (!existing.isNullOrBlank()) return existing

        val created = "node-${UUID.randomUUID().toString().take(8)}"

        prefs.edit()
            .putString(KEY_NODE_ID, created)
            .apply()

        return created
    }

    fun getIdentity(context: Context): DeviceIdentity {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val nodeId = getNodeId(context)
        val savedName = prefs.getString(KEY_DISPLAY_NAME, null)

        val manufacturer = Build.MANUFACTURER.orEmpty()
            .replaceFirstChar { it.uppercaseChar() }

        val model = Build.MODEL.orEmpty()

        return DeviceIdentity(
            nodeId = nodeId,
            displayName = savedName ?: defaultDeviceName(),
            modelName = model.ifBlank { "Unknown Model" },
            manufacturer = manufacturer.ifBlank { "Unknown Manufacturer" },
            role = DeviceRole.WORKER
        )
    }

    fun setDisplayName(context: Context, name: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_DISPLAY_NAME, name.trim())
            .apply()
    }
}
