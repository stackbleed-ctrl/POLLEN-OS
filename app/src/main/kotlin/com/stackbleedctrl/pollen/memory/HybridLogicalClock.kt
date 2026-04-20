package com.stackbleedctrl.pollen.memory

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HybridLogicalClock @Inject constructor() {
    private var lastWall = 0L
    private var counter = 0

    @Synchronized
    fun tick(now: Long = System.currentTimeMillis()): String {
        if (now == lastWall) counter++ else { lastWall = now; counter = 0 }
        return "$lastWall:$counter"
    }
}
