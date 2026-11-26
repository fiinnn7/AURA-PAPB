package com.aura.server.sales

import com.aura.server.auth.Akun
import com.aura.server.core.StatusStokEvaluator
import com.aura.server.db.AkunTable
import com.aura.server.db.BarangTable
import com.aura.server.db.ItemTransaksiTable
import com.aura.server.db.LogAktivitasTable
import com.aura.server.db.NotifikasiTable
import com.aura.server.db.PergerakanStokTable
import com.aura.server.db.PengajuanPembatalanTable
import com.aura.server.db.TokoTable
import com.aura.server.db.TransaksiPenjualanTable
import com.aura.server.stock.NotifikasiStokResponse
import com.aura.server.stock.createStockNotification
import com.google.gson.Gson
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.UUID
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

internal class ExposedSalesRepository {
    private val gson = Gson()

    fun create(
        account: Akun,
        request: TransaksiRequest,
        requestHash: String,
    ): TransaksiBuatResponse = transaction {
        lockStore(account.idToko)
        val id = request.id ?: error("ID transaksi wajib diisi.")
        findCreated(account.idToko, id, requestHash)?.let { return@transaction it }
        val lines = request.barangDibeli.orEmpty()
        val now = System.currentTimeMillis()
        val day = Instant.ofEpochMilli(now).atZone(WIB).toLocalDate()
        val dateCode = day.format(DATE_CODE)
        val start = day.atStartOfDay(WIB).toInstant().toEpochMilli()
        val end = day.plusDays(1).atStartOfDay(WIB).toInstant().toEpochMilli()
        val number = TransaksiPenjualanTable.selectAll().where {
            (TransaksiPenjualanTable.idToko eq account.idToko) and
                ((TransaksiPenjualanTable.dibuatPada greater start) or (TransaksiPenjualanTable.dibuatPada eq start)) and
                (TransaksiPenjualanTable.dibuatPada less end)
        }.count().toInt() + 1
        if (number > 9999) invalid("NOMOR_TRANSAKSI_PENUH", "Nomor transaksi hari ini sudah mencapai batas.")
        val invoice = "INV-$dateCode-${number.toString().padStart(4, '0')}"

        val before = mutableListOf<SaleLine>()
        var total = 0L
        lines.sortedBy { it.barangId.toString() }.forEach { line ->
            val productId = line.barangId ?: invalid("BARANG_TIDAK_VALID", "Barang wajib dipilih.")
            val quantity = line.jumlah ?: invalid("JUMLAH_TIDAK_VALID", "Jumlah Barang wajib diisi.")
            if (quantity <= 0) invalid("JUMLAH_TIDAK_VALID", "Jumlah Barang harus lebih dari nol.")
            val row = BarangTable.selectAll().where {
                (BarangTable.idToko eq account.idToko) and (BarangTable.id eq productId.toKotlinUuid())
            }.singleOrNull() ?: notFound("BARANG_TIDAK_DITEMUKAN", "Barang tidak ditemukan.")
            if (row[BarangTable.status] != "AKTIF") conflict("BARANG_DIARSIPKAN", "Barang yang diarsipkan tidak dapat dijual.")
            val stock = row[BarangTable.stokSaatIni]
            if (quantity > stock) conflict("STOK_TIDAK_CUKUP", "Stok ${row[BarangTable.nama]} tidak mencukupi. Tersedia $stock.")
            val lineTotal = try {
                Math.multiplyExact(row[BarangTable.hargaJual], quantity.toLong())
            } catch (_: ArithmeticException) {
                invalid("TOTAL_TIDAK_VALID", "Total transaksi melebihi batas yang didukung.")
            }
            total = try {
                Math.addExact(total, lineTotal)
            } catch (_: ArithmeticException) {
                invalid("TOTAL_TIDAK_VALID", "Total transaksi melebihi batas yang didukung.")
            }
            before += SaleLine(row, quantity)
        }
        if (before.isEmpty()) invalid("BARANG_KOSONG", "Pilih minimal satu Barang.")
        if (before.map { it.row[BarangTable.id] }.distinct().size != before.size) {
            invalid("BARANG_DUPLIKAT", "Setiap Barang hanya boleh muncul satu kali.")
        }

        val method = request.metodePembayaran ?: invalid("METODE_TIDAK_VALID", "Metode pembayaran wajib dipilih.")
        val received: Long?
        val change: Long?
        when (method) {
            "TUNAI" -> {
                received = request.uangDiterima ?: invalid("UANG_DITERIMA_TIDAK_VALID", "Uang diterima wajib diisi.")
                if (received < total) invalid("UANG_KURANG", "Uang diterima kurang dari total tagihan.")
                change = received - total
            }
            "TRANSFER", "QRIS" -> {
                if (request.pembayaranDiterima != true) {
                    invalid("PEMBAYARAN_BELUM_DITERIMA", "Konfirmasi pembayaran sebelum menyelesaikan transaksi.")
                }
                received = null
                change = null
            }
            else -> invalid("METODE_TIDAK_VALID", "Metode pembayaran tidak valid.")
        }
        val normalizedNote = request.catatan?.trim()?.takeIf(String::isNotEmpty)
        if ((normalizedNote?.length ?: 0) > 1000) invalid("CATATAN_TIDAK_VALID", "Catatan maksimal 1000 karakter.")

        val transactionId = id
        val movementResponses = mutableListOf<PergerakanTransaksiResponse>()
        val itemResponses = mutableListOf<ItemTransaksiResponse>()
        val notifications = mutableListOf<NotifikasiStokResponse>()
        val storeRow = TokoTable.selectAll().where { TokoTable.idToko eq account.idToko }.single()
        var balance = storeRow[TokoTable.saldoKumulatif]

        TransaksiPenjualanTable.insert {
            it[TransaksiPenjualanTable.id] = transactionId.toKotlinUuid()
            it[idToko] = account.idToko
            it[nomorTransaksi] = invoice
            it[akunId] = account.id.toKotlinUuid()
            it[namaAkunSnapshot] = account.nama.take(80)
            it[metodePembayaran] = method
            it[TransaksiPenjualanTable.total] = total
            it[uangDiterima] = received
            it[kembalian] = change
            it[status] = "SELESAI"
            it[alasanPembatalan] = null
            it[catatan] = normalizedNote
            it[dibuatPada] = now
            it[TransaksiPenjualanTable.requestHash] = requestHash
            it[responseSnapshot] = ""
        }

        before.forEach { line ->
            val row = line.row
            val oldStock = row[BarangTable.stokSaatIni]
            val newStock = oldStock - line.quantity
            val updated = BarangTable.update({
                (BarangTable.idToko eq account.idToko) and
                    (BarangTable.id eq row[BarangTable.id]) and
                    (BarangTable.stokSaatIni eq oldStock)
            }) {
                it[stokSaatIni] = newStock
                it[diubahPada] = now
            }
            if (updated != 1) conflict("STOK_BERUBAH", "Stok ${row[BarangTable.nama]} berubah. Muat ulang sebelum menyelesaikan transaksi.")
            createStockNotification(
                account.idToko,
                row[BarangTable.id].toJavaUuid(),
                row[BarangTable.nama],
                newStock,
                StatusStokEvaluator.evaluate(oldStock, row[BarangTable.stokMinimum]),
                StatusStokEvaluator.evaluate(newStock, row[BarangTable.stokMinimum]),
                now,
            )?.let(notifications::add)
            val itemId = UUID.randomUUID()
            ItemTransaksiTable.insert {
                it[ItemTransaksiTable.id] = itemId.toKotlinUuid()
                it[idToko] = account.idToko
                it[transaksiId] = transactionId.toKotlinUuid()
                it[barangId] = row[BarangTable.id]
                it[namaBarangSnapshot] = row[BarangTable.nama]
                it[satuanSnapshot] = row[BarangTable.satuan]
                it[jumlah] = line.quantity
                it[hargaJualSnapshot] = row[BarangTable.hargaJual]
                it[hppSnapshot] = row[BarangTable.hppRataRata]
            }
            val movementId = UUID.randomUUID()
            PergerakanStokTable.insert {
                it[PergerakanStokTable.id] = movementId.toKotlinUuid()
                it[idToko] = account.idToko
                it[PergerakanStokTable.barangId] = row[BarangTable.id]
                it[akunId] = account.id.toKotlinUuid()
                it[jenis] = "KELUAR"
                it[sumber] = "PENJUALAN"
                it[jumlah] = line.quantity
                it[stokSebelum] = oldStock
                it[stokSesudah] = newStock
                it[referensiId] = transactionId.toKotlinUuid()
                it[jenisReferensi] = "TRANSAKSI"
                it[dibuatPada] = now
            }
            itemResponses += ItemTransaksiResponse(
                itemId, row[BarangTable.id].toJavaUuid(), row[BarangTable.nama],
                row[BarangTable.satuan], line.quantity, row[BarangTable.hargaJual], row[BarangTable.hppRataRata],
            )
            movementResponses += PergerakanTransaksiResponse(
                movementId, row[BarangTable.id].toJavaUuid(), "KELUAR", "PENJUALAN",
                line.quantity, oldStock, newStock, now, account.id, account.nama,
                id, "TRANSAKSI",
            )
        }
        balance = try {
            Math.addExact(balance, total)
        } catch (_: ArithmeticException) {
            invalid("SALDO_TIDAK_VALID", "Perubahan saldo melebihi batas yang didukung.")
        }
        TokoTable.update({ TokoTable.idToko eq account.idToko }) {
            it[saldoKumulatif] = balance
        }
        log(account, "TRANSAKSI_DICATAT", id, "TRANSAKSI", now)
        val result = TransaksiBuatResponse(
            transaksi = transactionResponse(transactionId, invoice, account.id, account.nama, method, total, received, change, "SELESAI", null, normalizedNote, now),
            rincianBarang = itemResponses,
            pergerakanStok = movementResponses,
            saldoKumulatif = balance,
            notifikasi = notifications.firstOrNull(),
        )
        TransaksiPenjualanTable.update({ TransaksiPenjualanTable.id eq transactionId.toKotlinUuid() }) {
            it[responseSnapshot] = gson.toJson(result)
        }
        result
    }

