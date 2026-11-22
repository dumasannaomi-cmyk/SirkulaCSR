
package com.example.sirkulacsr.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EstimasiDao {

    @Insert
    suspend fun simpanEstimasi(
        estimasi: EstimasiEntity
    ): Long

    @Query(
        "SELECT * FROM estimasi ORDER BY dibuatPada DESC, id DESC"
    )
    fun amatiRiwayatEstimasi(): Flow<List<EstimasiEntity>>

    @Query(
        "SELECT * FROM estimasi WHERE id = :id LIMIT 1"
    )
    suspend fun ambilEstimasi(
        id: Long
    ): EstimasiEntity?

    @Query(
        "DELETE FROM estimasi WHERE id = :id"
    )
    suspend fun hapusEstimasi(
        id: Long
    )
}
