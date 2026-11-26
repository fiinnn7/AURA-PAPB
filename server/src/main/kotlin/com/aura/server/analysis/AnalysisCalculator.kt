package com.aura.server.analysis

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.round

internal object AnalysisCalculator {
    val WIB: ZoneId = ZoneId.of("Asia/Jakarta")
    private val ISO_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
    private val LOSS_REASONS = setOf("RUSAK", "HILANG", "KADALUARSA")

    fun computeRange(periode: AnalisisPeriode, nowMs: Long = System.currentTimeMillis()): RentangWaktu {
        val today = Instant.ofEpochMilli(nowMs).atZone(WIB).toLocalDate()
        val startDate = today.minusDays((periode.days - 1).toLong())
        val endDateExclusive = today.plusDays(1)
        val prevStartDate = startDate.minusDays(periode.days.toLong())

        val startMs = startDate.atStartOfDay(WIB).toInstant().toEpochMilli()
        val endMs = endDateExclusive.atStartOfDay(WIB).toInstant().toEpochMilli()
        val prevStartMs = prevStartDate.atStartOfDay(WIB).toInstant().toEpochMilli()
        val prevEndMs = startMs

        return RentangWaktu(
            startMs = startMs,
            endMs = endMs,
            prevStartMs = prevStartMs,
            prevEndMs = prevEndMs,
            startIso = startDate.atStartOfDay(WIB).format(ISO_FORMATTER),
            endIso = Instant.ofEpochMilli(nowMs).atZone(WIB).format(ISO_FORMATTER),
        )
    }

    fun computeOverview(
        dataset: StoreAnalysisDataset,
        periode: AnalisisPeriode,
        nowMs: Long = System.currentTimeMillis(),
    ): AnalisisRingkasanResponse {
        val range = computeRange(periode, nowMs)
        val nowIso = Instant.ofEpochMilli(nowMs).atZone(WIB).format(ISO_FORMATTER)

        val finansial = computeFinansial(dataset, periode, range, nowMs)
        val movementResult = computeMovement(dataset, periode, range, nowMs)
        val runway = computeRunway(dataset, periode, range)
        val profitList = computeProfitProportions(dataset, range)
        val expenseList = computeExpenseProportions(dataset, range)
        val trendAll = computeTrends(dataset, periode, nowMs)
        val corr = computeCorrelations(dataset, periode, range)

        val profitTop = profitList.filter { !it.isRugi }.take(5)
        val profitOthers = round1((100.0 - profitTop.sumOf { it.persentase }).coerceAtLeast(0.0))
        val expenseTop = expenseList.take(5)
        val expenseOthers = round1((100.0 - expenseTop.sumOf { it.persentase }).coerceAtLeast(0.0))

        val naik = trendAll.filter { it.arah == "naik" }
        val turun = trendAll.filter { it.arah == "turun" }

        return AnalisisRingkasanResponse(
            periode = periode.key,
            rentangMulai = range.startIso,
            rentangSelesai = range.endIso,
            diperbaruiPada = nowIso,
            finansial = finansial,
            pergerakan = PergerakanRingkasDto(
                status = movementResult.status,
                fastCount = movementResult.fast.size,
                slowCount = movementResult.slow.size,
                fastPreview = movementResult.fast.take(3),
                slowPreview = movementResult.slow.take(3),
                totalModalSlow = movementResult.totalModalSlow,
                runwayKritisCount = runway.count { it.kategori == "kritis" },
            ),
            proporsi = ProporsiRingkasDto(
                totalProfit = profitList.filter { !it.isRugi }.sumOf { it.nominal },
                profitTop = profitTop,
                profitLainnyaPct = if (profitList.none { !it.isRugi }) 0.0 else profitOthers,
                totalPengeluaran = expenseList.sumOf { it.nominal },
                pengeluaranTop = expenseTop,
                pengeluaranLainnyaPct = if (expenseList.isEmpty()) 0.0 else expenseOthers,
            ),
            tren = TrenRingkasDto(
                naikCount = naik.size,
                turunCount = turun.size,
                naikPreview = naik.take(3),
                turunPreview = turun.take(3),
            ),
            korelasi = KorelasiRingkasDto(
                status = corr.status,
                jumlahProduk = corr.produk.size,
                keranjangDitemukan = corr.keranjangDitemukan,
                keranjangDibutuhkan = corr.keranjangDibutuhkan,
            ),
        )
    }

