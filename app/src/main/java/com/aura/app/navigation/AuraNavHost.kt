package com.aura.app.navigation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.aura.app.R
import com.aura.app.core.access.AksiAkses
import com.aura.app.core.access.AksesPeran
import com.aura.app.core.model.Peran
import com.aura.app.data.network.KoneksiChecker
import com.aura.app.data.remote.GlobalSessionHandler
import com.aura.app.data.repository.AkunRepository
import com.aura.app.data.repository.AnalisisRepository
import com.aura.app.data.repository.BarangRepository
import com.aura.app.data.repository.AuthRepository
import com.aura.app.data.repository.KeuanganRepository
import com.aura.app.data.repository.PenjualanRepository
import com.aura.app.ui.AuraViewModel
import com.aura.app.ui.barang.KatalogEvent
import com.aura.app.ui.barang.KatalogViewModel
import com.aura.app.ui.auth.AuthSessionEvent
import com.aura.app.ui.auth.AuthSessionViewModel
import com.aura.app.ui.auth.LoginEvent
import com.aura.app.ui.auth.LoginViewModel
import com.aura.app.ui.auth.RegisterEvent
import com.aura.app.ui.auth.RegisterViewModel
import com.aura.app.ui.auth.UbahKataSandiEvent
import com.aura.app.ui.auth.UbahKataSandiViewModel
import com.aura.app.ui.kasir.KasirEvent
import com.aura.app.ui.kasir.KasirViewModel
import com.aura.app.ui.keuangan.KeuanganEvent
import com.aura.app.ui.keuangan.KeuanganViewModel
import com.aura.app.ui.components.AvatarSheet
import com.aura.app.ui.components.GantiAkunSheet
import com.aura.app.ui.components.LogoutConfirmationDialog
import com.aura.app.ui.profile.EditProfilEvent
import com.aura.app.ui.profile.ManajemenKasirViewModel
import com.aura.app.ui.profile.ProfilViewModel
import com.aura.app.ui.analisis.AnalisisViewModel
import com.aura.app.ui.screens.analisis.AnalysisScreen
import com.aura.app.ui.screens.analisis.CorrelationScreen
import com.aura.app.ui.screens.analisis.MovementListScreen
import com.aura.app.ui.screens.analisis.ProportionListScreen
import com.aura.app.ui.screens.analisis.StockRunwayScreen
import com.aura.app.ui.screens.analisis.TrendListScreen
import com.aura.app.ui.screens.auth.LoginScreen
import com.aura.app.ui.screens.auth.RegisterOneScreen
import com.aura.app.ui.screens.auth.RegisterTwoScreen
import com.aura.app.ui.screens.auth.UbahKataSandiScreen
import com.aura.app.ui.screens.auth.WelcomeScreen
import com.aura.app.ui.screens.barang.AdjustmentScreen
import com.aura.app.ui.screens.barang.CategoryManagementScreen
import com.aura.app.ui.screens.barang.MovementScreen
import com.aura.app.ui.screens.barang.ProductDetailScreen
import com.aura.app.ui.screens.barang.ProductFormScreen
import com.aura.app.ui.screens.barang.ProductListScreen
import com.aura.app.ui.screens.barang.RestockScreen
import com.aura.app.ui.screens.barang.SupplierScreen
import com.aura.app.ui.screens.barang.SupplierDetailScreen
import com.aura.app.ui.screens.dashboard.DashboardScreen
import com.aura.app.ui.screens.kasir.CartScreen
import com.aura.app.ui.screens.kasir.CashierScreen
import com.aura.app.ui.screens.kasir.HistoryScreen
import com.aura.app.ui.screens.kasir.PaymentScreen
import com.aura.app.ui.screens.kasir.SuccessScreen
import com.aura.app.ui.screens.keuangan.ExpenseFormScreen
import com.aura.app.ui.screens.keuangan.ExpenseListScreen
import com.aura.app.ui.screens.keuangan.IncomeFormScreen
import com.aura.app.ui.screens.keuangan.IncomeListScreen
import com.aura.app.ui.screens.keuangan.ExpenseDetailScreen
import com.aura.app.ui.screens.keuangan.IncomeDetailScreen
import com.aura.app.ui.screens.manajemen.ManagementScreen
import com.aura.app.ui.screens.notifikasi.NotificationScreen
import com.aura.app.ui.screens.profile.ActivityScreen
import com.aura.app.ui.screens.profile.CashierAccountsScreen
import com.aura.app.ui.screens.profile.EditProfilScreen
import com.aura.app.ui.screens.profile.PaymentSettingsScreen
import com.aura.app.ui.screens.profile.PengaturanTokoScreen
import com.aura.app.ui.screens.profile.ProfileScreen
import com.aura.app.ui.screens.profile.SettingsScreen
import com.aura.app.ui.theme.Bg
import com.aura.app.ui.theme.Purple
import com.aura.app.ui.theme.Red
import com.aura.app.ui.theme.Soft
import kotlinx.coroutines.launch

private val ownerRoutes = setOf(
    Routes.DASHBOARD, Routes.MENU_MANAJEMEN, Routes.ANALISIS,
    Routes.ANALISIS_PERGERAKAN, Routes.ANALISIS_UMUR_STOK, Routes.ANALISIS_PROPORSI,
    Routes.ANALISIS_TREN, Routes.ANALISIS_KORELASI,
    Routes.PROFIL, Routes.NOTIFIKASI,
    Routes.PENGELUARAN, Routes.DETAIL_PENGELUARAN, Routes.CATAT_PENGELUARAN,
    Routes.PEMASUKAN, Routes.DETAIL_PEMASUKAN, Routes.CATAT_PEMASUKAN,
    Routes.MANAJEMEN_BARANG, Routes.PROFIL_BARANG, Routes.TAMBAH_BARANG, Routes.CATAT_RESTOCK,
    Routes.PENYESUAIAN_STOK, Routes.PERGERAKAN_STOK, Routes.SUPPLIER, Routes.PROFIL_SUPPLIER,
    Routes.KELOLA_KATEGORI, Routes.RIWAYAT_AKTIVITAS,
    Routes.PENGATURAN, Routes.MANAJEMEN_AKUN_KASIR, Routes.METODE_PEMBAYARAN, Routes.PENGATURAN_TOKO,
    Routes.K4_TRANSAKSI_BERHASIL_OWNER, Routes.RIWAYAT_HARI_INI_OWNER,
)

private val noChromeRoutes = setOf(
    Routes.WELCOME,
    Routes.LOGIN,
    Routes.REGISTER_STEP1,
    Routes.REGISTER_STEP2,
    Routes.UBAH_KATA_SANDI_WAJIB,
)

