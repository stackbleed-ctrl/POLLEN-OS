package com.stackbleed.pollen.oslayer

import android.telecom.CallScreeningService

class PollynCallScreeningService : CallScreeningService() {
    override fun onScreenCall(callDetails: Call.Details) {
        respondToCall(callDetails, CallResponse.Builder().build())
    }
}