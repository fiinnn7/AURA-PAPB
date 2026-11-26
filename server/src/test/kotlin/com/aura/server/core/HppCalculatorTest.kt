package com.aura.server.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HppCalculatorTest {
    @Test
    fun `harga per item rounds half up and accepts zero total`() {
        assertEquals(3L, HppCalculator.hargaPerItem(5, 2))
        assertEquals(0L, HppCalculator.hargaPerItem(0, 3))
    }

    @Test
    fun `weighted average uses half up`() {
        assertEquals(3L, HppCalculator.hppBaru(stokLama = 1, hppLama = 2, jumlahMasuk = 1, hargaPerItem = 3))
    }

    @Test
    fun `empty or negative prior stock starts with purchase price`() {
        assertEquals(25L, HppCalculator.hppBaru(stokLama = 0, hppLama = 99, jumlahMasuk = 2, hargaPerItem = 25))
        assertEquals(25L, HppCalculator.hppBaru(stokLama = -2, hppLama = 99, jumlahMasuk = 2, hargaPerItem = 25))
    }

    @Test
    fun `invalid quantity and negative amount are rejected`() {
        assertFailsWith<IllegalArgumentException> { HppCalculator.hargaPerItem(-1, 1) }
        assertFailsWith<IllegalArgumentException> { HppCalculator.hargaPerItem(1, 0) }
        assertFailsWith<IllegalArgumentException> { HppCalculator.hppBaru(1, -1, 1, 2) }
    }
}
