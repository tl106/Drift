package com.drift.sleep.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SleepRecord::class], version = 1)
abstract class DriftDatabase : RoomDatabase() {
    abstract fun sleepRecordDao(): SleepRecordDao

    companion object {
        @Volatile
        private var INSTANCE: DriftDatabase? = null

        fun getInstance(context: Context): DriftDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    DriftDatabase::class.java,
                    "drift_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
