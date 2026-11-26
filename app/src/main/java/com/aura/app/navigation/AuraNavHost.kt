package com.aura.app.navigation

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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
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
import com.aura.app.core.access.AksesPeran
import com.aura.app.core.model.Peran
import com.aura.app.data.network.KoneksiChecker
import com.aura.app.data.remote.GlobalSessionHandler
import com.aura.app.data.repository.AkunRepository
import com.aura.app.data.repository.AuthRepository
import com.aura.app.ui.AuraViewModel
import com.aura.app.ui.auth.AuthSessionEvent
import com.aura.app.ui.auth.AuthSessionViewModel
import com.aura.app.ui.auth.LoginEvent
import com.aura.app.ui.auth.LoginViewModel
import com.aura.app.ui.auth.RegisterEvent
import com.aura.app.ui.auth.RegisterViewModel
import com.aura.app.ui.auth.UbahKataSandiEvent
import com.aura.app.ui.auth.UbahKataSandiViewModel
import com.aura.app.ui.components.AvatarSheet
import com.aura.app.ui.components.GantiAkunSheet
import com.aura.app.ui.components.LogoutConfirmationDialog
import com.aura.app.ui.profile.EditProfilEvent
import com.aura.app.ui.profile.ManajemenKasirViewModel
import com.aura.app.ui.profile.ProfilViewModel
import com.aura.app.ui.screens.analisis.AnalysisScreen
import com.aura.app.ui.screens.auth.LoginScreen
import com.aura.app.ui.screens.auth.RegisterOneScreen
import com.aura.app.ui.screens.auth.RegisterTwoScreen
import com.aura.app.ui.screens.auth.UbahKataSandiScreen
import com.aura.app.ui.screens.auth.WelcomeScreen
import com.aura.app.ui.screens.barang.AdjustmentScreen
import com.aura.app.ui.screens.barang.MovementScreen
import com.aura.app.ui.screens.barang.ProductDetailScreen
import com.aura.app.ui.screens.barang.ProductFormScreen
import com.aura.app.ui.screens.barang.ProductListScreen
import com.aura.app.ui.screens.barang.RestockScreen
import com.aura.app.ui.screens.barang.SupplierScreen
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
import com.aura.app.ui.screens.manajemen.ManagementScreen
import com.aura.app.ui.screens.notifikasi.NotificationScreen
import com.aura.app.ui.screens.profile.ActivityScreen
import com.aura.app.ui.screens.profile.CashierAccountsScreen
import com.aura.app.ui.screens.profile.EditProfilScreen
import com.aura.app.ui.screens.profile.PaymentSettingsScreen
import com.aura.app.ui.screens.profile.ProfileScreen
import com.aura.app.ui.screens.profile.SettingsScreen
import com.aura.app.ui.theme.Bg
import com.aura.app.ui.theme.Purple
import com.aura.app.ui.theme.Red
import com.aura.app.ui.theme.Soft
import kotlinx.coroutines.launch

