package com.aura.server.core

import java.math.BigDecimal
import java.math.RoundingMode

internal object HppCalculator {
    fun hargaPerItem(totalDibayar: Long, jumlahMasuk: Int): Long {
        require(totalDibayar >= 0) { "Total dibayar tidak boleh negatif." }
        require(jumlahMasuk > 0) { "Jumlah masuk harus lebih dari nol." }
        return BigDecimal.valueOf(totalDibayar)
            .divide(BigDecimal.valueOf(jumlahMasuk.toLong()), 0, RoundingMode.HALF_UP)
            .longValueExact()
    }

    fun hppBaru(stokLama: Int, hppLama: Long, jumlahMasuk: Int, hargaPerItem: Long): Long {
        require(hppLama >= 0 && hargaPerItem >= 0) { "HPP tidak boleh negatif." }
        require(jumlahMasuk > 0) { "Jumlah masuk harus lebih dari nol." }
        if (stokLama <= 0) return hargaPerItem
        val totalStok = stokLama.toLong() + jumlahMasuk
        val nilaiLama = BigDecimal.valueOf(stokLama.toLong()).multiply(BigDecimal.valueOf(hppLama))
        val nilaiMasuk = BigDecimal.valueOf(jumlahMasuk.toLong()).multiply(BigDecimal.valueOf(hargaPerItem))
        return nilaiLama.add(nilaiMasuk)
            .divide(BigDecimal.valueOf(totalStok), 0, RoundingMode.HALF_UP)
            .longValueExact()
    }
}
