package com.aura.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class BarangCreateRequestDto(
    @SerializedName("id") val id: String,
    @SerializedName("nama") val nama: String,
    @SerializedName("satuan") val satuan: String,
    @SerializedName("sku") val sku: String? = null,
    @SerializedName("hargaJual") val hargaJual: Long,
    @SerializedName("stokMinimum") val stokMinimum: Int,
    @SerializedName("stokAwal") val stokAwal: Int,
    @SerializedName("fotoFileId") val fotoFileId: String? = null,
    @SerializedName("kategoriIds") val kategoriIds: List<String> = emptyList(),
    @SerializedName("hargaBeliAwal") val hargaBeliAwal: Long? = null,
    @SerializedName("supplierId") val supplierId: String? = null,
)

data class BarangUpdateRequestDto(
    @SerializedName("nama") val nama: String? = null,
    @SerializedName("satuan") val satuan: String? = null,
    @SerializedName("sku") val sku: String? = null,
    @SerializedName("hargaJual") val hargaJual: Long? = null,
    @SerializedName("stokMinimum") val stokMinimum: Int? = null,
    @SerializedName("fotoFileId") val fotoFileId: String? = null,
    @SerializedName("kategoriIds") val kategoriIds: List<String>? = null,
)

data class BarangListResponseDto(
    @SerializedName("data") val data: List<BarangDto>,
    @SerializedName("nextCursor") val nextCursor: String?,
)

data class BarangDetailResponseDto(
    @SerializedName("barang") val barang: BarangDto,
    @SerializedName("hargaBeliSupplier") val hargaBeliSupplier: List<HargaBeliSupplierDto>,
    @SerializedName("kartuStokRingkas") val kartuStokRingkas: PergerakanStokRingkasDto,
)

data class BarangWriteResponseDto(
    @SerializedName("barang") val barang: BarangDto,
    @SerializedName("pergerakan") val pergerakan: PergerakanStokDto?,
    @SerializedName("saldoKumulatif") val saldoKumulatif: Long,
)

data class BarangEnvelopeDto(
    @SerializedName("barang") val barang: BarangDto,
)

data class BarangDeleteResponseDto(
    @SerializedName("id") val id: String,
    @SerializedName("status") val status: String,
)

data class BarangDto(
    @SerializedName("id") val id: String,
    @SerializedName("nama") val nama: String,
    @SerializedName("satuan") val satuan: String,
    @SerializedName("sku") val sku: String?,
    @SerializedName("hargaJual") val hargaJual: Long,
    @SerializedName("stokMinimum") val stokMinimum: Int,
    @SerializedName("stokSaatIni") val stokSaatIni: Int,
    @SerializedName("hppRataRata") val hppRataRata: Long,
    @SerializedName("status") val status: String,
    @SerializedName("fotoFileId") val fotoFileId: String?,
    @SerializedName("kategori") val kategori: List<KategoriDto>,
    @SerializedName("dibuatOleh") val dibuatOleh: String,
    @SerializedName("dibuatPada") val dibuatPada: Long,
    @SerializedName("diubahPada") val diubahPada: Long,
)

data class KategoriRequestDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("nama") val nama: String,
)

data class KategoriDto(
    @SerializedName("id") val id: String,
    @SerializedName("nama") val nama: String,
    @SerializedName("jumlahBarang") val jumlahBarang: Int = 0,
)

data class KategoriEnvelopeDto(
    @SerializedName("kategori") val kategori: KategoriDto,
)

data class KategoriListResponseDto(
    @SerializedName("data") val data: List<KategoriDto>,
)

data class SupplierRequestDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("nama") val nama: String,
    @SerializedName("link") val link: String? = null,
    @SerializedName("catatan") val catatan: String? = null,
)

data class SupplierStatusRequestDto(
    @SerializedName("status") val status: String,
)

