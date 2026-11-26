package com.aura.app.data.remote.datasource

import com.aura.app.data.remote.ApiErrorParser
import com.aura.app.data.remote.ApiServerException
import com.aura.app.data.remote.api.KeuanganApiService
import com.aura.app.data.remote.dto.MetodePembayaranEnvelopeDto
import com.aura.app.data.remote.dto.MetodePembayaranTokoDto
import com.aura.app.data.remote.dto.PemasukanBuatRequestDto
import com.aura.app.data.remote.dto.PemasukanResponseDto
import com.aura.app.data.remote.dto.PemasukanWriteResponseDto
import com.aura.app.data.remote.dto.PembatalanKeuanganRequestDto
import com.aura.app.data.remote.dto.PengeluaranBuatRequestDto
import com.aura.app.data.remote.dto.PengeluaranResponseDto
import com.aura.app.data.remote.dto.PengeluaranWriteResponseDto
import retrofit2.Response

class KeuanganRemoteDataSource(
    private val api: KeuanganApiService,
) {
    suspend fun listPengeluaran(): List<PengeluaranResponseDto> {
        val result = mutableListOf<PengeluaranResponseDto>()
        val cursors = mutableSetOf<String>()
        var cursor: String? = null
        do {
            val page = unwrap(api.listPengeluaran(cursor = cursor))
            result += page.data
            cursor = page.nextCursor
            check(cursor == null || cursors.add(cursor)) {
                "Cursor daftar Pengeluaran dari server berulang."
            }
        } while (cursor != null)
        return result
    }

    suspend fun listPemasukan(): List<PemasukanResponseDto> {
        val result = mutableListOf<PemasukanResponseDto>()
        val cursors = mutableSetOf<String>()
        var cursor: String? = null
        do {
            val page = unwrap(api.listPemasukan(cursor = cursor))
            result += page.data
            cursor = page.nextCursor
            check(cursor == null || cursors.add(cursor)) {
                "Cursor daftar Pemasukan dari server berulang."
            }
        } while (cursor != null)
        return result
    }

    suspend fun createPengeluaran(request: PengeluaranBuatRequestDto): PengeluaranWriteResponseDto =
        unwrap(api.createPengeluaran(request))

    suspend fun detailPengeluaran(id: String): PengeluaranResponseDto =
        unwrap(api.detailPengeluaran(id)).pengeluaran

    suspend fun cancelPengeluaran(id: String, alasan: String): PengeluaranWriteResponseDto =
        unwrap(api.cancelPengeluaran(id, PembatalanKeuanganRequestDto(alasan)))

    suspend fun createPemasukan(request: PemasukanBuatRequestDto): PemasukanWriteResponseDto =
        unwrap(api.createPemasukan(request))

    suspend fun detailPemasukan(id: String): PemasukanResponseDto =
        unwrap(api.detailPemasukan(id)).pemasukan

    suspend fun cancelPemasukan(id: String, alasan: String): PemasukanWriteResponseDto =
        unwrap(api.cancelPemasukan(id, PembatalanKeuanganRequestDto(alasan)))

    suspend fun getMetodePembayaran(): MetodePembayaranTokoDto =
        unwrap(api.getMetodePembayaran())

    suspend fun updateMetodePembayaran(request: MetodePembayaranTokoDto): MetodePembayaranEnvelopeDto =
        unwrap(api.updateMetodePembayaran(request))

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
