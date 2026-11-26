package com.aura.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class MetodePembayaranTokoDto(
    @SerializedName("namaPenerima") val namaPenerima: String? = null,
    @SerializedName("infoRekening") val infoRekening: String? = null,
    @SerializedName("qrisFileId") val qrisFileId: String? = null,
)

data class TransaksiBuatRequestDto(
    @SerializedName("id") val id: String,
    @SerializedName("metodePembayaran") val metodePembayaran: String,
    @SerializedName("uangDiterima") val uangDiterima: Long? = null,
    @SerializedName("pembayaranDiterima") val pembayaranDiterima: Boolean? = null,
    @SerializedName("catatan") val catatan: String? = null,
    @SerializedName("barangDibeli") val barangDibeli: List<BarangDibeliRequestDto>,
)

data class BarangDibeliRequestDto(
    @SerializedName("barangId") val barangId: String,
    @SerializedName("jumlah") val jumlah: Int,
)

data class PembatalanTransaksiRequestDto(
    @SerializedName("alasan") val alasan: String,
)

data class PengajuanPembatalanRequestDto(
    @SerializedName("id") val id: String,
    @SerializedName("alasan") val alasan: String,
)

data class TransaksiResponseDto(
    @SerializedName("id") val id: String,
    @SerializedName("nomorTransaksi") val nomorTransaksi: String,
    @SerializedName("akunId") val akunId: String?,
    @SerializedName("namaAkun") val namaAkun: String,
    @SerializedName("metodePembayaran") val metodePembayaran: String,
    @SerializedName("total") val total: Long,
    @SerializedName("uangDiterima") val uangDiterima: Long?,
    @SerializedName("kembalian") val kembalian: Long?,
    @SerializedName("status") val status: String,
    @SerializedName("alasanPembatalan") val alasanPembatalan: String?,
    @SerializedName("catatan") val catatan: String?,
    @SerializedName("dibuatPada") val dibuatPada: Long,
)

data class ItemTransaksiResponseDto(
    @SerializedName("id") val id: String,
    @SerializedName("barangId") val barangId: String,
    @SerializedName("namaBarang") val namaBarang: String,
    @SerializedName("satuan") val satuan: String,
    @SerializedName("jumlah") val jumlah: Int,
    @SerializedName("hargaJual") val hargaJual: Long,
    @SerializedName("hpp") val hpp: Long,
)

data class PergerakanTransaksiResponseDto(
    @SerializedName("id") val id: String,
    @SerializedName("barangId") val barangId: String,
    @SerializedName("jenis") val jenis: String,
    @SerializedName("sumber") val sumber: String,
    @SerializedName("jumlah") val jumlah: Int,
    @SerializedName("stokSebelum") val stokSebelum: Int,
    @SerializedName("stokSesudah") val stokSesudah: Int,
    @SerializedName("dibuatPada") val dibuatPada: Long,
    @SerializedName("akunId") val akunId: String?,
    @SerializedName("namaAkun") val namaAkun: String?,
    @SerializedName("referensiId") val referensiId: String?,
    @SerializedName("jenisReferensi") val jenisReferensi: String?,
)

data class TransaksiBuatResponseDto(
    @SerializedName("transaksi") val transaksi: TransaksiResponseDto,
    @SerializedName("rincianBarang") val rincianBarang: List<ItemTransaksiResponseDto>,
    @SerializedName("pergerakanStok") val pergerakanStok: List<PergerakanTransaksiResponseDto>,
    @SerializedName("saldoKumulatif") val saldoKumulatif: Long,
    @SerializedName("notifikasi") val notifikasi: NotifikasiStokDto?,
)

data class TransaksiBatalResponseDto(
    @SerializedName("transaksi") val transaksi: TransaksiResponseDto,
    @SerializedName("pergerakanStok") val pergerakanStok: List<PergerakanTransaksiResponseDto>,
    @SerializedName("saldoKumulatif") val saldoKumulatif: Long,
)

data class TransaksiDetailResponseDto(
    @SerializedName("transaksi") val transaksi: TransaksiResponseDto,
    @SerializedName("rincianBarang") val rincianBarang: List<ItemTransaksiResponseDto>,
)

data class TransaksiListResponseDto(
    @SerializedName("data") val data: List<TransaksiResponseDto>,
    @SerializedName("nextCursor") val nextCursor: String?,
)

data class PengajuanPembatalanResponseDto(
    @SerializedName("id") val id: String,
    @SerializedName("jenis") val jenis: String,
    @SerializedName("judul") val judul: String,
    @SerializedName("pesan") val pesan: String,
    @SerializedName("referensiId") val referensiId: String?,
    @SerializedName("jenisReferensi") val jenisReferensi: String?,
    @SerializedName("dibuatPada") val dibuatPada: Long,
    @SerializedName("dibacaPada") val dibacaPada: Long?,
)