data class SupplierDto(
    @SerializedName("id") val id: String,
    @SerializedName("nama") val nama: String,
    @SerializedName("link") val link: String?,
    @SerializedName("catatan") val catatan: String?,
    @SerializedName("status") val status: String,
    @SerializedName("jumlahBarang") val jumlahBarang: Int,
)

data class SupplierEnvelopeDto(
    @SerializedName("supplier") val supplier: SupplierDto,
)

data class SupplierListResponseDto(
    @SerializedName("data") val data: List<SupplierDto>,
    @SerializedName("nextCursor") val nextCursor: String?,
)

data class SupplierDetailResponseDto(
    @SerializedName("supplier") val supplier: SupplierDto,
    @SerializedName("barang") val barang: List<BarangDto>,
    @SerializedName("hargaBeliTerakhir") val hargaBeliTerakhir: List<HargaBeliSupplierDto>,
    @SerializedName("riwayatPembelian") val riwayatPembelian: List<HargaBeliSupplierDto>,
)

data class HargaBeliSupplierDto(
    @SerializedName("barangId") val barangId: String,
    @SerializedName("supplierId") val supplierId: String,
    @SerializedName("namaSupplier") val namaSupplier: String,
    @SerializedName("hargaPerItem") val hargaPerItem: Long,
    @SerializedName("diperbaruiPada") val diperbaruiPada: Long,
)

data class PergerakanStokRingkasDto(
    @SerializedName("jumlahPergerakan") val jumlahPergerakan: Int,
    @SerializedName("stokMasuk") val stokMasuk: Int,
    @SerializedName("stokKeluar") val stokKeluar: Int,
)

data class PergerakanStokDto(
    @SerializedName("id") val id: String,
    @SerializedName("jenis") val jenis: String,
    @SerializedName("sumber") val sumber: String,
    @SerializedName("jumlah") val jumlah: Int,
    @SerializedName("stokSebelum") val stokSebelum: Int,
    @SerializedName("stokSesudah") val stokSesudah: Int,
    @SerializedName("dibuatPada") val dibuatPada: Long,
    @SerializedName("akunId") val akunId: String? = null,
    @SerializedName("namaAkun") val namaAkun: String? = null,
    @SerializedName("referensiId") val referensiId: String? = null,
    @SerializedName("jenisReferensi") val jenisReferensi: String? = null,
    @SerializedName("alasan") val alasan: String? = null,
)

data class RestockRequestDto(
    @SerializedName("id") val id: String,
    @SerializedName("barangId") val barangId: String,
    @SerializedName("supplierId") val supplierId: String,
    @SerializedName("jumlahMasuk") val jumlahMasuk: Int,
    @SerializedName("totalDibayar") val totalDibayar: Long,
    @SerializedName("tanggal") val tanggal: Long? = null,
    @SerializedName("notaFileId") val notaFileId: String? = null,
    @SerializedName("catatan") val catatan: String? = null,
)

data class PenyesuaianStokRequestDto(
    @SerializedName("id") val id: String,
    @SerializedName("barangId") val barangId: String,
    @SerializedName("stokFisik") val stokFisik: Int,
    @SerializedName("alasan") val alasan: String,
    @SerializedName("catatan") val catatan: String? = null,
    @SerializedName("buktiFileId") val buktiFileId: String? = null,
    @SerializedName("tanggal") val tanggal: Long? = null,
)

data class RestockCreateResponseDto(
    @SerializedName("restock") val restock: RestockRecordDto,
    @SerializedName("barang") val barang: BarangDto,
    @SerializedName("pergerakan") val pergerakan: PergerakanStokDto,
    @SerializedName("pengeluaranPembelian") val pengeluaranPembelian: PengeluaranPembelianDto,
    @SerializedName("saldoKumulatif") val saldoKumulatif: Long,
    @SerializedName("notifikasi") val notifikasi: NotifikasiStokDto?,
)

