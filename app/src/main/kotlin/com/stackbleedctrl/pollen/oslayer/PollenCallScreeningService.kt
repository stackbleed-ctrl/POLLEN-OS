package com.stackbleedctrl.pollen.oslayer

import android.telecom.Call
import android.telecom.CallScreeningService
import com.stackbleedctrl.pollen.core.model.PhoneEvent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PollenCallScreeningService : CallScreeningService() {
    @Inject lateinit var bus: BrainEventBus

    override fun onScreenCall(callDetails: Call.Details) {
        val number = callDetails.handle?.schemeSpecificPart
        bus.tryEmit(BrainEvent.InputEvent(PhoneEvent.IncomingCall(number)))
        respondToCall(callDetails, CallResponse.Builder().setDisallowCall(false).setRejectCall(false).build())
    }
}
