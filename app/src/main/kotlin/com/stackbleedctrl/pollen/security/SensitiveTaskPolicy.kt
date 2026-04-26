package com.stackbleedctrl.pollen.security

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SensitiveTaskPolicy @Inject constructor() {
    @Volatile
    private var trustedPeerLabel: String = ""

    fun trustPeerLabel(label: String) {
        trustedPeerLabel = label.trim()
    }

    fun clearTrustedPeerLabel() {
        trustedPeerLabel = ""
    }

    fun isTrustedPeerLabel(label: String?): Boolean {
        if (label.isNullOrBlank()) return false
        return trustedPeerLabel.isNotBlank() && label.trim() == trustedPeerLabel
    }

    fun currentTrustedPeerLabel(): String = trustedPeerLabel
}
