package com.aura.app.ui.analisis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aura.app.data.remote.dto.AnalisisRingkasanResponseDto
import com.aura.app.data.remote.dto.KorelasiDetailResponseDto
import com.aura.app.data.remote.dto.PergerakanDetailResponseDto
import com.aura.app.data.remote.dto.ProporsiDetailResponseDto
import com.aura.app.data.remote.dto.TrenDetailResponseDto
import com.aura.app.data.remote.dto.UmurStokDetailResponseDto
import com.aura.app.data.repository.AnalisisRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AnalisisPeriodeUi(val key: String, val label: String) {
    MINGGUAN("mingguan", "Mingguan"),
    BULANAN("bulanan", "Bulanan"),
}

sealed interface AnalisisDataState<out T> {
    data object Loading : AnalisisDataState<Nothing>
    data class Success<T>(val data: T, val isFromCache: Boolean = false) : AnalisisDataState<T>
    data class Error(val message: String) : AnalisisDataState<Nothing>
}

data class AnalisisUiState(
    val periode: AnalisisPeriodeUi = AnalisisPeriodeUi.MINGGUAN,
    val isRefreshing: Boolean = false,
    val overview: AnalisisDataState<AnalisisRingkasanResponseDto> = AnalisisDataState.Loading,
    val proporsiTabProfit: Boolean = true,
    val movement: AnalisisDataState<PergerakanDetailResponseDto> = AnalisisDataState.Loading,
    val movementSort: String = "kecepatan",
    val runway: AnalisisDataState<UmurStokDetailResponseDto> = AnalisisDataState.Loading,
    val proportion: AnalisisDataState<ProporsiDetailResponseDto> = AnalisisDataState.Loading,
    val trend: AnalisisDataState<TrenDetailResponseDto> = AnalisisDataState.Loading,
    val correlation: AnalisisDataState<KorelasiDetailResponseDto> = AnalisisDataState.Loading,
    val expandedCorrelationId: String? = null,
)

class AnalisisViewModel(
    private val repository: AnalisisRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AnalisisUiState())
    val uiState: StateFlow<AnalisisUiState> = _uiState.asStateFlow()

    private var overviewJob: Job? = null
    private var detailJob: Job? = null
    private var activeStoreId: String? = null

    fun sambungkanSesi(idToko: String?) {
        if (activeStoreId == idToko) return
        activeStoreId = idToko
        overviewJob?.cancel()
        detailJob?.cancel()
        repository.clearCache()
        _uiState.value = AnalisisUiState()
    }

    fun setPeriode(periode: AnalisisPeriodeUi, reloadDetail: (() -> Unit)? = null) {
        if (_uiState.value.periode == periode) return
        _uiState.update { it.copy(periode = periode, expandedCorrelationId = null) }
        muatOverview()
        reloadDetail?.invoke()
    }

    fun setProporsiTab(profit: Boolean) {
        _uiState.update { it.copy(proporsiTabProfit = profit) }
    }

    fun setMovementSort(sort: String) {
        _uiState.update { it.copy(movementSort = sort) }
    }

    fun toggleCorrelation(productId: String) {
        _uiState.update { state ->
            state.copy(
                expandedCorrelationId = if (state.expandedCorrelationId == productId) null else productId,
            )
        }
    }

    fun muatOverview(refreshing: Boolean = false) {
        val periode = _uiState.value.periode.key
        overviewJob?.cancel()
        overviewJob = viewModelScope.launch {
            val cached = repository.cachedOverview(periode)
            if (cached != null && refreshing) {
                _uiState.update { it.copy(isRefreshing = true, overview = AnalisisDataState.Success(cached, isFromCache = true)) }
            } else if (cached != null) {
                _uiState.update { it.copy(overview = AnalisisDataState.Success(cached, isFromCache = true)) }
            } else {
                _uiState.update { it.copy(overview = AnalisisDataState.Loading) }
            }

            try {
                val fresh = repository.getRingkasan(periode)
                _uiState.update {
                    it.copy(isRefreshing = false, overview = AnalisisDataState.Success(fresh, isFromCache = false))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val fallback = repository.cachedOverview(periode)
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        overview = if (fallback != null) {
                            AnalisisDataState.Success(fallback, isFromCache = true)
                        } else {
                            AnalisisDataState.Error(e.message ?: "Gagal memuat ringkasan analisis.")
                        },
                    )
                }
            }
        }
    }

    fun muatPergerakan(jenis: String) {
        val periode = _uiState.value.periode.key
        loadDetail(
            cached = { repository.cachedMovement(periode, jenis) },
            fetch = { repository.getPergerakan(periode, jenis) },
            onState = { state -> _uiState.update { it.copy(movement = state) } },
            fallbackMsg = "Gagal memuat data pergerakan barang.",
        )
    }

    fun muatUmurStok() {
        val periode = _uiState.value.periode.key
        loadDetail(
            cached = { repository.cachedRunway(periode) },
            fetch = { repository.getUmurStok(periode) },
            onState = { state -> _uiState.update { it.copy(runway = state) } },
            fallbackMsg = "Gagal memuat estimasi umur stok.",
        )
    }

    fun muatProporsi(jenis: String) {
        val periode = _uiState.value.periode.key
        loadDetail(
            cached = { repository.cachedProportion(periode, jenis) },
            fetch = { repository.getProporsi(periode, jenis) },
            onState = { state -> _uiState.update { it.copy(proportion = state) } },
            fallbackMsg = "Gagal memuat proporsi penjualan.",
        )
    }

    fun muatTren(arah: String) {
        val periode = _uiState.value.periode.key
        loadDetail(
            cached = { repository.cachedTrend(periode, arah) },
            fetch = { repository.getTren(periode, arah) },
            onState = { state -> _uiState.update { it.copy(trend = state) } },
            fallbackMsg = "Gagal memuat tren penjualan.",
        )
    }

    fun muatKorelasi() {
        val periode = _uiState.value.periode.key
        loadDetail(
            cached = { repository.cachedCorrelation(periode) },
            fetch = { repository.getKorelasi(periode) },
            onState = { state -> _uiState.update { it.copy(correlation = state) } },
            fallbackMsg = "Gagal memuat korelasi produk.",
        )
    }

    private fun <T> loadDetail(
        cached: () -> T?,
        fetch: suspend () -> T,
        onState: (AnalisisDataState<T>) -> Unit,
        fallbackMsg: String,
    ) {
        detailJob?.cancel()
        detailJob = viewModelScope.launch {
            val c = cached()
            if (c != null) {
                onState(AnalisisDataState.Success(c, isFromCache = true))
            } else {
                onState(AnalisisDataState.Loading)
            }
            try {
                val fresh = fetch()
                onState(AnalisisDataState.Success(fresh, isFromCache = false))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val fb = cached()
                if (fb != null) {
                    onState(AnalisisDataState.Success(fb, isFromCache = true))
                } else {
                    onState(AnalisisDataState.Error(e.message ?: fallbackMsg))
                }
            }
        }
    }

    companion object {
        fun factory(repository: AnalisisRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = AnalisisViewModel(repository) as T
            }
    }
}
