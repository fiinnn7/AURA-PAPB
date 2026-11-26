package com.aura.server.finance

import com.aura.server.auth.Akun
import com.aura.server.db.FileTable
import com.aura.server.db.LogAktivitasTable
import com.aura.server.db.PemasukanKustomTable
import com.aura.server.db.PengeluaranTable
import com.aura.server.db.TokoTable
import com.aura.server.db.TransaksiPenjualanTable
import com.aura.server.plugins.ApiException
import com.google.gson.Gson
import io.ktor.http.HttpStatusCode
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.core.sum
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.util.UUID
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

internal class ExposedFinanceRepository : FinanceRepository {
    private val gson = Gson()

    override fun createExpense(
        account: Akun,
        input: PengeluaranInput,
        requestHash: String,
    ): PengeluaranCreateResponse = transaction {
        val balance = lockStore(account.idToko)
        val id = input.id.toKotlinUuid()
        PengeluaranTable.selectAll().where { PengeluaranTable.id eq id }.singleOrNull()?.let { row ->
            if (row[PengeluaranTable.idToko] != account.idToko ||
                row[PengeluaranTable.jenis] != "KUSTOM" ||
                row[PengeluaranTable.requestHash]?.trim() != requestHash
            ) {
                conflict("ID_REQUEST_BENTROKAN", "ID Pengeluaran sudah digunakan untuk data berbeda.")
            }
            val snapshot = row[PengeluaranTable.responseSnapshot]
                ?: conflict("ID_REQUEST_BENTROKAN", "ID Pengeluaran sudah digunakan untuk data berbeda.")
            return@transaction gson.fromJson(snapshot, PengeluaranCreateResponse::class.java)
                ?: error("Respons Pengeluaran idempoten tidak tersedia.")
        }
        ensureFile(account.idToko, input.buktiFileId)
        val now = System.currentTimeMillis()
        val newBalance = try {
            Math.subtractExact(balance, input.nominal)
        } catch (_: ArithmeticException) {
            invalid("SALDO_TIDAK_VALID", "Perubahan saldo melebihi batas yang didukung.")
        }
        PengeluaranTable.insert {
            it[PengeluaranTable.id] = id
            it[idToko] = account.idToko
            it[jenis] = "KUSTOM"
            it[jenisBiaya] = input.jenisBiaya
            it[judul] = input.judul
            it[barangId] = null
            it[supplierId] = null
            it[jumlah] = null
            it[hargaPerItem] = null
            it[nominal] = input.nominal
            it[tanggal] = input.tanggal
            it[catatan] = input.catatan
            it[buktiFileId] = input.buktiFileId?.toKotlinUuid()
            it[status] = "TERCATAT"
            it[alasanPembatalan] = null
            it[referensiId] = null
            it[jenisReferensi] = null
            it[dibuatOleh] = account.id.toKotlinUuid()
            it[dibuatPada] = now
            it[dibatalkanOleh] = null
            it[dibatalkanPada] = null
            it[PengeluaranTable.requestHash] = requestHash
            it[PengeluaranTable.responseSnapshot] = ""
        }
        TokoTable.update({ TokoTable.idToko eq account.idToko }) {
            it[saldoKumulatif] = newBalance
        }
        log(account, "PENGELUARAN_DICATAT", input.id, "PENGELUARAN", now)
        val response = PengeluaranCreateResponse(expenseResponse(input, account, now), newBalance)
        PengeluaranTable.update({ PengeluaranTable.id eq id }) {
            it[PengeluaranTable.responseSnapshot] = gson.toJson(response)
        }
        response
    }

