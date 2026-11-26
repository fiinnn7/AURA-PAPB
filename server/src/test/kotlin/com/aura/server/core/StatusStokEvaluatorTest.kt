package com.aura.server.core

import kotlin.test.Test
import kotlin.test.assertEquals

class StatusStokEvaluatorTest {
    @Test
    fun `status changes only when stock crosses minimum thresholds`() {
        assertEquals(StatusStok.HABIS, StatusStokEvaluator.evaluate(0, 0))
        assertEquals(StatusStok.MENIPIS, StatusStokEvaluator.evaluate(2, 2))
        assertEquals(StatusStok.AMAN, StatusStokEvaluator.evaluate(3, 2))
    }
}