    fun computeFinansial(
        dataset: StoreAnalysisDataset,
        periode: AnalisisPeriode,
        range: RentangWaktu,
        nowMs: Long = System.currentTimeMillis(),
    ): RingkasanFinansialDto {
        val curItems = dataset.saleItems.filter { it.dibuatPada in range.startMs until range.endMs }
        val prevItems = dataset.saleItems.filter { it.dibuatPada in range.prevStartMs until range.prevEndMs }

        val omzetCur = curItems.sumOf { it.jumlah.toLong() * it.hargaJual }
        val omzetPrev = prevItems.sumOf { it.jumlah.toLong() * it.hargaJual }

        val labaKotorCur = curItems.sumOf { it.jumlah.toLong() * (it.hargaJual - it.hppSnapshot) }
        val labaKotorPrev = prevItems.sumOf { it.jumlah.toLong() * (it.hargaJual - it.hppSnapshot) }

        val biayaCur = dataset.expenses.filter { it.tanggal in range.startMs until range.endMs }.sumOf { it.nominal }
        val biayaPrev = dataset.expenses.filter { it.tanggal in range.prevStartMs until range.prevEndMs }.sumOf { it.nominal }

        val labaBersihCur = labaKotorCur - biayaCur
        val labaBersihPrev = labaKotorPrev - biayaPrev

        val kerugian = dataset.adjustmentLosses
            .filter { it.tanggal in range.startMs until range.endMs && it.alasan.uppercase() in LOSS_REASONS }
            .sumOf { it.nilaiKerugian }

        val modalTertahan = dataset.barangList
            .filter { it.status == "AKTIF" }
            .sumOf { it.stokSaatIni.toLong() * it.hppRataRata }

        val today = Instant.ofEpochMilli(nowMs).atZone(WIB).toLocalDate()
        val startDate = today.minusDays((periode.days - 1).toLong())
        val byDate = curItems.groupBy { Instant.ofEpochMilli(it.dibuatPada).atZone(WIB).toLocalDate() }
        val harian = (0 until periode.days).map { offset ->
            val date = startDate.plusDays(offset.toLong())
            val items = byDate[date].orEmpty()
            TitikHarianDto(
                tanggal = date.toString(),
                omzet = items.sumOf { it.jumlah.toLong() * it.hargaJual },
                labaKotor = items.sumOf { it.jumlah.toLong() * (it.hargaJual - it.hppSnapshot) },
            )
        }

        return RingkasanFinansialDto(
            omzet = omzetCur,
            omzetPerubahanPct = growthPct(omzetCur, omzetPrev),
            labaKotor = labaKotorCur,
            labaKotorPerubahanPct = growthPct(labaKotorCur, labaKotorPrev),
            labaBersih = labaBersihCur,
            labaBersihPerubahanPct = growthPct(labaBersihCur, labaBersihPrev),
            grossMarginPct = if (omzetCur <= 0L) null else round1(100.0 * labaKotorCur / omzetCur),
            netMarginPct = if (omzetCur <= 0L) null else round1(100.0 * labaBersihCur / omzetCur),
            biayaOperasional = biayaCur,
            kerugianPenyesuaian = kerugian,
            modalTertahan = modalTertahan,
            harian = harian,
        )
    }

    internal data class MovementResult(
        val status: String,
        val fast: List<PergerakanBarangItemDto>,
        val slow: List<PergerakanBarangItemDto>,
        val totalModalSlow: Long,
    )

