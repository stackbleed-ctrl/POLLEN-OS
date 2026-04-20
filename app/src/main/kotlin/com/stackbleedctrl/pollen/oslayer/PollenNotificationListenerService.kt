package com.stackbleedctrl.pollen.oslayer

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.stackbleedctrl.pollen.core.model.PhoneEvent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PollenNotificationListenerService : NotificationListenerService() {
    @Inject lateinit var bus: BrainEventBus

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        bus.tryEmit(
            BrainEvent.InputEvent(
                PhoneEvent.NotificationPosted(
                    packageName = sbn.packageName,
                    title = extras.getString("android.title"),
                    text = extras.getCharSequence("android.text")?.toString()
                )
            )
        )
    }
}
