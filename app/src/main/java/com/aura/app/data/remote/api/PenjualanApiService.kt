package com.aura.app.data.remote.api

import com.aura.app.data.remote.dto.MetodePembayaranTokoDto
import com.aura.app.data.remote.dto.PembatalanTransaksiRequestDto
import com.aura.app.data.remote.dto.PengajuanPembatalanRequestDto
import com.aura.app.data.remote.dto.PengajuanPembatalanResponseDto
import com.aura.app.data.remote.dto.TransaksiBatalResponseDto
import com.aura.app.data.remote.dto.TransaksiBuatRequestDto
import com.aura.app.data.remote.dto.TransaksiBuatResponseDto
import com.aura.app.data.remote.dto.TransaksiDetailResponseDto
import com.aura.app.data.remote.dto.TransaksiListResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface PenjualanApiService {
    @GET("toko/metode-pembayaran")
    suspend fun getMetodePembayaran(): Response<MetodePembayaranTokoDto>

    @POST("transaksi")
    suspend fun createTransaksi(@Body request: TransaksiBuatRequestDto): Response<TransaksiBuatResponseDto>

    @GET("transaksi")
    suspend fun listTransaksi(
        @Query("limit") limit: Int = 100,
        @Query("cursor") cursor: String? = null,
    ): Response<TransaksiListResponseDto>

    @GET("transaksi/{transaksiId}")
    suspend fun detailTransaksi(
        @Path("transaksiId") transaksiId: String,
    ): Response<TransaksiDetailResponseDto>

    @POST("transaksi/{transaksiId}/batal")
    suspend fun cancelTransaksi(
        @Path("transaksiId") transaksiId: String,
        @Body request: PembatalanTransaksiRequestDto,
    ): Response<TransaksiBatalResponseDto>

    @POST("transaksi/{transaksiId}/ajukan-pembatalan")
    suspend fun requestCancellation(
        @Path("transaksiId") transaksiId: String,
        @Body request: PengajuanPembatalanRequestDto,
    ): Response<PengajuanPembatalanResponseDto>
}
