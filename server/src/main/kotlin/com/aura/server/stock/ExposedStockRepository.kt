package com.aura.server.stock

import com.aura.server.auth.Akun
import com.aura.server.catalog.BarangResponse
import com.aura.server.catalog.KategoriResponse
import com.aura.server.catalog.PergerakanStokResponse
import com.aura.server.db.BarangKategoriTable
import com.aura.server.db.BarangTable
import com.aura.server.db.AkunTable
import com.aura.server.db.FileTable
import com.aura.server.db.HargaBeliSupplierTable
import com.aura.server.db.KategoriTable
import com.aura.server.db.LogAktivitasTable
import com.aura.server.db.PenyesuaianStokTable
import com.aura.server.db.PergerakanStokTable
import com.aura.server.db.PengeluaranTable
import com.aura.server.db.RestockTable
import com.aura.server.db.SupplierTable
import com.aura.server.db.TokoTable
import com.aura.server.core.HppCalculator
import com.aura.server.core.StatusStokEvaluator
import com.google.gson.Gson
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.util.UUID
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

internal class ExposedStockRepository : StockRepository {
    override fun createRestock(
        account: Akun,
        input: RestockInput,
        requestHash: String,
    ): RestockCreateResponse = transaction {
        val startingBalance = lockStore(account.idToko)
        findRestockResponse(account.idToko, input.id, requestHash)?.let { return@transaction it }

        val barangId = input.barangId.toKotlinUuid()
        val barang = BarangTable.selectAll().where {
            (BarangTable.idToko eq account.idToko) and (BarangTable.id eq barangId)
        }.singleOrNull() ?: notFound("BARANG_TIDAK_DITEMUKAN", "Barang tidak ditemukan.")
        findRestockResponse(account.idToko, input.id, requestHash)?.let { return@transaction it }
        if (barang[BarangTable.status] != "AKTIF") conflict("BARANG_DIARSIPKAN", "Barang yang diarsipkan tidak dapat di-Restock.")

        val supplierId = input.supplierId.toKotlinUuid()
        val supplier = SupplierTable.selectAll().where {
            (SupplierTable.idToko eq account.idToko) and (SupplierTable.id eq supplierId)
        }.singleOrNull() ?: notFound("SUPPLIER_TIDAK_DITEMUKAN", "Supplier tidak ditemukan.")
        if (supplier[SupplierTable.status] != "AKTIF") conflict("SUPPLIER_NONAKTIF", "Supplier tidak aktif.")
        input.notaFileId?.let { ensureFile(account.idToko, it) }

        val oldStock = barang[BarangTable.stokSaatIni]
        if (input.jumlahMasuk > Int.MAX_VALUE - oldStock) {
            invalid("STOK_TIDAK_VALID", "Jumlah Restock melebihi batas stok.")
        }
        val unitPrice = try {
            HppCalculator.hargaPerItem(input.totalDibayar, input.jumlahMasuk)
        } catch (_: ArithmeticException) {
            invalid("HARGA_PER_ITEM_TIDAK_VALID", "Harga per item melebihi batas yang didukung.")
        }
        val newHpp = try {
            HppCalculator.hppBaru(oldStock, barang[BarangTable.hppRataRata], input.jumlahMasuk, unitPrice)
        } catch (_: ArithmeticException) {
            invalid("HPP_TIDAK_VALID", "HPP melebihi batas yang didukung.")
        }
        val newStock = oldStock + input.jumlahMasuk
        val previousStatus = StatusStokEvaluator.evaluate(oldStock, barang[BarangTable.stokMinimum])
        val newStatus = StatusStokEvaluator.evaluate(newStock, barang[BarangTable.stokMinimum])
        val now = System.currentTimeMillis()
        val restockId = input.id.toKotlinUuid()
        val expenseId = UUID.randomUUID().toKotlinUuid()
        val movementId = UUID.randomUUID().toKotlinUuid()

        val updated = BarangTable.update({
            (BarangTable.idToko eq account.idToko) and
                (BarangTable.id eq barangId) and
                (BarangTable.stokSaatIni eq oldStock)
        }) {
            it[stokSaatIni] = newStock
            it[hppRataRata] = newHpp
            it[diubahPada] = now
        }
        if (updated != 1) {
            findRestockResponse(account.idToko, input.id, requestHash)?.let { return@transaction it }
            conflict("STOK_BERUBAH", "Stok Barang berubah. Muat ulang sebelum mencatat Restock.")
        }
        RestockTable.insert {
            it[id] = restockId
            it[idToko] = account.idToko
            it[RestockTable.barangId] = barangId
            it[RestockTable.supplierId] = supplierId
            it[jumlahMasuk] = input.jumlahMasuk
            it[totalDibayar] = input.totalDibayar
            it[hargaPerItem] = unitPrice
            it[tanggal] = input.tanggal
            it[notaFileId] = input.notaFileId?.toKotlinUuid()
            it[catatan] = input.catatan
            it[akunId] = account.id.toKotlinUuid()
            it[dibuatPada] = now
            it[RestockTable.requestHash] = requestHash
            it[responseSnapshot] = ""
        }
        PengeluaranTable.insert {
            it[id] = expenseId
            it[idToko] = account.idToko
            it[jenis] = "PEMBELIAN"
            it[jenisBiaya] = null
            it[judul] = "Restock ${barang[BarangTable.nama]}".take(120)
            it[PengeluaranTable.barangId] = barangId
            it[PengeluaranTable.supplierId] = supplierId
            it[jumlah] = input.jumlahMasuk
            it[hargaPerItem] = unitPrice
            it[nominal] = input.totalDibayar
            it[tanggal] = input.tanggal
            it[catatan] = input.catatan
            it[buktiFileId] = input.notaFileId?.toKotlinUuid()
            it[status] = "TERCATAT"
            it[alasanPembatalan] = null
            it[referensiId] = restockId
            it[jenisReferensi] = "RESTOCK"
            it[dibuatOleh] = account.id.toKotlinUuid()
            it[dibuatPada] = now
            it[dibatalkanOleh] = null
            it[dibatalkanPada] = null
        }
        val existingSupplierPrice = HargaBeliSupplierTable.selectAll().where {
            (HargaBeliSupplierTable.idToko eq account.idToko) and
                (HargaBeliSupplierTable.barangId eq barangId) and
                (HargaBeliSupplierTable.supplierId eq supplierId)
        }.singleOrNull()
        if (existingSupplierPrice == null) {
            HargaBeliSupplierTable.insert {
                it[idToko] = account.idToko
                it[HargaBeliSupplierTable.barangId] = barangId
                it[HargaBeliSupplierTable.supplierId] = supplierId
                it[hargaPerItem] = unitPrice
                it[diperbaruiPada] = now
            }
        } else {
            HargaBeliSupplierTable.update({
                (HargaBeliSupplierTable.idToko eq account.idToko) and
                    (HargaBeliSupplierTable.barangId eq barangId) and
                    (HargaBeliSupplierTable.supplierId eq supplierId)
            }) {
                it[hargaPerItem] = unitPrice
                it[diperbaruiPada] = now
            }
        }
        PergerakanStokTable.insert {
            it[id] = movementId
            it[idToko] = account.idToko
            it[PergerakanStokTable.barangId] = barangId
            it[akunId] = account.id.toKotlinUuid()
            it[jenis] = "MASUK"
            it[sumber] = "RESTOCK"
            it[jumlah] = input.jumlahMasuk
            it[stokSebelum] = oldStock
            it[stokSesudah] = newStock
            it[referensiId] = restockId
            it[jenisReferensi] = "RESTOCK"
            it[dibuatPada] = now
        }
        val newBalance = try {
            Math.subtractExact(startingBalance, input.totalDibayar)
        } catch (_: ArithmeticException) {
            invalid("SALDO_TIDAK_VALID", "Perubahan saldo melebihi batas yang didukung.")
        }
        TokoTable.update({ TokoTable.idToko eq account.idToko }) {
            it[saldoKumulatif] = newBalance
        }
        val notification = createStockNotification(
            account.idToko,
            input.barangId,
            barang[BarangTable.nama],
            newStock,
            previousStatus,
            newStatus,
            now,
        )
        log(account, "RESTOCK_DICATAT", input.id, "RESTOCK", now)

        val result = RestockCreateResponse(
            restock = RestockRecordResponse(
                id = input.id,
                barangId = input.barangId,
                supplierId = input.supplierId,
                jumlahMasuk = input.jumlahMasuk,
                totalDibayar = input.totalDibayar,
                hargaPerItem = unitPrice,
                tanggal = input.tanggal,
                notaFileId = input.notaFileId,
                catatan = input.catatan,
                akunId = account.id,
                dibuatPada = now,
            ),
            barang = barangResponse(account.idToko, barangId),
            pergerakan = PergerakanStokResponse(
                id = movementId.toJavaUuid(),
                jenis = "MASUK",
                sumber = "RESTOCK",
                jumlah = input.jumlahMasuk,
                stokSebelum = oldStock,
                stokSesudah = newStock,
                dibuatPada = now,
                akunId = account.id,
                namaAkun = account.nama,
                referensiId = input.id,
                jenisReferensi = "RESTOCK",
            ),
            pengeluaranPembelian = PengeluaranPembelianResponse(
                id = expenseId.toJavaUuid(),
                jenis = "PEMBELIAN",
                jenisBiaya = null,
                judul = "Restock ${barang[BarangTable.nama]}".take(120),
                barangId = input.barangId,
                supplierId = input.supplierId,
                jumlah = input.jumlahMasuk,
                hargaPerItem = unitPrice,
                nominal = input.totalDibayar,
                tanggal = input.tanggal,
                catatan = input.catatan,
                buktiFileId = input.notaFileId,
                status = "TERCATAT",
                referensiId = input.id,
                jenisReferensi = "RESTOCK",
                dibuatOleh = account.id,
                dibuatPada = now,
            ),
            saldoKumulatif = newBalance,
            notifikasi = notification,
        )
        RestockTable.update({ RestockTable.id eq restockId }) {
            it[responseSnapshot] = Gson().toJson(result)
        }
        result
    }