    fun computeMovement(
        dataset: StoreAnalysisDataset,
        periode: AnalisisPeriode,
        range: RentangWaktu,
        nowMs: Long = System.currentTimeMillis(),
    ): MovementResult {
        val activeBarang = dataset.barangList.filter { it.status == "AKTIF" }
        val minAgeMs = 14L * 86_400_000L
        val candidates = activeBarang.filter { nowMs - it.dibuatPada >= minAgeMs }
        if (candidates.size < 5) {
            return MovementResult("insufficient_data", emptyList(), emptyList(), 0L)
        }

        val soldByProduct = dataset.saleItems
            .filter { it.dibuatPada in range.startMs until range.endMs }
            .groupBy { it.barangId }
            .mapValues { (_, list) -> list.sumOf { it.jumlah } }

        val activeDaysMap = reconstructActiveStockDays(
            barangList = candidates,
            movements = dataset.movements,
            startMs = range.startMs,
            days = periode.days,
            nowMs = nowMs,
        )

        val minFast = if (periode == AnalisisPeriode.MINGGUAN) 2 else 5
        val maxSlow = if (periode == AnalisisPeriode.MINGGUAN) 1 else 3

        data class Scored(
            val barang: RawBarang,
            val qty: Int,
            val velocity: Double,
            val strPct: Double?,
            val umurHari: Int?,
        )

        val scored = candidates.map { b ->
            val qty = soldByProduct[b.id] ?: 0
            val daysAvail = max(1, activeDaysMap[b.id] ?: periode.days)
            val vel = qty.toDouble() / daysAvail
            val denom = qty + b.stokSaatIni
            val str = if (denom <= 0) null else round1(100.0 * qty / denom)
            val umur = if (vel <= 0.0) null else floor(b.stokSaatIni / vel).toInt()
            Scored(b, qty, vel, str, umur)
        }

        val sortedVel = scored.map { it.velocity }.sorted()
        val p80 = percentile(sortedVel, 0.80)
        val p20 = percentile(sortedVel, 0.20)

        val fastSet = scored.filter { it.velocity >= p80 && it.qty >= minFast }.map { it.barang.id }.toSet()
        val fast = scored.filter { it.barang.id in fastSet }
            .sortedByDescending { it.velocity }
            .map {
                PergerakanBarangItemDto(
                    barangId = it.barang.id.toString(),
                    nama = it.barang.nama,
                    fotoFileId = it.barang.fotoFileId?.toString(),
                    satuan = it.barang.satuan,
                    stok = it.barang.stokSaatIni,
                    qtyTerjual = it.qty,
                    kecepatan = round2(it.velocity),
                    strPct = it.strPct,
                    umurStokHari = it.umurHari,
                    tidakLaku = false,
                    modalTertahan = it.barang.stokSaatIni.toLong() * it.barang.hppRataRata,
                )
            }

        val slow = scored.filter { it.barang.id !in fastSet && (it.velocity <= p20 || it.qty < maxSlow) }
            .sortedBy { it.velocity }
            .map {
                PergerakanBarangItemDto(
                    barangId = it.barang.id.toString(),
                    nama = it.barang.nama,
                    fotoFileId = it.barang.fotoFileId?.toString(),
                    satuan = it.barang.satuan,
                    stok = it.barang.stokSaatIni,
                    qtyTerjual = it.qty,
                    kecepatan = round2(it.velocity),
                    strPct = it.strPct,
                    umurStokHari = it.umurHari,
                    tidakLaku = it.qty == 0,
                    modalTertahan = it.barang.stokSaatIni.toLong() * it.barang.hppRataRata,
                )
            }

        return MovementResult(
            status = "ok",
            fast = fast,
            slow = slow,
            totalModalSlow = slow.sumOf { it.modalTertahan },
        )
    }

    fun computeRunway(
        dataset: StoreAnalysisDataset,
        periode: AnalisisPeriode,
        range: RentangWaktu,
        nowMs: Long = System.currentTimeMillis(),
    ): List<UmurStokItemDto> {
        val activeBarang = dataset.barangList.filter { it.status == "AKTIF" }
        val soldByProduct = dataset.saleItems
            .filter { it.dibuatPada in range.startMs until range.endMs }
            .groupBy { it.barangId }
            .mapValues { (_, list) -> list.sumOf { it.jumlah } }

        val activeDaysMap = reconstructActiveStockDays(
            barangList = activeBarang,
            movements = dataset.movements,
            startMs = range.startMs,
            days = periode.days,
            nowMs = nowMs,
        )

        return activeBarang.map { b ->
            val qty = soldByProduct[b.id] ?: 0
            val daysAvail = max(1, activeDaysMap[b.id] ?: periode.days)
            val rataHarian = qty.toDouble() / daysAvail
            val umur = if (rataHarian <= 0.0) null else floor(b.stokSaatIni / rataHarian).toInt()
            val kategori = when {
                umur == null -> "tidak_diketahui"
                umur <= 3 -> "kritis"
                umur <= 7 -> "perhatian"
                else -> "aman"
            }
            UmurStokItemDto(
                barangId = b.id.toString(),
                nama = b.nama,
                fotoFileId = b.fotoFileId?.toString(),
                satuan = b.satuan,
                stok = b.stokSaatIni,
                rataHarian = round2(rataHarian),
                umurStokHari = umur,
                kategori = kategori,
            )
        }.sortedWith(compareBy<UmurStokItemDto> { it.umurStokHari == null }.thenBy { it.umurStokHari ?: Int.MAX_VALUE })
    }

