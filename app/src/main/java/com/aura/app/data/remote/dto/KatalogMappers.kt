package com.aura.app.data.remote.dto

import com.aura.app.core.model.StatusAkun
import com.aura.app.core.model.StatusBarang
import com.aura.app.core.model.AlasanPenyesuaian
import com.aura.app.core.model.JenisPergerakan
import com.aura.app.core.model.SumberPergerakan
import com.aura.app.data.local.entity.BarangEntity
import com.aura.app.data.local.entity.BarangKategoriCrossRef
import com.aura.app.data.local.entity.HargaBeliSupplierEntity
import com.aura.app.data.local.entity.KategoriEntity
import com.aura.app.data.local.entity.PergerakanStokEntity
import com.aura.app.data.local.entity.SupplierEntity

fun BarangDto.toEntity(idToko: String): BarangEntity = BarangEntity(
    id = id,
    idToko = idToko,
    sku = sku,
    nama = nama,
    satuan = satuan,
    hargaJual = hargaJual,
    hppRataRata = hppRataRata,
    stokSaatIni = stokSaatIni,
    stokMinimum = stokMinimum,
    status = StatusBarang.valueOf(status),
    fotoFileId = fotoFileId,
    dibuatOleh = dibuatOleh,
    dibuatPada = dibuatPada,
)

fun BarangDto.toKategoriCrossRefs(): List<BarangKategoriCrossRef> =
    kategori.map { BarangKategoriCrossRef(barangId = id, kategoriId = it.id) }

fun KategoriDto.toEntity(idToko: String): KategoriEntity = KategoriEntity(
    id = id,
    idToko = idToko,
    nama = nama,
)

fun SupplierDto.toEntity(idToko: String): SupplierEntity = SupplierEntity(
    id = id,
    idToko = idToko,
    nama = nama,
    link = link,
    catatan = catatan,
    status = StatusAkun.valueOf(status),
)

fun PergerakanStokItemDto.toEntity(idToko: String): PergerakanStokEntity = PergerakanStokEntity(
    id = id,
    idToko = idToko,
    barangId = barangId,
    namaBarang = namaBarang,
    jenis = JenisPergerakan.valueOf(jenis),
    sumber = sumber.toSumberPergerakan(),
    jumlah = stokSesudah - stokSebelum,
    stokSebelum = stokSebelum,
    stokSesudah = stokSesudah,
    alasan = alasan?.let(AlasanPenyesuaian::valueOf),
    akunId = akunId,
    namaAkun = namaAkun,
    referensiId = referensiId,
    jenisReferensi = jenisReferensi,
    dibuatPada = dibuatPada,
)

fun PergerakanStokDto.toEntity(
    idToko: String,
    barangId: String,
    namaBarang: String,
): PergerakanStokEntity = PergerakanStokEntity(
    id = id,
    idToko = idToko,
    barangId = barangId,
    namaBarang = namaBarang,
    jenis = JenisPergerakan.valueOf(jenis),
    sumber = sumber.toSumberPergerakan(),
    jumlah = stokSesudah - stokSebelum,
    stokSebelum = stokSebelum,
    stokSesudah = stokSesudah,
    alasan = alasan?.let(AlasanPenyesuaian::valueOf),
    akunId = akunId,
    namaAkun = namaAkun,
    referensiId = referensiId,
    jenisReferensi = jenisReferensi,
    dibuatPada = dibuatPada,
)

fun HargaBeliSupplierDto.toEntity(idToko: String): HargaBeliSupplierEntity = HargaBeliSupplierEntity(
    id = "$barangId:$supplierId",
    idToko = idToko,
    barangId = barangId,
    supplierId = supplierId,
    supplierNama = namaSupplier,
    hargaPerItem = hargaPerItem,
    terakhirDiperbarui = diperbaruiPada,
)

internal fun String.toSumberPergerakan(): SumberPergerakan = when (this) {
    "STOCK_OPNAME" -> SumberPergerakan.PENYESUAIAN
    "PEMBATALAN_PENJUALAN" -> SumberPergerakan.PEMBATALAN
    else -> SumberPergerakan.valueOf(this)
}
