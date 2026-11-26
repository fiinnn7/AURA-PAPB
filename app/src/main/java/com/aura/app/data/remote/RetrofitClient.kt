package com.aura.app.data.remote

import com.aura.app.BuildConfig
import com.aura.app.data.remote.api.AuthApiService
import com.aura.app.data.remote.api.KeuanganApiService
import com.aura.app.data.remote.api.KatalogApiService
import com.aura.app.data.remote.api.PenjualanApiService
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Penyedia instance Retrofit dan API service menuju server AURA (pola Modul 8–9).
 * BASE_URL dibaca dari BuildConfig (K-27); tidak menggunakan logging interceptor agar token/sandi tidak tercatat.
 */
object RetrofitClient {
    fun createOkHttpClient(
        tokenProvider: () -> String?,
        onUnauthorized: () -> Unit,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(tokenProvider, onUnauthorized))
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun createRetrofit(
        baseUrl: String = BuildConfig.BASE_URL,
        tokenProvider: () -> String? = { null },
        onUnauthorized: () -> Unit = {},
    ): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(createOkHttpClient(tokenProvider, onUnauthorized))
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    fun createAuthApiService(
        baseUrl: String = BuildConfig.BASE_URL,
        tokenProvider: () -> String?,
        onUnauthorized: () -> Unit,
    ): AuthApiService = createRetrofit(baseUrl, tokenProvider, onUnauthorized)
        .create(AuthApiService::class.java)

    fun createKatalogApiService(
        baseUrl: String = BuildConfig.BASE_URL,
        tokenProvider: () -> String?,
        onUnauthorized: () -> Unit,
    ): KatalogApiService = createRetrofit(baseUrl, tokenProvider, onUnauthorized)
        .create(KatalogApiService::class.java)

    fun createKeuanganApiService(
        baseUrl: String = BuildConfig.BASE_URL,
        tokenProvider: () -> String?,
        onUnauthorized: () -> Unit,
    ): KeuanganApiService = createRetrofit(baseUrl, tokenProvider, onUnauthorized)
        .create(KeuanganApiService::class.java)

    fun createPenjualanApiService(
        baseUrl: String = BuildConfig.BASE_URL,
        tokenProvider: () -> String?,
        onUnauthorized: () -> Unit,
    ): PenjualanApiService = createRetrofit(baseUrl, tokenProvider, onUnauthorized)
        .create(PenjualanApiService::class.java)
}
