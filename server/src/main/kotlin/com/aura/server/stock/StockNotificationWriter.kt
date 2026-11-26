package com.aura.server.stock

import com.aura.server.core.StatusStok
import com.aura.server.db.NotifikasiTable
import org.jetbrains.exposed.v1.jdbc.insert
import java.util.UUID
import kotlin.uuid.toKotlinUuid

internal fun createStockNotification(
    storeId: String,
    barangId: UUID,
    namaBarang: String,
    stock: Int,
    previous: StatusStok,
    current: StatusStok,
    now: Long,
): NotifikasiStokResponse? {
    if (previous == current || current == StatusStok.AMAN) return null
    val type = if (current == StatusStok.HABIS) "STOK_HABIS" else "STOK_MENIPIS"
    val title = if (current == StatusStok.HABIS) "Stok habis" else "Stok menipis"
    val message = if (current == StatusStok.HABIS) {
        "$namaBarang tidak memiliki stok."
    } else {
        "Stok $namaBarang tersisa $stock."
    }
    val id = UUID.randomUUID()
    NotifikasiTable.insert {
        it[NotifikasiTable.id] = id.toKotlinUuid()
        it[idToko] = storeId
        it[jenis] = type
        it[judul] = title
        it[pesan] = message
        it[referensiId] = barangId.toKotlinUuid()
        it[jenisReferensi] = "BARANG"
        it[dibuatPada] = now
        it[dibacaPada] = null
    }
    return NotifikasiStokResponse(id, type, title, message, barangId, "BARANG", now, null)
}
