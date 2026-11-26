package com.aura.server.sales

import com.aura.server.stock.NotifikasiStokResponse
import java.util.UUID

internal data class TransaksiRequest(
    val id: UUID? = null,
    val metodePembayaran: String? = null,
    val uangDiterima: Long? = null,
    val pembayaranDiterima: Boolean? = null,
    val catatan: String? = null,
    val barangDibeli: List<BarangDibeliRequest>? = null,
)

internal data class BarangDibeliRequest(
    val barangId: UUID? = null,
    val jumlah: Int? = null,
)

internal data class PembatalanRequest(val alasan: String? = null)
internal data class PengajuanPembatalanRequest(val id: UUID? = null, val alasan: String? = null)

internal data class TransaksiResponse(
    val id: UUID,
    val nomorTransaksi: String,
    val akunId: UUID?,
    val namaAkun: String,
    val metodePembayaran: String,
    val total: Long,
    val uangDiterima: Long?,
    val kembalian: Long?,
    val status: String,
    val alasanPembatalan: String?,
    val catatan: String?,
    val dibuatPada: Long,
)

internal data class ItemTransaksiResponse(
    val id: UUID,
    val barangId: UUID,
    val namaBarang: String,
    val satuan: String,
    val jumlah: Int,
    val hargaJual: Long,
    val hpp: Long,
)

internal data class PergerakanTransaksiResponse(
    val id: UUID,
    val barangId: UUID,
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
)

internal data class TransaksiBuatResponse(
    val transaksi: TransaksiResponse,
    val rincianBarang: List<ItemTransaksiResponse>,
    val pergerakanStok: List<PergerakanTransaksiResponse>,
    val saldoKumulatif: Long,
    val notifikasi: NotifikasiStokResponse?,
)

internal data class TransaksiBatalResponse(
    val transaksi: TransaksiResponse,
    val pergerakanStok: List<PergerakanTransaksiResponse>,
    val saldoKumulatif: Long,
)

internal data class TransaksiDetailResponse(
    val transaksi: TransaksiResponse,
    val rincianBarang: List<ItemTransaksiResponse>,
)

internal data class TransaksiListResponse(
    val data: List<TransaksiResponse>,
    val nextCursor: String?,
)

internal data class PengajuanPembatalanResponse(
    val id: UUID,
    val jenis: String,
    val judul: String,
    val pesan: String,
    val referensiId: UUID?,
    val jenisReferensi: String?,
    val dibuatPada: Long,
    val dibacaPada: Long?,
)
