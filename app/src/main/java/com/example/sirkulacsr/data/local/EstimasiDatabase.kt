
package com.example.sirkulacsr.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [EstimasiEntity::class],
    version = 1,
    exportSchema = false
)
abstract class EstimasiDatabase : RoomDatabase() {

    abstract fun estimasiDao(): EstimasiDao

    companion object {

        @Volatile
        private var INSTANCE: EstimasiDatabase? = null

        fun getInstance(context: Context): EstimasiDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    EstimasiDatabase::class.java,
                    "sirkula_estimasi.db"
                ).build().also {
                    INSTANCE = it
                }
            }
        }
    }
}
