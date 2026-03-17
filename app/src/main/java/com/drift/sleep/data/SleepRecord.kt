package com.drift.sleep.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class SleepRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,
    val pauseTime: Long,
    val pausedApp: String?
)