    override fun createAdjustment(
        account: Akun,
        input: PenyesuaianStokInput,
        requestHash: String,
    ): PenyesuaianStokCreateResponse = transaction {
        lockStore(account.idToko)
        findAdjustmentResponse(account.idToko, input.id, requestHash)?.let { return@transaction it }

        val barangId = input.barangId.toKotlinUuid()
        val barang = BarangTable.selectAll().where {
            (BarangTable.idToko eq account.idToko) and (BarangTable.id eq barangId)
        }.singleOrNull() ?: notFound("BARANG_TIDAK_DITEMUKAN", "Barang tidak ditemukan.")
        findAdjustmentResponse(account.idToko, input.id, requestHash)?.let { return@transaction it }
        if (barang[BarangTable.status] != "AKTIF") conflict("BARANG_DIARSIPKAN", "Barang yang diarsipkan tidak dapat disesuaikan.")
        input.buktiFileId?.let { ensureFile(account.idToko, it) }

        val oldStock = barang[BarangTable.stokSaatIni]
        val difference = input.stokFisik - oldStock
        val quantity = kotlin.math.abs(difference)
        val lossValue = if (difference < 0) {
            try {
                Math.multiplyExact(quantity.toLong(), barang[BarangTable.hppRataRata])
            } catch (_: ArithmeticException) {
                invalid("NILAI_KERUGIAN_TIDAK_VALID", "Nilai kerugian melebihi batas yang didukung.")
            }
        } else {
            0L
        }
        val previousStatus = StatusStokEvaluator.evaluate(oldStock, barang[BarangTable.stokMinimum])
        val newStatus = StatusStokEvaluator.evaluate(input.stokFisik, barang[BarangTable.stokMinimum])
        val now = System.currentTimeMillis()
        val adjustmentId = input.id.toKotlinUuid()
        val movementId = UUID.randomUUID().toKotlinUuid()
        val updated = BarangTable.update({
            (BarangTable.idToko eq account.idToko) and
                (BarangTable.id eq barangId) and
                (BarangTable.stokSaatIni eq oldStock)
        }) {
            it[stokSaatIni] = input.stokFisik
            it[diubahPada] = now
        }
        if (updated != 1) {
            findAdjustmentResponse(account.idToko, input.id, requestHash)?.let { return@transaction it }
            conflict("STOK_BERUBAH", "Stok Barang berubah. Muat ulang sebelum mencatat Penyesuaian.")
        }
        PenyesuaianStokTable.insert {
            it[id] = adjustmentId
            it[idToko] = account.idToko
            it[PenyesuaianStokTable.barangId] = barangId
            it[stokFisik] = input.stokFisik
            it[selisih] = difference
            it[alasan] = input.alasan
            it[nilaiKerugian] = lossValue
            it[catatan] = input.catatan
            it[buktiFileId] = input.buktiFileId?.toKotlinUuid()
            it[tanggal] = input.tanggal
            it[akunId] = account.id.toKotlinUuid()
            it[dibuatPada] = now
            it[PenyesuaianStokTable.requestHash] = requestHash
            it[responseSnapshot] = ""
        }
        PergerakanStokTable.insert {
            it[id] = movementId
            it[idToko] = account.idToko
            it[PergerakanStokTable.barangId] = barangId
            it[akunId] = account.id.toKotlinUuid()
            it[jenis] = "PENYESUAIAN"
            it[sumber] = "STOCK_OPNAME"
            it[jumlah] = quantity
            it[stokSebelum] = oldStock
            it[stokSesudah] = input.stokFisik
            it[referensiId] = adjustmentId
            it[jenisReferensi] = "PENYESUAIAN_STOK"
            it[dibuatPada] = now
        }
        val notification = createStockNotification(
            account.idToko,
            input.barangId,
            barang[BarangTable.nama],
            input.stokFisik,
            previousStatus,
            newStatus,
            now,
        )
        log(account, "PENYESUAIAN_STOK_DICATAT", input.id, "PENYESUAIAN_STOK", now)

        val result = PenyesuaianStokCreateResponse(
            penyesuaian = PenyesuaianStokRecordResponse(
                id = input.id,
                barangId = input.barangId,
                stokFisik = input.stokFisik,
                selisih = difference,
                alasan = input.alasan,
                nilaiKerugian = lossValue,
                catatan = input.catatan,
                buktiFileId = input.buktiFileId,
                tanggal = input.tanggal,
                akunId = account.id,
                dibuatPada = now,
            ),
            barang = barangResponse(account.idToko, barangId),
            pergerakan = PergerakanStokResponse(
                id = movementId.toJavaUuid(),
                jenis = "PENYESUAIAN",
                sumber = "STOCK_OPNAME",
                jumlah = quantity,
                stokSebelum = oldStock,
                stokSesudah = input.stokFisik,
                dibuatPada = now,
                akunId = account.id,
                namaAkun = account.nama,
                referensiId = input.id,
                jenisReferensi = "PENYESUAIAN_STOK",
                alasan = input.alasan,
            ),
            notifikasi = notification,
        )
        PenyesuaianStokTable.update({ PenyesuaianStokTable.id eq adjustmentId }) {
            it[responseSnapshot] = Gson().toJson(result)
        }
        result
    }

