
package com.example.sirkulacsr.data.repository

import com.example.sirkulacsr.data.local.EstimasiDao
import com.example.sirkulacsr.data.local.EstimasiEntity
import kotlinx.coroutines.flow.Flow
import java.util.Locale
import kotlin.math.roundToLong

class EstimasiRepository(
    private val estimasiDao: EstimasiDao
) {

    private val hargaSimulasi = mapOf(
        ("kardus" to "Kg") to 1500L,
        ("kertas" to "Kg") to 1000L,
        ("plastik" to "Kg") to 2000L,
        ("minyak" to "L") to 3000L,
        ("minyak jelantah" to "L") to 3000L,
        ("aluminium" to "Kg") to 8000L
    )

    fun amatiRiwayat(): Flow<List<EstimasiEntity>> {
        return estimasiDao.amatiRiwayatEstimasi()
    }

    suspend fun hitungDanSimpan(
        jenisLimbah: String,
        jumlah: Double,
        satuan: String
    ): EstimasiEntity {

        require(jenisLimbah.isNotBlank()) {
            "Silakan pilih jenis limbah"
        }

        require(jumlah.isFinite() && jumlah > 0.0) {
            "Jumlah limbah harus lebih dari nol"
        }

        val jenisNormal = jenisLimbah.trim().lowercase(Locale.ROOT)

        val satuanNormal = when (
            satuan.trim().lowercase(Locale.ROOT)
        ) {
            "kg", "kilogram", "kilogram (kg)" -> "Kg"
            "l", "liter", "liter (l)" -> "L"
            else -> throw IllegalArgumentException(
                "Satuan limbah tidak dikenali"
            )
        }

        val hargaSatuan = hargaSimulasi[
            jenisNormal to satuanNormal
        ] ?: throw IllegalArgumentException(
            "Harga simulasi belum tersedia untuk jenis dan satuan tersebut"
        )

        val total = jumlah * hargaSatuan

        require(total.isFinite() && total <= Long.MAX_VALUE.toDouble()) {
            "Jumlah limbah terlalu besar"
        }

        val estimasi = EstimasiEntity(
            jenisLimbah = jenisLimbah.trim(),
            jumlah = jumlah,
            satuan = satuanNormal,
            hargaSatuanRupiah = hargaSatuan,
            totalEstimasiRupiah = total.roundToLong()
        )

        val id = estimasiDao.simpanEstimasi(estimasi)

        return estimasi.copy(id = id)
    }

    suspend fun hapusEstimasi(id: Long) {
        estimasiDao.hapusEstimasi(id)
    }
}
