package com.drift.sleep.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.drift.sleep.data.DriftPreferences

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = DriftPreferences(context)
            if (prefs.autoStartEnabled) {
                AutoStartReceiver.scheduleAutoStart(
                    context, prefs.autoStartHour, prefs.autoStartMinute
                )
                Log.d("BootReceiver", "Re-scheduled auto-start after boot")
            }
        }
    }
}
