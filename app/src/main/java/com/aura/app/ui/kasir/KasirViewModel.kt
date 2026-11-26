package com.aura.app.ui.kasir

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aura.app.R
import com.aura.app.core.model.MetodePembayaran
import com.aura.app.core.model.Peran
import com.aura.app.core.model.StatusBarang
import com.aura.app.data.local.entity.BarangEntity
import com.aura.app.data.local.entity.TransaksiPenjualanEntity
import com.aura.app.data.preferences.SesiAktif
import com.aura.app.data.remote.dto.BarangDibeliRequestDto
import com.aura.app.data.remote.dto.MetodePembayaranTokoDto
import com.aura.app.data.remote.dto.TransaksiBuatRequestDto
import com.aura.app.data.repository.BarangRepository
import com.aura.app.data.repository.PenjualanRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class BarisKeranjang(
    val barangId: String,
    val namaBarang: String,
    val satuan: String,
    val hargaJual: Long,
    val stokSaatDipilih: Int,
    val jumlah: Int,
)

data class PesanKasir(
    val detail: String? = null,
    @StringRes val fallback: Int,
)

data class PengajuanPembatalanTertunda(
    val requestId: String,
    val alasan: String,
    val terkirim: Boolean = false,
)

data class KasirUiState(
    val keranjang: List<BarisKeranjang> = emptyList(),
    val transaksiHariIni: List<TransaksiPenjualanEntity> = emptyList(),
    val isLoadingRiwayat: Boolean = false,
    val riwayatError: PesanKasir? = null,
    val metodePembayaran: MetodePembayaranTokoDto? = null,
    val isLoadingMetodePembayaran: Boolean = false,
    val metodePembayaranError: PesanKasir? = null,
    val qrisBytes: ByteArray? = null,
    val qrisFileIdLoaded: String? = null,
    val qrisFileIdLoading: String? = null,
    val qrisFileIdFailed: String? = null,
    val qrisError: PesanKasir? = null,
    val isSavingTransaksi: Boolean = false,
    val checkoutError: PesanKasir? = null,
    val permintaanTransaksiTertunda: TransaksiBuatRequestDto? = null,
    val pengajuanPembatalanTertunda: Map<String, PengajuanPembatalanTertunda> = emptyMap(),
    val isSavingPembatalan: Boolean = false,
    val invoiceTerakhir: String = "",
    val totalTerakhir: Long = 0,
)

sealed interface KasirEvent {
    data class TampilkanPesan(val pesan: PesanKasir) : KasirEvent
    data class TransaksiBerhasil(val olehOwner: Boolean) : KasirEvent
    data class PembatalanBerhasil(val olehOwner: Boolean) : KasirEvent
}

