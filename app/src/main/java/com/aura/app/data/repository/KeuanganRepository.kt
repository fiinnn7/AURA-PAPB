package com.aura.app.data.repository

import android.content.ContentResolver
import android.net.Uri
import androidx.room.withTransaction
import com.aura.app.core.model.JenisPengeluaran
import com.aura.app.core.model.Peran
import com.aura.app.core.model.StatusCatatanKeuangan
import com.aura.app.core.model.StatusTransaksi
import com.aura.app.data.local.AppDatabase
import com.aura.app.data.local.entity.PemasukanKustomEntity
import com.aura.app.data.local.entity.PemasukanPenjualanCacheEntity
import com.aura.app.data.local.entity.PengeluaranEntity
import com.aura.app.data.local.entity.TokoEntity
import com.aura.app.data.network.KoneksiChecker
import com.aura.app.data.preferences.SesiAktif
import com.aura.app.data.preferences.SesiRepository
import com.aura.app.data.remote.datasource.AuthRemoteDataSource
import com.aura.app.data.remote.datasource.KeuanganRemoteDataSource
import com.aura.app.data.remote.dto.MetodePembayaranTokoDto
import com.aura.app.data.remote.dto.PemasukanBuatRequestDto
import com.aura.app.data.remote.dto.PemasukanResponseDto
import com.aura.app.data.remote.dto.PengeluaranBuatRequestDto
import com.aura.app.data.remote.dto.PengeluaranResponseDto
import com.aura.app.data.remote.dto.UpdateOpeningBalanceRequestDto
import com.aura.app.data.remote.dto.UpdateStoreRequestDto
import com.aura.app.data.remote.dto.TransaksiDetailResponseDto
import com.aura.app.data.remote.dto.toCustomEntity
import com.aura.app.data.remote.dto.toEntity
import com.aura.app.data.remote.dto.toPenjualanCacheEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID

interface KeuanganRepository {
    fun observeToko(idToko: String): Flow<TokoEntity?>
    fun observePengeluaran(idToko: String): Flow<List<PengeluaranEntity>>
    fun observePemasukanKustom(idToko: String): Flow<List<PemasukanKustomEntity>>
    fun observePemasukanPenjualan(idToko: String): Flow<List<PemasukanPenjualanCacheEntity>>
    suspend fun refresh(idToko: String)
    suspend fun catatPengeluaran(idToko: String, judul: String, jenisBiaya: String, nominal: Long, catatan: String?, buktiFileId: String?)
    suspend fun catatPemasukan(idToko: String, judul: String, nominal: Long, catatan: String?, buktiFileId: String?)
    suspend fun detailPengeluaran(idToko: String, pengeluaranId: String): PengeluaranEntity
    suspend fun detailPemasukan(idToko: String, pemasukanId: String): PemasukanResponseDto
    suspend fun detailTransaksi(idToko: String, transaksiId: String): TransaksiDetailResponseDto
    suspend fun batalkanPengeluaran(idToko: String, pengeluaranId: String, alasan: String)
    suspend fun batalkanPemasukanKustom(idToko: String, pemasukanId: String, alasan: String)
    suspend fun batalkanPenjualan(idToko: String, transaksiId: String, alasan: String)
    suspend fun simpanPengaturanToko(idToko: String, nama: String, jenisUsaha: String?, alamat: String?)
    suspend fun koreksiSaldoAwal(idToko: String, saldoAwal: Long)
    suspend fun simpanMetodePembayaran(idToko: String, namaPenerima: String?, infoRekening: String?, qrisFileId: String?)
    suspend fun unggahFile(resolver: ContentResolver, uri: Uri): String
    suspend fun ambilFile(fileId: String): ByteArray
}

