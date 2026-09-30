package com.example.services.localstorage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        DriverProfileEntity::class,
        KycDocumentEntity::class,
        VehicleEntity::class,
        RideOrderEntity::class,
        LedgerEntryEntity::class,
        ReviewEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class ZaldiDatabase : RoomDatabase() {
    abstract fun zaldiDao(): ZaldiDao

    companion object {
        @Volatile
        private var INSTANCE: ZaldiDatabase? = null

        fun getInstance(context: Context): ZaldiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ZaldiDatabase::class.java,
                    "zaldi_driver_relational_v5.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
