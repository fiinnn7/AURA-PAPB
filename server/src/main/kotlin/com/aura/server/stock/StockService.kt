package com.aura.server.stock

import com.aura.server.auth.Akun
import com.aura.server.auth.Peran
import com.aura.server.catalog.CatalogService
import com.aura.server.plugins.ApiException
import com.google.gson.Gson
import io.ktor.http.HttpStatusCode
import java.security.MessageDigest
import java.util.Base64
import java.util.UUID

internal class StockService(
    private val repository: StockRepository,
    private val catalogService: CatalogService,
) {
    fun createRestock(account: Akun, request: RestockRequest): RestockCreateResponse {
        requireOwner(account)
        val id = request.id ?: invalid("ID_RESTOCK_TIDAK_VALID", "ID Restock wajib diisi.")
        val barangId = request.barangId ?: invalid("BARANG_TIDAK_VALID", "Barang wajib dipilih.")
        val supplierId = request.supplierId ?: invalid("SUPPLIER_TIDAK_VALID", "Supplier wajib dipilih.")
        val quantity = request.jumlahMasuk ?: invalid("JUMLAH_TIDAK_VALID", "Jumlah Restock wajib diisi.")
        val total = request.totalDibayar ?: invalid("TOTAL_TIDAK_VALID", "Total dibayar wajib diisi.")
        if (quantity <= 0) invalid("JUMLAH_TIDAK_VALID", "Jumlah Restock harus lebih dari nol.")
        if (total < 0) invalid("TOTAL_TIDAK_VALID", "Total dibayar tidak boleh negatif.")
        validateNote(request.catatan)
        request.notaFileId?.let { catalogService.findFile(account, it) }

        return repository.createRestock(
            account,
            RestockInput(
                id = id,
                barangId = barangId,
                supplierId = supplierId,
                jumlahMasuk = quantity,
                totalDibayar = total,
                tanggal = request.tanggal ?: System.currentTimeMillis(),
                notaFileId = request.notaFileId,
                catatan = request.catatan?.trim()?.takeIf(String::isNotEmpty),
            ),
            requestHash(request),
        )
    }

    fun createAdjustment(account: Akun, request: PenyesuaianStokRequest): PenyesuaianStokCreateResponse {
        requireOwner(account)
        val id = request.id ?: invalid("ID_PENYESUAIAN_TIDAK_VALID", "ID Penyesuaian wajib diisi.")
        val barangId = request.barangId ?: invalid("BARANG_TIDAK_VALID", "Barang wajib dipilih.")
        val physicalStock = request.stokFisik ?: invalid("STOK_FISIK_TIDAK_VALID", "Stok fisik wajib diisi.")
        val reason = request.alasan ?: invalid("ALASAN_TIDAK_VALID", "Alasan Penyesuaian wajib dipilih.")
        if (physicalStock < 0) invalid("STOK_FISIK_TIDAK_VALID", "Stok fisik tidak boleh negatif.")
        if (reason !in ADJUSTMENT_REASONS) invalid("ALASAN_TIDAK_VALID", "Alasan Penyesuaian tidak valid.")
        validateNote(request.catatan)
        request.buktiFileId?.let { catalogService.findFile(account, it) }

        return repository.createAdjustment(
            account,
            PenyesuaianStokInput(
                id = id,
                barangId = barangId,
                stokFisik = physicalStock,
                alasan = reason,
                catatan = request.catatan?.trim()?.takeIf(String::isNotEmpty),
                buktiFileId = request.buktiFileId,
                tanggal = request.tanggal ?: System.currentTimeMillis(),
            ),
            requestHash(request),
        )
    }

    fun listMovements(
        account: Akun,
        from: Long?,
        through: Long?,
        type: String?,
        barangId: UUID?,
        limit: Int?,
        cursor: String?,
    ): PergerakanStokListResponse {
        requireOwner(account)
        if (from != null && through != null && from > through) {
            invalid("RENTANG_TANGGAL_TIDAK_VALID", "Tanggal awal tidak boleh melewati tanggal akhir.")
        }
        if (type != null && type !in MOVEMENT_TYPES) invalid("JENIS_PERGERAKAN_TIDAK_VALID", "Jenis pergerakan tidak valid.")
        val pageSize = limit ?: DEFAULT_PAGE_SIZE
        if (pageSize !in 1..MAX_PAGE_SIZE) invalid("LIMIT_TIDAK_VALID", "Jumlah data per halaman tidak valid.")
        val decodedCursor = cursor?.let(::decodeCursor)
        val page = repository.listMovements(
            storeId = account.idToko,
            from = from,
            through = through,
            type = type,
            barangId = barangId,
            limit = pageSize,
            cursor = decodedCursor,
        )
        return PergerakanStokListResponse(
            data = page.data,
            nextCursor = page.nextCursor?.let(::encodeCursor),
        )
    }

    fun listCardMovements(
        account: Akun,
        barangId: UUID,
        from: Long?,
        through: Long?,
        limit: Int?,
        cursor: String?,
    ): PergerakanStokListResponse {
        requireOwner(account)
        catalogService.detailBarang(account, barangId)
        return listMovements(account, from, through, null, barangId, limit, cursor)
    }

    private fun requestHash(request: Any): String =
        MessageDigest.getInstance("SHA-256")
            .digest(Gson().toJson(request).toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private fun decodeCursor(value: String): PergerakanStokCursor {
        val decoded = runCatching {
            String(Base64.getUrlDecoder().decode(value), Charsets.UTF_8)
        }.getOrNull() ?: invalid("CURSOR_TIDAK_VALID", "Cursor pergerakan tidak valid.")
        val parts = decoded.split(':', limit = 2)
        val timestamp = parts.getOrNull(0)?.toLongOrNull()
            ?: invalid("CURSOR_TIDAK_VALID", "Cursor pergerakan tidak valid.")
        val id = parts.getOrNull(1)?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            ?: invalid("CURSOR_TIDAK_VALID", "Cursor pergerakan tidak valid.")
        return PergerakanStokCursor(timestamp, id)
    }

    private fun encodeCursor(cursor: PergerakanStokCursor): String =
        Base64.getUrlEncoder().withoutPadding()
            .encodeToString("${cursor.dibuatPada}:${cursor.id}".toByteArray(Charsets.UTF_8))

    private fun requireOwner(account: Akun) {
        if (account.peran != Peran.OWNER) {
            throw ApiException(HttpStatusCode.Forbidden, "AKSES_DITOLAK", "Hanya Owner yang dapat mengubah data ini.")
        }
    }

    private fun validateNote(note: String?) {
        if (note != null && note.length > MAX_NOTE_LENGTH) {
            invalid("CATATAN_TIDAK_VALID", "Catatan maksimal $MAX_NOTE_LENGTH karakter.")
        }
    }

    private fun invalid(code: String, message: String): Nothing =
        throw ApiException(HttpStatusCode.BadRequest, code, message)

    companion object {
        const val DEFAULT_PAGE_SIZE = 30
        const val MAX_PAGE_SIZE = 100
        const val MAX_NOTE_LENGTH = 1000
        val ADJUSTMENT_REASONS = setOf("RUSAK", "HILANG", "KADALUARSA", "SALAH_CATAT", "LAINNYA")
        val MOVEMENT_TYPES = setOf("MASUK", "KELUAR", "PENYESUAIAN")
    }
}

internal data class StockComponents(
    val service: StockService,
)

internal fun stockComponents(
    catalogService: CatalogService,
    repository: StockRepository = ExposedStockRepository(),
): StockComponents = StockComponents(StockService(repository, catalogService))
