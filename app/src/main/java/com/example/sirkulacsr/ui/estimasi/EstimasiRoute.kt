package com.example.sirkulacsr.ui.estimasi

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sirkulacsr.data.local.EstimasiDatabase
import com.example.sirkulacsr.data.repository.EstimasiRepository

@Composable
fun EstimasiRoute(
    onJualSekarang: (() -> Unit)? = null,
    onBukaChat: (() -> Unit)? = null,
    onBukaNotifikasi: (() -> Unit)? = null,
    onLihatSemua: (() -> Unit)? = null,
    onNavigasi: ((String) -> Unit)? = null
) {
    val context = LocalContext.current

    val repository = remember(context) {
        val database = EstimasiDatabase.getInstance(context)

        EstimasiRepository(
            estimasiDao = database.estimasiDao()
        )
    }

    val estimasiViewModel: EstimasiViewModel = viewModel(
        factory = EstimasiViewModel.factory(repository)
    )

    val uiState by estimasiViewModel.uiState
        .collectAsStateWithLifecycle()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        EstimasiScreen(
            isLoading = uiState.isLoading,
            nilaiEstimasi = uiState.hasilEstimasi
                ?.totalEstimasiRupiah,
            onHitungNilai = { jenis, jumlah, satuan ->
                estimasiViewModel.hitungEstimasi(
                    jenisLimbah = jenis,
                    jumlah = jumlah,
                    satuan = satuan
                )
            },
            onJualSekarang = onJualSekarang,
            onBukaChat = onBukaChat,
            onBukaNotifikasi = onBukaNotifikasi,
            onLihatSemua = onLihatSemua,
            onNavigasi = onNavigasi
        )

        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }

    uiState.errorMessage?.let { pesan ->
        AlertDialog(
            onDismissRequest = {
                estimasiViewModel.hapusPesanError()
            },
            title = {
                Text("Estimasi Belum Berhasil")
            },
            text = {
                Text(pesan)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        estimasiViewModel.hapusPesanError()
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }
}