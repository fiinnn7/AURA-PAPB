package com.aura.server.catalog

import java.util.UUID

internal data class BarangCreateRequest(
    val id: UUID,
    val nama: String,
    val satuan: String,
    val sku: String? = null,
    val hargaJual: Long,
    val stokMinimum: Int,
    val stokAwal: Int,
    val fotoFileId: UUID? = null,
    val kategoriIds: List<UUID>? = null,
    val hargaBeliAwal: Long? = null,
    val supplierId: UUID? = null,
)

internal data class BarangUpdateRequest(
    val nama: String? = null,
    val satuan: String? = null,
    val sku: String? = null,
    val hargaJual: Long? = null,
    val stokMinimum: Int? = null,
    val fotoFileId: UUID? = null,
    val kategoriIds: List<UUID>? = null,
)

internal data class KategoriCreateRequest(
    val id: UUID,
    val nama: String,
)

internal data class KategoriUpdateRequest(
    val nama: String,
)

internal data class SupplierCreateRequest(
    val id: UUID,
    val nama: String,
    val link: String? = null,
    val catatan: String? = null,
)

internal data class SupplierUpdateRequest(
    val nama: String? = null,
    val link: String? = null,
    val catatan: String? = null,
)

internal data class SupplierStatusRequest(
    val status: String,
)

internal data class Barang(
    val id: UUID,
    val idToko: String,
    val nama: String,
    val satuan: String,
    val sku: String?,
    val hargaJual: Long,
    val stokMinimum: Int,
    val stokSaatIni: Int,
    val hppRataRata: Long,
    val status: String,
    val fotoFileId: UUID?,
    val kategori: List<Kategori> = emptyList(),
    val dibuatOleh: UUID,
    val dibuatPada: Long,
    val diubahPada: Long,
)

internal data class Kategori(
    val id: UUID,
    val idToko: String,
    val nama: String,
    val jumlahBarang: Int = 0,
    val dibuatPada: Long,
)

internal data class Supplier(
    val id: UUID,
    val idToko: String,
    val nama: String,
    val link: String?,
    val catatan: String?,
    val status: String,
    val jumlahBarang: Int = 0,
    val dibuatPada: Long,
)

internal data class HargaBeliSupplier(
    val barangId: UUID,
    val supplierId: UUID,
    val namaSupplier: String,
    val hargaPerItem: Long,
    val diperbaruiPada: Long,
)

internal data class PergerakanStokRingkas(
    val jumlahPergerakan: Int,
    val stokMasuk: Int,
    val stokKeluar: Int,
)

internal data class FileMetadata(
    val id: UUID,
    val idToko: String,
    val mimeType: String,
    val ukuran: Long,
    val dibuatPada: Long,
)

internal data class BarangResponse(
    val id: UUID,
    val nama: String,
    val satuan: String,
    val sku: String?,
    val hargaJual: Long,
    val stokMinimum: Int,
    val stokSaatIni: Int,
    val hppRataRata: Long,
    val status: String,
    val fotoFileId: UUID?,
    val kategori: List<KategoriResponse>,
    val dibuatOleh: UUID,
    val dibuatPada: Long,
    val diubahPada: Long,
)

internal data class BarangListResponse(
    val data: List<BarangResponse>,
    val nextCursor: String?,
)

internal data class BarangDetailResponse(
    val barang: BarangResponse,
    val hargaBeliSupplier: List<HargaBeliSupplierResponse>,
    val kartuStokRingkas: PergerakanStokRingkas,
)

internal data class BarangWriteResponse(
    val barang: BarangResponse,
    val pergerakan: PergerakanStokResponse?,
    val saldoKumulatif: Long,
)

internal data class BarangDeleteResponse(
    val id: UUID,
    val status: String,
)

internal data class PergerakanStokResponse(
    val id: UUID,
    val jenis: String,
    val sumber: String,
    val jumlah: Int,
    val stokSebelum: Int,
    val stokSesudah: Int,
    val dibuatPada: Long,
    val akunId: UUID? = null,
    val namaAkun: String? = null,
    val referensiId: UUID? = null,
    val jenisReferensi: String? = null,
    val alasan: String? = null,
)

internal data class KategoriResponse(
    val id: UUID,
    val nama: String,
    val jumlahBarang: Int,
)

internal data class KategoriListResponse(
    val data: List<KategoriResponse>,
)

internal data class SupplierResponse(
    val id: UUID,
    val nama: String,
    val link: String?,
    val catatan: String?,
    val status: String,
    val jumlahBarang: Int,
)

internal data class SupplierListResponse(
    val data: List<SupplierResponse>,
    val nextCursor: String?,
)

internal data class SupplierDetailResponse(
    val supplier: SupplierResponse,
    val barang: List<BarangResponse>,
    val hargaBeliTerakhir: List<HargaBeliSupplierResponse>,
    val riwayatPembelian: List<HargaBeliSupplierResponse>,
)

internal data class HargaBeliSupplierResponse(
    val barangId: UUID,
    val supplierId: UUID,
    val namaSupplier: String,
    val hargaPerItem: Long,
    val diperbaruiPada: Long,
)

internal data class FileUploadResponse(
    val fileId: UUID,
    val mimeType: String,
    val ukuran: Long,
)
