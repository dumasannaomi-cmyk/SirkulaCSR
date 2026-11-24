package com.example.sirkulacsr.ui.estimasi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.sirkulacsr.data.repository.EstimasiRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EstimasiViewModel(
    private val repository: EstimasiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EstimasiUiState())

    val uiState: StateFlow<EstimasiUiState> =
        _uiState.asStateFlow()

    init {
        amatiRiwayat()
    }

    private fun amatiRiwayat() {
        viewModelScope.launch {
            repository.amatiRiwayat().collect { riwayat ->
                _uiState.update {
                    it.copy(riwayatEstimasi = riwayat)
                }
            }
        }
    }

    fun hitungEstimasi(
        jenisLimbah: String,
        jumlah: Double,
        satuan: String
    ) {
        if (_uiState.value.isLoading) return

        _uiState.update {
            it.copy(
                isLoading = true,
                hasilEstimasi = null,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            try {
                val hasil = repository.hitungDanSimpan(
                    jenisLimbah = jenisLimbah,
                    jumlah = jumlah,
                    satuan = satuan
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        hasilEstimasi = hasil,
                        errorMessage = null
                    )
                }

            } catch (e: CancellationException) {
                throw e

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        hasilEstimasi = null,
                        errorMessage = e.message
                            ?: "Gagal menghitung estimasi"
                    )
                }
            }
        }
    }

    fun hapusEstimasi(id: Long) {
        viewModelScope.launch {
            try {
                repository.hapusEstimasi(id)
            } catch (e: CancellationException) {
                throw e

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = e.message
                            ?: "Gagal menghapus estimasi"
                    )
                }
            }
        }
    }

    fun hapusPesanError() {
        _uiState.update {
            it.copy(errorMessage = null)
        }
    }

    companion object {
        fun factory(
            repository: EstimasiRepository
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {

                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: Class<T>
                ): T {
                    require(
                        modelClass.isAssignableFrom(
                            EstimasiViewModel::class.java
                        )
                    )

                    return EstimasiViewModel(repository) as T
                }
            }
        }
    }
}