class KasirViewModel(
    private val penjualanRepository: PenjualanRepository,
    private val barangRepository: BarangRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(KasirUiState())
    val uiState: StateFlow<KasirUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<KasirEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<KasirEvent> = _events.asSharedFlow()

    private var sesiAktif: SesiAktif? = null
    private var observeRiwayatJob: Job? = null
    private var permintaanTransaksiTertunda: TransaksiBuatRequestDto? = null
    private val pengajuanPembatalanTertunda = mutableMapOf<String, PengajuanPembatalanTertunda>()

    fun sambungkanSesi(sesi: SesiAktif?) {
        if (sesiAktif?.akunId == sesi?.akunId && sesiAktif?.idToko == sesi?.idToko) return
        observeRiwayatJob?.cancel()
        sesiAktif = sesi
        permintaanTransaksiTertunda = null
        pengajuanPembatalanTertunda.clear()
        _uiState.value = KasirUiState()
        if (sesi != null) {
            observeRiwayatJob = viewModelScope.launch {
                penjualanRepository.observeTransaksiHariIni(sesi.idToko).collect { transaksi ->
                    val terlihat = if (sesi.peran == Peran.KASIR) {
                        transaksi.filter { it.akunId == sesi.akunId }
                    } else {
                        transaksi
                    }
                    _uiState.update { it.copy(transaksiHariIni = terlihat) }
                }
            }
        }
    }

    fun tambahBarang(barang: BarangEntity) {
        if (permintaanTransaksiTertunda != null) {
            pesan(R.string.kasir_checkout_retry_before_edit)
            return
        }
        if (barang.status != StatusBarang.AKTIF || barang.stokSaatIni <= 0) {
            pesan(R.string.kasir_stock_unavailable)
            return
        }
        val currentState = _uiState.value
        val currentIndex = currentState.keranjang.indexOfFirst { it.barangId == barang.id }
        if (currentIndex >= 0 && currentState.keranjang[currentIndex].jumlah >= currentState.keranjang[currentIndex].stokSaatDipilih) {
            pesan(R.string.kasir_quantity_exceeds_stock)
            return
        }
        _uiState.update { state ->
            val index = state.keranjang.indexOfFirst { it.barangId == barang.id }
            if (index < 0) {
                state.copy(
                    keranjang = state.keranjang + BarisKeranjang(
                        barangId = barang.id,
                        namaBarang = barang.nama,
                        satuan = barang.satuan,
                        hargaJual = barang.hargaJual,
                        stokSaatDipilih = barang.stokSaatIni,
                        jumlah = 1,
                    ),
                )
            } else {
                val current = state.keranjang[index]
                state.copy(
                    keranjang = state.keranjang.toMutableList().apply {
                        this[index] = current.copy(jumlah = current.jumlah + 1)
                    },
                )
            }
        }
    }

    fun ubahJumlah(barangId: String, delta: Int) {
        if (permintaanTransaksiTertunda != null) {
            pesan(R.string.kasir_checkout_retry_before_edit)
            return
        }
        val current = _uiState.value.keranjang.firstOrNull { it.barangId == barangId } ?: return
        if (current.jumlah + delta > current.stokSaatDipilih) {
            pesan(R.string.kasir_quantity_exceeds_stock)
            return
        }
        _uiState.update { state ->
            val index = state.keranjang.indexOfFirst { it.barangId == barangId }
            if (index < 0) return@update state
            val stateCurrent = state.keranjang[index]
            val nextQuantity = stateCurrent.jumlah + delta
            if (nextQuantity <= 0) {
                state.copy(keranjang = state.keranjang.filterNot { it.barangId == barangId })
            } else {
                state.copy(
                    keranjang = state.keranjang.toMutableList().apply {
                        this[index] = stateCurrent.copy(jumlah = nextQuantity)
                    },
                )
            }
        }
    }

    fun kosongkanKeranjang() {
        if (permintaanTransaksiTertunda != null) return
        _uiState.update { it.copy(keranjang = emptyList()) }
    }

    fun muatRiwayatHariIni() {
        val idToko = sesiAktif?.idToko ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingRiwayat = true, riwayatError = null) }
            try {
                penjualanRepository.refreshRiwayatHariIni(idToko)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(riwayatError = error.toPesanKasir(R.string.kasir_history_load_failed))
                }
            } finally {
                _uiState.update { it.copy(isLoadingRiwayat = false) }
            }
        }
    }

    fun muatMetodePembayaran() {
        val idToko = sesiAktif?.idToko ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMetodePembayaran = true, metodePembayaranError = null) }
            try {
                val metode = penjualanRepository.ambilMetodePembayaran(idToko)
                _uiState.update { state ->
                    state.copy(
                        metodePembayaran = metode,
                        qrisBytes = state.qrisBytes.takeIf { state.qrisFileIdLoaded == metode.qrisFileId },
                        qrisFileIdLoaded = state.qrisFileIdLoaded.takeIf { it == metode.qrisFileId },
                        qrisFileIdFailed = state.qrisFileIdFailed.takeIf { it == metode.qrisFileId },
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(metodePembayaranError = error.toPesanKasir(R.string.kasir_payment_settings_load_failed))
                }
            } finally {
                _uiState.update { it.copy(isLoadingMetodePembayaran = false) }
            }
        }
    }

    fun muatQris(fileId: String, ulangi: Boolean = false) {
        val state = _uiState.value
        if (state.qrisFileIdLoaded == fileId && state.qrisBytes != null) return
        if (state.qrisFileIdLoading == fileId) return
        if (state.qrisFileIdFailed == fileId && !ulangi) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    qrisFileIdLoading = fileId,
                    qrisFileIdFailed = null,
                    qrisError = null,
                )
            }
            try {
                val bytes = barangRepository.ambilFile(fileId)
                _uiState.update {
                    it.copy(
                        qrisBytes = bytes,
                        qrisFileIdLoaded = fileId,
                        qrisFileIdLoading = null,
                        qrisFileIdFailed = null,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        qrisFileIdLoading = null,
                        qrisFileIdFailed = fileId,
                        qrisError = error.toPesanKasir(R.string.kasir_qris_load_failed),
                    )
                }
            }
        }
    }

    fun selesaikanTransaksi(
        metode: MetodePembayaran,
        uangDiterima: Long,
        pembayaranDiterima: Boolean,
    ) {
        val sesi = sesiAktif
        if (sesi == null) {
            pesan(R.string.kasir_session_missing)
            return
        }
        val cart = _uiState.value.keranjang
        val pendingRequest = permintaanTransaksiTertunda
        if (pendingRequest == null) {
            if (cart.isEmpty()) {
                pesan(R.string.kasir_cart_required)
                return
            }
            if (metode == MetodePembayaran.TUNAI && uangDiterima < totalKeranjang(cart)) {
                pesan(R.string.kasir_cash_insufficient)
                return
            }
            if (metode != MetodePembayaran.TUNAI && !pembayaranDiterima) {
                pesan(R.string.kasir_payment_confirmation_required)
                return
            }
        }
        if (_uiState.value.isSavingTransaksi) return

        val request = pendingRequest ?: TransaksiBuatRequestDto(
            id = UUID.randomUUID().toString(),
            metodePembayaran = metode.name,
            uangDiterima = uangDiterima.takeIf { metode == MetodePembayaran.TUNAI },
            pembayaranDiterima = true.takeIf { metode != MetodePembayaran.TUNAI },
            barangDibeli = cart.map { BarangDibeliRequestDto(it.barangId, it.jumlah) },
        ).also {
            permintaanTransaksiTertunda = it
            _uiState.update { state -> state.copy(permintaanTransaksiTertunda = it) }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSavingTransaksi = true, checkoutError = null) }
            try {
                val response = penjualanRepository.catatTransaksi(sesi.idToko, request)
                val invoice = response.transaksi.nomorTransaksi
                val total = response.transaksi.total
                permintaanTransaksiTertunda = null
                _uiState.update {
                    it.copy(
                        keranjang = emptyList(),
                        invoiceTerakhir = invoice,
                        totalTerakhir = total,
                        isSavingTransaksi = false,
                        permintaanTransaksiTertunda = null,
                    )
                }
                _events.emit(KasirEvent.TransaksiBerhasil(sesi.peran == Peran.OWNER))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isSavingTransaksi = false,
                        checkoutError = error.toPesanKasir(R.string.kasir_checkout_failed),
                    )
                }
            }
        }
    }

    fun batalkanAtauAjukan(transaksiId: String, alasan: String, olehOwner: Boolean) {
        val sesi = sesiAktif
        if (sesi == null) {
            _events.tryEmit(KasirEvent.TampilkanPesan(PesanKasir(fallback = R.string.kasir_session_missing)))
            return
        }
        if (_uiState.value.isSavingPembatalan) return
        val request = if (olehOwner) null else {
            if (pengajuanPembatalanTertunda[transaksiId]?.terkirim == true) return
            pengajuanPembatalanTertunda.getOrPut(transaksiId) {
                PengajuanPembatalanTertunda(UUID.randomUUID().toString(), alasan)
            }
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingPembatalan = true) }
            try {
                if (olehOwner) {
                    penjualanRepository.batalkanTransaksi(sesi.idToko, transaksiId, alasan)
                } else {
                    val pengajuan = checkNotNull(request)
                    penjualanRepository.ajukanPembatalan(
                        sesi.idToko,
                        transaksiId,
                        pengajuan.requestId,
                        pengajuan.alasan,
                    )
                    pengajuanPembatalanTertunda[transaksiId] =
                        pengajuan.copy(terkirim = true)
                }
                _uiState.update {
                    it.copy(
                        isSavingPembatalan = false,
                        pengajuanPembatalanTertunda = pengajuanPembatalanTertunda.toMap(),
                    )
                }
                _events.emit(KasirEvent.PembatalanBerhasil(olehOwner))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isSavingPembatalan = false,
                        pengajuanPembatalanTertunda = pengajuanPembatalanTertunda.toMap(),
                    )
                }
                _events.emit(
                    KasirEvent.TampilkanPesan(
                        error.toPesanKasir(R.string.kasir_cancellation_failed),
                    ),
                )
            }
        }
    }

    private fun pesan(@StringRes resourceId: Int) {
        _events.tryEmit(KasirEvent.TampilkanPesan(PesanKasir(fallback = resourceId)))
    }

    private fun totalKeranjang(cart: List<BarisKeranjang>): Long =
        cart.sumOf { it.hargaJual * it.jumlah }

    private fun Exception.toPesanKasir(@StringRes fallback: Int): PesanKasir =
        PesanKasir(detail = message, fallback = fallback)

    companion object {
        fun factory(
            penjualanRepository: PenjualanRepository,
            barangRepository: BarangRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                KasirViewModel(penjualanRepository, barangRepository) as T
        }
    }
}