    fun computeProfitProportions(dataset: StoreAnalysisDataset, range: RentangWaktu): List<ProporsiItemDto> {
        val barangMap = dataset.barangList.associateBy { it.id }
        val grouped = dataset.saleItems
            .filter { it.dibuatPada in range.startMs until range.endMs }
            .groupBy { it.barangId }
            .mapNotNull { (barangId, items) ->
                val b = barangMap[barangId] ?: return@mapNotNull null
                val profit = items.sumOf { it.jumlah.toLong() * (it.hargaJual - it.hppSnapshot) }
                barangId to (b.nama to profit)
            }

        val positive = grouped.filter { it.second.second > 0L }.sortedByDescending { it.second.second }
        val totalPositive = positive.sumOf { it.second.second }

        var cum = 0L
        val positiveDtos = positive.map { (id, pair) ->
            val (nama, profit) = pair
            val pct = if (totalPositive <= 0L) 0.0 else round1(100.0 * profit / totalPositive)
            val ratioBefore = if (totalPositive <= 0L) 1.0 else cum.toDouble() / totalPositive
            cum += profit
            val abc = when {
                ratioBefore < 0.80 -> "A"
                ratioBefore < 0.95 -> "B"
                else -> "C"
            }
            ProporsiItemDto(
                barangId = id.toString(),
                nama = nama,
                nominal = profit,
                persentase = pct,
                kelasAbc = abc,
                isRugi = false,
            )
        }

        val lossDtos = grouped.filter { it.second.second <= 0L }
            .sortedBy { it.second.second }
            .map { (id, pair) ->
                ProporsiItemDto(
                    barangId = id.toString(),
                    nama = pair.first,
                    nominal = pair.second,
                    persentase = 0.0,
                    kelasAbc = null,
                    isRugi = true,
                )
            }

        return positiveDtos + lossDtos
    }

    fun computeExpenseProportions(dataset: StoreAnalysisDataset, range: RentangWaktu): List<ProporsiItemDto> {
        val barangMap = dataset.barangList.associateBy { it.id }
        val grouped = dataset.restocks
            .filter { it.tanggal in range.startMs until range.endMs }
            .groupBy { it.barangId }
            .mapNotNull { (barangId, list) ->
                val b = barangMap[barangId] ?: return@mapNotNull null
                val total = list.sumOf { it.totalDibayar }
                if (total <= 0L) null else Triple(barangId, b.nama, total)
            }
            .sortedByDescending { it.third }

        val grandTotal = grouped.sumOf { it.third }
        return grouped.map { (id, nama, total) ->
            ProporsiItemDto(
                barangId = id.toString(),
                nama = nama,
                nominal = total,
                persentase = if (grandTotal <= 0L) 0.0 else round1(100.0 * total / grandTotal),
                kelasAbc = null,
                isRugi = false,
            )
        }
    }