    override fun listMovements(
        storeId: String,
        from: Long?,
        through: Long?,
        type: String?,
        barangId: UUID?,
        limit: Int,
        cursor: PergerakanStokCursor?,
    ): PergerakanStokPage = transaction {
        var condition: Op<Boolean> = PergerakanStokTable.idToko eq storeId
        if (from != null) {
            condition = condition and (
                (PergerakanStokTable.dibuatPada greater from) or (PergerakanStokTable.dibuatPada eq from)
                )
        }
        if (through != null) {
            condition = condition and (
                (PergerakanStokTable.dibuatPada less through) or (PergerakanStokTable.dibuatPada eq through)
                )
        }
        if (type != null) condition = condition and (PergerakanStokTable.jenis eq type)
        if (barangId != null) condition = condition and (PergerakanStokTable.barangId eq barangId.toKotlinUuid())
        if (cursor != null) {
            val cursorId = cursor.id.toKotlinUuid()
            condition = condition and (
                (PergerakanStokTable.dibuatPada less cursor.dibuatPada) or
                    ((PergerakanStokTable.dibuatPada eq cursor.dibuatPada) and (PergerakanStokTable.id less cursorId))
                )
        }
        val rows = PergerakanStokTable.selectAll().where { condition }
            .orderBy(PergerakanStokTable.dibuatPada to SortOrder.DESC, PergerakanStokTable.id to SortOrder.DESC)
            .limit(limit + 1)
            .toList()
        val pageRows = rows.take(limit)
        val data = pageRows.map { row ->
            val item = BarangTable.selectAll().where {
                (BarangTable.idToko eq storeId) and (BarangTable.id eq row[PergerakanStokTable.barangId])
            }.singleOrNull() ?: notFound("BARANG_TIDAK_DITEMUKAN", "Barang pergerakan tidak ditemukan.")
            PergerakanStokItemResponse(
                id = row[PergerakanStokTable.id].toJavaUuid(),
                barangId = row[PergerakanStokTable.barangId].toJavaUuid(),
                namaBarang = item[BarangTable.nama],
                jenis = row[PergerakanStokTable.jenis],
                sumber = row[PergerakanStokTable.sumber],
                jumlah = row[PergerakanStokTable.jumlah],
                stokSebelum = row[PergerakanStokTable.stokSebelum],
                stokSesudah = row[PergerakanStokTable.stokSesudah],
                dibuatPada = row[PergerakanStokTable.dibuatPada],
                akunId = row[PergerakanStokTable.akunId]?.toJavaUuid(),
                namaAkun = row[PergerakanStokTable.akunId]?.let { accountId ->
                    AkunTable.selectAll().where { AkunTable.id eq accountId }.singleOrNull()?.get(AkunTable.nama)
                },
                referensiId = row[PergerakanStokTable.referensiId]?.toJavaUuid(),
                jenisReferensi = row[PergerakanStokTable.jenisReferensi],
                alasan = if (row[PergerakanStokTable.jenisReferensi] == "PENYESUAIAN_STOK") {
                    row[PergerakanStokTable.referensiId]?.let { referenceId ->
                        PenyesuaianStokTable.selectAll().where {
                            (PenyesuaianStokTable.idToko eq storeId) and
                                (PenyesuaianStokTable.id eq referenceId)
                        }.singleOrNull()?.get(PenyesuaianStokTable.alasan)
                    }
                } else {
                    null
                },
            )
        }
        val last = pageRows.lastOrNull()
        PergerakanStokPage(
            data = data,
            nextCursor = if (rows.size > limit && last != null) {
                PergerakanStokCursor(
                    last[PergerakanStokTable.dibuatPada],
                    last[PergerakanStokTable.id].toJavaUuid(),
                )
            } else {
                null
            },
        )
    }