    fun list(account: Akun, hari: String?, status: String?, limit: Int, cursor: String?): TransaksiListResponse = transaction {
        val day = try {
            (hari?.let(java.time.LocalDate::parse) ?: Instant.now().atZone(WIB).toLocalDate())
        } catch (_: Exception) {
            invalid("HARI_TIDAK_VALID", "Tanggal riwayat tidak valid.")
        }
        val start = day.atStartOfDay(WIB).toInstant().toEpochMilli()
        val end = day.plusDays(1).atStartOfDay(WIB).toInstant().toEpochMilli()
        var condition: Op<Boolean> = (TransaksiPenjualanTable.idToko eq account.idToko) and
            ((TransaksiPenjualanTable.dibuatPada greater start) or (TransaksiPenjualanTable.dibuatPada eq start)) and
            (TransaksiPenjualanTable.dibuatPada less end)
        if (account.peran.name == "KASIR") {
            condition = condition and (TransaksiPenjualanTable.akunId eq account.id.toKotlinUuid())
        }
        if (status != null) condition = condition and (TransaksiPenjualanTable.status eq status)
        val position = cursor?.let(::decodeCursor)
        if (position != null) {
            condition = condition and (
                (TransaksiPenjualanTable.dibuatPada less position.first) or
                    ((TransaksiPenjualanTable.dibuatPada eq position.first) and (TransaksiPenjualanTable.id less position.second.toKotlinUuid()))
                )
        }
        val rows = TransaksiPenjualanTable.selectAll().where { condition }
            .orderBy(TransaksiPenjualanTable.dibuatPada to SortOrder.DESC, TransaksiPenjualanTable.id to SortOrder.DESC)
            .limit(limit + 1).toList()
        val data = rows.take(limit).map(::mapTransaction)
        val last = rows.take(limit).lastOrNull()
        TransaksiListResponse(
            data,
            if (rows.size > limit && last != null) encodeCursor(last[TransaksiPenjualanTable.dibuatPada], last[TransaksiPenjualanTable.id].toJavaUuid()) else null,
        )
    }

