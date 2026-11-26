package com.aura.app.data.repository

import androidx.room.withTransaction
import com.aura.app.core.model.Peran
import com.aura.app.data.local.AppDatabase
import com.aura.app.data.local.entity.TransaksiPenjualanEntity
import com.aura.app.data.network.KoneksiChecker
import com.aura.app.data.preferences.SesiAktif
import com.aura.app.data.preferences.SesiRepository
import com.aura.app.data.remote.datasource.PenjualanRemoteDataSource
import com.aura.app.data.remote.dto.MetodePembayaranTokoDto
import com.aura.app.data.remote.dto.TransaksiBuatRequestDto
import com.aura.app.data.remote.dto.TransaksiBuatResponseDto
import com.aura.app.data.remote.dto.TransaksiDetailResponseDto
import com.aura.app.data.remote.dto.TransaksiBatalResponseDto
import com.aura.app.data.remote.dto.PengajuanPembatalanResponseDto
import com.aura.app.data.remote.dto.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

interface PenjualanRepository {
    fun observeTransaksiHariIni(idToko: String): Flow<List<TransaksiPenjualanEntity>>
    suspend fun refreshRiwayatHariIni(idToko: String)
    suspend fun ambilMetodePembayaran(idToko: String): MetodePembayaranTokoDto
    suspend fun detailTransaksi(idToko: String, transaksiId: String): TransaksiDetailResponseDto
    suspend fun catatTransaksi(idToko: String, request: TransaksiBuatRequestDto): TransaksiBuatResponseDto
    suspend fun batalkanTransaksi(idToko: String, transaksiId: String, alasan: String): TransaksiBatalResponseDto
    suspend fun ajukanPembatalan(
        idToko: String,
        transaksiId: String,
        requestId: String,
        alasan: String,
    ): PengajuanPembatalanResponseDto
}