class DefaultKeuanganRepository(
    private val db: AppDatabase,
    private val remote: KeuanganRemoteDataSource,
    private val authRemote: AuthRemoteDataSource,
    private val barangRepository: BarangRepository,
    private val penjualanRepository: PenjualanRepository,
    private val koneksi: KoneksiChecker,
    private val sesiRepository: SesiRepository,
) : KeuanganRepository {
    override fun observeToko(idToko: String): Flow<TokoEntity?> = db.tokoDao().observeToko(idToko)

    override fun observePengeluaran(idToko: String): Flow<List<PengeluaranEntity>> =
        db.keuanganDao().observePengeluaran(idToko)

    override fun observePemasukanKustom(idToko: String): Flow<List<PemasukanKustomEntity>> =
        db.keuanganDao().observePemasukanKustom(idToko)

    override fun observePemasukanPenjualan(idToko: String): Flow<List<PemasukanPenjualanCacheEntity>> =
        db.keuanganDao().observePemasukanPenjualan(idToko)

    override suspend fun refresh(idToko: String) {
        requireOwner(idToko)
        koneksi.pastikanOnline()
        val expenses = remote.listPengeluaran()
        val incomes = remote.listPemasukan()
        val store = authRemote.getToko()
        check(store.idToko == idToko) { "Data toko dari server tidak sesuai dengan sesi aktif." }
        val payment = remote.getMetodePembayaran()
        val existingStore = db.tokoDao().getToko(idToko)
        val storeEntity = store.toEntity(existingStore).withPaymentMethod(payment)
        val expenseEntities = expenses.map { it.toEntity(idToko) }
        val customIncomeEntities = incomes.filter { it.jenis == "KUSTOM" }.map { it.toCustomEntity(idToko) }
        val salesIncomeEntities = incomes.filter { it.jenis == "PENJUALAN" }.map { it.toPenjualanCacheEntity(idToko) }

        db.withTransaction {
            db.keuanganDao().clearPengeluaranByToko(idToko)
            db.keuanganDao().clearPemasukanKustomByToko(idToko)
            db.keuanganDao().clearPemasukanPenjualanByToko(idToko)
            if (expenseEntities.isNotEmpty()) db.keuanganDao().upsertPengeluaran(expenseEntities)
            if (customIncomeEntities.isNotEmpty()) db.keuanganDao().upsertPemasukanKustom(customIncomeEntities)
            if (salesIncomeEntities.isNotEmpty()) db.keuanganDao().upsertPemasukanPenjualan(salesIncomeEntities)
            db.tokoDao().upsert(storeEntity)
        }
    }

    override suspend fun catatPengeluaran(
        idToko: String,
        judul: String,
        jenisBiaya: String,
        nominal: Long,
        catatan: String?,
        buktiFileId: String?,
    ) {
        requireOwner(idToko)
        koneksi.pastikanOnline()
        val response = remote.createPengeluaran(
            PengeluaranBuatRequestDto(
                id = UUID.randomUUID().toString(),
                judul = judul.trim(),
                jenisBiaya = jenisBiaya,
                nominal = nominal,
                catatan = catatan?.trim()?.takeIf(String::isNotEmpty),
                buktiFileId = buktiFileId,
            ),
        )
        db.withTransaction {
            db.keuanganDao().upsertPengeluaran(listOf(response.pengeluaran.toEntity(idToko)))
            updateBalance(idToko, response.saldoKumulatif)
        }
    }

    override suspend fun catatPemasukan(
        idToko: String,
        judul: String,
        nominal: Long,
        catatan: String?,
        buktiFileId: String?,
    ) {
        requireOwner(idToko)
        koneksi.pastikanOnline()
        val response = remote.createPemasukan(
            PemasukanBuatRequestDto(
                id = UUID.randomUUID().toString(),
                judul = judul.trim(),
                nominal = nominal,
                catatan = catatan?.trim()?.takeIf(String::isNotEmpty),
                buktiFileId = buktiFileId,
            ),
        )
        db.withTransaction {
            db.keuanganDao().upsertPemasukanKustom(listOf(response.pemasukan.toCustomEntity(idToko)))
            updateBalance(idToko, response.saldoKumulatif)
        }
    }

    override suspend fun detailPengeluaran(idToko: String, pengeluaranId: String): PengeluaranEntity {
        requireOwner(idToko)
        koneksi.pastikanOnline()
        val expense = remote.detailPengeluaran(pengeluaranId).toEntity(idToko)
        db.keuanganDao().upsertPengeluaran(listOf(expense))
        return expense
    }

    override suspend fun detailPemasukan(idToko: String, pemasukanId: String): PemasukanResponseDto {
        requireOwner(idToko)
        koneksi.pastikanOnline()
        val income = remote.detailPemasukan(pemasukanId)
        if (income.jenis == "KUSTOM") {
            db.keuanganDao().upsertPemasukanKustom(listOf(income.toCustomEntity(idToko)))
        }
        return income
    }

    override suspend fun detailTransaksi(idToko: String, transaksiId: String): TransaksiDetailResponseDto =
        penjualanRepository.detailTransaksi(idToko, transaksiId)

    override suspend fun batalkanPengeluaran(idToko: String, pengeluaranId: String, alasan: String) {
        requireOwner(idToko)
        koneksi.pastikanOnline()
        val response = remote.cancelPengeluaran(pengeluaranId, alasan.trim())
        db.withTransaction {
            db.keuanganDao().upsertPengeluaran(listOf(response.pengeluaran.toEntity(idToko)))
            updateBalance(idToko, response.saldoKumulatif)
        }
    }

    override suspend fun batalkanPemasukanKustom(idToko: String, pemasukanId: String, alasan: String) {
        requireOwner(idToko)
        koneksi.pastikanOnline()
        val response = remote.cancelPemasukan(pemasukanId, alasan.trim())
        db.withTransaction {
            db.keuanganDao().upsertPemasukanKustom(listOf(response.pemasukan.toCustomEntity(idToko)))
            updateBalance(idToko, response.saldoKumulatif)
        }
    }

    override suspend fun batalkanPenjualan(idToko: String, transaksiId: String, alasan: String) {
        requireOwner(idToko)
        val response = penjualanRepository.batalkanTransaksi(idToko, transaksiId, alasan.trim())
        val current = db.keuanganDao().observePemasukanPenjualan(idToko).first()
            .firstOrNull { it.id == transaksiId }
        if (current != null) {
            db.keuanganDao().upsertPemasukanPenjualan(
                listOf(
                    current.copy(
                        status = StatusTransaksi.DIBATALKAN,
                        alasanBatal = response.transaksi.alasanPembatalan,
                    ),
                ),
            )
        }
    }

    override suspend fun simpanPengaturanToko(idToko: String, nama: String, jenisUsaha: String?, alamat: String?) {
        requireOwner(idToko)
        koneksi.pastikanOnline()
        val response = authRemote.updateToko(
            UpdateStoreRequestDto(
                namaToko = nama.trim(),
                jenisUsaha = jenisUsaha?.trim(),
                alamat = alamat?.trim(),
            ),
        )
        val existing = db.tokoDao().getToko(idToko)
        db.tokoDao().upsert(response.toko.toEntity(existing))
    }

    override suspend fun koreksiSaldoAwal(idToko: String, saldoAwal: Long) {
        requireOwner(idToko)
        koneksi.pastikanOnline()
        val response = authRemote.updateSaldoAwal(UpdateOpeningBalanceRequestDto(saldoAwal))
        val store = db.tokoDao().getToko(idToko)
            ?: error("Data toko belum tersedia di cache.")
        db.tokoDao().upsert(
            store.copy(
                saldoAwal = response.saldoAwal,
                saldoKumulatif = response.saldoKumulatif,
            ),
        )
    }

    override suspend fun simpanMetodePembayaran(
        idToko: String,
        namaPenerima: String?,
        infoRekening: String?,
        qrisFileId: String?,
    ) {
        requireOwner(idToko)
        koneksi.pastikanOnline()
        val response = remote.updateMetodePembayaran(
            MetodePembayaranTokoDto(
                namaPenerima = namaPenerima?.trim()?.takeIf(String::isNotEmpty),
                infoRekening = infoRekening?.trim()?.takeIf(String::isNotEmpty),
                qrisFileId = qrisFileId,
            ),
        ).metodePembayaran
        val store = db.tokoDao().getToko(idToko)
            ?: error("Data toko belum tersedia di cache.")
        db.tokoDao().upsert(store.withPaymentMethod(response))
    }

    override suspend fun unggahFile(resolver: ContentResolver, uri: Uri): String =
        barangRepository.unggahFoto(resolver, uri)

    override suspend fun ambilFile(fileId: String): ByteArray = barangRepository.ambilFile(fileId)

    private suspend fun updateBalance(idToko: String, balance: Long) {
        val store = db.tokoDao().getToko(idToko)
            ?: error("Data toko belum tersedia di cache.")
        db.tokoDao().upsert(store.copy(saldoKumulatif = balance))
    }

    private suspend fun requireOwner(idToko: String): SesiAktif {
        val session = sesiRepository.sesiAktif.first()
            ?: throw IllegalStateException("Sesi tidak tersedia.")
        if (session.idToko != idToko) {
            throw IllegalStateException("Toko aktif tidak sesuai dengan data.")
        }
        if (session.peran != Peran.OWNER) {
            throw IllegalStateException("Hanya Owner yang dapat mengubah data ini.")
        }
        return session
    }

    private fun TokoEntity.withPaymentMethod(payment: MetodePembayaranTokoDto): TokoEntity =
        copy(
            namaPenerima = payment.namaPenerima,
            infoRekening = payment.infoRekening,
            qrisFileId = payment.qrisFileId,
        )
}