    fun detail(account: Akun, id: UUID): TransaksiDetailResponse = transaction {
        val row = findTransaction(account.idToko, id) ?: notFound("TRANSAKSI_TIDAK_DITEMUKAN", "Transaksi tidak ditemukan.")
        if (account.peran.name == "KASIR" && row[TransaksiPenjualanTable.akunId] != account.id.toKotlinUuid()) {
            notFound("TRANSAKSI_TIDAK_DITEMUKAN", "Transaksi tidak ditemukan.")
        }
        TransaksiDetailResponse(mapTransaction(row), listItems(account.idToko, id))
    }

    fun cancel(account: Akun, id: UUID, reason: String): TransaksiBatalResponse = transaction {
        lockStore(account.idToko)
        val row = findTransaction(account.idToko, id) ?: notFound("TRANSAKSI_TIDAK_DITEMUKAN", "Transaksi tidak ditemukan.")
        if (row[TransaksiPenjualanTable.status] != "SELESAI") conflict("TRANSAKSI_SUDAH_DIBATALKAN", "Transaksi sudah dibatalkan.")
        val now = System.currentTimeMillis()
        val movements = mutableListOf<PergerakanTransaksiResponse>()
        listItems(account.idToko, id).forEach { item ->
            val productId = item.barangId.toKotlinUuid()
            val product = BarangTable.selectAll().where {
                (BarangTable.idToko eq account.idToko) and (BarangTable.id eq productId)
            }.singleOrNull() ?: notFound("BARANG_TIDAK_DITEMUKAN", "Barang transaksi tidak ditemukan.")
            if (item.jumlah > Int.MAX_VALUE - product[BarangTable.stokSaatIni]) {
                invalid("STOK_TIDAK_VALID", "Stok hasil pembatalan melebihi batas yang didukung.")
            }
            val before = product[BarangTable.stokSaatIni]
            val after = before + item.jumlah
            BarangTable.update({ (BarangTable.idToko eq account.idToko) and (BarangTable.id eq productId) }) {
                it[stokSaatIni] = after
                it[diubahPada] = now
            }
            val movementId = UUID.randomUUID()
            PergerakanStokTable.insert {
                it[PergerakanStokTable.id] = movementId.toKotlinUuid()
                it[idToko] = account.idToko
                it[PergerakanStokTable.barangId] = productId
                it[akunId] = account.id.toKotlinUuid()
                it[jenis] = "MASUK"
                it[sumber] = "PEMBATALAN_PENJUALAN"
                it[jumlah] = item.jumlah
                it[stokSebelum] = before
                it[stokSesudah] = after
                it[referensiId] = id.toKotlinUuid()
                it[jenisReferensi] = "TRANSAKSI"
                it[dibuatPada] = now
            }
            movements += PergerakanTransaksiResponse(
                movementId, item.barangId, "MASUK", "PEMBATALAN_PENJUALAN",
                item.jumlah, before, after, now, account.id, account.nama, id, "TRANSAKSI",
            )
        }
        val total = row[TransaksiPenjualanTable.total]
        val balance = try {
            Math.subtractExact(TokoTable.selectAll().where { TokoTable.idToko eq account.idToko }.single()[TokoTable.saldoKumulatif], total)
        } catch (_: ArithmeticException) {
            invalid("SALDO_TIDAK_VALID", "Perubahan saldo melebihi batas yang didukung.")
        }
        TokoTable.update({ TokoTable.idToko eq account.idToko }) { it[saldoKumulatif] = balance }
        TransaksiPenjualanTable.update({ TransaksiPenjualanTable.id eq id.toKotlinUuid() }) {
            it[status] = "DIBATALKAN"
            it[alasanPembatalan] = reason
        }
        log(account, "TRANSAKSI_DIBATALKAN", id, "TRANSAKSI", now)
        TransaksiBatalResponse(mapTransaction(findTransaction(account.idToko, id)!!), movements, balance)
    }

