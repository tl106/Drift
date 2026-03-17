package com.drift.sleep.service

import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.content.ComponentName
import android.content.Context

class MediaControlService : NotificationListenerService() {

    companion object {
        const val TAG = "MediaControlService"

        private var instance: MediaControlService? = null

        /**
         * Pause whatever media is currently playing.
         * Returns the name of the paused app, or null if nothing was playing.
         */
        fun pauseCurrentMedia(): String? {
            return instance?.doPauseMedia()
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d(TAG, "MediaControlService created")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        Log.d(TAG, "MediaControlService destroyed")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {}
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {}

    private fun doPauseMedia(): String? {
        try {
            val manager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val componentName = ComponentName(this, MediaControlService::class.java)
            val controllers = manager.getActiveSessions(componentName)

            for (controller in controllers) {
                val state = controller.playbackState
                if (state != null && state.state == PlaybackState.STATE_PLAYING) {
                    val appName = controller.packageName
                    controller.transportControls.pause()
                    Log.d(TAG, "Paused media from: $appName")
                    return getAppLabel(appName)
                }
            }

            // No active playing session found, try sending pause to all
            for (controller in controllers) {
                controller.transportControls.pause()
                Log.d(TAG, "Sent pause to: ${controller.packageName}")
                return getAppLabel(controller.packageName)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing media", e)
        }
        return null
    }

    private fun getAppLabel(packageName: String): String {
        return try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }
}