@Composable
private fun RouteNavHost(
    navController: NavHostController,
    content: @Composable (route: String, backStackEntry: NavBackStackEntry) -> Unit,
) {
    NavHost(navController = navController, startDestination = "auth_graph") {
        navigation(startDestination = Routes.WELCOME, route = "auth_graph") {
            composable(Routes.WELCOME) { content(Routes.WELCOME, it) }
            composable(Routes.LOGIN) { content(Routes.LOGIN, it) }
            composable(Routes.REGISTER_STEP1) { content(Routes.REGISTER_STEP1, it) }
            composable(Routes.REGISTER_STEP2) { content(Routes.REGISTER_STEP2, it) }
            composable(Routes.UBAH_KATA_SANDI_WAJIB) { content(Routes.UBAH_KATA_SANDI_WAJIB, it) }
        }
        navigation(startDestination = Routes.DASHBOARD, route = "owner_tab_graph") {
            composable(Routes.DASHBOARD) { content(Routes.DASHBOARD, it) }
            composable(Routes.MENU_MANAJEMEN) { content(Routes.MENU_MANAJEMEN, it) }
            composable(Routes.ANALISIS) { content(Routes.ANALISIS, it) }
            composable(Routes.ANALISIS_PERGERAKAN) { content(Routes.ANALISIS_PERGERAKAN, it) }
            composable(Routes.ANALISIS_UMUR_STOK) { content(Routes.ANALISIS_UMUR_STOK, it) }
            composable(Routes.ANALISIS_PROPORSI) { content(Routes.ANALISIS_PROPORSI, it) }
            composable(Routes.ANALISIS_TREN) { content(Routes.ANALISIS_TREN, it) }
            composable(Routes.ANALISIS_KORELASI) { content(Routes.ANALISIS_KORELASI, it) }
            composable(Routes.PROFIL) { content(Routes.PROFIL, it) }
        }
        navigation(startDestination = Routes.MANAJEMEN_BARANG, route = "barang_tab_graph") {
            composable(Routes.MANAJEMEN_BARANG) { content(Routes.MANAJEMEN_BARANG, it) }
            composable(Routes.PROFIL_BARANG) { content(Routes.PROFIL_BARANG, it) }
            composable(Routes.TAMBAH_BARANG) { content(Routes.TAMBAH_BARANG, it) }
            composable(Routes.CATAT_RESTOCK) { content(Routes.CATAT_RESTOCK, it) }
            composable(Routes.PENYESUAIAN_STOK) { content(Routes.PENYESUAIAN_STOK, it) }
            composable(Routes.PERGERAKAN_STOK) { content(Routes.PERGERAKAN_STOK, it) }
            composable(Routes.SUPPLIER) { content(Routes.SUPPLIER, it) }
            composable(Routes.PROFIL_SUPPLIER) { content(Routes.PROFIL_SUPPLIER, it) }
            composable(Routes.KELOLA_KATEGORI) { content(Routes.KELOLA_KATEGORI, it) }
        }
        navigation(startDestination = Routes.PENGELUARAN, route = "keuangan_tab_graph") {
            composable(Routes.PENGELUARAN) { content(Routes.PENGELUARAN, it) }
            composable(Routes.DETAIL_PENGELUARAN) { content(Routes.DETAIL_PENGELUARAN, it) }
            composable(Routes.CATAT_PENGELUARAN) { content(Routes.CATAT_PENGELUARAN, it) }
            composable(Routes.PEMASUKAN) { content(Routes.PEMASUKAN, it) }
            composable(Routes.DETAIL_PEMASUKAN) { content(Routes.DETAIL_PEMASUKAN, it) }
            composable(Routes.CATAT_PEMASUKAN) { content(Routes.CATAT_PEMASUKAN, it) }
        }
        navigation(startDestination = Routes.K1_BERANDA_KASIR, route = "kasir_tab_graph") {
            composable(Routes.K1_BERANDA_KASIR) { content(Routes.K1_BERANDA_KASIR, it) }
            composable(Routes.K2_CATAT_TRANSAKSI) { content(Routes.K2_CATAT_TRANSAKSI, it) }
            composable(Routes.K3_PEMBAYARAN) { content(Routes.K3_PEMBAYARAN, it) }
            composable(Routes.K4_TRANSAKSI_BERHASIL) { content(Routes.K4_TRANSAKSI_BERHASIL, it) }
            composable(Routes.K4_TRANSAKSI_BERHASIL_OWNER) { content(Routes.K4_TRANSAKSI_BERHASIL_OWNER, it) }
            composable(Routes.RIWAYAT_HARI_INI) { content(Routes.RIWAYAT_HARI_INI, it) }
            composable(Routes.RIWAYAT_HARI_INI_OWNER) { content(Routes.RIWAYAT_HARI_INI_OWNER, it) }
        }
        navigation(startDestination = Routes.PENGATURAN, route = "profil_tab_graph") {
            composable(Routes.NOTIFIKASI) { content(Routes.NOTIFIKASI, it) }
            composable(Routes.RIWAYAT_AKTIVITAS) { content(Routes.RIWAYAT_AKTIVITAS, it) }
            composable(Routes.PENGATURAN) { content(Routes.PENGATURAN, it) }
            composable(Routes.MANAJEMEN_AKUN_KASIR) { content(Routes.MANAJEMEN_AKUN_KASIR, it) }
            composable(Routes.METODE_PEMBAYARAN) { content(Routes.METODE_PEMBAYARAN, it) }
            composable(Routes.PENGATURAN_TOKO) { content(Routes.PENGATURAN_TOKO, it) }
            composable(Routes.EDIT_PROFIL) { content(Routes.EDIT_PROFIL, it) }
            composable(Routes.UBAH_KATA_SANDI) { content(Routes.UBAH_KATA_SANDI, it) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuraNavHost(
    authRepository: AuthRepository,
    akunRepository: AkunRepository,
    barangRepository: BarangRepository,
    penjualanRepository: PenjualanRepository,
    keuanganRepository: KeuanganRepository,
    analisisRepository: AnalisisRepository,
    globalSessionHandler: GlobalSessionHandler,
    koneksiChecker: KoneksiChecker,
    vm: AuraViewModel = viewModel(),
) {
    val context = LocalContext.current
    val authSessionVm: AuthSessionViewModel = viewModel(
        factory = AuthSessionViewModel.factory(authRepository, globalSessionHandler, koneksiChecker),
    )
    val sesiAktif by authSessionVm.sesiAktif.collectAsStateWithLifecycle()
    val riwayatAkun by authSessionVm.riwayatAkun.collectAsStateWithLifecycle()
    val startupChecked by authSessionVm.startupChecked.collectAsStateWithLifecycle()
    val gantiAkunState by authSessionVm.gantiAkunState.collectAsStateWithLifecycle()
    val kasirVm: KasirViewModel = viewModel(
        factory = KasirViewModel.factory(penjualanRepository, barangRepository),
    )
    val kasirState by kasirVm.uiState.collectAsStateWithLifecycle()
    val keuanganVm: KeuanganViewModel = viewModel(
        factory = KeuanganViewModel.factory(keuanganRepository, barangRepository),
    )
    val keuanganState by keuanganVm.uiState.collectAsStateWithLifecycle()
    val analisisVm: AnalisisViewModel = viewModel(
        factory = AnalisisViewModel.factory(analisisRepository),
    )
    val analisisState by analisisVm.uiState.collectAsStateWithLifecycle()

    val products by vm.products.collectAsStateWithLifecycle()
    val suppliers by vm.suppliers.collectAsStateWithLifecycle()
    val transactions by vm.transactions.collectAsStateWithLifecycle()
    val transactionItems by vm.transactionItems.collectAsStateWithLifecycle()
    val adjustments by vm.adjustments.collectAsStateWithLifecycle()
    val cashRecords by vm.cashRecords.collectAsStateWithLifecycle()
    val restocks by vm.restocks.collectAsStateWithLifecycle()
    val movements by vm.movements.collectAsStateWithLifecycle()
    val activities by vm.activities.collectAsStateWithLifecycle()
    val notifications by vm.notifications.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val ready by vm.ready.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val route = navController.currentBackStackEntryAsState().value?.destination?.route ?: Routes.WELCOME
    val stack = remember { mutableStateListOf<String>() }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var selectedProductId by rememberSaveable { mutableStateOf("") }
    var editProductId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedSupplierId by rememberSaveable { mutableStateOf("") }
    var showAvatar by remember { mutableStateOf(false) }
    var showGantiAkun by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var cancelTransaksiId by remember { mutableStateOf<String?>(null) }
    var isOwnerCancel by remember { mutableStateOf(true) }
    var cancelReason by rememberSaveable { mutableStateOf("") }
    var selectedExpenseId by rememberSaveable { mutableStateOf("") }
    var selectedIncomeId by rememberSaveable { mutableStateOf("") }
    var hideBalance by rememberSaveable { mutableStateOf(true) }
    var analisisMovementJenis by rememberSaveable { mutableStateOf("fast") }
    var analisisProporsiJenis by rememberSaveable { mutableStateOf("profit") }
    var analisisTrenArah by rememberSaveable { mutableStateOf("naik") }
    var search by rememberSaveable { mutableStateOf("") }

    val idTokoDisalinMsg = stringResource(R.string.profil_id_toko_disalin)
    val profilDisimpanMsg = stringResource(R.string.profil_simpan_berhasil)
    val sandiDiubahMsg = stringResource(R.string.ubah_sandi_berhasil)
    val kasirKembaliMsg = AksesPeran.ALASAN_KASIR_KEMBALI

    fun showSnackbar(pesan: String) {
        coroutineScope.launch { snackbarHostState.showSnackbar(pesan) }
    }

    fun root(to: String) {
        stack.clear()
        navController.navigate(to) {
            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
            launchSingleTop = true
        }
    }

    fun go(to: String) {
        if (sesiAktif?.peran == Peran.KASIR && to in ownerRoutes) {
            showSnackbar(AksesPeran.ALASAN_OWNER)
        } else {
            stack.add(route)
            navController.navigate(to) { launchSingleTop = true }
        }
    }

    val navigateToRoute by rememberUpdatedState<(String) -> Unit> { target -> go(target) }

    fun back() {
        if (route == Routes.UBAH_KATA_SANDI_WAJIB) {
            return
        }
        if (route == Routes.K1_BERANDA_KASIR && sesiAktif?.peran == Peran.KASIR) {
            showSnackbar(kasirKembaliMsg)
            return
        }
        if (stack.isNotEmpty()) {
            val target = stack.removeAt(stack.lastIndex)
            navController.popBackStack(target, inclusive = false)
        } else if (sesiAktif == null && route != Routes.WELCOME) {
            root(Routes.WELCOME)
        }
    }

    fun arahkanSetelahLogin(peran: Peran, wajibGantiSandi: Boolean) {
        when {
            wajibGantiSandi -> root(Routes.UBAH_KATA_SANDI_WAJIB)
            peran == Peran.OWNER -> root(Routes.DASHBOARD)
            else -> root(Routes.K1_BERANDA_KASIR)
        }
    }

    LaunchedEffect(sesiAktif?.akunId, sesiAktif?.idToko) {
        kasirVm.sambungkanSesi(sesiAktif)
        keuanganVm.sambungkanSesi(sesiAktif)
        analisisVm.sambungkanSesi(sesiAktif?.idToko)
    }

    LaunchedEffect(route, sesiAktif?.idToko) {
        when (route) {
            Routes.K3_PEMBAYARAN -> kasirVm.muatMetodePembayaran()
            Routes.RIWAYAT_HARI_INI, Routes.RIWAYAT_HARI_INI_OWNER -> kasirVm.muatRiwayatHariIni()
        }
    }

    LaunchedEffect(kasirVm) {
        kasirVm.events.collect { event ->
            when (event) {
                is KasirEvent.TampilkanPesan -> showSnackbar(
                    event.pesan.detail ?: context.getString(event.pesan.fallback),
                )
                is KasirEvent.TransaksiBerhasil -> navigateToRoute(
                    if (event.olehOwner) {
                        Routes.K4_TRANSAKSI_BERHASIL_OWNER
                    } else {
                        Routes.K4_TRANSAKSI_BERHASIL
                    },
                )
                is KasirEvent.PembatalanBerhasil -> {
                    cancelTransaksiId = null
                    cancelReason = ""
                    showSnackbar(
                        context.getString(
                            if (event.olehOwner) {
                                R.string.kasir_transaction_cancelled
                            } else {
                                R.string.kasir_cancel_request_sent
                            },
                        ),
                    )
                }
            }
        }
    }

    LaunchedEffect(keuanganVm) {
        keuanganVm.events.collect { event ->
            when (event) {
                KeuanganEvent.FormTersimpan -> back()
                KeuanganEvent.PembatalanBerhasil -> back()
                is KeuanganEvent.TampilkanPesan -> showSnackbar(event.teks)
            }
        }
    }

    LaunchedEffect(route, sesiAktif?.idToko) {
        if (route in setOf(
                Routes.PENGELUARAN,
                Routes.PEMASUKAN,
                Routes.METODE_PEMBAYARAN,
                Routes.PENGATURAN_TOKO,
            )
        ) {
            keuanganVm.refresh()
        }
    }

    LaunchedEffect(Unit) {
        authSessionVm.events.collect { event ->
            when (event) {
                is AuthSessionEvent.PaksaKeLogin -> {
                    vm.clearCart()
                    showAvatar = false
                    showGantiAkun = false
                    showLogoutConfirm = false
                    root(Routes.LOGIN)
                    showSnackbar(event.pesan)
                }

                is AuthSessionEvent.GantiAkunBerhasil -> {
                    vm.clearCart()
                    showGantiAkun = false
                    showAvatar = false
                    arahkanSetelahLogin(event.sesi.peran, event.sesi.wajibGantiSandi)
                }

                AuthSessionEvent.LogoutBerhasil -> {
                    vm.clearCart()
                    showAvatar = false
                    showGantiAkun = false
                    showLogoutConfirm = false
                    root(Routes.WELCOME)
                }
            }
        }
    }

    LaunchedEffect(startupChecked, sesiAktif?.akunId, sesiAktif?.wajibGantiSandi) {
        val current = sesiAktif
        if (startupChecked && current != null && route in listOf(Routes.WELCOME, Routes.LOGIN, Routes.REGISTER_STEP1, Routes.REGISTER_STEP2)) {
            arahkanSetelahLogin(current.peran, current.wajibGantiSandi)
        }
    }

    BackHandler(enabled = route != Routes.WELCOME) { back() }

    Scaffold(
        containerColor = Bg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (route !in noChromeRoutes) {
                TopAppBar(
                    title = {
                        Column {
                            Text("AURA", fontSize = 10.sp, letterSpacing = 2.sp, color = Red, fontWeight = FontWeight.ExtraBold)
                            Text(routeTitle(route), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    navigationIcon = {
                        if (stack.isNotEmpty() && !(route == Routes.K1_BERANDA_KASIR && sesiAktif?.peran == Peran.KASIR)) {
                            Text("‹", Modifier.clickable { back() }.padding(horizontal = 16.dp), fontSize = 30.sp, color = Red)
                        }
                    },
                    actions = {
                        val roleChip = if (sesiAktif?.peran == Peran.OWNER) "OWNER" else "KASIR"
                        if (route in listOf(
                                Routes.K1_BERANDA_KASIR, Routes.K2_CATAT_TRANSAKSI, Routes.K3_PEMBAYARAN,
                                Routes.K4_TRANSAKSI_BERHASIL, Routes.K4_TRANSAKSI_BERHASIL_OWNER,
                                Routes.RIWAYAT_HARI_INI, Routes.RIWAYAT_HARI_INI_OWNER,
                            )
                        ) {
                            Text(
                                roleChip,
                                Modifier.padding(horizontal = 6.dp).clip(CircleShape).background(Soft).padding(horizontal = 8.dp, vertical = 6.dp),
                                fontSize = 9.sp,
                                color = Purple,
                                fontWeight = FontWeight.Bold,
                            )
                            Text("●", Modifier.clickable { showAvatar = true }.padding(horizontal = 12.dp), color = Purple, fontSize = 20.sp)
                        } else if (sesiAktif != null) {
                            Text(
                                roleChip,
                                Modifier.padding(horizontal = 12.dp).clip(CircleShape).background(Soft).padding(horizontal = 10.dp, vertical = 6.dp),
                                fontSize = 9.sp,
                                color = Purple,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                )
            }
        },
        bottomBar = {
            if (sesiAktif?.peran == Peran.OWNER && route in listOf(Routes.DASHBOARD, Routes.MENU_MANAJEMEN, Routes.ANALISIS, Routes.PROFIL)) {
                AuraBottomBar(route) { target, isRoot -> if (isRoot) root(target) else go(target) }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            RouteNavHost(navController) { activeRoute, backStackEntry ->
                if (!ready || !startupChecked) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center), color = Red)
                } else when (activeRoute) {
                    Routes.WELCOME -> WelcomeScreen({ go(Routes.LOGIN) }, { go(Routes.REGISTER_STEP1) })

                    Routes.LOGIN -> {
                        val loginVm: LoginViewModel = viewModel(
                            backStackEntry,
                            factory = LoginViewModel.factory(authRepository, koneksiChecker),
                        )
                        val loginState by loginVm.uiState.collectAsStateWithLifecycle()
                        LaunchedEffect(loginVm) {
                            loginVm.refreshKoneksi()
                            loginVm.events.collect { event ->
                                when (event) {
                                    LoginEvent.MasukKeDashboard -> root(Routes.DASHBOARD)
                                    LoginEvent.MasukKeKasir -> root(Routes.K1_BERANDA_KASIR)
                                    LoginEvent.WajibGantiSandi -> root(Routes.UBAH_KATA_SANDI_WAJIB)
                                }
                            }
                        }
                        LoginScreen(
                            uiState = loginState,
                            onPilihPeran = loginVm::pilihPeran,
                            onEmailChange = loginVm::ubahEmail,
                            onIdTokoChange = loginVm::ubahIdToko,
                            onNamaAkunChange = loginVm::ubahNamaAkun,
                            onKataSandiChange = loginVm::ubahKataSandi,
                            onMasuk = loginVm::masuk,
                            onRegister = { go(Routes.REGISTER_STEP1) },
                        )
                    }

                    Routes.REGISTER_STEP1 -> {
                        val authEntry = remember(backStackEntry) { navController.getBackStackEntry("auth_graph") }
                        val registerVm: RegisterViewModel = viewModel(
                            authEntry,
                            factory = RegisterViewModel.factory(authRepository, koneksiChecker),
                        )
                        val regState by registerVm.uiState.collectAsStateWithLifecycle()
                        RegisterOneScreen(
                            uiState = regState,
                            onNamaChange = registerVm::ubahNamaLengkap,
                            onEmailChange = registerVm::ubahEmail,
                            onNomorHpChange = registerVm::ubahNomorHp,
                            onKataSandiChange = registerVm::ubahKataSandi,
                            onLanjut = { go(Routes.REGISTER_STEP2) },
                            onKembali = { back() },
                        )
                    }

                    Routes.REGISTER_STEP2 -> {
                        val authEntry = remember(backStackEntry) { navController.getBackStackEntry("auth_graph") }
                        val registerVm: RegisterViewModel = viewModel(
                            authEntry,
                            factory = RegisterViewModel.factory(authRepository, koneksiChecker),
                        )
                        val regState by registerVm.uiState.collectAsStateWithLifecycle()
                        LaunchedEffect(registerVm) {
                            registerVm.refreshKoneksi()
                            registerVm.events.collect { event ->
                                if (event is RegisterEvent.RegistrasiBerhasil) {
                                    root(Routes.DASHBOARD)
                                    event.pesanPeringatanProfil?.let { warning ->
                                        showSnackbar(
                                            context.getString(R.string.auth_register_profile_partial_warning, warning),
                                        )
                                    }
                                }
                            }
                        }
                        RegisterTwoScreen(
                            uiState = regState,
                            onNamaTokoChange = registerVm::ubahNamaToko,
                            onJenisUsahaChange = registerVm::ubahJenisUsaha,
                            onSaldoAwalChange = registerVm::ubahSaldoAwal,
                            onSelesai = registerVm::selesaikanRegistrasi,
                            onKembali = { back() },
                        )
                    }

                    Routes.UBAH_KATA_SANDI_WAJIB, Routes.UBAH_KATA_SANDI -> {
                        val wajib = activeRoute == Routes.UBAH_KATA_SANDI_WAJIB
                        val sandiVm: UbahKataSandiViewModel = viewModel(
                            backStackEntry,
                            factory = UbahKataSandiViewModel.factory(authRepository, koneksiChecker, wajib),
                        )
                        val sandiState by sandiVm.uiState.collectAsStateWithLifecycle()
                        LaunchedEffect(sandiVm) {
                            sandiVm.refreshKoneksi()
                            sandiVm.events.collect { event ->
                                if (event is UbahKataSandiEvent.Berhasil) {
                                    showSnackbar(sandiDiubahMsg)
                                    if (event.wajibSebelumnya) {
                                        val peran = sesiAktif?.peran ?: Peran.KASIR
                                        arahkanSetelahLogin(peran, wajibGantiSandi = false)
                                    } else {
                                        back()
                                    }
                                }
                            }
                        }
                        UbahKataSandiScreen(
                            uiState = sandiState,
                            onKataSandiSaatIniChange = sandiVm::ubahKataSandiSaatIni,
                            onKataSandiBaruChange = sandiVm::ubahKataSandiBaru,
                            onKonfirmasiChange = sandiVm::ubahKonfirmasiKataSandi,
                            onSimpan = sandiVm::simpan,
                            onBatalAtauKeluar = {
                                if (sandiState.wajibGantiSandi) showLogoutConfirm = true else back()
                            },
                        )
                    }

                    Routes.PROFIL -> ProfileScreen(
                        sesi = sesiAktif,
                        onEditProfil = { go(Routes.EDIT_PROFIL) },
                        onPengaturan = { go(Routes.PENGATURAN) },
                        onGantiAkun = { showGantiAkun = true },
                        onKeluar = { showLogoutConfirm = true },
                        onSalinIdToko = { showSnackbar(idTokoDisalinMsg) },
                    )

                    Routes.EDIT_PROFIL -> {
                        val profilVm: ProfilViewModel = viewModel(
                            backStackEntry,
                            factory = ProfilViewModel.factory(akunRepository, koneksiChecker),
                        )
                        val profilState by profilVm.uiState.collectAsStateWithLifecycle()
                        LaunchedEffect(sesiAktif?.akunId) {
                            sesiAktif?.let { profilVm.muatDariSesi(it) }
                        }
                        LaunchedEffect(profilVm) {
                            profilVm.events.collect { event ->
                                if (event is EditProfilEvent.SimpanBerhasil) {
                                    showSnackbar(profilDisimpanMsg)
                                    back()
                                }
                            }
                        }
                        EditProfilScreen(
                            uiState = profilState,
                            onNamaChange = profilVm::ubahNama,
                            onEmailChange = profilVm::ubahEmail,
                            onNomorHpChange = profilVm::ubahNomorHp,
                            onSimpan = profilVm::simpan,
                            onKembali = { back() },
                        )
                    }

                    Routes.PENGATURAN -> SettingsScreen(
                        onUbahSandi = { go(Routes.UBAH_KATA_SANDI) },
                        onAkunKasir = { go(Routes.MANAJEMEN_AKUN_KASIR) },
                        onMetodePembayaran = { go(Routes.METODE_PEMBAYARAN) },
                        onPengaturanToko = { go(Routes.PENGATURAN_TOKO) },
                    )

                    Routes.MANAJEMEN_AKUN_KASIR -> {
                        val kasirVm: ManajemenKasirViewModel = viewModel(
                            backStackEntry,
                            factory = ManajemenKasirViewModel.factory(akunRepository, koneksiChecker),
                        )
                        val kasirState by kasirVm.uiState.collectAsStateWithLifecycle()
                        LaunchedEffect(sesiAktif?.idToko) {
                            sesiAktif?.idToko?.let { kasirVm.muat(it) }
                        }
                        CashierAccountsScreen(
                            uiState = kasirState,
                            onBuatSandiOtomatis = kasirVm::buatSandiOtomatis,
                            onTambahKasir = kasirVm::tambahKasir,
                            onUbahStatus = kasirVm::ubahStatus,
                            onResetSandi = kasirVm::resetKataSandi,
                            onHapusKasir = kasirVm::hapusKasir,
                            onRefresh = kasirVm::refresh,
                            onTutupInfoLogin = kasirVm::tutupDialogInfoLogin,
                            onPesan = { showSnackbar(it) },
                        )
                    }

                    Routes.DASHBOARD -> DashboardScreen(keuanganState.toko, products, transactions, cashRecords, hideBalance, { hideBalance = !hideBalance }, { go(it) })
                    Routes.MENU_MANAJEMEN -> ManagementScreen(go = {
                        if (it == Routes.PERGERAKAN_STOK || it == Routes.CATAT_RESTOCK) selectedProductId = ""
                        go(it)
                    })
                    Routes.ANALISIS -> AnalysisScreen(
                        uiState = analisisState,
                        onSelectPeriode = { analisisVm.setPeriode(it) },
                        onRefresh = { analisisVm.muatOverview(refreshing = true) },
                        onToggleProporsi = analisisVm::setProporsiTab,
                        onOpenMovement = { jenis ->
                            analisisMovementJenis = jenis
                            go(Routes.ANALISIS_PERGERAKAN)
                        },
                        onOpenRunway = { go(Routes.ANALISIS_UMUR_STOK) },
                        onOpenProportion = { jenis ->
                            analisisProporsiJenis = jenis
                            go(Routes.ANALISIS_PROPORSI)
                        },
                        onOpenTrend = { arah ->
                            analisisTrenArah = arah
                            go(Routes.ANALISIS_TREN)
                        },
                        onOpenCorrelation = { go(Routes.ANALISIS_KORELASI) },
                    )
                    Routes.ANALISIS_PERGERAKAN -> MovementListScreen(
                        jenis = analisisMovementJenis,
                        uiState = analisisState,
                        onSelectPeriode = { p -> analisisVm.setPeriode(p) { analisisVm.muatPergerakan(analisisMovementJenis) } },
                        onSortChange = analisisVm::setMovementSort,
                        onLoad = { analisisVm.muatPergerakan(analisisMovementJenis) },
                        onOpenProduct = { productId ->
                            selectedProductId = productId
                            go(Routes.PROFIL_BARANG)
                        },
                    )
                    Routes.ANALISIS_UMUR_STOK -> StockRunwayScreen(
                        uiState = analisisState,
                        onSelectPeriode = { p -> analisisVm.setPeriode(p) { analisisVm.muatUmurStok() } },
                        onLoad = { analisisVm.muatUmurStok() },
                        onOpenProduct = { productId ->
                            selectedProductId = productId
                            go(Routes.PROFIL_BARANG)
                        },
                    )
                    Routes.ANALISIS_PROPORSI -> ProportionListScreen(
                        jenis = analisisProporsiJenis,
                        uiState = analisisState,
                        onSelectPeriode = { p -> analisisVm.setPeriode(p) { analisisVm.muatProporsi(analisisProporsiJenis) } },
                        onLoad = { analisisVm.muatProporsi(analisisProporsiJenis) },
                        onOpenProduct = { productId ->
                            selectedProductId = productId
                            go(Routes.PROFIL_BARANG)
                        },
                    )
                    Routes.ANALISIS_TREN -> TrendListScreen(
                        arah = analisisTrenArah,
                        uiState = analisisState,
                        onSelectPeriode = { p -> analisisVm.setPeriode(p) { analisisVm.muatTren(analisisTrenArah) } },
                        onSwitchArah = { arah -> analisisTrenArah = arah },
                        onLoad = { analisisVm.muatTren(analisisTrenArah) },
                        onOpenProduct = { productId ->
                            selectedProductId = productId
                            go(Routes.PROFIL_BARANG)
                        },
                    )
                    Routes.ANALISIS_KORELASI -> CorrelationScreen(
                        uiState = analisisState,
                        onSelectPeriode = { p -> analisisVm.setPeriode(p) { analisisVm.muatKorelasi() } },
                        onToggleProduct = analisisVm::toggleCorrelation,
                        onLoad = { analisisVm.muatKorelasi() },
                    )
                    Routes.NOTIFIKASI -> NotificationScreen(notifications, { vm.readNotification(it) }, { vm.readAllNotifications() }, { go(it) })
                    Routes.PENGELUARAN -> ExpenseListScreen(
                        uiState = keuanganState,
                        onAdd = { go(Routes.CATAT_PENGELUARAN) },
                        onDetail = {
                            selectedExpenseId = it
                            go(Routes.DETAIL_PENGELUARAN)
                        },
                        onRefresh = keuanganVm::refresh,
                    )
                    Routes.DETAIL_PENGELUARAN -> {
                        LaunchedEffect(selectedExpenseId) {
                            if (selectedExpenseId.isNotBlank()) keuanganVm.muatDetailPengeluaran(selectedExpenseId)
                        }
                        ExpenseDetailScreen(
                            uiState = keuanganState,
                            onCancel = keuanganVm::batalkanPengeluaran,
                            onLoadFile = keuanganVm::muatFile,
                        )
                    }
                    Routes.CATAT_PENGELUARAN -> {
                        LaunchedEffect(backStackEntry) { keuanganVm.siapkanFormPengeluaran() }
                        ExpenseFormScreen(
                            uiState = keuanganState,
                            onTitleChange = keuanganVm::ubahJudulPengeluaran,
                            onTypeChange = keuanganVm::ubahJenisBiaya,
                            onAmountChange = keuanganVm::ubahNominalPengeluaran,
                            onNoteChange = keuanganVm::ubahCatatanPengeluaran,
                            onUploadEvidence = keuanganVm::unggahBukti,
                            onRemoveEvidence = keuanganVm::resetBuktiUnggahan,
                            onSave = keuanganVm::catatPengeluaran,
                            onBack = { back() },
                        )
                    }
                    Routes.PEMASUKAN -> IncomeListScreen(
                        uiState = keuanganState,
                        onAdd = { go(Routes.CATAT_PEMASUKAN) },
                        onDetail = { id, _ ->
                            selectedIncomeId = id
                            go(Routes.DETAIL_PEMASUKAN)
                        },
                        onRefresh = keuanganVm::refresh,
                    )
                    Routes.DETAIL_PEMASUKAN -> {
                        LaunchedEffect(selectedIncomeId) {
                            if (selectedIncomeId.isNotBlank()) keuanganVm.muatDetailPemasukan(selectedIncomeId)
                        }
                        IncomeDetailScreen(
                            uiState = keuanganState,
                            onCancel = keuanganVm::batalkanPemasukan,
                            onLoadFile = keuanganVm::muatFile,
                        )
                    }
                    Routes.CATAT_PEMASUKAN -> {
                        LaunchedEffect(backStackEntry) { keuanganVm.siapkanFormPemasukan() }
                        IncomeFormScreen(
                            uiState = keuanganState,
                            onTitleChange = keuanganVm::ubahJudulPemasukan,
                            onAmountChange = keuanganVm::ubahNominalPemasukan,
                            onNoteChange = keuanganVm::ubahCatatanPemasukan,
                            onUploadEvidence = keuanganVm::unggahBukti,
                            onRemoveEvidence = keuanganVm::resetBuktiUnggahan,
                            onSave = keuanganVm::catatPemasukan,
                            onBack = { back() },
                        )
                    }
                    Routes.MANAJEMEN_BARANG -> {
                        val katalogVm: KatalogViewModel = viewModel(
                            backStackEntry,
                            factory = KatalogViewModel.factory(barangRepository),
                        )
                        val katalogState by katalogVm.uiState.collectAsStateWithLifecycle()
                        LaunchedEffect(sesiAktif?.idToko) {
                            sesiAktif?.idToko?.let(katalogVm::muat)
                        }
                        LaunchedEffect(katalogVm) {
                            katalogVm.events.collect { event ->
                                if (event is KatalogEvent.Pesan) showSnackbar(event.teks)
                            }
                        }
                        ProductListScreen(
                            items = katalogState.barang,
                            search = katalogState.query,
                            setSearch = katalogVm::ubahPencarian,
                            detail = { selectedProductId = it; go(Routes.PROFIL_BARANG) },
                            edit = { editProductId = it; selectedProductId = it; go(Routes.TAMBAH_BARANG) },
                            archive = katalogVm::hapusBarang,
                            manageCategories = { go(Routes.KELOLA_KATEGORI) },
                            suppliers = { go(Routes.SUPPLIER) },
                            loading = katalogState.isLoading,
                            error = katalogState.errorMessage,
                            retry = katalogVm::refresh,
                            fileBytes = katalogState.fileBytes,
                            failedFileIds = katalogState.fileIdsGagalDimuat,
                            loadFile = katalogVm::muatFile,
                            add = { editProductId = null; go(Routes.TAMBAH_BARANG) },
                        )
                    }
                    Routes.PROFIL_BARANG -> {
                        val katalogVm: KatalogViewModel = viewModel(
                            backStackEntry,
                            factory = KatalogViewModel.factory(barangRepository),
                        )
                        val katalogState by katalogVm.uiState.collectAsStateWithLifecycle()
                        LaunchedEffect(sesiAktif?.idToko, selectedProductId) {
                            sesiAktif?.idToko?.let {
                                katalogVm.muat(it)
                                katalogVm.muatDetail(selectedProductId)
                            }
                        }
                        LaunchedEffect(katalogVm) {
                            katalogVm.events.collect { event ->
                                when (event) {
                                    is KatalogEvent.Pesan -> showSnackbar(event.teks)
                                    is KatalogEvent.BarangDihapus -> {
                                        showSnackbar(context.getString(R.string.katalog_delete_success, event.status))
                                        back()
                                    }
                                    else -> Unit
                                }
                            }
                        }
                        ProductDetailScreen(
                            item = katalogState.barang.firstOrNull { it.barang.id == selectedProductId },
                            hargaBeliSupplier = katalogState.detail?.hargaBeliSupplier.orEmpty(),
                            fileBytes = katalogState.fileBytes,
                            failedFileIds = katalogState.fileIdsGagalDimuat,
                            onLoadFile = katalogVm::muatFile,
                            restock = { go(Routes.CATAT_RESTOCK) },
                            edit = { editProductId = selectedProductId; go(Routes.TAMBAH_BARANG) },
                            adjust = { go(Routes.PENYESUAIAN_STOK) },
                            movement = { go(Routes.PERGERAKAN_STOK) },
                            archive = { katalogVm.hapusBarang(selectedProductId) },
                        )
                    }
                    Routes.TAMBAH_BARANG -> {
                        val katalogVm: KatalogViewModel = viewModel(
                            backStackEntry,
                            factory = KatalogViewModel.factory(barangRepository),
                        )
                        val katalogState by katalogVm.uiState.collectAsStateWithLifecycle()
                        LaunchedEffect(sesiAktif?.idToko, editProductId) {
                            sesiAktif?.idToko?.let { id ->
                                katalogVm.muat(id)
                                editProductId?.let(katalogVm::muatDetail)
                            }
                            katalogVm.resetFotoUnggahan()
                        }
                        LaunchedEffect(katalogVm) {
                            katalogVm.events.collect { event ->
                                when (event) {
                                    KatalogEvent.BarangTersimpan -> {
                                        showSnackbar(context.getString(R.string.katalog_saved))
                                        back()
                                    }
                                    is KatalogEvent.Pesan -> showSnackbar(event.teks)
                                    else -> Unit
                                }
                            }
                        }
                        ProductFormScreen(
                            barang = katalogState.barang.firstOrNull { it.barang.id == editProductId },
                            categories = katalogState.kategori,
                            suppliers = katalogState.supplier,
                            fileBytes = katalogState.fileBytes,
                            failedFileIds = katalogState.fileIdsGagalDimuat,
                            uploadedPhotoFileId = katalogState.fotoTerunggahFileId,
                            uploadingPhoto = katalogState.isUploadingFoto,
                            saving = katalogState.isSaving,
                            onUploadPhoto = katalogVm::unggahFoto,
                            onLoadFile = katalogVm::muatFile,
                            onCreateCategory = { katalogVm.simpanKategori(null, it) },
                            onManageCategories = { go(Routes.KELOLA_KATEGORI) },
                            save = { katalogVm.simpanBarang(it, editProductId) },
                            back = { back() },
                        )
                    }
                    Routes.KELOLA_KATEGORI -> {
                        val katalogVm: KatalogViewModel = viewModel(
                            backStackEntry,
                            factory = KatalogViewModel.factory(barangRepository),
                        )
                        val katalogState by katalogVm.uiState.collectAsStateWithLifecycle()
                        LaunchedEffect(sesiAktif?.idToko) {
                            sesiAktif?.idToko?.let(katalogVm::muat)
                        }
                        LaunchedEffect(katalogVm) {
                            katalogVm.events.collect { event ->
                                if (event is KatalogEvent.Pesan) showSnackbar(event.teks)
                            }
                        }
                        CategoryManagementScreen(
                            categories = katalogState.kategoriTampilan,
                            save = katalogVm::simpanKategori,
                            delete = katalogVm::hapusKategori,
                        )
                    }
                    Routes.CATAT_RESTOCK -> {
                        val katalogVm: KatalogViewModel = viewModel(
                            backStackEntry,
                            factory = KatalogViewModel.factory(barangRepository),
                        )
                        val katalogState by katalogVm.uiState.collectAsStateWithLifecycle()
                        LaunchedEffect(sesiAktif?.idToko) {
                            sesiAktif?.idToko?.let(katalogVm::muat)
                        }
                        LaunchedEffect(katalogVm) {
                            katalogVm.events.collect { event ->
                                when (event) {
                                    KatalogEvent.StokTersimpan -> {
                                        showSnackbar(context.getString(R.string.restock_saved))
                                        back()
                                    }
                                    is KatalogEvent.Pesan -> showSnackbar(event.teks)
                                    else -> Unit
                                }
                            }
                        }
                        RestockScreen(
                            products = katalogState.barang.filter { it.barang.status == com.aura.app.core.model.StatusBarang.AKTIF },
                            suppliers = katalogState.supplier,
                            initialProductId = selectedProductId,
                            saving = katalogState.isSaving,
                            creatingSupplier = katalogState.isSavingSupplier,
                            save = katalogVm::catatRestock,
                            createSupplier = { katalogVm.simpanSupplier(null, it, null, null) },
                            manageSuppliers = { go(Routes.SUPPLIER) },
                            back = { back() },
                        )
                    }
                    Routes.PENYESUAIAN_STOK -> {
                        val katalogVm: KatalogViewModel = viewModel(
                            backStackEntry,
                            factory = KatalogViewModel.factory(barangRepository),
                        )
                        val katalogState by katalogVm.uiState.collectAsStateWithLifecycle()
                        LaunchedEffect(sesiAktif?.idToko) {
                            sesiAktif?.idToko?.let(katalogVm::muat)
                        }
                        LaunchedEffect(katalogVm) {
                            katalogVm.events.collect { event ->
                                when (event) {
                                    KatalogEvent.StokTersimpan -> {
                                        showSnackbar(context.getString(R.string.adjustment_saved))
                                        back()
                                    }
                                    is KatalogEvent.Pesan -> showSnackbar(event.teks)
                                    else -> Unit
                                }
                            }
                        }
                        AdjustmentScreen(
                            products = katalogState.barang,
                            initialProductId = selectedProductId,
                            saving = katalogState.isSaving,
                            save = katalogVm::catatPenyesuaian,
                            back = { back() },
                        )
                    }
                    Routes.PERGERAKAN_STOK -> {
                        val katalogVm: KatalogViewModel = viewModel(
                            backStackEntry,
                            factory = KatalogViewModel.factory(barangRepository),
                        )
                        val katalogState by katalogVm.uiState.collectAsStateWithLifecycle()
                        LaunchedEffect(sesiAktif?.idToko, selectedProductId) {
                            sesiAktif?.idToko?.let {
                                katalogVm.muatPergerakan(it, selectedProductId.takeIf(String::isNotBlank))
                            }
                        }
                        MovementScreen(
                            items = katalogState.pergerakan.filter {
                                selectedProductId.isBlank() || it.barangId == selectedProductId
                            },
                            nextCursor = katalogState.pergerakanNextCursor,
                            loading = katalogState.isLoadingPergerakan,
                            loadingMore = katalogState.isLoadingMorePergerakan,
                            error = katalogState.errorMessage,
                            retry = {
                                sesiAktif?.idToko?.let {
                                    katalogVm.muatPergerakan(
                                        it,
                                        selectedProductId.takeIf(String::isNotBlank),
                                    )
                                }
                            },
                            loadMore = { katalogVm.muatPergerakanBerikutnya(selectedProductId) },
                        )
                    }
                    Routes.SUPPLIER -> {
                        val katalogVm: KatalogViewModel = viewModel(
                            backStackEntry,
                            factory = KatalogViewModel.factory(barangRepository),
                        )
                        val katalogState by katalogVm.uiState.collectAsStateWithLifecycle()
                        LaunchedEffect(sesiAktif?.idToko) {
                            sesiAktif?.idToko?.let(katalogVm::muat)
                        }
                        LaunchedEffect(katalogVm) {
                            katalogVm.events.collect { event ->
                                when (event) {
                                    is KatalogEvent.Pesan -> showSnackbar(event.teks)
                                    is KatalogEvent.SupplierBerhasilDihapus -> showSnackbar(
                                        context.getString(
                                            if (event.dihapusPermanen) R.string.katalog_supplier_deleted else R.string.katalog_supplier_deactivated,
                                        ),
                                    )
                                    else -> Unit
                                }
                            }
                        }
                        SupplierScreen(
                            items = katalogState.supplier,
                            save = katalogVm::simpanSupplier,
                            delete = katalogVm::hapusSupplier,
                            setStatus = katalogVm::ubahStatusSupplier,
                            detail = { selectedSupplierId = it; go(Routes.PROFIL_SUPPLIER) },
                            onMessage = ::showSnackbar,
                        )
                    }
                    Routes.PROFIL_SUPPLIER -> {
                        val katalogVm: KatalogViewModel = viewModel(
                            backStackEntry,
                            factory = KatalogViewModel.factory(barangRepository),
                        )
                        val katalogState by katalogVm.uiState.collectAsStateWithLifecycle()
                        LaunchedEffect(sesiAktif?.idToko, selectedSupplierId) {
                            sesiAktif?.idToko?.let {
                                katalogVm.muat(it)
                                katalogVm.muatDetailSupplier(selectedSupplierId)
                            }
                        }
                        LaunchedEffect(katalogVm) {
                            katalogVm.events.collect { event ->
                                when (event) {
                                    is KatalogEvent.Pesan -> showSnackbar(event.teks)
                                    is KatalogEvent.SupplierBerhasilDihapus -> {
                                        showSnackbar(context.getString(if (event.dihapusPermanen) R.string.katalog_supplier_deleted else R.string.katalog_supplier_deactivated))
                                        back()
                                    }
                                    else -> Unit
                                }
                            }
                        }
                        SupplierDetailScreen(
                            detail = katalogState.detailSupplier,
                            onDelete = katalogVm::hapusSupplier,
                            onMessage = ::showSnackbar,
                        )
                    }
                    Routes.RIWAYAT_AKTIVITAS -> ActivityScreen(activities)
                    Routes.METODE_PEMBAYARAN -> PaymentSettingsScreen(
                        uiState = keuanganState,
                        onRecipientChange = keuanganVm::ubahNamaPenerima,
                        onAccountInfoChange = keuanganVm::ubahInfoRekening,
                        onUploadQris = keuanganVm::unggahQris,
                        onRemoveQris = keuanganVm::hapusQris,
                        onSave = keuanganVm::simpanMetodePembayaran,
                        onLoadFile = keuanganVm::muatFile,
                    )
                    Routes.PENGATURAN_TOKO -> PengaturanTokoScreen(
                        uiState = keuanganState,
                        onNameChange = keuanganVm::ubahNamaToko,
                        onBusinessTypeChange = keuanganVm::ubahJenisUsaha,
                        onAddressChange = keuanganVm::ubahAlamatToko,
                        onOpeningBalanceChange = keuanganVm::ubahSaldoAwal,
                        onSaveStore = keuanganVm::simpanPengaturanToko,
                        onSaveOpeningBalance = keuanganVm::koreksiSaldoAwal,
                    )
                    Routes.K1_BERANDA_KASIR -> {
                        val katalogVm: KatalogViewModel = viewModel(
                            backStackEntry,
                            factory = KatalogViewModel.factory(barangRepository),
                        )
                        val katalogState by katalogVm.uiState.collectAsStateWithLifecycle()
                        LaunchedEffect(sesiAktif?.idToko) {
                            sesiAktif?.idToko?.let(katalogVm::muat)
                        }
                        LaunchedEffect(katalogVm) {
                            katalogVm.events.collect { event ->
                                if (event is KatalogEvent.Pesan) showSnackbar(event.teks)
                            }
                        }
                        CashierScreen(
                            items = katalogState.barang.filter { it.barang.status == com.aura.app.core.model.StatusBarang.AKTIF },
                            cartQuantities = kasirState.keranjang.associate { it.barangId to it.jumlah },
                            search = search,
                            setSearch = { search = it },
                            fileBytes = katalogState.fileBytes,
                            failedFileIds = katalogState.fileIdsGagalDimuat,
                            loadFile = katalogVm::muatFile,
                            add = kasirVm::tambahBarang,
                            decrement = { kasirVm.ubahJumlah(it.id, -1) },
                            cart = { go(Routes.K2_CATAT_TRANSAKSI) },
                            history = { go(Routes.RIWAYAT_HARI_INI) },
                            avatar = { showAvatar = true },
                        )
                    }
                    Routes.K2_CATAT_TRANSAKSI -> CartScreen(
                        cart = kasirState.keranjang,
                        change = kasirVm::ubahJumlah,
                        pay = { go(Routes.K3_PEMBAYARAN) },
                        back = { back() },
                    )
                    Routes.K3_PEMBAYARAN -> {
                        val qrisFileId = kasirState.metodePembayaran?.qrisFileId?.takeIf(String::isNotBlank)
                        PaymentScreen(
                            total = kasirState.keranjang.sumOf { it.hargaJual * it.jumlah },
                            metodePembayaran = kasirState.metodePembayaran,
                            isLoadingMetode = kasirState.isLoadingMetodePembayaran,
                            metodeError = kasirState.metodePembayaranError?.let {
                                it.detail ?: context.getString(it.fallback)
                            },
                            retryMetode = kasirVm::muatMetodePembayaran,
                            qrisBytes = kasirState.qrisBytes.takeIf {
                                kasirState.qrisFileIdLoaded == qrisFileId
                            },
                            qrisLoading = qrisFileId != null &&
                                kasirState.qrisFileIdLoading == qrisFileId,
                            qrisError = kasirState.qrisError?.takeIf {
                                kasirState.qrisFileIdFailed == qrisFileId
                            }?.let { it.detail ?: context.getString(it.fallback) },
                            loadQris = kasirVm::muatQris,
                            retryQris = {
                                qrisFileId?.let { kasirVm.muatQris(it, ulangi = true) }
                            },
                            isSaving = kasirState.isSavingTransaksi,
                            requestTertunda = kasirState.permintaanTransaksiTertunda,
                            checkoutError = kasirState.checkoutError?.let {
                                it.detail ?: context.getString(it.fallback)
                            },
                            checkout = kasirVm::selesaikanTransaksi,
                            copyInfoRekening = { info ->
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(
                                    ClipData.newPlainText(
                                        context.getString(R.string.kasir_transfer_info_title),
                                        info,
                                    ),
                                )
                                showSnackbar(context.getString(R.string.kasir_account_copied))
                            },
                            back = { back() },
                        )
                    }
                    Routes.K4_TRANSAKSI_BERHASIL, Routes.K4_TRANSAKSI_BERHASIL_OWNER -> SuccessScreen(
                        kasirState.invoiceTerakhir,
                        kasirState.totalTerakhir,
                        sesiAktif?.peran == Peran.OWNER,
                        { root(Routes.K1_BERANDA_KASIR) },
                        { go(if (sesiAktif?.peran == Peran.OWNER) Routes.RIWAYAT_HARI_INI_OWNER else Routes.RIWAYAT_HARI_INI) },
                        { root(Routes.DASHBOARD) },
                    )
                    Routes.RIWAYAT_HARI_INI, Routes.RIWAYAT_HARI_INI_OWNER -> HistoryScreen(
                        items = kasirState.transaksiHariIni,
                        isLoading = kasirState.isLoadingRiwayat,
                        errorMessage = kasirState.riwayatError?.let {
                            it.detail ?: context.getString(it.fallback)
                        },
                        retry = kasirVm::muatRiwayatHariIni,
                        openCancel = { transaksi ->
                            isOwnerCancel = sesiAktif?.peran == Peran.OWNER
                            cancelTransaksiId = transaksi.id
                            cancelReason = kasirState.pengajuanPembatalanTertunda[transaksi.id]?.alasan.orEmpty()
                        },
                        isCashier = sesiAktif?.peran == Peran.KASIR,
                        submittedCancellationIds = kasirState.pengajuanPembatalanTertunda
                            .filterValues { it.terkirim }
                            .keys,
                    )
                    else -> WelcomeScreen({ root(Routes.LOGIN) }, { root(Routes.REGISTER_STEP1) })
                }
                if (
                    busy || kasirState.isSavingTransaksi || kasirState.isSavingPembatalan ||
                    kasirState.isLoadingRiwayat || kasirState.isLoadingMetodePembayaran
                ) {
                    LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter), color = Red)
                }
            }
        }
    }

    if (showAvatar) {
        AvatarSheet(
            sesi = sesiAktif,
            onDismiss = { showAvatar = false },
            onCopyStoreId = { showSnackbar(idTokoDisalinMsg) },
            onEditProfil = { go(Routes.EDIT_PROFIL) },
            onUbahSandi = { go(Routes.UBAH_KATA_SANDI) },
            onBukaGantiAkun = { showGantiAkun = true },
            onMintaKeluar = { showLogoutConfirm = true },
        )
    }

    if (showGantiAkun) {
        GantiAkunSheet(
            riwayatAkun = riwayatAkun,
            activeAccountId = sesiAktif?.akunId,
            dialogState = gantiAkunState,
            onDismiss = { showGantiAkun = false },
            onSubmitPassword = authSessionVm::gantiAkun,
            onClearError = authSessionVm::clearGantiAkunError,
            onTambahAkunLain = {
                showGantiAkun = false
                root(Routes.LOGIN)
            },
        )
    }

    if (showLogoutConfirm) {
        LogoutConfirmationDialog(
            onConfirm = {
                showLogoutConfirm = false
                authSessionVm.logout()
            },
            onDismiss = { showLogoutConfirm = false },
        )
    }

    cancelTransaksiId?.let { transaksiId ->
        AlertDialog(
            onDismissRequest = {
                if (!kasirState.isSavingPembatalan) {
                    cancelTransaksiId = null
                    cancelReason = ""
                }
            },
            title = {
                Text(
                    stringResource(
                        if (isOwnerCancel) {
                            R.string.kasir_cancel_owner_title
                        } else {
                            R.string.kasir_cancel_cashier_title
                        },
                    ),
                )
            },
            text = {
                Column {
                    Text(
                        stringResource(
                            if (isOwnerCancel) {
                                R.string.kasir_cancel_owner_message
                            } else {
                                R.string.kasir_cancel_cashier_message
                            },
                        ),
                        fontSize = 12.sp,
                    )
                    OutlinedTextField(
                        value = cancelReason,
                        onValueChange = { cancelReason = it },
                        label = { Text(stringResource(R.string.kasir_cancel_reason_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !kasirState.isSavingPembatalan &&
                            (isOwnerCancel || transaksiId !in kasirState.pengajuanPembatalanTertunda),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = cancelReason.isNotBlank() && !kasirState.isSavingPembatalan,
                    onClick = {
                        kasirVm.batalkanAtauAjukan(
                            transaksiId = transaksiId,
                            alasan = cancelReason.trim(),
                            olehOwner = isOwnerCancel,
                        )
                    },
                ) {
                    Text(
                        stringResource(
                            if (isOwnerCancel) {
                                R.string.kasir_cancel_owner_confirm
                            } else {
                                R.string.kasir_cancel_cashier_confirm
                            },
                        ),
                        color = Red,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !kasirState.isSavingPembatalan,
                    onClick = {
                        cancelTransaksiId = null
                        cancelReason = ""
                    },
                ) {
                    Text(stringResource(R.string.kasir_cancel_dismiss))
                }
            },
        )
    }

    if (message != null) {
        AlertDialog(
            onDismissRequest = { vm.dismissMessage() },
            title = { Text("AURA") },
            text = { Text(message ?: "") },
            confirmButton = { TextButton(onClick = { vm.dismissMessage() }) { Text("OK", color = Red) } },
        )
    }
}

private fun routeTitle(route: String) = mapOf(
    Routes.DASHBOARD to "Dashboard",
    Routes.MENU_MANAJEMEN to "Menu Manajemen",
    Routes.ANALISIS to "Analisis Bisnis",
    Routes.ANALISIS_PERGERAKAN to "Pergerakan Barang",
    Routes.ANALISIS_UMUR_STOK to "Estimasi Umur Stok",
    Routes.ANALISIS_PROPORSI to "Proporsi Penjualan",
    Routes.ANALISIS_TREN to "Tren Penjualan",
    Routes.ANALISIS_KORELASI to "Korelasi Antar Produk",
    Routes.PROFIL to "Profil",
    Routes.EDIT_PROFIL to "Edit Profil",
    Routes.UBAH_KATA_SANDI to "Ubah Kata Sandi",
    Routes.NOTIFIKASI to "Notifikasi",
    Routes.PENGELUARAN to "Pengeluaran",
    Routes.DETAIL_PENGELUARAN to "Detail Pengeluaran",
    Routes.CATAT_PENGELUARAN to "Catat Pengeluaran",
    Routes.PEMASUKAN to "Pemasukan",
    Routes.DETAIL_PEMASUKAN to "Detail Pemasukan",
    Routes.CATAT_PEMASUKAN to "Catat Pemasukan",
    Routes.MANAJEMEN_BARANG to "Manajemen Barang",
    Routes.PROFIL_BARANG to "Profil Barang",
    Routes.TAMBAH_BARANG to "Tambah / Edit Barang",
    Routes.CATAT_RESTOCK to "Catat Restock",
    Routes.PENYESUAIAN_STOK to "Penyesuaian Stok",
    Routes.PERGERAKAN_STOK to "Pergerakan Stok",
    Routes.SUPPLIER to "Supplier",
    Routes.PROFIL_SUPPLIER to "Detail Supplier",
    Routes.KELOLA_KATEGORI to "Kelola Kategori",
    Routes.RIWAYAT_AKTIVITAS to "Riwayat Aktivitas",
    Routes.PENGATURAN to "Pengaturan",
    Routes.MANAJEMEN_AKUN_KASIR to "Akun Kasir",
    Routes.METODE_PEMBAYARAN to "Metode Pembayaran",
    Routes.PENGATURAN_TOKO to "Pengaturan Toko",
    Routes.K1_BERANDA_KASIR to "Beranda Kasir",
    Routes.K2_CATAT_TRANSAKSI to "Catat Transaksi",
    Routes.K3_PEMBAYARAN to "Pembayaran",
    Routes.K4_TRANSAKSI_BERHASIL to "Transaksi Berhasil",
    Routes.K4_TRANSAKSI_BERHASIL_OWNER to "Transaksi Berhasil",
    Routes.RIWAYAT_HARI_INI to "Riwayat Hari Ini",
    Routes.RIWAYAT_HARI_INI_OWNER to "Riwayat Hari Ini",
)[route] ?: "AURA"