    fun requestCancellation(
        account: Akun,
        transactionId: UUID,
        request: PengajuanPembatalanRequest,
        hash: String,
    ): PengajuanPembatalanResponse = transaction {
        lockStore(account.idToko)
        val id = request.id ?: invalid("ID_REQUEST_TIDAK_VALID", "ID pengajuan wajib diisi.")
        val previous = PengajuanPembatalanTable.selectAll().where { PengajuanPembatalanTable.id eq id.toKotlinUuid() }.singleOrNull()
        if (previous != null) {
            if (previous[PengajuanPembatalanTable.idToko] != account.idToko ||
                previous[PengajuanPembatalanTable.requestHash].trim() != hash
            ) conflict("ID_REQUEST_BENTROKAN", "ID pengajuan sudah digunakan untuk data berbeda.")
            return@transaction gson.fromJson(previous[PengajuanPembatalanTable.responseSnapshot], PengajuanPembatalanResponse::class.java)
        }
        val reason = request.alasan ?: invalid("ALASAN_TIDAK_VALID", "Alasan pembatalan wajib diisi.")
        val target = findTransaction(account.idToko, transactionId) ?: notFound("TRANSAKSI_TIDAK_DITEMUKAN", "Transaksi tidak ditemukan.")
        if (target[TransaksiPenjualanTable.akunId] != account.id.toKotlinUuid()) {
            notFound("TRANSAKSI_TIDAK_DITEMUKAN", "Transaksi tidak ditemukan.")
        }
        if (target[TransaksiPenjualanTable.status] != "SELESAI") conflict("TRANSAKSI_SUDAH_DIBATALKAN", "Transaksi sudah dibatalkan.")
        val now = System.currentTimeMillis()
        val notification = PengajuanPembatalanResponse(
            id, "PENGAJUAN_PEMBATALAN", "Pengajuan pembatalan transaksi",
            "${target[TransaksiPenjualanTable.nomorTransaksi]}: $reason",
            transactionId, "TRANSAKSI", now, null,
        )
        PengajuanPembatalanTable.insert {
            it[PengajuanPembatalanTable.id] = id.toKotlinUuid()
            it[idToko] = account.idToko
            it[PengajuanPembatalanTable.transaksiId] = transactionId.toKotlinUuid()
            it[akunId] = account.id.toKotlinUuid()
            it[alasan] = reason
            it[dibuatPada] = now
            it[requestHash] = hash
            it[responseSnapshot] = gson.toJson(notification)
        }
        NotifikasiTable.insert {
            it[NotifikasiTable.id] = id.toKotlinUuid()
            it[idToko] = account.idToko
            it[jenis] = notification.jenis
            it[judul] = notification.judul
            it[pesan] = notification.pesan
            it[referensiId] = transactionId.toKotlinUuid()
            it[jenisReferensi] = "TRANSAKSI"
            it[dibuatPada] = now
            it[dibacaPada] = null
        }
        log(account, "PEMBATALAN_DIAJUKAN", transactionId, "TRANSAKSI", now)
        notification
    }