    override fun listExpenses(
        account: Akun,
        from: Long?,
        through: Long?,
        type: String?,
        status: String?,
        limit: Int,
        cursor: FinanceExpenseCursor?,
    ): FinanceExpensePage = transaction {
        val condition = expenseCondition(account.idToko, from, through, type, status, cursor)
        val rows = PengeluaranTable.selectAll().where { condition }
            .orderBy(PengeluaranTable.tanggal to SortOrder.DESC, PengeluaranTable.id to SortOrder.DESC)
            .limit(limit + 1)
            .toList()
        val totalCondition = expenseCondition(account.idToko, from, through, type, status = null, cursor = null)
        val totalExpression = PengeluaranTable.nominal.sum()
        val total = if (status == null || status == "TERCATAT") {
            PengeluaranTable.select(totalExpression)
                .where { totalCondition and (PengeluaranTable.status eq "TERCATAT") }
                .singleOrNull()?.get(totalExpression) ?: 0L
        } else {
            0L
        }
        val hasMore = rows.size > limit
        val dataRows = rows.take(limit)
        FinanceExpensePage(
            data = dataRows.map { it.toExpenseResponse() },
            total = total,
            nextCursor = if (hasMore) dataRows.lastOrNull()?.let {
                FinanceExpenseCursor(it[PengeluaranTable.tanggal], it[PengeluaranTable.id].toJavaUuid())
            } else {
                null
            },
        )
    }

    override fun expenseDetail(storeId: String, id: UUID): PengeluaranResponse? = transaction {
        PengeluaranTable.selectAll().where {
            (PengeluaranTable.idToko eq storeId) and (PengeluaranTable.id eq id.toKotlinUuid())
        }.singleOrNull()?.toExpenseResponse()
    }

    override fun cancelExpense(account: Akun, id: UUID, reason: String): PengeluaranCancelResponse = transaction {
        val balance = lockStore(account.idToko)
        val row = PengeluaranTable.selectAll().where {
            (PengeluaranTable.idToko eq account.idToko) and (PengeluaranTable.id eq id.toKotlinUuid())
        }.singleOrNull() ?: notFound("PENGELUARAN_TIDAK_DITEMUKAN", "Pengeluaran tidak ditemukan.")
        if (row[PengeluaranTable.jenis] != "KUSTOM") {
            conflict("PENGELUARAN_TIDAK_DAPAT_DIBATALKAN", "Pengeluaran Pembelian tidak dapat dibatalkan terpisah dari Restock.")
        }
        if (row[PengeluaranTable.status] != "TERCATAT") {
            conflict("PENGELUARAN_SUDAH_DIBATALKAN", "Pengeluaran sudah dibatalkan.")
        }
        val newBalance = try {
            Math.addExact(balance, row[PengeluaranTable.nominal])
        } catch (_: ArithmeticException) {
            invalid("SALDO_TIDAK_VALID", "Perubahan saldo melebihi batas yang didukung.")
        }
        val now = System.currentTimeMillis()
        val updated = PengeluaranTable.update({
            (PengeluaranTable.idToko eq account.idToko) and
                (PengeluaranTable.id eq id.toKotlinUuid()) and
                (PengeluaranTable.status eq "TERCATAT")
        }) {
            it[status] = "DIBATALKAN"
            it[alasanPembatalan] = reason
            it[dibatalkanOleh] = account.id.toKotlinUuid()
            it[dibatalkanPada] = now
        }
        if (updated != 1) conflict("PENGELUARAN_SUDAH_DIBATALKAN", "Pengeluaran sudah dibatalkan.")
        TokoTable.update({ TokoTable.idToko eq account.idToko }) {
            it[saldoKumulatif] = newBalance
        }
        log(account, "PENGELUARAN_DIBATALKAN", id, "PENGELUARAN", now)
        PengeluaranCancelResponse(
            pengeluaran = PengeluaranTable.selectAll().where {
                (PengeluaranTable.idToko eq account.idToko) and (PengeluaranTable.id eq id.toKotlinUuid())
            }.single().toExpenseResponse(),
            saldoKumulatif = newBalance,
        )
    }

