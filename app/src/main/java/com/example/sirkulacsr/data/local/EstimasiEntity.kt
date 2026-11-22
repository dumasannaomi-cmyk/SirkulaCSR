
package com.example.sirkulacsr.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "estimasi")
data class EstimasiEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val jenisLimbah: String,
    val jumlah: Double,
    val satuan: String,
    val hargaSatuanRupiah: Long,
    val totalEstimasiRupiah: Long,
    val dibuatPada: Long = System.currentTimeMillis()
)
