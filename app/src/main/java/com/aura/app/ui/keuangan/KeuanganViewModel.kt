package com.aura.app.ui.keuangan

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aura.app.data.local.entity.BarangEntity
import com.aura.app.data.local.entity.PemasukanKustomEntity
import com.aura.app.data.local.entity.PemasukanPenjualanCacheEntity
import com.aura.app.data.local.entity.PengeluaranEntity
import com.aura.app.data.local.entity.SupplierEntity
import com.aura.app.data.local.entity.TokoEntity
import com.aura.app.data.preferences.SesiAktif
import com.aura.app.data.remote.dto.PemasukanResponseDto
import com.aura.app.data.remote.dto.TransaksiDetailResponseDto
import com.aura.app.data.repository.BarangRepository
import com.aura.app.data.repository.KeuanganRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PemasukanTampilan(
    val id: String,
    val jenis: String,
    val judul: String,
    val nominal: Long,
    val tanggal: Long,
    val status: String,
    val alasanBatal: String?,
    val catatan: String?,
    val buktiFileId: String?,
    val nomorTransaksi: String? = null,
    val metodePembayaran: String? = null,
    val namaAkun: String? = null,
)

data class PengeluaranFormState(
    val judul: String = "",
    val jenisBiaya: String = "LAINNYA",
    val nominal: String = "",
    val catatan: String = "",
)

data class PemasukanFormState(
    val judul: String = "",
    val nominal: String = "",
    val catatan: String = "",
)

