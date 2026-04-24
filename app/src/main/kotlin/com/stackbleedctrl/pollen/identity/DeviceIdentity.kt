package com.stackbleedctrl.pollen.identity

import android.os.Build

data class DeviceIdentity(
    val nodeId: String,
    val displayName: String,
    val modelName: String,
    val manufacturer: String,
    val role: DeviceRole = DeviceRole.WORKER
)

enum class DeviceRole {
    SEED,
    RELAY,
    WORKER,
    OBSERVER
}

fun defaultDeviceName(): String {
    val manufacturer = Build.MANUFACTURER.orEmpty()
        .replaceFirstChar { it.uppercaseChar() }

    val model = Build.MODEL.orEmpty()

    return when {
        manufacturer.isNotBlank() && model.isNotBlank() &&
            !model.lowercase().contains(manufacturer.lowercase()) -> {
            "$manufacturer $model"
        }

        model.isNotBlank() -> model

        else -> "POLLEN Node"
    }
}
