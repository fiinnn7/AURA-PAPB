package com.aura.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.aura.app.data.local.converter.Converters
import com.aura.app.data.local.dao.AkunDao
import com.aura.app.data.local.dao.BarangDao
import com.aura.app.data.local.dao.KeuanganDao
import com.aura.app.data.local.dao.LogAktivitasDao
import com.aura.app.data.local.dao.NotifikasiDao
import com.aura.app.data.local.dao.PergerakanStokDao
import com.aura.app.data.local.dao.SupplierDao
import com.aura.app.data.local.dao.TokoDao
import com.aura.app.data.local.dao.TransaksiDao
import com.aura.app.data.local.entity.AkunEntity
import com.aura.app.data.local.entity.BarangEntity
import com.aura.app.data.local.entity.BarangKategoriCrossRef
import com.aura.app.data.local.entity.HargaBeliSupplierEntity
import com.aura.app.data.local.entity.ItemTransaksiEntity
import com.aura.app.data.local.entity.KategoriEntity
import com.aura.app.data.local.entity.LogAktivitasEntity
import com.aura.app.data.local.entity.NotifikasiEntity
import com.aura.app.data.local.entity.PemasukanKustomEntity
import com.aura.app.data.local.entity.PemasukanPenjualanCacheEntity
import com.aura.app.data.local.entity.PengeluaranEntity
import com.aura.app.data.local.entity.PergerakanStokEntity
import com.aura.app.data.local.entity.SupplierEntity
import com.aura.app.data.local.entity.TokoEntity
import com.aura.app.data.local.entity.TransaksiPenjualanEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Skema cache Room baru AURA dengan istilah baku sesuai spec/14 (K-09, K-24, C-08).
 * Berdampingan dengan AuraDatabase lama sampai migrasi UI selesai di L09–L20.
 */
@Database(
    entities = [
        TokoEntity::class,
        AkunEntity::class,
        BarangEntity::class,
        KategoriEntity::class,
        BarangKategoriCrossRef::class,
        SupplierEntity::class,
        HargaBeliSupplierEntity::class,
        TransaksiPenjualanEntity::class,
        ItemTransaksiEntity::class,
        PergerakanStokEntity::class,
        PengeluaranEntity::class,
        PemasukanKustomEntity::class,
        PemasukanPenjualanCacheEntity::class,
        NotifikasiEntity::class,
        LogAktivitasEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tokoDao(): TokoDao
    abstract fun akunDao(): AkunDao
    abstract fun barangDao(): BarangDao
    abstract fun supplierDao(): SupplierDao
    abstract fun transaksiDao(): TransaksiDao
    abstract fun pergerakanStokDao(): PergerakanStokDao
    abstract fun keuanganDao(): KeuanganDao
    abstract fun notifikasiDao(): NotifikasiDao
    abstract fun logAktivitasDao(): LogAktivitasDao

    suspend fun clearAllCache() = withContext(Dispatchers.IO) {
        clearAllTables()
    }
}

object DatabaseProvider {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase =
        INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "aura.db",
            )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                .also { INSTANCE = it }
        }
}
