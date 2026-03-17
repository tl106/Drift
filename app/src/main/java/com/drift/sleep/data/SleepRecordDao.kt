package com.drift.sleep.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepRecordDao {
    @Insert
    fun insert(record: SleepRecord)

    @Query("SELECT * FROM SleepRecord ORDER BY pauseTime DESC")
    fun getAll(): Flow<List<SleepRecord>>

    @Query("DELETE FROM SleepRecord")
    fun deleteAll()
}
