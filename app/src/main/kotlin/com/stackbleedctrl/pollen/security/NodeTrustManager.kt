package com.stackbleedctrl.pollen.security

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NodeTrustManager @Inject constructor() {
    private val scores = ConcurrentHashMap<String, Int>()

    fun notePeer(peerId: String, delta: Int = 1) {
        scores[peerId] = (scores[peerId] ?: 0) + delta
    }

    fun trusted(peerId: String): Boolean = (scores[peerId] ?: 0) > 0
}
