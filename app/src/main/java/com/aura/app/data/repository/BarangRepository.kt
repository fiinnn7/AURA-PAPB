package com.aura.app.data.repository

import android.content.ContentResolver
import android.net.Uri
import androidx.room.withTransaction
import com.aura.app.data.local.AppDatabase
import com.aura.app.data.local.entity.BarangEntity
import com.aura.app.data.local.entity.BarangKategoriCrossRef
import com.aura.app.data.local.entity.HargaBeliSupplierEntity
import com.aura.app.data.local.entity.KategoriEntity
import com.aura.app.data.local.entity.SupplierEntity
import com.aura.app.data.local.entity.PergerakanStokEntity
import com.aura.app.data.network.KoneksiChecker
import com.aura.app.data.preferences.SesiAktif
import com.aura.app.data.preferences.SesiRepository
import com.aura.app.data.remote.datasource.KatalogRemoteDataSource
import com.aura.app.data.remote.dto.BarangCreateRequestDto
import com.aura.app.data.remote.dto.BarangDetailResponseDto
import com.aura.app.data.remote.dto.BarangDto
import com.aura.app.data.remote.dto.BarangUpdateRequestDto
import com.aura.app.data.remote.dto.KategoriDto
import com.aura.app.data.remote.dto.PenyesuaianStokCreateResponseDto
import com.aura.app.data.remote.dto.RestockCreateResponseDto
import com.aura.app.data.remote.dto.SupplierDto
import com.aura.app.data.remote.dto.toEntity
import com.aura.app.data.remote.dto.toKategoriCrossRefs
import com.aura.app.data.remote.dto.RestockRequestDto
import com.aura.app.data.remote.dto.PenyesuaianStokRequestDto
import com.aura.app.data.remote.dto.toEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.UUID

interface BarangRepository {
    fun observeBarang(idToko: String): Flow<List<BarangEntity>>
    fun observeKategori(idToko: String): Flow<List<KategoriEntity>>
    fun observeRelasiKategori(): Flow<List<BarangKategoriCrossRef>>
    fun observeSupplier(idToko: String): Flow<List<SupplierEntity>>
    suspend fun refresh(idToko: String, termasukArsip: Boolean = true)
    suspend fun detailBarang(idToko: String, barangId: String): BarangDetailResponseDto
    suspend fun simpanBarang(idToko: String, input: DataBarang, barangId: String? = null)
    suspend fun hapusBarang(idToko: String, barangId: String): String
    suspend fun tambahKategori(idToko: String, nama: String)
    suspend fun ubahKategori(idToko: String, kategoriId: String, nama: String)
    suspend fun hapusKategori(idToko: String, kategoriId: String)
    suspend fun detailSupplier(idToko: String, supplierId: String): com.aura.app.data.remote.dto.SupplierDetailResponseDto
    suspend fun simpanSupplier(idToko: String, supplierId: String?, nama: String, link: String?, catatan: String?)
    suspend fun ubahStatusSupplier(idToko: String, supplierId: String, status: String)
    suspend fun hapusSupplier(idToko: String, supplierId: String): Boolean
    suspend fun unggahFoto(resolver: ContentResolver, uri: Uri): String
    suspend fun ambilFile(fileId: String): ByteArray
    fun observePergerakan(idToko: String): Flow<List<PergerakanStokEntity>>
    suspend fun refreshPergerakan(
        idToko: String,
        barangId: String? = null,
        cursor: String? = null,
    ): String?
    suspend fun catatRestock(
        idToko: String,
        barangId: String,
        supplierId: String,
        jumlahMasuk: Int,
        totalDibayar: Long,
        catatan: String?,
    ): RestockCreateResponseDto
    suspend fun catatPenyesuaian(
        idToko: String,
        barangId: String,
        stokFisik: Int,
        alasan: String,
        catatan: String?,
    ): PenyesuaianStokCreateResponseDto
}

data class DataBarang(
    val nama: String,
    val satuan: String,
    val sku: String?,
    val hargaJual: Long,
    val stokMinimum: Int,
    val stokAwal: Int = 0,
    val fotoFileId: String? = null,
    val kategoriIds: List<String> = emptyList(),
    val hargaBeliAwal: Long? = null,
    val supplierId: String? = null,
)

