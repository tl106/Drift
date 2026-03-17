package com.drift.sleep.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.drift.sleep.R
import com.drift.sleep.service.DriftService

class DriftWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE = "com.drift.sleep.widget.TOGGLE"

        fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, DriftWidget::class.java))
            for (id in ids) {
                updateWidget(context, manager, id)
            }
        }

        private fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_drift)
            val isRunning = DriftService.isRunning

            views.setTextViewText(
                R.id.widget_status,
                if (isRunning) "正在守护" else "点击开始"
            )
            views.setTextViewText(
                R.id.widget_button_text,
                if (isRunning) "停止" else "开始"
            )

            val toggleIntent = Intent(context, DriftWidget::class.java).apply {
                action = ACTION_TOGGLE
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            manager.updateAppWidget(widgetId, views)
        }
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, widgetIds: IntArray) {
        for (id in widgetIds) {
            updateWidget(context, manager, id)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE) {
            if (DriftService.isRunning) {
                context.stopService(Intent(context, DriftService::class.java))
            } else {
                ContextCompat.startForegroundService(
                    context, Intent(context, DriftService::class.java)
                )
            }
            // Update widget state after a short delay
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                updateAllWidgets(context)
            }, 500)
        }
    }
}