    private fun findRestockResponse(storeId: String, id: UUID, requestHash: String): RestockCreateResponse? {
        val row = RestockTable.selectAll().where { RestockTable.id eq id.toKotlinUuid() }.singleOrNull()
            ?: return null
        if (row[RestockTable.idToko] != storeId) conflict("ID_RESTOCK_SUDAH_DIPAKAI", "ID Restock sudah digunakan.")
        if (row[RestockTable.requestHash].trim() != requestHash) {
            conflict("ID_RESTOCK_SUDAH_DIPAKAI", "ID Restock sudah digunakan untuk data berbeda.")
        }
        return Gson().fromJson(row[RestockTable.responseSnapshot], RestockCreateResponse::class.java)
            ?: error("Respons Restock idempoten tidak tersedia.")
    }

    private fun findAdjustmentResponse(storeId: String, id: UUID, requestHash: String): PenyesuaianStokCreateResponse? {
        val row = PenyesuaianStokTable.selectAll().where { PenyesuaianStokTable.id eq id.toKotlinUuid() }.singleOrNull()
            ?: return null
        if (row[PenyesuaianStokTable.idToko] != storeId) conflict("ID_PENYESUAIAN_SUDAH_DIPAKAI", "ID Penyesuaian sudah digunakan.")
        if (row[PenyesuaianStokTable.requestHash].trim() != requestHash) {
            conflict("ID_PENYESUAIAN_SUDAH_DIPAKAI", "ID Penyesuaian sudah digunakan untuk data berbeda.")
        }
        return Gson().fromJson(row[PenyesuaianStokTable.responseSnapshot], PenyesuaianStokCreateResponse::class.java)
            ?: error("Respons Penyesuaian idempoten tidak tersedia.")
    }

