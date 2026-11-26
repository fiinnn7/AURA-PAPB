package com.aura.server.db

import org.jetbrains.exposed.v1.core.Table

internal object TransaksiPenjualanTable : Table("transaksi_penjualan") {
    val id = uuid("id")
    val idToko = varchar("id_toko", 8)
    val nomorTransaksi = varchar("nomor_transaksi", 32)
    val akunId = uuid("akun_id").nullable()
    val namaAkunSnapshot = varchar("nama_akun_snapshot", 80)
    val metodePembayaran = varchar("metode_pembayaran", 16)
    val total = long("total")
    val uangDiterima = long("uang_diterima").nullable()
    val kembalian = long("kembalian").nullable()
    val status = varchar("status", 16)
    val alasanPembatalan = varchar("alasan_pembatalan", 500).nullable()
    val catatan = varchar("catatan", 1000).nullable()
    val dibuatPada = long("dibuat_pada")
    val requestHash = char("request_hash", 64)
    val responseSnapshot = text("response_snapshot")

    override val primaryKey = PrimaryKey(id)
}

internal object ItemTransaksiTable : Table("item_transaksi") {
    val id = uuid("id")
    val idToko = varchar("id_toko", 8)
    val transaksiId = uuid("transaksi_id")
    val barangId = uuid("barang_id")
    val namaBarangSnapshot = varchar("nama_barang_snapshot", 120)
    val satuanSnapshot = varchar("satuan_snapshot", 30)
    val jumlah = integer("jumlah")
    val hargaJualSnapshot = long("harga_jual_snapshot")
    val hppSnapshot = long("hpp_snapshot")

    override val primaryKey = PrimaryKey(id)
}

internal object PengajuanPembatalanTable : Table("pengajuan_pembatalan") {
    val id = uuid("id")
    val idToko = varchar("id_toko", 8)
    val transaksiId = uuid("transaksi_id")
    val akunId = uuid("akun_id").nullable()
    val alasan = varchar("alasan", 500)
    val dibuatPada = long("dibuat_pada")
    val requestHash = char("request_hash", 64)
    val responseSnapshot = text("response_snapshot")

    override val primaryKey = PrimaryKey(id)
}
