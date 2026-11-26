package com.aura.app.data.remote.datasource

import com.aura.app.data.remote.ApiErrorParser
import com.aura.app.data.remote.ApiServerException
import com.aura.app.data.remote.api.PenjualanApiService
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

class PenjualanRemoteDataSource(
    private val api: PenjualanApiService,
) {
    suspend fun getMetodePembayaran(): MetodePembayaranTokoDto = unwrap(api.getMetodePembayaran())

    suspend fun createTransaksi(request: TransaksiBuatRequestDto): TransaksiBuatResponseDto =
        unwrap(api.createTransaksi(request))

    suspend fun listTransaksi(cursor: String?): TransaksiListResponseDto =
        unwrap(api.listTransaksi(cursor = cursor))

    suspend fun detailTransaksi(transaksiId: String): TransaksiDetailResponseDto =
        unwrap(api.detailTransaksi(transaksiId))

    suspend fun cancelTransaksi(
        transaksiId: String,
        alasan: String,
    ): TransaksiBatalResponseDto = unwrap(
        api.cancelTransaksi(transaksiId, PembatalanTransaksiRequestDto(alasan)),
    )

    suspend fun requestCancellation(
        transaksiId: String,
        requestId: String,
        alasan: String,
    ): PengajuanPembatalanResponseDto = unwrap(
        api.requestCancellation(transaksiId, PengajuanPembatalanRequestDto(requestId, alasan)),
    )

    private fun <T> unwrap(response: Response<T>): T {
        if (response.isSuccessful) {
            return response.body() ?: throw ApiServerException(
                httpCode = response.code(),
                kode = "RESPONS_KOSONG",
                message = "Respons server kosong.",
            )
        }
        throw ApiErrorParser.parse(response.code(), response.errorBody()?.string())
    }
}
