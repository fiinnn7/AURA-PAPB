package com.aura.server.stock

import com.aura.server.catalog.BarangResponse
import com.aura.server.catalog.PergerakanStokResponse
import java.util.UUID

internal data class RestockRequest(
    val id: UUID? = null,
    val barangId: UUID? = null,
    val supplierId: UUID? = null,
    val jumlahMasuk: Int? = null,
    val totalDibayar: Long? = null,
    val tanggal: Long? = null,
    val notaFileId: UUID? = null,
    val catatan: String? = null,
)

internal data class PenyesuaianStokRequest(
    val id: UUID? = null,
    val barangId: UUID? = null,
    val stokFisik: Int? = null,
    val alasan: String? = null,
    val catatan: String? = null,
    val buktiFileId: UUID? = null,
    val tanggal: Long? = null,
)

internal data class RestockInput(
    val id: UUID,
    val barangId: UUID,
    val supplierId: UUID,
    val jumlahMasuk: Int,
    val totalDibayar: Long,
    val tanggal: Long,
    val notaFileId: UUID?,
    val catatan: String?,
)

internal data class PenyesuaianStokInput(
    val id: UUID,
    val barangId: UUID,
    val stokFisik: Int,
    val alasan: String,
    val catatan: String?,
    val buktiFileId: UUID?,
    val tanggal: Long,
)

internal data class RestockRecordResponse(
    val id: UUID,
    val barangId: UUID,
    val supplierId: UUID,
    val jumlahMasuk: Int,
    val totalDibayar: Long,
    val hargaPerItem: Long,
    val tanggal: Long,
    val notaFileId: UUID?,
    val catatan: String?,
    val akunId: UUID?,
    val dibuatPada: Long,
)

internal data class PenyesuaianStokRecordResponse(
    val id: UUID,
    val barangId: UUID,
    val stokFisik: Int,
    val selisih: Int,
    val alasan: String,
    val nilaiKerugian: Long,
    val catatan: String?,
    val buktiFileId: UUID?,
    val tanggal: Long,
    val akunId: UUID?,
    val dibuatPada: Long,
)

internal data class PengeluaranPembelianResponse(
    val id: UUID,
    val jenis: String,
    val jenisBiaya: String?,
    val judul: String,
    val barangId: UUID?,
    val supplierId: UUID?,
    val jumlah: Int?,
    val hargaPerItem: Long?,
    val nominal: Long,
    val tanggal: Long,
    val catatan: String?,
    val buktiFileId: UUID?,
    val status: String,
    val referensiId: UUID?,
    val jenisReferensi: String?,
    val dibuatOleh: UUID?,
    val dibuatPada: Long,
)

internal data class NotifikasiStokResponse(
    val id: UUID,
    val jenis: String,
    val judul: String,
    val pesan: String,
    val referensiId: UUID?,
    val jenisReferensi: String?,
    val dibuatPada: Long,
    val dibacaPada: Long?,
)

internal data class RestockCreateResponse(
    val restock: RestockRecordResponse,
    val barang: BarangResponse,
    val pergerakan: PergerakanStokResponse,
    val pengeluaranPembelian: PengeluaranPembelianResponse,
    val saldoKumulatif: Long,
    val notifikasi: NotifikasiStokResponse?,
)

internal data class PenyesuaianStokCreateResponse(
    val penyesuaian: PenyesuaianStokRecordResponse,
    val barang: BarangResponse,
    val pergerakan: PergerakanStokResponse,
    val notifikasi: NotifikasiStokResponse?,
)

internal data class PergerakanStokItemResponse(
    val id: UUID,
    val barangId: UUID,
    val namaBarang: String,
    val jenis: String,
    val sumber: String,
    val jumlah: Int,
    val stokSebelum: Int,
    val stokSesudah: Int,
    val dibuatPada: Long,
    val akunId: UUID?,
    val namaAkun: String?,
    val referensiId: UUID?,
    val jenisReferensi: String?,
    val alasan: String?,
)

internal data class PergerakanStokListResponse(
    val data: List<PergerakanStokItemResponse>,
    val nextCursor: String?,
)

internal data class PergerakanStokCursor(
    val dibuatPada: Long,
    val id: UUID,
)

internal data class PergerakanStokPage(
    val data: List<PergerakanStokItemResponse>,
    val nextCursor: PergerakanStokCursor?,
)

internal interface StockRepository {
    fun createRestock(
        account: com.aura.server.auth.Akun,
        input: RestockInput,
        requestHash: String,
    ): RestockCreateResponse

    fun createAdjustment(
        account: com.aura.server.auth.Akun,
        input: PenyesuaianStokInput,
        requestHash: String,
    ): PenyesuaianStokCreateResponse

    fun listMovements(
        storeId: String,
        from: Long?,
        through: Long?,
        type: String?,
        barangId: UUID?,
        limit: Int,
        cursor: PergerakanStokCursor?,
    ): PergerakanStokPage
}