    private fun ensureFile(storeId: String, fileId: UUID) {
        val exists = FileTable.selectAll().where {
            (FileTable.idToko eq storeId) and (FileTable.id eq fileId.toKotlinUuid())
        }.singleOrNull() != null
        if (!exists) notFound("FILE_TIDAK_DITEMUKAN", "File bukti tidak ditemukan.")
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

    private fun barangResponse(storeId: String, barangId: kotlin.uuid.Uuid): BarangResponse {
        val row = BarangTable.selectAll().where {
            (BarangTable.idToko eq storeId) and (BarangTable.id eq barangId)
        }.singleOrNull() ?: notFound("BARANG_TIDAK_DITEMUKAN", "Barang tidak ditemukan.")
        val categories = BarangKategoriTable.selectAll().where {
            (BarangKategoriTable.idToko eq storeId) and (BarangKategoriTable.barangId eq barangId)
        }.mapNotNull { relation ->
            val category = KategoriTable.selectAll().where {
                (KategoriTable.idToko eq storeId) and (KategoriTable.id eq relation[BarangKategoriTable.kategoriId])
            }.singleOrNull() ?: return@mapNotNull null
            KategoriResponse(
                id = category[KategoriTable.id].toJavaUuid(),
                nama = category[KategoriTable.nama],
                jumlahBarang = BarangKategoriTable.selectAll().where {
                    (BarangKategoriTable.idToko eq storeId) and
                        (BarangKategoriTable.kategoriId eq category[KategoriTable.id])
                }.count().toInt(),
            )
        }
        return BarangResponse(
            id = row[BarangTable.id].toJavaUuid(),
            nama = row[BarangTable.nama],
            satuan = row[BarangTable.satuan],
            sku = row[BarangTable.sku],
            hargaJual = row[BarangTable.hargaJual],
            stokMinimum = row[BarangTable.stokMinimum],
            stokSaatIni = row[BarangTable.stokSaatIni],
            hppRataRata = row[BarangTable.hppRataRata],
            status = row[BarangTable.status],
            fotoFileId = row[BarangTable.fotoFileId]?.toJavaUuid(),
            kategori = categories,
            dibuatOleh = row[BarangTable.dibuatOleh].toJavaUuid(),
            dibuatPada = row[BarangTable.dibuatPada],
            diubahPada = row[BarangTable.diubahPada],
        )
    }

    private fun log(account: Akun, action: String, referenceId: UUID, referenceType: String, at: Long) {
        LogAktivitasTable.insert {
            it[id] = UUID.randomUUID().toKotlinUuid()
            it[idToko] = account.idToko
            it[akunId] = account.id.toKotlinUuid()
            it[namaAkunSnapshot] = account.nama.take(80)
            it[kategori] = "STOK"
            it[aksi] = action
            it[LogAktivitasTable.referensiId] = referenceId.toKotlinUuid()
            it[jenisReferensi] = referenceType
            it[dibuatPada] = at
        }
    }

    private fun invalid(code: String, message: String): Nothing =
        throw com.aura.server.plugins.ApiException(
            io.ktor.http.HttpStatusCode.BadRequest,
            code,
            message,
        )

    private fun conflict(code: String, message: String): Nothing =
        throw com.aura.server.plugins.ApiException(
            io.ktor.http.HttpStatusCode.Conflict,
            code,
            message,
        )

    private fun notFound(code: String, message: String): Nothing =
        throw com.aura.server.plugins.ApiException(
            io.ktor.http.HttpStatusCode.NotFound,
            code,
            message,
        )
}