data class RestockRecordDto(
    @SerializedName("id") val id: String,
    @SerializedName("barangId") val barangId: String,
    @SerializedName("supplierId") val supplierId: String,
    @SerializedName("jumlahMasuk") val jumlahMasuk: Int,
    @SerializedName("totalDibayar") val totalDibayar: Long,
    @SerializedName("hargaPerItem") val hargaPerItem: Long,
    @SerializedName("tanggal") val tanggal: Long,
    @SerializedName("notaFileId") val notaFileId: String?,
    @SerializedName("catatan") val catatan: String?,
    @SerializedName("akunId") val akunId: String?,
    @SerializedName("dibuatPada") val dibuatPada: Long,
)

data class PengeluaranPembelianDto(
    @SerializedName("id") val id: String,
    @SerializedName("jenis") val jenis: String,
    @SerializedName("jenisBiaya") val jenisBiaya: String?,
    @SerializedName("judul") val judul: String,
    @SerializedName("barangId") val barangId: String?,
    @SerializedName("supplierId") val supplierId: String?,
    @SerializedName("jumlah") val jumlah: Int?,
    @SerializedName("hargaPerItem") val hargaPerItem: Long?,
    @SerializedName("nominal") val nominal: Long,
    @SerializedName("tanggal") val tanggal: Long,
    @SerializedName("catatan") val catatan: String?,
    @SerializedName("buktiFileId") val buktiFileId: String?,
    @SerializedName("status") val status: String,
    @SerializedName("referensiId") val referensiId: String?,
    @SerializedName("jenisReferensi") val jenisReferensi: String?,
    @SerializedName("dibuatOleh") val dibuatOleh: String?,
    @SerializedName("dibuatPada") val dibuatPada: Long,
)

data class PenyesuaianStokCreateResponseDto(
    @SerializedName("penyesuaian") val penyesuaian: PenyesuaianStokRecordDto,
    @SerializedName("barang") val barang: BarangDto,
    @SerializedName("pergerakan") val pergerakan: PergerakanStokDto,
    @SerializedName("notifikasi") val notifikasi: NotifikasiStokDto?,
)

data class PenyesuaianStokRecordDto(
    @SerializedName("id") val id: String,
    @SerializedName("barangId") val barangId: String,
    @SerializedName("stokFisik") val stokFisik: Int,
    @SerializedName("selisih") val selisih: Int,
    @SerializedName("alasan") val alasan: String,
    @SerializedName("nilaiKerugian") val nilaiKerugian: Long,
    @SerializedName("catatan") val catatan: String?,
    @SerializedName("buktiFileId") val buktiFileId: String?,
    @SerializedName("tanggal") val tanggal: Long,
    @SerializedName("akunId") val akunId: String?,
    @SerializedName("dibuatPada") val dibuatPada: Long,
)

data class NotifikasiStokDto(
    @SerializedName("id") val id: String,
    @SerializedName("jenis") val jenis: String,
    @SerializedName("judul") val judul: String,
    @SerializedName("pesan") val pesan: String,
    @SerializedName("referensiId") val referensiId: String?,
    @SerializedName("jenisReferensi") val jenisReferensi: String?,
    @SerializedName("dibuatPada") val dibuatPada: Long,
    @SerializedName("dibacaPada") val dibacaPada: Long?,
)

data class PergerakanStokItemDto(
    @SerializedName("id") val id: String,
    @SerializedName("barangId") val barangId: String,
    @SerializedName("namaBarang") val namaBarang: String,
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
    @SerializedName("alasan") val alasan: String?,
)

data class PergerakanStokListResponseDto(
    @SerializedName("data") val data: List<PergerakanStokItemDto>,
    @SerializedName("nextCursor") val nextCursor: String?,
)

data class FileUploadResponseDto(
    @SerializedName("fileId") val fileId: String,
    @SerializedName("mimeType") val mimeType: String,
    @SerializedName("ukuran") val ukuran: Long,
)
