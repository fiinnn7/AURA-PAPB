package com.aura.server.analysis

import com.aura.server.db.BarangTable
import com.aura.server.db.ItemTransaksiTable
import com.aura.server.db.PengeluaranTable
import com.aura.server.db.PenyesuaianStokTable
import com.aura.server.db.PergerakanStokTable
import com.aura.server.db.RestockTable
import com.aura.server.db.TransaksiPenjualanTable
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.innerJoin
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.uuid.toJavaUuid

internal class ExposedAnalysisRepository : AnalysisRepository {
    override fun loadStoreDataset(idToko: String, sinceMs: Long): StoreAnalysisDataset = transaction {
        val barangList = BarangTable
            .selectAll()
            .where { BarangTable.idToko eq idToko }
            .map { row ->
                RawBarang(
                    id = row[BarangTable.id].toJavaUuid(),
                    nama = row[BarangTable.nama],
                    fotoFileId = row[BarangTable.fotoFileId]?.toJavaUuid(),
                    satuan = row[BarangTable.satuan],
                    stokSaatIni = row[BarangTable.stokSaatIni],
                    hppRataRata = row[BarangTable.hppRataRata],
                    status = row[BarangTable.status],
                    dibuatPada = row[BarangTable.dibuatPada],
                )
            }

        val saleItems = ItemTransaksiTable
            .innerJoin(
                TransaksiPenjualanTable,
                { ItemTransaksiTable.transaksiId },
                { TransaksiPenjualanTable.id },
            )
            .selectAll()
            .where {
                (TransaksiPenjualanTable.idToko eq idToko) and
                    (TransaksiPenjualanTable.status eq "SELESAI") and
                    (TransaksiPenjualanTable.dibuatPada greaterEq sinceMs)
            }
            .map { row ->
                RawSaleItem(
                    transaksiId = row[ItemTransaksiTable.transaksiId].toJavaUuid(),
                    barangId = row[ItemTransaksiTable.barangId].toJavaUuid(),
                    jumlah = row[ItemTransaksiTable.jumlah],
                    hargaJual = row[ItemTransaksiTable.hargaJualSnapshot],
                    hppSnapshot = row[ItemTransaksiTable.hppSnapshot],
                    dibuatPada = row[TransaksiPenjualanTable.dibuatPada],
                )
            }

        val restocks = RestockTable
            .selectAll()
            .where { (RestockTable.idToko eq idToko) and (RestockTable.tanggal greaterEq sinceMs) }
            .map { row ->
                RawRestock(
                    barangId = row[RestockTable.barangId].toJavaUuid(),
                    totalDibayar = row[RestockTable.totalDibayar],
                    tanggal = row[RestockTable.tanggal],
                )
            }

        val movements = PergerakanStokTable
            .selectAll()
            .where { (PergerakanStokTable.idToko eq idToko) and (PergerakanStokTable.dibuatPada greaterEq sinceMs) }
            .orderBy(PergerakanStokTable.dibuatPada to SortOrder.ASC)
            .map { row ->
                RawMovement(
                    barangId = row[PergerakanStokTable.barangId].toJavaUuid(),
                    stokSesudah = row[PergerakanStokTable.stokSesudah],
                    dibuatPada = row[PergerakanStokTable.dibuatPada],
                )
            }

        val expenses = PengeluaranTable
            .selectAll()
            .where {
                (PengeluaranTable.idToko eq idToko) and
                    (PengeluaranTable.jenis eq "KUSTOM") and
                    (PengeluaranTable.status eq "TERCATAT") and
                    (PengeluaranTable.tanggal greaterEq sinceMs)
            }
            .map { row ->
                RawExpense(
                    nominal = row[PengeluaranTable.nominal],
                    tanggal = row[PengeluaranTable.tanggal],
                )
            }

        val adjustmentLosses = PenyesuaianStokTable
            .selectAll()
            .where { (PenyesuaianStokTable.idToko eq idToko) and (PenyesuaianStokTable.tanggal greaterEq sinceMs) }
            .map { row ->
                RawAdjustmentLoss(
                    nilaiKerugian = row[PenyesuaianStokTable.nilaiKerugian],
                    alasan = row[PenyesuaianStokTable.alasan],
                    tanggal = row[PenyesuaianStokTable.tanggal],
                )
            }

        StoreAnalysisDataset(
            barangList = barangList,
            saleItems = saleItems,
            restocks = restocks,
            movements = movements,
            expenses = expenses,
            adjustmentLosses = adjustmentLosses,
        )
    }
}
