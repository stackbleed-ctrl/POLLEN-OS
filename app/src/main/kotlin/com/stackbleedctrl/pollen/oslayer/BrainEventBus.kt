package com.stackbleedctrl.pollen.oslayer

import com.stackbleedctrl.pollen.core.model.BrainDecision
import com.stackbleedctrl.pollen.core.model.PhoneEvent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed class BrainEvent {
    data class InputEvent(val event: PhoneEvent) : BrainEvent()
    data class DecisionMade(val decision: BrainDecision) : BrainEvent()
    data class MeshStatus(val text: String) : BrainEvent()
}

@Singleton
class BrainEventBus @Inject constructor() {
    private val _events = MutableSharedFlow<BrainEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<BrainEvent> = _events.asSharedFlow()
    suspend fun emit(event: BrainEvent) = _events.emit(event)
    fun tryEmit(event: BrainEvent): Boolean = _events.tryEmit(event)
}
