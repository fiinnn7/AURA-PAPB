package com.aura.app.data.remote.dto

import com.aura.app.core.model.JenisPengeluaran
import com.aura.app.core.model.MetodePembayaran
import com.aura.app.core.model.StatusCatatanKeuangan
import com.aura.app.core.model.StatusTransaksi
import com.aura.app.data.local.entity.PemasukanKustomEntity
import com.aura.app.data.local.entity.PemasukanPenjualanCacheEntity
import com.aura.app.data.local.entity.PengeluaranEntity

fun PengeluaranResponseDto.toEntity(idToko: String): PengeluaranEntity = PengeluaranEntity(
    id = id,
    idToko = idToko,
    judul = judul,
    jenis = JenisPengeluaran.valueOf(jenis),
    jenisBiaya = jenisBiaya,
    nominal = nominal,
    supplierId = supplierId,
    buktiFileId = buktiFileId,
    tanggal = tanggal,
    status = StatusCatatanKeuangan.valueOf(status),
    alasanBatal = alasanPembatalan,
    catatan = catatan,
    dibuatPada = dibuatPada,
    barangId = barangId,
    jumlah = jumlah,
    hargaPerItem = hargaPerItem,
    dibuatOleh = dibuatOleh,
)

fun PemasukanResponseDto.toCustomEntity(idToko: String): PemasukanKustomEntity {
    require(jenis == "KUSTOM") { "Data Pemasukan bukan jenis Kustom." }
    return PemasukanKustomEntity(
        id = id,
        idToko = idToko,
        judul = judul,
        nominal = nominal,
        buktiFileId = buktiFileId,
        tanggal = tanggal,
        status = StatusCatatanKeuangan.valueOf(status),
        alasanBatal = alasanPembatalan,
        catatan = catatan,
        dibuatPada = dibuatPada,
    )
}

fun PemasukanResponseDto.toPenjualanCacheEntity(idToko: String): PemasukanPenjualanCacheEntity {
    require(jenis == "PENJUALAN") { "Data Pemasukan bukan jenis Penjualan." }
    return PemasukanPenjualanCacheEntity(
        id = id,
        idToko = idToko,
        nomorTransaksi = nomorTransaksi ?: error("Nomor transaksi tidak tersedia."),
        namaAkun = namaAkun,
        metodePembayaran = MetodePembayaran.valueOf(metodePembayaran ?: error("Metode pembayaran tidak tersedia.")),
        nominal = nominal,
        tanggal = tanggal,
        status = StatusTransaksi.valueOf(status),
        alasanBatal = alasanPembatalan,
        catatan = catatan,
    )
}
