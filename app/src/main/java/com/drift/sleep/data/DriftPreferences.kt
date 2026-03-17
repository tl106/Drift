package com.drift.sleep.data

import android.content.Context

class DriftPreferences(context: Context) {

    private val prefs = context.getSharedPreferences("drift_prefs", Context.MODE_PRIVATE)

    var waitMinutes: Long
        get() = prefs.getLong("wait_minutes", 25L)
        set(value) = prefs.edit().putLong("wait_minutes", value).apply()

    var fadeMinutes: Long
        get() = prefs.getLong("fade_minutes", 10L)
        set(value) = prefs.edit().putLong("fade_minutes", value).apply()

    var autoStartEnabled: Boolean
        get() = prefs.getBoolean("auto_start_enabled", false)
        set(value) = prefs.edit().putBoolean("auto_start_enabled", value).apply()

    var autoStartHour: Int
        get() = prefs.getInt("auto_start_hour", 22)
        set(value) = prefs.edit().putInt("auto_start_hour", value).apply()

    var autoStartMinute: Int
        get() = prefs.getInt("auto_start_minute", 30)
        set(value) = prefs.edit().putInt("auto_start_minute", value).apply()
}
