package com.stackbleedctrl.pollen.oslayer

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Unit tests for IntentRouter keyword classification.
 * Does not require Android context — tests the pure Kotlin classifier only.
 */
class IntentRouterTest {

    // Reflection accessor to test the private KeywordClassifier
    // Replace with package-internal visibility in production if preferred.
    private fun classify(input: String): Pair<IntentRouter.IntentCategory, Float> {
        val cls = Class.forName("com.stackbleedctrl.pollen.oslayer.IntentRouter\$KeywordClassifier")
        val instance = cls.getDeclaredField("INSTANCE").also { it.isAccessible = true }.get(null)
        val method = cls.getDeclaredMethod("classify", String::class.java).also { it.isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        return method.invoke(instance, input) as Pair<IntentRouter.IntentCategory, Float>
    }

    @Test
    fun `messaging intent classified correctly`() {
        val (cat, conf) = classify("tell everyone i'm late")
        assertEquals(IntentRouter.IntentCategory.MESSAGING, cat)
        assert(conf > 0.5f) { "Expected confidence > 0.5, got $conf" }
    }

    @Test
    fun `summarise intent classified correctly`() {
        val (cat, _) = classify("handle everything i missed today")
        assertEquals(IntentRouter.IntentCategory.SUMMARISE, cat)
    }

    @Test
    fun `calendar intent classified correctly`() {
        val (cat, _) = classify("reschedule my meeting")
        assertEquals(IntentRouter.IntentCategory.CALENDAR, cat)
    }

    @Test
    fun `calls intent classified correctly`() {
        val (cat, _) = classify("mute spam callers forever")
        assertEquals(IntentRouter.IntentCategory.CALLS, cat)
    }

    @Test
    fun `travel intent classified correctly`() {
        val (cat, _) = classify("find cheapest flight to new york")
        assertEquals(IntentRouter.IntentCategory.TRAVEL, cat)
    }

    @Test
    fun `unknown intent returns low confidence`() {
        val (cat, conf) = classify("xyzzy frobnicate wibble")
        assertEquals(IntentRouter.IntentCategory.UNKNOWN, cat)
        assertEquals(0.0f, conf)
    }

    @Test
    fun `briefing intent not confused with messaging`() {
        val (cat, _) = classify("summarize today for me")
        assertNotEquals(IntentRouter.IntentCategory.MESSAGING, cat)
        assertEquals(IntentRouter.IntentCategory.SUMMARISE, cat)
    }
}
