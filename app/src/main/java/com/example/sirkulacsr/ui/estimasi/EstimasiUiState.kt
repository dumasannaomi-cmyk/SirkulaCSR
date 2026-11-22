
package com.example.sirkulacsr.ui.estimasi

import com.example.sirkulacsr.data.local.EstimasiEntity

data class EstimasiUiState(
    val isLoading: Boolean = false,
    val hasilEstimasi: EstimasiEntity? = null,
    val riwayatEstimasi: List<EstimasiEntity> = emptyList(),
    val errorMessage: String? = null
)