    fun computeTrends(
        dataset: StoreAnalysisDataset,
        periode: AnalisisPeriode,
        nowMs: Long = System.currentTimeMillis(),
    ): List<TrenItemDto> {
        val today = Instant.ofEpochMilli(nowMs).atZone(WIB).toLocalDate()
        val endMs = today.plusDays(1).atStartOfDay(WIB).toInstant().toEpochMilli()

        val curDays = if (periode == AnalisisPeriode.MINGGUAN) 7 else 30
        val compDays = if (periode == AnalisisPeriode.MINGGUAN) 28 else 30
        val curStart = today.minusDays((curDays - 1).toLong())
        val curStartMs = curStart.atStartOfDay(WIB).toInstant().toEpochMilli()

        val compStart: LocalDate
        val compEndExclusive: LocalDate
        if (periode == AnalisisPeriode.MINGGUAN) {
            compStart = today.minusDays(27)
            compEndExclusive = today.plusDays(1)
        } else {
            compStart = curStart.minusDays(30)
            compEndExclusive = curStart
        }
        val compStartMs = compStart.atStartOfDay(WIB).toInstant().toEpochMilli()
        val compEndMs = compEndExclusive.atStartOfDay(WIB).toInstant().toEpochMilli()

        val minAgeDays = if (periode == AnalisisPeriode.MINGGUAN) 28L else 30L
        val activeBarang = dataset.barangList.filter {
            it.status == "AKTIF" && (nowMs - it.dibuatPada >= minAgeDays * 86_400_000L)
        }
        if (activeBarang.isEmpty()) return emptyList()

        val sparkStart = if (periode == AnalisisPeriode.MINGGUAN) today.minusDays(27) else today.minusDays(29)
        val sparkLen = if (periode == AnalisisPeriode.MINGGUAN) 28 else 30

        val curActiveDays = reconstructActiveStockDays(activeBarang, dataset.movements, curStartMs, curDays, nowMs)
        val compActiveDays = reconstructActiveStockDays(activeBarang, dataset.movements, compStartMs, compDays, compEndMs)

        val itemsByProduct = dataset.saleItems.groupBy { it.barangId }

        return activeBarang.mapNotNull { b ->
            val items = itemsByProduct[b.id].orEmpty()
            val curQty = items.filter { it.dibuatPada in curStartMs until endMs }.sumOf { it.jumlah }
            val compQty = items.filter { it.dibuatPada in compStartMs until compEndMs }.sumOf { it.jumlah }
            if (compQty < 5 && curQty < 5) return@mapNotNull null

            val lajuCur = curQty.toDouble() / max(1, curActiveDays[b.id] ?: curDays)
            val lajuComp = compQty.toDouble() / max(1, compActiveDays[b.id] ?: compDays)

            val (arah, pct) = when {
                lajuComp == 0.0 && lajuCur > 0.0 -> "naik" to 100.0
                lajuComp == 0.0 -> return@mapNotNull null
                else -> {
                    val ratio = lajuCur / lajuComp
                    val change = round1((ratio - 1.0) * 100.0)
                    when {
                        ratio >= 1.25 -> "naik" to change
                        ratio <= 0.75 -> "turun" to change
                        else -> return@mapNotNull null
                    }
                }
            }

            val dailyMap = items
                .filter { it.dibuatPada in sparkStart.atStartOfDay(WIB).toInstant().toEpochMilli() until endMs }
                .groupBy { Instant.ofEpochMilli(it.dibuatPada).atZone(WIB).toLocalDate() }
                .mapValues { (_, list) -> list.sumOf { it.jumlah } }
            val sparkline = (0 until sparkLen).map { dailyMap[sparkStart.plusDays(it.toLong())] ?: 0 }

            TrenItemDto(
                barangId = b.id.toString(),
                nama = b.nama,
                fotoFileId = b.fotoFileId?.toString(),
                satuan = b.satuan,
                lajuSekarang = round2(lajuCur),
                lajuPembanding = round2(lajuComp),
                perubahanPct = pct,
                arah = arah,
                sparkline = sparkline,
            )
        }.sortedByDescending { abs(it.perubahanPct) }
    }

