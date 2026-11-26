package com.aura.app.data.remote.api

import com.aura.app.data.remote.dto.BarangDetailResponseDto
import com.aura.app.data.remote.dto.BarangListResponseDto
import com.aura.app.data.remote.dto.BarangUpdateRequestDto
import com.aura.app.data.remote.dto.BarangWriteResponseDto
import com.aura.app.data.remote.dto.FileUploadResponseDto
import com.aura.app.data.remote.dto.KategoriEnvelopeDto
import com.aura.app.data.remote.dto.KategoriListResponseDto
import com.aura.app.data.remote.dto.KategoriRequestDto
import com.aura.app.data.remote.dto.RestockRequestDto
import com.aura.app.data.remote.dto.PenyesuaianStokRequestDto
import com.aura.app.data.remote.dto.RestockCreateResponseDto
import com.aura.app.data.remote.dto.PenyesuaianStokCreateResponseDto
import com.aura.app.data.remote.dto.PergerakanStokListResponseDto
import com.aura.app.data.remote.dto.SupplierDetailResponseDto
import com.aura.app.data.remote.dto.SupplierEnvelopeDto
import com.aura.app.data.remote.dto.SupplierListResponseDto
import com.aura.app.data.remote.dto.SupplierRequestDto
import com.aura.app.data.remote.dto.SupplierStatusRequestDto
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

interface KatalogApiService {
    @POST("restock")
    suspend fun createRestock(@Body request: RestockRequestDto): Response<RestockCreateResponseDto>

    @POST("penyesuaian-stok")
    suspend fun createAdjustment(@Body request: PenyesuaianStokRequestDto): Response<PenyesuaianStokCreateResponseDto>

    @GET("pergerakan-stok")
    suspend fun listMovements(
        @Query("dari") from: Long? = null,
        @Query("sampai") through: Long? = null,
        @Query("jenis") type: String? = null,
        @Query("barangId") barangId: String? = null,
        @Query("limit") limit: Int = 100,
        @Query("cursor") cursor: String? = null,
    ): Response<PergerakanStokListResponseDto>

    @GET("barang/{barangId}/kartu-stok")
    suspend fun stockCard(
        @Path("barangId") barangId: String,
        @Query("dari") from: Long? = null,
        @Query("sampai") through: Long? = null,
        @Query("limit") limit: Int = 100,
        @Query("cursor") cursor: String? = null,
    ): Response<PergerakanStokListResponseDto>

    @GET("barang")
    suspend fun listBarang(
        @Query("q") query: String? = null,
        @Query("kategoriId") kategoriId: String? = null,
        @Query("status") status: String = "AKTIF",
        @Query("sort") sort: String = "NAMA",
        @Query("limit") limit: Int = 100,
        @Query("cursor") cursor: String? = null,
    ): Response<BarangListResponseDto>

    @GET("barang/{barangId}")
    suspend fun detailBarang(@Path("barangId") barangId: String): Response<BarangDetailResponseDto>

    @POST("barang")
    suspend fun createBarang(@Body request: com.aura.app.data.remote.dto.BarangCreateRequestDto): Response<BarangWriteResponseDto>

    @PUT("barang/{barangId}")
    suspend fun updateBarang(
        @Path("barangId") barangId: String,
        @Body request: BarangUpdateRequestDto,
    ): Response<com.aura.app.data.remote.dto.BarangEnvelopeDto>

    @DELETE("barang/{barangId}")
    suspend fun deleteBarang(@Path("barangId") barangId: String): Response<com.aura.app.data.remote.dto.BarangDeleteResponseDto>

    @GET("kategori")
    suspend fun listKategori(): Response<KategoriListResponseDto>

    @POST("kategori")
    suspend fun createKategori(@Body request: KategoriRequestDto): Response<KategoriEnvelopeDto>

    @PUT("kategori/{kategoriId}")
    suspend fun updateKategori(
        @Path("kategoriId") kategoriId: String,
        @Body request: KategoriRequestDto,
    ): Response<KategoriEnvelopeDto>

    @DELETE("kategori/{kategoriId}")
    suspend fun deleteKategori(@Path("kategoriId") kategoriId: String): Response<Unit>

    @GET("supplier")
    suspend fun listSupplier(
        @Query("q") query: String? = null,
        @Query("status") status: String? = null,
        @Query("limit") limit: Int = 100,
        @Query("cursor") cursor: String? = null,
    ): Response<SupplierListResponseDto>

    @GET("supplier/{supplierId}")
    suspend fun detailSupplier(@Path("supplierId") supplierId: String): Response<SupplierDetailResponseDto>

    @POST("supplier")
    suspend fun createSupplier(@Body request: SupplierRequestDto): Response<SupplierEnvelopeDto>

    @PUT("supplier/{supplierId}")
    suspend fun updateSupplier(
        @Path("supplierId") supplierId: String,
        @Body request: SupplierRequestDto,
    ): Response<SupplierEnvelopeDto>

    @PATCH("supplier/{supplierId}/status")
    suspend fun setSupplierStatus(
        @Path("supplierId") supplierId: String,
        @Body request: SupplierStatusRequestDto,
    ): Response<SupplierEnvelopeDto>

    @DELETE("supplier/{supplierId}")
    suspend fun deleteSupplier(@Path("supplierId") supplierId: String): Response<Unit>

    @Multipart
    @POST("file")
    suspend fun uploadFile(@Part file: MultipartBody.Part): Response<FileUploadResponseDto>

    @GET("file/{fileId}")
    suspend fun downloadFile(@Path("fileId") fileId: String): Response<ResponseBody>
}
