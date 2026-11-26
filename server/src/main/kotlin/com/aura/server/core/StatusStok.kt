package com.aura.server.core

internal enum class StatusStok {
    AMAN,
    MENIPIS,
    HABIS,
}

internal object StatusStokEvaluator {
    fun evaluate(stokSaatIni: Int, stokMinimum: Int): StatusStok = when {
        stokSaatIni <= 0 -> StatusStok.HABIS
        stokSaatIni <= stokMinimum -> StatusStok.MENIPIS
        else -> StatusStok.AMAN
    }
}
