package com.aura.server.db

import org.jetbrains.exposed.v1.core.Table

internal object PengeluaranTable : Table("pengeluaran") {
    val id = uuid("id")
    val idToko = varchar("id_toko", 8)
    val jenis = varchar("jenis", 20)
    val jenisBiaya = varchar("jenis_biaya", 40).nullable()
    val judul = varchar("judul", 120)
    val barangId = uuid("barang_id").nullable()
    val supplierId = uuid("supplier_id").nullable()
    val jumlah = integer("jumlah").nullable()
    val hargaPerItem = long("harga_per_item").nullable()
    val nominal = long("nominal")
    val tanggal = long("tanggal")
    val catatan = varchar("catatan", 1000).nullable()
    val buktiFileId = uuid("bukti_file_id").nullable()
    val status = varchar("status", 16)
    val alasanPembatalan = varchar("alasan_pembatalan", 500).nullable()
    val referensiId = uuid("referensi_id").nullable()
    val jenisReferensi = varchar("jenis_referensi", 30).nullable()
    val dibuatOleh = uuid("dibuat_oleh").nullable()
    val dibuatPada = long("dibuat_pada")
    val dibatalkanOleh = uuid("dibatalkan_oleh").nullable()
    val dibatalkanPada = long("dibatalkan_pada").nullable()
    val requestHash = char("request_hash", 64).nullable()
    val responseSnapshot = text("response_snapshot").nullable()

    override val primaryKey = PrimaryKey(id)
}

internal object RestockTable : Table("restock") {
    val id = uuid("id")
    val idToko = varchar("id_toko", 8)
    val barangId = uuid("barang_id")
    val supplierId = uuid("supplier_id")
    val jumlahMasuk = integer("jumlah_masuk")
    val totalDibayar = long("total_dibayar")
    val hargaPerItem = long("harga_per_item")
    val tanggal = long("tanggal")
    val notaFileId = uuid("nota_file_id").nullable()
    val catatan = varchar("catatan", 1000).nullable()
    val akunId = uuid("akun_id").nullable()
    val dibuatPada = long("dibuat_pada")
    val requestHash = char("request_hash", 64)
    val responseSnapshot = text("response_snapshot")

    override val primaryKey = PrimaryKey(id)
}

internal object PenyesuaianStokTable : Table("penyesuaian_stok") {
    val id = uuid("id")
    val idToko = varchar("id_toko", 8)
    val barangId = uuid("barang_id")
    val stokFisik = integer("stok_fisik")
    val selisih = integer("selisih")
    val alasan = varchar("alasan", 20)
    val nilaiKerugian = long("nilai_kerugian")
    val catatan = varchar("catatan", 1000).nullable()
    val buktiFileId = uuid("bukti_file_id").nullable()
    val tanggal = long("tanggal")
    val akunId = uuid("akun_id").nullable()
    val dibuatPada = long("dibuat_pada")
    val requestHash = char("request_hash", 64)
    val responseSnapshot = text("response_snapshot")

    override val primaryKey = PrimaryKey(id)
}

internal object NotifikasiTable : Table("notifikasi") {
    val id = uuid("id")
    val idToko = varchar("id_toko", 8)
    val jenis = varchar("jenis", 30)
    val judul = varchar("judul", 160)
    val pesan = varchar("pesan", 500)
    val referensiId = uuid("referensi_id").nullable()
    val jenisReferensi = varchar("jenis_referensi", 30).nullable()
    val dibuatPada = long("dibuat_pada")
    val dibacaPada = long("dibaca_pada").nullable()

    override val primaryKey = PrimaryKey(id)
}
