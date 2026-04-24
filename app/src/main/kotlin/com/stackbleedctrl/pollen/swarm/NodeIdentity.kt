package com.stackbleedctrl.pollen.swarm

import android.content.Context
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.absoluteValue

@Singleton
class NodeIdentity @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val nodeId: String by lazy {
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown"

        val model = android.os.Build.MODEL ?: "android"
        val shortHash = androidId.hashCode().absoluteValue.toString(16).take(6)

        "pollen-${model.replace(" ", "-")}-$shortHash"
    }

    val displayName: String by lazy {
        android.os.Build.MODEL ?: "Android Node"
    }
}