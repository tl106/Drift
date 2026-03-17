package com.drift.sleep

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import com.drift.sleep.data.DriftDatabase
import com.drift.sleep.data.DriftPreferences
import com.drift.sleep.receiver.AutoStartReceiver
import com.drift.sleep.service.DriftService
import com.drift.sleep.service.MediaControlService
import com.drift.sleep.ui.DriftApp
import com.drift.sleep.ui.Page
import com.drift.sleep.ui.theme.DriftTheme
import com.drift.sleep.util.BatteryHelper
import com.drift.sleep.util.ShareCardGenerator

class MainActivity : ComponentActivity() {

    private val isRunning = mutableStateOf(false)
    private val hasNotificationAccess = mutableStateOf(false)
    private val hasNotificationPermission = mutableStateOf(false)
    private val currentPage = mutableStateOf(Page.Home)
    private val waitMinutes = mutableStateOf(25L)
    private val fadeMinutes = mutableStateOf(10L)
    private val autoStartEnabled = mutableStateOf(false)
    private val autoStartHour = mutableIntStateOf(22)
    private val autoStartMinute = mutableIntStateOf(30)
    private val isBatteryOptimized = mutableStateOf(false)

    private lateinit var prefs: DriftPreferences

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission.value = granted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        prefs = DriftPreferences(this)
        waitMinutes.value = prefs.waitMinutes
        fadeMinutes.value = prefs.fadeMinutes
        autoStartEnabled.value = prefs.autoStartEnabled
        autoStartHour.intValue = prefs.autoStartHour
        autoStartMinute.intValue = prefs.autoStartMinute

        val db = DriftDatabase.getInstance(this)

        setContent {
            val records by db.sleepRecordDao().getAll().collectAsState(initial = emptyList())

            DriftTheme {
                DriftApp(
                    currentPage = currentPage.value,
                    onPageChange = { currentPage.value = it },
                    isRunning = isRunning.value,
                    hasNotificationAccess = hasNotificationAccess.value,
                    hasNotificationPermission = hasNotificationPermission.value,
                    onToggle = { toggle() },
                    onRequestNotificationAccess = { requestNotificationAccess() },
                    onRequestNotificationPermission = { requestNotificationPermission() },
                    waitMinutes = waitMinutes.value,
                    fadeMinutes = fadeMinutes.value,
                    onWaitChange = { minutes ->
                        waitMinutes.value = minutes
                        prefs.waitMinutes = minutes
                    },
                    onFadeChange = { minutes ->
                        fadeMinutes.value = minutes
                        prefs.fadeMinutes = minutes
                    },
                    autoStartEnabled = autoStartEnabled.value,
                    autoStartHour = autoStartHour.intValue,
                    autoStartMinute = autoStartMinute.intValue,
                    onAutoStartToggle = { enabled ->
                        autoStartEnabled.value = enabled
                        prefs.autoStartEnabled = enabled
                        if (enabled) {
                            AutoStartReceiver.scheduleAutoStart(this, autoStartHour.intValue, autoStartMinute.intValue)
                        } else {
                            AutoStartReceiver.cancelAutoStart(this)
                        }
                    },
                    onAutoStartTimeChange = { hour, minute ->
                        autoStartHour.intValue = hour
                        autoStartMinute.intValue = minute
                        prefs.autoStartHour = hour
                        prefs.autoStartMinute = minute
                        if (autoStartEnabled.value) {
                            AutoStartReceiver.scheduleAutoStart(this, hour, minute)
                        }
                    },
                    isBatteryOptimized = isBatteryOptimized.value,
                    onRequestBatteryOptimization = {
                        BatteryHelper.requestIgnoreBatteryOptimizations(this)
                    },
                    batteryTip = BatteryHelper.getManufacturerTip(),
                    records = records,
                    onShareRecord = { record ->
                        ShareCardGenerator.generateAndShare(this@MainActivity, record)
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hasNotificationAccess.value = isNotificationListenerEnabled()
        hasNotificationPermission.value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else true
        isRunning.value = DriftService.isRunning
        isBatteryOptimized.value = BatteryHelper.isIgnoringBatteryOptimizations(this)
    }

    private fun toggle() {
        if (isRunning.value) {
            stopDriftService()
        } else {
            startDriftService()
        }
        isRunning.value = !isRunning.value
    }

    private fun startDriftService() {
        val intent = Intent(this, DriftService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

    private fun stopDriftService() {
        val intent = Intent(this, DriftService::class.java)
        stopService(intent)
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        val componentName = ComponentName(this, MediaControlService::class.java)
        return flat?.contains(componentName.flattenToString()) == true
    }

    private fun requestNotificationAccess() {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