private val ownerRoutes = setOf(
    Routes.DASHBOARD, Routes.MENU_MANAJEMEN, Routes.ANALISIS, Routes.PROFIL, Routes.NOTIFIKASI,
    Routes.PENGELUARAN, Routes.CATAT_PENGELUARAN, Routes.PEMASUKAN, Routes.CATAT_PEMASUKAN,
    Routes.MANAJEMEN_BARANG, Routes.PROFIL_BARANG, Routes.TAMBAH_BARANG, Routes.CATAT_RESTOCK,
    Routes.PENYESUAIAN_STOK, Routes.PERGERAKAN_STOK, Routes.SUPPLIER, Routes.RIWAYAT_AKTIVITAS,
    Routes.PENGATURAN, Routes.MANAJEMEN_AKUN_KASIR, Routes.METODE_PEMBAYARAN,
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
        }
        navigation(startDestination = Routes.PENGELUARAN, route = "keuangan_tab_graph") {
            composable(Routes.PENGELUARAN) { content(Routes.PENGELUARAN, it) }
            composable(Routes.CATAT_PENGELUARAN) { content(Routes.CATAT_PENGELUARAN, it) }
            composable(Routes.PEMASUKAN) { content(Routes.PEMASUKAN, it) }
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
    globalSessionHandler: GlobalSessionHandler,
    koneksiChecker: KoneksiChecker,
    vm: AuraViewModel = viewModel(),
) {
    val resources = LocalResources.current
    val authSessionVm: AuthSessionViewModel = viewModel(
        factory = AuthSessionViewModel.factory(authRepository, globalSessionHandler, koneksiChecker),
    )
    val sesiAktif by authSessionVm.sesiAktif.collectAsStateWithLifecycle()
    val riwayatAkun by authSessionVm.riwayatAkun.collectAsStateWithLifecycle()
    val startupChecked by authSessionVm.startupChecked.collectAsStateWithLifecycle()
    val gantiAkunState by authSessionVm.gantiAkunState.collectAsStateWithLifecycle()

    val store by vm.store.collectAsStateWithLifecycle()
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
    val cart by vm.cart.collectAsStateWithLifecycle()
    val invoice by vm.lastInvoice.collectAsStateWithLifecycle()
    val lastTotal by vm.lastTotal.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val ready by vm.ready.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val route = navController.currentBackStackEntryAsState().value?.destination?.route ?: Routes.WELCOME
    val stack = remember { mutableStateListOf<String>() }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var selectedProductId by rememberSaveable { mutableStateOf("BRG-001") }
    var editProductId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedTransaction by rememberSaveable { mutableStateOf("") }
    var showAvatar by remember { mutableStateOf(false) }
    var showGantiAkun by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var cancelInvoice by remember { mutableStateOf<String?>(null) }
    var isOwnerCancel by remember { mutableStateOf(true) }
    var cancelReason by rememberSaveable { mutableStateOf("") }
    var hideBalance by rememberSaveable { mutableStateOf(true) }
    var analysisTab by rememberSaveable { mutableStateOf("Ringkasan") }
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
                                            resources.getString(R.string.auth_register_profile_partial_warning, warning),
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

                    Routes.DASHBOARD -> DashboardScreen(store, products, transactions, cashRecords, hideBalance, { hideBalance = !hideBalance }, { go(it) })
                    Routes.MENU_MANAJEMEN -> ManagementScreen(go = { go(it) })
                    Routes.ANALISIS -> AnalysisScreen(analysisTab, { analysisTab = it }, products, transactions, transactionItems, cashRecords, adjustments, restocks, movements)
                    Routes.NOTIFIKASI -> NotificationScreen(notifications, { vm.readNotification(it) }, { vm.readAllNotifications() }, { go(it) })
                    Routes.PENGELUARAN -> ExpenseListScreen(
                        records = cashRecords.filter { it.kind == "expense" },
                        restocks = restocks,
                        products = products,
                        add = { go(Routes.CATAT_PENGELUARAN) },
                    )
                    Routes.CATAT_PENGELUARAN -> ExpenseFormScreen(
                        save = { category, amount, note, prive -> vm.expense(category, amount, note, prive); back() },
                        back = { back() },
                    )
                    Routes.PEMASUKAN -> IncomeListScreen(
                        transactions = transactions,
                        records = cashRecords.filter { it.kind == "income" },
                        add = { go(Routes.CATAT_PEMASUKAN) },
                    )
                    Routes.CATAT_PEMASUKAN -> IncomeFormScreen(
                        save = { category, amount, note, capital -> vm.income(category, amount, note, capital); back() },
                        back = { back() },
                    )
                    Routes.MANAJEMEN_BARANG -> ProductListScreen(
                        items = products,
                        search = search,
                        setSearch = { search = it },
                        detail = { selectedProductId = it; go(Routes.PROFIL_BARANG) },
                        add = { editProductId = null; go(Routes.TAMBAH_BARANG) },
                        restock = { go(Routes.CATAT_RESTOCK) },
                    )
                    Routes.PROFIL_BARANG -> ProductDetailScreen(products.firstOrNull { it.id == selectedProductId }, { go(Routes.CATAT_RESTOCK) }, { editProductId = selectedProductId; go(Routes.TAMBAH_BARANG) }, { go(Routes.PENYESUAIAN_STOK) }, { go(Routes.PERGERAKAN_STOK) }, { vm.archiveProduct(selectedProductId); back() })
                    Routes.TAMBAH_BARANG -> ProductFormScreen(products.firstOrNull { it.id == editProductId }, { sku, name, categories, unit, sell, hpp, stock, min -> vm.saveProduct(editProductId, sku, name, categories, unit, sell, hpp, stock, min); back() }, { back() })
                    Routes.CATAT_RESTOCK -> RestockScreen(products, suppliers, { pid, qty, price, sid, note -> vm.restock(pid, qty, price, sid, note); back() }, { back() })
                    Routes.PENYESUAIAN_STOK -> AdjustmentScreen(products, { pid, stock, reason, note -> vm.adjustStock(pid, stock, reason, note); back() }, { back() })
                    Routes.PERGERAKAN_STOK -> MovementScreen(movements)
                    Routes.SUPPLIER -> SupplierScreen(suppliers, { id, name, phone, address, note -> vm.saveSupplier(id, name, phone, address, note) }, { vm.deleteSupplier(it) })
                    Routes.RIWAYAT_AKTIVITAS -> ActivityScreen(activities)
                    Routes.METODE_PEMBAYARAN -> PaymentSettingsScreen(store?.bankInfo ?: "", { vm.updateBankInfo(it) }, { vm.updateQris(it) })
                    Routes.K1_BERANDA_KASIR -> CashierScreen(
                        items = products,
                        search = search,
                        setSearch = { search = it },
                        add = { vm.addToCart(it) },
                        cart = { go(Routes.K2_CATAT_TRANSAKSI) },
                        history = { go(Routes.RIWAYAT_HARI_INI) },
                        avatar = { showAvatar = true },
                        cartItems = cart,
                        onChangeQty = { id, delta -> vm.changeCart(id, delta) },
                    )
                    Routes.K2_CATAT_TRANSAKSI -> CartScreen(
                        cart = cart,
                        change = { id, delta -> vm.changeCart(id, delta) },
                        pay = { go(Routes.K3_PEMBAYARAN) },
                        back = { back() },
                    )
                    Routes.K3_PEMBAYARAN -> PaymentScreen(
                        cart.sumOf { it.unitPrice * it.quantity },
                        { method, received ->
                            vm.checkout(method, received) {
                                go(if (sesiAktif?.peran == Peran.OWNER) Routes.K4_TRANSAKSI_BERHASIL_OWNER else Routes.K4_TRANSAKSI_BERHASIL)
                            }
                        },
                        { back() },
                    )
                    Routes.K4_TRANSAKSI_BERHASIL, Routes.K4_TRANSAKSI_BERHASIL_OWNER -> SuccessScreen(
                        invoice,
                        lastTotal,
                        sesiAktif?.peran == Peran.OWNER,
                        { root(Routes.K1_BERANDA_KASIR) },
                        { go(if (sesiAktif?.peran == Peran.OWNER) Routes.RIWAYAT_HARI_INI_OWNER else Routes.RIWAYAT_HARI_INI) },
                        { root(Routes.DASHBOARD) },
                    )
                    Routes.RIWAYAT_HARI_INI, Routes.RIWAYAT_HARI_INI_OWNER -> HistoryScreen(
                        transactions,
                        { tx ->
                            selectedTransaction = tx.invoiceNo
                            isOwnerCancel = sesiAktif?.peran == Peran.OWNER
                            cancelInvoice = tx.invoiceNo
                        },
                        sesiAktif?.peran == Peran.KASIR,
                    )
                    else -> WelcomeScreen({ root(Routes.LOGIN) }, { root(Routes.REGISTER_STEP1) })
                }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter), color = Red)
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

    if (cancelInvoice != null) {
        AlertDialog(
            onDismissRequest = { cancelInvoice = null; cancelReason = "" },
            title = { Text(if (isOwnerCancel) "Batalkan Transaksi?" else "Ajukan Pembatalan?") },
            text = {
                Column {
                    Text("Alasan wajib diisi. Stok akan dikembalikan setelah pembatalan Owner disetujui.", fontSize = 12.sp)
                    OutlinedTextField(cancelReason, { cancelReason = it }, label = { Text("Alasan pembatalan") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (cancelReason.isNotBlank()) {
                        vm.cancelTransaction(cancelInvoice!!, cancelReason, !isOwnerCancel)
                        cancelInvoice = null
                        cancelReason = ""
                    }
                }) { Text(if (isOwnerCancel) "Batalkan" else "Kirim Pengajuan", color = Red) }
            },
            dismissButton = { TextButton(onClick = { cancelInvoice = null; cancelReason = "" }) { Text("Kembali") } },
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
    Routes.PROFIL to "Profil",
    Routes.EDIT_PROFIL to "Edit Profil",
    Routes.UBAH_KATA_SANDI to "Ubah Kata Sandi",
    Routes.NOTIFIKASI to "Notifikasi",
    Routes.PENGELUARAN to "Pengeluaran",
    Routes.CATAT_PENGELUARAN to "Catat Pengeluaran",
    Routes.PEMASUKAN to "Pemasukan",
    Routes.CATAT_PEMASUKAN to "Catat Pemasukan",
    Routes.MANAJEMEN_BARANG to "Manajemen Barang",
    Routes.PROFIL_BARANG to "Profil Barang",
    Routes.TAMBAH_BARANG to "Tambah / Edit Barang",
    Routes.CATAT_RESTOCK to "Catat Restock",
    Routes.PENYESUAIAN_STOK to "Penyesuaian Stok",
    Routes.PERGERAKAN_STOK to "Pergerakan Stok",
    Routes.SUPPLIER to "Supplier",
    Routes.RIWAYAT_AKTIVITAS to "Riwayat Aktivitas",
    Routes.PENGATURAN to "Pengaturan",
    Routes.MANAJEMEN_AKUN_KASIR to "Akun Kasir",
    Routes.METODE_PEMBAYARAN to "Metode Pembayaran",
    Routes.K1_BERANDA_KASIR to "Beranda Kasir",
    Routes.K2_CATAT_TRANSAKSI to "Keranjang",
    Routes.K3_PEMBAYARAN to "Pembayaran",
    Routes.K4_TRANSAKSI_BERHASIL to "Transaksi Berhasil",
    Routes.K4_TRANSAKSI_BERHASIL_OWNER to "Transaksi Berhasil",
    Routes.RIWAYAT_HARI_INI to "Riwayat Hari Ini",
    Routes.RIWAYAT_HARI_INI_OWNER to "Riwayat Hari Ini",
)[route] ?: "AURA"