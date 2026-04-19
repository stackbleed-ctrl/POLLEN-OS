package com.stackbleedctrl.pollyn

import com.stackbleedctrl.pollyn.llm.RuleBasedFallbackLlm
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleBasedFallbackLlmTest {
    @Test
    fun mapsIntentRoutesToOpenApp() = runBlocking {
        val llm = RuleBasedFallbackLlm()
        val decision = llm.decide("please open maps")
        assertTrue(decision.startsWith("OPEN_APP:"))
    }
}
