package com.aura.app.ui.barang

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aura.app.core.logic.StatusStokEvaluator
import com.aura.app.core.model.StatusStok
import com.aura.app.data.local.entity.BarangEntity
import com.aura.app.data.local.entity.KategoriEntity
import com.aura.app.data.local.entity.PergerakanStokEntity
import com.aura.app.data.local.entity.SupplierEntity
import com.aura.app.data.remote.dto.BarangDetailResponseDto
import com.aura.app.data.repository.BarangRepository
import com.aura.app.data.repository.DataBarang
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BarangTampilan(
    val barang: BarangEntity,
    val kategori: List<String>,
    val kategoriIds: List<String>,
    val statusStok: StatusStok,
)

data class KategoriTampilan(
    val kategori: KategoriEntity,
    val jumlahBarang: Int,
)

data class KatalogUiState(
    val idToko: String? = null,
    val barang: List<BarangTampilan> = emptyList(),
    val kategori: List<KategoriEntity> = emptyList(),
    val kategoriTampilan: List<KategoriTampilan> = emptyList(),
    val supplier: List<SupplierEntity> = emptyList(),
    val pergerakan: List<PergerakanStokEntity> = emptyList(),
    val isLoadingPergerakan: Boolean = true,
    val pergerakanNextCursor: String? = null,
    val isLoadingMorePergerakan: Boolean = false,
    val detail: BarangDetailResponseDto? = null,
    val detailSupplier: com.aura.app.data.remote.dto.SupplierDetailResponseDto? = null,
    val fileBytes: Map<String, ByteArray> = emptyMap(),
    val fileIdsGagalDimuat: Set<String> = emptySet(),
    val fotoTerunggahFileId: String? = null,
    val isUploadingFoto: Boolean = false,
    val query: String = "",
    val filterStatus: String = "Semua",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSavingSupplier: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface KatalogEvent {
    data class Pesan(val teks: String) : KatalogEvent
    data object BarangTersimpan : KatalogEvent
    data class BarangDihapus(val status: String) : KatalogEvent
    data class SupplierBerhasilDihapus(val dihapusPermanen: Boolean) : KatalogEvent
    data object StokTersimpan : KatalogEvent
}

class KatalogViewModel(
    private val repository: BarangRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(KatalogUiState())
    val uiState: StateFlow<KatalogUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<KatalogEvent>()
    val events: SharedFlow<KatalogEvent> = _events.asSharedFlow()

    private var observedStoreId: String? = null
    private var observeJob: Job? = null

    fun muat(idToko: String) {
        if (observedStoreId != idToko) {
            observedStoreId = idToko
            observeJob?.cancel()
            observeJob = viewModelScope.launch {
                combine(
                    repository.observeBarang(idToko),
                    repository.observeKategori(idToko),
                    repository.observeRelasiKategori(),
                    repository.observeSupplier(idToko),
                    repository.observePergerakan(idToko),
                ) { barang, kategori, relasi, supplier, pergerakan ->
                    val kategoriById = kategori.associateBy { it.id }
                    val kategoriByBarang = relasi.groupBy({ it.barangId }, { it.kategoriId })
                    _uiState.update { state ->
                        state.copy(
                            barang = barang.map { entity ->
                                BarangTampilan(
                                    barang = entity,
                                    kategori = kategoriByBarang[entity.id].orEmpty().mapNotNull(kategoriById::get).map { it.nama },
                                    kategoriIds = kategoriByBarang[entity.id].orEmpty(),
                                    statusStok = StatusStokEvaluator.evaluate(
                                        entity.stokSaatIni,
                                        entity.stokMinimum,
                                        entity.status == com.aura.app.core.model.StatusBarang.DIARSIPKAN,
                                    ),
                                )
                            },
                            kategori = kategori,
                            kategoriTampilan = kategori.map { category ->
                                KategoriTampilan(
                                    kategori = category,
                                    jumlahBarang = relasi.count { it.kategoriId == category.id },
                                )
                            },
                            supplier = supplier,
                            pergerakan = pergerakan,
                            idToko = idToko,
                            isLoading = false,
                        )
                    }
                }.collect {}
            }
        }
        viewModelScope.launch {
            _uiState.update { it.copy(idToko = idToko, isLoading = true, errorMessage = null) }
            try {
                repository.refresh(idToko)
                _uiState.update { it.copy(isLoading = false, errorMessage = null) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "Gagal memuat katalog.") }
            }
        }
    }

    fun ubahPencarian(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun ubahFilterStatus(status: String) {
        _uiState.update { it.copy(filterStatus = status) }
    }

    fun refresh() {
        _uiState.value.idToko?.let(::muat)
    }

    fun muatDetail(barangId: String) {
        val idToko = _uiState.value.idToko ?: return
        viewModelScope.launch {
            try {
                val detail = repository.detailBarang(idToko, barangId)
                _uiState.update { it.copy(detail = detail, errorMessage = null) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _uiState.update { it.copy(errorMessage = error.message ?: "Gagal memuat detail Barang.") }
            }
        }
    }

    fun muatDetailSupplier(supplierId: String) {
        val idToko = _uiState.value.idToko ?: return
        viewModelScope.launch {
            try {
                val detail = repository.detailSupplier(idToko, supplierId)
                _uiState.update { it.copy(detailSupplier = detail, errorMessage = null) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _uiState.update { it.copy(errorMessage = error.message ?: "Gagal memuat detail Supplier.") }
            }
        }
    }

    fun muatPergerakan(idToko: String, barangId: String? = null) {
        _uiState.update {
            it.copy(
                idToko = idToko,
                isLoadingPergerakan = true,
                pergerakanNextCursor = null,
                isLoadingMorePergerakan = false,
                errorMessage = null,
            )
        }
        viewModelScope.launch {
            try {
                val nextCursor = repository.refreshPergerakan(idToko, barangId)
                _uiState.update {
                    it.copy(
                        isLoadingPergerakan = false,
                        errorMessage = null,
                        pergerakanNextCursor = nextCursor,
                        isLoadingMorePergerakan = false,
                    )
                }
            } catch (error: CancellationException) {
                _uiState.update { it.copy(isLoadingPergerakan = false) }
                throw error
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingPergerakan = false,
                        errorMessage = error.message ?: "Gagal memuat Pergerakan Stok.",
                    )
                }
            }
        }
    }

    fun muatPergerakanBerikutnya(barangId: String? = null) {
        val state = _uiState.value
        val idToko = state.idToko ?: return
        val cursor = state.pergerakanNextCursor ?: return
        if (state.isLoadingMorePergerakan) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMorePergerakan = true) }
            try {
                val nextCursor = repository.refreshPergerakan(idToko, barangId, cursor)
                _uiState.update {
                    it.copy(
                        isLoadingMorePergerakan = false,
                        pergerakanNextCursor = nextCursor,
                        errorMessage = null,
                    )
                }
            } catch (error: CancellationException) {
                _uiState.update { it.copy(isLoadingMorePergerakan = false) }
                throw error
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingMorePergerakan = false,
                        errorMessage = error.message ?: "Gagal memuat Pergerakan Stok berikutnya.",
                    )
                }
            }
        }
    }

    fun catatRestock(
        barangId: String,
        supplierId: String,
        jumlahMasuk: Int,
        totalDibayar: Long,
        catatan: String?,
    ) {
        val idToko = _uiState.value.idToko ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                repository.catatRestock(idToko, barangId, supplierId, jumlahMasuk, totalDibayar, catatan)
                _uiState.update { it.copy(isSaving = false, detail = null) }
                _events.emit(KatalogEvent.StokTersimpan)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _uiState.update { it.copy(isSaving = false, errorMessage = error.message ?: "Restock gagal disimpan.") }
                _events.emit(KatalogEvent.Pesan(error.message ?: "Restock gagal disimpan."))
            }
        }
    }

    fun catatPenyesuaian(barangId: String, stokFisik: Int, alasan: String, catatan: String?) {
        val idToko = _uiState.value.idToko ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                repository.catatPenyesuaian(idToko, barangId, stokFisik, alasan, catatan)
                _uiState.update { it.copy(isSaving = false, detail = null) }
                _events.emit(KatalogEvent.StokTersimpan)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _uiState.update { it.copy(isSaving = false, errorMessage = error.message ?: "Penyesuaian Stok gagal disimpan.") }
                _events.emit(KatalogEvent.Pesan(error.message ?: "Penyesuaian Stok gagal disimpan."))
            }
        }
    }

    fun simpanBarang(input: DataBarang, barangId: String? = null) {
        val idToko = _uiState.value.idToko ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                repository.simpanBarang(idToko, input, barangId)
                _uiState.update { it.copy(isSaving = false, detail = null) }
                _events.emit(KatalogEvent.BarangTersimpan)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _uiState.update { it.copy(isSaving = false, errorMessage = error.message ?: "Barang gagal disimpan.") }
                _events.emit(KatalogEvent.Pesan(error.message ?: "Barang gagal disimpan."))
            }
        }
    }

    fun hapusBarang(barangId: String) {
        val idToko = _uiState.value.idToko ?: return
        viewModelScope.launch {
            try {
                val status = repository.hapusBarang(idToko, barangId)
                _uiState.update { it.copy(detail = null) }
                _events.emit(KatalogEvent.BarangDihapus(status))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _events.emit(KatalogEvent.Pesan(error.message ?: "Barang gagal diarsipkan atau dihapus."))
            }
        }
    }

    fun simpanKategori(kategoriId: String?, nama: String) {
        val idToko = _uiState.value.idToko ?: return
        viewModelScope.launch {
            try {
                if (kategoriId == null) repository.tambahKategori(idToko, nama)
                else repository.ubahKategori(idToko, kategoriId, nama)
                _events.emit(KatalogEvent.Pesan("Kategori berhasil disimpan."))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _events.emit(KatalogEvent.Pesan(error.message ?: "Kategori gagal disimpan."))
            }
        }
    }

    fun hapusKategori(kategoriId: String) {
        val idToko = _uiState.value.idToko ?: return
        viewModelScope.launch {
            try {
                repository.hapusKategori(idToko, kategoriId)
                _events.emit(KatalogEvent.Pesan("Kategori dihapus dari Barang terkait."))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _events.emit(KatalogEvent.Pesan(error.message ?: "Kategori gagal dihapus."))
            }
        }
    }

    fun simpanSupplier(supplierId: String?, nama: String, link: String?, catatan: String?) {
        val idToko = _uiState.value.idToko ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingSupplier = true) }
            try {
                repository.simpanSupplier(idToko, supplierId, nama, link, catatan)
                _uiState.update { it.copy(isSavingSupplier = false) }
                _events.emit(KatalogEvent.Pesan("Supplier berhasil disimpan."))
            } catch (error: CancellationException) {
                _uiState.update { it.copy(isSavingSupplier = false) }
                throw error
            } catch (error: Exception) {
                _uiState.update { it.copy(isSavingSupplier = false) }
                _events.emit(KatalogEvent.Pesan(error.message ?: "Supplier gagal disimpan."))
            }
        }
    }

    fun ubahStatusSupplier(supplierId: String, status: String) {
        val idToko = _uiState.value.idToko ?: return
        viewModelScope.launch {
            try {
                repository.ubahStatusSupplier(idToko, supplierId, status)
                _events.emit(KatalogEvent.Pesan("Status Supplier berhasil diperbarui."))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _events.emit(KatalogEvent.Pesan(error.message ?: "Status Supplier gagal diperbarui."))
            }
        }
    }

    fun hapusSupplier(supplierId: String) {
        val idToko = _uiState.value.idToko ?: return
        viewModelScope.launch {
            try {
                val dihapus = repository.hapusSupplier(idToko, supplierId)
                _events.emit(KatalogEvent.SupplierBerhasilDihapus(dihapus))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _events.emit(KatalogEvent.Pesan(error.message ?: "Supplier gagal dihapus."))
            }
        }
    }

    fun unggahFoto(resolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingFoto = true, fotoTerunggahFileId = null) }
            try {
                val fileId = repository.unggahFoto(resolver, uri)
                _uiState.update { it.copy(isUploadingFoto = false, fotoTerunggahFileId = fileId) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _uiState.update { it.copy(isUploadingFoto = false) }
                _events.emit(KatalogEvent.Pesan(error.message ?: "Foto gagal diunggah."))
            }
        }
    }

    fun resetFotoUnggahan() {
        _uiState.update { it.copy(fotoTerunggahFileId = null, isUploadingFoto = false) }
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
                _uiState.update { it.copy(fileIdsGagalDimuat = it.fileIdsGagalDimuat + fileId) }
                _events.emit(KatalogEvent.Pesan(error.message ?: "Foto gagal dimuat."))
            }
        }
    }

    companion object {
        fun factory(repository: BarangRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    KatalogViewModel(repository) as T
            }
    }
}
