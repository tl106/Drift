package com.drift.sleep.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/**
 * Helper to guide users through disabling battery optimization
 * for Drift on various Chinese Android manufacturers.
 */
object BatteryHelper {

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun requestIgnoreBatteryOptimizations(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {
            // Fallback to general battery settings
            openBatterySettings(context)
        }
    }

    fun openBatterySettings(context: Context) {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val intent = when {
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> huaweiIntent()
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") -> xiaomiIntent()
            manufacturer.contains("oppo") || manufacturer.contains("realme") || manufacturer.contains("oneplus") -> oppoIntent()
            manufacturer.contains("vivo") -> vivoIntent()
            manufacturer.contains("samsung") -> samsungIntent()
            else -> defaultIntent()
        }

        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                context.startActivity(defaultIntent().apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
            } catch (_: Exception) {}
        }
    }

    fun getManufacturerTip(): String {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return when {
            manufacturer.contains("huawei") || manufacturer.contains("honor") ->
                "华为/荣耀：设置 → 电池 → 启动管理 → 找到Drift → 关闭自动管理"
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") ->
                "小米/红米：设置 → 电池与性能 → 应用智能省电 → 找到Drift → 无限制"
            manufacturer.contains("oppo") || manufacturer.contains("realme") || manufacturer.contains("oneplus") ->
                "OPPO/一加：设置 → 电池 → 更多电池设置 → 优化电池使用 → 找到Drift → 不优化"
            manufacturer.contains("vivo") ->
                "vivo：设置 → 电池 → 后台高耗电 → 允许Drift后台运行"
            manufacturer.contains("samsung") ->
                "三星：设置 → 电池 → 后台使用限制 → 找到Drift → 取消限制"
            else ->
                "设置 → 电池 → 找到Drift → 允许后台运行"
        }
    }

    private fun huaweiIntent() = Intent().apply {
        component = ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")
    }

    private fun xiaomiIntent() = Intent().apply {
        component = ComponentName("com.miui.powerkeeper", "com.miui.powerkeeper.ui.HiddenAppsConfigActivity")
        putExtra("package_name", "com.drift.sleep")
        putExtra("package_label", "Drift")
    }

    private fun oppoIntent() = Intent().apply {
        component = ComponentName("com.coloros.oppoguardelf", "com.coloros.powermanager.fuelgaue.PowerUsageModelActivity")
    }

    private fun vivoIntent() = Intent().apply {
        component = ComponentName("com.vivo.abe", "com.vivo.applicationbehaviorengine.ui.ExcessivePowerManagerActivity")
    }

    private fun samsungIntent() = Intent().apply {
        component = ComponentName("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity")
    }

    private fun defaultIntent() = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
}
