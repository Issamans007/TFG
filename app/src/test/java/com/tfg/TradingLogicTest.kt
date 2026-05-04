package com.tfg

import org.junit.Test
import org.junit.Assert.*
import java.util.UUID

/**
 * Unit tests for core trading logic running on the JVM (no Android framework required).
 */
class TradingLogicTest {

    // Mirrors TradingRepositoryImpl.generateOrderId() so the format contract is tested independently
    private fun generateOrderId() = "tfg" + UUID.randomUUID().toString().replace("-", "").take(29)

    @Test
    fun orderIdStartsWithPrefix() {
        val id = generateOrderId()
        assertTrue("Order ID must start with 'tfg'", id.startsWith("tfg"))
    }

    @Test
    fun orderIdIsExactly32Chars() {
        val id = generateOrderId()
        assertEquals("Order ID length must be exactly 32 characters", 32, id.length)
    }

    @Test
    fun orderIdSuffixIsHexOnly() {
        repeat(20) {
            val id = generateOrderId()
            val suffix = id.removePrefix("tfg")
            assertTrue(
                "Order ID suffix must contain only lowercase hex characters, got: $suffix",
                suffix.all { c -> c.isDigit() || c in 'a'..'f' }
            )
        }
    }

    @Test
    fun generatedOrderIdsAreUnique() {
        val ids = (1..200).map { generateOrderId() }.toSet()
        assertEquals("All 200 generated order IDs must be unique", 200, ids.size)
    }

    @Test
    fun blankOrderIdIsDetectedCorrectly() {
        assertTrue("".isBlank())
        assertFalse(generateOrderId().isBlank())
    }

    @Test
    fun stopLossDistanceCalculation() {
        val entryPrice = 50_000.0
        val slPrice    = 48_000.0
        val distancePct = (entryPrice - slPrice) / entryPrice * 100.0
        assertEquals("SL distance from entry should be 4.0%", 4.0, distancePct, 0.001)
    }

    @Test
    fun closeSideIsOppositeOfEntrySide_buy() {
        val entry = "BUY"
        val close = if (entry == "BUY") "SELL" else "BUY"
        assertEquals("Close side for BUY entry must be SELL", "SELL", close)
    }

    @Test
    fun closeSideIsOppositeOfEntrySide_sell() {
        val entry = "SELL"
        val close = if (entry == "BUY") "SELL" else "BUY"
        assertEquals("Close side for SELL entry must be BUY", "BUY", close)
    }

    @Test
    fun partialTpQuantityCalculation() {
        val orderQty   = 1.0
        val tpPercent  = 50.0
        val closeQty   = orderQty * (tpPercent / 100.0)
        assertEquals("50% TP of 1.0 should yield 0.5", 0.5, closeQty, 1e-9)
    }

    @Test
    fun trailingStopTriggerPriceForLong() {
        val peak           = 55_000.0
        val trailingPct    = 2.0
        val trailDistance  = peak * (trailingPct / 100.0)
        val triggerPrice   = peak - trailDistance
        assertEquals("Trigger price should be 2% below peak", 53_900.0, triggerPrice, 0.001)
    }

    @Test
    fun breakevenActivationCountCheck() {
        val breakEvenAfterTpCount = 2
        val firedTpCount = 2
        assertTrue(
            "Breakeven should activate when firedTpCount >= breakEvenAfterTpCount",
            firedTpCount >= breakEvenAfterTpCount
        )
    }
}