    override fun createIncome(
        account: Akun,
        input: PemasukanInput,
        requestHash: String,
    ): PemasukanCreateResponse = transaction {
        val balance = lockStore(account.idToko)
        val id = input.id.toKotlinUuid()
        PemasukanKustomTable.selectAll().where { PemasukanKustomTable.id eq id }.singleOrNull()?.let { row ->
            if (row[PemasukanKustomTable.idToko] != account.idToko ||
                row[PemasukanKustomTable.requestHash].trim() != requestHash
            ) {
                conflict("ID_REQUEST_BENTROKAN", "ID Pemasukan sudah digunakan untuk data berbeda.")
            }
            return@transaction gson.fromJson(row[PemasukanKustomTable.responseSnapshot], PemasukanCreateResponse::class.java)
                ?: error("Respons Pemasukan idempoten tidak tersedia.")
        }
        val saleExists = TransaksiPenjualanTable.selectAll().where {
            (TransaksiPenjualanTable.idToko eq account.idToko) and (TransaksiPenjualanTable.id eq id)
        }.singleOrNull() != null
        if (saleExists) conflict("ID_REQUEST_BENTROKAN", "ID Pemasukan sudah digunakan oleh Penjualan.")
        ensureFile(account.idToko, input.buktiFileId)
        val now = System.currentTimeMillis()
        val newBalance = try {
            Math.addExact(balance, input.nominal)
        } catch (_: ArithmeticException) {
            invalid("SALDO_TIDAK_VALID", "Perubahan saldo melebihi batas yang didukung.")
        }
        PemasukanKustomTable.insert {
            it[PemasukanKustomTable.id] = id
            it[idToko] = account.idToko
            it[judul] = input.judul
            it[nominal] = input.nominal
            it[tanggal] = input.tanggal
            it[catatan] = input.catatan
            it[buktiFileId] = input.buktiFileId?.toKotlinUuid()
            it[status] = "TERCATAT"
            it[alasanPembatalan] = null
            it[dibuatOleh] = account.id.toKotlinUuid()
            it[dibuatPada] = now
            it[dibatalkanOleh] = null
            it[dibatalkanPada] = null
            it[PemasukanKustomTable.requestHash] = requestHash
            it[responseSnapshot] = ""
        }
        TokoTable.update({ TokoTable.idToko eq account.idToko }) {
            it[saldoKumulatif] = newBalance
        }
        log(account, "PEMASUKAN_DICATAT", input.id, "PEMASUKAN", now)
        val response = PemasukanCreateResponse(incomeResponse(input, account, now), newBalance)
        PemasukanKustomTable.update({ PemasukanKustomTable.id eq id }) {
            it[PemasukanKustomTable.responseSnapshot] = gson.toJson(response)
        }
        response
    }

    override fun listIncomes(
        account: Akun,
        from: Long?,
        through: Long?,
        type: String?,
        status: String?,
        limit: Int,
        cursor: FinanceIncomeCursor?,
    ): FinanceIncomePage = transaction {
        val customCondition = incomeCustomCondition(account.idToko, from, through, status, cursor)
        val salesCondition = incomeSalesCondition(account.idToko, from, through, status, cursor)
        val customRows = if ((type == null || type == "KUSTOM") && (status == null || status in setOf("TERCATAT", "DIBATALKAN"))) {
            PemasukanKustomTable.selectAll().where { customCondition }
                .orderBy(PemasukanKustomTable.tanggal to SortOrder.DESC, PemasukanKustomTable.id to SortOrder.DESC)
                .limit(limit + 1)
                .toList()
        } else {
            emptyList()
        }
        val salesRows = if ((type == null || type == "PENJUALAN") && (status == null || status in setOf("SELESAI", "DIBATALKAN"))) {
            TransaksiPenjualanTable.selectAll().where { salesCondition }
                .orderBy(TransaksiPenjualanTable.dibuatPada to SortOrder.DESC, TransaksiPenjualanTable.id to SortOrder.DESC)
                .limit(limit + 1)
                .toList()
        } else {
            emptyList()
        }
        val customTotal = if (status == null || status == "TERCATAT") {
            sumIncomeCustom(account.idToko, from, through, type)
        } else {
            0L
        }
        val salesTotal = if (status == null || status == "SELESAI") {
            sumIncomeSales(account.idToko, from, through, type)
        } else {
            0L
        }
        val total = try {
            Math.addExact(customTotal, salesTotal)
        } catch (_: ArithmeticException) {
            invalid("TOTAL_TIDAK_VALID", "Total Pemasukan melebihi batas yang didukung.")
        }
        val entries = (customRows.map {
            IncomeEntry(it[PemasukanKustomTable.tanggal], "KUSTOM", it[PemasukanKustomTable.id].toJavaUuid(), it.toCustomIncomeResponse())
        } + salesRows.map {
            IncomeEntry(it[TransaksiPenjualanTable.dibuatPada], "PENJUALAN", it[TransaksiPenjualanTable.id].toJavaUuid(), it.toSaleIncomeResponse())
        })
            .sortedWith(compareByDescending<IncomeEntry> { it.tanggal }.thenBy { it.jenis }.thenByDescending { it.id })
        val hasMore = entries.size > limit
        val data = entries.take(limit)
        FinanceIncomePage(
            data = data.map { it.response },
            total = total,
            nextCursor = if (hasMore) data.lastOrNull()?.let { FinanceIncomeCursor(it.tanggal, it.jenis, it.id) } else null,
        )
    }

