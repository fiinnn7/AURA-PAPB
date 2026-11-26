package com.aura.server.db

import org.jetbrains.exposed.v1.core.Table

internal object TokoTable : Table("toko") {
    val idToko = varchar("id_toko", 8)
    val nama = varchar("nama_toko", 120)
    val jenisUsaha = varchar("jenis_usaha", 80).nullable()
    val alamat = varchar("alamat", 500).nullable()
    val logoFileId = uuid("logo_file_id").nullable()
    val namaPenerima = varchar("nama_penerima", 120).nullable()
    val infoRekening = varchar("info_rekening", 500).nullable()
    val qrisFileId = uuid("qris_file_id").nullable()
    val saldoAwal = long("saldo_awal")
    val saldoKumulatif = long("saldo_kumulatif")
    val hapusPada = long("hapus_pada").nullable()
    val dibuatPada = long("dibuat_pada")

    override val primaryKey = PrimaryKey(idToko)
}

internal object AkunTable : Table("akun") {
    val id = uuid("id")
    val idToko = varchar("id_toko", 8)
    val email = varchar("email", 254).nullable()
    val nama = varchar("nama_akun", 80)
    val namaNormalisasi = varchar("nama_akun_normalisasi", 80)
    val peran = varchar("peran", 10)
    val sandiHash = varchar("sandi_hash", 128)
    val sandiSalt = varchar("sandi_salt", 64)
    val status = varchar("status", 12)
    val wajibGantiSandi = bool("wajib_ganti_sandi")
    val nomorHp = varchar("nomor_hp", 30).nullable()
    val fotoFileId = uuid("foto_file_id").nullable()
    val gagalLogin = integer("gagal_login")
    val terkunciSampai = long("terkunci_sampai").nullable()
    val terakhirLogin = long("terakhir_login").nullable()
    val dibuatPada = long("dibuat_pada")
    val dihapusPada = long("dihapus_pada").nullable()

    override val primaryKey = PrimaryKey(id)
}

internal object SesiTable : Table("sesi") {
    val jti = uuid("jti")
    val akunId = uuid("akun_id")
    val dibuatPada = long("dibuat_pada")
    val berakhirPada = long("berakhir_pada")
    val dicabutPada = long("dicabut_pada").nullable()

    override val primaryKey = PrimaryKey(jti)
}

internal object PercobaanLoginTable : Table("percobaan_login") {
    val kunci = varchar("kunci", 64)
    val jumlahGagal = integer("jumlah_gagal")
    val terkunciSampai = long("terkunci_sampai").nullable()
    val diperbaruiPada = long("diperbarui_pada")

    override val primaryKey = PrimaryKey(kunci)
}

internal object LogAktivitasTable : Table("log_aktivitas") {
    val id = uuid("id")
    val idToko = varchar("id_toko", 8)
    val akunId = uuid("akun_id").nullable()
    val namaAkunSnapshot = varchar("nama_akun_snapshot", 80)
    val kategori = varchar("kategori", 10)
    val aksi = varchar("aksi", 40)
    val referensiId = uuid("referensi_id").nullable()
    val jenisReferensi = varchar("jenis_referensi", 30).nullable()
    val dibuatPada = long("dibuat_pada")

    override val primaryKey = PrimaryKey(id)
}