    fun computeCorrelations(
        dataset: StoreAnalysisDataset,
        periode: AnalisisPeriode,
        range: RentangWaktu,
        nowMs: Long = System.currentTimeMillis(),
    ): KorelasiDetailResponse {
        val nowIso = Instant.ofEpochMilli(nowMs).atZone(WIB).format(ISO_FORMATTER)
        val minBaskets = if (periode == AnalisisPeriode.MINGGUAN) 50 else 100
        val minSupport = if (periode == AnalisisPeriode.MINGGUAN) 0.02 else 0.015

        val baskets = dataset.saleItems
            .filter { it.dibuatPada in range.startMs until range.endMs }
            .groupBy { it.transaksiId }
            .values
            .map { items -> items.map { it.barangId }.toSet() }
            .filter { it.size >= 2 }

        if (baskets.size < minBaskets) {
            return KorelasiDetailResponse(
                status = "insufficient_data",
                periode = periode.key,
                keranjangDitemukan = baskets.size,
                keranjangDibutuhkan = minBaskets,
                diperbaruiPada = nowIso,
                produk = emptyList(),
            )
        }

        val n = baskets.size.toDouble()
        val singleCount = HashMap<UUID, Int>()
        val pairCount = HashMap<Pair<UUID, UUID>, Int>()

        for (basket in baskets) {
            for (a in basket) {
                singleCount[a] = (singleCount[a] ?: 0) + 1
                for (b in basket) {
                    if (a != b) {
                        val key = a to b
                        pairCount[key] = (pairCount[key] ?: 0) + 1
                    }
                }
            }
        }

        val barangMap = dataset.barangList.associateBy { it.id }

        data class RawRule(
            val a: UUID,
            val b: UUID,
            val support: Double,
            val confidence: Double,
            val lift: Double,
        )

        val rules = pairCount.mapNotNull { (pair, cnt) ->
            val (a, b) = pair
            val cntA = singleCount[a] ?: return@mapNotNull null
            val cntB = singleCount[b] ?: return@mapNotNull null
            val sup = cnt / n
            val conf = cnt.toDouble() / cntA
            val lift = conf / (cntB / n)
            if (sup >= minSupport && conf >= 0.30 && lift > 1.2) {
                RawRule(a, b, sup, conf, lift)
            } else {
                null
            }
        }

        val produkList = rules.groupBy { it.a }.mapNotNull { (aId, ruleList) ->
            val aInfo = barangMap[aId] ?: return@mapNotNull null
            val topRelated = ruleList
                .sortedWith(compareByDescending<RawRule> { it.lift }.thenByDescending { it.confidence })
                .take(5)
                .mapNotNull { r ->
                    val bInfo = barangMap[r.b] ?: return@mapNotNull null
                    val liftRounded = round2(r.lift)
                    val strength = when {
                        r.lift >= 2.0 -> "sangat_kuat"
                        r.lift >= 1.5 -> "kuat"
                        else -> "sedang"
                    }
                    KorelasiTerkaitDto(
                        barangId = bInfo.id.toString(),
                        nama = bInfo.nama,
                        fotoFileId = bInfo.fotoFileId?.toString(),
                        support = round4(r.support),
                        confidence = round4(r.confidence),
                        lift = liftRounded,
                        kekuatan = strength,
                        pesan = "${round(r.confidence * 100).toInt()}% pembeli ${aInfo.nama} juga membeli ${bInfo.nama}.",
                    )
                }
            if (topRelated.isEmpty()) return@mapNotNull null
            KorelasiProdukDto(
                barangId = aInfo.id.toString(),
                nama = aInfo.nama,
                fotoFileId = aInfo.fotoFileId?.toString(),
                maxLift = topRelated.first().lift,
                terkait = topRelated,
            )
        }.sortedByDescending { it.maxLift }

        return KorelasiDetailResponse(
            status = "ok",
            periode = periode.key,
            keranjangDitemukan = baskets.size,
            keranjangDibutuhkan = minBaskets,
            diperbaruiPada = nowIso,
            produk = produkList,
        )
    }

    internal fun reconstructActiveStockDays(
        barangList: List<RawBarang>,
        movements: List<RawMovement>,
        startMs: Long,
        days: Int,
        nowMs: Long,
    ): Map<UUID, Int> {
        val startDate = Instant.ofEpochMilli(startMs).atZone(WIB).toLocalDate()
        val movementsByProduct = movements.groupBy { it.barangId }

        return barangList.associate { b ->
            val prodMovements = movementsByProduct[b.id].orEmpty().sortedBy { it.dibuatPada }
            if (prodMovements.isEmpty()) {
                val createdDate = Instant.ofEpochMilli(b.dibuatPada).atZone(WIB).toLocalDate()
                val activeCount = (0 until days).count { offset ->
                    val day = startDate.plusDays(offset.toLong())
                    !day.isBefore(createdDate) && b.stokSaatIni > 0
                }
                b.id to max(1, activeCount)
            } else {
                var activeCount = 0
                for (offset in 0 until days) {
                    val day = startDate.plusDays(offset.toLong())
                    val dayEndMs = day.plusDays(1).atStartOfDay(WIB).toInstant().toEpochMilli().coerceAtMost(nowMs)
                    val lastBeforeOrOnDay = prodMovements.lastOrNull { it.dibuatPada < dayEndMs }
                    val stockOnDay = lastBeforeOrOnDay?.stokSesudah
                        ?: if (b.dibuatPada < dayEndMs) 0 else 0
                    if (stockOnDay > 0) activeCount++
                }
                b.id to max(1, activeCount)
            }
        }
    }

    private fun growthPct(cur: Long, prev: Long): Double? {
        if (prev == 0L) return null
        return round1(((cur - prev).toDouble() / abs(prev).toDouble()) * 100.0)
    }

    private fun percentile(sorted: List<Double>, q: Double): Double {
        if (sorted.isEmpty()) return 0.0
        if (sorted.size == 1) return sorted[0]
        val pos = q * (sorted.size - 1)
        val lo = floor(pos).toInt()
        val hi = (lo + 1).coerceAtMost(sorted.lastIndex)
        val frac = pos - lo
        return sorted[lo] + frac * (sorted[hi] - sorted[lo])
    }

    private fun round1(v: Double): Double = round(v * 10.0) / 10.0
    private fun round2(v: Double): Double = round(v * 100.0) / 100.0
    private fun round4(v: Double): Double = round(v * 10000.0) / 10000.0
}
