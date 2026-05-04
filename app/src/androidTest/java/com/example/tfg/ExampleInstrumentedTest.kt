package com.tfg

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * Instrumented tests that verify app-level correctness on a real device / emulator.
 */
@RunWith(AndroidJUnit4::class)
class TradingAppInstrumentedTest {

    @Test
    fun appPackageName_isCorrect() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals(
            "Application ID must be com.tfg.tradeforgood — was the manifest namespace changed?",
            "com.tfg.tradeforgood",
            ctx.packageName
        )
    }

    @Test
    fun targetContext_isNotNull() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        assertNotNull("Target context must not be null", ctx)
    }
}