class DefaultBarangRepository(
    private val db: AppDatabase,
    private val remote: KatalogRemoteDataSource,
    private val koneksi: KoneksiChecker,
    private val sesiRepository: SesiRepository,
) : BarangRepository {
    override fun observePergerakan(idToko: String): Flow<List<PergerakanStokEntity>> =
        db.pergerakanStokDao().observePergerakan(idToko)

    override suspend fun refreshPergerakan(idToko: String, barangId: String?, cursor: String?): String? {
        requireOwner(idToko)
        koneksi.pastikanOnline()
        val page = remote.listMovements(barangId, cursor)
        val data = page.data.map { it.toEntity(idToko) }
        db.withTransaction {
            if (cursor == null) {
                if (barangId == null) {
                    db.pergerakanStokDao().clearByToko(idToko)
                } else {
                    db.pergerakanStokDao().clearByBarang(idToko, barangId)
                }
            }
            db.pergerakanStokDao().upsertAll(data)
        }
        return page.nextCursor
    }

    override suspend fun catatRestock(
        idToko: String,
        barangId: String,
        supplierId: String,
        jumlahMasuk: Int,
        totalDibayar: Long,
        catatan: String?,
    ): RestockCreateResponseDto {
        requireOwner(idToko)
        koneksi.pastikanOnline()
        val response = remote.createRestock(
            RestockRequestDto(
                id = UUID.randomUUID().toString(),
                barangId = barangId,
                supplierId = supplierId,
                jumlahMasuk = jumlahMasuk,
                totalDibayar = totalDibayar,
                catatan = catatan?.trim()?.takeIf(String::isNotEmpty),
            ),
        )
        db.withTransaction {
            db.barangDao().upsert(response.barang.toEntity(idToko))
            db.pergerakanStokDao().upsertAll(
                listOf(response.pergerakan.toEntity(idToko, barangId, response.barang.nama)),
            )
            response.notifikasi?.let { notification ->
                db.notifikasiDao().upsertAll(
                    listOf(
                        com.aura.app.data.local.entity.NotifikasiEntity(
                            id = notification.id,
                            idToko = idToko,
                            judul = notification.judul,
                            pesan = notification.pesan,
                            jenis = notification.jenis,
                            referensiId = notification.referensiId,
                            dibuatPada = notification.dibuatPada,
                        ),
                    ),
                )
            }
            db.tokoDao().getToko(idToko)?.let { store ->
                db.tokoDao().upsert(store.copy(saldoKumulatif = response.saldoKumulatif))
            }
            db.keuanganDao().upsertPengeluaran(
                listOf(
                    com.aura.app.data.local.entity.PengeluaranEntity(
                        id = response.pengeluaranPembelian.id,
                        idToko = idToko,
                        judul = response.pengeluaranPembelian.judul,
                        jenis = com.aura.app.core.model.JenisPengeluaran.PEMBELIAN,
                        jenisBiaya = null,
                        nominal = response.pengeluaranPembelian.nominal,
                        supplierId = response.pengeluaranPembelian.supplierId,
                        buktiFileId = response.pengeluaranPembelian.buktiFileId,
                        tanggal = response.pengeluaranPembelian.tanggal,
                        status = com.aura.app.core.model.StatusCatatanKeuangan.TERCATAT,
                        catatan = response.pengeluaranPembelian.catatan,
                        dibuatPada = response.pengeluaranPembelian.dibuatPada,
                    ),
                ),
            )
        }
        return response
    }

    override suspend fun catatPenyesuaian(
        idToko: String,
        barangId: String,
        stokFisik: Int,
        alasan: String,
        catatan: String?,
    ): PenyesuaianStokCreateResponseDto {
        requireOwner(idToko)
        koneksi.pastikanOnline()
        val response = remote.createAdjustment(
            PenyesuaianStokRequestDto(
                id = UUID.randomUUID().toString(),
                barangId = barangId,
                stokFisik = stokFisik,
                alasan = alasan,
                catatan = catatan?.trim()?.takeIf(String::isNotEmpty),
            ),
        )
        db.withTransaction {
            db.barangDao().upsert(response.barang.toEntity(idToko))
            db.pergerakanStokDao().upsertAll(
                listOf(response.pergerakan.toEntity(idToko, barangId, response.barang.nama)),
            )
            response.notifikasi?.let { notification ->
                db.notifikasiDao().upsertAll(
                    listOf(
                        com.aura.app.data.local.entity.NotifikasiEntity(
                            id = notification.id,
                            idToko = idToko,
                            judul = notification.judul,
                            pesan = notification.pesan,
                            jenis = notification.jenis,
                            referensiId = notification.referensiId,
                            dibuatPada = notification.dibuatPada,
                        ),
                    ),
                )
            }
        }
        return response
    }

    override fun observeBarang(idToko: String): Flow<List<BarangEntity>> =
        db.barangDao().observeSemuaBarang(idToko)

    override fun observeKategori(idToko: String): Flow<List<KategoriEntity>> =
        db.barangDao().observeKategori(idToko)

    override fun observeRelasiKategori(): Flow<List<BarangKategoriCrossRef>> =
        db.barangDao().observeRelasiKategori()

    override fun observeSupplier(idToko: String): Flow<List<SupplierEntity>> =
        db.supplierDao().observeSuppliers(idToko)

    override suspend fun refresh(idToko: String, termasukArsip: Boolean) {
        val sesi = requireStore(idToko)
        koneksi.pastikanOnline()
        val dataBarang = remote.listBarang("AKTIF").toMutableList()
        if (termasukArsip && sesi.peran == com.aura.app.core.model.Peran.OWNER) {
            dataBarang += remote.listBarang("DIARSIPKAN")
        }
        val kategori = if (sesi.peran == com.aura.app.core.model.Peran.OWNER) remote.listKategori() else {
            dataBarang.flatMap { it.kategori }.distinctBy { it.id }
        }
        val supplier = if (sesi.peran == com.aura.app.core.model.Peran.OWNER) remote.listSupplier() else emptyList()
        db.withTransaction {
            db.barangDao().clearRelasiKategoriByToko(idToko)
            db.barangDao().replaceByToko(idToko, dataBarang.map { it.toEntity(idToko) })
            db.barangDao().clearKategoriByToko(idToko)
            db.barangDao().upsertKategori(kategori.map { it.toEntity(idToko) })
            db.barangDao().upsertRelasiKategori(dataBarang.flatMap(BarangDto::toKategoriCrossRefs))
            db.supplierDao().replaceByToko(idToko, supplier.map { it.toEntity(idToko) })
        }
    }

    override suspend fun detailBarang(idToko: String, barangId: String): BarangDetailResponseDto {
        requireStore(idToko)
        koneksi.pastikanOnline()
        val detail = remote.detailBarang(barangId)
        simpanDetail(idToko, detail.barang, detail.hargaBeliSupplier.map { it.toEntity(idToko) })
        return detail
    }

    override suspend fun simpanBarang(idToko: String, input: DataBarang, barangId: String?) {
        requireOwner(idToko)
        koneksi.pastikanOnline()
        if (barangId == null) {
            val response = remote.createBarang(
                BarangCreateRequestDto(
                    id = UUID.randomUUID().toString(),
                    nama = input.nama,
                    satuan = input.satuan,
                    sku = input.sku,
                    hargaJual = input.hargaJual,
                    stokMinimum = input.stokMinimum,
                    stokAwal = input.stokAwal,
                    fotoFileId = input.fotoFileId,
                    kategoriIds = input.kategoriIds,
                    hargaBeliAwal = input.hargaBeliAwal,
                    supplierId = input.supplierId,
                ),
            )
            simpanDetail(idToko, response.barang, emptyList())
        } else {
            val response = remote.updateBarang(
                barangId,
                BarangUpdateRequestDto(
                    nama = input.nama,
                    satuan = input.satuan,
                    sku = input.sku,
                    hargaJual = input.hargaJual,
                    stokMinimum = input.stokMinimum,
                    fotoFileId = input.fotoFileId,
                    kategoriIds = input.kategoriIds,
                ),
            )
            simpanDetail(idToko, response, emptyList())
        }
    }

    override suspend fun hapusBarang(idToko: String, barangId: String): String {
        requireOwner(idToko)
        koneksi.pastikanOnline()
        val response = remote.deleteBarang(barangId)
        val dao = db.barangDao()
        if (response.status == "DIARSIPKAN") {
            dao.getById(barangId)?.let { dao.upsert(it.copy(status = com.aura.app.core.model.StatusBarang.DIARSIPKAN)) }
        } else {
            dao.clearRelasiBarang(barangId)
            dao.clearBarang(barangId)
        }
        return response.status
    }

    override suspend fun tambahKategori(idToko: String, nama: String) {
        requireOwner(idToko)
        koneksi.pastikanOnline()
        val category = remote.createKategori(nama)
        db.barangDao().upsertKategori(listOf(category.toEntity(idToko)))
    }

    override suspend fun ubahKategori(idToko: String, kategoriId: String, nama: String) {
        requireOwner(idToko)
        koneksi.pastikanOnline()
        val category = remote.updateKategori(kategoriId, nama)
        db.barangDao().upsertKategori(listOf(category.toEntity(idToko)))
    }

    override suspend fun hapusKategori(idToko: String, kategoriId: String) {
        requireOwner(idToko)
        koneksi.pastikanOnline()
        remote.deleteKategori(kategoriId)
        db.withTransaction {
            db.barangDao().clearRelasiKategori(kategoriId)
            db.barangDao().deleteKategori(kategoriId)
        }
    }

    override suspend fun simpanSupplier(
        idToko: String,
        supplierId: String?,
        nama: String,
        link: String?,
        catatan: String?,
    ) {
        requireOwner(idToko)
        koneksi.pastikanOnline()
        val supplier = if (supplierId == null) {
            remote.createSupplier(nama, link, catatan)
        } else {
            remote.updateSupplier(supplierId, nama, link, catatan)
        }
        db.supplierDao().upsert(supplier.toEntity(idToko))
    }

    override suspend fun ubahStatusSupplier(idToko: String, supplierId: String, status: String) {
        requireOwner(idToko)
        koneksi.pastikanOnline()
        val supplier = remote.setSupplierStatus(supplierId, status)
        db.supplierDao().upsert(supplier.toEntity(idToko))
    }

    override suspend fun detailSupplier(idToko: String, supplierId: String): com.aura.app.data.remote.dto.SupplierDetailResponseDto {
        requireOwner(idToko)
        koneksi.pastikanOnline()
        return remote.detailSupplier(supplierId)
    }

    override suspend fun hapusSupplier(idToko: String, supplierId: String): Boolean {
        requireOwner(idToko)
        koneksi.pastikanOnline()
        try {
            remote.deleteSupplier(supplierId)
            db.supplierDao().delete(supplierId)
            return true
        } catch (error: com.aura.app.data.remote.ApiServerException) {
            if (error.kode != "SUPPLIER_SUDAH_DIPAKAI") throw error
            val updated = remote.setSupplierStatus(supplierId, "NONAKTIF")
            db.supplierDao().upsert(updated.toEntity(idToko))
            return false
        }
    }

    override suspend fun unggahFoto(resolver: ContentResolver, uri: Uri): String = withContext(Dispatchers.IO) {
        val sesi = sesiRepository.sesiAktif.first()
        if (sesi?.peran != com.aura.app.core.model.Peran.OWNER) {
            throw IllegalStateException("Hanya Owner yang dapat mengubah data ini.")
        }
        koneksi.pastikanOnline()
        val mimeType = resolver.getType(uri)?.takeIf { it in setOf("image/jpeg", "image/png", "image/webp") }
            ?: throw IllegalArgumentException("Foto harus berupa JPEG, PNG, atau WebP.")
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalArgumentException("Foto tidak dapat dibaca.")
        require(bytes.isNotEmpty() && bytes.size <= MAX_UPLOAD_BYTES) {
            "Ukuran foto harus lebih dari 0 dan maksimal 10 MiB."
        }
        val filename = "foto-${UUID.randomUUID()}.${mimeType.substringAfter('/')}"
        remote.uploadFile(filename, mimeType, bytes).fileId
    }

    override suspend fun ambilFile(fileId: String): ByteArray {
        if (sesiRepository.sesiAktif.first() == null) {
            throw IllegalStateException("Sesi tidak tersedia.")
        }
        koneksi.pastikanOnline()
        return remote.downloadFile(fileId)
    }

    private suspend fun requireOwner(idToko: String) {
        val sesi = requireStore(idToko)
        if (sesi.peran != com.aura.app.core.model.Peran.OWNER) {
            throw IllegalStateException("Hanya Owner yang dapat mengubah data ini.")
        }
    }

    private suspend fun requireStore(idToko: String): SesiAktif {
        val sesi = sesiRepository.sesiAktif.first()
            ?: throw IllegalStateException("Sesi tidak tersedia.")
        if (sesi.idToko != idToko) throw IllegalStateException("Toko aktif tidak sesuai dengan data.")
        return sesi
    }

    private suspend fun simpanDetail(
        idToko: String,
        dto: BarangDto,
        hargaBeli: List<HargaBeliSupplierEntity>,
    ) {
        val dao = db.barangDao()
        db.withTransaction {
            dao.upsert(dto.toEntity(idToko))
            dao.clearRelasiBarang(dto.id)
            dao.upsertRelasiKategori(dto.toKategoriCrossRefs())
            if (hargaBeli.isNotEmpty()) dao.upsertHargaBeliSupplier(hargaBeli)
        }
    }

    companion object {
        const val MAX_UPLOAD_BYTES = 10 * 1024 * 1024
    }
}
