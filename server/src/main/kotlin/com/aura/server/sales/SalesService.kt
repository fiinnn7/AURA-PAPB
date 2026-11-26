package com.aura.server.sales

import com.aura.server.auth.Akun
import com.aura.server.auth.Peran
import com.aura.server.plugins.ApiException
import com.google.gson.Gson
import io.ktor.http.HttpStatusCode
import java.security.MessageDigest
import java.util.UUID

internal class SalesService(private val repository: ExposedSalesRepository = ExposedSalesRepository()) {
    private val gson = Gson()

    fun create(account: Akun, request: TransaksiRequest): TransaksiBuatResponse {
        val id = request.id ?: invalid("ID_TRANSAKSI_TIDAK_VALID", "ID transaksi wajib diisi.")
        if ((request.catatan?.length ?: 0) > MAX_NOTE_LENGTH) {
            invalid("CATATAN_TIDAK_VALID", "Catatan maksimal $MAX_NOTE_LENGTH karakter.")
        }
        request.barangDibeli?.forEach { line ->
            if (line.barangId == null) invalid("BARANG_TIDAK_VALID", "Barang wajib dipilih.")
            if (line.jumlah == null || line.jumlah <= 0) invalid("JUMLAH_TIDAK_VALID", "Jumlah Barang harus lebih dari nol.")
        }
        return repository.create(account, request, hash(request))
    }

    fun list(account: Akun, hari: String?, status: String?, limit: Int?, cursor: String?): TransaksiListResponse {
        val pageSize = limit ?: DEFAULT_PAGE_SIZE
        if (pageSize !in 1..MAX_PAGE_SIZE) invalid("LIMIT_TIDAK_VALID", "Jumlah data per halaman tidak valid.")
        if (hari != null && runCatching { java.time.LocalDate.parse(hari) }.isFailure) {
            invalid("HARI_TIDAK_VALID", "Tanggal riwayat tidak valid.")
        }
        if (status != null && status !in TRANSACTION_STATUSES) {
            invalid("STATUS_TRANSAKSI_TIDAK_VALID", "Status transaksi tidak valid.")
        }
        return repository.list(account, hari, status, pageSize, cursor)
    }

    fun detail(account: Akun, id: UUID): TransaksiDetailResponse = repository.detail(account, id)

    fun cancel(account: Akun, id: UUID, request: PembatalanRequest): TransaksiBatalResponse {
        requireRole(account, Peran.OWNER)
        val reason = request.alasan?.trim()?.takeIf(String::isNotEmpty)
            ?: invalid("ALASAN_TIDAK_VALID", "Alasan pembatalan wajib diisi.")
        if (reason.length > MAX_REASON_LENGTH) invalid("ALASAN_TIDAK_VALID", "Alasan maksimal $MAX_REASON_LENGTH karakter.")
        return repository.cancel(account, id, reason)
    }

    fun requestCancellation(account: Akun, transactionId: UUID, request: PengajuanPembatalanRequest): PengajuanPembatalanResponse {
        requireRole(account, Peran.KASIR)
        val id = request.id ?: invalid("ID_REQUEST_TIDAK_VALID", "ID pengajuan wajib diisi.")
        val reason = request.alasan?.trim()?.takeIf(String::isNotEmpty)
            ?: invalid("ALASAN_TIDAK_VALID", "Alasan pembatalan wajib diisi.")
        if (reason.length > MAX_REASON_LENGTH) invalid("ALASAN_TIDAK_VALID", "Alasan maksimal $MAX_REASON_LENGTH karakter.")
        val normalized = request.copy(alasan = reason)
        return repository.requestCancellation(account, transactionId, normalized, hash(normalized))
    }

    private fun hash(value: Any): String =
        MessageDigest.getInstance("SHA-256")
            .digest(gson.toJson(value).toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private fun requireRole(account: Akun, role: Peran) {
        if (account.peran != role) {
            val message = if (role == Peran.OWNER) "Hanya Owner yang dapat mengubah data ini." else "Fitur ini hanya dapat digunakan Kasir."
            throw ApiException(HttpStatusCode.Forbidden, "AKSES_DITOLAK", message)
        }
    }

    private fun invalid(code: String, message: String): Nothing =
        throw ApiException(HttpStatusCode.BadRequest, code, message)

    companion object {
        const val DEFAULT_PAGE_SIZE = 50
        const val MAX_PAGE_SIZE = 100
        const val MAX_NOTE_LENGTH = 1000
        const val MAX_REASON_LENGTH = 500
        val TRANSACTION_STATUSES = setOf("SELESAI", "DIBATALKAN")
    }
}

internal data class SalesComponents(val service: SalesService)

internal fun salesComponents(): SalesComponents = SalesComponents(SalesService())
