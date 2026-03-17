package com.drift.sleep.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.drift.sleep.data.DriftPreferences
import com.drift.sleep.service.DriftService
import java.util.Calendar

class AutoStartReceiver : BroadcastReceiver() {

    companion object {
        const val TAG = "AutoStartReceiver"
        private const val REQUEST_CODE = 100

        fun scheduleAutoStart(context: Context, hour: Int, minute: Int) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AutoStartReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                if (before(Calendar.getInstance())) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }

            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )

            Log.d(TAG, "Auto-start scheduled for $hour:${minute.toString().padStart(2, '0')} daily")
        }

        fun cancelAutoStart(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AutoStartReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            Log.d(TAG, "Auto-start cancelled")
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Auto-start triggered")
        if (!DriftService.isRunning) {
            val serviceIntent = Intent(context, DriftService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }
}