    private fun findCreated(storeId: String, id: UUID, hash: String): TransaksiBuatResponse? {
        val row = TransaksiPenjualanTable.selectAll().where { TransaksiPenjualanTable.id eq id.toKotlinUuid() }.singleOrNull()
            ?: return null
        if (row[TransaksiPenjualanTable.idToko] != storeId || row[TransaksiPenjualanTable.requestHash].trim() != hash) {
            conflict("ID_REQUEST_BENTROKAN", "ID transaksi sudah digunakan untuk data berbeda.")
        }
        return gson.fromJson(row[TransaksiPenjualanTable.responseSnapshot], TransaksiBuatResponse::class.java)
    }

    private fun findTransaction(storeId: String, id: UUID): ResultRow? =
        TransaksiPenjualanTable.selectAll().where {
            (TransaksiPenjualanTable.idToko eq storeId) and (TransaksiPenjualanTable.id eq id.toKotlinUuid())
        }.singleOrNull()

    private fun listItems(storeId: String, transactionId: UUID): List<ItemTransaksiResponse> =
        ItemTransaksiTable.selectAll().where {
            (ItemTransaksiTable.idToko eq storeId) and (ItemTransaksiTable.transaksiId eq transactionId.toKotlinUuid())
        }.map { row ->
            ItemTransaksiResponse(
                row[ItemTransaksiTable.id].toJavaUuid(),
                row[ItemTransaksiTable.barangId].toJavaUuid(),
                row[ItemTransaksiTable.namaBarangSnapshot],
                row[ItemTransaksiTable.satuanSnapshot],
                row[ItemTransaksiTable.jumlah],
                row[ItemTransaksiTable.hargaJualSnapshot],
                row[ItemTransaksiTable.hppSnapshot],
            )
        }

