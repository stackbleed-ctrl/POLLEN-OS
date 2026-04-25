package com.stackbleedctrl.pollen.version

object PollenBuildInfo {
    const val APP_VERSION_LABEL = "Alpha 0.3-dev"
    const val PROTOCOL_VERSION = "0.2"
    const val TASK_LAYER_VERSION = 1
    const val AI_LAYER_VERSION = 1

    fun protocolPayload(): String {
        return "POLLEN_PROTOCOL=$PROTOCOL_VERSION;TASK_LAYER=$TASK_LAYER_VERSION;AI_LAYER=$AI_LAYER_VERSION;APP=$APP_VERSION_LABEL"
    }
}
