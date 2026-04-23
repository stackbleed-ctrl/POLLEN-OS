package com.stackbleedctrl.pollyn.oslayer

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.stackbleedctrl.pollyn.core.model.Decision
import com.stackbleedctrl.pollyn.core.model.DecisionType
import com.stackbleedctrl.pollyn.tracing.PollynTracer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActionExecutor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tracer: PollynTracer
) {
    fun execute(decision: Decision) {
        when (decision.type) {
            DecisionType.OPEN_APP -> openPackage(decision.actionPayload)
            DecisionType.BLOCK_CALL -> tracer.trace("action", "call block requested")
            DecisionType.REPLY -> tracer.trace("action", "reply requested: ${decision.actionPayload}")
            else -> tracer.trace("action", "no-op ${decision.type}")
        }
    }

    private fun openPackage(pkg: String) {
        val launch = context.packageManager.getLaunchIntentForPackage(pkg)
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launch)
        } else {
            val web = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(web)
        }
    }
}
