package com.aura.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aura.app.core.model.AlasanPenyesuaian
import com.aura.app.core.model.JenisPengeluaran
import com.aura.app.core.model.JenisPergerakan
import com.aura.app.core.model.MetodePembayaran
import com.aura.app.core.model.StatusCatatanKeuangan
import com.aura.app.core.model.StatusTransaksi
import com.aura.app.core.model.SumberPergerakan

@Entity(
    tableName = "transaksi_penjualan",
    indices = [Index("idToko"), Index("dibuatPada")],
)
data class TransaksiPenjualanEntity(
    @PrimaryKey val id: String,
    val idToko: String,
    val nomorTransaksi: String,
    val akunId: String,
    val kasirNama: String,
    val metodePembayaran: MetodePembayaran,
    val total: Long,
    val uangDiterima: Long? = null,
    val kembalian: Long? = null,
    val status: StatusTransaksi,
    val alasanBatal: String? = null,
    val catatan: String? = null,
    val dibuatPada: Long,
)

@Entity(
    tableName = "item_transaksi",
    indices = [Index("transaksiId"), Index("barangId")],
)
data class ItemTransaksiEntity(
    @PrimaryKey val id: String,
    val transaksiId: String,
    val barangId: String,
    val namaBarangSnapshot: String,
    val satuanSnapshot: String,
    val jumlah: Int,
    val hargaJualSnapshot: Long,
    val hppSnapshot: Long,
)

@Entity(
    tableName = "pergerakan_stok",
    indices = [Index("idToko"), Index("barangId"), Index("dibuatPada")],
)
data class PergerakanStokEntity(
    @PrimaryKey val id: String,
    val idToko: String,
    val barangId: String,
    val namaBarang: String,
    val jenis: JenisPergerakan,
    val sumber: SumberPergerakan,
    val jumlah: Int,
    val stokSebelum: Int,
    val stokSesudah: Int,
    val alasan: AlasanPenyesuaian? = null,
    val akunId: String?,
    val namaAkun: String?,
    val referensiId: String? = null,
    val jenisReferensi: String? = null,
    val catatan: String? = null,
    val dibuatPada: Long,
)

@Entity(
    tableName = "pengeluaran",
    indices = [Index("idToko"), Index("tanggal")],
)
data class PengeluaranEntity(
    @PrimaryKey val id: String,
    val idToko: String,
    val judul: String,
    val jenis: JenisPengeluaran,
    val jenisBiaya: String? = null,
    val nominal: Long,
    val supplierId: String? = null,
    val buktiFileId: String? = null,
    val tanggal: Long,
    val status: StatusCatatanKeuangan,
    val alasanBatal: String? = null,
    val catatan: String? = null,
    val dibuatPada: Long,
    val barangId: String? = null,
    val jumlah: Int? = null,
    val hargaPerItem: Long? = null,
    val dibuatOleh: String? = null,
)

@Entity(
    tableName = "pemasukan_kustom",
    indices = [Index("idToko"), Index("tanggal")],
)
data class PemasukanKustomEntity(
    @PrimaryKey val id: String,
    val idToko: String,
    val judul: String,
    val nominal: Long,
    val buktiFileId: String? = null,
    val tanggal: Long,
    val status: StatusCatatanKeuangan,
    val alasanBatal: String? = null,
    val catatan: String? = null,
    val dibuatPada: Long,
)

@Entity(
    tableName = "pemasukan_penjualan_cache",
    indices = [Index("idToko"), Index("tanggal")],
)
data class PemasukanPenjualanCacheEntity(
    @PrimaryKey val id: String,
    val idToko: String,
    val nomorTransaksi: String,
    val namaAkun: String?,
    val metodePembayaran: MetodePembayaran,
    val nominal: Long,
    val tanggal: Long,
    val status: StatusTransaksi,
    val alasanBatal: String? = null,
    val catatan: String? = null,
)

@Entity(
    tableName = "notifikasi",
    indices = [Index("idToko"), Index("dibuatPada")],
)
data class NotifikasiEntity(
    @PrimaryKey val id: String,
    val idToko: String,
    val judul: String,
    val pesan: String,
    val jenis: String,
    val referensiId: String? = null,
    val dibaca: Boolean = false,
    val dibuatPada: Long,
)

@Entity(
    tableName = "log_aktivitas",
    indices = [Index("idToko"), Index("dibuatPada")],
)
data class LogAktivitasEntity(
    @PrimaryKey val id: String,
    val idToko: String,
    val akunId: String? = null,
    val namaAkunSnapshot: String,
    val kategori: String,
    val aksi: String,
    val detail: String? = null,
    val referensiId: String? = null,
    val jenisReferensi: String? = null,
    val dibuatPada: Long,
)