class DefaultPenjualanRepository(
    private val db: AppDatabase,
    private val remote: PenjualanRemoteDataSource,
    private val koneksi: KoneksiChecker,
    private val sesiRepository: SesiRepository,
) : PenjualanRepository {
    override fun observeTransaksiHariIni(idToko: String): Flow<List<TransaksiPenjualanEntity>> =
        db.transaksiDao().observeTransaksi(idToko)

    override suspend fun refreshRiwayatHariIni(idToko: String) {
        requireSession(idToko)
        koneksi.pastikanOnline()
        val data = mutableListOf<com.aura.app.data.remote.dto.TransaksiResponseDto>()
        val cursors = mutableSetOf<String>()
        var cursor: String? = null
        do {
            val page = remote.listTransaksi(cursor)
            data += page.data
            val nextCursor = page.nextCursor
            check(nextCursor == null || cursors.add(nextCursor)) {
                "Cursor riwayat transaksi dari server berulang."
            }
            cursor = nextCursor
        } while (cursor != null)

        val transaksi = data.map { it.toEntity(idToko) }
        db.withTransaction {
            db.transaksiDao().clearItemsByToko(idToko)
            db.transaksiDao().clearByToko(idToko)
            if (transaksi.isNotEmpty()) db.transaksiDao().upsertTransaksi(transaksi)
        }
    }

    override suspend fun ambilMetodePembayaran(idToko: String): MetodePembayaranTokoDto {
        requireSession(idToko)
        koneksi.pastikanOnline()
        val metode = remote.getMetodePembayaran()
        db.withTransaction {
            val toko = db.tokoDao().getToko(idToko)
                ?: error("Data toko belum tersedia di cache.")
            db.tokoDao().upsert(
                toko.copy(
                    namaPenerima = metode.namaPenerima,
                    infoRekening = metode.infoRekening,
                    qrisFileId = metode.qrisFileId,
                ),
            )
        }
        return metode
    }

    override suspend fun detailTransaksi(idToko: String, transaksiId: String): TransaksiDetailResponseDto {
        requireSession(idToko)
        koneksi.pastikanOnline()
        val response = remote.detailTransaksi(transaksiId)
        db.withTransaction {
            db.transaksiDao().upsertTransaksi(listOf(response.transaksi.toEntity(idToko)))
            db.transaksiDao().upsertItems(
                response.rincianBarang.map { it.toEntity(response.transaksi.id) },
            )
        }
        return response
    }

    override suspend fun catatTransaksi(
        idToko: String,
        request: TransaksiBuatRequestDto,
    ): TransaksiBuatResponseDto {
        requireSession(idToko)
        koneksi.pastikanOnline()
        val response = remote.createTransaksi(request)
        db.withTransaction {
            db.transaksiDao().upsertTransaksi(listOf(response.transaksi.toEntity(idToko)))
            db.transaksiDao().upsertItems(
                response.rincianBarang.map { it.toEntity(response.transaksi.id) },
            )
            simpanPergerakan(idToko, response.pergerakanStok)
            response.notifikasi?.let {
                db.notifikasiDao().upsertAll(listOf(it.toEntity(idToko)))
            }
            val toko = db.tokoDao().getToko(idToko)
                ?: error("Data toko belum tersedia di cache.")
            db.tokoDao().upsert(toko.copy(saldoKumulatif = response.saldoKumulatif))
        }
        return response
    }

    override suspend fun batalkanTransaksi(
        idToko: String,
        transaksiId: String,
        alasan: String,
    ): TransaksiBatalResponseDto {
        requireRole(idToko, Peran.OWNER)
        koneksi.pastikanOnline()
        val response = remote.cancelTransaksi(transaksiId, alasan)
        db.withTransaction {
            db.transaksiDao().upsertTransaksi(listOf(response.transaksi.toEntity(idToko)))
            simpanPergerakan(idToko, response.pergerakanStok)
            val toko = db.tokoDao().getToko(idToko)
                ?: error("Data toko belum tersedia di cache.")
            db.tokoDao().upsert(toko.copy(saldoKumulatif = response.saldoKumulatif))
        }
        return response
    }

    override suspend fun ajukanPembatalan(
        idToko: String,
        transaksiId: String,
        requestId: String,
        alasan: String,
    ): PengajuanPembatalanResponseDto {
        requireRole(idToko, Peran.KASIR)
        koneksi.pastikanOnline()
        val response = remote.requestCancellation(transaksiId, requestId, alasan)
        db.notifikasiDao().upsertAll(
            listOf(
                com.aura.app.data.local.entity.NotifikasiEntity(
                    id = response.id,
                    idToko = idToko,
                    judul = response.judul,
                    pesan = response.pesan,
                    jenis = response.jenis,
                    referensiId = response.referensiId,
                    dibaca = response.dibacaPada != null,
                    dibuatPada = response.dibuatPada,
                ),
            ),
        )
        return response
    }

    private suspend fun simpanPergerakan(
        idToko: String,
        pergerakan: List<com.aura.app.data.remote.dto.PergerakanTransaksiResponseDto>,
    ) {
        val barangDao = db.barangDao()
        val entities = pergerakan.map { item ->
            val barang = barangDao.getById(item.barangId)
                ?: error("Barang ${item.barangId} belum tersedia di cache.")
            barangDao.upsert(barang.copy(stokSaatIni = item.stokSesudah))
            item.toEntity(idToko, barang.nama)
        }
        if (entities.isNotEmpty()) db.pergerakanStokDao().upsertAll(entities)
    }

    private suspend fun requireSession(idToko: String): SesiAktif {
        val sesi = sesiRepository.sesiAktif.first()
            ?: throw IllegalStateException("Sesi tidak tersedia.")
        if (sesi.idToko != idToko) {
            throw IllegalStateException("Toko aktif tidak sesuai dengan data.")
        }
        return sesi
    }

    private suspend fun requireRole(idToko: String, role: Peran) {
        if (requireSession(idToko).peran != role) {
            val message = if (role == Peran.OWNER) {
                "Hanya Owner yang dapat membatalkan transaksi."
            } else {
                "Hanya Kasir yang dapat mengajukan pembatalan."
            }
            throw IllegalStateException(message)
        }
    }
}
