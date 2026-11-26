package com.aura.server.db

import org.jetbrains.exposed.v1.core.Table

internal object PemasukanKustomTable : Table("pemasukan_kustom") {
    val id = uuid("id")
    val idToko = varchar("id_toko", 8)
    val judul = varchar("judul", 120)
    val nominal = long("nominal")
    val tanggal = long("tanggal")
    val catatan = varchar("catatan", 1000).nullable()
    val buktiFileId = uuid("bukti_file_id").nullable()
    val status = varchar("status", 16)
    val alasanPembatalan = varchar("alasan_pembatalan", 500).nullable()
    val dibuatOleh = uuid("dibuat_oleh").nullable()
    val dibuatPada = long("dibuat_pada")
    val dibatalkanOleh = uuid("dibatalkan_oleh").nullable()
    val dibatalkanPada = long("dibatalkan_pada").nullable()
    val requestHash = char("request_hash", 64)
    val responseSnapshot = text("response_snapshot")

    override val primaryKey = PrimaryKey(id)
}
