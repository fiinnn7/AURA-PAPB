package com.aura.app.data.repository

import com.aura.app.data.remote.ApiErrorParser
import com.aura.app.data.remote.api.AnalisisApiService
import com.aura.app.data.remote.dto.AnalisisRingkasanResponseDto
import com.aura.app.data.remote.dto.KorelasiDetailResponseDto
import com.aura.app.data.remote.dto.PergerakanDetailResponseDto
import com.aura.app.data.remote.dto.ProporsiDetailResponseDto
import com.aura.app.data.remote.dto.TrenDetailResponseDto
import com.aura.app.data.remote.dto.UmurStokDetailResponseDto
import retrofit2.Response
import java.util.concurrent.ConcurrentHashMap

class AnalisisRepository(
    private val api: AnalisisApiService,
) {
    private val overviewCache = ConcurrentHashMap<String, AnalisisRingkasanResponseDto>()
    private val movementCache = ConcurrentHashMap<String, PergerakanDetailResponseDto>()
    private val runwayCache = ConcurrentHashMap<String, UmurStokDetailResponseDto>()
    private val proportionCache = ConcurrentHashMap<String, ProporsiDetailResponseDto>()
    private val trendCache = ConcurrentHashMap<String, TrenDetailResponseDto>()
    private val correlationCache = ConcurrentHashMap<String, KorelasiDetailResponseDto>()

    fun clearCache() {
        overviewCache.clear()
        movementCache.clear()
        runwayCache.clear()
        proportionCache.clear()
        trendCache.clear()
        correlationCache.clear()
    }

    fun cachedOverview(periode: String): AnalisisRingkasanResponseDto? = overviewCache[periode]
    fun cachedMovement(periode: String, jenis: String): PergerakanDetailResponseDto? = movementCache["$periode|$jenis"]
    fun cachedRunway(periode: String): UmurStokDetailResponseDto? = runwayCache[periode]
    fun cachedProportion(periode: String, jenis: String): ProporsiDetailResponseDto? = proportionCache["$periode|$jenis"]
    fun cachedTrend(periode: String, arah: String): TrenDetailResponseDto? = trendCache["$periode|$arah"]
    fun cachedCorrelation(periode: String): KorelasiDetailResponseDto? = correlationCache[periode]

    suspend fun getRingkasan(periode: String): AnalisisRingkasanResponseDto {
        val data = unwrap(api.getRingkasan(periode))
        overviewCache[periode] = data
        return data
    }

    suspend fun getPergerakan(periode: String, jenis: String): PergerakanDetailResponseDto {
        val data = unwrap(api.getPergerakan(periode, jenis))
        movementCache["$periode|$jenis"] = data
        return data
    }

    suspend fun getUmurStok(periode: String): UmurStokDetailResponseDto {
        val data = unwrap(api.getUmurStok(periode))
        runwayCache[periode] = data
        return data
    }

    suspend fun getProporsi(periode: String, jenis: String): ProporsiDetailResponseDto {
        val data = unwrap(api.getProporsi(periode, jenis))
        proportionCache["$periode|$jenis"] = data
        return data
    }

    suspend fun getTren(periode: String, arah: String): TrenDetailResponseDto {
        val data = unwrap(api.getTren(periode, arah))
        trendCache["$periode|$arah"] = data
        return data
    }

    suspend fun getKorelasi(periode: String): KorelasiDetailResponseDto {
        val data = unwrap(api.getKorelasi(periode))
        correlationCache[periode] = data
        return data
    }

    private fun <T> unwrap(response: Response<T>): T {
        if (response.isSuccessful) {
            return response.body() ?: throw ApiErrorParser.parse(response.code(), null)
        }
        val err = response.errorBody()?.string()
        throw ApiErrorParser.parse(response.code(), err)
    }
}
