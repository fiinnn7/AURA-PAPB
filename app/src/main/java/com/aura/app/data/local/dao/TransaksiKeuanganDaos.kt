package com.aura.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.aura.app.data.local.entity.ItemTransaksiEntity
import com.aura.app.data.local.entity.LogAktivitasEntity
import com.aura.app.data.local.entity.NotifikasiEntity
import com.aura.app.data.local.entity.PemasukanKustomEntity
import com.aura.app.data.local.entity.PemasukanPenjualanCacheEntity
import com.aura.app.data.local.entity.PengeluaranEntity
import com.aura.app.data.local.entity.PergerakanStokEntity
import com.aura.app.data.local.entity.TransaksiPenjualanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransaksiDao {
    @Query("SELECT * FROM transaksi_penjualan WHERE idToko = :idToko ORDER BY dibuatPada DESC")
    fun observeTransaksi(idToko: String): Flow<List<TransaksiPenjualanEntity>>

    @Query("SELECT * FROM item_transaksi WHERE transaksiId = :transaksiId")
    fun observeItems(transaksiId: String): Flow<List<ItemTransaksiEntity>>

    @Upsert
    suspend fun upsertTransaksi(daftar: List<TransaksiPenjualanEntity>)

    @Upsert
    suspend fun upsertItems(daftar: List<ItemTransaksiEntity>)

    @Query("DELETE FROM item_transaksi WHERE transaksiId IN (SELECT id FROM transaksi_penjualan WHERE idToko = :idToko)")
    suspend fun clearItemsByToko(idToko: String)

    @Query("DELETE FROM transaksi_penjualan WHERE idToko = :idToko")
    suspend fun clearByToko(idToko: String)

    @Query("DELETE FROM transaksi_penjualan")
    suspend fun clearAll()
}

@Dao
interface PergerakanStokDao {
    @Query("SELECT * FROM pergerakan_stok WHERE idToko = :idToko ORDER BY dibuatPada DESC")
    fun observePergerakan(idToko: String): Flow<List<PergerakanStokEntity>>

    @Query("SELECT * FROM pergerakan_stok WHERE barangId = :barangId ORDER BY dibuatPada DESC")
    fun observeKartuStok(barangId: String): Flow<List<PergerakanStokEntity>>

    @Upsert
    suspend fun upsertAll(daftar: List<PergerakanStokEntity>)

    @Query("DELETE FROM pergerakan_stok WHERE idToko = :idToko")
    suspend fun clearByToko(idToko: String)

    @Query("DELETE FROM pergerakan_stok WHERE idToko = :idToko AND barangId = :barangId")
    suspend fun clearByBarang(idToko: String, barangId: String)

    @Query("DELETE FROM pergerakan_stok")
    suspend fun clearAll()
}

@Dao
interface KeuanganDao {
    @Query("SELECT * FROM pengeluaran WHERE idToko = :idToko ORDER BY tanggal DESC, dibuatPada DESC")
    fun observePengeluaran(idToko: String): Flow<List<PengeluaranEntity>>

    @Query("SELECT * FROM pemasukan_kustom WHERE idToko = :idToko ORDER BY tanggal DESC, dibuatPada DESC")
    fun observePemasukanKustom(idToko: String): Flow<List<PemasukanKustomEntity>>

    @Query("SELECT * FROM pemasukan_penjualan_cache WHERE idToko = :idToko ORDER BY tanggal DESC")
    fun observePemasukanPenjualan(idToko: String): Flow<List<PemasukanPenjualanCacheEntity>>

    @Upsert
    suspend fun upsertPengeluaran(daftar: List<PengeluaranEntity>)

    @Upsert
    suspend fun upsertPemasukanKustom(daftar: List<PemasukanKustomEntity>)

    @Upsert
    suspend fun upsertPemasukanPenjualan(daftar: List<PemasukanPenjualanCacheEntity>)

    @Query("DELETE FROM pengeluaran WHERE idToko = :idToko")
    suspend fun clearPengeluaranByToko(idToko: String)

    @Query("DELETE FROM pemasukan_kustom WHERE idToko = :idToko")
    suspend fun clearPemasukanKustomByToko(idToko: String)

    @Query("DELETE FROM pemasukan_penjualan_cache WHERE idToko = :idToko")
    suspend fun clearPemasukanPenjualanByToko(idToko: String)
}

@Dao
interface NotifikasiDao {
    @Query("SELECT * FROM notifikasi WHERE idToko = :idToko ORDER BY dibuatPada DESC")
    fun observeNotifikasi(idToko: String): Flow<List<NotifikasiEntity>>

    @Query("SELECT COUNT(*) FROM notifikasi WHERE idToko = :idToko AND dibaca = 0")
    fun observeJumlahBelumDibaca(idToko: String): Flow<Int>

    @Upsert
    suspend fun upsertAll(daftar: List<NotifikasiEntity>)

    @Query("UPDATE notifikasi SET dibaca = 1 WHERE id = :id")
    suspend fun tandaiDibaca(id: String)

    @Query("UPDATE notifikasi SET dibaca = 1 WHERE idToko = :idToko")
    suspend fun tandaiSemuaDibaca(idToko: String)

    @Query("DELETE FROM notifikasi")
    suspend fun clearAll()
}

@Dao
interface LogAktivitasDao {
    @Query("SELECT * FROM log_aktivitas WHERE idToko = :idToko ORDER BY dibuatPada DESC")
    fun observeLogs(idToko: String): Flow<List<LogAktivitasEntity>>

    @Upsert
    suspend fun upsertAll(daftar: List<LogAktivitasEntity>)

    @Query("DELETE FROM log_aktivitas")
    suspend fun clearAll()
}
