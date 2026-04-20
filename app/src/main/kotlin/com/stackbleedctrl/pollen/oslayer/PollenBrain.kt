package com.stackbleedctrl.pollen.oslayer

import com.stackbleedctrl.pollen.core.model.BrainDecision
import com.stackbleedctrl.pollen.core.model.DecisionType
import com.stackbleedctrl.pollen.core.model.PhoneEvent
import com.stackbleedctrl.pollen.llm.LocalLlmManager
import com.stackbleedctrl.pollen.memory.CrdtMemoryStore
import com.stackbleedctrl.pollen.swarm.SwarmCoordinator
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PollenBrain @Inject constructor(
    private val llm: LocalLlmManager,
    private val memory: CrdtMemoryStore,
    private val swarm: SwarmCoordinator
) {
    suspend fun decide(event: PhoneEvent): BrainDecision {
        val context = memory.recent().joinToString("
") { it.second }
        val prompt = "Context:
$context
Event:
${event.summary()}"
        val raw = llm.decide(prompt)
        val decision = when {
            raw.startsWith("OPEN_APP:") -> BrainDecision(DecisionType.OPEN_APP, "Open app", raw.removePrefix("OPEN_APP:"))
            raw.startsWith("MESH_FORWARD:") -> BrainDecision(DecisionType.MESH_FORWARD, "Forward to nearby", raw.removePrefix("MESH_FORWARD:"))
            raw == "BLOCK_CALL" -> BrainDecision(DecisionType.BLOCK_CALL, "Block suspicious call")
            raw == "SUMMARIZE" -> BrainDecision(DecisionType.SUMMARIZE, "Summarize current context")
            else -> BrainDecision(DecisionType.LOG_ONLY, raw)
        }
        memory.append(event.summary(), decision.summary)
        if (decision.type == DecisionType.MESH_FORWARD) swarm.forwardIfPossible(decision.actionPayload)
        return decision
    }
}
