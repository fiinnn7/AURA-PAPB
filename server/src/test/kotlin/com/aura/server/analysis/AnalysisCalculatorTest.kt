package com.aura.server.analysis

import com.aura.server.auth.Akun
import com.aura.server.auth.Peran
import com.aura.server.auth.StatusAkun
import com.aura.server.plugins.ApiException
import io.ktor.http.HttpStatusCode
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AnalysisCalculatorTest {
    private val dayMs = 86_400_000L
    private val nowMs = 1_764_144_000_000L // fixed epoch ms (2025-11-26 WIB)

    private fun makeBarang(
        nama: String,
        stok: Int = 20,
        hpp: Long = 2000L,
        ageDays: Long = 40L,
        status: String = "AKTIF",
    ) = RawBarang(
        id = UUID.randomUUID(),
        nama = nama,
        fotoFileId = null,
        satuan = "pcs",
        stokSaatIni = stok,
        hppRataRata = hpp,
        status = status,
        dibuatPada = nowMs - ageDays * dayMs,
    )

    @Test
    fun `financial summary calculates omzet, laba kotor, biaya ops, laba bersih, and kerugian`() {
        val b1 = makeBarang("Indomie", stok = 10, hpp = 2500L)
        val b2 = makeBarang("Telur", stok = 5, hpp = 20000L)
        val tx1 = UUID.randomUUID()
        val txPrev = UUID.randomUUID()

        val dataset = StoreAnalysisDataset(
            barangList = listOf(b1, b2),
            saleItems = listOf(
                RawSaleItem(tx1, b1.id, 10, 3500L, 2500L, nowMs - 1 * dayMs),
                RawSaleItem(txPrev, b1.id, 5, 3500L, 2500L, nowMs - 9 * dayMs),
            ),
            restocks = emptyList(),
            movements = emptyList(),
            expenses = listOf(RawExpense(3000L, nowMs - 1 * dayMs)),
            adjustmentLosses = listOf(
                RawAdjustmentLoss(2500L, "RUSAK", nowMs - 1 * dayMs),
                RawAdjustmentLoss(5000L, "SALAH_CATAT", nowMs - 1 * dayMs), // not shrinkage
            ),
        )

        val res = AnalysisCalculator.computeOverview(dataset, AnalisisPeriode.MINGGUAN, nowMs)
        assertEquals(35_000L, res.finansial.omzet)
        assertEquals(10_000L, res.finansial.labaKotor)
        assertEquals(3_000L, res.finansial.biayaOperasional)
        assertEquals(7_000L, res.finansial.labaBersih)
        assertEquals(2_500L, res.finansial.kerugianPenyesuaian)
        assertEquals(10 * 2500L + 5 * 20000L, res.finansial.modalTertahan)
        assertEquals(100.0, res.finansial.omzetPerubahanPct)
        assertEquals(7, res.finansial.harian.size)
    }

    @Test
    fun `fast and slow moving excludes new items lt 14 days and handles zero sales`() {
        val bList = (1..6).map { idx -> makeBarang("Barang $idx", stok = 10, hpp = 1000L, ageDays = 30) }
        val newItem = makeBarang("Barang Baru", stok = 50, hpp = 1000L, ageDays = 5)
        val all = bList + newItem

        val sales = listOf(
            RawSaleItem(UUID.randomUUID(), bList[0].id, 30, 2000L, 1000L, nowMs - 1 * dayMs),
            RawSaleItem(UUID.randomUUID(), bList[1].id, 20, 2000L, 1000L, nowMs - 2 * dayMs),
            RawSaleItem(UUID.randomUUID(), bList[2].id, 5, 2000L, 1000L, nowMs - 2 * dayMs),
            RawSaleItem(UUID.randomUUID(), bList[3].id, 3, 2000L, 1000L, nowMs - 2 * dayMs),
            // bList[4] and bList[5] have 0 sales
        )

        val dataset = StoreAnalysisDataset(all, sales, emptyList(), emptyList(), emptyList(), emptyList())
        val range = AnalysisCalculator.computeRange(AnalisisPeriode.MINGGUAN, nowMs)
        val mov = AnalysisCalculator.computeMovement(dataset, AnalisisPeriode.MINGGUAN, range, nowMs)

        assertEquals("ok", mov.status)
        assertTrue(mov.fast.any { it.barangId == bList[0].id.toString() })
        assertTrue(mov.slow.any { it.barangId == bList[5].id.toString() && it.tidakLaku })
        assertTrue(mov.fast.none { it.barangId == newItem.id.toString() })
        assertTrue(mov.slow.none { it.barangId == newItem.id.toString() })

        val zeroRunway = mov.slow.first { it.barangId == bList[5].id.toString() }
        assertNull(zeroRunway.umurStokHari)
    }

    @Test
    fun `pair counting finds strong lift gt 1_2 and returns insufficient_data below min baskets`() {
        val kopi = makeBarang("Kopi")
        val gula = makeBarang("Gula")
        val sabun = makeBarang("Sabun")

        val sales = mutableListOf<RawSaleItem>()
        // 40 baskets with kopi + gula, 20 baskets with only sabun + extra
        val extra = makeBarang("Sikat")
        repeat(40) { i ->
            val tx = UUID.randomUUID()
            val t = nowMs - (i % 5 + 1) * dayMs
            sales += RawSaleItem(tx, kopi.id, 1, 5000L, 3000L, t)
            sales += RawSaleItem(tx, gula.id, 1, 12000L, 9000L, t)
        }
        repeat(20) { i ->
            val tx = UUID.randomUUID()
            val t = nowMs - (i % 5 + 1) * dayMs
            sales += RawSaleItem(tx, sabun.id, 1, 4000L, 2500L, t)
            sales += RawSaleItem(tx, extra.id, 1, 6000L, 4000L, t)
        }

        val dataset = StoreAnalysisDataset(listOf(kopi, gula, sabun, extra), sales, emptyList(), emptyList(), emptyList(), emptyList())
        val rangeW = AnalysisCalculator.computeRange(AnalisisPeriode.MINGGUAN, nowMs)
        val corrW = AnalysisCalculator.computeCorrelations(dataset, AnalisisPeriode.MINGGUAN, rangeW, nowMs)

        assertEquals("ok", corrW.status)
        assertEquals(60, corrW.keranjangDitemukan)
        val kopiRules = corrW.produk.firstOrNull { it.barangId == kopi.id.toString() }
        assertNotNull(kopiRules)
        assertEquals(gula.id.toString(), kopiRules.terkait.first().barangId)
        assertTrue(kopiRules.terkait.first().lift > 1.2)

        // Monthly needs 100 baskets -> 60 should be insufficient_data
        val rangeM = AnalysisCalculator.computeRange(AnalisisPeriode.BULANAN, nowMs)
        val corrM = AnalysisCalculator.computeCorrelations(dataset, AnalisisPeriode.BULANAN, rangeM, nowMs)
        assertEquals("insufficient_data", corrM.status)
    }

    @Test
    fun `kasir role is forbidden from analysis endpoints`() {
        val emptyRepo = object : AnalysisRepository {
            override fun loadStoreDataset(idToko: String, sinceMs: Long) =
                StoreAnalysisDataset(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        }
        val service = AnalysisService(emptyRepo) { nowMs }
        val kasir = Akun(
            id = UUID.randomUUID(),
            idToko = "AB12CD34",
            email = null,
            nama = "Siti",
            namaNormalisasi = "siti",
            peran = Peran.KASIR,
            hash = "h",
            salt = "s",
            status = StatusAkun.AKTIF,
            wajibGantiSandi = false,
            nomorHp = null,
            fotoFileId = null,
            gagalLogin = 0,
            terkunciSampai = null,
            terakhirLogin = null,
            hapusTokoPada = null,
        )

        val ex = assertFailsWith<ApiException> {
            service.overview(kasir, "mingguan")
        }
        assertEquals(HttpStatusCode.Forbidden, ex.status)
    }
}
