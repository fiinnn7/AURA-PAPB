package com.aura.app.data.remote.api

import com.aura.app.data.remote.dto.AnalisisRingkasanResponseDto
import com.aura.app.data.remote.dto.KorelasiDetailResponseDto
import com.aura.app.data.remote.dto.PergerakanDetailResponseDto
import com.aura.app.data.remote.dto.ProporsiDetailResponseDto
import com.aura.app.data.remote.dto.TrenDetailResponseDto
import com.aura.app.data.remote.dto.UmurStokDetailResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface AnalisisApiService {
    @GET("analisis/ringkasan")
    suspend fun getRingkasan(
        @Query("periode") periode: String,
    ): Response<AnalisisRingkasanResponseDto>

    @GET("analisis/pergerakan")
    suspend fun getPergerakan(
        @Query("periode") periode: String,
        @Query("jenis") jenis: String,
    ): Response<PergerakanDetailResponseDto>

    @GET("analisis/umur-stok")
    suspend fun getUmurStok(
        @Query("periode") periode: String,
    ): Response<UmurStokDetailResponseDto>

    @GET("analisis/proporsi")
    suspend fun getProporsi(
        @Query("periode") periode: String,
        @Query("jenis") jenis: String,
    ): Response<ProporsiDetailResponseDto>

    @GET("analisis/tren")
    suspend fun getTren(
        @Query("periode") periode: String,
        @Query("arah") arah: String,
    ): Response<TrenDetailResponseDto>

    @GET("analisis/korelasi")
    suspend fun getKorelasi(
        @Query("periode") periode: String,
    ): Response<KorelasiDetailResponseDto>
}