    override fun incomeDetail(storeId: String, id: UUID): PemasukanResponse? = transaction {
        PemasukanKustomTable.selectAll().where {
            (PemasukanKustomTable.idToko eq storeId) and (PemasukanKustomTable.id eq id.toKotlinUuid())
        }.singleOrNull()?.toCustomIncomeResponse()
            ?: TransaksiPenjualanTable.selectAll().where {
                (TransaksiPenjualanTable.idToko eq storeId) and (TransaksiPenjualanTable.id eq id.toKotlinUuid())
            }.singleOrNull()?.toSaleIncomeResponse()
    }

    override fun cancelIncome(account: Akun, id: UUID, reason: String): PemasukanCancelResponse = transaction {
        val balance = lockStore(account.idToko)
        val row = PemasukanKustomTable.selectAll().where {
            (PemasukanKustomTable.idToko eq account.idToko) and (PemasukanKustomTable.id eq id.toKotlinUuid())
        }.singleOrNull()
        if (row == null) {
            val saleExists = TransaksiPenjualanTable.selectAll().where {
                (TransaksiPenjualanTable.idToko eq account.idToko) and (TransaksiPenjualanTable.id eq id.toKotlinUuid())
            }.singleOrNull() != null
            if (saleExists) {
                conflict("PEMASUKAN_PENJUALAN_TIDAK_DAPAT_DIBATALKAN", "Batalkan Penjualan melalui endpoint transaksi.")
            }
            notFound("PEMASUKAN_TIDAK_DITEMUKAN", "Pemasukan tidak ditemukan.")
        }
        if (row[PemasukanKustomTable.status] != "TERCATAT") {
            conflict("PEMASUKAN_SUDAH_DIBATALKAN", "Pemasukan sudah dibatalkan.")
        }
        val newBalance = try {
            Math.subtractExact(balance, row[PemasukanKustomTable.nominal])
        } catch (_: ArithmeticException) {
            invalid("SALDO_TIDAK_VALID", "Perubahan saldo melebihi batas yang didukung.")
        }
        val now = System.currentTimeMillis()
        val updated = PemasukanKustomTable.update({
            (PemasukanKustomTable.idToko eq account.idToko) and
                (PemasukanKustomTable.id eq id.toKotlinUuid()) and
                (PemasukanKustomTable.status eq "TERCATAT")
        }) {
            it[status] = "DIBATALKAN"
            it[alasanPembatalan] = reason
            it[dibatalkanOleh] = account.id.toKotlinUuid()
            it[dibatalkanPada] = now
        }
        if (updated != 1) conflict("PEMASUKAN_SUDAH_DIBATALKAN", "Pemasukan sudah dibatalkan.")
        TokoTable.update({ TokoTable.idToko eq account.idToko }) {
            it[saldoKumulatif] = newBalance
        }
        log(account, "PEMASUKAN_DIBATALKAN", id, "PEMASUKAN", now)
        PemasukanCancelResponse(
            pemasukan = PemasukanKustomTable.selectAll().where {
                (PemasukanKustomTable.idToko eq account.idToko) and (PemasukanKustomTable.id eq id.toKotlinUuid())
            }.single().toCustomIncomeResponse(),
            saldoKumulatif = newBalance,
        )
    }

    override fun paymentMethod(storeId: String): MetodePembayaranResponse? = transaction {
        TokoTable.selectAll().where { TokoTable.idToko eq storeId }.singleOrNull()?.toPaymentMethod()
    }

