package com.aura.server.analysis

import com.aura.server.auth.Akun
import com.aura.server.auth.Peran
import com.aura.server.plugins.ApiException
import io.ktor.http.HttpStatusCode
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

internal class AnalysisService(
    private val repository: AnalysisRepository = ExposedAnalysisRepository(),
    private val clockMs: () -> Long = { System.currentTimeMillis() },
) {
    private data class CachedDataset(val fetchedAtMs: Long, val dataset: StoreAnalysisDataset)

    private val cache = ConcurrentHashMap<String, CachedDataset>()
    private val cacheTtlMs = 5 * 60_000L

    fun overview(account: Akun, rawPeriode: String?): AnalisisRingkasanResponse {
        requireOwner(account)
        val periode = parsePeriode(rawPeriode)
        val nowMs = clockMs()
        val dataset = getDataset(account.idToko, nowMs)
        return AnalysisCalculator.computeOverview(dataset, periode, nowMs)
    }

    fun movement(account: Akun, rawPeriode: String?, rawJenis: String?): PergerakanDetailResponse {
        requireOwner(account)
        val periode = parsePeriode(rawPeriode)
        val jenis = when (rawJenis?.trim()?.lowercase()) {
            null, "", "fast" -> "fast"
            "slow" -> "slow"
            else -> throw ApiException(HttpStatusCode.BadRequest, "JENIS_TIDAK_VALID", "Jenis harus fast atau slow.")
        }
        val nowMs = clockMs()
        val dataset = getDataset(account.idToko, nowMs)
        val range = AnalysisCalculator.computeRange(periode, nowMs)
        val result = AnalysisCalculator.computeMovement(dataset, periode, range, nowMs)
        val nowIso = Instant.ofEpochMilli(nowMs).atZone(AnalysisCalculator.WIB).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        return PergerakanDetailResponse(
            jenis = jenis,
            periode = periode.key,
            status = result.status,
            totalModalSlow = result.totalModalSlow,
            diperbaruiPada = nowIso,
            items = if (jenis == "fast") result.fast else result.slow,
        )
    }

    fun stockRunway(account: Akun, rawPeriode: String?): UmurStokDetailResponse {
        requireOwner(account)
        val periode = parsePeriode(rawPeriode)
        val nowMs = clockMs()
        val dataset = getDataset(account.idToko, nowMs)
        val range = AnalysisCalculator.computeRange(periode, nowMs)
        val nowIso = Instant.ofEpochMilli(nowMs).atZone(AnalysisCalculator.WIB).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        return UmurStokDetailResponse(
            periode = periode.key,
            diperbaruiPada = nowIso,
            items = AnalysisCalculator.computeRunway(dataset, periode, range, nowMs),
        )
    }

    fun proportion(account: Akun, rawPeriode: String?, rawJenis: String?): ProporsiDetailResponse {
        requireOwner(account)
        val periode = parsePeriode(rawPeriode)
        val jenis = when (rawJenis?.trim()?.lowercase()) {
            null, "", "profit" -> "profit"
            "pengeluaran" -> "pengeluaran"
            else -> throw ApiException(HttpStatusCode.BadRequest, "JENIS_TIDAK_VALID", "Jenis harus profit atau pengeluaran.")
        }
        val nowMs = clockMs()
        val dataset = getDataset(account.idToko, nowMs)
        val range = AnalysisCalculator.computeRange(periode, nowMs)
        val nowIso = Instant.ofEpochMilli(nowMs).atZone(AnalysisCalculator.WIB).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        val items = if (jenis == "profit") {
            AnalysisCalculator.computeProfitProportions(dataset, range)
        } else {
            AnalysisCalculator.computeExpenseProportions(dataset, range)
        }
        val total = if (jenis == "profit") {
            items.filter { !it.isRugi }.sumOf { it.nominal }
        } else {
            items.sumOf { it.nominal }
        }
        return ProporsiDetailResponse(
            jenis = jenis,
            periode = periode.key,
            totalNominal = total,
            diperbaruiPada = nowIso,
            items = items,
        )
    }

    fun trend(account: Akun, rawPeriode: String?, rawArah: String?): TrenDetailResponse {
        requireOwner(account)
        val periode = parsePeriode(rawPeriode)
        val arah = when (rawArah?.trim()?.lowercase()) {
            null, "", "naik" -> "naik"
            "turun" -> "turun"
            else -> throw ApiException(HttpStatusCode.BadRequest, "ARAH_TIDAK_VALID", "Arah harus naik atau turun.")
        }
        val nowMs = clockMs()
        val dataset = getDataset(account.idToko, nowMs)
        val items = AnalysisCalculator.computeTrends(dataset, periode, nowMs).filter { it.arah == arah }
        val nowIso = Instant.ofEpochMilli(nowMs).atZone(AnalysisCalculator.WIB).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        val keterangan = if (periode == AnalisisPeriode.MINGGUAN) {
            "Membandingkan laju 7 hari terakhir dengan rata-rata 28 hari terakhir"
        } else {
            "Membandingkan laju 30 hari terakhir dengan 30 hari sebelumnya"
        }
        return TrenDetailResponse(
            arah = arah,
            periode = periode.key,
            keteranganPembanding = keterangan,
            diperbaruiPada = nowIso,
            items = items,
        )
    }

    fun correlations(account: Akun, rawPeriode: String?): KorelasiDetailResponse {
        requireOwner(account)
        val periode = parsePeriode(rawPeriode)
        val nowMs = clockMs()
        val dataset = getDataset(account.idToko, nowMs)
        val range = AnalysisCalculator.computeRange(periode, nowMs)
        return AnalysisCalculator.computeCorrelations(dataset, periode, range, nowMs)
    }

    fun invalidateCache(idToko: String) {
        cache.remove(idToko)
    }

    private fun getDataset(idToko: String, nowMs: Long): StoreAnalysisDataset {
        val cached = cache[idToko]
        if (cached != null && nowMs - cached.fetchedAtMs < cacheTtlMs) {
            return cached.dataset
        }
        val sinceMs = nowMs - 65L * 86_400_000L
        val fresh = repository.loadStoreDataset(idToko, sinceMs)
        cache[idToko] = CachedDataset(nowMs, fresh)
        return fresh
    }

    private fun parsePeriode(raw: String?): AnalisisPeriode = try {
        AnalisisPeriode.fromQuery(raw)
    } catch (e: IllegalArgumentException) {
        throw ApiException(HttpStatusCode.BadRequest, "PERIODE_TIDAK_VALID", e.message ?: "Periode tidak valid.")
    }

    private fun requireOwner(account: Akun) {
        if (account.peran != Peran.OWNER) {
            throw ApiException(HttpStatusCode.Forbidden, "AKSES_DITOLAK", "Hanya Owner yang dapat mengakses analisis.")
        }
    }
}

internal data class AnalysisComponents(val service: AnalysisService)

internal fun analysisComponents(repository: AnalysisRepository = ExposedAnalysisRepository()): AnalysisComponents =
    AnalysisComponents(AnalysisService(repository))
