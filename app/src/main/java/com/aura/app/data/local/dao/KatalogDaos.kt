package com.aura.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.aura.app.data.local.entity.BarangEntity
import com.aura.app.data.local.entity.BarangKategoriCrossRef
import com.aura.app.data.local.entity.HargaBeliSupplierEntity
import com.aura.app.data.local.entity.KategoriEntity
import com.aura.app.data.local.entity.SupplierEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BarangDao {
    @Query("SELECT * FROM barang WHERE idToko = :idToko AND status = 'AKTIF' ORDER BY nama ASC")
    fun observeBarangAktif(idToko: String): Flow<List<BarangEntity>>

    @Query("SELECT * FROM barang WHERE idToko = :idToko ORDER BY nama ASC")
    fun observeSemuaBarang(idToko: String): Flow<List<BarangEntity>>

    @Query("SELECT * FROM barang WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<BarangEntity?>

    @Query("SELECT * FROM barang WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): BarangEntity?

    @Upsert
    suspend fun upsert(barang: BarangEntity)

    @Query("DELETE FROM barang WHERE id = :id")
    suspend fun clearBarang(id: String)

    @Upsert
    suspend fun upsertAll(daftar: List<BarangEntity>)

    @Query("DELETE FROM barang WHERE idToko = :idToko")
    suspend fun clearByToko(idToko: String)

    @Transaction
    suspend fun replaceByToko(idToko: String, daftar: List<BarangEntity>) {
        clearByToko(idToko)
        if (daftar.isNotEmpty()) upsertAll(daftar)
    }

    @Query("SELECT * FROM kategori WHERE idToko = :idToko ORDER BY nama ASC")
    fun observeKategori(idToko: String): Flow<List<KategoriEntity>>

    @Upsert
    suspend fun upsertKategori(daftar: List<KategoriEntity>)

    @Query("DELETE FROM kategori WHERE idToko = :idToko")
    suspend fun clearKategoriByToko(idToko: String)

    @Upsert
    suspend fun upsertRelasiKategori(relasi: List<BarangKategoriCrossRef>)

    @Query("SELECT * FROM barang_kategori")
    fun observeRelasiKategori(): Flow<List<BarangKategoriCrossRef>>

    @Query("DELETE FROM barang_kategori WHERE barangId = :barangId")
    suspend fun clearRelasiBarang(barangId: String)

    @Query("DELETE FROM barang_kategori WHERE kategoriId = :kategoriId")
    suspend fun clearRelasiKategori(kategoriId: String)

    @Query("DELETE FROM kategori WHERE id = :kategoriId")
    suspend fun deleteKategori(kategoriId: String)

    @Query("DELETE FROM barang_kategori WHERE barangId IN (SELECT id FROM barang WHERE idToko = :idToko)")
    suspend fun clearRelasiKategoriByToko(idToko: String)

    @Query("SELECT * FROM harga_beli_supplier WHERE barangId = :barangId ORDER BY terakhirDiperbarui DESC")
    fun observeHargaBeliSupplier(barangId: String): Flow<List<HargaBeliSupplierEntity>>

    @Upsert
    suspend fun upsertHargaBeliSupplier(daftar: List<HargaBeliSupplierEntity>)

    @Query("DELETE FROM barang")
    suspend fun clearAll()
}

@Dao
interface SupplierDao {
    @Query("SELECT * FROM supplier WHERE idToko = :idToko ORDER BY nama ASC")
    fun observeSuppliers(idToko: String): Flow<List<SupplierEntity>>

    @Query("SELECT * FROM supplier WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): SupplierEntity?

    @Upsert
    suspend fun upsert(supplier: SupplierEntity)

    @Query("DELETE FROM supplier WHERE id = :id")
    suspend fun delete(id: String)

    @Upsert
    suspend fun upsertAll(daftar: List<SupplierEntity>)

    @Query("DELETE FROM supplier WHERE idToko = :idToko")
    suspend fun clearByToko(idToko: String)

    @Transaction
    suspend fun replaceByToko(idToko: String, daftar: List<SupplierEntity>) {
        clearByToko(idToko)
        if (daftar.isNotEmpty()) upsertAll(daftar)
    }

    @Query("DELETE FROM supplier")
    suspend fun clearAll()
}
