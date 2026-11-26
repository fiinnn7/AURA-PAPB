package com.aura.app.data.remote.dto

import com.aura.app.core.model.JenisPergerakan
import com.aura.app.core.model.MetodePembayaran
import com.aura.app.core.model.StatusTransaksi
import com.aura.app.data.local.entity.ItemTransaksiEntity
import com.aura.app.data.local.entity.NotifikasiEntity
import com.aura.app.data.local.entity.PergerakanStokEntity
import com.aura.app.data.local.entity.TransaksiPenjualanEntity

fun TransaksiResponseDto.toEntity(idToko: String): TransaksiPenjualanEntity =
    TransaksiPenjualanEntity(
        id = id,
        idToko = idToko,
        nomorTransaksi = nomorTransaksi,
        akunId = akunId ?: error("Data transaksi server tidak memiliki akun pelaku."),
        kasirNama = namaAkun,
        metodePembayaran = MetodePembayaran.valueOf(metodePembayaran),
        total = total,
        uangDiterima = uangDiterima,
        kembalian = kembalian,
        status = StatusTransaksi.valueOf(status),
        alasanBatal = alasanPembatalan,
        catatan = catatan,
        dibuatPada = dibuatPada,
    )

fun ItemTransaksiResponseDto.toEntity(transaksiId: String): ItemTransaksiEntity =
    ItemTransaksiEntity(
        id = id,
        transaksiId = transaksiId,
        barangId = barangId,
        namaBarangSnapshot = namaBarang,
        satuanSnapshot = satuan,
        jumlah = jumlah,
        hargaJualSnapshot = hargaJual,
        hppSnapshot = hpp,
    )

fun PergerakanTransaksiResponseDto.toEntity(
    idToko: String,
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
    akunId = akunId,
    namaAkun = namaAkun,
    referensiId = referensiId,
    jenisReferensi = jenisReferensi,
    dibuatPada = dibuatPada,
)

fun NotifikasiStokDto.toEntity(idToko: String): NotifikasiEntity = NotifikasiEntity(
    id = id,
    idToko = idToko,
    judul = judul,
    pesan = pesan,
    jenis = jenis,
    referensiId = referensiId,
    dibaca = dibacaPada != null,
    dibuatPada = dibuatPada,
)