    override fun updatePaymentMethod(account: Akun, request: MetodePembayaranRequest): MetodePembayaranResponse = transaction {
        request.qrisFileId?.let { ensureFile(account.idToko, it) }
        val updated = TokoTable.update({ TokoTable.idToko eq account.idToko }) {
            it[namaPenerima] = request.namaPenerima
            it[infoRekening] = request.infoRekening
            it[qrisFileId] = request.qrisFileId?.toKotlinUuid()
        }
        if (updated != 1) notFound("TOKO_TIDAK_DITEMUKAN", "Toko tidak ditemukan.")
        val now = System.currentTimeMillis()
        log(account, "METODE_PEMBAYARAN_DIUBAH", null, null, now)
        TokoTable.selectAll().where { TokoTable.idToko eq account.idToko }.single().toPaymentMethod()
    }

    private fun expenseCondition(
        storeId: String,
        from: Long?,
        through: Long?,
        type: String?,
        status: String?,
        cursor: FinanceExpenseCursor?,
    ): Op<Boolean> {
        var condition: Op<Boolean> = PengeluaranTable.idToko eq storeId
        if (from != null) condition = condition and (PengeluaranTable.tanggal greaterEq from)
        if (through != null) condition = condition and (PengeluaranTable.tanggal lessEq through)
        if (type != null) condition = condition and (PengeluaranTable.jenis eq type)
        if (status != null) condition = condition and (PengeluaranTable.status eq status)
        if (cursor != null) {
            condition = condition and (
                (PengeluaranTable.tanggal less cursor.tanggal) or
                    ((PengeluaranTable.tanggal eq cursor.tanggal) and (PengeluaranTable.id less cursor.id.toKotlinUuid()))
                )
        }
        return condition
    }

    private fun incomeCustomCondition(
        storeId: String,
        from: Long?,
        through: Long?,
        status: String?,
        cursor: FinanceIncomeCursor?,
    ): Op<Boolean> {
        var condition: Op<Boolean> = PemasukanKustomTable.idToko eq storeId
        if (from != null) condition = condition and (PemasukanKustomTable.tanggal greaterEq from)
        if (through != null) condition = condition and (PemasukanKustomTable.tanggal lessEq through)
        if (status != null) condition = condition and (PemasukanKustomTable.status eq status)
        if (cursor != null) condition = condition and incomeCursorCondition(
            PemasukanKustomTable.tanggal,
            PemasukanKustomTable.id,
            "KUSTOM",
            cursor,
        )
        return condition
    }

    private fun incomeSalesCondition(
        storeId: String,
        from: Long?,
        through: Long?,
        status: String?,
        cursor: FinanceIncomeCursor?,
    ): Op<Boolean> {
        var condition: Op<Boolean> = TransaksiPenjualanTable.idToko eq storeId
        if (from != null) condition = condition and (TransaksiPenjualanTable.dibuatPada greaterEq from)
        if (through != null) condition = condition and (TransaksiPenjualanTable.dibuatPada lessEq through)
        if (status != null) condition = condition and (TransaksiPenjualanTable.status eq status)
        if (cursor != null) condition = condition and incomeCursorCondition(
            TransaksiPenjualanTable.dibuatPada,
            TransaksiPenjualanTable.id,
            "PENJUALAN",
            cursor,
        )
        return condition
    }

    private fun incomeCursorCondition(
        dateColumn: org.jetbrains.exposed.v1.core.Column<Long>,
        idColumn: org.jetbrains.exposed.v1.core.Column<kotlin.uuid.Uuid>,
        type: String,
        cursor: FinanceIncomeCursor,
    ): Op<Boolean> = when {
        type < cursor.jenis -> dateColumn less cursor.tanggal
        type > cursor.jenis -> dateColumn lessEq cursor.tanggal
        else -> (dateColumn less cursor.tanggal) or
            ((dateColumn eq cursor.tanggal) and (idColumn less cursor.id.toKotlinUuid()))
    }

