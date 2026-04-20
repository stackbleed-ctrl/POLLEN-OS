package com.stackbleedctrl.pollen.core.model

sealed class PhoneEvent {
    data class NotificationPosted(
        val packageName: String,
        val title: String?,
        val text: String?
    ) : PhoneEvent()

    data class UserIntent(val raw: String) : PhoneEvent()
    data class MeshMessage(val fromPeer: String, val payload: String) : PhoneEvent()
    data class IncomingCall(val number: String?) : PhoneEvent()

    fun summary(): String = when (this) {
        is NotificationPosted -> "Notification from $packageName"
        is UserIntent -> raw
        is MeshMessage -> "Mesh from $fromPeer"
        is IncomingCall -> "Incoming call ${number ?: "unknown"}"
    }
}