data class KeuanganUiState(
    val idToko: String? = null,
    val toko: TokoEntity? = null,
    val pengeluaran: List<PengeluaranEntity> = emptyList(),
    val pemasukan: List<PemasukanTampilan> = emptyList(),
    val barang: List<BarangEntity> = emptyList(),
    val supplier: List<SupplierEntity> = emptyList(),
    val detailPengeluaran: PengeluaranEntity? = null,
    val detailPemasukan: PemasukanResponseDto? = null,
    val detailTransaksi: TransaksiDetailResponseDto? = null,
    val fileBytes: Map<String, ByteArray> = emptyMap(),
    val fileIdsGagalDimuat: Set<String> = emptySet(),
    val buktiFileIdUnggahan: String? = null,
    val formPengeluaran: PengeluaranFormState = PengeluaranFormState(),
    val formPemasukan: PemasukanFormState = PemasukanFormState(),
    val namaPenerimaDraft: String = "",
    val infoRekeningDraft: String = "",
    val metodePembayaranBerubah: Boolean = false,
    val namaTokoDraft: String = "",
    val jenisUsahaDraft: String = "",
    val alamatDraft: String = "",
    val saldoAwalDraft: String = "",
    val pengaturanTokoBerubah: Boolean = false,
    val isLoading: Boolean = true,
    val isLoadingDetail: Boolean = false,
    val isSaving: Boolean = false,
    val isUploadingFile: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface KeuanganEvent {
    data class TampilkanPesan(val teks: String) : KeuanganEvent
    data object FormTersimpan : KeuanganEvent
    data object PembatalanBerhasil : KeuanganEvent
}

class KeuanganViewModel(
    private val repository: KeuanganRepository,
    private val barangRepository: BarangRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(KeuanganUiState())
    val uiState: StateFlow<KeuanganUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<KeuanganEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<KeuanganEvent> = _events.asSharedFlow()

    private var sesiAktif: SesiAktif? = null
    private var observeJob: Job? = null
    private var observeCatalogJob: Job? = null
    private var refreshJob: Job? = null
    private var detailPengeluaranJob: Job? = null

    fun sambungkanSesi(sesi: SesiAktif?) {
        if (sesiAktif?.akunId == sesi?.akunId && sesiAktif?.idToko == sesi?.idToko) return
        observeJob?.cancel()
        observeCatalogJob?.cancel()
        refreshJob?.cancel()
        detailPengeluaranJob?.cancel()
        sesiAktif = sesi
        _uiState.value = KeuanganUiState(idToko = sesi?.idToko, isLoading = sesi != null)
        if (sesi?.peran != com.aura.app.core.model.Peran.OWNER) return

        observeJob = viewModelScope.launch {
            combine(
                repository.observeToko(sesi.idToko),
                repository.observePengeluaran(sesi.idToko),
                repository.observePemasukanKustom(sesi.idToko),
                repository.observePemasukanPenjualan(sesi.idToko),
            ) { toko, expenses, customIncomes, saleIncomes ->
                _uiState.update { state ->
                    state.copy(
                        toko = toko,
                        pengeluaran = expenses,
                        pemasukan = combineIncomes(customIncomes, saleIncomes),
                        namaPenerimaDraft = if (state.metodePembayaranBerubah) state.namaPenerimaDraft else toko?.namaPenerima.orEmpty(),
                        infoRekeningDraft = if (state.metodePembayaranBerubah) state.infoRekeningDraft else toko?.infoRekening.orEmpty(),
                        namaTokoDraft = if (state.pengaturanTokoBerubah) state.namaTokoDraft else toko?.namaToko.orEmpty(),
                        jenisUsahaDraft = if (state.pengaturanTokoBerubah) state.jenisUsahaDraft else toko?.jenisUsaha.orEmpty(),
                        alamatDraft = if (state.pengaturanTokoBerubah) state.alamatDraft else toko?.alamat.orEmpty(),
                        saldoAwalDraft = if (state.pengaturanTokoBerubah) state.saldoAwalDraft else toko?.saldoAwal?.toString().orEmpty(),
                    )
                }
            }.collect {}
        }
        observeCatalogJob = viewModelScope.launch {
            combine(
                barangRepository.observeBarang(sesi.idToko),
                barangRepository.observeSupplier(sesi.idToko),
            ) { barang, supplier ->
                _uiState.update { it.copy(barang = barang, supplier = supplier) }
            }.collect {}
        }
        refresh()
    }

    fun refresh() {
        val idToko = _uiState.value.idToko ?: return
        if (sesiAktif?.peran != com.aura.app.core.model.Peran.OWNER || refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                repository.refresh(idToko)
                _uiState.update { it.copy(isLoading = false, errorMessage = null) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Data Keuangan gagal dimuat.",
                    )
                }
            }
        }
    }

    fun siapkanFormPengeluaran() {
        _uiState.update { it.copy(formPengeluaran = PengeluaranFormState(), buktiFileIdUnggahan = null, errorMessage = null) }
    }

    fun ubahJudulPengeluaran(value: String) {
        _uiState.update { it.copy(formPengeluaran = it.formPengeluaran.copy(judul = value)) }
    }

    fun ubahJenisBiaya(value: String) {
        _uiState.update { it.copy(formPengeluaran = it.formPengeluaran.copy(jenisBiaya = value)) }
    }

    fun ubahNominalPengeluaran(value: String) {
        _uiState.update { it.copy(formPengeluaran = it.formPengeluaran.copy(nominal = value.filter(Char::isDigit))) }
    }

    fun ubahCatatanPengeluaran(value: String) {
        _uiState.update { it.copy(formPengeluaran = it.formPengeluaran.copy(catatan = value)) }
    }

    fun siapkanFormPemasukan() {
        _uiState.update { it.copy(formPemasukan = PemasukanFormState(), buktiFileIdUnggahan = null, errorMessage = null) }
    }

    fun ubahJudulPemasukan(value: String) {
        _uiState.update { it.copy(formPemasukan = it.formPemasukan.copy(judul = value)) }
    }

    fun ubahNominalPemasukan(value: String) {
        _uiState.update { it.copy(formPemasukan = it.formPemasukan.copy(nominal = value.filter(Char::isDigit))) }
    }

    fun ubahCatatanPemasukan(value: String) {
        _uiState.update { it.copy(formPemasukan = it.formPemasukan.copy(catatan = value)) }
    }

    fun catatPengeluaran() {
        val idToko = _uiState.value.idToko ?: return
        val form = _uiState.value.formPengeluaran
        val nominal = form.nominal.toLongOrNull() ?: 0L
        if (form.judul.isBlank() || nominal <= 0 || form.jenisBiaya !in setOf("LISTRIK", "SEWA", "GAJI", "LAINNYA")) {
            pesan("Lengkapi judul, jenis biaya, dan nominal Pengeluaran yang valid.")
            return
        }
        jalankanSimpan(
            sukses = "Pengeluaran berhasil dicatat.",
            kembaliKeDaftar = true,
        ) {
            repository.catatPengeluaran(
                idToko,
                form.judul,
                form.jenisBiaya,
                nominal,
                form.catatan,
                _uiState.value.buktiFileIdUnggahan,
            )
        }
    }

    fun catatPemasukan() {
        val idToko = _uiState.value.idToko ?: return
        val form = _uiState.value.formPemasukan
        val nominal = form.nominal.toLongOrNull() ?: 0L
        if (form.judul.isBlank() || nominal <= 0) {
            pesan("Lengkapi judul dan nominal Pemasukan yang valid.")
            return
        }
        jalankanSimpan(
            sukses = "Pemasukan berhasil dicatat.",
            kembaliKeDaftar = true,
        ) {
            repository.catatPemasukan(
                idToko,
                form.judul,
                nominal,
                form.catatan,
                _uiState.value.buktiFileIdUnggahan,
            )
        }
    }

    fun muatDetailPengeluaran(id: String) {
        val idToko = _uiState.value.idToko ?: return
        detailPengeluaranJob?.cancel()
        detailPengeluaranJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    detailPengeluaran = null,
                    isLoadingDetail = true,
                    errorMessage = null,
                )
            }
            try {
                val detail = repository.detailPengeluaran(idToko, id)
                _uiState.update { it.copy(detailPengeluaran = detail, isLoadingDetail = false) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(isLoadingDetail = false, errorMessage = error.message ?: "Detail Pengeluaran gagal dimuat.")
                }
            }
        }
    }

    fun muatDetailPemasukan(id: String) {
        val idToko = _uiState.value.idToko ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoadingDetail = true, detailPemasukan = null, detailTransaksi = null, errorMessage = null)
            }
            try {
                val income = repository.detailPemasukan(idToko, id)
                val transaction = if (income.jenis == "PENJUALAN") {
                    repository.detailTransaksi(idToko, id)
                } else {
                    null
                }
                _uiState.update {
                    it.copy(
                        detailPemasukan = income,
                        detailTransaksi = transaction,
                        isLoadingDetail = false,
                    )
                }
                income.buktiFileId?.let(::muatFile)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(isLoadingDetail = false, errorMessage = error.message ?: "Detail Pemasukan gagal dimuat.")
                }
            }
        }
    }

    fun batalkanPengeluaran(id: String, alasan: String) {
        if (alasan.isBlank()) {
            pesan("Alasan pembatalan wajib diisi.")
            return
        }
        val idToko = _uiState.value.idToko ?: return
        jalankanSimpan(sukses = "Catatan berhasil dibatalkan.", kembaliKeDaftar = false) {
            repository.batalkanPengeluaran(idToko, id, alasan)
            _events.emit(KeuanganEvent.PembatalanBerhasil)
        }
    }

    fun batalkanPemasukan(id: String, jenis: String, alasan: String) {
        if (alasan.isBlank()) {
            pesan("Alasan pembatalan wajib diisi.")
            return
        }
        val idToko = _uiState.value.idToko ?: return
        jalankanSimpan(sukses = "Catatan berhasil dibatalkan.", kembaliKeDaftar = false) {
            if (jenis == "PENJUALAN") {
                repository.batalkanPenjualan(idToko, id, alasan)
            } else {
                repository.batalkanPemasukanKustom(idToko, id, alasan)
            }
            _events.emit(KeuanganEvent.PembatalanBerhasil)
        }
    }

    fun ubahNamaToko(value: String) {
        _uiState.update { it.copy(namaTokoDraft = value, pengaturanTokoBerubah = true) }
    }

    fun ubahJenisUsaha(value: String) {
        _uiState.update { it.copy(jenisUsahaDraft = value, pengaturanTokoBerubah = true) }
    }

    fun ubahAlamatToko(value: String) {
        _uiState.update { it.copy(alamatDraft = value, pengaturanTokoBerubah = true) }
    }

    fun ubahSaldoAwal(value: String) {
        _uiState.update { it.copy(saldoAwalDraft = value.filter(Char::isDigit), pengaturanTokoBerubah = true) }
    }

    fun simpanPengaturanToko() {
        val idToko = _uiState.value.idToko ?: return
        val state = _uiState.value
        val nama = state.namaTokoDraft
        if (nama.isBlank()) {
            pesan("Nama Toko wajib diisi.")
            return
        }
        jalankanSimpan("Pengaturan Toko berhasil diperbarui.") {
            repository.simpanPengaturanToko(idToko, nama, state.jenisUsahaDraft, state.alamatDraft)
            _uiState.update { it.copy(pengaturanTokoBerubah = false) }
        }
    }

    fun koreksiSaldoAwal() {
        val saldoAwal = _uiState.value.saldoAwalDraft.toLongOrNull()
        if (saldoAwal == null || saldoAwal < 0) {
            pesan("Saldo awal harus berupa angka nol atau lebih.")
            return
        }
        val idToko = _uiState.value.idToko ?: return
        jalankanSimpan("Saldo awal berhasil diperbarui.") {
            repository.koreksiSaldoAwal(idToko, saldoAwal)
            _uiState.update {
                it.copy(saldoAwalDraft = saldoAwal.toString(), pengaturanTokoBerubah = false)
            }
        }
    }

    fun ubahNamaPenerima(value: String) {
        _uiState.update { it.copy(namaPenerimaDraft = value, metodePembayaranBerubah = true) }
    }

    fun ubahInfoRekening(value: String) {
        _uiState.update { it.copy(infoRekeningDraft = value, metodePembayaranBerubah = true) }
    }

    fun simpanMetodePembayaran() {
        val idToko = _uiState.value.idToko ?: return
        val state = _uiState.value
        jalankanSimpan("Metode Pembayaran berhasil diperbarui.") {
            repository.simpanMetodePembayaran(
                idToko,
                state.namaPenerimaDraft,
                state.infoRekeningDraft,
                state.toko?.qrisFileId,
            )
            _uiState.update { it.copy(metodePembayaranBerubah = false) }
        }
    }

    fun unggahQris(resolver: ContentResolver, uri: Uri) {
        val idToko = _uiState.value.idToko ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingFile = true, errorMessage = null) }
            try {
                val draft = _uiState.value
                val fileId = repository.unggahFile(resolver, uri)
                repository.simpanMetodePembayaran(
                    idToko,
                    draft.namaPenerimaDraft,
                    draft.infoRekeningDraft,
                    fileId,
                )
                _uiState.update {
                    it.copy(
                        isUploadingFile = false,
                        metodePembayaranBerubah = false,
                    )
                }
                muatFile(fileId)
                _events.emit(KeuanganEvent.TampilkanPesan("Gambar QRIS berhasil diperbarui."))
            } catch (error: CancellationException) {
                _uiState.update { it.copy(isUploadingFile = false) }
                throw error
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(isUploadingFile = false, errorMessage = error.message ?: "Gambar QRIS gagal diunggah.")
                }
            }
        }
    }

    fun hapusQris() {
        val idToko = _uiState.value.idToko ?: return
        val state = _uiState.value
        jalankanSimpan("Gambar QRIS berhasil dihapus.") {
            repository.simpanMetodePembayaran(
                idToko,
                state.namaPenerimaDraft,
                state.infoRekeningDraft,
                null,
            )
            _uiState.update { it.copy(metodePembayaranBerubah = false) }
        }
    }

    fun unggahBukti(resolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingFile = true, errorMessage = null) }
            try {
                val fileId = repository.unggahFile(resolver, uri)
                _uiState.update {
                    it.copy(isUploadingFile = false, buktiFileIdUnggahan = fileId)
                }
                _events.emit(KeuanganEvent.TampilkanPesan("Bukti berhasil diunggah."))
            } catch (error: CancellationException) {
                _uiState.update { it.copy(isUploadingFile = false) }
                throw error
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(isUploadingFile = false, errorMessage = error.message ?: "Bukti gagal diunggah.")
                }
            }
        }
    }

    fun resetBuktiUnggahan() {
        _uiState.update { it.copy(buktiFileIdUnggahan = null) }
    }

    fun muatFile(fileId: String) {
        if (fileId in _uiState.value.fileBytes || fileId in _uiState.value.fileIdsGagalDimuat) return
        viewModelScope.launch {
            try {
                val bytes = repository.ambilFile(fileId)
                _uiState.update { it.copy(fileBytes = it.fileBytes + (fileId to bytes)) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        fileIdsGagalDimuat = it.fileIdsGagalDimuat + fileId,
                        errorMessage = error.message ?: "File gagal dimuat.",
                    )
                }
            }
        }
    }

    fun clearDetail() {
        _uiState.update {
            it.copy(
                detailPengeluaran = null,
                detailPemasukan = null,
                detailTransaksi = null,
                errorMessage = null,
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun jalankanSimpan(
        sukses: String,
        kembaliKeDaftar: Boolean = false,
        block: suspend () -> Unit,
    ) {
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                block()
                _uiState.update {
                    it.copy(isSaving = false, buktiFileIdUnggahan = null, errorMessage = null)
                }
                if (kembaliKeDaftar) _events.emit(KeuanganEvent.FormTersimpan)
                _events.emit(KeuanganEvent.TampilkanPesan(sukses))
            } catch (error: CancellationException) {
                _uiState.update { it.copy(isSaving = false) }
                throw error
            } catch (error: Exception) {
                val message = error.message ?: "Perubahan gagal disimpan."
                _uiState.update { it.copy(isSaving = false, errorMessage = message) }
                _events.emit(KeuanganEvent.TampilkanPesan(message))
            }
        }
    }

    private fun pesan(teks: String) {
        _events.tryEmit(KeuanganEvent.TampilkanPesan(teks))
    }

    private fun combineIncomes(
        customIncomes: List<PemasukanKustomEntity>,
        sales: List<PemasukanPenjualanCacheEntity>,
    ): List<PemasukanTampilan> =
        (
            customIncomes.map {
                PemasukanTampilan(
                    id = it.id,
                    jenis = "KUSTOM",
                    judul = it.judul,
                    nominal = it.nominal,
                    tanggal = it.tanggal,
                    status = it.status.name,
                    alasanBatal = it.alasanBatal,
                    catatan = it.catatan,
                    buktiFileId = it.buktiFileId,
                )
            } + sales.map {
                PemasukanTampilan(
                    id = it.id,
                    jenis = "PENJUALAN",
                    judul = it.nomorTransaksi,
                    nominal = it.nominal,
                    tanggal = it.tanggal,
                    status = it.status.name,
                    alasanBatal = it.alasanBatal,
                    catatan = it.catatan,
                    buktiFileId = null,
                    nomorTransaksi = it.nomorTransaksi,
                    metodePembayaran = it.metodePembayaran.name,
                    namaAkun = it.namaAkun,
                )
            }
        ).sortedByDescending { it.tanggal }

    companion object {
        fun factory(
            repository: KeuanganRepository,
            barangRepository: BarangRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                KeuanganViewModel(repository, barangRepository) as T
        }
    }
}