    private fun sumIncomeCustom(storeId: String, from: Long?, through: Long?, type: String?): Long {
        if (type != null && type != "KUSTOM") return 0
        var condition: Op<Boolean> = (PemasukanKustomTable.idToko eq storeId) and
            (PemasukanKustomTable.status eq "TERCATAT")
        if (from != null) condition = condition and (PemasukanKustomTable.tanggal greaterEq from)
        if (through != null) condition = condition and (PemasukanKustomTable.tanggal lessEq through)
        val expression = PemasukanKustomTable.nominal.sum()
        return PemasukanKustomTable.select(expression).where { condition }.singleOrNull()?.get(expression) ?: 0L
    }

    private fun sumIncomeSales(storeId: String, from: Long?, through: Long?, type: String?): Long {
        if (type != null && type != "PENJUALAN") return 0
        var condition: Op<Boolean> = (TransaksiPenjualanTable.idToko eq storeId) and
            (TransaksiPenjualanTable.status eq "SELESAI")
        if (from != null) condition = condition and (TransaksiPenjualanTable.dibuatPada greaterEq from)
        if (through != null) condition = condition and (TransaksiPenjualanTable.dibuatPada lessEq through)
        val expression = TransaksiPenjualanTable.total.sum()
        return TransaksiPenjualanTable.select(expression).where { condition }.singleOrNull()?.get(expression) ?: 0L
    }

    private fun ensureFile(storeId: String, fileId: UUID?) {
        if (fileId == null) return
        val exists = FileTable.selectAll().where {
            (FileTable.idToko eq storeId) and (FileTable.id eq fileId.toKotlinUuid())
        }.singleOrNull() != null
        if (!exists) notFound("FILE_TIDAK_DITEMUKAN", "File tidak ditemukan.")
    }

    private fun lockStore(storeId: String): Long {
        while (true) {
            val row = TokoTable.selectAll().where { TokoTable.idToko eq storeId }.singleOrNull()
                ?: notFound("TOKO_TIDAK_DITEMUKAN", "Toko tidak ditemukan.")
            val balance = row[TokoTable.saldoKumulatif]
            val locked = TokoTable.update({
                (TokoTable.idToko eq storeId) and (TokoTable.saldoKumulatif eq balance)
            }) {
                it[saldoKumulatif] = balance
            }
            if (locked == 1) return balance
        }
    }

    private fun log(account: Akun, action: String, referenceId: UUID?, referenceType: String?, at: Long) {
        LogAktivitasTable.insert {
            it[id] = UUID.randomUUID().toKotlinUuid()
            it[idToko] = account.idToko
            it[akunId] = account.id.toKotlinUuid()
            it[namaAkunSnapshot] = account.nama.take(80)
            it[kategori] = "KEUANGAN"
            it[aksi] = action
            it[LogAktivitasTable.referensiId] = referenceId?.toKotlinUuid()
            it[jenisReferensi] = referenceType
            it[dibuatPada] = at
        }
    }

    private fun expenseResponse(input: PengeluaranInput, account: Akun, now: Long) = PengeluaranResponse(
        id = input.id,
        jenis = "KUSTOM",
        jenisBiaya = input.jenisBiaya,
        judul = input.judul,
        barangId = null,
        supplierId = null,
        jumlah = null,
        hargaPerItem = null,
        nominal = input.nominal,
        tanggal = input.tanggal,
        catatan = input.catatan,
        buktiFileId = input.buktiFileId,
        status = "TERCATAT",
        alasanPembatalan = null,
        referensiId = null,
        jenisReferensi = null,
        dibuatOleh = account.id,
        dibuatPada = now,
        dibatalkanOleh = null,
        dibatalkanPada = null,
    )

    private fun incomeResponse(input: PemasukanInput, account: Akun, now: Long) = PemasukanResponse(
        id = input.id,
        jenis = "KUSTOM",
        judul = input.judul,
        nominal = input.nominal,
        tanggal = input.tanggal,
        status = "TERCATAT",
        alasanPembatalan = null,
        catatan = input.catatan,
        buktiFileId = input.buktiFileId,
        dibuatOleh = account.id,
        namaAkun = account.nama,
        dibuatPada = now,
    )

