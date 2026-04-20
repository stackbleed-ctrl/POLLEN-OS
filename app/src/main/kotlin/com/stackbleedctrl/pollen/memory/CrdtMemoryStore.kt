package com.stackbleedctrl.pollen.memory

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class CrdtMemoryStore @Inject constructor(
    private val hlc: HybridLogicalClock
) {
    private val lock = Mutex()
    private val entries = linkedMapOf<String, String>()

    suspend fun append(key: String, value: String) {
        lock.withLock {
            entries["${hlc.tick()}::$key"] = value
        }
    }

    suspend fun recent(limit: Int = 20): List<Pair<String, String>> = lock.withLock {
        entries.entries.toList().takeLast(limit).map { it.toPair() }
    }
}