    private fun mapTransaction(row: ResultRow) = transactionResponse(
        row[TransaksiPenjualanTable.id].toJavaUuid(),
        row[TransaksiPenjualanTable.nomorTransaksi],
        row[TransaksiPenjualanTable.akunId]?.toJavaUuid(),
        row[TransaksiPenjualanTable.namaAkunSnapshot],
        row[TransaksiPenjualanTable.metodePembayaran],
        row[TransaksiPenjualanTable.total],
        row[TransaksiPenjualanTable.uangDiterima],
        row[TransaksiPenjualanTable.kembalian],
        row[TransaksiPenjualanTable.status],
        row[TransaksiPenjualanTable.alasanPembatalan],
        row[TransaksiPenjualanTable.catatan],
        row[TransaksiPenjualanTable.dibuatPada],
    )

    private fun lockStore(storeId: String) {
        while (true) {
            val row = TokoTable.selectAll().where { TokoTable.idToko eq storeId }.singleOrNull()
                ?: notFound("TOKO_TIDAK_DITEMUKAN", "Toko tidak ditemukan.")
            val balance = row[TokoTable.saldoKumulatif]
            val locked = TokoTable.update({
                (TokoTable.idToko eq storeId) and (TokoTable.saldoKumulatif eq balance)
            }) { it[saldoKumulatif] = balance }
            if (locked == 1) return
        }
    }

    private fun log(account: Akun, action: String, referenceId: UUID, referenceType: String, at: Long) {
        LogAktivitasTable.insert {
            it[id] = UUID.randomUUID().toKotlinUuid()
            it[idToko] = account.idToko
            it[akunId] = account.id.toKotlinUuid()
            it[namaAkunSnapshot] = account.nama.take(80)
            it[kategori] = "PENJUALAN"
            it[aksi] = action
            it[LogAktivitasTable.referensiId] = referenceId.toKotlinUuid()
            it[jenisReferensi] = referenceType
            it[dibuatPada] = at
        }
    }

    private fun encodeCursor(timestamp: Long, id: UUID): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString("$timestamp:$id".toByteArray())

    private fun decodeCursor(cursor: String): Pair<Long, UUID> {
        val parts = runCatching { String(Base64.getUrlDecoder().decode(cursor)).split(':', limit = 2) }.getOrNull()
        val time = parts?.getOrNull(0)?.toLongOrNull() ?: invalid("CURSOR_TIDAK_VALID", "Cursor riwayat tidak valid.")
        val id = parts.getOrNull(1)?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            ?: invalid("CURSOR_TIDAK_VALID", "Cursor riwayat tidak valid.")
        return time to id
    }

    private data class SaleLine(val row: ResultRow, val quantity: Int)

    companion object {
        private val WIB = ZoneId.of("Asia/Jakarta")
        private val DATE_CODE = DateTimeFormatter.ofPattern("yyMMdd")
        private fun invalid(code: String, message: String): Nothing =
            throw com.aura.server.plugins.ApiException(io.ktor.http.HttpStatusCode.BadRequest, code, message)
        private fun conflict(code: String, message: String): Nothing =
            throw com.aura.server.plugins.ApiException(io.ktor.http.HttpStatusCode.Conflict, code, message)
        private fun notFound(code: String, message: String): Nothing =
            throw com.aura.server.plugins.ApiException(io.ktor.http.HttpStatusCode.NotFound, code, message)

        private fun transactionResponse(
            id: UUID,
            invoice: String,
            accountId: UUID?,
            accountName: String,
            method: String,
            total: Long,
            received: Long?,
            change: Long?,
            status: String,
            reason: String?,
            note: String?,
            createdAt: Long,
        ) = TransaksiResponse(id, invoice, accountId, accountName, method, total, received, change, status, reason, note, createdAt)
    }
}
