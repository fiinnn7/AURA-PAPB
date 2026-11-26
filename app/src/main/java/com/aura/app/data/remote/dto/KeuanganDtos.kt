package com.aura.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PengeluaranBuatRequestDto(
    @SerializedName("id") val id: String,
    @SerializedName("judul") val judul: String,
    @SerializedName("jenisBiaya") val jenisBiaya: String,
    @SerializedName("nominal") val nominal: Long,
    @SerializedName("tanggal") val tanggal: Long? = null,
    @SerializedName("catatan") val catatan: String? = null,
    @SerializedName("buktiFileId") val buktiFileId: String? = null,
)

data class PemasukanBuatRequestDto(
    @SerializedName("id") val id: String,
    @SerializedName("judul") val judul: String,
    @SerializedName("nominal") val nominal: Long,
    @SerializedName("tanggal") val tanggal: Long? = null,
    @SerializedName("catatan") val catatan: String? = null,
    @SerializedName("buktiFileId") val buktiFileId: String? = null,
)

data class PembatalanKeuanganRequestDto(
    @SerializedName("alasan") val alasan: String,
)

data class PengeluaranResponseDto(
    @SerializedName("id") val id: String,
    @SerializedName("jenis") val jenis: String,
    @SerializedName("jenisBiaya") val jenisBiaya: String? = null,
    @SerializedName("judul") val judul: String,
    @SerializedName("barangId") val barangId: String? = null,
    @SerializedName("supplierId") val supplierId: String? = null,
    @SerializedName("jumlah") val jumlah: Int? = null,
    @SerializedName("hargaPerItem") val hargaPerItem: Long? = null,
    @SerializedName("nominal") val nominal: Long,
    @SerializedName("tanggal") val tanggal: Long,
    @SerializedName("catatan") val catatan: String? = null,
    @SerializedName("buktiFileId") val buktiFileId: String? = null,
    @SerializedName("status") val status: String,
    @SerializedName("alasanPembatalan") val alasanPembatalan: String? = null,
    @SerializedName("referensiId") val referensiId: String? = null,
    @SerializedName("jenisReferensi") val jenisReferensi: String? = null,
    @SerializedName("dibuatOleh") val dibuatOleh: String? = null,
    @SerializedName("dibuatPada") val dibuatPada: Long,
    @SerializedName("dibatalkanOleh") val dibatalkanOleh: String? = null,
    @SerializedName("dibatalkanPada") val dibatalkanPada: Long? = null,
)

data class PemasukanResponseDto(
    @SerializedName("id") val id: String,
    @SerializedName("jenis") val jenis: String,
    @SerializedName("judul") val judul: String,
    @SerializedName("nominal") val nominal: Long,
    @SerializedName("tanggal") val tanggal: Long,
    @SerializedName("status") val status: String,
    @SerializedName("alasanPembatalan") val alasanPembatalan: String? = null,
    @SerializedName("catatan") val catatan: String? = null,
    @SerializedName("buktiFileId") val buktiFileId: String? = null,
    @SerializedName("dibuatOleh") val dibuatOleh: String? = null,
    @SerializedName("namaAkun") val namaAkun: String? = null,
    @SerializedName("dibuatPada") val dibuatPada: Long,
    @SerializedName("nomorTransaksi") val nomorTransaksi: String? = null,
    @SerializedName("metodePembayaran") val metodePembayaran: String? = null,
)

data class PengeluaranListResponseDto(
    @SerializedName("data") val data: List<PengeluaranResponseDto>,
    @SerializedName("total") val total: Long,
    @SerializedName("nextCursor") val nextCursor: String?,
)

data class PemasukanListResponseDto(
    @SerializedName("data") val data: List<PemasukanResponseDto>,
    @SerializedName("total") val total: Long,
    @SerializedName("nextCursor") val nextCursor: String?,
)

data class PengeluaranEnvelopeDto(
    @SerializedName("pengeluaran") val pengeluaran: PengeluaranResponseDto,
)

data class PemasukanEnvelopeDto(
    @SerializedName("pemasukan") val pemasukan: PemasukanResponseDto,
)

data class PengeluaranWriteResponseDto(
    @SerializedName("pengeluaran") val pengeluaran: PengeluaranResponseDto,
    @SerializedName("saldoKumulatif") val saldoKumulatif: Long,
)

data class PemasukanWriteResponseDto(
    @SerializedName("pemasukan") val pemasukan: PemasukanResponseDto,
    @SerializedName("saldoKumulatif") val saldoKumulatif: Long,
)

data class MetodePembayaranEnvelopeDto(
    @SerializedName("metodePembayaran") val metodePembayaran: MetodePembayaranTokoDto,
)