    private fun ResultRow.toExpenseResponse() = PengeluaranResponse(
        id = this[PengeluaranTable.id].toJavaUuid(),
        jenis = this[PengeluaranTable.jenis],
        jenisBiaya = this[PengeluaranTable.jenisBiaya],
        judul = this[PengeluaranTable.judul],
        barangId = this[PengeluaranTable.barangId]?.toJavaUuid(),
        supplierId = this[PengeluaranTable.supplierId]?.toJavaUuid(),
        jumlah = this[PengeluaranTable.jumlah],
        hargaPerItem = this[PengeluaranTable.hargaPerItem],
        nominal = this[PengeluaranTable.nominal],
        tanggal = this[PengeluaranTable.tanggal],
        catatan = this[PengeluaranTable.catatan],
        buktiFileId = this[PengeluaranTable.buktiFileId]?.toJavaUuid(),
        status = this[PengeluaranTable.status],
        alasanPembatalan = this[PengeluaranTable.alasanPembatalan],
        referensiId = this[PengeluaranTable.referensiId]?.toJavaUuid(),
        jenisReferensi = this[PengeluaranTable.jenisReferensi],
        dibuatOleh = this[PengeluaranTable.dibuatOleh]?.toJavaUuid(),
        dibuatPada = this[PengeluaranTable.dibuatPada],
        dibatalkanOleh = this[PengeluaranTable.dibatalkanOleh]?.toJavaUuid(),
        dibatalkanPada = this[PengeluaranTable.dibatalkanPada],
    )

    private fun ResultRow.toCustomIncomeResponse() = PemasukanResponse(
        id = this[PemasukanKustomTable.id].toJavaUuid(),
        jenis = "KUSTOM",
        judul = this[PemasukanKustomTable.judul],
        nominal = this[PemasukanKustomTable.nominal],
        tanggal = this[PemasukanKustomTable.tanggal],
        status = this[PemasukanKustomTable.status],
        alasanPembatalan = this[PemasukanKustomTable.alasanPembatalan],
        catatan = this[PemasukanKustomTable.catatan],
        buktiFileId = this[PemasukanKustomTable.buktiFileId]?.toJavaUuid(),
        dibuatOleh = this[PemasukanKustomTable.dibuatOleh]?.toJavaUuid(),
        namaAkun = null,
        dibuatPada = this[PemasukanKustomTable.dibuatPada],
    )

    private fun ResultRow.toSaleIncomeResponse(): PemasukanResponse = PemasukanResponse(
        id = this[TransaksiPenjualanTable.id].toJavaUuid(),
        jenis = "PENJUALAN",
        judul = this[TransaksiPenjualanTable.nomorTransaksi],
        nominal = this[TransaksiPenjualanTable.total],
        tanggal = this[TransaksiPenjualanTable.dibuatPada],
        status = this[TransaksiPenjualanTable.status],
        alasanPembatalan = this[TransaksiPenjualanTable.alasanPembatalan],
        catatan = this[TransaksiPenjualanTable.catatan],
        buktiFileId = null,
        dibuatOleh = this[TransaksiPenjualanTable.akunId]?.toJavaUuid(),
        namaAkun = this[TransaksiPenjualanTable.namaAkunSnapshot],
        dibuatPada = this[TransaksiPenjualanTable.dibuatPada],
        nomorTransaksi = this[TransaksiPenjualanTable.nomorTransaksi],
        metodePembayaran = this[TransaksiPenjualanTable.metodePembayaran],
    )

    private fun ResultRow.toPaymentMethod() = MetodePembayaranResponse(
        namaPenerima = this[TokoTable.namaPenerima],
        infoRekening = this[TokoTable.infoRekening],
        qrisFileId = this[TokoTable.qrisFileId]?.toJavaUuid(),
    )

    private data class IncomeEntry(
        val tanggal: Long,
        val jenis: String,
        val id: UUID,
        val response: PemasukanResponse,
    )

    private fun invalid(code: String, message: String): Nothing =
        throw ApiException(HttpStatusCode.BadRequest, code, message)

    private fun notFound(code: String, message: String): Nothing =
        throw ApiException(HttpStatusCode.NotFound, code, message)

    private fun conflict(code: String, message: String): Nothing =
        throw ApiException(HttpStatusCode.Conflict, code, message)
}
