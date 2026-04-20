package com.stackbleedctrl.pollen.oslayer

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class BrainEventBusTest {

    @Test
    fun `emitted event is received by observer`() = runTest {
        val bus = BrainEventBus()
        val expected = BrainEvent.UrgentMessage(
            senderName  = "Alice",
            preview     = "Are you coming?",
            channelHint = "sms"
        )

        launch { bus.emit(expected) }

        val received = bus.events.first()
        assertEquals(expected, received)
    }

    @Test
    fun `tryEmit returns true when buffer available`() {
        val bus = BrainEventBus()
        val result = bus.tryEmit(BrainEvent.SpamCallBlocked("+1234567890", "known_spam_list"))
        assert(result) { "tryEmit should succeed with available buffer capacity" }
    }

    @Test
    fun `missed day event carries correct gap`() = runTest {
        val bus = BrainEventBus()
        val gapMs = 72_000_000L

        launch {
            bus.emit(BrainEvent.MissedDay(
                gapMs          = gapMs,
                pendingIntents = emptyList()
            ))
        }

        val event = bus.events.first() as BrainEvent.MissedDay
        assertEquals(gapMs, event.gapMs)
    }
}
