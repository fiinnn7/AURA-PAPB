package com.aura.server.finance

import java.util.UUID

internal data class PengeluaranRequest(
    val id: UUID? = null,
    val judul: String? = null,
    val jenisBiaya: String? = null,
    val nominal: Long? = null,
    val tanggal: Long? = null,
    val catatan: String? = null,
    val buktiFileId: UUID? = null,
)

internal data class PemasukanRequest(
    val id: UUID? = null,
    val judul: String? = null,
    val nominal: Long? = null,
    val tanggal: Long? = null,
    val catatan: String? = null,
    val buktiFileId: UUID? = null,
)

internal data class PembatalanKeuanganRequest(val alasan: String? = null)

internal data class MetodePembayaranRequest(
    val namaPenerima: String? = null,
    val infoRekening: String? = null,
    val qrisFileId: UUID? = null,
)

internal data class MetodePembayaranResponse(
    val namaPenerima: String?,
    val infoRekening: String?,
    val qrisFileId: UUID?,
)

internal data class PengeluaranInput(
    val id: UUID,
    val judul: String,
    val jenisBiaya: String,
    val nominal: Long,
    val tanggal: Long,
    val catatan: String?,
    val buktiFileId: UUID?,
)

internal data class PemasukanInput(
    val id: UUID,
    val judul: String,
    val nominal: Long,
    val tanggal: Long,
    val catatan: String?,
    val buktiFileId: UUID?,
)

internal data class FinanceExpenseCursor(val tanggal: Long, val id: UUID)

internal data class FinanceIncomeCursor(val tanggal: Long, val jenis: String, val id: UUID)

internal data class PengeluaranResponse(
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
    val alasanPembatalan: String?,
    val referensiId: UUID?,
    val jenisReferensi: String?,
    val dibuatOleh: UUID?,
    val dibuatPada: Long,
    val dibatalkanOleh: UUID?,
    val dibatalkanPada: Long?,
)

internal data class PemasukanResponse(
    val id: UUID,
    val jenis: String,
    val judul: String,
    val nominal: Long,
    val tanggal: Long,
    val status: String,
    val alasanPembatalan: String?,
    val catatan: String?,
    val buktiFileId: UUID?,
    val dibuatOleh: UUID?,
    val namaAkun: String?,
    val dibuatPada: Long,
    val nomorTransaksi: String? = null,
    val metodePembayaran: String? = null,
)

internal data class PengeluaranCreateResponse(val pengeluaran: PengeluaranResponse, val saldoKumulatif: Long)

internal data class PengeluaranCancelResponse(val pengeluaran: PengeluaranResponse, val saldoKumulatif: Long)

internal data class PemasukanCreateResponse(val pemasukan: PemasukanResponse, val saldoKumulatif: Long)

internal data class PemasukanCancelResponse(val pemasukan: PemasukanResponse, val saldoKumulatif: Long)

internal data class PengeluaranDetailResponse(val pengeluaran: PengeluaranResponse)

internal data class PemasukanDetailResponse(val pemasukan: PemasukanResponse)

internal data class PengeluaranListResponse(
    val data: List<PengeluaranResponse>,
    val total: Long,
    val nextCursor: String?,
)

internal data class PemasukanListResponse(
    val data: List<PemasukanResponse>,
    val total: Long,
    val nextCursor: String?,
)

internal data class FinanceExpensePage(
    val data: List<PengeluaranResponse>,
    val total: Long,
    val nextCursor: FinanceExpenseCursor?,
)

internal data class FinanceIncomePage(
    val data: List<PemasukanResponse>,
    val total: Long,
    val nextCursor: FinanceIncomeCursor?,
)
