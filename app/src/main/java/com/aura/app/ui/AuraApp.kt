package com.aura.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aura.app.data.local.DatabaseProvider
import com.aura.app.data.network.KoneksiChecker
import com.aura.app.data.preferences.SesiRepository
import com.aura.app.data.remote.GlobalSessionHandler
import com.aura.app.data.remote.RetrofitClient
import com.aura.app.data.remote.datasource.AuthRemoteDataSource
import com.aura.app.data.remote.datasource.KeuanganRemoteDataSource
import com.aura.app.data.remote.datasource.KatalogRemoteDataSource
import com.aura.app.data.remote.datasource.PenjualanRemoteDataSource
import com.aura.app.data.repository.AkunRepository
import com.aura.app.data.repository.AuthRepository
import com.aura.app.data.repository.BarangRepository
import com.aura.app.data.repository.DefaultPenjualanRepository
import com.aura.app.data.repository.DefaultBarangRepository
import com.aura.app.data.repository.DefaultAkunRepository
import com.aura.app.data.repository.DefaultAuthRepository
import com.aura.app.data.repository.DefaultKeuanganRepository
import com.aura.app.data.repository.KeuanganRepository
import com.aura.app.data.repository.PenjualanRepository
import com.aura.app.navigation.AuraNavHost

@Composable
fun AuraApp(vm: AuraViewModel = viewModel()) {
    val context = LocalContext.current.applicationContext
    val appDatabase = remember { DatabaseProvider.getDatabase(context) }
    val sesiRepository = remember { SesiRepository(context) }
    val koneksiChecker = remember { KoneksiChecker(context) }
    val globalSessionHandler = remember { GlobalSessionHandler(sesiRepository, appDatabase) }
    val authApiService = remember {
        RetrofitClient.createAuthApiService(
            tokenProvider = { sesiRepository.tokenTersimpanSinkron() },
            onUnauthorized = { globalSessionHandler.onUnauthorized() },
        )
    }
    val authRemoteDataSource = remember { AuthRemoteDataSource(authApiService) }
    val katalogApiService = remember {
        RetrofitClient.createKatalogApiService(
            tokenProvider = { sesiRepository.tokenTersimpanSinkron() },
            onUnauthorized = { globalSessionHandler.onUnauthorized() },
        )
    }
    val katalogRemoteDataSource = remember { KatalogRemoteDataSource(katalogApiService) }
    val penjualanApiService = remember {
        RetrofitClient.createPenjualanApiService(
            tokenProvider = { sesiRepository.tokenTersimpanSinkron() },
            onUnauthorized = { globalSessionHandler.onUnauthorized() },
        )
    }
    val penjualanRemoteDataSource = remember { PenjualanRemoteDataSource(penjualanApiService) }
    val keuanganApiService = remember {
        RetrofitClient.createKeuanganApiService(
            tokenProvider = { sesiRepository.tokenTersimpanSinkron() },
            onUnauthorized = { globalSessionHandler.onUnauthorized() },
        )
    }
    val keuanganRemoteDataSource = remember { KeuanganRemoteDataSource(keuanganApiService) }
    val authRepository: AuthRepository = remember {
        DefaultAuthRepository(appDatabase, authRemoteDataSource, sesiRepository, koneksiChecker)
    }
    val akunRepository: AkunRepository = remember {
        DefaultAkunRepository(appDatabase, authRemoteDataSource, sesiRepository, koneksiChecker)
    }
    val barangRepository: BarangRepository = remember {
        DefaultBarangRepository(appDatabase, katalogRemoteDataSource, koneksiChecker, sesiRepository)
    }
    val penjualanRepository: PenjualanRepository = remember {
        DefaultPenjualanRepository(appDatabase, penjualanRemoteDataSource, koneksiChecker, sesiRepository)
    }
    val keuanganRepository: KeuanganRepository = remember {
        DefaultKeuanganRepository(
            appDatabase,
            keuanganRemoteDataSource,
            authRemoteDataSource,
            barangRepository,
            penjualanRepository,
            koneksiChecker,
            sesiRepository,
        )
    }

    AuraNavHost(
        authRepository = authRepository,
        akunRepository = akunRepository,
        barangRepository = barangRepository,
        penjualanRepository = penjualanRepository,
        keuanganRepository = keuanganRepository,
        globalSessionHandler = globalSessionHandler,
        koneksiChecker = koneksiChecker,
        vm = vm,
    )
}
