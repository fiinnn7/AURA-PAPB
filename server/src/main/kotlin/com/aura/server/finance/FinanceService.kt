package com.aura.server.finance

import com.aura.server.auth.Akun
import com.aura.server.auth.Peran
import com.aura.server.plugins.ApiException
import com.google.gson.Gson
import io.ktor.http.HttpStatusCode
import java.security.MessageDigest
import java.util.Base64
import java.util.UUID

internal class FinanceService(
    private val repository: FinanceRepository = ExposedFinanceRepository(),
) {
    private val gson = Gson()

    fun createExpense(account: Akun, request: PengeluaranRequest): PengeluaranCreateResponse {
        requireOwner(account)
        val id = request.id ?: invalid("ID_PENGELUARAN_TIDAK_VALID", "ID Pengeluaran wajib diisi.")
        val title = validateTitle(request.judul, "Judul Pengeluaran")
        val type = request.jenisBiaya?.trim()?.uppercase()
            ?: invalid("JENIS_BIAYA_TIDAK_VALID", "Jenis biaya wajib dipilih.")
        if (type !in EXPENSE_TYPES) invalid("JENIS_BIAYA_TIDAK_VALID", "Jenis biaya tidak valid.")
        val amount = request.nominal ?: invalid("NOMINAL_TIDAK_VALID", "Nominal Pengeluaran wajib diisi.")
        if (amount < 0) invalid("NOMINAL_TIDAK_VALID", "Nominal tidak boleh negatif.")
        val now = System.currentTimeMillis()
        val date = validateDate(request.tanggal ?: now, now)
        val note = validateNote(request.catatan)
        val normalized = request.copy(judul = title, jenisBiaya = type, catatan = note)
        return repository.createExpense(
            account,
            PengeluaranInput(id, title, type, amount, date, note, request.buktiFileId),
            hash(normalized),
        )
    }

    fun listExpenses(
        account: Akun,
        from: Long?,
        through: Long?,
        type: String?,
        status: String?,
        limit: Int?,
        cursor: String?,
    ): PengeluaranListResponse {
        requireOwner(account)
        validateRange(from, through)
        if (type != null && type !in EXPENSE_TYPES_FILTER) invalid("JENIS_PENGELUARAN_TIDAK_VALID", "Jenis Pengeluaran tidak valid.")
        if (status != null && status !in EXPENSE_STATUSES) invalid("STATUS_PENGELUARAN_TIDAK_VALID", "Status Pengeluaran tidak valid.")
        val pageSize = validateLimit(limit)
        val page = repository.listExpenses(account, from, through, type, status, pageSize, cursor?.let(::decodeExpenseCursor))
        return PengeluaranListResponse(page.data, page.total, page.nextCursor?.let(::encodeExpenseCursor))
    }

    fun expenseDetail(account: Akun, id: UUID): PengeluaranDetailResponse {
        requireOwner(account)
        val expense = repository.expenseDetail(account.idToko, id)
            ?: notFound("PENGELUARAN_TIDAK_DITEMUKAN", "Pengeluaran tidak ditemukan.")
        return PengeluaranDetailResponse(expense)
    }

    fun cancelExpense(account: Akun, id: UUID, request: PembatalanKeuanganRequest): PengeluaranCancelResponse {
        requireOwner(account)
        return repository.cancelExpense(account, id, validateReason(request.alasan))
    }

    fun createIncome(account: Akun, request: PemasukanRequest): PemasukanCreateResponse {
        requireOwner(account)
        val id = request.id ?: invalid("ID_PEMASUKAN_TIDAK_VALID", "ID Pemasukan wajib diisi.")
        val title = validateTitle(request.judul, "Judul Pemasukan")
        val amount = request.nominal ?: invalid("NOMINAL_TIDAK_VALID", "Nominal Pemasukan wajib diisi.")
        if (amount < 0) invalid("NOMINAL_TIDAK_VALID", "Nominal tidak boleh negatif.")
        val now = System.currentTimeMillis()
        val date = validateDate(request.tanggal ?: now, now)
        val note = validateNote(request.catatan)
        val normalized = request.copy(judul = title, catatan = note)
        return repository.createIncome(
            account,
            PemasukanInput(id, title, amount, date, note, request.buktiFileId),
            hash(normalized),
        )
    }

    fun listIncomes(
        account: Akun,
        from: Long?,
        through: Long?,
        type: String?,
        status: String?,
        limit: Int?,
        cursor: String?,
    ): PemasukanListResponse {
        requireOwner(account)
        validateRange(from, through)
        if (type != null && type !in INCOME_TYPES) invalid("JENIS_PEMASUKAN_TIDAK_VALID", "Jenis Pemasukan tidak valid.")
        if (status != null && status !in INCOME_STATUSES) invalid("STATUS_PEMASUKAN_TIDAK_VALID", "Status Pemasukan tidak valid.")
        val pageSize = validateLimit(limit)
        val page = repository.listIncomes(account, from, through, type, status, pageSize, cursor?.let(::decodeIncomeCursor))
        return PemasukanListResponse(page.data, page.total, page.nextCursor?.let(::encodeIncomeCursor))
    }

    fun incomeDetail(account: Akun, id: UUID): PemasukanDetailResponse {
        requireOwner(account)
        val income = repository.incomeDetail(account.idToko, id)
            ?: notFound("PEMASUKAN_TIDAK_DITEMUKAN", "Pemasukan tidak ditemukan.")
        return PemasukanDetailResponse(income)
    }

    fun cancelIncome(account: Akun, id: UUID, request: PembatalanKeuanganRequest): PemasukanCancelResponse {
        requireOwner(account)
        return repository.cancelIncome(account, id, validateReason(request.alasan))
    }

    fun paymentMethod(account: Akun): MetodePembayaranResponse =
        repository.paymentMethod(account.idToko)
            ?: notFound("TOKO_TIDAK_DITEMUKAN", "Toko tidak ditemukan.")

    fun updatePaymentMethod(account: Akun, request: MetodePembayaranRequest): MetodePembayaranResponse {
        requireOwner(account)
        val normalized = request.copy(
            namaPenerima = normalizeOptional(request.namaPenerima, MAX_RECIPIENT_NAME, "Nama penerima"),
            infoRekening = normalizeOptional(request.infoRekening, MAX_ACCOUNT_INFO, "Info rekening"),
        )
        return repository.updatePaymentMethod(account, normalized)
    }

    private fun validateTitle(value: String?, label: String): String {
        val title = value?.trim().orEmpty()
        if (title.isEmpty() || title.length > MAX_TITLE_LENGTH) {
            invalid("JUDUL_TIDAK_VALID", "$label wajib diisi dan maksimal $MAX_TITLE_LENGTH karakter.")
        }
        return title
    }

    private fun validateNote(value: String?): String? {
        val note = value?.trim()?.takeIf(String::isNotEmpty)
        if ((note?.length ?: 0) > MAX_NOTE_LENGTH) invalid("CATATAN_TIDAK_VALID", "Catatan maksimal $MAX_NOTE_LENGTH karakter.")
        return note
    }

    private fun validateReason(value: String?): String {
        val reason = value?.trim()?.takeIf(String::isNotEmpty)
            ?: invalid("ALASAN_TIDAK_VALID", "Alasan pembatalan wajib diisi.")
        if (reason.length > MAX_REASON_LENGTH) invalid("ALASAN_TIDAK_VALID", "Alasan maksimal $MAX_REASON_LENGTH karakter.")
        return reason
    }

    private fun validateDate(value: Long, now: Long): Long {
        if (value < 0) invalid("TANGGAL_TIDAK_VALID", "Tanggal tidak valid.")
        if (value > now) invalid("TANGGAL_MASA_DEPAN_TIDAK_DIIJINKAN", "Tanggal catatan tidak boleh di masa depan.")
        return value
    }

    private fun normalizeOptional(value: String?, maximumLength: Int, label: String): String? {
        val normalized = value?.trim()?.takeIf(String::isNotEmpty)
        if ((normalized?.length ?: 0) > maximumLength) {
            invalid("METODE_PEMBAYARAN_TIDAK_VALID", "$label maksimal $maximumLength karakter.")
        }
        return normalized
    }

    private fun validateRange(from: Long?, through: Long?) {
        if (from != null && through != null && from > through) {
            invalid("RENTANG_TANGGAL_TIDAK_VALID", "Tanggal awal tidak boleh melewati tanggal akhir.")
        }
    }

    private fun validateLimit(limit: Int?): Int {
        val pageSize = limit ?: DEFAULT_PAGE_SIZE
        if (pageSize !in 1..MAX_PAGE_SIZE) invalid("LIMIT_TIDAK_VALID", "Jumlah data per halaman tidak valid.")
        return pageSize
    }

    private fun decodeExpenseCursor(value: String): FinanceExpenseCursor {
        val parts = decode(value).split(':', limit = 2)
        val timestamp = parts.getOrNull(0)?.toLongOrNull()
            ?: invalid("CURSOR_TIDAK_VALID", "Cursor Pengeluaran tidak valid.")
        val id = parts.getOrNull(1)?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            ?: invalid("CURSOR_TIDAK_VALID", "Cursor Pengeluaran tidak valid.")
        return FinanceExpenseCursor(timestamp, id)
    }

    private fun encodeExpenseCursor(cursor: FinanceExpenseCursor): String =
        encode("${cursor.tanggal}:${cursor.id}")

    private fun decodeIncomeCursor(value: String): FinanceIncomeCursor {
        val parts = decode(value).split('|', limit = 3)
        val timestamp = parts.getOrNull(0)?.toLongOrNull()
            ?: invalid("CURSOR_TIDAK_VALID", "Cursor Pemasukan tidak valid.")
        val type = parts.getOrNull(1)
            ?.takeIf { it in INCOME_TYPES }
            ?: invalid("CURSOR_TIDAK_VALID", "Cursor Pemasukan tidak valid.")
        val id = parts.getOrNull(2)?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            ?: invalid("CURSOR_TIDAK_VALID", "Cursor Pemasukan tidak valid.")
        return FinanceIncomeCursor(timestamp, type, id)
    }

    private fun encodeIncomeCursor(cursor: FinanceIncomeCursor): String =
        encode("${cursor.tanggal}|${cursor.jenis}|${cursor.id}")

    private fun decode(value: String): String = runCatching {
        String(Base64.getUrlDecoder().decode(value), Charsets.UTF_8)
    }.getOrElse {
        invalid("CURSOR_TIDAK_VALID", "Cursor tidak valid.")
    }

    private fun encode(value: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(Charsets.UTF_8))

    private fun hash(value: Any): String =
        MessageDigest.getInstance("SHA-256")
            .digest(gson.toJson(value).toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private fun requireOwner(account: Akun) {
        if (account.peran != Peran.OWNER) {
            throw ApiException(HttpStatusCode.Forbidden, "AKSES_DITOLAK", "Hanya Owner yang dapat mengubah data ini.")
        }
    }

    private fun invalid(code: String, message: String): Nothing =
        throw ApiException(HttpStatusCode.BadRequest, code, message)

    private fun notFound(code: String, message: String): Nothing =
        throw ApiException(HttpStatusCode.NotFound, code, message)

    companion object {
        const val DEFAULT_PAGE_SIZE = 50
        const val MAX_PAGE_SIZE = 100
        const val MAX_TITLE_LENGTH = 120
        const val MAX_NOTE_LENGTH = 1000
        const val MAX_REASON_LENGTH = 500
        const val MAX_RECIPIENT_NAME = 120
        const val MAX_ACCOUNT_INFO = 500
        val EXPENSE_TYPES = setOf("LISTRIK", "SEWA", "GAJI", "LAINNYA")
        val EXPENSE_TYPES_FILTER = setOf("PEMBELIAN", "KUSTOM")
        val EXPENSE_STATUSES = setOf("TERCATAT", "DIBATALKAN")
        val INCOME_TYPES = setOf("PENJUALAN", "KUSTOM")
        val INCOME_STATUSES = setOf("SELESAI", "TERCATAT", "DIBATALKAN")
    }
}

internal data class FinanceComponents(val service: FinanceService)

internal fun financeComponents(repository: FinanceRepository = ExposedFinanceRepository()): FinanceComponents =
    FinanceComponents(FinanceService(repository))
