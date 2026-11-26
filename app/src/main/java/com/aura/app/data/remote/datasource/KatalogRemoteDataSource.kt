package com.aura.app.data.remote.datasource

import com.aura.app.data.remote.ApiErrorParser
import com.aura.app.data.remote.ApiServerException
import com.aura.app.data.remote.api.KatalogApiService
import com.aura.app.data.remote.dto.BarangCreateRequestDto
import com.aura.app.data.remote.dto.BarangDeleteResponseDto
import com.aura.app.data.remote.dto.BarangDetailResponseDto
import com.aura.app.data.remote.dto.BarangDto
import com.aura.app.data.remote.dto.BarangListResponseDto
import com.aura.app.data.remote.dto.BarangUpdateRequestDto
import com.aura.app.data.remote.dto.BarangWriteResponseDto
import com.aura.app.data.remote.dto.FileUploadResponseDto
import com.aura.app.data.remote.dto.KategoriDto
import com.aura.app.data.remote.dto.KategoriListResponseDto
import com.aura.app.data.remote.dto.KategoriRequestDto
import com.aura.app.data.remote.dto.RestockRequestDto
import com.aura.app.data.remote.dto.RestockCreateResponseDto
import com.aura.app.data.remote.dto.PenyesuaianStokRequestDto
import com.aura.app.data.remote.dto.PenyesuaianStokCreateResponseDto
import com.aura.app.data.remote.dto.PergerakanStokListResponseDto
import com.aura.app.data.remote.dto.SupplierDetailResponseDto
import com.aura.app.data.remote.dto.SupplierDto
import com.aura.app.data.remote.dto.SupplierListResponseDto
import com.aura.app.data.remote.dto.SupplierRequestDto
import com.aura.app.data.remote.dto.SupplierStatusRequestDto
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.util.UUID

class KatalogRemoteDataSource(
    private val api: KatalogApiService,
) {
    suspend fun createRestock(request: RestockRequestDto): RestockCreateResponseDto =
        unwrap(api.createRestock(request))

    suspend fun createAdjustment(request: PenyesuaianStokRequestDto): PenyesuaianStokCreateResponseDto =
        unwrap(api.createAdjustment(request))

    suspend fun listMovements(barangId: String? = null, cursor: String? = null): PergerakanStokListResponseDto =
        unwrap(api.listMovements(barangId = barangId, cursor = cursor))

    suspend fun listBarang(status: String): List<BarangDto> {
        val result = mutableListOf<BarangDto>()
        var cursor: String? = null
        do {
            val page: BarangListResponseDto = unwrap(api.listBarang(status = status, cursor = cursor))
            result += page.data
            cursor = page.nextCursor
        } while (cursor != null)
        return result
    }

    suspend fun detailBarang(id: String): BarangDetailResponseDto = unwrap(api.detailBarang(id))

    suspend fun createBarang(request: BarangCreateRequestDto): BarangWriteResponseDto =
        unwrap(api.createBarang(request))

    suspend fun updateBarang(id: String, request: BarangUpdateRequestDto): BarangDto =
        unwrap(api.updateBarang(id, request)).barang

    suspend fun deleteBarang(id: String): BarangDeleteResponseDto = unwrap(api.deleteBarang(id))

    suspend fun listKategori(): List<KategoriDto> {
        val response: KategoriListResponseDto = unwrap(api.listKategori())
        return response.data
    }

    suspend fun createKategori(nama: String): KategoriDto =
        unwrap(api.createKategori(KategoriRequestDto(id = UUID.randomUUID().toString(), nama = nama))).kategori

    suspend fun updateKategori(id: String, nama: String): KategoriDto =
        unwrap(api.updateKategori(id, KategoriRequestDto(nama = nama))).kategori

    suspend fun deleteKategori(id: String) {
        unwrapEmpty(api.deleteKategori(id))
    }

    suspend fun listSupplier(): List<SupplierDto> {
        val result = mutableListOf<SupplierDto>()
        var cursor: String? = null
        do {
            val page: SupplierListResponseDto = unwrap(api.listSupplier(cursor = cursor))
            result += page.data
            cursor = page.nextCursor
        } while (cursor != null)
        return result
    }

    suspend fun detailSupplier(id: String): SupplierDetailResponseDto = unwrap(api.detailSupplier(id))

    suspend fun createSupplier(nama: String, link: String?, catatan: String?): SupplierDto =
        unwrap(
            api.createSupplier(
                SupplierRequestDto(
                    id = UUID.randomUUID().toString(),
                    nama = nama,
                    link = link,
                    catatan = catatan,
                ),
            ),
        ).supplier

    suspend fun updateSupplier(id: String, nama: String, link: String?, catatan: String?): SupplierDto =
        unwrap(api.updateSupplier(id, SupplierRequestDto(nama = nama, link = link, catatan = catatan))).supplier

    suspend fun setSupplierStatus(id: String, status: String): SupplierDto =
        unwrap(api.setSupplierStatus(id, SupplierStatusRequestDto(status))).supplier

    suspend fun deleteSupplier(id: String) {
        unwrapEmpty(api.deleteSupplier(id))
    }

    suspend fun uploadFile(name: String, mimeType: String, bytes: ByteArray): FileUploadResponseDto {
        val body = bytes.toRequestBody(mimeType.toMediaType())
        val part = MultipartBody.Part.createFormData("file", name, body)
        return unwrap(api.uploadFile(part))
    }

    suspend fun downloadFile(fileId: String): ByteArray =
        unwrap(api.downloadFile(fileId)).use { it.bytes() }

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

    private fun unwrapEmpty(response: Response<Unit>) {
        if (response.isSuccessful) return
        throw ApiErrorParser.parse(response.code(), response.errorBody()?.string())
    }

}
