package com.aura.app.data.remote.api

import com.aura.app.data.remote.dto.MetodePembayaranEnvelopeDto
import com.aura.app.data.remote.dto.MetodePembayaranTokoDto
import com.aura.app.data.remote.dto.PemasukanBuatRequestDto
import com.aura.app.data.remote.dto.PemasukanEnvelopeDto
import com.aura.app.data.remote.dto.PemasukanListResponseDto
import com.aura.app.data.remote.dto.PemasukanWriteResponseDto
import com.aura.app.data.remote.dto.PembatalanKeuanganRequestDto
import com.aura.app.data.remote.dto.PengeluaranBuatRequestDto
import com.aura.app.data.remote.dto.PengeluaranEnvelopeDto
import com.aura.app.data.remote.dto.PengeluaranListResponseDto
import com.aura.app.data.remote.dto.PengeluaranWriteResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface KeuanganApiService {
    @GET("pengeluaran")
    suspend fun listPengeluaran(
        @Query("limit") limit: Int = 100,
        @Query("cursor") cursor: String? = null,
    ): Response<PengeluaranListResponseDto>

    @POST("pengeluaran")
    suspend fun createPengeluaran(@Body request: PengeluaranBuatRequestDto): Response<PengeluaranWriteResponseDto>

    @GET("pengeluaran/{id}")
    suspend fun detailPengeluaran(@Path("id") id: String): Response<PengeluaranEnvelopeDto>

    @POST("pengeluaran/{id}/batal")
    suspend fun cancelPengeluaran(
        @Path("id") id: String,
        @Body request: PembatalanKeuanganRequestDto,
    ): Response<PengeluaranWriteResponseDto>

    @GET("pemasukan")
    suspend fun listPemasukan(
        @Query("limit") limit: Int = 100,
        @Query("cursor") cursor: String? = null,
    ): Response<PemasukanListResponseDto>

    @POST("pemasukan")
    suspend fun createPemasukan(@Body request: PemasukanBuatRequestDto): Response<PemasukanWriteResponseDto>

    @GET("pemasukan/{id}")
    suspend fun detailPemasukan(@Path("id") id: String): Response<PemasukanEnvelopeDto>

    @POST("pemasukan/{id}/batal")
    suspend fun cancelPemasukan(
        @Path("id") id: String,
        @Body request: PembatalanKeuanganRequestDto,
    ): Response<PemasukanWriteResponseDto>

    @GET("toko/metode-pembayaran")
    suspend fun getMetodePembayaran(): Response<MetodePembayaranTokoDto>

    @PUT("toko/metode-pembayaran")
    suspend fun updateMetodePembayaran(
        @Body request: MetodePembayaranTokoDto,
    ): Response<MetodePembayaranEnvelopeDto>
}
