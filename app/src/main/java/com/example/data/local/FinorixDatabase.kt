package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SignalEntity::class],
    version = 1,
    exportSchema = false
)
abstract class FinorixDatabase : RoomDatabase() {
    abstract fun signalDao(): SignalDao

    companion object {
        @Volatile
        private var INSTANCE: FinorixDatabase? = null

        fun getInstance(context: Context): FinorixDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FinorixDatabase::class.java,
                    "finorix_pro_signals.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
